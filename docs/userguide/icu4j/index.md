---
layout: default
title: ICU4J
nav_order: 500
has_children: true
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# ICU4J Readme
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Introduction to ICU4J

The International Components for Unicode (ICU) library provides robust and full-featured Unicode services on a wide variety of platforms. ICU supports the most current version of the Unicode standard, including support for supplementary characters (needed for GB 18030 repertoire support).

Java provides a strong foundation for global programs, and IBM and the ICU team played a key role in providing globalization technology to Java. But because of its long release schedule, Java cannot always keep up with evolving standards. The ICU team continues to extend Java's Unicode and internationalization support, focusing on improving performance, keeping current with the Unicode standard, and providing richer APIs, while remaining as compatible as possible with the original Java text and internationalization API design.

ICU4J is an add-on to the regular JRE that provides:

*   [Collation](../collation) – rule-based, up-to-date Unicode Collation Algorithm (UCA) sorting order
        For fast multilingual string comparison; faster and more complete than the J2SE implementation
*   [Charset Detection](../conversion/detection) – Recognition of various single and multibyte charsets
        Useful for recognizing untagged text data
*   [UnicodeSet](../strings/unicodeset) – standard set operations optimized for sets of Unicode characters
        UnicodeSets can be built from string patterns using any Unicode properties.
*   [Transforms](../transforms) – a flexible mechanism for Unicode text conversions
        Including Full/Halfwidth conversions, Normalization, Case conversions, Hex conversions, and transliterations between scripts (50+ pairs)
*   [Unicode Normalization](../transforms/normalization) – NFC, NFD, NFKD, NFKC
        For canonical text representations, needed for XML and the net
*   [International Calendars](../datetime/calendar) – Arabic, Buddhist, Chinese, Hebrew, Japanese, Ethiopic, Islamic, Coptic and other calendars
        Required for correct presentation of dates in certain countries
*   [Date Format Enhancements](../format_parse/datetime) – Date/time pattern generator, Relative date formatting, etc.
        Enhancements to the normal Java date formatting.
*   [Number Format Enhancements](../format_parse/numbers) – Scientific Notation, Spelled-out, Compact decimal format, etc.
        Enhancements to the normal Java number formatting. The spell-out format is used for checks and similar documents
*   [Enhanced Word-Break Detection](../boundaryanalysis) – Rule-based, supports Thai, Khmer, Chinese, etc.
        Required for correct support of Thai
*   [Unicode Text Compression](../conversion/compression) – Standard compression of Unicode text
        Suitable for large numbers of small fields, where LZW and similar schemes do not apply
*   [Charset Conversion](../conversion) – Conversion to and from different charsets.
        Plugs into Java CharsetProvider Service Provider Interface (SPI)

> :point_right: **Note:** We continue to provide assistance to Java, and in some cases, ICU4J support has been rolled into a later release of Java. For example, BCP47 language tag support including Unicode locale extensions is now in Java 7\. However, the most current and complete version is always found in ICU4J.

## Platform Dependencies

