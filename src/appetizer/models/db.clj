(ns appetizer.models.db
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.sql :as sql]))

(defn db []
  {:classname "org.postgresql.Driver"
   :subprotocol "postgresql"
   :subname "//localhost:5432/dujourdb"
   :user "justincarr"
   :password "DR4g0n"})
