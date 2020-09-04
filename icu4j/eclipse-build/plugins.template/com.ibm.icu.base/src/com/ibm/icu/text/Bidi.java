// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
*******************************************************************************
*   Copyright (C) 2001-2012, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*/

/* FOOD FOR THOUGHT: currently the reordering modes are a mixture of
 * algorithm for direct BiDi, algorithm for inverse Bidi and the bizarre
 * concept of RUNS_ONLY which is a double operation.
 * It could be advantageous to divide this into 3 concepts:
 * a) Operation: direct / inverse / RUNS_ONLY
 * b) Direct algorithm: default / NUMBERS_SPECIAL / GROUP_NUMBERS_WITH_L
 * c) Inverse algorithm: default / INVERSE_LIKE_DIRECT / NUMBERS_SPECIAL
 * This would allow combinations not possible today like RUNS_ONLY with
 * NUMBERS_SPECIAL.
 * Also allow to set INSERT_MARKS for the direct step of RUNS_ONLY and
 * REMOVE_CONTROLS for the inverse step.
 * Not all combinations would be supported, and probably not all do make sense.
 * This would need to document which ones are supported and what are the
 * fallbacks for unsupported combinations.
 */

//TODO: make sample program do something simple but real and complete

package com.ibm.icu.text;

import java.text.AttributedCharacterIterator;

/**
 *
 * <h2>Bidi algorithm for ICU</h2>
 *
 * This is an implementation of the Unicode Bidirectional algorithm. The
 * algorithm is defined in the <a
 * href="http://www.unicode.org/unicode/reports/tr9/">Unicode Standard Annex #9</a>,
 * version 13, also described in The Unicode Standard, Version 4.0 .
 * <p>
 *
 * Note: Libraries that perform a bidirectional algorithm and reorder strings
 * accordingly are sometimes called "Storage Layout Engines". ICU's Bidi and
 * shaping (ArabicShaping) classes can be used at the core of such "Storage
 * Layout Engines".
 *
 * <h3>General remarks about the API:</h3>
 *
 * The &quot;limit&quot; of a sequence of characters is the position just after
 * their last character, i.e., one more than that position.
 * <p>
 *
 * Some of the API methods provide access to &quot;runs&quot;. Such a
 * &quot;run&quot; is defined as a sequence of characters that are at the same
 * embedding level after performing the Bidi algorithm.
 * <p>
 *
 * <h3>Basic concept: paragraph</h3>
 * A piece of text can be divided into several paragraphs by characters
 * with the Bidi class <code>Block Separator</code>. For handling of
 * paragraphs, see:
 * <ul>
 * <li>{@link #countParagraphs}
 * <li>{@link #getParaLevel}
 * <li>{@link #getParagraph}
 * <li>{@link #getParagraphByIndex}
 * </ul>
 *
 * <h3>Basic concept: text direction</h3>
 * The direction of a piece of text may be:
 * <ul>
 * <li>{@link #LTR}
 * <li>{@link #RTL}
 * <li>{@link #MIXED}
 * </ul>
 *
 * <h3>Basic concept: levels</h3>
 *
 * Levels in this API represent embedding levels according to the Unicode
 * Bidirectional Algorithm.
 * Their low-order bit (even/odd value) indicates the visual direction.<p>
 *
 * Levels can be abstract values when used for the
 * <code>paraLevel</code> and <code>embeddingLevels</code>
 * arguments of <code>setPara()</code>; there:
 * <ul>
 * <li>the high-order bit of an <code>embeddingLevels[]</code>
 * value indicates whether the using application is
 * specifying the level of a character to <i>override</i> whatever the
 * Bidi implementation would resolve it to.</li>
 * <li><code>paraLevel</code> can be set to the
 * pseudo-level values <code>LEVEL_DEFAULT_LTR</code>
 * and <code>LEVEL_DEFAULT_RTL</code>.</li>
 * </ul>
 *
 * <p>The related constants are not real, valid level values.
 * <code>DEFAULT_XXX</code> can be used to specify
 * a default for the paragraph level for
 * when the <code>setPara()</code> method
 * shall determine it but there is no
 * strongly typed character in the input.<p>
 *
 * Note that the value for <code>LEVEL_DEFAULT_LTR</code> is even
 * and the one for <code>LEVEL_DEFAULT_RTL</code> is odd,
 * just like with normal LTR and RTL level values -
 * these special values are designed that way. Also, the implementation
 * assumes that MAX_EXPLICIT_LEVEL is odd.
 *
 * <ul><b>See Also:</b>
 * <li>{@link #LEVEL_DEFAULT_LTR}
 * <li>{@link #LEVEL_DEFAULT_RTL}
 * <li>{@link #LEVEL_OVERRIDE}
 * <li>{@link #MAX_EXPLICIT_LEVEL}
 * <li>{@link #setPara}
 * </ul>
 *
 * <h3>Basic concept: Reordering Mode</h3>
 * Reordering mode values indicate which variant of the Bidi algorithm to
 * use.
 *
 * <ul><b>See Also:</b>
 * <li>{@link #setReorderingMode}
 * <li>{@link #REORDER_DEFAULT}
 * <li>{@link #REORDER_NUMBERS_SPECIAL}
 * <li>{@link #REORDER_GROUP_NUMBERS_WITH_R}
 * <li>{@link #REORDER_RUNS_ONLY}
 * <li>{@link #REORDER_INVERSE_NUMBERS_AS_L}
 * <li>{@link #REORDER_INVERSE_LIKE_DIRECT}
 * <li>{@link #REORDER_INVERSE_FOR_NUMBERS_SPECIAL}
 * </ul>
 *
 * <h3>Basic concept: Reordering Options</h3>
 * Reordering options can be applied during Bidi text transformations.
 * <ul><b>See Also:</b>
 * <li>{@link #setReorderingOptions}
 * <li>{@link #OPTION_DEFAULT}
 * <li>{@link #OPTION_INSERT_MARKS}
 * <li>{@link #OPTION_REMOVE_CONTROLS}
 * <li>{@link #OPTION_STREAMING}
 * </ul>
 *
 *
 * @author Simon Montagu, Matitiahu Allouche (ported from C code written by Markus W. Scherer)
 * @stable ICU 3.8
 *
 *
 * <h4> Sample code for the ICU Bidi API </h4>
 *
 * <h5>Rendering a paragraph with the ICU Bidi API</h5>
 *
 * This is (hypothetical) sample code that illustrates how the ICU Bidi API
 * could be used to render a paragraph of text. Rendering code depends highly on
 * the graphics system, therefore this sample code must make a lot of
 * assumptions, which may or may not match any existing graphics system's
 * properties.
 *
 * <p>
 * The basic assumptions are:
 * </p>
 * <ul>
 * <li>Rendering is done from left to right on a horizontal line.</li>
 * <li>A run of single-style, unidirectional text can be rendered at once.
 * </li>
 * <li>Such a run of text is passed to the graphics system with characters
 * (code units) in logical order.</li>
 * <li>The line-breaking algorithm is very complicated and Locale-dependent -
 * and therefore its implementation omitted from this sample code.</li>
 * </ul>
 *
 * <pre>
 *
 *  package com.ibm.icu.dev.test.bidi;
 *
 *  import com.ibm.icu.text.Bidi;
 *  import com.ibm.icu.text.BidiRun;
 *
 *  public class Sample {
 *
 *      static final int styleNormal = 0;
 *      static final int styleSelected = 1;
 *      static final int styleBold = 2;
 *      static final int styleItalics = 4;
 *      static final int styleSuper=8;
 *      static final int styleSub = 16;
 *
 *      static class StyleRun {
 *          int limit;
 *          int style;
 *
 *          public StyleRun(int limit, int style) {
 *              this.limit = limit;
 *              this.style = style;
 *          }
 *      }
 *
 *      static class Bounds {
 *          int start;
 *          int limit;
 *
 *          public Bounds(int start, int limit) {
 *              this.start = start;
 *              this.limit = limit;
 *          }
 *      }
 *
 *      static int getTextWidth(String text, int start, int limit,
 *                              StyleRun[] styleRuns, int styleRunCount) {
 *          // simplistic way to compute the width
 *          return limit - start;
 *      }
 *
 *      // set limit and StyleRun limit for a line
 *      // from text[start] and from styleRuns[styleRunStart]
 *      // using Bidi.getLogicalRun(...)
 *      // returns line width
 *      static int getLineBreak(String text, Bounds line, Bidi para,
 *                              StyleRun styleRuns[], Bounds styleRun) {
 *          // dummy return
 *          return 0;
 *      }
 *
 *      // render runs on a line sequentially, always from left to right
 *
 *      // prepare rendering a new line
 *      static void startLine(byte textDirection, int lineWidth) {
 *          System.out.println();
 *      }
 *
 *      // render a run of text and advance to the right by the run width
 *      // the text[start..limit-1] is always in logical order
 *      static void renderRun(String text, int start, int limit,
 *                            byte textDirection, int style) {
 *      }
 *
 *      // We could compute a cross-product
 *      // from the style runs with the directional runs
 *      // and then reorder it.
 *      // Instead, here we iterate over each run type
 *      // and render the intersections -
 *      // with shortcuts in simple (and common) cases.
 *      // renderParagraph() is the main function.
 *
 *      // render a directional run with
 *      // (possibly) multiple style runs intersecting with it
 *      static void renderDirectionalRun(String text, int start, int limit,
 *                                       byte direction, StyleRun styleRuns[],
 *                                       int styleRunCount) {
 *          int i;
 *
 *          // iterate over style runs
 *          if (direction == Bidi.LTR) {
 *              int styleLimit;
 *              for (i = 0; i < styleRunCount; ++i) {
 *                  styleLimit = styleRuns[i].limit;
 *                  if (start < styleLimit) {
 *                      if (styleLimit > limit) {
 *                          styleLimit = limit;
 *                      }
 *                      renderRun(text, start, styleLimit,
 *                                direction, styleRuns[i].style);
 *                      if (styleLimit == limit) {
 *                          break;
 *                      }
 *                      start = styleLimit;
 *                  }
 *              }
 *          } else {
 *              int styleStart;
 *
 *              for (i = styleRunCount-1; i >= 0; --i) {
 *                  if (i > 0) {
 *                      styleStart = styleRuns[i-1].limit;
 *                  } else {
 *                      styleStart = 0;
 *                  }
 *                  if (limit >= styleStart) {
 *                      if (styleStart < start) {
 *                          styleStart = start;
 *                      }
 *                      renderRun(text, styleStart, limit, direction,
 *                                styleRuns[i].style);
 *                      if (styleStart == start) {
 *                          break;
 *                      }
 *                      limit = styleStart;
 *                  }
 *              }
 *          }
 *      }
 *
 *      // the line object represents text[start..limit-1]
 *      static void renderLine(Bidi line, String text, int start, int limit,
 *                             StyleRun styleRuns[], int styleRunCount) {
 *          byte direction = line.getDirection();
 *          if (direction != Bidi.MIXED) {
 *              // unidirectional
 *              if (styleRunCount <= 1) {
 *                  renderRun(text, start, limit, direction, styleRuns[0].style);
 *              } else {
 *                  renderDirectionalRun(text, start, limit, direction,
 *                                       styleRuns, styleRunCount);
 *              }
 *          } else {
 *              // mixed-directional
 *              int count, i;
 *              BidiRun run;
 *
 *              try {
 *                  count = line.countRuns();
 *              } catch (IllegalStateException e) {
 *                  e.printStackTrace();
 *                  return;
 *              }
 *              if (styleRunCount <= 1) {
 *                  int style = styleRuns[0].style;
 *
 *                  // iterate over directional runs
 *                  for (i = 0; i < count; ++i) {
 *                      run = line.getVisualRun(i);
 *                      renderRun(text, run.getStart(), run.getLimit(),
 *                                run.getDirection(), style);
 *                  }
 *              } else {
 *                  // iterate over both directional and style runs
 *                  for (i = 0; i < count; ++i) {
 *                      run = line.getVisualRun(i);
 *                      renderDirectionalRun(text, run.getStart(),
 *                                           run.getLimit(), run.getDirection(),
 *                                           styleRuns, styleRunCount);
 *                  }
 *              }
 *          }
 *      }
 *
 *      static void renderParagraph(String text, byte textDirection,
 *                                  StyleRun styleRuns[], int styleRunCount,
 *                                  int lineWidth) {
 *          int length = text.length();
 *          Bidi para = new Bidi();
 *          try {
 *              para.setPara(text,
 *                           textDirection != 0 ? Bidi.LEVEL_DEFAULT_RTL
 *                                              : Bidi.LEVEL_DEFAULT_LTR,
 *                           null);
 *          } catch (Exception e) {
 *              e.printStackTrace();
 *              return;
 *          }
 *          byte paraLevel = (byte)(1 & para.getParaLevel());
 *          StyleRun styleRun = new StyleRun(length, styleNormal);
 *
 *          if (styleRuns == null || styleRunCount <= 0) {
 *              styleRuns = new StyleRun[1];
 *              styleRunCount = 1;
 *              styleRuns[0] = styleRun;
 *          }
 *          // assume styleRuns[styleRunCount-1].limit>=length
 *
 *          int width = getTextWidth(text, 0, length, styleRuns, styleRunCount);
 *          if (width <= lineWidth) {
 *              // everything fits onto one line
 *
 *              // prepare rendering a new line from either left or right
 *              startLine(paraLevel, width);
 *
 *              renderLine(para, text, 0, length, styleRuns, styleRunCount);
 *          } else {
 *              // we need to render several lines
 *              Bidi line = new Bidi(length, 0);
 *              int start = 0, limit;
 *              int styleRunStart = 0, styleRunLimit;
 *
 *              for (;;) {
 *                  limit = length;
 *                  styleRunLimit = styleRunCount;
 *                  width = getLineBreak(text, new Bounds(start, limit),
 *                                       para, styleRuns,
 *                                       new Bounds(styleRunStart, styleRunLimit));
 *                  try {
 *                      line = para.setLine(start, limit);
 *                  } catch (Exception e) {
 *                      e.printStackTrace();
 *                      return;
 *                  }
 *                  // prepare rendering a new line
 *                  // from either left or right
 *                  startLine(paraLevel, width);
 *
 *                  if (styleRunStart > 0) {
 *                      int newRunCount = styleRuns.length - styleRunStart;
 *                      StyleRun[] newRuns = new StyleRun[newRunCount];
 *                      System.arraycopy(styleRuns, styleRunStart, newRuns, 0,
 *                                       newRunCount);
 *                      renderLine(line, text, start, limit, newRuns,
 *                                 styleRunLimit - styleRunStart);
 *                  } else {
 *                      renderLine(line, text, start, limit, styleRuns,
 *                                 styleRunLimit - styleRunStart);
 *                  }
 *                  if (limit == length) {
 *                      break;
 *                  }
 *                  start = limit;
 *                  styleRunStart = styleRunLimit - 1;
 *                  if (start >= styleRuns[styleRunStart].limit) {
 *                      ++styleRunStart;
 *                  }
 *              }
 *          }
 *      }
 *
 *      public static void main(String[] args)
 *      {
 *          renderParagraph("Some Latin text...", Bidi.LTR, null, 0, 80);
 *          renderParagraph("Some Hebrew text...", Bidi.RTL, null, 0, 60);
 *      }
 *  }
 *
 * </pre>
 */

