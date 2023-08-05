// © 2023 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include <memory>

#include "cmemory.h"
#include "cstring.h"
#include "uniquecharstr.h"
#include "uniquecharstrtst.h"

UniqueCharStringsTest::UniqueCharStringsTest()
{
}

UniqueCharStringsTest::~UniqueCharStringsTest()
{
}

void UniqueCharStringsTest::runIndexedTest( int32_t index, UBool exec, const char* &name, char* /*par*/ )
{
    TESTCASE_AUTO_BEGIN;
    TESTCASE_AUTO(TestBasic);
    TESTCASE_AUTO_END;
}

void UniqueCharStringsTest::TestBasic() {
    UErrorCode status = U_ZERO_ERROR;
    UniqueCharStrings strings(status);
    if (U_FAILURE(status)) {
       errln("failed to construct UniqueCharStrings - %s", u_errorName(status));
    }
    UnicodeString key1("abcde", -1, US_INV);
    UnicodeString key2(u"fgihjk", 2); // the key should be only "fg" since the
                                      // length is 2.
    UnicodeString key3("lmn", -1, US_INV);
    int32_t index1 = strings.add(key1, status);
    if (U_FAILURE(status)) {
       errln("Cannot add key1 - %s", u_errorName(status));
    }
    int32_t index2 = strings.add(key2, status);
    if (U_FAILURE(status)) {
       errln("Cannot add key2 - %s", u_errorName(status));
    }
    strings.add(key3, status);
    if (U_FAILURE(status)) {
       errln("Cannot add key3 - %s", u_errorName(status));
    }
    strings.freeze();
    if (strcmp(strings.get(index1), "abcde") != 0) {
      errln("strcmp(strings.get(index1) should get \"abcde\" but get %s", strings.get(index1));
    }
    if (strcmp(strings.get(index2), "fg") != 0) {
      errln("strcmp(strings.get(index2) should get \"fg\" but get %s", strings.get(index2));
    }
}
