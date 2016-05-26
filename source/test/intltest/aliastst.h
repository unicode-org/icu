/********************************************************************
 * COPYRIGHT: 
 * Copyright (C) 2016 and later: Unicode, Inc. and others.
 * License & terms of use: http://www.unicode.org/copyright.html
 ********************************************************************/
#ifndef _ALIASTST
#define _ALIASTST

#include "intltest.h"
#include "unicode/locid.h"
#include "unicode/ures.h"

class LocaleAliasTest: public IntlTest {
public:
    void TestCalendar();
    void TestDateFormat();
    void TestCollation();
    void TestULocale();
    void TestUResourceBundle();
    void TestDisplayName(); 
    void runIndexedTest( int32_t index, UBool exec, const char* &name, char* par = NULL );
    LocaleAliasTest();
    virtual ~LocaleAliasTest();
private:
    UResourceBundle* resIndex;
    UBool isLocaleAvailable(const char*);
    Locale defLocale; 
};

#endif
