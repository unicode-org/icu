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

#include "unicode/loengine.h"

#include "RenderingFontInstance.h"

#include "unicode/utypes.h"
#include "unicode/uchar.h"
#include "unicode/uchriter.h"
#include "unicode/brkiter.h"
#include "unicode/locid.h"
#include "unicode/ubidi.h"

#include "paragraph.h"
#include "scrptrun.h"
#include "UnicodeReader.h"
#include "FontMap.h"

#define MARGIN 10

Paragraph::Paragraph(void *surface, RunParams params[], int32_t count, UBiDi *bidi)
    : fBidi(bidi), fRunCount(count), fRunInfo(NULL), fCharCount(0), fText(NULL), fGlyphCount(0), fGlyphs(NULL),
      fCharIndices(NULL), fGlyphIndices(NULL), fDX(NULL), fBreakArray(NULL), fBreakCount(0),
      fLineHeight(-1), fAscent(-1)
{
    int32_t i;

    fWidth = fHeight = 0;

    fRunInfo = new RunInfo[count + 1];

    // Set charBase and rightToLeft for
    // each run and count the total characters
    for (i = 0; i < count; i += 1) {
        fRunInfo[i].charBase = fCharCount;
        fRunInfo[i].rightToLeft = params[i].rightToLeft;
        fCharCount += params[i].count;
    }

    // Set charBase and rightToLeft for the
    // fake run at the end.
    fRunInfo[count].charBase = fCharCount;
    fRunInfo[count].rightToLeft = false;

    fBreakArray = new int32_t[fCharCount + 1];
    fText = new LEUnicode[fCharCount];
    
    // Copy the text runs into a single array
    for (i = 0; i < count; i += 1) {
        int32_t charBase = fRunInfo[i].charBase;
        int32_t charCount = fRunInfo[i + 1].charBase - charBase;

        LE_ARRAY_COPY(&fText[charBase], params[i].text, charCount);
    }

    Locale thai("th");
    UCharCharacterIterator *iter = new UCharCharacterIterator(fText, fCharCount);
    UErrorCode status = U_ZERO_ERROR;
    Locale dummyLocale;

    fBrkiter = BreakIterator::createLineInstance(thai, status);
    fBrkiter->adoptText(iter);

    ICULayoutEngine **engines = new ICULayoutEngine *[count];
    int32_t maxAscent = -1, maxDescent = -1, maxLeading = -1;
    float x = 0, y = 0;

    // Layout each run, set glyphBase and glyphCount
    // and count the total number of glyphs
    for (i = 0; i < count; i += 1) {
        int32_t charBase = fRunInfo[i].charBase;
        int32_t charCount = fRunInfo[i + 1].charBase - charBase;
        int32_t glyphCount = 0;
        int32_t runAscent = 0, runDescent = 0, runLeading = 0;
        UErrorCode success = U_ZERO_ERROR;

        fRunInfo[i].fontInstance = params[i].fontInstance;

        fRunInfo[i].fontInstance->setFont(surface);

        runAscent  = fRunInfo[i].fontInstance->getAscent();
        runDescent = fRunInfo[i].fontInstance->getDescent();
        runLeading = fRunInfo[i].fontInstance->getLeading();


        if (runAscent > maxAscent) {
            maxAscent = runAscent;
        }

        if (runDescent > maxDescent) {
            maxDescent = runDescent;
        }

        if (runLeading > maxLeading) {
            maxLeading = runLeading;
        }

        engines[i] = ICULayoutEngine::createInstance(fRunInfo[i].fontInstance, params[i].scriptCode, dummyLocale, success);

        glyphCount = engines[i]->layoutChars(fText, charBase, charBase + charCount, fCharCount,
            fRunInfo[i].rightToLeft, x, y, success);

        engines[i]->getGlyphPosition(glyphCount, x, y, success);

        fRunInfo[i].glyphBase = fGlyphCount;
        fGlyphCount += glyphCount;
    }

    fLineHeight = maxAscent + maxDescent + maxLeading;
    fAscent = maxAscent;

    // Set glyphBase for the fake run at the end
    fRunInfo[count].glyphBase = fGlyphCount;

    fGlyphs = new LEGlyphID[fGlyphCount];
    fCharIndices = new int32_t[fGlyphCount];
    fGlyphIndices = new int32_t[fCharCount + 1];
    fDX = new int32_t[fGlyphCount];
    fDY = new int32_t[fGlyphCount];


    float *positions = new float[fGlyphCount * 2 + 2];

    // Build the glyph, charIndices and positions arrays
    for (i = 0; i < count; i += 1) {
        ICULayoutEngine *engine = engines[i];
        int32_t charBase = fRunInfo[i].charBase;
        int32_t glyphBase = fRunInfo[i].glyphBase;
        UErrorCode success = U_ZERO_ERROR;

        engine->getGlyphs(&fGlyphs[glyphBase], success);
        engine->getCharIndices(&fCharIndices[glyphBase], charBase, success);
        engine->getGlyphPositions(&positions[glyphBase * 2], success);
    }

    // Filter deleted glyphs, compute logical advances
    // and set the char to glyph map
    for (i = 0; i < fGlyphCount; i += 1) {
        // Filter deleted glyphs
        if (fGlyphs[i] == 0xFFFE || fGlyphs[i] == 0xFFFF) {
            fGlyphs[i] = 0x0001;
        }

        // compute the logical advance
        fDX[i] = (int32_t) (positions[i * 2 + 2] - positions[i * 2]);

        // save the Y offset
        fDY[i] = (int32_t) positions[i * 2 + 1];

        // set char to glyph map
        fGlyphIndices[fCharIndices[i]] = i;
    }

    if (fRunInfo[count - 1].rightToLeft) {
        fGlyphIndices[fCharCount] = fRunInfo[count - 1].glyphBase - 1;
    } else {
        fGlyphIndices[fCharCount] = fGlyphCount;
    }

    delete[] positions;

    // Get rid of the LayoutEngine's:
    for (i = 0; i < count; i += 1) {
        delete engines[i];
    }

    delete[] engines;
}

