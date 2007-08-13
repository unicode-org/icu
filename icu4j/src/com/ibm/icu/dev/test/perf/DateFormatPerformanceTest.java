/*
 * ******************************************************************************
 * Copyright (C) 2007, International Business Machines Corporation and * others.
 * All Rights Reserved. *
 * ******************************************************************************
 */
package com.ibm.icu.dev.test.perf;

import java.util.Date;
import java.util.Locale;

/**
 * @author ajmacher
 */
public class DateFormatPerformanceTest extends PerfTest {
    String pattern;

    String dateString;

    Date date;
    
    com.ibm.icu.text.SimpleDateFormat icuSDF;
    
    java.text.SimpleDateFormat javaSDF;

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

            icuSDF = new com.ibm.icu.text.SimpleDateFormat(pattern, locale);
            javaSDF = new java.text.SimpleDateFormat(pattern, locale);
            
            if (args.length == 2) {
                dateString = args[1];
                date = icuSDF.parse(dateString);
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
                    new com.ibm.icu.text.SimpleDateFormat(pattern, locale);
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
                    new java.text.SimpleDateFormat(pattern, locale);
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
                    icuSDF.parse(dateString).getTime();
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
                    javaSDF.parse(dateString).getTime();
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
                    icuSDF.format(date);
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
                    javaSDF.format(date);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException(e.getMessage());
                }
            }
        };
    }
}
