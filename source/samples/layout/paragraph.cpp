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

#include "layout/ParagraphLayout.h"

#include "RenderingSurface.h"
#include "ScriptCompositeFontInstance.h"

#include "paragraph.h"
#include "UnicodeReader.h"
#include "FontMap.h"

#define MARGIN 10
#define LINE_GROW 32

Paragraph::Paragraph(const LEUnicode chars[], int32_t charCount, const FontRuns *fontRuns)
  : fParagraphLayout(NULL), fLineCount(0), fLinesMax(0), fLinesGrow(LINE_GROW), fLines(NULL),
    fLineHeight(-1), fAscent(-1), fWidth(-1), fHeight(-1)
{
    fParagraphLayout = new ParagraphLayout(chars, charCount, fontRuns, NULL, NULL, NULL, UBIDI_LTR, false);

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

