// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package newapi;

import java.util.EnumMap;
import java.util.Map;

import com.ibm.icu.impl.CurrencyData;
import com.ibm.icu.impl.SimpleFormatterImpl;
import com.ibm.icu.impl.StandardPlural;
import com.ibm.icu.impl.number.FormatQuantity;
import com.ibm.icu.impl.number.Modifier;
import com.ibm.icu.impl.number.modifiers.SimpleModifier;
import com.ibm.icu.text.MeasureFormat.FormatWidth;
import com.ibm.icu.text.NumberFormat.Field;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.ULocale;

import newapi.impl.MeasureData;
import newapi.impl.MicroProps;
import newapi.impl.QuantityChain;

class MurkyLongNameHandler implements QuantityChain {

    private final Map<StandardPlural, Modifier> data;
    /* unsafe */ PluralRules rules;
    /* unsafe */ QuantityChain parent;

    private MurkyLongNameHandler(Map<StandardPlural, Modifier> data) {
        this.data = data;
    }

    public static MurkyLongNameHandler getCurrencyLongNameModifiers(ULocale loc, Currency currency) {
        Map<String, String> data = CurrencyData.provider.getInstance(loc, true).getUnitPatterns();
        Map<StandardPlural, Modifier> result = new EnumMap<StandardPlural, Modifier>(StandardPlural.class);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : data.entrySet()) {
            String pluralKeyword = e.getKey();
            StandardPlural plural = StandardPlural.fromString(e.getKey());
            String longName = currency.getName(loc, Currency.PLURAL_LONG_NAME, pluralKeyword, null);
            String simpleFormat = e.getValue(); // e.g., "{0} {1}"
            simpleFormat = simpleFormat.replace("{1}", longName);
            String compiled = SimpleFormatterImpl.compileToStringMinMaxArguments(simpleFormat, sb, 1, 1);
            Modifier mod = new SimpleModifier(compiled, Field.CURRENCY, false);
            result.put(plural, mod);
        }
        return new MurkyLongNameHandler(result);
    }

    public static MurkyLongNameHandler getMeasureUnitModifiers(ULocale loc, MeasureUnit unit, FormatWidth width) {
        Map<StandardPlural, String> simpleFormats = MeasureData.getMeasureData(loc, unit, width);
        Map<StandardPlural, Modifier> result = new EnumMap<StandardPlural, Modifier>(StandardPlural.class);
        StringBuilder sb = new StringBuilder();
        for (StandardPlural plural : StandardPlural.VALUES) {
            if (simpleFormats.get(plural) == null) {
                plural = StandardPlural.OTHER;
            }
            String simpleFormat = simpleFormats.get(plural);
            String compiled = SimpleFormatterImpl.compileToStringMinMaxArguments(simpleFormat, sb, 1, 1);
            Modifier mod = new SimpleModifier(compiled, Field.CURRENCY, false);
            result.put(plural, mod);
        }
        return new MurkyLongNameHandler(result);
    }

    public QuantityChain withLocaleData(PluralRules rules, boolean safe, QuantityChain parent) {
        if (safe) {
            // Safe code path: return a new object
            return new ImmutableLongNameHandler(data, rules, parent);
        } else {
            // Unsafe code path: re-use this object!
            this.rules = rules;
            this.parent = parent;
            return this;
        }
    }

    @Override
    public MicroProps withQuantity(FormatQuantity quantity) {
        MicroProps micros = parent.withQuantity(quantity);
        // TODO: Avoid the copy here?
        FormatQuantity copy = quantity.createCopy();
        micros.rounding.apply(copy);
        micros.modOuter = data.get(copy.getStandardPlural(rules));
        return micros;
    }

    public static class ImmutableLongNameHandler implements QuantityChain {
        final Map<StandardPlural, Modifier> data;
        final PluralRules rules;
        final QuantityChain parent;

        public ImmutableLongNameHandler(Map<StandardPlural, Modifier> data, PluralRules rules, QuantityChain parent) {
            this.data = data;
            this.rules = rules;
            this.parent = parent;
        }

        @Override
        public MicroProps withQuantity(FormatQuantity quantity) {
            MicroProps micros = parent.withQuantity(quantity);
            // TODO: Avoid the copy here?
            FormatQuantity copy = quantity.createCopy();
            micros.rounding.apply(copy);
            micros.modOuter = data.get(copy.getStandardPlural(rules));
            return micros;
        }
    }
}
