package de.aporz.doctorassistant.repository;

import de.aporz.doctorassistant.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientRepository extends JpaRepository<Patient, Long> {}

