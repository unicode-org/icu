/********************************************************************
 * COPYRIGHT: 
 * Copyright (C) 2016 and later: Unicode, Inc. and others.
 * License & terms of use: http://www.unicode.org/copyright.html
 ********************************************************************/

#ifndef _INTLTESTNUMBERFORMATAPI
#define _INTLTESTNUMBERFORMATAPI

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

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
    void testRegistration();
};

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif
