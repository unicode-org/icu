---
layout: default
title: Layout Engine
nav_order: 12
has_children: true
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Layout Engine
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Line Layout Deprecation

> :warning: ***The ICU Line LayoutEngine has been removed in ICU 58.***
> It had not had active development for some time, had many open bugs,
> and had been deprecated in ICU 54.
>
> Users of ICU Layout are **strongly** encouraged to consider the HarfBuzz project
> as a replacement for the ICU Layout Engine. An ICU team member responsible for
> the Layout Engine is contributing fixes and features to HarfBuzz, and a drop in
> wrapper is available to allow use of HarfBuzz as a direct replacement for the
> ICU layout engine.
>
> HarfBuzz has its own active mailing lists, please use those for discussion of
> HarfBuzz and its use as a replacement for the ICU layout engine.
> See: [http://www.freedesktop.org/wiki/Software/HarfBuzz](http://www.freedesktop.org/wiki/Software/HarfBuzz)


> :point_right: **Users of the "layoutex" ParagraphLayout library**: Please see information
about how to build "layoutex" on the [Paragraph Layout](paragraph.md) page.


## Overview

> :warning: **See the deletion/deprecation notice, above.**

The Latin script, which is the most commonly used script among software
developers, is also the least complex script to display especially when it is
used to write English. Using the Latin script, characters can be displayed from
left to right in the order that they are stored in memory. Some scripts require
rendering behavior that is more complicated than the Latin script. We refer to
these scripts as "complex scripts" and to text written in these scripts as
"complex text." Examples of complex scripts are the Indic scripts (for example,
Devanagari, Tamil, Telugu, and Gujarati), Thai, and Arabic.

These complex scripts exhibit complications that are not found in the Latin
script. The following lists the main complications in complex text:

The ICU LayoutEngine is designed to handle these complications through a simple,
uniform client interface. Clients supply Unicode code points in reading or
"logical" order, and the LayoutEngine provides a list of what to display,
indicates the correct order, and supplies the positioning information.

Because the ICU LayoutEngine is platform independent and text rendering is
inherently platform dependent, the LayoutEngine cannot directly display text.
Instead, it uses an abstract base class to access font files. This base class
models a TrueType font at a particular point size and device resolution. The
TrueType fonts have the following characteristics:

1.  A font is a collection of images, called glyphs. Each glyph in the font is
    referred to by a 16-bit glyph id.

2.  There is a mapping from Unicode code points to glyph ids. There may be
    glyphs in the font for which there is no mapping.

3.  The font contains data tables referred to by 4 byte tags. (e.g. `GSUB`,
    `cmap`). These tables can be read into memory for processing.

4.  There is a method to get the width of a glyph.

5.  There is a method to get the position of a control point from a glyph.

Since many of the contextual forms, ligatures, and split characters needed to
display complex text do not have Unicode code points, they can only be referred
to by their glyph indices. Because of this, the LayoutEngine's output is a list
of glyph indices. This means that the output must be displayed using an
interface where the characters are specified by glyph indices rather than code
points.

A concrete instance of this base class must be written for each target platform.
For a simple example which uses the standard C library to access a TrueType
font, look at the PortableFontInstance class in
[icu/source/test/letest](https://github.com/unicode-org/icu/tree/master/icu4c/source/test/letest)
.

The ICU LayoutEngine supports complex text in the following ways:

1.  If the font contains OpenType® tables, the LayoutEngine uses those tables.

2.  If the font contains Apple Advanced Typography (AAT) tables, the
    LayoutEngine uses those tables.

3.  For Arabic and Hebrew text, if OpenType tables are not present, the
    LayoutEngine uses Unicode presentation forms.

4.  For Thai text, the LayoutEngine uses either the Microsoft or Apple Thai
    forms.

OpenType processing requires script-specific processing to be done before the
tables are used. The ICU LayoutEngine performs this processing for Arabic,
Devanagari, Bengali, Gurmukhi, Gujarati, Oriya, Tamil, Telegu, Kannada, and
Malayalam text.

The AAT processing in the LayoutEngine is relatively basic as it only applies
the default features in left-to-right text. This processing has been tested for
Devanagari text. Since AAT processing is not script-specific, it might not work
for other scripts.

## Programming with the LayoutEngine

**See deprecation notice, above.**

The ICU LayoutEngine is designed to process a run of text which is in a single
font. It is written in a single direction (left-to-right or right-to-left), and
is written in a single script. Clients can use ICU's
[Bidi](../transforms/bidi.md) processing to determine the direction of the text
and use the ScriptRun class in
[icu/source/extra/scrptrun](https://github.com/unicode-org/icu/tree/master/icu4c/source/extra/scrptrun)
to find a run of text in the same script. Since the representation of font
information is application specific, ICU cannot help clients find these runs of
text.

Once the text has been broken into pieces that the LayoutEngine can handle, call
the LayoutEngineFactory method to create an instance of the LayoutEngine class
that is specific to the text. The following demonstrates a call to the
LayoutEngineFactory:

```c
LEFontInstace *font = <the text's font>;
UScriptCode script = <the text's script>;
LEErrorCode error = LE_NO_ERROR;
LayoutEngine *engine;
engine = LayoutEngine::layoutEngineFactory(font,
script,
0, // language - ignored
error);
The following example shows how to use the LayoutEngine to process the text:
LEUnicode text[] = <the text to process>;
le_int32 offset = <the starting offset of the text to process>;
le_int32 count = <the number of code points to process>;
le_int32 max = <the total number of characters in text>;
le_bool rtl = <true if the text is right-to-left, false otherwise>;
float x, y = <starting x, y position of the text>;
le_int32 glyphCount;
glyphCount = engine->layoutChars(text, offset, count, max, rtl,
x, y, error);
```

This previous example computes three arrays: an array of glyph indices in
display order, an array of x, y position pairs for each glyph, and an array that
maps each output glyph back to the input text array. Use the following get
methods to copy these arrays:

```c
LEGlyphID *glyphs = new LEGlyphID[glyphCount];
le_int32 *indices = new le_int32[glyphCount];
float *positions = new float[(glyphCount * 2) + 2];
engine->getGlyphs(glyphs, error);
engine->getCharIndices(indices, error);
engine->getGlyphPositions(positions, error);
```

> :point_right: **Note** The positions array contains (glyphCount * 2) + 2 entries. This is because
> there is an x and a y position for each glyph. The extra two positions hold the
> x, y position of the end of the text run.

Once users have the glyph indices and positions, they can use the
platform-specific code to draw the glyphs. For example, on Windows 2000, users
can call `ExtTextOut` with the `ETO_GLYPH_INDEX` option to draw the glyphs and on
Linux, users can call `TT_Load_Glyph` to get the bitmap for each glyph. However,
users must draw the bitmaps themselves.

> :point_right: **Note:** The ICU LayoutEngine was developed separately from the rest of ICU and uses
> different coding conventions and basic types. To use the LayoutEngine with ICU
> coding conventions, users can use the ICULayoutEngine class, which is a thin
> wrapper around the LayoutEngine class that incorporates ICU conventions and
> basic types.

For a more detailed example of how to call the LayoutEngine, look at
[icu/source/test/letest/letest.cpp](https://github.com/unicode-org/icu/tree/master/icu4c/source/test/letest/letest.cpp)
. This is a simple test used to verify that the LayoutEngine is working
properly. It does not do any complex text rendering.

For more information, see [ICU](http://icu-project.org/) , the [OpenType
Specification](http://www.microsoft.com/typography/tt/tt.htm) , and the
[TrueType Font File
Specification](http://developer.apple.com/fonts/TTRefMan/RM06/Chap6.html) .

> :warning: **Note:** See deprecation notice, above.
