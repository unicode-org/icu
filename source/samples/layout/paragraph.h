/*
 *******************************************************************************
 *
 *   Copyright (C) 1999-2001, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 *
 *******************************************************************************
 *   file name:  Paragraph.h
 *
 *   created on: 09/06/2000
 *   created by: Eric R. Mader
 */
#ifndef __PARAGRAPH_H
#define __PARAGRAPH_H

#include "unicode/utypes.h"
#include "unicode/uscript.h"
#include "unicode/brkiter.h"
#include "unicode/ubidi.h"

#include "GUISupport.h"
#include "RenderingFontInstance.h"
#include "FontMap.h"

U_NAMESPACE_USE

#define MARGIN 10

struct RunParams
{
    const RenderingFontInstance *fontInstance;
    UChar *text;
    int32_t count;
    UScriptCode scriptCode;
    UBool rightToLeft;
};

struct RunInfo
{
    const RenderingFontInstance *fontInstance;
    int32_t charBase;
    int32_t glyphBase;
    UBool rightToLeft;
};

class Paragraph
{
public:
    Paragraph(void *surface,RunParams runs[], int32_t count, UBiDi *bidi);

    ~Paragraph();

    int32_t getAscent();
    int32_t getLineHeight();
    int32_t getLineCount();
    void breakLines(int32_t width, int32_t height);
    void draw(void *surface, int32_t firstLine, int32_t lastLine);

    static Paragraph *paragraphFactory(const char *fileName, FontMap *fontMap, GUISupport *guiSupport, void *surface);

protected:
    int32_t previousBreak(int32_t charIndex);
    int32_t getCharRun(int32_t ch, int32_t startingRun, int32_t direction);
    int32_t getGlyphRun(int32_t glyph, int32_t startingRun, int32_t direction);

    int32_t getRunWidth(int32_t startGlyph, int32_t endGlyph);
    int32_t drawRun(void *surface, const RenderingFontInstance *fontInstance, int32_t firstChar, int32_t lastChar,
                     int32_t x, int32_t y);


private:
    UBiDi *fBidi;

    int32_t fRunCount;
    RunInfo *fRunInfo;

    int32_t fCharCount;
    UChar *fText;
    BreakIterator *fBrkiter;

    int32_t fGlyphCount;
    LEGlyphID *fGlyphs;
    int32_t *fCharIndices;
    int32_t *fGlyphIndices;
    int32_t *fDX;
    int32_t *fDY;

    int32_t fBreakCount;
    int32_t *fBreakArray;

    int32_t fLineHeight;
    int32_t fAscent;
    int32_t fWidth;
    int32_t fHeight;
};

#endif


