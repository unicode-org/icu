<!--
© 2026 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# MeasureUnit code generator

`generate_measure_units.py` updates the built-in `MeasureUnit` constants of
ICU4C and ICU4J (such as `MeasureUnit.METER` and `MeasureUnit::getMeter()`)
from the unit lists that CLDR generates. It edits the files in place, so
nobody has to copy generated code by hand.

It needs Python 3.9 or later and nothing outside the standard library.

## Usage

```sh
python3 tools/cldr/measure_units/generate_measure_units.py --cldr-dir=<cldr>
```

`--cldr-dir` defaults to `$CLDR_DIR`. With `--check`, the tool writes
nothing: it prints a diff and exits with 1 if a file would change.

Run the tests of the tool with:

```sh
python3 tools/cldr/measure_units/test_generate_measure_units.py
```

## What it reads

* CLDR `common/properties/units_info.tsv`: the units. Each current unit (not
  `deprecated`) gets a constant. Its `category` is the ICU type, and its
  `unit` is the ICU subtype. `unit_categories.tsv` and
  `unit_quantities.tsv` are read to check that the three files agree.
* ICU `icu4c/source/data/misc/currencyNumericCodes.txt`: the currency codes in
  the C++ arrays. This data comes from ISO 4217, not from CLDR, and ICU4J's
  `MeasureUnit.getAvailable("currency")` returns the same codes.
* ICU `icu4c/source/common/unicode/uvernum.h`: the ICU major version, for new
  APIs and for the name of a new compatibility test.
* The generated files themselves, for the API tags (`@stable`, `@draft`,
  `@deprecated`) of the constants that already exist.

## What it writes

* `MeasureUnit.java`, `measunit.h`, and `measunit.cpp`: all the code between
  the `// Start generated` and `// End generated` lines. The Java code is
  formatted the way `mvn spotless:apply` would format it.
* `MeasureUnitCompatibilityTest.java` and `measfmttest.cpp`: when the list of
  constants changed since the newest `TestCompatible<N>()` test, a test for
  the current ICU major version. If that test exists already, the tool
  replaces it.

Types and subtypes are sorted alphabetically, and the C++ functions use their
array indices, so a new unit can change many lines in `measunit.cpp`.

## Names and API tags

Constant names are API, so they never change. The names come from the CLDR
unit ID (`part-per-1e6` becomes `PART_PER_1E6` and `PartPer1E6`), except:

* `NAME_OVERRIDES`: units that keep an older or more specific name, such as
  `temperature-generic`, which is `GENERIC_TEMPERATURE`.
* `ALIASES`: old constants that ICU keeps after CLDR renamed a unit, such as
  `METRIC_TON` for `tonne`.

A constant that is already in the files keeps its API tags. A new constant
gets `@draft ICU <current major version>`, and its C++ functions go inside
`#ifndef U_HIDE_DRAFT_API`. To make a draft API stable, change its tags in the
files; the tool keeps them.

The tool stops with an error, and changes nothing, when CLDR no longer has a
unit that has a constant. That would remove API, so a person has to decide
what to do, for example add an alias.
