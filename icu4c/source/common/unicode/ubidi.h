/*
******************************************************************************
*
*   Copyright (C) 1999-2004, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
******************************************************************************
*   file name:  ubidi.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 1999jul27
*   created by: Markus W. Scherer
*/

#ifndef UBIDI_H
#define UBIDI_H

#include "unicode/utypes.h"
#include "unicode/uchar.h"

/*
 * javadoc-style comments are intended to be transformed into HTML
 * using DOC++ - see
 * http://www.zib.de/Visual/software/doc++/index.html .
 *
 * The HTML documentation is created with
 *  doc++ -H ubidi.h
 *
 * The following #define trick allows us to do it all in one file
 * and still be able to compile it.
 */
/*#define DOCXX_TAG*/
/*#define BIDI_SAMPLE_CODE*/

/**
 *\file
 * \brief C API: BIDI algorithm
 *
 * <h2>BIDI algorithm for ICU</h2>
 *
 * This is an implementation of the Unicode Bidirectional algorithm.
 * The algorithm is defined in the
 * <a href="http://www.unicode.org/unicode/reports/tr9/">Unicode Standard Annex #9</a>,
 * version 13, also described in The Unicode Standard, Version 4.0 .<p>
 *
 * Note: Libraries that perform a bidirectional algorithm and
 * reorder strings accordingly are sometimes called "Storage Layout Engines".
 * ICU's BiDi and shaping (u_shapeArabic()) APIs can be used at the core of such
 * "Storage Layout Engines".
 *
 * <h3>General remarks about the API:</h3>
 *
 * In functions with an error code parameter,
 * the <code>pErrorCode</code> pointer must be valid
 * and the value that it points to must not indicate a failure before
 * the function call. Otherwise, the function returns immediately.
 * After the function call, the value indicates success or failure.<p>
 *
 * The &quot;limit&quot; of a sequence of characters is the position just after their
 * last character, i.e., one more than that position.<p>
 *
 * Some of the API functions provide access to &quot;runs&quot;.
 * Such a &quot;run&quot; is defined as a sequence of characters
 * that are at the same embedding level
 * after performing the BIDI algorithm.<p>
 *
 * @author Markus W. Scherer
 * @version 1.0
 *
 *
 * <h4> Sample code for the ICU BIDI API </h4>
 *
 * <h5>Rendering a paragraph with the ICU BiDi API</h5>
 *
 * This is (hypothetical) sample code that illustrates
 * how the ICU BiDi API could be used to render a paragraph of text.
 * Rendering code depends highly on the graphics system,
 * therefore this sample code must make a lot of assumptions,
 * which may or may not match any existing graphics system's properties.
 *
 * <p>The basic assumptions are:</p>
 * <ul>
 * <li>Rendering is done from left to right on a horizontal line.</li>
 * <li>A run of single-style, unidirectional text can be rendered at once.</li>
 * <li>Such a run of text is passed to the graphics system with
 *     characters (code units) in logical order.</li>
 * <li>The line-breaking algorithm is very complicated
 *     and Locale-dependent -
 *     and therefore its implementation omitted from this sample code.</li>
 * </ul>
 *
 * <pre>
 * \code
 *#include "unicode/ubidi.h"
 *
 *typedef enum {
 *     styleNormal=0, styleSelected=1,
 *     styleBold=2, styleItalics=4,
 *     styleSuper=8, styleSub=16
 *} Style;
 *
 *typedef struct { int32_t limit; Style style; } StyleRun;
 *
 *int getTextWidth(const UChar *text, int32_t start, int32_t limit,
 *                  const StyleRun *styleRuns, int styleRunCount);
 *
 * // set *pLimit and *pStyleRunLimit for a line
 * // from text[start] and from styleRuns[styleRunStart]
 * // using ubidi_getLogicalRun(para, ...)
 *void getLineBreak(const UChar *text, int32_t start, int32_t *pLimit,
 *                  UBiDi *para,
 *                  const StyleRun *styleRuns, int styleRunStart, int *pStyleRunLimit,
 *                  int *pLineWidth);
 *
 * // render runs on a line sequentially, always from left to right
 *
 * // prepare rendering a new line
 * void startLine(UBiDiDirection textDirection, int lineWidth);
 *
 * // render a run of text and advance to the right by the run width
 * // the text[start..limit-1] is always in logical order
 * void renderRun(const UChar *text, int32_t start, int32_t limit,
 *               UBiDiDirection textDirection, Style style);
 *
 * // We could compute a cross-product
 * // from the style runs with the directional runs
 * // and then reorder it.
 * // Instead, here we iterate over each run type
 * // and render the intersections -
 * // with shortcuts in simple (and common) cases.
 * // renderParagraph() is the main function.
 *
 * // render a directional run with
 * // (possibly) multiple style runs intersecting with it
 * void renderDirectionalRun(const UChar *text,
 *                           int32_t start, int32_t limit,
 *                           UBiDiDirection direction,
 *                           const StyleRun *styleRuns, int styleRunCount) {
 *     int i;
 *
 *     // iterate over style runs
 *     if(direction==UBIDI_LTR) {
 *         int styleLimit;
 *
 *         for(i=0; i<styleRunCount; ++i) {
 *             styleLimit=styleRun[i].limit;
 *             if(start<styleLimit) {
 *                 if(styleLimit>limit) { styleLimit=limit; }
 *                 renderRun(text, start, styleLimit,
 *                           direction, styleRun[i].style);
 *                 if(styleLimit==limit) { break; }
 *                 start=styleLimit;
 *             }
 *         }
 *     } else {
 *         int styleStart;
 *
 *         for(i=styleRunCount-1; i>=0; --i) {
 *             if(i>0) {
 *                 styleStart=styleRun[i-1].limit;
 *             } else {
 *                 styleStart=0;
 *             }
 *             if(limit>=styleStart) {
 *                 if(styleStart<start) { styleStart=start; }
 *                 renderRun(text, styleStart, limit,
 *                           direction, styleRun[i].style);
 *                 if(styleStart==start) { break; }
 *                 limit=styleStart;
 *             }
 *         }
 *     }
 * }
 *
 * // the line object represents text[start..limit-1]
 * void renderLine(UBiDi *line, const UChar *text,
 *                 int32_t start, int32_t limit,
 *                 const StyleRun *styleRuns, int styleRunCount) {
 *     UBiDiDirection direction=ubidi_getDirection(line);
 *     if(direction!=UBIDI_MIXED) {
 *         // unidirectional
 *         if(styleRunCount<=1) {
 *             renderRun(text, start, limit, direction, styleRuns[0].style);
 *         } else {
 *             renderDirectionalRun(text, start, limit,
 *                                  direction, styleRuns, styleRunCount);
 *         }
 *     } else {
 *         // mixed-directional
 *         int32_t count, i, length;
 *         UBiDiLevel level;
 *
 *         count=ubidi_countRuns(para, pErrorCode);
 *         if(U_SUCCESS(*pErrorCode)) {
 *             if(styleRunCount<=1) {
 *                 Style style=styleRuns[0].style;
 *
 *                 // iterate over directional runs
 *                for(i=0; i<count; ++i) {
 *                    direction=ubidi_getVisualRun(para, i, &start, &length);
 *                     renderRun(text, start, start+length, direction, style);
 *                }
 *             } else {
 *                 int32_t j;
 *
 *                 // iterate over both directional and style runs
 *                 for(i=0; i<count; ++i) {
 *                     direction=ubidi_getVisualRun(line, i, &start, &length);
 *                     renderDirectionalRun(text, start, start+length,
 *                                          direction, styleRuns, styleRunCount);
 *                 }
 *             }
 *         }
 *     }
 * }
 *
 *void renderParagraph(const UChar *text, int32_t length,
 *                     UBiDiDirection textDirection,
 *                      const StyleRun *styleRuns, int styleRunCount,
 *                      int lineWidth,
 *                      UErrorCode *pErrorCode) {
 *     UBiDi *para;
 *
 *     if(pErrorCode==NULL || U_FAILURE(*pErrorCode) || length<=0) {
 *         return;
 *     }
 *
 *     para=ubidi_openSized(length, 0, pErrorCode);
 *     if(para==NULL) { return; }
 *
 *     ubidi_setPara(para, text, length,
 *                   textDirection ? UBIDI_DEFAULT_RTL : UBIDI_DEFAULT_LTR,
 *                   NULL, pErrorCode);
 *     if(U_SUCCESS(*pErrorCode)) {
 *         UBiDiLevel paraLevel=1&ubidi_getParaLevel(para);
 *         StyleRun styleRun={ length, styleNormal };
 *         int width;
 *
 *         if(styleRuns==NULL || styleRunCount<=0) {
 *            styleRunCount=1;
 *             styleRuns=&styleRun;
 *         }
 *
 *        // assume styleRuns[styleRunCount-1].limit>=length
 *
 *         width=getTextWidth(text, 0, length, styleRuns, styleRunCount);
 *         if(width<=lineWidth) {
 *             // everything fits onto one line
 *
 *            // prepare rendering a new line from either left or right
 *             startLine(paraLevel, width);
 *
 *             renderLine(para, text, 0, length,
 *                        styleRuns, styleRunCount);
 *         } else {
 *             UBiDi *line;
 *
 *             // we need to render several lines
 *             line=ubidi_openSized(length, 0, pErrorCode);
 *             if(line!=NULL) {
 *                 int32_t start=0, limit;
 *                 int styleRunStart=0, styleRunLimit;
 *
 *                 for(;;) {
 *                     limit=length;
 *                     styleRunLimit=styleRunCount;
 *                     getLineBreak(text, start, &limit, para,
 *                                  styleRuns, styleRunStart, &styleRunLimit,
 *                                 &width);
 *                     ubidi_setLine(para, start, limit, line, pErrorCode);
 *                     if(U_SUCCESS(*pErrorCode)) {
 *                         // prepare rendering a new line
 *                         // from either left or right
 *                         startLine(paraLevel, width);
 *
 *                         renderLine(line, text, start, limit,
 *                                    styleRuns+styleRunStart,
 *                                    styleRunLimit-styleRunStart);
 *                     }
 *                     if(limit==length) { break; }
 *                     start=limit;
 *                     styleRunStart=styleRunLimit-1;
 *                     if(start>=styleRuns[styleRunStart].limit) {
 *                         ++styleRunStart;
 *                     }
 *                 }
 *
 *                 ubidi_close(line);
 *             }
 *        }
 *    }
 *
 *     ubidi_close(para);
 *}
 *\endcode
 * </pre>
 */

