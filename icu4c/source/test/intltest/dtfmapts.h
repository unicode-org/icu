
/*
********************************************************************
* COPYRIGHT: 
* (C) Copyright Taligent, Inc., 1997
* (C) Copyright International Business Machines Corporation, 1997 - 1998
* Copyright (C) 1999 Alan Liu and others. All rights reserved.
* Licensed Material - Program-Property of IBM - All Rights Reserved. 
* US Government Users Restricted Rights - Use, duplication, or disclosure 
* restricted by GSA ADP Schedule Contract with IBM Corp. 
*
********************************************************************
*/

#ifndef _INTLTESTDATEFORMATAPI
#define _INTLTESTDATEFORMATAPI


#include "unicode/utypes.h"
#include "intltest.h"


/*
 * This is an API test, not a unit test.  It doesn't test very many cases, and doesn't
 * try to test the full functionality.  It just calls each function in the class and
 * verifies that it works on a basic level.
 */
class IntlTestDateFormatAPI: public IntlTest {
    void runIndexedTest( int32_t index, bool_t exec, char* &name, char* par = NULL );  

private:
    /**
     * Tests basic functionality of various generic API methods in DateFormat 
     */
    void testAPI(char *par);
    /**
     * Test that the equals method works correctly.
     */
    void TestEquals(void);

    /**
     * Test that no parse or format methods are hidden.
     */
    void TestNameHiding(void);
};

#endif
