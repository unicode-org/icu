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
    public enum Quantifier {
        /**
         * Collapse spans. That is, modify/count the entire matching span as a single item, instead of separate
         * code points.
         * 
         */
        SPAN,
        /**
         * Use the smallest number of elements in the spanned range for counting and modification. In other words, the "longest matches" are
         * used where possible. If there are no strings, this will be the same as code points.
         * <p>For example, in the string "abab":
         * <ul>
         * <li>spanning with [ab] will also count four MIN_ELEMENTS.</li>
         * <li>spanning with [{ab}] will count two MIN_ELEMENTS.</li>
         * <li>spanning with [ab{ab}] will also count two MIN_ELEMENTS.</li>
         * </ul>
         */
        MIN_ELEMENTS,
        // Note: could in the future have an additional option MAX_ELEMENTS
    }

    /**
     * Returns the number of matching characters found in a character sequence, counting by Quantifier.ELEMENT using SpanCondition.CONTAINED.
     * 
     * @param sequence
     *            the sequence to count characters in
     * @return the count. Zero if there are none.
     */
    public int countIn(CharSequence sequence) {
        return countIn(sequence, Quantifier.MIN_ELEMENTS, SpanCondition.CONTAINED);
    }

    /**
     * Returns the number of matching characters found in a character sequence, using SpanCondition.CONTAINED
     * 
     * @param sequence
     *            the sequence to count characters in
     * @return the count. Zero if there are none.
     */
    public int countIn(CharSequence sequence, Quantifier quantifier) {
        return countIn(sequence, quantifier, SpanCondition.CONTAINED);
    }

    /**
     * Returns the number of matching characters found in a character sequence.
     * 
     * @param sequence
     *            the sequence to count characters in
     * @param quantifier
     *            (optional) whether to treat the entire span as a match, or individual code points
     * @param countSpan
     *            (optional) the spanCondition to use. CONTAINED means only count the code points in the CONTAINED span;
     *            NOT_CONTAINED is the reverse.
     * @return the count. Zero if there are none.
     */
    public int countIn(CharSequence sequence, Quantifier quantifier, SpanCondition countSpan) {
        int count = 0;
        int start = 0;
        SpanCondition skipSpan = countSpan == SpanCondition.CONTAINED ? SpanCondition.NOT_CONTAINED
                : SpanCondition.CONTAINED;
        final int length = sequence.length();
        OutputInt spanCount = new OutputInt();
        while (start != length) {
            int endNotContained = unicodeSet.span(sequence, start, skipSpan);
            if (endNotContained == length) {
                break;
            }
            start = unicodeSet.spanAndCount(sequence, endNotContained, countSpan, spanCount);
            count += quantifier == Quantifier.SPAN ? 1 : spanCount.value;
        }
        return count;
    }

    /**
     * Delete all the matching spans in sequence, using SpanCondition.CONTAINED
     * 
     * @param sequence
     *            charsequence to replace matching spans in.
     * @return modified string.
     */
    public String deleteFrom(CharSequence sequence) {
        return replaceFrom(sequence, "", Quantifier.SPAN, SpanCondition.CONTAINED);
    }

    /**
     * Delete all matching spans in sequence, according to the operations.
     * 
     * @param sequence
     *            charsequence to replace matching spans in.
     * @param modifySpan
     *            specify whether to modify the matching spans (CONTAINED) or the non-matching (NOT_CONTAINED)
     * @return modified string.
     */
    public String deleteFrom(CharSequence sequence, SpanCondition modifySpan) {
        return replaceFrom(sequence, "", Quantifier.SPAN, modifySpan);
    }

    /**
     * Replace all matching spans in sequence by the replacement,
     * counting by Quantifier.ELEMENT using SpanCondition.CONTAINED.
     * 
     * @param sequence
     *            charsequence to replace matching spans in.
     * @param replacement
     *            replacement sequence. To delete, use ""
     * @return modified string.
     */
    public String replaceFrom(CharSequence sequence, CharSequence replacement) {
        return replaceFrom(sequence, replacement, Quantifier.MIN_ELEMENTS, SpanCondition.CONTAINED);
    }

    /**
     * Replace all matching spans in sequence by replacement, according to the Quantifier, using SpanCondition.CONTAINED. 
     * 
     * @param sequence
     *            charsequence to replace matching spans in.
     * @param replacement
     *            replacement sequence. To delete, use ""
     * @param quantifier
     *            whether to treat the entire span as a match, or individual code points
     * @return modified string.
     */
    public String replaceFrom(CharSequence sequence, CharSequence replacement, Quantifier quantifier) {
        return replaceFrom(sequence, replacement, quantifier, SpanCondition.CONTAINED);
    }

    /**
     * Replace all matching spans in sequence by replacement, according to the operations quantifier and modifySpan.
     * 
     * @param sequence
     *            charsequence to replace matching spans in.
     * @param replacement
     *            replacement sequence. To delete, use ""
     * @param modifySpan
     *            (optional) specify whether to modify the matching spans (CONTAINED) or the non-matching
     *            (NOT_CONTAINED)
     * @param quantifier
     *            (optional) specify whether to collapse or do codepoint by codepoint.
     * @return modified string.
     */
    public String replaceFrom(CharSequence sequence, CharSequence replacement, Quantifier quantifier,
            SpanCondition modifySpan) {
        SpanCondition copySpan = modifySpan == SpanCondition.CONTAINED ? SpanCondition.NOT_CONTAINED
                : SpanCondition.CONTAINED;
        final boolean remove = replacement.length() == 0;
        StringBuilder result = new StringBuilder();
        // TODO, we can optimize this to
        // avoid this allocation unless needed

        final int length = sequence.length();
        OutputInt spanCount = new OutputInt();
        for (int endCopy = 0; endCopy != length;) {
            int endModify = unicodeSet.spanAndCount(sequence, endCopy, modifySpan, spanCount);
            if (remove || endModify == 0) {
                // do nothing
            } else if (quantifier == Quantifier.SPAN) {
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
         * Trim leading spans (subject to INVERT).
         * 
         */
        LEADING,
        /**
         * Trim leading and trailing spans (subject to INVERT).
         * 
         */
        BOTH,
        /**
         * Trim trailing spans (subject to INVERT).
         * 
         */
        TRAILING;
    }

    /**
     * Returns a trimmed sequence (using CharSequence.subsequence()), that omits matching code points at the start or
     * end of the string, using TrimOption.BOTH and SpanCondition.CONTAINED. For example:
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
        return trim(sequence, TrimOption.BOTH, SpanCondition.CONTAINED);
    }

    /**
     * Returns a trimmed sequence (using CharSequence.subsequence()), that omits matching code points at the start or
     * end of the string, using the trimOption and SpanCondition.CONTAINED. For example:
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
        return trim(sequence, trimOption, SpanCondition.CONTAINED);
    }

    /**
     * Returns a trimmed sequence (using CharSequence.subsequence()), that omits matching code points at the start or
     * end of the string, depending on the trimOption and modifySpan. For example:
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
     *            (optional) LEADING, TRAILING, or BOTH
     * @param modifySpan
     *            (optional) CONTAINED or NOT_CONTAINED
     * @return a subsequence
     */
    public CharSequence trim(CharSequence sequence, TrimOption trimOption, SpanCondition modifySpan) {
        int endLeadContained, startTrailContained;
        final int length = sequence.length();
        if (trimOption != TrimOption.TRAILING) {
            endLeadContained = unicodeSet.span(sequence, modifySpan);
            if (endLeadContained == length) {
                return "";
            }
        } else {
            endLeadContained = 0;
        }
        if (trimOption != TrimOption.LEADING) {
            startTrailContained = unicodeSet.spanBack(sequence, modifySpan);
        } else {
            startTrailContained = length;
        }
        return endLeadContained == 0 && startTrailContained == length ? sequence : sequence.subSequence(
                endLeadContained, startTrailContained);
    }

}
