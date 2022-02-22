// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.number;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

import com.ibm.icu.impl.number.DecimalQuantity;
import com.ibm.icu.impl.number.MultiplierProducer;
import com.ibm.icu.impl.number.RoundingUtils;
import com.ibm.icu.number.NumberFormatter.RoundingPriority;
import com.ibm.icu.number.NumberFormatter.TrailingZeroDisplay;
import com.ibm.icu.text.PluralRules.Operand;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.Currency.CurrencyUsage;

/**
 * A class that defines the rounding precision to be used when formatting numbers in NumberFormatter.
 *
 * <p>
 * To create a Precision, use one of the factory methods.
 *
 * @stable ICU 62
 * @see NumberFormatter
 */
public abstract class Precision {

    /* package-private final */ MathContext mathContext;
    /* package-private final */ TrailingZeroDisplay trailingZeroDisplay;

    /* package-private */ Precision() {
        mathContext = RoundingUtils.DEFAULT_MATH_CONTEXT_UNLIMITED;
    }

    /**
     * Show all available digits to full precision.
     *
     * <p>
     * <strong>NOTE:</strong> When formatting a <em>double</em>, this method, along with
     * {@link #minFraction} and {@link #minSignificantDigits}, will trigger complex algorithm similar to
     * <em>Dragon4</em> to determine the low-order digits and the number of digits to display based on
     * the value of the double. If the number of fraction places or significant digits can be bounded,
     * consider using {@link #maxFraction} or {@link #maxSignificantDigits} instead to maximize performance.
     * For more information, read the following blog post.
     *
     * <p>
     * http://www.serpentine.com/blog/2011/06/29/here-be-dragons-advances-in-problems-you-didnt-even-know-you-had/
     *
     * @return A Precision for chaining or passing to the NumberFormatter precision() setter.
     * @stable ICU 60
     * @see NumberFormatter
     */
    public static Precision unlimited() {
        return constructInfinite();
    }

    /**
     * Show numbers rounded if necessary to the nearest integer.
     *
     * @return A FractionPrecision for chaining or passing to the NumberFormatter precision() setter.
     * @stable ICU 60
     * @see NumberFormatter
     */
    public static FractionPrecision integer() {
        return constructFraction(0, 0);
    }

    /**
     * Show numbers rounded if necessary to a certain number of fraction places (numerals after the
     * decimal separator). Additionally, pad with zeros to ensure that this number of places are always
     * shown.
     *
     * <p>
     * Example output with minMaxFractionPlaces = 3:
     *
     * <p>
     * 87,650.000<br>
     * 8,765.000<br>
     * 876.500<br>
     * 87.650<br>
     * 8.765<br>
     * 0.876<br>
     * 0.088<br>
     * 0.009<br>
     * 0.000 (zero)
     *
     * <p>
     * This method is equivalent to {@link #minMaxFraction} with both arguments equal.
     *
     * @param minMaxFractionPlaces
     *            The minimum and maximum number of numerals to display after the decimal separator
     *            (rounding if too long or padding with zeros if too short).
     * @return A FractionPrecision for chaining or passing to the NumberFormatter precision() setter.
     * @throws IllegalArgumentException if the input number is too big or smaller than 0.
     * @stable ICU 60
     * @see NumberFormatter
     */
    public static FractionPrecision fixedFraction(int minMaxFractionPlaces) {
        if (minMaxFractionPlaces >= 0 && minMaxFractionPlaces <= RoundingUtils.MAX_INT_FRAC_SIG) {
            return constructFraction(minMaxFractionPlaces, minMaxFractionPlaces);
        } else {
            throw new IllegalArgumentException("Fraction length must be between 0 and "
                    + RoundingUtils.MAX_INT_FRAC_SIG
                    + " (inclusive)");
        }
    }

