/*
*******************************************************************************
* Copyright (C) 2014, International Business Machines Corporation and         *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* File TEMPLATETEST.CPP
*
********************************************************************************
*/
#include "cstring.h"
#include "intltest.h"
#include "template.h"

#define LENGTHOF(array) (int32_t)(sizeof(array) / sizeof((array)[0]))

class TemplateTest : public IntlTest {
public:
    TemplateTest() {
    }
    void TestNoPlaceholders();
    void TestOnePlaceholder();
    void TestManyPlaceholders();
    void runIndexedTest(int32_t index, UBool exec, const char *&name, char *par=0);
private:
};

void TemplateTest::runIndexedTest(int32_t index, UBool exec, const char* &name, char* /*par*/) {
  TESTCASE_AUTO_BEGIN;
  TESTCASE_AUTO(TestNoPlaceholders);
  TESTCASE_AUTO(TestOnePlaceholder);
  TESTCASE_AUTO(TestManyPlaceholders);
  TESTCASE_AUTO_END;
}

void TemplateTest::TestNoPlaceholders() {
    UErrorCode status = U_ZERO_ERROR;
    Template t("This doesn''t have templates '{0}");
    assertEquals("PlaceholderCount", 0, t.getPlaceholderCount());
    UnicodeString appendTo;
    assertEquals(
            "Evaluate",
            "This doesn't have templates {0}", 
            t.evaluate(
                    NULL,
                    0,
                    appendTo,
                    status));
    appendTo.remove();
    t.compile("This has {} bad {012d placeholders", status);
    assertEquals("PlaceholderCount", 0, t.getPlaceholderCount());
    assertEquals(
            "Evaluate",
            "This has {} bad {012d placeholders", 
            t.evaluate(
                    NULL,
                    0,
                    appendTo,
                    status));
    appendTo.remove();
    assertSuccess("Status", status);
}

void TemplateTest::TestOnePlaceholder() {
    UErrorCode status = U_ZERO_ERROR;
    Template t;
    t.compile("{0} meter", status);
    assertEquals("PlaceholderCount", 1, t.getPlaceholderCount());
    UnicodeString one("1");
    UnicodeString *params[] = {&one};
    UnicodeString appendTo;
    assertEquals(
            "Evaluate",
            "1 meter",
            t.evaluate(
                    params,
                    LENGTHOF(params),
                    appendTo,
                    status));
    appendTo.remove();
    assertSuccess("Status", status);

    // assignment
    Template s;
    s = t;
    assertEquals(
            "Assignment",
            "1 meter",
            s.evaluate(
                    params,
                    LENGTHOF(params),
                    appendTo,
                    status));
    appendTo.remove();

    // Copy constructor
    Template r(t);
    assertEquals(
            "Copy constructor",
            "1 meter",
            r.evaluate(
                    params,
                    LENGTHOF(params),
                    appendTo,
                    status));
    appendTo.remove();
    assertSuccess("Status", status);
}

void TemplateTest::TestManyPlaceholders() {
    UErrorCode status = U_ZERO_ERROR;
    Template t;
    t.compile(
            "Templates {2}{1}{5} and {4} are out of order.", status);
    assertEquals("PlaceholderCount", 6, t.getPlaceholderCount());
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
            t.evaluate(
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
    t.evaluate(
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
    t.evaluate(
            params,
            LENGTHOF(params),
            appendTo,
            offsets,
            LENGTHOF(offsets) - 1,
            status);
    appendTo.remove();
    assertEquals("Offsets buffer length", 289, offsets[LENGTHOF(offsets) - 1]);

    // Test assignment
    Template s;
    s = t;
    assertEquals(
            "Assignment",
            "Templates frogtommy{0} and leg are out of order.",
            s.evaluate(
                    params,
                    LENGTHOF(params),
                    appendTo,
                    status));
    appendTo.remove();

    // Copy constructor
    Template r(t);
    assertEquals(
            "Assignment",
            "Templates frogtommy{0} and leg are out of order.",
            r.evaluate(
                    params,
                    LENGTHOF(params),
                    appendTo,
                    status));
    appendTo.remove();
    r.compile("{0} meter", status);
    assertEquals("PlaceholderCount", 1, r.getPlaceholderCount());
    assertEquals(
            "Assignment",
            "freddy meter",
            r.evaluate(
                    params,
                    1,
                    appendTo,
                    status));
    appendTo.remove();
    assertSuccess("Status", status);
}

extern IntlTest *createTemplateTest() {
    return new TemplateTest();
}
