/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

#include <unicode/umachine.h>

#if !defined(WIN32) && !defined(XP_MAC)
#define POSIX 1
#endif

#if defined(POSIX)||defined(SOLARIS)||defined(AIX)||defined(HPUX)
#define HAVE_IMP

#if defined(__hpux) && defined(HPUX_CMA)
# ifndef _INCLUDE_POSIX_SOURCE
#  define _INCLUDE_POSIX_SOURCE
# endif
#endif

#include <pthread.h>

#if defined(__hpux) && defined(HPUX_CMA)
# if defined(read)  // read being defined as cma_read causes trouble with iostream::read
#  undef read
# endif
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

#include <unistd.h>

#endif
/* HPUX */
#ifdef sleep
#undef sleep
#endif

#include "unicode/utypes.h"

/* APP_NO_THREADS is an old symbol. We'll honour it if present. */
#ifdef APP_NO_THREADS
# define ICU_USE_THREADS 0
#endif

/* Default: use threads. */
#ifndef ICU_USE_THREADS
# define ICU_USE_THREADS 1
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
                char* &name, char* par ) {
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

#ifdef WIN32
#define HAVE_IMP

#include <windows.h>

struct Win32ThreadImplementation
{
    HANDLE fHandle;
    DWORD fThreadID;
};

extern "C" unsigned long _stdcall SimpleThreadProc(void *arg)
{
    ((SimpleThread*)arg)->run();
    return 0;
}

SimpleThread::SimpleThread()
:fImplementation(0)
{
    Win32ThreadImplementation *imp = new Win32ThreadImplementation;
    imp->fHandle = 0;
    imp->fThreadID = 0;

    fImplementation = imp;
}

SimpleThread::~SimpleThread()
{
    delete (Win32ThreadImplementation*)fImplementation;
}

void SimpleThread::start()
{
    Win32ThreadImplementation *imp = (Win32ThreadImplementation*)fImplementation;

    if(imp->fHandle != NULL)
        return;

    imp->fHandle = CreateThread(NULL,0,SimpleThreadProc,(void*)this,0,&imp->fThreadID);
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

void 
SimpleThread::start()
{}

void 
SimpleThread::run()
{}

void 
SimpleThread::sleep(int32_t millis)
{}
#endif


#if defined(POSIX)||defined(SOLARIS)||defined(AIX)||defined(HPUX)
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

void SimpleThread::start()
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

}

void SimpleThread::sleep(int32_t millis)
{
#ifdef SOLARIS
   sigignore(SIGALRM);
#endif

#ifdef HPUX_CMA
   cma_sleep(millis/100);
#elif defined(HPUX)
   useconds_t m = millis * 1000;
   if (m > 1000000) m = 1000000;
   usleep(m); 
#elif defined(OS390)
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
                char* &name, char* par ) {
  if (exec) logln("TestSuite MultithreadTest: ");
  switch (index) {
  case 0: name = "TestThreads"; if (exec) TestThreads(); break;
  case 1: name = "TestMutex"; if (exec) TestMutex(); break;
  case 2: name = "TestThreadedIntl"; if (exec) TestThreadedIntl(); break;
    
  default: name = ""; break; //needed to end loop
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
    virtual void run() { SimpleThread::sleep(1000); *fWhatToChange = '*'; }
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
        threads[i]->start();
        SimpleThread::sleep(200);
        logln(" Subthread started.");
    }

    logln("Waiting for threads to be set..");

    int32_t patience = 40; // seconds to wait

    while(patience--)
    {
        int32_t count = 0;
        for(i=0;i<THREADTEST_NRTHREADS;i++)
        {
            if(threadTestChars[i] == '*')
            {
                count++;
            }
        }
        
        if(count == THREADTEST_NRTHREADS)
        {
            logln("->" + UnicodeString(threadTestChars) + "<- Got all threads! cya");
            return;
        }

        logln("->" + UnicodeString(threadTestChars) + "<- Waiting..");
        SimpleThread::sleep(500);
    }

    errln("->" + UnicodeString(threadTestChars) + "<- PATIENCE EXCEEDED!! Still missing some.");
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
};

void MultithreadTest::TestMutex()
{
  /* this test uses printf so that we don't hang by calling UnicodeString inside of a mutex. */
  //logln("Bye.");
  //  printf("Warning: MultiThreadTest::Testmutex() disabled.\n");
  //  return; 
  
  if(verbose)
    printf("Before mutex.");
  {
    Mutex m;
    if(verbose)
      printf(" Exitted 2nd mutex");
  }
  if(verbose)
    printf("exitted 1st mutex. Now testing with threads:");
  
  TestMutexThread1  thread1;
  TestMutexThread2  thread2(thread1);
  thread2.start();
  thread1.start();
  
  for(int32_t patience = 12; patience > 0;patience--)
    {
      if(thread1.fDone && verbose)
    printf("Thread1 done");
      
      if(thread1.fDone && thread2.fDone)
        {
      char tmp[999];
      sprintf(tmp,"%lu",thread2.fElapsed);
      if(thread2.fErr)
        errln("Thread 2 says: thread1 didn't run before I aquired the mutex.");
      logln("took " + UnicodeString(tmp) + " seconds for thread2 to aquire the mutex.");
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

// ** FormatThreadTest - a thread that tests performing a number of numberformats.


#define kFormatThreadIterations 20  // # of iterations per thread
#define kFormatThreadThreads    10  // # of threads to spawn
#define kFormatThreadPatience   60  // time in seconds to wait for all threads
struct FormatThreadTestData
{
    double number;
    UnicodeString string;
    FormatThreadTestData(double a, const UnicodeString& b) : number(a),string(b) {}
} ;

// 
FormatThreadTestData kNumberFormatTestData[] = 
{
   FormatThreadTestData((double)5., UnicodeString("5")),
   FormatThreadTestData( 6., "6" ),
   FormatThreadTestData( 20., "20" ),
   FormatThreadTestData( 8., "8" ),
   FormatThreadTestData( 8.3, "8.3" ),
   FormatThreadTestData( 12345, "12,345" ),
   FormatThreadTestData( 81890.23, "81,890.23" ),
};
int32_t kNumberFormatTestDataLength = sizeof(kNumberFormatTestData) / sizeof(kNumberFormatTestData[0]);

// 
FormatThreadTestData kPercentFormatTestData[] = 
{
   FormatThreadTestData((double)5., UnicodeString("500%")),
   FormatThreadTestData( 1, "100%" ),
   FormatThreadTestData( 0.26, "26%" ),
   FormatThreadTestData( 16384.99, CharsToUnicodeString("1\\u00a0638\\u00a0499%") ), // U+00a0 = NBSP
   FormatThreadTestData( 81890.23, CharsToUnicodeString("8\\u00a0189\\u00a0023%" )),
};
int32_t kPercentFormatTestDataLength = sizeof(kPercentFormatTestData) / sizeof(kPercentFormatTestData[0]);


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


class FormatThreadTest : public ThreadWithStatus
{
public:
    FormatThreadTest() // constructor is NOT multithread safe.
        // the locale to use
    {
        static int32_t fgOffset = 0;
        fOffset = fgOffset += 3;
    }

    virtual void run()
    {
        int32_t iteration;

        UErrorCode status = U_ZERO_ERROR;
        NumberFormat *formatter = NumberFormat::createInstance(Locale::ENGLISH,status);

        if(U_FAILURE(status))
        {
            error("Error on NumberFormat::createInstance()");
            return;
        }

        NumberFormat *percentFormatter = NumberFormat::createPercentInstance(Locale::FRENCH,status);

        if(U_FAILURE(status))
        {
            error("Error on NumberFormat::createPercentInstance()");
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
                error("format().. expected " + kNumberFormatTestData[whichLine].string + " got " + output);
                continue; // will break
            }

            // Now check percent.
            output.remove();
            whichLine = (iteration + fOffset)%kPercentFormatTestDataLength;

            percentFormatter->format(kPercentFormatTestData[whichLine].number, output);

            if(0 != output.compare(kPercentFormatTestData[whichLine].string))
            {
                error("percent format().. \n" + showDifference(kPercentFormatTestData[whichLine].string,output));
                continue;
            }

            // Test message error 
#define kNumberOfMessageTests 3
            UErrorCode       statusToCheck;
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
                messageLocale=                      Locale("de","DE");
                countryToCheck=                     Locale("","BF");
                currencyToCheck=                    2.32;
                expected=                           "1:A customer in Burkina Faso is receiving a #8 error - U_INDEX_OUTOFBOUNDS_ERROR. Their telephone call is costing $2.32.";
            case 2:
                statusToCheck=                      U_MEMORY_ALLOCATION_ERROR;
                patternToCheck=                     "2:user in {2} is receiving a #{0} error - {1}. They insist they just spent {3,number,currency} on memory."; // number,currency
                messageLocale=                      Locale("de","AT"); // Austrian German
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
               error("Failure on message format, pattern=" + patternToCheck +", error = " + tmp);
               continue;
            }

            if(result != expected)
            {
                error("PatternFormat: \n" + showDifference(expected,result));
                continue;
            }
        }

        delete formatter;
        delete percentFormatter;
        done();
    }

private:
    int32_t fOffset; // where we are testing from.
};

// ** The actual test function.

void MultithreadTest::TestThreadedIntl()
{

    FormatThreadTest tests[kFormatThreadThreads];
 
    logln(UnicodeString("Spawning: ") + kFormatThreadThreads + " threads * " + kFormatThreadIterations + " iterations each.");
    for(int32_t j = 0; j < kFormatThreadThreads; j++)
        tests[j].start();

    for(int32_t patience = kFormatThreadPatience;patience > 0; patience --)
    {
        logln("Waiting...");

        int32_t i;
        int32_t terrs = 0;
        int32_t completed =0;

        for(i=0;i<kFormatThreadThreads;i++)
        {
            if(tests[i].getDone())
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

            return;
        }

        SimpleThread::sleep(900);
    }
    errln("patience exceeded. ");
}

#endif // ICU_USE_THREADS

