// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#define TEMP_TEST_HELPERS_NO_PRINT_PASS 1

#include <charstr.h>
#include <cstdarg>
#include <unicode/unum.h>
#include <unicode/uclean.h>
#include "temp_test_helpers.h"
#include "unicode/numberformatter.h"
#include "number_types.h"

#include <iostream>
#include "unicode/ustream.h"

using namespace icu;
using namespace icu::number;
using namespace icu::number::impl;

class numbertest_api {
  public:
    explicit numbertest_api(UErrorCode &status) : USD(u"USD", status), GBP(u"GBP", status),
                                                       CZK(u"CZK", status), CAD(u"CAD", status),
                                                       FRENCH_SYMBOLS(Locale::getFrench(), status),
                                                       SWISS_SYMBOLS(Locale("de-CH"), status),
                                                       MYANMAR_SYMBOLS(Locale("my"), status) {
        MeasureUnit *unit = MeasureUnit::createMeter(status);
        METER = *unit;
        delete unit;
        unit = MeasureUnit::createDay(status);
        DAY = *unit;
        delete unit;
        unit = MeasureUnit::createSquareMeter(status);
        SQUARE_METER = *unit;
        delete unit;
        unit = MeasureUnit::createFahrenheit(status);
        FAHRENHEIT = *unit;
        delete unit;

        NumberingSystem *ns = NumberingSystem::createInstanceByName("mathsanb", status);
        MATHSANB = *ns;
        delete ns;
        ns = NumberingSystem::createInstanceByName("latn", status);
        LATN = *ns;
        delete ns;
    }

    void notationSimple();

    void notationScientific();

    void notationCompact();

    void unitMeasure();

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

    void locale();

    void errors();

  private:
    CurrencyUnit USD;
    CurrencyUnit GBP;
    CurrencyUnit CZK;
    CurrencyUnit CAD;

    MeasureUnit METER;
    MeasureUnit DAY;
    MeasureUnit SQUARE_METER;
    MeasureUnit FAHRENHEIT;

    NumberingSystem MATHSANB;
    NumberingSystem LATN;

    DecimalFormatSymbols FRENCH_SYMBOLS;
    DecimalFormatSymbols SWISS_SYMBOLS;
    DecimalFormatSymbols MYANMAR_SYMBOLS;

    void assertFormatDescending(const UnicodeString &message, const UnlocalizedNumberFormatter &f,
                                const Locale &locale, ...);

    void assertFormatDescendingBig(const UnicodeString &message, const UnlocalizedNumberFormatter &f,
                                   const Locale &locale, ...);

    void assertFormatSingle(const UnicodeString &message, const UnlocalizedNumberFormatter &f,
                            const Locale &locale, double input, const UnicodeString &expected);
};

void numbertest_api::notationSimple() {
    assertFormatDescending(
            u"Basic",
            NumberFormatter::with(),
            Locale::getEnglish(),
            u"87,650",
            u"8,765",
            u"876.5",
            u"87.65",
            u"8.765",
            u"0.8765",
            u"0.08765",
            u"0.008765",
            u"0");

    assertFormatSingle(
            u"Basic with Negative Sign",
            NumberFormatter::with(),
            Locale::getEnglish(),
            -9876543.21,
            u"-9,876,543.21");
}


void numbertest_api::notationScientific() {
    assertFormatDescending(
            u"Scientific",
            NumberFormatter::with().notation(Notation::scientific()),
            Locale::getEnglish(),
            u"8.765E4",
            u"8.765E3",
            u"8.765E2",
            u"8.765E1",
            u"8.765E0",
            u"8.765E-1",
            u"8.765E-2",
            u"8.765E-3",
            u"0E0");

    assertFormatDescending(
            u"Engineering",
            NumberFormatter::with().notation(Notation::engineering()),
            Locale::getEnglish(),
            u"87.65E3",
            u"8.765E3",
            u"876.5E0",
            u"87.65E0",
            u"8.765E0",
            u"876.5E-3",
            u"87.65E-3",
            u"8.765E-3",
            u"0E0");

    assertFormatDescending(
            u"Scientific sign always shown",
            NumberFormatter::with().notation(
                    Notation::scientific().withExponentSignDisplay(UNumberSignDisplay::UNUM_SIGN_ALWAYS)),
            Locale::getEnglish(),
            u"8.765E+4",
            u"8.765E+3",
            u"8.765E+2",
            u"8.765E+1",
            u"8.765E+0",
            u"8.765E-1",
            u"8.765E-2",
            u"8.765E-3",
            u"0E+0");

    assertFormatDescending(
            u"Scientific min exponent digits",
            NumberFormatter::with().notation(Notation::scientific().withMinExponentDigits(2)),
            Locale::getEnglish(),
            u"8.765E04",
            u"8.765E03",
            u"8.765E02",
            u"8.765E01",
            u"8.765E00",
            u"8.765E-01",
            u"8.765E-02",
            u"8.765E-03",
            u"0E00");

    assertFormatSingle(
            u"Scientific Negative",
            NumberFormatter::with().notation(Notation::scientific()),
            Locale::getEnglish(),
            -1000000,
            u"-1E6");
}

