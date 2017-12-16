// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.parse;

import com.ibm.icu.text.UnicodeSet;

/**
 * @author sffc
 *
 */
public class PercentMatcher extends SymbolMatcher {

    public PercentMatcher() {
        // FIXME
        super("%", new UnicodeSet("[%]"));
    }

    @Override
    protected boolean isDisabled(ParsedNumber result) {
        return 0 != (result.flags & ParsedNumber.FLAG_PERCENT);
    }

    @Override
    protected void accept(ParsedNumber result) {
        result.flags |= ParsedNumber.FLAG_PERCENT;
    }

    @Override
    public void postProcess(ParsedNumber result) {
        super.postProcess(result);
        if (0 != (result.flags & ParsedNumber.FLAG_PERCENT) && result.quantity != null) {
            result.quantity.adjustMagnitude(-2);
        }
    }

    @Override
    public String toString() {
        return "<PercentMatcher>";
    }
}
