(ns dujour.test.core
  (:require [ring.util.response :as rr]
            [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.sql :as sql]
            [cheshire.core :as json]
            [dujour.db :as db])
  (:use [dujour.core])
  (:use [clojure.test]
        [dujour.fixtures]
        [ring.mock.request]
        [clj-semver.core :only (newer?)]
        [clojure.string :only (join)]
        [clj-time.core :only (now)]
        [clj-time.coerce :only (to-timestamp)]
        [clojure.walk :only (keywordize-keys)]))

(use-fixtures :each with-test-database with-test-default-releases)

(deftest test-make-webapp
    (testing "testing make-webapp with an invalid checkin request to make sure that make-webapp is correctly routing to checkins namespace, expecting status 404"
      (let [req (request :get "/" {"product" "Metallo"})]
        (is (-> ((make-webapp *db*) )
               (:status)
               (= 404)) "invalid product information :: 404"))))

    (testing "testing make-webapp with a valid \"/query\" request to make sure make-webapp is correctly routing to query namespace, expecting valid sql output."
      (let [query (sql/select * :checkins)
            req (request :get "/query/product/pe-master")]
        (is (= ((make-webapp *db*) req)
               (jdbc/query query)) "Both queries should return empty sets." )))
