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
* File ieeetest.cpp
*
* Modification History:
*
*   Date        Name        Description
*   08/21/98    stephen	    Creation.
*******************************************************************************
*/

#include <iostream.h>		// cout, cerr
#include <float.h>		// DBL_MAX

#include "ieeetest.h"
#include "unicode/utypes.h"

//==============================

int
main(int argc, 
     char **argv)
{
  int flags = IEEETest::kNone;

  // parse command line switches
  for(int i = 1; i < argc; ++i) {
    if(argv[i][0] == '-') {
      switch(argv[i][1]) {
      case 'v':
	flags += IEEETest::kVerboseMode;
	break;
				
      case '?':
      case 'h':
      case 'H':
	usage(argv[0]);
	return 0;
	break;

      default:
	break;
      }
    }
  }

  IEEETest test(flags);	

  return test.run();
}

//==============================

void
usage(const char *execName)
{
  cout << endl;
  cout << "usage: " <<  execName << " [flags]" << endl << endl;
  cout << "Flags:" << endl;
  cout << " -v  Verbose mode" << endl;
}

//==============================

IEEETest::IEEETest(int flags)
  : mFlags(flags), 
    mTestLevel(0), 
    mNeedLogIndent(TRUE), 
    mNeedErrIndent(TRUE)
{}

//==============================

IEEETest::~IEEETest()
{}

//==============================

int
IEEETest::run(void)
{
  int errCount = 0;

  logln();
  log("Starting IEEETest").logln();
  increaseTestLevel();

  // add more tests here
  errCount += runTest("NaN behavior", &IEEETest::testNaN);
  errCount += runTest("+Infinity behavior", &IEEETest::testPositiveInfinity);
  errCount += runTest("-Infinity behavior", &IEEETest::testNegativeInfinity);
  errCount += runTest("Zero behavior", &IEEETest::testZero);
	
  decreaseTestLevel();
  if(errCount == 0)
    log("IEEETest Passed");
  else {
    log("IEEETest failed with ").log(errCount)
      .log(errCount == 1 ? " error." : " errors.");
  }
  logln();

  if(errCount == 0 && ! (mFlags & kVerboseMode))
    cout << endl << "All tests passed without error." << endl;

  return errCount;
}

//==============================
int
IEEETest::runTest(const char *testName, 
		  int (IEEETest::*testFunc)(void))
{
  logln().log("Running test ").log(testName).logln();
  increaseTestLevel();
  int errCount = (this->*testFunc)();
  decreaseTestLevel();
  log("Test ").log(testName);
  if(errCount == 0)
    log(" passed.");
  else {
    log(" failed with ").log(errCount)
      .log(errCount == 1 ? " error." : " errors.");
  }

  logln().logln();

  return errCount;
}


//==============================

// NaN is weird- comparisons with NaN _always_ return false, with the
// exception of !=, which _always_ returns true
int
IEEETest::testNaN(void)
{
  int errCount = 0;

  log("NaN tests may show that the expected NaN!=NaN etc. is not true on some").logln();
  log("platforms; however, ICU does not rely on them because it defines").logln();
  log("and uses uprv_isNaN(). Therefore, most failing NaN tests only report warnings.").logln();

  errCount += runTest("isNaN", &IEEETest::testIsNaN);
  errCount += runTest("NaN >", &IEEETest::NaNGT);
  errCount += runTest("NaN <", &IEEETest::NaNLT);
  errCount += runTest("NaN >=", &IEEETest::NaNGTE);
  errCount += runTest("NaN <=", &IEEETest::NaNLTE);
  errCount += runTest("NaN ==", &IEEETest::NaNE);
  errCount += runTest("NaN !=", &IEEETest::NaNNE);

  log("End of NaN tests.").logln();

  return errCount;
}

//==============================

int			
IEEETest::testPositiveInfinity(void)
{
  int errCount = 0;
  double	pinf		= uprv_getInfinity();
  double	ninf		= -uprv_getInfinity();
  double	ten		= 10.0;

  if(uprv_isInfinite(pinf) != TRUE) {
    err("FAIL: isInfinite(+Infinity) returned FALSE, should be TRUE.").errln();
    errCount++;
  }

  if(uprv_isPositiveInfinity(pinf) != TRUE) {
    err("FAIL: isPositiveInfinity(+Infinity) returned FALSE, should be TRUE.").errln();
    errCount++;
  }

  if(uprv_isNegativeInfinity(pinf) != FALSE) {
    err("FAIL: isNegativeInfinity(+Infinity) returned TRUE, should be FALSE.").errln();
    errCount++;
  }

  if(pinf > DBL_MAX != TRUE) {
    err("FAIL: +Infinity > DBL_MAX returned FALSE, should be TRUE.").errln();
    errCount++;
  }

  if(pinf > DBL_MIN != TRUE) {
    err("FAIL: +Infinity > DBL_MIN returned FALSE, should be TRUE.").errln();
    errCount++;
  }

  if(pinf > ninf != TRUE) {
    err("FAIL: +Infinity > -Infinity returned FALSE, should be TRUE.").errln();
    errCount++;
  }

  if(pinf > ten != TRUE) {
    err("FAIL: +Infinity > 10.0 returned FALSE, should be TRUE.").errln();
    errCount++;
  }

  return errCount;
}

