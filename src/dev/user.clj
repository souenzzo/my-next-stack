(ns user
  (:require [my-next-stack.core :as server]
            [shadow.cljs.devtools.server :as shadow.server]
            [shadow.cljs.devtools.api :as shadow.api]))

(defonce shadow-server
         (atom nil))

(defn -main
  [& _]
  (server/dev-start)
  (swap! shadow-server #(or % (shadow.server/start!)))
  (shadow.api/watch :workspaces)
  (shadow.api/watch :pwa))


(defn stop!
  [& _]
  (server/dev-stop)
  (swap! shadow-server #(when % (shadow.server/stop!))))
