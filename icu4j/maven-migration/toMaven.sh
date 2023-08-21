#!/usr/bin/env bash
# Copyright (C) 2023 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

# Move the sources from main/classes and main/tests into a maven structure
# - `main/classes/foo/src/java` go to `foo/src/main`, then split into `java` and `resources`
# - `main/tests/foo/src/java` go to `foo/src/test`, then split into `java` and `resources`
# - `main/tests/foo/src/META-INF` goes to ... `resources`
#
# From this:
# classes/
#   <module>/
#     src/
# test/
#   <module>/
#     src/
#
# To this (standard maven structure):
# <module>/
#   main/
#     src/
#       java/
#       resources/
#   test/
#     src/
#       java/
#       resources/
#
# Then we cleanup older Eclipse project & launcher files, ant scripts, manifest.stub

function rmDirIfExists() {
  [ -d "$1" ] && rm -fr "$1"
}

function rmFileIfExists() {
  [ -f "$1" ] && rm "$1"
}

function safeMoveDir() {
  export FOLDER_NAME=$1
  export FROM_FOLDER=$2
  export TO_FOLDER=$3

  if [ ! -d "$FROM_FOLDER/$FOLDER_NAME" ]; then
    echo "  No '$FROM_FOLDER/$FOLDER_NAME' to move."
    return
  fi
  if [ -e "$TO_FOLDER/$FOLDER_NAME" ]; then
    echo "  Error: folder '$TO_FOLDER/$FOLDER_NAME' already exists!"
    exit
  else
    mkdir -p $TO_FOLDER
    mv $FROM_FOLDER/$FOLDER_NAME $TO_FOLDER/
    echo "  Moving '$FOLDER_NAME' folder from '$FROM_FOLDER' to '$TO_FOLDER'"
  fi
}

# Split the content of the `java` foldert into `java` and `resources`, the way maven wants it
function splitJavaToResources() {
  # Should point to the folder containing `java`, either main/ or test/
  export BASE_FOLDER=$1

  echo "  Splitting '$BASE_FOLDER/java/' into '$BASE_FOLDER/java/' and '$BASE_FOLDER/resources/'"
  # copy `java` to `resources`
  cp -R $BASE_FOLDER/java/ $BASE_FOLDER/resources/
  # delete all not `.java` from `java`
  find $BASE_FOLDER/java/      -type f -not -name *.java -exec rm {} \;
  # delete all `.java` from `resources`
  find $BASE_FOLDER/resources/ -type f      -name *.java -exec rm {} \;
}

function removeEclipseProjectFiles() {
  # Should point to the old folder (to be moved), containing the eclipse / ant files
  export BASE_FOLDER=$1

  # Cleanup Eclipse project & launcher files, ant projects, other stuff
  # Eclipse
  rmDirIfExists  "$BASE_FOLDER/.externalToolBuilders/"
  rmDirIfExists  "$BASE_FOLDER/.settings/"
  rmFileIfExists "$BASE_FOLDER/.project"
  rmFileIfExists "$BASE_FOLDER/.classpath"
  # Ant
  rmFileIfExists "$BASE_FOLDER/build.properties"
  rmFileIfExists "$BASE_FOLDER/build.xml"
  rmFileIfExists "$BASE_FOLDER/manifest.stub"
  rmFileIfExists "$BASE_FOLDER/findbugs-exclude.xml"
  find $BASE_FOLDER/ -type f -name '*.launch' -exec rm {} \;
}

# Takes a folder as parameter and removes the all the empty sub-folders.
function removeEmptyFolders() {
  export BASE_FOLDER=$1
  # `find -type d .` finds all subfolders, empty or not.
  # We can't just force delete (-r), as that would also delete non-empty folders.
  # And the find iteration is not children first, but parent first.
  # If I call this with `main/classes/` then I need a loop:
  # loop 1:
  #   rm `main/classes/` => FAILS, not empty
  #   rm `main/classes/core/` => FAILS, not empty
  #   rm `main/classes/core/src/` => OK
  # loop 2:
  #   rm `main/classes/` => FAILS, not empty
  #   rm `main/classes/core/` => OK
  # loop 3:
  #   rm `main/classes/` => OK
  # If there is any file left in some folder, that (and the parent folders) are not deleted.
  # That's why we loop here (although 15 might be a bit much :-)
  for ((n = 0; n < 15; n++)) do
    find $BASE_FOLDER -type d -exec rm -d {} \; 2> /dev/null
  done
}

