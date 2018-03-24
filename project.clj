(defproject my-next-stack "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [com.walmartlabs/lacinia "0.25.0"]
                 [com.walmartlabs/lacinia-pedestal "0.7.0"]
                 [ch.qos.logback/logback-classic "1.2.3"]]
  :main server.core
  :profiles {:dev {:source-paths ["src" "dev"]
                   :main         user}})
