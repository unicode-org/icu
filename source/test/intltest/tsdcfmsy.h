/********************************************************************
 * COPYRIGHT: 
 * Copyright (C) 2016 and later: Unicode, Inc. and others.
 * License & terms of use: http://www.unicode.org/copyright.html
 ********************************************************************/

#ifndef _INTLTESTDECIMALFORMATSYMBOLS
#define _INTLTESTDECIMALFORMATSYMBOLS

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/unistr.h"
#include "unicode/dcfmtsym.h"
#include "intltest.h"
/**
 * Tests for DecimalFormatSymbols
 **/
class IntlTestDecimalFormatSymbols: public IntlTest {
    void runIndexedTest( int32_t index, UBool exec, const char* &name, char* par = NULL );  

private:
    /**
     * Test the API of DecimalFormatSymbols; primarily a simple get/set set.
     */
    void testSymbols(/*char *par*/);
    void testLastResortData();

     /** helper functions**/
    void Verify(double value, const UnicodeString& pattern,
                const DecimalFormatSymbols &sym, const UnicodeString& expected);
};

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif
