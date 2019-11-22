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

<OUT_DIR>   = The output directory into which ICU data files should be written.


Initial Setup
-------------

This project relies on the Maven build tool for managing dependencies and uses
Ant for configuration purposes, so both will need to be installed. On a debian
based system, this should be as simple as:

$ sudo apt-get install maven ant

See also lib/README.txt for instructions on how to install the CLDR JAR files
which contains the CLDR API used by these tools. This step will only need to
be repeated when you update the CLDR project you are using.

Generating all ICU data
-----------------------

First edit the Ant build file to

$ CLDR_DIR=<CLDR_DIR> ant -f build-icu-data.xml


Running unit tests
------------------

$ mvn test -DCLDR_DIR=<CLDR_DIR>


Importing and running from an IDE
---------------------------------

This project should be easy to import into an IDE which supports Maven development, such
as IntelliJ or Eclipse. It uses a local Maven repository directory for the unpublished
CLDR libraries (which are included in the project), but otherwise gets all dependencies
via Maven's public repositories.
