/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

/**
 * CollationFinnishTest is a third level test class. This tests the locale
 * specific primary and tertiary rules. For example, a-ring sorts after z
 * and w and v are equivalent.
 */

#ifndef _FICOLL
#define _FICOLL

#ifndef _UTYPES
#include "unicode/utypes.h"
#endif

#ifndef _COLL
#include "unicode/coll.h"
#endif

#ifndef _INTLTEST
#include "intltest.h"
#endif

class CollationFinnishTest: public IntlTest {
public:
    // static constants
    enum EToken_Len { MAX_TOKEN_LEN = 128 };

    CollationFinnishTest();
    virtual ~CollationFinnishTest();
    void runIndexedTest( int32_t index, UBool exec, const char* &name, char* par = NULL );

    // main test routine, tests rules specific to the finish locale
    void doTest( UnicodeString source, UnicodeString target, Collator::EComparisonResult result);

    // perform tests with strength PRIMARY
    void TestPrimary(/* char* par */);

    // perform test with strength TERTIARY
    void TestTertiary(/* char* par */);

private:
    static const UChar testSourceCases[][MAX_TOKEN_LEN];
    static const UChar testTargetCases[][MAX_TOKEN_LEN];
    static const Collator::EComparisonResult results[];

    Collator *myCollation;
};
#endif
