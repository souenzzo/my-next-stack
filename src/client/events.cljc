(ns client.events
  (:require [re-frame.core :as rf]))

(rf/reg-event-db
  :init
  (fn [_ _]
    {:text "Olá mundo!!"}))


(rf/reg-event-db
  :text
  (fn [db [_ text]]
    (assoc db :text text)))
