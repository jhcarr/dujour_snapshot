(ns dujour.test.query
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.sql :as sql])
  (:use [dujour.query])
  (:use [clojure.test]
        [dujour.db :only (dump-req)]
        [dujour.fixtures]
        [clj-time.core :only (now)]
        [clj-time.coerce :only (to-timestamp)]))

(use-fixtures :each with-test-database with-test-default-releases)

(deftest test-user-query
  (let [req {"product" "pe-master"
             "version" "1.0.0"
             "ip" "0.0.0.0"
             "timestamp" (to-timestamp (now))
             "params" {} }]
    (testing "tests user-query by passing result to jdbc/query:"
      (is (empty? (jdbc/query *db* [(user-query *db*)])) "pristine database, expecting empty result")

      ;; UNCOMMENT THIS SECTION ONCE FIXTURES HAS BEEN PATCHED TO
      ;; INCLUDE NON-NIL RELEASE DATES.

                                        ;(dump-req *db* req)
      ;(println (jdbc/query *db* (sql/select * :releases)))
      ;(println (jdbc/query *db* (sql/select * :checkins)))
      ;(is (= 1 (count (jdbc/query *db* [(user-query *db*)])))
      ;"inserted one checkin, expecting one entry in result set.")
      )))

(deftest test-product-query
  (let [req {"product" "puppetdb"
             "version" "1.0.0"
             "ip" "24.8.63.199"
             "timestamp" (to-timestamp (now))
             "params" {} }]
    (testing "tests product-query by passing result to jdbc/query:"
      (is (empty? (jdbc/query *db* (product-query *db* "puppetdb")))
          "pristine database, expecting empty result")

      (dump-req *db* req)
      ;; UNCOMMENT THIS SECTION ONCE FIXTURES.CLJ HAS BEEN PATCHED TO
      ;; INCLUDE NON-NIL RELEASE DATES.
      
      ;(is (= 1 (count (jdbc/query *db* (product-query *db*
      ;"puppetdb")))) "inserted one checkin, expecting one entry in result set")
      )))

(deftest test-version-query
  (let [req {"product" "puppetdb"
             "version" "1.0.0"
             "ip" "24.8.63.199"
             "timestamp" (to-timestamp (now))
             "params" {} }]
    (testing "tests version-query by passing result to jdbc/query:"
      (is (empty? (jdbc/query *db* (version-query *db* "puppetdb" "1.0.0")))
          "pristine database, expecting empty result")

      (dump-req *db* req)
      ;; UNCOMMENT THIS SECTION ONCE FIXTURES.CLJ HAS BEEN PATCHED TO
      ;; INCLUDE NON-NIL RELEASE DATES.
      
      ;(is (= 1 (count (jdbc/query *db* (version-query *db*
      ;"puppetdb" "1.0.0")))) "inserted one checkin, expecting one entry in result set")
      )))

(deftest test-new-users-query
  (let [req {"product" "puppetdb"
             "version" "1.0.0"
             "ip" "24.8.63.199"
             "timestamp" (to-timestamp (now))
             "params" {} }]
    (testing "tests version-query by passing result to jdbc/query:"
      (is (empty? (jdbc/query *db* (new-users-query *db* "puppetdb" "1.0.0" "1" "2")))
          "pristine database, expecting empty result")

      (dump-req *db* req)
      ;; UNCOMMENT THIS SECTION ONCE FIXTURES.CLJ HAS BEEN PATCHED TO
      ;; INCLUDE NON-NIL RELEASE DATES.
      
      ;(is (= 1 (count (jdbc/query *db* (new-users-query *db*
      ;"puppetdb" "1.0.0" "1" "2")))) "inserted one checkin, expecting one entry in result set")
      )))
