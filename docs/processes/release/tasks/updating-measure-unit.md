---
layout: default
title: Updating MeasureUnit with new CLDR data
parent: Release & Milestone Tasks
grand_parent: Contributors
nav_order: 120
---

<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: https://www.unicode.org/copyright.html
-->

# Updating MeasureUnit with new CLDR data

{: .no_toc }

## Contents

{: .no_toc .text-delta }

1. TOC
{:toc}

---

This document explains how to update the C++ and Java `MeasureUnit` built-in unit constants and
compatibility tests after integrating new CLDR data.

For ICU 76 and older, see `updating-measure-unit-old.md`.

## Running the generator

`MeasureUnitGeneratorTest` reads the unit data from ICU4J's resource bundles and updates all five
generated files in place:

* `icu4j/main/core/src/main/java/com/ibm/icu/util/MeasureUnit.java`
* `icu4c/source/i18n/unicode/measunit.h`
* `icu4c/source/i18n/measunit.cpp`
* `icu4j/main/common_tests/src/test/java/com/ibm/icu/dev/test/format/MeasureUnitCompatibilityTest.java`
* `icu4c/source/test/intltest/measfmttest.cpp`

Existing `@stable`, `@draft`, and `@deprecated` tags in `MeasureUnit.java` and `measunit.h` are
kept automatically, and newly added units are tagged `@draft ICU <major>`. A new
`TestCompatible<major>()` test is added (or updated) in `MeasureUnitCompatibilityTest.java` and
`measfmttest.cpp` only when the list of constants changes.

1. With **Maven** (command line):
   - Change folder to `{icuRoot}/icu4j`
   - Build the updated ICU4J jars so the generator sees the new resource bundles:
     `mvn install -DskipTests -DskipITs`
   - Update the generated files in place:
     `mvn test -Dtest=MeasureUnitGeneratorTest -DgenerateMeasureUnitUpdate -pl main/common_tests`

2. Within **Eclipse**:
   - Open `MeasureUnitGeneratorTest.java`, find `generateUnitTestsUpdate`, and run it once as a
     JUnit Test to create a Run Configuration.
   - Open **Run** -> **Run Configurations**, select `MeasureUnitGeneratorTest.generateUnitTestsUpdate`,
     go to the **Arguments** tab, add `-DgenerateMeasureUnitUpdate` to **VM arguments**, and run it.

The output reports which of the five files were updated or unchanged:

```text
Updated:   icu4j/main/core/src/main/java/com/ibm/icu/util/MeasureUnit.java
Updated:   icu4c/source/i18n/unicode/measunit.h
Updated:   icu4c/source/i18n/measunit.cpp
Updated:   icu4j/main/common_tests/src/test/java/com/ibm/icu/dev/test/format/MeasureUnitCompatibilityTest.java
Updated:   icu4c/source/test/intltest/measfmttest.cpp
MeasureUnit generation finished (5 file(s) updated).
```

To check whether the generated files match the current data without writing changes, pass
`-DcheckMeasureUnitUpdate` instead of `-DgenerateMeasureUnitUpdate`.

## Handling renamed or removed units

If CLDR renames or removes a unit that already has a constant in `MeasureUnit.java` or
`measunit.h`, the generator stops with an error before modifying any file:

* **Renamed unit with stable API:** add an entry to `NAME_OVERRIDES` (if keeping only the old name)
  or `ALIASES` (if adding a new constant while keeping the old constant as an alias) in
  `icu4j/main/common_tests/src/test/java/com/ibm/icu/dev/test/format/measureunitgen/BuiltInUnits.java`,
  then rerun the generator.
* **Removed or renamed draft unit:** if the old constant was still `@draft` and should be removed,
  delete its declaration from `MeasureUnit.java` and `measunit.h` first, then rerun the generator.

## Run tests for both `icu4c` and `icu4j`

After updating the files, run the ICU4J and ICU4C unit tests (including `MeasureUnitTest`,
`MeasureUnitCompatibilityTest`, and `intltest format/MeasureFormatTest`) and `mvn spotless:check`.

## Updating `units.txt` and `unitConstants`

The standard `ldml2icu` process is used to update ICU's resource files (see
[`cldr-icu-readme.txt`](https://github.com/unicode-org/icu/blob/main/icu4c/source/data/cldr-icu-readme.txt)).
CLDR's `units.xml` defines conversion rates in terms of constants defined in `unitConstants`.

For efficiency and simplicity, ICU does not read `unitConstants` from the
resource file. If any new constants are added, some code changes would be
needed. This would be caught by `testUnitConstantFreshness` unit test in
`units_test.cpp`.

They are hard-coded:

* Java: `UnitConverter.java` has the constant names in
  `UnitConverter.Factor.addEntity()` and constant values in
  `UnitConverter.Factor.getConversionRate()`.
* C++: `units_converter.cpp` has the constant names in
  `addSingleFactorConstant()`, with the constant values in `double
  constantsValues[]` in the `units_converter.h` header file.
