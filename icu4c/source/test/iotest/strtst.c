/*
**********************************************************************
*   Copyright (C) 2004-2004, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   file name:  strtst.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2004apr06
*   created by: George Rhoten
*/

#include "unicode/ustdio.h"
#include "unicode/ustring.h"
#include "iotest.h"

#include <string.h>

static void TestString(void) {
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
    int32_t retVal;
    void *origPtr, *ptr;
    U_STRING_DECL(myStringOrig, "My-String", 9);

    U_STRING_INIT(myStringOrig, "My-String", 9);
    u_memset(myUString, 0x0a, sizeof(myUString)/ sizeof(*myUString));
    u_memset(uStringBuf, 0x0a, sizeof(uStringBuf) / sizeof(*uStringBuf));

    *n = -1234;
    if (sizeof(void *) == 4) {
        origPtr = (void *)0xdeadbeef;
    } else if (sizeof(void *) == 8) {
        origPtr = (void *) INT64_C(0x1000200030004000);
    } else if (sizeof(void *) == 16) {
        /* iSeries */
        int32_t massiveBigEndianPtr[] = { 0x10002000, 0x30004000, 0x50006000, 0x70008000 };
        origPtr = *((void **)massiveBigEndianPtr);
    } else {
        log_err("sizeof(void*)=%d hasn't been tested before", (int)sizeof(void*));
    }

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

    ptr = NULL;
    u_sprintf(uStringBuf, NULL, "Pointer %%p: %p\n", origPtr);
    u_sscanf(uStringBuf, NULL, "Pointer %%p: %p\n", &ptr);
    if (ptr != origPtr || u_strlen(uStringBuf) != 13+(sizeof(void*)*2)) {
        log_err("%%p Got: %p, Expected: %p\n", ptr, origPtr);
    }

    u_sprintf(uStringBuf, NULL, "Char c: %c", 'A');
    u_sscanf(uStringBuf, NULL, "Char c: %c", myString);
    if (*myString != 'A') {
        log_err("%%c Got: %c, Expected: A\n", *myString);
    }

    u_sprintf(uStringBuf, NULL, "UChar %%C: %C", (UChar)0x0041); /*'A'*/
    u_sscanf(uStringBuf, NULL, "UChar %%C: %C", myUString);
    if (*myUString != (UChar)0x0041) { /*'A'*/
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

    u_sprintf(uStringBuf, NULL, "Unicode String %%S: %S", myStringOrig);
    u_sscanf(uStringBuf, NULL, "Unicode String %%S: %S", myUString);
    u_austrncpy(myString, myUString, sizeof(myString)/sizeof(*myString));
    if (strcmp(myString, "My-String")) {
        log_err("%%S Got: %s, Expected: My String\n", myString);
    }

    u_sprintf(uStringBuf, NULL, "NULL Unicode String %%S: %S", NULL);
    u_sscanf(uStringBuf, NULL, "NULL Unicode String %%S: %S", myUString);
    u_austrncpy(myString, myUString, sizeof(myString)/sizeof(*myString));
    if (strcmp(myString, "(null)")) {
        log_err("%%S Got: %s, Expected: (null)\n", myString);
    }

    u_sprintf(uStringBuf, NULL, "Percent %%P (non-ANSI): %P", myFloat);
    *newDoubleValuePtr = -1.0;
    u_sscanf(uStringBuf, NULL, "Percent %%P (non-ANSI): %P", newDoubleValuePtr);
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

    {
        static const char longStr[] = "This is a long test12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890";

        retVal = u_sprintf(uStringBuf, NULL, longStr);
        u_austrncpy(myString, uStringBuf, sizeof(uStringBuf)/sizeof(*uStringBuf));
        if (strcmp(myString, longStr)) {
            log_err("%%S Got: %s, Expected: %s\n", myString, longStr);
        }
        if (retVal != strlen(longStr)) {
            log_err("%%S returned different sizes. Got: %d  Expected: %d\n", retVal, strlen(longStr));
        }

        retVal = u_sprintf(uStringBuf, NULL, "%s", longStr);
        u_austrncpy(myString, uStringBuf, sizeof(uStringBuf)/sizeof(*uStringBuf));
        if (strcmp(myString, longStr)) {
            log_err("%%S Got: %s, Expected: %s\n", myString, longStr);
        }
        if (retVal != strlen(longStr)) {
            log_err("%%S returned different sizes. Got: %d  Expected: %d\n", retVal, strlen(longStr));
        }

        u_uastrncpy(myUString, longStr, sizeof(longStr)/sizeof(*longStr));
        u_sprintf_u(uStringBuf, NULL, myUString);
        if (u_strcmp(myUString, uStringBuf)) {
            log_err("%%S Long strings differ. Expected: %s\n", longStr);
        }

        u_uastrncpy(myUString, longStr, sizeof(longStr)/sizeof(*longStr));
        retVal = u_sprintf_u(uStringBuf, NULL, myUString+10);
        if (u_strcmp(myUString+10, uStringBuf)) {
            log_err("%%S Long strings differ. Expected: %s\n", longStr + 10);
        }
        if (retVal != strlen(longStr + 10)) {
            log_err("%%S returned different sizes. Got: %d  Expected: %d\n", retVal, strlen(longStr));
        }

        u_memset(uStringBuf, 1, sizeof(longStr)/sizeof(*longStr));
        u_uastrncpy(myUString, longStr, sizeof(longStr)/sizeof(*longStr));
        retVal = u_snprintf_u(uStringBuf, 10, NULL, myUString);
        if (u_strncmp(myUString, uStringBuf, 10) || uStringBuf[10] != 1 || retVal != 10) {
            log_err("%%S Long strings differ. Expected the first 10 characters of %s\n", longStr);
        }
    }

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


static void TestSnprintf(void) {
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

static void TestSprintfFormat(void) {
    static const UChar abcUChars[] = {0x61,0x62,0x63,0};
    static const char abcChars[] = "abc";
    UChar uBuffer[256];
    char buffer[256];
    char compBuffer[256];
    int32_t uNumPrinted;
    int32_t cNumPrinted;

    TestSPrintFormat("%8U", abcUChars, "%8s", abcChars);
    TestSPrintFormat("%-8U", abcUChars, "%-8s", abcChars);
    TestSPrintFormat("%.2U", abcUChars, "%.2s", abcChars); /* strlen is 3 */

    TestSPrintFormat("%8s", abcChars, "%8s", abcChars);
    TestSPrintFormat("%-8s", abcChars, "%-8s", abcChars);
    TestSPrintFormat("%.2s", abcChars, "%.2s", abcChars); /* strlen is 3 */

    TestSPrintFormat("%8c", 0x65, "%8c", 0x65);
    TestSPrintFormat("%-8c", 0x65, "%-8c", 0x65);

    TestSPrintFormat("%8C", (UChar)0x65, "%8c", (char)0x65);
    TestSPrintFormat("%-8C", (UChar)0x65, "%-8c", (char)0x65);

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
    TestSPrintFormat("%.12d", 123,   "%.12d", 123);
    TestSPrintFormat("%.12d", -123,  "%.12d", -123);

    TestSPrintFormat("%-+12.1e", 1.234,  "%-+12.1e", 1.234);
    TestSPrintFormat("%-+12.1e", -1.234, "%-+12.1e", -1.234);
    TestSPrintFormat("%- 12.10e", 1.234, "%- 12.10e", 1.234);
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
    TestSPrintFormat("%- 12.10f", 1.234, "%- 12.10f", 1.234);
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

static void TestStringCompatibility(void) {
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

/*        sprintf(testBuf, "%e", (double)num);
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
        }*/

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

static void TestSScanSetFormat(const char *format, const UChar *uValue, const char *cValue, UBool expectedToPass) {
    UChar uBuffer[256];
    char buffer[256];
    char compBuffer[256];
    int32_t uNumScanned;
    int32_t cNumScanned;

    /* Reinitialize the buffer to verify null termination works. */
    u_memset(uBuffer, 0x2a, sizeof(uBuffer)/sizeof(*uBuffer));
    uBuffer[sizeof(uBuffer)/sizeof(*uBuffer)-1] = 0;
    memset(buffer, 0x2a, sizeof(buffer)/sizeof(*buffer));
    buffer[sizeof(buffer)/sizeof(*buffer)-1] = 0;

    uNumScanned = u_sscanf(uValue, NULL, format, uBuffer);
    if (expectedToPass) {
        u_austrncpy(compBuffer, uBuffer, sizeof(uBuffer)/sizeof(uBuffer[0]));
        cNumScanned = sscanf(cValue, format, buffer);
        if (strncmp(buffer, compBuffer, sizeof(uBuffer)/sizeof(uBuffer[0])) != 0) {
            log_err("%s Got: \"%s\", Expected: \"%s\"\n", format, compBuffer, buffer);
        }
        if (cNumScanned != uNumScanned) {
            log_err("%s number scanned Got: %d, Expected: %d\n", format, uNumScanned, cNumScanned);
        }
        if (uNumScanned > 0 && uBuffer[u_strlen(uBuffer)+1] != 0x2a) {
            log_err("%s too much stored\n", format);
        }
    }
    else {
        if (uNumScanned != 0 || uBuffer[0] != 0x2a || uBuffer[1] != 0x2a) {
            log_err("%s too much stored on a failure\n", format);
        }
    }
}

static void TestSScanset(void) {
    static const UChar abcUChars[] = {0x61,0x62,0x63,0x63,0x64,0x65,0x66,0x67,0};
    static const char abcChars[] = "abccdefg";

    TestSScanSetFormat("%[bc]S", abcUChars, abcChars, TRUE);
    TestSScanSetFormat("%[cb]S", abcUChars, abcChars, TRUE);

    TestSScanSetFormat("%[ab]S", abcUChars, abcChars, TRUE);
    TestSScanSetFormat("%[ba]S", abcUChars, abcChars, TRUE);

    TestSScanSetFormat("%[ab]", abcUChars, abcChars, TRUE);
    TestSScanSetFormat("%[ba]", abcUChars, abcChars, TRUE);

    TestSScanSetFormat("%[abcdefgh]", abcUChars, abcChars, TRUE);
    TestSScanSetFormat("%[;hgfedcba]", abcUChars, abcChars, TRUE);

    TestSScanSetFormat("%[a-f]", abcUChars, abcChars, TRUE);
    TestSScanSetFormat("%[a-c]", abcUChars, abcChars, TRUE);

    TestSScanSetFormat("%[^e-f]", abcUChars, abcChars, TRUE);

    TestSScanSetFormat("%[^a]", abcUChars, abcChars, TRUE);
    TestSScanSetFormat("%[^e]", abcUChars, abcChars, TRUE);
    TestSScanSetFormat("%[^ed]", abcUChars, abcChars, TRUE);
    TestSScanSetFormat("%[^dc]", abcUChars, abcChars, TRUE);
    TestSScanSetFormat("%[^e]  ", abcUChars, abcChars, TRUE);

    TestSScanSetFormat("%[]  ", abcUChars, abcChars, TRUE);
    TestSScanSetFormat("%1[ab]  ", abcUChars, abcChars, TRUE);
    TestSScanSetFormat("%2[^f]", abcUChars, abcChars, TRUE);
    TestSScanSetFormat("%[a-]", abcUChars, abcChars, TRUE);

    /* Bad format */
    TestSScanSetFormat("%[a", abcUChars, abcChars, FALSE);
    TestSScanSetFormat("%[f-a]", abcUChars, abcChars, FALSE);
    TestSScanSetFormat("%[c-a]", abcUChars, abcChars, FALSE);
    /* The following is not deterministic on Windows */
/*    TestSScanSetFormat("%[a-", abcUChars, abcChars);*/

    /* TODO: Need to specify precision with a "*" */
}

U_CFUNC void
addStringTest(TestNode** root) {
    addTest(root, &TestString, "string/TestString");
    addTest(root, &TestSprintfFormat, "string/TestSprintfFormat");
    addTest(root, &TestSnprintf, "string/TestSnprintf");
    addTest(root, &TestSScanset, "string/TestSScanset");
    addTest(root, &TestStringCompatibility, "string/TestStringCompatibility");
}