package de.aporz.doctorassistant.dto;

import java.time.Instant;
import java.util.UUID;

public class KnowledgeDocumentDto {
    private UUID id;
    private String content;
    private Instant createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}