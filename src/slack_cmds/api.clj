(ns slack-cmds.api
  (:require [clj-http.client :as http]
            [environ.core :refer [env]]))

(def api-url "https://www.versioneye.com/api")

(defn get-user-key
  [team-id user-id]
  ;;TODO: integrate memcache
  (env :api-key))

(defn to-api-uri
  [& path-items]
  (apply str (cons api-url path-items)))

(defn search
  [api-key term {:keys [n page] :or {n 10 page 1}}]
  (http/get (to-api-uri "/v2/products/search/" term)
            {:accept :json
             :as :json
             :query-params {:api_key api-key
                            :n n
                            :page page}}))

(defn project-list
  [api-key {:keys [org team]}]
  (let [qparams {:api_key api-key}]
    (http/get (to-api-uri "/v2/projects")
              {:accept :json
               :as :json
               :query-params (merge qparams
                                    (when org {:orga_name org})
                                    (when team {:team_name org}))})))

(comment
  (require '[slack-cmds.api :as api] :reload)
  (def api-key (api/get-user-key "" ""))
  (api/search api-key "rails" {})
  (api/project-list api-key {})
  )

