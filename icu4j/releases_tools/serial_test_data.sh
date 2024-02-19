#!/usr/bin/env bash
# Copyright (C) 2023 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html


if [ ! -f "releases_tools/shared.sh" ]; then
  echo "ERROR: This script should be executed while being in the icu4j folder"
  exit
fi
. releases_tools/shared.sh

export serial_test_data_dir=${out_dir}/serialTestData

# ====================================================================================
# The start of the script proper

# Build everything
mvn clean install -DskipITs -DskipTests

# Prepare classpath folder to run the tools
copyDependencyArtifacts

reportTitle serialTestData :: generate the serialization compatibility test data files

rm   -fr ${serial_test_data_dir}
mkdir -p ${serial_test_data_dir}
java -cp "$toolcp" com.ibm.icu.dev.test.serializable.SerializableWriter ${serial_test_data_dir}

echo "Note: The serialization compatibility test data files were"
echo "created in ${serial_test_data_dir}. Once you confirm"
echo "the test runs clean, you should copy the data file directory to"
echo "main/core/src/test/resources/com/ibm/icu/dev/test/serializable/data"
