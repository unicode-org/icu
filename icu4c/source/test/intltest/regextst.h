/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 2002, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/


#ifndef REGEXTST_H
#define REGEXTST_H


#include "intltest.h"
#include "unicode/regex.h"


class RegexTest: public IntlTest {
public:
  
    RegexTest();
    virtual ~RegexTest();

    virtual void runIndexedTest(int32_t index, UBool exec, const char* &name, char* par = NULL );

    virtual void TestRegexAPI();
    virtual UBool doRegexLMTest(char *pat, char *text, UBool looking, UBool match, int line);

};
#endif
