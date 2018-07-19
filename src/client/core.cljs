(ns client.core
  (:require [client.views :as views]
            [re-frame.core :as rf]
            [reagent.core :as r]))

(defn render
  [target]
  (r/render [views/root] target))

(defn ^:export main
  [target]
  #_(rf/dispatch [::init])
  (render target))

