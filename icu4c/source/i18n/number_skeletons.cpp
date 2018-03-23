// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

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
    generateSkeleton(macros, sb, status);
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


#endif /* #if !UCONFIG_NO_FORMATTING */
