/*
 **********************************************************************
 *   Copyright (C) 2002, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 **********************************************************************
 */

#ifndef __PARAGRAPHLAYOUT_H

#define __PARAGRAPHLAYOUT_H

#include "layout/LETypes.h"
#include "layout/LayoutEngine.h"
#include "layout/LEFontInstance.h"
#include "unicode/ubidi.h"
#include "unicode/brkiter.h"

struct VisualRunInfo;
struct StyleRunInfo;

/**
 * ParagraphLayout.
 *
 * The <code>ParagraphLayout</code> object will analyze the text into runs of text in the
 * same font, script and direction, and will create a <code>LayoutEngine</code> object for each run.
 * The <code>LayoutEngine</code> will transform the characers into glyph codes in visual order.
 *
 * Clients can use this to break a paragraph into lines, and to display the glyphs in each line.
 *
 */

/*
 * NOTES:
 * * The documentation needs a *lot* of work...
 *
 * * Need a usage example, esp. to show that you need to call findLineBreak() before
 *   countLineRuns() and / or getVisualRun(). (AND, what should happen if you don't??)
 *   (It treats the whole paragraph as a single line.)
 *
 *   If you don't call countLineRuns() before getVisualRun(), we'll (effectively) call
 *   it internally.
 *
 * * Might want to change to a model where a paragraph object iterates over lines objects
 *   and a line object iterates over runs.
 * 
 * * May need to handle composite fonts via LEFontInstance. We'd get a 32-bit glyph ID
 *   back from mapCharToGlyph() where the high 16 bits identify the physical font. We'd
 *   need to use this to compute physical font runs. Might want to make some of the high
 *   bits be client-defined flags, which the LayoutEngine will always ignore. Say 8 bits
 *   of flags, and 8 bits of font ID. (The flags could be used by clients for whatever they
 *   want, we wouldn't look at them at all...)
 *
 * * Might want language (or maybe locale?) runs in the constructor so that language tags
 *   can be passed to the LayoutEngines.
 */
class ParagraphLayout
{
public:
    /**
     * Construct a <code>ParagraphLayout</code> object for a styled paragraph. The paragraph is specified
     * as runs of text all in the same font. An <code>LEFontInstance</code> object and a limit offset
     * are specified for each font run. The limit offset is the offset of the character immediately
     * after the font run.
     *
     * Clients can optionally specify directional runs and / or script runs. If these aren't specified
     * they will be computed.
     *
     * @param chars is an array of the characters in the paragraph
     *
     * @param count is the number of characters in the paragraph.
     *
     * @param fonts is an array of the <code>LEFontInstance</code> objects associated
     *        with each font run.
     *
     * @param fontRunLimits is an array of the run limits of each font run.
     *
     * @param fontRunCount is the number of font runs.
     *
     * @param levels is an array of directional levels. If this pointer in <code>NULL</code>
     *        the levels will be determined by running the Unicde Bidi algorithm.
     *
     * @param levelRunLimits is an array of run limits for each level run. If <code>levels</code>
     *        is <code>NULL</code> this pointer must be <code>NULL</code> too.
     *
     * @param levelRunCount is the number of directional level runs. If <code>levels</code> is
     *        <code>NULL</code> this count must be zero.
     *
     * @param scripts is an array of script codes. If this pointer in <code>NULL</code>
     *        the script runs will be determined using the Unicode code points.
     *
     * @param scriptRunLimits is an array of run limits for each script run. If <code>scripts</code>
     *        is <code>NULL</code> this pointer must be <code>NULL</code> too.
     *
     * @param scriptRunCount is the number of script runs. If <code>scripts</code> is
     *        <code>NULL</code> this count must be zero.
     *
     * @param paragraphLevel is the directionality of the paragraph, as in the UBiDi object.
     *
     * @param vertical is <code>true</code> if the paragraph should be set vertically.
     *
     * @see ubidi.h
     * @see LEFontInstance.h
     * @see LayoutEngine.h
     *
     * @draft
     */
    ParagraphLayout(const LEUnicode chars[], le_int32 count,
                    const LEFontInstance **fonts, const le_int32 fontRunLimits[], le_int32 fontRunCount,
                    const UBiDiLevel levels[], const le_int32 levelRunLimits[], le_int32 levelRunCount,
                    const UScriptCode scripts[], const le_int32 scriptRunLimits[], le_int32 scriptRunCount,
                    UBiDiLevel paragraphLevel, le_bool vertical);

    ~ParagraphLayout();

#if 0
    /**
     * Examine the given styled paragraph and determine if it contains any text which
     * requires complex processing. (i.e. that cannot be correctly rendered by
     * just mapping the characters to glyphs and rendering them in order)
     *
     * @param chars is an array of the characters in the paragraph
     *
     * @param count is the number of characters in the paragraph.
     *
     * @param fonts is an array of the <code>LEFontInstance</code> objects associated
     *        with each font run.
     *
     * @param fontRunLimits is an array of the run limits of each font run.
     *
     * @param fontRunCount is the number of font runs.
     *
     * @return <code>true</code> if the paragraph contains complex text.
     */
    static le_bool isComplex(const LEUnicode chars[], le_int32 count,
                      const LEFontInstance *fonts[], const le_int32 fontRunLimits[], le_int32 fontRunCount);
#else
    /**
     * Examine the given text and determine if it contains characters in any
     * script which requires complex processing to be rendered correctly.
     *
     * @param chars is an array of the characters in the paragraph
     *
     * @param count is the number of characters in the paragraph.
     *
     * @return <code>true</code> if any of the text requires complex processing.
     */
    static le_bool isComplex(const LEUnicode chars[], le_int32 count);

#endif

