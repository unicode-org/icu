/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
#ifndef _INTLTEST
#include "intltest.h"
#endif

#ifndef _MUTEX
#include "mutex.h"
#endif

/**
 * Tests Mutex and MutexImplementation functionality using
 * a custom MutexImplementation test class, simulating the behaviour
 * of an actual mutex
 **/
class MutexTest: public IntlTest {
public:
    MutexTest();
    ~MutexTest();
    
    void runIndexedTest( int32_t index, bool_t exec, char* &name, char* par = NULL );

    /**
     * test the Mutex functionality and API using subroutine TestLock
     **/
    void TestMutex(void);
    /**
     * subroutine for TestMutex
     **/
    void TestLock(void);

private:
};



