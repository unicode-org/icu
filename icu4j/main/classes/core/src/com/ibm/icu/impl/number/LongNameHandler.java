// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.number;

import java.util.EnumMap;
import java.util.Map;
import java.util.MissingResourceException;

import com.ibm.icu.impl.CurrencyData;
import com.ibm.icu.impl.ICUData;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.SimpleFormatterImpl;
import com.ibm.icu.impl.StandardPlural;
import com.ibm.icu.impl.UResource;
import com.ibm.icu.impl.number.Modifier.Signum;
import com.ibm.icu.impl.units.MeasureUnitImpl;
import com.ibm.icu.impl.units.SingleUnitImpl;
import com.ibm.icu.number.NumberFormatter.UnitWidth;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.ICUException;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;

/**
 * Takes care of formatting currency and measurement unit names, as well as populating the gender of measure units.
 */
public class LongNameHandler
    implements MicroPropsGenerator, ModifierStore, LongNameMultiplexer.ParentlessMicroPropsGenerator {

    private static int i = 0;
    private static final int DNAM_INDEX = StandardPlural.COUNT + i++;
    private static final int PER_INDEX = StandardPlural.COUNT + i++;
    private static final int GENDER_INDEX = StandardPlural.COUNT + i++;
    static final int ARRAY_LENGTH = StandardPlural.COUNT + i++;

    private static int getIndex(String pluralKeyword) {
        // pluralKeyword can also be "dnam", "per" or "gender"
        if (pluralKeyword.equals("dnam")) {
            return DNAM_INDEX;
        } else if (pluralKeyword.equals("per")) {
            return PER_INDEX;
        } else if (pluralKeyword.equals("gender")) {
            return GENDER_INDEX;
        } else {
            return StandardPlural.fromString(pluralKeyword).ordinal();
        }
    }

    static String getWithPlural(String[] strings, StandardPlural plural) {
        String result = strings[plural.ordinal()];
        if (result == null) {
            result = strings[StandardPlural.OTHER.ordinal()];
        }
        if (result == null) {
            // There should always be data in the "other" plural variant.
            throw new ICUException("Could not find data in 'other' plural variant");
        }
        return result;
    }

    //////////////////////////
    /// BEGIN DATA LOADING ///
    //////////////////////////

    private static final class PluralTableSink extends UResource.Sink {

        String[] outArray;

        public PluralTableSink(String[] outArray) {
            this.outArray = outArray;
        }

        @Override
        public void put(UResource.Key key, UResource.Value value, boolean noFallback) {
            UResource.Table pluralsTable = value.getTable();
            for (int i = 0; pluralsTable.getKeyAndValue(i, key, value); ++i) {
                String keyString = key.toString();

                if (keyString.equals("case")) {
                    continue;
                }

                int index = getIndex(keyString);
                if (outArray[index] != null) {
                    continue;
                }

                String formatString = value.getString();
                outArray[index] = formatString;
            }
        }
    }

    // NOTE: outArray MUST have at least ARRAY_LENGTH entries. No bounds checking is performed.
    static void getMeasureData(
            ULocale locale,
            MeasureUnit unit,
            UnitWidth width,
            String unitDisplayCase,
            String[] outArray) {
        PluralTableSink sink = new PluralTableSink(outArray);
        ICUResourceBundle resource;
        resource = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_UNIT_BASE_NAME,
                locale);
        StringBuilder key = new StringBuilder();
        key.append("units");
        // TODO(icu-units#140): support gender for other unit widths.
        if (width == UnitWidth.NARROW) {
            key.append("Narrow");
        } else if (width == UnitWidth.SHORT) {
            key.append("Short");
        }
        key.append("/");
        key.append(unit.getType());
        key.append("/");

        // Map duration-year-person, duration-week-person, etc. to duration-year, duration-week, ...
        // TODO(ICU-20400): Get duration-*-person data properly with aliases.
        if (unit.getSubtype() != null && unit.getSubtype().endsWith("-person")) {
            key.append(unit.getSubtype(), 0, unit.getSubtype().length() - 7);
        } else {
            key.append(unit.getSubtype());
        }

        // Grab desired case first, if available. Then grab nominative case to fill
        // in the gaps.
        //
        // TODO(icu-units#138): check that fallback is spec-compliant
        if (width == UnitWidth.FULL_NAME
                        && unitDisplayCase != null
                        && !unitDisplayCase.isEmpty()) {
            StringBuilder caseKey = new StringBuilder();
            caseKey.append(key);
            caseKey.append("/case/");
            caseKey.append(unitDisplayCase);

            try {
                resource.getAllItemsWithFallback(caseKey.toString(), sink);
                // TODO(icu-units#138): our fallback logic is not spec-compliant: we
                // check the given case, then go straight to the no-case data. The spec
                // states we should first look for case="nominative". As part of #138,
                // either get the spec changed, or add unit tests that warn us if
                // case="nominative" data differs from no-case data?
            } catch (MissingResourceException e) {
                // continue.
            }
        }
        try {
            resource.getAllItemsWithFallback(key.toString(), sink);
        } catch (MissingResourceException e) {
            throw new IllegalArgumentException("No data for unit " + unit + ", width " + width, e);
        }
    }

    private static void getCurrencyLongNameData(ULocale locale, Currency currency, String[] outArray) {
        // In ICU4J, this method gets a CurrencyData from CurrencyData.provider.
        // TODO(ICU4J): Implement this without going through CurrencyData, like in ICU4C?
        Map<String, String> data = CurrencyData.provider.getInstance(locale, true).getUnitPatterns();
        for (Map.Entry<String, String> e : data.entrySet()) {
            String pluralKeyword = e.getKey();
            int index = getIndex(pluralKeyword);
            String longName = currency.getName(locale, Currency.PLURAL_LONG_NAME, pluralKeyword, null);
            String simpleFormat = e.getValue();
            // Example pattern from data: "{0} {1}"
            // Example output after find-and-replace: "{0} US dollars"
            simpleFormat = simpleFormat.replace("{1}", longName);
            // String compiled = SimpleFormatterImpl.compileToStringMinMaxArguments(simpleFormat, sb, 1,
            // 1);
            // SimpleModifier mod = new SimpleModifier(compiled, Field.CURRENCY, false);
            outArray[index] = simpleFormat;
        }
    }

    private static String getPerUnitFormat(ULocale locale, UnitWidth width) {
        ICUResourceBundle resource;
        resource = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_UNIT_BASE_NAME,
                locale);
        StringBuilder key = new StringBuilder();
        key.append("units");
        if (width == UnitWidth.NARROW) {
            key.append("Narrow");
        } else if (width == UnitWidth.SHORT) {
            key.append("Short");
        }
        key.append("/compound/per");
        try {
            return resource.getStringWithFallback(key.toString());
        } catch (MissingResourceException e) {
            throw new IllegalArgumentException(
                    "Could not find x-per-y format for " + locale + ", width " + width);
        }
    }

    private static String getDeriveCompoundRule(ULocale locale, String feature, String structure) {
        ICUResourceBundle derivationsBundle =
                (ICUResourceBundle) UResourceBundle
                        .getBundleInstance(ICUData.ICU_BASE_NAME, "grammaticalFeatures");

        derivationsBundle = (ICUResourceBundle) derivationsBundle.get("grammaticalData");
        derivationsBundle = (ICUResourceBundle) derivationsBundle.get("derivations");

        ICUResourceBundle stackBundle;
        try {
            // TODO: use standard normal locale resolution algorithms rather than just grabbing language:
            stackBundle = (ICUResourceBundle) derivationsBundle.get(locale.getLanguage());
        } catch (MissingResourceException e) {
            stackBundle = (ICUResourceBundle) derivationsBundle.get("root");
        }

        stackBundle = (ICUResourceBundle) stackBundle.get("compound");
        stackBundle = (ICUResourceBundle) stackBundle.get(feature);

        return stackBundle.getString(structure);
    }

    ////////////////////////
    /// END DATA LOADING ///
    ////////////////////////

    private final Map<StandardPlural, SimpleModifier> modifiers;
    private final PluralRules rules;
    private final MicroPropsGenerator parent;
    // Grammatical gender of the formatted result.
    private String gender = "";

    private LongNameHandler(
            Map<StandardPlural, SimpleModifier> modifiers,
            PluralRules rules,
            MicroPropsGenerator parent) {
        this.modifiers = modifiers;
        this.rules = rules;
        this.parent = parent;
    }

    public static String getUnitDisplayName(ULocale locale, MeasureUnit unit, UnitWidth width) {
        String[] measureData = new String[ARRAY_LENGTH];
        getMeasureData(locale, unit, width, "", measureData);
        return measureData[DNAM_INDEX];
    }

    public static LongNameHandler forCurrencyLongNames(
            ULocale locale,
            Currency currency,
            PluralRules rules,
            MicroPropsGenerator parent) {
        String[] simpleFormats = new String[ARRAY_LENGTH];
        getCurrencyLongNameData(locale, currency, simpleFormats);
        // TODO(ICU4J): Reduce the number of object creations here?
        Map<StandardPlural, SimpleModifier> modifiers = new EnumMap<>(
                StandardPlural.class);
        LongNameHandler result = new LongNameHandler(modifiers, rules, parent);
        result.simpleFormatsToModifiers(simpleFormats, NumberFormat.Field.CURRENCY);
        return result;
    }

    /**
     * Construct a localized LongNameHandler for the specified MeasureUnit.
     * <p>
     * Mixed units are not supported, use MixedUnitLongNameHandler.forMeasureUnit.
     *
     * @param locale The desired locale.
     * @param unit The measure unit to construct a LongNameHandler for.
     * @param width Specifies the desired unit rendering.
     * @param unitDisplayCase
     * @param rules Plural rules.
     * @param parent Plural rules.
     */
    public static LongNameHandler forMeasureUnit(
            ULocale locale,
            MeasureUnit unit,
            UnitWidth width,
            String unitDisplayCase,
            PluralRules rules,
            MicroPropsGenerator parent) {
        if (unit.getType() == null) {
            // Not a built-in unit. Split it up, since we can already format
            // "builtin-per-builtin".
            // TODO(ICU-20941): support more generic case than builtin-per-builtin.
            MeasureUnitImpl fullUnit = unit.getCopyOfMeasureUnitImpl();
            unit = null;
            MeasureUnit perUnit = null;
            for (SingleUnitImpl subUnit : fullUnit.getSingleUnits()) {
                if (subUnit.getDimensionality() > 0) {
                    if (unit == null) {
                        unit = subUnit.build();
                    } else {
                        unit = unit.product(subUnit.build());
                    }
                } else {
                    // It's okay to mutate fullUnit, we made a temporary copy:
                    subUnit.setDimensionality(subUnit.getDimensionality() * -1);
                    if (perUnit == null) {
                        perUnit = subUnit.build();
                    } else {
                        perUnit = perUnit.product(subUnit.build());
                    }
                }
            }
            return forCompoundUnit(locale, unit, perUnit, width, unitDisplayCase, rules, parent);
        }

        String[] simpleFormats = new String[ARRAY_LENGTH];
        getMeasureData(locale, unit, width, unitDisplayCase, simpleFormats);
        // TODO(ICU4J): Reduce the number of object creations here?
        Map<StandardPlural, SimpleModifier> modifiers = new EnumMap<>(
                StandardPlural.class);
        LongNameHandler result = new LongNameHandler(modifiers, rules, parent);
        result.simpleFormatsToModifiers(simpleFormats, NumberFormat.Field.MEASURE_UNIT);
        if (simpleFormats[GENDER_INDEX] != null) {
            result.gender = simpleFormats[GENDER_INDEX];
        }

        return result;
    }

    /**
     * Loads and applies deriveComponent rules from CLDR's grammaticalFeatures.xml.
     * <pre>
     * Consider a deriveComponent rule that looks like this:
     * </pre>
     * <deriveComponent feature="case" structure="per" value0="compound" value1="nominative"/>
     * <p>
     * Instantiating an instance as follows:
     * <pre>
     * DerivedComponents d(loc, "case", "per", "foo");
     * </pre>
     * <p>
     * Applying the rule in the XML element above, <code>d.value0()</code> will be "foo", and
     * <code>d.value1()</code> will be "nominative".
     * <p>
     * <p>
     * In case of any kind of failure, value0() and value1() will simply return "".
     */
    private static class DerivedComponents {
        /**
         * Constructor.
         */
        public DerivedComponents(ULocale locale,
                                 String feature,
                                 String structure,
                                 String compoundValue) {
            ICUResourceBundle derivationsBundle =
                    (ICUResourceBundle) UResourceBundle
                            .getBundleInstance(ICUData.ICU_BASE_NAME, "grammaticalFeatures");
            derivationsBundle = (ICUResourceBundle) derivationsBundle.get("grammaticalData");
            derivationsBundle = (ICUResourceBundle) derivationsBundle.get("derivations");

            ICUResourceBundle stackBundle;
            try {
                // TODO: use standard normal locale resolution algorithms rather than just grabbing language:
                stackBundle = (ICUResourceBundle) derivationsBundle.get(locale.getLanguage());
            } catch (MissingResourceException e) {
                stackBundle = (ICUResourceBundle) derivationsBundle.get("root");
            }

            stackBundle = (ICUResourceBundle) stackBundle.get("component");
            stackBundle = (ICUResourceBundle) stackBundle.get(feature);
            stackBundle = (ICUResourceBundle) stackBundle.get(structure);

            String value = stackBundle.getString(0);
            if (value.compareTo("compound") == 0) {
                this.value0 = compoundValue;
            } else {
                this.value0 = value;
            }

            value = stackBundle.getString(1);
            if (value.compareTo("compound") == 0) {
                this.value1 = compoundValue;
            } else {
                this.value1 = value;
            }
        }

        public final String value0, value1;
    }

    private static LongNameHandler forCompoundUnit(
            ULocale locale,
            MeasureUnit unit,
            MeasureUnit perUnit,
            UnitWidth width,
            String unitDisplayCase,
            PluralRules rules,
            MicroPropsGenerator parent) {
        if (unit.getType() == null || perUnit.getType() == null) {
            // TODO(ICU-20941): Unsanctioned unit. Not yet fully supported. Set an
            // error code.
            throw new UnsupportedOperationException(
                "Unsanctioned units, not yet supported: " + unit.getIdentifier() + "/" +
                perUnit.getIdentifier());
        }

         DerivedComponents derivedPerCases = new DerivedComponents(locale, "case", "per", unitDisplayCase);


        String[] primaryData = new String[ARRAY_LENGTH];
        getMeasureData(locale, unit, width, derivedPerCases.value0, primaryData);
        String[] secondaryData = new String[ARRAY_LENGTH];
        getMeasureData(locale, perUnit, width, derivedPerCases.value1, secondaryData);

        // TODO(icu-units#139): implement these rules:
        //    - <deriveComponent feature="plural" structure="per" ...>
        //    - This has impact on multiSimpleFormatsToModifiers(...) below too.
        //
        // These rules are currently (ICU 69) all the same and hard-coded below.
        String perUnitFormat;
        if (secondaryData[PER_INDEX] != null) {
            perUnitFormat = secondaryData[PER_INDEX];
        } else {
            String rawPerUnitFormat = getPerUnitFormat(locale, width);
            // rawPerUnitFormat is something like "{0}/{1}"; we need to substitute in the secondary unit.
            // TODO: Lots of thrashing. Improve?
            StringBuilder sb = new StringBuilder();
            String compiled = SimpleFormatterImpl
                    .compileToStringMinMaxArguments(rawPerUnitFormat, sb, 2, 2);
            String secondaryFormat = getWithPlural(secondaryData, StandardPlural.ONE);

            // Some "one" pattern may not contain "{0}". For example in "ar" or "ne" locale.
            String secondaryCompiled = SimpleFormatterImpl
                    .compileToStringMinMaxArguments(secondaryFormat, sb, 0, 1);
            String secondaryFormatString = SimpleFormatterImpl.getTextWithNoArguments(secondaryCompiled);

            // TODO(icu-units#28): do not use regular expression
            String secondaryString = secondaryFormatString.replaceAll("(^\\h*)|(\\h*$)",""); // Trim all spaces.

            perUnitFormat = SimpleFormatterImpl.formatCompiledPattern(compiled, "{0}", secondaryString);
        }
        Map<StandardPlural, SimpleModifier> modifiers = new EnumMap<>(
                StandardPlural.class);
        LongNameHandler result = new LongNameHandler(modifiers, rules, parent);
        result.multiSimpleFormatsToModifiers(primaryData, perUnitFormat, NumberFormat.Field.MEASURE_UNIT);

        // Gender
        String val = getDeriveCompoundRule(locale, "gender", "per");

        assert (val != null && val.length() == 1);
        switch (val.charAt(0)) {
        case '0':
            result.gender = primaryData[GENDER_INDEX];
            break;
        case '1':
            result.gender = secondaryData[GENDER_INDEX];
            break;
        default:
            // Data error. Assert-fail in debug mode, else return no gender.
            assert false;
        }

        return result;
    }

    private void simpleFormatsToModifiers(
            String[] simpleFormats,
            NumberFormat.Field field) {
        StringBuilder sb = new StringBuilder();
        for (StandardPlural plural : StandardPlural.VALUES) {
            String simpleFormat = getWithPlural(simpleFormats, plural);
            String compiled = SimpleFormatterImpl.compileToStringMinMaxArguments(simpleFormat, sb, 0, 1);
            Modifier.Parameters parameters = new Modifier.Parameters();
            parameters.obj = this;
            parameters.signum = null;// Signum ignored
            parameters.plural = plural;
            modifiers.put(plural, new SimpleModifier(compiled, field, false, parameters));
        }
    }

    private void multiSimpleFormatsToModifiers(
            String[] leadFormats,
            String trailFormat,
            NumberFormat.Field field) {
        StringBuilder sb = new StringBuilder();
        String trailCompiled = SimpleFormatterImpl.compileToStringMinMaxArguments(trailFormat, sb, 1, 1);
        for (StandardPlural plural : StandardPlural.VALUES) {
            String leadFormat = getWithPlural(leadFormats, plural);
            String compoundFormat = SimpleFormatterImpl.formatCompiledPattern(trailCompiled, leadFormat);
            String compoundCompiled = SimpleFormatterImpl
                    .compileToStringMinMaxArguments(compoundFormat, sb, 0, 1);
            Modifier.Parameters parameters = new Modifier.Parameters();
            parameters.obj = this;
            parameters.signum = null; // Signum ignored
            parameters.plural = plural;
            modifiers.put(plural, new SimpleModifier(compoundCompiled, field, false, parameters));
        }
    }

    @Override
    public MicroProps processQuantity(DecimalQuantity quantity) {
        MicroProps micros = parent.processQuantity(quantity);
        StandardPlural pluralForm = RoundingUtils.getPluralSafe(micros.rounder, rules, quantity);
        micros.modOuter = modifiers.get(pluralForm);
        micros.gender = this.gender;
        return micros;
    }

    /**
     * Produces a plural-appropriate Modifier for a unit: `quantity` is taken as
     * the final smallest unit, while the larger unit values must be provided
     * via `micros.mixedMeasures`.
     *
     * Does not call parent.processQuantity, so cannot get a MicroProps instance
     * that way. Instead, the instance is passed in as a parameter.
     */
    @Override
    public MicroProps processQuantityWithMicros(DecimalQuantity quantity, MicroProps micros) {
        StandardPlural pluralForm = RoundingUtils.getPluralSafe(micros.rounder, rules, quantity);
        micros.modOuter = modifiers.get(pluralForm);
        return micros;
    }

    @Override
    public Modifier getModifier(Signum signum, StandardPlural plural) {
        // Signum ignored
        return modifiers.get(plural);
    }
}
