
#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#include "unicode/utypes.h"
#include "umutex.h"
#include "unicode/unistr.h"

//------------------------------------------------------------------------------
//
//   class AbstractTest    Base class for threading tests.
//                         Use of this abstract base isolates the part of the
//                         program that nows how to spin up and control threads
//                         from the specific stuff being tested, and (hopefully)
//                         simplifies adding new threading tests for different parts
//                         of ICU.
//
//     Derived classes:    A running test will have exactly one instance of the
//                         derived class, which will persist for the duration of the
//                         test and be shared among all of the threads involved in
//                         the test.
//
//                         The constructor will be called in a single-threaded environment,
//                         and should set up any data that will need to persist for the
//                         duration.
//
//                         runOnce() will be called repeatedly by the working threads of
//                         the test in the full multi-threaded environment.
//
//                         check() will be called periodically in a single threaded
//                         environment, with the worker threads temporarily suspended between
//                         between calls to runOnce().  Do consistency checks here.
//
//------------------------------------------------------------------------------
class AbstractTest {
public:
                     AbstractTest() {};
    virtual         ~AbstractTest() {};
    virtual void     check()   = 0;
    virtual void     runOnce() = 0;
};



class StringTest: public AbstractTest {
public:
                    StringTest();
    virtual        ~StringTest();
    virtual void    check();
    virtual void    runOnce();

private:
    UnicodeString   *fCleanStrings;
    UnicodeString   *fSourceStrings;
};

StringTest::StringTest() {
    // cleanStrings and sourceStrings are separately initialized to the same values.
    // cleanStrings are never touched after in any remotely unsafe way.
    // sourceStrings are copied during the test, which will run their buffer's reference
    //    counts all over the place.
    fCleanStrings     = new UnicodeString[5];
    fSourceStrings    = new UnicodeString[5];

    fCleanStrings[0]  = "When sorrows come, they come not single spies, but in batallions.";
    fSourceStrings[0] = "When sorrows come, they come not single spies, but in batallions.";
    fCleanStrings[1]  = "Away, you scullion! You rampallion! You fustilarion! I'll tickle your catastrophe!";
    fSourceStrings[1] = "Away, you scullion! You rampallion! You fustilarion! I'll tickle your catastrophe!"; 
    fCleanStrings[2]  = "hot";
    fSourceStrings[2] = "hot"; 
    fCleanStrings[3]  = "";
    fSourceStrings[3] = ""; 
    fCleanStrings[4]  = "Tomorrow, and tomorrow, and tomorrow,\n"
                        "Creeps in this petty pace from day to day\n"
                        "To the last syllable of recorded time;\n"
                        "And all our yesterdays have lighted fools \n"
                        "The way to dusty death. Out, out brief candle!\n"
                        "Life's but a walking shadow, a poor player\n"
                        "That struts and frets his hour upon the stage\n"
                        "And then is heard no more. It is a tale\n"
                        "Told by and idiot, full of sound and fury,\n"
                        "Signifying nothing.\n";
    fSourceStrings[4] = "Tomorrow, and tomorrow, and tomorrow,\n"
                        "Creeps in this petty pace from day to day\n"
                        "To the last syllable of recorded time;\n"
                        "And all our yesterdays have lighted fools \n"
                        "The way to dusty death. Out, out brief candle!\n"
                        "Life's but a walking shadow, a poor player\n"
                        "That struts and frets his hour upon the stage\n"
                        "And then is heard no more. It is a tale\n"
                        "Told by and idiot, full of sound and fury,\n"
                        "Signifying nothing.\n";
};


StringTest::~StringTest() {
    delete [] fCleanStrings;
    delete [] fSourceStrings;
}


