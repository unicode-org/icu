/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

/**
 * MajorTestLevel is the top level test class for everything in the directory "IntlWork".
 */

#ifndef _INTLTESTCOLLATOR
#define _INTLTESTCOLLATOR


#include "unicode/utypes.h"
#include "intltest.h"


class IntlTestCollator: public IntlTest {
    void runIndexedTest( int32_t index, UBool exec, char* &name, char* par = NULL );
};


#endif
