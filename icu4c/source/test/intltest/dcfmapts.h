/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/


#ifndef _INTLTESTDECIMALFORMATAPI
#define _INTLTESTDECIMALFORMATAPI


#include "unicode/utypes.h"
#include "intltest.h"


class IntlTestDecimalFormatAPI: public IntlTest {
    void runIndexedTest( int32_t index, bool_t exec, char* &name, char* par = NULL );  

private:
    /**
     * Tests basic functionality of various API functions for DecimalFormat
     **/
    void testAPI(char *par);
	void testRounding(char *par);
	
    /*Helper functions */
	void verify(const UnicodeString& message, const UnicodeString& got, double expected);
};

#endif
