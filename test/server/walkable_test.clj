(ns server.walkable-test
  (:require [clojure.test :refer [deftest use-fixtures]]
            [midje.sweet :refer :all]
            [walkable.sql-query-builder :as sqb]
            [clojure.java.jdbc :as j]
            [clojure.java.io :as io]
            [com.wsscode.pathom.connect :as pc]
            [com.wsscode.pathom.core :as p]))

(def db
  {:dbtype   "postgresql"
   :dbname   "app"
   :host     "localhost"
   :user     "postgres"
   :password "postgres"})

(use-fixtures :each
              (fn [f]
                (j/execute! (assoc db :dbname "")
                            "DROP DATABASE IF EXISTS app;" {:transaction? false})
                (j/execute! (assoc db :dbname "")
                            "CREATE DATABASE app;" {:transaction? false})
                (j/execute! db (slurp (io/resource "schema.sql")))
                (f)))

(def compiled-schema
  (sqb/compile-schema
    {:columns        #{:account/name
                       :account/slug}
     :cardinality    {:account/by-slug :one}
     :joins          {:account/friends [:account/id :friends/account_id
                                        :friends/friend_id :account/id]}
     :reversed-joins {}
     :idents         {:account/by-slug :account/slug}
     :quote-marks    sqb/quotation-marks}))

(def parser-w
  (p/parser {::p/plugins [(p/env-plugin {::p/reader       [sqb/pull-entities p/map-reader]
                                         ::sqb/sql-db     db
                                         ::sqb/sql-schema compiled-schema
                                         ::sqb/run-query  (fn [& args]
                                                            (prn (second args))
                                                            (apply j/query args))})]}))



;; How to go from :person/id to that person's details
(pc/defresolver person-resolver
  [env {:keys [person/id] :as params}]
  ;; The minimum data we must already know in order to resolve the outputs
  {::pc/input  #{:person/id}
   ;; A query template for what this resolver outputs
   ::pc/output [:person/name {:person/address [:address/id]}]}
  ;; normally you'd pull the person from the db, and satisfy the listed
  ;; outputs. For demo, we just always return the same person details.
  {:person/name    "Tom"
   :person/address {:address/id 1}})

;; how to go from :address/id to address details.
(pc/defresolver address-resolver
  [env {:keys [address/id] :as params}]
  {::pc/input  #{:address/id}
   ::pc/output [:address/city :address/state]}
  {:address/city  "Salem"
   :address/state "MA"})

;; define a list with our resolvers
(def my-resolvers [person-resolver address-resolver])

;; setup for a given connect system
(def parser-c
  (p/parser
    {::p/env     {::p/reader [p/map-reader pc/reader]}
     ::p/plugins [(pc/connect-plugin {::pc/register my-resolvers})]}))

(deftest my-sql-test
  (let [enzzo {:account/name "Enzzo"
               :account/slug "enzzo"}
        ian {:account/name "Ian"
             :account/slug "ian"}
        ingrid {:account/name "Ingrid"
                :account/slug "ingrid"}
        tx-accounts [enzzo ian ingrid]
        [enzzo-id ian-id ingrid-id] (map :id (j/insert-multi! db :account tx-accounts))
        _ (j/insert-multi! db :friends [{:friend/account_id enzzo-id
                                         :friend/friend_id  ian-id}
                                        {:friend/account_id enzzo-id
                                         :friend/friend_id  ingrid-id}
                                        {:friend/account_id ian-id
                                         :friend/friend_id  enzzo-id}])]
    (fact
      (parser-w {} [{[:account/by-slug "enzzo"] [:account/name]}])
      => {[:account/by-slug "enzzo"] {:account/name "Enzzo"}})
    (fact
      (parser-w {} [{[:account/by-slug "enzzo"] [:account/name
                                                 {:account/friends [:account/name]}]}])
      => {[:account/by-slug "enzzo"] {:account/friends [{:account/name "Ian"}
                                                        {:account/name "Ingrid"}]
                                      :account/name    "Enzzo"}})
    (fact
      (parser-c {} [{[:account/by-slug "enzzo"] [:account/name]}])
      => {[:account/by-slug "enzzo"] {:account/name "Enzzo"}})
    (fact
      (parser-c {} [{[:account/by-slug "enzzo"] [:account/name
                                                 {:account/friends [:account/name]}]}])
      => {[:account/by-slug "enzzo"] {:account/friends [{:account/name "Ian"}
                                                        {:account/name "Ingrid"}]
                                      :account/name    "Enzzo"}})))
