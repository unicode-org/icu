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


#include "unicode\ustdio.h"
#include "unicode\ustream.h"

#include "unicode\ucnv.h"
#include "unicode\unistr.h"
#include "unicode\ustring.h"
#include "unicode\ctest.h"
#include "ustr_imp.h"

#if U_IOSTREAM_SOURCE >= 199711
#include <iostream>
#include <strstream>
using namespace std;
#elif U_IOSTREAM_SOURCE >= 198506
#include <iostream.h>
#include <strstrea.h>
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

static void TestFile() {
    UFILE *myFile = u_fopen(STANDARD_TEST_FILE, "w", NULL, NULL);
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
//	u_fprintf(myFile, "Pointer %%p: %p\n", myFile);
	u_fprintf(myFile, "Char %%c: %c\n", 'A');
	u_fprintf(myFile, "UChar %%K (non-ANSI, should be %%C for Microsoft?): %K\n", L'A');
	u_fprintf(myFile, "String %%s: %s\n", "My-String");
	u_fprintf(myFile, "Unicode String %%U (non-ANSI, should be %%S for Microsoft?): %U\n", L"My-String");
    u_fprintf(myFile, "Date %%D (non-ANSI): %D\n", myDate);
    u_fprintf(myFile, "Time %%T (non-ANSI): %T\n", myDate);
    u_fprintf(myFile, "Percent %%P (non-ANSI): %P\n", myFloat);
    u_fprintf(myFile, "Currency %%M (non-ANSI): %M\n", myFloat);
    u_fprintf(myFile, "Spell Out %%V (non-ANSI): %V\n", *n);

    *n = 1;
    u_fprintf(myFile, "Pointer to integer (Count) %%n: n=%d %n n=%d\n", *n, n, *n);
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
//	u_fscanf(myFile, "Pointer %%p: %p\n", newDoubleValue);
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
	u_fscanf(myFile, "Unicode String %%U (non-ANSI, should be %%S for Microsoft?): %U\n", myUString);
    if (u_strcmp(myUString, L"My-String")) {
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
        log_err("%%P Got: %P, Expected: %P\n", *newDoubleValuePtr, myFloat);
    }
    *newDoubleValuePtr = -1.0;
    u_fscanf(myFile, "Currency %%M (non-ANSI): %M\n", newDoubleValuePtr);
    if (myFloat != *newDoubleValuePtr) {
        log_err("%%P Got: %P, Expected: %P\n", *newDoubleValuePtr, myFloat);
    }
/*
    u_fscanf(myFile, "Spell Out %%V (non-ANSI): %V\n", *n);

    *n = 1;
    u_fscanf(myFile, "Pointer to integer (Count) %%n: n=%d %n n=%d\n", *n, n, *n);
    u_fscanf(myFile, "Pointer to integer Value: %d\n", *n);
    *n = 1;
    fscanf(u_fgetfile(myFile), "\tNormal fprintf count: n=%d %n n=%d\n", *n ,n, *n);
    fscanf(u_fgetfile(myFile), "\tNormal fprintf count value: n=%d\n", *n);
*/

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
    if (myFile == NULL) {
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

/*	u_snprintf(uStringBuf, 28, NULL, "Signed decimal integer d: %d\n", *n);
	u_snprintf(uStringBuf, 3, NULL, "Signed decimal integer i: %i\n", *n);
	u_snprintf(uStringBuf, 3, NULL, "Unsigned octal integer o: %o\n", *n);
	u_snprintf(uStringBuf, 3, NULL, "Unsigned decimal integer %%u: %u\n", *n);
	u_snprintf(uStringBuf, 3, NULL, "Lowercase unsigned hexadecimal integer x: %x\n", *n);
	u_snprintf(uStringBuf, 3, NULL, "Uppercase unsigned hexadecimal integer X: %X\n", *n);
	u_snprintf(uStringBuf, 3, NULL, "Float f: %f\n", myFloat);
	u_snprintf(uStringBuf, 3, NULL, "Lowercase float e: %e\n", myFloat);
	u_snprintf(uStringBuf, 3, NULL, "Uppercase float E: %E\n", myFloat);
	u_snprintf(uStringBuf, 3, NULL, "Lowercase float g: %g\n", myFloat);
	u_snprintf(uStringBuf, 3, NULL, "Uppercase float G: %G\n", myFloat);
//	u_sprintf(uStringBuf, NULL, "Pointer %%p: %p\n", myFile);
	u_snprintf(uStringBuf, 3, NULL, "Char c: %c\n", 'A');
	u_sprintf(uStringBuf, NULL, "UChar %%K (non-ANSI, should be %%C for Microsoft?): %K\n", L'A');
	u_sprintf(uStringBuf, NULL, "String %%s: %s\n", "My-String");
	u_sprintf(uStringBuf, NULL, "Unicode String %%U (non-ANSI, should be %%S for Microsoft?): %U\n", L"My-String");
    u_sprintf(uStringBuf, NULL, "Date %%D (non-ANSI): %D\n", myDate);
    u_sprintf(uStringBuf, NULL, "Time %%T (non-ANSI): %T\n", myDate);
    u_sprintf(uStringBuf, NULL, "Percent %%P (non-ANSI): %P\n", myFloat);
    u_sprintf(uStringBuf, NULL, "Currency %%M (non-ANSI): %M\n", myFloat);
    u_sprintf(uStringBuf, NULL, "Spell Out %%V (non-ANSI): %V\n", *n);*/

    /* Test sprintf */
	u_sprintf(uStringBuf, NULL, "Signed decimal integer d: %d\n", *n);
    *newValuePtr = 1;
	u_sscanf(uStringBuf, NULL, "Signed decimal integer d: %d\n", newValuePtr);
    if (*n != *newValuePtr) {
        log_err("%%d Got: %d, Expected: %d\n", *newValuePtr, *n);
    }

    u_sprintf(uStringBuf, NULL, "Signed decimal integer i: %i\n", *n);
    *newValuePtr = 1;
	u_sscanf(uStringBuf, NULL, "Signed decimal integer i: %i\n", newValuePtr);
    if (*n != *newValuePtr) {
        log_err("%%i Got: %i, Expected: %i\n", *newValuePtr, *n);
    }

    u_sprintf(uStringBuf, NULL, "Unsigned octal integer o: %o\n", *n);
    *newValuePtr = 1;
	u_sscanf(uStringBuf, NULL, "Unsigned octal integer o: %o\n", newValuePtr);
    if (*n != *newValuePtr) {
        log_err("%%o Got: %o, Expected: %o\n", *newValuePtr, *n);
    }

    u_sprintf(uStringBuf, NULL, "Unsigned decimal integer %%u: %u\n", *n);
    *newValuePtr = 1;
	u_sscanf(uStringBuf, NULL, "Unsigned decimal integer %%u: %u\n", newValuePtr);
    if (*n != *newValuePtr) {
        log_err("%%u Got: %u, Expected: %u\n", *newValuePtr, *n);
    }

    u_sprintf(uStringBuf, NULL, "Lowercase unsigned hexadecimal integer x: %x\n", *n);
    *newValuePtr = 1;
	u_sscanf(uStringBuf, NULL, "Lowercase unsigned hexadecimal integer x: %x\n", newValuePtr);
    if (*n != *newValuePtr) {
        log_err("%%x Got: %x, Expected: %x\n", *newValuePtr, *n);
    }

    u_sprintf(uStringBuf, NULL, "Uppercase unsigned hexadecimal integer X: %X\n", *n);
    *newValuePtr = 1;
	u_sscanf(uStringBuf, NULL, "Uppercase unsigned hexadecimal integer X: %X\n", newValuePtr);
    if (*n != *newValuePtr) {
        log_err("%%X Got: %X, Expected: %X\n", *newValuePtr, *n);
    }

    u_sprintf(uStringBuf, NULL, "Float f: %f\n", myFloat);
    *newDoubleValuePtr = -1.0;
	u_sscanf(uStringBuf, NULL, "Float f: %f\n", newDoubleValuePtr);
    if (myFloat != *newDoubleValuePtr) {
        log_err("%%f Got: %f, Expected: %f\n", *newDoubleValuePtr, myFloat);
    }

    u_sprintf(uStringBuf, NULL, "Lowercase float e: %e\n", myFloat);
    *newDoubleValuePtr = -1.0;
	u_sscanf(uStringBuf, NULL, "Lowercase float e: %e\n", newDoubleValuePtr);
    if (myFloat != *newDoubleValuePtr) {
        log_err("%%e Got: %e, Expected: %e\n", *newDoubleValuePtr, myFloat);
    }

    u_sprintf(uStringBuf, NULL, "Uppercase float E: %E\n", myFloat);
    *newDoubleValuePtr = -1.0;
	u_sscanf(uStringBuf, NULL, "Uppercase float E: %E\n", newDoubleValuePtr);
    if (myFloat != *newDoubleValuePtr) {
        log_err("%%E Got: %E, Expected: %E\n", *newDoubleValuePtr, myFloat);
    }

    u_sprintf(uStringBuf, NULL, "Lowercase float g: %g\n", myFloat);
    *newDoubleValuePtr = -1.0;
	u_sscanf(uStringBuf, NULL, "Lowercase float g: %g\n", newDoubleValuePtr);
    if (myFloat != *newDoubleValuePtr) {
        log_err("%%g Got: %g, Expected: %g\n", *newDoubleValuePtr, myFloat);
    }

    u_sprintf(uStringBuf, NULL, "Uppercase float G: %G\n", myFloat);
    *newDoubleValuePtr = -1.0;
	u_sscanf(uStringBuf, NULL, "Uppercase float G: %G\n", newDoubleValuePtr);
    if (myFloat != *newDoubleValuePtr) {
        log_err("%%G Got: %G, Expected: %G\n", *newDoubleValuePtr, myFloat);
    }

//    u_sprintf(uStringBuf, NULL, "Pointer %%p: %p\n", myFile);
	u_sprintf(uStringBuf, NULL, "Char c: %c\n", 'A');
	u_sscanf(uStringBuf, NULL, "Char c: %c\n", myString);
    if (*myString != 'A') {
        log_err("%%c Got: %c, Expected: A\n", *myString);
    }

    u_sprintf(uStringBuf, NULL, "UChar %%K (non-ANSI, should be %%C for Microsoft?): %K\n", L'A');
	u_sscanf(uStringBuf, NULL, "UChar %%K (non-ANSI, should be %%C for Microsoft?): %K\n", myUString);
    if (*myUString != L'A') {
        log_err("%%C Got: %C, Expected: A\n", *myUString);
    }

    u_sprintf(uStringBuf, NULL, "String %%s: %s\n", "My-String");
	u_sscanf(uStringBuf, NULL, "String %%s: %s\n", myString);
    if (strcmp(myString, "My-String")) {
        log_err("%%s Got: %s, Expected: My-String\n", myString);
    }
    if (uStringBuf[21] != 0) {
        log_err("String not terminated. Got %c\n", uStringBuf[21] );
    }

    u_sprintf(uStringBuf, NULL, "Unicode String %%U (non-ANSI, should be %%S for Microsoft?): %U\n", L"My-String");
	u_sscanf(uStringBuf, NULL, "Unicode String %%U (non-ANSI, should be %%S for Microsoft?): %U\n", myUString);
    if (u_strcmp(myUString, L"My-String")) {
        log_err("%%S Got: %S, Expected: My String\n", myUString);
    }

    u_sprintf(uStringBuf, NULL, "Date %%D (non-ANSI): %D\n", myDate);
    myNewDate = -1.0;
    u_sscanf(uStringBuf, NULL, "Date %%D (non-ANSI): %D\n", &myNewDate);
    if (myNewDate != dec_31_1969) {
        log_err("%%D Got: %f, Expected: %f\n", myNewDate, dec_31_1969);
    }

    u_sprintf(uStringBuf, NULL, "Time %%T (non-ANSI): %T\n", myDate);
    myNewDate = -1.0;
    u_sscanf(uStringBuf, NULL, "Time %%T (non-ANSI): %T\n", &myNewDate);
    if (myNewDate != midnight) {
        log_err("%%T Got: %f, Expected: %f\n", myNewDate, midnight);
    }

    u_sprintf(uStringBuf, NULL, "Percent %%P (non-ANSI): %P\n", myFloat);
    *newDoubleValuePtr = -1.0;
    u_sscanf(uStringBuf, NULL, "Percent %%P (non-ANSI): %P\n", newDoubleValuePtr);
    if (myFloat != *newDoubleValuePtr) {
        log_err("%%P Got: %P, Expected: %P\n", *newDoubleValuePtr, myFloat);
    }

    u_sprintf(uStringBuf, NULL, "Currency %%M (non-ANSI): %M\n", myFloat);
    *newDoubleValuePtr = -1.0;
    u_sscanf(uStringBuf, NULL, "Currency %%M (non-ANSI): %M\n", newDoubleValuePtr);
    if (myFloat != *newDoubleValuePtr) {
        log_err("%%P Got: %P, Expected: %P\n", *newDoubleValuePtr, myFloat);
    }

//    u_sprintf(uStringBuf, NULL, "Spell Out %%V (non-ANSI): %V\n", *n);

//	u_sscanf(uStringBuf, NULL, "Pointer %%p: %p\n", myFile);
//    u_sscanf(uStringBuf, NULL, "Spell Out %%V (non-ANSI): %V\n", *n);
}

static void TestStringCompatibility() {
    UChar myUString[512];
    UChar uStringBuf[512];
    char myString[512] = "";
    char testBuf[512] = "";
    int32_t num;

    u_memset(myUString, 0x0a, sizeof(myUString)/ sizeof(*myUString));
    u_memset(uStringBuf, 0x0a, sizeof(uStringBuf) / sizeof(*uStringBuf));

    /* Compare against C API compatibility */
    for (num = -STANDARD_TEST_NUM_RANGE; num < STANDARD_TEST_NUM_RANGE; num++) {
        sprintf(testBuf, "%x", num);
        u_sprintf(uStringBuf, NULL, "%x", num);
        u_austrcpy(myString, uStringBuf);
        if (strcmp(myString, testBuf) != 0) {
            log_err("%%x Got: \"%s\", Expected: \"%s\"\n", myString, testBuf);
        }

        sprintf(testBuf, "%X", num);
        u_sprintf(uStringBuf, NULL, "%X", num);
        u_austrcpy(myString, uStringBuf);
        if (strcmp(myString, testBuf) != 0) {
            log_err("%%X Got: \"%s\", Expected: \"%s\"\n", myString, testBuf);
        }

        sprintf(testBuf, "%o", num);
        u_sprintf(uStringBuf, NULL, "%o", num);
        u_austrcpy(myString, uStringBuf);
        if (strcmp(myString, testBuf) != 0) {
            log_err("%%o Got: \"%s\", Expected: \"%s\"\n", myString, testBuf);
        }

        /* sprintf is not compatible on all platforms e.g. the iSeries*/
        sprintf(testBuf, "%d", num);
        u_sprintf(uStringBuf, "en_US_POSIX", "%d", num);
        u_austrcpy(myString, uStringBuf);
        if (strcmp(myString, testBuf) != 0) {
            log_err("%%d Got: \"%s\", Expected: \"%s\"\n", myString, testBuf);
        }

        sprintf(testBuf, "%i", num);
        u_sprintf(uStringBuf, "en_US_POSIX", "%i", num);
        u_austrcpy(myString, uStringBuf);
        if (strcmp(myString, testBuf) != 0) {
            log_err("%%i Got: \"%s\", Expected: \"%s\"\n", myString, testBuf);
        }

        sprintf(testBuf, "%f", (double)num);
        u_sprintf(uStringBuf, "en_US_POSIX", "%f", (double)num);
        u_austrcpy(myString, uStringBuf);
        if (strcmp(myString, testBuf) != 0) {
            log_err("%%f Got: \"%s\", Expected: \"%s\"\n", myString, testBuf);
        }

        sprintf(testBuf, "%e", (double)num);
        u_sprintf(uStringBuf, "en_US_POSIX", "%e", (double)num);
        u_austrcpy(myString, uStringBuf);
        if (strcmp(myString, testBuf) != 0) {
            log_err("%%e Got: \"%s\", Expected: \"%s\"\n", myString, testBuf);
        }

        sprintf(testBuf, "%E", (double)num);
        u_sprintf(uStringBuf, "en_US_POSIX", "%E", (double)num);
        u_austrcpy(myString, uStringBuf);
        if (strcmp(myString, testBuf) != 0) {
            log_err("%%E Got: \"%s\", Expected: \"%s\"\n", myString, testBuf);
        }

        sprintf(testBuf, "%g", (double)num);
        u_sprintf(uStringBuf, "en_US_POSIX", "%g", (double)num);
        u_austrcpy(myString, uStringBuf);
        if (strcmp(myString, testBuf) != 0) {
            log_err("%%g Got: \"%s\", Expected: \"%s\"\n", myString, testBuf);
        }

        sprintf(testBuf, "%G", (double)num);
        u_sprintf(uStringBuf, "en_US_POSIX", "%G", (double)num);
        u_austrcpy(myString, uStringBuf);
        if (strcmp(myString, testBuf) != 0) {
            log_err("%%G Got: \"%s\", Expected: \"%s\"\n", myString, testBuf);
        }
    }

    for (num = 0; num < 0x80; num++) {
        testBuf[0] = -1;
        uStringBuf[0] = -1;
        sprintf(testBuf, "%c", num);
        u_sprintf(uStringBuf, NULL, "%c", num);
        u_austrcpy(myString, uStringBuf);
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
    istrstream inTestStream(" tHis\xCE\xBC mu world", 0);
    const UChar thisMu[] = { 0x74, 0x48, 0x69, 0x73, 0x3BC ,0};
    UnicodeString str1 = UNICODE_STRING_SIMPLE("str1");
    UnicodeString str2 = UNICODE_STRING_SIMPLE(" <<");
    UnicodeString str3 = UNICODE_STRING_SIMPLE("4");
    UnicodeString str4 = UNICODE_STRING_SIMPLE(" UTF-8 ");
    UnicodeString inStr = UNICODE_STRING_SIMPLE(" UTF-8 ");
    char defConvName[128];
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
    strcpy(defConvName, ucnv_getDefaultName());
    ucnv_setDefaultName("UTF-8");

    outTestStream << "Beginning of test ";
    outTestStream << str1 << "  " << str2 << str3 << 3 << "2 " << 1.0 << NEW_LINE << str4 << ends;
    if (strcmp(testStreamBuf, testStr) != 0) {
        log_err("Got: \"%s\", Expected: \"%s\"\n", testStreamBuf, testStr);
    }
    
    inTestStream >> inStr;
    if (inStr.compare(thisMu) != 0) {
        u_austrncpy(inStrC, inStr.getBuffer(), inStr.length());
        inStrC[inStr.length()] = 0;
        log_err("Got: \"%s\", Expected: \"tHis\\u03BC\"\n", inStrC);
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
    addTest(root, &TestFile, "fileapi/TestFile");
    addTest(root, &TestFilePrintCompatibility, "fileapi/TestFilePrintCompatibility");
    addTest(root, &TestString, "strapi/TestString");
    addTest(root, &TestStringCompatibility, "strapi/TestStringCompatibility");
    addTest(root, &TestStream, "iostream/TestStream");
}

int main(int argc, char* argv[])
{
    int32_t nerrors = 0;
    TestNode *root = NULL;

    addAllTests(&root);
    nerrors = processArgs(root, argc, argv);

    return nerrors;
}
