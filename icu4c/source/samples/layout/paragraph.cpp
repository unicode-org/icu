/*
 *******************************************************************************
 *
 *   Copyright (C) 1999-2001, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 *
 *******************************************************************************
 *   file name:  Paragraph.cpp
 *
 *   created on: 09/06/2000
 *   created by: Eric R. Mader
 */

#include "RenderingFontInstance.h"

#include "unicode/utypes.h"
#include "unicode/uchar.h"
#include "unicode/ubidi.h"
#include "usc_impl.h" /* this is currently private! */

#include "paragraph.h"
#include "UnicodeReader.h"
#include "FontMap.h"

#include "ParagraphLayout.h"

#define MARGIN 10
#define LINE_GROW 32

class LineRun
{
public:
                   LineRun(ParagraphLayout *paragraphLayout, le_int32 runIndex);
                  ~LineRun();

          le_int32 draw(RenderingSurface *surface, le_int32 x, le_int32 y, le_int32 width, le_int32 height) const;

private:
          le_int32               fGlyphCount;
    const RenderingFontInstance *fFont;
          UBiDiDirection         fDirection;
    const LEGlyphID             *fGlyphs;
          le_int32              *fDX;
          le_int32              *fDY;
};

LineRun::LineRun(ParagraphLayout *paragraphLayout, le_int32 runIndex)
{
    LEGlyphID *glyphs;
    float     *positions;
    le_int32   i;

    fGlyphCount = paragraphLayout->getVisualRun(runIndex, NULL, NULL, NULL, NULL, NULL);

    if (fGlyphCount <= 0) {
        return;
    }

    fGlyphs   = new LEGlyphID[fGlyphCount];
    fDX       = new le_int32[fGlyphCount];
    fDY       = new le_int32[fGlyphCount];
    positions = new float[fGlyphCount * 2 + 2];

    glyphs = (LEGlyphID *) fGlyphs;

    paragraphLayout->getVisualRun(runIndex, glyphs, (float *) positions, NULL, (const LEFontInstance **) &fFont, &fDirection);

    for (i = 0; i < fGlyphCount; i += 1) {
        TTGlyphID ttGlyph = (TTGlyphID) LE_GET_GLYPH(glyphs[i]);
        // filter out deleted glyphs
        if (ttGlyph == 0xFFFE || ttGlyph == 0xFFFF) {
            glyphs[i] = LE_SET_GLYPH(glyphs[i], 0x0002);
        }

        fDX[i] = (le_int32) (positions[i * 2 + 2] - positions[i * 2]);
        fDY[i] = (le_int32)  positions[i * 2 + 1];
    }

    delete[] positions;
}


LineRun::~LineRun()
{
    delete[] fDY;
    delete[] fDX;
    delete[] (LEGlyphID *) fGlyphs;
}

le_int32 LineRun::draw(RenderingSurface *surface, le_int32 x, le_int32 y, le_int32 width, le_int32 height) const
{
    le_int32 dyEnd, dyStart;

    dyStart  = dyEnd = 0;

    while (dyEnd < fGlyphCount) {
        while (dyEnd < fGlyphCount && fDY[dyStart] == fDY[dyEnd]) {
            dyEnd += 1;
        }

        surface->drawGlyphs(fFont, &fGlyphs[dyStart], dyEnd - dyStart,
            &fDX[dyStart], x, y + fDY[dyStart], width, height);

        for (le_int32 i = dyStart; i < dyEnd; i += 1) {
            x += fDX[i];
        }

        dyStart = dyEnd;
    }

    return x;
}

class LineInfo
{
public:
    LineInfo(ParagraphLayout *paragraphLayout);
    ~LineInfo();

    le_int32  getRunCount() const {return fRunCount;};
    void      draw(RenderingSurface *surface, le_int32 x, le_int32 y, le_int32 width, le_int32 height);

private:
    le_int32  fRunCount;
    LineRun **fRuns;
};

LineInfo::LineInfo(ParagraphLayout *paragraphLayout)
{
    fRunCount = paragraphLayout->countLineRuns();
    fRuns     = new LineRun *[fRunCount];
    le_int32 run;

    for (run = 0; run < fRunCount; run += 1) {
        fRuns[run] = new LineRun(paragraphLayout, run);
    }
}

LineInfo::~LineInfo()
{
    le_int32 run;

    for (run = 0; run < fRunCount; run += 1) {
        delete fRuns[run];
    }

    delete[] fRuns;
}

void LineInfo::draw(RenderingSurface *surface, le_int32 x, le_int32 y, le_int32 width, le_int32 height)
{
    le_int32 run;

    for (run = 0; run < fRunCount; run += 1) {
        x = fRuns[run]->draw(surface, x, y, width, height);
    }
}

