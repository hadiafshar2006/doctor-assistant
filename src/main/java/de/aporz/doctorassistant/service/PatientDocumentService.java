package de.aporz.doctorassistant.service;

import de.aporz.doctorassistant.entity.Patient;
import de.aporz.doctorassistant.entity.PatientDocument;
import de.aporz.doctorassistant.repository.PatientDocumentRepository;
import de.aporz.doctorassistant.repository.PatientRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@ConditionalOnBean(name = "patientVectorStore")
public class PatientDocumentService {

    private final VectorStore patientVectorStore;
    private final PatientDocumentRepository docRepo;
    private final PatientRepository patientRepo;

    public PatientDocumentService(@Qualifier("patientVectorStore") VectorStore patientVectorStore,
                                  PatientDocumentRepository docRepo,
                                  PatientRepository patientRepo) {
        this.patientVectorStore = patientVectorStore;
        this.docRepo = docRepo;
        this.patientRepo = patientRepo;
    }

    public PatientDocument create(Long patientId, String content, LocalDate documentDate) {
        Patient patient = patientRepo.findById(patientId).orElseThrow();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("patient_id", String.valueOf(patientId));
        metadata.put("document_date", documentDate.toString());

        Document doc = new Document(content, metadata);
        patientVectorStore.add(List.of(doc));

        PatientDocument entity = new PatientDocument();
        entity.setPatient(patient);
        entity.setContent(content);
        entity.setDocumentDate(documentDate);
        entity.setVectorId(doc.getId());
        return docRepo.save(entity);
    }

    public List<PatientDocument> listByPatient(Long patientId) {
        Patient patient = patientRepo.findById(patientId).orElseThrow();
        return docRepo.findByPatientOrderByDocumentDateDesc(patient);
    }

    public PatientDocument update(UUID id, String content, LocalDate documentDate) {
        PatientDocument existing = docRepo.findById(id).orElseThrow();
        // delete old vector
        if (existing.getVectorId() != null) {
            patientVectorStore.delete(List.of(existing.getVectorId()));
        }
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("patient_id", String.valueOf(existing.getPatient().getId()));
        metadata.put("document_date", documentDate.toString());
        Document newDoc = new Document(content, metadata);
        patientVectorStore.add(List.of(newDoc));

        existing.setContent(content);
        existing.setDocumentDate(documentDate);
        existing.setVectorId(newDoc.getId());
        return docRepo.save(existing);
    }

    public void delete(UUID id) {
        PatientDocument existing = docRepo.findById(id).orElseThrow();
        if (existing.getVectorId() != null) {
            patientVectorStore.delete(List.of(existing.getVectorId()));
        }
        docRepo.delete(existing);
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
