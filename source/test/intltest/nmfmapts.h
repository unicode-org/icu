/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

#ifndef _INTLTESTNUMBERFORMATAPI
#define _INTLTESTNUMBERFORMATAPI


#include "unicode/utypes.h"
#include "intltest.h"


/**
 *  This test executes basic functionality checks of various API functions
 **/
class IntlTestNumberFormatAPI: public IntlTest {
    void runIndexedTest( int32_t index, UBool exec, const char* &name, char* par = NULL );  

private:
    /**
     * executes tests of API functions, see detailed comments in source code
     **/
    void testAPI(/* char* par */);
};

#endif
