/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/*   file name:  cbiditst.cpp
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 1999sep27
*   created by: Markus W. Scherer
*/

#include "cintltst.h"
#include "unicode/utypes.h"
#include "unicode/uchar.h"
#include "unicode/ubidi.h"
#include "cbiditst.h"

/* prototypes ---------------------------------------------------------------*/

extern void
doBiDiTest(void);

static void
doTests(UBiDi *pBiDi, UBiDi *pLine);

static void
doTest(UBiDi *pBiDi, int testNumber, BiDiTestData *test, UTextOffset lineStart);

static void
testReordering(UBiDi *pBiDi, int testNumber);

extern void
doInverseBiDiTest();

static void
testManyInverseBiDi(UBiDi *pBiDi, UBiDiLevel direction);

static void
testInverseBiDi(UBiDi *pBiDi, const UChar *src, int32_t srcLength, UBiDiLevel direction, UErrorCode *pErrorCode);

static char *levelString;

static UChar *
getStringFromDirProps(const uint8_t *dirProps, UTextOffset length);

static void
printUnicode(const UChar *s, int32_t length, const UBiDiLevel *levels);

/* regression tests ---------------------------------------------------------*/

extern void
addComplexTest(TestNode** root) {
    addTest(root, doBiDiTest, "complex/bidi");
    addTest(root, doInverseBiDiTest, "complex/invbidi");
}

extern void
doBiDiTest() {
    UBiDi *pBiDi, *pLine=NULL;
    UErrorCode errorCode=U_ZERO_ERROR;

    log_verbose("*** bidi regression test ***\n");

    pBiDi=ubidi_openSized(MAX_STRING_LENGTH, 0, &errorCode);
    if(pBiDi!=NULL) {
        pLine=ubidi_open();
        if(pLine!=NULL) {
            doTests(pBiDi, pLine);
        } else {
            log_err("ubidi_open() returned NULL, out of memory\n");
        }
    } else {
        log_err("ubidi_openSized() returned NULL, errorCode %s\n", myErrorName(errorCode));
    }

    if(pLine!=NULL) {
        ubidi_close(pLine);
    }
    if(pBiDi!=NULL) {
        ubidi_close(pBiDi);
    }

    log_verbose("*** bidi regression test finished ***\n");
}

static void
doTests(UBiDi *pBiDi, UBiDi *pLine) {
    int i;
    UChar *s;
    UErrorCode errorCode;
    UTextOffset lineStart;
    UBiDiLevel paraLevel;

    for(i=0; i<bidiTestCount; ++i) {
        errorCode=U_ZERO_ERROR;
        s=getStringFromDirProps(tests[i].text, tests[i].length);
        paraLevel=tests[i].paraLevel;
        ubidi_setPara(pBiDi, s, -1, paraLevel, NULL, &errorCode);
        if(U_SUCCESS(errorCode)) {
            log_verbose("ubidi_setPara(tests[%d], paraLevel %d) ok, direction %d paraLevel=%d\n",
                    i, paraLevel, ubidi_getDirection(pBiDi), ubidi_getParaLevel(pBiDi));
            lineStart=tests[i].lineStart;
            if(lineStart==-1) {
                doTest(pBiDi, i, tests+i, 0);
            } else {
                ubidi_setLine(pBiDi, lineStart, tests[i].lineLimit, pLine, &errorCode);
                if(U_SUCCESS(errorCode)) {
                    log_verbose("ubidi_setLine(%d, %d) ok, direction %d paraLevel=%d\n",
                            lineStart, tests[i].lineLimit, ubidi_getDirection(pLine), ubidi_getParaLevel(pLine));
                    doTest(pLine, i, tests+i, lineStart);
                } else {
                    log_err("ubidi_setLine(tests[%d], %d, %d) failed with errorCode %s\n",
                            i, lineStart, tests[i].lineLimit, myErrorName(errorCode));
                }
            }
        } else {
            log_err("ubidi_setPara(tests[%d], paraLevel %d) failed with errorCode %s\n",
                    i, paraLevel, myErrorName(errorCode));
        }
    }
}

