(defproject my-next-stack "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.0-beta8"]
                 [org.clojure/core.specs.alpha "0.2.36"]]
  :source-paths ["src"]
  :resource-paths ["resources"]
  :clean-targets ["target" "dist"]
  :profiles {:uberjar {:main       server.core
                       :aot        :all
                       #_#_:jar-exclusions [#"public/js/test" #"public/js/workspaces" #"public/workspaces.html"]
                       :prep-tasks ["clean" ["clean"]
                                    "compile" ["with-profile" "client" "run" "-m" "shadow.cljs.devtools.cli" "release" "main"]]}

             :server  {:dependencies [[org.clojure/core.async "0.4.490"]
                                      [buddy/buddy-sign "3.0.0"]
                                      [com.cognitect/transit-clj "0.8.313"]
                                      [com.wsscode/pathom "2.2.0-RC5"]
                                      [io.pedestal/pedestal.jetty "0.5.4"]
                                      [io.pedestal/pedestal.service "0.5.4"]
                                      [cheshire/cheshire "5.8.1"]
                                      [walkable "1.1.0-SNAPSHOT"]
                                      [org.clojure/java.jdbc "0.7.8"]
                                      [org.postgresql/postgresql "42.2.5"]
                                      ;; ignore logging
                                      [org.slf4j/slf4j-nop "1.8.0-beta2"]


                                      ;; conflicts
                                      [com.google.guava/guava "27.0.1-jre"]

                                      ;; to remove:
                                      [com.datomic/datomic-free "0.9.5697"]
                                      [com.walmartlabs/lacinia "0.30.0"]]
                       :main         server.core}
             :client  {:dependencies [[org.clojure/clojurescript "1.10.439"]
                                      [thheller/shadow-cljs "2.7.3"]
                                      [com.cognitect/transit-cljs "0.8.256"]
                                      [fulcrologic/fulcro "2.6.15" :exclusions [cljsjs/react
                                                                                cljsjs/react-dom
                                                                                cljsjs/react-dom-server]]]}
             :test    {:source-paths ["src" "test"]
                       :dependencies [[midje/midje "1.9.4"]
                                      [org.clojure/test.check "0.10.0-alpha3"]]}
             :webdev  {:source-paths   ["src" "webdev"]
                       :resource-paths ["target"]
                       :dependencies   [[binaryage/devtools "0.9.10"]
                                        [fulcrologic/fulcro-inspect "2.2.4"]]
                       :main           user}
             :dev     {:source-paths ["src" "dev"]
                       :main         user}})
