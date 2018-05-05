// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
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
import com.ibm.icu.number.NumberFormatter.UnitWidth;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.ICUException;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;

public class LongNameHandler implements MicroPropsGenerator {

    private static final int DNAM_INDEX = StandardPlural.COUNT;
    private static final int PER_INDEX = StandardPlural.COUNT + 1;
    private static final int ARRAY_LENGTH = StandardPlural.COUNT + 2;

    private static int getIndex(String pluralKeyword) {
        // pluralKeyword can also be "dnam" or "per"
        if (pluralKeyword.equals("dnam")) {
            return DNAM_INDEX;
        } else if (pluralKeyword.equals("per")) {
            return PER_INDEX;
        } else {
            return StandardPlural.fromString(pluralKeyword).ordinal();
        }
    }

    private static String getWithPlural(String[] strings, StandardPlural plural) {
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
                int index = getIndex(key.toString());
                if (outArray[index] != null) {
                    continue;
                }
                String formatString = value.getString();
                outArray[index] = formatString;
            }
        }
    }

    // NOTE: outArray MUST have at least ARRAY_LENGTH entries. No bounds checking is performed.

    private static void getMeasureData(
            ULocale locale,
            MeasureUnit unit,
            UnitWidth width,
            String[] outArray) {
        PluralTableSink sink = new PluralTableSink(outArray);
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
        key.append("/");
        key.append(unit.getType());
        key.append("/");
        key.append(unit.getSubtype());
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

    ////////////////////////
    /// END DATA LOADING ///
    ////////////////////////

    private final Map<StandardPlural, SimpleModifier> modifiers;
    private final PluralRules rules;
    private final MicroPropsGenerator parent;

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
        getMeasureData(locale, unit, width, measureData);
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
        Map<StandardPlural, SimpleModifier> modifiers = new EnumMap<StandardPlural, SimpleModifier>(
                StandardPlural.class);
        simpleFormatsToModifiers(simpleFormats, null, modifiers);
        return new LongNameHandler(modifiers, rules, parent);
    }

    public static LongNameHandler forMeasureUnit(
            ULocale locale,
            MeasureUnit unit,
            MeasureUnit perUnit,
            UnitWidth width,
            PluralRules rules,
            MicroPropsGenerator parent) {
        if (perUnit != null) {
            // Compound unit: first try to simplify (e.g., meters per second is its own unit).
            MeasureUnit simplified = MeasureUnit.resolveUnitPerUnit(unit, perUnit);
            if (simplified != null) {
                unit = simplified;
            } else {
                // No simplified form is available.
                return forCompoundUnit(locale, unit, perUnit, width, rules, parent);
            }
        }

        String[] simpleFormats = new String[ARRAY_LENGTH];
        getMeasureData(locale, unit, width, simpleFormats);
        // TODO: What field to use for units?
        // TODO(ICU4J): Reduce the number of object creations here?
        Map<StandardPlural, SimpleModifier> modifiers = new EnumMap<StandardPlural, SimpleModifier>(
                StandardPlural.class);
        simpleFormatsToModifiers(simpleFormats, null, modifiers);
        return new LongNameHandler(modifiers, rules, parent);
    }

    private static LongNameHandler forCompoundUnit(
            ULocale locale,
            MeasureUnit unit,
            MeasureUnit perUnit,
            UnitWidth width,
            PluralRules rules,
            MicroPropsGenerator parent) {
        String[] primaryData = new String[ARRAY_LENGTH];
        getMeasureData(locale, unit, width, primaryData);
        String[] secondaryData = new String[ARRAY_LENGTH];
        getMeasureData(locale, perUnit, width, secondaryData);
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
            String secondaryCompiled = SimpleFormatterImpl
                    .compileToStringMinMaxArguments(secondaryFormat, sb, 1, 1);
            String secondaryString = SimpleFormatterImpl.getTextWithNoArguments(secondaryCompiled)
                    .trim();
            perUnitFormat = SimpleFormatterImpl.formatCompiledPattern(compiled, "{0}", secondaryString);
        }
        // TODO: What field to use for units?
        Map<StandardPlural, SimpleModifier> modifiers = new EnumMap<StandardPlural, SimpleModifier>(
                StandardPlural.class);
        multiSimpleFormatsToModifiers(primaryData, perUnitFormat, null, modifiers);
        return new LongNameHandler(modifiers, rules, parent);
    }

    private static void simpleFormatsToModifiers(
            String[] simpleFormats,
            NumberFormat.Field field,
            Map<StandardPlural, SimpleModifier> output) {
        StringBuilder sb = new StringBuilder();
        for (StandardPlural plural : StandardPlural.VALUES) {
            String simpleFormat = getWithPlural(simpleFormats, plural);
            String compiled = SimpleFormatterImpl.compileToStringMinMaxArguments(simpleFormat, sb, 0, 1);
            output.put(plural, new SimpleModifier(compiled, field, false));
        }
    }

    private static void multiSimpleFormatsToModifiers(
            String[] leadFormats,
            String trailFormat,
            NumberFormat.Field field,
            Map<StandardPlural, SimpleModifier> output) {
        StringBuilder sb = new StringBuilder();
        String trailCompiled = SimpleFormatterImpl.compileToStringMinMaxArguments(trailFormat, sb, 1, 1);
        for (StandardPlural plural : StandardPlural.VALUES) {
            String leadFormat = getWithPlural(leadFormats, plural);
            String compoundFormat = SimpleFormatterImpl.formatCompiledPattern(trailCompiled, leadFormat);
            String compoundCompiled = SimpleFormatterImpl
                    .compileToStringMinMaxArguments(compoundFormat, sb, 0, 1);
            output.put(plural, new SimpleModifier(compoundCompiled, field, false));
        }
    }

    @Override
    public MicroProps processQuantity(DecimalQuantity quantity) {
        MicroProps micros = parent.processQuantity(quantity);
        // TODO: Avoid the copy here?
        DecimalQuantity copy = quantity.createCopy();
        micros.rounder.apply(copy);
        micros.modOuter = modifiers.get(copy.getStandardPlural(rules));
        return micros;
    }
}