void   StringTest::runOnce() {
    UnicodeString firstGeneration[5];
    UnicodeString secondGeneration[5];
    UnicodeString thirdGeneration[5];
    UnicodeString fourthGeneration[5];

    int i;

    // Make four generations of copies of the source strings, in slightly variant ways.
    //
    for (i=0; i<5; i++) {
         firstGeneration[i]   = fSourceStrings[i];
         secondGeneration[i]  = firstGeneration[i];
         thirdGeneration[i]   = UnicodeString(secondGeneration[i]);
 //        fourthGeneration[i]  = UnicodeString("Lay on, MacDuff, And damn'd be him that first cries, \"Hold, enough!\"");
         fourthGeneration[i]  = UnicodeString();
         fourthGeneration[i]  = thirdGeneration[i];
    }

    // Verify that all four generations are equal.
    for (i=0; i<5; i++) {
        if (firstGeneration[i] !=  fSourceStrings[i]   ||
            firstGeneration[i] !=  secondGeneration[i] ||
            firstGeneration[i] !=  thirdGeneration[i]  ||
            firstGeneration[i] !=  fourthGeneration[i])
        {
            fprintf(stderr, "Error, strings don't compare equal.\n");
        }
    }


};
  

void   StringTest::check() {
};
  


//------------------------------------------------------------------------------
//
//   Windows specific code for starting threads
//
//------------------------------------------------------------------------------
#ifdef WIN32

#include "Windows.h"
#include "process.h"



typedef void (*ThreadFunc)(void *);

class ThreadFuncs           // This class isolates OS dependent threading
{                           //   functions from the rest of ThreadTest program.
public:
    static void            Sleep(int millis) {::Sleep(millis);};
    static void            startThread(ThreadFunc, void *param);
    static unsigned long   getCurrentMillis();
    static void            yield() {::Sleep(0);};
};

void ThreadFuncs::startThread(ThreadFunc func, void *param)
{
    unsigned long x;
    x = _beginthread(func, 0x10000, param);
    if (x == -1)
    {
        fprintf(stderr, "Error starting thread.  Errno = %d\n", errno);
        exit(-1);
    }
}

unsigned long ThreadFuncs::getCurrentMillis()
{
    return (unsigned long)::GetTickCount();
}




// #elif defined (POSIX) 
#else

//------------------------------------------------------------------------------
//
//   UNIX specific code for starting threads
//
//------------------------------------------------------------------------------
#include <pthread.h>
#include <unistd.h>
#include <errno.h>
#include <sched.h>
#include <sys/timeb.h>


extern "C" {


typedef void (*ThreadFunc)(void *);
typedef void *(*pthreadfunc)(void *);

class ThreadFuncs           // This class isolates OS dependent threading
{                           //   functions from the rest of ThreadTest program.
public:
    static void            Sleep(int millis);
    static void            startThread(ThreadFunc, void *param);
    static unsigned long   getCurrentMillis();
    static void            yield() {sched_yield();};
};

void ThreadFuncs::Sleep(int millis)
{
   int seconds = millis/1000;
   if (seconds <= 0) seconds = 1;
   ::sleep(seconds);
}


void ThreadFuncs::startThread(ThreadFunc func, void *param)
{
    unsigned long x;

    pthread_t tId;
    //thread_t tId;
#if defined(_HP_UX) && defined(XML_USE_DCE)
    x = pthread_create( &tId, pthread_attr_default,  (pthreadfunc)func,  param);
#else
    pthread_attr_t attr;
    pthread_attr_init(&attr);
    x = pthread_create( &tId, &attr,  (pthreadfunc)func,  param);
#endif
    if (x == -1)
    {
        fprintf(stderr, "Error starting thread.  Errno = %d\n", errno);
        exit(-1);
    }
}

unsigned long ThreadFuncs::getCurrentMillis() {
    timeb aTime;
    ftime(&aTime);
    return (unsigned long)(aTime.time*1000 + aTime.millitm);
}
}


// #else
// #error This platform is not supported
#endif



//------------------------------------------------------------------------------
//
//  struct runInfo     Holds the info extracted from the command line and data
//                     that is shared by all threads.
//                     There is only one of these, and it is static.
//                     During the test, the threads will access this info without
//                     any synchronization.
//
//------------------------------------------------------------------------------
const int MAXINFILES = 25;
struct RunInfo
{
    bool           quiet;
    bool           verbose;
    int            numThreads;
    int            totalTime;
    AbstractTest   *fTest;
    bool           stopFlag;
    int32_t        runningThreads;
};


