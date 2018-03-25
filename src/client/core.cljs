(ns client.core
  (:require [reagent.core :as reagent]
            [client.subs]
            [client.views :as views]
            [client.fetch]
            [client.events]
            [re-frame.core :as rf]))

(defn ^:export main
  [target]
  (rf/dispatch [:init])
  (reagent/render [views/hello] target))
