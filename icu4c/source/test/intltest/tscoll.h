/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-2001, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

/**
 * MajorTestLevel is the top level test class for everything in the directory "IntlWork".
 */

#ifndef _INTLTESTCOLLATOR
#define _INTLTESTCOLLATOR


#include "intltest.h"
#include "unicode/coll.h"
#include "unicode/sortkey.h"


class IntlTestCollator: public IntlTest {
    void runIndexedTest( int32_t index, UBool exec, const char* &name, char* par = NULL );
protected:
    virtual void reportCResult( UnicodeString &source, UnicodeString &target,
                                CollationKey &sourceKey, CollationKey &targetKey,
                                Collator::EComparisonResult compareResult,
                                Collator::EComparisonResult keyResult,
                                Collator::EComparisonResult incResult,
                                Collator::EComparisonResult expectedResult );

    static UnicodeString &prettify(const CollationKey &source, UnicodeString &target);
    static UnicodeString &appendCompareResult(Collator::EComparisonResult result, UnicodeString &target);
};


#endif
