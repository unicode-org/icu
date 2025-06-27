// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_NORMALIZATION

#if !UCONFIG_NO_FORMATTING

#if !UCONFIG_NO_MF2

#include "unicode/messageformat2_formattable.h"
#include "unicode/smpdtfmt.h"
#include "messageformat2_allocation.h"
#include "messageformat2_macros.h"

#include "limits.h"

U_NAMESPACE_BEGIN

namespace message2 {

    Formattable& Formattable::operator=(Formattable other) noexcept {
        swap(*this, other);
        return *this;
    }

    Formattable::Formattable(const Formattable& other) {
        contents = other.contents;
    }

    Formattable Formattable::forDecimal(std::string_view number, UErrorCode &status) {
        Formattable f;
        // The relevant overload of the StringPiece constructor
        // casts the string length to int32_t, so we have to check
        // that the length makes sense
        if (number.size() > INT_MAX) {
            status = U_ILLEGAL_ARGUMENT_ERROR;
        } else {
            f.contents = icu::Formattable(StringPiece(number), status);
        }
        return f;
    }

    UFormattableType Formattable::getType() const {
        if (std::holds_alternative<double>(contents)) {
            return UFMT_DOUBLE;
        }
        if (std::holds_alternative<int64_t>(contents)) {
            return UFMT_INT64;
        }
        if (std::holds_alternative<UnicodeString>(contents)) {
            return UFMT_STRING;
        }
        if (isDecimal()) {
            switch (std::get_if<icu::Formattable>(&contents)->getType()) {
            case icu::Formattable::Type::kLong: {
                return UFMT_LONG;
            }
            case icu::Formattable::Type::kDouble: {
                return UFMT_DOUBLE;
            }
            default: {
                return UFMT_INT64;
            }
            }
        }
        if (isDate()) {
            return UFMT_DATE;
        }
        if (std::holds_alternative<const FormattableObject*>(contents)) {
            return UFMT_OBJECT;
        }
        return UFMT_ARRAY;
    }

    const Formattable* Formattable::getArray(int32_t& len, UErrorCode& status) const {
        NULL_ON_ERROR(status);

        if (getType() != UFMT_ARRAY) {
            len = 0;
            status = U_ILLEGAL_ARGUMENT_ERROR;
            return nullptr;
        }
        const std::pair<const Formattable*, int32_t>& p = *std::get_if<std::pair<const Formattable*, int32_t>>(&contents);
        U_ASSERT(p.first != nullptr);
        len = p.second;
        return p.first;
    }

    int64_t Formattable::getInt64(UErrorCode& status) const {
        if (isDecimal() && isNumeric()) {
            return std::get_if<icu::Formattable>(&contents)->getInt64(status);
        }

        switch (getType()) {
        case UFMT_LONG:
        case UFMT_INT64: {
            return *std::get_if<int64_t>(&contents);
        }
        case UFMT_DOUBLE: {
            return icu::Formattable(*std::get_if<double>(&contents)).getInt64(status);
        }
        default: {
            status = U_INVALID_FORMAT_ERROR;
            return 0;
        }
        }
    }

    icu::Formattable Formattable::asICUFormattable(UErrorCode& status) const {
        if (U_FAILURE(status)) {
            return {};
        }
        // Type must not be UFMT_ARRAY or UFMT_OBJECT
        if (getType() == UFMT_ARRAY || getType() == UFMT_OBJECT) {
            status = U_ILLEGAL_ARGUMENT_ERROR;
            return {};
        }

        if (isDecimal()) {
            return *std::get_if<icu::Formattable>(&contents);
        }

        switch (getType()) {
        case UFMT_DATE: {
            return icu::Formattable(*std::get_if<double>(&contents), icu::Formattable::kIsDate);
        }
        case UFMT_DOUBLE: {
            return icu::Formattable(*std::get_if<double>(&contents));
        }
        case UFMT_LONG: {
            return icu::Formattable(static_cast<int32_t>(*std::get_if<double>(&contents)));
        }
        case UFMT_INT64: {
            return icu::Formattable(*std::get_if<int64_t>(&contents));
        }
        case UFMT_STRING: {
            return icu::Formattable(*std::get_if<UnicodeString>(&contents));
        }
        default: {
            // Already checked for UFMT_ARRAY and UFMT_OBJECT
            return icu::Formattable();
        }
        }
    }

