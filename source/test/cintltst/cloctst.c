/********************************************************************
 * COPYRIGHT:
 * Copyright (c) 1997-2003, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/********************************************************************************
*
* File CLOCTST.C
*
* Modification History:
*        Name                     Description 
*     Madhu Katragadda            Ported for C API
*********************************************************************************
*/
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include "unicode/utypes.h"
#include "unicode/putil.h"
#include "cloctst.h"
#include "unicode/uloc.h"
#include "unicode/uscript.h"
#include "unicode/uchar.h"
#include "unicode/ustring.h"
#include "unicode/uset.h"
#include "cintltst.h"
#include "cstring.h"
#include "unicode/ures.h"

#define LENGTHOF(array) (int32_t)(sizeof(array)/sizeof((array)[0]))

#ifdef WIN32
#include "locmap.h"
#endif

static void TestNullDefault(void);
static void VerifyTranslation(void);
void PrintDataTable();

/*---------------------------------------------------
  table of valid data
 --------------------------------------------------- */
#define LOCALE_SIZE 5
#define LOCALE_INFO_SIZE 23

static const char* rawData2[LOCALE_INFO_SIZE][LOCALE_SIZE] = {
    /* language code */
    {   "en",   "fr",   "hr",   "el",   "no"    },
    /* country code */
    {   "US",   "FR",   "HR",   "GR",   "NO"    },
    /* variant code */
    {   "",     "",     "",     "",     "NY"    },
    /* full name */
    {   "en_US",    "fr_FR",    "hr_HR",    "el_GR",    "no_NO_NY"  },
    /* ISO-3 language */
    {   "eng",  "fra",  "hrv",  "ell",  "nor"   },
    /* ISO-3 country */
    {   "USA",  "FRA",  "HRV",  "GRC",  "NOR"   },
    /* LCID (not currently public) */
    {   "409", "40c", "41a", "408", "814"  },

    /* display language (English) */
    {   "English",  "French",   "Croatian", "Greek",    "Norwegian" },
    /* display country (English) */
    {   "United States",    "France",   "Croatia",  "Greece",   "Norway"    },
    /* display variant (English) */
    {   "",     "",     "",     "",     "Nynorsk"    },
    /* display name (English) */
    {   "English (United States)", "French (France)", "Croatian (Croatia)", "Greek (Greece)", "Norwegian (Norway, Nynorsk)" },

    /* display language (French) */
    {   "anglais",  "fran\\u00E7ais",   "croate", "grec",    "norv\\u00E9gien" },
    /* display country (French) */
    {   "\\u00C9tats-Unis",    "France",   "Croatie",  "Gr\\u00E8ce",   "Norv\\u00E8ge"    },
    /* display variant (French) */
    {   "",     "",     "",     "",     "Nynorsk"    },
    /* display name (French) */
    {   "anglais (\\u00C9tats-Unis)", "fran\\u00E7ais (France)", "croate (Croatie)", "grec (Gr\\u00E8ce)", "norv\\u00E9gien (Norv\\u00E8ge, Nynorsk)" },

    /* display language (Croatian) */
    {   "", "", "hrvatski",            "",  "" },
    /* display country (Croatian) */
    {   "", "", "Hrvatska",            "", "" },
    /* display variant (Croatian) */
    {   "", "", "",                    "", "Nynorsk" },
    /* display name (Croatian) */
    {   "", "", "hrvatski (Hrvatska)", "", "" },

    /* display language (Greek) */
    {
        "\\u0391\\u03b3\\u03b3\\u03bb\\u03b9\\u03ba\\u03ac",
        "\\u0393\\u03b1\\u03bb\\u03bb\\u03b9\\u03ba\\u03ac",
        "\\u039a\\u03c1\\u03bf\\u03b1\\u03c4\\u03b9\\u03ba\\u03ac",
        "\\u03b5\\u03bb\\u03bb\\u03b7\\u03bd\\u03b9\\u03ba\\u03ac",
        "\\u039d\\u03bf\\u03c1\\u03b2\\u03b7\\u03b3\\u03b9\\u03ba\\u03ac"
    },
    /* display country (Greek) */
    {
        "\\u0397\\u03bd\\u03c9\\u03bc\\u03ad\\u03bd\\u03b5\\u03c2 \\u03a0\\u03bf\\u03bb\\u03b9\\u03c4\\u03b5\\u03af\\u03b5\\u03c2 \\u0391\\u03bc\\u03b5\\u03c1\\u03b9\\u03ba\\u03ae\\u03c2",
        "\\u0393\\u03b1\\u03bb\\u03bb\\u03af\\u03b1",
        "\\u039a\\u03c1\\u03bf\\u03b1\\u03c4\\u03af\\u03b1",
        "\\u0395\\u03bb\\u03bb\\u03ac\\u03b4\\u03b1",
        "\\u039d\\u03bf\\u03c1\\u03b2\\u03b7\\u03b3\\u03af\\u03b1"
    },
    /* display variant (Greek) */
    {   "", "", "", "", "Nynorsk" },
    /* display name (Greek) */
    {
        "\\u0391\\u03b3\\u03b3\\u03bb\\u03b9\\u03ba\\u03ac (\\u0397\\u03bd\\u03c9\\u03bc\\u03ad\\u03bd\\u03b5\\u03c2 \\u03a0\\u03bf\\u03bb\\u03b9\\u03c4\\u03b5\\u03af\\u03b5\\u03c2 \\u0391\\u03bc\\u03b5\\u03c1\\u03b9\\u03ba\\u03ae\\u03c2)",
        "\\u0393\\u03b1\\u03bb\\u03bb\\u03b9\\u03ba\\u03ac (\\u0393\\u03b1\\u03bb\\u03bb\\u03af\\u03b1)",
        "\\u039a\\u03c1\\u03bf\\u03b1\\u03c4\\u03b9\\u03ba\\u03ac (\\u039a\\u03c1\\u03bf\\u03b1\\u03c4\\u03af\\u03b1)",
        "\\u03b5\\u03bb\\u03bb\\u03b7\\u03bd\\u03b9\\u03ba\\u03ac (\\u0395\\u03bb\\u03bb\\u03ac\\u03b4\\u03b1)",
        "\\u039d\\u03bf\\u03c1\\u03b2\\u03b7\\u03b3\\u03b9\\u03ba\\u03ac (\\u039d\\u03bf\\u03c1\\u03b2\\u03b7\\u03b3\\u03af\\u03b1, Nynorsk)"
    }
};

static UChar*** dataTable=0;
enum {
    ENGLISH = 0,
    FRENCH = 1,
    CROATIAN = 2,
    GREEK = 3,
    NORWEGIAN = 4
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
    DNAME_EL = 22
};

void addLocaleTest(TestNode** root);

void addLocaleTest(TestNode** root)
{
    addTest(root, &TestObsoleteNames,        "tsutil/cloctst/TestObsoleteNames"); /* srl- move */
    addTest(root, &TestBasicGetters,         "tsutil/cloctst/TestBasicGetters");
    addTest(root, &TestNullDefault,          "tsutil/cloctst/TestNullDefault");
    addTest(root, &TestPrefixes,             "tsutil/cloctst/TestPrefixes");
    addTest(root, &TestSimpleResourceInfo,   "tsutil/cloctst/TestSimpleResourceInfo");
    addTest(root, &TestDisplayNames,         "tsutil/cloctst/TestDisplayNames");
    addTest(root, &TestGetAvailableLocales,  "tsutil/cloctst/TestGetAvailableLocales");
    addTest(root, &TestDataDirectory,        "tsutil/cloctst/TestDataDirectory");
    addTest(root, &TestISOFunctions,         "tsutil/cloctst/TestISOFunctions");
    addTest(root, &TestISO3Fallback,         "tsutil/cloctst/TestISO3Fallback");
    addTest(root, &TestUninstalledISO3Names, "tsutil/cloctst/TestUninstalledISO3Names");
    addTest(root, &TestSimpleDisplayNames,   "tsutil/cloctst/TestSimpleDisplayNames");
    addTest(root, &TestVariantParsing,       "tsutil/cloctst/TestVariantParsing");
    addTest(root, &TestLocaleStructure,      "tsutil/cloctst/TestLocaleStructure");
    addTest(root, &TestConsistentCountryInfo,"tsutil/cloctst/TestConsistentCountryInfo");
    addTest(root, &VerifyTranslation,        "tsutil/cloctst/VerifyTranslation");
}


