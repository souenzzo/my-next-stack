(ns server.core
  (:gen-class)
  (:require [clojure.string :as string]
            [clojure.tools.reader.edn :as edn]
            [cognitect.transit :as transit]
            [datomic.api :as d]
            [com.wsscode.pathom.connect :as pc]
            [com.wsscode.pathom.core :as p]
            [io.pedestal.http :as http]))

(def indexes (atom {}))

(defmulti mutation-fn pc/mutation-dispatch)
(def defmutation
  (pc/mutation-factory mutation-fn indexes))

(defmutation `app/new-todo
             {}
             (fn [{:keys [conn]} {:keys [todo/text]}]
               (let [tx-data [{:todo/text text :todo/done? false}]
                     {:keys [tempids db-after]} @(d/transact conn tx-data)]
                 {})))

(defmutation `app/done-todo
             {}
             (fn [{:keys [conn]} {:keys [todo/done? db/id]}]
               (let [tx-data [{:db/id id :todo/done? done?}]
                     {:keys [tempids db-after]} @(d/transact conn tx-data)]
                 {})))

(defmulti resolver-fn pc/resolver-dispatch)
(def defresolver
  (pc/resolver-factory resolver-fn indexes))

(defresolver `app-todos
             {::pc/output [{:app/todos [:db/id
                                        :todo/done?
                                        :todo/text]}]}
             (fn [{:keys [conn query]} args]
               {:app/todos (d/q '[:find [(pull ?e pattern) ...]
                                  :in $ pattern
                                  :where
                                  [?e :todo/text]]
                                (d/db conn) query)}))

(def parser
  (p/parser {::p/env {::p/reader             [p/map-reader pc/all-readers]
                      ::pc/mutate-dispatch   mutation-fn
                      ::pc/resolver-dispatch resolver-fn
                      ::pc/indexes           @indexes}
             :mutate pc/mutate}))

(def schema
  [{:db/ident       :todo/text
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/fulltext    true
    :db/index       true}
   {:db/ident       :todo/done?
    :db/valueType   :db.type/boolean
    :db/cardinality :db.cardinality/one
    :db/index       true}])

(defonce conn (let [db-uri "datomic:mem://my-next-stack"]
                (d/delete-database db-uri)
                (d/create-database db-uri)
                (doto (d/connect db-uri)
                  (d/transact-async schema))))

(defn api
  [{:keys [body]}]
  (prn [:in body])
  (let [body (parser {:conn conn} body)]
    (prn [:out body])
    {:body   body
     :status 200}))

(def read-writer
  {:name  ::read-writer
   :enter (fn [{{{:strs [content-type]
                  :or   {content-type "application/edn"}} :headers} :request
                :as                                                 ctx}]
            (let [reader (cond (string/starts-with? content-type "application/transit+json") (fn [in]
                                                                                               (transit/read (transit/reader in :json)))
                               :else (comp edn/read-string slurp))]
              (update-in ctx [:request :body] reader)))
   :leave (fn [{{{:strs [accept]
                  :or   {accept "application/edn"}} :headers} :request
                :as                                           ctx}]
            (let [writer (case accept
                           ("application/transit+json" "*/*") (fn [data]
                                                                (fn [out]
                                                                  (try
                                                                    (transit/write (transit/writer out :json) data)
                                                                    (catch Throwable e
                                                                      (prn e)))))
                           pr-str)]
              (-> ctx
                  (assoc-in [:response :headers "Content-Type"] (case accept "*/*" "application/transit+json" accept))
                  (update-in [:response :body] writer))))})
(def routes
  `#{["/api" :post [read-writer api]]})

(def service
  (-> {::http/routes          routes
       ::http/port            8080
       ::http/join?           false
       ::http/type            :jetty
       ::http/cred            true
       ::http/allowed-origins {:creds           true
                               :allowed-origins (constantly true)}
       ::http/secure-headers  {:content-security-policy-settings ""}}
      http/default-interceptors
      http/dev-interceptors
      http/create-server))

(defonce http-service (atom nil))

(defn -main
  [& argv]
  (println "\nCreating your server...")
  (when-let [srv @http-service]
    (http/stop srv))
  (reset! http-service (http/start service)))
