/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

/**
 * CollationISOTest is a third level test class.  This tests the ISO 14651
 * test entries with the French locale.
 */

#ifndef _ISOCOLL
#define _ISOCOLL

#ifndef _UTYPES
#include "unicode/utypes.h"
#endif

#ifndef _COLL
#include "unicode/coll.h"
#endif

#ifndef _TBLCOLL
#include "unicode/tblcoll.h"
#endif

#ifndef _INTLTEST
#include "intltest.h"
#endif

class CollationISOTest: public IntlTest {
public:
    // static constants
    enum EToken_Len { MAX_TOKEN_LEN = 5 };

    CollationISOTest();
    virtual ~CollationISOTest();
    void runIndexedTest( int32_t index, UBool exec, char* &name, char* par = NULL );

    // test proper comparing for 3774 entries of ISO 14651 characters
    void TestAll( char* par );

private:
    void quickSort(CollationKey** keyArray, int *indexes, int lo0, int hi0);

    static const UChar testCases[][MAX_TOKEN_LEN];

    Collator *myCollation;
};
#endif