void numbertest_api::notationCompact() {
    assertFormatDescending(
            u"Compact Short",
            NumberFormatter::with().notation(Notation::compactShort()),
            Locale::getEnglish(),
            u"88K",
            u"8.8K",
            u"876",
            u"88",
            u"8.8",
            u"0.88",
            u"0.088",
            u"0.0088",
            u"0");

    assertFormatDescending(
            u"Compact Long",
            NumberFormatter::with().notation(Notation::compactLong()),
            Locale::getEnglish(),
            u"88 thousand",
            u"8.8 thousand",
            u"876",
            u"88",
            u"8.8",
            u"0.88",
            u"0.088",
            u"0.0088",
            u"0");

    assertFormatDescending(
            u"Compact Short Currency",
            NumberFormatter::with().notation(Notation::compactShort()).unit(USD),
            Locale::getEnglish(),
            u"$88K",
            u"$8.8K",
            u"$876",
            u"$88",
            u"$8.8",
            u"$0.88",
            u"$0.088",
            u"$0.0088",
            u"$0");

    assertFormatDescending(
            u"Compact Short with ISO Currency",
            NumberFormatter::with().notation(Notation::compactShort())
                    .unit(USD)
                    .unitWidth(UNumberUnitWidth::UNUM_UNIT_WIDTH_ISO_CODE),
            Locale::getEnglish(),
            u"USD 88K",
            u"USD 8.8K",
            u"USD 876",
            u"USD 88",
            u"USD 8.8",
            u"USD 0.88",
            u"USD 0.088",
            u"USD 0.0088",
            u"USD 0");

    assertFormatDescending(
            u"Compact Short with Long Name Currency",
            NumberFormatter::with().notation(Notation::compactShort())
                    .unit(USD)
                    .unitWidth(UNumberUnitWidth::UNUM_UNIT_WIDTH_FULL_NAME),
            Locale::getEnglish(),
            u"88K US dollars",
            u"8.8K US dollars",
            u"876 US dollars",
            u"88 US dollars",
            u"8.8 US dollars",
            u"0.88 US dollars",
            u"0.088 US dollars",
            u"0.0088 US dollars",
            u"0 US dollars");

    // Note: Most locales don't have compact long currency, so this currently falls back to short.
    // This test case should be fixed when proper compact long currency patterns are added.
    assertFormatDescending(
            u"Compact Long Currency",
            NumberFormatter::with().notation(Notation::compactLong()).unit(USD),
            Locale::getEnglish(),
            u"$88K", // should be something like "$88 thousand"
            u"$8.8K",
            u"$876",
            u"$88",
            u"$8.8",
            u"$0.88",
            u"$0.088",
            u"$0.0088",
            u"$0");

    // Note: Most locales don't have compact long currency, so this currently falls back to short.
    // This test case should be fixed when proper compact long currency patterns are added.
    assertFormatDescending(
            u"Compact Long with ISO Currency",
            NumberFormatter::with().notation(Notation::compactLong())
                    .unit(USD)
                    .unitWidth(UNumberUnitWidth::UNUM_UNIT_WIDTH_ISO_CODE),
            Locale::getEnglish(),
            u"USD 88K", // should be something like "USD 88 thousand"
            u"USD 8.8K",
            u"USD 876",
            u"USD 88",
            u"USD 8.8",
            u"USD 0.88",
            u"USD 0.088",
            u"USD 0.0088",
            u"USD 0");

    // TODO: This behavior could be improved and should be revisited.
    assertFormatDescending(
            u"Compact Long with Long Name Currency",
            NumberFormatter::with().notation(Notation::compactLong())
                    .unit(USD)
                    .unitWidth(UNumberUnitWidth::UNUM_UNIT_WIDTH_FULL_NAME),
            Locale::getEnglish(),
            u"88 thousand US dollars",
            u"8.8 thousand US dollars",
            u"876 US dollars",
            u"88 US dollars",
            u"8.8 US dollars",
            u"0.88 US dollars",
            u"0.088 US dollars",
            u"0.0088 US dollars",
            u"0 US dollars");

    assertFormatSingle(
            u"Compact Plural One",
            NumberFormatter::with().notation(Notation::compactLong()),
            Locale::createFromName("es"),
            1000000,
            u"1 millón");

    assertFormatSingle(
            u"Compact Plural Other",
            NumberFormatter::with().notation(Notation::compactLong()),
            Locale::createFromName("es"),
            2000000,
            u"2 millones");

    assertFormatSingle(
            u"Compact with Negative Sign",
            NumberFormatter::with().notation(Notation::compactShort()),
            Locale::getEnglish(),
            -9876543.21,
            u"-9.9M");

    assertFormatSingle(
            u"Compact Rounding",
            NumberFormatter::with().notation(Notation::compactShort()),
            Locale::getEnglish(),
            990000,
            u"990K");

    assertFormatSingle(
            u"Compact Rounding",
            NumberFormatter::with().notation(Notation::compactShort()),
            Locale::getEnglish(),
            999000,
            u"999K");

    assertFormatSingle(
            u"Compact Rounding",
            NumberFormatter::with().notation(Notation::compactShort()),
            Locale::getEnglish(),
            999900,
            u"1M");

    assertFormatSingle(
            u"Compact Rounding",
            NumberFormatter::with().notation(Notation::compactShort()),
            Locale::getEnglish(),
            9900000,
            u"9.9M");

    assertFormatSingle(
            u"Compact Rounding",
            NumberFormatter::with().notation(Notation::compactShort()),
            Locale::getEnglish(),
            9990000,
            u"10M");
}

