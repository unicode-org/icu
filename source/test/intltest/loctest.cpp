/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

#include "loctest.h"
#include <stdio.h>
#include <string.h>

#include "unicode/decimfmt.h"
//#include "unicode/datefmt.h"
#include "unicode/smpdtfmt.h"

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


#define test_dumpLocale(l) { UnicodeString s; l.getName(s); logln(#l + UnicodeString(" = ") + s); }

LocaleTest::LocaleTest()
{
    setUpDataTable();
}

LocaleTest::~LocaleTest()
{
}

#define CASE(id,test) case id: name = #test; if (exec) { logln(#test "---"); logln((UnicodeString)""); test(); } break;

void LocaleTest::runIndexedTest( int32_t index, bool_t exec, char* &name, char* par )
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
        CASE(9, Test4126880)
        CASE(10, TestBug4135316)
        CASE(11, TestSimpleDisplayNames)
        CASE(12, TestUninstalledISO3Names)
        CASE(13, TestAtypicalLocales)
        CASE(14, TestThaiCurrencyFormat)
        CASE(15, TestEuroSupport)
        CASE(16, TestToString)
        CASE(17, Test4139940)
        CASE(18, Test4143951)
        CASE(19, Test4147315)
        CASE(20, Test4147317)
        CASE(21, Test4147552)
        CASE(22, TestVariantParsing)
        CASE(23, Test4105828)

        default: name = ""; break; //needed to end loop
    }
}

void LocaleTest::TestBasicGetters() {
    UnicodeString   temp;

    int32_t i;
    for (i = 0; i <= MAX_LOCALES; i++) {
        Locale testLocale(dataTable[LANG][i], dataTable[CTRY][i], dataTable[VAR][i]);
        logln("Testing " + testLocale.getName(temp) + "...");

        if (testLocale.getLanguage(temp) != (dataTable[LANG][i]))
            errln("  Language code mismatch: " + temp + " versus "
                        + dataTable[LANG][i]);
        if (testLocale.getCountry(temp) != (dataTable[CTRY][i]))
            errln("  Country code mismatch: " + temp + " versus "
                        + dataTable[CTRY][i]);
        if (testLocale.getVariant(temp) != (dataTable[VAR][i]))
            errln("  Variant code mismatch: " + temp + " versus "
                        + dataTable[VAR][i]);
        if (testLocale.getName(temp) != (dataTable[NAME][i]))
            errln("  Locale name mismatch: " + temp + " versus "
                        + dataTable[NAME][i]);
    }

    logln("Same thing without variant codes...");
    for (i = 0; i <= MAX_LOCALES; i++) {
        Locale testLocale(dataTable[LANG][i], dataTable[CTRY][i]);
        logln("Testing " + testLocale.getName(temp) + "...");

        if (testLocale.getLanguage(temp) != (dataTable[LANG][i]))
            errln("  Language code mismatch: " + temp + " versus "
                        + dataTable[LANG][i]);
        if (testLocale.getCountry(temp) != (dataTable[CTRY][i]))
            errln("  Country code mismatch: " + temp + " versus "
                        + dataTable[CTRY][i]);
        if (testLocale.getVariant(temp).length() != 0)
            errln("  Variant code mismatch: " + temp + " versus \"\"");
    }


    // NOTE: There used to be a special test for locale names that had language or
    // country codes that were longer than two letters.  The new version of Locale
    // doesn't support anything that isn't an officially recognized language or
    // country code, so we no longer support this feature.
}

void LocaleTest::TestSimpleResourceInfo() {
  UnicodeString   temp;
  char            temp2[20];
  
  for (int32_t i = 0; i <= MAX_LOCALES; i++) {
    Locale testLocale(dataTable[LANG][i], dataTable[CTRY][i], dataTable[VAR][i]);
    logln("Testing " + testLocale.getName(temp) + "...");
    
    if (testLocale.getISO3Language(temp) != (dataTable[LANG3][i]))
      errln("  ISO-3 language code mismatch: " + temp
        + " versus " + dataTable[LANG3][i]);
    if (testLocale.getISO3Country(temp) != (dataTable[CTRY3][i]))
      errln("  ISO-3 country code mismatch: " + temp
        + " versus " + dataTable[CTRY3][i]);
    
    sprintf(temp2, "%x", testLocale.getLCID());
    if (UnicodeString(temp2) != dataTable[LCID][i])
      errln((UnicodeString)"  LCID mismatch: " + (int32_t)testLocale.getLCID() + " versus "
        + dataTable[LCID][i]);
  }
}

