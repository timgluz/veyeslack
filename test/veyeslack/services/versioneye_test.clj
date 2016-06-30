(ns veyeslack.services.versioneye-test
  (:require [clojure.test :refer :all]
            [clj-http.client :as http]
            [clj-http.fake :refer [with-global-fake-routes]]
            [cheshire.core :as json]
            [environ.core :refer [env]]
            [veyeslack.services.versioneye :as api]))

(def the-token "abc-123")

(deftest search-test
  (testing "throws exception when request failed"
    (with-global-fake-routes
      {#"https://www.versioneye.com/.*" (fn [res]
                                          {:status 404
                                           :headers {"Content-Type" "application/json"}
                                           :body "Found no page"})}
      (is (thrown? Exception (api/search the-token "clo" {})))))

  (testing "returns search result with default settings"
    (with-global-fake-routes
      {#"https://www.versioneye.com/api/.*" (fn [req]
                                              {:status 200
                                               :headers {"Content-Type" "application/json"}
                                               :body (json/generate-string
                                                       {"query" {
                                                          "q" "clojure"
                                                          "lang" []
                                                          "g" nil
                                                        }
                                                        "results" [
                                                          {"name" "clojure"
                                                           "language" "java"
                                                           "prod_key" "org.clojure/clojure"
                                                           "version" "1.8.0"
                                                           "prod_type" "Maven2"}]})})}
      (let [res (api/search the-token "clo" {})]
        (is (= 200 (:status res)))
        (is (= "clojure" (get-in res [:body :query :q])))
        (is (= 1 (count (get-in res [:body :results]))))))))


(def project-dt
  {"id" "52126476632bac1ae3007c88",
   "name" "bugtraqer",
   "project_type" "Lein",
   "organisation" "",
   "team" nil,
   "public" false,
   "period" "weekly",
   "source" "upload",
   "dep_number" 9,
   "out_number" 9,
   "licenses_red" 0,
   "licenses_unknown" 3,
   "dep_number_sum" 9,
   "out_number_sum" 9,
   "licenses_red_sum" 0,
   "licenses_unknown_sum" 3,
   "license_whitelist_name" nil,
   "created_at" "19.08.2013-18:31",
   "updated_at" "19.08.2013-18:31"})

(deftest project-list-test
  (testing "throws exception when request failed"
    (with-global-fake-routes
      {#"https://www.versioneye.com/.*" (fn [res]
                                          {:status 400
                                           :headers {"Content-Type" "application/json"}
                                           :body "Kaputt"})}
      (is (thrown? Exception (api/project-list the-token {})))))

  (testing "returns list of projects with default settings"
    (with-global-fake-routes
      {#"https://www.versioneye.com/.*" (fn [res]
                                          {:status 200
                                           :headers {"Content-Type" "application/json"}
                                           :body (json/generate-string [project-dt])})}
      (let [res (api/project-list the-token {})]
        (is (= 200 (:status res)))
        (is (= "bugtraqer" (-> res :body first :name)))
        (is (= "Lein" (-> res :body first :project_type)))
        (is (= 9 (-> res :body first :dep_number)))))))

(deftest project-details-test
  (testing "raises exception when request failed"
    (with-global-fake-routes
      {#"https://www.versioneye.com/.*" (fn [res]
                                          {:status 400
                                           :headers {"Content-Type" "application/json"}
                                           :body "[]"})}
      (is (thrown? Exception (api/project-details the-token (get project-dt "id"))))))

  (testing "returns project details with default params"
    (with-global-fake-routes
      {#"https://www.versioneye.com/.*" (fn [res]
                                          {:status 200
                                           :headers {"Content-Type" "application/json"}
                                           :body (json/generate-string project-dt)})}
      (let [res (api/project-details the-token (get project-dt "id"))]
        (is (= 200 (:status res)))
        (is (= "bugtraqer" (-> res :body :name)))
        (is (= "Lein" (-> res :body :project_type)))
        (is (= 9 (-> res :body :dep_number)))))))

(def notification-dt
  {:user {:fullname "Nipi Tri"
          :username "nipitri"}
   :unread 2850
   :notifications [{:created_at "2016-06-28T01:23:12.334Z"
                    :version "2.41.0"
                    :sent_email true
                    :read false
                    :product {:name "boto"
                              :language "python"
                              :prod_key "boto"
                              :version "2.41.0"
                              :prod_type "PIP"}}]})

(deftest user-notifications-test
  (testing "raises exception when request failed as wrong api-key was used"
    (with-global-fake-routes
      {#"https://www.versioneye.com/.*" (fn [res]
                                     {:status 401
                                      :headers {"Content-Type" "application/json"}
                                      :body "[]"})}
      (is (thrown? Exception (api/user-notifications the-token)))))

  (testing "returns user notifications"
    (with-global-fake-routes
      {#".*"
        (fn [res]
          {:status 200
           :headers {"Content-Type" "application/json"}
           :body (json/generate-string notification-dt)})}
      (let [res (api/user-notifications the-token)]
        (is (= 200 (:status res)))
        (is (= (:unread notification-dt) (-> res :body :unread)))
        (is (= (-> notification-dt :notifications first :version)
               (-> res :body :notifications first :version)))))))

(def cve-dt
  {:paging {:current_page 1
            :per_page 30
            :total_pages 1
            :total_entries 0}
   :results [{:language "Java"
              :prod_key "org.jruby/jruby"
              :name_id "2013-0269"
              :summary "Nana"}]})

(deftest security-test
  (testing "raises exception when request failed"
    (with-global-fake-routes
      {#"https://www.versioneye.com/.*" (fn [res]
                                          {:status 401
                                           :headers {"Content-Type" "application/json"}
                                           :body "[]"})}
      (is (thrown? Exception (api/security the-token "java")))))
  
  (testing "returns security information for a language"
    (with-global-fake-routes
      {#"https://www.versioneye.com/.*" (fn [res]
                                          {:status 200
                                           :headers {"Content-Type" "application/json"}
                                           :body (json/generate-string cve-dt)})}
      (let [res (api/security the-token {:lang "java"})]
        (is (= 200 (:status res)))
        (is (= "Java" (-> res :body :results first :language)))))))


