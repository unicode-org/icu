/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/text/Attic/TransliterationRule.java,v $ 
 * $Date: 2000/11/29 19:12:32 $ 
 * $Revision: 1.25 $
 *
 *****************************************************************************************
 */
package com.ibm.text;

import com.ibm.util.Utility;

/**
 * A transliteration rule used by
 * <code>RuleBasedTransliterator</code>.
 * <code>TransliterationRule</code> is an immutable object.
 *
 * <p>A rule consists of an input pattern and an output string.  When
 * the input pattern is matched, the output string is emitted.  The
 * input pattern consists of zero or more characters which are matched
 * exactly (the key) and optional context.  Context must match if it
 * is specified.  Context may be specified before the key, after the
 * key, or both.  The key, preceding context, and following context
 * may contain variables.  Variables represent a set of Unicode
 * characters, such as the letters <i>a</i> through <i>z</i>.
 * Variables are detected by looking up each character in a supplied
 * variable list to see if it has been so defined. 
 *
 * <p>A rule may contain segments in its input string and segment references in
 * its output string.  A segment is a substring of the input pattern, indicated
 * by an offset and limit.  The segment may span the preceding or following
 * context.  A segment reference is a special character in the output string
 * that causes a segment of the input string (not the input pattern) to be
 * copied to the output string.  The range of special characters that represent
 * segment references is defined by RuleBasedTransliterator.Data.
 *
 * <p>Example: The rule "([a-z]) . ([0-9]) > $2 . $1" will change the input
 * string "abc.123" to "ab1.c23".
 *
 * <p>Copyright &copy; IBM Corporation 1999.  All rights reserved.
 *
 * @author Alan Liu
 * @version $RCSfile: TransliterationRule.java,v $ $Revision: 1.25 $ $Date: 2000/11/29 19:12:32 $
 */
class TransliterationRule {
    /**
     * Constant returned by <code>getMatchDegree()</code> indicating a mismatch
     * between the text and this rule.  One or more characters of the context or
     * key do not match the text.
     * @see #getMatchDegree
     */
    public static final int MISMATCH      = 0;

    /**
     * Constant returned by <code>getMatchDegree()</code> indicating a partial
     * match between the text and this rule.  All characters of the text match
     * the corresponding context or key, but more characters are required for a
     * complete match.  There are some key or context characters at the end of
     * the pattern that remain unmatched because the text isn't long enough.
     * @see #getMatchDegree
     */
    public static final int PARTIAL_MATCH = 1;

    /**
     * Constant returned by <code>getMatchDegree()</code> indicating a complete
     * match between the text and this rule.  The text matches all context and
     * key characters.
     * @see #getMatchDegree
     */
    public static final int FULL_MATCH    = 2;

    /**
     * The string that must be matched, consisting of the anteContext, key,
     * and postContext, concatenated together, in that order.  Some components
     * may be empty (zero length).
     * @see anteContextLength
     * @see keyLength
     */
    private String pattern;

    /**
     * The string that is emitted if the key, anteContext, and postContext
     * are matched.
     */
    private String output;

    /**
     * Array of segments.  These are segments of the input string that may be
     * referenced and appear in the output string.  Each segment is stored as an
     * offset, limit pair.  Segments are referenced by a 1-based index;
     * reference i thus includes characters at offset segments[2*i-2] to
     * segments[2*i-1]-1 in the pattern string.
     *
     * In the output string, a segment reference is indicated by a character in
     * a special range, as defined by RuleBasedTransliterator.Data.
     *
     * Most rules have no segments, in which case segments is null, and the
     * output string need not be checked for segment reference characters.
     */
    private int[] segments;

    /**
     * The length of the string that must match before the key.  If
     * zero, then there is no matching requirement before the key.
     * Substring [0,anteContextLength) of pattern is the anteContext.
     */
    private int anteContextLength;

    /**
     * The length of the key.  Substring [anteContextLength,
     * anteContextLength + keyLength) is the key.
     */
    private int keyLength;

    /**
     * The position of the cursor after emitting the output string, from 0 to
     * output.length().  For most rules with no special cursor specification,
     * the cursorPos is output.length().
     */
    private int cursorPos;

