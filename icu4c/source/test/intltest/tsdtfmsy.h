
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

#ifndef _INTLTESTDATEFORMATSYMBOLS
#define _INTLTESTDATEFORMATSYMBOLS


#include "unicode/utypes.h"
#include "intltest.h"

/**
 * Tests for DateFormatSymbols
 **/
class IntlTestDateFormatSymbols: public IntlTest {
    void runIndexedTest( int32_t index, bool_t exec, char* &name, char* par = NULL );  

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