public class Bidi {

    private java.text.Bidi bidi;

    private Bidi(java.text.Bidi delegate) {
        this.bidi = delegate;
    }

    /** Paragraph level setting<p>
     *
     * Constant indicating that the base direction depends on the first strong
     * directional character in the text according to the Unicode Bidirectional
     * Algorithm. If no strong directional character is present,
     * then set the paragraph level to 0 (left-to-right).<p>
     *
     * If this value is used in conjunction with reordering modes
     * <code>REORDER_INVERSE_LIKE_DIRECT</code> or
     * <code>REORDER_INVERSE_FOR_NUMBERS_SPECIAL</code>, the text to reorder
     * is assumed to be visual LTR, and the text after reordering is required
     * to be the corresponding logical string with appropriate contextual
     * direction. The direction of the result string will be RTL if either
     * the righmost or leftmost strong character of the source text is RTL
     * or Arabic Letter, the direction will be LTR otherwise.<p>
     *
     * If reordering option <code>OPTION_INSERT_MARKS</code> is set, an RLM may
     * be added at the beginning of the result string to ensure round trip
     * (that the result string, when reordered back to visual, will produce
     * the original source text).
     * @see #REORDER_INVERSE_LIKE_DIRECT
     * @see #REORDER_INVERSE_FOR_NUMBERS_SPECIAL
     * @stable ICU 3.8
     */
    public static final byte LEVEL_DEFAULT_LTR = (byte)0x7e;

    /** Paragraph level setting<p>
     *
     * Constant indicating that the base direction depends on the first strong
     * directional character in the text according to the Unicode Bidirectional
     * Algorithm. If no strong directional character is present,
     * then set the paragraph level to 1 (right-to-left).<p>
     *
     * If this value is used in conjunction with reordering modes
     * <code>REORDER_INVERSE_LIKE_DIRECT</code> or
     * <code>REORDER_INVERSE_FOR_NUMBERS_SPECIAL</code>, the text to reorder
     * is assumed to be visual LTR, and the text after reordering is required
     * to be the corresponding logical string with appropriate contextual
     * direction. The direction of the result string will be RTL if either
     * the righmost or leftmost strong character of the source text is RTL
     * or Arabic Letter, or if the text contains no strong character;
     * the direction will be LTR otherwise.<p>
     *
     * If reordering option <code>OPTION_INSERT_MARKS</code> is set, an RLM may
     * be added at the beginning of the result string to ensure round trip
     * (that the result string, when reordered back to visual, will produce
     * the original source text).
     * @see #REORDER_INVERSE_LIKE_DIRECT
     * @see #REORDER_INVERSE_FOR_NUMBERS_SPECIAL
     * @stable ICU 3.8
     */
    public static final byte LEVEL_DEFAULT_RTL = (byte)0x7f;

    /**
     * Maximum explicit embedding level.
     * (The maximum resolved level can be up to <code>MAX_EXPLICIT_LEVEL+1</code>).
     * @stable ICU 3.8
     */
    public static final byte MAX_EXPLICIT_LEVEL = 61;

    /**
     * Bit flag for level input.
     * Overrides directional properties.
     * @stable ICU 3.8
     */
    public static final byte LEVEL_OVERRIDE = (byte)0x80;

    /**
     * Special value which can be returned by the mapping methods when a
     * logical index has no corresponding visual index or vice-versa. This may
     * happen for the logical-to-visual mapping of a Bidi control when option
     * <code>OPTION_REMOVE_CONTROLS</code> is
     * specified. This can also happen for the visual-to-logical mapping of a
     * Bidi mark (LRM or RLM) inserted by option
     * <code>OPTION_INSERT_MARKS</code>.
     * @see #getVisualIndex
     * @see #getVisualMap
     * @see #getLogicalIndex
     * @see #getLogicalMap
     * @see #OPTION_INSERT_MARKS
     * @see #OPTION_REMOVE_CONTROLS
     * @stable ICU 3.8
     */
    public static final int MAP_NOWHERE = -1;

    /**
     * All left-to-right text.
     * @stable ICU 3.8
     */
    public static final byte LTR = 0;

    /**
     * All right-to-left text.
     * @stable ICU 3.8
     */
    public static final byte RTL = 1;

    /**
     * Mixed-directional text.
     * @stable ICU 3.8
     */
    public static final byte MIXED = 2;

    /**
     * option bit for writeReordered():
     * keep combining characters after their base characters in RTL runs
     *
     * @see #writeReordered
     * @stable ICU 3.8
     */
    public static final short KEEP_BASE_COMBINING = 1;

    /**
     * option bit for writeReordered():
     * replace characters with the "mirrored" property in RTL runs
     * by their mirror-image mappings
     *
     * @see #writeReordered
     * @stable ICU 3.8
     */
    public static final short DO_MIRRORING = 2;

    /**
     * option bit for writeReordered():
     * surround the run with LRMs if necessary;
     * this is part of the approximate "inverse Bidi" algorithm
     *
     * <p>This option does not imply corresponding adjustment of the index
     * mappings.</p>
     *
     * @see #setInverse
     * @see #writeReordered
     * @stable ICU 3.8
     */
    public static final short INSERT_LRM_FOR_NUMERIC = 4;

    /**
     * option bit for writeReordered():
     * remove Bidi control characters
     * (this does not affect INSERT_LRM_FOR_NUMERIC)
     *
     * <p>This option does not imply corresponding adjustment of the index
     * mappings.</p>
     *
     * @see #writeReordered
     * @see #INSERT_LRM_FOR_NUMERIC
     * @stable ICU 3.8
     */
    public static final short REMOVE_BIDI_CONTROLS = 8;

    /**
     * option bit for writeReordered():
     * write the output in reverse order
     *
     * <p>This has the same effect as calling <code>writeReordered()</code>
     * first without this option, and then calling
     * <code>writeReverse()</code> without mirroring.
     * Doing this in the same step is faster and avoids a temporary buffer.
     * An example for using this option is output to a character terminal that
     * is designed for RTL scripts and stores text in reverse order.</p>
     *
     * @see #writeReordered
     * @stable ICU 3.8
     */
    public static final short OUTPUT_REVERSE = 16;

    /** Reordering mode: Regular Logical to Visual Bidi algorithm according to Unicode.
     * @see #setReorderingMode
     * @stable ICU 3.8
     */
    public static final short REORDER_DEFAULT = 0;

    /** Reordering mode: Logical to Visual algorithm which handles numbers in
     * a way which mimicks the behavior of Windows XP.
     * @see #setReorderingMode
     * @stable ICU 3.8
     */
    public static final short REORDER_NUMBERS_SPECIAL = 1;

    /** Reordering mode: Logical to Visual algorithm grouping numbers with
     * adjacent R characters (reversible algorithm).
     * @see #setReorderingMode
     * @stable ICU 3.8
     */
    public static final short REORDER_GROUP_NUMBERS_WITH_R = 2;

    /** Reordering mode: Reorder runs only to transform a Logical LTR string
     * to the logical RTL string with the same display, or vice-versa.<br>
     * If this mode is set together with option
     * <code>OPTION_INSERT_MARKS</code>, some Bidi controls in the source
     * text may be removed and other controls may be added to produce the
     * minimum combination which has the required display.
     * @see #OPTION_INSERT_MARKS
     * @see #setReorderingMode
     * @stable ICU 3.8
     */
    public static final short REORDER_RUNS_ONLY = 3;

    /** Reordering mode: Visual to Logical algorithm which handles numbers
     * like L (same algorithm as selected by <code>setInverse(true)</code>.
     * @see #setInverse
     * @see #setReorderingMode
     * @stable ICU 3.8
     */
    public static final short REORDER_INVERSE_NUMBERS_AS_L = 4;

    /** Reordering mode: Visual to Logical algorithm equivalent to the regular
     * Logical to Visual algorithm.
     * @see #setReorderingMode
     * @stable ICU 3.8
     */
    public static final short REORDER_INVERSE_LIKE_DIRECT = 5;

    /** Reordering mode: Inverse Bidi (Visual to Logical) algorithm for the
     * <code>REORDER_NUMBERS_SPECIAL</code> Bidi algorithm.
     * @see #setReorderingMode
     * @stable ICU 3.8
     */
    public static final short REORDER_INVERSE_FOR_NUMBERS_SPECIAL = 6;

    /**
     * Option value for <code>setReorderingOptions</code>:
     * disable all the options which can be set with this method
     * @see #setReorderingOptions
     * @stable ICU 3.8
     */
    public static final int OPTION_DEFAULT = 0;

    /**
     * Option bit for <code>setReorderingOptions</code>:
     * insert Bidi marks (LRM or RLM) when needed to ensure correct result of
     * a reordering to a Logical order
     *
     * <p>This option must be set or reset before calling
     * <code>setPara</code>.</p>
     *
     * <p>This option is significant only with reordering modes which generate
     * a result with Logical order, specifically.</p>
     * <ul>
     *   <li><code>REORDER_RUNS_ONLY</code></li>
     *   <li><code>REORDER_INVERSE_NUMBERS_AS_L</code></li>
     *   <li><code>REORDER_INVERSE_LIKE_DIRECT</code></li>
     *   <li><code>REORDER_INVERSE_FOR_NUMBERS_SPECIAL</code></li>
     * </ul>
     *
     * <p>If this option is set in conjunction with reordering mode
     * <code>REORDER_INVERSE_NUMBERS_AS_L</code> or with calling
     * <code>setInverse(true)</code>, it implies option
     * <code>INSERT_LRM_FOR_NUMERIC</code> in calls to method
     * <code>writeReordered()</code>.</p>
     *
     * <p>For other reordering modes, a minimum number of LRM or RLM characters
     * will be added to the source text after reordering it so as to ensure
     * round trip, i.e. when applying the inverse reordering mode on the
     * resulting logical text with removal of Bidi marks
     * (option <code>OPTION_REMOVE_CONTROLS</code> set before calling
     * <code>setPara()</code> or option
     * <code>REMOVE_BIDI_CONTROLS</code> in
     * <code>writeReordered</code>), the result will be identical to the
     * source text in the first transformation.
     *
     * <p>This option will be ignored if specified together with option
     * <code>OPTION_REMOVE_CONTROLS</code>. It inhibits option
     * <code>REMOVE_BIDI_CONTROLS</code> in calls to method
     * <code>writeReordered()</code> and it implies option
     * <code>INSERT_LRM_FOR_NUMERIC</code> in calls to method
     * <code>writeReordered()</code> if the reordering mode is
     * <code>REORDER_INVERSE_NUMBERS_AS_L</code>.</p>
     *
     * @see #setReorderingMode
     * @see #setReorderingOptions
     * @see #INSERT_LRM_FOR_NUMERIC
     * @see #REMOVE_BIDI_CONTROLS
     * @see #OPTION_REMOVE_CONTROLS
     * @see #REORDER_RUNS_ONLY
     * @see #REORDER_INVERSE_NUMBERS_AS_L
     * @see #REORDER_INVERSE_LIKE_DIRECT
     * @see #REORDER_INVERSE_FOR_NUMBERS_SPECIAL
     * @stable ICU 3.8
     */
    public static final int OPTION_INSERT_MARKS = 1;

