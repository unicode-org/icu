// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package newapi;

import com.ibm.icu.text.CompactDecimalFormat.CompactStyle;

import newapi.NumberFormatter.SignDisplay;

public class Notation {

    // FIXME: Support engineering intervals other than 3?
    private static final ScientificNotation SCIENTIFIC = new ScientificNotation(1, false, 1, SignDisplay.AUTO);
    private static final ScientificNotation ENGINEERING = new ScientificNotation(3, false, 1, SignDisplay.AUTO);
    private static final CompactNotation COMPACT_SHORT = new CompactNotation(CompactStyle.SHORT);
    private static final CompactNotation COMPACT_LONG = new CompactNotation(CompactStyle.LONG);
    private static final SimpleNotation SIMPLE = new SimpleNotation();

    /* package-private */ Notation() {
    }

    public static ScientificNotation scientific() {
        return SCIENTIFIC;
    }

    public static ScientificNotation engineering() {
        return ENGINEERING;
    }

    public static CompactNotation compactShort() {
        return COMPACT_SHORT;
    }

    public static CompactNotation compactLong() {
        return COMPACT_LONG;
    }

    public static SimpleNotation simple() {
        return SIMPLE;
    }
}