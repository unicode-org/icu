// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include <unicode/numberformatter.h>
#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING && !UPRV_INCOMPLETE_CPP11_SUPPORT

// Allow implicit conversion from char16_t* to UnicodeString for this file:
// Helpful in toString methods and elsewhere.
#define UNISTR_FROM_STRING_EXPLICIT

#include "number_skeletons.h"
#include "umutex.h"
#include "ucln_in.h"
#include "hash.h"
#include "patternprops.h"
#include "unicode/ucharstriebuilder.h"
#include "number_utils.h"

using namespace icu;
using namespace icu::number;
using namespace icu::number::impl;
using namespace icu::number::impl::skeleton;

static constexpr UErrorCode U_NUMBER_SKELETON_SYNTAX_ERROR = U_ILLEGAL_ARGUMENT_ERROR;

namespace {

icu::UInitOnce gNumberSkeletonsInitOnce = U_INITONCE_INITIALIZER;

char16_t* kSerializedStemTrie = nullptr;

UBool U_CALLCONV cleanupNumberSkeletons() {
    uprv_free(kSerializedStemTrie);
    kSerializedStemTrie = nullptr;
}

void U_CALLCONV initNumberSkeletons(UErrorCode& status) {
    ucln_i18n_registerCleanup(UCLN_I18N_NUMBER_SKELETONS, cleanupNumberSkeletons);

    UCharsTrieBuilder b(status);
    if (U_FAILURE(status)) { return; }

    // Section 1:
    b.add(u"compact-short", STEM_COMPACT_SHORT, status);
    b.add(u"compact-long", STEM_COMPACT_LONG, status);
    b.add(u"scientific", STEM_SCIENTIFIC, status);
    b.add(u"engineering", STEM_ENGINEERING, status);
    b.add(u"notation-simple", STEM_NOTATION_SIMPLE, status);
    b.add(u"base-unit", STEM_BASE_UNIT, status);
    b.add(u"percent", STEM_PERCENT, status);
    b.add(u"permille", STEM_PERMILLE, status);
    b.add(u"round-integer", STEM_ROUND_INTEGER, status);
    b.add(u"round-unlimited", STEM_ROUND_UNLIMITED, status);
    b.add(u"round-currency-standard", STEM_ROUND_CURRENCY_STANDARD, status);
    b.add(u"round-currency-cash", STEM_ROUND_CURRENCY_CASH, status);
    b.add(u"group-off", STEM_GROUP_OFF, status);
    b.add(u"group-min2", STEM_GROUP_MIN2, status);
    b.add(u"group-auto", STEM_GROUP_AUTO, status);
    b.add(u"group-on-aligned", STEM_GROUP_ON_ALIGNED, status);
    b.add(u"group-thousands", STEM_GROUP_THOUSANDS, status);
    b.add(u"latin", STEM_LATIN, status);
    b.add(u"unit-width-narrow", STEM_UNIT_WIDTH_NARROW, status);
    b.add(u"unit-width-short", STEM_UNIT_WIDTH_SHORT, status);
    b.add(u"unit-width-full-name", STEM_UNIT_WIDTH_FULL_NAME, status);
    b.add(u"unit-width-iso-code", STEM_UNIT_WIDTH_ISO_CODE, status);
    b.add(u"unit-width-hidden", STEM_UNIT_WIDTH_HIDDEN, status);
    b.add(u"sign-auto", STEM_SIGN_AUTO, status);
    b.add(u"sign-always", STEM_SIGN_ALWAYS, status);
    b.add(u"sign-never", STEM_SIGN_NEVER, status);
    b.add(u"sign-accounting", STEM_SIGN_ACCOUNTING, status);
    b.add(u"sign-accounting-always", STEM_SIGN_ACCOUNTING_ALWAYS, status);
    b.add(u"sign-except-zero", STEM_SIGN_EXCEPT_ZERO, status);
    b.add(u"sign-accounting-except-zero", STEM_SIGN_ACCOUNTING_EXCEPT_ZERO, status);
    b.add(u"decimal-auto", STEM_DECIMAL_AUTO, status);
    b.add(u"decimal-always", STEM_DECIMAL_ALWAYS, status);
    if (U_FAILURE(status)) { return; }

    // Section 2:
    b.add(u"round-increment", STEM_ROUND_INCREMENT, status);
    b.add(u"measure-unit", STEM_MEASURE_UNIT, status);
    b.add(u"per-measure-unit", STEM_PER_MEASURE_UNIT, status);
    b.add(u"currency", STEM_CURRENCY, status);
    b.add(u"integer-width", STEM_INTEGER_WIDTH, status);
    b.add(u"numbering-system", STEM_NUMBERING_SYSTEM, status);
    if (U_FAILURE(status)) { return; }

    // Build the CharsTrie
    // TODO: Use SLOW or FAST here?
    UnicodeString result;
    b.buildUnicodeString(USTRINGTRIE_BUILD_FAST, result, status);
    if (U_FAILURE(status)) { return; }

    // Copy the result into the global constant pointer
    size_t numBytes = result.length() * sizeof(char16_t);
    kSerializedStemTrie = static_cast<char16_t*>(uprv_malloc(numBytes));
    uprv_memcpy(kSerializedStemTrie, result.getBuffer(), numBytes);
}


#define CHECK_NULL(seen, field, status) void; /* for auto-format line wraping */ \
{ \
    if ((seen).field) { \
        (status) = U_NUMBER_SKELETON_SYNTAX_ERROR; \
        return STATE_NULL; \
    } \
    (seen).field = true; \
}


} // anonymous namespace


