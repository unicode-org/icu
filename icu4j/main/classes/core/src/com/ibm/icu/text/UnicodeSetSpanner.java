/*
 *******************************************************************************
 * Copyright (C) 2014, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
package com.ibm.icu.text;

import com.ibm.icu.text.UnicodeSet.SpanCondition;
import com.ibm.icu.util.OutputInt;

/**
 * A helper class used to count, replace, and trim CharSequences based on UnicodeSet matches.
 * An instance is immutable (and thus thread-safe) iff the source UnicodeSet is frozen.
 * <p><b>Note:</b> The counting, deletion, and replacement depend on alternating a {@link SpanCondition} with
 * its inverse. That is, the code spans, then spans for the inverse, then spans, and so on.
 * For the inverse, the following mapping is used:</p>
 * <ul>
 * <li>{@link SpanCondition.SIMPLE} → {@link SpanCondition.NOT_CONTAINED}</li>
 * <li>{@link SpanCondition.CONTAINED} → {@link SpanCondition.NOT_CONTAINED}</li>
 * <li>{@link SpanCondition.NOT_CONTAINED} → {@link SpanCondition.SIMPLE}</li>
 * </ul>
 * These are actually not complete inverses. However, the alternating works because there are no gaps.
 * For example, with [a{ab}{bc}], you get the following behavior when scanning forward:
 * <p>
 * <table border="1">
 * <tr><th>SIMPLE</th><td>xxx[ab]cyyy</td></tr>
 * <tr><th>CONTAINED</th><td>xxx[abc]yyy</td></tr>
 * <tr><th>NOT_CONTAINED</th><td>[xxx]ab[cyyy]</td></tr>
 * </table>
 * <p>So here is what happens when you alternate:
 * <p>
 * <table border="1">
 * <tr><th>start</th><td>|xxxabcyyy</td></tr>
 * <tr><th>NOT_CONTAINED</th><td>xxx|abcyyy</td></tr>
 * <tr><th>CONTAINED</th><td>xxxabc|yyy</td></tr>
 * <tr><th>NOT_CONTAINED</th><td>xxxabcyyy|</td></tr>
 * </table>
 * </p>The entire string is traversed.
 */
public class UnicodeSetSpanner {

    private final UnicodeSet unicodeSet;

    /**
     * Create a spanner from a UnicodeSet. For speed and safety, the UnicodeSet should be frozen. However, this class
     * can be used with a non-frozen version to avoid the cost of freezing.
     * 
     * @param source
     *            the original UnicodeSet
     */
    public UnicodeSetSpanner(UnicodeSet source) {
        unicodeSet = source;
    }

