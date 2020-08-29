---
layout: default
title: Conversion Data
nav_order: 2
parent: Conversion
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Conversion Data
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Introduction

### Algorithmic vs. Data-based

In a comprehensive conversion library, there are three kinds of codepage
converter implementations: converters that use algorithms, mapping data, or
those converters that use both.

1.  Most codepages have a simple and straightforward structure but have an
    arbitrary relationship between input and output character codes. Mapping
    tables are necessary to define the conversion. If the codepage characters
    use more than one byte each, then the mapping table must also define the
    structure of the codepage.

2.  Algorithmic converters work by transforming the input stream with built-in
    algorithms and possibly small, hard coded tables. The conversion can be
    complex, but the actual mapping of a character code is done numerically if
    the converter is purely algorithmic.

3.  In some cases, a converter needs to be algorithmic for its basic operations
    but also relies on mapping data.

ICU provides converter implementations for all three groups of codepages. Since
ICU always converts, to or from Unicode, the purely algorithmic converters are
the ones for Unicode encodings (such as UTF-8, UTF-16BE, UTF-16LE, UTF-32BE,
UTF-32LE, SCSU, BOCU-1 and UTF-7). Since Unicode is based on US-ASCII and
ISO-8859-1 ("ISO Latin-1"), these encodings also use algorithmic converters for
performance reasons.

Most other codepages use simple byte sequences but are not encodings of Unicode.
They are converted with generic code using mapping data tables. ICU also
supports a few encodings, like ISO-2022 and its variants, that employ an
algorithmic structure to switch between a set of codepages. The converters for
these encodings are algorithmic but use mapping tables for the embedded
codepages.

### Stateful vs. Stateless

Character encodings are either stateful or stateless:

1.  Stateless encodings define a byte sequence for each character. Complete
    character byte sequences can be used in any order, and the same complete
    character byte sequences always encodes the same characters. It is
    preferable to always encode one character using the same byte sequence.

2.  Stateful encodings define byte sequences that change the state of the text
    stream. Depending on the current state, the same byte sequence may encode a
    different character and the same character may be encoded with different
    byte sequences.

This distinction between stateless and stateful encodings is important, because
it determines if any available ICU converter implementation is used. The
following are some more important considerations related to stateless versus
stateful encodings:

1.  A runtime converter object is always stateful, even for "stateless"
    encodings. They are always stateful because an input buffer may end with a
    partial byte sequence that is to be continued in the next input buffer in
    the following conversion call. The information about this is stored in the
    converter object. Similarly, if the input is Unicode text, then an input
    buffer may end with the first of a pair of surrogates. The converter object
    also stores overflow bytes or code units if the result of a character
    mapping did not fit entirely into the output buffer.

2.  Stateless encodings are stateful in our converter implementation to
    interpret "complete byte sequences". They are "stateful" because many
    encodings can have the same byte value used in different positions of byte
    sequences for different characters; a specific byte value may be a lead byte
    or a trail byte. For instance, the lead and trail byte values overlap in
    codepages like Shift-JIS. If a program does not start reading at a character
    boundary, it may instead interpret the byte sequences from two or more
    separate characters as one character. Often, character boundaries can be
    detected reliably only by reading the non-Unicode text linearly from the
    beginning. This can be a problem for non-Unicode text processing, where text
    insertion, deletion, and searching are common. The UTF-8/16/32 encodings do
    not have this problem because the single, lead, or trail units have disjoint
    values and character boundary can be easily found.

3.  Some stateful encodings only switch between two states: one with one byte
    per character and one with two bytes per character. This type of encoding is
    very common in mainframe systems based on Extended Binary Coded Decimal
    Interchange Code (EBCDIC) and is actually handled in ICU with almost the
    same code and type of mapping tables as stateless codepages.

4.  The classifications of algorithmic vs. data-based converters and of
    stateless vs. stateful encodings are independent of each other: UTF-8,
    UTF-16, and UTF-32 encodings are algorithmic but stateless; UTF-7 and SCSU
    encodings are algorithmic and stateful; Windows-1252 and Shift-JIS encodings
    are data-based and stateless; ISO-2022-JP encoding is algorithmic,
    data-based, and stateful.

### Scope of this chapter

