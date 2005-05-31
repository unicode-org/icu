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
#include <unicode/utf8.h>
#include <unicode/ustring.h>
#include "utxttest.h"

UBool  gFailed = FALSE;
#define TEST_ASSERT(x) \
   {if ((x)==FALSE) {errln("Test failure in file %s at line %d\n", __FILE__, __LINE__);\
                     gFailed = TRUE;\
   }}


#define TEST_SUCCESS(status) \
   {if (U_FAILURE(status)) {errln("Test failure in file %s at line %d. Error = \"%s\"\n", \
       __FILE__, __LINE__, u_errorName(status)); \
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
    UErrorCode  status = U_ZERO_ERROR;

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
    cpMap[j].nativeIdx = i;   // position following the last char in utf-16 string.    


    // UChar * test, null term

    // UChar * test, with length

    // const UChar * test, null term


    // const UChar * test, length

    // UnicodeString test
    UText *ut;
    ut = utext_openUnicodeString(NULL, &sa, &status);
    TEST_SUCCESS(status);
    TestAccess(sa, ut, cpCount, cpMap);
    utext_close(ut);

    //
    // UTF-8 test
    //

    // Convert the test string from UnicodeString to (char *) in utf-8 format
    int u8Len = sa.extract(0, sa.length(), NULL, 0, "utf-8");
    char *u8String = new char[u8Len + 1];
    sa.extract(0, sa.length(), u8String, u8Len+1, "utf-8");

    // Build up the map of code point indices in the utf-8 string
    m * u8Map = new m[sa.length() + 1];
    i = 0;   // native utf-8 index
    for (j=0; j<cpCount ; j++) {  // code point number
        u8Map[j].nativeIdx = i;
        U8_NEXT(u8String, i, u8Len, c)
        u8Map[j].cp = c;
    }
    u8Map[cpCount].nativeIdx = u8Len;   // position following the last char in utf-8 string.

    // Do the test itself
    status = U_ZERO_ERROR;
    ut = utext_openUTF8(NULL, (uint8_t *)u8String, -1, &status);
    TEST_SUCCESS(status);
    TestAccess(sa, ut, cpCount, u8Map);
    utext_close(ut);

    // UTF-32 test

    // Code Page test

    // Replaceable test

}


void UTextTest::TestAccess(const UnicodeString &us, UText *ut, int cpCount, m *cpMap) {
    UErrorCode  status = U_ZERO_ERROR;

    //
    //  Check the length from the UText
    //
    int expectedLen = cpMap[cpCount].nativeIdx;
    int utlen = ut->length(ut);
    TEST_ASSERT(expectedLen == utlen);

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
        expectedC     = cpMap[i].cp;
        expectedIndex = cpMap[i].nativeIdx;
        foundC        = uti.previous32();
        foundIndex    = uti.getIndex();
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
    //  next32From(), prevous32From(), Iterate in a somewhat random order.
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
        index         = cpMap[cpIndex+1].nativeIdx;
        expectedC     = cpMap[cpIndex].cp;
        foundC        = uti.previous32From(index);
        TEST_ASSERT(expectedC == foundC);
        TEST_ASSERT(expectedIndex == foundIndex);
        if (gFailed) {
            return;
        }
    }

    //
    // moveIndex(int32_t delta);
    //

    // Walk through frontwards, incrementing by one
    uti.setIndex(0);
    for (i=1; i<=cpCount; i++) {
        uti.moveIndex(1);
        index = uti.getIndex();
        expectedIndex = cpMap[i].nativeIdx;
        TEST_ASSERT(expectedIndex == index);
    }

    // Walk through frontwards, incrementing by two
    uti.setIndex(0);
    for (i=2; i<cpCount; i+=2) {
        uti.moveIndex(2);
        index = uti.getIndex();
        expectedIndex = cpMap[i].nativeIdx;
        TEST_ASSERT(expectedIndex == index);
    }

    // walk through the string backwards, decrementing by one.
    i = cpMap[cpCount].nativeIdx;
    uti.setIndex(i);
    for (i=cpCount; i>=0; i--) {
        expectedIndex = cpMap[i].nativeIdx;
        index = uti.getIndex();
        TEST_ASSERT(expectedIndex == index);
        uti.moveIndex(-1);
    }


    // walk through backwards, decrementing by three
    i = cpMap[cpCount].nativeIdx;
    uti.setIndex(i);
    for (i=cpCount; i>=0; i-=3) {
        expectedIndex = cpMap[i].nativeIdx;
        index = uti.getIndex();
        TEST_ASSERT(expectedIndex == index);
        uti.moveIndex(-3);
    }


    //
    // Extract
    //
    int bufSize = us.length() + 10;
    UChar *buf = new UChar[bufSize];
    status = U_ZERO_ERROR;
    expectedLen = us.length();
    len = ut->extract(ut, 0, utlen, buf, bufSize, &status);
    TEST_SUCCESS(status);
    TEST_ASSERT(len == expectedLen);
    int compareResult = us.compare(buf, -1);
    TEST_ASSERT(compareResult == 0);

    status = U_ZERO_ERROR;
    len = ut->extract(ut, 0, utlen, NULL, 0, &status);
    TEST_ASSERT(status == U_BUFFER_OVERFLOW_ERROR)
    TEST_ASSERT(len == expectedLen);

    status = U_ZERO_ERROR;
    u_memset(buf, 0x5555, bufSize);
    len = ut->extract(ut, 0, utlen, buf, 1, &status);
    if (us.length() == 0) {
        TEST_SUCCESS(status);
        TEST_ASSERT(buf[0] == 0);
    } else {
        TEST_ASSERT(buf[0] == us.charAt(0));
        TEST_ASSERT(buf[1] == 0x5555);
        if (us.length() == 1) {
            TEST_ASSERT(status == U_STRING_NOT_TERMINATED_WARNING);
        } else {
            TEST_ASSERT(status == U_BUFFER_OVERFLOW_ERROR);
        }
    }

    delete buf;

}


