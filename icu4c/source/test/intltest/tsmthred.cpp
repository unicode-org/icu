/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1999-2003, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

#if defined(hpux)
# ifndef _INCLUDE_POSIX_SOURCE
#  define _INCLUDE_POSIX_SOURCE
# endif
#endif

#include "unicode/utypes.h"
#include "unicode/ustring.h"
#include "umutex.h"
#include "cmemory.h"
#include "cstring.h"
#include "uparse.h"


#if !defined(WIN32) && !defined(XP_MAC) && !defined(U_RHAPSODY)
#define POSIX 1
#endif

#if defined(POSIX) || defined(U_SOLARIS) || defined(U_AIX) || defined(U_HPUX)

#define HAVE_IMP

#if (ICU_USE_THREADS == 1)
#include <pthread.h>
#endif

#if defined(__hpux) && defined(HPUX_CMA)
# if defined(read)  // read being defined as cma_read causes trouble with iostream::read
#  undef read
# endif
#endif

/* Define __EXTENSIONS__ for Solaris and old friends in strict mode. */
#ifndef __EXTENSIONS__
#define __EXTENSIONS__
#endif

#include <signal.h>

/* Define _XPG4_2 for Solaris and friends. */
#ifndef _XPG4_2
#define _XPG4_2
#endif

/* Define __USE_XOPEN_EXTENDED for Linux and glibc. */
#ifndef __USE_XOPEN_EXTENDED
#define __USE_XOPEN_EXTENDED 
#endif

/* Define _INCLUDE_XOPEN_SOURCE_EXTENDED for HP/UX (11?). */
#ifndef _INCLUDE_XOPEN_SOURCE_EXTENDED
#define _INCLUDE_XOPEN_SOURCE_EXTENDED
#endif

#include <unistd.h>

#endif
/* HPUX */
#ifdef sleep
#undef sleep
#endif



#include "tsmthred.h"


MultithreadTest::MultithreadTest()
{
}

MultithreadTest::~MultithreadTest()
{
}



#if (ICU_USE_THREADS==0)
void MultithreadTest::runIndexedTest( int32_t index, UBool exec, 
                const char* &name, char* par ) {
  if (exec) logln("TestSuite MultithreadTest: ");

  if(index == 0)
      name = "NO_THREADED_TESTS";
  else
      name = "";

  if(exec) { logln("MultithreadTest - test DISABLED.  ICU_USE_THREADS set to 0, check your configuration if this is a problem..");
  }
}
#else



// Note: A LOT OF THE FUNCTIONS IN THIS FILE SHOULD LIVE ELSEWHERE!!!!!
// Note: A LOT OF THE FUNCTIONS IN THIS FILE SHOULD LIVE ELSEWHERE!!!!!
//   -srl

#include <stdio.h>
#include <string.h>
#include <ctype.h>    // tolower, toupper

#include "unicode/putil.h"

/* for mthreadtest*/
#include "unicode/numfmt.h"
#include "unicode/choicfmt.h"
#include "unicode/msgfmt.h"
#include "unicode/locid.h"
#include "unicode/ucol.h"
#include "ucaconf.h"

#ifdef WIN32
#define HAVE_IMP

#   define VC_EXTRALEAN
#   define WIN32_LEAN_AND_MEAN
#   define NOGDI
#   define NOUSER
#   define NOSERVICE
#   define NOIME
#   define NOMCX
#include <windows.h>
#include <process.h>

struct Win32ThreadImplementation
{
    unsigned long fHandle;
};

extern "C" void __cdecl SimpleThreadProc(void *arg)
{
    ((SimpleThread*)arg)->run();
}

SimpleThread::SimpleThread()
:fImplementation(0)
{
    Win32ThreadImplementation *imp = new Win32ThreadImplementation;
    imp->fHandle = 0;

    fImplementation = imp;
}

SimpleThread::~SimpleThread()
{
    delete (Win32ThreadImplementation*)fImplementation;
}

int32_t SimpleThread::start()
{
    Win32ThreadImplementation *imp = (Win32ThreadImplementation*)fImplementation;
    if(imp->fHandle != NULL) {
        // The thread appears to have already been started.
        //   This is probably an error on the part of our caller.
        return -1;
    }

    imp->fHandle = _beginthread( SimpleThreadProc, 0 /*stack size*/ , (void *)this );
    if (imp->fHandle == -1) {
        // An error occured
        int err = errno;
        if (err == 0) {
            err = -1;
        }
        return err;
    }
    return 0;
}

