/*
**********************************************************************
* Copyright (c) 2004-2014, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: Alan Liu
* Created: April 26, 2004
* Since: ICU 3.0
**********************************************************************
*/
#include "utypeinfo.h" // for 'typeid' to work

#include "unicode/measunit.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/uenum.h"
#include "ustrenum.h"
#include "cstring.h"
#include "uassert.h"

U_NAMESPACE_BEGIN

UOBJECT_DEFINE_RTTI_IMPLEMENTATION(MeasureUnit)

// Start generated code

static const int32_t gOffsets[] = {
    0,      // acceleration
    2,      // angle
    6,      // area
    15,     // consumption
    17,     // currency
    277,    // digital
    287,    // duration
    297,    // electric
    301,    // energy
    307,    // frequency
    311,    // length
    329,    // light
    330,    // mass
    341,    // power
    347,    // pressure
    352,    // proportion
    353,    // speed
    356,    // temperature
    359,    // volume
    381     // total in gSubTypes
};

static const int32_t gIndexes[] = {
    0,
    2,
    6,
    15,
    17,
    17,     // from here, offsets less the number of currencies (260)
    27,
    37,
    41,
    47,
    51,
    69,
    70,
    81,
    87,
    92,
    93,
    96,
    99,
    121     // total in gSubTypes less number of currencies, should match MEAS_UNIT_COUNT in measfmt.cpp
};

static const char * const gTypes[] = {
    "acceleration",
    "angle",
    "area",
    "consumption",
    "currency",
    "digital",
    "duration",
    "electric",
    "energy",
    "frequency",
    "length",
    "light",
    "mass",
    "power",
    "pressure",
    "proportion",
    "speed",
    "temperature",
    "volume"
};

