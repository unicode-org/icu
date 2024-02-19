<!--
© 2019 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Basic instructions for running the LdmlConverter via Maven

> Note: While this document provides useful background information about the
  LdmlConverter, the actual complete process for integrating CLDR data to ICU
  is described in the document `../../../docs/processes/cldr-icu.md` which is
  best viewed as
  [CLDR-ICU integration](https://unicode-org.github.io/icu/processes/cldr-icu.html)

## Requirements

* A CLDR release for supplying CLDR data and the CLDR API.
* The Maven build tool
* The Ant build tool (using JDK 11+)

## Important directories

| Directory       | Description                                                                                                                                                                                                                          |
|-----------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `TOOLS_ROOT`    | Path to root of ICU tools directory, below which are (e.g.) the `cldr/` and `unicodetools/` directories.                                                                                                                             |
| `CLDR_DIR`      | This is the path to the to root of standard CLDR sources, below which are the `common/` and `tools/` directories.                                                                                                                    |
| `CLDR_DATA_DIR` | The top-level directory for the CLDR production data (typically the "production" directory in the staging repository). Usually generated locally or obtained from:  https://github.com/unicode-org/cldr-staging/tree/main/production |

In Posix systems, it's best to set these as exported shell variables, and any
following instructions assume they have been set accordingly:

```
$ export TOOLS_ROOT=/path/to/icu/tools
$ export CLDR_DIR=/path/to/cldr
$ export CLDR_DATA_DIR=/path/to/cldr-staging/production
```

Note that you should not attempt to use data from the CLDR project directory
(where the CLDR API code exists) for conversion into ICU data. The process now
relies on a pre-processing step, and the CLDR data must come from the separate
"staging" repository (i.e. https://github.com/unicode-org/cldr-staging) or be
pre-processed locally into a different directory.


## Initial Setup

This project relies on the Maven build tool for managing dependencies and uses
Ant for configuration purposes, so both will need to be installed. On a Debian
based system, this should be as simple as:

```
$ sudo apt-get install maven ant
```

You must also install an additional CLDR JAR file the local Maven repository at
`$TOOLS_ROOT/cldr/lib` (see the `README.txt` in that directory for more
information).

```
$ cd "$TOOLS_ROOT/cldr/lib"
$ ./install-cldr-jars.sh "$CLDR_DIR"
```

## Generating all ICU data and source code

```
$ cd "$TOOLS_ROOT/cldr/cldr-to-icu"
$ ant -f build-icu-data.xml
```

## Other Examples

* Outputting a subset of the supplemental data into a specified directory:
  ```
  $ ant -f build-icu-data.xml -DoutDir=/tmp/cldr -DoutputTypes=plurals,dayPeriods -DdontGenCode=true
  ```
  Note: Output types can be listed with mixedCase, lower_underscore or UPPER_UNDERSCORE.
  Pass `-DoutputTypes=help` to see the full list.


* Outputting only a subset of locale IDs (and all the supplemental data):
  ```
  $ ant -f build-icu-data.xml -DoutDir=/tmp/cldr -DlocaleIdFilter='(zh|yue).*' -DdontGenCode=true
  ```

* Overriding the default CLDR version string (which normally matches the CLDR library code):
  ```
  $ ant -f build-icu-data.xml -DcldrVersion="36.1"
  ```

* Using alternate CLDR values (ex: use `alt="ascii"` values from the CLDR XML):

  First, edit the `build-icu-data.xml` file where it mentions `ALTERNATE VALUES`
  with the correctly annotated source path, target path, and locales list:
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
  +                     source="//ldml/dates/calendars/calendar[@type='gregorian']/dateTimeFormats/availableFormats/dateFormatItem[@id='Ehm'][@alt='ascii']"
  +                     locales="en"/>
  +            <altPath target="//ldml/dates/calendars/calendar[@type='gregorian']/dateTimeFormats/availableFormats/dateFormatItem[@id='Ehms']"
  +                     source="//ldml/dates/calendars/calendar[@type='gregorian']/dateTimeFormats/availableFormats/dateFormatItem[@id='Ehms'][@alt='ascii']"
  +                     locales="en"/>
  +            <altPath target="//ldml/dates/calendars/calendar[@type='gregorian']/dateTimeFormats/availableFormats/dateFormatItem[@id='h']"
  +                     source="//ldml/dates/calendars/calendar[@type='gregorian']/dateTimeFormats/availableFormats/dateFormatItem[@id='h'][@alt='ascii']"
  +                     locales="en"/>
  +            <altPath target="//ldml/dates/calendars/calendar[@type='gregorian']/dateTimeFormats/availableFormats/dateFormatItem[@id='hm']"
  +                     source="//ldml/dates/calendars/calendar[@type='gregorian']/dateTimeFormats/availableFormats/dateFormatItem[@id='hm'][@alt='ascii']"
  +                     locales="en"/>
  +            <altPath target="//ldml/dates/calendars/calendar[@type='gregorian']/dateTimeFormats/availableFormats/dateFormatItem[@id='hms']"
  +                     source="//ldml/dates/calendars/calendar[@type='gregorian']/dateTimeFormats/availableFormats/dateFormatItem[@id='hms'][@alt='ascii']"
  +                     locales="en"/>
  +            <altPath target="//ldml/dates/calendars/calendar[@type='gregorian']/dateTimeFormats/availableFormats/dateFormatItem[@id='hmsv']"
  +                     source="//ldml/dates/calendars/calendar[@type='gregorian']/dateTimeFormats/availableFormats/dateFormatItem[@id='hmsv'][@alt='ascii']"
  +                     locales="en"/>
  +            <altPath target="//ldml/dates/calendars/calendar[@type='gregorian']/dateTimeFormats/availableFormats/dateFormatItem[@id='hmv']"
  +                     source="//ldml/dates/calendars/calendar[@type='gregorian']/dateTimeFormats/availableFormats/dateFormatItem[@id='hmv'][@alt='ascii']"
  +                     locales="en"/>
  +            <altPath target="//ldml/dates/calendars/calendar[@type='gregorian']/timeFormats/timeFormatLength[@type='full']/timeFormat[@type='standard']/pattern[@type='standard']"
  +                     source="//ldml/dates/calendars/calendar[@type='gregorian']/timeFormats/timeFormatLength[@type='full']/timeFormat[@type='standard']/pattern[@alt='ascii'][@type='standard']"
  +                     locales="en"/>
  +            <altPath target="//ldml/dates/calendars/calendar[@type='gregorian']/timeFormats/timeFormatLength[@type='long']/timeFormat[@type='standard']/pattern[@type='standard']"
  +                     source="//ldml/dates/calendars/calendar[@type='gregorian']/timeFormats/timeFormatLength[@type='long']/timeFormat[@type='standard']/pattern[@alt='ascii'][@type='standard']"
  +                     locales="en"/>
  +            <altPath target="//ldml/dates/calendars/calendar[@type='gregorian']/timeFormats/timeFormatLength[@type='medium']/timeFormat[@type='standard']/pattern[@type='standard']"
  +                     source="//ldml/dates/calendars/calendar[@type='gregorian']/timeFormats/timeFormatLength[@type='medium']/timeFormat[@type='standard']/pattern[@alt='ascii'][@type='standard']"
  +                     locales="en"/>
  +            <altPath target="//ldml/dates/calendars/calendar[@type='gregorian']/timeFormats/timeFormatLength[@type='short']/timeFormat[@type='standard']/pattern[@type='standard']"
  +                     source="//ldml/dates/calendars/calendar[@type='gregorian']/timeFormats/timeFormatLength[@type='short']/timeFormat[@type='standard']/pattern[@alt='ascii'][@type='standard']"
  +                     locales="en"/>
  +            <altPath target="//ldml/dates/calendars/calendar[@type='generic']/dateTimeFormats/availableFormats/dateFormatItem[@id='Ehm']"
  +                     source="//ldml/dates/calendars/calendar[@type='generic']/dateTimeFormats/availableFormats/dateFormatItem[@id='Ehm'][@alt='ascii']"
  +                     locales="en"/>
  +            <altPath target="//ldml/dates/calendars/calendar[@type='generic']/dateTimeFormats/availableFormats/dateFormatItem[@id='Ehms']"
  +                     source="//ldml/dates/calendars/calendar[@type='generic']/dateTimeFormats/availableFormats/dateFormatItem[@id='Ehms'][@alt='ascii']"
  +                     locales="en"/>
  +            <altPath target="//ldml/dates/calendars/calendar[@type='generic']/dateTimeFormats/availableFormats/dateFormatItem[@id='h']"
  +                     source="//ldml/dates/calendars/calendar[@type='generic']/dateTimeFormats/availableFormats/dateFormatItem[@id='h'][@alt='ascii']"
  +                     locales="en"/>
  +            <altPath target="//ldml/dates/calendars/calendar[@type='generic']/dateTimeFormats/availableFormats/dateFormatItem[@id='hm']"
  +                     source="//ldml/dates/calendars/calendar[@type='generic']/dateTimeFormats/availableFormats/dateFormatItem[@id='hm'][@alt='ascii']"
  +                     locales="en"/>
  +            <altPath target="//ldml/dates/calendars/calendar[@type='generic']/dateTimeFormats/availableFormats/dateFormatItem[@id='hms']"
  +                     source="//ldml/dates/calendars/calendar[@type='generic']/dateTimeFormats/availableFormats/dateFormatItem[@id='hms'][@alt='ascii']"
  +                     locales="en"/>
  ```
  Then run the generator:
  ```
  $ ant -f build-icu-data.xml <options>
  ```

See build-icu-data.xml for documentation of all options and additional customization.


## Running unit tests

```
$ mvn test -DCLDR_DIR="$CLDR_DATA_DIR"
```


## Importing and running from an IDE

This project should be easy to import into an IDE which supports Maven development, such
as IntelliJ or Eclipse. It uses a local Maven repository directory for the unpublished
CLDR libraries (which are included in the project), but otherwise gets all dependencies
via Maven's public repositories.
