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
class Selector;

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


using Literal         = MessageFormatDataModel::Literal;

class Context;
class Formatter;
class MessageFormatter;

// This is per-operand/expression being formatted,
// so it's not part of the MessageFormatter object
class FormattedValueBuilder : public UMemory {
    private:
    friend class MessageFormatter;

    using Builder = FormattedValueBuilder;

    FormattedValueBuilder(Context&);

    Context& context;

    static FormattedValueBuilder* create(Context& context, UErrorCode& errorCode);
    FormattedValueBuilder* create(UErrorCode& errorCode);

    Builder& setParseError(uint32_t line, uint32_t offset);
    Builder& setUnresolvedVariable(const VariableName&);
    Builder& setUnknownFunction(const FunctionName&);
    Builder& setVariantKeyMismatch();
    Builder& setFormattingWarning(const FunctionName&);
    Builder& setNonexhaustivePattern();
    Builder& setDuplicateOptionName();
    Builder& setReservedError();
    Builder& setSelectorError(const UnicodeString&);
    Builder& setMissingSelectorAnnotation();
    // Resets input and output and uses existing fallback
    Builder& setFallback();
    Builder& setFallback(const Literal&);
    Builder& setFallback(const VariableName&);
    Builder& setFallback(const FunctionName&);
    Builder& setFunctionName(const FunctionName&);
    // Function name must be set; clears it
    Builder& resolveSelector(Selector*);
    Builder& setStringOption(const UnicodeString&, const UnicodeString&);
    Builder& setDateOption(const UnicodeString&, UDate);
    Builder& setNumericOption(const UnicodeString&, double);
    Builder& setInput(const UObject*);
    Builder& setInput(const Formattable&);
    Builder& setInput(const UnicodeString&);
    Builder& setObjectInput(UObject*);
    Builder& setOutput(const UnicodeString&);
    Builder& setOutput(number::FormattedNumber&&);
    Builder& propagateErrors(const FormattedValueBuilder& localContext);
    // If any errors were set, update `status` accordingly
    Builder& checkErrors(UErrorCode& status);
    Builder& promoteFallbackToInput();
    // Doesn't change output if it already exists
    // Appends to `result`
    void formatToString(const Locale& locale, UnicodeString& result);

    bool buildToFunctionCall();
    // If there is a formatter name, clear it and
    // call the function, setting the input and/or output appropriately
    // Precondition: hasFormatter()
    void evalPendingFunctionCall();

    public:
    void formatWithDefaults(UErrorCode& errorCode);
    
    bool hasGlobal(const VariableName& v) { return hasGlobalAsFormattable(v) || hasGlobalAsObject(v); }
    bool hasGlobalAsFormattable(const VariableName&);
    bool hasGlobalAsObject(const VariableName&);
    const Formattable& getGlobalAsFormattable(const VariableName&);
    const UObject* getGlobalAsObject(const VariableName&);
    bool hasOperand() const;
    bool hasSelector() const;
    bool hasFormatter() const;
    bool hasOperator() const { return (hasSelector() || hasFormatter()); }
    const FunctionName& getOperator() const;
    const Formatter* maybeCachedFormatter(const FunctionName&, UErrorCode&) const;
    // Precondition: hasSelector()
    const Selector& getSelector() const;
    bool hasFormattableInput() const;
    bool hasObjectInput() const;
    const Formattable& getFormattableInput() const;
    const UObject* getObjectInput() const;
    bool hasStringOutput() const;
    bool hasNumberOutput() const;
    bool hasOutput() { return (hasStringOutput() || hasNumberOutput()); }
    // just gets existing output, doesn't force evaluation
    const UnicodeString& getStringOutput() const;
    const number::FormattedNumber& getNumberOutput() const;
    bool getStringOption(UnicodeString&) const;
    bool getNumericOption(double&) const;
    bool isFallback() const;
    bool hasParseError() const;
    bool hasDataModelError() const;
    bool hasUnknownFunctionError() const;
    bool hasSelectorError() const;
    bool hasError() const;
    bool hasWarning() const;

    virtual ~FormattedValueBuilder();
};

} // namespace message2
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif /* U_SHOW_CPLUSPLUS_API */

#endif // MESSAGEFORMAT2_FORMATTED_VALUE_H

#endif // U_HIDE_DEPRECATED_API
// eof