static const char * const gSubTypes[] = {
    "g-force",                  //  0 (was 0) acceleration/0
    "meter-per-second-squared",
    "arc-minute",               //  2 (was 1) angle/1
    "arc-second",
    "degree",
    "radian",
    "acre",                     //  6 (was 4) area/2
    "hectare",
    "square-centimeter",
    "square-foot",
    "square-inch",
    "square-kilometer",
    "square-meter",
    "square-mile",
    "square-yard",
    "liter-per-kilometer",      // 15 (new) consumption/3
    "mile-per-gallon",
    "ADP",                      // 17 (was 10) currency/4
    "AED",
    "AFA",
    "AFN",
    "ALL",
    "AMD",
    "ANG",
    "AOA",
    "AON",
    "AOR",
    "ARA",
    "ARP",
    "ARS",
    "ATS",
    "AUD",
    "AWG",
    "AYM",
    "AZM",
    "AZN",
    "BAD",
    "BAM",
    "BBD",
    "BDT",
    "BEC",
    "BEF",
    "BEL",
    "BGL",
    "BGN",
    "BHD",
    "BIF",
    "BMD",
    "BND",
    "BOB",
    "BOV",
    "BRC",
    "BRE",
    "BRL",
    "BRN",
    "BRR",
    "BSD",
    "BTN",
    "BWP",
    "BYB",
    "BYR",
    "BZD",
    "CAD",
    "CDF",
    "CHC",
    "CHE",
    "CHF",
    "CHW",
    "CLF",
    "CLP",
    "CNY",
    "COP",
    "COU",
    "CRC",
    "CSD",
    "CSK",
    "CUC",
    "CUP",
    "CVE",
    "CYP",
    "CZK",
    "DDM",
    "DEM",
    "DJF",
    "DKK",
    "DOP",
    "DZD",
    "ECS",
    "ECV",
    "EEK",
    "EGP",
    "ERN",
    "ESA",
    "ESB",
    "ESP",
    "ETB",
    "EUR",
    "FIM",
    "FJD",
    "FKP",
    "FRF",
    "GBP",
    "GEK",
    "GEL",
    "GHC",
    "GHP",
    "GHS",
    "GIP",
    "GMD",
    "GNF",
    "GQE",
    "GRD",
    "GTQ",
    "GWP",
    "GYD",
    "HKD",
    "HNL",
    "HRD",
    "HRK",
    "HTG",
    "HUF",
    "IDR",
    "IEP",
    "ILS",
    "INR",
    "IQD",
    "IRR",
    "ISK",
    "ITL",
    "JMD",
    "JOD",
    "JPY",
    "KES",
    "KGS",
    "KHR",
    "KMF",
    "KPW",
    "KRW",
    "KWD",
    "KYD",
    "KZT",
    "LAK",
    "LBP",
    "LKR",
    "LRD",
    "LSL",
    "LTL",
    "LTT",
    "LUC",
    "LUF",
    "LUL",
    "LVL",
    "LVR",
    "LYD",
    "MAD",
    "MDL",
    "MGA",
    "MGF",
    "MKD",
    "MLF",
    "MMK",
    "MNT",
    "MOP",
    "MRO",
    "MTL",
    "MUR",
    "MVR",
    "MWK",
    "MXN",
    "MXV",
    "MYR",
    "MZM",
    "MZN",
    "NAD",
    "NGN",
    "NIO",
    "NLG",
    "NOK",
    "NPR",
    "NZD",
    "OMR",
    "PAB",
    "PEI",
    "PEN",
    "PES",
    "PGK",
    "PHP",
    "PKR",
    "PLN",
    "PLZ",
    "PTE",
    "PYG",
    "QAR",
    "ROL",
    "RON",
    "RSD",
    "RUB",
    "RUR",
    "RWF",
    "SAR",
    "SBD",
    "SCR",
    "SDD",
    "SDG",
    "SEK",
    "SGD",
    "SHP",
    "SIT",
    "SKK",
    "SLL",
    "SOS",
    "SRD",
    "SRG",
    "SSP",
    "STD",
    "SVC",
    "SYP",
    "SZL",
    "THB",
    "TJR",
    "TJS",
    "TMM",
    "TMT",
    "TND",
    "TOP",
    "TPE",
    "TRL",
    "TRY",
    "TTD",
    "TWD",
    "TZS",
    "UAH",
    "UAK",
    "UGX",
    "USD",
    "USN",
    "USS",
    "UYI",
    "UYU",
    "UZS",
    "VEB",
    "VEF",
    "VND",
    "VUV",
    "WST",
    "XAF",
    "XAG",
    "XAU",
    "XBA",
    "XBB",
    "XBC",
    "XBD",
    "XCD",
    "XDR",
    "XEU",
    "XOF",
    "XPD",
    "XPF",
    "XPT",
    "XSU",
    "XTS",
    "XUA",
    "XXX",
    "YDD",
    "YER",
    "YUM",
    "YUN",
    "ZAL",
    "ZAR",
    "ZMK",
    "ZMW",
    "ZRN",
    "ZRZ",
    "ZWD",
    "ZWL",
    "ZWN",
    "ZWR",
    "bit",                      // 277 (new) digital/5
    "byte",
    "gigabit",
    "gigabyte",
    "kilobit",
    "kilobyte",
    "megabit",
    "megabyte",
    "terabit",
    "terabyte",
    "day",                      // 287 (was 270) duration/6
    "hour",
    "microsecond",
    "millisecond",
    "minute",
    "month",
    "nanosecond",
    "second",
    "week",
    "year",
    "ampere",                   // 297 (new) electric/7
    "milliampere",
    "ohm",
    "volt",
    "calorie",                  // 301 (new) energy/8
    "foodcalorie",
    "joule",
    "kilocalorie",
    "kilojoule",
    "kilowatt-hour",
    "gigahertz",                // 307 (new) frequency/9
    "hertz",
    "kilohertz",
    "megahertz",
    "astronomical-unit",        // 311 (was 278) length/10
    "centimeter",
    "decimeter",
    "fathom",
    "foot",
    "furlong",
    "inch",
    "kilometer",
    "light-year",
    "meter",
    "micrometer",
    "mile",
    "millimeter",
    "nanometer",
    "nautical-mile",
    "parsec",
    "picometer",
    "yard",
    "lux",                      // 329 (new) light/11
    "carat",                    // 330 (was 288) mass/12
    "gram",
    "kilogram",
    "metric-ton",
    "microgram",
    "milligram",
    "ounce",
    "ounce-troy",
    "pound",
    "stone",
    "ton",
    "gigawatt",                 // 341 (was 292) power/13
    "horsepower",
    "kilowatt",
    "megawatt",
    "milliwatt",
    "watt",
    "hectopascal",              // 347 (was 295) pressure/14
    "inch-hg",
    "millibar",
    "millimeter-of-mercury",
    "pound-per-square-inch",
    "karat",                    // 352 (new) proportion/15
    "kilometer-per-hour",       // 353 (was 298) speed/16
    "meter-per-second",
    "mile-per-hour",
    "celsius",                  // 356 (was 301) temperature/17
    "fahrenheit",
    "kelvin",
    "acre-foot",                // 359 (was 303) volume/18
    "bushel",
    "centiliter",
    "cubic-centimeter",
    "cubic-foot",
    "cubic-inch",
    "cubic-kilometer",
    "cubic-meter",
    "cubic-mile",
    "cubic-yard",
    "cup",
    "deciliter",
    "fluid-ounce",
    "gallon",
    "hectoliter",
    "liter",
    "megaliter",
    "milliliter",
    "pint",
    "quart",
    "tablespoon",
    "teaspoon"
};

