package com.ibm.text;

import java.util.Dictionary;

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
 * <p>Copyright &copy; IBM Corporation 1999.  All rights reserved.
 *
 * @author Alan Liu
 * @version $RCSfile: TransliterationRule.java,v $ $Revision: 1.2 $ $Date: 1999/12/21 23:58:44 $
 *
 * $Log: TransliterationRule.java,v $
 * Revision 1.2  1999/12/21 23:58:44  Alan
 * Detect a>x masking a>y
 *
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
     * The string that must be matched.
     */
    private String key;

    /**
     * The string that is emitted if the key, anteContext, and postContext
     * are matched.
     */
    private String output;

    /**
     * The string that must match before the key.  Must not be the empty string.
     * May be null; if null, then there is no matching requirement before the
     * key.
     */
    private String anteContext;

    /**
     * The string that must match after the key.  Must not be the empty string.
     * May be null; if null, then there is no matching requirement after the
     * key.
     */
    private String postContext;

    /**
     * The position of the cursor after emitting the output string, from 0 to
     * output.length().  For most rules with no special cursor specification,
     * the cursorPos is output.length().
     */
    private int cursorPos;

    /**
     * A string used to implement masks().
     */
    private String maskKey;

    private static final String COPYRIGHT =
        "\u00A9 IBM Corporation 1999. All rights reserved.";

    /**
     * Construct a new rule with the given key, output text, and other
     * attributes.  Zero, one, or two context strings may be specified.  A
     * cursor position may be specified for the output text.
     * @param key the string to match
     * @param output the string to produce when the <code>key</code> is seen
     * @param anteContext if not null and not empty, then it must be matched
     * before the <code>key</code>
     * @param postContext if not null and not empty, then it must be matched
     * after the <code>key</code>
     * @param cursorPos a position for the cursor after the <code>output</code>
     * is emitted.  If less than zero, then the cursor is placed after the
     * <code>output</code>; that is, -1 is equivalent to
     * <code>output.length()</code>.  If greater than
     * <code>output.length()</code> then an exception is thrown.
     * @exception IllegalArgumentException if the cursor position is out of
     * range.
     */
    public TransliterationRule(String key, String output,
                               String anteContext, String postContext,
                               int cursorPos) {
        this.key = key;
        this.output = output;
        this.anteContext = (anteContext != null && anteContext.length() > 0)
            ? anteContext : null;
        this.postContext = (postContext != null && postContext.length() > 0)
            ? postContext : null;
        this.cursorPos = cursorPos < 0 ? output.length() : cursorPos;
        if (this.cursorPos > output.length()) {
            throw new IllegalArgumentException("Illegal cursor position");
        }

        /* The mask key is needed when we are adding individual rules to a rule
         * set, for performance.  Here are the numbers: Without mask key, 13.0
         * seconds.  With mask key, 6.2 seconds.  However, once the rules have
         * been added to the set, then they can be discarded to free up space.
         * This is what the freeze() method does.  After freeze() has been
         * called, the method masks() must NOT be called.
         */
        maskKey = key;
        if (postContext != null) {
            maskKey += postContext;
        }
    }

    /**
     * Return the length of the key.  Equivalent to <code>getKey().length()</code>.
     * @return the length of the match key.
     */
    public int getKeyLength() {
        return key.length();
    }

    /**
     * Return the key.
     * @return the match key.
     */
    public String getKey() {
        return key;
    }

    /**
     * Return the output string.
     * @return the output string.
     */
    public String getOutput() {
        return output;
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
        return anteContext == null ? 0 : anteContext.length();
    }

    /**
     * Return true if this rule masks another rule.  If r1 masks r2 then
     * r1 matches any input string that r2 matches.  If r1 masks r2 and r2 masks
     * r1 then r1 == r2.  Examples: "a>x" masks "ab>y".  "a>x" masks "a[b]>y".
     * "[c]a>x" masks "[dc]a>y".
     *
     * <p>This method must not be called after freeze() is called.
     */
    public boolean masks(TransliterationRule r2) {
        /* There are three cases of masking.  In each instance, rule1
         * masks rule2.
         *
         * 1. KEY mask: len(key1) <= len(key2), key2 starts with key1.
         * 1<2 detects a>b masking ab>c; 1=2 detects a>b masking a>c.
         *
         * 2. PREFIX mask: key1 == key2, len(prefix1) < len(prefix2),
         * prefix2 ends with prefix1, suffix2 starts with suffix1.
         *
         * 3. SUFFIX mask: key1 == key2, len(suffix1) < len(suffix2),
         * prefix2 ends with prefix1, suffix2 starts with suffix1.
         */

        /* LIMITATION of the current mask algorithm: Some rule
         * maskings are currently not detected.  For example,
         * "{Lu}]a>x" masks "A]a>y".  To detect these sorts of masking,
         * we need a subset operator on UnicodeSet objects, which we
         * currently do not have.  This can be added later.
         */

        // maskKey = key + postContext
        return ((maskKey.length() <= r2.maskKey.length() &&
                 r2.maskKey.startsWith(maskKey)) ||
                (r2.anteContext != null && maskKey.equals(r2.maskKey) &&
                 ((anteContext == null) ||
                  (anteContext.length() < r2.anteContext.length() &&
                   r2.anteContext.endsWith(anteContext)))));
    }

    /**
     * Free up space.  Once this method is called, masks() must NOT be called.
     * If it is called, an exception will be thrown.
     */
    public void freeze() {
        maskKey = null;
    }

    /**
     * Return a string representation of this object.
     * @return string representation of this object
     */
    public String toString() {
        return getClass().getName() + '['
            + escape((anteContext != null ? ("[" + anteContext + ']') : "")
            + key
            + (postContext != null ? ("[" + postContext + ']') : "")
            + " -> "
            + (cursorPos < output.length()
               ? (output.substring(0, cursorPos) + '|' + output.substring(cursorPos))
               : output))
            + ']';
    }

    /**
     * Return true if this rule matches the given text.  The text being matched
     * occupies a virtual buffer consisting of the contents of
     * <code>result</code> concatenated to a substring of <code>text</code>.
     * The substring is specified by <code>start</code> and <code>limit</code>.
     * The value of <code>cursor</code> is an index into this virtual buffer,
     * from 0 to the length of the buffer.  In terms of the parameters,
     * <code>cursor</code> must be between 0 and <code>result.length() + limit -
     * start</code>.
     * @param text the untranslated text
     * @param start the beginning index, inclusive; <code>0 <= start
     * <= limit</code>.
     * @param limit the ending index, exclusive; <code>start <= limit
     * <= text.length()</code>.
     * @param result translated text so far
     * @param cursor position at which to translate next, an offset into result.
     * If greater than or equal to result.length(), represents offset start +
     * cursor - result.length() into text.
     * @param filter the filter.  Any character for which
     * <tt>filter.isIn()</tt> returns <tt>false</tt> will not be
     * altered by this transliterator.  If <tt>filter</tt> is
     * <tt>null</tt> then no filtering is applied.
     */
    public boolean matches(String text, int start, int limit,
                           StringBuffer result, int cursor,
                           Dictionary variables,
                           UnicodeFilter filter) {
        return
            (anteContext == null
             || regionMatches(text, start, limit, result,
                              cursor - anteContext.length(),
                              anteContext, variables, filter)) &&
            regionMatches(text, start, limit, result, cursor,
                          key, variables, filter) &&
            (postContext == null
             || regionMatches(text, start, limit, result,
                              cursor + key.length(),
                              postContext, variables, filter));
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
     * <tt>filter.isIn()</tt> returns <tt>false</tt> will not be
     * altered by this transliterator.  If <tt>filter</tt> is
     * <tt>null</tt> then no filtering is applied.
     */
    public boolean matches(Replaceable text, int start, int limit,
                           int cursor, Dictionary variables,
                           UnicodeFilter filter) {
        return
            (anteContext == null
             || regionMatches(text, start, limit, cursor - anteContext.length(),
                              anteContext, variables, filter)) &&
            regionMatches(text, start, limit, cursor,
                          key, variables, filter) &&
            (postContext == null
             || regionMatches(text, start, limit, cursor + key.length(),
                              postContext, variables, filter));
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
     * <tt>filter.isIn()</tt> returns <tt>false</tt> will not be
     * altered by this transliterator.  If <tt>filter</tt> is
     * <tt>null</tt> then no filtering is applied.
     * @return one of <code>MISMATCH</code>, <code>PARTIAL_MATCH</code>, or
     * <code>FULL_MATCH</code>.
     * @see #MISMATCH
     * @see #PARTIAL_MATCH
     * @see #FULL_MATCH
     */
    public int getMatchDegree(Replaceable text, int start, int limit,
                              int cursor, Dictionary variables,
                              UnicodeFilter filter) {
        if (anteContext != null
            && !regionMatches(text, start, limit, cursor - anteContext.length(),
                              anteContext, variables, filter)) {
            return MISMATCH;
        }
        int len = getRegionMatchLength(text, start, limit, cursor,
                                       key, variables, filter);
        if (len < 0) {
            return MISMATCH;
        }
        if (len < key.length()) {
            return PARTIAL_MATCH;
        }
        if (postContext == null) {
            return FULL_MATCH;
        }
        len = getRegionMatchLength(text, start, limit,
                                   cursor + key.length(),
                                   postContext, variables, filter);
        return (len < 0) ? MISMATCH
                         : ((len == postContext.length()) ? FULL_MATCH
                                                          : PARTIAL_MATCH);
    }

    /**
     * Return true if a template matches the text.  The entire length of the
     * template is compared to the text at the cursor.  As in
     * <code>matches()</code>, the text being matched occupies a virtual buffer
     * consisting of the contents of <code>result</code> concatenated to a
     * substring of <code>text</code>.  See <code>matches()</code> for details.
     * @param text the untranslated text
     * @param start the beginning index, inclusive; <code>0 <= start
     * <= limit</code>.
     * @param limit the ending index, exclusive; <code>start <= limit
     * <= text.length()</code>.
     * @param result translated text so far
     * @param cursor position at which to translate next, an offset into result.
     * If greater than or equal to result.length(), represents offset start +
     * cursor - result.length() into text.
     * @param template the text to match against.  All characters must match.
     * @param variables a dictionary of variables mapping <code>Character</code>
     * to <code>UnicodeSet</code>
     * @param filter the filter.  Any character for which
     * <tt>filter.isIn()</tt> returns <tt>false</tt> will not be
     * altered by this transliterator.  If <tt>filter</tt> is
     * <tt>null</tt> then no filtering is applied.
     * @return true if there is a match
     */
    protected static boolean regionMatches(String text, int start, int limit,
                                           StringBuffer result, int cursor,
                                           String template,
                                           Dictionary variables,
                                           UnicodeFilter filter) {
        int rlen = result.length();
        if (cursor < 0
            || (cursor + template.length()) > (rlen + limit - start)) {
            return false;
        }
        for (int i=0; i<template.length(); ++i, ++cursor) {
            if (!charMatches(template.charAt(i),
                             cursor < rlen ? result.charAt(cursor)
                                           : text.charAt(cursor - rlen + start),
                             variables, filter)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Return true if a template matches the text.  The entire length of the
     * template is compared to the text at the cursor.
     * @param text the text, both translated and untranslated
     * @param start the beginning index, inclusive; <code>0 <= start
     * <= limit</code>.
     * @param limit the ending index, exclusive; <code>start <= limit
     * <= text.length()</code>.
     * @param cursor position at which to translate next, representing offset
     * into text.  This value must be between <code>start</code> and
     * <code>limit</code>.
     * @param template the text to match against.  All characters must match.
     * @param variables a dictionary of variables mapping <code>Character</code>
     * to <code>UnicodeSet</code>
     * @param filter the filter.  Any character for which
     * <tt>filter.isIn()</tt> returns <tt>false</tt> will not be
     * altered by this transliterator.  If <tt>filter</tt> is
     * <tt>null</tt> then no filtering is applied.
     * @return true if there is a match
     */
    protected static boolean regionMatches(Replaceable text, int start, int limit,
                                           int cursor,
                                           String template, Dictionary variables,
                                           UnicodeFilter filter) {
        if (cursor < start
            || (cursor + template.length()) > limit) {
            return false;
        }
        for (int i=0; i<template.length(); ++i, ++cursor) {
            if (!charMatches(template.charAt(i), text.charAt(cursor),
                             variables, filter)) {
                return false;
            }
        }
        return true;
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
     * @param template the text to match against.  All characters must match.
     * @param variables a dictionary of variables mapping <code>Character</code>
     * to <code>UnicodeSet</code>
     * @param filter the filter.  Any character for which
     * <tt>filter.isIn()</tt> returns <tt>false</tt> will not be
     * altered by this transliterator.  If <tt>filter</tt> is
     * <tt>null</tt> then no filtering is applied.
     * @return -1 if there is a mismatch, 0 if the text is not long enough to
     * match any characters, otherwise the number of characters of text that
     * match this rule.
     */
    protected static int getRegionMatchLength(Replaceable text, int start,
                                              int limit, int cursor,
                                              String template,
                                              Dictionary variables,
                                              UnicodeFilter filter) {
        if (cursor < start) {
            return -1;
        }
        int i;
        for (i=0; i<template.length() && cursor<limit; ++i, ++cursor) {
            if (!charMatches(template.charAt(i), text.charAt(cursor),
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
     * <tt>filter.isIn()</tt> returns <tt>false</tt> will not be
     * altered by this transliterator.  If <tt>filter</tt> is
     * <tt>null</tt> then no filtering is applied.
     */
    protected static boolean charMatches(char keyChar, char textChar,
                                         Dictionary variables, UnicodeFilter filter) {
        UnicodeSet set = null;
        return (filter == null || filter.isIn(textChar)) &&
            ((set = (UnicodeSet) variables.get(new Character(keyChar)))
             == null) ?
            keyChar == textChar : set.contains(textChar);
    }

    /**
     * Escape non-ASCII characters as Unicode.
     */
    public static final String escape(String s) {
        StringBuffer buf = new StringBuffer();
        for (int i=0; i<s.length(); ++i) {
            char c = s.charAt(i);
            if (c >= ' ' && c <= 0x007F) {
                buf.append(c);
            } else {
                buf.append("\\u");
                if (c < 0x1000) {
                    buf.append('0');
                    if (c < 0x100) {
                        buf.append('0');
                        if (c < 0x10) {
                            buf.append('0');
                        }
                    }
                }
                buf.append(Integer.toHexString(c));
            }
        }
        return buf.toString();
    }
}
