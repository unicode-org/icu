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

TOOLS_ROOT: Path to root of ICU tools directory, below which are (e.g.) the
            cldr/ and unicodetools/ directories.

CLDR_DIR:   This is the path to the to root of standard CLDR sources, below
            which are the common/ and tools/ directories.

CLDR_DATA_DIR: The top-level directory for the CLDR production data (typically
               the "production" directory in the staging repository). Usually
               generated locally or obtained from:
               https://github.com/unicode-org/cldr-staging/tree/main/production

In Posix systems, it's best to set these as exported shell variables, and any
following instructions assume they have been set accordingly:

$ export TOOLS_ROOT=/path/to/icu/tools
$ export CLDR_DIR=/path/to/cldr
$ export CLDR_DATA_DIR=/path/to/cldr-staging/production

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
$TOOLS_ROOT/cldr/lib (see the README.txt in that directory for more
information).

$ cd "$TOOLS_ROOT/cldr/lib"
$ ./install-cldr-jars.sh "$CLDR_DIR"


Generating all ICU data and source code
---------------------------------------

$ cd "$TOOLS_ROOT/cldr/cldr-to-icu"
$ ant -f build-icu-data.xml


Other Examples
--------------

* Outputting a subset of the supplemental data into a specified directory:

  $ ant -f build-icu-data.xml -DoutDir=/tmp/cldr -DoutputTypes=plurals,dayPeriods -DdontGenCode=true

  Note: Output types can be listed with mixedCase, lower_underscore or UPPER_UNDERSCORE.
  Pass '-DoutputTypes=help' to see the full list.


* Outputting only a subset of locale IDs (and all the supplemental data):

  $ ant -f build-icu-data.xml -DoutDir=/tmp/cldr -DlocaleIdFilter='(zh|yue).*' -DdontGenCode=true


* Overriding the default CLDR version string (which normally matches the CLDR library code):

  $ ant -f build-icu-data.xml -DcldrVersion="36.1"


See build-icu-data.xml for documentation of all options and additional customization.


Running unit tests
------------------

$ mvn test -DCLDR_DIR="$CLDR_DATA_DIR"


Importing and running from an IDE
---------------------------------

This project should be easy to import into an IDE which supports Maven development, such
as IntelliJ or Eclipse. It uses a local Maven repository directory for the unpublished
CLDR libraries (which are included in the project), but otherwise gets all dependencies
via Maven's public repositories.
