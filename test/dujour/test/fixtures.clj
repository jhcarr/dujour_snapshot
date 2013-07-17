(ns dujour.test.fixtures
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.sql :as sql]
            [dujour.jdbc.ddl :as ddl])
  (:use [dujour.db]
        [dujour.testutils])
  (:use [clojure.test]))

(def ^:dynamic *db* nil)

(defn init!
  [database]
  (do
    (ddl/create-table :releases
                  [:product "text" "PRIMARY KEY"]
                  [:version "text"]
                  ["release_date" "TIMESTAMP"]
                  ["link" "text"]
                  ["message" "text"]
                  ["PRIMARY KEY" "(product, version)"])
    (ddl/create-table :checkins
                  [:checkin_id "text" "SERIAL" "PRIMARY KEY"]
                  [:product "text"]
                  [:version "text"]
                  [:timestamp "TIMESTAMP"]
                  ["FOREIGN KEY" "(product, version)" "REFERENCES" "releases(product, version)"
                   "ON CASCADE DELETE"]
                  [:ip "text"])
    (ddl/create-table :params
                  [:param "text"]
                  [:value "text"]
                  [:checkin_id "INTEGER" "REFERENCES" "checkins(checkin_id)" 
                   "ON DELETE CASCADE"]
                  ["PRIMARY KEY" "(checkin_id)"])))

(defn with-test-database
  [function]
  (binding [*db* (test-db)]
    (clear-db-for-testing! [*db*])
    (init! *db*)
    (function)))