static void
doTest(UBiDi *pBiDi, int testNumber, BiDiTestData *test, UTextOffset lineStart) {
    const uint8_t *dirProps=test->text+lineStart;
    const UBiDiLevel *levels=test->levels;
    const uint8_t *visualMap=test->visualMap;
    UTextOffset i, len=ubidi_getLength(pBiDi), logicalIndex, runCount;
    UErrorCode errorCode=U_ZERO_ERROR;
    UBiDiLevel level, level2;

    testReordering(pBiDi, testNumber);

    for(i=0; i<len; ++i) {
        log_verbose("%3d %3d %.*s%-3s @%d\n",
                i, ubidi_getLevelAt(pBiDi, i), ubidi_getLevelAt(pBiDi, i), levelString,
                dirPropNames[dirProps[i]],
                ubidi_getVisualIndex(pBiDi, i, &errorCode));
    }

    log_verbose("\n-----levels:");
    for(i=0; i<len; ++i) {
        if(i>0) {
            log_verbose(",");
        }
        log_verbose(" %d", ubidi_getLevelAt(pBiDi, i));
    }

    log_verbose("\n--reordered:");
    for(i=0; i<len; ++i) {
        if(i>0) {
            log_verbose(",");
        }
        log_verbose(" %d", ubidi_getVisualIndex(pBiDi, i, &errorCode));
    }
    log_verbose("\n");

    if(test->direction!=ubidi_getDirection(pBiDi)) {
        log_err("ubidi_getDirection(tests[%d]): wrong direction %d\n", testNumber, ubidi_getDirection(pBiDi));
    }

    if(test->resultLevel!=ubidi_getParaLevel(pBiDi)) {
        log_err("ubidi_getParaLevel(tests[%d]): wrong paragraph level %d\n", testNumber, ubidi_getParaLevel(pBiDi));
    }

    for(i=0; i<len; ++i) {
        if(levels[i]!=ubidi_getLevelAt(pBiDi, i)) {
            log_err("ubidi_getLevelAt(tests[%d], %d): wrong level %d\n", testNumber, i, ubidi_getLevelAt(pBiDi, i));
            return;
        }
    }

    for(i=0; i<len; ++i) {
        logicalIndex=ubidi_getVisualIndex(pBiDi, i, &errorCode);
        if(U_FAILURE(errorCode)) {
            log_err("ubidi_getVisualIndex(tests[%d], %d): error %s\n", testNumber, i, myErrorName(errorCode));
            return;
        }
        if(visualMap[i]!=logicalIndex) {
            log_err("ubidi_getVisualIndex(tests[%d], %d): wrong index %d\n", testNumber, i, logicalIndex);
            return;
        }
    }

    runCount=ubidi_countRuns(pBiDi, &errorCode);
    if(U_FAILURE(errorCode)) {
        log_err("ubidi_countRuns(tests[%d]): error %s\n", testNumber, myErrorName(errorCode));
        return;
    }

    for(logicalIndex=0; logicalIndex<len;) {
        level=ubidi_getLevelAt(pBiDi, logicalIndex);
        ubidi_getLogicalRun(pBiDi, logicalIndex, &logicalIndex, &level2);
        if(level!=level2) {
            log_err("ubidi_getLogicalRun(tests[%d], run ending at index %d): wrong level %d\n", testNumber, logicalIndex, level2);
        }
        if(--runCount<0) {
            log_err("\nubidi_getLogicalRun(tests[%d]): wrong number of runs compared to %d=ubidi_getRunCount()\n", testNumber, ubidi_countRuns(pBiDi, &errorCode));
            return;
        }
    }
    if(runCount!=0) {
        log_err("\nubidi_getLogicalRun(tests[%d]): wrong number of runs compared to %d=ubidi_getRunCount()\n", testNumber, ubidi_countRuns(pBiDi, &errorCode));
        return;
    }

    log_verbose("\n\n");
}

