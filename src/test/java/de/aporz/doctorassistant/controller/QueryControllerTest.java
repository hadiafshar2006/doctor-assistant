package de.aporz.doctorassistant.controller;

import de.aporz.doctorassistant.dto.QueryRequest;
import de.aporz.doctorassistant.dto.QueryResponse;
import de.aporz.doctorassistant.service.QueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QueryController.class)
class QueryControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private QueryService queryService;

    @Test
    void postsQuery() throws Exception {
        QueryResponse response = new QueryResponse();
        response.setAnswer("Antwort");
        response.setPatientDocuments(new ArrayList<>());
        response.setKnowledgeDocuments(new ArrayList<>());
        
        when(queryService.handle(any(QueryRequest.class))).thenReturn(response);
        String body = "{\"query\":\"Wert?\",\"patientId\":1}";
        mvc.perform(post("/api/query").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Antwort"))
                .andExpect(jsonPath("$.patientDocuments").isArray())
                .andExpect(jsonPath("$.knowledgeDocuments").isArray());
    }
}

