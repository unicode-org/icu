
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

#ifndef _INTLTESTNUMBERFORMATAPI
#define _INTLTESTNUMBERFORMATAPI


#include "utypes.h"
#include "intltest.h"


/**
 *  This test executes basic functionality checks of various API functions
 **/
class IntlTestNumberFormatAPI: public IntlTest {
    void runIndexedTest( int32_t index, bool_t exec, char* &name, char* par = NULL );  

private:
    /**
     * executes tests of API functions, see detailed comments in source code
     **/
    void testAPI(char *par);
};

#endif