/*DOCXX_TAG*/
/*@{*/

/**
 * UBiDiLevel is the type of the level values in this
 * BiDi implementation.
 * It holds an embedding level and indicates the visual direction
 * by its bit&nbsp;0 (even/odd value).<p>
 *
 * It can also hold non-level values for the
 * <code>paraLevel</code> and <code>embeddingLevels</code>
 * arguments of <code>ubidi_setPara()</code>; there:
 * <ul>
 * <li>bit&nbsp;7 of an <code>embeddingLevels[]</code>
 * value indicates whether the using application is
 * specifying the level of a character to <i>override</i> whatever the
 * BiDi implementation would resolve it to.</li>
 * <li><code>paraLevel</code> can be set to the
 * pseudo-level values <code>UBIDI_DEFAULT_LTR</code>
 * and <code>UBIDI_DEFAULT_RTL</code>.</li>
 * </ul>
 *
 * @see ubidi_setPara
 *
 * <p>The related constants are not real, valid level values.
 * <code>UBIDI_DEFAULT_XXX</code> can be used to specify
 * a default for the paragraph level for
 * when the <code>ubidi_setPara()</code> function
 * shall determine it but there is no
 * strongly typed character in the input.<p>
 *
 * Note that the value for <code>UBIDI_DEFAULT_LTR</code> is even
 * and the one for <code>UBIDI_DEFAULT_RTL</code> is odd,
 * just like with normal LTR and RTL level values -
 * these special values are designed that way. Also, the implementation
 * assumes that UBIDI_MAX_EXPLICIT_LEVEL is odd.
 *
 * @see UBIDI_DEFAULT_LTR
 * @see UBIDI_DEFAULT_RTL
 * @see UBIDI_LEVEL_OVERRIDE
 * @see UBIDI_MAX_EXPLICIT_LEVEL
 * @stable ICU 2.0
 */
