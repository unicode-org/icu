// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl;

import static com.ibm.icu.impl.number.parse.ParsingUtils.safeContains;

import java.util.EnumMap;
import java.util.Map;

import com.ibm.icu.impl.UResource.Value;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;

/**
 * This class statically initializes UnicodeSets, originally built for number parsing. Microbenchmarks
 * show this to bring a very sizeable performance boost.
 *
 * IMPORTANT ASSUMPTION FOR NUMBER PARSING: All of the sets contain code points (no strings) and they are
 * all case-folded. If this assumption were ever broken, logic in classes such as SymbolMatcher would
 * need to be updated in order to return well-formed sets upon calls to getLeadCodePoints().
 *
 * @author sffc
 */
public class StaticUnicodeSets {
    public static enum Key {
        EMPTY,
        // Ignorables
        DEFAULT_IGNORABLES,
        STRICT_IGNORABLES,

        // Separators
        // Notes:
        // - COMMA is a superset of STRICT_COMMA
        // - PERIOD is a superset of SCRICT_PERIOD
        // - ALL_SEPARATORS is the union of COMMA, PERIOD, and OTHER_GROUPING_SEPARATORS
        // - STRICT_ALL_SEPARATORS is the union of STRICT_COMMA, STRICT_PERIOD, and OTHER_GRP_SEPARATORS
        COMMA,
        PERIOD,
        STRICT_COMMA,
        STRICT_PERIOD,
        APOSTROPHE_SIGN,
        OTHER_GROUPING_SEPARATORS,
        ALL_SEPARATORS,
        STRICT_ALL_SEPARATORS,

        // Symbols
        // TODO: NaN?
        MINUS_SIGN,
        PLUS_SIGN,
        PERCENT_SIGN,
        PERMILLE_SIGN,
        INFINITY_SIGN,

        // Currency Symbols
        DOLLAR_SIGN,
        POUND_SIGN,
        RUPEE_SIGN,
        YEN_SIGN,
        WON_SIGN,

        // Other
        DIGITS,

        // Combined Separators with Digits (for lead code points)
        DIGITS_OR_ALL_SEPARATORS,
        DIGITS_OR_STRICT_ALL_SEPARATORS,
    };

    private static final Map<Key, UnicodeSet> unicodeSets = new EnumMap<>(Key.class);

    /**
     * Gets the static-allocated UnicodeSet according to the provided key.
     *
     * @param key
     *            The desired UnicodeSet according to the enum in this file.
     * @return The requested UnicodeSet. Guaranteed to be frozen and non-null, but may be empty if an
     *         error occurred during data loading.
     */
    public static UnicodeSet get(Key key) {
        UnicodeSet candidate = unicodeSets.get(key);
        if (candidate == null) {
            return UnicodeSet.EMPTY;
        }
        return candidate;
    }

    /**
     * Checks if the UnicodeSet given by key1 contains the given string.
     *
     * @param str
     *            The string to check.
     * @param key1
     *            The set to check.
     * @return key1 if the set contains str, or COUNT if not.
     */
    public static Key chooseFrom(String str, Key key1) {
        return safeContains(get(key1), str) ? key1 : null;
    }

    /**
     * Checks if the UnicodeSet given by either key1 or key2 contains the string.
     *
     * Exported as U_COMMON_API for numparse_decimal.cpp
     *
     * @param str
     *            The string to check.
     * @param key1
     *            The first set to check.
     * @param key2
     *            The second set to check.
     * @return key1 if that set contains str; key2 if that set contains str; or COUNT if neither set
     *         contains str.
     */
    public static Key chooseFrom(String str, Key key1, Key key2) {
        return safeContains(get(key1), str) ? key1 : chooseFrom(str, key2);
    }

    /**
     * Looks through all Currency-related sets for the given string, returning the first match or null if
     * no match was round.
     */
    public static Key chooseCurrency(String str) {
        if (get(Key.DOLLAR_SIGN).contains(str)) {
            return Key.DOLLAR_SIGN;
        } else if (get(Key.POUND_SIGN).contains(str)) {
            return Key.POUND_SIGN;
        } else if (get(Key.RUPEE_SIGN).contains(str)) {
            return Key.RUPEE_SIGN;
        } else if (get(Key.YEN_SIGN).contains(str)) {
            return Key.YEN_SIGN;
        } else if (get(Key.WON_SIGN).contains(str)) {
            return Key.WON_SIGN;
        } else {
            return null;
        }
    }

    private static UnicodeSet computeUnion(Key k1, Key k2) {
        return new UnicodeSet().addAll(get(k1)).addAll(get(k2)).freeze();
    }

    private static UnicodeSet computeUnion(Key k1, Key k2, Key k3) {
        return new UnicodeSet().addAll(get(k1)).addAll(get(k2)).addAll(get(k3)).freeze();
    }

    private static void saveSet(Key key, String unicodeSetPattern) {
        assert unicodeSets.get(key) == null;
        unicodeSets.put(key, new UnicodeSet(unicodeSetPattern).freeze());
    }

