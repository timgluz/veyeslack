(ns veyeslack.services.slack
  (:require [clj-http.client :as http]
            [cheshire.core :as json]))

(defn deliver-hook
  "sends a message via a Slack hook-url"
  [{:keys [url access_token channel]} message]
  (let [the-message (assoc message
                           :channel (or channel "#general"))]
    (http/post url
               {:headers {"Content-Type" "application/json"}
                :content-type "json"
                :body (json/generate-string the-message)})))
