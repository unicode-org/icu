/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-2003, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/


/**
 * IntlTest is a base class for tests.  */

#ifndef _INTLTEST
#define _INTLTEST

#include <stdio.h>

#include "unicode/utypes.h"
#include "unicode/unistr.h"
#include "unicode/fmtable.h"

U_NAMESPACE_USE

#ifdef OS390
// avoid collision with math.h/log()
// this must be after including utypes.h so that OS390 is actually defined
#pragma map(IntlTest::log( const UnicodeString &message ),"logos390")
#endif

#define it_out (*IntlTest::gTest)

//-----------------------------------------------------------------------------
//convenience classes to ease porting code that uses the Java
//string-concatenation operator (moved from findword test by rtg)
UnicodeString UCharToUnicodeString(UChar c);
UnicodeString operator+(const UnicodeString& left, const UnicodeString& right);
UnicodeString operator+(const UnicodeString& left, long num);
UnicodeString operator+(const UnicodeString& left, unsigned long num);
UnicodeString operator+(const UnicodeString& left, double num);
UnicodeString operator+(const UnicodeString& left, char num); 
UnicodeString operator+(const UnicodeString& left, short num);  
UnicodeString operator+(const UnicodeString& left, int num);      
UnicodeString operator+(const UnicodeString& left, unsigned char num);  
UnicodeString operator+(const UnicodeString& left, unsigned short num);  
UnicodeString operator+(const UnicodeString& left, unsigned int num);      
UnicodeString operator+(const UnicodeString& left, float num);
#if !UCONFIG_NO_FORMATTING
UnicodeString toString(const Formattable& f); // liu
#endif
//-----------------------------------------------------------------------------

// Use the TESTCASE macro in subclasses of IntlTest.  Define the
// runIndexedTest method in this fashion:
//
//| void MyTest::runIndexedTest(int32_t index, UBool exec,
//|                             const char* &name, char* /*par*/) {
//|     switch (index) {
//|         TESTCASE(0,TestSomething);
//|         TESTCASE(1,TestSomethingElse);
//|         TESTCASE(2,TestAnotherThing);
//|         default: name = ""; break;
//|     }
//| }
#define TESTCASE(id,test)             \
    case id:                          \
        name = #test;                 \
        if (exec) {                   \
            logln(#test "---");       \
            logln((UnicodeString)""); \
            test();                   \
        }                             \
        break

class IntlTest {
public:

    IntlTest();

    virtual UBool runTest( char* name = NULL, char* par = NULL ); // not to be overidden

    virtual UBool setVerbose( UBool verbose = TRUE );
    virtual UBool setNoErrMsg( UBool no_err_msg = TRUE );
    virtual UBool setQuick( UBool quick = TRUE );
    virtual UBool setLeaks( UBool leaks = TRUE );

    virtual int32_t getErrors( void );

    virtual void setCaller( IntlTest* callingTest ); // for internal use only
    virtual void setPath( char* path ); // for internal use only

    virtual void log( const UnicodeString &message );

    virtual void logln( const UnicodeString &message );

    virtual void logln( void );

    virtual void info( const UnicodeString &message );

    virtual void infoln( const UnicodeString &message );

    virtual void infoln( void );

    virtual void err(void);
    
    virtual void err( const UnicodeString &message );

    virtual void errln( const UnicodeString &message );

    // convenience functions: sprintf() + errln() etc.
    void log(const char *fmt, ...);
    void logln(const char *fmt, ...);
    void info(const char *fmt, ...);
    void infoln(const char *fmt, ...);
    void err(const char *fmt, ...);
    void errln(const char *fmt, ...);

    // Print ALL named errors encountered so far
    void printErrors(); 
        
    virtual void usage( void ) ;

    FILE *testoutfp;

protected:
    virtual void runIndexedTest( int32_t index, UBool exec, const char* &name, char* par = NULL ); // overide !

    virtual UBool runTestLoop( char* testname, char* par );

    virtual int32_t IncErrorCount( void );

    virtual UBool callTest( IntlTest& testToBeCalled, char* par );


    UBool      verbose;
    UBool      no_err_msg;
    UBool      quick;
    UBool      leaks;

private:
    UBool      LL_linestart;
    int32_t     LL_indentlevel;

    int32_t     errorCount;
    IntlTest*   caller;
    char*       path;           // specifies subtests

protected:
    virtual void LL_message( UnicodeString message, UBool newline );

    // used for collation result reporting, defined here for convenience

    static UnicodeString &prettify(const UnicodeString &source, UnicodeString &target);
    static UnicodeString prettify(const UnicodeString &source, UBool parseBackslash=FALSE);
    static UnicodeString &appendHex(uint32_t number, int32_t digits, UnicodeString &target);

    /* complete a relative path to a full pathname, and convert to platform-specific syntax. */
    /* The character seperating directories for the relative path is '|'.                    */
    static void pathnameInContext( char* fullname, int32_t maxsize, const char* relpath );

public:
    static void setICU_DATA();       // Set up ICU_DATA if necessary.

    static const char* pathToDataDirectory();

public:
    UBool run_phase2( char* name, char* par ); // internally, supports reporting memory leaks
    static const char* loadTestData(UErrorCode& err);

// static members
public:
    static IntlTest* gTest;
    static const char* fgDataDir;

};

void it_log( UnicodeString message );
void it_logln( UnicodeString message );
void it_logln( void );
void it_info( UnicodeString message );
void it_infoln( UnicodeString message );
void it_infoln( void );
void it_err(void);
void it_err( UnicodeString message );
void it_errln( UnicodeString message );

IntlTest& operator<<(IntlTest& test, const UnicodeString& string);
IntlTest& operator<<(IntlTest& test, const char* string);
IntlTest& operator<<(IntlTest& test, const int32_t num);

IntlTest& endl( IntlTest& test );
IntlTest& operator<<(IntlTest& test,  IntlTest& ( * _f)(IntlTest&));

/**
 * This is a variant of cintltst/ccolltst.c:CharsToUChars().
 * It converts a character string into a UnicodeString, with
 * unescaping \u sequences.
 */
extern UnicodeString CharsToUnicodeString(const char* chars);

#endif // _INTLTEST
