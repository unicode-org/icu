<!--- © 2020 and later: Unicode, Inc. and others. ---> 
<!--- License & terms of use: http://www.unicode.org/copyright.html --->

# Tools and build scripts for updating data originating from CLDR

## CLDR test data

The ant [build.xml](build.xml) file takes care of copying some CLDR
test data directories to both the ICU4C and ICU4J source trees. To add
more directories to the list, modify the `cldrTestData` fileset.

## cldr-to-icu

The cldr-to-icu directory contains tools to convert from CLDR's XML
data to ICU resource files.

See [cldr-to-icu/README.txt](cldr-to-icu/README.txt) and
[cldr-icu-readme.txt](../../icu4c/source/data/cldr-icu-readme.txt) for
more information.
