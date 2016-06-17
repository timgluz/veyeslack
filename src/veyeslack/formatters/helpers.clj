(ns veyeslack.formatters.helpers
  (:require [clojure.string :as string]))

(defn display-type [public?]
  (if public? "in_channel" "ephemeral"))

(defn to-safe-key [prod-key]
  (-> prod-key (string/replace #"\/" ":")))

(def veye-url  "https://www.versioneye.com")

(defn to-veye-url [& path-items]
  (->> path-items
    (cons veye-url)
    (interpose "/")
    (apply str)))