    /**
     * Always show at least a certain number of fraction places after the decimal separator, padding with
     * zeros if necessary. Do not perform rounding (display numbers to their full precision).
     *
     * <p>
     * <strong>NOTE:</strong> If you are formatting <em>doubles</em>, see the performance note in
     * {@link #unlimited}.
     *
     * @param minFractionPlaces
     *            The minimum number of numerals to display after the decimal separator (padding with
     *            zeros if necessary).
     * @return A FractionPrecision for chaining or passing to the NumberFormatter precision() setter.
     * @throws IllegalArgumentException if the input number is too big or smaller than 0.
     * @stable ICU 60
     * @see NumberFormatter
     */
    public static FractionPrecision minFraction(int minFractionPlaces) {
        if (minFractionPlaces >= 0 && minFractionPlaces <= RoundingUtils.MAX_INT_FRAC_SIG) {
            return constructFraction(minFractionPlaces, -1);
        } else {
            throw new IllegalArgumentException("Fraction length must be between 0 and "
                    + RoundingUtils.MAX_INT_FRAC_SIG
                    + " (inclusive)");
        }
    }

    /**
     * Show numbers rounded if necessary to a certain number of fraction places (numerals after the
     * decimal separator). Unlike the other fraction rounding strategies, this strategy does <em>not</em>
     * pad zeros to the end of the number.
     *
     * @param maxFractionPlaces
     *            The maximum number of numerals to display after the decimal mark (rounding if
     *            necessary).
     * @return A FractionPrecision for chaining or passing to the NumberFormatter precision() setter.
     * @throws IllegalArgumentException if the input number is too big or smaller than 0.
     * @stable ICU 60
     * @see NumberFormatter
     */
    public static FractionPrecision maxFraction(int maxFractionPlaces) {
        if (maxFractionPlaces >= 0 && maxFractionPlaces <= RoundingUtils.MAX_INT_FRAC_SIG) {
            return constructFraction(0, maxFractionPlaces);
        } else {
            throw new IllegalArgumentException("Fraction length must be between 0 and "
                    + RoundingUtils.MAX_INT_FRAC_SIG
                    + " (inclusive)");
        }
    }

    /**
     * Show numbers rounded if necessary to a certain number of fraction places (numerals after the
     * decimal separator); in addition, always show at least a certain number of places after the decimal
     * separator, padding with zeros if necessary.
     *
     * @param minFractionPlaces
     *            The minimum number of numerals to display after the decimal separator (padding with
     *            zeros if necessary).
     * @param maxFractionPlaces
     *            The maximum number of numerals to display after the decimal separator (rounding if
     *            necessary).
     * @return A FractionPrecision for chaining or passing to the NumberFormatter precision() setter.
     * @throws IllegalArgumentException if the input number is too big or smaller than 0.
     * @stable ICU 60
     * @see NumberFormatter
     */
    public static FractionPrecision minMaxFraction(int minFractionPlaces, int maxFractionPlaces) {
        if (minFractionPlaces >= 0
                && maxFractionPlaces <= RoundingUtils.MAX_INT_FRAC_SIG
                && minFractionPlaces <= maxFractionPlaces) {
            return constructFraction(minFractionPlaces, maxFractionPlaces);
        } else {
            throw new IllegalArgumentException("Fraction length must be between 0 and "
                    + RoundingUtils.MAX_INT_FRAC_SIG
                    + " (inclusive)");
        }
    }

    /**
     * Show numbers rounded if necessary to a certain number of significant digits or significant
     * figures. Additionally, pad with zeros to ensure that this number of significant digits/figures are
     * always shown.
     *
     * <p>
     * This method is equivalent to {@link #minMaxSignificantDigits} with both arguments equal.
     *
     * @param minMaxSignificantDigits
     *            The minimum and maximum number of significant digits to display (rounding if too long
     *            or padding with zeros if too short).
     * @return A Precision for chaining or passing to the NumberFormatter precision() setter.
     * @throws IllegalArgumentException if the input number is too big or smaller than 1.
     * @stable ICU 62
     * @see NumberFormatter
     */
    public static Precision fixedSignificantDigits(int minMaxSignificantDigits) {
        if (minMaxSignificantDigits >= 1 && minMaxSignificantDigits <= RoundingUtils.MAX_INT_FRAC_SIG) {
            return constructSignificant(minMaxSignificantDigits, minMaxSignificantDigits);
        } else {
            throw new IllegalArgumentException("Significant digits must be between 1 and "
                    + RoundingUtils.MAX_INT_FRAC_SIG
                    + " (inclusive)");
        }
    }

