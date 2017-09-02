// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package newapi.impl;

import java.util.EnumMap;
import java.util.Map;

import com.ibm.icu.impl.CurrencyData;
import com.ibm.icu.impl.SimpleFormatterImpl;
import com.ibm.icu.impl.StandardPlural;
import com.ibm.icu.impl.number.DecimalQuantity;
import com.ibm.icu.impl.number.Modifier;
import com.ibm.icu.impl.number.modifiers.SimpleModifier;
import com.ibm.icu.text.NumberFormat.Field;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.ULocale;

import newapi.NumberFormatter.UnitWidth;

public class LongNameHandler implements MicroPropsGenerator {

    private final Map<StandardPlural, Modifier> data;
    /* unsafe */ PluralRules rules;
    /* unsafe */ MicroPropsGenerator parent;

    private LongNameHandler(Map<StandardPlural, Modifier> data) {
        this.data = data;
    }

    /** For use by the "safe" code path */
    private LongNameHandler(LongNameHandler other) {
        this.data = other.data;
    }

    public static LongNameHandler getCurrencyLongNameModifiers(ULocale loc, Currency currency) {
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
        return new LongNameHandler(result);
    }

    public static LongNameHandler getMeasureUnitModifiers(ULocale loc, MeasureUnit unit, UnitWidth width) {
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
        return new LongNameHandler(result);
    }

    /**
     * Applies locale data and inserts a long-name handler into the quantity chain.
     *
     * @param rules
     *            The PluralRules instance to reference.
     * @param safe
     *            If true, creates a new object to insert into the quantity chain. If false, re-uses <em>this</em>
     *            object in the quantity chain.
     * @param parent
     *            The old head of the quantity chain.
     * @return The new head of the quantity chain.
     */
    public MicroPropsGenerator withLocaleData(PluralRules rules, boolean safe, MicroPropsGenerator parent) {
        if (safe) {
            // Safe code path: return a new object
            LongNameHandler copy = new LongNameHandler(this);
            copy.rules = rules;
            copy.parent = parent;
            return copy;
        } else {
            // Unsafe code path: re-use this object!
            this.rules = rules;
            this.parent = parent;
            return this;
        }
    }

    @Override
    public MicroProps processQuantity(DecimalQuantity quantity) {
        MicroProps micros = parent.processQuantity(quantity);
        // TODO: Avoid the copy here?
        DecimalQuantity copy = quantity.createCopy();
        micros.rounding.apply(copy);
        micros.modOuter = data.get(copy.getStandardPlural(rules));
        return micros;
    }
}
