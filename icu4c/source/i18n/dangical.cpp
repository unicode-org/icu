// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 ******************************************************************************
 * Copyright (C) 2013, International Business Machines Corporation
 * and others. All Rights Reserved.
 ******************************************************************************
 *
 * File DANGICAL.CPP
 *****************************************************************************
 */

#include "chnsecal.h"
#include "dangical.h"

#if !UCONFIG_NO_FORMATTING

#include "astro.h" // CalendarCache
#include "gregoimp.h" // Math
#include "uassert.h"
#include "ucln_in.h"
#include "umutex.h"
#include "unicode/gregocal.h" // GregorianCalendar for post-1896 (gregorian)
#include "unicode/rbtz.h"
#include "unicode/tzrule.h"

// --- The cache --
// Lazy Creation & Access synchronized by class CalendarCache with a mutex.
static icu::CalendarCache *gWinterSolsticeCache = nullptr;
static icu::CalendarCache *gNewYearCache = nullptr;

// gAstronomerTimeZone
static icu::TimeZone *gAstronomerTimeZone = nullptr;
static icu::UInitOnce gAstronomerTimeZoneInitOnce {};

/**
 * The start year of the Korean traditional calendar (Dan-gi) is the inaugural
 * year of Dan-gun (BC 2333).
 */
static const int32_t DANGI_EPOCH_YEAR = -2332; // Gregorian year
static const int32_t DANGI_YEAR_OFFSET = 1 - DANGI_EPOCH_YEAR;

/***
 * The cutover date from lunar calendar to solar calendar is 1896-01-01 (Gregorian).
 * as part of the calendar migration in Korea, part of Gabo reform.
 * Imperial Directive: https://sillok.history.go.kr/id/kza_13209009_001
 *
 * Due to this, the calendar needs to handle gregorian date switchover from
 * 4228-11-17 (lunar) to 4229-01-01 (solar), as national calendar have migrated.
 *
 * Dangi was at least utilized as Gregorian calendar base since 1948-09-25 (Gregorian),
 * and national migration to gregorian calendar was done by deducting 2333 years from
 * existing documents as specified in "Era Act" at 1962-01-01 (Gregorian)
 *
 * Migration to Dangi:
 * https://www.law.go.kr/LSW//lsInfoP.do?lsiSeq=4301&ancYd=19480925
 *
 * Migration to Gregorian (Proof of Dangi was using Gregorian calendar as base):
 * https://www.law.go.kr/LSW//lsInfoP.do?lsiSeq=4302&ancYd=19611202
 */
static const int32_t GREGORIAN_CUTOVER_YEAR = 1896;

// - solar -
static const int32_t SOLAR_CUTOVER_YEAR = GREGORIAN_CUTOVER_YEAR + DANGI_YEAR_OFFSET;
static const int32_t SOLAR_CUTOVER_MONTH = UCAL_JANUARY;
static const int32_t SOLAR_CUTOVER_DAY = 1;
// - lunar -
static const int32_t LUNAR_CUTOVER_YEAR = SOLAR_CUTOVER_YEAR - 1;
static const int32_t LUNAR_CUTOVER_MONTH = UCAL_NOVEMBER;
// - magic -
static const int32_t GREGORIAN_CUTOVER_JULIAN_DAY =
    static_cast<int32_t>(
        icu::Grego::fieldsToDay(GREGORIAN_CUTOVER_YEAR, SOLAR_CUTOVER_MONTH, SOLAR_CUTOVER_DAY))
        + kEpochStartAsJulianDay;
static const int32_t LUNAR_CUTOVER_JULIAN_DAY = GREGORIAN_CUTOVER_JULIAN_DAY;

U_CDECL_BEGIN
static UBool calendar_dangi_cleanup() {
    if (gWinterSolsticeCache) {
        delete gWinterSolsticeCache;
        gWinterSolsticeCache = nullptr;
    }
    if (gNewYearCache) {
        delete gNewYearCache;
        gNewYearCache = nullptr;
    }

    if (gAstronomerTimeZone) {
        delete gAstronomerTimeZone;
        gAstronomerTimeZone = nullptr;
    }
    gAstronomerTimeZoneInitOnce.reset();
    return true;
}
U_CDECL_END

U_NAMESPACE_BEGIN