MeasureUnit *MeasureUnit::createGForce(UErrorCode &status) {
    return MeasureUnit::create(0, 0, status);
}

// missing "meter-per-second-squared", MeasureUnit::create(0, 1, status);

MeasureUnit *MeasureUnit::createArcMinute(UErrorCode &status) {
    return MeasureUnit::create(1, 0, status);
}

MeasureUnit *MeasureUnit::createArcSecond(UErrorCode &status) {
    return MeasureUnit::create(1, 1, status);
}

MeasureUnit *MeasureUnit::createDegree(UErrorCode &status) {
    return MeasureUnit::create(1, 2, status);
}

// missing "radian",        MeasureUnit::create(1, 3, status);

MeasureUnit *MeasureUnit::createAcre(UErrorCode &status) {
    return MeasureUnit::create(2, 0, status);
}

MeasureUnit *MeasureUnit::createHectare(UErrorCode &status) {
    return MeasureUnit::create(2, 1, status);
}

// missing "square-centimeter", MeasureUnit::create(2, 2, status);

MeasureUnit *MeasureUnit::createSquareFoot(UErrorCode &status) {
    return MeasureUnit::create(2, 3, status);
}

// missing "square-inch",   MeasureUnit::create(2, 4, status);

MeasureUnit *MeasureUnit::createSquareKilometer(UErrorCode &status) {
    return MeasureUnit::create(2, 5, status);
}

MeasureUnit *MeasureUnit::createSquareMeter(UErrorCode &status) {
    return MeasureUnit::create(2, 6, status);
}

MeasureUnit *MeasureUnit::createSquareMile(UErrorCode &status) {
    return MeasureUnit::create(2, 7, status);
}

// missing "square-yard",   MeasureUnit::create(2, 8, status);

// missing "liter-per-kilometer", MeasureUnit::create(3, 0, status);
// missing "mile-per-gallon", MeasureUnit::create(3, 1, status);

// missing "bit",           MeasureUnit::create(5, 0, status);
// missing "byte",          MeasureUnit::create(5, 1, status);
// missing "gigabit",       MeasureUnit::create(5, 2, status);
// missing "gigabyte",      MeasureUnit::create(5, 3, status);
// missing "kilobit",       MeasureUnit::create(5, 4, status);
// missing "kilobyte",      MeasureUnit::create(5, 5, status);
// missing "megabit",       MeasureUnit::create(5, 6, status);
// missing "megabyte",      MeasureUnit::create(5, 7, status);
// missing "terabit",       MeasureUnit::create(5, 8, status);
// missing "terabyte",      MeasureUnit::create(5, 9, status);

