/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 2002, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/


#ifndef REGEXTST_H
#define REGEXTST_H

#include "unicode/utypes.h"
#if !UCONFIG_NO_REGULAR_EXPRESSIONS

#include "intltest.h"
#include "unicode/regex.h"


class RegexTest: public IntlTest {
public:
  
    RegexTest();
    virtual ~RegexTest();

    virtual void runIndexedTest(int32_t index, UBool exec, const char* &name, char* par = NULL );

    // The following are test functions that are visible from the intltest test framework.
    virtual void API_Match();
    virtual void API_Pattern();
    virtual void API_Replace();
    virtual void Basic();
    virtual void Extended();
    virtual void Errors();

    // The following functions are internal to the regexp tests.
    virtual UBool doRegexLMTest(const char *pat, const char *text, UBool looking, UBool match, int line);
    virtual void regex_find(const char *pat, const char *input, UErrorCode expectedStatus, int line);
    virtual void regex_err(const char *pat, int32_t errline, int32_t errcol,
                            UErrorCode expectedStatus, int line);
};

#endif   // !UCONFIG_NO_REGULAR_EXPRESSIONS
#endif
