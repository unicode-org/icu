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
 * CollationDanishTest is a third level test class.  This tests the locale
 * specific primary and tertiary rules.  For example, a-ring sorts after z
 * and a-umlaut sorts before ae ligatures.  Additional test strings from the
 * internet are also included.
 */

#ifndef _DACOLL
#define _DACOLL

#ifndef _UTYPES
#include "unicode/utypes.h"
#endif

#ifndef _COLL
#include "unicode/coll.h"
#endif

#ifndef _INTLTEST
#include "intltest.h"
#endif

class CollationDanishTest: public IntlTest {
public:
    // static constants
    enum EToken_Len { MAX_TOKEN_LEN = 128 };

    CollationDanishTest();
    ~CollationDanishTest();
    void runIndexedTest( int32_t index, bool_t exec, char* &name, char* par = NULL );

    // main test routine, Tests rules specific to danish collation
    void doTest( UnicodeString source, UnicodeString target, Collator::EComparisonResult result);

    // perform tests with strength PRIMARY
    void TestPrimary( char* par );

    // perform test with strength TERTIARY
    void TestTertiary( char* par );

private:
    static const UChar testBugs[][MAX_TOKEN_LEN];
    static const UChar testNTList[][MAX_TOKEN_LEN];
    static const UChar testSourceCases[][MAX_TOKEN_LEN];
    static const UChar testTargetCases[][MAX_TOKEN_LEN];
    static const Collator::EComparisonResult results[];

    Collator *myCollation;
};
#endif