Paragraph::~Paragraph()
{
    delete[] fDY;
    delete[] fDX;
    delete[] fGlyphIndices;
    delete[] fCharIndices;
    delete[] fGlyphs;

    delete fBrkiter;
    delete fText;

    delete[] fBreakArray;
    delete[] fRunInfo;

    ubidi_close(fBidi);
}

int32_t Paragraph::getLineHeight()
{
    return fLineHeight;
}

int32_t Paragraph::getLineCount()
{
    return fBreakCount;
}

int32_t Paragraph::getAscent()
{
    return fAscent;
}

int32_t Paragraph::previousBreak(int32_t charIndex)
{
    LEUnicode ch = fText[charIndex];

    // skip over any whitespace or control
    // characters, because they can hang in
    // the margin.
    while (charIndex < fCharCount &&
           (u_isWhitespace(ch) ||
            u_iscntrl(ch))) {
        ch = fText[++charIndex];
    }

    // return the break location that's at or before
    // the character we stopped on. Note: if we're
    // on a break, the "+ 1" will cause preceding to
    // back up to it.
    return fBrkiter->preceding(charIndex + 1);
}

void Paragraph::breakLines(int32_t width, int32_t height)
{
    int32_t lineWidth = width - (2 * MARGIN);
    int32_t thisWidth = 0;
    int32_t thisBreak = -1;
    int32_t prevWidth = fWidth;

    fWidth  = width;
    fHeight = height;

    // don't re-break if the width hasn't changed
    if (width == prevWidth) {
        return;
    }

    fBreakArray[0] = 0;
    fBreakCount = 1;

    for (int32_t run = 0; run < fRunCount; run += 1) {
        int32_t glyph = fRunInfo[run].glyphBase;
        int32_t stop = fRunInfo[run + 1].glyphBase;
        int32_t dir = 1;

        if (fRunInfo[run].rightToLeft) {
            glyph = stop - 1;
            stop = fRunInfo[run].glyphBase - 1;
            dir = -1;
        }

        while (glyph != stop) {
            // Find the first glyph that doesn't fit on the line
            while (thisWidth + fDX[glyph] <= lineWidth) {
                thisWidth += fDX[glyph];
                glyph += dir;

                if (glyph == stop) {
                    break;
                }
            }

            // Check to see if we fell off the
            // end of the run
            if (glyph == stop) {
                break;
            }


            // Find a place before here to break,
            thisBreak = previousBreak(fCharIndices[glyph]);

            // If there wasn't one, force one
            if (thisBreak <= fBreakArray[fBreakCount - 1]) {
                thisBreak = fCharIndices[glyph];
            }

            // Save the break location.
            fBreakArray[fBreakCount++] = thisBreak;

            // Reset the accumulated width
            thisWidth = 0;

            // Map the character back to a glyph
            glyph = fGlyphIndices[thisBreak];


            // If the glyph's not in the run we stopped in, we
            // have to re-synch to the new run
            if (glyph < fRunInfo[run].glyphBase || glyph >= fRunInfo[run + 1].glyphBase) {
                run = getGlyphRun(glyph, 0, 1);

                if (fRunInfo[run].rightToLeft) {
                    stop = fRunInfo[run].glyphBase - 1;
                    dir = -1;
                } else {
                    stop = fRunInfo[run + 1].glyphBase;
                    dir = 1;
                }
            }
        }
    }

    // Make sure the last break is after the last character
    if (fBreakArray[--fBreakCount] != fCharCount) {
        fBreakArray[++fBreakCount] = fCharCount;
    }

    return;
}

