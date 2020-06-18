*********************************************************************
*** © 2019 and later: Unicode, Inc. and others.                   ***
*** License & terms of use: http://www.unicode.org/copyright.html ***
*********************************************************************

Basic instructions for running the LdmlConverter via Maven
==========================================================

Requirements
------------

* A CLDR release for supplying CLDR data and the CLDR API.
* The Maven build tool
* The Ant build tool

Important directories
---------------------

ICU_ROOT = The top-level directory for this ICU project installation.

CLDR_ROOT = The top-level directory for the CLDR project, containing CLDR
            code and non-production data. Usually obtained from:
            https://github.com/unicode-org/cldr

CLDR_DIR  = The top-level directory for the CLDR production data (typically
            the "production" directory in the staging repository). Usually
            obtained from:
            https://github.com/unicode-org/cldr-staging/tree/master/production

In Posix systems, it's best to set these as exported shell variables, and any
following instructions assume they have been set accordingly:

$ export ICU_ROOT=/path/to/icu
$ export CLDR_ROOT=/path/to/cldr
$ export CLDR_DIR=/path/to/cldr-staging/production

Note that you should not attempt to use data from the CLDR project directory
(where the CLDR API code exists) for conversion into ICU data. The process now
relies on a pre-processing step, and the CLDR data must come from the separate
"staging" repository (i.e. https://github.com/unicode-org/cldr-staging) or be
pre-processed locally into a different directory.


Initial Setup
-------------

This project relies on the Maven build tool for managing dependencies and uses
Ant for configuration purposes, so both will need to be installed. On a Debian
based system, this should be as simple as:

$ sudo apt-get install maven ant

You must also install an additional CLDR JAR file the local Maven repository at
$ICU_ROOT/tools/cldr/lib (see the README.txt in that directory for more
information).

$ cd "$ICU_ROOT/tools/cldr/lib"
$ ./install-cldr-jars.sh "$CLDR_ROOT"


Generating all ICU data
-----------------------

$ cd "$ICU_ROOT/tools/cldr/cldr-to-icu"
$ ant -f build-icu-data.xml


Other Examples
--------------

* Outputting a subset of the supplemental data into a specified directory:

  $ ant -f build-icu-data.xml -DoutDir=/tmp/cldr -DoutputTypes=plurals,dayPeriods

  Note: Output types can be listed with mixedCase, lower_underscore or UPPER_UNDERSCORE.
  Pass '-DoutputTypes=help' to see the full list.


* Outputting only a subset of locale IDs (and all the supplemental data):

  $ ant -f build-icu-data.xml -DoutDir=/tmp/cldr -DlocaleIdFilter='(zh|yue).*'


* Overriding the default CLDR version string (which normally matches the CLDR library code):

  $ ant -f build-icu-data.xml -DcldrVersion="36.1"


See build-icu-data.xml for documentation of all options and additional customization.


Running unit tests
------------------

$ mvn test -DCLDR_DIR="$CLDR_DIR"


Importing and running from an IDE
---------------------------------

This project should be easy to import into an IDE which supports Maven development, such
as IntelliJ or Eclipse. It uses a local Maven repository directory for the unpublished
CLDR libraries (which are included in the project), but otherwise gets all dependencies
via Maven's public repositories.
