// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "intltest.h"


class UnitsTest : public IntlTest {
public:
    UnitsTest() {}

    void runIndexedTest(int32_t index, UBool exec, const char *&name, char *par=NULL);

    void testBasic();
};

extern IntlTest *createUnitsTest() {
    return new UnitsTest();
}

void UnitsTest::runIndexedTest(int32_t index, UBool exec, const char *&name, char * /*par*/) {
    if(exec) {
        logln("TestSuite UnitsTest: ");
    }
    TESTCASE_AUTO_BEGIN;
    TESTCASE_AUTO(testBasic);
    TESTCASE_AUTO_END;
}

void UnitsTest::testBasic() {
    IcuTestErrorCode status(*this, "testBasic");

    assertEquals("message", 1, 2);
}


#endif // !UCONFIG_NO_FORMATTING
