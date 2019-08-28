*********************************************************************
*** © 2019 and later: Unicode, Inc. and others.                   ***
*** License & terms of use: http://www.unicode.org/copyright.html ***
*********************************************************************

Basic instructions for running the LdmlConverter via Maven
==========================================================

Important directories
---------------------

<CLDR_DIR>  = The root directory of the CLDR release.

<OUT_DIR>   = The output directory into which ICU data files should be written.


Generating all ICU data
-----------------------

First edit the Ant build file to

$ CLDR_DIR=<CLDR_DIR> ant -f build-icu-data.xml


Running unit tests
------------------

$ mvn test \
  -DCLDR_DIR='<CLDR_DIR>' \
  -DCLDR_DTD_CACHE='<DTD_CACHE>'


Importing and running from an IDE
---------------------------------

This project should be easy to import into an IDE which supports Maven development, such
as IntelliJ or Eclipse. It uses a local Maven repository directory for the unpublished
CLDR libraries (which are included in the project), but otherwise gets all dependencies
via Maven's public repositories.