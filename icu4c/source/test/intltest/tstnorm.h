/*
*****************************************************************************************
*                                                                                       *
* COPYRIGHT:                                                                            *
*   (C) Copyright Taligent, Inc.,  1997                                                 *
*   (C) Copyright International Business Machines Corporation,  1997-1998               *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.                  *
*   US Government Users Restricted Rights - Use, duplication, or disclosure             *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                              *
*                                                                                       *
*****************************************************************************************
*/

/**
 * Normalizer basic tests
 */

#ifndef _TSTNORM
#define _TSTNORM

#ifndef _UTYPES
#include "utypes.h"
#endif

#ifndef _COLL
#include "coll.h"
#endif

#ifndef _NORMLZR
#include "normlzr.h"
#endif

#ifndef _INTLTEST
#include "intltest.h"
#endif

class BasicNormalizerTest : public IntlTest {
public:
    BasicNormalizerTest();
    ~BasicNormalizerTest();

    void runIndexedTest( int32_t index, bool_t exec, char* &name, char* par = NULL );

    void TestHangulCompose(void);
    void TestHangulDecomp(void);
    void TestPrevious(void);
    void TestDecomp(void);
    void TestCompatDecomp(void);
    void TestCanonCompose(void);
    void TestCompatCompose(void);


private:
    static UnicodeString canonTests[][3];
    static UnicodeString compatTests[][3];
    static UnicodeString hangulCanon[][3];
    static UnicodeString hangulCompat[][3];


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