void SimpleThread::sleep(int32_t millis)
{
    ::Sleep(millis);
}

#elif defined XP_MAC

// since the Mac has no preemptive threading (at least on MacOS 8), only
// cooperative threading, threads are a no-op.  We have no yield() calls
// anywhere in the ICU, so we are guaranteed to be thread-safe.

#define HAVE_IMP

SimpleThread::SimpleThread()
{}

SimpleThread::~SimpleThread()
{}

int32_t 
SimpleThread::start()
{ return 0; }

void 
SimpleThread::run()
{}

void 
SimpleThread::sleep(int32_t millis)
{}
#endif


#if defined(POSIX)||defined(U_SOLARIS)||defined(U_AIX)||defined(U_HPUX)
#define HAVE_IMP

struct PosixThreadImplementation
{
    pthread_t fThread;
};

extern "C" void* SimpleThreadProc(void *arg)
{
    ((SimpleThread*)arg)->run();
    return 0;
}

SimpleThread::SimpleThread() :fImplementation(0)
{
    PosixThreadImplementation *imp = new PosixThreadImplementation;
    fImplementation = imp;
}

SimpleThread::~SimpleThread()
{
    delete (PosixThreadImplementation*)fImplementation;
}

int32_t SimpleThread::start()
{
    PosixThreadImplementation *imp = (PosixThreadImplementation*)fImplementation;

    int32_t rc;

    pthread_attr_t attr;

#ifdef HPUX_CMA
    rc = pthread_attr_create(&attr);
    rc = pthread_create(&(imp->fThread),attr,&SimpleThreadProc,(void*)this);
    pthread_attr_delete(&attr);
#else
    rc = pthread_attr_init(&attr);
    rc = pthread_create(&(imp->fThread),&attr,&SimpleThreadProc,(void*)this);
    pthread_attr_destroy(&attr);
#endif
    return rc;

}

void SimpleThread::sleep(int32_t millis)
{
#ifdef U_SOLARIS
    sigignore(SIGALRM);
#endif

#ifdef HPUX_CMA
    cma_sleep(millis/100);
#elif defined(U_HPUX) || defined(OS390)
    millis *= 1000;
    while(millis >= 1000000) {
        usleep(999999);
        millis -= 1000000;
    }
    if(millis > 0) {
        usleep(millis);
    }
#else
    usleep(millis * 1000);
#endif
}

#endif
// end POSIX


#ifndef HAVE_IMP
#error  No implementation for threads! Cannot test.
0 = 216; //die
#endif


// *************** end fluff ******************

/* now begins the real test. */
void MultithreadTest::runIndexedTest( int32_t index, UBool exec, 
                const char* &name, char* /*par*/ ) {
    if (exec)
        logln("TestSuite MultithreadTest: ");
    switch (index) {
    case 0:
        name = "TestThreads";
        if (exec)
            TestThreads();
        break;
    case 1:
        name = "TestMutex";
        if (exec)
            TestMutex();
        break;
    case 2:
        name = "TestThreadedIntl";
#if !UCONFIG_NO_FORMATTING
        if (exec)
            TestThreadedIntl();
#endif
        break;
    case 3:
      name = "TestCollators";
#if !UCONFIG_NO_COLLATION
      if (exec)
        TestCollators();
#endif /* #if !UCONFIG_NO_COLLATION */
      break;
    default:
        name = "";
        break; //needed to end loop
    }
}


/* 
   TestThreads -- see if threads really work at all.

   Set up N threads pointing at N chars. When they are started, they will
   each sleep 1 second and then set their chars. At the end we make sure they
   are all set.
 */

#define THREADTEST_NRTHREADS 8

class TestThreadsThread : public SimpleThread
{
public:
    TestThreadsThread(char* whatToChange) { fWhatToChange = whatToChange; }
    virtual void run() { SimpleThread::sleep(1000); 
                         Mutex m;
                         *fWhatToChange = '*'; 
    }
private:
    char *fWhatToChange;
};