Notation stem_to_object::notation(skeleton::StemEnum stem) {
    switch (stem) {
        case STEM_COMPACT_SHORT:
            return Notation::compactShort();
        case STEM_COMPACT_LONG:
            return Notation::compactLong();
        case STEM_SCIENTIFIC:
            return Notation::scientific();
        case STEM_ENGINEERING:
            return Notation::engineering();
        case STEM_NOTATION_SIMPLE:
            return Notation::simple();
        default:
            U_ASSERT(false);
    }
}

MeasureUnit stem_to_object::unit(skeleton::StemEnum stem) {
    switch (stem) {
        case STEM_BASE_UNIT:
            // Slicing is okay
            return NoUnit::base(); // NOLINT
        case STEM_PERCENT:
            // Slicing is okay
            return NoUnit::percent(); // NOLINT
        case STEM_PERMILLE:
            // Slicing is okay
            return NoUnit::permille(); // NOLINT
        default:
            U_ASSERT(false);
    }
}

Rounder stem_to_object::rounder(skeleton::StemEnum stem) {
    switch (stem) {
        case STEM_ROUND_INTEGER:
            return Rounder::integer();
        case STEM_ROUND_UNLIMITED:
            return Rounder::unlimited();
        case STEM_ROUND_CURRENCY_STANDARD:
            return Rounder::currency(UCURR_USAGE_STANDARD);
        case STEM_ROUND_CURRENCY_CASH:
            return Rounder::currency(UCURR_USAGE_CASH);
        default:
            U_ASSERT(false);
    }
}

UGroupingStrategy stem_to_object::groupingStrategy(skeleton::StemEnum stem) {
    switch (stem) {
        case STEM_GROUP_OFF:
            return UNUM_GROUPING_OFF;
        case STEM_GROUP_MIN2:
            return UNUM_GROUPING_MIN2;
        case STEM_GROUP_AUTO:
            return UNUM_GROUPING_AUTO;
        case STEM_GROUP_ON_ALIGNED:
            return UNUM_GROUPING_ON_ALIGNED;
        case STEM_GROUP_THOUSANDS:
            return UNUM_GROUPING_THOUSANDS;
        default:
            return UNUM_GROUPING_COUNT; // for objects, throw; for enums, return COUNT
    }
}

UNumberUnitWidth stem_to_object::unitWidth(skeleton::StemEnum stem) {
    switch (stem) {
        case STEM_UNIT_WIDTH_NARROW:
            return UNUM_UNIT_WIDTH_NARROW;
        case STEM_UNIT_WIDTH_SHORT:
            return UNUM_UNIT_WIDTH_SHORT;
        case STEM_UNIT_WIDTH_FULL_NAME:
            return UNUM_UNIT_WIDTH_FULL_NAME;
        case STEM_UNIT_WIDTH_ISO_CODE:
            return UNUM_UNIT_WIDTH_ISO_CODE;
        case STEM_UNIT_WIDTH_HIDDEN:
            return UNUM_UNIT_WIDTH_HIDDEN;
        default:
            return UNUM_UNIT_WIDTH_COUNT; // for objects, throw; for enums, return COUNT
    }
}