    /**
     * The character at index i, where i < contextStart || i >= contextLimit,
     * is ETHER.  This allows explicit matching by rules and UnicodeSets
     * of text outside the context.  In traditional terms, this allows anchoring
     * at the start and/or end.
     */
    static final char ETHER = '\uFFFF';

    private static final String COPYRIGHT =
        "\u00A9 IBM Corporation 1999. All rights reserved.";

    /**
     * Construct a new rule with the given input, output text, and other
     * attributes.  A cursor position may be specified for the output text.
     * @param input input string, including key and optional ante and
     * post context
     * @param anteContextPos offset into input to end of ante context, or -1 if
     * none.  Must be <= input.length() if not -1.
     * @param postContextPos offset into input to start of post context, or -1
     * if none.  Must be <= input.length() if not -1, and must be >=
     * anteContextPos.
     * @param output output string
     * @param cursorPos offset into output at which cursor is located, or -1 if
     * none.  If less than zero, then the cursor is placed after the
     * <code>output</code>; that is, -1 is equivalent to
     * <code>output.length()</code>.  If greater than
     * <code>output.length()</code> then an exception is thrown.
     * @param cursorOffset an offset to be added to cursorPos to position the
     * cursor either in the ante context, if < 0, or in the post context, if >
     * 0.  For example, the rule "abc{def} > | @@@ xyz;" changes "def" to
     * "xyz" and moves the cursor to before "a".  It would have a cursorOffset
     * of -3.
     * @param segs array of 2n integers.  Each of n pairs consists of offset,
     * limit for a segment of the input string.  Characters in the output string
     * refer to these segments if they are in a special range determined by the
     * associated RuleBasedTransliterator.Data object.  May be null if there are
     * no segments.  The caller is responsible for validating that segments
     * are well-formed.
     * @param anchorStart true if the the rule is anchored on the left to
     * the context start
     * @param anchorEnd true if the rule is anchored on the right to the
     * context limit
     */
    public TransliterationRule(String input,
                               int anteContextPos, int postContextPos,
                               String output,
                               int cursorPos, int cursorOffset,
                               int[] segs,
                               boolean anchorStart, boolean anchorEnd) {
        // Do range checks only when warranted to save time
        if (anteContextPos < 0) {
            anteContextLength = 0;
        } else {
            if (anteContextPos > input.length()) {
                throw new IllegalArgumentException("Invalid ante context");
            }
            anteContextLength = anteContextPos;
        }
        if (postContextPos < 0) {
            keyLength = input.length() - anteContextLength;
        } else {
            if (postContextPos < anteContextLength ||
                postContextPos > input.length()) {
                throw new IllegalArgumentException("Invalid post context");
            }
            keyLength = postContextPos - anteContextLength;
        }
        if (cursorPos < 0) {
            cursorPos = output.length();
        }
        if (cursorPos > output.length()) {
            throw new IllegalArgumentException("Invalid cursor position");
        }
        this.cursorPos = cursorPos + cursorOffset;
        pattern = input;
        this.output = output;
        // We don't validate the segments array.  The caller must
        // guarantee that the segments are well-formed.
        this.segments = segs;

        // Implement anchors by inserting an ETHER character on the
        // left or right.  If on the left, then the indices must be
        // incremented.  If on the right, no index change is
        // necessary.
        if (anchorStart || anchorEnd) {
            StringBuffer buf = new StringBuffer();
            if (anchorStart) {
                buf.append(ETHER);
                ++anteContextLength;
                ++cursorPos;
                // Adjust segment offsets
                if (segments != null) {
                    for (int i=0; i<segments.length; ++i) {
                        ++segments[i];
                    }
                }
            }
            buf.append(input);
            if (anchorEnd) {
                buf.append(ETHER);
            }
            pattern = buf.toString();
        }
    }

    /**
     * Construct a new rule with the given input, output text, and other
     * attributes.  A cursor position may be specified for the output text.
     * @param input input string, including key and optional ante and
     * post context
     * @param anteContextPos offset into input to end of ante context, or -1 if
     * none.  Must be <= input.length() if not -1.
     * @param postContextPos offset into input to start of post context, or -1
     * if none.  Must be <= input.length() if not -1, and must be >=
     * anteContextPos.
     * @param output output string
     * @param cursorPos offset into output at which cursor is located, or -1 if
     * none.  If less than zero, then the cursor is placed after the
     * <code>output</code>; that is, -1 is equivalent to
     * <code>output.length()</code>.  If greater than
     * <code>output.length()</code> then an exception is thrown.
     */
    public TransliterationRule(String input,
                               int anteContextPos, int postContextPos,
                               String output,
                               int cursorPos) {
        this(input, anteContextPos, postContextPos,
             output, cursorPos, 0, null, false, false);
    }

