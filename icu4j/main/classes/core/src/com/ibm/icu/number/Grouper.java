// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.number;

import com.ibm.icu.impl.number.DecimalQuantity;
import com.ibm.icu.impl.number.PatternStringParser.ParsedPatternInfo;

/**
 * @internal
 * @deprecated This API is a technical preview. It is likely to change in an upcoming release.
 */
@Deprecated
public class Grouper {

    // Conveniences for Java handling of bytes
    private static final byte N2 = -2;
    private static final byte N1 = -1;
    private static final byte B2 = 2;
    private static final byte B3 = 3;

    private static final Grouper DEFAULTS = new Grouper(N2, N2, false);
    private static final Grouper MIN2 = new Grouper(N2, N2, true);
    private static final Grouper NONE = new Grouper(N1, N1, false);

    private final byte grouping1; // -2 means "needs locale data"; -1 means "no grouping"
    private final byte grouping2;
    private final boolean min2;

    private Grouper(byte grouping1, byte grouping2, boolean min2) {
        this.grouping1 = grouping1;
        this.grouping2 = grouping2;
        this.min2 = min2;
    }

    /**
     * @internal
     * @deprecated This API is a technical preview. It is likely to change in an upcoming release.
     */
    @Deprecated
    public static Grouper defaults() {
        return DEFAULTS;
    }

    /**
     * @internal
     * @deprecated This API is a technical preview. It is likely to change in an upcoming release.
     */
    @Deprecated
    public static Grouper minTwoDigits() {
        return MIN2;
    }

    /**
     * @internal
     * @deprecated This API is a technical preview. It is likely to change in an upcoming release.
     */
    @Deprecated
    public static Grouper none() {
        return NONE;
    }

    //////////////////////////
    // PACKAGE-PRIVATE APIS //
    //////////////////////////

    private static final Grouper GROUPING_3 = new Grouper(B3, B3, false);
    private static final Grouper GROUPING_3_2 = new Grouper(B3, B2, false);
    private static final Grouper GROUPING_3_MIN2 = new Grouper(B3, B3, true);
    private static final Grouper GROUPING_3_2_MIN2 = new Grouper(B3, B2, true);

    static Grouper getInstance(byte grouping1, byte grouping2, boolean min2) {
        if (grouping1 == -1) {
            return NONE;
        } else if (!min2 && grouping1 == 3 && grouping2 == 3) {
            return GROUPING_3;
        } else if (!min2 && grouping1 == 3 && grouping2 == 2) {
            return GROUPING_3_2;
        } else if (min2 && grouping1 == 3 && grouping2 == 3) {
            return GROUPING_3_MIN2;
        } else if (min2 && grouping1 == 3 && grouping2 == 2) {
            return GROUPING_3_2_MIN2;
        } else {
            return new Grouper(grouping1, grouping2, min2);
        }
    }

    Grouper withLocaleData(ParsedPatternInfo patternInfo) {
        if (grouping1 != -2) {
            return this;
        }
        // TODO: short or byte?
        byte grouping1 = (byte) (patternInfo.positive.groupingSizes & 0xffff);
        byte grouping2 = (byte) ((patternInfo.positive.groupingSizes >>> 16) & 0xffff);
        byte grouping3 = (byte) ((patternInfo.positive.groupingSizes >>> 32) & 0xffff);
        if (grouping2 == -1) {
            grouping1 = -1;
        }
        if (grouping3 == -1) {
            grouping2 = grouping1;
        }
        return getInstance(grouping1, grouping2, min2);
    }

    boolean groupAtPosition(int position, DecimalQuantity value) {
        assert grouping1 != -2;
        if (grouping1 == -1 || grouping1 == 0) {
            // Either -1 or 0 means "no grouping"
            return false;
        }
        position -= grouping1;
        return position >= 0
                && (position % grouping2) == 0
                && value.getUpperDisplayMagnitude() - grouping1 + 1 >= (min2 ? 2 : 1);
    }
}