/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/text/Attic/Normalizer.java,v $ 
 * $Date: 2000/09/21 22:37:41 $ 
 * $Revision: 1.10 $
 *
 *****************************************************************************************
 */
package com.ibm.text;

import java.lang.Character;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import com.ibm.util.CompactByteArray;
import com.ibm.util.Utility;

/**
 * <tt>Normalizer</tt> transforms Unicode text into an equivalent composed or
 * decomposed form, allowing for easier sorting and searching of text.
 * <tt>Normalizer</tt> supports the standard normalization forms described in
 * <a href="http://www.unicode.org/unicode/reports/tr15/" target="unicode">
 * Unicode Technical Report #15</a>.
 * <p>
 * Characters with accents or other adornments can be encoded in
 * several different ways in Unicode.  For example, take the character "Â"
 * (A-acute).   In Unicode, this can be encoded as a single character (the
 * "composed" form):
 * <pre>
 *      00C1    LATIN CAPITAL LETTER A WITH ACUTE</pre>
 * or as two separate characters (the "decomposed" form):
 * <pre>
 *      0041    LATIN CAPITAL LETTER A
 *      0301    COMBINING ACUTE ACCENT</pre>
 * <p>
 * To a user of your program, however, both of these sequences should be
 * treated as the same "user-level" character "Â".  When you are searching or
 * comparing text, you must ensure that these two sequences are treated
 * equivalently.  In addition, you must handle characters with more than one
 * accent.  Sometimes the order of a character's combining accents is
 * significant, while in other cases accent sequences in different orders are
 * really equivalent.
 * <p>
 * Similarly, the string "ffi" can be encoded as three separate letters:
 * <pre>
 *      0066    LATIN SMALL LETTER F
 *      0066    LATIN SMALL LETTER F
 *      0069    LATIN SMALL LETTER I</pre>
 * or as the single character
 * <pre>
 *      FB03    LATIN SMALL LIGATURE FFI</pre>
 * <p>
 * The ffi ligature is not a distinct semantic character, and strictly speaking
 * it shouldn't be in Unicode at all, but it was included for compatibility
 * with existing character sets that already provided it.  The Unicode standard
 * identifies such characters by giving them "compatibility" decompositions
 * into the corresponding semantic characters.  When sorting and searching, you
 * will often want to use these mappings.
 * <p>
 * <tt>Normalizer</tt> helps solve these problems by transforming text into the
 * canonical composed and decomposed forms as shown in the first example above.
 * In addition, you can have it perform compatibility decompositions so that
 * you can treat compatibility characters the same as their equivalents.
 * Finally, <tt>Normalizer</tt> rearranges accents into the proper canonical
 * order, so that you do not have to worry about accent rearrangement on your
 * own.
 * <p>
 * <tt>Normalizer</tt> adds one optional behavior, {@link #IGNORE_HANGUL},
 * that differs from
 * the standard Unicode Normalization Forms.  This option can be passed
 * to the {@link #Normalizer constructors} and to the static
 * {@link #compose compose} and {@link #decompose decompose} methods.  This
 * option, and any that are added in the future, will be turned off by default.
 * <p>
 * There are three common usage models for <tt>Normalizer</tt>.  In the first,
 * the static {@link #normalize normalize()} method is used to process an
 * entire input string at once.  Second, you can create a <tt>Normalizer</tt>
 * object and use it to iterate through the normalized form of a string by
 * calling {@link #first} and {@link #next}.  Finally, you can use the
 * {@link #setIndex setIndex()} and {@link #getIndex} methods to perform
 * random-access iteration, which is very useful for searching.
 * <p>
 * <b>Note:</b> <tt>Normalizer</tt> objects behave like iterators and have
 * methods such as <tt>setIndex</tt>, <tt>next</tt>, <tt>previous</tt>, etc.
 * You should note that while the <tt>setIndex</tt> and <tt>getIndex</tt> refer
 * to indices in the underlying <em>input</em> text being processed, the
 * <tt>next</tt> and <tt>previous</tt> methods it iterate through characters
 * in the normalized <em>output</em>.  This means that there is not
 * necessarily a one-to-one correspondence between characters returned
 * by <tt>next</tt> and <tt>previous</tt> and the indices passed to and
 * returned from <tt>setIndex</tt> and <tt>getIndex</tt>.  It is for this
 * reason that <tt>Normalizer</tt> does not implement the
 * {@link CharacterIterator} interface.
 * <p>
 * <b>Note:</b> <tt>Normalizer</tt> is currently based on version 2.1.8
 * of the <a href="http://www.unicode.org" target="unicode">Unicode Standard</a>.
 * It will be updated as later versions of Unicode are released.  If you are
 * using this class on a JDK that supports an earlier version of Unicode, it
 * is possible that <tt>Normalizer</tt> may generate composed or dedecomposed
 * characters for which your JDK's {@link java.lang.Character} class does not
 * have any data.
 * <p>
 * @author Laura Werner, Mark Davis
 */
public final class Normalizer {

    /**
     * Constant indicating that the end of the iteration has been reached.
     * This is guaranteed to have the same value as {@link CharacterIterator#DONE}.
     */
    public static final char DONE = CharacterIterator.DONE;

    // This tells us what the bits in the "mode" object mean.
    private static final int COMPAT_BIT = 1;
    private static final int DECOMP_BIT = 2;
    private static final int COMPOSE_BIT = 4;

    /**
     * This class represents the mode of a {@link Normalizer}
     * object, <i>i.e.</i> the Unicode Normalization Form of the
     * text that the <tt>Normalizer</tt> produces.  <tt>Mode</tt> objects
     * are used as arguments to the {@link Normalizer#Normalizer constructors}
     * and {@link Normalizer#setMode setMode} method of <tt>Normalizer</tt>.
     * <p>
     * Clients cannot create <tt>Mode</tt> objects directly.
     * Instead, use the predefined constants {@link Normalizer#NO_OP},
     * {@link Normalizer#COMPOSE}, {@link Normalizer#COMPOSE_COMPAT},
     * {@link Normalizer#DECOMP}, and {@link Normalizer#DECOMP_COMPAT}.
     * <p>
     * @see Normalizer
     */
    public static final class Mode {
        Mode(int m) {
            mode = m;
        }
        final boolean compat() {
            return (mode & COMPAT_BIT) != 0;
        }
        final boolean compose() {
            return (mode & COMPOSE_BIT) != 0;
        }
        final boolean decomp() {
            return (mode & DECOMP_BIT) != 0;
        }
        final int mode;
    };

    /**
     * Null operation for use with the {@link #Normalizer constructors}
     * and the static {@link #normalize normalize} method.  This value tells
     * the <tt>Normalizer</tt> to do nothing but return unprocessed characters
     * from the underlying String or CharacterIterator.  If you have code which
     * requires raw text at some times and normalized text at others, you can
     * use <tt>NO_OP</tt> for the cases where you want raw text, rather
     * than having a separate code path that bypasses <tt>Normalizer</tt>
     * altogether.
     * <p>
     * @see #setMode
     */
    public static final Mode NO_OP = new Mode(0);

    /**
     * Canonical decomposition followed by canonical composition.  Used with the
     * {@link #Normalizer constructors} and the static {@link #normalize normalize}
     * method to determine the operation to be performed.
     * <p>
     * If all optional features (<i>e.g.</i> {@link #IGNORE_HANGUL}) are turned
     * off, this operation produces output that is in
     * <a href=http://www.unicode.org/unicode/reports/tr15/>Unicode Canonical Form</a>
     * <b>C</b>.
     * <p>
     * @see #setMode
     */
    public static final Mode COMPOSE = new Mode(COMPOSE_BIT);

