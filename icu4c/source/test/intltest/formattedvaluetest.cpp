// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include <set>

#include "unicode/formattedvalue.h"
#include "unicode/unum.h"
#include "intltest.h"
#include "itformat.h"


class FormattedValueTest : public IntlTest {
public:
    void runIndexedTest(int32_t index, UBool exec, const char *&name, char *par=0);
private:
    void testBasic();
    void testSetters();
    void testLocalPointer();

    void assertAllPartsEqual(
        UnicodeString messagePrefix,
        const ConstrainedFieldPosition& cfpos,
        UCFPosConstraintType constraint,
        UFieldCategory category,
        int32_t field,
        int32_t start,
        int32_t limit,
        int64_t context);
};

void FormattedValueTest::runIndexedTest(int32_t index, UBool exec, const char *&name, char *) {
    if (exec) {
        logln("TestSuite FormattedValueTest: ");
    }
    TESTCASE_AUTO_BEGIN;
    TESTCASE_AUTO(testBasic);
    TESTCASE_AUTO(testSetters);
    TESTCASE_AUTO(testLocalPointer);
    TESTCASE_AUTO_END;
}


void FormattedValueTest::testBasic() {
    IcuTestErrorCode status(*this, "testBasic");
    ConstrainedFieldPosition cfpos;
    assertAllPartsEqual(
        u"basic",
        cfpos,
        UCFPOS_CONSTRAINT_NONE,
        UFIELD_CATEGORY_UNDEFINED,
        0,
        0,
        0,
        0LL);
}

void FormattedValueTest::testSetters() {
    IcuTestErrorCode status(*this, "testSetters");
    ConstrainedFieldPosition cfpos;

    cfpos.constrainCategory(UFIELD_CATEGORY_DATE);
    assertAllPartsEqual(
        u"setters 0",
        cfpos,
        UCFPOS_CONSTRAINT_CATEGORY,
        UFIELD_CATEGORY_DATE,
        0,
        0,
        0,
        0LL);

    cfpos.constrainField(UFIELD_CATEGORY_NUMBER, UNUM_COMPACT_FIELD);
    assertAllPartsEqual(
        u"setters 1",
        cfpos,
        UCFPOS_CONSTRAINT_FIELD,
        UFIELD_CATEGORY_NUMBER,
        UNUM_COMPACT_FIELD,
        0,
        0,
        0LL);

    cfpos.setInt64IterationContext(42424242424242LL);
    assertAllPartsEqual(
        u"setters 2",
        cfpos,
        UCFPOS_CONSTRAINT_FIELD,
        UFIELD_CATEGORY_NUMBER,
        UNUM_COMPACT_FIELD,
        0,
        0,
        42424242424242LL);

    cfpos.setState(UFIELD_CATEGORY_NUMBER, UNUM_COMPACT_FIELD, 5, 10);
    assertAllPartsEqual(
        u"setters 3",
        cfpos,
        UCFPOS_CONSTRAINT_FIELD,
        UFIELD_CATEGORY_NUMBER,
        UNUM_COMPACT_FIELD,
        5,
        10,
        42424242424242LL);

    cfpos.reset();
    assertAllPartsEqual(
        u"setters 4",
        cfpos,
        UCFPOS_CONSTRAINT_NONE,
        UFIELD_CATEGORY_UNDEFINED,
        0,
        0,
        0,
        0LL);
}

void FormattedValueTest::testLocalPointer() {
    UErrorCode status = U_ZERO_ERROR;
    LocalUConstrainedFieldPositionPointer ucfpos(ucfpos_open(&status));
    assertSuccess("Openining LocalUConstrainedFieldPositionPointer", status);
    assertEquals(u"Test that object is valid",
        UCFPOS_CONSTRAINT_NONE,
        ucfpos_getConstraintType(ucfpos.getAlias(), &status));
    assertSuccess("Using LocalUConstrainedFieldPositionPointer", status);
}

