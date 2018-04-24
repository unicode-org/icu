// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.parse;

/**
 * @author sffc
 *
 */
public class RequireExponentValidator extends ValidationMatcher {

    @Override
    public void postProcess(ParsedNumber result) {
        if (0 == (result.flags & ParsedNumber.FLAG_HAS_EXPONENT)) {
            result.flags |= ParsedNumber.FLAG_FAIL;
        }
    }

    @Override
    public String toString() {
        return "<RequireExponent>";
    }

}
