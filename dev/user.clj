(ns user
  (:require [server.core :as server]
            [figwheel-sidecar.repl-api :as f]
            [clojure.java.shell :as sh]))

(def compiler
  '{:infer-externs   true
    :npm-deps        false
    :foreign-libs    [{:file           "dist/index_bundle.js"
                       :provides       ["create-react-class"
                                        "react"
                                        "material-ui.icons"
                                        "cljsjs.react"
                                        "cljsjs.marked"
                                        "cljsjs.react.dom"
                                        "material-ui.core"
                                        "material-ui.core.styles"
                                        "react-dom"]
                       :global-exports {react                   React
                                        cljsjs.react            React
                                        react-dom               ReactDOM
                                        cljsjs.react.dom        ReactDOM
                                        create-react-class      CreateReactClass
                                        cljsjs.marked           marked
                                        material-ui.core        MaterialUI
                                        material-ui.icons       Icons
                                        material-ui.core.styles MaterialStyles}}]
    :closure-defines {goog.asserts.ENABLE_ASSERTS true
                      goog.DEBUG                  true}
    :elide-asserts   false})

(def dev-build
  {:id           "dev"
   :source-paths ["src" "dev"]
   :figwheel     '{:on-jsload cljs.user/on-jsload}
   :compiler     (assoc compiler
                   :main 'cljs.user
                   :asset-path "/js/out"
                   :output-dir "resources/public/js/out"
                   :output-to "resources/public/js/app.js")})

(def card-build
  {:id           "card"
   :source-paths ["src" "dev"]
   :figwheel     {:devcards true}
   :compiler     (assoc compiler
                   :main 'client.cards
                   :source-map-timestamp true
                   :asset-path "/js/cards"
                   :output-dir "resources/public/js/cards"
                   :output-to "resources/public/js/cards.js")})

(defn start
  []
  (time
    (do (sh/sh "yarn" "install")
        (sh/sh "yarn" "webpack")
        (f/start-figwheel! {:builds          [card-build
                                              dev-build]
                            :builds-to-start ["dev" "card"]}))))

(defn cljs-repl
  []
  (f/cljs-repl "card"))

(defn stop
  []
  (f/stop-figwheel!))

(defn restart
  []
  (server/-main))
