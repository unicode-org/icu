// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.number;

import com.ibm.icu.impl.FormattedStringBuilder;
import com.ibm.icu.impl.StandardPlural;
import com.ibm.icu.impl.number.AffixUtils.SymbolProvider;
import com.ibm.icu.number.NumberFormatter.SignDisplay;
import com.ibm.icu.number.NumberFormatter.UnitWidth;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.NumberFormat.Field;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.util.Currency;

/**
 * This class is a {@link Modifier} that wraps a decimal format pattern. It applies the pattern's affixes
 * in {@link Modifier#apply}.
 *
 * <p>
 * In addition to being a Modifier, this class contains the business logic for substituting the correct
 * locale symbols into the affixes of the decimal format pattern.
 *
 * <p>
 * In order to use this class, create a new instance and call the following four setters:
 * {@link #setPatternInfo}, {@link #setPatternAttributes}, {@link #setSymbols}, and
 * {@link #setNumberProperties}. After calling these four setters, the instance will be ready for use as
 * a Modifier.
 *
 * <p>
 * This is a MUTABLE, NON-THREAD-SAFE class designed for performance. Do NOT save references to this or
 * attempt to use it from multiple threads! Instead, you can obtain a safe, immutable decimal format
 * pattern modifier by calling {@link MutablePatternModifier#createImmutable}, in effect treating this
 * instance as a builder for the immutable variant.
 */
public class MutablePatternModifier implements Modifier, SymbolProvider, MicroPropsGenerator {

    // Modifier details
    final boolean isStrong;

    // Pattern details
    AffixPatternProvider patternInfo;
    Field field;
    SignDisplay signDisplay;
    boolean perMilleReplacesPercent;

    // Symbol details
    DecimalFormatSymbols symbols;
    UnitWidth unitWidth;
    Currency currency;
    PluralRules rules;

    // Number details
    Signum signum;
    StandardPlural plural;

    // QuantityChain details
    MicroPropsGenerator parent;

    // Transient fields for rendering
    StringBuilder currentAffix;

    /**
     * @param isStrong
     *            Whether the modifier should be considered strong. For more information, see
     *            {@link Modifier#isStrong()}. Most of the time, decimal format pattern modifiers should
     *            be considered as non-strong.
     */
    public MutablePatternModifier(boolean isStrong) {
        this.isStrong = isStrong;
    }