    /**
     * Always show at least a certain number of significant digits/figures, padding with zeros if
     * necessary. Do not perform rounding (display numbers to their full precision).
     *
     * <p>
     * <strong>NOTE:</strong> If you are formatting <em>doubles</em>, see the performance note in
     * {@link #unlimited}.
     *
     * @param minSignificantDigits
     *            The minimum number of significant digits to display (padding with zeros if too short).
     * @return A Precision for chaining or passing to the NumberFormatter precision() setter.
     * @throws IllegalArgumentException if the input number is too big or smaller than 1.
     * @stable ICU 62
     * @see NumberFormatter
     */
    public static Precision minSignificantDigits(int minSignificantDigits) {
        if (minSignificantDigits >= 1 && minSignificantDigits <= RoundingUtils.MAX_INT_FRAC_SIG) {
            return constructSignificant(minSignificantDigits, -1);
        } else {
            throw new IllegalArgumentException("Significant digits must be between 1 and "
                    + RoundingUtils.MAX_INT_FRAC_SIG
                    + " (inclusive)");
        }
    }

    /**
     * Show numbers rounded if necessary to a certain number of significant digits/figures.
     *
     * @param maxSignificantDigits
     *            The maximum number of significant digits to display (rounding if too long).
     * @return A Precision for chaining or passing to the NumberFormatter precision() setter.
     * @throws IllegalArgumentException if the input number is too big or smaller than 1.
     * @stable ICU 62
     * @see NumberFormatter
     */
    public static Precision maxSignificantDigits(int maxSignificantDigits) {
        if (maxSignificantDigits >= 1 && maxSignificantDigits <= RoundingUtils.MAX_INT_FRAC_SIG) {
            return constructSignificant(1, maxSignificantDigits);
        } else {
            throw new IllegalArgumentException("Significant digits must be between 1 and "
                    + RoundingUtils.MAX_INT_FRAC_SIG
                    + " (inclusive)");
        }
    }

    /**
     * Show numbers rounded if necessary to a certain number of significant digits/figures; in addition,
     * always show at least a certain number of significant digits, padding with zeros if necessary.
     *
     * @param minSignificantDigits
     *            The minimum number of significant digits to display (padding with zeros if necessary).
     * @param maxSignificantDigits
     *            The maximum number of significant digits to display (rounding if necessary).
     * @return A Precision for chaining or passing to the NumberFormatter precision() setter.
     * @throws IllegalArgumentException if the input number is too big or smaller than 1.
     * @stable ICU 62
     * @see NumberFormatter
     */
    public static Precision minMaxSignificantDigits(int minSignificantDigits, int maxSignificantDigits) {
        if (minSignificantDigits >= 1
                && maxSignificantDigits <= RoundingUtils.MAX_INT_FRAC_SIG
                && minSignificantDigits <= maxSignificantDigits) {
            return constructSignificant(minSignificantDigits, maxSignificantDigits);
        } else {
            throw new IllegalArgumentException("Significant digits must be between 1 and "
                    + RoundingUtils.MAX_INT_FRAC_SIG
                    + " (inclusive)");
        }
    }

    /**
     * Show numbers rounded if necessary to the closest multiple of a certain rounding increment. For
     * example, if the rounding increment is 0.5, then round 1.2 to 1 and round 1.3 to 1.5.
     *
     * <p>
     * In order to ensure that numbers are padded to the appropriate number of fraction places, set the
     * scale on the rounding increment BigDecimal. For example, to round to the nearest 0.5 and always
     * display 2 numerals after the decimal separator (to display 1.2 as "1.00" and 1.3 as "1.50"), you
     * can run:
     *
     * <pre>
     * Precision.increment(new BigDecimal("0.50"))
     * </pre>
     *
     * <p>
     * For more information on the scale of Java BigDecimal, see {@link java.math.BigDecimal#scale()}.
     *
     * @param roundingIncrement
     *            The increment to which to round numbers.
     * @return A Precision for chaining or passing to the NumberFormatter precision() setter.
     * @throws IllegalArgumentException if the rounding increment is null or non-positive.
     * @stable ICU 60
     * @see NumberFormatter
     */
    public static Precision increment(BigDecimal roundingIncrement) {
        if (roundingIncrement != null && roundingIncrement.compareTo(BigDecimal.ZERO) > 0) {
            return constructIncrement(roundingIncrement);
        } else {
            throw new IllegalArgumentException("Rounding increment must be positive and non-null");
        }
    }

