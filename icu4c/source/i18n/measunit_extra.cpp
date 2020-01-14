// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

// Extra functions for MeasureUnit not needed for all clients.
// Separate .o file so that it can be removed for modularity.

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

// Allow implicit conversion from char16_t* to UnicodeString for this file:
// Helpful in toString methods and elsewhere.
#define UNISTR_FROM_STRING_EXPLICIT

#include "cstring.h"
#include "uassert.h"
#include "ucln_in.h"
#include "umutex.h"
#include "unicode/errorcode.h"
#include "unicode/measunit.h"
#include "unicode/ucharstrie.h"
#include "unicode/ucharstriebuilder.h"

#include "cstr.h"

U_NAMESPACE_BEGIN


namespace {

// This is to ensure we only insert positive integers into the trie
constexpr int32_t kSIPrefixOffset = 64;

constexpr int32_t kCompoundPartOffset = 128;

enum CompoundPart {
    COMPOUND_PART_PER = kCompoundPartOffset,
    COMPOUND_PART_TIMES,
    COMPOUND_PART_ONE_PER,
    COMPOUND_PART_PLUS,
};

constexpr int32_t kPowerPartOffset = 256;

enum PowerPart {
    POWER_PART_P2 = kPowerPartOffset + 2,
    POWER_PART_P3,
    POWER_PART_P4,
    POWER_PART_P5,
    POWER_PART_P6,
    POWER_PART_P7,
    POWER_PART_P8,
    POWER_PART_P9,
    POWER_PART_P10,
    POWER_PART_P11,
    POWER_PART_P12,
    POWER_PART_P13,
    POWER_PART_P14,
    POWER_PART_P15,
};

constexpr int32_t kSimpleUnitOffset = 512;

const struct SIPrefixStrings {
    const char* const string;
    UMeasureSIPrefix value;
} gSIPrefixStrings[] = {
    { "yotta", UMEASURE_SI_PREFIX_YOTTA },
    { "zetta", UMEASURE_SI_PREFIX_ZETTA },
    { "exa", UMEASURE_SI_PREFIX_EXA },
    { "peta", UMEASURE_SI_PREFIX_PETA },
    { "tera", UMEASURE_SI_PREFIX_TERA },
    { "giga", UMEASURE_SI_PREFIX_GIGA },
    { "mega", UMEASURE_SI_PREFIX_MEGA },
    { "kilo", UMEASURE_SI_PREFIX_KILO },
    { "hecto", UMEASURE_SI_PREFIX_HECTO },
    { "deka", UMEASURE_SI_PREFIX_DEKA },
    { "deci", UMEASURE_SI_PREFIX_DECI },
    { "centi", UMEASURE_SI_PREFIX_CENTI },
    { "milli", UMEASURE_SI_PREFIX_MILLI },
    { "micro", UMEASURE_SI_PREFIX_MICRO },
    { "nano", UMEASURE_SI_PREFIX_NANO },
    { "pico", UMEASURE_SI_PREFIX_PICO },
    { "femto", UMEASURE_SI_PREFIX_FEMTO },
    { "atto", UMEASURE_SI_PREFIX_ATTO },
    { "zepto", UMEASURE_SI_PREFIX_ZEPTO },
    { "yocto", UMEASURE_SI_PREFIX_YOCTO },
};

// FIXME: Get this list from data
const char16_t* const gSimpleUnits[] = {
    u"100kilometer",
    u"acre",
    u"ampere",
    u"arc-minute",
    u"arc-second",
    u"astronomical-unit",
    u"atmosphere",
    u"bar",
    u"barrel",
    u"bit",
    u"british-thermal-unit",
    u"bushel",
    u"byte",
    u"calorie",
    u"carat",
    u"celsius",
    u"century",
    u"cup",
    u"cup-metric",
    u"dalton",
    u"day",
    u"day-person",
    u"decade",
    u"degree",
    u"dot", // (as in "dot-per-inch")
    u"dunam",
    u"earth-mass",
    u"electronvolt",
    u"em",
    u"fahrenheit",
    u"fathom",
    u"fluid-ounce",
    u"fluid-ounce-imperial",
    u"foodcalorie",
    u"foot",
    u"furlong",
    u"g-force",
    u"gallon",
    u"gallon-imperial",
    u"generic", // (i.e., "temperature-generic")
    u"gram",
    u"hectare", // (note: other "are" derivatives are uncommon)
    u"hertz",
    u"horsepower",
    u"hour",
    u"inch",
    u"inch-hg",
    u"joule",
    u"karat",
    u"kelvin",
    u"knot",
    u"light-year",
    u"liter",
    u"lux",
    u"meter",
    u"meter-of-mercury", // (not "millimeter-of-mercury")
    u"metric-ton",
    u"mile",
    u"mile-scandinavian",
    u"minute",
    u"mole",
    u"month",
    u"month-person",
    u"nautical-mile",
    u"newton",
    u"ohm",
    u"ounce",
    u"ounce-troy",
    u"parsec",
    u"pascal",
    u"percent",
    u"permille",
    u"permillion",
    u"permyriad",
    u"pint",
    u"pint-metric",
    u"pixel",
    u"point",
    u"pound",
    u"pound-force",
    u"quart",
    u"radian",
    u"revolution",
    u"second",
    u"solar-luminosity",
    u"solar-mass",
    u"solar-radius",
    u"stone",
    u"tablespoon",
    u"teaspoon",
    u"therm-us",
    u"ton",
    u"volt",
    u"watt",
    u"week",
    u"week-person",
    u"yard",
    u"year",
    u"year-person",
};

icu::UInitOnce gUnitExtrasInitOnce = U_INITONCE_INITIALIZER;

char16_t* kSerializedUnitExtrasStemTrie = nullptr;

UBool U_CALLCONV cleanupUnitExtras() {
    uprv_free(kSerializedUnitExtrasStemTrie);
    kSerializedUnitExtrasStemTrie = nullptr;
    gUnitExtrasInitOnce.reset();
    return TRUE;
}

void U_CALLCONV initUnitExtras(UErrorCode& status) {
    ucln_i18n_registerCleanup(UCLN_I18N_UNIT_EXTRAS, cleanupUnitExtras);

    UCharsTrieBuilder b(status);
    if (U_FAILURE(status)) { return; }

    // Add SI prefixes
    for (const auto& siPrefixInfo : gSIPrefixStrings) {
        UnicodeString uSIPrefix(siPrefixInfo.string, -1, US_INV);
        b.add(uSIPrefix, siPrefixInfo.value + kSIPrefixOffset, status);
    }
    if (U_FAILURE(status)) { return; }

    // Add syntax parts (compound, power prefixes)
    b.add(u"-per-", COMPOUND_PART_PER, status);
    b.add(u"-", COMPOUND_PART_TIMES, status);
    b.add(u"one-per-", COMPOUND_PART_ONE_PER, status);
    b.add(u"+", COMPOUND_PART_PLUS, status);
    b.add(u"square-", POWER_PART_P2, status);
    b.add(u"cubic-", POWER_PART_P3, status);
    b.add(u"p2-", POWER_PART_P2, status);
    b.add(u"p3-", POWER_PART_P3, status);
    b.add(u"p4-", POWER_PART_P4, status);
    b.add(u"p5-", POWER_PART_P5, status);
    b.add(u"p6-", POWER_PART_P6, status);
    b.add(u"p7-", POWER_PART_P7, status);
    b.add(u"p8-", POWER_PART_P8, status);
    b.add(u"p9-", POWER_PART_P9, status);
    b.add(u"p10-", POWER_PART_P10, status);
    b.add(u"p11-", POWER_PART_P11, status);
    b.add(u"p12-", POWER_PART_P12, status);
    b.add(u"p13-", POWER_PART_P13, status);
    b.add(u"p14-", POWER_PART_P14, status);
    b.add(u"p15-", POWER_PART_P15, status);
    if (U_FAILURE(status)) { return; }

    // Add sanctioned simple units by offset
    int32_t simpleUnitOffset = kSimpleUnitOffset;
    for (auto simpleUnit : gSimpleUnits) {
        b.add(simpleUnit, simpleUnitOffset++, status);
    }

    // Build the CharsTrie
    // TODO: Use SLOW or FAST here?
    UnicodeString result;
    b.buildUnicodeString(USTRINGTRIE_BUILD_FAST, result, status);
    if (U_FAILURE(status)) { return; }

    // Copy the result into the global constant pointer
    size_t numBytes = result.length() * sizeof(char16_t);
    kSerializedUnitExtrasStemTrie = static_cast<char16_t*>(uprv_malloc(numBytes));
    uprv_memcpy(kSerializedUnitExtrasStemTrie, result.getBuffer(), numBytes);
}

class UnitIdentifierParser {
public:
    static UnitIdentifierParser from(StringPiece source, UErrorCode& status) {
        umtx_initOnce(gUnitExtrasInitOnce, &initUnitExtras, status);
        if (U_FAILURE(status)) {
            return UnitIdentifierParser();
        }
        return UnitIdentifierParser(source);
    }
    
