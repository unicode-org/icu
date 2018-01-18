// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.parse;

import com.ibm.icu.text.UnicodeSet;

/**
 * @author sffc
 *
 */
public abstract class SymbolMatcher implements NumberParseMatcher {
    protected final String string;
    protected final UnicodeSet uniSet;

    // TODO: Implement this class using only UnicodeSet and not String?
    // How to deal with case folding?

    protected SymbolMatcher(String symbolString, UnicodeSet symbolUniSet) {
        string = symbolString;
        uniSet = symbolUniSet;
    }

    protected SymbolMatcher(UnicodeSetStaticCache.Key key) {
        string = "";
        uniSet = UnicodeSetStaticCache.get(key);
    }

    @Override
    public boolean match(StringSegment segment, ParsedNumber result) {
        // Smoke test first; this matcher might be disabled.
        if (isDisabled(result)) {
            return false;
        }

        int cp = segment.getCodePoint();
        if (cp != -1 && uniSet.contains(cp)) {
            segment.adjustOffset(Character.charCount(cp));
            accept(segment, result);
            return false;
        }

        if (string.isEmpty()) {
            return false;
        }
        int overlap = segment.getCommonPrefixLength(string);
        if (overlap == string.length()) {
            segment.adjustOffset(string.length());
            accept(segment, result);
            return false;
        }
        return overlap == segment.length();
    }

    @Override
    public UnicodeSet getLeadCodePoints() {
        if (string == null || string.isEmpty()) {
            // Assumption: for sets from UnicodeSetStaticCache, uniSet == leadCodePoints.
            return uniSet;
        }

        UnicodeSet leadCodePoints = new UnicodeSet();
        ParsingUtils.putLeadCodePoints(uniSet, leadCodePoints);
        ParsingUtils.putLeadCodePoint(string, leadCodePoints);
        return leadCodePoints.freeze();
    }

    @Override
    public void postProcess(ParsedNumber result) {
        // No-op
    }

    protected abstract boolean isDisabled(ParsedNumber result);

    protected abstract void accept(StringSegment segment, ParsedNumber result);
}
