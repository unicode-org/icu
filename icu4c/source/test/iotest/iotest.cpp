/*
**********************************************************************
*   Copyright (C) 2002-2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   file name:  iotest.cpp
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2002feb21
*   created by: George Rhoten
*/


#include "unicode/ustdio.h"
#include "unicode/ustream.h"
#include "unicode/uclean.h"

#include "unicode/ucnv.h"
#include "unicode/uloc.h"
#include "unicode/unistr.h"
#include "unicode/ustring.h"
#include "unicode/ctest.h"
#include "unicode/utrans.h"
#include "ustr_imp.h"

#if U_IOSTREAM_SOURCE >= 199711
#include <iostream>
#include <strstream>
using namespace std;
#elif U_IOSTREAM_SOURCE >= 198506
#include <iostream.h>
#include <strstream.h>
#endif

#include <string.h>

static char STANDARD_TEST_FILE[] = "iotest-c.txt";
static const int32_t STANDARD_TEST_NUM_RANGE = 1000;

#ifdef WIN32
static const UChar NEW_LINE[] = {0x0d,0x0a,0};
#define C_NEW_LINE "\r\n"
#else
static const UChar NEW_LINE[] = {0x0a,0};
#define C_NEW_LINE "\n"
#endif

static void TestFileFromICU(UFILE *myFile) {
    int32_t n[1];
    float myFloat = -1234.0;
    UDate myDate = 0.0;
    UDate dec_31_1969 = -57600000.000000; /* TODO: These are not correct */
    UDate midnight = 86400000.000000; /* TODO: These are not correct */
    UDate myNewDate = -1.0;
    int32_t newValuePtr[1];
    double newDoubleValuePtr[1];
    UChar myUString[256];
    UChar uStringBuf[256];
    char myString[256] = "";
    char testBuf[256] = "";

    u_memset(myUString, 0x2a, sizeof(myUString)/sizeof(*myUString));
    u_memset(uStringBuf, 0x2a, sizeof(uStringBuf)/sizeof(*uStringBuf));
    memset(myString, 0x2a, sizeof(myString)/sizeof(*myString));
    memset(testBuf, 0x2a, sizeof(testBuf)/sizeof(*testBuf));

    if (myFile == NULL) {
        log_err("Can't write test file.");
        return;
    }

    *n = -1234;

    /* Test fprintf */
    u_fprintf(myFile, "Signed decimal integer %%d: %d\n", *n);
    u_fprintf(myFile, "Signed decimal integer %%i: %i\n", *n);
    u_fprintf(myFile, "Unsigned octal integer %%o: %o\n", *n);
    u_fprintf(myFile, "Unsigned decimal integer %%u: %u\n", *n);
    u_fprintf(myFile, "Lowercase unsigned hexadecimal integer %%x: %x\n", *n);
    u_fprintf(myFile, "Uppercase unsigned hexadecimal integer %%X: %X\n", *n);
    u_fprintf(myFile, "Float %%f: %f\n", myFloat);
    u_fprintf(myFile, "Lowercase float %%e: %e\n", myFloat);
    u_fprintf(myFile, "Uppercase float %%E: %E\n", myFloat);
    u_fprintf(myFile, "Lowercase float %%g: %g\n", myFloat);
    u_fprintf(myFile, "Uppercase float %%G: %G\n", myFloat);
//    u_fprintf(myFile, "Pointer %%p: %p\n", myFile);
    u_fprintf(myFile, "Char %%c: %c\n", 'A');
    u_fprintf(myFile, "UChar %%K (non-ANSI, should be %%C for Microsoft?): %K\n", L'A');
    u_fprintf(myFile, "String %%s: %s\n", "My-String");
    u_fprintf(myFile, "NULL String %%s: %s\n", NULL);
    u_fprintf(myFile, "Unicode String %%U (non-ANSI, should be %%S for Microsoft?): %U\n", L"My-String");
    u_fprintf(myFile, "NULL Unicode String %%U (non-ANSI, should be %%S for Microsoft?): %U\n", NULL);
    u_fprintf(myFile, "Date %%D (non-ANSI): %D\n", myDate);
    u_fprintf(myFile, "Time %%T (non-ANSI): %T\n", myDate);
    u_fprintf(myFile, "Percent %%P (non-ANSI): %P\n", myFloat);
    u_fprintf(myFile, "Currency %%M (non-ANSI): %M\n", myFloat);
    u_fprintf(myFile, "Spell Out %%V (non-ANSI): %V\n", myFloat);

    *n = 1;
    u_fprintf(myFile, "\t\nPointer to integer (Count) %%n: n=%d %n n=%d\n", *n, n, *n);
    u_fprintf(myFile, "Pointer to integer Value: %d\n", *n);
    u_fprintf(myFile, "This is a long test123456789012345678901234567890123456789012345678901234567890\n");
    *n = 1;
    fprintf(u_fgetfile(myFile), "\tNormal fprintf count: n=%d %n n=%d\n", *n ,n, *n);
    fprintf(u_fgetfile(myFile), "\tNormal fprintf count value: n=%d\n", *n);

    u_fclose(myFile);
    myFile = u_fopen(STANDARD_TEST_FILE, "r", NULL, NULL);

    if (myFile == NULL) {
        log_err("Can't read test file.");
        return;
    }

    *n = -1234;

    myString[0] = u_fgetc(myFile);
    if (myString[0] != 0x53 /* S */) {
        log_err("u_fgetc 1 returned %X. Expected 'S'.", myString[0]);
    }
    u_fungetc(myString[0], myFile);
    myString[0] = u_fgetc(myFile);
    if (myString[0] != 0x53 /* S */) {
        log_err("u_fgetc 2 returned %X. Expected 'S'.", myString[0]);
    }
    u_fungetc(myString[0], myFile);
    myString[0] = u_fgetc(myFile);
    if (myString[0] != 0x53 /* S */) {
        log_err("u_fgetc 3 returned %X. Expected 'S'.", myString[0]);
    }
    u_fungetc(myString[0], myFile);

    *newValuePtr = 1;
    u_fscanf(myFile, "Signed decimal integer %%d: %d\n", newValuePtr);
    if (*n != *newValuePtr) {
        log_err("%%d Got: %d, Expected: %d\n", *newValuePtr, *n);
    }
    *newValuePtr = 1;
    u_fscanf(myFile, "Signed decimal integer %%i: %i\n", newValuePtr);
    if (*n != *newValuePtr) {
        log_err("%%i Got: %i, Expected: %i\n", *newValuePtr, *n);
    }
    *newValuePtr = 1;
    u_fscanf(myFile, "Unsigned octal integer %%o: %o\n", newValuePtr);
    if (*n != *newValuePtr) {
        log_err("%%o Got: %o, Expected: %o\n", *newValuePtr, *n);
    }
    *newValuePtr = 1;
    u_fscanf(myFile, "Unsigned decimal integer %%u: %u\n", newValuePtr);
    if (*n != *newValuePtr) {
        log_err("%%u Got: %u, Expected: %u\n", *newValuePtr, *n);
    }
    *newValuePtr = 1;
    u_fscanf(myFile, "Lowercase unsigned hexadecimal integer %%x: %x\n", newValuePtr);
    if (*n != *newValuePtr) {
        log_err("%%x Got: %x, Expected: %x\n", *newValuePtr, *n);
    }
    *newValuePtr = 1;
    u_fscanf(myFile, "Uppercase unsigned hexadecimal integer %%X: %X\n", newValuePtr);
    if (*n != *newValuePtr) {
        log_err("%%X Got: %X, Expected: %X\n", *newValuePtr, *n);
    }
    *newDoubleValuePtr = -1.0;
    u_fscanf(myFile, "Float %%f: %f\n", newDoubleValuePtr);
    if (myFloat != *newDoubleValuePtr) {
        log_err("%%f Got: %f, Expected: %f\n", *newDoubleValuePtr, myFloat);
    }
    *newDoubleValuePtr = -1.0;
    u_fscanf(myFile, "Lowercase float %%e: %e\n", newDoubleValuePtr);
    if (myFloat != *newDoubleValuePtr) {
        log_err("%%e Got: %e, Expected: %e\n", *newDoubleValuePtr, myFloat);
    }
    *newDoubleValuePtr = -1.0;
    u_fscanf(myFile, "Uppercase float %%E: %E\n", newDoubleValuePtr);
    if (myFloat != *newDoubleValuePtr) {
        log_err("%%E Got: %E, Expected: %E\n", *newDoubleValuePtr, myFloat);
    }
    *newDoubleValuePtr = -1.0;
    u_fscanf(myFile, "Lowercase float %%g: %g\n", newDoubleValuePtr);
    if (myFloat != *newDoubleValuePtr) {
        log_err("%%g Got: %g, Expected: %g\n", *newDoubleValuePtr, myFloat);
    }
    *newDoubleValuePtr = -1.0;
    u_fscanf(myFile, "Uppercase float %%G: %G\n", newDoubleValuePtr);
    if (myFloat != *newDoubleValuePtr) {
        log_err("%%G Got: %G, Expected: %G\n", *newDoubleValuePtr, myFloat);
    }
//  u_fscanf(myFile, "Pointer %%p: %p\n", newDoubleValue);
    u_fscanf(myFile, "Char %%c: %c\n", myString);
    if (*myString != 'A') {
        log_err("%%c Got: %c, Expected: A\n", *myString);
    }
    u_fscanf(myFile, "UChar %%K (non-ANSI, should be %%C for Microsoft?): %K\n", myUString);
    if (*myUString != L'A') {
        log_err("%%C Got: %C, Expected: A\n", *myUString);
    }
    u_fscanf(myFile, "String %%s: %s\n", myString);
    if (strcmp(myString, "My-String")) {
        log_err("%%s Got: %s, Expected: My String\n", myString);
    }
    u_fscanf(myFile, "NULL String %%s: %s\n", myString);
    if (strcmp(myString, "(null)")) {
        log_err("%%s Got: %s, Expected: My String\n", myString);
    }
    u_fscanf(myFile, "Unicode String %%U (non-ANSI, should be %%S for Microsoft?): %U\n", myUString);
    u_austrncpy(myString, myUString, sizeof(myUString)/sizeof(*myUString));
    if (strcmp(myString, "My-String")) {
        log_err("%%S Got: %S, Expected: My String\n", myUString);
    }
    u_fscanf(myFile, "NULL Unicode String %%U (non-ANSI, should be %%S for Microsoft?): %U\n", myUString);
    u_austrncpy(myString, myUString, sizeof(myUString)/sizeof(*myUString));
    if (strcmp(myString, "(null)")) {
        log_err("%%S Got: %S, Expected: My String\n", myUString);
    }
    myNewDate = -1.0;
    u_fscanf(myFile, "Date %%D (non-ANSI): %D\n", &myNewDate);
    if (myNewDate != dec_31_1969) {
        log_err("%%D Got: %f, Expected: %f\n", myNewDate, dec_31_1969);
    }
    myNewDate = -1.0;
    u_fscanf(myFile, "Time %%T (non-ANSI): %T\n", &myNewDate);
    if (myNewDate != midnight) {
        log_err("%%T Got: %f, Expected: %f\n", myNewDate, midnight);
    }
    *newDoubleValuePtr = -1.0;
    u_fscanf(myFile, "Percent %%P (non-ANSI): %P\n", newDoubleValuePtr);
    if (myFloat != *newDoubleValuePtr) {
        log_err("%%P Got: %f, Expected: %f\n", *newDoubleValuePtr, myFloat);
    }
    *newDoubleValuePtr = -1.0;
    u_fscanf(myFile, "Currency %%M (non-ANSI): %M\n", newDoubleValuePtr);
    if (myFloat != *newDoubleValuePtr) {
        log_err("%%P Got: %f, Expected: %f\n", *newDoubleValuePtr, myFloat);
    }
    *newDoubleValuePtr = -1.0;
    u_fscanf(myFile, "Spell Out %%V (non-ANSI): %V\n", newDoubleValuePtr);
    if (myFloat != *newDoubleValuePtr) {
        log_err("%%V Got: %f, Expected: %f\n", *newDoubleValuePtr, myFloat);
    }

    u_fgets(myFile, 4, myUString);
    u_austrncpy(myString, myUString, sizeof(myUString)/sizeof(*myUString));
    if (myString == NULL || strcmp(myString, "\t\n") != 0) {
        log_err("u_fgets got \"%s\"\n", myString);
    }

    if (u_fgets(myFile, sizeof(myUString)/sizeof(*myUString), myUString) != myUString) {
        log_err("u_fgets did not return myUString\n");
    }
    u_austrncpy(myString, myUString, sizeof(myUString)/sizeof(*myUString));
    if (myString == NULL || strcmp(myString, "Pointer to integer (Count) %n: n=1  n=1\n") != 0) {
        log_err("u_fgets got \"%s\"\n", myString);
    }

    if (u_fgets(myFile, sizeof(myUString)/sizeof(*myUString), myUString) != myUString) {
        log_err("u_fgets did not return myUString\n");
    }
    u_austrncpy(myString, myUString, sizeof(myUString)/sizeof(*myUString));
    if (myString == NULL || strcmp(myString, "Pointer to integer Value: 37\n") != 0) {
        log_err("u_fgets got \"%s\"\n", myString);
    }

    if (u_fgets(myFile, sizeof(myUString)/sizeof(*myUString), myUString) != myUString) {
        log_err("u_fgets did not return myUString\n");
    }
    u_austrncpy(myString, myUString, sizeof(myUString)/sizeof(*myUString));
    if (myString == NULL || strcmp(myString, "This is a long test123456789012345678901234567890123456789012345678901234567890\n") != 0) {
        log_err("u_fgets got \"%s\"\n", myString);
    }

    if (u_fgets(myFile, 0, myUString) != NULL) {
        log_err("u_fgets got \"%s\" and it should have returned NULL\n", myString);
    }

    if (u_fgets(myFile, 1, myUString) != myUString) {
        log_err("u_fgets did not return myUString\n");
    }
    u_austrncpy(myString, myUString, sizeof(myUString)/sizeof(*myUString));
    if (myString == NULL || strcmp(myString, "") != 0) {
        log_err("u_fgets got \"%s\"\n", myString);
    }

    if (u_fgets(myFile, 2, myUString) != myUString) {
        log_err("u_fgets did not return myUString\n");
    }
    u_austrncpy(myString, myUString, sizeof(myUString)/sizeof(*myUString));
    if (myString == NULL || strcmp(myString, "\t") != 0) {
        log_err("u_fgets got \"%s\"\n", myString);
    }

    u_fclose(myFile);
}

