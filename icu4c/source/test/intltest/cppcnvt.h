/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

#include "intltest.h"

/**
 * Test for APIs of CPPUnicodeConverter
 **/
class ConvertTest: public IntlTest {
public:
    ConvertTest() {};
    ~ConvertTest() {};
    
    void runIndexedTest( int32_t index, UBool exec, const char* &name, char* par = NULL );

    /**
     * Test everything
     **/
    void TestConvert(void);
    void TestAmbiguous(void);
};



