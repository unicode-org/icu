/*
**********************************************************************
*   Copyright (C) 2002-2004, International Business Machines
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


#include "unicode/ustream.h"
#include "unicode/uclean.h"

#include "unicode/ucnv.h"
#include "unicode/unistr.h"
#include "unicode/ustring.h"
#include "ustr_imp.h"
#include "iotest.h"

#if U_IOSTREAM_SOURCE >= 199711
#include <iostream>
#include <strstream>
using namespace std;
#elif U_IOSTREAM_SOURCE >= 198506
#include <iostream.h>
#include <strstream.h>
#endif

#include <string.h>

U_CDECL_BEGIN
#ifdef WIN32
const UChar NEW_LINE[] = {0x0d,0x0a,0};
#define C_NEW_LINE "\r\n"
#else
const UChar NEW_LINE[] = {0x0a,0};
#define C_NEW_LINE "\n"
#endif
U_CDECL_END


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
    log_info("U_IOSTREAM_SOURCE is disabled\n");
#endif
}

static void addAllTests(TestNode** root) {
    addFileTest(root);
    addStringTest(root);
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
