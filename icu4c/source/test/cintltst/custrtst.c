/*
******************************************************************************
*
*   Copyright (C) 2002, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
******************************************************************************
*   file name:  custrtst.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2002oct09
*   created by: Markus W. Scherer
*
*   Tests of ustring.h Unicode string API functions.
*/

#include "unicode/utypes.h"
#include "unicode/ustring.h"
#include "unicode/uloc.h"
#include "cintltst.h"
#include "cucdtst.h"
#include <string.h>
#include <stdlib.h>

#define LENGTHOF(array) (sizeof(array)/sizeof((array)[0]))

/* get the sign of an integer */
#define _SIGN(value) ((value)==0 ? 0 : ((int32_t)(value)>>31)|1)

/* test setup --------------------------------------------------------------- */

static void TestStringCopy(void);
static void TestStringFunctions(void);
static void TestStringSearching(void);
static void TestUnescape(void);

void addUStringTest(TestNode** root)
{
    addTest(root, &TestStringCopy, "tsutil/custrtst/TestStringCopy");
    addTest(root, &TestStringFunctions, "tsutil/custrtst/TestStringFunctions");
    addTest(root, &TestStringSearching, "tsutil/custrtst/TestStringSearching");
    addTest(root, &TestUnescape, "tsutil/custrtst/TestUnescape");

    /* cstrcase.c functions, declared in cucdtst.h */
    addTest(root, &TestCaseLower, "tsutil/custrtst/TestCaseLower");
    addTest(root, &TestCaseUpper, "tsutil/custrtst/TestCaseUpper");
#if !UCONFIG_NO_BREAK_ITERATION
    addTest(root, &TestCaseTitle, "tsutil/custrtst/TestCaseTitle");
#endif
    addTest(root, &TestCaseFolding, "tsutil/custrtst/TestCaseFolding");
    addTest(root, &TestCaseCompare, "tsutil/custrtst/TestCaseCompare");
}

/* test data for TestStringFunctions ---------------------------------------- */

UChar*** dataTable = NULL;

static const char* raw[3][4] = {

    /* First String */
    {   "English_",  "French_",   "Croatian_", "English_"},
    /* Second String */
    {   "United States",    "France",   "Croatia",  "Unites States"},

   /* Concatenated string */
    {   "English_United States", "French_France", "Croatian_Croatia", "English_United States"}
};

static void setUpDataTable()
{
    int32_t i,j;
    if(dataTable == NULL) {
        dataTable = (UChar***)calloc(sizeof(UChar**),3);

            for (i = 0; i < 3; i++) {
              dataTable[i] = (UChar**)calloc(sizeof(UChar*),4);
                for (j = 0; j < 4; j++){
                    dataTable[i][j] = (UChar*) malloc(sizeof(UChar)*(strlen(raw[i][j])+1));
                    u_uastrcpy(dataTable[i][j],raw[i][j]);
                }
            }
    }
}

static void cleanUpDataTable()
{
    int32_t i,j;
    if(dataTable != NULL) {
        for (i=0; i<3; i++) {
            for(j = 0; j<4; j++) {
                free(dataTable[i][j]);
            }
            free(dataTable[i]);
        }
        free(dataTable);
    }
    dataTable = NULL;
}

