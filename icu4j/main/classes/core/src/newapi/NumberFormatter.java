// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package newapi;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.AttributedCharacterIterator;
import java.text.FieldPosition;
import java.util.Arrays;
import java.util.Locale;

import com.ibm.icu.impl.number.FormatQuantityBCD;
import com.ibm.icu.impl.number.NumberStringBuilder;
import com.ibm.icu.impl.number.formatters.PaddingFormat.PadPosition;
import com.ibm.icu.text.CompactDecimalFormat.CompactStyle;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.MeasureFormat.FormatWidth;
import com.ibm.icu.text.NumberingSystem;
import com.ibm.icu.text.PluralRules.IFixedDecimal;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.Currency.CurrencyUsage;
import com.ibm.icu.util.CurrencyAmount;
import com.ibm.icu.util.ICUUncheckedIOException;
import com.ibm.icu.util.Measure;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.ULocale;

import newapi.impl.GroupingImpl;
import newapi.impl.IntegerWidthImpl;
import newapi.impl.MicroProps;
import newapi.impl.NotationImpl.NotationCompactImpl;
import newapi.impl.NotationImpl.NotationScientificImpl;
import newapi.impl.NumberFormatterImpl;
import newapi.impl.PaddingImpl;
import newapi.impl.RoundingImpl.RoundingImplCurrency;
import newapi.impl.RoundingImpl.RoundingImplFraction;
import newapi.impl.RoundingImpl.RoundingImplIncrement;
import newapi.impl.RoundingImpl.RoundingImplInfinity;
import newapi.impl.RoundingImpl.RoundingImplSignificant;

public final class NumberFormatter {

  public interface IRounding {
    public BigDecimal round(BigDecimal input);
  }

  public interface IGrouping {
    public boolean groupAtPosition(int position, BigDecimal input);
  }

  // This could possibly be combined into MeasureFormat.FormatWidth
  public static enum CurrencyDisplay {
    SYMBOL, // ¤
    ISO_4217, // ¤¤
    DISPLAY_NAME, // ¤¤¤
    SYMBOL_NARROW, // ¤¤¤¤
    HIDDEN, // uses currency rounding and formatting but omits the currency symbol
    // TODO: For hidden, what to do if currency symbol appears in the middle, as in Portugal ?
  }

  public static enum DecimalMarkDisplay {
    AUTO,
    ALWAYS,
  }

  public static enum SignDisplay {
    AUTO,
    ALWAYS,
    NEVER,
  }

  public static class UnlocalizedNumberFormatter {

    public UnlocalizedNumberFormatter notation(Notation notation) {
      throw new AssertionError("See NumberFormatterImpl");
    }

    public UnlocalizedNumberFormatter unit(MeasureUnit unit) {
      throw new AssertionError("See NumberFormatterImpl");
    }

    public UnlocalizedNumberFormatter rounding(IRounding rounding) {
      throw new AssertionError("See NumberFormatterImpl");
    }

    public UnlocalizedNumberFormatter grouping(IGrouping grouping) {
      throw new AssertionError("See NumberFormatterImpl");
    }

    public UnlocalizedNumberFormatter padding(Padding padding) {
      throw new AssertionError("See NumberFormatterImpl");
    }

    public UnlocalizedNumberFormatter integerWidth(IntegerWidth style) {
      throw new AssertionError("See NumberFormatterImpl");
    }

    public UnlocalizedNumberFormatter symbols(DecimalFormatSymbols symbols) {
      throw new AssertionError("See NumberFormatterImpl");
    }

    public UnlocalizedNumberFormatter symbols(NumberingSystem ns) {
      throw new AssertionError("See NumberFormatterImpl");
    }

    public UnlocalizedNumberFormatter unitWidth(FormatWidth style) {
      throw new AssertionError("See NumberFormatterImpl");
    }

    public UnlocalizedNumberFormatter sign(SignDisplay style) {
      throw new AssertionError("See NumberFormatterImpl");
    }

    public UnlocalizedNumberFormatter decimal(DecimalMarkDisplay style) {
      throw new AssertionError("See NumberFormatterImpl");
    }

    public LocalizedNumberFormatter locale(Locale locale) {
      throw new AssertionError("See NumberFormatterImpl");
    }

    public LocalizedNumberFormatter locale(ULocale locale) {
      throw new AssertionError("See NumberFormatterImpl");
    }

    public String toSkeleton() {
      throw new AssertionError("See NumberFormatterImpl");
    }

