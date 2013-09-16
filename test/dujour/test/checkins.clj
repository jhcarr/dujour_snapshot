(ns dujour.test.checkins
  (:require [ring.util.response :as rr]
            [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.sql :as sql]
            [cheshire.core :as json]
            [dujour.db :as db])
  (:use [dujour.checkins])
  (:use [clojure.test]
        [dujour.fixtures]
        [ring.mock.request]
        [clj-semver.core :only (newer?)]
        [clojure.string :only (join)]
        [clj-time.core :only (now)]
        [clj-time.coerce :only (to-timestamp)]
        [clojure.walk :only (keywordize-keys)]))

(use-fixtures :each with-test-database with-test-default-releases)

(deftest test-make-response
  (let [product "puppetdb"
        version "1.0.1"
        bad-version "9.9.9.9"
        version-info (db/get-release *db* product)
        response-map
        (->> (assoc version-info :newer (newer? (:version version-info) version))
               (remove (comp nil? val))
               (into {}))]
    (testing "Tests make-response with invalid version information"
      (is (= (-> (rr/response
                  (clojure.core/format "%s is not a valid semantic version number, yo" bad-version))
                 (rr/status 400))
             (make-response *db* product bad-version "json"))))
    (testing "Tests make-response with valid information using json option"
      (is (= (rr/response (json/generate-string response-map))
             (make-response *db* product version "json"))))
    (testing "Tests make-response with valid information using text option"
      (is (= (rr/response (join "\n" (for [[k v] response-map] (format "%s=%s" (name k) v))))
             (make-response *db* product version "txt"))))))

(deftest test-format-checkin
  (let [x-real-ip-option {:params {"product" "HAL9000" "version" "1.0.0" "fmt" "json" "foo" "bar"}
                          :headers {"x-real-ip" "255.255.255.255"}
                          :fmt "json"}
        remote-addr-option {:params {"product" "GLaDOS" "version" "1.0.0" "fmt" "txt" "foo" "baz"}
                            :remote-addr "100.100.100.100"}
        t1 (to-timestamp (now))]
    (testing "Tests format-checkin with x-real-ip option"
      (is (= {"product" "HAL9000" "version" "1.0.0" "timestamp" t1 "ip" "255.255.255.255" "params" {"foo" "bar"}}
             (format-checkin x-real-ip-option t1))))
    (testing "Tests format-checkin with remote-addr option"
      (is (= {"product" "GLaDOS" "version" "1.0.0" "timestamp" (to-timestamp (now)) "ip" "100.100.100.100" "params" {"foo" "baz"}}
             (format-checkin remote-addr-option (to-timestamp (now))))))))

(deftest test-dump-req-and-resp
  (let [success-app (fn [param] {:status 200})
        fail-app (fn [param] {:status 400})
        req {:params {"product" "puppetdb" "version" "1.0.1" "foo" "bar"}
             :headers {"x-real-ip" "255.255.255.255"}
             :fmt "json"}
        keyworded-req (keywordize-keys req)
        ]

    (testing "tests dump-req-and-resp with invalid app"
      ((dump-req-and-resp *db* fail-app) req)
      (is (empty? (jdbc/query *db* (sql/select * :checkins)))))

    (testing "Tests dump-req-and-resp with valid app"
      ((dump-req-and-resp *db* success-app) req)
      (is (= (-> (dissoc (:params keyworded-req) :foo)
                 (assoc :ip (:x-real-ip (:headers keyworded-req))))
             (dissoc (first (jdbc/query *db* (sql/select * :checkins))) :timestamp :checkin_id))))))
