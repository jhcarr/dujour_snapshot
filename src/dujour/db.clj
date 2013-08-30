(ns dujour.db
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.sql :as sql]
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
                              ;; Do we need a "WHERE" in here?
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

(defn is-user?
  "Checks the dujour database for whether a given ip has
  an entry in the users table."
  [database ip]
  {:pre [(map? database)
         (string? ip)]}
  (not (empty? (jdbc/query database
                           (sql/select * :users
                                       (sql/where {:users.ip ip}))))))

(defn make-release!
  "If a given product and version does not have release info in the database,
  make a table entry for it."
  [database product version]
  {:pre [(map? database)
         (string? product)
         (string? version)]}
  (when-not (is-release? database product version)
    (jdbc/insert! database :releases {:product product :version version})))

(defn make-user!
  "If a given ip does not have user info in the database,
  make a table entry for it."
  [database ip]
  {:pre [(map? database)
         (string? ip)]}
  (when-not (is-user? database ip)
    (jdbc/insert! database :users {:ip ip})))

(defn is-product?
  "Checks the dujour database for whether a given product has
  an entry in the products table."
  [database product]
  {:pre [(map? database)
         (string? product)]}
  (not (empty? (jdbc/query database
                           (sql/select * :products (sql/where {:products.product product}))))))

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
    release-info))

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
      (make-user! conn ip)
      (let [checkin_id
            (:checkin_id (first (jdbc/insert! conn :checkins {:timestamp (to-timestamp timestamp)
                                                              :ip ip
                                                              :product product
                                                              :version version})))]
        (when-not (empty? params)
          (apply jdbc/insert! conn :params
                 (for [[param value] params]
                   {:checkin_id checkin_id :param param :value value})))))))
