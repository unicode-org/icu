// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number;

import java.util.EnumMap;
import java.util.Map;

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

    //////////////////////////
    /// BEGIN DATA LOADING ///
    //////////////////////////

    private static final class PluralTableSink extends UResource.Sink {

        Map<StandardPlural, String> output;

        public PluralTableSink(Map<StandardPlural, String> output) {
            this.output = output;
        }

        @Override
        public void put(UResource.Key key, UResource.Value value, boolean noFallback) {
            UResource.Table pluralsTable = value.getTable();
            for (int i = 0; pluralsTable.getKeyAndValue(i, key, value); ++i) {
                if (key.contentEquals("dnam") || key.contentEquals("per")) {
                    continue;
                }
                StandardPlural plural = StandardPlural.fromString(key);
                if (output.containsKey(plural)) {
                    continue;
                }
                String formatString = value.getString();
                output.put(plural, formatString);
            }
        }
    }

    private static void getMeasureData(ULocale locale, MeasureUnit unit, UnitWidth width,
            Map<StandardPlural, String> output) {
        PluralTableSink sink = new PluralTableSink(output);
        ICUResourceBundle resource;
        resource = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_UNIT_BASE_NAME, locale);
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
        resource.getAllItemsWithFallback(key.toString(), sink);
    }

    private static void getCurrencyLongNameData(ULocale locale, Currency currency, Map<StandardPlural, String> output) {
        // In ICU4J, this method gets a CurrencyData from CurrencyData.provider.
        // TODO(ICU4J): Implement this without going through CurrencyData, like in ICU4C?
        Map<String, String> data = CurrencyData.provider.getInstance(locale, true).getUnitPatterns();
        for (Map.Entry<String, String> e : data.entrySet()) {
            String pluralKeyword = e.getKey();
            StandardPlural plural = StandardPlural.fromString(e.getKey());
            String longName = currency.getName(locale, Currency.PLURAL_LONG_NAME, pluralKeyword, null);
            String simpleFormat = e.getValue();
            // Example pattern from data: "{0} {1}"
            // Example output after find-and-replace: "{0} US dollars"
            simpleFormat = simpleFormat.replace("{1}", longName);
            // String compiled = SimpleFormatterImpl.compileToStringMinMaxArguments(simpleFormat, sb, 1, 1);
            // SimpleModifier mod = new SimpleModifier(compiled, Field.CURRENCY, false);
            output.put(plural, simpleFormat);
        }
    }

    ////////////////////////
    /// END DATA LOADING ///
    ////////////////////////

    private final Map<StandardPlural, SimpleModifier> modifiers;
    private final PluralRules rules;
    private final MicroPropsGenerator parent;

    private LongNameHandler(Map<StandardPlural, SimpleModifier> modifiers, PluralRules rules,
            MicroPropsGenerator parent) {
        this.modifiers = modifiers;
        this.rules = rules;
        this.parent = parent;
    }

    public static LongNameHandler forCurrencyLongNames(ULocale locale, Currency currency, PluralRules rules,
            MicroPropsGenerator parent) {
        Map<StandardPlural, String> simpleFormats = new EnumMap<StandardPlural, String>(StandardPlural.class);
        getCurrencyLongNameData(locale, currency, simpleFormats);
        // TODO(ICU4J): Reduce the number of object creations here?
        Map<StandardPlural, SimpleModifier> modifiers = new EnumMap<StandardPlural, SimpleModifier>(
                StandardPlural.class);
        simpleFormatsToModifiers(simpleFormats, null, modifiers);
        return new LongNameHandler(modifiers, rules, parent);
    }

    public static LongNameHandler forMeasureUnit(ULocale locale, MeasureUnit unit, UnitWidth width, PluralRules rules,
            MicroPropsGenerator parent) {
        Map<StandardPlural, String> simpleFormats = new EnumMap<StandardPlural, String>(StandardPlural.class);
        getMeasureData(locale, unit, width, simpleFormats);
        // TODO: What field to use for units?
        // TODO(ICU4J): Reduce the number of object creations here?
        Map<StandardPlural, SimpleModifier> modifiers = new EnumMap<StandardPlural, SimpleModifier>(
                StandardPlural.class);
        simpleFormatsToModifiers(simpleFormats, null, modifiers);
        return new LongNameHandler(modifiers, rules, parent);
    }

    private static void simpleFormatsToModifiers(Map<StandardPlural, String> simpleFormats, NumberFormat.Field field,
            Map<StandardPlural, SimpleModifier> output) {
        StringBuilder sb = new StringBuilder();
        for (StandardPlural plural : StandardPlural.VALUES) {
            String simpleFormat = simpleFormats.get(plural);
            if (simpleFormat == null) {
                simpleFormat = simpleFormats.get(StandardPlural.OTHER);
            }
            if (simpleFormat == null) {
                // There should always be data in the "other" plural variant.
                throw new ICUException("Could not find data in 'other' plural variant with field " + field);
            }
            String compiled = SimpleFormatterImpl.compileToStringMinMaxArguments(simpleFormat, sb, 1, 1);
            output.put(plural, new SimpleModifier(compiled, null, false));
        }
    }

    @Override
    public MicroProps processQuantity(DecimalQuantity quantity) {
        MicroProps micros = parent.processQuantity(quantity);
        // TODO: Avoid the copy here?
        DecimalQuantity copy = quantity.createCopy();
        micros.rounding.apply(copy);
        micros.modOuter = modifiers.get(copy.getStandardPlural(rules));
        return micros;
    }
}
