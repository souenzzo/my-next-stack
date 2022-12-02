(ns mns.main
  (:require [io.pedestal.http :as http]
            [io.pedestal.log :as log]))

(defonce *server
  (atom nil))

(defn -main
  [& _]
  (swap! *server
    (fn [server]
      (some-> server http/stop)
      (-> {::http/type          :jetty
           ::http/join?         false
           ::http/port          8080
           ::http/routes        #{}
           ::http/secure-headers nil
           ::http/file-path     "target/classes/public"
           ::http/resource-path "public"}
        http/default-interceptors
        http/create-server
        http/start))))

(defn dev-main
  [& _]
  (-main)
  (-> `shadow.cljs.devtools.server/start!
    requiring-resolve
    (apply []))
  (-> `shadow.cljs.devtools.api/watch
    requiring-resolve
    (apply [:client])))

(comment
  (dev-main))
