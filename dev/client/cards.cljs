(ns client.cards
  (:require [client.views :as v]
            [devcards.core :as dc :include-macros true]
            [reagent.core :as r]))

(dc/defcard todo-item
  (dc/reagent (fn [state owner]
                (let [{:keys [text done?]} @state]
                  [v/ui-todo-item {:text      text
                                   :done?     done?
                                   :on-change #(swap! state assoc :done? %)}])))
  (r/atom {:text "Hello!" :done? false})
  {:inspect-data true})

(dc/defcard new-todo-item
  (dc/reagent (fn [state owner]
                (let [{:keys [text done?]} @state]
                  [v/ui-new-todo {:text    text
                                  :on-text #(swap! state assoc :text %)
                                  :on-add  #(swap! state assoc :text "")}])))
  (r/atom {:text "Hello!"})
  {:inspect-data true})

(dc/defcard auth
  (dc/reagent (fn [state owner]
                (let [{:keys [username two-factor-in-progress? two-factor-text authed?]} @state]
                  [v/ui-auth {:user                  {:user/username                username
                                                      :user/authed?                 authed?
                                                      :user/two-factor              two-factor-text
                                                      :user/two-factor-in-progress? two-factor-in-progress?}
                              :on-request-two-factor #(swap! state assoc :two-factor-in-progress? true)
                              :on-login              #(swap! state assoc :authed? true)
                              :on-logout             #(reset! state {})
                              :on-two-factor-text    #(swap! state assoc :two-factor-text %)
                              :on-username-text      #(swap! state assoc :username %)}])))
  (r/atom {:username "Hello!"})
  {:inspect-data true})
