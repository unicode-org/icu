
/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-2001, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

/**
 * TestChoiceFormat is a third level test class
 */


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

// We can deprecate the test code right away; the API itself goes 2002-Jun-30
#ifdef ICU_ENABLE_DEPRECATED_NEXTDOUBLE
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
#endif

    /**
     * Test new closure API
     */
    void TestClosures(void);

    /**
     * Test applyPattern
     */
    void TestPatterns(void);

    void _testPattern(const char* pattern,
                      UBool isValid,
                      double v1, const char* str1,
                      double v2, const char* str2,
                      double v3, const char* str3);
    /** 
     *    runs tests in local funtions:
     **/
    void runIndexedTest( int32_t index, UBool exec, const char* &name, char* par = NULL );
};
