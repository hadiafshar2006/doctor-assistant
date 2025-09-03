# Spring AI Vector Database CRUD mit PostgreSQL/pgvector

## Überblick

Dieses Dokument beschreibt, wie man mit Spring AI und PostgreSQL/pgvector einen vollständigen CRUD Controller für Vector-Dokumente implementiert.

## Abhängigkeiten

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-vector-store-pgvector</artifactId>
</dependency>
```

## Konfiguration

### application.yml
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/postgres
    username: postgres
    password: postgres
  ai:
    vectorstore:
      pgvector:
        index-type: HNSW
        distance-type: COSINE_DISTANCE
        dimensions: 1536
        initialize-schema: true
```

### PostgreSQL Setup
Erforderliche Extensions:
```sql
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
```

## Datenmodell

### Document Klasse
```java
import org.springframework.ai.document.Document;
import java.util.Map;

// Document Struktur:
// - id: String (automatisch generiert)
// - content: String (Text-Inhalt)
// - metadata: Map<String, Object> (Metadaten)
// - embedding: List<Double> (automatisch generiert)

Document document = new Document("Inhalt des Dokuments", 
    Map.of("title", "Titel", "author", "Autor"));
```

## CRUD Operationen

### 1. CREATE - Dokumente hinzufügen

```java
@RestController
@RequestMapping("/api/documents")
public class VectorDocumentController {

    @Autowired
    private VectorStore vectorStore;

    @PostMapping
    public ResponseEntity<String> createDocument(@RequestBody CreateDocumentRequest request) {
        Document document = new Document(
            request.getContent(), 
            request.getMetadata()
        );
        
        vectorStore.add(List.of(document));
        return ResponseEntity.ok("Dokument erstellt mit ID: " + document.getId());
    }

    @PostMapping("/batch")
    public ResponseEntity<String> createDocuments(@RequestBody List<CreateDocumentRequest> requests) {
        List<Document> documents = requests.stream()
            .map(req -> new Document(req.getContent(), req.getMetadata()))
            .toList();
            
        vectorStore.add(documents);
        return ResponseEntity.ok(documents.size() + " Dokumente erstellt");
    }
}
```

### 2. READ - Dokumente durchsuchen

```java
@GetMapping("/search")
public ResponseEntity<List<Document>> searchDocuments(
        @RequestParam String query,
        @RequestParam(defaultValue = "4") int topK,
        @RequestParam(required = false) Double threshold) {
    
    SearchRequest.Builder builder = SearchRequest.builder()
        .query(query)
        .topK(topK);
        
    if (threshold != null) {
        builder.similarityThreshold(threshold);
    }
    
    List<Document> results = vectorStore.similaritySearch(builder.build());
    return ResponseEntity.ok(results);
}

@GetMapping("/search/filtered")
public ResponseEntity<List<Document>> searchWithFilter(
        @RequestParam String query,
        @RequestParam String filter,
        @RequestParam(defaultValue = "4") int topK) {
    
    List<Document> results = vectorStore.similaritySearch(
        SearchRequest.builder()
            .query(query)
            .topK(topK)
            .filterExpression(filter) // z.B. "author == 'John Doe'"
            .build()
    );
    
    return ResponseEntity.ok(results);
}
```

### 3. UPDATE - Dokumente aktualisieren

```java
@PutMapping("/{id}")
public ResponseEntity<String> updateDocument(
        @PathVariable String id, 
        @RequestBody UpdateDocumentRequest request) {
    
    // Erst das alte Dokument löschen
    vectorStore.delete(List.of(id));
    
    // Dann das neue Dokument mit gleicher ID hinzufügen
    Document updatedDocument = new Document(id, request.getContent(), request.getMetadata());
    vectorStore.add(List.of(updatedDocument));
    
    return ResponseEntity.ok("Dokument " + id + " aktualisiert");
}
```

### 4. DELETE - Dokumente löschen

```java
@DeleteMapping("/{id}")
public ResponseEntity<String> deleteDocument(@PathVariable String id) {
    vectorStore.delete(List.of(id));
    return ResponseEntity.ok("Dokument " + id + " gelöscht");
}

@DeleteMapping
public ResponseEntity<String> deleteDocuments(@RequestBody List<String> ids) {
    vectorStore.delete(ids);
    return ResponseEntity.ok(ids.size() + " Dokumente gelöscht");
}

@DeleteMapping("/by-filter")
public ResponseEntity<String> deleteByFilter(@RequestParam String filter) {
    // Löschen basierend auf Metadaten-Filter
    vectorStore.delete(filter); // z.B. "author == 'John Doe'"
    return ResponseEntity.ok("Dokumente mit Filter gelöscht: " + filter);
}
```