/* testing uloc(), uloc_getName(), uloc_getLanguage(), uloc_getVariant(), uloc_getCountry() */
static void TestBasicGetters() {
    int32_t i;
    int32_t cap;
    UErrorCode status = U_ZERO_ERROR;
    char *testLocale = 0;
    char *temp = 0, *name = 0;
    log_verbose("Testing Basic Getters\n");
    for (i = 0; i < LOCALE_SIZE; i++) {
        testLocale=(char*)malloc(sizeof(char) * (strlen(rawData2[NAME][i])+1));
        strcpy(testLocale,rawData2[NAME][i]);

        log_verbose("Testing   %s  .....\n", testLocale);
        cap=uloc_getLanguage(testLocale, NULL, 0, &status);
        if(status==U_BUFFER_OVERFLOW_ERROR){
            status=U_ZERO_ERROR;
            temp=(char*)malloc(sizeof(char) * (cap+1));
            uloc_getLanguage(testLocale, temp, cap+1, &status);
        }
        if(U_FAILURE(status)){
            log_err("ERROR: in uloc_getLanguage  %s\n", myErrorName(status));
        }
        if (0 !=strcmp(temp,rawData2[LANG][i]))    {
            log_err("  Language code mismatch: %s versus  %s\n", temp, rawData2[LANG][i]);
        }


        cap=uloc_getCountry(testLocale, temp, cap, &status);
        if(status==U_BUFFER_OVERFLOW_ERROR){
            status=U_ZERO_ERROR;
            temp=(char*)realloc(temp, sizeof(char) * (cap+1));
            uloc_getCountry(testLocale, temp, cap+1, &status);
        }
        if(U_FAILURE(status)){
            log_err("ERROR: in uloc_getCountry  %s\n", myErrorName(status));
        }
        if (0 != strcmp(temp, rawData2[CTRY][i])) {
            log_err(" Country code mismatch:  %s  versus   %s\n", temp, rawData2[CTRY][i]);

          }

        cap=uloc_getVariant(testLocale, temp, cap, &status);
        if(status==U_BUFFER_OVERFLOW_ERROR){
            status=U_ZERO_ERROR;
            temp=(char*)realloc(temp, sizeof(char) * (cap+1));
            uloc_getVariant(testLocale, temp, cap+1, &status);
        }
        if(U_FAILURE(status)){
            log_err("ERROR: in uloc_getVariant  %s\n", myErrorName(status));
        }
        if (0 != strcmp(temp, rawData2[VAR][i])) {
            log_err("Variant code mismatch:  %s  versus   %s\n", temp, rawData2[VAR][i]);
        }

        cap=uloc_getName(testLocale, NULL, 0, &status);
        if(status==U_BUFFER_OVERFLOW_ERROR){
            status=U_ZERO_ERROR;
            name=(char*)malloc(sizeof(char) * (cap+1));
            uloc_getName(testLocale, name, cap+1, &status);
        } else if(status==U_ZERO_ERROR) {
          log_err("ERROR: in uloc_getName(%s,NULL,0,..), expected U_BUFFER_OVERFLOW_ERROR!\n", testLocale);
        }
        if(U_FAILURE(status)){
            log_err("ERROR: in uloc_getName   %s\n", myErrorName(status));
        }
        if (0 != strcmp(name, rawData2[NAME][i])){
            log_err(" Mismatch in getName:  %s  versus   %s\n", name, rawData2[NAME][i]);
        }


        free(temp);
        free(name);

        free(testLocale);
    }
}

static void TestNullDefault() {
    UErrorCode status = U_ZERO_ERROR;
    char original[ULOC_FULLNAME_CAPACITY];

    uprv_strcpy(original, uloc_getDefault());
    uloc_setDefault("qq_BLA", &status);
    if (uprv_strcmp(uloc_getDefault(), "qq_BLA") != 0) {
        log_err(" Mismatch in uloc_setDefault:  qq_BLA  versus   %s\n", uloc_getDefault());
    }
    uloc_setDefault(NULL, &status);
    if (uprv_strcmp(uloc_getDefault(), original) != 0) {
        log_err(" uloc_setDefault(NULL, &status) didn't get the default locale back!\n");
    }
}
/* Test the i- and x- and @ and . functionality 
*/

#define PREFIXBUFSIZ 128

static void TestPrefixes() {
  int row = 0;
  int n;
  const char *loc;

  const char *testData[][5] =
  {
    {"sv", "FI", "AL", "sv-fi-al", "sv_FI_AL" },
    {"en", "GB", "", "en-gb", "en_GB" },
    {"i-hakka", "MT", "XEMXIJA", "i-hakka_MT_XEMXIJA", "i-hakka_MT_XEMXIJA"},
    {"i-hakka", "CN", "", "i-hakka_CN", "i-hakka_CN"},
    {"i-hakka", "MX", "", "I-hakka_MX", "i-hakka_MX"},
    {"x-klingon", "US", "SANJOSE", "X-KLINGON_us_SANJOSE", "x-klingon_US_SANJOSE"},

    {"mr", "", "", "mr.utf8", "mr"},
    {"de", "TV", "", "de-tv.koi8r", "de_TV"},
    {"x-piglatin", "ML", "", "x-piglatin_ML.MBE", "x-piglatin_ML"},  /* Multibyte English */
    {"i-cherokee","US", "", "i-Cherokee_US.utf7", "i-cherokee_US"},
    {"x-filfli", "MT", "FILFLA", "x-filfli_MT_FILFLA.gb-18030", "x-filfli_MT_FILFLA"},
    {"no", "NO", "NY", "no-no-ny.utf32@B", "no_NO_NY"}, /* @ ignored unless variant is empty */
    {"no", "NO", "B",  "no-no.utf32@B", "no_NO_B" },
    {"no", "",   "NY", "no__ny", "no__NY" },
    {"no", "",   "NY", "no@ny", "no__NY" },

    { "","","","",""}
  };

  const char *testTitles[] = { "uloc_getLanguage()", "uloc_getCountry()", "uloc_getVariant()", "name", "uloc_getName()", "country3", "lcid" };

  char buf[PREFIXBUFSIZ];
  int32_t len;
  UErrorCode err;


  for(row=0;testData[row][0][0] != 0;row++) {
    loc = testData[row][NAME];
    log_verbose("Test #%d: %s\n", row, loc);

    err = U_ZERO_ERROR;
    len=0;
    buf[0]=0;
    for(n=0;n<=(NAME+1);n++) {
      if(n==NAME) continue;

      for(len=0;len<PREFIXBUFSIZ;len++) {
        buf[len] = '%'; /* Set a tripwire.. */
      }
      len = 0;

      switch(n) {
      case LANG:
        len = uloc_getLanguage(loc, buf, PREFIXBUFSIZ, &err);
        break;

      case CTRY:
        len = uloc_getCountry(loc, buf, PREFIXBUFSIZ, &err);
        break;

      case VAR:
        len = uloc_getVariant(loc, buf, PREFIXBUFSIZ, &err);
        break;

      case NAME+1:
        len = uloc_getName(loc, buf, PREFIXBUFSIZ, &err);
        break;

      default:
        strcpy(buf, "**??");
        len=4;
      }

      if(U_FAILURE(err)) {
        log_err("#%d: %s on %s: err %s\n",
                row, testTitles[n], loc, u_errorName(err));
      } else {
        log_verbose("#%d: %s on %s: -> [%s] (length %d)\n",
                row, testTitles[n], loc, buf, len);

        if(len != (int32_t)strlen(buf)) {
          log_err("#%d: %s on %s: -> [%s] (length returned %d, actual %d!)\n",
                row, testTitles[n], loc, buf, len, strlen(buf)+1);

        }

        /* see if they smashed something */
        if(buf[len+1] != '%') {
          log_err("#%d: %s on %s: -> [%s] - wrote [%X] out ofbounds!\n",
                row, testTitles[n], loc, buf, buf[len+1]);
        }

        if(strcmp(buf, testData[row][n])) {
          log_err("#%d: %s on %s: -> [%s] (expected '%s'!)\n",
                row, testTitles[n], loc, buf, testData[row][n]);

        }
      }
    }
  }
}


/* testing uloc_getISO3Language(), uloc_getISO3Country(),  */
static void TestSimpleResourceInfo() {
    int32_t i;
    char* testLocale = 0;
    UChar* expected = 0;

    const char* temp;
    char            temp2[20];
    testLocale=(char*)malloc(sizeof(char) * 1);
    expected=(UChar*)malloc(sizeof(UChar) * 1);

setUpDataTable();
    log_verbose("Testing getISO3Language and getISO3Country\n");
    for (i = 0; i < LOCALE_SIZE; i++) {

        testLocale=(char*)realloc(testLocale, sizeof(char) * (u_strlen(dataTable[NAME][i])+1));
        u_austrcpy(testLocale, dataTable[NAME][i]);

        log_verbose("Testing   %s ......\n", testLocale);

        temp=uloc_getISO3Language(testLocale);
        expected=(UChar*)realloc(expected, sizeof(UChar) * (strlen(temp) + 1));
        u_uastrcpy(expected,temp);
        if (0 != u_strcmp(expected, dataTable[LANG3][i])) {
             log_err("  ISO-3 language code mismatch:  %s versus  %s\n",  austrdup(expected),
                                                            austrdup(dataTable[LANG3][i]));
        }

        temp=uloc_getISO3Country(testLocale);
        expected=(UChar*)realloc(expected, sizeof(UChar) * (strlen(temp) + 1));
        u_uastrcpy(expected,temp);
        if (0 != u_strcmp(expected, dataTable[CTRY3][i])) {
            log_err("  ISO-3 Country code mismatch:  %s versus  %s\n",  austrdup(expected),
                                                            austrdup(dataTable[CTRY3][i]));
        }
        sprintf(temp2, "%x", uloc_getLCID(testLocale));
        if (strcmp(temp2, rawData2[LCID][i]) != 0) {
            log_err("LCID mismatch: %s versus %s\n", temp2 , rawData2[LCID][i]);
        }

    }

 free(expected);
 free(testLocale);
cleanUpDataTable();
}

/*
 * Jitterbug 2439 -- markus 20030425
 *
 * The lookup of display names must not fall back through the default
 * locale because that yields useless results.
 */
static void TestDisplayNames()
{
    UChar buffer[100];
    UErrorCode errorCode;
    int32_t length;

    log_verbose("Testing getDisplayName for different locales\n");

    log_verbose("  In locale = en_US...\n");
    doTestDisplayNames("en_US", DLANG_EN);
    log_verbose("  In locale = fr_FR....\n");
    doTestDisplayNames("fr_FR", DLANG_FR);
    log_verbose("  In locale = hr_HR...\n");
    doTestDisplayNames("hr_HR", DLANG_HR);
    log_verbose("  In locale = gr_EL..\n");
    doTestDisplayNames("el_GR", DLANG_EL);

    /* test that the default locale has a display name for its own language */
    errorCode=U_ZERO_ERROR;
    length=uloc_getDisplayLanguage(NULL, NULL, buffer, LENGTHOF(buffer), &errorCode);
    if(U_FAILURE(errorCode) || (length<=3 && buffer[0]<=0x7f)) {
        /* check <=3 to reject getting the language code as a display name */
        log_err("unable to get a display string for the language of the default locale - %s\n", u_errorName(errorCode));
    }

    /* test that we get the language code itself for an unknown language, and a default warning */
    errorCode=U_ZERO_ERROR;
    length=uloc_getDisplayLanguage("qq", "rr", buffer, LENGTHOF(buffer), &errorCode);
    if(errorCode!=U_USING_DEFAULT_WARNING || length!=2 || buffer[0]!=0x71 || buffer[1]!=0x71) {
        log_err("error getting the display string for an unknown language - %s\n", u_errorName(errorCode));
    }
}


