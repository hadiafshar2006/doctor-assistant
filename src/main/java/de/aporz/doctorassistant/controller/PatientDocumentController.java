package de.aporz.doctorassistant.controller;

import de.aporz.doctorassistant.entity.PatientDocument;
import de.aporz.doctorassistant.service.PatientDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/patient-documents")
@Tag(name = "Patient Documents")
public class PatientDocumentController {

    private final PatientDocumentService service;

    public PatientDocumentController(PatientDocumentService service) {
        this.service = service;
    }

    @Operation(summary = "Create a patient document")
    @PostMapping
    public ResponseEntity<PatientDocument> create(@RequestParam Long patientId,
                                                  @RequestParam String content,
                                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate documentDate) {
        return ResponseEntity.ok(service.create(patientId, content, documentDate));
    }

    @Operation(summary = "List patient documents by patient id (DESC by date)")
    @GetMapping
    public ResponseEntity<List<PatientDocument>> list(@RequestParam Long patientId) {
        return ResponseEntity.ok(service.listByPatient(patientId));
    }

    @Operation(summary = "Update a patient document")
    @PutMapping("/{id}")
    public ResponseEntity<PatientDocument> update(@PathVariable UUID id,
                                                  @RequestParam String content,
                                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate documentDate) {
        return ResponseEntity.ok(service.update(id, content, documentDate));
    }

    @Operation(summary = "Delete a patient document")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.ok(Map.of("message", "deleted", "id", id.toString()));
    }

    @Operation(summary = "Search patient documents with optional date filter")
    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam Long patientId,
                                    @RequestParam String query,
                                    @RequestParam(required = false) Integer topK,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(service.search(patientId, query, topK, from, to));
    }
}