MeasureUnit *MeasureUnit::createDay(UErrorCode &status) {
    return MeasureUnit::create(6, 0, status);
}

MeasureUnit *MeasureUnit::createHour(UErrorCode &status) {
    return MeasureUnit::create(6, 1, status);
}

// missing "microsecond",   MeasureUnit::create(6, 2, status);

MeasureUnit *MeasureUnit::createMillisecond(UErrorCode &status) {
    return MeasureUnit::create(6, 3, status);
}

MeasureUnit *MeasureUnit::createMinute(UErrorCode &status) {
    return MeasureUnit::create(6, 4, status);
}

MeasureUnit *MeasureUnit::createMonth(UErrorCode &status) {
    return MeasureUnit::create(6, 5, status);
}

// missing "nanosecond",    MeasureUnit::create(6, 6, status);

MeasureUnit *MeasureUnit::createSecond(UErrorCode &status) {
    return MeasureUnit::create(6, 7, status);
}

MeasureUnit *MeasureUnit::createWeek(UErrorCode &status) {
    return MeasureUnit::create(6, 8, status);
}

MeasureUnit *MeasureUnit::createYear(UErrorCode &status) {
    return MeasureUnit::create(6, 9, status);
}

// missing "ampere",        MeasureUnit::create(7, 0, status);
// missing "milliampere",   MeasureUnit::create(7, 1, status);
// missing "ohm",           MeasureUnit::create(7, 2, status);
// missing "volt",          MeasureUnit::create(7, 3, status);

// missing "calorie",       MeasureUnit::create(8, 0, status);
// missing "foodcalorie",   MeasureUnit::create(8, 1, status);
// missing "joule",         MeasureUnit::create(8, 2, status);
// missing "kilocalorie",   MeasureUnit::create(8, 3, status);
// missing "kilojoule",     MeasureUnit::create(8, 4, status);
// missing "kilowatt-hour", MeasureUnit::create(8, 5, status);

// missing "gigahertz",     MeasureUnit::create(9, 0, status);
// missing "hertz",         MeasureUnit::create(9, 1, status);
// missing "kilohertz",     MeasureUnit::create(9, 2, status);
// missing "megahertz",     MeasureUnit::create(9, 3, status);

// missing "astronomical-unit", MeasureUnit::create(10, 0, status);

MeasureUnit *MeasureUnit::createCentimeter(UErrorCode &status) {
    return MeasureUnit::create(10, 1, status);
}

// missing "decimeter",     MeasureUnit::create(10, 2, status);
// missing "fathom",        MeasureUnit::create(10, 3, status);

MeasureUnit *MeasureUnit::createFoot(UErrorCode &status) {
    return MeasureUnit::create(10, 4, status);
}

// missing "furlong",       MeasureUnit::create(10, 5, status);

MeasureUnit *MeasureUnit::createInch(UErrorCode &status) {
    return MeasureUnit::create(10, 6, status);
}

MeasureUnit *MeasureUnit::createKilometer(UErrorCode &status) {
    return MeasureUnit::create(10, 7, status);
}

MeasureUnit *MeasureUnit::createLightYear(UErrorCode &status) {
    return MeasureUnit::create(10, 8, status);
}

MeasureUnit *MeasureUnit::createMeter(UErrorCode &status) {
    return MeasureUnit::create(10, 9, status);
}

// missing "micrometer",    MeasureUnit::create(10, 10, status);

MeasureUnit *MeasureUnit::createMile(UErrorCode &status) {
    return MeasureUnit::create(10, 11, status);
}

MeasureUnit *MeasureUnit::createMillimeter(UErrorCode &status) {
    return MeasureUnit::create(10, 12, status);
}