function moveMainModuleToMaven() {
  # 1. $1: component name (core, charset, etc)
  export MODULE_NAME=$1
  # 2. folder name in the pre-maven structure (`classes` or `tests`)
  export SRC_TYPE=$2
  # 3. folder name in the maven standard structure (`main` or `test`)
  export TRG_TYPE=$3

  if [ ! -d main/$SRC_TYPE/$MODULE_NAME ]; then
    echo "  Module '$MODULE_NAME' does not have '$SRC_TYPE' to move to '$TRG_TYPE'"
    return
  fi

  safeMoveDir com      main/$SRC_TYPE/$MODULE_NAME/src main/$MODULE_NAME/src/$TRG_TYPE/java
  splitJavaToResources main/$MODULE_NAME/src/$TRG_TYPE
  safeMoveDir META-INF main/$SRC_TYPE/$MODULE_NAME/src main/$MODULE_NAME/src/$TRG_TYPE/resources

  removeEclipseProjectFiles main/$SRC_TYPE/$MODULE_NAME

  # Remove the original (pre-maven) folders that were left empty after we moved stuff out
  # For example if all we had was source code + Eclipse files the folder will be left empty.
  # e.g. main/classes/collate
  echo "  Remove all empty pre-maven folders from 'main/$SRC_TYPE/$MODULE_NAME'"
  removeEmptyFolders main/$SRC_TYPE/$MODULE_NAME

  # Remove folders that didn't receive any content in the new structure.
  # For example when we copy the `java` folder to `resources`, then remove
  # all the *.java files, we might end up with empty `resources` folder
  # if there was no data file mixed-in with the code to begin with.
  # e.g. main/collate/src/main
  echo "  Remove all empty post-maven folders from 'main/$MODULE_NAME/src/$TRG_TYPE'"
  removeEmptyFolders main/$MODULE_NAME/src/$TRG_TYPE
}

function mainModuleToMaven() {
  echo "Migrating $1 to maven"
  moveMainModuleToMaven $1 classes main
  moveMainModuleToMaven $1 tests test
  mkdir -p main/$MODULE_NAME/src/main/resources/
  ln -s ../../../../../../LICENSE main/$MODULE_NAME/src/main/resources/LICENSE
}

function simpleModuleToMaven() {
  # 1. $1: component name (core, charset, etc)
  export MODULE_NAME=$1
  export LICENSE_PATH=$2
  echo "Migrating $MODULE_NAME to maven"

  safeMoveDir com $MODULE_NAME/src $MODULE_NAME/src/main/java
  splitJavaToResources $MODULE_NAME/src/main

  removeEclipseProjectFiles $MODULE_NAME

  mkdir -p $MODULE_NAME/src/main/resources/
  ln -s $LICENSE_PATH $MODULE_NAME/src/main/resources/LICENSE

  echo "  Remove empty folders from '$MODULE_NAME'"
  removeEmptyFolders $MODULE_NAME
}

function moveCoreTestFileToCommon() {
  export FOLDER_NAME=$1
  export FILE_NAME=$2
  mkdir -p $COMMON_TEST_FOLDER/$FOLDER_NAME
  mv       $CORE_TEST_FOLDER/$FOLDER_NAME/$FILE_NAME $COMMON_TEST_FOLDER/$FOLDER_NAME/
}

