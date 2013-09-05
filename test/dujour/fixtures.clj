(ns dujour.fixtures
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.sql :as sql])
  (:use [dujour.db]
        [clj-time.coerce :only (to-timestamp)]
        [dujour.migrations]
        [dujour.testutils])
  (:use [clojure.test]))

(def ^:dynamic *db* nil)

(defn with-test-database
  [function]
  (let [db (test-db)]
    (binding [*db* (assoc db :db-type (:subprotocol db))]
    (clear-db-for-testing! *db*)
    (migrate-db! *db*)
    (function))))


(defn with-test-default-releases
  [function]
  (jdbc/db-transaction [conn *db*]
                       (apply jdbc/insert! conn :products
                              (for [product ["pe-master" "pe-agent" "puppetdb"]] {:product product}))
                       (apply jdbc/insert! conn :releases
                              (for [[product version release_date link message]
                                    [["pe-master" "1.0.0" (to-timestamp "January 1, 1991")
                                      "http://www.puppetlabs.com"
                                      "Message, I thought you meant massage!"]
                                     ["pe-agent" "1.0.0" (to-timestamp "December 25, 1991")
                                      "http://www.puppetlabs.com"
                                      "Message, that is I!"]
                                     ["puppetdb" "1.0.0" (to-timestamp "February 20, 1991")
                                      "http://www.puppetlabs.com"
                                      "Message, brought to you by Puppet Labs!"]]]
                                {:product product
                                 :version version
                                 :release_date release_date
                                 :link link
                                 :message message})))
  (function))
