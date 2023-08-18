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
#include "unicode/messageformat2_data_model.h"
#include "unicode/numberformatter.h"
#include "unicode/smpdtfmt.h"

U_NAMESPACE_BEGIN namespace message2 {

using Expression      = MessageFormatDataModel::Expression;
using Operand         = MessageFormatDataModel::Operand;

class StringFormattedValue : public FormattedValue, public UMemory {
    public:
    static StringFormattedValue* create(UnicodeString s, UErrorCode& status) {
        if (U_FAILURE(status)) {
            return nullptr;
        }
        StringFormattedValue* result(new StringFormattedValue(s));
        if (result == nullptr) {
            status = U_MEMORY_ALLOCATION_ERROR;
        }
        return result;
    }
    static StringFormattedValue* create(UErrorCode& status) {
        return create(UnicodeString(""), status);
    }
    UnicodeString toString(UErrorCode& status) const {
        (void) status;
        return value;
    }
    UnicodeString toTempString(UErrorCode& status) const {
        return toString(status);
    }
    Appendable& appendTo(Appendable& appendable, UErrorCode& status) const {
        if (U_FAILURE(status)) {
            return appendable;
        }
        const char16_t* temp = value.getBuffer();
        appendable.appendString(temp, value.length());
        return appendable;
    }
    UBool nextPosition(ConstrainedFieldPosition& cfpos, UErrorCode& status) const {
        (void) cfpos;
        (void) status;
        return false;
    }
    private:
    StringFormattedValue() {}
    StringFormattedValue(UnicodeString s) : value(s) {}

    const UnicodeString value;
};

/*
class FallbackValue : public FormattedValue, public UMemory {
    public:
    UnicodeString toString() const {
        UErrorCode ignore;
        return toString(ignore);
    }
    UnicodeString toString(UErrorCode& status) const {
        if (U_FAILURE(status)) {
            return UnicodeString("");
        }

        UnicodeString r;
        UnicodeStringAppendable appendable(r);
        appendTo(appendable, status);
        return r;
    }
    UnicodeString toTempString(UErrorCode& status) const {
        return toString(status);
    }
    Appendable& appendTo(Appendable& appendable, UErrorCode& status) const {
        if (U_FAILURE(status)) {
            return appendable;
        }

        if (useBraces) {
            appendable.appendCodePoint(LEFT_CURLY_BRACE);
        }
        if (operand->isVariable()) {
            appendable.appendCodePoint(DOLLAR);
            appendable.appendString(operand->asVariable().getTerminatedBuffer(), -1);
        }
        if (useBraces) {
            appendable.appendCodePoint(RIGHT_CURLY_BRACE);
        }
        return appendable;
    }
    FallbackValue(bool u, const Operand& rand, UErrorCode& errorCode) : useBraces(u) {
        CHECK_ERROR(errorCode);
        operand.adoptInstead(new Operand(rand));
        if (!operand.isValid()) {
            errorCode = U_MEMORY_ALLOCATION_ERROR;
        }
    }

    private:
    const bool useBraces;
    LocalPointer<Operand> operand;
};
*/


/*
  TODO

  A FormattedPlaceholder can either be a fully formatted value,
  or a "Formattable" that could be formatted with a default formatter,
  but is available to be manipulated by formatting functions or deconstructed
  by selector functions.
*/
class FormattedPlaceholder : /* public FormattedValue,*/  public UMemory {
    public:

    static FormattedPlaceholder* formatDateWithDefaults(Locale locale, Formattable* toFormat, UErrorCode& errorCode) {
        NULL_ON_ERROR(errorCode);
        U_ASSERT(toFormat != nullptr);
        LocalPointer<DateFormat> df(DateFormat::createDateTimeInstance(DateFormat::SHORT, DateFormat::SHORT, locale));
        if (!df.isValid()) {
            errorCode = U_MEMORY_ALLOCATION_ERROR;
            return nullptr;
        }
        UnicodeString result;
        df->format(*toFormat, result, 0, errorCode);
        return FormattedPlaceholder::create(toFormat, result, errorCode);
    }

    static FormattedPlaceholder* formatNumberWithDefaults(Locale locale, Formattable* savedInput, double toFormat, UErrorCode& errorCode) {
        NULL_ON_ERROR(errorCode);
        return create(savedInput, number::NumberFormatter::withLocale(locale).formatDouble(toFormat, errorCode), errorCode);
    }
    static FormattedPlaceholder* formatNumberWithDefaults(Locale locale, Formattable* savedInput, int32_t toFormat, UErrorCode& errorCode) {
        NULL_ON_ERROR(errorCode);
        return create(savedInput, number::FormattedNumber(number::NumberFormatter::withLocale(locale).formatInt(toFormat, errorCode)), errorCode);
    }
    static FormattedPlaceholder* formatNumberWithDefaults(Locale locale, Formattable* savedInput, int64_t toFormat, UErrorCode& errorCode) {
        NULL_ON_ERROR(errorCode);
        return create(savedInput, number::FormattedNumber(number::NumberFormatter::withLocale(locale).formatInt(toFormat, errorCode)), errorCode);
    }

    /*
      Four types:
      STRING => plain string with no metadata
      NUMBER => FormattedNumber
      DYNAMIC => Formattable -- a ready-to-format thing that hasn't been formatted yet
      FALLBACK => Something resulting in a formatting error; getString() returns the fallback string
     */
    enum Type {
        NUMBER,
        STRING,
        DYNAMIC,
        FALLBACK
    };

    Type getType() const { return type; }
/*
    FormattedPlaceholder& operator+= (FormattedValue* next);
    void add(FormattedValue*, UErrorCode&);
*/
//    UnicodeString toString() const;
    UnicodeString toString(Locale locale, UErrorCode& status) const;
/*
    UnicodeString toTempString(UErrorCode& status) const;
    Appendable& appendTo(Appendable& appendable, UErrorCode& status) const;
    UBool nextPosition(ConstrainedFieldPosition& cfpos, UErrorCode& status) const;
*/

    const UnicodeString& getString() const {
        U_ASSERT(type == Type::STRING);
        U_ASSERT(string.isValid());
        return *string;
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
        return *input;
    }
    const Formattable* aliasInput() const {
        return input;
    }

    static FormattedPlaceholder* create(const Formattable* input, number::FormattedNumber v, UErrorCode& status) {
        NULL_ON_ERROR(status);
        FormattedPlaceholder* result(new FormattedPlaceholder(input, std::move(v)));
        if (result == nullptr) {
            status = U_MEMORY_ALLOCATION_ERROR;
        }
        return result;
    }

    static FormattedPlaceholder* create(const Formattable* v, UErrorCode& status) {
        NULL_ON_ERROR(status);
        FormattedPlaceholder* result(new FormattedPlaceholder(v));
        if (result == nullptr) {
            status = U_MEMORY_ALLOCATION_ERROR;
        }
        return result;
    }

    static FormattedPlaceholder* create(const Formattable* input, UnicodeString s, UErrorCode& status) {
        NULL_ON_ERROR(status);
        LocalPointer<UnicodeString> sPtr(new UnicodeString(s));
        if (!sPtr.isValid()) {
            status = U_MEMORY_ALLOCATION_ERROR;
            return nullptr;
        }
        FormattedPlaceholder* result(new FormattedPlaceholder(input, sPtr.orphan(), false));
        if (result == nullptr) {
            status = U_MEMORY_ALLOCATION_ERROR;
        }
        return result;
    }

    static FormattedPlaceholder* createFallback(UErrorCode& status) {
        return createFallback(UnicodeString(REPLACEMENT), status);
    }

    static FormattedPlaceholder* createFallback(UnicodeString s, UErrorCode& status) {
        NULL_ON_ERROR(status);
        LocalPointer<UnicodeString> sPtr(new UnicodeString(s));
        LocalPointer<Formattable> input(new Formattable(s));
        if (!sPtr.isValid() || !input.isValid()) {
            status = U_MEMORY_ALLOCATION_ERROR;
            return nullptr;
        }
        FormattedPlaceholder* result(new FormattedPlaceholder(input.orphan(), sPtr.orphan(), true));
        if (result == nullptr) {
            status = U_MEMORY_ALLOCATION_ERROR;
        }
        return result;
    }
/*
    static FormattedPlaceholder* create(UnicodeString s, UErrorCode& status) {
        NULL_ON_ERROR(status);
        LocalPointer<UnicodeString> sPtr(new UnicodeString(s));
        LocalPointer<Formattable> input(new Formattable(s));
        if (!sPtr.isValid() || !input.isValid()) {
            status = U_MEMORY_ALLOCATION_ERROR;
            return nullptr;
        }
        FormattedPlaceholder* result(new FormattedPlaceholder(input.orphan(), sPtr.orphan(), false));
        if (result == nullptr) {
            status = U_MEMORY_ALLOCATION_ERROR;
        }
        return result;
    }
*/
    // Formats a `Formattable` using defaults
    static FormattedPlaceholder* format(Locale loc, const Formattable& in, UErrorCode& status) {
        NULL_ON_ERROR(status);

        LocalPointer<FormattedPlaceholder> result;
        LocalPointer<Formattable> savedInput(new Formattable(in));
        if (!savedInput.isValid()) {
            status = U_MEMORY_ALLOCATION_ERROR;
            return nullptr;
        }
        switch (in.getType()) {
            case Formattable::Type::kDate: {
                result.adoptInstead(formatDateWithDefaults(loc, savedInput.orphan(), status));
                break;
            }
            case Formattable::Type::kDouble: {
                result.adoptInstead(formatNumberWithDefaults(loc, savedInput.orphan(), in.getDouble(), status));
                break;
            }
            case Formattable::Type::kLong: {
                result.adoptInstead(formatNumberWithDefaults(loc, savedInput.orphan(), in.getLong(), status));
                break;
            }
            case Formattable::Type::kInt64: {
                result.adoptInstead(formatNumberWithDefaults(loc, savedInput.orphan(), in.getInt64(), status));
                break;
            }
            case Formattable::Type::kString: {
                result.adoptInstead(create(savedInput.orphan(), in.getString(), status));
                break;
            }
            default: {
                // TODO: ??
                UnicodeString fallback(REPLACEMENT);
                result.adoptInstead(create(savedInput.orphan(), fallback, status));
                break;
            }
        }
        NULL_ON_ERROR(status);
        return result.orphan();
    }

    virtual ~FormattedPlaceholder();

    private:

    // All constructors adopt their arguments
    FormattedPlaceholder(const Formattable* f, UnicodeString* s, bool isFallback) : type(isFallback ? Type::FALLBACK : Type::STRING), string(s), input(f) {
        U_ASSERT(f != nullptr);
        U_ASSERT(s != nullptr);
    }
    FormattedPlaceholder(const Formattable* f, number::FormattedNumber v) : type(Type::NUMBER), num(std::move(v)), input(f) {
        U_ASSERT(f != nullptr);
    }
    FormattedPlaceholder(const Formattable* f) : type(Type::DYNAMIC), input(f) { U_ASSERT(f != nullptr); }

    Type type;
    // ?? - Should this be a Formattable or a FormattedValue?
    // Maybe this shouldn't allow a Formattable, only a string or number (and other types if we want)?

    // TODO: this wastes memory, but not sure how we can have a union with LocalPointers
    LocalPointer<UnicodeString> string;
    number::FormattedNumber num;

    // If the other two fields are invalid,
    // this is assumed to be a not-formatted-yet value

    // This does not own input (it may be in the global environment)
    const Formattable* input;

//    LocalPointer<FormattedPlaceholder> next;
};

} // namespace message2
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif /* U_SHOW_CPLUSPLUS_API */

#endif // MESSAGEFORMAT2_FORMATTED_VALUE_H

#endif // U_HIDE_DEPRECATED_API
// eof
