/********************************************************************
 * Copyright (c) 2003, International Business Machines Corporation
 * and others. All Rights Reserved.
 ********************************************************************/

/**
 * CollationServiceTest tests registration of collators.
 */

#ifndef _SVCCOLL
#define _SVCCOLL

#include "unicode/utypes.h"

#if !UCONFIG_NO_COLLATION

#include "intltest.h"

class CollationServiceTest: public IntlTest {
public:
    void runIndexedTest(int32_t index, UBool exec, const char* &name, char* /*par = NULL */);

    void TestRegister(void);
    void TestRegisterFactory(void);
};

/* #if !UCONFIG_NO_COLLATION */
#endif

/* #ifndef _SVCCOLL */
#endif
