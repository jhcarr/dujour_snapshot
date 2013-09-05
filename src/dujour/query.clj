(ns dujour.query
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.sql :as sql]
            [clj-time.coerce :refer (to-timestamp)])
  (:gen-class))

(defn user-query
  [{:keys [subprotocol] :as database}]
  (let [date-fn-sql
        (case subprotocol
          "postgresql" "date(timestamp)"
          "hsqldb" "TO_DATE(timestamp, 'YYYY-MM-DD')")]
    (format "SELECT DISTINCT c.product, c.version, %s AS checkin_date, ip
            FROM checkins c
            JOIN releases r ON (c.product, c.version) = (r.product, r.version)
            JOIN params p ON c.checkin_id = p.checkin_id
            WHERE r.release_date IS NOT NULL"
            date-fn-sql date-fn-sql)))

(defn product-query
  [database product & prods]
  [(format "SELECT product, version, checkin_date, ip
           FROM (%s) pq1
           WHERE product = ?"
           (user-query database)) product])

(defn version-query
  [database product version]
  [(format "SELECT product, version, checkin_date, ip
           FROM (%s) pq1
           WHERE product = ? AND version = ?"
           (user-query database)) product version])

(defn new-users-query
  [{:keys [subprotocol] :as database} product version start-date end-date]
  (let [initial-query  (product-query database product)
        sql (format "SELECT product, version, checkin_date, ip
                    FROM (%s) nu1
                    WHERE checkin_date BETWEEN ? AND ?
                    AND ip NOT IN
                    (SELECT ip FROM checkins c JOIN releases r ON (c.product, c.version) = (r.product, r.version)
                    WHERE r.release_date IS NOT NULL AND timestamp <= ? AND c.product = ?)"
                    (first initial-query))
        params (conj (vec (rest initial-query))
                     (to-timestamp start-date) (to-timestamp end-date) (to-timestamp start-date) product)]
    (apply vector sql params)))
