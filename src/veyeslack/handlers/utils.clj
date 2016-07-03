(ns veyeslack.handlers.utils
  (:require [catacumba.core :as ct]
            [catacumba.http :as http]
            [catacumba.serializers :as sz]
            [environ.core :refer [env]]
            [circleci.rollcage.core :as rollcage]))

(defn json-response
  [context]
  (http/response
    (-> context :body (sz/encode :json))
    (or (:status context) 200)
    (merge {:content-type "application/json"}
           (get context :response-headers {}))))

(defn- get-slack-token []
  (env :slack-token))

(defn check-command-origin
  [context]
  (if (= (get-slack-token) (get-in context [:data :token]))
    (ct/delegate context)
    ;short circuit with error message
    (http/ok
      (sz/encode {:response_type "ephemeral"
                  :title "Unknown Origin"
                  :text "Sorry, we were not able to validate the origin of your command."}
                 :json)
      {:content-type "application/json"})))

(defn default-error-handler
  [context error]
  (when-let [yeller (get-in context [:app :yeller])]
    (rollcage/error yeller error))
  (println "#-- Check errors:\n" context)
  (println "#-- reason: " error)
  (http/internal-server-error
    "Bad things happened, sent notification to the team"
    {:content-type "text/html"}))

(defn command-error-handler
  [context error]
  (when-let [yeller (get-in context [:app :yeller])]
    (rollcage/error yeller error))
  (println "#-- Failed to execute command:\n" context)
  (println "#-- reason: " error)
  (http/ok
    (sz/encode {:response_type "ephemeral"
                :title "Sorry!"
                :text "Oops, you found something our developer missed."}
               :json)
    {:content-type "application/json"}))
