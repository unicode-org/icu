/********************************************************************
 * COPYRIGHT:
 * Copyright (c) 1997-2004, International Business Machines Corporation and
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
#include "cloctst.h"
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include "cintltst.h"
#include "cmemory.h"
#include "cstring.h"
#include "locmap.h"
#include "uresimp.h"
#include "uassert.h"

#include "unicode/putil.h"
#include "unicode/ubrk.h"
#include "unicode/uchar.h"
#include "unicode/ucol.h"
#include "unicode/udat.h"
#include "unicode/uloc.h"
#include "unicode/ulocdata.h"
#include "unicode/umsg.h"
#include "unicode/ures.h"
#include "unicode/uscript.h"
#include "unicode/uset.h"
#include "unicode/ustring.h"
#include "unicode/utypes.h"
#include "unicode/uversion.h"
#include "unicode/ulocdata.h"

#define LENGTHOF(array) (int32_t)(sizeof(array)/sizeof((array)[0]))

static void TestNullDefault(void);
static void VerifyTranslation(void);
static void TestExemplarSet(void);
void PrintDataTable();

/*---------------------------------------------------
  table of valid data
 --------------------------------------------------- */
#define LOCALE_SIZE 9
#define LOCALE_INFO_SIZE 28

static const char* rawData2[LOCALE_INFO_SIZE][LOCALE_SIZE] = {
    /* language code */
    {   "en",   "fr",   "ca",   "el",   "no",   "zh",   "de",   "es",  "ja"    },
    /* script code */
    {   "",     "",     "",     "",     "",     "Hans", "", "", ""  },
    /* country code */
    {   "US",   "FR",   "ES",   "GR",   "NO",   "CN", "DE", "", "JP"    },
    /* variant code */
    {   "",     "",     "",     "",     "NY",   "", "", "", ""      },
    /* full name */
    {   "en_US",    "fr_FR",    "ca_ES",    
        "el_GR",    "no_NO_NY", "zh_Hans_CN", 
        "de_DE@collation=phonebook", "es@collation=traditional",  "ja_JP@calendar=japanese" },
    /* ISO-3 language */
    {   "eng",  "fra",  "cat",  "ell",  "nor",  "zho", "deu", "spa", "jpn"   },
    /* ISO-3 country */
    {   "USA",  "FRA",  "ESP",  "GRC",  "NOR",  "CHN", "DEU", "", "JPN"   },
    /* LCID */
    {   "409", "40c", "403", "408", "814",  "804", "407", "a", "411"     },

    /* display language (English) */
    {   "English",  "French",   "Catalan", "Greek",    "Norwegian", "Chinese", "German", "Spanish", "Japanese"    },
    /* display script code (English) */
    {   "",     "",     "",     "",     "",     "Simplified Han", "", "", ""       },
    /* display country (English) */
    {   "United States",    "France",   "Spain",  "Greece",   "Norway", "China", "Germany", "", "Japan"       },
    /* display variant (English) */
    {   "",     "",     "",     "",     "NY",  "", "", "", ""       },
    /* display name (English) */
    {   "English (United States)", "French (France)", "Catalan (Spain)", 
        "Greek (Greece)", "Norwegian (Norway, NY)", "Chinese (Simplified Han, China)", 
        "German (Germany, Collation=Phonebook Order)", "Spanish (Collation=Traditional)", "Japanese (Japan, Calendar=Japanese Calendar)" },

    /* display language (French) */
    {   "anglais",  "fran\\u00E7ais",   "catalan", "grec",    "norv\\u00E9gien",    "chinois", "allemand", "espagnol", "japonais"     },
    /* display script code (French) */
    {   "",     "",     "",     "",     "",     "han simplifi\\u00E9", "", "", ""         },
    /* display country (French) */
    {   "\\u00C9tats-Unis",    "France",   "Espagne",  "Gr\\u00E8ce",   "Norv\\u00E8ge",    "Chine", "Allemagne", "", "Japon"       },
    /* display variant (French) */
    {   "",     "",     "",     "",     "NY",   "", "", "", ""       },
    /* display name (French) */
    {   "anglais (\\u00C9tats-Unis)", "fran\\u00E7ais (France)", "catalan (Espagne)", 
        "grec (Gr\\u00E8ce)", "norv\\u00E9gien (Norv\\u00E8ge, NY)",  "chinois (han simplifi\\u00E9, Chine)", 
        "allemand (Allemagne, Ordonnancement=Ordre de l\\u2019annuaire)", "espagnol (Ordonnancement=Ordre traditionnel)", "japonais (Japon, Calendrier=Calendrier japonais)" },

    /* display language (Catalan) */
    {   "angl\\u00E8s", "franc\\u00E8s", "catal\\u00E0", "grec",  "noruec", "xin\\u00E9s", "alemany", "espanyol", "japon\\u00E8s"    },
    /* display script code (Catalan) */
    {   "",     "",     "",     "",     "",     "Hans", "", "", ""         },
    /* display country (Catalan) */
    {   "Estats Units", "Fran\\u00E7a", "Espanya",  "Gr\\u00E8cia", "Noruega",  "Xina", "Alemanya", "", "Jap\\u00F3"    },
    /* display variant (Catalan) */
    {   "", "", "",                    "", "NY",    "", "", "", ""    },
    /* display name (Catalan) */
    {   "angl\\u00E8s (Estats Units)", "franc\\u00E8s (Fran\\u00E7a)", "catal\\u00E0 (Espanya)", 
    "grec (Gr\\u00E8cia)", "noruec (Noruega, NY)", "xin\\u00E9s (Hans, Xina)", 
    "alemany (Alemanya, COLLATION=PHONEBOOK)", "espanyol (COLLATION=TRADITIONAL)", "japon\\u00E8s (Jap\\u00F3, CALENDAR=JAPANESE)" },

    /* display language (Greek) */
    {
        "\\u0391\\u03b3\\u03b3\\u03bb\\u03b9\\u03ba\\u03ac",
        "\\u0393\\u03b1\\u03bb\\u03bb\\u03b9\\u03ba\\u03ac",
        "\\u039a\\u03b1\\u03c4\\u03b1\\u03bb\\u03b1\\u03bd\\u03b9\\u03ba\\u03ac",
        "\\u0395\\u03bb\\u03bb\\u03b7\\u03bd\\u03b9\\u03ba\\u03ac",
        "\\u039d\\u03bf\\u03c1\\u03b2\\u03b7\\u03b3\\u03b9\\u03ba\\u03ac",
        "\\u039A\\u03B9\\u03BD\\u03B5\\u03B6\\u03B9\\u03BA\\u03AC", 
        "\\u0393\\u03B5\\u03C1\\u03BC\\u03B1\\u03BD\\u03B9\\u03BA\\u03AC", 
        "\\u0399\\u03C3\\u03C0\\u03B1\\u03BD\\u03B9\\u03BA\\u03AC", 
        "\\u0399\\u03B1\\u03C0\\u03C9\\u03BD\\u03B9\\u03BA\\u03AC"   
    },
    /* display script code (Greek) */
    {   "",     "",     "",     "",     "",     "Hans", "", "", ""         },
    /* display country (Greek) */
    {
        "\\u0397\\u03bd\\u03c9\\u03bc\\u03ad\\u03bd\\u03b5\\u03c2 \\u03a0\\u03bf\\u03bb\\u03b9\\u03c4\\u03b5\\u03af\\u03b5\\u03c2",
        "\\u0393\\u03b1\\u03bb\\u03bb\\u03af\\u03b1",
        "\\u0399\\u03c3\\u03c0\\u03b1\\u03bd\\u03af\\u03b1",
        "\\u0395\\u03bb\\u03bb\\u03ac\\u03b4\\u03b1",
        "\\u039d\\u03bf\\u03c1\\u03b2\\u03b7\\u03b3\\u03af\\u03b1",
        "\\u039A\\u03AF\\u03BD\\u03B1", 
        "\\u0393\\u03B5\\u03C1\\u03BC\\u03B1\\u03BD\\u03AF\\u03B1", 
        "", 
        "\\u0399\\u03B1\\u03C0\\u03C9\\u03BD\\u03AF\\u03B1"   
    },
    /* display variant (Greek) */
    {   "", "", "", "", "NY", "", "", "", ""    }, /* TODO: currently there is no translation for NY in Greek fix this test when we have it */
    /* display name (Greek) */
    {
        "\\u0391\\u03b3\\u03b3\\u03bb\\u03b9\\u03ba\\u03ac (\\u0397\\u03bd\\u03c9\\u03bc\\u03ad\\u03bd\\u03b5\\u03c2 \\u03a0\\u03bf\\u03bb\\u03b9\\u03c4\\u03b5\\u03af\\u03b5\\u03c2)",
        "\\u0393\\u03b1\\u03bb\\u03bb\\u03b9\\u03ba\\u03ac (\\u0393\\u03b1\\u03bb\\u03bb\\u03af\\u03b1)",
        "\\u039a\\u03b1\\u03c4\\u03b1\\u03bb\\u03b1\\u03bd\\u03b9\\u03ba\\u03ac (\\u0399\\u03c3\\u03c0\\u03b1\\u03bd\\u03af\\u03b1)",
        "\\u0395\\u03bb\\u03bb\\u03b7\\u03bd\\u03b9\\u03ba\\u03ac (\\u0395\\u03bb\\u03bb\\u03ac\\u03b4\\u03b1)",
        "\\u039d\\u03bf\\u03c1\\u03b2\\u03b7\\u03b3\\u03b9\\u03ba\\u03ac (\\u039d\\u03bf\\u03c1\\u03b2\\u03b7\\u03b3\\u03af\\u03b1, NY)",
        "\\u039A\\u03B9\\u03BD\\u03B5\\u03B6\\u03B9\\u03BA\\u03AC (Hans, \\u039A\\u03AF\\u03BD\\u03B1)", 
        "\\u0393\\u03B5\\u03C1\\u03BC\\u03B1\\u03BD\\u03B9\\u03BA\\u03AC (\\u0393\\u03B5\\u03C1\\u03BC\\u03B1\\u03BD\\u03AF\\u03B1, COLLATION=PHONEBOOK)", 
        "\\u0399\\u03C3\\u03C0\\u03B1\\u03BD\\u03B9\\u03BA\\u03AC (COLLATION=TRADITIONAL)", 
        "\\u0399\\u03B1\\u03C0\\u03C9\\u03BD\\u03B9\\u03BA\\u03AC (\\u0399\\u03B1\\u03C0\\u03C9\\u03BD\\u03AF\\u03B1, CALENDAR=JAPANESE)"
    }
};

