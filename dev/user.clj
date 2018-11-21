(ns user
  (:require [server.core :as server]
            [figwheel-sidecar.repl-api :as f]
            [clojure.java.shell :as sh]
            [clojure.java.io :as io]
            [clojure.edn :as edn]))

(def compiler
  (-> (io/resource "build.edn")
      slurp
      edn/read-string
      (assoc :elide-asserts false
             :fn-invoke-direct false
             :static-fns false
             :optimizations :none)
      (update :closure-defines merge '{goog.asserts.ENABLE_ASSERTS true
                                       goog.DEBUG                  true})))


(def dev-build
  {:id           :dev
   :source-paths ["src" "dev"]
   :figwheel     '{:on-jsload cljs.user/on-jsload}
   :compiler     (assoc compiler
                   :main 'cljs.user
                   :output-dir "target/public/js/out"
                   :asset-path "/js/out"
                   :preloads '[devtools.preload
                               fulcro.inspect.preload])})

(def card-build
  {:id           :card
   :source-paths ["src" "test" "dev"]
   :figwheel     {:devcards true}
   :compiler     (assoc compiler
                   :preloads '[devcards.core
                               devtools.preload
                               fulcro.client.cards]
                   :main 'client.cards
                   :source-map-timestamp true
                   :asset-path "/js/cards"
                   :output-dir "target/public/js/cards"
                   :output-to "target/public/js/cards.js")})

(defn start
  []
  (let [builds [card-build dev-build]]
    (time
      (do (sh/sh "yarn" "install")
          (sh/sh "yarn" "webpack" "--mode=development")
          (f/start-figwheel! {:builds          builds
                              :builds-to-start (mapv :id builds)})))))

(defn cljs-repl
  []
  (f/cljs-repl "card"))

(defn stop
  []
  (f/stop-figwheel!))

(defn restart
  []
  (server/-main))
