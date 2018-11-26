(ns cljs.user
  (:require [client.core :as client]))

(defn ^:dev/after-load on-jsload
  []
  (.log js/console "ok!!")

  (let [target (.getElementById js/document "app")]
    (client/render target)))