Paragraph::Paragraph(const LEUnicode chars[], int32_t charCount,
          const RenderingFontInstance *runFonts[], const UScriptCode runScripts[],
          const le_int32 runLimits[], le_int32 runCount)
  : fParagraphLayout(NULL), fLineCount(0), fLinesMax(0), fLinesGrow(LINE_GROW), fLines(NULL),
    fLineHeight(-1), fAscent(-1), fWidth(-1), fHeight(-1)
{
    le_int32 run, maxAscent = -1, maxDescent = -1, maxLeading = -1;

    for (run = 0; run < runCount; run += 1) {
        le_int32 runAscent  = runFonts[run]->getAscent();
        le_int32 runDescent = runFonts[run]->getDescent();
        le_int32 runLeading = runFonts[run]->getLeading();


        if (runAscent > maxAscent) {
            maxAscent = runAscent;
        }

        if (runDescent > maxDescent) {
            maxDescent = runDescent;
        }

        if (runLeading > maxLeading) {
            maxLeading = runLeading;
        }
    }

    fLineHeight = maxAscent + maxDescent + maxLeading;
    fAscent     = maxAscent;

    // NOTE: We're passing the same array in for both font and script run limits...
    fParagraphLayout = new ParagraphLayout(chars, charCount, (const LEFontInstance **) runFonts, runLimits, runCount, NULL, NULL, 0,
        runScripts, runLimits, runCount, UBIDI_LTR, false);
}

Paragraph::~Paragraph()
{
    for (le_int32 line = 0; line < fLineCount; line += 1) {
        delete (LineInfo *) fLines[line];
    }

    delete[] fLines;
}

void Paragraph::breakLines(le_int32 width, le_int32 height)
{
    fHeight = height;

    // don't re-break if the width hasn't changed
    if (fWidth == width) {
        return;
    }

    fWidth  = width;

    float lineWidth = (float) (width - 2 * MARGIN);
    le_int32 line;

    // Free the old LineInfo's...
    for (line = 0; line < fLineCount; line += 1) {
        delete fLines[line];
    }

    line = 0;
    fParagraphLayout->reflow();
    while (fParagraphLayout->nextLineBreak(lineWidth) >= 0) {
        LineInfo *lineInfo = new LineInfo(fParagraphLayout);
        
        // grow the line array, if we need to.
        if (line >= fLinesMax) {
            LineInfo **newLines = new LineInfo *[fLinesMax + fLinesGrow];

            if (fLines != NULL) {
                LE_ARRAY_COPY(newLines, fLines, fLinesMax);

                delete[] fLines;
            }

            fLinesMax += fLinesGrow;
            fLines     = newLines;
        }

        fLines[line++] = lineInfo;
    }

    fLineCount = line;
}

void Paragraph::draw(RenderingSurface *surface, le_int32 firstLine, le_int32 lastLine)
{
    le_int32 line, x, y;

    x = MARGIN;
    y = fAscent;

    for (line = firstLine; line <= lastLine; line += 1) {
        fLines[line]->draw(surface, x, y, fWidth, fHeight);

        y += fLineHeight;
    }
}

Paragraph *Paragraph::paragraphFactory(const char *fileName, FontMap *fontMap, GUISupport *guiSupport)
{
    RFIErrorCode fontStatus = RFI_NO_ERROR;
    UErrorCode scriptStatus = U_ZERO_ERROR;
    le_int32 charCount, runCount;
    UScriptRun *sr;
    const UChar *text = UnicodeReader::readFile(fileName, guiSupport, charCount);

    if (text == NULL) {
        return NULL;
    }

    sr = uscript_openRun(text, charCount, &scriptStatus);
    
    runCount = 0;

    while (uscript_nextRun(sr, NULL, NULL, NULL)) {
        runCount += 1;
    }

    uscript_resetRun(sr);

    UScriptCode            *scripts   = new UScriptCode[runCount];
    RenderingFontInstance **fonts     = new RenderingFontInstance *[runCount];
    le_int32               *runLimits = new le_int32[runCount];

    le_int32 limit, run;
    UScriptCode script;

    run = 0;
    while (uscript_nextRun(sr, NULL, &limit, &script)) {
        scripts[run]   = script;
        runLimits[run] = limit;
        fonts[run]     = (RenderingFontInstance *) fontMap->getScriptFont(script, fontStatus);

        if (fonts[run] == NULL) {
            delete[] runLimits;
            delete[] fonts;
            delete[] scripts;

            uscript_closeRun(sr);
            return NULL;
        }

        run += 1;
    }

    uscript_closeRun(sr);

    return new Paragraph(text, charCount, (const RenderingFontInstance **) fonts, scripts, runLimits, runCount);
}