void MultithreadTest::TestThreads()
{
    char threadTestChars[THREADTEST_NRTHREADS + 1];
    SimpleThread *threads[THREADTEST_NRTHREADS];

    int32_t i;
    for(i=0;i<THREADTEST_NRTHREADS;i++)
    {
        threadTestChars[i] = ' ';
        threads[i] = new TestThreadsThread(&threadTestChars[i]);
    }
    threadTestChars[THREADTEST_NRTHREADS] = '\0';

    logln("->" + UnicodeString(threadTestChars) + "<- Firing off threads.. ");
    for(i=0;i<THREADTEST_NRTHREADS;i++)
    {
        if (threads[i]->start() != 0) {
            errln("Error starting thread %d", i);
        }
        SimpleThread::sleep(200);
        logln(" Subthread started.");
    }

    logln("Waiting for threads to be set..");

    int32_t patience = 40; // seconds to wait

    while(patience--)
    {
        int32_t count = 0;
        umtx_lock(NULL);
        for(i=0;i<THREADTEST_NRTHREADS;i++)
        {
            if(threadTestChars[i] == '*')
            {
                count++;
            }
        }
        umtx_unlock(NULL);
        
        if(count == THREADTEST_NRTHREADS)
        {
            logln("->" + UnicodeString(threadTestChars) + "<- Got all threads! cya");
            for(i=0;i<THREADTEST_NRTHREADS;i++)
            {
                delete threads[i];
            }
            return;
        }

        logln("->" + UnicodeString(threadTestChars) + "<- Waiting..");
        SimpleThread::sleep(500);
    }

    errln("->" + UnicodeString(threadTestChars) + "<- PATIENCE EXCEEDED!! Still missing some.");
    for(i=0;i<THREADTEST_NRTHREADS;i++)
    {
        delete threads[i];
    }
}


class TestMutexThread1 : public SimpleThread
{
public:
    TestMutexThread1() : fDone(FALSE) {}
    virtual void run()
    {
        Mutex m;                        // grab the lock first thing
        SimpleThread::sleep(900);      // then wait
        fDone = TRUE;                   // finally, set our flag
    }
public:
    UBool fDone;
};

class TestMutexThread2 : public SimpleThread
{
public:
    TestMutexThread2(TestMutexThread1& r) : fOtherThread(r), fDone(FALSE), fErr(FALSE) {}
    virtual void run()
    {
        SimpleThread::sleep(500);          // wait, make sure they aquire the lock
        fElapsed = uprv_getUTCtime();
        {
            Mutex m;                        // wait here

            fElapsed = uprv_getUTCtime() - fElapsed;

            if(fOtherThread.fDone == FALSE) 
                fErr = TRUE;                // they didnt get to it yet

            fDone = TRUE;               // we're done.
        }
    }
public:
    TestMutexThread1 & fOtherThread;
    UBool fDone, fErr;
    int32_t fElapsed;
private:
    /**
     * The assignment operator has no real implementation.
     * It is provided to make the compiler happy. Do not call.
     */
    TestMutexThread2& operator=(const TestMutexThread2&) { return *this; }
};

void MultithreadTest::TestMutex()
{
    /* this test uses printf so that we don't hang by calling UnicodeString inside of a mutex. */
    //logln("Bye.");
    //  printf("Warning: MultiThreadTest::Testmutex() disabled.\n");
    //  return; 

    if(verbose)
        printf("Before mutex.\n");
    {
        Mutex m;
        if(verbose)
            printf(" Exited 2nd mutex\n");
    }
    if(verbose)
        printf("exited 1st mutex. Now testing with threads:");

    TestMutexThread1  thread1;
    TestMutexThread2  thread2(thread1);
    if (thread2.start() != 0  || 
        thread1.start() != 0 ) {
        errln("Error starting threads.");
    }

    for(int32_t patience = 12; patience > 0;patience--)
    {
        // TODO:  Possible memory coherence issue in looking at fDone values
        //        that are set in another thread without the mutex here.
        if(thread1.fDone && verbose)
            printf("Thread1 done\n");

        if(thread1.fDone && thread2.fDone)
        {
            if(thread2.fErr)
                errln("Thread 2 says: thread1 didn't run before I aquired the mutex.");
            logln("took %lu seconds for thread2 to aquire the mutex.", thread2.fElapsed);
            return;
        }
        SimpleThread::sleep(1000);
    }
    if(verbose)
        printf("patience exceeded. [WARNING mutex may still be acquired.] ");
}

