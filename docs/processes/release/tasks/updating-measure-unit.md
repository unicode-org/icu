---
layout: default
title: Updating MeasureUnit with new CLDR data
parent: Release & Milestone Tasks
grand_parent: Contributors
nav_order: 120
---

<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Updating MeasureUnit with new CLDR data

{: .no_toc }

## Contents

{: .no_toc .text-delta }

1. TOC
{:toc}

---

This document explains how to update the C++ and Java version of the `MeasureUnit`
class with new CLDR data.

This document applies to ICU 80 and later.
ICU 77 to 79 generated the code with `MeasureUnitGeneratorTest.java`; see this document
in those releases. For older versions see updating-measure-unit-old.md

## Generate the code

The Python tool `tools/cldr/measure_units/generate_measure_units.py` generates the code.
It reads the unit lists that CLDR generates in `common/properties/`
(`units_info.tsv`, `unit_categories.tsv`, and `unit_quantities.tsv`), and it updates
these files in place:

* `icu4j/main/core/src/main/java/com/ibm/icu/util/MeasureUnit.java`
* `icu4c/source/i18n/unicode/measunit.h`
* `icu4c/source/i18n/measunit.cpp`
* `icu4j/main/common_tests/src/test/java/com/ibm/icu/dev/test/format/MeasureUnitCompatibilityTest.java`
* `icu4c/source/test/intltest/measfmttest.cpp`

Point it to the CLDR checkout that the ICU data comes from:

```sh
cd {icuRoot}
python3 tools/cldr/measure_units/generate_measure_units.py --cldr-dir=$CLDR_DIR
```

`--cldr-dir` defaults to `$CLDR_DIR`. With `--check`, the tool only prints a diff.

Then review the changes with `git diff`:

* A new unit gets a Java constant and C++ functions marked `@draft ICU <version>`,
  with the C++ functions inside `#ifndef U_HIDE_DRAFT_API`.
* Types and subtypes are sorted alphabetically and the C++ functions use their array
  indices, so a new unit can change many lines in `measunit.cpp`.
* If the list of constants changed, `MeasureUnitCompatibilityTest.java` and
  `measfmttest.cpp` get a `TestCompatible<version>()` test for the current ICU version.

The tool keeps the API tags (`@stable`, `@draft`, `@deprecated`) that are in the files.
To make draft APIs stable, change their tags in the files, as for any other API.

If CLDR no longer has a unit that has a constant, the tool stops without changing any
file, because that would remove API. Keep the constant with an entry in `ALIASES` or
`NAME_OVERRIDES` in the tool. See the tool's
[README](https://github.com/unicode-org/icu/blob/main/tools/cldr/measure_units/README.md)
for details.

## Run tests for both `icu4c` and `icu4j`

The tests that check the generated code most directly are `MeasureUnitTest` and
`MeasureUnitCompatibilityTest` in ICU4J, and `format/MeasureFormatTest` in ICU4C's
`intltest`.

## Updating `units.txt` and `unitConstants`

The standard `ldml2icu` process is used to update ICU's resource files (see
[`cldr-icu-readme.txt`](https://github.com/unicode-org/icu/blob/main/icu4c/source/data/cldr-icu-readme.txt)).
CLDR's units.xml defines conversion rates in terms of some constants defined in
`unitConstants`.

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
