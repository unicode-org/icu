#!/usr/bin/env bash
# Copyright (C) 2023 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

# Unpack the pre-built .jar files with data only

ICU_DATA_VER=icudt74b

echo "Unpacking icudata.jar"
unzip -q -d main/core/src/main/resources/ main/shared/data/icudata.jar
rm -fr   main/core/src/main/resources/META-INF

echo "  Moving charset data"
mkdir -p main/charset/src/main/resources/com/ibm/icu/impl/data/${ICU_DATA_VER}
mv       main/core/src/main/resources/com/ibm/icu/impl/data/${ICU_DATA_VER}/*.cnv        main/charset/src/main/resources/com/ibm/icu/impl/data/${ICU_DATA_VER}/
mv       main/core/src/main/resources/com/ibm/icu/impl/data/${ICU_DATA_VER}/cnvalias.icu main/charset/src/main/resources/com/ibm/icu/impl/data/${ICU_DATA_VER}/

echo "  Moving currency data"
mkdir -p main/currdata/src/main/resources/com/ibm/icu/impl/data/${ICU_DATA_VER}/
mv       main/core/src/main/resources/com/ibm/icu/impl/data/${ICU_DATA_VER}/curr         main/currdata/src/main/resources/com/ibm/icu/impl/data/${ICU_DATA_VER}/curr

echo "  Moving collate data"
mkdir -p main/collate/src/main/resources/com/ibm/icu/impl/data/${ICU_DATA_VER}/
mv       main/core/src/main/resources/com/ibm/icu/impl/data/${ICU_DATA_VER}/coll         main/collate/src/main/resources/com/ibm/icu/impl/data/${ICU_DATA_VER}/coll

echo "  Moving langdata data"
mkdir -p main/langdata/src/main/resources/com/ibm/icu/impl/data/${ICU_DATA_VER}/
mv       main/core/src/main/resources/com/ibm/icu/impl/data/${ICU_DATA_VER}/lang         main/langdata/src/main/resources/com/ibm/icu/impl/data/${ICU_DATA_VER}/lang

echo "  Moving regiondata data"
mkdir -p main/regiondata/src/main/resources/com/ibm/icu/impl/data/${ICU_DATA_VER}/
mv       main/core/src/main/resources/com/ibm/icu/impl/data/${ICU_DATA_VER}/region       main/regiondata/src/main/resources/com/ibm/icu/impl/data/${ICU_DATA_VER}/region

echo "  Moving translit data"
mkdir -p main/translit/src/main/resources/com/ibm/icu/impl/data/${ICU_DATA_VER}/
mv       main/core/src/main/resources/com/ibm/icu/impl/data/${ICU_DATA_VER}/translit     main/translit/src/main/resources/com/ibm/icu/impl/data/${ICU_DATA_VER}/translit

echo "Unpacking icutzudata.jar"
unzip -q -d main/core/src/main/resources/ main/shared/data/icutzdata.jar
rm -fr   main/core/src/main/resources/META-INF

echo "Unpacking testdata.jar"
unzip -q -d main/core/src/test/resources/ main/shared/data/testdata.jar
rm -fr   main/core/src/test/resources/META-INF

echo DONE
