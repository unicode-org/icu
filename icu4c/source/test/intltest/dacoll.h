/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-2003, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

/**
 * CollationDanishTest is a third level test class.  This tests the locale
 * specific primary and tertiary rules.  For example, a-ring sorts after z
 * and a-umlaut sorts before ae ligatures.  Additional test strings from the
 * internet are also included.
 */

#ifndef _DACOLL
#define _DACOLL

#include "unicode/utypes.h"

#if !UCONFIG_NO_COLLATION

#include "tscoll.h"

class CollationDanishTest: public IntlTestCollator {
public:
    // If this is too small for the test data, just increase it.
    // Just don't make it too large, otherwise the executable will get too big
    enum EToken_Len { MAX_TOKEN_LEN = 24 };

    CollationDanishTest();
    virtual ~CollationDanishTest();
    void runIndexedTest( int32_t index, UBool exec, const char* &name, char* par = NULL );

    // perform tests with strength PRIMARY
    void TestPrimary(/* char* par */);

    // perform test with strength TERTIARY
    void TestTertiary(/* char* par */);

private:
    static const UChar testBugs[][MAX_TOKEN_LEN];
    static const UChar testNTList[][MAX_TOKEN_LEN];
    static const UChar testSourceCases[][MAX_TOKEN_LEN];
    static const UChar testTargetCases[][MAX_TOKEN_LEN];
    static const Collator::EComparisonResult results[];

    Collator *myCollation;
};

#endif /* #if !UCONFIG_NO_COLLATION */

#endif