    /**
     * Compatibility decomposition followed by canonical composition.
     * Used with the {@link #Normalizer constructors} and the static
     * {@link #normalize normalize} method to determine the operation to be performed.
     * <p>
     * If all optional features (<i>e.g.</i> {@link #IGNORE_HANGUL}) are turned
     * off, this operation produces output that is in
     * <a href=http://www.unicode.org/unicode/reports/tr15/>Unicode Canonical Form</a>
     * <b>KC</b>.
     * <p>
     * @see #setMode
     */
    public static final Mode COMPOSE_COMPAT = new Mode(COMPOSE_BIT | COMPAT_BIT);

    /**
     * Canonical decomposition.  This value is passed to the
     * {@link #Normalizer constructors} and the static {@link #normalize normalize}
     * method to determine the operation to be performed.
     * <p>
     * If all optional features (<i>e.g.</i> {@link #IGNORE_HANGUL}) are turned
     * off, this operation produces output that is in
     * <a href=http://www.unicode.org/unicode/reports/tr15/>Unicode Canonical Form</a>
     * <b>D</b>.
     * <p>
     * @see #setMode
     */
    public static final Mode DECOMP = new Mode(DECOMP_BIT);

    /**
     * Compatibility decomposition.  This value is passed to the
     * {@link #Normalizer constructors} and the static {@link #normalize normalize}
     * method to determine the operation to be performed.
     * <p>
     * If all optional features (<i>e.g.</i> {@link #IGNORE_HANGUL}) are turned
     * off, this operation produces output that is in
     * <a href=http://www.unicode.org/unicode/reports/tr15/>Unicode Canonical Form</a>
     * <b>KD</b>.
     * <p>
     * @see #setMode
     */
    public static final Mode DECOMP_COMPAT = new Mode(DECOMP_BIT | COMPAT_BIT);

    /**
     * Option to disable Hangul/Jamo composition and decomposition.
     * This option applies to Korean text,
     * which can be represented either in the Jamo alphabet or in Hangul
     * characters, which are really just two or three Jamo combined
     * into one visual glyph.  Since Jamo takes up more storage space than
     * Hangul, applications that process only Hangul text may wish to turn
     * this option on when decomposing text.
     * <p>
     * The Unicode standard treates Hangul to Jamo conversion as a
     * canonical decomposition, so this option must be turned <b>off</b> if you
     * wish to transform strings into one of the standard
     * <a href="http://www.unicode.org/unicode/reports/tr15/" target="unicode">
     * Unicode Normalization Forms</a>.
     * <p>
     * @see #setOption
     */
    public static final int IGNORE_HANGUL = 0x0001;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Creates a new <tt>Normalizer</tt> object for iterating over the
     * normalized form of a given string.
     * <p>
     * @param str   The string to be normalized.  The normalization
     *              will start at the beginning of the string.
     *
     * @param mode  The normalization mode.
     */
    public Normalizer(String str, Mode mode) {
        this(new StringCharacterIterator(str), mode, 0);
    }

    /**
     * Creates a new <tt>Normalizer</tt> object for iterating over the
     * normalized form of a given string.
     * <p>
     * The <tt>options</tt> parameter specifies which optional
     * <tt>Normalizer</tt> features are to be enabled for this object.
     * <p>
     * @param str   The string to be normalized.  The normalization
     *              will start at the beginning of the string.
     *
     * @param mode  The normalization mode.
     *
     * @param opt   Any optional features to be enabled.
     *              Currently the only available option is {@link #IGNORE_HANGUL}.
     *              If you want the default behavior corresponding to one of the
     *              standard Unicode Normalization Forms, use 0 for this argument.
     */
    public Normalizer(String str, Mode mode, int opt) {
        this(new StringCharacterIterator(str), mode, opt);
    }

    /**
     * Creates a new <tt>Normalizer</tt> object for iterating over the
     * normalized form of the given text.
     * <p>
     * @param iter  The input text to be normalized.  The normalization
     *              will start at the beginning of the string.
     *
     * @param mode  The normalization mode.
     *
     */
    public Normalizer(CharacterIterator iter, Mode mode) {
        this(iter, mode, 0);
    }

    /**
     * Creates a new <tt>Normalizer</tt> object for iterating over the
     * normalized form of the given text.
     * <p>
     * @param iter  The input text to be normalized.  The normalization
     *              will start at the beginning of the string.
     *
     * @param mode  The normalization mode.
     *
     * @param opt   Any optional features to be enabled.
     *              Currently the only available option is {@link #IGNORE_HANGUL}.
     *              If you want the default behavior corresponding to one of the
     *              standard Unicode Normalization Forms, use 0 for this argument.
     */
    public Normalizer(CharacterIterator iter, Mode mode, int opt) {
        text = iter;
        this.mode = mode;
        options = opt;

        // Compatibility explosions have lower indices; skip them if necessary
        minDecomp = mode.compat() ? 0 : DecompData.MAX_COMPAT;
    }

    /**
     * Clones this <tt>Normalizer</tt> object.  All properties of this
     * object are duplicated in the new object, including the cloning of any
     * {@link CharacterIterator} that was passed in to the constructor
     * or to {@link #setText(CharacterIterator) setText}.
     * However, the text storage underlying
     * the <tt>CharacterIterator</tt> is not duplicated unless the
     * iterator's <tt>clone</tt> method does so.
     */
    public Object clone() {
        try {
            Normalizer copy = (Normalizer) super.clone();
            copy.text = (CharacterIterator) text.clone();
            return copy;
        }
        catch (CloneNotSupportedException e) {
            throw new InternalError(e.toString());
        }
    }

    //-------------------------------------------------------------------------
    // Static utility methods
    //-------------------------------------------------------------------------

    /**
     * Normalizes a <tt>String</tt> using the given normalization operation.
     * <p>
     * The <tt>options</tt> parameter specifies which optional
     * <tt>Normalizer</tt> features are to be enabled for this operation.
     * Currently the only available option is {@link #IGNORE_HANGUL}.
     * If you want the default behavior corresponding to one of the standard
     * Unicode Normalization Forms, use 0 for this argument.
     * <p>
     * @param str       the input string to be normalized.
     *
     * @param aMode     the normalization mode
     *
     * @param options   the optional features to be enabled.
     */
    public static String normalize(String str, Mode mode, int options) {
        if (mode.compose()) {
            // compose() handles decomposition and reordering;
            // don't call decompose() first.
            return compose(str, mode.compat(), options);
        }
        if (mode.decomp()) {
            return decompose(str, mode.compat(), options);
        }
        return str;
    }

    //-------------------------------------------------------------------------
    // Compose methods
    //-------------------------------------------------------------------------

