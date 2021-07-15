// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.number;

import com.ibm.icu.impl.ICUData;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.number.PatternStringParser.ParsedPatternInfo;
import com.ibm.icu.number.NumberFormatter.GroupingStrategy;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;

/**
 * A full options object for grouping sizes.
 */
public class Grouper {

    private static final Grouper GROUPER_NEVER = new Grouper((short) -1, (short) -1, (short) -2);
    private static final Grouper GROUPER_MIN2 = new Grouper((short) -2, (short) -2, (short) -3);
    private static final Grouper GROUPER_AUTO = new Grouper((short) -2, (short) -2, (short) -2);
    private static final Grouper GROUPER_ON_ALIGNED = new Grouper((short) -4, (short) -4, (short) 1);

    private static final Grouper GROUPER_WESTERN = new Grouper((short) 3, (short) 3, (short) 1);
    private static final Grouper GROUPER_INDIC = new Grouper((short) 3, (short) 2, (short) 1);
    private static final Grouper GROUPER_WESTERN_MIN2 = new Grouper((short) 3, (short) 3, (short) 2);
    private static final Grouper GROUPER_INDIC_MIN2 = new Grouper((short) 3, (short) 2, (short) 2);

    /**
     * Convert from the GroupingStrategy enum to a Grouper object.
     */
    public static Grouper forStrategy(GroupingStrategy grouping) {
        switch (grouping) {
        case OFF:
            return GROUPER_NEVER;
        case MIN2:
            return GROUPER_MIN2;
        case AUTO:
            return GROUPER_AUTO;
        case ON_ALIGNED:
            return GROUPER_ON_ALIGNED;
        case THOUSANDS:
            return GROUPER_WESTERN;
        default:
            throw new AssertionError();
        }
    }

    /**
     * Resolve the values in Properties to a Grouper object.
     */
    public static Grouper forProperties(DecimalFormatProperties properties) {
        if (!properties.getGroupingUsed()) {
            return GROUPER_NEVER;
        }
        short grouping1 = (short) properties.getGroupingSize();
        short grouping2 = (short) properties.getSecondaryGroupingSize();
        short minGrouping = (short) properties.getMinimumGroupingDigits();
        grouping1 = grouping1 > 0 ? grouping1 : grouping2 > 0 ? grouping2 : grouping1;
        grouping2 = grouping2 > 0 ? grouping2 : grouping1;
        return getInstance(grouping1, grouping2, minGrouping);
    }

    public static Grouper getInstance(short grouping1, short grouping2, short minGrouping) {
        if (grouping1 == -1) {
            return GROUPER_NEVER;
        } else if (grouping1 == 3 && grouping2 == 3 && minGrouping == 1) {
            return GROUPER_WESTERN;
        } else if (grouping1 == 3 && grouping2 == 2 && minGrouping == 1) {
            return GROUPER_INDIC;
        } else if (grouping1 == 3 && grouping2 == 3 && minGrouping == 2) {
            return GROUPER_WESTERN_MIN2;
        } else if (grouping1 == 3 && grouping2 == 2 && minGrouping == 2) {
            return GROUPER_INDIC_MIN2;
        } else {
            return new Grouper(grouping1, grouping2, minGrouping);
        }
    }

    private static short getMinGroupingForLocale(ULocale locale) {
        // TODO: Cache this?
        ICUResourceBundle resource = (ICUResourceBundle) UResourceBundle
                .getBundleInstance(ICUData.ICU_BASE_NAME, locale);
        String result = resource.getStringWithFallback("NumberElements/minimumGroupingDigits");
        return Short.valueOf(result);
    }

    /**
     * The primary grouping size, with the following special values:
     * <ul>
     * <li>-1 = no grouping
     * <li>-2 = needs locale data
     * <li>-4 = fall back to Western grouping if not in locale
     * </ul>
     */
    private final short grouping1;

    /**
     * The secondary grouping size, with the following special values:
     * <ul>
     * <li>-1 = no grouping
     * <li>-2 = needs locale data
     * <li>-4 = fall back to Western grouping if not in locale
     * </ul>
     */
    private final short grouping2;

    /**
     * The minimum gropuing size, with the following special values:
     * <ul>
     * <li>-2 = needs locale data
     * <li>-3 = no less than 2
     * </ul>
     */
    private final short minGrouping;

    private Grouper(short grouping1, short grouping2, short minGrouping) {
        this.grouping1 = grouping1;
        this.grouping2 = grouping2;
        this.minGrouping = minGrouping;
    }

    public Grouper withLocaleData(ULocale locale, ParsedPatternInfo patternInfo) {
        short minGrouping;
        if (this.minGrouping == -2) {
            minGrouping = getMinGroupingForLocale(locale);
        } else if (this.minGrouping == -3) {
            minGrouping = (short) Math.max(2, getMinGroupingForLocale(locale));
        } else {
            minGrouping = this.minGrouping;
        }

        if (this.grouping1 != -2 && this.grouping2 != -4) {
            if (minGrouping == this.minGrouping) {
              return this;
            }
            return getInstance(this.grouping1, this.grouping2, minGrouping);
        }

        short grouping1 = (short) (patternInfo.positive.groupingSizes & 0xffff);
        short grouping2 = (short) ((patternInfo.positive.groupingSizes >>> 16) & 0xffff);
        short grouping3 = (short) ((patternInfo.positive.groupingSizes >>> 32) & 0xffff);
        if (grouping2 == -1) {
            grouping1 = this.grouping1 == -4 ? (short) 3 : (short) -1;
        }
        if (grouping3 == -1) {
            grouping2 = grouping1;
        }

        return getInstance(grouping1, grouping2, minGrouping);
    }

    public boolean groupAtPosition(int position, DecimalQuantity value) {
        assert grouping1 != -2 && grouping1 != -4;
        if (grouping1 == -1 || grouping1 == 0) {
            // Either -1 or 0 means "no grouping"
            return false;
        }
        position -= grouping1;
        return position >= 0
                && (position % grouping2) == 0
                && value.getUpperDisplayMagnitude() - grouping1 + 1 >= minGrouping;
    }

    public short getPrimary() {
        return grouping1;
    }

    public short getSecondary() {
        return grouping2;
    }
}
