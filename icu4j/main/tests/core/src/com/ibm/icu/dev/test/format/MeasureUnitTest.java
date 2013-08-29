/*
 *******************************************************************************
 * Copyright (C) 2013, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.format;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.test.serializable.SerializableTest;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.GeneralMeasureFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.FormatWidth;
import com.ibm.icu.util.Measure;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.ULocale;

/**
 * @author markdavis
 */
public class MeasureUnitTest extends TestFmwk {
    /**
     * @author markdavis
     *
     */
    public static void main(String[] args) {
        //generateConstants(); if (true) return;
        new MeasureUnitTest().run(args);
    }

    public void testAUnit() {
        String lastType = null;
        for (MeasureUnit expected : MeasureUnit.getAvailable()) {
            String type = expected.getType();
            String code = expected.getCode();
            if (!type.equals(lastType)) {
                logln(type);
                lastType = type;
            }
            MeasureUnit actual = MeasureUnit.getInstance(type, code);
            assertSame("Identity check", expected, actual);
        }
    }

    public void testMultiples() {
        for (ULocale locale : new ULocale[]{
                ULocale.ENGLISH, 
                new ULocale("ru"), 
                //ULocale.JAPANESE
        }) {

            for (FormatWidth style : FormatWidth.values()) {
                GeneralMeasureFormat gmlf = GeneralMeasureFormat.getInstance(locale, style);
                String formatted = gmlf.format(
                        new Measure(2, MeasureUnit.MILE), 
                        new Measure(1, MeasureUnit.FOOT), 
                        new Measure(2.3, MeasureUnit.INCH));
                logln(locale + ",\t" + style + ": " + formatted);
            }

        }
    }

    public void testGram() {
        checkRoundtrip(ULocale.ENGLISH, MeasureUnit.GRAM, 1, 0, FormatWidth.SHORT);
        checkRoundtrip(ULocale.ENGLISH, MeasureUnit.G_FORCE, 1, 0, FormatWidth.SHORT);
    }

    public void testRoundtripFormat() {        
        for (ULocale locale : new ULocale[]{
                ULocale.ENGLISH, 
                new ULocale("ru"), 
                //ULocale.JAPANESE
        }) {
            for (MeasureUnit unit : MeasureUnit.getAvailable()) {
                for (double d : new double[]{2.1, 1}) {
                    for (int fractionalDigits : new int[]{0, 1}) {
                        for (FormatWidth style : FormatWidth.values()) {
                            checkRoundtrip(locale, unit, d, fractionalDigits, style);
                        }
                    }
                }
            }
        }
    }

    private void checkRoundtrip(ULocale locale, MeasureUnit unit, double d, int fractionalDigits, FormatWidth style) {
        if (unit instanceof Currency) {
            return; // known limitation
        }
        Measure amount = new Measure(d, unit);
        String header = locale
                + "\t" + unit
                + "\t" + d
                + "\t" + fractionalDigits;
        ParsePosition pex = new ParsePosition(0);
        NumberFormat nformat = NumberFormat.getInstance(locale);
        nformat.setMinimumFractionDigits(fractionalDigits);

        GeneralMeasureFormat format = GeneralMeasureFormat.getInstance(locale, style, nformat);
        
        FieldPosition pos = new FieldPosition(DecimalFormat.FRACTION_FIELD);
        StringBuffer b = format.format(amount, new StringBuffer(), pos);
        String message = header + "\t" + style
                + "\t«" + b.substring(0, pos.getBeginIndex())
                + "⟪" + b.substring(pos.getBeginIndex(), pos.getEndIndex())
                + "⟫" + b.substring(pos.getEndIndex()) + "»";
        pex.setIndex(0);
        Measure unitAmount = format.parseObject(b.toString(), pex);
        if (!assertNotNull(message, unitAmount)) {
            logln("Parse: «" 
                    + b.substring(0,pex.getErrorIndex())
                    + "||" + b.substring(pex.getErrorIndex()) + "»");
        } else if (style != FormatWidth.NARROW) { // narrow items may collide
            if (unit.equals(MeasureUnit.GRAM)) {
                logKnownIssue("cldrupdate", "waiting on collision fix for gram");
                return;
            }
            if (unit.equals(MeasureUnit.ARC_MINUTE) || unit.equals(MeasureUnit.ARC_SECOND) || unit.equals(MeasureUnit.METER)) {
                logKnownIssue("8474", "Waiting for CLDR data");
            } else {
                assertEquals(message + "\tParse Roundtrip of unit", unit, unitAmount.getUnit());
            }
            double actualNumber = unitAmount.getNumber().doubleValue();
            assertEquals(message + "\tParse Roundtrip of number", d, actualNumber);
        }
    }

