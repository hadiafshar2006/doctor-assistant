package de.aporz.doctorassistant.controller;

import de.aporz.doctorassistant.entity.Patient;
import de.aporz.doctorassistant.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patients")
@Tag(name = "Patients")
@Validated
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @Operation(summary = "Create a patient")
    @PostMapping
    public ResponseEntity<Patient> create(@RequestBody Patient request) {
        return ResponseEntity.ok(patientService.create(request));
    }

    @Operation(summary = "Get a patient by id")
    @GetMapping("/{id}")
    public ResponseEntity<Patient> get(@PathVariable Long id) {
        return ResponseEntity.ok(patientService.get(id));
    }

    @Operation(summary = "List patients")
    @GetMapping
    public ResponseEntity<List<Patient>> list() {
        return ResponseEntity.ok(patientService.list());
    }

    @Operation(summary = "Update a patient")
    @PutMapping("/{id}")
    public ResponseEntity<Patient> update(@PathVariable Long id, @RequestBody Patient request) {
        return ResponseEntity.ok(patientService.update(id, request));
    }

    @Operation(summary = "Delete a patient")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        patientService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

