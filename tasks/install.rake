# Task to install dujour's files into a target directory
#
# DESTDIR is defined in the top-level Rakefile
# JAR_FILE is defined in the ext/tar.rake file
#
desc "Install Dujour (DESTDIR optional argument)"
task :install => [  JAR_FILE  ] do
  unless File.exists?("ext/files/config.clj")
    Rake::Task[:template].invoke
  end

  require 'facter'
  raise "Oh damn. You need a newer facter or better facts. Facter version: #{Facter.version}" if Facter.value(:osfamily).nil?
  @osfamily = Facter.value(:osfamily).downcase
  mkdir_p "#{DESTDIR}/#{@install_dir}"
  mkdir_p "#{DESTDIR}/#{@log_dir}"
  mkdir_p "#{DESTDIR}/etc/init.d/"
  mkdir_p "#{DESTDIR}/#{@lib_dir}"
  mkdir_p "#{DESTDIR}/#{@lib_dir}/db"
  mkdir_p "#{DESTDIR}/#{@etc_dir}"
  ln_sf @log_dir, "#{DESTDIR}/#{@install_dir}/log"

  cp_p JAR_FILE, "#{DESTDIR}/#{@install_dir}"
  cp_p "ext/files/config.clj", "#{DESTDIR}/#{@etc_dir}"
  cp_p "ext/files/log4j.properties", "#{DESTDIR}/#{@etc_dir}"

  # figure out which init script to install based on facter
  if @osfamily == "redhat"
    mkdir_p "#{DESTDIR}/etc/sysconfig"
    mkdir_p "#{DESTDIR}/etc/rc.d/init.d/"
    cp_p "ext/files/#{@name}.default", "#{DESTDIR}/etc/sysconfig/#{@name}"
    cp_p "ext/files/#{@name}.redhat.init", "#{DESTDIR}/etc/rc.d/init.d/#{@name}"
    chmod 0755, "#{DESTDIR}/etc/rc.d/init.d/#{@name}"
  elsif @osfamily == "debian"
    mkdir_p "#{DESTDIR}/etc/default"
    cp_p "ext/files/#{@name}.default", "#{DESTDIR}/etc/default/#{@name}"
    cp_p "ext/files/#{@name}.debian.init", "#{DESTDIR}/etc/init.d/#{@name}"
    %x{ls "#{DESTDIR}/etc/init.d"}
    chmod 0755, "#{DESTDIR}/etc/init.d/#{@name}"
  else
    raise "Unknown or unsupported osfamily: #{@osfamily}"
  end
  chmod 0640, "#{DESTDIR}/#{@etc_dir}/log4j.properties"
end
