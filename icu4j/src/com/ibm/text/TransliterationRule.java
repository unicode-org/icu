/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/text/Attic/TransliterationRule.java,v $
 * $Date: 2001/12/03 21:33:58 $
 * $Revision: 1.39 $
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
 * <p>A rule may contain segments in its input string and segment
 * references in its output string.  A segment is a substring of the
 * input pattern, indicated by an offset and limit.  The segment may
 * be in the preceding or following context.  It may not span a
 * context boundary.  A segment reference is a special character in
 * the output string that causes a segment of the input string (not
 * the input pattern) to be copied to the output string.  The range of
 * special characters that represent segment references is defined by
 * RuleBasedTransliterator.Data.
 *
 * <p>Example: The rule "([a-z]) . ([0-9]) > $2 . $1" will change the input
 * string "abc.123" to "ab1.c23".
 *
 * <p>Copyright &copy; IBM Corporation 1999.  All rights reserved.
 *
 * @author Alan Liu
 * @version $RCSfile: TransliterationRule.java,v $ $Revision: 1.39 $ $Date: 2001/12/03 21:33:58 $
 */
class TransliterationRule {

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
     * An array of matcher objects corresponding to the input pattern
     * segments.  If there are no segments this is null.  N.B. This is
     * a UnicodeMatcher for generality, but in practice it is always a
     * StringMatcher.  In the future we may generalize this, but for
     * now we sometimes cast down to StringMatcher.
     */
    UnicodeMatcher[] segments;

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
     * Miscellaneous attributes.
     */
    byte flags;

    /**
     * Flag attributes.
     */
    static final int ANCHOR_START = 1;
    static final int ANCHOR_END   = 2;

    /**
     * An alias pointer to the data for this rule.  The data provides
     * lookup services for matchers and segments.
     */
    private final RuleBasedTransliterator.Data data;

    /**
     * The character at index i, where i < contextStart || i >= contextLimit,
     * is ETHER.  This allows explicit matching by rules and UnicodeSets
     * of text outside the context.  In traditional terms, this allows anchoring
     * at the start and/or end.
     */
    static final char ETHER = '\uFFFF';

    private static final char APOSTROPHE = '\'';
    private static final char BACKSLASH  = '\\';

    private static final String COPYRIGHT =
        "\u00A9 IBM Corporation 1999-2001. All rights reserved.";

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
     * @param segs array of UnicodeMatcher corresponding to input pattern
     * segments, or null if there are none
     * @param anchorStart true if the the rule is anchored on the left to
     * the context start
     * @param anchorEnd true if the rule is anchored on the right to the
     * context limit
     */
    public TransliterationRule(String input,
                               int anteContextPos, int postContextPos,
                               String output,
                               int cursorPos, int cursorOffset,
                               UnicodeMatcher[] segs,
                               boolean anchorStart, boolean anchorEnd,
                               RuleBasedTransliterator.Data theData) {
        data = theData;

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
        this.output = output;
        // We don't validate the segments array.  The caller must
        // guarantee that the segments are well-formed (that is, that
        // all $n references in the output refer to indices of this
        // array, and that no array elements are null).
        this.segments = segs;

        pattern = input;
        flags = 0;
        if (anchorStart) {
            flags |= ANCHOR_START;
        }
        if (anchorEnd) {
            flags |= ANCHOR_END;
        }
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
        return anteContextLength + (((flags & ANCHOR_START) != 0) ? 1 : 0);
    }