    int32_t nextToken(UErrorCode& status) {
        fTrie.reset();
        int32_t match = -1;
        int32_t previ = -1;
        do {
            fTrie.next(fSource.data()[fIndex++]);
            if (fTrie.current() == USTRINGTRIE_NO_MATCH) {
                break;
            } else if (fTrie.current() == USTRINGTRIE_NO_VALUE) {
                continue;
            } else if (fTrie.current() == USTRINGTRIE_FINAL_VALUE) {
                match = fTrie.getValue();
                previ = fIndex;
                break;
            } else if (fTrie.current() == USTRINGTRIE_INTERMEDIATE_VALUE) {
                match = fTrie.getValue();
                previ = fIndex;
                continue;
            } else {
                UPRV_UNREACHABLE;
            }
        } while (fIndex < fSource.length());

        if (match < 0) {
            // TODO: Make a new status code?
            status = U_ILLEGAL_ARGUMENT_ERROR;
        } else {
            fIndex = previ;
        }
        return match;
    }

    bool hasNext() const {
        return fIndex < fSource.length();
    }

    int32_t currentIndex() const {
        return fIndex;
    }

private:
    int32_t fIndex = 0;
    StringPiece fSource;
    UCharsTrie fTrie;

    UnitIdentifierParser() : fSource(""), fTrie(u"") {}

