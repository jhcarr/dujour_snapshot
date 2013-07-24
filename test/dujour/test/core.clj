(ns dujour.test.core
  (:require [ring.util.response :as rr]
            [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.sql :as sql]
            [cheshire.core :as json]
            [dujour.db :as db])
  (:use [dujour.core])
  (:use [clojure.test]
        [dujour.fixtures]
        [clj-semver.core :only (newer?)]
        [clojure.string :only (join)]
        [clj-time.core :only (now)]
        [clj-time.coerce :only (to-timestamp)]))


(use-fixtures :each with-test-database)

(deftest response-making
  (let [latest-versions 
        {"puppetdb" {:version "1.0.1"
                     :link    "https://github.com/puppetlabs/puppetdb/blob/1.0.2/CHANGELOG.md"}
         "pe-master" {:version "2.7.2"
                      :message "Version 2.7.2 of Puppet Enterprise is available"
                      :product "pe-master"
                      :link    "http://links.puppetlabs.com/enterpriseupgrade"}
         "pe-agent"  {:version "2.7.2"
                      :message "Version 2.7.2 available for this Puppet Enterprise agent"
                      :product "pe-agent"
                      :link    "http://links.puppetlabs.com/enterpriseupgrade"}}]
    (is (thrown? AssertionError (make-response (latest-versions "puppetdb") [] "2.7.2" "txt")))))

(def test-releases
  {"puppetdb" {:product "puppetdb"
               :version "1.0.1"
               :link    "https://github.com/puppetlabs/puppetdb/blob/1.0.2/CHANGELOG.md"}
   "pe-master" {:version "3.0.0"
                :message "Version 3.0.0 of Puppet Enterprise is available"
                :product "pe-master"
                :link    "http://links.puppetlabs.com/enterpriseupgrade"}
   "pe-agent"  {:version "3.0.0"
                :message "Version 3.0.0 available for this Puppet Enterprise agent"
                :product "pe-agent"
                :link    "http://links.puppetlabs.com/enterpriseupgrade"}}
  )

(def missing-product-req
  {:params { "version" "2.7.2" "format" "json"}}
  )

(def invalid-product-req
  {:params {"product" "Skynet" "version" "6.6.6" "format" "json"}}
  )

(def valid-req
  {:params {"product" "pe-agent" "version" "2.7.2" "format" "json"}}
  )

(deftest test-version-app
  (jdbc/insert! *db* :releases (test-releases "puppetdb"))
  (jdbc/insert! *db* :releases (test-releases "pe-master"))
  (jdbc/insert! *db* :releases (test-releases "pe-agent"))
  (testing "Tests version-app with missing product field."
    (is (=
         (-> (rr/response "No product and/or version parameters in query, yo")
             (rr/status 400))
         (version-app *db* missing-product-req)
         )
        )
    )
  (testing "Tests version-app with invalid product and version"
    (is (= (-> (rr/response (clojure.core/format "%s is not a Puppet Labs product, yo" ((:params invalid-product-req) "product")))
               (rr/status 404))
           (version-app *db* invalid-product-req)
           )
        )    
    )
  (testing "Tests version-app with valid product and version"
    (let [product ((:params valid-req) "product")
          version ((:params valid-req) "version")
          format ((:params valid-req) "format")]
      (is (= (make-response *db* product version format)
             (version-app *db* valid-req)))
      )
    )
  )

(deftest test-make-response
  (jdbc/insert! *db* :releases (test-releases "puppetdb"))
  (jdbc/insert! *db* :releases (test-releases "pe-master"))
  (jdbc/insert! *db* :releases (test-releases "pe-agent"))
  (let [product ((test-releases "puppetdb") :product)
        version ((test-releases "puppetdb") :version)
        bad-version "9.9.9.9"
        version-info (db/get-release *db* product)
        response-map
        (->> (assoc version-info :newer (newer? (:version version-info) version))
               (remove (comp nil? val))
               (into {}))
        ]
    (testing "Tests make-response with invalid version information"
      (is (= (-> (rr/response
                  (clojure.core/format "%s is not a valid semantic version number, yo" bad-version))
                 (rr/status 400))
             (make-response *db* product bad-version "json")
             )
          )
      )
    (testing "Tests make-response with valid information using json option"
      (is (= (rr/response (json/generate-string response-map))
             (make-response *db* product version "json")))
      )
    (testing "Tests make-response with valid information using text option"
      (is (= (rr/response (join "\n" (for [[k v] response-map] (format "%s=%s" (name k) v))))
             (make-response *db* product version "txt")))
      )
    )
  )

(deftest test-format-checkin
  (let [x-real-ip-option {:params {"product" "HAL9000"
                                   "version" "1.0.0"
                                   "fmt" "json"
                                   "foo" "bar"}
                          :headers {"x-real-ip" "255.255.255.255"}
                          :fmt "json"
                          }
        remote-addr-option {:params {"product" "GLaDOS"
                                     "version" "1.0.0"
                                     "fmt" "txt"
                                     "foo" "baz"}
                            :remote-addr "100.100.100.100"
                            }
        ]
    (testing "Tests format-checkin with x-real-ip option"
      (is (= {"product" "HAL9000" "version" "1.0.0" "timestamp" (to-timestamp (now)) "ip" "255.255.255.255" "params" {"foo" "bar"}}
             (format-checkin x-real-ip-option (to-timestamp (now)))))
      )
    (testing "Tests format-checkin with remote-addr option"
      (is (= {"product" "GLaDOS" "version" "1.0.0" "timestamp" (to-timestamp (now)) "ip" "100.100.100.100" "params" {"foo" "baz"}}
             (format-checkin remote-addr-option (to-timestamp (now)))))
      
      )
    )
  )
