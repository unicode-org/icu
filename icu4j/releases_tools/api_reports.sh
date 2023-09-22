#!/usr/bin/env bash
# Copyright (C) 2023 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

export MAVEN_ARGS='--no-transfer-progress'

# Version update!
export artifact_version='74.0.1-SNAPSHOT'
export api_report_version='74'
export api_report_prev_version='73'
export out_dir=target


function checkThatJdk8IsDefault() {
  javac -version appPath 2>&1 | grep -E 'javac 1\.8\.' > /dev/null
  if [ $? -eq 0 ]; then
    echo "The default JDK is JDK 8, all good!"
    javac -version
  else
    echo "This step can only be executed with JDK 8!"
    echo "Make sure that you have the PATH pointing to a JDK 8!"
    javac -version
    exit
  fi

}

# Copy the icu artifacts from the local maven repo to the lib folder,
# so that we can use it as classpath.
function copyArtifacts() {
  rm   -fr ${out_dir}/lib/
  mkdir -p ${out_dir}/lib/

  mvn dependency:copy -q -Dartifact=com.ibm.icu:tools_build:${artifact_version}   -DoutputDirectory=${out_dir}/lib/
  mvn dependency:copy -q -Dartifact=com.ibm.icu:core:${artifact_version}          -DoutputDirectory=${out_dir}/lib/
  mvn dependency:copy -q -Dartifact=com.ibm.icu:collate:${artifact_version}       -DoutputDirectory=${out_dir}/lib/
  mvn dependency:copy -q -Dartifact=com.ibm.icu:icu4j-charset:${artifact_version} -DoutputDirectory=${out_dir}/lib/
  mvn dependency:copy -q -Dartifact=com.ibm.icu:currdata:${artifact_version}      -DoutputDirectory=${out_dir}/lib/
  mvn dependency:copy -q -Dartifact=com.ibm.icu:langdata:${artifact_version}      -DoutputDirectory=${out_dir}/lib/
  mvn dependency:copy -q -Dartifact=com.ibm.icu:regiondata:${artifact_version}    -DoutputDirectory=${out_dir}/lib/
  mvn dependency:copy -q -Dartifact=com.ibm.icu:translit:${artifact_version}      -DoutputDirectory=${out_dir}/lib/
}

function checkFileCreated() {
  local OUT_FILE=$1
  if [ -f "$OUT_FILE" ]; then
    echo "    Output file $OUT_FILE generated"
  else
    echo "    Error generating output file $OUT_FILE"
    exit
  fi
}

function reportTitle() {
  echo ""
  echo "=============================================="
  echo $*
  echo "=============================================="
  echo ""
}

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
copyArtifacts
export toolcp="${out_dir}/lib/*"

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

