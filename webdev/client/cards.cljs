(ns client.cards
  (:require [client.views :as v]
            #_[devcards.core :as dc :include-macros true]
            [reagent.core :as r]))

#_(dc/defcard todo-item
    (dc/reagent (fn [state owner]
                  (let [{:keys [text done?]} @state]
                    [v/ui-todo-item {:text      text
                                     :done?     done?
                                     :on-change #(swap! state assoc :done? %)}])))
    (r/atom {:text "Hello!" :done? false})
    {:inspect-data true})

#_(dc/defcard new-todo-item
    (dc/reagent (fn [state owner]
                  (let [{:keys [text done?]} @state]
                    [v/ui-new-todo {:text    text
                                    :on-text #(swap! state assoc :text %)
                                    :on-add  #(swap! state assoc :text "")}])))
    (r/atom {:text "Hello!"})
    {:inspect-data true})

(defn box
  [{:keys [shape x y on-click]}]
  (let [+x #(+ % (* 10 x))
        +y #(+ % (* 10 y))]
    [:<>
     [:rect {:x        (+x 1)
             :y        (+y 1)
             :style    {:fill-opacity ".1"}
             :on-click on-click
             :width    8
             :height   8}]
     (case shape
       :circle [:circle {:r            4
                         :cx           (+x 5)
                         :cy           (+y 5)
                         :stroke-width 1
                         :stroke       :black
                         :fill         :none}]
       :cross [:<>
               [:line {:x1     (+x 9)
                       :x2     (+x 1)
                       :y1     (+y 9)
                       :y2     (+y 1)
                       :stroke :black}]
               [:line {:x1     (+x 9)
                       :x2     (+x 1)
                       :y1     (+y 1)
                       :y2     (+y 9)
                       :stroke :black}]]
       nil)]))

(defn grid
  []
  [:<>
   [:line {:x1     10
           :x2     10
           :y1     0
           :y2     30
           :stroke :black}]
   [:line {:x1     20
           :x2     20
           :y1     0
           :y2     30
           :stroke :black}]
   [:line {:x1     0
           :x2     30
           :y1     10
           :y2     10
           :stroke :black}]
   [:line {:x1     0
           :x2     30
           :y1     20
           :y2     20
           :stroke :black}]])

(def empty-state
  {:shape :circle
   :table (vec (repeat 3 (vec (repeat 3 :_))))})


#_(dc/defcard tic-tac-toe
    (dc/reagent (fn [state owner]
                  (let [{:keys [shape x y table]} @state]
                    [:div
                     [:button {:on-click #(reset! state empty-state)} "cleanup"]
                     [:svg {:view-box "0 0 100 100"}
                      (for [i (range 3)
                            j (range 3)]
                        [box {:key      (str [i j])
                              :on-click (fn []
                                          (swap! state (comp
                                                         #(update % :table assoc-in [i j] shape)
                                                         #(update % :shape {:circle :cross :cross :circle}))))
                              :x        i
                              :y        j
                              :shape    (get-in table [i j])}])]])))
    (r/atom empty-state)
    {:inspect-data true})


#_(dc/defcard auth
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