    /*
    parse{
        date{
            lenient{
                "[\\--/]",
                "[\\:∶]",
            }
        }
        general{
            lenient{
                "[.․。︒﹒．｡]",
                "[\$﹩＄$]",
                "[£₤]",
                "[₨₹{Rp}{Rs}]",
            }
        }
        number{
            lenient{
                "[\\-‒⁻₋−➖﹣－]",
                "[,،٫、︐︑﹐﹑，､]",
                "[+⁺₊➕﬩﹢＋]",
            }
            stricter{
                "[,٫︐﹐，]",
                "[.․﹒．｡]",
            }
        }
    }
     */
    static class ParseDataSink extends UResource.Sink {
        @Override
        public void put(com.ibm.icu.impl.UResource.Key key, Value value, boolean noFallback) {
            UResource.Table contextsTable = value.getTable();
            for (int i = 0; contextsTable.getKeyAndValue(i, key, value); i++) {
                if (key.contentEquals("date")) {
                    // ignore
                } else {
                    assert key.contentEquals("general") || key.contentEquals("number");
                    UResource.Table strictnessTable = value.getTable();
                    for (int j = 0; strictnessTable.getKeyAndValue(j, key, value); j++) {
                        boolean isLenient = key.contentEquals("lenient");
                        UResource.Array array = value.getArray();
                        for (int k = 0; k < array.getSize(); k++) {
                            array.getValue(k, value);
                            String str = value.toString();
                            // There is both lenient and strict data for comma/period,
                            // but not for any of the other symbols.
                            if (str.indexOf('.') != -1) {
                                saveSet(isLenient ? Key.PERIOD : Key.STRICT_PERIOD, str);
                            } else if (str.indexOf(',') != -1) {
                                saveSet(isLenient ? Key.COMMA : Key.STRICT_COMMA, str);
                            } else if (str.indexOf('+') != -1) {
                                saveSet(Key.PLUS_SIGN, str);
                            } else if (str.indexOf('-') != -1) {
                                saveSet(Key.MINUS_SIGN, str);
                            } else if (str.indexOf('$') != -1) {
                                saveSet(Key.DOLLAR_SIGN, str);
                            } else if (str.indexOf('£') != -1) {
                                saveSet(Key.POUND_SIGN, str);
                            } else if (str.indexOf('₹') != -1) {
                                saveSet(Key.RUPEE_SIGN, str);
                            } else if (str.indexOf('¥') != -1) {
                                saveSet(Key.YEN_SIGN, str);
                            } else if (str.indexOf('₩') != -1) {
                                saveSet(Key.WON_SIGN, str);
                            } else if (str.indexOf('%') != -1) {
                                saveSet(Key.PERCENT_SIGN, str);
                            } else if (str.indexOf('‰') != -1) {
                                saveSet(Key.PERMILLE_SIGN, str);
                            } else if (str.indexOf('’') != -1) {
                                saveSet(Key.APOSTROPHE_SIGN, str);
                            } else {
                                // TODO(ICU-20428): Make ICU automatically accept new classes?
                                throw new AssertionError("Unknown class of parse lenients: " + str);
                            }
                        }
                    }
                }
            }
        }
    }

    static {
        unicodeSets.put(Key.EMPTY, new UnicodeSet("[]").freeze());
        // These sets were decided after discussion with icu-design@. See tickets #13084 and #13309.
        // Zs+TAB is "horizontal whitespace" according to UTS #18 (blank property).
        unicodeSets.put(Key.DEFAULT_IGNORABLES,
                new UnicodeSet("[[:Zs:][\\u0009][:Bidi_Control:][:Variation_Selector:]]").freeze());
        unicodeSets.put(Key.STRICT_IGNORABLES, new UnicodeSet("[[:Bidi_Control:]]").freeze());

        // CLDR provides data for comma, period, minus sign, and plus sign.
        ICUResourceBundle rb = (ICUResourceBundle) UResourceBundle
                .getBundleInstance(ICUData.ICU_BASE_NAME, ULocale.ROOT);
        rb.getAllItemsWithFallback("parse", new ParseDataSink());

        // NOTE: It is OK for these assertions to fail if there was a no-data build.
        assert unicodeSets.containsKey(Key.COMMA);
        assert unicodeSets.containsKey(Key.STRICT_COMMA);
        assert unicodeSets.containsKey(Key.PERIOD);
        assert unicodeSets.containsKey(Key.STRICT_PERIOD);
        assert unicodeSets.containsKey(Key.APOSTROPHE_SIGN);

        UnicodeSet otherGrouping = new UnicodeSet(
                "[٬‘\\u0020\\u00A0\\u2000-\\u200A\\u202F\\u205F\\u3000]");
        otherGrouping.addAll(unicodeSets.get(Key.APOSTROPHE_SIGN));
        unicodeSets.put(Key.OTHER_GROUPING_SEPARATORS, otherGrouping.freeze());
        unicodeSets.put(Key.ALL_SEPARATORS,
                computeUnion(Key.COMMA, Key.PERIOD, Key.OTHER_GROUPING_SEPARATORS));
        unicodeSets.put(Key.STRICT_ALL_SEPARATORS,
                computeUnion(Key.STRICT_COMMA, Key.STRICT_PERIOD, Key.OTHER_GROUPING_SEPARATORS));

        assert unicodeSets.containsKey(Key.MINUS_SIGN);
        assert unicodeSets.containsKey(Key.PLUS_SIGN);
        assert unicodeSets.containsKey(Key.PERCENT_SIGN);
        assert unicodeSets.containsKey(Key.PERMILLE_SIGN);

        unicodeSets.put(Key.INFINITY_SIGN, new UnicodeSet("[∞]").freeze());

        assert unicodeSets.containsKey(Key.DOLLAR_SIGN);
        assert unicodeSets.containsKey(Key.POUND_SIGN);
        assert unicodeSets.containsKey(Key.RUPEE_SIGN);
        assert unicodeSets.containsKey(Key.YEN_SIGN);
        assert unicodeSets.containsKey(Key.WON_SIGN);

        unicodeSets.put(Key.DIGITS, new UnicodeSet("[:digit:]").freeze());

        unicodeSets.put(Key.DIGITS_OR_ALL_SEPARATORS, computeUnion(Key.DIGITS, Key.ALL_SEPARATORS));
        unicodeSets.put(Key.DIGITS_OR_STRICT_ALL_SEPARATORS,
                computeUnion(Key.DIGITS, Key.STRICT_ALL_SEPARATORS));
    }
}
