/********************************************************************
 * COPYRIGHT:
 * Copyright (c) 1997-2004, International Business Machines Corporation and
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
#include "unicode/ustring.h"
#include "unicode/ubidi.h"
#include "unicode/ushape.h"
#include "cmemory.h"
#include "cbiditst.h"
#include "cstring.h"

#define LENGTHOF(array) (sizeof(array)/sizeof((array)[0]))

/* prototypes ---------------------------------------------------------------*/

static void
charFromDirPropTest(void);

static void
doBiDiTest(void);

static void
doTests(UBiDi *pBiDi, UBiDi *pLine, UBool countRunsFirst);

static void
doTest(UBiDi *pBiDi, int testNumber, BiDiTestData *test, int32_t lineStart, UBool countRunsFirst);

static void
testReordering(UBiDi *pBiDi, int testNumber);

static void
doInverseBiDiTest(void);

static void
testManyInverseBiDi(UBiDi *pBiDi, UBiDiLevel direction);

static void
testInverseBiDi(UBiDi *pBiDi, const UChar *src, int32_t srcLength, UBiDiLevel direction, UErrorCode *pErrorCode);

static void
testWriteReverse(void);

static void
doArabicShapingTest(void);

static void
doLamAlefSpecialVLTRArabicShapingTest(void);

static void
doTashkeelSpecialVLTRArabicShapingTest(void);

static void
doLOGICALArabicDeShapingTest(void);

static void TestReorder(void);

/* helpers ------------------------------------------------------------------ */

static const char *levelString="...............................................................";

static void
initCharFromDirProps(void);

static UChar *
getStringFromDirProps(const uint8_t *dirProps, int32_t length);

static void
printUnicode(const UChar *s, int32_t length, const UBiDiLevel *levels);

/* regression tests ---------------------------------------------------------*/

void addComplexTest(TestNode** root);

void
addComplexTest(TestNode** root) {
    addTest(root, charFromDirPropTest, "complex/bidi/charFromDirPropTest");
    addTest(root, doBiDiTest, "complex/bidi/BiDiTest");
    addTest(root, doInverseBiDiTest, "complex/bidi/inverse");
    addTest(root, TestReorder,"complex/bidi/TestReorder");
    addTest(root, doArabicShapingTest, "complex/arabic-shaping/ArabicShapingTest");
    addTest(root, doLamAlefSpecialVLTRArabicShapingTest, "complex/arabic-shaping/lamalef");
    addTest(root, doTashkeelSpecialVLTRArabicShapingTest, "complex/arabic-shaping/tashkeel");
    addTest(root, doLOGICALArabicDeShapingTest, "complex/arabic-shaping/unshaping");
}

/* verify that the exemplar characters have the expected bidi classes */
static void
charFromDirPropTest(void) {
    int32_t i;

    initCharFromDirProps();

    for(i=0; i<U_CHAR_DIRECTION_COUNT; ++i) {
        if(u_charDirection(charFromDirProp[i])!=(UCharDirection)i) {
            log_err("u_charDirection(charFromDirProp[%d]=U+%04x)==%d!=%d\n",
                    i, charFromDirProp[i], u_charDirection(charFromDirProp[i]), i);
        }
    }
}

