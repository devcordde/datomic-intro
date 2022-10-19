(ns comment-system
  (:require [datahike.api :as d]))

(comment
  "Schemadefinition: Eine Liste von Entities, die Attribute beschreiben

   :db/ident - Name des Attributs
   :db/valueType - Datentyp
   :db/cardinality - Kardinalität (entweder `one` oder `many`)
   :db/unique - Falls Werte dieses Attributs einzigartig sein müssen

   Ein Schema ist nichts anderes als eine Reihe von Transaktionsdaten wie sonst auch - genauso könnte man für ein Attribut auch..."

  [[:db/add "new-attr" :db/ident :post/id]
   [:db/add "new-attr" :db/valueType :db.type/uuid]
   ...]

  "...schreiben. Attribute sind also selbst Entities mit speziellen, built-in Attributen, die Metadaten definieren.")


(def schema
  [;; Post entity
   {:db/ident :post/id
    :db/valueType :db.type/uuid
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity}
   {:db/ident :post/locked?
    :db/valueType :db.type/boolean
    :db/cardinality :db.cardinality/one}
   {:db/ident :post/comment
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many}

   ;; Comment entity
   {:db/ident :comment/id
    :db/valueType :db.type/uuid
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity}
   {:db/ident :comment/author
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}
   {:db/ident :comment/text
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident :comment/points
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one}
   {:db/ident :comment/timestamp
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one}
   {:db/ident :comment/child
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many}

   ;; User entity
   {:db/ident :user/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity}
   {:db/ident :user/id
    :db/valueType :db.type/uuid
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity}
   {:db/ident :user/avatar-url
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   ;; For user accounts created via WebAuthn
   {:db/ident :user/fido-credential-id
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/many}
   ;; For user accounts created via password
   {:db/ident :user/password-hash
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}])

(comment
  "`create-user` ist eine Funktion, die Informationen über einen User entgegennimmt (`user-map`) und diesen nur eine zufällige ID zuweist. `assoc` macht dabei nur folgendes:"

     (assoc #:user {:name "johnny", :avatar-url "http://example.com"} :user/id (random-uuid))
  => #:user {:name "johnny", :avatar-url "http://example.com", :id #uuid "a359b25b-cc74-4d9f-b220-db831d2a1e8e"}

  "Beim Rückgabewert von `create-user` handelt es sich um eine Transaktion (pure Daten!), die man dann an die `transact` Funktion geben kann.")

(defn create-user [user-map]
  [(assoc user-map :user/id (random-uuid))])

(comment
  "`update-points` ist eine Funktion, die *innerhalb* einer Transaktion verwendet werden kann.
   Sie identifiziert eine Comment-Entity und addiert eine gegebene Zahl Punkte auf den existierenden Wert.
   Siehe `upvote` und `downvote` für Benutzung.")

(defn update-points [db comment delta]
  (let [current (d/pull db [:comment/points :db/id] comment)]
    [[:db/add (:db/id current) :comment/points (+ (:comment/points current) delta)]]))

(defn upvote [comment]
  [[:update-points comment 1]])

(defn downvote [comment]
  [[:update-points comment -1]])


(comment
  "Eine Funktion, die Transaktionsdaten zurück gibt, mit welchen ein neuer Kommentar erstellt werden kann.
   Die Transaktion hat dabei 2 wichtige Bestandteile: ein mal die Fakten über den Kommentar selbst, zum Beispiel:"

  #:comment {:id #uuid "a359b25b-cc74-4d9f-b220-db831d2a1e8e"
             :timestamp #inst "2022-10-19T09:13:39.065-00:00"
             :points 0
             :author [:user/name "johnny"] ; <- Das hier ist eine "lookup ref" - statt entity IDs kann man auch den Wert eines unique Attributs angeben.
             :text "Hello, comment section 🙋"}

  "Der andere Teil ist das Hinzufügen eines Fakts, der beschreibt worunter sich der Kommentar befindet.
   Kommentare können direkt unter einem Post stehen, in diesem Fall ist folgendes Teil der Transaktion:"
  [:db/add post-id :post/comment comment-id]
  "Falls der Kommentar eine Antwort auf einen existierenden Kommentar ist, ist stattdessen in der Transaktion:"
  [:db/add parent-comment-id :comment/child comment-id])

(defn create-comment [comment-map parent reply?]
  [(assoc comment-map
          :comment/id (random-uuid)
          :comment/timestamp (java.util.Date.)
          :comment/points 0
          :db/id "comment")
   [:db/add parent (if reply? :comment/child :post/comment) "comment"]])

(comment
  "Editieren eines Kommentars löscht nicht den alten Text, sondern erstellt einen neuen, aktuelleren Fakt über den Text.
   Das erlaubt es, problemlos die Edit-History abzufragen (s.u.)!")

(defn edit-comment [comment new-text]
  [[:db/add comment :comment/text new-text]])


(comment
  "Die `comment-tree-pattern` Funktion gibt ein 'Pull'-Pattern zurück, mit dem man einen Kommentar-Baum ausgehend von einem Root-Kommentar abfragen kann.
   `max-depth` ist dabei eine Beschränkung für die maximale Rekursion. Also wenn es 20 Kommentare gibt, die die ganze Zeit nur aufeinander antworten
   und `max-depth` auf 5 gesetzt ist, dann werden nur die ersten 5 in diesem Thread herausgesucht.
   Dieses Pattern, angewendet auf einen Kommentar, resultiert in so etwas:"

  #:comment {... ; id, timestamp
             :text "Hi, does anyone know X? I'm having the problem Y."
             :author {...}
             :points 1
             :children
             [#:comment {:text "Yeah, I've worked with it for a while. You might want to try Z."
                         :author {...}
                         :points 2
                         :children
                         [#:comment {:text "Cool, I'll try that right now."
                                     :author {...}
                                     :points 0}]}
              #:comment {:text "No, sorry, but maybe this is helpful? http://example.com"
                         :author {...}
                         :points 4}]})

