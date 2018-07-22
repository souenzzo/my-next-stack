(defproject my-next-stack "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.0-alpha6"]
                 [buddy/buddy-sign "3.0.0"]
                 [com.cognitect/transit-clj "0.8.309"]
                 [com.cognitect/transit-cljs "0.8.256"]
                 [com.wsscode/pathom "2.0.15"]
                 [com.datomic/datomic-free "0.9.5697"]
                 [io.pedestal/pedestal.jetty "0.5.4"]
                 [io.pedestal/pedestal.service "0.5.4"]
                 ;; ignore logging
                 [org.slf4j/slf4j-nop "1.8.0-beta2"]
                 ;; conflicts
                 [com.google.guava/guava "25.1-jre"]
                 [commons-codec/commons-codec "1.11"]
                 [cheshire/cheshire "5.8.0"]
                 [org.clojure/tools.reader "1.3.0"]]
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
                                     [org.clojure/test.check "0.10.0-alpha3"]
                                     ;; conflicts
                                     [org.clojure/tools.nrepl "0.2.13"]]
                      :repl-options {:nrepl-middleware [cider.piggieback/wrap-cljs-repl]}
                      :main         user}})
