#! /bin/bash
#
#  Copyright (C) 2010, International Business Machines Corporation and others.  All Rights Reserved.                  
#
# Commands for regenerating ICU4C locale data (.txt files) from CLDR.
#
# The process requires local copies of
#    - CLDR (the source of the data, and some Java tools)
#    - ICU4J  (used by the conversion tools)
#    - ICU4C  (the destination for the new data)
#
# The versions of each of these must match.  Included with the release notes for ICU
# is the version number and/or a CLDR svn tag name for the revision of CLDR
# that was the source of the data for that release of ICU.
#
# Note: Some versions of the OpenJDK will not build the CLDR java utilities.
#   If you see compilation errors complaining about type incompatibilities with
#   functions on generic classes, try switching to the Sun JDK.
#
# Note: Enough things can fail in this process that it can be convenient to
#   run the commands separately from an interactive shell.  They should all
#   copy and paste without problems.
#
# Define the locations of icu4c, icu4j and cldr sources
# These three defines should be changed to match the locations on your machine.

export ICU4C_DIR=$HOME/icu/icu/trunk
export ICU4J_DIR=$HOME/icu/icu4j/trunk
export CLDR_DIR=$HOME/cldr/trunk

# Build ICU4J, including the cldr Utilities.

cd $ICU4J_DIR
ant jar
ant cldrUtil
export ICU4J_JAR=$ICU4J_DIR/icu4j.jar
export ICU4J_CLASSES=$ICU4J_DIR/out/cldr_util/bin

# Build the Java utilities included with the CLDR project

cd $CLDR_DIR/tools/java
ant all
export CLDR_CLASSES=$CLDR_DIR/tools/java/classes

# Set up a temporary director for caching the CLDR xml dtd.
#   This speeds up the data generation.

mkdir /tmp/cldrdtd
export ANT_OPTS="-DCLDR_DTD_CACHE=/tmp/cldrdtd"

# Build the ICU4C .txt data files.
# The new data will replace whatever was already present in the ICU4C sources
# This process will take several minutes.

cd $ICU4C_DIR/source/data
ant clean
ant all

# After rebuilding .txt files, ICU4C should be rebuilt to regenerate
# the new res files.  