    /**
     * Option bit for <code>setReorderingOptions</code>:
     * remove Bidi control characters
     *
     * <p>This option must be set or reset before calling
     * <code>setPara</code>.</p>
     *
     * <p>This option nullifies option
     * <code>OPTION_INSERT_MARKS</code>. It inhibits option
     * <code>INSERT_LRM_FOR_NUMERIC</code> in calls to method
     * <code>writeReordered()</code> and it implies option
     * <code>REMOVE_BIDI_CONTROLS</code> in calls to that method.</p>
     *
     * @see #setReorderingMode
     * @see #setReorderingOptions
     * @see #OPTION_INSERT_MARKS
     * @see #INSERT_LRM_FOR_NUMERIC
     * @see #REMOVE_BIDI_CONTROLS
     * @stable ICU 3.8
     */
    public static final int OPTION_REMOVE_CONTROLS = 2;

    /**
     * Option bit for <code>setReorderingOptions</code>:
     * process the output as part of a stream to be continued
     *
     * <p>This option must be set or reset before calling
     * <code>setPara</code>.</p>
     *
     * <p>This option specifies that the caller is interested in processing
     * large text object in parts. The results of the successive calls are
     * expected to be concatenated by the caller. Only the call for the last
     * part will have this option bit off.</p>
     *
     * <p>When this option bit is on, <code>setPara()</code> may process
     * less than the full source text in order to truncate the text at a
     * meaningful boundary. The caller should call
     * <code>getProcessedLength()</code> immediately after calling
     * <code>setPara()</code> in order to determine how much of the source
     * text has been processed. Source text beyond that length should be
     * resubmitted in following calls to <code>setPara</code>. The
     * processed length may be less than the length of the source text if a
     * character preceding the last character of the source text constitutes a
     * reasonable boundary (like a block separator) for text to be continued.<br>
     * If the last character of the source text constitutes a reasonable
     * boundary, the whole text will be processed at once.<br>
     * If nowhere in the source text there exists
     * such a reasonable boundary, the processed length will be zero.<br>
     * The caller should check for such an occurrence and do one of the following:
     * <ul><li>submit a larger amount of text with a better chance to include
     *         a reasonable boundary.</li>
     *     <li>resubmit the same text after turning off option
     *         <code>OPTION_STREAMING</code>.</li></ul>
     * In all cases, this option should be turned off before processing the last
     * part of the text.</p>
     *
     * <p>When the <code>OPTION_STREAMING</code> option is used, it is
     * recommended to call <code>orderParagraphsLTR(true)</code> before calling
     * <code>setPara()</code> so that later paragraphs may be concatenated to
     * previous paragraphs on the right.
     * </p>
     *
     * @see #setReorderingMode
     * @see #setReorderingOptions
     * @see #getProcessedLength
     * @stable ICU 3.8
     */
    public static final int OPTION_STREAMING = 4;