    /**
     * Returns the UnicodeSet used for processing. It is frozen iff the original was.
     * 
     * @return the construction set.
     */
    public UnicodeSet getUnicodeSet() {
        return unicodeSet;
    }


    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object other) {
        return other instanceof UnicodeSetSpanner && unicodeSet.equals(((UnicodeSetSpanner) other).unicodeSet);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return unicodeSet.hashCode();
    }

    /**
     * Options for replaceFrom and countIn to control how to treat each matched span. The name is from "qualifier" as used in regex,
     * since it is similar to whether one is replacing [abc] by x, or [abc]* by x.
     * 
     */
    public enum CountMethod {
        /**
         * Collapse spans. That is, modify/count the entire matching span as a single item, instead of separate
         * code points.
         * 
         */
        WHOLE_SPAN,
        /**
         * Use the smallest number of elements in the spanned range for counting and modification,
         * based on the {@link UnicodeSet.SpanCondition}.
         * If the set has no strings, this will be the same as the number of spanned code points.
         * <p>For example, in the string "abab" with SpanCondition.SIMPLE:
         * <ul>
         * <li>spanning with [ab] will count four MIN_ELEMENTS.</li>
         * <li>spanning with [{ab}] will count two MIN_ELEMENTS.</li>
         * <li>spanning with [ab{ab}] will also count two MIN_ELEMENTS.</li>
         * </ul>
         */
        MIN_ELEMENTS,
        // Note: could in the future have an additional option MAX_ELEMENTS
    }

    /**
     * Returns the number of matching characters found in a character sequence, 
     * counting by Quantifier.MIN_ELEMENTS using SpanCondition.SIMPLE.
     * The code alternates spans; see the class doc for {@link UnicodeSetSpanner} for a note about boundary conditions.
     * @param sequence
     *            the sequence to count characters in
     * @return the count. Zero if there are none.
     */
    public int countIn(CharSequence sequence) {
        return countIn(sequence, CountMethod.MIN_ELEMENTS, SpanCondition.SIMPLE);
    }

    /**
     * Returns the number of matching characters found in a character sequence, using SpanCondition.SIMPLE.
     * The code alternates spans; see the class doc for {@link UnicodeSetSpanner} for a note about boundary conditions.
     * @param sequence
     *            the sequence to count characters in
     * @return the count. Zero if there are none.
     */
    public int countIn(CharSequence sequence, CountMethod quantifier) {
        return countIn(sequence, quantifier, SpanCondition.SIMPLE);
    }

    /**
     * Returns the number of matching characters found in a character sequence.
     * The code alternates spans; see the class doc for {@link UnicodeSetSpanner} for a note about boundary conditions.
     * @param sequence
     *            the sequence to count characters in
     * @param quantifier whether to treat an entire span as a match, or individual code points
     * @param spanCondition
     *            the spanCondition to use. SIMPLE or CONTAINED means only count the code points in the span;
     *            NOT_CONTAINED is the reverse.
     *            <br><b>WARNING: </b> when a UnicodeSet contains strings, there may be unexpected behavior in edge cases.
     * @return the count. Zero if there are none.
     */
    public int countIn(CharSequence sequence, CountMethod quantifier, SpanCondition spanCondition) {
        int count = 0;
        int start = 0;
        SpanCondition skipSpan = spanCondition == SpanCondition.NOT_CONTAINED ? SpanCondition.SIMPLE
                : SpanCondition.NOT_CONTAINED;
        final int length = sequence.length();
        OutputInt spanCount = new OutputInt();
        while (start != length) {
            int endNotContained = unicodeSet.span(sequence, start, skipSpan);
            if (endNotContained == length) {
                break;
            }
            start = unicodeSet.spanAndCount(sequence, endNotContained, spanCondition, spanCount);
            count += quantifier == CountMethod.WHOLE_SPAN ? 1 : spanCount.value;
        }
        return count;
    }

    /**
     * Delete all the matching spans in sequence, using SpanCondition.SIMPLE
     * The code alternates spans; see the class doc for {@link UnicodeSetSpanner} for a note about boundary conditions.
     * @param sequence
     *            charsequence to replace matching spans in.
     * @return modified string.
     */
    public String deleteFrom(CharSequence sequence) {
        return replaceFrom(sequence, "", CountMethod.WHOLE_SPAN, SpanCondition.SIMPLE);
    }

    /**
     * Delete all matching spans in sequence, according to the operations.
     * The code alternates spans; see the class doc for {@link UnicodeSetSpanner} for a note about boundary conditions.
     * @param sequence
     *            charsequence to replace matching spans in.
     * @param spanCondition
     *            specify whether to modify the matching spans (CONTAINED or SIMPLE) or the non-matching (NOT_CONTAINED)
     * @return modified string.
     */
    public String deleteFrom(CharSequence sequence, SpanCondition spanCondition) {
        return replaceFrom(sequence, "", CountMethod.WHOLE_SPAN, spanCondition);
    }

    /**
     * Replace all matching spans in sequence by the replacement,
     * counting by Quantifier.MIN_ELEMENTS using SpanCondition.SIMPLE.
     * The code alternates spans; see the class doc for {@link UnicodeSetSpanner} for a note about boundary conditions.
     * @param sequence
     *            charsequence to replace matching spans in.
     * @param replacement
     *            replacement sequence. To delete, use ""
     * @return modified string.
     */
    public String replaceFrom(CharSequence sequence, CharSequence replacement) {
        return replaceFrom(sequence, replacement, CountMethod.MIN_ELEMENTS, SpanCondition.SIMPLE);
    }

    /**
     * Replace all matching spans in sequence by replacement, according to the Quantifier, using SpanCondition.SIMPLE. 
     * The code alternates spans; see the class doc for {@link UnicodeSetSpanner} for a note about boundary conditions.
     * @param sequence
     *            charsequence to replace matching spans in.
     * @param replacement
     *            replacement sequence. To delete, use ""
     * @param quantifier
     *            whether to treat an entire span as a match, or individual code points
     * @return modified string.
     */
    public String replaceFrom(CharSequence sequence, CharSequence replacement, CountMethod quantifier) {
        return replaceFrom(sequence, replacement, quantifier, SpanCondition.SIMPLE);
    }

    /**
     * Replace all matching spans in sequence by replacement, according to the operations quantifier and spanCondition.
     * The code alternates spans; see the class doc for {@link UnicodeSetSpanner} for a note about boundary conditions.
     * @param sequence
     *            charsequence to replace matching spans in.
     * @param replacement
     *            replacement sequence. To delete, use ""
     * @param spanCondition
     *            specify whether to modify the matching spans (CONTAINED or SIMPLE) or the non-matching
     *            (NOT_CONTAINED)
     * @param quantifier
     *            specify whether to collapse or do codepoint by codepoint.
     * @return modified string.
     */
    public String replaceFrom(CharSequence sequence, CharSequence replacement, CountMethod quantifier,
            SpanCondition spanCondition) {
        SpanCondition copySpan = spanCondition == SpanCondition.NOT_CONTAINED ? SpanCondition.SIMPLE
                : SpanCondition.NOT_CONTAINED;
        final boolean remove = replacement.length() == 0;
        StringBuilder result = new StringBuilder();
        // TODO, we can optimize this to
        // avoid this allocation unless needed

        final int length = sequence.length();
        OutputInt spanCount = new OutputInt();
        for (int endCopy = 0; endCopy != length;) {
            int endModify = unicodeSet.spanAndCount(sequence, endCopy, spanCondition, spanCount);
            if (remove || endModify == 0) {
                // do nothing
            } else if (quantifier == CountMethod.WHOLE_SPAN) {
                result.append(replacement);
            } else {
                for (int i = spanCount.value; i > 0; --i) {
                    result.append(replacement);
                }
            }
            if (endModify == length) {
                break;
            }
            endCopy = unicodeSet.span(sequence, endModify, copySpan);
            result.append(sequence.subSequence(endModify, endCopy));
        }
        return result.toString();
    }

    /**
     * Options for the trim() method
     * 
     */
    public enum TrimOption {
        /**
         * Trim leading spans.
         * 
         */
        LEADING,
        /**
         * Trim leading and trailing spans.
         * 
         */
        BOTH,
        /**
         * Trim trailing spans.
         * 
         */
        TRAILING;
    }

    /**
     * Returns a trimmed sequence (using CharSequence.subsequence()), that omits matching code points at the start or
     * end of the string, using TrimOption.BOTH and SpanCondition.SIMPLE. For example:
     * 
     * <pre>
     * {@code
     * 
     *   new UnicodeSet("[ab]").trim("abacatbab")}
     * </pre>
     * 
     * ... returns {@code "catbab"}.
     * 
     */
    public CharSequence trim(CharSequence sequence) {
        return trim(sequence, TrimOption.BOTH, SpanCondition.SIMPLE);
    }

    /**
     * Returns a trimmed sequence (using CharSequence.subsequence()), that omits matching code points at the start or
     * end of the string, using the trimOption and SpanCondition.SIMPLE. For example:
     * 
     * <pre>
     * {@code
     * 
     *   new UnicodeSet("[ab]").trim("abacatbab")}
     * </pre>
     * 
     * ... returns {@code "catbab"}.
     * 
     */
    public CharSequence trim(CharSequence sequence, TrimOption trimOption) {
        return trim(sequence, trimOption, SpanCondition.SIMPLE);
    }

    /**
     * Returns a trimmed sequence (using CharSequence.subsequence()), that omits matching code points at the start or
     * end of the string, depending on the trimOption and spanCondition. For example:
     * 
     * <pre>
     * {@code
     * 
     *   new UnicodeSet("[ab]").trim("abacatbab")}
     * </pre>
     * 
     * ... returns {@code "catbab"}.
     * 
     * @param sequence
     *            the sequence to trim
     * @param trimOption
     *            LEADING, TRAILING, or BOTH
     * @param spanCondition
     *            SIMPLE, CONTAINED or NOT_CONTAINED
     * @return a subsequence
     */
    public CharSequence trim(CharSequence sequence, TrimOption trimOption, SpanCondition spanCondition) {
        int endLeadContained, startTrailContained;
        final int length = sequence.length();
        if (trimOption != TrimOption.TRAILING) {
            endLeadContained = unicodeSet.span(sequence, spanCondition);
            if (endLeadContained == length) {
                return "";
            }
        } else {
            endLeadContained = 0;
        }
        if (trimOption != TrimOption.LEADING) {
            startTrailContained = unicodeSet.spanBack(sequence, spanCondition);
        } else {
            startTrailContained = length;
        }
        return endLeadContained == 0 && startTrailContained == length ? sequence : sequence.subSequence(
                endLeadContained, startTrailContained);
    }

}
