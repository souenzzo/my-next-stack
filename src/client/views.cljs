(ns client.views
  (:require [client.atoms :as a]
            [fulcro.client.primitives :as prim :refer [defsc]]
            [fulcro.client.dom :as dom]))


(defsc TodoItem
  [this {:keys [todo/text todo/done? db/id]}]
  {:query [:todo/text :todo/done? :db/id]}
  (dom/div #js {:key id}
           (str [id done? text])))

(def ui-todo-item (prim/factory TodoItem {:keyfn :db/id}))

(defsc TodoApp
  [this {:keys [app/todos todo/text] :as x}]
  {:query [{:app/todos (prim/get-query ui-todo-item)}
           :todo/text]}
  (do (prn [:xx x])
      (dom/div #js {}
               (dom/input #js {:value text})
               (dom/hr)
               (map ui-todo-item todos))))

(def ui-todo-app (prim/factory TodoApp))

(def data
  {:todo/text "WIP"
   :app/todos [{:db/id      1
                :todo/done? false
                :todo/text  "aaa"}]})

(defsc Root
  [this {:keys [#_todo/data]}]
  {:query [{:todo/data (prim/get-query ui-todo-app)}]}
  (dom/div #js {}
           "My next stack"
           (ui-todo-item data)))
