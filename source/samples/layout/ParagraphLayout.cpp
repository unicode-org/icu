/*
 **********************************************************************
 *   Copyright (C) 2002, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 **********************************************************************
 */

#include "layout/LETypes.h"
#include "layout/LayoutEngine.h"
#include "layout/LEFontInstance.h"
#include "unicode/ubidi.h"
#include "unicode/uchriter.h"
#include "unicode/brkiter.h"
#include "Utilities.h"
#include "usc_impl.h" /* this is currently private! */

#include "ParagraphLayout.h"

struct VisualRunInfo
{
    le_int32 styleRun;
    le_int32 firstChar;
    le_int32 lastChar;
};

struct StyleRunInfo
{
      LayoutEngine   *engine;
const LEFontInstance *font;
      LEGlyphID      *glyphs;
      float          *positions;
      UScriptCode     script;
      UBiDiLevel      level;
      le_int32        runBase;
      le_int32        runLimit;
      le_int32        glyphBase;
      le_int32        glyphCount;
};

class StyleRuns
{
public:
    StyleRuns(const le_int32 *styleRunLimits[], const le_int32 styleRunCounts[], le_int32 styleCount);

    ~StyleRuns();

    le_int32 getRuns(le_int32 runLimits[], le_int32 styleIndices[]);

private:
    le_int32 fStyleCount;
    le_int32 fRunCount;

    le_int32 *fRunLimits;
    le_int32 *fStyleIndices;
};

StyleRuns::StyleRuns(const le_int32 *styleRunLimits[], const le_int32 styleRunCounts[], le_int32 styleCount)
    : fStyleCount(styleCount), fRunCount(0), fRunLimits(NULL), fStyleIndices(NULL)
{
    le_int32 maxRunCount = 0;
    le_int32 style, run, runStyle;
    le_int32 *currentRun = new le_int32[styleCount];

    for (int i = 0; i < styleCount; i += 1) {
        maxRunCount += styleRunCounts[i];
    }

    maxRunCount -= styleCount - 1;

    fRunLimits    = new le_int32[maxRunCount];
    fStyleIndices = new le_int32[maxRunCount * styleCount];

    for (style = 0; style < styleCount; style += 1) {
        currentRun[style] = 0;
    }

    run = 0;
    runStyle = 0;

    /*
     * Since the last run limit for each style run must be
     * the same, all the styles will hit the last limit at
     * the same time, so we know when we're done when the first
     * style hits the last limit.
     */
    while (currentRun[0] < styleRunCounts[0]) {
        fRunLimits[run] = 0x7FFFFFFF;

        // find the minimum run limit for all the styles
        for (style = 0; style < styleCount; style += 1) {
            if (styleRunLimits[style][currentRun[style]] < fRunLimits[run]) {
                fRunLimits[run] = styleRunLimits[style][currentRun[style]];
            }
        }

        // advance all styles whose current run is at this limit to the next run
        for (style = 0; style < styleCount; style += 1) {
            fStyleIndices[runStyle++] = currentRun[style];

            if (styleRunLimits[style][currentRun[style]] == fRunLimits[run]) {
                currentRun[style] += 1;
            }
        }

        run += 1;
    }

    fRunCount = run;
    delete[] currentRun;
}

StyleRuns::~StyleRuns()
{
    fRunCount = 0;

    delete[] fStyleIndices;
    fStyleIndices = NULL;

    delete[] fRunLimits;
    fRunLimits = NULL;
}

le_int32 StyleRuns::getRuns(le_int32 runLimits[], le_int32 styleIndices[])
{
    if (runLimits != NULL) {
        LE_ARRAY_COPY(runLimits, fRunLimits, fRunCount);
    }

    if (styleIndices != NULL) {
        LE_ARRAY_COPY(styleIndices, fStyleIndices, fRunCount * fStyleCount);
    }

    return fRunCount;
}

