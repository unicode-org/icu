/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-2003, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/


#ifndef MULTITHREADTEST_H
#define MULTITHREADTEST_H

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
    int32_t start(void); // start the thread
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
    virtual ~MultithreadTest();
    
    void runIndexedTest( int32_t index, UBool exec, const char* &name, char* par = NULL );

    /**
     * test that threads even work
     **/
    void TestThreads(void);
    /**
     * test that mutexes work 
     **/
    void TestMutex(void);
#if !UCONFIG_NO_FORMATTING
    /**
     * test that intl functions work in a multithreaded context
     **/
    void TestThreadedIntl(void);
#endif
  void TestCollators(void);

};

#endif

