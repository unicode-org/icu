/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

#ifndef _INTLTESTDECIMALFORMATSYMBOLS
#define _INTLTESTDECIMALFORMATSYMBOLS


#include "unicode/utypes.h"
#include "intltest.h"

/**
 * Tests for DecimalFormatSymbols
 **/
class IntlTestDecimalFormatSymbols: public IntlTest {
    void runIndexedTest( int32_t index, bool_t exec, char* &name, char* par = NULL );  

private:
    /**
     * Test the API of DecimalFormatSymbols; primarily a simple get/set set.
     */
    void testSymbols(char *par);
};

#endif
