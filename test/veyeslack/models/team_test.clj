(ns veyeslack.models.team-test
  (:require [clojure.test :refer :all]
            [veyeslack.server :as server]
            [veyeslack.fixtures :as fx]
            [veyeslack.models.auth-token :as tkn-mdl]
            [veyeslack.models.api-key :as key-mdl]
            [veyeslack.models.team :as team-mdl]))

(def the-db (-> (server/get-system-configs)
                (server/create-system)
                :postgres
                .start))

(use-fixtures :each (fx/make-table-truncate-fixture
                      (:spec the-db)
                      ["auth_tokens" "api_keys"]))

(def the-api-key "bfde26531924d992")
(def the-slack-message
  {:team_id "team-123"
   :team_name "pew-pew"
   :user_id "user-123"
   :channel_id "#boomerang"
   :text "connect veye-token-123"
   :command "/veye"})

(def auth-response-dt
  {:ok true
   :access_token "oauth-test-case-123"
   :scope "read,command"
   :team_name "ClojureTest"
   :team_id "team-123"
   :user_id "user-123"
   :incoming_webhook {:url "https://slack.com/id/code"
                      :channel_id "ch-112"
                      :channel "#spooky"
                      :configuration_url "https://x.y"}
   :bot {:bot_user_id "bot-user-123"
         :bot_access_token "bot-secret-123"}})

(deftest get-many-with-key-test
  (testing "returns empty list if no rows"
    (is (empty? (team-mdl/get-many-with-key the-db))))
  
  (testing "returns the-team 123 with correct api-key"
    (let [the-tkn (tkn-mdl/add! the-db
                                (tkn-mdl/auth-response->model auth-response-dt))
          the-key (key-mdl/add! the-db the-slack-message the-api-key)
          res (team-mdl/get-many-with-key the-db)]

      (is (false? (empty? res)))
      (is (= the-api-key (-> res first :api_key)))
      (is (= (:team_id auth-response-dt) (-> res first :team_id)))
      (is (= (:access_token auth-response-dt) (-> res first :access_token)))
      (is (= (get-in auth-response-dt [:incoming_webhook :url])
             (-> res first :url))))))
