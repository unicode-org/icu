/*
 *******************************************************************************
 * Copyright (C) 2008-2013, International Business Machines Corporation and    *
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
import com.ibm.icu.util.TimePeriod;
import com.ibm.icu.util.TimeUnit;
import com.ibm.icu.util.TimeUnitAmount;
import com.ibm.icu.util.ULocale;

/**
 * @author markdavis
 *
 */
public class TimeUnitTest extends TestFmwk {
    
    private static final TimePeriod _1h_23_5s = TimePeriod.forAmounts(
            new TimeUnitAmount(1.0, TimeUnit.HOUR),
            new TimeUnitAmount(23.5, TimeUnit.SECOND));
    private static final TimePeriod _1h_0m_23s = TimePeriod.forAmounts(
            new TimeUnitAmount(1.0, TimeUnit.HOUR),
            new TimeUnitAmount(0.0, TimeUnit.MINUTE),
            new TimeUnitAmount(23.0, TimeUnit.SECOND));
    private static final TimePeriod _2y_5M_3w_4d = TimePeriod.forAmounts(
            new TimeUnitAmount(2.0, TimeUnit.YEAR),
            new TimeUnitAmount(5.0, TimeUnit.MONTH),
            new TimeUnitAmount(3.0, TimeUnit.WEEK),
            new TimeUnitAmount(4.0, TimeUnit.DAY));
            
    public static void main(String[] args) throws Exception{
        new TimeUnitTest().run(args);
    }

    public void TestBasic() {
        String[] locales = {"en", "sl", "fr", "zh", "ar", "ru", "zh_Hant"};
        for ( int locIndex = 0; locIndex < locales.length; ++locIndex ) {
            //System.out.println("locale: " + locales[locIndex]);
            TimeUnitFormat[] formats = new TimeUnitFormat[] {
                new TimeUnitFormat(new ULocale(locales[locIndex]), TimeUnitFormat.FULL_NAME),
                new TimeUnitFormat(new ULocale(locales[locIndex]), TimeUnitFormat.ABBREVIATED_NAME),
                new TimeUnitFormat(new ULocale(locales[locIndex]), TimeUnitFormat.NUMERIC)
                
            };
            for (int style = TimeUnitFormat.FULL_NAME;
                 style <= TimeUnitFormat.NUMERIC;
                 ++style) {
                final TimeUnit[] values = TimeUnit.values();
                for (int j = 0; j < values.length; ++j) {
                    final TimeUnit timeUnit = values[j];
                    double[] tests = {0, 0.5, 1, 1.5, 2, 2.5, 3, 3.5, 5, 10, 100, 101.35};
                    for (int i = 0; i < tests.length; ++i) {
                        TimeUnitAmount source = new TimeUnitAmount(tests[i], timeUnit);
                        String formatted = formats[style].format(source);
                        //System.out.println(formatted);
                        logln(tests[i] + " => " + formatted);
                        try {
                            // Style should not matter when parsing.
                            for (int parseStyle = TimeUnitFormat.FULL_NAME; parseStyle <= TimeUnitFormat.NUMERIC; parseStyle++) {
                                TimeUnitAmount result = (TimeUnitAmount) formats[parseStyle].parseObject(formatted);
                                if (result == null || !source.equals(result)) {
                                    errln("No round trip: " + source + " => " + formatted + " => " + result);
                                }
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
        final TimeUnit[] units = new TimeUnit[]{
                TimeUnit.SECOND,
                TimeUnit.MINUTE,
                TimeUnit.HOUR,
                TimeUnit.DAY,
                TimeUnit.WEEK,
                TimeUnit.MONTH,
                TimeUnit.YEAR};
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

    /**
     * @bug9042 
     * Performs tests for Greek.
     * This tests that if the plural count listed in time unit format does not 
     * match those in the plural rules for the locale, those plural count in 
     * time unit format will be ingored and subsequently, fall back will kick in 
     * which is tested above. 
     * Without data sanitization, setNumberFormat() would crash. 
     * As of CLDR shiped in ICU4.8, Greek is one such language. 
     */ 
    public void TestGreekWithSanitization() {
        ULocale loc = new ULocale("el");
        NumberFormat numfmt = NumberFormat.getInstance(loc);
        TimeUnitFormat tuf = new TimeUnitFormat(loc);
        tuf.parseObject("", new ParsePosition(0));
        tuf.setNumberFormat(numfmt);        
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
        int[] cases = { TimeUnitFormat.FULL_NAME - 1, TimeUnitFormat.FULL_NAME - 2, 3 };
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
    
    public void TestFormatPeriodEn() {
        Object[][] fullData = {
                {_1h_23_5s, "1 hour and 23.5 seconds"},
                {_1h_0m_23s, "1 hour, 0 minutes, and 23 seconds"},
                {_2y_5M_3w_4d, "2 years, 5 months, 3 weeks, and 4 days"}};
        TimeUnitFormat tuf = new TimeUnitFormat(ULocale.ENGLISH, TimeUnitFormat.FULL_NAME);
        verifyFormatPeriod("en FULL", tuf, fullData);
    }
    
    private void verifyFormatPeriod(String desc, TimeUnitFormat tuf, Object[][] testData) {
        boolean failure = false;
        for (Object[] testCase : testData) {
            try {
                assertEquals(desc, testCase[1], tuf.formatTimePeriod((TimePeriod) testCase[0]));
            } catch (RuntimeException e) {
                logln(e.getMessage());
                failure = true;
            }
        }
        if (failure) {
            errln("Test failed.");
        }
    }
}
