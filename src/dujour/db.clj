(ns dujour.db
  (:require [cheshire.core :as json]
            [fs.core :as fs]
            [clojure.java.jdbc :as jdbc]
            [dujour.jdbc.ddl :as ddl]
            [clojure.java.jdbc.sql :as sql]
            [korma.core :refer :all]
            [korma.db :refer :all]
            [clojure.string :refer (join)]
            [clj-semver.core :refer (newer?)]
            [clj-time.core :refer (now)]
            [clj-time.format :refer (formatters unparse)]
            [clj-time.coerce :refer (to-timestamp)]
            )
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

(defn is-release?
  "Checks the dujour database for whether a given product and version has
  an entry in the releases table."
  [database product version]
  {:pre [(map? database)
         (string? product)
         (string? version)]}
  (not (empty? (jdbc/query database
                           (sql/select * :releases
                                       (sql/where {:releases.product product
                                                   :releases.version version}))))))

(defn make-release!
  "If a given product and version does not have release info in the database,
  make a table entry for it."
  [database product version]
  {:pre [(map? database)
         (string? product)
         (string? version)]}
  (when-not (is-release? database product version)
    (jdbc/insert! database :releases {:product product :version version})))

(defn product?
  "Checks the dujour database for whether a given product has
  an entry in the releases table."
  [database product]
  {:pre [(map? database)
         (string? product)]}
  (not (empty? (jdbc/query database
                           (sql/select * :releases (sql/where {:releases.product product}))))))

(defn get-release
  "Returns a record of the response information for a given release,
  i.e. link and message information."
  [database product]
  {:pre [(map? database)
         (string? product)]
   :post [(map? %)]}
  (let [sql-query (sql/select [:version :message :link :product]
                              :releases (sql/where {:releases.product product})
                              (sql/order-by {:releases.release_date :desc}))
        release-info (first (jdbc/query database sql-query))]
    ;; Still necessary since we do the same thing in core?
    (into {} (remove (comp nil? val) release-info))))

(defn dump-req
  "Inserts a formatted dujour request into a PostgreSQL table"
  [database req]
  {:pre [(map? database)
         (map? req)
         (string? (req "product"))
         (string? (req "version"))
         (string? (req "ip"))
         (map? (req "params"))]}
  (jdbc/db-transaction [conn database]
    (let [{:strs [product version ip timestamp params]} req]
      (make-release! conn product version)
      (let [checkin_id
            (:checkin_id (first (jdbc/insert! conn :checkins {:timestamp timestamp
                                                              :ip ip
                                                              :product product
                                                              :version version})))]
        (when-not (empty? params)
          (apply jdbc/insert! conn :params
                 (for [[param value] params]
                   {:checkin_id checkin_id :param param :value value})))))))
