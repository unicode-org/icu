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
 * Some tests for CharacterIterator and StringCharacterIterator
 **/
class CharIterTest: public IntlTest {
public:
    CharIterTest();
    ~CharIterTest();
    
    void runIndexedTest( int32_t index, bool_t exec, char* &name, char* par = NULL );

    /**
     * Test Constructors and operators ==, != and a few other methods
     **/
    void TestConstructionAndEquality(void);
    /**
     * test the iteration functionality in different ways
     **/
    void TestIteration(void);
};