## Request/Response DTOs

```java
public class CreateDocumentRequest {
    private String content;
    private Map<String, Object> metadata;
    
    // Getters und Setters
}

public class UpdateDocumentRequest {
    private String content;
    private Map<String, Object> metadata;
    
    // Getters und Setters
}
```

## Service Layer

```java
@Service
public class VectorDocumentService {
    
    @Autowired
    private VectorStore vectorStore;
    
    public String addDocument(String content, Map<String, Object> metadata) {
        Document document = new Document(content, metadata);
        vectorStore.add(List.of(document));
        return document.getId();
    }
    
    public List<Document> searchSimilar(String query, int topK, Double threshold) {
        SearchRequest.Builder builder = SearchRequest.builder()
            .query(query)
            .topK(topK);
            
        if (threshold != null) {
            builder.similarityThreshold(threshold);
        }
        
        return vectorStore.similaritySearch(builder.build());
    }
    
    public void deleteDocument(String id) {
        vectorStore.delete(List.of(id));
    }
    
    public void deleteByMetadata(String filterExpression) {
        vectorStore.delete(filterExpression);
    }
}
```

## Filter-Beispiele

### String-basierte Filter
```java
// Einfache Gleichheit
"author == 'John Doe'"

// Kombinierte Bedingungen
"category == 'tech' AND year > 2020"

// Listen-Operationen
"tags IN ['spring', 'java']"
```

### Programmatische Filter
```java
import static org.springframework.ai.vectorstore.filter.FilterExpressionBuilder.*;

var filterExpression = 
    and(
        eq("author", "John Doe"),
        gt("year", 2020)
    );
```

## Konfigurationsoptionen

### Index-Typen
- `NONE`: Kein Index (exakte Suche)
- `IVFFlat`: Inverted File Index
- `HNSW`: Hierarchical Navigable Small World (empfohlen)

### Distanz-Metriken
- `COSINE_DISTANCE`: Kosinus-Distanz
- `EUCLIDEAN_DISTANCE`: Euklidische Distanz
- `INNER_PRODUCT`: Inneres Produkt

## Best Practices

1. **Batch-Operationen**: Verwende `add(List<Document>)` für mehrere Dokumente
2. **Metadaten nutzen**: Füge sinnvolle Metadaten für Filterung hinzu
3. **Index-Optimierung**: Wähle den passenden Index-Typ für deine Anwendung
4. **Error Handling**: Implementiere robuste Fehlerbehandlung
5. **Performance**: Nutze `topK` und `threshold` Parameter für optimierte Suchen

## Vollständiges Controller-Beispiel

```java
@RestController
@RequestMapping("/api/vector-documents")
@Validated
public class VectorDocumentController {

    private final VectorStore vectorStore;
    
    public VectorDocumentController(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> create(@RequestBody @Valid CreateDocumentRequest request) {
        Document document = new Document(request.getContent(), request.getMetadata());
        vectorStore.add(List.of(document));
        return ResponseEntity.ok(Map.of("id", document.getId(), "message", "Dokument erstellt"));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Document>> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "4") int topK,
            @RequestParam(required = false) Double threshold,
            @RequestParam(required = false) String filter) {
        
        SearchRequest.Builder builder = SearchRequest.builder()
            .query(query)
            .topK(topK);
            
        if (threshold != null) {
            builder.similarityThreshold(threshold);
        }
        
        if (filter != null) {
            builder.filterExpression(filter);
        }
        
        List<Document> results = vectorStore.similaritySearch(builder.build());
        return ResponseEntity.ok(results);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable String id) {
        vectorStore.delete(List.of(id));
        return ResponseEntity.ok(Map.of("message", "Dokument gelöscht", "id", id));
    }

    @DeleteMapping
    public ResponseEntity<Map<String, String>> deleteByFilter(@RequestParam String filter) {
        vectorStore.delete(filter);
        return ResponseEntity.ok(Map.of("message", "Dokumente gelöscht", "filter", filter));
    }
}
```

## Testen mit Docker

```yaml
# docker-compose.yml
version: '3.8'
services:
  postgres:
    image: pgvector/pgvector:pg16
    environment:
      POSTGRES_DB: vectordb
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```