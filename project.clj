(defproject my-next-stack "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.0-alpha6"]
                 [com.cognitect/transit-clj "0.8.309"]
                 [com.cognitect/transit-cljs "0.8.256"]
                 [com.wsscode/pathom "2.0.14"]
                 [io.pedestal/pedestal.jetty "0.5.4"]
                 [io.pedestal/pedestal.service "0.5.4"]
                 ;; ignore logging
                 [org.slf4j/slf4j-nop "1.8.0-beta2"]
                 ;; conflicts
                 [cheshire/cheshire "5.8.0"]
                 [org.clojure/tools.reader "1.3.0"]
                 [org.clojure/test.check "0.10.0-alpha3"]]
  :source-paths ["src"]
  :main server.core
  :profiles {:client {:source-paths  ["src"]
                      :clean-targets ^{:protect false} ["resources/public/js" "dist/"]
                      :dependencies  [[org.clojure/clojurescript "1.10.339"]
                                      [re-frame/re-frame "0.10.5"]
                                      [reagent/reagent "0.8.1" :exclusions [cljsjs/react
                                                                            cljsjs/react-dom
                                                                            cljsjs/react-dom-server
                                                                            cljsjs/create-react-class]]]}
             :dev    {:source-paths ["src" "dev" "test"]
                      :dependencies [[figwheel-sidecar/figwheel-sidecar "0.5.16"]
                                     [cider/piggieback "0.3.6"]
                                     [midje/midje "1.9.2"]
                                     ;; conflicts
                                     [org.clojure/tools.nrepl "0.2.13"]]
                      :repl-options {:nrepl-middleware [cider.piggieback/wrap-cljs-repl]}
                      :main         user}})