(defn comment-tree-pattern [max-depth]
  (let [comment-attrs [:comment/id :comment/text :comment/timestamp :comment/points]]
    (into
     comment-attrs
     [{:comment/author [:user/name :user/id :user/avatar-url]}
      {[:comment/child :as :comment/children :limit max-depth] comment-attrs}])))

(comment
  "Eine Funktion, die basierend auf `comment-tree-pattern` ein Pattern erstellt, welches die Kommentar-Bäume für alle Kommentare unter einem Post findet.")

(defn post-comment-tree-pattern [max-depth]
  [{[:post/comment :as :post/comments] (comment-treee-pattern max-depth)}])


(comment
  "Das hier ist ein Beispiel für eine Query, die den gesamten Editier-Verlauf eines Kommentars findet.
   Zurückgegeben wird eine Relation mit Timestamp und Text für jede Änderung.
   So ruft man diese Query im Code auf:"

  (d/q edit-history-q (d/history q) #uuid "meine-kommentar-id"))

(def edit-history-q
  '[:find ?timestamp ?text
    :in $ ?comment-id
    :where [?comment :comment/id ?comment-id]
           [?comment :comment/text ?text ?tx] ;; ?tx matcht die Transaktions-ID, mit welcher der Fakt hinzugefügt wurde...
           [?tx :db/txInstant ?timestamp]]) ;; ...darüber kommt man dann auch an den Timestamp.

(def user-exists-q
  '[:find ?user .
    :in $ ?name
    :where [?user :user/name ?name]])

(comment
  "Das hier ist eine sehr einfache Query: sie zählt einfach die Anzahl Kommentare.
   Was man hier aber sehen kann ist das 'Zeitreise'-Feature - Zum Beispiel können wir diese Query benutzen,
   um zu zählen, wie viele Kommentare zwischen Datum x und Datum y gepostet wurden:"

  (d/q comment-count-q (-> db (d/as-of y) (d/since x)))

  "`as-of` gibt eine Sicht auf die Datenbank *bis* zu einem Zeitpunkt, `since` gibt eine Sicht auf die Datenbank *ab* einem Zeitpunkt.
   Beide zusammen schränken die Datenbank auf eine Zeitspanne ein.")

(def comment-count-q
  '[:find (count ?comment) .
    :where [?comment :comment/id]])
