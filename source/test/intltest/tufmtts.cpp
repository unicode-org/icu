/********************************************************************
 * Copyright (c) 2008-2013, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/tmunit.h"
#include "unicode/tmutamt.h"
#include "unicode/tmutfmt.h"
#include "tufmtts.h"
#include "unicode/timeperiod.h"
#include "unicode/ustring.h"

//TODO: put as compilation flag
//#define TUFMTTS_DEBUG 1

#ifdef TUFMTTS_DEBUG
#include <iostream>
#endif

struct TimePeriodResult {
    TimePeriod timePeriod;
    const char* result;
};

class TimeUnitAmountSubClass : public TimeUnitAmount {
  public:
    TimeUnitAmountSubClass(double amount, TimeUnit::UTimeUnitFields timeUnitField, int ex, UErrorCode &status) : TimeUnitAmount(amount, timeUnitField, status), extra(ex) { }

    TimeUnitAmountSubClass(const TimeUnitAmountSubClass &that)
    : TimeUnitAmount(that), extra(that.extra) { }

    TimeUnitAmountSubClass &operator=(const TimeUnitAmountSubClass &that) {
      TimeUnitAmount::operator=(that);
      extra = that.extra;
      return *this;
    }

    virtual UObject* clone() const {
      return new TimeUnitAmountSubClass(*this);
    }

    virtual ~TimeUnitAmountSubClass() { }
    int extra;
};

static TimePeriod create1m59_9996s(UErrorCode &status);
static TimePeriod create19m(UErrorCode &status);
static TimePeriod create19m28s(UErrorCode &status);
static TimePeriod create19m29s(UErrorCode &status);
static TimePeriod create1h23_5s(UErrorCode &status);
static TimePeriod create1h23s(UErrorCode &status);
static TimePeriod create1h23_5m(UErrorCode &status);
static TimePeriod create1h0m23s(UErrorCode &status);
static TimePeriod create5h17m(UErrorCode &status);
static TimePeriod create2y5M3w4d(UErrorCode &status);
static TimePeriod create0h0m17s(UErrorCode &status);
static TimePeriod create6h56_92m(UErrorCode &status);

void TimeUnitTest::runIndexedTest( int32_t index, UBool exec, const char* &name, char* /*par*/ ) {
    if (exec) logln("TestSuite TimeUnitTest");
    switch (index) {
        TESTCASE(0, testBasic);
        TESTCASE(1, testAPI);
        TESTCASE(2, testGreekWithFallback);
        TESTCASE(3, testGreekWithSanitization);
        TESTCASE(4, testFormatPeriodEn);
        TESTCASE(5, testTimePeriodForAmounts);
        TESTCASE(6, testTimeUnitAmountSubClass);
        TESTCASE(7, testTimePeriodEquals);
        TESTCASE(8, testTimePeriodLength);
        default: name = ""; break;
    }
}

/**
 * Test basic
 */
