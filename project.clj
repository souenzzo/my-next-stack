(defproject my-next-stack "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [com.walmartlabs/lacinia "0.25.0"]
                 [com.walmartlabs/lacinia-pedestal "0.7.0"]]
  :source-paths ["src"]
  :main server.core
  :profiles {:client {:source-paths  ["src"]
                      :jvm-opts      ["--add-modules" "java.xml.bind"]
                      :clean-targets ^{:protect false} ["resources/public/js"]
                      :dependencies  [[org.clojure/clojurescript "1.10.217"]
                                      [re-frame/re-frame "0.10.5"]
                                      [reagent/reagent "0.8.0-alpha2"]]}
             :dev    {:source-paths ["src" "dev"]
                      :dependencies [[figwheel-sidecar/figwheel-sidecar "0.5.15"]
                                     [com.cemerick/piggieback "0.2.2"]
                                     [org.clojure/tools.nrepl "0.2.13"]]
                      :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
                      :main         user}})
