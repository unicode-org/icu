#ifndef _TESTUNISET
#define _TESTUNISET

#include "utypes.h"
#include "intltest.h"

class UnicodeSet;
class UnicodeString;

/**
 * UnicodeSet test
 */
class UnicodeSetTest: public IntlTest {

    void runIndexedTest(int32_t index, bool_t exec, char* &name, char* par=NULL);
    
private:

    void Test1();

    void expect(const UnicodeSet& set, const UnicodeString& expectedPairs);
    static UnicodeString escape(const UnicodeString& s);
};

#endif