void TimeUnitTest::testBasic() {
    const char* locales[] = {"en", "sl", "fr", "zh", "ar", "ru", "zh_Hant", "pa"};
    for ( unsigned int locIndex = 0; 
          locIndex < sizeof(locales)/sizeof(locales[0]); 
          ++locIndex ) {
        UErrorCode status = U_ZERO_ERROR;
        Locale loc(locales[locIndex]);
        TimeUnitFormat** formats = new TimeUnitFormat*[2];
        formats[UTMUTFMT_FULL_STYLE] = new TimeUnitFormat(loc, status);
        if (!assertSuccess("TimeUnitFormat(full)", status, TRUE)) return;
        formats[UTMUTFMT_ABBREVIATED_STYLE] = new TimeUnitFormat(loc, UTMUTFMT_ABBREVIATED_STYLE, status);
        if (!assertSuccess("TimeUnitFormat(short)", status)) return;
#ifdef TUFMTTS_DEBUG
        std::cout << "locale: " << locales[locIndex] << "\n";
#endif
        for (int style = UTMUTFMT_FULL_STYLE; 
             style <= UTMUTFMT_ABBREVIATED_STYLE;
             ++style) {
          for (TimeUnit::UTimeUnitFields j = TimeUnit::UTIMEUNIT_YEAR; 
             j < TimeUnit::UTIMEUNIT_FIELD_COUNT; 
             j = (TimeUnit::UTimeUnitFields)(j+1)) {
#ifdef TUFMTTS_DEBUG
            std::cout << "time unit: " << j << "\n";
#endif
            double tests[] = {0, 0.5, 1, 1.5, 2, 2.5, 3, 3.5, 5, 10, 100, 101.35};
            for (unsigned int i = 0; i < sizeof(tests)/sizeof(tests[0]); ++i) {
#ifdef TUFMTTS_DEBUG
                std::cout << "number: " << tests[i] << "\n";
#endif
                TimeUnitAmount* source = new TimeUnitAmount(tests[i], j, status);
                if (!assertSuccess("TimeUnitAmount()", status)) return;
                UnicodeString formatted;
                Formattable formattable;
                formattable.adoptObject(source);
                formatted = ((Format*)formats[style])->format(formattable, formatted, status);
                if (!assertSuccess("format()", status)) return;
#ifdef TUFMTTS_DEBUG
                char formatResult[1000];
                formatted.extract(0, formatted.length(), formatResult, "UTF-8");
                std::cout << "format result: " << formatResult << "\n";
#endif
                Formattable result;
                ((Format*)formats[style])->parseObject(formatted, result, status);
                if (!assertSuccess("parseObject()", status)) return;
                if (result != formattable) {
                    dataerrln("No round trip: ");
                }
                // other style parsing
                Formattable result_1;
                ((Format*)formats[1-style])->parseObject(formatted, result_1, status);
                if (!assertSuccess("parseObject()", status)) return;
                if (result_1 != formattable) {
                    dataerrln("No round trip: ");
                }
            }
          }
        }
        delete formats[UTMUTFMT_FULL_STYLE];
        delete formats[UTMUTFMT_ABBREVIATED_STYLE];
        delete[] formats;
    }
}


void TimeUnitTest::testAPI() {
    //================= TimeUnit =================
    UErrorCode status = U_ZERO_ERROR;

    TimeUnit* tmunit = TimeUnit::createInstance(TimeUnit::UTIMEUNIT_YEAR, status);
    if (!assertSuccess("TimeUnit::createInstance", status)) return;

    TimeUnit* another = (TimeUnit*)tmunit->clone();
    TimeUnit third(*tmunit);
    TimeUnit fourth = third;

    assertTrue("orig and clone are equal", (*tmunit == *another));
    assertTrue("copied and assigned are equal", (third == fourth));

    TimeUnit* tmunit_m = TimeUnit::createInstance(TimeUnit::UTIMEUNIT_MONTH, status);
    assertTrue("year != month", (*tmunit != *tmunit_m));

    TimeUnit::UTimeUnitFields field = tmunit_m->getTimeUnitField();
    assertTrue("field of month time unit is month", (field == TimeUnit::UTIMEUNIT_MONTH));
    
    delete tmunit;
    delete another;
    delete tmunit_m;
    //
    //================= TimeUnitAmount =================

    Formattable formattable((int32_t)2);
    TimeUnitAmount tma_long(formattable, TimeUnit::UTIMEUNIT_DAY, status);
    if (!assertSuccess("TimeUnitAmount(formattable...)", status)) return;

    formattable.setDouble(2);
    TimeUnitAmount tma_double(formattable, TimeUnit::UTIMEUNIT_DAY, status);
    if (!assertSuccess("TimeUnitAmount(formattable...)", status)) return;

    formattable.setDouble(3);
    TimeUnitAmount tma_double_3(formattable, TimeUnit::UTIMEUNIT_DAY, status);
    if (!assertSuccess("TimeUnitAmount(formattable...)", status)) return;

    TimeUnitAmount tma(2, TimeUnit::UTIMEUNIT_DAY, status);
    if (!assertSuccess("TimeUnitAmount(number...)", status)) return;

    TimeUnitAmount tma_h(2, TimeUnit::UTIMEUNIT_HOUR, status);
    if (!assertSuccess("TimeUnitAmount(number...)", status)) return;

    TimeUnitAmount second(tma);
    TimeUnitAmount third_tma = tma;
    TimeUnitAmount* fourth_tma = (TimeUnitAmount*)tma.clone();

    assertTrue("orig and copy are equal", (second == tma));
    assertTrue("clone and assigned are equal", (third_tma == *fourth_tma));
    assertTrue("different if number diff", (tma_double != tma_double_3));
    assertTrue("different if number type diff", (tma_double != tma_long));
    assertTrue("different if time unit diff", (tma != tma_h));
    assertTrue("same even different constructor", (tma_double == tma));

    assertTrue("getTimeUnitField", (tma.getTimeUnitField() == TimeUnit::UTIMEUNIT_DAY));
    delete fourth_tma;
    //
    //================= TimeUnitFormat =================
    //
    TimeUnitFormat* tmf_en = new TimeUnitFormat(Locale("en"), status);
    if (!assertSuccess("TimeUnitFormat(en...)", status, TRUE)) return;
    TimeUnitFormat tmf_fr(Locale("fr"), status);
    if (!assertSuccess("TimeUnitFormat(fr...)", status)) return;

    assertTrue("TimeUnitFormat: en and fr diff", (*tmf_en != tmf_fr));

    TimeUnitFormat tmf_assign = *tmf_en;
    assertTrue("TimeUnitFormat: orig and assign are equal", (*tmf_en == tmf_assign));

    TimeUnitFormat tmf_copy(tmf_fr);
    assertTrue("TimeUnitFormat: orig and copy are equal", (tmf_fr == tmf_copy));

    TimeUnitFormat* tmf_clone = (TimeUnitFormat*)tmf_en->clone();
    assertTrue("TimeUnitFormat: orig and clone are equal", (*tmf_en == *tmf_clone));
    delete tmf_clone;

    tmf_en->setLocale(Locale("fr"), status);
    if (!assertSuccess("setLocale(fr...)", status)) return;

    NumberFormat* numberFmt = NumberFormat::createInstance(
                                 Locale("fr"), status);
    if (!assertSuccess("NumberFormat::createInstance()", status)) return;
    tmf_en->setNumberFormat(*numberFmt, status);
    if (!assertSuccess("setNumberFormat(en...)", status)) return;
    assertTrue("TimeUnitFormat: setLocale", (*tmf_en == tmf_fr));

    delete tmf_en;

    TimeUnitFormat* en_long = new TimeUnitFormat(Locale("en"), UTMUTFMT_FULL_STYLE, status);
    if (!assertSuccess("TimeUnitFormat(en...)", status)) return;
    delete en_long;

    TimeUnitFormat* en_short = new TimeUnitFormat(Locale("en"), UTMUTFMT_ABBREVIATED_STYLE, status);
    if (!assertSuccess("TimeUnitFormat(en...)", status)) return;
    delete en_short;

    TimeUnitFormat* format = new TimeUnitFormat(status);
    format->setLocale(Locale("zh"), status);
    format->setNumberFormat(*numberFmt, status);
    if (!assertSuccess("TimeUnitFormat(en...)", status)) return;
    delete numberFmt;
    delete format;
}

