/*
 *******************************************************************************
 * Copyright (C) 2016 and later: Unicode, Inc. and others.        
 * License & terms of use: http://www.unicode.org/copyright.html                                                
 *******************************************************************************
 */

#ifndef ITRBNFP_H
#define ITRBNFP_H

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "intltest.h"
#include "unicode/rbnf.h"


class IntlTestRBNFParse : public IntlTest {
 public:

  // IntlTest override
  virtual void runIndexedTest(int32_t index, UBool exec, const char* &name, char* par);

#if U_HAVE_RBNF
  /** 
   * Perform an API test
   */
  virtual void TestParse();

  void testfmt(RuleBasedNumberFormat* formatter, double val, UErrorCode& status);
  void testfmt(RuleBasedNumberFormat* formatter, int val, UErrorCode& status);

 protected:

/* U_HAVE_RBNF */
#else

  virtual void TestRBNFParseDisabled();

/* U_HAVE_RBNF */
#endif
};

#endif /* #if !UCONFIG_NO_FORMATTING */

// endif ITRBNFP_H
#endif
