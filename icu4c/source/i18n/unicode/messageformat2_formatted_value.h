// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#ifndef U_HIDE_DEPRECATED_API

#ifndef MESSAGEFORMAT2_FORMATTED_VALUE_H
#define MESSAGEFORMAT2_FORMATTED_VALUE_H

#if U_SHOW_CPLUSPLUS_API

/**
 * \file
 * \brief C++ API: Formats messages using the draft MessageFormat 2.0.
 */

#if !UCONFIG_NO_FORMATTING

#include "unicode/formattedvalue.h"
#include "unicode/messageformat2_macros.h"
#include "unicode/numberformatter.h"
#include "unicode/smpdtfmt.h"

U_NAMESPACE_BEGIN namespace message2 {

/*
  TODO

  A FormattedPlaceholder can either be a fully formatted value,
  or a "Formattable" that could be formatted with a default formatter,
  but is available to be manipulated by formatting functions or deconstructed
  by selector functions.
*/
class FormattedPlaceholder : public UMemory {
    public:

    static DateFormat* defaultDateTimeInstance(Locale locale, UErrorCode& errorCode) {
        NULL_ON_ERROR(errorCode);
        LocalPointer<DateFormat> df(DateFormat::createDateTimeInstance(DateFormat::SHORT, DateFormat::SHORT, locale));
        if (!df.isValid()) {
            errorCode = U_MEMORY_ALLOCATION_ERROR;
            return nullptr;
        }
        return df.orphan();
    }

    static FormattedPlaceholder* formatNumberWithDefaults(Locale locale, const Formattable* savedInput, double toFormat, UErrorCode& errorCode) {
        NULL_ON_ERROR(errorCode);
        return create(savedInput, number::NumberFormatter::withLocale(locale).formatDouble(toFormat, errorCode), errorCode);
    }
    static FormattedPlaceholder* formatNumberWithDefaults(Locale locale, const Formattable* savedInput, int32_t toFormat, UErrorCode& errorCode) {
        NULL_ON_ERROR(errorCode);
        return create(savedInput, number::FormattedNumber(number::NumberFormatter::withLocale(locale).formatInt(toFormat, errorCode)), errorCode);
    }
    static FormattedPlaceholder* formatNumberWithDefaults(Locale locale, const Formattable* savedInput, int64_t toFormat, UErrorCode& errorCode) {
        NULL_ON_ERROR(errorCode);
        return create(savedInput, number::FormattedNumber(number::NumberFormatter::withLocale(locale).formatInt(toFormat, errorCode)), errorCode);
    }

    /*
      Four types:
      STRING => plain string with no metadata
      NUMBER => FormattedNumber
      DYNAMIC => Formattable -- a ready-to-format thing that hasn't been formatted yet
      NULL_ARGUMENT => a null value -- this can only happen if a message argument was set to nullptr
     */
    enum Type {
        NUMBER,
        STRING,
        DYNAMIC,
        NULL_ARGUMENT,
    };

    Type getType() const { return type; }
    bool isFormattedNumber() const { return (getType() == Type::NUMBER); }
    UnicodeString toString(Locale locale, UErrorCode& status) const;

    const UnicodeString& getString() const {
        U_ASSERT(type == Type::STRING);
        return string;
    }
    const number::FormattedNumber& getNumber() const {
        U_ASSERT(type == Type::NUMBER);
        return num;
    }
    number::FormattedNumber getNumber() {
        U_ASSERT(type == Type::NUMBER);
        return std::move(num);
    }
    const Formattable& getInput() const {
        U_ASSERT(hasInput());
        return *input;
    }
    const Formattable* aliasInput() const {
        U_ASSERT(hasInput());
        return input;
    }

    // Null argument constructor (NULL_ARGUMENT)
    static FormattedPlaceholder* create(UErrorCode& status) {
        NULL_ON_ERROR(status);
        FormattedPlaceholder* result(new FormattedPlaceholder());
        if (result == nullptr) {
            status = U_MEMORY_ALLOCATION_ERROR;
        }
        return result;
    }

    // Creates a formatted number (NUMBER)
    static FormattedPlaceholder* create(const Formattable* input, number::FormattedNumber v, UErrorCode& status) {
        NULL_ON_ERROR(status);
        FormattedPlaceholder* result(new FormattedPlaceholder(input, std::move(v)));
        if (result == nullptr) {
            status = U_MEMORY_ALLOCATION_ERROR;
        }
        return result;
    }

    // Creates an unformatted value (DYNAMIC)
    static FormattedPlaceholder* create(const Formattable* v, UErrorCode& status) {
        NULL_ON_ERROR(status);
        FormattedPlaceholder* result(new FormattedPlaceholder(v));
        if (result == nullptr) {
            status = U_MEMORY_ALLOCATION_ERROR;
        }
        return result;
    }

    // Creates a formatted string (STRING), saving its input
    static FormattedPlaceholder* create(const Formattable* input, UnicodeString s, UErrorCode& status) {
        NULL_ON_ERROR(status);
        FormattedPlaceholder* result(new FormattedPlaceholder(input, s));
        if (result == nullptr) {
            status = U_MEMORY_ALLOCATION_ERROR;
        }
        return result;
    }

    // Sets input to `s` (same input and formatted result) - type STRING
    // The fallback string is different: for example: fallback=|42|, input="42", s="42"
    static FormattedPlaceholder* create(const UnicodeString& s, UErrorCode& status) {
        NULL_ON_ERROR(status);
        // TODO: perhaps this could be optimized?
        // depends if we ever want to distinguish between "an input string, not yet formatted"

        LocalPointer<Formattable> input(new Formattable(s));
        if (!input.isValid()) {
            status = U_MEMORY_ALLOCATION_ERROR;
            return nullptr;
        }
        FormattedPlaceholder* result(new FormattedPlaceholder(input.orphan(), s));
        if (result == nullptr) {
            status = U_MEMORY_ALLOCATION_ERROR;
        }
        return result;
    }
    // Formats a `Formattable` using defaults
    static FormattedPlaceholder* format(Locale loc, const Formattable* in, UErrorCode& status) {
        NULL_ON_ERROR(status);
        U_ASSERT(in != nullptr);

        LocalPointer<FormattedPlaceholder> result;
        switch (in->getType()) {
            case Formattable::Type::kDate: {
                result.adoptInstead(formatDateWithDefaults(loc, in, status));
                break;
            }
            case Formattable::Type::kDouble: {
                result.adoptInstead(formatNumberWithDefaults(loc, in, in->getDouble(), status));
                break;
            }
            case Formattable::Type::kLong: {
                result.adoptInstead(formatNumberWithDefaults(loc, in, in->getLong(), status));
                break;
            }
            case Formattable::Type::kInt64: {
                result.adoptInstead(formatNumberWithDefaults(loc, in, in->getInt64(), status));
                break;
            }
            case Formattable::Type::kString: {
                result.adoptInstead(create(in, in->getString(), status));
                break;
            }
            default: {
                result.adoptInstead(create(in, status));
                break;
            }
        }
        NULL_ON_ERROR(status);
        return result.orphan();
    }

    virtual ~FormattedPlaceholder();

    private:

    static FormattedPlaceholder* formatDateWithDefaults(Locale locale, const Formattable* toFormat, UErrorCode& errorCode) {
        NULL_ON_ERROR(errorCode);
        U_ASSERT(toFormat != nullptr);
        LocalPointer<DateFormat> df(defaultDateTimeInstance(locale, errorCode));
        NULL_ON_ERROR(errorCode);
        UnicodeString result;
        df->format(*toFormat, result, 0, errorCode);
        return FormattedPlaceholder::create(toFormat, result, errorCode);
    }

    bool hasInput() const {
        switch (getType()) {
            case Type::NULL_ARGUMENT: {
                return false;
            }
            default: {
                return true;
            }
        }
    }

    // All constructors adopt their arguments
    FormattedPlaceholder(const Formattable* f, UnicodeString s) : type(Type::STRING), string(s), input(f) {
        U_ASSERT(f != nullptr);
    }

    FormattedPlaceholder(const Formattable* f, number::FormattedNumber v) : type(Type::NUMBER), num(std::move(v)), input(f) {
        U_ASSERT(f != nullptr);
    }
    FormattedPlaceholder(const Formattable* f) : type(Type::DYNAMIC), input(f) { U_ASSERT(f != nullptr); }
    FormattedPlaceholder() : type(Type::NULL_ARGUMENT), input(nullptr) {}

    Type type;
    // ?? - Should this be a Formattable or a FormattedValue?
    // Maybe this shouldn't allow a Formattable, only a string or number (and other types if we want)?

    // If `type` is not DYNAMIC, then the contents are either a formatted string,
    // or a formatted number
    union {
        UnicodeString string;
        number::FormattedNumber num;
    };
    // This does not own input (it may be in the global environment)
    // TODO: since Formattables are immutable, can we use a reference here instead?
    // (not if it means a Formattable would have to be copied)
    // Null if and only if the type is NULL_ARGUMENT
    const Formattable* input;
};

} // namespace message2
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif /* U_SHOW_CPLUSPLUS_API */

#endif // MESSAGEFORMAT2_FORMATTED_VALUE_H

#endif // U_HIDE_DEPRECATED_API
// eof