    /**
     * Return the position of the cursor within the output string.
     * @return a value from 0 to <code>getOutput().length()</code>, inclusive.
     */
    public int getCursorPos() {
        return cursorPos;
    }

    /**
     * Return the preceding context length.  This method is needed to
     * support the <code>Transliterator</code> method
     * <code>getMaximumContextLength()</code>.
     */
    public int getAnteContextLength() {
        return anteContextLength;
    }

    /**
     * Internal method.  Returns 8-bit index value for this rule.
     * This is the low byte of the first character of the key,
     * unless the first character of the key is a set.  If it's a
     * set, or otherwise can match multiple keys, the index value is -1.
     */
    final int getIndexValue(RuleBasedTransliterator.Data variables) {
        if (anteContextLength == pattern.length()) {
            // A pattern with just ante context {such as foo)>bar} can
            // match any key.
            return -1;
        }
        char c = pattern.charAt(anteContextLength);
        return variables.lookupSet(c) == null ? (c & 0xFF) : -1;
    }

    /**
     * Do a replacement of the input pattern with the output text in
     * the given string, at the given offset.  This method assumes
     * that a match has already been found in the given text at the
     * given position.
     * @param text the text containing the substring to be replaced
     * @param offset the offset into the text at which the pattern
     * matches.  This is the offset to the point after the ante
     * context, if any, and before the match string and any post
     * context.
     * @param data the RuleBasedTransliterator.Data object specifying
     * context for this transliterator.
     * @return the change in the length of the text
     */
    public int replace(Replaceable text, int offset,
                       RuleBasedTransliterator.Data data) {
        if (segments == null) {
            text.replace(offset, offset + keyLength, output);
            return output.length() - keyLength;
        } else {
            /* When there are segments to be copied, use the Replaceable.copy()
             * API in order to retain out-of-band data.  Copy everything to the
             * point after the key, then delete the key.  That is, copy things
             * into offset + keyLength, then replace offset .. offset +
             * keyLength with the empty string.
             *
             * Minimize the number of calls to Replaceable.replace() and
             * Replaceable.copy().
             */
            int textStart = offset - anteContextLength;
            int dest = offset + keyLength; // copy new text to here
            StringBuffer buf = new StringBuffer();
            for (int i=0; i<output.length(); ++i) {
                char c = output.charAt(i);
                int b = data.lookupSegmentReference(c);
                if (b < 0) {
                    // Accumulate straight (non-segment) text.
                    buf.append(c);
                } else {
                    // Insert any accumulated straight text.
                    if (buf.length() > 0) {
                        text.replace(dest, dest, buf.toString());
                        dest += buf.length();
                        buf.setLength(0);
                    }
                    // Copy segment with out-of-band data
                    b *= 2;
                    text.copy(textStart + segments[b],
                              textStart + segments[b+1], dest);
                    dest += segments[b+1] - segments[b];
                }
                
            }
            // Insert any accumulated straight text.
            if (buf.length() > 0) {
                text.replace(dest, dest, buf.toString());
                dest += buf.length();
            }
            // Delete the key
            text.replace(offset, offset + keyLength, "");
            return dest - (offset + keyLength) - keyLength;
        }
    }

    /**
     * Internal method.  Returns true if this rule matches the given
     * index value.  The index value is an 8-bit integer, 0..255,
     * representing the low byte of the first character of the key.
     * It matches this rule if it matches the first character of the
     * key, or if the first character of the key is a set, and the set
     * contains any character with a low byte equal to the index
     * value.  If the rule contains only ante context, as in foo)>bar,
     * then it will match any key.
     */
    final boolean matchesIndexValue(int v, RuleBasedTransliterator.Data variables) {
        if (anteContextLength == pattern.length()) {
            // A pattern with just ante context {such as foo)>bar} can
            // match any key.
            return true;
        }
        char c = pattern.charAt(anteContextLength);
        UnicodeSet set = variables.lookupSet(c);
        return set == null ? (c & 0xFF) == v : set.containsIndexValue(v);
    }