    /**
     * Value returned by <code>BidiClassifier</code> when there is no need to
     * override the standard Bidi class for a given code point.
     * @see BidiClassifier
     * @stable ICU 3.8
     */
    public static final int CLASS_DEFAULT = 19; //UCharacterDirection.CHAR_DIRECTION_COUNT;

//    /**
//     * Allocate a <code>Bidi</code> object.
//     * Such an object is initially empty. It is assigned
//     * the Bidi properties of a piece of text containing one or more paragraphs
//     * by <code>setPara()</code>
//     * or the Bidi properties of a line within a paragraph by
//     * <code>setLine()</code>.<p>
//     * This object can be reused.<p>
//     * <code>setPara()</code> and <code>setLine()</code> will allocate
//     * additional memory for internal structures as necessary.
//     *
//     * @stable ICU 3.8
//     */
//    public Bidi()
//    {
//        throw new UnsupportedOperationException("Constructor not supported by com.ibm.icu.base");
//    }

//    /**
//     * Allocate a <code>Bidi</code> object with preallocated memory
//     * for internal structures.
//     * This method provides a <code>Bidi</code> object like the default constructor
//     * but it also preallocates memory for internal structures
//     * according to the sizings supplied by the caller.<p>
//     * The preallocation can be limited to some of the internal memory
//     * by setting some values to 0 here. That means that if, e.g.,
//     * <code>maxRunCount</code> cannot be reasonably predetermined and should not
//     * be set to <code>maxLength</code> (the only failproof value) to avoid
//     * wasting  memory, then <code>maxRunCount</code> could be set to 0 here
//     * and the internal structures that are associated with it will be allocated
//     * on demand, just like with the default constructor.
//     *
//     * @param maxLength is the maximum text or line length that internal memory
//     *        will be preallocated for. An attempt to associate this object with a
//     *        longer text will fail, unless this value is 0, which leaves the allocation
//     *        up to the implementation.
//     *
//     * @param maxRunCount is the maximum anticipated number of same-level runs
//     *        that internal memory will be preallocated for. An attempt to access
//     *        visual runs on an object that was not preallocated for as many runs
//     *        as the text was actually resolved to will fail,
//     *        unless this value is 0, which leaves the allocation up to the implementation.<br><br>
//     *        The number of runs depends on the actual text and maybe anywhere between
//     *        1 and <code>maxLength</code>. It is typically small.
//     *
//     * @throws IllegalArgumentException if maxLength or maxRunCount is less than 0
//     * @stable ICU 3.8
//     */
//    public Bidi(int maxLength, int maxRunCount)
//    {
//        throw new UnsupportedOperationException("Constructor not supported by com.ibm.icu.base");
//    }

//    /**
//     * Modify the operation of the Bidi algorithm such that it
//     * approximates an "inverse Bidi" algorithm. This method
//     * must be called before <code>setPara()</code>.
//     *
//     * <p>The normal operation of the Bidi algorithm as described
//     * in the Unicode Technical Report is to take text stored in logical
//     * (keyboard, typing) order and to determine the reordering of it for visual
//     * rendering.
//     * Some legacy systems store text in visual order, and for operations
//     * with standard, Unicode-based algorithms, the text needs to be transformed
//     * to logical order. This is effectively the inverse algorithm of the
//     * described Bidi algorithm. Note that there is no standard algorithm for
//     * this "inverse Bidi" and that the current implementation provides only an
//     * approximation of "inverse Bidi".</p>
//     *
//     * <p>With <code>isInversed</code> set to <code>true</code>,
//     * this method changes the behavior of some of the subsequent methods
//     * in a way that they can be used for the inverse Bidi algorithm.
//     * Specifically, runs of text with numeric characters will be treated in a
//     * special way and may need to be surrounded with LRM characters when they are
//     * written in reordered sequence.</p>
//     *
//     * <p>Output runs should be retrieved using <code>getVisualRun()</code>.
//     * Since the actual input for "inverse Bidi" is visually ordered text and
//     * <code>getVisualRun()</code> gets the reordered runs, these are actually
//     * the runs of the logically ordered output.</p>
//     *
//     * <p>Calling this method with argument <code>isInverse</code> set to
//     * <code>true</code> is equivalent to calling <code>setReorderingMode</code>
//     * with argument <code>reorderingMode</code>
//     * set to <code>REORDER_INVERSE_NUMBERS_AS_L</code>.<br>
//     * Calling this method with argument <code>isInverse</code> set to
//     * <code>false</code> is equivalent to calling <code>setReorderingMode</code>
//     * with argument <code>reorderingMode</code>
//     * set to <code>REORDER_DEFAULT</code>.
//     *
//     * @param isInverse specifies "forward" or "inverse" Bidi operation.
//     *
//     * @see #setPara
//     * @see #writeReordered
//     * @see #setReorderingMode
//     * @see #REORDER_INVERSE_NUMBERS_AS_L
//     * @see #REORDER_DEFAULT
//     * @stable ICU 3.8
//     */
//    public void setInverse(boolean isInverse) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * Is this <code>Bidi</code> object set to perform the inverse Bidi
//     * algorithm?
//     * <p>Note: calling this method after setting the reordering mode with
//     * <code>setReorderingMode</code> will return <code>true</code> if the
//     * reordering mode was set to
//     * <code>REORDER_INVERSE_NUMBERS_AS_L<code>, <code>false</code>
//     * for all other values.</p>
//     *
//     * @return <code>true</code> if the <code>Bidi</code> object is set to
//     * perform the inverse Bidi algorithm by handling numbers as L.
//     *
//     * @see #setInverse
//     * @see #setReorderingMode
//     * @see #REORDER_INVERSE_NUMBERS_AS_L
//     * @stable ICU 3.8
//     */
//    public boolean isInverse() {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * Modify the operation of the Bidi algorithm such that it implements some
//     * variant to the basic Bidi algorithm or approximates an "inverse Bidi"
//     * algorithm, depending on different values of the "reordering mode".
//     * This method must be called before <code>setPara()</code>, and stays in
//     * effect until called again with a different argument.
//     *
//     * <p>The normal operation of the Bidi algorithm as described in the Unicode
//     * Standard Annex #9 is to take text stored in logical (keyboard, typing)
//     * order and to determine how to reorder it for visual rendering.</p>
//     *
//     * <p>With the reordering mode set to a value other than
//     * <code>REORDER_DEFAULT</code>, this method changes the behavior of some of
//     * the subsequent methods in a way such that they implement an inverse Bidi
//     * algorithm or some other algorithm variants.</p>
//     *
//     * <p>Some legacy systems store text in visual order, and for operations
//     * with standard, Unicode-based algorithms, the text needs to be transformed
//     * into logical order. This is effectively the inverse algorithm of the
//     * described Bidi algorithm. Note that there is no standard algorithm for
//     * this "inverse Bidi", so a number of variants are implemented here.</p>
//     *
//     * <p>In other cases, it may be desirable to emulate some variant of the
//     * Logical to Visual algorithm (e.g. one used in MS Windows), or perform a
//     * Logical to Logical transformation.</p>
//     *
//     * <ul>
//     * <li>When the Reordering Mode is set to
//     * <code>REORDER_DEFAULT</code>,
//     * the standard Bidi Logical to Visual algorithm is applied.</li>
//     *
//     * <li>When the reordering mode is set to
//     * <code>REORDER_NUMBERS_SPECIAL</code>,
//     * the algorithm used to perform Bidi transformations when calling
//     * <code>setPara</code> should approximate the algorithm used in Microsoft
//     * Windows XP rather than strictly conform to the Unicode Bidi algorithm.
//     * <br>
//     * The differences between the basic algorithm and the algorithm addressed
//     * by this option are as follows:
//     * <ul>
//     *   <li>Within text at an even embedding level, the sequence "123AB"
//     *   (where AB represent R or AL letters) is transformed to "123BA" by the
//     *   Unicode algorithm and to "BA123" by the Windows algorithm.</li>
//     *
//     *   <li>Arabic-Indic numbers (AN) are handled by the Windows algorithm just
//     *   like regular numbers (EN).</li>
//     * </ul></li>
//     *
//     * <li>When the reordering mode is set to
//     * <code>REORDER_GROUP_NUMBERS_WITH_R</code>,
//     * numbers located between LTR text and RTL text are associated with the RTL
//     * text. For instance, an LTR paragraph with content "abc 123 DEF" (where
//     * upper case letters represent RTL characters) will be transformed to
//     * "abc FED 123" (and not "abc 123 FED"), "DEF 123 abc" will be transformed
//     * to "123 FED abc" and "123 FED abc" will be transformed to "DEF 123 abc".
//     * This makes the algorithm reversible and makes it useful when round trip
//     * (from visual to logical and back to visual) must be achieved without
//     * adding LRM characters. However, this is a variation from the standard
//     * Unicode Bidi algorithm.<br>
//     * The source text should not contain Bidi control characters other than LRM
//     * or RLM.</li>
//     *
//     * <li>When the reordering mode is set to
//     * <code>REORDER_RUNS_ONLY</code>,
//     * a "Logical to Logical" transformation must be performed:
//     * <ul>
//     * <li>If the default text level of the source text (argument
//     * <code>paraLevel</code> in <code>setPara</code>) is even, the source text
//     * will be handled as LTR logical text and will be transformed to the RTL
//     * logical text which has the same LTR visual display.</li>
//     * <li>If the default level of the source text is odd, the source text
//     * will be handled as RTL logical text and will be transformed to the
//     * LTR logical text which has the same LTR visual display.</li>
//     * </ul>
//     * This mode may be needed when logical text which is basically Arabic or
//     * Hebrew, with possible included numbers or phrases in English, has to be
//     * displayed as if it had an even embedding level (this can happen if the
//     * displaying application treats all text as if it was basically LTR).
//     * <br>
//     * This mode may also be needed in the reverse case, when logical text which
//     * is basically English, with possible included phrases in Arabic or Hebrew,
//     * has to be displayed as if it had an odd embedding level.
//     * <br>
//     * Both cases could be handled by adding LRE or RLE at the head of the
//     * text, if the display subsystem supports these formatting controls. If it
//     * does not, the problem may be handled by transforming the source text in
//     * this mode before displaying it, so that it will be displayed properly.
//     * <br>
//     * The source text should not contain Bidi control characters other than LRM
//     * or RLM.</li>
//     *
//     * <li>When the reordering mode is set to
//     * <code>REORDER_INVERSE_NUMBERS_AS_L</code>, an "inverse Bidi"
//     * algorithm is applied.
//     * Runs of text with numeric characters will be treated like LTR letters and
//     * may need to be surrounded with LRM characters when they are written in
//     * reordered sequence (the option <code>INSERT_LRM_FOR_NUMERIC</code> can
//     * be used with method <code>writeReordered</code> to this end. This mode
//     * is equivalent to calling <code>setInverse()</code> with
//     * argument <code>isInverse</code> set to <code>true</code>.</li>
//     *
//     * <li>When the reordering mode is set to
//     * <code>REORDER_INVERSE_LIKE_DIRECT</code>, the "direct" Logical to
//     * Visual Bidi algorithm is used as an approximation of an "inverse Bidi"
//     * algorithm. This mode is similar to mode
//     * <code>REORDER_INVERSE_NUMBERS_AS_L</code> but is closer to the
//     * regular Bidi algorithm.
//     * <br>
//     * For example, an LTR paragraph with the content "FED 123 456 CBA" (where
//     * upper case represents RTL characters) will be transformed to
//     * "ABC 456 123 DEF", as opposed to "DEF 123 456 ABC"
//     * with mode <code>REORDER_INVERSE_NUMBERS_AS_L</code>.<br>
//     * When used in conjunction with option
//     * <code>OPTION_INSERT_MARKS</code>, this mode generally
//     * adds Bidi marks to the output significantly more sparingly than mode
//     * <code>REORDER_INVERSE_NUMBERS_AS_L</code>.<br> with option
//     * <code>INSERT_LRM_FOR_NUMERIC</code> in calls to
//     * <code>writeReordered</code>.</li>
//     *
//     * <li>When the reordering mode is set to
//     * <code>REORDER_INVERSE_FOR_NUMBERS_SPECIAL</code>, the Logical to Visual
//     * Bidi algorithm used in Windows XP is used as an approximation of an "inverse
//     * Bidi" algorithm.
//     * <br>
//     * For example, an LTR paragraph with the content "abc FED123" (where
//     * upper case represents RTL characters) will be transformed to
//     * "abc 123DEF.</li>
//     * </ul>
//     *
//     * <p>In all the reordering modes specifying an "inverse Bidi" algorithm
//     * (i.e. those with a name starting with <code>REORDER_INVERSE</code>),
//     * output runs should be retrieved using <code>getVisualRun()</code>, and
//     * the output text with <code>writeReordered()</code>. The caller should
//     * keep in mind that in "inverse Bidi" modes the input is actually visually
//     * ordered text and reordered output returned by <code>getVisualRun()</code>
//     * or <code>writeReordered()</code> are actually runs or character string
//     * of logically ordered output.<br>
//     * For all the "inverse Bidi" modes, the source text should not contain
//     * Bidi control characters other than LRM or RLM.</p>
//     *
//     * <p>Note that option <code>OUTPUT_REVERSE</code> of
//     * <code>writeReordered</code> has no useful meaning and should not be used
//     * in conjunction with any value of the reordering mode specifying "inverse
//     * Bidi" or with value <code>REORDER_RUNS_ONLY</code>.
//     *
//     * @param reorderingMode specifies the required variant of the Bidi
//     *                       algorithm.
//     *
//     * @see #setInverse
//     * @see #setPara
//     * @see #writeReordered
//     * @see #INSERT_LRM_FOR_NUMERIC
//     * @see #OUTPUT_REVERSE
//     * @see #REORDER_DEFAULT
//     * @see #REORDER_NUMBERS_SPECIAL
//     * @see #REORDER_GROUP_NUMBERS_WITH_R
//     * @see #REORDER_RUNS_ONLY
//     * @see #REORDER_INVERSE_NUMBERS_AS_L
//     * @see #REORDER_INVERSE_LIKE_DIRECT
//     * @see #REORDER_INVERSE_FOR_NUMBERS_SPECIAL
//     * @stable ICU 3.8
//     */
//    public void setReorderingMode(int reorderingMode) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * What is the requested reordering mode for a given Bidi object?
//     *
//     * @return the current reordering mode of the Bidi object
//     *
//     * @see #setReorderingMode
//     * @stable ICU 3.8
//     */
//    public int getReorderingMode() {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * Specify which of the reordering options should be applied during Bidi
//     * transformations.
//     *
//     * @param options A combination of zero or more of the following
//     * reordering options:
//     * <code>OPTION_DEFAULT</code>, <code>OPTION_INSERT_MARKS</code>,
//     * <code>OPTION_REMOVE_CONTROLS</code>, <code>OPTION_STREAMING</code>.
//     *
//     * @see #getReorderingOptions
//     * @see #OPTION_DEFAULT
//     * @see #OPTION_INSERT_MARKS
//     * @see #OPTION_REMOVE_CONTROLS
//     * @see #OPTION_STREAMING
//     * @stable ICU 3.8
//     */
//    public void setReorderingOptions(int options) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * What are the reordering options applied to a given Bidi object?
//     *
//     * @return the current reordering options of the Bidi object
//     *
//     * @see #setReorderingOptions
//     * @stable ICU 3.8
//     */
//    public int getReorderingOptions() {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * Perform the Unicode Bidi algorithm. It is defined in the
//     * <a href="http://www.unicode.org/unicode/reports/tr9/">Unicode Standard Annex #9</a>,
//     * version 13,
//     * also described in The Unicode Standard, Version 4.0 .<p>
//     *
//     * This method takes a piece of plain text containing one or more paragraphs,
//     * with or without externally specified embedding levels from <i>styled</i>
//     * text and computes the left-right-directionality of each character.<p>
//     *
//     * If the entire text is all of the same directionality, then
//     * the method may not perform all the steps described by the algorithm,
//     * i.e., some levels may not be the same as if all steps were performed.
//     * This is not relevant for unidirectional text.<br>
//     * For example, in pure LTR text with numbers the numbers would get
//     * a resolved level of 2 higher than the surrounding text according to
//     * the algorithm. This implementation may set all resolved levels to
//     * the same value in such a case.<p>
//     *
//     * The text can be composed of multiple paragraphs. Occurrence of a block
//     * separator in the text terminates a paragraph, and whatever comes next starts
//     * a new paragraph. The exception to this rule is when a Carriage Return (CR)
//     * is followed by a Line Feed (LF). Both CR and LF are block separators, but
//     * in that case, the pair of characters is considered as terminating the
//     * preceding paragraph, and a new paragraph will be started by a character
//     * coming after the LF.
//     *
//     * Although the text is passed here as a <code>String</code>, it is
//     * stored internally as an array of characters. Therefore the
//     * documentation will refer to indexes of the characters in the text.
//     *
//     * @param text contains the text that the Bidi algorithm will be performed
//     *        on. This text can be retrieved with <code>getText()</code> or
//     *        <code>getTextAsString</code>.<br>
//     *
//     * @param paraLevel specifies the default level for the text;
//     *        it is typically 0 (LTR) or 1 (RTL).
//     *        If the method shall determine the paragraph level from the text,
//     *        then <code>paraLevel</code> can be set to
//     *        either <code>LEVEL_DEFAULT_LTR</code>
//     *        or <code>LEVEL_DEFAULT_RTL</code>; if the text contains multiple
//     *        paragraphs, the paragraph level shall be determined separately for
//     *        each paragraph; if a paragraph does not include any strongly typed
//     *        character, then the desired default is used (0 for LTR or 1 for RTL).
//     *        Any other value between 0 and <code>MAX_EXPLICIT_LEVEL</code>
//     *        is also valid, with odd levels indicating RTL.
//     *
//     * @param embeddingLevels (in) may be used to preset the embedding and override levels,
//     *        ignoring characters like LRE and PDF in the text.
//     *        A level overrides the directional property of its corresponding
//     *        (same index) character if the level has the
//     *        <code>LEVEL_OVERRIDE</code> bit set.<br><br>
//     *        Except for that bit, it must be
//     *        <code>paraLevel<=embeddingLevels[]<=MAX_EXPLICIT_LEVEL</code>,
//     *        with one exception: a level of zero may be specified for a
//     *        paragraph separator even if <code>paraLevel&gt;0</code> when multiple
//     *        paragraphs are submitted in the same call to <code>setPara()</code>.<br><br>
//     *        <strong>Caution: </strong>A reference to this array, not a copy
//     *        of the levels, will be stored in the <code>Bidi</code> object;
//     *        the <code>embeddingLevels</code>
//     *        should not be modified to avoid unexpected results on subsequent
//     *        Bidi operations. However, the <code>setPara()</code> and
//     *        <code>setLine()</code> methods may modify some or all of the
//     *        levels.<br><br>
//     *        <strong>Note:</strong> the <code>embeddingLevels</code> array must
//     *        have one entry for each character in <code>text</code>.
//     *
//     * @throws IllegalArgumentException if the values in embeddingLevels are
//     *         not within the allowed range
//     *
//     * @see #LEVEL_DEFAULT_LTR
//     * @see #LEVEL_DEFAULT_RTL
//     * @see #LEVEL_OVERRIDE
//     * @see #MAX_EXPLICIT_LEVEL
//     * @stable ICU 3.8
//     */
//    public void setPara(String text, byte paraLevel, byte[] embeddingLevels)
//    {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * Perform the Unicode Bidi algorithm. It is defined in the
//     * <a href="http://www.unicode.org/unicode/reports/tr9/">Unicode Standard Annex #9</a>,
//     * version 13,
//     * also described in The Unicode Standard, Version 4.0 .<p>
//     *
//     * This method takes a piece of plain text containing one or more paragraphs,
//     * with or without externally specified embedding levels from <i>styled</i>
//     * text and computes the left-right-directionality of each character.<p>
//     *
//     * If the entire text is all of the same directionality, then
//     * the method may not perform all the steps described by the algorithm,
//     * i.e., some levels may not be the same as if all steps were performed.
//     * This is not relevant for unidirectional text.<br>
//     * For example, in pure LTR text with numbers the numbers would get
//     * a resolved level of 2 higher than the surrounding text according to
//     * the algorithm. This implementation may set all resolved levels to
//     * the same value in such a case.<p>
//     *
//     * The text can be composed of multiple paragraphs. Occurrence of a block
//     * separator in the text terminates a paragraph, and whatever comes next starts
//     * a new paragraph. The exception to this rule is when a Carriage Return (CR)
//     * is followed by a Line Feed (LF). Both CR and LF are block separators, but
//     * in that case, the pair of characters is considered as terminating the
//     * preceding paragraph, and a new paragraph will be started by a character
//     * coming after the LF.
//     *
//     * The text is stored internally as an array of characters. Therefore the
//     * documentation will refer to indexes of the characters in the text.
//     *
//     * @param chars contains the text that the Bidi algorithm will be performed
//     *        on. This text can be retrieved with <code>getText()</code> or
//     *        <code>getTextAsString</code>.<br>
//     *
//     * @param paraLevel specifies the default level for the text;
//     *        it is typically 0 (LTR) or 1 (RTL).
//     *        If the method shall determine the paragraph level from the text,
//     *        then <code>paraLevel</code> can be set to
//     *        either <code>LEVEL_DEFAULT_LTR</code>
//     *        or <code>LEVEL_DEFAULT_RTL</code>; if the text contains multiple
//     *        paragraphs, the paragraph level shall be determined separately for
//     *        each paragraph; if a paragraph does not include any strongly typed
//     *        character, then the desired default is used (0 for LTR or 1 for RTL).
//     *        Any other value between 0 and <code>MAX_EXPLICIT_LEVEL</code>
//     *        is also valid, with odd levels indicating RTL.
//     *
//     * @param embeddingLevels (in) may be used to preset the embedding and
//     *        override levels, ignoring characters like LRE and PDF in the text.
//     *        A level overrides the directional property of its corresponding
//     *        (same index) character if the level has the
//     *        <code>LEVEL_OVERRIDE</code> bit set.<br><br>
//     *        Except for that bit, it must be
//     *        <code>paraLevel<=embeddingLevels[]<=MAX_EXPLICIT_LEVEL</code>,
//     *        with one exception: a level of zero may be specified for a
//     *        paragraph separator even if <code>paraLevel&gt;0</code> when multiple
//     *        paragraphs are submitted in the same call to <code>setPara()</code>.<br><br>
//     *        <strong>Caution: </strong>A reference to this array, not a copy
//     *        of the levels, will be stored in the <code>Bidi</code> object;
//     *        the <code>embeddingLevels</code>
//     *        should not be modified to avoid unexpected results on subsequent
//     *        Bidi operations. However, the <code>setPara()</code> and
//     *        <code>setLine()</code> methods may modify some or all of the
//     *        levels.<br><br>
//     *        <strong>Note:</strong> the <code>embeddingLevels</code> array must
//     *        have one entry for each character in <code>text</code>.
//     *
//     * @throws IllegalArgumentException if the values in embeddingLevels are
//     *         not within the allowed range
//     *
//     * @see #LEVEL_DEFAULT_LTR
//     * @see #LEVEL_DEFAULT_RTL
//     * @see #LEVEL_OVERRIDE
//     * @see #MAX_EXPLICIT_LEVEL
//     * @stable ICU 3.8
//     */
//    public void setPara(char[] chars, byte paraLevel, byte[] embeddingLevels)
//    {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * Perform the Unicode Bidi algorithm on a given paragraph, as defined in the
//     * <a href="http://www.unicode.org/unicode/reports/tr9/">Unicode Standard Annex #9</a>,
//     * version 13,
//     * also described in The Unicode Standard, Version 4.0 .<p>
//     *
//     * This method takes a paragraph of text and computes the
//     * left-right-directionality of each character. The text should not
//     * contain any Unicode block separators.<p>
//     *
//     * The RUN_DIRECTION attribute in the text, if present, determines the base
//     * direction (left-to-right or right-to-left). If not present, the base
//     * direction is computed using the Unicode Bidirectional Algorithm,
//     * defaulting to left-to-right if there are no strong directional characters
//     * in the text. This attribute, if present, must be applied to all the text
//     * in the paragraph.<p>
//     *
//     * The BIDI_EMBEDDING attribute in the text, if present, represents
//     * embedding level information. Negative values from -1 to -62 indicate
//     * overrides at the absolute value of the level. Positive values from 1 to
//     * 62 indicate embeddings. Where values are zero or not defined, the base
//     * embedding level as determined by the base direction is assumed.<p>
//     *
//     * The NUMERIC_SHAPING attribute in the text, if present, converts European
//     * digits to other decimal digits before running the bidi algorithm. This
//     * attribute, if present, must be applied to all the text in the paragraph.
//     *
//     * If the entire text is all of the same directionality, then
//     * the method may not perform all the steps described by the algorithm,
//     * i.e., some levels may not be the same as if all steps were performed.
//     * This is not relevant for unidirectional text.<br>
//     * For example, in pure LTR text with numbers the numbers would get
//     * a resolved level of 2 higher than the surrounding text according to
//     * the algorithm. This implementation may set all resolved levels to
//     * the same value in such a case.<p>
//     *
//     * @param paragraph a paragraph of text with optional character and
//     *        paragraph attribute information
//     * @stable ICU 3.8
//     */
//    public void setPara(AttributedCharacterIterator paragraph)
//    {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * Specify whether block separators must be allocated level zero,
//     * so that successive paragraphs will progress from left to right.
//     * This method must be called before <code>setPara()</code>.
//     * Paragraph separators (B) may appear in the text.  Setting them to level zero
//     * means that all paragraph separators (including one possibly appearing
//     * in the last text position) are kept in the reordered text after the text
//     * that they follow in the source text.
//     * When this feature is not enabled, a paragraph separator at the last
//     * position of the text before reordering will go to the first position
//     * of the reordered text when the paragraph level is odd.
//     *
//     * @param ordarParaLTR specifies whether paragraph separators (B) must
//     * receive level 0, so that successive paragraphs progress from left to right.
//     *
//     * @see #setPara
//     * @stable ICU 3.8
//     */
//    public void orderParagraphsLTR(boolean ordarParaLTR) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * Is this <code>Bidi</code> object set to allocate level 0 to block
//     * separators so that successive paragraphs progress from left to right?
//     *
//     * @return <code>true</code> if the <code>Bidi</code> object is set to
//     *         allocate level 0 to block separators.
//     *
//     * @stable ICU 3.8
//     */
//    public boolean isOrderParagraphsLTR() {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * Get the directionality of the text.
//     *
//     * @return a value of <code>LTR</code>, <code>RTL</code> or <code>MIXED</code>
//     *         that indicates if the entire text
//     *         represented by this object is unidirectional,
//     *         and which direction, or if it is mixed-directional.
//     *
//     * @throws IllegalStateException if this call is not preceded by a successful
//     *         call to <code>setPara</code> or <code>setLine</code>
//     *
//     * @see #LTR
//     * @see #RTL
//     * @see #MIXED
//     * @stable ICU 3.8
//     */
//    public byte getDirection()
//    {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * Get the text.
//     *
//     * @return A <code>String</code> containing the text that the
//     *         <code>Bidi</code> object was created for.
//     *
//     * @throws IllegalStateException if this call is not preceded by a successful
//     *         call to <code>setPara</code> or <code>setLine</code>
//     *
//     * @see #setPara
//     * @see #setLine
//     * @stable ICU 3.8
//     */
//    public String getTextAsString()
//    {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * Get the text.
//     *
//     * @return A <code>char</code> array containing the text that the
//     *         <code>Bidi</code> object was created for.
//     *
//     * @throws IllegalStateException if this call is not preceded by a successful
//     *         call to <code>setPara</code> or <code>setLine</code>
//     *
//     * @see #setPara
//     * @see #setLine
//     * @stable ICU 3.8
//     */
//    public char[] getText()
//    {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