// missing "nanometer",     MeasureUnit::create(10, 13, status);
// missing "nautical-mile", MeasureUnit::create(10, 14, status);
// missing "parsec",        MeasureUnit::create(10, 15, status);

MeasureUnit *MeasureUnit::createPicometer(UErrorCode &status) {
    return MeasureUnit::create(10, 16, status);
}

MeasureUnit *MeasureUnit::createYard(UErrorCode &status) {
    return MeasureUnit::create(10, 17, status);
}

// missing "lux",           MeasureUnit::create(11, 0, status);

// missing "carat",         MeasureUnit::create(12, 0, status);

MeasureUnit *MeasureUnit::createGram(UErrorCode &status) {
    return MeasureUnit::create(12, 1, status);
}

MeasureUnit *MeasureUnit::createKilogram(UErrorCode &status) {
    return MeasureUnit::create(12, 2, status);
}

// missing "metric-ton",    MeasureUnit::create(12, 3, status);
// missing "microgram",     MeasureUnit::create(12, 4, status);
// missing "milligram",     MeasureUnit::create(12, 5, status);

MeasureUnit *MeasureUnit::createOunce(UErrorCode &status) {
    return MeasureUnit::create(12, 6, status);
}

// missing "ounce-troy",    MeasureUnit::create(12, 7, status);

MeasureUnit *MeasureUnit::createPound(UErrorCode &status) {
    return MeasureUnit::create(12, 8, status);
}

// missing "stone",         MeasureUnit::create(12, 9, status);
// missing "ton",           MeasureUnit::create(12, 10, status);

// missing "gigawatt",      MeasureUnit::create(13, 0, status);

MeasureUnit *MeasureUnit::createHorsepower(UErrorCode &status) {
    return MeasureUnit::create(13, 1, status);
}

MeasureUnit *MeasureUnit::createKilowatt(UErrorCode &status) {
    return MeasureUnit::create(13, 2, status);
}

// missing "megawatt",      MeasureUnit::create(13, 3, status);
// missing "milliwatt",     MeasureUnit::create(13, 4, status);

MeasureUnit *MeasureUnit::createWatt(UErrorCode &status) {
    return MeasureUnit::create(13, 5, status);
}

MeasureUnit *MeasureUnit::createHectopascal(UErrorCode &status) {
    return MeasureUnit::create(14, 0, status);
}

MeasureUnit *MeasureUnit::createInchHg(UErrorCode &status) {
    return MeasureUnit::create(14, 1, status);
}

MeasureUnit *MeasureUnit::createMillibar(UErrorCode &status) {
    return MeasureUnit::create(14, 2, status);
}

// missing "millimeter-of-mercury", MeasureUnit::create(14, 3, status);
// missing "pound-per-square-inch", MeasureUnit::create(14, 4, status);

// missing "karat",         MeasureUnit::create(15, 0, status);

MeasureUnit *MeasureUnit::createKilometerPerHour(UErrorCode &status) {
    return MeasureUnit::create(16, 0, status);
}

MeasureUnit *MeasureUnit::createMeterPerSecond(UErrorCode &status) {
    return MeasureUnit::create(16, 1, status);
}

MeasureUnit *MeasureUnit::createMilePerHour(UErrorCode &status) {
    return MeasureUnit::create(16, 2, status);
}

MeasureUnit *MeasureUnit::createCelsius(UErrorCode &status) {
    return MeasureUnit::create(17, 0, status);
}

MeasureUnit *MeasureUnit::createFahrenheit(UErrorCode &status) {
    return MeasureUnit::create(17, 1, status);
}

// missing "kelvin",        MeasureUnit::create(17, 2, status);

// missing "acre-foot",         MeasureUnit::create(18, 0, status);
// missing "bushel",            MeasureUnit::create(18, 1, status);
// missing "centiliter",        MeasureUnit::create(18, 2, status);
// missing "cubic-centimeter",  MeasureUnit::create(18, 3, status);
// missing "cubic-foot",        MeasureUnit::create(18, 4, status);
// missing "cubic-inch",        MeasureUnit::create(18, 5, status);