    /**
     * Sets a reference to the parsed decimal format pattern, usually obtained from
     * {@link PatternStringParser#parseToPatternInfo(String)}, but any implementation of
     * {@link AffixPatternProvider} is accepted.
     *
     * @param field
     *            Which field to use for literal characters in the pattern.
     */
    public void setPatternInfo(AffixPatternProvider patternInfo, Field field) {
        this.patternInfo = patternInfo;
        this.field = field;
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
     *            Required if the triple currency sign, "¤¤¤", appears in the pattern, which can be
     *            determined from the convenience method {@link #needsPlurals()}.
     */
    public void setSymbols(
            DecimalFormatSymbols symbols,
            Currency currency,
            UnitWidth unitWidth,
            PluralRules rules) {
        assert (rules != null) == needsPlurals();
        this.symbols = symbols;
        this.currency = currency;
        this.unitWidth = unitWidth;
        this.rules = rules;
    }

    /**
     * Sets attributes of the current number being processed.
     *
     * @param signum
     *            -1 if negative; +1 if positive; or 0 if zero.
     * @param plural
     *            The plural form of the number, required only if the pattern contains the triple
     *            currency sign, "¤¤¤" (and as indicated by {@link #needsPlurals()}).
     */
    public void setNumberProperties(Signum signum, StandardPlural plural) {
        assert (plural != null) == needsPlurals();
        this.signum = signum;
        this.plural = plural;
    }

    /**
     * Returns true if the pattern represented by this MurkyModifier requires a plural keyword in order
     * to localize. This is currently true only if there is a currency long name placeholder in the
     * pattern ("¤¤¤").
     */
    public boolean needsPlurals() {
        return patternInfo.containsSymbolType(AffixUtils.TYPE_CURRENCY_TRIPLE);
    }

    /**
     * Creates a new quantity-dependent Modifier that behaves the same as the current instance, but which
     * is immutable and can be saved for future use. The number properties in the current instance are
     * mutated; all other properties are left untouched.
     *
     * <p>
     * The resulting modifier cannot be used in a QuantityChain.
     *
     * @return An immutable that supports both positive and negative numbers.
     */
    public ImmutablePatternModifier createImmutable() {
        FormattedStringBuilder a = new FormattedStringBuilder();
        FormattedStringBuilder b = new FormattedStringBuilder();
        if (needsPlurals()) {
            // Slower path when we require the plural keyword.
            AdoptingModifierStore pm = new AdoptingModifierStore();
            for (StandardPlural plural : StandardPlural.VALUES) {
                setNumberProperties(Signum.POS, plural);
                pm.setModifier(Signum.POS, plural, createConstantModifier(a, b));
                setNumberProperties(Signum.POS_ZERO, plural);
                pm.setModifier(Signum.POS_ZERO, plural, createConstantModifier(a, b));
                setNumberProperties(Signum.NEG_ZERO, plural);
                pm.setModifier(Signum.NEG_ZERO, plural, createConstantModifier(a, b));
                setNumberProperties(Signum.NEG, plural);
                pm.setModifier(Signum.NEG, plural, createConstantModifier(a, b));
            }
            pm.freeze();
            return new ImmutablePatternModifier(pm, rules);
        } else {
            // Faster path when plural keyword is not needed.
            setNumberProperties(Signum.POS, null);
            Modifier positive = createConstantModifier(a, b);
            setNumberProperties(Signum.POS_ZERO, null);
            Modifier posZero = createConstantModifier(a, b);
            setNumberProperties(Signum.NEG_ZERO, null);
            Modifier negZero = createConstantModifier(a, b);
            setNumberProperties(Signum.NEG, null);
            Modifier negative = createConstantModifier(a, b);
            AdoptingModifierStore pm = new AdoptingModifierStore(positive, posZero, negZero, negative);
            return new ImmutablePatternModifier(pm, null);
        }
    }

    /**
     * Uses the current properties to create a single {@link ConstantMultiFieldModifier} with currency
     * spacing support if required.
     *
     * @param a
     *            A working FormattedStringBuilder object; passed from the outside to prevent the need to
     *            create many new instances if this method is called in a loop.
     * @param b
     *            Another working FormattedStringBuilder object.
     * @return The constant modifier object.
     */
    private ConstantMultiFieldModifier createConstantModifier(
            FormattedStringBuilder a,
            FormattedStringBuilder b) {
        insertPrefix(a.clear(), 0);
        insertSuffix(b.clear(), 0);
        if (patternInfo.hasCurrencySign()) {
            return new CurrencySpacingEnabledModifier(a, b, !patternInfo.hasBody(), isStrong, symbols);
        } else {
            return new ConstantMultiFieldModifier(a, b, !patternInfo.hasBody(), isStrong);
        }
    }

    public static class ImmutablePatternModifier implements MicroPropsGenerator {
        final AdoptingModifierStore pm;
        final PluralRules rules;
        /* final */ MicroPropsGenerator parent;

        ImmutablePatternModifier(
                AdoptingModifierStore pm,
                PluralRules rules) {
            this.pm = pm;
            this.rules = rules;
            this.parent = null;
        }

        public ImmutablePatternModifier addToChain(MicroPropsGenerator parent) {
            this.parent = parent;
            return this;
        }

        @Override
        public MicroProps processQuantity(DecimalQuantity quantity) {
            MicroProps micros = parent.processQuantity(quantity);
            if (micros.rounder != null) {
                micros.rounder.apply(quantity);
            }
            if (micros.modMiddle != null) {
                return micros;
            }
            applyToMicros(micros, quantity);
            return micros;
        }

        public void applyToMicros(MicroProps micros, DecimalQuantity quantity) {
            if (rules == null) {
                micros.modMiddle = pm.getModifierWithoutPlural(quantity.signum());
            } else {
                StandardPlural pluralForm = RoundingUtils.getPluralSafe(micros.rounder, rules, quantity);
                micros.modMiddle = pm.getModifier(quantity.signum(), pluralForm);
            }
        }

        // NOTE: This method is not used in ICU4J right now.
        // In ICU4C, it is used by getPrefixSuffix().
        // Un-comment this method when getPrefixSuffix() is cleaned up in ICU4J.
        // public Modifier getModifier(byte signum, StandardPlural plural) {
        // if (rules == null) {
        // return pm.getModifier(signum);
        // } else {
        // return pm.getModifier(signum, plural);
        // }
        // }
    }

    /** Used by the unsafe code path. */
    public MicroPropsGenerator addToChain(MicroPropsGenerator parent) {
        this.parent = parent;
        return this;
    }

    @Override
    public MicroProps processQuantity(DecimalQuantity fq) {
        MicroProps micros = parent.processQuantity(fq);
        if (micros.rounder != null) {
            micros.rounder.apply(fq);
        }
        if (micros.modMiddle != null) {
            return micros;
        }
        if (needsPlurals()) {
            StandardPlural pluralForm = RoundingUtils.getPluralSafe(micros.rounder, rules, fq);
            setNumberProperties(fq.signum(), pluralForm);
        } else {
            setNumberProperties(fq.signum(), null);
        }
        micros.modMiddle = this;
        return micros;
    }

    @Override
    public int apply(FormattedStringBuilder output, int leftIndex, int rightIndex) {
        int prefixLen = insertPrefix(output, leftIndex);
        int suffixLen = insertSuffix(output, rightIndex + prefixLen);
        // If the pattern had no decimal stem body (like #,##0.00), overwrite the value.
        int overwriteLen = 0;
        if (!patternInfo.hasBody()) {
            overwriteLen = output.splice(leftIndex + prefixLen, rightIndex + prefixLen, "", 0, 0, null);
        }
        CurrencySpacingEnabledModifier.applyCurrencySpacing(output,
                leftIndex,
                prefixLen,
                rightIndex + prefixLen + overwriteLen,
                suffixLen,
                symbols);
        return prefixLen + overwriteLen + suffixLen;
    }

    @Override
    public int getPrefixLength() {
        // Render the affix to get the length
        prepareAffix(true);
        int result = AffixUtils.unescapedCount(currentAffix, true, this); // prefix length
        return result;
    }

    @Override
    public int getCodePointCount() {
        // Render the affixes to get the length
        prepareAffix(true);
        int result = AffixUtils.unescapedCount(currentAffix, false, this); // prefix length
        prepareAffix(false);
        result += AffixUtils.unescapedCount(currentAffix, false, this); // suffix length
        return result;
    }

    @Override
    public boolean isStrong() {
        return isStrong;
    }

    @Override
    public boolean containsField(java.text.Format.Field field) {
        // This method is not currently used. (unsafe path not used in range formatting)
        assert false;
        return false;
    }

    @Override
    public Parameters getParameters() {
        // This method is not currently used.
        assert false;
        return null;
    }

    @Override
    public boolean semanticallyEquivalent(Modifier other) {
        // This method is not currently used. (unsafe path not used in range formatting)
        assert false;
        return false;
    }

    private int insertPrefix(FormattedStringBuilder sb, int position) {
        prepareAffix(true);
        int length = AffixUtils.unescape(currentAffix, sb, position, this, field);
        return length;
    }

    private int insertSuffix(FormattedStringBuilder sb, int position) {
        prepareAffix(false);
        int length = AffixUtils.unescape(currentAffix, sb, position, this, field);
        return length;
    }

    /**
     * Pre-processes the prefix or suffix into the currentAffix field, creating and mutating that field
     * if necessary. Calls down to {@link PatternStringUtils#affixPatternProviderToStringBuilder}.
     *
     * @param isPrefix
     *            true to prepare the prefix; false to prepare the suffix.
     */
    private void prepareAffix(boolean isPrefix) {
        if (currentAffix == null) {
            currentAffix = new StringBuilder();
        }
        PatternStringUtils.patternInfoToStringBuilder(patternInfo,
                isPrefix,
                PatternStringUtils.resolveSignDisplay(signDisplay, signum),
                plural,
                perMilleReplacesPercent,
                currentAffix);
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
            } else {
                int selector;
                switch (unitWidth) {
                    case SHORT:
                        selector = Currency.SYMBOL_NAME;
                        break;
                    case NARROW:
                        selector = Currency.NARROW_SYMBOL_NAME;
                        break;
                    case FORMAL:
                        selector = Currency.FORMAL_SYMBOL_NAME;
                        break;
                    case VARIANT:
                        selector = Currency.VARIANT_SYMBOL_NAME;
                        break;
                    default:
                        throw new AssertionError();
                }
                return currency.getName(symbols.getULocale(), selector, null);
            }
        case AffixUtils.TYPE_CURRENCY_DOUBLE:
            return currency.getCurrencyCode();
        case AffixUtils.TYPE_CURRENCY_TRIPLE:
            // NOTE: This is the code path only for patterns containing "¤¤¤".
            // Plural currencies set via the API are formatted in LongNameHandler.
            // This code path is used by DecimalFormat via CurrencyPluralInfo.
            assert plural != null;
            return currency
                    .getName(symbols.getULocale(), Currency.PLURAL_LONG_NAME, plural.getKeyword(), null);
        case AffixUtils.TYPE_CURRENCY_QUAD:
            return "\uFFFD";
        case AffixUtils.TYPE_CURRENCY_QUINT:
            return currency.getName(symbols.getULocale(), Currency.NARROW_SYMBOL_NAME, null);
        default:
            throw new AssertionError();
        }
    }
}
