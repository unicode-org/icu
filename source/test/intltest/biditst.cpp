/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-2001, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/*   file name:  cbididat.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 1999sep22
*   created by: Markus W. Scherer
*/

#include "biditst.h"
#include "cmemory.h"    

static const char *dirPropNames[dirPropCount]={
    "L", "R", "EN", "ES", "ET", "AN", "CS", "B", "S", "WS", "ON",
    "LRE", "LRO", "AL", "RLE", "RLO", "PDF", "NSM", "BN"
};

static const UChar
charFromDirProp[dirPropCount]={
 /* L     R      EN    ES    ET    AN     CS    B    S    WS    ON */
    0x61, 0x5d0, 0x30, 0x2f, 0x25, 0x660, 0x2c, 0xa, 0x9, 0x20, 0x26,
 /* LRE     LRO     AL     RLE     RLO     PDF     NSM    BN */
    0x202a, 0x202d, 0x627, 0x202b, 0x202e, 0x202c, 0x308, 0x200c
};

static const uint8_t
testText1[]={
    L, L, WS, L, WS, EN, L, B
};

static const UBiDiLevel
testLevels1[]={
    0, 0, 0, 0, 0, 0, 0, 0
};

static const uint8_t
testVisualMap1[]={
    0, 1, 2, 3, 4, 5, 6, 7
};

static const uint8_t
testText2[]={
    R, AL, WS, R, AL, WS, R
};

static const UBiDiLevel
testLevels2[]={
    1, 1, 1, 1, 1, 1, 1
};

static const uint8_t
testVisualMap2[]={
    6, 5, 4, 3, 2, 1, 0
};

static const uint8_t
testText3[]={
    L, L, WS, EN, CS, WS, EN, CS, EN, WS, L, L
};

static const UBiDiLevel
testLevels3[]={
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
};

static const uint8_t
testVisualMap3[]={
    0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11
};

static const uint8_t
testText4[]={
    L, AL, AL, AL, L, AL, AL, L, WS, EN, CS, WS, EN, CS, EN, WS, L, L
};

static const UBiDiLevel
testLevels4[]={
    0, 1, 1, 1, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
};

static const uint8_t
testVisualMap4[]={
    0, 3, 2, 1, 4, 6, 5, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17
};

static const uint8_t
testText5[]={
    AL, R, AL, WS, EN, CS, WS, EN, CS, EN, WS, R, R, WS, L, L
};

static const UBiDiLevel
testLevels5[]={
    1, 1, 1, 1, 2, 1, 1, 2, 2, 2, 1, 1, 1, 1, 2, 2
};

static const uint8_t
testVisualMap5[]={
    15, 14, 13, 12, 11, 10, 9, 6, 7, 8, 5, 4, 3, 2, 0, 1
};

static const uint8_t
testText6[]={
    R, EN, NSM, ET
};

static const UBiDiLevel
testLevels6[]={
    1, 2, 2, 2
};

static const uint8_t
testVisualMap6[]={
    3, 0, 1, 2
};

#if 0
static const uint8_t
testText7[]={
    /* empty */
};

static const UBiDiLevel
testLevels7[]={
};

static const uint8_t
testVisualMap7[]={
};

#endif

static const uint8_t
testText8[]={
    RLE, WS, R, R, R, WS, PDF, WS, B
};

static const UBiDiLevel
testLevels8[]={
    1, 1, 1, 1, 1, 1, 1, 1, 1
};

static const uint8_t
testVisualMap8[]={
    8, 7, 6, 5, 4, 3, 2, 1, 0
};

static const uint8_t
testText9[]={
    LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE,
    LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE,
    AN, RLO, NSM, LRE, PDF, RLE, ES, EN, ON
};

static const UBiDiLevel
testLevels9[]={
    62, 62, 62, 62, 62, 62, 62, 62, 62, 62, 62, 62, 62, 62, 62, 62, 62, 62, 62, 62, 62, 62, 62, 62, 62, 62, 62, 62, 62, 62, 62, 61, 61, 61, 61, 61, 61, 61, 61
};

static const uint8_t
testVisualMap9[]={
    8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 7, 6, 5, 4, 3, 2, 1, 0
};

static const uint8_t
testText10[]={
    LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE,
    LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE,
    LRE, BN, CS, RLO, S, PDF, EN, LRO, AN, ES
};

static const UBiDiLevel
testLevels10[]={
    60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 0, 0, 62, 62, 62, 62, 60
};

static const uint8_t
testVisualMap10[]={
    0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39
};