    /**
     * Compose a <tt>String</tt>.
     * <p>
     * The <tt>options</tt> parameter specifies which optional
     * <tt>Normalizer</tt> features are to be enabled for this operation.
     * Currently the only available option is {@link #IGNORE_HANGUL}.
     * If you want the default behavior corresponding
     * to Unicode Normalization Form <b>C</b> or <b>KC</b>,
     * use 0 for this argument.
     * <p>
     * @param source    the string to be composed.
     *
     * @param compat    Perform compatibility decomposition before composition.
     *                  If this argument is <tt>false</tt>, only canonical
     *                  decomposition will be performed.
     *
     * @param options   the optional features to be enabled.
     *
     * @return          the composed string.
     */
    public static String compose(String source, boolean compat, int options)
    {
        StringBuffer result = new StringBuffer();
        StringBuffer explodeBuf = new StringBuffer();

        int     explodePos = EMPTY;         // Position in input buffer
        int     basePos = 0;                // Position of last base in output string
        int     baseIndex = 0;              // Index of last base in "actions" array
        int     classesSeenL = 0;           // Combining classes seen since last base
        int     classesSeenH = 0;           //  64-bit mask
        int     action;

        // Compatibility explosions have lower indices; skip them if necessary
        int minExplode = compat ? 0 : ComposeData.MAX_COMPAT;
        int minDecomp  = compat ? 0 : DecompData.MAX_COMPAT;

        if (DEBUG) System.out.println("minExplode = " + minExplode);

        int i = 0;
        while (i < source.length() || explodePos != EMPTY) {
            // Get the next char from either the buffer or the source
            char ch;
            if (explodePos == EMPTY) {
                ch = source.charAt(i++);
            } else {
                ch = explodeBuf.charAt(explodePos++);
                if (explodePos >= explodeBuf.length()) {
                    explodePos = EMPTY;
                    explodeBuf.setLength(0);
                }
            }

            // Get the basic info for the character
            int charInfo = composeLookup(ch);
            int type = charInfo & ComposeData.TYPE_MASK;
            int index = charInfo >>> ComposeData.INDEX_SHIFT;
            
            if (DEBUG) System.out.println("Got char " + Utility.hex(ch) + ", type=" + type + ", index=" + index);

            // Examples of NON_COMPOSING_COMBINING with an index < minExplode:
            // 00A8 017F 03D2 1FBF 1FFE
            if (type == ComposeData.BASE || (type == ComposeData.NON_COMPOSING_COMBINING && index < minExplode)) {
                if (DEBUG) System.out.println("New base " + Utility.hex(ch) + ", type=" + type + ", index=" + index);
                classesSeenL = classesSeenH = 0;
                baseIndex = index;
                basePos = result.length();
                result.append(ch);
            }
            else if (type == ComposeData.COMBINING)
            {
                // assert(index > 0);
                int cclass = ComposeData.typeBit[index];
                // typeBit is a bit value from 0..63, indicating the class.
                // We use a bit mask of 2 32-bit ints.
                boolean seen = 0 != ((cclass < 32) ?
                    (classesSeenL & (1 << cclass)) :
                    (classesSeenH & (1 << (cclass & 31))));

                if (DEBUG) System.out.println("Class of " + Utility.hex(ch) + " = " + cclass +
                    " seen:" + seen +
                    " baseIndex:" + baseIndex +
                    " action:" + composeAction(baseIndex, index));

                // We can only combine a character with the base if we haven't
                // already seen a combining character with the same canonical class.
                // We only combine characters with an index from
                // 1..COMBINING_COUNT-1.  Indices >= COMBINING_COUNT are
                // also combining characters, but we know that they don't
                // compose with anything.
                if (index < ComposeData.COMBINING_COUNT && !seen
                    && (action = composeAction(baseIndex, index)) > 0)
                {
                    if (action > ComposeData.MAX_COMPOSED) {
                        // Pairwise explosion.  Actions above this value are really
                        // indices into an array that in turn contains indices
                        // into the exploding string table
                        // TODO: What if there are unprocessed chars in the explode buffer?
                        if (DEBUG) System.out.println("Pairwise exploding");
                        char newBase = pairExplode(explodeBuf, action);
                        explodePos = 0;
                        result.setCharAt(basePos, newBase);

                        baseIndex = composeLookup(newBase) >>> ComposeData.INDEX_SHIFT;
                        if (DEBUG) System.out.println("New base " + Utility.hex(newBase));
                    } else {
                        // Normal pairwise combination.  Replace the base char
                        if (DEBUG) System.out.println("Pairwise combining");
                        char newBase = (char) action;
                        result.setCharAt(basePos, newBase);

                        baseIndex = composeLookup(newBase) >>> ComposeData.INDEX_SHIFT;
                        if (DEBUG) System.out.println("New base " + Utility.hex(newBase));
                    }
                    //
                    // Since there are Unicode characters that cannot be combined in arbitrary
                    // order, we have to re-process any combining marks that go with this
                    // base character.  There are only four characters in Unicode that have
                    // this problem.  If they are fixed in Unicode 3.0, this code can go away.
                    //
                    int len = result.length();
                    if (len - basePos > 1) {
                        for (int j = basePos+1; j < len; j++) {
                            explodeBuf.append(result.charAt(j));
                        }
                        result.setLength(basePos+1);
                        classesSeenL = classesSeenH = 0;
                        if (explodePos == EMPTY) explodePos = 0;
                    }
                } else {
                    // No combination with this character
                    if (DEBUG) System.out.println("No action");
                    bubbleAppend(result, ch, cclass);
                    if (cclass < 32) {
                        classesSeenL |= 1 << cclass;
                    } else {
                        classesSeenH |= 1 << (cclass & 31);
                    }
                }
            }
            else if (index > minExplode) {
                // Single exploding character
                explode(explodeBuf, index);
                explodePos = 0;
                if (DEBUG) System.out.println("explosion: " + Utility.hex(ch) + " --> " + Utility.hex(explodeBuf));
            }
            else if (type == ComposeData.HANGUL && minExplode == 0) {
                // If we're in compatibility mode we need to decompose Hangul to Jamo,
                // because some of the Jamo might have compatibility decompositions.
                hangulToJamo(ch, explodeBuf, minDecomp);
                if (DEBUG) System.out.println("decomposed hangul " + Utility.hex(ch) + " to jamo " + Utility.hex(explodeBuf));
                explodePos = 0;
            }
            else if (type == ComposeData.INITIAL_JAMO) {
                classesSeenL = classesSeenH = 0;
                baseIndex = ComposeData.INITIAL_JAMO_INDEX;
                basePos = result.length();
                result.append(ch);
                if (DEBUG) System.out.println("got initial jamo " + Utility.hex(ch));
            }
            else if (type == ComposeData.MEDIAL_JAMO && classesSeenL == 0 && classesSeenH == 0
                        && baseIndex == ComposeData.INITIAL_JAMO_INDEX) {
                // If the last character was an initial jamo, we can combine it with this
                // one to create a Hangul character.
                int l = result.charAt(basePos) - JAMO_LBASE;
                int v = ch - JAMO_VBASE;
                char newCh = (char)(HANGUL_BASE + (l*JAMO_VCOUNT + v) * JAMO_TCOUNT);
                result.setCharAt(basePos, newCh);

                if (DEBUG) System.out.println("got medial jamo " + Utility.hex(ch) + ", replacing with Hangul " + Utility.hex(newCh));

                baseIndex = ComposeData.MEDIAL_JAMO_INDEX;
            }
            else if (type == ComposeData.FINAL_JAMO && classesSeenL == 0 && classesSeenH == 0
                        && baseIndex == ComposeData.MEDIAL_JAMO_INDEX) {
                // If the last character was a medial jamo that we turned into Hangul,
                // we can add this character too.
                char newCh = (char)(result.charAt(basePos) + (ch - JAMO_TBASE));
                result.setCharAt(basePos, newCh);

                if (DEBUG) System.out.println("got final jamo " + Utility.hex(ch) + ", replacing with Hangul " + Utility.hex(newCh));

                baseIndex = 0;
                basePos = -1;
                classesSeenL = classesSeenH = 0;
            } else {
                if (DEBUG) System.out.println("No base as of " + Utility.hex(ch));
                baseIndex = 0;
                basePos = -1;
                classesSeenL = classesSeenH = 0;
                result.append(ch);
            }
        }
        return result.toString();
    }

