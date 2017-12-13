// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.parse;

import com.ibm.icu.text.UnicodeSet;

/**
 * @author sffc
 *
 */
public class WhitespaceMatcher implements NumberParseMatcher {

    // This set was decided after discussion with icu-design@. See ticket #13309.
    // Zs+TAB is "horizontal whitespace" according to UTS #18 (blank property).
    private static final UnicodeSet UNISET_WHITESPACE = new UnicodeSet("[[:Zs:][\\u0009]]").freeze();

    private static final WhitespaceMatcher INSTANCE = new WhitespaceMatcher();

    public static WhitespaceMatcher getInstance() {
        return INSTANCE;
    }

    private WhitespaceMatcher() {
    }

    @Override
    public boolean match(StringSegment segment, ParsedNumber result) {
        while (segment.length() > 0) {
            int cp = segment.getCodePoint();
            if (cp == -1 || !UNISET_WHITESPACE.contains(cp)) {
                break;
            }
            segment.adjustOffset(Character.charCount(cp));
            // Note: Do not touch the charsConsumed.
        }
        return segment.length() == 0 || segment.isLeadingSurrogate();
    }

    @Override
    public void postProcess(ParsedNumber result) {
        // No-op
    }

    @Override
    public String toString() {
        return "<WhitespaceMatcher>";
    }
}
