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
#include "tedadrvr.h"


class DataDrivenCollatorTest: public IntlTestCollator {
    void runIndexedTest(int32_t index, UBool exec, const char* &name, char* par = NULL );
public:
    DataDrivenCollatorTest();
    ~DataDrivenCollatorTest();
protected:

    void DataDrivenTest(char *par);
    void processSequence(Collator* col, const UnicodeString &sequence, UErrorCode &status);
    void processTest(UErrorCode &status);
    void processArguments(Collator *col, const UChar *start, int32_t optLen, UErrorCode &status);
    UBool setTestSequence(const UnicodeString &setSequence, UnicodeString &source, Collator::EComparisonResult &relation, UErrorCode &status);
    UBool getNextInSequence(UnicodeString &source, Collator::EComparisonResult &relation, UErrorCode &status);
private:
  StringCharacterIterator seq;
  TestDataDriver *driver;
};


#endif
