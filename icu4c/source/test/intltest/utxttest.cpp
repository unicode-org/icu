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

static UBool  gFailed = FALSE;
static int    gTestNum = 0;


#define TEST_ASSERT(x) \
{if ((x)==FALSE) {errln("Test #%d failure in file %s at line %d\n", gTestNum, __FILE__, __LINE__);\
                     gFailed = TRUE;\
   }}


#define TEST_SUCCESS(status) \
{if (U_FAILURE(status)) {errln("Test #%d failure in file %s at line %d. Error = \"%s\"\n", \
       gTestNum, __FILE__, __LINE__, u_errorName(status)); \
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

//
// Quick and dirty random number generator.
//   (don't use library so that results are portable.
static uint32_t m_seed = 1;
static uint32_t m_rand()
{
    m_seed = m_seed * 1103515245 + 12345;
    return (uint32_t)(m_seed/65536) % 32768;
}


void  UTextTest::TextTest() {
    int32_t i, j;

    TestString("abcd\\U00010001xyz");

    // Test simple strings of lengths 1 to 60, looking for glitches at buffer boundaries
    UnicodeString s;
    for (i=1; i<60; i++) {
        s.truncate(0);
        for (j=0; j<i; j++) {
            if (j+0x30 == 0x5c) {
                // backslash.  Needs to be escaped
                s.append((UChar)0x5c);
            }
            s.append(UChar(j+0x30));
        }
        TestString(s);
    }

   // Test strings with odd-aligned supplementary chars,
   //    looking for glitches at buffer boundaries
    for (i=1; i<60; i++) {
        s.truncate(0);
        s.append((UChar)0x41);
        for (j=0; j<i; j++) {
            s.append(UChar32(j+0x11000));
        }
        TestString(s);
    }

    // String of chars of randomly varying size in utf-8 representation.
    //   Exercise the mapping, and the varying sized buffer.
    //
    s.truncate(0);
    UChar32  c1 = 0;
    UChar32  c2 = 0x100;
    UChar32  c3 = 0xa000;
    UChar32  c4 = 0x11000;
    for (i=0; i<1000; i++) {
        int len8 = m_rand()%4 + 1;
        switch (len8) {
            case 1: 
                c1 = (c1+1)%0x80;
                // don't put 0 into string (0 terminated strings for some tests)
                // don't put '\', will cause unescape() to fail.
                if (c1==0x5c || c1==0) {
                    c1++;
                }
                s.append(c1);
                break;
            case 2:
                s.append(c2++);
                break;
            case 3:
                s.append(c3++);
                break;
            case 4:
                s.append(c4++);
                break;
        }
    }
    TestString(s);
}

//
//  mapping between native indexes and code points.
//     native indexes could be utf-8, utf-16, utf32, or some code page.
//     The general purpose UText test funciton takes an array of these as
//     expected contents of the text being accessed.
//


void UTextTest::TestString(const UnicodeString &s) {
    int32_t       i;
    int32_t       j;
    UChar32       c;
    int32_t       cpCount = 0;
    UErrorCode    status  = U_ZERO_ERROR;
    UText        *ut      = NULL;
    int32_t       saLen;

    UnicodeString sa = s.unescape();
    saLen = sa.length();

    //
    // Build up the mapping between code points and UTF-16 code unit indexes.
    //
    m *cpMap = new m[sa.length() + 1];
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
    status = U_ZERO_ERROR;
    UChar *buf = new UChar[saLen+1];
    sa.extract(buf, saLen+1, status);
    TEST_SUCCESS(status);
    ut = utext_openUChars(NULL, buf, -1, &status);
    TEST_SUCCESS(status);
    TestAccess(sa, ut, cpCount, cpMap);
    utext_close(ut);
    delete [] buf;

    // UChar * test, with length
    status = U_ZERO_ERROR;
    buf = new UChar[saLen+1];
    sa.extract(buf, saLen+1, status);
    TEST_SUCCESS(status);
    ut = utext_openUChars(NULL, buf, saLen, &status);
    TEST_SUCCESS(status);
    TestAccess(sa, ut, cpCount, cpMap);
    utext_close(ut);
    delete [] buf;


    // UnicodeString test
    status = U_ZERO_ERROR;
    ut = utext_openUnicodeString(NULL, &sa, &status);
    TEST_SUCCESS(status);
    TestAccess(sa, ut, cpCount, cpMap);
    TestCMR(sa, ut, cpCount, cpMap, cpMap);
    utext_close(ut);


    // Const UnicodeString test
    status = U_ZERO_ERROR;
    ut = utext_openConstUnicodeString(NULL, &sa, &status);
    TEST_SUCCESS(status);
    TestAccess(sa, ut, cpCount, cpMap);
    utext_close(ut);


    // Replaceable test.  (UnicodeString inherits Replaceable)
    status = U_ZERO_ERROR;
    ut = utext_openReplaceable(NULL, &sa, &status);
    TEST_SUCCESS(status);
    TestAccess(sa, ut, cpCount, cpMap);
    TestCMR(sa, ut, cpCount, cpMap, cpMap);
    utext_close(ut);


    //
    // UTF-8 test
    //

    // Convert the test string from UnicodeString to (char *) in utf-8 format
    int32_t u8Len = sa.extract(0, sa.length(), NULL, 0, "utf-8");
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
    ut = utext_openUTF8(NULL, u8String, -1, &status);
    TEST_SUCCESS(status);
    TestAccess(sa, ut, cpCount, u8Map);
    utext_close(ut);



	delete []cpMap;
	delete []u8Map;
	delete []u8String;
}

//  TestCMR   test Copy, Move and Replace operations.
//              us         UnicodeString containing the test text.
//              ut         UText containing the same test text.
//              cpCount    number of code points in the test text.
//              nativeMap  Mapping from code points to native indexes for the UText.
//              u16Map     Mapping from code points to UTF-16 indexes, for use with teh UnicodeString.
//
//     This function runs a whole series of opertions on each incoming UText.
//     The UText is deep-cloned prior to each operation, so that the original UText remains unchanged.
//     
void UTextTest::TestCMR(const UnicodeString &us, UText *ut, int cpCount, m *nativeMap, m *u16Map) {
    TEST_ASSERT(utext_isWritable(ut) == TRUE);

    int  srcLengthType;       // Loop variables for selecting the postion and length
    int  srcPosType;          //   of the block to operate on within the source text.
    int  destPosType; 

    int  srcIndex  = 0;       // Code Point indexes of the block to operate on for
    int  srcLength = 0;       //   a specific test.

    int  destIndex = 0;       // Code point index of the destination for a copy/move test.

    int32_t  nativeStart = 0; // Native unit indexes for a test.
    int32_t  nativeLimit = 0;
    int32_t  nativeDest  = 0;

    int32_t  u16Start    = 0; // UTF-16 indexes for a test.
    int32_t  u16Limit    = 0; //   used when performing the same operation in a Unicode String
    int32_t  u16Dest     = 0;

    // Iterate over a whole series of source index, length and a target indexes.
    // This is done with code point indexes; these will be later translated to native
    //   indexes using the cpMap.
    for (srcLengthType=1; srcLengthType<=3; srcLengthType++) {
        switch (srcLengthType) {
            case 1: srcLength = 1; break;
            case 2: srcLength = 5; break;
            case 3: srcLength = cpCount / 3;
        }
        for (srcPosType=1; srcPosType<=5; srcPosType++) {
            switch (srcPosType) {
                case 1: srcIndex = 0; break;
                case 2: srcIndex = 1; break;
                case 3: srcIndex = cpCount - srcLength; break;
                case 4: srcIndex = cpCount - srcLength - 1; break;
                case 5: srcIndex = cpCount / 2; break;
            }
            if (srcIndex < 0 || srcIndex + srcLength > cpCount) {
                // filter out bogus test cases - 
                //   those with a source range that falls of an edge of the string.
                continue;
            }

            //
            // Copy and move tests.
            //   iterate over a variety of destination positions.
            //
            for (destPosType=1; destPosType<=4; destPosType++) {
                switch (destPosType) {
                    case 1: destIndex = 0; break;
                    case 2: destIndex = 1; break;
                    case 3: destIndex = srcIndex - 1; break;
                    case 4: destIndex = srcIndex + srcLength + 1; break;
                    case 5: destIndex = cpCount-1; break;
                    case 6: destIndex = cpCount; break;
                }
                if (destIndex<0 || destIndex>cpCount) {
                    // filter out bogus test cases.
                    continue;
                }

                nativeStart = nativeMap[srcIndex].nativeIdx;
                nativeLimit = nativeMap[srcIndex+srcLength].nativeIdx;
                nativeDest  = nativeMap[destIndex].nativeIdx;

                u16Start    = u16Map[srcIndex].nativeIdx;
                u16Limit    = u16Map[srcIndex+srcLength].nativeIdx;
                u16Dest     = u16Map[destIndex].nativeIdx;

                gFailed = FALSE;
                TestCopyMove(us, ut, FALSE,
                    nativeStart, nativeLimit, nativeDest,
                    u16Start, u16Limit, u16Dest);

                TestCopyMove(us, ut, TRUE,
                    nativeStart, nativeLimit, nativeDest,
                    u16Start, u16Limit, u16Dest);

                if (gFailed) {
                    return;
                }
            }

            //
            //  Replace tests.
            //
            UnicodeString fullRepString("This is an arbitrary string that will be used as replacement text");
            for (int32_t replStrLen=0; replStrLen<20; replStrLen++) {
                UnicodeString repStr(fullRepString, 0, replStrLen);
                TestReplace(us, ut,
                    nativeStart, nativeLimit,
                    u16Start, u16Limit,
                    repStr);
                if (gFailed) {
                    return;
                }
            }

        }
    }

}

//
//   TestCopyMove    run a single test case for utext_copy.
//                   Test cases are created in TestCMR and dispatched here for execution.
//
void UTextTest::TestCopyMove(const UnicodeString &us, UText *ut, UBool move,
                    int32_t nativeStart, int32_t nativeLimit, int32_t nativeDest,
                    int32_t u16Start, int32_t u16Limit, int32_t u16Dest) 
{
    UErrorCode      status   = U_ZERO_ERROR;
    UText          *targetUT = NULL;
    gTestNum++;
    gFailed = FALSE;

    //
    //  clone the UText.  The test will be run in the cloned copy
    //  so that we don't alter the original.
    //
    targetUT = utext_clone(NULL, ut, TRUE, &status);
    TEST_SUCCESS(status);
    UnicodeString targetUS(us);    // And copy the reference string.

    // do the test operation first in the reference
    targetUS.copy(u16Start, u16Limit, u16Dest);
    if (move) {
        // delete out the source range.
        if (u16Limit < u16Dest) {
            targetUS.removeBetween(u16Start, u16Limit);
        } else {
            int32_t amtCopied = u16Limit - u16Start;
            targetUS.removeBetween(u16Start+amtCopied, u16Limit+amtCopied);
        }
    }

    // Do the same operation in the UText under test
    utext_copy(targetUT, nativeStart, nativeLimit, nativeDest, move, &status);
    if (nativeDest > nativeStart && nativeDest < nativeLimit) {
        TEST_ASSERT(status == U_INDEX_OUTOFBOUNDS_ERROR);
    } else {
        TEST_SUCCESS(status);

        // Compare the results of the two parallel tests
        int32_t  usi = 0;    // UnicodeString postion, utf-16 index.
        int32_t  uti = 0;    // UText position, native index.
        int32_t  cpi;        // char32 position (code point index) 
        UChar32  usc;        // code point from Unicode String
        UChar32  utc;        // code point from UText
        utext_setNativeIndex(targetUT, 0);
        for (cpi=0; ; cpi++) {
            usc = targetUS.char32At(usi);
            utc = utext_next32(targetUT);
            if (utc < 0) {
                break;
            }
            TEST_ASSERT(uti == usi);
            TEST_ASSERT(utc == usc);
            usi = targetUS.moveIndex32(usi, 1);
            uti = utext_getNativeIndex(targetUT);
            if (gFailed) {
                goto cleanupAndReturn;
            }
        }
        int32_t expectedNativeLength = utext_nativeLength(ut);
        if (move == FALSE) {
            expectedNativeLength += nativeLimit - nativeStart;
        }
        uti = utext_getNativeIndex(targetUT);
        TEST_ASSERT(uti == expectedNativeLength);
    }

cleanupAndReturn:
    utext_close(targetUT);
}
    

//
//  TestReplace   Test a single Replace operation.
//
void UTextTest::TestReplace(
            const UnicodeString &us,     // reference UnicodeString in which to do the replace 
            UText         *ut,                // UnicodeText object under test.
            int32_t       nativeStart,        // Range to be replaced, in UText native units. 
            int32_t       nativeLimit,
            int32_t       u16Start,           // Range to be replaced, in UTF-16 units
            int32_t       u16Limit,           //    for use in the reference UnicodeString.
            const UnicodeString &repStr)      // The replacement string
{
    UErrorCode      status   = U_ZERO_ERROR;
    UText          *targetUT = NULL;
    gTestNum++;
    gFailed = FALSE;

    //
    //  clone the target UText.  The test will be run in the cloned copy
    //  so that we don't alter the original.
    //
    targetUT = utext_clone(NULL, ut, TRUE, &status);
    TEST_SUCCESS(status);
    UnicodeString targetUS(us);    // And copy the reference string.

    //
    // Do the replace operation in the Unicode String, to 
    //   produce a reference result.
    //
    targetUS.replace(u16Start, u16Limit-u16Start, repStr);

    //
    // Do the replace on the UText under test
    //
    const UChar *rs = repStr.getBuffer();
    int32_t  rsLen = repStr.length();
    int32_t actualDelta = utext_replace(targetUT, nativeStart, nativeLimit, rs, rsLen, &status);
    int32_t expectedDelta = repStr.length() - (nativeLimit - nativeStart);
    TEST_ASSERT(actualDelta == expectedDelta);

    //
    // Compare the results
    //
    int32_t  usi = 0;    // UnicodeString postion, utf-16 index.
    int32_t  uti = 0;    // UText position, native index.
    int32_t  cpi;        // char32 position (code point index) 
    UChar32  usc;        // code point from Unicode String
    UChar32  utc;        // code point from UText
    int32_t  expectedNativeLength = 0;
    utext_setNativeIndex(targetUT, 0);
    for (cpi=0; ; cpi++) {
        usc = targetUS.char32At(usi);
        utc = utext_next32(targetUT);
        if (utc < 0) {
            break;
        }
        TEST_ASSERT(uti == usi);
        TEST_ASSERT(utc == usc);
        usi = targetUS.moveIndex32(usi, 1);
        uti = utext_getNativeIndex(targetUT);
        if (gFailed) {
            goto cleanupAndReturn;
        }
    }
    expectedNativeLength = utext_nativeLength(ut) + expectedDelta;
    uti = utext_getNativeIndex(targetUT);
    TEST_ASSERT(uti == expectedNativeLength);

cleanupAndReturn:
    utext_close(targetUT);
}

void UTextTest::TestAccess(const UnicodeString &us, UText *ut, int cpCount, m *cpMap) {
    UErrorCode  status = U_ZERO_ERROR;
    gTestNum++;

    //
    //  Check the length from the UText
    //
    int expectedLen = cpMap[cpCount].nativeIdx;
    int utlen = ut->nativeLength(ut);
    TEST_ASSERT(expectedLen == utlen);

    //
    //  Iterate forwards, verify that we get the correct code points
    //   at the correct native offsets.
    //
    int         i = 0;
    int         index;
    int         expectedIndex = 0;
    int         foundIndex = 0;
    UChar32     expectedC;
    UChar32     foundC;
    int32_t     len;

    for (i=0; i<cpCount; i++) {
        expectedIndex = cpMap[i].nativeIdx;
        foundIndex    = utext_getNativeIndex(ut);
        TEST_ASSERT(expectedIndex == foundIndex);
        expectedC     = cpMap[i].cp;
        foundC        = utext_next32(ut);    
        TEST_ASSERT(expectedC == foundC);
        if (gFailed) {
            return;
        }
    }
    foundC = utext_next32(ut);
    TEST_ASSERT(foundC == U_SENTINEL);
    
    // Repeat above, using macros
    utext_setNativeIndex(ut, 0);
    for (i=0; i<cpCount; i++) {
        expectedIndex = cpMap[i].nativeIdx;
        foundIndex    = utext_getNativeIndex(ut);
        TEST_ASSERT(expectedIndex == foundIndex);
        expectedC     = cpMap[i].cp;
        foundC        = UTEXT_NEXT32(ut);    
        TEST_ASSERT(expectedC == foundC);
        if (gFailed) {
            return;
        }
    }
    foundC = utext_next32(ut);
    TEST_ASSERT(foundC == U_SENTINEL);

    //
    //  Forward iteration (above) should have left index at the
    //   end of the input, which should == length().
    //
    len = utext_nativeLength(ut);
    foundIndex  = utext_getNativeIndex(ut);
    TEST_ASSERT(len == foundIndex);

    //
    // Iterate backwards over entire test string
    //
    len = utext_getNativeIndex(ut);
    utext_setNativeIndex(ut, len);
    for (i=cpCount-1; i>=0; i--) {
        expectedC     = cpMap[i].cp;
        expectedIndex = cpMap[i].nativeIdx;
        foundC        = utext_previous32(ut);
        foundIndex    = utext_getNativeIndex(ut);
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
    foundIndex = utext_getNativeIndex(ut);
    TEST_ASSERT(foundIndex == 0);

    foundC = utext_previous32(ut);
    TEST_ASSERT(foundC == U_SENTINEL);
    foundIndex = utext_getNativeIndex(ut);
    TEST_ASSERT(foundIndex == 0);


    // And again, with the macros
    utext_setNativeIndex(ut, len);
    for (i=cpCount-1; i>=0; i--) {
        expectedC     = cpMap[i].cp;
        expectedIndex = cpMap[i].nativeIdx;
        foundC        = UTEXT_PREVIOUS32(ut);
        foundIndex    = utext_getNativeIndex(ut);
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
    foundIndex = utext_getNativeIndex(ut);
    TEST_ASSERT(foundIndex == 0);

    foundC = utext_previous32(ut);
    TEST_ASSERT(foundC == U_SENTINEL);
    foundIndex = utext_getNativeIndex(ut);
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
        foundC        = utext_next32From(ut, index);
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
        foundC        = utext_previous32From(ut, index);
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
    utext_setNativeIndex(ut, 0);
    for (i=1; i<=cpCount; i++) {
        utext_moveIndex32(ut, 1);
        index = utext_getNativeIndex(ut);
        expectedIndex = cpMap[i].nativeIdx;
        TEST_ASSERT(expectedIndex == index);
    }

    // Walk through frontwards, incrementing by two
    utext_setNativeIndex(ut, 0);
    for (i=2; i<cpCount; i+=2) {
        utext_moveIndex32(ut, 2);
        index = utext_getNativeIndex(ut);
        expectedIndex = cpMap[i].nativeIdx;
        TEST_ASSERT(expectedIndex == index);
    }

    // walk through the string backwards, decrementing by one.
    i = cpMap[cpCount].nativeIdx;
    utext_setNativeIndex(ut, i);
    for (i=cpCount; i>=0; i--) {
        expectedIndex = cpMap[i].nativeIdx;
        index = utext_getNativeIndex(ut);
        TEST_ASSERT(expectedIndex == index);
        utext_moveIndex32(ut, -1);
    }


    // walk through backwards, decrementing by three
    i = cpMap[cpCount].nativeIdx;
    utext_setNativeIndex(ut, i);
    for (i=cpCount; i>=0; i-=3) {
        expectedIndex = cpMap[i].nativeIdx;
        index = utext_getNativeIndex(ut);
        TEST_ASSERT(expectedIndex == index);
        utext_moveIndex32(ut, -3);
    }


    //
    // Extract
    //
    int bufSize = us.length() + 10;
    UChar *buf = new UChar[bufSize];
    status = U_ZERO_ERROR;
    expectedLen = us.length();
    len = utext_extract(ut, 0, utlen, buf, bufSize, &status);
    TEST_SUCCESS(status);
    TEST_ASSERT(len == expectedLen);
    int compareResult = us.compare(buf, -1);
    TEST_ASSERT(compareResult == 0);

    status = U_ZERO_ERROR;
    len = utext_extract(ut, 0, utlen, NULL, 0, &status);
    TEST_ASSERT(status == U_BUFFER_OVERFLOW_ERROR)
    TEST_ASSERT(len == expectedLen);

    status = U_ZERO_ERROR;
    u_memset(buf, 0x5555, bufSize);
    len = utext_extract(ut, 0, utlen, buf, 1, &status);
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


