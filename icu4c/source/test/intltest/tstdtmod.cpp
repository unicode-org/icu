/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 2002, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

/* Created by weiv 05/09/2002 */

#include "tstdtmod.h"
#include "cmemory.h"

TestDataModule *TestDataModule::getTestDataModule(const char* name, TestLog& log, UErrorCode &status)
{
  if(U_FAILURE(status)) {
    return NULL;
  }
  TestDataModule *result = NULL;

  // TODO: probe for resource bundle and then for XML.
  // According to that, construct an appropriate driver object

  result = new RBTestDataModule(name, log, status);
  if(U_SUCCESS(status)) {
    return result;
  } else {
    return NULL;
  }
}

TestDataModule::TestDataModule(const char* name, TestLog& log, UErrorCode& status)
: name(name),
fInfo(NULL),
log(log)
{
}

TestDataModule::~TestDataModule() {
  if(fInfo != NULL) {
    delete fInfo;
  }
}

const char * TestDataModule::getName() const
{
  return name;
}



RBTestDataModule::~RBTestDataModule()
{
  ures_close(fTestData);
  ures_close(fModuleBundle);
  ures_close(info);
  uprv_free(tdpath);
}

RBTestDataModule::RBTestDataModule(const char* name, TestLog& log, UErrorCode& status) 
: TestDataModule(name, log, status),
  fModuleBundle(NULL),
  fTestData(NULL),
  info(NULL),
  tdpath(NULL)
{
  fNumberOfTests = 0;
  fDataTestValid = TRUE;
  fModuleBundle = getTestBundle(name, status);
  if(fDataTestValid) {
    fTestData = ures_getByKey(fModuleBundle, "TestData", NULL, &status);
    fNumberOfTests = ures_getSize(fTestData);
    info = ures_getByKey(fModuleBundle, "Info", NULL, &status);
    if(status != U_ZERO_ERROR) {
      log.errln("Unable to initalize test data - missing mandatory description resources!");
      fDataTestValid = FALSE;
    } else {
      fInfo = new RBDataMap(info, status);
    }
  }
}

UBool RBTestDataModule::getInfo(const DataMap *& info, UErrorCode &status) const
{
  if(fInfo) {
    info = fInfo;
    return TRUE;
  } else {
    info = NULL;
    return FALSE;
  }
}

TestData* RBTestDataModule::createTestData(int32_t index, UErrorCode &status) const 
{
  TestData *result = NULL;
  UErrorCode intStatus = U_ZERO_ERROR;

  // Both of these resources get adopted by a TestData object.
  UResourceBundle *DataFillIn = ures_getByIndex(fTestData, index, NULL, &status); 
  UResourceBundle *headers = ures_getByKey(info, "Headers", NULL, &intStatus);
  
  if(U_SUCCESS(status)) {
    result = new RBTestData(DataFillIn, headers, status);

    if(U_SUCCESS(status)) {
      return result;
    }
  }
  return NULL;
}

TestData* RBTestDataModule::createTestData(const char* name, UErrorCode &status) const
{
  TestData *result = NULL;
  UErrorCode intStatus = U_ZERO_ERROR;

  // Both of these resources get adopted by a TestData object.
  UResourceBundle *DataFillIn = ures_getByKey(fTestData, name, NULL, &status); 
  UResourceBundle *headers = ures_getByKey(info, "Headers", NULL, &intStatus);
   
  if(U_SUCCESS(status)) {
    result = new RBTestData(DataFillIn, headers, status);
    if(U_SUCCESS(status)) {
      return result;
    }
  }
  return NULL;
}



//Get test data from ResourceBundles
UResourceBundle* 
RBTestDataModule::getTestBundle(const char* bundleName, UErrorCode &status) 
{
  if(U_SUCCESS(status)) {
    UResourceBundle *testBundle = NULL;
    const char* icu_data = (char*)loadTestData(status);
    if (testBundle == NULL) {
        testBundle = ures_openDirect(icu_data, bundleName, &status);
        if (status != U_ZERO_ERROR) {
            log.errln(UnicodeString("Failed: could not load test data from resourcebundle: ") + UnicodeString(bundleName));
            fDataTestValid = FALSE;
        }
    }
    return testBundle;
  } else {
    return NULL;
  }
}

const char* 
RBTestDataModule::loadTestData(UErrorCode& err){
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
                log.errln("construction of NULL did not succeed  :  %s \n", u_errorName(err));
                return "";
            }
            ures_close(test);
            return tdpath;
        }
        ures_close(test);
        return tdpath;
    }
    return tdpath;
}

