/*
*****************************************************************************************
*                                                                                       *
* COPYRIGHT:                                                                            *
*   (C) Copyright Taligent, Inc.,  1997                                                 *
*   (C) Copyright International Business Machines Corporation,  1997-1998               *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.                  *
*   US Government Users Restricted Rights - Use, duplication, or disclosure             *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                              *
*                                                                                       *
*****************************************************************************************
*/

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



