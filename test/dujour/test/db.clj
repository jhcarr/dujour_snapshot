(ns dujour.test.db
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.sql :as sql])
  (:use [dujour.db])
  (:use [clojure.test]
        [dujour.fixtures]
        [clj-time.core :only (now)]
        [clj-time.coerce :only (to-timestamp)]))

(use-fixtures :each with-test-database with-test-default-releases)

(deftest test-is-product?
    (testing "Tests is-product? function with empty string."
      (is (not (is-product? *db* ""))))
    (testing "Tests is-product? function with invalid argument."
      (is (not (is-product? *db* "This is a test!"))))
    (testing "Tests is-product? function with valid argument."
      (is (is-product? *db* "puppetdb"))))

(deftest test-get-release
  (let [pe-agent {:product "pe-agent" :version "1.0.0"
                  :link "http://www.puppetlabs.com"
                  :message "Message, that is I!"}]
    (testing "Tests get-release with empty string."
      (is (empty? (get-release *db* ""))))
    (testing "Tests get-release with invalid argument."
      (is (empty? (get-release *db* "This is a test!"))))
    (testing "Tests get-release with valid argument"
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

    (testing "Tests dump-req with single req"
      (dump-req *db* req1)
      (is (= (req1 "timestamp")
             ((first (jdbc/query *db* (sql/select [:timestamp] :checkins))) :timestamp)))
      (is (= (req1 "ip")
             ((first (jdbc/query *db* (sql/select [:ip] :checkins))) :ip)))
      (is (= (req1 "product")
             ((first (jdbc/query *db* (sql/select [:product] :checkins))) :product)))
      (is (= (req1 "version")
             ((first (jdbc/query *db* (sql/select [:version] :checkins))) :version))))

    (testing "Tests dump-req with second req"
      (dump-req *db* req2)
      (is (= (req2 "timestamp")
             ((second (jdbc/query *db* (sql/select [:timestamp] :checkins))) :timestamp)))
      (is (= (req2 "ip")
             ((second (jdbc/query *db* (sql/select [:ip] :checkins))) :ip)))
      (is (= (req2 "product")
             ((second (jdbc/query *db* (sql/select [:product] :checkins))) :product)))
      (is (= (req2 "version")
             ((second (jdbc/query *db* (sql/select [:version] :checkins))) :version))))))

