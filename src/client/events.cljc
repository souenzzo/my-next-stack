(ns client.events
  (:require [re-frame.core :as rf]))

(rf/reg-event-fx
  :init
  (fn [_ _]
    {:client.fetch/send [:app/n]
     :db                {:text "Olá mundo!!"}}))


(rf/reg-event-fx
  :transact
  (fn [{:keys [db]} [_ query]]
    {:client.fetch/send query}))

(rf/reg-event-db
  :text
  (fn [db [_ text]]
    (assoc db :text text)))
