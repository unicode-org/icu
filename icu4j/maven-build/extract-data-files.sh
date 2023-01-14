#!/bin/bash
# Copyright (C) 2023 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

# This command will copy data files from the ICU4J jars built for the
# Ant build by extracting and copying them over to the Maven build.
#
# If and when the Ant build is fully replaced by the Maven build, then the 
# upstream ICU4C make target `icu4j-data-install` can be modified accordingly
# to copy the files directly for Maven, and then allowing this script to be
# deleted.
#
# This script should be run from the root of the Maven build.

rm -rf maven-icu4j-datafiles/src/main/resources/*
rm -rf maven-icu4j-test-datafiles/src/main/resources/*

unzip ../main/shared/data/icudata.jar \
 -d ./maven-icu4j-datafiles/src/main/resources/ \
 -x *MANIFEST.MF

unzip ../main/shared/data/icutzdata.jar\
 -d ./maven-icu4j-datafiles/src/main/resources/ \
 -x *MANIFEST.MF

unzip ../main/shared/data/testdata.jar\
 -d ./maven-icu4j-test-datafiles/src/main/resources/ \
 -x *MANIFEST.MF