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
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [cheshire "4.0.1"]
                 [org.clojure/tools.nrepl "0.2.0-beta9"]
                 [grimradical/clj-semver "0.1.0"]
                 [grimradical/ring-geoipviz "0.1.0"]
                 [clj-time "0.4.4"]
                 [fs "1.3.2"]
                 [ring/ring-core "1.1.3"]
                 [ring/ring-jetty-adapter "1.1.3"]]
  :aot [dujour.core]
  :main dujour.core
)

