// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.parse;

/**
 * @author sffc
 *
 */
public class RequireCurrencyMatcher implements NumberParseMatcher {

    @Override
    public boolean match(StringSegment segment, ParsedNumber result) {
        return false;
    }

    @Override
    public void postProcess(ParsedNumber result) {
        if (result.currencyCode == null && 0 == (result.flags & ParsedNumber.FLAG_HAS_DEFAULT_CURRENCY)) {
            result.clear();
        }
    }

    @Override
    public String toString() {
        return "<RequireCurrency>";
    }

}
