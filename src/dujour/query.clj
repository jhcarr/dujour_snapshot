(ns dujour.query
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.sql :as sql]
            [korma.core :refer :all]
            [korma.db :refer :all]
            )
  (:gen-class))

(declare releases checkins params)

(defdb db
  {:classname "org.postgresql.Driver"
   :subprotocol "postgresql"
   :subname "//localhost:5432/dujourdb"})

(defentity releases
  (pk "(product, version)")
  (database db)
  (entity-fields :product :version :release_date)
  (has-many checkins {:fk (keyword "(product, version)")}))

(defentity checkins
  (pk "checkin_id")
  (database db)
  (entity-fields :product :version :timestamp :ip)
  (belongs-to releases {:fk (keyword "(product, version)")})
  (has-many params {:fk :checkin_id}))

(defentity params
  (pk "(param, checkin_id)")
  (database db)
  (entity-fields :param :value)
  (belongs-to checkins {:fk :checkin_id}))

(defn user-query
  [product]
  (-> (select* checkins)
      (modifier "DISTINCT")
      (fields :product
              :version
              (raw "date(timestamp)")
              :ip)
      (where {:product product})))
