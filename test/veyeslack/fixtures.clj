(ns veyeslack.fixtures
  (:require [clojure.test :refer :all]
            [veyeslack.test_helpers :refer [clean-tables!]]))

(defn make-table-truncate-fixture
  [db-spec table-names]
  (fn [f]
    (clean-tables! db-spec table-names)
    (f)
    (clean-tables! db-spec table-names)))
