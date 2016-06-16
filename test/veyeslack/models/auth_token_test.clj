(ns veyeslack.models.auth-token-test
  (:require [clojure.test :refer :all]
            [veyeslack.server :as server]
            [veyeslack.fixtures :as fx]
            [veyeslack.models.auth-token :as tkn-mdl]))

(def the-db (-> (server/get-system-configs)
                (server/create-system)
                :postgres
                .start))

(use-fixtures :each (fx/make-table-truncate-fixture (:spec the-db) ["auth_tokens"]))

(def auth-response-dt
  {:ok true
   :access_token "oauth-test-case-123"
   :scope "read,command"
   :team_name "ClojureTest"
   :team_id "id-123"
   :user_id "user-123"
   :incoming_webhook {:url "https://slack.com/id/code"
                      :channel_id "ch-112"
                      :channel "#spooky"
                      :configuration_url "https://x.y"}
   :bot {:bot_user_id "bot-user-123"
         :bot_access_token "bot-secret-123"}})

(def updated-response-dt
  (assoc auth-response-dt
         :scope "command"
         :incoming_webhook {:url "https://1.com"
                            :channel_id "ch-112"
                            :channel "#updated2"
                            :configuration_url "https://z.w"}))

(deftest auth-response->model-test
  (testing "raises validation error when token data is empty"
    (is (thrown? RuntimeException (tkn-mdl/auth-response->model {}))))
  
  (testing "it returns flatten and processed doc if input was correct"
    (let [res (tkn-mdl/auth-response->model auth-response-dt)]
      (is (= (:access_token auth-response-dt) (:access_token res)))
      (is (= (:scope auth-response-dt) (:scope res)))
      (is (= (get-in auth-response-dt [:incoming_webhook :url])
             (:url res)))
      (is (= (get-in auth-response-dt [:incoming_webhook :channel])
             (:channel res)))

      (is (= (get-in auth-response-dt [:bot :bot_user_id])
             (:bot_user_id res)))

      (is (= (get-in auth-response-dt [:bot :bot_access_token])
             (:bot_access_token res))))))


(deftest add!-test
  (testing "it raises exception if db client has no spec field"
    (is (thrown? Exception (tkn-mdl/add! {} auth-response-dt))))
  
  (testing "it adds a new auth-token"
    (let [res (tkn-mdl/add! the-db (tkn-mdl/auth-response->model auth-response-dt))]
      (is (false? (nil? res)))
      (is (= 1 (:id res)))
      (is (= (:access_token auth-response-dt) (:access_token res)))
      (is (= (get-in auth-response-dt [:incoming_webhook :channel])
             (:channel res)))
      (is (= (get-in auth-response-dt [:bot :bot_access_token])
             (:bot_access_token res))))))

(deftest get-one-by-team-id-test
  (testing "it returns nil? if no auth_token for the team"
    (is (nil? (tkn-mdl/get-one-by-team-id the-db (:team_id auth-response-dt)))))
  
  (testing "it returns a correct auth_token"
    (let [save-res (tkn-mdl/add! the-db (tkn-mdl/auth-response->model auth-response-dt))
          get-res (tkn-mdl/get-one-by-team-id the-db (:team_id auth-response-dt))]
      
      (is (false? (nil? save-res)))
      (is (false? (nil? get-res)))
      (is (= (:access_token save-res) (:access_token get-res)))
      (is (= (:team_id save-res) (:team_id get-res)))
      (is (= (:channel save-res) (:channel get-res)))
      (is (= (:bot_access_token save-res) (:bot_access_token get-res))))))

(deftest update!-test
  (testing "it raises exception if not valid db-client"
    (is (thrown? Exception (tkn-mdl/update! {} auth-response-dt))))
  
  (testing "it updates existing token"
    (let [old-dt (tkn-mdl/add! the-db (tkn-mdl/auth-response->model auth-response-dt))
          res (tkn-mdl/update! the-db
                               (:id old-dt)
                               (tkn-mdl/auth-response->model updated-response-dt))
          updated-dt (tkn-mdl/get-one the-db (:id old-dt))]
      (is (false? (nil? old-dt)))
      (is (= 1 res))
      (is (= (:access_token auth-response-dt) (:access_token updated-dt)))
      (is (= (:scope updated-response-dt) (:scope updated-dt)))
      (is (= (get-in updated-response-dt [:incoming_webhook :url])
             (:url updated-dt)))
      (is (= (get-in updated-response-dt [:incoming_webhook :channel_id])
             (:channel_id updated-dt))))))

(deftest upsert!-test
  (testing "it adds a new auth-token when there's no auth-token for the team"
    (is (nil? (tkn-mdl/get-one-by-team-id the-db (:team_id auth-response-dt))))
    
    (let [res (tkn-mdl/upsert! the-db (tkn-mdl/auth-response->model auth-response-dt))]
      (is (false? (empty? (tkn-mdl/get-one-by-team-id the-db (:team_id auth-response-dt)))))
      (is (false? (nil? res)))
      (is (= (:access_token auth-response-dt) (:access_token res)))
      (is (= (:team_id auth-response-dt) (:team_id res)))))
  
  (testing "it updates a existing auth-token of the team"
    (let [old-dt (tkn-mdl/add! the-db (tkn-mdl/auth-response->model auth-response-dt))
          res-dt (tkn-mdl/upsert! the-db
                                  (tkn-mdl/auth-response->model updated-response-dt))]
      (is (not (empty? old-dt)))
      (is (not (empty? res-dt)))

      (is (= (:access_token old-dt) (:access_token res-dt)))
      (is (= (:team_id old-dt) (:team_id res-dt)))
      (is (= (:scope updated-response-dt) (:scope res-dt)))
      (is (= (get-in updated-response-dt [:incoming_webhook :url])
             (:url res-dt)))
      (is (= (get-in updated-response-dt [:incoming_webhook :channel_id])
             (:channel_id res-dt)))))
  )

