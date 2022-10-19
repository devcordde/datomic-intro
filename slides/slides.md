---
theme: default
layout: cover
background: https://source.unsplash.com/collection/94734566/1920x1080
highlighter: shiki
lineNumbers: false
info: Presentation on Datomic and Datalog for the DevCord Discord server
drawings:
  persist: false
css: unocss
title: Kein SQL Mehr
---

# Kein SQL Mehr

Eine Einführung in Datomic-Style-Datenbanken


---

<style>
li {
  @apply text-2xl;
}
</style>

# Inhalt

1. Was wir von Datenbanken wollen
2. Die aktuelle Datenbanklandschaft
3. Die Fakten-Datenbank
4. Datalog als Query-Sprache
7. Weiterführend

---

# Was wir von Datenbanken wollen

<style>
li {
  @apply text-2xl;
}
</style>

<v-clicks>

- Effiziente Datenspeicherung
- Möglichst der echten Welt treue Modellierung der Daten (Kapazitätserhaltung)
- Einfache Änderung der Modelle (Flexibilität)
- Übersichtliche APIs
- Gute Integration in unseren Anwendungen
- Konsistenz

</v-clicks>


---
layout: statement
---

# Klingt sinnvoll, oder?

---

# Die aktuelle Datenbanklandschaft

## Wie schlägt sich SQL?

<style>
li {
  @apply text-2xl;
}

h2 {
  text-align: left;
}
</style>

<p> 
<v-clicks>

- <twemoji-slightly-smiling-face /> Effizienz
- <twemoji-neutral-face /> Kapazitätserhaltung
- <twemoji-confused-face /> Flexibilität
- <twemoji-worried-face /> Übersichtlichkeit
- <twemoji-tired-face /> Gute Integration in unseren Anwendungen
- <twemoji-face-with-spiral-eyes /> Konsistenz


</v-clicks>
</p>

---

# Die aktuelle Datenbanklandschaft

<style>
li {
  @apply text-2xl;
}

h2 {
  text-align: left;
}
</style>

## Und was ist mit populären NoSQL-Datenbanken?

<p>
<v-clicks>


- MongoDB? Angenehme APIs und Integration, aber limitierte Modellierung 
- Apache Cassandra, Neo4J? Kompletter Overkill für die meisten Anwendungen, kompliziert
- Key-Value-Datenbanken sind offensichtlich nicht immer ausreichend...
- Konsistenz bei fast allen: <twemoji-zipper-mouth-face />


</v-clicks>
</p>

---
layout: full
---

# Die Fakten-Datenbank

<style>

.shiki {
  margin: auto;
  margin-top: 10em;
  transform: scale(2);
}

.label {
  position: absolute;
  top: 12em;
  @apply text-3xl;
}

.label-1 {
  left: 7.5em;
  @apply text-red-600;
}

.label-2 {
  left: 13.5em;
  @apply text-green-600;
}

.label-3 {
  left: 22em;
  @apply text-blue-600;
}
</style>
```clj
[1234 :person/name "Jane Doe"]
```

<v-click>
<arrow x1="260" y1="360" x2="325" y2="250" width="2" />
<span class="label-1 label">Entity</span>

</v-click>

<v-click>
<arrow x1="455" y1="360" x2="455" y2="250" width="2" />
<span class="label-2 label">Attribute</span>
</v-click> 

<v-click>
<arrow x1="685" y1="360" x2="620" y2="250" width="2" />
<span class="label-3 label">Value</span>
</v-click>

---

<style>
img {
  max-height: 90%;
  width: auto;
  margin: auto;
}
</style>

# Die Fakten-Datenbank

<img src="/sample-graph.png" class="rounded" />

---

# Die Fakten-Datenbank

<style>
li {
  @apply text-xl;
}
code {
  @apply text-xl;
}
</style>

<v-clicks>

- Die Datenbank ist eine Sammlung von Fakten über Entitäten
- Wir können an einem bestimmten Zeitpunkt\
**den unveränderlichen Wert** der Datenbank auslesen
- Fakten können hinzugefügt oder widerrufen werden (Transaktionen)
- Informationen über Transaktionen sind selbst Fakten...
- ...Wodurch die Analyse "vergangener" Werte der Datenbank möglich ist (Zeitreise!)