    // Prevent external subclassing with private constructor
    private UnlocalizedNumberFormatter() {}
  }

  public static class LocalizedNumberFormatter extends UnlocalizedNumberFormatter {

    @Override
    public UnlocalizedNumberFormatter notation(Notation notation) {
      throw new AssertionError("See NumberFormatterImpl");
    }

    @Override
    public LocalizedNumberFormatter unit(MeasureUnit unit) {
      throw new AssertionError("See NumberFormatterImpl");
    }

    @Override
    public LocalizedNumberFormatter rounding(IRounding rounding) {
      throw new AssertionError("See NumberFormatterImpl");
    }

    @Override
    public LocalizedNumberFormatter grouping(IGrouping grouping) {
      throw new AssertionError("See NumberFormatterImpl");
    }

    @Override
    public LocalizedNumberFormatter padding(Padding padding) {
      throw new AssertionError("See NumberFormatterImpl");
    }

    @Override
    public LocalizedNumberFormatter integerWidth(IntegerWidth style) {
      throw new AssertionError("See NumberFormatterImpl");
    }

    @Override
    public LocalizedNumberFormatter symbols(DecimalFormatSymbols symbols) {
      throw new AssertionError("See NumberFormatterImpl");
    }

    @Override
    public LocalizedNumberFormatter symbols(NumberingSystem ns) {
      throw new AssertionError("See NumberFormatterImpl");
    }

    @Override
    public LocalizedNumberFormatter unitWidth(FormatWidth style) {
      throw new AssertionError("See NumberFormatterImpl");
    }

    @Override
    public LocalizedNumberFormatter sign(SignDisplay style) {
      throw new AssertionError("See NumberFormatterImpl");
    }

    @Override
    public LocalizedNumberFormatter decimal(DecimalMarkDisplay style) {
      throw new AssertionError("See NumberFormatterImpl");
    }

    public NumberFormatterResult format(long input) {
      throw new AssertionError("See NumberFormatterImpl");
    }

    public NumberFormatterResult format(double input) {
      throw new AssertionError("See NumberFormatterImpl");
    }

    public NumberFormatterResult format(Number input) {
      throw new AssertionError("See NumberFormatterImpl");
    }

    public NumberFormatterResult format(Measure input) {
      throw new AssertionError("See NumberFormatterImpl");
    }

    // Prevent external subclassing with private constructor
    private LocalizedNumberFormatter() {}

    /**
     * @internal
     * @deprecated This API is for ICU internal use only.
     */
    @Deprecated
    public static class Internal extends LocalizedNumberFormatter {}
  }

  public static UnlocalizedNumberFormatter fromSkeleton(String skeleton) {
    // FIXME
    throw new UnsupportedOperationException();
  }

  public static UnlocalizedNumberFormatter with() {
    return NumberFormatterImpl.with();
  }

  public static LocalizedNumberFormatter withLocale(Locale locale) {
    return NumberFormatterImpl.with().locale(locale);
  }

  public static LocalizedNumberFormatter withLocale(ULocale locale) {
    return NumberFormatterImpl.with().locale(locale);
  }

  public static class NumberFormatterResult {
    NumberStringBuilder nsb;
    FormatQuantityBCD fq;
    MicroProps micros;

    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public NumberFormatterResult(NumberStringBuilder nsb, FormatQuantityBCD fq, MicroProps micros) {
      this.nsb = nsb;
      this.fq = fq;
      this.micros = micros;
    }

    @Override
    public String toString() {
      return nsb.toString();
    }

    public <A extends Appendable> A appendTo(A appendable) {
      try {
        appendable.append(nsb);
      } catch (IOException e) {
        // Throw as an unchecked exception to avoid users needing try/catch
        throw new ICUUncheckedIOException(e);
      }
      return appendable;
    }

    public AttributedCharacterIterator toAttributedCharacterIterator() {
      return nsb.getIterator();
    }

    /**
     * @internal
     * @deprecated This API a technology preview. It is not stable and may change or go away in an
     *     upcoming release.
     */
    @Deprecated
    public void populateFieldPosition(FieldPosition fieldPosition, int offset) {
      nsb.populateFieldPosition(fieldPosition, offset);
      fq.populateUFieldPosition(fieldPosition);
    }

    /**
     * @internal
     * @deprecated This API a technology preview. It is not stable and may change or go away in an
     *     upcoming release.
     */
    @Deprecated
    public String getPrefix() {
      return micros.modOuter.getPrefix()
          + micros.modMiddle.getPrefix()
          + micros.modInner.getPrefix();
    }