void numbertest_api::unitMeasure() {
    assertFormatDescending(
            u"Meters Short",
            NumberFormatter::with().adoptUnit(new MeasureUnit(METER)),
            Locale::getEnglish(),
            u"87,650 m",
            u"8,765 m",
            u"876.5 m",
            u"87.65 m",
            u"8.765 m",
            u"0.8765 m",
            u"0.08765 m",
            u"0.008765 m",
            u"0 m");

    assertFormatDescending(
            u"Meters Long",
            NumberFormatter::with().adoptUnit(new MeasureUnit(METER))
                    .unitWidth(UNumberUnitWidth::UNUM_UNIT_WIDTH_FULL_NAME),
            Locale::getEnglish(),
            u"87,650 meters",
            u"8,765 meters",
            u"876.5 meters",
            u"87.65 meters",
            u"8.765 meters",
            u"0.8765 meters",
            u"0.08765 meters",
            u"0.008765 meters",
            u"0 meters");

    assertFormatDescending(
            u"Compact Meters Long",
            NumberFormatter::with().notation(Notation::compactLong())
                    .adoptUnit(new MeasureUnit(METER))
                    .unitWidth(UNumberUnitWidth::UNUM_UNIT_WIDTH_FULL_NAME),
            Locale::getEnglish(),
            u"88 thousand meters",
            u"8.8 thousand meters",
            u"876 meters",
            u"88 meters",
            u"8.8 meters",
            u"0.88 meters",
            u"0.088 meters",
            u"0.0088 meters",
            u"0 meters");

//    TODO: Implement Measure in C++
//    assertFormatSingleMeasure(
//            u"Meters with Measure Input",
//            NumberFormatter::with().unitWidth(UNumberUnitWidth::UNUM_UNIT_WIDTH_FULL_NAME),
//            Locale::getEnglish(),
//            new Measure(5.43, new MeasureUnit(METER)),
//            u"5.43 meters");

//    TODO: Implement Measure in C++
//    assertFormatSingleMeasure(
//            u"Measure format method takes precedence over fluent chain",
//            NumberFormatter::with().adoptUnit(new MeasureUnit(METER)),
//            Locale::getEnglish(),
//            new Measure(5.43, USD),
//            u"$5.43");

    assertFormatSingle(
            u"Meters with Negative Sign",
            NumberFormatter::with().adoptUnit(new MeasureUnit(METER)),
            Locale::getEnglish(),
            -9876543.21,
            u"-9,876,543.21 m");

    // The locale string "सान" appears only in brx.txt:
    assertFormatSingle(
            u"Interesting Data Fallback 1",
            NumberFormatter::with().adoptUnit(new MeasureUnit(DAY))
                    .unitWidth(UNumberUnitWidth::UNUM_UNIT_WIDTH_FULL_NAME),
            Locale::createFromName("brx"),
            5.43,
            u"5.43 सान");

    // Requires following the alias from unitsNarrow to unitsShort:
    assertFormatSingle(
            u"Interesting Data Fallback 2",
            NumberFormatter::with().adoptUnit(new MeasureUnit(DAY))
                    .unitWidth(UNumberUnitWidth::UNUM_UNIT_WIDTH_NARROW),
            Locale::createFromName("brx"),
            5.43,
            u"5.43 d");

    // en_001.txt has a unitsNarrow/area/square-meter table, but table does not contain the OTHER unit,
    // requiring fallback to the root.
    assertFormatSingle(
            u"Interesting Data Fallback 3",
            NumberFormatter::with().adoptUnit(new MeasureUnit(SQUARE_METER))
                    .unitWidth(UNumberUnitWidth::UNUM_UNIT_WIDTH_NARROW),
            Locale::createFromName("en-GB"),
            5.43,
            u"5.43 m²");

    // es_US has "{0}°" for unitsNarrow/temperature/FAHRENHEIT.
    // NOTE: This example is in the documentation.
    assertFormatSingle(
            u"Difference between Narrow and Short (Narrow Version)",
            NumberFormatter::with().adoptUnit(new MeasureUnit(FAHRENHEIT))
                    .unitWidth(UNUM_UNIT_WIDTH_NARROW),
            Locale("es-US"),
            5.43,
            u"5.43°");

    assertFormatSingle(
            u"Difference between Narrow and Short (Short Version)",
            NumberFormatter::with().adoptUnit(new MeasureUnit(FAHRENHEIT))
                    .unitWidth(UNUM_UNIT_WIDTH_SHORT),
            Locale("es-US"),
            5.43,
            u"5.43 °F");
}

void numbertest_api::unitCurrency() {
    assertFormatDescending(
            u"Currency",
            NumberFormatter::with().unit(GBP),
            Locale::getEnglish(),
            u"£87,650.00",
            u"£8,765.00",
            u"£876.50",
            u"£87.65",
            u"£8.76",
            u"£0.88",
            u"£0.09",
            u"£0.01",
            u"£0.00");

    assertFormatDescending(
            u"Currency ISO",
            NumberFormatter::with().unit(GBP).unitWidth(UNumberUnitWidth::UNUM_UNIT_WIDTH_ISO_CODE),
            Locale::getEnglish(),
            u"GBP 87,650.00",
            u"GBP 8,765.00",
            u"GBP 876.50",
            u"GBP 87.65",
            u"GBP 8.76",
            u"GBP 0.88",
            u"GBP 0.09",
            u"GBP 0.01",
            u"GBP 0.00");

    assertFormatDescending(
            u"Currency Long Name",
            NumberFormatter::with().unit(GBP).unitWidth(UNumberUnitWidth::UNUM_UNIT_WIDTH_FULL_NAME),
            Locale::getEnglish(),
            u"87,650.00 British pounds",
            u"8,765.00 British pounds",
            u"876.50 British pounds",
            u"87.65 British pounds",
            u"8.76 British pounds",
            u"0.88 British pounds",
            u"0.09 British pounds",
            u"0.01 British pounds",
            u"0.00 British pounds");

    assertFormatDescending(
            u"Currency Hidden",
            NumberFormatter::with().unit(GBP).unitWidth(UNUM_UNIT_WIDTH_HIDDEN),
            Locale::getEnglish(),
            u"87,650.00",
            u"8,765.00",
            u"876.50",
            u"87.65",
            u"8.76",
            u"0.88",
            u"0.09",
            u"0.01",
            u"0.00");

//    TODO: Implement Measure in C++
//    assertFormatSingleMeasure(
//            u"Currency with CurrencyAmount Input",
//            NumberFormatter::with(),
//            Locale::getEnglish(),
//            new CurrencyAmount(5.43, GBP),
//            u"£5.43");

//    TODO: Enable this test when DecimalFormat wrapper is done.
//    assertFormatSingle(
//            u"Currency Long Name from Pattern Syntax", NumberFormatter.fromDecimalFormat(
//                    PatternStringParser.parseToProperties("0 ¤¤¤"),
//                    DecimalFormatSymbols.getInstance(Locale::getEnglish()),
//                    null).unit(GBP), Locale::getEnglish(), 1234567.89, u"1234568 British pounds");

    assertFormatSingle(
            u"Currency with Negative Sign",
            NumberFormatter::with().unit(GBP),
            Locale::getEnglish(),
            -9876543.21,
            u"-£9,876,543.21");
}

void numbertest_api::unitPercent() {
    assertFormatDescending(
            u"Percent",
            NumberFormatter::with().unit(NoUnit::percent()),
            Locale::getEnglish(),
            u"87,650%",
            u"8,765%",
            u"876.5%",
            u"87.65%",
            u"8.765%",
            u"0.8765%",
            u"0.08765%",
            u"0.008765%",
            u"0%");

    assertFormatDescending(
            u"Permille",
            NumberFormatter::with().unit(NoUnit::permille()),
            Locale::getEnglish(),
            u"87,650‰",
            u"8,765‰",
            u"876.5‰",
            u"87.65‰",
            u"8.765‰",
            u"0.8765‰",
            u"0.08765‰",
            u"0.008765‰",
            u"0‰");

    assertFormatSingle(
            u"NoUnit Base",
            NumberFormatter::with().unit(NoUnit::base()),
            Locale::getEnglish(),
            51423,
            u"51,423");

    assertFormatSingle(
            u"Percent with Negative Sign",
            NumberFormatter::with().unit(NoUnit::percent()),
            Locale::getEnglish(),
            -98.7654321,
            u"-98.765432%");
}

