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

#include "tsmutex.h"
#include <stdio.h>
#include <string.h>

#ifdef SOLARIS
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

void MutexTest::runIndexedTest( int32_t index, bool_t exec, 
                char* &name, char* par ) {
  if (exec) logln("TestSuite MutexTest: ");
  switch (index) {
  case 0: name = "TestMutex"; if (exec) TestMutex(); break;
    
  default: name = ""; break; //needed to end loop
  }
}

void MutexTest::TestMutex() {
}


void MutexTest::TestLock() {
  }




