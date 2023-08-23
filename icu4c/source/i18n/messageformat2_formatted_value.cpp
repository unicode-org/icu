// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/messageformat2_formatted_value.h"
#include "unicode/messageformat2_data_model.h"
#include "uvector.h" // U_ASSERT

U_NAMESPACE_BEGIN namespace message2 {

UnicodeString FormattedString::toString(Locale locale, UErrorCode& status) const {
    (void) locale;
    (void) status;

    return getString();
}

UnicodeString FormattedNumber::toString(Locale locale, UErrorCode& status) const {
    (void) locale;

    return num.toString(status);
}

UnicodeString FormattingInputNotNull::toString(Locale locale, UErrorCode& status) const {
    // "Finalize" formatting, e.g. applying default formatters
    // to dates/numbers
    LocalPointer<FullyFormatted> formatted(FullyFormatted::format(locale, *this, status));
    if (U_FAILURE(status)) {
        return UnicodeString();
    }
    return formatted->toString(locale, status);
}

Fallback::Fallback(const Text& t) : fallback(t), fallbackAsFmtable(Formattable(t.toString())) {}

FormattedPlaceholder::~FormattedPlaceholder() {}
Fallback::~Fallback() {}
FormattingInput::~FormattingInput() {}
FormattingInputNotNull::~FormattingInputNotNull() {}
FormattedString::~FormattedString() {}
FormattedNumber::~FormattedNumber() {}

/* static */ FullyFormatted* FullyFormatted::format(Locale loc, const FormattingInput& inp, UErrorCode& status) {
    NULL_ON_ERROR(status);
    
    LocalPointer<FullyFormatted> result;
    if (inp.isFallback()) {
        return FullyFormatted::promoteFallback(static_cast<const Fallback&>(inp), status);
    }

    U_ASSERT(inp.hasInput());
    const FormattingInputNotNull& input = static_cast<const FormattingInputNotNull&>(inp); 
    const Formattable& in = input.getInput();

    switch (in.getType()) {
    case Formattable::Type::kDate: {
        result.adoptInstead((FullyFormatted*) formatDateWithDefaults(loc, input, status));
        break;
    }
    case Formattable::Type::kDouble: {
        result.adoptInstead((FullyFormatted*) formatNumberWithDefaults(loc, input, in.getDouble(), status));
        break;
    }
    case Formattable::Type::kLong: {
        result.adoptInstead((FullyFormatted*) formatNumberWithDefaults(loc, input, in.getLong(), status));
        break;
    }
    case Formattable::Type::kInt64: {
        result.adoptInstead((FullyFormatted*) formatNumberWithDefaults(loc, input, in.getInt64(), status));
        break;
    }
    case Formattable::Type::kString: {
        result.adoptInstead(FormattedString::create(input, in.getString(), status));
        break;
    }
    default: {
        // TODO: no default formatters for these. use fallback
        return FullyFormatted::promoteFallback(static_cast<const Fallback&>(input), status);
    }
    }
    NULL_ON_ERROR(status);
    return result.orphan();
}

/* static */ Fallback* Fallback::create(const Text& t, UErrorCode& status) {
    NULL_ON_ERROR(status);
    Fallback* result = new Fallback(t);
    if (result == nullptr) {
        status = U_MEMORY_ALLOCATION_ERROR;
    }
    return result;
}

FormattedNumber* formatNumberWithDefaults(Locale locale, const FormattingInput& savedInput, double toFormat, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);
    return FormattedNumber::create(savedInput, number::NumberFormatter::withLocale(locale).formatDouble(toFormat, errorCode), errorCode);
}

FormattedNumber* formatNumberWithDefaults(Locale locale, const FormattingInput& savedInput, int32_t toFormat, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);
    return FormattedNumber::create(savedInput, number::FormattedNumber(number::NumberFormatter::withLocale(locale).formatInt(toFormat, errorCode)), errorCode);
}

FormattedNumber* formatNumberWithDefaults(Locale locale, const FormattingInput& savedInput, int64_t toFormat, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);
    return FormattedNumber::create(savedInput, number::FormattedNumber(number::NumberFormatter::withLocale(locale).formatInt(toFormat, errorCode)), errorCode);
}

/* static */ DateFormat* FormattedPlaceholder::defaultDateTimeInstance(Locale locale, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);
    LocalPointer<DateFormat> df(DateFormat::createDateTimeInstance(DateFormat::SHORT, DateFormat::SHORT, locale));
    if (!df.isValid()) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }
    return df.orphan();
}

FormattedString* formatDateWithDefaults(Locale locale, const FormattingInput& savedInput, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);
    LocalPointer<DateFormat> df(FormattedPlaceholder::defaultDateTimeInstance(locale, errorCode));
    NULL_ON_ERROR(errorCode);
    UnicodeString result;
    df->format(savedInput.getInput(), result, 0, errorCode);
    return FormattedString::create(savedInput, result, errorCode);
}

/* static */ FullyFormatted* FullyFormatted::promoteFallback(const Fallback& fallback, UErrorCode& status) {
    NULL_ON_ERROR(status);
    UnicodeString fallbackStr = fallback.toString();
    Formattable fmtable(fallbackStr);
    FormattingInputNotNull input(fallback.fallback, fallback.fallbackAsFmtable);
    return FormattedString::create(input, fallbackStr, status);
}

UnicodeString Fallback::toString() const {
    UnicodeString result;
    result += LEFT_CURLY_BRACE;
    result += fallback.toString();
    result += RIGHT_CURLY_BRACE;
    return result;
}

UnicodeString Fallback::toString(Locale l, UErrorCode& s) const {
    (void) l;
    (void) s;
    return toString();
}

 const FullyFormatted& FormattedPlaceholder::getOutput() const {
     U_ASSERT(hasOutput());
     return static_cast<const FullyFormatted&>(*this);
 }

} // namespace message2
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */
