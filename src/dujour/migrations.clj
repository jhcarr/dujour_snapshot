(ns dujour.migrations
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.sql :as sql]
            [dujour.db :refer :all]
            [ragtime.core :refer :all]
            [ragtime.sql.database :refer :all]
            ))

(defn clear-database!
  [database]
  (let [ddl-drop-tables
        ["DROP TABLE IF EXISTS params CASCADE"
         "DROP TABLE IF EXISTS checkins CASCADE"
         "DROP TABLE IF EXISTS releases CASCADE"]]
    (apply jdbc/db-do-commands database true ddl-drop-tables)))

(defn init-database!
  [{:keys [subprotocol] :as database}]
  (let [checkin_id-sql
        (case subprotocol
          "postgresql" "SERIAL PRIMARY KEY"
          "hsqldb" "INTEGER GENERATED BY DEFAULT AS IDENTITY (START WITH 0) PRIMARY KEY")
        ddl-create-tables
        ["CREATE TABLE releases
           (product TEXT,
           version TEXT, release_date TIMESTAMP,
           link TEXT,
           message TEXT,
           PRIMARY KEY (product, version))",
         (format "CREATE TABLE checkins
                   (checkin_id %s,
                   product TEXT,
                   version TEXT,
                   timestamp TIMESTAMP,
                   ip TEXT,
                   FOREIGN KEY (product, version) REFERENCES releases (product, version) ON DELETE CASCADE)"
                 checkin_id-sql),
         "CREATE TABLE params
           (checkin_id INTEGER REFERENCES checkins(checkin_id) ON DELETE CASCADE,
           param TEXT,
           value TEXT,
           PRIMARY KEY (checkin_id, param))",
         "CREATE INDEX checkins_timestamp ON checkins (timestamp)",
         "CREATE INDEX checkins_ip ON checkins (ip)"]]
    (apply jdbc/db-do-commands database true ddl-create-tables)))

(def init-dujour-db
  {:id "init-dujour-db"
   :up init-database!
   :down clear-database!})

(defn remove-users-and-products!
  [database]
  (let [ddl-drop-tables
        ["DROP TABLE IF EXISTS users CASCADE"
         "DROP TABLE IF EXISTS products CASCADE"]]
    (apply jdbc/db-do-commands database true ddl-drop-tables)))

(defn add-users-and-products!
  [{:keys [subprotocol] :as database}]
  (let [ddl-commands
        ["CREATE TABLE users
           (ip TEXT PRIMARY KEY)"
         "CREATE TABLE products
           (product TEXT PRIMARY KEY)"
         "INSERT INTO users SELECT DISTINCT ip FROM checkins"
         "INSERT INTO products SELECT DISTINCT product FROM releases"
         "ALTER TABLE checkins ADD FOREIGN KEY (ip) REFERENCES users (ip) ON DELETE CASCADE"
         "ALTER TABLE releases ADD FOREIGN KEY (product) REFERENCES products (product) ON DELETE CASCADE"]]
    (apply jdbc/db-do-commands database true ddl-commands)))

(def add-users-and-products
  {:id "add-users-and-products"
   :up add-users-and-products!
   :down remove-users-and-products!})

(defn migrate-db!
  "Applies a list of migrations using a given strategy from ragtime
  :apply-new, :raise-error (default) or :rebase"
  [database]
  (let [migrations [init-dujour-db
                    add-users-and-products]]
    (migrate-all (map->SqlDatabase database) migrations)))