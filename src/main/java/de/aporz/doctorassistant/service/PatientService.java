package de.aporz.doctorassistant.service;

import de.aporz.doctorassistant.entity.Patient;
import de.aporz.doctorassistant.repository.PatientRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PatientService {
    private final PatientRepository patientRepository;

    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public Patient create(Patient p) { return patientRepository.save(p); }

    public Patient get(Long id) { return patientRepository.findById(id).orElseThrow(); }

    public List<Patient> list() { return patientRepository.findAll(); }

    public Patient update(Long id, Patient update) {
        Patient p = get(id);
        p.setFirstName(update.getFirstName());
        p.setLastName(update.getLastName());
        p.setNotes(update.getNotes());
        return patientRepository.save(p);
    }

    public void delete(Long id) { patientRepository.deleteById(id); }
}

