package de.aporz.doctorassistant.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class QueryRequest {
    private String query;
    private Long patientId;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateFrom;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateTo;
    private String order; // ASC or DESC

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public LocalDate getDateFrom() { return dateFrom; }
    public void setDateFrom(LocalDate dateFrom) { this.dateFrom = dateFrom; }
    public LocalDate getDateTo() { return dateTo; }
    public void setDateTo(LocalDate dateTo) { this.dateTo = dateTo; }
    public String getOrder() { return order; }
    public void setOrder(String order) { this.order = order; }
}

