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
 * Collation currency tests.
 * (It's important to stay current!)
 */

#ifndef _CURRCOLL
#define _CURRCOLL

#ifndef _UTYPES
#include "utypes.h"
#endif

#ifndef _COLL
#include "coll.h"
#endif

#ifndef _COLEITR
#include "coleitr.h"
#endif

#ifndef _INTLTEST
#include "intltest.h"
#endif

#ifndef _UNISTR
#include "unistr.h"
#endif

class CollationCurrencyTest: public IntlTest
{
public:

    enum EToken_Len { MAX_TOKEN_LEN = 16 };

    CollationCurrencyTest();
    ~CollationCurrencyTest();

    void runIndexedTest(int32_t index, bool_t exec, char* &name, char* par = NULL);

    void currencyTest(char *par);
};

#endif
