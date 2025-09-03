package de.aporz.doctorassistant.dto;

import java.util.Map;

public class CreateDocumentRequest {
    private String content;
    private Map<String, Object> metadata;

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}

