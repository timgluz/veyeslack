(ns veyeslack.services.versioneye
  (:require [clj-http.client :as http]
            [clojurewerkz.spyglass.client :as spyglass]))

(def api-url "https://www.versioneye.com/api")

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


;;-- project releated endpoints
(defn project-list
  [api-key {:keys [org team]}]
  (let [qparams {:api_key api-key}]
    (http/get (to-api-uri "/v2/projects")
              {:accept :json
               :as :json
               :query-params (merge qparams
                                    (when org {:orga_name org})
                                    (when team {:team_name org}))})))

(defn project-details
  [api-key project-id]
  (http/get (to-api-uri "/v2/projects/" project-id)
            {:accept :json
             :as :json
             :query-params {:api_key api-key}}))

;;user related endpoints
(defn user-notifications
  [api-key]
  (http/get (to-api-uri "/v2/me/notifications")
            {:accept :json
             :as :json
             :query-params {:api_key api-key}}))
