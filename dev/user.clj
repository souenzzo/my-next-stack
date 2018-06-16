(ns user
  (:require [server.core :as server]
            [figwheel-sidecar.repl-api :as f]
            [io.pedestal.http :as http]))

(defn start
  []
  (f/start-figwheel!
    '{:builds [{:id           "dev"
                :source-paths ["src" "dev"]
                :figwheel     {:on-jsload cljs.user/on-jsload}
                :compiler     {:main            cljs.user
                               :asset-path      "/js/out"
                               :elide-asserts   false
                               :closure-defines {goog.asserts.ENABLE_ASSERTS true
                                                 goog.DEBUG                  true}
                               :output-dir      "resources/public/js/out"
                               :output-to       "resources/public/js/app.js"}}]}))

(defn cljs-repl
  []
  (f/cljs-repl))

(defn stop
  []
  (f/stop-figwheel!))

(defn restart
  []
  (server/-main))
