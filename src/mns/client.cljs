(ns mns.client
  (:require ["react" :as r]
            ["react-dom/client" :as rdc]))

(defn ui-counter
  []
  (let [[n set-n] (r/useState 0)]
    (r/createElement "div" #js{}
      (r/createElement "button" #js{:onClick (fn [evt]
                                               (set-n (dec n)))}
        "-")
      (str n)
      (r/createElement "button" #js{:onClick (fn [evt]
                                               (set-n (inc n)))}
        "+"))))

(def ui-root
  (r/createElement "div" #js{}
    "Hello World"
    (r/createElement ui-counter)))

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
