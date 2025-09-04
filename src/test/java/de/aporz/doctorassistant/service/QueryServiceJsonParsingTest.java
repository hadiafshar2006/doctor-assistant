package de.aporz.doctorassistant.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QueryServiceJsonParsingTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    @Test
    void parsesAnswerOnly() throws Exception {
        String json = "{\n  \"answer\": \"Direkte Antwort\",\n  \"queries\": null\n}";
        QueryService.Llm1Response resp = mapper.readValue(json, QueryService.Llm1Response.class);
        assertEquals("Direkte Antwort", resp.answer);
        assertNull(resp.queries);
    }

    @Test
    void parsesQueriesList() throws Exception {
        String json = "{\n  \"answer\": null,\n  \"queries\": [\n    {\n      \"query\": \"Creatin\",\n      \"database\": \"PATIENT_DOCUMENTS\",\n      \"from\": \"2024-01-01\",\n      \"to\": \"NOW\"\n    },\n    {\n      \"query\": \"Creatine Fitness\",\n      \"database\": \"MEDICAL_KNOWLEDGE\"\n    }\n  ]\n}";

        QueryService.Llm1Response resp = mapper.readValue(json, QueryService.Llm1Response.class);
        assertNull(resp.answer);
        assertNotNull(resp.queries);
        assertEquals(2, resp.queries.size());
        QueryService.QuerySpec first = resp.queries.get(0);
        assertEquals("Creatin", first.query);
        assertEquals(QueryService.Database.PATIENT_DOCUMENTS, first.database);
        assertEquals("2024-01-01", first.from);
        assertEquals("NOW", first.to);
    }
}