/* test for uloc_getAvialable()  and uloc_countAvilable()*/
static void TestGetAvailableLocales()
{

    const char *locList;
    int32_t locCount,i;

    log_verbose("Testing the no of avialable locales\n");
    locCount=uloc_countAvailable();
    if (locCount == 0)
        log_data_err("countAvailable() returned an empty list!\n");

    /* use something sensible w/o hardcoding the count */
    else if(locCount < 0){
        log_data_err("countAvailable() returned a wrong value!= %d\n", locCount);
    }
    else{
        log_info("Number of locales returned = %d\n", locCount);
    }
    for(i=0;i<locCount;i++){
        locList=uloc_getAvailable(i);

        log_verbose(" %s\n", locList);
    }
}

/* test for u_getDataDirectory, u_setDataDirectory, uloc_getISO3Language */
static void TestDataDirectory()
{

    char            oldDirectory[512];
    const char     *temp,*testValue1,*testValue2,*testValue3;
    const char path[40] ="d:\\icu\\source\\test\\intltest" U_FILE_SEP_STRING; /*give the required path */

    log_verbose("Testing getDataDirectory()\n");
    temp = u_getDataDirectory();
    strcpy(oldDirectory, temp);

    testValue1=uloc_getISO3Language("en_US");
    log_verbose("first fetch of language retrieved  %s\n", testValue1);

    if (0 != strcmp(testValue1,"eng")){
        log_err("Initial check of ISO3 language failed: expected \"eng\", got  %s \n", testValue1);
    }

    /*defining the path for DataDirectory */
    log_verbose("Testing setDataDirectory\n");
    u_setDataDirectory( path );
    if(strcmp(path, u_getDataDirectory())==0)
        log_verbose("setDataDirectory working fine\n");
    else
        log_err("Error in setDataDirectory. Directory not set correctly - came back as [%s], expected [%s]\n", u_getDataDirectory(), path);

    testValue2=uloc_getISO3Language("en_US");
    log_verbose("second fetch of language retrieved  %s \n", testValue2);

    u_setDataDirectory(oldDirectory);
    testValue3=uloc_getISO3Language("en_US");
    log_verbose("third fetch of language retrieved  %s \n", testValue3);

    if (0 != strcmp(testValue3,"eng")) {
       log_err("get/setDataDirectory() failed: expected \"eng\", got \" %s  \" \n", testValue3);
    }
}



/*=========================================================== */

static UChar _NUL=0;

static void doTestDisplayNames(const char* displayLocale, int32_t compareIndex)
{
    UErrorCode status = U_ZERO_ERROR;
    int32_t i;
    int32_t maxresultsize;

    const char *testLocale;


    UChar  *testLang  = 0;
    UChar  *testCtry = 0;
    UChar  *testVar = 0;
    UChar  *testName = 0;


    UChar*  expectedLang = 0;
    UChar*  expectedCtry = 0;
    UChar*  expectedVar = 0;
    UChar*  expectedName = 0;

setUpDataTable();

    for(i=0;i<LOCALE_SIZE; ++i)
    {
        testLocale=rawData2[NAME][i];

        log_verbose("Testing.....  %s\n", testLocale);

        maxresultsize=0;
        maxresultsize=uloc_getDisplayLanguage(testLocale, displayLocale, NULL, maxresultsize, &status);
        if(status==U_BUFFER_OVERFLOW_ERROR)
        {
            status=U_ZERO_ERROR;
            testLang=(UChar*)malloc(sizeof(UChar) * (maxresultsize+1));
            uloc_getDisplayLanguage(testLocale, displayLocale, testLang, maxresultsize + 1, &status);
        }
        else
        {
            testLang=&_NUL;
        }
        if(U_FAILURE(status)){
            log_err("Error in getDisplayLanguage()  %s\n", myErrorName(status));
        }

        maxresultsize=0;
        maxresultsize=uloc_getDisplayCountry(testLocale, displayLocale, NULL, maxresultsize, &status);
        if(status==U_BUFFER_OVERFLOW_ERROR)
        {
            status=U_ZERO_ERROR;
            testCtry=(UChar*)malloc(sizeof(UChar) * (maxresultsize+1));
            uloc_getDisplayCountry(testLocale, displayLocale, testCtry, maxresultsize + 1, &status);
        }
        else
        {
            testCtry=&_NUL;
        }
        if(U_FAILURE(status)){
            log_err("Error in getDisplayCountry()  %s\n", myErrorName(status));
        }

        maxresultsize=0;
        maxresultsize=uloc_getDisplayVariant(testLocale, displayLocale, NULL, maxresultsize, &status);
        if(status==U_BUFFER_OVERFLOW_ERROR)
        {
            status=U_ZERO_ERROR;
            testVar=(UChar*)malloc(sizeof(UChar) * (maxresultsize+1));
            uloc_getDisplayVariant(testLocale, displayLocale, testVar, maxresultsize + 1, &status);
        }
        else
        {
            testVar=&_NUL;
        }
        if(U_FAILURE(status)){
                log_err("Error in getDisplayVariant()  %s\n", myErrorName(status));
        }

        maxresultsize=0;
        maxresultsize=uloc_getDisplayName(testLocale, displayLocale, NULL, maxresultsize, &status);
        if(status==U_BUFFER_OVERFLOW_ERROR)
        {
            status=U_ZERO_ERROR;
            testName=(UChar*)malloc(sizeof(UChar) * (maxresultsize+1));
            uloc_getDisplayName(testLocale, displayLocale, testName, maxresultsize + 1, &status);
        }
        else
        {
            testName=&_NUL;
        }
        if(U_FAILURE(status)){
            log_err("Error in getDisplayName()  %s\n", myErrorName(status));
        }

        expectedLang=dataTable[compareIndex][i];
        if(u_strlen(expectedLang)== 0)
            expectedLang=dataTable[DLANG_EN][i];

        expectedCtry=dataTable[compareIndex + 1][i];
        if(u_strlen(expectedCtry)== 0)
            expectedCtry=dataTable[DCTRY_EN][i];

        expectedVar=dataTable[compareIndex + 2][i];
        if(u_strlen(expectedCtry)== 0)
            expectedVar=dataTable[DVAR_EN][i];

        expectedName=dataTable[compareIndex + 3][i];
        if(u_strlen(expectedName) == 0)
            expectedName=dataTable[DNAME_EN][i];

        if (0 !=u_strcmp(testLang,expectedLang))  {
            log_data_err(" Display Language mismatch: got %s expected %s displayLocale=%s\n", austrdup(testLang), austrdup(expectedLang), displayLocale);
        }

        if (0 != u_strcmp(testCtry,expectedCtry))   {
            log_data_err(" Display Country mismatch: got %s expected %s displayLocale=%s\n", austrdup(testCtry), austrdup(expectedCtry), displayLocale);
        }

        if (0 != u_strcmp(testVar,expectedVar))    {
            log_data_err(" Display Variant mismatch: got %s expected %s displayLocale=%s\n", austrdup(testVar), austrdup(expectedVar), displayLocale);
        }

        if(0 != u_strcmp(testName, expectedName))    {
            log_data_err(" Display Name mismatch: got %s expected %s displayLocale=%s\n", austrdup(testName), austrdup(expectedName), displayLocale);
        }

        if(testName!=&_NUL) {
            free(testName);
        }
        if(testLang!=&_NUL) {
            free(testLang);
        }
        if(testCtry!=&_NUL) {
            free(testCtry);
        }
        if(testVar!=&_NUL) {
            free(testVar);
        }
    }
cleanUpDataTable();
}

/* test for uloc_getISOLanguages, uloc_getISOCountries */
static void TestISOFunctions()
{
    const char* const* str=uloc_getISOLanguages();
    const char* const* str1=uloc_getISOCountries();
    const char* test;
    int32_t count  = 0;
    UBool done = FALSE;
    int32_t expect;

    /*  test getISOLanguages*/
    /*str=uloc_getISOLanguages(); */
    log_verbose("Testing ISO Languages: \n");

    while(!done)
    {
        if(*(str+count++) == 0)
        {
            done = TRUE;
        }
        else
        {
            test = *(str+count-1);
            if(!strcmp(test,"in"))
                log_err("FAIL getISOLanguages() has obsolete language code %s\n", test);
            if(!strcmp(test,"iw"))
                log_err("FAIL getISOLanguages() has obsolete language code %s\n", test);
            if(!strcmp(test,"ji"))
                log_err("FAIL getISOLanguages() has obsolete language code %s\n", test);
            if(!strcmp(test,"jw"))
                log_err("FAIL getISOLanguages() has obsolete language code %s\n", test);
            if(!strcmp(test,"sh"))
                log_err("FAIL getISOLanguages() has obsolete language code %s\n", test);
        }
    }
    count--;
    expect = 450;

    if(count!=expect) {
        log_err("There is an error in getISOLanguages, got %d, expected %d\n", count, expect);
    }

    log_verbose("Testing ISO Countries");
    count=0;
    done=FALSE;
    while(!done)
    {
        if(*(str1 + count++)==0)
        {
            done=TRUE;
        }
        else
        {
            test = *(str1+count-1);
            if(!strcmp(test,"FX"))
                log_err("FAIL getISOCountries() has obsolete country code %s\n", test);
            if(!strcmp(test,"ZR"))
                log_err("FAIL getISOCountries() has obsolete country code %s\n", test);
        }
    }
    count--;
    expect=239;
    if(count!=expect)
    {
        log_err("There is an error in getISOCountries, got %d, expected %d \n", count, expect);
    }
}

static void setUpDataTable()
{
    int32_t i,j;
    dataTable = (UChar***)(calloc(sizeof(UChar**),LOCALE_INFO_SIZE));

    for (i = 0; i < 23; i++) {
        dataTable[i] = (UChar**)(calloc(sizeof(UChar*),LOCALE_SIZE));
        for (j = 0; j < 5; j++){
            dataTable[i][j] = CharsToUChars(rawData2[i][j]);
        }
    }
}

