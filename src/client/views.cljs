(ns client.views
  (:require [re-frame.core :as rf]
            [client.atoms :as a]
            [reagent.core :as reagent]))


(defn todo-app
  [{:keys [todo/text todos]}]
  (let []
    [:div
     [a/text-field {:value     text
                    :on-change #(rf/dispatch-sync [:todo/text (-> % .-target .-value)])}]
     [a/button {:on-click #(rf/dispatch [:transact `[(todo/add-todo {:todo/text ~text})
                                                     {:app/todos [:todo/text
                                                                  :todo/done?
                                                                  :todo/id]}]])} "+"]
     [:hr]
     [a/table
      [a/table-head
       [a/table-row
        [a/table-cell "_"]
        [a/table-cell "_"]
        [a/table-cell "_"]]]
      [a/table-body
       (for [{:keys [todo/text todo/id todo/done?]} todos]
         [a/table-row
          {:key id}
          [a/table-cell [a/checkbox {:checked   done?
                                     :on-change (fn [e checked?]
                                                  (rf/dispatch [:transact `[(todo/check ~{:todo/id    id
                                                                                          :todo/done? checked?})
                                                                            {:app/todos [:todo/text
                                                                                         :todo/done?
                                                                                         :todo/id]}]]))}]]
          [a/table-cell text]
          [a/table-cell [a/checkbox]]])]]]))

(defn hello
  []
  (let [text @(rf/subscribe [:todo/text])
        todos @(rf/subscribe [:app/todos])]
    [todo-app {:todo/text text :todos todos}]))