// Implementation of the DangiCalendar class

//-------------------------------------------------------------------------
// Constructors...
//-------------------------------------------------------------------------

const TimeZone* getAstronomerTimeZone(UErrorCode &status);

DangiCalendar::DangiCalendar(const Locale& aLocale, UErrorCode& success)
:   ChineseCalendar(aLocale, success)
{
}

DangiCalendar::DangiCalendar (const DangiCalendar& other)
: ChineseCalendar(other)
{
}

DangiCalendar::~DangiCalendar()
{
}

DangiCalendar*
DangiCalendar::clone() const
{
    return new DangiCalendar(*this);
}

const char *DangiCalendar::getType() const {
    return "dangi";
}

/**
 * The time zone used for performing astronomical computations for
 * Dangi calendar. In Korea various timezones have been used historically 
 * (cf. http://www.math.snu.ac.kr/~kye/others/lunar.html): 
 *  
 *            - 1908/04/01: GMT+8 
 * 1908/04/01 - 1911/12/31: GMT+8.5 
 * 1912/01/01 - 1954/03/20: GMT+9 
 * 1954/03/21 - 1961/08/09: GMT+8.5 
 * 1961/08/10 -           : GMT+9 
 *  
 * Note that, in 1908-1911, the government did not apply the timezone change 
 * but used GMT+8. In addition, 1954-1961's timezone change does not affect 
 * the lunar date calculation. Therefore, the following simpler rule works: 
 *   
 * -1911: GMT+8 
 * 1912-: GMT+9 
 *  
 * Unfortunately, our astronomer's approximation doesn't agree with the 
 * references (http://www.math.snu.ac.kr/~kye/others/lunar.html and 
 * http://astro.kasi.re.kr/Life/ConvertSolarLunarForm.aspx?MenuID=115) 
 * in 1897/7/30. So the following ad hoc fix is used here: 
 *  
 *     -1896: GMT+8 
 *      1897: GMT+7 
 * 1898-1911: GMT+8 
 * 1912-    : GMT+9 
 */
static void U_CALLCONV initAstronomerTimeZone(UErrorCode &status) {
    U_ASSERT(gAstronomerTimeZone == nullptr);
    const UDate millis1897[] = { static_cast<UDate>((1897 - 1970) * 365 * kOneDay) }; // some days of error is not a problem here
    const UDate millis1898[] = { static_cast<UDate>((1898 - 1970) * 365 * kOneDay) }; // some days of error is not a problem here
    const UDate millis1912[] = { static_cast<UDate>((1912 - 1970) * 365 * kOneDay) }; // this doesn't create an issue for 1911/12/20
    LocalPointer<InitialTimeZoneRule> initialTimeZone(new InitialTimeZoneRule(
        UnicodeString(u"GMT+8"), 8*kOneHour, 0), status);

    LocalPointer<TimeZoneRule> rule1897(new TimeArrayTimeZoneRule(
        UnicodeString(u"Korean 1897"), 7*kOneHour, 0, millis1897, 1, DateTimeRule::STANDARD_TIME), status);

    LocalPointer<TimeZoneRule> rule1898to1911(new TimeArrayTimeZoneRule(
        UnicodeString(u"Korean 1898-1911"), 8*kOneHour, 0, millis1898, 1, DateTimeRule::STANDARD_TIME), status);

    LocalPointer<TimeZoneRule> ruleFrom1912(new TimeArrayTimeZoneRule(
        UnicodeString(u"Korean 1912-"), 9*kOneHour, 0, millis1912, 1, DateTimeRule::STANDARD_TIME), status);

    LocalPointer<RuleBasedTimeZone> zone(new RuleBasedTimeZone(
        UnicodeString(u"KOREA_ZONE"), initialTimeZone.orphan()), status); // adopts initialTimeZone

    if (U_FAILURE(status)) {
        return;
    }
    zone->addTransitionRule(rule1897.orphan(), status); // adopts rule1897
    zone->addTransitionRule(rule1898to1911.orphan(), status);
    zone->addTransitionRule(ruleFrom1912.orphan(), status);
    zone->complete(status);
    if (U_SUCCESS(status)) {
        gAstronomerTimeZone = zone.orphan();
    }
    ucln_i18n_registerCleanup(UCLN_I18N_DANGI_CALENDAR, calendar_dangi_cleanup);
}

