*********************************************************************
*** © 2019 and later: Unicode, Inc. and others.                   ***
*** License & terms of use: http://www.unicode.org/copyright.html ***
*********************************************************************

What is this directory and why is it empty?
-------------------------------------------

This is the root of a local Maven repository which needs to be populated before
code which uses the CLDR data API can be executed.

To do this, you need to have a local copy of the CLDR project configured on your
computer and be able able to build the API jar file and copy an existing utility
jar file. In the examples below it is assumed that $CLDR_ROOT references this
CLDR release.

Setup
-----

This project relies on the Maven build tool for managing dependencies and uses
Ant for configuration purposes, so both will need to be installed. On a Debian
based system, this should be as simple as:

$ sudo apt-get install maven ant


Installing the CLDR API jar
---------------------------

From this directory:

$ ./install-cldr-jars.sh "$CLDR_ROOT"


Manually installing the CLDR API jar
------------------------------------

Only follow these remaining steps if the installation script isn't suitable or
doesn't work on your system.

To regenerate the CLDR API jar you need to build the "jar" target manually
using the Ant build.xml file in the "tools/java" directory of the CLDR project:

$ cd <CLDR_ROOT>/tools/java
$ ant clean jar

This should result in the cldr.jar file being built into that directory, which
can then be installed as a Maven dependency as described above.


Updating local Maven repository
-------------------------------

To update the local Maven repository (e.g. to install the CLDR jar) then from
this directory (lib/) you should run:

$ mvn install:install-file \
  -DgroupId=org.unicode.cldr \
  -DartifactId=cldr-api \
  -Dversion=0.1-SNAPSHOT \
  -Dpackaging=jar \
  -DgeneratePom=true \
  -DlocalRepositoryPath=. \
  -Dfile=<CLDR_ROOT>/tools/java/cldr.jar

And if you have updated one of these libraries then from the main project
directory (i.e. the directory the Maven pom.xml file(s) are in) run:

$ mvn dependency:purge-local-repository -DsnapshotsOnly=true

After doing this, you should see something like the following list of files in
this directory:

README.txt   <-- this file
org/unicode/cldr/cldr-api/maven-metadata-local.xml
org/unicode/cldr/cldr-api/0.1-SNAPSHOT/maven-metadata-local.xml
org/unicode/cldr/cldr-api/0.1-SNAPSHOT/cldr-api-0.1-SNAPSHOT.pom
org/unicode/cldr/cldr-api/0.1-SNAPSHOT/cldr-api-0.1-SNAPSHOT.jar

Finally, if you choose to update the version number of the snapshot, then also
update all the the pom.xml files which reference it (but this is unlikely to be
necessary).
