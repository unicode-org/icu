/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/test/Attic/TestAll.java,v $ 
 * $Date: 2002/02/08 01:12:45 $ 
 * $Revision: 1.19 $
 *
 *****************************************************************************************
 */
package com.ibm.test;
import com.ibm.test.TestFmwk;
import java.text.*;
import java.util.*;

/**
 * Top level test used to run all other tests as a batch.
 */
 
public class TestAll extends TestFmwk {

    public static void main(String[] args) throws Exception {
        new TestAll().run(args);
    }

    public void TestBigNumberFormat() throws Exception{
        run(new com.ibm.test.bnf.BigNumberFormatTest());
    }
    
    public void TestCompression() throws Exception{
        run(new TestFmwk[] {
            new com.ibm.test.compression.DecompressionTest(),
            new com.ibm.test.compression.ExhaustiveTest()
                });
    }
    
    public void TestNormalizer() throws Exception{
        run(new TestFmwk[] {
            new com.ibm.test.normalizer.BasicTest(),
            new com.ibm.test.normalizer.ExhaustiveTest(),
            new com.ibm.test.normalizer.ConformanceTest(),
        });
    }

    public void TestRuleBasedNumberFormat() throws Exception {
        run(new TestFmwk[] {
            new com.ibm.test.rbnf.RbnfTest(),
            new com.ibm.test.rbnf.RbnfRoundTripTest()
                });
    }

    public void TestRuleBasedBreakIterator() throws Exception {
        run(new TestFmwk[] {
            new com.ibm.test.rbbi.SimpleBITest(),
            new com.ibm.test.rbbi.BreakIteratorTest(),
            new com.ibm.test.rbbi.RBBITest(),
            new com.ibm.test.rbbi.RBBIAPITest()
                });
    }

    public void TestTranslit() throws Exception {
        run(new TestFmwk[] {
            new com.ibm.test.translit.TransliteratorTest(),
            new com.ibm.test.translit.UnicodeSetTest(),
            new com.ibm.test.translit.UnicodeFilterLogicTest(),
            new com.ibm.test.translit.CompoundTransliteratorTest(),
            new com.ibm.test.translit.UnicodeToHexTransliteratorTest(),
            new com.ibm.test.translit.HexToUnicodeTransliteratorTest(),
            new com.ibm.test.translit.JamoTest(),
            new com.ibm.test.translit.ErrorTest(),
            new com.ibm.test.translit.RoundTripTest(),
            new com.ibm.test.translit.ReplaceableTest()
                });
    }

    public void TestSearch() throws Exception {
        run(new com.ibm.test.search.SearchTest());
    }
	
    public void TestRichEdit() throws Exception {
        run(new com.ibm.test.richtext.TestAll());
    }

    public void TestArabicShaping() throws Exception {
        run(new com.ibm.icu.test.text.ArabicShapingRegTest());
    }

    public void TestCalendar() throws Exception {
        run(new TestFmwk[] {
            new com.ibm.test.calendar.AstroTest(),
            new com.ibm.test.calendar.CalendarRegression(),
            new com.ibm.test.calendar.CompatibilityTest(),
            new com.ibm.test.calendar.HebrewTest(),
            new com.ibm.test.calendar.IBMCalendarTest(),
            new com.ibm.test.calendar.IslamicTest(),
            new com.ibm.test.calendar.ChineseTest()
                });
    }

    public void TestTimeZone() throws Exception {
        run(new TestFmwk[] {
            new com.ibm.test.timezone.TimeZoneTest(),
            new com.ibm.test.timezone.TimeZoneRegression(),
            new com.ibm.test.timezone.TimeZoneBoundaryTest()
                });
    }

    public void TestCharacter() throws Exception {
        run(new TestFmwk[] {
            new com.ibm.icu.test.text.UCharacterTest(),
            new com.ibm.icu.test.text.UTF16Test()
                });
    }
    
    public void TestTrie() throws Exception {
        run(new TestFmwk[] {
            new com.ibm.icu.test.util.TrieTest()
            });
    }
    
    public void TestUScript() throws Exception {
        run( new TestFmwk[] {
            new com.ibm.icu.test.text.TestUScript(),
        });
    }
    
    public void TestNumberFormat() throws Exception {
        run(new TestFmwk[] {
            new com.ibm.icu.test.format.IntlTestNumberFormat(),
            new com.ibm.icu.test.format.IntlTestNumberFormatAPI(),
            new com.ibm.icu.test.format.NumberFormatTest(),
            new com.ibm.icu.test.format.NumberFormatRoundTripTest(),
            new com.ibm.icu.test.format.NumberRegression(),
            new com.ibm.icu.test.format.NumberFormatRegressionTest(),
            new com.ibm.icu.test.format.IntlTestDecimalFormatAPI(),
            new com.ibm.icu.test.format.IntlTestDecimalFormatAPIC(),
            new com.ibm.icu.test.format.IntlTestDecimalFormatSymbols(),
            new com.ibm.icu.test.format.IntlTestDecimalFormatSymbolsC()
                });
    }
    
    public void TestDateFormat() throws Exception {
        run(new TestFmwk[] {
            new com.ibm.icu.test.format.DateFormatMiscTests(),
            new com.ibm.icu.test.format.DateFormatRegressionTest(),
            new com.ibm.icu.test.format.DateFormatRoundTripTest(),
            new com.ibm.icu.test.format.DateFormatTest(),
            new com.ibm.icu.test.format.IntlTestDateFormat(),
            new com.ibm.icu.test.format.IntlTestDateFormatAPI(),
            new com.ibm.icu.test.format.IntlTestDateFormatAPIC(),
            new com.ibm.icu.test.format.IntlTestDateFormatSymbols(),
            new com.ibm.icu.test.format.IntlTestSimpleDateFormatAPI(),
            new com.ibm.icu.test.format.DateFormatRegressionTestJ()
                });
    }
}
