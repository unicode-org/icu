// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package newapi;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import com.ibm.icu.impl.number.FormatQuantity;
import com.ibm.icu.impl.number.RoundingUtils;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.Currency.CurrencyUsage;

import newapi.impl.MultiplierProducer;

public abstract class Rounder implements Cloneable {

    // FIXME
    /** @internal */
    public static final int MAX_VALUE = 100;

    /* package-private final */ MathContext mathContext;

    /* package-private */ Rounder() {
        mathContext = RoundingUtils.mathContextUnlimited(RoundingMode.HALF_EVEN);
    }

    public static Rounder none() {
        return constructInfinite();
    }

    public static FractionRounder integer() {
        return constructFraction(0, 0);
    }

    public static FractionRounder fixedFraction(int minMaxFractionDigits) {
        if (minMaxFractionDigits >= 0 && minMaxFractionDigits <= MAX_VALUE) {
            return constructFraction(minMaxFractionDigits, minMaxFractionDigits);
        } else {
            throw new IllegalArgumentException("Fraction length must be between 0 and " + MAX_VALUE);
        }
    }

    public static FractionRounder minFraction(int minFractionDigits) {
        if (minFractionDigits >= 0 && minFractionDigits < MAX_VALUE) {
            return constructFraction(minFractionDigits, -1);
        } else {
            throw new IllegalArgumentException("Fraction length must be between 0 and " + MAX_VALUE);
        }
    }

    public static FractionRounder maxFraction(int maxFractionDigits) {
        if (maxFractionDigits >= 0 && maxFractionDigits < MAX_VALUE) {
            return constructFraction(0, maxFractionDigits);
        } else {
            throw new IllegalArgumentException("Fraction length must be between 0 and " + MAX_VALUE);
        }
    }

    public static FractionRounder minMaxFraction(int minFractionDigits, int maxFractionDigits) {
        if (minFractionDigits >= 0 && maxFractionDigits <= MAX_VALUE && minFractionDigits <= maxFractionDigits) {
            return constructFraction(minFractionDigits, maxFractionDigits);
        } else {
            throw new IllegalArgumentException("Fraction length must be between 0 and " + MAX_VALUE);
        }
    }

    public static Rounder fixedDigits(int minMaxSignificantDigits) {
        if (minMaxSignificantDigits > 0 && minMaxSignificantDigits <= MAX_VALUE) {
            return constructSignificant(minMaxSignificantDigits, minMaxSignificantDigits);
        } else {
            throw new IllegalArgumentException("Significant digits must be between 0 and " + MAX_VALUE);
        }
    }

    public static Rounder minDigits(int minSignificantDigits) {
        if (minSignificantDigits > 0 && minSignificantDigits <= MAX_VALUE) {
            return constructSignificant(minSignificantDigits, -1);
        } else {
            throw new IllegalArgumentException("Significant digits must be between 0 and " + MAX_VALUE);
        }
    }

    public static Rounder maxDigits(int maxSignificantDigits) {
        if (maxSignificantDigits > 0 && maxSignificantDigits <= MAX_VALUE) {
            return constructSignificant(0, maxSignificantDigits);
        } else {
            throw new IllegalArgumentException("Significant digits must be between 0 and " + MAX_VALUE);
        }
    }

    public static Rounder minMaxDigits(int minSignificantDigits, int maxSignificantDigits) {
        if (minSignificantDigits > 0 && maxSignificantDigits <= MAX_VALUE && minSignificantDigits <= maxSignificantDigits) {
            return constructSignificant(minSignificantDigits, maxSignificantDigits);
        } else {
            throw new IllegalArgumentException("Significant digits must be between 0 and " + MAX_VALUE);
        }
    }

    public static Rounder increment(BigDecimal roundingIncrement) {
        if (roundingIncrement != null && roundingIncrement.compareTo(BigDecimal.ZERO) > 0) {
            return constructIncrement(roundingIncrement);
        } else {
            throw new IllegalArgumentException("Rounding increment must be positive and non-null");
        }
    }

    public static CurrencyRounder currency(CurrencyUsage currencyUsage) {
        if (currencyUsage != null) {
            return constructCurrency(currencyUsage);
        } else {
            throw new IllegalArgumentException("CurrencyUsage must be non-null");
        }
    }

    /**
     * Sets the {@link java.math.RoundingMode} to use when picking the direction to round (up or down).
     *
     * <p>
     * Common values include {@link RoundingMode#HALF_EVEN}, {@link RoundingMode#HALF_UP}, and
     * {@link RoundingMode#CEILING}. The default is HALF_EVEN.
     *
     * @param roundingMode
     *            The RoundingMode to use.
     * @return An immutable object for chaining.
     */
    public Rounder withMode(RoundingMode roundingMode) {
        return withMode(RoundingUtils.mathContextUnlimited(roundingMode));
    }