static void cleanUpDataTable()
{
    int32_t i,j;
    if(dataTable != NULL) {
        for (i=0; i<LOCALE_INFO_SIZE; i++) {
            for(j = 0; j < LOCALE_SIZE; j++) {
                free(dataTable[i][j]);
            }
            free(dataTable[i]);
        }
        free(dataTable);
    }
    dataTable = NULL;
}

/**
 * @bug 4011756 4011380
 */
static void TestISO3Fallback()
{
    const char* test="xx_YY";

    const char * result;

    result = uloc_getISO3Language(test);

    /* Conform to C API usage  */

    if (!result || (result[0] != 0))
       log_err("getISO3Language() on xx_YY returned %s instead of \"\"");

    result = uloc_getISO3Country(test);

    if (!result || (result[0] != 0))
        log_err("getISO3Country() on xx_YY returned %s instead of \"\"");
}

/**
 * @bug 4118587
 */
static void TestSimpleDisplayNames()
{
  /*
     This test is different from TestDisplayNames because TestDisplayNames checks
     fallback behavior, combination of language and country names to form locale
     names, and other stuff like that.  This test just checks specific language
     and country codes to make sure we have the correct names for them.
  */
    char languageCodes[] [4] = { "he", "id", "iu", "ug", "yi", "za" };
    const char* languageNames [] = { "Hebrew", "Indonesian", "Inukitut", "Uighur", "Yiddish",
                               "Zhuang" };
    UErrorCode status=U_ZERO_ERROR;

    int32_t i;
    for (i = 0; i < 6; i++) {
        UChar *testLang=0;
        UChar *expectedLang=0;
        int size=0;
        size=uloc_getDisplayLanguage(languageCodes[i], "en_US", NULL, size, &status);
        if(status==U_BUFFER_OVERFLOW_ERROR) {
            status=U_ZERO_ERROR;
            testLang=(UChar*)malloc(sizeof(UChar) * (size + 1));
            uloc_getDisplayLanguage(languageCodes[i], "en_US", testLang, size + 1, &status);
        }
        expectedLang=(UChar*)malloc(sizeof(UChar) * (strlen(languageNames[i])+1));
        u_uastrcpy(expectedLang, languageNames[i]);
        if (u_strcmp(testLang, expectedLang) != 0)
            log_data_err("Got wrong display name for %s : Expected \"%s\", got \"%s\".\n",
                    languageCodes[i], languageNames[i], austrdup(testLang));
        free(testLang);
        free(expectedLang);
    }

}

/**
 * @bug 4118595
 */
static void TestUninstalledISO3Names()
{
  /* This test checks to make sure getISO3Language and getISO3Country work right
     even for locales that are not installed. */
    const char iso2Languages [][4] = {     "am", "ba", "fy", "mr", "rn",
                                        "ss", "tw", "zu" };
    const char iso3Languages [][5] = {     "amh", "bak", "fry", "mar", "run",
                                        "ssw", "twi", "zul" };
    char iso2Countries [][6] = {     "am_AF", "ba_BW", "fy_KZ", "mr_MO", "rn_MN",
                                        "ss_SB", "tw_TC", "zu_ZW" };
    char iso3Countries [][4] = {     "AFG", "BWA", "KAZ", "MAC", "MNG",
                                        "SLB", "TCA", "ZWE" };
    int32_t i;

    for (i = 0; i < 8; i++) {
      UErrorCode err = U_ZERO_ERROR;
      const char *test;
      test = uloc_getISO3Language(iso2Languages[i]);
      if(strcmp(test, iso3Languages[i]) !=0 || U_FAILURE(err))
         log_err("Got wrong ISO3 code for %s : Expected \"%s\", got \"%s\". %s\n",
                     iso2Languages[i], iso3Languages[i], test, myErrorName(err));
    }
    for (i = 0; i < 8; i++) {
      UErrorCode err = U_ZERO_ERROR;
      const char *test;
      test = uloc_getISO3Country(iso2Countries[i]);
      if(strcmp(test, iso3Countries[i]) !=0 || U_FAILURE(err))
         log_err("Got wrong ISO3 code for %s : Expected \"%s\", got \"%s\". %s\n",
                     iso2Countries[i], iso3Countries[i], test, myErrorName(err));
    }
}


static void TestVariantParsing()
{
    const char* en_US_custom="en_US_De Anza_Cupertino_California_United States_Earth";
    const char* dispName="English (United States, DE ANZA_CUPERTINO_CALIFORNIA_UNITED STATES_EARTH)";
    const char* dispVar="DE ANZA_CUPERTINO_CALIFORNIA_UNITED STATES_EARTH";
    const char* shortVariant="fr_FR_foo";
    const char* bogusVariant="fr_FR__foo";
    const char* bogusVariant2="fr_FR_foo_";
    const char* bogusVariant3="fr_FR__foo_";


    UChar displayVar[100];
    UChar displayName[100];
    UErrorCode status=U_ZERO_ERROR;
    UChar* got=0;
    int32_t size=0;
    size=uloc_getDisplayVariant(en_US_custom, "en_US", NULL, size, &status);
    if(status==U_BUFFER_OVERFLOW_ERROR) {
        status=U_ZERO_ERROR;
        got=(UChar*)realloc(got, sizeof(UChar) * (size+1));
        uloc_getDisplayVariant(en_US_custom, "en_US", got, size + 1, &status);
    }
    else {
        log_err("FAIL: Didn't get U_BUFFER_OVERFLOW_ERROR\n");
    }
    u_uastrcpy(displayVar, dispVar);
    if(u_strcmp(got,displayVar)!=0) {
        log_err("FAIL: getDisplayVariant() Wanted %s, got %s\n", dispVar, austrdup(got));
    }
    size=0;
    size=uloc_getDisplayName(en_US_custom, "en_US", NULL, size, &status);
    if(status==U_BUFFER_OVERFLOW_ERROR) {
        status=U_ZERO_ERROR;
        got=(UChar*)realloc(got, sizeof(UChar) * (size+1));
        uloc_getDisplayName(en_US_custom, "en_US", got, size + 1, &status);
    }
    else {
        log_err("FAIL: Didn't get U_BUFFER_OVERFLOW_ERROR\n");
    }
    u_uastrcpy(displayName, dispName);
    if(u_strcmp(got,displayName)!=0) {
        log_err("FAIL: getDisplayName() Wanted %s, got %s\n", dispName, austrdup(got));
    }

    size=0;
    status=U_ZERO_ERROR;
    size=uloc_getDisplayVariant(shortVariant, NULL, NULL, size, &status);
    if(status==U_BUFFER_OVERFLOW_ERROR) {
        status=U_ZERO_ERROR;
        got=(UChar*)realloc(got, sizeof(UChar) * (size+1));
        uloc_getDisplayVariant(shortVariant, NULL, got, size + 1, &status);
    }
    else {
        log_err("FAIL: Didn't get U_BUFFER_OVERFLOW_ERROR\n");
    }
    if(strcmp(austrdup(got),"FOO")!=0) {
        log_err("FAIL: getDisplayVariant()  Wanted: foo  Got: %s\n", austrdup(got));
    }
    size=0;
    status=U_ZERO_ERROR;
    size=uloc_getDisplayVariant(bogusVariant, NULL, NULL, size, &status);
    if(status==U_BUFFER_OVERFLOW_ERROR) {
        status=U_ZERO_ERROR;
        got=(UChar*)realloc(got, sizeof(UChar) * (size+1));
        uloc_getDisplayVariant(bogusVariant, NULL, got, size + 1, &status);
    }
    else {
        log_err("FAIL: Didn't get U_BUFFER_OVERFLOW_ERROR\n");
    }
    if(strcmp(austrdup(got),"_FOO")!=0) {
        log_err("FAIL: getDisplayVariant()  Wanted: _FOO  Got: %s\n", austrdup(got));
    }
    size=0;
    status=U_ZERO_ERROR;
    size=uloc_getDisplayVariant(bogusVariant2, NULL, NULL, size, &status);
    if(status==U_BUFFER_OVERFLOW_ERROR) {
        status=U_ZERO_ERROR;
        got=(UChar*)realloc(got, sizeof(UChar) * (size+1));
        uloc_getDisplayVariant(bogusVariant2, NULL, got, size + 1, &status);
    }
    else {
        log_err("FAIL: Didn't get U_BUFFER_OVERFLOW_ERROR\n");
    }
    if(strcmp(austrdup(got),"FOO_")!=0) {
        log_err("FAIL: getDisplayVariant()  Wanted: FOO_  Got: %s\n", austrdup(got));
    }
    size=0;
    status=U_ZERO_ERROR;
    size=uloc_getDisplayVariant(bogusVariant3, NULL, NULL, size, &status);
    if(status==U_BUFFER_OVERFLOW_ERROR) {
        status=U_ZERO_ERROR;
        got=(UChar*)realloc(got, sizeof(UChar) * (size+1));
        uloc_getDisplayVariant(bogusVariant3, NULL, got, size + 1, &status);
    }
    else {
        log_err("FAIL: Didn't get U_BUFFER_OVERFLOW_ERROR\n");
    }
    if(strcmp(austrdup(got),"_FOO_")!=0) {
        log_err("FAIL: getDisplayVariant()  Wanted: _FOO_  Got: %s\n", austrdup(got));
    }
    free(got);
}


