/*
**********************************************************************
* Copyright (c) 2002-2003, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
**********************************************************************
*/
#ifndef _UPERF_H
#define _UPERF_H

#include "unicode/utypes.h"
#include "unicode/unistr.h"
#include "unicode/ustring.h"
#include "utimer.h"
#include "uoptions.h"
#include "ucbuf.h"

U_NAMESPACE_USE
// Use the TESTCASE macro in subclasses of IntlTest.  Define the
// runIndexedTest method in this fashion:
//
//| void MyTest::runIndexedTest(int32_t index, UBool exec,
//|                             const char* &name, char* /*par*/) {
//|     switch (index) {
//|         TESTCASE(0,TestSomething);
//|         TESTCASE(1,TestSomethingElse);
//|         TESTCASE(2,TestAnotherThing);
//|         default: 
//|             name = ""; 
//|             return NULL;
//|     }
//| }
#if 0
#define TESTCASE(id,test)                       \
    case id:                                    \
        name = #test;                           \
        if (exec) {                             \
            fprintf(stdout,#test "---");        \
            fprintf(stdout,"\n");               \
            return test();                      \
        }                                       \
        break

#endif
#define TESTCASE(id,test)                       \
    case id:                                    \
        name = #test;                           \
        if (exec) {                             \
            return test();                      \
        }                                       \
        break

/**
 * Subclasses of PerfTest will need to create subclasses of
 * Function that define a call() method which contains the code to
 * be timed.  They then call setTestFunction() in their "Test..."
 * method to establish this as the current test functor.
 */
class U_EXPORT UPerfFunction {
public:
    /**
     * Subclasses must implement this method to do the action to be
     * measured.
     */
    virtual void call(UErrorCode* status)=0;

    /**
     * Subclasses must implement this method to return positive
     * integer indicating the number of operations in a single
     * call to this object's call() method.
     */
    virtual long getOperationsPerIteration()=0;
    /**
     * Subclasses should override this method to return either positive
     * or negative integer indicating the number of events in a single
     * call to this object's call() method, if applicable
     * e.g: Number of breaks / iterations for break iterator
     */
    virtual long getEventsPerIteration(){
        return -1;
    }

    /**
     * Call call() n times in a tight loop and return the elapsed
     * milliseconds.  If n is small and call() is fast the return
     * result may be zero.  Small return values have limited
     * meaningfulness, depending on the underlying CPU and OS.
     */
     double time(int32_t n, UErrorCode* status) {
        UTimer start, stop;
        utimer_getTime(&start); 
        while (n-- > 0) {
            call(status);
        }
        utimer_getTime(&stop);
        return utimer_getDeltaSeconds(&start,&stop); // ms
    }

};


class U_EXPORT UPerfTest {
public:
    UBool run();
    UBool runTest( char* name = NULL, char* par = NULL ); // not to be overidden
        
    virtual void usage( void ) ;
    
    virtual ~UPerfTest();

    void setCaller( UPerfTest* callingTest ); // for internal use only
    
    void setPath( char* path ); // for internal use only
    
    ULine* getLines(UErrorCode& status);

    const UChar* getBuffer(int32_t& len,UErrorCode& status);

protected:
    UPerfTest(int32_t argc, const char* argv[], UErrorCode& status);

    virtual UPerfFunction* runIndexedTest( int32_t index, UBool exec, const char* &name, char* par = NULL ); // overide !

    virtual UBool runTestLoop( char* testname, char* par );

    virtual UBool callTest( UPerfTest& testToBeCalled, char* par );

    UBool        verbose;
    const char*  sourceDir;
    const char*  fileName;
    char*        resolvedFileName;
    const char*  encoding;
    UBool        uselen;
    int32_t      iterations;
    int32_t      passes;
    int32_t      time;
    const char** _argv;
    int32_t      _argc;
    int32_t      _remainingArgc;
    ULine*       lines;
    int32_t      numLines;
    UCHARBUF*    ucharBuf;
    UBool        line_mode;
    UBool        bulk_mode;
    UChar* buffer;
    int32_t      bufferLen;
    const char*  locale;
private:
    UPerfTest*   caller;
    char*        path;           // specifies subtests

// static members
public:
    static UPerfTest* gTest;
    static const char gUsageString[];
};

#endif

