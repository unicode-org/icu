/*
**********************************************************************
*   Copyright (C) 1999 Alan Liu and others. All rights reserved.
**********************************************************************
*   Date        Name        Description
*   10/20/99    alan        Creation.
**********************************************************************
*/

#ifndef _TESTUNISET
#define _TESTUNISET

#include "unicode/utypes.h"
#include "intltest.h"

class UnicodeSet;
class UnicodeString;

/**
 * UnicodeSet test
 */
class UnicodeSetTest: public IntlTest {

    void runIndexedTest(int32_t index, bool_t exec, char* &name, char* par=NULL);
    
private:

    void TestPatterns(void);
    void TestAddRemove(void);

    void expectPattern(UnicodeSet& set,
                       const UnicodeString& pattern,
                       const UnicodeString& expectedPairs);
    void expectPairs(const UnicodeSet& set,
                     const UnicodeString& expectedPairs);
    static UnicodeString escape(const UnicodeString& s);
};

#endif
