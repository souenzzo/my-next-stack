(ns client.cards
  (:require [client.views :as v]
            [devcards.core :as dc :include-macros true]
            [reagent.core :as r]))

(dc/defcard todo-item
  "**todo item**"
  (dc/reagent (fn [state owner]
                (let [{:keys [text done?]} @state]
                  [v/ui-todo-item {:text      text
                                   :done?     done?
                                   :on-change #(swap! state assoc :done? %)}])))
  (r/atom {:text "Hello!" :done? false})
  {:inspect-data true
   :history      true})


(dc/defcard new-todo-item
  "**todo item**"
  (dc/reagent (fn [state owner]
                (let [{:keys [text done?]} @state]
                  [v/ui-new-todo {:text    text
                                  :on-text #(swap! state assoc :text %)
                                  :on-add  #(swap! state assoc :text "")}])))
  (r/atom {:text "Hello!"})
  {:inspect-data true
   :history      true})