    /**
     * @internal
     * @deprecated This API a technology preview. It is not stable and may change or go away in an
     *     upcoming release.
     */
    @Deprecated
    public String getSuffix() {
      return micros.modInner.getSuffix()
          + micros.modMiddle.getSuffix()
          + micros.modOuter.getSuffix();
    }

    /**
     * @internal
     * @deprecated This API a technology preview. It is not stable and may change or go away in an
     *     upcoming release.
     */
    @Deprecated
    public IFixedDecimal getFixedDecimal() {
      return fq;
    }

    public BigDecimal toBigDecimal() {
      return fq.toBigDecimal();
    }

    @Override
    public int hashCode() {
      // NumberStringBuilder and BigDecimal are mutable, so we can't call
      // #equals() or #hashCode() on them directly.
      return Arrays.hashCode(nsb.toCharArray())
          ^ Arrays.hashCode(nsb.toFieldArray())
          ^ fq.toBigDecimal().hashCode();
    }

    @Override
    public boolean equals(Object other) {
      if (this == other) return true;
      if (other == null) return false;
      if (!(other instanceof NumberFormatterResult)) return false;
      // NumberStringBuilder and BigDecimal are mutable, so we can't call
      // #equals() or #hashCode() on them directly.
      NumberFormatterResult _other = (NumberFormatterResult) other;
      return Arrays.equals(nsb.toCharArray(), _other.nsb.toCharArray())
          ^ Arrays.equals(nsb.toFieldArray(), _other.nsb.toFieldArray())
          ^ fq.toBigDecimal().equals(_other.fq.toBigDecimal());
    }
  }

  public static class Notation {

    // FIXME: Support engineering intervals other than 3?
    public static final NotationScientific SCIENTIFIC = new NotationScientificImpl(1);
    public static final NotationScientific ENGINEERING = new NotationScientificImpl(3);
    public static final NotationCompact COMPACT_SHORT = new NotationCompactImpl(CompactStyle.SHORT);
    public static final NotationCompact COMPACT_LONG = new NotationCompactImpl(CompactStyle.LONG);
    public static final NotationSimple SIMPLE = new NotationSimple();

    // Prevent subclassing
    private Notation() {}
  }

  @SuppressWarnings("unused")
  public static class NotationScientific extends Notation {

    public NotationScientific withMinExponentDigits(int minExponentDigits) {
      // Overridden in NotationImpl
      throw new AssertionError();
    }

    public NotationScientific withExponentSignDisplay(SignDisplay exponentSignDisplay) {
      // Overridden in NotationImpl
      throw new AssertionError();
    }

    // Prevent subclassing
    private NotationScientific() {}

    /**
     * @internal
     * @deprecated This API is for ICU internal use only.
     */
    @Deprecated
    public static class Internal extends NotationScientific {}
  }

  public static class NotationCompact extends Notation {

    // Prevent subclassing
    private NotationCompact() {}

    /**
     * @internal
     * @deprecated This API is for ICU internal use only.
     */
    @Deprecated
    public static class Internal extends NotationCompact {}
  }

  public static class NotationSimple extends Notation {
    // Prevent subclassing
    private NotationSimple() {}
  }

  public static class Rounding implements IRounding {

    // FIXME
    /** @internal */
    public static final int MAX_VALUE = 100;

    public static final Rounding NONE = new RoundingImplInfinity();
    public static final Rounding INTEGER = new RoundingImplFraction();

    public static FractionRounding fixedFraction(int minMaxFrac) {
      if (minMaxFrac >= 0 && minMaxFrac <= MAX_VALUE) {
        return RoundingImplFraction.getInstance(minMaxFrac, minMaxFrac);
      } else {
        throw new IllegalArgumentException("Fraction length must be between 0 and " + MAX_VALUE);
      }
    }

    public static FractionRounding minFraction(int minFrac) {
      if (minFrac >= 0 && minFrac < MAX_VALUE) {
        return RoundingImplFraction.getInstance(minFrac, Integer.MAX_VALUE);
      } else {
        throw new IllegalArgumentException("Fraction length must be between 0 and " + MAX_VALUE);
      }
    }

    public static FractionRounding maxFraction(int maxFrac) {
      if (maxFrac >= 0 && maxFrac < MAX_VALUE) {
        return RoundingImplFraction.getInstance(0, maxFrac);
      } else {
        throw new IllegalArgumentException("Fraction length must be between 0 and " + MAX_VALUE);
      }
    }