/* @bug 7902
 * Tests for Greek Language.
 * This tests that requests for short unit names correctly fall back 
 * to long unit names for a locale where the locale data does not 
 * provide short unit names. As of CLDR 1.9, Greek is one such language.
 */
void TimeUnitTest::testGreekWithFallback() {
    UErrorCode status = U_ZERO_ERROR;

    const char* locales[] = {"el-GR", "el"};
    TimeUnit::UTimeUnitFields tunits[] = {TimeUnit::UTIMEUNIT_SECOND, TimeUnit::UTIMEUNIT_MINUTE, TimeUnit::UTIMEUNIT_HOUR, TimeUnit::UTIMEUNIT_DAY, TimeUnit::UTIMEUNIT_MONTH, TimeUnit::UTIMEUNIT_YEAR};
    UTimeUnitFormatStyle styles[] = {UTMUTFMT_FULL_STYLE, UTMUTFMT_ABBREVIATED_STYLE};
    const int numbers[] = {1, 7};

    const UChar oneSecond[] = {0x0031, 0x0020, 0x03b4, 0x03b5, 0x03c5, 0x03c4, 0x03b5, 0x03c1, 0x03cc, 0x03bb, 0x03b5, 0x03c0, 0x03c4, 0x03bf, 0};
    const UChar oneMinute[] = {0x0031, 0x0020, 0x03bb, 0x03b5, 0x03c0, 0x03c4, 0x03cc, 0};
    const UChar oneHour[] = {0x0031, 0x0020, 0x03ce, 0x03c1, 0x03b1, 0};
    const UChar oneDay[] = {0x0031, 0x0020, 0x03b7, 0x03bc, 0x03ad, 0x03c1, 0x03b1, 0};
    const UChar oneMonth[] = {0x0031, 0x0020, 0x03bc, 0x03ae, 0x03bd, 0x03b1, 0x03c2, 0};
    const UChar oneYear[] = {0x0031, 0x0020, 0x03ad, 0x03c4, 0x03bf, 0x03c2, 0};
    const UChar sevenSeconds[] = {0x0037, 0x0020, 0x03b4, 0x03b5, 0x03c5, 0x03c4, 0x03b5, 0x03c1, 0x03cc, 0x03bb, 0x03b5, 0x03c0, 0x03c4, 0x03b1, 0};
    const UChar sevenMinutes[] = {0x0037, 0x0020, 0x03bb, 0x03b5, 0x03c0, 0x03c4, 0x03ac, 0};
    const UChar sevenHours[] = {0x0037, 0x0020, 0x03ce, 0x03c1, 0x03b5, 0x03c2, 0};
    const UChar sevenDays[] = {0x0037, 0x0020, 0x03b7, 0x03bc, 0x03ad, 0x03c1, 0x03b5, 0x03c2, 0};
    const UChar sevenMonths[] = {0x0037, 0x0020, 0x03bc, 0x03ae, 0x03bd, 0x03b5, 0x3c2, 0};
    const UChar sevenYears[] = {0x0037, 0x0020, 0x03ad, 0x03c4, 0x03b7, 0};

    const UnicodeString oneSecondStr(oneSecond);
    const UnicodeString oneMinuteStr(oneMinute);
    const UnicodeString oneHourStr(oneHour);
    const UnicodeString oneDayStr(oneDay);
    const UnicodeString oneMonthStr(oneMonth);
    const UnicodeString oneYearStr(oneYear);
    const UnicodeString sevenSecondsStr(sevenSeconds);
    const UnicodeString sevenMinutesStr(sevenMinutes);
    const UnicodeString sevenHoursStr(sevenHours);
    const UnicodeString sevenDaysStr(sevenDays);
    const UnicodeString sevenMonthsStr(sevenMonths);
    const UnicodeString sevenYearsStr(sevenYears);

    const UnicodeString expected[] = {oneSecondStr, oneMinuteStr, oneHourStr, oneDayStr, oneMonthStr, oneYearStr,
                              oneSecondStr, oneMinuteStr, oneHourStr, oneDayStr, oneMonthStr, oneYearStr,
                              sevenSecondsStr, sevenMinutesStr, sevenHoursStr, sevenDaysStr, sevenMonthsStr, sevenYearsStr,
                              sevenSecondsStr, sevenMinutesStr, sevenHoursStr, sevenDaysStr, sevenMonthsStr, sevenYearsStr,
                              oneSecondStr, oneMinuteStr, oneHourStr, oneDayStr, oneMonthStr, oneYearStr,
                              oneSecondStr, oneMinuteStr, oneHourStr, oneDayStr, oneMonthStr, oneYearStr,
                              sevenSecondsStr, sevenMinutesStr, sevenHoursStr, sevenDaysStr, sevenMonthsStr, sevenYearsStr,
                              sevenSecondsStr, sevenMinutesStr, sevenHoursStr, sevenDaysStr, sevenMonthsStr, sevenYearsStr};

    int counter = 0;
    for ( unsigned int locIndex = 0;
        locIndex < sizeof(locales)/sizeof(locales[0]);
        ++locIndex ) {

        Locale l = Locale::createFromName(locales[locIndex]);

        for ( unsigned int numberIndex = 0;
            numberIndex < sizeof(numbers)/sizeof(int);
            ++numberIndex ) {

            for ( unsigned int styleIndex = 0;
                styleIndex < sizeof(styles)/sizeof(styles[0]);
                ++styleIndex ) {

                for ( unsigned int unitIndex = 0;
                    unitIndex < sizeof(tunits)/sizeof(tunits[0]);
                    ++unitIndex ) {

                    TimeUnitAmount *tamt = new TimeUnitAmount(numbers[numberIndex], tunits[unitIndex], status);
                    if (U_FAILURE(status)) {
                        dataerrln("generating TimeUnitAmount Object failed.");
#ifdef TUFMTTS_DEBUG
                        std::cout << "Failed to get TimeUnitAmount for " << tunits[unitIndex] << "\n";
#endif
                        return;
                    }

                    TimeUnitFormat *tfmt = new TimeUnitFormat(l, styles[styleIndex], status);
                    if (U_FAILURE(status)) {
                        dataerrln("generating TimeUnitAmount Object failed.");
#ifdef TUFMTTS_DEBUG
                       std::cout <<  "Failed to get TimeUnitFormat for " << locales[locIndex] << "\n";
#endif
                       return;
                    }

                    Formattable fmt;
                    UnicodeString str;

                    fmt.adoptObject(tamt);
                    str = ((Format *)tfmt)->format(fmt, str, status);
                    if (!assertSuccess("formatting relative time failed", status)) {
                        delete tfmt;
#ifdef TUFMTTS_DEBUG
                        std::cout <<  "Failed to format" << "\n";
#endif
                        return;
                    }

#ifdef TUFMTTS_DEBUG
                    char tmp[128];    //output
                    char tmp1[128];    //expected
                    int len = 0;
                    u_strToUTF8(tmp, 128, &len, str.getTerminatedBuffer(), str.length(), &status);
                    u_strToUTF8(tmp1, 128, &len, expected[counter].unescape().getTerminatedBuffer(), expected[counter].unescape().length(), &status);
                    std::cout <<  "Formatted string : " << tmp << " expected : " << tmp1 << "\n";
#endif
                    if (!assertEquals("formatted time string is not expected, locale: " + UnicodeString(locales[locIndex]) + " style: " + (int)styles[styleIndex] + " units: " + (int)tunits[unitIndex], expected[counter], str)) {
                        delete tfmt;
                        str.remove();
                        return;
                    }
                    delete tfmt;
                    str.remove();
                    ++counter;
                }
            }
        }
    }
}