UNumberSignDisplay stem_to_object::signDisplay(skeleton::StemEnum stem) {
    switch (stem) {
        case STEM_SIGN_AUTO:
            return UNUM_SIGN_AUTO;
        case STEM_SIGN_ALWAYS:
            return UNUM_SIGN_ALWAYS;
        case STEM_SIGN_NEVER:
            return UNUM_SIGN_NEVER;
        case STEM_SIGN_ACCOUNTING:
            return UNUM_SIGN_ACCOUNTING;
        case STEM_SIGN_ACCOUNTING_ALWAYS:
            return UNUM_SIGN_ACCOUNTING_ALWAYS;
        case STEM_SIGN_EXCEPT_ZERO:
            return UNUM_SIGN_EXCEPT_ZERO;
        case STEM_SIGN_ACCOUNTING_EXCEPT_ZERO:
            return UNUM_SIGN_ACCOUNTING_EXCEPT_ZERO;
        default:
            return UNUM_SIGN_COUNT; // for objects, throw; for enums, return COUNT
    }
}

UNumberDecimalSeparatorDisplay stem_to_object::decimalSeparatorDisplay(skeleton::StemEnum stem) {
    switch (stem) {
        case STEM_DECIMAL_AUTO:
            return UNUM_DECIMAL_SEPARATOR_AUTO;
        case STEM_DECIMAL_ALWAYS:
            return UNUM_DECIMAL_SEPARATOR_ALWAYS;
        default:
            return UNUM_DECIMAL_SEPARATOR_COUNT; // for objects, throw; for enums, return COUNT
    }
}


void enum_to_stem_string::groupingStrategy(UGroupingStrategy value, UnicodeString& sb) {
    switch (value) {
        case UNUM_GROUPING_OFF:
            sb.append(u"group-off", -1);
            break;
        case UNUM_GROUPING_MIN2:
            sb.append(u"group-min2", -1);
            break;
        case UNUM_GROUPING_AUTO:
            sb.append(u"group-auto", -1);
            break;
        case UNUM_GROUPING_ON_ALIGNED:
            sb.append(u"group-on-aligned", -1);
            break;
        case UNUM_GROUPING_THOUSANDS:
            sb.append(u"group-thousands", -1);
            break;
        default:
            U_ASSERT(false);
    }
}

void enum_to_stem_string::unitWidth(UNumberUnitWidth value, UnicodeString& sb) {
    switch (value) {
        case UNUM_UNIT_WIDTH_NARROW:
            sb.append(u"unit-width-narrow", -1);
            break;
        case UNUM_UNIT_WIDTH_SHORT:
            sb.append(u"unit-width-short", -1);
            break;
        case UNUM_UNIT_WIDTH_FULL_NAME:
            sb.append(u"unit-width-full-name", -1);
            break;
        case UNUM_UNIT_WIDTH_ISO_CODE:
            sb.append(u"unit-width-iso-code", -1);
            break;
        case UNUM_UNIT_WIDTH_HIDDEN:
            sb.append(u"unit-width-hidden", -1);
            break;
        default:
            U_ASSERT(false);
    }
}

void enum_to_stem_string::signDisplay(UNumberSignDisplay value, UnicodeString& sb) {
    switch (value) {
        case UNUM_SIGN_AUTO:
            sb.append(u"sign-auto", -1);
            break;
        case UNUM_SIGN_ALWAYS:
            sb.append(u"sign-always", -1);
            break;
        case UNUM_SIGN_NEVER:
            sb.append(u"sign-never", -1);
            break;
        case UNUM_SIGN_ACCOUNTING:
            sb.append(u"sign-accounting", -1);
            break;
        case UNUM_SIGN_ACCOUNTING_ALWAYS:
            sb.append(u"sign-accounting-always", -1);
            break;
        case UNUM_SIGN_EXCEPT_ZERO:
            sb.append(u"sign-except-zero", -1);
            break;
        case UNUM_SIGN_ACCOUNTING_EXCEPT_ZERO:
            sb.append(u"sign-accounting-except-zero", -1);
            break;
        default:
            U_ASSERT(false);
    }
}

void
enum_to_stem_string::decimalSeparatorDisplay(UNumberDecimalSeparatorDisplay value, UnicodeString& sb) {
    switch (value) {
        case UNUM_DECIMAL_SEPARATOR_AUTO:
            sb.append(u"decimal-auto", -1);
            break;
        case UNUM_DECIMAL_SEPARATOR_ALWAYS:
            sb.append(u"decimal-always", -1);
            break;
        default:
            U_ASSERT(false);
    }
}


