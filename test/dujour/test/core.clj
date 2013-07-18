(ns dujour.test.core
  (:use [dujour.core])
  (:use [clojure.test]
        [dujour.fixtures]))

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
