{
 :port           9999
 :nrepl-port     10000

 :database
 {:classname "org.postgresql.Driver"
  :subprotocol "postgresql"
  :subname "//localhost:5432/dujourdb"
  :user "aroetker"
  :password "'"}


 :latest-version
 {"puppetdb" {:version "1.0.1"
              :link    "https://github.com/puppetlabs/puppetdb/blob/1.0.2/CHANGELOG.md"}
  "pe-master" {:version "3.0.0"
              :message "Version 3.0.0 of Puppet Enterprise is available"
              :product "pe-master"
              :link    "http://links.puppetlabs.com/enterpriseupgrade"}
 "pe-agent"  {:version "3.0.0"
              :message "Version 3.0.0 available for this Puppet Enterprise agent"
              :product "pe-agent"
              :link    "http://links.puppetlabs.com/enterpriseupgrade"}}
}