//------------------------------------------------------------------------------
//
//  struct threadInfo  Holds information specific to an individual thread.
//                     One of these is set up for each thread in the test.
//                     The main program monitors the threads by looking
//                     at the status stored in these structs.
//
//------------------------------------------------------------------------------
struct ThreadInfo
{
    bool    fHeartBeat;            // Set true by the thread each time it finishes
                                   //   parsing a file.
    unsigned int     fCycles;      // Number of cycles completed.
    int              fThreadNum;   // Identifying number for this thread.
    ThreadInfo() {
        fHeartBeat = false;
        fCycles = 0;
        fThreadNum = -1;
    }
};


//
//------------------------------------------------------------------------------
//
//  Global Data
//
//------------------------------------------------------------------------------
RunInfo         gRunInfo;
ThreadInfo      *gThreadInfo;
UMTX            *gMutex;


//----------------------------------------------------------------------
//
//   parseCommandLine   Read through the command line, and save all
//                      of the options in the gRunInfo struct.
//
//                      Display the usage message if the command line
//                      is no good.
//
//                      Probably ought to be a member function of RunInfo.
//
//----------------------------------------------------------------------

void parseCommandLine(int argc, char **argv)
{
    gRunInfo.quiet = false;               // Set up defaults for run.
    gRunInfo.verbose = false;
    gRunInfo.numThreads = 2;
    gRunInfo.totalTime = 0;

    try             // Use exceptions for command line syntax errors.
    {
        int argnum = 1;
        while (argnum < argc)
        {
            if      (strcmp(argv[argnum], "-quiet") == 0)
                gRunInfo.quiet = true;
            else if (strcmp(argv[argnum], "-verbose") == 0)
                gRunInfo.verbose = true;
            else if (strcmp(argv[argnum], "--help") == 0 ||
                    (strcmp(argv[argnum],     "?")      == 0)) {throw 1; }
                
            else if (strcmp(argv[argnum], "-threads") == 0)
            {
                ++argnum;
                if (argnum >= argc)
                    throw 1;
                gRunInfo.numThreads = atoi(argv[argnum]);
                if (gRunInfo.numThreads < 0)
                    throw 1;
            }
            else if (strcmp(argv[argnum], "-time") == 0)
            {
                ++argnum;
                if (argnum >= argc)
                    throw 1;
                gRunInfo.totalTime = atoi(argv[argnum]);
                if (gRunInfo.totalTime < 1)
                    throw 1;
            }
            else  
            {
                fprintf(stderr, "Unrecognized command line option.  Scanning \"%s\"\n",
                    argv[argnum]);
                throw 1;
            }
            argnum++;
        }

    }
    catch (int)
    {
        fprintf(stderr, "usage:  threadtest [-threads nnn] [-time nnn] [-quiet] [-verbose] \n"
            "     -v             Use validating parser.  Non-validating is default. \n"
            "     -quiet         Suppress periodic status display. \n"
            "     -verbose       Display extra messages. \n"
            "     -threads nnn   Number of threads.  Default is 2. \n"
            "     -time nnn      Total time to run, in seconds.  Default is forever.\n"
            );
        exit(1);
    }
}





//----------------------------------------------------------------------
//
//  threadMain   The main function for each of the swarm of test threads.
//               Run in an infinite loop, parsing each of the documents
//               given on the command line in turn.
//
//               There is no return from this fuction, and no graceful
//               thread termination.  Threads are stuck running here
//               until the OS shuts them down as a consequence of the
//               main thread of the process (which never calls this
//               function) exiting.
//
//----------------------------------------------------------------------

