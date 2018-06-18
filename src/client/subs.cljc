(ns client.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub
  :todo/text
  (fn [db _]
    (:todo/text db "fobar")))

(rf/reg-sub
  :app/todos
  (fn [db _]
    (:app/todos db)))

(rf/reg-sub
  :n
  (fn [db _]
    (:app/n db)))
