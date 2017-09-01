// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package newapi;

import com.ibm.icu.impl.StandardPlural;
import com.ibm.icu.impl.number.AffixPatternUtils;
import com.ibm.icu.impl.number.AffixPatternUtils.SymbolProvider;
import com.ibm.icu.impl.number.FormatQuantity;
import com.ibm.icu.impl.number.PatternParser;
import com.ibm.icu.impl.number.Modifier;
import com.ibm.icu.impl.number.NumberStringBuilder;
import com.ibm.icu.impl.number.modifiers.ConstantMultiFieldModifier;
import com.ibm.icu.impl.number.modifiers.CurrencySpacingEnabledModifier;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.util.Currency;

import newapi.NumberFormatter.SignDisplay;
import newapi.NumberFormatter.UnitWidth;
import newapi.impl.AffixPatternProvider;
import newapi.impl.MicroProps;
import newapi.impl.MicroPropsGenerator;

/**
 * This class is a {@link Modifier} that wraps a decimal format pattern. It applies the pattern's affixes in
 * {@link Modifier#apply}.
 *
 * <p>
 * In addition to being a Modifier, this class contains the business logic for substituting the correct locale symbols
 * into the affixes of the decimal format pattern.
 *
 * <p>
 * In order to use this class, create a new instance and call the following four setters: {@link #setPatternInfo},
 * {@link #setPatternAttributes}, {@link #setSymbols}, and {@link #setNumberProperties}. After calling these four
 * setters, the instance will be ready for use as a Modifier.
 *
 * <p>
 * This is a MUTABLE, NON-THREAD-SAFE class designed for performance. Do NOT save references to this or attempt to use
 * it from multiple threads! Instead, you can obtain a safe, immutable decimal format pattern modifier by calling
 * {@link MutablePatternModifier#createImmutable}, in effect treating this instance as a builder for the immutable
 * variant.
 *
 * FIXME: Make this package-private
 */
public class MutablePatternModifier implements Modifier, SymbolProvider, CharSequence, MicroPropsGenerator {

    // Modifier details
    final boolean isStrong;

    // Pattern details
    AffixPatternProvider patternInfo;
    SignDisplay signDisplay;
    boolean perMilleReplacesPercent;

    // Symbol details
    DecimalFormatSymbols symbols;
    UnitWidth unitWidth;
    String currency1;
    String currency2;
    String[] currency3;
    PluralRules rules;

    // Number details
    boolean isNegative;
    StandardPlural plural;

    // QuantityChain details
    MicroPropsGenerator parent;

    // Transient CharSequence fields
    boolean inCharSequenceMode;
    int flags;
    int length;
    boolean prependSign;
    boolean plusReplacesMinusSign;

    /**
     * @param isStrong
     *            Whether the modifier should be considered strong. For more information, see
     *            {@link Modifier#isStrong()}. Most of the time, decimal format pattern modifiers should be considered
     *            as non-strong.
     */
    public MutablePatternModifier(boolean isStrong) {
        this.isStrong = isStrong;
    }

    /**
     * Sets a reference to the parsed decimal format pattern, usually obtained from
     * {@link PatternParser#parse(String)}, but any implementation of {@link AffixPatternProvider} is accepted.
     */
    public void setPatternInfo(AffixPatternProvider patternInfo) {
        this.patternInfo = patternInfo;
    }

    /**
     * Sets attributes that imply changes to the literal interpretation of the pattern string affixes.
     *
     * @param signDisplay
     *            Whether to force a plus sign on positive numbers.
     * @param perMille
     *            Whether to substitute the percent sign in the pattern with a permille sign.
     */
    public void setPatternAttributes(SignDisplay signDisplay, boolean perMille) {
        this.signDisplay = signDisplay;
        this.perMilleReplacesPercent = perMille;
    }

    /**
     * Sets locale-specific details that affect the symbols substituted into the pattern string affixes.
     *
     * @param symbols
     *            The desired instance of DecimalFormatSymbols.
     * @param currency
     *            The currency to be used when substituting currency values into the affixes. Cannot be null, but a
     *            bogus currency like "XXX" can be used.
     * @param unitWidth
     *            The width used to render currencies.
     * @param rules
     *            Required if the triple currency sign, "¤¤¤", appears in the pattern, which can be determined from the
     *            convenience method {@link #needsPlurals()}.
     */
    public void setSymbols(DecimalFormatSymbols symbols, Currency currency, UnitWidth unitWidth, PluralRules rules) {
        assert (rules != null) == needsPlurals();
        this.symbols = symbols;
        this.unitWidth = unitWidth;
        this.rules = rules;

        currency1 = currency.getName(symbols.getULocale(), Currency.SYMBOL_NAME, null);
        currency2 = currency.getCurrencyCode();

        if (rules != null) {
            currency3 = new String[StandardPlural.COUNT];
            for (StandardPlural plural : StandardPlural.VALUES) {
                currency3[plural.ordinal()] = currency.getName(symbols.getULocale(), Currency.PLURAL_LONG_NAME,
                        plural.getKeyword(), null);
            }
        }
    }

