/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

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



