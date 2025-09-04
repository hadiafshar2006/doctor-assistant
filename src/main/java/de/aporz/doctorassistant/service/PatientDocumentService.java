package de.aporz.doctorassistant.service;

import de.aporz.doctorassistant.dto.PatientDocumentDto;
import de.aporz.doctorassistant.entity.Patient;
import de.aporz.doctorassistant.repository.PatientRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.*;

@Service
public class PatientDocumentService {

    private final VectorStore patientVectorStore;
    private final PatientRepository patientRepo;
    private final JdbcTemplate jdbcTemplate;

    public PatientDocumentService(@Qualifier("patientVectorStore") VectorStore patientVectorStore,
                                 PatientRepository patientRepo,
                                 @Autowired DataSource dataSource) {
        this.patientVectorStore = patientVectorStore;
        this.patientRepo = patientRepo;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public PatientDocumentDto create(Long patientId, String content, LocalDate documentDate) {
        // Verify patient exists
        Patient patient = patientRepo.findById(patientId).orElseThrow(
            () -> new RuntimeException("Patient not found: " + patientId)
        );

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("patient_id", String.valueOf(patientId));
        metadata.put("document_date", documentDate.toString());
        
        Document doc = new Document(UUID.randomUUID().toString(), content, metadata);
        patientVectorStore.add(List.of(doc));
        
        PatientDocumentDto dto = new PatientDocumentDto();
        dto.setId(UUID.fromString(doc.getId()));
        dto.setPatientId(patientId);
        dto.setContent(content);
        dto.setDocumentDate(documentDate);
        return dto;
    }

    public List<PatientDocumentDto> listByPatient(Long patientId) {
        // Verify patient exists
        Patient patient = patientRepo.findById(patientId).orElseThrow(
            () -> new RuntimeException("Patient not found: " + patientId)
        );

        String sql = """
            SELECT id, content, 
                   (metadata->>'patient_id')::bigint as patient_id,
                   (metadata->>'document_date')::date as document_date
            FROM patient_documents_vectors
            WHERE metadata->>'patient_id' = ?
            ORDER BY (metadata->>'document_date')::date DESC
            """;
        
        return jdbcTemplate.query(sql, new Object[]{patientId.toString()}, (rs, rowNum) -> {
            PatientDocumentDto dto = new PatientDocumentDto();
            dto.setId(UUID.fromString(rs.getString("id")));
            dto.setPatientId(rs.getLong("patient_id"));
            dto.setContent(rs.getString("content"));
            dto.setDocumentDate(rs.getDate("document_date").toLocalDate());
            return dto;
        });
    }

    public PatientDocumentDto update(UUID id, String content, LocalDate documentDate) {
        // Get existing document
        String sql = """
            SELECT id, content, 
                   (metadata->>'patient_id')::bigint as patient_id,
                   (metadata->>'document_date')::date as document_date
            FROM patient_documents_vectors
            WHERE id = ?::uuid
            """;
        
        PatientDocumentDto existing = jdbcTemplate.queryForObject(sql, new Object[]{id.toString()}, (rs, rowNum) -> {
            PatientDocumentDto dto = new PatientDocumentDto();
            dto.setId(UUID.fromString(rs.getString("id")));
            dto.setPatientId(rs.getLong("patient_id"));
            dto.setContent(rs.getString("content"));
            dto.setDocumentDate(rs.getDate("document_date").toLocalDate());
            return dto;
        });
        
        if (existing == null) {
            throw new RuntimeException("Document not found: " + id);
        }
        
        // Delete old vector
        patientVectorStore.delete(List.of(id.toString()));
        
        // Add new vector with same ID
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("patient_id", String.valueOf(existing.getPatientId()));
        metadata.put("document_date", documentDate.toString());
        
        Document newDoc = new Document(id.toString(), content, metadata);
        patientVectorStore.add(List.of(newDoc));
        
        PatientDocumentDto dto = new PatientDocumentDto();
        dto.setId(id);
        dto.setPatientId(existing.getPatientId());
        dto.setContent(content);
        dto.setDocumentDate(documentDate);
        return dto;
    }

    public void delete(UUID id) {
        patientVectorStore.delete(List.of(id.toString()));
    }

    public List<Document> search(Long patientId, String query, Integer topK, LocalDate from, LocalDate to) {
        StringBuilder filter = new StringBuilder();
        filter.append("patient_id == '").append(patientId).append("' ");

        if (from != null) {
            filter.append("&& document_date >= '").append(from).append("' ");
        }
        if (to != null) {
            filter.append("&& document_date <= '").append(to).append("' ");
        }

        SearchRequest.Builder builder = SearchRequest.builder()
                .query(query)
                .topK(topK != null ? topK : 5)
                .filterExpression(filter.toString().trim());

        return patientVectorStore.similaritySearch(builder.build());
    }

}
