/*
 *******************************************************************************
 * Copyright (C) 2008-2011, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.format;

import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Locale;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.TimeUnitFormat;
import com.ibm.icu.util.TimeUnit;
import com.ibm.icu.util.TimeUnitAmount;
import com.ibm.icu.util.ULocale;

/**
 * @author markdavis
 *
 */
public class TimeUnitTest extends TestFmwk {
    public static void main(String[] args) throws Exception{
        new TimeUnitTest().run(args);
    }

    public void TestBasic() {
        String[] locales = {"en", "sl", "fr", "zh", "ar", "ru", "zh_Hant"};
        for ( int locIndex = 0; locIndex < locales.length; ++locIndex ) {
            //System.out.println("locale: " + locales[locIndex]);
            Object[] formats = new Object[] {
                new TimeUnitFormat(new ULocale(locales[locIndex]), TimeUnitFormat.FULL_NAME),
                new TimeUnitFormat(new ULocale(locales[locIndex]), TimeUnitFormat.ABBREVIATED_NAME)
            };
            for (int style = TimeUnitFormat.FULL_NAME;
                 style <= TimeUnitFormat.ABBREVIATED_NAME;
                 ++style) {
                final TimeUnit[] values = TimeUnit.values();
                for (int j = 0; j < values.length; ++j) {
                    final TimeUnit timeUnit = values[j];
                    double[] tests = {0, 0.5, 1, 1.5, 2, 2.5, 3, 3.5, 5, 10, 100, 101.35};
                    for (int i = 0; i < tests.length; ++i) {
                        TimeUnitAmount source = new TimeUnitAmount(tests[i], timeUnit);
                        String formatted = ((TimeUnitFormat)formats[style]).format(source);
                        //System.out.println(formatted);
                        logln(tests[i] + " => " + formatted);
                        try {
                            TimeUnitAmount result = (TimeUnitAmount) ((TimeUnitFormat)formats[style]).parseObject(formatted);
                            if (result == null || !source.equals(result)) {
                                errln("No round trip: " + source + " => " + formatted + " => " + result);
                            }
                            // mix style parsing
                            result = (TimeUnitAmount) ((TimeUnitFormat)formats[1 - style]).parseObject(formatted);
                            if (result == null || !source.equals(result)) {
                                errln("No round trip: " + source + " => " + formatted + " => " + result);
                            }
                        } catch (ParseException e) {
                            errln(e.getMessage());
                        }
                    }
                }
            }
        }
    }

    public void TestAPI() {
        TimeUnitFormat format = new TimeUnitFormat();
        format.setLocale(new ULocale("pt_BR"));
        formatParsing(format);
        format = new TimeUnitFormat(new ULocale("de"));
        formatParsing(format);
        format = new TimeUnitFormat(new ULocale("ja"));
        format.setNumberFormat(NumberFormat.getNumberInstance(new ULocale("en")));
        formatParsing(format);

        format = new TimeUnitFormat();
        ULocale es = new ULocale("es");
        format.setNumberFormat(NumberFormat.getNumberInstance(es));
        format.setLocale(es);
        formatParsing(format);
        
        format.setLocale(new Locale("pt_BR"));
        formatParsing(format);
        format = new TimeUnitFormat(new Locale("de"));
        formatParsing(format);
        format = new TimeUnitFormat(new Locale("ja"));
        format.setNumberFormat(NumberFormat.getNumberInstance(new Locale("en")));
        formatParsing(format);
    }

