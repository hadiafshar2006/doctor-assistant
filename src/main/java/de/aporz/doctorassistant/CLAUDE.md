# Doctor Assistant Package

## Package Summary
Ein KI-basierter Arztassistent, der medizinische Fragen durch eine hybride Architektur aus Large Language Models (LLMs) und Vector-Datenbanken beantwortet. Das System verwaltet Patienten, deren Dokumente und medizinisches Wissen in separaten Vector Stores für effiziente semantische Suche.

## File Listing
```
src/main/java/de/aporz/doctorassistant/
├── DoctorAssistantApplication.java
├── config/
│   ├── VectorStoreConfig.java
│   ├── VectorStoreProperties.java
│   └── VectorTopKProperties.java
├── controller/
│   ├── KnowledgeDocumentController.java
│   ├── PatientController.java
│   ├── PatientDocumentController.java
│   └── QueryController.java
├── dto/
│   ├── CreateDocumentRequest.java
│   ├── QueryRequest.java
│   └── UpdateDocumentRequest.java
├── entity/
│   ├── KnowledgeDocument.java
│   ├── Patient.java
│   └── PatientDocument.java
├── repository/
│   ├── KnowledgeDocumentRepository.java
│   ├── PatientDocumentRepository.java
│   └── PatientRepository.java
├── service/
│   ├── KnowledgeDocumentService.java
│   ├── PatientDocumentService.java
│   ├── PatientService.java
│   └── QueryService.java
└── util/
    └── QueryUtils.java
```

## File Descriptions

**DoctorAssistantApplication.java**: Spring Boot Hauptklasse mit Standard-Konfiguration für die Anwendung.

**config/VectorStoreConfig.java**: Konfiguriert zwei PgVector-Instanzen für medizinische Wissensdokumente und Patientendokumente mit COSINE_DISTANCE als Ähnlichkeitsmetrik.

**config/VectorStoreProperties.java**: Configuration Properties für Vector Store Einstellungen mit separaten Konfigurationen für Medical- und Patient-Stores (Tabellennamen und Dimensionen).

**config/VectorTopKProperties.java**: Konfiguration der TopK-Parameter für Vector-Suchen in Patient- (Standard: 5) und Knowledge-Dokumenten (Standard: 5).

**controller/QueryController.java**: REST-Controller für medizinische Anfragen mit LLM1+Vector+LLM2 Orchestrierung über `/api/query` Endpoint.

**controller/PatientController.java**: CRUD-REST-Controller für Patientenverwaltung über `/api/patients` mit Standard Create/Read/Update/Delete Operationen.

**controller/PatientDocumentController.java**: REST-Controller für Patientendokumente mit CRUD-Operationen und Vector-Search-Funktionalität über `/api/patient-documents`.

**controller/KnowledgeDocumentController.java**: REST-Controller für medizinische Wissensdokumente mit CRUD-Operationen und Suchfunktionen über `/api/knowledge-documents`.

**dto/QueryRequest.java**: Data Transfer Object für medizinische Anfragen mit Patientenfiltern und Datumsbereich-Parametern.

**dto/CreateDocumentRequest.java**: DTO für die Erstellung von Dokumenten mit Inhalt und Metadaten.

**dto/UpdateDocumentRequest.java**: DTO für die Aktualisierung von Dokumenten mit Inhalt und Metadaten.

**entity/Patient.java**: JPA-Entity für Patienten mit Grunddaten (Name, Notizen) und automatischer ID-Generierung.

**entity/PatientDocument.java**: JPA-Entity für Patientendokumente mit Referenz zum Patienten, Inhalt, Dokumentdatum und Vector Store ID.

**entity/KnowledgeDocument.java**: JPA-Entity für medizinische Wissensdokumente mit Inhalt, Erstellungszeitpunkt und Vector Store Referenz.

**repository/PatientRepository.java**: Standard JPA Repository für Patienten-Entities.

**repository/PatientDocumentRepository.java**: JPA Repository mit custom Query für Patientendokumente sortiert nach Dokumentdatum.

**repository/KnowledgeDocumentRepository.java**: JPA Repository mit Sortierung nach Erstellungsdatum für Wissensdokumente.

**service/QueryService.java**: Kernlogik der KI-Orchestrierung - koordiniert LLM-Aufrufe, Vector-Suchen und Antwortgenerierung für medizinische Anfragen.

**service/PatientService.java**: Business Logic für Patienten-CRUD-Operationen mit Repository-Delegation.

**service/PatientDocumentService.java**: Service für Patientendokumente mit synchroner Vector Store Integration und erweiterten Suchfunktionen.

**service/KnowledgeDocumentService.java**: Service für medizinische Wissensdokumente mit Vector Store Synchronisation und Suchfunktionalität.

**util/QueryUtils.java**: Utility-Klasse für Vector Store Filter-Erstellung und Datums-Parsing mit "NOW" Support.

## File Relationships

**Architektur Flow**: `QueryController` → `QueryService` → LLM1 (Anfrage-Analyse) → Vector Search (Patient/Knowledge) → LLM2 (Antwort-Generierung)

**Data Persistence**: Entities (`Patient`, `PatientDocument`, `KnowledgeDocument`) ↔ Repositories ↔ Services ↔ Controllers mit paralleler Vector Store Synchronisation

**Configuration Chain**: `VectorStoreConfig` nutzt `VectorStoreProperties` und `VectorTopKProperties` für die PgVector-Konfiguration beider Stores

**Query Processing**: `QueryService` verwendet `QueryUtils` für Filter-Erstellung und koordiniert zwischen beiden Vector Stores basierend auf LLM1-Entscheidungen

**Service Dependencies**: Document Services verwalten sowohl JPA-Entities als auch Vector Store Synchronisation, Patient Service fokussiert auf reine CRUD-Operationen

## Notable Insights

**Hybride KI-Architektur**: Das System implementiert eine dreistufige Verarbeitung: LLM1 analysiert Anfragen und plant Vector-Suchen, Vector Stores liefern relevante Kontexte, LLM2 generiert finale Antworten auf Deutsch.

**Dual Vector Store Design**: Separate Vector Stores für Patientendokumente (mit Patient-ID und Datum-Filtern) und allgemeines medizinisches Wissen ermöglichen präzise kontextuelle Suchen.

**Automatische Vector Synchronisation**: Alle Document Services halten JPA-Entities und Vector Store automatisch synchron, inklusive Update/Delete Operationen.

**Flexible Query Planning**: QueryService kann basierend auf LLM1-Entscheidungen sowohl direkt antworten als auch komplexe Multi-Database-Suchen durchführen.

**German Language Support**: Das System ist explizit für deutsche medizinische Anwendungen optimiert mit deutschen Antworten und Terminologie.

**Conditional Bean Loading**: Alle Vector-abhängigen Components nutzen `@ConditionalOnBean` für graceful Degradation bei fehlenden Vector Store Konfigurationen.

**Spring AI Integration**: Nutzt Spring AI Framework für nahtlose LLM- und Vector Store Integration mit PgVector Backend.

Verwenden Sie das devtool für Projektoperationen:
```bash
cd <project-root>
python devtool.py <command> <args>
```

## About this file
Update this file when the contents within the package change.