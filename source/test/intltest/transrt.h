/*
**********************************************************************
*   Copyright (C) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   05/23/00    aliu        Creation.
**********************************************************************
*/
#ifndef TRANSRT_H
#define TRANSRT_H

#include "unicode/utypes.h"
#include "intltest.h"

class Transliterator;

/**
 * @test
 * @summary Round trip test of Transliterator
 */
class TransliteratorRoundTripTest : public IntlTest {

    void runIndexedTest(int32_t index, UBool exec, const char* &name,
                        char* par=NULL);

    void TestHiragana(void);
    void TestKatakana(void);
    void TestArabic(void);
    void TestHebrew(void);
    void TestHangul(void);
    void TestGreek(void);
    void TestCyrillic(void);
    void TestJamo(void);
    void TestJamoHangul(void);
};

#endif
