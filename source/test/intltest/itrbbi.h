/*
**********************************************************************
* Copyright (C) 2016 and later: Unicode, Inc. and others. 
* License & terms of use: http://www.unicode.org/copyright.html
**********************************************************************
************************************************************************
*   Date        Name        Description
*   12/14/99    Madhu        Creation.
************************************************************************/
/**
 * IntlTestRBBI is the top level test class for the RuleBasedBreakIterator tests
 */

#ifndef INTLTESTRBBI_H
#define INTLTESTRBBI_H

#include "unicode/utypes.h"

#if !UCONFIG_NO_BREAK_ITERATION && !UCONFIG_NO_REGULAR_EXPRESSIONS

#include "intltest.h"


class IntlTestRBBI: public IntlTest {
public:
    void runIndexedTest( int32_t index, UBool exec, const char* &name, char* par = NULL );
};

#endif /* #if !UCONFIG_NO_BREAK_ITERATION && !UCONFIG_NO_REGULAR_EXPRESSIONS */

#endif