    Formattable::~Formattable() {}

    FormattableObject::~FormattableObject() {}

    FormattedMessage::~FormattedMessage() {}

    FormattedValue::FormattedValue(const UnicodeString& s) {
        type = kString;
        stringOutput = std::move(s);
    }

    FormattedValue::FormattedValue(number::FormattedNumber&& n) {
        type = kNumber;
        numberOutput = std::move(n);
    }

    FormattedValue& FormattedValue::operator=(FormattedValue&& other) noexcept {
        type = other.type;
        if (type == kString) {
            stringOutput = std::move(other.stringOutput);
        } else {
            numberOutput = std::move(other.numberOutput);
        }
        return *this;
    }

    FormattedValue::~FormattedValue() {}

    // FormattedPlaceholder
    // -------------------


    FormattedPlaceholder& FormattedPlaceholder::operator=(FormattedPlaceholder&& other) noexcept {
        origin = other.origin;
        source = other.source;
        formatted = std::move(other.formatted);
        previousOptions = std::move(other.previousOptions);
        fallback = other.fallback;
        return *this;
    }

    const Formattable* FormattedPlaceholder::getSource(UErrorCode& errorCode) const {
        if (U_SUCCESS(errorCode)) {
            if (origin != kNull) {
                return &source;
            } else {
                errorCode = U_ILLEGAL_ARGUMENT_ERROR;
            }
        }
        return nullptr;
    }

    FormattedPlaceholder FormattedPlaceholder::withResult(FormattedValue&& result) {
        formatted = std::move(result);
        origin = kFunctionResult;
        return std::move(*this);
    }

    FormattedPlaceholder FormattedPlaceholder::withResultAndOptions(FormattedValue&& result,
                                                                    FunctionOptions&& opts,
                                                                    UErrorCode& status) {
        if (U_FAILURE(status)) {
            return {};
        }
        formatted = std::move(result);
        origin = kFunctionResult;
        previousOptions = std::move(opts);
        return std::move(*this);
    }

    FormattedPlaceholder::FormattedPlaceholder(const FormattedPlaceholder& input,
                                               FunctionOptions&& opts,
                                               FormattedValue&& output)
        : fallback(input.fallback),
          source(input.source),
          formatted(std::move(output)),
          previousOptions(std::move(opts)),
          origin(kFunctionResult) {}

    FormattedPlaceholder::FormattedPlaceholder(const FormattedPlaceholder& input,
                                               FormattedValue&& output)
        : fallback(input.fallback),
          source(input.source),
          formatted(std::move(output)),
          origin(kFunctionResult) {}

    FormattedPlaceholder::FormattedPlaceholder(const Formattable& input,
                                               const UnicodeString& fb)
        : fallback(fb), source(input), origin(kArgumentOrLiteral) {}

    FormattedPlaceholder::FormattedPlaceholder() : origin(kNull) {}

    const message2::FunctionOptions& FormattedPlaceholder::getOptions() const {
        return previousOptions;
    }

    FormattedPlaceholder::~FormattedPlaceholder() {}

    // Default formatters
    // ------------------

    number::FormattedNumber formatNumberWithDefaults(const Locale& locale, double toFormat, UErrorCode& errorCode) {
        return number::NumberFormatter::withLocale(locale).formatDouble(toFormat, errorCode);
    }

    number::FormattedNumber formatNumberWithDefaults(const Locale& locale, int32_t toFormat, UErrorCode& errorCode) {
        return number::NumberFormatter::withLocale(locale).formatInt(toFormat, errorCode);
    }

    number::FormattedNumber formatNumberWithDefaults(const Locale& locale, int64_t toFormat, UErrorCode& errorCode) {
        return number::NumberFormatter::withLocale(locale).formatInt(toFormat, errorCode);
    }

    number::FormattedNumber formatNumberWithDefaults(const Locale& locale, StringPiece toFormat, UErrorCode& errorCode) {
        return number::NumberFormatter::withLocale(locale).formatDecimal(toFormat, errorCode);
    }

    DateFormat* defaultDateTimeInstance(const Locale& locale, UErrorCode& errorCode) {
        NULL_ON_ERROR(errorCode);
        LocalPointer<DateFormat> df(DateFormat::createDateTimeInstance(DateFormat::SHORT, DateFormat::SHORT, locale));
        if (!df.isValid()) {
            errorCode = U_MEMORY_ALLOCATION_ERROR;
            return nullptr;
        }
        return df.orphan();
    }

