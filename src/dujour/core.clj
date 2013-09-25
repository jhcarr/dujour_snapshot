(ns dujour.core
  (:require [ring.util.response :as rr]
            [cheshire.core :as json]
            [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [dujour.jdbc :as dj-jdbc]
            [dujour.db :as db])
  (:use [ring.adapter.jetty :only (run-jetty)]
        [ring.middleware.params :only (wrap-params)]
        [ring-geoipviz.core :only (wrap-with-geoip wrap-with-buffer)]
        [clojure.tools.nrepl.server :only (start-server)]
        [clojure.string :only (join)]
        [clojure.walk :only (stringify-keys)]
        [clj-semver.core :only (newer?)]
        [dujour.migrations :only (migrate-db!)]
        [dujour.controllers.handler]
        [dujour.checkins :only (checkins-app)]
        [clj-time.core :only (now)]
        [clj-time.coerce :only (to-timestamp)]
        )
  (:gen-class))


(defn guarded-load-file
  "Evaluates all forms in `file` in a temporary namespace, returning
  the last thing evaluated."
  [file]
  (let [temp-ns (symbol "scratch")]
    (binding [*ns* (create-ns temp-ns)]
      (let [result (load-file file)]
        (remove-ns temp-ns)
        result))))

(defn make-webapp
  [database]
  {:pre [(map? database)]
   :post [(ifn? %)]}
  (let [app (routes (GET "/" [:as request] (checkins-app database request))
                    (context "/query" [] (query-app database))
                    (context "/dashboard" [] (dashboard-app)) 
                    )]
    (-> app
      (wrap-with-buffer #(assoc (:geoip %) :uri (:uri %)) "/geo" 100)
      (wrap-with-geoip [:headers "x-real-ip"])
      (wrap-params))))

(defn -main
  [& args]
  ;; Do not proceed if there is no config file
  (when (empty? args)
    (println "Need a configuration file, yo")
    (System/exit 1))

  (let [defaults {:host "localhost" :port 9990 :nrepl-port 9991}
        config (merge defaults
                      (guarded-load-file (first args)))
        nrepl-server (start-server :port (:nrepl-port config) :bind "localhost")
        {:keys [database]} config
        ;; Add a database type to connection mappings to use the correct SQL
        db (assoc (dj-jdbc/pooled-datasource database) :db-type (:subprotocol database))]
    (migrate-db! db)
    (run-jetty (make-webapp db) config)))
