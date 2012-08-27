# dujour

Version-checking backend for PuppetDB (and I suppose others, eventually).

Available routes:

*<any URL>?product=some-product&version=some-version*

We'll lookup the latest available version for _product_, and compare it
semver-style to the _version_ query param (which we take to mean the "current"
version running on the client). We'll return a JSON object with the following
keys:

* version: string containing the latest available version for the indicated
  product

* newer: boolean indicating whether or not the above version should be
  considered newer than the supplied version. This may not always be a strict
  semver comparison...for example, if a beta version is available.

* link: a link to a page containing more information about the release

## Configuration

Look at the supplied config.clj.

## Usage

    # lein uberjar
    # java -Xmx32m -jar *standalone.jar config.clj

## License

Copyright (C) 2012 Puppet Labs

Apache Licensed
