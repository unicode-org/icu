---
layout: default
title: Compression
nav_order: 4
parent: Conversion
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Compression
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Overview of SCSU

Compressing Unicode text for transmission or storage results in minimal
bandwidth usage and fewer storage devices. The compression scheme compresses
Unicode text into a sequence of bytes by using characteristics of Unicode text.
The compressed sequence can be used on its own or as further input to a general
purpose file or disk-block based compression scheme. Note that the combination
of the Unicode compression algorithm plus disk-block based compression produces
better results than either method alone.

Strings in languages using small alphabets contain runs of characters that are
coded close together in Unicode. These runs are typically interrupted only by
punctuation characters, which are themselves coded in proximity to each other in
Unicode (usually in the Basic Latin range).

For additional detail about the compression algorithm, which has been approved
by the Unicode Consortium, please refer to [Unicode Technical Report #6 (A
Standard Compression Scheme for
Unicode)](https://www.unicode.org/reports/tr6/).

The Standard Compression Scheme for Unicode (SCSU) is used to:

*   express all code points in Unicode

*   approximate the storage size of traditional character sets

*   facilitate the use of short strings

*   provide transparency for characters between `U+0020`-`U+00FF`, as well as `CR`, `LF`
    and `TAB`

*   support very simple decoders

*   support simple as well as sophisticated encoders

It does not attempt to avoid the use of control bytes (including `NUL`) in the
compressed stream.

The compression scheme is mainly intended for use with short to medium length
Unicode strings. The resulting compressed format is intended for storage or
transmission in bandwidth limited environments. It can be used stand-alone or as
input to traditional general purpose data compression schemes. It is not
intended as processing format or as general purpose interchange format.

## BOCU-1

A MIME compatible encoding called BOCU-1 is also available in ICU. Details about
this encoding can be found in the [Unicode Technical Note
#6](https://www.unicode.org/notes/tn6/). Both SCSU and BOCU-1 are IANA
registered names.

## Usage

The compression service in ICU is a part of Conversion framework, and follows
the semantics of converters. For more information on how to use ICU's conversion
service, please refer to the Usage Model section in the [Using
Converters](converters.md) chapter.

```c++
uint16_t germanUTF16[]={
    0x00d6, 0x006c, 0x0020, 0x0066, 0x006c, 0x0069, 0x0065, 0x00df, 0x0074
};

uint8_t germanSCSU[]={
    0xd6, 0x6c, 0x20, 0x66, 0x6c, 0x69, 0x65, 0xdf, 0x74
};
char target[100];
UChar uTarget[100];
UErrorCode status = U_ZERO_ERROR;
UConverter *conv;
int32_t len;

/* set up the SCSU converter */
conv = ucnv_open("SCSU", &status);
assert(U_SUCCESS(status));

/* compress the string using SCSU */
len = ucnv_fromUChars(conv, target, 100, germanUTF16, -1, &status);
assert(U_SUCCESS(status));

len = ucnv_toUChars(conv, uTarget, 100, germanSCSU, -1, &status);

/* close the converter */
ucnv_close(conv);
```
