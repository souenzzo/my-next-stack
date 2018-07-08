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
                                                       "react-dom"
                                                       "material-ui"
                                                       "material-styles"
                                                       "cljsjs.react"
                                                       "cljsjs.react.dom"]
                                      :global-exports {react            React
                                                       material-ui      MaterialUI
                                                       material-styles  MaterialStyles
                                                       cljsjs.react.dom ReactDOM
                                                       cljsjs.react     React
                                                       react-dom        ReactDOM}}]


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