extern "C" {

void threadMain (void *param)
{
    ThreadInfo   *thInfo = (ThreadInfo *)param;

    if (gRunInfo.verbose)
        printf("Thread #%d: starting\n", thInfo->fThreadNum);


    //
    //
    while (true)
    {
        //
        //  If the main thread is asking us to wait, do so by locking gMutex
        //     which will block us, since the main thread will be holding it already.
        // 
        if (gRunInfo.stopFlag) {
            umtx_atomic_dec(&gRunInfo.runningThreads);
            while (gRunInfo.stopFlag) {
                umtx_lock(gMutex);
                umtx_unlock(gMutex);
            }
            umtx_atomic_inc(&gRunInfo.runningThreads);
        }

        if (gRunInfo.verbose )
            printf("Thread #%d: starting loop\n", thInfo->fThreadNum);

        //
        // The real work of the test happens here.
        //
        gRunInfo.fTest->runOnce();

        thInfo->fHeartBeat = true;
        thInfo->fCycles++;
    }
}

}




//----------------------------------------------------------------------
//
//   main
//
//----------------------------------------------------------------------

int main (int argc, char **argv)
{

    int             totalCyclesCompleted = 0;

    parseCommandLine(argc, argv);

    //
    // While we are still single threaded, inialize the test.
    //
    gRunInfo.fTest = new StringTest;

    //
    //  Fire off the requested number of parallel threads
    //

    if (gRunInfo.numThreads == 0)
        exit(0);

    gRunInfo.stopFlag = TRUE;      // Will cause the new threads to block 
    umtx_lock(gMutex);

    gThreadInfo = new ThreadInfo[gRunInfo.numThreads];
    int threadNum;
    for (threadNum=0; threadNum < gRunInfo.numThreads; threadNum++)
    {
        gThreadInfo[threadNum].fThreadNum = threadNum;
        ThreadFuncs::startThread(threadMain, &gThreadInfo[threadNum]);
    }

    //
    //  Loop, watching the heartbeat of the worker threads.
    //    Each second,
    //            Stop all the worker threads at the top of their loop, then check
    //                 the reference counts of the shared Strings.
    //            display "+" if all threads have completed at least one loop
    //            display "." if some thread hasn't since previous "+"
    //

    unsigned long startTime = ThreadFuncs::getCurrentMillis();
    int elapsedSeconds = 0;
    while (gRunInfo.totalTime == 0 || gRunInfo.totalTime > elapsedSeconds)
    {
        gRunInfo.stopFlag = FALSE;       // Unblocks the worker threads.
        umtx_unlock(gMutex);      

        ThreadFuncs::Sleep(1000);        // Worker threads do their work now...

        umtx_lock(gMutex);               // Block the worker threads at the top of their loop
        gRunInfo.stopFlag = TRUE;
        while (gRunInfo.runningThreads > 0) { ThreadFuncs::yield(); }
        
                
        if (gRunInfo.quiet == false && gRunInfo.verbose == false)
        {
            char c = '+';
            int threadNum;
            for (threadNum=0; threadNum < gRunInfo.numThreads; threadNum++)
            {
                if (gThreadInfo[threadNum].fHeartBeat == false)
                {
                    c = '.';
                    break;
                };
            }
            fputc(c, stdout);
            fflush(stdout);
            if (c == '+')
                for (threadNum=0; threadNum < gRunInfo.numThreads; threadNum++)
                    gThreadInfo[threadNum].fHeartBeat = false;
        }

        //  Call back to the test to let it check its internal validity
        //    while we've got all of the threads paused.
        gRunInfo.fTest->check();

        elapsedSeconds = (ThreadFuncs::getCurrentMillis() - startTime) / 1000;
    };

    //
    //  Time's up, we are done.  (We only get here if this was a timed run)
    //  Tally up the total number of cycles completed by each of the threads.
    //
    double totalParsesCompleted = 0;
    for (threadNum=0; threadNum < gRunInfo.numThreads; threadNum++)
    {
        totalCyclesCompleted += gThreadInfo[threadNum].fCycles;
        // printf("%f   ", totalParsesCompleted);
    }

    double cyclesPerMinute = totalCyclesCompleted / (double(gRunInfo.totalTime) / double(60));
    printf("\n%8.1f cycles per minute.", cyclesPerMinute);

    //  The threads are all still alive; we just return
    //   and leave it to the operating sytem to kill them.
    //

    return 0;
}