    /**
     * Sets a MathContext directly instead of RoundingMode.
     *
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public Rounder withMode(MathContext mathContext) {
        if (this.mathContext.equals(mathContext)) {
            return this;
        }
        Rounder other = (Rounder) this.clone();
        other.mathContext = mathContext;
        return other;
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            // Should not happen since parent is Object
            throw new AssertionError(e);
        }
    }

    //////////////////////////
    // PACKAGE-PRIVATE APIS //
    //////////////////////////

    private static final InfiniteRounderImpl NONE = new InfiniteRounderImpl();

    private static final FractionRounderImpl FIXED_FRAC_0 = new FractionRounderImpl(0, 0);
    private static final FractionRounderImpl FIXED_FRAC_2 = new FractionRounderImpl(2, 2);

    private static final SignificantRounderImpl FIXED_SIG_2 = new SignificantRounderImpl(2, 2);
    private static final SignificantRounderImpl FIXED_SIG_3 = new SignificantRounderImpl(3, 3);
    private static final SignificantRounderImpl RANGE_SIG_2_3 = new SignificantRounderImpl(2, 3);

    /* package-private */ static final FracSigRounderImpl COMPACT_STRATEGY = new FracSigRounderImpl(0, 0, 2, -1);

    private static final IncrementRounderImpl NICKEL = new IncrementRounderImpl(BigDecimal.valueOf(0.5));

    private static final CurrencyRounderImpl MONETARY_STANDARD = new CurrencyRounderImpl(CurrencyUsage.STANDARD);
    private static final CurrencyRounderImpl MONETARY_CASH = new CurrencyRounderImpl(CurrencyUsage.CASH);

    private static final PassThroughRounderImpl PASS_THROUGH = new PassThroughRounderImpl();

    static Rounder constructInfinite() {
        return NONE;
    }

    static FractionRounder constructFraction(int minFrac, int maxFrac) {
        if (minFrac == 0 && maxFrac == 0) {
            return FIXED_FRAC_0;
        } else if (minFrac == 2 && maxFrac == 2) {
            return FIXED_FRAC_2;
        } else {
            return new FractionRounderImpl(minFrac, maxFrac);
        }
    }

