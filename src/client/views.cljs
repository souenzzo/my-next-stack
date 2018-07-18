(ns client.views
  (:require [reagent.core :as r]))

(defn ui-todo-item
  [{:keys [todo/text todo/done?]}]
  [:div
   {:key text}
   [:input {:type     :checkbox
            :disabled true
            :checked  done?}]
   [:div text]])

(defn ui-todo-app
  [{:keys [app/todos todo.new/text]}]
  [:div
   [:div text]
   [:button "+"]
   [:hr]
   [:div (map ui-todo-item todos)]])

(def data
  {:db/id         0
   :todo.new/text "WIP"
   :app/todos     [{:db/id      1
                    :todo/text  "[clinet]Do network!"
                    :todo/done? false}
                   {:db/id      2
                    :todo/done? true
                    :todo/text  "[clinet]Keep trying"}]})

(defn root
  []
  [:div
   "My next stack"
   [ui-todo-app data]])
