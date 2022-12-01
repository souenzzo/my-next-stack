(ns mns.client
  (:require ["react" :as r]
            ["react-dom/client" :as rdc]))

(def ui-root
  (r/createElement "div" #js{} "Hello World"))

(defonce *root
  (atom nil))

(defn after-load
  [& _]
  (some-> @*root
    (.render ui-root)))


(defn start
  [& _]
  (-> "root"
    js/document.getElementById
    rdc/createRoot
    (->> (reset! *root)))
  (after-load))