    /**
     * Compose starting with current input character and continuing
     * until just before the next base char.
     * <p>
     * <b>Input</b>:
     * <ul>
     *  <li>underlying char iter points to first character to compose
     * </ul>
     * <p>
     * <b>Output:</b>
     * <ul>
     *  <li>returns first char of composition or DONE if at end
     *  <li>Underlying char iter is pointing at next base char or past end
     * </ul>
     */
    private char nextCompose()
    {
        if (DEBUG) System.out.println("--------------- top of nextCompose() ---------------");

        int     explodePos = EMPTY;         // Position in input buffer
        int     basePos = 0;                // Position of last base in output string
        int     baseIndex = 0;              // Index of last base in "actions" array
        int     classesSeenL = 0;           // Combining classes seen since last base
        int     classesSeenH = 0;           //  64-bit mask
        int     action;
        char    lastBase = 0;
        boolean chFromText = true;

        // Compatibility explosions have lower indices; skip them if necessary
        int minExplode = mode.compat() ? 0 : ComposeData.MAX_COMPAT;
        int minDecomp  = mode.compat() ? 0 : DecompData.MAX_COMPAT;

        initBuffer();
        if (explodeBuf == null) {
            explodeBuf = new StringBuffer();
        } else {
            explodeBuf.setLength(0);
        }

        char ch = curForward();

        while (ch != DONE) {
            // Get the basic info for the character
            int charInfo = composeLookup(ch);
            int type = charInfo & ComposeData.TYPE_MASK;
            int index = charInfo >>> ComposeData.INDEX_SHIFT;

            if (type == ComposeData.BASE || (type == ComposeData.NON_COMPOSING_COMBINING && index < minExplode)) {
                if (buffer.length() > 0 && chFromText && explodePos == EMPTY) {
                    // When we hit a base char in the source text, we can return the text
                    // that's been composed so far.  We'll re-process this char next time through.
                    if (DEBUG) System.out.println("returning early because we hit a new base");
                    break;
                }
                classesSeenL = classesSeenH = 0;
                baseIndex = index;
                basePos = buffer.length();
                buffer.append(ch);
                if (DEBUG) System.out.println("got BASE char " + Utility.hex(ch) + ", type=" + type + ", index=" + index);
                lastBase = ch;
            }
            else if (type == ComposeData.COMBINING)
            {
                // assert(index > 0);
                int cclass = ComposeData.typeBit[index];
                boolean seen = 0 != ((cclass < 32) ?
                    (classesSeenL & (1 << cclass)) :
                    (classesSeenH & (1 << (cclass & 31))));

                if (DEBUG) System.out.println("got COMBINING char " + Utility.hex(ch) + ", type=" + type + ", index=" + index
                        + ", class=" + cclass);

                // We can only combine a character with the base if we haven't
                // already seen a combining character with the same canonical class.
                if (index < ComposeData.COMBINING_COUNT && !seen
                    && (action = composeAction(baseIndex, index)) > 0)
                {
                    if (action > ComposeData.MAX_COMPOSED) {
                        // Pairwise explosion.  Actions above this value are really
                        // indices into an array that in turn contains indices
                        // into the exploding string table
                        // TODO: What if there are unprocessed chars in the explode buffer?
                        char newBase = pairExplode(explodeBuf, action);
                        explodePos = 0;
                        buffer.setCharAt(basePos, newBase);

                        baseIndex = composeLookup(newBase) >>> ComposeData.INDEX_SHIFT;

                        if (DEBUG) System.out.println("Pairwise explosion: " + Utility.hex(lastBase) + "," + Utility.hex(ch)
                            + " --> " + Utility.hex(newBase) + "," + Utility.hex(explodeBuf));
                        lastBase = newBase;
                    } else {
                        // Normal pairwise combination.  Replace the base char
                        char newBase = (char) action;
                        buffer.setCharAt(basePos, newBase);

                        baseIndex = composeLookup(newBase) >>> ComposeData.INDEX_SHIFT;

                        if (DEBUG) System.out.println("Pairwise combination: " + Utility.hex(lastBase) + "," + Utility.hex(ch)
                            + " --> " + Utility.hex(newBase));
                        lastBase = newBase;
                    }
                    //
                    // Since there are Unicode characters that cannot be combined in arbitrary
                    // order, we have to re-process any combining marks that go with this
                    // base character.  There are only four characters in Unicode that have
                    // this problem.  If they are fixed in Unicode 3.0, this code can go away.
                    //
                    int len = buffer.length();
                    if (len - basePos > 1) {
                        if (DEBUG) System.out.println("Reprocessing combining marks");
                        for (int j = basePos+1; j < len; j++) {
                            explodeBuf.append(buffer.charAt(j));
                        }
                        buffer.setLength(basePos+1);
                        classesSeenL = classesSeenH = 0;
                        if (explodePos == EMPTY) explodePos = 0;
                    }
                } else {
                    if (DEBUG) System.out.println("char doesn't combine");
                    // No combination with this character
                    bubbleAppend(buffer, ch, cclass);
                    if (cclass < 32) {
                        classesSeenL |= 1 << cclass;
                    } else {
                        classesSeenH |= 1 << (cclass & 31);
                    }
                }
            }
            else if (index > minExplode) {
                // Single exploding character
                explode(explodeBuf, index);
                explodePos = 0;
                if (DEBUG) System.out.println("explosion: " + Utility.hex(ch) + " --> " + Utility.hex(explodeBuf));
            }
            else if (type == ComposeData.HANGUL && minExplode == 0) {
                // If we're in compatibility mode we need to decompose Hangul to Jamo,
                // because some of the Jamo might have compatibility decompositions.
                hangulToJamo(ch, explodeBuf, minDecomp);
                if (DEBUG) System.out.println("decomposed hangul " + Utility.hex(ch) + " to jamo " + Utility.hex(explodeBuf));
                explodePos = 0;
            }
            else if (type == ComposeData.INITIAL_JAMO) {
                if (buffer.length() > 0 && chFromText && explodePos == EMPTY) {
                    // When we hit a base char in the source text, we can return the text
                    // that's been composed so far.  We'll re-process this char next time through.
                    if (DEBUG) System.out.println("returning early because we hit a new base");
                    break;
                }
                classesSeenL = classesSeenH = 0;
                baseIndex = ComposeData.INITIAL_JAMO_INDEX;
                basePos = buffer.length();
                buffer.append(ch);
                if (DEBUG) System.out.println("got initial jamo " + Utility.hex(ch));
            }
            else if (type == ComposeData.MEDIAL_JAMO && classesSeenL == 0 && classesSeenH == 0
                        && baseIndex == ComposeData.INITIAL_JAMO_INDEX) {
                // If the last character was an initial jamo, we can combine it with this
                // one to create a Hangul character.
                int l = buffer.charAt(basePos) - JAMO_LBASE;
                int v = ch - JAMO_VBASE;
                char newCh = (char)(HANGUL_BASE + (l*JAMO_VCOUNT + v) * JAMO_TCOUNT);
                buffer.setCharAt(basePos, newCh);

                if (DEBUG) System.out.println("got medial jamo " + Utility.hex(ch) + ", replacing with Hangul " + Utility.hex(newCh));

                baseIndex = ComposeData.MEDIAL_JAMO_INDEX;
            }
            else if (type == ComposeData.FINAL_JAMO && classesSeenL == 0 && classesSeenH == 0
                        && baseIndex == ComposeData.MEDIAL_JAMO_INDEX) {
                // If the last character was a medial jamo that we turned into Hangul,
                // we can add this character too.
                char newCh = (char)(buffer.charAt(basePos) + (ch - JAMO_TBASE));
                buffer.setCharAt(basePos, newCh);

                if (DEBUG) System.out.println("got final jamo " + Utility.hex(ch) + ", replacing with Hangul " + Utility.hex(newCh));

                baseIndex = 0;
                basePos = -1;
                classesSeenL = classesSeenH = 0;
            } else {
                // TODO: deal with JAMO character types
                baseIndex = 0;
                basePos = -1;
                classesSeenL = classesSeenH = 0;
                buffer.append(ch);
                if (DEBUG) System.out.println("UNKNOWN char " + Utility.hex(ch));
            }

            if (explodePos == EMPTY) {
                ch = text.next();
                chFromText = true;
            } else {
                ch = explodeBuf.charAt(explodePos++);
                if (explodePos >= explodeBuf.length()) {
                    explodePos = EMPTY;
                    explodeBuf.setLength(0);
                }
                chFromText = false;
            }
        }
        if (buffer.length() > 0) {
            bufferLimit = buffer.length() - 1;
            ch = buffer.charAt(0);
        } else {
            ch = DONE;
            bufferLimit = 0;
        }
        return ch;
    }

