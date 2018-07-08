(ns user
  (:require [server.core :as server]
            [figwheel-sidecar.repl-api :as f]
            [clojure.java.shell :as sh]))

(def dev-build
  '{:id           "dev"
    :source-paths ["src" "dev"]
    :figwheel     {:on-jsload cljs.user/on-jsload}
    :compiler     {:main            cljs.user
                   :asset-path      "/js/out"
                   :elide-asserts   false

                   :infer-externs   true
                   :npm-deps        false
                   :foreign-libs    [{:file           "dist/index_bundle.js"
                                      :provides       ["react"
                                                       "cljsjs.react"
                                                       "react-dom"
                                                       "cljsjs.react.dom"
                                                       "material-ui.core"
                                                       "material-ui.core.styles"]
                                      :global-exports {react                   React
                                                       cljsjs.react            React
                                                       react-dom               ReactDOM
                                                       cljsjs.react.dom        ReactDOM
                                                       material-ui.core        MaterialUI
                                                       material-ui.core.styles MaterialStyles}}]


                   :closure-defines {goog.asserts.ENABLE_ASSERTS true
                                     goog.DEBUG                  true}
                   :output-dir      "resources/public/js/out"
                   :output-to       "resources/public/js/app.js"}})

(defn start
  []
  (time
    (do (sh/sh "yarn" "install")
        (sh/sh "yarn" "webpack")
        (f/start-figwheel! {:builds [dev-build]}))))

(defn cljs-repl
  []
  (f/cljs-repl))

(defn stop
  []
  (f/stop-figwheel!))

(defn restart
  []
  (server/-main))