static const uint8_t
testText11[]={
    S, WS, NSM, RLE, WS, L, L, L, WS, LRO, WS, R, R, R, WS, RLO, WS, L, L,
    L, WS, LRE, WS, R, R, R, WS, PDF, WS, L, L, L, WS, PDF, WS, 
    AL, AL, AL, WS, PDF, WS, L, L, L, WS, PDF, WS, L, L, L, WS, PDF, 
    ON, PDF, BN, BN, ON, PDF
};

static const UBiDiLevel
testLevels11[]={
    0, 0, 0, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 4, 4, 5, 5, 5, 4, 3, 3, 3, 3, 3, 3, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
};

static const uint8_t
testVisualMap11[]={
    0, 1, 2, 44, 43, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 31, 30, 29, 28, 27, 26, 20, 21, 24, 23, 22, 25, 19, 18, 17, 16, 15, 14, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 3, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57
};

static const uint8_t
testText12[]={
    NSM, WS, L, L, L, L, L, L, L, WS, L, L, L, L, WS, 
    R, R, R, R, R, WS, L, L, L, L, L, L, L, WS, WS, AL, 
    AL, AL, AL, WS, EN, EN, ES, EN, EN, CS, S, EN, EN, CS, WS, 
    EN, EN, WS, AL, AL, AL, AL, AL, B, L, L, L, L, L, L, 
    L, L, WS, AN, AN, CS, AN, AN, WS
};

static const UBiDiLevel
testLevels12[]={
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 2, 2, 1, 2, 2, 1, 0, 2, 2, 1, 1, 2, 2, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 2, 2, 2, 2, 0
};

static const uint8_t
testVisualMap12[]={
    0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 19, 18, 17, 16, 15, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 40, 39, 38, 37, 36, 34, 35, 33, 31, 32, 30, 41, 52, 53, 51, 50, 48, 49, 47, 46, 45, 44, 43, 42, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69
};

static const UBiDiLevel
testLevels13[]={
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 2, 2, 1, 2, 2, 1, 0, 2, 2, 1, 1, 2, 2, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 2, 2, 2, 2, 0
};

static const uint8_t
testVisualMap13[]={
    0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 19, 18, 17, 16, 15, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 40, 39, 38, 37, 36, 34, 35, 33, 31, 32, 30, 41, 52, 53, 51, 50, 48, 49, 47, 46, 45, 44, 43, 42, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69
};

static const UBiDiLevel
testLevels14[]={
    2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 4, 4, 3, 4, 4, 3, 2, 4, 4, 3, 3, 4, 4, 3, 3, 3, 3, 3, 3, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 4, 4, 4, 4, 4, 2
};

static const uint8_t
testVisualMap14[]={
    0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 19, 18, 17, 16, 15, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 40, 39, 38, 37, 36, 34, 35, 33, 31, 32, 30, 41, 52, 53, 51, 50, 48, 49, 47, 46, 45, 44, 43, 42, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69
};

static const UBiDiLevel
testLevels15[]={
    5, 5, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 5, 5, 5, 5, 5, 5, 5, 6, 6, 6, 6, 6, 6, 6, 5, 5, 5, 5, 5, 5, 5, 6, 6, 5, 6, 6, 5, 5, 6, 6, 5, 5, 6, 6, 5, 5, 5, 5, 5, 5, 5, 6, 6, 6, 6, 6, 6, 6, 6, 5, 6, 6, 6, 6, 6, 5
};

static const uint8_t
testVisualMap15[]={
    69, 68, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 55, 54, 53, 52, 51, 50, 49, 42, 43, 44, 45, 46, 47, 48, 41, 40, 39, 38, 37, 36, 35, 33, 34, 32, 30, 31, 29, 28, 26, 27, 25, 24, 22, 23, 21, 20, 19, 18, 17, 16, 15, 7, 8, 9, 10, 11, 12, 13, 14, 6, 1, 2, 3, 4, 5, 0
};

static const UBiDiLevel
testLevels16[]={
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 2, 2, 1, 2, 2, 1, 0, 2, 2, 1, 1, 2, 2, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 2, 2, 2, 2, 0
};

static const uint8_t
testVisualMap16[]={
    0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 19, 18, 17, 16, 15, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 40, 39, 38, 37, 36, 34, 35, 33, 31, 32, 30, 41, 52, 53, 51, 50, 48, 49, 47, 46, 45, 44, 43, 42, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69
};

static const uint8_t
testText13[]={
    ON, L, RLO, CS, R, WS, AN, AN, PDF, LRE, R, L, LRO, WS, BN, ON, S, LRE, LRO, B
};

