
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

/**
 * TestChoiceFormat is a third level test class
 */


#include "utypes.h"
#include "intltest.h"

/** 
 * tests 3 smaller classes in the format library
 **/
class TestFormatSmallClasses: public IntlTest {
    /**
     * runs tests in 4 local routines,
     * performs test for API and funtionalty of 3 smaller format classes:
     *    ParsePosition in test_ParsePosition(),
     *    FieldPosition in test_FieldPosition(),
     *    Formattable in test_Formattable().
     **/    
    void runIndexedTest( int32_t index, bool_t exec, char* &name, char* par = NULL );
};
