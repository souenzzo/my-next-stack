(ns my-next-stack.testing
  (:require [souenzzo.pedestal :as pedestal]
            [clojure.string :as string]
            [clojure.edn :as edn]
            [io.pedestal.test :refer [response-for]]
            [io.pedestal.http :as http]))

(defn ->app
  [service-map]
  (let [app (-> service-map
                (assoc :env :dev)
                pedestal/init-service
                http/create-servlet)]
    (assoc app
      ::last-return (atom nil)
      ::session-headers (atom nil))))

(defn last-return
  [app]
  (-> app ::last-return deref))

(defn request->response-for
  [{:keys [url method] :as req}]
  (into [method url]
        cat
        (for [[k v] (dissoc req :url :method)]
          [k v])))

(defn request
  [app req]
  (let [service-fn (::http/service-fn app)
        last-return (::last-return app)
        {:keys [headers body]
         :as   response} (apply response-for service-fn (request->response-for req))
        return (cond
                 (= (get headers "Content-Type")
                    "application/edn") (assoc response
                                         :body (edn/read-string body))
                 :else response)]
    (reset! last-return return)))

(defn session-headers!
  [{::keys [session-headers]
    :as    app}]
  (or @session-headers
      (swap! session-headers (fn [st]
                               (or st
                                   (let [{:keys [headers body]} (request app {:url "/api" :method :get})
                                         cookie (-> headers (get "Set-Cookie") first (string/split #";") first)
                                         x-csrf-token (-> body :io.pedestal.http.csrf/anti-forgery-token)]
                                     (assoc st
                                       "Content-Type" "application/edn"
                                       "Cookie" cookie
                                       "X-CSRF-Token" x-csrf-token)))))))

(defn api
  [app query]
  (let [headers (session-headers! app)]
    (:body (request app {:url     "/api"
                         :method  :post
                         :headers headers
                         :body    (pr-str query)}))))

(defn query
  [app key params query]
  (-> (api app `[{(~key ~params) ~query}])
      (get key)))

(defn mutation
  [app key params query]
  (-> (api app `[{(~key ~params) ~query}])
      (get key)))