    /**
     * Compose starting with the input char just before the current position
     * and continuing backward until (and including) the previous base char.
     * <p>
     * <b>Input</b>:
     * <ul>
     *  <li>underlying char iter points just after last char to decompose
     * </ul>
     * <p>
     * <b>Output:</b>
     * <ul>
     *  <li>returns last char of resulting decomposition sequence
     *  <li>underlying iter points to lowest-index char we decomposed, i.e. the base char
     * </ul>
     */
    private char prevCompose() {
        if (DEBUG) System.out.println("--------------- top of prevCompose() ---------------");

        // Compatibility explosions have lower indices; skip them if necessary
        int minExplode = mode.compat() ? 0 : ComposeData.MAX_COMPAT;

        initBuffer();

        // Slurp up characters until we hit a base char or an initial Jamo
        char ch;
        while ((ch = curBackward()) != DONE) {
            buffer.insert(0, ch);

            // Get the basic info for the character
            int charInfo = composeLookup(ch);
            int type = charInfo & ComposeData.TYPE_MASK;
            int index = charInfo >>> ComposeData.INDEX_SHIFT;

            if (DEBUG) System.out.println("prevCompose got char " + Utility.hex(ch) +
                                          ", type=" + type + ", index=" + index +
                                          ", minExplode=" + minExplode);

            if (type == ComposeData.BASE
                || (type == ComposeData.NON_COMPOSING_COMBINING && index < minExplode)
                || type == ComposeData.HANGUL
                || type == ComposeData.INITIAL_JAMO)
            {
                break;
            }
        }
        // If there's more than one character in the buffer, compose it all at once....
        if (buffer.length() > 0) {
            // TODO: The performance of this is awful; add a way to compose
            // a StringBuffer in place.
            String composed = compose(buffer.toString(), mode.compat(), options);
            if (DEBUG) System.out.println("prevCompose called compose(" + Utility.hex(buffer) +
                                          ")->" + Utility.hex(composed));            
            buffer.setLength(0);
            buffer.append(composed);

            if (buffer.length() > 1) {
                bufferLimit = bufferPos = buffer.length() - 1;
                ch = buffer.charAt(bufferPos);
            } else {
                ch = buffer.charAt(0);
            }
        }
        else {
            ch = DONE;
        }

        if (DEBUG) System.out.println("prevCompose returning " + Utility.hex(ch));
        return ch;
    }

    private static void bubbleAppend(StringBuffer target, char ch, int cclass) {
        if (DEBUG) System.out.println(" bubbleAppend(" + Utility.hex(target) + ", " + Utility.hex(ch) + ", " + cclass + ")" );
        if (DEBUG) System.out.println("  getComposeClass(" + Utility.hex(ch) + ")=" + getComposeClass(ch));
        int i;
        for (i = target.length() - 1; i > 0; --i) {
            int iClass = getComposeClass(target.charAt(i));
            if (DEBUG) System.out.println(" bubbleAppend: target[" + i + "]=" + Utility.hex(target.charAt(i)) + " is class " + iClass);

            if (iClass == 1 || iClass <= cclass) {      // 1 means combining class 0
                // We've hit something we can't bubble this character past, so insert here
                break;
            }
        }
        // We need to insert just after character "i"
        if (DEBUG) System.out.println(" bubbleAppend inserting at index " + (i+1));
        target.insert(i+1, ch);
    }

    private static int getComposeClass(char ch) {
        int cclass = 0;
        int charInfo = composeLookup(ch);
        int type = charInfo & ComposeData.TYPE_MASK;
        if (type == ComposeData.COMBINING) {
            cclass = ComposeData.typeBit[charInfo >>> ComposeData.INDEX_SHIFT];
        }
        return cclass;
    }

    static final int composeLookup(char ch) {
        return ComposeData.lookup.elementAt(ch);
    }

    static final int composeAction(int baseIndex, int comIndex) {
        return ComposeData.actions.elementAt((char)(baseIndex
                                            + ComposeData.MAX_BASES*comIndex));
    }

    static final void explode(StringBuffer target, int index) {
        char ch;
        while ((ch = ComposeData.replace.charAt(index++)) != 0)
            target.append(ch);
    }

    static final char pairExplode(StringBuffer target, int action) {
        int index = ComposeData.actionIndex[action - ComposeData.MAX_COMPOSED];
        explode(target, index + 1);
        return ComposeData.replace.charAt(index);   // New base char
    }


    //-------------------------------------------------------------------------
    // Decompose methods
    //-------------------------------------------------------------------------

    /**
     * Static method to decompose a <tt>String</tt>.
     * <p>
     * The <tt>options</tt> parameter specifies which optional
     * <tt>Normalizer</tt> features are to be enabled for this operation.
     * Currently the only available option is {@link #IGNORE_HANGUL}.
     * The desired options should be OR'ed together to determine the value
     * of this argument.  If you want the default behavior corresponding
     * to Unicode Normalization Form <b>D</b> or <b>KD</b>,
     * use 0 for this argument.
     * <p>
     * @param str   the string to be decomposed.
     *
     * @param compat    Perform compatibility decomposition.
     *                  If this argument is <tt>false</tt>, only canonical
     *                  decomposition will be performed.
     *
     *
     * @return      the decomposed string.
     */
    public static String decompose(String source, boolean compat, int options)
    {
        if (DEBUG) System.out.println("--------------- top of decompose() ---------------");

        boolean hangul = (options & IGNORE_HANGUL) == 0;
        int minDecomp = compat ? 0 : DecompData.MAX_COMPAT;
 
        StringBuffer result = new StringBuffer();
        StringBuffer buffer = null;

        int i = 0, bufPtr = -1;

        while (i < source.length() || bufPtr >= 0)
        {
            char ch;

            if (bufPtr >= 0) {
                ch = buffer.charAt(bufPtr++);
                if (bufPtr == buffer.length()) {
                    bufPtr = -1;
                }
            } else {
                ch = source.charAt(i++);
            }

            int offset = DecompData.offsets.elementAt(ch);
            int index = offset & DecompData.DECOMP_MASK;

            if (DEBUG) System.out.println("decompose got " + Utility.hex(ch));

            if (index > minDecomp) {
                if ((offset & DecompData.DECOMP_RECURSE) != 0) {
                    if (DEBUG) System.out.println(" " + Utility.hex(ch) + " has RECURSIVE decomposition, index=" + index);
                    if (buffer == null) {
                        buffer = new StringBuffer();
                    } else {
                        buffer.setLength(0);
                    }
                    doAppend(DecompData.contents, index, buffer);
                    bufPtr = 0;
                } else {
                    if (DEBUG) System.out.println(" " + Utility.hex(ch) + " has decomposition, index=" + index);
                    doAppend(DecompData.contents, index, result);
                }
            } else if (ch >= HANGUL_BASE && ch < HANGUL_LIMIT && hangul) {
                hangulToJamo(ch, result, minDecomp);
            } else {
                result.append(ch);
            }
        }
        fixCanonical(result);
        return result.toString();
    }