/*
 * NOTE: This table only has "true" values for
 * those scripts which the LayoutEngine can currently
 * process, rather for all scripts which require
 * complex processing for correct rendering.
 */
le_bool ParagraphLayout::fComplexTable[] = {
    false , /* Zyyy */
    false,  /* Qaai */
    true,   /* Arab */
    false,  /* Armn */
    true,   /* Beng */
    false,  /* Bopo */
    false,  /* Cher */
    false,  /* Qaac */
    false,  /* Cyrl */
    false,  /* Dsrt */
    true,   /* Deva */
    false,  /* Ethi */
    false,  /* Geor */
    false,  /* Goth */
    false,  /* Grek */
    true,   /* Gujr */
    true,   /* Guru */
    false,  /* Hani */
    false,  /* Hang */
    true,   /* Hebr */
    false,  /* Hira */
    true,   /* Knda */
    false,  /* Kana */
    false,  /* Khmr */
    false,  /* Laoo */
    false,  /* Latn */
    true,   /* Mlym */
    false,  /* Mong */
    false,  /* Mymr */
    false,  /* Ogam */
    false,  /* Ital */
    true,   /* Orya */
    false,  /* Runr */
    false,  /* Sinh */
    false,  /* Syrc */
    true,   /* Taml */
    true,   /* Telu */
    false,  /* Thaa */
    true,   /* Thai */
    false,  /* Tibt */
    false,  /* Cans */
    false,  /* Yiii */
    false,  /* Tglg */
    false,  /* Hano */
    false,  /* Buhd */
    false   /* Tagb */
};

