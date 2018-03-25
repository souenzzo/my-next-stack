(ns client.fetch
  (:require [re-frame.core :as rf]))

(def url "//localhost:8888/graphql")

(def headers (new js/Headers #js {:Content-Type "application/json"}))

(rf/reg-fx
  ::graphql
  (fn [query]
    (.log js/console "ok!!")
    (let [body (.stringify js/JSON #js {:query query})
          fetch (.fetch js/window url #js {:method "POST" :headers headers :body body})]
      (.then (.then fetch (fn [text] (.json text)))
             (fn [json]
               (let [data (js->clj json :keywordize-keys true)]
                 (.log js/console json)
                 (prn data)))))))