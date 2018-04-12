// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING && !UPRV_INCOMPLETE_CPP11_SUPPORT

// Allow implicit conversion from char16_t* to UnicodeString for this file:
// Helpful in toString methods and elsewhere.
#define UNISTR_FROM_STRING_EXPLICIT

#include "numparse_types.h"
#include "numparse_validators.h"
#include "numparse_unisets.h"

using namespace icu;
using namespace icu::numparse;
using namespace icu::numparse::impl;


void RequireAffixValidator::postProcess(ParsedNumber& result) const {
    if (result.prefix.isBogus() || result.suffix.isBogus()) {
        // We saw a prefix or a suffix but not both. Fail the parse.
        result.flags |= FLAG_FAIL;
    }
}

UnicodeString RequireAffixValidator::toString() const {
    return u"<ReqAffix>";
}


void RequireCurrencyValidator::postProcess(ParsedNumber& result) const {
    if (result.currencyCode[0] == 0) {
        result.flags |= FLAG_FAIL;
    }
}

UnicodeString RequireCurrencyValidator::toString() const {
    return u"<ReqCurrency>";
}


RequireDecimalSeparatorValidator::RequireDecimalSeparatorValidator(bool patternHasDecimalSeparator)
        : fPatternHasDecimalSeparator(patternHasDecimalSeparator) {
}

void RequireDecimalSeparatorValidator::postProcess(ParsedNumber& result) const {
    bool parseHasDecimalSeparator = 0 != (result.flags & FLAG_HAS_DECIMAL_SEPARATOR);
    if (parseHasDecimalSeparator != fPatternHasDecimalSeparator) {
        result.flags |= FLAG_FAIL;
    }
}

UnicodeString RequireDecimalSeparatorValidator::toString() const {
    return u"<ReqDecimal>";
}


void RequireExponentValidator::postProcess(ParsedNumber& result) const {
    if (0 == (result.flags & FLAG_HAS_EXPONENT)) {
        result.flags |= FLAG_FAIL;
    }
}

UnicodeString RequireExponentValidator::toString() const {
    return u"<ReqExponent>";
}


void RequireNumberValidator::postProcess(ParsedNumber& result) const {
    // Require that a number is matched.
    if (!result.seenNumber()) {
        result.flags |= FLAG_FAIL;
    }
}

UnicodeString RequireNumberValidator::toString() const {
    return u"<ReqNumber>";
}


FlagHandler::FlagHandler(result_flags_t flags)
        : fFlags(flags) {}

void FlagHandler::postProcess(ParsedNumber& result) const {
    result.flags |= fFlags;
}

UnicodeString FlagHandler::toString() const {
    return u"<Flags>";
}


#endif /* #if !UCONFIG_NO_FORMATTING */