ParagraphLayout::ParagraphLayout(const LEUnicode chars[], le_int32 count,
                                 const LEFontInstance **fonts, const le_int32 fontRunLimits[], le_int32 fontRunCount,
                                 const UBiDiLevel levels[], const le_int32 levelRunLimits[], le_int32 levelRunCount,
                                 const UScriptCode scripts[], const le_int32 scriptRunLimits[], le_int32 scriptRunCount,
                                 UBiDiLevel paragraphLevel, le_bool vertical)
                                 : fChars(chars), fCharCount(count),
                                   fFonts(fonts), fFontRunLimits(fontRunLimits), fFontRunCount(fontRunCount),
                                   fLevels(levels), fLevelRunLimits(levelRunLimits), fLevelRunCount(levelRunCount),
                                   fScripts(scripts), fScriptRunLimits(scriptRunLimits), fScriptRunCount(scriptRunCount),
                                   fVertical(vertical), fClientLevels(true), fClientScripts(true), fEmbeddingLevels(NULL),
                                   fGlyphToCharMap(NULL), fCharToGlyphMap(NULL), fGlyphWidths(NULL), fGlyphCount(0),
                                   fParaBidi(NULL), fLineBidi(NULL),
                                   fStyleRunLimits(NULL), fStyleIndices(NULL), fStyleRunCount(0),
                                   fBreakIterator(NULL), fLineStart(-1), fLineEnd(0),
                                   fVisualRuns(NULL), fStyleRunInfo(NULL), fVisualRunCount(-1),
                                   fFirstVisualRun(-1), fLastVisualRun(-1), fVisualRunLastX(0), fVisualRunLastY(0)
{
    // FIXME: should check the limit arrays for consistency...

    computeLevels(paragraphLevel);

    if (scripts == NULL) {
        computeScripts();
    }

    // now intersect the font, direction and script runs...
    const le_int32 *styleRunLimits[] = {fFontRunLimits, fLevelRunLimits, fScriptRunLimits};
    const le_int32  styleRunCounts[] = {fFontRunCount,  fLevelRunCount,  fScriptRunCount};
    le_int32  styleCount = sizeof styleRunLimits / sizeof styleRunLimits[0];
    StyleRuns styleRuns(styleRunLimits, styleRunCounts, styleCount);
    LEErrorCode layoutStatus = LE_NO_ERROR;
    
    fStyleRunCount = styleRuns.getRuns(NULL, NULL);

    fStyleRunLimits = new le_int32[fStyleRunCount];
    fStyleIndices   = new le_int32[fStyleRunCount * styleCount];
    
    styleRuns.getRuns(fStyleRunLimits, fStyleIndices);

    // now build a LayoutEngine for each style run...
    le_int32 *styleIndices = fStyleIndices;
    le_int32 run, runStart;

    fStyleRunInfo = new StyleRunInfo[fStyleRunCount];

    fGlyphCount = 0;
    for (runStart = 0, run = 0; run < fStyleRunCount; run += 1) {
        fStyleRunInfo[run].font = fFonts[styleIndices[0]];
        fStyleRunInfo[run].runBase   = runStart;
        fStyleRunInfo[run].runLimit  = fStyleRunLimits[run];
        fStyleRunInfo[run].script    = fScripts[styleIndices[2]];
        fStyleRunInfo[run].level     = fLevels[styleIndices[1]];
        fStyleRunInfo[run].glyphBase = fGlyphCount;

        fStyleRunInfo[run].engine = LayoutEngine::layoutEngineFactory(fStyleRunInfo[run].font,
            fStyleRunInfo[run].script, 0, layoutStatus);

        fStyleRunInfo[run].glyphCount = fStyleRunInfo[run].engine->layoutChars(fChars, runStart, fStyleRunLimits[run] - runStart, fCharCount,
            fStyleRunInfo[run].level & 1, 0, 0, layoutStatus);

        runStart = fStyleRunLimits[run];
        styleIndices += styleCount;
        fGlyphCount += fStyleRunInfo[run].glyphCount;
    }

    // Make big arrays for the glyph widths, glyph-to-char and char-to-glyph maps,
    // in logical order. (Both maps need an extra entry for the end of the text.) 
    //
    // For each layout get the positions and convert them into glyph widths, in
    // logical order. Get the glyph-to-char mapping, offset by starting index in the
    // width array, and swap it into logical order. Then fill in the char-to-glyph map
    // from this. (charToGlyph[glyphToChar[i]] = i)
    fGlyphWidths    = new float[fGlyphCount];
    fGlyphToCharMap = new le_int32[fGlyphCount];
    fCharToGlyphMap = new le_int32[fCharCount + 1];

    for (runStart = 0, run = 0; run < fStyleRunCount; run += 1) {
        LayoutEngine *engine = fStyleRunInfo[run].engine;
        le_int32 glyphCount  = fStyleRunInfo[run].glyphCount;
        le_int32 glyphBase   = fStyleRunInfo[run].glyphBase;
        le_int32 glyph;

        fStyleRunInfo[run].glyphs = new LEGlyphID[glyphCount];
        fStyleRunInfo[run].positions = new float[glyphCount * 2 + 2];

        engine->getGlyphs(fStyleRunInfo[run].glyphs, layoutStatus);
        engine->getGlyphPositions(fStyleRunInfo[run].positions, layoutStatus);
        engine->getCharIndices(&fGlyphToCharMap[glyphBase], runStart, layoutStatus);

        for (glyph = 0; glyph < glyphCount; glyph += 1) {
            fGlyphWidths[glyphBase + glyph] = fStyleRunInfo[run].positions[glyph * 2 + 2] - fStyleRunInfo[run].positions[glyph * 2];
            fCharToGlyphMap[fGlyphToCharMap[glyphBase + glyph]] = glyphBase + glyph;
        }

        if ((fStyleRunInfo[run].level & 1) != 0) {
            Utilities::reverse(&fGlyphWidths[glyphBase], glyphCount);
            Utilities::reverse(&fGlyphToCharMap[glyphBase], glyphCount);

            // Utilities::reverse(&fCharToGlyphMap[runStart], fStyleRunLimits[run] - runStart);
            // convert from visual to logical glyph indices
            for (glyph = glyphBase; glyph < glyphBase + glyphCount; glyph += 1) {
                le_int32 ch = fGlyphToCharMap[glyph];
                le_int32 lastGlyph = glyphBase + glyphCount - 1;

                // both lastGlyph and fCharToGlyphMap[ch] are biased by
                // glyphBase, so subtracting them will remove the bias.
                fCharToGlyphMap[ch] = lastGlyph - fCharToGlyphMap[ch] + glyphBase;
            }
        }

        runStart = fStyleRunLimits[run];

        delete engine;
        fStyleRunInfo[run].engine = NULL;
    }

    fCharToGlyphMap[fCharCount]  = fGlyphCount;
    fGlyphToCharMap[fGlyphCount] = fCharCount;
}