// ***********
// ***********   TestMultithreadedIntl.  Test the ICU in a multithreaded way. 




// ** First, some utility classes.

//
///* Here is an idea which needs more work
//   TestATest simply runs another Intltest subset against itself.
//    The correct subset of intltest that should be run in this way should be identified.
// */
//
//class TestATest : public SimpleThread
//{
//public:
//    TestATest(IntlTest &t) : fTest(t), fDone(FALSE) {}
//    virtual void run()
//    {
//       fTest.runTest(NULL,"TestNumberSpelloutFormat");
//       fErrs = fTest.getErrors();
//       fDone = TRUE;
//    }
//public:
//    IntlTest &fTest;
//    UBool    fDone;
//    int32_t   fErrs;
//};
//
//
//#include "itutil.h"
////#include "tscoll.h"
////#include "ittxtbd.h"
//#include "itformat.h"
////#include "itcwrap.h"
//
///* main code was:
//    IntlTestFormat formatTest;
////    IntlTestCollator collatorTest;
//
//  #define NUMTESTS 2
//    TestATest tests[NUMTESTS] = { TestATest(formatTest), TestATest(formatTest) };
//    char testName[NUMTESTS][20] = { "formatTest", "formatTest2" };
//*/


#include <string.h>

// * Show exactly where the string's differences lie.
UnicodeString showDifference(const UnicodeString& expected, const UnicodeString& result)
{
    UnicodeString res;
    res = expected + "<Expected\n";
    if(expected.length() != result.length())
        res += " [ Different lengths ] \n";
    else
    {
        for(int32_t i=0;i<expected.length();i++)
        {
            if(expected[i] == result[i])
            {
                res += " ";
            }
            else
            {
                res += "|";
            }
        }
        res += "<Differences";
        res += "\n";
    }
    res += result + "<Result\n";

    return res;
}


// ** ThreadWithStatus - a thread that we can check the status and error condition of


class ThreadWithStatus : public SimpleThread
{
public:
    UBool  getDone() { return fDone; }
    UBool  getError() { return (fErrors > 0); } 
    UBool  getError(UnicodeString& fillinError) { fillinError = fErrorString; return (fErrors > 0); } 
    virtual ~ThreadWithStatus(){}
protected:
    ThreadWithStatus() : fDone(FALSE), fErrors(0) {}
    void done() { fDone = TRUE; }
    void error(const UnicodeString &error) { fErrors++; fErrorString = error; done(); }
    void error() { error("An error occured."); }
private:
    UBool fDone;
    int32_t fErrors;
    UnicodeString fErrorString;
};

#define kFormatThreadIterations 20  // # of iterations per thread
#define kFormatThreadThreads    10  // # of threads to spawn
#define kFormatThreadPatience   60  // time in seconds to wait for all threads

#if !UCONFIG_NO_FORMATTING

// ** FormatThreadTest - a thread that tests performing a number of numberformats.


struct FormatThreadTestData
{
    double number;
    UnicodeString string;
    FormatThreadTestData(double a, const UnicodeString& b) : number(a),string(b) {}
} ;


void errorToString(UErrorCode theStatus, UnicodeString &string)
{
    string=u_errorName(theStatus);
}

// "Someone from {2} is receiving a #{0} error - {1}. Their telephone call is costing {3 number,currency}."

void formatErrorMessage(UErrorCode &realStatus, const UnicodeString& pattern, const Locale& theLocale,
                     UErrorCode inStatus0, /* statusString 1 */ const Locale &inCountry2, double currency3, // these numbers are the message arguments.
                     UnicodeString &result)
{
    if(U_FAILURE(realStatus))
        return; // you messed up

    UnicodeString errString1;
    errorToString(inStatus0, errString1);

    UnicodeString countryName2;
    inCountry2.getDisplayCountry(theLocale,countryName2);

    Formattable myArgs[] = {
        Formattable((int32_t)inStatus0),   // inStatus0      {0}
        Formattable(errString1), // statusString1 {1}
        Formattable(countryName2),  // inCountry2 {2}
        Formattable(currency3)// currency3  {3,number,currency}
    };

    MessageFormat *fmt = new MessageFormat("MessageFormat's API is broken!!!!!!!!!!!",realStatus);
    fmt->setLocale(theLocale);
    fmt->applyPattern(pattern, realStatus);
    
    if (U_FAILURE(realStatus)) {
        delete fmt;
        return;
    }

    FieldPosition ignore = 0;                      
    fmt->format(myArgs,4,result,ignore,realStatus);

    delete fmt;
};

