package de.aporz.doctorassistant.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.aporz.doctorassistant.config.VectorTopKProperties;
import de.aporz.doctorassistant.dto.QueryRequest;
import de.aporz.doctorassistant.dto.QueryResponse;
import de.aporz.doctorassistant.entity.Patient;
import de.aporz.doctorassistant.repository.PatientRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.text.StringEscapeUtils;

@Service
public class QueryService {

    public enum Database { PATIENT_DOCUMENTS, MEDICAL_KNOWLEDGE }

    public static class QuerySpec {
        public String query;
        public Database database;
        public String from;
        public String to;
    }

    public static class Llm1Response {
        public String answer;
        public List<QuerySpec> queries;
    }

    private final ChatClient.Builder chatClientBuilder;
    private final VectorStore medicalVectorStore;
    private final VectorStore patientVectorStore;
    private final VectorTopKProperties topKProps;
    private final PatientRepository patientRepository;
    private final ObjectMapper mapper;

    public QueryService(ChatClient.Builder chatClientBuilder,
                        @Qualifier("medicalVectorStore") VectorStore medicalVectorStore,
                        @Qualifier("patientVectorStore") VectorStore patientVectorStore,
                        VectorTopKProperties topKProps,
                        PatientRepository patientRepository) {
        this.chatClientBuilder = chatClientBuilder;
        this.medicalVectorStore = medicalVectorStore;
        this.patientVectorStore = patientVectorStore;
        this.topKProps = topKProps;
        this.patientRepository = patientRepository;
        this.mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public QueryResponse handle(QueryRequest request) {
        Patient patient = patientRepository.findById(request.getPatientId()).orElseThrow();

        Llm1Response plan = callLlm1(request, patient);
        
        QueryResponse response = new QueryResponse();
        List<QueryResponse.PatientDocumentDto> patientDocs = new ArrayList<>();
        List<QueryResponse.KnowledgeDocumentDto> knowledgeDocs = new ArrayList<>();
        
        if (plan.answer != null && !plan.answer.isBlank()) {
            response.setAnswer(plan.answer);
            response.setPatientDocuments(patientDocs);
            response.setKnowledgeDocuments(knowledgeDocs);
            return response;
        }

        List<Document> collected = new ArrayList<>();
        for (QuerySpec spec : Optional.ofNullable(plan.queries).orElseGet(List::of)) {
            switch (spec.database) {
                case PATIENT_DOCUMENTS -> {
                    List<Document> docs = searchPatientDocuments(request.getPatientId(), spec);
                    collected.addAll(docs);
                    for (Document doc : docs) {
                        QueryResponse.PatientDocumentDto dto = new QueryResponse.PatientDocumentDto();
                        dto.setId(doc.getId());
                        dto.setContent(doc.getText());
                        dto.setPatientId(request.getPatientId());
                        if (doc.getMetadata().containsKey("document_date")) {
                            dto.setDocumentDate(LocalDate.parse(doc.getMetadata().get("document_date").toString()));
                        }
                        patientDocs.add(dto);
                    }
                }
                case MEDICAL_KNOWLEDGE -> {
                    List<Document> docs = searchKnowledgeDatabase(spec);
                    collected.addAll(docs);
                    for (Document doc : docs) {
                        QueryResponse.KnowledgeDocumentDto dto = new QueryResponse.KnowledgeDocumentDto();
                        dto.setId(doc.getId());
                        dto.setContent(doc.getText());
                        knowledgeDocs.add(dto);
                    }
                }
            }
        }

        String answer = callLlm2(request, patient, collected);
        response.setAnswer(answer);
        response.setPatientDocuments(patientDocs);
        response.setKnowledgeDocuments(knowledgeDocs);
        return response;
    }

    private Llm1Response callLlm1(QueryRequest request, Patient patient) {
        String sys = "You are a medical assistant. Output ONLY valid JSON without any markdown formatting, backticks, or additional text.";
        String jsonSchema = "Required JSON format: {\"answer\": string|null, \"queries\": [{\"query\": string, \"database\": string, \"from\": string|null, \"to\": string|null}]} where database must be either PATIENT_DOCUMENTS or MEDICAL_KNOWLEDGE. Dates as ISO-8601 or NOW.";
        String user = "User question: " + request.getQuery() + "\n" +
                "Patient: name=" + safe(patient.getFirstName()) + " " + safe(patient.getLastName()) + ", notes=" + safe(patient.getNotes()) + "\n" +
                "If you need DB queries, output them in German terms where appropriate.\n" +
                jsonSchema + "\nIMPORTANT: Return ONLY raw JSON, no markdown, no backticks, no explanations.";

        CallResponseSpec resp = chatClientBuilder.build().prompt().system(sys).user(user).call();
        String content = resp.content();
        
        // Clean up response if it contains markdown formatting
        content = content.trim();
        if (content.startsWith("```json")) {
            content = content.substring(7);
        } else if (content.startsWith("```")) {
            content = content.substring(3);
        }
        if (content.endsWith("```")) {
            content = content.substring(0, content.length() - 3);
        }
        content = content.trim();
        
        try {
            return mapper.readValue(content, Llm1Response.class);
        } catch (JsonProcessingException e) {
            // one retry with stricter instruction
            String retry = chatClientBuilder.build().prompt().system(sys)
                    .user(user + "\nERROR: Your previous response was not valid JSON. Return ONLY the JSON object, nothing else. No backticks, no markdown, no explanations.").call().content();
            
            // Clean retry response too
            retry = retry.trim();
            if (retry.startsWith("```json")) {
                retry = retry.substring(7);
            } else if (retry.startsWith("```")) {
                retry = retry.substring(3);
            }
            if (retry.endsWith("```")) {
                retry = retry.substring(0, retry.length() - 3);
            }
            retry = retry.trim();
            
            try {
                return mapper.readValue(retry, Llm1Response.class);
            } catch (JsonProcessingException ex) {
                throw new RuntimeException("InvalidLlmJsonResponseException: " + ex.getMessage());
            }
        }
    }

    private List<Document> searchKnowledgeDatabase(QuerySpec spec) {
        int topK = Math.max(1, topKProps.getKnowledge());
        SearchRequest.Builder b = SearchRequest.builder().query(spec.query).topK(topK);
        return medicalVectorStore.similaritySearch(b.build());
    }

    private List<Document> searchPatientDocuments(Long patientId, QuerySpec spec) {
        int topK = Math.max(1, topKProps.getPatient());
        LocalDate from = de.aporz.doctorassistant.util.QueryUtils.parseDateOrNull(spec.from);
        LocalDate to = de.aporz.doctorassistant.util.QueryUtils.parseDateOrNull(spec.to);
        String filter = de.aporz.doctorassistant.util.QueryUtils.buildPatientFilter(patientId, from, to);
        SearchRequest.Builder b = SearchRequest.builder().query(spec.query).topK(topK).filterExpression(filter);
        return patientVectorStore.similaritySearch(b.build());
    }

    private String callLlm2(QueryRequest request, Patient patient, List<Document> contextDocs) {
        String contextMedical = contextDocs.stream()
                .map(d -> "<doc id=\"" + safe(d.getId()) + "\">" + safe(d.getText()) + "</doc>")
                .collect(Collectors.joining("\n"));

        String userPrompt = "<patient><id>" + patient.getId() + "</id><name>" + safe(patient.getFirstName() + " " + patient.getLastName()) + "</name><notes>" + safe(patient.getNotes()) + "</notes></patient>\n" +
                "<context>" + contextMedical + "</context>\n" +
                "<question>" + safe(request.getQuery()) + "</question>\n" +
                "Answer in German. If not answerable, say you cannot answer with available context.";

        return chatClientBuilder.build().prompt().user(userPrompt).call().content();
    }

    private static String safe(String s) {
        if (s == null) return "";
        return StringEscapeUtils.escapeXml11(s);
    }
}
