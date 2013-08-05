require 'rake'
require 'erb'
require 'facter'

JAR_FILE = 'dujour.jar'

RAKE_ROOT = File.dirname(__FILE__)

#LEIN_SNAPSHOTS_IN_RELEASE = 'y'

# Load tasks and variables for packaging automation
begin
  load File.join(RAKE_ROOT, 'ext', 'packaging', 'packaging.rake')
rescue LoadError
end

def ln_sf(src, dest)
  if !File.exist?(dest)
    sh "ln -sf #{src} #{dest}"
  end
end

def cp_pr(src, dest, options={})
  mandatory = {:preserve => true}
  cp_r(src, dest, options.merge(mandatory))
end

def cp_p(src, dest, options={})
  mandatory = {:preserve => true}
  cp(src, dest, options.merge(mandatory))
end

# We want to use dujour's package:tar and its dependencies, because it
# contains all the special java snowflake magicks, so we have to clear the
# packaging repo's. We also want to use dujour's clean task, since it has so
# much more clean than the packaging repo knows about
['package:tar', 'clean'].each do |task|
  Rake::Task[task].clear if Rake::Task.task_defined?(task)
end

@install_dir = "/usr/share/dujour"
@etc_dir = "/etc/dujour"
@initscriptname = "/etc/init.d/dujour"
@log_dir = "/var/log/dujour"
@lib_dir = "/var/lib/dujour"
@name = "dujour"

PATH = ENV['PATH']
DESTDIR=  ENV['DESTDIR'] || ''

@osfamily = (Facter.value(:osfamily) || "").downcase

@default_java_args = "-Xmx192m "

# All variables have been set, so we can load the dujour tasks
Dir[ File.join(RAKE_ROOT, 'tasks','*.rake') ].sort.each { |t| load t }

task :default => [ :package ]

task :allclean => [ :clobber ]

desc "Remove build artifacts (other than clojure (lein) builds)"
task :clean do
  rm_rf FileList["ext/files", "pkg", "*.tar.gz"]
end

desc "Get rid of build artifacts including clojure (lein) builds"
task :clobber => [ :clean ] do
  rm_rf FileList["target/dujour*jar"]
end

task :version do
  puts @version
end

file "ext/files/config.ini" => [ :template, JAR_FILE ]   do
end

desc 'Build deb package'
task :deb => [ 'package:implode', 'package:bootstrap', 'package:deb' ]

desc 'Build a Source rpm for dujour'
task :srpm => [ 'package:implode', 'package:bootstrap', 'package:srpm' ]