    public static FractionRounding minMaxFraction(int minFrac, int maxFrac) {
      if (minFrac >= 0 && maxFrac <= MAX_VALUE && minFrac <= maxFrac) {
        return RoundingImplFraction.getInstance(minFrac, maxFrac);
      } else {
        throw new IllegalArgumentException("Fraction length must be between 0 and " + MAX_VALUE);
      }
    }

    public static Rounding fixedFigures(int minMaxSig) {
      if (minMaxSig > 0 && minMaxSig <= MAX_VALUE) {
        return RoundingImplSignificant.getInstance(minMaxSig, minMaxSig);
      } else {
        throw new IllegalArgumentException("Significant digits must be between 0 and " + MAX_VALUE);
      }
    }

    public static Rounding minFigures(int minSig) {
      if (minSig > 0 && minSig <= MAX_VALUE) {
        return RoundingImplSignificant.getInstance(minSig, Integer.MAX_VALUE);
      } else {
        throw new IllegalArgumentException("Significant digits must be between 0 and " + MAX_VALUE);
      }
    }

    public static Rounding maxFigures(int maxSig) {
      if (maxSig > 0 && maxSig <= MAX_VALUE) {
        return RoundingImplSignificant.getInstance(0, maxSig);
      } else {
        throw new IllegalArgumentException("Significant digits must be between 0 and " + MAX_VALUE);
      }
    }

    public static Rounding minMaxFigures(int minSig, int maxSig) {
      if (minSig > 0 && maxSig <= MAX_VALUE && minSig <= maxSig) {
        return RoundingImplSignificant.getInstance(minSig, maxSig);
      } else {
        throw new IllegalArgumentException("Significant digits must be between 0 and " + MAX_VALUE);
      }
    }

    public static Rounding increment(BigDecimal roundingIncrement) {
      if (roundingIncrement == null) {
        throw new IllegalArgumentException("Rounding increment must be non-null");
      } else if (roundingIncrement.compareTo(BigDecimal.ZERO) <= 0) {
        throw new IllegalArgumentException("Rounding increment must be positive");
      } else {
        return RoundingImplIncrement.getInstance(roundingIncrement);
      }
    }

    public static CurrencyRounding currency(CurrencyUsage currencyUsage) {
      if (currencyUsage != CurrencyUsage.STANDARD && currencyUsage != CurrencyUsage.CASH) {
        throw new IllegalArgumentException("Unknown CurrencyUsage: " + currencyUsage);
      } else {
        return RoundingImplCurrency.getInstance(currencyUsage);
      }
    }

    /**
     * Sets the {@link java.math.RoundingMode} to use when picking the direction to round (up or
     * down).
     *
     * <p>Common values include {@link RoundingMode#HALF_EVEN}, {@link RoundingMode#HALF_UP}, and
     * {@link RoundingMode#CEILING}. The default is HALF_EVEN.
     *
     * @param roundingMode The RoundingMode to use.
     * @return An immutable object for chaining.
     */
    public Rounding withMode(RoundingMode roundingMode) {
      // Overridden in RoundingImpl
      throw new AssertionError();
    }

    /**
     * Sets a MathContext directly instead of RoundingMode.
     *
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public Rounding withMode(MathContext mathContext) {
      // Overridden in RoundingImpl
      throw new AssertionError();
    }

    @Override
    public BigDecimal round(BigDecimal input) {
      // Overridden in RoundingImpl
      throw new AssertionError();
    }

    // Prevent subclassing
    private Rounding() {}

    /**
     * @internal
     * @deprecated This API is for ICU internal use only.
     */
    @Deprecated
    public static class Internal extends Rounding {}
  }

  /**
   * A rounding strategy based on a minimum and/or maximum number of fraction digits. Allows for a
   * minimum or maximum number of significant digits to be specified.
   */
  public static class FractionRounding extends Rounding {
    /**
     * Ensures that no less than this number of significant figures are retained when rounding
     * according to fraction rules.
     *
     * <p>For example, with integer rounding, the number 3.141 becomes "3". However, with minimum
     * figures set to 2, 3.141 becomes "3.1" instead.
     *
     * <p>This setting does not affect the number of trailing zeros. For example, 3.01 would print
     * as "3", not "3.0".
     *
     * @param minFigures The number of significant figures to guarantee.
     * @return An immutable object for chaining.
     */
    public Rounding withMinFigures(int minFigures) {
      // Overridden in RoundingImpl
      throw new AssertionError();
    }

