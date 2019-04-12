(ns my-next-stack.core
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [clojure.java.jdbc :as j]
            [io.pedestal.http.csrf :as csrf]
            [com.wsscode.pathom.core :as p]
            [com.wsscode.pathom.connect :as pc]
            [fulcro.client.dom-server :as dom]
            [ring.util.mime-type :as mime]
            [my-next-stack.client.ui :as ui]
            [io.pedestal.log :as log]
            [clojure.core.async :as async]
            [cognitect.transit :as transit]
            [clojure.string :as string]
            [io.pedestal.interceptor :as interceptor]
            [clojure.java.io :as io]
            [fulcro.client.primitives :as fp]
            [fulcro.client.impl.protocols :as fcip]
            [io.pedestal.http.body-params :as body-params])
  (:import (org.eclipse.jetty.server.handler.gzip GzipHandler)
           (fulcro.tempid TempId)
           (fulcro.transit TempIdHandler)
           (org.eclipse.jetty.servlet ServletContextHandler)
           (com.cognitect.transit ReadHandler)))

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

(def transit-write-hanlers
  {TempId (new TempIdHandler)})

(def transit-read-handlers
  {"fulcro/tempid" (reify
                     ReadHandler
                     (fromRep [_ id] (TempId. id)))})

(defn pr-transit
  [type body]
  (fn pr-transit [out]
    (try
      (let [writer (transit/writer out type {:handlers transit-write-hanlers})]
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
  {:app.user/username (-> (j/query db ["SELECT username FROM app_user WHERE id = ?"
                                       id])
                          first
                          :username)})

(defn user-id-from-username
  [db username]
  (-> (j/query db ["SELECT id FROM app_user WHERE username = ?"
                   username])
      first
      :id))

(defn user-id-from-csrf
  [db csrf]
  (-> (j/query db ["
SELECT
  app_user.id
FROM
  app_user
INNER JOIN app_session ON
  (app_user.id = app_session.authed
  AND app_session.csrf = ?)
" csrf])
      first
      :id))

(defn session-id-from-csrf
  [db csrf]
  (-> (j/query db ["SELECT id FROM app_session WHERE csrf = ?"
                   csrf])
      first
      :id))

(pc/defmutation login [{::csrf/keys [anti-forgery-token]
                        :keys       [db]} {tempid         :app.user/id
                                           :app.user/keys [username]}]
  {::pc/sym    `app.user/login
   ::pc/output [:app.user/username]
   ::pc/params [:app.user/id]}
  (let [id (j/with-db-transaction [db* db]
             (let [id (or (user-id-from-username db* username)
                          (do
                            (j/insert! db* :app_user {:username username})
                            (user-id-from-username db* username)))
                   session-id (or (session-id-from-csrf db* anti-forgery-token)
                                  (do
                                    (j/insert! db* :app_session {:csrf anti-forgery-token})
                                    (session-id-from-csrf db* anti-forgery-token)))]
               (j/update! db* :app_session
                          {:authed id}
                          ["id = ?" session-id])
               id))]
    {:app.user/id id
     ::fp/tempids {tempid id}}))

(pc/defmutation exit [{::csrf/keys [anti-forgery-token]
                       :keys       [db]} _]
  {::pc/sym `app.session/exit}
  (let [id (j/with-db-transaction [db* db]
             (let [id (session-id-from-csrf db* anti-forgery-token)]
               (when id
                 (j/update! db* :app_session
                            {:authed nil}
                            ["id = ?" id]))
               id))]
    {}))

(defn chat-id-by-2-users
  [db id1 id2]
  (-> (j/query db ["
select acu1.chat
from app_user_chat as acu1
inner join app_chat ON
  ( acu1.owner = ? )
  AND
  ( app_chat.id = acu1.chat )
inner join app_user_chat AS acu2 ON
  ( acu2.owner = ? )
  AND
  ( app_chat.id = acu2.chat )
" id1 id2])
      first
      :chat))

(pc/defmutation chat-with [{::csrf/keys [anti-forgery-token]
                            :keys       [db]} {tempid         :app.chat/id
                                               :app.user/keys [id]}]
  {::pc/sym    `app.chat/chat-with
   ::pc/input  [:app.user/id]
   ::pc/output [:app.chat/id]}
  (let [id (j/with-db-transaction [db* db]
             (let [my-id (user-id-from-csrf db* anti-forgery-token)]
               (or (chat-id-by-2-users db* id my-id)
                   (let [chat-id (:id (first (j/insert! db* :app_chat {:title nil})))]
                     (j/insert! db* :app_user_chat {:owner my-id :chat chat-id})
                     (j/insert! db* :app_user_chat {:owner id :chat chat-id})
                     chat-id))))]
    {:app.chat/id id
     ::fp/tempids {tempid id}}))

(pc/defmutation send-msg [{::csrf/keys [anti-forgery-token]
                           :keys       [db]} {tempid :app.message/id
                                              :keys  [app.chat/id app.message/body]}]
  {::pc/sym    `app.message/send
   ::pc/input  #{}
   ::pc/output [:app.message/id]}
  (let [me (user-id-from-csrf db anti-forgery-token)
        {:keys [id]} (first (j/insert! db :app_message {:author me
                                                        :chat   id
                                                        :body   body}))]
    {:app.message/id id
     ::fp/tempids    {tempid id}}))

(pc/defmutation set-title [{:keys [db]} {:keys [app.chat/id app.chat/title]}]
  {::pc/sym    `app.chat/new-title
   ::pc/input  #{}
   ::pc/params [:app.chat/id
                :app.chat/title]
   ::pc/output [:app.chat/id
                :app.chat/title]}
  (j/update! db :app_chat {:title title} ["id = ?" id])
  {:app.chat/id    id
   :app.chat/title title})

(pc/defresolver message-title [{:keys [db]} {:keys [app.chat/id]}]
  {::pc/input  #{:app.chat/id}
   ::pc/output [:app.chat/title]}
  (let [{:keys [id title]} (-> (j/query db ["SELECT title, id FROM app_chat WHERE id = ?" id])
                               first)]
    {:app.chat/title (or title (pr-str {:id id}))}))


(pc/defresolver chat-messages [{:keys [db]} {:keys [app.chat/id]}]
  {::pc/input  #{:app.chat/id}
   ::pc/output [:app.chat/messages]}
  (let [messages (j/query db ["SELECT * FROM app_message WHERE chat = ?" id])]
    {:app.chat/messages (for [{:keys [id]} messages]
                          {:app.message/id id})}))


(pc/defresolver friends [{::csrf/keys [anti-forgery-token]
                          :keys       [db]} {:app.user/keys [id]}]
  {::pc/output [{:app.user/friends [:app.user/id
                                    :app.user/me?]}]
   ::pc/input  #{:app.user/id}}
  (let [me (user-id-from-csrf db anti-forgery-token)
        users (j/query db ["SELECT id FROM app_user"])]
    {:app.user/friends (for [{:keys [id]} users]
                         {:app.user/id  id
                          :app.user/me? (= me id)})}))


(pc/defresolver message-body [{:keys [db]} {:keys [app.message/id]}]
  {::pc/input  #{:app.message/id}
   ::pc/output [:app.message/body]}
  (->> ["SELECT body FROM app_message WHERE id = ?" id]
       (j/query db)
       first
       :body
       (hash-map :app.message/body)))

(pc/defresolver index-data [{::csrf/keys [anti-forgery-token]} _]
  {::pc/output [::csrf/anti-forgery-token]}
  {::csrf/anti-forgery-token anti-forgery-token
   ::script-src              "/_static/pwa/main.js"})

(def ui-root (fp/factory ui/Root))

(fp/defsc Index [this {::csrf/keys [anti-forgery-token]}]
  {:query [::csrf/anti-forgery-token]}
  (dom/html
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
      (dom/div {:id "app"}
               (ui-root (fp/get-initial-state ui/Root {})))
      (dom/script {:src "/_static/pwa/main.js"})
      (dom/script {:dangerouslySetInnerHTML {:__html "my_next_stack.pwa.main()"}}))))

(def ui-index (fp/factory Index))

(def index
  {:name  ::index
   :enter (fn [{{:keys [parser]} :request
                :keys            [request]
                :as              context}]
            (let [parser (parser)
                  props (async/<!! (parser request (fp/get-query Index)))]
              (assoc context :response {:body   (fcip/render (ui-index props))
                                        :status 200})))})

(def api
  {:name  ::api
   :enter (fn [{{:keys [edn-params transit-params parser]} :request
                :keys                                      [request]
                :as                                        context}]
            (let [query (or edn-params transit-params)
                  parser (parser)
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

(def db
  {:dbtype   "postgresql"
   :dbname   "app"
   :host     "localhost"
   :user     "postgres"
   :password "postgres"})

(def service
  {:env                     :prod
   :db                      db
   :parser                  #(p/parallel-parser
                               {::p/env     {::p/reader                 [p/map-reader
                                                                         pc/all-parallel-readers
                                                                         p/env-placeholder-reader]
                                             ::pc/mutation-join-globals [::fp/tempids]
                                             ::p/placeholder-prefixes   #{">"}}
                                ::p/mutate  pc/mutate-async
                                ::p/plugins [(pc/connect-plugin {::pc/register [login exit friends
                                                                                index-data message-body set-title
                                                                                message-title chat-messages
                                                                                chat-with send-msg
                                                                                username-by-id]})
                                             p/error-handler-plugin]})
   ::http/enable-csrf       {:body-params (body-params/default-parser-map :transit-options [{:handlers transit-read-handlers}])}
   ::http/mime-types        mime/default-mime-types
   ::http/resource-path     "public"
   ::http/file-path         "target/public"
   ::http/container-options {:context-configurator context-configurator}
   ::http/secure-headers    {:content-security-policy-settings content-security-policy-settings}
   ::http/port              8080
   ::http/routes            routes
   ::http/host              "0.0.0.0"
   ::http/type              :jetty})

(defn env-interceptor
  [env]
  (->> (fn [ctx] (update ctx :request #(into env %)))
       (hash-map :name ::env-interceptor :enter)
       (interceptor/interceptor)))

(defn add-env-interceptor
  [env]
  (->> (fn [interceptors]
         (into [(env-interceptor env)]
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

(defn dev-stop
  []
  (swap! http-state (fn [st]
                      (when st
                        (http/stop st)))))

(defn -main
  [& _]
  (swap! http-state (fn [st]
                      (when st
                        (http/stop st))
                      (-> service
                          (update :parser (fn [parser]
                                            (constantly (parser))))
                          http/default-interceptors
                          add-env-interceptor
                          http/create-server
                          http/start))))
