// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.parse;

import java.util.Iterator;

import com.ibm.icu.impl.StringSegment;
import com.ibm.icu.impl.TextTrieMap;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.Currency.CurrencyStringInfo;

/**
 * Matches a currency, either a custom currency or one from the data bundle. The class is called
 * "combined" to emphasize that the currency string may come from one of multiple sources.
 *
 * Will match currency spacing either before or after the number depending on whether we are currently in
 * the prefix or suffix.
 *
 * The implementation of this class is slightly different between J and C. See #13584 for a follow-up.
 *
 * @author sffc
 */
public class CombinedCurrencyMatcher implements NumberParseMatcher {

    private final String isoCode;
    private final String currency1;
    private final String currency2;

    private final String afterPrefixInsert;
    private final String beforeSuffixInsert;

    private final TextTrieMap<CurrencyStringInfo> longNameTrie;
    private final TextTrieMap<CurrencyStringInfo> symbolTrie;

    private final UnicodeSet leadCodePoints;

    public static CombinedCurrencyMatcher getInstance(Currency currency, DecimalFormatSymbols dfs) {
        // TODO: Cache these instances. They are somewhat expensive.
        return new CombinedCurrencyMatcher(currency, dfs);
    }

    private CombinedCurrencyMatcher(Currency currency, DecimalFormatSymbols dfs) {
        this.isoCode = currency.getSubtype();
        this.currency1 = currency.getSymbol(dfs.getULocale());
        this.currency2 = currency.getCurrencyCode();

        afterPrefixInsert = dfs
                .getPatternForCurrencySpacing(DecimalFormatSymbols.CURRENCY_SPC_INSERT, false);
        beforeSuffixInsert = dfs
                .getPatternForCurrencySpacing(DecimalFormatSymbols.CURRENCY_SPC_INSERT, true);

        // TODO: Currency trie does not currently have an option for case folding. It defaults to use
        // case folding on long-names but not symbols.
        longNameTrie = Currency.getParsingTrie(dfs.getULocale(), Currency.LONG_NAME);
        symbolTrie = Currency.getParsingTrie(dfs.getULocale(), Currency.SYMBOL_NAME);

        // Compute the full set of characters that could be the first in a currency to allow for
        // efficient smoke test.
        leadCodePoints = new UnicodeSet();
        leadCodePoints.add(currency1.codePointAt(0));
        leadCodePoints.add(currency2.codePointAt(0));
        leadCodePoints.add(beforeSuffixInsert.codePointAt(0));
        longNameTrie.putLeadCodePoints(leadCodePoints);
        symbolTrie.putLeadCodePoints(leadCodePoints);
        // Always apply case mapping closure for currencies
        leadCodePoints.closeOver(UnicodeSet.ADD_CASE_MAPPINGS);
        leadCodePoints.freeze();
    }

    @Override
    public boolean match(StringSegment segment, ParsedNumber result) {
        if (result.currencyCode != null) {
            return false;
        }

        // Try to match a currency spacing separator.
        int initialOffset = segment.getOffset();
        boolean maybeMore = false;
        if (result.seenNumber()) {
            int overlap = segment.getCommonPrefixLength(beforeSuffixInsert);
            if (overlap == beforeSuffixInsert.length()) {
                segment.adjustOffset(overlap);
                // Note: let currency spacing be a weak match. Don't update chars consumed.
            }
            maybeMore = maybeMore || overlap == segment.length();
        }

        // Match the currency string, and reset if we didn't find one.
        maybeMore = maybeMore || matchCurrency(segment, result);
        if (result.currencyCode == null) {
            segment.setOffset(initialOffset);
            return maybeMore;
        }

        // Try to match a currency spacing separator.
        if (!result.seenNumber()) {
            int overlap = segment.getCommonPrefixLength(afterPrefixInsert);
            if (overlap == afterPrefixInsert.length()) {
                segment.adjustOffset(overlap);
                // Note: let currency spacing be a weak match. Don't update chars consumed.
            }
            maybeMore = maybeMore || overlap == segment.length();
        }

        return maybeMore;
    }

    /** Matches the currency string without concern for currency spacing. */
    private boolean matchCurrency(StringSegment segment, ParsedNumber result) {
        int overlap1 = segment.getCaseSensitivePrefixLength(currency1);
        if (overlap1 == currency1.length()) {
            result.currencyCode = isoCode;
            segment.adjustOffset(overlap1);
            result.setCharsConsumed(segment);
            return segment.length() == 0;
        }

        int overlap2 = segment.getCaseSensitivePrefixLength(currency2);
        if (overlap2 == currency2.length()) {
            result.currencyCode = isoCode;
            segment.adjustOffset(overlap2);
            result.setCharsConsumed(segment);
            return segment.length() == 0;
        }

        TextTrieMap.Output trieOutput = new TextTrieMap.Output();
        Iterator<CurrencyStringInfo> values = longNameTrie.get(segment, 0, trieOutput);
        if (values == null) {
            values = symbolTrie.get(segment, 0, trieOutput);
        }
        if (values != null) {
            result.currencyCode = values.next().getISOCode();
            segment.adjustOffset(trieOutput.matchLength);
            result.setCharsConsumed(segment);
        }

        return overlap1 == segment.length() || overlap2 == segment.length() || trieOutput.partialMatch;
    }

    @Override
    public boolean smokeTest(StringSegment segment) {
        return segment.startsWith(leadCodePoints);
    }

    @Override
    public void postProcess(ParsedNumber result) {
        // No-op
    }

    @Override
    public String toString() {
        return "<CombinedCurrencyMatcher " + isoCode + ">";
    }

}
