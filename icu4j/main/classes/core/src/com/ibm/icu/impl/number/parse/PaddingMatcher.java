// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.parse;

import com.ibm.icu.text.UnicodeSet;

/**
 * @author sffc
 *
 */
public class PaddingMatcher extends RangeMatcher {

    /**
     * @param uniSet
     */
    protected PaddingMatcher(String padString) {
        super(new UnicodeSet().add(padString).freeze());
    }

    @Override
    protected boolean isDisabled(ParsedNumber result) {
        return false;
    }

    @Override
    protected void accept(StringSegment segment, ParsedNumber result) {
        // No-op
    }

    @Override
    public String toString() {
        return "<PaddingMatcher " + uniSet + ">";
    }
}
