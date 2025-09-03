package de.aporz.doctorassistant.util;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class QueryUtils {
    private QueryUtils() {}

    public static String buildPatientFilter(Long patientId, LocalDate from, LocalDate to) {
        List<String> parts = new ArrayList<>();
        parts.add("patient_id == '" + patientId + "'");
        if (from != null) parts.add("document_date >= '" + from + "'");
        if (to != null) parts.add("document_date <= '" + to + "'");
        return String.join(" && ", parts);
    }

    public static LocalDate parseDateOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        if ("NOW".equalsIgnoreCase(s)) return LocalDate.now();
        return LocalDate.parse(s);
    }
}

