(ns client.atoms
  (:refer-clojure :exclude [list])
  (:require [react :as r]
            [material-ui.core :as m]))

(defn factory-apply
  [class]
  (fn [props & children]
    (apply r/createElement class props children)))

(def button (factory-apply m/Button))

(def input (factory-apply m/Input))

(def list-item (factory-apply m/ListItem))

(def list-item-text (factory-apply m/ListItemText))

(def list (factory-apply m/List))

(def divider (factory-apply m/Divider))

(def checkbox (factory-apply m/Checkbox))

(def text-field (factory-apply m/TextField))

(def table (factory-apply m/Table))

(def table-head (factory-apply m/TableHead))
(def table-body (factory-apply m/TableBody))
(def table-row (factory-apply m/TableRow))

(def table-cell (factory-apply m/TableCell))
