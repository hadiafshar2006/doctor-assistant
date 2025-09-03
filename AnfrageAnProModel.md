Anbei erhältst du Informationen zu einem Projekt von uns. 


<PLAN>

# Projekt-Ideen: Doctor Assistant
Doctor Assistant (Agent) (Blutwerte, Informationen vom Patienten) "Wieviel war Creatin von Patient X?" "Wie hat sich der Blutwert XY verändert seit letzter Messung"

## Technologie
Wir verwenden Spring AI und PostgreSQL mit PG Vector, und das ist alles schon installiert.
Java 21

## Frontend
Das Frontend ist nicht Teil dieses Projekts und wird separat entwickelt.

## Roadmap
### Version 1.0
- Vector Search Setup
- Normale Text Eingabe
    - Endpoint zum Daten speichern (Controller)
    - Model
- Endpoint für Abfrage
    - Ablauf:
        - Controller übergibt Auftrag an Service und Service führt folgendes aus:
            - LLM 1 Antwort generieren oder sagen was es benötigt
            - Vector Abfrage mit Spring AI
            - LLM 2 Antwort generiert

### Version 1.1
Patienten-Dokumente können hochgeladen werden (in V1 einfache Text eingabe)
PDF / Wort hochladen -> umwandeln in text mit spring ai -> Chunking mit spring ai -> in vector tabelle von patienten daten (ggf. erst chunken und dann in text umwandeln? markdown statt normalem text?)


### Zukünftig
- Alle Speziallösungen und Optimierungen
- Optimierung: Dedizierte Colums anstatt Metadaten
- LLM (schnelles Modell) prüft ob die Frage beantwortet werden konnte. Und wenn nicht => Hier einen tieferen Suchprozess (tbd.) anstoßen und in der Oberfläche "Ich muss noch tiefer suchen..."
- Visuell entertainment
- Optionale Internet-Recherche machen (dazu geben wir ihm ein Internet-Recherche Werkzeug, z.B. Serper oder Brave Search zusammen mit einem Fetch tool, oder eine AI Suchmaschien wie Perplexity AI. Eine weitere Möglichkeit: OpenAI hat auch Suchmodelle und Suchwerkzeuge (bei Suchwerkzeuge benötigt man Responses API anstatt Chat-Completion))
- Beim Dokument einfügen Embedding erstellung parallel durchführen (ggf. mit API Provider)

## Architekur

tbd.

## Datenquellen
- Patienten-Dokumente => vector search tabelle
    - Großes und kleines Blutbild
- Allgemeine Patientendaten (Name, Telefon, ...)
- Notizen vom Arzt (Angstpatient, ... )
- Knowledge Database für Medizinische Informationen => vector search tabelle

### Grundsätzliches Vorgehen
- Default Weg (irgend eine Form von Wissenabfrage) <- das machen wir in version 1.0
- Speziallösungen für Häufige Fälle (z.B. Blutwerte) <- Das machen wir nicht in version 1.0

### Lösungsansätze für Default Weg

Beispiel: Doktor schreibt "Wie hat sich der Blutwert XY verändert seit letzter Messung?"

