/********************************************************************
 * COPYRIGHT:
 * Copyright (c) 1997-2003, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/


#include "unicode/utypes.h"

/**
 * IntlTest is a base class for tests.
 */

#include <stdio.h>
#include <string.h>
#include <assert.h>
#include <stdarg.h>
#include <stdlib.h>

#include "unicode/unistr.h"
#include "unicode/ures.h"
#include "unicode/smpdtfmt.h"
#include "unicode/ucnv.h"
#include "unicode/uclean.h"
#include "unicode/timezone.h"

#include "intltest.h"
#include "caltztst.h"
#include "itmajor.h"
#include "tsmutex.h"

#include "umutex.h"

#ifdef XP_MAC_CONSOLE
#include <console.h>
#include "Files.h"
#endif


static char* _testDataPath=NULL;

// Static list of errors found
static UnicodeString errorList;

//-----------------------------------------------------------------------------
//convenience classes to ease porting code that uses the Java
//string-concatenation operator (moved from findword test by rtg)

// [LIU] Just to get things working
UnicodeString
UCharToUnicodeString(UChar c)
{ return UnicodeString(c); }

// [LIU] Just to get things working
UnicodeString
operator+(const UnicodeString& left,
      const UnicodeString& right)
{
    UnicodeString str(left);
    str += right;
    return str;
}

// [rtg] Just to get things working
UnicodeString
operator+(const UnicodeString& left,
      long num)
{
    char buffer[64];    // nos changed from 10 to 64
    char danger = 'p';  // guard against overrunning the buffer (rtg)

    sprintf(buffer, "%ld", num);
    assert(danger == 'p');

    return left + buffer;
}

UnicodeString
operator+(const UnicodeString& left,
      unsigned long num)
{
    char buffer[64];    // nos changed from 10 to 64
    char danger = 'p';  // guard against overrunning the buffer (rtg)

    sprintf(buffer, "%lu", num);
    assert(danger == 'p');

    return left + buffer;
}

// [LIU] Just to get things working
UnicodeString
operator+(const UnicodeString& left,
      double num)
{
    char buffer[64];   // was 32, made it arbitrarily bigger (rtg)
    char danger = 'p'; // guard against overrunning the buffer (rtg)

    sprintf(buffer, "%.30g", num); // nos changed from 99 to 30
    assert(danger == 'p');

    return left + buffer;
}

#if !UCONFIG_NO_FORMATTING

/**
 * Originally coded this as operator+, but that makes the expression
 * + char* ambiguous. - liu
 */
UnicodeString toString(const Formattable& f) {
    UnicodeString s;
    switch (f.getType()) {
    case Formattable::kDate:
        {
            UErrorCode status = U_ZERO_ERROR;
            SimpleDateFormat fmt(status);
            if (U_SUCCESS(status)) {
                FieldPosition pos;
                fmt.format(f.getDate(), s, pos);
                s.insert(0, "[Date:");
                s.insert(s.length(), (UChar)0x005d);
            } else {
                s = UnicodeString("[Error creating date format]");
            }
        }
        break;
    case Formattable::kDouble:
        s = UnicodeString("[Double:") + f.getDouble() + "]";
        break;
    case Formattable::kLong:
        s = UnicodeString("[Long:") + f.getLong() + "]";
        break;
    case Formattable::kString:
        f.getString(s);
        s.insert(0, "[String:");
        s.insert(s.length(), (UChar)0x005d);
        break;
    case Formattable::kArray:
        {
            int32_t i, n;
            const Formattable* array = f.getArray(n);
            s.insert(0, UnicodeString("[Array:"));
            UnicodeString delim(", ");
            for (i=0; i<n; ++i) {
                if (i > 0) {
                    s.append(delim);
                }
                s = s + toString(array[i]);
            }
            s.append(UChar(0x005d));
        }
        break;
    }
    return s;
}

#endif

// stephen - cleaned up 05/05/99
UnicodeString operator+(const UnicodeString& left, char num)
{ return left + (long)num; }
UnicodeString operator+(const UnicodeString& left, short num)
{ return left + (long)num; }
UnicodeString operator+(const UnicodeString& left, int num)
{ return left + (long)num; }
UnicodeString operator+(const UnicodeString& left, unsigned char num)
{ return left + (unsigned long)num; }
UnicodeString operator+(const UnicodeString& left, unsigned short num)
{ return left + (unsigned long)num; }
UnicodeString operator+(const UnicodeString& left, unsigned int num)
{ return left + (unsigned long)num; }
UnicodeString operator+(const UnicodeString& left, float num)
{ return left + (double)num; }

//------------------