    /**
     * Get the length of the text.
     *
     * @return The length of the text that the <code>Bidi</code> object was
     *         created for.
     *
     * @throws IllegalStateException if this call is not preceded by a successful
     *         call to <code>setPara</code> or <code>setLine</code>
     * @stable ICU 3.8
     */
    public int getLength()
    {
        return bidi.getLength();
    }

//    /**
//     * Get the length of the source text processed by the last call to
//     * <code>setPara()</code>. This length may be different from the length of
//     * the source text if option <code>OPTION_STREAMING</code> has been
//     * set.
//     * <br>
//     * Note that whenever the length of the text affects the execution or the
//     * result of a method, it is the processed length which must be considered,
//     * except for <code>setPara</code> (which receives unprocessed source text)
//     * and <code>getLength</code> (which returns the original length of the
//     * source text).<br>
//     * In particular, the processed length is the one to consider in the
//     * following cases:
//     * <ul>
//     * <li>maximum value of the <code>limit</code> argument of
//     * <code>setLine</code></li>
//     * <li>maximum value of the <code>charIndex</code> argument of
//     * <code>getParagraph</code></li>
//     * <li>maximum value of the <code>charIndex</code> argument of
//     * <code>getLevelAt</code></li>
//     * <li>number of elements in the array returned by <code>getLevels</code>
//     * </li>
//     * <li>maximum value of the <code>logicalStart</code> argument of
//     * <code>getLogicalRun</code></li>
//     * <li>maximum value of the <code>logicalIndex</code> argument of
//     * <code>getVisualIndex</code></li>
//     * <li>number of elements returned by <code>getLogicalMap</code></li>
//     * <li>length of text processed by <code>writeReordered</code></li>
//     * </ul>
//     *
//     * @return The length of the part of the source text processed by
//     *         the last call to <code>setPara</code>.
//     *
//     * @throws IllegalStateException if this call is not preceded by a successful
//     *         call to <code>setPara</code> or <code>setLine</code>
//     *
//     * @see #setPara
//     * @see #OPTION_STREAMING
//     * @stable ICU 3.8
//     */
//    public int getProcessedLength() {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * Get the length of the reordered text resulting from the last call to
//     * <code>setPara()</code>. This length may be different from the length
//     * of the source text if option <code>OPTION_INSERT_MARKS</code>
//     * or option <code>OPTION_REMOVE_CONTROLS</code> has been set.
//     * <br>
//     * This resulting length is the one to consider in the following cases:
//     * <ul>
//     * <li>maximum value of the <code>visualIndex</code> argument of
//     * <code>getLogicalIndex</code></li>
//     * <li>number of elements returned by <code>getVisualMap</code></li>
//     * </ul>
//     * Note that this length stays identical to the source text length if
//     * Bidi marks are inserted or removed using option bits of
//     * <code>writeReordered</code>, or if option
//     * <code>REORDER_INVERSE_NUMBERS_AS_L</code> has been set.
//     *
//     * @return The length of the reordered text resulting from
//     *         the last call to <code>setPara</code>.
//     *
//     * @throws IllegalStateException if this call is not preceded by a successful
//     *         call to <code>setPara</code> or <code>setLine</code>
//     *
//     * @see #setPara
//     * @see #OPTION_INSERT_MARKS
//     * @see #OPTION_REMOVE_CONTROLS
//     * @see #REORDER_INVERSE_NUMBERS_AS_L
//     * @stable ICU 3.8
//     */
//    public int getResultLength() {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

    /* paragraphs API methods ------------------------------------------------- */

//    /**
//     * Get the paragraph level of the text.
//     *
//     * @return The paragraph level. If there are multiple paragraphs, their
//     *         level may vary if the required paraLevel is LEVEL_DEFAULT_LTR or
//     *         LEVEL_DEFAULT_RTL.  In that case, the level of the first paragraph
//     *         is returned.
//     *
//     * @throws IllegalStateException if this call is not preceded by a successful
//     *         call to <code>setPara</code> or <code>setLine</code>
//     *
//     * @see #LEVEL_DEFAULT_LTR
//     * @see #LEVEL_DEFAULT_RTL
//     * @see #getParagraph
//     * @see #getParagraphByIndex
//     * @stable ICU 3.8
//     */
//    public byte getParaLevel()
//    {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * Get the number of paragraphs.
//     *
//     * @return The number of paragraphs.
//     *
//     * @throws IllegalStateException if this call is not preceded by a successful
//     *         call to <code>setPara</code> or <code>setLine</code>
//     * @stable ICU 3.8
//     */
//    public int countParagraphs()
//    {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * Get a paragraph, given the index of this paragraph.
//     *
//     * This method returns information about a paragraph.<p>
//     *
//     * @param paraIndex is the number of the paragraph, in the
//     *        range <code>[0..countParagraphs()-1]</code>.
//     *
//     * @return a BidiRun object with the details of the paragraph:<br>
//     *        <code>start</code> will receive the index of the first character
//     *        of the paragraph in the text.<br>
//     *        <code>limit</code> will receive the limit of the paragraph.<br>
//     *        <code>embeddingLevel</code> will receive the level of the paragraph.
//     *
//     * @throws IllegalStateException if this call is not preceded by a successful
//     *         call to <code>setPara</code> or <code>setLine</code>
//     * @throws IllegalArgumentException if paraIndex is not in the range
//     *        <code>[0..countParagraphs()-1]</code>
//     *
//     * @see com.ibm.icu.text.BidiRun
//     * @stable ICU 3.8
//     */
//    public BidiRun getParagraphByIndex(int paraIndex)
//    {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * Get a paragraph, given a position within the text.
//     * This method returns information about a paragraph.<br>
//     * Note: if the paragraph index is known, it is more efficient to
//     * retrieve the paragraph information using getParagraphByIndex().<p>
//     *
//     * @param charIndex is the index of a character within the text, in the
//     *        range <code>[0..getProcessedLength()-1]</code>.
//     *
//     * @return a BidiRun object with the details of the paragraph:<br>
//     *        <code>start</code> will receive the index of the first character
//     *        of the paragraph in the text.<br>
//     *        <code>limit</code> will receive the limit of the paragraph.<br>
//     *        <code>embeddingLevel</code> will receive the level of the paragraph.
//     *
//     * @throws IllegalStateException if this call is not preceded by a successful
//     *         call to <code>setPara</code> or <code>setLine</code>
//     * @throws IllegalArgumentException if charIndex is not within the legal range
//     *
//     * @see com.ibm.icu.text.BidiRun
//     * @see #getParagraphByIndex
//     * @see #getProcessedLength
//     * @stable ICU 3.8
//     */
//    public BidiRun getParagraph(int charIndex)
//    {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * Get the index of a paragraph, given a position within the text.<p>
//     *
//     * @param charIndex is the index of a character within the text, in the
//     *        range <code>[0..getProcessedLength()-1]</code>.
//     *
//     * @return The index of the paragraph containing the specified position,
//     *         starting from 0.
//     *
//     * @throws IllegalStateException if this call is not preceded by a successful
//     *         call to <code>setPara</code> or <code>setLine</code>
//     * @throws IllegalArgumentException if charIndex is not within the legal range
//     *
//     * @see com.ibm.icu.text.BidiRun
//     * @see #getProcessedLength
//     * @stable ICU 3.8
//     */
//    public int getParagraphIndex(int charIndex)
//    {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * Set a custom Bidi classifier used by the UBA implementation for Bidi
//     * class determination.
//     *
//     * @param classifier A new custom classifier. This can be null.
//     *
//     * @see #getCustomClassifier
//     * @stable ICU 3.8
//     */
//    public void setCustomClassifier(BidiClassifier classifier) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * Gets the current custom class classifier used for Bidi class
//     * determination.
//     *
//     * @return An instance of class <code>BidiClassifier</code>
//     *
//     * @see #setCustomClassifier
//     * @stable ICU 3.8
//     */
//    public BidiClassifier getCustomClassifier() {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * Retrieves the Bidi class for a given code point.
//     * <p>If a <code>BidiClassifier</code> is defined and returns a value
//     * other than <code>CLASS_DEFAULT</code>, that value is used; otherwise
//     * the default class determination mechanism is invoked.</p>
//     *
//     * @param c The code point to get a Bidi class for.
//     *
//     * @return The Bidi class for the character <code>c</code> that is in effect
//     *         for this <code>Bidi</code> instance.
//     *
//     * @see BidiClassifier
//     * @stable ICU 3.8
//     */
//    public int getCustomizedClass(int c) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * <code>setLine()</code> returns a <code>Bidi</code> object to
//     * contain the reordering information, especially the resolved levels,
//     * for all the characters in a line of text. This line of text is
//     * specified by referring to a <code>Bidi</code> object representing
//     * this information for a piece of text containing one or more paragraphs,
//     * and by specifying a range of indexes in this text.<p>
//     * In the new line object, the indexes will range from 0 to <code>limit-start-1</code>.<p>
//     *
//     * This is used after calling <code>setPara()</code>
//     * for a piece of text, and after line-breaking on that text.
//     * It is not necessary if each paragraph is treated as a single line.<p>
//     *
//     * After line-breaking, rules (L1) and (L2) for the treatment of
//     * trailing WS and for reordering are performed on
//     * a <code>Bidi</code> object that represents a line.<p>
//     *
//     * <strong>Important: </strong>the line <code>Bidi</code> object may
//     * reference data within the global text <code>Bidi</code> object.
//     * You should not alter the content of the global text object until
//     * you are finished using the line object.
//     *
//     * @param start is the line's first index into the text.
//     *
//     * @param limit is just behind the line's last index into the text
//     *        (its last index +1).
//     *
//     * @return a <code>Bidi</code> object that will now represent a line of the text.
//     *
//     * @throws IllegalStateException if this call is not preceded by a successful
//     *         call to <code>setPara</code>
//     * @throws IllegalArgumentException if start and limit are not in the range
//     *         <code>0&lt;=start&lt;limit&lt;=getProcessedLength()</code>,
//     *         or if the specified line crosses a paragraph boundary
//     *
//     * @see #setPara
//     * @see #getProcessedLength
//     * @stable ICU 3.8
//     */
//    public Bidi setLine(int start, int limit)
//    {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

