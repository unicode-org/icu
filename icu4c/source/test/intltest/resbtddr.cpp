/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 2002, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

/* Created by weiv 05/09/2002 */

#include "resbtddr.h"
#include "cmemory.h"
#include "cstring.h"

ResBundTestDataDriver::ResBundTestDataDriver(const char* testName, UErrorCode &status)
: TestDataDriver(testName),
  fTestBundle(NULL),
  fPurpose(NULL),
  fParsing(NULL),
  fModuleSettings(NULL),
  fTestData(NULL),
  fCurrentTest(NULL),
  fTestSettings(NULL),
  fCurrentSettings(NULL),
  fTestCases(NULL),
  fCurrentCase(NULL),
  fDataTestValid(TRUE),
  tdpath(NULL)
{
  fNumberOfTests = 0;
  fTestBundle = getTestBundle(testName);
  if(fDataTestValid) {
    fPurpose = ures_getByKey(fTestBundle, "TestPurpose", NULL, &status);
    fParsing = ures_getByKey(fTestBundle, "TestDataParsing", NULL, &status);
    fTestData = ures_getByKey(fTestBundle, "TestData", NULL, &status);
    fNumberOfTests = ures_getSize(fTestData);
    if(status != U_ZERO_ERROR) {
      errln("Unable to initalize test data - missing mandatory description resources!");
      fDataTestValid = FALSE;
    }
    fCurrentTest = NULL;
  }
}

ResBundTestDataDriver::~ResBundTestDataDriver() 
{
  ures_close(fParsing);
  ures_close(fPurpose);
  ures_close(fModuleSettings);
  ures_close(fCurrentTest);
  ures_close(fTestData);
  ures_close(fTestBundle);
  ures_close(fTestSettings);
  ures_close(fCurrentSettings);
  ures_close(fTestCases);
  ures_close(fCurrentCase);
  uprv_free(tdpath);
}

void 
ResBundTestDataDriver::resetTests(UErrorCode &status) 
{
  ures_resetIterator(fTestData);
}

UBool
ResBundTestDataDriver::initSettingsAndCases(UErrorCode &status)
{
  fTestSettings = ures_getByKey(fCurrentTest, "Settings", fTestSettings, &status);
  if(U_FAILURE(status)) {
    ures_close(fTestSettings);
    fTestSettings = NULL;
    status = U_ZERO_ERROR;
  }
  fTestCases = ures_getByKey(fCurrentTest, "TestCases", fTestCases, &status);
  if(U_FAILURE(status)) {
    ures_close(fTestCases);
    fTestCases = NULL;
    errln("Couldn't find test cases!");
    return FALSE;
  }
  return TRUE;
}

UBool 
ResBundTestDataDriver::getNextTest(const char * &name, UErrorCode &status) 
{
  if(!fDataTestValid) {
    fNumberOfTests = 0;
  }

  if(ures_hasNext(fTestData)) {
    fCurrentTest = ures_getNextResource(fTestData, fCurrentTest, &status);
    name = ures_getKey(fCurrentTest);
    return initSettingsAndCases(status);
  } else {
    return FALSE;
  }
}

// get the test by index. Returns the name of the test
UBool 
ResBundTestDataDriver::getTest(int32_t index, const char * &name, UErrorCode &status)
{
  fCurrentTest = ures_getByIndex(fTestData, index, fCurrentTest, &status);
  if(status != U_ZERO_ERROR) {
    return FALSE;
  }
  name = ures_getKey(fCurrentTest);
  return initSettingsAndCases(status);
}
// get test by name.
UBool 
ResBundTestDataDriver::getTest(const char *name, UErrorCode &status) {
  fCurrentTest = ures_getByKey(fTestData, name, fCurrentTest, &status);
  if(status != U_ZERO_ERROR) {
    return FALSE;
  }
  return initSettingsAndCases(status);
}

int32_t
ResBundTestDataDriver::fillSettingsArrays(UResourceBundle *res, const char **names, UnicodeString * values, int32_t capacity, UErrorCode &status) 
{
  const char *key = NULL;
  int32_t i = 0;
  if(capacity < ures_getSize(fCurrentSettings)) {
    status = U_BUFFER_OVERFLOW_ERROR;
    return 0;
  }
  while(ures_hasNext(fCurrentSettings)) {
    values[i] = ures_getNextUnicodeString(fCurrentSettings, &key, &status);
    names[i++] = key;
  }
  return ures_getSize(fCurrentSettings);
}

