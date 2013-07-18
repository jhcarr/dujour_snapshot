(ns dujour.test.db
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.sql :as sql])
  (:use [dujour.db])
  (:use [clojure.test]
        [dujour.fixtures]))

(use-fixtures :each with-test-database)

(def database
  {:classname "org.postgresql.Driver"
  :subprotocol "postgresql"
  :subname "//localhost:5432/dujourdbtest"
  :user "justincarr"
  :password ""}
  )

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
  "Tests product? function with no argument, invalid argument and valid argument."
    (jdbc/insert! *db* :releases (test-releases "puppetdb"))
    (is (not (product? *db* "")))
    (is (not (product? *db* "This is a test!")))
    (is (product? *db* "puppetdb"))
    )

(deftest test-get-release
  "Tests get-release function with no argument, invalid argument and valid argument"
  (let [ pe-agent (test-releases "pe-agent") ]
    (jdbc/insert! *db* :releases pe-agent)
    (is (empty? (get-release *db* "")))
    (is (empty? (get-release *db* "This is a test!")))
    (is (= pe-agent (get-release *db* "pe-agent")))
    )
  )

(deftest test-dump-req
  "Tests dump-req ... but we're not sure how yet.")
