/*
*****************************************************************************************
*                                                                                       *
* COPYRIGHT:                                                                            *
*   (C) Copyright Taligent, Inc.,  1997                                                 *
*   (C) Copyright International Business Machines Corporation,  1997-1998               *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.                  *
*   US Government Users Restricted Rights - Use, duplication, or disclosure             *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                              *
*                                                                                       *
*****************************************************************************************
*/

/**
 * CollationEnglishTest is a third level test class.  This tests the locale
 * specific primary, secondary and tertiary rules.  For example, the ignorable
 * character '-' in string "black-bird".  The en_US locale uses the default
 * collation rules as its sorting sequence.
 */

#ifndef _ENCOLL
#define _ENCOLL

#ifndef _UTYPES
#include "unicode/utypes.h"
#endif

#ifndef _COLL
#include "unicode/coll.h"
#endif

#ifndef _INTLTEST
#include "intltest.h"
#endif

class CollationEnglishTest: public IntlTest {
public:
    // static constants
    enum EToken_Len { MAX_TOKEN_LEN = 128 };

    CollationEnglishTest();
    ~CollationEnglishTest();
    void runIndexedTest( int32_t index, bool_t exec, char* &name, char* par = NULL );

    // main test routine, tests rules defined by the "en" locale
    void doTest( UnicodeString source, UnicodeString target, Collator::EComparisonResult result);

    // performs test with strength PRIMARY
    void TestPrimary( char* par );

    // perform test with strength SECONDARY
    void TestSecondary( char* par );

    // perform test with strength TERTIARY
    void TestTertiary( char* par );

private:
    static const UChar testBugs[][MAX_TOKEN_LEN];
    static const UChar testSourceCases[][MAX_TOKEN_LEN];
    static const UChar testTargetCases[][MAX_TOKEN_LEN];
    static const Collator::EComparisonResult results[];
    static const UChar testAcute[][MAX_TOKEN_LEN];

    Collator *myCollation;
};
#endif
