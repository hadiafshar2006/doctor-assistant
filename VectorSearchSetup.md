# Spring AI Multi-Vector Store Lösung für Medizinische Daten

## Architektur-Übersicht

Das System nutzt zwei separate Vector Stores mit unterschiedlichen PostgreSQL-Tabellen:
- **Medizinische Wissensdatenbank** - Allgemeine medizinische Informationen
- **Patientendokumente** - Zeitgestempelte Patientendokumente mit Altersfilterung

## 1. Datenbank-Setup

### 1.1 PostgreSQL Konfiguration

**WICHTIG:** Spring AI filtert ausschließlich über die `metadata` JSONB Spalte. Die generierte `document_date` Spalte dient nur der Performance-Optimierung für native SQL-Queries.

```sql
-- Datenbank und Extensions erstellen
CREATE DATABASE medical_system;
\c medical_system;

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Tabelle für medizinisches Wissen
CREATE TABLE IF NOT EXISTS medical_knowledge_vectors (
                                                         id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
                                                         content TEXT NOT NULL,
                                                         metadata JSONB,
                                                         embedding vector(768),
                                                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabelle für Patientendokumente
CREATE TABLE IF NOT EXISTS patient_documents_vectors (
                                                         id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
                                                         content TEXT NOT NULL,
                                                         metadata JSONB,
                                                         embedding vector(768),
                                                         document_date DATE NOT NULL,  -- Dedizierte Spalte für bessere Performance
                                                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indizes für Vector-Suche (HNSW)
CREATE INDEX idx_medical_knowledge_embedding
    ON medical_knowledge_vectors USING HNSW (embedding vector_cosine_ops);

CREATE INDEX idx_patient_documents_embedding
    ON patient_documents_vectors USING HNSW (embedding vector_cosine_ops);

-- WICHTIG: GIN-Indizes für Metadaten-Filterung
CREATE INDEX idx_medical_knowledge_metadata
    ON medical_knowledge_vectors USING GIN (metadata);

CREATE INDEX idx_patient_documents_metadata
    ON patient_documents_vectors USING GIN (metadata);

-- Zusätzlicher B-Tree Index für Datumsfilterung
CREATE INDEX idx_patient_documents_date
    ON patient_documents_vectors (document_date);
```

## 2. Spring Boot Konfiguration

### 2.1 Dependencies (pom.xml)

```xml
<dependencies>
    <!-- Spring Boot Starter -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <!-- Spring AI PgVector -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-pgvector-store-spring-boot-starter</artifactId>
    </dependency>
    
    <!-- Spring AI OpenAI (oder andere Embedding-Modelle) -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
    </dependency>
    
    <!-- PostgreSQL -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>
    
    <!-- Spring Boot JDBC -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-jdbc</artifactId>
    </dependency>
</dependencies>
```

### 2.2 Application Properties

```yaml


# Custom Properties für unsere Vector Stores
vector-store:
  medical:
    table-name: medical_knowledge_vectors
    dimension: 768
  patient:
    table-name: patient_documents_vectors
    dimension: 768
```

## 3. Java Implementation

### 3.1 Vector Store Konfiguration

```java
package com.medical.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class VectorStoreConfig {

    @Value("${vector-store.medical.table-name}")
    private String medicalTableName;

    @Value("${vector-store.patient.table-name}")
    private String patientTableName;

    @Value("${vector-store.medical.dimension}")
    private int dimension;

    /**
     * Vector Store für medizinisches Wissen
     */
    @Bean
    @Qualifier("medicalVectorStore")
    public VectorStore medicalVectorStore(
            JdbcTemplate jdbcTemplate,
            EmbeddingModel embeddingModel) {

        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .vectorTableName(medicalTableName)
                .schemaName("public")
                .dimensions(dimension)
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .initializeSchema(false) // Wir verwenden eigene Tabellen
                .build();
    }

    /**
     * Vector Store für Patientendokumente
     */
    @Bean
    @Qualifier("patientVectorStore")
    public VectorStore patientVectorStore(
            JdbcTemplate jdbcTemplate,
            EmbeddingModel embeddingModel) {

        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .vectorTableName(patientTableName)
                .schemaName("public")
                .dimensions(dimension)
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .initializeSchema(false) // Wir verwenden eigene Tabellen
                .build();
    }
}
```

### 3.2 DTOs für Anfragen

