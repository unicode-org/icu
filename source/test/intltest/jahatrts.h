/*
/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-2001, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************
************************************************************************
*   Date         Name        Description
*   03/20/2000   Madhu        Creation.
************************************************************************/

#ifndef JAMOTOHANGTRTST_H
#define JAMOTOHANGTRTST_H

#include "unicode/utypes.h"
#include "unicode/translit.h"
#include "unicode/jamohang.h"
#include "intltest.h"

class JamoHangulTransliterator;

/**
 * @test
 * @summary General test of JamoToHangul Transliterator
 */
class JamoToHangTransliteratorTest : public IntlTest {
public:
    void runIndexedTest(int32_t index, UBool exec, const char* &name, char* par=NULL);

    /*Tests the constructors */
    void TestConstruction(void);
    /*Tests the function clone, and operator==()*/
    void TestCloneEqual(void);
    /*Tests the function handleTransliterate()*/
    void TestSimpleTransliterate(void);
    /*Tests the function handleTransliterate()*/
    void TestTransliterate(void);
    void TestTransliterate2(void);


    //======================================================================
    // Support methods
    //======================================================================
    void expectTranslit(const JamoHangulTransliterator& t,
                        const UnicodeString& message,
                        const UnicodeString& source, 
                        int32_t start,
                        int32_t limit, 
                        int32_t cursor,
                        const UnicodeString& expectedResult);

    void expect(const JamoHangulTransliterator& t,
                const UnicodeString& message,
                const UnicodeString& source,
                const UnicodeString& expectedResult);
      
    void expectAux(const UnicodeString& tag,
                   const UnicodeString& summary, UBool pass,
                   const UnicodeString& expectedResult);


};

#endif




