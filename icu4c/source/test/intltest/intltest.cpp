/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/



/**
 * IntlTest is a base class for tests.
 */


#include <limits.h>
#include <stdio.h>
#include <string.h>
#include <assert.h>

#include "unicode/utypes.h"
#include "unicode/unistr.h"
#include "unicode/ures.h"
#include "unicode/coll.h"
#include "unicode/smpdtfmt.h"
#include "unicode/ustring.h"

#include "intltest.h"
#include "itmajor.h"

#ifdef XP_MAC
#include <console.h>
#include "Files.h"
#endif
static char* _testDirectory=NULL;
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
  
  sprintf(buffer, "%d", num);
  assert(danger == 'p');
  
  return left + buffer;
}

UnicodeString 
operator+(const UnicodeString& left, 
      unsigned long num)
{
  char buffer[64];    // nos changed from 10 to 64
  char danger = 'p';  // guard against overrunning the buffer (rtg)
  
  sprintf(buffer, "%u", num);
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

// used for collation result reporting, defined here for convenience
// (maybe moved later)
void 
IntlTest::reportCResult( UnicodeString &source, UnicodeString &target,
             CollationKey &sourceKey, CollationKey &targetKey,
             Collator::EComparisonResult compareResult,
             Collator::EComparisonResult keyResult,
                         Collator::EComparisonResult expectedResult )
{
    if (expectedResult < -1 || expectedResult > 1)
    {
        errln("***** invalid call to reportCResult ****");
        return;
    }
  
    UBool ok1 = (compareResult == expectedResult);
    UBool ok2 = (keyResult == expectedResult);

    if (ok1 && ok2 && !verbose) {
        // Keep non-verbose, passing tests fast
        return;
    } else {
        UnicodeString msg1(ok1 ? "Ok: compare(" : "FAIL: compare(");
        UnicodeString msg2(", "), msg3(") returned "), msg4("; expected ");
        UnicodeString prettySource, prettyTarget, sExpect, sResult;
      
        prettify(source, prettySource);
        prettify(target, prettyTarget);
        appendCompareResult(compareResult, sResult);
        appendCompareResult(expectedResult, sExpect);
      
        if (ok1) {
            logln(msg1 + prettySource + msg2 + prettyTarget + msg3 + sResult);
        } else {
            errln(msg1 + prettySource + msg2 + prettyTarget + msg3 + sResult + msg4 + sExpect);
        }

        msg1 = UnicodeString(ok2 ? "Ok: key(" : "FAIL: key(");
        msg2 = ").compareTo(key(";
        msg3 = ")) returned ";
      
        appendCompareResult(keyResult, sResult);
      
        if (ok2) {
            logln(msg1 + prettySource + msg2 + prettyTarget + msg3 + sResult);
        } else {
            errln(msg1 + prettySource + msg2 + prettyTarget + msg3 + sResult + msg4 + sExpect);
      
            msg1 = "  ";
            msg2 = " vs. ";
      
            prettify(sourceKey, prettySource);
            prettify(targetKey, prettyTarget);
      
            errln(msg1 + prettySource + msg2 + prettyTarget);
        }
    }
}

// Append a hex string to the target
UnicodeString& 
IntlTest::appendHex(uint32_t number, 
            int8_t digits, 
            UnicodeString& target)
{
  static const UnicodeString digitString("0123456789ABCDEF");

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

UnicodeString& 
IntlTest::appendCompareResult(Collator::EComparisonResult result, 
                  UnicodeString& target)
{
  if (result == Collator::LESS)
    {
      target += "LESS";
    }
  else if (result == Collator::EQUAL)
    {
      target += "EQUAL";
    }
  else if (result == Collator::GREATER)
    {
      target += "GREATER";
    }
  else
    {
      UnicodeString huh = "?";

      target += (huh + (int32_t)result);
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
  
  for (i = 0; i < source.length(); i += 1)
    {
      UChar ch = source[i];

      if (ch < 0x09 || (ch > 0x0A && ch < 0x20)|| ch > 0x7E)
    {
      target += "\\u";
      appendHex(ch, 4, target);
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
IntlTest::prettify(const UnicodeString &source) 
           
{
  int32_t i;
  UnicodeString target;
  target.remove();
  target += "\"";
  
  for (i = 0; i < source.length(); i += 1)
    {
      UChar ch = source[i];

      if (ch < 0x09 || (ch > 0x0A && ch < 0x20)|| ch > 0x7E)
    {
      target += "\\u";
      appendHex(ch, 4, target);
        }
      else
    {
      target += ch;
        }
    }

  target += "\"";

  return target;
}
// Produce a printable representation of a CollationKey
UnicodeString &IntlTest::prettify(const CollationKey &source, UnicodeString &target)
{
    int32_t i, byteCount;
    const uint8_t *bytes = source.getByteArray(byteCount);

    target.remove();
    target += "[";

    for (i = 0; i < byteCount; i += 1)
    {
        appendHex(bytes[i], 2, target);
        target += " ";
    }

    target += "]";

    return target;
}

void
IntlTest::pathnameInContext( char* fullname, int32_t maxsize, const char* relPath ) //nosmac
{
    const char* mainDir;
    char  sepChar;
    const char inpSepChar = '|';

    mainDir = u_getDataDirectory();
    sepChar = U_FILE_SEP_CHAR;
    char sepString[] = U_FILE_SEP_STRING;

    #if defined(_WIN32) || defined(WIN32) || defined(__OS2__) || defined(OS2)
        char mainDirBuffer[200];
        if(mainDir!=NULL) {
            strcpy(mainDirBuffer, mainDir);
            strcat(mainDirBuffer, "..\\..");
        } else {
            mainDirBuffer[0]='\0';
        }
        mainDir=mainDirBuffer;
    #elif defined(_AIX) || defined(U_SOLARIS) || defined(U_LINUX) || defined(HPUX) || defined(POSIX) || defined(OS390)
        char mainDirBuffer[200];
        strcpy(mainDirBuffer, u_getDataDirectory());
        strcat(mainDirBuffer, "/../");
        mainDir = mainDirBuffer;
    #elif defined(XP_MAC)
        Str255 volName;
        int16_t volNum;
        OSErr err = GetVol( volName, &volNum );
        if (err != noErr) volName[0] = 0;
        mainDir = (char*) &(volName[1]);
        mainDir[volName[0]] = 0;
    #else
        mainDir = "";
    #endif
/*
    #if defined(_WIN32) || defined(WIN32) || defined(__OS2__) || defined(OS2)
        char mainDirBuffer[200];
        if(mainDir!=NULL) {
            strcpy(mainDirBuffer, mainDir);
            strcat(mainDirBuffer, "..\\..");
        } else {
            mainDirBuffer[0]='\0';
        }
        mainDir=mainDirBuffer;
        sepChar = '\\';
    #elif defined(_AIX) || defined(SOLARIS) || defined(U_LINUX) || defined(HPUX) || defined(OS390)
        mainDir = getenv("HOME");
        sepChar = '/';
    #elif defined(XP_MAC)
        Str255 volName;
        int16_t volNum;
        OSErr err = GetVol( volName, &volNum );
        if (err != noErr) volName[0] = 0;
        mainDir = (char*) &(volName[1]);
        mainDir[volName[0]] = 0;
        sepChar = ':';
    #else
        mainDir = "";
        sepChar = '\\';
    #endif
*/
    
    if (relPath[0] == '|') relPath++;
    int32_t lenMainDir = strlen( mainDir );
    int32_t lenRelPath = strlen( relPath );
    if (maxsize < lenMainDir + lenRelPath + 2) { fullname[0] = 0; return; }
    strcpy( fullname, mainDir );
    strcat( fullname, sepString );
    strcat( fullname, relPath );
    char* tmp = strchr( fullname, inpSepChar );
    while (tmp) {
        *tmp = sepChar;
        tmp = strchr( tmp+1, inpSepChar );
    }

}

/**
 * Functions to get and set the directory containing the Test files.
 */

const char*
IntlTest::getTestDirectory()
{
    if (_testDirectory == NULL) 
    {
#if defined(_AIX) || defined(U_SOLARIS) || defined(U_LINUX) || defined(HPUX) || defined(POSIX) || defined(OS390)
      setTestDirectory("source|test|testdata|");
#else
      setTestDirectory("icu|source|test|testdata|");
#endif
    }
    return _testDirectory;
}

void
IntlTest::setTestDirectory(const char* newDir) 
{
    char newTestDir[256];
    IntlTest::pathnameInContext(newTestDir, sizeof(newTestDir), newDir); 
    if(_testDirectory != NULL)
        delete _testDirectory;
    _testDirectory = new char[strlen(newTestDir) + 1];
    strcpy(_testDirectory, newTestDir);
}

//--------------------------------------------------------------------------------------

static const int32_t indentLevel_offset = 3;
static const char delim = '/';

IntlTest* IntlTest::gTest = NULL;

static int32_t execCount = 0;

void it_log( UnicodeString message )
{
    if (IntlTest::gTest) IntlTest::gTest->log( message );
}
    
void it_logln( UnicodeString message )
{
    if (IntlTest::gTest) IntlTest::gTest->logln( message );
}
    
void it_logln( void )
{
    if (IntlTest::gTest) IntlTest::gTest->logln();
}
    
void it_err()
{
    if (IntlTest::gTest) IntlTest::gTest->err();
}
        
void it_err( UnicodeString message )
{
    if (IntlTest::gTest) IntlTest::gTest->err( message );
}
    
void it_errln( UnicodeString message )
{
    if (IntlTest::gTest) IntlTest::gTest->errln( message );
}

IntlTest& operator<<(IntlTest& test, const UnicodeString&   string)
{
    if (&test == NULL) return *((IntlTest*) NULL);
    test.log( string );
    return test;
}

IntlTest& operator<<(IntlTest& test, const char*    string)
{
    if (&test == NULL) return *((IntlTest*) NULL);
    test.log( string );
    return test;
}

IntlTest& operator<<(IntlTest& test, const int32_t num)
{
    if (&test == NULL) return *((IntlTest*) NULL);
    char convert[20];
    sprintf( convert, "%li", num );
    test.log( convert );
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

void IntlTest::setPath( char* path )
{
    this->path = path;
}

UBool IntlTest::setVerbose( UBool verbose )
{
    UBool rval = this->verbose;
    this->verbose = verbose;
    return rval;
}

UBool IntlTest::setNoErrMsg( UBool no_err_msg )
{
    UBool rval = this->no_err_msg;
    this->no_err_msg = no_err_msg;
    return rval;
}

UBool IntlTest::setQuick( UBool quick )
{
    UBool rval = this->quick;
    this->quick = quick;
    return rval;
}

UBool IntlTest::setLeaks( UBool leaks )
{
    UBool rval = this->leaks;
    this->leaks = leaks;
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

    if (name) pos = strchr( name, delim ); // check if name contains path (by looking for '/')
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

    if (pos) *pos = delim;  // restore original value at pos
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
    
    IntlTest* saveTest = gTest;
    gTest = this;
    do {
        this->runIndexedTest( index, FALSE, name );
        if (!name || (name[0] == 0)) break;
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
            }else{
                sprintf( msg, "---ERRORS (%li) in %s", (errorCount-lastErrorCount), name );
            }
            LL_indentlevel -= 3;
            LL_message( "", TRUE); 
            LL_message( msg, TRUE); 
            LL_message( "", TRUE);
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
    length = indent.extract(1, indent.length(), buffer);
    fwrite(buffer, sizeof(*buffer), length, testoutfp);

    // replace each LineFeed by the indentation string
    message.findAndReplace(UnicodeString((UChar)'\n'), indent);

    // stream out the message
    length = message.extract(0, message.length(), buffer);
    fwrite(buffer, sizeof(*buffer), length, testoutfp);

    if (newline) {
        char newLine = '\n';
        fwrite(&newLine, sizeof(newLine), 1, testoutfp);
    }
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
    return this->runTest( name, par );
}


int
main(int argc, char* argv[])
{

#ifdef XP_MAC
    argc = ccommand( &argv );
#endif

    UBool syntax = FALSE;
    UBool all = TRUE;
    UBool verbose = FALSE;
    UBool no_err_msg = FALSE;
    UBool quick = TRUE;
    UBool name = FALSE;
    UBool leaks = FALSE;

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
            else {
                syntax = TRUE;
            }
        }else{
            name = TRUE;
            all = FALSE;
        }
    }

    if (all && name) syntax = TRUE;
    if (!all && !name) syntax = TRUE;
 
    if (syntax) {
        fprintf(stdout,
                "### Syntax:\n"
                "### IntlTest [-option1 -option2 ...] [testname1 testname2 ...] \n"
                "### where options are: verbose (v), all (a), noerrormsg (n), \n"
                "### exhaustive (e) and leaks (l). \n"
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
    fprintf(stdout, " IntlTest Test Suite for                       \n");
    fprintf(stdout, "   International Classes for Unicode           \n");
    fprintf(stdout, "-----------------------------------------------\n");
    fprintf(stdout, " Options:                                       \n");
    fprintf(stdout, "   all (a)               : %s\n", (all?        "On" : "Off"));
    fprintf(stdout, "   Verbose (v)           : %s\n", (verbose?    "On" : "Off"));
    fprintf(stdout, "   No error messages (n) : %s\n", (no_err_msg? "On" : "Off"));
    fprintf(stdout, "   Exhaustive (e)        : %s\n", (!quick?     "On" : "Off"));
    fprintf(stdout, "   Leaks (l)             : %s\n", (leaks?      "On" : "Off"));
    fprintf(stdout, "-----------------------------------------------\n");

    // initial check for the default converter
    UErrorCode errorCode = U_ZERO_ERROR;
    UConverter *cnv = ucnv_open(0, &errorCode);
    if(cnv != 0) {
        // ok
        ucnv_close(cnv);
    } else {
        fprintf(stdout,
                "*** Failure! The default converter [%s] cannot be opened.\n"
                "*** Check the ICU_DATA environment variable and\n"
                "*** check that the data files are present.\n",
                ucnv_getDefaultName());
        return 1;
    }

    // try more data
    cnv = ucnv_open("iso-8859-7", &errorCode);
    if(cnv != 0) {
        // ok
        ucnv_close(cnv);
    } else {
        fprintf(stdout,
                "*** Failure! The converter for iso-8859-7 cannot be opened.\n"
                "*** Check the ICU_DATA environment variable and \n"
                "*** check that the data files are present.\n");
        return 1;
    }

    UResourceBundle *rb = ures_open(0, "en", &errorCode);
    if(U_SUCCESS(errorCode)) {
        // ok
        ures_close(rb);
    } else {
        fprintf(stdout,
                "*** Failure! The \"en\" locale resource bundle cannot be opened.\n"
                "*** Check the ICU_DATA environment variable and \n"
                "*** check that the data files are present.\n");
        return 1;
    }

    if (all) {
        major.runTest();
        if (leaks) {
            major.run_phase2( NULL, NULL );    
        }
    }else{
        for (int i = 1; i < argc; ++i) {
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
    fprintf(stdout, "\n--------------------------------------\n");
    if (major.getErrors() == 0) {
        fprintf(stdout, "OK: All tests passed without error.\n");
    }else{
        fprintf(stdout, "Errors in total: %ld.\n", major.getErrors());
    }

    fprintf(stdout, "--------------------------------------\n");

    if (execCount <= 0) {
        fprintf(stdout, "***** Not all called tests actually exist! *****\n");
    }

    return major.getErrors();
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

