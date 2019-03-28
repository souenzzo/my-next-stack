(ns my-next-stack.core
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [clojure.java.jdbc :as j]
            [io.pedestal.http.csrf :as csrf]
            [com.wsscode.pathom.core :as p]
            [com.wsscode.pathom.connect :as pc]
            [fulcro.client.dom-server :as dom]
            [ring.util.mime-type :as mime]
            [io.pedestal.log :as log]
            [clojure.core.async :as async]
            [cognitect.transit :as transit]
            [clojure.string :as string]
            [io.pedestal.interceptor :as interceptor]
            [clojure.java.io :as io])
  (:import (org.eclipse.jetty.server.handler.gzip GzipHandler)
           (org.eclipse.jetty.servlet ServletContextHandler)))

(defn transit-type
  ([x] (transit-type x false))
  ([x verbose?]
   (when (and (string? x)
              (or (string/starts-with? x "application/transit")
                  (= x "*/*")))
     (cond
       (string/ends-with? x "+msgpack") :msgpack
       verbose? :json-verbose
       :else :json))))

(defn pr-transit
  [type body]
  (fn pr-transit [out]
    (try
      (let [writer (transit/writer out type)]
        (transit/write writer body))
      (catch Throwable e
        (log/error :type type :body body :pr-transit e)))))


(def type->conten-type
  {:json         "application/transit+json"
   :json-verbose "application/transit+json"
   :msgpack      "application/transit+msgpack"
   :edn          "application/edn"})

(def write-body
  {:name  ::write-body
   :leave (fn leave-write-body
            [{{::keys           [verbose-transit?]
               {:strs [accept]} :headers} :request
              {:keys [body]}              :response
              :as                         ctx}]
            (cond
              (dom/element? body) (-> ctx
                                      (assoc-in [:response :headers "Content-Type"] "text/html")
                                      (assoc-in [:response :body] (str "<!DOCTYPE html>\n"
                                                                       (dom/render-to-str body)
                                                                       "\n")))
              :else (let [type (transit-type accept verbose-transit?)
                          response-body (if type
                                          (pr-transit type body)
                                          (pr-str body))
                          content-type (type->conten-type (or type :edn))]
                      (-> ctx
                          (assoc-in [:response :body] response-body)
                          (assoc-in [:response :headers "Content-Type"] content-type)))))})


(pc/defresolver username-by-id [{:keys [db]} {:app.user/keys [id]}]
  {::pc/input  #{:app.user/id}
   ::pc/output [:app.user/username]}
  {:app.user/username (-> (j/query db ["SELECT username FROM account WHERE id = ?"
                                       id])
                          first
                          :username)})

(defn user-id-from-username
  [db username]
  (-> (j/query db ["SELECT id FROM account WHERE username = ?"
                   username])
      first
      :id))

