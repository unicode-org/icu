/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

/**
 * Normalizer basic tests
 */

#ifndef _TSTNORM
#define _TSTNORM

#ifndef _UTYPES
#include "unicode/utypes.h"
#endif

#ifndef _COLL
#include "unicode/coll.h"
#endif

#ifndef _NORMLZR
#include "unicode/normlzr.h"
#endif

#ifndef _INTLTEST
#include "intltest.h"
#endif

class BasicNormalizerTest : public IntlTest {
public:
    BasicNormalizerTest();
    ~BasicNormalizerTest();

    void runIndexedTest( int32_t index, UBool exec, char* &name, char* par = NULL );

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

private:
    static UnicodeString canonTests[][3];
    static UnicodeString compatTests[][3];
    static UnicodeString hangulCanon[][3];


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
