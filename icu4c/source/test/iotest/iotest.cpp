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


#include "unicode/ustdio.h"
#include "unicode/ustream.h"
#include "unicode/uclean.h"

#include "unicode/ucnv.h"
#include "unicode/uchar.h"
#include "unicode/unistr.h"
#include "unicode/ustring.h"
#include "ustr_imp.h"
#include "iotest.h"
#include "unicode/tstdtmod.h"

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

class DataDrivenLogger : public TestLog {
    static const char* fgDataDir;

public:
    virtual void errln( const UnicodeString &message ) {
        char buffer[4000];
        message.extract(0, message.length(), buffer, sizeof(buffer));
        buffer[3999] = 0; /* NULL terminate */
        log_err(buffer);
    }

    static const char * pathToDataDirectory(void)
    {

        if(fgDataDir != NULL) {
            return fgDataDir;
        }

        /* U_TOPSRCDIR is set by the makefiles on UNIXes when building cintltst and intltst
        //              to point to the top of the build hierarchy, which may or
        //              may not be the same as the source directory, depending on
        //              the configure options used.  At any rate,
        //              set the data path to the built data from this directory.
        //              The value is complete with quotes, so it can be used
        //              as-is as a string constant.
        */
    #if defined (U_TOPSRCDIR)
        {
            fgDataDir = U_TOPSRCDIR  U_FILE_SEP_STRING "data" U_FILE_SEP_STRING;
        }
    #else

        /* On Windows, the file name obtained from __FILE__ includes a full path.
        *             This file is "wherever\icu\source\test\cintltst\cintltst.c"
        *             Change to    "wherever\icu\source\data"
        */
        {
            static char p[sizeof(__FILE__) + 10];
            char *pBackSlash;
            int i;

            strcpy(p, __FILE__);
            /* We want to back over three '\' chars.                            */
            /*   Only Windows should end up here, so looking for '\' is safe.   */
            for (i=1; i<=3; i++) {
                pBackSlash = strrchr(p, U_FILE_SEP_CHAR);
                if (pBackSlash != NULL) {
                    *pBackSlash = 0;        /* Truncate the string at the '\'   */
                }
            }

            if (pBackSlash != NULL) {
                /* We found and truncated three names from the path.
                *  Now append "source\data" and set the environment
                */
                strcpy(pBackSlash, U_FILE_SEP_STRING "data" U_FILE_SEP_STRING );
                fgDataDir = p;
            }
            else {
                /* __FILE__ on MSVC7 does not contain the directory */
                FILE *file = fopen(".."U_FILE_SEP_STRING".."U_FILE_SEP_STRING "data" U_FILE_SEP_STRING "Makefile.in", "r");
                if (file) {
                    fclose(file);
                    fgDataDir = ".."U_FILE_SEP_STRING".."U_FILE_SEP_STRING "data" U_FILE_SEP_STRING;
                }
                else {
                    fgDataDir = ".."U_FILE_SEP_STRING".."U_FILE_SEP_STRING".."U_FILE_SEP_STRING "data" U_FILE_SEP_STRING;
                }
            }
        }
    #endif

        return fgDataDir;

    }

    static const char* loadTestData(UErrorCode& err){
        static char *testDataPath = NULL;
        if( testDataPath == NULL){
            const char*      directory=NULL;
            UResourceBundle* test =NULL;
            char* tdpath=NULL;
            const char* tdrelativepath;

#if defined (U_TOPBUILDDIR)
            tdrelativepath = "test"U_FILE_SEP_STRING"testdata"U_FILE_SEP_STRING"out"U_FILE_SEP_STRING;
            directory = U_TOPBUILDDIR;
#else
            tdrelativepath = ".."U_FILE_SEP_STRING"test"U_FILE_SEP_STRING"testdata"U_FILE_SEP_STRING"out"U_FILE_SEP_STRING;
            directory = pathToDataDirectory();
#endif

            tdpath = (char*) malloc(sizeof(char) *(( strlen(directory) * strlen(tdrelativepath)) + 100));


            /* u_getDataDirectory shoul return \source\data ... set the
            * directory to ..\source\data\..\test\testdata\out\testdata
            */
            strcpy(tdpath, directory);
            strcat(tdpath, tdrelativepath);
            strcat(tdpath,"testdata");

            test=ures_open(tdpath, "testtypes", &err);

            if(U_FAILURE(err)){
                err = U_FILE_ACCESS_ERROR;
                log_err("Could not load testtypes.res in testdata bundle with path %s - %s\n", tdpath, u_errorName(err));
                return "";
            }
            ures_close(test);
            testDataPath = tdpath;
            return testDataPath;
        }
        return testDataPath;
    }

