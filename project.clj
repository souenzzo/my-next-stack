(defproject my-next-stack "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.0-beta4"]
                 [org.clojure/core.async "0.4.474"]
                 [io.pedestal/pedestal.interceptor "0.5.4"]
                 [buddy/buddy-sign "3.0.0"]
                 [com.cognitect/transit-clj "0.8.313"]
                 [com.wsscode/pathom "2.2.0-RC2"]
                 [com.walmartlabs/lacinia "0.30.0"]
                 [com.datomic/datomic-free "0.9.5697"]
                 [io.pedestal/pedestal.jetty "0.5.4"]
                 [io.pedestal/pedestal.service "0.5.4"]
                 [org.clojure/data.json "0.2.6"]
                 [walkable "1.0.0-SNAPSHOT"]
                 ;; ignore logging
                 [org.slf4j/slf4j-nop "1.8.0-beta2"]
                 ;; conflicts
                 [org.clojure/tools.analyzer.jvm "0.7.2"]
                 [com.google.guava/guava "25.1-jre"]
                 [commons-codec/commons-codec "1.11"]
                 [cheshire/cheshire "5.8.1"]
                 [org.clojure/tools.reader "1.3.2"]]
  :source-paths ["src"]
  :main server.core
  :profiles {:client {:source-paths  ["src"]
                      :clean-targets ^{:protect false} ["resources/public/js" "dist/"]
                      :dependencies  [[org.clojure/clojurescript "1.10.439"]
                                      [com.cognitect/transit-cljs "0.8.256"]
                                      [re-frame/re-frame "0.10.6"]
                                      [reagent/reagent "0.8.1" :exclusions [cljsjs/react
                                                                            cljsjs/react-dom
                                                                            cljsjs/react-dom-server
                                                                            cljsjs/create-react-class]]]}
             :dev    {:source-paths ["src" "dev" "test"]
                      :dependencies [[figwheel-sidecar/figwheel-sidecar "0.5.17"]
                                     [cider/piggieback "0.3.10"]
                                     [midje/midje "1.9.4"]
                                     [devcards/devcards "0.2.6" :exclusions [cljsjs/react
                                                                             cljsjs/react-dom
                                                                             cljsjs/marked
                                                                             cljsjs/create-react-class]]
                                     [org.clojure/test.check "0.10.0-alpha3"]
                                     ;; conflicts
                                     [org.clojure/tools.nrepl "0.2.13"]]
                      :repl-options {:nrepl-middleware [cider.piggieback/wrap-cljs-repl]}
                      :main         user}})
