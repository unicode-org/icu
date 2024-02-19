#!/usr/bin/env bash
# Copyright (C) 2023 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

if [ ! -f "releases_tools/shared.sh" ]; then
  echo "ERROR: This script should be executed while being in the icu4j folder"
  exit
fi
. releases_tools/shared.sh

# ====================================================================================
# The start of the script proper

reportTitle "Checking the JDK version (must be 8)"
checkThatJdk8IsDefault

# ====================================================================================

reportTitle gatherapi :: Collect API information

mvn clean -q --batch-mode
# Build everything
mvn install -q --batch-mode -DskipITs -DskipTests
# Gather API info
mvn site -q  --batch-mode -DskipITs -DskipTests -P gatherapi > /dev/null

checkFileCreated "${out_dir}/icu4j${api_report_version}.api3.gz"

# Prepare classpath folder to run the tools
copyDependencyArtifacts

# ====================================================================================

reportTitle apireport :: Run API report generator tool

java -cp "$toolcp" \
    com.ibm.icu.dev.tool.docs.ReportAPI \
    -old: tools/build/icu4j${api_report_prev_version}.api3.gz \
    -new: ${out_dir}/icu4j${api_report_version}.api3.gz \
    -html \
    -out: ${out_dir}/icu4j_compare_${api_report_prev_version}_${api_report_version}.html

checkFileCreated "${out_dir}/icu4j_compare_${api_report_prev_version}_${api_report_version}.html"

# ====================================================================================

reportTitle checkDeprecated :: Check consistency between javadoc @deprecated and @Deprecated annotation

java -cp "$toolcp" \
    com.ibm.icu.dev.tool.docs.DeprecatedAPIChecker \
        ${out_dir}/icu4j${api_report_version}.api3.gz

# ====================================================================================

reportTitle checkAPIStatusConsistency :: Check consistency between API class status and methods overriding java.lang.Object

# If you need classes excluded from this check, define following property in build-local.properties.
# e.g. checkAPIStatusConsistency.skip.classes=com.ibm.icu.text.Normalizer;com.ibm.icu.util.ULocale

java -cp "$toolcp" \
    com.ibm.icu.dev.tool.docs.APIStatusConsistencyChecker \
        ${out_dir}/icu4j${api_report_version}.api3.gz \
        checkAPIStatusConsistency.skip.classes=

# ====================================================================================

reportTitle draftAPIs :: Run API collector tool and generate draft API report in html

java -cp "$toolcp" \
    com.ibm.icu.dev.tool.docs.CollectAPI \
        -f Draft \
        -o ${out_dir}/draftAPIs.html \
        ${out_dir}/icu4j${api_report_version}.api3.gz

checkFileCreated "${out_dir}/draftAPIs.html"

# ====================================================================================

reportTitle draftAPIsTSV :: Run API collector tool and generate draft API report in TSV

java -cp "$toolcp" \
    com.ibm.icu.dev.tool.docs.CollectAPI \
        -f Draft \
        -o ${out_dir}/draftAPIs.tsv \
        -t ${out_dir}/icu4j${api_report_version}.api3.gz

checkFileCreated "${out_dir}/draftAPIs.tsv"