// Append a hex string to the target
UnicodeString&
IntlTest::appendHex(uint32_t number,
            int32_t digits,
            UnicodeString& target)
{
    static const UChar digitString[] = {
        0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39,
        0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0
    }; /* "0123456789ABCDEF" */

    switch (digits)
    {
    case 8:
        target += digitString[(number >> 28) & 0xF];
    case 7:
        target += digitString[(number >> 24) & 0xF];
    case 6:
        target += digitString[(number >> 20) & 0xF];
    case 5:
        target += digitString[(number >> 16) & 0xF];
    case 4:
        target += digitString[(number >> 12) & 0xF];
    case 3:
        target += digitString[(number >>  8) & 0xF];
    case 2:
        target += digitString[(number >>  4) & 0xF];
    case 1:
        target += digitString[(number >>  0) & 0xF];
        break;
    default:
        target += "**";
    }
    return target;
}

// Replace nonprintable characters with unicode escapes
UnicodeString&
IntlTest::prettify(const UnicodeString &source,
           UnicodeString &target)
{
    int32_t i;

    target.remove();
    target += "\"";

    for (i = 0; i < source.length(); )
    {
        UChar32 ch = source.char32At(i);
        i += UTF_CHAR_LENGTH(ch);

        if (ch < 0x09 || (ch > 0x0A && ch < 0x20)|| ch > 0x7E)
        {
            if (ch <= 0xFFFF) {
                target += "\\u";
                appendHex(ch, 4, target);
            } else {
                target += "\\U";
                appendHex(ch, 8, target);
            }
        }
        else
        {
            target += ch;
        }
    }

    target += "\"";

    return target;
}

// Replace nonprintable characters with unicode escapes
UnicodeString
IntlTest::prettify(const UnicodeString &source, UBool parseBackslash)
{
    int32_t i;
    UnicodeString target;
    target.remove();
    target += "\"";

    for (i = 0; i < source.length();)
    {
        UChar32 ch = source.char32At(i);
        i += UTF_CHAR_LENGTH(ch);

        if (ch < 0x09 || (ch > 0x0A && ch < 0x20)|| ch > 0x7E)
        {
            if (parseBackslash) {
                // If we are preceded by an odd number of backslashes,
                // then this character has already been backslash escaped.
                // Delete a backslash.
                int32_t backslashCount = 0;
                for (int32_t j=target.length()-1; j>=0; --j) {
                    if (target.charAt(j) == (UChar)92) {
                        ++backslashCount;
                    } else {
                        break;
                    }
                }
                if ((backslashCount % 2) == 1) {
                    target.truncate(target.length() - 1);
                }
            }
            if (ch <= 0xFFFF) {
                target += "\\u";
                appendHex(ch, 4, target);
            } else {
                target += "\\U";
                appendHex(ch, 8, target);
            }
        }
        else
        {
            target += ch;
        }
    }

    target += "\"";

    return target;
}

#if defined(_WIN32) || defined(WIN32) || defined(__OS2__) || defined(OS2)
#define PREV_DIR ".."
#else
#define PREV_DIR "/../"
#endif

void
IntlTest::pathnameInContext( char* fullname, int32_t maxsize, const char* relPath ) //nosmac
{
    const char* mainDir;
    char  sepChar;
    const char inpSepChar = '|';

    // So what's going on is that ICU_DATA during tests points to:
    //              ICU | source | data
    //and we want   ICU | source |
    //
    // We'll add                 | test | testdata
    //
    // So, just add a .. here - back up one level

    mainDir = u_getDataDirectory();
    sepChar = U_FILE_SEP_CHAR;
    char sepString[] = U_FILE_SEP_STRING;

#if defined(XP_MAC)
    Str255 volName;
    int16_t volNum;
    OSErr err = GetVol( volName, &volNum );
    if (err != noErr)
        volName[0] = 0;
    mainDir = (char*) &(volName[1]);
    mainDir[volName[0]] = 0;
#else
    char mainDirBuffer[255];
    if(mainDir!=NULL) {
        strcpy(mainDirBuffer, mainDir);
        strcat(mainDirBuffer, PREV_DIR);
    } else {
        mainDirBuffer[0]='\0';
    }
    mainDir=mainDirBuffer;
#endif

    if (relPath[0] == '|')
        relPath++;
    int32_t lenMainDir = strlen(mainDir);
    int32_t lenRelPath = strlen(relPath);
    if (maxsize < lenMainDir + lenRelPath + 2) {
        fullname[0] = 0;
        return;
    }
    strcpy(fullname, mainDir);
    strcat(fullname, sepString);
    strcat(fullname, relPath);
    char* tmp = strchr(fullname, inpSepChar);
    while (tmp) {
        *tmp = sepChar;
        tmp = strchr(tmp+1, inpSepChar);
    }
}

