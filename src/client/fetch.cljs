(ns client.fetch
  (:require [re-frame.core :as rf]
            [cognitect.transit :as t]))

(def writer (t/writer :json))
(def reader (t/reader :json))

(def headers #js{:Content-Type "application/transit+json"
                 :Accept       "application/transit+json"})

(defn fetch-fx
  [{:keys [api]}]
  (fn [{:keys [query then]}]
    (let [fetch (.fetch js/window api #js {:method  "POST"
                                           :headers headers
                                           :body    (t/write writer query)})]
      (.then (.then fetch (fn [res] (.text res))) (fn [text]
                                                    (rf/dispatch-sync (conj then (t/read reader text))))))))
