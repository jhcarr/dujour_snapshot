# Template task to handle all of dujour's erb templates
#
task :template => [ :clean ] do
   mkdir_p "ext/files/debian"
   # files for deb and rpm
   erb "ext/templates/config.clj.erb", "ext/files/config.clj"
   erb "ext/templates/log4j.properties.erb", "ext/files/log4j.properties"

   # files for deb
   erb "ext/templates/init_debian.erb", "ext/files/#{@name}.init"
   erb "ext/templates/dujour_default.erb", "ext/files/debian/#{@name}.default"
   erb "ext/templates/deb/control.erb", "ext/files/debian/control"
   erb "ext/templates/deb/prerm.erb", "ext/files/debian/#{@name}.prerm"
   erb "ext/templates/deb/postrm.erb", "ext/files/debian/#{@name}.postrm"
   erb "ext/templates/deb/base.install.erb", "ext/files/debian/#{@name}.install"
   erb "ext/templates/deb/rules.erb", "ext/files/debian/rules"
   chmod 0755, "ext/files/debian/rules"
   erb "ext/templates/deb/changelog.erb", "ext/files/debian/changelog"
   erb "ext/templates/deb/preinst.erb", "ext/files/debian/#{@name}.preinst"
   erb "ext/templates/deb/postinst.erb", "ext/files/debian/#{@name}.postinst"
   erb "ext/templates/init_debian.erb", "ext/files/#{@name}.debian.init"
   cp_pr FileList["ext/templates/deb/*"], "ext/files/debian"
   rm_rf FileList["ext/files/debian/*.erb"]

   # files for rpm
   erb "ext/templates/init_redhat.erb", "ext/files/dujour.redhat.init"
   erb "ext/templates/dujour_default.erb", "ext/files/dujour.default"

   # developer utility files for redhat
   mkdir_p "ext/files/dev/redhat"
   erb "ext/templates/dev/redhat/redhat_dev_preinst.erb", "ext/files/dev/redhat/redhat_dev_preinst"
end
