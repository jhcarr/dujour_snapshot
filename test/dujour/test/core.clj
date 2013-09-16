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
  (let [fail-req {:params {"product" "SKYNET" "version" "6.6.6" "foo" "bar"}
                  :headers {"x-real-ip" "0.0.0.0"}
                  :fmt "json"}
        success-req {:params {"product" "puppetdb" "version" "1.0.1"}
                     :headers {"x-real-ip" "255.255.255.255"}
                     :fmt "json"}]

    (testing "Tests make-webapp with an invalid request."
      ((make-webapp *db*) (header (request :get "/" (:params fail-req)) "x-real-ip" "0.0.0.0"))
      (is (empty? (jdbc/query *db* (sql/select * :checkins)))))

    (testing "Tests make-webapp with a valid request."
      ((make-webapp *db*) success-req)
      (let [req (keywordize-keys success-req)
            params (dissoc (:params req) :foo)]
        (is (= (assoc params :ip (:x-real-ip (:headers req)))
               (dissoc (first (jdbc/query *db* (sql/select * :checkins))) :timestamp :checkin_id)))))))
