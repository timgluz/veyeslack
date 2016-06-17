(ns veyeslack.handlers.oauth-test
  (:require [clojure.test :refer :all]
            [clj-http.client :as http-client]
            [clj-http.fake :refer [with-fake-routes with-global-fake-routes]]
            [cheshire.core :as json]
            [catacumba.testing :refer [with-server]]
            [veyeslack.test_helpers :refer [with-component-server]]
            [veyeslack.fixtures :as fx]
            [veyeslack.server :as server]
            [veyeslack.models.auth-token :as tkn-mdl]
            [veyeslack.handlers.oauth :as oauth]))

(def base-url "http://127.0.0.1:5050")
(def slack-oauth-url "https://slack.com/api/oauth.access")
(def slack-invalid-code-error
  {:ok false
   :error "invalid_code"})

(def slack-success-response
  {:access_token "oauth-test-case-123"
   :scope "read,command"
   :team_name "ClojureTest"
   :team_id "id-123"
   :user_id "user-123"
   :channel_id "ch-112"
   :incoming_webhook {:url "https://slack.com/id/code"
                      :channel "#spooky"
                      :configuration_url "https://x.y"}
   :bot {:bot_user_id "bot-user-123"
         :bot_access_token "bot-secret-123"}})

(def the-db (-> (server/get-system-configs)
                (server/create-system)
                :postgres
                .start))

(use-fixtures :each (fx/make-table-truncate-fixture (:spec the-db) ["auth_tokens"]))

(deftest oauth-error-messages
  (testing "oauth returns 503, when it fails to swap code"
    (with-fake-routes
      {slack-oauth-url (fn [req]
                         {:status 503
                          :headers {"Content-Type"  "application/json"}
                          :body (json/generate-string slack-invalid-code-error)})}
      
      (with-server {:handler oauth/request-handler}
        (let [res (http-client/get
                    (str base-url)
                    {:as :json :throw-exceptions? false})]
          (is (false? (nil? res)) "API response cant be nil")
          (is (= 301 (:status res)))))))
  
  (testing "oauth returns 503, when it fails to swap code"
    (with-global-fake-routes
      {#"https://slack.*" (fn [req]
                           {:status 503
                            :headers {"Content-Type" "application/json"}
                            :body (json/generate-string slack-invalid-code-error)})}

      (with-component-server {:system (server/start!)}
        (let [res (http-client/get
                      (str base-url "/oauth/request")
                      {:as :json
                       :query-params {:code "abc-123"}
                       :throw-exceptions? false})]
          (is (false? (nil? res)) "API response cant be nil")
          (is (= 301 (:status res))))))))

(deftest oauth-happy-flow
  (testing "oauth returns proper token-data and saves results"
    (with-global-fake-routes
      {#"https://slack.*" (fn [req]
                           {:status 200
                            :headers {"Content-Type" "application/json"}
                            :body (json/generate-string slack-success-response)})}
      
      (with-component-server {:system (server/start!)}
        (let [res (http-client/get
                    (str base-url "/oauth/request")
                    {:as :json
                     :query-params {:code "abc-123"}
                     :throw-exceptions? false})]
          (is (false? (nil? res)) "API response cant be nil")
          (is (= 301 (:status res)))
          (let [team-tkn (tkn-mdl/get-one-by-team-id
                           the-db
                           (:team_id slack-success-response))]
            (is (false? (empty? team-tkn)) "It didnt save user api token")
            (is (= (:team_id slack-success-response)
                   (:team_id team-tkn)))
            (is (= (:access_token slack-success-response)
                   (:access_token team-tkn)))))))))