ParagraphLayout::~ParagraphLayout()
{
    if (! fClientLevels) {
        delete[] (UBiDiLevel *) fLevels;
        fLevels = NULL;

        delete[] (le_int32 *) fLevelRunLimits;
        fLevelRunLimits = NULL;

        fClientLevels = true;
    }

    if (! fClientScripts) {
        delete[] (UScriptCode *) fScripts;
        fScripts = NULL;

        delete[] (le_int32 *) fScriptRunLimits;
        fScriptRunLimits = NULL;

        fClientScripts = true;
    }

    if (fEmbeddingLevels != NULL) {
        delete[] fEmbeddingLevels;
        fEmbeddingLevels = NULL;
    }

    if (fGlyphToCharMap != NULL) {
        delete[] fGlyphToCharMap;
        fGlyphToCharMap = NULL;
    }

    if (fCharToGlyphMap != NULL) {
        delete[] fCharToGlyphMap;
        fCharToGlyphMap = NULL;
    }

    if (fGlyphWidths != NULL) {
        delete[] fGlyphWidths;
        fGlyphWidths = NULL;
    }

    if (fParaBidi != NULL) {
        ubidi_close(fParaBidi);
        fParaBidi = NULL;
    }

    if (fLineBidi != NULL) {
        ubidi_close(fLineBidi);
        fLineBidi = NULL;
    }

    if (fStyleRunCount > 0) {
        le_int32 run;

        delete[] fStyleRunLimits;
        delete[] fStyleIndices;

        for (run = 0; run < fStyleRunCount; run += 1) {
            delete[] fStyleRunInfo[run].glyphs;
            delete[] fStyleRunInfo[run].positions;

            fStyleRunInfo[run].glyphs    = NULL;
            fStyleRunInfo[run].positions = NULL;
        }

        delete[] fStyleRunInfo;

        fStyleRunLimits = NULL;
        fStyleIndices   = NULL;
        fStyleRunInfo        = NULL;
        fStyleRunCount  = 0;
    }

    if (fBreakIterator != NULL) {
        delete fBreakIterator;
        fBreakIterator = NULL;
    }

    if (fVisualRunCount > 0) {
        delete[] fVisualRuns;

        fVisualRuns     = NULL;
        fVisualRunCount = 0;
    }
}

le_bool ParagraphLayout::isComplex(const LEUnicode chars[], le_int32 count)
{
    UErrorCode scriptStatus = U_ZERO_ERROR;
    UScriptCode scriptCode  = USCRIPT_INVALID_CODE;
    UScriptRun *sr = uscript_openRun(chars, count, &scriptStatus);

    while (uscript_nextRun(sr, NULL, NULL, &scriptCode)) {
        if (isComplex(scriptCode)) {
            return true;
        }
    }

    return false;
}

le_int32 ParagraphLayout::nextLineBreak(float width)
{
    if (fLineEnd >= fCharCount) {
        return -1;
    }

    fLineStart = fLineEnd;

    le_int32 glyph    = fCharToGlyphMap[fLineStart];
    float widthSoFar  = 0;

    while (glyph < fGlyphCount && widthSoFar + fGlyphWidths[glyph] <= width) {
        widthSoFar += fGlyphWidths[glyph++];
    }

    // If no glyphs fit on the line, force one to fit.
    //
    // (There shouldn't be any zero width glyphs at the
    // start of a line unless the paragraph consists of
    // only zero width glyphs, because otherwise the zero
    // width glyphs will have been included on the end of
    // the previous line...)
    if (widthSoFar == 0 && glyph < fGlyphCount) {
        glyph += 1;
    }

    fLineEnd = previousBreak(fGlyphToCharMap[glyph]);

    // If there's no real break, break at the
    // glyph that didn't fit.
    if (fLineEnd <= fLineStart) {
        fLineEnd = fGlyphToCharMap[glyph];
    }

    computeVisualRuns();

    return fLineEnd;
}