UnlocalizedNumberFormatter skeleton::create(const UnicodeString& skeletonString, UErrorCode& status) {
    if (U_FAILURE(status)) { return {}; }
    umtx_initOnce(gNumberSkeletonsInitOnce, &initNumberSkeletons, status);
    if (U_FAILURE(status)) { return {}; }

    MacroProps macros = parseSkeleton(skeletonString, status);
    return NumberFormatter::with().macros(macros);
}

UnicodeString skeleton::generate(const MacroProps& macros, UErrorCode& status) {
    if (U_FAILURE(status)) { return {}; }
    umtx_initOnce(gNumberSkeletonsInitOnce, &initNumberSkeletons, status);
    if (U_FAILURE(status)) { return {}; }

    UnicodeString sb;
    GeneratorHelpers::generateSkeleton(macros, sb, status);
    return sb;
}

MacroProps skeleton::parseSkeleton(const UnicodeString& skeletonString, UErrorCode& status) {
    // Add a trailing whitespace to the end of the skeleton string to make code cleaner.
    UnicodeString tempSkeletonString(skeletonString);
    tempSkeletonString.append(u' ');

    SeenMacroProps seen;
    MacroProps macros;
    StringSegment segment(skeletonString, false);
    UCharsTrie stemTrie(kSerializedStemTrie);
    ParseState stem = STATE_NULL;
    int offset = 0;

    // Primary skeleton parse loop:
    while (offset < segment.length()) {
        int cp = segment.codePointAt(offset);
        bool isTokenSeparator = PatternProps::isWhiteSpace(cp);
        bool isOptionSeparator = (cp == u'/');

        if (!isTokenSeparator && !isOptionSeparator) {
            // Non-separator token; consume it.
            offset += U16_LENGTH(cp);
            if (stem == STATE_NULL) {
                // We are currently consuming a stem.
                // Go to the next state in the stem trie.
                stemTrie.nextForCodePoint(cp);
            }
            continue;
        }

        // We are looking at a token or option separator.
        // If the segment is nonempty, parse it and reset the segment.
        // Otherwise, make sure it is a valid repeating separator.
        if (offset != 0) {
            segment.setLength(offset);
            if (stem == STATE_NULL) {
                // The first separator after the start of a token. Parse it as a stem.
                stem = parseStem(segment, stemTrie, seen, macros, status);
                stemTrie.reset();
            } else {
                // A separator after the first separator of a token. Parse it as an option.
                stem = parseOption(stem, segment, macros, status);
            }
            segment.resetLength();

            // Consume the segment:
            segment.adjustOffset(offset);
            offset = 0;

        } else if (stem != STATE_NULL) {
            // A separator ('/' or whitespace) following an option separator ('/')
            // segment.setLength(U16_LENGTH(cp)); // for error message
            // throw new SkeletonSyntaxException("Unexpected separator character", segment);
            status = U_NUMBER_SKELETON_SYNTAX_ERROR;
            return macros;

        } else {
            // Two spaces in a row; this is OK.
        }

        // Does the current stem forbid options?
        if (isOptionSeparator && stem == STATE_NULL) {
            // segment.setLength(U16_LENGTH(cp)); // for error message
            // throw new SkeletonSyntaxException("Unexpected option separator", segment);
            status = U_NUMBER_SKELETON_SYNTAX_ERROR;
            return macros;
        }

        // Does the current stem require an option?
        if (isTokenSeparator && stem != STATE_NULL) {
            switch (stem) {
                case STATE_INCREMENT_ROUNDER:
                case STATE_MEASURE_UNIT:
                case STATE_PER_MEASURE_UNIT:
                case STATE_CURRENCY_UNIT:
                case STATE_INTEGER_WIDTH:
                case STATE_NUMBERING_SYSTEM:
                    // segment.setLength(U16_LENGTH(cp)); // for error message
                    // throw new SkeletonSyntaxException("Stem requires an option", segment);
                    status = U_NUMBER_SKELETON_SYNTAX_ERROR;
                    return macros;
                default:
                    break;
            }
            stem = STATE_NULL;
        }

        // Consume the separator:
        segment.adjustOffset(U16_LENGTH(cp));
    }
    U_ASSERT(stem == STATE_NULL);
    return macros;
}

