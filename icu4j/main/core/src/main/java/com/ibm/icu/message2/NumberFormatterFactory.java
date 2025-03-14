// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.message2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import com.ibm.icu.math.BigDecimal;
import com.ibm.icu.message2.MFDataModel.CatchallKey;
import com.ibm.icu.number.FormattedNumber;
import com.ibm.icu.number.LocalizedNumberFormatter;
import com.ibm.icu.number.Notation;
import com.ibm.icu.number.NumberFormatter;
import com.ibm.icu.number.NumberFormatter.GroupingStrategy;
import com.ibm.icu.number.NumberFormatter.SignDisplay;
import com.ibm.icu.number.NumberFormatter.UnitWidth;
import com.ibm.icu.number.Precision;
import com.ibm.icu.number.UnlocalizedNumberFormatter;
import com.ibm.icu.text.FormattedValue;
import com.ibm.icu.text.NumberingSystem;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.text.PluralRules.PluralType;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.CurrencyAmount;

/**
 * Creates a {@link Formatter} doing numeric formatting, similar to <code>{exp, number}</code>
 * in {@link com.ibm.icu.text.MessageFormat}.
 */
class NumberFormatterFactory implements FormatterFactory, SelectorFactory {
    private final String kind;

    public NumberFormatterFactory(String kind) {
        switch (kind) {
            case "number": // $FALL-THROUGH$
            case "integer":
            case "currency":
            case "math":
                break;
            default:
                // Default to number
                kind = "number";
        }
        this.kind = kind;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Formatter createFormatter(Locale locale, Map<String, Object> fixedOptions) {
        return new NumberFormatterImpl(locale, fixedOptions, kind);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Selector createSelector(Locale locale, Map<String, Object> fixedOptions) {
        String type = OptUtils.getString(fixedOptions, "select", "");
        PluralType pluralType;
        switch (type) {
            case "ordinal":
                pluralType = PluralType.ORDINAL;
                break;
            case "cardinal": // $FALL-THROUGH$
            default:
                pluralType = PluralType.CARDINAL;
        }

        PluralRules rules = PluralRules.forLocale(locale, pluralType);
        return new PluralSelectorImpl(locale, rules, fixedOptions, kind);
    }

    static class NumberFormatterImpl implements Formatter {
        private final Locale locale;
        private final Map<String, Object> fixedOptions;
        private final LocalizedNumberFormatter icuFormatter;
        private final String kind;

        NumberFormatterImpl(Locale locale, Map<String, Object> fixedOptions, String kind) {
            this.locale = OptUtils.getBestLocale(fixedOptions, locale);
            this.fixedOptions = new HashMap<>(fixedOptions);
            String skeleton = OptUtils.getString(fixedOptions, "icu:skeleton");
            boolean fancy = skeleton != null;
            this.icuFormatter = formatterForOptions(this.locale, fixedOptions, kind);
            this.kind = kind;
        }

        LocalizedNumberFormatter getIcuFormatter() {
            return icuFormatter;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String formatToString(Object toFormat, Map<String, Object> variableOptions) {
            return format(toFormat, variableOptions).toString();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public FormattedPlaceholder format(Object toFormat, Map<String, Object> variableOptions) {
            boolean reportErrors = OptUtils.reportErrors(fixedOptions) || OptUtils.reportErrors(variableOptions);
            LocalizedNumberFormatter realFormatter;
            Map<String, Object> mergedOptions = new HashMap<>(fixedOptions);
            if (variableOptions.isEmpty()) {
                realFormatter = this.icuFormatter;
            } else {
                mergedOptions.putAll(variableOptions);
                // This is really wasteful, as we don't use the existing
                // formatter if even one option is variable.
                // We can optimize, but for now will have to do.
                realFormatter = formatterForOptions(locale, mergedOptions, kind);
            }

            Integer offset = OptUtils.getInteger(variableOptions, reportErrors, "icu:offset");
            if (offset == null && fixedOptions != null) {
                offset = OptUtils.getInteger(fixedOptions, reportErrors, "icu:offset");
            }
            if (offset == null) {
                offset = 0;
            }

            Double mathOperand = null;
            if ("math".equals(kind)) {
                ResolvedMathOptions resolvedMathOptions = ResolvedMathOptions.of(fixedOptions);
                mathOperand = resolvedMathOptions.operand;
            }

            if (kind.equals("currency")) {
                String currencyCode = getCurrency(mergedOptions);
                if (currencyCode == null && !(toFormat instanceof CurrencyAmount)) {
                    // Error, we need a currency code, either from the message,
                    // with the {... :currency currency=<iso_code>}, or from the thing to format
                    throw new IllegalArgumentException(
                            "bad-option: the `currency` must be an ISO 4217 code.");
                }
            }

            boolean isInt = kind.equals("integer");
            FormattedValue result = null;
            if (toFormat == null) {
                // This is also what MessageFormat does.
                throw new NullPointerException("Argument to format can't be null");
            } else if (toFormat instanceof Double) {
                if (isInt) toFormat = Math.floor((double) toFormat);
                double toFormatAdjusted =(double) toFormat - offset;
                if (mathOperand != null) {
                    toFormatAdjusted += mathOperand;
                }
                result = realFormatter.format(toFormatAdjusted);
            } else if (toFormat instanceof Long) {
                if (mathOperand != null) {
                    result = realFormatter.format((long) toFormat - offset + mathOperand);
                } else {
                    result = realFormatter.format((long) toFormat - offset);
                }
            } else if (toFormat instanceof Integer) {
                if (mathOperand != null) {
                    result = realFormatter.format((int) toFormat - offset + mathOperand);
                } else {
                    result = realFormatter.format((int) toFormat - offset);
                }
            } else if (toFormat instanceof BigDecimal) {
                BigDecimal bd = (BigDecimal) toFormat;
                if (isInt) toFormat = bd.longValue();
                bd = bd.subtract(BigDecimal.valueOf(offset));
                if (mathOperand != null) {
                    bd = bd.add(BigDecimal.valueOf(mathOperand));
                }
                result = realFormatter.format(bd);
            } else if (toFormat instanceof Number) {
                if (isInt) toFormat = Math.floor(((Number) toFormat).doubleValue());
                double toFormatAdjusted = ((Number) toFormat).doubleValue() - offset;
                if (mathOperand != null) {
                    toFormatAdjusted += mathOperand;
                }
                result = realFormatter.format(toFormatAdjusted);
            } else if (toFormat instanceof CurrencyAmount) {
                result = realFormatter.format((CurrencyAmount) toFormat);
            } else {
                // The behavior is not in the spec, will be in the registry.
                // We can return "NaN", or try to parse the string as a number
                String strValue = Objects.toString(toFormat);
                Number nrValue = OptUtils.asNumber(reportErrors, "argument", strValue);
                if (nrValue != null) {
                    if (isInt) toFormat = Math.floor(nrValue.doubleValue());
                    double toFormatAdjusted = nrValue.doubleValue() - offset;
                    if (mathOperand != null) {
                        toFormatAdjusted += mathOperand;
                    }
                    result = realFormatter.format(toFormatAdjusted);
                } else {
                    result = new PlainStringFormattedValue("{|" + strValue + "|}");
                }
            }
            Directionality dir = OptUtils.getBestDirectionality(variableOptions, locale);
            return new FormattedPlaceholder(toFormat, result, dir, false);
        }
    }

    private static class PluralSelectorImpl implements Selector {
        private static final String NO_MATCH = "\uFFFDNO_MATCH\uFFFE"; // Unlikely to show in a key
        private final PluralRules rules;
        private final Map<String, Object> fixedOptions;
        private final LocalizedNumberFormatter icuFormatter;
        private final String kind;

        private PluralSelectorImpl(
                Locale locale, PluralRules rules, Map<String, Object> fixedOptions, String kind) {
            this.rules = rules;
            this.fixedOptions = fixedOptions;
            this.icuFormatter = formatterForOptions(locale, fixedOptions, kind);
            this.kind = kind;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<String> matches(
                Object value, List<String> keys, Map<String, Object> variableOptions) {
            List<String> result = new ArrayList<>();
            if (value == null) {
                return result;
            }
            for (String key : keys) {
                if (matches(value, key, variableOptions)) {
                    result.add(key);
                } else {
                    result.add(NO_MATCH);
                }
            }

            result.sort(PluralSelectorImpl::pluralComparator);
            return result;
        }

        // The order is exact values, key, other
        // There is no need to be very strict, as these are keys that are already equal
        // So we will not get to compare "1" vs "2", or "one" vs "few".
        // TODO: This is quite ugly, change when time.
        private static int pluralComparator(String o1, String o2) {
            if (o1.equals(o2)) {
                return 0;
            }
            if (NO_MATCH.equals(o1)) {
                return 1;
            }
            if (NO_MATCH.equals(o2)) {
                return -1;
            }
            // * sorts last
            if (CatchallKey.isCatchAll(o1)) {
                return 1;
            }
            if (CatchallKey.isCatchAll(o2)) {
                return -1;
            }
            // Numbers sort first
            if (OptUtils.asNumber(o1) != null) {
                return -1;
            }
            if (OptUtils.asNumber(o2) != null) {
                return 1;
            }
            // At this point they are both strings
            // We should never get here, so the order does not really matter
            return o1.compareTo(o2);
        }

        private boolean matches(Object value, String key, Map<String, Object> variableOptions) {
            if (CatchallKey.isCatchAll(key)) {
                return true;
            }

            boolean reportErrors = OptUtils.reportErrors(fixedOptions);
            Integer offset = OptUtils.getInteger(variableOptions, reportErrors, "icu:offset");
            if (offset == null && fixedOptions != null) {
                offset = OptUtils.getInteger(fixedOptions, reportErrors, "icu:offset");
            }
            if (offset == null) {
                offset = 0;
            }

            Number valToCheck = Double.MIN_VALUE;
            if (value instanceof FormattedPlaceholder) {
                FormattedPlaceholder fph = (FormattedPlaceholder) value;
                value = fph.getInput();
            }

            if (value instanceof Number) {
                valToCheck = ((Number) value).doubleValue();
            } else if (value instanceof CharSequence) {
                return value.equals(key);
            } else {
                return false;
            }
            if ("integer".equals(kind)) {
                valToCheck = valToCheck.longValue();
            }

            Number keyNrVal = OptUtils.asNumber(key);
            if (keyNrVal != null && valToCheck.doubleValue() == keyNrVal.doubleValue()) {
                return true;
            }

            FormattedNumber formatted = icuFormatter.format(valToCheck.doubleValue() - offset);
            String match = rules.select(formatted);
            if (match.equals("other")) {
                match = CatchallKey.AS_KEY_STRING;
            }
            return match.equals(key);
        }
    }

    // Currency ISO code
    private final static Pattern CURRENCY_ISO_CODE =
            Pattern.compile("^[A-Z][A-Z][A-Z]$", Pattern.CASE_INSENSITIVE);

    private static LocalizedNumberFormatter formatterForOptions(
            Locale locale, Map<String, Object> fixedOptions, String kind) {
        boolean reportErrors = OptUtils.reportErrors(fixedOptions);

        UnlocalizedNumberFormatter nf;
        String skeleton = OptUtils.getString(fixedOptions, "icu:skeleton");
        if (skeleton != null) {
            return NumberFormatter.forSkeleton(skeleton).locale(locale);
        }

        Integer option;
        String strOption;
        nf = NumberFormatter.with();

        // These options don't apply to `:integer`
        if ("number".equals(kind)) {
            Notation notation;
            switch (OptUtils.getString(fixedOptions, "notation", "standard")) {
                case "scientific":
                    notation = Notation.scientific();
                    break;
                case "engineering":
                    notation = Notation.engineering();
                    break;
                case "compact":
                    {
                        switch (OptUtils.getString(fixedOptions, "compactDisplay", "short")) {
                            case "long":
                                notation = Notation.compactLong();
                                break;
                            case "short": // $FALL-THROUGH$
                            default:
                                notation = Notation.compactShort();
                        }
                    }
                    break;
                case "standard": // $FALL-THROUGH$
                default:
                    notation = Notation.simple();
            }
            nf = nf.notation(notation);

            strOption = OptUtils.getString(fixedOptions, "style", "decimal");

            option = OptUtils.getInteger(fixedOptions, reportErrors, "minimumFractionDigits");
            if (option != null) {
                nf = nf.precision(Precision.minFraction(option));
            }
            option = OptUtils.getInteger(fixedOptions, reportErrors, "maximumFractionDigits");
            if (option != null) {
                nf = nf.precision(Precision.maxFraction(option));
            }
            option = OptUtils.getInteger(fixedOptions, reportErrors, "minimumSignificantDigits");
            if (option != null) {
                nf = nf.precision(Precision.minSignificantDigits(option));
            }
        } // end of `:number` specific options

        strOption = OptUtils.getString(fixedOptions, "numberingSystem", "");
        if (!strOption.isEmpty()) {
            strOption = strOption.toLowerCase(Locale.US);
            // No good way to validate, there are too many.
            NumberingSystem ns = NumberingSystem.getInstanceByName(strOption);
            nf = nf.symbols(ns);
        }

        // The options below apply to both `:number` and `:integer`
        option = OptUtils.getInteger(fixedOptions, reportErrors, "minimumIntegerDigits");
        if (option != null) {
            // TODO! Ask Shane. nf.integerWidth(null) ?
        }
        option = OptUtils.getInteger(fixedOptions, reportErrors, "maximumSignificantDigits");
        if (option != null) {
            nf = nf.precision(Precision.maxSignificantDigits(option));
        }

        strOption = OptUtils.getString(fixedOptions, "signDisplay", "auto");
        SignDisplay signDisplay;
        switch (strOption) {
            case "always":
                signDisplay = SignDisplay.ALWAYS;
                break;
            case "exceptZero":
                signDisplay = SignDisplay.EXCEPT_ZERO;
                break;
            case "negative":
                signDisplay = SignDisplay.NEGATIVE;
                break;
            case "never":
                signDisplay = SignDisplay.NEVER;
                break;
            case "auto": // $FALL-THROUGH$
            default:
                signDisplay = SignDisplay.AUTO;
        }
        nf = nf.sign(signDisplay);

        GroupingStrategy grp;
        strOption = OptUtils.getString(fixedOptions, "useGrouping", "auto");
        switch (strOption) {
            case "always":
                grp = GroupingStrategy.ON_ALIGNED;
                break; // TODO: check with Shane
            case "never":
                grp = GroupingStrategy.OFF;
                break;
            case "min2":
                grp = GroupingStrategy.MIN2;
                break;
            case "auto": // $FALL-THROUGH$
            default:
                grp = GroupingStrategy.AUTO;
        }
        nf = nf.grouping(grp);

        if (kind.equals("integer")) {
            nf = nf.precision(Precision.integer());
        }
        if (kind.equals("currency")) {
            strOption = getCurrency(fixedOptions);
            if (strOption != null) {
                nf = nf.unit(Currency.getInstance(strOption));
            }
            strOption = OptUtils.getString(fixedOptions, "currencySign", "standard");
            switch (strOption) {
                case "accounting":
                case "standard":
                    break;
            }
            strOption = OptUtils.getString(fixedOptions, "currencyDisplay", "symbol");
            UnitWidth width;
            switch (strOption) {
                case "narrowSymbol":
                    width = UnitWidth.NARROW;
                    break;
                case "symbol":
                    width = UnitWidth.SHORT;
                    break;
                case "name":
                    width = UnitWidth.FULL_NAME;
                    break;
                case "code":
                    width = UnitWidth.ISO_CODE;
                    break;
                case "formalSymbol":
                    width = UnitWidth.FORMAL;
                    break;
                case "never":
                    width = UnitWidth.HIDDEN;
                    break;
                default:
                    width = UnitWidth.SHORT;
            }
            nf = nf.unitWidth(width);
        }

        return nf.locale(locale);
    }

    static String getCurrency(Map<String, Object> options) {
        String value = OptUtils.getString(options, "currency", null);
        if (value != null) {
            if (CURRENCY_ISO_CODE.matcher(value).find()) {
                return value;
            } else {
                if (OptUtils.reportErrors(options)) {
                    throw new IllegalArgumentException(
                            "bad-option: the `currency` must be an ISO 4217 code.");
                }
            }
        }
        return null;
    }

    private static class ResolvedMathOptions {
        final Double operand;
        final boolean reportErrors;

        ResolvedMathOptions(Double operand, boolean reportErrors) {
            this.operand = operand;
            this.reportErrors = reportErrors;
        }

        static ResolvedMathOptions of(Map<String, Object> options) {
            boolean reportErrors = OptUtils.reportErrors(options);

            Double operand = null;
            String addOption = OptUtils.getString(options, "add", null);
            String subtractOption = OptUtils.getString(options, "subtract");

            if (addOption == null) {
                if (subtractOption == null) { // both null
                    throw new IllegalArgumentException(
                            "bad-option: :math function needs an `add` or `subtract` option.");
                } else {
                    operand = -OptUtils.asNumber(reportErrors, "subtract", subtractOption).doubleValue();
                }
            } else {
                if (subtractOption == null) {
                    operand = OptUtils.asNumber(reportErrors, "add", addOption).doubleValue();
                } else { // both set
                    throw new IllegalArgumentException(
                            "bad-option: :math function can't have both `add` and `subtract` options.");
                }
            }

            return new ResolvedMathOptions(operand, reportErrors);
        }
    }

}
