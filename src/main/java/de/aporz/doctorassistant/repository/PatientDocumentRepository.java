package de.aporz.doctorassistant.repository;

import de.aporz.doctorassistant.entity.PatientDocument;
import de.aporz.doctorassistant.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PatientDocumentRepository extends JpaRepository<PatientDocument, UUID> {
    List<PatientDocument> findByPatientOrderByDocumentDateDesc(Patient patient);
}

