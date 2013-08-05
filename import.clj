(ns import
  (:require [cheshire.core :as json]
            [clojure.java.jdbc.sql :as sql]
            [clojure.java.jdbc :as jdbc]
            [dujour.db :as db]
            [dujour.core :as core]
            [fs.core :as fs]
            )
  (:use [clojure.java.io]
        [clj-time.local :only (local-now)]
        [clj-time.coerce :only (to-timestamp from-sql-date)]
        [clj-time.format :only (formatters unparse)]
        ))

(def db
  {:classname "org.postgresql.Driver"
  :subprotocol "postgresql"
  :subname "//localhost:5432/dujourdb"})

(defn keywordify-keys
  [request]
  (into {} (for [[k v] request] [(keyword k) v])))

(defn last-dump-date
  "Gets the latest timestamp's date from dujourdb"
  [database]
  (let [sql-query (sql/select [:timestamp] {:checkins :ch} (sql/order-by {:ch.timestamp :desc}))
        newest-timestamp (:timestamp (first (jdbc/query database sql-query)))]
    ;; from-sql-date to cast the java.sql.Timestamp
    (unparse (formatters :date) (from-sql-date newest-timestamp))))

(defn dump-reqs-from-file
  "Parses and dumps a file with JSON dujour requests to the dujour psqldb"
  [database file]
  (println file)
  (doseq [{:strs [request timestamp]} (json/parsed-seq (java.io.FileReader. file))]
    (db/dump-req database (core/format-checkin (keywordify-keys request) (to-timestamp timestamp)))))

(defn import-dumps
  ""
  [database dump-dir-path]
  (dorun (->> dump-dir-path
    (file)
    (file-seq)
    (reverse)
    (filter fs/file?)
    (pmap #(dump-reqs-from-file database %)))))

(import-dumps db "/Users/aroetker/Projects/dujourdb/dumps")




