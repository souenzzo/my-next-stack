(ns server.walkable-test
  (:require [clojure.test :refer [deftest use-fixtures]]
            [midje.sweet :refer :all]
            [walkable.sql-query-builder :as sqb]
            [walkable.sql-query-builder.pathom-env :as penv]
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
     :idents         {:account/by-slug :account/slug
                      :account/slug    :account/slug}
     :quote-marks    sqb/quotation-marks}))

(def parser-w
  (p/parser {::p/plugins [(p/env-plugin {::p/reader       [sqb/pull-entities p/map-reader]
                                         ::sqb/sql-db     db
                                         ::sqb/sql-schema compiled-schema
                                         ::sqb/run-query  (fn [& args]
                                                            (prn (second args))
                                                            (apply j/query args))})]}))



(def person-resolver
  (pc/resolver `person-resolver
               {::pc/input  #{:account/slug}
                ::pc/output [:account/name
                             :account/slug
                             {:account/friends [:account/id]}]}
               (fn [{::sqb/keys [run-query sql-schema sql-db]
                     ::p/keys   [parent-query]
                     :as        env} {:keys [account/by-slug]
                                      :as   params}]

                 (clojure.pprint/pprint (penv/dispatch-key env))
                 (clojure.pprint/pprint (::sqb/target-tables sql-schema))
                 (clojure.pprint/pprint (sqb/pull-entities env))
                 (first (run-query sql-db ["SELECT  \"account\".\"name\" AS \"account/name\"  FROM \"account\" WHERE (\"account\".\"slug\")=( ? )" by-slug])))))

;; define a list with our resolvers
(def my-resolvers [person-resolver])

;; setup for a given connect system
(def parser-c
  (p/parser
    {::p/env     {::p/reader       [p/map-reader
                                    pc/all-readers]
                  ::sqb/sql-db     db
                  ::sqb/sql-schema compiled-schema
                  ::sqb/run-query  (fn [& args]
                                     (prn (second args))
                                     (apply j/query args))}
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
      "Original"
      (parser-w {} [{[:account/by-slug "enzzo"] [:account/name]}])
      => {[:account/by-slug "enzzo"] {:account/name "Enzzo"}})
    (fact
      (parser-w {} [{[:account/by-slug "enzzo"] [:account/name
                                                 {:account/friends [:account/name]}]}])
      => {[:account/by-slug "enzzo"] {:account/friends [{:account/name "Ian"}
                                                        {:account/name "Ingrid"}]
                                      :account/name    "Enzzo"}})
    (fact
      "walkable connect"
      (parser-c {} [{[:account/slug "enzzo"] [:account/slug
                                              :account/name]}])
      => {[:account/slug "enzzo"] {:account/name "Enzzo"}})
    #_(fact
        (parser-c {} [{[:account/by-slug "enzzo"] [:account/name
                                                   {:account/friends [:account/name]}]}])
        => {[:account/by-slug "enzzo"] {:account/friends [{:account/name "Ian"}
                                                          {:account/name "Ingrid"}]
                                        :account/name    "Enzzo"}})))
