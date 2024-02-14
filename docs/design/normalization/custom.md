---
layout: default
title: Custom Normalization
parent: Design Docs
---

<!--
© 2016 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Custom Normalization

## Goals

ICU 4.2 offers standard Unicode normalization (all standard forms) for the latest Unicode version, as well as for Unicode 3.2 (needed for IDNA2003). Sometimes, normalization is part of a specification that also includes a mapping step and a validation step. For example, NFKC may be combined with case folding, and IDNA/NamePrep/StringPrep combines a custom mapping table with Unicode 3.2 NFKC and simple (forbidden code points) as well as context-sensitive validation. IDNA 2008 and the related UTS #46 add further mapping+normalization combinations in common use. MacOS X uses custom normalization in file names, and other systems need to be able to match that for compatibility (e.g., for file synchronization (rsync)).

In the past, we have done some of this by

*   Providing a small, fixed set of exclusion sets, available via either internal or public option bits, to make some characters "inert". For example, for restricting normalization to Unicode 3.2. This was not scalable with the static API functions.
*   Using a separate mapping step before normalization. This restricts the possible customizations.
        
We want to be able to combine a custom mapping with Unicode normalization into one API plus a loadable data file, for better combined performance and for providing a build tool for such data. To minimize additional code and to get good test coverage, standard normalization code should be rerouted to the new code.

We would like

*   More flexible API, using a service object and non-static methods, to handle complicated setup if and when necessary.
*   The ability to specify and customize all mappings.
*   Better performance by removing the separate mapping step.
        
The new data file _might_ also carry optional UnicodeSets, for example:

*   A set of code points for validation. Validation would not be built into the normalization processing. Users would probably be able to get this set, and would use its span() function.
        
## Status