static    UMTX ftMutex;

class FormatThreadTest : public ThreadWithStatus
{
public:
    FormatThreadTest() // constructor is NOT multithread safe.
        : ThreadWithStatus(),
        fOffset(0)
        // the locale to use
    {
        static int32_t fgOffset = 0;
        fgOffset += 3;
        fOffset = fgOffset;
    }


    virtual void run()
    {
        // Keep this data here to avoid static initialization.
        FormatThreadTestData kNumberFormatTestData[] = 
        {
            FormatThreadTestData((double)5.0, UnicodeString("5", "")),
            FormatThreadTestData( 6.0, UnicodeString("6", "")),
            FormatThreadTestData( 20.0, UnicodeString("20", "")),
            FormatThreadTestData( 8.0, UnicodeString("8", "")),
            FormatThreadTestData( 8.3, UnicodeString("8.3", "")),
            FormatThreadTestData( 12345, UnicodeString("12,345", "")),
            FormatThreadTestData( 81890.23, UnicodeString("81,890.23", "")),
        };
        int32_t kNumberFormatTestDataLength = (int32_t)(sizeof(kNumberFormatTestData) / sizeof(kNumberFormatTestData[0]));

        // Keep this data here to avoid static initialization.
        FormatThreadTestData kPercentFormatTestData[] = 
        {
            FormatThreadTestData((double)5.0, UnicodeString("500%", "")),
            FormatThreadTestData( 1.0, UnicodeString("100%", "")),
            FormatThreadTestData( 0.26, UnicodeString("26%", "")),
            FormatThreadTestData( 16384.99, CharsToUnicodeString("1\\u00a0638\\u00a0499%") ), // U+00a0 = NBSP
            FormatThreadTestData( 81890.23, CharsToUnicodeString("8\\u00a0189\\u00a0023%" )),
        };
        int32_t kPercentFormatTestDataLength = (int32_t)(sizeof(kPercentFormatTestData) / sizeof(kPercentFormatTestData[0]));
        int32_t iteration;

        UErrorCode status = U_ZERO_ERROR;
        NumberFormat *formatter = NumberFormat::createInstance(Locale::getEnglish(),status);

        if(U_FAILURE(status))
        {
            Mutex m(&ftMutex);
            error("Error on NumberFormat::createInstance()");
            return;
        }

        NumberFormat *percentFormatter = NumberFormat::createPercentInstance(Locale::getFrench(),status);

        if(U_FAILURE(status))
        {
            {
                Mutex m(&ftMutex);
                error("Error on NumberFormat::createPercentInstance()");
            }
            delete formatter;
            return;
        }

        for(iteration = 0;!getError() && iteration<kFormatThreadIterations;iteration++)
        {

            int32_t whichLine = (iteration + fOffset)%kNumberFormatTestDataLength;

            UnicodeString  output;

            formatter->format(kNumberFormatTestData[whichLine].number, output);

            if(0 != output.compare(kNumberFormatTestData[whichLine].string))
            {
                Mutex m(&ftMutex);
                error("format().. expected " + kNumberFormatTestData[whichLine].string + " got " + output);
                continue; // will break
            }

            // Now check percent.
            output.remove();
            whichLine = (iteration + fOffset)%kPercentFormatTestDataLength;

            percentFormatter->format(kPercentFormatTestData[whichLine].number, output);

            if(0 != output.compare(kPercentFormatTestData[whichLine].string))
            {
                Mutex m(&ftMutex);
                error("percent format().. \n" + showDifference(kPercentFormatTestData[whichLine].string,output));
                continue;
            }

            // Test message error 
#define kNumberOfMessageTests 3
            UErrorCode      statusToCheck;
            UnicodeString   patternToCheck;
            Locale          messageLocale;
            Locale          countryToCheck;
            double          currencyToCheck;

            UnicodeString   expected;

            // load the cases.
            switch((iteration+fOffset) % kNumberOfMessageTests)
            {
            default:
            case 0:
                statusToCheck=                      U_FILE_ACCESS_ERROR;
                patternToCheck=                     "0:Someone from {2} is receiving a #{0} error - {1}. Their telephone call is costing {3,number,currency}."; // number,currency
                messageLocale=                      Locale("en","US");
                countryToCheck=                     Locale("","HR");
                currencyToCheck=                    8192.77;
                expected=                           "0:Someone from Croatia is receiving a #4 error - U_FILE_ACCESS_ERROR. Their telephone call is costing $8,192.77.";
                break;
            case 1:
                statusToCheck=                      U_INDEX_OUTOFBOUNDS_ERROR;
                patternToCheck=                     "1:A customer in {2} is receiving a #{0} error - {1}. Their telephone call is costing {3,number,currency}."; // number,currency
                messageLocale=                      Locale("de","DE_PREEURO");
                countryToCheck=                     Locale("","BF");
                currencyToCheck=                    2.32;
                expected=                           "1:A customer in Burkina Faso is receiving a #8 error - U_INDEX_OUTOFBOUNDS_ERROR. Their telephone call is costing $2.32.";
            case 2:
                statusToCheck=                      U_MEMORY_ALLOCATION_ERROR;
                patternToCheck=                     "2:user in {2} is receiving a #{0} error - {1}. They insist they just spent {3,number,currency} on memory."; // number,currency
                messageLocale=                      Locale("de","AT_PREEURO"); // Austrian German
                countryToCheck=                     Locale("","US"); // hmm
                currencyToCheck=                    40193.12;
                expected=                           CharsToUnicodeString("2:user in Vereinigte Staaten is receiving a #7 error - U_MEMORY_ALLOCATION_ERROR. They insist they just spent \\u00f6S 40.193,12 on memory.");
                break;
            }

            UnicodeString result;
            UErrorCode status = U_ZERO_ERROR;
            formatErrorMessage(status,patternToCheck,messageLocale,statusToCheck,countryToCheck,currencyToCheck,result);
            if(U_FAILURE(status))
            {
               UnicodeString tmp;
               errorToString(status,tmp);
               Mutex m(&ftMutex);
               error("Failure on message format, pattern=" + patternToCheck +", error = " + tmp);
               continue;
            }

            if(result != expected)
            {
                Mutex m(&ftMutex);
                error("PatternFormat: \n" + showDifference(expected,result));
                continue;
            }
        }

        delete formatter;
        delete percentFormatter;
        Mutex m(&ftMutex);
        done();
    }

private:
    int32_t fOffset; // where we are testing from.
};

