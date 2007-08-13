/*
 * ******************************************************************************
 * Copyright (C) 2007, International Business Machines Corporation and * others.
 * All Rights Reserved. *
 * ******************************************************************************
 */
package com.ibm.icu.dev.test.perf;

import java.util.Locale;

/**
 * @author ajmacher
 */
public class DecimalFormatPerformanceTest extends PerfTest {
    String pattern;

    String decimalAsString;

    Number decimalAsNumber;
    
    com.ibm.icu.text.DecimalFormatSymbols icuDecimalFormatSymbols;
    
    com.ibm.icu.text.DecimalFormat icuDecimalFormat;
    
    java.text.DecimalFormatSymbols javaDecimalFormatSymbols;
    
    java.text.DecimalFormat javaDecimalFormat;

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

            icuDecimalFormatSymbols = new com.ibm.icu.text.DecimalFormatSymbols(locale);
            icuDecimalFormat = new com.ibm.icu.text.DecimalFormat(pattern, icuDecimalFormatSymbols);
            
            javaDecimalFormatSymbols = new java.text.DecimalFormatSymbols(locale);
            javaDecimalFormat = new java.text.DecimalFormat(pattern, javaDecimalFormatSymbols);
            
            if (args.length == 2) {
                decimalAsString = args[1];
                decimalAsNumber = icuDecimalFormat.parse(decimalAsString);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }

    }

    PerfTest.Function TestICUConstruction() {
        return new PerfTest.Function() {
            public void call() {
                try {
                    new com.ibm.icu.text.DecimalFormat(pattern, icuDecimalFormatSymbols);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException(e.getMessage());
                }
            }
        };
    }

    PerfTest.Function TestJDKConstruction() {
        return new PerfTest.Function() {
            public void call() {
                try {
                    new java.text.DecimalFormat(pattern, javaDecimalFormatSymbols);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException(e.getMessage());
                }
            }
        };
    }

    PerfTest.Function TestICUParse() {
        return new PerfTest.Function() {
            public void call() {
                try {
                    icuDecimalFormat.parse(decimalAsString);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException(e.getMessage());
                }
            }
        };
    }

    PerfTest.Function TestJDKParse() {
        return new PerfTest.Function() {
            public void call() {
                try {
                    javaDecimalFormat.parse(decimalAsString);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException(e.getMessage());
                }
            }
        };
    }

    PerfTest.Function TestICUFormat() {
        return new PerfTest.Function() {
            public void call() {
                try {
                    icuDecimalFormat.format(decimalAsNumber);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException(e.getMessage());
                }
            }
        };
    }

    PerfTest.Function TestJDKFormat() {
        return new PerfTest.Function() {
            public void call() {
                try {
                    javaDecimalFormat.format(decimalAsNumber);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException(e.getMessage());
                }
            }
        };
    }
}
