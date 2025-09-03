# Model Fine Tuning
## OpenSource
https://unsloth.ai/

## Propritär 
Über den jeweiligen Anbieter, sofern er es anbietet.
I.d.R. gibt es dafür eine spezielle API Endpoints und Oberflächen vom Anbieter.

# Wofür LLM?
- Menschen verstehen (was er will) -> und dann etwas sinnvolles machen
- Intelligente Entscheidungen treffen 
  Beispiel: Komplexe Anfrage von Kunden
    => Welche Wissenquellen muss ich anzapfen?
    => Welche Anfragen muss ich an die jeweilige Quelle stellen
- Menschen zu antworten

# Warum einem LLM Tools geben?


# Projekt-Ideen:
## Doctor Assistant
- Doctor Assistant (Agent) (Blutwerte, Informationen vom Patienten) "Wieviel war Blutdruck von Patient X?" "Wie hat sich der Blutwert XY verändert seit letzter Messung"

### Datenquellen
- Patienten-Dokumente
  - Großes und kleines Blutbild
- Allgemeine Patientendaten (Name, Telefon, ...)
- Notizen vom Arzt (Angstpatient, ... )
- Knowledge Database für Medizinische Informationen

### Grundsätzliches Vorgehen
- Default Weg (irgend eine Form von Wissenabfrage)
- Speziallösungen für Häufige Fälle (z.B. Blutwerte) 


### Lösungsansätze für Default Weg

Beispiel: Doktor schreibt "Wie hat sich der Blutwert XY verändert seit letzter Messung?"


Lösung A: Anfrage geht direkt an Datenbank, Ergebnisse an LLM (1 Datenbank Query, 1 LLM Aufruf)
-> Embedding aus der Eingabe bilden (Anfrage an das Embedding Model: Embedding-Model) <- ZEITAUFWAND
-> Wir geben das Embedding in die Vector Datenbank <- ZEITAUFWAND (gering)
(Es gibt übrigens auch Reranking Modelle -  kann Qualität erhöhen, kostet aber Zeit) <- ZEITAUFWAND
-> Dann geben wir die Anfrage+Ergebnisse von Vector Datenbank in -> LLM Aufruf (-- alle infomationen --) <- ZEITAUFWAND