    /**
     * Show numbers rounded and padded according to the rules for the currency unit. The most common
     * rounding precision settings for currencies include <code>Precision.fixedFraction(2)</code>,
     * <code>Precision.integer()</code>, and <code>Precision.increment(0.05)</code> for cash transactions
     * ("nickel rounding").
     *
     * <p>
     * The exact rounding details will be resolved at runtime based on the currency unit specified in the
     * NumberFormatter chain. To round according to the rules for one currency while displaying the
     * symbol for another currency, the withCurrency() method can be called on the return value of this
     * method.
     *
     * @param currencyUsage
     *            Either STANDARD (for digital transactions) or CASH (for transactions where the rounding
     *            increment may be limited by the available denominations of cash or coins).
     * @return A CurrencyPrecision for chaining or passing to the NumberFormatter precision() setter.
     * @throws IllegalArgumentException if currencyUsage is null.
     * @stable ICU 60
     * @see NumberFormatter
     */
    public static CurrencyPrecision currency(CurrencyUsage currencyUsage) {
        if (currencyUsage != null) {
            return constructCurrency(currencyUsage);
        } else {
            throw new IllegalArgumentException("CurrencyUsage must be non-null");
        }
    }

    /**
     * Configure how trailing zeros are displayed on numbers. For example, to hide trailing zeros
     * when the number is an integer, use HIDE_IF_WHOLE.
     *
     * @param trailingZeroDisplay Option to configure the display of trailing zeros.
     * @draft ICU 69
     */
    public Precision trailingZeroDisplay(TrailingZeroDisplay trailingZeroDisplay) {
        Precision result = this.createCopy();
        result.trailingZeroDisplay = trailingZeroDisplay;
        return result;
    }

    /**
     * Sets a MathContext to use on this Precision.
     *
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public Precision withMode(MathContext mathContext) {
        if (this.mathContext.equals(mathContext)) {
            return this;
        }
        Precision other = createCopy();
        other.mathContext = mathContext;
        return other;
    }

    /** Package-private clone method */
    abstract Precision createCopy();

    /**
     * Call this function to copy the fields from the Precision base class.
     *
     * Note: It would be nice if this returned the copy, but most impls return the child class, not Precision.
     */
    /* package-private */ void createCopyHelper(Precision copy) {
        copy.mathContext = mathContext;
        copy.trailingZeroDisplay = trailingZeroDisplay;
    }

    /**
     * @internal
     * @deprecated ICU 60 This API is ICU internal only.
     */
    @Deprecated
    public abstract void apply(DecimalQuantity value);

    //////////////////////////
    // PACKAGE-PRIVATE APIS //
    //////////////////////////

    /**
     * @internal
     * @deprecated ICU internal only.
     */
    @Deprecated
    public static final BogusRounder BOGUS_PRECISION = new BogusRounder();

    static final InfiniteRounderImpl NONE = new InfiniteRounderImpl();

    static final FractionRounderImpl FIXED_FRAC_0 = new FractionRounderImpl(0, 0);
    static final FractionRounderImpl FIXED_FRAC_2 = new FractionRounderImpl(2, 2);
    static final FractionRounderImpl DEFAULT_MAX_FRAC_6 = new FractionRounderImpl(0, 6);

    static final SignificantRounderImpl FIXED_SIG_2 = new SignificantRounderImpl(2, 2);
    static final SignificantRounderImpl FIXED_SIG_3 = new SignificantRounderImpl(3, 3);
    static final SignificantRounderImpl RANGE_SIG_2_3 = new SignificantRounderImpl(2, 3);

    static final FracSigRounderImpl COMPACT_STRATEGY = new FracSigRounderImpl(0, 0, 1, 2, RoundingPriority.RELAXED,
            false);

    static final IncrementFiveRounderImpl NICKEL = new IncrementFiveRounderImpl(new BigDecimal("0.05"), 2, 2);

