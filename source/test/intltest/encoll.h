/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-2001, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

/**
 * CollationEnglishTest is a third level test class.  This tests the locale
 * specific primary, secondary and tertiary rules.  For example, the ignorable
 * character '-' in string "black-bird".  The en_US locale uses the default
 * collation rules as its sorting sequence.
 */

#ifndef _ENCOLL
#define _ENCOLL

#include "tscoll.h"

class CollationEnglishTest: public IntlTestCollator {
public:
    // If this is too small for the test data, just increase it.
    // Just don't make it too large, otherwise the executable will get too big
    enum EToken_Len { MAX_TOKEN_LEN = 16 };

    CollationEnglishTest();
    virtual ~CollationEnglishTest();
    void runIndexedTest( int32_t index, UBool exec, const char* &name, char* par = NULL );

    // main test routine, tests rules defined by the "en" locale
    void doTest( UnicodeString source, UnicodeString target, Collator::EComparisonResult result);

    // performs test with strength PRIMARY
    void TestPrimary(/* char* par */);

    // perform test with strength SECONDARY
    void TestSecondary(/* char* par */);

    // perform test with strength TERTIARY
    void TestTertiary(/* char* par */);

private:
    static const UChar testBugs[][MAX_TOKEN_LEN];
    static const UChar testSourceCases[][MAX_TOKEN_LEN];
    static const UChar testTargetCases[][MAX_TOKEN_LEN];
    static const Collator::EComparisonResult results[];
    static const UChar testAcute[][MAX_TOKEN_LEN];

    Collator *myCollation;
};
#endif
