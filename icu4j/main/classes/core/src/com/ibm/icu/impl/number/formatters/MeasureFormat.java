// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.formatters;

import com.ibm.icu.impl.StandardPlural;
import com.ibm.icu.impl.number.modifiers.GeneralPluralModifier;
import com.ibm.icu.impl.number.modifiers.SimpleModifier;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.MeasureFormat.FormatWidth;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.ULocale;

public class MeasureFormat {

  public static interface IProperties {

    static MeasureUnit DEFAULT_MEASURE_UNIT = null;

    /** @see #setMeasureUnit */
    public MeasureUnit getMeasureUnit();

    /**
     * Apply prefixes and suffixes for the specified {@link MeasureUnit} to the formatted number.
     *
     * @param measureUnit The measure unit.
     * @return The property bag, for chaining.
     */
    public IProperties setMeasureUnit(MeasureUnit measureUnit);

    static FormatWidth DEFAULT_MEASURE_FORMAT_WIDTH = null;

    /** @see #setMeasureFormatWidth */
    public FormatWidth getMeasureFormatWidth();

    /**
     * Use the specified {@link FormatWidth} when choosing the style of measure unit prefix/suffix.
     *
     * <p>Must be used in conjunction with {@link #setMeasureUnit}.
     *
     * @param measureFormatWidth The width style. Defaults to FormatWidth.WIDE.
     * @return The property bag, for chaining.
     */
    public IProperties setMeasureFormatWidth(FormatWidth measureFormatWidth);
  }

  public static boolean useMeasureFormat(IProperties properties) {
    return properties.getMeasureUnit() != IProperties.DEFAULT_MEASURE_UNIT;
  }

  public static GeneralPluralModifier getInstance(DecimalFormatSymbols symbols, IProperties properties) {
    ULocale uloc = symbols.getULocale();
    MeasureUnit unit = properties.getMeasureUnit();
    FormatWidth width = properties.getMeasureFormatWidth();

    if (unit == null) {
      throw new IllegalArgumentException("A measure unit is required for MeasureFormat");
    }
    if (width == null) {
      width = FormatWidth.WIDE;
    }

    // Temporarily, create a MeasureFormat instance for its data loading capability
    // TODO: Move data loading directly into this class file
    com.ibm.icu.text.MeasureFormat mf = com.ibm.icu.text.MeasureFormat.getInstance(uloc, width);
    GeneralPluralModifier mod = new GeneralPluralModifier();
    for (StandardPlural plural : StandardPlural.VALUES) {
      String formatString = null;
      mf.getPluralFormatter(unit, width, plural.ordinal());
      mod.put(plural, new SimpleModifier(formatString, null, false));
    }
    return mod;
  }
}