void 
LocaleTest::TestDisplayNames() 
{
    Locale  saveDefault = Locale::getDefault();
    Locale  empty("", "");
    Locale  english("en", "US");
    Locale  french("fr", "FR");
    Locale  croatian("hr", "HR");
    Locale  greek("el", "GR");
    UErrorCode err = U_ZERO_ERROR;

    Locale::setDefault(english, err);
    if (U_FAILURE(err)) {
        errln("Locale::setDefault returned error code " + (int)err);
        return;
    }

    logln("With default = en_US...");
    logln("  In default locale...");
    doTestDisplayNames(empty, DLANG_EN, FALSE);
    logln("  In locale = en_US...");
    doTestDisplayNames(english, DLANG_EN, FALSE);
    logln("  In locale = fr_FR...");
    doTestDisplayNames(french, DLANG_FR, FALSE);
    logln("  In locale = hr_HR...");
    doTestDisplayNames(croatian, DLANG_HR, FALSE);
    logln("  In locale = el_GR...");
    doTestDisplayNames(greek, DLANG_EL, FALSE);

    Locale::setDefault(french, err);
    if (U_FAILURE(err)) {
        errln("Locale::setDefault returned error code " + (int)err);
        return;
    }

    logln("With default = fr_FR...");
    logln("  In default locale...");
    doTestDisplayNames(empty, DLANG_FR, TRUE);
    logln("  In locale = en_US...");
    doTestDisplayNames(english, DLANG_EN, TRUE);
    logln("  In locale = fr_FR...");
    doTestDisplayNames(french, DLANG_FR, TRUE);
    logln("  In locale = hr_HR...");
    doTestDisplayNames(croatian, DLANG_HR, TRUE);
    logln("  In locale = el_GR...");
    doTestDisplayNames(greek, DLANG_EL, TRUE);

    Locale::setDefault(saveDefault, err);
    if (U_FAILURE(err)) {
        errln("Locale::setDefault returned error code " + (int)err);
        return;
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
            errln("FAIL: " + UnicodeString(#test) + " was not true." ); \
        else \
            logln("PASS: asserted " + UnicodeString(#test) ); \
    }

void LocaleTest::TestSimpleObjectStuff() {
    Locale  test1("aa", "AA");
    Locale  test2("aa", "AA");
    Locale  test3(test1);
    Locale  test4("zz", "ZZ");
    Locale  test5("aa", "AA", ""); 
    Locale  test6("aa", "AA", "Antares"); 
    Locale  test7("aa", "AA", "Jupiter");

    // now list them all for debugging usage.
    test_dumpLocale(test1);
    test_dumpLocale(test2);
    test_dumpLocale(test3);
    test_dumpLocale(test4);
    test_dumpLocale(test5);
    test_dumpLocale(test6);
    test_dumpLocale(test7)

    // Make sure things compare to themselves!
    test_assert(test1 == test1);
    test_assert(test2 == test2);
    test_assert(test3 == test3);
    test_assert(test4 == test4);
    test_assert(test5 == test5);
    test_assert(test6 == test6);
    test_assert(test7 == test7);

    // make sure things are not equal to themselves.
    test_assert(!(test1 != test1));
    test_assert(!(test2 != test2));
    test_assert(!(test3 != test3));
    test_assert(!(test4 != test4));
    test_assert(!(test5 != test5));
    test_assert(!(test6 != test6));
    test_assert(!(test7 != test7));

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
        setFromPOSIXID(l);
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
            logln(locList[i].getName(temp));
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

void LocaleTest::doTestDisplayNames(Locale& inLocale,
                                    int32_t compareIndex,
                                    bool_t defaultIsFrench) {
    UnicodeString   temp;
    
    if (defaultIsFrench && Locale::getDefault().getLanguage(temp) != "fr")
        errln("Default locale should be French, but it's really " + temp);
    else if (!defaultIsFrench && Locale::getDefault().getLanguage(temp) != "en")
        errln("Default locale should be English, but it's really " + temp);

    for (int32_t i = 0; i <= MAX_LOCALES; i++) {
        Locale testLocale(dataTable[LANG][i], dataTable[CTRY][i], dataTable[VAR][i]);
        logln("  Testing " + testLocale.getName(temp) + "...");

        UnicodeString  testLang;
        UnicodeString  testCtry;
        UnicodeString  testVar;
        UnicodeString  testName;

        if (inLocale == Locale("", "", "")) {
            testLocale.getDisplayLanguage(testLang);
            testLocale.getDisplayCountry(testCtry);
            testLocale.getDisplayVariant(testVar);
            testLocale.getDisplayName(testName);
        }
        else {
            testLocale.getDisplayLanguage(inLocale, testLang);
            testLocale.getDisplayCountry(inLocale, testCtry);
            testLocale.getDisplayVariant(inLocale, testVar);
            testLocale.getDisplayName(inLocale, testName);
        }

        UnicodeString  expectedLang;
        UnicodeString  expectedCtry;
        UnicodeString  expectedVar;
        UnicodeString  expectedName;

        expectedLang = dataTable[compareIndex][i];
        if (expectedLang.length() == 0 && defaultIsFrench)
            expectedLang = dataTable[DLANG_FR][i];
        if (expectedLang.length() == 0)
            expectedLang = dataTable[DLANG_EN][i];

        expectedCtry = dataTable[compareIndex + 1][i];
        if (expectedCtry.length() == 0 && defaultIsFrench)
            expectedCtry = dataTable[DCTRY_FR][i];
        if (expectedCtry.length() == 0)
            expectedCtry = dataTable[DCTRY_EN][i];

        expectedVar = dataTable[compareIndex + 2][i];
        if (expectedVar.length() == 0 && defaultIsFrench)
            expectedVar = dataTable[DVAR_FR][i];
        if (expectedVar.length() == 0)
            expectedVar = dataTable[DVAR_EN][i];

        expectedName = dataTable[compareIndex + 3][i];
        if (expectedName.length() == 0 && defaultIsFrench)
            expectedName = dataTable[DNAME_FR][i];
        if (expectedName.length() == 0)
            expectedName = dataTable[DNAME_EN][i];

        if (testLang != expectedLang)
            errln("Display language mismatch: " + testLang + " versus " + expectedLang);
        if (testCtry != expectedCtry)
            errln("Display country mismatch: " + testCtry + " versus " + expectedCtry);
        if (testVar != expectedVar)
            errln("Display variant mismatch: " + testVar + " versus " + expectedVar);
        if (testName != expectedName)
            errln("Display name mismatch: " + testName + " versus " + expectedName);
    }
}

//---------------------------------------------------
// table of valid data
//---------------------------------------------------

UnicodeString** LocaleTest::dataTable = 0;

char* rawData[27][7] = {

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
        {   "anglais",  "fran\\u00E7ais",   "", "grec",    "norv\\u00E9gien",    "italien", "xx" },
        // display country (French)
        {   "\\u00C9tats-Unis",    "France",   "",  "Gr\\u00E8ce",   "Norv\\u00E8ge", "",     "YY" },
        // display variant (French)
        {   "",     "",     "",     "",     "Nynorsk",     "",     "" },
        // display name (French)
        //{   "anglais (États-Unis)", "français (France)", "", "grec (Grèce)", "norvégien (Norvège,Nynorsk)", "italien", "xx (YY)" },
        {   "anglais (\\u00C9tats-Unis)", "fran\\u00E7ais (France)", "", "grec (Gr\\u00E8ce)", "norv\\u00E9gien (Norv\\u00E8ge, Nynorsk)", "italien", "xx (YY)" },

        // display langage (Croatian)
        {   "",  "", "hrvatski", "",    "", "", "xx" },
        // display country (Croatian)
        {   "",    "",   "Hrvatska",  "",   "", "", "YY" },
        // display variant (Croatian)
        {   "",     "",     "",     "",     "", "", ""},
        // display name (Croatian)
        {   "", "", "hrvatski (Hrvatska)", "", "",  "", "xx (YY)" },

        // display langage (Greek)[actual values listed below]
    {   "",  "", "", "",    "", "", "" },
        // display country (Greek)[actual values listed below]
    {   "",  "", "", "",    "", "", "" },
        // display variant (Greek)
        {   "",     "",     "",     "",     "", "", "" },
        // display name (Greek)[actual values listed below]
    {   "",  "", "", "",    "", "", "" },

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
    bool_t gotException = FALSE;
    UnicodeString result;
    UErrorCode err = U_ZERO_ERROR;

    //try {
        result = test.getISO3Language(result, err);
    //}
    //catch (MissingResourceException e) {
        if(U_FAILURE(err))
            gotException = TRUE;
    //}
    if (!gotException)
        errln("getISO3Language() on xx_YY returned " + result + " instead of throwing an exception");

    gotException = FALSE;
    err = U_ZERO_ERROR;
    //try {
        result = test.getISO3Country(result, err);
    //}
    //catch (MissingResourceException e) {
        if(U_FAILURE(err))
            gotException = TRUE;
    //}
    if (!gotException)
        errln("getISO3Country() on xx_YY returned " + result + " instead of throwing an exception");
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
    const UnicodeString *test = Locale::getISOLanguages(testCount);
    UnicodeString spotCheck1 [] = { "en", "es", "fr", "de", "it", 
                                    "ja", "ko", "zh", "th", "he", 
                                    "id", "iu", "ug", "yi", "za" };
    int32_t spotLen = 15;

    int32_t i;

    if (testCount != 142)
        errln("Expected getISOLanguages() to return 142 languages; it returned" + testCount);
    else {
        for (i = 0; i < 15; i++) {
            int32_t j;
            for (j = 0; j < testCount; j++)
                if (test[j] == spotCheck1[i])
                    break;
            if (j == testCount || test[j] != spotCheck1[i])
                errln("Couldn't find " + spotCheck1[i] + " in language list.");
        }
    }
    for (i = 0; i < testCount; i++) {
        UnicodeString lc(test[i]);
        if (test[i] != lc.toLower())
            errln(test[i] + " is not all lower case.");
        if (test[i].length() != 2)
            errln(test[i] + " is not two characters long.");
        if (i > 0 && test[i].compare(test[i - 1]) <= 0)
            errln(test[i] + " appears in an out-of-order position in the list.");
    }

    test = Locale::getISOCountries(testCount);
    UnicodeString spotCheck2 [] = { "US", "CA", "GB", "FR", "DE", 
                                    "IT", "JP", "KR", "CN", "TW", 
                                    "TH" };
    int32_t spot2Len = 11;


    if (testCount != 239)
        errln("Expected getISOLanguages to return 238 languages; it returned" + testCount);
    else {
        for (i = 0; i < spot2Len; i++) {
            int32_t j;
            for (j = 0; j < testCount; j++)
                if (test[j] == spotCheck2[i])
                    break;
            if (j == testCount || test[j] != spotCheck2[i])
                errln("Couldn't find " + spotCheck2[i] + " in country list.");
        }
    }
    for (i = 0; i < testCount; i++) {
        UnicodeString uc(test[i]);
        if (test[i] != uc.toUpper())
            errln(test[i] + " is not all upper case.");
        if (test[i].length() != 2)
            errln(test[i] + " is not two characters long.");
        if (i > 0 && test[i].compare(test[i - 1]) <= 0)
            errln(test[i] + " appears in an out-of-order position in the list.");
    }
}

/**
 * @bug 4126880
 */
// since this returns const UnicodeString* in C++, is this test applicable?
void 
LocaleTest::Test4126880() 
{
    /*
    const UnicodeString *test;
    int32_t testCount;

    test = Locale::getISOCountries(testCount);
    test[0] = "SUCKER!!!";
    test = Locale::getISOCountries(testCount);
    if (test[0] == "SUCKER!!!")
        errln("Changed internal country code list!");

    test = Locale::getISOLanguages(testCount);
    test[0] = "HAHAHAHA!!!";
    test = Locale::getISOLanguages(testCount);
    if (test[0] == "HAHAHAHA!!!") // Fixed typo
        errln("Changes internal language code list!");
*/
}

/**
 * @bug 4135316
 */
// not applicable in C++ - const Locale*
void 
LocaleTest::TestBug4135316() 
{
/*
    Locale[] locales1 = Locale.getAvailableLocales();
    Locale[] locales2 = Locale.getAvailableLocales();
    if (locales1 == locales2)
        errln("Locale.getAvailableLocales() doesn't clone its internal storage!");
*/
}

/**
 * @bug 4107953
 */
/*
test commented out pending API-change approval
public void TestGetLanguagesForCountry() {
    UnicodeString[] languages = Locale.getLanguagesForCountry("US");

    if (!searchStringArrayFor("en", languages))
        errln("Didn't get en as a language for US");

    languages = Locale.getLanguagesForCountry("FR");
    if (!searchStringArrayFor("fr", languages))
        errln("Didn't get fr as a language for FR");

    languages = Locale.getLanguagesForCountry("CH");
    if (!searchStringArrayFor("fr", languages))
        errln("Didn't get fr as a language for CH");
    if (!searchStringArrayFor("it", languages))
        errln("Didn't get it as a language for CH");
    if (!searchStringArrayFor("de", languages))
        errln("Didn't get de as a language for CH");

    languages = Locale.getLanguagesForCountry("JP");
    if (!searchStringArrayFor("ja", languages))
        errln("Didn't get ja as a language for JP");
}
*/

/*
private bool_t searchStringArrayFor(UnicodeString s, UnicodeString[] array) {
    for (int32_t i = 0; i < array.length; i++)
        if (s.equals(array[i]))
            return TRUE;
    return FALSE;
}
*/

/**
 * @bug 4110613
 */
// not applicable in C++
 /*
void 
LocaleTest::TestSerialization() throws ClassNotFoundException, OptionalDataException,
                IOException, StreamCorruptedException
{
    ObjectOutputStream ostream;
    ByteArrayOutputStream obstream;
    byte[] bytes = null;

    obstream = new ByteArrayOutputStream();
    ostream = new ObjectOutputStream(obstream);

    Locale test1 = new Locale("zh", "TW", "");
    int32_t dummy = test1.hashCode();   // fill in the cached hash-code value
    ostream.writeObject(test1);

    bytes = obstream.toByteArray();

    ObjectInputStream istream = new ObjectInputStream(new ByteArrayInputStream(bytes));

    Locale test2 = (Locale)(istream.readObject());

    if (!test1.equals(test2) || test1.hashCode() != test2.hashCode())
        errln("Locale failed to deserialize correctly.");
}
*/

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
    UnicodeString languageCodes [] = { "he", "id", "iu", "ug", "yi", "za" };
    UnicodeString languageNames [] = { "Hebrew", "Indonesian", "Inukitut", "Uighur", "Yiddish",
                               "Zhuang" };

    for (int32_t i = 0; i < 6; i++) {
        UnicodeString test;
        Locale l(languageCodes[i], "", "");
        l.getDisplayLanguage(Locale::US, test);
        if (test != languageNames[i])
            errln("Got wrong display name for " + languageCodes[i] + ": Expected \"" +
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
    UnicodeString iso2Languages [] = {     "am", "ba", "fy", "mr", "rn", 
                                        "ss", "tw", "zu" };
    UnicodeString iso3Languages [] = {     "amh", "bak", "fry", "mar", "run", 
                                        "ssw", "twi", "zul" };

    int32_t i;

    for (i = 0; i < 8; i++) {
        UnicodeString test;
        Locale l(iso2Languages[i], "", "");
        l.getISO3Language(test);
        if(test != iso3Languages[i])
            errln("Got wrong ISO3 code for " + iso2Languages[i] + ": Expected \"" +
                    iso3Languages[i] + "\", got \"" + test + "\".");
    }

    UnicodeString iso2Countries [] = {     "AF", "BW", "KZ", "MO", "MN", 
                                        "SB", "TC", "ZW" };
    UnicodeString iso3Countries [] = {     "AFG", "BWA", "KAZ", "MAC", "MNG", 
                                        "SLB", "TCA", "ZWE" };

    for (i = 0; i < 8; i++) {
        UnicodeString test;
        Locale l("", iso2Countries[i], "");
        l.getISO3Country(test);
        if (test != iso3Countries[i])
            errln("Got wrong ISO3 code for " + iso2Countries[i] + ": Expected \"" +
                    iso3Countries[i] + "\", got \"" + test + "\".");
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
                                    "japonais (South Africa)",
                                    "Russian (Mexico)",
                                     "anglais (France)",
                                     "espagnol (Allemagne)",
                                    "Croatia",
                                    CharsToUnicodeString("Su\\u00E8de"),
                                    "Dominican Republic",
                                    "Belgique" };
    UnicodeString rus("Russian (M");
    rus += (UChar32)0x00e9;
    rus += "xico)";

    UnicodeString esp("espa");
    esp += (UChar32)0x00f1;
    esp += "ol (Germany)";

    UnicodeString dr("Rep");
    dr += (UChar32)0x00fa;
    dr += "blica Dominicana";

    UnicodeString spanishDisplayNames [] = { "German (Canada)",
                                     "Japanese (South Africa)",
                                     rus,
                                     "English (France)",
                                     esp,
                                     "Croatia",
                                     "Sweden",
                                     dr,
                                     "Belgium" };

    int32_t i;
    UErrorCode status = U_ZERO_ERROR;
    Locale::setDefault(Locale::US, status);
    for (i = 0; i < 9; ++i) {
        UnicodeString name;
        localesToTest[i].getDisplayName(Locale::US, name);
        logln(name);
        if (name != englishDisplayNames[i])
        {
            errln("Lookup in English failed: expected \"" + englishDisplayNames[i]
                        + "\", got \"" + name + "\"");
            logln("Locale name was-> " + localesToTest[i].getName(name));
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
        localesToTest[i].getDisplayName(Locale::FRANCE, name);
        logln(name);
        if (name != frenchDisplayNames[i])
            errln("Lookup in French failed: expected \"" + frenchDisplayNames[i]
                        + "\", got \"" + name + "\"");
    }
}

/**
 * @bug 4126371
 */
// not applicable in C++, setDefault() takes a reference
void 
LocaleTest::TestNullDefault() 
{
/*
    // why on earth anyone would ever try to do this is beyond me, but we should
    // definitely make sure we don't let them
    bool_t gotException = FALSE;
    try {
        Locale.setDefault(null);
    }
    catch (NullPointerException e) {
        // all other exception types propagate through here back to the test harness
        gotException = TRUE;
    }
    if (Locale.getDefault() == null)
        errln("Locale.getDefault() allowed us to set default to NULL!");
    if (!gotException)
        errln("Trying to set default locale to NULL didn't throw exception!");
*/
}

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
 * DON'T ASSUME: Any specific countries support the Euro.  Instead,
 * iterate through all locales.
 */
void 
LocaleTest::TestEuroSupport() 
{
    const UnicodeString EURO_VARIANT("EURO");
    UChar euro = 0x20ac;
    const UnicodeString EURO_CURRENCY(&euro, 1, 1); // Look for this UnicodeString in formatted Euro currency

    UErrorCode status = U_ZERO_ERROR;

    UnicodeString temp;

    int32_t locCount = 0;
    const Locale *locales = NumberFormat::getAvailableLocales(locCount);      
    for (int32_t i=0; i < locCount; ++i) {
        Locale loc = locales[i];
        UnicodeString temp;
        if (loc.getVariant(temp).indexOf(EURO_VARIANT) >= 0) {
            NumberFormat *nf = NumberFormat::createCurrencyInstance(loc, status);
            UnicodeString pos;
            nf->format(271828.182845, pos);
            UnicodeString neg;
            nf->format(-271828.182845, neg);
            if (pos.indexOf(EURO_CURRENCY) >= 0 &&
                neg.indexOf(EURO_CURRENCY) >= 0) {
                logln("Ok: " + loc.getName(temp) +
                      ": " + pos + " / " + neg);
            }
            else {
                errln("Fail: " + loc.getName(temp) +
                      " formats without " + EURO_CURRENCY +
                      ": " + pos + " / " + neg +
                      "\n*** THIS FAILURE MAY ONLY MEAN THAT LOCALE DATA HAS CHANGED ***");
            }
        
            delete nf;
        }
    }
}

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

    UnicodeString DATA_S [] = {
        "xx",
        "_YY",
        "__ZZ",
        "xx_YY",
        "xx__ZZ",
        "_YY_ZZ",
        "xx_YY_ZZ",
    };
    
    for (int32_t i=0; i < 7; ++i) {
        UnicodeString name;
        if (DATA[i].getName(name) != DATA_S[i]) {
            errln("Fail: Locale.getName(), got:" + name + ", expected: " + DATA_S[i]);
        }
        else
            logln("Pass: Locale.getName(), got:" + name );
    }
}

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
    if (cal->getFirstDayOfWeek() != Calendar::MONDAY) {
        errln("Fail: First day of week in Russia should be Monday");
    }
    delete cal;
}

/**
 * @bug 4147315
 * java.util.Locale.getISO3Country() works wrong for non ISO-3166 codes.
 * Should throw an exception for unknown locales
 */
void 
LocaleTest::Test4147315() 
{
    // Try with codes that are the wrong length but happen to match text
    // at a valid offset in the mapping table
    Locale locale("aaa", "CCC");
    
    //try {
        UnicodeString result;
        UErrorCode err = U_ZERO_ERROR;
        locale.getISO3Country(result, err);

        UnicodeString temp;
        if(U_SUCCESS(err))
            errln("ERROR: getISO3Country() returns: " + result + 
                " for locale '" + locale.getName(temp) + "' rather than exception" );
    //} catch(MissingResourceException e) { }
}

/**
 * @bug 4147317
 * java.util.Locale.getISO3Language() works wrong for non ISO-3166 codes.
 * Should throw an exception for unknown locales
 */
void 
LocaleTest::Test4147317() 
{
    // Try with codes that are the wrong length but happen to match text
    // at a valid offset in the mapping table
    Locale locale("aaa", "CCC");
    
    //try {
        UnicodeString result;
        UErrorCode err = U_ZERO_ERROR;
        locale.getISO3Language(result, err);

        UnicodeString temp;
        if(U_SUCCESS(err))
            errln("ERROR: getISO3Language() returns: " + result + 
               " for locale '" + locale.getName(temp) + "' rather than exception" );
    //} catch(MissingResourceException e) { }
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
                                                 "norsk (Norge, Nynorsk)" 
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
    
    en_US_custom.getDisplayVariant(Locale::US, got);
    if(got != dispVar) {
        errln("FAIL: getDisplayVariant()");
        errln("Wanted: " + dispVar);
        errln("Got   : " + got);
    }

    en_US_custom.getDisplayName(Locale::US, got);
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

/**
 * @bug 4105828
 * Currency symbol in zh is wrong.  We will test this at the NumberFormat
 * end to test the whole pipe.
 */
void 
LocaleTest::Test4105828() 
{
    Locale LOC [] = { Locale::CHINESE,  Locale("zh", "CN", ""),
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
