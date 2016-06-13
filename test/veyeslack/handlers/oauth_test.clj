(ns veyeslack.handlers.oauth-test
  (require [clojure.test :refer :all]
           [clj-http.client :as http-client]
           [clj-http.fake :refer [with-fake-routes]]
           [cheshire.core :as json]
           [catacumba.testing :refer [with-server]]
           [veyeslack.test_helpers :refer [with-component-server]]
           [veyeslack.server :as server]
           [veyeslack.handlers.oauth :as oauth]))

(def base-url "http://127.0.0.1:5050")
(def slack-oauth-url "https://slack.com/api/oauth.access")
(def slack-invalid-code-error
  {:ok false
   :error "invalid_code"})

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
          (is (= 400 (:status res)))))))
  
  (testing "oauth returns 503, when it fails to swap code"
    (with-fake-routes
      {#".*" (fn [req]
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
          (is (= 503 (:status res))))))))

;;TODO: add table cleaners
(deftest oauth-happy-flow
  (testing "oauth returns"))
