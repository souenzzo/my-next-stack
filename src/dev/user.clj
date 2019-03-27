(ns user
  (:require [my-next-stack.core :as server]
            [shadow.cljs.devtools.server :as shadow.server]
            [shadow.cljs.devtools.api :as shadow.api]))

(defonce shadow-server
         (delay (shadow.server/start!)))

(defn -main
  [& _]
  (server/dev-start)
  (prn [@shadow-server])
  (shadow.api/watch :workspaces)
  (shadow.api/watch :pwa))