MeasureUnit *MeasureUnit::createCubicKilometer(UErrorCode &status) {
    return MeasureUnit::create(18, 6, status);
}

// missing "cubic-meter",       MeasureUnit::create(18, 7, status);

MeasureUnit *MeasureUnit::createCubicMile(UErrorCode &status) {
    return MeasureUnit::create(18, 8, status);
}

// missing "cubic-yard",        MeasureUnit::create(18, 9, status);
// missing "cup",               MeasureUnit::create(18, 10, status);
// missing "deciliter",         MeasureUnit::create(18, 11, status);
// missing "fluid-ounce",       MeasureUnit::create(18, 12, status);
// missing "gallon",            MeasureUnit::create(18, 13, status);
// missing "hectoliter",        MeasureUnit::create(18, 14, status);

MeasureUnit *MeasureUnit::createLiter(UErrorCode &status) {
    return MeasureUnit::create(18, 15, status);
}

// missing "megaliter",         MeasureUnit::create(18, 16, status);
// missing "milliliter",        MeasureUnit::create(18, 17, status);
// missing "pint",              MeasureUnit::create(18, 18, status);
// missing "quart",             MeasureUnit::create(18, 19, status);
// missing "tablespoon",        MeasureUnit::create(18, 20, status);
// missing "teaspoon",          MeasureUnit::create(18, 21, status);

// End generated code

static int32_t binarySearch(
        const char * const * array, int32_t start, int32_t end, const char * key) {
    while (start < end) {
        int32_t mid = (start + end) / 2;
        int32_t cmp = uprv_strcmp(array[mid], key);
        if (cmp < 0) {
            start = mid + 1;
            continue;
        }
        if (cmp == 0) {
            return mid;
        }
        end = mid;
    }
    return -1;
}
    
MeasureUnit::MeasureUnit(const MeasureUnit &other)
        : fTypeId(other.fTypeId), fSubTypeId(other.fSubTypeId) {
    uprv_strcpy(fCurrency, other.fCurrency);
}

MeasureUnit &MeasureUnit::operator=(const MeasureUnit &other) {
    if (this == &other) {
        return *this;
    }
    fTypeId = other.fTypeId;
    fSubTypeId = other.fSubTypeId;
    uprv_strcpy(fCurrency, other.fCurrency);
    return *this;
}

UObject *MeasureUnit::clone() const {
    return new MeasureUnit(*this);
}

MeasureUnit::~MeasureUnit() {
}

const char *MeasureUnit::getType() const {
    return gTypes[fTypeId];
}

const char *MeasureUnit::getSubtype() const {
    return fCurrency[0] == 0 ? gSubTypes[getOffset()] : fCurrency;
}

UBool MeasureUnit::operator==(const UObject& other) const {
    if (this == &other) {  // Same object, equal
        return TRUE;
    }
    if (typeid(*this) != typeid(other)) { // Different types, not equal
        return FALSE;
    }
    const MeasureUnit &rhs = static_cast<const MeasureUnit&>(other);
    return (
            fTypeId == rhs.fTypeId
            && fSubTypeId == rhs.fSubTypeId
            && uprv_strcmp(fCurrency, rhs.fCurrency) == 0);
}

int32_t MeasureUnit::getIndex() const {
    return gIndexes[fTypeId] + fSubTypeId;
}

