/*
*******************************************************************************
*
*   Copyright (C) 1998-2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*
* File ieeetest.h
*
* Modification History:
*
*   Date        Name        Description
*   08/21/98    stephen     Creation.
*******************************************************************************
*/

#ifndef _IEEETEST
#define _IEEETEST

void usage(const char *execName);

// Very simple class for testing IEEE compliance
class IEEETest 
{
 public:
  
  // additive constants for flags
  enum EModeFlags {
    kNone           = 0x00,
    kVerboseMode    = 0x01
  };
  
  
  IEEETest(int flags = kNone);
  ~IEEETest();
  
  // method returns the number of errors
  int           run(void);
  
 private:
  // utility function for running a test function
  int           runTest(const char *testName, 
                        int (IEEETest::*testFunc)(void));
  
  // the actual tests; methods return the number of errors
  int           testNaN(void);
  int           testPositiveInfinity(void);
  int           testNegativeInfinity(void);
  int           testZero(void);
  
  // subtests of testNaN
  int           testIsNaN(void);
  int           NaNGT(void);
  int           NaNLT(void);
  int           NaNGTE(void);
  int           NaNLTE(void);
  int           NaNE(void);
  int           NaNNE(void);
  
  
  // logging utilities
  int           getTestLevel(void) const;
  void          increaseTestLevel(void);
  void          decreaseTestLevel(void);
  
  IEEETest&     log(char c);
  IEEETest&     log(const char *s);
  IEEETest&     log(int i);
  IEEETest&     log(long l);
  IEEETest&     log(double d);
  IEEETest&     logln(void);
  
  IEEETest&     err(char c);
  IEEETest&     err(const char *s);
  IEEETest&     err(int i);
  IEEETest&     err(long l);
  IEEETest&     err(double d);
  IEEETest&     errln(void);
  
  // data members
  int           mFlags;         // flags - only verbose for now
  int           mTestLevel;     // indent level
  
  short         mNeedLogIndent;
  short         mNeedErrIndent;
};

inline int
IEEETest::getTestLevel(void) const
{ return mTestLevel; }

inline void
IEEETest::increaseTestLevel(void)
{ mTestLevel++; }

inline void
IEEETest::decreaseTestLevel(void)
{ mTestLevel--; }

#endif

//eof
