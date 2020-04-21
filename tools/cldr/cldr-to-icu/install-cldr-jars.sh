#!/bin/bash -u
#
#####################################################################
### © 2020 and later: Unicode, Inc. and others.                   ###
### License & terms of use: http://www.unicode.org/copyright.html ###
#####################################################################
#
# This script will attempt to build and install the necessary CLDR JAR files
# from a given CLDR installation root directory. The JAR files are installed
# according to the manual instructions given in README.txt and lib/README.txt.
#
# The user must have installed both 'ant' and 'maven' in accordance with the
# instructions in README.txt before attempting to run this script.
#
# Usage (from the directory of this script):
#
# ./install-cldr-jars.sh <CLDR-root-directory>

# Exit with a message for fatal errors.
function die() {
  echo "$1"
  echo "Exiting..."
  exit 1
} >&2

# Runs a given command and captures output to the global log file.
# If a command errors, the user can then view the log file.
function run_with_logging() {
  echo >> "${LOG_FILE}"
  echo "Running: ${@}" >> "${LOG_FILE}"
  echo "----------------------------------------------------------------" >> "${LOG_FILE}"
  "${@}" >> "${LOG_FILE}" 2>&1
  if (( $? != 0 )) ; then
    echo "---- Previous command failed ----" >> "${LOG_FILE}"
    echo "Error running: ${@}"
    read -p "Show log file? " -n 1 -r
    echo
    if [[ "${REPLY}" =~ ^[Yy]$ ]] ; then
      less -X "${LOG_FILE}"
    fi
    mv -f "${LOG_FILE}" "${ROOT_DIR}/last_log.txt"
    echo "Log file: ${ROOT_DIR}/last_log.txt"
    exit 1
  fi
  echo "---- Previous command succeeded ----" >> "${LOG_FILE}"
}

# First require that we are run from the same directory as the script.
ROOT_DIR="$(realpath $(dirname $0))"
if [[ "${ROOT_DIR}" != "$(realpath ${PWD})" ]] ; then
  echo "WARNING: Shell script should be run from the project root directory"
  echo "Current directory:"
  echo "  ${PWD}"
  echo "Project root direcory (where this script is):"
  echo "  ${ROOT_DIR}"
  read -p "Change to project root and continue? " -n 1 -r
  echo
  [[ "${REPLY}" =~ ^[Yy]$ ]] || die "Script must be run from the project root directory"
  cd "$ROOT_DIR"
fi

# Check for some expected environmental things early.
which ant > /dev/null || die "Cannot find Ant executable 'ant' in the current path."
which mvn > /dev/null || die "Cannot find Maven executable 'mvn' in the current path."
[[ -d "lib" ]] || die "Cannot find expected 'lib' directory in: $PWD"

# Check there's one argument that points at a directory (or a symbolic link to a directory).
(( $# == 1 )) && [[ -d "$1" ]] || die "Usage: ./install-cldr-jars.sh <CLDR-root-directory>"

# Set up a log file (and be nice about tidying it up).
LOG_FILE="$(tempfile)" || die "Cannot create temporary file!"
trap "rm -f -- '${LOG_FILE}'" EXIT
echo "---- LOG FILE ---- $(date '+%F %T') ----" >> "${LOG_FILE}"

# Build the cldr.jar in the CLDR tools directory.
CLDR_TOOLS_DIR="$1/tools/java"
pushd "${CLDR_TOOLS_DIR}" > /dev/null || die "Cannot change directory to: ${CLDR_TOOLS_DIR}"

echo "Building CLDR JAR file..."
run_with_logging ant -f ./build.xml clean jar
[[ -f "cldr.jar" ]] || die "Error creating cldr.jar file"
[[ -f "libs/utilities.jar" ]] || die "Cannot find libs/utilities.jar"

popd > /dev/null

# Install both required CLDR jars in the lib/ directory.
pushd "${ROOT_DIR}/lib" > /dev/null || die "Cannot change to lib directory"

# The -B flag is "batch" mode and won't mess about with escape codes in the log file.
echo "Installing CLDR JAR file..."
run_with_logging mvn -B install:install-file \
  -DgroupId=org.unicode.cldr \
  -DartifactId=cldr-api \
  -Dversion=0.1-SNAPSHOT \
  -Dpackaging=jar \
  -DgeneratePom=true \
  -DlocalRepositoryPath=. \
  -Dfile="${CLDR_TOOLS_DIR}/cldr.jar"

echo "Installing CLDR utilities JAR file..."
run_with_logging mvn -B install:install-file \
  -DgroupId=com.ibm.icu \
  -DartifactId=icu-utilities \
  -Dversion=0.1-SNAPSHOT \
  -Dpackaging=jar \
  -DgeneratePom=true \
  -DlocalRepositoryPath=. \
  -Dfile="${CLDR_TOOLS_DIR}/libs/utilities.jar"

popd > /dev/null

# We are back in the root directory now.
echo "Syncing local Maven repository..."
run_with_logging mvn -B dependency:purge-local-repository -DsnapshotsOnly=true

mv -f "${LOG_FILE}" "last_log.txt"
echo "All done! (log file: last_log.txt)"
trap - EXIT
