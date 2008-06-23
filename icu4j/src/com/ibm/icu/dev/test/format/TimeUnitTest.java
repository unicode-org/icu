/*
 *******************************************************************************
 * Copyright (C) 2008, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.format;

import java.text.ParseException;

import com.ibm.icu.dev.test.TestFmwk;
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
        TimeUnitFormat format = new TimeUnitFormat();
        //String[] locales = {"en", "sl", "fr", "zh", "ar"};
        String[] locales = {"ar", "ru", "en", "sl", "fr", "zh"};
        for ( int locIndex = 0; locIndex < locales.length; ++locIndex ) {
            format.setLocale(new ULocale(locales[locIndex]));
            //System.out.println(locales[locIndex]);
            final TimeUnit[] values = TimeUnit.values();
            for (int j = 0; j < values.length; ++j) {
                final TimeUnit timeUnit = values[j];
                double[] tests = {0, 0.5, 1, 1.5, 2, 2.5, 3, 3.5, 5, 10, 100, 101.35};
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
    }
}
