/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-2001, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/


#ifndef MUTEXTEST_H
#define MUTEXTEST_H

#include "intltest.h"
#include "mutex.h"

/**
 * Tests Mutex and MutexImplementation functionality using
 * a custom MutexImplementation test class, simulating the behaviour
 * of an actual mutex
 **/
class MutexTest: public IntlTest {
public:
    MutexTest();
    virtual ~MutexTest();
    
    void runIndexedTest( int32_t index, UBool exec, const char* &name, char* par = NULL );

    /**
     * test the Mutex functionality and API using subroutine TestLock
     **/
    void TestMutex(void);
    /**
     * subroutine for TestMutex
     **/
    void TestLock(void);

    /* Was the global mutex initialized. */
    static UBool gMutexInitialized;

private:
};

#endif