//==============================

int			
IEEETest::testNegativeInfinity(void)
{
  int errCount = 0;
  double	pinf		= uprv_getInfinity();
  double	ninf		= -uprv_getInfinity();
  double	ten			= 10.0;

  if(uprv_isInfinite(ninf) != TRUE) {
    err("FAIL: isInfinite(-Infinity) returned FALSE, should be TRUE.").errln();
    errCount++;
  }

  if(uprv_isNegativeInfinity(ninf) != TRUE) {
    err("FAIL: isNegativeInfinity(-Infinity) returned FALSE, should be TRUE.").errln();
    errCount++;
  }

  if(uprv_isPositiveInfinity(ninf) != FALSE) {
    err("FAIL: isPositiveInfinity(-Infinity) returned TRUE, should be FALSE.").errln();
    errCount++;
  }

  if(ninf < DBL_MAX != TRUE) {
    err("FAIL: -Infinity < DBL_MAX returned FALSE, should be TRUE.").errln();
    errCount++;
  }

  if(ninf < DBL_MIN != TRUE) {
    err("FAIL: -Infinity < DBL_MIN returned FALSE, should be TRUE.").errln();
    errCount++;
  }

  if(ninf < pinf != TRUE) {
    err("FAIL: -Infinity < +Infinity returned FALSE, should be TRUE.").errln();
    errCount++;
  }

  if(ninf < ten != TRUE) {
    err("FAIL: -Infinity < 10.0 returned FALSE, should be TRUE.").errln();
    errCount++;
  }

  return errCount;
}

//==============================

// notes about zero:
// -0.0 == 0.0 == TRUE
// -0.0 <  0.0 == FALSE
// generating -0.0 must be done at runtime.  compiler apparently ignores sign?
int			
IEEETest::testZero(void)
{
  int errCount = 0;
  double	ten			= 10.0;
  double	pzero		= 0.0;
  double	nzero		= 0.0;

  nzero *= -1;

  if(pzero == nzero != TRUE) {
    err("FAIL: 0.0 == -0.0 returned FALSE, should be TRUE.").errln();
    errCount++;
  }

  if(pzero > nzero != FALSE) {
    err("FAIL: 0.0 > -0.0 returned TRUE, should be FALSE.").errln();
    errCount++;
  }

  if(pzero >= nzero != TRUE) {
    err("FAIL: 0.0 >= -0.0 returned FALSE, should be TRUE.").errln();
    errCount++;
  }

  if(pzero < nzero != FALSE) {
    err("FAIL: 0.0 < -0.0 returned TRUE, should be FALSE.").errln();
    errCount++;
  }

  if(pzero <= nzero != TRUE) {
    err("FAIL: 0.0 <= -0.0 returned FALSE, should be TRUE.").errln();
    errCount++;
  }

  if(uprv_isInfinite(1/pzero) != TRUE) {
    err("FAIL: isInfinite(1/0.0) returned FALSE, should be TRUE.").errln();
    errCount++;
  }

  if(uprv_isInfinite(1/nzero) != TRUE) {
    err("FAIL: isInfinite(1/-0.0) returned FALSE, should be TRUE.").errln();
    errCount++;
  }

  if(uprv_isPositiveInfinity(1/pzero) != TRUE) {
    err("FAIL: isPositiveInfinity(1/0.0) returned FALSE, should be TRUE.").errln();
    errCount++;
  }

  if(uprv_isNegativeInfinity(1/nzero) != TRUE) {
    err("FAIL: isNegativeInfinity(1/-0.0) returned FALSE, should be TRUE.").errln();
    errCount++;
  }

  return errCount;
}

//==============================

