// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.parse;

import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.ULocale;

/**
 * Small helper class that generates matchers for SeriesMatcher.
 *
 * @author sffc
 */
public class MatcherFactory {
    public Currency currency;
    public DecimalFormatSymbols symbols;
    public IgnorablesMatcher ignorables;
    public ULocale locale;

    public MinusSignMatcher minusSign(boolean allowTrailing) {
        return MinusSignMatcher.getInstance(symbols, allowTrailing);
    }

    public PlusSignMatcher plusSign(boolean allowTrailing) {
        return PlusSignMatcher.getInstance(symbols, allowTrailing);
    }

    public PercentMatcher percent() {
        return PercentMatcher.getInstance(symbols);
    }

    public PermilleMatcher permille() {
        return PermilleMatcher.getInstance(symbols);
    }

    public AnyMatcher currency() {
        AnyMatcher any = new AnyMatcher();
        any.addMatcher(CurrencyCustomMatcher.getInstance(currency, locale));
        any.addMatcher(CurrencyNamesMatcher.getInstance(locale));
        any.freeze();
        return any;
    }

    public IgnorablesMatcher ignorables() {
        return ignorables;
    }
}
