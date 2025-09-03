package de.aporz.doctorassistant.controller;

import de.aporz.doctorassistant.entity.KnowledgeDocument;
import de.aporz.doctorassistant.service.KnowledgeDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/knowledge-documents")
@Tag(name = "Knowledge Documents")
public class KnowledgeDocumentController {

    private final KnowledgeDocumentService service;

    public KnowledgeDocumentController(KnowledgeDocumentService service) {
        this.service = service;
    }

    @Operation(summary = "Create a knowledge document")
    @PostMapping
    public ResponseEntity<KnowledgeDocument> create(@RequestParam String content) {
        return ResponseEntity.ok(service.create(content));
    }

    @Operation(summary = "List knowledge documents (DESC by createdAt)")
    @GetMapping
    public ResponseEntity<List<KnowledgeDocument>> list() {
        return ResponseEntity.ok(service.list());
    }

    @Operation(summary = "Delete a knowledge document")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.ok(Map.of("message", "deleted", "id", id.toString()));
    }

    @Operation(summary = "Search knowledge documents")
    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam String query,
                                    @RequestParam(required = false) Integer topK) {
        return ResponseEntity.ok(service.search(query, topK));
    }
}
