(ns dujour.jdbc.ddl
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.sql :as sql]))


(defn create-table
  "Given a table name and column specs with an optional table-spec
   return the DDL string for creating that table."
  [name & specs]
  (let [split-specs (partition-by #(= :table-spec %) specs)
        col-specs (first split-specs)
        table-spec (first (second (rest split-specs)))
        table-spec-str (or (and table-spec (str " " table-spec)) "")
        specs-to-string (fn [specs]
                          (apply str
                                 (map (sql/as-str identity)
                                      (apply concat
                                             (interpose [", "]
                                                        (map (partial interpose " ") specs))))))]
    (format "CREATE TABLE %s (%s)%s"
            (sql/as-str identity name)
            (specs-to-string col-specs)
            table-spec-str)))


(defn drop-table
  "Given a table name, return the DDL string for dropping that table."
  [name]
  (format "DROP TABLE IF EXISTS %s CASCADE" (sql/as-str identity name)))

(defn create-index
  "Given an index name, table name, vector of column names, and
  (optional) is-unique, return the DDL string for creating an index.

   Examples:
   (create-index :indexname :tablename [:field1 :field2] :unique)
   \"CREATE UNIQUE INDEX indexname ON tablename (field1, field2)\"

   (create-index :indexname :tablename [:field1 :field2])
   \"CREATE INDEX indexname ON tablename (field1, field2)\""
  [index-name table-name cols & is-unique]
  (let [cols-string (apply str
                           (interpose ", "
                                      (map (sql/as-str identity)
                                           cols)))
        is-unique (if is-unique "UNIQUE " "")]
    (format "CREATE %sINDEX %s ON %s (%s)"
            is-unique
            (sql/as-str identity index-name)
            (sql/as-str identity table-name)
            cols-string)))

(defn drop-index
  "Given an index name, return the DDL string for dropping that index."
  [name]
  (format "DROP INDEX %s" (sql/as-str identity name)))