const TimeZone* getAstronomerTimeZone(UErrorCode &status) {
    umtx_initOnce(gAstronomerTimeZoneInitOnce, &initAstronomerTimeZone, status);
    return gAstronomerTimeZone;
}

ChineseCalendar::Setting DangiCalendar::getSetting(UErrorCode& status) const {
  return {
    getAstronomerTimeZone(status),
    &gWinterSolsticeCache, &gNewYearCache
  };
}

int32_t DangiCalendar::handleGetExtendedYear(UErrorCode& status) {
    if (U_FAILURE(status)) {
        return 0;
    }

    // Dangi exposes continuous Dan-gi years, not Chinese cycle years.
    if (newerField(UCAL_EXTENDED_YEAR, UCAL_YEAR) == UCAL_EXTENDED_YEAR) {
        return internalGet(UCAL_EXTENDED_YEAR, 1970 + DANGI_YEAR_OFFSET);
    }
    return internalGet(UCAL_YEAR, 1970 + DANGI_YEAR_OFFSET);
}

UBool DangiCalendar::useGregorianSolarFields(UCalendarDateFields bestField, UErrorCode& status) {
    if (U_FAILURE(status)) {
        return false;
    }
    if (isSet(UCAL_IS_LEAP_MONTH) && internalGet(UCAL_IS_LEAP_MONTH) != 0) {
        return false;
    }

    int32_t dangiYear;
    if (bestField == UCAL_WEEK_OF_YEAR &&
            newerField(UCAL_YEAR_WOY, UCAL_YEAR) == UCAL_YEAR_WOY) {
        dangiYear = internalGet(UCAL_YEAR_WOY);
    } else {
        dangiYear = handleGetExtendedYear(status);
        if (U_FAILURE(status)) {
            return false;
        }
    }

    if (dangiYear >= SOLAR_CUTOVER_YEAR) {
        return true;
    }
    return false;
}

void DangiCalendar::handleComputeFields(int32_t julianDay, UErrorCode& status) {
    if (U_FAILURE(status)) {
        return;
    }
    // For dates after the cutover, compute fields based on the
    // proleptic Gregorian calendar, and then adjust the year and era fields.
    if (julianDay >= GREGORIAN_CUTOVER_JULIAN_DAY) {
        int32_t gregorianYear = getGregorianYear();
        int32_t dangiYear;
        if (uprv_add32_overflow(gregorianYear, DANGI_YEAR_OFFSET, &dangiYear)) {
            status = U_ILLEGAL_ARGUMENT_ERROR;
            return;
        }
        internalSet(UCAL_ERA, 0);
        internalSet(UCAL_YEAR, dangiYear);
        internalSet(UCAL_EXTENDED_YEAR, dangiYear);
        internalSet(UCAL_MONTH, getGregorianMonth());
        internalSet(UCAL_ORDINAL_MONTH, getGregorianMonth());
        internalSet(UCAL_DAY_OF_MONTH, getGregorianDayOfMonth());
        internalSet(UCAL_DAY_OF_YEAR, getGregorianDayOfYear());
        internalSet(UCAL_IS_LEAP_MONTH, 0);
        return;
    }

    // For dates before the cutover, compute fields using the lunar calendar.
    ChineseCalendar::handleComputeFields(julianDay, status);
    if (U_FAILURE(status)) {
        return;
    }

    int32_t dangiYear;
    if (uprv_add32_overflow(internalGet(UCAL_EXTENDED_YEAR), DANGI_YEAR_OFFSET, &dangiYear)) {
        status = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }
    internalSet(UCAL_ERA, 0);
    internalSet(UCAL_YEAR, dangiYear);
    internalSet(UCAL_EXTENDED_YEAR, dangiYear);
}