typedef uint8_t UBiDiLevel;

/** Paragraph level setting.
 *  If there is no strong character, then set the paragraph level to 0 (left-to-right).
 * @stable ICU 2.0
 */
#define UBIDI_DEFAULT_LTR 0xfe

/** Paragraph level setting.
 *  If there is no strong character, then set the paragraph level to 1 (right-to-left).
 * @stable ICU 2.0
 */
#define UBIDI_DEFAULT_RTL 0xff

/**
 * Maximum explicit embedding level.
 * (The maximum resolved level can be up to <code>UBIDI_MAX_EXPLICIT_LEVEL+1</code>).
 * @stable ICU 2.0
 */
#define UBIDI_MAX_EXPLICIT_LEVEL 61

/** Bit flag for level input.
 *  Overrides directional properties.
 * @stable ICU 2.0
 */
#define UBIDI_LEVEL_OVERRIDE 0x80

/**
 * @memo <code>UBiDiDirection</code> values indicate the text direction.
 * @stable ICU 2.0
 */
enum UBiDiDirection {
    /** @memo All left-to-right text. This is a 0 value. @stable ICU 2.0 */
    UBIDI_LTR,
    /** @memo All right-to-left text. This is a 1 value. @stable ICU 2.0 */
    UBIDI_RTL,
    /** @memo Mixed-directional text. @stable ICU 2.0 */
    UBIDI_MIXED
};

/** @stable ICU 2.0 */
typedef enum UBiDiDirection UBiDiDirection;

/**
 * Forward declaration of the <code>UBiDi</code> structure for the declaration of
 * the API functions. Its fields are implementation-specific.<p>
 * This structure holds information about a paragraph of text
 * with BiDi-algorithm-related details, or about one line of
 * such a paragraph.<p>
 * Reordering can be done on a line, or on a paragraph which is
 * then interpreted as one single line.
 * @stable ICU 2.0
 */
struct UBiDi;

/** @stable ICU 2.0 */
typedef struct UBiDi UBiDi;

/**
 * Allocate a <code>UBiDi</code> structure.
 * Such an object is initially empty. It is assigned
 * the BiDi properties of a paragraph by <code>ubidi_setPara()</code>
 * or the BiDi properties of a line of a paragraph by
 * <code>ubidi_setLine()</code>.<p>
 * This object can be reused for as long as it is not deallocated
 * by calling <code>ubidi_close()</code>.<p>
 * <code>ubidi_set()</code> will allocate additional memory for
 * internal structures as necessary.
 *
 * @return An empty <code>UBiDi</code> object.
 * @stable ICU 2.0
 */
U_STABLE UBiDi * U_EXPORT2
ubidi_open(void);

/**
 * Allocate a <code>UBiDi</code> structure with preallocated memory
 * for internal structures.
 * This function provides a <code>UBiDi</code> object like <code>ubidi_open()</code>
 * with no arguments, but it also preallocates memory for internal structures
 * according to the sizings supplied by the caller.<p>
 * Subsequent functions will not allocate any more memory, and are thus
 * guaranteed not to fail because of lack of memory.<p>
 * The preallocation can be limited to some of the internal memory
 * by setting some values to 0 here. That means that if, e.g.,
 * <code>maxRunCount</code> cannot be reasonably predetermined and should not
 * be set to <code>maxLength</code> (the only failproof value) to avoid
 * wasting memory, then <code>maxRunCount</code> could be set to 0 here
 * and the internal structures that are associated with it will be allocated
 * on demand, just like with <code>ubidi_open()</code>.
 *
 * @param maxLength is the maximum paragraph or line length that internal memory
 *        will be preallocated for. An attempt to associate this object with a
 *        longer text will fail, unless this value is 0, which leaves the allocation
 *        up to the implementation.
 *
 * @param maxRunCount is the maximum anticipated number of same-level runs
 *        that internal memory will be preallocated for. An attempt to access
 *        visual runs on an object that was not preallocated for as many runs
 *        as the text was actually resolved to will fail,
 *        unless this value is 0, which leaves the allocation up to the implementation.<p>
 *        The number of runs depends on the actual text and maybe anywhere between
 *        1 and <code>maxLength</code>. It is typically small.<p>
 *
 * @param pErrorCode must be a valid pointer to an error code value,
 *        which must not indicate a failure before the function call.
 *
 * @return An empty <code>UBiDi</code> object with preallocated memory.
 * @stable ICU 2.0
 */
U_STABLE UBiDi * U_EXPORT2
ubidi_openSized(int32_t maxLength, int32_t maxRunCount, UErrorCode *pErrorCode);