/*Tests  for u_strcat(),u_strcmp(), u_strlen(), u_strcpy(),u_strncat(),u_strncmp(),u_strncpy, u_uastrcpy(),u_austrcpy(), u_uastrncpy(); */
static void TestStringFunctions()
{
    int32_t i,j,k;
    UChar temp[512];
    UChar nullTemp[512];
    char test[512];
    char tempOut[512];

    setUpDataTable();

    log_verbose("Testing u_strlen()\n");
    if( u_strlen(dataTable[0][0])!= u_strlen(dataTable[0][3]) || u_strlen(dataTable[0][0]) == u_strlen(dataTable[0][2]))
        log_err("There is an error in u_strlen()");

    log_verbose("Testing u_memcpy() and u_memcmp()\n");

    for(i=0;i<3;++i)
    {
        for(j=0;j<4;++j)
        {
            log_verbose("Testing  %s\n", u_austrcpy(tempOut, dataTable[i][j]));
            temp[0] = 0;
            temp[7] = 0xA4; /* Mark the end */
            u_memcpy(temp,dataTable[i][j], 7);

            if(temp[7] != 0xA4)
                log_err("an error occured in u_memcpy()\n");
            if(u_memcmp(temp, dataTable[i][j], 7)!=0)
                log_err("an error occured in u_memcpy() or u_memcmp()\n");
        }
    }
    if(u_memcmp(dataTable[0][0], dataTable[1][1], 7)==0)
        log_err("an error occured in u_memcmp()\n");

    log_verbose("Testing u_memset()\n");
    nullTemp[0] = 0;
    nullTemp[7] = 0;
    u_memset(nullTemp, 0xa4, 7);
    for (i = 0; i < 7; i++) {
        if(nullTemp[i] != 0xa4) {
            log_err("an error occured in u_memset()\n");
        }
    }
    if(nullTemp[7] != 0) {
        log_err("u_memset() went too far\n");
    }

    u_memset(nullTemp, 0, 7);
    nullTemp[7] = 0xa4;
    temp[7] = 0;
    u_memcpy(temp,nullTemp, 7);
    if(u_memcmp(temp, nullTemp, 7)!=0 || temp[7]!=0)
        log_err("an error occured in u_memcpy() or u_memcmp()\n");


    log_verbose("Testing u_memmove()\n");
    for (i = 0; i < 7; i++) {
        temp[i] = (UChar)i;
    }
    u_memmove(temp + 1, temp, 7);
    if(temp[0] != 0) {
        log_err("an error occured in u_memmove()\n");
    }
    for (i = 1; i <= 7; i++) {
        if(temp[i] != (i - 1)) {
            log_err("an error occured in u_memmove()\n");
        }
    }

    log_verbose("Testing u_strcpy() and u_strcmp()\n");

    for(i=0;i<3;++i)
    {
        for(j=0;j<4;++j)
        {
            log_verbose("Testing  %s\n", u_austrcpy(tempOut, dataTable[i][j]));
            temp[0] = 0;
            u_strcpy(temp,dataTable[i][j]);

            if(u_strcmp(temp,dataTable[i][j])!=0)
                log_err("something threw an error in u_strcpy() or u_strcmp()\n");
        }
    }
    if(u_strcmp(dataTable[0][0], dataTable[1][1])==0)
        log_err("an error occured in u_memcmp()\n");

    log_verbose("testing u_strcat()\n");
    i=0;
    for(j=0; j<2;++j)
    {
        u_uastrcpy(temp, "");
        u_strcpy(temp,dataTable[i][j]);
        u_strcat(temp,dataTable[i+1][j]);
        if(u_strcmp(temp,dataTable[i+2][j])!=0)
            log_err("something threw an error in u_strcat()\n");

    }
    log_verbose("Testing u_strncmp()\n");
    for(i=0,j=0;j<4; ++j)
    {
        k=u_strlen(dataTable[i][j]);
        if(u_strncmp(dataTable[i][j],dataTable[i+2][j],k)!=0)
            log_err("Something threw an error in u_strncmp\n");
    }
    if(u_strncmp(dataTable[0][0], dataTable[1][1], 7)==0)
        log_err("an error occured in u_memcmp()\n");


    log_verbose("Testing u_strncat\n");
    for(i=0,j=0;j<4; ++j)
    {
        k=u_strlen(dataTable[i][j]);

        u_uastrcpy(temp,"");

        if(u_strcmp(u_strncat(temp,dataTable[i+2][j],k),dataTable[i][j])!=0)
            log_err("something threw an error in u_strncat or u_uastrcpy()\n");

    }

    log_verbose("Testing u_strncpy() and u_uastrcpy()\n");
    for(i=2,j=0;j<4; ++j)
    {
        k=u_strlen(dataTable[i][j]);
        u_strncpy(temp, dataTable[i][j],k);
        temp[k] = 0xa4;

        if(u_strncmp(temp, dataTable[i][j],k)!=0)
            log_err("something threw an error in u_strncpy()\n");

        if(temp[k] != 0xa4)
            log_err("something threw an error in u_strncpy()\n");

        u_memset(temp, 0x3F, (sizeof(temp) / sizeof(UChar)) - 1);
        u_uastrncpy(temp, raw[i][j], k-1);
        if(u_strncmp(temp, dataTable[i][j],k-1)!=0)
            log_err("something threw an error in u_uastrncpy(k-1)\n");

        if(temp[k-1] != 0x3F)
            log_err("something threw an error in u_uastrncpy(k-1)\n");

        u_memset(temp, 0x3F, (sizeof(temp) / sizeof(UChar)) - 1);
        u_uastrncpy(temp, raw[i][j], k+1);
        if(u_strcmp(temp, dataTable[i][j])!=0)
            log_err("something threw an error in u_uastrncpy(k+1)\n");

        if(temp[k] != 0)
            log_err("something threw an error in u_uastrncpy(k+1)\n");

        u_memset(temp, 0x3F, (sizeof(temp) / sizeof(UChar)) - 1);
        u_uastrncpy(temp, raw[i][j], k);
        if(u_strncmp(temp, dataTable[i][j], k)!=0)
            log_err("something threw an error in u_uastrncpy(k)\n");

        if(temp[k] != 0x3F)
            log_err("something threw an error in u_uastrncpy(k)\n");
    }

    log_verbose("Testing u_strchr() and u_memchr()\n");

    for(i=2,j=0;j<4;j++)
    {
        UChar saveVal = dataTable[i][j][0];
        UChar *findPtr = u_strchr(dataTable[i][j], 0x005F);
        int32_t dataSize = (int32_t)(u_strlen(dataTable[i][j]) + 1);

        log_verbose("%s ", u_austrcpy(tempOut, findPtr));

        if (findPtr == NULL || *findPtr != 0x005F) {
            log_err("u_strchr can't find '_' in the string\n");
        }

        findPtr = u_strchr32(dataTable[i][j], 0x005F);
        if (findPtr == NULL || *findPtr != 0x005F) {
            log_err("u_strchr32 can't find '_' in the string\n");
        }

        findPtr = u_strchr(dataTable[i][j], 0);
        if (findPtr != (&(dataTable[i][j][dataSize - 1]))) {
            log_err("u_strchr can't find NULL in the string\n");
        }

        findPtr = u_strchr32(dataTable[i][j], 0);
        if (findPtr != (&(dataTable[i][j][dataSize - 1]))) {
            log_err("u_strchr32 can't find NULL in the string\n");
        }

        findPtr = u_memchr(dataTable[i][j], 0, dataSize);
        if (findPtr != (&(dataTable[i][j][dataSize - 1]))) {
            log_err("u_memchr can't find NULL in the string\n");
        }

        findPtr = u_memchr32(dataTable[i][j], 0, dataSize);
        if (findPtr != (&(dataTable[i][j][dataSize - 1]))) {
            log_err("u_memchr32 can't find NULL in the string\n");
        }

        dataTable[i][j][0] = 0;
        /* Make sure we skip over the NULL termination */
        findPtr = u_memchr(dataTable[i][j], 0x005F, dataSize);
        if (findPtr == NULL || *findPtr != 0x005F) {
            log_err("u_memchr can't find '_' in the string\n");
        }

        findPtr = u_memchr32(dataTable[i][j], 0x005F, dataSize);
        if (findPtr == NULL || *findPtr != 0x005F) {
            log_err("u_memchr32 can't find '_' in the string\n");
        }
        findPtr = u_memchr32(dataTable[i][j], 0xFFFD, dataSize);
        if (findPtr != NULL) {
            log_err("Should have found NULL when the character is not there.\n");
        }
        dataTable[i][j][0] = saveVal;   /* Put it back for the other tests */
    }

    /*
     * test that u_strchr32()
     * does not find surrogate code points when they are part of matched pairs
     * (= part of supplementary code points)
     * Jitterbug 1542
     */
    {
        static const UChar s[]={
            /*   0       1       2       3       4       5       6       7       8  9 */
            0x0061, 0xd841, 0xdc02, 0xd841, 0x0062, 0xdc02, 0xd841, 0xdc02, 0x0063, 0
        };

        if(u_strchr32(s, 0xd841)!=(s+3) || u_strchr32(s, 0xdc02)!=(s+5)) {
            log_err("error: u_strchr32(surrogate) finds a partial supplementary code point\n");
        }
        if(u_memchr32(s, 0xd841, 9)!=(s+3) || u_memchr32(s, 0xdc02, 9)!=(s+5)) {
            log_err("error: u_memchr32(surrogate) finds a partial supplementary code point\n");
        }
    }

    log_verbose("Testing u_austrcpy()");
    u_austrcpy(test,dataTable[0][0]);
    if(strcmp(test,raw[0][0])!=0)
        log_err("There is an error in u_austrcpy()");


    log_verbose("Testing u_strtok_r()");
    {
        const char tokString[] = "  ,  1 2 3  AHHHHH! 5.5 6 7    ,        8\n";
        const char *tokens[] = {",", "1", "2", "3", "AHHHHH!", "5.5", "6", "7", "8\n"};
        UChar delimBuf[sizeof(test)];
        UChar currTokenBuf[sizeof(tokString)];
        UChar *state;
        uint32_t currToken = 0;
        UChar *ptr;

        u_uastrcpy(temp, tokString);
        u_uastrcpy(delimBuf, " ");

        ptr = u_strtok_r(temp, delimBuf, &state);
        u_uastrcpy(delimBuf, " ,");
        while (ptr != NULL) {
            u_uastrcpy(currTokenBuf, tokens[currToken]);
            if (u_strcmp(ptr, currTokenBuf) != 0) {
                log_err("u_strtok_r mismatch at %d. Got: %s, Expected: %s\n", currToken, ptr, tokens[currToken]);
            }
            ptr = u_strtok_r(NULL, delimBuf, &state);
            currToken++;
        }

        if (currToken != sizeof(tokens)/sizeof(tokens[0])) {
            log_err("Didn't get correct number of tokens\n");
        }
        state = delimBuf;       /* Give it an "invalid" saveState */
        u_uastrcpy(currTokenBuf, "");
        if (u_strtok_r(currTokenBuf, delimBuf, &state) != NULL) {
            log_err("Didn't get NULL for empty string\n");
        }
        if (state != NULL) {
            log_err("State should be NULL for empty string\n");
        }
        state = delimBuf;       /* Give it an "invalid" saveState */
        u_uastrcpy(currTokenBuf, ", ,");
        if (u_strtok_r(currTokenBuf, delimBuf, &state) != NULL) {
            log_err("Didn't get NULL for a string of delimiters\n");
        }
        if (state != NULL) {
            log_err("State should be NULL for a string of delimiters\n");
        }

        state = delimBuf;       /* Give it an "invalid" saveState */
        u_uastrcpy(currTokenBuf, "q, ,");
        if (u_strtok_r(currTokenBuf, delimBuf, &state) == NULL) {
            log_err("Got NULL for a string that does not begin with delimiters\n");
        }
        if (u_strtok_r(NULL, delimBuf, &state) != NULL) {
            log_err("Didn't get NULL for a string that ends in delimiters\n");
        }
        if (state != NULL) {
            log_err("State should be NULL for empty string\n");
        }

        state = delimBuf;       /* Give it an "invalid" saveState */
        u_uastrcpy(currTokenBuf, tokString);
        u_uastrcpy(temp, tokString);
        u_uastrcpy(delimBuf, "q");  /* Give it a delimiter that it can't find. */
        ptr = u_strtok_r(currTokenBuf, delimBuf, &state);
        if (ptr == NULL || u_strcmp(ptr, temp) != 0) {
            log_err("Should have recieved the same string when there are no delimiters\n");
        }
        if (u_strtok_r(NULL, delimBuf, &state) != NULL) {
            log_err("Should not have found another token in a one token string\n");
        }
    }

    /* test u_strcmpCodePointOrder() */
    {
        /* these strings are in ascending order */
        static const UChar strings[][4]={
            { 0x61, 0 },                    /* U+0061 */
            { 0x20ac, 0xd801, 0 },          /* U+20ac U+d801 */
            { 0x20ac, 0xd800, 0xdc00, 0 },  /* U+20ac U+10000 */
            { 0xd800, 0 },                  /* U+d800 */
            { 0xd800, 0xff61, 0 },          /* U+d800 U+ff61 */
            { 0xdfff, 0 },                  /* U+dfff */
            { 0xff61, 0xdfff, 0 },          /* U+ff61 U+dfff */
            { 0xff61, 0xd800, 0xdc02, 0 },  /* U+ff61 U+10002 */
            { 0xd800, 0xdc02, 0 },          /* U+10002 */
            { 0xd84d, 0xdc56, 0 }           /* U+23456 */
        };
        int32_t len1, len2;

        for(i=0; i<(sizeof(strings)/sizeof(strings[0])-1); ++i) {
            if(u_strcmpCodePointOrder(strings[i], strings[i+1])>=0) {
                log_err("error: u_strcmpCodePointOrder() fails for string %d and the following one\n", i);
            }

            /* test u_strCompare(TRUE) */
            len1=u_strlen(strings[i]);
            len2=u_strlen(strings[i+1]);
            if( u_strCompare(strings[i], -1, strings[i+1], -1, TRUE)>=0 ||
                u_strCompare(strings[i], -1, strings[i+1], len2, TRUE)>=0 ||
                u_strCompare(strings[i], len1, strings[i+1], -1, TRUE)>=0 ||
                u_strCompare(strings[i], len1, strings[i+1], len2, TRUE)>=0
            ) {
                log_err("error: u_strCompare(code point order) fails for string %d and the following one\n", i);
            }

            /* test u_strCompare(FALSE) */
            if(_SIGN(u_strCompare(strings[i], -1, strings[i+1], -1, FALSE))!=_SIGN(u_strcmp(strings[i], strings[i+1]))) {
                log_err("error: u_strCompare(code unit order)!=u_strcmp() for string %d and the following one\n", i);
            }
        }
    }

    cleanUpDataTable();
}

