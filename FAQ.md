yDoctor Assistant – FAQ (Version 1.0)

Dieses FAQ konsolidiert die abgestimmten Entscheidungen und klärt typische Fragen für die Umsetzung von V1.0 gemäß Plan.md, DocumentCRUD.md und VectorSearchSetup.md.

**Scope & Architektur**
- Ziel: Orchestrierte Q&A-Pipeline über zwei Vector Stores (Patientendokumente und medizinisches Wissen) + zwei LLM-Schritte (LLM1 für Query-Planung/sofortige Antwort, LLM2 für finalen Antwortentwurf).
- Frontend: Nicht Bestandteil von V1.0.
- Datenhaltung: JPA-Entities etablieren das Schema; VectorStore für Embeddings/Ähnlichkeitssuche.

**Vector Stores & Embeddings**
- Stores: Zwei getrennte Tabellen/Stores wie im VectorSearchSetup.
- `medical_knowledge_vectors`: global/patientenunabhängig.
- `patient_documents_vectors`: patientengebunden.
- Embedding-Dimension: 768 (jina-embeddings-v2-base-de).
- Tabellenanlage: Per JPA/Hibernate (soweit möglich). Hinweis: Die PostgreSQL-Extension „vector“ kann nicht via JPA aktiviert werden; diese muss in der DB vorhanden sein.
- Keine direkte JDBC-Nutzung im App-Code; Spring AI VectorStore ist erlaubt (nutzt intern JDBC, aber Pattern bleibt JPA-zentriert).

**Dokumenttypen & IDs**
- PatientDocument (pflicht):
- Felder: `patientId` (int, verweist auf Patient), `content` (String), `documentDate` (Date)
- VectorStore-Metadaten: Keine freie Metadata-Map in der API; intern im VectorStore werden feste Keys verwendet, um Filter zu ermöglichen:
- `patient_id` (String/Number) und `document_date` (ISO-8601, z. B. 2024-01-20)
- ID: UUID für Dokumente. (Macht Spring AI automatisch)
- KnowledgeDocument (global):
- Pflichtfeld: `content` (String). (Macht Spring AI automatisch)
- ID: UUID für Dokumente. (Macht Spring AI automatisch)

**Endpoints & Verhalten**
- CRUD Endpoints:
- Patient-Dokumente: Sortierung in List-Endpoints nach `documentDate` DESC.
- Knowledge-Dokumente: Sortierung in List-Endpoints nach `createdAt` DESC.
- Update in VectorStore: Nur über delete + add.
- Keine Pagination in V1.0.
- Query Endpoint:
- Eingaben: `query` (String), `patientId` (int), optional `dateFrom`/`dateTo` (YYYY-MM-DD oder „NOW“ → in LocalDate.now() konvertieren), `order` (ASC/DESC) zur Eingrenzung der Patientendokumente.
- `topK`: Nicht per Request, ausschließlich aus Konfiguration.
- Similarity Search: Nach Relevanz; Datumsgrenzen nur Filter, keine Sortierung der Similarity-Treffer.

**Konfiguration**
- LLM- & Embedding-Modelle: Bereits in `application.properties` hinterlegt.
- Top-K je Store (nur aus Config, keine Request-Überschreibung):
- `vector.topk.patient` (int)
- `vector.topk.knowledge` (int)

**Query-Orchestrierung (LLM1 → DB → LLM2)**
- LLM1 Eingabe: Nutzerfrage + Patientenkontext (Name, Notes) + optional jüngste Dokumente.
- LLM1 Ausgabe (JSON):
- `{ answer: string|null, queries: [ { query, database, from?, to?, order? } ] }`
- `database` Enum: `PATIENT_DOCUMENTS` | `MEDICAL_KNOWLEDGE`
- Datumsformat: YYYY-MM-DD oder „NOW“ (Serverzeit)
- JSON-Policy:
- Einmaliges Re-Prompting bei ungültigem JSON.
- Danach Wurf einer unchecked Custom Runtime Exception (z. B. `InvalidLlmJsonResponseException`).
- LLM2: Bekommt Originalfrage + selektierten Kontext und liefert finale Antwort auf Deutsch.

**Prompting & Escaping**
- Prompt-Sprache: Englisch; am Prompt-Ende explizit „Antwort auf Deutsch“. Queries ebenfalls auf Deutsch; englische Fachbegriffe bleiben Englisch.
- XML-Struktur im Prompt:
- `<patient><id>…</id><name>…</name><notes>…</notes></patient>`
- `<medical_knowledge><doc id="…">…</doc>…</medical_knowledge>`
- `<patient_documents><doc id="…" date="YYYY-MM-DD">…</doc>…</patient_documents>`
- Escaping: `org.apache.commons.text.StringEscapeUtils` verwenden (Dependency `org.apache.commons:commons-text` ergänzen).

**Sicherheit & Doku**
- Authentifizierung/Anonymisierung: In V1.0 nicht vorhanden.
- API-Doku: SpringDoc/OpenAPI-Annotations ausreichend in V1.0.

**Datenbank**
- DB-URL/Credentials: Aus `application.properties` (z. B. `medical_system`). Extensions (z. B. `vector`) müssen in der DB vorhanden sein.

**Implementierungshinweise**
- Similarity-Filter Patientendokumente: `filterExpression` nutzt die festen Keys, z. B.:
- `patient_id == '<ID>'`
- `document_date >= '2024-01-01' && document_date <= '2024-12-31'`
- Order-Parameter: Gilt zur Eingrenzung (z. B. Auswahl der neuesten Dokumente vor dem Einbetten), nicht als Sortierung der Similarity-Treffer.
- Keine feste Threshold-Konfiguration in V1.0 vorgesehen; Start mit Standardwerten der Library (ggf. später konfigurierbar).