    /**
     * Get the level for one character.
     *
     * @param charIndex the index of a character.
     *
     * @return The level for the character at <code>charIndex</code>.
     *
     * @throws IllegalStateException if this call is not preceded by a successful
     *         call to <code>setPara</code> or <code>setLine</code>
     * @throws IllegalArgumentException if charIndex is not in the range
     *         <code>0&lt;=charIndex&lt;getProcessedLength()</code>
     *
     * @see #getProcessedLength
     * @stable ICU 3.8
     */
    public byte getLevelAt(int charIndex)
    {
        return (byte)bidi.getLevelAt(charIndex);
    }

//    /**
//     * Get an array of levels for each character.<p>
//     *
//     * Note that this method may allocate memory under some
//     * circumstances, unlike <code>getLevelAt()</code>.
//     *
//     * @return The levels array for the text,
//     *         or <code>null</code> if an error occurs.
//     *
//     * @throws IllegalStateException if this call is not preceded by a successful
//     *         call to <code>setPara</code> or <code>setLine</code>
//     * @stable ICU 3.8
//     */
//    public byte[] getLevels()
//    {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * Get a logical run.
//     * This method returns information about a run and is used
//     * to retrieve runs in logical order.<p>
//     * This is especially useful for line-breaking on a paragraph.
//     *
//     * @param logicalPosition is a logical position within the source text.
//     *
//     * @return a BidiRun object filled with <code>start</code> containing
//     *        the first character of the run, <code>limit</code> containing
//     *        the limit of the run, and <code>embeddingLevel</code> containing
//     *        the level of the run.
//     *
//     * @throws IllegalStateException if this call is not preceded by a successful
//     *         call to <code>setPara</code> or <code>setLine</code>
//     * @throws IllegalArgumentException if logicalPosition is not in the range
//     *         <code>0&lt;=logicalPosition&lt;getProcessedLength()</code>
//     *
//     * @see com.ibm.icu.text.BidiRun
//     * @see com.ibm.icu.text.BidiRun#getStart()
//     * @see com.ibm.icu.text.BidiRun#getLimit()
//     * @see com.ibm.icu.text.BidiRun#getEmbeddingLevel()
//     *
//     * @stable ICU 3.8
//     */
//    public BidiRun getLogicalRun(int logicalPosition)
//    {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * Get the number of runs.
//     * This method may invoke the actual reordering on the
//     * <code>Bidi</code> object, after <code>setPara()</code>
//     * may have resolved only the levels of the text. Therefore,
//     * <code>countRuns()</code> may have to allocate memory,
//     * and may throw an exception if it fails to do so.
//     *
//     * @return The number of runs.
//     *
//     * @throws IllegalStateException if this call is not preceded by a successful
//     *         call to <code>setPara</code> or <code>setLine</code>
//     * @stable ICU 3.8
//     */
//    public int countRuns()
//    {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     *
//     * Get a <code>BidiRun</code> object according to its index. BidiRun methods
//     * may be used to retrieve the run's logical start, length and level,
//     * which can be even for an LTR run or odd for an RTL run.
//     * In an RTL run, the character at the logical start is
//     * visually on the right of the displayed run.
//     * The length is the number of characters in the run.<p>
//     * <code>countRuns()</code> is normally called
//     * before the runs are retrieved.
//     *
//     * <p>
//     *  Example:
//     * <pre>
//     *  Bidi bidi = new Bidi();
//     *  String text = "abc 123 DEFG xyz";
//     *  bidi.setPara(text, Bidi.RTL, null);
//     *  int i, count=bidi.countRuns(), logicalStart, visualIndex=0, length;
//     *  BidiRun run;
//     *  for (i = 0; i &lt; count; ++i) {
//     *      run = bidi.getVisualRun(i);
//     *      logicalStart = run.getStart();
//     *      length = run.getLength();
//     *      if (Bidi.LTR == run.getEmbeddingLevel()) {
//     *          do { // LTR
//     *              show_char(text.charAt(logicalStart++), visualIndex++);
//     *          } while (--length &gt; 0);
//     *      } else {
//     *          logicalStart += length;  // logicalLimit
//     *          do { // RTL
//     *              show_char(text.charAt(--logicalStart), visualIndex++);
//     *          } while (--length &gt; 0);
//     *      }
//     *  }
//     * </pre>
//     * <p>
//     * Note that in right-to-left runs, code like this places
//     * second surrogates before first ones (which is generally a bad idea)
//     * and combining characters before base characters.
//     * <p>
//     * Use of <code>{@link #writeReordered}</code>, optionally with the
//     * <code>{@link #KEEP_BASE_COMBINING}</code> option, can be considered in
//     * order to avoid these issues.
//     *
//     * @param runIndex is the number of the run in visual order, in the
//     *        range <code>[0..countRuns()-1]</code>.
//     *
//     * @return a BidiRun object containing the details of the run. The
//     *         directionality of the run is
//     *         <code>LTR==0</code> or <code>RTL==1</code>,
//     *         never <code>MIXED</code>.
//     *
//     * @throws IllegalStateException if this call is not preceded by a successful
//     *         call to <code>setPara</code> or <code>setLine</code>
//     * @throws IllegalArgumentException if <code>runIndex</code> is not in
//     *         the range <code>0&lt;=runIndex&lt;countRuns()</code>
//     *
//     * @see #countRuns()
//     * @see com.ibm.icu.text.BidiRun
//     * @see com.ibm.icu.text.BidiRun#getStart()
//     * @see com.ibm.icu.text.BidiRun#getLength()
//     * @see com.ibm.icu.text.BidiRun#getEmbeddingLevel()
//     * @stable ICU 3.8
//     */
//    public BidiRun getVisualRun(int runIndex)
//    {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * Get the visual position from a logical text position.
//     * If such a mapping is used many times on the same
//     * <code>Bidi</code> object, then calling
//     * <code>getLogicalMap()</code> is more efficient.
//     * <p>
//     * The value returned may be <code>MAP_NOWHERE</code> if there is no
//     * visual position because the corresponding text character is a Bidi
//     * control removed from output by the option
//     * <code>OPTION_REMOVE_CONTROLS</code>.
//     * <p>
//     * When the visual output is altered by using options of
//     * <code>writeReordered()</code> such as <code>INSERT_LRM_FOR_NUMERIC</code>,
//     * <code>KEEP_BASE_COMBINING</code>, <code>OUTPUT_REVERSE</code>,
//     * <code>REMOVE_BIDI_CONTROLS</code>, the visual position returned may not
//     * be correct. It is advised to use, when possible, reordering options
//     * such as {@link #OPTION_INSERT_MARKS} and {@link #OPTION_REMOVE_CONTROLS}.
//     * <p>
//     * Note that in right-to-left runs, this mapping places
//     * second surrogates before first ones (which is generally a bad idea)
//     * and combining characters before base characters.
//     * Use of <code>{@link #writeReordered}</code>, optionally with the
//     * <code>{@link #KEEP_BASE_COMBINING}</code> option can be considered instead
//     * of using the mapping, in order to avoid these issues.
//     *
//     * @param logicalIndex is the index of a character in the text.
//     *
//     * @return The visual position of this character.
//     *
//     * @throws IllegalStateException if this call is not preceded by a successful
//     *         call to <code>setPara</code> or <code>setLine</code>
//     * @throws IllegalArgumentException if <code>logicalIndex</code> is not in
//     *         the range <code>0&lt;=logicalIndex&lt;getProcessedLength()</code>
//     *
//     * @see #getLogicalMap
//     * @see #getLogicalIndex
//     * @see #getProcessedLength
//     * @see #MAP_NOWHERE
//     * @see #OPTION_REMOVE_CONTROLS
//     * @see #writeReordered
//     * @stable ICU 3.8
//     */
//    public int getVisualIndex(int logicalIndex)
//    {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }


//    /**
//     * Get the logical text position from a visual position.
//     * If such a mapping is used many times on the same
//     * <code>Bidi</code> object, then calling
//     * <code>getVisualMap()</code> is more efficient.
//     * <p>
//     * The value returned may be <code>MAP_NOWHERE</code> if there is no
//     * logical position because the corresponding text character is a Bidi
//     * mark inserted in the output by option
//     * <code>OPTION_INSERT_MARKS</code>.
//     * <p>
//     * This is the inverse method to <code>getVisualIndex()</code>.
//     * <p>
//     * When the visual output is altered by using options of
//     * <code>writeReordered()</code> such as <code>INSERT_LRM_FOR_NUMERIC</code>,
//     * <code>KEEP_BASE_COMBINING</code>, <code>OUTPUT_REVERSE</code>,
//     * <code>REMOVE_BIDI_CONTROLS</code>, the logical position returned may not
//     * be correct. It is advised to use, when possible, reordering options
//     * such as {@link #OPTION_INSERT_MARKS} and {@link #OPTION_REMOVE_CONTROLS}.
//     *
//     * @param visualIndex is the visual position of a character.
//     *
//     * @return The index of this character in the text.
//     *
//     * @throws IllegalStateException if this call is not preceded by a successful
//     *         call to <code>setPara</code> or <code>setLine</code>
//     * @throws IllegalArgumentException if <code>visualIndex</code> is not in
//     *         the range <code>0&lt;=visualIndex&lt;getResultLength()</code>
//     *
//     * @see #getVisualMap
//     * @see #getVisualIndex
//     * @see #getResultLength
//     * @see #MAP_NOWHERE
//     * @see #OPTION_INSERT_MARKS
//     * @see #writeReordered
//     * @stable ICU 3.8
//     */
//    public int getLogicalIndex(int visualIndex)
//    {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * Get a logical-to-visual index map (array) for the characters in the
//     * <code>Bidi</code> (paragraph or line) object.
//     * <p>
//     * Some values in the map may be <code>MAP_NOWHERE</code> if the
//     * corresponding text characters are Bidi controls removed from the visual
//     * output by the option <code>OPTION_REMOVE_CONTROLS</code>.
//     * <p>
//     * When the visual output is altered by using options of
//     * <code>writeReordered()</code> such as <code>INSERT_LRM_FOR_NUMERIC</code>,
//     * <code>KEEP_BASE_COMBINING</code>, <code>OUTPUT_REVERSE</code>,
//     * <code>REMOVE_BIDI_CONTROLS</code>, the visual positions returned may not
//     * be correct. It is advised to use, when possible, reordering options
//     * such as {@link #OPTION_INSERT_MARKS} and {@link #OPTION_REMOVE_CONTROLS}.
//     * <p>
//     * Note that in right-to-left runs, this mapping places
//     * second surrogates before first ones (which is generally a bad idea)
//     * and combining characters before base characters.
//     * Use of <code>{@link #writeReordered}</code>, optionally with the
//     * <code>{@link #KEEP_BASE_COMBINING}</code> option can be considered instead
//     * of using the mapping, in order to avoid these issues.
//     *
//     * @return an array of <code>getProcessedLength()</code>
//     *        indexes which will reflect the reordering of the characters.<br><br>
//     *        The index map will result in
//     *        <code>indexMap[logicalIndex]==visualIndex</code>, where
//     *        <code>indexMap</code> represents the returned array.
//     *
//     * @throws IllegalStateException if this call is not preceded by a successful
//     *         call to <code>setPara</code> or <code>setLine</code>
//     *
//     * @see #getVisualMap
//     * @see #getVisualIndex
//     * @see #getProcessedLength
//     * @see #MAP_NOWHERE
//     * @see #OPTION_REMOVE_CONTROLS
//     * @see #writeReordered
//     * @stable ICU 3.8
//     */
//    public int[] getLogicalMap()
//    {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * Get a visual-to-logical index map (array) for the characters in the
//     * <code>Bidi</code> (paragraph or line) object.
//     * <p>
//     * Some values in the map may be <code>MAP_NOWHERE</code> if the
//     * corresponding text characters are Bidi marks inserted in the visual
//     * output by the option <code>OPTION_INSERT_MARKS</code>.
//     * <p>
//     * When the visual output is altered by using options of
//     * <code>writeReordered()</code> such as <code>INSERT_LRM_FOR_NUMERIC</code>,
//     * <code>KEEP_BASE_COMBINING</code>, <code>OUTPUT_REVERSE</code>,
//     * <code>REMOVE_BIDI_CONTROLS</code>, the logical positions returned may not
//     * be correct. It is advised to use, when possible, reordering options
//     * such as {@link #OPTION_INSERT_MARKS} and {@link #OPTION_REMOVE_CONTROLS}.
//     *
//     * @return an array of <code>getResultLength()</code>
//     *        indexes which will reflect the reordering of the characters.<br><br>
//     *        The index map will result in
//     *        <code>indexMap[visualIndex]==logicalIndex</code>, where
//     *        <code>indexMap</code> represents the returned array.
//     *
//     * @throws IllegalStateException if this call is not preceded by a successful
//     *         call to <code>setPara</code> or <code>setLine</code>
//     *
//     * @see #getLogicalMap
//     * @see #getLogicalIndex
//     * @see #getResultLength
//     * @see #MAP_NOWHERE
//     * @see #OPTION_INSERT_MARKS
//     * @see #writeReordered
//     * @stable ICU 3.8
//     */
//    public int[] getVisualMap()
//    {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * This is a convenience method that does not use a <code>Bidi</code> object.
//     * It is intended to be used for when an application has determined the levels
//     * of objects (character sequences) and just needs to have them reordered (L2).
//     * This is equivalent to using <code>getLogicalMap()</code> on a
//     * <code>Bidi</code> object.
//     *
//     * @param levels is an array of levels that have been determined by
//     *        the application.
//     *
//     * @return an array of <code>levels.length</code>
//     *        indexes which will reflect the reordering of the characters.<p>
//     *        The index map will result in
//     *        <code>indexMap[logicalIndex]==visualIndex</code>, where
//     *        <code>indexMap</code> represents the returned array.
//     *
//     * @stable ICU 3.8
//     */
//    public static int[] reorderLogical(byte[] levels)
//    {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * This is a convenience method that does not use a <code>Bidi</code> object.
//     * It is intended to be used for when an application has determined the levels
//     * of objects (character sequences) and just needs to have them reordered (L2).
//     * This is equivalent to using <code>getVisualMap()</code> on a
//     * <code>Bidi</code> object.
//     *
//     * @param levels is an array of levels that have been determined by
//     *        the application.
//     *
//     * @return an array of <code>levels.length</code>
//     *        indexes which will reflect the reordering of the characters.<p>
//     *        The index map will result in
//     *        <code>indexMap[visualIndex]==logicalIndex</code>, where
//     *        <code>indexMap</code> represents the returned array.
//     *
//     * @stable ICU 3.8
//     */
//    public static int[] reorderVisual(byte[] levels)
//    {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * Invert an index map.
//     * The index mapping of the argument map is inverted and returned as
//     * an array of indexes that we will call the inverse map.
//     *
//     * @param srcMap is an array whose elements define the original mapping
//     * from a source array to a destination array.
//     * Some elements of the source array may have no mapping in the
//     * destination array. In that case, their value will be
//     * the special value <code>MAP_NOWHERE</code>.
//     * All elements must be >=0 or equal to <code>MAP_NOWHERE</code>.
//     * Some elements in the source map may have a value greater than the
//     * srcMap.length if the destination array has more elements than the
//     * source array.
//     * There must be no duplicate indexes (two or more elements with the
//     * same value except <code>MAP_NOWHERE</code>).
//     *
//     * @return an array representing the inverse map.
//     *         This array has a number of elements equal to 1 + the highest
//     *         value in <code>srcMap</code>.
//     *         For elements of the result array which have no matching elements
//     *         in the source array, the corresponding elements in the inverse
//     *         map will receive a value equal to <code>MAP_NOWHERE</code>.
//     *         If element with index i in <code>srcMap</code> has a value k different
//     *         from <code>MAP_NOWHERE</code>, this means that element i of
//     *         the source array maps to element k in the destination array.
//     *         The inverse map will have value i in its k-th element.
//     *         For all elements of the destination array which do not map to
//     *         an element in the source array, the corresponding element in the
//     *         inverse map will have a value equal to <code>MAP_NOWHERE</code>.
//     *
//     * @see #MAP_NOWHERE
//     * @stable ICU 3.8
//     */
//    public static int[] invertMap(int[] srcMap)
//    {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