le_int32 ParagraphLayout::countLineRuns()
{
    if (fVisualRunCount < 0) {
        fLineStart = 0;
        fLineEnd   = fCharCount;
        computeVisualRuns();
    }

    return fVisualRunCount;
}

le_int32 ParagraphLayout::getVisualRun(le_int32 runIndex, LEGlyphID glyphs[], float positions[], le_int32 glyphToCharMap[],
                                        const LEFontInstance **font, UBiDiDirection *runDirection)
{
    countLineRuns();

    if (runIndex < 0 || runIndex >= fVisualRunCount) {
        return -1;
    }

    // need to deal with partial first, last run...
    le_int32 run       = fVisualRuns[runIndex].styleRun;
    le_int32 firstChar = fVisualRuns[runIndex].firstChar;
    le_int32 lastChar  = fVisualRuns[runIndex].lastChar;
    le_int32 glyphBase = fStyleRunInfo[run].glyphBase;
    le_int32 inGlyph, outGlyph;

    // Get the glyph indices for all the characters between firstChar and lastChar,
    // make the minimum one be leftGlyph and the maximum one be rightGlyph.
    // (need to do this to handle local reorderings like Indic left matras)
    le_int32 leftGlyph  = fGlyphCount;
    le_int32 rightGlyph = -1;
    le_int32 ch;

    for (ch = firstChar; ch <= lastChar; ch += 1) {
        le_int32 glyph = fCharToGlyphMap[ch];

        if (glyph < leftGlyph) {
            leftGlyph = glyph;
        }

        if (glyph > rightGlyph) {
            rightGlyph = glyph;
        }
    }

    if ((fStyleRunInfo[run].level & 1) != 0) {
        le_int32 swap = rightGlyph;
        le_int32 last = glyphBase + fStyleRunInfo[run].glyphCount - 1;

        // Here, we want to remove the glyphBase bias...
        rightGlyph = last - leftGlyph;
        leftGlyph  = last - swap;
    } else {
        rightGlyph -= glyphBase;
        leftGlyph  -= glyphBase;
    }

    // Make sure that the first glyph on the line is positioned at X = 0,
    // even if we start in the middle of a layout
    if (run == fFirstVisualRun) {
        fVisualRunLastX = - fStyleRunInfo[run].positions[leftGlyph * 2];
    }
 
    // Make rightGlyph be the glyph just to the right of
    // the run's glyphs
    rightGlyph += 1;

    if (glyphs != NULL) {
        LE_ARRAY_COPY(glyphs, &fStyleRunInfo[run].glyphs[leftGlyph], rightGlyph - leftGlyph);
    }

    if(positions != NULL) {
        for (outGlyph = 0, inGlyph = leftGlyph * 2; inGlyph <= rightGlyph * 2; inGlyph += 2, outGlyph += 2) {
            positions[outGlyph]     = fStyleRunInfo[run].positions[inGlyph] + fVisualRunLastX;
            positions[outGlyph + 1] = fStyleRunInfo[run].positions[inGlyph + 1] /* + fVisualRunLastY */;
        }

        // Save the ending position of this run
        // to use for the start of the next run
        fVisualRunLastX = positions[outGlyph - 2];
     // fVisualRunLastY = positions[rightGlyph * 2 + 2];
    }

    if(glyphToCharMap != NULL) {
        if ((fStyleRunInfo[run].level & 1) == 0) {
            for (outGlyph = 0, inGlyph = leftGlyph; inGlyph < rightGlyph; inGlyph += 1, outGlyph += 1) {
                glyphToCharMap[outGlyph] = fGlyphToCharMap[glyphBase + inGlyph];
            }
        } else {
            for (outGlyph = 0, inGlyph = rightGlyph - 1; inGlyph >= leftGlyph; inGlyph -= 1, outGlyph += 1) {
                glyphToCharMap[outGlyph] = fGlyphToCharMap[glyphBase + inGlyph];
            }
        }
    }

    if (font != NULL) {
        *font = fStyleRunInfo[run].font;
    }

    if(runDirection != NULL) {
        if ((fStyleRunInfo[run].level & 1) == 0) {
            *runDirection = UBIDI_LTR;
        } else {
            *runDirection = UBIDI_RTL;
        }
    }

    return rightGlyph - leftGlyph;
}