    /**
     * Sets attributes of the current number being processed.
     *
     * @param isNegative
     *            Whether the number is negative.
     * @param plural
     *            The plural form of the number, required only if the pattern contains the triple currency sign, "¤¤¤"
     *            (and as indicated by {@link #needsPlurals()}).
     */
    public void setNumberProperties(boolean isNegative, StandardPlural plural) {
        assert (plural != null) == needsPlurals();
        this.isNegative = isNegative;
        this.plural = plural;
    }

    /**
     * Returns true if the pattern represented by this MurkyModifier requires a plural keyword in order to localize.
     * This is currently true only if there is a currency long name placeholder in the pattern ("¤¤¤").
     */
    public boolean needsPlurals() {
        return patternInfo.containsSymbolType(AffixPatternUtils.TYPE_CURRENCY_TRIPLE);
    }

    /**
     * Creates a new quantity-dependent Modifier that behaves the same as the current instance, but which is immutable
     * and can be saved for future use. The number properties in the current instance are mutated; all other properties
     * are left untouched.
     *
     * <p>
     * The resulting modifier cannot be used in a QuantityChain.
     *
     * @return An immutable that supports both positive and negative numbers.
     */
    public ImmutableMurkyModifier createImmutable() {
        return createImmutableAndChain(null);
    }

    /**
     * Creates a new quantity-dependent Modifier that behaves the same as the current instance, but which is immutable
     * and can be saved for future use. The number properties in the current instance are mutated; all other properties
     * are left untouched.
     *
     * @param parent
     *            The QuantityChain to which to chain this immutable.
     * @return An immutable that supports both positive and negative numbers.
     */
    public ImmutableMurkyModifier createImmutableAndChain(MicroPropsGenerator parent) {
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
            return new ImmutableMurkyModifierWithPlurals(mods, rules, parent);
        } else {
            // Faster path when plural keyword is not needed.
            setNumberProperties(false, null);
            Modifier positive = createConstantModifier(a, b);
            setNumberProperties(true, null);
            Modifier negative = createConstantModifier(a, b);
            return new ImmutableMurkyModifierWithoutPlurals(positive, negative, parent);
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

    public static interface ImmutableMurkyModifier extends MicroPropsGenerator {
        public void applyToMicros(MicroProps micros, FormatQuantity quantity);
    }

    public static class ImmutableMurkyModifierWithoutPlurals implements ImmutableMurkyModifier {
        final Modifier positive;
        final Modifier negative;
        final MicroPropsGenerator parent;

        public ImmutableMurkyModifierWithoutPlurals(Modifier positive, Modifier negative, MicroPropsGenerator parent) {
            this.positive = positive;
            this.negative = negative;
            this.parent = parent;
        }

        @Override
        public MicroProps processQuantity(FormatQuantity quantity) {
            assert parent != null;
            MicroProps micros = parent.processQuantity(quantity);
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
        final MicroPropsGenerator parent;

        public ImmutableMurkyModifierWithPlurals(Modifier[] mods, PluralRules rules, MicroPropsGenerator parent) {
            assert mods.length == getModsLength();
            assert rules != null;
            this.mods = mods;
            this.rules = rules;
            this.parent = parent;
        }

        public static int getModsLength() {
            return 2 * StandardPlural.COUNT;
        }

        public static int getModIndex(boolean isNegative, StandardPlural plural) {
            return plural.ordinal() * 2 + (isNegative ? 1 : 0);
        }

        @Override
        public MicroProps processQuantity(FormatQuantity quantity) {
            assert parent != null;
            MicroProps micros = parent.processQuantity(quantity);
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

    public MicroPropsGenerator addToChain(MicroPropsGenerator parent) {
        this.parent = parent;
        return this;
    }

    @Override
    public MicroProps processQuantity(FormatQuantity fq) {
        MicroProps micros = parent.processQuantity(fq);
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

    @Override
    public int apply(NumberStringBuilder output, int leftIndex, int rightIndex) {
        int prefixLen = insertPrefix(output, leftIndex);
        int suffixLen = insertSuffix(output, rightIndex + prefixLen);
        CurrencySpacingEnabledModifier.applyCurrencySpacing(output, leftIndex, prefixLen, rightIndex + prefixLen,
                suffixLen, symbols);
        return prefixLen + suffixLen;
    }

    @Override
    public int getPrefixLength() {
        NumberStringBuilder dummy = new NumberStringBuilder();
        return insertPrefix(dummy, 0);
    }

    @Override
    public boolean isStrong() {
        return isStrong;
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
            if (unitWidth == UnitWidth.ISO_CODE) {
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
        plusReplacesMinusSign = !isNegative
                && (signDisplay == SignDisplay.ALWAYS || signDisplay == SignDisplay.ACCOUNTING_ALWAYS)
                && patternInfo.positiveHasPlusSign() == false;

        // Should we use the negative affix pattern? (If not, we will use the positive one)
        boolean useNegativeAffixPattern = patternInfo.hasNegativeSubpattern()
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
            prependSign = signDisplay != SignDisplay.NEVER;
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
