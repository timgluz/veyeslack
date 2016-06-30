(ns veyeslack.models.auth-token
  (:require [clojure.java.jdbc :as jdbc]
            [schema.core :as s]
            [veyeslack.models.utils :refer [normalize-str]]))

(s/defschema NewAuthToken
  {:access_token s/Str
   :scope s/Str
   :team_name s/Str
   :team_id s/Str
   :user_id s/Str
   :url (s/maybe s/Str)
   :channel (s/maybe s/Str)
   :channel_id (s/maybe s/Str)
   :configuration_url (s/maybe s/Str)
   :bot_user_id (s/maybe s/Str)
   :bot_access_token (s/maybe s/Str)})

(s/defschema AuthToken
  (merge NewAuthToken
         {(s/optional-key :id) s/Num}))

(defn validate [dt the-schema]
  (s/validate the-schema dt))

(defn normalize-ids
  "make sure that FK fields are normalized"
  [auth-dt]
  (assoc auth-dt
         :team_id (normalize-str (:team_id auth-dt))
         :user_id (normalize-str (:user_id auth-dt))
         :channel_id (normalize-str (:channel_id auth-dt))))

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
        (normalize-ids)
        ;;remove flattened keys of subdoc
        (select-keys (keys NewAuthToken))
        (validate AuthToken))))

(s/defn get-one
  [db-client :- s/Any
   token-id :- s/Num]
  (-> (:spec db-client)
      (jdbc/query ["SELECT * FROM auth_tokens WHERE id = ?" token-id])
      first))

(s/defn get-one-by-team-id
  [db-client :- s/Any
   team-id :- s/Str]
  (first
    (jdbc/query 
      (:spec db-client)
      ["SELECT * FROM auth_tokens WHERE team_id = ?"
       (normalize-str team-id)])))

(s/defn add!
  [db-client :- s/Any
   token-dt :- NewAuthToken]
  (if (:spec db-client)
    (first
      (jdbc/insert! (:spec db-client) :auth_tokens token-dt))
    (throw (ex-info "DBClient has no :spec field" {:data db-client}))))

(s/defn update!
  [db-client :- s/Any
   token-id :- s/Num
   token-dt :- AuthToken]
  (if (:spec db-client)
    (-> (:spec db-client)
      (jdbc/update! "auth_tokens"
                    (normalize-ids token-dt)
                    ["id = ?" token-id])
      (first))
    (throw (ex-info "DBClient has no :spec field" {:data db-client}))))

(s/defn upsert!
  "adds or updates existing auth_token; and returns changed model"
  [db-client :- s/Any
   token-dt :- AuthToken]
  (if-let [team-tkn (get-one-by-team-id db-client (:team_id token-dt))]
    (update! db-client (:id team-tkn) (merge team-tkn token-dt))
    (add! db-client token-dt))
  (get-one-by-team-id db-client (:team_id token-dt)))