static void
testReordering(UBiDi *pBiDi, int testNumber) {
    UTextOffset
        logicalMap1[200], logicalMap2[200], logicalMap3[200],
        visualMap1[200], visualMap2[200], visualMap3[200], visualMap4[200];
    UErrorCode errorCode=U_ZERO_ERROR;
    UBiDiLevel levels[200];
    UTextOffset i, length=ubidi_getLength(pBiDi);
    UTextOffset runCount, visualIndex, logicalStart, runLength;
    int result=0;
    UBool odd;

    if(length<=0) {
        return;
    }

    /* get the logical and visual maps from the object */
    ubidi_getLogicalMap(pBiDi, logicalMap1, &errorCode);
    if(U_FAILURE(errorCode)) {
        log_err("ubidi_getLogicalMap(tests[%d]): error %s\n", testNumber, myErrorName(errorCode));
        return;
    }

    ubidi_getVisualMap(pBiDi, visualMap1, &errorCode);

    if(U_FAILURE(errorCode)) {
        log_err("ubidi_getVisualMap(tests[%d]): error %s\n", testNumber, myErrorName(errorCode));
        return;
    }

    /* invert them both */
    ubidi_invertMap(logicalMap1, visualMap2, length);
    ubidi_invertMap(visualMap1, logicalMap2, length);

    /* get them from the levels array, too */
    memcpy(levels, ubidi_getLevels(pBiDi, &errorCode), length);

    if(U_FAILURE(errorCode)) {
        log_err("ubidi_getLevels(tests[%d]): error %s\n", testNumber, myErrorName(errorCode));
        return;
    }

    ubidi_reorderLogical(levels, length, logicalMap3);
    ubidi_reorderVisual(levels, length, visualMap3);

    /* get the visual map from the runs, too */
    runCount=ubidi_countRuns(pBiDi, &errorCode);
    if(U_FAILURE(errorCode)) {
        log_err("ubidi_countRuns(tests[%d]): error %s\n", testNumber, myErrorName(errorCode));
        return;
    }

    log_verbose("\n----%2d runs:", runCount);
    for(i=0; i<runCount; ++i) {
        odd=ubidi_getVisualRun(pBiDi, i, &logicalStart, &runLength);
        log_verbose(" (%c @%d[%d])", odd ? 'R' : 'L', logicalStart, runLength);
    }
    log_verbose("\n");

    visualIndex=0;
    for(i=0; i<runCount; ++i) {
        if(UBIDI_LTR==ubidi_getVisualRun(pBiDi, i, &logicalStart, &runLength)) {
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
    log_verbose("logical maps:\n");
    for(i=0; i<length; ++i) {
        log_verbose("%4d", logicalMap1[i]);
    }
    log_verbose("\n");
    for(i=0; i<length; ++i) {
        log_verbose("%4d", logicalMap2[i]);
    }
    log_verbose("\n");
    for(i=0; i<length; ++i) {
        log_verbose("%4d", logicalMap3[i]);
    }

    log_verbose("\nvisual maps:\n");
    for(i=0; i<length; ++i) {
        log_verbose("%4d", visualMap1[i]);
    }
    log_verbose("\n");
    for(i=0; i<length; ++i) {
        log_verbose("%4d", visualMap2[i]);
    }
    log_verbose("\n");
    for(i=0; i<length; ++i) {
        log_verbose("%4d", visualMap3[i]);
    }
    log_verbose("\n");
    for(i=0; i<length; ++i) {
        log_verbose("%4d", visualMap4[i]);
    }
    log_verbose("\n");

    /* check that the indexes are the same between these and ubidi_getLogical/VisualIndex() */
    for(i=0; i<length; ++i) {
        if(logicalMap1[i]!=logicalMap2[i]) {
            log_verbose("bidi reordering error in tests[%d]: logicalMap1[i]!=logicalMap2[i] at i=%d\n", testNumber, i);
            break;
        }
        if(logicalMap1[i]!=logicalMap3[i]) {
            log_verbose("bidi reordering error in tests[%d]: logicalMap1[i]!=logicalMap3[i] at i=%d\n", testNumber, i);
            break;
        }

        if(visualMap1[i]!=visualMap2[i]) {
            log_verbose("bidi reordering error in tests[%d]: visualMap1[i]!=visualMap2[i] at i=%d\n", testNumber, i);
            break;
        }
        if(visualMap1[i]!=visualMap3[i]) {
            log_verbose("bidi reordering error in tests[%d]: visualMap1[i]!=visualMap3[i] at i=%d\n", testNumber, i);
            break;
        }
        if(visualMap1[i]!=visualMap4[i]) {
            log_verbose("bidi reordering error in tests[%d]: visualMap1[i]!=visualMap4[i] at i=%d\n", testNumber, i);
            break;
        }

        if(logicalMap1[i]!=ubidi_getVisualIndex(pBiDi, i, &errorCode)) {
            log_verbose("bidi reordering error in tests[%d]: logicalMap1[i]!=ubidi_getVisualIndex(i) at i=%d\n", testNumber, i);
            break;
        }
        if(U_FAILURE(errorCode)) {
            log_verbose("ubidi_getVisualIndex(tests[%d], %d): error %s\n", testNumber, i, myErrorName(errorCode));
            break;
        }
        if(visualMap1[i]!=ubidi_getLogicalIndex(pBiDi, i, &errorCode)) {
            log_verbose("bidi reordering error in tests[%d]: visualMap1[i]!=ubidi_getLogicalIndex(i) at i=%d\n", testNumber, i);
            break;
        }
        if(U_FAILURE(errorCode)) {
            log_verbose("ubidi_getLogicalIndex(tests[%d], %d): error %s\n", testNumber, i, myErrorName(errorCode));
            break;
        }
    }
}

/* inverse BiDi ------------------------------------------------------------- */

static const UChar
    string0[]={ 0x6c, 0x61, 0x28, 0x74, 0x69, 0x6e, 0x20, 0x5d0, 0x5d1, 0x29, 0x5d2, 0x5d3 },
    string1[]={ 0x6c, 0x61, 0x74, 0x20, 0x5d0, 0x5d1, 0x5d2, 0x20, 0x31, 0x32, 0x33 },
    string2[]={ 0x6c, 0x61, 0x74, 0x20, 0x5d0, 0x28, 0x5d1, 0x5d2, 0x20, 0x31, 0x29, 0x32, 0x33 },
    string3[]={ 0x31, 0x32, 0x33, 0x20, 0x5d0, 0x5d1, 0x5d2, 0x20, 0x34, 0x35, 0x36 },
    string4[]={ 0x61, 0x62, 0x20, 0x61, 0x62, 0x20, 0x661, 0x662 };

#define STRING_TEST_CASE(s) { (s), sizeof(s)/U_SIZEOF_UCHAR }

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

extern void
doInverseBiDiTest() {
    UBiDi *pBiDi;
    UErrorCode errorCode;
    int i;

    pBiDi=ubidi_open();
    if(pBiDi==NULL) {
        log_err("unable to open a UBiDi object (out of memory)\n");
        return;
    }

    log_info("inverse BiDi: testInverseBiDi(L) with %u test cases ---\n", sizeof(testCases)/sizeof(testCases[0]));
    for(i=0; i<sizeof(testCases)/sizeof(testCases[0]); ++i) {
        errorCode=U_ZERO_ERROR;
        testInverseBiDi(pBiDi, testCases[i].s, testCases[i].length, 0, &errorCode);
    }

    log_info("inverse BiDi: testInverseBiDi(R) with %u test cases ---\n", sizeof(testCases)/sizeof(testCases[0]));
    for(i=0; i<sizeof(testCases)/sizeof(testCases[0]); ++i) {
        errorCode=U_ZERO_ERROR;
        testInverseBiDi(pBiDi, testCases[i].s, testCases[i].length, 1, &errorCode);
    }

    testManyInverseBiDi(pBiDi, 0);
    testManyInverseBiDi(pBiDi, 1);

    ubidi_close(pBiDi);

    log_verbose("inverse BiDi: rountrips: %5u\nnon-roundtrips: %5u\n", countRoundtrips, countNonRoundtrips);
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

static void
testManyInverseBiDi(UBiDi *pBiDi, UBiDiLevel direction) {
    static UChar text[8]={ 0, 0, 0x20, 0, 0, 0x20, 0, 0 };
    int i, j, k;
    UErrorCode errorCode;

    log_info("inverse BiDi: testManyInverseBiDi(%c) - test permutations of text snippets ---\n", direction==0 ? 'L' : 'R');
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
                log_verbose("inverse BiDi: testManyInverseBiDi()[%u %u %u]\n", i, j, k);
                testInverseBiDi(pBiDi, text, 8, direction, &errorCode);
            }
        }
    }
}

