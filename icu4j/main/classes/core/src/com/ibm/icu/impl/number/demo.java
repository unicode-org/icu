// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.List;

import com.ibm.icu.impl.number.formatters.PaddingFormat.PadPosition;
import com.ibm.icu.impl.number.formatters.RangeFormat;
import com.ibm.icu.impl.number.modifiers.SimpleModifier;
import com.ibm.icu.text.CompactDecimalFormat.CompactStyle;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.util.MeasureUnit;

public class demo {

  public static void main(String[] args) throws ParseException {
    SimpleModifier.testFormatAsPrefixSuffix();

    System.out.println(new FormatQuantity1(3.14159));
    System.out.println(new FormatQuantity1(3.14159, true));
    System.out.println(new FormatQuantity2(3.14159));

    System.out.println(
        PatternString.propertiesToString(PatternString.parseToProperties("+**##,##,#00.05#%")));

    ParsePosition ppos = new ParsePosition(0);
    System.out.println(
        Parse.parse(
            "dd123",
            ppos,
            new Properties().setPositivePrefix("dd").setNegativePrefix("ddd"),
            DecimalFormatSymbols.getInstance()));
    System.out.println(ppos);

    List<Format> formats = new ArrayList<Format>();

    Properties properties = new Properties();
    Format ndf = Endpoint.fromBTA(properties);
    formats.add(ndf);

    properties =
        new Properties()
            .setMinimumSignificantDigits(3)
            .setMaximumSignificantDigits(3)
            .setCompactStyle(CompactStyle.LONG);
    Format cdf = Endpoint.fromBTA(properties);
    formats.add(cdf);

    properties =
        new Properties().setFormatWidth(10).setPadPosition(PadPosition.AFTER_PREFIX);
    Format pdf = Endpoint.fromBTA(properties);
    formats.add(pdf);

    properties =
        new Properties()
            .setMinimumExponentDigits(1)
            .setMaximumIntegerDigits(3)
            .setMaximumFractionDigits(1);
    Format exf = Endpoint.fromBTA(properties);
    formats.add(exf);

    properties = new Properties().setRoundingIncrement(new BigDecimal("0.5"));
    Format rif = Endpoint.fromBTA(properties);
    formats.add(rif);

    properties = new Properties().setMeasureUnit(MeasureUnit.HECTARE);
    Format muf = Endpoint.fromBTA(properties);
    formats.add(muf);

    properties =
        new Properties().setMeasureUnit(MeasureUnit.HECTARE).setCompactStyle(CompactStyle.LONG);
    Format cmf = Endpoint.fromBTA(properties);
    formats.add(cmf);

    properties = PatternString.parseToProperties("#,##0.00 \u00a4");
    Format ptf = Endpoint.fromBTA(properties);
    formats.add(ptf);

    RangeFormat rf = new RangeFormat(cdf, cdf, " to ");
    System.out.println(rf.format(new FormatQuantity2(1234), new FormatQuantity2(2345)));

    String[] cases = {
      "1.0",
      "2.01",
      "1234.56",
      "3000.0",
      //      "512.0000000000017",
      //      "4096.000000000001",
      //      "4096.000000000004",
      //      "4096.000000000005",
      //      "4096.000000000006",
      //      "4096.000000000007",
      "0.00026418",
      "0.01789261",
      "468160.0",
      "999000.0",
      "999900.0",
      "999990.0",
      "0.0",
      "12345678901.0",
      //      "789000000000000000000000.0",
      //      "789123123567853156372158.0",
      "-5193.48",
    };

    for (String str : cases) {
      System.out.println("----------");
      System.out.println(str);
      System.out.println("  NDF: " + ndf.format(new FormatQuantity2(Double.parseDouble(str))));
      System.out.println("  CDF: " + cdf.format(new FormatQuantity2(Double.parseDouble(str))));
      System.out.println("  PWD: " + pdf.format(new FormatQuantity2(Double.parseDouble(str))));
      System.out.println("  EXF: " + exf.format(new FormatQuantity2(Double.parseDouble(str))));
      System.out.println("  RIF: " + rif.format(new FormatQuantity2(Double.parseDouble(str))));
      System.out.println("  MUF: " + muf.format(new FormatQuantity2(Double.parseDouble(str))));
      System.out.println("  CMF: " + cmf.format(new FormatQuantity2(Double.parseDouble(str))));
      System.out.println("  PTF: " + ptf.format(new FormatQuantity2(Double.parseDouble(str))));
    }
  }
}
