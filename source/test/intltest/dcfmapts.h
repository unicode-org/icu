/********************************************************************
 * COPYRIGHT:
 * Copyright (C) 2016 and later: Unicode, Inc. and others.
 * License & terms of use: http://www.unicode.org/copyright.html
 ********************************************************************/


#ifndef _INTLTESTDECIMALFORMATAPI
#define _INTLTESTDECIMALFORMATAPI

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/unistr.h"
#include "intltest.h"


class IntlTestDecimalFormatAPI: public IntlTest {
    void runIndexedTest( int32_t index, UBool exec, const char* &name, char* par = NULL );

public:
    /**
     * Tests basic functionality of various API functions for DecimalFormat
     **/
    void testAPI(/*char *par*/);
    void testRounding(/*char *par*/);
    void testRoundingInc(/*char *par*/);
    void TestCurrencyPluralInfo();
    void TestScale();
    void TestFixedDecimal();
    void TestBadFastpath();
    void TestRequiredDecimalPoint();
private:
    /*Helper functions */
    void verify(const UnicodeString& message, const UnicodeString& got, double expected);
    void verifyString(const UnicodeString& message, const UnicodeString& got, UnicodeString& expected);
};

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif
