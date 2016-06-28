(ns veyeslack.models.utils
  (:require [clojure.string :as string]
            [clojure.java.jdbc :as jdbc]
            [cheshire.core :as json])
  (:import [org.postgresql.jdbc.PgArray]
           [org.postgresql.util PGobject]
           [java.sql Connection]))

(defn normalize-str
  "trims trailing spaces and lower-cases string"
  [the-txt]
  (-> the-txt str string/trim string/lower-case))

;;Postgres Vector support for JDBC
(defn vec->sql
  "converts array to sql array"
  [^Connection db-spec array-vector]
  (jdbc/with-db-connection [conn db-spec]
    (.createArrayOf (jdbc/get-connection conn)
                    "varchar"
                    (into-array String array-vector))))

(defn sql->vec
  "converts sql array to clj array"
  [sql-arr]
  (into [] (.getArray sql-arr)))

(extend-protocol jdbc/IResultSetReadColumn
  org.postgresql.jdbc.PgArray
  (result-set-read-column [pgobj metadata i]
    (vec (.getArray pgobj))))

(extend-protocol clojure.java.jdbc/ISQLParameter
  clojure.lang.IPersistentVector
  (set-parameter [v ^java.sql.PreparedStatement stmt ^long i]
    (jdbc/with-db-connection [conn (.getConnection stmt)]
      (let [meta (.getParameterMetaData stmt)
            type-name (.getParameterTypeName meta i)]
        (if-let [elem-type (when (= (first type-name) \_)
                              (apply str (rest type-name)))]
          (.setObject stmt i (.createArrayOf conn elem-type (to-array v)))
          (.setObject stmt i v))))))

;;Postgres JSONB support
;;source: https://github.com/siscia/postgres-type
(defn- add-jsonx-type [jsonx write-json read-json]
  {:pre [(or (= "json" jsonx) (= "jsonb" jsonx))
         (fn? write-json)
         (fn? read-json)]}
  (let [to-pgjson (fn [value] (doto  (PGobject.)
                                (.setType jsonx)
                                (.setValue (write-json value))))]

    (extend-protocol jdbc/ISQLValue
      clojure.lang.Sequential
      (sql-value [value]
        (to-pgjson value))
      clojure.lang.IPersistentMap
      (sql-value [value] (to-pgjson value)))

    (extend-protocol jdbc/IResultSetReadColumn
      PGobject
      (result-set-read-column [pgobj _metadata _index]
        (let [type  (.getType pgobj)
              value (.getValue pgobj)]
          (if (= type jsonx)
            (read-json value)
            value))))))

(def pg-jsonb-extension
  (add-jsonx-type "jsonb"
                   json/generate-string
                   #(json/parse-string %1 true)))
