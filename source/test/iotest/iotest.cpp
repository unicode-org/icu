/*  
**********************************************************************
*   Copyright (C) 2002-2002, International Business Machines
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
#include "unicode/unistr.h"
#include "unicode/ustring.h"
#include "unicode/ctest.h"
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

static void TestCodepage() {
    UFILE *myFile = u_fopen(STANDARD_TEST_FILE, "w", NULL, NULL);
    if (u_fgetcodepage(myFile) == NULL
        || strcmp(u_fgetcodepage(myFile), ucnv_getDefaultName()) != 0)
    {
        log_err("Didn't get the proper default codepage. Got %s expected: %s\n",
            u_fgetcodepage(myFile), ucnv_getDefaultName());
    }
    u_fclose(myFile);

    myFile = u_fopen(STANDARD_TEST_FILE, "w", "en", NULL);
    if (u_fgetcodepage(myFile) == NULL
        || strcmp(u_fgetcodepage(myFile), "ISO-8859-1") != 0)
    {
        log_err("Didn't get the proper default codepage for \"en\". Got %s expected: iso-8859-1\n",
            u_fgetcodepage(myFile));
    }
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

    TestFPrintFormat("%8K", (UChar)0x65, "%8c", 0x65);
    TestFPrintFormat("%-8K", (UChar)0x65, "%-8c", 0x65);

    TestFPrintFormat("%8f", 1.23456789, "%8f", 1.23456789);
    TestFPrintFormat("%-8f", 1.23456789, "%-8f", 1.23456789);

    TestFPrintFormat("%8e", 1.23456789, "%8e", 1.23456789);
    TestFPrintFormat("%-8e", 1.23456789, "%-8e", 1.23456789);

    TestFPrintFormat("%8g", 1.23456789, "%8g", 1.23456789);
    TestFPrintFormat("%-8g", 1.23456789, "%-8g", 1.23456789);

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

    TestFPrintFormat("%8i", 123456, "%8i", 123456);
    TestFPrintFormat("%-8i", 123456, "%-8i", 123456);

    TestFPrintFormat("% d", 123456, "% d", 123456);
    TestFPrintFormat("% d", -123456, "% d", -123456);

    log_verbose("Get really crazy with the formatting.\n");

    TestFPrintFormat("%-+ #12x", 123, "%-+ #12x", 123);
    TestFPrintFormat("%-+ #12x", -123, "%-+ #12x", -123);
    TestFPrintFormat("%+ #12x", 123, "%+ #12x", 123);
    TestFPrintFormat("%+ #12x", -123, "%+ #12x", -123);

    TestFPrintFormat("%-+ 12d", 123, "%-+ 12d", 123);
    TestFPrintFormat("%-+ 12d", -123, "%-+ 12d", -123);
    TestFPrintFormat("%+ 12d", 123, "%+ 12d", 123);
    TestFPrintFormat("%+ 12d", -123, "%+ 12d", -123);
    TestFPrintFormat("%+12d", 123, "%+12d", 123);
    TestFPrintFormat("%+12d", -123, "%+12d", -123);
    TestFPrintFormat("%- 12d", 123, "%- 12d", 123);
    TestFPrintFormat("%- 12d", -123, "%- 12d", -123);
    TestFPrintFormat("% 12d", 123, "% 12d", 123);
    TestFPrintFormat("% 12d", -123, "% 12d", -123);

    TestFPrintFormat("%-+ 12.1e", 1.234, "%-+ 12.1e", 1.234);
    TestFPrintFormat("%-+ 12.1e", -1.234, "%-+ 12.1e", -1.234);
    TestFPrintFormat("%+ 12.1e", 1.234, "%+ 12.1e", 1.234);
    TestFPrintFormat("%+ 12.1e", -1.234, "%+ 12.1e", -1.234);
    TestFPrintFormat("%+12.1e", 1.234, "%+12.1e", 1.234);
    TestFPrintFormat("%+12.1e", -1.234, "%+12.1e", -1.234);
    TestFPrintFormat("% 12.1e", 1.234, "% 12.1e", 1.234);
    TestFPrintFormat("% 12.1e", -1.234, "% 12.1e", -1.234);

    TestFPrintFormat("%-+ 12.1f", 1.234, "%-+ 12.1f", 1.234);
    TestFPrintFormat("%-+ 12.1f", -1.234, "%-+ 12.1f", -1.234);
    TestFPrintFormat("%+ 12.1f", 1.234, "%+ 12.1f", 1.234);
    TestFPrintFormat("%+ 12.1f", -1.234, "%+ 12.1f", -1.234);
    TestFPrintFormat("%+12.1f", 1.234, "%+12.1f", 1.234);
    TestFPrintFormat("%+12.1f", -1.234, "%+12.1f", -1.234);
    TestFPrintFormat("% 12.1f", 1.234, "% 12.1f", 1.234);
    TestFPrintFormat("% 12.1f", -1.234, "% 12.1f", -1.234);
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
    if (u_strcmp(myUString, L"My-String")) {
        log_err("%%S Got: %S, Expected: My String\n", myUString);
    }

    u_sprintf(uStringBuf, NULL, "NULL Unicode String %%U (non-ANSI, should be %%S for Microsoft?): %U", NULL);
    u_sscanf(uStringBuf, NULL, "NULL Unicode String %%U (non-ANSI, should be %%S for Microsoft?): %U", myUString);
    u_austrncpy(myString, myUString, sizeof(myString)/sizeof(*myString));
    if (strcmp(myString, "(null)")) {
        log_err("%%S Got: %s, Expected: My String\n", myString);
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

    TestSPrintFormat("%8K", (UChar)0x65, "%8c", 0x65);
    TestSPrintFormat("%-8K", (UChar)0x65, "%-8c", 0x65);

    TestSPrintFormat("%8f", 1.23456789, "%8f", 1.23456789);
    TestSPrintFormat("%-8f", 1.23456789, "%-8f", 1.23456789);

    TestSPrintFormat("%8e", 1.23456789, "%8e", 1.23456789);
    TestSPrintFormat("%-8e", 1.23456789, "%-8e", 1.23456789);

    TestSPrintFormat("%8g", 1.23456789, "%8g", 1.23456789);
    TestSPrintFormat("%-8g", 1.23456789, "%-8g", 1.23456789);

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

    TestSPrintFormat("%8i", 123456, "%8i", 123456);
    TestSPrintFormat("%-8i", 123456, "%-8i", 123456);

    TestSPrintFormat("% d", 123456, "% d", 123456);
    TestSPrintFormat("% d", -123456, "% d", -123456);

    log_verbose("Get really crazy with the formatting.\n");

    TestSPrintFormat("%-+ #12x", 123, "%-+ #12x", 123);
    TestSPrintFormat("%-+ #12x", -123, "%-+ #12x", -123);
    TestSPrintFormat("%+ #12x", 123, "%+ #12x", 123);
    TestSPrintFormat("%+ #12x", -123, "%+ #12x", -123);

    TestSPrintFormat("%-+ 12d", 123, "%-+ 12d", 123);
    TestSPrintFormat("%-+ 12d", -123, "%-+ 12d", -123);
    TestSPrintFormat("%+ 12d", 123, "%+ 12d", 123);
    TestSPrintFormat("%+ 12d", -123, "%+ 12d", -123);
    TestSPrintFormat("%+12d", 123, "%+12d", 123);
    TestSPrintFormat("%+12d", -123, "%+12d", -123);
    TestSPrintFormat("%- 12d", 123, "%- 12d", 123);
    TestSPrintFormat("%- 12d", -123, "%- 12d", -123);
    TestSPrintFormat("% 12d", 123, "% 12d", 123);
    TestSPrintFormat("% 12d", -123, "% 12d", -123);

    TestSPrintFormat("%-+ 12.1e", 1.234, "%-+ 12.1e", 1.234);
    TestSPrintFormat("%-+ 12.1e", -1.234, "%-+ 12.1e", -1.234);
    TestSPrintFormat("%+ 12.1e", 1.234, "%+ 12.1e", 1.234);
    TestSPrintFormat("%+ 12.1e", -1.234, "%+ 12.1e", -1.234);
    TestSPrintFormat("%+12.1e", 1.234, "%+12.1e", 1.234);
    TestSPrintFormat("%+12.1e", -1.234, "%+12.1e", -1.234);
    TestSPrintFormat("% 12.1e", 1.234, "% 12.1e", 1.234);
    TestSPrintFormat("% 12.1e", -1.234, "% 12.1e", -1.234);

    TestSPrintFormat("%-+ 12.1f", 1.234, "%-+ 12.1f", 1.234);
    TestSPrintFormat("%-+ 12.1f", -1.234, "%-+ 12.1f", -1.234);
    TestSPrintFormat("%+ 12.1f", 1.234, "%+ 12.1f", 1.234);
    TestSPrintFormat("%+ 12.1f", -1.234, "%+ 12.1f", -1.234);
    TestSPrintFormat("%+12.1f", 1.234, "%+12.1f", 1.234);
    TestSPrintFormat("%+12.1f", -1.234, "%+12.1f", -1.234);
    TestSPrintFormat("% 12.1f", 1.234, "% 12.1f", 1.234);
    TestSPrintFormat("% 12.1f", -1.234, "% 12.1f", -1.234);
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
        testBuf[0] = -1;
        uStringBuf[0] = -1;
        sprintf(testBuf, "%c", num);
        u_sprintf(uStringBuf, NULL, "%c", num);
        u_austrncpy(myString, uStringBuf, sizeof(myString)/sizeof(myString[0]));
        if (testBuf[0] != uStringBuf[0] || uStringBuf[0] != num) {
            log_err("%%c Got: 0x%x, Expected: 0x%x\n", myString[0], testBuf[0]);
        }
    }
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

static void addAllTests(TestNode** root) {
    addTest(root, &TestFile, "file/TestFile");
    addTest(root, &TestCodepage, "file/TestCodepage");
    addTest(root, &TestFprintfFormat, "file/TestFprintfFormat");
    addTest(root, &TestFilePrintCompatibility, "file/TestFilePrintCompatibility");
    addTest(root, &TestString, "string/TestString");
    addTest(root, &TestSprintfFormat, "string/TestSprintfFormat");
    addTest(root, &TestSnprintf, "string/TestSnprintf");
    addTest(root, &TestStringCompatibility, "string/TestStringCompatibility");
    addTest(root, &TestStream, "stream/TestStream");
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
