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
#
# Note to maintainers: This script cannot be assumed to run on a Unix/Linux
# based system, and while a Posix compliant bash shell is required, any
# assumptions about auxiliary Unix tools should be minimized (e.g. things
# like "dirname" or "tempfile" may not exist). Where bash-only alternatives
# have to be used, they should be clearly documented.

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
  echo -- "----------------------------------------------------------------" >> "${LOG_FILE}"
  "${@}" >> "${LOG_FILE}" 2>&1
  if (( $? != 0 )) ; then
    echo -- "---- Previous command failed ----" >> "${LOG_FILE}"
    echo "Error running: ${@}"
    read -p "Show log file? " -n 1 -r
    echo
    if [[ "${REPLY}" =~ ^[Yy]$ ]] ; then
      less -X "${LOG_FILE}"
    fi
    echo "Log file: ${LOG_FILE}"
    exit 1
  fi
  echo -- "---- Previous command succeeded ----" >> "${LOG_FILE}"
}

# First require that we are run from the same directory as the script.
# Can't assume users have "dirname" available so hack it a bit with shell
# substitution (if no directory path was prepended, SCRIPT_DIR==$0).
SCRIPT_DIR=${0%/*}
if [[ "$SCRIPT_DIR" != "$0" ]] ; then
  cd $SCRIPT_DIR
fi

# Check for some expected environmental things early.
which ant > /dev/null || die "Cannot find Ant executable 'ant' in the current path."
which mvn > /dev/null || die "Cannot find Maven executable 'mvn' in the current path."

# Check there's one argument that points at a directory (or a symbolic link to a directory).
(( $# == 1 )) && [[ -d "$1" ]] || die "Usage: ./install-cldr-jars.sh <CLDR-root-directory>"

# Set up a log file (and be nice about tidying it up).
# Cannot assume "tempfile" exists so use a timestamp (we expect "date" to exist though).
LOG_FILE="${TMPDIR:-/tmp}/cldr2icu_log_$(date '+%m%d_%H%M%S').txt"
touch $LOG_FILE || die "Cannot create temporary file: ${LOG_FILE}"
echo -- "---- LOG FILE ---- $(date '+%F %T') ----" >> "${LOG_FILE}"

# Build the cldr-code.jar in the cldr-code/target subdirectory of the CLDR tools directory.
CLDR_TOOLS_DIR="$1/tools"
pushd "${CLDR_TOOLS_DIR}" > /dev/null || die "Cannot change directory to: ${CLDR_TOOLS_DIR}"

echo "Building CLDR JAR file..."
run_with_logging mvn package -DskipTests=true
[[ -f "cldr-code/target/cldr-code.jar" ]] || die "Error creating cldr-code.jar file"

popd > /dev/null

# The -B flag is "batch" mode and won't mess about with escape codes in the log file.
echo "Installing CLDR JAR file..."
run_with_logging mvn -B install:install-file \
  -Dproject.parent.relativePath="" \
  -DgroupId=org.unicode.cldr \
  -DartifactId=cldr-api \
  -Dversion=0.1-SNAPSHOT \
  -Dpackaging=jar \
  -DgeneratePom=true \
  -DlocalRepositoryPath=. \
  -Dfile="${CLDR_TOOLS_DIR}/cldr-code/target/cldr-code.jar"

echo "Syncing local Maven repository..."
run_with_logging mvn -B dependency:purge-local-repository \
  -Dproject.parent.relativePath="" \
  -DmanualIncludes=org.unicode.cldr:cldr-api:jar 

echo "All done!"
echo "Log file: ${LOG_FILE}"
