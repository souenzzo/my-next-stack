(ns my-next-stack.core
  (:require [io.pedestal.http :as http]
            [clojure.java.jdbc :as j]
            [io.pedestal.http.csrf :as csrf]
            [com.wsscode.pathom.core :as p]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as jsql]
            [souenzzo.pedestal :as pedestal]
            [com.wsscode.pathom.connect :as pc]
            [fulcro.client.dom-server :as dom]
            [my-next-stack.client.ui :as ui]
            [clojure.java.io :as io]
            [fulcro.client.primitives :as fp]))

(pc/defresolver username-by-id [{::keys [db]} {:app.user/keys [id]}]
  {::pc/input  #{:app.user/id}
   ::pc/output [:app.user/username]}
  {:app.user/username (-> (jdbc/execute! db ["SELECT username FROM app_user WHERE id = ?"]
                                         id)
                          first
                          :username)})

(defn user-id-from-username
  [db username]
  (-> (jsql/query db ["SELECT id FROM app_user WHERE username = ?"
                      username])
      first
      :app_user/id))

(defn user-id-from-csrf
  [db csrf]
  (-> (jsql/query db ["
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
  (-> (jsql/query db ["SELECT id FROM app_session WHERE csrf = ?"
                      csrf])
      first
      :app_session/id))

(pc/defmutation login [{::csrf/keys [anti-forgery-token]
                        ::keys      [db]} {tempid         :app.user/id
                                           :app.user/keys [username]}]
  {::pc/sym    `app.user/login
   ::pc/output [:app.user/username]
   ::pc/params [:app.user/id]}
  (let [id (jdbc/with-transaction [db* (jdbc/get-datasource db)]
             (let [id (or (user-id-from-username db* username)
                          (do
                            (jsql/insert! db* :app_user {:username username})
                            (user-id-from-username db* username)))
                   session-id (or (session-id-from-csrf db* anti-forgery-token)
                                  (do
                                    (jsql/insert! db* :app_session {:csrf anti-forgery-token})
                                    (session-id-from-csrf db* anti-forgery-token)))]
               (jsql/update! db* :app_session
                             {:authed id}
                             ["id = ?" session-id])
               id))]
    {:app.user/id id
     ::fp/tempids {tempid id}}))

(pc/defmutation exit [{::csrf/keys [anti-forgery-token]
                       ::keys      [db]} _]
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
  (-> (jdbc/execute! db ["
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
                            ::keys      [db]} {tempid         :app.chat/id
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
                           ::keys      [db]} {tempid :app.message/id
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

(pc/defmutation set-title [{::keys [db]} {:keys [app.chat/id app.chat/title]}]
  {::pc/sym    `app.chat/new-title
   ::pc/input  #{}
   ::pc/params [:app.chat/id
                :app.chat/title]
   ::pc/output [:app.chat/id
                :app.chat/title]}
  (j/update! db :app_chat {:title title} ["id = ?" id])
  {:app.chat/id    id
   :app.chat/title title})

(pc/defresolver message-title [{::keys [db]} {:keys [app.chat/id]}]
  {::pc/input  #{:app.chat/id}
   ::pc/output [:app.chat/title]}
  (let [{:keys [id title]} (-> (jdbc/execute! db ["SELECT title, id FROM app_chat WHERE id = ?" id])
                               first)]
    {:app.chat/title (or title (pr-str {:id id}))}))


(pc/defresolver chat-messages [{::keys [db]} {:keys [app.chat/id]}]
  {::pc/input  #{:app.chat/id}
   ::pc/output [:app.chat/messages]}
  (let [messages (jdbc/execute! db ["SELECT * FROM app_message WHERE chat = ?" id])]
    {:app.chat/messages (for [{:keys [id]} messages]
                          {:app.message/id id})}))


(pc/defresolver friends [{::csrf/keys [anti-forgery-token]
                          ::keys      [db]} {:app.user/keys [id]}]
  {::pc/output [{:app.user/friends [:app.user/id
                                    :app.user/me?]}]
   ::pc/input  #{:app.user/id}}
  (let [me (user-id-from-csrf db anti-forgery-token)
        users (jdbc/execute! db ["SELECT id FROM app_user"])]
    {:app.user/friends (for [{:keys [id]} users]
                         {:app.user/id  id
                          :app.user/me? (= me id)})}))


(pc/defresolver message-body [{::keys [db]} {:keys [app.message/id]}]
  {::pc/input  #{:app.message/id}
   ::pc/output [:app.message/body]}
  (->> ["SELECT body FROM app_message WHERE id = ?" id]
       (jdbc/execute! db)
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

(def db
  {:dbtype   "postgresql"
   :dbname   "app"
   :host     "localhost"
   :user     "postgres"
   :password "postgres"})

(def service
  {:env                       :prod
   ::db                       db
   ::jdbc/conn                (jdbc/get-datasource db)
   ::http/port                8080
   ::pedestal/ui-index        ui-index
   ::pedestal/parser-gen      p/parallel-parser
   ::p/reader                 [p/map-reader
                               pc/all-parallel-readers
                               p/env-placeholder-reader]
   ::pc/mutation-join-globals [::fp/tempids]
   ::p/placeholder-prefixes   #{">"}
   ::p/mutate                 pc/mutate-async
   ::p/plugins                [(pc/connect-plugin {::pc/register [login exit friends
                                                                  index-data message-body set-title
                                                                  message-title chat-messages
                                                                  chat-with send-msg
                                                                  username-by-id]})
                               p/error-handler-plugin]
   ::http/resource-path       "public"
   ::http/host                "0.0.0.0"
   ::http/join?               false
   ::http/type                :jetty})


(defn install-schema!
  [{:keys [dbname] :as db} schemas]
  (jdbc/execute! (assoc db :dbname "")
                 [(format "DROP DATABASE IF EXISTS %s;" dbname)])
  (jdbc/execute! (assoc db :dbname "")
                 [(format "CREATE DATABASE %s;" dbname)])
  (jdbc/execute! db schemas))

(defonce http-state (atom nil))

(defn dev-start
  []
  (install-schema! (::db service) (mapv slurp [(io/resource "schema.sql")]))
  (swap! http-state (fn [st]
                      (when st
                        (http/stop st))
                      (-> (assoc service :env :dev)
                          pedestal/init-service
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
                          pedestal/init-service
                          http/create-server
                          http/start))))
