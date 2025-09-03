package de.aporz.doctorassistant.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.aporz.doctorassistant.config.VectorTopKProperties;
import de.aporz.doctorassistant.dto.QueryRequest;
import de.aporz.doctorassistant.entity.Patient;
import de.aporz.doctorassistant.repository.PatientRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.text.StringEscapeUtils;

@Service
@ConditionalOnBean(name = {"medicalVectorStore", "patientVectorStore"})
public class QueryService {

    public enum Database { PATIENT_DOCUMENTS, MEDICAL_KNOWLEDGE }

    public static class QuerySpec {
        public String query;
        public Database database;
        public String from;
        public String to;
        public String order;
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

    public String handle(QueryRequest request) {
        Patient patient = patientRepository.findById(request.getPatientId()).orElseThrow();

        Llm1Response plan = callLlm1(request, patient);
        if (plan.answer != null && !plan.answer.isBlank()) {
            return plan.answer;
        }

        List<Document> collected = new ArrayList<>();
        for (QuerySpec spec : Optional.ofNullable(plan.queries).orElseGet(List::of)) {
            switch (spec.database) {
                case PATIENT_DOCUMENTS -> collected.addAll(searchPatient(request.getPatientId(), spec));
                case MEDICAL_KNOWLEDGE -> collected.addAll(searchMedical(spec));
            }
        }

        return callLlm2(request, patient, collected);
    }

    private Llm1Response callLlm1(QueryRequest request, Patient patient) {
        String sys = "You are a medical assistant. Decide whether to answer directly or propose vector DB queries.";
        String jsonSchema = "Output JSON with: {answer: string|null, queries: [{query, database, from?, to?, order?}]} where database in [PATIENT_DOCUMENTS, MEDICAL_KNOWLEDGE]. Dates ISO-8601 or NOW.";
        String user = "User question: " + request.getQuery() + "\n" +
                "Patient: name=" + safe(patient.getFirstName()) + " " + safe(patient.getLastName()) + ", notes=" + safe(patient.getNotes()) + "\n" +
                "If you need DB queries, output them in German terms where appropriate.\n" +
                jsonSchema + " Only JSON, no prose.";

        CallResponseSpec resp = chatClientBuilder.build().prompt().system(sys).user(user).call();
        String content = resp.content();
        try {
            return mapper.readValue(content, Llm1Response.class);
        } catch (JsonProcessingException e) {
            // one retry with stricter instruction
            String retry = chatClientBuilder.build().prompt().system(sys)
                    .user(user + "\nAchtung: Antworte strikt als valides JSON ohne weiteren Text.").call().content();
            try {
                return mapper.readValue(retry, Llm1Response.class);
            } catch (JsonProcessingException ex) {
                throw new RuntimeException("InvalidLlmJsonResponseException: " + ex.getMessage());
            }
        }
    }

    private List<Document> searchMedical(QuerySpec spec) {
        int topK = Math.max(1, topKProps.getKnowledge());
        SearchRequest.Builder b = SearchRequest.builder().query(spec.query).topK(topK);
        return medicalVectorStore.similaritySearch(b.build());
    }

    private List<Document> searchPatient(Long patientId, QuerySpec spec) {
        int topK = Math.max(1, topKProps.getPatient());
        LocalDate from = de.aporz.doctorassistant.util.QueryUtils.parseDateOrNull(spec.from);
        LocalDate to = de.aporz.doctorassistant.util.QueryUtils.parseDateOrNull(spec.to);
        String filter = de.aporz.doctorassistant.util.QueryUtils.buildPatientFilter(patientId, from, to);
        SearchRequest.Builder b = SearchRequest.builder().query(spec.query).topK(topK).filterExpression(filter);
        return patientVectorStore.similaritySearch(b.build());
    }


    private String callLlm2(QueryRequest request, Patient patient, List<Document> contextDocs) {
        String contextMedical = contextDocs.stream()
                .map(d -> "<doc id=\"" + safe(d.getId()) + "\">" + safe(d.getContent()) + "</doc>")
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