/*  IntlTest::setICU_DATA  - if the ICU_DATA environment variable is not already
 *                       set, try to deduce the directory in which ICU was built,
 *                       and set ICU_DATA to "icu/source/data" in that location.
 *                       The intent is to allow the tests to have a good chance
 *                       of running without requiring that the user manually set
 *                       ICU_DATA.  Common data isn't a problem, since it is
 *                       picked up via a static (build time) reference, but the
 *                       tests dynamically load some data.
 */
void IntlTest::setICU_DATA() {
    const char *original_ICU_DATA = getenv("ICU_DATA");

    if (original_ICU_DATA != NULL && *original_ICU_DATA != 0) {
        /*  If the user set ICU_DATA, don't second-guess the person. */
        return;
    }

    // U_TOPBUILDDIR is set by the makefiles on UNIXes when building cintltst and intltst
    //              to point to the top of the build hierarchy, which may or
    //              may not be the same as the source directory, depending on
    //              the configure options used.  At any rate,
    //              set the data path to the built data from this directory.
    //              The value is complete with quotes, so it can be used 
    //              as-is as a string constant.

#if defined (U_TOPBUILDDIR)
    {
        static char env_string[] = U_TOPBUILDDIR "data" U_FILE_SEP_STRING "out" U_FILE_SEP_STRING;
        u_setDataDirectory(env_string);
        return;
    }

#else
    // Use #else so we don't get compiler warnings due to the return above.

    /* On Windows, the file name obtained from __FILE__ includes a full path.
     *             This file is "wherever\icu\source\test\cintltst\cintltst.c"
     *             Change to    "wherever\icu\source\data"
     */
    {
        char p[sizeof(__FILE__) + 10];
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
            strcpy(pBackSlash, U_FILE_SEP_STRING "data" U_FILE_SEP_STRING "out" U_FILE_SEP_STRING);
            u_setDataDirectory(p);     /*  p is "ICU_DATA=wherever\icu\source\data"    */
            return;
        }
        else {
            /* __FILE__ on MSVC7 does not contain the directory */
            u_setDataDirectory(".."U_FILE_SEP_STRING".."U_FILE_SEP_STRING "data" U_FILE_SEP_STRING "out" U_FILE_SEP_STRING);
            return;
        }
    }
#endif

    /* No location for the data dir was identifiable.
     *   Add other fallbacks for the test data location here if the need arises
     */
}


//--------------------------------------------------------------------------------------

static const int32_t indentLevel_offset = 3;
static const char delim = '/';

IntlTest* IntlTest::gTest = NULL;

static int32_t execCount = 0;

void it_log( UnicodeString message )
{
    if (IntlTest::gTest)
        IntlTest::gTest->log( message );
}

void it_logln( UnicodeString message )
{
    if (IntlTest::gTest)
        IntlTest::gTest->logln( message );
}

void it_logln( void )
{
    if (IntlTest::gTest)
        IntlTest::gTest->logln();
}

void it_info( UnicodeString message )
{
    if (IntlTest::gTest)
        IntlTest::gTest->info( message );
}

void it_infoln( UnicodeString message )
{
    if (IntlTest::gTest)
        IntlTest::gTest->infoln( message );
}

void it_infoln( void )
{
    if (IntlTest::gTest)
        IntlTest::gTest->infoln();
}

void it_err()
{
    if (IntlTest::gTest)
        IntlTest::gTest->err();
}

void it_err( UnicodeString message )
{
    if (IntlTest::gTest)
        IntlTest::gTest->err( message );
}

void it_errln( UnicodeString message )
{
    if (IntlTest::gTest)
        IntlTest::gTest->errln( message );
}

IntlTest& operator<<(IntlTest& test, const UnicodeString&   string)
{
/* NULL shouldn't happen */
//    if (&test == NULL)
//        return *((IntlTest*) NULL);
    test.log( string );
    return test;
}

IntlTest& operator<<(IntlTest& test, const char*    string)
{
/* NULL shouldn't happen */
//    if (&test == NULL)
//        return *((IntlTest*) NULL);
    test.log( string );
    return test;
}

IntlTest& operator<<(IntlTest& test, const int32_t num)
{
/* NULL shouldn't happen */
//    if (&test == NULL)
//        return *((IntlTest*) NULL);
    char convert[20];
    sprintf(convert, "%li", (long)num);
    test.log(convert);
    return test;
}

