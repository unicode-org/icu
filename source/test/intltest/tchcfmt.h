
/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

/**
 * TestChoiceFormat is a third level test class
 */


#include "unicode/utypes.h"
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
    void runIndexedTest( int32_t index, UBool exec, const char* &name, char* par = NULL );
};
