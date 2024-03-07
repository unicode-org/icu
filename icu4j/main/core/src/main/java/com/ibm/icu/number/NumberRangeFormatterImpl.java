// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.number;

import java.util.MissingResourceException;

import com.ibm.icu.impl.FormattedStringBuilder;
import com.ibm.icu.impl.FormattedValueStringBuilderImpl;
import com.ibm.icu.impl.ICUData;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.PatternProps;
import com.ibm.icu.impl.SimpleFormatterImpl;
import com.ibm.icu.impl.StandardPlural;
import com.ibm.icu.impl.UResource;
import com.ibm.icu.impl.number.DecimalQuantity;
import com.ibm.icu.impl.number.MacroProps;
import com.ibm.icu.impl.number.MicroProps;
import com.ibm.icu.impl.number.Modifier;
import com.ibm.icu.impl.number.SimpleModifier;
import com.ibm.icu.impl.number.Modifier.Parameters;
import com.ibm.icu.impl.number.Modifier.Signum;
import com.ibm.icu.impl.number.range.PrefixInfixSuffixLengthHelper;
import com.ibm.icu.impl.number.range.RangeMacroProps;
import com.ibm.icu.impl.number.range.StandardPluralRanges;
import com.ibm.icu.number.NumberRangeFormatter.RangeCollapse;
import com.ibm.icu.number.NumberRangeFormatter.RangeIdentityFallback;
import com.ibm.icu.number.NumberRangeFormatter.RangeIdentityResult;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;

/**
 * Business logic behind NumberRangeFormatter.
 */
class NumberRangeFormatterImpl {

    final NumberFormatterImpl formatterImpl1;
    final NumberFormatterImpl formatterImpl2;
    final boolean fSameFormatters;

    final NumberRangeFormatter.RangeCollapse fCollapse;
    final NumberRangeFormatter.RangeIdentityFallback fIdentityFallback;

    // Should be final, but it is set in a helper function, not the constructor proper.
    // TODO: Clean up to make this field actually final.
    /* final */ String fRangePattern;
    final NumberFormatterImpl fApproximatelyFormatter;

    final StandardPluralRanges fPluralRanges;

    ////////////////////

     // Helper function for 2-dimensional switch statement
     int identity2d(RangeIdentityFallback a, RangeIdentityResult b) {
         return a.ordinal() | (b.ordinal() << 4);
     }

    private static final class NumberRangeDataSink extends UResource.Sink {

        String rangePattern;
        // Note: approximatelyPattern is unused since ICU 69.
        // String approximatelyPattern;

        // For use with SimpleFormatterImpl
        StringBuilder sb;

        NumberRangeDataSink(StringBuilder sb) {
            this.sb = sb;
        }

        @Override
        public void put(UResource.Key key, UResource.Value value, boolean noFallback) {
            UResource.Table miscTable = value.getTable();
            for (int i = 0; miscTable.getKeyAndValue(i, key, value); ++i) {
                if (key.contentEquals("range") && !hasRangeData()) {
                    String pattern = value.getString();
                    rangePattern = SimpleFormatterImpl.compileToStringMinMaxArguments(pattern, sb, 2, 2);
                }
                /*
                // Note: approximatelyPattern is unused since ICU 69.
                if (key.contentEquals("approximately") && !hasApproxData()) {
                    String pattern = value.getString();
                    approximatelyPattern = SimpleFormatterImpl.compileToStringMinMaxArguments(pattern, sb, 1, 1); // 1 arg, as in "~{0}"
                }
                */
            }
        }

        private boolean hasRangeData() {
            return rangePattern != null;
        }

        /*
        // Note: approximatelyPattern is unused since ICU 69.
        private boolean hasApproxData() {
            return approximatelyPattern != null;
        }
        */

        public boolean isComplete() {
            return hasRangeData() /* && hasApproxData() */;
        }

        public void fillInDefaults() {
            if (!hasRangeData()) {
                rangePattern = SimpleFormatterImpl.compileToStringMinMaxArguments("{0}–{1}", sb, 2, 2);
            }
            /*
            if (!hasApproxData()) {
                approximatelyPattern = SimpleFormatterImpl.compileToStringMinMaxArguments("~{0}", sb, 1, 1);
            }
            */
        }
    }

    private static void getNumberRangeData(
            ULocale locale,
            String nsName,
            NumberRangeFormatterImpl out) {
        StringBuilder sb = new StringBuilder();
        NumberRangeDataSink sink = new NumberRangeDataSink(sb);
        ICUResourceBundle resource;
        resource = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, locale);
        sb.append("NumberElements/");
        sb.append(nsName);
        sb.append("/miscPatterns");
        String key = sb.toString();
        try {
            resource.getAllItemsWithFallback(key, sink);
        } catch (MissingResourceException e) {
            // ignore; fall back to latn
        }