static const UBiDiLevel
testLevels17[]={
    0, 0, 1, 1, 1, 1, 1, 1, 3, 3, 3, 2, 4, 4, 4, 4, 0, 0, 0, 0
};

static const uint8_t
testVisualMap17[]={
    0, 1, 15, 14, 13, 12, 11, 10, 4, 3, 2, 5, 6, 7, 8, 9, 16, 17, 18, 19
};

static const UBiDiLevel
testLevels18[]={
    0, 0, 1, 1, 1, 0
};

static const uint8_t
testVisualMap18[]={
    0, 1, 4, 3, 2, 5
};

static const uint8_t
testText14[]={
    RLO, RLO, AL, AL, WS, EN, ES, ON, WS, S, S, PDF, LRO, WS, AL, ET, RLE, ON, EN, B
};

static const UBiDiLevel
testLevels19[]={
    1
};

static const uint8_t
testVisualMap19[]={
    0
};

static const uint8_t
testText15[]={
    R, L, CS, L
};

static const UBiDiLevel
testLevels20[]={
    2
};

BiDiTestData
tests[]={
    {testText1,  ARRAY_LENGTH(testText1),  UBIDI_DEFAULT_LTR, -1, -1,
        UBIDI_LTR, 0,
        testLevels1, testVisualMap1},
    {testText2,  ARRAY_LENGTH(testText2),  UBIDI_DEFAULT_LTR, -1, -1,
        UBIDI_RTL, 1,
        testLevels2, testVisualMap2},
    {testText3,  ARRAY_LENGTH(testText3),  UBIDI_DEFAULT_LTR, -1, -1,
        UBIDI_LTR, 0,
        testLevels3, testVisualMap3},
    {testText4,  ARRAY_LENGTH(testText4),  UBIDI_DEFAULT_LTR, -1, -1,
        UBIDI_MIXED, 0,
        testLevels4, testVisualMap4},
    {testText5,  ARRAY_LENGTH(testText5),  UBIDI_DEFAULT_LTR, -1, -1,
        UBIDI_MIXED, 1,
        testLevels5, testVisualMap5},
    {testText6,  ARRAY_LENGTH(testText6),  UBIDI_DEFAULT_LTR, -1, -1,
        UBIDI_MIXED, 1,
        testLevels6, testVisualMap6},
    {NULL,       0,                        UBIDI_DEFAULT_LTR, -1, -1,
        UBIDI_LTR, 0,
        NULL, NULL},
    {testText8,  ARRAY_LENGTH(testText8),  UBIDI_DEFAULT_LTR, -1, -1,
        UBIDI_RTL, 1,
        testLevels8, testVisualMap8},
    {testText9,  ARRAY_LENGTH(testText9),  UBIDI_DEFAULT_LTR, -1, -1,
        UBIDI_MIXED, 0,
        testLevels9, testVisualMap9},
    {testText10, ARRAY_LENGTH(testText10), UBIDI_DEFAULT_LTR, -1, -1,
        UBIDI_MIXED, 0,
        testLevels10, testVisualMap10},
    {testText11, ARRAY_LENGTH(testText11), UBIDI_DEFAULT_LTR, -1, -1,
        UBIDI_MIXED, 0,
        testLevels11, testVisualMap11},
    {testText12, ARRAY_LENGTH(testText12), UBIDI_DEFAULT_LTR, -1, -1,
        UBIDI_MIXED, 0,
        testLevels12, testVisualMap12},
    {testText12, ARRAY_LENGTH(testText12), UBIDI_DEFAULT_RTL, -1, -1,
        UBIDI_MIXED, 0,
        testLevels13, testVisualMap13},
    {testText12, ARRAY_LENGTH(testText12), 2, -1, -1,
        UBIDI_MIXED, 2,
        testLevels14, testVisualMap14},
    {testText12, ARRAY_LENGTH(testText12), 5, -1, -1,
        UBIDI_MIXED, 5,
        testLevels15, testVisualMap15},
    {testText12, ARRAY_LENGTH(testText12), UBIDI_DEFAULT_LTR, -1, -1,
        UBIDI_MIXED, 0,
        testLevels16, testVisualMap16},
    {testText13, ARRAY_LENGTH(testText13), UBIDI_DEFAULT_LTR, -1, -1,
        UBIDI_MIXED, 0,
        testLevels17, testVisualMap17},
    {testText13, ARRAY_LENGTH(testText13), UBIDI_DEFAULT_LTR, 0, 6,
        UBIDI_MIXED, 0,
        testLevels18, testVisualMap18},
    {testText14, ARRAY_LENGTH(testText14), UBIDI_DEFAULT_LTR, 13, 14,
        UBIDI_RTL, 1,
        testLevels19, testVisualMap19},
    {testText15, ARRAY_LENGTH(testText15), UBIDI_DEFAULT_LTR, 2, 3,
        UBIDI_LTR, 2,
        testLevels20, testVisualMap19}
};