//------------------------------------------------------------------
// Dangi Cutover handling
//------------------------------------------------------------------
namespace {

void normalizeMonth(int32_t& year, int32_t& month, UErrorCode& status) {
    if (U_FAILURE(status)) {
        return;
    }
    if (month < UCAL_JANUARY || month > UCAL_DECEMBER) {
        int32_t delta = ClockMath::floorDivide(month, 12, &month);
        if (uprv_add32_overflow(year, delta, &year)) {
            status = U_ILLEGAL_ARGUMENT_ERROR;
        }
    }
}

UBool isSolarMonth(int32_t dangiYear, int32_t month) {
    return dangiYear > SOLAR_CUTOVER_YEAR ||
        (dangiYear == SOLAR_CUTOVER_YEAR && month >= SOLAR_CUTOVER_MONTH);
}

UBool isLunarCutoverMonthOrLater(int32_t dangiYear, int32_t month) {
    return dangiYear > LUNAR_CUTOVER_YEAR ||
        (dangiYear == LUNAR_CUTOVER_YEAR && month >= LUNAR_CUTOVER_MONTH);
}

UBool isSolarDate(const DangiCalendar& calendar, UErrorCode& status) {
    if (U_FAILURE(status)) {
        return false;
    }
    return calendar.get(UCAL_JULIAN_DAY, status) >= GREGORIAN_CUTOVER_JULIAN_DAY;
}

void setSolarMonth(DangiCalendar& calendar, int32_t dangiYear, int32_t month,
                   int32_t dayOfMonth, UErrorCode& status) {
    if (U_FAILURE(status)) {
        return;
    }
    int32_t gregorianYear;
    if (uprv_add32_overflow(dangiYear, -DANGI_YEAR_OFFSET, &gregorianYear)) {
        status = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }
    int32_t pinnedDay = uprv_min(dayOfMonth, Grego::monthLength(gregorianYear, month));
    int64_t julianDay = Grego::fieldsToDay(gregorianYear, month, pinnedDay) +
        kEpochStartAsJulianDay;
    if (julianDay < INT32_MIN || julianDay > INT32_MAX) {
        status = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }
    calendar.set(UCAL_JULIAN_DAY, static_cast<int32_t>(julianDay));
}

}  // namespace

int32_t DangiCalendar::lunarToGregorianYear(int32_t dangiYear, UErrorCode& status) const {
    if (U_FAILURE(status)) {
        return 0;
    }

    int32_t gregorianYear;
    if (uprv_add32_overflow(dangiYear, -DANGI_YEAR_OFFSET, &gregorianYear)) {
        status = U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }
    return gregorianYear;
}

int32_t DangiCalendar::handleComputeJulianDay(
        UCalendarDateFields bestField, UErrorCode& status) {
    if (U_FAILURE(status)) {
        return 0;
    }

    UBool oldUseGregorianSolar = fUseGregorianSolar;
    fUseGregorianSolar = useGregorianSolarFields(bestField, status);
    int32_t julianDay = ChineseCalendar::handleComputeJulianDay(bestField, status);
    if (U_FAILURE(status)) {
        fUseGregorianSolar = oldUseGregorianSolar;
        return 0;
    }

    // If solar field resolution lands before the cutover, retry on the lunar side.
    if (fUseGregorianSolar && julianDay < GREGORIAN_CUTOVER_JULIAN_DAY) {
        fUseGregorianSolar = false;
        julianDay = ChineseCalendar::handleComputeJulianDay(bestField, status);
    }
    if (!fUseGregorianSolar && U_SUCCESS(status)) {
        int32_t dangiYear = handleGetExtendedYear(status);
        int32_t defaultMonth = getDefaultMonthInYear(dangiYear, status);
        int32_t month = internalGetMonth(defaultMonth, status);
        if (U_SUCCESS(status) && isLunarCutoverMonthOrLater(dangiYear, month) &&
                julianDay >= LUNAR_CUTOVER_JULIAN_DAY) {
            // The lunar cutover label canonicalizes to the solar cutover instant.
            julianDay += GREGORIAN_CUTOVER_JULIAN_DAY - LUNAR_CUTOVER_JULIAN_DAY;
        }
    }

    fUseGregorianSolar = oldUseGregorianSolar;
    return U_SUCCESS(status) ? julianDay : 0;
}