/**
 * <code>ubidi_close()</code> must be called to free the memory
 * associated with a UBiDi object.<p>
 *
 * <strong>Important: </strong>
 * A parent <code>UBiDi</code> object must not be destroyed or reused if
 * it still has children.
 * If a <code>UBiDi</code> object is the <i>child</i>
 * of another one (its <i>parent</i>), after calling
 * <code>ubidi_setLine()</code>, then the child object must
 * be destroyed (closed) or reused (by calling
 * <code>ubidi_setPara()</code> or <code>ubidi_setLine()</code>)
 * before the parent object.
 *
 * @param pBiDi is a <code>UBiDi</code> object.
 *
 * @see ubidi_setPara
 * @see ubidi_setLine
 * @stable ICU 2.0
 */
U_STABLE void U_EXPORT2
ubidi_close(UBiDi *pBiDi);

/**
 * Modify the operation of the BiDi algorithm such that it
 * approximates an "inverse BiDi" algorithm. This function
 * must be called before <code>ubidi_setPara()</code>.
 *
 * <p>The normal operation of the BiDi algorithm as described
 * in the Unicode Technical Report is to take text stored in logical
 * (keyboard, typing) order and to determine the reordering of it for visual
 * rendering.
 * Some legacy systems store text in visual order, and for operations
 * with standard, Unicode-based algorithms, the text needs to be transformed
 * to logical order. This is effectively the inverse algorithm of the
 * described BiDi algorithm. Note that there is no standard algorithm for
 * this "inverse BiDi" and that the current implementation provides only an
 * approximation of "inverse BiDi".</p>
 *
 * <p>With <code>isInverse</code> set to <code>TRUE</code>,
 * this function changes the behavior of some of the subsequent functions
 * in a way that they can be used for the inverse BiDi algorithm.
 * Specifically, runs of text with numeric characters will be treated in a
 * special way and may need to be surrounded with LRM characters when they are
 * written in reordered sequence.</p>
 *
 * <p>Output runs should be retrieved using <code>ubidi_getVisualRun()</code>.
 * Since the actual input for "inverse BiDi" is visually ordered text and
 * <code>ubidi_getVisualRun()</code> gets the reordered runs, these are actually
 * the runs of the logically ordered output.</p>
 *
 * @param pBiDi is a <code>UBiDi</code> object.
 *
 * @param isInverse specifies "forward" or "inverse" BiDi operation
 *
 * @see ubidi_setPara
 * @see ubidi_writeReordered
 * @stable ICU 2.0
 */
U_STABLE void U_EXPORT2
ubidi_setInverse(UBiDi *pBiDi, UBool isInverse);

/**
 * Is this BiDi object set to perform the inverse BiDi algorithm?
 *
 * @param pBiDi is a <code>UBiDi</code> object.
 * @return TRUE if the BiDi object set to perform the inverse BiDi algorithm
 *
 * @see ubidi_setInverse
 * @stable ICU 2.0
 */
U_STABLE UBool U_EXPORT2
ubidi_isInverse(UBiDi *pBiDi);

/**
 * Perform the Unicode BiDi algorithm. It is defined in the
 * <a href="http://www.unicode.org/unicode/reports/tr9/">Unicode Standard Anned #9</a>,
 * version 13,
 * also described in The Unicode Standard, Version 4.0 .<p>
 *
 * This function takes a single plain text paragraph with or without
 * externally specified embedding levels from <i>styled</i> text
 * and computes the left-right-directionality of each character.<p>
 *
 * If the entire paragraph consists of text of only one direction, then
 * the function may not perform all the steps described by the algorithm,
 * i.e., some levels may not be the same as if all steps were performed.
 * This is not relevant for unidirectional text.<br>
 * For example, in pure LTR text with numbers the numbers would get
 * a resolved level of 2 higher than the surrounding text according to
 * the algorithm. This implementation may set all resolved levels to
 * the same value in such a case.<p>
 *
 * The text must be externally split into separate paragraphs (rule P1).
 * Paragraph separators (B) should appear at most at the very end.
 *
 * @param pBiDi A <code>UBiDi</code> object allocated with <code>ubidi_open()</code>
 *        which will be set to contain the reordering information,
 *        especially the resolved levels for all the characters in <code>text</code>.
 *
 * @param text is a pointer to the single-paragraph text that the
 *        BiDi algorithm will be performed on
 *        (step (P1) of the algorithm is performed externally).
 *        <strong>The text must be (at least) <code>length</code> long.</strong>
 *        This pointer is stored in the UBiDi object and can be retrieved
 *        with <code>ubidi_getText()</code>.
 *
 * @param length is the length of the text; if <code>length==-1</code> then
 *        the text must be zero-terminated.
 *
 * @param paraLevel specifies the default level for the paragraph;
 *        it is typically 0 (LTR) or 1 (RTL).
 *        If the function shall determine the paragraph level from the text,
 *        then <code>paraLevel</code> can be set to
 *        either <code>UBIDI_DEFAULT_LTR</code>
 *        or <code>UBIDI_DEFAULT_RTL</code>;
 *        if there is no strongly typed character, then
 *        the desired default is used (0 for LTR or 1 for RTL).
 *        Any other value between 0 and <code>UBIDI_MAX_EXPLICIT_LEVEL</code> is also valid,
 *        with odd levels indicating RTL.
 *
 * @param embeddingLevels (in) may be used to preset the embedding and override levels,
 *        ignoring characters like LRE and PDF in the text.
 *        A level overrides the directional property of its corresponding
 *        (same index) character if the level has the
 *        <code>UBIDI_LEVEL_OVERRIDE</code> bit set.<p>
 *        Except for that bit, it must be
 *        <code>paraLevel<=embeddingLevels[]<=UBIDI_MAX_EXPLICIT_LEVEL</code>.<p>
 *        <strong>Caution: </strong>A copy of this pointer, not of the levels,
 *        will be stored in the <code>UBiDi</code> object;
 *        the <code>embeddingLevels</code> array must not be
 *        deallocated before the <code>UBiDi</code> structure is destroyed or reused,
 *        and the <code>embeddingLevels</code>
 *        should not be modified to avoid unexpected results on subsequent BiDi operations.
 *        However, the <code>ubidi_setPara()</code> and
 *        <code>ubidi_setLine()</code> functions may modify some or all of the levels.<p>
 *        After the <code>UBiDi</code> object is reused or destroyed, the caller
 *        must take care of the deallocation of the <code>embeddingLevels</code> array.<p>
 *        <strong>The <code>embeddingLevels</code> array must be
 *        at least <code>length</code> long.</strong>
 *
 * @param pErrorCode must be a valid pointer to an error code value,
 *        which must not indicate a failure before the function call.
 * @stable ICU 2.0
 */
