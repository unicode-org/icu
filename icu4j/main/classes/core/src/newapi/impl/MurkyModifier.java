// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package newapi.impl;

import com.ibm.icu.impl.StandardPlural;
import com.ibm.icu.impl.number.AffixPatternUtils;
import com.ibm.icu.impl.number.AffixPatternUtils.SymbolProvider;
import com.ibm.icu.impl.number.FormatQuantity;
import com.ibm.icu.impl.number.Modifier;
import com.ibm.icu.impl.number.NumberStringBuilder;
import com.ibm.icu.impl.number.modifiers.ConstantMultiFieldModifier;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.MeasureFormat.FormatWidth;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.util.Currency;

import newapi.NumberFormatter.SignDisplay;

/**
 * This is a MUTABLE, NON-THREAD-SAFE class designed for performance. Do NOT save references to this
 * or attempt to use it from multiple threads!!!
 *
 * <p>This class takes a parsed pattern and returns a Modifier, without creating any objects. When
 * the Modifier methods are called, symbols are substituted directly into the output
 * NumberStringBuilder, without creating any intermediate Strings.
 */
public class MurkyModifier implements Modifier, SymbolProvider, CharSequence, QuantityChain {

  // Modifier details
  final boolean isStrong;

  // Pattern details
  AffixPatternProvider patternInfo;
  SignDisplay signDisplay;
  boolean perMilleReplacesPercent;

  // Symbol details
  DecimalFormatSymbols symbols;
  FormatWidth unitWidth;
  String currency1;
  String currency2;
  String[] currency3;
  PluralRules rules;

  // Number details
  boolean isNegative;
  StandardPlural plural;

  // QuantityChain details
  QuantityChain parent;

  // Transient CharSequence fields
  boolean inCharSequenceMode;
  int flags;
  int length;
  boolean prependSign;
  boolean plusReplacesMinusSign;

  public MurkyModifier(boolean isStrong) {
    this.isStrong = isStrong;
  }

  public void setPatternInfo(AffixPatternProvider patternInfo) {
    this.patternInfo = patternInfo;
  }

  public void setPatternAttributes(SignDisplay signDisplay, boolean perMille) {
    this.signDisplay = signDisplay;
    this.perMilleReplacesPercent = perMille;
  }

  public void setSymbols(
      DecimalFormatSymbols symbols, Currency currency, FormatWidth unitWidth, PluralRules rules) {
    assert (rules != null) == needsPlurals();
    this.symbols = symbols;
    this.unitWidth = unitWidth;
    this.rules = rules;

    currency1 = currency.getName(symbols.getULocale(), Currency.SYMBOL_NAME, null);
    currency2 = currency.getCurrencyCode();

    if (rules != null) {
      currency3 = new String[StandardPlural.COUNT];
      for (StandardPlural plural : StandardPlural.VALUES) {
        currency3[plural.ordinal()] =
            currency.getName(
                symbols.getULocale(), Currency.PLURAL_LONG_NAME, plural.getKeyword(), null);
      }
    }
  }

  public void setNumberProperties(boolean isNegative, StandardPlural plural) {
    assert (plural != null) == needsPlurals();
    this.isNegative = isNegative;
    this.plural = plural;
  }

  /**
   * Returns true if the pattern represented by this MurkyModifier requires a plural keyword in
   * order to localize. This is currently true only if there is a currency long name placeholder in
   * the pattern.
   */
  public boolean needsPlurals() {
    return patternInfo.containsSymbolType(AffixPatternUtils.TYPE_CURRENCY_TRIPLE);
  }

  @Override
  public QuantityChain chain(QuantityChain parent) {
    this.parent = parent;
    return this;
  }

  @Override
  public MicroProps withQuantity(FormatQuantity fq) {
    MicroProps micros = parent.withQuantity(fq);
    if (needsPlurals()) {
      // TODO: Fix this. Avoid the copy.
      FormatQuantity copy = fq.createCopy();
      micros.rounding.apply(copy);
      setNumberProperties(fq.isNegative(), copy.getStandardPlural(rules));
    } else {
      setNumberProperties(fq.isNegative(), null);
    }
    micros.modMiddle = this;
    return micros;
  }

