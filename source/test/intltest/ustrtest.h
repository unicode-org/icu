/*
*****************************************************************************************
*                                                                                       *
* COPYRIGHT:                                                                            *
*   (C) Copyright Taligent, Inc.,  1997                                                 *
*   (C) Copyright International Business Machines Corporation,  1997-1998               *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.                  *
*   US Government Users Restricted Rights - Use, duplication, or disclosure             *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                              *
*                                                                                       *
*****************************************************************************************
*/

#include "intltest.h"

/**
 * Perform API and functionality tests for class UnicodeString
 **/
class UnicodeStringTest: public IntlTest {
public:
    UnicodeStringTest();
    ~UnicodeStringTest();
    
    void runIndexedTest( int32_t index, bool_t exec, char* &name, char* par = NULL );

    /**
     * Test some basic methods (insert, remove, replace, ...)
     **/
    void TestBasicManipulation(void);
    /**
     * Test the methods for comparison
     **/
    void TestCompare(void);
    /**
     * Test the methods for extracting
     **/
    void TestExtract(void);
    /**
     * More extensively test methods for removing and replacing
     **/
    void TestRemoveReplace(void);
    /**
     * Test language specific case conversions
     **/
    void TestCaseConversion(void);
    /**
     * Test methods indexOf and lastIndexOf
     **/
    void TestSearching(void);
    /**
     * Test methods for padding, trimmimg and truncating
     **/
    void TestSpacePadding(void);
    /**
     * Test methods startsWith and endsWith
     **/
    void TestPrefixAndSuffix(void);
    /**
     * Test method findAndReplace
     **/
    void TestFindAndReplace(void);
    /**
     * Test method numDisplayCells
     **/
    void TestCellWidth(void);
    /**
     * Test method reverse
     **/
    void TestReverse(void);
    /**
     * Test a few miscellaneous methods (isBogus, hashCode,...)
     **/
    void TestMiscellaneous(void);
    /**
     * Test the functionality of allocating UnicodeStrings on the stack
     **/
    void TestStackAllocation(void);
};