static void TestStringSearching()
{
    const UChar testString[] = {0x0061, 0x0062, 0x0063, 0x0064, 0x0064, 0x0061, 0};
    const UChar testSurrogateString[] = {0xdbff, 0x0061, 0x0062, 0xdbff, 0xdfff, 0x0063, 0x0064, 0x0064, 0xdbff, 0xdfff, 0xdb00, 0xdf00, 0x0061, 0};
    const UChar surrMatchSet1[] = {0xdbff, 0xdfff, 0};
    const UChar surrMatchSet2[] = {0x0061, 0x0062, 0xdbff, 0xdfff, 0};
    const UChar surrMatchSet3[] = {0xdb00, 0xdf00, 0xdbff, 0xdfff, 0};
    const UChar surrMatchSet4[] = {0x0000};
    const UChar surrMatchSetBad[] = {0xdbff, 0x0061, 0};
    const UChar surrMatchSetBad2[] = {0x0061, 0xdbff, 0};
    const UChar surrMatchSetBad3[] = {0xdbff, 0x0061, 0x0062, 0xdbff, 0xdfff, 0};   /* has partial surrogate */
    const UChar
        empty[] = { 0 },
        a[] = { 0x61, 0 },
        ab[] = { 0x61, 0x62, 0 },
        ba[] = { 0x62, 0x61, 0 },
        abcd[] = { 0x61, 0x62, 0x63, 0x64, 0 },
        cd[] = { 0x63, 0x64, 0 },
        dc[] = { 0x64, 0x63, 0 },
        cdh[] = { 0x63, 0x64, 0x68, 0 },
        f[] = { 0x66, 0 },
        fg[] = { 0x66, 0x67, 0 },
        gf[] = { 0x67, 0x66, 0 };

    log_verbose("Testing u_strpbrk()");

    if (u_strpbrk(testString, a) != &testString[0]) {
        log_err("u_strpbrk couldn't find first letter a.\n");
    }
    if (u_strpbrk(testString, dc) != &testString[2]) {
        log_err("u_strpbrk couldn't find d or c.\n");
    }
    if (u_strpbrk(testString, cd) != &testString[2]) {
        log_err("u_strpbrk couldn't find c or d.\n");
    }
    if (u_strpbrk(testString, cdh) != &testString[2]) {
        log_err("u_strpbrk couldn't find c, d or h.\n");
    }
    if (u_strpbrk(testString, f) != NULL) {
        log_err("u_strpbrk didn't return NULL for \"f\".\n");
    }
    if (u_strpbrk(testString, fg) != NULL) {
        log_err("u_strpbrk didn't return NULL for \"fg\".\n");
    }
    if (u_strpbrk(testString, gf) != NULL) {
        log_err("u_strpbrk didn't return NULL for \"gf\".\n");
    }
    if (u_strpbrk(testString, empty) != NULL) {
        log_err("u_strpbrk didn't return NULL for \"\".\n");
    }

    log_verbose("Testing u_strpbrk() with surrogates");

    if (u_strpbrk(testSurrogateString, a) != &testSurrogateString[1]) {
        log_err("u_strpbrk couldn't find first letter a.\n");
    }
    if (u_strpbrk(testSurrogateString, dc) != &testSurrogateString[5]) {
        log_err("u_strpbrk couldn't find d or c.\n");
    }
    if (u_strpbrk(testSurrogateString, cd) != &testSurrogateString[5]) {
        log_err("u_strpbrk couldn't find c or d.\n");
    }
    if (u_strpbrk(testSurrogateString, cdh) != &testSurrogateString[5]) {
        log_err("u_strpbrk couldn't find c, d or h.\n");
    }
    if (u_strpbrk(testSurrogateString, f) != NULL) {
        log_err("u_strpbrk didn't return NULL for \"f\".\n");
    }
    if (u_strpbrk(testSurrogateString, fg) != NULL) {
        log_err("u_strpbrk didn't return NULL for \"fg\".\n");
    }
    if (u_strpbrk(testSurrogateString, gf) != NULL) {
        log_err("u_strpbrk didn't return NULL for \"gf\".\n");
    }
    if (u_strpbrk(testSurrogateString, surrMatchSet1) != &testSurrogateString[3]) {
        log_err("u_strpbrk couldn't find \"0xdbff, 0xdfff\".\n");
    }
    if (u_strpbrk(testSurrogateString, surrMatchSet2) != &testSurrogateString[1]) {
        log_err("u_strpbrk couldn't find \"0xdbff, a, b, 0xdbff, 0xdfff\".\n");
    }
    if (u_strpbrk(testSurrogateString, surrMatchSet3) != &testSurrogateString[3]) {
        log_err("u_strpbrk couldn't find \"0xdb00, 0xdf00, 0xdbff, 0xdfff\".\n");
    }
    if (u_strpbrk(testSurrogateString, surrMatchSet4) != NULL) {
        log_err("u_strpbrk should have returned NULL for empty string.\n");
    }
    if (u_strpbrk(testSurrogateString, surrMatchSetBad) != &testSurrogateString[0]) {
        log_err("u_strpbrk should have found bad surrogate.\n");
    }

    log_verbose("Testing u_strcspn()");

    if (u_strcspn(testString, a) != 0) {
        log_err("u_strcspn couldn't find first letter a.\n");
    }
    if (u_strcspn(testString, dc) != 2) {
        log_err("u_strcspn couldn't find d or c.\n");
    }
    if (u_strcspn(testString, cd) != 2) {
        log_err("u_strcspn couldn't find c or d.\n");
    }
    if (u_strcspn(testString, cdh) != 2) {
        log_err("u_strcspn couldn't find c, d or h.\n");
    }
    if (u_strcspn(testString, f) != u_strlen(testString)) {
        log_err("u_strcspn didn't return NULL for \"f\".\n");
    }
    if (u_strcspn(testString, fg) != u_strlen(testString)) {
        log_err("u_strcspn didn't return NULL for \"fg\".\n");
    }
    if (u_strcspn(testString, gf) != u_strlen(testString)) {
        log_err("u_strcspn didn't return NULL for \"gf\".\n");
    }

    log_verbose("Testing u_strcspn() with surrogates");

    if (u_strcspn(testSurrogateString, a) != 1) {
        log_err("u_strcspn couldn't find first letter a.\n");
    }
    if (u_strcspn(testSurrogateString, dc) != 5) {
        log_err("u_strcspn couldn't find d or c.\n");
    }
    if (u_strcspn(testSurrogateString, cd) != 5) {
        log_err("u_strcspn couldn't find c or d.\n");
    }
    if (u_strcspn(testSurrogateString, cdh) != 5) {
        log_err("u_strcspn couldn't find c, d or h.\n");
    }
    if (u_strcspn(testSurrogateString, f) != u_strlen(testSurrogateString)) {
        log_err("u_strcspn didn't return NULL for \"f\".\n");
    }
    if (u_strcspn(testSurrogateString, fg) != u_strlen(testSurrogateString)) {
        log_err("u_strcspn didn't return NULL for \"fg\".\n");
    }
    if (u_strcspn(testSurrogateString, gf) != u_strlen(testSurrogateString)) {
        log_err("u_strcspn didn't return NULL for \"gf\".\n");
    }
    if (u_strcspn(testSurrogateString, surrMatchSet1) != 3) {
        log_err("u_strcspn couldn't find \"0xdbff, 0xdfff\".\n");
    }
    if (u_strcspn(testSurrogateString, surrMatchSet2) != 1) {
        log_err("u_strcspn couldn't find \"a, b, 0xdbff, 0xdfff\".\n");
    }
    if (u_strcspn(testSurrogateString, surrMatchSet3) != 3) {
        log_err("u_strcspn couldn't find \"0xdb00, 0xdf00, 0xdbff, 0xdfff\".\n");
    }
    if (u_strcspn(testSurrogateString, surrMatchSet4) != u_strlen(testSurrogateString)) {
        log_err("u_strcspn should have returned strlen for empty string.\n");
    }


    log_verbose("Testing u_strspn()");

    if (u_strspn(testString, a) != 1) {
        log_err("u_strspn couldn't skip first letter a.\n");
    }
    if (u_strspn(testString, ab) != 2) {
        log_err("u_strspn couldn't skip a or b.\n");
    }
    if (u_strspn(testString, ba) != 2) {
        log_err("u_strspn couldn't skip a or b.\n");
    }
    if (u_strspn(testString, f) != 0) {
        log_err("u_strspn didn't return 0 for \"f\".\n");
    }
    if (u_strspn(testString, dc) != 0) {
        log_err("u_strspn couldn't find first letter a (skip d or c).\n");
    }
    if (u_strspn(testString, abcd) != u_strlen(testString)) {
        log_err("u_strspn couldn't skip over the whole string.\n");
    }
    if (u_strspn(testString, empty) != 0) {
        log_err("u_strspn should have returned 0 for empty string.\n");
    }

    log_verbose("Testing u_strspn() with surrogates");
    if (u_strspn(testSurrogateString, surrMatchSetBad) != 2) {
        log_err("u_strspn couldn't skip 0xdbff or a.\n");
    }
    if (u_strspn(testSurrogateString, surrMatchSetBad2) != 2) {
        log_err("u_strspn couldn't skip 0xdbff or a.\n");
    }
    if (u_strspn(testSurrogateString, f) != 0) {
        log_err("u_strspn couldn't skip d or c (skip first letter).\n");
    }
    if (u_strspn(testSurrogateString, dc) != 0) {
        log_err("u_strspn couldn't skip d or c (skip first letter).\n");
    }
    if (u_strspn(testSurrogateString, cd) != 0) {
        log_err("u_strspn couldn't skip d or c (skip first letter).\n");
    }
    if (u_strspn(testSurrogateString, testSurrogateString) != u_strlen(testSurrogateString)) {
        log_err("u_strspn couldn't skip whole string.\n");
    }
    if (u_strspn(testSurrogateString, surrMatchSet1) != 0) {
        log_err("u_strspn couldn't skip \"0xdbff, 0xdfff\" (get first letter).\n");
    }
    if (u_strspn(testSurrogateString, surrMatchSetBad3) != 5) {
        log_err("u_strspn couldn't skip \"0xdbff, a, b, 0xdbff, 0xdfff\".\n");
    }
    if (u_strspn(testSurrogateString, surrMatchSet4) != 0) {
        log_err("u_strspn should have returned 0 for empty string.\n");
    }
}

