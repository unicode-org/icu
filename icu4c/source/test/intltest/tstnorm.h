/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-2001, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

/**
 * Normalizer basic tests
 */

#ifndef _TSTNORM
#define _TSTNORM

#include "unicode/normlzr.h"
#include "intltest.h"

class BasicNormalizerTest : public IntlTest {
public:
    BasicNormalizerTest();
    virtual ~BasicNormalizerTest();

    void runIndexedTest( int32_t index, UBool exec, const char* &name, char* par = NULL );

    void TestHangulCompose(void);
    void TestHangulDecomp(void);
    void TestPrevious(void);
    void TestDecomp(void);
    void TestCompatDecomp(void);
    void TestCanonCompose(void);
    void TestCompatCompose(void);
    void TestTibetan(void);
    void TestCompositionExclusion(void);
    void TestZeroIndex(void);
    void TestVerisign(void);

private:
    UnicodeString canonTests[24][3];
    UnicodeString compatTests[11][3];
    UnicodeString hangulCanon[2][3];


    //------------------------------------------------------------------------
    // Internal utilities
    //
    void backAndForth(Normalizer* iter, const UnicodeString& input);

    void staticTest(Normalizer::EMode mode, int options,
                    UnicodeString tests[][3], int length, int outCol);

    void iterateTest(Normalizer* iter, UnicodeString tests[][3], int length, int outCol);

    void assertEqual(const UnicodeString& input,
             const UnicodeString& expected, 
             Normalizer* result,
             const UnicodeString& errPrefix);

    static UnicodeString hex(UChar ch);
    static UnicodeString hex(const UnicodeString& str);

};

#endif // _TSTNORM
