# Doctor Assistant

Ein KI-gestütztes medizinisches Assistenzsystem, das Ärzte bei der Abfrage und Analyse von Patientendaten unterstützt. Das System verwendet eine zwei-stufige LLM-Architektur mit Vektor-basierter Suche für präzise und kontextuelle Antworten.

## Funktionalitäten (Version 1.0)

### Kern-Features
- **Intelligente Abfragen**: Natürlichsprachliche Fragen zu Patientendaten und medizinischen Informationen
- **Zwei-LLM Workflow**: 
  - LLM1 bestimmt die Suchstrategie oder liefert direkte Antworten
  - Vektor-Suche in Patientendokumenten und medizinischer Wissensdatenbank
  - LLM2 generiert finale Antworten basierend auf abgerufenen Kontexten
- **Patientenverwaltung**: CRUD-Operationen für Patienten und deren Dokumente
- **Wissensdatenbank**: Verwaltung allgemeiner medizinischer Informationen

### Beispiel-Abfragen
- "Wieviel war Creatin von Patient X?"
- "Wie hat sich der Blutwert XY verändert seit letzter Messung?"
- "Zeige mir alle Blutbilder von Patient Y aus 2024"

## Technologie-Stack

- **Backend**: Spring Boot 3 mit Java 21
- **KI-Framework**: Spring AI für Vector-Suche und LLM-Integration
- **Datenbank**: PostgreSQL mit pgvector Extension
- **Embeddings**: Deutsche Sprachmodelle (jina/jina-embeddings-v2-base-de)
- **LLM**: Ollama (lokal) oder OpenAI (konfigurierbar)
- **API-Dokumentation**: SpringDoc OpenAPI (Swagger UI)

## Voraussetzungen

### System-Anforderungen
- **Linux**: Ubuntu 24.04+ (oder vergleichbare Distribution)
- **Windows**: Windows 10/11 (mit entsprechenden Anpassungen der Setup-Befehle)
- **Java**: OpenJDK 21 oder höher
- **RAM**: Mindestens 8GB (16GB empfohlen für lokale LLM-Modelle)
- **GPU**: Optional, aber empfohlen für bessere LLM-Performance

### Benötigte Software

#### 1. PostgreSQL mit pgvector
```bash
sudo apt update
sudo apt install postgresql postgresql-contrib postgresql-dev
sudo -u postgres psql -c "CREATE EXTENSION IF NOT EXISTS vector;"
```

#### 2. Ollama (für lokale LLM-Modelle)
```bash
curl -fsSL https://ollama.com/install.sh | sh
```

#### 3. Java Development Kit 21
```bash
sudo apt install openjdk-21-jdk
```

## Installation und Setup

### 1. Repository klonen
```bash
git clone <repository-url>
cd doctor-assistant
```

### 2. Datenbank einrichten
Die Datenbank wird automatisch mit folgenden Einstellungen erstellt:
- **Datenbankname**: `medical_system`
- **Benutzer**: `dev`
- **Passwort**: `dev`
- **Port**: 5432 (Standard PostgreSQL)

Tabellen werden automatisch via JPA erstellt (`spring.jpa.hibernate.ddl-auto=update`).

### 3. Ollama-Modelle herunterladen
```bash
ollama serve &
ollama pull jina/jina-embeddings-v2-base-de  # Embedding-Modell
ollama pull gemma3:4b                         # Chat-Modell
```

### 4. Anwendung starten
```bash
# Kompilieren
./mvnw clean compile

# Anwendung starten
./mvnw spring-boot:run
```

Die Anwendung läuft dann auf `http://localhost:8080`.

### 5. API-Dokumentation
Swagger UI ist verfügbar unter: `http://localhost:8080/swagger-ui.html`

## Konfiguration

### Wichtige Einstellungen (application.properties)
```properties
# Datenbank
spring.datasource.url=jdbc:postgresql://localhost:5432/medical_system
spring.datasource.username=dev
spring.datasource.password=dev

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false

# Vector Search Parameter
vector.topk.patient=5        # Anzahl Patient-Dokumente für Suche
vector.topk.knowledge=5      # Anzahl Knowledge-Dokumente für Suche

# AI-Modelle (Ollama lokal)
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.model=gemma3:4b
spring.ai.ollama.embedding.model=jina/jina-embeddings-v2-base-de
```

### Alternative: OpenAI-Modelle
Für die Nutzung von OpenAI anstelle von Ollama:
```properties
spring.ai.openai.api-key=your-api-key-here
spring.ai.openai.chat.model=gpt-4
spring.ai.openai.embedding.model=text-embedding-3-large
```

## Architektur

### Paket-Struktur
```
src/main/java/de/aporz/doctorassistant/
├── config/          # Konfiguration (VectorStoreConfig, Properties)
├── controller/      # REST-Controller 
├── dto/            # Data Transfer Objects
├── entity/         # JPA-Entitäten (Patient, PatientDocument, KnowledgeDocument)
├── repository/     # Spring Data JPA Repositories
├── service/        # Business Logic (QueryService, PatientService, etc.)
└── util/           # Utility-Klassen (QueryUtils)
```

### Zwei-Vektor-Store Architektur
- **Patient Documents**: Patientenspezifische Dokumente mit Filterung nach Patient-ID und Zeiträumen
- **Medical Knowledge**: Allgemeine medizinische Informationen ohne patientenspezifische Filter

## Entwicklung

### Tests ausführen
```bash
# Alle Tests
./mvnw test

# Spezifische Testklasse
./mvnw test -Dtest=QueryServiceTest

# Mit Coverage-Report
./mvnw test jacoco:report
```

### JAR erstellen
```bash
./mvnw clean package
```

## Sicherheit und Compliance

- **Lokale LLM-Modelle**: Unterstützung für vollständig lokale KI-Verarbeitung
- **Datenschutz**: Alle Patientendaten bleiben im lokalen System
- **EU AI Act**: Architektur berücksichtigt regulatorische Anforderungen
- **Datenbereinigung**: XML-Escaping für alle Benutzereingaben in Prompts

## Roadmap

### Version 1.1 (geplant)
- Dokument-Upload (PDF, Word) mit automatischem Chunking
- Erweiterte Dokumentformate und -verarbeitung

### Zukünftige Versionen
- Spezialisierte Abfrage-Optimierungen
- Internet-Recherche Integration
- Visuelle Dashboards und Reports
- Performance-Optimierungen für größere Datenmengen

## Support

Bei Problemen oder Fragen zur Installation und Konfiguration wenden Sie sich an das Entwicklungsteam.