function moveCircDepTestOutOfCore() {
  export CORE_TEST_FOLDER=main/core/src/test/java/com/ibm/icu/dev/test
  export COMMON_TEST_FOLDER=main/common_tests/src/test/java/com/ibm/icu/dev/test

  mkdir -p $CORE_TEST_FOLDER

  moveCoreTestFileToCommon calendar DataDrivenCalendarTest.java
  moveCoreTestFileToCommon format CompactDecimalFormatTest.java
  moveCoreTestFileToCommon format DataDrivenFormatTest.java
  moveCoreTestFileToCommon format DateFormatTest.java
  moveCoreTestFileToCommon format MeasureUnitTest.java
  moveCoreTestFileToCommon format NumberFormatDataDrivenTest.java
  moveCoreTestFileToCommon format NumberFormatRegressionTest.java
  moveCoreTestFileToCommon format NumberFormatSpecificationTest.java
  moveCoreTestFileToCommon format NumberFormatTest.java
  moveCoreTestFileToCommon format NumberRegressionTests.java
  moveCoreTestFileToCommon format PluralRangesTest.java
  moveCoreTestFileToCommon format PluralRulesTest.java
  moveCoreTestFileToCommon format RbnfTest.java
  moveCoreTestFileToCommon format TestMessageFormat.java
  moveCoreTestFileToCommon format TimeZoneFormatTest.java
  moveCoreTestFileToCommon message2 Mf2FeaturesTest.java
  moveCoreTestFileToCommon normalizer BasicTest.java
  moveCoreTestFileToCommon number ModifierTest.java
  moveCoreTestFileToCommon number NumberFormatterApiTest.java
  moveCoreTestFileToCommon number NumberParserTest.java
  moveCoreTestFileToCommon number NumberPermutationTest.java
  moveCoreTestFileToCommon number NumberRangeFormatterTest.java
  moveCoreTestFileToCommon number PatternStringTest.java
  moveCoreTestFileToCommon number PropertiesTest.java
  moveCoreTestFileToCommon rbbi LSTMBreakEngineTest.java
  moveCoreTestFileToCommon serializable CalendarHandler.java
  moveCoreTestFileToCommon serializable CompatibilityTest.java
  moveCoreTestFileToCommon serializable CoverageTest.java
  moveCoreTestFileToCommon serializable ExceptionHandler.java
  moveCoreTestFileToCommon serializable FormatHandler.java
  moveCoreTestFileToCommon serializable SerializableChecker.java
  moveCoreTestFileToCommon serializable SerializableTestUtility.java
  moveCoreTestFileToCommon serializable SerializableWriter.java
  moveCoreTestFileToCommon stringprep TestIDNARef.java
  moveCoreTestFileToCommon stringprep TestStringPrep.java
  moveCoreTestFileToCommon util CurrencyTest.java
  moveCoreTestFileToCommon util ICUResourceBundleTest.java
  moveCoreTestFileToCommon util ICUServiceTest.java
  moveCoreTestFileToCommon util LocaleDataTest.java
  moveCoreTestFileToCommon util ULocaleTest.java

  # Looks like the packaging project was already some kind of test for how things come together.
  # Should we move all the files in this project instead of common_tests?
  mv main/tests/packaging/src/com/ibm/icu/dev/test/* $COMMON_TEST_FOLDER/
  removeEclipseProjectFiles main/tests/packaging

  # At this point this folder should be empty
  # So remove if empty (-d) should work
  rm -d main/core/src/test/java/com/ibm/icu/dev/test/serializable
}

# ===============================================================
# Here starts the real script execution

if [ -f "main/core/pom.xml" ]; then
  echo "ERROR: looks like the structure was already migrated to maven?"
  exit
fi
if [ ! -f "main/classes/core/build.xml" ]; then
  echo "ERROR: the current folder when running this script should be <icu_root>/icu4j"
  echo "It is currently $PWD."
  exit
fi

MVN_MIG_DIR="$(dirname "${BASH_SOURCE[0]}")"

cp -R ${MVN_MIG_DIR}/* .
# Don't copy files that are only used for migration
rm README_MAVEN.md
rm toMaven.sh
rm unpack_jars.sh

# Migrate the modules in icu4j/main, which have code (in main/classes) & unit tests (in main/test)
echo "===================================="
echo "==== Migrating the main modules ===="
echo "===================================="
mainModuleToMaven core
mainModuleToMaven langdata
mainModuleToMaven charset
mainModuleToMaven collate
mainModuleToMaven localespi
mainModuleToMaven translit
# main only
mainModuleToMaven currdata
mainModuleToMaven regiondata
# test only
mainModuleToMaven framework

# Migrate the modules in icu4j, which have only code and no unit tests
echo "===================================="
echo "==== Migrating the root modules ===="
echo "===================================="
simpleModuleToMaven demos        ../../../../../LICENSE
simpleModuleToMaven samples      ../../../../../LICENSE
simpleModuleToMaven tools/build  ../../../../../../LICENSE
simpleModuleToMaven tools/misc   ../../../../../../LICENSE

echo "================================================================================="
echo "==== Moving core unit tests that depend on non-core (circular dependencies) ====="
echo "================================================================================="

moveCircDepTestOutOfCore

# Some final cleanup for any empty folders
removeEmptyFolders main/classes
removeEmptyFolders main/tests

# Unpack the CLDR data from the shared .jar files
${MVN_MIG_DIR}/unpack_jars.sh

echo DONE
