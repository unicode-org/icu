/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

/**
 * MajorTestLevel is the top level test class for everything in the directory "IntlWork".
 */

#ifndef _INTLTESTFORMAT
#define _INTLTESTFORMAT


#include "unicode/utypes.h"
#include "intltest.h"


class IntlTestFormat: public IntlTest {
    void runIndexedTest( int32_t index, UBool exec, char* &name, char* par = NULL );
};


#endif
