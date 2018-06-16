(ns client.fetch
  (:require [re-frame.core :as rf]
            [cognitect.transit :as transit]))

(def writer (transit/writer :json))
(def reader (transit/reader :json))

(def url "//localhost:8080/api")

(def headers (new js/Headers #js {:Accept       "application/transit+json"
                                  :Content-Type "application/transit+json"}))


(defn fetch
  [query callback]
  (let [body (transit/write writer query)
        fetch (.fetch js/window url #js {:method "POST" :headers headers :body body})]
    (.then (.then fetch (fn [response] (.text response)))
           (fn [text] (callback (transit/read reader text))))))

(rf/reg-fx
  ::send
  (fn [query]
    (fetch query #(rf/dispatch [::recive %]))))

(rf/reg-event-db
  ::recive
  (fn [db [_ data]] (merge db data)))
