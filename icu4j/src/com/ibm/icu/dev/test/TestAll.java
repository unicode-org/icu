/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/TestAll.java,v $
 * $Date: 2003/02/01 00:51:29 $
 * $Revision: 1.46 $
 *
 *****************************************************************************************
 */
package com.ibm.icu.dev.test;

import java.util.Hashtable;
import java.util.TimeZone;

/**
 * Top level test used to run all other tests as a batch.
 */

public class TestAll extends TestFmwk {

    public static void main(String[] args) throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("PST"));
        TestAll ta = new TestAll();
        ta.init();
        ta.run(args);
    }
    
    // This method creates a map of module names vs Test names
    private void init(){
        Hashtable hashtable = new Hashtable();
        // set up the map
        hashtable.put("normalizer","TestNormalizer");
        hashtable.put("collator",        new String[]{  "TestCollator",
                                                        "TestSearch"});
        hashtable.put("propertiesBasic", new String[]{  "TestCharacter", 
                                                        "TestUScript", 
                                                        "TestUScriptRun"});
        hashtable.put("propertiesFull", new String[]{   "TestCharacter", 
                                                        "TestUScript", 
                                                        "TestUScriptRun"});
        hashtable.put("compression", "TestComparession");
        hashtable.put("calendar", "TestCalendar");
        hashtable.put("transliterator", "TestTranslit");
        hashtable.put("format",   new String[]{ "TestBigNumberFormat",
                                                "TestRuleBasedNumberFormat",
                                                "TestNumberFormat",
                                                "TestDateFormat"});
        hashtable.put("breakIterator","TestRuleBasedBreakIterator");
        
        setModuleHash(hashtable);    
    }
    
    public void TestBigNumberFormat() throws Exception{
        run(getObject("com.ibm.icu.dev.test.format.BigNumberFormatTest"));
    }

    public void TestCompression() throws Exception{
        run(new TestFmwk[] {
            getObject("com.ibm.icu.dev.test.compression.DecompressionTest"),
            getObject("com.ibm.icu.dev.test.compression.ExhaustiveTest")
                });
    }

    public void TestRuleBasedNumberFormat() throws Exception {
        run(new TestFmwk[] {
            getObject("com.ibm.icu.dev.test.format.RbnfTest"),
            getObject("com.ibm.icu.dev.test.format.RbnfRoundTripTest")
                });
    }

    public void TestRuleBasedBreakIterator() throws Exception {
        run(new TestFmwk[] {
            getObject("com.ibm.icu.dev.test.rbbi.SimpleBITest"),
            getObject("com.ibm.icu.dev.test.rbbi.BreakIteratorTest"),
            getObject("com.ibm.icu.dev.test.rbbi.RBBITest"),
            getObject("com.ibm.icu.dev.test.rbbi.RBBIAPITest")
                });
    }

    public void TestTranslit() throws Exception {
        run(new TestFmwk[] {
            getObject("com.ibm.icu.dev.test.translit.TransliteratorTest"),
            getObject("com.ibm.icu.dev.test.translit.UnicodeSetTest"),
            getObject("com.ibm.icu.dev.test.translit.UnicodeFilterLogicTest"),
            getObject("com.ibm.icu.dev.test.translit.CompoundTransliteratorTest"),
            getObject("com.ibm.icu.dev.test.translit.UnicodeToHexTransliteratorTest"),
            getObject("com.ibm.icu.dev.test.translit.HexToUnicodeTransliteratorTest"),
            getObject("com.ibm.icu.dev.test.translit.JamoTest"),
            getObject("com.ibm.icu.dev.test.translit.ErrorTest"),
            getObject("com.ibm.icu.dev.test.translit.RoundTripTest"),
            getObject("com.ibm.icu.dev.test.translit.ReplaceableTest")
                });
    }

    public void TestSearch() throws Exception {
        run(
            getObject("com.ibm.icu.dev.test.search.SearchTest"));
    }

    public void TestCollator() throws Exception {
        run(new TestFmwk[] {
            getObject("com.ibm.icu.dev.test.collator.CollationTest"),
            getObject("com.ibm.icu.dev.test.collator.CollationAPITest"),
            getObject("com.ibm.icu.dev.test.collator.CollationCurrencyTest"),
            getObject("com.ibm.icu.dev.test.collator.CollationDanishTest"),
            getObject("com.ibm.icu.dev.test.collator.CollationDummyTest"),
            getObject("com.ibm.icu.dev.test.collator.CollationEnglishTest"),
            getObject("com.ibm.icu.dev.test.collator.CollationFinnishTest"),
            getObject("com.ibm.icu.dev.test.collator.CollationFrenchTest"),
            getObject("com.ibm.icu.dev.test.collator.CollationGermanTest"),
            getObject("com.ibm.icu.dev.test.collator.CollationIteratorTest"),
            getObject("com.ibm.icu.dev.test.collator.CollationKanaTest"),
            getObject("com.ibm.icu.dev.test.collator.CollationMonkeyTest"),
            getObject("com.ibm.icu.dev.test.collator.CollationRegressionTest"),
            getObject("com.ibm.icu.dev.test.collator.CollationSpanishTest"),
            getObject("com.ibm.icu.dev.test.collator.CollationThaiTest"),
            getObject("com.ibm.icu.dev.test.collator.CollationTurkishTest"),
            getObject("com.ibm.icu.dev.test.collator.G7CollationTest"),
            getObject("com.ibm.icu.dev.test.collator.LotusCollationKoreanTest"),
            getObject("com.ibm.icu.dev.test.collator.CollationMiscTest")
                });
    }

    public void TestArabicShaping() throws Exception {
        run(getObject("com.ibm.icu.dev.test.shaping.ArabicShapingRegTest"));
    }

    public void TestCalendar() throws Exception {
        run(new TestFmwk[] {
            getObject("com.ibm.icu.dev.test.calendar.AstroTest"),
            getObject("com.ibm.icu.dev.test.calendar.CalendarRegression"),
            getObject("com.ibm.icu.dev.test.calendar.CompatibilityTest"),
            getObject("com.ibm.icu.dev.test.calendar.HebrewTest"),
            getObject("com.ibm.icu.dev.test.calendar.IBMCalendarTest"),
            getObject("com.ibm.icu.dev.test.calendar.IslamicTest"),
            getObject("com.ibm.icu.dev.test.calendar.JapaneseTest"),
            getObject("com.ibm.icu.dev.test.calendar.ChineseTest"),
            getObject("com.ibm.icu.dev.test.calendar.HolidayTest")
                });
    }

    public void TestTimeZone() throws Exception {
        run(new TestFmwk[] {
            getObject("com.ibm.icu.dev.test.timezone.TimeZoneTest"),
            getObject("com.ibm.icu.dev.test.timezone.TimeZoneRegression"),
            getObject("com.ibm.icu.dev.test.timezone.TimeZoneBoundaryTest")
                });
    }

    public void TestCharacter() throws Exception {
        run(new TestFmwk[] {
            getObject("com.ibm.icu.dev.test.lang.UCharacterTest"),
            getObject("com.ibm.icu.dev.test.lang.UCharacterCaseTest"),
            getObject("com.ibm.icu.dev.test.lang.UCharacterIteratorTest"),
            getObject("com.ibm.icu.dev.test.lang.UCharacterCategoryTest"),
            getObject("com.ibm.icu.dev.test.lang.UCharacterDirectionTest"),
            getObject("com.ibm.icu.dev.test.lang.UPropertyAliasesTest"),
            getObject("com.ibm.icu.dev.test.lang.UTF16Test")
                });
    }

    public void TestTrie() throws Exception {
        run(new TestFmwk[] {
            getObject("com.ibm.icu.dev.test.util.TrieTest")
            });
    }

    public void TestUScript() throws Exception {
        run( new TestFmwk[] {
            getObject("com.ibm.icu.dev.test.lang.TestUScript"),
        });
    }

    public void TestNormalizer() throws Exception {
        run( new TestFmwk[] {
            getObject("com.ibm.icu.dev.test.normalizer.BasicTest"),
            getObject("com.ibm.icu.dev.test.normalizer.ConformanceTest"),
            getObject("com.ibm.icu.dev.test.normalizer.ExhaustiveTest"),
            getObject("com.ibm.icu.dev.test.normalizer.TestCanonicalIterator"),
            getObject("com.ibm.icu.dev.test.normalizer.NormalizationMonkeyTest"),

        });
    }

    public void TestUScriptRun() throws Exception {
        run( new TestFmwk[] {
            getObject("com.ibm.icu.dev.test.lang.TestUScriptRun"),
        });
    }

    public void TestNumberFormat() throws Exception {
        run(new TestFmwk[] {
            getObject("com.ibm.icu.dev.test.format.IntlTestNumberFormat"),
            getObject("com.ibm.icu.dev.test.format.IntlTestNumberFormatAPI"),
            getObject("com.ibm.icu.dev.test.format.NumberFormatTest"),
            getObject("com.ibm.icu.dev.test.format.NumberFormatRoundTripTest"),
            getObject("com.ibm.icu.dev.test.format.NumberRegression"),
            getObject("com.ibm.icu.dev.test.format.NumberFormatRegressionTest"),
            getObject("com.ibm.icu.dev.test.format.IntlTestDecimalFormatAPI"),
            getObject("com.ibm.icu.dev.test.format.IntlTestDecimalFormatAPIC"),
            getObject("com.ibm.icu.dev.test.format.IntlTestDecimalFormatSymbols"),
            getObject("com.ibm.icu.dev.test.format.IntlTestDecimalFormatSymbolsC")
                });
    }

    public void TestDateFormat() throws Exception {
        run(new TestFmwk[] {
            getObject("com.ibm.icu.dev.test.format.DateFormatMiscTests"),
            getObject("com.ibm.icu.dev.test.format.DateFormatRegressionTest"),
            getObject("com.ibm.icu.dev.test.format.DateFormatRoundTripTest"),
            getObject("com.ibm.icu.dev.test.format.DateFormatTest"),
            getObject("com.ibm.icu.dev.test.format.IntlTestDateFormat"),
            getObject("com.ibm.icu.dev.test.format.IntlTestDateFormatAPI"),
            getObject("com.ibm.icu.dev.test.format.IntlTestDateFormatAPIC"),
            getObject("com.ibm.icu.dev.test.format.IntlTestDateFormatSymbols"),
            getObject("com.ibm.icu.dev.test.format.IntlTestSimpleDateFormatAPI"),
            getObject("com.ibm.icu.dev.test.format.DateFormatRegressionTestJ")
                });
    }

    public void TestService() throws Exception {
	run(new TestFmwk[] {
	    getObject("com.ibm.icu.dev.test.util.ICUServiceTest"),
	    getObject("com.ibm.icu.dev.test.util.ICUServiceThreadTest")
		});

    }

    public void TestVersionInfo() throws Exception {
        run(new TestFmwk[] {
            getObject("com.ibm.icu.dev.test.util.VersionInfoTest")
        });        

    }

    public void TestIterator() throws Exception {
        run(new TestFmwk[] {
            getObject("com.ibm.icu.dev.test.iterator.TestUCharacterIterator"),
        });
    }

    public void TestUtil() throws Exception {
        run(new TestFmwk[] {
            getObject("com.ibm.icu.dev.test.util.ICUListResourceBundleTest"),
    	    getObject("com.ibm.icu.dev.test.util.CompactArrayTest"),
            getObject("com.ibm.icu.dev.test.util.StringTokenizerTest"),
            getObject("com.ibm.icu.dev.test.util.CurrencyTest"),

            });
    }
    public void TestMath() throws Exception{
        run( new TestFmwk[]{
                getObject("com.ibm.icu.dev.test.bigdec.DiagBigDecimal"),
              });
    }
    public void TestICUBinary() throws Exception{
        run( new TestFmwk[]{
                getObject("com.ibm.icu.dev.test.util.ICUBinaryTest"),
              });
    }
}
