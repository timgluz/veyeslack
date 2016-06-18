(ns veyeslack.formatters.help
  (:require [veyeslack.formatters.helpers :refer [display-type]]))

(defn ->full-help
  [commands]
  {:response_type (display-type false)
   :text "All supported commands"
   :attachments (doall
                  (for [[k v] commands]
                    {:text (str (-> v :examples first) " - " (:text v))}))})

(defn ->command-help
  [commands the-command]
  (if-let [the-help (get commands (keyword the-command))]
    {:response_type (display-type false)
     :text (str the-command " - " (:text the-help))
     :attachments (doall
                    (for [the-ex (:examples the-help)]
                      {:text (str the-ex)}))}
    {:response_type (display-type false)
     :text (str "The command `" the-command "` is not supported")}))