Lösung B: Anfrage geht zuerst an LLM, dann ein oder mehrere Queries, Ergebnisse gehen an LLM (0-x Datenbank Queries, 1-2 LLM Aufrufe)
-> Wir geben die Anfrage (+ Notizen + neustes Dokument) an ein LLM und LLM kann entweder direkt antworten (wenn es die Antwort hat, oder sagt welche Queries an die Wissensdatenbanken (Mehrzahl) gestellt werden sollen
Wenn Antwort geliefert => Fertig
Wenn stattdessen Queries geliefert werden, dann pro Query -parallel:
  - Embedding aus dem Query machen
  - Anfragen Vector Datenbank  
Gesammelt aus den Ergebnissen aller Queries (ggf. hier Reranking Modell - macht mehr sinn als in Lösung A weil hier mehrere Listen an Datenbank Ergebnissen zu einer Liste geordnet werden müssen) in LLM gegeben (+ Orginal Anfrage)

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

{
  answer: string|null
  queries: {query, database, [from], [to], [order]}[]
}

Beispiel:
{
   answer: null
   queries: [
    {"Blutmessung", "PATIENT_DOCUMENTS", "2024", "NOW", "DESC"},
    {"Creatin", "PATIENT_DOCUMENTS", "2024", "NOW", "DESC"},
    {"Creatine Fitness", "MEDICAL_KNOWLEDGE"},
    (...)
   ]
}

### Alternative Schreibweisen für LLM 1 von Lösung B

<result>
  <answer />
  <requiredInformation>

    <Query query="Blutmessung" type="PATIENT_DOCUMENTS", from="2024", to="NOW", order="DESC">
    (...)

    <GetBloodValues value="creatin">
  </requiredInformation>
</result>


<result>
  <answer />
  <requiredInformation>

    <DefaultRequest>
      <query>Blutmessung</query>
      <type>PATIENT_DOCUMENTS</type>
      ...
    </DefaultRequest>

    (...)

    <BloodValueRequest>creatin</BloodValueRequest>
  </requiredInformation>
</result>

### Allgemeine Optimierungsideen

#### Im Prompt genauer erklären was das LLM tun soll 
z.B. im Prompt erklären was für Datumsformate im Query eingegeben werden können (z.B. "NOW", "YYYY-MM-DD")

#### Arzt bekommt Möglichkeit Feedback zu geben wie gut eine Antwort war
-> Wir nehmen das Feedback um das Modell zu Fine Tunen, oder den Prompt anzupassen

### Lösungsansätze für Fall "Arzt benötigt Blutwerte"

#### Motivation
Kommt häufig vor und deshalb wollen wir dass das auch sehr gut ist
=> Optimieren

#### Prompt 
Wir erklären im Prompt welche Queries für Blutdruckanfragen sinnvoll sind und worauf er achten soll und bieten Beispiele an

Beispiel-Prompt
```
Wenn du gefragt wirst wie sich Blutwerte verändert haben, dann solltest du folgende Queries produzieren:
 {"Blutmessung", "PATIENT_DOCUMENTS", "[--- Jahr vor 3 Jahren ---]", "NOW", "DESC"}
 
Wenn der Arzt Zeitangaben macht, dann berücksichtige die in den Queries. 

Beispiel: 
Der Arzt sagt, wie haben sich die Werte verändert, seit der letzten Messung letztes Jahr und seit der Messung im Januar von diesem Jahr,
dann füge das in den Query ein - Beispiel:
{"...", "PATIENT_DOCUMENTS", "2024", "2025-01", "DESC"} 


```

#### Zusätzliche Tool
{
  answer: string|null
  queries: null
  tools: ... 
}

Beispiel:
{
   answer: null
   queries: null,
   tools: ["get_blood_values"]
}

get_blood_values("value name") 

Z.B. get_blood_values("creatin")

unser Tool listet dann alle Creatin-Werte auf, z.B.
```
Dezember 2024: Creatin: 89798
Januar 2025: Creatin: 23423
```

Um das zu ermöglichen müssen wir uns überlegen wie wir an diese Werte kommen

Lösung:
Bei der Datenspeicherung (Arzt läd Dokument hoch), direkt mit einem LLM interessante Werte wie Blutwerte herausziehen und in separater Tabelle speichern
Z.B. Tabelle "blood_values"
id | patient_id | value_name | value | date
Beispiel-Eintrag:
234324 | 343 | "creatin" | 89798 | 2024-12-10 


### Fine Tuning
Wir schreiben mehrere Anfragen wo nach Blutwerten gefragt werden und schreiben Musterantworten wie das LLM nach Blutwerten fragen soll,
und dann könnten wir überprüfen ob die Antwort gut war oder nicht auf einem der folgenden Wege: 
 A) LLM-as-a-Judge: ein LLM speziell als Bewerter einsetzen um die Antwort des im Training befindlichen LLMs mit der Musterlösung zu vergleichen
 B) Ein Mensch muss es bewerten
 C) Algorithmisch???


### Feature
- Datenabfrage 

### Dateneingabe vom Doktor (wenn er Daten eingibt)
PDF => Umwandeln in Text, LLM macht tagging, z.B. Tagging als "Blutmessung"

### Datenabfrage
LLM versteht "das Creatin -> Blutwert -> Blutmessung" (Training (Finetuning), Prompten, Tool das Zusammenhang liefert (separate gepromptetes LLM)) daraufhin entscheidet LLM das es diese abfrage benötigt: -> data_query(type=Blutmessung, Customer=2345234, LIMIT=2, ORBER BY DESC)



# RAG 
Retrieval Augmented Generation

Das ist eine System das Fragen beantwortet unter Verwendung von Vector Datenbanken und LLMs

=> Wir wollen eine spezialisierte Lösung entwickeln (siehe oben)

Es gibt aber auch allgemeine Lösungen!

Die Haupt-Herausforderung bei RAGs ist allgemein: Die richtigen Informationen in der Datenbank zu finden
-> Und da ganze soll dann auch noch schnell sein
-> Korrekte Antworten liefern

Es gibt verschiedene Ansätze die diese Herausforderung lösen versuchen
- GraphRAG <- das funktioniert mit Knowledge Graphen
- LightRAG

Youtube Channel der immer wieder RAG Arten vorstellt: https://www.youtube.com/@code4AI
Z.B.: https://www.youtube.com/watch?v=JyILtjM3dCE