ParseState
skeleton::parseStem(const StringSegment& segment, const UCharsTrie& stemTrie, SeenMacroProps& seen,
                    MacroProps& macros, UErrorCode& status) {
    // First check for "blueprint" stems, which start with a "signal char"
    switch (segment.charAt(0)) {
        case u'.':
        CHECK_NULL(seen, rounder, status);
            blueprint_helpers::parseFractionStem(segment, macros, status);
            return STATE_FRACTION_ROUNDER;
        case u'@':
        CHECK_NULL(seen, rounder, status);
            blueprint_helpers::parseDigitsStem(segment, macros, status);
            return STATE_NULL;
    }

    // Now look at the stemsTrie, which is already be pointing at our stem.
    UStringTrieResult stemResult = stemTrie.current();

    if (stemResult != USTRINGTRIE_INTERMEDIATE_VALUE && stemResult != USTRINGTRIE_FINAL_VALUE) {
        // throw new SkeletonSyntaxException("Unknown stem", segment);
        status = U_NUMBER_SKELETON_SYNTAX_ERROR;
        return STATE_NULL;
    }

    auto stem = static_cast<StemEnum>(stemTrie.getValue());
    switch (stem) {

        // Stems with meaning on their own, not requiring an option:

        case STEM_COMPACT_SHORT:
        case STEM_COMPACT_LONG:
        case STEM_SCIENTIFIC:
        case STEM_ENGINEERING:
        case STEM_NOTATION_SIMPLE:
        CHECK_NULL(seen, notation, status);
            macros.notation = stem_to_object::notation(stem);
            switch (stem) {
                case STEM_SCIENTIFIC:
                case STEM_ENGINEERING:
                    return STATE_SCIENTIFIC; // allows for scientific options
                default:
                    return STATE_NULL;
            }

        case STEM_BASE_UNIT:
        case STEM_PERCENT:
        case STEM_PERMILLE:
        CHECK_NULL(seen, unit, status);
            macros.unit = stem_to_object::unit(stem);
            return STATE_NULL;

        case STEM_ROUND_INTEGER:
        case STEM_ROUND_UNLIMITED:
        case STEM_ROUND_CURRENCY_STANDARD:
        case STEM_ROUND_CURRENCY_CASH:
        CHECK_NULL(seen, rounder, status);
            macros.rounder = stem_to_object::rounder(stem);
            switch (stem) {
                case STEM_ROUND_INTEGER:
                    return STATE_FRACTION_ROUNDER; // allows for "round-integer/@##"
                default:
                    return STATE_ROUNDER; // allows for rounding mode options
            }

        case STEM_GROUP_OFF:
        case STEM_GROUP_MIN2:
        case STEM_GROUP_AUTO:
        case STEM_GROUP_ON_ALIGNED:
        case STEM_GROUP_THOUSANDS:
        CHECK_NULL(seen, grouper, status);
            macros.grouper = Grouper::forStrategy(stem_to_object::groupingStrategy(stem));
            return STATE_NULL;

        case STEM_LATIN:
        CHECK_NULL(seen, symbols, status);
            macros.symbols.setTo(NumberingSystem::createInstanceByName("latn", status));
            return STATE_NULL;

        case STEM_UNIT_WIDTH_NARROW:
        case STEM_UNIT_WIDTH_SHORT:
        case STEM_UNIT_WIDTH_FULL_NAME:
        case STEM_UNIT_WIDTH_ISO_CODE:
        case STEM_UNIT_WIDTH_HIDDEN:
        CHECK_NULL(seen, unitWidth, status);
            macros.unitWidth = stem_to_object::unitWidth(stem);
            return STATE_NULL;

        case STEM_SIGN_AUTO:
        case STEM_SIGN_ALWAYS:
        case STEM_SIGN_NEVER:
        case STEM_SIGN_ACCOUNTING:
        case STEM_SIGN_ACCOUNTING_ALWAYS:
        case STEM_SIGN_EXCEPT_ZERO:
        case STEM_SIGN_ACCOUNTING_EXCEPT_ZERO:
        CHECK_NULL(seen, sign, status);
            macros.sign = stem_to_object::signDisplay(stem);
            return STATE_NULL;

        case STEM_DECIMAL_AUTO:
        case STEM_DECIMAL_ALWAYS:
        CHECK_NULL(seen, decimal, status);
            macros.decimal = stem_to_object::decimalSeparatorDisplay(stem);
            return STATE_NULL;

            // Stems requiring an option:

        case STEM_ROUND_INCREMENT:
        CHECK_NULL(seen, rounder, status);
            return STATE_INCREMENT_ROUNDER;

        case STEM_MEASURE_UNIT:
        CHECK_NULL(seen, unit, status);
            return STATE_MEASURE_UNIT;

        case STEM_PER_MEASURE_UNIT:
        CHECK_NULL(seen, perUnit, status);
            return STATE_PER_MEASURE_UNIT;

        case STEM_CURRENCY:
        CHECK_NULL(seen, unit, status);
            return STATE_CURRENCY_UNIT;

        case STEM_INTEGER_WIDTH:
        CHECK_NULL(seen, integerWidth, status);
            return STATE_INTEGER_WIDTH;

        case STEM_NUMBERING_SYSTEM:
        CHECK_NULL(seen, symbols, status);
            return STATE_NUMBERING_SYSTEM;

        default:
            U_ASSERT(false);
    }
}