IntlTest& endl( IntlTest& test )
{
    test.logln();
    return test;
}

IntlTest& operator<<(IntlTest& test,  IntlTest& ( * _f)(IntlTest&))
{
    (*_f)(test);
    return test;
}


IntlTest::IntlTest()
{
    caller = NULL;
    path = NULL;
    LL_linestart = TRUE;
    errorCount = 0;
    verbose = FALSE;
    no_err_msg = FALSE;
    quick = FALSE;
    leaks = FALSE;
    testoutfp = stdout;
    LL_indentlevel = indentLevel_offset;
}

void IntlTest::setCaller( IntlTest* callingTest )
{
    caller = callingTest;
    if (caller) {
        verbose = caller->verbose;
        no_err_msg = caller->no_err_msg;
        quick = caller->quick;
        testoutfp = caller->testoutfp;
        LL_indentlevel = caller->LL_indentlevel + indentLevel_offset;
    }
}

UBool IntlTest::callTest( IntlTest& testToBeCalled, char* par )
{
    execCount--; // correct a previously assumed test-exec, as this only calls a subtest
    testToBeCalled.setCaller( this );
    return testToBeCalled.runTest( path, par );
}

void IntlTest::setPath( char* pathVal )
{
    this->path = pathVal;
}

UBool IntlTest::setVerbose( UBool verboseVal )
{
    UBool rval = this->verbose;
    this->verbose = verboseVal;
    return rval;
}

UBool IntlTest::setNoErrMsg( UBool no_err_msgVal )
{
    UBool rval = this->no_err_msg;
    this->no_err_msg = no_err_msgVal;
    return rval;
}

UBool IntlTest::setQuick( UBool quickVal )
{
    UBool rval = this->quick;
    this->quick = quickVal;
    return rval;
}

UBool IntlTest::setLeaks( UBool leaksVal )
{
    UBool rval = this->leaks;
    this->leaks = leaksVal;
    return rval;
}

int32_t IntlTest::getErrors( void )
{
    return errorCount;
}

UBool IntlTest::runTest( char* name, char* par )
{
    UBool rval;
    char* pos = NULL;

    if (name)
        pos = strchr( name, delim ); // check if name contains path (by looking for '/')
    if (pos) {
        path = pos+1;   // store subpath for calling subtest
        *pos = 0;       // split into two strings
    }else{
        path = NULL;
    }

    if (!name || (name[0] == 0) || (strcmp(name, "*") == 0)) {
        rval = runTestLoop( NULL, NULL );

    }else if (strcmp( name, "LIST" ) == 0) {
        this->usage();
        rval = TRUE;

    }else{
        rval = runTestLoop( name, par );
    }

    if (pos)
        *pos = delim;  // restore original value at pos
    return rval;
}

// call individual tests, to be overriden to call implementations
void IntlTest::runIndexedTest( int32_t index, UBool exec, const char* &name, char* par )
{
    // to be overriden by a method like:
    /*
    switch (index) {
        case 0: name = "First Test"; if (exec) FirstTest( par ); break;
        case 1: name = "Second Test"; if (exec) SecondTest( par ); break;
        default: name = ""; break;
    }
    */
    this->errln("*** runIndexedTest needs to be overriden! ***");
    name = ""; exec = exec; index = index; par = par;
}


UBool IntlTest::runTestLoop( char* testname, char* par )
{
    int32_t    index = 0;
    const char*   name;
    UBool  run_this_test;
    int32_t    lastErrorCount;
    UBool  rval = FALSE;
    UBool   lastTestFailed;

    IntlTest* saveTest = gTest;
    gTest = this;
    do {
        this->runIndexedTest( index, FALSE, name );
        if (!name || (name[0] == 0))
            break;
        if (!testname) {
            run_this_test = TRUE;
        }else{
            run_this_test = (UBool) (strcmp( name, testname ) == 0);
        }
        if (run_this_test) {
            lastErrorCount = errorCount;
            execCount++;
            this->runIndexedTest( index, TRUE, name, par );
            rval = TRUE; // at least one test has been called
            char msg[256];
            if (lastErrorCount == errorCount) {
                sprintf( msg, "---OK:   %s", name );
                lastTestFailed = FALSE;
            }else{
                sprintf(msg, "---ERRORS (%li) in %s", (long)(errorCount-lastErrorCount), name);

                for(int i=0;i<LL_indentlevel;i++) {
                    errorList += " ";
                }
                errorList += name;
                errorList += "\n";
                lastTestFailed = TRUE;
            }
            LL_indentlevel -= 3;
            if (lastTestFailed) {
                LL_message( "", TRUE);
            }
            LL_message( msg, TRUE);
            if (lastTestFailed) {
                LL_message( "", TRUE);
            }
            LL_indentlevel += 3;
        }
        index++;
    }while(name);

    gTest = saveTest;
    return rval;
}


