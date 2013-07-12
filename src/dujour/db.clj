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

(defn product?
  "Checks the dujour database for whether a given product has
  an entry in the releases table."
  [database product]
  (not (empty? (jdbc/query database
                           (sql/select * :releases (sql/where {:product product}))))))

(defn get-release
  "Returns a record of the response information for a given release,
  i.e. link and message information."
  [database product]
  (let [sql-query (sql/select [:version :message :link :product]
                              :releases (sql/where {:product product}) (sql/order-by {:release_date :desc}))
        release-info (first (jdbc/query database sql-query))
        release-response (into {} (filter identity release-info))]
  (if (= product "puppetdb") (dissoc release-response :product :message) release-response)))

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
