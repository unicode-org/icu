// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "putilimp.h"
#include "unicode/dcfmtsym.h"
#include "numbertest.h"
#include "number_utils.h"
#include "number_skeletons.h"

using namespace icu::number::impl;


void NumberSkeletonTest::runIndexedTest(int32_t index, UBool exec, const char*& name, char*) {
    if (exec) {
        logln("TestSuite AffixUtilsTest: ");
    }
    TESTCASE_AUTO_BEGIN;
        TESTCASE_AUTO(validTokens);
        TESTCASE_AUTO(invalidTokens);
        TESTCASE_AUTO(unknownTokens);
        TESTCASE_AUTO(unexpectedTokens);
        TESTCASE_AUTO(duplicateValues);
        TESTCASE_AUTO(stemsRequiringOption);
        TESTCASE_AUTO(defaultTokens);
        TESTCASE_AUTO(flexibleSeparators);
    TESTCASE_AUTO_END;
}

void NumberSkeletonTest::validTokens() {
    // This tests only if the tokens are valid, not their behavior.
    // Most of these are from the design doc.
    static const char16_t* cases[] = {
            u"round-integer",
            u"round-unlimited",
            u"@@@##",
            u"@@+",
            u".000##",
            u".00+",
            u".",
            u".+",
            u".######",
            u".00/@@+",
            u".00/@##",
            u"round-increment/3.14",
            u"round-currency-standard",
            u"round-integer/half-up",
            u".00#/ceiling",
            u".00/@@+/floor",
            u"scientific",
            u"scientific/+ee",
            u"scientific/sign-always",
            u"scientific/+ee/sign-always",
            u"scientific/sign-always/+ee",
            u"scientific/sign-except-zero",
            u"engineering",
            u"engineering/+eee",
            u"compact-short",
            u"compact-long",
            u"notation-simple",
            u"percent",
            u"permille",
            u"measure-unit/length-meter",
            u"measure-unit/area-square-meter",
            u"measure-unit/energy-joule per-measure-unit/length-meter",
            u"currency/XXX",
            u"currency/ZZZ",
            u"currency/usd",
            u"group-off",
            u"group-min2",
            u"group-auto",
            u"group-on-aligned",
            u"group-thousands",
            u"integer-width/00",
            u"integer-width/#0",
            u"integer-width/+00",
            u"sign-always",
            u"sign-auto",
            u"sign-never",
            u"sign-accounting",
            u"sign-accounting-always",
            u"sign-except-zero",
            u"sign-accounting-except-zero",
            u"unit-width-narrow",
            u"unit-width-short",
            u"unit-width-iso-code",
            u"unit-width-full-name",
            u"unit-width-hidden",
            u"decimal-auto",
            u"decimal-always",
            u"scale/5.2",
            u"scale/-5.2",
            u"scale/100",
            u"scale/1E2",
            u"scale/1",
            u"latin",
            u"numbering-system/arab",
            u"numbering-system/latn",
            u"round-integer/@##",
            u"round-integer/ceiling",
            u"round-currency-cash/ceiling"};

    for (auto& cas : cases) {
        UnicodeString skeletonString(cas);
        UErrorCode status = U_ZERO_ERROR;
        NumberFormatter::fromSkeleton(skeletonString, status);
        assertSuccess(skeletonString, status);
    }
}

void NumberSkeletonTest::invalidTokens() {
    static const char16_t* cases[] = {
            u".00x",
            u".00##0",
            u".##+",
            u".00##+",
            u".0#+",
            u"@@x",
            u"@@##0",
            u"@#+",
            u".00/@",
            u".00/@@",
            u".00/@@x",
            u".00/@@#",
            u".00/@@#+",
            u".00/floor/@@+", // wrong order
            u"round-increment/français", // non-invariant characters for C++
            u"round-currency-cash/XXX",
            u"scientific/ee",
            u"round-increment/xxx",
            u"round-increment/NaN",
            u"round-increment/0.1.2",
            u"scale/xxx",
            u"scale/NaN",
            u"scale/0.1.2",
            u"scale/français", // non-invariant characters for C++
            u"currency/dummy",
            u"currency/ççç", // three characters but not ASCII
            u"measure-unit/foo",
            u"integer-width/xxx",
            u"integer-width/0+",
            u"integer-width/+0#",
            u"scientific/foo"};

    expectedErrorSkeleton(cases, sizeof(cases) / sizeof(*cases));
}

