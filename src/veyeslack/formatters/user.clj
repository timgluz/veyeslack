(ns veyeslack.formatters.user
  (:require [veyeslack.formatters.helpers :refer [display-type]]))

(defn ->connect-success
  [api-key-dt]
  {:response_type (display-type false)
   :text "Your API key is now saved - all the commands are unlocked."})

(defn ->connect-failure
  [api-key-dt]
  {:response_type (display-type false)
   :text "Failed to save your API key. Contact info: /veye info"})
