(ns veyeslack.models.utils
  (:require [clojure.string :as string]))


(defn normalize-str
  "trims trailing spaces and lower-cases string"
  [the-txt]
  (-> the-txt str string/trim string/lower-case))
