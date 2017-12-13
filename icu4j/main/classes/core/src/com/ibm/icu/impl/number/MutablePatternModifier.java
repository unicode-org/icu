// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number;

import com.ibm.icu.impl.StandardPlural;
import com.ibm.icu.impl.number.AffixUtils.SymbolProvider;
import com.ibm.icu.number.NumberFormatter.SignDisplay;
import com.ibm.icu.number.NumberFormatter.UnitWidth;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.util.Currency;

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
    Currency currency;
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
     * {@link PatternStringParser#parseToPatternInfo(String)}, but any implementation of {@link AffixPatternProvider} is
     * accepted.
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
     *            The currency to be used when substituting currency values into the affixes.
     * @param unitWidth
     *            The width used to render currencies.
     * @param rules
     *            Required if the triple currency sign, "¤¤¤", appears in the pattern, which can be determined from the
     *            convenience method {@link #needsPlurals()}.
     */
    public void setSymbols(DecimalFormatSymbols symbols, Currency currency, UnitWidth unitWidth, PluralRules rules) {
        //assert (rules != null) == needsPlurals();
        this.symbols = symbols;
        this.currency = currency;
        this.unitWidth = unitWidth;
        this.rules = rules;
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
        return patternInfo.containsSymbolType(AffixUtils.TYPE_CURRENCY_TRIPLE);
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
    public ImmutablePatternModifier createImmutable() {
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
    public ImmutablePatternModifier createImmutableAndChain(MicroPropsGenerator parent) {
        NumberStringBuilder a = new NumberStringBuilder();
        NumberStringBuilder b = new NumberStringBuilder();
        if (needsPlurals()) {
            // Slower path when we require the plural keyword.
            ParameterizedModifier pm = new ParameterizedModifier();
            for (StandardPlural plural : StandardPlural.VALUES) {
                setNumberProperties(false, plural);
                pm.setModifier(false, plural, createConstantModifier(a, b));
                setNumberProperties(true, plural);
                pm.setModifier(true, plural, createConstantModifier(a, b));
            }
            pm.freeze();
            return new ImmutablePatternModifier(pm, rules, parent);
        } else {
            // Faster path when plural keyword is not needed.
            setNumberProperties(false, null);
            Modifier positive = createConstantModifier(a, b);
            setNumberProperties(true, null);
            Modifier negative = createConstantModifier(a, b);
            ParameterizedModifier pm = new ParameterizedModifier(positive, negative);
            return new ImmutablePatternModifier(pm, null, parent);
        }
    }

    /**
     * Uses the current properties to create a single {@link ConstantMultiFieldModifier} with currency spacing support
     * if required.
     *
     * @param a
     *            A working NumberStringBuilder object; passed from the outside to prevent the need to create many new
     *            instances if this method is called in a loop.
     * @param b
     *            Another working NumberStringBuilder object.
     * @return The constant modifier object.
     */
    private ConstantMultiFieldModifier createConstantModifier(NumberStringBuilder a, NumberStringBuilder b) {
        insertPrefix(a.clear(), 0);
        insertSuffix(b.clear(), 0);
        if (patternInfo.hasCurrencySign()) {
            return new CurrencySpacingEnabledModifier(a, b, isStrong, symbols);
        } else {
            return new ConstantMultiFieldModifier(a, b, isStrong);
        }
    }

    public static class ImmutablePatternModifier implements MicroPropsGenerator {
        final ParameterizedModifier pm;
        final PluralRules rules;
        final MicroPropsGenerator parent;

        ImmutablePatternModifier(ParameterizedModifier pm, PluralRules rules, MicroPropsGenerator parent) {
            this.pm = pm;
            this.rules = rules;
            this.parent = parent;
        }

        @Override
        public MicroProps processQuantity(DecimalQuantity quantity) {
            MicroProps micros = parent.processQuantity(quantity);
            applyToMicros(micros, quantity);
            return micros;
        }

        public void applyToMicros(MicroProps micros, DecimalQuantity quantity) {
            if (rules == null) {
                micros.modMiddle = pm.getModifier(quantity.isNegative());
            } else {
                // TODO: Fix this. Avoid the copy.
                DecimalQuantity copy = quantity.createCopy();
                copy.roundToInfinity();
                StandardPlural plural = copy.getStandardPlural(rules);
                micros.modMiddle = pm.getModifier(quantity.isNegative(), plural);
            }
        }
    }

    /** Used by the unsafe code path. */
    public MicroPropsGenerator addToChain(MicroPropsGenerator parent) {
        this.parent = parent;
        return this;
    }

    @Override
    public MicroProps processQuantity(DecimalQuantity fq) {
        MicroProps micros = parent.processQuantity(fq);
        if (needsPlurals()) {
            // TODO: Fix this. Avoid the copy.
            DecimalQuantity copy = fq.createCopy();
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
        // Enter and exit CharSequence Mode to get the length.
        enterCharSequenceMode(true);
        int result = AffixUtils.unescapedCodePointCount(this, this);  // prefix length
        exitCharSequenceMode();
        return result;
    }

    @Override
    public int getCodePointCount() {
        // Enter and exit CharSequence Mode to get the length.
        enterCharSequenceMode(true);
        int result = AffixUtils.unescapedCodePointCount(this, this);  // prefix length
        exitCharSequenceMode();
        enterCharSequenceMode(false);
        result += AffixUtils.unescapedCodePointCount(this, this);  // suffix length
        exitCharSequenceMode();
        return result;
    }

    @Override
    public boolean isStrong() {
        return isStrong;
    }

    private int insertPrefix(NumberStringBuilder sb, int position) {
        enterCharSequenceMode(true);
        int length = AffixUtils.unescape(this, sb, position, this);
        exitCharSequenceMode();
        return length;
    }

    private int insertSuffix(NumberStringBuilder sb, int position) {
        enterCharSequenceMode(false);
        int length = AffixUtils.unescape(this, sb, position, this);
        exitCharSequenceMode();
        return length;
    }

    /**
     * Returns the string that substitutes a given symbol type in a pattern.
     */
    @Override
    public CharSequence getSymbol(int type) {
        switch (type) {
        case AffixUtils.TYPE_MINUS_SIGN:
            return symbols.getMinusSignString();
        case AffixUtils.TYPE_PLUS_SIGN:
            return symbols.getPlusSignString();
        case AffixUtils.TYPE_PERCENT:
            return symbols.getPercentString();
        case AffixUtils.TYPE_PERMILLE:
            return symbols.getPerMillString();
        case AffixUtils.TYPE_CURRENCY_SINGLE:
            // UnitWidth ISO, HIDDEN, or NARROW overrides the singular currency symbol.
            if (unitWidth == UnitWidth.ISO_CODE) {
                return currency.getCurrencyCode();
            } else if (unitWidth == UnitWidth.HIDDEN) {
                return "";
            } else if (unitWidth == UnitWidth.NARROW) {
                return currency.getName(symbols.getULocale(), Currency.NARROW_SYMBOL_NAME, null);
            } else {
                return currency.getName(symbols.getULocale(), Currency.SYMBOL_NAME, null);
            }
        case AffixUtils.TYPE_CURRENCY_DOUBLE:
            return currency.getCurrencyCode();
        case AffixUtils.TYPE_CURRENCY_TRIPLE:
            // NOTE: This is the code path only for patterns containing "¤¤¤".
            // Plural currencies set via the API are formatted in LongNameHandler.
            // This code path is used by DecimalFormat via CurrencyPluralInfo.
            assert plural != null;
            return currency.getName(symbols.getULocale(), Currency.PLURAL_LONG_NAME, plural.getKeyword(), null);
        case AffixUtils.TYPE_CURRENCY_QUAD:
            return "\uFFFD";
        case AffixUtils.TYPE_CURRENCY_QUINT:
            return currency.getName(symbols.getULocale(), Currency.NARROW_SYMBOL_NAME, null);
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

        // Should we use the affix from the negative subpattern? (If not, we will use the positive subpattern.)
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
        assert inCharSequenceMode;
        return length;
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
        // Never called by AffixUtils
        throw new AssertionError();
    }
}