static void
testInverseBiDi(UBiDi *pBiDi, const UChar *src, int32_t srcLength, UBiDiLevel direction, UErrorCode *pErrorCode) {
    static UChar visualLTR[200], logicalDest[200], visualDest[200];
    int32_t ltrLength, logicalLength, visualLength;

    if(direction==0) {
        log_verbose("inverse BiDi: testInverseBiDi(L)\n");

        /* convert visual to logical */
        ubidi_setInverse(pBiDi, TRUE);
        ubidi_setPara(pBiDi, src, srcLength, 0, NULL, pErrorCode);
        logicalLength=ubidi_writeReordered(pBiDi, logicalDest, sizeof(logicalDest)/U_SIZEOF_UCHAR,
                                           UBIDI_DO_MIRRORING|UBIDI_INSERT_LRM_FOR_NUMERIC, pErrorCode);
        log_verbose("  v ");
        printUnicode(src, srcLength, ubidi_getLevels(pBiDi, pErrorCode));
        log_verbose("\n");

        /* convert back to visual LTR */
        ubidi_setInverse(pBiDi, FALSE);
        ubidi_setPara(pBiDi, logicalDest, logicalLength, 0, NULL, pErrorCode);
        visualLength=ubidi_writeReordered(pBiDi, visualDest, sizeof(visualDest)/U_SIZEOF_UCHAR,
                                          UBIDI_DO_MIRRORING|UBIDI_REMOVE_BIDI_CONTROLS, pErrorCode);
    } else {
        log_verbose("inverse BiDi: testInverseBiDi(R)\n");

        /* reverse visual from RTL to LTR */
        ltrLength=ubidi_writeReverse(src, srcLength, visualLTR, sizeof(visualLTR)/U_SIZEOF_UCHAR, 0, pErrorCode);
        log_verbose("  vr");
        printUnicode(src, srcLength, NULL);
        log_verbose("\n");

        /* convert visual RTL to logical */
        ubidi_setInverse(pBiDi, TRUE);
        ubidi_setPara(pBiDi, visualLTR, ltrLength, 0, NULL, pErrorCode);
        logicalLength=ubidi_writeReordered(pBiDi, logicalDest, sizeof(logicalDest)/U_SIZEOF_UCHAR,
                                           UBIDI_DO_MIRRORING|UBIDI_INSERT_LRM_FOR_NUMERIC, pErrorCode);
        log_verbose("  vl");
        printUnicode(visualLTR, ltrLength, ubidi_getLevels(pBiDi, pErrorCode));
        log_verbose("\n");

        /* convert back to visual RTL */
        ubidi_setInverse(pBiDi, FALSE);
        ubidi_setPara(pBiDi, logicalDest, logicalLength, 0, NULL, pErrorCode);
        visualLength=ubidi_writeReordered(pBiDi, visualDest, sizeof(visualDest)/U_SIZEOF_UCHAR,
                                          UBIDI_DO_MIRRORING|UBIDI_REMOVE_BIDI_CONTROLS|UBIDI_OUTPUT_REVERSE, pErrorCode);
    }
    log_verbose("  l ");
    printUnicode(logicalDest, logicalLength, ubidi_getLevels(pBiDi, pErrorCode));
    log_verbose("\n");
    log_verbose("  v ");
    printUnicode(visualDest, visualLength, NULL);
    log_verbose("\n");

    /* check and print results */
    if(U_FAILURE(*pErrorCode)) {
        log_err("inverse BiDi: *** error %s\n"
                "                 turn on verbose mode to see details\n", u_errorName(*pErrorCode));
    } else if(srcLength==visualLength && memcmp(src, visualDest, srcLength*U_SIZEOF_UCHAR)==0) {
        ++countRoundtrips;
        log_verbose(" + roundtripped\n");
    } else {
        ++countNonRoundtrips;
        log_verbose(" * did not roundtrip\n");
        log_err("inverse BiDi: transformation visual->logical->visual did not roundtrip the text;\n"
                "                 turn on verbose mode to see details\n");
    }
}

/* helpers ------------------------------------------------------------------ */

static char *levelString="...............................................................";

/* return a string with characters according to the desired directional properties */
static UChar *
getStringFromDirProps(const uint8_t *dirProps, UTextOffset length) {
    static UChar s[MAX_STRING_LENGTH];
    UTextOffset i;

    /* this part would have to be modified for UTF-x */
    for(i=0; i<length; ++i) {
        s[i]=charFromDirProp[dirProps[i]];
    }
    s[i]=0;
    return s;
}

static void
printUnicode(const UChar *s, int32_t length, const UBiDiLevel *levels) {
    int32_t i;

    log_verbose("{ ");
    for(i=0; i<length; ++i) {
        if(levels!=NULL) {
            log_verbose("%4x.%u  ", s[i], levels[i]);
        } else {
            log_verbose("%4x    ", s[i]);
        }
    }
    log_verbose(" }");
}
