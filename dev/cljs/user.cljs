(ns cljs.user
  (:require [client.core :as client]))

(defn on-jsload
  []
  (let [target (.getElementById js/document "app")]
    (client/main target)))