    /**
     * Return true if this rule masks another rule.  If r1 masks r2 then
     * r1 matches any input string that r2 matches.  If r1 masks r2 and r2 masks
     * r1 then r1 == r2.  Examples: "a>x" masks "ab>y".  "a>x" masks "a[b]>y".
     * "[c]a>x" masks "[dc]a>y".
     */
    public boolean masks(TransliterationRule r2) {
        /* Rule r1 masks rule r2 if the string formed of the
         * antecontext, key, and postcontext overlaps in the following
         * way:
         *
         * r1:      aakkkpppp
         * r2:     aaakkkkkpppp
         *            ^
         * 
         * The strings must be aligned at the first character of the
         * key.  The length of r1 to the left of the alignment point
         * must be <= the length of r2 to the left; ditto for the
         * right.  The characters of r1 must equal (or be a superset
         * of) the corresponding characters of r2.  The superset
         * operation should be performed to check for UnicodeSet
         * masking.
         */

        /* LIMITATION of the current mask algorithm: Some rule
         * maskings are currently not detected.  For example,
         * "{Lu}]a>x" masks "A]a>y".  This can be added later. TODO
         */

        int left = anteContextLength;
        int left2 = r2.anteContextLength;
        int right = pattern.length() - left;
        int right2 = r2.pattern.length() - left2;
        return left <= left2 && right <= right2 &&
            r2.pattern.substring(left2 - left).startsWith(pattern);
    }

    /**
     * Return a string representation of this object.
     * @return string representation of this object
     */
    public String toString() {
        return getClass().getName() + '{'
            + Utility.escape((anteContextLength > 0 ? (pattern.substring(0, anteContextLength) +
                                              " {") : "")
                     + pattern.substring(anteContextLength, anteContextLength + keyLength)
                     + (anteContextLength + keyLength < pattern.length() ?
                        ("} " + pattern.substring(anteContextLength + keyLength)) : "")
                     + " > "
                     + (cursorPos < output.length()
                        ? (output.substring(0, cursorPos) + '|' + output.substring(cursorPos))
                        : output))
            + '}';
    }

