(ns client.core
  (:require [client.views :as views]
            [reagent.core :as r]))

(defn ^:export main
  [target]
  (r/render  [views/root] target))
