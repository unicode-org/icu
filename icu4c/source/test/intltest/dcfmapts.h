
/*
********************************************************************
* COPYRIGHT: 
* (C) Copyright Taligent, Inc., 1997
* (C) Copyright International Business Machines Corporation, 1997 - 1998
* Licensed Material - Program-Property of IBM - All Rights Reserved. 
* US Government Users Restricted Rights - Use, duplication, or disclosure 
* restricted by GSA ADP Schedule Contract with IBM Corp. 
*
********************************************************************
*/

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
};

#endif