Lösung (0-x Datenbank Queries, 1-2 LLM Aufrufe):
Anfrage geht zuerst an LLM (genannt "LLM 1"), dann ein oder mehrere Queries, Ergebnisse gehen an LLM
-> Wir geben die Anfrage (+ Notizen + neustes Dokument) an ein LLM und LLM kann entweder direkt antworten (wenn es die Antwort hat, oder sagt welche Queries an die Wissensdatenbanken (Mehrzahl) gestellt werden sollen
Wenn Antwort geliefert => Fertig
Wenn stattdessen Queries geliefert werden, dann pro Query -parallel:
- Embedding aus dem Query machen
- Anfragen Vector Datenbank  
  Gesammelt aus den Ergebnissen aller Queries (ggf. hier Reranking Modell) in LLM (genannt "LLM 2") gegeben (+ Orginal Anfrage)
  LLM 2 beantwortet Frage final, oder sagt, dass es nicht die Frage beantworten kann

LLM 1:
Request:
- Anfrage
- Optimierung: ggf. Notizen
- Optimierung: ggf. neustes oder die neuste Patienten-Dokumente
  Response:
- entweder Antwort
- oder welche Queries
  pro Query es uns mitteilt:
    - Query (z.B. "Blutmessung", "Blutmessung")
    - Welche Datenbank für welchen Query
    - Zeitraum
    - Sortierung
      Abbilden mit einer Funktion:
    - get_information(query, database, [from], [to], [order])

Response-Format:
```json
{
answer: string|null
queries: {query, database, [from], [to], [order]}[]
}
```

Beispiel-Response:
```json
{
answer: null
queries: [
{"Blutmessung", "PATIENT_DOCUMENTS", "2024", "NOW", "DESC"},
{"Creatin", "PATIENT_DOCUMENTS", "2024", "NOW", "DESC"},
{"Creatine Fitness", "MEDICAL_KNOWLEDGE"},
(...)
]
}
```

Was mit der Response passiert:
Wenn wir die Response bekommen, schauen wir, ob String einen Wert enthält, bei Answer und Queries null ist.
Wenn wir nur eine Answer haben, aber keine Queries, dann wissen wir, dass wir die Antwort bekommen haben, und geben wir dann zurück. In dem Fall hat llm1 direkt die Antwort geliefert.
Wenn statt dessen Queries vorhanden sind und die Answer null ist, dann iterieren wir über die Queries und machen unsere Abfragen an die verschiedenen Datenbanken. Die Patienten-Dokumente und die Medical Knowledge sind ja Vektortabellen bei uns.

Die Ergebnisse der Vector Searches (jeder Query ein Search) werden dann in den In den User Prompt von dem Aufruf an LLM2 eingebettet, d.h. innerhalb von diesem User Prompt benutzen wir XML-Tags als Delimiter. Und den Inhalt zitieren wir natürlich entsprechend.
Alle Benutzerdaten und alle Daten aus den Datenbanken, die irgendwie in die Prompts reingegeben werden, müssen immer entsprechend gequotet werden und müssen entsprechend mit Elementen versehen werden.

In Prompts muss immer sehr genau das Ausgabeformat für die verschiedenen Situationen beschrieben werden, und es sollen auch immer Beispiele im Prompt enthalten sein.



## Herausforderungen
- Zu klären: In wieweit Spring AI's Structured Outputs Mapping über Annotationen in unserem Case funktioniert




</PLAN>


Das Vector Search Setup haben wir schon mal durch eine KI-brainstorming durchgeführt und auch unsere eigene Ideen rein gegeben. Das sieht so aus: 

<VECTOR_SEARCH_SETUP>
# Spring AI Multi-Vector Store LÃ¶sung fÃ¼r Medizinische Daten

## Architektur-Ãœbersicht

Das System nutzt zwei separate Vector Stores mit unterschiedlichen PostgreSQL-Tabellen:
- **Medizinische Wissensdatenbank** - Allgemeine medizinische Informationen
- **Patientendokumente** - Zeitgestempelte Patientendokumente mit Altersfilterung

## 1. Datenbank-Setup

### 1.1 PostgreSQL Konfiguration

**WICHTIG:** Spring AI filtert ausschlieÃŸlich Ã¼ber die `metadata` JSONB Spalte. Die generierte `document_date` Spalte dient nur der Performance-Optimierung fÃ¼r native SQL-Queries.

```sql
-- Datenbank und Extensions erstellen
CREATE DATABASE medical_system;
\c medical_system;

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Tabelle fÃ¼r medizinisches Wissen
CREATE TABLE IF NOT EXISTS medical_knowledge_vectors (
                                                         id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
                                                         content TEXT NOT NULL,
                                                         metadata JSONB,
                                                         embedding vector(768),
                                                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabelle fÃ¼r Patientendokumente
CREATE TABLE IF NOT EXISTS patient_documents_vectors (
                                                         id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
                                                         content TEXT NOT NULL,
                                                         metadata JSONB,
                                                         embedding vector(768),
                                                         document_date DATE NOT NULL,  -- Dedizierte Spalte fÃ¼r bessere Performance
                                                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indizes fÃ¼r Vector-Suche (HNSW)
CREATE INDEX idx_medical_knowledge_embedding
    ON medical_knowledge_vectors USING HNSW (embedding vector_cosine_ops);

CREATE INDEX idx_patient_documents_embedding
    ON patient_documents_vectors USING HNSW (embedding vector_cosine_ops);

-- WICHTIG: GIN-Indizes fÃ¼r Metadaten-Filterung
CREATE INDEX idx_medical_knowledge_metadata
    ON medical_knowledge_vectors USING GIN (metadata);

CREATE INDEX idx_patient_documents_metadata
    ON patient_documents_vectors USING GIN (metadata);

-- ZusÃ¤tzlicher B-Tree Index fÃ¼r Datumsfilterung
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


# Custom Properties fÃ¼r unsere Vector Stores
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
     * Vector Store fÃ¼r medizinisches Wissen
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
     * Vector Store fÃ¼r Patientendokumente
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

### 3.2 DTOs fÃ¼r Anfragen

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
    
    // Builder Pattern fÃ¼r bessere Usability
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

### 3.3 Service Layer (sehr Ã¤hnlich bei uns wird es ein DTO sein das die methoden bekommen und kein Request objekt)

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
                .similarityThreshold(0.7) // Optional: Mindest-Ã„hnlichkeit
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
     * Dokument zu medizinischem Wissen hinzufÃ¼gen
     */
    public void addMedicalKnowledge(String content, Map<String, Object> metadata) {
        Document doc = new Document(content, metadata);
        medicalVectorStore.add(List.of(doc));
    }

    /**
     * Patientendokument hinzufÃ¼gen
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
     * Endpoint fÃ¼r medizinische Wissenssuche
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
     * Endpoint fÃ¼r Patientendokumentensuche mit optionaler Datumsfilterung
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
     * POST Endpoint fÃ¼r erweiterte Patientendokumentensuche
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

### 3.5 Daten-Import Service (Daten sind hardgecoded.. sollen aber Ã¼ber Entity kommen)

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
            "Hypertonie ist definiert als dauerhaft erhÃ¶hter Blutdruck...",
            Map.of(
                "category", "Kardiologie",
                "type", "Definition",
                "icd10", "I10-I15"
            )
        );
    }
    
    private void loadPatientDocuments() {
        searchService.addPatientDocument(
            "Patient klagt Ã¼ber Kopfschmerzen seit 3 Tagen...",
            LocalDate.of(2024, 1, 15),
            Map.of(
                "patient_id", "PAT-001",
                "document_type", "Anamnese",
                "department", "Neurologie"
            )
        );
        
        searchService.addPatientDocument(
            "Laborwerte vom 20.01.2024: Blutzucker nÃ¼chtern 126 mg/dl...",
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


## 5. ZukÃ¼nftige Performance-Optimierung

### 5.1 ZusÃ¤tzliche Index-Optimierungen

```sql
-- Zusammengesetzte Indizes fÃ¼r hÃ¤ufige Abfragen
CREATE INDEX idx_patient_docs_date_metadata
    ON patient_documents_vectors (document_date, metadata);

-- Partial Index fÃ¼r aktive Dokumente
CREATE INDEX idx_patient_docs_active
    ON patient_documents_vectors (document_date)
    WHERE metadata->>'status' = 'active';

-- Index fÃ¼r spezifische Metadaten-Felder
CREATE INDEX idx_patient_docs_patient_id
    ON patient_documents_vectors ((metadata->>'patient_id'));

CREATE INDEX idx_medical_knowledge_category
    ON medical_knowledge_vectors ((metadata->>'category'));
```



</VECTOR_SEARCH_SETUP>


Ich möchte, dass du einen Architekturplan ausarbeitest.

Wichtig ist uns, dass du das System einfach hältst.

Keep it simple, stupid.

Wir wollen kein über-engineertes System. 

Wichtig ist, dass es eine sinnvolle Aufteilung in Dateien gibt und die Dateien sinnvoll in entsprechenden Unterverzeichnissen, gegebenfalls unter Unterverzeichnissen, platziert werden, so dass es auch, wenn man auf das Dateisystem blickt, eine gute Struktur ist.

Verwende bitte Best Practices, die im Spring-Umfeld gewöhnlich sind. 

Es ist auf jeden Fall JPA zu verwenden. 

Wir verwenden Controller, Repository, Service und Entity-Klassen.

Der Controller darf nicht direkt auf das Repository zugreifen, sondern der Controller übergibt die Aufgaben an den Service, und der Service setzt die Aufgaben um. Der Service darf auf das Repository zugreifen. Und nur das Repository darf auf die Datenbank zugreifen. 

Verwende englische Bezeichnungen. 


Deine Aufgabe ist es ausschließlich, einen Architekturplan zu machen.

Das heißt, du zeigst, welche der Dateien es gibt und beschreibst kurz, was in den Dateien drin ist und wie sie miteinander verknüpft sind. Du zeigst die Dateien in einer Verzeichnisbaumstruktur an.

Das ist deine Aufgabe, dass du dir überlegst, welche Dateien es geben muss, wie kommunizieren die miteinander, was muss in welcher Datei drin stehen, damit wir dieses System wie im Plan oben beschrieben umgesetzt bekommen. 