// ** The actual test function.

void MultithreadTest::TestThreadedIntl()
{
    umtx_init(&ftMutex);

    FormatThreadTest tests[kFormatThreadThreads];
 
    logln(UnicodeString("Spawning: ") + kFormatThreadThreads + " threads * " + kFormatThreadIterations + " iterations each.");
    for(int32_t j = 0; j < kFormatThreadThreads; j++) {
        int32_t threadStatus = tests[j].start();
        if (threadStatus != 0) {
            errln("System Error %d starting thread number %d.", threadStatus, j);
            return;
        }
    }

    int32_t patience;
    for(patience = kFormatThreadPatience;patience > 0; patience --)
    {
        logln("Waiting...");

        int32_t i;
        int32_t terrs = 0;
        int32_t completed =0;

        for(i=0;i<kFormatThreadThreads;i++) {
            umtx_lock(&ftMutex);
            UBool threadIsDone = tests[i].getDone();
            umtx_unlock(&ftMutex);
            if(threadIsDone)
            {
                completed++;
                
                logln(UnicodeString("Test #") + i + " is complete.. ");
                
                UnicodeString theErr;
                if(tests[i].getError(theErr))
                {
                    terrs++;
                    errln(UnicodeString("#") + i + ": " + theErr);
                }
                // print out the error, too, if any.
            }
        }
        
        if(completed == kFormatThreadThreads)
        {
            logln("Done!");

            if(terrs)
            {
                errln("There were errors.");
            }

            break;
        }

        SimpleThread::sleep(900);
    }

    if (patience <= 0) {
        errln("patience exceeded. ");
    }
    umtx_destroy(&ftMutex);
    return;

}

#endif /* #if !UCONFIG_NO_FORMATTING */

#if !UCONFIG_NO_COLLATION