// Test bug9042
void TimeUnitTest::testGreekWithSanitization() {
    
    UErrorCode status = U_ZERO_ERROR;
    Locale elLoc("el");
    NumberFormat* numberFmt = NumberFormat::createInstance(Locale("el"), status);
    if (!assertSuccess("NumberFormat::createInstance for el locale", status, TRUE)) return;
    numberFmt->setMaximumFractionDigits(1);

    TimeUnitFormat* timeUnitFormat = new TimeUnitFormat(elLoc, status);
    if (!assertSuccess("TimeUnitFormat::TimeUnitFormat for el locale", status)) return;

    timeUnitFormat->setNumberFormat(*numberFmt, status);

    delete numberFmt;
    delete timeUnitFormat;
}

void TimeUnitTest::testFormatPeriodEn() {
    UErrorCode status = U_ZERO_ERROR;

    TimePeriodResult fullResults[] = {
        {create1m59_9996s(status), "1 minute, 59.9996 seconds"},
        {create19m(status), "19 minutes"},
        {create1h23_5s(status), "1 hour, 23.5 seconds"},
        {create1h23_5m(status), "1 hour, 23.5 minutes"},
        {create1h0m23s(status), "1 hour, 0 minutes, 23 seconds"},
        {create2y5M3w4d(status), "2 years, 5 months, 3 weeks, 4 days"}};
  
    TimePeriodResult abbrevResults[] = {
        {create1m59_9996s(status), "1 min, 59.9996 secs"},
        {create19m(status), "19 mins"},
        {create1h23_5s(status), "1 hr, 23.5 secs"},
        {create1h23_5m(status), "1 hr, 23.5 mins"},
        {create1h0m23s(status), "1 hr, 0 mins, 23 secs"},
        {create2y5M3w4d(status), "2 yrs, 5 mths, 3 wks, 4 days"}};
  
    TimePeriodResult numericResults[] = {
        {create1m59_9996s(status), "1:59.9996"},
        {create19m(status), "19 mins"},
        {create1h23_5s(status), "1:00:23.5"},
        {create1h0m23s(status), "1:00:23"},
        {create5h17m(status), "5:17"},
        {create19m28s(status), "19:28"},
        {create2y5M3w4d(status), "2 yrs, 5 mths, 3 wks, 4 days"},
        {create0h0m17s(status), "0:00:17"},
        {create6h56_92m(status), "6:56.92"}};

   if (U_FAILURE(status)) {
      dataerrln("Unable to create time periods - %s", u_errorName(status));
      return;
    }

    LocalPointer<NumberFormat> nf(NumberFormat::createInstance(Locale::getEnglish(), status));
    if (U_FAILURE(status)) {
        dataerrln("Unable to create NumberFormat object - %s", u_errorName(status));
        return;
    }
    nf->setMaximumFractionDigits(4);
    {
        TimeUnitFormat tuf(Locale::getEnglish(), UTMUTFMT_FULL_STYLE, status);
        tuf.setNumberFormat(*nf, status);
        if (U_FAILURE(status)) {
            dataerrln("Unable to create TimeUnitFormat object - %s", u_errorName(status));
            return;
        }
        verifyFormatTimePeriod(
            tuf,
            fullResults,
            sizeof(fullResults) / sizeof(TimePeriodResult));
    }
    {
        TimeUnitFormat tuf(Locale::getEnglish(), UTMUTFMT_ABBREVIATED_STYLE, status);
        tuf.setNumberFormat(*nf, status);
        if (U_FAILURE(status)) {
            dataerrln("Unable to create TimeUnitFormat object - %s", u_errorName(status));
            return;
        }
        verifyFormatTimePeriod(
            tuf,
            abbrevResults,
            sizeof(abbrevResults) / sizeof(TimePeriodResult));
    }
    {
        TimeUnitFormat tuf(Locale::getEnglish(), UTMUTFMT_NUMERIC_STYLE, status);
        tuf.setNumberFormat(*nf, status);
        if (U_FAILURE(status)) {
            dataerrln("Unable to create TimeUnitFormat object - %s", u_errorName(status));
            return;
        }
        verifyFormatTimePeriod(
            tuf,
            numericResults,
            sizeof(numericResults) / sizeof(TimePeriodResult));
    }
}

