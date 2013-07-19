(ns dujour.test.db
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.sql :as sql])
  (:use [dujour.db])
  (:use [clojure.test]
        [dujour.fixtures]
        [clj-time.core :only (now)]
        [clj-time.coerce :only (to-timestamp)]))

(use-fixtures :each with-test-database)

(def test-releases
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
  )

(deftest test-product?
    (jdbc/insert! *db* :releases (test-releases "puppetdb"))
    (testing "Tests product? function with empty string."
      (is (not (product? *db* "")))
      )
    (testing "Tests product? function with invalid argument."
      (is (not (product? *db* "This is a test!")))
      )
    (testing "Tests product? function with valid argument."
      (is (product? *db* "puppetdb"))
      )
    )

(deftest test-get-release
  (let [ pe-agent (test-releases "pe-agent") ]
    (jdbc/insert! *db* :releases pe-agent)
    (testing "Tests get-release with empty string."
      (is (empty? (get-release *db* "")))
      )
    (testing "Tests get-release with invalid argument."
      (is (empty? (get-release *db* "This is a test!")))
      )
    (testing "Tests get-release with valid argument"
      (is (= pe-agent (get-release *db* "pe-agent")))
      )
    )
  )

(deftest test-dump-req
  (let [pe-master (test-releases "pe-master")
        skynet {:version "6.6.6"
                :message "Prepare to meet absolution, human."
                :product "Skynet"
                :link    "http://terminator.wikia.com/wiki/Skynet"}
        req1 {:request {:params {"product" "Skynet"
                                 "version" "6.6.6"}
                        :headers {"x-real-ip" "24.8.63.199"}
                        }
              :timestamp (to-timestamp (now))}
        req2 {:request {:params {"product" "pe-master"
                                 "version" "3.0.0"}
                        :headers {"x-real-ip" "24.8.63.199"}
                        }
              :timestamp (to-timestamp (now))}
        ]
    (jdbc/insert! *db* :releases pe-master)
    (jdbc/insert! *db* :releases skynet)

    (testing "Tests dump-req with single req"
      (dump-req *db* req1)
      (is (= (req1 :timestamp)
             ((first (jdbc/query *db* (sql/select [:timestamp] :checkins))) :timestamp))
          )
      (is (= (((req1 :request) :headers) "x-real-ip")
             ((first (jdbc/query *db* (sql/select [:ip] :checkins))) :ip))
          )
      (is (= (((req1 :request) :params) "product")
             ((first (jdbc/query *db* (sql/select [:product] :checkins))) :product))
          )
      (is (= (((req1 :request) :params) "version")
             ((first (jdbc/query *db* (sql/select [:version] :checkins))) :version))
          )
      )
    
    (testing "Tests dump-req with second req"
      (dump-req *db* req2)
      (is (= (req2 :timestamp)
             ((second (jdbc/query *db* (sql/select [:timestamp] :checkins))) :timestamp))
          )
      (is (= (((req2 :request) :headers) "x-real-ip")
             ((second (jdbc/query *db* (sql/select [:ip] :checkins))) :ip))
          )
      (is (= (((req2 :request) :params) "product")
             ((second (jdbc/query *db* (sql/select [:product] :checkins))) :product))
          )
      (is (= (((req2 :request) :params) "version")
             ((second (jdbc/query *db* (sql/select [:version] :checkins))) :version))
          )
      )
    )
  )

