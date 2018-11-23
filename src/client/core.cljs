(ns client.core
  (:require [fulcro.client :as fc]
            [fulcro.client.primitives :as prim :refer [defsc]]
            [fulcro.client.dom :as dom]
            [fulcro.client.mutations :refer-macros [defmutation]]
            [fulcro.client.data-fetch :as df]))

(defmutation app.counter/inc
  [params]
  (action [{:keys [state] :as env}]
    (swap! state update :n inc))
  (remote [env] true))

(defmutation app/done-todo
  [{:keys [todo/done? db/id]}]
  (action [{:keys [state] :as env}]
    (swap! state assoc-in [:todo/by-id id :todo/done?] done?))
  (remote [env] true))

(defsc Todo [this {:keys [db/id todo/text todo/done?]}]
  {:query         [:db/id :todo/text :todo/done?]
   :ident         [:todo/by-id :db/id]
   :initial-state (fn [{:keys [db/id]}] {:db/id      id
                                         :todo/text  (str "loading " id)
                                         :todo/done? false})}
  (dom/div
    (dom/button
      {:onClick #(prim/transact! this `[(app/done-todo ~{:todo/done? (not done?)
                                                         :db/id      id})])}
      (if done? "x" "v"))
    (dom/span (str text))))

(def ui-todo (prim/factory Todo {:keyfn :db/id}))

(defsc Root [this {:keys [app/todos]}]
  {:query         [{:app/todos (prim/get-query Todo)}]
   :initial-state (fn [_] {:app/todos []})}
  (dom/div
    (map ui-todo todos)
    (dom/button {:onClick #(df/load this :app/todos Todo)}
                "load")))

(defn started-callback
  [this]
  (df/load this :app/todos Todo))


(defonce client (atom (fc/new-fulcro-client
                        :started-callback started-callback)))


(defn render
  [target]
  (swap! client fc/mount Root target))



(defn ^:export main
  [target]
  (let []
    (render target)))
