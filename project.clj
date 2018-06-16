(defproject my-next-stack "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [com.cognitect/transit-clj "0.8.309"]
                 [com.cognitect/transit-cljs "0.8.256"]
                 [com.wsscode/pathom "2.0.4"]
                 [io.pedestal/pedestal.jetty "0.5.3"]
                 [io.pedestal/pedestal.service "0.5.3"]]
  :source-paths ["src"]
  :main server.core
  :profiles {:client {:source-paths  ["src"]
                      :clean-targets ^{:protect false} ["resources/public/js"]
                      :dependencies  [[org.clojure/clojurescript "1.10.312"]
                                      [re-frame/re-frame "0.10.5"]
                                      [reagent/reagent "0.8.1"]]}
             :dev    {:source-paths ["src" "dev"]
                      :dependencies [[figwheel-sidecar/figwheel-sidecar "0.5.16"]
                                     [com.cemerick/piggieback "0.2.2"]
                                     [org.clojure/tools.nrepl "0.2.13"]]
                      :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
                      :main         user}})
