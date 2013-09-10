(ns dujour.core
  (:require [ring.util.response :as rr]
            [cheshire.core :as json]
            [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [fs.core :as fs]
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
        [dujour.controllers.handler :only (query-app)]
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

(defn make-response
  "Build response comparing client's version to latest available"
  [database product version fmt]
  {:pre  [(map? database)
          (string? product)
          (string? version)
          (string? fmt)]
   :post [(map? %)]}
  (try
    (let [version-info (db/get-release database product)
          response-map
          (->> (assoc version-info :newer (newer? (:version version-info) version))
               (remove (comp nil? val))
               (into {}))
          resp (condp = fmt
                 "json"
                 (json/generate-string response-map)

                 "txt"
                 (join "\n" (for [[k v] response-map] (format "%s=%s" (name k) v))))
          ]
      (rr/response resp))
    (catch IllegalArgumentException msg
      (-> (rr/response
            (clojure.core/format "%s is not a valid semantic version number, yo" version))
          (rr/status 400)))))

(defn format-checkin
  [{:keys [params headers remote-addr]} timestamp]
  {:pre [(map? params)
         (params "product")
         (params "version")]
   :post [(map? %)]}
  (let [{:strs [product version]} params
        {:strs [x-real-ip]} headers
        ip (or x-real-ip remote-addr)
        other-params (dissoc params "fmt" "product" "version")
        ]
    {"product" product "version" version "timestamp" timestamp "ip" ip "params" other-params}))

(defn dump-req-and-resp
  "Ring middleware that dumps successfull (200) requests to a
  database."
  [database app]
  {:pre [(map? database)
         (ifn? app)]
   :post [(ifn? %)]}
  (fn [req]
    (let [resp (app req)]
      (when (= (:status resp) 200)
        (db/dump-req database (format-checkin req (now))))
      resp)))

(defn process-request
  "Checks for the correct query string parameters
  and responds to version requests."
  [database {:keys [params] :as request}]
  (let [{:strs [product version format] :or {format "json"}} params]
    (cond
      (not (and product version))
      (-> (rr/response "No product and/or version parameters in query, yo")
          (rr/status 400))

      (not (db/is-product? database product))
      (-> (rr/response (clojure.core/format  "%s is not a Puppet Labs product, yo" product))
          (rr/status 404))

      :else
      (make-response database product version format))))

(defn version-app
  [database]
  (routes (GET "/" [:as request] (process-request database request))))

(defn make-webapp
  [database]
  {:pre [(map? database)]
   :post [(ifn? %)]}
  (let [versions (dump-req-and-resp database (version-app database))
        query (query-app database)
        app (routes (context "/" [] versions)
                    (context "/query" [] query))]
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
