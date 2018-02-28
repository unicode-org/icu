// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.parse;

import java.util.EnumMap;
import java.util.Map;

import com.ibm.icu.text.UnicodeSet;

/**
 * This class statically initializes UnicodeSets useful for number parsing. Microbenchmarks show this to
 * bring a very sizeable performance boost.
 *
 * IMPORTANT ASSUMPTION: All of the sets contain code points (no strings) and they are all case-folded.
 * If this assumption were ever broken, logic in classes such as SymbolMatcher would need to be updated
 * in order to return well-formed sets upon calls to getLeadCodePoints().
 *
 * @author sffc
 */
public class UnicodeSetStaticCache {
    public static enum Key {
        // Ignorables
        BIDI,
        WHITESPACE,
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
        OTHER_GROUPING_SEPARATORS,
        ALL_SEPARATORS,
        STRICT_ALL_SEPARATORS,

        // Symbols
        // TODO: NaN?
        MINUS_SIGN,
        PLUS_SIGN,
        PERCENT_SIGN,
        PERMILLE_SIGN,
        INFINITY,

        // Other
        DIGITS,
        NAN_LEAD,
        SCIENTIFIC_LEAD,
        CWCF, // TODO: Check if this is being used and remove it if not.

        // Combined Separators with Digits (for lead code points)
        DIGITS_OR_ALL_SEPARATORS,
        DIGITS_OR_STRICT_ALL_SEPARATORS,
    };

    private static final Map<Key, UnicodeSet> unicodeSets = new EnumMap<Key, UnicodeSet>(Key.class);

    public static UnicodeSet get(Key key) {
        return unicodeSets.get(key);
    }

    public static Key chooseFrom(String str, Key key1) {
        return get(key1).contains(str) ? key1 : null;
    }

    public static Key chooseFrom(String str, Key key1, Key key2) {
        return get(key1).contains(str) ? key1 : chooseFrom(str, key2);
    }

    private static UnicodeSet computeUnion(Key k1, Key k2) {
        return new UnicodeSet().addAll(get(k1)).addAll(get(k2)).freeze();
    }

    private static UnicodeSet computeUnion(Key k1, Key k2, Key k3) {
        return new UnicodeSet().addAll(get(k1)).addAll(get(k2)).addAll(get(k3)).freeze();
    }

    static {
        // These characters are skipped over and ignored at any point in the string, even in strict mode.
        // See ticket #13084.
        unicodeSets.put(Key.BIDI, new UnicodeSet("[[:DI:]]").freeze());

        // This set was decided after discussion with icu-design@. See ticket #13309.
        // Zs+TAB is "horizontal whitespace" according to UTS #18 (blank property).
        unicodeSets.put(Key.WHITESPACE, new UnicodeSet("[[:Zs:][\\u0009]]").freeze());

        unicodeSets.put(Key.DEFAULT_IGNORABLES, computeUnion(Key.BIDI, Key.WHITESPACE));
        unicodeSets.put(Key.STRICT_IGNORABLES, get(Key.BIDI));

        // TODO: Re-generate these sets from the UCD. They probably haven't been updated in a while.
        unicodeSets.put(Key.COMMA, new UnicodeSet("[,،٫、︐︑﹐﹑，､]").freeze());
        unicodeSets.put(Key.STRICT_COMMA, new UnicodeSet("[,٫︐﹐，]").freeze());
        unicodeSets.put(Key.PERIOD, new UnicodeSet("[.․。︒﹒．｡]").freeze());
        unicodeSets.put(Key.STRICT_PERIOD, new UnicodeSet("[.․﹒．｡]").freeze());
        unicodeSets.put(Key.OTHER_GROUPING_SEPARATORS,
                new UnicodeSet("['٬‘’＇\\u0020\\u00A0\\u2000-\\u200A\\u202F\\u205F\\u3000]").freeze());
        unicodeSets.put(Key.ALL_SEPARATORS,
                computeUnion(Key.COMMA, Key.PERIOD, Key.OTHER_GROUPING_SEPARATORS));
        unicodeSets.put(Key.STRICT_ALL_SEPARATORS,
                computeUnion(Key.STRICT_COMMA, Key.STRICT_PERIOD, Key.OTHER_GROUPING_SEPARATORS));

        unicodeSets.put(Key.MINUS_SIGN, new UnicodeSet("[-⁻₋−➖﹣－]").freeze());
        unicodeSets.put(Key.PLUS_SIGN, new UnicodeSet("[+⁺₊➕﬩﹢＋]").freeze());

        unicodeSets.put(Key.PERCENT_SIGN, new UnicodeSet("[%٪]").freeze());
        unicodeSets.put(Key.PERMILLE_SIGN, new UnicodeSet("[‰؉]").freeze());
        unicodeSets.put(Key.INFINITY, new UnicodeSet("[∞]").freeze());

        unicodeSets.put(Key.DIGITS, new UnicodeSet("[:digit:]").freeze());
        unicodeSets.put(Key.NAN_LEAD,
                new UnicodeSet("[NnТтmeՈոс¤НнчTtsҳ\u975e\u1002\u0e9a\u10d0\u0f68\u0644\u0646]")
                        .freeze());
        unicodeSets.put(Key.SCIENTIFIC_LEAD, new UnicodeSet("[Ee×·е\u0627]").freeze());
        unicodeSets.put(Key.CWCF, new UnicodeSet("[:CWCF:]").freeze());

        unicodeSets.put(Key.DIGITS_OR_ALL_SEPARATORS, computeUnion(Key.DIGITS, Key.ALL_SEPARATORS));
        unicodeSets.put(Key.DIGITS_OR_STRICT_ALL_SEPARATORS,
                computeUnion(Key.DIGITS, Key.STRICT_ALL_SEPARATORS));
    }
}