    void formatDateWithDefaults(const Locale& locale, UDate date, UnicodeString& result, UErrorCode& errorCode) {
        CHECK_ERROR(errorCode);

        LocalPointer<DateFormat> df(defaultDateTimeInstance(locale, errorCode));
        CHECK_ERROR(errorCode);
        df->format(date, result, 0, errorCode);
    }

#if false
    // Called when output is required and the contents are an unevaluated `Formattable`;
    // formats the source `Formattable` to a string with defaults, if it can be
    // formatted with a default formatter
    static FormattedPlaceholder formatWithDefaults(const Locale& locale, const FormattedPlaceholder& input, UErrorCode& status) {
        if (U_FAILURE(status)) {
            return {};
        }

        const Formattable* toFormat = input.getSource(status);
        U_ASSERT(U_SUCCESS(status)); // Shouldn't get called on a null argument

        return formattableToString(locale, *toFormat, status);
    }
#endif

    UnicodeString formattableToString(const Locale& locale,
                                             const Formattable& toFormat,
                                             UErrorCode& status) {
        EMPTY_ON_ERROR(status);

        // Try as decimal number first
        if (toFormat.isNumeric()) {
            // Note: the ICU Formattable has to be created here since the StringPiece
            // refers to state inside the Formattable; so otherwise we'll have a reference
            // to a temporary object
            icu::Formattable icuFormattable = toFormat.asICUFormattable(status);
            StringPiece asDecimal = icuFormattable.getDecimalNumber(status);
            if (U_FAILURE(status)) {
                return {};
            }
            if (asDecimal != nullptr) {
                return formatNumberWithDefaults(locale, asDecimal, status).toString(status);
            }
        }

        UFormattableType type = toFormat.getType();
        switch (type) {
        case UFMT_DATE: {
            UnicodeString result;
            const DateInfo* dateInfo = toFormat.getDate(status);
            U_ASSERT(U_SUCCESS(status));
            formatDateWithDefaults(locale, d, result, status);
            return result;
        }
        case UFMT_DOUBLE: {
            double d = toFormat.getDouble(status);
            U_ASSERT(U_SUCCESS(status));
            return formatNumberWithDefaults(locale, d, status).toString(status);
        }
        case UFMT_LONG: {
            int32_t l = toFormat.getLong(status);
            U_ASSERT(U_SUCCESS(status));
            return formatNumberWithDefaults(locale, l, status).toString(status);
        }
        case UFMT_INT64: {
            int64_t i = toFormat.getInt64Value(status);
            U_ASSERT(U_SUCCESS(status));
            return formatNumberWithDefaults(locale, i, status).toString(status);
        }
        case UFMT_STRING: {
            const UnicodeString& s = toFormat.getString(status);
            U_ASSERT(U_SUCCESS(status));
            return s;
        }
        default: {
            // No default formatters for other types; use fallback
            status = U_MF_FORMATTING_ERROR;
            // Note: it would be better to set an internal formatting error so that a string
            // (e.g. the type tag) can be provided. However, this  method is called by the
            // public method formatToString() and thus can't take a MessageContext
            return {};
        }
        }
    }

#if false
    // Called when string output is required; forces output to be produced
    // if none is present (including formatting number output as a string)
    UnicodeString FormattedPlaceholder::formatToString(const Locale& locale,
                                                       UErrorCode& status) const {
        if (U_FAILURE(status)) {
            return {};
        }

        // Function result: either just return the string, or format the number
        // as a string and return it
        if (isFunctionResult()) {
            if (formatted.isString()) {
                return formatted.getString();
            } else {
                return formatted.getNumber().toString(status);
            }
        }
        // Unannotated value: apply default formatters
        UErrorCode savedStatus = status;
        FormattedPlaceholder evaluated = formatWithDefaults(locale, *this, status);
        if (status == U_MF_FORMATTING_ERROR) {
            return {};
        }
        // Ignore U_USING_DEFAULT_WARNING
        if (status == U_USING_DEFAULT_WARNING) {
            status = savedStatus;
        }
        return evaluated.formatToString(locale, status);
    }
#endif

} // namespace message2

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_MF2 */

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif /* #if !UCONFIG_NO_NORMALIZATION */