int bidiTestCount=ARRAY_LENGTH(tests);

void BiDiTest::TestBiDi() {
    UErrorCode errorCode=U_ZERO_ERROR;

    logln("*** bidi regression test ***\n");

    BiDi bidi(MAX_STRING_LENGTH, 0, errorCode);
    BiDi line;

    if(U_SUCCESS(errorCode)) {
        TestBiDi(bidi, line);

    } else {
        errln("BiDi constructor with 3 arguments returned NULL, errorCode %s\n", u_errorName(errorCode));
    }

    logln("*** bidi regression test finished ***\n");
}

void BiDiTest::TestBiDi(BiDi& bidi, BiDi& line) {
    int i;
    UChar* s;
    UErrorCode errorCode;
    int32_t lineStart;
    UBiDiLevel paraLevel;

    for(i=0; i<bidiTestCount; i++) {
        errorCode=U_ZERO_ERROR;
        s=getStringFromDirProps(tests[i].text, tests[i].length);
        paraLevel=tests[i].paraLevel;
        bidi.setPara(s, -1, paraLevel, NULL, errorCode);
        if(U_SUCCESS(errorCode)) {
            logln("setPara(tests[%d], paraLevel %d) ok, direction %d paraLevel=%d\n",
                    i, paraLevel, bidi.getDirection(), bidi.getParaLevel());
            lineStart=tests[i].lineStart;
            if(lineStart==-1) {
                TestBiDi(bidi, i, tests+i, 0);
            } else {
                /*ubidi_setLine(pBiDi, lineStart, tests[i].lineLimit, pLine, &errorCode);*/
                line.setLine(bidi,lineStart,tests[i].lineLimit,errorCode);
                if(U_SUCCESS(errorCode)) {
                    logln("setLine(%d, %d) ok, direction %d paraLevel=%d\n",
                            lineStart, tests[i].lineLimit, line.getDirection(), line.getParaLevel());
                    TestBiDi(line, i, tests+i, lineStart);
                } else {
                    errln("setLine(tests[%d], %d, %d) failed with errorCode %s\n",
                            i, lineStart, tests[i].lineLimit, u_errorName(errorCode));
                }
            }
        } else {
            errln("setPara(tests[%d], paraLevel %d) failed with errorCode %s\n",
                    i, paraLevel, u_errorName(errorCode));
        }
    }
}
void BiDiTest::TestBiDi(BiDi& bidi, int testNumber, BiDiTestData *test, int32_t lineStart) {
    const uint8_t *dirProps=test->text+lineStart;
    const UBiDiLevel *levels=test->levels;
    const uint8_t *visualMap=test->visualMap;
    int32_t i, len=bidi.getLength(), logicalIndex, runCount;
    UErrorCode errorCode=U_ZERO_ERROR;
    UBiDiLevel level, level2;

    TestReordering(bidi, testNumber);

    for(i=0; i<len; ++i) {
        logln("%3d %3d %.*s%-3s @%d\n",
                i, bidi.getLevelAt(i), bidi.getLevelAt(i), levelString,
                dirPropNames[dirProps[i]],
                bidi.getVisualIndex(i, errorCode));
    }

    logln("\n-----levels:");
    for(i=0; i<len; ++i) {
        if(i>0) {
            logln(",");
        }
        logln(" %d", bidi.getLevelAt(i));
    }

    logln("\n--reordered:");
    for(i=0; i<len; ++i) {
        if(i>0) {
            logln(",");
        }
        logln(" %d", bidi.getVisualIndex( i, errorCode));
    }
    logln("\n");

    if(test->direction!=bidi.getDirection()) {
        errln("getDirection(tests[%d]): wrong direction %d\n", testNumber, bidi.getDirection());
    }

    if(test->resultLevel!=bidi.getParaLevel()) {
        errln("getParaLevel(tests[%d]): wrong paragraph level %d\n", testNumber, bidi.getParaLevel());
    }

    for(i=0; i<len; ++i) {
        if(levels[i]!=bidi.getLevelAt(i)) {
            errln("getLevelAt(tests[%d], %d): wrong level %d\n", testNumber, i, bidi.getLevelAt(i));
            return;
        }
    }

    for(i=0; i<len; ++i) {
        logicalIndex=bidi.getVisualIndex(i,errorCode);
        if(U_FAILURE(errorCode)) {
            errln("getVisualIndex(tests[%d], %d): error %s\n", testNumber, i, u_errorName(errorCode));
            return;
        }
        if(visualMap[i]!=logicalIndex) {
            errln("getVisualIndex(tests[%d], %d): wrong index %d\n", testNumber, i, logicalIndex);
            return;
        }
    }

    runCount=bidi.countRuns(errorCode);
    if(U_FAILURE(errorCode)) {
        errln("countRuns(tests[%d]): error %s\n", testNumber, u_errorName(errorCode));
        return;
    }

    for(logicalIndex=0; logicalIndex<len;) {
        level=bidi.getLevelAt(logicalIndex);
        bidi.getLogicalRun(logicalIndex, logicalIndex, level2);
        if(level!=level2) {
            errln("getLogicalRun(tests[%d], run ending at index %d): wrong level %d\n", testNumber, logicalIndex, level2);
        }
        if(--runCount<0) {
            errln("\ngetLogicalRun(tests[%d]): wrong number of runs compared to %d=getRunCount()\n", testNumber, bidi.countRuns(errorCode));
            return;
        }
    }
    if(runCount!=0) {
        errln("\ngetLogicalRun(tests[%d]): wrong number of runs compared to %d=getRunCount()\n", testNumber, bidi.countRuns(errorCode));
        return;
    }

    logln("\n\n");
}

