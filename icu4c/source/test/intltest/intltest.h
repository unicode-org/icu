/*
*******************************************************************************
*                                                                             *
* COPYRIGHT:                                                                  *
*   (C) Copyright Taligent, Inc.,  1997                                       *
*   (C) Copyright International Business Machines Corporation,  1997-1999     *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.        *
*   US Government Users Restricted Rights - Use, duplication, or disclosure   *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                    *
*                                                                             *
*******************************************************************************
*/

/*
 * IntlTest in java is Copyright (c) 1997 Sun Microsystems, Inc. All
 * Rights Reserved.  */

/**
 * IntlTest is a base class for tests.  It is modelled according to,
 * but not exactly similar to, JavaSoft's test class with the same
 * name.  */

#ifndef _INTLTEST
#define _INTLTEST

#include "utypes.h"
#include "unistr.h"
#include "coll.h"
#include "sortkey.h"
#include "fmtable.h" // liu

#include <iostream.h>

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
UnicodeString toString(const Formattable& f); // liu
//-----------------------------------------------------------------------------

class IntlTest {
public:

    IntlTest();

    virtual bool_t runTest( char* name = NULL, char* par = NULL ); // not to be overidden

    virtual bool_t setVerbose( bool_t verbose = TRUE );
    virtual bool_t setNoErrMsg( bool_t no_err_msg = TRUE );
    virtual bool_t setQuick( bool_t quick = TRUE );
    virtual bool_t setLeaks( bool_t leaks = TRUE );

    virtual int32_t getErrors( void );

    virtual void setCaller( IntlTest* callingTest ); // for internal use only
    virtual void setPath( char* path ); // for internal use only

    virtual void log( const UnicodeString &message );

    // convenience function; assume that the message contains only invariant charcters
    virtual void log( const char *message );
    
    virtual void logln( const UnicodeString &message );

    // convenience function; assume that the message contains only invariant charcters
    virtual void logln( const char *message );

    virtual void logln( void );

    virtual void err(void);
    
    virtual void err( const UnicodeString &message );

    // convenience function; assume that the message contains only invariant charcters
    virtual void err( const char *message );

    virtual void errln( const UnicodeString &message );

    // convenience function; assume that the message contains only invariant charcters
    virtual void errln( const char *message );
    
    virtual void usage( void ) ;

    ostream*    testout;

protected:
    virtual void runIndexedTest( int32_t index, bool_t exec, char* &name, char* par = NULL ); // overide !

    virtual bool_t runTestLoop( char* testname, char* par );

    virtual int32_t IncErrorCount( void );

    virtual bool_t callTest( IntlTest& testToBeCalled, char* par );


    bool_t      LL_linestart;
    int32_t     LL_indentlevel;

    bool_t      verbose;
    bool_t      no_err_msg;
    bool_t      quick;
    bool_t      leaks;
    int32_t     errorCount;
    IntlTest*   caller;
    char*       path;           // specifies subtests

    virtual void LL_message( UnicodeString message, bool_t newline );

    // used for collation result reporting, defined here for convenience
    virtual void reportCResult( UnicodeString &source, UnicodeString &target,
                                CollationKey &sourceKey, CollationKey &targetKey,
                                Collator::EComparisonResult compareResult,
                                Collator::EComparisonResult keyResult,
                                Collator::EComparisonResult expectedResult );

    static UnicodeString &prettify(const UnicodeString &source, UnicodeString &target);
    static UnicodeString &prettify(const CollationKey &source, UnicodeString &target);
    static UnicodeString &appendHex(uint32_t number, int8_t digits, UnicodeString &target);
    static UnicodeString &appendCompareResult(Collator::EComparisonResult result, UnicodeString &target);

    /* complete a relative path to a full pathname, and convert to platform-specific syntax. */
    /* The character seperating directories for the relative path is '|'.                    */
    static void pathnameInContext( char* fullname, int32_t maxsize, const char* relpath );
    /*The function to set the Test Directory*/
    static void setTestDirectory(const char* newDir);
    /*The function to get the Test Directory*/
    static const char* getTestDirectory(void);

public:
    bool_t run_phase2( char* name, char* par ); // internally, supports reporting memory leaks

// static members
public:
    static IntlTest* gTest;

};

inline void IntlTest::log( const char *message ) {
    log(UnicodeString(message, ""));
}

inline void IntlTest::logln( const char *message ) {
    logln(UnicodeString(message, ""));
}

inline void IntlTest::err( const char *message ) {
    err(UnicodeString(message, ""));
}

inline void IntlTest::errln( const char *message ) {
    errln(UnicodeString(message, ""));
}

void it_log( UnicodeString message );
void it_logln( UnicodeString message );
void it_logln( void );
void it_err(void);
void it_err( UnicodeString message );
void it_errln( UnicodeString message );

IntlTest& operator<<(IntlTest& test, const UnicodeString& string);
IntlTest& operator<<(IntlTest& test, const char* string);
IntlTest& operator<<(IntlTest& test, const int32_t num);

IntlTest& endl( IntlTest& test );
IntlTest& operator<<(IntlTest& test,  IntlTest& ( * _f)(IntlTest&));


#endif // _INTLTEST
