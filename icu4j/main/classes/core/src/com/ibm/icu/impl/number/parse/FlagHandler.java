// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.parse;

/**
 * Unconditionally applies a given set of flags to the ParsedNumber in the post-processing step.
 */
public class FlagHandler extends ValidationMatcher {

    public static final FlagHandler PERCENT = new FlagHandler(ParsedNumber.FLAG_PERCENT);
    public static final FlagHandler PERMILLE = new FlagHandler(ParsedNumber.FLAG_PERMILLE);

    private final int flags;

    private FlagHandler(int flags) {
        this.flags = flags;
    }

    @Override
    public void postProcess(ParsedNumber result) {
        result.flags |= flags;
    }

    @Override
    public String toString() {
        return "<FlagsHandler " + Integer.toHexString(flags) + ">";
    }
}
