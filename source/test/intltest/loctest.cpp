/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-2003, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

#include "loctest.h"
#include <stdio.h>
#include <string.h>
#include <cstring.h>

#include "unicode/decimfmt.h"
//#include "unicode/datefmt.h"
#include "unicode/smpdtfmt.h"

const char* rawData[27][7] = {

        // language code
        {   "en",   "fr",   "hr",   "el",   "no",   "it",   "xx"    },
        // country code
        {   "US",   "FR",   "HR",   "GR",   "NO",   "",     "YY"    },
        // variant code
        {   "",     "",     "",     "",     "NY",   "",     ""    },
        // full name
        {   "en_US",    "fr_FR",    "hr_HR",    "el_GR",    "no_NO_NY", "it",   "xx_YY"  },
        // ISO-3 language
        {   "eng",  "fra",  "hrv",  "ell",  "nor",  "ita",  ""   },
        // ISO-3 country
        {   "USA",  "FRA",  "HRV",  "GRC",  "NOR",  "",     ""   },
        // LCID (not currently public)
        {   "409", "40c", "41a", "408", "814", "",     ""  },

        // display langage (English)
        {   "English",  "French",   "Croatian", "Greek",    "Norwegian",    "Italian",  "xx" },
        // display country (English)
        {   "United States",    "France",   "Croatia",  "Greece",   "Norway",   "",     "YY" },
        // display variant (English)
        {   "",     "",     "",     "",     "Nynorsk",   "",     ""},
        //{   "",     "",     "",     "",     "NY",   "",     ""},
        // display name (English)
        // Updated no_NO_NY English display name for new pattern-based algorithm
        // (part of Euro support).
        {   "English (United States)", "French (France)", "Croatian (Croatia)", "Greek (Greece)", "Norwegian (Norway, Nynorsk)", "Italian", "xx (YY)" },
        //{   "English (United States)", "French (France)", "Croatian (Croatia)", "Greek (Greece)", "Norwegian (Norway,NY)", "Italian", "xx (YY)" },

        // display langage (French)
        {   "anglais",  "fran\\u00E7ais",   "croate", "grec",    "norv\\u00E9gien",    "italien", "xx" },
        // display country (French)
        {   "\\u00C9tats-Unis",    "France",   "Croatie",  "Gr\\u00E8ce",   "Norv\\u00E8ge", "",     "YY" },
        // display variant (French)
        {   "",     "",     "",     "",     "Nynorsk",     "",     "" },
        // display name (French)
        //{   "anglais (États-Unis)", "français (France)", "croate (Croatie)", "grec (Grèce)", "norvégien (Norvège,Nynorsk)", "italien", "xx (YY)" },
        {   "anglais (\\u00C9tats-Unis)", "fran\\u00E7ais (France)", "croate (Croatie)", "grec (Gr\\u00E8ce)", "norv\\u00E9gien (Norv\\u00E8ge, Nynorsk)", "italien", "xx (YY)" }, // STILL not right

        // display langage (Croatian)
        {   "",  "", "hrvatski", "",    "", "", "xx" },
        // display country (Croatian)
        {   "",    "",   "Hrvatska",  "",   "", "", "YY" },
        // display variant (Croatian)
        {   "",     "",     "",     "",     "", "", ""},
        // display name (Croatian)
        {   "", "", "hrvatski (Hrvatska)", "", "",  "", "xx (YY)" },

        // display langage (Greek)[actual values listed below]
        {   "\\u0391\\u03b3\\u03b3\\u03bb\\u03b9\\u03ba\\u03ac", "\\u0393\\u03b1\\u03bb\\u03bb\\u03b9\\u03ba\\u03ac", "\\u039a\\u03c1\\u03bf\\u03b1\\u03c4\\u03b9\\u03ba\\u03ac", "",    "\\u039d\\u03bf\\u03c1\\u03b2\\u03b7\\u03b3\\u03b9\\u03ba\\u03ac", "", "" },
        // display country (Greek)[actual values listed below]
        {   "\\u0397\\u03bd\\u03c9\\u03bc\\u03ad\\u03bd\\u03b5\\u03c2 \\u03a0\\u03bf\\u03bb\\u03b9\\u03c4\\u03b5\\u03af\\u03b5\\u03c2 \\u0391\\u03bc\\u03b5\\u03c1\\u03b9\\u03ba\\u03ae\\u03c2", "\\u0393\\u03b1\\u03bb\\u03bb\\u03af\\u03b1", "\\u039a\\u03c1\\u03bf\\u03b1\\u03c4\\u03af\\u03b1", "",    "\\u039d\\u03bf\\u03c1\\u03b2\\u03b7\\u03b3\\u03af\\u03b1", "", "" },
        // display variant (Greek)
        {   "",     "",     "",     "",     "", "", "" },
        // display name (Greek)[actual values listed below]
        {   "\\u0391\\u03b3\\u03b3\\u03bb\\u03b9\\u03ba\\u03ac (\\u0397\\u03bd\\u03c9\\u03bc\\u03ad\\u03bd\\u03b5\\u03c2 \\u03a0\\u03bf\\u03bb\\u03b9\\u03c4\\u03b5\\u03af\\u03b5\\u03c2 \\u0391\\u03bc\\u03b5\\u03c1\\u03b9\\u03ba\\u03ae\\u03c2)", "\\u0393\\u03b1\\u03bb\\u03bb\\u03b9\\u03ba\\u03ac (\\u0393\\u03b1\\u03bb\\u03bb\\u03af\\u03b1)", "\\u039a\\u03c1\\u03bf\\u03b1\\u03c4\\u03b9\\u03ba\\u03ac (\\u039a\\u03c1\\u03bf\\u03b1\\u03c4\\u03af\\u03b1)", "",    "\\u039d\\u03bf\\u03c1\\u03b2\\u03b7\\u03b3\\u03b9\\u03ba\\u03ac (\\u039d\\u03bf\\u03c1\\u03b2\\u03b7\\u03b3\\u03af\\u03b1, Nynorsk)", "", "" },

        // display langage (<root>)
        {   "English",  "French",   "Croatian", "Greek",    "Norwegian",    "Italian",  "xx" },
        // display country (<root>)
        {   "United States",    "France",   "Croatia",  "Greece",   "Norway",   "",     "YY" },
        // display variant (<root>)
        {   "",     "",     "",     "",     "Nynorsk",   "",     ""},
        // display name (<root>)
        //{   "English (United States)", "French (France)", "Croatian (Croatia)", "Greek (Greece)", "Norwegian (Norway,Nynorsk)", "Italian", "xx (YY)" },
        {   "English (United States)", "French (France)", "Croatian (Croatia)", "Greek (Greece)", "Norwegian (Norway,NY)", "Italian", "xx (YY)" }
};

// * test macros
/*
 Usage:
    test_compare(    Function to be performed,
                       Test of the function,
                       expected result of the test,
                       printable result
                  )

   Example:
       test_compare(i=3,i,3, someNumberFormatter(i));
       test_compare(0,1+1,2,someNumberFormatter(1+1)); 

   Note that in the second example the expression is 0, because the fcn produces it's own result.

   Macro is ugly but makes the tests pretty.
*/

