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

#include "unicode/utypes.h"
#include "unicode/uchar.h"
#include "unicode/ubidi.h"
#include "usc_impl.h" /* this is currently private! */

#include "RenderingSurface.h"
#include "ScriptCompositeFontInstance.h"

#include "paragraph.h"
#include "UnicodeReader.h"
#include "FontMap.h"

#include "ParagraphLayout.h"

#define MARGIN 10
#define LINE_GROW 32

#if 0
class LineRun
{
public:
                   LineRun(ParagraphLayout *paragraphLayout, le_int32 runIndex);
                  ~LineRun();

          le_int32 draw(RenderingSurface *surface, le_int32 x, le_int32 y, le_int32 width, le_int32 height) const;

private:
          le_int32        fGlyphCount;
    const LEFontInstance *fFont;
          UBiDiDirection  fDirection;
    const LEGlyphID      *fGlyphs;
          le_int32       *fDX;
          le_int32       *fDY;
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

    fGlyphs   = LE_NEW_ARRAY(LEGlyphID, fGlyphCount);
    fDX       = LE_NEW_ARRAY(le_int32, fGlyphCount);
    fDY       = LE_NEW_ARRAY(le_int32, fGlyphCount);
    positions = LE_NEW_ARRAY(float, fGlyphCount * 2 + 2);

    glyphs = (LEGlyphID *) fGlyphs;

    paragraphLayout->getVisualRun(runIndex, glyphs, positions, NULL, &fFont, &fDirection);

    for (i = 0; i < fGlyphCount; i += 1) {
        TTGlyphID ttGlyph = (TTGlyphID) LE_GET_GLYPH(glyphs[i]);
        // filter out deleted glyphs
        if (ttGlyph == 0xFFFE || ttGlyph == 0xFFFF) {
            glyphs[i] = LE_SET_GLYPH(glyphs[i], 0x0002);
        }

        fDX[i] = (le_int32) (positions[i * 2 + 2] - positions[i * 2]);
        fDY[i] = (le_int32)  positions[i * 2 + 1];
    }

    LE_DELETE_ARRAY(positions);
}


LineRun::~LineRun()
{
    LE_DELETE_ARRAY(fDY);
    LE_DELETE_ARRAY(fDX);
    LE_DELETE_ARRAY(fGlyphs);
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
    fRuns     = LE_NEW_ARRAY(LineRun *, fRunCount);
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

    LE_DELETE_ARRAY(fRuns);
}

void LineInfo::draw(RenderingSurface *surface, le_int32 x, le_int32 y, le_int32 width, le_int32 height)
{
    le_int32 run;

    for (run = 0; run < fRunCount; run += 1) {
        x = fRuns[run]->draw(surface, x, y, width, height);
    }
}
#endif

Paragraph::Paragraph(const LEUnicode chars[], int32_t charCount, const FontRuns *fontRuns)
  : fParagraphLayout(NULL), fLineCount(0), fLinesMax(0), fLinesGrow(LINE_GROW), fLines(NULL),
    fLineHeight(-1), fAscent(-1), fWidth(-1), fHeight(-1)
{
    fParagraphLayout = new ParagraphLayout(chars, charCount, fontRuns, NULL, NULL, UBIDI_LTR, false);

    le_int32 ascent  = fParagraphLayout->getAscent();
    le_int32 descent = fParagraphLayout->getDescent();
    le_int32 leading = fParagraphLayout->getLeading();

    fLineHeight = ascent + descent + leading;
    fAscent     = ascent;

    delete fontRuns;
}

Paragraph::~Paragraph()
{
    for (le_int32 line = 0; line < fLineCount; line += 1) {
        delete /*(LineInfo *)*/ fLines[line];
    }

    LE_DELETE_ARRAY(fLines);
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
    const ParagraphLayout::Line *line;
    le_int32 li;

    // Free the old LineInfo's...
    for (li = 0; li < fLineCount; li += 1) {
        delete fLines[li];
    }

    li = 0;
    fParagraphLayout->reflow();
    while ((line = fParagraphLayout->nextLine(lineWidth)) != NULL) {
        // grow the line array, if we need to.
        if (li >= fLinesMax) {
            fLines = (const ParagraphLayout::Line **) LE_GROW_ARRAY(fLines, fLinesMax + fLinesGrow);
            fLinesMax += fLinesGrow;
        }

        fLines[li++] = line;
    }

    fLineCount = li;
}

void Paragraph::draw(RenderingSurface *surface, le_int32 firstLine, le_int32 lastLine)
{
    le_int32 li, x, y;

    x = MARGIN;
    y = fAscent;

    for (li = firstLine; li <= lastLine; li += 1) {
        const ParagraphLayout::Line *line = fLines[li];
        le_int32 runCount = line->countRuns();
        le_int32 run;

        for (run = 0; run < runCount; run += 1) {
            const ParagraphLayout::VisualRun *visualRun = line->getVisualRun(run);
            le_int32 glyphCount = visualRun->getGlyphCount();
            const LEFontInstance *font = visualRun->getFont();
            const LEGlyphID *glyphs = visualRun->getGlyphs();
            const float *positions = visualRun->getPositions();

            surface->drawGlyphs(font, glyphs, glyphCount, positions, x, y, fWidth, fHeight);
        }

        y += fLineHeight;
    }
}

Paragraph *Paragraph::paragraphFactory(const char *fileName, FontMap *fontMap, GUISupport *guiSupport)
{
    LEErrorCode fontStatus  = LE_NO_ERROR;
    UErrorCode scriptStatus = U_ZERO_ERROR;
    le_int32 charCount;
    const UChar *text = UnicodeReader::readFile(fileName, guiSupport, charCount);

    if (text == NULL) {
        return NULL;
    }

    ScriptCompositeFontInstance *font = new ScriptCompositeFontInstance(fontMap);
    FontRuns  *fontRuns = new FontRuns((const LEFontInstance **) &font, &charCount, 1);

    return new Paragraph(text, charCount, fontRuns);
}

