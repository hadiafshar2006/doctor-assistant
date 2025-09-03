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
- Eingegebene Dokumente um Personenbezogene Daten bereinigen (fuzzy search)

## Sicherheits-Gedanken
EU AI Act im Auge behalten
deshalb lokale Modelle verwenden
Wenn System wenig last hat (bzw. sporadische Last) und das LLM Modell klein genug ist, dann kann Consumer Grafikkarte mit schnellem Speicher und ausreichen RAM (z.B. RTX 5090) eine gute Wahl sein


## Architektur Plan

DoctorAssistantApplication.java

controller
- PatientController
  + Request/Response Objekte
  nutzt PatientService
- QueryController.java (Abfrage Endpoint)
  + Request/Response Objekte
  1 Abfrage Endpoing
  Kommuniziert mit
- PatientDocumentController.java (PatientDokumenten CRUD)
  + Request/Response Objekte (siehe DocumentCRUD.md )
  kommuniziert mit PatientDocumentService
- KnowledgeDocumentController.java (KnowledgeDokumenten CRUD)
  + Request/Response Objekte (siehe DocumentCRUD.md )
  kommuniziert mit KnowledgeDocumentService
    
  # Hinweis: in Version 1.0 sind PatientDocumentController und KnowledgeDocumentController weitestgehend identisch.. in Version 1.1 dagegen wird der PatientDocumentController Dateiuploads unterstützen mit verschiedenen Dokument-Arten (PDF, ...). In Version 1 ist das nur reiner Text.

entity
- PatientDocument (ist verknüpft mit Patient)
- KnowledgeDocument
- Patient (id:int(auto-increment), firstName:varchar, lastName:varchar, notes:text)

service
- PatientService (CRUD über PatientRepository)
- PatientDocumentService (CRUD über PatientDocumentRepository + Abfrage-Methode die Springs similarSearch API nutzt)
- KnowledgeDocumentService (CRUD über PatientDocumentRepository + Abfrage-Methode die Springs similarSearch API nutzt)
- QueryService (Verarbeitet eine Anfrage komplett wie in unserem Plan beschrieben)
  + LLM1Response für das Ergebnis von LLM1 Aufruf (answer + tools ... beide optional aber einer von beiden muss gesetzt sein) möglw. als record?
  (LLM2 gibt nur string zurück)
  nutzt die Abfrage-Methoden von PatientDocumentService und KnowledgeDocumentService
  nutzt die patient.notes (übergibt das an den prompt)
  nutzt patient.firstName + " " + patient.lastName

repository
- PatientRepository
- PatientDocumentRepository
- KnowledgeDocumentRepository


Architektur Regeln: 
- Nur Repository darf mit Datenbank reden (Ausnahme: similarSearch bzw. API Methoden von Spring AI)
- Nur Service darf mit Repository sprechen 
- Nutze SpringDoc Annotationen damit wir über die bereits installierte springdoc-openapi-starter-webmvc-ui dependency eine Swagger Oberfläche für die Endpoints haben 

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
- Zu klären (z.B. via Websearch): In wieweit Spring AI's Structured Outputs Mapping über Annotationen in unserem Case funktioniert

## Wichtige Dokumente (BITTE ZUERST LESEN!!!)
DocumentCRUD.md
VectorSearchSetup.md

## Wichtig
- Kein Flyway oder Liquidbase verwenden
- Kein JDBC verwenden, sondern JPA verwenden (darüber auch die Tabellen erstellen)
- Nutze Websearch wenn Spring AI API nicht funktioniert wie erwartet um die korrekte API zu erfahren
- Kläre die Herausforderungen via Websearch an passender Stelle

SUPERWICHTIG: Wenn an irgend einer Stelle etwas nicht möglich ist - z.B. du der Meinung bist das es JDBC benötigt - dann halte an und Frage nach was du tun sollst