int64_t DangiCalendar::handleComputeMonthStart(
        int32_t eyear, int32_t month, UBool useMonth, UErrorCode& status) const {
    if (U_FAILURE(status)) {
        return 0;
    }
    int32_t normalizedYear = eyear;
    int32_t normalizedMonth = useMonth ? month : 0;
    normalizeMonth(normalizedYear, normalizedMonth, status);
    if (U_FAILURE(status)) {
        return 0;
    }
    if (fUseGregorianSolar) {
        int32_t gregorianYear;
        if (uprv_add32_overflow(normalizedYear, -DANGI_YEAR_OFFSET, &gregorianYear)) {
            status = U_ILLEGAL_ARGUMENT_ERROR;
            return 0;
        }
        return Grego::fieldsToDay(gregorianYear, normalizedMonth, 1) +
            kEpochStartAsJulianDay - 1;
    }
    return ChineseCalendar::handleComputeMonthStart(
        lunarToGregorianYear(eyear, status), month, useMonth, status);
}

int32_t DangiCalendar::handleGetMonthLength(
        int32_t extendedYear, int32_t month, UErrorCode& status) const {
    int32_t normalizedYear = extendedYear;
    int32_t normalizedMonth = month;
    normalizeMonth(normalizedYear, normalizedMonth, status);
    if (U_FAILURE(status)) {
        return 0;
    }
    if (isSolarMonth(normalizedYear, normalizedMonth)) {
        int32_t gregorianYear;
        if (uprv_add32_overflow(normalizedYear, -DANGI_YEAR_OFFSET, &gregorianYear)) {
            status = U_ILLEGAL_ARGUMENT_ERROR;
            return 0;
        }
        return Grego::monthLength(gregorianYear, normalizedMonth);
    }
    return ChineseCalendar::handleGetMonthLength(
        lunarToGregorianYear(extendedYear, status), month, status);
}

int32_t DangiCalendar::handleGetYearLength(int32_t eyear, UErrorCode& status) const {
    if (U_FAILURE(status)) {
        return 0;
    }
    if (eyear >= SOLAR_CUTOVER_YEAR) {
        int32_t gregorianYear;
        if (uprv_add32_overflow(eyear, -DANGI_YEAR_OFFSET, &gregorianYear)) {
            status = U_ILLEGAL_ARGUMENT_ERROR;
            return 0;
        }
        int32_t nextGregorianYear;
        if (uprv_add32_overflow(gregorianYear, 1, &nextGregorianYear)) {
            status = U_ILLEGAL_ARGUMENT_ERROR;
            return 0;
        }
        return static_cast<int32_t>(
            Grego::fieldsToDay(nextGregorianYear, UCAL_JANUARY, 1) -
            Grego::fieldsToDay(gregorianYear, UCAL_JANUARY, 1));
    }
    return ChineseCalendar::handleGetYearLength(lunarToGregorianYear(eyear, status), status);
}

int32_t DangiCalendar::handleGetLimit(UCalendarDateFields field, ELimitType limitType) const {
    // fetch from GregorianCalendar for fields
    auto gregorianLimit = [field, limitType]() -> int32_t {
        UErrorCode status = U_ZERO_ERROR;
        GregorianCalendar gregorian(*TimeZone::getGMT(), Locale::getRoot(), status);
        if (U_FAILURE(status)) {
            return 0;
        }
        return limitType == UCAL_LIMIT_LEAST_MAXIMUM ?
            gregorian.getLeastMaximum(field) : gregorian.getMaximum(field);
    };

    switch (field) {
    case UCAL_ERA:
        return 0;
    case UCAL_YEAR:
        return ChineseCalendar::handleGetLimit(UCAL_EXTENDED_YEAR, limitType);
    case UCAL_DAY_OF_MONTH:
        if (limitType == UCAL_LIMIT_LEAST_MAXIMUM) {
            return gregorianLimit();
        }
        if (limitType == UCAL_LIMIT_MAXIMUM) {
            return gregorianLimit();
        }
        return ChineseCalendar::handleGetLimit(field, limitType);
    case UCAL_DAY_OF_WEEK_IN_MONTH:
        if (limitType == UCAL_LIMIT_LEAST_MAXIMUM) {
            return gregorianLimit();
        }
        if (limitType == UCAL_LIMIT_MAXIMUM) {
            return gregorianLimit();
        }
        return ChineseCalendar::handleGetLimit(field, limitType);
    default:
        return ChineseCalendar::handleGetLimit(field, limitType);
    }
}

