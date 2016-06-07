(ns veyeslack.handlers.help
  (:require [catacumba
              [http :as http]
              [serializers :as sz]]
            [clojure.string :as string]))

;;it allows to display help info for subcommands
(defmulti help-dispatcher (fn [cmd-dt] :all))

(defmethod help-dispatcher :all [cmd-dt]
 {:response_type "ephemeral"
   :text "VersionEye commands:"
   :attachments [{:text "/veye clojure - search a package"}
                 {:text "/veye list    - list projects"}
                 {:text "/veye connect - save your api-key"}
                 {:text "/veye help    - show commands"}]})

(defn handler
  [context]
  (http/ok
    (sz/encode (help-dispatcher {:text "help"}) :json)
    {:content-type "application/json"}))
