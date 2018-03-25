(ns client.events
  (:require [re-frame.core :as rf]))

(rf/reg-event-fx
  :init
  (fn [_ _]
    {:client.fetch/graphql "{ hello }"
     :db                   {:text "Olá mundo!!"}}))


(rf/reg-event-db
  :text
  (fn [db [_ text]]
    (assoc db :text text)))
