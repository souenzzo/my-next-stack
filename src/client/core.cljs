(ns client.core
  (:require [reagent.core :as reagent]))

(defn hello
  []
  [:div "Olá mundo!!"])

(defn ^:export main
  [target]
  (reagent/render hello target))
