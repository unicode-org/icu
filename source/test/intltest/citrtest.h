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
    
    void runIndexedTest( int32_t index, UBool exec, char* &name, char* par = NULL );

    /**
     * Test Constructors and operators ==, != and a few other methods
     **/
    void TestConstructionAndEquality(void);
    /**
     * Test Constructors and operators ==, != and a few other methods for UChariter
     **/
    void TestConstructionAndEqualityUChariter(void);
    /**
     * test the iteration functionality in different ways
     **/
    void TestIteration(void);
	 /**
     * test the iteration functionality in different ways with  unicodestring of UChar32's
     **/
    void TestIterationUChar32(void);
};



