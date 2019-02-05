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
    LocalArray<UFieldPositionWithCategory> converted(new UFieldPositionWithCategory[length]);
    for (int32_t i=0; i<length; i++) {
        converted[i].category = expectedCategory;
        converted[i].field = expectedFieldPositions[i].field;
        converted[i].beginIndex = expectedFieldPositions[i].beginIndex;
        converted[i].endIndex = expectedFieldPositions[i].endIndex;
    }
    checkMixedFormattedValue(message, fv, expectedString, converted.getAlias(), length);
}


UnicodeString CFPosToUnicodeString(const ConstrainedFieldPosition& cfpos) {
    UnicodeString sb;
    sb.append(u"CFPos[");
    sb.append(Int64ToUnicodeString(cfpos.getStart()));
    sb.append(u'-');
    sb.append(Int64ToUnicodeString(cfpos.getLimit()));
    sb.append(u' ');
    sb.append(Int64ToUnicodeString(cfpos.getCategory()));
    sb.append(u':');
    sb.append(Int64ToUnicodeString(cfpos.getField()));
    sb.append(u']');
    return sb;
}


void IntlTestWithFieldPosition::checkMixedFormattedValue(
        const char16_t* message,
        const FormattedValue& fv,
        UnicodeString expectedString,
        const UFieldPositionWithCategory* expectedFieldPositions,
        int32_t length) {
    IcuTestErrorCode status(*this, "checkMixedFormattedValue");
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
    for (int32_t i = 0; i < length; i++) {
        assertTrue(baseMessage + u"A has next position @ " + Int64ToUnicodeString(i),
            fv.nextPosition(cfpos, status));
        int32_t expectedCategory = expectedFieldPositions[i].category;
        int32_t expectedField = expectedFieldPositions[i].field;
        int32_t expectedStart = expectedFieldPositions[i].beginIndex;
        int32_t expectedLimit = expectedFieldPositions[i].endIndex;
        assertEquals(baseMessage + u"A category @ " + Int64ToUnicodeString(i),
            expectedCategory, cfpos.getCategory());
        assertEquals(baseMessage + u"A field @ " + Int64ToUnicodeString(i),
            expectedField, cfpos.getField());
        assertEquals(baseMessage + u"A start @ " + Int64ToUnicodeString(i),
            expectedStart, cfpos.getStart());
        assertEquals(baseMessage + u"A limit @ " + Int64ToUnicodeString(i),
            expectedLimit, cfpos.getLimit());
    }
    UBool afterLoopResult = fv.nextPosition(cfpos, status);
    assertFalse(baseMessage + u"A after loop: " + CFPosToUnicodeString(cfpos), afterLoopResult);

    // Check nextPosition constrained over each category one at a time
    for (int32_t category=0; category<UFIELD_CATEGORY_COUNT; category++) {
        cfpos.reset();
        cfpos.constrainCategory(static_cast<UFieldCategory>(category));
        for (int32_t i = 0; i < length; i++) {
            if (expectedFieldPositions[i].category != category) {
                continue;
            }
            assertTrue(baseMessage + u"B has next position @ " + Int64ToUnicodeString(i),
                fv.nextPosition(cfpos, status));
            int32_t expectedCategory = expectedFieldPositions[i].category;
            int32_t expectedField = expectedFieldPositions[i].field;
            int32_t expectedStart = expectedFieldPositions[i].beginIndex;
            int32_t expectedLimit = expectedFieldPositions[i].endIndex;
            assertEquals(baseMessage + u"B category @ " + Int64ToUnicodeString(i),
                expectedCategory, cfpos.getCategory());
            assertEquals(baseMessage + u"B field @ " + Int64ToUnicodeString(i),
                expectedField, cfpos.getField());
            assertEquals(baseMessage + u"B start @ " + Int64ToUnicodeString(i),
                expectedStart, cfpos.getStart());
            assertEquals(baseMessage + u"B limit @ " + Int64ToUnicodeString(i),
                expectedLimit, cfpos.getLimit());
        }
        UBool afterLoopResult = fv.nextPosition(cfpos, status);
        assertFalse(baseMessage + u"B after loop @ " + CFPosToUnicodeString(cfpos), afterLoopResult);
    }

    // Check nextPosition constrained over each field one at a time
    std::set<std::pair<UFieldCategory, int32_t>> uniqueFields;
    for (int32_t i = 0; i < length; i++) {
        uniqueFields.insert({expectedFieldPositions[i].category, expectedFieldPositions[i].field});
    }
    for (std::pair<UFieldCategory, int32_t> categoryAndField : uniqueFields) {
        cfpos.reset();
        cfpos.constrainField(categoryAndField.first, categoryAndField.second);
        for (int32_t i = 0; i < length; i++) {
            if (expectedFieldPositions[i].category != categoryAndField.first) {
                continue;
            }
            if (expectedFieldPositions[i].field != categoryAndField.second) {
                continue;
            }
            assertTrue(baseMessage + u"C has next position @ " + Int64ToUnicodeString(i),
                fv.nextPosition(cfpos, status));
            int32_t expectedCategory = expectedFieldPositions[i].category;
            int32_t expectedField = expectedFieldPositions[i].field;
            int32_t expectedStart = expectedFieldPositions[i].beginIndex;
            int32_t expectedLimit = expectedFieldPositions[i].endIndex;
            assertEquals(baseMessage + u"C category @ " + Int64ToUnicodeString(i),
                expectedCategory, cfpos.getCategory());
            assertEquals(baseMessage + u"C field @ " + Int64ToUnicodeString(i),
                expectedField, cfpos.getField());
            assertEquals(baseMessage + u"C start @ " + Int64ToUnicodeString(i),
                expectedStart, cfpos.getStart());
            assertEquals(baseMessage + u"C limit @ " + Int64ToUnicodeString(i),
                expectedLimit, cfpos.getLimit());
        }
        UBool afterLoopResult = fv.nextPosition(cfpos, status);
        assertFalse(baseMessage + u"C after loop: " + CFPosToUnicodeString(cfpos), afterLoopResult);
    }
}


extern IntlTest *createFormattedValueTest() {
    return new FormattedValueTest();
}

#endif /* !UCONFIG_NO_FORMATTING */