void TimeUnitTest::testTimePeriodLength() {
   UErrorCode status = U_ZERO_ERROR;
   int32_t actual = create1h23_5m(status).length();
    if (U_FAILURE(status)) {
      dataerrln("Unable to create time period object - %s", u_errorName(status));
      return;
    }
   if (actual != 2) {
       errln("Expected 2, got %d", actual);
    }
}

void TimeUnitTest::testTimePeriodForAmounts() {
    UErrorCode status = U_ZERO_ERROR;
    TimeUnitAmount _5h(5.0, TimeUnit::UTIMEUNIT_HOUR, status);
    TimeUnitAmount _3_5h(3.5, TimeUnit::UTIMEUNIT_HOUR, status);
    TimeUnitAmount _3h(3.0, TimeUnit::UTIMEUNIT_HOUR, status);
    TimeUnitAmount _5m(5.0, TimeUnit::UTIMEUNIT_MINUTE, status);
    if (U_FAILURE(status)) {
      dataerrln("Unable to alocate time unit amounts - %s", u_errorName(status));
      return;
    }
    {
        UErrorCode status = U_ZERO_ERROR;
        TimeUnitAmount *amounts[] = {&_3h, &_5h};
        int32_t len = sizeof(amounts) / sizeof(TimeUnitAmount*);
        TimePeriod(amounts, len, status);
        if (status != U_ILLEGAL_ARGUMENT_ERROR) {
            errln("Expected U_ILLEGAL_ARGUMENT_ERROR for 3h + 5h, got %s", u_errorName(status));
        }
    } 
    {
        UErrorCode status = U_ZERO_ERROR;
        TimePeriod(NULL, 0, status);
        if (status != U_ILLEGAL_ARGUMENT_ERROR) {
            errln("Expected U_ILLEGAL_ARGUMENT_ERROR for empty time period, got %s", u_errorName(status));
        }
    } 
    {
        UErrorCode status = U_ZERO_ERROR;
        TimeUnitAmount *amounts[] = {&_3_5h, &_5m};
        int32_t len = sizeof(amounts) / sizeof(TimeUnitAmount*);
        TimePeriod(amounts, len, status);
        if (status != U_ILLEGAL_ARGUMENT_ERROR) {
            errln("Expected U_ILLEGAL_ARGUMENT_ERROR for 3.5h + 5m, got %s", u_errorName(status));
        }
    } 
}

