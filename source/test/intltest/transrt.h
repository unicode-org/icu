/*
**********************************************************************
*   Copyright (C) 2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   05/23/00    aliu        Creation.
**********************************************************************
*/
#ifndef TRANSRT_H
#define TRANSRT_H

#include "unicode/translit.h"
#include "intltest.h"

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
    void TestGreek(void);
    void Testel(void);
    void TestCyrillic(void);
    void TestJamo(void);
    void TestJamoHangul(void);

    void TestDevanagariLatin(void);
    void TestDevanagariBengali(void);
    void TestDevanagariGurmukhi(void);
    void TestDevanagariGujarati(void);
    void TestDevanagariOriya(void);
    void TestDevanagariTamil(void);
    void TestDevanagariTelugu(void);
    void TestDevanagariKannada(void);
    void TestDevanagariMalayalam(void);
    void TestBengaliGurmukhi(void);
    void TestBengaliGujarati(void);
    void TestBengaliOriya(void);
    void TestBengaliTamil(void);
    void TestBengaliTelugu(void);
    void TestBengaliKannada(void);
    void TestBengaliMalayalam(void);  
    void TestGurmukhiGujarati(void);
    void TestGurmukhiOriya(void);
    void TestGurmukhiTamil(void);
    void TestGurmukhiTelugu(void);
    void TestGurmukhiKannada(void);
    void TestGurmukhiMalayalam(void); 
    void TestGujaratiOriya(void);
    void TestGujaratiTamil(void);
    void TestGujaratiTelugu(void);
    void TestGujaratiKannada(void);
    void TestGujaratiMalayalam(void);
    void TestOriyaTamil(void);
    void TestOriyaTelugu(void);
    void TestOriyaKannada(void);
    void TestOriyaMalayalam(void);
    void TestTamilTelugu(void);
    void TestTamilKannada(void);
    void TestTamilMalayalam(void);
    void TestTeluguKannada(void);
    void TestTeluguMalayalam(void);
    void TestKannadaMalayalam(void);
};

#endif
