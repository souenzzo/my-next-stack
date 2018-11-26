(ns user
  (:require [server.core :as server]
            [clojure.java.shell :as sh]
            [shadow.cljs.devtools.server :as shadow-srv]
            [shadow.cljs.devtools.api :as shadow]))

(defonce shadow-server
         (delay (shadow-srv/start!)))

(defn start
  []
  (time
    (do
      (prn [@shadow-server])
      (sh/sh "yarn" "install")
      (shadow/watch :main)
      (shadow/watch :cards))))

(defn restart
  []
  (server/-main))
