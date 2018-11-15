// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-2001, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

/**
 * MajorTestLevel is the top level test class for everything in the directory "IntlWork".
 */

#ifndef _INTLTESTFORMAT
#define _INTLTESTFORMAT

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/formattedvalue.h"
#include "intltest.h"


class IntlTestFormat: public IntlTest {
    void runIndexedTest( int32_t index, UBool exec, const char* &name, char* par = NULL );
};


class IntlTestWithFieldPosition : public IntlTest {
public:
    // TODO: When needed, add overload with a different category for each position
    void checkFormattedValue(
        const char16_t* message,
        const FormattedValue& fv,
        UnicodeString expectedString,
        UFieldCategory expectedCategory,
        const UFieldPosition* expectedFieldPositions,
        int32_t length);
};


#endif /* #if !UCONFIG_NO_FORMATTING */

#endif