    static final CurrencyRounderImpl MONETARY_STANDARD = new CurrencyRounderImpl(CurrencyUsage.STANDARD);
    static final CurrencyRounderImpl MONETARY_CASH = new CurrencyRounderImpl(CurrencyUsage.CASH);

    static Precision constructInfinite() {
        return NONE;
    }

    static FractionPrecision constructFraction(int minFrac, int maxFrac) {
        if (minFrac == 0 && maxFrac == 0) {
            return FIXED_FRAC_0;
        } else if (minFrac == 2 && maxFrac == 2) {
            return FIXED_FRAC_2;
        } else if (minFrac == 0 && maxFrac == 6) {
            return DEFAULT_MAX_FRAC_6;
        } else {
            return new FractionRounderImpl(minFrac, maxFrac);
        }
    }

    /** Assumes that minSig <= maxSig. */
    static Precision constructSignificant(int minSig, int maxSig) {
        if (minSig == 2 && maxSig == 2) {
            return FIXED_SIG_2;
        } else if (minSig == 3 && maxSig == 3) {
            return FIXED_SIG_3;
        } else if (minSig == 2 && maxSig == 3) {
            return RANGE_SIG_2_3;
        } else {
            return new SignificantRounderImpl(minSig, maxSig);
        }
    }

    static Precision constructFractionSignificant(FractionPrecision base_, int minSig, int maxSig,
            RoundingPriority priority, boolean retain) {
        assert base_ instanceof FractionRounderImpl;
        FractionRounderImpl base = (FractionRounderImpl) base_;
        Precision returnValue;
        if (base.minFrac == 0 && base.maxFrac == 0 && minSig == 1 && maxSig == 2 && priority == RoundingPriority.RELAXED
                && !retain) {
            returnValue = COMPACT_STRATEGY;
        } else {
            returnValue = new FracSigRounderImpl(base.minFrac, base.maxFrac, minSig, maxSig, priority, retain);
        }
        return returnValue.withMode(base.mathContext);
    }

    static Precision constructIncrement(BigDecimal increment) {
        // NOTE: .equals() is what we want, not .compareTo()
        if (increment.equals(NICKEL.increment)) {
            return NICKEL;
        }
        // Note: For number formatting, the BigDecimal increment is used for IncrementRounderImpl
        // but not mIncrementOneRounderImpl or IncrementFiveRounderImpl. However, fIncrement is
        // used in all three when constructing a skeleton.
        BigDecimal reduced = increment.stripTrailingZeros();
        if (reduced.precision() == 1) {
            int minFrac = increment.scale();
            int maxFrac = reduced.scale();
            BigInteger digit = reduced.unscaledValue();
            if (digit.intValue() == 1) {
                return new IncrementOneRounderImpl(increment, minFrac, maxFrac);
            } else if (digit.intValue() == 5) {
                return new IncrementFiveRounderImpl(increment, minFrac, maxFrac);
            }
        }
        return new IncrementRounderImpl(increment);
    }

    static CurrencyPrecision constructCurrency(CurrencyUsage usage) {
        if (usage == CurrencyUsage.STANDARD) {
            return MONETARY_STANDARD;
        } else if (usage == CurrencyUsage.CASH) {
            return MONETARY_CASH;
        } else {
            throw new AssertionError();
        }
    }

    static Precision constructFromCurrency(CurrencyPrecision base_, Currency currency) {
        assert base_ instanceof CurrencyRounderImpl;
        CurrencyRounderImpl base = (CurrencyRounderImpl) base_;
        double incrementDouble = currency.getRoundingIncrement(base.usage);
        Precision returnValue;
        if (incrementDouble != 0.0) {
            BigDecimal increment = BigDecimal.valueOf(incrementDouble);
            returnValue = constructIncrement(increment);
        } else {
            int minMaxFrac = currency.getDefaultFractionDigits(base.usage);
            returnValue = constructFraction(minMaxFrac, minMaxFrac);
        }
        return returnValue.withMode(base.mathContext);
    }