static void TestObsoleteNames(void)
{
    int32_t i;
    UErrorCode status = U_ZERO_ERROR;
    char buff[256];

    struct
    {
        char locale[9];
        char lang3[6];
        char lang[6];
        char ctry3[6];
        char ctry[6];
    } tests[] =
    {
        { "eng_USA", "eng", "en", "USA", "US" },
        { "kok",  "kok", "kok", "", "" },
        { "in",  "ind", "in", "", "" },
        { "id",  "ind", "id", "", "" }, /* NO aliasing */
        { "sh",  "srp", "sh", "", "" },
        { "zz_FX",  "", "zz", "FXX", "FX" },
        { "zz_RO",  "", "zz", "ROU", "RO" },
        { "zz_TP",  "", "zz", "TMP", "TP" },
        { "zz_TL",  "", "zz", "TLS", "TL" },
        { "zz_ZR",  "", "zz", "ZAR", "ZR" },
        { "zz_FXX",  "", "zz", "FXX", "FX" }, /* no aliasing. Doesn't go to PS(PSE). */
        { "zz_ROM",  "", "zz", "ROU", "RO" },
        { "zz_ROU",  "", "zz", "ROU", "RO" },
        { "zz_ZAR",  "", "zz", "ZAR", "ZR" },
        { "zz_TMP",  "", "zz", "TMP", "TP" },
        { "zz_TLS",  "", "zz", "TLS", "TL" },
        { "mlt_PSE", "mlt", "mt", "PSE", "PS" },
        { "iw", "heb", "iw", "", "" },
        { "ji", "yid", "ji", "", "" },
        { "jw", "jaw", "jw", "", "" },
        { "sh", "srp", "sh", "", "" },
        { "", "", "", "", "" }
    };

    for(i=0;tests[i].locale[0];i++)
    {
        const char *locale;

        locale = tests[i].locale;
        log_verbose("** %s:\n", locale);

        status = U_ZERO_ERROR;
        if(strcmp(tests[i].lang3,uloc_getISO3Language(locale)))
        {
            log_err("FAIL: uloc_getISO3Language(%s)==\t\"%s\",\t expected \"%s\"\n",
                locale,  uloc_getISO3Language(locale), tests[i].lang3);
        }
        else
        {
            log_verbose("   uloc_getISO3Language()==\t\"%s\"\n",
                uloc_getISO3Language(locale) );
        }

        status = U_ZERO_ERROR;
        uloc_getLanguage(locale, buff, 256, &status);
        if(U_FAILURE(status))
        {
            log_err("FAIL: error getting language from %s\n", locale);
        }
        else
        {
            if(strcmp(buff,tests[i].lang))
            {
                log_err("FAIL: uloc_getLanguage(%s)==\t\"%s\"\t expected \"%s\"\n",
                    locale, buff, tests[i].lang);
            }
            else
            {
                log_verbose("  uloc_getLanguage(%s)==\t%s\n", locale, buff);
            }
        }
        if(strcmp(tests[i].lang3,uloc_getISO3Language(locale)))
        {
            log_err("FAIL: uloc_getISO3Language(%s)==\t\"%s\",\t expected \"%s\"\n",
                locale,  uloc_getISO3Language(locale), tests[i].lang3);
        }
        else
        {
            log_verbose("   uloc_getISO3Language()==\t\"%s\"\n",
                uloc_getISO3Language(locale) );
        }

        if(strcmp(tests[i].ctry3,uloc_getISO3Country(locale)))
        {
            log_err("FAIL: uloc_getISO3Country(%s)==\t\"%s\",\t expected \"%s\"\n",
                locale,  uloc_getISO3Country(locale), tests[i].ctry3);
        }
        else
        {
            log_verbose("   uloc_getISO3Country()==\t\"%s\"\n",
                uloc_getISO3Country(locale) );
        }

        status = U_ZERO_ERROR;
        uloc_getCountry(locale, buff, 256, &status);
        if(U_FAILURE(status))
        {
            log_err("FAIL: error getting country from %s\n", locale);
        }
        else
        {
            if(strcmp(buff,tests[i].ctry))
            {
                log_err("FAIL: uloc_getCountry(%s)==\t\"%s\"\t expected \"%s\"\n",
                    locale, buff, tests[i].ctry);
            }
            else
            {
                log_verbose("  uloc_getCountry(%s)==\t%s\n", locale, buff);
            }
        }
    }

#if 0

    i = uloc_getLanguage("kok",NULL,0,&icu_err);
    if(U_FAILURE(icu_err))
    {
        log_err("FAIL: Got %s trying to do uloc_getLanguage(kok)\n", u_errorName(icu_err));
    }

    icu_err = U_ZERO_ERROR;
    uloc_getLanguage("kok",r1_buff,12,&icu_err);
    if(U_FAILURE(icu_err))
    {
        log_err("FAIL: Got %s trying to do uloc_getLanguage(kok, buff)\n", u_errorName(icu_err));
    }

    r1_addr = (char *)uloc_getISO3Language("kok");

    icu_err = U_ZERO_ERROR;
    if (strcmp(r1_buff,"kok") != 0)
    {
        log_err("FAIL: uloc_getLanguage(kok)==%s not kok\n",r1_buff);
        line--;
    }
    r1_addr = (char *)uloc_getISO3Language("in");
    i = uloc_getLanguage(r1_addr,r1_buff,12,&icu_err);
    if (strcmp(r1_buff,"id") != 0)
    {
        printf("uloc_getLanguage error (%s)\n",r1_buff);
        line--;
    }
    r1_addr = (char *)uloc_getISO3Language("sh");
    i = uloc_getLanguage(r1_addr,r1_buff,12,&icu_err);
    if (strcmp(r1_buff,"sr") != 0)
    {
        printf("uloc_getLanguage error (%s)\n",r1_buff);
        line--;
    }

    r1_addr = (char *)uloc_getISO3Country("zz_ZR");
    strcpy(p1_buff,"zz_");
    strcat(p1_buff,r1_addr);
    i = uloc_getCountry(p1_buff,r1_buff,12,&icu_err);
    if (strcmp(r1_buff,"ZR") != 0)
    {
        printf("uloc_getCountry error (%s)\n",r1_buff);
        line--;
    }
    r1_addr = (char *)uloc_getISO3Country("zz_FX");
    strcpy(p1_buff,"zz_");
    strcat(p1_buff,r1_addr);
    i = uloc_getCountry(p1_buff,r1_buff,12,&icu_err);
    if (strcmp(r1_buff,"FX") != 0)
    {
        printf("uloc_getCountry error (%s)\n",r1_buff);
        line--;
    }

#endif

}