The following sections in this chapter discuss the mapping data tables that are
used in ICU. For related material, please see:

1.  [ICU character set collection](http://icu-project.org/charts/charset/)

2.  [Unicode Technical Report 22](http://www.unicode.org/reports/tr22/)

3.  "Cross Mapping Tables" in [Unicode Online
    Data](http://www.unicode.org/onlinedat/online.html)

## ICU Mapping Table Data Files

### Overview

As stated above, most ICU converters rely on character mapping tables. ICU 1.8
has one single data structure for all character mapping tables, which is used by
a generic Multi-Byte Character Set (MBCS) converter implementation. The
implementation is flexible enough to handle stateless encodings with the
following parameters:

1.  Support for variable-length, byte-based encodings with 1 to 4 bytes per
    character.

2.  Support for all Unicode characters (code points 0..0x10ffff). Since ICU 1.8
    uses the UTF-16 encoding as its Unicode encoding form, surrogate pairs are
    completely supported.

3.  Efficient distinction between unassigned (unmappable) and illegal byte
    sequences.

4.  It is not possible to convert from Unicode to byte sequences with leading
    zero bytes.

5.  Simple stateful encodings are also handled using only Shift-In and Shift-Out
    (SI/SO) codes and one single-byte and one double-byte state.

> :point_right: **Note**: *In the context of conversion tables, "unassigned" code
> points or codepage byte sequences are valid but do not have a **mapping**. This
> is different from "unassigned" code points in a character set like Unicode or
> Shift-JIS which are codes that do not have assigned **characters**.*

Prior to version 1.8, ICU used more specific, more limited, converter
implementations for Single Byte Character Set (SBCS), Double Byte Character Set
(DBCS), and the stateful Extended Binary Coded Decimal Interchange Code (EBCDIC)
codepages. Mapping table data is provided in text files. ICU comes with several
dozen .ucm files (UniCode Mapping, in icu/source/data/mappings/) that are
translated at build time by its makeconv tool (source code in
icu/source/tools/makeconv). The makeconv tool writes one binary, memory-mappable
.cnv file per .ucm file. The resulting .cnv files are included by default in the
common data file for use at runtime.

The format of the .ucm files is similar to the format of the UPMAP files as
provided by IBM® in the codepage repository and as used in the uconvdef tool on
AIX. UPMAP is a text file that specifies the mapping of a codepage character to
and from Unicode.

The format of the .cnv files is ICU-specific. The .cnv file format may change
between ICU versions even for the same .ucm files. The .ucm file format may be
extended to include more features.

The following sections concentrate on the .ucm file format. The .cnv file format
is described in the source code in the `icu/source/common/ucnvmbcs.c` directory
and is updated using the MBCS converter implementation.

These conversion tables can have more than one name. ICU allows multiple names
("aliases") for the same encoding. It matches a requested encoding name against
a list of names in `icu/source/data/mappings/convrtrs.txt` and when it finds a
match, ICU opens a converter with the name in the leftmost position in the
matching line. The name matching is not case-sensitive and ICU ignores spaces,
dashes, and underscores. At build time, the gencnval tool located in the
`icu/source/tools/gencnval` directory, generates a binary form of the convrtrs.txt
file as a data file for runtime for the cnvalias.icu file ("Converter Aliases
data file").

### .ucm File Format

.ucm files are line-oriented text files. Empty lines and comments starting with
'`#`' are ignored.

A .ucm file contains two sections:

1.  a header with general specifications of the codepage

2.  a mapping table section between the "CHARMAP" and "END CHARMAP" lines.

For example:

```
<code_set_name>               "IBM-943"
<char_name_mask>              "AXXXX"
<mb_cur_min>                  1
<mb_cur_max>                  2
<uconv_class>                 "MBCS"
<subchar>                     \xFC\xFC
<subchar1>                    \x7F
<icu:state>                   0-7f, 81-9f:1, a0-df, e0-fc:1
<icu:state>                   40-7e, 80-fc
#
CHARMAP
#
#
#ISO 10646      IBM-943
#_________      _________
<U0000> \x00 |0
<U0001> \x01 |0
<U0002> \x02 |0
<U0003> \x03 |0
...
<UFFE4> \xFA\x55 |1
<UFFE5> \x81\x8F |0
<UFFFD> \xFC\xFC |2
END CHARMAP

```

The header fields are:

1.  code_set_name - The name of the codepage. The makeconv tool generates the
    .cnv file name from the .ucm filename but uses this header field for the
    converter name that it writes into the .cnv file for ucnv_getName. The
    makeconv tool prints a warning message if this header field does not match
    the file name. The file name is not case-sensitive.

2.  char_name_mask - This is ignored by makeconv tool. "AXXXX" specifies that
    the POSIX-style character "name" consists of one letter (Alpha) followed by
    4 hexadecimal digits. Since ICU only uses Unicode character "names" (for
    example, code points) the format is fixed (see below).

3.  mb_cur_min - The minimum number of bytes per character.

4.  mb_cur_max - The maximum number of bytes per character.

5.  uconv_class - This can be either "SBCS", "DBCS", "MBCS", or
    "EBCDIC_STATEFUL"
    The most general converter class/type/category is MBCS, which requires that
    the codepage structure has the following <icu:state> lines. The other types
    of converters are subsets of MBCS. The makeconv tool uses predefined state
    tables for these other converters when their structure is not explicitly
    specified. The following describes how the converter types are interpreted:

    a.  MBCS: Generic ICU converter type, requires a state table

    b.  SBCS: Single-byte, 8-bit codepages

    c.  DBCS: Double-byte EBCDIC codepages

    d.  EBCDIC_STATEFUL: Mixed Single-Byte or Double-Byte EBCDIC codepages (stateful, using SI/SO)

The following shows the exact implied state tables for non-MBCS types. A state
table may need to be overwritten in order to allow supplementary characters
(U+10000 and up).

1.  subchar - The substitution character byte sequence for this codepage. This sequence must be a valid byte sequence according to the codepage structure.

2.  subchar1 - This is the single byte substitution character when subchar is defined. Some IBM converter libraries use different substitution characters for "narrow" and "wide" characters (single-byte and double-byte). ICU uses only one substitution character per codepage because it is common industry  practice.

3.  icu:state - See the "State Table Syntax in .ucm Files" section for a  detailed description of how to specify a codepage structure.

4.  icu:charsetFamily - This specifies if the codepage is ASCII or EBCDIC based.

The subchar and subchar1 fields have been known to cause some confusion. The
following conditions outline when each are used:

1.  Conversion from Unicode to a codepage occurs and an unassigned code point is
    found

    a.  If a subchar1 byte is defined and a subchar1 mapping is defined for the code point (with a |2 precision indicator),
        output the subchar1

    b.  Otherwise output the regular subchar

2.  Conversion from a codepage to Unicode occurs and an unassigned codepoint is found

    a.  If the input sequence is of length 1 and a subchar1 byte is specified for the codepage, output U+001A

    b.  Otherwise output U+FFFD

In the CHARMAP section of a .ucm file, each line contains a Unicode code point
(like <U(*1-6 hexadecimal digits for the code point*)> ), a codepage character
byte sequence (each byte like `\xhh` (2 hexadecimal digits) ), and an optional
"precision" or "fallback" indicator.

The precision indicator either must be present in all mappings or in none of
them. The indicator is a pipe symbol `|` followed by a 0, 1, 2, 3, or 4 that has
the following meaning:

*   `|0` - A "normal", roundtrip mapping from a Unicode code point and back.
*   `|1` - A "fallback" mapping only from Unicode to the codepage, but not back.
*   `|2` - A subchar1 mapping. The code point is unmappable, and if a substitution
    is performed, then the subchar1 should be used rather than the subchar.
    Otherwise, such mappings are ignored.
*   `|3` - A "reverse fallback" mapping only from the codepage to Unicode, but not
    back to the codepage.
*   `|4` - A "good one-way" mapping only from Unicode to the codepage, but not
    back.

Fallback mappings from Unicode typically do not map codes for the same
character, but for "similar" ones. This mapping is sometimes done if a character
exists in Unicode but not in the codepage. To replace it, ICU maps a codepage
code to a similar-looking code for human-readable output. This mapping feature
is not useful for text data transmission especially in markup languages where a
Unicode code point can be escaped with its code point value. The ICU application
programming interface (API) `ucnv_setFallback()` controls this fallback behavior.

"Reverse fallbacks" are technically similar, but the same Unicode character can
be encoded twice in the codepage. ICU always uses reverse fallbacks at runtime.

A subset of the fallback mappings from Unicode is always used at runtime: Those
that map private-use Unicode code points. Fallbacks from private-use code points
are often introduced as replacements for previous roundtrip mappings for the
same pair of codes. These replacements are used when a Unicode version assigns a
new character that was previously mapped to that private-use code point. The
mapping table is then changed to map the same codepage byte sequence to the new
Unicode code point (as a new roundtrip) and the mapping from the old private-use
code point to the same codepage code is preserved as a fallback.

A "good one-way" mapping is like a fallback, but ICU always uses "good one-way"
mappings at runtime, regardless of the fallback API flag.

The idea is that fallbacks normally lose information, such as mapping from a
compatibility variant of a letter to the ASCII version; however, fallbacks from
PUA and reverse fallbacks are assumed to be for "the same character", just an
older code for it.

Something similar happens with from-Unicode Variation Selector sequences. It is
possible to round-trip (`|0`) either the unadorned character or the sequence with
a variation selector, and add a "good one-way" mapping (`|4`) from the other
version. That "good one-way" mapping does not lose much information, and it is
used even if the "use fallback" API flag is false. Alternatively, both mappings
could be fallbacks (`|1`) that should be controlled by the "use fallback"
attribute.

### State table syntax in .ucm files

The conversion to Unicode uses a state machine to achieve the above capabilities
with reasonable data file sizes. The state machine information itself is loaded
with the conversion data and defines the structure of the codepage, including
which byte sequences are valid, unassigned, and illegal. This data cannot (or
not easily) be computed from the pure mapping data. Instead, the .ucm files for
MBCS encodings have additional entries that are specific to the ICU makeconv
tool. The state tables for SBCS, DBCS, and EBCDIC_STATEFUL are implied, but they
can be overridden (see the examples below). These state tables are specified in
the header section of the .ucm file that contains the `<icu:state>` element. Each
line defines one aspect of the state machine. The state machine uses a table of
as many rows as there are states (= as many as there are `<icu:state>` lines).
Each row has 256 entries; one for each possible byte value.

The state table lines in the .ucm header conform to the following Extended
Backus-Naur Form (EBNF)-like grammar (whitespace is allowed between all tokens):

```
row=[[firstentry ','] entry (',' entry)*]
firstentry="initial" | "surrogates"
           (initial state (default for state 0), output is all surrogate pairs)
```

Each state table row description (that follows the `<icu:state>`) begins with an
optional initial or surrogates keyword and is followed by one or more column
entries. For the purpose of codepage state tables, the states=rows in the table
are numbered beginning at 0 for the first line in the .ucm file header. The
numbers are assigned implicitly by the makeconv tool in order of the `<icu:state>`
lines.

A row may be empty (nothing following the `<icu:state>`) - that is equivalent to
"all illegal" or 0-ff.i and is useful for trail byte states for all-illegal byte
sequences.

```
entry=range [':' nextstate] ['.' [action]]
range     = number ['-' number]
nextstate = number (0..7f)
action    = 'u' | 's' | 'p' | 'i'
                (unassigned, state change only, surrogate pair, illegal)
number    = (1- or 2-digit hexadecimal number)
```

Each column entry contains at least one hexadecimal byte value or value range
and is separated by a comma. The column entry specifies how to interpret an
input byte in the row's state. If neither a next state nor an action is
explicitly specified (only the byte range is given) then the byte value
terminates the byte sequence, results in a valid mapping to a Unicode BMP
character, and resets the state number to 0. The first line with `<icu:state>` is
called state 0.

The next state can be explicitly specified with a separating colon ( : )
followed by the number of the state (=number/index of the row, starting at 0).
This specification is mostly used for intermediate byte values (such as bytes
that are not the last ones in a sequence). The state machine needs to proceed to
the next state and read another byte. In this case, no other action is
specified.

If the byte value(s) terminate(s) a byte sequence, then the byte sequence
results in the following depending on the action that is announced with a period
( . ) followed by a letter:

| letter | meaning |
|--|---------|
| u | Unassigned. The byte sequence is valid but does not encode a character. |
| none | (no letter) - Valid. If no action letter is specified, then the byte sequence is valid and encodes a Unicode character up to U+ffff |
| p | Surrogate Pair. The byte sequence is valid and the result may map to a UTF-16 encoded surrogate pair |
| i | Illegal. The byte sequence is illegal. This is the default for all byte values in a row that are not otherwise specified with column entries|
| s | State change only. The byte sequence does not encode any character but may change the state number. This may be used with simple, stateful encodings (for example, SI/SO codes), but currently it is not used by ICU.|

If an action is specified without a next state, then the next state number
defaults to 0. In other words, a byte value (range) terminates a sequence if
there is an action specified for it, or when there is neither an action nor a
next state. In this case, the byte value defaults to "valid, next state is 0"
(equivalent to :0.).

If a byte value is not specified in any column entry row, then it is illegal in
the current state. If a byte value is specified in more than one column entry of
the same row, then ICU uses the last state. These specifications allow you to
assign common properties for a wide byte value range followed by a few
exceptions. This is easier than having to specify mutually exclusive ranges,
especially if many of them have the same properties.

The optional keyword at the beginning of a state line has the following effect:

| keyword | effect |
|---------|--------|
| initial | The state machine can start reading byte sequences in this state. State 0 is always an initial state. Only initial states can be next states for final byte values. In an initial state, the Unicode mappings for all final bytes are also stored directly in the state table.
| surrogates | All Unicode mappings for final bytes in non-initial states are stored in a separate table of 16-bit Unicode (UTF-16) code units. Since most legacy codepages map only to Unicode code points up to U+ffff (the Basic Multilingual Plane, BMP), the default allocation per mapping result is one 16-bit unit. Individual byte values can be specified to map to surrogate pairs (= two 16-bit units) with action letter p. The surrogates keyword specifies the values for the entire state (row). Surrogate pair mapping entries can still hold single units depending on the actual mapping data, but single-unit mapping entries cannot hold a pair of units. Mapping to single-unit entries is the default because the mapping is faster, uses half as much memory in the code units table, and is sufficient for most legacy codepages.|

When converting to Unicode, the state machine starts in state number 0. In each
iteration, the state machine reads one input (codepage) byte and either proceeds
to the next state as specified, or treats it as a final byte with the specified
action and an optional non-0 next (initial) state. This means that a state table
needs to have at least as many state rows as the maximum number of bytes per
character, which is the maximum length of any byte sequence.

Exception: For EBCDIC_STATEFUL codepages, double-byte sequences start in state
1, with the SI/SO bytes switching from state 0 to state 1 or from state 1 to
state 0. See the default state table below.

### Extension and delta tables

ICU 2.8 adds an additional "extension" data structure to its conversion tables.
The new data structure supports a number of new features. When any of the
following features are used, then all mappings must use a precision indicator.

#### Converting multiple characters as a unit

Before ICU 2.8, only one Unicode code point could be converted to or from one
complete codepage byte sequence. The new data structure supports the conversion
between multiple Unicode code points and multiple complete codepage byte
sequences. (A "complete codepage byte sequence" is a sequence of bytes which is
valid according to the state table.)

Syntax: Simply write more than one Unicode code point on a mapping line, and/or
more than one complete codepage byte sequence. Plus signs (+) are optional
between code points and between bytes. For example,
ibm-1390_P110-2003.ucm contains

    <U304B><U309A> \xEC\xB5 |0

and test3.ucm contains

    <U101234>+<U50005>+<U60006> \x07+\x00+\x01\x02\x0f+\x09 |0

For more examples see the ICU conversion data and the
`icu/source/test/testdata/test*.ucm` test data files.

ICU 2.8 supports up to 19 UChars on the Unicode side of a mapping and up to 31
bytes on the codepage side.

The longest match possible is converted in order to properly handle tables where
the source sides of some mappings are prefixes of the source sides of other
mappings.

As a side effect, if conversion offsets are written and a potential match
crosses buffer boundaries, then some of the initial offsets for the following
output may be unknown (-1) because their input was stored in the converter from
a previous buffer while looking for a longer match.

Conversion tables for SI/SO-stateful (usually EBCDIC_STATEFUL) codepages cannot
include mappings with SI or SO bytes or where there are SBCS characters in a
multi-character byte sequence. In other words, for these tables there must be
exactly one byte in a mapping or else a sequence of one or more DBCS characters.

#### Delta (extension-only) conversion table files

Physically, a binary conversion table (.cnv) file automatically contains both a
traditional "base table" data structure for the 1:1 mappings and a new
"extension table" for the m:n mappings if any are encountered in the .ucm file.
An extension table can also be requested manually by splitting the CHARMAP into
two. The first CHARMAP section will be used for the base table, and the second
only for the extension table. M:n mappings in the first CHARMAP will be moved to
the extension table.

In order to save space for very similar conversion tables, it is possible to
create delta .cnv files that contain only an extension table and the name of
another .cnv file with a base table. The base file must be split into two
CHARMAPs such that the base file's base table does not contain any mappings that
contradict any of the delta file's mappings.

The delta (extension-only) file uses only a single CHARMAP section. In addition,
it nees a line in the header that both causes building just a delta file and
specifies the name of the base file. For example, windows-936-2000.ucm contains

    <icu:base> “ibm-1386_P100-2002”

makeconv ignores all mappings for the delta file that are also in the base
file's base table. If the two conversion tables are sufficiently similar, then
the delta file will contain only a relatively small set of mappings, which
results in a small .cnv file. At runtime, both the delta file and its base file
are loaded, and the base file's base table is used together with the extension
file. The base file works as a standalone file, using its own extension table
for its full set of mappings. The base file must be in the same ICU data package
as the delta file.

The hard part is to split the base file's mappings into base and extension
CHARMAPs such that the base table does not overlap with any delta file, while
all shared mappings should be in the base table. (The base table data structure
is more compact than the extension table data structure.)

ICU provides the ucmkbase tool in the
[ucmtools](https://github.com/unicode-org/icu-data/tree/master/charset/source/ucmtools)
collection to do this.

For example, the following illustrates how to use ucmkbase to make a base .ucm
file for three Shift-JIS conversion table variants. (ibm-943_P15A-2003.ucm
becomes the base.)

```
C:\tmp\icu\ucm>ren ibm-943_P15A-2003.ucm ibm-943_P15A-2003.orig
C:\tmp\icu\ucm>ucmkbase ibm-943_P15A-2003.orig ibm-943_P130-1999.ucm ibm-942_P12A-1999.ucm > ibm-943_P15A-2003.ucm
```

After this, the two delta .ucm files only need to get the following line added
before the start of their CHARMAPs:

```
<icu:base> "ibm-943_P15A-2003"
```

The ICU tools and runtime code handle DBCS-only conversion tables specially,
allowing them to be built into delta files with MBCS or EBCDIC_STATEFUL base
files without using their single-byte mappings, and without ucmkbase moving the
single-byte mappings of the base file into the base file's extension table. See
for example ibm-16684_P110-2003.ucm and ibm-1390_P110-2003.ucm.

#### Other enhancements

ICU 2.8 adds support for the specification of which unassigned Unicode code
points should be mapped to subchar1 rather than the default subchar. See the
discussion of subchar1 above for more details.

The extension table data structure also removes one minor limitation on ICU
conversion tables: Fallback mappings to a single byte 00 are now allowed and
handled properly. ICU versions before 2.8 could only handle roundtrips to/from
00.

### Examples for codepage state tables

The following shows the exact implied state tables for non-MBCS types, A state
table may need to be overwritten in order to allow supplementary characters
(U+10000 and up).

US-ASCII
```
0-7f
```

This single-row state table describes US-ASCII. Byte values from 0 to 0x7f are
valid and map to Unicode characters up to U+ffff. Byte values from 0x80 to 0xff
are illegal.

Shift-JIS
```
0-7f, 81-9f:1, a0-df, e0-fc:1
40-7e, 80-fc
```

This two-row state table describes the Shift-JIS structure which encodes some
characters with one byte each and others with two bytes each. Bytes 0 to 0x7f
and 0xa0 to 0xdf are valid single-byte encodings. Bytes 0x81 to 0x9f and 0xe0 to
0xfc are lead bytes. (For example, they are followed by one of the bytes that is
specified as valid in state 1). A byte sequence of 0x85 0x61 is valid while a
single byte of 0x80 or 0xff is illegal. Similarly, a byte sequence of 0x85 0x31
is illegal.

EUC-JP
```
0-8d, 8e:2, 8f:3, 90-9f, a1-fe:1
a1-fe
a1-e4
a1-fe:1, a1:4, a3-af:4, b6:4, d6:4, da-db:4, ed-f2:4
a1-fe.u
```

This fairly complicated state table describes EUC-JP. Valid byte sequences are
one, two, or three bytes long. Two-byte sequences have a lead byte of 0x8e and
end in state 2, or have lead bytes 0xa1 to 0xfe and end in state 1. Three-byte
sequences have a lead byte of 0x8f and continue in state 3. Some final byte
value ranges are entirely unassigned, therefore they end in state 4 with an
action letter of u for "unassigned" to save significant memory for the code
units table. Assigned three-byte sequences end in state 1 like most two-byte
sequences.

SBCS default state table:
```
0-ff
```
SBCS by default implies the structure for single-byte, 8-bit codepages.

DBCS default state table:
```
0-3f:3, 40:2, 41-fe:1, ff:3
41-fe
40

```

**Important**:
These are four states — the fourth has an empty line (equivalent to 0-ff.i)!
DBCS codepages, by default, are defined with the EBCDIC double-byte structure.
Valid sequences are pairs of bytes from 0x41 to 0xfe and the one pair 0x40/0x40
for the double-byte space. The structure is defined such that all illegal byte
sequences are always two in length. Therefore, every byte in the initial state
is a lead byte.

EBCDIC_STATEFUL default state table:
```
0-ff, e:1.s, f:0.s
initial, 0-3f:4, e:1.s, f:0.s, 40:3, 41-fe:2, ff:4
0-40:1.i, 41-fe:1., ff:1.i
0-ff:1.i, 40:1.
0-ff:1.i
```

This is the structure of Mixed Single-byte and Double-byte EBCDIC codepages,
which are stateful and use the Shift-In/Shift-Out (SI/SO) bytes 0x0f/0x0e. The
initial state 0 is almost the same as for SBCS except for SI and SO. State 1 is
also an initial state and is the basis for a state-shifted version of the DBCS
structure above. All double-byte sequences return to state 1 and SI switches
back to state 0. SI and SO are also allowed in their own states with no effect.

> :point_right:  **Note**: *If a DBCS or EBCDIC_STATEFUL codepage maps supplementary (non-BMP) Unicode
> characters, then a modified state table needs to be specified in the .ucm file.
> The state table needs to use the surrogates designation for a table row or .p
> for some entries.*
> 
> *The reuse of a final or intermediate state (shown for EUC-JP) is valid for as
> long as there is no circle in the state chain. The mappings will be unique
> because of the different path to the shared state (sharing a state saves some
> memory; each state table row occupies 1kB in the .cnv file). This table also
> shows the redefinition of byte value ranges within one state row (State number
> 3) as shorthand. State 3 defines bytes a1-fe to go to state 1, but the following
> entries redefine and override certain bytes to go to state 4.*

An initial state never needs a surrogates designation or .p because Unicode
mapping results in initial states that are stored directly in the state table,
providing enough room in each cell. The size of a generated .cnv mapping table
file depends primarily on the number and distribution of the mappings and on the
number of valid, multi-byte sequences that the state table allows. Each state
table row takes up one kilobyte.

For single-byte codepages, the state table cells contain all two-Unicode
mappings. Code point results for multi-byte sequences are stored in an array
with enough room for all valid byte sequences. For all byte sequences that end
in a surrogates or .p state, Unicode allocates two code units.

If possible, valid state table entries may be changed to .u to reduce the number
of valid, assignable sequences and to make the .cnv file smaller. If additional
states are necessary, then each additional state itself adds 1kB to the file
size, diminishing the file size savings. See the EUC-JP example above.

For codepages with up to two bytes per character, the makeconv tool
automatically compacts the bytes, if possible, by introducing one more trail
byte state. This state replaces valid entries in the original trail state with
unassigned entries and changes each lead byte entry to work with the new state
if there are no mappings with that lead byte.

For codepages with up to three or four bytes per character, compaction must be
done manually. However, if the verbose option is set on the command line, the
makeconv tool will print useful information about unassigned byte sequences.
