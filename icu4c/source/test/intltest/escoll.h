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
 * CollationSpanishTest is a third level test class. This tests the locale
 * specific primary and tertiary rules. This Spanish sort uses the traditional
 * sorting sequence.  The Spanish modern sorting sequence does not sort
 * ch and ll as unique characters.
 */

#ifndef _ESCOLL
#define _ESCOLL

#ifndef _UTYPES
#include "unicode/utypes.h"
#endif

#ifndef _COLL
#include "unicode/coll.h"
#endif

#ifndef _INTLTEST
#include "intltest.h"
#endif

class CollationSpanishTest: public IntlTest {
public:
    // static constants
    enum EToken_Len { MAX_TOKEN_LEN = 128 };

    CollationSpanishTest();
    ~CollationSpanishTest();
    void runIndexedTest( int32_t index, bool_t exec, char* &name, char* par = NULL );

    // amin test routine, tests rules specific to the spanish locale
    void doTest( UnicodeString source, UnicodeString target, Collator::EComparisonResult result);

    // performs tests with strength PRIMARY
    void TestPrimary( char* par );

    // prforms test with strength TERTIARY
    void TestTertiary( char* par );

private:
    static const UChar testSourceCases[][MAX_TOKEN_LEN];
    static const UChar testTargetCases[][MAX_TOKEN_LEN];
    static const Collator::EComparisonResult results[];

    Collator *myCollation;
};
#endif
