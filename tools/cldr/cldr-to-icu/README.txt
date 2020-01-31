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

<CLDR_DIR>  = The top-level directory for the CLDR production data (typically
              the "production" directory in the staging repository).

Note that you should not attempt to use data from the CLDR project directory
(were the CLDR API code exists) for conversion into ICU data. The process now
relies on a pre-processing step, and the CLDR data must come from the separate
"staging" repository (i.e. https://github.com/unicode-org/cldr-staging) or be
pre-processed locally into a different directory.


Initial Setup
-------------

This project relies on the Maven build tool for managing dependencies and uses
Ant for configuration purposes, so both will need to be installed. On a debian
based system, this should be as simple as:

$ sudo apt-get install maven ant

You also need to follow the instructions in lib/README.txt to install the CLDR
JAR files, which contain the CLDR API used by these tools. This step will only
need to be repeated if you update the code in the CLDR project you are using.

Generating all ICU data
-----------------------

$ export CLDR_DIR="<CLDR_DIR>"
$ ant -f build-icu-data.xml


Other Examples (assuming CLDR_DIR is set)
-----------------------------------------

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
