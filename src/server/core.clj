(ns server.core
  (:gen-class)
  (:require [clojure.string :as string]
            [clojure.tools.reader.edn :as edn]
            [cognitect.transit :as transit]
            [com.wsscode.pathom.connect :as pc]
            [com.wsscode.pathom.core :as p]
            [io.pedestal.http :as http]))

(def indexes (atom {}))

(defmulti mutation-fn pc/mutation-dispatch)
(def defmutation
  (pc/mutation-factory mutation-fn indexes))


(defmulti resolver-fn pc/resolver-dispatch)
(def defresolver
  (pc/resolver-factory resolver-fn indexes))

(defmutation `app/add
             {::pc/args [:app/n]}
             (fn [{:keys [conn]} {:keys [app/n]}]
               (swap! conn update :n (fnil + 0) n)))

(defresolver `app-n
             {::pc/output [:app/n]}
             (fn [{:keys [conn]} _]
               {:app/n (:n @conn 0)}))


(defresolver `app-todos
             {::pc/output [{:app/todos [:todo/id
                                        :todo/done?
                                        :todo/text]}]}
             (fn [{:keys [conn]} _]
               {:app/todos (:app/todos @conn [])}))

(defmutation `todo/add-todo
             {::pc/args [:todo/text]}
             (fn [{:keys [conn]} {:keys [todo/text]}]
               (swap! conn (fn [{:keys [app/todos app/next-id]
                                 :or   {todos   []
                                        next-id 0}
                                 :as   st}]
                             (assoc st
                               :app/todos (conj todos {:todo/id   next-id
                                                       :todo/text text})
                               :app/next-id (inc next-id))))))

(defmutation `todo/check
             {::pc/args [:todo/id :todo/done?]}
             (fn [{:keys [conn]} {:keys [todo/id todo/done?]}]
               (swap! conn (fn [{:keys [app/todos]
                                 :or   {todos []}
                                 :as   st}]
                             (assoc st
                               :app/todos (for [todo todos]
                                            (if (= (:todo/id todo) id)
                                              (assoc todo :todo/done? done?)
                                              todo)))))))




(def parser
  (p/parser {::p/env {::p/reader             [p/map-reader pc/all-readers]
                      ::pc/mutate-dispatch   mutation-fn
                      ::pc/resolver-dispatch resolver-fn
                      ::pc/indexes           @indexes}
             :mutate pc/mutate}))

(def conn (atom {}))

(defn api
  [{:keys [body]}]
  {:body   (parser {:conn conn} body)
   :status 200})


(def read-writer
  {:name  ::read-writer
   :leave (fn [{{{:strs [accept]
                  :or   {accept "application/edn"}} :headers} :request
                :as                                           ctx}]
            (let [writer (cond (string/starts-with? accept "application/transit+json") (fn [data]
                                                                                         (fn [out]
                                                                                           (transit/write (transit/writer out :json) data)))
                               :else pr-str)]
              (update-in ctx [:response :body] writer)))
   :enter (fn [{{{:strs [content-type]
                  :or   {content-type "application/edn"}} :headers} :request
                :as                                                 ctx}]
            (let [reader (cond (string/starts-with? content-type "application/transit+json") (fn [in]
                                                                                               (transit/read (transit/reader in :json)))
                               :else (comp edn/read-string slurp))]
              (update-in ctx [:request :body] reader)))})
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
