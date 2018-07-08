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

(defresolver `todo-data
             {::pc/output [:todo/data]}
             (fn [_ _] {:todo/data {}}))

(defresolver `app-data
             {::pc/output [:app/data]}
             (fn [_ _] {:app/data {}}))


(defresolver `app-todos
             {::pc/output [{:app/todos [:db/id
                                        :todo/done?
                                        :todo/text]}]}
             (fn [_ _]
               {:app/todos [{:db/id      1
                             :todo/text  "Do fulcro network!"
                             :todo/done? false}]}))

(def parser
  (p/parser {::p/env {::p/reader             [p/map-reader pc/all-readers]
                      ::pc/mutate-dispatch   mutation-fn
                      ::pc/resolver-dispatch resolver-fn
                      ::pc/indexes           @indexes}
             :mutate pc/mutate}))

#_(defonce conn (atom {}))

(defn api
  [{:keys [body]}]
  {:body   (parser {} body)
   :status 200})

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
                                                                  (transit/write (transit/writer out :json) data)))
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
