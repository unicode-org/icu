// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.parse;

import com.ibm.icu.text.UnicodeSet;

/**
 * @author sffc
 *
 */
public class MinusSignMatcher extends SymbolMatcher {

    public MinusSignMatcher() {
        super("-", new UnicodeSet("[-_]"));
    }

    @Override
    protected boolean isDisabled(ParsedNumber result) {
        return 0 != (result.flags & ParsedNumber.FLAG_NEGATIVE);
    }

    @Override
    protected void accept(ParsedNumber result) {
        result.flags |= ParsedNumber.FLAG_NEGATIVE;
    }

    @Override
    public String toString() {
        return "<MinusSignMatcher>";
    }
}