void numbertest_api::roundingFraction() {
    assertFormatDescending(
            u"Integer",
            NumberFormatter::with().rounding(Rounder::integer()),
            Locale::getEnglish(),
            u"87,650",
            u"8,765",
            u"876",
            u"88",
            u"9",
            u"1",
            u"0",
            u"0",
            u"0");

    assertFormatDescending(
            u"Fixed Fraction",
            NumberFormatter::with().rounding(Rounder::fixedFraction(3)),
            Locale::getEnglish(),
            u"87,650.000",
            u"8,765.000",
            u"876.500",
            u"87.650",
            u"8.765",
            u"0.876",
            u"0.088",
            u"0.009",
            u"0.000");

    assertFormatDescending(
            u"Min Fraction",
            NumberFormatter::with().rounding(Rounder::minFraction(1)),
            Locale::getEnglish(),
            u"87,650.0",
            u"8,765.0",
            u"876.5",
            u"87.65",
            u"8.765",
            u"0.8765",
            u"0.08765",
            u"0.008765",
            u"0.0");

    assertFormatDescending(
            u"Max Fraction",
            NumberFormatter::with().rounding(Rounder::maxFraction(1)),
            Locale::getEnglish(),
            u"87,650",
            u"8,765",
            u"876.5",
            u"87.6",
            u"8.8",
            u"0.9",
            u"0.1",
            u"0",
            u"0");

    assertFormatDescending(
            u"Min/Max Fraction",
            NumberFormatter::with().rounding(Rounder::minMaxFraction(1, 3)),
            Locale::getEnglish(),
            u"87,650.0",
            u"8,765.0",
            u"876.5",
            u"87.65",
            u"8.765",
            u"0.876",
            u"0.088",
            u"0.009",
            u"0.0");
}

void numbertest_api::roundingFigures() {
    assertFormatSingle(
            u"Fixed Significant",
            NumberFormatter::with().rounding(Rounder::fixedDigits(3)),
            Locale::getEnglish(),
            -98,
            u"-98.0");

    assertFormatSingle(
            u"Fixed Significant Rounding",
            NumberFormatter::with().rounding(Rounder::fixedDigits(3)),
            Locale::getEnglish(),
            -98.7654321,
            u"-98.8");

    assertFormatSingle(
            u"Fixed Significant Zero",
            NumberFormatter::with().rounding(Rounder::fixedDigits(3)),
            Locale::getEnglish(),
            0,
            u"0.00");

    assertFormatSingle(
            u"Min Significant",
            NumberFormatter::with().rounding(Rounder::minDigits(2)),
            Locale::getEnglish(),
            -9,
            u"-9.0");

    assertFormatSingle(
            u"Max Significant",
            NumberFormatter::with().rounding(Rounder::maxDigits(4)),
            Locale::getEnglish(),
            98.7654321,
            u"98.77");

    assertFormatSingle(
            u"Min/Max Significant",
            NumberFormatter::with().rounding(Rounder::minMaxDigits(3, 4)),
            Locale::getEnglish(),
            9.99999,
            u"10.0");
}

void numbertest_api::roundingFractionFigures() {
    assertFormatDescending(
            u"Basic Significant", // for comparison
            NumberFormatter::with().rounding(Rounder::maxDigits(2)),
            Locale::getEnglish(),
            u"88,000",
            u"8,800",
            u"880",
            u"88",
            u"8.8",
            u"0.88",
            u"0.088",
            u"0.0088",
            u"0");

    assertFormatDescending(
            u"FracSig minMaxFrac minSig",
            NumberFormatter::with().rounding(Rounder::minMaxFraction(1, 2).withMinDigits(3)),
            Locale::getEnglish(),
            u"87,650.0",
            u"8,765.0",
            u"876.5",
            u"87.65",
            u"8.76",
            u"0.876", // minSig beats maxFrac
            u"0.0876", // minSig beats maxFrac
            u"0.00876", // minSig beats maxFrac
            u"0.0");

    assertFormatDescending(
            u"FracSig minMaxFrac maxSig A",
            NumberFormatter::with().rounding(Rounder::minMaxFraction(1, 3).withMaxDigits(2)),
            Locale::getEnglish(),
            u"88,000.0", // maxSig beats maxFrac
            u"8,800.0", // maxSig beats maxFrac
            u"880.0", // maxSig beats maxFrac
            u"88.0", // maxSig beats maxFrac
            u"8.8", // maxSig beats maxFrac
            u"0.88", // maxSig beats maxFrac
            u"0.088",
            u"0.009",
            u"0.0");

    assertFormatDescending(
            u"FracSig minMaxFrac maxSig B",
            NumberFormatter::with().rounding(Rounder::fixedFraction(2).withMaxDigits(2)),
            Locale::getEnglish(),
            u"88,000.00", // maxSig beats maxFrac
            u"8,800.00", // maxSig beats maxFrac
            u"880.00", // maxSig beats maxFrac
            u"88.00", // maxSig beats maxFrac
            u"8.80", // maxSig beats maxFrac
            u"0.88",
            u"0.09",
            u"0.01",
            u"0.00");
}