void BiDiTest::TestReordering(BiDi& bidi, int testNumber) {
    int32_t
        logicalMap1[200], logicalMap2[200], logicalMap3[200],
        visualMap1[200], visualMap2[200], visualMap3[200], visualMap4[200];
    UErrorCode errorCode=U_ZERO_ERROR;
    UBiDiLevel levels[200];
    int32_t i, length=bidi.getLength();
    int32_t runCount, visualIndex, logicalStart, runLength;
    UBool odd;

    if(length<=0) {
        return;
    }

    /* get the logical and visual maps from the object */
    bidi.getLogicalMap(logicalMap1, errorCode);
    if(U_FAILURE(errorCode)) {
        errln("getLogicalMap(tests[%d]): error %s\n", testNumber, u_errorName(errorCode));
        return;
    }

    bidi.getVisualMap(visualMap1, errorCode);

    if(U_FAILURE(errorCode)) {
        errln("getVisualMap(tests[%d]): error %s\n", testNumber, u_errorName(errorCode));
        return;
    }

    /* invert them both */
    bidi.invertMap(logicalMap1, visualMap2, length);
    bidi.invertMap(visualMap1, logicalMap2, length);

    /* get them from the levels array, too */
    uprv_memcpy(levels, bidi.getLevels(errorCode), length);

    if(U_FAILURE(errorCode)) {
        errln("getLevels(tests[%d]): error %s\n", testNumber, u_errorName(errorCode));
        return;
    }

    bidi.reorderLogical(levels, length, logicalMap3);
    bidi.reorderVisual(levels, length, visualMap3);

    /* get the visual map from the runs, too */
    runCount=bidi.countRuns(errorCode);
    if(U_FAILURE(errorCode)) {
        errln("countRuns(tests[%d]): error %s\n", testNumber, u_errorName(errorCode));
        return;
    }

    logln("\n----%2d runs:", runCount);
    for(i=0; i<runCount; ++i) {
        odd=(UBool)(bidi.getVisualRun(i, logicalStart, runLength));
        logln(" (%c @%d[%d])", odd ? 'R' : 'L', logicalStart, runLength);
    }
    logln("\n");

    visualIndex=0;
    for(i=0; i<runCount; ++i) {
        if(UBIDI_LTR==bidi.getVisualRun( i, logicalStart, runLength)) {
            do { /* LTR */
                visualMap4[visualIndex++]=logicalStart++;
            } while(--runLength>0);
        } else {
            logicalStart+=runLength;   /* logicalLimit */
            do { /* RTL */
                visualMap4[visualIndex++]=--logicalStart;
            } while(--runLength>0);
        }
    }

    /* print all the maps */
    logln("logical maps:\n");
    for(i=0; i<length; ++i) {
        logln("%4d", logicalMap1[i]);
    }
    logln("\n");
    for(i=0; i<length; ++i) {
        logln("%4d", logicalMap2[i]);
    }
    logln("\n");
    for(i=0; i<length; ++i) {
        logln("%4d", logicalMap3[i]);
    }

    logln("\nvisual maps:\n");
    for(i=0; i<length; ++i) {
        logln("%4d", visualMap1[i]);
    }
    logln("\n");
    for(i=0; i<length; ++i) {
        logln("%4d", visualMap2[i]);
    }
    logln("\n");
    for(i=0; i<length; ++i) {
        logln("%4d", visualMap3[i]);
    }
    logln("\n");
    for(i=0; i<length; ++i) {
        logln("%4d", visualMap4[i]);
    }
    logln("\n");

    /* check that the indexes are the same between these and getLogical/VisualIndex() */
    for(i=0; i<length; ++i) {
        if(logicalMap1[i]!=logicalMap2[i]) {
            logln("bidi reordering error in tests[%d]: logicalMap1[i]!=logicalMap2[i] at i=%d\n", testNumber, i);
            break;
        }
        if(logicalMap1[i]!=logicalMap3[i]) {
            logln("bidi reordering error in tests[%d]: logicalMap1[i]!=logicalMap3[i] at i=%d\n", testNumber, i);
            break;
        }

        if(visualMap1[i]!=visualMap2[i]) {
            logln("bidi reordering error in tests[%d]: visualMap1[i]!=visualMap2[i] at i=%d\n", testNumber, i);
            break;
        }
        if(visualMap1[i]!=visualMap3[i]) {
            logln("bidi reordering error in tests[%d]: visualMap1[i]!=visualMap3[i] at i=%d\n", testNumber, i);
            break;
        }
        if(visualMap1[i]!=visualMap4[i]) {
            logln("bidi reordering error in tests[%d]: visualMap1[i]!=visualMap4[i] at i=%d\n", testNumber, i);
            break;
        }

        if(logicalMap1[i]!=bidi.getVisualIndex( i, errorCode)) {
            logln("bidi reordering error in tests[%d]: logicalMap1[i]!=getVisualIndex(i) at i=%d\n", testNumber, i);
            break;
        }
        if(U_FAILURE(errorCode)) {
            logln("getVisualIndex(tests[%d], %d): error %s\n", testNumber, i, u_errorName(errorCode));
            break;
        }
        if(visualMap1[i]!=bidi.getLogicalIndex(i, errorCode)) {
            logln("bidi reordering error in tests[%d]: visualMap1[i]!=getLogicalIndex(i) at i=%d\n", testNumber, i);
            break;
        }
        if(U_FAILURE(errorCode)) {
            logln("getLogicalIndex(tests[%d], %d): error %s\n", testNumber, i, u_errorName(errorCode));
            break;
        }
    }
}
 
