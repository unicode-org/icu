---
layout: default
title: Integration Tests
parent: Release & Milestone Tasks
grand_parent: Contributors
nav_order: 60
---

<!--
© 2021 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Integration Tests
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Run ICU4J Locale Service Provider tests

JDK 6 introduced the locale service provider interface. ICU4J release include a
special jar file (icu4j-localespi.jar) implementing the locale service provider
interface.

The test case for the ICU4J locale service provider requires JRE 6 or later
version. Otherwise, there is no extra settings except for the standard ant set
up ( necessary. To run the test cases, use ant with the top-level build.xml with
target "localespiCheck"

```sh
$ ant localespiCheck
```

You should get the output like below -

```
...
build:

_runLocalespiCheck:
     [java] TestAll {
     [java]   BreakIteratorTest {
     [java]     TestGetInstance (7.581s) Passed
     [java]     TestICUEquivalent (0.832s) Passed
     [java]   } (8.669s) Passed
     [java]   CollatorTest {
     [java]     TestGetInstance (0.900s) Passed
     [java]     TestICUEquivalent (0.040s) Passed
     [java]   } (0.966s) Passed
     [java]   CurrencyNameTest {
     [java]     TestCurrencySymbols (2.682s) Passed
     [java]   } (2.683s) Passed
     [java]     DateFormatSymbolsTest {
     [java]     TestGetInstance (0.348s) Passed
     [java]     TestICUEquivalent (0.207s) Passed
     [java]     TestNynorsk (0.000s) Passed
     [java]     TestSetSymbols (0.071s) Passed
     [java]   } (0.631s) Passed
     [java]   DateFormatTest {
     [java]     TestGetInstance (0.456s) Passed
     [java]     TestICUEquivalent (0.113s) Passed
     [java]     TestThaiDigit (0.000s) Passed
     [java]   } (0.571s) Passed
     [java]   DecimalFormatSymbolsTest {
     [java]     TestGetInstance (0.098s) Passed
     [java]     TestICUEquivalent (0.000s) Passed
     [java]     TestSetSymbols (0.000s) Passed
     [java]   } (0.099s) Passed
     [java]   LocaleNameTest {
     [java]     TestCountryNames (2.500s) Passed
     [java]     TestLanguageNames (14.262s) Passed
     [java]   TestVariantNames (8.638s) Passed
     [java]   } (25.402s) Passed
     [java]   NumberFormatTest {
     [java]     TestGetInstance (0.266s) Passed
     [java]     TestICUEquivalent (0.209s) Passed
     [java]   } (0.475s) Passed
     [java]   TimeZoneNameTest {
     [java]     TestTimeZoneNames (17.766s) Passed
     [java]   } (17.766s) Passed
     [java] } (57.268s) Passed
     [java]
     [java] Test cases taking excessive time (>10s):
     [java] TestAll/LocaleNameTest/TestLanguageNames (14.262s)
     [java] TestAll/TimeZoneNameTest/TestTimeZoneNames (17.766s)
     [java]

BUILD SUCCESSFUL
Total time: 1 minute 46 seconds
```

---

## Run ICU4J Test cases with JDK TimeZone

ICU4J provides an option to alter TimeZone implementation to use the JRE's own
implementation. The test cases used for this is the regular ICU4J unit test
suite, except forcing ICU4J to use JRE TimeZone by special system property -
com.ibm.icu.util.TimeZone.DefaultTimeZoneType=JDK.

To run the test, you have to apply the latest time zone patch from a JRE vendor,
because some test cases are sensitive to actual time zone transitions. For
Oracle JRE, you should go to the J2SE download page
<http://www.oracle.com/technetwork/java/javase/downloads/index.html> and
download the latest JDK DST Timezone Update Tool and apply the patch to your
local JRE.

To run the test case, you just need to invoke the ant target "jdktzCheck".

```sh
$ ant jdktzCheck
```

**Note:** You might not be able to get the update tool matching the tzdata
version used by ICU. In this case, some test cases may reports failures.
Unfortunately, you have to walk though the failures to see if they are expected
or not manually in this case.

---

## Verify the Eclipse ICU4J plug-in

1.  Make sure the Eclipse ICU4J plug-in binaries are successfully produced.
2.  Run the ICU4J plug-in test cases.
3.  Update license files, build version strings for the new release.

---

## Run Tests Without ICU Data

### ICU4C

ICU data should be removed, so that tests cannot access it. Both cintltst and
intltest should be run with -w option and they should not crash. Every crash
should be investigated and fixed.

To do this, build and test normally, then replace the ICU data shared library
with the stubdata library and run the tests again with the -w option.

Using an in-source build on Linux:

```sh
cd icu4c/source
./runConfigureICU Linux
make -j2 check
rm lib/libicudata.so*
cp -P stubdata/libicudata.so* lib/
cd test/intltest
INTLTEST_OPTS=-w make check
cd ../cintltst
CINTLTST_OPTS=-w make check
```

For debugging (for example using gdb) you cannot use `make check`. You need
to set the `LD_LIBRARY_PATH` for the lib folder and then run
`./cintltst -w` or `./intltest -w` etc. in the debugger.
Example (in-source build on Linux):
```sh
cd test/cintltst
export LD_LIBRARY_PATH=../../lib:../../tools/ctestfw
./cintltst -w
```

### ICU4J

ICU4J has the test target for this, but does not work as designed for now. For
now, this task is not required for ICU4J.

---

## Verify that ICU4C tests pass without collation rule strings

Note: Since ICU 73, this test has been included in the Github Actions Continuous Integration jobs.
These instructions explain how to run the test manually.

***ICU4C 53 and later***

Background: [ICU-10636](https://unicode-org.atlassian.net/browse/ICU-10636)

### Rebuild ICU data without collation rule strings

#### ICU 64+

You need to configure ICU with a data filter config file that removes collation
rule strings.

The file syntax is nicer with hjson rather than json.

```sh
sudo apt install python3-pip
pip3 install hjson
```

Use an `~/icu/coll-norules.hjson` config file like this:

```
{
  resourceFilters: [
    {
      categories: [
        coll_tree
      ]
      rules: [
        -/UCARules
        -/collations/*/Sequence
      ]
    }
  ]
}
```

Configure ICU using this file:

```sh
ICU_DATA_FILTER_FILE=~/icu/coll-norules.hjson ... runConfigureICU ...
```

Run "make clean" (or delete just the collation .res files), then test as below.

#### ICU 54..63

This should work: `make GENRBOPTS='-k --omitCollationRules'` (-k is --strict)

For ICU 54..63, I went into the build output folder and did:

```sh
cd data
ICUDT=icudt63l
rm out/build/$ICUDT/coll/*.res
make GENRBOPTS='-k --omitCollationRules'
```

If this does not work, then add this option to the configure'd data/Makefile,
see ticket [ICU-10636](https://unicode-org.atlassian.net/browse/ICU-10636).

### Run the tests with data-errors-as-warnings

`INTLTEST_OPTS=-w CINTLTST_OPTS=-w make -j5 check`

See that they pass, or fix them to pass. See ticket #10636 test code changes for
examples.

---

## Test ICU4J with only little-endian ICU4C data

*Only available since ICU 54.*

### With ICU 70 and later:

This test is performed automatically via a post-merge GHA check,
based on the following instructions for ICU 64+.

### With ICU 64 and later:

*   Reconfigure ICU4C with
    <code><b>ICU_DATA_BUILDTOOL_OPTS=--include_uni_core_data</b>
    ./runConfigureICU Linux</code> or similar
    *   Should be little-endian for coverage
*   Clean and build ICU4C: `make -j6 check`
*   Make a clean directory for testing
    *   Find the .data file in the build output area, e.g.,
        `icu4c/source/data/out/tmp/icudt64l.dat`
    *   Create a temporary directory such as `mkdir -p /tmp/icu4j_data_test`
    *   Copy the .`dat` file to the new directory.
*   Build and test ICU4J without its own data: `ant clean && ant
    -Dicu4c.data.path=/tmp/icu4j_data_test check`
    *   The configuration option sets the `ICUConfig.properties` data path
    *   Verify that all tests pass.
    *   If you get very many test failures, double-check that you enabled
        unicore data in the ICU4C build (see first step).

### With ICU 55 through ICU 63:

*   Rebuild ICU4C with <code>make <b>INCLUDE_UNI_CORE_DATA=1</b> check</code> or
    similar, and provide a path only for the .dat file.
    *   Should be little-endian for coverage
    *   Find the .data file in the build output area, e.g.,
        `data/out/tmp/icudt59l.dat`
    *   Create a temporary directory such as `mkdir -p
        /tmp/icu/build/data/out/tmp`
    *   Copy the .`dat` file to the new directory.
*   Build and test ICU4J without its own data: `ant clean && ant
    -Dicu4c.data.path=/tmp/icu/build/data/out/tmp check`
    *   The configuration option sets the `ICUConfig.properties` data path
    *   Verify that all tests pass.

### ICU 54 method:

In `icu4j-core/src/com/ibm/icu/ICUConfig.properties` set
`com.ibm.icu.impl.ICUBinary.dataPath` to a list of paths with all of the ICU4C
data (should be little-endian for coverage), with a path to where the .dat file
is and a path to the source/data/in files for data that is hardcoded in ICU4C
and therefore not in the .dat file (e.g., uprops.icu).

Change `com.ibm.icu.impl.ICUData.logBinaryDataFromInputStream` to `true`, maybe
set a breakpoint where such a message is logged.

Run all of the ICU4J tests, maybe in the debugger for the breakpoint, or look
for logger output to the console.

Revert your config changes.

## Build and run testmap

Build and run the source/test/testmap project. (There is currently no Windows
project defined for it.)

```sh
$ cd <root of your ICU build tree>
$ CONFIG_FILES=test/testmap/Makefile CONFIG_HEADERS= ./config.status
$ cd test/testmap
$ make check
```

---

## Verify XLIFF conversion

<span style="background-color:orange">**Note:** The following instruction does not work. Please read the comments with
orange background. There are some issues in the current ICU XLIFF tools and the
test case below. See the comments in
[ticket ICU-6383](https://unicode-org.atlassian.net/browse/ICU-6383).</span>

Instructions for verifying the XLIFF conversion tools.

*   Convert icu/source/test/testdata/ra.txt to XLIFF

    `genrb -s icu/source/test/testdata -d icu/source/test/testdata/ -x -l en ra.txt`

    <span style="background-color:orange">-d icu/source/test/testdata/ overwrite
    the existing ra.xlf. Specify another directory.</span>

    <span style="background-color:orange">ra.txt has the top level item "ra",
    which is supposed to be the content language. Thus, with -l en, you'll get a
    warning - "The top level tag in the resource and language specified are not
    the same. Please check the input." We should use "-l ra" here.</span>

*   Verify that the ra.xlf produced is identical to the one in CVS HEAD (except
    for generation date)

    <span style="background-color:orange">If you use "-l ra" above, you'll get
    <file .... source-language = "ra" .... />, which is different from ra.xlf in
    the repository. Also, new line codes is broken for imported contents.</span>

*   Convert icu/source/test/testdata/ra.xlf back to ICU format

    `java -cp icu4j/classes com.ibm.icu.dev.tool.localeconverter.XLIFF2ICUConverter -d . -t ra ra.xlf`

    <span style="background-color:orange">The option "-t ra" does not work,
    because ra.xlf does not contain target language data. Use "-c ra"
    instead.</span>

*   Verify that the ra.txt produced is identical to the one in CVS HEAD (except
    for generation date)

    <span style="background-color:orange">You cannot expect the generated ra.txt
    exactly matches the original one because of table item re-ordering, new line
    code changes, and explicit resource types (e.g. "ra {" vs. "ra:table
    {").</span>
*   Go through the steps given in
    <http://icu.sourceforge.net/docs/papers/localize_with_XLIFF_and_ICU.pdf>

---

## Test sample and demo programs

Build and run all of the sample and demo apps that are included with ICU, on
each of the reference platforms. A list of them is in the
[readme](http://icu.sanjose.ibm.com/internal/checklist/icu-progs-list.html).
Also see the build system.

Another worthy test: Test suites and demos *from the previous release* should
also compile and run with the libraries of the current release, at least when
certain #defines are set (unless they test APIs that are deprecated and have
been removed since)!

---

## Test data portability

Test if the data portability (under common endianness & charset family) is ok.
On the ICU build server, you would use the "Build from source .dat archive"
option. When it's not available, you would do the following:

1.  Build ICU4C on Win32.
2.  Copy the icu/source/data/out/icudt<data_version>l.dat file into
    icu/source/data/in
3.  Delete non-essential directories from icu/source/data.
4.  Package up a clean copy of ICU for a non-Windows machine, like Linux on x86.
5.  Build ICU on the non-Windows machine from the newly created package.
6.  Run all tests on that non-Windows machine.

---

## Run the environment tests
This test is performed automatically by a GitHub Action once per week. We can also run it manually by visiting 
[GHA EnvTest Action page](https://github.com/unicode-org/icu/actions/workflows/icu_envtest.yml) then click on the "Run workflow v" drop down on the right side of the screen

Run
[environmentTest.sh](https://github.com/unicode-org/icu/blob/main/tools/release/c/environmentTest.sh)
on a Linux machine which has many (all possible?) POSIX locales installed. This
test verifies that the ICU test suite will work regardless of a user's default
locale and timezone. This test should be run on a fast machine with several CPU
cores. This will take a long time to run. Here are the steps to run the test.

1.  cd icu4c/source/
2.  ./runConfigureICU Linux
3.  make check
4.  cd ../../tools/release/c/
5.  ./environmentTest.sh
6.  Wait a while for the tests to finish. The logs will be written in each test
    directory. e.g. icu4c/source/test/intltest/intltest-\*.txt. A message will
    be printed from each spawned test script when it finishes.
7.  grep the logs for any test failures when the tests are done.

---

## Run Thread Sanitizer tests

Thread sanitizer testing is one of the standard Travis builds. If it is passing
there, nothing further is required.

To run manually, on a Linux system with clang,

```sh
cd icu4c/source
CPPFLAGS=-fsanitize=thread LDFLAGS=-fsanitize=thread ./runConfigureICU --enable-debug --disable-release Linux
make clean
make -j6 check
```

Errors are displayed at the point they occur, and stop further testing.

---

## Run Address Sanitizer tests

Address sanitizer testing is included in the standard Linux with Clang Travis
builds. If it is passing there, nothing further is required.

To run manually, on a Linux system with clang,

```sh
cd icu4c/source
CPPFLAGS=-fsanitize=address LDFLAGS=-fsanitize=address ./runConfigureICU --enable-debug --disable-release Linux
make clean
make -j6 check
```

Memory leaks are summarized at the end. Other errors are displayed at the point
they occur, and stop further testing.

---

## ICU4J Serialization Compatibility Test Data

The regular ICU4J unit test includes serialization compatibility tests. The test
case creates an ICU service object from serialized data created by a former
version of ICU. When we prepare a new release, the serialization compatibility
test data should be created and checked in for future testing. This task is
usually done just before publishing release candidate.

1.  Run regular ICU4J unit tests - `ant check`
2.  Make sure the unit tests pass successfully.
3.  Run - `ant serialTestData`
4.  Copy a folder with ICU version (e.g. ICU_61.1) generated under <icu4j
    root>/out/serialTestData to <icu4j
    root>/main/tests/core/src/com/ibm/icu/dev/test/serializable/data.
5.  You may delete older serialization test data from the directory (but keep
    the oldest one - ICU_3.6).
6.  Run `ant check` again before committing the changes.