void numbertest_api::roundingOther() {
    assertFormatDescending(
            u"Rounding None",
            NumberFormatter::with().rounding(Rounder::unlimited()),
            Locale::getEnglish(),
            u"87,650",
            u"8,765",
            u"876.5",
            u"87.65",
            u"8.765",
            u"0.8765",
            u"0.08765",
            u"0.008765",
            u"0");

    assertFormatDescending(
            u"Increment",
            NumberFormatter::with().rounding(Rounder::increment(0.5).withMinFraction(1)),
            Locale::getEnglish(),
            u"87,650.0",
            u"8,765.0",
            u"876.5",
            u"87.5",
            u"9.0",
            u"1.0",
            u"0.0",
            u"0.0",
            u"0.0");

    assertFormatDescending(
            u"Increment with Min Fraction",
            NumberFormatter::with().rounding(Rounder::increment(0.5).withMinFraction(2)),
            Locale::getEnglish(),
            u"87,650.00",
            u"8,765.00",
            u"876.50",
            u"87.50",
            u"9.00",
            u"1.00",
            u"0.00",
            u"0.00",
            u"0.00");

    assertFormatDescending(
            u"Currency Standard",
            NumberFormatter::with().rounding(Rounder::currency(UCurrencyUsage::UCURR_USAGE_STANDARD))
                    .unit(CZK),
            Locale::getEnglish(),
            u"CZK 87,650.00",
            u"CZK 8,765.00",
            u"CZK 876.50",
            u"CZK 87.65",
            u"CZK 8.76",
            u"CZK 0.88",
            u"CZK 0.09",
            u"CZK 0.01",
            u"CZK 0.00");

    assertFormatDescending(
            u"Currency Cash",
            NumberFormatter::with().rounding(Rounder::currency(UCurrencyUsage::UCURR_USAGE_CASH))
                    .unit(CZK),
            Locale::getEnglish(),
            u"CZK 87,650",
            u"CZK 8,765",
            u"CZK 876",
            u"CZK 88",
            u"CZK 9",
            u"CZK 1",
            u"CZK 0",
            u"CZK 0",
            u"CZK 0");

    assertFormatDescending(
            u"Currency Cash with Nickel Rounding",
            NumberFormatter::with().rounding(Rounder::currency(UCurrencyUsage::UCURR_USAGE_CASH))
                    .unit(CAD),
            Locale::getEnglish(),
            u"CA$87,650.00",
            u"CA$8,765.00",
            u"CA$876.50",
            u"CA$87.65",
            u"CA$8.75",
            u"CA$0.90",
            u"CA$0.10",
            u"CA$0.00",
            u"CA$0.00");

    assertFormatDescending(
            u"Currency not in top-level fluent chain",
            NumberFormatter::with().rounding(
                    Rounder::currency(UCurrencyUsage::UCURR_USAGE_CASH).withCurrency(CZK)),
            Locale::getEnglish(),
            u"87,650",
            u"8,765",
            u"876",
            u"88",
            u"9",
            u"1",
            u"0",
            u"0",
            u"0");

    // NOTE: Other tests cover the behavior of the other rounding modes.
    assertFormatDescending(
            u"Rounding Mode CEILING",
            NumberFormatter::with().rounding(Rounder::integer().withMode(UNumberFormatRoundingMode::UNUM_ROUND_CEILING)),
            Locale::getEnglish(),
            u"87,650",
            u"8,765",
            u"877",
            u"88",
            u"9",
            u"1",
            u"1",
            u"1",
            u"0");
}

void numbertest_api::grouping() {
    assertFormatDescendingBig(
            u"Western Grouping",
            NumberFormatter::with().grouping(Grouper::defaults()),
            Locale::getEnglish(),
            u"87,650,000",
            u"8,765,000",
            u"876,500",
            u"87,650",
            u"8,765",
            u"876.5",
            u"87.65",
            u"8.765",
            u"0");

    assertFormatDescendingBig(
            u"Indic Grouping",
            NumberFormatter::with().grouping(Grouper::defaults()),
            Locale("en-IN"),
            u"8,76,50,000",
            u"87,65,000",
            u"8,76,500",
            u"87,650",
            u"8,765",
            u"876.5",
            u"87.65",
            u"8.765",
            u"0");

    assertFormatDescendingBig(
            u"Western Grouping, Wide",
            NumberFormatter::with().grouping(Grouper::minTwoDigits()),
            Locale::getEnglish(),
            u"87,650,000",
            u"8,765,000",
            u"876,500",
            u"87,650",
            u"8765",
            u"876.5",
            u"87.65",
            u"8.765",
            u"0");

    assertFormatDescendingBig(
            u"Indic Grouping, Wide",
            NumberFormatter::with().grouping(Grouper::minTwoDigits()),
            Locale("en-IN"),
            u"8,76,50,000",
            u"87,65,000",
            u"8,76,500",
            u"87,650",
            u"8765",
            u"876.5",
            u"87.65",
            u"8.765",
            u"0");

    assertFormatDescendingBig(
            u"No Grouping",
            NumberFormatter::with().grouping(Grouper::none()),
            Locale("en-IN"),
            u"87650000",
            u"8765000",
            u"876500",
            u"87650",
            u"8765",
            u"876.5",
            u"87.65",
            u"8.765",
            u"0");
}

