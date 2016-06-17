(ns veyeslack.handlers.oauth
  (:require [catacumba.http :refer [ok bad-request not-found internal-server-error]]
            [catacumba.serializers :as sz]
            [clj-http.client :as http]
            [environ.core :refer [env]]
            [selmer.parser :refer [render-file]]
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

;;TODO: separate logic from rendering
(defn request-handler
  [context]
  (let [req-dt (merge {} (:query-params context)  (:data context))]
    (if (or (empty? req-dt)
            (contains? req-dt :error))
      ;;complain about missing data
      (bad-request
        (sz/encode {:reason "Not valid query arguments - missing or cancelled request"
                    :data {:error (str (:error req-dt))}}
                   :json)
        {:content-type "application/json"})
      ;;process if request includes secret code
      (let [auth-rsp (fetch-authorization-token (:code req-dt))
            db (get-in context [:app :db])]

        (if (= 200 (:status auth-rsp))
          ;save user authorization token
          (let [save-res (-> (:body auth-rsp)
                             tkn-mdl/auth-response->model
                             ((partial tkn-mdl/upsert! db)))]
            (if (false? (empty? save-res))
              (ok
                (sz/encode {:success true} :json)
                {:content-type "application/json"})
              (internal-server-error
                (sz/encode {:reason "Failed to save Slack secret."} :json)
                {:content-type "application/json"})))
          
          (internal-server-error
            (sz/encode {:reason "Failed to change secrets with Slack"
                        :data (:body auth-rsp)}
                       :json)
            {:content-type "application/json"}))))))

(defn success-handler
  [context]
  (ok
    (render-file "templates/success.html"
                 {:links [["Rationale" "#rationale"]
                          ["What next?" "#what-next"]
                          ["Quick Start" "#quick-start"]]})
    {:content-type "text/html"}))

(defn failure-handler
  [context]
  (ok
    (render-file "templates/failure.html"
                 {:links [["Message" "#message"]]})
    {:content-type "text/html"}))
