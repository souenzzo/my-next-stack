(ns mns.build
  (:require [clojure.tools.build.api :as b]
            [shadow.cljs.devtools.api :as shadow.api]
            [shadow.cljs.devtools.server :as shadow.server]))

(def class-dir "target/classes")
(def uber-file "target/mns.jar")

(defn -main
  [& _]
  (let [basis (b/create-basis {:project "deps.edn"})]
    (b/delete {:path "target"})
    (shadow.server/start!)
    (shadow.api/release :client)
    (shadow.server/stop!)
    #_(b/copy-dir {:src-dirs   (:paths basis)
                   :target-dir class-dir})))