static void TestFile() {
/*    FILE *standardFile;*/

    log_verbose("Testing u_fopen\n");
    TestFileFromICU(u_fopen(STANDARD_TEST_FILE, "w", NULL, NULL));

/* Don't know how to make this work without stdout or stderr */
/*
    log_verbose("Testing u_finit\n");
    standardFile = fopen(STANDARD_TEST_FILE, "wb");
    TestFileFromICU(u_finit(standardFile, NULL, NULL));
    fclose(standardFile);
*/
}

static void TestCodepageAndLocale() {
    UFILE *myFile = u_fopen(STANDARD_TEST_FILE, "w", NULL, NULL);
    if (u_fgetcodepage(myFile) == NULL
        || strcmp(u_fgetcodepage(myFile), ucnv_getDefaultName()) != 0)
    {
        log_err("Didn't get the proper default codepage. Got %s expected: %s\n",
            u_fgetcodepage(myFile), ucnv_getDefaultName());
    }
    if (u_fgetlocale(myFile) == NULL
        || strcmp(u_fgetlocale(myFile), uloc_getDefault()) != 0)
    {
        log_err("Didn't get the proper default locale. Got %s expected: %s\n",
            u_fgetlocale(myFile), uloc_getDefault());
    }
    u_fclose(myFile);

    myFile = u_fopen(STANDARD_TEST_FILE, "w", "es", NULL);
    if (u_fgetcodepage(myFile) == NULL
        || strcmp(u_fgetcodepage(myFile), "ISO-8859-1") != 0)
    {
        log_err("Didn't get the proper default codepage for \"en\". Got %s expected: iso-8859-1\n",
            u_fgetcodepage(myFile));
    }
    if (u_fgetlocale(myFile) == NULL
        || strcmp(u_fgetlocale(myFile), "es") != 0)
    {
        log_err("Didn't get the proper default locale. Got %s expected: %s\n",
            u_fgetlocale(myFile), "es");
    }
    u_fclose(myFile);

    myFile = u_fopen(STANDARD_TEST_FILE, "w", NULL, "UTF-16");
    if (u_fgetcodepage(myFile) == NULL
        || strcmp(u_fgetcodepage(myFile), "UTF-16") != 0)
    {
        log_err("Didn't get the proper default codepage for \"en\". Got %s expected: iso-8859-1\n",
            u_fgetcodepage(myFile));
    }
    if (u_fgetlocale(myFile) == NULL
        || strcmp(u_fgetlocale(myFile), uloc_getDefault()) != 0)
    {
        log_err("Didn't get the proper default locale. Got %s expected: %s\n",
            u_fgetlocale(myFile), uloc_getDefault());
    }
    u_fclose(myFile);

    myFile = u_fopen(STANDARD_TEST_FILE, "w", "zh", "UTF-16");
    if (u_fgetcodepage(myFile) == NULL
        || strcmp(u_fgetcodepage(myFile), "UTF-16") != 0)
    {
        log_err("Didn't get the proper default codepage for \"en\". Got %s expected: iso-8859-1\n",
            u_fgetcodepage(myFile));
    }
    if (u_fgetlocale(myFile) == NULL
        || strcmp(u_fgetlocale(myFile), "zh") != 0)
    {
        log_err("Didn't get the proper default locale. Got %s expected: %s\n",
            u_fgetlocale(myFile), "zh");
    }
    u_fclose(myFile);
}


static void TestfgetsBuffers() {
    UChar buffer[2048];
    UChar expectedBuffer[2048];
    static const char testStr[] = "This is a test string that tests u_fgets. It makes sure that we don't try to read too much!";
    UFILE *myFile = u_fopen(STANDARD_TEST_FILE, "w", NULL, "UTF-16");
    int32_t expectedSize = strlen(testStr);
    int32_t readSize;
    int32_t repetitions;

    u_fputc(0x3BC, myFile);
    u_fputc(0xFF41, myFile);
    u_memset(buffer, 0xDEAD, sizeof(buffer)/sizeof(buffer[0]));
    u_memset(expectedBuffer, 0, sizeof(expectedBuffer)/sizeof(expectedBuffer[0]));
    u_uastrncpy(buffer, testStr, expectedSize+1);
    for (repetitions = 0; repetitions < 16; repetitions++) {
        u_file_write(buffer, expectedSize, myFile);
        u_strcat(expectedBuffer, buffer);
    }
    u_fclose(myFile);

    u_memset(buffer, 0xDEAD, sizeof(buffer)/sizeof(buffer[0]));
    myFile = u_fopen(STANDARD_TEST_FILE, "r", NULL, "UTF-16");
    if (u_fgetc(myFile) != 0x3BC) {
        log_err("The first character is wrong\n");
    }
    if (u_fgetc(myFile) != 0xFF41) {
        log_err("The second character is wrong\n");
    }
    if (u_fgets(myFile, sizeof(buffer)/sizeof(buffer[0]), buffer) != buffer) {
        log_err("Didn't get the buffer back\n");
        return;
    }
    readSize = u_strlen(buffer);
    if (readSize != expectedSize*repetitions) {
        log_err("Buffer is the wrong size. Got %d Expected %d\n", u_strlen(buffer), expectedSize*repetitions);
    }
    if (buffer[(expectedSize*repetitions) + 1] != 0xDEAD) {
        log_err("u_fgets wrote too much data\n");
    }
    if (u_strcmp(buffer, expectedBuffer) != 0) {
        log_err("Did get expected string back\n");
    }
    if (strcmp(u_fgetcodepage(myFile), "UTF-16") != 0) {
        log_err("Got %s instead of UTF-16\n", u_fgetcodepage(myFile));
    }
    u_fclose(myFile);


    log_verbose("Now trying a multi-byte encoding (UTF-8).\n");

    myFile = u_fopen(STANDARD_TEST_FILE, "w", NULL, "UTF-8");

    u_fputc(0x3BC, myFile);
    u_fputc(0xFF41, myFile);
    u_memset(buffer, 0xDEAD, sizeof(buffer)/sizeof(buffer[0]));
    u_memset(expectedBuffer, 0, sizeof(expectedBuffer)/sizeof(expectedBuffer[0]));
    u_uastrncpy(buffer, testStr, expectedSize+1);
    for (repetitions = 0; repetitions < 16; repetitions++) {
        u_file_write(buffer, expectedSize, myFile);
        u_strcat(expectedBuffer, buffer);
    }
    u_fclose(myFile);

    u_memset(buffer, 0xDEAD, sizeof(buffer)/sizeof(buffer[0]));
    myFile = u_fopen(STANDARD_TEST_FILE, "r", NULL, "UTF-8");
    if (strcmp(u_fgetcodepage(myFile), "UTF-8") != 0) {
        log_err("Got %s instead of UTF-8\n", u_fgetcodepage(myFile));
    }
    if (u_fgetc(myFile) != 0x3BC) {
        log_err("The first character is wrong\n");
    }
    if (u_fgetc(myFile) != 0xFF41) {
        log_err("The second character is wrong\n");
    }
    if (u_fgets(myFile, sizeof(buffer)/sizeof(buffer[0]), buffer) != buffer) {
        log_err("Didn't get the buffer back\n");
        return;
    }
    readSize = u_strlen(buffer);
    if (readSize != expectedSize*repetitions) {
        log_err("Buffer is the wrong size. Got %d Expected %d\n", u_strlen(buffer), expectedSize*repetitions);
    }
    if (buffer[(expectedSize*repetitions) + 1] != 0xDEAD) {
        log_err("u_fgets wrote too much data\n");
    }
    if (u_strcmp(buffer, expectedBuffer) != 0) {
        log_err("Did get expected string back\n");
    }
    u_fclose(myFile);


    log_verbose("Now trying a multi-byte encoding (UTF-8) with a really small buffer.\n");

    myFile = u_fopen(STANDARD_TEST_FILE, "w", NULL, "UTF-8");

    u_fputc(0xFF41, myFile);
    u_memset(buffer, 0xDEAD, sizeof(buffer)/sizeof(buffer[0]));
    u_memset(expectedBuffer, 0, sizeof(expectedBuffer)/sizeof(expectedBuffer[0]));
    u_uastrncpy(buffer, testStr, expectedSize+1);
    for (repetitions = 0; repetitions < 1; repetitions++) {
        u_file_write(buffer, expectedSize, myFile);
        u_strcat(expectedBuffer, buffer);
    }
    u_fclose(myFile);

    u_memset(buffer, 0xDEAD, sizeof(buffer)/sizeof(buffer[0]));
    myFile = u_fopen(STANDARD_TEST_FILE, "r", NULL, "UTF-8");
    if (u_fgets(myFile, 2, buffer) != buffer) {
        log_err("Didn't get the buffer back\n");
        return;
    }
    readSize = u_strlen(buffer);
    if (readSize != 1) {
        log_err("Buffer is the wrong size. Got %d Expected %d\n", u_strlen(buffer), 1);
    }
    if (buffer[0] != 0xFF41 || buffer[1] != 0) {
        log_err("Did get expected string back\n");
    }
    if (buffer[2] != 0xDEAD) {
        log_err("u_fgets wrote too much data\n");
    }
    u_fclose(myFile);

}