  /**
   * Creates a new quantity-dependent Modifier that behaves the same as the current instance, but
   * which is immutable and can be saved for future use. The current instance is not changed by
   * calling this method except for the number properties.
   *
   * @return An immutable that supports both positive and negative numbers.
   */
  public ImmutableMurkyModifier createImmutable() {
    NumberStringBuilder a = new NumberStringBuilder();
    NumberStringBuilder b = new NumberStringBuilder();
    if (needsPlurals()) {
      // Slower path when we require the plural keyword.
      Modifier[] mods = new Modifier[ImmutableMurkyModifierWithPlurals.getModsLength()];
      for (StandardPlural plural : StandardPlural.VALUES) {
        setNumberProperties(false, plural);
        Modifier positive = createConstantModifier(a, b);
        setNumberProperties(true, plural);
        Modifier negative = createConstantModifier(a, b);
        mods[ImmutableMurkyModifierWithPlurals.getModIndex(false, plural)] = positive;
        mods[ImmutableMurkyModifierWithPlurals.getModIndex(true, plural)] = negative;
      }
      return new ImmutableMurkyModifierWithPlurals(mods, rules);
    } else {
      // Faster path when plural keyword is not needed.
      setNumberProperties(false, null);
      Modifier positive = createConstantModifier(a, b);
      setNumberProperties(true, null);
      Modifier negative = createConstantModifier(a, b);
      return new ImmutableMurkyModifierWithoutPlurals(positive, negative);
    }
  }

  private Modifier createConstantModifier(NumberStringBuilder a, NumberStringBuilder b) {
    insertPrefix(a.clear(), 0);
    insertSuffix(b.clear(), 0);
    if (patternInfo.hasCurrencySign()) {
      return new CurrencySpacingEnabledModifier(a, b, isStrong, symbols);
    } else {
      return new ConstantMultiFieldModifier(a, b, isStrong);
    }
  }

  public static interface ImmutableMurkyModifier extends QuantityChain {
    public void applyToMicros(MicroProps micros, FormatQuantity quantity);
  }

  public static class ImmutableMurkyModifierWithoutPlurals implements ImmutableMurkyModifier {
    final Modifier positive;
    final Modifier negative;
    /* final */ QuantityChain parent;

    public ImmutableMurkyModifierWithoutPlurals(Modifier positive, Modifier negative) {
      this.positive = positive;
      this.negative = negative;
    }

    @Override
    public QuantityChain chain(QuantityChain parent) {
      this.parent = parent;
      return this;
    }

    @Override
    public MicroProps withQuantity(FormatQuantity quantity) {
      MicroProps micros = parent.withQuantity(quantity);
      applyToMicros(micros, quantity);
      return micros;
    }

    @Override
    public void applyToMicros(MicroProps micros, FormatQuantity quantity) {
      if (quantity.isNegative()) {
        micros.modMiddle = negative;
      } else {
        micros.modMiddle = positive;
      }
    }
  }

  public static class ImmutableMurkyModifierWithPlurals implements ImmutableMurkyModifier {
    final Modifier[] mods;
    final PluralRules rules;
    /* final */ QuantityChain parent;

    public ImmutableMurkyModifierWithPlurals(Modifier[] mods, PluralRules rules) {
      assert mods.length == getModsLength();
      assert rules != null;
      this.mods = mods;
      this.rules = rules;
    }

    public static int getModsLength() {
      return 2 * StandardPlural.COUNT;
    }

    public static int getModIndex(boolean isNegative, StandardPlural plural) {
      return plural.ordinal() * 2 + (isNegative ? 1 : 0);
    }

    @Override
    public QuantityChain chain(QuantityChain parent) {
      this.parent = parent;
      return this;
    }

    @Override
    public MicroProps withQuantity(FormatQuantity quantity) {
      MicroProps micros = parent.withQuantity(quantity);
      applyToMicros(micros, quantity);
      return micros;
    }

    @Override
    public void applyToMicros(MicroProps micros, FormatQuantity quantity) {
      // TODO: Fix this. Avoid the copy.
      FormatQuantity copy = quantity.createCopy();
      copy.roundToInfinity();
      StandardPlural plural = copy.getStandardPlural(rules);
      Modifier mod = mods[getModIndex(quantity.isNegative(), plural)];
      micros.modMiddle = mod;
    }
  }

  @Override
  public int apply(NumberStringBuilder output, int leftIndex, int rightIndex) {
    int prefixLen = insertPrefix(output, leftIndex);
    int suffixLen = insertSuffix(output, rightIndex + prefixLen);
    CurrencySpacingEnabledModifier.applyCurrencySpacing(
        output, leftIndex, prefixLen, rightIndex + prefixLen, suffixLen, symbols);
    return prefixLen + suffixLen;
  }

  @Override
  public boolean isStrong() {
    return isStrong;
  }

  @Override
  public String getPrefix() {
    NumberStringBuilder sb = new NumberStringBuilder(10);
    insertPrefix(sb, 0);
    return sb.toString();
  }

