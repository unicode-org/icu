/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 2002, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

/* Created by weiv 05/09/2002 */

/* Actual implementation of TestDataDriver that uses resource bundles */

#ifndef INTLTST_RESBUNDTESTDATADRIVER
#define INTLTST_RESBUNDTESTDATADRIVER

#include "tedadrvr.h"
#include "unicode/ures.h"

class ResBundTestDataDriver: public TestDataDriver {
public:
  ResBundTestDataDriver(const char* testName, UErrorCode &status);
  virtual ~ResBundTestDataDriver();
  virtual void resetTests(UErrorCode &status);
  // Gets the module wide settings
  virtual int32_t getModuleSettings(const char **names, UnicodeString * values, int32_t capacity, UErrorCode &status);
  // get next test
  virtual UBool getNextTest(const char * &name, UErrorCode &status);
  // get the test by index. Returns the name of the test
  virtual UBool getTest(int32_t index, const char * &name, UErrorCode &status);
  // get test by name.
  virtual UBool getTest(const char *name, UErrorCode &status);

  virtual int32_t getNextSettingsSet(const char **names, UnicodeString * values, int32_t capacity, UErrorCode &status);
  virtual int32_t getNextTestCase(UnicodeString * testCase, int32_t capacity, UErrorCode &status);
private:
  UResourceBundle *getTestBundle(const char* bundleName);
  const char* loadTestData(UErrorCode& err);
  UBool initSettingsAndCases(UErrorCode &status);
  int32_t fillSettingsArrays(UResourceBundle *res, const char **names, UnicodeString * values, int32_t capacity, UErrorCode &status);

  UResourceBundle *fTestBundle;
  UResourceBundle *fPurpose;
  UResourceBundle *fParsing;
  UResourceBundle *fModuleSettings;
  UResourceBundle *fTestData;
  UResourceBundle *fCurrentTest;
  UResourceBundle *fTestSettings;
  UResourceBundle *fCurrentSettings;
  UResourceBundle *fTestCases;
  UResourceBundle *fCurrentCase;
  UBool fDataTestValid;
};

#endif
