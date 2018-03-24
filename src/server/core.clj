(ns server.core
  (:gen-class)
  (:require [io.pedestal.http :as http]
            [com.walmartlabs.lacinia.pedestal :as lacinia]
            [com.walmartlabs.lacinia.schema :as schema]
            [com.walmartlabs.lacinia.parser.schema :as parser.schema]
            [clojure.java.io :as io]))

(def resolvers
  {:Query {:hello (fn [& args] "hello")}})

(def graphql-schema
  (-> (io/resource "schema.graphql")
      slurp
      (parser.schema/parse-schema {:resolvers resolvers})
      schema/compile))

(def service
  (-> graphql-schema
      (lacinia/service-map {:graphiql true})
      http/create-server))

(defonce http-service (atom nil))

(defn -main
  [& argv]
  (println "\nCreating your server...")
  (->> (http/start service)
       (reset! http-service)))

