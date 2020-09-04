// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2014, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.perf;


/**
 * ICU service object creation performance test cases
 */
public class ServiceObjectCreationPerf extends PerfTest {
    private static final long DEF_COUNT = 1000L;

    public static void main(String... args) throws Exception {
        new ServiceObjectCreationPerf().run(args);
    }

    PerfTest.Function TestCalendarJava() {
        return new PerfTest.Function() {
            private long n = DEF_COUNT;
            public void call() {
                for (long i = 0; i < n; i++) {
                    @SuppressWarnings("unused")
                    java.util.Calendar cal = java.util.Calendar.getInstance();
                }
            }
            public long getOperationsPerIteration() {
                return n;
            }
        };
    }

    PerfTest.Function TestCalendarICU() {
        return new PerfTest.Function() {
            private long n = DEF_COUNT;
            public void call() {
                for (long i = 0; i < n; i++) {
                    @SuppressWarnings("unused")
                    com.ibm.icu.util.Calendar cal = com.ibm.icu.util.Calendar.getInstance();
                }
            }
            public long getOperationsPerIteration() {
                return n;
            }
        };
    }

    PerfTest.Function TestTimeZoneJava() {
        return new PerfTest.Function() {
            private long n = DEF_COUNT;
            public void call() {
                for (long i = 0; i < n; i++) {
                    @SuppressWarnings("unused")
                    java.util.TimeZone tz = java.util.TimeZone.getDefault();
                }
            }
            public long getOperationsPerIteration() {
                return n;
            }
        };
    }

    PerfTest.Function TestTimeZoneICU() {
        return new PerfTest.Function() {
            private long n = DEF_COUNT;
            public void call() {
                for (long i = 0; i < n; i++) {
                    @SuppressWarnings("unused")
                    com.ibm.icu.util.TimeZone tz = com.ibm.icu.util.TimeZone.getDefault();
                }
            }
            public long getOperationsPerIteration() {
                return n;
            }
        };
    }

}
