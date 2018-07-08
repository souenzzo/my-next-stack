(ns cljs.user
  (:require [client.core :as client]))

(defn on-jsload
  []
  (client/main "app"))