U_STABLE void U_EXPORT2
ubidi_setPara(UBiDi *pBiDi, const UChar *text, int32_t length,
              UBiDiLevel paraLevel, UBiDiLevel *embeddingLevels,
              UErrorCode *pErrorCode);

/**
 * <code>ubidi_setLine()</code> sets a <code>UBiDi</code> to
 * contain the reordering information, especially the resolved levels,
 * for all the characters in a line of text. This line of text is
 * specified by referring to a <code>UBiDi</code> object representing
 * this information for a paragraph of text, and by specifying
 * a range of indexes in this paragraph.<p>
 * In the new line object, the indexes will range from 0 to <code>limit-start-1</code>.<p>
 *
 * This is used after calling <code>ubidi_setPara()</code>
 * for a paragraph, and after line-breaking on that paragraph.
 * It is not necessary if the paragraph is treated as a single line.<p>
 *
 * After line-breaking, rules (L1) and (L2) for the treatment of
 * trailing WS and for reordering are performed on
 * a <code>UBiDi</code> object that represents a line.<p>
 *
 * <strong>Important: </strong><code>pLineBiDi</code> shares data with
 * <code>pParaBiDi</code>.
 * You must destroy or reuse <code>pLineBiDi</code> before <code>pParaBiDi</code>.
 * In other words, you must destroy or reuse the <code>UBiDi</code> object for a line
 * before the object for its parent paragraph.<p>
 *
 * The text pointer that was stored in <code>pParaBiDi</code> is also copied,
 * and <code>start</code> is added to it so that it points to the beginning of the
 * line for this object.
 *
 * @param pParaBiDi is the parent paragraph object.
 *
 * @param start is the line's first index into the paragraph text.
 *
 * @param limit is just behind the line's last index into the paragraph text
 *        (its last index +1).<br>
 *        It must be <code>0<=start<=limit<=</code>paragraph length.
 *
 * @param pLineBiDi is the object that will now represent a line of the paragraph.
 *
 * @param pErrorCode must be a valid pointer to an error code value,
 *        which must not indicate a failure before the function call.
 *
 * @see ubidi_setPara
 * @stable ICU 2.0
 */
U_STABLE void U_EXPORT2
ubidi_setLine(const UBiDi *pParaBiDi,
              int32_t start, int32_t limit,
              UBiDi *pLineBiDi,
              UErrorCode *pErrorCode);

/**
 * Get the directionality of the text.
 *
 * @param pBiDi is the paragraph or line <code>UBiDi</code> object.
 *
 * @return A <code>UBIDI_XXX</code> value that indicates if the entire text
 *         represented by this object is unidirectional,
 *         and which direction, or if it is mixed-directional.
 *
 * @see UBiDiDirection
 * @stable ICU 2.0
 */
U_STABLE UBiDiDirection U_EXPORT2
ubidi_getDirection(const UBiDi *pBiDi);

/**
 * Get the pointer to the text.
 *
 * @param pBiDi is the paragraph or line <code>UBiDi</code> object.
 *
 * @return The pointer to the text that the UBiDi object was created for.
 *
 * @see ubidi_setPara
 * @see ubidi_setLine
 * @stable ICU 2.0
 */
U_STABLE const UChar * U_EXPORT2
ubidi_getText(const UBiDi *pBiDi);

/**
 * Get the length of the text.
 *
 * @param pBiDi is the paragraph or line <code>UBiDi</code> object.
 *
 * @return The length of the text that the UBiDi object was created for.
 * @stable ICU 2.0
 */
U_STABLE int32_t U_EXPORT2
ubidi_getLength(const UBiDi *pBiDi);

/**
 * Get the paragraph level of the text.
 *
 * @param pBiDi is the paragraph or line <code>UBiDi</code> object.
 *
 * @return The paragraph level.
 *
 * @see UBiDiLevel
 * @stable ICU 2.0
 */
U_STABLE UBiDiLevel U_EXPORT2
ubidi_getParaLevel(const UBiDi *pBiDi);

/**
 * Get the level for one character.
 *
 * @param pBiDi is the paragraph or line <code>UBiDi</code> object.
 *
 * @param charIndex the index of a character.
 *
 * @return The level for the character at charIndex.
 *
 * @see UBiDiLevel
 * @stable ICU 2.0
 */
U_STABLE UBiDiLevel U_EXPORT2
ubidi_getLevelAt(const UBiDi *pBiDi, int32_t charIndex);

/**
 * Get an array of levels for each character.<p>
 *
 * Note that this function may allocate memory under some
 * circumstances, unlike <code>ubidi_getLevelAt()</code>.
 *
 * @param pBiDi is the paragraph or line <code>UBiDi</code> object.
 *
 * @param pErrorCode must be a valid pointer to an error code value,
 *        which must not indicate a failure before the function call.
 *
 * @return The levels array for the text,
 *         or <code>NULL</code> if an error occurs.
 *
 * @see UBiDiLevel
 * @stable ICU 2.0
 */
U_STABLE const UBiDiLevel * U_EXPORT2
ubidi_getLevels(UBiDi *pBiDi, UErrorCode *pErrorCode);

