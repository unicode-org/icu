// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.number.parse;

/**
 * @author sffc
 *
 */
public class RequireDecimalSeparatorValidator extends ValidationMatcher {

    private static final RequireDecimalSeparatorValidator A = new RequireDecimalSeparatorValidator(true);
    private static final RequireDecimalSeparatorValidator B = new RequireDecimalSeparatorValidator(false);

    private final boolean patternHasDecimalSeparator;

    public static RequireDecimalSeparatorValidator getInstance(boolean patternHasDecimalSeparator) {
        return patternHasDecimalSeparator ? A : B;
    }

    private RequireDecimalSeparatorValidator(boolean patternHasDecimalSeparator) {
        this.patternHasDecimalSeparator = patternHasDecimalSeparator;
    }

    @Override
    public void postProcess(ParsedNumber result) {
        boolean parseIsInfNaN = 0 != (result.flags & ParsedNumber.FLAG_INFINITY) || 0 != (result.flags & ParsedNumber.FLAG_NAN);
        boolean parseHasDecimalSeparator = 0 != (result.flags & ParsedNumber.FLAG_HAS_DECIMAL_SEPARATOR);
        if (!parseIsInfNaN && parseHasDecimalSeparator != patternHasDecimalSeparator) {
            result.flags |= ParsedNumber.FLAG_FAIL;
        }
    }

    @Override
    public String toString() {
        return "<RequireDecimalSeparator>";
    }
}
