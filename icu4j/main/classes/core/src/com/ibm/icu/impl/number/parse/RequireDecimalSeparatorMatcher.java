// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.parse;

/**
 * @author sffc
 *
 */
public class RequireDecimalSeparatorMatcher extends ValidationMatcher {

    private static final RequireDecimalSeparatorMatcher A = new RequireDecimalSeparatorMatcher(true);
    private static final RequireDecimalSeparatorMatcher B = new RequireDecimalSeparatorMatcher(false);

    private final boolean patternHasDecimalSeparator;

    public static RequireDecimalSeparatorMatcher getInstance(boolean patternHasDecimalSeparator) {
        return patternHasDecimalSeparator ? A : B;
    }

    private RequireDecimalSeparatorMatcher(boolean patternHasDecimalSeparator) {
        this.patternHasDecimalSeparator = patternHasDecimalSeparator;
    }

    @Override
    public void postProcess(ParsedNumber result) {
        boolean parseHasDecimalSeparator = 0 != (result.flags & ParsedNumber.FLAG_HAS_DECIMAL_SEPARATOR);
        if (parseHasDecimalSeparator != patternHasDecimalSeparator) {
            result.flags |= ParsedNumber.FLAG_FAIL;
        }
    }

    @Override
    public String toString() {
        return "<RequireDecimalSeparator>";
    }
}