static void
doBiDiTest() {
    UBiDi *pBiDi, *pLine=NULL;
    UErrorCode errorCode=U_ZERO_ERROR;

    log_verbose("*** bidi regression test ***\n");

    pBiDi=ubidi_openSized(MAX_STRING_LENGTH, 0, &errorCode);
    if(pBiDi!=NULL) {
        pLine=ubidi_open();
        if(pLine!=NULL) {
            doTests(pBiDi, pLine, FALSE);
			doTests(pBiDi, pLine, TRUE);
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
doTests(UBiDi *pBiDi, UBiDi *pLine, UBool countRunsFirst) {
    int i;
    UChar *s;
    UErrorCode errorCode;
    int32_t lineStart;
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
                doTest(pBiDi, i, tests+i, 0, countRunsFirst);
            } else {
                ubidi_setLine(pBiDi, lineStart, tests[i].lineLimit, pLine, &errorCode);
                if(U_SUCCESS(errorCode)) {
                    log_verbose("ubidi_setLine(%d, %d) ok, direction %d paraLevel=%d\n",
                            lineStart, tests[i].lineLimit, ubidi_getDirection(pLine), ubidi_getParaLevel(pLine));
                    doTest(pLine, i, tests+i, lineStart, countRunsFirst);
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
static void TestReorder(){
    static const char* const logicalOrder[] ={
            "DEL(\\u062F\\u0625)ADD(\\u062F.\\u0625.\\u200F)",
            "DEL(\\u0645\\u0627\\u064A\\u0648) ADD(\\u0623\\u064A\\u0627\\u0631)",
            "DEL(\\u0644\\u0644)ADD(\\u0644.\\u0644.\\u0029\\u0644)\\u0644.\\u200F",
            "DEL(\\u0631\\u064A)ADD(\\u0631.\\u064A.) \\u0631.\\u064A.\\u200F",
            "DAY  2  \\u0646  \\u0627\\u0644\\u0627\\u062B\\u0646\\u064A\\u0646 DAYABBR",
            "DAY  3  \\u062B  \\u0627\\u0644\\u062B\\u0644\\u0627\\u062B\\u0627\\u0621 DAYABBR",     
            "DAY  4   \\u0631  \\u0627\\u0644\\u0623\\u0631\\u0628\\u0639\\u0627\\u0621 DAYABBR",   
            "DAY  5  \\u062E  \\u0627\\u0644\\u062E\\u0645\\u064A\\u0633  DAYABBR",   
            "DAY  6   \\u062C  \\u0627\\u0644\\u062C\\u0645\\u0639\\u0629    DAYABBR", 
            "DAY  7  \\u0633  \\u0627\\u0644\\u0633\\u0628\\u062A  DAYABBR",
            "HELLO\\u0627\\u0644\\u0633\\u0628\\u062A",
    };
    static const char* const visualOrder[]={
            "DEL(\\u0625\\u062F)ADD(\\u200F.\\u0625.\\u062F)",
            "DEL(\\u0648\\u064A\\u0627\\u0645) ADD(\\u0631\\u0627\\u064A\\u0623)",
            "DEL(\\u0644\\u0644)ADD(\\u0644\\u0029.\\u0644.\\u0644)\\u200F.\\u0644",
            /* I am doutful about this...
             * what I would expect is :
             * DEL(\\u064A\\u0631)ADD(.\\u064A.\\u0631) \\u200F.\\u064A.\\u0631
             */
            "DEL(\\u064A\\u0631)ADD(\\u200F.\\u064A.\\u0631 (.\\u064A.\\u0631",
            "DAY  2  \\u0646\\u064A\\u0646\\u062B\\u0627\\u0644\\u0627  \\u0646 DAYABBR",
            "DAY  3  \\u0621\\u0627\\u062B\\u0627\\u0644\\u062B\\u0644\\u0627  \\u062B DAYABBR",     
            "DAY  4   \\u0621\\u0627\\u0639\\u0628\\u0631\\u0623\\u0644\\u0627  \\u0631 DAYABBR",   
            "DAY  5  \\u0633\\u064A\\u0645\\u062E\\u0644\\u0627  \\u062E  DAYABBR",   
            "DAY  6   \\u0629\\u0639\\u0645\\u062C\\u0644\\u0627  \\u062C    DAYABBR", 
            "DAY  7  \\u062A\\u0628\\u0633\\u0644\\u0627  \\u0633  DAYABBR",
            "HELLO\\u062A\\u0628\\u0633\\u0644\\u0627",
    };
    static const char* const visualOrder1[]={
            ")\\u062F.\\u0625.\\u200F(DDA)\\u062F\\u0625(LED",
            ")\\u0623\\u064A\\u0627\\u0631(DDA )\\u0645\\u0627\\u064A\\u0648(LED",
            "\\u0644.\\u0644.(\\u0644(\\u0644.\\u200F(DDA)\\u0644\\u0644(LED", 
            "\\u0631.\\u064A.( \\u0631.\\u064A.\\u200F(DDA)\\u0631\\u064A(LED",
            "RBBAYAD \\u0646  \\u0627\\u0644\\u0627\\u062B\\u0646\\u064A\\u0646  2  YAD",
            "RBBAYAD \\u062B  \\u0627\\u0644\\u062B\\u0644\\u0627\\u062B\\u0627\\u0621  3  YAD", 
            "RBBAYAD \\u0631  \\u0627\\u0644\\u0623\\u0631\\u0628\\u0639\\u0627\\u0621   4  YAD",
            "RBBAYAD  \\u062E  \\u0627\\u0644\\u062E\\u0645\\u064A\\u0633  5  YAD",
            "RBBAYAD    \\u062C  \\u0627\\u0644\\u062C\\u0645\\u0639\\u0629   6  YAD",
            "RBBAYAD  \\u0633  \\u0627\\u0644\\u0633\\u0628\\u062A  7  YAD",
            "\\u0627\\u0644\\u0633\\u0628\\u062AOLLEH",
    };

    static const char* const visualOrder2[]={
            "\\u200E)\\u200E\\u062F.\\u0625.\\u200F\\u200E(DDA)\\u200E\\u062F\\u0625\\u200E(LED",
            "\\u200E)\\u200E\\u0623\\u064A\\u0627\\u0631\\u200E(DDA )\\u200E\\u0645\\u0627\\u064A\\u0648\\u200E(LED",
            "\\u0644.\\u0644.)\\u0644)\\u0644.\\u200F\\u200E(DDA)\\u200E\\u0644\\u0644\\u200E(LED",
            "\\u0631.\\u064A.) \\u0631.\\u064A.\\u200F\\u200E(DDA)\\u200E\\u0631\\u064A\\u200E(LED",
            "RBBAYAD \\u200E\\u0646  \\u0627\\u0644\\u0627\\u062B\\u0646\\u064A\\u0646\\u200E  2  YAD",
            "RBBAYAD \\u200E\\u062B  \\u0627\\u0644\\u062B\\u0644\\u0627\\u062B\\u0627\\u0621\\u200E  3  YAD",
            "RBBAYAD \\u200E\\u0631  \\u0627\\u0644\\u0623\\u0631\\u0628\\u0639\\u0627\\u0621\\u200E   4  YAD",
            "RBBAYAD  \\u200E\\u062E  \\u0627\\u0644\\u062E\\u0645\\u064A\\u0633\\u200E  5  YAD",
            "RBBAYAD    \\u200E\\u062C  \\u0627\\u0644\\u062C\\u0645\\u0639\\u0629\\u200E   6  YAD",
            "RBBAYAD  \\u200E\\u0633  \\u0627\\u0644\\u0633\\u0628\\u062A\\u200E  7  YAD",
            "\\u0627\\u0644\\u0633\\u0628\\u062AOLLEH",
    };
    static const char* const visualOrder3[]={
            ")\\u062F.\\u0625.\\u200F(DDA)\\u062F\\u0625(LED",
            ")\\u0623\\u064A\\u0627\\u0631(DDA )\\u0645\\u0627\\u064A\\u0648(LED",
            "\\u0644.\\u0644.)\\u0644)\\u0644.\\u200F(\\u0644\\u0644)DDA(LED",
            "\\u0631.\\u064A.) \\u0631.\\u064A.\\u200F(\\u0631\\u064A)DDA(LED",
            "RBBAYAD \\u0627\\u0644\\u0627\\u062B\\u0646\\u064A\\u0646   \\u0646  2 YAD",
            "RBBAYAD \\u0627\\u0644\\u062B\\u0644\\u0627\\u062B\\u0627\\u0621   \\u062B  3 YAD",
            "RBBAYAD \\u0627\\u0644\\u0623\\u0631\\u0628\\u0639\\u0627\\u0621     \\u0631 4 YAD",
            "RBBAYAD  \\u0627\\u0644\\u062E\\u0645\\u064A\\u0633   \\u062E  5 YAD",
            "RBBAYAD    \\u0627\\u0644\\u062C\\u0645\\u0639\\u0629     \\u062C",
            "RBBAYAD  \\u0627\\u0644\\u0633\\u0628\\u062A   \\u0633  7 YAD",
            "\\u0627\\u0644\\u0633\\u0628\\u062AOLLEH"
    };
    static const char* const visualOrder4[]={
            "DEL(ADD(\\u0625\\u062F(.\\u0625.\\u062F)",
            "DEL( (\\u0648\\u064A\\u0627\\u0645ADD(\\u0631\\u0627\\u064A\\u0623)",
            "DEL(ADD(\\u0644\\u0644(.\\u0644(\\u0644(.\\u0644.\\u0644",
            "DEL(ADD(\\u064A\\u0631(.\\u064A.\\u0631 (.\\u064A.\\u0631",
            "DAY 2  \\u0646   \\u0646\\u064A\\u0646\\u062B\\u0627\\u0644\\u0627 DAYABBR",
            "DAY 3  \\u062B   \\u0621\\u0627\\u062B\\u0627\\u0644\\u062B\\u0644\\u0627 DAYABBR",
            "DAY 4 \\u0631     \\u0621\\u0627\\u0639\\u0628\\u0631\\u0623\\u0644\\u0627 DAYABBR",
            "DAY 5  \\u062E   \\u0633\\u064A\\u0645\\u062E\\u0644\\u0627  DAYABBR",
            "DAY 6 \\u062C     \\u0629\\u0639\\u0645\\u062C\\u0644\\u0627    DAYABBR",
            "DAY 7  \\u0633   \\u062A\\u0628\\u0633\\u0644\\u0627  DAYABBR ",
            "HELLO\\u062A\\u0628\\u0633\\u0644\\u0627"
    };
    UErrorCode ec = U_ZERO_ERROR;
    UBiDi* bidi = ubidi_open();
    int i=0;
    for(;i<(sizeof(logicalOrder)/sizeof(logicalOrder[0]));i++){
        int32_t srcSize = (int32_t)uprv_strlen(logicalOrder[i]);
        int32_t destSize = srcSize*2;
        UChar* src = (UChar*) malloc(sizeof(UChar)*srcSize );
        UChar* dest = (UChar*) malloc(sizeof(UChar)*destSize);
        char* chars=NULL;
        ec = U_ZERO_ERROR;
        u_unescape(logicalOrder[i],src,srcSize);
        srcSize= u_strlen(src);
        ubidi_setPara(bidi,src,srcSize,UBIDI_DEFAULT_LTR ,NULL,&ec);
        if(U_FAILURE(ec)){
            log_err("ubidi_setPara(tests[%d], paraLevel %d) failed with errorCode %s\n",
                    i, UBIDI_DEFAULT_LTR, u_errorName(ec));
        }
        /* try pre-flighting */
        destSize = ubidi_writeReordered(bidi,dest,0,UBIDI_DO_MIRRORING,&ec);
        if(ec!=U_BUFFER_OVERFLOW_ERROR){
            log_err("Pre-flighting did not give expected error: Expected: U_BUFFER_OVERFLOW_ERROR. Got: %s \n",u_errorName(ec));
        }else if(destSize!=srcSize){
            log_err("Pre-flighting did not give expected size: Expected: %d. Got: %d \n",srcSize,destSize);
        }else{
            ec= U_ZERO_ERROR;
        }
        destSize=ubidi_writeReordered(bidi,dest,destSize+1,UBIDI_DO_MIRRORING,&ec);
        chars = aescstrdup(dest,-1);
        if(destSize!=srcSize){
            log_err("ubidi_writeReordered() destSize and srcSize do not match\n");
        }else if(uprv_strncmp(visualOrder[i],chars,destSize)!=0){
            log_err("ubidi_writeReordered() did not give expected results. Expected: %s Got: %s At Index: %d\n",visualOrder[i],chars,i);

            
        }
        free(src);
        free(dest);
    }
    
    for(i=0;i<(sizeof(logicalOrder)/sizeof(logicalOrder[0]));i++){
        int32_t srcSize = (int32_t)uprv_strlen(logicalOrder[i]);
        int32_t destSize = srcSize*2;
        UChar* src = (UChar*) malloc(sizeof(UChar)*srcSize );
        UChar* dest = (UChar*) malloc(sizeof(UChar)*destSize);
        char* chars=NULL;
        ec = U_ZERO_ERROR;
        u_unescape(logicalOrder[i],src,srcSize);
        srcSize=u_strlen(src);
        ubidi_setPara(bidi,src,srcSize,UBIDI_DEFAULT_LTR ,NULL,&ec);
        if(U_FAILURE(ec)){
            log_err("ubidi_setPara(tests[%d], paraLevel %d) failed with errorCode %s\n",
                    i, UBIDI_DEFAULT_LTR, u_errorName(ec));
        }
        /* try pre-flighting */
        destSize = ubidi_writeReordered(bidi,dest,0,UBIDI_DO_MIRRORING+UBIDI_OUTPUT_REVERSE,&ec);
        if(ec!=U_BUFFER_OVERFLOW_ERROR){
            log_err("Pre-flighting did not give expected error: Expected: U_BUFFER_OVERFLOW_ERROR. Got: %s \n",u_errorName(ec));
        }else if(destSize!=srcSize){
            log_err("Pre-flighting did not give expected size: Expected: %d. Got: %d \n",srcSize,destSize);
        }else{
            ec= U_ZERO_ERROR;
        }
        destSize=ubidi_writeReordered(bidi,dest,destSize+1,UBIDI_DO_MIRRORING+UBIDI_OUTPUT_REVERSE,&ec);
        chars = aescstrdup(dest,destSize);
        if(destSize!=srcSize){
            log_err("ubidi_writeReordered() destSize and srcSize do not match\n");
        }else if(uprv_strncmp(visualOrder1[i],chars,destSize)!=0){
            log_err("ubidi_writeReordered() did not give expected results for UBIDI_DO_MIRRORING+UBIDI_OUTPUT_REVERSE. Expected: %s Got: %s At Index: %d\n",visualOrder[i],chars,i);

            
        }
        
        free(src);
        free(dest);
    }

    for(i=0;i<(sizeof(logicalOrder)/sizeof(logicalOrder[0]));i++){
        int32_t srcSize = (int32_t)uprv_strlen(logicalOrder[i]);
        int32_t destSize = srcSize*2;
        UChar* src = (UChar*) malloc(sizeof(UChar)*srcSize );
        UChar* dest = (UChar*) malloc(sizeof(UChar)*destSize);
        char* chars=NULL;
        ec = U_ZERO_ERROR;
        u_unescape(logicalOrder[i],src,srcSize);
        srcSize=u_strlen(src);
        ubidi_setInverse(bidi,TRUE);
        ubidi_setPara(bidi,src,srcSize,UBIDI_DEFAULT_LTR ,NULL,&ec);

        if(U_FAILURE(ec)){
            log_err("ubidi_setPara(tests[%d], paraLevel %d) failed with errorCode %s\n",
                    i, UBIDI_DEFAULT_LTR, u_errorName(ec));
        }
                /* try pre-flighting */
        destSize = ubidi_writeReordered(bidi,dest,0,UBIDI_INSERT_LRM_FOR_NUMERIC+UBIDI_OUTPUT_REVERSE,&ec);
        if(ec!=U_BUFFER_OVERFLOW_ERROR){
            log_err("Pre-flighting did not give expected error: Expected: U_BUFFER_OVERFLOW_ERROR. Got: %s \n",u_errorName(ec));
        }else{
            ec= U_ZERO_ERROR;
        }
        destSize=ubidi_writeReordered(bidi,dest,destSize+1,UBIDI_INSERT_LRM_FOR_NUMERIC+UBIDI_OUTPUT_REVERSE,&ec);
        chars = aescstrdup(dest,destSize);

        /*if(destSize!=srcSize){
            log_err("ubidi_writeReordered() destSize and srcSize do not match. Dest Size = %d Source Size = %d\n",destSize,srcSize );
        }else*/
            if(uprv_strncmp(visualOrder2[i],chars,destSize)!=0){
            log_err("ubidi_writeReordered() did not give expected results for UBIDI_INSERT_LRM_FOR_NUMERIC+UBIDI_OUTPUT_REVERSE. Expected: %s Got: %s At Index: %d\n",visualOrder[i],chars,i);

            
        }
        
        free(src);
        free(dest);
    }
        /* Max Explicit level */
    for(i=0;i<(sizeof(logicalOrder)/sizeof(logicalOrder[0]));i++){
        int32_t srcSize = (int32_t)uprv_strlen(logicalOrder[i]);
        int32_t destSize = srcSize*2;
        UChar* src = (UChar*) malloc(sizeof(UChar)*srcSize );
        UChar* dest = (UChar*) malloc(sizeof(UChar)*destSize);
        char* chars=NULL;
        UBiDiLevel levels[UBIDI_MAX_EXPLICIT_LEVEL]={1,2,3,4,5,6,7,8,9,10};
        ec = U_ZERO_ERROR;
        u_unescape(logicalOrder[i],src,srcSize);
        srcSize=u_strlen(src);
        ubidi_setPara(bidi,src,srcSize,UBIDI_DEFAULT_LTR,levels,&ec);
        if(U_FAILURE(ec)){
            log_err("ubidi_setPara(tests[%d], paraLevel %d) failed with errorCode %s\n",
                    i, UBIDI_MAX_EXPLICIT_LEVEL, u_errorName(ec));
        }
                /* try pre-flighting */
        destSize = ubidi_writeReordered(bidi,dest,0,UBIDI_OUTPUT_REVERSE,&ec);
        if(ec!=U_BUFFER_OVERFLOW_ERROR){
            log_err("Pre-flighting did not give expected error: Expected: U_BUFFER_OVERFLOW_ERROR. Got: %s \n",u_errorName(ec));
        }else if(destSize!=srcSize){
            log_err("Pre-flighting did not give expected size: Expected: %d. Got: %d \n",srcSize,destSize);
        }else{
            ec= U_ZERO_ERROR;
        }
        destSize=ubidi_writeReordered(bidi,dest,destSize+1,UBIDI_OUTPUT_REVERSE,&ec);
        chars = aescstrdup(dest,destSize);

        if(destSize!=srcSize){
            log_err("ubidi_writeReordered() destSize and srcSize do not match. Dest Size = %d Source Size = %d\n",destSize,srcSize );
        }else if(uprv_strncmp(visualOrder3[i],chars,destSize)!=0){
            log_err("ubidi_writeReordered() did not give expected results for UBIDI_OUTPUT_REVERSE. Expected: %s Got: %s At Index: %d\n",visualOrder[i],chars,i);

            
        }
        
        free(src);
        free(dest);
    }
    for(i=0;i<(sizeof(logicalOrder)/sizeof(logicalOrder[0]));i++){
        int32_t srcSize = (int32_t)uprv_strlen(logicalOrder[i]);
        int32_t destSize = srcSize*2;
        UChar* src = (UChar*) malloc(sizeof(UChar)*srcSize );
        UChar* dest = (UChar*) malloc(sizeof(UChar)*destSize);
        char* chars=NULL;
        UBiDiLevel levels[UBIDI_MAX_EXPLICIT_LEVEL]={1,2,3,4,5,6,7,8,9,10};
        ec = U_ZERO_ERROR;
        u_unescape(logicalOrder[i],src,srcSize);
        srcSize=u_strlen(src);
        ubidi_setPara(bidi,src,srcSize,UBIDI_DEFAULT_LTR,levels,&ec);
        if(U_FAILURE(ec)){
            log_err("ubidi_setPara(tests[%d], paraLevel %d) failed with errorCode %s\n",
                    i, UBIDI_MAX_EXPLICIT_LEVEL, u_errorName(ec));
        }
        
        /* try pre-flighting */
        destSize = ubidi_writeReordered(bidi,dest,0,UBIDI_DO_MIRRORING+UBIDI_REMOVE_BIDI_CONTROLS,&ec);
        if(ec!=U_BUFFER_OVERFLOW_ERROR){
            log_err("Pre-flighting did not give expected error: Expected: U_BUFFER_OVERFLOW_ERROR. Got: %s \n",u_errorName(ec));
        /*}else if(destSize!=srcSize){
            log_err("Pre-flighting did not give expected size: Expected: %d. Got: %d \n",srcSize,destSize);*/
        }else{
            ec= U_ZERO_ERROR;
        }
        destSize=ubidi_writeReordered(bidi,dest,destSize+1,UBIDI_DO_MIRRORING+UBIDI_REMOVE_BIDI_CONTROLS,&ec);
        chars = aescstrdup(dest,destSize);

        /*if(destSize!=srcSize){
            log_err("ubidi_writeReordered() destSize and srcSize do not match. Dest Size = %d Source Size = %d\n",destSize,srcSize );
        }else*/ if(uprv_strncmp(visualOrder4[i],chars,destSize)!=0){
            log_err("ubidi_writeReordered() did not give expected results for UBIDI_DO_MIRRORING+UBIDI_REMOVE_BIDI_CONTROLS. Expected: %s Got: %s At Index: %d\n",visualOrder[i],chars,i);
        }
        
        free(src);
        free(dest);
    }
    ubidi_close(bidi);
}

static void
doTest(UBiDi *pBiDi, int testNumber, BiDiTestData *test, int32_t lineStart, UBool countRunsFirst) {
    const uint8_t *dirProps=test->text+lineStart;
    const UBiDiLevel *levels=test->levels;
    const uint8_t *visualMap=test->visualMap;
    int32_t i, len=ubidi_getLength(pBiDi), logicalIndex, runCount = 0;
    UErrorCode errorCode=U_ZERO_ERROR;
    UBiDiLevel level, level2;

	if (countRunsFirst) {
		log_verbose("Calling ubidi_countRuns() first.\n");

		runCount = ubidi_countRuns(pBiDi, &errorCode);

		if(U_FAILURE(errorCode)) {
			log_err("ubidi_countRuns(tests[%d]): error %s\n", testNumber, myErrorName(errorCode));
			return;
		}
	} else {
		log_verbose("Calling ubidi_getLogicalMap() first.\n");
	}

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

	if (! countRunsFirst) {
		runCount=ubidi_countRuns(pBiDi, &errorCode);
		if(U_FAILURE(errorCode)) {
			log_err("ubidi_countRuns(tests[%d]): error %s\n", testNumber, myErrorName(errorCode));
			return;
		}
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
    int32_t
        logicalMap1[200], logicalMap2[200], logicalMap3[200],
        visualMap1[200], visualMap2[200], visualMap3[200], visualMap4[200];
    UErrorCode errorCode=U_ZERO_ERROR;
    UBiDiLevel levels[200];
    int32_t i, length=ubidi_getLength(pBiDi);
    int32_t runCount, visualIndex, logicalStart, runLength;
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
    uprv_memcpy(levels, ubidi_getLevels(pBiDi, &errorCode), length);

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
        odd=(UBool)(ubidi_getVisualRun(pBiDi, i, &logicalStart, &runLength));
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

static void
doInverseBiDiTest() {
    UBiDi *pBiDi;
    UErrorCode errorCode;
    int i;

    pBiDi=ubidi_open();
    if(pBiDi==NULL) {
        log_err("unable to open a UBiDi object (out of memory)\n");
        return;
    }

    log_verbose("inverse BiDi: testInverseBiDi(L) with %u test cases ---\n", LENGTHOF(testCases));
    for(i=0; i<LENGTHOF(testCases); ++i) {
        errorCode=U_ZERO_ERROR;
        testInverseBiDi(pBiDi, testCases[i].s, testCases[i].length, 0, &errorCode);
    }

    log_verbose("inverse BiDi: testInverseBiDi(R) with %u test cases ---\n", LENGTHOF(testCases));
    for(i=0; i<LENGTHOF(testCases); ++i) {
        errorCode=U_ZERO_ERROR;
        testInverseBiDi(pBiDi, testCases[i].s, testCases[i].length, 1, &errorCode);
    }

    testManyInverseBiDi(pBiDi, 0);
    testManyInverseBiDi(pBiDi, 1);

    ubidi_close(pBiDi);

    log_verbose("inverse BiDi: rountrips: %5u\nnon-roundtrips: %5u\n", countRoundtrips, countNonRoundtrips);

    testWriteReverse();
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

    log_verbose("inverse BiDi: testManyInverseBiDi(%c) - test permutations of text snippets ---\n", direction==0 ? 'L' : 'R');
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
        logicalLength=ubidi_writeReordered(pBiDi, logicalDest, LENGTHOF(logicalDest),
                                           UBIDI_DO_MIRRORING|UBIDI_INSERT_LRM_FOR_NUMERIC, pErrorCode);
        log_verbose("  v ");
        printUnicode(src, srcLength, ubidi_getLevels(pBiDi, pErrorCode));
        log_verbose("\n");

        /* convert back to visual LTR */
        ubidi_setInverse(pBiDi, FALSE);
        ubidi_setPara(pBiDi, logicalDest, logicalLength, 0, NULL, pErrorCode);
        visualLength=ubidi_writeReordered(pBiDi, visualDest, LENGTHOF(visualDest),
                                          UBIDI_DO_MIRRORING|UBIDI_REMOVE_BIDI_CONTROLS, pErrorCode);
    } else {
        log_verbose("inverse BiDi: testInverseBiDi(R)\n");

        /* reverse visual from RTL to LTR */
        ltrLength=ubidi_writeReverse(src, srcLength, visualLTR, LENGTHOF(visualLTR), 0, pErrorCode);
        log_verbose("  vr");
        printUnicode(src, srcLength, NULL);
        log_verbose("\n");

        /* convert visual RTL to logical */
        ubidi_setInverse(pBiDi, TRUE);
        ubidi_setPara(pBiDi, visualLTR, ltrLength, 0, NULL, pErrorCode);
        logicalLength=ubidi_writeReordered(pBiDi, logicalDest, LENGTHOF(logicalDest),
                                           UBIDI_DO_MIRRORING|UBIDI_INSERT_LRM_FOR_NUMERIC, pErrorCode);
        log_verbose("  vl");
        printUnicode(visualLTR, ltrLength, ubidi_getLevels(pBiDi, pErrorCode));
        log_verbose("\n");

        /* convert back to visual RTL */
        ubidi_setInverse(pBiDi, FALSE);
        ubidi_setPara(pBiDi, logicalDest, logicalLength, 0, NULL, pErrorCode);
        visualLength=ubidi_writeReordered(pBiDi, visualDest, LENGTHOF(visualDest),
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
    } else if(srcLength==visualLength && uprv_memcmp(src, visualDest, srcLength*U_SIZEOF_UCHAR)==0) {
        ++countRoundtrips;
        log_verbose(" + roundtripped\n");
    } else {
        ++countNonRoundtrips;
        log_verbose(" * did not roundtrip\n");
        log_err("inverse BiDi: transformation visual->logical->visual did not roundtrip the text;\n"
                "                 turn on verbose mode to see details\n");
    }
}

static void
testWriteReverse() {
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

    /* test ubidi_writeReverse() with "interesting" options */
    errorCode=U_ZERO_ERROR;
    length=ubidi_writeReverse(forward, LENGTHOF(forward),
                              reverse, LENGTHOF(reverse),
                              UBIDI_KEEP_BASE_COMBINING,
                              &errorCode);
    if(U_FAILURE(errorCode) || length!=LENGTHOF(reverseKeepCombining) || uprv_memcmp(reverse, reverseKeepCombining, length*U_SIZEOF_UCHAR)!=0) {
        log_err("failure in ubidi_writeReverse(UBIDI_KEEP_BASE_COMBINING): length=%d (should be %d), error code %s\n",
                length, LENGTHOF(reverseKeepCombining), u_errorName(errorCode));
    }

    uprv_memset(reverse, 0xa5, LENGTHOF(reverse)*U_SIZEOF_UCHAR);
    errorCode=U_ZERO_ERROR;
    length=ubidi_writeReverse(forward, LENGTHOF(forward),
                              reverse, LENGTHOF(reverse),
                              UBIDI_REMOVE_BIDI_CONTROLS|UBIDI_DO_MIRRORING|UBIDI_KEEP_BASE_COMBINING,
                              &errorCode);
    if(U_FAILURE(errorCode) || length!=LENGTHOF(reverseRemoveControlsKeepCombiningDoMirror) || uprv_memcmp(reverse, reverseRemoveControlsKeepCombiningDoMirror, length*U_SIZEOF_UCHAR)!=0) {
        log_err("failure in ubidi_writeReverse(UBIDI_REMOVE_BIDI_CONTROLS|UBIDI_DO_MIRRORING|UBIDI_KEEP_BASE_COMBINING):\n"
                "    length=%d (should be %d), error code %s\n",
                length, LENGTHOF(reverseRemoveControlsKeepCombiningDoMirror), u_errorName(errorCode));
    }
}

/* arabic shaping ----------------------------------------------------------- */

static void
doArabicShapingTest() {
    static const UChar
    source[]={
        0x31,   /* en:1 */
        0x627,  /* arabic:alef */
        0x32,   /* en:2 */
        0x6f3,  /* an:3 */
        0x61,   /* latin:a */
        0x34,   /* en:4 */
        0
    }, en2an[]={
        0x661, 0x627, 0x662, 0x6f3, 0x61, 0x664, 0
    }, an2en[]={
        0x31, 0x627, 0x32, 0x33, 0x61, 0x34, 0
    }, logical_alen2an_init_lr[]={
        0x31, 0x627, 0x662, 0x6f3, 0x61, 0x34, 0
    }, logical_alen2an_init_al[]={
        0x6f1, 0x627, 0x6f2, 0x6f3, 0x61, 0x34, 0
    }, reverse_alen2an_init_lr[]={
        0x661, 0x627, 0x32, 0x6f3, 0x61, 0x34, 0
    }, reverse_alen2an_init_al[]={
        0x6f1, 0x627, 0x32, 0x6f3, 0x61, 0x6f4, 0
    };
    UChar dest[8];
    UErrorCode errorCode;
    int32_t length;

    /* test number shaping */

    /* european->arabic */
    errorCode=U_ZERO_ERROR;
    length=u_shapeArabic(source, LENGTHOF(source),
                         dest, LENGTHOF(dest),
                         U_SHAPE_DIGITS_EN2AN|U_SHAPE_DIGIT_TYPE_AN,
                         &errorCode);
    if(U_FAILURE(errorCode) || length!=LENGTHOF(source) || uprv_memcmp(dest, en2an, length*U_SIZEOF_UCHAR)!=0) {
        log_err("failure in u_shapeArabic(en2an)\n");
    }

    /* arabic->european */
    errorCode=U_ZERO_ERROR;
    length=u_shapeArabic(source, -1,
                         dest, LENGTHOF(dest),
                         U_SHAPE_DIGITS_AN2EN|U_SHAPE_DIGIT_TYPE_AN_EXTENDED,
                         &errorCode);
    if(U_FAILURE(errorCode) || length!=u_strlen(source) || uprv_memcmp(dest, an2en, length*U_SIZEOF_UCHAR)!=0) {
        log_err("failure in u_shapeArabic(an2en)\n");
    }

    /* european->arabic with context, logical order, initial state not AL */
    errorCode=U_ZERO_ERROR;
    length=u_shapeArabic(source, LENGTHOF(source),
                         dest, LENGTHOF(dest),
                         U_SHAPE_DIGITS_ALEN2AN_INIT_LR|U_SHAPE_DIGIT_TYPE_AN,
                         &errorCode);
    if(U_FAILURE(errorCode) || length!=LENGTHOF(source) || uprv_memcmp(dest, logical_alen2an_init_lr, length*U_SIZEOF_UCHAR)!=0) {
        log_err("failure in u_shapeArabic(logical_alen2an_init_lr)\n");
    }

    /* european->arabic with context, logical order, initial state AL */
    errorCode=U_ZERO_ERROR;
    length=u_shapeArabic(source, LENGTHOF(source),
                         dest, LENGTHOF(dest),
                         U_SHAPE_DIGITS_ALEN2AN_INIT_AL|U_SHAPE_DIGIT_TYPE_AN_EXTENDED,
                         &errorCode);
    if(U_FAILURE(errorCode) || length!=LENGTHOF(source) || uprv_memcmp(dest, logical_alen2an_init_al, length*U_SIZEOF_UCHAR)!=0) {
        log_err("failure in u_shapeArabic(logical_alen2an_init_al)\n");
    }

    /* european->arabic with context, reverse order, initial state not AL */
    errorCode=U_ZERO_ERROR;
    length=u_shapeArabic(source, LENGTHOF(source),
                         dest, LENGTHOF(dest),
                         U_SHAPE_DIGITS_ALEN2AN_INIT_LR|U_SHAPE_DIGIT_TYPE_AN|U_SHAPE_TEXT_DIRECTION_VISUAL_LTR,
                         &errorCode);
    if(U_FAILURE(errorCode) || length!=LENGTHOF(source) || uprv_memcmp(dest, reverse_alen2an_init_lr, length*U_SIZEOF_UCHAR)!=0) {
        log_err("failure in u_shapeArabic(reverse_alen2an_init_lr)\n");
    }

    /* european->arabic with context, reverse order, initial state AL */
    errorCode=U_ZERO_ERROR;
    length=u_shapeArabic(source, LENGTHOF(source),
                         dest, LENGTHOF(dest),
                         U_SHAPE_DIGITS_ALEN2AN_INIT_AL|U_SHAPE_DIGIT_TYPE_AN_EXTENDED|U_SHAPE_TEXT_DIRECTION_VISUAL_LTR,
                         &errorCode);
    if(U_FAILURE(errorCode) || length!=LENGTHOF(source) || uprv_memcmp(dest, reverse_alen2an_init_al, length*U_SIZEOF_UCHAR)!=0) {
        log_err("failure in u_shapeArabic(reverse_alen2an_init_al)\n");
    }

    /* test noop */
    errorCode=U_ZERO_ERROR;
    length=u_shapeArabic(source, LENGTHOF(source),
                         dest, LENGTHOF(dest),
                         0,
                         &errorCode);
    if(U_FAILURE(errorCode) || length!=LENGTHOF(source) || uprv_memcmp(dest, source, length*U_SIZEOF_UCHAR)!=0) {
        log_err("failure in u_shapeArabic(noop)\n");
    }

    errorCode=U_ZERO_ERROR;
    length=u_shapeArabic(source, 0,
                         dest, LENGTHOF(dest),
                         U_SHAPE_DIGITS_EN2AN|U_SHAPE_DIGIT_TYPE_AN,
                         &errorCode);
    if(U_FAILURE(errorCode) || length!=0) {
        log_err("failure in u_shapeArabic(en2an, sourceLength=0), returned %d/%s\n", u_errorName(errorCode), LENGTHOF(source));
    }

    /* preflight digit shaping */
    errorCode=U_ZERO_ERROR;
    length=u_shapeArabic(source, LENGTHOF(source),
                         NULL, 0,
                         U_SHAPE_DIGITS_EN2AN|U_SHAPE_DIGIT_TYPE_AN,
                         &errorCode);
    if(errorCode!=U_BUFFER_OVERFLOW_ERROR || length!=LENGTHOF(source)) {
        log_err("failure in u_shapeArabic(en2an preflighting), returned %d/%s instead of %d/U_BUFFER_OVERFLOW_ERROR\n",
                length, u_errorName(errorCode), LENGTHOF(source));
    }

    /* test illegal arguments */
    errorCode=U_ZERO_ERROR;
    length=u_shapeArabic(NULL, LENGTHOF(source),
                         dest, LENGTHOF(dest),
                         U_SHAPE_DIGITS_EN2AN|U_SHAPE_DIGIT_TYPE_AN,
                         &errorCode);
    if(errorCode!=U_ILLEGAL_ARGUMENT_ERROR) {
        log_err("failure in u_shapeArabic(source=NULL), returned %s instead of U_ILLEGAL_ARGUMENT_ERROR\n", u_errorName(errorCode));
    }

    errorCode=U_ZERO_ERROR;
    length=u_shapeArabic(source, -2,
                         dest, LENGTHOF(dest),
                         U_SHAPE_DIGITS_EN2AN|U_SHAPE_DIGIT_TYPE_AN,
                         &errorCode);
    if(errorCode!=U_ILLEGAL_ARGUMENT_ERROR) {
        log_err("failure in u_shapeArabic(sourceLength=-2), returned %s instead of U_ILLEGAL_ARGUMENT_ERROR\n", u_errorName(errorCode));
    }

    errorCode=U_ZERO_ERROR;
    length=u_shapeArabic(source, LENGTHOF(source),
                         NULL, LENGTHOF(dest),
                         U_SHAPE_DIGITS_EN2AN|U_SHAPE_DIGIT_TYPE_AN,
                         &errorCode);
    if(errorCode!=U_ILLEGAL_ARGUMENT_ERROR) {
        log_err("failure in u_shapeArabic(dest=NULL), returned %s instead of U_ILLEGAL_ARGUMENT_ERROR\n", u_errorName(errorCode));
    }

    errorCode=U_ZERO_ERROR;
    length=u_shapeArabic(source, LENGTHOF(source),
                         dest, -1,
                         U_SHAPE_DIGITS_EN2AN|U_SHAPE_DIGIT_TYPE_AN,
                         &errorCode);
    if(errorCode!=U_ILLEGAL_ARGUMENT_ERROR) {
        log_err("failure in u_shapeArabic(destSize=-1), returned %s instead of U_ILLEGAL_ARGUMENT_ERROR\n", u_errorName(errorCode));
    }

    errorCode=U_ZERO_ERROR;
    length=u_shapeArabic(source, LENGTHOF(source),
                         dest, LENGTHOF(dest),
                         U_SHAPE_DIGITS_RESERVED|U_SHAPE_DIGIT_TYPE_AN,
                         &errorCode);
    if(errorCode!=U_ILLEGAL_ARGUMENT_ERROR) {
        log_err("failure in u_shapeArabic(U_SHAPE_DIGITS_RESERVED), returned %s instead of U_ILLEGAL_ARGUMENT_ERROR\n", u_errorName(errorCode));
    }

    errorCode=U_ZERO_ERROR;
    length=u_shapeArabic(source, LENGTHOF(source),
                         dest, LENGTHOF(dest),
                         U_SHAPE_DIGITS_EN2AN|U_SHAPE_DIGIT_TYPE_RESERVED,
                         &errorCode);
    if(errorCode!=U_ILLEGAL_ARGUMENT_ERROR) {
        log_err("failure in u_shapeArabic(U_SHAPE_DIGIT_TYPE_RESERVED), returned %s instead of U_ILLEGAL_ARGUMENT_ERROR\n", u_errorName(errorCode));
    }

    errorCode=U_ZERO_ERROR;
    length=u_shapeArabic(source, LENGTHOF(source),
                         (UChar *)(source+2), LENGTHOF(dest), /* overlap source and destination */
                         U_SHAPE_DIGITS_EN2AN|U_SHAPE_DIGIT_TYPE_AN,
                         &errorCode);
    if(errorCode!=U_ILLEGAL_ARGUMENT_ERROR) {
        log_err("failure in u_shapeArabic(U_SHAPE_DIGIT_TYPE_RESERVED), returned %s instead of U_ILLEGAL_ARGUMENT_ERROR\n", u_errorName(errorCode));
    }
}

static void
doLamAlefSpecialVLTRArabicShapingTest() {
    static const UChar
    source[]={
/*a*/   0x20 ,0x646,0x622,0x644,0x627,0x20,
/*b*/   0x646,0x623,0x64E,0x644,0x627,0x20,
/*c*/   0x646,0x627,0x670,0x644,0x627,0x20,
/*d*/   0x646,0x622,0x653,0x644,0x627,0x20,
/*e*/   0x646,0x625,0x655,0x644,0x627,0x20,
/*f*/   0x646,0x622,0x654,0x644,0x627,0x20,
/*g*/   0xFEFC,0x639
    }, shape_near[]={
        0x20,0xfee5,0x20,0xfef5,0xfe8d,0x20,0xfee5,0x20,0xfe76,0xfef7,0xfe8d,0x20,
        0xfee5,0x20,0x670,0xfefb,0xfe8d,0x20,0xfee5,0x20,0x653,0xfef5,0xfe8d,0x20,
        0xfee5,0x20,0x655,0xfef9,0xfe8d,0x20,0xfee5,0x20,0x654,0xfef5,0xfe8d,0x20,
        0xfefc,0xfecb
    }, shape_at_end[]={
        0x20,0xfee5,0xfef5,0xfe8d,0x20,0xfee5,0xfe76,0xfef7,0xfe8d,0x20,0xfee5,0x670,
        0xfefb,0xfe8d,0x20,0xfee5,0x653,0xfef5,0xfe8d,0x20,0xfee5,0x655,0xfef9,0xfe8d,
        0x20,0xfee5,0x654,0xfef5,0xfe8d,0x20,0xfefc,0xfecb,0x20,0x20,0x20,0x20,0x20,0x20
    }, shape_at_begin[]={
        0x20,0x20,0x20,0x20,0x20,0x20,0x20,0xfee5,0xfef5,0xfe8d,0x20,0xfee5,0xfe76,
        0xfef7,0xfe8d,0x20,0xfee5,0x670,0xfefb,0xfe8d,0x20,0xfee5,0x653,0xfef5,0xfe8d,
        0x20,0xfee5,0x655,0xfef9,0xfe8d,0x20,0xfee5,0x654,0xfef5,0xfe8d,0x20,0xfefc,0xfecb
    }, shape_grow_shrink[]={
        0x20,0xfee5,0xfef5,0xfe8d,0x20,0xfee5,0xfe76,0xfef7,0xfe8d,0x20,0xfee5,
        0x670,0xfefb,0xfe8d,0x20,0xfee5,0x653,0xfef5,0xfe8d,0x20,0xfee5,0x655,0xfef9,
        0xfe8d,0x20,0xfee5,0x654,0xfef5,0xfe8d,0x20,0xfefc,0xfecb
    }, shape_excepttashkeel_near[]={
        0x20,0xfee5,0x20,0xfef5,0xfe8d,0x20,0xfee5,0x20,0xfe76,0xfef7,0xfe8d,0x20,
        0xfee5,0x20,0x670,0xfefb,0xfe8d,0x20,0xfee5,0x20,0x653,0xfef5,0xfe8d,0x20,
        0xfee5,0x20,0x655,0xfef9,0xfe8d,0x20,0xfee5,0x20,0x654,0xfef5,0xfe8d,0x20,
        0xfefc,0xfecb
    }, shape_excepttashkeel_at_end[]={
        0x20,0xfee5,0xfef5,0xfe8d,0x20,0xfee5,0xfe76,0xfef7,0xfe8d,0x20,0xfee5,
        0x670,0xfefb,0xfe8d,0x20,0xfee5,0x653,0xfef5,0xfe8d,0x20,0xfee5,0x655,0xfef9,
        0xfe8d,0x20,0xfee5,0x654,0xfef5,0xfe8d,0x20,0xfefc,0xfecb,0x20,0x20,0x20,
        0x20,0x20,0x20
    }, shape_excepttashkeel_at_begin[]={
        0x20,0x20,0x20,0x20,0x20,0x20,0x20,0xfee5,0xfef5,0xfe8d,0x20,0xfee5,0xfe76,
        0xfef7,0xfe8d,0x20,0xfee5,0x670,0xfefb,0xfe8d,0x20,0xfee5,0x653,0xfef5,0xfe8d,
        0x20,0xfee5,0x655,0xfef9,0xfe8d,0x20,0xfee5,0x654,0xfef5,0xfe8d,0x20,0xfefc,0xfecb
    }, shape_excepttashkeel_grow_shrink[]={
        0x20,0xfee5,0xfef5,0xfe8d,0x20,0xfee5,0xfe76,0xfef7,0xfe8d,0x20,0xfee5,0x670,
        0xfefb,0xfe8d,0x20,0xfee5,0x653,0xfef5,0xfe8d,0x20,0xfee5,0x655,0xfef9,0xfe8d,
        0x20,0xfee5,0x654,0xfef5,0xfe8d,0x20,0xfefc,0xfecb
    };

    UChar dest[38];
    UErrorCode errorCode;
    int32_t length;

    errorCode=U_ZERO_ERROR;

    length=u_shapeArabic(source, LENGTHOF(source),
                         dest, LENGTHOF(dest),
                         U_SHAPE_LETTERS_SHAPE|U_SHAPE_LENGTH_FIXED_SPACES_NEAR|
                         U_SHAPE_TEXT_DIRECTION_VISUAL_LTR,
                         &errorCode);

    if(U_FAILURE(errorCode) || length!=LENGTHOF(shape_near) || uprv_memcmp(dest, shape_near, length*U_SIZEOF_UCHAR)!=0) {
        log_err("failure in u_shapeArabic(LAMALEF shape_near)\n");
    }

    errorCode=U_ZERO_ERROR;

    length=u_shapeArabic(source, LENGTHOF(source),
                         dest, LENGTHOF(dest),
                         U_SHAPE_LETTERS_SHAPE|U_SHAPE_LENGTH_FIXED_SPACES_AT_END|
                         U_SHAPE_TEXT_DIRECTION_VISUAL_LTR,
                         &errorCode);

    if(U_FAILURE(errorCode) || length!=LENGTHOF(shape_at_end) || uprv_memcmp(dest, shape_at_end, length*U_SIZEOF_UCHAR)!=0) {
        log_err("failure in u_shapeArabic(LAMALEF shape_at_end)\n");
    }

    errorCode=U_ZERO_ERROR;

    length=u_shapeArabic(source, LENGTHOF(source),
                         dest, LENGTHOF(dest),
                         U_SHAPE_LETTERS_SHAPE|U_SHAPE_LENGTH_FIXED_SPACES_AT_BEGINNING|
                         U_SHAPE_TEXT_DIRECTION_VISUAL_LTR,
                         &errorCode);

    if(U_FAILURE(errorCode) || length!=LENGTHOF(shape_at_begin) || uprv_memcmp(dest, shape_at_begin, length*U_SIZEOF_UCHAR)!=0) {
        log_err("failure in u_shapeArabic(LAMALEF shape_at_begin)\n");
    }

    errorCode=U_ZERO_ERROR;

    length=u_shapeArabic(source, LENGTHOF(source),
                         dest, LENGTHOF(dest),
                         U_SHAPE_LETTERS_SHAPE|U_SHAPE_LENGTH_GROW_SHRINK|
                         U_SHAPE_TEXT_DIRECTION_VISUAL_LTR,
                         &errorCode);

    if(U_FAILURE(errorCode) || uprv_memcmp(dest, shape_grow_shrink, length*U_SIZEOF_UCHAR)!=0) {
        log_err("failure in u_shapeArabic(LAMALEF shape_grow_shrink)\n");
    }

    /* ==================== U_SHAPE_LETTERS_SHAPE_TASHKEEL_ISOLATED ==================== */

    errorCode=U_ZERO_ERROR;

    length=u_shapeArabic(source, LENGTHOF(source),
                         dest, LENGTHOF(dest),
                         U_SHAPE_LETTERS_SHAPE_TASHKEEL_ISOLATED|U_SHAPE_LENGTH_FIXED_SPACES_NEAR|
                         U_SHAPE_TEXT_DIRECTION_VISUAL_LTR,
                         &errorCode);

    if(U_FAILURE(errorCode) || length!=LENGTHOF(shape_excepttashkeel_near) || uprv_memcmp(dest, shape_excepttashkeel_near, length*U_SIZEOF_UCHAR)!=0) {
        log_err("failure in u_shapeArabic(LAMALEF shape_excepttashkeel_near)\n");
    }

    errorCode=U_ZERO_ERROR;

    length=u_shapeArabic(source, LENGTHOF(source),
                         dest, LENGTHOF(dest),
                         U_SHAPE_LETTERS_SHAPE_TASHKEEL_ISOLATED|U_SHAPE_LENGTH_FIXED_SPACES_AT_END|
                         U_SHAPE_TEXT_DIRECTION_VISUAL_LTR,
                         &errorCode);

    if(U_FAILURE(errorCode) || length!=LENGTHOF(shape_excepttashkeel_at_end) || uprv_memcmp(dest,shape_excepttashkeel_at_end , length*U_SIZEOF_UCHAR)!=0) {
        log_err("failure in u_shapeArabic(LAMALEF shape_excepttashkeel_at_end)\n");
    }

    errorCode=U_ZERO_ERROR;

    length=u_shapeArabic(source, LENGTHOF(source),
                         dest, LENGTHOF(dest),
                         U_SHAPE_LETTERS_SHAPE_TASHKEEL_ISOLATED|U_SHAPE_LENGTH_FIXED_SPACES_AT_BEGINNING|
                         U_SHAPE_TEXT_DIRECTION_VISUAL_LTR,
                         &errorCode);

    if(U_FAILURE(errorCode) || length!=LENGTHOF(shape_excepttashkeel_at_begin) || uprv_memcmp(dest, shape_excepttashkeel_at_begin, length*U_SIZEOF_UCHAR)!=0) {
        log_err("failure in u_shapeArabic(LAMALEF shape_excepttashkeel_at_begin)\n");
    }

    errorCode=U_ZERO_ERROR;

    length=u_shapeArabic(source, LENGTHOF(source),
                         dest, LENGTHOF(dest),
                         U_SHAPE_LETTERS_SHAPE_TASHKEEL_ISOLATED|U_SHAPE_LENGTH_GROW_SHRINK|
                         U_SHAPE_TEXT_DIRECTION_VISUAL_LTR,
                         &errorCode);

    if(U_FAILURE(errorCode) || uprv_memcmp(dest, shape_excepttashkeel_grow_shrink, length*U_SIZEOF_UCHAR)!=0) {
        log_err("failure in u_shapeArabic(LAMALEF shape_excepttashkeel_grow_shrink)\n");
    }
}

static void
doTashkeelSpecialVLTRArabicShapingTest() {
    static const UChar
    source[]={
        0x64A,0x628,0x631,0x639,0x20,
        0x64A,0x628,0x651,0x631,0x64E,0x639,0x20,
        0x64C,0x64A,0x628,0x631,0x64F,0x639,0x20,
        0x628,0x670,0x631,0x670,0x639,0x20,
        0x628,0x653,0x631,0x653,0x639,0x20,
        0x628,0x654,0x631,0x654,0x639,0x20,
        0x628,0x655,0x631,0x655,0x639,0x20,
    }, shape_near[]={
        0xfef2,0xfe91,0xfeae,0xfecb,0x20,0xfef2,0xfe91,0xfe7c,0xfeae,0xfe77,0xfecb,
        0x20,0xfe72,0xfef2,0xfe91,0xfeae,0xfe79,0xfecb,0x20,0xfe8f,0x670,0xfeae,0x670,
        0xfecb,0x20,0xfe8f,0x653,0xfeae,0x653,0xfecb,0x20,0xfe8f,0x654,0xfeae,0x654,
        0xfecb,0x20,0xfe8f,0x655,0xfeae,0x655,0xfecb,0x20
    }, shape_excepttashkeel_near[]={
        0xfef2,0xfe91,0xfeae,0xfecb,0x20,0xfef2,0xfe91,0xfe7c,0xfeae,0xfe76,0xfecb,0x20,
        0xfe72,0xfef2,0xfe91,0xfeae,0xfe78,0xfecb,0x20,0xfe8f,0x670,0xfeae,0x670,0xfecb,
        0x20,0xfe8f,0x653,0xfeae,0x653,0xfecb,0x20,0xfe8f,0x654,0xfeae,0x654,0xfecb,0x20,
        0xfe8f,0x655,0xfeae,0x655,0xfecb,0x20
    };

    UChar dest[43];
    UErrorCode errorCode;
    int32_t length;

    errorCode=U_ZERO_ERROR;

    length=u_shapeArabic(source, LENGTHOF(source),
                         dest, LENGTHOF(dest),
                         U_SHAPE_LETTERS_SHAPE|U_SHAPE_LENGTH_FIXED_SPACES_NEAR|
                         U_SHAPE_TEXT_DIRECTION_VISUAL_LTR,
                         &errorCode);

    if(U_FAILURE(errorCode) || length!=LENGTHOF(shape_near) || uprv_memcmp(dest, shape_near, length*U_SIZEOF_UCHAR)!=0) {
        log_err("failure in u_shapeArabic(TASHKEEL shape_near)\n");
    }

    errorCode=U_ZERO_ERROR;

    length=u_shapeArabic(source, LENGTHOF(source),
                         dest, LENGTHOF(dest),
                         U_SHAPE_LETTERS_SHAPE_TASHKEEL_ISOLATED|U_SHAPE_LENGTH_FIXED_SPACES_NEAR|
                         U_SHAPE_TEXT_DIRECTION_VISUAL_LTR,
                         &errorCode);

    if(U_FAILURE(errorCode) || length!=LENGTHOF(shape_excepttashkeel_near) || uprv_memcmp(dest, shape_excepttashkeel_near, length*U_SIZEOF_UCHAR)!=0) {
        log_err("failure in u_shapeArabic(TASHKEEL shape_excepttashkeel_near)\n");
    }
}

static void
doLOGICALArabicDeShapingTest() {
    static const UChar
    source[]={
        0x0020,0x0020,0x0020,0xFE8D,0xFEF5,0x0020,0xFEE5,0x0020,0xFE8D,0xFEF7,0x0020,
        0xFED7,0xFEFC,0x0020,0xFEE1,0x0020,0xFE8D,0xFEDF,0xFECC,0xFEAE,0xFE91,0xFEF4,
        0xFE94,0x0020,0xFE8D,0xFEDF,0xFEA4,0xFEAE,0xFE93,0x0020,0x0020,0x0020,0x0020
    }, unshape_near[]={
        0x20,0x20,0x20,0x627,0x644,0x622,0x646,0x20,0x627,0x644,0x623,0x642,0x644,0x627,
        0x645,0x20,0x627,0x644,0x639,0x631,0x628,0x64a,0x629,0x20,0x627,0x644,0x62d,0x631,
        0x629,0x20,0x20,0x20,0x20
    }, unshape_at_end[]={
        0x20,0x20,0x20,0x627,0x644,0x622,0x20,0x646,0x20,0x627,0x644,0x623,0x20,0x642,
        0x644,0x627,0x20,0x645,0x20,0x627,0x644,0x639,0x631,0x628,0x64a,0x629,0x20,0x627,
        0x644,0x62d,0x631,0x629,0x20
    }, unshape_at_begin[]={
        0x627,0x644,0x622,0x20,0x646,0x20,0x627,0x644,0x623,0x20,0x642,0x644,0x627,0x20,
        0x645,0x20,0x627,0x644,0x639,0x631,0x628,0x64a,0x629,0x20,0x627,0x644,0x62d,0x631,
        0x629,0x20,0x20,0x20,0x20
    }, unshape_grow_shrink[]={
        0x20,0x20,0x20,0x627,0x644,0x622,0x20,0x646,0x20,0x627,0x644,0x623,0x20,0x642,
        0x644,0x627,0x20,0x645,0x20,0x627,0x644,0x639,0x631,0x628,0x64a,0x629,0x20,0x627,
        0x644,0x62d,0x631,0x629,0x20,0x20,0x20,0x20
    };

    UChar dest[36];
    UErrorCode errorCode;
    int32_t length;

    errorCode=U_ZERO_ERROR;

    length=u_shapeArabic(source, LENGTHOF(source),
                         dest, LENGTHOF(dest),
                         U_SHAPE_LETTERS_UNSHAPE|U_SHAPE_LENGTH_FIXED_SPACES_NEAR|
                         U_SHAPE_TEXT_DIRECTION_LOGICAL,
                         &errorCode);

    if(U_FAILURE(errorCode) || length!=LENGTHOF(unshape_near) || uprv_memcmp(dest, unshape_near, length*U_SIZEOF_UCHAR)!=0) {
        log_err("failure in u_shapeArabic(unshape_near)\n");
    }

    errorCode=U_ZERO_ERROR;

    length=u_shapeArabic(source, LENGTHOF(source),
                         dest, LENGTHOF(dest),
                         U_SHAPE_LETTERS_UNSHAPE|U_SHAPE_LENGTH_FIXED_SPACES_AT_END|
                         U_SHAPE_TEXT_DIRECTION_LOGICAL,
                         &errorCode);

    if(U_FAILURE(errorCode) || length!=LENGTHOF(unshape_at_end) || uprv_memcmp(dest, unshape_at_end, length*U_SIZEOF_UCHAR)!=0) {
        log_err("failure in u_shapeArabic(unshape_at_end)\n");
    }

    errorCode=U_ZERO_ERROR;

    length=u_shapeArabic(source, LENGTHOF(source),
                         dest, LENGTHOF(dest),
                         U_SHAPE_LETTERS_UNSHAPE|U_SHAPE_LENGTH_FIXED_SPACES_AT_BEGINNING|
                         U_SHAPE_TEXT_DIRECTION_LOGICAL,
                         &errorCode);

    if(U_FAILURE(errorCode) || length!=LENGTHOF(unshape_at_begin) || uprv_memcmp(dest, unshape_at_begin, length*U_SIZEOF_UCHAR)!=0) {
        log_err("failure in u_shapeArabic(unshape_at_begin)\n");
    }

    errorCode=U_ZERO_ERROR;

    length=u_shapeArabic(source, LENGTHOF(source),
                         dest, LENGTHOF(dest),
                         U_SHAPE_LETTERS_UNSHAPE|U_SHAPE_LENGTH_GROW_SHRINK|
                         U_SHAPE_TEXT_DIRECTION_LOGICAL,
                         &errorCode);

    if(U_FAILURE(errorCode) || uprv_memcmp(dest, unshape_grow_shrink, length*U_SIZEOF_UCHAR)!=0) {
        log_err("failure in u_shapeArabic(unshape_grow_shrink)\n");
    }

}

/* helpers ------------------------------------------------------------------ */

static void
initCharFromDirProps() {
    static const UVersionInfo ucd401={ 4, 0, 1, 0 };
    static UVersionInfo ucdVersion={ 0, 0, 0, 0 };

    /* lazy initialization */
    if(ucdVersion[0]>0) {
        return;
    }

    u_getUnicodeVersion(ucdVersion);
    if(memcmp(ucdVersion, ucd401, sizeof(UVersionInfo))>=0) {
        /* Unicode 4.0.1 changes bidi classes for +-/ */
        charFromDirProp[U_EUROPEAN_NUMBER_SEPARATOR]=0x2b; /* change ES character from / to + */
    }
}

/* return a string with characters according to the desired directional properties */
static UChar *
getStringFromDirProps(const uint8_t *dirProps, int32_t length) {
    static UChar s[MAX_STRING_LENGTH];
    int32_t i;

    initCharFromDirProps();

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
