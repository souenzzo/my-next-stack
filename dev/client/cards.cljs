(ns client.cards
  (:require [client.views])
  (:require-macros [devcards.core :as dc]))

(defn ui-hello
  [& _]
  [:div "hello!!!!"])

(dc/defcard my-first-card
  (dc/reagent ui-hello))
