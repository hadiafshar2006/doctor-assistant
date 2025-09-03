package de.aporz.doctorassistant.util;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class QueryUtilsTest {

    @Test
    void buildsFilterWithAllParts() {
        LocalDate from = LocalDate.of(2024,1,1);
        LocalDate to = LocalDate.of(2024,12,31);
        String f = QueryUtils.buildPatientFilter(42L, from, to);
        assertEquals("patient_id == '42' && document_date >= '2024-01-01' && document_date <= '2024-12-31'", f);
    }

    @Test
    void parseNowAndIso() {
        assertNotNull(QueryUtils.parseDateOrNull("NOW"));
        assertEquals(LocalDate.of(2024,1,20), QueryUtils.parseDateOrNull("2024-01-20"));
        assertNull(QueryUtils.parseDateOrNull(null));
    }
}