void NumberSkeletonTest::unknownTokens() {
    static const char16_t* cases[] = {
            u"maesure-unit",
            u"measure-unit/foo-bar",
            u"numbering-system/dummy",
            u"français",
            u"measure-unit/français-français", // non-invariant characters for C++
            u"numbering-system/français", // non-invariant characters for C++
            u"currency-USD"};

    expectedErrorSkeleton(cases, sizeof(cases) / sizeof(*cases));
}

void NumberSkeletonTest::unexpectedTokens() {
    static const char16_t* cases[] = {
            u"group-thousands/foo",
            u"round-integer//ceiling group-off",
            u"round-integer//ceiling  group-off",
            u"round-integer/ group-off",
            u"round-integer// group-off"};

    expectedErrorSkeleton(cases, sizeof(cases) / sizeof(*cases));
}

void NumberSkeletonTest::duplicateValues() {
    static const char16_t* cases[] = {
            u"round-integer round-integer",
            u"round-integer .00+",
            u"round-integer round-unlimited",
            u"round-integer @@@",
            u"scientific engineering",
            u"engineering compact-long",
            u"sign-auto sign-always"};

    expectedErrorSkeleton(cases, sizeof(cases) / sizeof(*cases));
}

void NumberSkeletonTest::stemsRequiringOption() {
    static const char16_t* stems[] = {
            u"round-increment",
            u"measure-unit",
            u"per-unit",
            u"currency",
            u"integer-width",
            u"numbering-system",
            u"scale"};
    static const char16_t* suffixes[] = {u"", u"/ceiling", u" scientific", u"/ceiling scientific"};

    for (auto& stem : stems) {
        for (auto& suffix : suffixes) {
            UnicodeString skeletonString = UnicodeString(stem) + suffix;
            UErrorCode status = U_ZERO_ERROR;
            NumberFormatter::fromSkeleton(skeletonString, status);
            assertEquals(skeletonString, U_NUMBER_SKELETON_SYNTAX_ERROR, status);
        }
    }
}

void NumberSkeletonTest::defaultTokens() {
    IcuTestErrorCode status(*this, "defaultTokens");

    static const char16_t* cases[] = {
            u"notation-simple",
            u"base-unit",
            u"group-auto",
            u"integer-width/+0",
            u"sign-auto",
            u"unit-width-short",
            u"decimal-auto"};

    for (auto& cas : cases) {
        UnicodeString skeletonString(cas);
        status.setScope(skeletonString);
        UnicodeString normalized = NumberFormatter::fromSkeleton(
                skeletonString, status).toSkeleton(status);
        // Skeleton should become empty when normalized
        assertEquals(skeletonString, u"", normalized);
    }
}

void NumberSkeletonTest::flexibleSeparators() {
    IcuTestErrorCode status(*this, "flexibleSeparators");

    static struct TestCase {
        const char16_t* skeleton;
        const char16_t* expected;
    } cases[] = {{u"round-integer group-off", u"5142"},
                 {u"round-integer  group-off", u"5142"},
                 {u"round-integer/ceiling group-off", u"5143"},
                 {u"round-integer/ceiling  group-off", u"5143"}};

    for (auto& cas : cases) {
        UnicodeString skeletonString(cas.skeleton);
        UnicodeString expected(cas.expected);
        status.setScope(skeletonString);
        UnicodeString actual = NumberFormatter::fromSkeleton(skeletonString, status).locale("en")
                .formatDouble(5142.3, status)
                .toString();
        assertEquals(skeletonString, expected, actual);
    }
}

// In C++, there is no distinguishing between "invalid", "unknown", and "unexpected" tokens.
void NumberSkeletonTest::expectedErrorSkeleton(const char16_t** cases, int32_t casesLen) {
    for (int32_t i = 0; i < casesLen; i++) {
        UnicodeString skeletonString(cases[i]);
        UErrorCode status = U_ZERO_ERROR;
        NumberFormatter::fromSkeleton(skeletonString, status);
        assertEquals(skeletonString, U_NUMBER_SKELETON_SYNTAX_ERROR, status);
    }
}


#endif /* #if !UCONFIG_NO_FORMATTING */
