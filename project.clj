(defproject dujour "1.0.0-SNAPSHOT"
  :description "Version checking backend for Puppet Labs projects."
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [cheshire "4.0.1"]
                 [org.clojure/tools.nrepl "0.2.0-beta9"]
                 [grimradical/clj-semver "0.1.0-SNAPSHOT"]
                 [grimradical/ring-geoipviz "0.1.0-SNAPSHOT"]
                 [clj-time "0.4.4"]
                 [fs "1.3.2"]
                 [ring/ring-core "1.1.3"]
                 [ring/ring-jetty-adapter "1.1.3"]
                 ;; Database connectivity
                 [org.clojure/java.jdbc "0.3.0-alpha4"]
                 [postgresql/postgresql "9.1-901.jdbc4"]
                 [org.hsqldb/hsqldb "2.2.9"]]
  :aot [dujour.core]
  :main dujour.core
)
