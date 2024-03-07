// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 * ******************************************************************************
 * Copyright (C) 2007, International Business Machines Corporation and * others.
 * All Rights Reserved. *
 * ******************************************************************************
 */
package com.ibm.icu.dev.test.perf;

import java.text.ParseException;
import java.util.Locale;

/**
 * @author ajmacher
 */
public class DecimalFormatPerformanceTest extends PerfTest {
    String pattern;

    String decimalAsString;

    Number decimalAsNumber;

    com.ibm.icu.text.DecimalFormat[] icuDecimalFormat;

    java.text.DecimalFormat[] javaDecimalFormat;

    public static void main(String[] args) throws Exception {
        new DecimalFormatPerformanceTest().run(args);
    }

    protected void setup(String[] args) {
        try {
            if (args.length == 0 || args.length > 2) {
                throw new UsageException();
            }

            pattern = args[0];

            if (locale == null)
                locale = Locale.getDefault();

            icuDecimalFormat = new com.ibm.icu.text.DecimalFormat[threads];
            javaDecimalFormat = new java.text.DecimalFormat[threads];
            for (int i = 0; i < threads; i++) {
                icuDecimalFormat[i] = new com.ibm.icu.text.DecimalFormat(pattern,
                        new com.ibm.icu.text.DecimalFormatSymbols(locale));
                javaDecimalFormat[i] = new java.text.DecimalFormat(pattern,
                        new java.text.DecimalFormatSymbols(locale));
            }

            if (args.length == 2) {
                decimalAsString = args[1];
                decimalAsNumber = icuDecimalFormat[0].parse(decimalAsString);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }

    }

    PerfTest.Function TestICUConstruction() {
        return new PerfTest.Function() {
            public void call() {
                new com.ibm.icu.text.DecimalFormat(pattern,
                        new com.ibm.icu.text.DecimalFormatSymbols(locale));
            }
        };
    }

    PerfTest.Function TestJDKConstruction() {
        return new PerfTest.Function() {
            public void call() {
                new java.text.DecimalFormat(pattern, new java.text.DecimalFormatSymbols(locale));
            }
        };
    }

    PerfTest.Function TestICUParse() {
        return new PerfTest.Function() {
            public void call(int id) {
                try {
                    icuDecimalFormat[id].parse(decimalAsString);
                } catch (ParseException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e.getMessage());
                }
            }
        };
    }

    PerfTest.Function TestJDKParse() {
        return new PerfTest.Function() {
            public void call(int id) {
                try {
                    javaDecimalFormat[id].parse(decimalAsString);
                } catch (ParseException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e.getMessage());
                }
            }
        };
    }

    PerfTest.Function TestICUFormat() {
        return new PerfTest.Function() {
            public void call(int id) {
                icuDecimalFormat[id].format(decimalAsNumber);
            }
        };
    }

    PerfTest.Function TestJDKFormat() {
        return new PerfTest.Function() {
            public void call(int id) {
                javaDecimalFormat[id].format(decimalAsNumber);
            }
        };
    }
}
