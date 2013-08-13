(ns dujour.query
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.sql :as sql]
            )
  (:use [korma.core]
        [korma.db]
        )
  )

(defentity releases
  (pk "(product, version)")
  (database dujourdb)
  )

(defentity checkins
  (pk "checkin_id")
  (has-one releases {:fk "(product,version)"})
  )

(defentity params
  (pk "(param, checkin_id)")
  (has-one releases {:fk "(product,version)"})
  )

(defn user-query
  []
  (let [])
  )