    /**
     * Ensures that no more than this number of significant figures are retained when rounding
     * according to fraction rules.
     *
     * <p>For example, with integer rounding, the number 123.4 becomes "123". However, with maximum
     * figures set to 2, 123.4 becomes "120" instead.
     *
     * <p>This setting does not affect the number of trailing zeros. For example, with fixed
     * fraction of 2, 123.4 would become "120.00".
     *
     * @param maxFigures
     * @return An immutable object for chaining.
     */
    public Rounding withMaxFigures(int maxFigures) {
      // Overridden in RoundingImpl
      throw new AssertionError();
    }

    // Prevent subclassing
    private FractionRounding() {}

    /**
     * @internal
     * @deprecated This API is for ICU internal use only.
     */
    @Deprecated
    public static class Internal extends FractionRounding {}
  }

  /** A rounding strategy parameterized by a currency. */
  public static class CurrencyRounding extends Rounding {
    /**
     * Associates a {@link com.ibm.icu.util.Currency} with this rounding strategy. Only applies to
     * rounding strategies returned from {@link #currency(CurrencyUsage)}.
     *
     * <p><strong>Calling this method is <em>not required</em></strong>, because the currency
     * specified in {@link NumberFormatter#unit(MeasureUnit)} or via a {@link CurrencyAmount} passed
     * into {@link LocalizedNumberFormatter#format(Measure)} is automatically applied to currency
     * rounding strategies. However, this method enables you to override that automatic association.
     *
     * <p>This method also enables numbers to be formatted using currency rounding rules without
     * explicitly using a currency format.
     *
     * @param currency The currency to associate with this rounding strategy.
     * @return An immutable object for chaining.
     */
    public Rounding withCurrency(Currency currency) {
      // Overridden in RoundingImpl
      throw new AssertionError();
    }

    // Prevent subclassing
    private CurrencyRounding() {}

    /**
     * @internal
     * @deprecated This API is for ICU internal use only.
     */
    @Deprecated
    public static class Internal extends CurrencyRounding {}
  }

  public static class Grouping implements IGrouping {

    public static final Grouping DEFAULT = new GroupingImpl(GroupingImpl.TYPE_PLACEHOLDER);
    public static final Grouping MIN_2_DIGITS = new GroupingImpl(GroupingImpl.TYPE_MIN2);
    public static final Grouping NONE = new GroupingImpl(GroupingImpl.TYPE_NONE);

    @Override
    public boolean groupAtPosition(int position, BigDecimal input) {
      throw new UnsupportedOperationException(
          "This grouping strategy cannot be used outside of number formatting.");
    }

    // Prevent subclassing
    private Grouping() {}

    /**
     * @internal
     * @deprecated This API is for ICU internal use only.
     */
    @Deprecated
    public static class Internal extends Grouping {}
  }

  public static class Padding {

    public static final Padding NONE = new PaddingImpl();

    public static Padding codePoints(int cp, int targetWidth, PadPosition position) {
        // TODO: Validate the code point
      if (targetWidth >= 0) {
        String paddingString = String.valueOf(Character.toChars(cp));
        return PaddingImpl.getInstance(paddingString, targetWidth, position);
      } else {
        throw new IllegalArgumentException("Padding width must not be negative");
      }
    }

    // Prevent subclassing
    private Padding() {}

    /**
     * @internal
     * @deprecated This API is for ICU internal use only.
     */
    @Deprecated
    public static class Internal extends Padding {}
  }

  @SuppressWarnings("unused")
  public static class IntegerWidth {

    public static final IntegerWidth DEFAULT = new IntegerWidthImpl();

    public static IntegerWidth zeroFillTo(int minInt) {
      if (minInt >= 0 && minInt < Rounding.MAX_VALUE) {
        return new IntegerWidthImpl(minInt, Integer.MAX_VALUE);
      } else {
        throw new IllegalArgumentException(
            "Integer digits must be between 0 and " + Rounding.MAX_VALUE);
      }
    }

    public IntegerWidth truncateAt(int maxInt) {
      // Implemented in IntegerWidthImpl
      throw new AssertionError();
    }

    // Prevent subclassing
    private IntegerWidth() {}

    /**
     * @internal
     * @deprecated This API is for ICU internal use only.
     */
    @Deprecated
    public static class Internal extends IntegerWidth {}
  }
}
