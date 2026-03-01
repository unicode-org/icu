// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.number.parse;

import java.util.Iterator;

import com.ibm.icu.impl.StandardPlural;
import com.ibm.icu.impl.StringSegment;
import com.ibm.icu.impl.TextTrieMap;
import com.ibm.icu.text.DecimalFormatSymbols;
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

    private final String[] localLongNames;

    private final String afterPrefixInsert;
    private final String beforeSuffixInsert;

    private final TextTrieMap<CurrencyStringInfo> longNameTrie;
    private final TextTrieMap<CurrencyStringInfo> symbolTrie;

    // TODO: See comments in constructor.
    // private final UnicodeSet leadCodePoints;

    public static CombinedCurrencyMatcher getInstance(Currency currency, DecimalFormatSymbols dfs, int parseFlags) {
        // TODO: Cache these instances. They are somewhat expensive.
        return new CombinedCurrencyMatcher(currency, dfs, parseFlags);
    }

    private CombinedCurrencyMatcher(Currency currency, DecimalFormatSymbols dfs, int parseFlags) {
        this.isoCode = currency.getSubtype();
        this.currency1 = currency.getSymbol(dfs.getULocale());
        this.currency2 = currency.getCurrencyCode();

        afterPrefixInsert = dfs.getPatternForCurrencySpacing(DecimalFormatSymbols.CURRENCY_SPC_INSERT,
                false);
        beforeSuffixInsert = dfs.getPatternForCurrencySpacing(DecimalFormatSymbols.CURRENCY_SPC_INSERT,
                true);

        if (0 == (parseFlags & ParsingUtils.PARSE_FLAG_NO_FOREIGN_CURRENCIES)) {
            // TODO: Currency trie does not currently have an option for case folding. It defaults to use
            // case folding on long-names but not symbols.
            longNameTrie = Currency.getParsingTrie(dfs.getULocale(), Currency.LONG_NAME);
            symbolTrie = Currency.getParsingTrie(dfs.getULocale(), Currency.SYMBOL_NAME);
            localLongNames = null;

        } else {
            longNameTrie = null;
            symbolTrie = null;
            localLongNames = new String[StandardPlural.COUNT];
            for (int i = 0; i < StandardPlural.COUNT; i++) {
                String pluralKeyword = StandardPlural.VALUES.get(i).getKeyword();
                localLongNames[i] = currency
                        .getName(dfs.getLocale(), Currency.PLURAL_LONG_NAME, pluralKeyword, null);
            }
        }

        // TODO: Figure out how to make this faster and re-enable.
        // Computing the "lead code points" set for fastpathing is too slow to use in production.
        // See http://bugs.icu-project.org/trac/ticket/13584
        // // Compute the full set of characters that could be the first in a currency to allow for
        // // efficient smoke test.
        // leadCodePoints = new UnicodeSet();
        // leadCodePoints.add(currency1.codePointAt(0));
        // leadCodePoints.add(currency2.codePointAt(0));
        // leadCodePoints.add(beforeSuffixInsert.codePointAt(0));
        // longNameTrie.putLeadCodePoints(leadCodePoints);
        // symbolTrie.putLeadCodePoints(leadCodePoints);
        // // Always apply case mapping closure for currencies
        // leadCodePoints.closeOver(UnicodeSet.ADD_CASE_MAPPINGS);
        // leadCodePoints.freeze();
    }

    @Override
    public boolean match(StringSegment segment, ParsedNumber result) {
        if (result.currencyCode != null) {
            return false;
        }

        // Try to match a currency spacing separator.
        int initialOffset = segment.getOffset();
        boolean maybeMore = false;
        if (result.seenNumber() && !beforeSuffixInsert.isEmpty()) {
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
        if (!result.seenNumber() && !afterPrefixInsert.isEmpty()) {
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
        boolean maybeMore = false;

        int overlap1;
        if (!currency1.isEmpty()) {
            overlap1 = segment.getCaseSensitivePrefixLength(currency1);
        } else {
            overlap1 = -1;
        }
        maybeMore = maybeMore || overlap1 == segment.length();
        if (overlap1 == currency1.length()) {
            result.currencyCode = isoCode;
            segment.adjustOffset(overlap1);
            result.setCharsConsumed(segment);
            return maybeMore;
        }

        int overlap2;
        if (!currency2.isEmpty()) {
            // ISO codes should be accepted case-insensitive.
            // https://unicode-org.atlassian.net/browse/ICU-13696
            overlap2 = segment.getCommonPrefixLength(currency2);
        } else {
            overlap2 = -1;
        }
        maybeMore = maybeMore || overlap2 == segment.length();
        if (overlap2 == currency2.length()) {
            result.currencyCode = isoCode;
            segment.adjustOffset(overlap2);
            result.setCharsConsumed(segment);
            return maybeMore;
        }

        if (longNameTrie != null) {
            // Use the full currency data.
            TextTrieMap.Output trieOutput = new TextTrieMap.Output();
            Iterator<CurrencyStringInfo> values = longNameTrie.get(segment, 0, trieOutput);
            maybeMore = maybeMore || trieOutput.partialMatch;
            if (values == null) {
                values = symbolTrie.get(segment, 0, trieOutput);
                maybeMore = maybeMore || trieOutput.partialMatch;
            }
            if (values != null) {
                result.currencyCode = values.next().getISOCode();
                segment.adjustOffset(trieOutput.matchLength);
                result.setCharsConsumed(segment);
                return maybeMore;
            }

        } else {
            // Use the locale long names.
            int longestFullMatch = 0;
            for (int i=0; i<StandardPlural.COUNT; i++) {
                String name = localLongNames[i];
                if (name.isEmpty()) {
                    continue;
                }
                int overlap = segment.getCommonPrefixLength(name);
                if (overlap == name.length() && name.length() > longestFullMatch) {
                    longestFullMatch = name.length();
                }
                maybeMore = maybeMore || overlap > 0;
            }
            if (longestFullMatch > 0) {
                result.currencyCode = isoCode;
                segment.adjustOffset(longestFullMatch);
                result.setCharsConsumed(segment);
                return maybeMore;
            }
        }

        // No match found.
        return maybeMore;
    }

    @Override
    public boolean smokeTest(StringSegment segment) {
        // TODO: See constructor
        return true;
        // return segment.startsWith(leadCodePoints);
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
