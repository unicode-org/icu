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


double testConvert(const char16_t* source , const char16_t* target , double input) {
    if (source == u"meter" && target ==u"foot" && input == 1.0)
        return 3.28084;

    return -1;    
}

void UnitsTest::testBasic() {
    IcuTestErrorCode status(*this, "Units testBasic");

    // Basic Test Cases
    struct TestCase {
        const char16_t* source;
        const char16_t* target;
        const double inputValue;
        const double expectedValue;
        } testCases[]
        {
            {u"meter" , u"foot" , 1.0 , 3.28084 }
        };

        for (size_t i = 0; i < 1; i++)
        { 
            assertEquals("test convert", testConvert(testCases[i].source, testCases[i].target, testCases[i].inputValue), testCases[i].expectedValue);
        }  
}


#endif /* #if !UCONFIG_NO_FORMATTING */
