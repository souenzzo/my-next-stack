(ns my-next-stack.user
  (:require [my-next-stack.pwa :as pwa]))

(defn after-load
  []
  (swap! pwa/app pwa/render))

