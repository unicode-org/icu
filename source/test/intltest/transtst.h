/*
**********************************************************************
*   Copyright (C) 1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   11/10/99    aliu        Creation.
**********************************************************************
*/
#ifndef TRANSTST_H
#define TRANSTST_H

#include "unicode/utypes.h"
#include "intltest.h"

class Transliterator;

/**
 * @test
 * @summary General test of Transliterator
 */
class TransliteratorTest : public IntlTest {

    void runIndexedTest(int32_t index, UBool exec, char* &name,
                        char* par=NULL);

    void TestInstantiation(void);
    
    void TestSimpleRules(void);

    void TestInlineSet(void);

    void TestPatternQuoting(void);

    /**
     * Create some inverses and confirm that they work.  We have to be
     * careful how we do this, since the inverses will not be true
     * inverses -- we can't throw any random string at the composition
     * of the transliterators and expect the identity function.  F x
     * F' != I.  However, if we are careful about the input, we will
     * get the expected results.
     */
    void TestRuleBasedInverse(void);

    /**
     * Basic test of keyboard.
     */
    void TestKeyboard(void);

    /**
     * Basic test of keyboard with cursor.
     */
    void TestKeyboard2(void);

    /**
     * Test keyboard transliteration with back-replacement.
     */
    void TestKeyboard3(void);
    
    void keyboardAux(const Transliterator& t,
                     const char* DATA[], int32_t DATA_length);
    
    void TestArabic(void);

    /**
     * Compose the Kana transliterator forward and reverse and try
     * some strings that should come out unchanged.
     */
    void TestCompoundKana(void);

    /**
     * Compose the hex transliterators forward and reverse.
     */
    void TestCompoundHex(void);

    /**
     * Do some basic tests of filtering.
     */
    void TestFiltering(void);

    /**
     * Regression test for bugs found in Greek transliteration.
     */
    void TestJ277(void);

    /**
     * Prefix, suffix support in hex transliterators.
     */
    void TestJ243(void);

    /**
     * Parsers need better syntax error messages.
     */
    void TestJ329(void);

    /**
     * Test segments and segment references.
     */
    void TestSegments(void);
    
    /**
     * Test cursor positioning outside of the key
     */
    void TestCursorOffset(void);
    
    /**
     * Test zero length and > 1 char length variable values.  Test
     * use of variable refs in UnicodeSets.
     */
    void TestArbitraryVariableValues(void);

    /**
     * Confirm that the contextStart, contextLimit, start, and limit
     * behave correctly. J474.
     */
    void TestPositionHandling(void);

    /**
     * Test the Hiragana-Katakana transliterator.
     */
    void TestHiraganaKatakana(void);


    //======================================================================
    // Support methods
    //======================================================================
    void expect(const UnicodeString& rules,
                const UnicodeString& source,
                const UnicodeString& expectedResult);

    void expect(const Transliterator& t,
                const UnicodeString& source,
                const UnicodeString& expectedResult,
                const Transliterator& reverseTransliterator);
    
    void expect(const Transliterator& t,
                const UnicodeString& source,
                const UnicodeString& expectedResult);
    
    void expectAux(const UnicodeString& tag,
                   const UnicodeString& source,
                   const UnicodeString& result,
                   const UnicodeString& expectedResult);
    
    void expectAux(const UnicodeString& tag,
                   const UnicodeString& summary, UBool pass,
                   const UnicodeString& expectedResult);
};

#endif
