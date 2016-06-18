(ns veyeslack.handlers.pages
  (:require [catacumba.http :refer [ok not-found]]
            [selmer.parser :refer [render-file]]
            [veyeslack.commands.help :as help-cmd]))

(defn index-handler
  [context]
  (ok
    (render-file "templates/index.html"
                 {:links [["Rationale" "#rationale"]
                          ["Install" "#install-guide"]]})
    {:content-type "text/html"}))

(defn command-handler
  [context]
  (ok
    (render-file "templates/commands.html"
                 {:hide_hero_image true
                  :commands (help-cmd/get-details nil)})
    {:content-type "text/html"}))
