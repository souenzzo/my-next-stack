(ns server.graphql-test
  (:require [clojure.test :refer [deftest use-fixtures]]
            [server.graphql :as gql]
            [midje.sweet :refer :all]
            [com.wsscode.pathom.diplomat.http :as http]
            [com.walmartlabs.lacinia :as lacinia]
            [clojure.string :as string]))

(deftest lacinia-demo
  (let [requests (atom [])
        api-url "http://my-facke.domain"
        http-driver (fn [{::http/keys [url] :as ctx}]
                      (swap! requests conj url)
                      (let [pattern (re-pattern (format "^%s/api" api-url))
                            path (string/replace url pattern "")]
                        (cond
                          (= path "/users") (for [i (range 3)]
                                              {:id        i
                                               :name      (str "Usuario - " i)
                                               :addresses [1 2 3]})
                          (string/starts-with? path "/address") (let [[_ address-id user-id] (re-matches #"/address/([^?]+)\?user-id=(.+)" path)]
                                                                  {:id     (bigint address-id)
                                                                   :street (str "street " address-id user-id)})
                          :else (-> (str "404: '" (pr-str url) "' não existe")
                                    (ex-info ctx)
                                    (throw)))))
        context {:api-url      "http://my-facke.domain"
                 ::http/driver http-driver}]
    (fact
      "Hello world"
      (lacinia/execute gql/schema
                       "{ hello }"
                       {}
                       context)
      => {:data {:hello "world"}})
    (fact
      "Simple user"
      (lacinia/execute gql/schema
                       "{ users { id name } }"
                       {}
                       context)
      => {:data {:users [{:id 0 :name "Usuario - 0"}
                         {:id 1 :name "Usuario - 1"}
                         {:id 2 :name "Usuario - 2"}]}})
    (fact
      "users with address"
      (lacinia/execute gql/schema
                       "{ users { id name addresses { id street } } }"
                       {}
                       context)
      => {:data {:users [{:addresses [{:id 1 :street "street 10"}
                                      {:id 2 :street "street 20"}
                                      {:id 3 :street "street 30"}]
                          :id        0
                          :name      "Usuario - 0"}
                         {:addresses [{:id 1 :street "street 11"}
                                      {:id 2 :street "street 21"}
                                      {:id 3 :street "street 31"}]
                          :id        1
                          :name      "Usuario - 1"}
                         {:addresses [{:id 1 :street "street 12"}
                                      {:id 2 :street "street 22"}
                                      {:id 3 :street "street 32"}]
                          :id        2
                          :name      "Usuario - 2"}]}})
    (fact
      "http calls"
      @requests
      => ["http://my-facke.domain/api/users"
          "http://my-facke.domain/api/users"
          "http://my-facke.domain/api/address/1?user-id=0"
          "http://my-facke.domain/api/address/2?user-id=0"
          "http://my-facke.domain/api/address/3?user-id=0"
          "http://my-facke.domain/api/address/1?user-id=1"
          "http://my-facke.domain/api/address/2?user-id=1"
          "http://my-facke.domain/api/address/3?user-id=1"
          "http://my-facke.domain/api/address/1?user-id=2"
          "http://my-facke.domain/api/address/2?user-id=2"
          "http://my-facke.domain/api/address/3?user-id=2"])))
