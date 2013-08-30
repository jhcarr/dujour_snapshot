(require '[clojure.string :as s])
(use '[clojure.java.shell :only  (sh)]
     '[clojure.java.io :only  (file)])

(def version-string
  (memoize
  (fn []
    "Determine the version number using 'rake version -s'"
    (if (.exists (file "version"))
      (s/trim (slurp "version"))
      (let [command                ["rake" "package:version" "-s"]
            {:keys [exit out err]} (apply sh command)]
        (if (zero? exit)
          (s/trim out)
          "0.0-dev-build"))))))

(defproject dujour (version-string)
  :description "Version checking backend for Puppet Labs projects."
  :plugins  [[ragtime/ragtime.lein "0.3.3"]
             [lein-ring "0.8.5"]]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 ;; Routing Library
                 [compojure "1.1.5"]
                 ;; JSON encoding
                 [cheshire "5.2.0"]
                 [org.clojure/tools.nrepl "0.2.0-beta9"]
                 [grimradical/clj-semver "0.1.0"]
                 [grimradical/ring-geoipviz "0.1.0"]
                 [clj-time "0.5.1"]
                 [fs "1.3.2"]
                 [ring/ring-core "1.1.3"]
                 [ring/ring-jetty-adapter "1.1.3"]
                 [log4j "1.2.16" :exclusions [javax.mail/mail
                                              javax.jms/jms
                                              com.sun.jdmk/jmxtools
                                              com.sun.jmx/jmxri]]
                 ;; Database connectivity
                 [com.jolbox/bonecp "0.7.1.RELEASE" :exclusions [org.slf4j/slf4j-api]]
                 ;; Configure jetty to use log4
                 [org.slf4j/slf4j-log4j12 "1.7.5"]
                 [org.clojure/java.jdbc "0.3.0-alpha4"]
                 [postgresql/postgresql "9.1-901.jdbc4"]
                 [org.hsqldb/hsqldb "2.2.9"]
                 ;; Database Migrations
                 [ragtime "0.3.3"]]
  :ragtime {:migrations ragtime.sql.files/migrations
            :database "jdbc:postgresql://localhost:5432/dujourdb"}
  :ring {:handler dujour.controllers.handler/app :port 4000}
  :profiles {:dev {:resource-paths ["test-resources"]
                   :dependencies [[ring-mock "0.1.5"]]}}
  :aot [dujour.core]
  :main dujour.core
)

