/*
 * ******************************************************************************
 * Copyright (C) 2007, International Business Machines Corporation and others.  * 
 * All Rights Reserved.                                                         *
 * ******************************************************************************
 */
package com.ibm.icu.dev.test.perf;

import java.text.ParseException;
import java.util.Date;
import java.util.Locale;

/**
 * @author ajmacher
 */
public class DateFormatPerformanceTest extends PerfTest {
    private String pattern;

    private String dateString;

    private Date date;

    private com.ibm.icu.text.SimpleDateFormat[] icuDateFormat;

    private java.text.SimpleDateFormat[] jdkDateFormat;

    public static void main(String[] args) throws Exception {
        new DateFormatPerformanceTest().run(args);
    }

    protected void setup(String[] args) {
        try {
            if (args.length == 0 || args.length > 2) {
                throw new UsageException();
            }

            pattern = args[0];

            if (locale == null)
                locale = Locale.getDefault();

            icuDateFormat = new com.ibm.icu.text.SimpleDateFormat[threads];
            jdkDateFormat = new java.text.SimpleDateFormat[threads];
            for (int i = 0; i < threads; i++) {
                icuDateFormat[i] = new com.ibm.icu.text.SimpleDateFormat(pattern, locale);
                jdkDateFormat[i] = new java.text.SimpleDateFormat(pattern, locale);
            }

            if (args.length == 2) {
                dateString = args[1];
                date = icuDateFormat[0].parse(dateString);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }

    }

    PerfTest.Function TestICUConstruction() {
        return new PerfTest.Function() {
            public void call() {
                new com.ibm.icu.text.SimpleDateFormat(pattern, locale);
            }
        };
    }

    PerfTest.Function TestJDKConstruction() {
        return new PerfTest.Function() {
            public void call() {
                new java.text.SimpleDateFormat(pattern, locale);
            }
        };
    }

    PerfTest.Function TestICUParse() {
        return new PerfTest.Function() {
            public void call(int id) {
                try {
                    icuDateFormat[id].parse(dateString);
                } catch (ParseException ex) {
                    ex.printStackTrace();
                }
            }
        };
    }

    PerfTest.Function TestJDKParse() {
        return new PerfTest.Function() {
            public void call(int id) {
                try {
                    jdkDateFormat[id].parse(dateString);
                } catch (ParseException ex) {
                    ex.printStackTrace();
                }
            }
        };
    }

    PerfTest.Function TestICUFormat() {
        return new PerfTest.Function() {
            public void call(int id) {
                icuDateFormat[id].format(date);
            }
        };
    }

    PerfTest.Function TestJDKFormat() {
        return new PerfTest.Function() {
            public void call(int id) {
                jdkDateFormat[id].format(date);
            }
        };
    }
}
