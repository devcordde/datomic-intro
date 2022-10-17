(ns comment-system
  (:require [datahike.api :as d]))

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

(defn create-user [user-map]
  [(assoc user-map :user/id (random-uuid))])

(defn update-points [db comment f & args]
  (let [current (d/pull db '[:comment/points :db/id] comment)]
    [[:db/add (:db/id current) :comment/points (apply f (:comment/points current) args)]]))

(defn create-comment [comment-map parent reply?]
  [(assoc comment-map :comment/id (random-uuid) :db/id "comment")
   [:db/add parent (if reply? :comment/child :post/comment) "comment"]])

(defn edit-comment [comment new-text]
  [[:db/add comment :comment/text new-text]])

(defn upvote [comment]
  [[:update-points comment inc]])

(defn downvote [comment]
  [[:update-points comment dec]])

(defn comment-tree-pattern [root-comment max-depth]
  (let [comment-attrs [:comment/id :comment/text :comment/timestamp :comment/points]]
    (into
     comment-attrs
     [{:comment/author [:user/name :user/id :user/avatar-url]}
      {[:comment/child :as :comment/children :limit max-depth] comment-attrs}])))

(def edit-history-q
  '[:find ?timestamp ?text
    :in $ ?comment-id
    :where [?comment :comment/id ?comment-id]
           [?comment :comment/text ?text ?tx]
           [?tx :db/txInstant ?timestamp]])

(def user-exists-q
  '[:find ?user .
    :in $ ?name
    :where [?user :user/name ?name]])

(def comment-count-q
  '[:find (count ?comment) .
    :where [?comment :comment/id]])
