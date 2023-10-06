#!/usr/bin/env bash
# Copyright (C) 2023 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

export MAVEN_ARGS='--no-transfer-progress'

# Version update!
export artifact_version='74.1-SNAPSHOT'
export github_rel_version='74rc'
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
function copyDependencyArtifacts() {
  rm   -fr ${out_dir}/lib/
  mkdir -p ${out_dir}/lib/

  mvn dependency:copy -q -Dartifact=com.ibm.icu:core:${artifact_version}                   -DoutputDirectory=${out_dir}/lib/
  mvn dependency:copy -q -Dartifact=com.ibm.icu:collate:${artifact_version}                -DoutputDirectory=${out_dir}/lib/
  mvn dependency:copy -q -Dartifact=com.ibm.icu:icu4j-charset:${artifact_version}          -DoutputDirectory=${out_dir}/lib/
  mvn dependency:copy -q -Dartifact=com.ibm.icu:currdata:${artifact_version}               -DoutputDirectory=${out_dir}/lib/
  mvn dependency:copy -q -Dartifact=com.ibm.icu:langdata:${artifact_version}               -DoutputDirectory=${out_dir}/lib/
  mvn dependency:copy -q -Dartifact=com.ibm.icu:regiondata:${artifact_version}             -DoutputDirectory=${out_dir}/lib/
  mvn dependency:copy -q -Dartifact=com.ibm.icu:translit:${artifact_version}               -DoutputDirectory=${out_dir}/lib/
  mvn dependency:copy -q -Dartifact=com.ibm.icu:tools_build:${artifact_version}            -DoutputDirectory=${out_dir}/lib/
  mvn dependency:copy -q -Dartifact=com.ibm.icu:common_tests:${artifact_version}:jar:tests -DoutputDirectory=${out_dir}/lib/

  export toolcp="${out_dir}/lib/*"
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
