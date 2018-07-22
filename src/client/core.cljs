(ns client.core
  (:require [client.views :as views]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [client.fetch :as fetch]))

(def api "http://localhost:8080/api")

(rf/reg-fx :fetch (fetch/fetch-fx {:api api}))

(defn render
  [target]
  (r/render [views/root] target))

(defn ^:export main
  [target]
  #_(rf/dispatch [::init])
  (render target))
