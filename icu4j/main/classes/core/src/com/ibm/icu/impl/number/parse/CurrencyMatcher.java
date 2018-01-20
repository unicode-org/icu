// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.parse;

import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.ULocale;

/**
 * A matcher for a single currency instance (not the full trie).
 */
public class CurrencyMatcher implements NumberParseMatcher {

    private final String isoCode;
    private final String currency1;
    private final String currency2;

    public static CurrencyMatcher getInstance(Currency currency, ULocale loc, int setupFlags) {
        return new CurrencyMatcher(currency.getSubtype(),
                ParsingUtils.maybeFold(currency.getSymbol(loc), setupFlags),
                ParsingUtils.maybeFold(currency.getCurrencyCode(), setupFlags));
    }

    private CurrencyMatcher(String isoCode, String currency1, String currency2) {
        this.isoCode = isoCode;
        this.currency1 = currency1;
        this.currency2 = currency2;
    }

    @Override
    public boolean match(StringSegment segment, ParsedNumber result) {
        if (result.currencyCode != null) {
            return false;
        }

        int overlap1 = segment.getCommonPrefixLength(currency1);
        if (overlap1 == currency1.length()) {
            result.currencyCode = isoCode;
            segment.adjustOffset(overlap1);
            result.setCharsConsumed(segment);
        }

        int overlap2 = segment.getCommonPrefixLength(currency2);
        if (overlap2 == currency2.length()) {
            result.currencyCode = isoCode;
            segment.adjustOffset(overlap2);
            result.setCharsConsumed(segment);
        }

        return overlap1 == segment.length() || overlap2 == segment.length();
    }

    @Override
    public UnicodeSet getLeadCodePoints() {
        UnicodeSet leadCodePoints = new UnicodeSet();
        ParsingUtils.putLeadCodePoint(currency1, leadCodePoints);
        ParsingUtils.putLeadCodePoint(currency2, leadCodePoints);
        return leadCodePoints.freeze();
    }

    @Override
    public boolean matchesEmpty() {
        return false;
    }

    @Override
    public void postProcess(ParsedNumber result) {
        // No-op
    }

    @Override
    public String toString() {
        return "<CurrencyMatcher " + isoCode + ">";
    }
}
