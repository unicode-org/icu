#!/usr/bin/env bash
# Copyright (C) 2023 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

if [ ! -f "releases_tools/shared.sh" ]; then
  echo "ERROR: This script should be executed while being in the icu4j folder"
  exit
fi
. releases_tools/shared.sh

function copyArtifactForGithubRelease() {
  local artifactId=$1
  # Copy artifacts in the output folder
  mvn dependency:copy -q -Dmdep.stripVersion=true -Dartifact=com.ibm.icu:${artifactId}:${artifact_version} -DoutputDirectory=${release_folder}
  mvn dependency:copy -q -Dmdep.stripVersion=true -Dartifact=com.ibm.icu:${artifactId}:${artifact_version}:jar:sources -DoutputDirectory=${release_folder}
  # Change the names
  mv ${release_folder}/${artifactId}.jar ${release_folder}/${artifactId}-${github_rel_version}.jar
  mv ${release_folder}/${artifactId}-sources.jar ${release_folder}/${artifactId}-${github_rel_version}-sources.jar
}

# ====================================================================================
# The start of the script proper

release_folder=${out_dir}/github_release
# We still need JDK 8 to generate the javadoc (because of the doclets)
checkThatJdk8IsDefault

# ====================================================================================
# Build artifacts and copy them in the output folder

reportTitle Prepare folder with artifacts for GitHub release

mvn clean install -DskipITs -DskipTests -P with_sources

rm   -fr ${release_folder}
mkdir -p ${release_folder}
copyArtifactForGithubRelease icu4j
copyArtifactForGithubRelease icu4j-charset
copyArtifactForGithubRelease icu4j-localespi

# ====================================================================================
# Build complete javadoc and copy it in the output folder

reportTitle Prepare complete javadoc for GitHub release

mvn site -DskipITs -DskipTests -P with_full_javadoc

jar -Mcf ${release_folder}/icu4j-${github_rel_version}-javadoc.jar  -C ${out_dir}/site/apidocs/ .

# ====================================================================================

pushd ${release_folder}
md5sum *.jar > icu4j-${github_rel_version}.md5
popd

reportTitle "You can find the results in ${release_folder}/"
