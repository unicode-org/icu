// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.number.parse;

/**
 * @author sffc
 *
 */
public class RequireNumberValidator extends ValidationMatcher {

    @Override
    public void postProcess(ParsedNumber result) {
        // Require that a number is matched.
        if (!result.seenNumber()) {
            result.flags |= ParsedNumber.FLAG_FAIL;
        }
    }

    @Override
    public String toString() {
        return "<RequireNumber>";
    }

}
