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
 * CollationISOTest is a third level test class.  This tests the ISO 14651
 * test entries with the French locale.
 */

#ifndef _ISOCOLL
#define _ISOCOLL

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

class CollationISOTest: public IntlTest {
public:
    // static constants
    enum EToken_Len { MAX_TOKEN_LEN = 5 };

    CollationISOTest();
    ~CollationISOTest();
    void runIndexedTest( int32_t index, bool_t exec, char* &name, char* par = NULL );

    // test proper comparing for 3774 entries of ISO 14651 characters
    void TestAll( char* par );

private:
    void quickSort(CollationKey** keyArray, int *indexes, int lo0, int hi0);

    static const UChar testCases[][MAX_TOKEN_LEN];

    Collator *myCollation;
};
#endif