/**
* Adds given string to the log if we are in verbose mode.
*/
void IntlTest::log( const UnicodeString &message )
{
    if( verbose ) {
        LL_message( message, FALSE );
    }
}

/**
* Adds given string to the log if we are in verbose mode. Adds a new line to
* the given message.
*/
void IntlTest::logln( const UnicodeString &message )
{
    if( verbose ) {
        LL_message( message, TRUE );
    }
}

void IntlTest::logln( void )
{
    if( verbose ) {
        LL_message( "", TRUE );
    }
}

/**
* Unconditionally adds given string to the log.
*/
void IntlTest::info( const UnicodeString &message )
{
  LL_message( message, FALSE );
}

/**
* Unconditionally adds given string to the log. Adds a new line to
* the given message.
*/
void IntlTest::infoln( const UnicodeString &message )
{
  LL_message( message, TRUE );
}

void IntlTest::infoln( void )
{
  LL_message( "", TRUE );
}

int32_t IntlTest::IncErrorCount( void )
{
    errorCount++;
    if (caller) caller->IncErrorCount();
    return errorCount;
}

void IntlTest::err() {
    IncErrorCount();
}

void IntlTest::err( const UnicodeString &message )
{
    IncErrorCount();
    if (!no_err_msg) LL_message( message, FALSE );
}

void IntlTest::errln( const UnicodeString &message )
{
    IncErrorCount();
    if (!no_err_msg) LL_message( message, TRUE );
}

/* convenience functions that include sprintf formatting */
void IntlTest::log(const char *fmt, ...)
{
    char buffer[512];
    va_list ap;

    va_start(ap, fmt);
    /* sprintf it just to make sure that the information is valid */
    vsprintf(buffer, fmt, ap);
    va_end(ap);
    if( verbose ) {
        log(UnicodeString(buffer, ""));
    }
}

void IntlTest::logln(const char *fmt, ...)
{
    char buffer[512];
    va_list ap;

    va_start(ap, fmt);
    /* sprintf it just to make sure that the information is valid */
    vsprintf(buffer, fmt, ap);
    va_end(ap);
    if( verbose ) {
        logln(UnicodeString(buffer, ""));
    }
}

/* convenience functions that include sprintf formatting */
void IntlTest::info(const char *fmt, ...)
{
    char buffer[512];
    va_list ap;

    va_start(ap, fmt);
    /* sprintf it just to make sure that the information is valid */
    vsprintf(buffer, fmt, ap);
    va_end(ap);
    info(UnicodeString(buffer, ""));
}

void IntlTest::infoln(const char *fmt, ...)
{
    char buffer[512];
    va_list ap;

    va_start(ap, fmt);
    /* sprintf it just to make sure that the information is valid */
    vsprintf(buffer, fmt, ap);
    va_end(ap);
    infoln(UnicodeString(buffer, ""));
}

void IntlTest::err(const char *fmt, ...)
{
    char buffer[512];
    va_list ap;

    va_start(ap, fmt);
    vsprintf(buffer, fmt, ap);
    va_end(ap);
    err(UnicodeString(buffer, ""));
}

void IntlTest::errln(const char *fmt, ...)
{
    char buffer[512];
    va_list ap;

    va_start(ap, fmt);
    vsprintf(buffer, fmt, ap);
    va_end(ap);
    errln(UnicodeString(buffer, ""));
}

void IntlTest::printErrors()
{
     IntlTest::LL_message(errorList, TRUE);
}

void IntlTest::LL_message( UnicodeString message, UBool newline )
{
    // string that starts with a LineFeed character and continues
    // with spaces according to the current indentation
    static const UChar indentUChars[] = {
        '\n',
        32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32,
        32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32,
        32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32,
        32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32,
        32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32,
        32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32,
        32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32,
        32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32,
        32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32,
        32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32
    };
    UnicodeString indent(FALSE, indentUChars, 1 + LL_indentlevel);

    char buffer[10000];
    int32_t length;

    // stream out the indentation string first if necessary
    length = indent.extract(1, indent.length(), buffer, sizeof(buffer));
    if (length > 0) {
        fwrite(buffer, sizeof(*buffer), length, testoutfp);
    }

    // replace each LineFeed by the indentation string
    message.findAndReplace(UnicodeString((UChar)'\n'), indent);

    // stream out the message
    length = message.extract(0, message.length(), buffer, sizeof(buffer));
    if (length > 0) {
        fwrite(buffer, sizeof(*buffer), length, testoutfp);
    }

    if (newline) {
        char newLine = '\n';
        fwrite(&newLine, sizeof(newLine), 1, testoutfp);
    }

    // A newline usually flushes the buffer, but
    // flush the message just in case of a core dump.
    fflush(testoutfp);
}