#define test_compare(expression,test,expected,printableResult) \
    { \
        expression; \
        \
        if((test) != (expected)) \
            errln("FAIL: " + UnicodeString(#expression) + "; -> " + printableResult + "\n" + \
                    "   (" + UnicodeString(#test) + " != " + UnicodeString(#expected) + ")" ); \
        else \
            logln(UnicodeString(#expression) + " -> " + printableResult + " (" + UnicodeString(#test) + ")"); \
    }




/*
 Usage:
    test_assert_print(    Test (should be TRUE),  printable  )

   Example:
       test_assert(i==3, toString(i));

   the macro is ugly but makes the tests pretty.
*/

#define test_assert_print(test,print) \
    { \
        if(!(test)) \
            errln("FAIL: " + UnicodeString(#test) + " was not true." + "-> " + UnicodeString(print) ); \
        else \
            logln("PASS: asserted " + UnicodeString(#test) + "-> " + UnicodeString(print)); \
    }


#define test_dumpLocale(l) { UnicodeString s(l.getName(),""); logln(#l + UnicodeString(" = ") + s); }

LocaleTest::LocaleTest()
: dataTable(NULL)
{
    setUpDataTable();
}

LocaleTest::~LocaleTest()
{
    if (dataTable != 0) {
        for (int32_t i = 0; i < 27; i++) {
            delete []dataTable[i];
        }
        delete []dataTable;
        dataTable = 0;
    }
}

#define CASE(id,test) case id: name = #test; if (exec) { logln(#test "---"); logln((UnicodeString)""); test(); } break;

void LocaleTest::runIndexedTest( int32_t index, UBool exec, const char* &name, char* /*par*/ )
{
    if (exec) logln("TestSuite LocaleTest: ");
    switch (index) {
        case 0: name = "TestBasicGetters"; if (exec) TestBasicGetters(); break;
        case 1: name = "TestSimpleResourceInfo"; if (exec) TestSimpleResourceInfo(); break;
        case 2: name = "TestDisplayNames"; if (exec) TestDisplayNames(); break;
        case 3: name = "TestSimpleObjectStuff"; if (exec) TestSimpleObjectStuff(); break;
        case 4: name = "TestPOSIXParsing"; if (exec) TestPOSIXParsing(); break;
        case 5: name = "TestGetAvailableLocales"; if (exec) TestGetAvailableLocales(); break;
        case 6: name = "TestDataDirectory"; if (exec) TestDataDirectory(); break;

        CASE(7, TestISO3Fallback)
        CASE(8, TestGetLangsAndCountries)
        CASE(9, TestSimpleDisplayNames)
        CASE(10, TestUninstalledISO3Names)
        CASE(11, TestAtypicalLocales)
#if !UCONFIG_NO_FORMATTING
        CASE(12, TestThaiCurrencyFormat)
        CASE(13, TestEuroSupport)
#endif
        CASE(14, TestToString)
#if !UCONFIG_NO_FORMATTING
        CASE(15, Test4139940)
        CASE(16, Test4143951)
#endif
        CASE(17, Test4147315)
        CASE(18, Test4147317)
        CASE(19, Test4147552)
        CASE(20, TestVariantParsing)
#if !UCONFIG_NO_FORMATTING
        CASE(21, Test4105828)
#endif
        CASE(22, TestSetIsBogus)
        CASE(23, TestParallelAPIValues)
        // keep the last index in sync with the condition in default:

        default:
            if(index <= 23) { // keep this in sync with the last index!
                name = "switched off"; // UCONFIG_NO_FORMATTING
            } else {
                name = "";
            }
            break; //needed to end loop
    }
}

void LocaleTest::TestBasicGetters() {
    UnicodeString   temp;

    int32_t i;
    for (i = 0; i <= MAX_LOCALES; i++) {
        Locale testLocale(rawData[LANG][i], rawData[CTRY][i], rawData[VAR][i]);
        logln("Testing " + (UnicodeString)testLocale.getName() + "...");

        if ( (temp=testLocale.getLanguage()) != (dataTable[LANG][i]))
            errln("  Language code mismatch: " + temp + " versus "
                        + dataTable[LANG][i]);
        if ( (temp=testLocale.getCountry()) != (dataTable[CTRY][i]))
            errln("  Country code mismatch: " + temp + " versus "
                        + dataTable[CTRY][i]);
        if ( (temp=testLocale.getVariant()) != (dataTable[VAR][i]))
            errln("  Variant code mismatch: " + temp + " versus "
                        + dataTable[VAR][i]);
        if ( (temp=testLocale.getName()) != (dataTable[NAME][i]))
            errln("  Locale name mismatch: " + temp + " versus "
                        + dataTable[NAME][i]);
    }

    logln("Same thing without variant codes...");
    for (i = 0; i <= MAX_LOCALES; i++) {
        Locale testLocale(rawData[LANG][i], rawData[CTRY][i]);
        logln("Testing " + (temp=testLocale.getName()) + "...");

        if ( (temp=testLocale.getLanguage()) != (dataTable[LANG][i]))
            errln("  Language code mismatch: " + temp + " versus "
                        + dataTable[LANG][i]);
        if ( (temp=testLocale.getCountry()) != (dataTable[CTRY][i]))
            errln("  Country code mismatch: " + temp + " versus "
                        + dataTable[CTRY][i]);
        if (testLocale.getVariant()[0] != 0)
            errln("  Variant code mismatch: something versus \"\"");
    }

    logln("Testing long language names and getters");
    Locale  test8 = Locale::createFromName("x-klingon-zx.utf32be@special");

    temp = test8.getLanguage();
    if (temp != UnicodeString("x-klingon") )
      errln("  Language code mismatch: " + temp + "  versus \"x-klingon\"");

    temp = test8.getCountry();
    if (temp != UnicodeString("ZX") )
      errln("  Country code mismatch: " + temp + "  versus \"ZX\"");

    temp = test8.getVariant();
    if (temp != UnicodeString("SPECIAL") )
      errln("  Variant code mismatch: " + temp + "  versus \"SPECIAL\"");

    if (Locale::getDefault() != Locale::createFromName(NULL))
      errln("Locale::getDefault() == Locale::createFromName(NULL)");

    /*----------*/
    // NOTE: There used to be a special test for locale names that had language or
    // country codes that were longer than two letters.  The new version of Locale
    // doesn't support anything that isn't an officially recognized language or
    // country code, so we no longer support this feature.

    Locale bogusLang("THISISABOGUSLANGUAGE"); // Jitterbug 2864: language code too long
    if(!bogusLang.isBogus()) {
        errln("Locale(\"THISISABOGUSLANGUAGE\").isBogus()==FALSE");
    }

    bogusLang=Locale("eo");
    if( bogusLang.isBogus() ||
        strcmp(bogusLang.getLanguage(), "eo")!=0 ||
        *bogusLang.getCountry()!=0 ||
        *bogusLang.getVariant()!=0 ||
        strcmp(bogusLang.getName(), "eo")!=0
    ) {
        errln("assignment to bogus Locale does not unbogus it or sets bad data");
    }
}

void LocaleTest::TestParallelAPIValues() {
    logln("Test synchronization between C and C++ API");
    if (strcmp(Locale::getChinese().getName(), ULOC_CHINESE) != 0) {
        errln("Differences for ULOC_CHINESE Locale");
    }
    if (strcmp(Locale::getEnglish().getName(), ULOC_ENGLISH) != 0) {
        errln("Differences for ULOC_ENGLISH Locale");
    }
    if (strcmp(Locale::getFrench().getName(), ULOC_FRENCH) != 0) {
        errln("Differences for ULOC_FRENCH Locale");
    }
    if (strcmp(Locale::getGerman().getName(), ULOC_GERMAN) != 0) {
        errln("Differences for ULOC_GERMAN Locale");
    }
    if (strcmp(Locale::getItalian().getName(), ULOC_ITALIAN) != 0) {
        errln("Differences for ULOC_ITALIAN Locale");
    }
    if (strcmp(Locale::getJapanese().getName(), ULOC_JAPANESE) != 0) {
        errln("Differences for ULOC_JAPANESE Locale");
    }
    if (strcmp(Locale::getKorean().getName(), ULOC_KOREAN) != 0) {
        errln("Differences for ULOC_KOREAN Locale");
    }
    if (strcmp(Locale::getSimplifiedChinese().getName(), ULOC_SIMPLIFIED_CHINESE) != 0) {
        errln("Differences for ULOC_SIMPLIFIED_CHINESE Locale");
    }
    if (strcmp(Locale::getTraditionalChinese().getName(), ULOC_TRADITIONAL_CHINESE) != 0) {
        errln("Differences for ULOC_TRADITIONAL_CHINESE Locale");
    }


    if (strcmp(Locale::getCanada().getName(), ULOC_CANADA) != 0) {
        errln("Differences for ULOC_CANADA Locale");
    }
    if (strcmp(Locale::getCanadaFrench().getName(), ULOC_CANADA_FRENCH) != 0) {
        errln("Differences for ULOC_CANADA_FRENCH Locale");
    }
    if (strcmp(Locale::getChina().getName(), ULOC_CHINA) != 0) {
        errln("Differences for ULOC_CHINA Locale");
    }
    if (strcmp(Locale::getPRC().getName(), ULOC_PRC) != 0) {
        errln("Differences for ULOC_PRC Locale");
    }
    if (strcmp(Locale::getFrance().getName(), ULOC_FRANCE) != 0) {
        errln("Differences for ULOC_FRANCE Locale");
    }
    if (strcmp(Locale::getGermany().getName(), ULOC_GERMANY) != 0) {
        errln("Differences for ULOC_GERMANY Locale");
    }
    if (strcmp(Locale::getItaly().getName(), ULOC_ITALY) != 0) {
        errln("Differences for ULOC_ITALY Locale");
    }
    if (strcmp(Locale::getJapan().getName(), ULOC_JAPAN) != 0) {
        errln("Differences for ULOC_JAPAN Locale");
    }
    if (strcmp(Locale::getKorea().getName(), ULOC_KOREA) != 0) {
        errln("Differences for ULOC_KOREA Locale");
    }
    if (strcmp(Locale::getTaiwan().getName(), ULOC_TAIWAN) != 0) {
        errln("Differences for ULOC_TAIWAN Locale");
    }
    if (strcmp(Locale::getUK().getName(), ULOC_UK) != 0) {
        errln("Differences for ULOC_UK Locale");
    }
    if (strcmp(Locale::getUS().getName(), ULOC_US) != 0) {
        errln("Differences for ULOC_US Locale");
    }
}


void LocaleTest::TestSimpleResourceInfo() {
  UnicodeString   temp;
  char            temp2[20];
  UErrorCode err = U_ZERO_ERROR;
  int32_t i = 0;

  for (i = 0; i <= MAX_LOCALES; i++) {
    Locale testLocale(rawData[LANG][i], rawData[CTRY][i], rawData[VAR][i]);
    logln("Testing " + (temp=testLocale.getName()) + "...");
    
    if ( (temp=testLocale.getISO3Language()) != (dataTable[LANG3][i]))
      errln("  ISO-3 language code mismatch: " + temp
        + " versus " + dataTable[LANG3][i]);
    if ( (temp=testLocale.getISO3Country()) != (dataTable[CTRY3][i]))
      errln("  ISO-3 country code mismatch: " + temp
        + " versus " + dataTable[CTRY3][i]);
    
    sprintf(temp2, "%x", testLocale.getLCID());
    if (UnicodeString(temp2) != dataTable[LCID][i])
      errln((UnicodeString)"  LCID mismatch: " + temp2 + " versus "
        + dataTable[LCID][i]);

    if(U_FAILURE(err))
      {
        errln((UnicodeString)"Some error on number " + i + u_errorName(err));
      }
    err = U_ZERO_ERROR;
  }

   Locale locale("en");
   if(strcmp(locale.getName(), "en") != 0||
       strcmp(locale.getLanguage(), "en") != 0) {
       errln("construction of Locale(en) failed\n");
   }
   /*-----*/

}

/*
 * Jitterbug 2439 -- markus 20030425
 *
 * The lookup of display names must not fall back through the default
 * locale because that yields useless results.
 */
void 
LocaleTest::TestDisplayNames() 
{
    Locale  english("en", "US");
    Locale  french("fr", "FR");
    Locale  croatian("hr", "HR");
    Locale  greek("el", "GR");

    logln("  In locale = en_US...");
    doTestDisplayNames(english, DLANG_EN);
    logln("  In locale = fr_FR...");
    doTestDisplayNames(french, DLANG_FR);
    logln("  In locale = hr_HR...");
    doTestDisplayNames(croatian, DLANG_HR);
    logln("  In locale = el_GR...");
    doTestDisplayNames(greek, DLANG_EL);

    /* test that the default locale has a display name for its own language */
    UnicodeString s;
    Locale().getDisplayLanguage(Locale(), s);
    if(s.length()<=3 && s.charAt(0)<=0x7f) {
        /* check <=3 to reject getting the language code as a display name */
        errln("unable to get a display string for the language of the default locale\n");
    }

    /*
     * API coverage improvements: call
     * Locale::getDisplayLanguage(UnicodeString &) and
     * Locale::getDisplayCountry(UnicodeString &)
     */
    s.remove();
    Locale().getDisplayLanguage(s);
    if(s.length()<=3 && s.charAt(0)<=0x7f) {
        errln("unable to get a display string for the language of the default locale [2]\n");
    }
    s.remove();
    french.getDisplayCountry(s);
    if(s.isEmpty()) {
        errln("unable to get any default-locale display string for the country of fr_FR\n");
    }
}

/*
 Usage:
    test_assert(    Test (should be TRUE)  )

   Example:
       test_assert(i==3);

   the macro is ugly but makes the tests pretty.
*/

#define test_assert(test) \
    { \
        if(!(test)) \
            errln("FAIL: " + UnicodeString(#test) + " was not true. " + UnicodeString(__FILE__ " line ") + __LINE__ ); \
        else \
            logln("PASS: asserted " + UnicodeString(#test) ); \
    }

void LocaleTest::TestSimpleObjectStuff() {
    Locale  test1("aa", "AA");
    Locale  test2("aa", "AA");
    Locale  test3(test1);
    Locale  test4("zz", "ZZ");
    Locale  test5("aa", "AA", ""); 
    Locale  test6("aa", "AA", "ANTARES"); 
    Locale  test7("aa", "AA", "JUPITER");
    Locale  test8 = Locale::createFromName("aa-aa.utf8@jupiter");

    // now list them all for debugging usage.
    test_dumpLocale(test1);
    test_dumpLocale(test2);
    test_dumpLocale(test3);
    test_dumpLocale(test4);
    test_dumpLocale(test5);
    test_dumpLocale(test6);
    test_dumpLocale(test7);
    test_dumpLocale(test8);

    // Make sure things compare to themselves!
    test_assert(test1 == test1);
    test_assert(test2 == test2);
    test_assert(test3 == test3);
    test_assert(test4 == test4);
    test_assert(test5 == test5);
    test_assert(test6 == test6);
    test_assert(test7 == test7);
    test_assert(test8 == test8);

    // make sure things are not equal to themselves.
    test_assert(!(test1 != test1));
    test_assert(!(test2 != test2));
    test_assert(!(test3 != test3));
    test_assert(!(test4 != test4));
    test_assert(!(test5 != test5));
    test_assert(!(test6 != test6));
    test_assert(!(test7 != test7));
    test_assert(!(test8 != test8));

    // make sure things that are equal to each other don't show up as unequal.
    test_assert(!(test1 != test2));
    test_assert(!(test2 != test1));
    test_assert(!(test1 != test3));
    test_assert(!(test2 != test3));
    test_assert(test5 == test1);
    test_assert(test6 != test2);
    test_assert(test6 != test5);

    test_assert(test6 != test7);

    // test for things that shouldn't compare equal.
    test_assert(!(test1 == test4));
    test_assert(!(test2 == test4));
    test_assert(!(test3 == test4));

    test_assert(test7 == test8);

    // test for hash codes to be the same.
    int32_t hash1 = test1.hashCode();
    int32_t hash2 = test2.hashCode();
    int32_t hash3 = test3.hashCode();

    test_assert(hash1 == hash2);
    test_assert(hash1 == hash3);
    test_assert(hash2 == hash3);

    // test that the assignment operator works.
    test4 = test1;
    logln("test4=test1;");
    test_dumpLocale(test4);
    test_assert(test4 == test4);

    test_assert(!(test1 != test4));
    test_assert(!(test2 != test4));
    test_assert(!(test3 != test4));
    test_assert(test1 == test4);
    test_assert(test4 == test1);
    
    // test assignments with a variant
    logln("test7 = test6");
    test7 = test6;
    test_dumpLocale(test7);
    test_assert(test7 == test7);
    test_assert(test7 == test6);
    test_assert(test7 != test5);

    logln("test6 = test1");
    test6=test1;
    test_dumpLocale(test6);
    test_assert(test6 != test7);
    test_assert(test6 == test1);
    test_assert(test6 == test6);
}

// A class which exposes constructors that are implemented in terms of the POSIX parsing code.
class POSIXLocale : public Locale
{
public:
    POSIXLocale(const UnicodeString& l)
        :Locale()
    {
      char *ch;
      ch = new char[l.length() + 1];
      ch[l.extract(0, 0x7fffffff, ch, "")] = 0;
      setFromPOSIXID(ch);
      delete [] ch;
    }
    POSIXLocale(const char *l)
        :Locale()
    {
        setFromPOSIXID(l);
    }
};

void LocaleTest::TestPOSIXParsing()
{
    POSIXLocale  test1("ab_AB");
    POSIXLocale  test2(UnicodeString("ab_AB"));
    Locale  test3("ab","AB");

    POSIXLocale test4("ab_AB_Antares");
    POSIXLocale test5(UnicodeString("ab_AB_Antares"));
    Locale  test6("ab", "AB", "Antares"); 

    test_dumpLocale(test1);
    test_dumpLocale(test2);
    test_dumpLocale(test3);
    test_dumpLocale(test4);
    test_dumpLocale(test5);
    test_dumpLocale(test6);

    test_assert(test1 == test1);

    test_assert(test1 == test2);
    test_assert(test2 == test3);
    test_assert(test3 == test1);

    test_assert(test4 == test5);
    test_assert(test5 == test6);
    test_assert(test6 == test4);

    test_assert(test1 != test4);
    test_assert(test5 != test3);
    test_assert(test5 != test2);

    int32_t hash1 = test1.hashCode();
    int32_t hash2 = test2.hashCode();
    int32_t hash3 = test3.hashCode();

    test_assert(hash1 == hash2);
    test_assert(hash2 == hash3);
    test_assert(hash3 == hash1);
}

void LocaleTest::TestGetAvailableLocales()
{
    int32_t locCount = 0;
    const Locale* locList = Locale::getAvailableLocales(locCount);

    if (locCount == 0)
        errln("getAvailableLocales() returned an empty list!");
    else {
        logln(UnicodeString("Number of locales returned = ") + locCount);
        UnicodeString temp;
        for(int32_t i = 0; i < locCount; ++i)
            logln(locList[i].getName());
    }
    // I have no idea how to test this function...
}

// This test isn't applicable anymore - getISO3Language is
// independent of the data directory
void LocaleTest::TestDataDirectory()
{
/*
    char            oldDirectory[80];
    const char*     temp;
    UErrorCode       err = U_ZERO_ERROR;
    UnicodeString   testValue;

    temp = Locale::getDataDirectory();
    strcpy(oldDirectory, temp);
    logln(UnicodeString("oldDirectory = ") + oldDirectory);

    Locale  test(Locale::US);
    test.getISO3Language(testValue);
    logln("first fetch of language retrieved " + testValue);
    if (testValue != "eng")
        errln("Initial check of ISO3 language failed: expected \"eng\", got \"" + testValue + "\"");

    {
        char *path;
        path=IntlTest::getTestDirectory();
        Locale::setDataDirectory( path );
    }

    test.getISO3Language(testValue);
    logln("second fetch of language retrieved " + testValue);
    if (testValue != "xxx")
        errln("setDataDirectory() failed: expected \"xxx\", got \"" + testValue + "\"");
    
    Locale::setDataDirectory(oldDirectory);
    test.getISO3Language(testValue);
    logln("third fetch of language retrieved " + testValue);
    if (testValue != "eng")
        errln("get/setDataDirectory() failed: expected \"eng\", got \"" + testValue + "\"");
*/
}

//===========================================================

void LocaleTest::doTestDisplayNames(Locale& displayLocale, int32_t compareIndex) {
    UnicodeString   temp;
    
    for (int32_t i = 0; i <= MAX_LOCALES; i++) {
        Locale testLocale(rawData[LANG][i], rawData[CTRY][i], rawData[VAR][i]);
        logln("  Testing " + (temp=testLocale.getName()) + "...");

        UnicodeString  testLang;
        UnicodeString  testCtry;
        UnicodeString  testVar;
        UnicodeString  testName;

        testLocale.getDisplayLanguage(displayLocale, testLang);
        testLocale.getDisplayCountry(displayLocale, testCtry);
        testLocale.getDisplayVariant(displayLocale, testVar);
        testLocale.getDisplayName(displayLocale, testName);

        UnicodeString  expectedLang;
        UnicodeString  expectedCtry;
        UnicodeString  expectedVar;
        UnicodeString  expectedName;

        expectedLang = dataTable[compareIndex][i];
        if (expectedLang.length() == 0)
            expectedLang = dataTable[DLANG_EN][i];

        expectedCtry = dataTable[compareIndex + 1][i];
        if (expectedCtry.length() == 0)
            expectedCtry = dataTable[DCTRY_EN][i];

        expectedVar = dataTable[compareIndex + 2][i];
        if (expectedVar.length() == 0)
            expectedVar = dataTable[DVAR_EN][i];

        expectedName = dataTable[compareIndex + 3][i];
        if (expectedName.length() == 0)
            expectedName = dataTable[DNAME_EN][i];

        if (testLang != expectedLang)
            errln("Display language (" + UnicodeString(displayLocale.getName()) + ") got " + testLang + " expected " + expectedLang);
        if (testCtry != expectedCtry)
            errln("Display country (" + UnicodeString(displayLocale.getName()) + ") got " + testCtry + " expected " + expectedCtry);
        if (testVar != expectedVar)
            errln("Display variant (" + UnicodeString(displayLocale.getName()) + ") got " + testVar + " expected " + expectedVar);
        if (testName != expectedName)
            errln("Display name (" + UnicodeString(displayLocale.getName()) + ") got " + testName + " expected " + expectedName);
    }
}

//---------------------------------------------------
// table of valid data
//---------------------------------------------------


UChar greekDisplayLanguage[] = { 0x03b5, 0x03bb, 0x03bb, 0x03b7, 0x03bd, 0x03b9, 0x03ba, 0x03ac, 0 };
UChar greekDisplayCountry[] = { 0x0395, 0x03bb, 0x03bb, 0x03ac, 0x03b4, 0x03b1, 0 };
UChar greekDisplayName[] = { 0x03b5, 0x03bb, 0x03bb, 0x03b7, 0x03bd, 0x03b9, 0x03ba,
    0x03ac, 0x20, 0x28, 0x0395, 0x03bb, 0x03bb, 0x03ac, 0x03b4, 0x03b1, 0x29, 0 };
    
void LocaleTest::setUpDataTable()
{
    if (dataTable == 0) {
        dataTable = new UnicodeString*[27];

        for (int32_t i = 0; i < 27; i++) {
            dataTable[i] = new UnicodeString[7];
            for (int32_t j = 0; j < 7; j++) {
                dataTable[i][j] = CharsToUnicodeString(rawData[i][j]);
            }
        }
        dataTable[DLANG_EL][GREEK] = greekDisplayLanguage;
        dataTable[DCTRY_EL][GREEK] = greekDisplayCountry;
        dataTable[DNAME_EL][GREEK] = greekDisplayName;
    }
}

// ====================


/**
 * @bug 4011756 4011380
 */
void 
LocaleTest::TestISO3Fallback() 
{
    Locale test("xx", "YY");

    const char * result;

    result = test.getISO3Language();

    // Conform to C API usage 

    if (!result || (result[0] != 0))
        errln("getISO3Language() on xx_YY returned " + UnicodeString(result) + " instead of \"\"");

    result = test.getISO3Country();

    if (!result || (result[0] != 0))
        errln("getISO3Country() on xx_YY returned " + UnicodeString(result) + " instead of \"\"");
}

/**
 * @bug 4106155 4118587
 */
void 
LocaleTest::TestGetLangsAndCountries() 
{
    // It didn't seem right to just do an exhaustive test of everything here, so I check
    // for the following things:
    // 1) Does each list have the right total number of entries?
    // 2) Does each list contain certain language and country codes we think are important
    //     (the G7 countries, plus a couple others)?
    // 3) Does each list have every entry formatted correctly? (i.e., two characters,
    //     all lower case for the language codes, all upper case for the country codes)
    // 4) Is each list in sorted order?
    int32_t testCount = 0;
    const char * const * test = Locale::getISOLanguages();
    const char spotCheck1[ ][4] = { "en", "es", "fr", "de", "it", 
                                    "ja", "ko", "zh", "th", "he", 
                                    "id", "iu", "ug", "yi", "za" };

    int32_t i;
    
    for(testCount = 0;test[testCount];testCount++)
      ;

    if (testCount != 450)
        errln("Expected getISOLanguages() to return 450 languages; it returned %d", testCount);
    else {
        for (i = 0; i < 15; i++) {
            int32_t j;
            for (j = 0; j < testCount; j++)
              if (uprv_strcmp(test[j],spotCheck1[i])== 0)
                    break;
            if (j == testCount || (uprv_strcmp(test[j],spotCheck1[i])!=0))
                errln("Couldn't find " + (UnicodeString)spotCheck1[i] + " in language list.");
        }
    }
    for (i = 0; i < testCount; i++) {
        UnicodeString testee(test[i],"");
        UnicodeString lc(test[i],"");
        if (testee != lc.toLower())
            errln(lc + " is not all lower case.");
        if ( (testee.length() != 2) && (testee.length() != 3))
            errln(testee + " is not two or three characters long.");
        if (i > 0 && testee.compare(test[i - 1]) <= 0)
            errln(testee + " appears in an out-of-order position in the list.");
    }

    test = Locale::getISOCountries();
    UnicodeString spotCheck2 [] = { "US", "CA", "GB", "FR", "DE", 
                                    "IT", "JP", "KR", "CN", "TW", 
                                    "TH" };
    int32_t spot2Len = 11;
    for(testCount=0;test[testCount];testCount++)
      ;

    if (testCount != 239)
        errln("Expected getISOLanguages to return 238 languages; it returned" + testCount);
    else {
        for (i = 0; i < spot2Len; i++) {
            int32_t j;
            for (j = 0; j < testCount; j++)
              {
                UnicodeString testee(test[j],"");

                if (testee == spotCheck2[i])
                    break;
              }
                UnicodeString testee(test[j],"");
            if (j == testCount || testee != spotCheck2[i])
                errln("Couldn't find " + spotCheck2[i] + " in country list.");
        }
    }
    for (i = 0; i < testCount; i++) {
      UnicodeString testee(test[i],"");
        UnicodeString uc(test[i],"");
        if (testee != uc.toUpper())
            errln(testee + " is not all upper case.");
        if (testee.length() != 2)
            errln(testee + " is not two characters long.");
        if (i > 0 && testee.compare(test[i - 1]) <= 0)
            errln(testee + " appears in an out-of-order position in the list.");
    }
}

/**
 * @bug 4118587
 */
void 
LocaleTest::TestSimpleDisplayNames() 
{
    // This test is different from TestDisplayNames because TestDisplayNames checks
    // fallback behavior, combination of language and country names to form locale
    // names, and other stuff like that.  This test just checks specific language
    // and country codes to make sure we have the correct names for them.
    char languageCodes[] [4] = { "he", "id", "iu", "ug", "yi", "za" };
    UnicodeString languageNames [] = { "Hebrew", "Indonesian", "Inukitut", "Uighur", "Yiddish",
                               "Zhuang" };

    for (int32_t i = 0; i < 6; i++) {
        UnicodeString test;
        Locale l(languageCodes[i], "", "");
        l.getDisplayLanguage(Locale::getUS(), test);
        if (test != languageNames[i])
            errln("Got wrong display name for " + UnicodeString(languageCodes[i]) + ": Expected \"" +
                  languageNames[i] + "\", got \"" + test + "\".");
    }
}

/**
 * @bug 4118595
 */
void 
LocaleTest::TestUninstalledISO3Names() 
{
    // This test checks to make sure getISO3Language and getISO3Country work right
    // even for locales that are not installed.
    const char iso2Languages [][4] = {     "am", "ba", "fy", "mr", "rn", 
                                        "ss", "tw", "zu" };
    const char iso3Languages [][5] = {     "amh", "bak", "fry", "mar", "run", 
                                        "ssw", "twi", "zul" };

    int32_t i;

    for (i = 0; i < 8; i++) {
      UErrorCode err = U_ZERO_ERROR;

      UnicodeString test;
        Locale l(iso2Languages[i], "", "");
        test = l.getISO3Language();
        if((test != iso3Languages[i]) || U_FAILURE(err))
            errln("Got wrong ISO3 code for " + UnicodeString(iso2Languages[i]) + ": Expected \"" +
                    iso3Languages[i] + "\", got \"" + test + "\"." + UnicodeString(u_errorName(err)));
    }

    char iso2Countries [][4] = {     "AF", "BW", "KZ", "MO", "MN", 
                                        "SB", "TC", "ZW" };
    char iso3Countries [][4] = {     "AFG", "BWA", "KAZ", "MAC", "MNG", 
                                        "SLB", "TCA", "ZWE" };

    for (i = 0; i < 8; i++) {
      UErrorCode err = U_ZERO_ERROR;
        Locale l("", iso2Countries[i], "");
        UnicodeString test(l.getISO3Country(), "");
        if (test != iso3Countries[i])
            errln("Got wrong ISO3 code for " + UnicodeString(iso2Countries[i]) + ": Expected \"" +
                    UnicodeString(iso3Countries[i]) + "\", got \"" + test + "\"." + u_errorName(err));
    }
}

/**
 * @bug 4092475
 * I could not reproduce this bug.  I'm pretty convinced it was fixed with the
 * big locale-data reorg of 10/28/97.  The lookup logic for language and country
 * display names was also changed at that time in that check-in.    --rtg 3/20/98
 */
void 
LocaleTest::TestAtypicalLocales() 
{
    Locale localesToTest [] = { Locale("de", "CA"),
                                  Locale("ja", "ZA"),
                                   Locale("ru", "MX"),
                                   Locale("en", "FR"),
                                   Locale("es", "DE"),
                                   Locale("", "HR"),
                                   Locale("", "SE"),
                                   Locale("", "DO"),
                                   Locale("", "BE") };

    UnicodeString englishDisplayNames [] = { "German (Canada)",
                                     "Japanese (South Africa)",
                                     "Russian (Mexico)",
                                     "English (France)",
                                     "Spanish (Germany)",
                                     "Croatia",
                                     "Sweden",
                                     "Dominican Republic",
                                     "Belgium" };
    UnicodeString frenchDisplayNames []= { "allemand (Canada)",
                                    "japonais (Afrique du Sud)",
                                    "russe (Mexique)",
                                     "anglais (France)",
                                     "espagnol (Allemagne)",
                                    "Croatie",
                                    CharsToUnicodeString("Su\\u00E8de"),
                                    CharsToUnicodeString("R\\u00E9publique Dominicaine"),
                                    "Belgique" };
    UnicodeString spanishDisplayNames [] = {
                                     CharsToUnicodeString("alem\\u00E1n (Canad\\u00E1)"),
                                     CharsToUnicodeString("japon\\u00E9s (Sud\\u00E1frica)"),
                                     CharsToUnicodeString("ruso (M\\u00E9xico)"),
                                     CharsToUnicodeString("ingl\\u00E9s (Francia)"),
                                     CharsToUnicodeString("espa\\u00F1ol (Alemania)"),
                                     "Croacia",
                                     "Suecia",
                                     CharsToUnicodeString("Rep\\u00FAblica Dominicana"),
                                     CharsToUnicodeString("B\\u00E9lgica") };
    UnicodeString arabicDisplayNames [] = { "German (Canada)",
                                     "Japanese (South Africa)",
                                     "Russian (Mexico)",
                                     "English (France)",
                                     "Spanish (Germany)",
                                     "Croatia",
                                     "Sweden",
                                     "Dominican Republic",
                                     "Belgium" };


    int32_t i;
    UErrorCode status = U_ZERO_ERROR;
    Locale::setDefault(Locale::getUS(), status);
    for (i = 0; i < 9; ++i) {
        UnicodeString name;
        localesToTest[i].getDisplayName(Locale::getUS(), name);
        logln(name);
        if (name != englishDisplayNames[i])
        {
            errln("Lookup in English failed: expected \"" + englishDisplayNames[i]
                        + "\", got \"" + name + "\"");
            logln("Locale name was-> " + (name=localesToTest[i].getName()));
        }
    }

    for (i = 0; i < 9; i++) {
        UnicodeString name;
        localesToTest[i].getDisplayName(Locale("es", "ES"), name);
        logln(name);
        if (name != spanishDisplayNames[i])
            errln("Lookup in Spanish failed: expected \"" + spanishDisplayNames[i]
                        + "\", got \"" + name + "\"");
    }

    for (i = 0; i < 9; i++) {
        UnicodeString name;
        localesToTest[i].getDisplayName(Locale::getFrance(), name);
        logln(name);
        if (name != frenchDisplayNames[i])
            errln("Lookup in French failed: expected \"" + frenchDisplayNames[i]
                        + "\", got \"" + name + "\"");
    }

    for (i = 0; i < 9; i++) {
        UnicodeString name;
        localesToTest[i].getDisplayName(Locale("ar", "ES"), name);
        logln(name + " Locale fallback to ar, and data fallback to root");
        if (name != arabicDisplayNames[i])
            errln("Lookup in Arabic failed: expected \"" + arabicDisplayNames[i]
                        + "\", got \"" + name + "\"");
        localesToTest[i].getDisplayName(Locale("ar", "EG"), name);
        logln(name + " Data fallback to root");
        if (name != arabicDisplayNames[i])
            errln("Lookup in Arabic failed: expected \"" + arabicDisplayNames[i]
                        + "\", got \"" + name + "\"");
    }
}

#if !UCONFIG_NO_FORMATTING

/**
 * @bug 4135752
 * This would be better tested by the LocaleDataTest.  Will move it when I
 * get the LocaleDataTest working again.
 */
void 
LocaleTest::TestThaiCurrencyFormat() 
{
    UErrorCode status = U_ZERO_ERROR;
    DecimalFormat *thaiCurrency = (DecimalFormat*)NumberFormat::createCurrencyInstance(
                    Locale("th", "TH"), status);
    UChar posPrefix = 0x0e3f;
    UnicodeString temp;

    if(U_FAILURE(status) || !thaiCurrency)
    {
        errln("Couldn't get th_TH currency -> " + UnicodeString(u_errorName(status)));
        return;
    }
    if (thaiCurrency->getPositivePrefix(temp) != UnicodeString(&posPrefix, 1, 1))
        errln("Thai currency prefix wrong: expected 0x0e3f, got \"" +
                        thaiCurrency->getPositivePrefix(temp) + "\"");
    if (thaiCurrency->getPositiveSuffix(temp) != "")
        errln("Thai currency suffix wrong: expected \"\", got \"" +
                        thaiCurrency->getPositiveSuffix(temp) + "\"");

    delete thaiCurrency;
}

/**
 * @bug 4122371
 * Confirm that Euro support works.  This test is pretty rudimentary; all it does
 * is check that any locales with the EURO variant format a number using the
 * Euro currency symbol.
 *
 * ASSUME: All locales encode the Euro character "\u20AC".
 * If this is changed to use the single-character Euro symbol, this
 * test must be updated.
 *
 */
void 
LocaleTest::TestEuroSupport() 
{
    UChar euro = 0x20ac;
    const UnicodeString EURO_CURRENCY(&euro, 1, 1); // Look for this UnicodeString in formatted Euro currency
    const char* localeArr[] = {
                            "ca_ES",
                            "de_AT",
                            "de_DE",
                            "de_LU",
                            "el_GR",
                            "en_BE",
                            "en_IE",
                            "es_ES",
                            "eu_ES",
                            "fi_FI",
                            "fr_BE",
                            "fr_FR",
                            "fr_LU",
                            "ga_IE",
                            "gl_ES",
                            "it_IT",
                            "nl_BE",
                            "nl_NL",
                            "pt_PT",
                            NULL
                        };
    const char** locales = localeArr;

    UErrorCode status = U_ZERO_ERROR;

    UnicodeString temp;

    for (;*locales!=NULL;locales++) {
        Locale loc (*locales);
        UnicodeString temp;
            NumberFormat *nf = NumberFormat::createCurrencyInstance(loc, status);
            UnicodeString pos;
            nf->format(271828.182845, pos);
            UnicodeString neg;
            nf->format(-271828.182845, neg);
            if (pos.indexOf(EURO_CURRENCY) >= 0 &&
                neg.indexOf(EURO_CURRENCY) >= 0) {
                logln("Ok: " + (temp=loc.getName()) +
                      ": " + pos + " / " + neg);
            }
            else {
                errln("Fail: " + (temp=loc.getName()) +
                      " formats without " + EURO_CURRENCY +
                      ": " + pos + " / " + neg +
                      "\n*** THIS FAILURE MAY ONLY MEAN THAT LOCALE DATA HAS CHANGED ***");
            }
        
            delete nf;
    }
}

#endif

/**
 * @bug 4139504
 * toString() doesn't work with language_VARIANT.
 */
void 
LocaleTest::TestToString() {
    Locale DATA [] = {
        Locale("xx", "", ""),
        Locale("", "YY", ""),
        Locale("", "", "ZZ"),
        Locale("xx", "YY", ""),
        Locale("xx", "", "ZZ"),
        Locale("", "YY", "ZZ"),
        Locale("xx", "YY", "ZZ"),
    };

    const char DATA_S [][20] = {
        "xx",
        "_YY",
        "__ZZ",
        "xx_YY",
        "xx__ZZ",
        "_YY_ZZ",
        "xx_YY_ZZ",
    };
    
    for (int32_t i=0; i < 7; ++i) {
      const char *name;
      name = DATA[i].getName();

      if (strcmp(name, DATA_S[i]) != 0)
        {
            errln("Fail: Locale.getName(), got:" + UnicodeString(name) + ", expected: " + DATA_S[i]);
        }
        else
            logln("Pass: Locale.getName(), got:" + UnicodeString(name) );
    }
}

#if !UCONFIG_NO_FORMATTING

/**
 * @bug 4139940
 * Couldn't reproduce this bug -- probably was fixed earlier.
 *
 * ORIGINAL BUG REPORT:
 * -- basically, hungarian for monday shouldn't have an \u00f4 
 * (o circumflex)in it instead it should be an o with 2 inclined 
 * (right) lines over it..
 *
 * You may wonder -- why do all this -- why not just add a line to
 * LocaleData?  Well, I could see by inspection that the locale file had the
 * right character in it, so I wanted to check the rest of the pipeline -- a
 * very remote possibility, but I wanted to be sure.  The other possibility
 * is that something is wrong with the font mapping subsystem, but we can't
 * test that here.
 */
void 
LocaleTest::Test4139940() 
{
    Locale mylocale("hu", "", "");       
    UDate mydate = date(98,3,13); // A Monday
    UErrorCode status = U_ZERO_ERROR;
    SimpleDateFormat df_full("EEEE", mylocale, status);
    UnicodeString str;
    FieldPosition pos(FieldPosition::DONT_CARE);
    df_full.format(mydate, str, pos);
    // Make sure that o circumflex (\u00F4) is NOT there, and
    // o double acute (\u0151) IS.
    UChar ocf = 0x00f4;
    UChar oda = 0x0151;
    if (str.indexOf(oda) < 0 || str.indexOf(ocf) >= 0)
        errln("Fail: Monday in Hungarian is wrong");
}

UDate
LocaleTest::date(int32_t y, int32_t m, int32_t d, int32_t hr, int32_t min, int32_t sec)
{
    UErrorCode status = U_ZERO_ERROR;
    Calendar *cal = Calendar::createInstance(status);
    if (cal == 0) 
        return 0.0;
    cal->clear();
    cal->set(1900 + y, m, d, hr, min, sec); // Add 1900 to follow java.util.Date protocol
    UDate dt = cal->getTime(status);
    if (U_FAILURE(status))
        return 0.0;
    
    delete cal;
    return dt;
}

/**
 * @bug 4143951
 * Russian first day of week should be Monday. Confirmed.
 */
void 
LocaleTest::Test4143951() 
{
    UErrorCode status = U_ZERO_ERROR;
    Calendar *cal = Calendar::createInstance(Locale("ru", "", ""), status);
    if(U_SUCCESS(status)) {
      if (cal->getFirstDayOfWeek(status) != UCAL_MONDAY) {
          errln("Fail: First day of week in Russia should be Monday");
      }
    }
    delete cal;
}

#endif

/**
 * @bug 4147315
 * java.util.Locale.getISO3Country() works wrong for non ISO-3166 codes.
 * Should throw an exception for unknown locales
 */
void 
LocaleTest::Test4147315() 
{
  UnicodeString temp;
    // Try with codes that are the wrong length but happen to match text
    // at a valid offset in the mapping table
    Locale locale("aaa", "CCC");
    
    const char *result = locale.getISO3Country();

    // Change to conform to C api usage
    if((result==NULL)||(result[0] != 0))
      errln("ERROR: getISO3Country() returns: " + UnicodeString(result,"") + 
                " for locale '" + (temp=locale.getName()) + "' rather than exception" );
}

/**
 * @bug 4147317
 * java.util.Locale.getISO3Language() works wrong for non ISO-3166 codes.
 * Should throw an exception for unknown locales
 */
void 
LocaleTest::Test4147317() 
{
    UnicodeString temp;
    // Try with codes that are the wrong length but happen to match text
    // at a valid offset in the mapping table
    Locale locale("aaa", "CCC");
    
    const char *result = locale.getISO3Language();

    // Change to conform to C api usage
    if((result==NULL)||(result[0] != 0))
      errln("ERROR: getISO3Language() returns: " + UnicodeString(result,"") + 
                " for locale '" + (temp=locale.getName()) + "' rather than exception" );
}

/*
 * @bug 4147552
 */
void 
LocaleTest::Test4147552() 
{
    Locale locales [] = {     Locale("no", "NO"), 
                            Locale("no", "NO", "B"),
                             Locale("no", "NO", "NY") 
    };
    
    UnicodeString edn("Norwegian (Norway, Bokm");
    edn += (UChar32)0x00e5;
    edn += "l)";
    UnicodeString englishDisplayNames [] = { 
                                                "Norwegian (Norway)",
                                                 edn,
                                                 // "Norwegian (Norway,B)",
                                                 //"Norwegian (Norway,NY)" 
                                                 "Norwegian (Norway, Nynorsk)" 
    };
    UnicodeString ndn("norsk (Norge, Bokm");
    ndn += (UChar32)0x00e5;
    ndn += "l)";
    UnicodeString norwegianDisplayNames [] = { 
                                                "norsk (Norge)",
                                                //"norsk (Norge,B)", 
                                                ndn, 
                                                 //"norsk (Norge,NY)" 
                                                 "norsk (Noreg, Nynorsk)" 
    };

    for (int32_t i = 0; i < 3; ++i) {
        Locale loc = locales[i];
        UnicodeString temp;
        if (loc.getDisplayName(temp) != englishDisplayNames[i])
           errln("English display-name mismatch: expected " +
                   englishDisplayNames[i] + ", got " + loc.getDisplayName(temp));
        if (loc.getDisplayName(loc, temp) != norwegianDisplayNames[i])
            errln("Norwegian display-name mismatch: expected " +
                   norwegianDisplayNames[i] + ", got " +
                   loc.getDisplayName(loc, temp));
    }
}

void
LocaleTest::TestVariantParsing()
{
    Locale en_US_custom("en", "US", "De Anza_Cupertino_California_United States_Earth");

    UnicodeString dispName("English (United States, DE ANZA_CUPERTINO_CALIFORNIA_UNITED STATES_EARTH)");
    UnicodeString dispVar("DE ANZA_CUPERTINO_CALIFORNIA_UNITED STATES_EARTH");
    
    UnicodeString got;
    
    en_US_custom.getDisplayVariant(Locale::getUS(), got);
    if(got != dispVar) {
        errln("FAIL: getDisplayVariant()");
        errln("Wanted: " + dispVar);
        errln("Got   : " + got);
    }

    en_US_custom.getDisplayName(Locale::getUS(), got);
    if(got != dispName) {
        errln("FAIL: getDisplayName()");
        errln("Wanted: " + dispName);
        errln("Got   : " + got);
    }

    Locale shortVariant("fr", "FR", "foo");
    shortVariant.getDisplayVariant(got);
    
    if(got != "FOO") {
        errln("FAIL: getDisplayVariant()");
        errln("Wanted: foo");
        errln("Got   : " + got);
    }

    Locale bogusVariant("fr", "FR", "_foo");
    bogusVariant.getDisplayVariant(got);
    
    if(got != "FOO") {
        errln("FAIL: getDisplayVariant()");
        errln("Wanted: foo");
        errln("Got   : " + got);
    }

    Locale bogusVariant2("fr", "FR", "foo_");
    bogusVariant2.getDisplayVariant(got);
    
    if(got != "FOO") {
        errln("FAIL: getDisplayVariant()");
        errln("Wanted: foo");
        errln("Got   : " + got);
    }

    Locale bogusVariant3("fr", "FR", "_foo_");
    bogusVariant3.getDisplayVariant(got);
    
    if(got != "FOO") {
        errln("FAIL: getDisplayVariant()");
        errln("Wanted: foo");
        errln("Got   : " + got);
    }
}

#if !UCONFIG_NO_FORMATTING

/**
 * @bug 4105828
 * Currency symbol in zh is wrong.  We will test this at the NumberFormat
 * end to test the whole pipe.
 */
void 
LocaleTest::Test4105828() 
{
    Locale LOC [] = { Locale::getChinese(),  Locale("zh", "CN", ""),
                     Locale("zh", "TW", ""), Locale("zh", "HK", "") };
    UErrorCode status = U_ZERO_ERROR;
    for (int32_t i = 0; i < 4; ++i) {
        NumberFormat *fmt = NumberFormat::createPercentInstance(LOC[i], status);
        if(U_FAILURE(status)) {
            errln("Couldn't create NumberFormat");
            return;
        }
        UnicodeString result;
        FieldPosition pos(0);
        fmt->format((int32_t)1, result, pos);
        UnicodeString temp;
        if(result != "100%") {
            errln(UnicodeString("Percent for ") + LOC[i].getDisplayName(temp) + " should be 100%, got " + result);
        }
        delete fmt;
    }
}

#endif

// Tests setBogus and isBogus APIs for Locale
// Jitterbug 1735
void
LocaleTest::TestSetIsBogus() {
  Locale l("en_US");
  l.setToBogus();
  if(l.isBogus() != TRUE) {
    errln("After setting bogus, didn't return TRUE");
  }
  l = "en_US"; // This should reset bogus
  if(l.isBogus() != FALSE) {
    errln("After resetting bogus, didn't return FALSE");
  }
}

