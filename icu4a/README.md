# International Components for Unicode for Assembly (ICU4A)

## Introduction

Today's software market is a global one in which it is desirable to develop and maintain one application (single source/single binary) that supports a wide variety of languages. The International Components for Unicode (ICU) libraries provide robust and full-featured Unicode services on a wide variety of 6502-based platforms to help this design goal. The ICU libraries provide support for:

* The latest version of the Unicode standard
* Character set conversions with support for over 220 codepages
* Locale data for more than 300 locales
* Language sensitive text collation (sorting) and searching based on the Unicode Collation Algorithm (=ISO 14651)
* Regular expression matching and Unicode sets
* Transformations for normalization, upper/lowercase, script transliterations (50+ pairs)
* Resource bundles for storing and accessing localized information
* Date/Number/Message formatting and parsing of culture specific input/output formats
* Calendar specific date and time manipulation
* Text boundary analysis for finding characters, word and sentence boundaries

ICU has sister projects ICU4J that extends the internationalization capabilities of Java to a level similar to ICU, as well as ICU4C.


## Known Issues

* Only the 6502 architecture is currently supported.
* Limited API coverage

## Using

* uses the [dasm](http://dasm-dillon.sourceforge.net) assembler
* from the command line, `make` will invoke the assembler on the test suite

## Calling convention

* Functions are noted with @draft, @stable etc in comments
* JavaDoc does not currently read assembly fils.
* Input buffers, where noted, use the constant `UBUF` (which may be relocated).

## API Docs

### `u_strlen`

```asm
;; Determine the length of an array of UChar.
;; @param UBUF The array of UChars, NULL (U+0000) terminated.
;; @return A The number of UChars in UBUF, minus the terminator.
;; @modifies A, X, and status register
;; @draft ICU 59
u_strlen SUBROUTINE
```

## Testing

* Not tested on actual hardware… yet

### Testing on an emulator

Instructions for the `AINTLTST` test suite:

* run `make` (or just use `aintltst.hex` included in the source repo.)

* open up `aintltst.hex` - it is a hex stream starting at org $0100

* fire up http://e-tradition.net/bytes/6502/index.html

* enter `0100` for the Start Address field
* paste the `aintltst.hex` contents into the memory area
* click `load memory`
* now click the green letters `PC` at top… change to `0200` (the address of `aintltst`)
* click `continuous run` (or single step…)
* when BRK is hit, accumulator will contain `1A` (as the input string is 26 code units.)
* Currently the test must be run manually. In the future, we will look for a Jenkins plugin to interface with an RS-232-C interface.

### LICENSE

© 2016 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
