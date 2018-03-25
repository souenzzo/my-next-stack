(ns client.core
  (:require [reagent.core :as reagent]
            [client.subs]
            [client.views :as views]
            [client.events]
            [re-frame.core :as rf]))

(defn ^:export main
  [target]
  (rf/dispatch-sync [:init])
  (reagent/render [views/hello] target))
