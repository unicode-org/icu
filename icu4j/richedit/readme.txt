#*******************************************************************************
#* Copyright (C) 2009, International Business Machines Corporation and         *
#* others. All Rights Reserved.                                                *
#*******************************************************************************

ICU4J Rich Edit app was moved from the icu4j SVN repository to the icuapps
repository on 2009-06-16.

To build ICU Rich Edit app, create build-local.properties and define following
properties:

icu4j.core.jar = <icu4j core jar file path>
icu4j.test-framework.jar = <icu4j test framework jar file path>
icu4j.license.html = <icu4j license HTML file>

Build output files are created in ./out directory (richtext.jar and richtext.zip)

For supported build targets, see the target descriptions by ant -p.
