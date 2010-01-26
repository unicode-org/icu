/*
*******************************************************************************
*   Copyright (C) 2009-2010, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*/
package com.ibm.icu.text;

import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.UnicodeSet;

/**
 * Normalization filtered by a UnicodeSet.
 * Normalizes portions of the text contained in the filter set and leaves
 * portions not contained in the filter set unchanged.
 * Filtering is done via UnicodeSet.span(..., UnicodeSet.SpanCondition.SIMPLE).
 * Not-in-the-filter text is treated as "is normalized" and "quick check yes".
 * This class implements all of (and only) the Normalizer2 API.
 * An instance of this class is unmodifiable/immutable.
 * @draft ICU 4.4
 * @provisional This API might change or be removed in a future release.
 * @author Markus W. Scherer
 */
public class FilteredNormalizer2 extends Normalizer2 {
    /**
     * Constructs a filtered normalizer wrapping any Normalizer2 instance
     * and a filter set.
     * Both are aliased and must not be modified or deleted while this object
     * is used.
     * The filter set should be frozen; otherwise the performance will suffer greatly.
     * @param n2 wrapped Normalizer2 instance
     * @param filterSet UnicodeSet which determines the characters to be normalized
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public FilteredNormalizer2(Normalizer2 n2, UnicodeSet filterSet) {
        norm2=n2;
        set=filterSet;
    }

    /** {@inheritDoc}
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    @Override
    public StringBuilder normalize(CharSequence src, StringBuilder dest) {
        return dest;
    }
    /** {@inheritDoc}
     * @internal ICU 4.4 TODO: propose for 4.6
     * @provisional This API might change or be removed in a future release.
     */
    public Appendable normalize(CharSequence src, Appendable dest) {
        return dest;
    }
    /** {@inheritDoc}
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    @Override
    public StringBuilder normalizeSecondAndAppend(
            StringBuilder first, CharSequence second) {
        return first;
    }
    /** {@inheritDoc}
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    @Override
    public StringBuilder append(StringBuilder first, CharSequence second) {
        return first;
    }

    /** {@inheritDoc}
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    @Override
    public boolean isNormalized(CharSequence s) {
        return false;
    }
    /** {@inheritDoc}
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    @Override
    public Normalizer.QuickCheckResult quickCheck(CharSequence s) {
        return Normalizer.NO;
    }
    /** {@inheritDoc}
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    @Override
    public int spanQuickCheckYes(CharSequence s) {
        return 0;
    }

    /** {@inheritDoc}
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    @Override
    public boolean hasBoundaryBefore(int c) {
        return false;
    }

    /** {@inheritDoc}
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    @Override
    public boolean hasBoundaryAfter(int c) {
        return false;
    }

    /** {@inheritDoc}
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    @Override
    public boolean isInert(int c) {
        return false;
    }

    /*private StringBuilder normalize(CharSequence src, StringBuilder dest, USetSpanCondition spanCondition) {
        return dest;
    } TODO: need UnicodeSet.span() */

    /*private StringBuilder normalizeSecondAndAppend(
            StringBuilder first, CharSequence second, boolean doNormalize) {
        return first;
    } TODO: need UnicodeSet.span() */

    @SuppressWarnings("unused")
    private Normalizer2 norm2;
    @SuppressWarnings("unused")
    private UnicodeSet set;
};