</v-clicks>

<v-click>


```java {all|1|2|3|4|5|6,3|5}
var conn = connectToDatabase(config);
Datahike.transact(conn, setJaneDoesAgeTo25Tx);
var db1 = Datahike.dConn(conn); // aktuellen Datenbank-Wert holen
var age1 = Datahike.q(findJaneDoesAgeQuery, db1); // 25
Datahike.transact(conn, setJaneDoesAgeTo99Tx);
var age2 = Datahike.q(findJaneDoesAgeQuery, db1); // immer noch 25
```


</v-click>



---

# Die Fakten-Datenbank

#### Wie füge ich einen Fakt hinzu?

```clj
[:db/add 1234 :person/name "Jane Doe"]
```


<v-click> 

#### Wie widerrufe ich einen Fakt?

```clj 
[:db/retract 1234 :person/name "Jane Doe"]
```

</v-click>

<v-click>

#### Wie erstelle ich eine neue Entität?

```clj
[[:db/add "temp-id" :person/name "Jane Doe"]
 [:db/add "temp-id" :person/email "jane.doe@example.com"]
 [:db/add "temp-id" :person/birthday #inst "1997-10-08"]]
```

</v-click>

<v-click>

#### Alternative Notation für mehrere Fakten über dieselbe Entität:

```clj {all|2-4|1}
{:db/id ...
 :person/name "Jane Doe"
 :person/email "jane.doe@example.com"
 :person/birthday #inst "1997-10-08"}
```

</v-click>

---
layout: center
---

<style>
h2 {
  text-align: center;
}
</style>

## Im Folgenden stellen wir uns eine Datenbank vor, die Informationen über Filme und Schauspieler speichert.

---

# Datalog als Query-Sprache

<style>
.shiki code {
  @apply text-3xl;
}

.explanation {
  margin-top: 4em;
}

.label {
  position: absolute;
  top: 195px;
  @apply text-3xl;
}

.label-1 {
  left: 235px;
  @apply text-red-600;
}

.label-2 {
  left: 370px;
  @apply text-green-600;
}

.label-3 {
  left: 525px;
  @apply text-blue-600;
}

</style>

```clj
[:find ?e
 :where [?e :movie/year 1987]]
```

<p class="explanation text-xl">

<v-clicks>

- Finde alle **Entity**-IDs `?e`, die das **Attribut** `:movie/year` 
  mit dem **Wert** `1987` haben.
- Finde alle Filme aus dem Jahr 1987.

</v-clicks>

</p>

<v-click>

<span class="label-1 label">E</span>

</v-click>

<v-click>

<span class="label-2 label">A</span>

</v-click>

<v-click>

<span class="label-3 label">V</span>

</v-click>

---

# Datalog als Query-Sprache

<style>
code {
  @apply text-2xl;
}
</style>

```clj
;; Datensatz
[123 :movie/year 1985]
[123 :movie/title "Back To The Future"]
[123 :movie/cast 234]
[234 :person/name "Michael J. Fox"]

;; Query
[:find ?title
 :where [_ :movie/title ?title]]
```

---

# Datalog als Query-Sprache 

<style>
code {
  @apply text-2xl;
}
</style>

```clj {all|2,9|3,10|2,3,9,10|all}
;; Datensatz
[123 :movie/year 1985]
[123 :movie/title "Back To The Future"]
[123 :movie/cast 234]
[234 :person/name "Michael J. Fox"]

;; Query
[:find ?title
 :where [?e :movie/year 1985]
        [?e :movie/title ?title]]
```

<v-click>

<arrow x1="305" y1="215" x2="125" y2="248" width="2" />

</v-click>

---

# Datalog als Query-Sprache

<style>
code {
  @apply text-2xl;
}

.bracket {
  position: absolute;
  top: 390px;
  left: 535px;
  @apply text-6xl;
}

.label {
  position: absolute;
  top: 405px;
  left: 565px;
  @apply text-3xl;
}
</style>