ParseState skeleton::parseOption(ParseState stem, const StringSegment& segment, MacroProps& macros,
                                 UErrorCode& status) {

    ///// Required options: /////

    switch (stem) {
        case STATE_CURRENCY_UNIT:
            blueprint_helpers::parseCurrencyOption(segment, macros, status);
            return STATE_NULL;
        case STATE_MEASURE_UNIT:
            blueprint_helpers::parseMeasureUnitOption(segment, macros, status);
            return STATE_NULL;
        case STATE_PER_MEASURE_UNIT:
            blueprint_helpers::parseMeasurePerUnitOption(segment, macros, status);
            return STATE_NULL;
        case STATE_INCREMENT_ROUNDER:
            blueprint_helpers::parseIncrementOption(segment, macros, status);
            return STATE_ROUNDER;
        case STATE_INTEGER_WIDTH:
            blueprint_helpers::parseIntegerWidthOption(segment, macros, status);
            return STATE_NULL;
        case STATE_NUMBERING_SYSTEM:
            blueprint_helpers::parseNumberingSystemOption(segment, macros, status);
            return STATE_NULL;
        default:
            break;
    }

    ///// Non-required options: /////

    // Scientific options
    switch (stem) {
        case STATE_SCIENTIFIC:
            if (blueprint_helpers::parseExponentWidthOption(segment, macros, status)) {
                return STATE_SCIENTIFIC;
            }
            if (blueprint_helpers::parseExponentSignOption(segment, macros, status)) {
                return STATE_SCIENTIFIC;
            }
            break;
        default:
            break;
    }

    // Frac-sig option
    switch (stem) {
        case STATE_FRACTION_ROUNDER:
            if (blueprint_helpers::parseFracSigOption(segment, macros, status)) {
                return STATE_ROUNDER;
            }
            break;
        default:
            break;
    }

    // Rounding mode option
    switch (stem) {
        case STATE_ROUNDER:
        case STATE_FRACTION_ROUNDER:
            if (blueprint_helpers::parseRoundingModeOption(segment, macros, status)) {
                return STATE_ROUNDER;
            }
            break;
        default:
            break;
    }

    // Unknown option
    // throw new SkeletonSyntaxException("Invalid option", segment);
    status = U_NUMBER_SKELETON_SYNTAX_ERROR;
    return STATE_NULL;
}

void GeneratorHelpers::generateSkeleton(const MacroProps& macros, UnicodeString& sb, UErrorCode& status) {
    // Supported options
    if (GeneratorHelpers::notation(macros, sb, status)) {
        sb.append(u' ');
    }
    if (U_FAILURE(status)) { return; }
    if (GeneratorHelpers::unit(macros, sb, status)) {
        sb.append(u' ');
    }
    if (U_FAILURE(status)) { return; }
    if (GeneratorHelpers::perUnit(macros, sb, status)) {
        sb.append(u' ');
    }
    if (U_FAILURE(status)) { return; }
    if (GeneratorHelpers::rounding(macros, sb, status)) {
        sb.append(u' ');
    }
    if (U_FAILURE(status)) { return; }
    if (GeneratorHelpers::grouping(macros, sb, status)) {
        sb.append(u' ');
    }
    if (U_FAILURE(status)) { return; }
    if (GeneratorHelpers::integerWidth(macros, sb, status)) {
        sb.append(u' ');
    }
    if (U_FAILURE(status)) { return; }
    if (GeneratorHelpers::symbols(macros, sb, status)) {
        sb.append(u' ');
    }
    if (U_FAILURE(status)) { return; }
    if (GeneratorHelpers::unitWidth(macros, sb, status)) {
        sb.append(u' ');
    }
    if (U_FAILURE(status)) { return; }
    if (GeneratorHelpers::sign(macros, sb, status)) {
        sb.append(u' ');
    }
    if (U_FAILURE(status)) { return; }
    if (GeneratorHelpers::decimal(macros, sb, status)) {
        sb.append(u' ');
    }
    if (U_FAILURE(status)) { return; }

    // Unsupported options
    if (!macros.padder.isBogus()) {
        status = U_UNSUPPORTED_ERROR;
        return;
    }
    if (macros.affixProvider != nullptr) {
        status = U_UNSUPPORTED_ERROR;
        return;
    }
    if (macros.multiplier.isValid()) {
        status = U_UNSUPPORTED_ERROR;
        return;
    }
    if (macros.rules != nullptr) {
        status = U_UNSUPPORTED_ERROR;
        return;
    }
    if (macros.currencySymbols != nullptr) {
        status = U_UNSUPPORTED_ERROR;
        return;
    }

    // Remove the trailing space
    if (sb.length() > 0) {
        sb.truncate(sb.length() - 1);
    }
}


