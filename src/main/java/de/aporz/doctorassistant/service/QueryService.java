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
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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

        String answer = callLlm2(request, patient, patientDocs, knowledgeDocs);

        log.info("response of llm 2: {}", answer);

        response.setAnswer(answer);
        response.setPatientDocuments(patientDocs);
        response.setKnowledgeDocuments(knowledgeDocs);
        return response;
    }

    private Llm1Response callLlm1(QueryRequest request, Patient patient) {
        String user = """
                You are an AI assistant for a medical information system used in a doctor's office. Your task is to analyze patient questions and data, and either provide an immediate answer or suggest appropriate database queries to find the information.
                
                You will receive a user question and patient information in the following format:
                
                <user_question>
                %s
                </user_question>
                
                <patient_info>
                Name: %s %s
                Notes: %s
                </patient_info>
                
                Analyze the question and patient data carefully. Determine if you can answer the question immediately based on the provided patient information or your general medical knowledge.
                
                If you can answer the question immediately:
                1. Formulate a clear and concise answer in german language.
                2. Output the response in the required JSON format with the answer field filled and an empty queries array.
                
                If you cannot answer the question immediately and need to query a database:
                1. Determine which database(s) to query: PATIENT_DOCUMENTS for patient-specific information or MEDICAL_KNOWLEDGE for general medical information.
                2. Formulate appropriate query or queries in German where medical terms are involved.
                3. If relevant, specify date ranges using the "from" and "to" fields. Use ISO-8601 format for dates or "NOW" for the current date.
                4. Output the response in the required JSON format with a null answer field and the queries array filled with the necessary queries.
                
                When formulating queries:
                - Use clear and specific terms related to the question and patient information.
                - For PATIENT_DOCUMENTS queries, include relevant patient identifiers or characteristics.
                - For MEDICAL_KNOWLEDGE queries, focus on general medical terms and concepts.
                
                The output must be in the following JSON format:
                {"answer": string|null, "queries": [{"query": string, "database": string, "from": string|null, "to": string|null}]}
                
                Where:
                - "answer" is either a string containing the immediate answer or null if database queries are needed.
                - "queries" is an array of query objects, each containing:
                  - "query": the query string in German where appropriate
                  - "database": either "PATIENT_DOCUMENTS" or "MEDICAL_KNOWLEDGE"
                  - "from" and "to": date range in ISO-8601 format or "NOW", or null if not applicable
                
                VERY IMPORTANT:
                If you are missing information to answer the doctors question. Then make the answer null and provide the queries (in the format described above).
                With these queries the databases will be searched.
                A query shall consist of keywords (search-terms). 
                Do not include the patient name (the system knows that).
                You can search a database with more than one query.
                For every query you add an entry to the list.  
                Example:
                ```json
                {
                   answer: null
                   queries: [
                    {"Blutmessung", "PATIENT_DOCUMENTS"},
                    {"Creatin", "PATIENT_DOCUMENTS"},
                    {"Creatine Fitness", "MEDICAL_KNOWLEDGE"},
                    (...)
                   ]
                }
                ```
                As you see here the patient documents are searched using 2 different queries.
                
                For searches in the PATIENT_DOCUMENTS you can optionally provide a from and to information:
                Example:
                ```json
                {
                   answer: null
                   queries: [
                    {"some query", "PATIENT_DOCUMENTS", "2025-09-04", "NOW"},
                   ]
                }
                ```
                Only use this if the doctor requests information from a specific time period.
                NOW is a special keyword that references the current time. Otherwise the date must be in ISO-8601 format.
                
                Important reminders:
                - Output only raw JSON, without any markdown, backticks, or explanations.
                - Use German terms in the queries where appropriate, especially for medical terminology.
                - Ensure that the "database" field is always either "PATIENT_DOCUMENTS" or "MEDICAL_KNOWLEDGE".""".formatted(
                request.getQuery(),
                safe(patient.getFirstName()),
                safe(patient.getLastName()),
                safe(patient.getNotes())
        );

        CallResponseSpec resp = chatClientBuilder.build().prompt().user(user).call();
        String content = resp.content();

        log.info("response of llm 1: {}", content);
        
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
            String retry = chatClientBuilder.build().prompt()
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
        return patientVectorStore.similaritySearch(b.similarityThreshold(0.3).build());
    }

    private String callLlm2(QueryRequest request, Patient patient, List<QueryResponse.PatientDocumentDto> patientDocs, List<QueryResponse.KnowledgeDocumentDto> knowledgeDocs) {
        String patientDocuments = patientDocs.stream()
                .map(d -> safe(d.getContent()))
                .collect(Collectors.joining("\n\n"));

        String generalMedicalDocuments = knowledgeDocs.stream()
                .map(d -> safe(d.getContent()))
                .collect(Collectors.joining("\n\n"));

        String userPrompt = """
                You are an AI assistant working in a medical practice to help doctors retrieve and analyze patient information. Your primary role is to provide accurate and helpful responses to doctors' queries based on the available patient data and medical documents. Always prioritize the information provided in the patient-specific documents and general medical documents over your general knowledge.
                
                Here is the information you have access to:
                
                <patient_name>
                %s
                </patient_name>
                
                <patient_notes>
                %s
                </patient_notes>
                
                <general_medical_documents>
                %s
                </general_medical_documents>
                
                <patient_specific_documents>
                %s
                </patient_specific_documents>
                
                The doctor's question(s) is/are as follows:
                <doctor_question>
                %s
                </doctor_question>
                
                Guidelines for using the provided information:
                1. Always prioritize information from the patient-specific documents and general medical documents over your general knowledge.
                2. If the required information is not available in any of the provided documents or your general knowledge, clearly state that you cannot answer the question due to lack of available data.
                
                Instructions for answering the doctor's question(s):
                1. Carefully read and analyze the doctor's question(s).
                2. Review the patient-specific documents and general medical documents for relevant information.
                3. Formulate a comprehensive and detailed answer based primarily on the provided documents.
                4. Ensure your response is thorough and addresses all parts of the doctor's question(s).
                5. If you cannot answer the question or parts of the doctor's question(s), then clearly tell the doctor what you can't answer.
                6. Maintain a friendly and professional tone throughout your response.
                7. And most important: Formulate your answer in German
                
                Special considerations and restrictions:
                1. Do not invent or assume any patient information that is not present in the provided documents.
                2. If multiple questions are asked, address each one separately and thoroughly.
                
                Output format instructions:
                Provide your response in the following format:
                - Your detailed answer to the doctor's question(s), following the guidelines and instructions provided above
                - If applicable, include a clear statement about which parts of the answer are based on your general knowledge rather than the provided documents]
                - If unable to answer any part of the question due to lack of available data, clearly state this

                Remember to answer in german while maintaining a professional and helpful tone throughout your response, and always prioritize the information from the provided documents over your general knowledge.""".formatted(
                safe(patient.getFirstName() + " " + patient.getLastName()),
                safe(patient.getNotes()),
                generalMedicalDocuments,
                patientDocuments,
                safe(request.getQuery())
        );

        log.info("userPrompt of llm 2: {}", userPrompt);

        return chatClientBuilder.build().prompt().user(userPrompt).call().content();
    }

    private static String safe(String s) {
        if (s == null) return "";
        return StringEscapeUtils.escapeXml11(s);
    }
}