static UChar*** dataTable=0;
enum {
    ENGLISH = 0,
    FRENCH = 1,
    CATALAN = 2,
    GREEK = 3,
    NORWEGIAN = 4
};

enum {
    LANG = 0,
    SCRIPT = 1,
    CTRY = 2,
    VAR = 3,
    NAME = 4,
    LANG3 = 5,
    CTRY3 = 6,
    LCID = 7,
    DLANG_EN = 8,
    DSCRIPT_EN = 9,
    DCTRY_EN = 10,
    DVAR_EN = 11,
    DNAME_EN = 12,
    DLANG_FR = 13,
    DSCRIPT_FR = 14,
    DCTRY_FR = 15,
    DVAR_FR = 16,
    DNAME_FR = 17,
    DLANG_CA = 18,
    DSCRIPT_CA = 19,
    DCTRY_CA = 20,
    DVAR_CA = 21,
    DNAME_CA = 22,
    DLANG_EL = 23,
    DSCRIPT_EL = 24,
    DCTRY_EL = 25,
    DVAR_EL = 26,
    DNAME_EL = 27
};

#define TESTCASE(name) addTest(root, &name, "tsutil/cloctst/" #name)

void addLocaleTest(TestNode** root);

void addLocaleTest(TestNode** root)
{
    TESTCASE(TestObsoleteNames); /* srl- move */
    TESTCASE(TestBasicGetters);
    TESTCASE(TestNullDefault);
    TESTCASE(TestPrefixes);
    TESTCASE(TestSimpleResourceInfo);
    TESTCASE(TestDisplayNames);
    TESTCASE(TestGetAvailableLocales);
    TESTCASE(TestDataDirectory);
    TESTCASE(TestISOFunctions);
    TESTCASE(TestISO3Fallback);
    TESTCASE(TestUninstalledISO3Names);
    TESTCASE(TestSimpleDisplayNames);
    TESTCASE(TestVariantParsing);
    TESTCASE(TestLocaleStructure);
    TESTCASE(TestConsistentCountryInfo);
    TESTCASE(VerifyTranslation);
    /*TESTCASE(MoreVariants);*/
    TESTCASE(TestKeywordVariants);
    TESTCASE(TestKeywordVariantParsing);
    TESTCASE(TestCanonicalization);
    TESTCASE(TestDisplayKeywords);
    TESTCASE(TestDisplayKeywordValues);
    TESTCASE(TestGetBaseName);
    TESTCASE(TestGetLocale);
    TESTCASE(TestExemplarSet);
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

    {
    /* Test that set & get of default locale work, and that
     * default locales are cached and reused, and not overwritten.
     */
        const char *n_en_US;
        const char *n_fr_FR;
        const char *n2_en_US;
        
        status = U_ZERO_ERROR;
        uloc_setDefault("en_US", &status);
        n_en_US = uloc_getDefault();
        if (strcmp(n_en_US, "en_US") != 0) {
            log_err("Wrong result from uloc_getDefault().  Expected \"en_US\", got \"%s\"\n", n_en_US);
        }
        
        uloc_setDefault("fr_FR", &status);
        n_fr_FR = uloc_getDefault();
        if (strcmp(n_en_US, "en_US") != 0) {
            log_err("uloc_setDefault altered previously default string."
                "Expected \"en_US\", got \"%s\"\n",  n_en_US);
        }
        if (strcmp(n_fr_FR, "fr_FR") != 0) {
            log_err("Wrong result from uloc_getDefault().  Expected \"fr_FR\", got %s\n",  n_fr_FR);
        }
        
        uloc_setDefault("en_US", &status);
        n2_en_US = uloc_getDefault();
        if (strcmp(n2_en_US, "en_US") != 0) {
            log_err("Wrong result from uloc_getDefault().  Expected \"en_US\", got \"%s\"\n", n_en_US);
        }
        if (n2_en_US != n_en_US) {
            log_err("Default locale cache failed to reuse en_US locale.\n");
        }
        
        if (U_FAILURE(status)) {
            log_err("Failure returned from uloc_setDefault - \"%s\"\n", u_errorName(status));
        }
        
    }
    
}
/* Test the i- and x- and @ and . functionality 
*/

#define PREFIXBUFSIZ 128

