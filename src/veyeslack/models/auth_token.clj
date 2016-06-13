(ns veyeslack.models.auth-token
  (:require [clojure.java.jdbc :as jdbc]
            [schema.core :as s]))

(s/defschema NewAuthToken
  {:access_token s/Str
   :scope s/Str
   :team_name s/Str
   :team_id s/Str
   :url (s/maybe s/Str)
   :channel (s/maybe s/Str)
   :configuration_url (s/maybe s/Str)
   :bot_user_id (s/maybe s/Str)
   :bot_access_token (s/maybe s/Str)})


(defn auth-response->model
  "transform Slack auth response into AuthToken "
  [auth-rsp]
  (-> auth-rsp
      (merge (get auth-rsp :incoming_webhook {})
             (get auth-rsp :bot {}))
      (assoc :incoming (contains? auth-rsp :incoming_webhook))
      (dissoc :bot :incoming_webhook)))

(s/defn add! [db :- s/Any
              token-dt :- NewAuthToken]
  (if (:spec db)
    (jdbc/insert! (:spec db) :auth_tokens token-dt)
    (throw (ex-info "DBClient has no :spec value"
                    {:data db}))))