    virtual const char* getTestDataPath(UErrorCode& err) {
        return loadTestData(err);
    }
};

const char* DataDrivenLogger::fgDataDir = NULL;

int64_t
uto64(const UChar     *buffer)
{
    int64_t result = 0;
    /* iterate through buffer */
    while(*buffer) {
        /* read the next digit */
        result *= 16;
        if (!u_isxdigit(*buffer)) {
            log_err("\\u%04X is not a valid hex digit for this test", (UChar)*buffer);
        }
        result += *buffer - 0x0030 - (*buffer >= 0x0041 ? (*buffer >= 0x0061 ? 39 : 7) : 0);
        buffer++;
    }
    return result;
}


static void DataDrivenPrintf(void) {
    UErrorCode errorCode;
    TestDataModule *dataModule;
    TestData *testData;
    const DataMap *testCase;
    DataDrivenLogger logger;
    UChar uBuffer[2048];
    char cBuffer[2048];
    UnicodeString tempStr;
    UChar format[2048];
    //char locale[ULOC_FULLNAME_CAPACITY];
    UChar expectedResult[2048];
    UChar argument[2048];
    int32_t i;
    int8_t i8;
    int16_t i16;
    int32_t i32;
    int64_t i64;
    double dbl;

    errorCode=U_ZERO_ERROR;
    dataModule=TestDataModule::getTestDataModule("icuio", logger, errorCode);
    if(U_SUCCESS(errorCode)) {
        testData=dataModule->createTestData("printf", errorCode);
        if(U_SUCCESS(errorCode)) {
            for(i=0; testData->nextCase(testCase, errorCode); ++i) {
                if(U_FAILURE(errorCode)) {
                    log_err("error retrieving icuio/printf test case %d - %s\n",
                            i, u_errorName(errorCode));
                    errorCode=U_ZERO_ERROR;
                    continue;
                }
                tempStr=testCase->getString("format", errorCode);
                tempStr.extract(format, sizeof(format)/sizeof(format[0]), errorCode);
//                tempStr=testCase->getString("locale", errorCode);
//                tempStr.append((UChar)0);
//                tempStr.extract(0, tempStr.length(), locale, sizeof(locale), "");
                tempStr=testCase->getString("result", errorCode);
                tempStr.extract(expectedResult, sizeof(expectedResult)/sizeof(expectedResult[0]), errorCode);
                tempStr=testCase->getString("argument", errorCode);
                tempStr.extract(argument, sizeof(argument)/sizeof(argument[0]), errorCode);
                switch (testCase->getString("argumentType", errorCode)[0]) {
                case 0x64:  // 'd' double
                    dbl = atof(u_austrcpy(cBuffer, argument));
                    u_sprintf_u(uBuffer, "en_US_POSIX", format, dbl);
                    break;
                case 0x31:  // '1' int8_t
                    i8 = (int8_t)uto64(argument);
                    u_sprintf_u(uBuffer, "en_US_POSIX", format, i8);
                    break;
                case 0x32:  // '2' int16_t
                    i16 = (int16_t)uto64(argument);
                    u_sprintf_u(uBuffer, "en_US_POSIX", format, i16);
                    break;
                case 0x34:  // '4' int32_t
                    i32 = (int32_t)uto64(argument);
                    u_sprintf_u(uBuffer, "en_US_POSIX", format, i32);
                    break;
                case 0x38:  // '8' int64_t
                    i64 = uto64(argument);
                    u_sprintf_u(uBuffer, "en_US_POSIX", format, i64);
                    break;
                case 0x73:  // 's' char *
                    u_austrncpy(cBuffer, uBuffer, sizeof(cBuffer));
                    u_sprintf_u(uBuffer, "en_US_POSIX", format, cBuffer);
                    break;
                case 0x53:  // 'S' UChar *
                    u_sprintf_u(uBuffer, "en_US_POSIX", format, argument);
                    break;
                }
                if (u_strcmp(uBuffer, expectedResult) != 0) {
                    u_austrncpy(cBuffer, uBuffer, sizeof(cBuffer));
                    cBuffer[sizeof(cBuffer)-1] = 0;
                    log_err("FAILURE test case %d - Got: %s\n",
                            i, cBuffer);
                }
                if(U_FAILURE(errorCode)) {
                    log_err("error running icuio/printf test case %d - %s\n",
                            i, u_errorName(errorCode));
                    errorCode=U_ZERO_ERROR;
                    continue;
                }
            }
            delete testData;
        }
        delete dataModule;
    }
    else {
        log_err("Failed: could not load test icuio data\n");
    }
}

static void TestStream(void) {
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
        log_err("Can't get default converter\n");
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
    addTest(root, &DataDrivenPrintf, "data/DataDrivenPrintf");
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
