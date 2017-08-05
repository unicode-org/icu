// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package newapi.impl;

import java.util.EnumMap;
import java.util.Map;

import com.ibm.icu.impl.ICUData;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.StandardPlural;
import com.ibm.icu.impl.UResource;
import com.ibm.icu.text.MeasureFormat.FormatWidth;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;

public class MeasureData {

  private static final class ShanesMeasureUnitSink extends UResource.Sink {

    Map<StandardPlural, String> output;

    public ShanesMeasureUnitSink(Map<StandardPlural, String> output) {
      this.output = output;
    }

    @Override
    public void put(UResource.Key key, UResource.Value value, boolean noFallback) {
      UResource.Table pluralsTable = value.getTable();
      for (int i1 = 0; pluralsTable.getKeyAndValue(i1, key, value); ++i1) {
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

  public static Map<StandardPlural, String> getMeasureData(
      ULocale locale, MeasureUnit unit, FormatWidth width) {
    ICUResourceBundle resource =
        (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_UNIT_BASE_NAME, locale);
    StringBuilder key = new StringBuilder();
    key.append("units");
    if (width == FormatWidth.NARROW) {
      key.append("Narrow");
    } else if (width == FormatWidth.SHORT) {
      key.append("Short");
    }
    key.append("/");
    key.append(unit.getType());
    key.append("/");
    key.append(unit.getSubtype());
    Map<StandardPlural, String> output = new EnumMap<StandardPlural, String>(StandardPlural.class);
    ShanesMeasureUnitSink sink = new ShanesMeasureUnitSink(output);
    resource.getAllItemsWithFallback(key.toString(), sink);
    return output;
  }
}
