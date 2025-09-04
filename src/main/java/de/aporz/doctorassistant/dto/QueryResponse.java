package de.aporz.doctorassistant.dto;

import java.time.LocalDate;
import java.util.List;

public class QueryResponse {
    private String answer;
    private List<PatientDocumentDto> patientDocuments;
    private List<KnowledgeDocumentDto> knowledgeDocuments;

    public static class PatientDocumentDto {
        private String id;
        private String content;
        private Long patientId;
        private LocalDate documentDate;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public Long getPatientId() { return patientId; }
        public void setPatientId(Long patientId) { this.patientId = patientId; }
        public LocalDate getDocumentDate() { return documentDate; }
        public void setDocumentDate(LocalDate documentDate) { this.documentDate = documentDate; }
    }

    public static class KnowledgeDocumentDto {
        private String id;
        private String content;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public List<PatientDocumentDto> getPatientDocuments() { return patientDocuments; }
    public void setPatientDocuments(List<PatientDocumentDto> patientDocuments) { this.patientDocuments = patientDocuments; }
    public List<KnowledgeDocumentDto> getKnowledgeDocuments() { return knowledgeDocuments; }
    public void setKnowledgeDocuments(List<KnowledgeDocumentDto> knowledgeDocuments) { this.knowledgeDocuments = knowledgeDocuments; }
}