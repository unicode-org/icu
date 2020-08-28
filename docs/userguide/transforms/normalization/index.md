---
layout: default
title: Normalization
nav_order: 3
parent: Transforms
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Normalization
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Overview

Normalization is used to convert text to a unique, equivalent form. Software can
normalize equivalent strings to one particular sequence, such as normalizing
composite character sequences into pre-composed characters.

Normalization allows for easier sorting and searching of text. The ICU
normalization APIs support the standard normalization forms which are described
in great detail in [Unicode Technical Report #15 (Unicode Normalization
Forms)](http://www.unicode.org/reports/tr15/) and the Normalization, Sorting and
Searching sections of chapter 5 of the [Unicode
Standard](http://www.unicode.org/versions/latest/). ICU also supports related,
additional operations. Some of them are described in [Unicode Technical Note #5
(Canonical Equivalence in Applications)](http://www.unicode.org/notes/tn5/).

## New API

ICU 4.4 adds the Normalizer2 API (in
[Java](https://unicode-org.github.io/icu-docs/apidoc/released/icu4j/com/ibm/icu/text/Normalizer2.html),
[C++](https://unicode-org.github.io/icu-docs/apidoc/released/icu4c/classNormalizer2.html) and
[C](https://unicode-org.github.io/icu-docs/apidoc/released/icu4c/unorm2_8h.html)), replacing almost all
of the old Normalizer API. There is a [design
doc](http://site.icu-project.org/design/normalization/custom) with many details.
All of the replaced old API is now implemented as a thin wrapper around the new
API.

Here is a summary of the differences:

*   Custom data: The new API uses non-static functions. A Normalizer2 instance
    can be created from standard Unicode normalization data, or from a custom
    (application-specific) data file with custom data processed by the new
    gennorm2 tool.
    *   Examples for possible custom data include UTS #46 IDNA mappings, MacOS X
        file system normalization, and a combination of NFKC with case folding
        (see the Unicode FC_NFKC_Closure property).
    *   By using a single data file and a single processing step for
        combinations like NFKC + case folding, the performance for such
        operations is improved.
*   NFKC_Casefold: ICU 4.4 supports the combination of NFKC, case folding and
    removing ignorable characters which was introduced with Unicode 5.2.
*   The old unorm.icu data file (used in Java, was hardcoded in the common
    library in C/C++) has been replaced with two new files, nfc.nrm and
    nfkc.nrm. If only canonical or only compatibility mappings are needed, then
    the other data file can be removed. There is also a new nfkc_cf.nrm file for
    NFKC_Casefold.
*   FCD: The old API supports [FCD
    processing](http://www.unicode.org/notes/tn5/#FCD) only for NFC/NFD data.
    Normalizer2 supports it for any data file, including NFKC/NFKD and custom
    data.
*   FCC: Normalizer2 optionally supports [contiguous
    composition](http://www.unicode.org/notes/tn5/#FCC) which is almost the same
    as NFC/NFKC except that the normalized form also passes the FCD test. This
    is also supported for any standard or custom data file.
*   Quick check: There is a new `spanQuickCheckYes()` function for an optimized
    combination of quick check and normalization.
*   Filtered: The new FilteredNormalizer2 class combines a Normalizer2 instance
    with a UnicodeSet to limit normalization to certain characters. For example,
    The old API's UNICODE_3_2 option is implemented via a FilteredNormalizer2
    using a UnicodeSet with the pattern `[:age=3.2:]`. (In other words, Unicode
    3.2 normalization now requires the uprops.icu data.)
*   Ease of use: In general, the switch to a factory method, otherwise
    non-static functions, and multiple data files, simplifies all of the
    function signatures.
*   Iteration: Support for iterative normalization is now provided by functions
    that test properties of code points, rather than requiring a particular type
    of ICU character iterator. The old implementation anyway simply fetched the
    code points and used equivalent code point test functions. The new API also
    provides a wider variety of such test functions.
*   String interfaces: In Java, input parameters are now CharSequence
    references, and output is to StringBuilder or Appendable.

The new API does not replace a few pieces of the old API:

*   The string comparison functions are still provided only on the old API,
    although reimplemented using the new code. They use multiple Normalizer2
    instances (FCD and NFD) and are therefore a poor fit for the new Normalizer2
    class. If necessary, a modernized replacement taking multiple Normalizer2
    instances as parameters is possible, but not planned.
*   The old QuickCheck return values are used by the new API as well.

## Data File Syntax

The gennorm2 tool accepts one or more .txt files and generates a .nrm binary
data file for `Normalizer2.getInstance()`. For gennorm2 command line options,
invoke `gennorm2 --help`.

gennorm2 starts with no data. If you want to include standard Unicode
Normalization data, use the files in
[{ICU4C}/source/data/unidata/norm2/](https://github.com/unicode-org/icu/tree/master/icu4c/source/data/unidata/norm2)
. You can modify one of them, or provide it together with one or more additional
files that add or remove mappings.

Hangul/Jamo data (mappings and ccc=0) are predefined and cannot be modified.

Mappings in one text file can override mappings in previous files of the same
gennorm2 invocation.

Comments start with #. White space between tokens is ignored. Characters are
written as hexadecimal code points. Combining class values are written as
decimal numbers.

In each file, each character can have at most one mapping and at most one ccc
(canonical combining class) value. A ccc value must not be 0. (ccc=0 is the
default.)

Each line defines data for either a single code point (`00E1`) or a range of
code points (`0300..0314`).

A two-way mapping must map to a sequence of exactly two characters. Multi-code
point ranges cannot have two-way mappings.

A one-way mapping can map to zero, one, two or more characters. Mapping to zero
characters removes the original character in normalization.

The generator tool will apply each mapping recursively to each other. Groups of
mappings that are forbidden by the Unicode Normalization algorithms are reported
as errors. For example, if a character has a two-way mapping, then neither of
its mapping characters can have a one-way mapping.

```
* Unicode 6.1         # Optional Unicode version (since ICU 49; default: uchar.h U_UNICODE_VERSION)
00E1=0061 0301        # Two-way mapping
00AA>0061             # One-way mapping
0300..0314:230        # ccc for a code point range
0315:232              # ccc for a single code point
0132..0133>0069 006A  # Range, each code point mapping to "ij"
E0000..E0FFF>         # Range, each code point mapping to the empty string
```

It is possible to override mappings from previous source files, including
removing a mapping:

```
    00AA-
    E0000..E0FFF-
```

## Data Generation Tool

Normally, data from one or more input files is combined as described above,
processed, and a binary data file is written for use by the ICU library (same
file for C++ and Java). The binary data file format changes occasionally in
order to support additional functionality.

```shell
    bin/gennorm2 -v -o $ICU4C_DATA_IN/nfkc_cf.nrm -s $ICU4C_UNIDATA/norm2 nfc.txt nfkc.txt nfkc_cf.txt
```

For the complete set of options, invoke `gennorm2 --help`.

Instead of the binary data file, the processed data can be written into a C
file. This is closely tied to the needs of the ICU library. The format may
change from one ICU version to the next.

```shell
    bin/gennorm2 -v -o $ICU_SRC/icu4c/source/common/norm2_nfc_data.h -s $ICU4C_UNIDATA/norm2 nfc.txt **--csource**
```

With the --combined option, gennorm2 writes the combined data of the input
files. The following example writes the combined NFKC_Casefold data. (New in ICU
60.)

```shell
    bin/gennorm2 -o /tmp/nfkc_cf.txt -s $ICU4C_UNIDATA/norm2 nfc.txt nfkc.txt nfkc_cf.txt **--combined**
```

With the "minus" operator, gennorm2 writes the diffs of the combined data from
two sets of input files. (New in ICU 60.)

For example, the nfkc_cf.txt file in ICU contains the Unicode NFKC_CF mappings,
extracted from the UCD file DerivedNormalizationProps.txt. It is not minimal.
The following command line generates the minimal differences of NFKC_Casefold
compared with NFKC.

```shell
    bin/gennorm2 -o /tmp/nfkc_cf-minus-nfkc.txt -s $ICU4C_UNIDATA/norm2 nfc.txt nfkc.txt nfkc_cf.txt **minus** nfc.txt nfkc.txt
```

## Example

```java
class NormSample {
public:
    // ICU service objects should be cached and reused, as usual.
    NormSample(UErrorCode &errorCode)
        : nfkc(*Normalizer2::getNFKCInstance(errorCode),
            fcd(*Normalizer2::getInstance(NULL, "nfc", UNORM2_FCD, errorCode) {}

    // Normalize a string.
    UnicodeString toNFKC(const UnicodeString &s, UErrorCode &errorCode) {
        return nfkc.normalize(s, errorCode);
    }

    // Ensure FCD before processing (like in sort key generation).
    // In practice, almost all strings pass the FCD test, so it might make sense to
    // test for it and only normalize when necessary, rather than always normalizing.
    void processText(const UnicodeString &s, UErrorCode &errorCode) {
        UnicodeString fcdString;
        const UnicodeString *ps;  // points to either s or fcdString
        int32_t spanQCYes=fcd.spanQuickCheckYes(s, errorCode);
        if(U_FAILURE(errorCode)) {
            return;  // report error
        }
        if(spanQCYes==s.length()) {
            ps=&s;  // s is already in FCD
        } else {
            // unnormalized suffix as a read-only alias (does not copy characters)
            UnicodeString unnormalized=s.tempSubString(spanQCYes);
            // set the fcdString to the FCD prefix as a read-only alias
            fcdString.setTo(FALSE, s.getBuffer(), spanQCYes);
            // automatic copy-on-write, and append the FCD'ed suffix
            fcd.normalizeSecondAndAppend(fcdString, unnormalized, errorCode);
            ps=&fcdString;
            if(U_FAILURE(errorCode)) {
                return;  // report error
            }
        }
        // ... now process the string *ps which is in FCD ...
    }
private:
    const Normalizer2 &nfkc;
    const Normalizer2 &fcd;
};
```
