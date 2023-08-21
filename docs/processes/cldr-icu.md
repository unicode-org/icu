---
layout: default
title: CLDR-ICU integration (including ICU4C data to ICU4J)
parent: Release & Milestone Tasks
grand_parent: Contributors
nav_order: 115
---

<!--
© 2016 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
© 2010-2014: International Business Machines Corporation and others.
All Rights Reserved.
-->

# CLDR-ICU integration (including ICU4C data to ICU4J)
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

# Intro and setup

These instructions describe how to regenerate ICU4C locale and linguistic data from CLDR,
and then how to convert that ICU4 data for ICU4J (data jars and maven resources).
They apply to CLDR 44 / ICU 74 and later.

To use these instructions just for generating ICU4J data from ICU4C, you only need to use
steps 1, 8, and 12 in the Process section.

The full process requires local copies of

* CLDR (the source of most of the data, and some Java tools)
* The complete ICU source tree, including:
  * tools: includes the LdmlConverter build tool and associated config files
  * icu4c: the target for converted CLDR data, and source for ICU4J data; includes tests for the converted data 
  * icu4j: the target for updated data jars; includes tests for the converted data

For an official CLDR data integration into ICU, these should be clean, freshly
checked-out. For released CLDR sources, an alternative to checking out sources
for a given version is downloading the zipped sources for the common (core.zip)
and tools (tools.zip) directory subtrees from the Data column in
[CLDR Releases/Downloads](https://cldr.unicode.org/index/downloads)

Besides a standard JDK, the process also requires [ant](https://ant.apache.org) and
[maven](https://maven.apache.org) plus the xml-apis.jar from the
[Apache xalan package](https://xalan.apache.org/xalan-j/downloads.html) _(Is this
latter requirement still true?)_. You will also need to have performed the
[CLDR Maven setup](http://cldr.unicode.org/development/maven) (non-Eclipse version).

Notes:

* Enough things can (and will) fail in this process that it is best to
  run the commands separately from an interactive shell. They should all
  copy and paste without problems.
* It is often useful to save logs of the output of many of the steps in this
  process. The commands below save them to a NOTES directory.
* If you are adding or removing locales, or specific kinds of locale data,
  there are some xml files in the ICU sources that need to be updated (these xml
  files are used in addition to the CLDR files as inputs to the CLDR data build
  process for ICU):
  * The primary file to edit for adding/removing locales and/or collation and
    rbnf data is<br>
    `$TOOLS_ROOT/cldr/cldr-to-icu/build-icu-data.xml`.
  * There are some files in `icu4c/source/data/xml/` that may need editing for
    certain additions. This is especially true for brkitr additions; however there
    are rbnf files there that add some rules. The collation files there mainly
    hook up the UCA collation rules in `icu4c/data/unidata/UCARules.txt` to the
    collation data. To process these files, certain CLDR dtds are copied over to
    ICU.

For an official CLDR data integration into ICU, there are some additional
considerations:

* Don't commit anything in ICU sources (and possibly any changes in CLDR
  sources, depending on their nature) until you have finished testing and
  resolving build issues and test failures for both ICU4C and ICU4J.
* After everything is committed, you will need to tag the cldr and cldr-staging
  sources that ended up being used for the integration (see process below).

# Environment variables

There are several environment variables that need to be defined.

1. Java- and ant-related variables

   * `JAVA_HOME`: Path to JDK (a directory, containing e.g. `bin/java`, `bin/javac`,
     etc.); on many systems this can be set using the output of `/usr/libexec/java_home`.

   * `ANT_OPTS`: You may want to set `-Xmx8192m` to give Java more memory; otherwise
     it may run out of heap.

2. CLDR-related variables

   * `CLDR_DIR`: This is the path to the to root of standard CLDR sources, below
     which are the common and tools directories.

   * `CLDR_TMP_DIR`: Parent of temporary CLDR production data. Defaults to
     `$CLDR_DIR/../cldr-aux` (sibling to `CLDR_DIR`).
     
     > **NOTE:** As of CLDR 36 and 37, the GenerateProductionData tool no longer
       generates data by default into `$CLDR_TMP_DIR/production`; instead it
       generates data into `$CLDR_DIR/../cldr-staging/production` (though there is
       a command-line option to override this). However the rest of the build still
       assumes that the generated data is in `$CLDR_TMP_DIR/production`.
       So `CLDR_TMP_DIR` must be defined to be `CLDR_DIR/../cldr-staging`.
       
3. ICU-related variables

   * `ICU4C_DIR`: Path to root of ICU4C sources, below which is the source dir.

   * `ICU4J_ROOT`: Path to root of ICU4J sources, below which is the main dir.

   * `TOOLS_ROOT`: Path to root of ICU tools directory, below which are (e.g.) the
     cldr and unicodetools dirs.
 
# Process
 
## 1 Environment variables

1a. Java and ant variables, adjust for your system
```
export JAVA_HOME=`/usr/libexec/java_home`
export ANT_OPTS="-Xmx8192m"
```

1b. CLDR variables, adjust for your setup; with cygwin it might be e.g.
```
CLDR_DIR=`cygpath -wp /build/cldr`
```

Note that for cldr-staging we do not use personal forks, we commit directly.
```
export CLDR_DIR=$HOME/cldr-myfork
export CLDR_TMP_DIR=$HOME/cldr-staging
export CLDR_DATA_DIR=$HOME/cldr-staging/production
```

1c. ICU variables
```
export ICU4C_DIR=$HOME/icu-myfork/icu4c
export ICU4J_ROOT=$HOME/icu-myfork/icu4j
export TOOLS_ROOT=$HOME/icu-myfork/tools
```

1d. Directory for logs/notes (create if does not exist)
```
export NOTES=...(some directory)...
mkdir -p $NOTES
```

## 2 Initial builds of ICU4C and ICU4J

2a. Configure ICU4C, build and test without new data first, to verify that
there are no pre-existing errors, and to build some tools needed for later
steps. Here `<platform>` is the runConfigureICU code for the platform you
are building on, e.g. Linux, MacOSX, Cygwin.
(optionally build with debug enabled)
```
cd $ICU4C_DIR/source
./runConfigureICU [--enable-debug] <platform>
make clean
make check 2>&1 | tee $NOTES/icu4c-oldData-makeCheck.txt
```

2b. Now with ICU4J, build and test without new data first, to verify that
there are no pre-existing errors (or at least to have the pre-existing errors
as a base for comparison):
```
cd $ICU4J_ROOT
ant clean
ant check 2>&1 | tee $NOTES/icu4j-oldData-antCheck.txt
```

2c. Additionally for ICU4J, repeat the same as 2b, but for building with
Maven instead of with Ant.
```
cd $ICU4J_ROOT/maven-build
mvn clean
mvn verify 2>&1 | tee $NOTES/icu4j-oldData-mvnVerify.txt
```

## 3 Make pre-adjustments

3a. Copy latest relevant CLDR dtds to ICU
```
cp -p $CLDR_DIR/common/dtd/ldml.dtd $ICU4C_DIR/source/data/dtd/cldr/common/dtd/
cp -p $CLDR_DIR/common/dtd/ldmlICU.dtd $ICU4C_DIR/source/data/dtd/cldr/common/dtd/
```

3b. Update the cldr-icu tooling to use the latest tagged version of ICU
```
open $TOOLS_ROOT/cldr/cldr-to-icu/pom.xml
```
(search for `icu4j-for-cldr` and update to the latest tagged version per instructions)

3c. Update the build for any new icu version, added locales, etc.
```
open $TOOLS_ROOT/cldr/cldr-to-icu/build-icu-data.xml
```
(update icuVersion, icuDataVersion if necessary; update lists of locales to include if necessary)

3d. If there are new data types or variants in CLDR, you may need to update the
files that specify mapping of CLDR data to ICU rseources:
```
open $TOOLS_ROOT/cldr/cldr-to-icu/src/main/resources/ldml2icu_locale.txt
open $TOOLS_ROOT/cldr/cldr-to-icu/src/main/resources/ldml2icu_supplemental.txt
```

## 4 Build and install CLDR jar

See `$TOOLS_ROOT/cldr/lib/README.txt` for more information on the CLDR
jar and the `install-cldr-jars.sh` script.
```
cd $TOOLS_ROOT/cldr
ant install-cldr-libs
```

## 5 Generate CLDR production data and convert for ICU

5a. Generate the CLDR production data.

This process uses ant with ICU4C's `data/build.xml`

* Running `ant cleanprod` is necessary to clean out the production data directory
  (usually `$CLDR_TMP_DIR/production`), required if any CLDR data has changed.
* Running `ant setup` is not required, but it will print useful errors to
  debug issues with your path when it fails.
```
cd $ICU4C_DIR/source/data
ant cleanprod
ant setup
ant proddata 2>&1 | tee $NOTES/cldr-newData-proddataLog.txt
```

>  Note, for CLDR development, at this point tests are sometimes run on the
   production data, see
   [BRS: Run tests on production data](https://cldr.unicode.org/development/cldr-big-red-switch/brs-run-tests-on-production-data)

5b. Build the new ICU4C data files.

These include .txt files and .py files. These new files will replace whatever was
already present in the ICU4C sources. This process uses the `LdmlConverter` in
`$TOOLS_ROOT/cldr/cldr-to-icu/`; see `$TOOLS_ROOT/cldr/cldr-to-icu/README.txt`.

* This process will take several minutes, during most of which there will be no log
  output (so do not assume nothing is happening). Keep a log so you can investigate
  anything that looks suspicious.
* Note that `ant clean` should _not_ be run before this. The `build-icu-data.xml` process
  will automatically run its own "clean" step to delete files it cannot determine to
  be ones that it would generate, except for pasts listed in `<retain>` elements such as
  `coll/de__PHONEBOOK.txt`, `coll/de_.txt`, etc.
* Before running ant to regenerate the data, make any necessary changes to the
  build-icu-data.xml file, such as adding new locales etc.
```
cd $TOOLS_ROOT/cldr/cldr-to-icu
ant -f build-icu-data.xml -DcldrDataDir="$CLDR_TMP_DIR/production" | tee $NOTES/cldr-newData-builddataLog.txt
```

5c. Update the CLDR testData files needed by ICU4C/J tests, ensuring
they are representative of the newest CLDR data.
```
cd $TOOLS_ROOT/cldr
ant copy-cldr-testdata
```

5d. Copy localeCanonicalization.txt from CLDR testData and add a source reference line:
```
cp -p $CLDR_DIR/common/testData/localeIdentifiers/localeCanonicalization.txt $ICU4C_DIR/source/test/testdata/
cp -p $CLDR_DIR/common/testData/localeIdentifiers/localeCanonicalization.txt $ICU4J_ROOT/main/tests/core/src/com/ibm/icu/dev/data/unicode/
open $ICU4C_DIR/source/test/testdata/localeCanonicalization.txt
open $ICU4J_ROOT/main/tests/core/src/com/ibm/icu/dev/data/unicode/localeCanonicalization.txt
```
At the beginning of each file add the following line:\
```
# File copied from cldr common/testData/localeIdentifiers/localeCanonicalization.txt
```

5e. For now, manually re-add the `lstm` entries in `data/brkitr/root.txt`
```
open $ICU4C_DIR/source/data/brkitr/root.txt 
```
Paste the following block after the dictionaries block and before the final closing '}':
```
    lstm{
        Thai{"Thai_graphclust_model4_heavy.res"}
        Mymr{"Burmese_graphclust_model5_heavy.res"}
    }
```

## 6 Check the results

Check which data files have modifications, which have been added or removed
(if there are no changes, you may not need to proceed further). Make sure the
list seems reasonable. You may want to save logs, and possibly examine them...
```
cd $ICU4C_DIR/..
git status
git status >  $NOTES/gitStatusDelta-data.txt
git diff >  $NOTES/gitDiffDelta-data.txt
open $NOTES/gitDiffDelta-data.txt
```

6a. You may also want to check which files were modified in CLDR production data:
```
cd $CLDR_TMP_DIR
git status
git status >  $NOTES/gitStatusDelta-staging.txt
git diff >  $NOTES/gitDiffDelta-staging.txt
```

## 7 Fix data generation errors

Look for evident errors in the list of file changes, or in the file diffs.
Fixing them may entail modifying CLDR source data or `TOOLS_ROOT` config files or
tooling.

## 8 Rebuild ICU4C with new data, run tests

8a. Re-run configure and make clean, necessary to handle any files added or deleted:
```
cd $ICU4C_DIR/source
./runConfigureICU [--enable-debug] <platform>
make clean
```

8b. Do the rebuild, keeping a log as before:
```
make check 2>&1 | tee $NOTES/icu4c-newData-makeCheck.txt
```

To re-run a specific test if necessary when fixing bugs; for example:
```
cd test/intltest
DYLD_LIBRARY_PATH=../../lib:../../stubdata:../../tools/ctestfw:$DYLD_LIBRARY_PATH ./intltest -e -G format/NumberTest/NumberPermutationTest
cd ../..

cd build/test/cintltst
DYLD_LIBRARY_PATH=../../lib:../../stubdata:../../tools/ctestfw:$DYLD_LIBRARY_PATH ./cintltst /tsformat/ccaltst
cd ../../..
```

## 9 Investigate and fix make check test case failures

The first run processing new CLDR data from the Survey Tool can result in thousands
of failures (in many cases, one CLDR data fix can resolve hundreds of test failures).

If the error is caused by bad CLDR data, then file a CLDR bug (or use the existing BRS
ticket under which you are performing the integration, if you have one), fix the data,
and regenerate from step 4.

If the data is OK , other sources of failure can include:
* Problems with the CLDR-ICU conversion process (pehaps some locale data is not getting
  converted properly; go back to step 3, adjust and repeat from there.
* Problems with ICU library code that may not be using new resources properly. Fix and
  repeat from step 8.
* Problems in which ICU test cases need to be updated to match CLDR changes. Fix and
  repeat from step 8. Some special cases of this include:
  * If there are new resource types or new attribute values for existing resource types,
    you will need to update `icu4c/test/testdata/structLocale.txt` (otherwise
    `/tsutil/cldrtest/TestLocaleStructure` may fail).

## 10 Running ICU4C tests in exhaustive mode.

Exhautive tests should always be run for a CLDR-ICU integration PR before it is merged.
Once you have a PR, you can do this for both C and J as part of the pre-merge CI tests
by adding the following as a comment in the pull request:<br>
`/azp run CI-Exhaustive` (the exhaustive tests are not run automatically on every PR).

The following instructions run the ICU4C exhaustive tests locally (which you may want to do
before even committing changes, or which may be necessary to diagnose failures in the
CI tests:
```
cd $ICU4C_DIR/source
export INTLTEST_OPTS="-e"
export CINTLTST_OPTS="-e"
make check 2>&1 | tee $NOTES/icu4c-newData-makeCheckEx.txt
```

## 11 Investigate and fix ICU4C exhaustive test case failures

Again, investigate each failure, fixing CLDR data or ICU test cases as
appropriate, and repeating from step 4 or 8 as appropriate.

## 12 Transfer the ICU4C data to ICU4J

12a. You need to reconfigure ICU4C to include the unicore data.
```
cd $ICU4C_DIR/source
ICU_DATA_BUILDTOOL_OPTS=--include_uni_core_data ./runConfigureICU <platform>
```

12b. Rebuild the data with the new config setting, then create the ICU4J data jar.
```
cd $ICU4C_DIR/source/data
make clean
make -j -l2.5
make icu4j-data-install
```

12c. Create the  test data jar
```
cd $ICU4C_DIR/source/test/testdata
make icu4j-data-install
```

12d. Update the extracted {main, test} data files in the Maven build
```
cd $ICU4J_ROOT/maven-build
sh ./extract-data-files.sh
```

## 13 Rebuild ICU4J with new data, run tests

13a. Run the tests using the ant build
```
cd $ICU4J_ROOT
ant clean
ant check 2>&1 | tee $NOTES/icu4j-newData-antCheck.txt
```

To re-run a specific test if necessary when fixing bugs; for example:
```
ant checkTest -Dtestclass='com.ibm.icu.dev.test.format.MeasureUnitTest'
```

13b. Optionally run the tests in exhautive mode

Optionally run before committing changes, or run to diagnose failures from
running exhastive CI tests in the PR using `/azp run CI-Exhaustive`: 
```
cd $ICU4J_ROOT
ant exhaustiveCheck 2>&1 | tee $NOTES/icu4j-newData-antCheckEx.txt
```
(Not sure there is a way to re-run a specific test in exhaustive mode)

13c. Run the tests using the Maven build
```
cd $ICU4J_ROOT/maven-build
mvn verify 2>&1 | tee $NOTES/icu4j-newData-mavenVerify.txt
```
Currently (due to maven sync issues?) there may be errors in these
local maven tests even if there are no errors in the ant check tests.
Such maven errors should not be blockers for a commit and push; they
may not show up in the CI maven tests (but if they do and do not go
away after re-running the CI test, then they need investigation).

## 14 Investigate and fix ant check test failures

Fix test cases and repeat from step 13, or fix CLDR data and repeat from
step 4, as appropriate, until there are no more failures in ICU4C or ICU4J.

Note that certain data changes and related test failures may require the
rebuilding of other kinds of data and/or code. For example:

### Updating MeasureUnit code and tests

If you see a failure such as
```
MeasureUnitTest	testCLDRUnitAvailability	Failure	(MeasureUnitTest.java:3410) : Unit present in CLDR but not available via constant in MeasureUnit: speed-beaufort 
```
then you will need to update the C and J library and test code for new measurement
units, see the procedure at
[Updating MeasureUnit with new CLDR data](https://unicode-org.github.io/icu/processes/release/tasks/updating-measure-unit.html)

### Updating plurals test data

Changes to plurals data may cause failures in e.g. the following:
```
com.ibm.icu.dev.test.format.PluralRulesTest (TestLocales)
```

To address these requires updating the LOCALE_SNAPSHOT data in
```
$ICU4J_ROOT/main/tests/core/src/com/ibm/icu/dev/test/format/PluralRulesTest.java
```
by modifying the TestLocales() test there to run `generateLOCALE_SNAPSHOT()` and
then copying in the updated data.

## 15 Check the ICU file changes and commit

```
cd $ICU4C_DIR/source
make clean
cd $ICU4J_ROOT
ant clean
cd ..
git status
```

Then `git add` or `git rm` files as necessary. Record the changes, commit and push.
```
git status >  $NOTES/gitStatusDelta-newData-afterAdd.txt
git commit -m 'ICU-nnnnn CLDR release-nn-stage to ICU main'
git push origin ICU-nnnnn-branchname
```

## 16 commit cldr-staging and tag
(Only for an official integration from CLDR git repositories)

16a. Check cldr-staging changes, and commit
```
cd $CLDR_TMP_DIR
git status
```

Then `git add` or `git rm` files as necessary. Record the changes, commit and push.
```
git status >  $NOTES/gitStatusDelta-production-afterAdd.txt
git commit -m 'CLDR-nnnnn production data corresponding to CLDR release-nn-stage'
git push origin main
```
(usually for cldr-staging we just work with the main branch)

16b. Update cldr-staging and tag

(There may be other cldr-staging changes unrelated to production data, such as charts
or spec; we want to include them in the tag, so pull first, but log to see what the
chnages are first)
```
cd $CLDR_TMP_DIR
git pull
git log
git tag -a "release-nn-stage" -m "CLDR-nnnnn: tag production data corresponding to CLDR release-nn-stage"
git push --tags
```

## 17 tag cldr
(Only for an official integration from CLDR git repositories)

We need to tag the main cldr repository. If $CLDR_DIR represents that repository,
this is easy:
```
cd $CLDR_DIR
git tag -a "release-nn-stage" -m "CLDR-nnnnn: tag CLDR release-nn-stage"
git push --tags
```

However if $CLDR_DIR represents your personal fork or a branch from it, you need to
figure out what commit hash yo have integrated, and tag that hash in the main repo.
```
cd $CLDR_DIR
git log
```
Note the latest commit hash hhhhhhhh...

Then switch to the main repo, update it, and tag the appropriate hash (making sure
it is in that repo!):
```
cd $HOME/cldr
git pull
git log
git tag -a "release-nn-stage" -m "CLDR-nnnnn: tag CLDR release-nn-stage" hhhhhhhh...
git push --tags
```

## 18 Pubish the cldr tags in github

You should publish the cldr and cldr-staging tags in github.

For cldr, go to [unicode-org/cldr/tags](https://github.com/unicode-org/cldr/tags)
and click on the tag you just created. Click on the "Create release from tag" button
at the upper right. Set release title to be the same as the tag. Click the checkbox
for "Set as a pre-release" for all but the final release. For the description, see
what was done for earlier tags; it should reference the download page for the release.
When you are all ready, click the "Publish release" button.

For cldr-staging, go to [unicode-org/cldr-staging/tags](https://github.com/unicode-org/cldr-staging/tags)
and do something similar.