    /**
     * Decompose starting with current input character and continuing
     * until just before the next base char.
     * <p>
     * <b>Input</b>:
     * <ul>
     *  <li>underlying char iter points to first character to decompose
     * </ul>
     * <p>
     * <b>Output:</b>
     * <ul>
     *  <li>returns first char of decomposition or DONE if at end
     *  <li>Underlying char iter is pointing at next base char or past end
     * </ul>
     */
    private char nextDecomp()
    {
        if (DEBUG) System.out.println("--------------- top of nextDecomp() ---------------");

        boolean hangul = (options & IGNORE_HANGUL) == 0;
        char ch = curForward();

        int offset = DecompData.offsets.elementAt(ch);
        int index = offset & DecompData.DECOMP_MASK;

        if (index > minDecomp || DecompData.canonClass.elementAt(ch) != DecompData.BASE)
        {
            initBuffer();

            if (index > minDecomp) {
                if (DEBUG) System.out.println(" " + Utility.hex(ch) + " has decomposition, index=" + index);
                doAppend(DecompData.contents, index, buffer);

                if ((offset & DecompData.DECOMP_RECURSE) != 0) {
                    // Need to decompose the output of this decomposition recursively.
                    for (int i = 0; i < buffer.length(); i++) {
                        ch = buffer.charAt(i);
                        index = DecompData.offsets.elementAt(ch) & DecompData.DECOMP_MASK;

                        if (index > minDecomp) {
                            i += doReplace(DecompData.contents, index, buffer, i);
                        }
                    }
                }
            } else {
                buffer.append(ch);
            }
            boolean needToReorder = false;

            // Any other combining chacters that immediately follow the decomposed
            // character must be included in the buffer too, because they're
            // conceptually part of the same logical character.
            while ((ch = text.next()) != DONE
                && DecompData.canonClass.elementAt(ch) != DecompData.BASE)
            {
                needToReorder = true;
                // Decompose any of these characters that need it - Liu
                index = DecompData.offsets.elementAt(ch) & DecompData.DECOMP_MASK;
                if (index > minDecomp) {
                    doAppend(DecompData.contents, index, buffer);
                } else {
                    buffer.append(ch);
                }
            }

            if (buffer.length() > 1 && needToReorder) {
                // If there is more than one combining character in the buffer,
                // put them into the canonical order.
                // But we don't need to sort if only characters are the ones that
                // resulted from decomosing the base character.
                fixCanonical(buffer);
            }
            bufferLimit = buffer.length() - 1;
            ch = buffer.charAt(0);
        } else {
            // Just use this character, but first advance to the next one
            text.next();

            // Do Hangul -> Jamo decomposition if necessary
            if (hangul && ch >= HANGUL_BASE && ch < HANGUL_LIMIT) {
                initBuffer();
                hangulToJamo(ch, buffer, minDecomp);
                bufferLimit = buffer.length() - 1;
                ch = buffer.charAt(0);
            }
        }
        if (DEBUG) System.out.println(" nextDecomp returning " + Utility.hex(ch) + ", text index=" + text.getIndex());
        return ch;
    }

    /**
     * Decompose starting with the input char just before the current position
     * and continuing backward until (and including) the previous base char.
     * <p>
     * <b>Input</b>:
     * <ul>
     *  <li>underlying char iter points just after last char to decompose
     * </ul>
     * <p>
     * <b>Output:</b>
     * <ul>
     *  <li>returns last char of resulting decomposition sequence
     *  <li>underlying iter points to lowest-index char we decomposed, i.e. the base char
     * </ul>
     */
    private char prevDecomp() {
        if (DEBUG) System.out.println("--------------- top of prevDecomp() ---------------");

        boolean hangul = (options & IGNORE_HANGUL) == 0;

        char ch = curBackward();

        int offset = DecompData.offsets.elementAt(ch);
        int index = offset & DecompData.DECOMP_MASK;

        if (DEBUG) System.out.println("prevDecomp got input char " + Utility.hex(ch));

        if (index > minDecomp || DecompData.canonClass.elementAt(ch) != DecompData.BASE)
        {
            initBuffer();

            // This method rewritten to pass conformance tests. - Liu
            // Collect all characters up to the previous base char
            while (ch != DONE) {
                buffer.insert(0, ch);
                if (DecompData.canonClass.elementAt(ch) == DecompData.BASE) break;
                ch = text.previous();
            }

            if (DEBUG) System.out.println("prevDecomp buffer: " + Utility.hex(buffer));

            // Decompose the buffer
            for (int i = 0; i < buffer.length(); i++) {
                ch = buffer.charAt(i);
                offset = DecompData.offsets.elementAt(ch);
                index = offset & DecompData.DECOMP_MASK;                

                if (index > minDecomp) {
                    int j = doReplace(DecompData.contents, index, buffer, i);
                    if ((offset & DecompData.DECOMP_RECURSE) != 0) {
                        // Need to decompose this recursively
                        for (; i < j; ++i) {
                            ch = buffer.charAt(i);
                            index = DecompData.offsets.elementAt(ch) & DecompData.DECOMP_MASK;
                            if (index > minDecomp) {
                                i += doReplace(DecompData.contents, index, buffer, i);
                            }
                        }
                    }
                    i = j;
                }
            }
            
            if (DEBUG) System.out.println("prevDecomp buffer after decomp: " + Utility.hex(buffer));

            if (buffer.length() > 1) {
                // If there is more than one combining character in the buffer,
                // put them into the canonical order.
                fixCanonical(buffer);
            }
            bufferLimit = bufferPos = buffer.length() - 1;
            ch = buffer.charAt(bufferPos);
        }
        else if (hangul && ch >= HANGUL_BASE && ch < HANGUL_LIMIT) {
            initBuffer();
            hangulToJamo(ch, buffer, minDecomp);
            bufferLimit = bufferPos = buffer.length() - 1;
            ch = buffer.charAt(bufferPos);
        }
        if (DEBUG) System.out.println(" prevDecomp returning '" + ch + "' " + Utility.hex(ch) + ", text index=" + text.getIndex());
        return ch;
    }

    static final int getClass(char ch) {
        int value = DecompData.canonClass.elementAt(ch);
        return (value >= 0) ? value : value + 256;
    }


    //-------------------------------------------------------------------------
    // CharacterIterator overrides
    //-------------------------------------------------------------------------