    /**
     * Return the resolved paragraph level. This is useful for those cases
     * where the bidi analysis has determined the level based on the first
     * strong character in the paragraph.
     *
     * @return the resolved paragraph level.
     */
    UBiDiLevel getParagraphLevel();

    /**
     * Return the directionality of the text in the paragraph.
     *
     * @return <code>UBIDI_LTR</code> if the text is all left to right,
     *         <code>UBIDI_RTL</code> if the text is all right to left,
     *         or <code>UBIDI_MIXED</code> if the text has mixed direction.
     */
    UBiDiDirection getTextDirection();

    /**
     * Reset line breaking to start from the beginning of the paragraph.
     *
     */
    void reflow();

    /**
     * Find the next line in the paragraph. The width of the line is specified
     * each time so that it can be varied to support arbitrary paragraph shapes.
     *
     * @param width is the width of the line.
     *
     * @return the offset of the first character which will not fit on the line, or -1
     * if there are no more lines in the paragraph.
     */
    le_int32 nextLineBreak(float width);

    /**
     * Count the number of runs in the line. Each run will contain glyphs
     * in the same font and direction.
     *
     * @return the number of runs on the line.
     */
    le_int32 countLineRuns();

    /**
     * Get the glyphs in the a visual run of the current line.
     *
     * @param runIndex is the index of the run.
     *
     * @param glyphs is an array which will receive the glyphs in the run, in visual order. If this
     *        is <code>NULL</code>, no glyphs are retreived.
     *
     * @param positions is an array which will receive the x and y position for
     *        each glyph in the run. This array will contain two entries for each glyph. The entry
     *        at the even offset will be the x position of the glyph, and the entry at the following
     *        odd offset will be the y position. There will be an extra pair of positions at the end
     *        of the array to specify the position of the glyph following the run. If this is <code>NULL</code>
     *        no positions are stored.
     *
     * @param glyphToCharMap is an array which will receive the original character offset for each glyph
     *        in the run. If this is <code>NULL</code> no character offsets are stored.
     *
     * @param font will receive the <code>LEFontInstance</code> object associated with this run.
     *
     * @param runLevel will receive the run direction.
     *
     * @return the number of glyphs in the run, or -1 if <code>runIndex</code> is out or range.
     *
     * NOTE: All input arrays are owned by the Caller. You can call this method with <code>glyphs</code>,
     * <code>advances</code>, and <code>glyphToCharMap</code> all set to <code>NULL</code> to get the number
     * of glyphs in the run, allocate the arrays, and call this method again to fill the arrays.
     */
    le_int32 getVisualRun(le_int32 runIndex, LEGlyphID glyphs[], float positions[], le_int32 glyphToCharMap[],
                     const LEFontInstance **font, UBiDiDirection *runDirection);

private:

    void computeLevels(UBiDiLevel paragraphLevel);

    void computeVisualRuns();

    void computeScripts();

    le_int32 getCharRun(le_int32 charIndex);

    static le_bool isComplex(UScriptCode script);

    le_int32 previousBreak(le_int32 charIndex);


    const LEUnicode *fChars;
          le_int32   fCharCount;

    const LEFontInstance **fFonts;
    const le_int32        *fFontRunLimits;
          le_int32         fFontRunCount;

    const UBiDiLevel *fLevels;
    const le_int32   *fLevelRunLimits;
          le_int32    fLevelRunCount;

    const UScriptCode *fScripts;
    const le_int32    *fScriptRunLimits;
          le_int32     fScriptRunCount;

          le_bool fVertical;
          le_bool fClientLevels;
          le_bool fClientScripts;

          UBiDiLevel *fEmbeddingLevels;

          le_int32 *fGlyphToCharMap;
          le_int32 *fCharToGlyphMap;
          float    *fGlyphWidths;
          le_int32  fGlyphCount;

          UBiDi *fParaBidi;
          UBiDi *fLineBidi;

          le_int32     *fStyleRunLimits;
          le_int32     *fStyleIndices;
          StyleRunInfo *fStyleRunInfo;
          le_int32      fStyleRunCount;

          BreakIterator *fBreakIterator;
          le_int32       fLineStart;
          le_int32       fLineEnd;

          VisualRunInfo *fVisualRuns;
          le_int32       fVisualRunCount;
          le_int32       fFirstVisualRun;
          le_int32       fLastVisualRun;
          float          fVisualRunLastX;
          float          fVisualRunLastY;

    static le_bool fComplexTable[];
};

inline UBiDiLevel ParagraphLayout::getParagraphLevel()
{
    return ubidi_getParaLevel(fParaBidi);
}

inline UBiDiDirection ParagraphLayout::getTextDirection()
{
    return ubidi_getDirection(fParaBidi);
}

inline void ParagraphLayout::reflow()
{
    fLineEnd = 0;
}

#endif


