(ns server.graphql
  (:require [com.walmartlabs.lacinia.schema :as lacinia.schema]
            [com.wsscode.pathom.diplomat.http :as http]))

(defn resolve-hello
  [ctx _ _]
  "world")

(defn resolve-users
  [{:keys [api-url] :as ctx} _ _]
  (http/request (assoc ctx
                  ::http/method ::http/get
                  ::http/url (str api-url "/api/users"))))

(defn resolve-addresses
  [{:keys [api-url] :as ctx} _ {:keys [id addresses]}]
  (for [address-id addresses]
    (http/request (assoc ctx
                    ::http/url (str api-url "/api/address/" address-id "?user-id=" id)
                    ::http/method ::http/get))))

(def schema
  (-> {:queries {:hello {:type    'String
                         :resolve resolve-hello}
                 :users {:type    '(list :user)
                         :resolve resolve-users}}
       :objects {:address {:fields {:id     {:type 'Int}
                                    :street {:type 'String}}}
                 :user    {:fields {:id        {:type 'Int}
                                    :name      {:type 'String}
                                    :addresses {:type    '(list :address)
                                                :resolve resolve-addresses}}}}}
      lacinia.schema/compile))