#define LENGTHOF(array) (sizeof(array)/sizeof((array)[0]))
/* inverse BiDi ------------------------------------------------------------- */

static const UChar
    string0[]={ 0x6c, 0x61, 0x28, 0x74, 0x69, 0x6e, 0x20, 0x5d0, 0x5d1, 0x29, 0x5d2, 0x5d3 },
    string1[]={ 0x6c, 0x61, 0x74, 0x20, 0x5d0, 0x5d1, 0x5d2, 0x20, 0x31, 0x32, 0x33 },
    string2[]={ 0x6c, 0x61, 0x74, 0x20, 0x5d0, 0x28, 0x5d1, 0x5d2, 0x20, 0x31, 0x29, 0x32, 0x33 },
    string3[]={ 0x31, 0x32, 0x33, 0x20, 0x5d0, 0x5d1, 0x5d2, 0x20, 0x34, 0x35, 0x36 },
    string4[]={ 0x61, 0x62, 0x20, 0x61, 0x62, 0x20, 0x661, 0x662 };

#define STRING_TEST_CASE(s) { (s), LENGTHOF(s) }

static const struct {
    const UChar *s;
    int32_t length;
} testCases[]={
    STRING_TEST_CASE(string0),
    STRING_TEST_CASE(string1),
    STRING_TEST_CASE(string2),
    STRING_TEST_CASE(string3)
};

static int countRoundtrips=0, countNonRoundtrips=0;

void BiDiTest::TestInverseBiDi() {
    BiDi bidi;
    UErrorCode errorCode;
    int i;

    logln("inverse BiDi: testInverseBiDi(L) with %u test cases ---\n", LENGTHOF(testCases));
    for(i=0; i<LENGTHOF(testCases); ++i) {
        errorCode=U_ZERO_ERROR;
        TestInverseBiDi(bidi, testCases[i].s,testCases[i].length, 0, errorCode);
    }

    logln("inverse BiDi: testInverseBiDi(R) with %u test cases ---\n", LENGTHOF(testCases));
    for(i=0; i<LENGTHOF(testCases); ++i) {
        errorCode=U_ZERO_ERROR;
        TestInverseBiDi(bidi, testCases[i].s,testCases[i].length, 1, errorCode);
    }

    TestInverseBiDi(bidi, 0);
    TestInverseBiDi(bidi, 1);

 
    logln("inverse BiDi: rountrips: %5u\nnon-roundtrips: %5u\n", countRoundtrips, countNonRoundtrips);

    TestWriteReverse();
}

