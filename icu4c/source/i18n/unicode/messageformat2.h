// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#ifndef U_HIDE_DEPRECATED_API

#ifndef MESSAGEFORMAT2_H
#define MESSAGEFORMAT2_H

#if U_SHOW_CPLUSPLUS_API

/**
 * \file
 * \brief C++ API: Formats messages using the draft MessageFormat 2.0.
 */

#if !UCONFIG_NO_FORMATTING

#include "unicode/format.h"
#include "unicode/messageformat2_checker.h"
#include "unicode/messageformat2_data_model.h"
#include "unicode/messageformat2_function_registry.h"
#include "unicode/messageformat2_macros.h"
#include "unicode/unistr.h"
#include "unicode/utypes.h"

U_NAMESPACE_BEGIN namespace message2 {

/**
 * <p>MessageFormatter is a Technical Preview API implementing MessageFormat 2.0.
 * Since it is not final, documentation has not yet been added everywhere.
 *
 * <p>See <a target="github" href="https://github.com/unicode-org/message-format-wg/blob/main/spec/syntax.md">the
 * description of the syntax with examples and use cases</a> and the corresponding
 * <a target="github" href="https://github.com/unicode-org/message-format-wg/blob/main/spec/message.abnf">ABNF</a> grammar.</p>
 *
 * @internal ICU 74.0 technology preview
 * @deprecated This API is for technology preview only.
 */

// Note: This class does not currently inherit from the existing
// `Format` class.
class U_I18N_API MessageFormatter : UMemory {
public:
    /**
     * Destructor.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    virtual ~MessageFormatter();

    virtual void formatToString(const Hashtable& arguments, UErrorCode &status, UnicodeString &result) const;
 
    Locale getLocale() const { return locale; }
    void getPattern(UnicodeString& result) const {
        // Converts the current data model back to a string
        U_ASSERT(dataModel.isValid());
        Serializer serializer(*dataModel, result);
        serializer.serialize();
    }

    // Give public access to the data model
    const MessageFormatDataModel& getDataModel() const { return *dataModel; }

    /**
     * The mutable Builder class allows each part of the MessageFormatter to be initialized
     * separately; calling its `build()` method yields an immutable MessageFormatter.
     */
    class U_I18N_API Builder {
    private:
       friend class MessageFormatter;

       Builder() : locale(Locale::getDefault()), customFunctionRegistry(nullptr) {}

       // The pattern to be parsed to generate the formatted message
       LocalPointer<UnicodeString> pattern;
       // The data model to be used to generate the formatted message
       // Invariant: !(pattern.isValid() && dataModel.isValid())
       LocalPointer<MessageFormatDataModel> dataModel;
       Locale locale;
       LocalPointer<FunctionRegistry> standardFunctionRegistry;
       LocalPointer<FunctionRegistry> customFunctionRegistry;

    public:
        Builder& setLocale(Locale locale);
        Builder& setPattern(const UnicodeString& pattern, UErrorCode& errorCode);
        // Takes ownership of the FunctionRegistry
        Builder& setFunctionRegistry(FunctionRegistry* functionRegistry);
        // Takes ownership of the data model
        Builder& setDataModel(MessageFormatDataModel* dataModel);

        /**
         * Constructs a new immutable MessageFormatter using the pattern or data model
         * that was previously set, and the locale (if it was previously set)
         * or default locale (otherwise).
         *
         * The builder object (`this`) cannot be used after calling `build()`.
         *
         * @param parseError Struct to receive information on the position
         *                   of an error within the pattern (not used if
         *                   the data model is set).
         * @param status    Input/output error code.  If the
         *                  pattern cannot be parsed, or if neither the pattern
         *                  nor the data model is set, set to failure code.
         *
         * @internal ICU 74.0 technology preview
         * @deprecated This API is for technology preview only.
         */
        MessageFormatter* build(UParseError& parseError, UErrorCode& errorCode);
    }; // class MessageFormatter::Builder

    static Builder* builder(UErrorCode& errorCode);

    // TODO: Shouldn't be public; only used for testing
    const UnicodeString& getNormalizedPattern() const { return *normalizedInput; }

  private:
    friend class Builder;

    MessageFormatter(MessageFormatter::Builder& builder, UParseError &parseError, UErrorCode &status);

    MessageFormatter() = delete; // default constructor not implemented

    // Do not define default assignment operator
    const MessageFormatter &operator=(const MessageFormatter &) = delete;

    // Parser class (private)
    class Parser : public UMemory {
    public:
        virtual ~Parser();
        static Parser* create(const UnicodeString &input, MessageFormatDataModel::Builder& dataModelBuilder, UnicodeString& normalizedInput, UErrorCode& errorCode) {
            if (U_FAILURE(errorCode)) {
                return nullptr;
            }
            Parser* p = new Parser(input, dataModelBuilder, normalizedInput);
            if (p == nullptr) {
                errorCode = U_MEMORY_ALLOCATION_ERROR;
            }
            return p;
        }
        // The parser validates the message and builds the data model
        // from it.
        void parse(UParseError &, UErrorCode &);
    private:
        friend class MessageFormatDataModel::Builder;

        /*
          Use an internal "parse error" structure to make it easier to translate
          absolute offsets to line offsets.
          This is translated back to a `UParseError` at the end of parsing.
        */
        typedef struct MessageParseError {
            // The line on which the error occurred
            uint32_t line;
            // The offset, relative to the erroneous line, on which the error occurred
            uint32_t offset;
            // The total number of characters seen before advancing to the current line. It has a value of 0 if line == 0.
            // It includes newline characters, because the index does too.
            uint32_t lengthBeforeCurrentLine;

            // This parser doesn't yet use the last two fields.
            UChar   preContext[U_PARSE_CONTEXT_LEN];
            UChar   postContext[U_PARSE_CONTEXT_LEN];
        } MessageParseError;

        Parser(const UnicodeString &input, MessageFormatDataModel::Builder& dataModelBuilder, UnicodeString& normalizedInputRef)
            : source(input), index(0), normalizedInput(normalizedInputRef), dataModel(dataModelBuilder) {
            parseError.line = 0;
            parseError.offset = 0;
            parseError.lengthBeforeCurrentLine = 0;
            parseError.preContext[0] = '\0';
            parseError.postContext[0] = '\0';
        }

        // Used so `parseEscapeSequence()` can handle all types of escape sequences
        // (literal, text, and reserved)
        typedef enum { LITERAL, TEXT, RESERVED } EscapeKind;

        static void translateParseError(const MessageParseError&, UParseError&);
        static void setParseError(MessageParseError&, uint32_t);
        void maybeAdvanceLine();
        void parseBody(UErrorCode &);
        void parseDeclarations(UErrorCode &);
        void parseSelectors(UErrorCode &);

        void parseWhitespaceMaybeRequired(bool, UErrorCode &);
        void parseRequiredWhitespace(UErrorCode &);
        void parseOptionalWhitespace(UErrorCode &);
        void parseToken(UChar32, UErrorCode &);
        void parseTokenWithWhitespace(UChar32, UErrorCode &);
        template <size_t N>
        void parseToken(const UChar32 (&)[N], UErrorCode &);
        template <size_t N>
        void parseTokenWithWhitespace(const UChar32 (&)[N], UErrorCode &);
        void parseName(UErrorCode&, VariableName&);
        void parseVariableName(UErrorCode&, VariableName&);
        FunctionName* parseFunction(UErrorCode&);
        void parseEscapeSequence(EscapeKind, UErrorCode &, String&);
        void parseLiteralEscape(UErrorCode &, String&);
        void parseLiteral(UErrorCode &, bool&, String&);
        void parseOption(UErrorCode&, MessageFormatDataModel::Operator::Builder&);
        void parseOptions(UErrorCode &, MessageFormatDataModel::Operator::Builder&);
        void parseReservedEscape(UErrorCode&, String&);
        void parseReservedChunk(UErrorCode &, MessageFormatDataModel::Reserved::Builder&);
        MessageFormatDataModel::Reserved* parseReserved(UErrorCode &);
        MessageFormatDataModel::Operator* parseAnnotation(UErrorCode &);
        void parseLiteralOrVariableWithAnnotation(bool, UErrorCode &, MessageFormatDataModel::Expression::Builder&);
        MessageFormatDataModel::Expression* parseExpression(UErrorCode &);
        void parseTextEscape(UErrorCode&, String&);
        void parseText(UErrorCode&, String&);
        MessageFormatDataModel::Key* parseKey(UErrorCode&);
        MessageFormatDataModel::SelectorKeys* parseNonEmptyKeys(UErrorCode&);
        MessageFormatDataModel::Pattern* parsePattern(UErrorCode&);

        // The input string
        const UnicodeString &source;
        // The current position within the input string
        uint32_t index;
        // Represents the current line (and when an error is indicated),
        // character offset within the line of the parse error
        MessageParseError parseError;

        // Normalized version of the input string (optional whitespace removed)
        UnicodeString& normalizedInput;

        // The parent builder
        MessageFormatDataModel::Builder &dataModel;
    }; // class Parser

    // Serializer class (private)
    // Converts a data model back to a string
    class Serializer : UMemory {
    public:
         Serializer(const MessageFormatDataModel& m, UnicodeString& s) : dataModel(m), result(s) {}
         void serialize();

         const MessageFormatDataModel& dataModel;
         UnicodeString& result;

    private:
        void whitespace();
        void emit(UChar32);
        template <size_t N>
        void emit(const UChar32 (&)[N]);
        void emit(const UnicodeString&);
        void emit(const MessageFormatDataModel::FunctionName&);
        void emit(const MessageFormatDataModel::Literal&);
        void emit(const MessageFormatDataModel::Key&);
        void emit(const MessageFormatDataModel::SelectorKeys&);
        void emit(const MessageFormatDataModel::Operand&);
        void emit(const MessageFormatDataModel::Expression&);
        void emit(const MessageFormatDataModel::PatternPart&);
        void emit(const MessageFormatDataModel::Pattern&);
        void emit(const MessageFormatDataModel::VariantMap&);
        void emit(const MessageFormatDataModel::OptionMap&);
        void serializeDeclarations();
        void serializeSelectors();
        void serializeVariants();
    }; // class Serializer

    // Checks a data model for semantic errors
    // (Errors are defined in https://github.com/unicode-org/message-format-wg/blob/main/spec/formatting.md       )
    class Checker {
    public:
        void check(UErrorCode& error);
        Checker(const MessageFormatDataModel& m) : dataModel(m) {}
    private:
        void checkDeclarations(TypeEnvironment&, UErrorCode&);
        void checkSelectors(const TypeEnvironment&, UErrorCode&);
        void checkVariants(UErrorCode&);
        void check(const MessageFormatDataModel::OptionMap&, UErrorCode&);
        void check(const MessageFormatDataModel::Operand&, UErrorCode&);
        void check(const MessageFormatDataModel::Expression&, UErrorCode&);
        void check(const MessageFormatDataModel::Pattern&, UErrorCode&);
        const MessageFormatDataModel& dataModel;
    };

    // Intermediate classes used internally in the formatter

    // `ResolvedExpression`
    // represents the result of resolving an expression in a selector
    // context.
    // The selector function is `:identity` for a simple expression,
    // and the looked-up selector function otherwise.
    // The `Hashtable` (UnicodeString -> UnicodeString)
    // is the result of resolving the options
    // in the annotation.
    // The `operand` is the result of resolving the operand.
    class ResolvedExpression : public UMemory {
    public:
        virtual ~ResolvedExpression();
        const LocalPointer<Selector> selectorFunction;
        const LocalPointer<Hashtable> resolvedOptions;
        const UnicodeString resolvedOperand;
        // Adopts all its arguments
        ResolvedExpression(Selector* s, Hashtable* o, UnicodeString r) : selectorFunction(s), resolvedOptions(o), resolvedOperand(r) {}
    };
     void resolveVariables(const Hashtable&, const MessageFormatDataModel::Operand&, bool&, Hashtable*&, UnicodeString&, UnicodeString&, UErrorCode &) const;
     void resolveVariables(const Hashtable&, const MessageFormatDataModel::Expression&, bool&, Hashtable*&, UnicodeString&, UnicodeString&, UErrorCode &) const;

     // Selection methods
     void resolveSelectors(const Hashtable&, const MessageFormatDataModel::ExpressionList&, UErrorCode&, UVector&) const;
     void matchSelectorKeys(const ResolvedExpression&, const UVector&, UErrorCode&, UVector&) const;
     void resolvePreferences(const UVector&, const MessageFormatDataModel::VariantMap&, UErrorCode&, UVector& pref) const;

     // Formatting methods
     void formatBuiltInCall(const Hashtable&, const FunctionName&, const MessageFormatDataModel::OptionMap&, const MessageFormatDataModel::Operand&, UErrorCode&, UnicodeString&) const;
     void formatPattern(const Hashtable&, const MessageFormatDataModel::Pattern&, UErrorCode&, UnicodeString&) const;
     // Formats an expression in a selector context
     ResolvedExpression* formatSelectorExpression(const Hashtable&, const MessageFormatDataModel::Expression&, UErrorCode&) const;
     // Formats an expression in a pattern context
     void formatPatternExpression(const Hashtable&, const MessageFormatDataModel::Expression&, UErrorCode&, UnicodeString&) const;
     Hashtable* resolveOptions(const Hashtable&, const MessageFormatDataModel::OptionMap&, UErrorCode&) const;
     void formatFunctionCall(const FormatterFactory&, const Hashtable&, const MessageFormatDataModel::OptionMap&, const MessageFormatDataModel::Operand&, UnicodeString&, UErrorCode&) const;
     void formatOperand(const Hashtable&, const MessageFormatDataModel::Operand&, UErrorCode&, UnicodeString&) const;
     void formatSelectors(const Hashtable& arguments, const MessageFormatDataModel::ExpressionList& selectors, const MessageFormatDataModel::VariantMap& variants, UErrorCode &status, UnicodeString& result) const;

     // Function registry methods
     bool isBuiltInSelector(const FunctionName&) const;
     bool isBuiltInFormatter(const FunctionName&) const;
     const SelectorFactory* lookupSelectorFactory(const FunctionName&, UErrorCode& status) const;
     const FormatterFactory* lookupFormatterFactory(const FunctionName&, UErrorCode& status) const;

     // Precondition: custom function registry exists
     const FunctionRegistry& getCustomFunctionRegistry() const {
         U_ASSERT(customFunctionRegistry.isValid());
         return *customFunctionRegistry;
     }

     // Convenience method for formatting selectors
     Hashtable* emptyOptions(UErrorCode& success) const {
         // Creates a new hashtable -- this is annoying!
         if (U_FAILURE(success)) {
             return nullptr;
         }
         return new Hashtable(compareVariableName, nullptr, success);
     }

     // Checking for resolution errors
     void checkDeclarations(const Hashtable&, UErrorCode&) const;
     void check(const Hashtable&, const UVector&, const MessageFormatDataModel::Expression&, UErrorCode&) const;
     void check(const Hashtable&, const UVector&, const MessageFormatDataModel::Operand&, UErrorCode&) const;
     void check(const Hashtable&, const UVector&, const MessageFormatDataModel::OptionMap&, UErrorCode&) const;

     // Registry for built-in functions
     LocalPointer<FunctionRegistry> standardFunctionRegistry;
     // Registry for custom functions; may be null if no custom registry supplied
     LocalPointer<FunctionRegistry> customFunctionRegistry;

     // Data model, representing the parsed message
     LocalPointer<MessageFormatDataModel> dataModel;

     // Normalized version of the input string (optional whitespace removed)
     LocalPointer<UnicodeString> normalizedInput;

     // The locale this MessageFormatter was created with
     const Locale locale;
}; // class MessageFormatter

// For how this class is used, see the references to (integer, variant) tuples
// in https://github.com/unicode-org/message-format-wg/blob/main/spec/formatting.md#pattern-selection
// Ideally this would have been a private class nested in MessageFormatter,
// but sorting comparators need to reference it
class PrioritizedVariant : public UMemory {
public:
    int32_t priority;
    const MessageFormatDataModel::SelectorKeys& keys;
    const MessageFormatDataModel::Pattern& pat;
    PrioritizedVariant(uint32_t p,
                       const MessageFormatDataModel::SelectorKeys& k,
                       const MessageFormatDataModel::Pattern& pattern) : priority(p), keys(k), pat(pattern) {}
}; // class PrioritizedVariant

} // namespace message2
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif /* U_SHOW_CPLUSPLUS_API */

#endif // MESSAGEFORMAT2_H

#endif // U_HIDE_DEPRECATED_API
// eof
