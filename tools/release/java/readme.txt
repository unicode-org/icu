# Copyright (C) 2009-2010 IBM and Others. All Rights Reserved

API Change Report:

A tool to generate a report of API status changes between two ICU4C releases.
(ICU4J has a builtin change report generator)

Requirements:
  - Everything needed to build ICU4C from a command line (UNIX) environment
  - Doxygen (for generating docs)
  - Java JDK 1.5+
  - Apache Ant
  - Xalan/Xerces ( put the jars in the lib/ directory ) - the built in version to Java doesn't seem to work properly.

To use the utility:
    1. setup ICU
            (put the two ICU releases on your machine ^_^  )
            run 'configure' in both releases (or runConfigureICU)
            Doxygen must be found during configure.
	2. create a Makefile.local with just these two lines:
			OLD_ICU=/xsrl/E/icu-1.0
			NEW_ICU=/xsrl/E/icu-6.8
	   ( where these are the paths to the parent of 'source', etc)     
    3. Build the API docs
            make
	4. This will create an 'APIChangeReport.html' in this directory. Check it over, then check it in.
