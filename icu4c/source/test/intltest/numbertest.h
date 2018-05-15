// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING
#pragma once

#include "number_stringbuilder.h"
#include "intltest.h"
#include "number_affixutils.h"
#include "numparse_stringsegment.h"
#include "unicode/locid.h"

using namespace icu::number;
using namespace icu::number::impl;
using namespace icu::numparse;
using namespace icu::numparse::impl;

////////////////////////////////////////////////////////////////////////////////////////
// INSTRUCTIONS:                                                                      //
// To add new NumberFormat unit test classes, create a new class like the ones below, //
// and then add it as a switch statement in NumberTest at the bottom of this file.    /////////
// To add new methods to existing unit test classes, add the method to the class declaration //
// below, and also add it to the class's implementation of runIndexedTest().                 //
///////////////////////////////////////////////////////////////////////////////////////////////

class AffixUtilsTest : public IntlTest {
  public:
    void testEscape();
    void testUnescape();
    void testContainsReplaceType();
    void testInvalid();
    void testUnescapeWithSymbolProvider();

    void runIndexedTest(int32_t index, UBool exec, const char *&name, char *par = 0);

  private:
    UnicodeString unescapeWithDefaults(const SymbolProvider &defaultProvider, UnicodeString input,
                                       UErrorCode &status);
};

class NumberFormatterApiTest : public IntlTest {
  public:
    NumberFormatterApiTest();
    NumberFormatterApiTest(UErrorCode &status);

    void notationSimple();
    void notationScientific();
    void notationCompact();
    void unitMeasure();
    void unitCompoundMeasure();
    void unitCurrency();
    void unitPercent();
    void roundingFraction();
    void roundingFigures();
    void roundingFractionFigures();
    void roundingOther();
    void grouping();
    void padding();
    void integerWidth();
    void symbols();
    // TODO: Add this method if currency symbols override support is added.
    //void symbolsOverride();
    void sign();
    void decimal();
    void scale();
    void locale();
    void formatTypes();
    void fieldPosition();
    void toFormat();
    void errors();
    void validRanges();
    void copyMove();
    void localPointerCAPI();

    void runIndexedTest(int32_t index, UBool exec, const char *&name, char *par = 0);

  private:
    CurrencyUnit USD;
    CurrencyUnit GBP;
    CurrencyUnit CZK;
    CurrencyUnit CAD;
    CurrencyUnit ESP;
    CurrencyUnit PTE;

    MeasureUnit METER;
    MeasureUnit DAY;
    MeasureUnit SQUARE_METER;
    MeasureUnit FAHRENHEIT;
    MeasureUnit SECOND;
    MeasureUnit POUND;
    MeasureUnit SQUARE_MILE;
    MeasureUnit JOULE;
    MeasureUnit FURLONG;
    MeasureUnit KELVIN;

    NumberingSystem MATHSANB;
    NumberingSystem LATN;

    DecimalFormatSymbols FRENCH_SYMBOLS;
    DecimalFormatSymbols SWISS_SYMBOLS;
    DecimalFormatSymbols MYANMAR_SYMBOLS;

    void assertFormatDescending(const char16_t* message, const char16_t* skeleton,
                                const UnlocalizedNumberFormatter& f, Locale locale, ...);

    void assertFormatDescendingBig(const char16_t* message, const char16_t* skeleton,
                                   const UnlocalizedNumberFormatter& f, Locale locale, ...);

    void assertFormatSingle(const char16_t* message, const char16_t* skeleton,
                            const UnlocalizedNumberFormatter& f, Locale locale, double input,
                            const UnicodeString& expected);

    void assertUndefinedSkeleton(const UnlocalizedNumberFormatter& f);
};

class DecimalQuantityTest : public IntlTest {
  public:
    void testDecimalQuantityBehaviorStandalone();
    void testSwitchStorage();
    void testCopyMove();
    void testAppend();
    void testConvertToAccurateDouble();
    void testUseApproximateDoubleWhenAble();
    void testHardDoubleConversion();
    void testToDouble();
    void testMaxDigits();

    void runIndexedTest(int32_t index, UBool exec, const char *&name, char *par = 0);

  private:
    void assertDoubleEquals(UnicodeString message, double a, double b);
    void assertHealth(const DecimalQuantity &fq);
    void assertToStringAndHealth(const DecimalQuantity &fq, const UnicodeString &expected);
    void checkDoubleBehavior(double d, bool explicitRequired);
};

class DoubleConversionTest : public IntlTest {
  public:
    void testDoubleConversionApi();

    void runIndexedTest(int32_t index, UBool exec, const char *&name, char *par = 0);
};

class ModifiersTest : public IntlTest {
  public:
    void testConstantAffixModifier();
    void testConstantMultiFieldModifier();
    void testSimpleModifier();
    void testCurrencySpacingEnabledModifier();

