/*
*****************************************************************************************
*                                                                                       *
* COPYRIGHT:                                                                            *
*   (C) Copyright International Business Machines Corporation,  1998                    *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.                  *
*   US Government Users Restricted Rights - Use, duplication, or disclosure             *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                              *
*                                                                                       *
*****************************************************************************************
*/

#include "intltest.h"
#include "mutex.h"

/*
    Test multithreading.   Of course we need a thread class first..
    this wrapper has a ported implementation.
 */

class SimpleThread
{
public:
    SimpleThread();
    virtual ~SimpleThread();
    void start(void); // start the thread
public: // should be private, but then we couldn't be asocial.
    virtual void run(void) = 0; // Override this to provide some real implementation
private:
    void *fImplementation;

public:
    static void sleep(int32_t millis); // probably shouldn't go here but oh well. 
};


/**
 * Tests actual threading
 **/
class MultithreadTest : public IntlTest
{
public:
    MultithreadTest();
    ~MultithreadTest();
    
    void runIndexedTest( int32_t index, bool_t exec, char* &name, char* par = NULL );

    /**
     * test that threads even work
     **/
    void TestThreads(void);
    /**
     * test that mutexes work 
     **/
    void TestMutex(void);
    /**
     * test that intl functions work in a multithreaded context
     **/
    void TestThreadedIntl(void);
};