    /*
     * @bug 7902
     * This tests that requests for short unit names correctly fall back 
     * to long unit names for a locale where the locale data does not 
     * provide short unit names. As of CLDR 1.9, Greek is one such language.
     */
    public void TestGreek() {
        String[] locales = {"el_GR", "el"};
        final TimeUnit[] units = TimeUnit.values();
        int[] styles = new int[] {TimeUnitFormat.FULL_NAME, TimeUnitFormat.ABBREVIATED_NAME};
        int[] numbers = new int[] {1, 7};

        String[] expected = {"1 \u03B4\u03B5\u03C5\u03C4\u03B5\u03C1\u03CC\u03BB\u03B5\u03C0\u03C4\u03BF",
                             "1 \u03BB\u03B5\u03C0\u03C4\u03CC",
                             "1 \u03CE\u03C1\u03B1",
                             "1 \u03B7\u03BC\u03AD\u03C1\u03B1",
                             "1 \u03B5\u03B2\u03B4\u03BF\u03BC\u03AC\u03B4\u03B1",
                             "1 \u03BC\u03AE\u03BD\u03B1\u03C2",
                             "1 \u03AD\u03C4\u03BF\u03C2",
                             "1 \u03B4\u03B5\u03C5\u03C4\u03B5\u03C1\u03CC\u03BB\u03B5\u03C0\u03C4\u03BF",
                             "1 \u03BB\u03B5\u03C0\u03C4\u03CC",
                             "1 \u03CE\u03C1\u03B1",
                             "1 \u03B7\u03BC\u03AD\u03C1\u03B1",
                             "1 \u03B5\u03B2\u03B4\u03BF\u03BC\u03AC\u03B4\u03B1",
                             "1 \u03BC\u03AE\u03BD\u03B1\u03C2",
                             "1 \u03AD\u03C4\u03BF\u03C2",
                             "7 \u03B4\u03B5\u03C5\u03C4\u03B5\u03C1\u03CC\u03BB\u03B5\u03C0\u03C4\u03B1",
                             "7 \u03BB\u03B5\u03C0\u03C4\u03AC",
                             "7 \u03CE\u03C1\u03B5\u03C2",
                             "7 \u03B7\u03BC\u03AD\u03C1\u03B5\u03C2",
                             "7 \u03B5\u03B2\u03B4\u03BF\u03BC\u03AC\u03B4\u03B5\u03C2",
                             "7 \u03BC\u03AE\u03BD\u03B5\u03C2",
                             "7 \u03AD\u03C4\u03B7",
                             "7 \u03B4\u03B5\u03C5\u03C4\u03B5\u03C1\u03CC\u03BB\u03B5\u03C0\u03C4\u03B1",
                             "7 \u03BB\u03B5\u03C0\u03C4\u03AC",
                             "7 \u03CE\u03C1\u03B5\u03C2",
                             "7 \u03B7\u03BC\u03AD\u03C1\u03B5\u03C2",
                             "7 \u03B5\u03B2\u03B4\u03BF\u03BC\u03AC\u03B4\u03B5\u03C2",
                             "7 \u03BC\u03AE\u03BD\u03B5\u03C2",
                             "7 \u03AD\u03C4\u03B7",
                             "1 \u03B4\u03B5\u03C5\u03C4\u03B5\u03C1\u03CC\u03BB\u03B5\u03C0\u03C4\u03BF",
                             "1 \u03BB\u03B5\u03C0\u03C4\u03CC",
                             "1 \u03CE\u03C1\u03B1",
                             "1 \u03B7\u03BC\u03AD\u03C1\u03B1",
                             "1 \u03B5\u03B2\u03B4\u03BF\u03BC\u03AC\u03B4\u03B1",
                             "1 \u03BC\u03AE\u03BD\u03B1\u03C2",
                             "1 \u03AD\u03C4\u03BF\u03C2",
                             "1 \u03B4\u03B5\u03C5\u03C4\u03B5\u03C1\u03CC\u03BB\u03B5\u03C0\u03C4\u03BF",
                             "1 \u03BB\u03B5\u03C0\u03C4\u03CC",
                             "1 \u03CE\u03C1\u03B1",
                             "1 \u03B7\u03BC\u03AD\u03C1\u03B1",
                             "1 \u03B5\u03B2\u03B4\u03BF\u03BC\u03AC\u03B4\u03B1",
                             "1 \u03BC\u03AE\u03BD\u03B1\u03C2",
                             "1 \u03AD\u03C4\u03BF\u03C2",
                             "7 \u03B4\u03B5\u03C5\u03C4\u03B5\u03C1\u03CC\u03BB\u03B5\u03C0\u03C4\u03B1",
                             "7 \u03BB\u03B5\u03C0\u03C4\u03AC",
                             "7 \u03CE\u03C1\u03B5\u03C2",
                             "7 \u03B7\u03BC\u03AD\u03C1\u03B5\u03C2",
                             "7 \u03B5\u03B2\u03B4\u03BF\u03BC\u03AC\u03B4\u03B5\u03C2",
                             "7 \u03BC\u03AE\u03BD\u03B5\u03C2",
                             "7 \u03AD\u03C4\u03B7",
                             "7 \u03B4\u03B5\u03C5\u03C4\u03B5\u03C1\u03CC\u03BB\u03B5\u03C0\u03C4\u03B1",
                             "7 \u03BB\u03B5\u03C0\u03C4\u03AC",
                             "7 \u03CE\u03C1\u03B5\u03C2",
                             "7 \u03B7\u03BC\u03AD\u03C1\u03B5\u03C2",
                             "7 \u03B5\u03B2\u03B4\u03BF\u03BC\u03AC\u03B4\u03B5\u03C2",
                             "7 \u03BC\u03AE\u03BD\u03B5\u03C2",
                             "7 \u03AD\u03C4\u03B7"};

        int counter = 0;
        TimeUnitFormat timeUnitFormat;
        TimeUnitAmount timeUnitAmount;
        String formatted;

        for ( int locIndex = 0; locIndex < locales.length; ++locIndex ) {
            for( int numIndex = 0; numIndex < numbers.length; ++numIndex ) {
                for ( int styleIndex = 0; styleIndex < styles.length; ++styleIndex ) {
                    for ( int unitIndex = 0; unitIndex < units.length; ++unitIndex ) {

                        timeUnitAmount = new TimeUnitAmount(numbers[numIndex], units[unitIndex]);
                        timeUnitFormat = new TimeUnitFormat(new ULocale(locales[locIndex]), styles[styleIndex]);
                        formatted = timeUnitFormat.format(timeUnitAmount);

                        assertEquals("formatted time string is not expected, locale: " + locales[locIndex] +
                                " style: " + styles[styleIndex] + " units: " + units[unitIndex], expected[counter], formatted);
                        ++counter;
                    }
                }
            }
        }
    }

