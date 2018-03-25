(ns client.views
  (:require [re-frame.core :as rf]))

(defn hello
  []
  (let [text @(rf/subscribe [:text])]
    [:div
     [:code text]
     [:br]
     [:input {:value     text
              :on-change #(rf/dispatch-sync [:text (-> % .-target .-value)])}]]))
