---
layout: default
title: ICU Code Point Tries
parent: Data Structures
grand_parent: Design Docs
---

<!--
© 2016 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# ICU Code Point Tries
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Fast lookup in arrays

For fast lookup by code point, we store data in arrays. It costs too much space
to use a single array indexed directly by code points: There are about 1.1M of
them (max 0x10ffff, about 20.1 bits), and about 90% are unassigned or private
use code points. For some uses, there are non-default values only for a few
hundred characters.

We use a form of "trie" adapted to single code points. The bits in the code
point integer are divided into two or more parts. The first part is used as an
array offset, the value there is used as a start offset into another array. The
next code point bit field is used as an additional offset into that array, to
fetch another value. The final part yields the data for the code point.
Non-final arrays are called index arrays or tables.

> See also [ICU String Tries](tries/index.md).

For lookup of arbitrary code points, we need at least three successive arrays,
so that the first index table is not too large.

For all but the first index table, different blocks of code points with the same
values can overlap. A special block contains only default values and is shared
among all blocks of code points that map there.

Block sharing works better, and thus leads to smaller data structures, the
smaller the blocks are, that is, the fewer bits in the code point bit fields
used as intra-block offsets.

On the other hand, shorter bit fields require more bit fields and more
successive arrays and lookups, which adds code size and makes lookups slower.

(Until about 2001, all ICU data structures only handled BMP code points.
"Compact arrays" split 16-bit code points into fields of 9 and 7 bits.)

We tend to make compromises including additional index tables for smaller parts
of the Unicode code space, for simpler, faster lookup there.

For a general-purpose structure, we want to be able to be able to store a unique
value for every character. This determines the number of bits needed in the last
index table. With 136,690 characters assigned in Unicode 10, we need at least 18
bits. We allocate data values in blocks aligned at multiples of 4, and we use
16-bit index words shifted left by 2 bits. This leads to a small loss in how
densely the data table can be used, and how well it can be compacted, but not
nearly as much as if we were using 32-bit index words.

## Character conversion

The ICU conversion code uses several variants of code point tries with data
values of 1, 2, 3, or 4 bytes corresponding to the number of bytes in the output
encoding.

## UTrie

The original "UTrie" structure was developed for Unicode Normalization for all
of Unicode. It was then generalized for collation, character properties, and
eventually almost every Unicode data lookup. Values are 16 or 32 bits wide.

It was designed for fast UTF-16 lookup with a special, complicated structure for
supplementary code points using custom values for lead surrogate units. This
custom data and code made this structure relatively hard to use.

11:5 bits for the BMP and effectively 5:5:5:5 bits for supplementary code points
provide for good compaction. The BMP index table is always 2<sup>11</sup> uint16_t = 4kB.
Small index blocks for the supplementary range are added as needed.