static void TestfgetsLineCount() {
    UChar buffer[2048];
    UChar expectedBuffer[2048];
    char charBuffer[2048];
    static const char testStr[] = "This is a test string that tests u_fgets. It makes sure that we don't try to read too much!";
    UFILE *myFile = NULL;
    FILE *stdFile = fopen(STANDARD_TEST_FILE, "w");
    int32_t expectedSize = strlen(testStr);
    int32_t repetitions;
    int32_t nlRepetitions;

    u_memset(expectedBuffer, 0, sizeof(expectedBuffer)/sizeof(expectedBuffer[0]));

    for (repetitions = 0; repetitions < 16; repetitions++) {
        fwrite(testStr, sizeof(testStr[0]), expectedSize, stdFile);
        for (nlRepetitions = 0; nlRepetitions < repetitions; nlRepetitions++) {
            fwrite("\n", sizeof(testStr[0]), 1, stdFile);
        }
    }
    fclose(stdFile);

    myFile = u_fopen(STANDARD_TEST_FILE, "r", NULL, NULL);
    stdFile = fopen(STANDARD_TEST_FILE, "r");

    for (;;) {
        u_memset(buffer, 0xDEAD, sizeof(buffer)/sizeof(buffer[0]));
        char *returnedCharBuffer = fgets(charBuffer, sizeof(charBuffer)/sizeof(charBuffer[0]), stdFile);
        UChar *returnedUCharBuffer = u_fgets(myFile, sizeof(buffer)/sizeof(buffer[0]), buffer);

        if (!returnedCharBuffer && !returnedUCharBuffer) {
            /* Both returned NULL. stop. */
            break;
        }
        if (returnedCharBuffer != charBuffer) {
            log_err("Didn't get the charBuffer back\n");
            continue;
        }
        u_uastrncpy(expectedBuffer, charBuffer, strlen(charBuffer)+1);
        if (returnedUCharBuffer != buffer) {
            log_err("Didn't get the buffer back\n");
            continue;
        }
        if (u_strcmp(buffer, expectedBuffer) != 0) {
            log_err("buffers are different\n");
        }
        if (buffer[u_strlen(buffer)+1] != 0xDEAD) {
            log_err("u_fgets wrote too much\n");
        }
    }
    fclose(stdFile);
    u_fclose(myFile);
}

static void TestFilePrintCompatibility() {
    UFILE *myFile = u_fopen(STANDARD_TEST_FILE, "wb", "en_US_POSIX", NULL);
    FILE *myCFile;
    int32_t num;
    char cVal;
    static const UChar emptyStr[] = {0};
    char readBuf[512] = "";
    char testBuf[512] = "";

    if (myFile == NULL) {
        log_err("Can't read test file.");
        return;
    }

    if (strcmp(u_fgetlocale(myFile), "en_US_POSIX") != 0) {
        log_err("Got %s instead of en_US_POSIX for locale\n", u_fgetlocale(myFile));
    }

    /* Compare against C API compatibility */
    for (num = -STANDARD_TEST_NUM_RANGE; num < STANDARD_TEST_NUM_RANGE; num++) {
        u_fprintf(myFile, "%x ", num);
        u_fprintf(myFile, "%X ", num);
        u_fprintf(myFile, "%o ", num);
        u_fprintf(myFile, "%d ", num);
        u_fprintf(myFile, "%i ", num);
        u_fprintf(myFile, "%f ", (double)num);
        u_fprintf(myFile, "%e ", (double)num);
        u_fprintf(myFile, "%E ", (double)num);
        u_fprintf(myFile, "%g ", (double)num);
        u_fprintf(myFile, "%G", (double)num);
        u_fputs(emptyStr, myFile);
    }

    u_fprintf_u(myFile, NEW_LINE);

    for (num = 0; num < 0x80; num++) {
        u_fprintf(myFile, "%c", num);
    }

    u_fclose(myFile);
    myCFile = fopen(STANDARD_TEST_FILE, "rb");
    if (myCFile == NULL) {
        log_err("Can't read test file.");
        return;
    }

    for (num = -STANDARD_TEST_NUM_RANGE; num < STANDARD_TEST_NUM_RANGE; num++) {
        fscanf(myCFile, "%s", readBuf);
        sprintf(testBuf, "%x", num);
        if (strcmp(readBuf, testBuf) != 0) {
            log_err("%%x Got: \"%s\", Expected: \"%s\"\n", readBuf, testBuf);
        }

        fscanf(myCFile, "%s", readBuf);
        sprintf(testBuf, "%X", num);
        if (strcmp(readBuf, testBuf) != 0) {
            log_err("%%X Got: \"%s\", Expected: \"%s\"\n", readBuf, testBuf);
        }

        fscanf(myCFile, "%s", readBuf);
        sprintf(testBuf, "%o", num);
        if (strcmp(readBuf, testBuf) != 0) {
            log_err("%%o Got: \"%s\", Expected: \"%s\"\n", readBuf, testBuf);
        }

        /* fprintf is not compatible on all platforms e.g. the iSeries */
        fscanf(myCFile, "%s", readBuf);
        sprintf(testBuf, "%d", num);
        if (strcmp(readBuf, testBuf) != 0) {
            log_err("%%d Got: \"%s\", Expected: \"%s\"\n", readBuf, testBuf);
        }

        fscanf(myCFile, "%s", readBuf);
        sprintf(testBuf, "%i", num);
        if (strcmp(readBuf, testBuf) != 0) {
            log_err("%%i Got: \"%s\", Expected: \"%s\"\n", readBuf, testBuf);
        }

        fscanf(myCFile, "%s", readBuf);
        sprintf(testBuf, "%f", (double)num);
        if (strcmp(readBuf, testBuf) != 0) {
            log_err("%%f Got: \"%s\", Expected: \"%s\"\n", readBuf, testBuf);
        }

        fscanf(myCFile, "%s", readBuf);
        sprintf(testBuf, "%e", (double)num);
        if (strcmp(readBuf, testBuf) != 0) {
            log_err("%%e Got: \"%s\", Expected: \"%s\"\n", readBuf, testBuf);
        }

        fscanf(myCFile, "%s", readBuf);
        sprintf(testBuf, "%E", (double)num);
        if (strcmp(readBuf, testBuf) != 0) {
            log_err("%%E Got: \"%s\", Expected: \"%s\"\n", readBuf, testBuf);
        }

        fscanf(myCFile, "%s", readBuf);
        sprintf(testBuf, "%g", (double)num);
        if (strcmp(readBuf, testBuf) != 0) {
            log_err("%%g Got: \"%s\", Expected: \"%s\"\n", readBuf, testBuf);
        }

        fscanf(myCFile, "%s", readBuf);
        sprintf(testBuf, "%G", (double)num);
        if (strcmp(readBuf, testBuf) != 0) {
            log_err("%%G Got: \"%s\", Expected: \"%s\"\n", readBuf, testBuf);
        }
    }

    /* Properly eat the newlines */
    for (num = 0; num < u_strlen(NEW_LINE); num++) {
        fscanf(myCFile, "%c", &cVal);
        if (cVal != NEW_LINE[num]) {
            log_err("OS newline error\n");
        }
    }
    for (num = 0; num < u_strlen(NEW_LINE); num++) {
        fscanf(myCFile, "%c", &cVal);
        if (cVal != NEW_LINE[num]) {
            log_err("ustdio newline error\n");
        }
    }

    for (num = 0; num < 0x80; num++) {
        cVal = -1;
        fscanf(myCFile, "%c", &cVal);
        if (num != cVal) {
            log_err("%%c Got: 0x%x, Expected: 0x%x\n", cVal, num);
        }
    }
    fclose(myCFile);
}

#define TestFPrintFormat(uFormat, uValue, cFormat, cValue) \
    myFile = u_fopen(STANDARD_TEST_FILE, "w", "en_US_POSIX", NULL);\
    /* Reinitialize the buffer to verify null termination works. */\
    u_memset(uBuffer, 0x2a, sizeof(uBuffer)/sizeof(*uBuffer));\
    memset(buffer, 0x2a, sizeof(buffer)/sizeof(*buffer));\
    \
    uNumPrinted = u_fprintf(myFile, uFormat, uValue);\
    u_fclose(myFile);\
    myFile = u_fopen(STANDARD_TEST_FILE, "r", "en_US_POSIX", NULL);\
    u_fgets(myFile, sizeof(uBuffer)/sizeof(*uBuffer), uBuffer);\
    u_fclose(myFile);\
    u_austrncpy(compBuffer, uBuffer, sizeof(uBuffer)/sizeof(*uBuffer));\
    cNumPrinted = sprintf(buffer, cFormat, cValue);\
    if (strcmp(buffer, compBuffer) != 0) {\
        log_err("%" uFormat " Got: \"%s\", Expected: \"%s\"\n", compBuffer, buffer);\
    }\
    if (cNumPrinted != uNumPrinted) {\
        log_err("%" uFormat " number printed Got: %d, Expected: %d\n", uNumPrinted, cNumPrinted);\
    }\
    if (buffer[uNumPrinted+1] != 0x2a) {\
        log_err("%" uFormat " too much stored\n");\
    }\

