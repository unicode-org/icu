/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

/**
 * Collation currency tests.
 * (It's important to stay current!)
 */

#ifndef _CURRCOLL
#define _CURRCOLL

#ifndef _UTYPES
#include "unicode/utypes.h"
#endif

#ifndef _COLL
#include "unicode/coll.h"
#endif

#ifndef _COLEITR
#include "unicode/coleitr.h"
#endif

#ifndef _INTLTEST
#include "intltest.h"
#endif

#ifndef _UNISTR
#include "unicode/unistr.h"
#endif

class CollationCurrencyTest: public IntlTest
{
public:

    enum EToken_Len { MAX_TOKEN_LEN = 16 };

    CollationCurrencyTest();
    virtual ~CollationCurrencyTest();
    void runIndexedTest(int32_t index, UBool exec, const char* &name, char* par = NULL);

    void currencyTest(/*char *par*/);
};

#endif