void TimeUnitTest::testTimePeriodEquals() {
    UErrorCode status = U_ZERO_ERROR;

    TimePeriod _1h23s = create1h23s(status);

    // Same variable
    verifyEquals(_1h23s, _1h23s);

    // Different variables same value
    verifyEquals(_1h23s, TimePeriod(_1h23s));

    // Different fields
    verifyNotEqual(_1h23s, create1h0m23s(status));

    // Same fields different values
    verifyNotEqual(create19m28s(status), create19m29s(status));

    if (U_FAILURE(status)) {
        errln("Failure creating TimePeriods, got %s", u_errorName(status));
    }
}

void TimeUnitTest::testTimeUnitAmountSubClass() {
    UErrorCode status = U_ZERO_ERROR;
    TimeUnitAmountSubClass _6h(6.0, TimeUnit::UTIMEUNIT_HOUR, 1, status);
    TimeUnitAmountSubClass _5m(5.0, TimeUnit::UTIMEUNIT_MINUTE, 2, status);
    if (U_FAILURE(status)) {
      dataerrln("Unable to alocate time unit amounts - %s", u_errorName(status));
      return;
    }
    {
        UErrorCode status = U_ZERO_ERROR;
        TimeUnitAmount *amounts[] = {&_6h, &_5m};
        int32_t len = sizeof(amounts) / sizeof(TimeUnitAmount*);
        TimePeriod period(amounts, len, status);
        if (2 != ((const TimeUnitAmountSubClass *) period.getAmount(TimeUnit::UTIMEUNIT_MINUTE))->extra) {
            errln("Expected polymorphic behavior.");
        }
    } 
}