```clj {all|3,9|4,10|3,4,9,10|5,11|4,5,10,11|all}
;; Datensatz
[123 :movie/year 1985]
[123 :movie/title "Back To The Future"]
[123 :movie/cast 234]
[234 :person/name "Michael J. Fox"]

;; Query
[:find ?name
 :where [?m :movie/title "Back To The Future"]
        [?m :movie/cast ?p]
        [?p :person/name ?name]]
```

<v-click>

<span class="bracket">}</span>
<span class="label">Impliziter Join!</span>

</v-click>

---

# Datalog als Query-Sprache

<style>
code {
  @apply text-2xl;
}
</style>
```clj {all|9}
;; Datensatz
[123 :movie/year 1985]
[123 :movie/title "Back To The Future"]
[123 :movie/cast 234]
[234 :person/name "Michael J. Fox"]

;; Query
[:find ?name
 :in $ ?title
 :where [?m :movie/title ?title]
        [?m :movie/cast ?p]
        [?p :person/name ?name]]
```

---

# Datalog als Query-Sprache

<style>
h2 {
  text-align: left;
}

li {
  @apply text-2xl;
}
</style>

## Außerdem...

<p class="text-2xl">

<v-clicks>

- Datenbank ist Eingabeparameter\
=> Query über mehrere Datenbanken möglich
- Aggregatfunktionen: `count`, `sum`, `avg`, ...
- Ausdrucksklauseln: `<`, `>`, ...
- Logische Klauseln: `or`, `and`, `not`
- Aufrufen von eigenen Funktionen/Methoden innerhalb einer Query
- **Pull** - Schneller Weg, um Entity-Infos zu kriegen
- **Rules** - Pattern Matching auf Steroiden

</v-clicks>

</p>

---

# Datalog als Query-Sprache

<style>
code {
  @apply text-xl;
}
</style>
```clj
;; Datensatz
[123 :movie/year 1985]
[123 :movie/title "Back To The Future"]
[123 :movie/cast 234]
[234 :person/name "Michael J. Fox"]

;; Query
[:find (pull ?m [* {:movie/cast [*]}])
 :where [?m :movie/title "Back To The Future"]]
 
;; Ergebnis
[{:title "Back To The Future"
  :year 1985
  :cast [{:name "Michael J. Fox"}, ...]
  ...}]
```


---

# Datalog als Query-Sprache

<style>
code {
  @apply text-xl;
}
</style>

```clj
;; Query
[:find (count ?outer) .
  :in $ % ?search
  :where
  [?inner :bag/name ?search]
  [?c :bag.child/bag ?inner]
  (child ?outer ?c)]

;; Rules
[[(child ?outer ?c)
   [?outer :bag/children ?c]]
  [(child ?outer ?c)
   [?outer :bag/children ?nc]
   [?nc :bag.child/bag ?next]
   (child ?next ?c)]]
```

---

# Weiterführend

- [Kommentierter Beispiel-Code](https://github.com/devcordde/datomic-intro/blob/main/example-code)
- [Learn Datalog Today](http://learndatalogtoday.org)
- [Rich Hickey - Datomic](https://www.youtube.com/watch?v=9TYfcyvSpEQ) (Architektur & Implementation von  Datomic)
- [Datomic](https://www.datomic.com/)
- [Datahike](https://github.com/replikativ/datahike), [Datalevin](https://github.com/juji-io/datalevin) (Datomic-ähnliche Open Source Datenbanken)
- [XTDB](https://xtdb.com/index.html) (Graph- und relationale Datenbank in einem, mit Datalog Support)
- [DataScript](https://github.com/tonsky/DataScript) (In-Memory Datalog DB fürs Frontend)
- [Using Datahike's Java API to build a web application](https://lambdaforge.io/2020/05/25/java-api.html)
- [Rich Hickey - The Value of Values](https://www.youtube.com/watch?v=-6BsiVyC1kM)

---
layout: statement
background: https://source.unsplash.com/collection/94734566/1920x1080
---

# Danke für eure Aufmerksamkeit
## Gibt es Fragen?

<br>

### (Slides zu finden auf [**devcord.club/datomic-intro**](https://devcord.club/datomic-intro))