    /** Assumes that minSig <= maxSig. */
    static Rounder constructSignificant(int minSig, int maxSig) {
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

    static Rounder constructFractionSignificant(FractionRounder base_, int minSig, int maxSig) {
        assert base_ instanceof FractionRounderImpl;
        FractionRounderImpl base = (FractionRounderImpl) base_;
        if (base.minFrac == 0 && base.maxFrac == 0 && minSig == 2 /* && maxSig == -1 */) {
            return COMPACT_STRATEGY;
        } else {
            return new FracSigRounderImpl(base.minFrac, base.maxFrac, minSig, maxSig);
        }
    }

    static Rounder constructIncrement(BigDecimal increment) {
        if (increment.compareTo(NICKEL.increment) == 0) {
            return NICKEL;
        } else {
            return new IncrementRounderImpl(increment);
        }
    }

    static CurrencyRounder constructCurrency(CurrencyUsage usage) {
        if (usage == CurrencyUsage.STANDARD) {
            return MONETARY_STANDARD;
        } else if (usage == CurrencyUsage.CASH) {
            return MONETARY_CASH;
        } else {
            throw new AssertionError();
        }
    }

    static Rounder constructFromCurrency(CurrencyRounder base_, Currency currency) {
        assert base_ instanceof CurrencyRounderImpl;
        CurrencyRounderImpl base = (CurrencyRounderImpl) base_;
        double incrementDouble = currency.getRoundingIncrement(base.usage);
        if (incrementDouble != 0.0) {
            BigDecimal increment = BigDecimal.valueOf(incrementDouble);
            return constructIncrement(increment);
        } else {
            int minMaxFrac = currency.getDefaultFractionDigits(base.usage);
            return constructFraction(minMaxFrac, minMaxFrac);
        }
    }

    static Rounder constructPassThrough() {
        return PASS_THROUGH;
    }

    /**
     * Returns a valid working Rounder. If the Rounder is a CurrencyRounder, applies the given currency. Otherwise,
     * simply passes through the argument.
     *
     * @param rounder
     *            The input object.
     * @param currency
     *            A currency object to use in case the input object needs it.
     * @return A Rounder object ready for use.
     */
    static Rounder normalizeType(Rounder rounder, Currency currency) {
        if (rounder instanceof CurrencyRounder) {
            return ((CurrencyRounder) rounder).withCurrency(currency);
        } else {
            return rounder;
        }
    }

    abstract void apply(FormatQuantity value);

    int chooseMultiplierAndApply(FormatQuantity input, MultiplierProducer producer) {
        // TODO: Make a better and more efficient implementation.
        // TODO: Avoid the object creation here.
        FormatQuantity copy = input.createCopy();

        assert !input.isZero();
        int magnitude = input.getMagnitude();
        int multiplier = producer.getMultiplier(magnitude);
        input.adjustMagnitude(multiplier);
        apply(input);

        // If the number turned to zero when rounding, do not re-attempt the rounding.
        if (!input.isZero() && input.getMagnitude() == magnitude + multiplier + 1) {
            magnitude += 1;
            input.copyFrom(copy);
            multiplier = producer.getMultiplier(magnitude);
            input.adjustMagnitude(multiplier);
            assert input.getMagnitude() == magnitude + multiplier - 1;
            apply(input);
            assert input.getMagnitude() == magnitude + multiplier;
        }

        return multiplier;
    }

    ///////////////
    // INTERNALS //
    ///////////////

    static class InfiniteRounderImpl extends Rounder {

        private InfiniteRounderImpl() {
        }

        @Override
        void apply(FormatQuantity value) {
            value.roundToInfinity();
            value.setFractionLength(0, Integer.MAX_VALUE);
        }
    }

    static class FractionRounderImpl extends FractionRounder {
        final int minFrac;
        final int maxFrac;

        private FractionRounderImpl(int minFrac, int maxFrac) {
            this.minFrac = minFrac;
            this.maxFrac = maxFrac;
        }

        @Override
        void apply(FormatQuantity value) {
            value.roundToMagnitude(getRoundingMagnitudeFraction(maxFrac), mathContext);
            value.setFractionLength(Math.max(0, -getDisplayMagnitudeFraction(minFrac)), Integer.MAX_VALUE);
        }
    }

    static class SignificantRounderImpl extends Rounder {
        final int minSig;
        final int maxSig;

        private SignificantRounderImpl(int minSig, int maxSig) {
            this.minSig = minSig;
            this.maxSig = maxSig;
        }

        @Override
        void apply(FormatQuantity value) {
            value.roundToMagnitude(getRoundingMagnitudeSignificant(value, maxSig), mathContext);
            value.setFractionLength(Math.max(0, -getDisplayMagnitudeSignificant(value, minSig)), Integer.MAX_VALUE);
        }

        /** Version of {@link #apply} that obeys minInt constraints. Used for scientific notation compatibility mode. */
        public void apply(FormatQuantity quantity, int minInt) {
            assert quantity.isZero();
            quantity.setFractionLength(minSig - minInt, Integer.MAX_VALUE);
        }
    }

    static class FracSigRounderImpl extends Rounder {
        final int minFrac;
        final int maxFrac;
        final int minSig;
        final int maxSig;

        private FracSigRounderImpl(int minFrac, int maxFrac, int minSig, int maxSig) {
            this.minFrac = minFrac;
            this.maxFrac = maxFrac;
            this.minSig = minSig;
            this.maxSig = maxSig;
        }

        @Override
        void apply(FormatQuantity value) {
            int displayMag = getDisplayMagnitudeFraction(minFrac);
            int roundingMag = getRoundingMagnitudeFraction(maxFrac);
            if (minSig == -1) {
                // Max Sig override
                int candidate = getRoundingMagnitudeSignificant(value, maxSig);
                roundingMag = Math.max(roundingMag, candidate);
            } else {
                // Min Sig override
                int candidate = getDisplayMagnitudeSignificant(value, minSig);
                roundingMag = Math.min(roundingMag, candidate);
            }
            value.roundToMagnitude(roundingMag, mathContext);
            value.setFractionLength(Math.max(0, -displayMag), Integer.MAX_VALUE);
        }
    }

    static class IncrementRounderImpl extends Rounder {
        final BigDecimal increment;

        private IncrementRounderImpl(BigDecimal increment) {
            this.increment = increment;
        }

        @Override
        void apply(FormatQuantity value) {
            value.roundToIncrement(increment, mathContext);
            value.setFractionLength(increment.scale(), increment.scale());
        }
    }

    static class CurrencyRounderImpl extends CurrencyRounder {
        final CurrencyUsage usage;

        private CurrencyRounderImpl(CurrencyUsage usage) {
            this.usage = usage;
        }

        @Override
        void apply(FormatQuantity value) {
            // Call .withCurrency() before .apply()!
            throw new AssertionError();
        }
    }

    static class PassThroughRounderImpl extends Rounder {

        private PassThroughRounderImpl() {
        }

        @Override
        void apply(FormatQuantity value) {
            // TODO: Assert that value has already been rounded
        }
    }

    private static int getRoundingMagnitudeFraction(int maxFrac) {
        if (maxFrac == -1) {
            return Integer.MIN_VALUE;
        }
        return -maxFrac;
    }

    private static int getRoundingMagnitudeSignificant(FormatQuantity value, int maxSig) {
        if (maxSig == -1) {
            return Integer.MIN_VALUE;
        }
        int magnitude = value.isZero() ? 0 : value.getMagnitude();
        return magnitude - maxSig + 1;
    }

    private static int getDisplayMagnitudeFraction(int minFrac) {
        if (minFrac == 0) {
            return Integer.MAX_VALUE;
        }
        return -minFrac;
    }

    private static int getDisplayMagnitudeSignificant(FormatQuantity value, int minSig) {
        int magnitude = value.isZero() ? 0 : value.getMagnitude();
        return magnitude - minSig + 1;
    }
}
