// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package newapi.impl;

import java.util.EnumMap;
import java.util.Map;

import com.ibm.icu.impl.CurrencyData;
import com.ibm.icu.impl.SimpleFormatterImpl;
import com.ibm.icu.impl.StandardPlural;
import com.ibm.icu.impl.number.Modifier;
import com.ibm.icu.impl.number.modifiers.SimpleModifier;
import com.ibm.icu.text.MeasureFormat.FormatWidth;
import com.ibm.icu.text.NumberFormat.Field;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.ULocale;

public class DataUtils {

  public static Map<StandardPlural, Modifier> getCurrencyLongNameModifiers(
      ULocale loc, Currency currency) {
    Map<String, String> data = CurrencyData.provider.getInstance(loc, true).getUnitPatterns();
    Map<StandardPlural, Modifier> result =
        new EnumMap<StandardPlural, Modifier>(StandardPlural.class);
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
    return result;
  }

  public static Map<StandardPlural, Modifier> getMeasureUnitModifiers(
      ULocale loc, MeasureUnit unit, FormatWidth width) {
    Map<StandardPlural, String> simpleFormats = MeasureData.getMeasureData(loc, unit, width);
    Map<StandardPlural, Modifier> result =
        new EnumMap<StandardPlural, Modifier>(StandardPlural.class);
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
    return result;
    //    Map<StandardPlural, Modifier> result =
    //        new EnumMap<StandardPlural, Modifier>(StandardPlural.class);
    //    // TODO: Get the data directly instead of taking the detour through MeasureFormat.
    //    MeasureFormat mf = MeasureFormat.getInstance(loc, width);
    //    for (StandardPlural plural : StandardPlural.VALUES) {
    //      String compiled = mf.getPluralFormatter(unit, width, plural.ordinal());
    //      Modifier mod = new SimpleModifier(compiled, null, false);
    //      result.put(plural, mod);
    //    }
    //    return result;
  }
}
