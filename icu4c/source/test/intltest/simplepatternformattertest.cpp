/*
*******************************************************************************
* Copyright (C) 2014, International Business Machines Corporation and         *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* File SIMPLEPATTERNFORMATTERTEST.CPP
*
********************************************************************************
*/
#include "cstring.h"
#include "intltest.h"
#include "simplepatternformatter.h"

#define LENGTHOF(array) (int32_t)(sizeof(array) / sizeof((array)[0]))

class SimplePatternFormatterTest : public IntlTest {
public:
    SimplePatternFormatterTest() {
    }
    void TestNoPlaceholders();
    void TestOnePlaceholder();
    void TestManyPlaceholders();
    void runIndexedTest(int32_t index, UBool exec, const char *&name, char *par=0);
private:
};

void SimplePatternFormatterTest::runIndexedTest(int32_t index, UBool exec, const char* &name, char* /*par*/) {
  TESTCASE_AUTO_BEGIN;
  TESTCASE_AUTO(TestNoPlaceholders);
  TESTCASE_AUTO(TestOnePlaceholder);
  TESTCASE_AUTO(TestManyPlaceholders);
  TESTCASE_AUTO_END;
}

void SimplePatternFormatterTest::TestNoPlaceholders() {
    UErrorCode status = U_ZERO_ERROR;
    SimplePatternFormatter fmt("This doesn''t have templates '{0}");
    assertEquals("PlaceholderCount", 0, fmt.getPlaceholderCount());
    UnicodeString appendTo;
    assertEquals(
            "Evaluate",
            "This doesn't have templates {0}", 
            fmt.format(
                    "unused",
                    appendTo,
                    status));
    appendTo.remove();
    fmt.compile("This has {} bad {012d placeholders", status);
    assertEquals("PlaceholderCount", 0, fmt.getPlaceholderCount());
    assertEquals(
            "Evaluate",
            "This has {} bad {012d placeholders", 
            fmt.format(
                    "unused",
                    appendTo,
                    status));
    appendTo.remove();
    assertSuccess("Status", status);
}

void SimplePatternFormatterTest::TestOnePlaceholder() {
    UErrorCode status = U_ZERO_ERROR;
    SimplePatternFormatter fmt;
    fmt.compile("{0} meter", status);
    assertEquals("PlaceholderCount", 1, fmt.getPlaceholderCount());
    UnicodeString appendTo;
    assertEquals(
            "Evaluate",
            "1 meter",
            fmt.format(
                    "1",
                    appendTo,
                    status));
    appendTo.remove();
    assertSuccess("Status", status);

    // assignment
    SimplePatternFormatter s;
    s = fmt;
    assertEquals(
            "Assignment",
            "1 meter",
            s.format(
                    "1",
                    appendTo,
                    status));
    appendTo.remove();

    // Copy constructor
    SimplePatternFormatter r(fmt);
    assertEquals(
            "Copy constructor",
            "1 meter",
            r.format(
                    "1",
                    appendTo,
                    status));
    appendTo.remove();
    assertSuccess("Status", status);
}

void SimplePatternFormatterTest::TestManyPlaceholders() {
    UErrorCode status = U_ZERO_ERROR;
    SimplePatternFormatter fmt;
    fmt.compile(
            "Templates {2}{1}{5} and {4} are out of order.", status);
    assertEquals("PlaceholderCount", 6, fmt.getPlaceholderCount());
    UnicodeString values[] = {
            "freddy", "tommy", "frog", "billy", "leg", "{0}"};
    UnicodeString *params[] = {
           &values[0], &values[1], &values[2], &values[3], &values[4], &values[5]}; 
    int32_t offsets[6];
    int32_t expectedOffsets[6] = {-1, 14, 10, -1, 27, 19};
    UnicodeString appendTo;
    assertEquals(
            "Evaluate",
            "Templates frogtommy{0} and leg are out of order.",
            fmt.format(
                    params,
                    LENGTHOF(params),
                    appendTo,
                    offsets,
                    LENGTHOF(offsets),
                    status));
    appendTo.remove();
    assertSuccess("Status", status);
    for (int32_t i = 0; i < LENGTHOF(expectedOffsets); ++i) {
        if (expectedOffsets[i] != offsets[i]) {
            errln("Expected %d, got %d", expectedOffsets[i], offsets[i]);
        }
    }
    fmt.format(
            params,
            LENGTHOF(params) - 1,
            appendTo,
            offsets,
            LENGTHOF(offsets),
            status);
    if (status != U_ILLEGAL_ARGUMENT_ERROR) {
        errln("Expected U_ILLEGAL_ARGUMENT_ERROR");
    }
    status = U_ZERO_ERROR;
    offsets[LENGTHOF(offsets) - 1] = 289;
    fmt.format(
            params,
            LENGTHOF(params),
            appendTo,
            offsets,
            LENGTHOF(offsets) - 1,
            status);
    appendTo.remove();
    assertEquals("Offsets buffer length", 289, offsets[LENGTHOF(offsets) - 1]);

    // Test assignment
    SimplePatternFormatter s;
    s = fmt;
    assertEquals(
            "Assignment",
            "Templates frogtommy{0} and leg are out of order.",
            s.format(
                    params,
                    LENGTHOF(params),
                    appendTo,
                    NULL,
                    0,
                    status));
    appendTo.remove();

    // Copy constructor
    SimplePatternFormatter r(fmt);
    assertEquals(
            "Copy constructor",
            "Templates frogtommy{0} and leg are out of order.",
            r.format(
                    params,
                    LENGTHOF(params),
                    appendTo,
                    NULL,
                    0,
                    status));
    appendTo.remove();
    r.compile("{0} meter", status);
    assertEquals("PlaceholderCount", 1, r.getPlaceholderCount());
    assertEquals(
            "Replace with new compile",
            "freddy meter",
            r.format(
                    "freddy",
                    appendTo,
                    status));
    appendTo.remove();
    assertSuccess("Status", status);
}

extern IntlTest *createSimplePatternFormatterTest() {
    return new SimplePatternFormatterTest();
}
