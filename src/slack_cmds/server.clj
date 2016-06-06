(ns slack-cmds.server
  (:require [catacumba.core :as ct]
            [catacumba.handlers.parse :as parse]
            [clojure.string :as string]
            [slack-cmds.handlers.commands :as commands]
            [slack-cmds.handlers.help :as help])
  (:gen-class))

(def app
  (ct/routes
    [[:prefix "commands"
      [:any (parse/body-params)]
      [:post commands/handler]]
     [:all help/handler] ;;TODO refactor it as info-handler + include release dt
     ]))

(defn -main [& args]
  (ct/run-server app {:port 3030
                      :debug true}))