int32_t MeasureUnit::getAvailable(
        MeasureUnit *dest,
        int32_t destCapacity,
        UErrorCode &errorCode) {
    if (U_FAILURE(errorCode)) {
        return 0;
    }
    if (destCapacity < UPRV_LENGTHOF(gSubTypes)) {
        errorCode = U_BUFFER_OVERFLOW_ERROR;
        return UPRV_LENGTHOF(gSubTypes);
    }
    int32_t idx = 0;
    for (int32_t typeIdx = 0; typeIdx < UPRV_LENGTHOF(gTypes); ++typeIdx) {
        int32_t len = gOffsets[typeIdx + 1] - gOffsets[typeIdx];
        for (int32_t subTypeIdx = 0; subTypeIdx < len; ++subTypeIdx) {
            dest[idx].setTo(typeIdx, subTypeIdx);
            ++idx;
        }
    }
    U_ASSERT(idx == UPRV_LENGTHOF(gSubTypes));
    return UPRV_LENGTHOF(gSubTypes);
}

int32_t MeasureUnit::getAvailable(
        const char *type,
        MeasureUnit *dest,
        int32_t destCapacity,
        UErrorCode &errorCode) {
    if (U_FAILURE(errorCode)) {
        return 0;
    }
    int32_t typeIdx = binarySearch(gTypes, 0, UPRV_LENGTHOF(gTypes), type);
    if (typeIdx == -1) {
        return 0;
    }
    int32_t len = gOffsets[typeIdx + 1] - gOffsets[typeIdx];
    if (destCapacity < len) {
        errorCode = U_BUFFER_OVERFLOW_ERROR;
        return len;
    }
    for (int subTypeIdx = 0; subTypeIdx < len; ++subTypeIdx) {
        dest[subTypeIdx].setTo(typeIdx, subTypeIdx);
    }
    return len;
}

StringEnumeration* MeasureUnit::getAvailableTypes(UErrorCode &errorCode) {
    UEnumeration *uenum = uenum_openCharStringsEnumeration(
            gTypes, UPRV_LENGTHOF(gTypes), &errorCode);
    if (U_FAILURE(errorCode)) {
        uenum_close(uenum);
        return NULL;
    }
    StringEnumeration *result = new UStringEnumeration(uenum);
    if (result == NULL) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        uenum_close(uenum);
        return NULL;
    }
    return result;
}

int32_t MeasureUnit::getIndexCount() {
    return gIndexes[UPRV_LENGTHOF(gIndexes) - 1];
}

MeasureUnit *MeasureUnit::create(int typeId, int subTypeId, UErrorCode &status) {
    if (U_FAILURE(status)) {
        return NULL;
    }
    MeasureUnit *result = new MeasureUnit(typeId, subTypeId);
    if (result == NULL) {
        status = U_MEMORY_ALLOCATION_ERROR;
    }
    return result;
}

void MeasureUnit::initTime(const char *timeId) {
    int32_t result = binarySearch(gTypes, 0, UPRV_LENGTHOF(gTypes), "duration");
    U_ASSERT(result != -1);
    fTypeId = result;
    result = binarySearch(gSubTypes, gOffsets[fTypeId], gOffsets[fTypeId + 1], timeId);
    U_ASSERT(result != -1);
    fSubTypeId = result - gOffsets[fTypeId]; 
}

void MeasureUnit::initCurrency(const char *isoCurrency) {
    int32_t result = binarySearch(gTypes, 0, UPRV_LENGTHOF(gTypes), "currency");
    U_ASSERT(result != -1);
    fTypeId = result;
    result = binarySearch(
            gSubTypes, gOffsets[fTypeId], gOffsets[fTypeId + 1], isoCurrency);
    if (result != -1) {
        fSubTypeId = result - gOffsets[fTypeId];
    } else {
        uprv_strncpy(fCurrency, isoCurrency, UPRV_LENGTHOF(fCurrency));
    }
}

void MeasureUnit::setTo(int32_t typeId, int32_t subTypeId) {
    fTypeId = typeId;
    fSubTypeId = subTypeId;
    fCurrency[0] = 0;
}

int32_t MeasureUnit::getOffset() const {
    return gOffsets[fTypeId] + fSubTypeId;
}

U_NAMESPACE_END

#endif /* !UNCONFIG_NO_FORMATTING */
