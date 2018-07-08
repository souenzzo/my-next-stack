(ns client.views
  (:require [client.atoms :as a]
            [fulcro.client.primitives :as prim :refer [defsc]]
            [fulcro.client.dom :as dom]))


(defsc TodoItem
  [this {:keys [todo/text todo/done?]}]
  {:query [:todo/text :todo/done? :db/id]}
  (a/list-item #js {:dense  true
                    :button true}
               (a/checkbox #js {:checked done?})
               (a/list-item-text #js {:primary text})))

(def ui-todo-item (prim/factory TodoItem {:keyfn :db/id}))

(defsc TodoApp
  [this {:keys [app/todos todo.new/text]}]
  {:query [:db/id
           {:app/todos (prim/get-query ui-todo-item)}
           :todo.new/text]}
  (dom/div #js {}
           (a/input #js {:value text})
           (a/button #js {} "+")
           (dom/hr)
           (a/list #js {} (map ui-todo-item todos))))

(def ui-todo-app (prim/factory TodoApp {:keyfn :db/id}))

(def data
  {:db/id         0
   :todo.new/text "WIP"
   :app/todos     [{:db/id      1
                    :todo/text  "[clinet]Do fulcro network!"
                    :todo/done? false}
                   {:db/id      2
                    :todo/done? true
                    :todo/text  "[clinet]Keep trying"}]})

(defsc Root
  [this _]
  ;[this {:keys [todo/data]}]
  {:query [{:todo/data (prim/get-query ui-todo-app)}]}
  (prn data)
  (dom/div #js {}
           "My next stack"
           (ui-todo-app data)))