#define TestFPrintFormat2(format, precision, value) \
    myFile = u_fopen(STANDARD_TEST_FILE, "w", "en_US_POSIX", NULL);\
    /* Reinitialize the buffer to verify null termination works. */\
    u_memset(uBuffer, 0x2a, sizeof(uBuffer)/sizeof(*uBuffer));\
    memset(buffer, 0x2a, sizeof(buffer)/sizeof(*buffer));\
    \
    uNumPrinted = u_fprintf(myFile, format, precision, value);\
    u_fclose(myFile);\
    myFile = u_fopen(STANDARD_TEST_FILE, "r", "en_US_POSIX", NULL);\
    u_fgets(myFile, sizeof(uBuffer)/sizeof(*uBuffer), uBuffer);\
    u_fclose(myFile);\
    u_austrncpy(compBuffer, uBuffer, sizeof(uBuffer)/sizeof(*uBuffer));\
    cNumPrinted = sprintf(buffer, format, precision, value);\
    if (strcmp(buffer, compBuffer) != 0) {\
        log_err("%" format " Got: \"%s\", Expected: \"%s\"\n", compBuffer, buffer);\
    }\
    if (cNumPrinted != uNumPrinted) {\
        log_err("%" format " number printed Got: %d, Expected: %d\n", uNumPrinted, cNumPrinted);\
    }\

static void TestFprintfFormat() {
    static const UChar abcUChars[] = {0x61,0x62,0x63,0};
    static const char abcChars[] = "abc";
    UChar uBuffer[256];
    char buffer[256];
    char compBuffer[256];
    int32_t uNumPrinted;
    int32_t cNumPrinted;
    UFILE *myFile;

    TestFPrintFormat("%8U", abcUChars, "%8s", abcChars);
    TestFPrintFormat("%-8U", abcUChars, "%-8s", abcChars);

    TestFPrintFormat("%8s", abcChars, "%8s", abcChars);
    TestFPrintFormat("%-8s", abcChars, "%-8s", abcChars);

    TestFPrintFormat("%8c", 0x65, "%8c", 0x65);
    TestFPrintFormat("%-8c", 0x65, "%-8c", 0x65);

    TestFPrintFormat("%8K", (UChar)0x65, "%8c", (char)0x65);
    TestFPrintFormat("%-8K", (UChar)0x65, "%-8c", (char)0x65);

    TestFPrintFormat("%10f", 1.23456789, "%10f", 1.23456789);
    TestFPrintFormat("%-10f", 1.23456789, "%-10f", 1.23456789);
    TestFPrintFormat("%10f", 123.456789, "%10f", 123.456789);
    TestFPrintFormat("%-10f", 123.456789, "%-10f", 123.456789);

    TestFPrintFormat("%e", 1234567.89, "%e", 1234567.89);
    TestFPrintFormat("%E", 1234567.89, "%E", 1234567.89);
    TestFPrintFormat("%10e", 1.23456789, "%10e", 1.23456789);
    TestFPrintFormat("%-10e", 1.23456789, "%-10e", 1.23456789);
    TestFPrintFormat("%10e", 1234.56789, "%10e", 1234.56789);
    TestFPrintFormat("%-10e", 1234.56789, "%-10e", 1234.56789);

    TestFPrintFormat("%g", 123456.789, "%g", 123456.789);
    TestFPrintFormat("%g", 1234567.89, "%g", 1234567.89);
    TestFPrintFormat("%G", 1234567.89, "%G", 1234567.89);
    TestFPrintFormat("%10g", 1.23456789, "%10g", 1.23456789);
    TestFPrintFormat("%-10g", 1.23456789, "%-10g", 1.23456789);
    TestFPrintFormat("%10g", 123.456789, "%10g", 123.456789);
    TestFPrintFormat("%-10g", 123.456789, "%-10g", 123.456789);

    TestFPrintFormat("%8x", 123456, "%8x", 123456);
    TestFPrintFormat("%-8x", 123456, "%-8x", 123456);

    TestFPrintFormat("%8X", 123456, "%8X", 123456);
    TestFPrintFormat("%-8X", 123456, "%-8X", 123456);
    TestFPrintFormat("%#x", 123456, "%#x", 123456);
    TestFPrintFormat("%#x", -123456, "%#x", -123456);

    TestFPrintFormat("%8o", 123456, "%8o", 123456);
    TestFPrintFormat("%-8o", 123456, "%-8o", 123456);
    TestFPrintFormat("%#o", 123, "%#o", 123);
    TestFPrintFormat("%#o", -123, "%#o", -123);

    TestFPrintFormat("%8u", 123456, "%8u", 123456);
    TestFPrintFormat("%-8u", 123456, "%-8u", 123456);
    TestFPrintFormat("%8u", -123456, "%8u", -123456);
    TestFPrintFormat("%-8u", -123456, "%-8u", -123456);

    TestFPrintFormat("%8d", 123456, "%8d", 123456);
    TestFPrintFormat("%-8d", 123456, "%-8d", 123456);
    TestFPrintFormat("% d", 123456, "% d", 123456);
    TestFPrintFormat("% d", -123456, "% d", -123456);

    TestFPrintFormat("%8i", 123456, "%8i", 123456);
    TestFPrintFormat("%-8i", 123456, "%-8i", 123456);

    TestFPrintFormat2("%+1.*e", 4, 1.2345678);
    TestFPrintFormat2("%+2.*e", 6, 1.2345678);

    log_verbose("Get really crazy with the formatting.\n");

    TestFPrintFormat("%-#12x", 123, "%-#12x", 123);
    TestFPrintFormat("%-#12x", -123, "%-#12x", -123);
    TestFPrintFormat("%#12x", 123, "%#12x", 123);
    TestFPrintFormat("%#12x", -123, "%#12x", -123);

    TestFPrintFormat("%-+12d", 123,  "%-+12d", 123);
    TestFPrintFormat("%-+12d", -123, "%-+12d", -123);
    TestFPrintFormat("%- 12d", 123,  "%- 12d", 123);
    TestFPrintFormat("%- 12d", -123, "%- 12d", -123);
    TestFPrintFormat("%+12d", 123,   "%+12d", 123);
    TestFPrintFormat("%+12d", -123,  "%+12d", -123);
    TestFPrintFormat("% 12d", 123,   "% 12d", 123);
    TestFPrintFormat("% 12d", -123,  "% 12d", -123);
    TestFPrintFormat("%12d", 123,    "%12d", 123);
    TestFPrintFormat("%12d", -123,   "%12d", -123);

    TestFPrintFormat("%-+12.1e", 1.234,  "%-+12.1e", 1.234);
    TestFPrintFormat("%-+12.1e", -1.234, "%-+12.1e", -1.234);
    TestFPrintFormat("%- 12.1e", 1.234,  "%- 12.1e", 1.234);
    TestFPrintFormat("%- 12.1e", -1.234, "%- 12.1e", -1.234);
    TestFPrintFormat("%+12.1e", 1.234,   "%+12.1e", 1.234);
    TestFPrintFormat("%+12.1e", -1.234,  "%+12.1e", -1.234);
    TestFPrintFormat("% 12.1e", 1.234,   "% 12.1e", 1.234);
    TestFPrintFormat("% 12.1e", -1.234,  "% 12.1e", -1.234);
    TestFPrintFormat("%12.1e", 1.234,    "%12.1e", 1.234);
    TestFPrintFormat("%12.1e", -1.234,   "%12.1e", -1.234);
    TestFPrintFormat("%.2e", 1.234,      "%.2e", 1.234);
    TestFPrintFormat("%.2e", -1.234,     "%.2e", -1.234);
    TestFPrintFormat("%3e", 1.234,       "%3e", 1.234);
    TestFPrintFormat("%3e", -1.234,      "%3e", -1.234);

    TestFPrintFormat("%-+12.1f", 1.234,  "%-+12.1f", 1.234);
    TestFPrintFormat("%-+12.1f", -1.234, "%-+12.1f", -1.234);
    TestFPrintFormat("%- 12.1f", 1.234,  "%- 12.1f", 1.234);
    TestFPrintFormat("%- 12.1f", -1.234, "%- 12.1f", -1.234);
    TestFPrintFormat("%+12.1f", 1.234,   "%+12.1f", 1.234);
    TestFPrintFormat("%+12.1f", -1.234,  "%+12.1f", -1.234);
    TestFPrintFormat("% 12.1f", 1.234,   "% 12.1f", 1.234);
    TestFPrintFormat("% 12.1f", -1.234,  "% 12.1f", -1.234);
    TestFPrintFormat("%12.1f", 1.234,    "%12.1f", 1.234);
    TestFPrintFormat("%12.1f", -1.234,   "%12.1f", -1.234);
    TestFPrintFormat("%.2f", 1.234,      "%.2f", 1.234);
    TestFPrintFormat("%.2f", -1.234,     "%.2f", -1.234);
    TestFPrintFormat("%3f", 1.234,       "%3f", 1.234);
    TestFPrintFormat("%3f", -1.234,      "%3f", -1.234);

}

#undef TestFPrintFormat

