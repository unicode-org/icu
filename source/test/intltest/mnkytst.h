/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

/**
 * CollationMonkeyTest is a third level test class.  This tests the random 
 * substrings of the default test strings to verify if the compare and 
 * sort key algorithm works correctly.  For example, any string is always
 * less than the string itself appended with any character.
 */

#ifndef _MNKYTST
#define _MNKYTST

#ifndef _UTYPES
#include "unicode/utypes.h"
#endif

#ifndef _COLL
#include "unicode/coll.h"
#endif

#ifndef _INTLTEST
#include "intltest.h"
#endif

class CollationMonkeyTest: public IntlTest {
public:
    // static constants
    enum EToken_Len { MAX_TOKEN_LEN = 128 };

    CollationMonkeyTest();
    ~CollationMonkeyTest();
    void runIndexedTest( int32_t index, UBool exec, char* &name, char* par = NULL );

    // utility function used in tests, returns absolute value
    int32_t checkValue(int32_t value);

    // perform monkey tests using Collator::compare
    void TestCompare( char* par );

    // perform monkey tests using CollationKey::compareTo
    void TestCollationKey( char* par );

    void TestRules(char* par);

private:
    void report(UnicodeString& s, UnicodeString& t, int32_t result, int32_t revResult);

    void doTest(RuleBasedCollator *myCollation, UnicodeString source, UnicodeString target, Collator::EComparisonResult result);

    static const UnicodeString source;

    Collator *myCollator;
};
#endif
