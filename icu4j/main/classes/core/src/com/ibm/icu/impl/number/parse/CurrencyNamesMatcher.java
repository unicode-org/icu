// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.parse;

import java.util.Iterator;

import com.ibm.icu.impl.StringSegment;
import com.ibm.icu.impl.TextTrieMap;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.Currency.CurrencyStringInfo;
import com.ibm.icu.util.ULocale;

/**
 * Matches currencies according to all available strings in locale data.
 *
 * The implementation of this class is different between J and C. See #13584 for a follow-up.
 *
 * @author sffc
 */
public class CurrencyNamesMatcher implements NumberParseMatcher {

    private final TextTrieMap<CurrencyStringInfo> longNameTrie;
    private final TextTrieMap<CurrencyStringInfo> symbolTrie;

    private final UnicodeSet leadCodePoints;

    public static CurrencyNamesMatcher getInstance(ULocale locale) {
        // TODO: Pre-compute some of the more popular locales?
        return new CurrencyNamesMatcher(locale);
    }

    private CurrencyNamesMatcher(ULocale locale) {
        // TODO: Currency trie does not currently have an option for case folding. It defaults to use
        // case folding on long-names but not symbols.
        longNameTrie = Currency.getParsingTrie(locale, Currency.LONG_NAME);
        symbolTrie = Currency.getParsingTrie(locale, Currency.SYMBOL_NAME);

        // Compute the full set of characters that could be the first in a currency to allow for
        // efficient smoke test.
        leadCodePoints = new UnicodeSet();
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
        return trieOutput.partialMatch;
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
        return "<CurrencyTrieMatcher>";
    }
}
