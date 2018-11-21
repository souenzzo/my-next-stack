(defproject my-next-stack "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.0-beta8"]
                 [org.clojure/core.specs.alpha "0.2.36"]]
  :source-paths ["src"]
  :resource-paths ["resources"]
  :clean-targets ["target" "dist"]
  :profiles {:server {:dependencies [[org.clojure/core.async "0.4.490"]
                                     [buddy/buddy-sign "3.0.0"]
                                     [com.cognitect/transit-clj "0.8.313"]
                                     [com.wsscode/pathom "2.2.0-RC4"]
                                     [io.pedestal/pedestal.jetty "0.5.4"]
                                     [io.pedestal/pedestal.service "0.5.4"]
                                     [org.clojure/data.json "0.2.6"]
                                     [walkable "1.1.0-SNAPSHOT"]
                                     [org.clojure/java.jdbc "0.7.8"]
                                     [org.postgresql/postgresql "42.2.5"]
                                     ;; ignore logging
                                     [org.slf4j/slf4j-nop "1.8.0-beta2"]

                                     [com.datomic/datomic-free "0.9.5697"]
                                     [com.walmartlabs/lacinia "0.30.0"]]
                      :main         server.core}
             :client {:dependencies [[org.clojure/clojurescript "1.10.439"]
                                     [com.cognitect/transit-cljs "0.8.256"]
                                     [re-frame/re-frame "0.10.6"]
                                     [reagent/reagent "0.8.1" :exclusions [cljsjs/react
                                                                           cljsjs/react-dom
                                                                           cljsjs/react-dom-server
                                                                           cljsjs/create-react-class]]]}
             :test   {:source-paths ["src" "test"]
                      :dependencies [[midje/midje "1.9.4"]
                                     [org.clojure/test.check "0.10.0-alpha3"]]}
             :webdev {:source-paths ["src" "dev"]
                      :dependencies [[figwheel-sidecar/figwheel-sidecar "0.5.17"]
                                     [cider/piggieback "0.3.10"]
                                     [devcards/devcards "0.2.6" :exclusions [cljsjs/react
                                                                             cljsjs/react-dom
                                                                             cljsjs/marked
                                                                             cljsjs/create-react-class]]]
                      :repl-options {:nrepl-middleware [cider.piggieback/wrap-cljs-repl]}
                      :main         user}
             :dev    {:source-paths ["src" "dev"]
                      :main         user}})