void TimeUnitTest::verifyFormatTimePeriod(
        const TimeUnitFormat& tuf,
        const TimePeriodResult* timePeriodResults,
        int32_t numResults) {
    for (int32_t i = 0; i < numResults; i++) {
        UnicodeString expected(timePeriodResults[i].result, -1, US_INV);
        expected = expected.unescape();
        UErrorCode status = U_ZERO_ERROR;
        Formattable formattable(new TimePeriod(timePeriodResults[i].timePeriod));
        UnicodeString actual;
        FieldPosition pos(0);
        tuf.format(formattable, actual, pos, status);
        if (U_FAILURE(status)) {
            dataerrln("Unable to format time period - %s", u_errorName(status));
            return;
        }
        if (actual != expected) {
            errln(UnicodeString("Fail: Expected: ") + expected
                + UnicodeString(" Got: ") + actual);
        }
    }
}

void TimeUnitTest::verifyEquals(const TimePeriod& lhs, const TimePeriod& rhs) {
  if (lhs != rhs) {
    errln("Expected equal.");
    return;
  }
  if (!(lhs == rhs)) {
    errln("Expected not not equal.");
  }
}

void TimeUnitTest::verifyNotEqual(const TimePeriod& lhs, const TimePeriod& rhs) {
  if (lhs == rhs) {
    errln("Expected not equal.");
    return;
  }
  if (!(lhs != rhs)) {
    errln("Expected not not not equal.");
  }
}

static TimePeriod create1m59_9996s(UErrorCode &status) {
  TimeUnitAmount minutes(1.0, TimeUnit::UTIMEUNIT_MINUTE, status);
  TimeUnitAmount seconds(59.9996, TimeUnit::UTIMEUNIT_SECOND, status);
  TimeUnitAmount *amounts[] = {&minutes, &seconds};
  int32_t len = sizeof(amounts) / sizeof(TimeUnitAmount*);
  return TimePeriod(amounts, len, status); 
}

static TimePeriod create19m(UErrorCode &status) {
  TimeUnitAmount minutes(19.0, TimeUnit::UTIMEUNIT_MINUTE, status);
  TimeUnitAmount *amounts[] = {&minutes};
  int32_t len = sizeof(amounts) / sizeof(TimeUnitAmount*);
  return TimePeriod(amounts, len, status); 
}

static TimePeriod create19m28s(UErrorCode &status) {
  TimeUnitAmount minutes(19.0, TimeUnit::UTIMEUNIT_MINUTE, status);
  TimeUnitAmount seconds(28.0, TimeUnit::UTIMEUNIT_SECOND, status);
  TimeUnitAmount *amounts[] = {&minutes, &seconds};
  int32_t len = sizeof(amounts) / sizeof(TimeUnitAmount*);
  return TimePeriod(amounts, len, status); 
}

static TimePeriod create19m29s(UErrorCode &status) {
  TimeUnitAmount minutes(19.0, TimeUnit::UTIMEUNIT_MINUTE, status);
  TimeUnitAmount seconds(29.0, TimeUnit::UTIMEUNIT_SECOND, status);
  TimeUnitAmount *amounts[] = {&minutes, &seconds};
  int32_t len = sizeof(amounts) / sizeof(TimeUnitAmount*);
  return TimePeriod(amounts, len, status); 
}