    /**
     * Internal method.  Returns 8-bit index value for this rule.
     * This is the low byte of the first character of the key,
     * unless the first character of the key is a set.  If it's a
     * set, or otherwise can match multiple keys, the index value is -1.
     */
   final int getIndexValue() {
        if (anteContextLength == pattern.length()) {
            // A pattern with just ante context {such as foo)>bar} can
            // match any key.
            return -1;
        }
        int c = UTF16.charAt(pattern, anteContextLength);
        return data.lookup(c) == null ? (c & 0xFF) : -1;
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
    final boolean matchesIndexValue(int v) {
        if (anteContextLength == pattern.length()) {
            // A pattern with just ante context {such as foo)>bar} can
            // match any key.
            return true;
        }
        int c = UTF16.charAt(pattern, anteContextLength);
        UnicodeMatcher matcher = data.lookup(c);
        return matcher == null ? (c & 0xFF) == v :
            matcher.matchesIndexValue(v);
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
         *
         * Anchors:  Two patterns that differ only in anchors only
         * mask one another if they are exactly equal, and r2 has
         * all the anchors r1 has (optionally, plus some).  Here Y
         * means the row masks the column, N means it doesn't.
         *
         *         ab   ^ab    ab$  ^ab$
         *   ab    Y     Y     Y     Y
         *  ^ab    N     Y     N     Y
         *   ab$   N     N     Y     Y
         *  ^ab$   N     N     N     Y
         *
         * Post context: {a}b masks ab, but not vice versa, since {a}b
         * matches everything ab matches, and {a}b matches {|a|}b but ab
         * does not.  Pre context is different (a{b} does not align with
         * ab).
         */

        /* LIMITATION of the current mask algorithm: Some rule
         * maskings are currently not detected.  For example,
         * "{Lu}]a>x" masks "A]a>y".  This can be added later. TODO
         */

        int len = pattern.length();
        int left = anteContextLength;
        int left2 = r2.anteContextLength;
        int right = pattern.length() - left;
        int right2 = r2.pattern.length() - left2;

        // TODO Clean this up -- some logic might be combinable with the
        // next statement.

        // Test for anchor masking
        if (left == left2 && right == right2 &&
            keyLength <= r2.keyLength &&
            r2.pattern.regionMatches(0, pattern, 0, len)) {
            // The following boolean logic implements the table above
            return (flags == r2.flags) ||
                (!((flags & ANCHOR_START) != 0) && !((flags & ANCHOR_END) != 0)) ||
                (((r2.flags & ANCHOR_START) != 0) && ((r2.flags & ANCHOR_END) != 0));
        }

        return left <= left2 &&
            (right < right2 ||
             (right == right2 && keyLength <= r2.keyLength)) &&
            r2.pattern.regionMatches(left2 - left, pattern, 0, len);
    }

    static final int posBefore(Replaceable str, int pos) {
        return (pos > 0) ?
            pos - UTF16.getCharCount(UTF16.charAt(str, pos-1)) :
            pos - 1;
    }

    static final int posAfter(Replaceable str, int pos) {
        return (pos >= 0 && pos < str.length()) ?
            pos + UTF16.getCharCount(UTF16.charAt(str, pos)) :
            pos + 1;
    }

    /**
     * Attempt a match and replacement at the given position.  Return
     * the degree of match between this rule and the given text.  The
     * degree of match may be mismatch, a partial match, or a full
     * match.  A mismatch means at least one character of the text
     * does not match the context or key.  A partial match means some
     * context and key characters match, but the text is not long
     * enough to match all of them.  A full match means all context
     * and key characters match.
     *
     * If a full match is obtained, perform a replacement, update pos,
     * and return U_MATCH.  Otherwise both text and pos are unchanged.
     *
     * @param text the text
     * @param pos the position indices
     * @param incremental if TRUE, test for partial matches that may
     * be completed by additional text inserted at pos.limit.
     * @return one of <code>U_MISMATCH</code>,
     * <code>U_PARTIAL_MATCH</code>, or <code>U_MATCH</code>.  If
     * incremental is FALSE then U_PARTIAL_MATCH will not be returned.
     */
    public int matchAndReplace(Replaceable text,
                               Transliterator.Position pos,
                               boolean incremental) {
        // Matching and replacing are done in one method because the
        // replacement operation needs information obtained during the
        // match.  Another way to do this is to have the match method
        // create a match result struct with relevant offsets, and to pass
        // this into the replace method.

        // ============================ MATCH ===========================

        // Reset segment match data
        if (segments != null) {
            for (int i=0; i<segments.length; ++i) {
                ((StringMatcher) segments[i]).resetMatch();
            }
        }

        int lenDelta, keyLimit;
        int[] intRef = new int[1];

        // ------------------------ Ante Context ------------------------

        // A mismatch in the ante context, or with the start anchor,
        // is an outright U_MISMATCH regardless of whether we are
        // incremental or not.
        int oText; // offset into 'text'
        int newStart = 0;
        int minOText;
        int oPattern; // offset into 'pattern'

        // Backup oText by one
        oText = posBefore(text, pos.start);

        // Note (1): We process text in 16-bit code units, rather than
        // 32-bit code points.  This works because stand-ins are
        // always in the BMP and because we are doing a literal match
        // operation, which can be done 16-bits at a time.

        for (oPattern=anteContextLength-1; oPattern>=0; --oPattern) {
            char keyChar = pattern.charAt(oPattern); // See note (1)
            UnicodeMatcher matcher = data.lookup(keyChar);
            if (matcher == null) {
                if (oText >= pos.contextStart &&
                    keyChar == text.charAt(oText)) { // See note (1)
                    --oText;
                } else {
                    return UnicodeMatcher.U_MISMATCH;
                }
            } else {
                // Subtract 1 from contextStart to make it a reverse limit
                intRef[0] = oText;
                if (matcher.matches(text, intRef, pos.contextStart-1, false)
                    != UnicodeMatcher.U_MATCH) {
                    return UnicodeMatcher.U_MISMATCH;
                }
                oText = intRef[0];
            }
        }

        minOText = posAfter(text, oText);

        // ------------------------ Start Anchor ------------------------

        if (((flags & ANCHOR_START) != 0) && oText != posBefore(text, pos.contextStart)) {
            return UnicodeMatcher.U_MISMATCH;
        }

        // -------------------- Key and Post Context --------------------

        oPattern = 0;
        oText = pos.start;
        keyLimit = 0;
        while (oPattern < (pattern.length() - anteContextLength)) {
            if (incremental && oText == pos.limit) {
                // We've reached the limit without a mismatch and
                // without completing our match.
                return UnicodeMatcher.U_PARTIAL_MATCH;
            }

            // It might seem that we could do a check like this here:
            //!if (oText == pos.limit && oPattern < keyLength) {
            //!    // We're still in the pattern key but we're entering the
            //!    // post context.
            // but this won't work if the end of the key is a
            // zero-length matcher, followed by post context: {a b?} c
            // Instead, what we do is proceed with matching as usual
            // so zero-length matchers can work, but restrict the
            // limit to either pos.limit or pos.contextLimit,
            // depending on whether we're in the key or in the post
            // context.

            if (oPattern == keyLength) {
                keyLimit = oText;
            }

            // Restrict the key to match up to pos.limit; the post-context
            // can match up to pos.contextLimit.
            int matchLimit = (oPattern < keyLength) ? pos.limit : pos.contextLimit;

            char keyChar = pattern.charAt(anteContextLength + oPattern++); // See note (1)
            UnicodeMatcher matcher = data.lookup(keyChar);
            if (matcher == null) {
                // Don't need the oText < pos.contextLimit check if
                // incremental is TRUE (because it's done above); do need
                // it otherwise.
                if (oText < matchLimit &&
                    keyChar == text.charAt(oText)) { // See note (1)
                    ++oText;
                } else {
                    return UnicodeMatcher.U_MISMATCH;
                }
            } else {
                intRef[0] = oText;
                int m = matcher.matches(text, intRef, matchLimit, incremental);                
                if (m != UnicodeMatcher.U_MATCH) {
                    return m;
                }
                oText = intRef[0];
            }
            
            // This check rendered superfluous by above use of
            // matchLimit, but kept around for documentation.
            //!if (oText > pos.limit && oPattern < keyLength) {
            //!    // We're still in the pattern key but we've entering the
            //!    // post context.  We must do this check _after_ doing the
            //!    // match in case we have zero-length matchers like /a?/
            //!    // at the end of the key.
            //!    return UnicodeMatcher.U_MISMATCH;
            //!}
        }
        if (oPattern == keyLength) {
            keyLimit = oText;
        }

        // ------------------------- Stop Anchor ------------------------

        if (((flags & ANCHOR_END)) != 0) {
            if (oText != pos.contextLimit) {
                return UnicodeMatcher.U_MISMATCH;
            }
            if (incremental) {
                return UnicodeMatcher.U_PARTIAL_MATCH;
            }
        }

        // =========================== REPLACE ==========================

        // We have a full match.  The key is between pos.start and
        // keyLimit.

        if (segments == null) {
            text.replace(pos.start, keyLimit, output);
            lenDelta = output.length() - (keyLimit - pos.start);
            if (cursorPos >= 0 && cursorPos <= output.length()) {
                // Within the output string, the cursor refers to 16-bit code units
                newStart = pos.start + cursorPos;
            } else {
                newStart = pos.start;
                int n = cursorPos;
                // Outside the output string, cursorPos counts code points
                while (n > 0) {
                    newStart += UTF16.getCharCount(UTF16.charAt(text, newStart));
                    --n;
                }
                while (n < 0) {
                    newStart -= UTF16.getCharCount(UTF16.charAt(text, newStart-1));
                    ++n;
                }
            }
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
            int dest = keyLimit; // copy new text to here
            StringBuffer buf = new StringBuffer();
            int oOutput; // offset into 'output'
            for (oOutput=0; oOutput<output.length(); ) {
                if (oOutput == cursorPos) {
                    // Record the position of the cursor
                    newStart = dest - (keyLimit - pos.start);
                }
                int c = UTF16.charAt(output, oOutput);
                int b = data.lookupSegmentReference(c);
                if (b < 0) {
                    // Accumulate straight (non-segment) text.
                    UTF16.append(buf, c);
                } else {
                    // Insert any accumulated straight text.
                    if (buf.length() > 0) {
                        text.replace(dest, dest, buf.toString());
                        dest += buf.length();
                        buf.setLength(0);
                    }
                    // Copy segment with out-of-band data
                    StringMatcher m = (StringMatcher) segments[b];
                    int start = m.getMatchStart();
                    int limit = m.getMatchLimit();
                    // If there was no match, that means that a quantifier
                    // matched zero-length.  E.g., x (a)* y matched "xy".
                    if (start >= 0) {
                        if (start != limit) {
                            // Adjust indices for segments in post context
                            // for any inserted text between the key and
                            // the post context.
                            if (start >= keyLimit) {
                                start += dest - keyLimit;
                                limit += dest - keyLimit;
                            }
                            text.copy(start, limit, dest);
                            dest += limit - start;
                        }
                    }
                }
                oOutput += UTF16.getCharCount(c);
            }
            // Insert any accumulated straight text.
            if (buf.length() > 0) {
                text.replace(dest, dest, buf.toString());
                dest += buf.length();
            }
            if (oOutput == cursorPos) {
                // Record the position of the cursor
                newStart = dest - (keyLimit - pos.start);
            }
            // Delete the key
            buf.setLength(0);
            text.replace(pos.start, keyLimit, buf.toString());
            lenDelta = dest - keyLimit - (keyLimit - pos.start);
            // Handle cursor in postContext
            if (cursorPos > output.length()) {
                newStart = pos.start + (dest - keyLimit);
                int n = cursorPos - output.length();
                // cursorPos counts code points
                while (n > 0) {
                    newStart += UTF16.getCharCount(UTF16.charAt(text, newStart));
                    n--;
                }
            }
        }

        oText += lenDelta;
        pos.limit += lenDelta;
        pos.contextLimit += lenDelta;
        // Restrict new value of start to [minOText, min(oText, pos.limit)].
        pos.start = Math.max(minOText, Math.min(Math.min(oText, pos.limit), newStart));
        return UnicodeMatcher.U_MATCH;
    }

    /**
     * Append a character to a rule that is being built up.  To flush
     * the quoteBuf to rule, make one final call with isLiteral == true.
     * If there is no final character, pass in (int)-1 as c.
     * @param rule the string to append the character to
     * @param c the character to append, or (int)-1 if none.
     * @param isLiteral if true, then the given character should not be
     * quoted or escaped.  Usually this means it is a syntactic element
     * such as > or $
     * @param escapeUnprintable if true, then unprintable characters
     * should be escaped using <backslash>uxxxx or <backslash>Uxxxxxxxx.  These escapes will
     * appear outside of quotes.
     * @param quoteBuf a buffer which is used to build up quoted
     * substrings.  The caller should initially supply an empty buffer,
     * and thereafter should not modify the buffer.  The buffer should be
     * cleared out by, at the end, calling this method with a literal
     * character.
     */
    static void appendToRule(StringBuffer rule,
                             int c,
                             boolean isLiteral,
                             boolean escapeUnprintable,
                             StringBuffer quoteBuf) {
        // If we are escaping unprintables, then escape them outside
        // quotes.  <backslash>u and <backslash>U are not recognized within quotes.  The same
        // logic applies to literals, but literals are never escaped.
        if (isLiteral ||
            (escapeUnprintable && Utility.isUnprintable(c))) {
            if (quoteBuf.length() > 0) {
                // We prefer backslash APOSTROPHE to double APOSTROPHE
                // (more readable, less similar to ") so if there are
                // double APOSTROPHEs at the ends, we pull them outside
                // of the quote.

                // If the first thing in the quoteBuf is APOSTROPHE
                // (doubled) then pull it out.
                while (quoteBuf.length() >= 2 &&
                       quoteBuf.charAt(0) == APOSTROPHE &&
                       quoteBuf.charAt(1) == APOSTROPHE) {
                    rule.append(BACKSLASH).append(APOSTROPHE);
                    quoteBuf.delete(0, 2);
                }
                // If the last thing in the quoteBuf is APOSTROPHE
                // (doubled) then remove and count it and add it after.
                int trailingCount = 0;
                while (quoteBuf.length() >= 2 &&
                       quoteBuf.charAt(quoteBuf.length()-2) == APOSTROPHE &&
                       quoteBuf.charAt(quoteBuf.length()-1) == APOSTROPHE) {
                    quoteBuf.setLength(quoteBuf.length()-2);
                    ++trailingCount;
                }
                if (quoteBuf.length() > 0) {
                    rule.append(APOSTROPHE);
                    rule.append(quoteBuf);
                    rule.append(APOSTROPHE);
                    quoteBuf.setLength(0);
                }
                while (trailingCount-- > 0) {
                    rule.append(BACKSLASH).append(APOSTROPHE);
                }
            }
            if (c != -1) {
                if (!escapeUnprintable || !Utility.escapeUnprintable(rule, c)) {
                    UTF16.append(rule, c);
                }
            }
        }

        // Escape ' and '\' and don't begin a quote just for them
        else if (quoteBuf.length() == 0 &&
                 (c == APOSTROPHE || c == BACKSLASH)) {
            rule.append(BACKSLASH).append((char)c);
        }

        // Specials (printable ascii that isn't [0-9a-zA-Z]) and
        // whitespace need quoting.  Also append stuff to quotes if we are
        // building up a quoted substring already.
        else if (quoteBuf.length() > 0 ||
                 (c >= 0x0021 && c <= 0x007E &&
                  !((c >= 0x0030/*'0'*/ && c <= 0x0039/*'9'*/) ||
                    (c >= 0x0041/*'A'*/ && c <= 0x005A/*'Z'*/) ||
                    (c >= 0x0061/*'a'*/ && c <= 0x007A/*'z'*/))) ||
                 UCharacter.isWhitespace(c)) {
            UTF16.append(quoteBuf, c);
            // Double ' within a quote
            if (c == APOSTROPHE) {
                quoteBuf.append((char)c);
            }
        }

        // Otherwise just append
        else {
            UTF16.append(rule, c);
        }

        //System.out.println("rule=" + rule.toString() + " qb=" + quoteBuf.toString());
    }

    static final void appendToRule(StringBuffer rule,
                                   String text,
                                   boolean isLiteral,
                                   boolean escapeUnprintable,
                                   StringBuffer quoteBuf) {
        for (int i=0; i<text.length(); ++i) {
            // Okay to process in 16-bit code units here
            appendToRule(rule, text.charAt(i), isLiteral, escapeUnprintable, quoteBuf);
        }
    }

    static private int[] POW10 = {1, 10, 100, 1000, 10000, 100000, 1000000,
                                  10000000, 100000000, 1000000000};

    /**
     * Create a source string that represents this rule.  Append it to the
     * given string.
     */
    public String toRule(boolean escapeUnprintable) {
        int i;

        StringBuffer rule = new StringBuffer();

        // Accumulate special characters (and non-specials following them)
        // into quoteBuf.  Append quoteBuf, within single quotes, when
        // a non-quoted element must be inserted.
        StringBuffer quoteBuf = new StringBuffer();

        // Do not emit the braces '{' '}' around the pattern if there
        // is neither anteContext nor postContext.
        boolean emitBraces =
            (anteContextLength != 0) || (keyLength != pattern.length());

        // Emit start anchor
        if ((flags & ANCHOR_START) != 0) {
            rule.append('^');
        }

        // Emit the input pattern
        for (i=0; i<pattern.length(); ++i) {
            if (emitBraces && i == anteContextLength) {
                appendToRule(rule, '{', true, escapeUnprintable, quoteBuf);
            }

            if (emitBraces && i == (anteContextLength + keyLength)) {
                appendToRule(rule, '}', true, escapeUnprintable, quoteBuf);
            }

            char c = pattern.charAt(i); // Ok to use 16-bits here
            UnicodeMatcher matcher = data.lookup(c);
            if (matcher == null) {
                appendToRule(rule, c, false, escapeUnprintable, quoteBuf);
            } else {
                appendToRule(rule, matcher.toPattern(escapeUnprintable),
                              true, escapeUnprintable, quoteBuf);
            }
        }

        if (emitBraces && i == (anteContextLength + keyLength)) {
            appendToRule(rule, '}', true, escapeUnprintable, quoteBuf);
        }

        // Emit end anchor
        if ((flags & ANCHOR_END) != 0) {
            rule.append('$');
        }

        appendToRule(rule, " > ", true, escapeUnprintable, quoteBuf);

        // Emit the output pattern

        // Handle a cursor preceding the output
        int cursor = cursorPos;
        if (cursor < 0) {
            while (cursor++ < 0) {
                appendToRule(rule, '@', true, escapeUnprintable, quoteBuf);
            }
            // Fall through and append '|' below
        }

        for (i=0; i<output.length(); ++i) {
            if (i == cursor) {
                appendToRule(rule, '|', true, escapeUnprintable, quoteBuf);
            }
            char c = output.charAt(i); // Ok to use 16-bits here
            int seg = data.lookupSegmentReference(c);
            if (seg < 0) {
                appendToRule(rule, c, false, escapeUnprintable, quoteBuf);
            } else {
                ++seg; // make 1-based
                appendToRule(rule, 0x20, true, escapeUnprintable, quoteBuf);
                rule.append('$');
                boolean show = false; // true if we should display digits
                for (int p=9; p>=0; --p) {
                    int d = seg / POW10[p];
                    seg -= d * POW10[p];
                    if (d != 0 || p == 0) {
                        show = true;
                    }
                    if (show) {
                        rule.append((char)(48+d));
                    }
                }
                rule.append(' ');
            }
        }

        // Handle a cursor after the output.  Use > rather than >= because
        // if cursor == output.length() it is at the end of the output,
        // which is the default position, so we need not emit it.
        if (cursor > output.length()) {
            cursor -= output.length();
            while (cursor-- > 0) {
                appendToRule(rule, '@', true, escapeUnprintable, quoteBuf);
            }
            appendToRule(rule, '|', true, escapeUnprintable, quoteBuf);
        }

        appendToRule(rule, ';', true, escapeUnprintable, quoteBuf);

        return rule.toString();
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
     * Union the set of all characters that may be modified by this rule
     * into the given set.
     */
    UnicodeSet getSourceSet(UnicodeSet toUnionTo) {
        int limit = anteContextLength + keyLength;
        for (int i=anteContextLength; i<limit; ) {
            int ch = UTF16.charAt(pattern, i);
            i += UTF16.getCharCount(ch);
            UnicodeMatcher matcher = data.lookup(ch);
            if (matcher == null) {
                toUnionTo.add(ch);
            } else {
                matcher.getMatchSet(toUnionTo);
            }
        }
        return toUnionTo;
    }
}

/**
 * $Log: TransliterationRule.java,v $
 * Revision 1.39  2001/12/03 21:33:58  alan
 * jitterbug 1373: more fixes to support supplementals
 *
 * Revision 1.38  2001/11/30 22:27:29  alan
 * jitterbug 1560: fix double increment bug in getSourceSet
 *
 * Revision 1.37  2001/11/29 22:31:18  alan
 * jitterbug 1560: add source-set methods and TransliteratorUtility class
 *
 * Revision 1.36  2001/11/21 22:21:45  alan
 * jitterbug 1533: incorporate Mark's review comments; move escape handling methods to Utility
 *
 * Revision 1.35  2001/11/02 17:46:05  alan
 * jitterbug 1426: eliminate NOP call to copy()
 *
 * Revision 1.34  2001/10/30 18:04:08  alan
 * jitterbug 1406: make quantified segments behave like perl counterparts
 *
 * Revision 1.33  2001/10/25 23:22:15  alan
 * jitterbug 73: changes to support zero-length matchers at end of key
 *
 * Revision 1.32  2001/10/25 22:42:24  alan
 * jitterbug 73: use int for index values to avoid signedness problems
 *
 * Revision 1.31  2001/10/18 23:02:32  alan
 * jitterbug 60: fix handling of anchors in toRule
 *
 * Revision 1.30  2001/10/04 22:33:53  alan
 * jitterbug 69: minor fix to incremental RBT code
 *
 * Revision 1.29  2001/10/03 00:14:23  alan
 * jitterbug 73: finish quantifier and supplemental char support
 *
 * Revision 1.28  2001/09/26 18:00:06  alan
 * jitterbug 67: sync parser with icu4c, allow unlimited, nested segments
 *
 * Revision 1.27  2001/09/19 17:43:38  alan
 * jitterbug 60: initial implementation of toRules()
 *
 * Revision 1.26  2001/06/29 22:35:41  alan4j
 * Implement Any-Upper Any-Lower and Any-Title transliterators
 *
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
