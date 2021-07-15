// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.number;

import java.util.ArrayList;
import java.util.List;

import com.ibm.icu.impl.FormattedStringBuilder;
import com.ibm.icu.impl.SimpleFormatterImpl;
import com.ibm.icu.impl.StandardPlural;
import com.ibm.icu.number.LocalizedNumberFormatter;
import com.ibm.icu.number.NumberFormatter;
import com.ibm.icu.text.ListFormatter;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.text.SimpleFormatter;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.ULocale;

/** Similar to LongNameHandler, but only for MIXED units. */
public class MixedUnitLongNameHandler
    implements MicroPropsGenerator, ModifierStore, LongNameMultiplexer.ParentlessMicroPropsGenerator {
    private final PluralRules rules;
    private final MicroPropsGenerator parent;

    /**
     * Stores unit data for each of the individual units. For each unit, it
     * stores ARRAY_LENGTH strings, as returned by getMeasureData.
     */
    private List<String[]> fMixedUnitData;

    /**
     * A localized NumberFormatter used to format the integer-valued bigger
     * units of Mixed Unit measurements.
     */
    private LocalizedNumberFormatter fIntegerFormatter;

    /** A localised list formatter for joining mixed units together. */
    private ListFormatter fListFormatter;

    private MixedUnitLongNameHandler(PluralRules rules, MicroPropsGenerator parent) {
        this.rules = rules;
        this.parent = parent;
    }

    /**
     * Construct a localized MixedUnitLongNameHandler for the specified
     * MeasureUnit. It must be a MIXED unit.
     * <p>
     *
     * @param locale    The desired locale.
     * @param mixedUnit The mixed measure unit to construct a
     *                  MixedUnitLongNameHandler for.
     * @param width     Specifies the desired unit rendering.
     * @param unitDisplayCase Specifies the desired grammatical case. If the
     *     specified case is not found, we fall back to nominative or no-case.
     * @param rules     PluralRules instance.
     * @param parent    MicroPropsGenerator instance.
     */
    public static MixedUnitLongNameHandler forMeasureUnit(ULocale locale,
                                                          MeasureUnit mixedUnit,
                                                          NumberFormatter.UnitWidth width,
                                                          String unitDisplayCase,
                                                          PluralRules rules,
                                                          MicroPropsGenerator parent) {
        assert mixedUnit.getComplexity() == MeasureUnit.Complexity.MIXED
            : "MixedUnitLongNameHandler only supports MIXED units";
        // In ICU4C, in addition to an assert, we return a failure status if the
        // unit is not mixed (commented by: "Defensive, for production code").
        // In Java, we don't have efficient access to MeasureUnitImpl, so we
        // skip this check - relying on unit tests and the assert above to help
        // enforce the invariant.

        MixedUnitLongNameHandler result = new MixedUnitLongNameHandler(rules, parent);
        List<MeasureUnit> individualUnits = mixedUnit.splitToSingleUnits();

        result.fMixedUnitData = new ArrayList<>();
        for (int i = 0; i < individualUnits.size(); i++) {
            // Grab data for each of the components.
            String[] unitData = new String[LongNameHandler.ARRAY_LENGTH];
            LongNameHandler.getMeasureData(locale, individualUnits.get(i), width, unitDisplayCase,
                                           unitData);
            // TODO(ICU-21494): if we add support for gender for mixed units, we may
            // need LongNameHandler.maybeCalculateGender() here.
            result.fMixedUnitData.add(unitData);
        }

        ListFormatter.Width listWidth = ListFormatter.Width.SHORT;
        if (width == NumberFormatter.UnitWidth.NARROW) {
            listWidth = ListFormatter.Width.NARROW;
        } else if (width == NumberFormatter.UnitWidth.FULL_NAME) {
            // This might be the same as SHORT in most languages:
            listWidth = ListFormatter.Width.WIDE;
        }

        result.fListFormatter = ListFormatter.getInstance(locale, ListFormatter.Type.UNITS, listWidth);


        // We need a localised NumberFormatter for the integers of the bigger units
        // (providing Arabic numerals, for example).
        result.fIntegerFormatter = NumberFormatter.withLocale(locale);

        return result;
    }

    /**
     * Produces a plural-appropriate Modifier for a mixed unit: `quantity` is
     * taken as the final smallest unit, while the larger unit values must be
     * provided by `micros.mixedMeasures`, micros being the MicroProps instance
     * returned by the parent.
     *
     * This function must not be called if this instance has no parent: call
     * processQuantityWithMicros() instead.
     */
    @Override
    public MicroProps processQuantity(DecimalQuantity quantity) {
        assert (fMixedUnitData.size() > 1);
        MicroProps micros;
        micros = parent.processQuantity(quantity);
        micros.modOuter = getMixedUnitModifier(quantity, micros);
        return micros;
    }

    /**
     * Produces a plural-appropriate Modifier for a mixed unit: `quantity` is
     * taken as the final smallest unit, while the larger unit values must be
     * provided via `micros.mixedMeasures`.
     *
     * Does not call parent.processQuantity, so cannot get a MicroProps instance
     * that way. Instead, the instance is passed in as a parameter.
     */
    public MicroProps processQuantityWithMicros(DecimalQuantity quantity, MicroProps micros) {
        assert (fMixedUnitData.size() > 1);
        micros.modOuter = getMixedUnitModifier(quantity, micros);
        return micros;
    }

    /**
     * Required for ModifierStore. And ModifierStore is required by
     * SimpleModifier constructor's last parameter. We assert his will never get
     * called though.
     */
    @Override
    public Modifier getModifier(Modifier.Signum signum, StandardPlural plural) {
        // TODO(icu-units#28): investigate this method while investigating where
        // LongNameHandler.getModifier() gets used. To be sure it remains
        // unreachable:
        assert false : "should be unreachable";
        return null;
    }

    /**
     * For a mixed unit, returns a Modifier that takes only one parameter: the
     * smallest and final unit of the set. The bigger units' values and labels
     * get baked into this Modifier, together with the unit label of the final
     * unit.
     */
    private Modifier getMixedUnitModifier(DecimalQuantity quantity, MicroProps micros) {
        // If we don't have at least one mixedMeasure, the LongNameHandler would be
        // sufficient and we shouldn't be running MixedUnitLongNameHandler code:
        if (micros.mixedMeasures.size() == 0) {
            assert false : "Mixed unit: we must have more than one unit value";
            throw new UnsupportedOperationException();
        }

        // Algorithm:
        //
        // For the mixed-units measurement of: "3 yard, 1 foot, 2.6 inch", we should
        // find "3 yard" and "1 foot" in micros.mixedMeasures.
        //
        // Obtain long-names with plural forms corresponding to measure values:
        //   * {0} yards, {0} foot, {0} inches
        //
        // Format the integer values appropriately and modify with the format
        // strings:
        //   - 3 yards, 1 foot
        //
        // Use ListFormatter to combine, with one placeholder:
        //   - 3 yards, 1 foot and {0} inches /* TODO: how about the case of `1 inch` */
        //
        // Return a SimpleModifier for this pattern, letting the rest of the
        // pipeline take care of the remaining inches.

        List<String> outputMeasuresList = new ArrayList<>();

        StandardPlural quantityPlural = StandardPlural.OTHER;
        for (int i = 0; i < micros.mixedMeasures.size(); i++) {

            if ( i  == micros.indexOfQuantity) {
                if (i > 0 && quantity.isNegative()) {
                    // If numbers are negative, only the first number needs to have its
                    // negative sign formatted.
                    quantity.negate();
                }

                quantityPlural = RoundingUtils.getPluralSafe(micros.rounder, rules, quantity);
                String quantitySimpleFormat = LongNameHandler.getWithPlural(this.fMixedUnitData.get(i), quantityPlural);
                SimpleFormatter finalFormatter = SimpleFormatter.compileMinMaxArguments(quantitySimpleFormat, 0, 1);
                outputMeasuresList.add(finalFormatter.format("{0}"));

                continue;
            }


            DecimalQuantity fdec = new DecimalQuantity_DualStorageBCD(micros.mixedMeasures.get(i).getNumber());
            if (i > 0 && fdec.isNegative()) {
                // If numbers are negative, only the first number needs to have its
                // negative sign formatted.
                fdec.negate();
            }

            StandardPlural pluralForm = RoundingUtils.getPluralSafe(micros.rounder, rules, fdec);

            String simpleFormat = LongNameHandler.getWithPlural(this.fMixedUnitData.get(i), pluralForm);
            SimpleFormatter compiledFormatter = SimpleFormatter.compileMinMaxArguments(simpleFormat, 0, 1);

            FormattedStringBuilder appendable = new FormattedStringBuilder();
            this.fIntegerFormatter.formatImpl(fdec, appendable);
            outputMeasuresList.add(compiledFormatter.format(appendable.toString()));
            // TODO(icu-units#67): fix field positions
        }


        // Combine list into a "premixed" pattern
        String premixedFormatPattern = this.fListFormatter.format(outputMeasuresList);
        StringBuilder sb = new StringBuilder();
        String premixedCompiled =
            SimpleFormatterImpl.compileToStringMinMaxArguments(premixedFormatPattern, sb, 0, 1);

        // TODO(icu-units#67): fix field positions
        Modifier.Parameters params = new Modifier.Parameters();
        params.obj = this;
        params.signum = Modifier.Signum.POS_ZERO;
        params.plural = quantityPlural;
        // Return a SimpleModifier for the "premixed" pattern
        return new SimpleModifier(premixedCompiled, null, false, params);
    }
}
