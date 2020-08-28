---
layout: default
title: Unicode Basics
nav_order: 3
parent: ICU
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Unicode Basics
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Introduction to Unicode

Unicode is a standard that precisely defines a character set as well as a small
number of encodings for it. It enables you to handle text in any language
efficiently. It allows a single application executable to work for a global
audience. ICU, like Java™, Microsoft® Windows NT™, Windows™ 2000 and other
modern systems, provides Internationalization solutions based on Unicode.

This chapter is intended as an introduction to codepages in general and Unicode
in particular. For further information, see:

1.  [The Web site of the Unicode consortium](http://www.unicode.org/)

2.  [What is
    Unicode?](https://www.unicode.org/standard/WhatIsUnicode.html)

3.  [IBM® Globalization](http://www.ibm.com/software/globalization/)

Go to the [online ICU demos](http://demo.icu-project.org/icu-bin/icudemos) to
see how a Unicode-based server application can handle text in many languages and
many encodings.

## Traditional Character Sets and Unicode

Representing text-format data in computers is a matter of defining a set of
characters and assigning each of them a number and a bit representation.
Underlying this basic idea are three related concepts:

1.  A character set or repertoire is an unordered collection of characters that
    can be represented by numeric values.

2.  A coded character set maps characters from a character set or repertoire to
    numeric values.

3.  A character encoding scheme defines the representation of numeric values
    from one or more coded character sets in bits and bytes.

For simple encodings such as ASCII, the last two concepts are basically the
same: ASCII assigns 128 characters and control codes to consecutive numbers from
0 to 127. These characters and control codes are encoded as simple, unsigned,
binary integers. Therefore, ASCII is both a coded character set and a character
encoding scheme.

ASCII only encodes 128 characters, 33 of which are control codes rather than
graphic, displayable characters. It was designed to represent English-language
text for an American user base, and is therefore insufficient for representing
text in almost any language other than American English. In fact, most
traditional encodings were limited to one or few languages and scripts.

ASCII offered a natural way to extend it: Designed in the 1960's to work in
systems with 7-bit bytes while most computers and Internet protocols since the
1970's use 8-bit bytes, the extra bit allowed another 128 byte values to
represent more characters. Various encodings were developed that supported
different languages. Some of these were based on ASCII, others were not.

Languages such as Japanese need to encode considerably more than 256 characters.
Various encoding schemes enable large character sets with thousands or tens of
thousands of characters to be represented. Most of those encodings are still
byte-based, which means that many characters require two or more bytes of
storage space. A process must be developed to interpret some byte values.

Various character sets and encoding schemes have been developed independently,
cover only one or few languages each, and are incompatible. This makes it very
difficult for a single system to handle text in more than one language at a
time, and especially difficult to do so in a way that is interoperable across
different systems.

Generally, the minimum requirement for the interoperable exchange of text data
is that the encoding (character set & encoding scheme) must be properly
specified in the document and in the protocol. For example, email/SMTP and
HTML/HTTP provide the means to specify the "charset", as it is called in
Internet standards. However, very often the encoding is not specified, specified
incorrectly, or the sender and receiver disagree on its implementation.

The ISO 2022 encoding scheme was created to store text in many different
languages. It allows other encodings to be embedded by first announcing them and
then switching between them. Full support for all features and possible
encodings with ISO 2022 requires complicated processing and the need to support
many encodings. For East Asian languages, subsets were developed that cover only
one language or a few at a time, but they are much more manageable. ISO 2022 is
not well-suited for use in internal processing. It is designed for data
exchange.

## Glyphs versus Characters

Programmers often need to distinguish between characters and glyphs. A character
is the smallest semantic unit in a writing system. It is an abstract concept
such as the letter A or the exclamation point. A glyph is the visual
presentation of one or more characters, and is often dependent on adjacent
characters.

There is not always a one-to-one mapping between characters and glyphs. In many
languages (Arabic is a prime example), the way a character looks depends heavily
on the surrounding characters. Standard printed Arabic has as many as four
different printed representations (glyphs) for every letter of the alphabet. In
many languages, two or more letters may combine together into a single glyph
(called a ligature), or a single character might be displayed with more than one
glyph.

Despite the different visual variants of a particular letter, it still retains
its identity. For example, the Arabic letter heh has four different visual
representations in common use. Whichever one is used, it still keeps its
identity as the letter heh. It is this identity that Unicode encodes, not the
visual representation. This also cuts down on the number of independent
character values required.

## Overview of Unicode

Unicode was developed as a single-coded character set that contains support for
all languages in the world. The first version of Unicode used 16-bit numbers,
which allowed for encoding 65,536 characters without complicated multibyte
schemes. With the inclusion of more characters, and following implementation
needs of many different platforms, Unicode was extended to allow more than one
million characters. Several other encoding schemes were added. This introduced
more complexity into the Unicode standard, but far less than managing a large
number of different encodings.

Starting with Unicode 2.0 (published in 1996), the Unicode standard began
assigning numbers from 0 to 10ffff<sub>16</sub>,which requires 21 bits but does not use
them completely. This gives more than enough room for all written languages in
the world. The original repertoire covered all major languages commonly used in
computing. Unicode continues to grow, and it includes more scripts.

The design of Unicode differs in several ways from traditional character sets
and encoding schemes:

1.  Its repertoire enables users to include text efficiently in almost all
    languages within a single document.

2.  It can be encoded in a byte-based way with one or more bytes per character,
    but the default encoding scheme uses 16-bit units that allow much simpler
    processing for all common characters.

3.  Many characters, such as letters with accents and umlauts, can be combined
    from the base character and accent or umlaut modifiers. This combining
    reduces the number of different characters that need to be encoded
    separately. "Precomposed" variants for characters that existed in common
    character sets at the time were included for compatibility.

4.  Characters and their usage are well-defined and described. While traditional
    character sets typically only provide the name or a picture of a character
    and its number and byte encoding, Unicode has a comprehensive database of
    properties available for download. It also defines a number of processes and
    algorithms for dealing with many aspects of text processing to make it more
    interoperable.

The early inclusion of all characters of commonly used character sets makes
Unicode a useful "pivot" point for converting between traditional character
sets, and makes it feasible to process non-Unicode text by first converting into
Unicode, process the text, and convert it back to the original encoding without
loss of data.

> :point_right: *The first 128 Unicode code point values are assigned to the same characters as
in US-ASCII. For example, the same number is assigned to the same character. The
same is true for the first 256 code point values of Unicode compared to ISO
8859-1 (Latin-1) which itself is a direct superset of US-ASCII. This makes it
easy to adapt many applications to Unicode because the numbers for many
syntactically important characters are the same.*

## Character Encoding Forms and Schemes for Unicode

Unicode assigns characters a number from 0 to 10FFFF<sub>16</sub>, giving enough elbow room
to allow for unambiguous encoding of every character in common use. Such a
character number is called a "code point".

> :point_right: *Unicode code points are just non-negative integer numbers in a certain range.
They do not have an implicit binary representation or a width of 21 or 32 bits.
Binary representation and unit widths are defined for encoding forms.*

For internal processing, the standard defines three encoding forms, and for file
storage and protocols, some of these encoding forms have encoding schemes that
differ in their byte ordering. The difference between an encoding form and an
encoding scheme is that an encoding form maps the character set codes to values
that fit into internal data types (like a short in C), while an encoding scheme
maps to bits and bytes. For traditional encodings, they are the same since the
encoding forms already map to bytes. 

The different Unicode encoding forms are optimized for a variety of different
uses:

1.  UTF-16, the default encoding form, maps a character code point to either one
    or two 16-bit integers.

2.  UTF-8 is a byte-based encoding that offers backwards compatibility with
    ASCII-based, byte-oriented APIs and protocols. A character is stored with 1,
    2, 3, or 4 bytes.

3.  UTF-32 is the simplest, but most memory-intensive encoding form: It uses one
    32-bit integer per Unicode character.

4.  SCSU is an encoding scheme that provides a simple compression of Unicode
    text. It is designed only for input and output, not for internal use.

ICU uses UTF-16 internally. ICU 2.0 fully supports supplementary characters
(with code points 10000<sub>16</sub>..10FFFF<sub>16</sub>). Older versions of ICU provided only partial
support for supplementary characters.

For input/output, character encoding schemes define a byte serialization of
text. UTF-8 is itself both an encoding form, and an encoding scheme because it is
byte-based. For each of UTF-16 and UTF-32, there are two variants defined: one
that serializes the code units in big-endian byte order (most significant byte
first), and one that serializes the code units in little-endian byte order
(least significant byte first). The corresponding encoding schemes are called
UTF-16BE, UTF-16LE, UTF-32BE, and UTF-32LE.

> :point_right: *The names "UTF-16" and "UTF-32" are ambiguous. Depending on context, they refer
either to character encoding forms where 16/32-bit words are processed and are
naturally stored in the platform endianness, or they refer to the
IANA-registered charset names, i.e., to character encoding schemes or byte
serializations. In addition to simple byte serialization, the charsets with
these names also use optional Byte Order Marks (see [Serialized Formats](#serialized-formats) below).*

## Overview of UTF-16

The default encoding form of the Unicode Standard uses 16-bit code units. Code
point values for the most common characters are in the range of 0 to FFFF<sub>16</sub> and
are encoded with just one 16-bit unit of the same value. Code points from
10000<sub>16</sub> to 10FFFF<sub>16</sub> are encoded with two code units that are often called
"surrogates", and they are called a "surrogate pair" when, together, they
correctly encode one Unicode character. The first surrogate in a pair must be in
the range D800<sub>16</sub> to DBFF<sub>16</sub>, and the second one must be in the range DC00<sub>16</sub> to
DFFF<sub>16</sub>. Every Unicode code point has only one possible UTF-16 encoding with
either one code unit that is not a surrogate or with a correct pair of
surrogates. The code point values D800<sub>16</sub> to DFFF<sub>16</sub> are set aside just for this
mechanism and will never, by themselves, be assigned any characters.

Most commonly used characters have code points below FFFF<sub>16</sub>, but Unicode 3.1
assigns more than 40,000 supplementary characters that make use of surrogate
pairs in UTF-16.

Note that comparing UTF-16 strings lexically based on their 16-bit code units
does not result in the same order as comparing the code points. This is not
usually an issue since only rarely-used characters are affected. Most processes
do not rely on the same results in such comparisons. Where necessary, a simple
modification to a string comparison can be performed that still allows efficient
code unit-based comparisons and makes them compatible with code point
comparisons. ICU has C and C++ API functions for this.

## Overview of UTF-8

To meet the requirements of byte-oriented, ASCII-based systems, the Unicode
Standard defines UTF-8. UTF-8 is a variable-length, byte-based encoding that
preserves ASCII transparency.

UTF-8 maintains transparency for all the ASCII code values (0..127). These
values do not appear in any byte of a transformed result except as the direct
representation of the ASCII values. Thus, ASCII text is also UTF-8 text.

Characteristics of UTF-8 include:

1.  Unicode code points 0 to 7F<sub>16</sub> are each encoded with a single byte of the
    same value. Therefore, ASCII characters take up 50% less space with UTF-8
    encoding than with UTF-16.

2.  All other code points are encoded with multibyte sequences, with the first
    byte (lead byte) indicating the number of bytes that follow (trail bytes).
    This results in very efficient parsing. The lead bytes are in the range c0<sub>16</sub>
    to fd<sub>16</sub>, the trail bytes are in the range 80<sub>16</sub> to bf<sub>16</sub>. The byte values fe<sub>16</sub>
    and FF<sub>16</sub> are never used.

3.  UTF-8 is relatively compact and resource conservative in its use of the
    bytes required for encoding text in European scripts, but uses 50% more
    space than UTF-16 for East Asian text. Code points up to 7FF<sub>16</sub> take up two
    bytes, code points up to FFFF<sub>16</sub> take up three (50% more memory than UTF-16),
    and all others four.

4.  Binary comparisons of UTF-8 strings based on their bytes result in the same
    order as comparing code point values.

## Overview of UTF-32

The UTF-32 encoding form always uses one single 32-bit integer per Unicode code
point. This results in a very simple encoding.

The drawback is its memory consumption: Since code point values use only 21
bits, one-third of the memory is always unused, and since most commonly used
characters have code point values of up to FFFF<sub>16</sub>, they take up only one 16-bit
unit in UTF-16 (50% less) and up to three bytes in UTF-8 (25% less).

UTF-32 is mainly used in APIs that are defined with the same data type for both
code points and code units. Modern versions of the C standard library that
support Unicode use a 32-bit `wchar_t` with UTF-32 semantics.

## Overview of SCSU

SCSU (Standard Compression Scheme for Unicode) is designed to reduce the size of
Unicode text for both input and output. It is a simple compression that
transforms the text into a byte stream. It typically uses one byte per character
in small scripts, and two bytes per character in large, East Asian scripts.

It is usually shorter than any of the UTFs. However, SCSU is stateful, which
makes it unsuitable for internal processing. It also uses all possible byte
values, which might require additional processing for protocols such as SMTP
(email).

See also <https://www.unicode.org/reports/tr6/> .

## Other Unicode Encodings

Other Unicode encodings have been developed over time for various purposes. Most
of them are implemented in ICU, see
[source/data/mappings/convrtrs.txt](https://github.com/unicode-org/icu/blob/master/icu4c/source/data/mappings/convrtrs.txt)

1.  BOCU-1: Binary-Ordered Compression of Unicode
    An encoding of Unicode that is about as compact as SCSU but has a much
    smaller amount of state. Unlike SCSU, it preserves code point order and can
    be used in 8bit emails without a transfer encoding. BOCU-1 does **not**
    preserve ASCII characters in ASCII-readable form. See [Unicode Technical
    Note #6](http://www.unicode.org/notes/tn6/) .

2.  UTF-7: Designed for 7bit emails; simple and not very compact. Since email
    systems have been 8-bit safe for several years, UTF-7 is not necessary any
    more and not recommended. Most ASCII characters are readable, others are
    base64-encoded. See [RFC 2152](http://www.ietf.org/rfc/rfc2152.txt) .

3.  IMAP-mailbox-name: A variant of UTF-7 that is suitable for expressing
    Unicode strings as ASCII characters for Unix filenames.
    **The name "IMAP-mailbox-name" is specific to ICU!**
    See [RFC 2060 INTERNET MESSAGE ACCESS PROTOCOL - VERSION
    4rev1](http://www.ietf.org/rfc/rfc2060.txt) section 5.1.3. Mailbox
    International Naming Convention.

4.  UTF-EBCDIC: An EBCDIC-friendly encoding that is similar to UTF-8. See
    [Unicode Technical Report #16](http://www.unicode.org/reports/tr16/) . **As
    of ICU 2.6, UTF-EBCDIC is not implemented in ICU.**

5.  CESU-8: Compatibility Encoding Scheme for UTF-16: 8-Bit
    An incompatible variant of UTF-8 that preserves 16-bit-Unicode (UTF-16)
    string order instead of code point order. Not for open interchange. See
    [Unicode Technical Report #26](http://www.unicode.org/reports/tr26/) .

## Programming using UTFs

Programming using any of the UTFs is much more straightforward than with
traditional multi-byte character encodings, even though UTF-8 and UTF-16 are
also variable-width encodings.

Within each Unicode encoding form, the code unit values for singletons (code
units that alone encode characters), lead units, and for trailing units are all
disjointed. This has crucial implications for implementations. The following
lists these implications:

1.  Determines the number of units for one code point using the lead unit. This
    is especially important for UTF-8, where there can be up to 4 bytes per
    character.

2.  Determines boundaries. If ICU users randomly access text, you can always
    determine the nearest code-point boundaries with a small number of machine
    instructions.

3.  Does not have any overlap. If ICU users search for string A in string B, you
    never get a false match on code points. Users do not need to convert to code
    points for string searching. False matches never occurs since the end of one
    sequence is never the same as the start of another sequence. Overlap is one
    of the biggest problems with common multi-byte encodings like Shift-JIS. All
    the UTFs avoid this problem.

4.  Uses simple iteration. Getting the next or previous code point is
    straightforward, and only takes a small number of machine instructions.

5.  Can use UTF-16 encoding, which is actually fully symmetric. ICU users can
    determine from any single code unit whether it is the first, last, or only
    one for a code point. Moving (iterating) in either direction through UTF-16
    text is equally fast and efficient.

6.  Uses slow indexing by code points. This indexing procedure is a disadvantage
    of all variable-width encodings. Except in UTF-32, it is inefficient to find
    code unit boundaries corresponding to the nth code point or to find the code
    point offset containing the nth code unit. Both involve scanning from the
    start of the text or from a last known boundary. ICU, like most common APIs,
    always indexes by code units. It counts code units and not code points.

Conversion between different UTFs is very fast. Unlike converting to and from
legacy encodings like Latin-2, conversion between UTFs does not require table
look-ups.

ICU provides two basic data type definitions for Unicode. `UChar32` is a 32-bit
type for code points, and used for single Unicode characters. It may be signed
or unsigned. It is the same as `wchar_t` if it is 32 bits wide. `UChar` is an
unsigned 16-bit integer for UTF-16 code units. It is the base type for strings
(`UChar *`), and it is the same as `wchar_t` if it is 16 bits wide.

Some higher-level APIs, used especially for formatting, use characters closer to
a representation for a glyph. Such "user characters" are also called "graphemes"
or "grapheme clusters" and require strings so that combining sequences can be
included.

## Serialized Formats

In files, input, output, and network protocols, text must be accompanied by the
specification of its character encoding scheme for a client to be able to
interpret it correctly. (This is called a "charset" in Internet protocols.)
However, an encoding scheme specification is not necessary if the text is only
used within a single platform, protocol, or application where it is otherwise
clear what the encoding is. (The language and text directionality should usually
be specified to enable spell checking, text-to-speech transformation, etc.)

*The discussion of encoding specifications in this section applies to standard
Internet protocols where charset name strings are used. Other protocols may use
numeric encoding identifiers and assign different semantics to those identifiers
than Internet protocols.*

Typically, the encoding specification is done in a protocol- and document
format-dependent way. However, the Unicode standard offers a mechanism for
tagging text files with a "signature" for cases where protocols do not identify
character encoding schemes.

The character ZERO WIDTH NO-BREAK SPACE (FEFF<sub>16</sub>) can be used as a signature by
prepending it to a file or stream. The alternative function of U+FEFF as a
format control character has been copied to U+2060 WORD JOINER, and U+FEFF
should only be used for Unicode signatures.

The different character encoding schemes generate different, distinct byte
sequences for U+FEFF:

1.  UTF-8: EF BB BF

2.  UTF-16BE: FE FF

3.  UTF-16LE: FF FE

4.  UTF-32BE: 00 00 FE FF

5.  UTF-32LE: FF FE 00 00

6.  SCSU: 0E FE FF

7.  BOCU-1: FB EE 28

8.  UTF-7: 2B 2F 76 ( 38 | 39 | 2B | 2F )

9.  UTF-EBCDIC: DD 73 66 73

ICU provides the function `ucnv_detectUnicodeSignature()` for Unicode signature
detection.

*There is no signature for CESU-8 separate from the one for UTF-8. UTF-8 and
CESU-8 encode U+FEFF and in fact all BMP code points with the same bytes. The
opportunity for misidentification of one as the other is one of the reasons why
CESU-8 should only be used in limited, closed, specific environments.*

In UTF-16 and UTF-32, where the signature also distinguishes between big-endian
and little-endian byte orders, it is also called a byte order mark (BOM). The
signature works for UTF-16 since the code point that has the byte-swapped
encoding, FFFE<sub>16</sub>, will never be a valid Unicode character. (It is a
"non-character" code point.) In Internet protocols, if an encoding specification
of "UTF-16" or "UTF-32" is used, it is expected that there is a signature byte
sequence (BOM) that identifies the byte ordering, which is not the case for the
encoding scheme/charset names with "BE" or "LE".

*If text is specified to be encoded in the UTF-16 or UTF-32 charset and does not
begin with a BOM, then it must be interpreted as UTF-16BE or UTF-32BE,
respectively.*

A signature is not part of the content, and must be stripped when processing.
For example, blindly concatenating two files will give an incorrect result.

If a signature was detected, then the signature "character" U+FEFF should be
removed from the Unicode stream **after** conversion. Removing the signature
bytes before conversion could cause the conversion to fail for stateful
encodings like BOCU-1 and UTF-7.

Whether a signature is to be recognized or not depends on the protocol or
application.

1.  If a protocol specifies a charset name, then the byte stream must be
    interpreted according to how that name is defined. Only the "UTF-16" and
    "UTF-32" names include recognition of the byte order marks that are specific
    to them (and the ICU converters for these names do this automatically). None
    of the other Unicode charsets are defined to include any signature/BOM
    handling.

2.  If no charset name is provided, for example for text files in most
    filesystems, then applications must usually rely on heuristics to determine
    the file encoding. Many document formats contain an embedded or implicit
    encoding declaration, but for plain text files it is reasonable to use
    Unicode signatures as simple and reliable heuristics. This is especially
    common on Windows systems. However, some tools for plain text file handling
    (e.g., many Unix command line tools) are not prepared for Unicode
    signatures.

## The Unicode Standard Is An Industry Standard

The Unicode standard is an industry standard and parallels ISO 10646-1. Around
1993, these two standards were effectively merged into the same character set
standard. Both standards have the same character repertoire and the same
encoding forms and schemes.

One difference used to be that the ISO standard defined code point values to be
from 0 to 7FFFFFFF<sub>16</sub>, not just up to 10FFFF<sub>16</sub>. The ISO work group decided to add
an amendment to the standard. The amendment removes this difference by declaring
that no characters will ever be assigned code points above 10FFFF<sub>16</sub>. The main
reason for the ISO work group's decision is interoperability between the UTFs.
UTF-16 can not encode any code points above this limit.

This means that the code point space for both Unicode and ISO 10646 is now the
same! **These changes to ISO 10646 have been made recently and should be
complete in the edition ISO 10646:2003 which also combines all parts of the
standard into one.**

The former, larger code space is the reason why the ISO definition of UTF-8
specifies sequences of five and six bytes to cover that whole range.

Another difference is that the ISO standard defines encoding forms "UCS-4" and
"UCS-2". UCS-4 is essentially UTF-32 with a theoretical upper limit of
7FFFFFFF<sub>16</sub>, using 31 out of the 32 bits. However, in practice, the ISO committee
has accepted that the characters above 10FFFF will not be encoded, so there is
essentially no difference between the forms. The "4" stands for "four-byte
form".

UCS-2 is a subset of UTF-16 that is limited to code points from 0 to FFFF,
excluding the surrogate code points. Thus, it cannot represent the characters
with code points above FFFF (called supplementary characters).

*There is no conversion necessary between UCS-2 and UTF-16. The difference is
only in the interpretation of surrogates.*

The standards differ in what kind of information they provide: The Unicode
standard provides more character properties and describes algorithms etc., while
the ISO standard defines collections, subsets and similar.

The standards are synchronized, and the respective committees work together to
add new characters and assign code point values.
