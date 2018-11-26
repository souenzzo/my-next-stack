(ns client.user
  (:require [client.core :as client]))

(defn on-jsload
  []
  (let [target (.getElementById js/document "app")]
    (client/render target)))