(pc/defmutation login [{:keys [db]} {:app.user/keys [username]}]
  {::pc/sym    `app.user/login
   ::pc/output [:app.user/username]
   ::pc/params [:app.user/id]}
  (let [id (j/with-db-transaction [db* db]
             (or (user-id-from-username db* username)
                 (do
                   (j/insert! db* :account {:username username})
                   (user-id-from-username db* username))))]
    {:app.user/id id}))

(pc/defresolver friends [{:keys [db]} {:app.user/keys [id]}]
  {::pc/output [{:app.user/friends [:app.user/id]}]
   ::pc/input  #{:app.user/id}}
  (let [users (j/query db ["SELECT id FROM account"])]
    {:app.user/friends (for [{:keys [id]} users]
                         {:app.user/id id})}))

(def parser
  (p/parallel-parser
    {::p/env     {::p/reader               [p/map-reader
                                            pc/all-parallel-readers
                                            p/env-placeholder-reader]
                  ::p/placeholder-prefixes #{">"}}
     ::p/mutate  pc/mutate-async
     ::p/plugins [(pc/connect-plugin {::pc/register [login friends username-by-id]})
                  p/error-handler-plugin]}))

(defn index
  [{::csrf/keys [anti-forgery-token] :keys [db]}]
  {:body   (dom/html
             {:lang "pt-BR"}
             (dom/head
               (dom/meta {:charset "UTF-8"})
               (dom/meta {:name    "viewport"
                          :content "width=device-width, initial-scale=1"})
               (dom/link {:id   "favicon"
                          :rel  "shortcut icon"
                          :type "image/svg+xml"
                          :href "data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg'></svg>"})
               (dom/title "My Next Stack"))
             (dom/body
               {:data-anti-forgery-token anti-forgery-token}
               (dom/div {:id "app"})
               (dom/script {:src "/_static/pwa/main.js"})
               (dom/script {:dangerouslySetInnerHTML {:__html "my_next_stack.pwa.main()"}})))

   :status 200})

(def api
  {:name  ::api
   :enter (fn [{{:keys [edn-params transit-params]} :request
                :keys                               [request]
                :as                                 context}]
            (let [query (or edn-params transit-params)
                  result (parser request query)]
              (async/go
                (assoc context :response {:body   (async/<! result)
                                          :status 200}))))})

(def routes
  `#{["/" :get [write-body index]]
     ["/api" :post [write-body api]]})


(defn context-configurator
  "Habilitando gzip nas respostas"
  [^ServletContextHandler context]
  (let [gzip-handler (GzipHandler.)]
    (.addIncludedMethods gzip-handler (into-array ["GET" "POST"]))
    (.setExcludedAgentPatterns gzip-handler (make-array String 0))
    (.setGzipHandler context gzip-handler))
  context)


(def content-security-policy-settings
  (string/join " " ["script-src"
                    "'self'"
                    "'unsafe-inline'"
                    "'unsafe-eval'"]))

(def service
  {:env                     :prod
   :db                      {:dbtype   "postgresql"
                             :dbname   "app"
                             :host     "localhost"
                             :user     "postgres"
                             :password "postgres"}
   ::http/enable-csrf       {}
   ::http/mime-types        mime/default-mime-types
   ::http/resource-path     "public"
   ::http/file-path         "target/public"
   ::http/container-options {:context-configurator context-configurator}
   ::http/secure-headers    {:content-security-policy-settings content-security-policy-settings}
   ::http/port              8080
   ::http/routes            routes
   ::http/host              "0.0.0.0"
   ::http/type              :jetty})

(defn add-env-interceptor
  [env]
  (->> (fn [interceptors]
         (into [(interceptor/interceptor {:name  ::env-interceptor
                                          :enter (fn [ctx]
                                                   (update ctx :request #(into env %)))})]
               interceptors))
       (update env ::http/interceptors)))

(defn install-schema!
  [{:keys [dbname] :as db} schemas]
  (j/execute! (assoc db :dbname "")
              (format "DROP DATABASE IF EXISTS %s;" dbname)
              {:transaction? false})
  (j/execute! (assoc db :dbname "")
              (format "CREATE DATABASE %s;" dbname)
              {:transaction? false})
  (doseq [schema schemas]
    (j/execute! db schema)))

(defonce http-state (atom nil))

(defn dev-start
  []
  (install-schema! (:db service) (mapv slurp [(io/resource "schema.sql")]))
  (swap! http-state (fn [st]
                      (when st
                        (http/stop st))
                      (-> service
                          (assoc :env :dev
                                 ::http/join? false)
                          (update ::http/routes (fn [routes]
                                                  #(route/expand-routes routes)))
                          http/default-interceptors
                          add-env-interceptor
                          http/dev-interceptors
                          http/create-server
                          http/start))))

(defn -main
  [& _]
  (swap! http-state (fn [st]
                      (when st
                        (http/stop st))
                      (-> service
                          http/default-interceptors
                          add-env-interceptor
                          http/create-server
                          http/start))))
