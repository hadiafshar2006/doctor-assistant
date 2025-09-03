package de.aporz.doctorassistant.controller;

import de.aporz.doctorassistant.entity.Patient;
import de.aporz.doctorassistant.service.PatientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PatientController.class)
class PatientControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private PatientService patientService;

    @Test
    void listsPatients() throws Exception {
        Patient p = new Patient();
        p.setId(1L); p.setFirstName("Max"); p.setLastName("Mustermann");
        when(patientService.list()).thenReturn(List.of(p));

        mvc.perform(get("/api/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].firstName").value("Max"));
    }

    @Test
    void createsPatient() throws Exception {
        Patient p = new Patient();
        p.setId(2L); p.setFirstName("Erika"); p.setLastName("Musterfrau");
        when(patientService.create(org.mockito.ArgumentMatchers.any(Patient.class))).thenReturn(p);

        String body = "{\"firstName\":\"Erika\",\"lastName\":\"Musterfrau\"}";
        mvc.perform(post("/api/patients").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.firstName").value("Erika"));
    }
}

