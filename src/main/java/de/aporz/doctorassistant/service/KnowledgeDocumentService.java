package de.aporz.doctorassistant.service;

import de.aporz.doctorassistant.dto.KnowledgeDocumentDto;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class KnowledgeDocumentService {

    private final VectorStore medicalVectorStore;
    private final JdbcTemplate jdbcTemplate;

    public KnowledgeDocumentService(@Qualifier("medicalVectorStore") VectorStore medicalVectorStore,
                                    @Autowired DataSource dataSource) {
        this.medicalVectorStore = medicalVectorStore;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public KnowledgeDocumentDto create(String content) {
        Map<String, Object> metadata = new HashMap<>();
        Instant now = Instant.now();
        metadata.put("created_at", now.toString());

        Document doc = new Document(UUID.randomUUID().toString(), content, metadata);
        medicalVectorStore.add(List.of(doc));

        KnowledgeDocumentDto dto = new KnowledgeDocumentDto();
        dto.setId(UUID.fromString(doc.getId()));
        dto.setContent(content);
        dto.setCreatedAt(now);
        return dto;
    }

    public List<KnowledgeDocumentDto> list() {
        String sql = """
            SELECT id, content, 
                   (metadata->>'created_at')::timestamp as created_at
            FROM medical_knowledge_vectors
            ORDER BY (metadata->>'created_at')::timestamp DESC
            """;
        
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            KnowledgeDocumentDto dto = new KnowledgeDocumentDto();
            dto.setId(UUID.fromString(rs.getString("id")));
            dto.setContent(rs.getString("content"));
            dto.setCreatedAt(rs.getTimestamp("created_at").toInstant());
            return dto;
        });
    }

    public void delete(UUID id) {
        medicalVectorStore.delete(List.of(id.toString()));
    }

    public List<Document> search(String query, Integer topK) {
        SearchRequest.Builder b = SearchRequest.builder().query(query).topK(topK != null ? topK : 5);
        return medicalVectorStore.similaritySearch(b.build());
    }
}