    /**
     * Return true if this rule matches the given text.
     * @param text the text, both translated and untranslated
     * @param start the beginning index, inclusive; <code>0 <= start
     * <= limit</code>.
     * @param limit the ending index, exclusive; <code>start <= limit
     * <= text.length()</code>.
     * @param cursor position at which to translate next, representing offset
     * into text.  This value must be between <code>start</code> and
     * <code>limit</code>.
     * @param filter the filter.  Any character for which
     * <tt>filter.contains()</tt> returns <tt>false</tt> will not be
     * altered by this transliterator.  If <tt>filter</tt> is
     * <tt>null</tt> then no filtering is applied.
     */
    public final boolean matches(Replaceable text,
                                 Transliterator.Position pos,
                                 RuleBasedTransliterator.Data variables,
                                 UnicodeFilter filter) {
        // Match anteContext, key, and postContext
        int cursor = pos.start - anteContextLength;
        //[ANCHOR]if (cursor < pos.contextStart
        //[ANCHOR]    || (cursor + pattern.length()) > pos.contextLimit) {
        //[ANCHOR]    return false;
        //[ANCHOR]}
        // Quick length check; this is a performance win for long rules.
        // Widen by one (on both sides) to allow anchor matching.
        if (cursor < (pos.contextStart - 1)
            || (cursor + pattern.length()) > (pos.contextLimit + 1)) {
            return false;
        }
        for (int i=0; i<pattern.length(); ++i, ++cursor) {
            //[ANCHOR]if (!charMatches(pattern.charAt(i), text.charAt(cursor),
            //[ANCHOR]                 variables, filter)) {
            if (!charMatches(pattern.charAt(i), text, cursor, pos,
                             variables, filter)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Return the degree of match between this rule and the given text.  The
     * degree of match may be mismatch, a partial match, or a full match.  A
     * mismatch means at least one character of the text does not match the
     * context or key.  A partial match means some context and key characters
     * match, but the text is not long enough to match all of them.  A full
     * match means all context and key characters match.
     * @param text the text, both translated and untranslated
     * @param start the beginning index, inclusive; <code>0 <= start
     * <= limit</code>.
     * @param limit the ending index, exclusive; <code>start <= limit
     * <= text.length()</code>.
     * @param cursor position at which to translate next, representing offset
     * into text.  This value must be between <code>start</code> and
     * <code>limit</code>.
     * @param filter the filter.  Any character for which
     * <tt>filter.contains()</tt> returns <tt>false</tt> will not be
     * altered by this transliterator.  If <tt>filter</tt> is
     * <tt>null</tt> then no filtering is applied.
     * @return one of <code>MISMATCH</code>, <code>PARTIAL_MATCH</code>, or
     * <code>FULL_MATCH</code>.
     * @see #MISMATCH
     * @see #PARTIAL_MATCH
     * @see #FULL_MATCH
     */
    public int getMatchDegree(Replaceable text,
                              Transliterator.Position pos,
                              RuleBasedTransliterator.Data variables,
                              UnicodeFilter filter) {
        int len = getRegionMatchLength(text, pos, variables, filter);
        return len < anteContextLength ? MISMATCH :
            (len < pattern.length() ? PARTIAL_MATCH : FULL_MATCH);
    }

    /**
     * Return the number of characters of the text that match this rule.  If
     * there is a mismatch, return -1.  If the text is not long enough to match
     * any characters, return 0.
     * @param text the text, both translated and untranslated
     * @param start the beginning index, inclusive; <code>0 <= start
     * <= limit</code>.
     * @param limit the ending index, exclusive; <code>start <= limit
     * <= text.length()</code>.
     * @param cursor position at which to translate next, representing offset
     * into text.  This value must be between <code>start</code> and
     * <code>limit</code>.
     * @param variables a dictionary of variables mapping <code>Character</code>
     * to <code>UnicodeSet</code>
     * @param filter the filter.  Any character for which
     * <tt>filter.contains()</tt> returns <tt>false</tt> will not be
     * altered by this transliterator.  If <tt>filter</tt> is
     * <tt>null</tt> then no filtering is applied.
     * @return -1 if there is a mismatch, 0 if the text is not long enough to
     * match any characters, otherwise the number of characters of text that
     * match this rule.
     */
    protected int getRegionMatchLength(Replaceable text,
                                       Transliterator.Position pos,
                                       RuleBasedTransliterator.Data variables,
                                       UnicodeFilter filter) {
        int cursor = pos.start - anteContextLength;
        //[ANCHOR]if (cursor < pos.contextStart) {
        //[ANCHOR]    return -1;
        //[ANCHOR]}
        // Quick length check; this is a performance win for long rules.
        // Widen by one to allow anchor matching.
        if (cursor < (pos.contextStart - 1)) {
            return -1;
        }
        int i;
        for (i=0; i<pattern.length() && cursor<pos.contextLimit; ++i, ++cursor) {
            //[ANCHOR]if (!charMatches(pattern.charAt(i), text.charAt(cursor),
            //[ANCHOR]                 variables, filter)) {
            if (!charMatches(pattern.charAt(i), text, cursor, pos,
                             variables, filter)) {
                return -1;
            }
        }
        return i;
    }

    /**
     * Return true if the given key matches the given text.  This method
     * accounts for the fact that the key character may represent a character
     * set.  Note that the key and text characters may not be interchanged
     * without altering the results.
     * @param keyChar a character in the match key
     * @param textChar a character in the text being transliterated
     * @param variables a dictionary of variables mapping <code>Character</code>
     * to <code>UnicodeSet</code>
     * @param filter the filter.  Any character for which
     * <tt>filter.contains()</tt> returns <tt>false</tt> will not be
     * altered by this transliterator.  If <tt>filter</tt> is
     * <tt>null</tt> then no filtering is applied.
     */
    protected static final boolean charMatches(char keyChar, Replaceable text,
                                               int index, Transliterator.Position pos,
                                               RuleBasedTransliterator.Data variables,
                                               UnicodeFilter filter) {
        UnicodeSet set = null;
        char textChar = (index >= pos.contextStart && index < pos.contextLimit)
            ? text.charAt(index) : ETHER;
        return (filter == null || filter.contains(textChar)) &&
            (((set = variables.lookupSet(keyChar)) == null) ?
             keyChar == textChar : set.contains(textChar));
    }

    /**
     * Return true if the given key matches the given text.  This method
     * accounts for the fact that the key character may represent a character
     * set.  Note that the key and text characters may not be interchanged
     * without altering the results.
     * @param keyChar a character in the match key
     * @param textChar a character in the text being transliterated
     * @param variables a dictionary of variables mapping <code>Character</code>
     * to <code>UnicodeSet</code>
     * @param filter the filter.  Any character for which
     * <tt>filter.contains()</tt> returns <tt>false</tt> will not be
     * altered by this transliterator.  If <tt>filter</tt> is
     * <tt>null</tt> then no filtering is applied.
     */
    protected static final boolean charMatches(char keyChar, char textChar,
                                               RuleBasedTransliterator.Data variables,
                                               UnicodeFilter filter) {
        UnicodeSet set = null;
        return (filter == null || filter.contains(textChar)) &&
            (((set = variables.lookupSet(keyChar)) == null) ?
             keyChar == textChar : set.contains(textChar));
    }
}

/**
 * $Log: TransliterationRule.java,v $
 * Revision 1.25  2000/11/29 19:12:32  alan4j
 * Update docs
 *
 * Revision 1.24  2000/08/30 20:40:30  alan4j
 * Implement anchors.
 *
 * Revision 1.23  2000/06/29 21:59:23  alan4j
 * Fix handling of Transliterator.Position fields
 *
 * Revision 1.22  2000/05/18 21:37:19  alan
 * Update docs
 *
 * Revision 1.21  2000/04/28 01:22:01  alan
 * Update syntax displayed by toString
 *
 * Revision 1.20  2000/04/25 17:17:37  alan
 * Add Replaceable.copy to retain out-of-band info during reordering.
 *
 * Revision 1.19  2000/04/25 01:42:58  alan
 * Allow arbitrary length variable values. Clean up Data API. Update javadocs.
 *
 * Revision 1.18  2000/04/22 01:25:10  alan
 * Add support for cursor positioner '@'; update javadoc
 *
 * Revision 1.17  2000/04/21 21:16:40  alan
 * Modify rule syntax
 *
 * Revision 1.16  2000/04/19 16:34:18  alan
 * Add segment support.
 *
 * Revision 1.15  2000/04/12 20:17:45  alan
 * Delegate replace operation to rule object
 *
 * Revision 1.14  2000/03/10 04:07:24  johnf
 * Copyright update
 *
 * Revision 1.13  2000/02/10 07:36:25  johnf
 * fixed imports for com.ibm.util.Utility
 *
 * Revision 1.12  2000/02/03 18:11:19  Alan
 * Use array rather than hashtable for char-to-set map
 *
 * Revision 1.11  2000/01/27 18:59:19  Alan
 * Use Position rather than int[] and move all subclass overrides to one method (handleTransliterate)
 *
 * Revision 1.10  2000/01/18 20:36:17  Alan
 * Make UnicodeSet inherit from UnicodeFilter
 *
 * Revision 1.9  2000/01/18 02:38:55  Alan
 * Fix filtering bug.
 *
 * Revision 1.8  2000/01/13 23:53:23  Alan
 * Fix bugs found during ICU port
 *
 * Revision 1.7  2000/01/11 04:12:06  Alan
 * Cleanup, embellish comments
 *
 * Revision 1.6  2000/01/11 02:25:03  Alan
 * Rewrite UnicodeSet and RBT parsers for better performance and new syntax
 *
 * Revision 1.5  2000/01/04 21:43:57  Alan
 * Add rule indexing, and move masking check to TransliterationRuleSet.
 *
 * Revision 1.4  1999/12/22 01:40:54  Alan
 * Consolidate rule pattern anteContext, key, and postContext into one string.
 *
 * Revision 1.3  1999/12/22 01:05:54  Alan
 * Improve masking checking; turn it off by default, for better performance
 *
 * Revision 1.2  1999/12/21 23:58:44  Alan
 * Detect a>x masking a>y
 */