```java
package com.medical.dto;

import java.time.LocalDate;
import java.util.Optional;

public class PatientDocumentSearchRequest {
    private String query;
    private Optional<LocalDate> dateFrom = Optional.empty();
    private Optional<LocalDate> dateTo = Optional.empty();
    private int topK = 5;
    
    // Konstruktoren
    public PatientDocumentSearchRequest() {}
    
    public PatientDocumentSearchRequest(String query, int topK) {
        this.query = query;
        this.topK = topK;
    }
    
    // Builder Pattern für bessere Usability
    public static class Builder {
        private String query;
        private LocalDate dateFrom;
        private LocalDate dateTo;
        private int topK = 5;
        
        public Builder query(String query) {
            this.query = query;
            return this;
        }
        
        public Builder dateFrom(LocalDate dateFrom) {
            this.dateFrom = dateFrom;
            return this;
        }
        
        public Builder dateTo(LocalDate dateTo) {
            this.dateTo = dateTo;
            return this;
        }
        
        public Builder topK(int topK) {
            this.topK = topK;
            return this;
        }
        
        public PatientDocumentSearchRequest build() {
            PatientDocumentSearchRequest request = new PatientDocumentSearchRequest();
            request.query = this.query;
            request.dateFrom = Optional.ofNullable(this.dateFrom);
            request.dateTo = Optional.ofNullable(this.dateTo);
            request.topK = this.topK;
            return request;
        }
    }
    
    // Getters und Setters
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public Optional<LocalDate> getDateFrom() { return dateFrom; }
    public void setDateFrom(Optional<LocalDate> dateFrom) { this.dateFrom = dateFrom; }
    public Optional<LocalDate> getDateTo() { return dateTo; }
    public void setDateTo(Optional<LocalDate> dateTo) { this.dateTo = dateTo; }
    public int getTopK() { return topK; }
    public void setTopK(int topK) { this.topK = topK; }
}
```

### 3.3 Service Layer (sehr ähnlich bei uns wird es ein DTO sein das die methoden bekommen und kein Request objekt)

```java
package com.medical.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import com.medical.dto.PatientDocumentSearchRequest;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class MedicalSearchService {

    private final VectorStore medicalVectorStore;
    private final VectorStore patientVectorStore;

    @Autowired
    public MedicalSearchService(
            @Qualifier("medicalVectorStore") VectorStore medicalVectorStore,
            @Qualifier("patientVectorStore") VectorStore patientVectorStore) {
        this.medicalVectorStore = medicalVectorStore;
        this.patientVectorStore = patientVectorStore;
    }

    /**
     * Suche in medizinischem Wissen - nur mit topK
     */
    public List<Document> searchMedicalKnowledge(String query, int topK) {
        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(0.7) // Optional: Mindest-Ähnlichkeit
                .build();

        return medicalVectorStore.similaritySearch(searchRequest);
    }

    /**
     * Suche in Patientendokumenten mit optionaler Datumsfilterung
     */
    public List<Document> searchPatientDocuments(PatientDocumentSearchRequest request) {
        SearchRequest.Builder searchBuilder = SearchRequest.builder()
                .query(request.getQuery())
                .topK(request.getTopK())
                .similarityThreshold(0.6);

        // Datumsfilter aufbauen
        String filterExpression = buildDateFilterExpression(
                request.getDateFrom().orElse(null),
                request.getDateTo().orElse(null)
        );

        if (filterExpression != null) {
            searchBuilder.filterExpression(filterExpression);
        }

        return patientVectorStore.similaritySearch(searchBuilder.build());
    }

    /**
     * Hilfsmethode zum Aufbau des Datumsfilters
     */
    private String buildDateFilterExpression(LocalDate dateFrom, LocalDate dateTo) {
        if (dateFrom == null && dateTo == null) {
            return null;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

        if (dateFrom != null && dateTo != null) {
            return String.format("document_date >= '%s' && document_date <= '%s'",
                    dateFrom.format(formatter),
                    dateTo.format(formatter));
        } else if (dateFrom != null) {
            return String.format("document_date >= '%s'",
                    dateFrom.format(formatter));
        } else {
            return String.format("document_date <= '%s'",
                    dateTo.format(formatter));
        }
    }

    /**
     * Dokument zu medizinischem Wissen hinzufügen
     */
    public void addMedicalKnowledge(String content, Map<String, Object> metadata) {
        Document doc = new Document(content, metadata);
        medicalVectorStore.add(List.of(doc));
    }

    /**
     * Patientendokument hinzufügen
     */
    public void addPatientDocument(String content, LocalDate documentDate,
                                   Map<String, Object> additionalMetadata) {
        // Metadaten mit Datum anreichern
        Map<String, Object> metadata = new java.util.HashMap<>(additionalMetadata);
        metadata.put("document_date", documentDate.toString());

        Document doc = new Document(content, metadata);
        patientVectorStore.add(List.of(doc));
    }
}
```

### 3.4 Controller 

