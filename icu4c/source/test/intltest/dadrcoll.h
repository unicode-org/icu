/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-2001, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

/**
 * MajorTestLevel is the top level test class for everything in the directory "IntlWork".
 */

#ifndef _INTLTESTDATADRIVENCOLLATOR
#define _INTLTESTDATADRIVENCOLLATOR


#include "tscoll.h"
#include "unicode/coll.h"
#include "unicode/tblcoll.h"
#include "unicode/sortkey.h"
#include "unicode/schriter.h"
#include "unicode/ures.h"


class DataDrivenCollatorTest: public IntlTestCollator {
    void runIndexedTest(int32_t index, UBool exec, const char* &name, char* par = NULL );
public:
    DataDrivenCollatorTest();
    ~DataDrivenCollatorTest();
protected:
    // These two should probably go down in IntlTest
    const char* loadTestData(UErrorCode& err);
    UResourceBundle* getTestBundle(const char* bundleName);

    void DataDrivenTest(char *par);
    void processReadyCollator(UResourceBundle *test, Collator *col, UErrorCode &status);
    void processCollatorTests(UResourceBundle *test, Collator *col, UErrorCode &status);
    void processTest(UResourceBundle *test, UErrorCode &status);
    void processArguments(Collator *col, const UChar *start, int32_t optLen, UErrorCode &status);
    UBool setTestSequence(const UnicodeString &setSequence, UnicodeString &source, Collator::EComparisonResult &relation, UErrorCode &status);
    UBool getNextInSequence(UnicodeString &source, Collator::EComparisonResult &relation, UErrorCode &status);
    void processSequence(Collator* col, const UnicodeString &sequence, UErrorCode &status);
private:
  StringCharacterIterator seq;
  UResourceBundle *testBundle;
  UResourceBundle *testData;
  UResourceBundle *currentTest;
  UResourceBundle *purpose;
  UResourceBundle *parsing;
  int32_t numberOfTests;
  UBool dataTestValid;
};


#endif
