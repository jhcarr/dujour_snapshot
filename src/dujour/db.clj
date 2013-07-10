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

(defn release?
  "Checks the dujour database for whether a given product and version has 
  an entry in the releases table."
  [database product version]
  (not (empty? (jdbc/query database
                           (sql/select * :releases (sql/where {:product product :version version}))))))

(defn dump-req
  "Inserts a request into a dujour configured PostgreSQL table"
  [database {:keys [request timestamp] :as req}]
  (let [{:keys [params uri headers]} request
        {:strs [x-real-ip connection user-agent accept host]} headers
        {:strs [product version fmt os database-name database-version]} params
        checkin_id
        (:checkin_id
          (jdbc/db-do-prepared-return-keys database true
                                           "INSERT INTO checkins (timestamp, uri, product, version) VALUES (?, ?, ?, ?)"
                                           [timestamp uri product version]))]
    (when checkin_id
      (jdbc/insert! database :headers
                    {:checkin_id checkin_id :x_real_ip x-real-ip :connection connection
                     :user_agent user-agent :accept accept :host host })
      (jdbc/insert! database :params
                    {:checkin_id checkin_id :database database-name
                     :database_version database-version :format fmt :os os}))))
