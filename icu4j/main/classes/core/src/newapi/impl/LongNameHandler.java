// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package newapi.impl;

import java.util.EnumMap;
import java.util.Map;

import com.ibm.icu.impl.CurrencyData;
import com.ibm.icu.impl.ICUData;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.SimpleFormatterImpl;
import com.ibm.icu.impl.StandardPlural;
import com.ibm.icu.impl.UResource;
import com.ibm.icu.impl.number.DecimalQuantity;
import com.ibm.icu.impl.number.modifiers.SimpleModifier;
import com.ibm.icu.text.NumberFormat.Field;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.ICUException;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;

import newapi.NumberFormatter.UnitWidth;

public class LongNameHandler implements MicroPropsGenerator {

    private final Map<StandardPlural, SimpleModifier> modifiers;
    private PluralRules rules;
    private MicroPropsGenerator parent;

    private LongNameHandler(Map<StandardPlural, SimpleModifier> modifiers) {
        this.modifiers = modifiers;
    }

    /** For use by the "safe" code path */
    private LongNameHandler(LongNameHandler other) {
        this.modifiers = other.modifiers;
    }

    public static LongNameHandler forCurrencyLongNames(ULocale loc, Currency currency) {
        Map<String, String> data = CurrencyData.provider.getInstance(loc, true).getUnitPatterns();
        Map<StandardPlural, SimpleModifier> result = new EnumMap<StandardPlural, SimpleModifier>(StandardPlural.class);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : data.entrySet()) {
            String pluralKeyword = e.getKey();
            StandardPlural plural = StandardPlural.fromString(e.getKey());
            String longName = currency.getName(loc, Currency.PLURAL_LONG_NAME, pluralKeyword, null);
            String simpleFormat = e.getValue(); // e.g., "{0} {1}"
            simpleFormat = simpleFormat.replace("{1}", longName);
            String compiled = SimpleFormatterImpl.compileToStringMinMaxArguments(simpleFormat, sb, 1, 1);
            SimpleModifier mod = new SimpleModifier(compiled, Field.CURRENCY, false);
            result.put(plural, mod);
        }
        return new LongNameHandler(result);
    }

    public static LongNameHandler forMeasureUnit(ULocale loc, MeasureUnit unit, UnitWidth width) {
        Map<StandardPlural, String> simpleFormats = getMeasureData(loc, unit, width);
        Map<StandardPlural, SimpleModifier> result = new EnumMap<StandardPlural, SimpleModifier>(StandardPlural.class);
        StringBuilder sb = new StringBuilder();
        for (StandardPlural plural : StandardPlural.VALUES) {
            String simpleFormat = simpleFormats.get(plural);
            if (simpleFormat == null) {
                simpleFormat = simpleFormats.get(StandardPlural.OTHER);
            }
            if (simpleFormat == null) {
                // There should always be data in the "other" plural variant.
                throw new ICUException("Could not find data in 'other' plural variant for unit " + unit);
            }
            String compiled = SimpleFormatterImpl.compileToStringMinMaxArguments(simpleFormat, sb, 1, 1);
            // TODO: What field to use for units?
            SimpleModifier mod = new SimpleModifier(compiled, null, false);
            result.put(plural, mod);
        }
        return new LongNameHandler(result);
    }

    /**
     * Applies locale data and inserts a long-name handler into the quantity chain.
     *
     * @param rules
     *            The PluralRules instance to reference.
     * @param parent
     *            The old head of the quantity chain.
     * @return The new head of the quantity chain.
     */
    public MicroPropsGenerator withLocaleData(PluralRules rules, MicroPropsGenerator parent) {
        this.rules = rules;
        this.parent = parent;
        return this;
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

    ///////////////////////////////////////
    /// BEGIN MEASURE UNIT DATA LOADING ///
    ///////////////////////////////////////

    private static final class MeasureUnitSink extends UResource.Sink {

        Map<StandardPlural, String> output;

        public MeasureUnitSink(Map<StandardPlural, String> output) {
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

    private static Map<StandardPlural, String> getMeasureData(ULocale locale, MeasureUnit unit, UnitWidth width) {
        ICUResourceBundle resource = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_UNIT_BASE_NAME,
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
        Map<StandardPlural, String> output = new EnumMap<StandardPlural, String>(StandardPlural.class);
        MeasureUnitSink sink = new MeasureUnitSink(output);
        resource.getAllItemsWithFallback(key.toString(), sink);
        return output;
    }

    /////////////////////////////////////
    /// END MEASURE UNIT DATA LOADING ///
    /////////////////////////////////////
}
