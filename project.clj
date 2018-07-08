(defproject my-next-stack "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.0-alpha6"]
                 [com.cognitect/transit-clj "0.8.309"]
                 [com.cognitect/transit-cljs "0.8.256"]
                 [com.wsscode/pathom "2.0.11"]
                 [io.pedestal/pedestal.jetty "0.5.4"]
                 [io.pedestal/pedestal.service "0.5.4"]
                 ;; ignore logging
                 [org.slf4j/slf4j-nop "1.8.0-beta2"]
                 ;; conflicts
                 [cheshire/cheshire "5.8.0"]
                 [org.clojure/test.check "0.10.0-alpha3"]]
  :source-paths ["src"]
  :main server.core
  :profiles {:client {:source-paths  ["src"]
                      :clean-targets ^{:protect false} ["resources/public/js" "dist/"]
                      :dependencies  [[org.clojure/clojurescript "1.10.339"]
                                      [fulcrologic/fulcro "2.5.12"]]
                      :exclusions    [cljsjs/react cljsjs/react-dom]}
             :dev    {:source-paths ["src" "dev"]
                      :dependencies [[figwheel-sidecar/figwheel-sidecar "0.5.16"]
                                     [cider/piggieback "0.3.6"]
                                     [midje/midje "1.9.2-alpha4"]]
                      :repl-options {:nrepl-middleware [cider.piggieback/wrap-cljs-repl]}
                      :main         user}})
