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

#define LENGTHOF(array) (int32_t)(sizeof(array) / sizeof((array)[0]))

U_NAMESPACE_BEGIN

UOBJECT_DEFINE_RTTI_IMPLEMENTATION(MeasureUnit)

static const int32_t gOffsets[] = {
    0,
    1,
    4,
    10,
    270,
    278,
    288,
    292,
    295,
    298,
    301,
    303,
    306
};

static const int32_t gIndexes[] = {
    0,
    1,
    4,
    10,
    10,
    18,
    28,
    32,
    35,
    38,
    41,
    43,
    46
};

static const char * const gTypes[] = {
    "acceleration",
    "angle",
    "area",
    "currency",
    "duration",
    "length",
    "mass",
    "power",
    "pressure",
    "speed",
    "temperature",
    "volume"
};

static const char * const gSubTypes[] = {
    "g-force",
    "arc-minute",
    "arc-second",
    "degree",
    "acre",
    "hectare",
    "square-foot",
    "square-kilometer",
    "square-meter",
    "square-mile",
    "ADP",
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
    "day",
    "hour",
    "millisecond",
    "minute",
    "month",
    "second",
    "week",
    "year",
    "centimeter",
    "foot",
    "inch",
    "kilometer",
    "light-year",
    "meter",
    "mile",
    "millimeter",
    "picometer",
    "yard",
    "gram",
    "kilogram",
    "ounce",
    "pound",
    "horsepower",
    "kilowatt",
    "watt",
    "hectopascal",
    "inch-hg",
    "millibar",
    "kilometer-per-hour",
    "meter-per-second",
    "mile-per-hour",
    "celsius",
    "fahrenheit",
    "cubic-kilometer",
    "cubic-mile",
    "liter"
};

MeasureUnit *MeasureUnit::createGForce(UErrorCode &status) {
    return MeasureUnit::create(0, 0, status);
}

MeasureUnit *MeasureUnit::createArcMinute(UErrorCode &status) {
    return MeasureUnit::create(1, 0, status);
}

MeasureUnit *MeasureUnit::createArcSecond(UErrorCode &status) {
    return MeasureUnit::create(1, 1, status);
}

MeasureUnit *MeasureUnit::createDegree(UErrorCode &status) {
    return MeasureUnit::create(1, 2, status);
}

MeasureUnit *MeasureUnit::createAcre(UErrorCode &status) {
    return MeasureUnit::create(2, 0, status);
}

MeasureUnit *MeasureUnit::createHectare(UErrorCode &status) {
    return MeasureUnit::create(2, 1, status);
}

MeasureUnit *MeasureUnit::createSquareFoot(UErrorCode &status) {
    return MeasureUnit::create(2, 2, status);
}

MeasureUnit *MeasureUnit::createSquareKilometer(UErrorCode &status) {
    return MeasureUnit::create(2, 3, status);
}

MeasureUnit *MeasureUnit::createSquareMeter(UErrorCode &status) {
    return MeasureUnit::create(2, 4, status);
}

MeasureUnit *MeasureUnit::createSquareMile(UErrorCode &status) {
    return MeasureUnit::create(2, 5, status);
}

MeasureUnit *MeasureUnit::createDay(UErrorCode &status) {
    return MeasureUnit::create(4, 0, status);
}

MeasureUnit *MeasureUnit::createHour(UErrorCode &status) {
    return MeasureUnit::create(4, 1, status);
}

MeasureUnit *MeasureUnit::createMillisecond(UErrorCode &status) {
    return MeasureUnit::create(4, 2, status);
}

MeasureUnit *MeasureUnit::createMinute(UErrorCode &status) {
    return MeasureUnit::create(4, 3, status);
}

MeasureUnit *MeasureUnit::createMonth(UErrorCode &status) {
    return MeasureUnit::create(4, 4, status);
}

MeasureUnit *MeasureUnit::createSecond(UErrorCode &status) {
    return MeasureUnit::create(4, 5, status);
}

MeasureUnit *MeasureUnit::createWeek(UErrorCode &status) {
    return MeasureUnit::create(4, 6, status);
}

MeasureUnit *MeasureUnit::createYear(UErrorCode &status) {
    return MeasureUnit::create(4, 7, status);
}

MeasureUnit *MeasureUnit::createCentimeter(UErrorCode &status) {
    return MeasureUnit::create(5, 0, status);
}

MeasureUnit *MeasureUnit::createFoot(UErrorCode &status) {
    return MeasureUnit::create(5, 1, status);
}

MeasureUnit *MeasureUnit::createInch(UErrorCode &status) {
    return MeasureUnit::create(5, 2, status);
}

MeasureUnit *MeasureUnit::createKilometer(UErrorCode &status) {
    return MeasureUnit::create(5, 3, status);
}

MeasureUnit *MeasureUnit::createLightYear(UErrorCode &status) {
    return MeasureUnit::create(5, 4, status);
}

MeasureUnit *MeasureUnit::createMeter(UErrorCode &status) {
    return MeasureUnit::create(5, 5, status);
}

MeasureUnit *MeasureUnit::createMile(UErrorCode &status) {
    return MeasureUnit::create(5, 6, status);
}