/**
 * Get a logical run.
 * This function returns information about a run and is used
 * to retrieve runs in logical order.<p>
 * This is especially useful for line-breaking on a paragraph.
 *
 * @param pBiDi is the paragraph or line <code>UBiDi</code> object.
 *
 * @param logicalStart is the first character of the run.
 *
 * @param pLogicalLimit will receive the limit of the run.
 *        The l-value that you point to here may be the
 *        same expression (variable) as the one for
 *        <code>logicalStart</code>.
 *        This pointer can be <code>NULL</code> if this
 *        value is not necessary.
 *
 * @param pLevel will receive the level of the run.
 *        This pointer can be <code>NULL</code> if this
 *        value is not necessary.
 * @stable ICU 2.0
 */
U_STABLE void U_EXPORT2
ubidi_getLogicalRun(const UBiDi *pBiDi, int32_t logicalStart,
                    int32_t *pLogicalLimit, UBiDiLevel *pLevel);

/**
 * Get the number of runs.
 * This function may invoke the actual reordering on the
 * <code>UBiDi</code> object, after <code>ubidi_setPara()</code>
 * may have resolved only the levels of the text. Therefore,
 * <code>ubidi_countRuns()</code> may have to allocate memory,
 * and may fail doing so.
 *
 * @param pBiDi is the paragraph or line <code>UBiDi</code> object.
 *
 * @param pErrorCode must be a valid pointer to an error code value,
 *        which must not indicate a failure before the function call.
 *
 * @return The number of runs.
 * @stable ICU 2.0
 */
U_STABLE int32_t U_EXPORT2
ubidi_countRuns(UBiDi *pBiDi, UErrorCode *pErrorCode);

/**
 * Get one run's logical start, length, and directionality,
 * which can be 0 for LTR or 1 for RTL.
 * In an RTL run, the character at the logical start is
 * visually on the right of the displayed run.
 * The length is the number of characters in the run.<p>
 * <code>ubidi_countRuns()</code> should be called
 * before the runs are retrieved.
 *
 * @param pBiDi is the paragraph or line <code>UBiDi</code> object.
 *
 * @param runIndex is the number of the run in visual order, in the
 *        range <code>[0..ubidi_countRuns(pBiDi)-1]</code>.
 *
 * @param pLogicalStart is the first logical character index in the text.
 *        The pointer may be <code>NULL</code> if this index is not needed.
 *
 * @param pLength is the number of characters (at least one) in the run.
 *        The pointer may be <code>NULL</code> if this is not needed.
 *
 * @return the directionality of the run,
 *         <code>UBIDI_LTR==0</code> or <code>UBIDI_RTL==1</code>,
 *         never <code>UBIDI_MIXED</code>.
 *
 * @see ubidi_countRuns
 *
 * Example:
 * <pre>
 * \code
 * int32_t i, count=ubidi_countRuns(pBiDi),
 *         logicalStart, visualIndex=0, length;
 * for(i=0; i<count; ++i) {
 *    if(UBIDI_LTR==ubidi_getVisualRun(pBiDi, i, &logicalStart, &length)) {
 *         do { // LTR
 *             show_char(text[logicalStart++], visualIndex++);
 *         } while(--length>0);
 *     } else {
 *         logicalStart+=length;  // logicalLimit
 *         do { // RTL
 *             show_char(text[--logicalStart], visualIndex++);
 *         } while(--length>0);
 *     }
 * }
 *\endcode
 * </pre>
 *
 * Note that in right-to-left runs, code like this places
 * modifier letters before base characters and second surrogates
 * before first ones.
 * @stable ICU 2.0
 */
U_STABLE UBiDiDirection U_EXPORT2
ubidi_getVisualRun(UBiDi *pBiDi, int32_t runIndex,
                   int32_t *pLogicalStart, int32_t *pLength);

/**
 * Get the visual position from a logical text position.
 * If such a mapping is used many times on the same
 * <code>UBiDi</code> object, then calling
 * <code>ubidi_getLogicalMap()</code> is more efficient.<p>
 *
 * Note that in right-to-left runs, this mapping places
 * modifier letters before base characters and second surrogates
 * before first ones.
 *
 * @param pBiDi is the paragraph or line <code>UBiDi</code> object.
 *
 * @param logicalIndex is the index of a character in the text.
 *
 * @param pErrorCode must be a valid pointer to an error code value,
 *        which must not indicate a failure before the function call.
 *
 * @return The visual position of this character.
 *
 * @see ubidi_getLogicalMap
 * @see ubidi_getLogicalIndex
 * @stable ICU 2.0
 */
U_STABLE int32_t U_EXPORT2
ubidi_getVisualIndex(UBiDi *pBiDi, int32_t logicalIndex, UErrorCode *pErrorCode);

/**
 * Get the logical text position from a visual position.
 * If such a mapping is used many times on the same
 * <code>UBiDi</code> object, then calling
 * <code>ubidi_getVisualMap()</code> is more efficient.<p>
 *
 * This is the inverse function to <code>ubidi_getVisualIndex()</code>.
 *
 * @param pBiDi is the paragraph or line <code>UBiDi</code> object.
 *
 * @param visualIndex is the visual position of a character.
 *
 * @param pErrorCode must be a valid pointer to an error code value,
 *        which must not indicate a failure before the function call.
 *
 * @return The index of this character in the text.
 *
 * @see ubidi_getVisualMap
 * @see ubidi_getVisualIndex
 * @stable ICU 2.0
 */
U_STABLE int32_t U_EXPORT2
ubidi_getLogicalIndex(UBiDi *pBiDi, int32_t visualIndex, UErrorCode *pErrorCode);