static void TestString() {
    int32_t n[1];
    float myFloat = -1234.0;
    UDate myDate = 0.0;
    UDate dec_31_1969 = -57600000.000000; /* TODO: These are not correct */
    UDate midnight = 86400000.000000; /* TODO: These are not correct */
    UDate myNewDate = -1.0;
    int32_t newValuePtr[1];
    double newDoubleValuePtr[1];
    UChar myUString[512];
    UChar uStringBuf[512];
    char myString[512] = "";
    char testBuf[512] = "";

    u_memset(myUString, 0x0a, sizeof(myUString)/ sizeof(*myUString));
    u_memset(uStringBuf, 0x0a, sizeof(uStringBuf) / sizeof(*uStringBuf));

    *n = -1234;

    /* Test sprintf */
    u_sprintf(uStringBuf, NULL, "Signed decimal integer d: %d", *n);
    *newValuePtr = 1;
    u_sscanf(uStringBuf, NULL, "Signed decimal integer d: %d", newValuePtr);
    if (*n != *newValuePtr) {
        log_err("%%d Got: %d, Expected: %d\n", *newValuePtr, *n);
    }

    u_sprintf(uStringBuf, NULL, "Signed decimal integer i: %i", *n);
    *newValuePtr = 1;
    u_sscanf(uStringBuf, NULL, "Signed decimal integer i: %i", newValuePtr);
    if (*n != *newValuePtr) {
        log_err("%%i Got: %i, Expected: %i\n", *newValuePtr, *n);
    }

    u_sprintf(uStringBuf, NULL, "Unsigned octal integer o: %o", *n);
    *newValuePtr = 1;
    u_sscanf(uStringBuf, NULL, "Unsigned octal integer o: %o", newValuePtr);
    if (*n != *newValuePtr) {
        log_err("%%o Got: %o, Expected: %o\n", *newValuePtr, *n);
    }

    u_sprintf(uStringBuf, NULL, "Unsigned decimal integer %%u: %u", *n);
    *newValuePtr = 1;
    u_sscanf(uStringBuf, NULL, "Unsigned decimal integer %%u: %u", newValuePtr);
    if (*n != *newValuePtr) {
        log_err("%%u Got: %u, Expected: %u\n", *newValuePtr, *n);
    }

    u_sprintf(uStringBuf, NULL, "Lowercase unsigned hexadecimal integer x: %x", *n);
    *newValuePtr = 1;
    u_sscanf(uStringBuf, NULL, "Lowercase unsigned hexadecimal integer x: %x", newValuePtr);
    if (*n != *newValuePtr) {
        log_err("%%x Got: %x, Expected: %x\n", *newValuePtr, *n);
    }

    u_sprintf(uStringBuf, NULL, "Uppercase unsigned hexadecimal integer X: %X", *n);
    *newValuePtr = 1;
    u_sscanf(uStringBuf, NULL, "Uppercase unsigned hexadecimal integer X: %X", newValuePtr);
    if (*n != *newValuePtr) {
        log_err("%%X Got: %X, Expected: %X\n", *newValuePtr, *n);
    }

    u_sprintf(uStringBuf, NULL, "Float f: %f", myFloat);
    *newDoubleValuePtr = -1.0;
    u_sscanf(uStringBuf, NULL, "Float f: %f", newDoubleValuePtr);
    if (myFloat != *newDoubleValuePtr) {
        log_err("%%f Got: %f, Expected: %f\n", *newDoubleValuePtr, myFloat);
    }

    u_sprintf(uStringBuf, NULL, "Lowercase float e: %e", myFloat);
    *newDoubleValuePtr = -1.0;
    u_sscanf(uStringBuf, NULL, "Lowercase float e: %e", newDoubleValuePtr);
    if (myFloat != *newDoubleValuePtr) {
        log_err("%%e Got: %e, Expected: %e\n", *newDoubleValuePtr, myFloat);
    }

    u_sprintf(uStringBuf, NULL, "Uppercase float E: %E", myFloat);
    *newDoubleValuePtr = -1.0;
    u_sscanf(uStringBuf, NULL, "Uppercase float E: %E", newDoubleValuePtr);
    if (myFloat != *newDoubleValuePtr) {
        log_err("%%E Got: %E, Expected: %E\n", *newDoubleValuePtr, myFloat);
    }

    u_sprintf(uStringBuf, NULL, "Lowercase float g: %g", myFloat);
    *newDoubleValuePtr = -1.0;
    u_sscanf(uStringBuf, NULL, "Lowercase float g: %g", newDoubleValuePtr);
    if (myFloat != *newDoubleValuePtr) {
        log_err("%%g Got: %g, Expected: %g\n", *newDoubleValuePtr, myFloat);
    }

    u_sprintf(uStringBuf, NULL, "Uppercase float G: %G", myFloat);
    *newDoubleValuePtr = -1.0;
    u_sscanf(uStringBuf, NULL, "Uppercase float G: %G", newDoubleValuePtr);
    if (myFloat != *newDoubleValuePtr) {
        log_err("%%G Got: %G, Expected: %G\n", *newDoubleValuePtr, myFloat);
    }

//    u_sprintf(uStringBuf, NULL, "Pointer %%p: %p\n", myFile);
    u_sprintf(uStringBuf, NULL, "Char c: %c", 'A');
    u_sscanf(uStringBuf, NULL, "Char c: %c", myString);
    if (*myString != 'A') {
        log_err("%%c Got: %c, Expected: A\n", *myString);
    }

    u_sprintf(uStringBuf, NULL, "UChar %%K (non-ANSI, should be %%C for Microsoft?): %K", L'A');
    u_sscanf(uStringBuf, NULL, "UChar %%K (non-ANSI, should be %%C for Microsoft?): %K", myUString);
    if (*myUString != L'A') {
        log_err("%%C Got: %C, Expected: A\n", *myUString);
    }

    u_sprintf(uStringBuf, NULL, "String %%s: %s", "My-String");
    u_sscanf(uStringBuf, NULL, "String %%s: %s", myString);
    if (strcmp(myString, "My-String")) {
        log_err("%%s Got: %s, Expected: My-String\n", myString);
    }
    if (uStringBuf[20] != 0) {
        log_err("String not terminated. Got %c\n", uStringBuf[20] );
    }
    u_sprintf(uStringBuf, NULL, "NULL String %%s: %s", NULL);
    u_sscanf(uStringBuf, NULL, "NULL String %%s: %s", myString);
    if (strcmp(myString, "(null)")) {
        log_err("%%s Got: %s, Expected: My-String\n", myString);
    }

    u_sprintf(uStringBuf, NULL, "Unicode String %%U (non-ANSI, should be %%S for Microsoft?): %U", L"My-String");
    u_sscanf(uStringBuf, NULL, "Unicode String %%U (non-ANSI, should be %%S for Microsoft?): %U", myUString);
    u_austrncpy(myString, myUString, sizeof(myString)/sizeof(*myString));
    if (strcmp(myString, "My-String")) {
        log_err("%%U Got: %s, Expected: My String\n", myString);
    }

    u_sprintf(uStringBuf, NULL, "NULL Unicode String %%U (non-ANSI, should be %%S for Microsoft?): %U", NULL);
    u_sscanf(uStringBuf, NULL, "NULL Unicode String %%U (non-ANSI, should be %%S for Microsoft?): %U", myUString);
    u_austrncpy(myString, myUString, sizeof(myString)/sizeof(*myString));
    if (strcmp(myString, "(null)")) {
        log_err("%%U Got: %s, Expected: (null)\n", myString);
    }

    u_sprintf(uStringBuf, NULL, "Date %%D (non-ANSI): %D", myDate);
    myNewDate = -1.0;
    u_sscanf(uStringBuf, NULL, "Date %%D (non-ANSI): %D", &myNewDate);
    if (myNewDate != dec_31_1969) {
        log_err("%%D Got: %f, Expected: %f\n", myNewDate, dec_31_1969);
    }

    u_sprintf(uStringBuf, NULL, "Time %%T (non-ANSI): %T", myDate);
    myNewDate = -1.0;
    u_sscanf(uStringBuf, NULL, "Time %%T (non-ANSI): %T", &myNewDate);
    if (myNewDate != midnight) {
        log_err("%%T Got: %f, Expected: %f\n", myNewDate, midnight);
    }

    u_sprintf(uStringBuf, NULL, "Percent %%P (non-ANSI): %P", myFloat);
    *newDoubleValuePtr = -1.0;
    u_sscanf(uStringBuf, NULL, "Percent %%P (non-ANSI): %P", newDoubleValuePtr);
    if (myFloat != *newDoubleValuePtr) {
        log_err("%%P Got: %P, Expected: %P\n", *newDoubleValuePtr, myFloat);
    }

    u_sprintf(uStringBuf, NULL, "Currency %%M (non-ANSI): %M", myFloat);
    *newDoubleValuePtr = -1.0;
    u_sscanf(uStringBuf, NULL, "Currency %%M (non-ANSI): %M", newDoubleValuePtr);
    if (myFloat != *newDoubleValuePtr) {
        log_err("%%P Got: %P, Expected: %P\n", *newDoubleValuePtr, myFloat);
    }

    u_sprintf(uStringBuf, NULL, "Spell Out %%V (non-ANSI): %V", myFloat);
    *newDoubleValuePtr = -1.0;
    u_sscanf(uStringBuf, NULL, "Spell Out %%V (non-ANSI): %V", newDoubleValuePtr);
    if (myFloat != *newDoubleValuePtr) {
        log_err("%%V Got: %f, Expected: %f\n", *newDoubleValuePtr, myFloat);
    }

    *newValuePtr = 1;
    u_sprintf(uStringBuf, NULL, "\t\nPointer to integer (Count) %%n: n=%d %n n=%d\n", *newValuePtr, newValuePtr, *newValuePtr);
    if (*newValuePtr != 37) {
        log_err("%%V Got: %f, Expected: %f\n", *newDoubleValuePtr, myFloat);
    }

    u_sprintf(myUString, NULL, "This is a long test123456789012345678901234567890123456789012345678901234567890");
    u_austrncpy(myString, myUString, sizeof(myString)/sizeof(*myString));
    if (strcmp(myString, "This is a long test123456789012345678901234567890123456789012345678901234567890")) {
        log_err("%%U Got: %s, Expected: My String\n", myString);
    }


//  u_sscanf(uStringBuf, NULL, "Pointer %%p: %p\n", myFile);
}

#define Test_u_snprintf(limit, format, value, expectedSize, expectedStr) \
    u_uastrncpy(testStr, "xxxxxxxxxxxxxx", sizeof(testStr)/sizeof(testStr[0]));\
    size = u_snprintf(testStr, limit, "en_US_POSIX", format, value);\
    u_austrncpy(cTestResult, testStr, sizeof(cTestResult)/sizeof(cTestResult[0]));\
    if (size != expectedSize || strcmp(cTestResult, expectedStr) != 0) {\
        log_err("Unexpected formatting. size=%d expectedSize=%d cTestResult=%s expectedStr=%s\n",\
            size, expectedSize, cTestResult, expectedStr);\
    }\
    else {\
        log_verbose("Got: %s\n", cTestResult);\
    }\


