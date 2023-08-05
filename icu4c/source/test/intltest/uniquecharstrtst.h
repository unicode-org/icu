// © 2023 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "intltest.h"
#include "uniquecharstr.h"


/**
 * Tests for the UniqueCharStrings class
 **/
class UniqueCharStringsTest: public IntlTest {
 public:
     UniqueCharStringsTest();
    virtual ~ UniqueCharStringsTest();

    void runIndexedTest( int32_t index, UBool exec, const char* &name, char* par = nullptr ) override;

    void TestBasic();
};