/**
* Print a usage message for this test class.
*/
void IntlTest::usage( void )
{
    UBool save_verbose = setVerbose( TRUE );
    logln("Test names:");
    logln("-----------");

    int32_t index = 0;
    const char* name = NULL;
    do{
        this->runIndexedTest( index, FALSE, name );
        if (!name) break;
        logln(name);
        index++;
    }while (name && (name[0] != 0));
    setVerbose( save_verbose );
}


// memory leak reporting software will be able to take advantage of the testsuite
// being run a second time local to a specific method in order to report only actual leaks
UBool
IntlTest::run_phase2( char* name, char* par ) // supports reporting memory leaks
{
    UnicodeString* strLeak = new UnicodeString("forced leak"); // for verifying purify filter
    strLeak->append(" for verifying purify filter");
    return this->runTest( name, par );
}


#if UCONFIG_NO_LEGACY_CONVERSION
#   define TRY_CNV_1 "iso-8859-1"
#   define TRY_CNV_2 "ibm-1208"
#else
#   define TRY_CNV_1 "iso-8859-7"
#   define TRY_CNV_2 "sjis"
#endif

int
main(int argc, char* argv[])
{
    UBool syntax = FALSE;
    UBool all = FALSE;
    UBool verbose = FALSE;
    UBool no_err_msg = FALSE;
    UBool quick = TRUE;
    UBool name = FALSE;
    UBool leaks = FALSE;
    UBool warnOnMissingData = FALSE;
    UErrorCode errorCode = U_ZERO_ERROR;
    UConverter *cnv = NULL;
    const char *warnOrErr = "Failure"; 
    const char* zone = "America/Los_Angeles";
    int argzone = -1;

    /* This must be tested before using anything! */
    MutexTest::gMutexInitialized = umtx_isInitialized(NULL);

#ifdef XP_MAC_CONSOLE
    argc = ccommand( &argv );
#endif

    /* try opening the data from dll instead of the dat file */
    cnv = ucnv_open(TRY_CNV_1, &errorCode);
    if(cnv != 0) {
        /* ok */
        ucnv_close(cnv);
    } else {
        fprintf(stderr,
                "#### WARNING! The converter for " TRY_CNV_1 " cannot be loaded from data dll/so."
                "Proceeding to load data from dat file.\n");
        errorCode = U_ZERO_ERROR;

    }
    // If user didn't set ICU_DATA, attempt to generate one.
    IntlTest::setICU_DATA();

    for (int i = 1; i < argc; ++i) {
        if (argv[i][0] == '-') {
            const char* str = argv[i] + 1;
            if (strcmp("verbose", str) == 0)
                verbose = TRUE;
            else if (strcmp("v", str) == 0)
                verbose = TRUE;
            else if (strcmp("noerrormsg", str) == 0)
                no_err_msg = TRUE;
            else if (strcmp("n", str) == 0)
                no_err_msg = TRUE;
            else if (strcmp("exhaustive", str) == 0)
                quick = FALSE;
            else if (strcmp("e", str) == 0)
                quick = FALSE;
            else if (strcmp("all", str) == 0)
                all = TRUE;
            else if (strcmp("a", str) == 0)
                all = TRUE;
            else if (strcmp("leaks", str) == 0)
                leaks = TRUE;
            else if (strcmp("l", str) == 0)
                leaks = TRUE;
            else if (strcmp("w", str) == 0) {
              warnOnMissingData = TRUE;
              warnOrErr = "WARNING";
            }
#if !UCONFIG_NO_FORMATTING
            else if (strcmp("tz", str) == 0) {
                zone = 0;
                if ((i+1) < argc) {
                    switch (argv[i+1][0]) {
                    case 0:
                        argzone = ++i; /* consume empty string in {-tz ""} */
                        break;
                    case '-':
                        break; /* don't process next arg if it is -x */
                    default:
                        argzone = ++i;
                        zone = argv[argzone]; /* next arg is zone */
                        break;
                    }
                }
            }
#endif
            else {
                syntax = TRUE;
            }
        }else{
            name = TRUE;
        }
    }

    if (!all && !name) {
        all = TRUE;
    } else if (all && name) {
        syntax = TRUE;
    }

    if (syntax) {
        fprintf(stdout,
                "### Syntax:\n"
                "### IntlTest [-option1 -option2 ...] [testname1 testname2 ...] \n"
                "### where options are: verbose (v), all (a), noerrormsg (n), \n"
                "### exhaustive (e), leaks (l)"
#if !UCONFIG_NO_FORMATTING
                                              ", and default time zone (tz)"
#endif
                                                                           ". \n"
#if !UCONFIG_NO_FORMATTING
                "### -tz takes an optional zone argument.  If absent, \n"
                "### or if the empty string, then the default time zone \n"
                "### is not set (host time zone is used). \n"
#endif
                "### (Specify either -all (shortcut -a) or a test name). \n"
                "### -all will run all of the tests.\n"
                "### \n"
                "### To get a list of the test names type: intltest LIST \n"
                "### To run just the utility tests type: intltest utility \n"
                "### \n"
                "### Test names can be nested using slashes (\"testA/subtest1\") \n"
                "### For example to list the utility tests type: intltest utility/LIST \n"
                "### To run just the Locale test type: intltest utility/LocaleTest \n"
                "### \n"
                "### A parameter can be specified for a test by appending '@' and the value \n"
                "### to the testname. \n\n");
        return 1;
    }

    UBool all_tests_exist = TRUE;
    MajorTestLevel major;
    major.setVerbose( verbose );
    major.setNoErrMsg( no_err_msg );
    major.setQuick( quick );
    major.setLeaks( leaks );
    fprintf(stdout, "-----------------------------------------------\n");
    fprintf(stdout, " IntlTest (C++) Test Suite for                 \n");
    fprintf(stdout, "   International Components for Unicode %s\n", U_ICU_VERSION);
    fprintf(stdout, "-----------------------------------------------\n");
    fprintf(stdout, " Options:                                       \n");
    fprintf(stdout, "   all (a)               : %s\n", (all?        "On" : "Off"));
    fprintf(stdout, "   Verbose (v)           : %s\n", (verbose?    "On" : "Off"));
    fprintf(stdout, "   No error messages (n) : %s\n", (no_err_msg? "On" : "Off"));
    fprintf(stdout, "   Exhaustive (e)        : %s\n", (!quick?     "On" : "Off"));
    fprintf(stdout, "   Leaks (l)             : %s\n", (leaks?      "On" : "Off"));
#if !UCONFIG_NO_FORMATTING
    fprintf(stdout, "   Time zone (tz)        : %s\n", (zone?       zone : "UNSET"));
#endif
    fprintf(stdout, "-----------------------------------------------\n");

    // Check that u_init() works
    errorCode = U_ZERO_ERROR;
    u_init(&errorCode);
    if (U_FAILURE(errorCode)) {
        fprintf(stdout,
            "*** u_init() failed with error code = %s\n"
                "*** Check the ICU_DATA environment variable and\n"
                "*** check that the data files are present.\n",
                u_errorName(errorCode));
        if(!warnOnMissingData) {
          fprintf(stdout, "*** Exiting.  Use the '-w' option if data files were\n*** purposely removed, to continue test anyway.\n");
          return 1;
        }
    }


    // initial check for the default converter
    errorCode = U_ZERO_ERROR;
    cnv = ucnv_open(0, &errorCode);
    if(cnv != 0) {
        // ok
        ucnv_close(cnv);
    } else {
        fprintf(stdout,
                "*** %s! The default converter [%s] cannot be opened.\n"
                "*** Check the ICU_DATA environment variable and\n"
                "*** check that the data files are present.\n",
                warnOrErr, ucnv_getDefaultName());
        if(!warnOnMissingData) {
          fprintf(stdout, "*** Exiting.  Use the '-w' option if data files were\n*** purposely removed, to continue test anyway.\n");
          return 1;
        }
    }

    // try more data
    cnv = ucnv_open(TRY_CNV_2, &errorCode);
    if(cnv != 0) {
        // ok
        ucnv_close(cnv);
    } else {
        fprintf(stdout,
                "*** %s! The converter for " TRY_CNV_2 " cannot be opened.\n"
                "*** Check the ICU_DATA environment variable and \n"
                "*** check that the data files are present.\n", warnOrErr);
        if(!warnOnMissingData) {
          fprintf(stdout, "*** Exiting.  Use the '-w' option if data files were\n*** purposely removed, to continue test anyway.\n");
          return 1;
        }
    }

    UResourceBundle *rb = ures_open(0, "en", &errorCode);
    ures_close(rb);
    if(U_FAILURE(errorCode)) {
        fprintf(stdout,
                "*** %s! The \"en\" locale resource bundle cannot be opened.\n"
                "*** Check the ICU_DATA environment variable and \n"
                "*** check that the data files are present.\n", warnOrErr);
        if(!warnOnMissingData) {
          fprintf(stdout, "*** Exiting.  Use the '-w' option if data files were\n*** purposely removed, to continue test anyway.\n");
          return 1;
        }
    }

#if !UCONFIG_NO_FORMATTING
    /* Set the default time zone */
    if (zone != 0) {
        UErrorCode ec = U_ZERO_ERROR;
        UnicodeString id(zone, ""), s;
        TimeZone *z = TimeZone::createTimeZone(id);
        if (z == 0) {
            fprintf(stdout,
                    "*** Error: TimeZone::createTimeZone(\"%s\") returned 0.\n",
                    zone);
        } else if (z->getID(s) != id) {
            fprintf(stdout,
                    "*** Error: \"%s\" is not a valid time zone.\n",
                    zone);
            delete z;
            z = 0;
        }
        if (z == 0) {
            fprintf(stdout,
                    "*** Exiting.  Use the '-tz' option with no argument to\n"
                    "*** bypass the setting of the default time zone.\n");
            return 1;
        }
        TimeZone::adoptDefault(z);
    }
#endif

    /* TODO: Add option to call u_cleanup and rerun tests. */
    if (all) {
        major.runTest();
        if (leaks) {
            major.run_phase2( NULL, NULL );
        }
    }else{
        for (int i = 1; i < argc; ++i) {
            if (i == argzone) {
                continue;
            }
            if (argv[i][0] != '-') {
                char* name = argv[i];
                fprintf(stdout, "\n=== Handling test: %s: ===\n", name);
                char* parameter = strchr( name, '@' );
                if (parameter) {
                    *parameter = 0;
                    parameter += 1;
                }
                execCount = 0;
                UBool res = major.runTest( name, parameter );
                if (leaks && res) {
                    major.run_phase2( name, parameter );
                }
                if (!res || (execCount <= 0)) {
                    fprintf(stdout, "\n---ERROR: Test doesn't exist: %s!\n", name);
                    all_tests_exist = FALSE;
                }
            }
        }
    }

#if !UCONFIG_NO_FORMATTING
    CalendarTimeZoneTest::cleanup();
#endif

    free(_testDataPath);
    _testDataPath = 0;

    fprintf(stdout, "\n--------------------------------------\n");
    if (major.getErrors() == 0) {
        /* Call it twice to make sure that the defaults were reset. */
        /* Call it before the OK message to verify proper cleanup. */
        u_cleanup();
     u_cleanup();

        fprintf(stdout, "OK: All tests passed without error.\n");
    }else{
        fprintf(stdout, "Errors in total: %ld.\n", (long)major.getErrors());
        major.printErrors();

        /* Call afterwards to display errors. */
        u_cleanup();
    }

    fprintf(stdout, "--------------------------------------\n");

    if (!MutexTest::gMutexInitialized) {
        fprintf(stderr,
            "#### WARNING!\n"
            "  The global mutex was not initialized during C++ static initialization.\n"
            "  You must use an ICU API in a single thread, like ucnv_open() or\n"
            "  uloc_countAvailable(), before using ICU in multiple threads.\n"
            "  Most ICU API functions will initialize the global mutex for you.\n"
            "  If you are using ICU in a single threaded application, please ignore this\n"
            "  warning.\n"
            "#### WARNING!\n"
            );
    }
    if (execCount <= 0) {
        fprintf(stdout, "***** Not all called tests actually exist! *****\n");
    }
    return major.getErrors();
}

const char* IntlTest::loadTestData(UErrorCode& err){
    if( _testDataPath == NULL){
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
            it_errln((UnicodeString)"Could not load testtypes.res in testdata bundle with path " + tdpath + (UnicodeString)" - " + u_errorName(err));
            return "";
        }
        ures_close(test);
        _testDataPath = tdpath;
        return _testDataPath;
    }
    return _testDataPath;
}

const char* IntlTest::fgDataDir = NULL;

/* returns the path to icu/source/data */
const char *  IntlTest::pathToDataDirectory()
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

/*
 * This is a variant of cintltst/ccolltst.c:CharsToUChars().
 * It converts a character string into a UnicodeString, with
 * unescaping \u sequences.
 */
UnicodeString CharsToUnicodeString(const char* chars)
{
    UnicodeString str(chars, ""); // Invariant conversion
    return str.unescape();
}

/*
 * Hey, Emacs, please set the following:
 *
 * Local Variables:
 * indent-tabs-mode: nil
 * End:
 *
 */

