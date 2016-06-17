(ns veyeslack.handlers.pages
  (:require [catacumba.http :refer [ok not-found]]
            [selmer.parser :refer [render-file]]))

(defn index-handler
  [context]
  (ok
    (render-file "templates/index.html"
                 {:links [["Rationale" "#rationale"]
                          ["Install" "#install-guide"]]})
    {:content-type "text/html"}))
