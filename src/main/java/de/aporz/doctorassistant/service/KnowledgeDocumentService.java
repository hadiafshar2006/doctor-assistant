package de.aporz.doctorassistant.service;

import de.aporz.doctorassistant.entity.KnowledgeDocument;
import de.aporz.doctorassistant.repository.KnowledgeDocumentRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@ConditionalOnBean(name = "medicalVectorStore")
public class KnowledgeDocumentService {

    private final VectorStore medicalVectorStore;
    private final KnowledgeDocumentRepository repo;

    public KnowledgeDocumentService(@Qualifier("medicalVectorStore") VectorStore medicalVectorStore,
                                    KnowledgeDocumentRepository repo) {
        this.medicalVectorStore = medicalVectorStore;
        this.repo = repo;
    }

    public KnowledgeDocument create(String content) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("created_at", Instant.now().toString());

        Document doc = new Document(content, metadata);
        medicalVectorStore.add(List.of(doc));

        KnowledgeDocument entity = new KnowledgeDocument();
        entity.setContent(content);
        entity.setCreatedAt(Instant.now());
        entity.setVectorId(doc.getId());
        return repo.save(entity);
    }

    public List<KnowledgeDocument> list() {
        return repo.findAllByOrderByCreatedAtDesc();
    }

    public void delete(UUID id) {
        KnowledgeDocument k = repo.findById(id).orElseThrow();
        if (k.getVectorId() != null) {
            medicalVectorStore.delete(List.of(k.getVectorId()));
        }
        repo.delete(k);
    }

    public List<Document> search(String query, Integer topK) {
        SearchRequest.Builder b = SearchRequest.builder().query(query).topK(topK != null ? topK : 5);
        return medicalVectorStore.similaritySearch(b.build());
    }
}