    UnitIdentifierParser(StringPiece source)
        : fSource(source), fTrie(kSerializedUnitExtrasStemTrie) {}
};

} // namespace


MeasureUnit MeasureUnit::forIdentifier(const char* identifier, UErrorCode& status) {
    UnitIdentifierParser parser = UnitIdentifierParser::from(identifier, status);
    if (U_FAILURE(status)) {
        // Unrecoverable error
        return MeasureUnit();
    }

    while (parser.hasNext()) {
        parser.nextToken(status);
        if (U_FAILURE(status)) {
            // Invalid syntax
            return MeasureUnit();
        }

        // if (match < kCompoundPartOffset) {
        //     // SI Prefix
        //     auto prefix = static_cast<UMeasureSIPrefix>(match - kSIPrefixOffset);
        // } else if (match < kPowerPartOffset) {
        //     // Compound part
        //     const char* operation = (match == COMPOUND_PART_PER) ? "per" : "times/plus";
        // } else if (match < kSimpleUnitOffset) {
        //     // Power part
        //     int32_t power = match - kPowerPartOffset;
        // } else {
        //     // Simple unit
        //     const char16_t* simpleUnit = gSimpleUnits[match - kSimpleUnitOffset];
        // }
    }

    // Success
    return MeasureUnit(uprv_strdup(identifier));
}