void ParagraphLayout::computeLevels(UBiDiLevel paragraphLevel)
{
    UErrorCode bidiStatus = U_ZERO_ERROR;

    if (fLevels != NULL) {
        le_int32 ch;
        le_int32 run;

        fEmbeddingLevels = new UBiDiLevel[fCharCount];

        for (ch = 0, run = 0; run < fLevelRunCount; run += 1) {
            UBiDiLevel runLevel = fLevels[run] | UBIDI_LEVEL_OVERRIDE;
            le_int32 runLimit = fLevelRunLimits[run];

            while (ch < runLimit) {
                fEmbeddingLevels[ch++] = runLevel;
            }
        }
    }

    fParaBidi = ubidi_openSized(fCharCount, 0, &bidiStatus);
    ubidi_setPara(fParaBidi, fChars, fCharCount, paragraphLevel, fEmbeddingLevels, &bidiStatus);

    if (fLevels == NULL) {
        fLevelRunCount = ubidi_countRuns(fParaBidi, &bidiStatus);
        fLevels = new UBiDiLevel[fLevelRunCount];
        fLevelRunLimits = new le_int32[fLevelRunCount];

        fClientLevels = false;

        le_int32 logicalStart = 0;
        le_int32 run;

        for (run = 0; run < fLevelRunCount; run += 1) {
            ubidi_getLogicalRun(fParaBidi, logicalStart, (le_int32 *) &fLevelRunLimits[run], (UBiDiLevel *) &fLevels[run]);
            logicalStart = fLevelRunLimits[run];
        }
    }
}

// FIXME: this iterates through the runs twice, which is a bit expensive
void ParagraphLayout::computeScripts()
{
    UErrorCode scriptStatus = U_ZERO_ERROR;
    UScriptRun *sr = uscript_openRun(fChars, fCharCount, &scriptStatus);
    
    fScriptRunCount = 0;

    while (uscript_nextRun(sr, NULL, NULL, NULL)) {
        fScriptRunCount += 1;
    }

    uscript_resetRun(sr);

    fScripts = new UScriptCode[fScriptRunCount];
    fScriptRunLimits = new le_int32[fScriptRunCount];

    fClientScripts = false;

    le_int32 run = 0;

    while (uscript_nextRun(sr, NULL, (le_int32 *) &fScriptRunLimits[run], (UScriptCode *) &fScripts[run])) {
        run += 1;
    }

    uscript_closeRun(sr);
}

le_bool ParagraphLayout::isComplex(UScriptCode script)
{
    if (script < 0 || script >= USCRIPT_CODE_LIMIT) {
        return false;
    }

    return fComplexTable[script];
}