static TimePeriod create1h23_5s(UErrorCode &status) {
  TimeUnitAmount hours(1.0, TimeUnit::UTIMEUNIT_HOUR, status);
  TimeUnitAmount seconds(23.5, TimeUnit::UTIMEUNIT_SECOND, status);
  TimeUnitAmount *amounts[] = {&hours, &seconds};
  int32_t len = sizeof(amounts) / sizeof(TimeUnitAmount*);
  return TimePeriod(amounts, len, status); 
}

static TimePeriod create1h23_5m(UErrorCode &status) {
  TimeUnitAmount hours(1.0, TimeUnit::UTIMEUNIT_HOUR, status);
  TimeUnitAmount seconds(23.5, TimeUnit::UTIMEUNIT_MINUTE, status);
  TimeUnitAmount *amounts[] = {&hours, &seconds};
  int32_t len = sizeof(amounts) / sizeof(TimeUnitAmount*);
  return TimePeriod(amounts, len, status); 
}

static TimePeriod create1h0m23s(UErrorCode &status) {
  TimeUnitAmount hours(1.0, TimeUnit::UTIMEUNIT_HOUR, status);
  TimeUnitAmount minutes(0.0, TimeUnit::UTIMEUNIT_MINUTE, status);
  TimeUnitAmount seconds(23.0, TimeUnit::UTIMEUNIT_SECOND, status);
  TimeUnitAmount *amounts[] = {&hours, &minutes, &seconds};
  int32_t len = sizeof(amounts) / sizeof(TimeUnitAmount*);
  return TimePeriod(amounts, len, status); 
}

static TimePeriod create1h23s(UErrorCode &status) {
  TimeUnitAmount hours(1.0, TimeUnit::UTIMEUNIT_HOUR, status);
  TimeUnitAmount seconds(23.0, TimeUnit::UTIMEUNIT_SECOND, status);
  TimeUnitAmount *amounts[] = {&hours, &seconds};
  int32_t len = sizeof(amounts) / sizeof(TimeUnitAmount*);
  return TimePeriod(amounts, len, status); 
}

static TimePeriod create5h17m(UErrorCode &status) {
  TimeUnitAmount hours(5.0, TimeUnit::UTIMEUNIT_HOUR, status);
  TimeUnitAmount minutes(17.0, TimeUnit::UTIMEUNIT_MINUTE, status);
  TimeUnitAmount *amounts[] = {&hours, &minutes};
  int32_t len = sizeof(amounts) / sizeof(TimeUnitAmount*);
  return TimePeriod(amounts, len, status); 
}

static TimePeriod create2y5M3w4d(UErrorCode &status) {
  TimeUnitAmount years(2.0, TimeUnit::UTIMEUNIT_YEAR, status);
  TimeUnitAmount months(5.0, TimeUnit::UTIMEUNIT_MONTH, status);
  TimeUnitAmount weeks(3.0, TimeUnit::UTIMEUNIT_WEEK, status);
  TimeUnitAmount days(4.0, TimeUnit::UTIMEUNIT_DAY, status);
  TimeUnitAmount *amounts[] = {&years, &months, &weeks, &days};
  int32_t len = sizeof(amounts) / sizeof(TimeUnitAmount*);
  return TimePeriod(amounts, len, status); 
}

static TimePeriod create0h0m17s(UErrorCode &status) {
  TimeUnitAmount hours(0.0, TimeUnit::UTIMEUNIT_HOUR, status);
  TimeUnitAmount minutes(0.0, TimeUnit::UTIMEUNIT_MINUTE, status);
  TimeUnitAmount seconds(17.0, TimeUnit::UTIMEUNIT_SECOND, status);
  TimeUnitAmount *amounts[] = {&hours, &minutes, &seconds};
  int32_t len = sizeof(amounts) / sizeof(TimeUnitAmount*);
  return TimePeriod(amounts, len, status); 
}

static TimePeriod create6h56_92m(UErrorCode &status) {
  TimeUnitAmount hours(6.0, TimeUnit::UTIMEUNIT_HOUR, status);
  TimeUnitAmount minutes(56.92, TimeUnit::UTIMEUNIT_MINUTE, status);
  TimeUnitAmount *amounts[] = {&hours, &minutes};
  int32_t len = sizeof(amounts) / sizeof(TimeUnitAmount*);
  return TimePeriod(amounts, len, status); 
}

#endif
