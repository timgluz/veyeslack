(ns veyeslack.handlers.oauth
  (:require [catacumba.http :refer [ok bad-request not-found internal-server-error moved-permanently]]
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

(defn run-authorization
  [db req-dt]
  (let [auth-rsp (fetch-authorization-token (:code req-dt))]
    (if (= 200 (:status auth-rsp))
      ;save user authorization token
      (let [save-res (-> (:body auth-rsp)
                         tkn-mdl/auth-response->model
                         ((partial tkn-mdl/upsert! db)))]
        (if (empty? save-res)
          (println {:reason "Failed to save a slack secret"
                    :context {:request req-dt
                              :response auth-rsp}})
          save-res))
        ;if failed to exchange secrets
        (println {:reason "Failed to changes secrets with Slack API"
                  :context {:request req-dt
                            :response auth-rsp}}))))

(defn request-handler
  [context]
  (let [req-dt (merge {} (:query-params context)  (:data context))
        the-db (get-in context [:app :db])]
    (if (or (empty? req-dt)
            (contains? req-dt :error))
      ;;complain about missing data
      (moved-permanently "/oauth/failure") 
      ;;process if request includes secret code
      (if-let [user-tkn (run-authorization the-db req-dt)]
        (moved-permanently "/oauth/success")
        (moved-permanently "/oauth/failure")))))

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