// Gets the module wide settings
int32_t 
ResBundTestDataDriver::getModuleSettings(const char **names, UnicodeString * values, int32_t capacity, UErrorCode &status)
{
  fModuleSettings = ures_getByKey(fTestBundle, "Settings", fModuleSettings, &status);
  if(status == U_ZERO_ERROR) {
    return fillSettingsArrays(fModuleSettings, names, values, capacity, status);
  } else {
    return 0;
  }
}

int32_t 
ResBundTestDataDriver::getNextSettingsSet(const char **names, UnicodeString * values, int32_t capacity, UErrorCode &status) 
{
  ures_resetIterator(fTestCases);
  if(ures_hasNext(fTestSettings)) {
    fCurrentSettings = ures_getNextResource(fTestSettings, fCurrentSettings, &status);
    return fillSettingsArrays(fCurrentSettings, names, values, capacity, status);
  } else {
    return 0;
  }

}

int32_t 
ResBundTestDataDriver::getNextTestCase(UnicodeString * testCase, int32_t capacity, UErrorCode &status)
{
  const char *key = NULL;
  int32_t i = 0;
  if(ures_hasNext(fTestCases)) {
    fCurrentCase = ures_getNextResource(fTestCases, fCurrentCase, &status);
    if(capacity < ures_getSize(fCurrentCase)) {
      status = U_BUFFER_OVERFLOW_ERROR;
      return 0;
    }
    while(ures_hasNext(fCurrentCase)) {
      testCase[i++] = ures_getNextUnicodeString(fCurrentCase, &key, &status);
    }
    return ures_getSize(fCurrentCase);
  } else {
    return 0;
  }
}

//Get test data from ResourceBundles
UResourceBundle* 
ResBundTestDataDriver::getTestBundle(const char* bundleName) 
{
    UErrorCode status = U_ZERO_ERROR;
    UResourceBundle *testBundle = NULL;
    const char* icu_data = (char*)loadTestData(status);
    if (testBundle == NULL) {
        testBundle = ures_openDirect(icu_data, bundleName, &status);
        if (status != U_ZERO_ERROR) {
            errln(UnicodeString("Failed: could not load test data from resourcebundle: ") + UnicodeString(bundleName));
            fDataTestValid = FALSE;
        }
    }
    return testBundle;
}

char* 
ResBundTestDataDriver::loadTestData(UErrorCode& err){
    const char*      directory=NULL;
    UResourceBundle* test =NULL;
    const char* tdrelativepath = ".."U_FILE_SEP_STRING"test"U_FILE_SEP_STRING"testdata"U_FILE_SEP_STRING"out"U_FILE_SEP_STRING;
    if( tdpath == NULL){
        directory= u_getDataDirectory();
    
        tdpath = (char*) uprv_malloc(sizeof(char) *(( strlen(directory) * strlen(tdrelativepath)) + 10));


        /* u_getDataDirectory shoul return \source\data ... set the
         * directory to ..\source\data\..\test\testdata\out\testdata
         *
         * Fallback: When Memory mapped file is built
         * ..\source\data\out\..\..\test\testdata\out\testdata
         */
        strcpy(tdpath, directory);
        strcat(tdpath, tdrelativepath);
        strcat(tdpath,"testdata");

    
        test=ures_open(tdpath, "testtypes", &err);
    
        /* we could not find the data in tdpath 
         * try tdpathFallback
         */
        if(U_FAILURE(err))
        {
            strcpy(tdpath,directory);
            strcat(tdpath,".."U_FILE_SEP_STRING);
            strcat(tdpath, tdrelativepath);
            strcat(tdpath,"testdata");
            err =U_ZERO_ERROR;
            test=ures_open(tdpath, "ja_data", &err);
            /* Fall back did not succeed either so return */
            if(U_FAILURE(err)){
                err = U_FILE_ACCESS_ERROR;
                errln("construction of NULL did not succeed  :  %s \n", u_errorName(err));
                strcpy(tdpath, "");
                return tdpath;
            }
            ures_close(test);
            return tdpath;
        }
        ures_close(test);
        return tdpath;
    }
    return tdpath;
}
