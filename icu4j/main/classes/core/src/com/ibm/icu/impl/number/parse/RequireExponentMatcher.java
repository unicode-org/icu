// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.parse;

/**
 * @author sffc
 *
 */
public class RequireExponentMatcher implements NumberParseMatcher {

    @Override
    public boolean match(StringSegment segment, ParsedNumber result) {
        return false;
    }

    @Override
    public void postProcess(ParsedNumber result) {
        if (0 == (result.flags & ParsedNumber.FLAG_HAS_EXPONENT)) {
            result.clear();
        }
    }

}