#define kCollatorThreadThreads   10  // # of threads to spawn
#define kCollatorThreadPatience kCollatorThreadThreads*100

struct Line {
  UChar buff[25];
  int32_t buflen;
} ;

class CollatorThreadTest : public ThreadWithStatus
{
private: 
  const UCollator *coll;
  const Line *lines;
  int32_t noLines;
public:
  CollatorThreadTest()  : ThreadWithStatus(),
  coll(NULL),
  lines(NULL),
  noLines(0)
 {
  };
  void setCollator(UCollator *c, Line *l, int32_t nl) 
  {
    coll = c;
    lines = l;
    noLines = nl;
  }
  virtual void run() {
    //sleep(10000);
  int32_t line = 0;

  uint8_t sk1[1024], sk2[1024];
  uint8_t *oldSk = NULL, *newSk = sk1;
  int32_t resLen = 0, oldLen = 0;
  int32_t i = 0;

  for(i = 0; i < noLines; i++) {
    resLen = ucol_getSortKey(coll, lines[i].buff, lines[i].buflen, newSk, 1024);

    int32_t res = 0, cmpres = 0, cmpres2 = 0;

    if(oldSk != NULL) {
      res = strcmp((char *)oldSk, (char *)newSk);
      cmpres = ucol_strcoll(coll, lines[i-1].buff, lines[i-1].buflen, lines[i].buff, lines[i].buflen);
      cmpres2 = ucol_strcoll(coll, lines[i].buff, lines[i].buflen, lines[i-1].buff, lines[i-1].buflen);
      //cmpres = res;
      //cmpres2 = -cmpres;

      if(cmpres != -cmpres2) {
        error("Compare result not symmetrical on line "+ line);
      }

      if(((res&0x80000000) != (cmpres&0x80000000)) || (res == 0 && cmpres != 0) || (res != 0 && cmpres == 0)) {
        error(UnicodeString("Difference between ucol_strcoll and sortkey compare on line ")+ UnicodeString(line));
      }

      if(res > 0) {
        error(UnicodeString("Line %i is not greater or equal than previous line ")+ UnicodeString(i));
        break;
      } else if(res == 0) { /* equal */
        res = u_strcmpCodePointOrder(lines[i-1].buff, lines[i].buff);
        if (res == 0) {
          error(UnicodeString("Probable error in test file on line %i (comparing identical strings)")+ UnicodeString(i));
          break;
        } else if (res > 0) {
          error(UnicodeString("Sortkeys are identical, but code point comapare gives >0 on line ")+ UnicodeString(i));
        }
      }
    }

    oldSk = newSk;
    oldLen = resLen;

    newSk = (newSk == sk1)?sk2:sk1;
  }

    Mutex m;
    done();
  }
};