void numbertest_api::padding() {
    assertFormatDescending(
            u"Padding",
            NumberFormatter::with().padding(Padder::none()),
            Locale::getEnglish(),
            u"87,650",
            u"8,765",
            u"876.5",
            u"87.65",
            u"8.765",
            u"0.8765",
            u"0.08765",
            u"0.008765",
            u"0");

    assertFormatDescending(
            u"Padding",
            NumberFormatter::with().padding(
                    Padder::codePoints(
                            '*', 8, PadPosition::UNUM_PAD_AFTER_PREFIX)),
            Locale::getEnglish(),
            u"**87,650",
            u"***8,765",
            u"***876.5",
            u"***87.65",
            u"***8.765",
            u"**0.8765",
            u"*0.08765",
            u"0.008765",
            u"*******0");

    assertFormatDescending(
            u"Padding with code points",
            NumberFormatter::with().padding(
                    Padder::codePoints(
                            0x101E4, 8, PadPosition::UNUM_PAD_AFTER_PREFIX)),
            Locale::getEnglish(),
            u"𐇤𐇤87,650",
            u"𐇤𐇤𐇤8,765",
            u"𐇤𐇤𐇤876.5",
            u"𐇤𐇤𐇤87.65",
            u"𐇤𐇤𐇤8.765",
            u"𐇤𐇤0.8765",
            u"𐇤0.08765",
            u"0.008765",
            u"𐇤𐇤𐇤𐇤𐇤𐇤𐇤0");

    assertFormatDescending(
            u"Padding with wide digits",
            NumberFormatter::with().padding(
                            Padder::codePoints(
                                    '*', 8, PadPosition::UNUM_PAD_AFTER_PREFIX))
                    .adoptSymbols(new NumberingSystem(MATHSANB)),
            Locale::getEnglish(),
            u"**𝟴𝟳,𝟲𝟱𝟬",
            u"***𝟴,𝟳𝟲𝟱",
            u"***𝟴𝟳𝟲.𝟱",
            u"***𝟴𝟳.𝟲𝟱",
            u"***𝟴.𝟳𝟲𝟱",
            u"**𝟬.𝟴𝟳𝟲𝟱",
            u"*𝟬.𝟬𝟴𝟳𝟲𝟱",
            u"𝟬.𝟬𝟬𝟴𝟳𝟲𝟱",
            u"*******𝟬");

    assertFormatDescending(
            u"Padding with currency spacing",
            NumberFormatter::with().padding(
                            Padder::codePoints(
                                    '*', 10, PadPosition::UNUM_PAD_AFTER_PREFIX))
                    .unit(GBP)
                    .unitWidth(UNumberUnitWidth::UNUM_UNIT_WIDTH_ISO_CODE),
            Locale::getEnglish(),
            u"GBP 87,650.00",
            u"GBP 8,765.00",
            u"GBP*876.50",
            u"GBP**87.65",
            u"GBP***8.76",
            u"GBP***0.88",
            u"GBP***0.09",
            u"GBP***0.01",
            u"GBP***0.00");

    assertFormatSingle(
            u"Pad Before Prefix",
            NumberFormatter::with().padding(
                    Padder::codePoints(
                            '*', 8, PadPosition::UNUM_PAD_BEFORE_PREFIX)),
            Locale::getEnglish(),
            -88.88,
            u"**-88.88");

    assertFormatSingle(
            u"Pad After Prefix",
            NumberFormatter::with().padding(
                    Padder::codePoints(
                            '*', 8, PadPosition::UNUM_PAD_AFTER_PREFIX)),
            Locale::getEnglish(),
            -88.88,
            u"-**88.88");

    assertFormatSingle(
            u"Pad Before Suffix",
            NumberFormatter::with().padding(
                    Padder::codePoints(
                            '*', 8, PadPosition::UNUM_PAD_BEFORE_SUFFIX)).unit(NoUnit::percent()),
            Locale::getEnglish(),
            88.88,
            u"88.88**%");

    assertFormatSingle(
            u"Pad After Suffix",
            NumberFormatter::with().padding(
                    Padder::codePoints(
                            '*', 8, PadPosition::UNUM_PAD_AFTER_SUFFIX)).unit(NoUnit::percent()),
            Locale::getEnglish(),
            88.88,
            u"88.88%**");

    assertFormatSingle(
            u"Currency Spacing with Zero Digit Padding Broken",
            NumberFormatter::with().padding(
                            Padder::codePoints(
                                    '0', 12, PadPosition::UNUM_PAD_AFTER_PREFIX))
                    .unit(GBP)
                    .unitWidth(UNumberUnitWidth::UNUM_UNIT_WIDTH_ISO_CODE),
            Locale::getEnglish(),
            514.23,
            u"GBP 000514.23"); // TODO: This is broken; it renders too wide (13 instead of 12).
}

void numbertest_api::integerWidth() {
    assertFormatDescending(
            u"Integer Width Default",
            NumberFormatter::with().integerWidth(IntegerWidth::zeroFillTo(1)),
            Locale::getEnglish(),
            u"87,650",
            u"8,765",
            u"876.5",
            u"87.65",
            u"8.765",
            u"0.8765",
            u"0.08765",
            u"0.008765",
            u"0");

    assertFormatDescending(
            u"Integer Width Zero Fill 0",
            NumberFormatter::with().integerWidth(IntegerWidth::zeroFillTo(0)),
            Locale::getEnglish(),
            u"87,650",
            u"8,765",
            u"876.5",
            u"87.65",
            u"8.765",
            u".8765",
            u".08765",
            u".008765",
            u""); // TODO: Avoid the empty string here?

    assertFormatDescending(
            u"Integer Width Zero Fill 3",
            NumberFormatter::with().integerWidth(IntegerWidth::zeroFillTo(3)),
            Locale::getEnglish(),
            u"87,650",
            u"8,765",
            u"876.5",
            u"087.65",
            u"008.765",
            u"000.8765",
            u"000.08765",
            u"000.008765",
            u"000");

    assertFormatDescending(
            u"Integer Width Max 3",
            NumberFormatter::with().integerWidth(IntegerWidth::zeroFillTo(1).truncateAt(3)),
            Locale::getEnglish(),
            u"650",
            u"765",
            u"876.5",
            u"87.65",
            u"8.765",
            u"0.8765",
            u"0.08765",
            u"0.008765",
            u"0");

    assertFormatDescending(
            u"Integer Width Fixed 2",
            NumberFormatter::with().integerWidth(IntegerWidth::zeroFillTo(2).truncateAt(2)),
            Locale::getEnglish(),
            u"50",
            u"65",
            u"76.5",
            u"87.65",
            u"08.765",
            u"00.8765",
            u"00.08765",
            u"00.008765",
            u"00");
}

