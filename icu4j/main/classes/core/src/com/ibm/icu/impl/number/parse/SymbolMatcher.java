// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.parse;

import com.ibm.icu.text.UnicodeSet;

/**
 * @author sffc
 *
 */
public abstract class SymbolMatcher implements NumberParseMatcher {
    private final String string;
    private final UnicodeSet uniSet;

    protected SymbolMatcher(String symbolString, UnicodeSet symbolUniSet) {
        string = symbolString;
        uniSet = symbolUniSet;
    }

    @Override
    public boolean match(StringSegment segment, ParsedNumber result) {
        // Smoke test first; this matcher might be disabled.
        if (isDisabled(result)) {
            return false;
        }

        int cp = segment.getCodePoint();
        if (cp != -1 && uniSet.contains(cp)) {
            accept(result);
            segment.adjustOffset(Character.charCount(cp));
            return false;
        }
        int overlap = segment.getCommonPrefixLength(string);
        if (overlap == string.length()) {
            accept(result);
            segment.adjustOffset(string.length());
            return false;
        }
        return overlap == segment.length();
    }

    @Override
    public void postProcess(ParsedNumber result) {
        // No-op
    }

    protected abstract boolean isDisabled(ParsedNumber result);

    protected abstract void accept(ParsedNumber result);
}
