(ns veyeslack.main
  (:gen-class))

(defn -main [& _]
  "entrypoint for compiled jar - fires up DB connections and web server"
  (require 'veyeslack.server)
  ((resolve 'veyeslack.server/start!)))
