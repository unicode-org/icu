
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
 * tests Choice Format, functionality of examples, as well as API functionality
 **/
class TestChoiceFormat: public IntlTest {
    /** 
     *    tests basic functionality in a simple example
     **/
    void TestSimpleExample(void); 
    /**
     *    tests functionality in a more complex example,
     *    and extensive API functionality.
     *    See verbose message output statements for specifically tested API
     **/
    void TestComplexExample(void);
    /**
     * test the use of next_Double with ChoiceFormat
     **/
    void TestChoiceNextDouble(void);
    /** 
     * test the numerical results of next_Double and previous_Double
     **/
    void TestGapNextDouble(void);
    /**
     * utiltity function for TestGapNextDouble
     **/
    void testValue( double val );

    /** 
     *    runs tests in local funtions:
     **/
    void runIndexedTest( int32_t index, bool_t exec, char* &name, char* par = NULL );
};