static void TestStringCopy()
{
    UChar temp[40];
    UChar *result=0;
    UChar subString[5];
    UChar uchars[]={0x61, 0x62, 0x63, 0x00};
    char  charOut[40];
    char  chars[]="abc";    /* needs default codepage */

    log_verbose("Testing u_uastrncpy() and u_uastrcpy()");

    u_uastrcpy(temp, "abc");
    if(u_strcmp(temp, uchars) != 0) {
        log_err("There is an error in u_uastrcpy() Expected %s Got %s\n", austrdup(uchars), austrdup(temp));
    }

    temp[0] = 0xFB; /* load garbage into it */
    temp[1] = 0xFB;
    temp[2] = 0xFB;
    temp[3] = 0xFB;

    u_uastrncpy(temp, "abcabcabc", 3);
    if(u_strncmp(uchars, temp, 3) != 0){
        log_err("There is an error in u_uastrncpy() Expected %s Got %s\n", austrdup(uchars), austrdup(temp));
    }
    if(temp[3] != 0xFB) {
        log_err("u_uastrncpy wrote past it's bounds. Expected undisturbed byte at 3\n");
    }

    charOut[0] = (char)0x7B; /* load garbage into it */
    charOut[1] = (char)0x7B;
    charOut[2] = (char)0x7B;
    charOut[3] = (char)0x7B;

    temp[0] = 0x0061;
    temp[1] = 0x0062;
    temp[2] = 0x0063;
    temp[3] = 0x0061;
    temp[4] = 0x0062;
    temp[5] = 0x0063;
    temp[6] = 0x0000;

    u_austrncpy(charOut, temp, 3);
    if(strncmp(chars, charOut, 3) != 0){
        log_err("There is an error in u_austrncpy() Expected %s Got %s\n", austrdup(uchars), austrdup(temp));
    }
    if(charOut[3] != (char)0x7B) {
        log_err("u_austrncpy wrote past it's bounds. Expected undisturbed byte at 3\n");
    }

    /*Testing u_strchr()*/
    log_verbose("Testing u_strchr\n");
    temp[0]=0x42;
    temp[1]=0x62;
    temp[2]=0x62;
    temp[3]=0x63;
    temp[4]=0xd841;
    temp[5]=0xd841;
    temp[6]=0xdc02;
    temp[7]=0;
    result=u_strchr(temp, (UChar)0x62);
    if(result != temp+1){
        log_err("There is an error in u_strchr() Expected match at position 1 Got %ld (pointer 0x%lx)\n", result-temp, result);
    }
    /*Testing u_strstr()*/
    log_verbose("Testing u_strstr\n");
    subString[0]=0x62;
    subString[1]=0x63;
    subString[2]=0;
    result=u_strstr(temp, subString);
    if(result != temp+2){
        log_err("There is an error in u_strstr() Expected match at position 2 Got %ld (pointer 0x%lx)\n", result-temp, result);
    }
    result=u_strstr(temp, subString+2); /* subString+2 is an empty string */
    if(result != temp){
        log_err("There is an error in u_strstr() Expected match at position 0 Got %ld (pointer 0x%lx)\n", result-temp, result);
    }
    result=u_strstr(subString, temp);
    if(result != NULL){
        log_err("There is an error in u_strstr() Expected NULL \"not found\" Got non-NULL \"found\" result\n");
    }

    /*Testing u_strchr32*/
    log_verbose("Testing u_strchr32\n");
    result=u_strchr32(temp, (UChar32)0x62);
    if(result != temp+1){
        log_err("There is an error in u_strchr32() Expected match at position 1 Got %ld (pointer 0x%lx)\n", result-temp, result);
    }
    result=u_strchr32(temp, (UChar32)0xfb);
    if(result != NULL){
        log_err("There is an error in u_strchr32() Expected NULL \"not found\" Got non-NULL \"found\" result\n");
    }
    result=u_strchr32(temp, (UChar32)0x20402);
    if(result != temp+5){
        log_err("There is an error in u_strchr32() Expected match at position 5 Got %ld (pointer 0x%lx)\n", result-temp, result);
    }

    temp[7]=0xfc00;
    result=u_memchr32(temp, (UChar32)0x20402, 7);
    if(result != temp+5){
        log_err("There is an error in u_memchr32() Expected match at position 5 Got %ld (pointer 0x%lx)\n", result-temp, result);
    }
    result=u_memchr32(temp, (UChar32)0x20402, 6);
    if(result != NULL){
        log_err("There is an error in u_memchr32() Expected no match Got %ld (pointer 0x%lx)\n", result-temp, result);
    }
    result=u_memchr32(temp, (UChar32)0x20402, 1);
    if(result != NULL){
        log_err("There is an error in u_memchr32() Expected no match Got %ld (pointer 0x%lx)\n", result-temp, result);
    }
    result=u_memchr32(temp, (UChar32)0xfc00, 8);
    if(result != temp+7){
        log_err("There is an error in u_memchr32() Expected match at position 7 Got %ld (pointer 0x%lx)\n", result-temp, result);
    }
}