MeasureUnit *MeasureUnit::createMillimeter(UErrorCode &status) {
    return MeasureUnit::create(5, 7, status);
}

MeasureUnit *MeasureUnit::createPicometer(UErrorCode &status) {
    return MeasureUnit::create(5, 8, status);
}

MeasureUnit *MeasureUnit::createYard(UErrorCode &status) {
    return MeasureUnit::create(5, 9, status);
}

MeasureUnit *MeasureUnit::createGram(UErrorCode &status) {
    return MeasureUnit::create(6, 0, status);
}

MeasureUnit *MeasureUnit::createKilogram(UErrorCode &status) {
    return MeasureUnit::create(6, 1, status);
}

MeasureUnit *MeasureUnit::createOunce(UErrorCode &status) {
    return MeasureUnit::create(6, 2, status);
}

MeasureUnit *MeasureUnit::createPound(UErrorCode &status) {
    return MeasureUnit::create(6, 3, status);
}

MeasureUnit *MeasureUnit::createHorsepower(UErrorCode &status) {
    return MeasureUnit::create(7, 0, status);
}

MeasureUnit *MeasureUnit::createKilowatt(UErrorCode &status) {
    return MeasureUnit::create(7, 1, status);
}

MeasureUnit *MeasureUnit::createWatt(UErrorCode &status) {
    return MeasureUnit::create(7, 2, status);
}

MeasureUnit *MeasureUnit::createHectopascal(UErrorCode &status) {
    return MeasureUnit::create(8, 0, status);
}

MeasureUnit *MeasureUnit::createInchHg(UErrorCode &status) {
    return MeasureUnit::create(8, 1, status);
}

MeasureUnit *MeasureUnit::createMillibar(UErrorCode &status) {
    return MeasureUnit::create(8, 2, status);
}

MeasureUnit *MeasureUnit::createKilometerPerHour(UErrorCode &status) {
    return MeasureUnit::create(9, 0, status);
}

MeasureUnit *MeasureUnit::createMeterPerSecond(UErrorCode &status) {
    return MeasureUnit::create(9, 1, status);
}

MeasureUnit *MeasureUnit::createMilePerHour(UErrorCode &status) {
    return MeasureUnit::create(9, 2, status);
}

MeasureUnit *MeasureUnit::createCelsius(UErrorCode &status) {
    return MeasureUnit::create(10, 0, status);
}

MeasureUnit *MeasureUnit::createFahrenheit(UErrorCode &status) {
    return MeasureUnit::create(10, 1, status);
}

MeasureUnit *MeasureUnit::createCubicKilometer(UErrorCode &status) {
    return MeasureUnit::create(11, 0, status);
}

MeasureUnit *MeasureUnit::createCubicMile(UErrorCode &status) {
    return MeasureUnit::create(11, 1, status);
}

MeasureUnit *MeasureUnit::createLiter(UErrorCode &status) {
    return MeasureUnit::create(11, 2, status);
}

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
    if (destCapacity < LENGTHOF(gSubTypes)) {
        errorCode = U_BUFFER_OVERFLOW_ERROR;
        return LENGTHOF(gSubTypes);
    }
    int32_t idx = 0;
    for (int32_t typeIdx = 0; typeIdx < LENGTHOF(gTypes); ++typeIdx) {
        int32_t len = gOffsets[typeIdx + 1] - gOffsets[typeIdx];
        for (int32_t subTypeIdx = 0; subTypeIdx < len; ++subTypeIdx) {
            dest[idx].setTo(typeIdx, subTypeIdx);
            ++idx;
        }
    }
    U_ASSERT(idx == LENGTHOF(gSubTypes));
    return LENGTHOF(gSubTypes);
}

int32_t MeasureUnit::getAvailable(
        const char *type,
        MeasureUnit *dest,
        int32_t destCapacity,
        UErrorCode &errorCode) {
    if (U_FAILURE(errorCode)) {
        return 0;
    }
    int32_t typeIdx = binarySearch(gTypes, 0, LENGTHOF(gTypes), type);
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
            gTypes, LENGTHOF(gTypes), &errorCode);
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
    return gIndexes[LENGTHOF(gIndexes) - 1];
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
    int32_t result = binarySearch(gTypes, 0, LENGTHOF(gTypes), "duration");
    U_ASSERT(result != -1);
    fTypeId = result;
    result = binarySearch(gSubTypes, gOffsets[fTypeId], gOffsets[fTypeId + 1], timeId);
    U_ASSERT(result != -1);
    fSubTypeId = result - gOffsets[fTypeId]; 
}

void MeasureUnit::initCurrency(const char *isoCurrency) {
    int32_t result = binarySearch(gTypes, 0, LENGTHOF(gTypes), "currency");
    U_ASSERT(result != -1);
    fTypeId = result;
    result = binarySearch(
            gSubTypes, gOffsets[fTypeId], gOffsets[fTypeId + 1], isoCurrency);
    if (result != -1) {
        fSubTypeId = result - gOffsets[fTypeId];
    } else {
        uprv_strncpy(fCurrency, isoCurrency, LENGTHOF(fCurrency));
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