le_int32 ParagraphLayout::previousBreak(le_int32 charIndex)
{
    // skip over any whitespace or control characters,
    // because they can hang in the margin.
    while (charIndex < fCharCount &&
           (u_isWhitespace(fChars[charIndex]) ||
            u_iscntrl(fChars[charIndex]))) {
        charIndex += 1;
    }

    // Create the BreakIterator if we don't already have one
    if (fBreakIterator == NULL) {
        Locale thai("th");
        UCharCharacterIterator *iter = new UCharCharacterIterator(fChars, fCharCount);
        UErrorCode status = U_ZERO_ERROR;

        fBreakIterator = BreakIterator::createLineInstance(thai, status);
        fBreakIterator->adoptText(iter);
    }

    // return the break location that's at or before
    // the character we stopped on. Note: if we're
    // on a break, the "+ 1" will cause preceding to
    // back up to it.
    return fBreakIterator->preceding(charIndex + 1);
}

void ParagraphLayout::computeVisualRuns()
{
    UErrorCode bidiStatus = U_ZERO_ERROR;
    le_int32 dirRunCount, lineRun, visualRun;

    if (fVisualRuns != NULL) {
        delete[] fVisualRuns;
    }

    fVisualRunLastX = 0;
    fVisualRunLastY = 0;
    fFirstVisualRun = getCharRun(fLineStart);
    fLastVisualRun  = getCharRun(fLineEnd - 1);

    //fVisualRunCount = fLastVisualRun - fFirstVisualRun + 1;
    // + 2 because bidi might split the last run in two if
    // it contains trailing whitespace
    fVisualRuns = new VisualRunInfo[fLastVisualRun - fFirstVisualRun + 2];

    if (fLineBidi == NULL) {
        fLineBidi = ubidi_openSized(fCharCount, 0, &bidiStatus);
    }

    ubidi_setLine(fParaBidi, fLineStart, fLineEnd, fLineBidi, &bidiStatus);
    dirRunCount = ubidi_countRuns(fLineBidi, &bidiStatus);

    for (lineRun = 0, visualRun = 0; visualRun < dirRunCount; visualRun += 1) {
        le_int32 relStart, run, runLength;
        UBiDiDirection runDirection = ubidi_getVisualRun(fLineBidi, visualRun, &relStart, &runLength);
        le_int32 runStart = fLineStart + relStart;
        le_int32 runEnd   = runStart + runLength - 1;
        le_int32 firstRun = getCharRun(runStart);
        le_int32 lastRun  = getCharRun(runEnd);

        if (runDirection == UBIDI_LTR) {
            for (run = firstRun; run <= lastRun; run += 1) {
                fVisualRuns[lineRun].styleRun  = run;
                fVisualRuns[lineRun].firstChar = run == firstRun? runStart : fStyleRunInfo[run].runBase;
                fVisualRuns[lineRun].lastChar  = run == lastRun?  runEnd   : fStyleRunInfo[run].runLimit - 1;
                lineRun += 1;
            }
        } else {
            for (run = lastRun; run >= firstRun; run -= 1) {
                fVisualRuns[lineRun].styleRun  = run;
                fVisualRuns[lineRun].firstChar = run == firstRun? runStart : fStyleRunInfo[run].runBase;
                fVisualRuns[lineRun].lastChar  = run == lastRun?  runEnd   : fStyleRunInfo[run].runLimit - 1;
                lineRun += 1;
            }
        }
    }

    fVisualRunCount = lineRun;
}

#if 0
// this doesn't work because the nth entry in the limit array
// contains the first offset that's *not* in the nth run, but
// Utilities::search will return a value of n for that offset.
le_int32 ParagraphLayout::getCharRun(le_int32 charIndex)
{
    if (charIndex < 0 || charIndex > fCharCount) {
        return -1;
    }

    return Utilities::search(charIndex, fStyleRunLimits, fStyleRunCount);
}
#endif

le_int32 ParagraphLayout::getCharRun(le_int32 charIndex)
{
    if (charIndex < 0 || charIndex > fCharCount) {
        return -1;
    }

    le_int32 run;

    // NOTE: as long as fStyleRunLimits is well-formed
    // the above range check guarantees that we'll never
    // fall off the end of the array.
    run = 0;
    while (charIndex >= fStyleRunLimits[run]) {
        run += 1;
    }

    return run;
}
