(ns veyeslack.models.notification-test
  (:require [clojure.test :refer :all]
            [clj-time.core :as dt]
            [clj-time.format :as df]
            [veyeslack.server :as server]
            [veyeslack.fixtures :as fx]
            [veyeslack.models.notification :as notif-mdl]))

(def the-db (-> (server/get-system-configs)
                (server/create-system)
                :postgres
                .start))

(use-fixtures :each (fx/make-table-truncate-fixture (:spec the-db) ["notifications"]))


(def the-raw-notif1
  {:created_at (df/unparse (df/formatters :date-time) (dt/now))
   :version "2.41.0"
   :sent_email true
   :read false
   :product {:name "boto"
             :language "python"
             :prod_key "boto"
             :version "2.41.0"
             :prod_type "PIP"}})

(def the-raw-notif2
  {:created_at (df/unparse (df/formatters :date-time) (dt/now))
   :version "5.6.3"
   :sent_email true
   :read false
   :product {:name "cheshire"
             :language "clojure"
             :prod_key "cheshire/cheshire"
             :version "5.6.3"
             :prod_type "Maven2"}})

(def the-veye-notification
  {:user {:fullname "Nipi Tri"
          :username "nipitri"}
   :unread 2850
   :notifications [the-raw-notif1 the-raw-notif2]})

(deftest process-api-result
  (testing "it processes items correctly"
    (let [res (notif-mdl/process-api-result "team-1" the-veye-notification)]
      (is (false? (empty? res)))
      (is (= 2 (count (get-in res [:items :pkgs])))))))

(deftest add!-test
  (testing "it saves correctly preprocessed notification"
    (let [the-new-notif (notif-mdl/process-api-result "team-2" the-veye-notification)
          res (notif-mdl/add! the-db the-new-notif)]
      (is (false? (empty? res)))
      (is (pos? (:id res)))
      (is (= 2 (count (get-in res [:items :pkgs])))))))

(deftest mark-sent!-test
  (testing "it returns false if no such notification were found"
    (is (false? (notif-mdl/mark-sent! the-db 102))))

  (testing "it marks notification as sent"
    (let [the-notif (notif-mdl/add!
                      the-db
                      (notif-mdl/process-api-result "team-3" the-veye-notification))]
      (is (true? (notif-mdl/mark-sent! the-db (:id the-notif))))
      (let [res (notif-mdl/get-one the-db (:id the-notif))]
        (is (true? (:success res)))
        (is (false? (nil? (:sent_at res))))))))

(deftest mark-failed!-test
  (testing "it returns false if no such notification were found"
    (is (false? (notif-mdl/mark-failed! the-db 102))))

 (testing "it marks notification as sent"
    (let [the-notif (notif-mdl/add!
                      the-db
                      (notif-mdl/process-api-result "team-3" the-veye-notification))]
      (is (true? (notif-mdl/mark-failed! the-db (:id the-notif))))
      (let [res (notif-mdl/get-one the-db (:id the-notif))]
        (is (false? (:success res)))
        (is (pos? (:n_tries res)))
        (is (nil? (:sent_at res)))))))

(deftest get-many-resendable-test
  (testing "it returns empty list if there no failed notifications"
    (is (empty? (notif-mdl/get-many-resendable the-db))))

  (testing "it returns resendable notification"
    (let [the-notif (notif-mdl/add!
                      the-db
                      (notif-mdl/process-api-result "team-4" the-veye-notification))
          _ (notif-mdl/mark-failed! the-db (:id the-notif))
          res (notif-mdl/get-many-resendable the-db)]
      
      (is (= 1 (count res)))
      (is (= "team-4" (-> res first :team_id))))))