    /*
     * Fields and methods for compatibility with java.text.bidi (Sun implementation)
     */

    /**
     * Constant indicating base direction is left-to-right.
     * @stable ICU 3.8
     */
    public static final int DIRECTION_LEFT_TO_RIGHT = LTR;

    /**
     * Constant indicating base direction is right-to-left.
     * @stable ICU 3.8
     */
    public static final int DIRECTION_RIGHT_TO_LEFT = RTL;

    /**
     * Constant indicating that the base direction depends on the first strong
     * directional character in the text according to the Unicode Bidirectional
     * Algorithm. If no strong directional character is present, the base
     * direction is left-to-right.
     * @stable ICU 3.8
     */
    public static final int DIRECTION_DEFAULT_LEFT_TO_RIGHT = LEVEL_DEFAULT_LTR;

    /**
     * Constant indicating that the base direction depends on the first strong
     * directional character in the text according to the Unicode Bidirectional
     * Algorithm. If no strong directional character is present, the base
     * direction is right-to-left.
     * @stable ICU 3.8
     */
    public static final int DIRECTION_DEFAULT_RIGHT_TO_LEFT = LEVEL_DEFAULT_RTL;

    /**
     * Create Bidi from the given paragraph of text and base direction.
     *
     * @param paragraph a paragraph of text
     * @param flags a collection of flags that control the algorithm. The
     *        algorithm understands the flags DIRECTION_LEFT_TO_RIGHT,
     *        DIRECTION_RIGHT_TO_LEFT, DIRECTION_DEFAULT_LEFT_TO_RIGHT, and
     *        DIRECTION_DEFAULT_RIGHT_TO_LEFT. Other values are reserved.
     * @see #DIRECTION_LEFT_TO_RIGHT
     * @see #DIRECTION_RIGHT_TO_LEFT
     * @see #DIRECTION_DEFAULT_LEFT_TO_RIGHT
     * @see #DIRECTION_DEFAULT_RIGHT_TO_LEFT
     * @stable ICU 3.8
     */
    public Bidi(String paragraph, int flags)
    {
        // Note: ICU and Oracle JDK are using the
        // same DIRECTION_* flags definitions.
        this(new java.text.Bidi(paragraph, flags));
    }

    /**
     * Create Bidi from the given paragraph of text.<p>
     *
     * The RUN_DIRECTION attribute in the text, if present, determines the base
     * direction (left-to-right or right-to-left). If not present, the base
     * direction is computed using the Unicode Bidirectional Algorithm,
     * defaulting to left-to-right if there are no strong directional characters
     * in the text. This attribute, if present, must be applied to all the text
     * in the paragraph.<p>
     *
     * The BIDI_EMBEDDING attribute in the text, if present, represents
     * embedding level information. Negative values from -1 to -62 indicate
     * overrides at the absolute value of the level. Positive values from 1 to
     * 62 indicate embeddings. Where values are zero or not defined, the base
     * embedding level as determined by the base direction is assumed.<p>
     *
     * The NUMERIC_SHAPING attribute in the text, if present, converts European
     * digits to other decimal digits before running the bidi algorithm. This
     * attribute, if present, must be applied to all the text in the paragraph.<p>
     *
     * Note: this constructor calls setPara() internally.
     *
     * @param paragraph a paragraph of text with optional character and
     *        paragraph attribute information
     * @stable ICU 3.8
     */
    public Bidi(AttributedCharacterIterator paragraph)
    {
        // ICU does not define its own attributes and just
        // use java.awt.font.TextAttribute. Thus, no mappings
        // are necessary.
        this(new java.text.Bidi(paragraph));
    }

    /**
     * Create Bidi from the given text, embedding, and direction information.
     * The embeddings array may be null. If present, the values represent
     * embedding level information. Negative values from -1 to -61 indicate
     * overrides at the absolute value of the level. Positive values from 1 to
     * 61 indicate embeddings. Where values are zero, the base embedding level
     * as determined by the base direction is assumed.<p>
     *
     * Note: this constructor calls setPara() internally.
     *
     * @param text an array containing the paragraph of text to process.
     * @param textStart the index into the text array of the start of the
     *        paragraph.
     * @param embeddings an array containing embedding values for each character
     *        in the paragraph. This can be null, in which case it is assumed
     *        that there is no external embedding information.
     * @param embStart the index into the embedding array of the start of the
     *        paragraph.
     * @param paragraphLength the length of the paragraph in the text and
     *        embeddings arrays.
     * @param flags a collection of flags that control the algorithm. The
     *        algorithm understands the flags DIRECTION_LEFT_TO_RIGHT,
     *        DIRECTION_RIGHT_TO_LEFT, DIRECTION_DEFAULT_LEFT_TO_RIGHT, and
     *        DIRECTION_DEFAULT_RIGHT_TO_LEFT. Other values are reserved.
     *
     * @throws IllegalArgumentException if the values in embeddings are
     *         not within the allowed range
     *
     * @see #DIRECTION_LEFT_TO_RIGHT
     * @see #DIRECTION_RIGHT_TO_LEFT
     * @see #DIRECTION_DEFAULT_LEFT_TO_RIGHT
     * @see #DIRECTION_DEFAULT_RIGHT_TO_LEFT
     * @stable ICU 3.8
     */
    public Bidi(char[] text,
            int textStart,
            byte[] embeddings,
            int embStart,
            int paragraphLength,
            int flags)
    {
        // Note: ICU and Oracle JDK are using the
        // same DIRECTION_* flags definitions.
        this(new java.text.Bidi(text, textStart, embeddings, embStart, paragraphLength, flags));
    }

    /**
     * Create a Bidi object representing the bidi information on a line of text
     * within the paragraph represented by the current Bidi. This call is not
     * required if the entire paragraph fits on one line.
     *
     * @param lineStart the offset from the start of the paragraph to the start
     *        of the line.
     * @param lineLimit the offset from the start of the paragraph to the limit
     *        of the line.
     *
     * @throws IllegalStateException if this call is not preceded by a successful
     *         call to <code>setPara</code>
     * @throws IllegalArgumentException if lineStart and lineLimit are not in the range
     *         <code>0&lt;=lineStart&lt;lineLimit&lt;=getProcessedLength()</code>,
     *         or if the specified line crosses a paragraph boundary
     * @stable ICU 3.8
     */
    public Bidi createLineBidi(int lineStart, int lineLimit)
    {
        return new Bidi(bidi.createLineBidi(lineStart, lineLimit));
    }

    /**
     * Return true if the line is not left-to-right or right-to-left. This means
     * it either has mixed runs of left-to-right and right-to-left text, or the
     * base direction differs from the direction of the only run of text.
     *
     * @return true if the line is not left-to-right or right-to-left.
     *
     * @throws IllegalStateException if this call is not preceded by a successful
     *         call to <code>setPara</code>
     * @stable ICU 3.8
     */
    public boolean isMixed()
    {
        return bidi.isMixed();
    }

    /**
     * Return true if the line is all left-to-right text and the base direction
     * is left-to-right.
     *
     * @return true if the line is all left-to-right text and the base direction
     *         is left-to-right.
     *
     * @throws IllegalStateException if this call is not preceded by a successful
     *         call to <code>setPara</code>
     * @stable ICU 3.8
     */
    public boolean isLeftToRight()
    {
        return bidi.isLeftToRight();
    }

    /**
     * Return true if the line is all right-to-left text, and the base direction
     * is right-to-left
     *
     * @return true if the line is all right-to-left text, and the base
     *         direction is right-to-left
     *
     * @throws IllegalStateException if this call is not preceded by a successful
     *         call to <code>setPara</code>
     * @stable ICU 3.8
     */
    public boolean isRightToLeft()
    {
        return bidi.isRightToLeft();
    }

