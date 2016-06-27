(ns veyeslack.models.api-key-test
  (:require [clojure.test :refer :all]
            [veyeslack.server :as server]
            [veyeslack.fixtures :as fx]
            [veyeslack.models.api-key :as key-mdl]))

(def the-db (-> (server/get-system-configs)
                (server/create-system)
                :postgres
                .start))

(use-fixtures :each (fx/make-table-truncate-fixture (:spec the-db) ["api_keys"]))

(def the-api-key "bfde26531924d992")
(def the-slack-message
  {:team_id "team-123"
   :team_name "pew-pew"
   :user_id "user-123"
   :channel_id "#boomerang"
   :text "connect veye-token-123"
   :command "/veye"})

(deftest add!-test
  (testing "it raises exception if slack message has no userid or teamid"
    (is (thrown? Exception (key-mdl/add! the-db {} the-api-key))))
  
  (testing "it saves a new api-key"
    (let [res (key-mdl/add! the-db the-slack-message the-api-key)]
      (is (false? (nil? res)))
      (is (= 1 (:id res)))
      (is (= (:team_id the-slack-message) (:team_id res)))
      (is (= (:user_id the-slack-message) (:user_id res)))
      (is (= the-api-key (:api_key res))))))


(deftest get-one-by-user-team-id-test
  (testing "it returns nil when no match"
    (is (nil? (key-mdl/get-one-by-user-team-id
                the-db
                (:user_id the-slack-message)
                (:team_id the-slack-message)))))
  (testing "it returns the correct model"
    (let [save-res (key-mdl/add! the-db the-slack-message the-api-key)
          res (key-mdl/get-one-by-user-team-id
                the-db
                (:user_id the-slack-message)
                (:team_id the-slack-message))]
      
      (is (false? (nil? save-res)))
      (is (false? (nil? res)))
      (is (= (:user_id the-slack-message) (:user_id res)))
      (is (= (:team_id the-slack-message) (:team_id res)))
      (is (= the-api-key (:api_key res))))))

(deftest get-many-by-team-id
  (testing "it returns an empty list"
    (is (empty? (key-mdl/get-many-by-team-id
                  the-db
                  (:team_id the-slack-message)))))
  
  (testing "it returns all team api_tokens"
    (let [save-res (key-mdl/add! the-db the-slack-message the-api-key)
          res (key-mdl/get-many-by-team-id the-db (:team_id the-slack-message))]
      (is (false? (nil? save-res)))
      (is (false? (empty? res)))
      (is (=  1 (count res)))
      (is (= (:team_id the-slack-message)
             (-> res first :team_id)))
      (is (= the-api-key (-> res first :api_key))))))

(deftest update!-test
  (testing "it returns nil if found no token by team and user"
    (is (thrown? Exception (key-mdl/update! the-db {} the-api-key))))
  
  (testing "it updates successfully a existing data"
    (let [save-res (key-mdl/add! the-db the-slack-message the-api-key)
          update-res (key-mdl/update! the-db the-slack-message "abc-123")
          res (key-mdl/get-one-by-user-team-id
                the-db
                (:user_id the-slack-message)
                (:team_id the-slack-message))]
      (is (false? (nil? save-res)))
      (is (= 1 update-res))
      (is (= (:user_id the-slack-message) (:user_id res)))
      (is (= (:team_id the-slack-message) (:team_id res)))
      (is (= "abc-123" (:api_key res))))))

(deftest upsert!-test
  (testing "it raises Exception if a slack message misses userid"
    (is (thrown? Exception (key-mdl/upsert! the-db {} the-api-key))))
  
  (testing "it adds a new api-token for the team"
    (is (empty? (key-mdl/get-many-by-team-id the-db (:team_id the-slack-message))))
    (let [res (key-mdl/upsert! the-db the-slack-message the-api-key)]
      (is (false? (nil? res)))
      (is (= (:team_id the-slack-message) (:team_id res)))
      (is (= (:user_id the-slack-message) (:user_id res)))
      (is (= the-api-key (:api_key res)))
      (is (= 1 (count (key-mdl/get-many-by-team-id the-db (:team_id the-slack-message)))))))

  (testing "it updates an existing api-token"
    (key-mdl/delete-by-team-id! the-db (:team_id the-slack-message))
    (is (empty? (key-mdl/get-many-by-team-id the-db (:team_id the-slack-message))))

    (let [old-dt (key-mdl/add! the-db the-slack-message the-api-key)
          updated-dt (key-mdl/upsert! the-db the-slack-message "toor-123-root")]
      (is (false? (nil? old-dt)))
      (is (= 1 (count (key-mdl/get-many-by-team-id the-db (:team_id the-slack-message)))))
      (is (= (:id old-dt) (:id updated-dt)))
      (is (= (:user_id old-dt) (:user_id updated-dt)))
      (is (= (:team_id old-dt) (:team_id updated-dt)))
      (is (= "toor-123-root" (:api_key updated-dt))))))


