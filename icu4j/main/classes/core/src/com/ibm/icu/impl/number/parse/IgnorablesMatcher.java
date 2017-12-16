// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.parse;

import com.ibm.icu.text.UnicodeSet;

/**
 * @author sffc
 *
 */
public class IgnorablesMatcher implements NumberParseMatcher {

    // BiDi characters are skipped over and ignored at any point in the string, even in strict mode.
    static final UnicodeSet UNISET_BIDI = new UnicodeSet("[[\\u200E\\u200F\\u061C]]").freeze();

    // This set was decided after discussion with icu-design@. See ticket #13309.
    // Zs+TAB is "horizontal whitespace" according to UTS #18 (blank property).
    static final UnicodeSet UNISET_WHITESPACE = new UnicodeSet("[[:Zs:][\\u0009]]").freeze();

    /** The default set of ignorables. */
    static final UnicodeSet DEFAULT_UNISET = UNISET_BIDI.cloneAsThawed().addAll(UNISET_WHITESPACE).freeze();

    /** The default set of ignorables for strict mode. */
    static final UnicodeSet STRICT_UNISET = UNISET_BIDI;

    private static final IgnorablesMatcher DEFAULT_INSTANCE = new IgnorablesMatcher(DEFAULT_UNISET);
    private static final IgnorablesMatcher STRICT_INSTANCE = new IgnorablesMatcher(STRICT_UNISET);

    public static IgnorablesMatcher getInstance(UnicodeSet ignorables) {
        assert ignorables.isFrozen();
        if (ignorables == DEFAULT_UNISET || ignorables.equals(DEFAULT_UNISET)) {
            return DEFAULT_INSTANCE;
        } else if (ignorables == STRICT_UNISET || ignorables.equals(STRICT_UNISET)) {
            return STRICT_INSTANCE;
        } else {
            return new IgnorablesMatcher(ignorables);
        }
    }

    private final UnicodeSet ignorables;

    private IgnorablesMatcher(UnicodeSet ignorables) {
        this.ignorables = ignorables;
    }

    @Override
    public boolean match(StringSegment segment, ParsedNumber result) {
        while (segment.length() > 0) {
            int cp = segment.getCodePoint();
            if (cp == -1 || !ignorables.contains(cp)) {
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