  @Override
  public String getSuffix() {
    NumberStringBuilder sb = new NumberStringBuilder(10);
    insertSuffix(sb, 0);
    return sb.toString();
  }

  private int insertPrefix(NumberStringBuilder sb, int position) {
    enterCharSequenceMode(true);
    int length = AffixPatternUtils.unescape(this, sb, position, this);
    exitCharSequenceMode();
    return length;
  }

  private int insertSuffix(NumberStringBuilder sb, int position) {
    enterCharSequenceMode(false);
    int length = AffixPatternUtils.unescape(this, sb, position, this);
    exitCharSequenceMode();
    return length;
  }

  @Override
  public CharSequence getSymbol(int type) {
    switch (type) {
      case AffixPatternUtils.TYPE_MINUS_SIGN:
        return symbols.getMinusSignString();
      case AffixPatternUtils.TYPE_PLUS_SIGN:
        return symbols.getPlusSignString();
      case AffixPatternUtils.TYPE_PERCENT:
        return symbols.getPercentString();
      case AffixPatternUtils.TYPE_PERMILLE:
        return symbols.getPerMillString();
      case AffixPatternUtils.TYPE_CURRENCY_SINGLE:
        // FormatWidth ISO overrides the singular currency symbol
        if (unitWidth == FormatWidth.SHORT) {
          return currency2;
        } else {
          return currency1;
        }
      case AffixPatternUtils.TYPE_CURRENCY_DOUBLE:
        return currency2;
      case AffixPatternUtils.TYPE_CURRENCY_TRIPLE:
        // NOTE: This is the code path only for patterns containing "".
        // Most plural currencies are formatted in DataUtils.
        assert plural != null;
        if (currency3 == null) {
          return currency2;
        } else {
          return currency3[plural.ordinal()];
        }
      case AffixPatternUtils.TYPE_CURRENCY_QUAD:
        return "\uFFFD";
      case AffixPatternUtils.TYPE_CURRENCY_QUINT:
        return "\uFFFD";
      default:
        throw new AssertionError();
    }
  }

  /** This method contains the heart of the logic for rendering LDML affix strings. */
  private void enterCharSequenceMode(boolean isPrefix) {
    assert !inCharSequenceMode;
    inCharSequenceMode = true;

    // Should the output render '+' where '-' would normally appear in the pattern?
    plusReplacesMinusSign =
        !isNegative
            && signDisplay == SignDisplay.ALWAYS_SHOWN
            && patternInfo.positiveHasPlusSign() == false;

    // Should we use the negative affix pattern? (If not, we will use the positive one)
    boolean useNegativeAffixPattern =
        patternInfo.hasNegativeSubpattern()
            && (isNegative || (patternInfo.negativeHasMinusSign() && plusReplacesMinusSign));

    // Resolve the flags for the affix pattern.
    flags = 0;
    if (useNegativeAffixPattern) {
      flags |= AffixPatternProvider.Flags.NEGATIVE_SUBPATTERN;
    }
    if (isPrefix) {
      flags |= AffixPatternProvider.Flags.PREFIX;
    }
    if (plural != null) {
      assert plural.ordinal() == (AffixPatternProvider.Flags.PLURAL_MASK & plural.ordinal());
      flags |= plural.ordinal();
    }

    // Should we prepend a sign to the pattern?
    if (!isPrefix || useNegativeAffixPattern) {
      prependSign = false;
    } else if (isNegative) {
      prependSign = signDisplay != SignDisplay.NEVER_SHOWN;
    } else {
      prependSign = plusReplacesMinusSign;
    }

    // Finally, compute the length of the affix pattern.
    length = patternInfo.length(flags) + (prependSign ? 1 : 0);
  }

  private void exitCharSequenceMode() {
    assert inCharSequenceMode;
    inCharSequenceMode = false;
  }

  @Override
  public int length() {
    if (inCharSequenceMode) {
      return length;
    } else {
      NumberStringBuilder sb = new NumberStringBuilder(20);
      apply(sb, 0, 0);
      return sb.length();
    }
  }

  @Override
  public char charAt(int index) {
    assert inCharSequenceMode;
    char candidate;
    if (prependSign && index == 0) {
      candidate = '-';
    } else if (prependSign) {
      candidate = patternInfo.charAt(flags, index - 1);
    } else {
      candidate = patternInfo.charAt(flags, index);
    }
    if (plusReplacesMinusSign && candidate == '-') {
      return '+';
    }
    if (perMilleReplacesPercent && candidate == '%') {
      return '‰';
    }
    return candidate;
  }

  @Override
  public CharSequence subSequence(int start, int end) {
    // Should never be called in normal circumstances
    throw new AssertionError();
  }
}
