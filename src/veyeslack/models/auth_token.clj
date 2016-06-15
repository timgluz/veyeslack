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
   :channel_id (s/maybe s/Str)
   :configuration_url (s/maybe s/Str)
   :bot_user_id (s/maybe s/Str)
   :bot_access_token (s/maybe s/Str)})

(s/defschema AuthToken
  (merge NewAuthToken
         {(s/optional-key :id) s/Num}))

(defn auth-response->model
  "transform Slack auth response into AuthToken "
  [auth-rsp]
  (let [default-hook-dt {:url nil
                         :channel nil
                         :configuration_url nil}
        default-bot-dt {:bot_user_id nil
                        :bot_access_token nil}]
    (-> auth-rsp
        ;;flatten doc
        (merge (get auth-rsp :incoming_webhook default-hook-dt)
               (get auth-rsp :bot default-bot-dt))
        ;;remove flattened keys of subdoc
        (dissoc :bot :incoming_webhook :ok))))

(s/defn get-one-by-team-id
  [db-client :- s/Any
   team-id :- s/Str]
  (println "get-one-by-team-id: using database:" (:spec db-client))

  (first
    (jdbc/query 
      (:spec db-client)
      ["SELECT * FROM auth_tokens WHERE team_id = ?" team-id])))

(s/defn add!
  [db-client :- s/Any
   token-dt :- NewAuthToken]
  (if (:spec db-client)
    (jdbc/insert! (:spec db-client) :auth_tokens token-dt)
    (throw (ex-info "DBClient has no :spec field" {:data db-client}))))

(s/defn update!
  [db-client :- s/Any
   token-id :- s/Num
   token-dt :- AuthToken]
  (if (:spec db-client)
    (jdbc/update! (:spec db-client)
                  "auth_tokens"
                  token-dt
                  ["id = ?" token-id])
    (throw (ex-info "DBClient has no :spec field" {:data db-client}))))

(s/defn upsert!
  [db-client :- s/Any
   token-dt :- AuthToken]
  (if-let [team-tkn (get-one-by-team-id db-client (:team_id token-dt))]
    (update! db-client (:id team-tkn) (merge team-tkn token-dt))
    (add! db-client token-dt)))