int
IEEETest::testIsNaN(void)
{
  int numErrors	= 0;
  double	pinf		= uprv_getInfinity();
  double	ninf		= -uprv_getInfinity();
  double	nan			= uprv_getNaN();
  double	ten			= 10.0;

  if(uprv_isNaN(nan) == FALSE) {
    err("FAIL: isNaN() returned FALSE for NaN.").errln();
    numErrors++;
  }

  if(uprv_isNaN(pinf) == TRUE) {
    err("FAIL: isNaN() returned TRUE for +Infinity.").errln();
    numErrors++;
  }

  if(uprv_isNaN(ninf) == TRUE) {
    err("FAIL: isNaN() returned TRUE for -Infinity.").errln();
    numErrors++;
  }

  if(uprv_isNaN(ten) == TRUE) {
    err("FAIL: isNaN() returned TRUE for 10.0.").errln();
    numErrors++;
  }

  return numErrors;
}

//==============================

int
IEEETest::NaNGT(void)
{
  double	pinf		= uprv_getInfinity();
  double	ninf		= -uprv_getInfinity();
  double	nan			= uprv_getNaN();
  double	ten			= 10.0;
  int numErrors	= 0;

  if(nan > nan != FALSE) {
    log("WARNING: NaN > NaN returned TRUE, should be FALSE").logln();
  }

  if(nan > pinf != FALSE) {
    log("WARNING: NaN > +Infinity returned TRUE, should be FALSE").logln();
  }

  if(nan > ninf != FALSE) {
    log("WARNING: NaN > -Infinity returned TRUE, should be FALSE").logln();
  }

  if(nan > ten != FALSE) {
    log("WARNING: NaN > 10.0 returned TRUE, should be FALSE").logln();
  }

  return numErrors;
}

//==============================

int				
IEEETest::NaNLT(void)
{
  double	pinf		= uprv_getInfinity();
  double	ninf		= -uprv_getInfinity();
  double	nan			= uprv_getNaN();
  double	ten			= 10.0;
  int numErrors	= 0;

  if(nan < nan != FALSE) {
    log("WARNING: NaN < NaN returned TRUE, should be FALSE").logln();
  }

  if(nan < pinf != FALSE) {
    log("WARNING: NaN < +Infinity returned TRUE, should be FALSE").logln();
  }

  if(nan < ninf != FALSE) {
    log("WARNING: NaN < -Infinity returned TRUE, should be FALSE").logln();
  }

  if(nan < ten != FALSE) {
    log("WARNING: NaN < 10.0 returned TRUE, should be FALSE").logln();
  }

  return numErrors;
}

//==============================

int				
IEEETest::NaNGTE(void)
{
  double	pinf		= uprv_getInfinity();
  double	ninf		= -uprv_getInfinity();
  double	nan			= uprv_getNaN();
  double	ten			= 10.0;
  int numErrors	= 0;

  if(nan >= nan != FALSE) {
    log("WARNING: NaN >= NaN returned TRUE, should be FALSE").logln();
  }

  if(nan >= pinf != FALSE) {
    log("WARNING: NaN >= +Infinity returned TRUE, should be FALSE").logln();
  }

  if(nan >= ninf != FALSE) {
    log("WARNING: NaN >= -Infinity returned TRUE, should be FALSE").logln();
  }

  if(nan >= ten != FALSE) {
    log("WARNING: NaN >= 10.0 returned TRUE, should be FALSE").logln();
  }

  return numErrors;
}

//==============================

int				
IEEETest::NaNLTE(void)
{
  double	pinf		= uprv_getInfinity();
  double	ninf		= -uprv_getInfinity();
  double	nan			= uprv_getNaN();
  double	ten			= 10.0;
  int numErrors	= 0;

  if(nan <= nan != FALSE) {
    log("WARNING: NaN <= NaN returned TRUE, should be FALSE").logln();
  }

  if(nan <= pinf != FALSE) {
    log("WARNING: NaN <= +Infinity returned TRUE, should be FALSE").logln();
  }

  if(nan <= ninf != FALSE) {
    log("WARNING: NaN <= -Infinity returned TRUE, should be FALSE").logln();
  }

  if(nan <= ten != FALSE) {
    log("WARNING: NaN <= 10.0 returned TRUE, should be FALSE").logln();
  }

  return numErrors;
}

//==============================

int				
IEEETest::NaNE(void)
{
  double	pinf		= uprv_getInfinity();
  double	ninf		= -uprv_getInfinity();
  double	nan			= uprv_getNaN();
  double	ten			= 10.0;
  int numErrors	= 0;

  if(nan == nan != FALSE) {
    log("WARNING: NaN == NaN returned TRUE, should be FALSE").logln();
  }

  if(nan == pinf != FALSE) {
    log("WARNING: NaN == +Infinity returned TRUE, should be FALSE").logln();
  }

  if(nan == ninf != FALSE) {
    log("WARNING: NaN == -Infinity returned TRUE, should be FALSE").logln();
  }

  if(nan == ten != FALSE) {
    log("WARNING: NaN == 10.0 returned TRUE, should be FALSE").logln();
  }

  return numErrors;
}

