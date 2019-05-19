(ns my-next-stack.core-test
  (:require [my-next-stack.core :as mns]
            [clojure.test :refer [deftest]]
            [my-next-stack.testing :as t]
            [midje.sweet :refer :all]))

(deftest login-test
  (let [app (t/->app mns/service)]
    (fact
      (t/mutation app
                  `app.user/login {:app.user/id       "tempid"
                                   :app.user/username "abc"}
                  [:app.user/id])
      => {:app.user/id                      3
          :fulcro.client.primitives/tempids {"tempid" 3}})))
