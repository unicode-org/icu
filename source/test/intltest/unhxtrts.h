/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-2003, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************
************************************************************************
*   Date         Name        Description
*   03/15/2000   Madhu        Creation.
************************************************************************/

#ifndef UNITOHEXTRTST_H
#define UNITOHEXTRTST_H

#include "unicode/utypes.h"

#if !UCONFIG_NO_TRANSLITERATION

#include "unicode/translit.h"
#include "unitohex.h"
#include "intltest.h"

/**
 * @test
 * @summary General test of UnicodeToHexadecimal Transliterator
 */
class UniToHexTransliteratorTest : public IntlTest {
public:
    void runIndexedTest(int32_t index, UBool exec, const char* &name, char* par=NULL);

    /*Tests the constructors */
    void TestConstruction(void);
    /*Tests the function clone, and operator==()*/
    void TestCloneEqual(void);
    /*Tests the function isUppercase and setUppercase()*/
    void TestUpperCase(void);
    /*Tests the function getTransliterator() and setTransliterators() and adoptTransliterators()*/
    void TestPattern(void);
    /*Tests the function handleTransliterate()*/
    void TestSimpleTransliterate();
    /*Tests the function handleTransliterate()*/
    void TestTransliterate();

    //======================================================================
    // Support methods
    //======================================================================
    void expectTranslit(const UnicodeToHexTransliterator& t,
                        const UnicodeString& message,
                        const UnicodeString& source, 
                        int32_t start, int32_t limit, int32_t cursor,
                        const UnicodeString& expectedResult);

    void expectPattern(UnicodeToHexTransliterator& t,
                       const UnicodeString& pattern, 
                       const UnicodeString& source, 
                       const UnicodeString& expectedResult);

    void expect(const UnicodeToHexTransliterator& t,
                const UnicodeString& message,
                const UnicodeString& source,
                const UnicodeString& expectedResult);

    void expectAux(const UnicodeString& tag,
                   const UnicodeString& summary, UBool pass,
                   const UnicodeString& expectedResult);


};

#endif /* #if !UCONFIG_NO_TRANSLITERATION */

#endif