        // Fall back to latn if necessary
        if (!sink.isComplete()) {
            resource.getAllItemsWithFallback("NumberElements/latn/miscPatterns", sink);
        }

        sink.fillInDefaults();

        out.fRangePattern = sink.rangePattern;
        // out.fApproximatelyModifier = new SimpleModifier(sink.approximatelyPattern, null, false);
    }

    ////////////////////

    public NumberRangeFormatterImpl(RangeMacroProps macros) {
        LocalizedNumberFormatter formatter1 = macros.formatter1 != null
            ? macros.formatter1.locale(macros.loc)
            : NumberFormatter.withLocale(macros.loc);
        LocalizedNumberFormatter formatter2 = macros.formatter2 != null
            ? macros.formatter2.locale(macros.loc)
            : NumberFormatter.withLocale(macros.loc);
        formatterImpl1 = new NumberFormatterImpl(formatter1.resolve());
        formatterImpl2 = new NumberFormatterImpl(formatter2.resolve());
        fSameFormatters = macros.sameFormatters != 0;
        fCollapse = macros.collapse != null ? macros.collapse : NumberRangeFormatter.RangeCollapse.AUTO;
        fIdentityFallback = macros.identityFallback != null ? macros.identityFallback
                : NumberRangeFormatter.RangeIdentityFallback.APPROXIMATELY;

        String nsName = formatterImpl1.getRawMicroProps().nsName;
        if (nsName == null || (!fSameFormatters && !nsName.equals(formatterImpl2.getRawMicroProps().nsName))) {
            throw new IllegalArgumentException("Both formatters must have same numbering system");
        }
        getNumberRangeData(macros.loc, nsName, this);

        if (fSameFormatters && (
                fIdentityFallback == RangeIdentityFallback.APPROXIMATELY ||
                fIdentityFallback == RangeIdentityFallback.APPROXIMATELY_OR_SINGLE_VALUE)) {
            MacroProps approximatelyMacros = new MacroProps();
            approximatelyMacros.approximately = true;
            fApproximatelyFormatter = new NumberFormatterImpl(
                formatter1.macros(approximatelyMacros).resolve());
        } else {
            fApproximatelyFormatter = null;
        }

        // TODO: Get locale from PluralRules instead?
        fPluralRanges = StandardPluralRanges.forLocale(macros.loc);
    }

    public FormattedNumberRange format(DecimalQuantity quantity1, DecimalQuantity quantity2, boolean equalBeforeRounding) {
        FormattedStringBuilder string = new FormattedStringBuilder();
        MicroProps micros1 = formatterImpl1.preProcess(quantity1);
        MicroProps micros2;
        if (fSameFormatters) {
            micros2 = formatterImpl1.preProcess(quantity2);
        } else {
            micros2 = formatterImpl2.preProcess(quantity2);
        }

        // If any of the affixes are different, an identity is not possible
        // and we must use formatRange().
        // TODO: Write this as MicroProps operator==() ?
        // TODO: Avoid the redundancy of these equality operations with the
        // ones in formatRange?
        if (!micros1.modInner.semanticallyEquivalent(micros2.modInner)
                || !micros1.modMiddle.semanticallyEquivalent(micros2.modMiddle)
                || !micros1.modOuter.semanticallyEquivalent(micros2.modOuter)) {
            formatRange(quantity1, quantity2, string, micros1, micros2);
            return new FormattedNumberRange(string, quantity1, quantity2, RangeIdentityResult.NOT_EQUAL);
        }

        // Check for identity
        RangeIdentityResult identityResult;
        if (equalBeforeRounding) {
            identityResult = RangeIdentityResult.EQUAL_BEFORE_ROUNDING;
        } else if (quantity1.equals(quantity2)) {
            identityResult = RangeIdentityResult.EQUAL_AFTER_ROUNDING;
        } else {
            identityResult = RangeIdentityResult.NOT_EQUAL;
        }

        // Java does not let us use a constexpr like C++;
        // we need to expand identity2d calls.
        switch (identity2d(fIdentityFallback, identityResult)) {
        case (3 | (2 << 4)): // RANGE, NOT_EQUAL
        case (3 | (1 << 4)): // RANGE, EQUAL_AFTER_ROUNDING
        case (3 | (0 << 4)): // RANGE, EQUAL_BEFORE_ROUNDING
        case (2 | (2 << 4)): // APPROXIMATELY, NOT_EQUAL
        case (1 | (2 << 4)): // APPROXIMATE_OR_SINGLE_VALUE, NOT_EQUAL
        case (0 | (2 << 4)): // SINGLE_VALUE, NOT_EQUAL
            formatRange(quantity1, quantity2, string, micros1, micros2);
            break;

        case (2 | (1 << 4)): // APPROXIMATELY, EQUAL_AFTER_ROUNDING
        case (2 | (0 << 4)): // APPROXIMATELY, EQUAL_BEFORE_ROUNDING
        case (1 | (1 << 4)): // APPROXIMATE_OR_SINGLE_VALUE, EQUAL_AFTER_ROUNDING
            formatApproximately(quantity1, quantity2, string, micros1, micros2);
            break;

        case (1 | (0 << 4)): // APPROXIMATE_OR_SINGLE_VALUE, EQUAL_BEFORE_ROUNDING
        case (0 | (1 << 4)): // SINGLE_VALUE, EQUAL_AFTER_ROUNDING
        case (0 | (0 << 4)): // SINGLE_VALUE, EQUAL_BEFORE_ROUNDING
            formatSingleValue(quantity1, quantity2, string, micros1, micros2);
            break;

        default:
            assert false;
            break;
        }

        return new FormattedNumberRange(string, quantity1, quantity2, identityResult);
    }

    private void formatSingleValue(DecimalQuantity quantity1, DecimalQuantity quantity2, FormattedStringBuilder string,
            MicroProps micros1, MicroProps micros2) {
        if (fSameFormatters) {
            int length = NumberFormatterImpl.writeNumber(micros1, quantity1, string, 0);
            NumberFormatterImpl.writeAffixes(micros1, string, 0, length);
        } else {
            formatRange(quantity1, quantity2, string, micros1, micros2);
        }

    }

    private void formatApproximately(DecimalQuantity quantity1, DecimalQuantity quantity2, FormattedStringBuilder string,
            MicroProps micros1, MicroProps micros2) {
        if (fSameFormatters) {
            // Re-format using the approximately formatter:
            quantity1.resetExponent();
            MicroProps microsAppx = fApproximatelyFormatter.preProcess(quantity1);
            int length = NumberFormatterImpl.writeNumber(microsAppx, quantity1, string, 0);
            // HEURISTIC: Desired modifier order: inner, middle, approximately, outer.
            length += microsAppx.modInner.apply(string, 0, length);
            length += microsAppx.modMiddle.apply(string, 0, length);
            microsAppx.modOuter.apply(string, 0, length);
        } else {
            formatRange(quantity1, quantity2, string, micros1, micros2);
        }
    }

    private void formatRange(DecimalQuantity quantity1, DecimalQuantity quantity2, FormattedStringBuilder string,
            MicroProps micros1, MicroProps micros2) {
        // modInner is always notation (scientific); collapsable in ALL.
        // modOuter is always units; collapsable in ALL, AUTO, and UNIT.
        // modMiddle could be either; collapsable in ALL and sometimes AUTO and UNIT.
        // Never collapse an outer mod but not an inner mod.
        boolean collapseOuter, collapseMiddle, collapseInner;
        switch (fCollapse) {
            case ALL:
            case AUTO:
            case UNIT:
            {
                // OUTER MODIFIER
                collapseOuter = micros1.modOuter.semanticallyEquivalent(micros2.modOuter);

                if (!collapseOuter) {
                    // Never collapse inner mods if outer mods are not collapsable
                    collapseMiddle = false;
                    collapseInner = false;
                    break;
                }

                // MIDDLE MODIFIER
                collapseMiddle = micros1.modMiddle.semanticallyEquivalent(micros2.modMiddle);

                if (!collapseMiddle) {
                    // Never collapse inner mods if outer mods are not collapsable
                    collapseInner = false;
                    break;
                }

                // MIDDLE MODIFIER HEURISTICS
                // (could disable collapsing of the middle modifier)
                // The modifiers are equal by this point, so we can look at just one of them.
                Modifier mm = micros1.modMiddle;
                if (fCollapse == RangeCollapse.UNIT) {
                    // Only collapse if the modifier is a unit.
                    // TODO: Make a better way to check for a unit?
                    // TODO: Handle case where the modifier has both notation and unit (compact currency)?
                    if (!mm.containsField(NumberFormat.Field.CURRENCY) && !mm.containsField(NumberFormat.Field.PERCENT)) {
                        collapseMiddle = false;
                    }
                } else if (fCollapse == RangeCollapse.AUTO) {
                    // Heuristic as of ICU 63: collapse only if the modifier is more than one code point.
                    if (mm.getCodePointCount() <= 1) {
                        collapseMiddle = false;
                    }
                }

                if (!collapseMiddle || fCollapse != RangeCollapse.ALL) {
                    collapseInner = false;
                    break;
                }

                // INNER MODIFIER
                collapseInner = micros1.modInner.semanticallyEquivalent(micros2.modInner);

                // All done checking for collapsibility.
                break;
            }

            default:
                collapseOuter = false;
                collapseMiddle = false;
                collapseInner = false;
                break;
        }

        // Java doesn't have macros, constexprs, or stack objects.
        // Use a helper object instead.
        PrefixInfixSuffixLengthHelper h = new PrefixInfixSuffixLengthHelper();

        SimpleModifier.formatTwoArgPattern(fRangePattern, string, 0, h, null);
        assert h.lengthInfix > 0;

        // SPACING HEURISTIC
        // Add spacing unless all modifiers are collapsed.
        // TODO: add API to control this?
        // TODO: Use a data-driven heuristic like currency spacing?
        // TODO: Use Unicode [:whitespace:] instead of PatternProps whitespace? (consider speed implications)
        {
            boolean repeatInner = !collapseInner && micros1.modInner.getCodePointCount() > 0;
            boolean repeatMiddle = !collapseMiddle && micros1.modMiddle.getCodePointCount() > 0;
            boolean repeatOuter = !collapseOuter && micros1.modOuter.getCodePointCount() > 0;
            if (repeatInner || repeatMiddle || repeatOuter) {
                // Add spacing if there is not already spacing
                if (!PatternProps.isWhiteSpace(string.charAt(h.index1()))) {
                    h.lengthInfix += string.insertCodePoint(h.index1(), '\u0020', null);
                }
                if (!PatternProps.isWhiteSpace(string.charAt(h.index2() - 1))) {
                    h.lengthInfix += string.insertCodePoint(h.index2(), '\u0020', null);
                }
            }
        }

        h.length1 += NumberFormatterImpl.writeNumber(micros1, quantity1, string, h.index0());
        // ICU-21684: Write the second number to a temp string to avoid repeated insert operations
        FormattedStringBuilder tempString = new FormattedStringBuilder();
        NumberFormatterImpl.writeNumber(micros2, quantity2, tempString, 0);
        h.length2 += string.insert(h.index2(), tempString);

        // TODO: Support padding?

        if (collapseInner) {
            Modifier mod = resolveModifierPlurals(micros1.modInner, micros2.modInner);
            h.lengthSuffix += mod.apply(string, h.index0(), h.index4());
            h.lengthPrefix += mod.getPrefixLength();
            h.lengthSuffix -= mod.getPrefixLength();
        } else {
            h.length1 += micros1.modInner.apply(string, h.index0(), h.index1());
            h.length2 += micros2.modInner.apply(string, h.index2(), h.index4());
        }

        if (collapseMiddle) {
            Modifier mod = resolveModifierPlurals(micros1.modMiddle, micros2.modMiddle);
            h.lengthSuffix += mod.apply(string, h.index0(), h.index4());
            h.lengthPrefix += mod.getPrefixLength();
            h.lengthSuffix -= mod.getPrefixLength();
        } else {
            h.length1 += micros1.modMiddle.apply(string, h.index0(), h.index1());
            h.length2 += micros2.modMiddle.apply(string, h.index2(), h.index4());
        }

        if (collapseOuter) {
            Modifier mod = resolveModifierPlurals(micros1.modOuter, micros2.modOuter);
            h.lengthSuffix += mod.apply(string, h.index0(), h.index4());
            h.lengthPrefix += mod.getPrefixLength();
            h.lengthSuffix -= mod.getPrefixLength();
        } else {
            h.length1 += micros1.modOuter.apply(string, h.index0(), h.index1());
            h.length2 += micros2.modOuter.apply(string, h.index2(), h.index4());
        }

        // Now that all pieces are added, save the span info.
        FormattedValueStringBuilderImpl.applySpanRange(
            string,
            NumberRangeFormatter.SpanField.NUMBER_RANGE_SPAN,
            0,
            h.index0(),
            h.index1());
        FormattedValueStringBuilderImpl.applySpanRange(
            string,
            NumberRangeFormatter.SpanField.NUMBER_RANGE_SPAN,
            1,
            h.index2(),
            h.index3());
    }

    Modifier resolveModifierPlurals(Modifier first, Modifier second) {
        Modifier.Parameters firstParameters = first.getParameters();
        if (firstParameters == null) {
            // No plural form; return a fallback (e.g., the first)
            return first;
        }

        Modifier.Parameters secondParameters = second.getParameters();
        if (secondParameters == null) {
            // No plural form; return a fallback (e.g., the first)
            return first;
        }

        // Get the required plural form from data
        StandardPlural resultPlural = fPluralRanges.resolve(firstParameters.plural, secondParameters.plural);

        // Get and return the new Modifier
        Modifier mod = firstParameters.obj.getModifier(firstParameters.signum, resultPlural);
        assert mod != null;
        return mod;
    }
}