```java
package com.medical.controller;

import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.medical.service.MedicalSearchService;
import com.medical.dto.PatientDocumentSearchRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/search")
public class MedicalSearchController {
    
    @Autowired
    private MedicalSearchService searchService;
    
    /**
     * Endpoint für medizinische Wissenssuche
     */
    @GetMapping("/medical")
    public ResponseEntity<SearchResponse> searchMedicalKnowledge(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int topK) {
        
        List<Document> results = searchService.searchMedicalKnowledge(query, topK);
        
        return ResponseEntity.ok(new SearchResponse(
            query,
            results.size(),
            results.stream()
                .map(doc -> new DocumentResult(
                    doc.getContent(),
                    doc.getMetadata()
                ))
                .collect(Collectors.toList())
        ));
    }
    
    /**
     * Endpoint für Patientendokumentensuche mit optionaler Datumsfilterung
     */
    @GetMapping("/patient-documents")
    public ResponseEntity<SearchResponse> searchPatientDocuments(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int topK,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        
        PatientDocumentSearchRequest request = PatientDocumentSearchRequest.Builder()
            .query(query)
            .topK(topK)
            .dateFrom(dateFrom)
            .dateTo(dateTo)
            .build();
            
        List<Document> results = searchService.searchPatientDocuments(request);
        
        return ResponseEntity.ok(new SearchResponse(
            query,
            results.size(),
            results.stream()
                .map(doc -> new DocumentResult(
                    doc.getContent(),
                    doc.getMetadata()
                ))
                .collect(Collectors.toList())
        ));
    }
    
    /**
     * POST Endpoint für erweiterte Patientendokumentensuche
     */
    @PostMapping("/patient-documents/advanced")
    public ResponseEntity<SearchResponse> advancedPatientSearch(
            @RequestBody PatientDocumentSearchRequest request) {
        
        List<Document> results = searchService.searchPatientDocuments(request);
        
        return ResponseEntity.ok(new SearchResponse(
            request.getQuery(),
            results.size(),
            results.stream()
                .map(doc -> new DocumentResult(
                    doc.getContent(),
                    doc.getMetadata()
                ))
                .collect(Collectors.toList())
        ));
    }
    
    // Response DTOs
    public static class SearchResponse {
        private String query;
        private int resultCount;
        private List<DocumentResult> documents;
        
        public SearchResponse(String query, int resultCount, List<DocumentResult> documents) {
            this.query = query;
            this.resultCount = resultCount;
            this.documents = documents;
        }
        
        // Getters
        public String getQuery() { return query; }
        public int getResultCount() { return resultCount; }
        public List<DocumentResult> getDocuments() { return documents; }
    }
    
    public static class DocumentResult {
        private String content;
        private Map<String, Object> metadata;
        
        public DocumentResult(String content, Map<String, Object> metadata) {
            this.content = content;
            this.metadata = metadata;
        }
        
        // Getters
        public String getContent() { return content; }
        public Map<String, Object> getMetadata() { return metadata; }
    }
}
```

### 3.5 Daten-Import Service (Daten sind hardgecoded.. sollen aber über Entity kommen)

```java
package com.medical.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.Map;

@Service
public class DataInitializationService {
    
    @Autowired
    private MedicalSearchService searchService;
    
    @PostConstruct
    public void initializeData() {
        // Beispiel: Medizinisches Wissen laden
        loadMedicalKnowledge();
        
        // Beispiel: Patientendokumente laden
        loadPatientDocuments();
    }
    
    private void loadMedicalKnowledge() {
        searchService.addMedicalKnowledge(
            "Diabetes mellitus ist eine Stoffwechselerkrankung...",
            Map.of(
                "category", "Endokrinologie",
                "type", "Definition",
                "icd10", "E10-E14"
            )
        );
        
        searchService.addMedicalKnowledge(
            "Hypertonie ist definiert als dauerhaft erhöhter Blutdruck...",
            Map.of(
                "category", "Kardiologie",
                "type", "Definition",
                "icd10", "I10-I15"
            )
        );
    }
    
    private void loadPatientDocuments() {
        searchService.addPatientDocument(
            "Patient klagt über Kopfschmerzen seit 3 Tagen...",
            LocalDate.of(2024, 1, 15),
            Map.of(
                "patient_id", "PAT-001",
                "document_type", "Anamnese",
                "department", "Neurologie"
            )
        );
        
        searchService.addPatientDocument(
            "Laborwerte vom 20.01.2024: Blutzucker nüchtern 126 mg/dl...",
            LocalDate.of(2024, 1, 20),
            Map.of(
                "patient_id", "PAT-002",
                "document_type", "Laborbericht",
                "department", "Labor"
            )
        );
    }
}
```


## 5. Zukünftige Performance-Optimierung

### 5.1 Zusätzliche Index-Optimierungen

```sql
-- Zusammengesetzte Indizes für häufige Abfragen
CREATE INDEX idx_patient_docs_date_metadata
    ON patient_documents_vectors (document_date, metadata);

-- Partial Index für aktive Dokumente
CREATE INDEX idx_patient_docs_active
    ON patient_documents_vectors (document_date)
    WHERE metadata->>'status' = 'active';

-- Index für spezifische Metadaten-Felder
CREATE INDEX idx_patient_docs_patient_id
    ON patient_documents_vectors ((metadata->>'patient_id'));

CREATE INDEX idx_medical_knowledge_category
    ON medical_knowledge_vectors ((metadata->>'category'));
```





