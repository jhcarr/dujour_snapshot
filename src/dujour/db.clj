(ns dujour.db
  (:require [cheshire.core :as json]
            [fs.core :as fs]
            [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.sql :as sql])
  (:use [clojure.string :only (join)]
        [clj-semver.core :only (newer?)]
        [clj-time.core :only (now)]
        [clj-time.format :only (formatters unparse)])
  (:gen-class))

(defn sql-database-table-names
  "Return all of the table names that are present in the database based on the
  current connection.  This is most useful for debugging / testing  purposes
  to allow introspection on the database.  (Some of our unit tests rely on this.)"
  [database]
  (let [sql-query (sql/select [:table_name] {:information_schema.tables :i}
                              ["LOWER(i.table_schema) = 'public'"])
        results (jdbc/query database sql-query)]
    (map :table_name results)))

(defn product?
  "Checks the dujour database for whether a given product has
  an entry in the releases table."
  [database product]
  (not (empty? (jdbc/query database
                           (sql/select * :releases (sql/where {:releases.product product}))))))

(defn get-release
  "Returns a record of the response information for a given release,
  i.e. link and message information."
  [database product]
  (let [sql-query (sql/select [:version :message :link :product]
                              :releases (sql/where {:releases.product product})
                              (sql/order-by {:releases.release_date :desc}))
        release-info (first (jdbc/query database sql-query))]
    (into {} (remove (comp nil? val) release-info))))

(defn dump-req
  "Inserts a request into a dujour configured PostgreSQL table"
  [database {:keys [request timestamp] :as req}]
  (let [{:keys [params headers]} request
        {:strs [product version]} params
        {:strs [x-real-ip]} headers
        checkin_id
        (:checkin_id
          (first (jdbc/insert! database :checkins {:timestamp timestamp
                                                  :ip x-real-ip
                                                  :product product
                                                  :version version})))
        other-params (dissoc params "fmt" "product" "version")]
    (when-not (empty? other-params)
      (apply jdbc/insert! database :params
                          (for [[param value] other-params]
                            {:checkin_id checkin_id :param param :value value})))))