static void TestSnprintf() {
    UChar testStr[256];
    char cTestResult[256];
    int32_t size;

    Test_u_snprintf(0, "%d", 123, 0, "xxxxxxxxxxxxxx");
    Test_u_snprintf(2, "%d", 123, 2, "12xxxxxxxxxxxx");
    Test_u_snprintf(3, "%d", 123, 3, "123xxxxxxxxxxx");
    Test_u_snprintf(4, "%d", 123, 3, "123");

    Test_u_snprintf(0, "%s", "abcd", 0, "xxxxxxxxxxxxxx");
    Test_u_snprintf(3, "%s", "abcd", 3, "abcxxxxxxxxxxx");
    Test_u_snprintf(4, "%s", "abcd", 4, "abcdxxxxxxxxxx");
    Test_u_snprintf(5, "%s", "abcd", 4, "abcd");

    Test_u_snprintf(0, "%e", 12.34, 0, "xxxxxxxxxxxxxx");
    Test_u_snprintf(1, "%e", 12.34, 1, "1xxxxxxxxxxxxx");
    Test_u_snprintf(2, "%e", 12.34, 2, "1.xxxxxxxxxxxx");
    Test_u_snprintf(3, "%e", 12.34, 3, "1.2xxxxxxxxxxx");
    Test_u_snprintf(5, "%e", 12.34, 5, "1.234xxxxxxxxx");
    Test_u_snprintf(6, "%e", 12.34, 6, "1.2340xxxxxxxx");
    Test_u_snprintf(8, "%e", 12.34, 8, "1.234000xxxxxx");
    Test_u_snprintf(9, "%e", 12.34, 9, "1.234000exxxxx");
    Test_u_snprintf(10, "%e", 12.34, 10, "1.234000e+xxxx");
    Test_u_snprintf(11, "%e", 12.34, 11, "1.234000e+0xxx");
    Test_u_snprintf(13, "%e", 12.34, 13, "1.234000e+001x");
    Test_u_snprintf(14, "%e", 12.34, 13, "1.234000e+001");
}

#define TestSPrintFormat(uFormat, uValue, cFormat, cValue) \
    /* Reinitialize the buffer to verify null termination works. */\
    u_memset(uBuffer, 0x2a, sizeof(uBuffer)/sizeof(*uBuffer));\
    memset(buffer, 0x2a, sizeof(buffer)/sizeof(*buffer));\
    \
    uNumPrinted = u_sprintf(uBuffer, "en_US_POSIX", uFormat, uValue);\
    u_austrncpy(compBuffer, uBuffer, sizeof(uBuffer)/sizeof(uBuffer[0]));\
    cNumPrinted = sprintf(buffer, cFormat, cValue);\
    if (strcmp(buffer, compBuffer) != 0) {\
        log_err("%" uFormat " Got: \"%s\", Expected: \"%s\"\n", compBuffer, buffer);\
    }\
    if (cNumPrinted != uNumPrinted) {\
        log_err("%" uFormat " number printed Got: %d, Expected: %d\n", uNumPrinted, cNumPrinted);\
    }\
    if (buffer[uNumPrinted+1] != 0x2a) {\
        log_err("%" uFormat " too much stored\n");\
    }\

#define TestSPrintFormat2(format, precision, value) \
    /* Reinitialize the buffer to verify null termination works. */\
    u_memset(uBuffer, 0x2a, sizeof(uBuffer)/sizeof(*uBuffer));\
    memset(buffer, 0x2a, sizeof(buffer)/sizeof(*buffer));\
    \
    uNumPrinted = u_sprintf(uBuffer, "en_US_POSIX", format, precision, value);\
    u_austrncpy(compBuffer, uBuffer, sizeof(uBuffer)/sizeof(uBuffer[0]));\
    cNumPrinted = sprintf(buffer, format, precision, value);\
    if (strcmp(buffer, compBuffer) != 0) {\
        log_err("%" format " Got: \"%s\", Expected: \"%s\"\n", compBuffer, buffer);\
    }\
    if (cNumPrinted != uNumPrinted) {\
        log_err("%" format " number printed Got: %d, Expected: %d\n", uNumPrinted, cNumPrinted);\
    }\

static void TestSprintfFormat() {
    static const UChar abcUChars[] = {0x61,0x62,0x63,0};
    static const char abcChars[] = "abc";
    UChar uBuffer[256];
    char buffer[256];
    char compBuffer[256];
    int32_t uNumPrinted;
    int32_t cNumPrinted;

    TestSPrintFormat("%8U", abcUChars, "%8s", abcChars);
    TestSPrintFormat("%-8U", abcUChars, "%-8s", abcChars);

    TestSPrintFormat("%8s", abcChars, "%8s", abcChars);
    TestSPrintFormat("%-8s", abcChars, "%-8s", abcChars);

    TestSPrintFormat("%8c", 0x65, "%8c", 0x65);
    TestSPrintFormat("%-8c", 0x65, "%-8c", 0x65);

    TestSPrintFormat("%8K", (UChar)0x65, "%8c", (char)0x65);
    TestSPrintFormat("%-8K", (UChar)0x65, "%-8c", (char)0x65);

    TestSPrintFormat("%10f", 1.23456789, "%10f", 1.23456789);
    TestSPrintFormat("%-10f", 1.23456789, "%-10f", 1.23456789);
    TestSPrintFormat("%10f", 123.456789, "%10f", 123.456789);
    TestSPrintFormat("%-10f", 123.456789, "%-10f", 123.456789);

    TestSPrintFormat("%e", 1234567.89, "%e", 1234567.89);
    TestSPrintFormat("%E", 1234567.89, "%E", 1234567.89);
    TestSPrintFormat("%10e", 1.23456789, "%10e", 1.23456789);
    TestSPrintFormat("%-10e", 1.23456789, "%-10e", 1.23456789);
    TestSPrintFormat("%10e", 123.456789, "%10e", 123.456789);
    TestSPrintFormat("%-10e", 123.456789, "%-10e", 123.456789);

    TestSPrintFormat("%g", 123456.789, "%g", 123456.789);
    TestSPrintFormat("%g", 1234567.89, "%g", 1234567.89);
    TestSPrintFormat("%G", 1234567.89, "%G", 1234567.89);
    TestSPrintFormat("%10g", 1.23456789, "%10g", 1.23456789);
    TestSPrintFormat("%-10g", 1.23456789, "%-10g", 1.23456789);
    TestSPrintFormat("%10g", 123.456789, "%10g", 123.456789);
    TestSPrintFormat("%-10g", 123.456789, "%-10g", 123.456789);

    TestSPrintFormat("%8x", 123456, "%8x", 123456);
    TestSPrintFormat("%-8x", 123456, "%-8x", 123456);

    TestSPrintFormat("%8X", 123456, "%8X", 123456);
    TestSPrintFormat("%-8X", 123456, "%-8X", 123456);
    TestSPrintFormat("%#x", 123456, "%#x", 123456);
    TestSPrintFormat("%#x", -123456, "%#x", -123456);

    TestSPrintFormat("%8o", 123456, "%8o", 123456);
    TestSPrintFormat("%-8o", 123456, "%-8o", 123456);
    TestSPrintFormat("%#o", 123, "%#o", 123);
    TestSPrintFormat("%#o", -123, "%#o", -123);

    TestSPrintFormat("%8u", 123456, "%8u", 123456);
    TestSPrintFormat("%-8u", 123456, "%-8u", 123456);
    TestSPrintFormat("%8u", -123456, "%8u", -123456);
    TestSPrintFormat("%-8u", -123456, "%-8u", -123456);

    TestSPrintFormat("%8d", 123456, "%8d", 123456);
    TestSPrintFormat("%-8d", 123456, "%-8d", 123456);
    TestSPrintFormat("% d", 123456, "% d", 123456);
    TestSPrintFormat("% d", -123456, "% d", -123456);

    TestSPrintFormat("%8i", 123456, "%8i", 123456);
    TestSPrintFormat("%-8i", 123456, "%-8i", 123456);

    TestSPrintFormat2("%+1.*e", 4, 1.2345678);
    TestSPrintFormat2("%+2.*e", 6, 1.2345678);

    log_verbose("Get really crazy with the formatting.\n");

    TestSPrintFormat("%-#12x", 123, "%-#12x", 123);
    TestSPrintFormat("%-#12x", -123, "%-#12x", -123);
    TestSPrintFormat("%#12x", 123, "%#12x", 123);
    TestSPrintFormat("%#12x", -123, "%#12x", -123);

    TestSPrintFormat("%-+12d", 123,  "%-+12d", 123);
    TestSPrintFormat("%-+12d", -123, "%-+12d", -123);
    TestSPrintFormat("%- 12d", 123,  "%- 12d", 123);
    TestSPrintFormat("%- 12d", -123, "%- 12d", -123);
    TestSPrintFormat("%+12d", 123,   "%+12d", 123);
    TestSPrintFormat("%+12d", -123,  "%+12d", -123);
    TestSPrintFormat("% 12d", 123,   "% 12d", 123);
    TestSPrintFormat("% 12d", -123,  "% 12d", -123);
    TestSPrintFormat("%12d", 123,    "%12d", 123);
    TestSPrintFormat("%12d", -123,   "%12d", -123);

    TestSPrintFormat("%-+12.1e", 1.234,  "%-+12.1e", 1.234);
    TestSPrintFormat("%-+12.1e", -1.234, "%-+12.1e", -1.234);
    TestSPrintFormat("%- 12.1e", 1.234,  "%- 12.1e", 1.234);
    TestSPrintFormat("%- 12.1e", -1.234, "%- 12.1e", -1.234);
    TestSPrintFormat("%+12.1e", 1.234,   "%+12.1e", 1.234);
    TestSPrintFormat("%+12.1e", -1.234,  "%+12.1e", -1.234);
    TestSPrintFormat("% 12.1e", 1.234,   "% 12.1e", 1.234);
    TestSPrintFormat("% 12.1e", -1.234,  "% 12.1e", -1.234);
    TestSPrintFormat("%12.1e", 1.234,    "%12.1e", 1.234);
    TestSPrintFormat("%12.1e", -1.234,   "%12.1e", -1.234);
    TestSPrintFormat("%.2e", 1.234,      "%.2e", 1.234);
    TestSPrintFormat("%.2e", -1.234,     "%.2e", -1.234);
    TestSPrintFormat("%3e", 1.234,       "%3e", 1.234);
    TestSPrintFormat("%3e", -1.234,      "%3e", -1.234);

    TestSPrintFormat("%-+12.1f", 1.234,  "%-+12.1f", 1.234);
    TestSPrintFormat("%-+12.1f", -1.234, "%-+12.1f", -1.234);
    TestSPrintFormat("%- 12.1f", 1.234,  "%- 12.1f", 1.234);
    TestSPrintFormat("%- 12.1f", -1.234, "%- 12.1f", -1.234);
    TestSPrintFormat("%+12.1f", 1.234,   "%+12.1f", 1.234);
    TestSPrintFormat("%+12.1f", -1.234,  "%+12.1f", -1.234);
    TestSPrintFormat("% 12.1f", 1.234,   "% 12.1f", 1.234);
    TestSPrintFormat("% 12.1f", -1.234,  "% 12.1f", -1.234);
    TestSPrintFormat("%12.1f", 1.234,    "%12.1f", 1.234);
    TestSPrintFormat("%12.1f", -1.234,   "%12.1f", -1.234);
    TestSPrintFormat("%.2f", 1.234,      "%.2f", 1.234);
    TestSPrintFormat("%.2f", -1.234,     "%.2f", -1.234);
    TestSPrintFormat("%3f", 1.234,       "%3f", 1.234);
    TestSPrintFormat("%3f", -1.234,      "%3f", -1.234);

}

