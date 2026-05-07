<!--
© 2019 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Basic instructions for running the LdmlConverter via Maven

> Note: While this document provides useful background information about the
  `LdmlConverter`, the actual complete process for integrating CLDR data to ICU
  is described in the document `../../../docs/processes/cldr-icu.md` which is
  best viewed as
  [CLDR-ICU integration](https://unicode-org.github.io/icu/processes/cldr-icu.html)

## TLDR

* Define the `ICU_DIR`, `CLDR_DIR`, and `CLDR_DATA_DIR` environment variables, or  (see below)
* Check / update versions
* Build ICU4J:
  ```sh
  cd "$ICU_DIR"
  mvn clean install -f icu4j -DskipTests -DskipITs
  ```
* Build the `cldr-code` library from the `cldr` repo:
  ```sh
  cd "$CLDR_DIR"
  mvn clean install -pl :cldr-all,:cldr-code -DskipTests -DskipITs
  ```
* Build the conversion tool:
  ```sh
  cd "$ICU_DIR/tools/cldr/cldr-to-icu/"
  mvn clean package -DskipTests -DskipITs
  ```
* Run the conversion tool:
  ```sh
  java -jar target/cldr-to-icu-1.0-SNAPSHOT-jar-with-dependencies.jar
  ```

## Requirements

* A CLDR release for supplying CLDR data and the CLDR API.
* JDK 11+
* The Maven build tool

## Important directories

| Directory       | Description                                                                                                                                                                                                                          |
|-----------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ICU_DIR`    | Path to root of ICU directory, below which are (e.g.) the `icu4c/`, `icu4j/` and `tools/` directories.                                                                                                                             |
| `CLDR_DIR`      | This is the path to the to root of standard CLDR sources, below which are the `common/` and `tools/` directories.                                                                                                                    |
| `CLDR_DATA_DIR` | The top-level directory for the CLDR production data (typically the "production" directory in the staging repository). Usually generated locally or obtained from:  https://github.com/unicode-org/cldr-staging/tree/main/production |

In Posix systems, it's best to set these as exported shell variables, and any
following instructions assume they have been set accordingly:

```sh
export TOOLS_ROOT=/path/to/icu/tools
export CLDR_DIR=/path/to/cldr
export CLDR_DATA_DIR=/path/to/cldr-staging/production
```

Note that you should not attempt to use data from the CLDR project directory
(where the CLDR API code exists) for conversion into ICU data. The process now
relies on a pre-processing step, and the CLDR data must come from the separate
"staging" repository (i.e. https://github.com/unicode-org/cldr-staging) or be
pre-processed locally into a different directory.

:point_right: **Note**: the 3 folders can also be overridden:

* with Java properties (e.g. `-DCLDR_DIR=/foo/bar`)
* from the command line when invoking the tool (the `icuDir`, `cldrDir`, and `cldrDataDir` options)

## Initial Setup

This project relies on the Maven build tool for managing dependencies, so it will need to be installed. On a Debian
based system, this should be as simple as:

```sh
sudo apt-get install maven
```

## Check / update versions

### Real versions

**ICU version (`real_icu_ver`):**
```sh
mvn help:evaluate -Dexpression=project.version -q -DforceStdout -f $ICU_DIR/icu4j
```

**CLDR Library version (`real_cldr_ver`):**
```sh
mvn help:evaluate -Dexpression=project.version -q -DforceStdout -f $CLDR_DIR/tools
```

### Dependency versions

**ICU version used by the cldr conversion tool:** \
⚠️ **Warning:** Must be the same as `real_icu_ver`
```sh
mvn help:evaluate -Dexpression=icu4j.version -q -DforceStdout -f $ICU_DIR/tools/cldr/cldr-to-icu
```

**CLDR library version used by the cldr conversion tool:** \
⚠️ **Warning:** Must be the same as `real_cldr_ver`
```sh
mvn help:evaluate -Dexpression=cldr-code.version -q -DforceStdout -f $ICU_DIR/tools/cldr/cldr-to-icu
```

**ICU version used by the cldr library:** \
⚠️ **Warning:** Must be the same as `real_icu_ver`
```sh
mvn help:evaluate -Dexpression=icu4j.version -q -DforceStdout -f $CLDR_DIR/tools
```

### TLDR (Quick update versions without checking)

```sh
# Get real versions
real_icu_ver=`mvn help:evaluate -Dexpression=project.version -q -DforceStdout -f $ICU_DIR/icu4j`
echo $real_icu_ver
real_cldr_ver=`mvn help:evaluate -Dexpression=project.version -q -DforceStdout -f $CLDR_DIR/tools`
echo $real_cldr_ver
# Set dependency versions
mvn versions:set-property -DgenerateBackupPoms=false -Dproperty=icu4j.version     -DnewVersion=$real_icu_ver  -f $ICU_DIR/tools/cldr/cldr-to-icu
mvn versions:set-property -DgenerateBackupPoms=false -Dproperty=cldr-code.version -DnewVersion=$real_cldr_ver -f $ICU_DIR/tools/cldr/cldr-to-icu
mvn versions:set-property -DgenerateBackupPoms=false -Dproperty=icu4j.version     -DnewVersion=$real_icu_ver  -f $CLDR_DIR/tools
```

## Build everything

You must also build and install an additional CLDR library in the local Maven repository.

Since that depends on ICU4J, you need to build and install that first.

Lastly, build the conversion tool

```sh
# Build ICU4J
cd "$ICU_DIR"
mvn clean install -f icu4j -DskipTests -DskipITs
# Build the CLDR library
cd "$CLDR_DIR"
mvn clean install -pl :cldr-all,:cldr-code -DskipTests -DskipITs
# Build the conversion tool
cd "$ICU_DIR/tools/cldr/cldr-to-icu/"
mvn clean package -DskipTests -DskipITs
```

## Generating all ICU data and source code

Run the conversion tool:
```sh
cd "$ICU_DIR/tools/cldr/cldr-to-icu/"
java -jar target/cldr-to-icu-1.0-SNAPSHOT-jar-with-dependencies.jar
```

You can run it with `--help` for all the options supported.

## Other Examples

* Outputting a subset of the supplemental data into a specified directory:
  ```sh
  java -jar target/cldr-to-icu-1.0-SNAPSHOT-jar-with-dependencies.jar --outDir=/tmp/cldr --outputTypes=plurals,dayPeriods --dontGenCode
  ```
  Note: Output types can be listed with mixedCase, lower_underscore or UPPER_UNDERSCORE.
  Pass `-DoutputTypes=help` to see the full list.


* Outputting only a subset of locale IDs (and all the supplemental data):
  ```sh
  java -jar target/cldr-to-icu-1.0-SNAPSHOT-jar-with-dependencies.jar --outDir=/tmp/cldr --outputTypes=plurals,dayPeriods --dontGenCode

  java -jar target/cldr-to-icu-1.0-SNAPSHOT-jar-with-dependencies.jar --outDir=/tmp/cldr --localeIdFilter='(zh|yue).*' --dontGenCode
  ```

* Overriding the default CLDR version string (which normally matches the CLDR library code):
  ```sh
  java -jar target/cldr-to-icu-1.0-SNAPSHOT-jar-with-dependencies.jar --cldrVersion="36.1"
  ```

### Using `alt="ascii"` CLDR alternate values from the CLDR XML

CLDR provides alternate values in addition to the default values for locale data.

For example, some locales have time formats using U+202F NARROW NO-BREAK SPACE (`NNBSP`) between the hours/minutes/seconds and the day periods.
In order to provide the equivalent time formats that use the ASCII space
U+0020 SPACE,
the alternate values have the extra attribute `alt="ascii"`.

Follw these steps to generate ICU data using the ASCII versions of locale data:

1. First, edit the `config.xml` file where it mentions `ALTERNATE VALUES`
with the correctly annotated source path, target path, and locales list
as follows:

    ```diff
    @@ -384,6 +399,20 @@
              <!-- ALTERNATE VALUES -->
              <!-- The following elements configure alternate values for some special case paths.
                   The target path will only be replaced if both it, and the source path, exist in
                   the CLDR data (paths will not be modified if only the source path exists).
                   Since the paths must represent the same semantic type of data, they must be in the
                   same "namespace" (same element names) and must not contain value attributes. Thus
                   they can only differ by distinguishing attributes (either added or modified).
                   This feature is typically used to select alternate translations (e.g. short forms)
                   for certain paths. -->
               <!-- <altPath target="//path/to/value[@attr='foo']"
                             source="//path/to/value[@attr='bar']"
                             locales="xx,yy_ZZ"/> -->
    +            <altPath target="//ldml/dates/calendars/calendar[@type='gregorian']/dateTimeFormats/availableFormats/dateFormatItem[@id='Ehm']"
    +                     source="//ldml/dates/calendars/calendar[@type='gregorian']/dateTimeFormats/availableFormats/dateFormatItem[@id='Ehm'][@alt='ascii']"/>
    +            <altPath target="//ldml/dates/calendars/calendar[@type='gregorian']/dateTimeFormats/availableFormats/dateFormatItem[@id='Ehms']"
    +                     source="//ldml/dates/calendars/calendar[@type='gregorian']/dateTimeFormats/availableFormats/dateFormatItem[@id='Ehms'][@alt='ascii']"/>
    +            <altPath target="//ldml/dates/calendars/calendar[@type='gregorian']/dateTimeFormats/availableFormats/dateFormatItem[@id='h']"
    +                     source="//ldml/dates/calendars/calendar[@type='gregorian']/dateTimeFormats/availableFormats/dateFormatItem[@id='h'][@alt='ascii']"/>
    +            <altPath target="//ldml/dates/calendars/calendar[@type='gregorian']/dateTimeFormats/availableFormats/dateFormatItem[@id='hm']"
    +                     source="//ldml/dates/calendars/calendar[@type='gregorian']/dateTimeFormats/availableFormats/dateFormatItem[@id='hm'][@alt='ascii']"/>
    +            <altPath target="//ldml/dates/calendars/calendar[@type='gregorian']/dateTimeFormats/availableFormats/dateFormatItem[@id='hms']"
    +                     source="//ldml/dates/calendars/calendar[@type='gregorian']/dateTimeFormats/availableFormats/dateFormatItem[@id='hms'][@alt='ascii']"/>
    +            <altPath target="//ldml/dates/calendars/calendar[@type='gregorian']/dateTimeFormats/availableFormats/dateFormatItem[@id='hmsv']"
    +                     source="//ldml/dates/calendars/calendar[@type='gregorian']/dateTimeFormats/availableFormats/dateFormatItem[@id='hmsv'][@alt='ascii']"/>
    +            <altPath target="//ldml/dates/calendars/calendar[@type='gregorian']/dateTimeFormats/availableFormats/dateFormatItem[@id='hmv']"
    +                     source="//ldml/dates/calendars/calendar[@type='gregorian']/dateTimeFormats/availableFormats/dateFormatItem[@id='hmv'][@alt='ascii']"/>
    +            <altPath target="//ldml/dates/calendars/calendar[@type='gregorian']/timeFormats/timeFormatLength[@type='full']/timeFormat[@type='standard']/pattern[@type='standard']"
    +                     source="//ldml/dates/calendars/calendar[@type='gregorian']/timeFormats/timeFormatLength[@type='full']/timeFormat[@type='standard']/pattern[@alt='ascii'][@type='standard']"/>
    +            <altPath target="//ldml/dates/calendars/calendar[@type='gregorian']/timeFormats/timeFormatLength[@type='long']/timeFormat[@type='standard']/pattern[@type='standard']"
    +                     source="//ldml/dates/calendars/calendar[@type='gregorian']/timeFormats/timeFormatLength[@type='long']/timeFormat[@type='standard']/pattern[@alt='ascii'][@type='standard']"/>
    +            <altPath target="//ldml/dates/calendars/calendar[@type='gregorian']/timeFormats/timeFormatLength[@type='medium']/timeFormat[@type='standard']/pattern[@type='standard']"
    +                     source="//ldml/dates/calendars/calendar[@type='gregorian']/timeFormats/timeFormatLength[@type='medium']/timeFormat[@type='standard']/pattern[@alt='ascii'][@type='standard']"/>
    +            <altPath target="//ldml/dates/calendars/calendar[@type='gregorian']/timeFormats/timeFormatLength[@type='short']/timeFormat[@type='standard']/pattern[@type='standard']"
    +                     source="//ldml/dates/calendars/calendar[@type='gregorian']/timeFormats/timeFormatLength[@type='short']/timeFormat[@type='standard']/pattern[@alt='ascii'][@type='standard']"/>
    +            <altPath target="//ldml/dates/calendars/calendar[@type='generic']/dateTimeFormats/availableFormats/dateFormatItem[@id='Ehm']"
    +                     source="//ldml/dates/calendars/calendar[@type='generic']/dateTimeFormats/availableFormats/dateFormatItem[@id='Ehm'][@alt='ascii']"/>
    +            <altPath target="//ldml/dates/calendars/calendar[@type='generic']/dateTimeFormats/availableFormats/dateFormatItem[@id='Ehms']"
    +                     source="//ldml/dates/calendars/calendar[@type='generic']/dateTimeFormats/availableFormats/dateFormatItem[@id='Ehms'][@alt='ascii']"/>
    +            <altPath target="//ldml/dates/calendars/calendar[@type='generic']/dateTimeFormats/availableFormats/dateFormatItem[@id='h']"
    +                     source="//ldml/dates/calendars/calendar[@type='generic']/dateTimeFormats/availableFormats/dateFormatItem[@id='h'][@alt='ascii']"/>
    +            <altPath target="//ldml/dates/calendars/calendar[@type='generic']/dateTimeFormats/availableFormats/dateFormatItem[@id='hm']"
    +                     source="//ldml/dates/calendars/calendar[@type='generic']/dateTimeFormats/availableFormats/dateFormatItem[@id='hm'][@alt='ascii']"/>
    +            <altPath target="//ldml/dates/calendars/calendar[@type='generic']/dateTimeFormats/availableFormats/dateFormatItem[@id='hms']"
    +                     source="//ldml/dates/calendars/calendar[@type='generic']/dateTimeFormats/availableFormats/dateFormatItem[@id='hms'][@alt='ascii']"/>
    ```

1. Then run the generator:

    ```sh
    java -jar target/cldr-to-icu-1.0-SNAPSHOT-jar-with-dependencies.jar <options>
    ```

## Config syntax details

Note: some elements have an implicit default attributes associated with them, according to [`ldml.dtd`](https://github.com/unicode-org/icu/blob/main/icu4c/source/data/dtd/cldr/common/dtd/ldml.dtd).
For example, for the `timeFormat` element,
the following excerpt of the DTD schema indicates that there is a default value `"standard"` for the `type` attribute:

```
<!ELEMENT timeFormat ... >
<!ATTLIST timeFormat type NMTOKEN "standard" >
```

See `config.xml` for documentation of all options and additional customization.

## Running unit tests (CURRENTLY FAILING)

```sh
mvn test -DCLDR_DIR="$CLDR_DATA_DIR"
```

## Importing and running from an IDE

This project should be easy to import into an IDE which supports Maven development, such
as IntelliJ or Eclipse. It uses a local Maven repository directory for the unpublished
CLDR libraries (which are included in the project), but otherwise gets all dependencies
via Maven's public repositories.

But before importing and running it you still need to build the ICU4J and the CLDR library (see above).
