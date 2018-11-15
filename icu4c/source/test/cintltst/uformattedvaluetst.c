// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/uformattedvalue.h"
#include "unicode/unum.h"
#include "unicode/ustring.h"
#include "cformtst.h"
#include "cintltst.h"
#include "cmemory.h"
#include "cstring.h"
#include "uassert.h"

static void TestBasic(void);
static void TestSetters(void);

static void AssertAllPartsEqual(
    const char* messagePrefix,
    const UConstrainedFieldPosition* ucfpos,
    UCFPosConstraintType constraint,
    UFieldCategory category,
    int32_t field,
    int32_t start,
    int32_t limit,
    int64_t context);

void addUFormattedValueTest(TestNode** root);

#define TESTCASE(x) addTest(root, &x, "tsformat/uformattedvalue/" #x)

void addUFormattedValueTest(TestNode** root) {
    TESTCASE(TestBasic);
    TESTCASE(TestSetters);
}


static void TestBasic() {
    UErrorCode status = U_ZERO_ERROR;
    UConstrainedFieldPosition* ucfpos = ucfpos_open(&status);
    assertSuccess("opening ucfpos", &status);
    assertTrue("ucfpos should not be null", ucfpos != NULL);

    AssertAllPartsEqual(
        "basic",
        ucfpos,
        UCFPOS_CONSTRAINT_NONE,
        UFIELD_CATEGORY_UNDEFINED,
        0,
        0,
        0,
        0LL);

    ucfpos_close(ucfpos);
}

void TestSetters() {
    UErrorCode status = U_ZERO_ERROR;
    UConstrainedFieldPosition* ucfpos = ucfpos_open(&status);
    assertSuccess("opening ucfpos", &status);
    assertTrue("ucfpos should not be null", ucfpos != NULL);

    ucfpos_constrainCategory(ucfpos, UFIELD_CATEGORY_DATE, &status);
    assertSuccess("setters 0", &status);
    AssertAllPartsEqual(
        "setters 0",
        ucfpos,
        UCFPOS_CONSTRAINT_CATEGORY,
        UFIELD_CATEGORY_DATE,
        0,
        0,
        0,
        0LL);

    ucfpos_constrainField(ucfpos, UFIELD_CATEGORY_NUMBER, UNUM_COMPACT_FIELD, &status);
    assertSuccess("setters 1", &status);
    AssertAllPartsEqual(
        "setters 1",
        ucfpos,
        UCFPOS_CONSTRAINT_FIELD,
        UFIELD_CATEGORY_NUMBER,
        UNUM_COMPACT_FIELD,
        0,
        0,
        0LL);

    ucfpos_setInt64IterationContext(ucfpos, 42424242424242LL, &status);
    assertSuccess("setters 2", &status);
    AssertAllPartsEqual(
        "setters 2",
        ucfpos,
        UCFPOS_CONSTRAINT_FIELD,
        UFIELD_CATEGORY_NUMBER,
        UNUM_COMPACT_FIELD,
        0,
        0,
        42424242424242LL);

    ucfpos_setState(ucfpos, UFIELD_CATEGORY_NUMBER, UNUM_COMPACT_FIELD, 5, 10, &status);
    assertSuccess("setters 3", &status);
    AssertAllPartsEqual(
        "setters 3",
        ucfpos,
        UCFPOS_CONSTRAINT_FIELD,
        UFIELD_CATEGORY_NUMBER,
        UNUM_COMPACT_FIELD,
        5,
        10,
        42424242424242LL);

    ucfpos_reset(ucfpos, &status);
    assertSuccess("setters 4", &status);
    AssertAllPartsEqual(
        "setters 4",
        ucfpos,
        UCFPOS_CONSTRAINT_NONE,
        UFIELD_CATEGORY_UNDEFINED,
        0,
        0,
        0,
        0LL);

    ucfpos_close(ucfpos);
}

static void AssertAllPartsEqual(
        const char* messagePrefix,
        const UConstrainedFieldPosition* ucfpos,
        UCFPosConstraintType constraint,
        UFieldCategory category,
        int32_t field,
        int32_t start,
        int32_t limit,
        int64_t context) {

    UErrorCode status = U_ZERO_ERROR;

    char message[256];
    uprv_strncpy(message, messagePrefix, 256);
    int32_t prefixEnd = uprv_strlen(messagePrefix);
    message[prefixEnd++] = ':';
    message[prefixEnd++] = ' ';
    U_ASSERT(prefixEnd < 256);

#define AAPE_MSG(suffix) (uprv_strncpy(message+prefixEnd, suffix, 256-prefixEnd)-prefixEnd)

    UCFPosConstraintType _constraintType = ucfpos_getConstraintType(ucfpos, &status);
    assertSuccess(AAPE_MSG("constraint"), &status);
    assertIntEquals(AAPE_MSG("constraint"), constraint, _constraintType);

    UFieldCategory _category = ucfpos_getCategory(ucfpos, &status);
    assertSuccess(AAPE_MSG("_"), &status);
    assertIntEquals(AAPE_MSG("category"), category, _category);

    int32_t _field = ucfpos_getField(ucfpos, &status);
    assertSuccess(AAPE_MSG("field"), &status);
    assertIntEquals(AAPE_MSG("field"), field, _field);

    int32_t _start, _limit;
    ucfpos_getIndexes(ucfpos, &_start, &_limit, &status);
    assertSuccess(AAPE_MSG("indexes"), &status);
    assertIntEquals(AAPE_MSG("start"), start, _start);
    assertIntEquals(AAPE_MSG("limit"), limit, _limit);

    int64_t _context = ucfpos_getInt64IterationContext(ucfpos, &status);
    assertSuccess(AAPE_MSG("context"), &status);
    assertIntEquals(AAPE_MSG("context"), context, _context);
}


// Declared in cformtst.h
void checkFormattedValue(
        const char* message,
        const UFormattedValue* fv,
        const UChar* expectedString,
        UFieldCategory expectedCategory,
        const UFieldPosition* expectedFieldPositions,
        int32_t expectedFieldPositionsLength) {
    UErrorCode status = U_ZERO_ERROR;
    int32_t length;
    const UChar* actualString = ufmtval_getString(fv, &length, &status);
    assertSuccess(message, &status);
    // The string is guaranteed to be NUL-terminated.
    int32_t actualLength = u_strlen(actualString);
    assertIntEquals(message, actualLength, length);
    assertUEquals(message, expectedString, actualString);
}


#endif /* #if !UCONFIG_NO_FORMATTING */
