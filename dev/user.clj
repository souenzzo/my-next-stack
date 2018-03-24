(ns user
  (:require [server.core :as server]
            [io.pedestal.http :as http]))

(defn restart
  []
  (when-let [srv @server/http-service]
    (http/stop srv))
  (server/-main))