/**
 * Get a logical-to-visual index map (array) for the characters in the UBiDi
 * (paragraph or line) object.
 *
 * @param pBiDi is the paragraph or line <code>UBiDi</code> object.
 *
 * @param indexMap is a pointer to an array of <code>ubidi_getLength()</code>
 *        indexes which will reflect the reordering of the characters.
 *        The array does not need to be initialized.<p>
 *        The index map will result in <code>indexMap[logicalIndex]==visualIndex</code>.<p>
 *
 * @param pErrorCode must be a valid pointer to an error code value,
 *        which must not indicate a failure before the function call.
 *
 * @see ubidi_getVisualMap
 * @see ubidi_getVisualIndex
 * @stable ICU 2.0
 */
U_STABLE void U_EXPORT2
ubidi_getLogicalMap(UBiDi *pBiDi, int32_t *indexMap, UErrorCode *pErrorCode);

/**
 * Get a visual-to-logical index map (array) for the characters in the UBiDi
 * (paragraph or line) object.
 *
 * @param pBiDi is the paragraph or line <code>UBiDi</code> object.
 *
 * @param indexMap is a pointer to an array of <code>ubidi_getLength()</code>
 *        indexes which will reflect the reordering of the characters.
 *        The array does not need to be initialized.<p>
 *        The index map will result in <code>indexMap[visualIndex]==logicalIndex</code>.<p>
 *
 * @param pErrorCode must be a valid pointer to an error code value,
 *        which must not indicate a failure before the function call.
 *
 * @see ubidi_getLogicalMap
 * @see ubidi_getLogicalIndex
 * @stable ICU 2.0
 */
U_STABLE void U_EXPORT2
ubidi_getVisualMap(UBiDi *pBiDi, int32_t *indexMap, UErrorCode *pErrorCode);

/**
 * This is a convenience function that does not use a UBiDi object.
 * It is intended to be used for when an application has determined the levels
 * of objects (character sequences) and just needs to have them reordered (L2).
 * This is equivalent to using <code>ubidi_getLogicalMap</code> on a
 * <code>UBiDi</code> object.
 *
 * @param levels is an array with <code>length</code> levels that have been determined by
 *        the application.
 *
 * @param length is the number of levels in the array, or, semantically,
 *        the number of objects to be reordered.
 *        It must be <code>length>0</code>.
 *
 * @param indexMap is a pointer to an array of <code>length</code>
 *        indexes which will reflect the reordering of the characters.
 *        The array does not need to be initialized.<p>
 *        The index map will result in <code>indexMap[logicalIndex]==visualIndex</code>.
 * @stable ICU 2.0
 */
U_STABLE void U_EXPORT2
ubidi_reorderLogical(const UBiDiLevel *levels, int32_t length, int32_t *indexMap);

/**
 * This is a convenience function that does not use a UBiDi object.
 * It is intended to be used for when an application has determined the levels
 * of objects (character sequences) and just needs to have them reordered (L2).
 * This is equivalent to using <code>ubidi_getVisualMap</code> on a
 * <code>UBiDi</code> object.
 *
 * @param levels is an array with <code>length</code> levels that have been determined by
 *        the application.
 *
 * @param length is the number of levels in the array, or, semantically,
 *        the number of objects to be reordered.
 *        It must be <code>length>0</code>.
 *
 * @param indexMap is a pointer to an array of <code>length</code>
 *        indexes which will reflect the reordering of the characters.
 *        The array does not need to be initialized.<p>
 *        The index map will result in <code>indexMap[visualIndex]==logicalIndex</code>.
 * @stable ICU 2.0
 */
U_STABLE void U_EXPORT2
ubidi_reorderVisual(const UBiDiLevel *levels, int32_t length, int32_t *indexMap);

/**
 * Invert an index map.
 * The one-to-one index mapping of the first map is inverted and written to
 * the second one.
 *
 * @param srcMap is an array with <code>length</code> indexes
 *        which define the original mapping.
 *
 * @param destMap is an array with <code>length</code> indexes
 *        which will be filled with the inverse mapping.
 *
 * @param length is the length of each array.
 * @stable ICU 2.0
 */
U_STABLE void U_EXPORT2
ubidi_invertMap(const int32_t *srcMap, int32_t *destMap, int32_t length);

/** option flags for ubidi_writeReordered() */

/**
 * option bit for ubidi_writeReordered():
 * keep combining characters after their base characters in RTL runs
 *
 * @see ubidi_writeReordered
 * @stable ICU 2.0
 */
#define UBIDI_KEEP_BASE_COMBINING       1

/**
 * option bit for ubidi_writeReordered():
 * replace characters with the "mirrored" property in RTL runs
 * by their mirror-image mappings
 *
 * @see ubidi_writeReordered
 * @stable ICU 2.0
 */
#define UBIDI_DO_MIRRORING              2

/**
 * option bit for ubidi_writeReordered():
 * surround the run with LRMs if necessary;
 * this is part of the approximate "inverse BiDi" algorithm
 *
 * @see ubidi_setInverse
 * @see ubidi_writeReordered
 * @stable ICU 2.0
 */
#define UBIDI_INSERT_LRM_FOR_NUMERIC    4

/**
 * option bit for ubidi_writeReordered():
 * remove BiDi control characters
 * (this does not affect UBIDI_INSERT_LRM_FOR_NUMERIC)
 *
 * @see ubidi_writeReordered
 * @stable ICU 2.0
 */
#define UBIDI_REMOVE_BIDI_CONTROLS      8

/**
 * option bit for ubidi_writeReordered():
 * write the output in reverse order
 *
 * <p>This has the same effect as calling <code>ubidi_writeReordered()</code>
 * first without this option, and then calling
 * <code>ubidi_writeReverse()</code> without mirroring.
 * Doing this in the same step is faster and avoids a temporary buffer.
 * An example for using this option is output to a character terminal that
 * is designed for RTL scripts and stores text in reverse order.</p>
 *
 * @see ubidi_writeReordered
 * @stable ICU 2.0
 */
