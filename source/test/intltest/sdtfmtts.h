/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

#ifndef _INTLTESTSIMPLEDATEFORMATAPI
#define _INTLTESTSIMPLEDATEFORMATAPI


#include "unicode/utypes.h"
#include "intltest.h"

/**
 * Test basic functionality of various API functions
 **/
class IntlTestSimpleDateFormatAPI : public IntlTest {
    void runIndexedTest( int32_t index, UBool exec, const char* &name, char* par = NULL );  

private:
    /**
     * Performs tests on many API functions, see detailed comments in source code
     **/
    void testAPI(char *par);
};

#endif