void MultithreadTest::TestCollators()
{

  UErrorCode status = U_ZERO_ERROR;
  FILE *testFile = NULL;
  char testDataPath[1024];
  uprv_strcpy(testDataPath, IntlTest::loadTestData(status));
  char* index = 0;
  if (U_FAILURE(status)) {
      errln("ERROR: could not open test data %s", u_errorName(status));
	  return;
  }
  index=strrchr(testDataPath,(char)U_FILE_SEP_CHAR);

  if((unsigned int)(index-testDataPath) != (strlen(testDataPath)-1)){
          *(index+1)=0;
  }
  uprv_strcat(testDataPath,".."U_FILE_SEP_STRING);
  uprv_strcat(testDataPath, "CollationTest_");

  const char* type = "NON_IGNORABLE";

  const char *ext = ".txt";
  if(testFile) {
    fclose(testFile);
  }
  char buffer[1024];
  uprv_strcpy(buffer, testDataPath);
  uprv_strcat(buffer, type);
  int32_t bufLen = uprv_strlen(buffer);

  // we try to open 3 files:
  // path/CollationTest_type.txt
  // path/CollationTest_type_SHORT.txt
  // path/CollationTest_type_STUB.txt
  // we are going to test with the first one that we manage to open.

  uprv_strcpy(buffer+bufLen, ext);

  testFile = fopen(buffer, "rb");

  if(testFile == 0) {
    uprv_strcpy(buffer+bufLen, "_SHORT");
    uprv_strcat(buffer, ext);
    testFile = fopen(buffer, "rb");

    if(testFile == 0) {
      uprv_strcpy(buffer+bufLen, "_STUB");
      uprv_strcat(buffer, ext);
      testFile = fopen(buffer, "rb");

      if (testFile == 0) {
        *(buffer+bufLen) = 0;
        errln("ERROR: could not open any of the conformance test files, tried opening base %s", buffer);
        return;        
      } else {
        infoln(
          "INFO: Working with the stub file.\n"
          "If you need the full conformance test, please\n"
          "download the appropriate data files from:\n"
          "http://oss.software.ibm.com/cvs/icu4j/unicodetools/com/ibm/text/data/");
      }
    }
  }

  Line *lines = new Line[200000];
  uprv_memset(lines, 0, sizeof(Line)*200000);
  int32_t lineNum = 0;

  UChar bufferU[1024];
  int32_t buflen = 0;
  uint32_t first = 0;
  uint32_t offset = 0;

  while (fgets(buffer, 1024, testFile) != NULL) {
    offset = 0;
    if(*buffer == 0 || buffer[0] == '#') {
      continue;
    }
    offset = u_parseString(buffer, bufferU, 1024, &first, &status);
    buflen = offset;
    bufferU[offset++] = 0;
    lines[lineNum].buflen = buflen;
    //lines[lineNum].buff = new UChar[buflen+1];
    u_memcpy(lines[lineNum].buff, bufferU, buflen);
    lineNum++;
  }
  fclose(testFile);


  
  UCollator *coll = ucol_open("root", &status);
  if(U_FAILURE(status)) {
    errln("Couldn't open UCA collator");
    return;
  }
  ucol_setAttribute(coll, UCOL_NORMALIZATION_MODE, UCOL_ON, &status);
  ucol_setAttribute(coll, UCOL_CASE_FIRST, UCOL_OFF, &status);
  ucol_setAttribute(coll, UCOL_CASE_LEVEL, UCOL_OFF, &status);
  ucol_setAttribute(coll, UCOL_STRENGTH, UCOL_TERTIARY, &status);
  ucol_setAttribute(coll, UCOL_ALTERNATE_HANDLING, UCOL_NON_IGNORABLE, &status);

  int32_t noSpawned = 0;
  int32_t spawnResult = 0;
    CollatorThreadTest *tests;
    tests = new CollatorThreadTest[kCollatorThreadThreads];
 
    logln(UnicodeString("Spawning: ") + kCollatorThreadThreads + " threads * " + kFormatThreadIterations + " iterations each.");
    int32_t j = 0;
    for(j = 0; j < kCollatorThreadThreads; j++) {
      //logln("Setting collator %i", j);
      tests[j].setCollator(coll, lines, lineNum);
    }
    for(j = 0; j < kCollatorThreadThreads; j++) {
      log("%i ", j);
      spawnResult = tests[j].start();
      if(spawnResult != 0) {
	infoln("THREAD INFO: Couldn't spawn more than %i threads", noSpawned);
	break;
      }
      noSpawned++;
    }
    logln("Spawned all");

    //for(int32_t patience = kCollatorThreadPatience;patience > 0; patience --)
    for(;;)
    {
        logln("Waiting...");

        int32_t i;
        int32_t terrs = 0;
        int32_t completed =0;

        for(i=0;i<kCollatorThreadThreads;i++)
        {
            umtx_lock(NULL);
            UBool threadIsDone = tests[i].getDone();
            umtx_unlock(NULL);
            if(threadIsDone)
            {
                completed++;

                //logln(UnicodeString("Test #") + i + " is complete.. ");

                UnicodeString theErr;
                if(tests[i].getError(theErr))
                {
                    terrs++;
                    errln(UnicodeString("#") + i + ": " + theErr);
                }
                // print out the error, too, if any.
            }
        }
	logln("Completed %i tests", completed);

        if(completed == noSpawned)
        {
            logln("Done! All %i tests are finished", noSpawned);

            if(terrs)
            {
                errln("There were errors.");
            }
            ucol_close(coll);
            delete[] tests;
            //for(i = 0; i < lineNum; i++) {
              //delete[] lines[i].buff;
            //}
            delete[] lines;

            return;
        }

        SimpleThread::sleep(900);
    }
    errln("patience exceeded. ");
            ucol_close(coll);
}

#endif /* #if !UCONFIG_NO_COLLATION */

#endif // ICU_USE_THREADS

