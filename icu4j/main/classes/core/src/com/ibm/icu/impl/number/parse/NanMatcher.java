// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.parse;

import com.ibm.icu.text.DecimalFormatSymbols;

/**
 * @author sffc
 *
 */
public class NanMatcher implements NumberParseMatcher {

    private final String nanString;

    public NanMatcher(DecimalFormatSymbols symbols) {
        nanString = symbols.getNaN();
    }

    @Override
    public boolean match(StringSegment segment, ParsedNumber result) {
        int overlap = segment.getCommonPrefixLength(nanString);
        if (overlap == nanString.length()) {
            result.flags |= ParsedNumber.FLAG_NAN;
            segment.adjustOffset(overlap);
            result.setCharsConsumed(segment);
            return false;
        } else if (overlap == segment.length()) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void postProcess(ParsedNumber result) {
        // No-op
    }

    @Override
    public String toString() {
        return "<NanMatcher>";
    }

}
