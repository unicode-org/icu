/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-2001, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

#include "intltest.h"

#ifdef ICU_UNICODECONVERTER_USE_DEPRECATES
#include "unicode/convert.h"

/**
 * Test for APIs of CPPUnicodeConverter
 **/
class ConvertTest: public IntlTest {
public:
    ConvertTest() {};
    virtual ~ConvertTest() {};
    
    void runIndexedTest( int32_t index, UBool exec, const char* &name, char* par = NULL );

    /**
     * Test everything
     **/
    void TestConvert(void);
    void TestAmbiguous(void);
};
#endif



