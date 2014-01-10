/*
 *******************************************************************************
 * Copyright (C) 2013-2014, International Business Machines Corporation and         *
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.test.serializable.SerializableTest;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.MeasureFormat;
import com.ibm.icu.text.MeasureFormat.FormatWidth;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.TimeUnitFormat;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.Measure;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.TimeUnit;
import com.ibm.icu.util.TimeUnitAmount;
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
    
    public void TestFormatPeriodEn() {
        TimeUnitAmount[] _19m = {new TimeUnitAmount(19.0, TimeUnit.MINUTE)};
        TimeUnitAmount[] _1h_23_5s = {
                new TimeUnitAmount(1.0, TimeUnit.HOUR),
                new TimeUnitAmount(23.5, TimeUnit.SECOND)};
        TimeUnitAmount[] _1h_23_5m = {
                new TimeUnitAmount(1.0, TimeUnit.HOUR),
                new TimeUnitAmount(23.5, TimeUnit.MINUTE)};
        TimeUnitAmount[] _1h_0m_23s = {
                new TimeUnitAmount(1.0, TimeUnit.HOUR),
                new TimeUnitAmount(0.0, TimeUnit.MINUTE),
                new TimeUnitAmount(23.0, TimeUnit.SECOND)};
        TimeUnitAmount[] _2y_5M_3w_4d = {
                new TimeUnitAmount(2.0, TimeUnit.YEAR),
                new TimeUnitAmount(5.0, TimeUnit.MONTH),
                new TimeUnitAmount(3.0, TimeUnit.WEEK),
                new TimeUnitAmount(4.0, TimeUnit.DAY)};
        TimeUnitAmount[] _1m_59_9996s = {
                new TimeUnitAmount(1.0, TimeUnit.MINUTE),
                new TimeUnitAmount(59.9996, TimeUnit.SECOND)};
        TimeUnitAmount[] _5h_17m = {
                new TimeUnitAmount(5.0, TimeUnit.HOUR),
                new TimeUnitAmount(17.0, TimeUnit.MINUTE)};
        TimeUnitAmount[] _19m_28s = {
                new TimeUnitAmount(19.0, TimeUnit.MINUTE),
                new TimeUnitAmount(28.0, TimeUnit.SECOND)};
        TimeUnitAmount[] _0h_0m_17s = {
                new TimeUnitAmount(0.0, TimeUnit.HOUR),
                new TimeUnitAmount(0.0, TimeUnit.MINUTE),
                new TimeUnitAmount(17.0, TimeUnit.SECOND)};
        TimeUnitAmount[] _6h_56_92m = {
                new TimeUnitAmount(6.0, TimeUnit.HOUR),
                new TimeUnitAmount(56.92, TimeUnit.MINUTE)};
        TimeUnitAmount[] _3h_5h = {
                new TimeUnitAmount(3.0, TimeUnit.HOUR),
                new TimeUnitAmount(5.0, TimeUnit.HOUR)};
        
        Object[][] fullData = {
                {_1m_59_9996s, "1 minute, 59.9996 seconds"},
                {_19m, "19 minutes"},
                {_1h_23_5s, "1 hour, 23.5 seconds"},
                {_1h_23_5m, "1 hour, 23.5 minutes"},
                {_1h_0m_23s, "1 hour, 0 minutes, 23 seconds"},
                {_2y_5M_3w_4d, "2 years, 5 months, 3 weeks, 4 days"}};
        Object[][] abbrevData = {
                {_1m_59_9996s, "1 min, 59.9996 secs"},
                {_19m, "19 mins"},
                {_1h_23_5s, "1 hr, 23.5 secs"},
                {_1h_23_5m, "1 hr, 23.5 mins"},
                {_1h_0m_23s, "1 hr, 0 mins, 23 secs"},
                {_2y_5M_3w_4d, "2 yrs, 5 mths, 3 wks, 4 days"}};
        Object[][] narrowData = {
                {_1m_59_9996s, "1 min, 59.9996 secs"},
                {_19m, "19 mins"},
                {_1h_23_5s, "1 hr, 23.5 secs"},
                {_1h_23_5m, "1 hr, 23.5 mins"},
                {_1h_0m_23s, "1 hr, 0 mins, 23 secs"},
                {_2y_5M_3w_4d, "2 yrs, 5 mths, 3 wks, 4 days"}};
        
        
        Object[][] numericData = {
                {_1m_59_9996s, "1:59.9996"},
                {_19m, "19m"},
                {_1h_23_5s, "1:00:23.5"},
                {_1h_0m_23s, "1:00:23"},
                {_1h_23_5m, "1:23.5"},
                {_5h_17m, "5:17"},
                {_19m_28s, "19:28"},
                {_2y_5M_3w_4d, "2y, 5m, 3w, 4d"},
                {_0h_0m_17s, "0:00:17"},
                {_6h_56_92m, "6:56.92"},
                {_3h_5h, "3h, 5h"}};
        
        NumberFormat nf = NumberFormat.getNumberInstance(ULocale.ENGLISH);
        nf.setMaximumFractionDigits(4);
        MeasureFormat mf = MeasureFormat.getInstance(ULocale.ENGLISH, FormatWidth.WIDE, nf);
        verifyFormatPeriod("en FULL", mf, fullData);
        mf = MeasureFormat.getInstance(ULocale.ENGLISH, FormatWidth.SHORT, nf);
        verifyFormatPeriod("en SHORT", mf, abbrevData);
        mf = MeasureFormat.getInstance(ULocale.ENGLISH, FormatWidth.NUMERIC, nf);
        verifyFormatPeriod("en NUMERIC", mf, numericData);   
    }
    

    
    private void verifyFormatPeriod(String desc, MeasureFormat mf, Object[][] testData) {
        StringBuilder builder = new StringBuilder();
        boolean failure = false;
        for (Object[] testCase : testData) {
            String actual = mf.format((Measure[]) testCase[0]);
            if (!testCase[1].equals(actual)) {
                builder.append(String.format("%s: Expected: '%s', got: '%s'\n", desc, testCase[1], actual));
                failure = true;
            }
        }
        if (failure) {
            errln(builder.toString());
        }
    }

    public void testAUnit() {
        String lastType = null;
        for (MeasureUnit expected : MeasureUnit.getAvailable()) {
            String type = expected.getType();
            String code = expected.getSubtype();
            if (!type.equals(lastType)) {
                logln(type);
                lastType = type;
            }
            MeasureUnit actual = MeasureUnit.internalGetInstance(type, code);
            assertSame("Identity check", expected, actual);
        }
    }
    
    public void testFormatSingleArg() {
        MeasureFormat mf = MeasureFormat.getInstance(ULocale.ENGLISH, FormatWidth.WIDE);
        assertEquals("", "5 meters", mf.format(new Measure(5, MeasureUnit.METER)));
    }
    
    public void testFormatMeasuresZeroArg() {
        MeasureFormat mf = MeasureFormat.getInstance(ULocale.ENGLISH, FormatWidth.WIDE);
        assertEquals("", "", mf.formatMeasures());
    }
    
    public void testFormatMeasuresOneArg() {
        MeasureFormat mf = MeasureFormat.getInstance(ULocale.ENGLISH, FormatWidth.WIDE);
        assertEquals("", "5 meters", mf.formatMeasures(new Measure(5, MeasureUnit.METER)));
    }
    
    

    public void testMultiples() {
        ULocale russia = new ULocale("ru");
        Object[][] data = new Object[][] {
                {ULocale.ENGLISH, FormatWidth.WIDE, "2 miles, 1 foot, 2.3 inches"},
                {ULocale.ENGLISH, FormatWidth.SHORT, "2 mi, 1 ft, 2.3 in"},
                {ULocale.ENGLISH, FormatWidth.NARROW, "2mi, 1′, 2.3″"},
                {russia, FormatWidth.WIDE, "2 мили, 1 фут и 2,3 дюйма"},
                {russia, FormatWidth.SHORT, "2 мили 1 фут 2,3 дюйма"},
                {russia, FormatWidth.NARROW, "2 мили 1 фут 2,3 дюйма"},
        };
        for (Object[] row : data) {
            MeasureFormat mf = MeasureFormat.getInstance(
                    (ULocale) row[0], (FormatWidth) row[1]);
            assertEquals(
                    "testMultiples",
                    row[2],
                    mf.formatMeasures(
                            new Measure(2, MeasureUnit.MILE), 
                            new Measure(1, MeasureUnit.FOOT), 
                            new Measure(2.3, MeasureUnit.INCH)));
        }
    }

    public void testGram() {
        MeasureFormat mf = MeasureFormat.getInstance(ULocale.ENGLISH, FormatWidth.SHORT);
        assertEquals(
                "testGram",
                "1 g",
                mf.format(new Measure(1, MeasureUnit.GRAM)));
        assertEquals(
                "testGram",
                "1 G",
                mf.format(new Measure(1, MeasureUnit.G_FORCE)));
    }

    public void testExamples() {
        MeasureFormat fmtFr = MeasureFormat.getInstance(ULocale.FRENCH, FormatWidth.SHORT);
        Measure measure = new Measure(23, MeasureUnit.CELSIUS);
        assertEquals("", "23 °C", fmtFr.format(measure));

        Measure measureF = new Measure(70, MeasureUnit.FAHRENHEIT);
        assertEquals("", "70 °F", fmtFr.format(measureF));

        MeasureFormat fmtFrFull = MeasureFormat.getInstance(ULocale.FRENCH, FormatWidth.WIDE);
        if (!logKnownIssue("8474", "needs latest CLDR data")) {
            assertEquals("", "70 pieds, 5,3 pouces", fmtFrFull.formatMeasures(new Measure(70, MeasureUnit.FOOT),
                    new Measure(5.3, MeasureUnit.INCH)));
            assertEquals("", "1 pied, 1 pouce", fmtFrFull.formatMeasures(new Measure(1, MeasureUnit.FOOT),
                    new Measure(1, MeasureUnit.INCH)));
        }
        // Degenerate case
        MeasureFormat fmtEn = MeasureFormat.getInstance(ULocale.ENGLISH, FormatWidth.WIDE);
        assertEquals("", "1 inch, 2 feet", fmtEn.formatMeasures(new Measure(1, MeasureUnit.INCH),
                new Measure(2, MeasureUnit.FOOT)));
    }
    
    public void testFieldPosition() {
        MeasureFormat fmt = MeasureFormat.getInstance(
                ULocale.ENGLISH, FormatWidth.SHORT);
        FieldPosition pos = new FieldPosition(NumberFormat.Field.DECIMAL_SEPARATOR);
        fmt.format(new Measure(43.5, MeasureUnit.FOOT), new StringBuffer(), pos);
        assertEquals("beginIndex", 2, pos.getBeginIndex());
        assertEquals("endIndex", 3, pos.getEndIndex());
        
        pos = new FieldPosition(NumberFormat.Field.DECIMAL_SEPARATOR);
        fmt.format(new Measure(43, MeasureUnit.FOOT), new StringBuffer(), pos);
        assertEquals("beginIndex", 0, pos.getBeginIndex());
        assertEquals("endIndex", 0, pos.getEndIndex());
    }
    
    public void testFieldPositionMultiple() {
        MeasureFormat fmt = MeasureFormat.getInstance(
                ULocale.ENGLISH, FormatWidth.SHORT);
        FieldPosition pos = new FieldPosition(NumberFormat.Field.INTEGER);
        String result = fmt.formatMeasures(
                new StringBuilder(),
                pos,
                new Measure(354, MeasureUnit.METER),
                new Measure(23, MeasureUnit.CENTIMETER)).toString();
        assertEquals("result", "354 m, 23 cm", result);
        
        // According to javadocs for {@link Format#format} FieldPosition is set to
        // beginning and end of first such field encountered instead of the last
        // such field encountered.
        assertEquals("beginIndex", 0, pos.getBeginIndex());
        assertEquals("endIndex", 3, pos.getEndIndex());
        
        pos = new FieldPosition(NumberFormat.Field.DECIMAL_SEPARATOR);
        result = fmt.formatMeasures(
                new StringBuilder(),
                pos,
                new Measure(354, MeasureUnit.METER),
                new Measure(23, MeasureUnit.CENTIMETER),
                new Measure(5.4, MeasureUnit.MILLIMETER)).toString();
        assertEquals("result", "354 m, 23 cm, 5.4 mm", result);
        assertEquals("beginIndex", 15, pos.getBeginIndex());
        assertEquals("endIndex", 16, pos.getEndIndex());
        
        result = fmt.formatMeasures(
                new StringBuilder(),
                pos,
                new Measure(3, MeasureUnit.METER),
                new Measure(23, MeasureUnit.CENTIMETER),
                new Measure(5.4, MeasureUnit.MILLIMETER)).toString();
        assertEquals("result", "3 m, 23 cm, 5.4 mm", result);
        assertEquals("beginIndex", 13, pos.getBeginIndex());
        assertEquals("endIndex", 14, pos.getEndIndex());
        
        pos = new FieldPosition(NumberFormat.Field.DECIMAL_SEPARATOR);
        result = fmt.formatMeasures(
                new StringBuilder(),
                pos,
                new Measure(3, MeasureUnit.METER),
                new Measure(23, MeasureUnit.CENTIMETER),
                new Measure(5, MeasureUnit.MILLIMETER)).toString();
        assertEquals("result", "3 m, 23 cm, 5 mm", result);
        assertEquals("beginIndex", 0, pos.getBeginIndex());
        assertEquals("endIndex", 0, pos.getEndIndex());
        
    }
    
    public void testOldFormatWithList() {
        List<Measure> measures = new ArrayList<Measure>(2);
        measures.add(new Measure(5, MeasureUnit.ACRE));
        measures.add(new Measure(3000, MeasureUnit.SQUARE_FOOT));
        MeasureFormat fmt = MeasureFormat.getInstance(
                ULocale.ENGLISH, FormatWidth.WIDE);
        assertEquals("", "5 acres, 3,000 square feet", fmt.format(measures));
        assertEquals("", "5 acres", fmt.format(measures.subList(0, 1)));
        List<String> badList = new ArrayList<String>();
        badList.add("be");
        badList.add("you");
        try {
            fmt.format(badList);
            fail("Expected IllegalArgumentException.");
        } catch (IllegalArgumentException expected) {
           // Expected 
        }
    }
    
    public void testOldFormatWithArray() {
        Measure[] measures = new Measure[] {
                new Measure(5, MeasureUnit.ACRE),
                new Measure(3000, MeasureUnit.SQUARE_FOOT),  
        };
        MeasureFormat fmt = MeasureFormat.getInstance(
                ULocale.ENGLISH, FormatWidth.WIDE);
        assertEquals("", "5 acres, 3,000 square feet", fmt.format(measures));
    }
    
    public void testOldFormatBadArg() {
        MeasureFormat fmt = MeasureFormat.getInstance(
                ULocale.ENGLISH, FormatWidth.WIDE);
        try {
            fmt.format("be");
            fail("Expected IllegalArgumentExceptino.");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }
    
    public void testEqHashCode() {
        MeasureFormat mf = MeasureFormat.getInstance(ULocale.CANADA, FormatWidth.SHORT);
        MeasureFormat mfeq = MeasureFormat.getInstance(ULocale.CANADA, FormatWidth.SHORT);
        MeasureFormat mfne = MeasureFormat.getInstance(ULocale.CANADA, FormatWidth.WIDE);
        MeasureFormat mfne2 = MeasureFormat.getInstance(ULocale.ENGLISH, FormatWidth.SHORT);
        verifyEqualsHashCode(mf, mfeq, mfne);
        verifyEqualsHashCode(mf, mfeq, mfne2);
    }
    
    public void testGetLocale() {
        MeasureFormat mf = MeasureFormat.getInstance(ULocale.GERMAN, FormatWidth.SHORT);
        assertEquals("", ULocale.GERMAN, mf.getLocale(ULocale.VALID_LOCALE));
    }

    static void generateConstants() {
        System.out.println("static final MeasureUnit");
        Map<String, MeasureUnit> seen = new HashMap<String, MeasureUnit>();
        boolean first = true;
        for (String type : new TreeSet<String>(MeasureUnit.getAvailableTypes())) {
            for (MeasureUnit unit : MeasureUnit.getAvailable(type)) {
                String code = unit.getSubtype();
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
        checkStreamingEquality(MeasureFormat.getInstance(ULocale.FRANCE, FormatWidth.NARROW));
        checkStreamingEquality(Currency.getInstance("EUR"));
        checkStreamingEquality(MeasureFormat.getInstance(ULocale.GERMAN, FormatWidth.SHORT));
        checkStreamingEquality(MeasureFormat.getCurrencyFormat(ULocale.ITALIAN));
    }
    
    public void TestSerialFormatWidthEnum() {
        // FormatWidth enum values must map to the same ordinal values for all time in order for
        // serialization to work.
        assertEquals("FormatWidth.WIDE", 0, FormatWidth.WIDE.ordinal());
        assertEquals("FormatWidth.SHORT", 1, FormatWidth.SHORT.ordinal());
        assertEquals("FormatWidth.NARROW", 2, FormatWidth.NARROW.ordinal());
        assertEquals("FormatWidth.NUMERIC", 3, FormatWidth.NUMERIC.ordinal());
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
          e.printStackTrace();
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
    
    private void verifyEqualsHashCode(Object o, Object eq, Object ne) {
        assertEquals("verifyEqualsHashCodeSame", o, o);
        assertEquals("verifyEqualsHashCodeEq", o, eq);
        assertNotEquals("verifyEqualsHashCodeNe", o, ne);
        assertNotEquals("verifyEqualsHashCodeEqTrans", eq, ne);
        assertEquals("verifyEqualsHashCodeHashEq", o.hashCode(), eq.hashCode());
        
        // May be a flaky test, but generally should be true.
        // May need to comment this out later.
        assertNotEquals("verifyEqualsHashCodeHashNe", o.hashCode(), ne.hashCode());
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
                    && a1.getSubtype().equals(b1.getSubtype());
        }
    }
   
    public static class MeasureFormatHandler  implements SerializableTest.Handler
    {
        public Object[] getTestObjects()
        {
            MeasureFormat items[] = {
                    MeasureFormat.getInstance(ULocale.FRANCE, FormatWidth.SHORT),
                    MeasureFormat.getInstance(
                            ULocale.FRANCE,
                            FormatWidth.WIDE,
                            NumberFormat.getIntegerInstance(ULocale.CANADA_FRENCH)),
            };
            return items;
        }
        public boolean hasSameBehavior(Object a, Object b)
        {
            MeasureFormat a1 = (MeasureFormat) a;
            MeasureFormat b1 = (MeasureFormat) b;
            return a1.getLocale().equals(b1.getLocale())
                    && a1.getWidth().equals(b1.getWidth())
                    && a1.getNumberFormat().equals(b1.getNumberFormat())
                    ;
        }
    }
}