#define UBIDI_OUTPUT_REVERSE            16

/**
 * Take a <code>UBiDi</code> object containing the reordering
 * information for one paragraph or line of text as set by
 * <code>ubidi_setPara()</code> or <code>ubidi_setLine()</code> and
 * write a reordered string to the destination buffer.
 *
 * This function preserves the integrity of characters with multiple
 * code units and (optionally) modifier letters.
 * Characters in RTL runs can be replaced by mirror-image characters
 * in the destination buffer. Note that "real" mirroring has
 * to be done in a rendering engine by glyph selection
 * and that for many "mirrored" characters there are no
 * Unicode characters as mirror-image equivalents.
 * There are also options to insert or remove BiDi control
 * characters; see the description of the <code>destSize</code>
 * and <code>options</code> parameters and of the option bit flags.
 *
 * @see UBIDI_DO_MIRRORING
 * @see UBIDI_INSERT_LRM_FOR_NUMERIC
 * @see UBIDI_KEEP_BASE_COMBINING
 * @see UBIDI_OUTPUT_REVERSE
 * @see UBIDI_REMOVE_BIDI_CONTROLS
 *
 * @param pBiDi A pointer to a <code>UBiDi</code> object that
 *              is set by <code>ubidi_setPara()</code> or
 *              <code>ubidi_setLine()</code> and contains the reordering
 *              information for the text that it was defined for,
 *              as well as a pointer to that text.
 *              <p>The text was aliased (only the pointer was stored
 *              without copying the contents) and must not have been modified
 *              since the <code>ubidi_setPara()</code> call.</p>
 *
 * @param dest A pointer to where the reordered text is to be copied.
 *             The source text and <code>dest[destSize]</code>
 *             must not overlap.
 *
 * @param destSize The size of the <code>dest</code> buffer,
 *                 in number of UChars.
 *                 If the <code>UBIDI_INSERT_LRM_FOR_NUMERIC</code>
 *                 option is set, then the destination length could be
 *                 as large as
 *                 <code>ubidi_getLength(pBiDi)+2*ubidi_countRuns(pBiDi)</code>.
 *                 If the <code>UBIDI_REMOVE_BIDI_CONTROLS</code> option
 *                 is set, then the destination length may be less than
 *                 <code>ubidi_getLength(pBiDi)</code>.
 *                 If none of these options is set, then the destination length
 *                 will be exactly <code>ubidi_getLength(pBiDi)</code>.
 *
 * @param options A bit set of options for the reordering that control
 *                how the reordered text is written.
 *                The options include mirroring the characters on a code
 *                point basis and inserting LRM characters, which is used
 *                especially for transforming visually stored text
 *                to logically stored text (although this is still an
 *                imperfect implementation of an "inverse BiDi" algorithm
 *                because it uses the "forward BiDi" algorithm at its core).
 *                The available options are:
 *                <code>#UBIDI_DO_MIRRORING</code>,
 *                <code>#UBIDI_INSERT_LRM_FOR_NUMERIC</code>,
 *                <code>#UBIDI_KEEP_BASE_COMBINING</code>,
 *                <code>#UBIDI_OUTPUT_REVERSE</code>,
 *                <code>#UBIDI_REMOVE_BIDI_CONTROLS</code>
 *
 * @param pErrorCode must be a valid pointer to an error code value,
 *        which must not indicate a failure before the function call.
 *
 * @return The length of the output string.
 * @stable ICU 2.0
 */
U_STABLE int32_t U_EXPORT2
ubidi_writeReordered(UBiDi *pBiDi,
                     UChar *dest, int32_t destSize,
                     uint16_t options,
                     UErrorCode *pErrorCode);

/**
 * Reverse a Right-To-Left run of Unicode text.
 *
 * This function preserves the integrity of characters with multiple
 * code units and (optionally) modifier letters.
 * Characters can be replaced by mirror-image characters
 * in the destination buffer. Note that "real" mirroring has
 * to be done in a rendering engine by glyph selection
 * and that for many "mirrored" characters there are no
 * Unicode characters as mirror-image equivalents.
 * There are also options to insert or remove BiDi control
 * characters.
 *
 * This function is the implementation for reversing RTL runs as part
 * of <code>ubidi_writeReordered()</code>. For detailed descriptions
 * of the parameters, see there.
 * Since no BiDi controls are inserted here, the output string length
 * will never exceed <code>srcLength</code>.
 *
 * @see ubidi_writeReordered
 *
 * @param src A pointer to the RTL run text.
 *
 * @param srcLength The length of the RTL run.
 *
 * @param dest A pointer to where the reordered text is to be copied.
 *             <code>src[srcLength]</code> and <code>dest[destSize]</code>
 *             must not overlap.
 *
 * @param destSize The size of the <code>dest</code> buffer,
 *                 in number of UChars.
 *                 If the <code>UBIDI_REMOVE_BIDI_CONTROLS</code> option
 *                 is set, then the destination length may be less than
 *                 <code>srcLength</code>.
 *                 If this option is not set, then the destination length
 *                 will be exactly <code>srcLength</code>.
 *
 * @param options A bit set of options for the reordering that control
 *                how the reordered text is written.
 *                See the <code>options</code> parameter in <code>ubidi_writeReordered()</code>.
 *
 * @param pErrorCode must be a valid pointer to an error code value,
 *        which must not indicate a failure before the function call.
 *
 * @return The length of the output string.
 * @stable ICU 2.0
 */
U_STABLE int32_t U_EXPORT2
ubidi_writeReverse(const UChar *src, int32_t srcLength,
                   UChar *dest, int32_t destSize,
                   uint16_t options,
                   UErrorCode *pErrorCode);

/*#define BIDI_SAMPLE_CODE*/
/*@}*/

#endif
