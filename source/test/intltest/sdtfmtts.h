
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

#ifndef _INTLTESTSIMPLEDATEFORMATAPI
#define _INTLTESTSIMPLEDATEFORMATAPI


#include "utypes.h"
#include "intltest.h"

/**
 * Test basic functionality of various API functions
 **/
class IntlTestSimpleDateFormatAPI : public IntlTest {
    void runIndexedTest( int32_t index, bool_t exec, char* &name, char* par = NULL );  

private:
    /**
     * Performs tests on many API functions, see detailed comments in source code
     **/
    void testAPI(char *par);
};

#endif
