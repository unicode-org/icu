/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-2001, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************
************************************************************************
*   Date         Name        Description
*   03/22/2000   Madhu        Creation.
************************************************************************/

#ifndef UNIFLTLOGICTST_H
#define UNIFLTLOGICTST_H

#include "unicode/utypes.h"

#if !UCONFIG_NO_TRANSLITERATION

#include "unicode/translit.h"
#include "unicode/unifltlg.h"
#include "intltest.h"


/**
 * @test
 * @summary General test of UnicodeFilterLogic API
 */
class UnicodeFilterLogicTest : public IntlTest {
public:
    void runIndexedTest(int32_t index, UBool exec, const char* &name, char* par=NULL);

    /*Tests all the NOT, OR and AND filters */
    void TestAll(void);

    void TestNOT(Transliterator& t,
                 const UnicodeFilter* f1, 
                 const UnicodeString& message,
                 const UnicodeString& source,
                 const UnicodeString& expected);

    void TestAND(Transliterator& t,
                 const UnicodeFilter* f1,
                 const UnicodeFilter* f2,
                 const UnicodeString& message,
                 const UnicodeString& source,
                 const UnicodeString& expected);

    void TestOR(Transliterator& t,
                 const UnicodeFilter* f1,
                 const UnicodeFilter* f2,
                 const UnicodeString& message,
                 const UnicodeString& source,
                 const UnicodeString& expected);

    //support functions
    void expect(const Transliterator& t,
                const UnicodeString& message,
                const UnicodeString& source,
                const UnicodeString& expectedResult);

    void expectAux(const UnicodeString& tag,
                   const UnicodeString& summary, UBool pass,
                   const UnicodeString& expectedResult); 

};

#endif /* #if !UCONFIG_NO_TRANSLITERATION */

#endif
