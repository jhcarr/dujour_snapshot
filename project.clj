(defproject dujour "1.0.0-SNAPSHOT"
  :description "Version checking backend for Puppet Labs projects."
  :plugins  [[ragtime/ragtime.lein "0.3.3"]]
  :dependencies [[org.clojure/clojure "1.4.0"]
                 ;; JSON encoding
                 [cheshire "5.2.0"]
                 [org.clojure/tools.nrepl "0.2.0-beta9"]
                 ;; Semantic versioning
                 [grimradical/clj-semver "0.1.0-SNAPSHOT"]
                 [grimradical/ring-geoipviz "0.1.0-SNAPSHOT"]
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
                 [korma "0.3.0-RC5"]
                 [postgresql/postgresql "9.1-901.jdbc4"]
                 [org.hsqldb/hsqldb "2.2.9"]
                 ;; Database Migrations
                 [ragtime "0.3.3"]]
  :ragtime {:migrations ragtime.sql.files/migrations
            :database "jdbc:postgresql://localhost:5432/dujourdb"}
  :profiles {:dev {:resource-paths ["test-resources"]
                   :dependencies [[ring-mock "0.1.1"]]}}
  :aot [dujour.core]
  :main dujour.core
)