UMeasureSIPrefix MeasureUnit::getSIPrefix() const {
    ErrorCode status;
    const char* id = toString();
    UnitIdentifierParser parser = UnitIdentifierParser::from(id, status);
    if (status.isFailure()) {
        // Unrecoverable error
        return UMEASURE_SI_PREFIX_ONE;
    }

    int32_t match = parser.nextToken(status);
    if (status.isFailure()) {
        // Invalid syntax
        return UMEASURE_SI_PREFIX_ONE;
    }

    if (match >= kPowerPartOffset && match < kSimpleUnitOffset) {
        // Skip the power part
        match = parser.nextToken(status);
        if (status.isFailure()) {
            // Invalid syntax
            return UMEASURE_SI_PREFIX_ONE;
        }
    }

    if (match >= kCompoundPartOffset) {
        // No SI prefix
        return UMEASURE_SI_PREFIX_ONE;
    }

    return static_cast<UMeasureSIPrefix>(match - kSIPrefixOffset);
}

MeasureUnit MeasureUnit::withSIPrefix(UMeasureSIPrefix prefix) const {
    ErrorCode status;
    const char* id = toString();
    UnitIdentifierParser parser = UnitIdentifierParser::from(id, status);
    if (status.isFailure()) {
        // Unrecoverable error
        return *this;
    }

    int32_t match = parser.nextToken(status);
    if (status.isFailure()) {
        // Invalid syntax
        return *this;
    }

    CharString builder;
    int32_t unitStart = 0;
    if (match >= kPowerPartOffset && match < kSimpleUnitOffset) {
        // Skip the power part
        unitStart = parser.currentIndex();
        builder.append(id, unitStart, status);
        match = parser.nextToken(status);
    }

    // Append the new SI prefix
    for (const auto& siPrefixInfo : gSIPrefixStrings) {
        if (siPrefixInfo.value == prefix) {
            builder.append(siPrefixInfo.string, status);
            break;
        }
    }

    if (match < kCompoundPartOffset) {
        // Remove the old SI prefix
        unitStart = parser.currentIndex();
    }
    builder.append(id + unitStart, status);
    if (status.isFailure()) {
        // Unrecoverable error
        return *this;
    }

    return MeasureUnit(builder.cloneData(status));
}

int8_t MeasureUnit::getPower() const {
    ErrorCode status;
    const char* id = toString();
    UnitIdentifierParser parser = UnitIdentifierParser::from(id, status);
    if (status.isFailure()) {
        // Unrecoverable error
        return 0;
    }

    int32_t match = parser.nextToken(status);
    if (status.isFailure()) {
        // Invalid syntax
        return 0;
    }

    if (match < kPowerPartOffset || match >= kSimpleUnitOffset) {
        // No power part
        return 0;
    }

    return static_cast<int8_t>(match - kPowerPartOffset);
}

MeasureUnit MeasureUnit::withPower(int8_t power) const {
    if (power < 0) {
        // Don't know how to handle this yet
        U_ASSERT(FALSE);
    }

    ErrorCode status;
    const char* id = toString();
    UnitIdentifierParser parser = UnitIdentifierParser::from(id, status);
    if (status.isFailure()) {
        // Unrecoverable error
        return *this;
    }

    int32_t match = parser.nextToken(status);
    if (status.isFailure()) {
        // Invalid syntax
        return *this;
    }

    // Append the new power
    CharString builder;
    if (power == 2) {
        builder.append("square-", status);
    } else if (power == 3) {
        builder.append("cubic-", status);
    } else if (power < 10) {
        builder.append('p', status);
        builder.append(power + '0', status);
        builder.append('-', status);
    } else {
        builder.append("p1", status);
        builder.append('0' + (power % 10), status);
        builder.append('-', status);
    }

    if (match < kCompoundPartOffset) {
        // Remove the old power
        builder.append(id + parser.currentIndex(), status);
    } else {
        // Append the whole identifier
        builder.append(id, status);
    }
    if (status.isFailure()) {
        // Unrecoverable error
        return *this;
    }

    return MeasureUnit(builder.cloneData(status));
}


U_NAMESPACE_END

#endif /* !UNCONFIG_NO_FORMATTING */
