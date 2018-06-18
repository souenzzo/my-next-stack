(ns client.atoms
  (:require [material-ui :as m]
            [reagent.core :as r]))

(def button (r/adapt-react-class m/Button))

(def checkbox (r/adapt-react-class m/Checkbox))

(def text-field (r/adapt-react-class m/TextField))

(def table (r/adapt-react-class m/Table))

(def table-head (r/adapt-react-class m/TableHead))
(def table-body (r/adapt-react-class m/TableBody))
(def table-row (r/adapt-react-class m/TableRow))

(def table-cell (r/adapt-react-class m/TableCell))

