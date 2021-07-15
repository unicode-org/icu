---
layout: default
title: Conversion
nav_order: 4
has_children: true
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Conversion
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Conversion Overview

A converter is used to convert from one character encoding to another. In the
case of ICU, the conversion is always between Unicode and another encoding, or
vice-versa. A text encoding is a particular mapping from a given character set
definition to the actual bits used to represent the data.

Unicode provides a single character set that covers the major languages of the
world, and a small number of machine-friendly encoding forms and schemes to fit
the needs of existing applications and protocols. It is designed for best
interoperability with both ASCII and ISO-8859-1 (the most widely used character
sets) to make it easier for Unicode to be used in almost all applications and
protocols.

Hundreds of encodings have been developed over the years, each for small groups
of languages and for special purposes. As a result, the interpretation of text,
input, sorting, display, and storage depends on the knowledge of all the
different types of character sets and their encodings. Programs have been
written to handle either one single encoding at a time and switch between them,
or to convert between external and internal encodings.

There is no single, authoritative source of precise definitions of many of the
encodings and their names. However,
[IANA](http://www.iana.org/assignments/character-sets) is the best source for
names, and our Character Set repository is a good source of encoding definitions
for each platform.

The transferring of text from one machine to another one often causes some loss
of information. Some platforms have a different interpretation of the text than
the other platforms. For example, Shift-JIS can be interpreted differently on
Windows™ compared to UNIX®. Windows maps byte value 0x5C to the backslash
symbol, while some UNIX machines map that byte value to the Yen symbol. Another
problem arises when a character in the codepage looks like the Unicode Greek
letter Mu or the Unicode micro symbol. Some platforms map this codepage byte
sequence to one Unicode character, while another platform maps it to the other
Unicode character. Fallbacks can partially fix this problem by mapping both
Unicode characters to the same codepage byte sequence. Even though some
character information is lost, the text is still readable.

ICU's converter API has the following main features:

1.  Unicode surrogate support

2.  Support for all major encodings

3.  Consistent text conversion across all computer platforms

4.  Text data can be streamed (buffered) through the API

5.  Fast text conversion

6.  Supports fallbacks to the codepage

7.  Supports reverse fallbacks to Unicode

8.  Allows callbacks for handling and substituting invalid or unmapped byte
    sequences

9.  Allows a user to add support for unsupported encodings

This section deals with the processes of converting encodings to and from
Unicode.

## Recommendations

1.  **Use Unicode encodings whenever possible.** Together with Unicode for
    internal processing, it makes completely globalized systems possible and
    avoids the many problems with non-algorithmic conversions. (For a discussion
    of such problems, see for example ["Character Conversions and Mapping
    Tables"](http://icu-project.org/docs/papers/conversions_and_mappings_iuc19.ppt)
    on <http://icu-project.org/docs/> and the [XML Japanese
    Profile](http://www.w3.org/TR/japanese-xml/)).

    1.  Use UTF-8 and UTF-16.

    2.  Use UTF-16BE, SCSU and BOCU-1 as appropriate.

    3.  In special environments, other Unicode encodings may be used as well,
        such as UTF-16LE, UTF-32, UTF-32BE, UTF-32LE, UTF-7, UTF-EBCDIC, and
        CESU-8. (For turning Unicode filenames into ASCII-only filename strings,
        the IMAP-mailbox-name encoding can be used.)

    4.  Do not exchange text with single/unpaired surrogates.

2.  **Use legacy charsets only when absolutely necessary**. For best data
    fidelity:

    1.  ISO-8859-1 is relatively unproblematic — if its limited character
        repertoire is sufficient — because it is converted trivially (1:1) to
        Unicode, avoiding conversion table problems for its small set of
        characters. (By contrast, proper conversion from US-ASCII requires a
        check for illegal byte values 0x80..0xff, which is an unnecessary
        complication for modern systems with 8-bit bytes. ISO-8859-1 is nearly
        as ubiquitous for modern systems as US-ASCII was for 7-bit systems.)

    2.  If you need to communicate with a certain platform, then use the same
        conversion tables as that platform itself, or at least ones that are
        very, very close.

    3.  ICU's conversion table repository contains hundreds of Unicode
        conversion tables from a number of common vendors and platforms as well
        as comparisons between these conversion tables:
        <http://icu-project.org/charts/charset/> .

    4.  Do not trust codepage documentation that is not machine-readable, for
        example nice-looking charts: They are usually incomplete and out of
        date.

    5.  ICU's default build includes about 200 conversion tables. See the [ICU
        Data](../icudata.md) chapter for how to add or remove conversion tables
        and other data.

    6.  In ICU, you can (and should) also use APIs that map a charset name
        together with a standard/platform name. This allows you to get different
        converters for the same ambiguous charset name (like "Shift-JIS"),
        depending on the standard or platform specified. See the
        [convrtrs.txt](https://github.com/unicode-org/icu/blob/master/icu4c/source/data/mappings/convrtrs.txt)
        alias table, the [Using Converters](converters.md) chapter and [API
        references](https://unicode-org.github.io/icu-docs/apidoc/released/icu4c/ucnv_8h.html) .

    7.  For data exchange (rather than pure display), turn off fallback
        mappings: `ucnv_setFallback(cnv, FALSE)`;

    8.  For some text formats, especially XML and HTML, it is possible to set an
        "escape callback" function that turns unmappable Unicode code points
        into corresponding escape sequences, preventing data loss. See the API
        references and the [ucnv sample
        code](https://github.com/unicode-org/icu/tree/master/icu4c/source/samples/ucnv/)
        .

    9.  **Never modify a conversion table.** Instead, use existing ones that
        match precisely those in systems with which you communicate. "Modifying"
        a conversion table in reality just creates a new one, which makes the
        whole situation even less manageable.
