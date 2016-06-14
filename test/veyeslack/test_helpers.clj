(ns veyeslack.test_helpers
  (:require [clojure.java.jdbc :as jdbc]))

(defn clean-tables!
  "truncates all the tables"
  [db-spec table-names]
  (doall
    (doseq  [tbl table-names]
      (jdbc/execute! db-spec
        [(format  "TRUNCATE TABLE %s RESTART IDENTITY CASCADE" tbl)]))))

(defmacro with-component-server
  [{:keys [system sleep] :or {sleep 50}} & body]
  "Evaluates code in context of initialized system and components"
  `(let [the-system# ~system]
     (try
       ~@body
       (finally
         (.stop (:app the-system#))
         (Thread/sleep ~sleep)))))