    void runIndexedTest(int32_t index, UBool exec, const char *&name, char *par = 0);

  private:
    void assertModifierEquals(const Modifier &mod, int32_t expectedPrefixLength, bool expectedStrong,
                              UnicodeString expectedChars, UnicodeString expectedFields,
                              UErrorCode &status);

    void assertModifierEquals(const Modifier &mod, NumberStringBuilder &sb, int32_t expectedPrefixLength,
                              bool expectedStrong, UnicodeString expectedChars,
                              UnicodeString expectedFields, UErrorCode &status);
};

class PatternModifierTest : public IntlTest {
  public:
    void testBasic();
    void testPatternWithNoPlaceholder();
    void testMutableEqualsImmutable();

    void runIndexedTest(int32_t index, UBool exec, const char *&name, char *par = 0);

  private:
    UnicodeString getPrefix(const MutablePatternModifier &mod, UErrorCode &status);
    UnicodeString getSuffix(const MutablePatternModifier &mod, UErrorCode &status);
};

class PatternStringTest : public IntlTest {
  public:
    void testLocalized();
    void testToPatternSimple();
    void testExceptionOnInvalid();
    void testBug13117();

    void runIndexedTest(int32_t index, UBool exec, const char *&name, char *par = 0);

  private:
};

class NumberStringBuilderTest : public IntlTest {
  public:
    void testInsertAppendUnicodeString();
    void testSplice();
    void testInsertAppendCodePoint();
    void testCopy();
    void testFields();
    void testUnlimitedCapacity();
    void testCodePoints();

    void runIndexedTest(int32_t index, UBool exec, const char *&name, char *par = 0);

  private:
    void assertEqualsImpl(const UnicodeString &a, const NumberStringBuilder &b);
};

class StringSegmentTest : public IntlTest {
  public:
    void testOffset();
    void testLength();
    void testCharAt();
    void testGetCodePoint();
    void testCommonPrefixLength();

    void runIndexedTest(int32_t index, UBool exec, const char *&name, char *par = 0);
};

class UniSetsTest : public IntlTest {
  public:
    void testSetCoverage();

    void runIndexedTest(int32_t index, UBool exec, const char *&name, char *par = 0);

  private:
    void assertInSet(const UnicodeString& localeName, const UnicodeString &setName,
                     const UnicodeSet& set, const UnicodeString& str);
    void assertInSet(const UnicodeString& localeName, const UnicodeString &setName,
                     const UnicodeSet& set, UChar32 cp);
};

class NumberParserTest : public IntlTest {
  public:
    void testBasic();
    void testLocaleFi();
    void testSeriesMatcher();
    void testCombinedCurrencyMatcher();
    void testAffixPatternMatcher();
    void testGroupingDisabled();
    void testCaseFolding();

    void runIndexedTest(int32_t index, UBool exec, const char *&name, char *par = 0);
};

class NumberSkeletonTest : public IntlTest {
  public:
    void validTokens();
    void invalidTokens();
    void unknownTokens();
    void unexpectedTokens();
    void duplicateValues();
    void stemsRequiringOption();
    void defaultTokens();
    void flexibleSeparators();

    void runIndexedTest(int32_t index, UBool exec, const char *&name, char *par = 0);

  private:
    void expectedErrorSkeleton(const char16_t** cases, int32_t casesLen);
};


// NOTE: This macro is identical to the one in itformat.cpp
#define TESTCLASS(id, TestClass)          \
    case id:                              \
        name = #TestClass;                \
        if (exec) {                       \
            logln(#TestClass " test---"); \
            logln((UnicodeString)"");     \
            TestClass test;               \
            callTest(test, par);          \
        }                                 \
        break

class NumberTest : public IntlTest {
  public:
    void runIndexedTest(int32_t index, UBool exec, const char *&name, char *par = 0) {
        if (exec) {
            logln("TestSuite NumberTest: ");
        }

        switch (index) {
        TESTCLASS(0, AffixUtilsTest);
        TESTCLASS(1, NumberFormatterApiTest);
        TESTCLASS(2, DecimalQuantityTest);
        TESTCLASS(3, ModifiersTest);
        TESTCLASS(4, PatternModifierTest);
        TESTCLASS(5, PatternStringTest);
        TESTCLASS(6, NumberStringBuilderTest);
        TESTCLASS(7, DoubleConversionTest);
        TESTCLASS(8, StringSegmentTest);
        TESTCLASS(9, UniSetsTest);
        TESTCLASS(10, NumberParserTest);
        TESTCLASS(11, NumberSkeletonTest);
        default: name = ""; break; // needed to end loop
        }
    }
};

#endif /* #if !UCONFIG_NO_FORMATTING */
