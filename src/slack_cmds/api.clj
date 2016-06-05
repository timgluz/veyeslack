(ns slack-cmds.api
  (:require [clj-http.client :as http]
            [environ.core :refer [env]]))

(def api-url "https://www.versioneye.com/api")

(defn get-api-key []
  (env :api-key))

(defn to-api-uri
  [& path-items]
  (apply str (cons api-url path-items)))

(defn search
  [term {:keys [n page] :or {n 10 page 1}}]
  (http/get (to-api-uri "/v2/products/search/" term)
            {:accept :json
             :as :json
             :query-params {:api_key (get-api-key)
                            :n n
                            :page page}}))

(comment
  (require '[slack-cmds.api :as api] :reload)
  (api/get-api-key)
  (api/search "rails" {})

  )

