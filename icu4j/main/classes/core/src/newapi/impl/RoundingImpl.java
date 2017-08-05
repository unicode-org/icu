// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package newapi.impl;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import com.ibm.icu.impl.number.FormatQuantity;
import com.ibm.icu.impl.number.FormatQuantity4;
import com.ibm.icu.impl.number.LdmlPatternInfo.PatternParseResult;
import com.ibm.icu.impl.number.RoundingUtils;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.Currency.CurrencyUsage;

import newapi.NumberFormatter.CurrencyRounding;
import newapi.NumberFormatter.FractionRounding;
import newapi.NumberFormatter.IRounding;
import newapi.NumberFormatter.Rounding;

/**
 * The internal version of {@link Rounding} with additional methods.
 *
 * <p>Although it seems as though RoundingImpl should extend Rounding, it actually extends
 * FractionRounding. This is because instances of FractionRounding are self-contained rounding
 * instances themselves, and they need to implement RoundingImpl. When ICU adopts Java 8, there will
 * be more options for the polymorphism, such as multiple inheritance with interfaces having default
 * methods and static factory methods on interfaces.
 */
@SuppressWarnings("deprecation")
public abstract class RoundingImpl extends FractionRounding.Internal implements Cloneable {

  public static RoundingImpl forPattern(PatternParseResult patternInfo) {
    if (patternInfo.positive.rounding != null) {
      return RoundingImplIncrement.getInstance(patternInfo.positive.rounding.toBigDecimal());
    } else if (patternInfo.positive.minimumSignificantDigits > 0) {
      return RoundingImplSignificant.getInstance(
          patternInfo.positive.minimumSignificantDigits,
          patternInfo.positive.maximumSignificantDigits);
    } else if (patternInfo.positive.exponentDigits > 0) {
      // FIXME
      throw new UnsupportedOperationException();
    } else {
      return RoundingImplFraction.getInstance(
          patternInfo.positive.minimumFractionDigits, patternInfo.positive.maximumFractionDigits);
    }
  }

  /**
   * Returns a RoundingImpl no matter what is the type of the provided argument. If the argument is
   * already a RoundingImpl, this method just returns the same object. Otherwise, it does some
   * processing to build a RoundingImpl.
   *
   * @param rounding The input object, which might or might not be a RoundingImpl.
   * @param currency A currency object to use in case the input object needs it.
   * @return A RoundingImpl object.
   */
  public static RoundingImpl normalizeType(IRounding rounding, Currency currency) {
    if (rounding instanceof RoundingImpl) {
      return (RoundingImpl) rounding;
    } else if (rounding instanceof RoundingImplCurrency) {
      return ((RoundingImplCurrency) rounding).withCurrency(currency);
    } else {
      return RoundingImplLambda.getInstance(rounding);
    }
  }

  private static final MathContext DEFAULT_MATH_CONTEXT =
      RoundingUtils.mathContextUnlimited(RoundingMode.HALF_EVEN);

  public MathContext mathContext;

  public RoundingImpl() {
    this.mathContext = DEFAULT_MATH_CONTEXT;
    // TODO: This is ugly, but necessary if a RoundingImpl is created
    // before this class has been initialized.
    if (this.mathContext == null) {
      this.mathContext = RoundingUtils.mathContextUnlimited(RoundingMode.HALF_EVEN);
    }
  }

  @Override
  public Rounding withMode(RoundingMode roundingMode) {
    return withMode(RoundingUtils.mathContextUnlimited(roundingMode));
  }

  @Override
  public Rounding withMode(MathContext mathContext) {
    if (this.mathContext.equals(mathContext)) {
      return this;
    }
    RoundingImpl other = (RoundingImpl) this.clone();
    other.mathContext = mathContext;
    return other;
  }

  abstract void apply(FormatQuantity value);

  @Override
  public BigDecimal round(BigDecimal input) {
    // Provided for API compatibility.
    FormatQuantity fq = new FormatQuantity4(input);
    this.apply(fq);
    return fq.toBigDecimal();
  }

  static interface MultiplierProducer {
    int getMultiplier(int magnitude);
  }

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

  @Override
  public Object clone() {
    try {
      return super.clone();
    } catch (CloneNotSupportedException e) {
      // Should not happen since parent is Object
      throw new AssertionError(e);
    }
  }

  /** A dummy class used when the number has already been rounded elsewhere. */
  public static class RoundingImplDummy extends RoundingImpl {
    public static final RoundingImplDummy INSTANCE = new RoundingImplDummy();

    private RoundingImplDummy() {}

    @Override
    void apply(FormatQuantity value) {}
  }

  public static class RoundingImplInfinity extends RoundingImpl {
    @Override
    void apply(FormatQuantity value) {
      value.roundToInfinity();
      value.setFractionLength(0, Integer.MAX_VALUE);
    }
  }

  public static class RoundingImplFraction extends RoundingImpl {
    int minFrac;
    int maxFrac;

    private static final RoundingImplFraction FIXED_0 = new RoundingImplFraction(0, 0);
    private static final RoundingImplFraction FIXED_2 = new RoundingImplFraction(2, 2);

