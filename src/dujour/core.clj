(ns dujour.core
  (:import [java.util UUID]
           [java.util.zip GZIPOutputStream])
  (:require [ring.util.response :as rr]
            [cheshire.core :as json]
            [fs.core :as fs])
  (:use [ring.adapter.jetty :only (run-jetty)]
        [ring.middleware.params :only (wrap-params)]
        [ring-geoipviz.core :only (wrap-with-geoip wrap-with-buffer)]
        [clojure.tools.nrepl.server :only (start-server)]
        [clojure.string :only (join)]
        [clj-semver.core :only (newer?)]
        [clj-time.core :only (now)]
        [clj-time.format :only (formatters unparse)]
        [clojure.java.io :only (output-stream)])
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
  [version-info product client-version fmt]
  {:pre  [(string? product)
          (string? client-version)]
   :post [(map? %)]}
  (try
    (let [response-map (assoc version-info :newer (newer? (:version version-info) client-version))
          resp (condp = fmt
                 "json"
                 (json/generate-string response-map)

                 "txt"
                 (join "\n" (for [[k v] response-map] (format "%s=%s" (name k) v))))
          ]
      (rr/response resp))
    (catch IllegalArgumentException msg
      (-> (rr/response
            (clojure.core/format "%s is not a valid semantic version number, yo" client-version))
          (rr/status 400)))))

(defn dump-req-and-resp
  "Ring middleware that dumps successfull (200) requests to a
  directory on disk, one gzipped file per request."
  [dump-dir app]
  (fn [req]
    (let [resp     (app req)
          payload  {:request   (dissoc req :body :ssl-client-cert)
                    ;; ^^ certain attributes of the response can't be
                    ;; serialized to JSON, like OutputStreams and such
                    :timestamp (System/currentTimeMillis)
                    :response  resp}
          datestr  (unparse (formatters :year-month-day) (now))
          filename (str (UUID/randomUUID) ".gz")
          path     (fs/file dump-dir datestr filename)
          output   (json/generate-string payload)]
      (when (= (:status resp) 200)
        (fs/mkdirs (fs/parent path))
        (spit (GZIPOutputStream. (output-stream path)) output))
      resp)))

(defn version-app
  "Checks for the correct query string parameters
  and responds to version requests."
  [latest-version {:keys [params] :as request}]
  (let [{:strs [product version format] :or {format "json"}} params
        latest-version-info (latest-version product)]
    (cond
      (not (and product version))
      (-> (rr/response "No product and/or version parameters in query, yo")
          (rr/status 400))

      (not latest-version-info)
      (-> (rr/response (clojure.core/format "Unknown product %s, yo" product))
          (rr/status 404))

      :else
      (make-response latest-version-info product version format))))

(defn make-webapp
  [{:keys [dump-dir latest-version] :as config}]
  (let [app #(version-app latest-version %)]
  (-> (dump-req-and-resp dump-dir app)
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
        nrepl-server (start-server :port (:nrepl-port config) :bind "localhost")]
    (run-jetty (make-webapp config) config)))
