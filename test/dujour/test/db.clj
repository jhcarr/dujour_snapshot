(ns dujour.test.db
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.sql :as sql])
  (:use [dujour.db])
  (:use [clojure.test]
        [dujour.fixtures]
        [clj-time.core :only (now)]
        [clj-time.coerce :only (to-timestamp)]))

(use-fixtures :each with-test-database with-test-default-releases)

(deftest test-sql-database-table-names
  (testing "tests sql-database-table-names, expecting a list of all table names present in database"
      (is (= (frequencies (sql-database-table-names *db*)) (frequencies '("ragtime_migrations" "params" "users" "checkins" "products" "releases")) ))))

(deftest test-is-release?
  (testing "tests is-release? function with empty strings, expecting false"
    (is (not (is-release? *db* "" ""))))
  (testing "tests is-release? function with invalid arguments, expecting false"
    (is (not (is-release? *db* "FOO" "BAR"))))
  (testing "tests is-release? function with valid argument, expecting true"
    (is (is-release? *db* "pe-master" "1.0.0"))))

(deftest test-is-user?
  (testing "tests is-user? function with empty string, expecting false"
    (is (not (is-user? *db* ""))))
  (testing "tests is-user? function with invalid argument, expecting false"
    (is (not (is-user? *db* "This is a test!"))))
  (testing "tests is-user? function with valid argument, expecting true"
    (jdbc/insert! *db* :users {:ip "1.2.3.4"})
    (is (is-user? *db* "1.2.3.4"))
    (jdbc/delete! *db* :users (sql/where {:ip "1.2.3.4"}))))

(deftest test-make-release!
  (testing "tests make-release! with new product and version data, querying releases table should return exactly one row containing this data"
    (make-release! *db* "puppetdb" "4.3.2")
    (let [release-query (sql/select '(:product :version) {:releases :r}
                                    (sql/where {:r.product "puppetdb"
                                                :r.version "4.3.2"}))
          release (jdbc/query *db* release-query)]
      (is (= 1 (count release)))))
  (testing "tests make-release! with old product and version data, querying releases table should return exactly one row containing this data"
    (make-release! *db* "puppetdb" "1.0.0")
    (make-release! *db* "puppetdb" "1.0.0")
    (let [release-query (sql/select '(:product :version) {:releases :r}
                                    (sql/where {:r.product "puppetdb"
                                                :r.version "1.0.0"}))
          release (jdbc/query *db* release-query)]
      (is (= 1 (count release))))))

(deftest test-make-user!
  (testing "tests make-user! with new ip address, querying users table should return exactly one row containing the test data."
    (make-user! *db* "255.0.255.0")
    (let [user-query (sql/select * :users
                                 (sql/where {:ip "255.0.255.0"}))
          user (jdbc/query *db* user-query)]
      (is (= 1 (count user)))))
  (testing "tests make-user! with old ip address, querying users table should return exactly one row containing the test data."
    (make-user! *db* "255.0.255.0")
    (make-user! *db* "255.0.255.0")
    (let [user-query (sql/select * :users
                                 (sql/where {:ip "255.0.255.0"}))
          user (jdbc/query *db* user-query)]
      (is (= 1 (count user)))))
  )

(deftest test-is-product?
  (testing "tests is-product? function with empty string, expecting false"
    (is (not (is-product? *db* ""))))
  (testing "tests is-product? function with invalid argument, expecting false"
    (is (not (is-product? *db* "This is a test!"))))
  (testing "tests is-product? function with valid argument, expecting true"
    (is (is-product? *db* "puppetdb"))))

(deftest test-get-release
  (let [pe-agent {:product "pe-agent" :version "1.0.0"
                  :link "http://www.puppetlabs.com"
                  :message "Message, that is I!"}]
    (testing "tests get-release with empty string, expecting empty collection"
      (is (empty? (get-release *db* ""))))
    (testing "tests get-release with invalid argument, expecting empty collection"
      (is (empty? (get-release *db* "This is a test!"))))
    (testing "tests get-release with valid argument, expecting a single row from releases table"
      (is (= pe-agent (get-release *db* "pe-agent"))))))

(deftest test-dump-req
  (let [test-releases
        {"puppetdb" {:product "puppetdb"
                     :version "1.0.1"
                     :link    "https://github.com/puppetlabs/puppetdb/blob/1.0.2/CHANGELOG.md"}
         "pe-master" {:version "3.0.0"
                      :message "Version 3.0.0 of Puppet Enterprise is available"
                      :product "pe-master"
                      :link    "http://links.puppetlabs.com/enterpriseupgrade"}
         "pe-agent"  {:version "3.0.0"
                      :message "Version 3.0.0 available for this Puppet Enterprise agent"
                      :product "pe-agent"
                      :link    "http://links.puppetlabs.com/enterpriseupgrade"}}
        pe-master (test-releases "pe-master")
        skynet {:version "6.6.6"
                :message "Prepare to meet absolution, human."
                :product "puppetdb"
                :link    "http://terminator.wikia.com/wiki/Skynet"}
        req1 {"product" "puppetdb" "version" "6.6.6" "ip" "24.8.63.199" "timestamp" (to-timestamp (now)) "params" {} }
        req2 {"product" "pe-master" "version" "3.0.0" "ip" "24.8.63.199" "timestamp" (to-timestamp (now)) "params" {} }]

    (testing "tests dump-req with single req, expecting query to return matching req data"
      (dump-req *db* req1)
      (is (= (req1 "timestamp")
             ((first (jdbc/query *db* (sql/select [:timestamp] :checkins))) :timestamp)))
      (is (= (req1 "ip")
             ((first (jdbc/query *db* (sql/select [:ip] :checkins))) :ip)))
      (is (= (req1 "product")
             ((first (jdbc/query *db* (sql/select [:product] :checkins))) :product)))
      (is (= (req1 "version")
             ((first (jdbc/query *db* (sql/select [:version] :checkins))) :version))))

    (testing "tests dump-req with second req, expecting query to return matching req data"
      (dump-req *db* req2)
      (is (= (req2 "timestamp")
             ((second (jdbc/query *db* (sql/select [:timestamp] :checkins))) :timestamp)))
      (is (= (req2 "ip")
             ((second (jdbc/query *db* (sql/select [:ip] :checkins))) :ip)))
      (is (= (req2 "product")
             ((second (jdbc/query *db* (sql/select [:product] :checkins))) :product)))
      (is (= (req2 "version")
             ((second (jdbc/query *db* (sql/select [:version] :checkins))) :version))))))

