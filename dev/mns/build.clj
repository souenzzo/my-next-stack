(ns mns.build
  (:require
    [clojure.java.io :as io]
    [clojure.tools.build.api :as b]
    [clojure.tools.cli :as cli]
    [shadow.cljs.devtools.api :as shadow.api]
    [shadow.cljs.devtools.server :as shadow.server]))

(def class-dir "target/classes")

(defn release
  [& {:keys [destination]
      :or   {destination "_site"}}]
  (b/delete {:path class-dir})
  (b/copy-dir {:src-dirs   ["resources"]
               :target-dir class-dir})
  (shadow.api/release :client)
  (b/copy-dir {:src-dirs   [(str (io/file class-dir "public"))]
               :target-dir destination}))

(defn -main
  [& args]
  (let [{:keys [options errors]} (cli/parse-opts args
                                   [["-d" "--destination DIR"
                                     :default "_site"]])]
    (binding [*out* *err*]
      (run! println errors))
    (shadow.server/start!)
    (release options)
    (shadow.server/stop!)
    (System/exit 0)))

(comment
  (shadow.server/start!)
  (release {:destination "_site"})
  (shadow.server/stop!))
