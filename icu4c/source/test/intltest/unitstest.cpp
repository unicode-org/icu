// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include <vector>

#include "intltest.h"


class UnitsTest : public IntlTest {
public:
    UnitsTest() {}

    void runIndexedTest(int32_t index, UBool exec, const char *&name, char *par=NULL);

    void testBasic();
    void testMass();
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
    TESTCASE_AUTO(testMass);
    TESTCASE_AUTO_END;
}

// Just for testing quick conversion ability.
double testConvert(const char16_t* source , const char16_t* target , double input) {
    if (source == u"meter" && target ==u"foot" && input == 1.0)
        return 3.28084;

    if (source == u"kilometer" && target ==u"foot" && input == 1.0)
        return 328.084;

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
            {u"meter" , u"foot" , 1.0 , 3.28084 },
            {u"kilometer" , u"foot" , 1.0 , 328.084 }
        };

        for (const auto& testCase : testCases) 
        {
            assertEquals("test convert", testConvert(testCase.source, testCase.target, testCase.inputValue), testCase.expectedValue);
        }
}

void UnitsTest::testMass() {
    IcuTestErrorCode status(*this, "Units testMass");
    ;
    // Test Cases
    struct TestCase 
    {
        const char16_t* source;
        const char16_t* target;
        const double inputValue;
        const double expectedValue;
    } testCases[] 
        {
            {u"gram", u"kilogram" , 1.0 , 0.001 },
            {u"pound", u"kilogram" , 1.0 , 0.453592 },
            {u"pound", u"kilogram" , 2.0 , 0.907185 },
            {u"ounce", u"pound" , 16.0 , 1.0 },
            {u"ounce", u"kilogram" , 16.0 , 0.453592 },
            {u"ton" , u"pound" , 1.0 , 2000 },
            {u"stone" , u"pound" , 1.0 , 14 },
            {u"stone" , u"kilogram" , 1.0 , 6.35029 }
        };

        for (const auto& testCase : testCases) 
        {
            assertEquals("test convert", testConvert(testCase.source, testCase.target, testCase.inputValue), testCase.expectedValue);
        }
}


#endif /* #if !UCONFIG_NO_FORMATTING */
