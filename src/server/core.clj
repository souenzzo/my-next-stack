(ns server.core
  (:gen-class)
  (:require [buddy.sign.jwt :as jwt]
            [clojure.string :as string]
            [clojure.tools.reader.edn :as edn]
            [cognitect.transit :as transit]
            [com.wsscode.pathom.connect :as pc]
            [com.wsscode.pathom.core :as p]
            [datomic.api :as d]
            [io.pedestal.http :as http]
            [server.telegram :as telegram]
            [io.pedestal.log :as log]
            [ring.util.mime-type :as mime]
            [ring.middleware.resource :as resource]
            [io.pedestal.http.route :as route])
  (:import (org.eclipse.jetty.server.handler.gzip GzipHandler)
           (org.eclipse.jetty.servlet ServletContextHandler)
           (java.io File)))

(defn ^:dynamic rand-int-in
  [min max]
  (+ min (rand-int (- max min))))

(def jwt-secret "5ec688a8-e42c-491d-bcec-0559d710e050")

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

(defmutation `app/send-two-factor
             {::pc/output [:app/two-factor]}
             (fn [{:keys [state telegram/token]} {:keys [user/username]}]
               (let [code (format "%04d" (rand-int-in 0 10000))
                     text (format "Verification code: %s" code)]
                 (swap! state update :two-factor assoc username code)
                 {:app/two-factor (telegram/send-two-factor! token username text)})))

(defmutation `app/login
             {::pc/output [:user/token :user/username]}
             (fn [{:keys [state]} {:keys [user/username user/two-factor]}]
               (let [ids (:two-factor @state)
                     token (jwt/sign {:username username} jwt-secret)]
                 (when (= (get ids username) two-factor)
                   {:user/username username
                    :user/token    token}))))

(defmutation `app.counter/inc
             {::pc/output [:app/counter]}
             (fn [{:keys [state]} _]
               {:app/counter (swap! state update :counter (fnil inc 0))}))

(defmulti resolver-fn pc/resolver-dispatch)
(def defresolver
  (pc/resolver-factory resolver-fn indexes))

(defresolver `app-counter
             {::pc/output [:n]}
             (fn [{:keys [state]} _]
               {:n (:counter @state)}))

(defresolver `app-data
             {::pc/output [{:app/data [:n]}]}
             (fn [{:keys [state]} _]
               {:app/data {:n (:counter @state)}}))

(defresolver `app-todos
             {::pc/output [{:app/todos [:db/id
                                        :todo/done?
                                        :todo/text]}]}
             (fn [{:keys [conn query]} args]
               {:app/todos (d/q '[:find [(pull ?e [*]) ...]
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

(defonce state (atom nil))


(def verbose? true)

(defn handler-parser
  [{:keys [params env]}]
  (let [result (parser env params)]
    (log/info :query params :result result)
    {:body   result
     :status 200}))

(defn pr-transit
  [type body]
  (fn pr-transit [out]
    (try
      (let [writer (transit/writer out type)]
        (transit/write writer body))
      (catch Throwable e
        (log/error :transit-writer e)))))

(defn transit-read
  [type in]
  (-> (transit/reader in type)
      (transit/read)))

(defn transit-type
  ([x] (transit-type x false))
  ([x verbose?]
   (when (and (string? x) (or (string/starts-with? x "application/transit")
                              (= "*/*" x)))
     (cond
       (string/ends-with? (str x) "+msgpack") :msgpack
       verbose? :json-verbose
       :else :json))))

(def type->conten-type
  {:json         "application/transit+json"
   :json-verbose "application/transit+json"
   :msgpack      "application/transit+msgpack"
   :edn          "application/edn"})

(def ->edn
  {:name  ::->edn
   :enter (fn [{{{:strs [content-type]} :headers} :request
                {:keys [body]}                    :request
                :as                               ctx}]
            (if-not body
              ctx
              (let [type (transit-type content-type)
                    params (if type
                             (transit-read type body)
                             (edn/read-string (slurp body)))]
                (assoc-in ctx [:request :params] params))))
   :leave (fn [{{{:strs [accept]} :headers} :request
                {:keys [body]}              :response
                :as                         ctx}]
            (let [type (transit-type accept verbose?)
                  response-body (if type
                                  (pr-transit type body)
                                  (pr-str body))]
              (-> ctx
                  (assoc-in [:response :body] response-body)
                  (assoc-in [:response :headers "Content-Type"] (type->conten-type (or type :edn))))))})

(defn handler-resources
  [{:keys [path-info uri]}]
  (log/debug :path-info path-info :uri uri)
  (resource/resource-request
    {:request-method :get
     :path-info      (if (= path-info "/")
                       "/index.html"
                       path-info)
     :uri            (if (= uri "/")
                       "/index.html"
                       uri)}
    "/public"))

(defn file?
  [x]
  (instance? File x))

(def +mime-types
  {:name  ::+mime-types
   :leave (fn [{{:keys [body]} :response
                :as            ctx}]
            (if (file? body)
              (let [filename (.getName ^File body)
                    path [:response :headers "Content-Type"]]
                (cond-> ctx
                        (string? filename) (assoc-in path (mime/ext-mime-type filename))))
              ctx))})

(defn +env
  [env]
  {:name  ::+env
   :enter (fn [ctx] (assoc-in ctx [:request :env] env))})

(defn routes
  [env]
  `#{["/api" :post [->edn ~(+env env) handler-parser]]
     ["/js/*path" :get [+mime-types ~(+env env) handler-resources]]
     ["/" :get [+mime-types ~(+env env) handler-resources] :route-name ::home]})


(defn context-configurator
  "Habilitando gzip nas respostas"
  [^ServletContextHandler context]
  (let [gzip-handler (GzipHandler.)]
    (.setExcludedAgentPatterns gzip-handler (make-array String 0))
    (.setGzipHandler context gzip-handler))
  context)



(def service
  (-> {::http/routes            #(route/expand-routes (routes {:state          state
                                                               :telegram/token (System/getenv "TELEGRAM_TOKEN")
                                                               :conn           conn}))
       ::http/port              8080
       ::http/join?             false
       ::http/type              :jetty
       ::http/cred              true
       ::http/allowed-origins   {:creds           true
                                 :allowed-origins (constantly true)}
       ::http/container-options {:h2c?                 true
                                 :context-configurator context-configurator}
       ::http/secure-headers    {:content-security-policy-settings ""}}
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
