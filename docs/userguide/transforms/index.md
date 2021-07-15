---
layout: default
title: Transforms
nav_order: 8
has_children: true
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Transforms
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Overview

Transforms are used to process Unicode text in many different ways. Some include
case mapping, normalization, transliteration and bidirectional text handling.

### Case Mappings

[Case mapping](casemappings.md) is used to handle mappings of upper- and lower-case characters from
one language to another language, and writing systems that use letters of the
same alphabet to handle titlecase mappings that are particular to some class.
They provide for certain language-specific mappings as well.

### Normalization

[Normalization](normalization/index.md) is used to convert text to a unique, equivalent form. Systems can
normalize Unicode-encoded text to one particular sequence, such as a normalizing
composite character sequences into precomposed characters. While Normalization
Forms are specified for Unicode text, they can also be extended to non-Unicode
(legacy) character encodings. This is based on mapping the legacy character set
strings to and from Unicode.

### Transforms

[Transforms](general/index.md) provide a general-purpose package for processing Unicode text. They
are a powerful and flexible mechanism for handling a variety of different tasks,
including:

*   Uppercase, Lowercase, Titlecase, Full/Halfwidth conversions

*   Normalization

*   Hex and Character Name conversions

*   Script to Script conversion

### Bidirectional Algorithm

The [Bidirectional Algorithm](bidi.md) was developed to specify the direction of text in a
text flow.
