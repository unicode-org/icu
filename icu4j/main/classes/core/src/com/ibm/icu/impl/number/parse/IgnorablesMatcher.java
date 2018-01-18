// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.parse;

import com.ibm.icu.text.UnicodeSet;

/**
 * @author sffc
 *
 */
public class IgnorablesMatcher extends RangeMatcher {

    public static final IgnorablesMatcher DEFAULT = new IgnorablesMatcher(
            UnicodeSetStaticCache.get(UnicodeSetStaticCache.Key.DEFAULT_IGNORABLES));

    public static final IgnorablesMatcher STRICT = new IgnorablesMatcher(
            UnicodeSetStaticCache.get(UnicodeSetStaticCache.Key.STRICT_IGNORABLES));

    public static IgnorablesMatcher getInstance(UnicodeSet ignorables) {
        assert ignorables.isFrozen();
        return new IgnorablesMatcher(ignorables);
    }

    private IgnorablesMatcher(UnicodeSet ignorables) {
        super(ignorables);
    }

    @Override
    public UnicodeSet getLeadCodePoints() {
        if (this == DEFAULT) {
            return UnicodeSetStaticCache.get(UnicodeSetStaticCache.Key.DEFAULT_IGNORABLES);
        } else if (this == STRICT) {
            return UnicodeSetStaticCache.get(UnicodeSetStaticCache.Key.STRICT_IGNORABLES);
        } else {
            return super.getLeadCodePoints();
        }
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
        return "<IgnorablesMatcher>";
    }
}