#undef TestSPrintFormat

static void TestStringCompatibility() {
    UChar myUString[256];
    UChar uStringBuf[256];
    char myString[256] = "";
    char testBuf[256] = "";
    int32_t num;

    u_memset(myUString, 0x0a, sizeof(myUString)/ sizeof(*myUString));
    u_memset(uStringBuf, 0x0a, sizeof(uStringBuf) / sizeof(*uStringBuf));

    /* Compare against C API compatibility */
    for (num = -STANDARD_TEST_NUM_RANGE; num < STANDARD_TEST_NUM_RANGE; num++) {
        sprintf(testBuf, "%x", num);
        u_sprintf(uStringBuf, NULL, "%x", num);
        u_austrncpy(myString, uStringBuf, sizeof(myString)/sizeof(myString[0]));
        if (strcmp(myString, testBuf) != 0) {
            log_err("%%x Got: \"%s\", Expected: \"%s\"\n", myString, testBuf);
        }

        sprintf(testBuf, "%X", num);
        u_sprintf(uStringBuf, NULL, "%X", num);
        u_austrncpy(myString, uStringBuf, sizeof(myString)/sizeof(myString[0]));
        if (strcmp(myString, testBuf) != 0) {
            log_err("%%X Got: \"%s\", Expected: \"%s\"\n", myString, testBuf);
        }

        sprintf(testBuf, "%o", num);
        u_sprintf(uStringBuf, NULL, "%o", num);
        u_austrncpy(myString, uStringBuf, sizeof(myString)/sizeof(myString[0]));
        if (strcmp(myString, testBuf) != 0) {
            log_err("%%o Got: \"%s\", Expected: \"%s\"\n", myString, testBuf);
        }

        /* sprintf is not compatible on all platforms e.g. the iSeries*/
        sprintf(testBuf, "%d", num);
        u_sprintf(uStringBuf, "en_US_POSIX", "%d", num);
        u_austrncpy(myString, uStringBuf, sizeof(myString)/sizeof(myString[0]));
        if (strcmp(myString, testBuf) != 0) {
            log_err("%%d Got: \"%s\", Expected: \"%s\"\n", myString, testBuf);
        }

        sprintf(testBuf, "%i", num);
        u_sprintf(uStringBuf, "en_US_POSIX", "%i", num);
        u_austrncpy(myString, uStringBuf, sizeof(myString)/sizeof(myString[0]));
        if (strcmp(myString, testBuf) != 0) {
            log_err("%%i Got: \"%s\", Expected: \"%s\"\n", myString, testBuf);
        }

        sprintf(testBuf, "%f", (double)num);
        u_sprintf(uStringBuf, "en_US_POSIX", "%f", (double)num);
        u_austrncpy(myString, uStringBuf, sizeof(myString)/sizeof(myString[0]));
        if (strcmp(myString, testBuf) != 0) {
            log_err("%%f Got: \"%s\", Expected: \"%s\"\n", myString, testBuf);
        }

        sprintf(testBuf, "%e", (double)num);
        u_sprintf(uStringBuf, "en_US_POSIX", "%e", (double)num);
        u_austrncpy(myString, uStringBuf, sizeof(myString)/sizeof(myString[0]));
        if (strcmp(myString, testBuf) != 0) {
            log_err("%%e Got: \"%s\", Expected: \"%s\"\n", myString, testBuf);
        }

        sprintf(testBuf, "%E", (double)num);
        u_sprintf(uStringBuf, "en_US_POSIX", "%E", (double)num);
        u_austrncpy(myString, uStringBuf, sizeof(myString)/sizeof(myString[0]));
        if (strcmp(myString, testBuf) != 0) {
            log_err("%%E Got: \"%s\", Expected: \"%s\"\n", myString, testBuf);
        }

        sprintf(testBuf, "%g", (double)num);
        u_sprintf(uStringBuf, "en_US_POSIX", "%g", (double)num);
        u_austrncpy(myString, uStringBuf, sizeof(myString)/sizeof(myString[0]));
        if (strcmp(myString, testBuf) != 0) {
            log_err("%%g Got: \"%s\", Expected: \"%s\"\n", myString, testBuf);
        }

        sprintf(testBuf, "%G", (double)num);
        u_sprintf(uStringBuf, "en_US_POSIX", "%G", (double)num);
        u_austrncpy(myString, uStringBuf, sizeof(myString)/sizeof(myString[0]));
        if (strcmp(myString, testBuf) != 0) {
            log_err("%%G Got: \"%s\", Expected: \"%s\"\n", myString, testBuf);
        }
    }

    for (num = 0; num < 0x80; num++) {
        testBuf[0] = (char)0xFF;
        uStringBuf[0] = (UChar)0xfffe;
        sprintf(testBuf, "%c", num);
        u_sprintf(uStringBuf, NULL, "%c", num);
        u_austrncpy(myString, uStringBuf, sizeof(myString)/sizeof(myString[0]));
        if (testBuf[0] != uStringBuf[0] || uStringBuf[0] != num) {
            log_err("%%c Got: 0x%x, Expected: 0x%x\n", myString[0], testBuf[0]);
        }
    }
}

#define TestSScanSetFormat(format, uValue, cValue) \
    /* Reinitialize the buffer to verify null termination works. */\
    u_memset(uBuffer, 0x2a, sizeof(uBuffer)/sizeof(*uBuffer));\
    memset(buffer, 0x2a, sizeof(buffer)/sizeof(*buffer));\
    \
    uNumScanned = u_sscanf(uValue, NULL, format, uBuffer);\
    u_austrncpy(compBuffer, uBuffer, sizeof(uBuffer)/sizeof(uBuffer[0]));\
    cNumScanned = sscanf(cValue, format, buffer);\
    if (strncmp(buffer, compBuffer, sizeof(uBuffer)/sizeof(uBuffer[0])) != 0) {\
        log_err("%" format " Got: \"%s\", Expected: \"%s\"\n", compBuffer, buffer);\
    }\
    if (cNumScanned != uNumScanned) {\
        log_err("%" format " number scanned Got: %d, Expected: %d\n", uNumScanned, cNumScanned);\
    }\
    if (uNumScanned > 0 && uBuffer[u_strlen(uBuffer)+1] != 0x2a) {\
        log_err("%" format " too much stored\n");\
    }\

static void TestSScanf() {
    static const UChar abcUChars[] = {0x61,0x62,0x63,0x63,0x64,0x65,0x66,0x67,0};
    static const char abcChars[] = "abccdefg";
    UChar uBuffer[256];
    char buffer[256];
    char compBuffer[256];
    int32_t uNumScanned;
    int32_t cNumScanned;

    TestSScanSetFormat("%[bc]U", abcUChars, abcChars);
    TestSScanSetFormat("%[cb]U", abcUChars, abcChars);

    TestSScanSetFormat("%[ab]U", abcUChars, abcChars);
    TestSScanSetFormat("%[ba]U", abcUChars, abcChars);

    TestSScanSetFormat("%[ab]", abcUChars, abcChars);
    TestSScanSetFormat("%[ba]", abcUChars, abcChars);

    TestSScanSetFormat("%[abcdefgh]", abcUChars, abcChars);
    TestSScanSetFormat("%[;hgfedcba]", abcUChars, abcChars);

    TestSScanSetFormat("%[a-f]", abcUChars, abcChars);
    TestSScanSetFormat("%[f-a]", abcUChars, abcChars);
    TestSScanSetFormat("%[a-c]", abcUChars, abcChars);
    TestSScanSetFormat("%[c-a]", abcUChars, abcChars);

    TestSScanSetFormat("%[^e-f]", abcUChars, abcChars);

    TestSScanSetFormat("%[^a]", abcUChars, abcChars);
    TestSScanSetFormat("%[^e]", abcUChars, abcChars);
    TestSScanSetFormat("%[^ed]", abcUChars, abcChars);
    TestSScanSetFormat("%[^dc]", abcUChars, abcChars);
    TestSScanSetFormat("%[^e]  ", abcUChars, abcChars);

    TestSScanSetFormat("%[]  ", abcUChars, abcChars);
    TestSScanSetFormat("%1[ab]  ", abcUChars, abcChars);
    TestSScanSetFormat("%2[^f]", abcUChars, abcChars);

    /* Bad format */
    TestSScanSetFormat("%[a", abcUChars, abcChars);
    /* The following is not deterministic on Windows */
/*    TestSScanSetFormat("%[a-", abcUChars, abcChars);*/
    TestSScanSetFormat("%[a-]", abcUChars, abcChars);

    /* TODO: Need to specify precision with a "*" */
}

#undef TestSScanSetFormat


#define TestFScanSetFormat(format, uValue, cValue) \
    myFile = u_fopen(STANDARD_TEST_FILE, "w", NULL, NULL);\
    /* Reinitialize the buffer to verify null termination works. */\
    u_memset(uBuffer, 0x2a, sizeof(uBuffer)/sizeof(*uBuffer));\
    memset(buffer, 0x2a, sizeof(buffer)/sizeof(*buffer));\
    \
    u_fprintf(myFile, "%U", uValue);\
    u_fclose(myFile);\
    myFile = u_fopen(STANDARD_TEST_FILE, "r", "en_US_POSIX", NULL);\
    uNumScanned = u_fscanf(myFile, format, uBuffer);\
    u_fclose(myFile);\
    u_austrncpy(compBuffer, uBuffer, sizeof(uBuffer)/sizeof(*uBuffer));\
    cNumScanned = sscanf(cValue, format, buffer);\
    if (strncmp(buffer, compBuffer, sizeof(uBuffer)/sizeof(*uBuffer)) != 0) {\
        log_err("%" format " Got: \"%s\", Expected: \"%s\"\n", compBuffer, buffer);\
    }\
    if (cNumScanned != uNumScanned) {\
        log_err("%" format " number printed Got: %d, Expected: %d\n", uNumScanned, cNumScanned);\
    }\
    if (uNumScanned > 0 && uBuffer[u_strlen(uBuffer)+1] != 0x2a) {\
        log_err("%" format " too much stored\n");\
    }\