//==============================

int				
IEEETest::NaNNE(void)
{
  double	pinf		= uprv_getInfinity();
  double	ninf		= -uprv_getInfinity();
  double	nan			= uprv_getNaN();
  double	ten			= 10.0;
  int numErrors	= 0;

  if(nan != nan != TRUE) {
    log("WARNING: NaN != NaN returned FALSE, should be TRUE").logln();
  }

  if(nan != pinf != TRUE) {
    log("WARNING: NaN != +Infinity returned FALSE, should be TRUE").logln();
  }

  if(nan != ninf != TRUE) {
    log("WARNING: NaN != -Infinity returned FALSE, should be TRUE").logln();
  }

  if(nan != ten != TRUE) {
    log("WARNING: NaN != 10.0 returned FALSE, should be TRUE").logln();
  }

  return numErrors;
}

//==============================

IEEETest& 
IEEETest::log(char c)
{
  if(mFlags & kVerboseMode) {
    if(mNeedLogIndent) {
      for(int j = 0; j < 2 * getTestLevel(); ++j)
	cout << ' ';
      mNeedLogIndent = FALSE;
    }
    cout << c;
  }
  return *this;
}

//==============================

IEEETest& 
IEEETest::log(const char *s)
{
  if(mFlags & kVerboseMode) {
    if(mNeedLogIndent) {
      for(int j = 0; j < 2 * getTestLevel(); ++j)
	cout << ' ';
      mNeedLogIndent = FALSE;
    }
    cout << s;
  }
  return *this;
}

//==============================

IEEETest& 
IEEETest::log(int i)
{
  if(mFlags & kVerboseMode) {
    if(mNeedLogIndent) {
      for(int j = 0; j < 2 * getTestLevel(); ++j)
	cout << ' ';
      mNeedLogIndent = FALSE;
    }
    cout << i;
  }
  return *this;
}

//==============================

IEEETest& 
IEEETest::log(long l)
{
  if(mFlags & kVerboseMode) {
    if(mNeedLogIndent) {
      for(int j = 0; j < 2 * getTestLevel(); ++j)
	cout << ' ';
      mNeedLogIndent = FALSE;
    }
    cout << l;
  }
  return *this;
}

//==============================

IEEETest& 
IEEETest::log(double d)
{
  if(mFlags & kVerboseMode) {
    if(mNeedLogIndent) {
      for(int j = 0; j < 2 * getTestLevel(); ++j)
	cout << ' ';
      mNeedLogIndent = FALSE;
    }
    cout << d;
  }
  return *this;
}

//==============================

IEEETest& 
IEEETest::logln(void)
{
  if(mFlags & kVerboseMode)
    cout << endl;
  mNeedLogIndent = TRUE;
  return *this;
}

//==============================

IEEETest& 
IEEETest::err(char c)
{
  if(mNeedErrIndent) {
    for(int j = 0; j < 2 * getTestLevel(); ++j)
      cerr << ' ';
    mNeedErrIndent = FALSE;
  }
  cerr << c;
  return *this;
}

//==============================

IEEETest& 
IEEETest::err(const char *s)
{
  if(mNeedErrIndent) {
    for(int j = 0; j < 2 * getTestLevel(); ++j)
      cerr << ' ';
    mNeedErrIndent = FALSE;
  }
  cerr << s;
  return *this;
}

//==============================

IEEETest& 
IEEETest::err(int i)
{
  if(mNeedErrIndent) {
    for(int j = 0; j < 2 * getTestLevel(); ++j)
      cerr << ' ';
    mNeedErrIndent = FALSE;
  }
  cerr << i;
  return *this;
}

//==============================

IEEETest& 
IEEETest::err(long l)
{
  if(mNeedErrIndent) {
    for(int j = 0; j < 2 * getTestLevel(); ++j)
      cerr << ' ';
    mNeedErrIndent = FALSE;
  }
  cerr << l;
  return *this;
}

//==============================

IEEETest& 
IEEETest::err(double d)
{
  if(mNeedErrIndent) {
    for(int j = 0; j < 2 * getTestLevel(); ++j)
      cerr << ' ';
    mNeedErrIndent = FALSE;
  }
  cerr << d;
  return *this;
}

//==============================

IEEETest& 
IEEETest::errln(void)
{
  cerr << endl;
  mNeedErrIndent = TRUE;
  return *this;
}

//eof
