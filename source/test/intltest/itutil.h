/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

/**
 * MajorTestLevel is the top level test class for everything in the directory "IntlWork".
 */

#ifndef _INTLTESTUTILITIES
#define _INTLTESTUTILITIES


#include "unicode/utypes.h"
#include "intltest.h"


class IntlTestUtilities: public IntlTest {
public:
    void runIndexedTest( int32_t index, UBool exec, char* &name, char* par = NULL );
};


#endif