    private void formatParsing(TimeUnitFormat format) {
        final TimeUnit[] values = TimeUnit.values();
        for (int j = 0; j < values.length; ++j) {
            final TimeUnit timeUnit = values[j];
            double[] tests = {0, 0.5, 1, 2, 3, 5};
            for (int i = 0; i < tests.length; ++i) {
                TimeUnitAmount source = new TimeUnitAmount(tests[i], timeUnit);
                String formatted = format.format(source);
                //System.out.println(formatted);
                logln(tests[i] + " => " + formatted);
                try {
                    TimeUnitAmount result = (TimeUnitAmount) format.parseObject(formatted);
                    if (result == null || !source.equals(result)) {
                        errln("No round trip: " + source + " => " + formatted + " => " + result);
                    }
                } catch (ParseException e) {
                    errln(e.getMessage());
                }
            }
        }
    }
    
    /*
     * Tests the method public TimeUnitFormat(ULocale locale, int style), public TimeUnitFormat(Locale locale, int style)
     */
    @SuppressWarnings("unused")
    public void TestTimeUnitFormat() {
        // Tests when "if (style < FULL_NAME || style >= TOTAL_STYLES)" is true
        // TOTAL_STYLES is 2
        int[] cases = { TimeUnitFormat.FULL_NAME - 1, TimeUnitFormat.FULL_NAME - 2, 2, 3 };
        for (int i = 0; i < cases.length; i++) {
            try {
                TimeUnitFormat tuf = new TimeUnitFormat(new ULocale("en_US"), cases[i]);
                errln("TimeUnitFormat(ULocale,int) was suppose to return an " + "exception for a style value of "
                        + cases[i] + "passed into the constructor.");
            } catch (Exception e) {
            }
        }
        for (int i = 0; i < cases.length; i++) {
            try {
                TimeUnitFormat tuf = new TimeUnitFormat(new Locale("en_US"), cases[i]);
                errln("TimeUnitFormat(ULocale,int) was suppose to return an " + "exception for a style value of "
                        + cases[i] + "passed into the constructor.");
            } catch (Exception e) {
            }
        }
    }
    
    /*
     * Tests the method public TimeUnitFormat setLocale(ULocale locale) public TimeUnitFormat setLocale(Locale locale)
     */
    public void TestSetLocale() {
        // Tests when "if ( locale != this.locale )" is false
        TimeUnitFormat tuf = new TimeUnitFormat(new ULocale("en_US"));
        if (!tuf.setLocale(new ULocale("en_US")).equals(tuf) && !tuf.setLocale(new Locale("en_US")).equals(tuf)) {
            errln("TimeUnitFormat.setLocale(ULocale) was suppose to "
                    + "return the same TimeUnitFormat object if the same " + "ULocale is entered as a parameter.");
        }
    }

    /*
     * Tests the method public TimeUnitFormat setNumberFormat(NumberFormat format)
     */
    public void TestSetNumberFormat() {
        TimeUnitFormat tuf = new TimeUnitFormat();

        // Tests when "if (format == this.format)" is false
        // Tests when "if ( format == null )" is false
        tuf.setNumberFormat(NumberFormat.getInstance());

        // Tests when "if (format == this.format)" is true
        if (!tuf.setNumberFormat(NumberFormat.getInstance()).equals(tuf)) {
            errln("TimeUnitFormat.setNumberFormat(NumberFormat) was suppose to "
                    + "return the same object when the same NumberFormat is passed.");
        }

        // Tests when "if ( format == null )" is true
        // Tests when "if ( locale == null )" is true
        if (!tuf.setNumberFormat(null).equals(tuf)) {
            errln("TimeUnitFormat.setNumberFormat(NumberFormat) was suppose to "
                    + "return the same object when null is passed.");
        }

        TimeUnitFormat tuf1 = new TimeUnitFormat(new ULocale("en_US"));

        // Tests when "if ( locale == null )" is false
        tuf1.setNumberFormat(NumberFormat.getInstance());
        tuf1.setNumberFormat(null);
    }
    
    /*
     * Tests the method public StringBuffer format(Object obj, ...
     */
    public void TestFormat() {
        TimeUnitFormat tuf = new TimeUnitFormat();
        try {
            tuf.format(new Integer("1"), null, null);
            errln("TimeUnitFormat.format(Object,StringBuffer,FieldPosition) "
                    + "was suppose to return an exception because the Object "
                    + "parameter was not of type TimeUnitAmount.");
        } catch (Exception e) {
        }
    }
    
    /* Tests the method private void setup() from
     * public Object parseObject(String source, ParsePosition pos)
     * 
     */
    public void TestSetup(){
        TimeUnitFormat tuf = new TimeUnitFormat();
        tuf.parseObject("", new ParsePosition(0));
        
        TimeUnitFormat tuf1 = new TimeUnitFormat();
        tuf1.setNumberFormat(NumberFormat.getInstance());
        tuf1.parseObject("", new ParsePosition(0));
    }
}
