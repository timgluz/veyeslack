(ns veyeslack.handlers.utils
  (:require [catacumba.http :as http]
            [catacumba.serializers :as sz]
            [circleci.rollcage.core :as rollcage]))

(defn json-response
  [context]
  (http/response
    (-> context :body (sz/encode :json))
    (:status context)
    (merge {:content-type "application/json"}
           (:headers context))))

(defn default-error-handler
  [context]
  (when-let [yeller (get-in context [:app :yeller])]
    (rollcage/error yeller context))
  (println "#-- Check errors:\n" context)
  (http/internal-server-error
    "Bad things happened, sent notification to the team"
    {:content-type "text/html"}))

(defn command-error-handler
  [context]
  (when-let [yeller (get-in context [:app :yeller])]
    (rollcage/error yeller context))
  (println "#-- Failed to execute command:\n" context)
  (http/ok
    (sz/encode {:response_type "ephemeral"
                :title "Sorry!"
                :text "Oops, you found something our developer misses."}
               :json)
    {:content-type "application/json"}))