int32_t Paragraph::getGlyphRun(int32_t glyph, int32_t startingRun, int32_t direction)
{
    int32_t limit;

    if (direction < 0) {
        limit = -1;
    } else {
        limit = fRunCount;
    }

    for (int32_t run = startingRun; run != limit; run += direction) {
        if (glyph >= fRunInfo[run].glyphBase && glyph < fRunInfo[run + 1].glyphBase) {
            return run;
        }
    }

    return limit;
}

int32_t Paragraph::getCharRun(int32_t ch, int32_t startingRun, int32_t direction)
{
    int32_t limit;

    if (direction < 0) {
        limit = -1;
    } else {
        limit = fRunCount;
    }

    for (int32_t run = startingRun; run != limit; run += direction) {
        if (ch >= fRunInfo[run].charBase && ch < fRunInfo[run + 1].charBase) {
            return run;
        }
    }

    return limit;
}

int32_t Paragraph::getRunWidth(int32_t startGlyph, int32_t endGlyph)
{
    int32_t width = 0;

    for (int32_t glyph = startGlyph; glyph <= endGlyph; glyph += 1) {
        width += fDX[glyph];
    }

    return width;
}

int32_t Paragraph::drawRun(void *surface, const RenderingFontInstance *fontInstance, int32_t firstChar, int32_t lastChar,
                         int32_t x, int32_t y)
{
    int32_t firstGlyph = fGlyphIndices[firstChar];
    int32_t lastGlyph  = fGlyphIndices[lastChar];

    for (int32_t ch = firstChar; ch <= lastChar; ch += 1) {
        int32_t glyph = fGlyphIndices[ch];

        if (glyph < firstGlyph) {
            firstGlyph = glyph;
        }

        if (glyph > lastGlyph) {
            lastGlyph = glyph;
        }
    }

    int32_t dyStart = firstGlyph, dyEnd = dyStart;

    fontInstance->setFont(surface);

    while (dyEnd <= lastGlyph) {
        while (dyEnd <= lastGlyph && fDY[dyStart] == fDY[dyEnd]) {
            dyEnd += 1;
        }

        fontInstance->drawGlyphs(surface, &fGlyphs[dyStart], dyEnd - dyStart,
            &fDX[dyStart], x, y + fDY[dyStart], fWidth, fHeight);

        for (int32_t i = dyStart; i < dyEnd; i += 1) {
            x += fDX[i];
        }

        dyStart = dyEnd;
    }

    return getRunWidth(firstGlyph, lastGlyph);
}

