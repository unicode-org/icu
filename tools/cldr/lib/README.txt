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

$ ./install-cldr-jars.sh "$CLDR_DIR"


Manually installing the CLDR API jar
------------------------------------

Only follow these remaining steps if the installation script isn't suitable or
doesn't work on your system.

To regenerate the CLDR API jar you need to build the "jar" target manually
using the Ant build.xml file in the "tools/java" directory of the CLDR project:

$ cd "$CLDR_ROOT/tools/java"
$ ant clean jar

This should result in the cldr.jar file being built into that directory, which
can then be installed as a Maven dependency as described above.


Updating local Maven repository
-------------------------------

To update the local Maven repository (e.g. to install the CLDR jar) then from
this directory (lib/) you should run:

$ mvn install:install-file \
  -Dproject.parent.relativePath="" \
  -DgroupId=org.unicode.cldr \
  -DartifactId=cldr-api \
  -Dversion=0.1-SNAPSHOT \
  -Dpackaging=jar \
  -DgeneratePom=true \
  -DlocalRepositoryPath=. \
  -Dfile="$CLDR_ROOT/tools/java/cldr.jar"

And if you have updated one of these libraries then from this directory run:

$ mvn dependency:purge-local-repository \
  -Dproject.parent.relativePath="" \
  -DmanualIncludes=org.unicode.cldr:cldr-api:jar

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

Troubleshooting
---------------

While the Maven system should keep the CLDR JAR up to date, there is a chance
that you may have an out of date JAR installed elsewhere. If you have any
issues with the JAR not being the expected version (e.g. after making changes)
then run the above "purge" step again, from this directory.

This should re-resolve the current JAR snapshot from the repository in this
directory. Having purged the Maven cache, next time you build a project, you
should see something like:

[exec] Downloading from <xxx>: <url>/org/unicode/cldr/cldr-api/0.1-SNAPSHOT/maven-metadata.xml
[exec] [INFO] Building jar: <path-to-icu-root>/tools/cldr/cldr-to-icu/target/cldr-to-icu-1.0-SNAPSHOT-jar-with-dependencies.jar

This shows that it has had to re-fetch the JAR file.
