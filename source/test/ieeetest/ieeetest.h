/*
*******************************************************************************
*                                                                             *
* COPYRIGHT:                                                                  *
*   (C) Copyright International Business Machines Corporation,  1998          *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.        *
*   US Government Users Restricted Rights - Use, duplication, or disclosure   *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                    *
*                                                                             *
*******************************************************************************
*
* File ieeetest.h
*
* Modification History:
*
*   Date        Name        Description
*   08/21/98    stephen	    Creation.
*******************************************************************************
*/

#ifndef _IEEETEST
#define _IEEETEST

int main(int argc, char **argv);
void usage(const char *execName);

// Very simple class for testing IEEE compliance
class IEEETest 
{
 public:
  
  // additive constants for flags
  enum EModeFlags {
    kNone		= 0x00,
    kVerboseMode	= 0x01
  };
  
  
  IEEETest(int flags = kNone);
  ~IEEETest();
  
  // method returns the number of errors
  int			run();
  
 private:
  // utility function for running a test function
  int			runTest(const char *testName, 
				int (IEEETest::*testFunc)(void));
  
  // the actual tests; methods return the number of errors
  int			testNaN();
  int			testPositiveInfinity();
  int			testNegativeInfinity();
  int			testZero();
  
  // subtests of testNaN
  int			testIsNaN();
  int			NaNGT();
  int			NaNLT();
  int			NaNGTE();
  int			NaNLTE();
  int			NaNE();
  int			NaNNE();
  
  
  // logging utilities
  int			getTestLevel() 		const;
  void			increaseTestLevel();
  void			decreaseTestLevel();
  
  IEEETest&		log(char c);
  IEEETest&		log(const char *s);
  IEEETest&		log(int i);
  IEEETest&		log(long l);
  IEEETest&		log(double d);
  IEEETest&		logln();
  
  IEEETest&		err(char c);
  IEEETest&		err(const char *s);
  IEEETest&		err(int i);
  IEEETest&		err(long l);
  IEEETest&		err(double d);
  IEEETest&		errln();
  
  // data members
  int			mFlags;			// flags - only verbose for now
  int			mTestLevel;		// indent level
  
  short			mNeedLogIndent;
  short			mNeedErrIndent;
};

inline int
IEEETest::getTestLevel() const
{ return mTestLevel; }

inline void
IEEETest::increaseTestLevel()
{ mTestLevel++; }

inline void
IEEETest::decreaseTestLevel()
{ mTestLevel--; }

#endif

//eof
