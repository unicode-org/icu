/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 2001, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/************************************************************************
*   Date        Name        Description
*   10/03/2001   Ram        Creation.
************************************************************************/

#ifndef INDICTRN_H
#define INDICTRN_H

#include "unicode/translit.h"
#include "intltest.h"

class Transliterator;

/**
 * @test
 * @summary General test of Transliterator
 */
class IndicLatinTest : public IntlTest {
public:
    void runIndexedTest(int32_t index, UBool exec, const char* &name, char* par=NULL);

    void TestDevanagariLatinRT(void);

    void TestTeluguLatinRT(void);

    void TestGujaratiLatinRT(void);
    
    void TestSanskritLatinRT(void);

    /*Internal functions used*/
    void doTest(const UnicodeString& , const UnicodeString& , const UnicodeString& );
};


#endif