void FormattedValueTest::assertAllPartsEqual(
        UnicodeString messagePrefix,
        const ConstrainedFieldPosition& cfpos,
        UCFPosConstraintType constraint,
        UFieldCategory category,
        int32_t field,
        int32_t start,
        int32_t limit,
        int64_t context) {
    assertEquals(messagePrefix + u": constraint",
        constraint, cfpos.getConstraintType());
    assertEquals(messagePrefix + u": category",
        category, cfpos.getCategory());
    assertEquals(messagePrefix + u": field",
        field, cfpos.getField());
    assertEquals(messagePrefix + u": start",
        start, cfpos.getStart());
    assertEquals(messagePrefix + u": limit",
        limit, cfpos.getLimit());
    assertEquals(messagePrefix + u": context",
        context, cfpos.getInt64IterationContext());
}


void IntlTestWithFieldPosition::checkFormattedValue(
        const char16_t* message,
        const FormattedValue& fv,
        UnicodeString expectedString,
        UFieldCategory expectedCategory,
        const UFieldPosition* expectedFieldPositions,
        int32_t length) {
    IcuTestErrorCode status(*this, "checkFormattedValue");
    UnicodeString baseMessage = UnicodeString(message) + u": " + fv.toString(status) + u": ";

    // Check string values
    assertEquals(baseMessage + u"string", expectedString, fv.toString(status));
    assertEquals(baseMessage + u"temp string", expectedString, fv.toTempString(status));

    // The temp string is guaranteed to be NUL-terminated
    UnicodeString readOnlyAlias = fv.toTempString(status);
    assertEquals(baseMessage + u"NUL-terminated",
        0, readOnlyAlias.getBuffer()[readOnlyAlias.length()]);

    // Check nextPosition over all fields
    ConstrainedFieldPosition cfpos;
    cfpos.constrainCategory(expectedCategory);
    for (int32_t i = 0; i < length; i++) {
        assertTrue(baseMessage + i, fv.nextPosition(cfpos, status));
        int32_t expectedField = expectedFieldPositions[i].field;
        int32_t expectedStart = expectedFieldPositions[i].beginIndex;
        int32_t expectedLimit = expectedFieldPositions[i].endIndex;
        assertEquals(baseMessage + u"category " + Int64ToUnicodeString(i),
            expectedCategory, cfpos.getCategory());
        assertEquals(baseMessage + u"field " + Int64ToUnicodeString(i),
            expectedField, cfpos.getField());
        assertEquals(baseMessage + u"start " + Int64ToUnicodeString(i),
            expectedStart, cfpos.getStart());
        assertEquals(baseMessage + u"limit " + Int64ToUnicodeString(i),
            expectedLimit, cfpos.getLimit());
    }
    assertFalse(baseMessage + u"after loop", fv.nextPosition(cfpos, status));

    // Check nextPosition constrained over each field one at a time
    std::set<int32_t> uniqueFields;
    for (int32_t i = 0; i < length; i++) {
        uniqueFields.insert(expectedFieldPositions[i].field);
    }
    for (int32_t field : uniqueFields) {
        cfpos.reset();
        cfpos.constrainField(expectedCategory, field);
        for (int32_t i = 0; i < length; i++) {
            if (expectedFieldPositions[i].field != field) {
                continue;
            }
            assertTrue(baseMessage + i, fv.nextPosition(cfpos, status));
            int32_t expectedField = expectedFieldPositions[i].field;
            int32_t expectedStart = expectedFieldPositions[i].beginIndex;
            int32_t expectedLimit = expectedFieldPositions[i].endIndex;
            assertEquals(baseMessage + u"category " + Int64ToUnicodeString(i),
                expectedCategory, cfpos.getCategory());
            assertEquals(baseMessage + u"field " + Int64ToUnicodeString(i),
                expectedField, cfpos.getField());
            assertEquals(baseMessage + u"start " + Int64ToUnicodeString(i),
                expectedStart, cfpos.getStart());
            assertEquals(baseMessage + u"limit " + Int64ToUnicodeString(i),
                expectedLimit, cfpos.getLimit());
        }
        assertFalse(baseMessage + u"after loop", fv.nextPosition(cfpos, status));
    }
}


extern IntlTest *createFormattedValueTest() {
    return new FormattedValueTest();
}

#endif /* !UCONFIG_NO_FORMATTING */
