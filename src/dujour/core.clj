(ns dujour.core
  (:import [java.util UUID]
           [java.util.zip GZIPOutputStream])
  (:require [ring.util.response :as rr]
            [cheshire.core :as json]
            [fs.core :as fs])
  (:use [ring.adapter.jetty :only (run-jetty)]
        [ring.middleware.params :only (wrap-params)]
        [ring-geoipviz.core :only (wrap-with-geoip wrap-with-buffer)]
        [clj-semver.core :only (newer?)]
        [clj-time.core :only (now)]
        [clj-time.format :only (formatters unparse)]
        [clojure.java.io :only (output-stream)])
  (:gen-class))

(declare config)
(def defaults {:port 9990})

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
  [product current-version]
  {:pre  [(string? product)
          (string? current-version)]
   :post [(map? %)]}
  (let [version-info (get-in config [:latest-version product])]
    (-> version-info
      (assoc :newer (newer? (:version version-info) current-version))
      (json/generate-string)
      (rr/response))))

(defn dump-req-and-resp
  "Ring middleware that dumps successfull (200) requests to a
  directory on disk, one gzipped file per request."
  [app]
  (fn [req]
    (let [resp     (app req)
          payload  {:request   (dissoc req :body :ssl-client-cert)
                    ;; ^^ certain attributes of the response can't be
                    ;; serialized to JSON, like OutputStreams and such
                    :timestamp (System/currentTimeMillis)
                    :response  resp}
          datestr  (unparse (formatters :year-month-day) (now))
          filename (str (UUID/randomUUID) ".gz")
          path     (fs/file (config :dump-dir) datestr filename)
          output   (json/generate-string payload)]
      (when (= (:status resp) 200)
        (fs/mkdirs (fs/parent path))
        (spit (GZIPOutputStream. (output-stream path)) output))
      resp)))

(defn version-app
  [{:keys [params] :as request}]
  (let [{:strs [product version]} params]
    (cond
      (not (and product version))
      (-> (rr/response "malformed, yo")
          (rr/status 400))

      (not (get-in config [:latest-version product]))
      (-> (rr/response "unknown product")
          (rr/status 404))

      :else
      (make-response product version))))

(def webapp
  (-> version-app
      (dump-req-and-resp)
      (wrap-with-buffer :geoip "/geo" 100)
      (wrap-with-geoip [:headers "x-real-ip"])
      (wrap-params)))

(defn -main
  [& args]
  (def config (merge defaults
                     (guarded-load-file (first args))))
  (run-jetty webapp config))
