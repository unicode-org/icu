/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 2002, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

/* Created by weiv 05/09/2002 */

/* Base class for data driven tests */

#ifndef INTLTST_TESTDATADRIVER
#define INTLTST_TESTDATADRIVER

#include "unicode/utypes.h"
#include "unicode/unistr.h"
#include "intltest.h"

/* This class abstracts the actual organization of the  */
/* data for data driven tests                             */

/* The usage model when per test settings are present is the following:     */
/*
 TestDataDriver *d = 
   TestDataDriver::createTestInstance("Name", status); 
 const char* settingNames[capacity1];
 UnicodeString settingValues[capacity1];
 int32_t noOfSettings = 0;
 UnicodeString case[capacity2];
 int32_t lengthOfCase = 0;
 while(getNextTest(testName, status) {                                          
  while(noOfSettings = getNextSettingsSet(settingNames, settingValues, capacity, status) {                             
    set the playfield according to settings
    while(lengthOfCase = getNextTestCase(case, capacity2, status) {
     do test with this case
    }
  }
 }
*/

/* If separate tests do not require different settings */
/*
 TestDataDriver *d = 
   TestDataDriver::createTestInstance("Name", status); 
 UnicodeString case[capacity2];
 int32_t lengthOfCase = 0;
 while(getNextTest(testName, status) {                                          
  while(lengthOfCase = getNextTestCase(case, capacity2, status) {
   do test with this case
  }
 }
*/


class TestDataDriver : public IntlTest { // inheriting from intltest just to be able to use errln. 
public:
  // Factory method. Give it a test name 
  static TestDataDriver *createTestInstance(const char* testName, UErrorCode &status);
  virtual ~TestDataDriver() {};
protected:
  TestDataDriver(const char* testName);
public:
  // reset the test iterator
  virtual void resetTests(UErrorCode &status) = 0;
  // returns number of tests
  virtual int32_t countTests(void);

  // Gets the module wide settings
  virtual int32_t getModuleSettings(const char **names, UnicodeString * values, int32_t capacity, UErrorCode &status) = 0;

  // get the next test from this test set. returns FALSE if there are no more tests
  // gets a name of test ("da_TestPrimary", "da_TestTertiary"...)
  virtual UBool getNextTest(const char * &name, UErrorCode &status) = 0;
  // get the test by index. Returns the name of the test
  virtual UBool getTest(int32_t index, const char * &name, UErrorCode &status) = 0;
  // get test by name.
  virtual UBool getTest(const char *name, UErrorCode &status) = 0;

  // Getting the next test object will reset both the test arguments and test case iterators
  virtual int32_t getNextSettingsSet(const char **names, UnicodeString * values, int32_t capacity, UErrorCode &status) = 0;
  // Gets the next test case
  virtual int32_t getNextTestCase(UnicodeString * testCase, int32_t capacity, UErrorCode &status) = 0;

protected:
  const char* fTestName;
  int32_t fNumberOfTests;

};

#endif
