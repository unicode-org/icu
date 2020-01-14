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
#include "ucln_in.h"
#include "umutex.h"
#include "unicode/errorcode.h"
#include "unicode/measunit.h"
#include "unicode/ucharstrie.h"
#include "unicode/ucharstriebuilder.h"

U_NAMESPACE_BEGIN


namespace {

// This is to ensure we only insert positive integers into the trie
constexpr int32_t kSIPrefixOffset = 64;

constexpr int32_t kSyntaxPartOffset = 256;

enum SyntaxPart {
    SYNTAX_PART_PER = kSyntaxPartOffset,
    SYNTAX_PART_SQUARE,
    SYNTAX_PART_CUBIC,
    SYNTAX_PART_P1,
    SYNTAX_PART_P2,
    SYNTAX_PART_P3,
    SYNTAX_PART_P4,
    SYNTAX_PART_P5,
    SYNTAX_PART_P6,
    SYNTAX_PART_P7,
    SYNTAX_PART_P8,
    SYNTAX_PART_P9,
};

constexpr int32_t kSimpleUnitOffset = 512;

// FIXME: Get this list from data
const char16_t* gSimpleUnits[] = {
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
    b.add(u"yotta", kSIPrefixOffset + UMEASURE_SI_PREFIX_YOTTA, status);
    b.add(u"zetta", kSIPrefixOffset + UMEASURE_SI_PREFIX_ZETTA, status);
    b.add(u"exa", kSIPrefixOffset + UMEASURE_SI_PREFIX_EXA, status);
    b.add(u"peta", kSIPrefixOffset + UMEASURE_SI_PREFIX_PETA, status);
    b.add(u"tera", kSIPrefixOffset + UMEASURE_SI_PREFIX_TERA, status);
    b.add(u"giga", kSIPrefixOffset + UMEASURE_SI_PREFIX_GIGA, status);
    b.add(u"mega", kSIPrefixOffset + UMEASURE_SI_PREFIX_MEGA, status);
    b.add(u"kilo", kSIPrefixOffset + UMEASURE_SI_PREFIX_KILO, status);
    b.add(u"hecto", kSIPrefixOffset + UMEASURE_SI_PREFIX_HECTO, status);
    b.add(u"deka", kSIPrefixOffset + UMEASURE_SI_PREFIX_DEKA, status);
    b.add(u"deci", kSIPrefixOffset + UMEASURE_SI_PREFIX_DECI, status);
    b.add(u"centi", kSIPrefixOffset + UMEASURE_SI_PREFIX_CENTI, status);
    b.add(u"milli", kSIPrefixOffset + UMEASURE_SI_PREFIX_MILLI, status);
    b.add(u"micro", kSIPrefixOffset + UMEASURE_SI_PREFIX_MICRO, status);
    b.add(u"nano", kSIPrefixOffset + UMEASURE_SI_PREFIX_NANO, status);
    b.add(u"pico", kSIPrefixOffset + UMEASURE_SI_PREFIX_PICO, status);
    b.add(u"femto", kSIPrefixOffset + UMEASURE_SI_PREFIX_FEMTO, status);
    b.add(u"atto", kSIPrefixOffset + UMEASURE_SI_PREFIX_ATTO, status);
    b.add(u"zepto", kSIPrefixOffset + UMEASURE_SI_PREFIX_ZEPTO, status);
    b.add(u"yocto", kSIPrefixOffset + UMEASURE_SI_PREFIX_YOCTO, status);
    if (U_FAILURE(status)) { return; }

    // Add syntax parts (per, power prefixes)
    b.add(u"-per-", SYNTAX_PART_PER, status);
    b.add(u"square-", SYNTAX_PART_SQUARE, status);
    b.add(u"cubic-", SYNTAX_PART_CUBIC, status);
    b.add(u"p1", SYNTAX_PART_P1, status);
    b.add(u"p2", SYNTAX_PART_P2, status);
    b.add(u"p3", SYNTAX_PART_P3, status);
    b.add(u"p4", SYNTAX_PART_P4, status);
    b.add(u"p5", SYNTAX_PART_P5, status);
    b.add(u"p6", SYNTAX_PART_P6, status);
    b.add(u"p7", SYNTAX_PART_P7, status);
    b.add(u"p8", SYNTAX_PART_P8, status);
    b.add(u"p9", SYNTAX_PART_P9, status);
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

} // namespace


U_NAMESPACE_END

#endif /* !UNCONFIG_NO_FORMATTING */
