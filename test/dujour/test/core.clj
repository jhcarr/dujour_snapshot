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
      (let [req (-> (request :get "/" {"product" "Metallo"})
                    (header "x-real-ip" "0.0.0.0"))]
        (is (-> ((make-webapp *db*) req)
               (:status)
               (= 400)) "missing version information :: 400")))

    (testing "testing make-webapp with a valid \"/query\" request to make sure make-webapp is correctly routing to query namespace, expecting valid sql output."
      (let [query (sql/select * :checkins)
            req (-> (request :get "/query/product/pe-master")
                    (header "x-real-ip" "1.1.1.1"))]
        (is (= (:body ((make-webapp *db*) req)) "[]")
             "/query/product/pe-master should return empty set." ))))