    /**
     * Return the current character in the normalized text.
     */
    public char current() {
        if (currentChar == DONE) {
            if (mode.compose()) {
                currentChar = nextCompose();
            }
            else if (mode.decomp()) {
                currentChar = nextDecomp();
            }
            else {
                currentChar = text.current();
            }
        }
        return currentChar;
    }

    /**
     * Return the first character in the normalized text.  This resets
     * the <tt>Normalizer's</tt> position to the beginning of the text.
     */
    public char first() {
        return setIndex(text.getBeginIndex());
    }

    /**
     * Return the last character in the normalized text.  This resets
     * the <tt>Normalizer's</tt> position to be just before the
     * the input text corresponding to that normalized character.
     */
    public char last() {
        text.setIndex(text.getEndIndex() - 1);  // Setting to getEndIndex() fails in 1.1
        atEnd = true;                               // so work around the bug

        currentChar = DONE;                     // The current char hasn't been processed
        clearBuffer();                          // The buffer is empty too
        return previous();
    }

    /**
     * Return the next character in the normalized text and advance
     * the iteration position by one.  If the end
     * of the text has already been reached, {@link #DONE} is returned.
     */
    public char next() {
        if (bufferPos < bufferLimit) {
            // There are output characters left in the buffer
            currentChar = buffer.charAt(++bufferPos);
        }
        else {
            bufferLimit = bufferPos = 0;    // Buffer is now out of date
            if (mode.compose()) {
                currentChar = nextCompose();
            }
            else if (mode.decomp()) {
                currentChar = nextDecomp();
            }
            else {
                currentChar = text.next();
            }
        }
        return currentChar;
    }

    /**
     * Return the previous character in the normalized text and decrement
     * the iteration position by one.  If the beginning
     * of the text has already been reached, {@link #DONE} is returned.
     */
    public char previous() {
        if (bufferPos > 0) {
            // There are output characters left in the buffer
            currentChar = buffer.charAt(--bufferPos);
        }
        else {
            bufferLimit = bufferPos = 0;    // Buffer is now out of date
            if (mode.compose()) {
                currentChar = prevCompose();
            }
            else if (mode.decomp()) {
                currentChar = prevDecomp();
            }
            else {
                currentChar = text.previous();
            }
        }
        return currentChar;
    }

    /**
     * Set the iteration position in the input text that is being normalized
     * and return the first normalized character at that position.
     * <p>
     * @param index the desired index in the input text.
     *
     * @return      the first normalized character that is the result of iterating
     *              forward starting at the given index.
     *
     * @throws IllegalArgumentException if the given index is less than
     *          {@link #getBeginIndex} or greater than {@link #getEndIndex}.
     */
    public char setIndex(int index) {
        text.setIndex(index);   // Checks range
        currentChar = DONE;     // The current char hasn't been processed
        clearBuffer();          // The buffer is empty too

        return current();
    }

    /**
     * Retrieve the current iteration position in the input text that is
     * being normalized.  This method is useful in applications such as
     * searching, where you need to be able to determine the position in
     * the input text that corresponds to a given normalized output character.
     */
    public final int getIndex() {
        return text.getIndex();
    }

    /**
     * Retrieve the index of the start of the input text.  This is the begin index
     * of the <tt>CharacterIterator</tt> or the start (i.e. 0) of the <tt>String</tt>
     * over which this <tt>Normalizer</tt> is iterating
     */
    public final int getBeginIndex() {
        return text.getBeginIndex();
    }

    /**
     * Retrieve the index of the end of the input text.  This is the end index
     * of the <tt>CharacterIterator</tt> or the length of the <tt>String</tt>
     * over which this <tt>Normalizer</tt> is iterating
     */
    public final int getEndIndex() {
        return text.getEndIndex();
    }

    //-------------------------------------------------------------------------
    // Property access methods
    //-------------------------------------------------------------------------

    /**
     * Set the normalization mode for this object.
     * <p>
     * <b>Note:</b>If the normalization mode is changed while iterating
     * over a string, calls to {@link #next} and {@link #previous} may
     * return previously buffers characters in the old normalization mode
     * until the iteration is able to re-sync at the next base character.
     * It is safest to call {@link #setText setText()}, {@link #first},
     * {@link #last}, etc. after calling <tt>setMode</tt>.
     * <p>
     * @param newMode the new mode for this <tt>Normalizer</tt>.
     * The supported modes are:
     * <ul>
     *  <li>{@link #COMPOSE}        - Unicode canonical decompositiion
     *                                  followed by canonical composition.
     *  <li>{@link #COMPOSE_COMPAT} - Unicode compatibility decompositiion
     *                                  follwed by canonical composition.
     *  <li>{@link #DECOMP}         - Unicode canonical decomposition
     *  <li>{@link #DECOMP_COMPAT}  - Unicode compatibility decomposition.
     *  <li>{@link #NO_OP}          - Do nothing but return characters
     *                                  from the underlying input text.
     * </ul>
     *
     * @see #getMode
     */
    public void setMode(Mode newMode) {
        mode = newMode;
        minDecomp = mode.compat() ? 0 : DecompData.MAX_COMPAT;
    }

    /**
     * Return the basic operation performed by this <tt>Normalizer</tt>
     *
     * @see #setMode
     */
    public Mode getMode() {
        return mode;
    }

    /**
     * Set options that affect this <tt>Normalizer</tt>'s operation.
     * Options do not change the basic composition or decomposition operation
     * that is being performed , but they control whether
     * certain optional portions of the operation are done.
     * Currently the only available option is:
     * <p>
     * <ul>
     *   <li>{@link #IGNORE_HANGUL} - Do not decompose Hangul syllables into the Jamo alphabet
     *          and vice-versa.  This option is off by default (<i>i.e.</i> Hangul processing
     *          is enabled) since the Unicode standard specifies that Hangul to Jamo
     *          is a canonical decomposition.  For any of the standard Unicode Normalization
     *          Forms, you should leave this option off.
     * </ul>
     * <p>
     * @param   option  the option whose value is to be set.
     * @param   value   the new setting for the option.  Use <tt>true</tt> to
     *                  turn the option on and <tt>false</tt> to turn it off.
     *
     * @see #getOption
     */
    public void setOption(int option, boolean value) {
        if (option != IGNORE_HANGUL) {
            throw new IllegalArgumentException("Illegal option");
        }
        if (value) {
            options |= option;
        } else {
            options &= (~option);
        }
    }

    /**
     * Determine whether an option is turned on or off.
     * <p>
     * @see #setOption
     */
    public boolean getOption(int option) {
        return (options & option) != 0;
    }

    /**
     * Set the input text over which this <tt>Normalizer</tt> will iterate.
     * The iteration position will be reset to the beginning.
     * <p>
     * @param newText   The new string to be normalized.
     */
    public void setText(String newText) {
        text = new StringCharacterIterator(newText);
        reset();
    }

    /**
     * Set the input text over which this <tt>Normalizer</tt> will iterate.
     * The iteration position will be reset to the beginning.
     * <p>
     * @param newText   The new text to be normalized.
     */
    public void setText(CharacterIterator newText) {
        text = newText;
        reset();
    }


    //-------------------------------------------------------------------------
    // Private utility methods
    //-------------------------------------------------------------------------

    private final char curForward() {
        char ch = text.current();
        if (DEBUG) System.out.println(" curForward returning " + Utility.hex(ch) + ", text index=" + text.getIndex());
        return ch;
    }

