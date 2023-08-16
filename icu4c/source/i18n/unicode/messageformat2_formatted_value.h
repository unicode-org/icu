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
  TODO: what to call this?
  Only strings and numbers? Other types?
  Maybe don't allow a Formattable* and replace that with UnicodeString?
*/
class FormattedPlaceholder : public FormattedValue, public UMemory {
    public:
    FormattedPlaceholder& operator+= (FormattedValue* next);
    void add(FormattedValue*, UErrorCode&);
    UnicodeString toString() const;
    UnicodeString toString(UErrorCode& status) const;
    UnicodeString toTempString(UErrorCode& status) const;
    Appendable& appendTo(Appendable& appendable, UErrorCode& status) const;
    UBool nextPosition(ConstrainedFieldPosition& cfpos, UErrorCode& status) const;

    // Invalidates `this`
    Formattable* getValue() { return value.orphan(); }

    bool isFormattedNumber() const { return kind; }

    static FormattedPlaceholder* create(Formattable* v, UErrorCode& status) {
        NULL_ON_ERROR(status);
        FormattedPlaceholder* result(new FormattedPlaceholder(v));
        if (result == nullptr) {
            status = U_MEMORY_ALLOCATION_ERROR;
        }
        return result;
    }
    static FormattedPlaceholder* create(number::FormattedNumber* v, UErrorCode& status) {
        NULL_ON_ERROR(status);
        FormattedPlaceholder* result(new FormattedPlaceholder(v));
        if (result == nullptr) {
            status = U_MEMORY_ALLOCATION_ERROR;
        }
        return result;
    }
    static FormattedPlaceholder* create(UnicodeString s, UErrorCode& status) {
        NULL_ON_ERROR(status);
        LocalPointer<Formattable> v(new Formattable(s));
        if (!v.isValid()) {
            status = U_MEMORY_ALLOCATION_ERROR;
            return nullptr;
        }
        FormattedPlaceholder* result(new FormattedPlaceholder(v.orphan()));
        if (result == nullptr) {
            status = U_MEMORY_ALLOCATION_ERROR;
        }
        return result;
    }

    private:
    // Adopts `v`
    FormattedPlaceholder(Formattable* v) : kind(false), value(v) { U_ASSERT(v != nullptr); }
    FormattedPlaceholder(number::FormattedNumber* v) : kind(true), num(v) { U_ASSERT(v != nullptr); }

    bool kind; // true if isFormattedNumber
    // ?? - Should this be a Formattable or a FormattedValue?
    LocalPointer<Formattable> value;
    LocalPointer<number::FormattedNumber> num; 
//    LocalPointer<FormattedPlaceholder> next;
};

} // namespace message2
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif /* U_SHOW_CPLUSPLUS_API */

#endif // MESSAGEFORMAT2_FORMATTED_VALUE_H

#endif // U_HIDE_DEPRECATED_API
// eof
