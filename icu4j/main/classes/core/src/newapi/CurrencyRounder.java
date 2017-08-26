// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package newapi;

import com.ibm.icu.util.Currency;
import com.ibm.icu.util.Currency.CurrencyUsage;
import com.ibm.icu.util.CurrencyAmount;
import com.ibm.icu.util.Measure;
import com.ibm.icu.util.MeasureUnit;

/** A rounding strategy parameterized by a currency. */
public abstract class CurrencyRounder extends Rounder {

    /* package-private */ CurrencyRounder() {
    }

    /**
     * Associates a {@link com.ibm.icu.util.Currency} with this rounding strategy. Only applies to rounding strategies
     * returned from {@link #currency(CurrencyUsage)}.
     *
     * <p>
     * <strong>Calling this method is <em>not required</em></strong>, because the currency specified in
     * {@link NumberFormatter#unit(MeasureUnit)} or via a {@link CurrencyAmount} passed into
     * {@link LocalizedNumberFormatter#format(Measure)} is automatically applied to currency rounding strategies.
     * However, this method enables you to override that automatic association.
     *
     * <p>
     * This method also enables numbers to be formatted using currency rounding rules without explicitly using a
     * currency format.
     *
     * @param currency
     *            The currency to associate with this rounding strategy.
     * @return An immutable object for chaining.
     */
    public Rounder withCurrency(Currency currency) {
        if (currency != null) {
            return constructFromCurrency(this, currency);
        } else {
            throw new IllegalArgumentException("Currency must not be null");
        }
    };
}