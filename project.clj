(defproject dujour "1.0.0-SNAPSHOT"
  :description "FIXME: write description"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [cheshire "4.0.1"]
                 [org.semver/api "0.9.14" :exclusions [ant/ant ant/ant-trax]]
                 [clj-time "0.4.4"]
                 [fs "1.3.2"]
                 [ring/ring-core "1.1.3"]
                 [ring/ring-jetty-adapter "1.1.3"]]
  :aot [dujour.core]
  :main dujour.core
)