/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 2005, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/************************************************************************
*   Tests for the UText and UTextIterator text abstraction classses
*
************************************************************************/

#include "unicode/utypes.h"

#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <unicode/utext.h>
#include "utxttest.h"

UBool  gFailed = FALSE;
#define TEST_ASSERT(x) \
   {if ((x)==FALSE) {errln("Test failure in file %s at line %d\n", __FILE__, __LINE__);\
                     gFailed = TRUE;\
   }}

UTextTest::UTextTest() {
}

UTextTest::~UTextTest() {
}

void
UTextTest::runIndexedTest(int32_t index, UBool exec,
                                      const char* &name, char* /*par*/) {
    switch (index) {
        case 0: name = "TextTest";
            if(exec) TextTest();                         break;
        default: name = ""; break;
    }
}

void  UTextTest::TextTest() {
    TestString("abcd\\U00010001xyz");
}

//
//  mapping between native indexes and code points.
//     native indexes could be utf-8, utf-16, utf32, or some code page.
//     The general purpose UText test funciton takes an array of these as
//     expected contents of the text being accessed.
//


void UTextTest::TestString(const UnicodeString &s) {
    int         i;
    int         j;
    UChar32     c;
    int         cpCount = 0;

    UnicodeString sa = s.unescape();

    //
    // Build up the mapping between code points and UTF-16 code unit indexes.
    //
    m * cpMap = new m[sa.length() + 1];
    j = 0;
    for (i=0; i<sa.length(); i=sa.moveIndex32(i, 1)) {
        c = sa.char32At(i);
        cpMap[j].nativeIdx = i;
        cpMap[j].cp = c;
        j++;
        cpCount++;
    }


    // UChar * test, null term

    // UChar * test, with length

    // const UChar * test, null term


    // const UChar * test, length

    // UnicodeString test
    UText ut;
    utext_setUnicodeString(&ut, &sa);
    TestAccess(&ut, cpCount, cpMap);

    // UTF-8 test

    // UTF-32 test

    // Code Page test

    // Replaceable test

}


void UTextTest::TestAccess(UText *ut, int cpCount, m *cpMap) {
    //
    //  Iterate forwards, verify that we get the correct code points
    //   at the correct native offsets.
    //
    UTextIterator it(ut);
    int         i = 0;
    int         index;
    int         expectedIndex;
    int         foundIndex;
    UChar32     expectedC;
    UChar32     foundC;
    int32_t     len;

    UTextIterator uti(ut);
    for (i=0; i<cpCount; i++) {
        expectedIndex = cpMap[i].nativeIdx;
        foundIndex    = uti.getIndex();
        TEST_ASSERT(expectedIndex == foundIndex);
        expectedC     = cpMap[i].cp;
        foundC        = uti.next32();
        TEST_ASSERT(expectedC == foundC);
        if (gFailed) {
            return;
        }
    }
    foundC = uti.next32();
    TEST_ASSERT(foundC == U_SENTINEL);
    
    //
    //  Forward iteration (above) should have left index at the
    //   end of the input, which should == length().
    //
    len = uti.length();
    foundIndex  = uti.getIndex();
    TEST_ASSERT(len == foundIndex);

    //
    // Iterate backwards over entire test string
    //
    len = uti.getIndex();
    uti.setIndex(len);
    for (i=cpCount-1; i>=0; i--) {
        foundC        = uti.previous32();
        expectedC     = cpMap[i].cp;
        foundIndex    = uti.getIndex();
        expectedIndex = cpMap[i].nativeIdx;
        TEST_ASSERT(expectedIndex == foundIndex);
        TEST_ASSERT(expectedC == foundC);
        if (gFailed) {
            return;
        }
    }

    //
    //  Backwards iteration, above, should have left our iterator
    //   position at zero, and continued backwards iterationshould fail.
    //
    foundIndex = uti.getIndex();
    TEST_ASSERT(foundIndex == 0);

    foundC = uti.previous32();
    TEST_ASSERT(foundC == U_SENTINEL);
    foundIndex = uti.getIndex();
    TEST_ASSERT(foundIndex == 0);
    if (gFailed) {
        return;
    }

    //
    //  Iterate in a somewhat random order.
    //
    int  cpIndex = 0;
    for (i=0; i<cpCount; i++) {
        cpIndex = (cpIndex + 9973) % cpCount;
        index         = cpMap[cpIndex].nativeIdx;
        expectedC     = cpMap[cpIndex].cp;
        foundC        = uti.next32From(index);
        TEST_ASSERT(expectedC == foundC);
        TEST_ASSERT(expectedIndex == foundIndex);
        if (gFailed) {
            return;
        }
    }

    cpIndex = 0;
    for (i=0; i<cpCount; i++) {
        cpIndex = (cpIndex + 9973) % cpCount;
        index         = cpMap[cpIndex].nativeIdx;
        expectedC     = cpMap[cpIndex].cp;
        foundC        = uti.previous32From(index+1);
        TEST_ASSERT(expectedC == foundC);
        TEST_ASSERT(expectedIndex == foundIndex);
        if (gFailed) {
            return;
        }
    }

    //
    // moveIndex(int32_t delta);
    //
    uti.setIndex(0);
    for (i=2; i<cpCount; i+=2) {
        uti.moveIndex(2);
        index = uti.getIndex();
        expectedIndex = cpMap[i].nativeIdx;
        TEST_ASSERT(expectedIndex == index);
    }

    i = cpMap[cpCount-1].nativeIdx;
    uti.setIndex(i);
    for (i=cpCount-1; i>=0; i-=3) {
        index = uti.getIndex();
        expectedIndex = cpMap[i].nativeIdx;
        TEST_ASSERT(expectedIndex == index);
        uti.moveIndex(-3);
    }




}


