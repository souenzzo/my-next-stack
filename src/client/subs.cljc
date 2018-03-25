(ns client.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub
  :text
  (fn [db _]
    (:text db)))
