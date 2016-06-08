(ns veyeslack.server
  (:require [catacumba.core :as ct]
            [catacumba.handlers.parse :as parse]
            [clojure.string :as string]
            [veyeslack.handlers.commands :as commands]
            [veyeslack.handlers.help :as help]
            [veyeslack.handlers.oauth :as oauth])
  (:gen-class))

(def app
  (ct/routes
    [[:prefix "commands"
      [:any (parse/body-params)]
      [:post commands/handler]]
     [:prefix "oauth"
      [:any (parse/body-params)]
      [:get "request" oauth/request-handler]]

     [:all help/handler] ;;TODO refactor it as info-handler + include release dt
     ]))

(defn -main [& args]
  (ct/run-server app {:port 3030
                      :debug true}))