    /** Assumes that minFrac <= maxFrac. */
    public static RoundingImplFraction getInstance(int minFrac, int maxFrac) {
      assert minFrac >= 0 && minFrac <= maxFrac;
      if (minFrac == 0 && maxFrac == 0) {
        return FIXED_0;
      } else if (minFrac == 2 && maxFrac == 2) {
        return FIXED_2;
      } else {
        return new RoundingImplFraction(minFrac, maxFrac);
      }
    }

    /** Hook for public static final; uses integer rounding */
    public RoundingImplFraction() {
      this(0, 0);
    }

    private RoundingImplFraction(int minFrac, int maxFrac) {
      this.minFrac = minFrac;
      this.maxFrac = maxFrac;
    }

    @Override
    void apply(FormatQuantity value) {
      value.roundToMagnitude(getRoundingMagnitude(maxFrac), mathContext);
      value.setFractionLength(Math.max(0, -getDisplayMagnitude(minFrac)), Integer.MAX_VALUE);
    }

    static int getRoundingMagnitude(int maxFrac) {
      if (maxFrac == Integer.MAX_VALUE) {
        return Integer.MIN_VALUE;
      }
      return -maxFrac;
    }

    static int getDisplayMagnitude(int minFrac) {
      if (minFrac == 0) {
        return Integer.MAX_VALUE;
      }
      return -minFrac;
    }

    @Override
    public Rounding withMinFigures(int minFigures) {
      if (minFigures > 0 && minFigures <= MAX_VALUE) {
        return RoundingImplFractionSignificant.getInstance(this, minFigures, -1);
      } else {
        throw new IllegalArgumentException("Significant digits must be between 0 and " + MAX_VALUE);
      }
    }

    @Override
    public Rounding withMaxFigures(int maxFigures) {
      if (maxFigures > 0 && maxFigures <= MAX_VALUE) {
        return RoundingImplFractionSignificant.getInstance(this, -1, maxFigures);
      } else {
        throw new IllegalArgumentException("Significant digits must be between 0 and " + MAX_VALUE);
      }
    }
  }

  public static class RoundingImplSignificant extends RoundingImpl {
    int minSig;
    int maxSig;

    private static final RoundingImplSignificant FIXED_2 = new RoundingImplSignificant(2, 2);
    private static final RoundingImplSignificant FIXED_3 = new RoundingImplSignificant(3, 3);
    private static final RoundingImplSignificant RANGE_2_3 = new RoundingImplSignificant(2, 3);

    /** Assumes that minSig <= maxSig. */
    public static RoundingImplSignificant getInstance(int minSig, int maxSig) {
      assert minSig >= 0 && minSig <= maxSig;
      if (minSig == 2 && maxSig == 2) {
        return FIXED_2;
      } else if (minSig == 3 && maxSig == 3) {
        return FIXED_3;
      } else if (minSig == 2 && maxSig == 3) {
        return RANGE_2_3;
      } else {
        return new RoundingImplSignificant(minSig, maxSig);
      }
    }

    RoundingImplSignificant(int minSig, int maxSig) {
      this.minSig = minSig;
      this.maxSig = maxSig;
    }

    @Override
    void apply(FormatQuantity value) {
      value.roundToMagnitude(getRoundingMagnitude(value, maxSig), mathContext);
      value.setFractionLength(Math.max(0, -getDisplayMagnitude(value, minSig)), Integer.MAX_VALUE);
    }

    /** Version of {@link #apply} that obeys minInt constraints. */
    public void apply(FormatQuantity quantity, int minInt) {
      assert quantity.isZero();
      quantity.setFractionLength(minSig - minInt, Integer.MAX_VALUE);
    }

    static int getRoundingMagnitude(FormatQuantity value, int maxSig) {
      if (maxSig == Integer.MAX_VALUE) {
        return Integer.MIN_VALUE;
      }
      int magnitude = value.isZero() ? 0 : value.getMagnitude();
      return magnitude - maxSig + 1;
    }

    static int getDisplayMagnitude(FormatQuantity value, int minSig) {
      int magnitude = value.isZero() ? 0 : value.getMagnitude();
      return magnitude - minSig + 1;
    }
  }

  public static class RoundingImplFractionSignificant extends RoundingImpl {
    int minFrac;
    int maxFrac;
    int minSig;
    int maxSig;

    // Package-private
    static final RoundingImplFractionSignificant COMPACT_STRATEGY =
        new RoundingImplFractionSignificant(0, 0, 2, -1);

    public static Rounding getInstance(FractionRounding _base, int minSig, int maxSig) {
      assert _base instanceof RoundingImplFraction;
      RoundingImplFraction base = (RoundingImplFraction) _base;
      if (base.minFrac == 0 && base.maxFrac == 0 && minSig == 2 /* && maxSig == -1 */) {
        return COMPACT_STRATEGY;
      } else {
        return new RoundingImplFractionSignificant(base.minFrac, base.maxFrac, minSig, maxSig);
      }
    }

