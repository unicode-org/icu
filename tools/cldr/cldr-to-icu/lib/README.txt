*********************************************************************
*** © 2019 and later: Unicode, Inc. and others.                   ***
*** License & terms of use: http://www.unicode.org/copyright.html ***
*********************************************************************

What is this directory and why is it empty?
-------------------------------------------

This is the root of a local Maven repository which needs to be populated before the
code in this project can be executed.

To do this, you need to have a local copy of the CLDR project configured on your
computer and be able able to build the API jar file and copy an existing utility
jar file. In the examples below it is assumed that <CLDR_ROOT> references this CLDR
release.


Regenerating the CLDR API jar
-----------------------------

To regenerate the CLDR API jar you need to build the "jar" target using the Ant
build.xml file in the "tools/java" directory of the CLDR project:

$ cd <CLDR_ROOT>/tools/java
$ ant clean jar

This should result in the cldr.jar file being built into that directory, which can then
be installed as a Maven dependency as described above.


Updating local Maven repository
-------------------------------

To update the local Maven repository (e.g. to install the CLDR jar) then from this
directory (lib/) you should run:

$ mvn install:install-file \
  -DgroupId=org.unicode.cldr \
  -DartifactId=cldr-api \
  -Dversion=0.1-SNAPSHOT \
  -Dpackaging=jar \
  -DgeneratePom=true \
  -DlocalRepositoryPath=. \
  -Dfile=<CLDR_ROOT>/tools/java/cldr.jar

And also (for the utility jar):

$ mvn install:install-file \
  -DgroupId=com.ibm.icu \
  -DartifactId=icu-utilities \
  -Dversion=0.1-SNAPSHOT \
  -Dpackaging=jar \
  -DgeneratePom=true \
  -DlocalRepositoryPath=. \
  -Dfile=<CLDR_ROOT>/tools/java/libs/utilities.jar

And if you have updated one of these libraries, run:

$ mvn dependency:purge-local-repository -DsnapshotsOnly=true

If you choose to update the version number, then remember to update the root pom.xml.