#define COUNT_REPEAT_SEGMENTS 6

static const UChar repeatSegments[COUNT_REPEAT_SEGMENTS][2]={
    { 0x61, 0x62 },     /* L */
    { 0x5d0, 0x5d1 },   /* R */
    { 0x627, 0x628 },   /* AL */
    { 0x31, 0x32 },     /* EN */
    { 0x661, 0x662 },   /* AN */
    { 0x20, 0x20 }      /* WS (N) */
};

void BiDiTest::TestInverseBiDi(BiDi& bidi, UBiDiLevel direction) {
    static UChar text[8]={ 0, 0, 0x20, 0, 0, 0x20, 0, 0 };
    int i, j, k;
    UErrorCode errorCode;

    logln("inverse BiDi: testManyInverseBiDi(%c) - test permutations of text snippets ---\n", direction==0 ? 'L' : 'R');
    for(i=0; i<COUNT_REPEAT_SEGMENTS; ++i) {
        text[0]=repeatSegments[i][0];
        text[1]=repeatSegments[i][1];
        for(j=0; j<COUNT_REPEAT_SEGMENTS; ++j) {
            text[3]=repeatSegments[j][0];
            text[4]=repeatSegments[j][1];
            for(k=0; k<COUNT_REPEAT_SEGMENTS; ++k) {
                text[6]=repeatSegments[k][0];
                text[7]=repeatSegments[k][1];

                errorCode=U_ZERO_ERROR;
                logln("inverse BiDi: testManyInverseBiDi()[%u %u %u]\n", i, j, k);
                TestInverseBiDi(bidi,text,8,direction, errorCode);
            }
        }
    }
}

void BiDiTest::TestInverseBiDi(BiDi& bidi, const UChar *src, int32_t srcLength, UBiDiLevel direction, UErrorCode& errorCode) {
    static UChar visualLTR[200], logicalDest[200], visualDest[200];
    int32_t ltrLength, logicalLength, visualLength;

    if(direction==0) {
        logln("inverse BiDi: testInverseBiDi(L)\n");

        /* convert visual to logical */
        bidi.setInverse(TRUE);
        bidi.setPara(src, srcLength, 0, NULL, errorCode);
        logicalLength=bidi.writeReordered(logicalDest, LENGTHOF(logicalDest),
                                           UBIDI_DO_MIRRORING|UBIDI_INSERT_LRM_FOR_NUMERIC, errorCode);
        logln("  v ");
        printUnicode(src, srcLength, bidi.getLevels(errorCode));
        logln("\n");

        /* convert back to visual LTR */
        bidi.setInverse(FALSE);
        bidi.setPara(logicalDest, logicalLength, 0, NULL, errorCode);
        visualLength=bidi.writeReordered(visualDest, LENGTHOF(visualDest),
                                          UBIDI_DO_MIRRORING|UBIDI_REMOVE_BIDI_CONTROLS, errorCode);
    } else {
        logln("inverse BiDi: testInverseBiDi(R)\n");

        /* reverse visual from RTL to LTR */
        ltrLength=bidi.writeReverse(src, srcLength, visualLTR, LENGTHOF(visualLTR), 0, errorCode);
        logln("  vr");
        printUnicode(src, srcLength, NULL);
        logln("\n");

        /* convert visual RTL to logical */
        bidi.setInverse(TRUE);
        bidi.setPara(visualLTR, ltrLength, 0, NULL, errorCode);
        logicalLength=bidi.writeReordered(logicalDest, LENGTHOF(logicalDest),
                                           UBIDI_DO_MIRRORING|UBIDI_INSERT_LRM_FOR_NUMERIC, errorCode);
        logln("  vl");
        printUnicode(visualLTR, ltrLength, bidi.getLevels(errorCode));
        logln("\n");

        /* convert back to visual RTL */
        bidi.setInverse(FALSE);
        bidi.setPara(logicalDest, logicalLength, 0, NULL, errorCode);
        visualLength=bidi.writeReordered(visualDest, LENGTHOF(visualDest),
                                          UBIDI_DO_MIRRORING|UBIDI_REMOVE_BIDI_CONTROLS|UBIDI_OUTPUT_REVERSE, errorCode);
    }
    logln("  l ");
    printUnicode(logicalDest, logicalLength, bidi.getLevels(errorCode));
    logln("\n");
    logln("  v ");
    printUnicode(visualDest, visualLength, NULL);
    logln("\n");

    /* check and print results */
    if(U_FAILURE(errorCode)) {
        errln("inverse BiDi: *** error %s\n"
                "                 turn on verbose mode to see details\n", u_errorName(errorCode));
    } else if(srcLength==visualLength && uprv_memcmp(src, visualDest, srcLength*U_SIZEOF_UCHAR)==0) {
        ++countRoundtrips;
        logln(" + roundtripped\n");
    } else {
        ++countNonRoundtrips;
        logln(" * did not roundtrip\n");
        errln("inverse BiDi: transformation visual->logical->visual did not roundtrip the text;\n"
                "                 turn on verbose mode to see details\n");
    }
}