static void
TestKeyInRootRecursive(UResourceBundle *root, const char *rootName,
                       UResourceBundle *currentBundle, const char *locale) {
    UErrorCode errorCode = U_ZERO_ERROR;
    UResourceBundle *subRootBundle = NULL, *subBundle = NULL;

    ures_resetIterator(root);
    ures_resetIterator(currentBundle);
    while (ures_hasNext(currentBundle)) {
        const char *subBundleKey = NULL;
        const char *currentBundleKey = NULL;

        errorCode = U_ZERO_ERROR;
        subBundle = ures_getNextResource(currentBundle, NULL, &errorCode);
        if (U_FAILURE(errorCode)) {
            log_err("Can't open a resource for locale %s\n", locale);
            continue;
        }
        subBundleKey = ures_getKey(subBundle);
        currentBundleKey = ures_getKey(currentBundle);

        subRootBundle = ures_getByKey(root, subBundleKey, NULL, &errorCode);
        if (U_FAILURE(errorCode)) {
/*            if (ures_hasNext(root)) {
                errorCode = U_ZERO_ERROR;
                subRootBundle = ures_getNextResource(root, NULL, &errorCode);
            }
            if (errorCode!=U_ZERO_ERROR) {
                if (ures_getKey(currentBundle) != 0 && strcmp(ures_getKey(currentBundle), "zoneStrings") == 0) {
                    break;
                }
                else {*/
                    if (subBundleKey == NULL
                        || (strcmp(subBundleKey, "TransliterateLATIN") != 0 /* Ignore these special cases */
                        && strcmp(subBundleKey, "BreakDictionaryData") != 0))
                    {
                        UBool isRoot = strcmp(rootName, "root") == 0;
                        UBool isSpecial = FALSE;
                        if (currentBundleKey) {
                            isSpecial = strcmp(currentBundleKey, "Currencies") == 0
                                || strcmp(currentBundleKey, "Languages") == 0
                                || strcmp(currentBundleKey, "Countries") == 0;
                        }

                        if ((isRoot && !isSpecial)
                            || (!isRoot && isSpecial))
                        {
                            log_err("Can't open a resource with key \"%s\" in \"%s\" from %s for locale \"%s\"\n",
                                    subBundleKey,
                                    ures_getKey(currentBundle),
                                    rootName,
                                    locale);
                        }
                    }
                    ures_close(subBundle);
                    continue;
/*                }
            }*/
        }
        if (ures_getType(subRootBundle) != ures_getType(subBundle)) {
            log_err("key \"%s\" in \"%s\" has a different type from root for locale \"%s\"\n"
                    "\troot=%d, locale=%d\n",
                    subBundleKey,
                    ures_getKey(currentBundle),
                    locale,
                    ures_getType(subRootBundle),
                    ures_getType(subBundle));
            continue;
        }
        else if (ures_getType(subBundle) == URES_INT_VECTOR) {
            int32_t minSize;
            int32_t subBundleSize;
            int32_t idx;
            UBool sameArray = TRUE;
            const int32_t *subRootBundleArr = ures_getIntVector(subRootBundle, &minSize, &errorCode);
            const int32_t *subBundleArr = ures_getIntVector(subBundle, &subBundleSize, &errorCode);

            if (minSize > subBundleSize) {
                minSize = subBundleSize;
                log_err("Arrays are different size with key \"%s\" in \"%s\" from root for locale \"%s\"\n",
                        subBundleKey,
                        ures_getKey(currentBundle),
                        locale);
            }

            for (idx = 0; idx < minSize && sameArray; idx++) {
                if (subRootBundleArr[idx] != subBundleArr[idx]) {
                    sameArray = FALSE;
                }
                if (strcmp(subBundleKey, "DateTimeElements") == 0
                    && (subBundleArr[idx] < 1 || 7 < subBundleArr[idx]))
                {
                    log_err("Value out of range with key \"%s\" at index %d in \"%s\" for locale \"%s\"\n",
                            subBundleKey,
                            idx,
                            ures_getKey(currentBundle),
                            locale);
                }
            }
            /* Special exception es_US and DateTimeElements */
            if (sameArray
                && !(strcmp(locale, "es_US") == 0 && strcmp(subBundleKey, "DateTimeElements") == 0))
            {
                log_err("Integer vectors are the same with key \"%s\" in \"%s\" from root for locale \"%s\"\n",
                        subBundleKey,
                        ures_getKey(currentBundle),
                        locale);
            }
        }
        else if (ures_getType(subBundle) == URES_ARRAY) {
            UResourceBundle *subSubBundle = ures_getByIndex(subBundle, 0, NULL, &errorCode);
            UResourceBundle *subSubRootBundle = ures_getByIndex(subRootBundle, 0, NULL, &errorCode);

            if (U_SUCCESS(errorCode)
                && (ures_getType(subSubBundle) == URES_ARRAY || ures_getType(subSubRootBundle) == URES_ARRAY))
            {
                /* TODO: Properly check for 2D arrays and zoneStrings */
                if (subBundleKey != NULL && strcmp(subBundleKey, "zoneStrings") == 0) {
/*                    int32_t minSize = ures_getSize(subBundle);
                    int32_t idx;

                    for (idx = 0; idx < minSize; idx++) {
                        UResourceBundle *subSubBundleAtIndex = ures_getByIndex(subBundle, idx, NULL, &errorCode);
                        if (ures_getSize(subSubBundleAtIndex) != 6) {
                            log_err("zoneStrings at index %d has wrong size for locale \"%s\". array size=%d\n",
                                    idx,
                                    locale,
                                    ures_getSize(subSubBundleAtIndex));
                        }
                        ures_close(subSubBundleAtIndex);
                    }*/
                }
                else {
                    /* Here is one of the recursive parts */
                    TestKeyInRootRecursive(subRootBundle, rootName, subBundle, locale);
                }
            }
            else {
                int32_t minSize = ures_getSize(subRootBundle);
                int32_t idx;
                UBool sameArray = TRUE;

                if (minSize > ures_getSize(subBundle)) {
                    minSize = ures_getSize(subBundle);
                }

                if ((subBundleKey == NULL
                    || (subBundleKey != NULL && strcmp(subBundleKey, "LocaleScript") != 0))
                    && ures_getSize(subRootBundle) != ures_getSize(subBundle))
                {
                    log_err("Different size array with key \"%s\" in \"%s\" from root for locale \"%s\"\n"
                            "\troot array size=%d, locale array size=%d\n",
                            subBundleKey,
                            ures_getKey(currentBundle),
                            locale,
                            ures_getSize(subRootBundle),
                            ures_getSize(subBundle));
                }

                for (idx = 0; idx < minSize; idx++) {
                    int32_t rootStrLen, localeStrLen;
                    const UChar *rootStr = ures_getStringByIndex(subRootBundle,idx,&rootStrLen,&errorCode);
                    const UChar *localeStr = ures_getStringByIndex(subBundle,idx,&localeStrLen,&errorCode);
                    if (rootStr && localeStr && U_SUCCESS(errorCode)) {
                        if (u_strcmp(rootStr, localeStr) != 0) {
                            sameArray = FALSE;
                        }
                    }
                    else {
                        log_err("Got a NULL string with key \"%s\" in \"%s\" at index %d for root or locale \"%s\"\n",
                                subBundleKey,
                                ures_getKey(currentBundle),
                                idx,
                                locale);
                        continue;
                    }
                    if (localeStr[0] == (UChar)0x20) {
                        log_err("key \"%s\" at index %d in \"%s\" starts with a space in locale \"%s\"\n",
                                subBundleKey,
                                idx,
                                ures_getKey(currentBundle),
                                locale);
                    }
                    else if (localeStr[localeStrLen - 1] == (UChar)0x20) {
                        log_err("key \"%s\" at index %d in \"%s\" ends with a space in locale \"%s\"\n",
                                subBundleKey,
                                idx,
                                ures_getKey(currentBundle),
                                locale);
                    }
                    else if (subBundleKey != NULL
                        && strcmp(subBundleKey, "DateTimePatterns") == 0)
                    {
                        int32_t quoted = 0;
                        const UChar *localeStrItr = localeStr;
                        while (*localeStrItr) {
                            if (*localeStrItr == (UChar)0x27 /* ' */) {
                                quoted++;
                            }
                            else if ((quoted % 2) == 0) {
                                /* Search for unquoted characters */
                                if (4 <= idx && idx <= 7
                                    && (*localeStrItr == (UChar)0x6B /* k */
                                    || *localeStrItr == (UChar)0x48 /* H */
                                    || *localeStrItr == (UChar)0x6D /* m */
                                    || *localeStrItr == (UChar)0x73 /* s */
                                    || *localeStrItr == (UChar)0x53 /* S */
                                    || *localeStrItr == (UChar)0x61 /* a */
                                    || *localeStrItr == (UChar)0x68 /* h */
                                    || *localeStrItr == (UChar)0x7A /* z */))
                                {
                                    log_err("key \"%s\" at index %d has time pattern chars in date for locale \"%s\"\n",
                                            subBundleKey,
                                            idx,
                                            locale);
                                }
                                else if (0 <= idx && idx <= 3
                                    && (*localeStrItr == (UChar)0x47 /* G */
                                    || *localeStrItr == (UChar)0x79 /* y */
                                    || *localeStrItr == (UChar)0x4D /* M */
                                    || *localeStrItr == (UChar)0x64 /* d */
                                    || *localeStrItr == (UChar)0x45 /* E */
                                    || *localeStrItr == (UChar)0x44 /* D */
                                    || *localeStrItr == (UChar)0x46 /* F */
                                    || *localeStrItr == (UChar)0x77 /* w */
                                    || *localeStrItr == (UChar)0x57 /* W */))
                                {
                                    log_err("key \"%s\" at index %d has date pattern chars in time for locale \"%s\"\n",
                                            subBundleKey,
                                            idx,
                                            locale);
                                }
                            }
                            localeStrItr++;
                        }
                    }
                    else if (idx == 4 && subBundleKey != NULL
                        && strcmp(subBundleKey, "NumberElements") == 0
                        && u_charDigitValue(localeStr[0]) != 0)
                    {
                        log_err("key \"%s\" at index %d has a non-zero based number for locale \"%s\"\n",
                                subBundleKey,
                                idx,
                                locale);
                    }
                }
                if (sameArray && strcmp(rootName, "root") == 0) {
                    log_err("Arrays are the same with key \"%s\" in \"%s\" from root for locale \"%s\"\n",
                            subBundleKey,
                            ures_getKey(currentBundle),
                            locale);
                }
            }
            ures_close(subSubBundle);
            ures_close(subSubRootBundle);
        }
        else if (ures_getType(subBundle) == URES_STRING) {
            int32_t len = 0;
            const UChar *string = ures_getString(subBundle, &len, &errorCode);
            if (U_FAILURE(errorCode) || string == NULL) {
                log_err("Can't open a string with key \"%s\" in \"%s\" for locale \"%s\"\n",
                        subBundleKey,
                        ures_getKey(currentBundle),
                        locale);
            } else if (string[0] == (UChar)0x20) {
                log_err("key \"%s\" in \"%s\" starts with a space in locale \"%s\"\n",
                        subBundleKey,
                        ures_getKey(currentBundle),
                        locale);
            } else if (string[len - 1] == (UChar)0x20) {
                log_err("key \"%s\" in \"%s\" ends with a space in locale \"%s\"\n",
                        subBundleKey,
                        ures_getKey(currentBundle),
                        locale);
            } else if (strcmp(subBundleKey, "localPatternChars") == 0 && len != 20) {
                log_err("key \"%s\" has the wrong number of characters in locale \"%s\"\n",
                        subBundleKey,
                        locale);
            }
            /* No fallback was done. Check for duplicate data */
            /* The ures_* API does not do fallback of sub-resource bundles,
               So we can't do this now. */
            else if (strcmp(locale, "root") != 0 && errorCode == U_ZERO_ERROR) {

                const UChar *rootString = ures_getString(subRootBundle, &len, &errorCode);
                if (U_FAILURE(errorCode) || rootString == NULL) {
                    log_err("Can't open a string with key \"%s\" in \"%s\" in root\n",
                            ures_getKey(subRootBundle),
                            ures_getKey(currentBundle));
                    continue;
                } else if (u_strcmp(string, rootString) == 0) {
                    if (strcmp(locale, "de_CH") != 0 && strcmp(subBundleKey, "Countries") != 0) {
                        log_err("Found duplicate data with key \"%s\" in \"%s\" in locale \"%s\"\n",
                                ures_getKey(subRootBundle),
                                ures_getKey(currentBundle),
                                locale);
                    }
                    else {
                        /* Ignore for now. */
                        /* Can be fixed if fallback through de locale was done. */
                        log_verbose("Skipping key %s in %s\n", subBundleKey, locale);
                    }
                }
            }
        }
        else if (ures_getType(subBundle) == URES_TABLE) {
            /* Here is one of the recursive parts */
            TestKeyInRootRecursive(subRootBundle, rootName, subBundle, locale);
        }
        else if (ures_getType(subBundle) == URES_BINARY || ures_getType(subBundle) == URES_INT) {
            /* Can't do anything to check it */
            /* We'll assume it's all correct */
            if (strcmp(subBundleKey, "LocaleID") != 0) {
                log_verbose("Skipping key \"%s\" in \"%s\" for locale \"%s\"\n",
                        subBundleKey,
                        ures_getKey(currentBundle),
                        locale);
            }
            /* Testing for LocaleID is done in testLCID */
        }
        else {
            log_err("Type %d for key \"%s\" in \"%s\" is unknown for locale \"%s\"\n",
                    ures_getType(subBundle),
                    subBundleKey,
                    ures_getKey(currentBundle),
                    locale);
        }
        ures_close(subRootBundle);
        ures_close(subBundle);
    }
}


#ifdef WIN32