    public void testExamples() {
        GeneralMeasureFormat fmtFr = GeneralMeasureFormat.getInstance(ULocale.FRENCH, FormatWidth.SHORT);
        Measure measure = new Measure(23, MeasureUnit.CELSIUS);
        assertEquals("", "23°C", fmtFr.format(measure));

        Measure measureF = new Measure(70, MeasureUnit.FAHRENHEIT);
        assertEquals("", "70°F", fmtFr.format(measureF));

        GeneralMeasureFormat fmtFrFull = GeneralMeasureFormat.getInstance(ULocale.FRENCH, FormatWidth.WIDE);
        if (!logKnownIssue("8474", "needs latest CLDR data")) {
            assertEquals("", "70 pieds, 5,3 pouces", fmtFrFull.format(new Measure(70, MeasureUnit.FOOT),
                    new Measure(5.3, MeasureUnit.INCH)));
            assertEquals("", "1 pied, 1 pouce", fmtFrFull.format(new Measure(1, MeasureUnit.FOOT),
                    new Measure(1, MeasureUnit.INCH)));
        }
        // Degenerate case
        GeneralMeasureFormat fmtEn = GeneralMeasureFormat.getInstance(ULocale.ENGLISH, FormatWidth.WIDE);
        assertEquals("", "1 inch, 2 feet", fmtEn.format(new Measure(1, MeasureUnit.INCH),
                new Measure(2, MeasureUnit.FOOT)));

        logln("Show all currently available units");
        String lastType = null;
        for (MeasureUnit unit : MeasureUnit.getAvailable()) {
            String type = unit.getType();
            if (!type.equals(lastType)) {
                logln(type);
                lastType = type;
            }
            logln("\t" + unit);
        }
        // TODO 
        // Add these examples (and others) to the class definition.
        // Clarify that these classes *do not* do conversion; they simply do the formatting of whatever units they
        // are provided.
    }

    static void generateConstants() {
        System.out.println("static final MeasureUnit");
        Map<String, MeasureUnit> seen = new HashMap<String, MeasureUnit>();
        boolean first = true;
        for (String type : new TreeSet<String>(MeasureUnit.getAvailableTypes())) {
            for (MeasureUnit unit : MeasureUnit.getAvailable(type)) {
                String code = unit.getCode();
                String name = code.toUpperCase(Locale.ENGLISH).replace("-", "_");

                if (type.equals("angle")) {
                    if (code.equals("minute") || code.equals("second")) {
                        name = "ARC_" + name;
                    }
                }
                if (first) {
                    first = false;
                } else {
                    System.out.print(",");
                }
                if (seen.containsKey(name)) {
                    System.out.println("\nCollision!!" + unit + ", " + seen.get(name));
                } else {
                    seen.put(name, unit);
                }
                System.out.println("\n\t/** Constant for unit of " + type +
                        ": " +
                        code +
                        " */");

                System.out.print("\t" + name + " = MeasureUnit.getInstance(\"" +
                        type +
                        "\", \"" +
                        code +
                        "\")");
            }
            System.out.println(";");
        }
    }
    
    public void TestSerial() {
        checkStreamingEquality(MeasureUnit.CELSIUS);
        checkStreamingEquality(GeneralMeasureFormat.getInstance(ULocale.FRANCE, FormatWidth.NARROW));
        checkStreamingEquality(Currency.getInstance("EUR"));
    }
    
    public <T extends Serializable> void checkStreamingEquality(T item) {
        try {
          ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
          ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteOut);
          objectOutputStream.writeObject(item);
          objectOutputStream.close();
          byte[] contents = byteOut.toByteArray();
          logln("bytes: " + contents.length + "; " + item.getClass() + ": " + showBytes(contents));
          ByteArrayInputStream byteIn = new ByteArrayInputStream(contents);
          ObjectInputStream objectInputStream = new ObjectInputStream(byteIn);
          Object obj = objectInputStream.readObject();
          assertEquals("Streamed Object equals ", item, obj);
        } catch (IOException e) {
          assertNull("Test Serialization " + item.getClass(), e);
        } catch (ClassNotFoundException e) {
          assertNull("Test Serialization " + item.getClass(), e);
        }
      }

    /**
     * @param contents
     * @return
     */
    private String showBytes(byte[] contents) {
      StringBuilder b = new StringBuilder('[');
      for (int i = 0; i < contents.length; ++i) {
        int item = contents[i] & 0xFF;
        if (item >= 0x20 && item <= 0x7F) {
          b.append((char) item);
        } else {
          b.append('(').append(Utility.hex(item, 2)).append(')');
        }
      }
      return b.append(']').toString();
    }
    
    public static class MeasureUnitHandler implements SerializableTest.Handler
    {
        public Object[] getTestObjects()
        {
            MeasureUnit items[] = {
                    MeasureUnit.CELSIUS,
                    Currency.getInstance("EUR")               
            };
            return items;
        }

        public boolean hasSameBehavior(Object a, Object b)
        {
            MeasureUnit a1 = (MeasureUnit) a;
            MeasureUnit b1 = (MeasureUnit) b;
            return a1.getType().equals(b1.getType()) 
                    && a1.getCode().equals(b1.getCode());
        }
    }
    
    public static class GeneralMeasureFormatHandler  implements SerializableTest.Handler
    {
        public Object[] getTestObjects()
        {
            GeneralMeasureFormat items[] = {
                    GeneralMeasureFormat.getInstance(ULocale.FRANCE, FormatWidth.SHORT),
                    GeneralMeasureFormat.getInstance(ULocale.FRANCE, FormatWidth.WIDE, NumberFormat.getIntegerInstance(ULocale.CANADA_FRENCH
                            )),
            };
            return items;
        }

        public boolean hasSameBehavior(Object a, Object b)
        {
            GeneralMeasureFormat a1 = (GeneralMeasureFormat) a;
            GeneralMeasureFormat b1 = (GeneralMeasureFormat) b;
            return a1.getLocale().equals(b1.getLocale()) 
                    && a1.getLength().equals(b1.getLength())
                    // && a1.getNumberFormat().equals(b1.getNumberFormat())
                    ;
        }
    }
}
