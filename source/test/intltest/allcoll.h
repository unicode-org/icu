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
 * CollationDummyTest is a third level test class.  This tests creation of 
 * a customized collator object.  For example, number 1 to be sorted 
 * equlivalent to word 'one'.
 */

#ifndef _ALLCOLL
#define _ALLCOLL

#ifndef _UTYPES
#include "utypes.h"
#endif

#ifndef _COLL
#include "coll.h"
#endif

#ifndef _TBLCOLL
#include "tblcoll.h"
#endif

#ifndef _INTLTEST
#include "intltest.h"
#endif

class CollationDummyTest: public IntlTest {
public:
    // static constants
    enum EToken_Len { MAX_TOKEN_LEN = 128 };

    CollationDummyTest();
    ~CollationDummyTest();
    void runIndexedTest( int32_t index, bool_t exec, char* &name, char* par = NULL );

    // main test method called with different strengths,
    // tests comparison of custum collation with different strengths
    void doTest( UnicodeString source, UnicodeString target, Collator::EComparisonResult result);

    // perform test with strength PRIMARY
    void TestPrimary( char* par );

    // perform test with strength SECONDARY
    void TestSecondary( char* par );

    // perform test with strength tertiary
    void TestTertiary( char* par );

    // perform extra tests
    void TestExtra( char* par );

private:
    static const UChar testCases[][MAX_TOKEN_LEN];
    static const UChar testSourceCases[][MAX_TOKEN_LEN];
    static const UChar testTargetCases[][MAX_TOKEN_LEN];
    static const Collator::EComparisonResult results[];

    RuleBasedCollator *myCollation;
};
#endif
