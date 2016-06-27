(ns veyeslack.models.api-key
  (:require [clojure.java.jdbc :as jdbc]
            [schema.core :as s]
            [clj-time.core :as dt]
            [clj-time.jdbc]
            [veyeslack.models.utils :refer [normalize-str]]))

(s/defschema with-timestamps
  {:created_at org.joda.time.DateTime
   :updated_at org.joda.time.DateTime})

(s/defschema NewApiKey
  {:team_id s/Str
   :user_id s/Str
   :api_key s/Str})

(s/defschema ApiKey
  (merge {:id s/Num}
         NewApiKey
         with-timestamps))

(defn add!
  [db-client {:keys [team_id user_id] :as msg} the-api-key]
  (if (and team_id user_id)
    (first
      (jdbc/insert! (:spec db-client)
                    :api_keys
                    {:team_id (normalize-str team_id)
                     :user_id (normalize-str user_id)
                     :api_key (normalize-str the-api-key)
                     :created_at (dt/now)
                     :updated_at (dt/now)}))
    (throw (ex-info "It misses team_id or user_id."
                    {:context msg}))))

(defn get-one-by-user-team-id
  [db-client user-id team-id]
  (first
    (jdbc/query
      (:spec db-client)
      ["SELECT * FROM api_keys WHERE user_id = ? AND team_id = ? LIMIT 1"
       (normalize-str user-id)
       (normalize-str team-id)])))

(defn get-many-by-team-id
  "returns a list of api tokens saved for team"
  [db-client team-id]
  (jdbc/query
    (:spec db-client)
    ["SELECT * FROM api_keys WHERE team_id = ?" (normalize-str team-id)]))

(defn update!
  "updates api_key for an user in a team;"
  [db-client {:keys [team_id user_id] :as msg} the-api-token]
  (if (and team_id user_id)
    (-> db-client
        :spec
        (jdbc/update! "api_keys"
                      {:api_key (normalize-str the-api-token)
                       :updated_at (dt/now)}
                      ["user_id = ? and team_id = ?"
                       (normalize-str user_id)
                       (normalize-str team_id)])
        first)
    (throw (ex-info "Missing team_id or user_id"
                    {:context msg}))))

(defn upsert!
  "adds a new apikey or updates existing key"
  [db-client the-slack-message the-api-token]
  (let [get-key (fn [db-client]
                  (get-one-by-user-team-id
                    db-client
                    (:user_id the-slack-message)
                    (:team_id the-slack-message)))]
    (jdbc/with-db-transaction [tx (:spec db-client)]
      (if (get-key {:spec tx})
        (update! {:spec tx} the-slack-message the-api-token)
        (add! {:spec tx} the-slack-message the-api-token))
      (get-key {:spec tx}))))

(defn delete-by-team-id!
  "deletes all the api-keys belonging to the team"
  [db-client team-id]
  (jdbc/delete! (:spec db-client)
                "api_keys"
                ["team_id = ?" (normalize-str team-id)]))