    /** Assumes that minFrac <= maxFrac and minSig <= maxSig except for -1. */
    private RoundingImplFractionSignificant(int minFrac, int maxFrac, int minSig, int maxSig) {
      // Exactly one of the arguments should be -1, either minSig or maxSig.
      assert (minFrac != -1 && maxFrac != -1 && minSig == -1 && maxSig != -1 && minFrac <= maxFrac)
          || (minFrac != -1 && maxFrac != -1 && minSig != -1 && maxSig == -1 && minFrac <= maxFrac);
      this.minFrac = minFrac;
      this.maxFrac = maxFrac;
      this.minSig = minSig;
      this.maxSig = maxSig;
    }

    @Override
    void apply(FormatQuantity value) {
      int displayMag = RoundingImplFraction.getDisplayMagnitude(minFrac);
      int roundingMag = RoundingImplFraction.getRoundingMagnitude(maxFrac);
      if (minSig == -1) {
        // Max Sig override
        int candidate = RoundingImplSignificant.getRoundingMagnitude(value, maxSig);
        roundingMag = Math.max(roundingMag, candidate);
      } else {
        // Min Sig override
        int candidate = RoundingImplSignificant.getDisplayMagnitude(value, minSig);
        roundingMag = Math.min(roundingMag, candidate);
      }
      value.roundToMagnitude(roundingMag, mathContext);
      value.setFractionLength(Math.max(0, -displayMag), Integer.MAX_VALUE);
    }
  }

  public static class RoundingImplIncrement extends RoundingImpl {
    BigDecimal increment;

    private static final RoundingImplIncrement NICKEL =
        new RoundingImplIncrement(BigDecimal.valueOf(0.5));

    public static RoundingImplIncrement getInstance(BigDecimal increment) {
      assert increment != null;
      if (increment.compareTo(NICKEL.increment) == 0) {
        return NICKEL;
      } else {
        return new RoundingImplIncrement(increment);
      }
    }

    private RoundingImplIncrement(BigDecimal increment) {
      this.increment = increment;
    }

    @Override
    void apply(FormatQuantity value) {
      value.roundToIncrement(increment, mathContext);
      value.setFractionLength(increment.scale(), increment.scale());
    }
  }

  public static class RoundingImplLambda extends RoundingImpl {
    IRounding lambda;

    public static RoundingImplLambda getInstance(IRounding lambda) {
      assert !(lambda instanceof Rounding);
      return new RoundingImplLambda(lambda);
    }

    private RoundingImplLambda(IRounding lambda) {
      this.lambda = lambda;
    }

    @Override
    void apply(FormatQuantity value) {
      // TODO: Cache the BigDecimal between calls?
      BigDecimal temp = value.toBigDecimal();
      temp = lambda.round(temp);
      value.setToBigDecimal(temp);
      value.setFractionLength(temp.scale(), Integer.MAX_VALUE);
    }
  }

  /**
   * NOTE: This is unlike the other classes here. It is NOT a standalone rounder and it does NOT
   * extend RoundingImpl.
   */
  public static class RoundingImplCurrency extends CurrencyRounding.Internal {
    final CurrencyUsage usage;
    final MathContext mc;

    private static final RoundingImplCurrency MONETARY_STANDARD =
        new RoundingImplCurrency(CurrencyUsage.STANDARD, DEFAULT_MATH_CONTEXT);

    private static final RoundingImplCurrency MONETARY_CASH =
        new RoundingImplCurrency(CurrencyUsage.CASH, DEFAULT_MATH_CONTEXT);

    public static RoundingImplCurrency getInstance(CurrencyUsage usage) {
      if (usage == CurrencyUsage.STANDARD) {
        return MONETARY_STANDARD;
      } else if (usage == CurrencyUsage.CASH) {
        return MONETARY_CASH;
      } else {
        throw new AssertionError();
      }
    }

    private RoundingImplCurrency(CurrencyUsage usage, MathContext mc) {
      this.usage = usage;
      this.mc = mc;
    }

    @Override
    public RoundingImpl withCurrency(Currency currency) {
      assert currency != null;
      double incrementDouble = currency.getRoundingIncrement(usage);
      if (incrementDouble != 0.0) {
        BigDecimal increment = BigDecimal.valueOf(incrementDouble);
        return RoundingImplIncrement.getInstance(increment);
      } else {
        int minMaxFrac = currency.getDefaultFractionDigits(usage);
        return RoundingImplFraction.getInstance(minMaxFrac, minMaxFrac);
      }
    }

    @Override
    public RoundingImplCurrency withMode(RoundingMode roundingMode) {
      // This is similar to RoundingImpl#withMode().
      return withMode(RoundingUtils.mathContextUnlimited(roundingMode));
    }

    @Override
    public RoundingImplCurrency withMode(MathContext mathContext) {
      // This is similar to RoundingImpl#withMode().
      if (mc.equals(mathContext)) {
        return this;
      }
      return new RoundingImplCurrency(usage, mathContext);
    }

    @Override
    public BigDecimal round(BigDecimal input) {
      throw new UnsupportedOperationException(
          "A currency must be specified before calling this method.");
    }
  }
}
