/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

#ifndef _INTLTESTDATEFORMATSYMBOLS
#define _INTLTESTDATEFORMATSYMBOLS


#include "unicode/utypes.h"
#include "intltest.h"

/**
 * Tests for DateFormatSymbols
 **/
class IntlTestDateFormatSymbols: public IntlTest {
    void runIndexedTest( int32_t index, UBool exec, char* &name, char* par = NULL );  

private:
    /**
     * Test the API of DateFormatSymbols; primarily a simple get/set set.
     */
    void testSymbols(char *par);
    /**
     * Test getMonths.
     */
    void TestGetMonths(void);
};

#endif
