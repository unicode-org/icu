// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.number.parse;

import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.ULocale;

/**
 * Small helper class that generates matchers for individual tokens for AffixPatternMatcher.
 *
 * @author sffc
 */
public class AffixTokenMatcherFactory {
    public Currency currency;
    public DecimalFormatSymbols symbols;
    public IgnorablesMatcher ignorables;
    public ULocale locale;
    public int parseFlags;

    public MinusSignMatcher minusSign() {
        return MinusSignMatcher.getInstance(symbols, true);
    }

    public PlusSignMatcher plusSign() {
        return PlusSignMatcher.getInstance(symbols, true);
    }

    public PercentMatcher percent() {
        return PercentMatcher.getInstance(symbols);
    }

    public PermilleMatcher permille() {
        return PermilleMatcher.getInstance(symbols);
    }

    public CombinedCurrencyMatcher currency() {
        return CombinedCurrencyMatcher.getInstance(currency, symbols, parseFlags);
    }

    public IgnorablesMatcher ignorables() {
        return ignorables;
    }
}