    /**
     * Returns a valid working Rounder. If the Rounder is a CurrencyRounder, applies the given currency.
     * Otherwise, simply passes through the argument.
     *
     * @param currency
     *            A currency object to use in case the input object needs it.
     * @return A Rounder object ready for use.
     */
    Precision withLocaleData(Currency currency) {
        if (this instanceof CurrencyPrecision) {
            return ((CurrencyPrecision) this).withCurrency(currency);
        } else {
            return this;
        }
    }

    /**
     * Rounding endpoint used by Engineering and Compact notation. Chooses the most appropriate
     * multiplier (magnitude adjustment), applies the adjustment, rounds, and returns the chosen
     * multiplier.
     *
     * <p>
     * In most cases, this is simple. However, when rounding the number causes it to cross a multiplier
     * boundary, we need to re-do the rounding. For example, to display 999,999 in Engineering notation
     * with 2 sigfigs, first you guess the multiplier to be -3. However, then you end up getting 1000E3,
     * which is not the correct output. You then change your multiplier to be -6, and you get 1.0E6,
     * which is correct.
     *
     * @param input
     *            The quantity to process.
     * @param producer
     *            Function to call to return a multiplier based on a magnitude.
     * @return The number of orders of magnitude the input was adjusted by this method.
     */
    int chooseMultiplierAndApply(DecimalQuantity input, MultiplierProducer producer) {
        // Do not call this method with zero, NaN, or infinity.
        assert !input.isZeroish();

        // Perform the first attempt at rounding.
        int magnitude = input.getMagnitude();
        int multiplier = producer.getMultiplier(magnitude);
        input.adjustMagnitude(multiplier);
        apply(input);

        // If the number rounded to zero, exit.
        if (input.isZeroish()) {
            return multiplier;
        }

        // If the new magnitude after rounding is the same as it was before rounding, then we are done.
        // This case applies to most numbers.
        if (input.getMagnitude() == magnitude + multiplier) {
            return multiplier;
        }

        // If the above case DIDN'T apply, then we have a case like 99.9 -> 100 or 999.9 -> 1000:
        // The number rounded up to the next magnitude. Check if the multiplier changes; if it doesn't,
        // we do not need to make any more adjustments.
        int _multiplier = producer.getMultiplier(magnitude + 1);
        if (multiplier == _multiplier) {
            return multiplier;
        }

        // We have a case like 999.9 -> 1000, where the correct output is "1K", not "1000".
        // Fix the magnitude and re-apply the rounding strategy.
        input.adjustMagnitude(_multiplier - multiplier);
        apply(input);
        return _multiplier;
    }

    ///////////////
    // INTERNALS //
    ///////////////

    /**
     * An BogusRounder's MathContext into precision.
     *
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public static class BogusRounder extends Precision {
        /**
         * Default constructor.
         * @internal
         * @deprecated This API is ICU internal only.
         */
        @Deprecated
        public BogusRounder() {
        }

        /**
         * {@inheritDoc}
         * @internal
         * @deprecated This API is ICU internal only.
         */
        @Override
        @Deprecated
        public void apply(DecimalQuantity value) {
            throw new AssertionError("BogusRounder must not be applied");
        }

        @Override
        BogusRounder createCopy() {
            BogusRounder copy = new BogusRounder();
            createCopyHelper(copy);
            return copy;
        }

