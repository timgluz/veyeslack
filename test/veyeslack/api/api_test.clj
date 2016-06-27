(ns veyeslack.api.api-test
  (:require [clojure.test :refer :all]
            [clj-http.client :as http]
            [clj-http.fake :refer [with-global-fake-routes]]
            [cheshire.core :as json]
            [environ.core :refer [env]]
            [veyeslack.api :as api]))

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