This has been implemented in ICU 4.4, with the Normalizer2 API (in [Java](https://unicode-org.github.io/icu-docs/apidoc/released/icu4j/com/ibm/icu/text/Normalizer2.html), [C++](https://unicode-org.github.io/icu-docs/apidoc/released/icu4c/classNormalizer2.html) and [C](https://unicode-org.github.io/icu-docs/apidoc/released/icu4c/unorm2_8h.html)) replacing almost all of the old API. See the [User Guide Normalization chapter](../../userguide/transforms/normalization/) for the new documentation. This design doc remains, with motivations, decisions and details of the new data structures not documented elsewhere.

ICU 49 modifies the file format slightly (formatVersion 2) in support of getRawDecomposition(c) and composePair(a, b) ([ticket #8804](https://unicode-org.atlassian.net/browse/ICU-8804)), and adding a small amount of data for FCD ([ticket #8942](https://unicode-org.atlassian.net/browse/ICU-8942)).

ICU 60 modifies the file format (formatVersion 3) to put more information directly into the per-character "norm16" trie lookup value. NFC/NFKC/... boundary detection is much simpler, and many mappings are possible without entering the slow path.

The current implementation does not support storing UnicodeSet data in normalization data files.

## New API

class Normalizer2: unmodifiable/immutable instances with all-virtual public methods

See the Normalizer2 API (in [Java](https://unicode-org.github.io/icu-docs/apidoc/released/icu4j/com/ibm/icu/text/Normalizer2.html), [C++](https://unicode-org.github.io/icu-docs/apidoc/released/icu4c/classNormalizer2.html) and [C](https://unicode-org.github.io/icu-docs/apidoc/released/icu4c/unorm2_8h.html)) implemented in ICU 4.4 and above.

Not currently implemented is a `getUnicodeVersion(UVersionInfo v) const;` function. The data is available, but there was no obvious use case since the normalizer's Unicode version is normally the same as the character property Unicode version. (This might not be true for custom data.)

## Filtered Normalization

We need to support Unicode 3.2 normalization, and it may be useful to restrict normalization to other sets of characters. While building this into the core normalization code (as in ICU 4.2 and below) is possible and allows single-pass operation, it significantly complicates the code and probably slows it down slightly for normal, unrestricted operation.

Instead, we implement this via `UnicodeSet::span()`: We (logically) split the input into spans of text covered by the set and spans of text outside the set. Each in-the-set span is normalized, the others are simply appended.

We could provide a wrapper class for this, either a simple one that only supports filtered normalization itself, or one that implements all of the new API.

If we needed better performance, then we could create a dedicated data file that combines the Unicode 3.2 normalization data (including corrigenda) with the NamePrep mappings.

(ICU 4.4 provides the FilteredNormalizer2 class and implements the old API's `UNICODE_3_2` option via this class and a `UnicodeSet("[:age=3.2:]")`.)

## Old and New Implementation Details

The old normalization data format (unorm.icu, ca. 2001..2009) uses three data structures for normalization: A trie for looking up 32-bit values for every code point, a 16-bit-unit array with decompositions and some other data, and a composition table (16-bit-unit array, linear search list per starter). The data is combined for all 4 standard normalization forms: NFC, NFD, NFKC and NFKD.

In addition, there are several more data structures, for a precomputed FCD table (used in collation and in canonical case comparisons) and for various normalization-related properties even though they don't directly contribute to the actual normalization processing. In ICU4C, the normalization data has been hardcoded in a .c file for a few versions to avoid mutexing in the static API functions.

For a custom normalization table, it does not make sense to combine canonical and compatibility mappings: Any one custom table will have one set of mappings, not two alternatives. Therefore, it makes sense to split the standard normalization data into two data files, one for NFC/NFD and one for NFKC/NFKD. While this may grow the total data size, we think we can make each data file substantially smaller, not only by omitting data specific to the other forms, but also by simplifying the data structure itself which becomes feasible when alternatives need not be included.

It would also be possible to split the decomposition data (sufficient for NFD and for the quick check and decomposition parts of NFC) from the composition data (used for recomposing NFD sequences), and to share the standard composition data among several custom data files rather than duplicating it. While this seems attractive at first glance, we decided against it: Splitting off the composition data would 1. duplicate some of the data between decomposition and composition tables because both need data for canonical combining classes (ccc) and whether a character combines with a preceding one, 2. require a separate trie (or another, fast lookup table) which would add some 6kB of overhead, and 3. application-specific data files relying on the standard composition data would fail to work with a new version of ICU that has normalization data for a newer Unicode version. Standard Unicode normalization has a small number of compositions, taking up maybe 5kB in a space-efficient data structure that also stores decomposition data.

We will need new API for custom normalization. It should have real service objects, rather than the current Normalizer's static worker functions. (**Done in ICU 4.4.**)

With the new API, we should consider reversing the hardcoding of the normalization data in C source code. The old API functions would then have to go through a mutex, but the new API would be available to avoid this. (**Done in ICU 4.4.**)

The new code should be able to work with either nfc.nrm or nfd.nrm data files to satisfy requests for both ("nfc"/decompose) and ("nfd"/decompose). Same for NFKC/NFKD. This should be hardcoded only to these names of the standard Unicode normalization forms, and not apply to custom tables. (**Not done**: NFD uses "nfc"/decompose and NFKD uses "nfkc"/decompose.)

We should probably continue to store a dedicated FCD table in nfc.nrm, but omit it from other files. If FCD operations are requested using a file that does not have the dedicated, pre-built table, then the runtime code can build one on the fly. (**Not done**: ICU 4.4 always builds the FCD trie at runtime, but only when it is first needed.)

The old implementation works with an optional set of code points "excluded from normalization", that is, making those code points normalization-inert despite what the mapping table says. We have a one-bit option in the public API to request Unicode 3.2 normalization which uses such an "exclusion set". This is rarely used (practically only in IDNA2003/StringPrep) and complicates the implementation. Instead, we plan to implement "filtered normalization" on top of standard normalization. There is a section about this near the end of this page. (**Done in ICU 4.4.**)

## Main Data Lookup

### ICU 4.2 and earlier

The old data yields a 32-bit word per code point:

*   6 bits of quick check data: NFC and NFKC Yes/Maybe/No and NFD/NFKD Yes/No.
    *   Without alternate mappings, only one of these two sets of quick check bits is needed.
*   2 bits for whether the character combines forward (with a following character) or backward (with a preceding character).
    *   It turns out that the "combines backward" bit is fully redundant with NFC\_QC=Maybe.
*   8 bits for the canonical combining class (ccc).
*   16 bits, usually an index to the "extra data" which stores the decompositions and an index into the composition table, if relevant; Hangul and Jamo characters have special values.
    *   The standard Unicode 5.2 data stores about 18000 16-bit units for decomposition and composition data. Removing alternate mappings would reduce this number, but adding further mappings for case folding or similar could significantly increase it as well. It is desirable to support a 15-bit or 16-bit range of index values.
    *   Observation after implementing the new code and data structure: Adding case folding to NFKC generates a _smaller_ file overall than NFKC alone, partly because there is less composition data. (Some two-way mappings are overridden or turned into one-way mappings.)
            
With the trivial redundancies removed for separate NFC/NFKC data (no second quick check flags set, no redundant combines-back bit), this effectively uses 28 bits.

Analysis of the normalization algorithm and its ICU implementation shows that some combinations of these values cannot occur, and the number of bits can be reduced. It is particularly desirable to encode the most performance-sensitive data (especially for efficient quick checks) in a 16-bit word rather than a 32-bit word, to use a significantly smaller and slightly faster version of the trie.

Per starter that combines forward, old and new data stores a linear, sorted list of (combining-back code point, composite) pairs. It would be faster to use a hash table, but that would be significantly larger. The current strategy is to try to optimize for quick check YesYes characters (including ones with ccc!=0), and the most common back-combining characters tend to have low code points and appear early in the composition list.

### Unicode Normalization: Features and Limitations

*   Decomposition is really just a mapping from code points to strings.
*   Canonical ordering requires ccc data.
*   Composition only combines the most recent starter with one other character, if such a mapping is defined. It only ever combines one pair into one composite per step.
*   Every composition is the reverse of a corresponding decomposition. That is, a decomposition can either be a one-way mapping (from one code point to a sequence of one or more others but not back from that sequence to the original), or it can be a two-way mapping (from one code point to a pair of others, and back).
    *   Note: Custom mappings may also map some characters away, that is, to an empty string. The ICU 4.2 implementation is not prepared to handle such a case because it does not occur in standard Unicode normalization. This will need to be supported for custom tables.
    *   Note: Unicode NFKC\_Casefold and UTS #46 map each Default\_Ignorable\_Code\_Point to an empty string.
*   A starter is defined as a character with ccc=0.
*   Only a starter can combine-forward, but most starters don't. (The set of compositions/2-way mappings in standard Unicode normalization increases only slowly.)
*   A composite (result of combining a pair of characters) must have ccc=0, or else the result of composition may not be in canonical order because there is not another reordering step.
*   A composite can combine-forward. The composition algorithm tries to combine the new composite with following characters. (For example, base characters with two diacritics, and Hangul LVT.)
    *   The ICU implementation recomposes starting from a fully decomposed sequence. Therefore, the lookup value needs to indicate combines-forward only for characters that do not have a mapping. The composition table result then indicates whether a composite combines-forward, and the index to the combined mapping+composition data is then found via the index from the composite's lookup result.
    *   ICU 49 composePair() needs to know whether the first character combines forward even if it is a composite. formatVersion 2 separates the YesNo range into two parts accordingly, adding the yesNoMappingsOnly threshold.
*   A composite cannot combine-back because the composition algorithm does not try to combine an earlier starter with the new composite.
*   The algorithm allows for a character to both combine-back and combine-forward, although this seems like a strange situation and it does not occur in Unicode 5.2..10.
*   Hangul syllables are algorithmically decomposed into Jamos, and algorithmically recomposed from them. The actual mappings are not stored in the table.
*   In the ICU implementation, recomposition is done only on a fully decomposed sequence. Composition then sees only YesYes and MaybeYes characters which do not have mappings.
*   A character that maps to an empty string (that is, one that is deleted during normalization) does not have normalization boundaries before or after it. Its FCD value would be the worst-case 0x1ff (lccc=1, tccc=0xff). (The standard Unicode normalization forms do not delete characters, but NFKC\_Casefold and UTS #46 do.)
        
### Possible and Impossible Combinations

Terminology:

*   NFC and NFD are mentioned, representing possible types of data. Since alternate mappings use their own tables, these terms stand for NFKC and NFKD if the nfkc data file is used; if a custom data file is used, these terms describe comparable properties of characters in that table.
*   NFD\_QC=Yes means "has no mapping" (decomposition in standard normalization)
*   NFD\_QC=No means "has a mapping"
*   NFC\_QC=Maybe means "combines backward" (with a preceding starter)
*   Terms like YesNo and MaybeYes refer to combinations of NFC+NFD quick check values.
        
The 16-bit values are arranged for maximum efficiency of quick checks which only drop out of a very tight loop when they see a No or Maybe, or ccc≠0.

*   NFC quick check tight loop tests each character's lookup value for <minNoNo.
        
*   NFD quick check tight loop tests each character's lookup value for <minYesNo.
        
A simple mapping to one code point can be stored directly in the lookup value, without extra data. This is only possible for NoNo characters because it only works for a one-way (one code point to one other code point) mapping.

ICU does not allow tailoring of Hangul/Jamo mappings and compositions, except to make the relevant characters completely inert.

MaybeNo is both forbidden and irrelevant:

*   Forbidden: If it has a one-way mapping, it has NoNo quick check values. If it has a two-way mapping, then it is a composite, but the Unicode composition would not try to combine it with a preceding character.
*   Irrelevant: Composition sees NFD, so it sees no characters with mappings. A combines-backward character would never combine with anything.
        
NoYes is impossible: If it has no mapping, it will occur in NFC.

A YesNo only ever decomposes into a YesYes+MaybeYes sequence or a YesNo+MaybeYes sequence. That is, a YesNo's decomposition (A=B+C) decomposes further if and only if the first (B) of its two components has a decomposition.

A YesNo always has ccc=lccc=0.

Only a starter can combine-forward, therefore no character can have ccc≠0 and combine forward.

A NoNo can have any of its components decompose further, but this is only visible in the raw mappings. The regular mappings are fully decomposed.

NoNo with combine-forward is impossible: A one-way mapping prevents composition (which starts from NFD where there are no decomposable characters).

### Per-character lookup values, .nrm formatVersion 3

Since ICU 60

Changes from version 2:

*   16-bit value bit 0 used for has-composition-boundary-after, ccc & indexes shifted left by 1.
*   16-bit values for delta mappings carry tccc data in bits 2..1.
*   Delta mappings restricted to mappings to NFC\_QC=Yes and ccc=0, ASCII maps only to ASCII.
*   minNoNo..limitNoNo subdivided with three new thresholds, see the table below.
*   Hangul LV & LVT use separate values because they have different properties.
*   Addition of `indexes[IX_MIN_LCCC_CP]`, the first code point where lccc!=0.
    *   This used to be hardcoded to U+0300, but in data like NFKC\_Casefold it is lower:
    *   U+00AD Soft Hyphen maps to an empty string, which is artificially assigned "worst case" values lccc=1 and tccc=255.
*   The extraData firstUnit bit 5 is no longer necessary (norm16 bit 0 used instead of firstUnit `MAPPING_NO_COMP_BOUNDARY_AFTER`), is reserved again, and always set to 0.
*   A mapping to an empty string has explicit lccc=1 and tccc=255 values.

Possible combinations and their encoding:

_The rows of the table, from bottom to top, are encoded with increasing 16-bit "norm16" values as noted in the last column. Per-row and per-row-group properties are determined via norm16 range checks._

<!-- The default style on these pages sets th/td padding and max-width that make these tables very bloated and hard to read. -->
<style type="text/css">.compacttable th,.compacttable td { padding: unset; min-width: unset; }</style>

<table border="1" bordercolor="#888888" class="compacttable" style="border-color:rgb(136,136,136);border-width:1px;border-collapse:collapse;border-spacing:0" data-table-local-id="table-2">
  <tbody>
    <tr>
      <td style="width:71px;height:47px">
        <b>NFC_QC</b>
      </td>
      <td style="width:71px;height:47px">
        <b>NFD_QC</b>
      </td>
      <td style="width:52px;height:47px">
        <b>ccc</b>
      </td>
      <td style="width:75px;height:47px">
        <b>
          has comp
          <br />
          boundary
          <br />
          before
        </b>
      </td>
      <td style="width:74px;height:47px">
        <b>
          combines
          <br />
          forward
        </b>
      </td>
      <td style="width:476px;height:47px">
        <b>comments</b>
      </td>
      <td style="width:60px">
        <b>
          sample char
          <br />
          NFKC_CF
        </b>
      </td>
      <td style="width:456px;height:47px">
        <b>16-bit value</b>
      </td>
    </tr>
    <tr>
      <td style="width:71px;height:31px">any</td>
      <td style="width:71px;height:31px">any</td>
      <td style="width:52px;height:31px">any</td>
      <td style="width:75px;height:31px">any</td>
      <td style="width:74px;height:31px">any</td>
      <td style="width:476px;height:31px">all values</td>
      <td style="width:60px">any</td>
      <td style="width:456px;height:31px">
        bit 0 is set if the character has a composition boundary after it;
        <br />
        mapping or composition indexes are in bits 15..1
      </td>
    </tr>
    <tr>
      <td style="background-color:rgb(217,234,211);width:71px;height:31px">
        Yes
        <br />
      </td>
      <td style="background-color:rgb(217,234,211);width:71px;height:31px">
        Yes
        <br />
      </td>
      <td style="width:52px;height:31px">
        ≠0
        <br />
      </td>
      <td style="width:75px;height:31px">no</td>
      <td style="width:74px;height:31px">
        no
        <br />
      </td>
      <td style="width:476px;height:31px">
        Most combining marks
        <br />
      </td>
      <td style="width:60px">◌्</td>
      <td style="width:456px;height:31px">
        fe02..fffe
        <br />
        bits 8..1: ccc≠0
        <br />
      </td>
    </tr>
    <tr>
      <td style="background-color:rgb(255,242,204);width:71px;height:15px">
        Maybe
        <br />
      </td>
      <td style="background-color:rgb(217,234,211);width:71px;height:15px">
        Yes
        <br />
      </td>
      <td style="width:52px;height:15px">
        0
        <br />
      </td>
      <td style="width:75px;height:15px">no</td>
      <td style="width:74px;height:15px">
        no
        <br />
      </td>
      <td style="width:476px;height:15px">
        Jamo V &amp; T
        <br />
      </td>
      <td style="width:60px">ᅡ</td>
      <td style="width:456px;height:15px">
        fe00
        <br />
      </td>
    </tr>
    <tr>
      <td style="background-color:rgb(255,242,204);width:71px;height:31px">
        Maybe
        <br />
      </td>
      <td style="background-color:rgb(217,234,211);width:71px;height:31px">
        Yes
        <br />
      </td>
      <td style="width:52px;height:31px">
        any
        <br />
      </td>
      <td style="width:75px;height:31px">no</td>
      <td style="width:74px;height:31px">
        no
        <br />
      </td>
      <td style="width:476px;height:31px">
        Combining marks that combine backward
        <br />
      </td>
      <td style="width:60px">◌̀</td>
      <td style="width:456px;height:31px">
        fc00..fdfe
        <br />
        bits 8..1: ccc
        <br />
      </td>
    </tr>
    <tr>
      <td style="background-color:rgb(255,242,204);width:71px;height:31px">
        Maybe
        <br />
      </td>
      <td style="background-color:rgb(217,234,211);width:71px;height:31px">
        Yes
        <br />
      </td>
      <td style="width:52px;height:31px">
        0
        <br />
      </td>
      <td style="width:75px;height:31px">no</td>
      <td style="width:74px;height:31px">
        yes
        <br />
      </td>
      <td style="width:476px;height:31px">
        Both combine-back &amp; combine-fwd: strange but allowed
        <br />
      </td>
      <td style="width:60px">none</td>
      <td style="width:456px;height:31px">
        ≥minMaybeYes which is 8-aligned
        <br />
        index into composition table
        <br />
      </td>
    </tr>
    <tr>
      <td style="background-color:rgb(244,204,204);width:71px;height:47px">
        No
        <br />
      </td>
      <td style="background-color:rgb(244,204,204);width:71px;height:47px">
        No
        <br />
      </td>
      <td style="width:52px;height:47px">
        0
        <br />
      </td>
      <td style="width:75px;height:47px">yes</td>
      <td style="width:74px;height:47px">
        no
        <br />
      </td>
      <td style="width:476px;height:47px">
        Has 1-way mapping to a nearby character with NFC_QC=Yes and ccc=0, ASCII maps only to ASCII;
        <br />
        size optimization for case mappings
        <br />
      </td>
      <td style="width:60px">A</td>
      <td style="width:456px;height:47px">
        ≥minNoNoDelta=minMaybeYes-((2*maxDelta+1)&lt;&lt;3)
        <br />
        delta=0 is at minMaybeYes-((maxDelta-1)&lt;&lt;3); it must not be used
        <br />
        bits 2..1: tccc=0 or 1 or &gt;1
      </td>
    </tr>
    <tr style="background-color:rgb(204,204,204)">
      <td style="width:71px;height:47px">
        No
        <br />
      </td>
      <td style="width:71px;height:47px">
        No
        <br />
      </td>
      <td style="width:52px;height:47px">
        any
        <br />
      </td>
      <td style="width:75px;height:47px">any</td>
      <td style="width:74px;height:47px">
        no
        <br />
      </td>
      <td style="width:476px;height:47px">
        Gap/unused values; could be used for other algorithmic mappings
        <br />
      </td>
      <td style="width:60px"> </td>
      <td style="width:456px;height:47px">
        ≥limitNoNo
        <br />
        builder must test for collision of adjacent ranges:
        <br />
        check for minNoNoDelta≥limitNoNo
      </td>
    </tr>
    <tr>
      <td style="background-color:rgb(244,204,204);width:71px;height:47px">
        No
        <br />
      </td>
      <td style="background-color:rgb(244,204,204);width:71px;height:47px">
        No
        <br />
      </td>
      <td style="width:52px;height:47px">
        any
        <br />
      </td>
      <td style="width:75px;height:47px">no</td>
      <td style="width:74px;height:47px">
        no
        <br />
      </td>
      <td style="width:476px;height:47px">Has 1-way mapping to an empty string, with explicit lccc=1 and tccc=255 values</td>
      <td style="width:60px">U+00AD Soft Hyphen</td>
      <td style="width:456px;height:47px">
        ≥minNoNoEmpty
        <br />
        index into decomposition table
        <br />
        (raw mappings might differ)
      </td>
    </tr>
    <tr>
      <td style="background-color:rgb(244,204,204);width:71px;height:31px">
        No
        <br />
      </td>
      <td style="background-color:rgb(244,204,204);width:71px;height:31px">
        No
        <br />
      </td>
      <td style="width:52px;height:31px">
        any
        <br />
      </td>
      <td style="width:75px;height:31px">no</td>
      <td style="width:74px;height:31px">
        no
        <br />
      </td>
      <td style="width:476px;height:31px">
        Has 1-way mapping which has no composition boundary before it
        <br />
      </td>
      <td style="width:60px">◌̈́</td>
      <td style="width:456px;height:31px">
        ≥minNoNoCompNoMaybeCC
        <br />
        index into decomposition table
        <br />
      </td>
    </tr>
    <tr>
      <td style="background-color:rgb(244,204,204);width:71px;height:31px">
        No
        <br />
      </td>
      <td style="background-color:rgb(244,204,204);width:71px;height:31px">
        No
        <br />
      </td>
      <td style="width:52px;height:31px">
        any
        <br />
      </td>
      <td style="width:75px;height:31px">yes</td>
      <td style="width:74px;height:31px">
        no
        <br />
      </td>
      <td style="width:476px;height:31px">
        Has 1-way mapping which has a composition boundary before it
        <br />
        (starts with a starter that does not combine backward)
      </td>
      <td style="width:60px">Ÿ</td>
      <td style="width:456px;height:31px">
        ≥minNoNoCompBoundaryBefore
        <br />
        index into decomposition table
        <br />
      </td>
    </tr>
    <tr>
      <td style="background-color:rgb(244,204,204);width:71px;height:31px">
        No
        <br />
      </td>
      <td style="background-color:rgb(244,204,204);width:71px;height:31px">
        No
        <br />
      </td>
      <td style="width:52px;height:31px">
        any
        <br />
      </td>
      <td style="width:75px;height:31px">yes</td>
      <td style="width:74px;height:31px">
        no
        <br />
      </td>
      <td style="width:476px;height:31px">
        Has 1-way mapping which is composition-normalized
        <br />
        (has a composition boundary before, does not recompose by itself)
      </td>
      <td style="width:60px">ß</td>
      <td style="width:456px;height:31px">
        ≥minNoNo
        <br />
        index into decomposition table
        <br />
      </td>
    </tr>
    <tr>
      <td style="background-color:rgb(217,234,211);width:71px;height:31px">
        Yes
        <br />
      </td>
      <td style="background-color:rgb(244,204,204);width:71px;height:31px">
        No
        <br />
      </td>
      <td style="width:52px;height:31px">
        0
        <br />
      </td>
      <td style="width:75px;height:31px">yes</td>
      <td style="width:74px;height:31px">
        no
        <br />
      </td>
      <td style="width:476px;height:31px">
        Has 2-way mapping, does not combine forward
        <br />
      </td>
      <td style="width:60px">à</td>
      <td style="width:456px;height:31px">
        &gt;minYesNoMappingsOnly
        <br />
        index into decomposition table
        <br />
      </td>
    </tr>
    <tr>
      <td style="background-color:rgb(217,234,211);width:71px;height:15px">
        Yes
        <br />
      </td>
      <td style="background-color:rgb(244,204,204);width:71px;height:15px">
        No
        <br />
      </td>
      <td style="width:52px;height:15px">
        0
        <br />
      </td>
      <td style="width:75px;height:15px">yes</td>
      <td style="width:74px;height:15px">
        no
        <br />
      </td>
      <td style="width:476px;height:15px">
        Hangul LVT (does not combine forward)
        <br />
      </td>
      <td style="width:60px">각</td>
      <td style="width:456px;height:15px">
        =minYesNoMappingsOnly|1
        <br />
      </td>
    </tr>
    <tr>
      <td style="background-color:rgb(217,234,211);width:71px;height:31px">
        Yes
        <br />
      </td>
      <td style="background-color:rgb(244,204,204);width:71px;height:31px">
        No
        <br />
      </td>
      <td style="width:52px;height:31px">
        0
        <br />
      </td>
      <td style="width:75px;height:31px">yes</td>
      <td style="width:74px;height:31px">
        yes
        <br />
      </td>
      <td style="width:476px;height:31px">
        Has 2-way mapping, combines forward
        <br />
      </td>
      <td style="width:60px">â</td>
      <td style="width:456px;height:31px">
        &gt;minYesNo
        <br />
        index into decomp+comp table
        <br />
      </td>
    </tr>
    <tr>
      <td style="background-color:rgb(217,234,211);width:71px;height:15px">
        Yes
        <br />
      </td>
      <td style="background-color:rgb(244,204,204);width:71px;height:15px">
        No
        <br />
      </td>
      <td style="width:52px;height:15px">
        0
        <br />
      </td>
      <td style="width:75px;height:15px">yes</td>
      <td style="width:74px;height:15px">
        yes
        <br />
      </td>
      <td style="width:476px;height:15px">
        Hangul LV (combines forward)
        <br />
      </td>
      <td style="width:60px">가</td>
      <td style="width:456px;height:15px">
        =minYesNo
        <br />
      </td>
    </tr>
    <tr>
      <td style="background-color:rgb(217,234,211);width:71px;height:31px">
        Yes
        <br />
      </td>
      <td style="background-color:rgb(217,234,211);width:71px;height:31px">
        Yes
        <br />
      </td>
      <td style="width:52px;height:31px">
        0
        <br />
      </td>
      <td style="width:75px;height:31px">yes</td>
      <td style="width:74px;height:31px">
        yes
        <br />
      </td>
      <td style="width:476px;height:31px">
        Starter, combines forward
        <br />
      </td>
      <td style="width:60px">a</td>
      <td style="width:456px;height:31px">
        ≥4
        <br />
        index into composition table
        <br />
      </td>
    </tr>
    <tr>
      <td style="background-color:rgb(217,234,211);width:71px;height:15px">
        Yes
        <br />
      </td>
      <td style="background-color:rgb(217,234,211);width:71px;height:15px">
        Yes
        <br />
      </td>
      <td style="width:52px;height:15px">
        0
        <br />
      </td>
      <td style="width:75px;height:15px">yes</td>
      <td style="width:74px;height:15px">
        yes
        <br />
      </td>
      <td style="width:476px;height:15px">
        Jamo L
        <br />
      </td>
      <td style="width:60px">ᄀ</td>
      <td style="width:456px;height:15px">
        2
        <br />
      </td>
    </tr>
    <tr>
      <td style="background-color:rgb(217,234,211);width:71px;height:15px">
        Yes
        <br />
      </td>
      <td style="background-color:rgb(217,234,211);width:71px;height:15px">
        Yes
        <br />
      </td>
      <td style="width:52px;height:15px">
        0
        <br />
      </td>
      <td style="width:75px;height:15px">yes</td>
      <td style="width:74px;height:15px">
        no
        <br />
      </td>
      <td style="width:476px;height:15px">
        Normalization-inert
        <br />
      </td>
      <td style="width:60px">!</td>
      <td style="width:456px;height:15px">
        1
        <br />
      </td>
    </tr>
  </tbody>
</table>

### Per-character lookup values, .nrm formatVersion 1 & 2

Version 1: ICU 4.4..4.8

Version 2: ICU 49..59

Possible combinations and their encoding:

_The rows of the table, from bottom to top, are encoded with increasing 16-bit "norm16" values as noted in the last column. Per-row and per-row-group properties are determined via norm16 range checks._

The minYesNoMappingsOnly distinction was added in ICU 49, .nrm formatVersion 2.0. In formatVersion 1.0, the data for either type of yesNo characters (combines-forward or not) were mixed, and the mapping's firstUnit bit 6 was the `MAPPING_PLUS_COMPOSITION_LIST` flag. See the firstUnit documentation below.

<table border="1" bordercolor="#888888" class="compacttable" style="border-color:rgb(136,136,136);border-width:1px;border-collapse:collapse;border-spacing:0" data-table-local-id="table-3">
  <tbody>
    <tr>
      <td style="width:60px">
        <b>NFC_QC</b>
      </td>
      <td style="width:60px">
        <b>
          NFD_QC
          <br />
        </b>
      </td>
      <td style="width:60px">
        <b>
          ccc
          <br />
        </b>
      </td>
      <td style="width:60px">
        <b>
          combines-fwd
          <br />
        </b>
      </td>
      <td style="width:60px">
        <b>
          comments
          <br />
        </b>
      </td>
      <td style="width:60px">
        <b>
          16-bit value
          <br />
        </b>
      </td>
    </tr>
    <tr>
      <td style="width:60px;background-color:rgb(217,234,211)">
        Yes
        <br />
      </td>
      <td style="width:60px;background-color:rgb(217,234,211)">
        Yes
        <br />
      </td>
      <td style="width:60px">
        !=0
        <br />
      </td>
      <td style="width:60px">
        no
        <br />
      </td>
      <td style="width:60px">
        Most combining marks
        <br />
      </td>
      <td style="width:60px">
        ff01..ffff
        <br />
        lower 8 bits: ccc
        <br />
      </td>
    </tr>
    <tr>
      <td style="width:60px;background-color:rgb(255,242,204)">
        Maybe
        <br />
      </td>
      <td style="width:60px;background-color:rgb(217,234,211)">
        Yes
        <br />
      </td>
      <td style="width:60px">
        0
        <br />
      </td>
      <td style="width:60px">
        no
        <br />
      </td>
      <td style="width:60px">
        Jamo V &amp; T
        <br />
      </td>
      <td style="width:60px">
        ff00
        <br />
      </td>
    </tr>
    <tr>
      <td style="width:60px;background-color:rgb(255,242,204)">
        Maybe
        <br />
      </td>
      <td style="width:60px;background-color:rgb(217,234,211)">
        Yes
        <br />
      </td>
      <td style="width:60px">
        any
        <br />
      </td>
      <td style="width:60px">
        no
        <br />
      </td>
      <td style="width:60px">
        Combining marks that combine-back
        <br />
      </td>
      <td style="width:60px">
        fe00..feff
        <br />
        lower 8 bits: ccc
        <br />
      </td>
    </tr>
    <tr>
      <td style="width:60px;background-color:rgb(255,242,204)">
        Maybe
        <br />
      </td>
      <td style="width:60px;background-color:rgb(217,234,211)">
        Yes
        <br />
      </td>
      <td style="width:60px">
        0
        <br />
      </td>
      <td style="width:60px">
        yes
        <br />
      </td>
      <td style="width:60px">
        Both combine-back &amp; combine-fwd: strange but allowed
        <br />
      </td>
      <td style="width:60px">
        minMaybeYes..fdff
        <br />
        index into composition table
        <br />
      </td>
    </tr>
    <tr>
      <td style="background-color:rgb(244,204,204)">
        No
        <br />
      </td>
      <td style="background-color:rgb(244,204,204)">
        No
        <br />
      </td>
      <td>
        0
        <br />
      </td>
      <td>
        no
        <br />
      </td>
      <td>
        Has 1-way mapping to a nearby character; size optimization for case mappings
        <br />
      </td>
      <td>
        minMaybeYes-(2*maxDelta+1)..minMaybeYes-1
        <br />
        (delta=0 is at minMaybeYes-maxDelta-1; it must not be used)
        <br />
      </td>
    </tr>
    <tr style="background-color:rgb(204,204,204)">
      <td>
        No
        <br />
      </td>
      <td>
        No
        <br />
      </td>
      <td>
        any
        <br />
      </td>
      <td>
        no
        <br />
      </td>
      <td>
        Gap; could be used for other algorithmic mappings
        <br />
      </td>
      <td>
        unused values
        <br />
        builder must test for collision of adjacent ranges
        <br />
      </td>
    </tr>
    <tr>
      <td style="width:60px;background-color:rgb(244,204,204)">
        No
        <br />
      </td>
      <td style="width:60px;background-color:rgb(244,204,204)">
        No
        <br />
      </td>
      <td style="width:60px">
        any
        <br />
      </td>
      <td style="width:60px">
        no
        <br />
      </td>
      <td style="width:60px">
        Has 1-way mapping
        <br />
      </td>
      <td style="width:60px">
        minNoNo..maxNoNo
        <br />
        index into decomposition table
        <br />
      </td>
    </tr>
    <tr>
      <td style="width:60px;background-color:rgb(217,234,211)">
        Yes
        <br />
      </td>
      <td style="width:60px;background-color:rgb(244,204,204)">
        No
        <br />
      </td>
      <td style="width:60px">
        0
        <br />
      </td>
      <td style="width:60px">
        no
        <br />
      </td>
      <td style="width:60px">
        Has 2-way mapping, does not combine forward
        <br />
      </td>
      <td style="width:60px">
        minYesNoMappingsOnly..minNoNo-1
        <br />
        index into decomposition table
        <br />
      </td>
    </tr>
    <tr>
      <td style="width:60px;background-color:rgb(217,234,211)">
        Yes
        <br />
      </td>
      <td style="width:60px;background-color:rgb(244,204,204)">
        No
        <br />
      </td>
      <td style="width:60px">
        0
        <br />
      </td>
      <td style="width:60px">
        yes
        <br />
      </td>
      <td style="width:60px">
        Has 2-way mapping, combines forward
        <br />
      </td>
      <td style="width:60px">
        minYesNo+1..minYesNoMappingsOnly-1
        <br />
        index into decomp+comp table
        <br />
      </td>
    </tr>
    <tr>
      <td style="background-color:rgb(217,234,211)">
        Yes
        <br />
      </td>
      <td style="background-color:rgb(244,204,204)">
        No
        <br />
      </td>
      <td>
        0
        <br />
      </td>
      <td>
        any
        <br />
      </td>
      <td>
        Hangul LV (combines forward) &amp; LVT (does not combine forward)
        <br />
      </td>
      <td>
        minYesNo
        <br />
      </td>
    </tr>
    <tr>
      <td style="width:60px;background-color:rgb(217,234,211)">
        Yes
        <br />
      </td>
      <td style="width:60px;background-color:rgb(217,234,211)">
        Yes
        <br />
      </td>
      <td style="width:60px">
        0
        <br />
      </td>
      <td style="width:60px">
        yes
        <br />
      </td>
      <td style="width:60px">
        Starter, combines forward
        <br />
      </td>
      <td style="width:60px">
        2..minYesNo-1
        <br />
        index into composition table
        <br />
      </td>
    </tr>
    <tr>
      <td style="width:60px;background-color:rgb(217,234,211)">
        Yes
        <br />
      </td>
      <td style="width:60px;background-color:rgb(217,234,211)">
        Yes
        <br />
      </td>
      <td style="width:60px">
        0
        <br />
      </td>
      <td style="width:60px">
        yes
        <br />
      </td>
      <td style="width:60px">
        Jamo L
        <br />
      </td>
      <td style="width:60px">
        1
        <br />
      </td>
    </tr>
    <tr>
      <td style="width:60px;background-color:rgb(217,234,211)">
        Yes
        <br />
      </td>
      <td style="width:60px;background-color:rgb(217,234,211)">
        Yes
        <br />
      </td>
      <td style="width:60px">
        0
        <br />
      </td>
      <td style="width:60px">
        no
        <br />
      </td>
      <td style="width:60px">
        Normalization-inert
        <br />
      </td>
      <td style="width:60px">
        0
        <br />
      </td>
    </tr>
  </tbody>
</table>

### Additional data indexed by the trie value

(**Implemented in ICU 4.4, .nrm formatVersion 1.0. Modified in ICU 49, .nrm formatVersion 2.0 and in ICU 60, .nrm formatVersion 3.0.**)

"Extra data" per code point, if it has a mapping or if it combines-forward, is stored in 16-bit-unit arrays. The character's lookup value is an index into one of these arrays. It is probably handy to have two arrays, so that indexes can be allocated independently for the two ranges of 16-bit lookup values that are indexes into extra data.

*   One array with composition lists for MaybeYes characters which don't also have a mapping.
    *   Usually, MaybeYes characters don't have composition lists, so this array will usually be empty.
*   One array with
    *   Composition lists for YesYes characters which don't also have a mapping
    *   Mappings and optional composition lists for YesNo characters which do have a mapping

Threshold values like minYesNo depend on the mapping data.

The mapping string is stored together with the trailing ccc (tccc) value, and also with the ccc and lccc values if they are not 0.

Mapping to an empty string is encoded as a regular mapping with length 0.

*   formatVersion 3 stores explicit worst-case values lccc=1 and tccc=255.
*   formatVersion 1 & 2 store ccc=lccc=tccc=0, and the worst-case values are computed at runtime.

If both a mapping and a composition list are stored for a character (only possible for YesNo), the mapping comes first.

*   In formatVersion 2+, the trie value thresholds indicate whether there is a composition list.
*   In formatVersion 1, a bit in the first word indicates that there is a composition list.

Optional mapping

*   First unit
    *   Bits 15..8: tccc
    *   Bit 7: Has another data word with ccc & lccc (otherwise they are 0) (`MAPPING_HAS_CCC_LCCC_WORD`)
    *   Bit 6:
        *   formatVersion 2+: Has a raw mapping that differs from the recursively-decomposed mapping (`MAPPING_HAS_RAW_MAPPING`)
        *   formatVersion 1: Has composition list (`MAPPING_PLUS_COMPOSITION_LIST`)
            *   Not actually needed in recomposition because the compositeAndFwd result includes this flag.
            *   The only use for this flag was in Normalizer2Impl::hasCompBoundaryAfter().
            *   formatVersion 2 sets bit 5 (`MAPPING_NO_COMP_BOUNDARY_AFTER`) when the original character has a composition list (combines-forward), freeing up this bit for `MAPPING_HAS_RAW_MAPPING`.
    *   Bit 5: Reserved (zero) since formatVersion 3.
        *   formatVersion 1 & 2: Does not have a composition boundary after its original character. (`MAPPING_NO_COMP_BOUNDARY_AFTER`)
        *   This bit is set if the mapping has no starters or if the last starter in the mapping can combine-forward with another character following the mapping.
        *   formatVersion 2: This bit is also set if the mapping length is 0 (the original character is deleted) or if the original character combines-forward (i.e., there is a composition list), so that those conditions need not be tested separately at runtime.
    *   Bits 4..0: Length of the mapping, 0..31
*   Optional ccc/lccc word, only stored if bit 7 of the first unit is set (`MAPPING_HAS_CCC_LCCC_WORD`)
    *   Bits 15..8: lccc
    *   Bits 7..0: ccc
    *   formatVersion 1: The ccc/lccc word was stored as a "second unit" between the "first unit" and the mapping string.
    *   formatVersion 2+: The ccc/lccc word is stored immediately before the "first unit". The mapping string always follows immediately after the first unit.
*   `<length>` UChars with the mapping string
*   formatVersion 2+ also stores a raw mapping if bit 6 of the first unit is set (`MAPPING_HAS_RAW_MAPPING`)
    *   The raw mapping is stored immediately before the ccc/lccc word (if present) or the first unit (if there is no ccc/lccc word).
    *   It is stored as either one 16-bit unit "rm0" (which is 0x20..0xffff) or as the raw mapping string followed by a 16-bit unit containing its length (1..0x1f).
    *   If "rm0" is stored, then the raw mapping is the character rm0 (raw mapping index 0) followed by all but the first two code units in the regular mapping string.

Optional composition list

*   Logically, a list of pairs (combining-back code point, compositeAndFwd) where compositeAndFwd is a combination of the composite (bits 21..1) and bit 0 which is set if the indicates composite combines-forward.
*   Physically, a list of pairs or triples of 16-bit units, sorted in ascending order of combining-back code points.
*   In the first unit, bit 15 is set in the last tuple. Bit 0 is set if it's a triple, otherwise it's a pair.
*   Combining-back code point 0..33ff, composite 0..7fff: pair
    *   First unit bits 14..1 contain the combining-back code point
    *   Second unit contains the compositeAndFwd
*   Combining-back code point 0..33ff, composite 8000..10ffff: triple
    *   First unit: same as for the previous case
    *   Second unit bits 15..6 are 0, bits 5..0 contain the compositeAndFwd's bits 21..16
    *   Third unit contains the compositeAndFwd's bits 15..0
*   Combining-back code point 3400..10ffff, composite 0..10ffff: triple
    *   First unit bits 14..1 contain 3400 plus the combining-back code point's bits 20..10 (total value 3400..383f)
    *   Second unit bits 15..6 contain the combining-back code point's bits 9..0
    *   The remaining second/third unit bits are the same as for the previous case

In the ICU implementation, it is ok to not store the ccc value directly in the lookup value for NoNo characters. When the quick check fails with YesNo, NoNo or MaybeYes, the surrounding sequence is decomposed, which does not use the original characters' ccc values. Composition then sees only YesYes and MaybeYes characters which do have their ccc values in the lookup value.

A composite that combines-forward has quick check flags YesNo, has a mapping, has ccc=0 (it's a starter) and lccc=0 (it composes from a starter plus another character) and has a composition list (it combines-forward).

Old vs. new: The old composition data uses combine-forward and combine-back indexes stored in the extra data next to the mapping. In the new data structure, the combine-forward index is replaced by appending the composition list after the mapping, and the combine-back index is replaced by searching in the list for the back-combining code point itself.

Multiple characters may share data when they have the same mappings. This is especially useful for mappings to empty strings and case and other foldings.

### Raw decomposition mappings

It is sometimes useful to have access to the raw/original decomposition mappings, without recursive decomposition.

*   It would be nice if we could build Normalizer2 data at runtime; for that, we would want to start from the raw/original mappings. ([ticket #7743](https://unicode-org.atlassian.net/browse/ICU-7743))
    *   We would also want API to find out if a mapping round-trips; best like getCompositionQuickCheck(c).
    *   Note: For standard Unicode data, we already have properties and getters for NFC\_QC and NFKC\_QC. We might not need public API for getCompositionQuickCheck(c) for custom data.
*   The raw/original mappings might also be useful for text layout engines. ([ticket #8804](https://unicode-org.atlassian.net/browse/ICU-8804))
*   The raw mapping from an NFKC instance corresponds to the Unicode Decomposition\_Mapping (dm) property.
*   The raw mapping from an NFC instance corresponds to the Unicode Decomposition\_Mapping (dm) property for characters with Decomposition\_Type (dt) = Canonical (Can).

Some of this data is computable from the current structure, by mapping and then recomposing until the original character would be written. However, this would be expensive and incomplete.

.nrm formatVersion 2 adds compressed data for raw mappings:

*   An algorithmic mapping is itself the raw mapping.
*   In most other cases, the normal mapping has not been recursively decomposed and is the same as the raw mapping.
*   If the normal mapping has been recursively decomposed, then the `MAPPING_HAS_RAW_MAPPING` is set in the extra data, and the raw mapping is stored there as well. Some of those mappings are further compressed; for details see the previous section.

This required an incompatible data format change because in the formatVersion 1 "extra data" for each code point there was not one unused bit. However, the first unit's bit 6 (`MAPPING_PLUS_COMPOSITION_LIST`) was underutilized. It was easy to subsume its actual use into bit 5 (`MAPPING_NO_COMP_BOUNDARY_AFTER`), freeing bit 6 for its new use as the `MAPPING_HAS_RAW_MAPPING` flag. As a result, access to the raw mapping is only slightly slower (due to the "compressed" format) than to the normal mapping.

## Size vs. Speed

### Minimal Mappings vs. Recursively Decomposed

The old data always stores decomposition mappings that cannot be further composed. The builder (gennorm) recursively decomposes each "raw" input mapping with each other applicable mapping. As a result, the decomposition is very fast.

For a smaller data file, it would be possible to store only the "raw" input mappings, without having recursively decomposed them. For example, the Angstrom character could have just the mapping to A-ring, and only A-ring would have the mapping to A+ring. For reasonable performance, one more bit could be stored with each mapping, indicating whether the mapping can be decomposed further. (Code optimization: 1:1 mappings could use a loop, not recursion.) There should also be a flag in the data file for whether all mappings are recursively resolved, or not.

This would especially help with the data size when compatibility mappings and case foldings are included. Many of those are one-way mappings to a single code point each (1:1 mappings).

Note: Extracting FCD data when the mappings are not already recursively decomposed would require full decomposition because the mapping's lccc and tccc values may not be those of the full decomposition.

Not-fully-decomposed 1:n mappings would significantly complicate the runtime code.

Even if the builder does not store recursively decomposed mappings, it should still compute them to check against cycles, and maybe to check against large recursion depths.

(**Implemented in ICU 4.4, formatVersion 1.0**: Mappings are recursively decomposed, favoring performance. **ICU 49, formatVersion 2.0**, adds additional, compressed data to recover the raw mappings efficiently and with little size overhead.)

### Optimized 1:1 Mappings

The subset of NoNo characters with 1:1 mappings are the most attractive for compact storage because there is relatively a lot of overhead in storing them like sequences. Ideas:

*   In particular, the (usually large) gap between NoNo and MaybeYes mapping indexes into extra data can be used for algorithmic 1:1 mappings.
    *   Store a small delta directly in the 16-bit lookup value. Deltas would be at least -0x20..+0x20 to cover common case mappings. An additional bit for "decomposes further" could be included.
    *   Store a BMP code point directly in the 16-bit lookup value. For example, ASCII or Latin characters and Katakana/Han/Hangul characters that are often the target for case foldings and compatibility mappings.
    *   Store an index into a more compact data structure.
*   Use impossible bit combinations in the extra data for alternate encodings.
    *   A NoNo character cannot combine-forward. If its extra data has the combine-forward bit set, the data structure could be different. One possibility: Store a one-character mapping in that first unit. Could store 0..0x33ff and the "decomposes further" bit; could store larger code points in two units, with the upper 7 bits added to 0x3400.
    *   A YesNo character cannot have non-zero ccc and lccc. If its extra data has the has-ccc/lccc bit set, the data structure could be different.

**Implemented in ICU 4.4, formatVersion 1.0**: Small deltas of -0x40..+0x40 (where 0 is forbidden) for one-way mappings from ccc=0 characters are stored directly in the trie lookup value, see the table above with the 16-bit value ranges.

*   formatVersion 3: A delta mapping may only lead to a YesNo character with ccc=0, so there is a composition boundary before it, and the runtime code never needs to loop. An ASCII character only maps to another ASCII character.
*   formatVersion 1 & 2: A delta mapping may lead to another NoNo or YesNo character, so the runtime code must loop.
*   All other mappings are stored in the extraData and are fully decomposed.

_We should measure_ how much of the data size we can save, compared with the additional code complexity and lower performance to handle more variants. (Measured: With Unicode 5.2 NFKC + case folding, a simple, fairly small delta of ±0x40 reduced the data file size by 8.8%. Smaller deltas yielded smaller savings. A much larger delta of ±0x1000 only got to 9.3%, so ±0x40 was chosen for formatVersion 1.0.)

## Mapping Table Text Files

Normalization data is built from text files. Basic syntax is supported starting in ICU 4.4. The supported syntax is documented in the [User Guide Normalization chapter](../../userguide/transforms/normalization/).

### Not implemented yet

The following syntax elements might be useful but are not yet supported.

It might be useful to be able to set the override mode, and to start a new "phase" within one source file where it is allowed to override mappings from previous phases:

```
* override previous # none|any|previous
```

It might be useful to add syntax for a "filter" set and a validity set (optional). The syntax needs to indicate type, start and end of the set and allow multiple lines. Line breaks will be removed before passing the set pattern into the UnicodeSet constructor. If a filter set is defined, then the generator tool would ignore any data for code points outside that set.

```
* filter [:age=3.2:]
* filter [:^
*+         Hani
*+        :]
```

Some syntax might be useful for the Unicode version (optional). Multiple version specifications are ok only if they have the same value. The Unicode version is otherwise settable via a command-line option.

```
* unicode 5.2
```

It might be useful to add syntax for whether Hangul syllables and conjoining Jamo behave as defined in the Unicode standard. By default, they do so, and data for them is not allowed.

```
* hangul off
```

## New gennorm2 Tool

gennorm generates text files (nfc.txt and nfkc.txt) from UCD files, for standard Unicode normalization. The output text files are checked into [icu4c/source/data/unidata/norm2](https://github.com/unicode-org/icu/tree/main/icu4c/source/data/unidata/norm2). gennorm has been moved to the tools repository tree.

gennorm2 is a new, installable (ICU 4.4) end-user tool in the icu repository tree, parsing the .txt files and generating .nrm files.

It should be easy to include the standard Unicode normalization ccc and composition data. One way is to always include the standard ccc data and have syntax for a flag to request standard compositions. (**Rejected.**)

Another, simpler way is for gennorm2 to take a list of mapping table files, and to provide standard files like ccc.txt, compose.txt, nfd.txt, nfkd.txt and casefold.txt that could be combined (with or without additional custom tables) in various combinations into one binary data file. This would also allow for a character to have different mappings in different files, and the later mapping would override the earlier one. gennorm2 should be able to also output a .txt file with all of the combined data, except without recursively resolved mappings, to keep two-way mappings in the file valid for input. (**Done in ICU 4.4.** _Modification:_ The NFKC mappings cannot simply add to the NFC mappings because some characters with two-way NFC mappings have one-way NFKC mappings. Therefore, there are separate files that specify each normalization form's mappings.)

We should make it easy to move StringPrep mappings from the .spp files into normalization .txt/.nrm files. (**Not done** (yet).)