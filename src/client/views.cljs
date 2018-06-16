(ns client.views
  (:require [re-frame.core :as rf]
            [material-ui :as mui]
            [reagent.core :as reagent]))

(def button
  (reagent/adapt-react-class mui/Button))

(defn hello
  []
  (let [n @(rf/subscribe [:n])
        text @(rf/subscribe [:text])]
    [:div
     [:code text]
     [:br]
     [:code (str n)]
     [:br]
     [button {:on-click #(rf/dispatch [:transact `[(app/add {:app/n 1})
                                                   :app/n]])} "+"]
     [:input {:value     text
              :on-change #(rf/dispatch-sync [:text (-> % .-target .-value)])}]]))
