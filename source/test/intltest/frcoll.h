/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

/**
 * CollationFrenchTest is a third level test class. This tests the locale
 * specific tertiary rules.  For example, the French secondary sorting on
 * accented characters.
 */
#ifndef _FRCOLL
#define _FRCOLL

#ifndef _UTYPES
#include "unicode/utypes.h"
#endif

#ifndef _COLL
#include "unicode/coll.h"
#endif

#ifndef _INTLTEST
#include "intltest.h"
#endif

class CollationFrenchTest: public IntlTest {
public:
    // static constants
    enum EToken_Len { MAX_TOKEN_LEN = 128 };

    CollationFrenchTest();
    ~CollationFrenchTest();
    void runIndexedTest( int32_t index, bool_t exec, char* &name, char* par = NULL );

    // amin test routine, test rules specific to the french locale
    void doTest( UnicodeString source, UnicodeString target, Collator::EComparisonResult result);

    // perform tests with strength SECONDARY
    void TestSecondary( char* par );

    // perform tests with strength TERTIARY
    void TestTertiary( char* par );

    // perform extra tests
    void TestExtra( char* par );

private:
    static const UChar testSourceCases[][MAX_TOKEN_LEN];
    static const UChar testTargetCases[][MAX_TOKEN_LEN];
    static const UChar testBugs[][MAX_TOKEN_LEN];
    static const Collator::EComparisonResult results[];
    static const UChar testAcute[][MAX_TOKEN_LEN];

    Collator *myCollation;
};

#endif
