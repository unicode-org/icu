/*  
*******************************************************************************
*                                                                             *
* COPYRIGHT:                                                                  *
*   (C) Copyright International Business Machines Corporation, 1999           *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.        *
*   US Government Users Restricted Rights - Use, duplication, or disclosure   *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                    *
*                                                                             *
*******************************************************************************
*   file name:  cbiditst.cpp
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

static char *levelString;

static UChar *
getStringFromDirProps(const uint8_t *dirProps, UTextOffset length);

/* regression tests ---------------------------------------------------------*/

extern void
addComplexTest(TestNode** root) {
    addTest(root, doBiDiTest, "complex/bidi/regression");
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
    bool_t odd;

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

/* helpers ------------------------------------------------------------------*/

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
