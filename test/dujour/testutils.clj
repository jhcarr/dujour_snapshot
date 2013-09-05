(ns dujour.testutils
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.sql :as sql])
  (:use [dujour.db :only [sql-database-table-names]]))

(defn test-db-config
  "This is a placeholder function; it is supposed to return a map containing
  the database configuration settings to use during testing.  We expect for
  it to be overridden by another definition from the test config file, so
  this implementation simply throws an exception that would indicate that our
  config file was invalid or not read properly."
  []
  (throw (IllegalStateException.
           (str "No test database configuration found!  Please make sure that "
              "your test config file defines a no-arg function named "
             "'test-db-config'."))))

(defn load-test-config
  "Loads the test configuration file from the classpath.  First looks for
  `config/local.clj`, and if that is not found, falls back to
  `config/default.clj`.

  Returns a map containing the test configuration.  Current keys include:

    :testdb-config-fn : a no-arg function that returns a hash of database
        settings, suitable for passing to the various `clojure.java.jdbc`
        functions."
  []
  (binding [*ns* (create-ns 'dujour.testutils)]
    (try
      (load "/config/local")
      (catch java.io.FileNotFoundException ex
          (load "/config/default")))
    {
     :testdb-config-fn test-db-config
    }))

;; Memoize the loading of the test config file so that we don't have to
;; keep going back to disk for it.
(def test-config
  (memoize load-test-config))

(defn test-db
  "Return a map of connection attrs for the test database"
  []
  ((:testdb-config-fn (test-config))))

(defn clear-db-for-testing!
  "Completely clears the database, dropping all puppetdb tables and other objects
  that exist within it.  Expects to be called from within a db binding.  You
  Exercise extreme caution when calling this function!"
  [database]
  (let [drop-stmts (map #(format "DROP TABLE IF EXISTS %s CASCADE" %) (sql-database-table-names database))]
    (apply jdbc/db-do-commands database true drop-stmts)))
