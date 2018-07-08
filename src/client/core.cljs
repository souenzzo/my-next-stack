(ns client.core
  (:require [client.views :as views]
            [fulcro.client :as fc]
            [fulcro.client.data-fetch :as df]
            [fulcro.client.network :as net]))

(def url "//localhost:8080/api")

(defn new-client
  [{:keys [url]}]
  (fc/new-fulcro-client
    :networking (net/fulcro-http-remote {:url url})
    :started-callback (fn [app] (df/load app :app/data views/Root))))

(defonce client (atom (new-client {:url url})))

(defn ^:export main
  [id]
  (swap! client fc/mount views/Root id))