The structure stores different values for lead surrogate code *units* (for fast
moving through UTF-16_ vs. code *points* (for lookup by code point).

The first 256 data table entries are a fixed-size, linear table for Latin-1 (up
to U+00FF).

## UTrie2

The "UTrie2" structure, developed in 2008, was designed to enable fast lookup
from UTF-8 without always having to assemble whole code points and to split them
again into the trie bit fields.

It retains separate lookups for lead surrogate code units vs. code points.

It retains the same 11:5 lookup for BMP code points, for good compaction and
good performance.

There is a special small index for lead bytes of two-byte UTF-8 sequences (up to
U+07FF), for 5:6 lookup. These index values are not shifted left by 2.

Lookup for three-byte UTF-8 uses the BMP index, which is clumsy.

Lookup for supplementary code points is much simpler than with UTrie, without
custom data values or code. Two index tables are used for 9:6:5 code point bits.
The first index table omits the BMP part. The structure stores the a code point
after which every one maps to the default value, and the first index is
truncated to below that.

With the fixed BMP index table and other required structures, an empty UTrie2 is
about 5kB large.

The UTF-8 lookup was also designed for the original handling of ill-formed
UTF-8: The first 192 data table entries are a linear table for ASCII plus the 64
trail bytes, to look up "single" bytes 0..BF without further checking, with
error values for the trail bytes. Lookup of two-byte non-shortest forms (C0
80..C1 BF) also yields error values. These error values became unused in 2017
when ICU 60 changed to handling ill-formed UTF-8 compatible with the W3C
Encoding standard (substituting maximal subparts of valid sequences). C0 and C1
are no longer recognized as lead bytes, requiring full byte sequence validation
separate from the data lookup.

## Ideas

Possible goals: Simpler code, smaller data especially for sparse tries, maybe
faster UTF-8, not much slower UTF-16.

We should try to store only one set of values for surrogates. Unicode property
APIs use only by-code point lookup without special lead surrogate values.
Collation uses special lead surrogate data but does not use code point lookup.
Normalization does both, but the per-code point lookup could test for surrogate
code points first and return trivial values for all of them. UTF-16 string
lookup should map unpaired surrogates to the error value.

We should remove the special data for old handling of ill-formed UTF-8, the
error values for trail bytes and two-byte non-shortest forms.

If we use 6 bits for the last code point bit field, then we can use the same
index table for code point/UTF-16 lookup as well as UTF-8 lookup. Compaction
will be less effective, so data will grow some. This would be somewhat
compensated by the smaller BMP index table.

If we also continue to use 6 bits for the second-to-last table, that is, 8:6:6
bits, then we can simplify the code for three- and four-byte UTF-8.

If we always include the BMP in the first index table, then we can also simplify
enumeration code a bit, and use smaller code for code point lookups where code
size is more important than maximum speed.

Alternatively, we could improve compaction and speed for the BMP by using no
index shift-left for BMP indexes (and keep omitting the BMP part of the first
index table). In order to ensure that BMP data can be indexed directly with
16-bit index values, the builder would probably have to copy at least the BMP
data into a new array for compaction, before adding data for supplementary code
points. When some of the indexes are not shifted, and their data is compacted to
arbitrary offsets, then that data cannot also be addressed with uniform
double-index lookup. We may or may not store unused first-index entries. If not
the whole BMP is indexed differently, then UTF-16 and three-byte UTF-8 lookups
need another code branch. (Size vs. simplicity & speed.)

The more tries we use, the higher the total cost of the size overhead. (For
example, many of the 100 or so collation tailorings carry a UTrie2.) The less
overhead, the more we could use separate tries where we currently combine them
or avoid them. Smaller overhead would make it more attractive to offer a public
code point map structure.

Going to 10:6 bits for the BMP cuts the fixed-size index in half, to 2kB.

We could reduce the fixed-size index table much further by using two-index
lookup for some or most of the BMP, trading off data size for speed and
simplicity. The index must be at least 32 uint16_t's for two-byte UTF-8, for up
to U+07FF including Cyrillic and Arabic. We could experiment with length 64 for
U+0FFF including Indic scripts and Thai, 208 entries for U+33FF (most small
scripts and Kana), or back to 1024 entries for the whole BMP. We could configure
a different value at build time for different services (optimizing for speed vs.
size). If we use the faster lookup for three-byte UTF-8, then the boundaries
should be multiples of 0x1000 (up to U+3FFF instead of U+33FF).

## UCPTrie / CodePointTrie

Added as public API in ICU 63. Developed between the very end of 2017 and
mid-2018.

Based on many of the ideas above and experimentation.

Continued linear data array lookup for ASCII.

No more separate values for lead surrogate code points vs. code units.

*   Normalization switched to UCPTrie, working around this: Storing special lead
    surrogate values for UTF-16 forward iteration; for code point lookup, the
    normalization code checks for lead surrogates and returns an "inert" value
    for them; for code point range iteration uses special API that treats lead
    surrogates as "inert" as well.
*   Otherwise simpler API, easier to explain.
*   UTF-16 string lookup maps unpaired surrogates to the error value.

For low-code point lookup, uses 6 bits for the last code point field.

*   No more need for special UTF-8 2/3-byte lookup structures.
*   Smaller BMP index reduces size overhead.

No more data structures for non-shortest UTF-8 sequences.

"Fast" type uses two-stage lookup for all of the BMP (10:6 bits). "Small" type
uses two-stage lookup only up to U+0FFF to trade off size vs. speed. (fastLimit
U+10000 vs. U+1000)

Continued use of highStart for the start of the last range (ending at U+10FFFF),
and highValue for the value of all of its code points.

For code points between fastLimit and highStart, a four-stage lookup is used
(compared with three stages in UTrie2), with small bit fields (6:5:5:4 bits).
"Fast" type: Only for supplementary code points below highStart, if any. "Small"
type: For all code points below highStart; this means that for U+0000..U+0FFF in
a "small" trie data can be accessed with either the two-stage or the four-stage
lookup (and for ASCII also with linear access).

Experimentation confirmed that larger bit fields, especially for the last one or
two stages, lead to poor compaction of sparse data. 6 bits for the data offset
work well for UTF-8 lookup and are a reasonable compromise for the BMP, but for
the large supplementary area which tends to have more sparse data, using a 4 bit
data offset was useful. The drawback is that then the index blocks get larger
and compact less well. Four-byte UTF-8 lookup (for supplementary

*   Started with 8:6:6 bits, but some tries were 30% larger than with UTrie2.
*   Went to 10:6:4 bits which saved 12% overall, with only one trie larger than
    UTrie2 (by 8%).
*   Experimented with a "gap", omitting parts of the index for another range
    like highStart for a typically large range of code points with a single
    common value. This helped
*   Experimented with 10:6:4 vs. 11:5:4 vs. 9:6:5 vs. 10:5:5 bits plus the gap.
    \*:4 were smaller than \*:5, but the bit distribution within the index
    stages had little effect. 11:5:4 yielded the smallest data, indicating that
    small bit fields are useful for index stages as well.
*   Replaced the gap with splitting the first index bit field into two, for a
    four-stage 6:5:5:4 lookup. Just slightly smaller data than 11:5:4+gap, but
    less complicated than checking for the gap and working around it; replaces
    gap start/limit reads and comparisons with unconditional index array
    accesses. 14% smaller overall than UTrie2.
*   Added the "small" type where the two-stage lookup only extends to U+0FFF
    (6:6 bits) and the four-stage lookup covers all code points below highStart.
    34% smaller overall than UTrie2.

The normalization code also lazy-builds a trie with CanonicalIterator data which
is very sparse even in the BMP. With a "fast" UCPTrie it is significantly larger
than with UTrie2, with a "small" UCPTrie it is significantly smaller. Switched
the code to use a "small" trie because it is less performance-sensitive than the
trie used for normalizing strings.

In order to cover up to 256k data values, UTrie2 always shifts 16-bit data block
start offsets left by 2. UCPTrie abandons this, which simplifies two-stage
lookups slightly and improves compaction (no more granularity of 4 for data
block alignment).

*   For a "fast" trie to always reach all BMP data values with 16-bit index
    entries, the data array is always accessed via a separate pointer, rather
    than UTrie2's sharing of the index array with 16-bit data via offsetting by
    the length of the index. This also simplifies code slightly and makes access
    uniform for all data value widths.
*   There are now at most 64k data values for BMP code points because there is
    no separate data for lead surrogates any more. The builder code writes data
    blocks in code point order to ensure that low code points have low data
    block offsets.
*   For supplementary code points, data block offsets may need 18 bits. This is
    very unusual but possible. (It currently happens only in the collation root
    data with Han radical-stroke order, and in a unit test.)
*   UCPTrie uses the high bit of the index-2 entry to indicate that the index-3
    block stores 18-bit data block offsets rather than 16-bit ones. (This limits
    somewhat the length of the index.) In this case, groups of 8 index-3 entries
    (= data block start offsets) share an additional entry that stores the two
    high bits of each of the eight entries. More complicated lookup, but almost
    never used, and keeps BMP lookups always simple.
*   A possible alternative could have used a bit per entry, or per small group
    of entries, to indicate that a common data value should be returned for
    "unused" parts of a sparse data block. There could have been a common value
    per index-3 block, per index-2 block, or for the whole trie, etc. Rejected
    as much too complicated.

UTrie2 stores a whole block of 64 error values for UTF-8 non-shortest-form
lookup. UCPTrie does not have this block any more; it stores the error value at
the end of the data array, at dataLength-1.

UTrie2 stores the highValue at dataLength-4. UCPTrie stores it at dataLength-2.

Comparison: [UTrie2 vs.
UCPTrie/CodePointTrie](https://docs.google.com/document/d/e/2PACX-1vTbwdDe2tVJ6pACMpOq7uKW_FgvyyjvPVdgZYsIwSoFJj-27cXR20wAO9qHVoaKOIoo-d8iHnsFOCdc/pub)

Sizes for BreakIterator & Collator tries, UTrie2 vs. UTrie3 experiments: [In
this
spreadsheet](https://docs.google.com/spreadsheets/d/e/2PACX-1vTgL260NFgmbiUAtptKj4fNf9wNm-OJ6Q0TbWzFWvhV7wVZk2Qe-gk2pbJh0pHY9XVsObZ3YaoOnb3I/pubhtml)
see the "nocid" sheet (no CanonicalIterator data).

The last columns on the "nocid" sheet, highlighted in green and blue, correspond
to the final UCPTrie/CodePointTrie. For these tries, the "fast" type (green)
yields 14% smaller data than UTrie2; the "small" type (blue) yields 34% smaller
data.

The simplenormperf sheets show performance comparison data between UTrie2 and
"fast" UCPTrie. There should be little difference for BMP characters; the
numbers are too inconsistent to show a significant difference.

UCPTrie has an option of storing 8-bit values, in addition to 16-bit and 32-bit
values that UTrie2 supports. It would be possible to add 12-bit or 64-bit values
etc. later.
