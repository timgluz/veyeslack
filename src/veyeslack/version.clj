(ns veyeslack.version)

(def version
  {:major 0
   :minor 3
   :patch 3
   :snapshot? false
   :build nil})

(defn as-semver []
  (str (:major version) "." (:minor version) "." (:patch version)
       (when (:snapshot? version)
         "-SNAPSHOT")
       (when (:build version)
         (str "+" (:build version)))))
