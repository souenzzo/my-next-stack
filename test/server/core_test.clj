(ns server.core-test
  (:require [clojure.test :refer [deftest]]
            [midje.sweet :refer :all]))

(deftest example
  (fact
    (+ 1 2) => 3))