static void
testLCID(UResourceBundle *currentBundle,
         const char *localeName)
{
    UErrorCode status = U_ZERO_ERROR;
    uint32_t lcid;
    uint32_t expectedLCID;
    char lcidStringC[64] = {0};
    int32_t lcidStringLen = 0;
    const UChar *lcidString = NULL;
    UResourceBundle *localeID = ures_getByKey(currentBundle, "LocaleID", NULL, &status);

    expectedLCID = ures_getInt(localeID, &status);
    ures_close(localeID);

    if (U_FAILURE(status)) {
        log_err("ERROR:   %s does not have a LocaleID (%s)\n",
            localeName, u_errorName(status));
        return;
    }

    lcid = uprv_convertToLCID(localeName, &status);
    if (U_FAILURE(status)) {
        if (expectedLCID == 0) {
            log_verbose("INFO:    %-5s does not have any LCID mapping\n",
                localeName);
        }
        else {
            log_err("ERROR:   %-5s does not have an LCID mapping to 0x%.4X\n",
                localeName, expectedLCID);
        }
        return;
    }

    status = U_ZERO_ERROR;
    uprv_strcpy(lcidStringC, uprv_convertToPosix(expectedLCID, &status));
    if (U_FAILURE(status)) {
        log_err("ERROR:   %.4x does not have a POSIX mapping due to %s\n",
            expectedLCID, u_errorName(status));
    }

    if(lcid != expectedLCID) {
        log_err("ERROR:   %-5s wrongfully has 0x%.4x instead of 0x%.4x for LCID\n",
            localeName, expectedLCID, lcid);
    }
    if(strcmp(localeName, lcidStringC) != 0) {
        char langName[1024];
        char langLCID[1024];
        uloc_getLanguage(localeName, langName, sizeof(langName), &status);
        uloc_getLanguage(lcidStringC, langLCID, sizeof(langLCID), &status);

        if (expectedLCID == lcid && strcmp(langName, langLCID) == 0) {
            log_verbose("WARNING: %-5s resolves to %s (0x%.4x)\n",
                localeName, lcidStringC, lcid);
        }
        else if (expectedLCID == lcid) {
            log_err("ERROR:   %-5s has 0x%.4x and the number resolves wrongfully to %s\n",
                localeName, expectedLCID, lcidStringC);
        }
        else {
            log_err("ERROR:   %-5s has 0x%.4x and the number resolves wrongfully to %s. It should be 0x%x.\n",
                localeName, expectedLCID, lcidStringC, lcid);
        }
    }
}

#endif

static void
TestLocaleStructure(void) {
    UResourceBundle *root, *completeLoc, *currentLocale, *subtable, *completeSubtable;
    int32_t locCount = uloc_countAvailable();
    int32_t locIndex;
    UErrorCode errorCode = U_ZERO_ERROR;
    const char *currLoc;

    /* TODO: Compare against parent's data too. This code can't handle fallbacks that some tools do already. */
/*    char locName[ULOC_FULLNAME_CAPACITY];
    char *locNamePtr;

    for (locIndex = 0; locIndex < locCount; locIndex++) {
        errorCode=U_ZERO_ERROR;
        strcpy(locName, uloc_getAvailable(locIndex));
        locNamePtr = strrchr(locName, '_');
        if (locNamePtr) {
            *locNamePtr = 0;
        }
        else {
            strcpy(locName, "root");
        }

        root = ures_openDirect(NULL, locName, &errorCode);
        if(U_FAILURE(errorCode)) {
            log_err("Can't open %s\n", locName);
            continue;
        }
*/
    if (locCount <= 1) {
        log_data_err("At least root needs to be installed\n");
    }

    root = ures_openDirect(NULL, "root", &errorCode);
    if(U_FAILURE(errorCode)) {
        log_data_err("Can't open root\n");
        return;
    }
    completeLoc = ures_openDirect(NULL, "en", &errorCode);
    if(U_FAILURE(errorCode)) {
        log_data_err("Can't open en\n");
        return;
    }
    for (locIndex = 0; locIndex < locCount; locIndex++) {
        errorCode=U_ZERO_ERROR;
        currLoc = uloc_getAvailable(locIndex);
        currentLocale = ures_open(NULL, currLoc, &errorCode);
        if(errorCode != U_ZERO_ERROR) {
            if(U_SUCCESS(errorCode)) {
                /* It's installed, but there is no data.
                   It's installed for the g18n white paper [grhoten] */
                log_err("ERROR: Locale %-5s not installed, and it should be!\n",
                    uloc_getAvailable(locIndex));
            } else {
                log_err("%%%%%%% Unexpected error %d in %s %%%%%%%",
                    u_errorName(errorCode),
                    uloc_getAvailable(locIndex));
            }
            ures_close(currentLocale);
            continue;
        }
        ures_getStringByKey(currentLocale, "Version", NULL, &errorCode);
        if(errorCode != U_ZERO_ERROR) {
            log_err("No version information is available for locale %s, and it should be!\n",
                currLoc);
        }
        else if (ures_getStringByKey(currentLocale, "Version", NULL, &errorCode)[0] == (UChar)(0x78)) {
            log_verbose("WARNING: The locale %s is experimental! It shouldn't be listed as an installed locale.\n",
                currLoc);
        }
        TestKeyInRootRecursive(root, "root", currentLocale, currLoc);

        completeSubtable = ures_getByKey(completeLoc, "Currencies", NULL, &errorCode);
        subtable = ures_getByKey(currentLocale, "Currencies", NULL, &errorCode);
        TestKeyInRootRecursive(completeSubtable, "en", subtable, currLoc);

#ifdef WIN32
        testLCID(currentLocale, currLoc);
#endif

        ures_close(completeSubtable);
        ures_close(subtable);
        ures_close(currentLocale);
    }

    ures_close(root);
    ures_close(completeLoc);
}

static void
compareArrays(const char *keyName,
              UResourceBundle *fromArray, const char *fromLocale,
              UResourceBundle *toArray, const char *toLocale,
              int32_t start, int32_t end)
{
    int32_t fromSize = ures_getSize(fromArray);
    int32_t toSize = ures_getSize(fromArray);
    int32_t idx;
    UErrorCode errorCode = U_ZERO_ERROR;

    if (fromSize > toSize) {
        fromSize = toSize;
        log_err("Arrays are different size from \"%s\" to \"%s\"\n",
                fromLocale,
                toLocale);
    }

    for (idx = start; idx <= end; idx++) {
        const UChar *fromBundleStr = ures_getStringByIndex(fromArray, idx, NULL, &errorCode);
        const UChar *toBundleStr = ures_getStringByIndex(toArray, idx, NULL, &errorCode);
        if (fromBundleStr && toBundleStr && u_strcmp(fromBundleStr, toBundleStr) != 0)
        {
            log_err("Difference for %s at index %d from %s= \"%s\" to %s= \"%s\"\n",
                    keyName,
                    idx,
                    fromLocale,
                    austrdup(fromBundleStr),
                    toLocale,
                    austrdup(toBundleStr));
        }
    }
}

static void
compareConsistentCountryInfo(const char *fromLocale, const char *toLocale) {
    UErrorCode errorCode = U_ZERO_ERROR;
    UResourceBundle *fromDateTimeElements, *toDateTimeElements;
    UResourceBundle *fromArray, *toArray;
    UResourceBundle *fromLocaleBund = ures_open(NULL, fromLocale, &errorCode);
    UResourceBundle *toLocaleBund = ures_open(NULL, toLocale, &errorCode);

    if(U_FAILURE(errorCode)) {
        log_err("Can't open resource bundle %s or %s - %s\n", fromLocale, toLocale, u_errorName(errorCode));
        return;
    }

    fromDateTimeElements = ures_getByKey(fromLocaleBund, "DateTimeElements", NULL, &errorCode);
    toDateTimeElements = ures_getByKey(toLocaleBund, "DateTimeElements", NULL, &errorCode);
    if (strcmp(fromLocale, "ar_IN") != 0)
    {
        int32_t fromSize;
        int32_t toSize;
        int32_t idx;
        const int32_t *fromBundleArr = ures_getIntVector(fromDateTimeElements, &fromSize, &errorCode);
        const int32_t *toBundleArr = ures_getIntVector(toDateTimeElements, &toSize, &errorCode);

        if (fromSize > toSize) {
            fromSize = toSize;
            log_err("Arrays are different size with key \"DateTimeElements\" from \"%s\" to \"%s\"\n",
                    fromLocale,
                    toLocale);
        }

        for (idx = 0; idx < fromSize; idx++) {
            if (fromBundleArr[idx] != toBundleArr[idx]) {
                log_err("Difference with key \"DateTimeElements\" at index %d from \"%s\" to \"%s\"\n",
                        idx,
                        fromLocale,
                        toLocale);
            }
        }
    }
    ures_close(fromDateTimeElements);
    ures_close(toDateTimeElements);

    fromArray = ures_getByKey(fromLocaleBund, "CurrencyElements", NULL, &errorCode);
    toArray = ures_getByKey(toLocaleBund, "CurrencyElements", NULL, &errorCode);
    if (strcmp(fromLocale, "en_CA") != 0)
    {
        /* The first one is probably localized. */
        compareArrays("CurrencyElements", fromArray, fromLocale, toArray, toLocale, 1, 2);
    }
    ures_close(fromArray);
    ures_close(toArray);

    fromArray = ures_getByKey(fromLocaleBund, "NumberPatterns", NULL, &errorCode);
    toArray = ures_getByKey(toLocaleBund, "NumberPatterns", NULL, &errorCode);
    if (strcmp(fromLocale, "en_CA") != 0)
    {
        compareArrays("NumberPatterns", fromArray, fromLocale, toArray, toLocale, 0, 3);
    }
    ures_close(fromArray);
    ures_close(toArray);

    /* Difficult to test properly */
/*
    fromArray = ures_getByKey(fromLocaleBund, "DateTimePatterns", NULL, &errorCode);
    toArray = ures_getByKey(toLocaleBund, "DateTimePatterns", NULL, &errorCode);
    {
        compareArrays("DateTimePatterns", fromArray, fromLocale, toArray, toLocale);
    }
    ures_close(fromArray);
    ures_close(toArray);*/

    fromArray = ures_getByKey(fromLocaleBund, "NumberElements", NULL, &errorCode);
    toArray = ures_getByKey(toLocaleBund, "NumberElements", NULL, &errorCode);
    if (strcmp(fromLocale, "en_CA") != 0)
    {
        compareArrays("NumberElements", fromArray, fromLocale, toArray, toLocale, 0, 3);
        /* Index 4 is a script based 0 */
        compareArrays("NumberElements", fromArray, fromLocale, toArray, toLocale, 5, 10);
    }
    ures_close(fromArray);
    ures_close(toArray);

    ures_close(fromLocaleBund);
    ures_close(toLocaleBund);
}