int32_t DangiCalendar::getDefaultDayInMonth(
        int32_t eyear, int32_t month, UErrorCode& status) {
    if (U_FAILURE(status)) {
        return 0;
    }
    if (eyear == SOLAR_CUTOVER_YEAR && month == SOLAR_CUTOVER_MONTH) {
        return SOLAR_CUTOVER_DAY;
    }
    return ChineseCalendar::getDefaultDayInMonth(eyear, month, status);
}

int32_t DangiCalendar::getRelatedYearDifference() const {
    return DANGI_EPOCH_YEAR - 1;
}

bool DangiCalendar::inTemporalLeapYear(UErrorCode& status) const {
    if (isSolarDate(*this, status)) {
        int32_t days = getActualMaximum(UCAL_DAY_OF_YEAR, status);
        return U_SUCCESS(status) && days == 366;
    }
    return ChineseCalendar::inTemporalLeapYear(status);
}

void DangiCalendar::add(UCalendarDateFields field, int32_t amount, UErrorCode& status) {
    if (amount != 0 &&
        (field == UCAL_MONTH || field == UCAL_ORDINAL_MONTH) &&
        isSolarDate(*this, status)) {
        int32_t month = get(UCAL_MONTH, status);
        int32_t dangiYear = get(UCAL_EXTENDED_YEAR, status);
        int32_t dayOfMonth = get(UCAL_DAY_OF_MONTH, status);
        int32_t gregorianYear;
        if (uprv_add32_overflow(dangiYear, -DANGI_YEAR_OFFSET, &gregorianYear)) {
            status = U_ILLEGAL_ARGUMENT_ERROR;
            return;
        }
        int32_t newMonth;
        if (uprv_add32_overflow(month, amount, &newMonth)) {
            status = U_ILLEGAL_ARGUMENT_ERROR;
            return;
        }
        normalizeMonth(gregorianYear, newMonth, status);
        if (U_FAILURE(status)) {
            return;
        }
        int32_t newDangiYear;
        if (uprv_add32_overflow(gregorianYear, DANGI_YEAR_OFFSET, &newDangiYear)) {
            status = U_ILLEGAL_ARGUMENT_ERROR;
            return;
        }
        setSolarMonth(*this, newDangiYear, newMonth, dayOfMonth, status);
        return;
    }
    ChineseCalendar::add(field, amount, status);
}

void DangiCalendar::add(EDateFields field, int32_t amount, UErrorCode& status) {
    add(static_cast<UCalendarDateFields>(field), amount, status);
}

void DangiCalendar::roll(UCalendarDateFields field, int32_t amount, UErrorCode& status) {
    if (amount != 0 &&
        (field == UCAL_MONTH || field == UCAL_ORDINAL_MONTH) &&
        isSolarDate(*this, status)) {
        int32_t month = get(UCAL_MONTH, status);
        int32_t dangiYear = get(UCAL_EXTENDED_YEAR, status);
        int32_t dayOfMonth = get(UCAL_DAY_OF_MONTH, status);
        int32_t newMonth = (month + amount) % 12;
        if (newMonth < 0) {
            newMonth += 12;
        }
        setSolarMonth(*this, dangiYear, newMonth, dayOfMonth, status);
        return;
    }
    ChineseCalendar::roll(field, amount, status);
}

void DangiCalendar::roll(EDateFields field, int32_t amount, UErrorCode& status) {
    roll(static_cast<UCalendarDateFields>(field), amount, status);
}

int32_t DangiCalendar::getActualMaximum(UCalendarDateFields field, UErrorCode& status) const {
    if (U_FAILURE(status)) {
       return 0;
    }
    if (field == UCAL_DATE) {
        LocalPointer<DangiCalendar> cal(clone(), status);
        if (U_FAILURE(status)) {
            return 0;
        }
        cal->setLenient(true);
        cal->prepareGetActual(field, false, status);
        int32_t year = cal->get(UCAL_EXTENDED_YEAR, status);
        int32_t month = cal->get(UCAL_MONTH, status);
        return cal->handleGetMonthLength(year, month, status);
    }
    if (field == UCAL_ORDINAL_MONTH && isSolarDate(*this, status)) {
        return UCAL_DECEMBER;
    }
    return ChineseCalendar::getActualMaximum(field, status);
}

UOBJECT_DEFINE_RTTI_IMPLEMENTATION(DangiCalendar)

U_NAMESPACE_END

#endif
