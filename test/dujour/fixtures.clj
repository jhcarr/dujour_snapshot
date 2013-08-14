(ns dujour.fixtures
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.sql :as sql]
            [dujour.jdbc.ddl :as ddl])
  (:use [dujour.db]
        [dujour.migrations]
        [dujour.testutils])
  (:use [clojure.test]))

(def ^:dynamic *db* nil)

(defn with-test-database
  [function]
  (binding [*db* (test-db)]
    (clear-db-for-testing! *db*)
    (migrate-db! *db*)
    (function)))
