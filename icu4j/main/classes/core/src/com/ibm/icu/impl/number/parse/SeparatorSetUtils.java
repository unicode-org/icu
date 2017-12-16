// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.parse;

import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.UnicodeSet;

/**
 * @author sffc
 *
 */
public class SeparatorSetUtils {

    // TODO: Re-generate these sets from the database. They probably haven't been updated in a while.

    static final UnicodeSet COMMA_LIKE = new UnicodeSet(
            "[,\\u060C\\u066B\\u3001\\uFE10\\uFE11\\uFE50\\uFE51\\uFF0C\\uFF64]").freeze();

    static final UnicodeSet STRICT_COMMA_LIKE = new UnicodeSet("[,\\u066B\\uFE10\\uFE50\\uFF0C]").freeze();

    static final UnicodeSet PERIOD_LIKE = new UnicodeSet("[.\\u2024\\u3002\\uFE12\\uFE52\\uFF0E\\uFF61]").freeze();

    static final UnicodeSet STRICT_PERIOD_LIKE = new UnicodeSet("[.\\u2024\\uFE52\\uFF0E\\uFF61]").freeze();

    static final UnicodeSet OTHER_GROUPING_SEPARATORS = new UnicodeSet(
            "[\\ '\\u00A0\\u066C\\u2000-\\u200A\\u2018\\u2019\\u202F\\u205F\\u3000\\uFF07]").freeze();

    static final UnicodeSet COMMA_OR_PERIOD_LIKE = new UnicodeSet().addAll(COMMA_LIKE).addAll(PERIOD_LIKE).freeze();

    static final UnicodeSet STRICT_COMMA_OR_PERIOD_LIKE = new UnicodeSet().addAll(STRICT_COMMA_LIKE)
            .addAll(STRICT_PERIOD_LIKE).freeze();

    static final UnicodeSet COMMA_LIKE_OR_OTHER = new UnicodeSet().addAll(COMMA_LIKE).addAll(OTHER_GROUPING_SEPARATORS)
            .freeze();

    static final UnicodeSet STRICT_COMMA_LIKE_OR_OTHER = new UnicodeSet().addAll(STRICT_COMMA_LIKE)
            .addAll(OTHER_GROUPING_SEPARATORS).freeze();

    static final UnicodeSet PERIOD_LIKE_OR_OTHER = new UnicodeSet().addAll(PERIOD_LIKE)
            .addAll(OTHER_GROUPING_SEPARATORS).freeze();

    static final UnicodeSet STRICT_PERIOD_LIKE_OR_OTHER = new UnicodeSet().addAll(STRICT_PERIOD_LIKE)
            .addAll(OTHER_GROUPING_SEPARATORS).freeze();

    static final UnicodeSet COMMA_OR_PERIOD_LIKE_OR_OTHER = new UnicodeSet().addAll(COMMA_LIKE).addAll(PERIOD_LIKE)
            .addAll(OTHER_GROUPING_SEPARATORS).freeze();

    static final UnicodeSet STRICT_COMMA_OR_PERIOD_LIKE_OR_OTHER = new UnicodeSet().addAll(STRICT_COMMA_LIKE)
            .addAll(STRICT_PERIOD_LIKE).addAll(OTHER_GROUPING_SEPARATORS).freeze();

    public static UnicodeSet getGroupingUnicodeSet(DecimalFormatSymbols symbols, boolean isStrict) {
        if (isStrict) {
            return chooseUnicodeSet(symbols.getGroupingSeparatorString(),
                    STRICT_COMMA_LIKE_OR_OTHER,
                    STRICT_PERIOD_LIKE_OR_OTHER,
                    OTHER_GROUPING_SEPARATORS);
        } else {
            return chooseUnicodeSet(symbols.getGroupingSeparatorString(),
                    COMMA_LIKE_OR_OTHER,
                    PERIOD_LIKE_OR_OTHER,
                    OTHER_GROUPING_SEPARATORS);
        }
    }

    public static UnicodeSet getDecimalUnicodeSet(DecimalFormatSymbols symbols, boolean isStrict) {
        if (isStrict) {
            return chooseUnicodeSet(symbols.getDecimalSeparatorString(), STRICT_COMMA_LIKE, STRICT_PERIOD_LIKE);
        } else {
            return chooseUnicodeSet(symbols.getDecimalSeparatorString(), COMMA_LIKE, PERIOD_LIKE);
        }
    }

    private static UnicodeSet chooseUnicodeSet(String str, UnicodeSet set1) {
        return set1.contains(str) ? set1 : new UnicodeSet().add(str).freeze();
    }

    private static UnicodeSet chooseUnicodeSet(String str, UnicodeSet set1, UnicodeSet set2) {
        return set1.contains(str) ? set1 : chooseUnicodeSet(str, set2);
    }

    private static UnicodeSet chooseUnicodeSet(String str, UnicodeSet set1, UnicodeSet set2, UnicodeSet set3) {
        return set1.contains(str) ? set1 : chooseUnicodeSet(str, set2, set3);
    }

    public static UnicodeSet unionUnicodeSets(UnicodeSet set1, UnicodeSet set2) {
        // Note: == operators should be okay here since non-static UnicodeSets happen only in fallback cases.
        if (set1 == UnicodeSet.EMPTY && set2 == UnicodeSet.EMPTY) {
            return UnicodeSet.EMPTY;
        } else if (set1 == COMMA_LIKE_OR_OTHER && set2 == PERIOD_LIKE_OR_OTHER) {
            return COMMA_OR_PERIOD_LIKE_OR_OTHER;
        } else if (set1 == PERIOD_LIKE_OR_OTHER && set2 == COMMA_LIKE_OR_OTHER) {
            return COMMA_OR_PERIOD_LIKE_OR_OTHER;
        } else if (set1 == STRICT_COMMA_LIKE_OR_OTHER && set2 == STRICT_PERIOD_LIKE_OR_OTHER) {
            return STRICT_COMMA_OR_PERIOD_LIKE_OR_OTHER;
        } else if (set1 == STRICT_PERIOD_LIKE_OR_OTHER && set2 == STRICT_COMMA_LIKE_OR_OTHER) {
            return STRICT_COMMA_OR_PERIOD_LIKE_OR_OTHER;
        } else if (set1 == COMMA_LIKE && set2 == PERIOD_LIKE) {
            return COMMA_OR_PERIOD_LIKE;
        } else if (set1 == PERIOD_LIKE && set2 == COMMA_LIKE) {
            return COMMA_OR_PERIOD_LIKE;
        } else if (set1 == STRICT_COMMA_LIKE && set2 == STRICT_PERIOD_LIKE) {
            return STRICT_COMMA_OR_PERIOD_LIKE;
        } else if (set1 == STRICT_PERIOD_LIKE && set2 == STRICT_COMMA_LIKE) {
            return STRICT_COMMA_OR_PERIOD_LIKE;
        } else {
            return set1.cloneAsThawed().addAll(set2).freeze();
        }
    }
}
