;; ## Database utilities

(ns dujour.jdbc
  (:import (com.jolbox.bonecp BoneCPDataSource BoneCPConfig)
           (java.util.concurrent TimeUnit))
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.string :as string]))

(defn make-connection-pool
  "Create a new database connection pool"
  [{:keys [classname subprotocol subname username password
           partition-conn-min partition-conn-max partition-count
           stats log-statements log-slow-statements
           conn-max-age conn-keep-alive]
    :or   {partition-conn-min  1
           partition-conn-max  50
           partition-count     1
           stats               true
           ;; setting this to a String value, because that's what it would
           ;;  be in the config file and we're manually converting it to a boolean
           log-statements      "true"
           log-slow-statements 10
           conn-max-age        60
           conn-keep-alive     240}
    :as   db}]
  ;; Load the database driver class
  (Class/forName classname)
  (let [log-statements? (Boolean/parseBoolean log-statements)
        config          (doto (new BoneCPConfig)
                          (.setDefaultAutoCommit false)
                          (.setLazyInit true)
                          (.setMinConnectionsPerPartition partition-conn-min)
                          (.setMaxConnectionsPerPartition partition-conn-max)
                          (.setPartitionCount partition-count)
                          (.setStatisticsEnabled stats)
                          (.setIdleMaxAgeInMinutes conn-max-age)
                          (.setIdleConnectionTestPeriodInMinutes conn-keep-alive)
                          ;; paste the URL back together from parts.
                          (.setJdbcUrl (str "jdbc:" subprotocol ":" subname))
                          )]
    ;; configurable without default
    (when username (.setUsername config (str username)))
    (when password (.setPassword config (str password)))
    (when log-statements? (.setLogStatementsEnabled config log-statements?))
    (when log-slow-statements
      (.setQueryExecuteTimeLimit config log-slow-statements (TimeUnit/SECONDS)))
    ;; ...aaand, create the pool.
    (BoneCPDataSource. config)))

(defn pooled-datasource
  "Given a database connection attribute map, return a JDBC datasource
  compatible with clojure.java.jdbc that is backed by a connection
  pool."
  [options]
  {:datasource (make-connection-pool options)})
