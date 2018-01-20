// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.parse;

import com.ibm.icu.text.UnicodeSet;

/**
 * @author sffc
 *
 */
public abstract class RangeMatcher implements NumberParseMatcher {
    protected final UnicodeSet uniSet;

    protected RangeMatcher(UnicodeSet uniSet) {
        this.uniSet = uniSet;
    }

    public UnicodeSet getSet() {
        return uniSet;
    }

    @Override
    public boolean match(StringSegment segment, ParsedNumber result) {
        // Smoke test first; this matcher might be disabled.
        if (isDisabled(result)) {
            return false;
        }

        while (segment.length() > 0) {
            int cp = segment.getCodePoint();
            if (cp != -1 && uniSet.contains(cp)) {
                segment.adjustOffset(Character.charCount(cp));
                accept(segment, result);
                continue;
            }

            // If we get here, the code point didn't match the uniSet.
            return false;
        }

        // If we get here, we consumed the entire string segment.
        return true;
    }

    @Override
    public UnicodeSet getLeadCodePoints() {
        UnicodeSet leadCodePoints = new UnicodeSet();
        ParsingUtils.putLeadCodePoints(uniSet, leadCodePoints);
        return leadCodePoints.freeze();
    }

    @Override
    public boolean matchesEmpty() {
        return true;
    }

    @Override
    public void postProcess(ParsedNumber result) {
        // No-op
    }

    protected abstract boolean isDisabled(ParsedNumber result);

    protected abstract void accept(StringSegment segment, ParsedNumber result);

}
