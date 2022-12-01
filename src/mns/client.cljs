(ns mns.client)


(defn start
  [& _]
  (prn :hello))


(defn after-load
  [& _]
  (prn :world))
