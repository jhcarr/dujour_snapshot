(ns dujour.checkins
  (:require [ring.util.response :as rr]
            [cheshire.core :as json]
            [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [dujour.db :as db])
  (:use [clojure.string :only (join)]
        [clj-semver.core :only (newer?)]
        [clj-time.core :only (now)]
        [clj-time.coerce :only (to-timestamp)])
  (:gen-class))

(defmulti format-response
  (fn [fmt response-map] fmt))
(defmethod format-response "json"
  [fmt response-map]
  (json/generate-string response-map))
(defmethod format-response "txt"
  [fmt response-map]
  (join "\n" (for [[k v] response-map] (format "%s=%s" (name k) v)))
  )

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
          response-map (->> (assoc version-info :newer (newer? (:version version-info) version))
                            (remove (comp nil? val))
                            (into {}))
          resp (format-response fmt response-map)
          ]
      (rr/status (rr/response resp) 200))
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

(defn checkins-app
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
      (let [resp (make-response database product version format)]
        (when (= 200 (:status resp))
          (db/dump-req database (format-checkin request (now))))
        resp))))