        /**
         * Copies the BogusRounder's MathContext into precision.
         *
         * @internal
         * @deprecated This API is ICU internal only.
         */
        @Deprecated
        public Precision into(Precision precision) {
            Precision copy = precision.createCopy();
            createCopyHelper(copy);
            return copy;
        }
    }

    static class InfiniteRounderImpl extends Precision {

        public InfiniteRounderImpl() {
        }

        @Override
        public void apply(DecimalQuantity value) {
            value.roundToInfinity();
            setResolvedMinFraction(value, 0);
        }

        @Override
        InfiniteRounderImpl createCopy() {
            InfiniteRounderImpl copy = new InfiniteRounderImpl();
            createCopyHelper(copy);
            return copy;
        }
    }

    static class FractionRounderImpl extends FractionPrecision {
        final int minFrac;
        final int maxFrac;

        public FractionRounderImpl(int minFrac, int maxFrac) {
            this.minFrac = minFrac;
            this.maxFrac = maxFrac;
        }

        @Override
        public void apply(DecimalQuantity value) {
            value.roundToMagnitude(getRoundingMagnitudeFraction(maxFrac), mathContext);
            setResolvedMinFraction(value, Math.max(0, -getDisplayMagnitudeFraction(minFrac)));
        }

        @Override
        FractionRounderImpl createCopy() {
            FractionRounderImpl copy = new FractionRounderImpl(minFrac, maxFrac);
            createCopyHelper(copy);
            return copy;
        }
    }

    static class SignificantRounderImpl extends Precision {
        final int minSig;
        final int maxSig;

        public SignificantRounderImpl(int minSig, int maxSig) {
            this.minSig = minSig;
            this.maxSig = maxSig;
        }

        @Override
        public void apply(DecimalQuantity value) {
            value.roundToMagnitude(getRoundingMagnitudeSignificant(value, maxSig), mathContext);
            setResolvedMinFraction(value, Math.max(0, -getDisplayMagnitudeSignificant(value, minSig)));
            // Make sure that digits are displayed on zero.
            if (value.isZeroish() && minSig > 0) {
                value.setMinInteger(1);
            }
        }

        /**
         * Version of {@link #apply} that obeys minInt constraints. Used for scientific notation
         * compatibility mode.
         */
        public void apply(DecimalQuantity quantity, int minInt) {
            assert quantity.isZeroish();
            setResolvedMinFraction(quantity, minSig - minInt);
        }

        @Override
        SignificantRounderImpl createCopy() {
            SignificantRounderImpl copy = new SignificantRounderImpl(minSig, maxSig);
            createCopyHelper(copy);
            return copy;
        }
    }

    static class FracSigRounderImpl extends Precision {
        final int minFrac;
        final int maxFrac;
        final int minSig;
        final int maxSig;
        final RoundingPriority priority;
        final boolean retain;

        public FracSigRounderImpl(int minFrac, int maxFrac, int minSig, int maxSig, RoundingPriority priority,
                boolean retain) {
            this.minFrac = minFrac;
            this.maxFrac = maxFrac;
            this.minSig = minSig;
            this.maxSig = maxSig;
            this.priority = priority;
            this.retain = retain;
        }

        @Override
        public void apply(DecimalQuantity value) {
            int roundingMag1 = getRoundingMagnitudeFraction(maxFrac);
            int roundingMag2 = getRoundingMagnitudeSignificant(value, maxSig);
            int roundingMag;
            if (priority == RoundingPriority.RELAXED) {
                roundingMag = Math.min(roundingMag1, roundingMag2);
            } else {
                roundingMag = Math.max(roundingMag1, roundingMag2);
            }
            if (!value.isZeroish()) {
                int upperMag = value.getMagnitude();
                value.roundToMagnitude(roundingMag, mathContext);
                if (!value.isZeroish() && value.getMagnitude() != upperMag && roundingMag1 == roundingMag2) {
                    // roundingMag2 needs to be the magnitude after rounding
                    roundingMag2 += 1;
                }
            }

            int displayMag1 = getDisplayMagnitudeFraction(minFrac);
            int displayMag2 = getDisplayMagnitudeSignificant(value, minSig);
            int displayMag;
            if (retain) {
                // withMinDigits + withMaxDigits
                displayMag = Math.min(displayMag1, displayMag2);
            } else if (priority == RoundingPriority.RELAXED) {
                if (roundingMag2 <= roundingMag1) {
                    displayMag = displayMag2;
                } else {
                    displayMag = displayMag1;
                }
            } else {
                assert(priority == RoundingPriority.STRICT);
                if (roundingMag2 <= roundingMag1) {
                    displayMag = displayMag1;
                } else {
                    displayMag = displayMag2;
                }
            }
            setResolvedMinFraction(value, Math.max(0, -displayMag));
        }

        @Override
        FracSigRounderImpl createCopy() {
            FracSigRounderImpl copy = new FracSigRounderImpl(minFrac, maxFrac, minSig, maxSig, priority, retain);
            createCopyHelper(copy);
            return copy;
        }
    }

    /**
     * Used for strange increments like 3.14.
     */
    static class IncrementRounderImpl extends Precision {
        final BigDecimal increment;

        public IncrementRounderImpl(BigDecimal increment) {
            this.increment = increment;
        }

        @Override
        public void apply(DecimalQuantity value) {
            value.roundToIncrement(increment, mathContext);
            setResolvedMinFraction(value, Math.max(0, increment.scale()));
        }

        @Override
        IncrementRounderImpl createCopy() {
            IncrementRounderImpl copy = new IncrementRounderImpl(increment);
            createCopyHelper(copy);
            return copy;
        }
    }

    /**
     * Used for increments with 1 as the only digit. This is different than fraction
     * rounding because it supports having additional trailing zeros. For example, this
     * class is used to round with the increment 0.010.
     */
    static class IncrementOneRounderImpl extends IncrementRounderImpl {
        final int minFrac;
        final int maxFrac;

        public IncrementOneRounderImpl(BigDecimal increment, int minFrac, int maxFrac) {
            super(increment);
            this.minFrac = minFrac;
            this.maxFrac = maxFrac;
        }

        @Override
        public void apply(DecimalQuantity value) {
            value.roundToMagnitude(-maxFrac, mathContext);
            setResolvedMinFraction(value, minFrac);
        }

        @Override
        IncrementOneRounderImpl createCopy() {
            IncrementOneRounderImpl copy = new IncrementOneRounderImpl(increment, minFrac, maxFrac);
            createCopyHelper(copy);
            return copy;
        }
    }

    /**
     * Used for increments with 5 as the only digit (nickel rounding).
     */
    static class IncrementFiveRounderImpl extends IncrementRounderImpl {
        final int minFrac;
        final int maxFrac;

        public IncrementFiveRounderImpl(BigDecimal increment, int minFrac, int maxFrac) {
            super(increment);
            this.minFrac = minFrac;
            this.maxFrac = maxFrac;
        }

        @Override
        public void apply(DecimalQuantity value) {
            value.roundToNickel(-maxFrac, mathContext);
            setResolvedMinFraction(value, minFrac);
        }

        @Override
        IncrementFiveRounderImpl createCopy() {
            IncrementFiveRounderImpl copy = new IncrementFiveRounderImpl(increment, minFrac, maxFrac);
            createCopyHelper(copy);
            return copy;
        }
    }

    static class CurrencyRounderImpl extends CurrencyPrecision {
        final CurrencyUsage usage;

        public CurrencyRounderImpl(CurrencyUsage usage) {
            this.usage = usage;
        }

        @Override
        public void apply(DecimalQuantity value) {
            // Call .withCurrency() before .apply()!
            throw new AssertionError();
        }

        @Override
        CurrencyRounderImpl createCopy() {
            CurrencyRounderImpl copy = new CurrencyRounderImpl(usage);
            createCopyHelper(copy);
            return copy;
        }
    }

    private static int getRoundingMagnitudeFraction(int maxFrac) {
        if (maxFrac == -1) {
            return Integer.MIN_VALUE;
        }
        return -maxFrac;
    }

    private static int getRoundingMagnitudeSignificant(DecimalQuantity value, int maxSig) {
        if (maxSig == -1) {
            return Integer.MIN_VALUE;
        }
        int magnitude = value.isZeroish() ? 0 : value.getMagnitude();
        return magnitude - maxSig + 1;
    }

    private static int getDisplayMagnitudeFraction(int minFrac) {
        if (minFrac == 0) {
            return Integer.MAX_VALUE;
        }
        return -minFrac;
    }

    void setResolvedMinFraction(DecimalQuantity value, int resolvedMinFraction) {
        if (trailingZeroDisplay == null ||
                trailingZeroDisplay == TrailingZeroDisplay.AUTO ||
                // PLURAL_OPERAND_T returns fraction digits as an integer
                value.getPluralOperand(Operand.t) != 0) {
            value.setMinFraction(resolvedMinFraction);
        }
    }

    private static int getDisplayMagnitudeSignificant(DecimalQuantity value, int minSig) {
        // Question: Is it useful to look at trailingZeroDisplay here?
        int magnitude = value.isZeroish() ? 0 : value.getMagnitude();
        return magnitude - minSig + 1;
    }
}
