(ns client.atoms
  (:require [react :as r]
            [material-ui :as m]))

(defn factory-apply
  [class]
  (fn [props & children]
    (apply r/createElement class props children)))

(def button (factory-apply m/Button))

(def checkbox (factory-apply m/Checkbox))

(def text-field (factory-apply m/TextField))

(def table (factory-apply m/Table))

(def table-head (factory-apply m/TableHead))
(def table-body (factory-apply m/TableBody))
(def table-row (factory-apply m/TableRow))

(def table-cell (factory-apply m/TableCell))
