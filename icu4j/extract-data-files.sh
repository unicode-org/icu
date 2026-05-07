#!/usr/bin/env bash
# Copyright (C) 2023 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

# Check that all the .jar files are present
JAR_FILE=main/shared/data/icudata.jar
if [ ! -f "$JAR_FILE" ]; then
  echo "Missing $JAR_FILE"
  exit
fi
JAR_FILE=main/shared/data/icutzdata.jar
if [ ! -f "$JAR_FILE" ]; then
  echo "Missing $JAR_FILE"
  exit
fi
JAR_FILE=main/shared/data/testdata.jar
if [ ! -f "$JAR_FILE" ]; then
  echo "Missing $JAR_FILE"
  exit
fi

# Unpack the pre-built .jar files with data only

echo "Unpacking icudata.jar"
rm -fr      main/core/src/main/resources/com/ibm/icu/impl/data/icudata
unzip -q -d main/core/src/main/resources/ main/shared/data/icudata.jar
rm -fr      main/core/src/main/resources/META-INF

echo "  Moving charset data"
rm   -fr main/charset/src/main/resources/com/ibm/icu/impl/data/icudata
mkdir -p main/charset/src/main/resources/com/ibm/icu/impl/data/icudata/
mv       main/core/src/main/resources/com/ibm/icu/impl/data/icudata/*.cnv        main/charset/src/main/resources/com/ibm/icu/impl/data/icudata/
mv       main/core/src/main/resources/com/ibm/icu/impl/data/icudata/cnvalias.icu main/charset/src/main/resources/com/ibm/icu/impl/data/icudata/

echo "  Moving currency data"
rm   -fr main/currdata/src/main/resources/com/ibm/icu/impl/data/icudata
mkdir -p main/currdata/src/main/resources/com/ibm/icu/impl/data/icudata/
mv       main/core/src/main/resources/com/ibm/icu/impl/data/icudata/curr         main/currdata/src/main/resources/com/ibm/icu/impl/data/icudata/curr

echo "  Moving collate data"
rm   -fr main/collate/src/main/resources/com/ibm/icu/impl/data/icudata
mkdir -p main/collate/src/main/resources/com/ibm/icu/impl/data/icudata/
mv       main/core/src/main/resources/com/ibm/icu/impl/data/icudata/coll         main/collate/src/main/resources/com/ibm/icu/impl/data/icudata/coll

echo "  Moving langdata data"
rm   -fr main/langdata/src/main/resources/com/ibm/icu/impl/data/icudata
mkdir -p main/langdata/src/main/resources/com/ibm/icu/impl/data/icudata/
mv       main/core/src/main/resources/com/ibm/icu/impl/data/icudata/lang         main/langdata/src/main/resources/com/ibm/icu/impl/data/icudata/lang

echo "  Moving regiondata data"
rm   -fr main/regiondata/src/main/resources/com/ibm/icu/impl/data/icudata
mkdir -p main/regiondata/src/main/resources/com/ibm/icu/impl/data/icudata/
mv       main/core/src/main/resources/com/ibm/icu/impl/data/icudata/region       main/regiondata/src/main/resources/com/ibm/icu/impl/data/icudata/region

echo "  Moving translit data"
rm   -fr main/translit/src/main/resources/com/ibm/icu/impl/data/icudata
mkdir -p main/translit/src/main/resources/com/ibm/icu/impl/data/icudata/
mv       main/core/src/main/resources/com/ibm/icu/impl/data/icudata/translit     main/translit/src/main/resources/com/ibm/icu/impl/data/icudata/translit

echo "Unpacking icutzdata.jar"
# This unzips together with other existing core files
# So we don't remove the folder
unzip -q -d main/core/src/main/resources/ main/shared/data/icutzdata.jar
rm -fr      main/core/src/main/resources/META-INF

echo "Unpacking testdata.jar"
rm -fr      main/core/src/test/resources/com/ibm/icu/dev/data/testdata/
unzip -q -d main/core/src/test/resources/ main/shared/data/testdata.jar
rm -fr      main/core/src/test/resources/META-INF

echo "Removing jar files"
echo "  icudata.jar"
rm main/shared/data/icudata.jar
echo "  icutzdata.jar"
rm main/shared/data/icutzdata.jar
echo "  testdata.jar"
rm main/shared/data/testdata.jar

echo DONE