    /**
     * Return true if the base direction is left-to-right
     *
     * @return true if the base direction is left-to-right
     *
     * @throws IllegalStateException if this call is not preceded by a successful
     *         call to <code>setPara</code> or <code>setLine</code>
     *
     * @stable ICU 3.8
     */
    public boolean baseIsLeftToRight()
    {
        return bidi.baseIsLeftToRight();
    }

    /**
     * Return the base level (0 if left-to-right, 1 if right-to-left).
     *
     * @return the base level
     *
     * @throws IllegalStateException if this call is not preceded by a successful
     *         call to <code>setPara</code> or <code>setLine</code>
     *
     * @stable ICU 3.8
     */
    public int getBaseLevel()
    {
        return bidi.getBaseLevel();
    }

    /**
     * Return the number of level runs.
     *
     * @return the number of level runs
     *
     * @throws IllegalStateException if this call is not preceded by a successful
     *         call to <code>setPara</code> or <code>setLine</code>
     *
     * @stable ICU 3.8
     */
    public int getRunCount()
    {
        return bidi.getRunCount();
    }

    /**
     * Return the level of the nth logical run in this line.
     *
     * @param run the index of the run, between 0 and <code>countRuns()-1</code>
     *
     * @return the level of the run
     *
     * @throws IllegalStateException if this call is not preceded by a successful
     *         call to <code>setPara</code> or <code>setLine</code>
     * @throws IllegalArgumentException if <code>run</code> is not in
     *         the range <code>0&lt;=run&lt;countRuns()</code>
     * @stable ICU 3.8
     */
    public int getRunLevel(int run)
    {
        return bidi.getRunLevel(run);
    }

    /**
     * Return the index of the character at the start of the nth logical run in
     * this line, as an offset from the start of the line.
     *
     * @param run the index of the run, between 0 and <code>countRuns()</code>
     *
     * @return the start of the run
     *
     * @throws IllegalStateException if this call is not preceded by a successful
     *         call to <code>setPara</code> or <code>setLine</code>
     * @throws IllegalArgumentException if <code>run</code> is not in
     *         the range <code>0&lt;=run&lt;countRuns()</code>
     * @stable ICU 3.8
     */
    public int getRunStart(int run)
    {
        return bidi.getRunStart(run);
    }

    /**
     * Return the index of the character past the end of the nth logical run in
     * this line, as an offset from the start of the line. For example, this
     * will return the length of the line for the last run on the line.
     *
     * @param run the index of the run, between 0 and <code>countRuns()</code>
     *
     * @return the limit of the run
     *
     * @throws IllegalStateException if this call is not preceded by a successful
     *         call to <code>setPara</code> or <code>setLine</code>
     * @throws IllegalArgumentException if <code>run</code> is not in
     *         the range <code>0&lt;=run&lt;countRuns()</code>
     * @stable ICU 3.8
     */
    public int getRunLimit(int run)
    {
        return bidi.getRunLimit(run);
    }

    /**
     * Return true if the specified text requires bidi analysis. If this returns
     * false, the text will display left-to-right. Clients can then avoid
     * constructing a Bidi object. Text in the Arabic Presentation Forms area of
     * Unicode is presumed to already be shaped and ordered for display, and so
     * will not cause this method to return true.
     *
     * @param text the text containing the characters to test
     * @param start the start of the range of characters to test
     * @param limit the limit of the range of characters to test
     *
     * @return true if the range of characters requires bidi analysis
     *
     * @stable ICU 3.8
     */
    public static boolean requiresBidi(char[] text,
            int start,
            int limit)
    {
        return java.text.Bidi.requiresBidi(text, start, limit);
    }

    /**
     * Reorder the objects in the array into visual order based on their levels.
     * This is a utility method to use when you have a collection of objects
     * representing runs of text in logical order, each run containing text at a
     * single level. The elements at <code>index</code> from
     * <code>objectStart</code> up to <code>objectStart + count</code> in the
     * objects array will be reordered into visual order assuming
     * each run of text has the level indicated by the corresponding element in
     * the levels array (at <code>index - objectStart + levelStart</code>).
     *
     * @param levels an array representing the bidi level of each object
     * @param levelStart the start position in the levels array
     * @param objects the array of objects to be reordered into visual order
     * @param objectStart the start position in the objects array
     * @param count the number of objects to reorder
     * @stable ICU 3.8
     */
    public static void reorderVisually(byte[] levels,
            int levelStart,
            Object[] objects,
            int objectStart,
            int count)
    {
        java.text.Bidi.reorderVisually(levels, levelStart, objects, objectStart, count);
    }

//    /**
//     * Take a <code>Bidi</code> object containing the reordering
//     * information for a piece of text (one or more paragraphs) set by
//     * <code>setPara()</code> or for a line of text set by <code>setLine()</code>
//     * and return a string containing the reordered text.
//     *
//     * <p>The text may have been aliased (only a reference was stored
//     * without copying the contents), thus it must not have been modified
//     * since the <code>setPara()</code> call.</p>
//     *
//     * This method preserves the integrity of characters with multiple
//     * code units and (optionally) combining characters.
//     * Characters in RTL runs can be replaced by mirror-image characters
//     * in the returned string. Note that "real" mirroring has to be done in a
//     * rendering engine by glyph selection and that for many "mirrored"
//     * characters there are no Unicode characters as mirror-image equivalents.
//     * There are also options to insert or remove Bidi control
//     * characters; see the descriptions of the return value and the
//     * <code>options</code> parameter, and of the option bit flags.
//     *
//     * @param options A bit set of options for the reordering that control
//     *                how the reordered text is written.
//     *                The options include mirroring the characters on a code
//     *                point basis and inserting LRM characters, which is used
//     *                especially for transforming visually stored text
//     *                to logically stored text (although this is still an
//     *                imperfect implementation of an "inverse Bidi" algorithm
//     *                because it uses the "forward Bidi" algorithm at its core).
//     *                The available options are:
//     *                <code>DO_MIRRORING</code>,
//     *                <code>INSERT_LRM_FOR_NUMERIC</code>,
//     *                <code>KEEP_BASE_COMBINING</code>,
//     *                <code>OUTPUT_REVERSE</code>,
//     *                <code>REMOVE_BIDI_CONTROLS</code>,
//     *                <code>STREAMING</code>
//     *
//     * @return The reordered text.
//     *         If the <code>INSERT_LRM_FOR_NUMERIC</code> option is set, then
//     *         the length of the returned string could be as large as
//     *         <code>getLength()+2*countRuns()</code>.<br>
//     *         If the <code>REMOVE_BIDI_CONTROLS</code> option is set, then the
//     *         length of the returned string may be less than
//     *         <code>getLength()</code>.<br>
//     *         If none of these options is set, then the length of the returned
//     *         string will be exactly <code>getProcessedLength()</code>.
//     *
//     * @throws IllegalStateException if this call is not preceded by a successful
//     *         call to <code>setPara</code> or <code>setLine</code>
//     *
//     * @see #DO_MIRRORING
//     * @see #INSERT_LRM_FOR_NUMERIC
//     * @see #KEEP_BASE_COMBINING
//     * @see #OUTPUT_REVERSE
//     * @see #REMOVE_BIDI_CONTROLS
//     * @see #OPTION_STREAMING
//     * @see #getProcessedLength
//     * @stable ICU 3.8
//     */
//    public String writeReordered(int options)
//    {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * Reverse a Right-To-Left run of Unicode text.
//     *
//     * This method preserves the integrity of characters with multiple
//     * code units and (optionally) combining characters.
//     * Characters can be replaced by mirror-image characters
//     * in the destination buffer. Note that "real" mirroring has
//     * to be done in a rendering engine by glyph selection
//     * and that for many "mirrored" characters there are no
//     * Unicode characters as mirror-image equivalents.
//     * There are also options to insert or remove Bidi control
//     * characters.
//     *
//     * This method is the implementation for reversing RTL runs as part
//     * of <code>writeReordered()</code>. For detailed descriptions
//     * of the parameters, see there.
//     * Since no Bidi controls are inserted here, the output string length
//     * will never exceed <code>src.length()</code>.
//     *
//     * @see #writeReordered
//     *
//     * @param src The RTL run text.
//     *
//     * @param options A bit set of options for the reordering that control
//     *                how the reordered text is written.
//     *                See the <code>options</code> parameter in <code>writeReordered()</code>.
//     *
//     * @return The reordered text.
//     *         If the <code>REMOVE_BIDI_CONTROLS</code> option
//     *         is set, then the length of the returned string may be less than
//     *         <code>src.length()</code>. If this option is not set,
//     *         then the length of the returned string will be exactly
//     *         <code>src.length()</code>.
//     *
//     * @throws IllegalArgumentException if <code>src</code> is null.
//     * @stable ICU 3.8
//     */
//    public static String writeReverse(String src, int options)
//    {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * Get the base direction of the text provided according to the Unicode
//     * Bidirectional Algorithm. The base direction is derived from the first
//     * character in the string with bidirectional character type L, R, or AL.
//     * If the first such character has type L, LTR is returned. If the first
//     * such character has type R or AL, RTL is returned. If the string does
//     * not contain any character of these types, then NEUTRAL is returned.
//     * This is a lightweight function for use when only the base direction is
//     * needed and no further bidi processing of the text is needed.
//     * @param paragraph the text whose paragraph level direction is needed.
//     * @return LTR, RTL, NEUTRAL
//     * @see #LTR
//     * @see #RTL
//     * @see #NEUTRAL
//     * @draft ICU 4.6
//     * @provisional This API might change or be removed in a future release.
//     */
//    public static byte getBaseDirection(CharSequence paragraph) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * Set the context before a call to setPara().<p>
//     *
//     * setPara() computes the left-right directionality for a given piece
//     * of text which is supplied as one of its arguments. Sometimes this piece
//     * of text (the "main text") should be considered in context, because text
//     * appearing before ("prologue") and/or after ("epilogue") the main text
//     * may affect the result of this computation.<p>
//     *
//     * This function specifies the prologue and/or the epilogue for the next
//     * call to setPara(). If successive calls to setPara()
//     * all need specification of a context, setContext() must be called
//     * before each call to setPara(). In other words, a context is not
//     * "remembered" after the following successful call to setPara().<p>
//     *
//     * If a call to setPara() specifies DEFAULT_LTR or
//     * DEFAULT_RTL as paraLevel and is preceded by a call to
//     * setContext() which specifies a prologue, the paragraph level will
//     * be computed taking in consideration the text in the prologue.<p>
//     *
//     * When setPara() is called without a previous call to
//     * setContext, the main text is handled as if preceded and followed
//     * by strong directional characters at the current paragraph level.
//     * Calling setContext() with specification of a prologue will change
//     * this behavior by handling the main text as if preceded by the last
//     * strong character appearing in the prologue, if any.
//     * Calling setContext() with specification of an epilogue will change
//     * the behavior of setPara() by handling the main text as if followed
//     * by the first strong character or digit appearing in the epilogue, if any.<p>
//     *
//     * Note 1: if <code>setContext</code> is called repeatedly without
//     *         calling <code>setPara</code>, the earlier calls have no effect,
//     *         only the last call will be remembered for the next call to
//     *         <code>setPara</code>.<p>
//     *
//     * Note 2: calling <code>setContext(null, null)</code>
//     *         cancels any previous setting of non-empty prologue or epilogue.
//     *         The next call to <code>setPara()</code> will process no
//     *         prologue or epilogue.<p>
//     *
//     * Note 3: users must be aware that even after setting the context
//     *         before a call to setPara() to perform e.g. a logical to visual
//     *         transformation, the resulting string may not be identical to what it
//     *         would have been if all the text, including prologue and epilogue, had
//     *         been processed together.<br>
//     * Example (upper case letters represent RTL characters):<br>
//     * &nbsp;&nbsp;prologue = "<code>abc DE</code>"<br>
//     * &nbsp;&nbsp;epilogue = none<br>
//     * &nbsp;&nbsp;main text = "<code>FGH xyz</code>"<br>
//     * &nbsp;&nbsp;paraLevel = LTR<br>
//     * &nbsp;&nbsp;display without prologue = "<code>HGF xyz</code>"
//     *             ("HGF" is adjacent to "xyz")<br>
//     * &nbsp;&nbsp;display with prologue = "<code>abc HGFED xyz</code>"
//     *             ("HGF" is not adjacent to "xyz")<br>
//     *
//     * @param prologue is the text which precedes the text that
//     *        will be specified in a coming call to setPara().
//     *        If there is no prologue to consider,
//     *        this parameter can be <code>null</code>.
//     *
//     * @param epilogue is the text which follows the text that
//     *        will be specified in a coming call to setPara().
//     *        If there is no epilogue to consider,
//     *        this parameter can be <code>null</code>.
//     *
//     * @see #setPara
//     * @draft ICU 4.8
//     * @provisional This API might change or be removed in a future release.
//     */
//    public void setContext(String prologue, String epilogue) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

}