    private final char curBackward() {
        char ch = atEnd ? text.current() : text.previous();
        atEnd = false;
        if (DEBUG) System.out.println(" curBackward returning " + Utility.hex(ch) + ", text index=" + text.getIndex());
        return ch;
    }

    static final int doAppend(String source, int offset, StringBuffer dest) {
        int index = offset >>> STR_INDEX_SHIFT;
        int length = offset & STR_LENGTH_MASK;

        if (length == 0) {
            char ch;
            while ((ch = DecompData.contents.charAt(index++)) != 0x0000) {
                dest.append(ch);
                length++;
            }
        } else {
            for (int i = 0; i < length; i++) {
                dest.append(DecompData.contents.charAt(index++));
            }
        }
        return length;
    }


    static final int doInsert(String source, int offset, StringBuffer dest, int pos)
    {
        int index = offset >>> STR_INDEX_SHIFT;
        int length = offset & STR_LENGTH_MASK;

        if (length == 0) {
            char ch;
            while ((ch = DecompData.contents.charAt(index++)) != 0x0000) {
                dest.insert(pos++, ch);
                length++;
            }
        } else {
            for (int i = 0; i < length; i++) {
                dest.insert(pos++, DecompData.contents.charAt(index++));
            }
        }
        return length;
    }

    static final int doReplace(String source, int offset, StringBuffer dest, int pos)
    {
        int index = offset >>> STR_INDEX_SHIFT;
        int length = offset & STR_LENGTH_MASK;

        dest.setCharAt(pos++, DecompData.contents.charAt(index++));
        if (length == 0) {
            char ch;
            while ((ch = DecompData.contents.charAt(index++)) != 0x0000) {
                dest.insert(pos++, ch);
                length++;
            }
        } else {
            for (int i = 1; i < length; i++) {
                dest.insert(pos++, DecompData.contents.charAt(index++));
            }
        }
        return length;
    }

    private void reset() {
        text.setIndex(text.getBeginIndex());
        atEnd = false;
        bufferPos = 0;
        bufferLimit = 0;
    }

    private final void initBuffer() {
        if (buffer == null) {
            buffer = new StringBuffer(10);
        } else {
            buffer.setLength(0);
        }
        clearBuffer();
    }

    private final void clearBuffer() {
        bufferLimit = bufferPos = 0;
    }


    /**
     * Fixes the sorting sequence of non-spacing characters according to
     * their combining class.  The algorithm is listed on p.3-11 in the
     * Unicode Standard 2.0.  The table of combining classes is on p.4-2
     * in the Unicode Standard 2.0.
     * @param result the string to fix.
     */
    private static void fixCanonical(StringBuffer result) {
        int i = result.length() - 1;
        int currentType = getClass(result.charAt(i));
        int lastType;

        for (--i; i >= 0; --i) {
            lastType = currentType;
            currentType = getClass(result.charAt(i));

            //
            // a swap is presumed to be rare (and a double-swap very rare),
            // so don't worry about efficiency here.
            //
            if (currentType > lastType && lastType != DecompData.BASE) {
                // swap characters
                char temp = result.charAt(i);
                result.setCharAt(i, result.charAt(i+1));
                result.setCharAt(i+1, temp);
                // if not at end, backup (one further, to compensate for for-loop)
                if (i < result.length() - 2) {
                    i += 2;
                }
                // reset type, since we swapped.
                currentType = getClass(result.charAt(i));
            }
        }
    }

    //-------------------------------------------------------------------------
    // Hangul / Jamo conversion utilities for internal use
    // See section 3.10 of The Unicode Standard, v 2.0.
    //

    // Package-accessible for use by ComposedCharIter
    static final char HANGUL_BASE   = 0xac00;
    static final char HANGUL_LIMIT  = 0xd7a4;

    private static final char JAMO_LBASE    = 0x1100;
    private static final char JAMO_VBASE    = 0x1161;
    private static final char JAMO_TBASE    = 0x11a7;
    private static final int  JAMO_LCOUNT   = 19;
    private static final int  JAMO_VCOUNT   = 21;
    private static final int  JAMO_TCOUNT   = 28;
    private static final int  JAMO_NCOUNT   = JAMO_VCOUNT * JAMO_TCOUNT;

    /**
     * Convert a single Hangul syllable into one or more Jamo characters.
     *
     * @param conjoin If true, decompose Jamo into conjoining Jamo.
     */
    static int hangulToJamo(char ch, StringBuffer result, int decompLimit) {
        char sIndex  = (char)(ch - HANGUL_BASE);
        char leading = (char)(JAMO_LBASE + sIndex / JAMO_NCOUNT);
        char vowel   = (char)(JAMO_VBASE +
                              (sIndex % JAMO_NCOUNT) / JAMO_TCOUNT);
        char trailing= (char)(JAMO_TBASE + (sIndex % JAMO_TCOUNT));

        int length = 0;

        length += jamoAppend(leading, decompLimit, result);
        length += jamoAppend(vowel, decompLimit, result);
        if (trailing != JAMO_TBASE) {
            length += jamoAppend(trailing, decompLimit, result);
        }
        return length;
    }
    static final int jamoAppend(char ch, int limit, StringBuffer dest) {
        int offset = DecompData.offsets.elementAt(ch);
        if (offset > limit) {
            return doAppend(DecompData.contents, offset, dest);
        } else {
            dest.append(ch);
            return 1;
        }
    }

    static private void jamoToHangul(StringBuffer buffer, int start) {
        int out = 0;
        int limit = buffer.length() - 1;

        int in, l, v, t;

        for (in = start; in < limit; in++) {
            char ch = buffer.charAt(in);

            if ((l = ch - JAMO_LBASE) >= 0 && l < JAMO_LCOUNT
                    && (v = buffer.charAt(in+1) - JAMO_VBASE) >= 0 && v < JAMO_VCOUNT) {
                //
                // We've found a pair of Jamo characters to compose.
                // Snarf the Jamo vowel and see if there's also a trailing char
                //
                in++;   // Snarf the Jamo vowel too.

                t = (in < limit) ? buffer.charAt(in+1) : 0;
                t -= JAMO_TBASE;

                if (t >= 0 && t < JAMO_TCOUNT) {
                    in++;   // Snarf the trailing consonant too
                } else {
                    t = 0;  // No trailing consonant
                }
                buffer.setCharAt(out++, (char)((l*JAMO_VCOUNT + v) * JAMO_TCOUNT
                                               + t + HANGUL_BASE));
            } else {
                buffer.setCharAt(out++, ch);
            }
        }
        while (in < buffer.length()) {
            buffer.setCharAt(out++, buffer.charAt(in++));
        }

        buffer.setLength(out);
    }


    //-------------------------------------------------------------------------
    // Private data
    //-------------------------------------------------------------------------

    private static final boolean DEBUG = false;

    private Mode                mode = DECOMP;
    private int                 options = 0;
    private transient int       minDecomp;

    // The input text and our position in it
    private CharacterIterator   text;
    private boolean             atEnd = false;

    // A buffer for holding intermediate results
    private StringBuffer        buffer = null;
    private int                 bufferPos = 0;
    private int                 bufferLimit = 0;
    private char                currentChar;

    // Another buffer for use during iterative composition
    private static final int    EMPTY = -1;
    private StringBuffer        explodeBuf = null;

    // These must agree with the constants used in NormalizerBuilder
    static final int STR_INDEX_SHIFT = 2;
    static final int STR_LENGTH_MASK = 0x0003;
};