bool GeneratorHelpers::notation(const MacroProps& macros, UnicodeString& sb, UErrorCode& status) {
    if (macros.notation.fType == Notation::NTN_COMPACT) {
        UNumberCompactStyle style = macros.notation.fUnion.compactStyle;
        if (style == UNumberCompactStyle::UNUM_LONG) {
            sb.append(u"compact-long", -1);
            return true;
        } else if (style == UNumberCompactStyle::UNUM_SHORT) {
            sb.append(u"compact-short", -1);
            return true;
        } else {
            // Compact notation generated from custom data (not supported in skeleton)
            // The other compact notations are literals
            status = U_UNSUPPORTED_ERROR;
            return false;
        }
    } else if (macros.notation.fType == Notation::NTN_SCIENTIFIC) {
        const Notation::ScientificSettings& impl = macros.notation.fUnion.scientific;
        if (impl.fEngineeringInterval == 3) {
            sb.append(u"engineering", -1);
        } else {
            sb.append(u"scientific", -1);
        }
        if (impl.fMinExponentDigits > 1) {
            sb.append(u'/');
            blueprint_helpers::generateExponentWidthOption(impl.fMinExponentDigits, sb, status);
        }
        if (impl.fExponentSignDisplay != UNUM_SIGN_AUTO) {
            sb.append(u'/');
            enum_to_stem_string::signDisplay(impl.fExponentSignDisplay, sb);
        }
        return true;
    } else {
        // Default value is not shown in normalized form
        return false;
    }
}

bool GeneratorHelpers::unit(const MacroProps& macros, UnicodeString& sb, UErrorCode& status) {
    if (unitIsCurrency(macros.unit)) {
        sb.append(u"currency/", -1);
        blueprint_helpers::generateCurrencyOption({macros.unit, status}, sb, status);
        return true;
    } else if (unitIsNoUnit(macros.unit)) {
        if (unitIsPercent(macros.unit)) {
            sb.append(u"percent", -1);
            return true;
        } else if (unitIsPermille(macros.unit)) {
            sb.append(u"permille", -1);
            return true;
        } else {
            // Default value is not shown in normalized form
            return false;
        }
    } else {
        sb.append(u"measure-unit/", -1);
        blueprint_helpers::generateMeasureUnitOption(macros.unit, sb, status);
        return true;
    }
}

bool GeneratorHelpers::perUnit(const MacroProps& macros, UnicodeString& sb, UErrorCode& status) {
    // Per-units are currently expected to be only MeasureUnits.
    if (unitIsCurrency(macros.perUnit) || unitIsNoUnit(macros.perUnit)) {
        status = U_UNSUPPORTED_ERROR;
    } else {
        sb.append(u"per-measure-unit/", -1);
        blueprint_helpers::generateMeasureUnitOption(macros.perUnit, sb, status);
        return true;
    }
}

