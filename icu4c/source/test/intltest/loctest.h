/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-2003, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

#include "intltest.h"
#include "unicode/locid.h"

/**
 * Tests for the Locale class
 **/
class LocaleTest: public IntlTest {
public:
    LocaleTest();
    virtual ~LocaleTest();
    
    void runIndexedTest( int32_t index, UBool exec, const char* &name, char* par = NULL );

    /**
     * Test methods to set and get data fields
     **/
    void TestBasicGetters(void);
    /**
     * Test methods to set and get data fields
     **/
    void TestParallelAPIValues(void);
    /**
     * Use Locale to access Resource file data and compare against expected values
     **/
    void TestSimpleResourceInfo(void);
    /**
     * Use Locale to access Resource file display names and compare against expected values
     **/
    void TestDisplayNames(void);
    /**
     * Test methods for basic object behaviour
     **/
    void TestSimpleObjectStuff(void);
    /**
     * Test methods for POSIX parsing behavior
     **/
    void TestPOSIXParsing(void);
    /**
     * Test Locale::getAvailableLocales
     **/
    void TestGetAvailableLocales(void);
    /**
     * Test methods to set and access a custom data directory
     **/
    void TestDataDirectory(void);

    void TestISO3Fallback(void);
    void TestGetLangsAndCountries(void);
    void TestSimpleDisplayNames(void);
    void TestUninstalledISO3Names(void);
    void TestAtypicalLocales(void);
#if !UCONFIG_NO_FORMATTING
    void TestThaiCurrencyFormat(void);
    void TestEuroSupport(void);
#endif
    void TestToString(void);
#if !UCONFIG_NO_FORMATTING
    void Test4139940(void);
    void Test4143951(void);
#endif
    void Test4147315(void);
    void Test4147317(void);
    void Test4147552(void);
    
    void TestVariantParsing(void);
    
#if !UCONFIG_NO_FORMATTING
    void Test4105828(void) ;
#endif

    void TestSetIsBogus(void);

#if !UCONFIG_NO_FORMATTING
    static UDate date(int32_t y, int32_t m, int32_t d, int32_t hr = 0, int32_t min = 0, int32_t sec = 0);
#endif

private:
    /**
     * routine to perform subtests, used by TestDisplayNames
     **/
    void doTestDisplayNames(Locale& inLocale, int32_t compareIndex);
    /**
     * additional intialization for datatables storing expected values
     **/
    void setUpDataTable(void);

    UnicodeString** dataTable;
    
    enum {
        ENGLISH = 0,
        FRENCH = 1,
        CROATIAN = 2,
        GREEK = 3,
        NORWEGIAN = 4,
        MAX_LOCALES = 4
    };

    enum {
        LANG = 0,
        CTRY = 1,
        VAR = 2,
        NAME = 3,
        LANG3 = 4,
        CTRY3 = 5,
        LCID = 6,
        DLANG_EN = 7,
        DCTRY_EN = 8,
        DVAR_EN = 9,
        DNAME_EN = 10,
        DLANG_FR = 11,
        DCTRY_FR = 12,
        DVAR_FR = 13,
        DNAME_FR = 14,
        DLANG_HR = 15,
        DCTRY_HR = 16,
        DVAR_HR = 17,
        DNAME_HR = 18,
        DLANG_EL = 19,
        DCTRY_EL = 20,
        DVAR_EL = 21,
        DNAME_EL = 22,
        DLANG_RT = 23,
        DCTRY_RT = 24,
        DVAR_RT = 25,
        DNAME_RT = 26
    };
};