Check the [Downloading ICU](https://icu.unicode.org/download) page to look up the minimum supported version of Java
for your version of ICU.

## How to Download ICU4J

There are a few different ways to download the ICU4J releases.

*   **Official Release:**
    If you want to use ICU4J (as opposed to developing it), your best bet is to download an official, packaged version of the ICU4J library files. These versions are tested more thoroughly than day-to-day development builds, and they are packaged in jar files for convenient download.
    *   [ICU Download page](https://icu.unicode.org/download)
    *   Maven repository:

        ```
        <dependency>
            <groupId>com.ibm.icu</groupId>
            <artifactId>icu4j</artifactId>
            <version>68.1</version>
        </dependency>

        <dependency>
            <groupId>com.ibm.icu</groupId>
            <artifactId>icu4j-charset</artifactId>
            <version>68.1</version>
        </dependency>

        <dependency>
            <groupId>com.ibm.icu</groupId>
            <artifactId>icu4j-localespi</artifactId>
            <version>68.1</version>
        </dependency>
        ```

*   **GitHub Source Repository:**
    If you are interested in developing features, patches, or bug fixes for ICU4J, you should probably be working with the latest version of the ICU4J source code. You will need to clone and checkout the code from our GitHub repository to ensure that you have the most recent version of all of the files. There are several ways to do this. Please follow the directions that are contained on the [Source Code Setup page](../../devsetup/source/) for details.

For more details on how to download ICU4J directly from the web site, please see the ICU download page at [https://icu.unicode.org/download](https://icu.unicode.org/download)


## The Structure and Contents of ICU4J - ICU 74 and later

Below, all directory paths are relative to the directory where the ICU4J source archive is extracted.

### Information and build files

| Path                         | Description                                                                             |
|------------------------------|-----------------------------------------------------------------------------------------|
| `pom.xml`                   | The root Maven build file for ICU4J. See [How to Install and Build](#how-to-install-and-build---icu-74-and-later) for more information |
| `LICENSE` | ICU license                                                                             |

### ICU4J runtime class files

Each sub-component is represented in the Maven build as a separate Maven module / project.

The directory structure of the codebase within each module follows Maven defaults.
Thus, runtime class sources will be in the `src/main/java` subdirectory of the module.
Ex: the class `com.ibm.icu.text.Collator` can be found at `<ICU>/icu4j/main/collate/src/main/java/com/ibm/icu/text/Collator.java`.
Any resource files (non-source files) will be located at `src/main/resources`.


| Sub-component Path                    | Sub-component Name | Build Dependencies        | Public API Packages | Description   |
|-------------------------|--------------------|---------------------------|---------------------|---------------|
| `main/charset`    | `icu4j-charset`      | `icu4j-core` | `com.ibm.icu.charset` | Implementation of   `java.nio.charset.spi.CharsetProvider`. This sub-component is shipped as `icu4j-charset.jar` along with ICU charset converter data files. |
| `main/collate`    | `collate`      | `core` | `com.ibm.icu.text` <br/> `com.ibm.icu.util` | Collator APIs and implementation. Also includes some public API classes that depend on Collator. This sub-component is packaged as a part of `icu4j.jar`. |
| `main/core`       | `core`         | n/a  | `com.ibm.icu.lang` <br/> `com.ibm.icu.math` <br/> `com.ibm.icu.text` <br/> `com.ibm.icu.util` | ICU core API classes and implementation. This sub-component is packaged as a part of `icu4j.jar`. |
| `main/currdata`   | `currdata`     | `core` | n/a | No public API classes. Provides access to currency display data. This sub-component is packaged as a part of `icu4j.jar`. |
| `main/langdata`   | `langdata`     | `core` | n/a | No public API classes. Provides access to language display data. This sub-component is packaged as a part of `icu4j.jar`. |
| `main/localespi`  | `icu4j-localespi`    | `core` <br/> `collate` | n/a | Implementation of various locale-sensitive service providers defined in   `java.text.spi`   and   `java.util.spi`  in J2SE 6.0 or later Java releases. This sub-component is shipped as `icu4j-localespi.jar`. |
| `main/regiondata` | `regiondata`   | `core` | n/a | No public API classes. Provides access to region display data. This sub-component is packaged as a part of `icu4j.jar`. |
| `main/translit`   | `translit`     | `core` | `com.ibm.icu.text` | Transliterator APIs and implementation. This sub-component is packaged as a part of `icu4j.jar`. |

### ICU4J unit test files

Unit test and integration tests are grouped together in the same Maven submodule as the runtime class code
for the same component.
The test source code lives in the standard Maven directory structure,
`src/test/java` for test sources,
and `src/test/resources` for test-specific non-source files.

> :point_right: **Note**: Tests that depend on multiple sub-components must be placed
in the `main/common_tests` directory and run as integration tests.

The Maven build enforces isolation vis-a-vis dependencies for tests within a multi-module project such that
unit tests must only test code within its same submodule.
When a test depends on runtime code from multiple modules, it cannot exist in any of those modules,
or else it would create a circular dependency in the Maven build.
Instead, such tests should be refactored into a separate component
and designated as integration tests.

| Path                                      | Sub-component Name              | Runtime Dependencies                                                                                         | Description                                     |
|-------------------------------------------|---------------------------------|--------------------------------------------------------------------------------------------------------------|-------------------------------------------------|
| `main/common_tests`                        | `common_tests`             | `core` <br/> `currdata` <br/> `translit` <br/> `langdata` <br/> `collate` <br/> `regiondata` <br/>                                                                 | Sub-component for integration tests that depend on runtime code of multiple components. |


### Others

<table>
    <tr>
        <th>Path</th>
        <th>Description</th>
    </tr>
    <tr>
        <td><code>demos</code></td>
        <td>ICU4J demo programs.</td>
    </tr>
    <tr>
        <td><code>perf-tests</code></td>
        <td>ICU4J performance test files.</td>
    </tr>
    <tr>
        <td><code>tools</code></td>
        <td>ICU4J tools including: <ul> <li>Custom JavaDoc taglets used for generating ICU4J API references.</li> <li>API report tool and data.</li> <li>Other independent utilities used for ICU4J development.</li> </ul></td>
    </tr>
</table>

## The Structure and Contents of ICU4J - ICU 73 and earlier

Below, all directory paths are relative to the directory where the ICU4J source archive is extracted.

### Information and build files

| Path                         | Description                                                                             |
|------------------------------|-----------------------------------------------------------------------------------------|
| `build.xml`                   | The main Ant build file for ICU4J. See [How to Install and Build](#how-to-install-and-build---icu-73-and-earlier) for more information |
| `main/shared/licenses/LICENSE` | ICU license                                                                             |

### ICU4J runtime class files

| Path                    | Sub-component Name | Build Dependencies        | Public API Packages | Description   |
|-------------------------|--------------------|---------------------------|---------------------|---------------|
| `main/classes/charset`    | `icu4j-charset`      | `icu4j-core` | `com.ibm.icu.charset` | Implementation of   `java.nio.charset.spi.CharsetProvider`. This sub-component is shipped as `icu4j-charset.jar` along with ICU charset converter data files. |
| `main/classes/collate`    | `icu4j-collate`      | `icu4j-core` | `com.ibm.icu.text` <br/> `com.ibm.icu.util` | Collator APIs and implementation. Also includes some public API classes that depend on Collator. This sub-component is packaged as a part of `icu4j.jar`. |
| `main/classes/core`       | `icu4j-core`         | n/a  | `com.ibm.icu.lang` <br/> `com.ibm.icu.math` <br/> `com.ibm.icu.text` <br/> `com.ibm.icu.util` | ICU core API classes and implementation. This sub-component is packaged as a part of `icu4j.jar`. |
| `main/classes/currdata`   | `icu4j-currdata`     | `icu4j-core` | n/a | No public API classes. Provides access to currency display data. This sub-component is packaged as a part of `icu4j.jar`. |
| `main/classes/langdata`   | `icu4j-langdata`     | `icu4j-core` | n/a | No public API classes. Provides access to language display data. This sub-component is packaged as a part of `icu4j.jar`. |
| `main/classes/localespi`  | `icu4j-localespi`    | `icu4j-core` <br/> `icu4j-collate` | n/a | Implementation of various locale-sensitive service providers defined in   `java.text.spi`   and   `java.util.spi`  in J2SE 6.0 or later Java releases. This sub-component is shipped as `icu4j-localespi.jar`. |
| `main/classes/regiondata` | `icu4j-regiondata`   | `icu4j-core` | n/a | No public API classes. Provides access to region display data. This sub-component is packaged as a part of `icu4j.jar`. |
| `main/classes/translit`   | `icu4j-translit`     | `icu4j-core` | `com.ibm.icu.text` | Transliterator APIs and implementation. This sub-component is packaged as a part of `icu4j.jar`. |

### ICU4J unit test files

| Path                                      | Sub-component Name              | Runtime Dependencies                                                                                         | Description                                     |
|-------------------------------------------|---------------------------------|--------------------------------------------------------------------------------------------------------------|-------------------------------------------------|
| `main/tests/charset`                        | `icu4j-charset-tests`             | `icu4j-charset` <br/> `icu4j-core` <br/> `icu4j-test-framework`                                                                | Test suite for charset sub-component.           |
| `main/tests/collate`                        | `icu4j-collate-tests`             | `icu4j-collate` <br/> `icu4j-core` <br/> `icu4j-test-framework`                                                                | Test suite for collate sub-component.           |
| `main/tests/core`                           | `icu4j-core-tests`                | `icu4j-core` <br/> `icu4j-currdata` <br/> `icu4j-langdata` <br/> `icu4j-regiondata` <br/> `icu4j-test-framework`                               | Test suite for core sub-component.              |
| `main/tests/framework`                      | `icu4j-test-framework`            | `icu4j-core`                                                                                                   | Common ICU4J unit test framework and utilities. |
| `main/tests/localespi`                      | `icu4j-localespi-tests`           | `icu4j-core` <br/> `icu4j-collate` <br/> `icu4j-currdata` <br/> `icu4j-langdata` <br/> `icu4j-localespi` <br/> `icu4j-regiondata` <br/> `icu4j-test-framework` | Test suite for localespi sub-component.         |
| `main/tests/packaging` | `icu4j-packaging-tests` | `icu4j-core` <br/> `icu4j-test-framework`                                                                      | Test suite for sub-component packaging.         |
| `main/tests/translit`                       | `icu4j-translit-tests`            | `icu4j-core` <br/> `icu4j-translit` <br/> `icu4j-test-framework`                                                               | Test suite for translit sub-component.          |

### Others

<table>
    <tr>
        <th>Path</th>
        <th>Description</th>
    </tr>
    <tr>
        <td><code>main/shared</code></td>
        <td>Files shared by ICU4J sub-components under the <code>main</code> directory including: <ul> <li>ICU4J runtime data archive (icudata.jar).</li> <li>ICU4J unit test data archive (testdata.jar).</li> <li>Shared Ant build script and configuration files.</li> <li>License files.</li></ul> </td>
    </tr>
    <tr>
        <td><code>demos</code></td>
        <td>ICU4J demo programs.</td>
    </tr>
    <tr>
        <td><code>perf-tests</code></td>
        <td>ICU4J performance test files.</td>
    </tr>
    <tr>
        <td><code>tools</code></td>
        <td>ICU4J tools including: <ul> <li>Custom JavaDoc taglets used for generating ICU4J API references.</li> <li>API report tool and data.</li> <li>Other independent utilities used for ICU4J development.</li> </ul></td>
    </tr>
    <tr>
        <td><code>lib</code></td>
        <td>Folder used for downloading depedency libraries. <br/> <b>Note:</b> ICU4J runtime libraries do not depend on any external libraries other than JDK. These dependencies are for testing (such as JUnit). </td>
    </tr>
</table>


## Where to get Documentation

The [ICU User Guide](../) contains lots of general information about ICU, in its C, C++, and Java incarnations.

The complete API documentation for ICU4J (javadoc) is available on the ICU4J web site, and can be built from the sources:

*   [Index to all ICU4J API](https://unicode-org.github.io/icu-docs/apidoc/released/icu4j/)
*   [Charset Detector](https://unicode-org.github.io/icu-docs/apidoc/released/icu4j/com/ibm/icu/text/CharsetDetector.html) – Detection of charset from a byte stream
*   International Calendars – [Buddhist](https://unicode-org.github.io/icu-docs/apidoc/released/icu4j/com/ibm/icu/util/BuddhistCalendar.html), [Chinese](https://unicode-org.github.io/icu-docs/apidoc/released/icu4j/com/ibm/icu/util/ChineseCalendar.html), [Coptic](https://unicode-org.github.io/icu-docs/apidoc/released/icu4j/com/ibm/icu/util/CopticCalendar.html), [Ethiopic](https://unicode-org.github.io/icu-docs/apidoc/released/icu4j/com/ibm/icu/util/EthiopicCalendar.html), [Gregorian](https://unicode-org.github.io/icu-docs/apidoc/released/icu4j/com/ibm/icu/util/GregorianCalendar.html), [Hebrew](https://unicode-org.github.io/icu-docs/apidoc/released/icu4j/com/ibm/icu/util/HebrewCalendar.html), [Indian](https://unicode-org.github.io/icu-docs/apidoc/released/icu4j/com/ibm/icu/util/IndianCalendar.html), [Islamic](https://unicode-org.github.io/icu-docs/apidoc/released/icu4j/com/ibm/icu/util/IslamicCalendar.html), [Japanese](https://unicode-org.github.io/icu-docs/apidoc/released/icu4j/com/ibm/icu/util/JapaneseCalendar.html), Persian, Dangi.
*   Time Zone Enhancements – [Time zone transition and rule detection](https://unicode-org.github.io/icu-docs/apidoc/released/icu4j/com/ibm/icu/util/BasicTimeZone.html), [iCalendar VTIMEZONE formatting and parsing](https://unicode-org.github.io/icu-docs/apidoc/released/icu4j/com/ibm/icu/util/VTimeZone.html), [Custom time zones constructed by user defined rules](https://unicode-org.github.io/icu-docs/apidoc/released/icu4j/com/ibm/icu/util/RuleBasedTimeZone.html).
*   Date Format Enhancements – [Date/Time Pattern Generator](https://unicode-org.github.io/icu-docs/apidoc/released/icu4j/com/ibm/icu/text/DateTimePatternGenerator.html), [Date Interval Format](https://unicode-org.github.io/icu-docs/apidoc/released/icu4j/com/ibm/icu/text/DateIntervalFormat.html), [Duration Format](https://unicode-org.github.io/icu-docs/apidoc/released/icu4j/com/ibm/icu/text/DurationFormat.html).
*   [Unicode Normalization](https://unicode-org.github.io/icu-docs/apidoc/released/icu4j/com/ibm/icu/text/Normalizer.html) – Canonical text representation for W3C.
*   [Number Format Enhancements](https://unicode-org.github.io/icu-docs/apidoc/released/icu4j/com/ibm/icu/text/NumberFormat.html) – Scientific Notation, Spelled out.
*   [Enhanced word-break detection](https://unicode-org.github.io/icu-docs/apidoc/released/icu4j/com/ibm/icu/text/BreakIterator.html) – Rule-based, supports Thai
*   [Transliteration](https://unicode-org.github.io/icu-docs/apidoc/released/icu4j/com/ibm/icu/text/Transliterator.html) – A general framework for converting text from one format to another, e.g. Cyrillic to Latin, or Hex to Unicode.
*   Unicode Text [Compression](https://unicode-org.github.io/icu-docs/apidoc/released/icu4j/com/ibm/icu/text/UnicodeCompressor.html) & [Decompression](https://unicode-org.github.io/icu-docs/apidoc/released/icu4j/com/ibm/icu/text/UnicodeDecompressor.html) – 2:1 compression on English Unicode text.
*   Collation – [Rule-based sorting](https://unicode-org.github.io/icu-docs/apidoc/released/icu4j/com/ibm/icu/text/RuleBasedCollator.html), [Efficient multi-lingual searching](https://unicode-org.github.io/icu-docs/apidoc/released/icu4j/com/ibm/icu/text/StringSearch.html), [Alphabetic indexing](https://unicode-org.github.io/icu-docs/apidoc/released/icu4j/com/ibm/icu/text/AlphabeticIndex.html)

## How to Install and Build - ICU 74 and later

Using a pre-built version of ICU from Maven Central can be achieved simply by using the artifact
coordinates as described above in the Maven portion of the [How to Download ICU4J](#how-to-download-icu4j)
section.

For non Maven-aware builds, to install ICU4J, simply place the pre-built jar file `icu4j.jar` on your Java `CLASSPATH`.
If you need Charset API support please also place `icu4j-charset.jar` on your class path along with `icu4j.jar`.

To build ICU4J, you will need a version of Maven and JDK supported by the version of ICU
and by the Maven build configuration. See [Maven Setup for Java](../../devsetup/java/maven).

Once the JDK and Maven are installed, run the desired Maven target. For example:

~~~
~/icu/icu4j$ mvn verify
[INFO] Scanning for projects...
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Build Order:
[INFO] 
[INFO] International Components for Unicode for Java (icu4j-root)         [pom]
[INFO] framework                                                          [jar]
[INFO] core                                                               [jar]
[INFO] langdata                                                           [jar]
[INFO] regiondata                                                         [jar]
[INFO] currdata                                                           [jar]
[INFO] collate                                                            [jar]
[INFO] translit                                                           [jar]
[INFO] icu4j                                                              [jar]
[INFO] icu4j-charset                                                      [jar]
[INFO] common_tests                                                       [jar]
[INFO] icu4j-localespi                                                    [jar]
[INFO] demos                                                              [jar]
[INFO] samples                                                            [jar]
[INFO] tools_misc                                                         [jar]
[INFO] utilities-for-cldr                                                 [jar]
[INFO] perf-tests                                                         [jar]
[INFO] 
[INFO] -----------------------< com.ibm.icu:icu4j-root >-----------------------
[INFO] Building International Components for Unicode for Java (icu4j-root) 74.1-SNAPSHOT [1/17]
[INFO] --------------------------------[ pom ]---------------------------------
[INFO] 
[INFO] --- maven-enforcer-plugin:3.3.0:enforce (enforce-maven) @ icu4j-root ---
[INFO] Rule 0: org.apache.maven.enforcer.rules.version.RequireMavenVersion passed
[INFO] 

...
...
...

[INFO] Reactor Summary for International Components for Unicode for Java (icu4j-root) 74.1-SNAPSHOT:
[INFO] 
[INFO] International Components for Unicode for Java (icu4j-root) SUCCESS [  0.317 s]
[INFO] framework .......................................... SUCCESS [  1.125 s]
[INFO] core ............................................... SUCCESS [02:28 min]
[INFO] langdata ........................................... SUCCESS [  0.117 s]
[INFO] regiondata ......................................... SUCCESS [  0.113 s]
[INFO] currdata ........................................... SUCCESS [  0.113 s]
[INFO] collate ............................................ SUCCESS [ 32.421 s]
[INFO] translit ........................................... SUCCESS [ 27.787 s]
[INFO] icu4j .............................................. SUCCESS [  0.019 s]
[INFO] icu4j-charset ...................................... SUCCESS [ 17.512 s]
[INFO] common_tests ....................................... SUCCESS [ 25.786 s]
[INFO] icu4j-localespi .................................... SUCCESS [  0.202 s]
[INFO] demos .............................................. SUCCESS [  0.111 s]
[INFO] samples ............................................ SUCCESS [  0.076 s]
[INFO] tools_misc ......................................... SUCCESS [  0.079 s]
[INFO] utilities-for-cldr ................................. SUCCESS [  1.284 s]
[INFO] perf-tests ......................................... SUCCESS [  0.128 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  04:16 min
[INFO] Finished at: 2023-10-03T16:16:06-07:00
[INFO] ------------------------------------------------------------------------
~~~

> :point_right: **Note**:  The above output is an example. The numbers are likely to be different with the current version ICU4J.

For more information on how to build or test specific components, 
or run specific tests,
or to set up your IDE, refer to
[Maven Setup for Java](../../devsetup/java/maven)

## How to Install and Build - ICU 73 and earlier

To install ICU4J, simply place the pre-built jar file `icu4j.jar` on your Java `CLASSPATH`. If you need Charset API support please also place `icu4j-charset.jar` on your class path along with `icu4j.jar`.

To build ICU4J, you will need JDK 7 or later (JDK 8 is the reference environment for this release) and the Apache Ant version 1.9 or later. It's recommended to install both the JDK and Ant somewhere _outside_the ICU4J directory. For example, on Linux you might install these in `/usr/local`.

*   Install JDK 8.
*   Install the [Apache Ant](https://ant.apache.org/) 1.9 or later.
*   Set environment variables `JAVA_HOME`, `ANT_HOME` and `PATH`, for example:

~~~
set JAVA_HOME=C:\jdk1.8.0
set ANT_HOME=C:\apache-ant
set PATH=%JAVA_HOME%\bin;%ANT_HOME%\bin;%PATH%
~~~

Once the JDK and Ant are configured, run the desired target defined in `build.xml`. The default target is `jar` which compiles ICU4J library class files and create ICU4J jar files. For example:

~~~
C:\icu4j>ant
Buildfile: C:\icu4j\build.xml

info:
     [echo] ----- Build Environment Information -------------------
     [echo] Java Home:    C:\jdk1.8.0\jre
     [echo] Java Version: 1.8.0_181
     [echo] Ant Home:     C:\apache-ant
     [echo] Ant Version:  Apache Ant(TM) version 1.10.1 compiled on February 2 2017
     [echo] OS:           Windows 10
     [echo] OS Version:   10.0
     [echo] OS Arch:      amd64
     [echo] Host:         ICUDEV
     [echo] -------------------------------------------------------

core:

@compile:
     [echo] build-local:     ../../shared/../../build-local.properties
     [echo] --- java compiler arguments ------------------------
     [echo] source dir:     C:\icu4j\main\classes\core/src
     [echo] output dir:     C:\icu4j\main\classes\core/out/bin
     [echo] bootclasspath:
     [echo] classpath:
     [echo] source:         1.7
     [echo] target:         1.7
     [echo] debug:          on
     [echo] encoding:       UTF-8
     [echo] compiler arg:   -Xlint:all,-deprecation,-dep-ann,-options,-overrides
     [echo] ----------------------------------------------------
    [mkdir] Created dir: C:\icu4j\main\classes\core\out\bin
    [javac] Compiling 470 source files to C:\icu4j\main\classes\core\out\bin
    [javac] Note: Some input files use or override a deprecated API.
    [javac] Note: Recompile with -Xlint:deprecation for details.

compile:

@copy:
     [copy] Copying 24 files to C:\icu4j\main\classes\core\out\bin

set-icuconfig-datapath:

copy-data:
    [unjar] Expanding: C:\icu4j\main\shared\data\icudata.jar into C:\icu4j\main\
classes\core\out\bin
    [unjar] Expanding: C:\icu4j\main\shared\data\icutzdata.jar into C:\icu4j\mai
n\classes\core\out\bin

...
...
...

localespi:

@compile:
     [echo] build-local:     ../../shared/../../build-local.properties
     [echo] --- java compiler arguments ------------------------
     [echo] source dir:     C:\icu4j\main\classes\localespi/src
     [echo] output dir:     C:\icu4j\main\classes\localespi/out/bin
     [echo] bootclasspath:
     [echo] classpath:      C:\icu4j\main\classes\core\out\lib\icu4j-core.jar;C:
\icu4j\main\classes\collate\out\lib\icu4j-collate.jar
     [echo] source:         1.7
     [echo] target:         1.7
     [echo] debug:          on
     [echo] encoding:       UTF-8
     [echo] compiler arg:   -Xlint:all,-deprecation,-dep-ann,-options
     [echo] ----------------------------------------------------
    [mkdir] Created dir: C:\icu4j\main\classes\localespi\out\bin
    [javac] Compiling 22 source files to C:\icu4j\main\classes\localespi\out\bin

compile:

@copy:
     [copy] Copying 11 files to C:\icu4j\main\classes\localespi\out\bin

copy:

@jar:
    [mkdir] Created dir: C:\icu4j\main\classes\localespi\out\lib
     [copy] Copying 1 file to C:\icu4j\main\classes\localespi\out
      [jar] Building jar: C:\icu4j\main\classes\localespi\out\lib\icu4j-localesp
i.jar

jar:

@src-jar:
      [jar] Building jar: C:\icu4j\main\classes\localespi\out\lib\icu4j-localesp
i-src.jar

src-jar:

build:

jar:
     [copy] Copying 1 file to C:\icu4j
     [copy] Copying 1 file to C:\icu4j

BUILD SUCCESSFUL
Total time: 30 seconds
~~~

> :point_right: **Note**:  The above output is an example. The numbers are likely to be different with the current version ICU4J.

The following are some targets that you can provide to `ant`. For more targets run `ant -projecthelp` or see the build.xml file.





| **jar (default)** | Create ICU4J runtime library jar archives (`icu4j.jar`, `icu4j-charset.jar` and `icu4j-localespi.jar`) in the root ICU4J directory. |
| **check**         | Build all ICU4J runtime library classes and corresponding unit test cases, then run the tests.                                      |
| **clean**         | Remove all build output files.                                                                                                      |
| **main**          | Build all ICU4J runtime library sub-components (under the directory `main/classes`).                                                |
| **tests**         | Build all ICU4J unit test sub-components (under the directory `main/tests`) and their dependencies.                                 |
| **tools**         | Build the tools.                                                                                                                    |
| **docs**          | Run javadoc over the ICU4J runtime library files, generating an HTML documentation tree in the subdirectory `doc`.                  |
| **jarDocs**       | Create ICU4J doc jar archive (`icu4jdocs.jar`) containing API reference docs in the root ICU4J directory.                           |
| **jarDemos**      | Create ICU4J demo jar archive (`icu4jdemos.jar`) in the root ICU4J directory.                                                       |

For more information, read the Ant documentation and the **build.xml** file.

> :point_right: **Note**: If you get an OutOfMemoryError when you are running <tt>"ant check"</tt>, you can set the heap size of the jvm by setting the environment variable JVM_OPTIONS to the appropriate java options.

> :point_right: **Note**: **Eclipse users:** See the ICU4J site for information on [how to configure Eclipse](https://icu.unicode.org/setup/eclipse) to build and develop ICU4J on Eclipse IDE.

> :point_right: **Note**: To install and configure ICU4J Locale Service Provider, please refer the user guide page [ICU4J Locale Service Provider](./locale-service-provider).

## Trying Out ICU4J

> :point_right: **Note**: the demos provided with ICU4J are for the most part undocumented. This list can show you where to look, but you'll have to experiment a bit. The demos are **unsupported** and may change or disappear without notice.

The icu4j.jar file contains only the ICU4J runtime library classes, not the demo classes, so unless you build ICU4J there is little to try out.

### Charset

To try out the **Charset** package, build **icu4j.jar** and **icu4j-charset.jar** using the `jar` target. You can use the charsets by placing these files on your classpath.

~~~
java -cp $icu4j_root/icu4j.jar:$icu4j_root/icu4j-charset.jar <your program>
~~~

### Other demos

The other demo programs are **not supported** and exist only to let you experiment with the ICU4J classes. First, build ICU4J using `ant jarDemos`. Then launch the demos as below:

~~~
java -jar $icu4j_root/icu4jdemos.jar
~~~

## ICU4J Resource Information

Starting with release 2.1, ICU4J includes its own resource information which is completely independent of the JRE resource information. (Note, ICU4J 2.8 to 3.4, time zone information depends on the underlying JRE). The ICU4J resource information is equivalent to the information in ICU4C and many resources are, in fact, the same binary files that ICU4C uses.

By default the ICU4J distribution includes all of the standard resource information. It is located under the directory com/ibm/icu/impl/data. Depending on the service, the data is in different locations and in different formats.

> :point_right: **Note**: This will continue to change from release to release, so clients should not depend on the exact organization of the data in ICU4J.

*   The primary **locale data** is under the directory `icudt68b`, as a set of `".res"` files whose names are the locale identifiers. Locale naming is documented the `com.ibm.icu.util.ULocale` class, and the use of these names in searching for resources is documented in `com.ibm.icu.util.UResourceBundle`.
*   The **break iterator data** is under the directory `icudt68b/brkitr`, as a set of `".res"`, `".brk"` and `".dict"` files.
*   The **collation data** is under the directory `icudt68b/coll`, as a set of `".res"` files.
*   The **currency display name data** is under the directory `icudt68b/curr`, as a set of `".res"` files.
*   The **language display name data** is under the directory `icudt68b/lang`, as a set of `".res"` files.
*   The **rule-based number format data** is under the directory `icudt68b/rbnf`, as a set of `".res"` files.
*   The **region display name data** is under the directory `icudt68b/region`, as a set of `".res"` files.
*   The **rule-based transliterator data** is under the directory `icudt68b/translit`, as a set of `".res"` files.
*   The **measurement unit data** is under the directory `icudt68b/unit`, as a set of `".res"` files.
*   The **time zone display name data** is under the directory `icudt68b/zone`, as a set of `".res"` files.
*   The **character property data** and default **unicode collation algorithm (UCA) data** is found under the directory `icudt68b`, as a set of `".icu"` files.
*   The **normalization data** is found under the directory `icudt68b`, as a set of `".nrm"` files.
*   The **character set converter data** is under the directory `icudt68b`, as a set of `".cnv"` files. These files are currently included only in icu-charset.jar.
*   The **time zone rule data** is under the directory `icudt68b`, as `zoneinfo64.res`.
*   The **holiday data** is under the directory `icudt68b`, as a set of `".class"` files, named `"HolidayBundle_"` followed by the locale ID.

Some of the data files alias or otherwise reference data from other data files. One reason for this is because some locale names have changed. For example, `he_IL` used to be `iw_IL`. In order to support both names but not duplicate the data, one of the resource files refers to the other file's data. In other cases, a file may alias a portion of another file's data in order to save space. Currently ICU4J provides no tool for revealing these dependencies.

> :point_right: **Note**: Java's `Locale` class silently converts the language code `"he"` to `"iw"` when you construct the Locale (for versions of Java through Java 5). Thus Java cannot be used to locate resources that use the `"he"` language code. ICU, on the other hand, does not perform this conversion in ULocale, and instead uses aliasing in the locale data to represent the same set of data under different locale ids.

Resource files that use locale ids form a hierarchy, with up to four levels: a root, language, region (country), and variant. Searches for locale data attempt to match as far down the hierarchy as possible, for example, `"he_IL"` will match `he_IL`, but `"he_US"` will match `he` (since there is no `US` variant for he, and `"xx_YY` will match root (the default fallback locale) since there is no `xx` language code in the locale hierarchy. Again, see `java.util.ResourceBundle` for more information.

**Currently ICU4J provides no tool for revealing these dependencies** between data files, so trimming the data directly in the ICU4J project is a hit-or-miss affair. The key point when you remove data is to make sure to remove all dependencies on that data as well. For example, if you remove `he.res`, you need to remove `he_IL.res`, since it is lower in the hierarchy, and you must remove iw.res, since it references `he.res`, and `iw_IL.res`, since it depends on it (and also references `he_IL.res`).

Unfortunately, the jar tool in the JDK provides no way to remove items from a jar file. Thus you have to extract the resources, remove the ones you don't want, and then create a new jar file with the remaining resources. See the jar tool information for how to do this. Before 'rejaring' the files, be sure to thoroughly test your application with the remaining resources, making sure each required resource is present.

### Using additional resource files with ICU4J

> :warning: **Warning: Resource file formats can change across releases of ICU4J!**
>
> The format of ICU4J resources is not part of the API. Clients who develop their own resources for use with ICU4J should be prepared to regenerate them when they move to new releases of ICU4J.


We are still developing ICU4J's resource mechanism. Currently it is not possible to mix icu's new binary `.res` resources with traditional java-style `.class` or `.txt` resources. We might allow for this in a future release, but since the resource data and format is not formally supported, you run the risk of incompatibilities with future releases of ICU4J.

Resource data in ICU4J is checked in to the repository as a jar file containing the resource binaries, `$icu4j_root/main/shared/data/icudata.jar`. This means that inspecting the contents of these resources is difficult. They currently are compiled from ICU4C `.txt` file data. You can view the contents of the ICU4C text resource files to understand the contents of the ICU4J resources.

The files in `icudata.jar` get extracted to `com/ibm/icu/impl/data` in the build output directory by some build targets.

### Building ICU4J Resources from ICU4C

ICU4J data is built by ICU4C tools. Please see [ICU Data Build Tool](../icu_data/buildtool) for the procedures.

#### Generating Data from CLDR

> :point_right: **Note**: This procedure assumes that all 3 sources are present

> :point_right: **Note**: The following example was written using the example of the CLDR 38 release which was released in conjunction with the ICU 68.1 release

1.  Checkout or download CLDR version `release-38`
2.  Checkout ICU with tag `release-68-1`
3.  cd to icu4c/source/data directory
4.  Follow the instructions in [`icu4c/source/data/cldr-icu-readme.txt`](https://github.com/unicode-org/icu/blob/main/icu4c/source/data/cldr-icu-readme.txt)
5.  Rebuild ICU4C with the newly generated data.
6.  Run ICU4C tests to verify that the new data is good.
7.  Build ICU4J data from ICU4C data by following the procedures in [ICU Data Build Tool](../icu_data/buildtool)
8.  cd to `icu4j` dir
9.  Build and test icu4j

## About ICU4J Time Zone

ICU4J library includes the latest time zone data, as of the release date. However, time zone data is frequently updated in response to changes made by local governments around the world. If you need to update the time zone data, please refer the ICU user guide topic [Updating the Time Zone Data](../datetime/timezone#updating-the-time-zone-data).

You can optionally configure ICU4J date and time service classes to use underlying JDK TimeZone implementation (see the ICU4J API reference [TimeZone](https://unicode-org.github.io/icu-docs/apidoc/released/icu4j/com/ibm/icu/util/TimeZone.html) for the details). When this configuration is enabled, ICU's own time zone data won't be used and you have to get time zone data patches from the JRE vendor.

## Where to Find More Information

[https://icu.unicode.org/](https://icu.unicode.org/) is the home page of International Components for Unicode development project

## Submitting Comments, Requesting Features and Reporting Bugs

Your comments are important to making ICU4J successful. We are committed to investigate any bug reports or suggestions, and will use your feedback to help plan future releases.

To submit comments, request features and report bugs, please see [ICU bug database information](https://icu.unicode.org/bugs) or contact us through the [ICU Support mailing list](https://icu.unicode.org/contacts). While we are not able to respond individually to each comment, we do review all comments.

* * *

© 2016 and later: Unicode, Inc. and others.
License & terms of use: [http://www.unicode.org/copyright.html](http://www.unicode.org/copyright.html)
