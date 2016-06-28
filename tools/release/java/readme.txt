# Copyright (C) 2016 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html
# Copyright (C) 2009-2013 IBM and Others. All Rights Reserved

API Change Report:

A tool to generate a report of API status changes between two ICU4C releases.
(ICU4J has a builtin change report generator)

Requirements:
  - Everything needed to build ICU4C from a command line (UNIX) environment
  - Doxygen (for generating docs).
     Doxygen 1.7.5.1 is recommended for ICU API docs, 
     but an older Doxygen may work for the API Change Report.
  - Java JDK 1.5+
  - Apache Ant
  - Note: if you have trouble with null output, put xalan.jar and xercesImpl.jar from <http://xalan.apache.org/xalan-j/downloads.html#latest-release> into ./lib/

To use the utility:
 1. Put both old and new ICU source trees on your system
 2. Run "configure" in both old and new (you can use any mixture of in-source and out-of-source builds). Doxygen must be found during the configure phase, but you do not need to build the standard API docs.
 3. create a Makefile.local in this readme's directory (tools/trunk/release/java/) 
            with just these two lines:
			OLD_ICU=/xsrl/E/icu-1.0
			NEW_ICU=/xsrl/E/icu-6.8
	   ( where these are the paths to the parent of 'source', etc)     
           If your ICU is an out-of-source-build, add these two lines
           indicating the build location:
                        OLD_ICU_BUILD=/xsrl/E/icu-build-m48
                        NEW_ICU_BUILD=/xsrl/E/icu-build
 4. from this directory, (tools/release/java/) run Make to build docs:
            make
 5. This will create an 'APIChangeReport.html' file in this directory. Look it over, and then check it in to ${NEW_ICU}/APIChangeReport.html (parent of icu/source).
