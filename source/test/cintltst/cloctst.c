/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
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
#include "unicode/utypes.h"
#include "unicode/utypes.h"
#include "cloctst.h"
#include "unicode/uloc.h"
#include <stdio.h>
#include <string.h>
#include "unicode/ustring.h"
#include "cintltst.h"

void PrintDataTable();

static char* rawData2[23][5];

static UChar*** dataTable=0;
enum {
    ENGLISH = 0,
    FRENCH = 1,
    CROATIAN = 2,
    GREEKS = 3,
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
    DNAME_EL = 22
};


void addLocaleTest(TestNode** root)
{
  static void TestCPforLocale();

    setUpDataTable();
    
    addTest(root, &TestBasicGetters, "tsutil/cloctst/TestBasicGetters");
    addTest(root, &TestDisplayNames, "tsutil/cloctst/TestDisplayNames");
    addTest(root, &TestGetAvailableLocales, "tsutil/cloctst/TestGetAvailableLocales");
    addTest(root, &TestDataDirectory, "tsutil/cloctst/TestDataDirectory");
    addTest(root, &TestISOFunctions, "tsutil/cloctst/TestISOFunctions");
    addTest(root, &TestCPforLocale, "tsutil/cloctst/TestCPforLocale");
}
        

/* testing uloc(), uloc_getName(), uloc_getLanguage(), uloc_getVariant(), uloc_getCountry() */
void TestBasicGetters() {
    int32_t i;
    int32_t cap;
    UErrorCode status = U_ZERO_ERROR;
    char *testLocale = 0;
    char *temp = 0, *name = 0;
    log_verbose("Testing Basic Getters\n");
    for (i = 0; i <= MAX_LOCALES; i++) {
        testLocale=(char*)malloc(sizeof(char) * (strlen(rawData2[NAME][i])+1));
        strcpy(testLocale,rawData2[NAME][i]);
        
        log_verbose("Testing   %s  .....\n", testLocale);
        cap=uloc_getLanguage(testLocale, NULL, 0, &status);
        if(status==U_BUFFER_OVERFLOW_ERROR){
            status=U_ZERO_ERROR;
            temp=(char*)malloc(sizeof(char) * (cap+1));
            uloc_getLanguage(testLocale, temp, cap, &status);
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
            uloc_getCountry(testLocale, temp, cap, &status);
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
            uloc_getVariant(testLocale, temp, cap, &status);
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
            uloc_getName(testLocale, name, cap, &status);
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
/* testing uloc_getISO3Language(), uloc_getISO3Country(),  */
void TestSimpleResourceInfo() {

    int32_t i;
    char* testLocale = 0;
    UChar* expected = 0;
    
    const char* temp;
    testLocale=(char*)malloc(sizeof(char) * 1);
    expected=(UChar*)malloc(sizeof(UChar) * 1);
    
    log_verbose("Testing getISO3Language and getISO3Country\n");
    for (i = 0; i <= MAX_LOCALES; i++) {
        
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
          
    }
    
 free(expected);
 free(testLocale);
}
void TestDisplayNames() 
{
  /* sfb 990721 
     Can't just save a pointer to the default locale.  
     Although the pointer won't change, the contents will, so the
     restore at the end doesn't actually restore the original.
  */
    const char *saveDefault;
    char *defaultLocale;
    
    UErrorCode err = U_ZERO_ERROR;
    

    saveDefault = uloc_getDefault();
    defaultLocale = (char*) malloc(strlen(saveDefault) + 1);
    if(defaultLocale == 0) {
      log_err("out of memory");
      return;
    }
    strcpy(defaultLocale, saveDefault);

    uloc_setDefault("en_US", &err);
    if (U_FAILURE(err)) {
        log_err("uloc_setDefault returned error code ");
        return;
    }
    
    
    log_verbose("Testing getDisplayName for different locales\n");
    log_verbose("With default = en_US...\n");
   
    log_verbose("  In default locale...\n");
    doTestDisplayNames(" ", DLANG_EN, FALSE);
    log_verbose("  In locale = en_US...\n");
    doTestDisplayNames("en_US", DLANG_EN, FALSE);
    log_verbose("  In locale = fr_FR....\n");
    doTestDisplayNames("fr_FR", DLANG_FR, FALSE);
    log_verbose("  In locale = hr_HR...\n");
    doTestDisplayNames("hr_HR", DLANG_HR, FALSE);
    log_verbose("  In locale = gr_EL..\n");
    doTestDisplayNames("el_GR", DLANG_EL, FALSE);

   uloc_setDefault("fr_FR", &err);
    if (U_FAILURE(err)) {
        log_err("Locale::setDefault returned error code  %s\n", myErrorName(err));
        return;
    }
    
    log_verbose("With default = fr_FR...\n");
    
    log_verbose("  In default locale...\n");
    doTestDisplayNames(" ", DLANG_FR, TRUE);
    log_verbose("  In locale = en_US...\n");
    doTestDisplayNames("en_US", DLANG_EN, TRUE);
    log_verbose("  In locale = fr_FR....\n");
    doTestDisplayNames("fr_FR", DLANG_FR, TRUE);
    log_verbose("  In locale = hr_HR...\n");
    doTestDisplayNames("hr_HR", DLANG_HR, TRUE);
    log_verbose("  In locale = el_GR...\n");
    doTestDisplayNames("el_GR", DLANG_EL, TRUE);

    uloc_setDefault(defaultLocale, &err);
    if (U_FAILURE(err)) {
        log_err("Locale::setDefault returned error code  %s\n", myErrorName(err));
        return;
    }

    free(defaultLocale);

}


/* test for uloc_getAvialable()  and uloc_countAvilable()*/
void TestGetAvailableLocales()
{

    const char *locList;
    int32_t locCount,i;
    
    log_verbose("Testing the no of avialable locales\n");
    locCount=uloc_countAvailable();
    if (locCount == 0)
        log_err("countAvailable() returned an empty list!\n");
    
    /* use something sensible w/o hardcoding the count */
    else if(locCount < 0){ 
        log_err("countAvailable() returned a wrong value!= %d\n", locCount);
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
void TestDataDirectory()
{

    char            oldDirectory[80];
    const char     *temp,*testValue1,*testValue2,*testValue3;
    UErrorCode       err = U_ZERO_ERROR;
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

void doTestDisplayNames(const char* inLocale,
                                    int32_t compareIndex,
                                    int32_t defaultIsFrench)
{
    UErrorCode status = U_ZERO_ERROR;
    int32_t i;
    int32_t maxresultsize;
    
    char* testLocale;
   
    
    UChar  *testLang  = 0;
    UChar  *testCtry = 0;
    UChar  *testVar = 0;
    UChar  *testName = 0;
   

    UChar*  expectedLang = 0;
    UChar*  expectedCtry = 0;
    UChar*  expectedVar = 0;
    UChar*  expectedName = 0;
    char temp[5];
    const char* defaultDefaultLocale=" ";
    
    
       
    uloc_getLanguage(uloc_getDefault(), temp, 5, &status);
    if(U_FAILURE(status)){
        log_err("ERROR: in getDefault  %s \n", myErrorName(status));
    }
    if (defaultIsFrench && 0 != strcmp(temp, "fr"))    {
        log_err("Default locale should be French, but it's really  %s\n", temp);
        
    }
    else if (!defaultIsFrench && 0 != strcmp(temp, "en")){  
    
        log_err("Default locale should be English, but it's really  %s\n", temp);
        
    }

   testLocale = (char*)malloc(sizeof(char)   * 1);
  
   
   for(i=0;i<MAX_LOCALES; ++i)
   {
       testLocale=(char*)realloc(testLocale, sizeof(char) * (u_strlen(dataTable[NAME][i])+1));
       u_austrcpy(testLocale,dataTable[NAME][i]);
    
        log_verbose("Testing.....  %s\n", testLocale);
                
        if (strcmp(inLocale, defaultDefaultLocale)==0) {
            maxresultsize=0;
            maxresultsize=uloc_getDisplayLanguage(testLocale, NULL, NULL, maxresultsize, &status);
            if(status==U_BUFFER_OVERFLOW_ERROR)
            {
                status=U_ZERO_ERROR;
                testLang=(UChar*)malloc(sizeof(UChar) * (maxresultsize + 1));
                uloc_getDisplayLanguage(testLocale, NULL, testLang, maxresultsize, &status);


            }
            if(U_FAILURE(status)){
                    log_err("Error in getDisplayLanguage()  %s\n", myErrorName(status));
                    
            }
            maxresultsize=0;
            maxresultsize=uloc_getDisplayCountry(testLocale, NULL, NULL, maxresultsize, &status);
            if(status==U_BUFFER_OVERFLOW_ERROR)
            {
                status=U_ZERO_ERROR;
                testCtry=(UChar*)malloc(sizeof(UChar) * (maxresultsize + 1));
                uloc_getDisplayCountry(testLocale, NULL, testCtry, maxresultsize, &status);
            }
            if(U_FAILURE(status)){
                    log_err("Error in getDisplayCountry()  %s\n", myErrorName(status));
                    
            }
            
            maxresultsize=0;
            maxresultsize=uloc_getDisplayVariant(testLocale, NULL, NULL, maxresultsize, &status);
            if(status==U_BUFFER_OVERFLOW_ERROR)
            {
                status=U_ZERO_ERROR;
                testVar=(UChar*)malloc(sizeof(UChar) * (maxresultsize+1));
                uloc_getDisplayVariant(testLocale, NULL, testVar, maxresultsize, &status);
            }
            if(U_FAILURE(status)){
                    log_err("Error in getDisplayVariant()  %s\n", myErrorName(status));
                
            }
            maxresultsize=0;
            maxresultsize=uloc_getDisplayName(testLocale, NULL, NULL, maxresultsize, &status);
            if(status==U_BUFFER_OVERFLOW_ERROR)
            {
                status=U_ZERO_ERROR;
                testName=(UChar*)malloc(sizeof(UChar) * (maxresultsize+1));
                uloc_getDisplayName(testLocale, NULL, testName, maxresultsize, &status);
            }
            if(U_FAILURE(status)){
                    log_err("Error in getDisplayName()  %s\n", myErrorName(status));
                
            }
           
        }
        else {
            maxresultsize=0;
            maxresultsize=uloc_getDisplayLanguage(testLocale, inLocale, NULL, maxresultsize, &status);
            if(status==U_BUFFER_OVERFLOW_ERROR)
            {
                status=U_ZERO_ERROR;
                testLang=(UChar*)malloc(sizeof(UChar) * (maxresultsize+1));
                uloc_getDisplayLanguage(testLocale, inLocale, testLang, maxresultsize, &status);
            }
            if(U_FAILURE(status)){
                log_err("Error in getDisplayLanguage()  %s\n", myErrorName(status));
            }

            maxresultsize=0;
            maxresultsize=uloc_getDisplayCountry(testLocale, inLocale, NULL, maxresultsize, &status);
            if(status==U_BUFFER_OVERFLOW_ERROR)
            {
                status=U_ZERO_ERROR;
                testCtry=(UChar*)malloc(sizeof(UChar) * (maxresultsize+1));
                uloc_getDisplayCountry(testLocale, inLocale, testCtry, maxresultsize, &status);
            }
            if(U_FAILURE(status)){
                log_err("Error in getDisplayCountry()  %s\n", myErrorName(status));
            }
            
            maxresultsize=0;
            maxresultsize=uloc_getDisplayVariant(testLocale, inLocale, NULL, maxresultsize, &status);
            if(status==U_BUFFER_OVERFLOW_ERROR)
            {
                status=U_ZERO_ERROR;
                testVar=(UChar*)malloc(sizeof(UChar) * (maxresultsize+1));
                uloc_getDisplayVariant(testLocale, inLocale, testVar, maxresultsize, &status);
            }
            if(U_FAILURE(status)){
                    log_err("Error in getDisplayVariant()  %s\n", myErrorName(status));
            }
            
            maxresultsize=0;
            maxresultsize=uloc_getDisplayName(testLocale, inLocale, NULL, maxresultsize, &status);
            if(status==U_BUFFER_OVERFLOW_ERROR)
            {
                status=U_ZERO_ERROR;
                testName=(UChar*)malloc(sizeof(UChar) * (maxresultsize+1));
                uloc_getDisplayName(testLocale, inLocale, testName, maxresultsize, &status);
            }
            if(U_FAILURE(status)){
                log_err("Error in getDisplayName()  %s\n", myErrorName(status));
            }

        }
    
        expectedLang=dataTable[compareIndex][i];
        if(u_strlen(expectedLang) == 0 && defaultIsFrench)  
            expectedLang=dataTable[DLANG_FR][i];
        if(u_strlen(expectedLang)== 0)
            expectedLang=dataTable[DLANG_EN][i];

    
        expectedCtry=dataTable[compareIndex + 1][i];
        if(u_strlen(expectedCtry) == 0 && defaultIsFrench)
            expectedCtry=dataTable[DCTRY_FR][i];
        if(u_strlen(expectedCtry)== 0)
            expectedCtry=dataTable[DCTRY_EN][i];    
   
        expectedVar=dataTable[compareIndex + 2][i];
        if(u_strlen(expectedVar) == 0 && defaultIsFrench)
            expectedVar=dataTable[DVAR_FR][i];
        if(u_strlen(expectedCtry)== 0)
            expectedVar=dataTable[DVAR_EN][i];
        
        
        expectedName=dataTable[compareIndex + 3][i];
        if(u_strlen(expectedName) ==0 && defaultIsFrench)
            expectedName=dataTable[DNAME_FR][i];
        if(u_strlen(expectedName) == 0)
            expectedName=dataTable[DNAME_EN][i];
       
        
     if (0 !=u_strcmp(testLang,expectedLang))  {
            log_err(" Display Language mismatch: %s  versus  %s\n", austrdup(testLang), austrdup(expectedLang));
            
          }
        
        if (0 != u_strcmp(testCtry,expectedCtry))   {
            log_err(" Display Country mismatch: %s  versus  %s\n", austrdup(testCtry), austrdup(expectedCtry));
            
          }
        
        if (0 != u_strcmp(testVar,expectedVar))    {
            log_err(" Display Variant mismatch: %s  versus  %s\n", austrdup(testVar), austrdup(expectedVar));
            
          }
       
        if(0 != u_strcmp(testName, expectedName))    {    
            log_err(" Display Name mismatch: %s  versus  %s\n", austrdup(testName), austrdup(expectedName));
        }
       
    free(testName);
    free(testLang);
    free(testCtry);
    free(testVar);

    }
    free(testLocale);
}
/* test for uloc_getISOLanguages, uloc_getISOCountries */
void TestISOFunctions()
{



    int32_t count  = 0;
    
    bool_t done = FALSE;
    
    const char* const* str=uloc_getISOLanguages();
    const char* const* str1=uloc_getISOCountries();
    /*  test getISOLanguages*/
    count = 0;
    done = FALSE;
    /*str=uloc_getISOLanguages(); */
    log_verbose("Testing ISO Languages: \n");
    
    while(!done){
        
        if(*(str+count++) == 0)
    
            done = TRUE;
                
        
    }
    count--;
    if(count!=142)
        
        log_err("There is an error in getISOLanguages %d\n", count);
    
    log_verbose("Testing ISO Countries");
    count=0;
    done=FALSE;
    while(!done)
    {
        if(*(str1 + count++)==0)
            done=TRUE;
            
    
    }
    count--;
    if(count!=239)
        log_err("There is an error in getISOCountries %d \n", count);

    
  
}



/*---------------------------------------------------
  table of valid data
 --------------------------------------------------- */


static  char* rawData2[23][5] = {
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

    /* display langage (English) */
    {   "English",  "French",   "Croatian", "Greek",    "Norwegian" },
    /* display country (English) */
    {   "United States",    "France",   "Croatia",  "Greece",   "Norway"    },
    /* display variant (English) */
    {   "",     "",     "",     "",     "Nynorsk"    },
    /* display name (English) */
    {   "English (United States)", "French (France)", "Croatian (Croatia)", "Greek (Greece)", "Norwegian (Norway, Nynorsk)" },

    /* display langage (French) */
    {   "anglais",  "français",   "", "grec",    "norvégien" },
    /* display country (French) */
    {   "États-Unis",    "France",   "",  "Grèce",   "Norvège"    },
    /* display variant (French) */
    {   "",     "",     "",     "",     "Nynorsk"    },
    /* display name (French) */
    {   "anglais (États-Unis)", "français (France)", "", "grec (Grèce)", "norvégien (Norvège, Nynorsk)" },

    /* display langage (Croatian) */
    {   "",  "", "hrvatski", "",    "" },
    /* display country (Croatian)  */
    {   "",    "",   "Hrvatska",  "",   ""    },
    /* display variant (Croatian) */
    {   "",     "",     "",     "",     ""    },
    /* display name (Croatian) */
    {   "", "", "hrvatski (Hrvatska)", "", "" },

    /* display langage (Greek) [actual values listed below] */
    {   "",  "", "", "",    "" },
    /* display country (Greek) [actual values listed below] */
    {   "",    "",   "",  "",   ""    },
    /* display variant (Greek) */
    {   "",     "",     "",     "",     ""    },
    /* display name (Greek) [actual values listed below] */
    {   "", "", "", "", "" }
};

static UChar greekDisplayLanguage[] = { 0x03b5, 0x03bb, 0x03bb, 0x03b7, 0x03bd, 0x03b9, 0x03ba, 0x03ac, 0 };
static UChar greekDisplayCountry[] = { 0x0395, 0x03bb, 0x03bb, 0x03ac, 0x03b4, 0x03b1, 0 };
static UChar greekDisplayName[] = { 0x03b5, 0x03bb, 0x03bb, 0x03b7, 0x03bd, 0x03b9, 0x03ba,
    0x03ac, ' ', '(', 0x0395, 0x03bb, 0x03bb, 0x03ac, 0x03b4, 0x03b1, ')', 0 };
    

void setUpDataTable()
{
    int32_t i,j;
    dataTable = calloc(sizeof(UChar**),23);

        for (i = 0; i < 23; i++) {
          dataTable[i] = calloc(sizeof(UChar*),5);
            for (j = 0; j < 5; j++){
                dataTable[i][j] = (UChar*) malloc(sizeof(UChar)*(strlen(rawData2[i][j])+1));
                u_uastrcpy(dataTable[i][j],rawData2[i][j]);
            }
        }
        dataTable[DLANG_EL][GREEKS]=(UChar*)realloc(dataTable[DLANG_EL][GREEKS],sizeof(UChar)*(u_strlen(greekDisplayLanguage)+1)); 
    u_strncpy(dataTable[DLANG_EL][GREEKS],greekDisplayLanguage,8);
        dataTable[DCTRY_EL][GREEKS]=(UChar*)realloc(dataTable[DCTRY_EL][GREEKS],sizeof(UChar)*(u_strlen(greekDisplayCountry)+1));
    u_strncpy(dataTable[DCTRY_EL][GREEKS],greekDisplayCountry,6);
        dataTable[DNAME_EL][GREEKS]=(UChar*)realloc(dataTable[DNAME_EL][GREEKS],sizeof(UChar)*(u_strlen(greekDisplayName)+1));        
    u_strncpy(dataTable[DNAME_EL][GREEKS],greekDisplayName,17);
    
}

void TestCPforLocale()
{
  int32_t i;
  const char *c;

  /* not API */
  U_CAPI const char *uprv_defaultCodePageForLocale(const char *locale);


  struct
  {
    const char *l;
    const char *c;
  }
  data[] = 
  {
    { "zh", "gb2312"   },
    { "zh_TW", "Big5"   },
    { "zh_TW_Taipei", "Big5" }, /* variant even */
    { "zh_CN_Beijing", "gb2312" },
    { "z", NULL },               /* nothing */
    { "es", "iso-8859-1" },
    { "mt_MT_QORMI", "iso-8859-3" }, /* variant, of a language w/ no variant */
    { "ja_JP", "Shift_JIS" },       /* Specific tests: */
    { "ko_KR", "euc-kr" },          /* " */
    { ""  , NULL    },
    { NULL, NULL }
    
  };

  log_info("Testing uprv_defaultCodePageForLocale()\n");

  for(i=0; data[i].l != NULL; i++)
  {
    c = uprv_defaultCodePageForLocale(data[i].l);

    if((c == NULL) && (data[i].c != NULL))
      {
        log_err("uprv_defaultCodePageForLocale(\"%s\") == NULL, expected \"%s\" **ERR**\n",
                data[i].l,
                data[i].c);
      }
    else if((data[i].c == NULL) && (c != NULL))
      {
        log_err("uprv_defaultCodePageForLocale(\"%s\") == \"%s\", expected NULL **ERR**\n",
                data[i].l,
                c);
      }
    else if(c == NULL)
      {
        log_verbose("uprv_defaultCodePageForLocale(\"%s\") == NULL\n",
                data[i].l);
      }
    else if(0 != strcmp(c, data[i].c))
      {
        log_err("uprv_defaultCodePageForLocale(\"%s\") == \"%s\", expected \"%s\" **ERR**\n",
                data[i].l,
                c,
                data[i].c);
      }
    else
      {
        log_verbose("uprv_defaultCodePageForLocale(\"%s\") == \"%s\"\n",
                 data[i].l,
                 (c!=NULL) ? c:"NULL" );
      }
  }
}
