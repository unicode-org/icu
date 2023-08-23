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
#include "unicode/messageformat2_macros.h"
#include "unicode/numberformatter.h"
#include "unicode/smpdtfmt.h"

U_NAMESPACE_BEGIN namespace message2 {

class FormattedString;
class FormattedNumber;
class FormattingInput;
class FullyFormatted;

extern FormattedString* formatDateWithDefaults(Locale locale, const FormattingInput& in, UErrorCode& errorCode);
extern FormattedNumber* formatNumberWithDefaults(Locale locale, const FormattingInput& in, double toFormat, UErrorCode& errorCode);
extern FormattedNumber* formatNumberWithDefaults(Locale locale, const FormattingInput& in, int32_t toFormat, UErrorCode& errorCode);
extern FormattedNumber* formatNumberWithDefaults(Locale locale, const FormattingInput& in, int64_t toFormat, UErrorCode& errorCode);

/*
  TODO

  A FormattedPlaceholder can either be a fully formatted value,
  or a "Formattable" that could be formatted with a default formatter,
  but is available to be manipulated by formatting functions or deconstructed
  by selector functions.
*/

class Fallback;

class FormattedPlaceholder : public UMemory {
    public:
    virtual bool hasInput() const = 0;
    virtual bool isFallback() const = 0;
    virtual bool hasOutput() const = 0;
    // Precondition: hasOutput()
    const FullyFormatted& getOutput() const;
    virtual UnicodeString toString(Locale locale, UErrorCode& status) const = 0;
    virtual const Fallback* fallbackToSource(UErrorCode&) const = 0;
    static DateFormat* defaultDateTimeInstance(Locale, UErrorCode&);

    virtual ~FormattedPlaceholder();
};

class FormattingInput;
class FullyFormatted;

class Fallback : public FormattedPlaceholder {
    public:
    bool hasInput() const { return false; }
    virtual bool isFallback() const { return true; }
    bool hasOutput() const { return false; }
    UnicodeString toString(Locale locale, UErrorCode& status) const;
    UnicodeString toString() const;
    static Fallback* create(const Text&, UErrorCode& status);
    // TODO: this doesn't work
    // const Fallback* fallbackToSource() const { return static_cast<const Fallback*>(this); }
    const Fallback* fallbackToSource(UErrorCode& status) const { return Fallback::create(fallback, status); }

    virtual ~Fallback();
    private:
    friend class FormattingInput;
    friend class FullyFormatted;

    const Text& fallback;
    Formattable fallbackAsFmtable;
    Fallback(const Text& t);
}; // class Fallback

class FormattingInput : public Fallback {
    public:
    virtual bool isNull() const { return true; }
    virtual bool isFallback() const override { return false; }
    virtual const Formattable& getInput() const {
        U_ASSERT(false);
    }
    virtual ~FormattingInput();
    static FormattingInput* create(const FunctionName& fn, UErrorCode& errorCode) {
        NULL_ON_ERROR(errorCode);
        FormattingInput* result = new FormattingInput(fn);
        if (result == nullptr) {
            errorCode = U_MEMORY_ALLOCATION_ERROR;
        }
        return result;
    }
    private:
    friend class FormattingInputNotNull;

    // Since this is used to represent an absent argument,
    // there is always a function name to use as a fallback
    FormattingInput(const Text& fn) : Fallback(fn) {}
}; // class FormattingInput

class FormattingInputNotNull : public FormattingInput {
    public:
    bool isNull() const override { return false; }
    bool hasInput() const { return true; }
    bool hasOutput() const { return false; }
    UnicodeString toString(Locale locale, UErrorCode& status) const;
    const Formattable& getInput() const override {
        return input;
    }
    // Creates an unformatted value
    static FormattingInputNotNull* create(const Text& source, const Formattable& v, UErrorCode& status) {
        NULL_ON_ERROR(status);
        FormattingInputNotNull* result(new FormattingInputNotNull(source, v));
        if (result == nullptr) {
            status = U_MEMORY_ALLOCATION_ERROR;
        }
        return result;
    }
    virtual ~FormattingInputNotNull();
    private:
    friend class FullyFormatted;

    const Formattable& input;
    FormattingInputNotNull(const Text& p, const Formattable& f) : FormattingInput(p), input(f) {}
}; // class FormattingInputNotNull

class FormattedString;
class FormattedNumber;

class FullyFormatted : public FormattingInputNotNull {
    public:
    virtual bool isFormattedNumber() const = 0;
    virtual bool isFormattedString() const = 0;
    bool hasInput() const { return true; }
    bool hasOutput() const { return true; }
    // Formats a `Formattable` using defaults
    static FullyFormatted* format(Locale loc, const FormattingInput& in, UErrorCode& status);
    static FullyFormatted* create(const FormattingInput&, UnicodeString, UErrorCode&);
    static FullyFormatted* promoteFallback(const Fallback&, UErrorCode& status);
    
    private:
    friend class FormattedString;
    friend class FormattedNumber;

    FullyFormatted(const FormattingInputNotNull& i) : FormattingInputNotNull(i.fallback, i.input) {}
    FullyFormatted(const FormattingInput& i) : FormattingInputNotNull(i.fallback, i.fallbackAsFmtable) {} 

}; // class FullyFormatted

class FormattedString : public FullyFormatted {
    public:
    UnicodeString toString(Locale locale, UErrorCode& status) const;
    bool isFormattedNumber() const { return false; }
    bool isFormattedString() const { return true; }
    const UnicodeString& getString() const {
        return string;
    }
    // Creates a formatted string, saving its input
    static FormattedString* create(const FormattingInput& input, const UnicodeString& s, UErrorCode& status) {
        NULL_ON_ERROR(status);
        FormattedString* result(new FormattedString(input, s));
        if (result == nullptr) {
            status = U_MEMORY_ALLOCATION_ERROR;
        }
        return result;
    }
    virtual ~FormattedString();
    private:
    const UnicodeString string;
    FormattedString(const FormattingInputNotNull& i, const UnicodeString& s) : FullyFormatted(i), string(s) {}
    FormattedString(const FormattingInput& i, const UnicodeString& s) : FullyFormatted(i), string(s) {}
}; // class FormattedString


class FormattedNumber : public FullyFormatted {
    public:
    UnicodeString toString(Locale locale, UErrorCode& status) const;
    bool isFormattedNumber() const { return true; }
    bool isFormattedString() const { return false; }
    const number::FormattedNumber& getNumber() const {
        return num;
    }
    // Creates a formatted number (NUMBER)
    static FormattedNumber* create(const FormattingInput& input, number::FormattedNumber v, UErrorCode& status) {
        NULL_ON_ERROR(status);
        U_ASSERT(!input.isNull());
        FormattedNumber* result(new FormattedNumber(static_cast<const FormattingInputNotNull&>(input), std::move(v)));
        if (result == nullptr) {
            status = U_MEMORY_ALLOCATION_ERROR;
        }
        return result;
    }
    virtual ~FormattedNumber();
    private:
    const number::FormattedNumber num;
    FormattedNumber(const FormattingInputNotNull& i, number::FormattedNumber n) : FullyFormatted(i), num(std::move(n)) {}
}; // class FormattedNumber

} // namespace message2
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif /* U_SHOW_CPLUSPLUS_API */

#endif // MESSAGEFORMAT2_FORMATTED_VALUE_H

#endif // U_HIDE_DEPRECATED_API
// eof
