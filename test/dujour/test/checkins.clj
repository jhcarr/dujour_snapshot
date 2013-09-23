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

(deftest test-format-response
  (let [test-map {:foo "foo" :bar "bar"}]
    (testing "tests format response with json and txt"
      (is (= (format-response "json" test-map)
             (json/generate-string test-map)) "expecting matching json output")
      (is (= (format-response "txt" test-map)
             "foo=foo\nbar=bar") "expecting matching text string output"))))

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


(deftest test-checkins-app
  (let [empty-product {:params {"version" "1.0.0"}
                       :headers {"x-real-ip" "0.0.0.0"}
                       :fmt "json"}
        empty-version {:params {"product" "BASS.EXE"}
                       :headers {"x-real-ip" "0.0.0.0"}
                       :fmt "json"}
        invalid-req {:params {"product" "CYLON" "version" "1.0.0" "fmt" "json" "foo" "bar"}
                     :headers {"x-real-ip" "1.1.1.1"}
                     :fmt "json"}
        success-req {:params {"product" "puppetdb" "version" "1.0.0" "fmt" "json" "baz" "buz"}
                     :headers {"x-real-ip" "2.2.2.2"}
                     :fmt "json"}]
   (testing "testing with empty request fields, expecting status 400 response"    
     (is (-> (checkins-app *db* empty-product)
             (:status)
             (= 400)) "empty product :: status 400")
     (is (-> (checkins-app *db* empty-version)
             (:status)
             (= 400)) "empty version :: status 400"))

   (testing "testing with invalid request, expecting status 404 response"
     (is (-> (checkins-app *db* invalid-req)
             (:status)
             (= 404))))

   (testing "testing with valid request, expecting status 200 and database write"
     (is (= 200 (:status (checkins-app *db* success-req))) "expecting status 200")
     (is (= 1 (count (jdbc/query *db* (sql/select * :checkins)))) "expecting a single entry in the checkins table.")
     (is (= (dissoc (first (jdbc/query *db* (sql/select * :checkins))) :timestamp :checkin_id)
            {:ip "2.2.2.2",             
             :version "1.0.0",
             :product "puppetdb"}) "expecting matching row from database query"))))
