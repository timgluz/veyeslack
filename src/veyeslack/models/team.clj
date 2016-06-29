(ns veyeslack.models.team
  (:require [clojure.java.jdbc :as jdbc]))

(defn get-many-with-key
  "returns list of teams which have VersionEye API-key"
  [db-client]
  (jdbc/query
    (:spec db-client)
    ["SELECT T.team_id, T.access_token, T.url, T.channel, A.api_key 
      FROM auth_tokens AS T, api_keys AS A
      WHERE T.team_id = A.team_id"]))