void Paragraph::draw(void *surface, int32_t firstLine, int32_t lastLine)
{
    int32_t line, x, y;
    int32_t prevRun = 0;
    UErrorCode bidiStatus = U_ZERO_ERROR;
    UBiDi  *lBidi = ubidi_openSized(fCharCount, 0, &bidiStatus);

    y = fAscent;

    for (line = firstLine; line <= lastLine; line += 1) {
        int32_t firstChar = fBreakArray[line];
        int32_t lastChar  = fBreakArray[line + 1] - 1;
        int32_t dirCount, dirRun;

        x = MARGIN;

        ubidi_setLine(fBidi, firstChar, lastChar + 1, lBidi, &bidiStatus);

        dirCount = ubidi_countRuns(lBidi, &bidiStatus);

        for (dirRun = 0; dirRun < dirCount; dirRun += 1) {
            int32_t relStart = 0, runLength = 0;
            UBiDiDirection runDirection = ubidi_getVisualRun(lBidi, dirRun, &relStart, &runLength);
            int32_t runStart  = relStart + firstChar;
            int32_t runEnd    = runStart + runLength - 1;
            int32_t firstRun  = getCharRun(runStart, prevRun, 1);
            int32_t lastRun   = getCharRun(runEnd,   firstRun, 1);

            for (int32_t run = firstRun; run <= lastRun; run += 1) {
                const RenderingFontInstance *fontInstance = fRunInfo[run].fontInstance;
                int32_t nextBase;

                if (run == lastRun) {
                    nextBase = runEnd + 1;
                } else {
                    nextBase = fRunInfo[run + 1].charBase;
                }

                x += drawRun(surface, fontInstance, runStart, nextBase - 1, x, y);
                runStart = nextBase;
            }

            prevRun = lastRun;
        }

        y += fLineHeight;
    }

    ubidi_close(lBidi);
}

Paragraph *Paragraph::paragraphFactory(const char *fileName, FontMap *fontMap, GUISupport *guiSupport, void *surface)
{
    RunParams params[64];
    int32_t paramCount = 0;
    int32_t charCount  = 0;
    int32_t dirCount   = 0;
    int32_t dirRun     = 0;
    RFIErrorCode fontStatus = RFI_NO_ERROR;
    UErrorCode bidiStatus = U_ZERO_ERROR;
    const UChar *text = UnicodeReader::readFile(fileName, guiSupport, charCount);
    ScriptRun scriptRun(text, charCount);

    if (text == NULL) {
        return NULL;
    }

    UBiDi *pBidi = ubidi_openSized(charCount, 0, &bidiStatus);

    ubidi_setPara(pBidi, text, charCount, UBIDI_DEFAULT_LTR, NULL, &bidiStatus);

    dirCount = ubidi_countRuns(pBidi, &bidiStatus);

    for (dirRun = 0; dirRun < dirCount; dirRun += 1) {
        int32_t runStart = 0, runLength = 0;
        UBiDiDirection runDirection = ubidi_getVisualRun(pBidi, dirRun, &runStart, &runLength);
        
        scriptRun.reset(runStart, runLength);

        while (scriptRun.next()) {
            int32_t     start = scriptRun.getScriptStart();
            int32_t     end   = scriptRun.getScriptEnd();
            UScriptCode code  = scriptRun.getScriptCode();

            params[paramCount].text = &((UChar *) text)[start];
            params[paramCount].count = end - start;
            params[paramCount].scriptCode = (UScriptCode) code;
            params[paramCount].rightToLeft = runDirection == UBIDI_RTL;

            params[paramCount].fontInstance = fontMap->getScriptFont(code, fontStatus);

            if (params[paramCount].fontInstance == NULL) {
                ubidi_close(pBidi);
                return 0;
            }

            paramCount += 1;
        }
    }

    return new Paragraph(surface, params, paramCount, pBidi);
}