static void TestPrefixes() {
    int row = 0;
    int n;
    const char *loc, *expected;
    
    const char *testData[][7] =
    {
        /* NULL canonicalize() column means "expect same as getName()" */
        {"sv", "", "FI", "AL", "sv-fi-al", "sv_FI_AL", NULL},
        {"en", "", "GB", "", "en-gb", "en_GB", NULL},
        {"i-hakka", "", "MT", "XEMXIJA", "i-hakka_MT_XEMXIJA", "i-hakka_MT_XEMXIJA", NULL},
        {"i-hakka", "", "CN", "", "i-hakka_CN", "i-hakka_CN", NULL},
        {"i-hakka", "", "MX", "", "I-hakka_MX", "i-hakka_MX", NULL},
        {"x-klingon", "", "US", "SANJOSE", "X-KLINGON_us_SANJOSE", "x-klingon_US_SANJOSE", NULL},
        
        {"mr", "", "", "", "mr.utf8", "mr.utf8", "mr"},
        {"de", "", "TV", "", "de-tv.koi8r", "de_TV.koi8r", "de_TV"},
        {"x-piglatin", "", "ML", "", "x-piglatin_ML.MBE", "x-piglatin_ML.MBE", "x-piglatin_ML"},  /* Multibyte English */
        {"i-cherokee", "","US", "", "i-Cherokee_US.utf7", "i-cherokee_US.utf7", "i-cherokee_US"},
        {"x-filfli", "", "MT", "FILFLA", "x-filfli_MT_FILFLA.gb-18030", "x-filfli_MT_FILFLA.gb-18030", "x-filfli_MT_FILFLA"},
        {"no", "", "NO", "NY", "no-no-ny.utf32@B", "no_NO_NY.utf32@B", "no_NO_NY_B"},
        {"no", "", "NO", "",  "no-no.utf32@B", "no_NO.utf32@B", "no_NO_B"},
        {"no", "", "",   "NY", "no__ny", "no__NY", NULL},
        {"no", "", "",   "", "no@ny", "no@ny", "no__NY"},
        {"el", "Latn", "", "", "el-latn", "el_Latn", NULL},
        {"en", "Cyrl", "RU", "", "en-cyrl-ru", "en_Cyrl_RU", NULL},
        {"zh", "Hant", "TW", "STROKE", "zh-hant_TW_STROKE", "zh_Hant_TW_STROKE", NULL},
        {"qq", "Qqqq", "QQ", "QQ", "qq_Qqqq_QQ_QQ", "qq_Qqqq_QQ_QQ", NULL},
        {"qq", "Qqqq", "", "QQ", "qq_Qqqq__QQ", "qq_Qqqq__QQ", NULL},
        {"12", "3456", "78", "90", "12_3456_78_90", "12_3456_78_90", NULL}, /* total garbage */
        
        {NULL,NULL,NULL,NULL,NULL,NULL,NULL}
    };
    
    const char *testTitles[] = {
        "uloc_getLanguage()",
        "uloc_getScript()",
        "uloc_getCountry()",
        "uloc_getVariant()",
        "name",
        "uloc_getName()",
        "uloc_canonicalize()"
    };
    
    char buf[PREFIXBUFSIZ];
    int32_t len;
    UErrorCode err;
    
    
    for(row=0;testData[row][0] != NULL;row++) {
        loc = testData[row][NAME];
        log_verbose("Test #%d: %s\n", row, loc);
        
        err = U_ZERO_ERROR;
        len=0;
        buf[0]=0;
        for(n=0;n<=(NAME+2);n++) {
            if(n==NAME) continue;
            
            for(len=0;len<PREFIXBUFSIZ;len++) {
                buf[len] = '%'; /* Set a tripwire.. */
            }
            len = 0;
            
            switch(n) {
            case LANG:
                len = uloc_getLanguage(loc, buf, PREFIXBUFSIZ, &err);
                break;
                
            case SCRIPT:
                len = uloc_getScript(loc, buf, PREFIXBUFSIZ, &err);
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
                
            case NAME+2:
                len = uloc_canonicalize(loc, buf, PREFIXBUFSIZ, &err);
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
                
                expected = testData[row][n];
                if (expected == NULL && n == (NAME+2)) {
                    /* NULL expected canonicalize() means "expect same as getName()" */
                    expected = testData[row][NAME+1];
                }
                if(strcmp(buf, expected)) {
                    log_err("#%d: %s on %s: -> [%s] (expected '%s'!)\n",
                        row, testTitles[n], loc, buf, expected);
                    
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
        sprintf(temp2, "%x", (int)uloc_getLCID(testLocale));
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
    UErrorCode errorCode=U_ZERO_ERROR;
    int32_t length;
    log_verbose("Testing getDisplayName for different locales\n");

    log_verbose("  In locale = en_US...\n");
    doTestDisplayNames("en_US", DLANG_EN);
    log_verbose("  In locale = fr_FR....\n");
    doTestDisplayNames("fr_FR", DLANG_FR);
    log_verbose("  In locale = ca_ES...\n");
    doTestDisplayNames("ca_ES", DLANG_CA);
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
    UChar  *testScript  = 0;
    UChar  *testCtry = 0;
    UChar  *testVar = 0;
    UChar  *testName = 0;


    UChar*  expectedLang = 0;
    UChar*  expectedScript = 0;
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
        maxresultsize=uloc_getDisplayScript(testLocale, displayLocale, NULL, maxresultsize, &status);
        if(status==U_BUFFER_OVERFLOW_ERROR)
        {
            status=U_ZERO_ERROR;
            testScript=(UChar*)malloc(sizeof(UChar) * (maxresultsize+1));
            uloc_getDisplayScript(testLocale, displayLocale, testScript, maxresultsize + 1, &status);
        }
        else
        {
            testScript=&_NUL;
        }
        if(U_FAILURE(status)){
            log_err("Error in getDisplayScript()  %s\n", myErrorName(status));
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

        expectedScript=dataTable[compareIndex + 1][i];
        if(u_strlen(expectedScript)== 0)
            expectedScript=dataTable[DSCRIPT_EN][i];

        expectedCtry=dataTable[compareIndex + 2][i];
        if(u_strlen(expectedCtry)== 0)
            expectedCtry=dataTable[DCTRY_EN][i];

        expectedVar=dataTable[compareIndex + 3][i];
        if(u_strlen(expectedVar)== 0)
            expectedVar=dataTable[DVAR_EN][i];

        expectedName=dataTable[compareIndex + 4][i];
        if(u_strlen(expectedName) == 0)
            expectedName=dataTable[DNAME_EN][i];

        if (0 !=u_strcmp(testLang,expectedLang))  {
            log_data_err(" Display Language mismatch: got %s expected %s displayLocale=%s\n", austrdup(testLang), austrdup(expectedLang), displayLocale);
        }

        if (0 != u_strcmp(testScript,expectedScript))   {
            log_data_err(" Display Script mismatch: got %s expected %s displayLocale=%s\n", austrdup(testScript), austrdup(expectedScript), displayLocale);
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
        if(testScript!=&_NUL) {
            free(testScript);
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
    int32_t expect;
    UResourceBundle *res;
    UErrorCode status = U_ZERO_ERROR;

    /*  test getISOLanguages*/
    /*str=uloc_getISOLanguages(); */
    log_verbose("Testing ISO Languages: \n");

    res = ures_open(NULL, "root", &status);
    ures_getByKey(res, "Languages", res, &status);
    if (U_FAILURE(status)) {
        log_err("There is an error in ures_getByKey(\"Languages\"), status=%s\n", u_errorName(status));
        status = U_ZERO_ERROR;
    }

    for(count = 0; *(str+count) != 0; count++)
    {
        test = *(str+count);

#if 0
        {
            /* This code only works on ASCII machines where the keys are stored in ASCII order */
            const char *key;
            ures_getNextString(res, NULL, &key, &status);
            if(!strcmp(key,"root"))
                ures_getNextString(res, NULL, &key, &status);
            if(!strcmp(key,"Fallback"))
                ures_getNextString(res, NULL, &key, &status);
            if(!strcmp(key,"sh")) /* Remove this once sh is removed. */
                ures_getNextString(res, NULL, &key, &status);
            if(!key || strcmp(test,key)) {
                /* The first difference usually implies the place where things get out of sync */
                log_err("FAIL diff at offset %d, \"%s\" != \"%s\"\n", count, test, key);
            }
            status = U_ZERO_ERROR;
        }
#endif

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

    /* We check root, just in case the en locale is removed. The en locale should have the same number of resources. */
    expect = ures_getSize(res) - 2; /* Ignore Fallback and root */
    expect -= 1; /* TODO: Remove this line once sh goes away. */
    ures_close(res);

    if(count!=expect) {
        log_err("There is an error in getISOLanguages, got %d, expected %d\n", count, expect);
    }

    log_verbose("Testing ISO Countries");
    for(count = 0; *(str1+count) != 0; count++)
    {
        test = *(str1+count);
        if(!strcmp(test,"FX"))
            log_err("FAIL getISOCountries() has obsolete country code %s\n", test);
        if(!strcmp(test,"ZR"))
            log_err("FAIL getISOCountries() has obsolete country code %s\n", test);
    }
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

    for (i = 0; i < LOCALE_INFO_SIZE; i++) {
        dataTable[i] = (UChar**)(calloc(sizeof(UChar*),LOCALE_SIZE));
        for (j = 0; j < LOCALE_SIZE; j++){
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
    const char* languageNames [] = { "Hebrew", "Indonesian", "Inuktitut", "Uighur", "Yiddish",
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

    if (uloc_getLCID("iw_IL") != uloc_getLCID("he_IL")) {
        log_err("he,iw LCID mismatch: %X versus %X\n", uloc_getLCID("iw_IL"), uloc_getLCID("he_IL"));
    }

    if (uloc_getLCID("iw") != uloc_getLCID("he")) {
        log_err("he,iw LCID mismatch: %X versus %X\n", uloc_getLCID("iw"), uloc_getLCID("he"));
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

static UBool isCurrencyPreEuro(const char* currencyKey){
    if( strcmp(currencyKey, "PTE") == 0 ||
        strcmp(currencyKey, "ESP") == 0 ||
        strcmp(currencyKey, "LUF") == 0 ||
        strcmp(currencyKey, "GRD") == 0 ||
        strcmp(currencyKey, "BEF") == 0 ||
        strcmp(currencyKey, "ITL") == 0 ){
            return TRUE;
    }
    return FALSE;
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
        currentBundleKey = ures_getKey(currentBundle);
        subBundle = ures_getNextResource(currentBundle, NULL, &errorCode);
        if (U_FAILURE(errorCode)) {
            log_err("Can't open a resource for locale %s. Error: %s\n", locale, u_errorName(errorCode));
            continue;
        }
        subBundleKey = ures_getKey(subBundle);
        

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
                            isSpecial = (strcmp(currentBundleKey, "Currencies") == 0 
                              || strcmp(currentBundleKey, "collations") == 0);
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
                    || (subBundleKey != NULL &&  strcmp(subBundleKey, "LocaleScript") != 0 && !isCurrencyPreEuro(subBundleKey)))
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
            } else if (strcmp(subBundleKey, "localPatternChars") == 0) {
                /* Check well-formedness of localPatternChars.  First, the
                 * length must match the number of fields defined by
                 * DateFormat.  Second, each character in the string must
                 * be in the set [A-Za-z].  Finally, each character must be
                 * unique.
                 */
                int32_t i,j;
                if (len != UDAT_FIELD_COUNT) {
                    log_err("key \"%s\" has the wrong number of characters in locale \"%s\"\n",
                            subBundleKey,
                            locale);
                }
                /* Check char validity. */
                for (i=0; i<len; ++i) {
                    if (!((string[i] >= 65/*'A'*/ && string[i] <= 90/*'Z'*/) ||
                          (string[i] >= 97/*'a'*/ && string[i] <= 122/*'z'*/))) {
                        log_err("key \"%s\" has illegal character '%c' in locale \"%s\"\n",
                                subBundleKey,
                                (char) string[i],
                                locale);
                    }
                    /* Do O(n^2) check for duplicate chars. */
                    for (j=0; j<i; ++j) {
                        if (string[j] == string[i]) {
                            log_err("key \"%s\" has duplicate character '%c' in locale \"%s\"\n",
                                    subBundleKey,
                                    (char) string[i],
                                    locale);
                        }
                    }
                }
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


static void
testLCID(UResourceBundle *currentBundle,
         const char *localeName)
{
    UErrorCode status = U_ZERO_ERROR;
    uint32_t lcid;
    uint32_t expectedLCID;
    char lcidStringC[64] = {0};

    expectedLCID = uloc_getLCID(localeName);
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

        testLCID(currentLocale, currLoc);

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
    UResourceBundle *toCalendar, *fromCalendar, *toGregorian, *fromGregorian;

    if(U_FAILURE(errorCode)) {
        log_err("Can't open resource bundle %s or %s - %s\n", fromLocale, toLocale, u_errorName(errorCode));
        return;
    }
    fromCalendar = ures_getByKey(fromLocaleBund, "calendar", NULL, &errorCode);
    fromGregorian = ures_getByKeyWithFallback(fromCalendar, "gregorian", NULL, &errorCode);
    fromDateTimeElements = ures_getByKeyWithFallback(fromGregorian, "DateTimeElements", NULL, &errorCode);

    toCalendar = ures_getByKey(toLocaleBund, "calendar", NULL, &errorCode);
    toGregorian = ures_getByKeyWithFallback(toCalendar, "gregorian", NULL, &errorCode);
    toDateTimeElements = ures_getByKeyWithFallback(toGregorian, "DateTimeElements", NULL, &errorCode);

    if(U_FAILURE(errorCode)){
        log_err("Did not get DateTimeElements from the bundle %s or %s\n", fromLocale, toLocale);
        return;
    }
    ures_close(fromCalendar);
    ures_close(toCalendar);
    ures_close(fromGregorian);
    ures_close(toGregorian);
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
            && string[strIdx] != 0x0020 && string[strIdx] != 0x00A0 && string[strIdx] != 0x002e && string[strIdx] != 0x002c && string[strIdx] != 0x002d && string[strIdx] != 0x0027) {
            if (!ignoreNumbers || (ignoreNumbers && (string[strIdx] < 0x30 || string[strIdx] > 0x39))) {
                return strIdx;
            }
        }
    }
    uset_close(exemplarSet);
    return -1;
}

static void 
findSetMatch( UScriptCode *scriptCodes, int32_t scriptsLen, 
              USet *exemplarSet,
              const char  *locale){
    USet *scripts[10]= {0};
    char pattern[256] = { '[', ':', 0x000 };
    UChar uPattern[256] = {0};
    UErrorCode status = U_ZERO_ERROR;
    int32_t i;

    /* create the sets with script codes */
    for(i = 0; i<scriptsLen; i++){
        strcat(pattern, uscript_getShortName(scriptCodes[i]));
        strcat(pattern, ":]");
        u_charsToUChars(pattern, uPattern, strlen(pattern));
        scripts[i] = uset_openPattern(uPattern, strlen(pattern), &status);
        if(U_FAILURE(status)){
            log_err("Could not create set for patter %s. Error: %s\n", pattern, u_errorName(status));
            break;
        }
        pattern[2] = 0; 
    }
    if(U_SUCCESS(status)){
        UBool existsInScript = FALSE;
        /* iterate over the exemplarSet and ascertain if all
         * UChars in exemplarSet belong to the scripts returned
         * by getScript
         */
        int32_t count = uset_getItemCount(exemplarSet);

        for( i=0; i < count; i++){
            UChar32 start = 0;
            UChar32 end = 0;
            UChar *str = NULL;
            int32_t strCapacity = 0;

            strCapacity = uset_getItem(exemplarSet, i, &start, &end, str, strCapacity, &status);
            if(U_SUCCESS(status)){
                int32_t j;
                if(strCapacity == 0){
                    /* ok the item is a range */
                     for( j = 0; j < scriptsLen; j++){
                        if(uset_containsRange(scripts[j], start, end) == TRUE){
                            existsInScript = TRUE;
                        }
                    }
                    if(existsInScript == FALSE){
                        log_err("ExemplarCharacters and LocaleScript containment test failed for locale %s. \n", locale);
                    }
                }else{
                    strCapacity++; /* increment for NUL termination */
                    /* allocate the str and call the api again */
                    str = (UChar*) malloc(U_SIZEOF_UCHAR * strCapacity);
                    strCapacity =  uset_getItem(exemplarSet, i, &start, &end, str, strCapacity, &status);
                    /* iterate over the scripts and figure out if the string contained is actually
                     * in the script set
                     */
                    for( j = 0; j < scriptsLen; j++){
                        if(uset_containsString(scripts[j],str, strCapacity) == TRUE){
                            existsInScript = TRUE;
                        }
                    }
                    if(existsInScript == FALSE){
                        log_err("ExemplarCharacters and LocaleScript containment test failed for locale %s. \n", locale);
                    }
                }
            }
        }

    }

    /* close the sets */
    for(i = 0; i<scriptsLen; i++){
        uset_close(scripts[i]);
    }
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
            {
                UResourceBundle* cal = ures_getByKey(currentLocale, "calendar", NULL, &errorCode);
                UResourceBundle* greg = ures_getByKeyWithFallback(cal, "gregorian", NULL, &errorCode);
                UResourceBundle* names = ures_getByKeyWithFallback(greg,  "dayNames", NULL, &errorCode);
                UResourceBundle* format = ures_getByKeyWithFallback(names,  "format", NULL, &errorCode);
                resArray = ures_getByKeyWithFallback(format,  "wide", NULL, &errorCode);

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
                ures_close(format);
                ures_close(names);

                names = ures_getByKeyWithFallback(greg, "monthNames", NULL, &errorCode);
                format = ures_getByKeyWithFallback(names,"format", NULL, &errorCode);
                resArray = ures_getByKeyWithFallback(format, "wide", NULL, &errorCode);
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
                ures_close(format);
                ures_close(names);
                ures_close(greg);
                ures_close(cal);
            }
            errorCode = U_ZERO_ERROR;
            numScripts = uscript_getCode(currLoc, scripts, sizeof(scripts)/sizeof(scripts[0]), &errorCode);
            if (numScripts == 0) {
                log_err("uscript_getCode(%s) doesn't work.\n", currLoc);
            }else if(scripts[0] == USCRIPT_COMMON){
                log_err("uscript_getCode(%s) returned USCRIPT_COMMON.\n", currLoc); 
            }

            /* test that the scripts are a superset of exemplar characters. */
           {
                USet *exemplarSet =  ulocdata_getExemplarSet(NULL,currLoc, 0, &errorCode);
                /* test if exemplar characters are part of script code */
                findSetMatch(scripts, numScripts, exemplarSet, currLoc);
                uset_close(exemplarSet);
            }

           /* test that the paperSize API works */
           {
               int32_t height=0, width=0;
               ulocdata_getPaperSize(currLoc, &height, &width, &errorCode);
               if(U_FAILURE(errorCode)){
                   log_err("ulocdata_getPaperSize failed for locale %s with error: %s \n", currLoc, u_errorName(errorCode));
               }
               if(strstr(currLoc, "_US")!=NULL && height != 279 && width != 216 ){
                   log_err("ulocdata_getPaperSize did not return expected data for locale %s \n", currLoc);
               }
           }
            /* test that the MeasurementSystem works API works */
           {
               UMeasurementSystem measurementSystem = ulocdata_getMeasurementSystem(currLoc, &errorCode);
               if(U_FAILURE(errorCode)){
                   log_err("ulocdata_getMeasurementSystem failed for locale %s with error: %s \n", currLoc, u_errorName(errorCode));
               }
               if(strstr(currLoc, "_US")!=NULL){
                   if(measurementSystem != UMS_US){
                        log_err("ulocdata_getMeasurementSystem did not return expected data for locale %s \n", currLoc);
                   }
               }else if(measurementSystem != UMS_SI){
                   log_err("ulocdata_getMeasurementSystem did not return expected data for locale %s \n", currLoc);
               }
           }
        }
        ures_close(currentLocale);
    }

    ures_close(root);
}

static void MoreVariants(void) 
{
    struct {
        const char *localeID;
        const char *keyword;
        const char *expectedValue;
    } testCases[] = {
        { "de_DE_EURO@collation=PHONEBOOK", "collation", "PHONEBOOK" },
        { "es_ES.utf8@euro", "collation", ""},
        { "es_ES.hello.utf8@euro", "", "" },
        { " s pa c e d  _  more spaces _ spaced variant ", "", ""}
    };
    
    UErrorCode status = U_ZERO_ERROR;
    
    int32_t i = 0;
    int32_t resultLen = 0;
    char buffer[256];
    
    for(i = 0; i < sizeof(testCases)/sizeof(testCases[0]); i++) {
        *buffer = 0;
        resultLen = uloc_getName(testCases[i].localeID, buffer, 256, &status);
        if(uprv_strcmp(testCases[i].expectedValue, buffer) != 0) {
            log_err("Expected to extract \"%s\" from \"%s\" for keyword \"%s\". Got \"%s\" instead\n",
                testCases[i].expectedValue, testCases[i].localeID, testCases[i].keyword, buffer);
        }
    }
}

static void TestKeywordVariants(void) 
{
    struct {
        const char *localeID;
        const char *expectedLocaleID;
        const char *expectedLocaleIDNoKeywords;
        const char *expectedCanonicalID;
        const char *expectedKeywords[10];
        int32_t numKeywords;
        UErrorCode expectedStatus; /* from uloc_openKeywords */
    } testCases[] = {
        {
            "de_DE@  currency = euro; C o ll A t i o n   = Phonebook   ; C alen dar = buddhist   ", 
            "de_DE@calendar=buddhist;collation=Phonebook;currency=euro", 
            "de_DE",
            "de_DE@calendar=buddhist;collation=Phonebook;currency=euro", 
            {"calendar", "collation", "currency"},
            3,
            U_ZERO_ERROR
        },
        {
            "de_DE@euro",
            "de_DE@euro",
            "de_DE",
            "de_DE@currency=EUR",
            {"","","","","","",""},
            0,
            U_INVALID_FORMAT_ERROR /* must have '=' after '@' */
        },
        {
            "de_DE@euro;collation=phonebook",
            "de_DE", /* error result; bad format */
            "de_DE", /* error result; bad format */
            "de_DE", /* error result; bad format */
            {"","","","","","",""},
            0,
            U_INVALID_FORMAT_ERROR
        }
    };
    UErrorCode status = U_ZERO_ERROR;
    
    int32_t i = 0, j = 0;
    int32_t resultLen = 0;
    char buffer[256];
    UEnumeration *keywords;
    int32_t keyCount = 0;
    const char *keyword = NULL;
    int32_t keywordLen = 0;
    
    for(i = 0; i < sizeof(testCases)/sizeof(testCases[0]); i++) {
        status = U_ZERO_ERROR;
        *buffer = 0;
        keywords = uloc_openKeywords(testCases[i].localeID, &status);
        
        if(status != testCases[i].expectedStatus) {
            log_err("Expected to uloc_openKeywords(\"%s\") => status %s. Got %s instead\n", 
                    testCases[i].localeID,
                    u_errorName(testCases[i].expectedStatus), u_errorName(status));
        }
        status = U_ZERO_ERROR;
        if(keywords) {
            if((keyCount = uenum_count(keywords, &status)) != testCases[i].numKeywords) {
                log_err("Expected to get %i keywords, got %i\n", testCases[i].numKeywords, keyCount);
            }
            if(keyCount) {
                j = 0;
                while((keyword = uenum_next(keywords, &keywordLen, &status))) {
                    if(strcmp(keyword, testCases[i].expectedKeywords[j]) != 0) {
                        log_err("Expected to get keyword value %s, got %s\n", testCases[i].expectedKeywords[j], keyword);
                    }
                    j++;
                }
            }
            uenum_close(keywords);
        }
        resultLen = uloc_getName(testCases[i].localeID, buffer, 256, &status);
        if (uprv_strcmp(testCases[i].expectedLocaleID, buffer) != 0) {
            log_err("Expected uloc_getName(\"%s\") => \"%s\"; got \"%s\"\n",
                    testCases[i].localeID, testCases[i].expectedLocaleID, buffer);
        }
        resultLen = uloc_canonicalize(testCases[i].localeID, buffer, 256, &status);
        if (uprv_strcmp(testCases[i].expectedCanonicalID, buffer) != 0) {
            log_err("Expected uloc_canonicalize(\"%s\") => \"%s\"; got \"%s\"\n",
                    testCases[i].localeID, testCases[i].expectedCanonicalID, buffer);
        }        
    }
    
}

static void TestKeywordVariantParsing(void) 
{
    struct {
        const char *localeID;
        const char *keyword;
        const char *expectedValue;
    } testCases[] = {
        { "de_DE@  C o ll A t i o n   = Phonebook   ", "c o ll a t i o n", "Phonebook" },
        { "de_DE", "collation", ""},
        { "de_DE@collation=PHONEBOOK", "collation", "PHONEBOOK" },
        { "de_DE@currency = euro; CoLLaTion   = PHONEBOOk", "collatiON", "PHONEBOOk" },
    };
    
    UErrorCode status = U_ZERO_ERROR;
    
    int32_t i = 0;
    int32_t resultLen = 0;
    char buffer[256];
    
    for(i = 0; i < sizeof(testCases)/sizeof(testCases[0]); i++) {
        *buffer = 0;
        resultLen = uloc_getKeywordValue(testCases[i].localeID, testCases[i].keyword, buffer, 256, &status);
        if(uprv_strcmp(testCases[i].expectedValue, buffer) != 0) {
            log_err("Expected to extract \"%s\" from \"%s\" for keyword \"%s\". Got \"%s\" instead\n",
                testCases[i].expectedValue, testCases[i].localeID, testCases[i].keyword, buffer);
        }
    }
}

static int32_t _canonicalize(int32_t selector, /* 0==getName, 1==canonicalize */
                             const char* localeID,
                             char* result,
                             int32_t resultCapacity,
                             UErrorCode* ec) {
    /* YOU can change this to use function pointers if you like */
    switch (selector) {
    case 0:
        return uloc_getName(localeID, result, resultCapacity, ec);
    case 1:
        return uloc_canonicalize(localeID, result, resultCapacity, ec);
    default:
        return -1;
    }
}

static void TestCanonicalization(void)
{
    static struct {
        const char *localeID;    /* input */
        const char *getNameID;   /* expected getName() result */
        const char *canonicalID; /* expected canonicalize() result */
    } testCases[] = {
        { "ca_ES_PREEURO-with-extra-stuff-that really doesn't make any sense-unless-you're trying to increase code coverage",
          "ca_ES_PREEURO_WITH_EXTRA_STUFF_THAT REALLY DOESN'T MAKE ANY SENSE_UNLESS_YOU'RE TRYING TO INCREASE CODE COVERAGE",
          "ca_ES_PREEURO_WITH_EXTRA_STUFF_THAT REALLY DOESN'T MAKE ANY SENSE_UNLESS_YOU'RE TRYING TO INCREASE CODE COVERAGE"},
        { "ca_ES_PREEURO", "ca_ES_PREEURO", "ca_ES@currency=ESP" },
        { "de_AT_PREEURO", "de_AT_PREEURO", "de_AT@currency=ATS" },
        { "de_DE_PREEURO", "de_DE_PREEURO", "de_DE@currency=DEM" },
        { "de_LU_PREEURO", "de_LU_PREEURO", "de_LU@currency=LUF" },
        { "el_GR_PREEURO", "el_GR_PREEURO", "el_GR@currency=GRD" },
        { "en_BE_PREEURO", "en_BE_PREEURO", "en_BE@currency=BEF" },
        { "en_IE_PREEURO", "en_IE_PREEURO", "en_IE@currency=IEP" },
        { "es_ES_PREEURO", "es_ES_PREEURO", "es_ES@currency=ESP" },
        { "eu_ES_PREEURO", "eu_ES_PREEURO", "eu_ES@currency=ESP" },
        { "fi_FI_PREEURO", "fi_FI_PREEURO", "fi_FI@currency=FIM" },
        { "fr_BE_PREEURO", "fr_BE_PREEURO", "fr_BE@currency=BEF" },
        { "fr_FR_PREEURO", "fr_FR_PREEURO", "fr_FR@currency=FRF" },
        { "fr_LU_PREEURO", "fr_LU_PREEURO", "fr_LU@currency=LUF" },
        { "ga_IE_PREEURO", "ga_IE_PREEURO", "ga_IE@currency=IEP" },
        { "gl_ES_PREEURO", "gl_ES_PREEURO", "gl_ES@currency=ESP" },
        { "it_IT_PREEURO", "it_IT_PREEURO", "it_IT@currency=ITL" },
        { "nl_BE_PREEURO", "nl_BE_PREEURO", "nl_BE@currency=BEF" },
        { "nl_NL_PREEURO", "nl_NL_PREEURO", "nl_NL@currency=NLG" },
        { "pt_PT_PREEURO", "pt_PT_PREEURO", "pt_PT@currency=PTE" },
        { "de__PHONEBOOK", "de__PHONEBOOK", "de@collation=phonebook" },
        { "en_GB_EURO", "en_GB_EURO", "en_GB@currency=EUR" },
        { "en_GB@EURO", "en_GB@EURO", "en_GB@currency=EUR" }, /* POSIX ID */
        { "es__TRADITIONAL", "es__TRADITIONAL", "es@collation=traditional" },
        { "hi__DIRECT", "hi__DIRECT", "hi@collation=direct" },
        { "ja_JP_TRADITIONAL", "ja_JP_TRADITIONAL", "ja_JP@calendar=japanese" },
        { "th_TH_TRADITIONAL", "th_TH_TRADITIONAL", "th_TH@calendar=buddhist" },
        { "zh_TW_STROKE", "zh_TW_STROKE", "zh_TW@collation=stroke" },
        { "zh__PINYIN", "zh__PINYIN", "zh@collation=pinyin" },
        { "zh@collation=pinyin", "zh@collation=pinyin", "zh@collation=pinyin" },
        { "zh_CN@collation=pinyin", "zh_CN@collation=pinyin", "zh_CN@collation=pinyin" },
        { "zh_CN_CA@collation=pinyin", "zh_CN_CA@collation=pinyin", "zh_CN_CA@collation=pinyin" },
        { "en_US_POSIX", "en_US_POSIX", "en_US_POSIX" }, 
        { "hy_AM_REVISED", "hy_AM_REVISED", "hy_AM_REVISED" }, 
        { "no_NO_NY", "no_NO_NY", "no_NO_NY" /* not: "nn_NO" [alan ICU3.0] */ },
        { "no@ny", "no@ny", "no__NY" /* not: "nn" [alan ICU3.0] */ }, /* POSIX ID */
        { "no-no.utf32@B", "no_NO.utf32@B", "no_NO_B" /* not: "nb_NO_B" [alan ICU3.0] */ }, /* POSIX ID */
        { "qz-qz@Euro", "qz_QZ@Euro", "qz_QZ@currency=EUR" }, /* qz-qz uses private use iso codes */
        { "en-BOONT", "en_BOONT", "en__BOONT" }, /* registered name */
        { "de-1901", "de_1901", "de__1901" }, /* registered name */
        { "de-1906", "de_1906", "de__1906" }, /* registered name */
        { "sr-SP-Cyrl", "sr_SP_CYRL", "sr_Cyrl_SP" }, /* .NET name */
        { "sr-SP-Latn", "sr_SP_LATN", "sr_Latn_SP" }, /* .NET name */
        { "uz-UZ-Cyrl", "uz_UZ_CYRL", "uz_Cyrl_UZ" }, /* .NET name */
        { "uz-UZ-Latn", "uz_UZ_LATN", "uz_Latn_UZ" }, /* .NET name */
        { "zh-CHS", "zh_CHS", "zh_Hans" }, /* .NET name */
        { "zh-CHT", "zh_CHT", "zh_TW" }, /* .NET name This may change back to zh_Hant */

        /* posix behavior that used to be performed by getName */
        { "mr.utf8", "mr.utf8", "mr" },
        { "de-tv.koi8r", "de_TV.koi8r", "de_TV" },
        { "x-piglatin_ML.MBE", "x-piglatin_ML.MBE", "x-piglatin_ML" },
        { "i-cherokee_US.utf7", "i-cherokee_US.utf7", "i-cherokee_US" },
        { "x-filfli_MT_FILFLA.gb-18030", "x-filfli_MT_FILFLA.gb-18030", "x-filfli_MT_FILFLA" },
        { "no-no-ny.utf8@B", "no_NO_NY.utf8@B", "no_NO_NY_B" /* not: "nn_NO" [alan ICU3.0] */ }, /* @ ignored unless variant is empty */

        /* fleshing out canonicalization */
        /* trim space and sort keywords, ';' is separator so not present at end in canonical form */
        { "en_Hant_IL_VALLEY_GIRL@ currency = EUR; calendar = Japanese ;", "en_Hant_IL_VALLEY_GIRL@calendar=Japanese;currency=EUR", "en_Hant_IL_VALLEY_GIRL@calendar=Japanese;currency=EUR" },
        /* already-canonical ids are not changed */
        { "en_Hant_IL_VALLEY_GIRL@calendar=Japanese;currency=EUR", "en_Hant_IL_VALLEY_GIRL@calendar=Japanese;currency=EUR", "en_Hant_IL_VALLEY_GIRL@calendar=Japanese;currency=EUR" },
        /* PRE_EURO and EURO conversions don't affect other keywords */
        { "es_ES_PREEURO@CALendar=Japanese", "es_ES_PREEURO@calendar=Japanese", "es_ES@calendar=Japanese;currency=ESP" },
        { "es_ES_EURO@SHOUT=zipeedeedoodah", "es_ES_EURO@shout=zipeedeedoodah", "es_ES@currency=EUR;shout=zipeedeedoodah" },
        /* currency keyword overrides PRE_EURO and EURO currency */
        { "es_ES_PREEURO@currency=EUR", "es_ES_PREEURO@currency=EUR", "es_ES@currency=EUR" },
        { "es_ES_EURO@currency=ESP", "es_ES_EURO@currency=ESP", "es_ES@currency=ESP" },
        /* norwegian is just too weird, if we handle things in their full generality */
        { "no-Hant-GB_NY@currency=$$$", "no_Hant_GB_NY@currency=$$$", "no_Hant_GB_NY@currency=$$$" /* not: "nn_Hant_GB@currency=$$$" [alan ICU3.0] */ },

        /* test cases reflecting internal resource bundle usage */
        { "root@kw=foo", "root@kw=foo", "root@kw=foo" },
        { "@calendar=gregorian", "@calendar=gregorian", "@calendar=gregorian" },
        { "ja_JP@calendar=Japanese", "ja_JP@calendar=Japanese", "ja_JP@calendar=Japanese" }
    };
    
    static const char* label[] = { "getName", "canonicalize" };

    UErrorCode status = U_ZERO_ERROR;
    int32_t i, j, resultLen = 0, origResultLen;
    char buffer[256];
    
    for (i=0; i < sizeof(testCases)/sizeof(testCases[0]); i++) {
        for (j=0; j<2; ++j) {
            const char* expected = (j==0) ? testCases[i].getNameID : testCases[i].canonicalID;
            *buffer = 0;
            status = U_ZERO_ERROR;
            /* log_verbose("testing %s -> %s\n", testCases[i], testCases[i].canonicalID); */
            origResultLen = _canonicalize(j, testCases[i].localeID, NULL, 0, &status);
            if (status != U_BUFFER_OVERFLOW_ERROR) {
                log_err("FAIL: uloc_%s(%s) => %s, expected U_BUFFER_OVERFLOW_ERROR\n",
                        label[j], testCases[i].localeID, u_errorName(status));
                continue;
            }
            status = U_ZERO_ERROR;
            resultLen = _canonicalize(j, testCases[i].localeID, buffer, sizeof(buffer), &status);
            if (U_FAILURE(status)) {
                log_err("FAIL: uloc_%s(%s) => %s, expected U_ZERO_ERROR\n",
                        label[j], testCases[i].localeID, u_errorName(status));
                continue;
            }
            if(uprv_strcmp(expected, buffer) != 0) {
                log_err("FAIL: uloc_%s(%s) => \"%s\", expected \"%s\"\n",
                        label[j], testCases[i].localeID, buffer, expected);
            } else {
                log_verbose("Ok: uloc_%s(%s) => \"%s\"\n",
                            label[j], testCases[i].localeID, buffer);
            }
            if (resultLen != (int32_t)strlen(buffer)) {
                log_err("FAIL: uloc_%s(%s) => len %d, expected len %d\n",
                        label[j], testCases[i].localeID, resultLen, strlen(buffer));
            }
            if (origResultLen != resultLen) {
                log_err("FAIL: uloc_%s(%s) => preflight len %d != actual len %d\n",
                        label[j], testCases[i].localeID, origResultLen, resultLen);
            }
        }
    }
}

static void TestDisplayKeywords(void)
{
    int32_t i;

    static const struct {
        const char *localeID;
        const char *displayLocale;
        UChar displayKeyword[200];
    } testCases[] = {
        {   "ca_ES@currency=ESP",         "de_AT", 
            {0x0057, 0x00e4, 0x0068, 0x0072, 0x0075, 0x006e, 0x0067, 0x0000}, 
        },
        {   "ja_JP@calendar=japanese",         "de", 
            { 0x004b, 0x0061, 0x006c, 0x0065, 0x006e, 0x0064, 0x0065, 0x0072, 0x0000}
        },
        {   "de_DE@collation=traditional",       "de_DE", 
            {0x0053, 0x006f, 0x0072, 0x0074, 0x0069, 0x0065, 0x0072, 0x0075, 0x006e, 0x0067, 0x0000}
        },
    };
    for(i = 0; i < sizeof(testCases)/sizeof(testCases[0]); i++) {
        UErrorCode status = U_ZERO_ERROR;
        const char* keyword =NULL;
        int32_t keywordLen = 0;
        int32_t keywordCount = 0;
        UChar *displayKeyword=NULL;
        int32_t displayKeywordLen = 0;
        UEnumeration* keywordEnum = uloc_openKeywords(testCases[i].localeID, &status);
        for(keywordCount = uenum_count(keywordEnum, &status); keywordCount > 0 ; keywordCount--){
              if(U_FAILURE(status)){
                  log_err("uloc_getKeywords failed for locale id: %s with error : %s \n", testCases[i].localeID, u_errorName(status)); 
                  break;
              }
              /* the uenum_next returns NUL terminated string */
              keyword = uenum_next(keywordEnum, &keywordLen, &status);
              /* fetch the displayKeyword */
              displayKeywordLen = uloc_getDisplayKeyword(keyword, testCases[i].displayLocale, displayKeyword, displayKeywordLen, &status);
              if(status==U_BUFFER_OVERFLOW_ERROR){
                  status = U_ZERO_ERROR;
                  displayKeywordLen++; /* for null termination */
                  displayKeyword = (UChar*) malloc(displayKeywordLen * U_SIZEOF_UCHAR);
                  displayKeywordLen = uloc_getDisplayKeyword(keyword, testCases[i].displayLocale, displayKeyword, displayKeywordLen, &status);
                  if(U_FAILURE(status)){
                      log_err("uloc_getDisplayKeyword filed for keyword : %s in locale id: %s for display locale: %s \n", testCases[i].localeID, keyword, testCases[i].displayLocale, u_errorName(status)); 
                      break; 
                  }
                  if(u_strncmp(displayKeyword, testCases[i].displayKeyword, displayKeywordLen)!=0){
                      log_err("uloc_getDisplayKeyword did not get the expected value for keyword : %s in locale id: %s for display locale: %s \n", testCases[i].localeID, keyword, testCases[i].displayLocale); 
                      break; 
                  }
              }else{
                  log_err("uloc_getDisplayKeyword did not return the expected error. Error: %s\n", u_errorName(status));
              }
              
              free(displayKeyword);

        }
        uenum_close(keywordEnum);
    }
}

static void TestDisplayKeywordValues(void){
        int32_t i;

    struct {
        const char *localeID;
        const char *displayLocale;
        UChar displayKeywordValue[500];
    } testCases[] = {
        {   "ca_ES@currency=ESP",         "de_AT", 
            {0x0053, 0x0070, 0x0061, 0x006e, 0x0069, 0x0073, 0x0063, 0x0068, 0x0065, 0x0020, 0x0050, 0x0065, 0x0073, 0x0065, 0x0074, 0x0065, 0x0000}
        },
        {   "de_AT@currency=ATS",         "fr_FR", 
            {0x0073, 0x0063, 0x0068, 0x0069, 0x006c, 0x006c, 0x0069, 0x006e, 0x0067, 0x0020, 0x0061, 0x0075, 0x0074, 0x0072, 0x0069, 0x0063, 0x0068, 0x0069, 0x0065, 0x006e, 0x0000}
        },
        { "de_DE@currency=DEM",         "it", 
            {0x004d, 0x0061, 0x0072, 0x0063, 0x006f, 0x0020, 0x0054, 0x0065, 0x0064, 0x0065, 0x0073, 0x0063, 0x006f, 0x0000}
        },
        {   "el_GR@currency=GRD",         "en",    
            {0x0047, 0x0072, 0x0065, 0x0065, 0x006b, 0x0020, 0x0044, 0x0072, 0x0061, 0x0063, 0x0068, 0x006d, 0x0061, 0x0000}
        },
        {   "eu_ES@currency=ESP",         "it_IT", 
            {0x0050, 0x0065, 0x0073, 0x0065, 0x0074, 0x0061, 0x0020, 0x0053, 0x0070, 0x0061, 0x0067, 0x006e, 0x006f, 0x006c, 0x0061, 0x0000}
        },
        {   "de@collation=phonebook",     "es",    
            {0x006F, 0x0072, 0x0064, 0x0065, 0x006E, 0x0020, 0x0064, 0x0065, 0x0020, 0x006C, 0x0069, 0x0073, 0x0074, 0x00ED, 0x006E, 0x0020, 0x0074, 0x0065, 0x006C, 0x0065, 0x0066, 0x00F3, 0x006E, 0x0069, 0x0063, 0x006F, 0x0000}
        },

        { "de_DE@collation=phonebook",  "es", 
          {0x006F, 0x0072, 0x0064, 0x0065, 0x006E, 0x0020, 0x0064, 0x0065, 0x0020, 0x006C, 0x0069, 0x0073, 0x0074, 0x00ED, 0x006E, 0x0020, 0x0074, 0x0065, 0x006C, 0x0065, 0x0066, 0x00F3, 0x006E, 0x0069, 0x0063, 0x006F, 0x0000}
        },
        { "es_ES@collation=traditional","de", 
          {0x0054, 0x0072, 0x0061, 0x0064, 0x0069, 0x0074, 0x0069, 0x006f, 0x006e, 0x0065, 0x006c, 0x006c, 0x0065, 0x0020, 0x0053, 0x006f, 0x0072, 0x0074, 0x0069, 0x0065, 0x0072, 0x0072, 0x0065, 0x0067, 0x0065, 0x006c, 0x006e, 0x0000}
        },
        { "ja_JP@calendar=japanese",    "de", 
           {0x004a, 0x0061, 0x0070, 0x0061, 0x006e, 0x0069, 0x0073, 0x0063, 0x0068, 0x0065, 0x0072, 0x0020, 0x004b, 0x0061, 0x006c, 0x0065, 0x006e, 0x0064, 0x0065, 0x0072, 0x0000}
        }, 
    };
    for(i = 0; i < sizeof(testCases)/sizeof(testCases[0]); i++) {
        UErrorCode status = U_ZERO_ERROR;
        const char* keyword =NULL;
        int32_t keywordLen = 0;
        int32_t keywordCount = 0;
        UChar *displayKeywordValue = NULL;
        int32_t displayKeywordValueLen = 0;
        UEnumeration* keywordEnum = uloc_openKeywords(testCases[i].localeID, &status);
        for(keywordCount = uenum_count(keywordEnum, &status); keywordCount > 0 ; keywordCount--){
              if(U_FAILURE(status)){
                  log_err("uloc_getKeywords failed for locale id: %s in display locale: % with error : %s \n", testCases[i].localeID, testCases[i].displayLocale, u_errorName(status)); 
                  break;
              }
              /* the uenum_next returns NUL terminated string */
              keyword = uenum_next(keywordEnum, &keywordLen, &status);
              
              /* fetch the displayKeywordValue */
              displayKeywordValueLen = uloc_getDisplayKeywordValue(testCases[i].localeID, keyword, testCases[i].displayLocale, displayKeywordValue, displayKeywordValueLen, &status);
              if(status==U_BUFFER_OVERFLOW_ERROR){
                  status = U_ZERO_ERROR;
                  displayKeywordValueLen++; /* for null termination */
                  displayKeywordValue = (UChar*)malloc(displayKeywordValueLen * U_SIZEOF_UCHAR);
                  displayKeywordValueLen = uloc_getDisplayKeywordValue(testCases[i].localeID, keyword, testCases[i].displayLocale, displayKeywordValue, displayKeywordValueLen, &status);
                  if(U_FAILURE(status)){
                      log_err("uloc_getDisplayKeywordValue failed for keyword : %s in locale id: %s for display locale: %s with error : %s \n", testCases[i].localeID, keyword, testCases[i].displayLocale, u_errorName(status)); 
                      break; 
                  }
                  if(u_strncmp(displayKeywordValue, testCases[i].displayKeywordValue, displayKeywordValueLen)!=0){
                      log_err("uloc_getDisplayKeywordValue did not return the expected value keyword : %s in locale id: %s for display locale: %s with error : %s \n", testCases[i].localeID, keyword, testCases[i].displayLocale, u_errorName(status)); 
                      break;   
                  }
              }else{
                  log_err("uloc_getDisplayKeywordValue did not return the expected error. Error: %s\n", u_errorName(status));
              }
              free(displayKeywordValue);
        }
        uenum_close(keywordEnum);
    }
    {   
        /* test a multiple keywords */
        UErrorCode status = U_ZERO_ERROR;
        const char* keyword =NULL;
        int32_t keywordLen = 0;
        int32_t keywordCount = 0;
        const char* localeID = "es@collation=phonebook;calendar=buddhist;currency=DEM";
        const char* displayLocale = "de";
        const UChar expected[][50] = {
            {0x0042, 0x0075, 0x0064, 0x0064, 0x0068, 0x0069, 0x0073, 0x0074, 0x0069, 0x0073, 0x0063, 0x0068, 0x0065, 0x0072, 0x0020, 0x004b, 0x0061, 0x006c, 0x0065, 0x006e, 0x0064, 0x0065, 0x0072, 0x0000},

            {0x0054, 0x0065, 0x006c, 0x0065, 0x0066, 0x006f, 0x006e, 0x0062, 0x0075, 0x0063, 0x0068, 0x002d, 0x0053, 0x006f, 0x0072, 0x0074, 0x0069, 0x0065, 0x0072, 0x0072, 0x0065, 0x0067, 0x0065, 0x006c, 0x006e, 0x0000},
            {0x0044, 0x0065, 0x0075, 0x0074, 0x0073, 0x0063, 0x0068, 0x0065, 0x0020, 0x004d, 0x0061, 0x0072, 0x006b, 0x0000},
        };

        UEnumeration* keywordEnum = uloc_openKeywords(localeID, &status);

        for(keywordCount = 0; keywordCount < uenum_count(keywordEnum, &status) ; keywordCount++){
              UChar *displayKeywordValue = NULL;
              int32_t displayKeywordValueLen = 0;
              if(U_FAILURE(status)){
                  log_err("uloc_getKeywords failed for locale id: %s in display locale: % with error : %s \n", localeID, displayLocale, u_errorName(status)); 
                  break;
              }
              /* the uenum_next returns NUL terminated string */
              keyword = uenum_next(keywordEnum, &keywordLen, &status);
              
              /* fetch the displayKeywordValue */
              displayKeywordValueLen = uloc_getDisplayKeywordValue(localeID, keyword, displayLocale, displayKeywordValue, displayKeywordValueLen, &status);
              if(status==U_BUFFER_OVERFLOW_ERROR){
                  status = U_ZERO_ERROR;
                  displayKeywordValueLen++; /* for null termination */
                  displayKeywordValue = (UChar*)malloc(displayKeywordValueLen * U_SIZEOF_UCHAR);
                  displayKeywordValueLen = uloc_getDisplayKeywordValue(localeID, keyword, displayLocale, displayKeywordValue, displayKeywordValueLen, &status);
                  if(U_FAILURE(status)){
                      log_err("uloc_getDisplayKeywordValue failed for keyword : %s in locale id: %s for display locale: %s with error : %s \n", localeID, keyword, displayLocale, u_errorName(status)); 
                      break; 
                  }
                  if(u_strncmp(displayKeywordValue, expected[keywordCount], displayKeywordValueLen)!=0){
                      log_err("uloc_getDisplayKeywordValue did not return the expected value keyword : %s in locale id: %s for display locale: %s \n", localeID, keyword, displayLocale); 
                      break;   
                  }
              }else{
                  log_err("uloc_getDisplayKeywordValue did not return the expected error. Error: %s\n", u_errorName(status));
              }
              free(displayKeywordValue);
        }
        uenum_close(keywordEnum);
    
    }
    {
        /* Test non existent keywords */
        UErrorCode status = U_ZERO_ERROR;
        const char* localeID = "es";
        const char* displayLocale = "de";
        UChar *displayKeywordValue = NULL;
        int32_t displayKeywordValueLen = 0;
        
        /* fetch the displayKeywordValue */
        displayKeywordValueLen = uloc_getDisplayKeywordValue(localeID, "calendar", displayLocale, displayKeywordValue, displayKeywordValueLen, &status);
        if(U_FAILURE(status)) {
          log_err("uloc_getDisplaykeywordValue returned error status %s\n", u_errorName(status));
        } else if(displayKeywordValueLen != 0) {
          log_err("uloc_getDisplaykeywordValue returned %d should be 0 \n", displayKeywordValueLen);
        }
    }
}
													

static void TestGetBaseName(void) {
    struct {
        const char *localeID;
        const char *baseName;
    } testCases[] = {
        { "de_DE@  C o ll A t i o n   = Phonebook   ", "de_DE" },
        { "de@currency = euro; CoLLaTion   = PHONEBOOk", "de" },
        { "ja@calendar = buddhist", "ja" }
    };

    int32_t i = 0, baseNameLen = 0;
    char baseName[256];
    UErrorCode status = U_ZERO_ERROR;

    for(i = 0; i < sizeof(testCases)/sizeof(testCases[0]); i++) {
        baseNameLen = uloc_getBaseName(testCases[i].localeID, baseName, 256, &status);
        if(strcmp(testCases[i].baseName, baseName)) {
            log_err("For locale \"%s\" expected baseName \"%s\", but got \"%s\"\n",
                testCases[i].localeID, testCases[i].baseName, baseName);
            return;
        }
    }

}


/**
 * Compare the ICU version against the given major/minor version.
 */
static int32_t _cmpversion(const char* version) {
    UVersionInfo x, icu;
    u_versionFromString(x, version);
    u_getVersion(icu);
    return uprv_memcmp(icu, x, U_MAX_VERSION_LENGTH);
}

/**
 * Compare two locale IDs.  If they are equal, return 0.  If `string'
 * starts with `prefix' plus an additional element, that is, string ==
 * prefix + '_' + x, then return 1.  Otherwise return a value < 0.
 */
static UBool _loccmp(const char* string, const char* prefix) {
    int32_t slen = uprv_strlen(string),
            plen = uprv_strlen(prefix);
    int32_t c = uprv_strncmp(string, prefix, plen);
    /* 'root' is less than everything */
    if (uprv_strcmp(prefix, "root") == 0) {
        return (uprv_strcmp(string, "root") == 0) ? 0 : 1;
    }
    if (c) return -1; /* mismatch */
    if (slen == plen) return 0;
    if (string[plen] == '_') return 1;
    return -2; /* false match, e.g. "en_USX" cmp "en_US" */
}

static void _checklocs(const char* label,
                       const char* req,
                       const char* valid,
                       const char* actual) {
    /* We want the valid to be strictly > the bogus requested locale,
       and the valid to be >= the actual. */
    if (_loccmp(req, valid) > 0 &&
        _loccmp(valid, actual) >= 0) {
        log_verbose("%s; req=%s, valid=%s, actual=%s\n",
                    label, req, valid, actual);
    } else {
        log_err("FAIL: %s; req=%s, valid=%s, actual=%s\n",
                label, req, valid, actual);
    }
}

static void TestGetLocale(void) {
    UErrorCode ec = U_ZERO_ERROR;
    UParseError pe;
    UChar EMPTY[1] = {0};

    /* === udat === */
#if !UCONFIG_NO_FORMATTING
    {
        UDateFormat *obj;
        const char *req = "en_US_REDWOODSHORES", *valid, *actual;
        obj = udat_open(UDAT_DEFAULT, UDAT_DEFAULT,
                        req,
                        NULL, 0,
                        NULL, 0, &ec);
        if (U_FAILURE(ec)) {
            log_err("udat_open failed\n");
            return;
        }
        valid = udat_getLocaleByType(obj, ULOC_VALID_LOCALE, &ec);
        actual = udat_getLocaleByType(obj, ULOC_ACTUAL_LOCALE, &ec);
        if (U_FAILURE(ec)) {
            log_err("udat_getLocaleByType() failed\n");
            return;
        }
        _checklocs("udat", req, valid, actual);
        udat_close(obj);
    }
#endif

    /* === ucal === */
#if !UCONFIG_NO_FORMATTING
    {
        UCalendar *obj;
        const char *req = "fr_FR_PROVENCAL", *valid, *actual;
        obj = ucal_open(NULL, 0,
                        req,
                        UCAL_GREGORIAN,
                        &ec);
        if (U_FAILURE(ec)) {
            log_err("ucal_open failed\n");
            return;
        }
        valid = ucal_getLocaleByType(obj, ULOC_VALID_LOCALE, &ec);
        actual = ucal_getLocaleByType(obj, ULOC_ACTUAL_LOCALE, &ec);
        if (U_FAILURE(ec)) {
            log_err("ucal_getLocaleByType() failed\n");
            return;
        }
        _checklocs("ucal", req, valid, actual);
        ucal_close(obj);
    }
#endif

    /* === unum === */
#if !UCONFIG_NO_FORMATTING
    {
        UNumberFormat *obj;
        const char *req = "zh_TW_TAINAN", *valid, *actual;
        obj = unum_open(UNUM_DECIMAL,
                        NULL, 0,
                        req,
                        &pe, &ec);
        if (U_FAILURE(ec)) {
            log_err("unum_open failed\n");
            return;
        }
        valid = unum_getLocaleByType(obj, ULOC_VALID_LOCALE, &ec);
        actual = unum_getLocaleByType(obj, ULOC_ACTUAL_LOCALE, &ec);
        if (U_FAILURE(ec)) {
            log_err("unum_getLocaleByType() failed\n");
            return;
        }
        _checklocs("unum", req, valid, actual);
        unum_close(obj);
    }
#endif

    /* === umsg === */
#if !UCONFIG_NO_FORMATTING
    {
        UMessageFormat *obj;
        const char *req = "ja_JP_TAKAYAMA", *valid, *actual;
        UBool test;
        obj = umsg_open(EMPTY, 0,
                        req,
                        &pe, &ec);
        if (U_FAILURE(ec)) {
            log_err("umsg_open failed\n");
            return;
        }
        valid = umsg_getLocaleByType(obj, ULOC_VALID_LOCALE, &ec);
        actual = umsg_getLocaleByType(obj, ULOC_ACTUAL_LOCALE, &ec);
        if (U_FAILURE(ec)) {
            log_err("umsg_getLocaleByType() failed\n");
            return;
        }
        /* We want the valid to be strictly > the bogus requested locale,
           and the valid to be >= the actual. */
        /* TODO MessageFormat is currently just storing the locale it is given.
           As a result, it will return whatever it was given, even if the
           locale is invalid. */
        test = (_cmpversion("3.0") <= 0) ?
            /* Here is the weakened test for 3.0: */
            (_loccmp(req, valid) >= 0) :
            /* Here is what the test line SHOULD be: */
            (_loccmp(req, valid) > 0);

        if (test &&
            _loccmp(valid, actual) >= 0) {
            log_verbose("umsg; req=%s, valid=%s, actual=%s\n", req, valid, actual);
        } else {
            log_err("FAIL: umsg; req=%s, valid=%s, actual=%s\n", req, valid, actual);
        }
        umsg_close(obj);
    }
#endif

    /* === ubrk === */
#if !UCONFIG_NO_BREAK_ITERATION
    {
        UBreakIterator *obj;
        const char *req = "ar_KW_ABDALI", *valid, *actual;
        obj = ubrk_open(UBRK_WORD,
                        req,
                        EMPTY,
                        0,
                        &ec);
        if (U_FAILURE(ec)) {
            log_err("ubrk_open failed\n");
            return;
        }
        valid = ubrk_getLocaleByType(obj, ULOC_VALID_LOCALE, &ec);
        actual = ubrk_getLocaleByType(obj, ULOC_ACTUAL_LOCALE, &ec);
        if (U_FAILURE(ec)) {
            log_err("ubrk_getLocaleByType() failed\n");
            return;
        }
        _checklocs("ubrk", req, valid, actual);
        ubrk_close(obj);
    }
#endif

    /* === ucol === */
#if !UCONFIG_NO_COLLATION
    {
        UCollator *obj;
        const char *req = "es_AR_BUENOSAIRES", *valid, *actual;
        obj = ucol_open(req, &ec);
        if (U_FAILURE(ec)) {
            log_err("ucol_open failed\n");
            return;
        }
        valid = ucol_getLocaleByType(obj, ULOC_VALID_LOCALE, &ec);
        actual = ucol_getLocaleByType(obj, ULOC_ACTUAL_LOCALE, &ec);
        if (U_FAILURE(ec)) {
            log_err("ucol_getLocaleByType() failed\n");
            return;
        }
        _checklocs("ucol", req, valid, actual);
        ucol_close(obj);
    }
#endif
}

/* adjust this limit as appropriate */
#define MAX_SCRIPTS_PER_LOCALE 8

static void TestExemplarSet(void){
    int32_t i, j, k, m, n;
    int32_t equalCount = 0;
    UErrorCode ec = U_ZERO_ERROR;
    UEnumeration* avail;
    USet* exemplarSets[2];
    UScriptCode code[MAX_SCRIPTS_PER_LOCALE];
    USet* codeSets[MAX_SCRIPTS_PER_LOCALE];
    int32_t codeLen;
    char cbuf[32]; /* 9 should be enough */
    UChar ubuf[64]; /* adjust as needed */
    UBool existsInScript;
    int32_t itemCount;
    int32_t strLen;
    UChar32 start, end;
    
    exemplarSets[0] = exemplarSets[1] = NULL;
    for (i=0; i<MAX_SCRIPTS_PER_LOCALE; ++i) {
        codeSets[i] = NULL;
    }

    avail = ures_openAvailableLocales(NULL, &ec);
    if (!assertSuccess("ures_openAvailableLocales", &ec)) goto END;
    n = uenum_count(avail, &ec);
    if (!assertSuccess("uenum_count", &ec)) goto END;

    for(i=0; i<n; i++){
        const char* locale = uenum_next(avail, NULL, &ec);
        if (!assertSuccess("uenum_next", &ec)) goto END;
        log_verbose("%s\n", locale);
        for (k=0; k<2; ++k) {
            uint32_t option = (k==0) ? 0 : USET_CASE_INSENSITIVE;
            USet* exemplarSet = ulocdata_getExemplarSet(NULL, locale, option, &ec);
            uset_close(exemplarSets[k]);
            exemplarSets[k] = exemplarSet;
            if (!assertSuccess("ulocaledata_getExemplarSet", &ec)) goto END;

            codeLen = uscript_getCode(locale, code, 8, &ec);
            if (!assertSuccess("uscript_getCode", &ec)) goto END;

            for (j=0; j<MAX_SCRIPTS_PER_LOCALE; ++j) {
                uset_close(codeSets[j]);
                codeSets[j] = NULL;
            }
            for (j=0; j<codeLen; ++j) {
                uprv_strcpy(cbuf, "[:");
                uprv_strcat(cbuf, uscript_getShortName(code[j]));
                uprv_strcat(cbuf, ":]");
                u_uastrcpy(ubuf, cbuf);
                codeSets[j] = uset_openPattern(ubuf, -1, &ec);
            }
            if (!assertSuccess("uset_openPattern", &ec)) goto END;

            existsInScript = FALSE;
            itemCount = uset_getItemCount(exemplarSet);
            for (m=0; m<itemCount && !existsInScript; ++m) {
                strLen = uset_getItem(exemplarSet, m, &start, &end, ubuf,
                                      sizeof(ubuf)/sizeof(ubuf[0]), &ec);
                /* failure here might mean str[] needs to be larger */
                if (!assertSuccess("uset_getItem", &ec)) goto END;
                if (strLen == 0) {
                    for (j=0; j<codeLen; ++j) {
                        if (uset_containsRange(codeSets[j], start, end)) {
                            existsInScript = TRUE;
                            break;
                        }
                    }
                } else {
                    for (j=0; j<codeLen; ++j) {
                        if (uset_containsString(codeSets[j], ubuf, strLen)) {
                            existsInScript = TRUE;
                            break;
                        }
                    }
                }
            }

            if (existsInScript == FALSE){
                log_err("ExemplarSet containment failed for locale : %s", locale);
            }
        }
        assertTrue("case-folded is a superset",
                   uset_containsAll(exemplarSets[1], exemplarSets[0]));
        if (uset_equals(exemplarSets[1], exemplarSets[0])) {
            ++equalCount;
        }
    }
    /* Note: The case-folded set should sometimes be a strict superset
       and sometimes be equal. */
    assertTrue("case-folded is sometimes a strict superset, and sometimes equal",
               equalCount > 0 && equalCount < n);
    
 END:
    uenum_close(avail);
    uset_close(exemplarSets[0]);
    uset_close(exemplarSets[1]);
    for (i=0; i<MAX_SCRIPTS_PER_LOCALE; ++i) {
        uset_close(codeSets[i]);
    }
}