static void
TestConsistentCountryInfo(void) {
/*    UResourceBundle *fromLocale, *toLocale;*/
    int32_t locCount = uloc_countAvailable();
    int32_t fromLocIndex, toLocIndex;

    int32_t fromCountryLen, toCountryLen;
    char fromCountry[ULOC_FULLNAME_CAPACITY], toCountry[ULOC_FULLNAME_CAPACITY];

    int32_t fromVariantLen, toVariantLen;
    char fromVariant[ULOC_FULLNAME_CAPACITY], toVariant[ULOC_FULLNAME_CAPACITY];

    UErrorCode errorCode = U_ZERO_ERROR;

    for (fromLocIndex = 0; fromLocIndex < locCount; fromLocIndex++) {
        const char *fromLocale = uloc_getAvailable(fromLocIndex);

        errorCode=U_ZERO_ERROR;
        fromCountryLen = uloc_getCountry(fromLocale, fromCountry, ULOC_FULLNAME_CAPACITY, &errorCode);
        if (fromCountryLen <= 0) {
            /* Ignore countryless locales */
            continue;
        }
        fromVariantLen = uloc_getVariant(fromLocale, fromVariant, ULOC_FULLNAME_CAPACITY, &errorCode);
        if (fromVariantLen > 0) {
            /* Most variants are ignorable like PREEURO, or collation variants. */
            continue;
        }
        /* Start comparing only after the current index.
           Previous loop should have already compared fromLocIndex.
        */
        for (toLocIndex = fromLocIndex + 1; toLocIndex < locCount; toLocIndex++) {
            const char *toLocale = uloc_getAvailable(toLocIndex);

            toCountryLen = uloc_getCountry(toLocale, toCountry, ULOC_FULLNAME_CAPACITY, &errorCode);
            if(U_FAILURE(errorCode)) {
                log_err("Unknown failure fromLocale=%s toLocale=%s errorCode=%s\n",
                    fromLocale, toLocale, u_errorName(errorCode));
                continue;
            }

            if (toCountryLen <= 0) {
                /* Ignore countryless locales */
                continue;
            }
            toVariantLen = uloc_getVariant(toLocale, toVariant, ULOC_FULLNAME_CAPACITY, &errorCode);
            if (toVariantLen > 0) {
                /* Most variants are ignorable like PREEURO, or collation variants. */
                /* They're a variant for a reason. */
                continue;
            }
            if (strcmp(fromCountry, toCountry) == 0) {
                log_verbose("comparing fromLocale=%s toLocale=%s\n",
                    fromLocale, toLocale);
                compareConsistentCountryInfo(fromLocale, toLocale);
            }
        }
    }
}

static int32_t
findStringSetMismatch(const UChar *string, int32_t langSize,
                      const UChar *exemplarCharacters, int32_t exemplarLen,
                      UBool ignoreNumbers) {
    UErrorCode errorCode = U_ZERO_ERROR;
    USet *exemplarSet = uset_openPatternOptions(exemplarCharacters, exemplarLen, USET_CASE_INSENSITIVE, &errorCode);
    int32_t strIdx;
    if (U_FAILURE(errorCode)) {
        log_err("error uset_openPattern returned %s\n", u_errorName(errorCode));
        return -1;
    }

    for (strIdx = 0; strIdx < langSize; strIdx++) {
        if (!uset_contains(exemplarSet, string[strIdx])
            && string[strIdx] != 0x0020 && string[strIdx] != 0x002e && string[strIdx] != 0x002c && string[strIdx] != 0x002d && string[strIdx] != 0x0027) {
            if (!ignoreNumbers || (ignoreNumbers && (string[strIdx] < 0x30 || string[strIdx] > 0x39))) {
                return strIdx;
            }
        }
    }
    uset_close(exemplarSet);
    return -1;
}

static void VerifyTranslation(void) {
    UResourceBundle *root, *currentLocale;
    int32_t locCount = uloc_countAvailable();
    int32_t locIndex;
    UErrorCode errorCode = U_ZERO_ERROR;
    int32_t exemplarLen;
    const UChar *exemplarCharacters;
    const char *currLoc;
    UScriptCode scripts[USCRIPT_CODE_LIMIT];
    int32_t numScripts;
    int32_t idx;
    int32_t end;
    UResourceBundle *resArray;

    if (locCount <= 1) {
        log_data_err("At least root needs to be installed\n");
    }

    root = ures_openDirect(NULL, "root", &errorCode);
    if(U_FAILURE(errorCode)) {
        log_data_err("Can't open root\n");
        return;
    }
    for (locIndex = 0; locIndex < locCount; locIndex++) {
        errorCode=U_ZERO_ERROR;
        currLoc = uloc_getAvailable(locIndex);
        currentLocale = ures_open(NULL, currLoc, &errorCode);
        if(errorCode != U_ZERO_ERROR) {
            if(U_SUCCESS(errorCode)) {
                /* It's installed, but there is no data.
                   It's installed for the g18n white paper [grhoten] */
                log_err("ERROR: Locale %-5s not installed, and it should be!\n",
                    uloc_getAvailable(locIndex));
            } else {
                log_err("%%%%%%% Unexpected error %d in %s %%%%%%%",
                    u_errorName(errorCode),
                    uloc_getAvailable(locIndex));
            }
            ures_close(currentLocale);
            continue;
        }
        exemplarCharacters = ures_getStringByKey(currentLocale, "ExemplarCharacters", &exemplarLen, &errorCode);
        if (U_FAILURE(errorCode)) {
            log_err("error ures_getStringByKey returned %s\n", u_errorName(errorCode));
        }
        else if (QUICK && exemplarLen > 2048) {
            log_verbose("skipping test for %s\n", currLoc);
        }
        else {
            UChar langBuffer[128];
            int32_t langSize;
            int32_t strIdx;
            langSize = uloc_getDisplayLanguage(currLoc, currLoc, langBuffer, sizeof(langBuffer)/sizeof(langBuffer[0]), &errorCode);
            if (U_FAILURE(errorCode)) {
                log_err("error uloc_getDisplayLanguage returned %s\n", u_errorName(errorCode));
            }
            else {
                strIdx = findStringSetMismatch(langBuffer, langSize, exemplarCharacters, exemplarLen, FALSE);
                if (strIdx >= 0) {
                    log_err("getDisplayLanguage(%s) at index %d returned characters not in the exemplar characters.\n",
                        currLoc, strIdx);
                }
            }
            langSize = uloc_getDisplayCountry(currLoc, currLoc, langBuffer, sizeof(langBuffer)/sizeof(langBuffer[0]), &errorCode);
            if (U_FAILURE(errorCode)) {
                log_err("error uloc_getDisplayCountry returned %s\n", u_errorName(errorCode));
            }
            else {
                strIdx = findStringSetMismatch(langBuffer, langSize, exemplarCharacters, exemplarLen, FALSE);
                if (strIdx >= 0) {
                    log_err("getDisplayCountry(%s) at index %d returned characters not in the exemplar characters.\n",
                        currLoc, strIdx);
                }
            }

            resArray = ures_getByKey(currentLocale, "DayNames", NULL, &errorCode);
            if (U_FAILURE(errorCode)) {
                log_err("error ures_getByKey returned %s\n", u_errorName(errorCode));
            }
            if (QUICK) {
                end = 1;
            }
            else {
                end = ures_getSize(resArray);
            }


            for (idx = 0; idx < end; idx++) {
                const UChar *fromBundleStr = ures_getStringByIndex(resArray, idx, &langSize, &errorCode);
                if (U_FAILURE(errorCode)) {
                    log_err("error ures_getStringByIndex(%d) returned %s\n", idx, u_errorName(errorCode));
                    continue;
                }
                strIdx = findStringSetMismatch(fromBundleStr, langSize, exemplarCharacters, exemplarLen, TRUE);
                if (strIdx >= 0) {
                    log_err("getDayNames(%s, %d) at index %d returned characters not in the exemplar characters.\n",
                        currLoc, idx, strIdx);
                }
            }
            ures_close(resArray);

            resArray = ures_getByKey(currentLocale, "MonthNames", NULL, &errorCode);
            if (U_FAILURE(errorCode)) {
                log_err("error ures_getByKey returned %s\n", u_errorName(errorCode));
            }
            if (QUICK) {
                end = 1;
            }
            else {
                end = ures_getSize(resArray);
            }

            for (idx = 0; idx < end; idx++) {
                const UChar *fromBundleStr = ures_getStringByIndex(resArray, idx, &langSize, &errorCode);
                if (U_FAILURE(errorCode)) {
                    log_err("error ures_getStringByIndex(%d) returned %s\n", idx, u_errorName(errorCode));
                    continue;
                }
                strIdx = findStringSetMismatch(fromBundleStr, langSize, exemplarCharacters, exemplarLen, TRUE);
                if (strIdx >= 0) {
                    log_err("getMonthNames(%s, %d) at index %d returned characters not in the exemplar characters.\n",
                        currLoc, idx, strIdx);
                }
            }
            ures_close(resArray);

            errorCode = U_ZERO_ERROR;
            numScripts = uscript_getCode(currLoc, scripts, sizeof(scripts)/sizeof(scripts[0]), &errorCode);
            if (numScripts == 0) {
                log_err("uscript_getCode(%s) doesn't work.\n", currLoc);
            }
            /* TODO: test that the scripts are a superset of exemplar characters. */
        }
        ures_close(currentLocale);
    }

    ures_close(root);
}
