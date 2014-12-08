/*
 *******************************************************************************
 * Copyright (c) 2004-2014, International Business Machines
 * Corporation and others.  All Rights Reserved.
 * Copyright (C) 2010 , Yahoo! Inc.                                            
 *******************************************************************************
 */
package com.ibm.icu.dev.test.format;

import com.ibm.icu.dev.test.TestFmwk.TestGroup;

/**
 * Top level test used to run all other tests as a batch.
 */

public class TestAll extends TestGroup {

    public static void main(String[] args) {
        new TestAll().run(args);
    }

    public TestAll() {
        super(new String[] {
                  "TestAll$RBNF",
                  "TestAll$NumberFormat",
                  "TestAll$DateFormat",
                  "TestAll$DateIntervalFormat",
                  "TestAll$TimeUnitFormat",
                  "com.ibm.icu.dev.test.format.BigNumberFormatTest",
                  "DataDrivenFormatTest",
                  "ListFormatterTest",
                  "MeasureUnitTest",
                  "TestAll$PluralFormat",
                  "TestAll$MessageFormat",
                  "TestAll$SelectFormat"
              },
              "Formatting Tests");
    }

    public static class RBNF extends TestGroup {
        public RBNF() {
            super(new String[] {
                "RbnfTest",
                "RbnfRoundTripTest",
                "RBNFParseTest",
            });
        }
    }

    public static class NumberFormat extends TestGroup {
        public NumberFormat() {
            super(new String[] {
                "CompactDecimalFormatTest",
                "IntlTestNumberFormat",
                "IntlTestNumberFormatAPI",
                "NumberFormatSpecificationTest",
                "NumberFormatTest",
                "NumberFormatRegistrationTest",
                "NumberFormatRoundTripTest",
                "NumberRegression",
                "NumberFormatRegressionTest",
                "ScientificNumberFormatterTest",
                "IntlTestDecimalFormatAPI",
                "IntlTestDecimalFormatAPIC",
                "IntlTestDecimalFormatSymbols",
                "IntlTestDecimalFormatSymbolsC",
                "PluralRangesTest",
            });
        }
    }

    public static class DateFormat extends TestGroup {
        public DateFormat() {
            super(new String[] {
                "DateFormatMiscTests",
                "DateFormatRegressionTest",
                "DateFormatRoundTripTest",
                "DateFormatTest",
                "IntlTestDateFormat",
                "IntlTestDateFormatAPI",
                "IntlTestDateFormatAPIC",
                "IntlTestDateFormatSymbols",
                "DateTimeGeneratorTest",
                "IntlTestSimpleDateFormatAPI",
                "DateFormatRegressionTestJ",
                "TimeZoneFormatTest",
                "RelativeDateTimeFormatterTest"
            });
        }
    }
    
    public static class DateIntervalFormat extends TestGroup {
        public DateIntervalFormat() {
            super(new String[] {
                "DateIntervalFormatTest"
            });
        }
    }
    
    public static class TimeUnitFormat extends TestGroup {
        public TimeUnitFormat() {
            super(new String[] {
                "TimeUnitTest"
            });
        }
    }
    
    public static class PluralFormat extends TestGroup {
        public PluralFormat() {
            super(new String[] {
                "PluralFormatUnitTest",
                "PluralFormatTest",
                "PluralRulesTest",
                "PluralRangesTest",
            });
        }
    }

    public static class SelectFormat extends TestGroup {
        public SelectFormat() {
            super(new String[] {
                "SelectFormatUnitTest",
                "SelectFormatAPITest",
            });
        }
    }

    public static class MessageFormat extends TestGroup {
        public MessageFormat() {
            super(new String[] {
                "TestMessageFormat",
                "MessageRegression",
                "MessagePatternUtilTest",
            });
        }
    }

    public static final String CLASS_TARGET_NAME = "Format";
}
