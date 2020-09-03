// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html


package com.ibm.icu.impl.number;

import com.ibm.icu.impl.FormattedStringBuilder;
import com.ibm.icu.impl.StandardPlural;
import com.ibm.icu.number.LocalizedNumberFormatter;
import com.ibm.icu.number.NumberFormatter;
import com.ibm.icu.text.ListFormatter;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.text.SimpleFormatter;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.ULocale;

import java.util.ArrayList;
import java.util.List;

public class MixedUnitLongNameHandler implements MicroPropsGenerator, ModifierStore {
    // Not owned
    private final PluralRules rules;
    // Not owned
    private final MicroPropsGenerator parent;

    // If this LongNameHandler is for a mixed unit, this stores unit data for
    // each of the individual units. For each unit, it stores ARRAY_LENGTH
    // strings, as returned by getMeasureData.
    private List<String[]> fMixedUnitData;

    // A localized NumberFormatter used to format the integer-valued bigger
    // units of Mixed Unit measurements.
    private LocalizedNumberFormatter fIntegerFormatter;

    // A localised list formatter for joining mixed units together.
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
     * @param rules     Does not take ownership.
     * @param parent    Does not take ownership.
     */
    public static MixedUnitLongNameHandler forMeasureUnit(ULocale locale, MeasureUnit mixedUnit,
                                                          NumberFormatter.UnitWidth width, PluralRules rules,
                                                          MicroPropsGenerator parent) {
        assert (mixedUnit.getComplexity() == MeasureUnit.Complexity.MIXED);

        MixedUnitLongNameHandler result = new MixedUnitLongNameHandler(rules, parent);
        List<MeasureUnit> individualUnits = mixedUnit.splitToSingleUnits();

        result.fMixedUnitData = new ArrayList<>();
        for (int i = 0; i < individualUnits.size(); i++) {
            // Grab data for each of the components.
            String[] unitData = new String[LongNameHandler.ARRAY_LENGTH];
            LongNameHandler.getMeasureData(locale, individualUnits.get(i), width, unitData);
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
     * provided via `micros.mixedMeasures`.
     */
    @Override
    public MicroProps processQuantity(DecimalQuantity quantity) {
        assert (fMixedUnitData.size() > 1);
        MicroProps micros;
        // if (parent != null)
        micros = parent.processQuantity(quantity);
        micros.modOuter = getMixedUnitModifier(quantity, micros);
        return micros;
    }

    // Required for ModifierStore. And ModifierStore is required by
    // SimpleModifier constructor's last parameter. We assert his will never get
    // called though.
    @Override
    public Modifier getModifier(Modifier.Signum signum, StandardPlural plural) {
        // TODO(units): investigate this method while investigating where
        // LongNameHandler.getModifier() gets used. To be sure it remains
        // unreachable:

        return null;
    }

    // For a mixed unit, returns a Modifier that takes only one parameter: the
    // smallest and final unit of the set. The bigger units' values and labels
    // get baked into this Modifier, together with the unit label of the final
    // unit.
    private Modifier getMixedUnitModifier(DecimalQuantity quantity, MicroProps micros) {
        // TODO(icu-units#21): mixed units without usage() is not yet supported.
        // That should be the only reason why this happens, so delete this whole if
        // once fixed:
        if (micros.mixedMeasures.size() == 0) {
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

        for (int i = 0; i < micros.mixedMeasures.size(); i++) {
            DecimalQuantity fdec = new DecimalQuantity_DualStorageBCD(micros.mixedMeasures.get(i).getNumber());
            StandardPlural pluralForm = fdec.getStandardPlural(rules);

            String simpleFormat = LongNameHandler.getWithPlural(this.fMixedUnitData.get(i), pluralForm);
            SimpleFormatter compiledFormatter = SimpleFormatter.compileMinMaxArguments(simpleFormat, 0, 1);


            FormattedStringBuilder appendable = new FormattedStringBuilder();
            this.fIntegerFormatter.formatImpl(fdec, appendable);
            outputMeasuresList.add(compiledFormatter.format(appendable.toString()));
            // TODO: fix this issue https://github.com/icu-units/icu/issues/67
        }

        String[] finalSimpleFormats = this.fMixedUnitData.get(this.fMixedUnitData.size() - 1);
        StandardPlural finalPlural = RoundingUtils.getPluralSafe(micros.rounder, rules, quantity);
        String finalSimpleFormat = LongNameHandler.getWithPlural(finalSimpleFormats, finalPlural);
        SimpleFormatter finalFormatter = SimpleFormatter.compileMinMaxArguments(finalSimpleFormat, 0, 1);
        finalFormatter.format("{0}", outputMeasuresList.get(outputMeasuresList.size() -1));

        // Combine list into a "premixed" pattern
        String premixedFormatPattern = this.fListFormatter.format(outputMeasuresList);
        SimpleFormatter premixedCompiled = SimpleFormatter.compileMinMaxArguments(premixedFormatPattern, 0, 1);

        // Return a SimpleModifier for the "premixed" pattern
        Modifier.Parameters params = new Modifier.Parameters();
        params.obj = this;
        params.signum = Modifier.Signum.POS_ZERO;
        params.plural = finalPlural;

        return new SimpleModifier(premixedCompiled.getTextWithNoArguments(), null, false, params);
        /*TODO: it was SimpleModifier(premixedCompiled, kUndefinedField, false, {this, SIGNUM_POS_ZERO, finalPlural});*/
    }
}