void numbertest_api::symbols() {
    assertFormatDescending(
            u"French Symbols with Japanese Data 1",
            NumberFormatter::with().symbols(FRENCH_SYMBOLS),
            Locale::getJapan(),
            u"87 650",
            u"8 765",
            u"876,5",
            u"87,65",
            u"8,765",
            u"0,8765",
            u"0,08765",
            u"0,008765",
            u"0");

    assertFormatSingle(
            u"French Symbols with Japanese Data 2",
            NumberFormatter::with().notation(Notation::compactShort()).symbols(FRENCH_SYMBOLS),
            Locale::getJapan(),
            12345,
            u"1,2\u4E07");

    assertFormatDescending(
            u"Latin Numbering System with Arabic Data",
            NumberFormatter::with().adoptSymbols(new NumberingSystem(LATN)).unit(USD),
            Locale("ar"),
            u"US$ 87,650.00",
            u"US$ 8,765.00",
            u"US$ 876.50",
            u"US$ 87.65",
            u"US$ 8.76",
            u"US$ 0.88",
            u"US$ 0.09",
            u"US$ 0.01",
            u"US$ 0.00");

    assertFormatDescending(
            u"Math Numbering System with French Data",
            NumberFormatter::with().adoptSymbols(new NumberingSystem(MATHSANB)),
            Locale::getFrench(),
            u"𝟴𝟳 𝟲𝟱𝟬",
            u"𝟴 𝟳𝟲𝟱",
            u"𝟴𝟳𝟲,𝟱",
            u"𝟴𝟳,𝟲𝟱",
            u"𝟴,𝟳𝟲𝟱",
            u"𝟬,𝟴𝟳𝟲𝟱",
            u"𝟬,𝟬𝟴𝟳𝟲𝟱",
            u"𝟬,𝟬𝟬𝟴𝟳𝟲𝟱",
            u"𝟬");

    assertFormatSingle(
            u"Swiss Symbols (used in documentation)",
            NumberFormatter::with().symbols(SWISS_SYMBOLS),
            Locale::getEnglish(),
            12345.67,
            u"12’345.67");

    assertFormatSingle(
            u"Myanmar Symbols (used in documentation)",
            NumberFormatter::with().symbols(MYANMAR_SYMBOLS),
            Locale::getEnglish(),
            12345.67,
            u"\u1041\u1042,\u1043\u1044\u1045.\u1046\u1047");

    // NOTE: Locale ar puts ¤ after the number in NS arab but before the number in NS latn.

    assertFormatSingle(
            u"Currency symbol should precede number in ar with NS latn",
            NumberFormatter::with().adoptSymbols(new NumberingSystem(LATN)).unit(USD),
            Locale("ar"),
            12345.67,
            u"US$ 12,345.67");

    assertFormatSingle(
            u"Currency symbol should precede number in ar@numbers=latn",
            NumberFormatter::with().unit(USD),
            Locale("ar@numbers=latn"),
            12345.67,
            u"US$ 12,345.67");

    assertFormatSingle(
            u"Currency symbol should follow number in ar with NS arab",
            NumberFormatter::with().unit(USD),
            Locale("ar"),
            12345.67,
            u"١٢٬٣٤٥٫٦٧ US$");

    assertFormatSingle(
            u"Currency symbol should follow number in ar@numbers=arab",
            NumberFormatter::with().unit(USD),
            Locale("ar@numbers=arab"),
            12345.67,
            u"١٢٬٣٤٥٫٦٧ US$");

    UErrorCode status = U_ZERO_ERROR;
    DecimalFormatSymbols symbols = SWISS_SYMBOLS;
    UnlocalizedNumberFormatter f = NumberFormatter::with().symbols(symbols);
    symbols.setSymbol(DecimalFormatSymbols::ENumberFormatSymbol::kGroupingSeparatorSymbol, u"!", status);
    assertFormatSingle(
            u"Symbols object should be copied", f, Locale::getEnglish(), 12345.67, u"12’345.67");

    assertFormatSingle(
            u"The last symbols setter wins",
            NumberFormatter::with().symbols(symbols).adoptSymbols(new NumberingSystem(LATN)),
            Locale::getEnglish(),
            12345.67,
            u"12,345.67");

    assertFormatSingle(
            u"The last symbols setter wins",
            NumberFormatter::with().adoptSymbols(new NumberingSystem(LATN)).symbols(symbols),
            Locale::getEnglish(),
            12345.67,
            u"12!345.67");
}

// TODO: Enable if/when currency symbol override is added.
//void NumberFormatterTest::symbolsOverride() {
//    DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance(Locale::getEnglish());
//    dfs.setCurrencySymbol("@");
//    dfs.setInternationalCurrencySymbol("foo");
//    assertFormatSingle(
//            u"Custom Short Currency Symbol",
//            NumberFormatter::with().unit(Currency.getInstance("XXX")).symbols(dfs),
//            Locale::getEnglish(),
//            12.3,
//            u"@ 12.30");
//}

void numbertest_api::sign() {
    assertFormatSingle(
            u"Sign Auto Positive",
            NumberFormatter::with().sign(UNumberSignDisplay::UNUM_SIGN_AUTO),
            Locale::getEnglish(),
            444444,
            u"444,444");

    assertFormatSingle(
            u"Sign Auto Negative",
            NumberFormatter::with().sign(UNumberSignDisplay::UNUM_SIGN_AUTO),
            Locale::getEnglish(),
            -444444,
            u"-444,444");

    assertFormatSingle(
            u"Sign Always Positive",
            NumberFormatter::with().sign(UNumberSignDisplay::UNUM_SIGN_ALWAYS),
            Locale::getEnglish(),
            444444,
            u"+444,444");

    assertFormatSingle(
            u"Sign Always Negative",
            NumberFormatter::with().sign(UNumberSignDisplay::UNUM_SIGN_ALWAYS),
            Locale::getEnglish(),
            -444444,
            u"-444,444");

    assertFormatSingle(
            u"Sign Never Positive",
            NumberFormatter::with().sign(UNumberSignDisplay::UNUM_SIGN_NEVER),
            Locale::getEnglish(),
            444444,
            u"444,444");

    assertFormatSingle(
            u"Sign Never Negative",
            NumberFormatter::with().sign(UNumberSignDisplay::UNUM_SIGN_NEVER),
            Locale::getEnglish(),
            -444444,
            u"444,444");

    assertFormatSingle(
            u"Sign Accounting Positive",
            NumberFormatter::with().sign(UNumberSignDisplay::UNUM_SIGN_ACCOUNTING).unit(USD),
            Locale::getEnglish(),
            444444,
            u"$444,444.00");

    assertFormatSingle(
            u"Sign Accounting Negative",
            NumberFormatter::with().sign(UNumberSignDisplay::UNUM_SIGN_ACCOUNTING).unit(USD),
            Locale::getEnglish(),
            -444444,
            u"($444,444.00)");

    assertFormatSingle(
            u"Sign Accounting-Always Positive",
            NumberFormatter::with().sign(UNumberSignDisplay::UNUM_SIGN_ACCOUNTING_ALWAYS).unit(USD),
            Locale::getEnglish(),
            444444,
            u"+$444,444.00");

    assertFormatSingle(
            u"Sign Accounting-Always Negative",
            NumberFormatter::with().sign(UNumberSignDisplay::UNUM_SIGN_ACCOUNTING_ALWAYS).unit(USD),
            Locale::getEnglish(),
            -444444,
            u"($444,444.00)");

    assertFormatSingle(
            u"Sign Accounting Negative Hidden",
            NumberFormatter::with().sign(UNumberSignDisplay::UNUM_SIGN_ACCOUNTING)
                    .unit(USD)
                    .unitWidth(UNUM_UNIT_WIDTH_HIDDEN),
            Locale::getEnglish(),
            -444444,
            u"(444,444.00)");
}