/* test u_unescape() and u_unescapeAt() ------------------------------------- */

static void
TestUnescape() {
    static UChar buffer[200];
    static const UChar expect[]={
        0x53, 0x63, 0x68, 0xf6, 0x6e, 0x65, 0x73, 0x20, 0x41, 0x75, 0x74, 0x6f, 0x3a, 0x20,
        0x20ac, 0x20, 0x31, 0x31, 0x32, 0x34, 0x30, 0x2e, 0x0c,
        0x50, 0x72, 0x69, 0x76, 0x61, 0x74, 0x65, 0x73, 0x20,
        0x5a, 0x65, 0x69, 0x63, 0x68, 0x65, 0x6e, 0x3a, 0x20, 0xdbc8, 0xdf45, 0x0a, 0
    };
    int32_t length;

    /* test u_unescape() */
    length=u_unescape(
        "Sch\\u00f6nes Auto: \\u20ac 11240.\\fPrivates Zeichen: \\U00102345\\n",
        buffer, sizeof(buffer)/sizeof(buffer[0]));
    if(length!=44 || u_strcmp(buffer, expect)!=0) {
        log_err("failure in u_unescape(): length %d!=45 and/or incorrect result string\n", length);
    }

    /* try preflighting */
    length=u_unescape(
        "Sch\\u00f6nes Auto: \\u20ac 11240.\\fPrivates Zeichen: \\U00102345\\n",
        NULL, sizeof(buffer)/sizeof(buffer[0]));
    if(length!=44 || u_strcmp(buffer, expect)!=0) {
        log_err("failure in u_unescape(preflighting): length %d!=45\n", length);
    }

    /* ### TODO: test u_unescapeAt() */
}
