package de.aporz.doctorassistant.dto;

import java.time.LocalDate;
import java.util.UUID;

public class PatientDocumentDto {
    private UUID id;
    private Long patientId;
    private String content;
    private LocalDate documentDate;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDate getDocumentDate() { return documentDate; }
    public void setDocumentDate(LocalDate documentDate) { this.documentDate = documentDate; }
}