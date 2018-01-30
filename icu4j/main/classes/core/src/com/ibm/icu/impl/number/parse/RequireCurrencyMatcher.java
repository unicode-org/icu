// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.parse;

/**
 * @author sffc
 *
 */
public class RequireCurrencyMatcher extends ValidationMatcher {

    @Override
    public void postProcess(ParsedNumber result) {
        if (result.currencyCode == null && 0 == (result.flags & ParsedNumber.FLAG_HAS_DEFAULT_CURRENCY)) {
            result.flags |= ParsedNumber.FLAG_FAIL;
        }
    }

    @Override
    public String toString() {
        return "<RequireCurrency>";
    }

}