bool GeneratorHelpers::rounding(const MacroProps& macros, UnicodeString& sb, UErrorCode& status) {
    if (macros.rounder.fType == Rounder::RND_NONE) {
        sb.append(u"round-unlimited", -1);
    } else if (macros.rounder.fType == Rounder::RND_FRACTION) {
        const Rounder::FractionSignificantSettings& impl = macros.rounder.fUnion.fracSig;
        blueprint_helpers::generateFractionStem(impl.fMinFrac, impl.fMaxFrac, sb, status);
    } else if (macros.rounder.fType == Rounder::RND_SIGNIFICANT) {
        const Rounder::FractionSignificantSettings& impl = macros.rounder.fUnion.fracSig;
        blueprint_helpers::generateDigitsStem(impl.fMinSig, impl.fMaxSig, sb, status);
    } else if (macros.rounder.fType == Rounder::RND_FRACTION_SIGNIFICANT) {
        const Rounder::FractionSignificantSettings& impl = macros.rounder.fUnion.fracSig;
        blueprint_helpers::generateFractionStem(impl.fMinFrac, impl.fMaxFrac, sb, status);
        sb.append(u'/');
        if (impl.fMinSig == -1) {
            blueprint_helpers::generateDigitsStem(1, impl.fMaxSig, sb, status);
        } else {
            blueprint_helpers::generateDigitsStem(impl.fMinSig, -1, sb, status);
        }
    } else if (macros.rounder.fType == Rounder::RND_INCREMENT) {
        const Rounder::IncrementSettings& impl = macros.rounder.fUnion.increment;
        sb.append(u"round-increment/", -1);
        blueprint_helpers::generateIncrementOption(impl.fIncrement, sb, status);
    } else if (macros.rounder.fType == Rounder::RND_CURRENCY) {
        UCurrencyUsage usage = macros.rounder.fUnion.currencyUsage;
        if (usage == UCURR_USAGE_STANDARD) {
            sb.append(u"round-currency-standard", -1);
        } else {
            sb.append(u"round-currency-cash", -1);
        }
    } else {
        // Bogus or Error
        return false;
    }

    // Generate the options
    if (macros.rounder.fRoundingMode != kDefaultMode) {
        sb.append(u'/');
        blueprint_helpers::generateRoundingModeOption(macros.rounder.fRoundingMode, sb, status);
    }

    // NOTE: Always return true for rounding because the default value depends on other options.
    return true;
}

bool GeneratorHelpers::grouping(const MacroProps& macros, UnicodeString& sb, UErrorCode& status) {
    if (macros.grouper.isBogus() || macros.grouper.fStrategy == UNUM_GROUPING_COUNT) {
        status = U_UNSUPPORTED_ERROR;
    } else if (macros.grouper.fStrategy == UNUM_GROUPING_AUTO) {
        return false; // Default value
    } else {
        enum_to_stem_string::groupingStrategy(macros.grouper.fStrategy, sb);
    }
}

bool GeneratorHelpers::integerWidth(const MacroProps& macros, UnicodeString& sb, UErrorCode& status) {
    if (macros.integerWidth.fHasError || macros.integerWidth == IntegerWidth::standard()) {
        // Error or Default
        return false;
    }
    sb.append(u"integer-width/", -1);
    blueprint_helpers::generateIntegerWidthOption(
            macros.integerWidth.fUnion.minMaxInt.fMinInt,
            macros.integerWidth.fUnion.minMaxInt.fMaxInt,
            sb,
            status);
    return true;
}

bool GeneratorHelpers::symbols(const MacroProps& macros, UnicodeString& sb, UErrorCode& status) {
    if (macros.symbols.isNumberingSystem()) {
        const NumberingSystem& ns = *macros.symbols.getNumberingSystem();
        if (uprv_strcmp(ns.getName(), "latn") == 0) {
            sb.append(u"latin", -1);
        } else {
            sb.append(u"numbering-system/", -1);
            blueprint_helpers::generateNumberingSystemOption(ns, sb, status);
        }
        return true;
    } else if (macros.symbols.isDecimalFormatSymbols()) {
        status = U_UNSUPPORTED_ERROR;
        return false;
    } else {
        // No custom symbols
        return false;
    }
}

bool GeneratorHelpers::unitWidth(const MacroProps& macros, UnicodeString& sb, UErrorCode& status) {
    if (macros.unitWidth == UNUM_UNIT_WIDTH_SHORT || macros.unitWidth == UNUM_UNIT_WIDTH_COUNT) {
        return false; // Default or Bogus
    }
    enum_to_stem_string::unitWidth(macros.unitWidth, sb);
    return true;
}

bool GeneratorHelpers::sign(const MacroProps& macros, UnicodeString& sb, UErrorCode& status) {
    if (macros.sign == UNUM_SIGN_AUTO || macros.sign == UNUM_SIGN_COUNT) {
        return false; // Default or Bogus
    }
    enum_to_stem_string::signDisplay(macros.sign, sb);
    return true;
}

bool GeneratorHelpers::decimal(const MacroProps& macros, UnicodeString& sb, UErrorCode& status) {
    if (macros.decimal == UNUM_DECIMAL_SEPARATOR_AUTO || macros.decimal == UNUM_DECIMAL_SEPARATOR_COUNT) {
        return false; // Default or Bogus
    }
    enum_to_stem_string::decimalSeparatorDisplay(macros.decimal, sb);
    return true;
}


#endif /* #if !UCONFIG_NO_FORMATTING */