void numbertest_api::decimal() {
    assertFormatDescending(
            u"Decimal Default",
            NumberFormatter::with().decimal(UNumberDecimalSeparatorDisplay::UNUM_DECIMAL_SEPARATOR_AUTO),
            Locale::getEnglish(),
            u"87,650",
            u"8,765",
            u"876.5",
            u"87.65",
            u"8.765",
            u"0.8765",
            u"0.08765",
            u"0.008765",
            u"0");

    assertFormatDescending(
            u"Decimal Always Shown",
            NumberFormatter::with().decimal(UNumberDecimalSeparatorDisplay::UNUM_DECIMAL_SEPARATOR_ALWAYS),
            Locale::getEnglish(),
            u"87,650.",
            u"8,765.",
            u"876.5",
            u"87.65",
            u"8.765",
            u"0.8765",
            u"0.08765",
            u"0.008765",
            u"0.");
}

void numbertest_api::locale() {
    // Coverage for the locale setters.
    UErrorCode status = U_ZERO_ERROR;
    UnicodeString actual = NumberFormatter::withLocale(Locale::getFrench()).formatInt(1234, status)
            .toString();
    assertEquals("Locale withLocale()", u"1 234", actual);
}

void numbertest_api::errors() {
    LocalizedNumberFormatter lnf = NumberFormatter::withLocale(Locale::getEnglish()).rounding(
            Rounder::fixedFraction(
                    -1));

    {
        UErrorCode status = U_ZERO_ERROR;
        lnf.formatInt(1, status);
        assertEquals(
                "Should fail with U_ILLEGAL_ARGUMENT_ERROR since rounder is not legal",
                U_ILLEGAL_ARGUMENT_ERROR,
                status);
    }

    {
        UErrorCode status = U_ZERO_ERROR;
        lnf.copyErrorTo(status);
        assertEquals(
                "Should fail with U_ILLEGAL_ARGUMENT_ERROR since rounder is not legal",
                U_ILLEGAL_ARGUMENT_ERROR,
                status);
    }
}


void numbertest_api::assertFormatDescending(const UnicodeString &message,
                                                 const UnlocalizedNumberFormatter &f,
                                                 const Locale &locale, ...) {
    va_list args;
    // TODO: Fix this? "warning: 'va_start' has undefined behavior with reference types [-Wvarargs]"
    va_start(args, locale);
    static double inputs[] = {87650, 8765, 876.5, 87.65, 8.765, 0.8765, 0.08765, 0.008765, 0};
    const LocalizedNumberFormatter l1 = f.threshold(0).locale(locale); // no self-regulation
    const LocalizedNumberFormatter l2 = f.threshold(1).locale(locale); // all self-regulation
    UErrorCode status = U_ZERO_ERROR;
    for (int16_t i = 0; i < 9; i++) {
        char16_t caseNumber = u'0' + i;
        double d = inputs[i];
        UnicodeString expected = va_arg(args, const char16_t*);
        UnicodeString actual1 = l1.formatDouble(d, status).toString();
        assertSuccess(message + u": Unsafe Path: " + caseNumber, status);
        assertEquals(message + u": Unsafe Path: " + caseNumber, expected, actual1);
        UnicodeString actual2 = l2.formatDouble(d, status).toString();
        assertSuccess(message + u": Safe Path: " + caseNumber, status);
        assertEquals(message + u": Safe Path: " + caseNumber, expected, actual2);
    }
}

void numbertest_api::assertFormatDescendingBig(const UnicodeString &message,
                                                    const UnlocalizedNumberFormatter &f,
                                                    const Locale &locale, ...) {
    va_list args;
    // TODO: Fix this? "warning: 'va_start' has undefined behavior with reference types [-Wvarargs]"
    va_start(args, locale);
    static double inputs[] = {87650000, 8765000, 876500, 87650, 8765, 876.5, 87.65, 8.765, 0};
    const LocalizedNumberFormatter l1 = f.threshold(0).locale(locale); // no self-regulation
    const LocalizedNumberFormatter l2 = f.threshold(1).locale(locale); // all self-regulation
    UErrorCode status = U_ZERO_ERROR;
    for (int16_t i = 0; i < 9; i++) {
        char16_t caseNumber = u'0' + i;
        double d = inputs[i];
        UnicodeString expected = va_arg(args, const char16_t*);
        UnicodeString actual1 = l1.formatDouble(d, status).toString();
        assertSuccess(message + u": Unsafe Path: " + caseNumber, status);
        assertEquals(message + u": Unsafe Path: " + caseNumber, expected, actual1);
        UnicodeString actual2 = l2.formatDouble(d, status).toString();
        assertSuccess(message + u": Safe Path: " + caseNumber, status);
        assertEquals(message + u": Safe Path: " + caseNumber, expected, actual2);
    }
}

void numbertest_api::assertFormatSingle(const UnicodeString &message,
                                             const UnlocalizedNumberFormatter &f, const Locale &locale,
                                             double input, const UnicodeString &expected) {
    const LocalizedNumberFormatter l1 = f.threshold(0).locale(locale); // no self-regulation
    const LocalizedNumberFormatter l2 = f.threshold(1).locale(locale); // all self-regulation
    UErrorCode status = U_ZERO_ERROR;
    UnicodeString actual1 = l1.formatDouble(input, status).toString();
    assertSuccess(message + u": Unsafe Path", status);
    assertEquals(message + u": Unsafe Path", expected, actual1);
    UnicodeString actual2 = l2.formatDouble(input, status).toString();
    assertSuccess(message + u": Safe Path", status);
    assertEquals(message + u": Safe Path", expected, actual2);
}

int main() {
    UErrorCode status = U_ZERO_ERROR;
    numbertest_api test(status);
    test.notationSimple();
    test.notationScientific();
    test.notationCompact();
    test.unitMeasure();
    test.unitCurrency();
    test.unitPercent();
    test.roundingFraction();
    test.roundingFigures();
    test.roundingFractionFigures();
    test.roundingOther();
    test.grouping();
    test.padding();
    test.integerWidth();
    test.symbols();
    test.sign();
    test.decimal();
    test.locale();
    test.errors();

    u_cleanup();
}
