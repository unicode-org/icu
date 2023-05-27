---
layout: default
title: Ant Setup for Java
grand_parent: Setup for Contributors
parent: Java Setup
---

<!--
© 2016 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Ant Setup for Java
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---


## Overview

ICU4J source layout was changed after 4.2. There are several ways to set up the
ICU4J development environment.

Get the source code by following the [Quick Start
instruction](http://site.icu-project.org/repository). Go into the icu4j/
directory to see the build.xml file. You can run targets displayed by `ant -p`.

Main targets:

* `all` Build all primary targets
* `apireport` Run API report generator tool
* `apireportOld` Run API report generator tool (Pre Java 5 Style)
* `build-tools` Build build-tool classes
* `charset` Build charset classes
* `charset-tests` Build charset tests
* `charsetCheck` Run only the charset tests
* `check` Run the standard ICU4J test suite
* `checkDeprecated` Check consistency between javadoc @deprecated and @Deprecated annotation
* `checkTest` Run only the specified tests of the specified test class or, if no arguments are given, the standard ICU4J test suite.
* `checktags` Check API tags before release
* `cldrUtil` Build Utilities for CLDR tooling
* `clean` Clean up build outputs
* `collate` Build collation classes
* `collate-tests` Build core tests
* `collateCheck` Run only the collation tests
* `core` Build core classes
* `core-tests` Build core tests
* `coreCheck` Run only the core tests
* `coverageJaCoCo` Run the ICU4J unit tests and generate code coverage report
* `currdata` Build currency data classes
* `demos` Build demo classes
* `docs` Build API documents
* `docsStrict` Build API documents with all doclint check enabled
* `draftAPIs` Run API collector tool and generate draft API report
* `exhaustiveCheck` Run the standard ICU4J test suite in exhaustive mode
* `findbugs` Run FindBugs on all library sub projects.
* `gatherapi` Run API database generator tool
* `gatherapiOld` Run API database generator tool (Pre Java 5 style)
* `icu4jJar` Build ICU4J all-in-one core jar
* `icu4jSrcJar` Build icu4j-src.jar
* `icu4jtestsJar` Build ICU4J all-in-one test jar
* `indicIMEJar` Build indic IME 'icuindicime.jar' jar file
* `info` Display the build environment information
* `init` Initialize the environment for build and test. May require internet access.
* `jar` Build ICU4J runtime library jar files
* `jarDemos` Build ICU4J demo jar file
* `jdktzCheck` Run the standard ICU4J test suite with JDK TimeZone
* `langdata` Build language data classes
* `localespi` Build Locale SPI classes
* `localespi-tests` Build Locale SPI tests
* `localespiCheck` Run the ICU4J Locale SPI test suite
* `main` Build ICU4J runtime library classes
* `packaging-tests` Build packaging tests
* `packagingCheck` Run packaging tests
* `perf-tests` Build performance test classes
* `regiondata` Build region data classes
* `release` Build all ICU4J release files for distribution
* `releaseBinaries` Build ICU4J binary files for distribution
* `releaseCLDR` Build release files for CLDR tooling
* `releaseDocs` Build ICU4J API reference doc jar file for distribution
* `releaseSourceArchiveTgz` Build ICU4J source release archive (.tgz)
* `releaseSourceArchiveZip` Build ICU4J source release archive (.zip)
* `releaseSrcJars` Build ICU4J src jar files for distribution
* `releaseVer` Build all ICU4J release files for distribution with versioned file names
* `runTest` Run the standard ICU4J test suite without calling any other build targets
* `samples` Build sample classes
* `secure` (Deprecated)Build ICU4J API and test classes for running the ICU4J test suite with Java security manager enabled
* `secureCheck` Run the secure (applet-like) ICU4J test suite
* `test-framework` Build test framework classes
* `tests` Build ICU4J test classes
* `timeZoneCheck` Run the complete test for TimeZoneRoundTripAll
* `tools` Build tool classes
* `translit` Build translit classes
* `translit-tests` Build translit tests
* `translitCheck` Run the ICU4J Translit test suite
* `translitIMEJar` Build transliterator IME 'icutransime.jar' jar file
* `xliff` Build xliff converter tool

Default target: main
The typical usage is `ant check`, which will build main ICU4J libraries and
run the standard unit test suite.

For running ant you may need to set up some environment variables first. For
example, on Windows:

```
set ANT_HOME=C:\\ant\\apache-ant-1.7.1

set JAVA_HOME=C:\\Program Files\\Java\\jdk1.5.0_07

set PATH=%JAVA_HOME%\\bin;%ANT_HOME%\\bin;%PATH%
```

## Test arguments and running just one test or the tests of just one test class

You can pass arguments to the test system by using the 'testclass' and
'testnames' variables and the 'checkTest' target. For example:

|Command Line|Meaning|
|------------|--------|
|`ant checkTest -Dtestclass='com.ibm.icu.dev.test.lang.TestUScript'` | Runs all the tests in test class 'TestUScript'.|
|`ant checkTest -Dtestclass='com.ibm.icu.dev.test.lang.TestUScript' -Dtestnames='TestNewCode,TestHasScript'` | Runs the tests `TestNewCode` and `TestHasScript` in test class `TestUScript`. |
|`ant checkTest -Dtestnames='TestNewCode,TestHasScript'` | Error: test class not specified.|
|`ant checkTest` | Runs the standard ICU4J test suite (same as 'ant check').|

The JUnit-generated test result reports are in out/junit-results/checkTest. Go
into the `html/` subdirectory and load `index.html` into a browser.

## Generating Test Code Coverage Report

[#10513](http://bugs.icu-project.org/trac/ticket/10513) added code coverage
target "coverageJaCoCo" in the ICU4J ant build.xml. To run the target:

1.  Download JaCoCo library from [EclEmma
    site](http://eclemma.org/jacoco/index.html).
2.  Extract library files to your local system - e.g. `C:\jacoco-0.7.6`
3.  Set environment variable JACOCO_DIR pointing to the directory where JaCoCo
    files are extracted - e.g. `set JACOCO_DIR=C:\jacoco-0.7.6`
4.  Set up ICU4J ant build environment.
5.  Run the ant target "coverageJaCoCo" in the top-level ICU4J build.xml

Following output report files will be generated in /out/jacoco directory.

*   report.csv
*   report.xml
*   report_html.zip

## Building ICU4J API Reference Document with JCite

Since ICU4J 49M2, JCite (Java Source Code Citation System) is integrated into
ICU4J documentation build. To build the API documentation for public release,
you must use JCite for embedding some coding examples in the API documentation.
To set up the environment:

1.  <http://arrenbrecht.ch/jcite/>Download JCite binary (you need 1.13.0+ for JDK 7 support) from
    *   Note that JCite no longer is available for download from the official
        web site, which links to Google Code, which was closed down in 2016.
    *   The Internet Archive has a copy of the last version of JCite found on
        Google Code before it was closed down:
        [jcite-1.13.0-bin.zip](https://web.archive.org/web/20160710183051/http://jcite.googlecode.com/files/jcite-1.13.0-bin.zip)
2.  Extract JCite file to your local system - e.g. `C:\jcite-1.13.0`
3.  Set environment variable `JCITE_DIR` pointing to the directory where JCite
    files are extracted. - e.g. `set JCITE_DIR=C:\jcite-1.13.0`
4.  Set up ICU4J ant build environment.
5.  Run the ant target "docs" in the top-level ICU4J build.xml
6.  If the build (on Linux) fails because package com.sun.javadoc is not found
    then set the JAVA_HOME environment variable to point to `<path>/java/jdk`. The
    Javadoc package is in `<path>/java/jdk/lib/tools.jar`.

*Note: The ant target "docs" checks if `JCITE_DIR` is defined or not. If not
defined, it will build ICU4J API docs without JCite. In this case, JCite taglet
"{@.jcite ....}" won't be resolved and the embedded tag is left unchanged in the
output files.*

## Build and test ICU4J Eclipse Plugin

Building Eclipse ICU4J plugin

1.  Download and install the latest Eclipse release from
    <http://www.eclipse.org/> (The latest stable milestone is desired, but the
    latest official release should be OK).
2.  cd to `<icu4j root>` directory, and make sure `$ ant releaseVer` runs clean.
3.  cd to` <icu4j root>/eclipse-build` directory.
4.  Copy `build-local.properties.template` to `build-local.properties`, edit the
    properties files
    *   eclipse.home pointing to the directory where the latest Eclipse version
        is installed (the directory contains configuration, dropins, features,
        p2 and others)
    *   java.rt - see the explanation in the properties file
5.  Run the default ant target - $ ant The output ICU4J plugin jar file is
    included in `<icu4j
    root>/eclipse-build/out/projects/ICU4J.com.ibm.icu/com.ibm.icu-com.ibm.icu.zip`

Plugin integration test

1.  Backup Eclipse installation (if you want to keep it - just copy the entire
    Eclipse installation folder)
2.  Delete ICU4J plugin included in Eclipse installation -
    `<eclipse>/plugins/com.ibm.icu_XX.Y.Z.vYYYYMMDD-HHMM.jar` XX.YY.Z is the ICU
    version, and YYYYMMDD-HHMM is build date. For example,
    com.ibm.icu_58.2.0.v20170418-1837.jar
3.  Copy the new ICU4J plugin jar file built by previous steps (e.g.
    com.ibm.icu_61.1.0.v20180502.jar) to the same folder.
4.  Search a text "`com.ibm.icu"` in files under `<eclipse>/features`. The RCP
    feature has a dependency on the ICU plugin and its `feature.xml` (e.g.
    `<eclipse>/features/org.eclipse.e4.rcp_1.6.2.v20171129-0543/feature.xml`)
    contains the dependent plugin information. Replace just version attribute to
    match the version built by above steps. You can leave size attributes
    unchanged. The current ICU build script does not append hour/minute in
    plugin jar file, so the version format is XX.Y.Z.vYYYYMMDD.
    ` <plugin`
    ` id="com.ibm.icu"`
    ` download-size="11775"`
    ` install-size="26242"`
    ` version="58.2.0.v20170418-1837" -> "61.1.0.v20180502" `
    ` unpack="false"/>`
5.  Open
    `<eclipse>/configuration/org.eclipse.equinox.simpleconfigurator/bundles.info`
    in a text editor, and update the line including com.ibm.icu plugin
    information.
    ```
    com.ibm.icu,58.2.0.v20170418-1837,plugins/com.ibm.icu_58.2.0.v20170418-1837.jar,4,false
    ```
     then becomes ->
    ```
    com.ibm.icu,**61.1.0.v20190502**,plugins/com.ibm.icu_**61.1.0.v20190502**.jar,4,false
    ```
6.  Make sure Eclipse can successfully starts with no errors. If ICU4J plug-in
    is not successfully loaded, Eclipse IDE won't start.

ICU4J plugin test - Note: This is currently broken
<http://bugs.icu-project.org/trac/ticket/13072>

1.  Start the Eclipse (with new ICU4J plugin), and create a new workspace.
2.  Import existing Eclipse project from `<icu4jroot>/eclipse-build/out/projects/com.ibm.icu.tests`
3.  Run the project as JUnit Plug-in Test.

## Building ICU4J Release Files

See [Release Build](../../../processes/release/tasks/release-build.md)
