/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-2001, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

#include "tsmutex.h"
#include <stdio.h>
#include <string.h>

#ifdef U_SOLARIS
#include <signal.h>
#endif

//////////////////////////
//
// Simple Mutex structure
//   Increments lockCount each time the mutex is locked.
//   Decrements lockCount each time the mutex is unlocked.
//Note: This test does not actually create locks, since
//that would be platform specific.  This test simply tests
//the functionality of the Mutex class.
//
//////////////////////////
#if 0
struct MyMutexImp {
  MyMutexImp();
  uint32_t lockCount;
};

MyMutexImp::MyMutexImp() {
  lockCount = 0;
}

static void _myLock(MutexPointer  p) {
  MyMutexImp* imp = (MyMutexImp*)p;
  (imp->lockCount)++; 
}

static void _myUnlock(MutexPointer  p) {
  MyMutexImp* imp = (MyMutexImp*)p;
  (imp->lockCount)--; 
}
#endif
//////////////////////////
//
// The Test Class
//
//////////////////////////
MutexTest::MutexTest() {
}

MutexTest::~MutexTest() {
}

void MutexTest::runIndexedTest( int32_t index, UBool exec, 
                const char* &name, char* /*par*/ ) {
  if (exec) logln("TestSuite MutexTest: ");
  switch (index) {
  case 0: name = "TestMutex"; if (exec) TestMutex(); break;
    
  default: name = ""; break; //needed to end loop
  }
}

UBool MutexTest::gMutexInitialized = FALSE;

void MutexTest::TestMutex() {
    /* This is tested in intltest.cpp before anything starts. */
    if (!gMutexInitialized) {
        logln("*** Failure! The global mutex was not initialized.\n"
                "*** Make sure the right linker was used.\n");
    }
}


void MutexTest::TestLock() {
}


