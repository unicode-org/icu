/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 2005, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/************************************************************************
*   Tests for the UText and UTextIterator text abstraction classses
*
************************************************************************/


#ifndef UTXTTEST_H
#define UTXTTEST_H

#include "unicode/utypes.h"
#include "unicode/unistr.h"
#include "unicode/utext.h"

#include "intltest.h"

/**
 * @test
 * @summary Testing the Replaceable class
 */
class UTextTest : public IntlTest {
public:
    UTextTest();
    virtual ~UTextTest();

    void runIndexedTest(int32_t index, UBool exec, const char* &name, char* par=NULL);
    void TextTest();

private:
    struct m {                              // Map between native indices & code points.
        int         nativeIdx;
        UChar32     cp;
    };

    void TestString(const UnicodeString &s);
    void TestAccess(UText *ut, int cpCount, m *cpMap);
};


#endif
