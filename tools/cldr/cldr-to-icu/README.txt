*********************************************************************
*** © 2019 and later: Unicode, Inc. and others.                   ***
*** License & terms of use: http://www.unicode.org/copyright.html ***
*********************************************************************

Basic instructions for running the LdmlConverter via Maven
==========================================================

Note that these instructions do not currently support configuration of the converter for things
such as limiting the set of files produced. That is supported in code and could be easily added
to the binary, or encapsulated via an Ant task, but currently it is not directly supported.
See the IcuConverterConfig class for the API by which this can be supported.


Important directories
---------------------

<CLDR_DIR>  = The root directory of the CLDR release.

<ICU_DIR>   = The root directory of the ICU release (probably a parent directory of where
              this README file is located). This is an optional property and defaults to
              the parent directory of the release from which it is run.

<DTD_CACHE> = The temporary cache directory in which DTD files are downloaded (this is the
              same directory as would be used when running tools from the CLDR project).
              Note that the need to specify this directory is scheduled to be removed after
              ICU release 65.

<OUT_DIR>   = The output directory into which ICU data files should be written.


Generating all ICU data
-----------------------

$ mvn exec:java \
  -DCLDR_DIR='<CLDR_DIR>' \
  -DCLDR_DTD_CACHE='<DTD_CACHE>' \
  -Dexec.args='<OUT_DIR>'


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