(ns veyeslack.handlers.oauth
  (:require [catacumba.http :refer [ok bad-request not-found internal-server-error]]
            [catacumba.serializers :as sz]
            [clj-http.client :as http]
            [environ.core :refer [env]]
            [veyeslack.models.auth-token :as tkn-mdl]))

(defn fetch-authorization-token
  [secret-code]
  (http/get "https://slack.com/api/oauth.access"
            {:as :json
             :accept :json
             :query-params {:code secret-code
                            :client_id (env :slack-client-id)
                            :client_secret (env :slack-client-secret)
                            :redirect_url (env :slack-redirect-url)}
             :throw-exceptions false}))

(defn request-handler
  [context]
  (if-let [req-dt (:data context)]
    (let [auth-rsp (fetch-authorization-token (:code req-dt))]
      (println "#-- auth-resp: " auth-rsp)

      (if (= 200 (:status auth-rsp))
        ;save user authorization token
        (let [save-res (-> auth-rsp :body tkn-mdl/auth-response->model tkn-mdl/add!)]
          (println "#-- model save result" save-res)
          (if (false? (empty? save-res))
            (ok
              (sz/encode {:success true} :json)
              {:content-type "application/json"})
            (internal-server-error
              (sz/encode {:reason "Failed to save Slack secret."} :json)
              {:content-type "application/json"})))
        
        (internal-server-error
          (sz/encode {:reason "Failed to change secrets with Slack"} :json)
          {:content-type "application/json"})
        
        )

      )
    ;;complain about missing data
    (bad-request
      (sz/encode {:reason "misses request arguments"} :json)
      {:content-type "application/json"})
    ))