static void TestFScanf() {
    UFILE *myFile;
    static const UChar abcUChars[] = {0x61,0x62,0x63,0x63,0x64,0x65,0x66,0x67,0};
    static const char abcChars[] = "abccdefg";
    UChar uBuffer[256];
    char buffer[256];
    char compBuffer[256];
    int32_t uNumScanned;
    int32_t cNumScanned;

    TestFScanSetFormat("%[bc]U", abcUChars, abcChars);
    TestFScanSetFormat("%[cb]U", abcUChars, abcChars);

    TestFScanSetFormat("%[ab]U", abcUChars, abcChars);
    TestFScanSetFormat("%[ba]U", abcUChars, abcChars);

    TestFScanSetFormat("%[ab]", abcUChars, abcChars);
    TestFScanSetFormat("%[ba]", abcUChars, abcChars);

    TestFScanSetFormat("%[abcdefgh]", abcUChars, abcChars);
    TestFScanSetFormat("%[;hgfedcba]", abcUChars, abcChars);

    TestFScanSetFormat("%[a-f]", abcUChars, abcChars);
    TestFScanSetFormat("%[f-a]", abcUChars, abcChars);
    TestFScanSetFormat("%[a-c]", abcUChars, abcChars);
    TestFScanSetFormat("%[c-a]", abcUChars, abcChars);

    TestFScanSetFormat("%[^e-f]", abcUChars, abcChars);

    TestFScanSetFormat("%[^a]", abcUChars, abcChars);
    TestFScanSetFormat("%[^e]", abcUChars, abcChars);
    TestFScanSetFormat("%[^ed]", abcUChars, abcChars);
    TestFScanSetFormat("%[^dc]", abcUChars, abcChars);
    TestFScanSetFormat("%[^e]  ", abcUChars, abcChars);

    TestFScanSetFormat("%[]  ", abcUChars, abcChars);
    TestFScanSetFormat("%1[ab]  ", abcUChars, abcChars);
    TestFScanSetFormat("%2[^f]", abcUChars, abcChars);

    /* Bad format */
    TestFScanSetFormat("%[a", abcUChars, abcChars);
    /* The following is not deterministic on Windows */
/*    TestFScanSetFormat("%[a-", abcUChars, abcChars);*/
    TestFScanSetFormat("%[a-]", abcUChars, abcChars);

    /* TODO: Need to specify precision with a "*" */
}

static void TestStream() {
#if U_IOSTREAM_SOURCE >= 198506
    char testStreamBuf[512];
    const char *testStr = "Beginning of test str1   <<432 1" C_NEW_LINE " UTF-8 \xCE\xBC\xF0\x90\x80\x81\xF0\x90\x80\x82";
    ostrstream outTestStream(testStreamBuf, sizeof(testStreamBuf));
    istrstream inTestStream(" tHis\xCE\xBC\xE2\x80\x82 mu world", 0);
    const UChar thisMu[] = { 0x74, 0x48, 0x69, 0x73, 0x3BC, 0};
    const UChar mu[] = { 0x6D, 0x75, 0};
    UnicodeString str1 = UNICODE_STRING_SIMPLE("str1");
    UnicodeString str2 = UNICODE_STRING_SIMPLE(" <<");
    UnicodeString str3 = UNICODE_STRING_SIMPLE("4");
    UnicodeString str4 = UNICODE_STRING_SIMPLE(" UTF-8 ");
    UnicodeString inStr = UNICODE_STRING_SIMPLE(" UTF-8 ");
    UnicodeString inStr2;
    char defConvName[UCNV_MAX_CONVERTER_NAME_LENGTH*2];
    char inStrC[128];
    UErrorCode status = U_ZERO_ERROR;
    UConverter *defConv;

    str4.append((UChar32)0x03BC);   /* mu */
    str4.append((UChar32)0x10001);
    str4.append((UChar32)0x10002);

    /* release the default converter and use utf-8 for a bit */
    defConv = u_getDefaultConverter(&status);
    if (U_FAILURE(status)) {
        log_err("Can't get default converter");
        return;
    }
    ucnv_close(defConv);
    strncpy(defConvName, ucnv_getDefaultName(), sizeof(defConvName)/sizeof(defConvName[0]));
    ucnv_setDefaultName("UTF-8");

    outTestStream << "Beginning of test ";
    outTestStream << str1 << "  " << str2 << str3 << 3 << "2 " << 1.0 << C_NEW_LINE << str4 << ends;
    if (strcmp(testStreamBuf, testStr) != 0) {
        log_err("Got: \"%s\", Expected: \"%s\"\n", testStreamBuf, testStr);
    }

    inTestStream >> inStr >> inStr2;
    if (inStr.compare(thisMu) != 0) {
        u_austrncpy(inStrC, inStr.getBuffer(), inStr.length());
        inStrC[inStr.length()] = 0;
        log_err("Got: \"%s\", Expected: \"tHis\\u03BC\"\n", inStrC);
    }
    if (inStr2.compare(mu) != 0) {
        u_austrncpy(inStrC, inStr.getBuffer(), inStr.length());
        inStrC[inStr.length()] = 0;
        log_err("Got: \"%s\", Expected: \"mu\"\n", inStrC);
    }

    /* return the default converter to the original state. */
    ucnv_setDefaultName(defConvName);
    defConv = u_getDefaultConverter(&status);
    if (U_FAILURE(status)) {
        log_err("Can't get default converter");
        return;
    }
    ucnv_close(defConv);
#else
    log_err("U_IOSTREAM_SOURCE is disabled");
#endif
}

static void TestTranslitOps()
{
    UFILE *f;
    UErrorCode err = U_ZERO_ERROR;
    UTransliterator *a = NULL, *b = NULL, *c = NULL;

    log_verbose("opening a transliterator and UFILE for testing\n");

    f = u_fopen(STANDARD_TEST_FILE, "w", "en_US_POSIX", NULL);
    if(f == NULL)
    {
        log_err("Couldn't open test file for writing");
        return;
    }

    a = utrans_open("Latin-Greek", UTRANS_FORWARD, NULL, -1, NULL, &err);
    if(U_FAILURE(err))
    {
        log_err("Err opening transliterator %s\n", u_errorName(err));
        u_fclose(f);
        return;
    }


    log_verbose("setting a transliterator\n");
    b = u_fsettransliterator(f, U_WRITE, a, &err);
    if(U_FAILURE(err))
    {
        log_err("Err setting transliterator %s\n", u_errorName(err));
        u_fclose(f);
        return;
    }

    if(b != NULL)
    {
        log_err("Err, a transliterator was already set!\n");
    }

    log_verbose("un-setting transliterator (setting to null)\n");
    c = u_fsettransliterator(f, U_WRITE, NULL, &err);
    if(U_FAILURE(err))
    {
        log_err("Err setting transliterator %s\n", u_errorName(err));
        u_fclose(f);
        return;
    }

    if(c != a)
    {
        log_err("Err, transliterator that came back was not the original one.\n");
    }

    log_verbose("Trying to set read transliterator (should fail)\n");
    b = u_fsettransliterator(f, U_READ, NULL, &err);
    if(err != U_UNSUPPORTED_ERROR)
    {
        log_err("Should have U_UNSUPPORTED_ERROR setting  Read transliterator but got %s - REVISIT AND UPDATE TEST\n", u_errorName(err));
        u_fclose(f);
        return;
    }
    else
    {
        log_verbose("Got %s error (expected) setting READ transliterator.\n", u_errorName(err));
        err = U_ZERO_ERROR;
    }


    utrans_close(c);
    u_fclose(f);
}

static void TestTranslitOut()
{
    UFILE *f;
    UErrorCode err = U_ZERO_ERROR;
    UTransliterator *a = NULL, *b = NULL, *c = NULL;
    FILE *infile;
    UChar compare[] = { 0xfeff, 0x03a3, 0x03c4, 0x03b5, 0x03c6, 0x1f00, 0x03bd, 0x03bf, 0x03c2, 0x0000 };
    UChar ubuf[256];
    int len;

    log_verbose("opening a transliterator and UFILE for testing\n");

    f = u_fopen(STANDARD_TEST_FILE, "w", "en_US_POSIX", "utf-16");
    if(f == NULL)
    {
        log_err("Couldn't open test file for writing");
        return;
    }

    a = utrans_open("Latin-Greek", UTRANS_FORWARD, NULL, -1, NULL, &err);
    if(U_FAILURE(err))
    {
        log_err("Err opening transliterator %s\n", u_errorName(err));
        u_fclose(f);
        return;
    }

    log_verbose("setting a transliterator\n");
    b = u_fsettransliterator(f, U_WRITE, a, &err);
    if(U_FAILURE(err))
    {
        log_err("Err setting transliterator %s\n", u_errorName(err));
        u_fclose(f);
        return;
    }

    if(b != NULL)
    {
        log_err("Err, a transliterator was already set!\n");
    }

    u_fprintf(f, "Stephanos");

    u_fclose(f);

    log_verbose("Re reading test file to verify transliteration\n");
    infile = fopen(STANDARD_TEST_FILE, "rb");
    if(infile == NULL)
    {
        log_err("Couldn't reopen test file\n");
        return;
    }

    len=fread(ubuf, sizeof(UChar), u_strlen(compare), infile);
    log_verbose("Read %d UChars\n", len);
    if(len != u_strlen(compare))
    {
        log_err("Wanted %d UChars from file, got %d\n", u_strlen(compare), len);
    }
    ubuf[len]=0;

    if(u_strlen(compare) != u_strlen(ubuf))
    {
        log_err("Wanted %d UChars from file, but u_strlen() returns %d\n", u_strlen(compare), len);
    }

    if(u_strcmp(compare, ubuf))
    {
        log_err("Read string doesn't match expected.\n");
    }
    else
    {
        log_verbose("Read string matches expected.\n");
    }

    fclose(infile);

}

static void addAllTests(TestNode** root) {
    addTest(root, &TestFile, "file/TestFile");
    addTest(root, &TestCodepageAndLocale, "file/TestCodepageAndLocale");
    addTest(root, &TestfgetsBuffers, "file/TestfgetsBuffers");
    addTest(root, &TestfgetsLineCount, "file/TestfgetsLineCount");
    addTest(root, &TestFprintfFormat, "file/TestFprintfFormat");
    addTest(root, &TestFScanf, "file/TestFScanf");
    addTest(root, &TestFilePrintCompatibility, "file/TestFilePrintCompatibility");

    addTest(root, &TestString, "string/TestString");
    addTest(root, &TestSprintfFormat, "string/TestSprintfFormat");
    addTest(root, &TestSnprintf, "string/TestSnprintf");
    addTest(root, &TestSScanf, "string/TestSScanf");
    addTest(root, &TestStringCompatibility, "string/TestStringCompatibility");
    addTest(root, &TestStream, "stream/TestStream");

    addTest(root, &TestTranslitOps, "translit/ops");
    addTest(root, &TestTranslitOut, "translit/out");
}

int main(int argc, char* argv[])
{
    int32_t nerrors = 0;
    TestNode *root = NULL;

    addAllTests(&root);
    nerrors = processArgs(root, argc, argv);

    cleanUpTestTree(root);
    u_cleanup();
    return nerrors;
}