void BiDiTest::TestWriteReverse() {
    /* U+064e and U+0650 are combining marks (Mn) */
    static const UChar forward[]={
        0x200f, 0x627, 0x64e, 0x650, 0x20, 0x28, 0x31, 0x29
    }, reverseKeepCombining[]={
        0x29, 0x31, 0x28, 0x20, 0x627, 0x64e, 0x650, 0x200f
    }, reverseRemoveControlsKeepCombiningDoMirror[]={
        0x28, 0x31, 0x29, 0x20, 0x627, 0x64e, 0x650
    };
    static UChar reverse[10];
    UErrorCode errorCode;
    int32_t length;

    /* test writeReverse() with "interesting" options */
    errorCode=U_ZERO_ERROR;
    length=BiDi::writeReverse(forward, LENGTHOF(forward),
                              reverse, LENGTHOF(reverse),
                              UBIDI_KEEP_BASE_COMBINING,
                              errorCode);
    if(U_FAILURE(errorCode) || length!=LENGTHOF(reverseKeepCombining) || uprv_memcmp(reverse, reverseKeepCombining, length*U_SIZEOF_UCHAR)!=0) {
        errln("failure in writeReverse(UBIDI_KEEP_BASE_COMBINING): length=%d (should be %d), error code %s\n",
                length, LENGTHOF(reverseKeepCombining), u_errorName(errorCode));
    }

    uprv_memset(reverse, 0xa5, LENGTHOF(reverse)*U_SIZEOF_UCHAR);
    errorCode=U_ZERO_ERROR;
    length=BiDi::writeReverse(forward, LENGTHOF(forward),
                              reverse, LENGTHOF(reverse),
                              UBIDI_REMOVE_BIDI_CONTROLS|UBIDI_DO_MIRRORING|UBIDI_KEEP_BASE_COMBINING,
                              errorCode);
    if(U_FAILURE(errorCode) || length!=LENGTHOF(reverseRemoveControlsKeepCombiningDoMirror) || uprv_memcmp(reverse, reverseRemoveControlsKeepCombiningDoMirror, length*U_SIZEOF_UCHAR)!=0) {
        errln("failure in writeReverse(UBIDI_REMOVE_BIDI_CONTROLS|UBIDI_DO_MIRRORING|UBIDI_KEEP_BASE_COMBINING):\n"
                "    length=%d (should be %d), error code %s\n",
                length, LENGTHOF(reverseRemoveControlsKeepCombiningDoMirror), u_errorName(errorCode));
    }
}

/* helpers ------------------------------------------------------------------ */

/* return a string with characters according to the desired directional properties */
UChar * BiDiTest::getStringFromDirProps(const uint8_t *dirProps, int32_t length) {
    static UChar s[MAX_STRING_LENGTH];
    int32_t i;

    /* this part would have to be modified for UTF-x */
    for(i=0; i<length; ++i) {
        s[i]=charFromDirProp[dirProps[i]];
    }
    s[i]=0;
    return s;
}

void BiDiTest::printUnicode(const UChar *s, int32_t length, const UBiDiLevel *levels) {
    int32_t i;

    logln("{ ");
    for(i=0; i<length; ++i) {
        if(levels!=NULL) {
            logln("%4x.%u  ", s[i], levels[i]);
        } else {
            logln("%4x    ", s[i]);
        }
    }
    logln(" }");
}

void BiDiTest::runIndexedTest(int32_t index, UBool exec, const char *&name, char * /*par*/) {
    if(exec) {
        logln("TestSuite Character and String Test: ");
    }
    switch(index) {
    case 0:
        name="TestBiDi";
        if(exec) {
            TestBiDi();
        }
        break;
    case 1:
        name="TestInverseBiDi";
        if(exec) {
            TestInverseBiDi();
        }
        break;
    case 2:
        name="TestWriteReverse";
        if(exec) {
            TestWriteReverse();
        }
        break;
    default:
        name="";
        break;
    }
}
