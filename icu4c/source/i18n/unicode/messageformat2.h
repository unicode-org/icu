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
#include "unicode/messageformat2_context.h"
#include "unicode/messageformat2_data_model.h"
#include "unicode/messageformat2_formatting_context.h"
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

    /**
     * Formats the message to a string, using the data model that was previously set or parsed,
     * and the given `arguments` object.
     *
     * @param arguments Reference to message arguments
     * @param status    Input/output error code used to indicate syntax errors, data model
     *                  errors, resolution errors, formatting errors, selection errors, as well
     *                  as other errors (such as memory allocation failures). Partial output
     *                  is still provided in the presence of most error types.
     * @param result    Mutable reference to a string that the output will be appended to.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    void formatToString(const MessageArguments& arguments, UErrorCode &status, UnicodeString &result) const;

    /**
     * Accesses the locale that this `MessageFormatter` object was created with.
     *
     * @return A reference to the locale.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    const Locale& getLocale() const { return locale; }

    /**
     * Serializes the data model as a string in MessageFormat 2.0 syntax.
     *
     * @param result    Mutable reference to a string that the output will be appended to.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    void getPattern(UnicodeString& result) const {
        // Converts the current data model back to a string
        U_ASSERT(dataModelOK());
        Serializer serializer(getDataModel(), result);
        serializer.serialize();
    }

    /**
     * Accesses the data model referred to by this
     * `MessageFormatter` object.
     *
     * @return A reference to the data model.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    // Give public access to the data model
    const MessageFormatDataModel& getDataModel() const;

    /**
     * The mutable Builder class allows each part of the MessageFormatter to be initialized
     * separately; calling its `build()` method yields an immutable MessageFormatter.
     */
    class U_I18N_API Builder {
    private:
       friend class MessageFormatter;

       Builder() : locale(Locale::getDefault()), customFunctionRegistry(nullptr) {}

       // The pattern to be parsed to generate the formatted message
       UnicodeString pattern;
       bool hasPattern;
       // The data model to be used to generate the formatted message
       // Invariant: !(hasPattern && dataModel != nullptr)
       const MessageFormatDataModel* dataModel;
       Locale locale;
       LocalPointer<FunctionRegistry> standardFunctionRegistry;
       // Not owned
       const FunctionRegistry* customFunctionRegistry;

    public:
       /**
        * Sets the locale to use for formatting.
        *
        * @param locale The desired locale.
        * @return       A reference to the builder.
        *
        * @internal ICU 74.0 technology preview
        * @deprecated This API is for technology preview only.
        */
        Builder& setLocale(const Locale& locale);
       /**
        * Sets the pattern to be parsed into a data model. (Parsing is
        * delayed until `build()` is called.) If a data model was
        * previously set, the reference to it held by this builder
        * is removed.
        *
        * @param pattern A string in MessageFormat 2.0 syntax.
        * @return       A reference to the builder.
        *
        * @internal ICU 74.0 technology preview
        * @deprecated This API is for technology preview only.
        */
        Builder& setPattern(const UnicodeString& pattern);
       /**
        * Sets a custom function registry.
        *
        * @param functionRegistry Function registry to use; this argument is
        *        not adopted, and the caller must ensure its lifetime contains
        *        the lifetime of the `MessageFormatter` object built by this
        *        builder.
        * @return       A reference to the builder.
        *
        * @internal ICU 74.0 technology preview
        * @deprecated This API is for technology preview only.
        */
        Builder& setFunctionRegistry(const FunctionRegistry* functionRegistry);
       /**
        * Sets a data model. If a pattern was previously set, it is removed.
        *
        * @param dataModel Data model to format; this argument is
        *        not adopted, and the caller must ensure its lifetime contains
        *        the lifetime of the `MessageFormatter` object built by this
        *        builder.
        * @return       A reference to the builder.
        *
        * @internal ICU 74.0 technology preview
        * @deprecated This API is for technology preview only.
        */
        Builder& setDataModel(const MessageFormatDataModel* dataModel);
        /**
         * Constructs a new immutable MessageFormatter using the pattern or data model
         * that was previously set, and the locale (if it was previously set)
         * or default locale (otherwise).
         *
         * The builder object (`this`) can still be used after calling `build()`.
         *
         * @param parseError Struct to receive information on the position
         *                   of an error within the pattern (not used if
         *                   the data model is set).
         * @param status    Input/output error code.  If the
         *                  pattern cannot be parsed, or if neither the pattern
         *                  nor the data model is set, set to failure code.
         * @return          The new MessageFormatter object, which is non-null if
         *                  U_SUCCESS(status).
         *
         * @internal ICU 74.0 technology preview
         * @deprecated This API is for technology preview only.
         */
        MessageFormatter* build(UParseError& parseError, UErrorCode& status) const;
    }; // class MessageFormatter::Builder

   /**
     * Returns a new `MessageFormatter::Builder` object.
     *
     * @param status  Input/output error code.
     * @return        The new Builder, which is non-null if U_SUCCESS(status).
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    static Builder* builder(UErrorCode& status);

    // TODO: Shouldn't be public; only used for testing
    const UnicodeString& getNormalizedPattern() const { return normalizedInput; }

  private:
    friend class Builder;
    friend class MessageContext;

    MessageFormatter(const MessageFormatter::Builder& builder, UParseError &parseError, UErrorCode &status);

    MessageFormatter() = delete; // default constructor not implemented

    // Do not define default assignment operator
    const MessageFormatter &operator=(const MessageFormatter &) = delete;

    // Parser class (private)
    class Parser : public UMemory {
    public:
        virtual ~Parser();
        static Parser* create(const UnicodeString &input, MessageFormatDataModel::Builder& dataModelBuilder, UnicodeString& normalizedInput, Errors& errors, UErrorCode& errorCode) {
            if (U_FAILURE(errorCode)) {
                return nullptr;
            }
            Parser* p = new Parser(input, dataModelBuilder, errors, normalizedInput);
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

        Parser(const UnicodeString &input, MessageFormatDataModel::Builder& dataModelBuilder, Errors& e, UnicodeString& normalizedInputRef)
            : source(input), index(0), errors(e), normalizedInput(normalizedInputRef), dataModel(dataModelBuilder) {
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
        template <int32_t N>
        void parseToken(const UChar32 (&)[N], UErrorCode &);
        template <int32_t N>
        void parseTokenWithWhitespace(const UChar32 (&)[N], UErrorCode &);
        void parseName(UErrorCode&, UnicodeString&);
        void parseVariableName(UErrorCode&, UnicodeString&);
        FunctionName* parseFunction(UErrorCode&);
        void parseEscapeSequence(EscapeKind, UErrorCode &, UnicodeString&);
        void parseLiteralEscape(UErrorCode &, UnicodeString&);
        void parseLiteral(UErrorCode &, bool&, UnicodeString&);
        void parseOption(UErrorCode&, MessageFormatDataModel::Operator::Builder&);
        void parseOptions(UErrorCode &, MessageFormatDataModel::Operator::Builder&);
        void parseReservedEscape(UErrorCode&, UnicodeString&);
        void parseReservedChunk(UErrorCode &, MessageFormatDataModel::Reserved::Builder&);
        MessageFormatDataModel::Reserved* parseReserved(UErrorCode &);
        MessageFormatDataModel::Operator* parseAnnotation(UErrorCode &);
        void parseLiteralOrVariableWithAnnotation(bool, UErrorCode &, MessageFormatDataModel::Expression::Builder&);
        MessageFormatDataModel::Expression* parseExpression(bool&, UErrorCode &);
        void parseTextEscape(UErrorCode&, UnicodeString&);
        void parseText(UErrorCode&, UnicodeString&);
        MessageFormatDataModel::Key* parseKey(UErrorCode&);
        MessageFormatDataModel::SelectorKeys* parseNonEmptyKeys(UErrorCode&);
        void errorPattern(UErrorCode&);
        MessageFormatDataModel::Pattern* parsePattern(UErrorCode&);

        // The input string
        const UnicodeString &source;
        // The current position within the input string
        uint32_t index;
        // Represents the current line (and when an error is indicated),
        // character offset within the line of the parse error
        MessageParseError parseError;

        // The structure to use for recording errors
        Errors& errors;

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
        template <int32_t N>
        void emit(const UChar32 (&)[N]);
        void emit(const UnicodeString&);
        void emit(const FunctionName&);
        void emit(const VariableName&);
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
        Checker(const MessageFormatDataModel& m, Errors& e) : dataModel(m), errors(e) {}
    private:
        void requireAnnotated(const TypeEnvironment&, const MessageFormatDataModel::Expression&, UErrorCode&);
        void checkDeclarations(TypeEnvironment&, UErrorCode&);
        void checkSelectors(const TypeEnvironment&, UErrorCode&);
        void checkVariants(UErrorCode&);
        void check(const MessageFormatDataModel::OptionMap&, UErrorCode&);
        void check(const MessageFormatDataModel::Operand&, UErrorCode&);
        void check(const MessageFormatDataModel::Expression&, UErrorCode&);
        void check(const MessageFormatDataModel::Pattern&, UErrorCode&);
        const MessageFormatDataModel& dataModel;
        Errors& errors;
    };

     void resolveVariables(const Environment& env, const MessageFormatDataModel::Operand&, ExpressionContext&, UErrorCode &) const;
     void resolveVariables(const Environment& env, const MessageFormatDataModel::Expression&, ExpressionContext&, UErrorCode &) const;

     // Selection methods
     void resolveSelectors(MessageContext&, const Environment& env, const MessageFormatDataModel::ExpressionList&, UErrorCode&, ExpressionContext**) const;
     void matchSelectorKeys(UnicodeString*/*[]*/, int32_t, ExpressionContext&, UErrorCode&, UnicodeString*, int32_t&) const;
     void resolvePreferences(ExpressionContext**, int32_t, const MessageFormatDataModel::VariantMap&, UErrorCode&, UnicodeString**, int32_t*) const;

     // Formatting methods
     void formatLiteral(const MessageFormatDataModel::Literal&, ExpressionContext&) const;
     void formatPattern(MessageContext&, const Environment&, const MessageFormatDataModel::Pattern&, UErrorCode&, UnicodeString&) const;
     // Formats an expression that appears as a selector
     void formatSelectorExpression(const Environment& env, const MessageFormatDataModel::Expression&, ExpressionContext&, UErrorCode&) const;
     // Formats an expression that appears in a pattern or as the definition of a local variable
     void formatExpression(const Environment&, const MessageFormatDataModel::Expression&, ExpressionContext&, UErrorCode&) const;
     void resolveOptions(const Environment& env, const MessageFormatDataModel::OptionMap&, ExpressionContext&, UErrorCode&) const;
     void formatOperand(const Environment&, const MessageFormatDataModel::Operand&, ExpressionContext&, UErrorCode&) const;
     void evalArgument(const VariableName&, ExpressionContext&) const;
     void formatSelectors(MessageContext& context, const Environment& env, const MessageFormatDataModel::ExpressionList& selectors, const MessageFormatDataModel::VariantMap& variants, UErrorCode &status, UnicodeString& result) const;

     // Function registry methods
     const Formatter* maybeCachedFormatter(MessageContext&, const FunctionName&, UErrorCode& errorCode) const;

     bool hasCustomFunctionRegistry() const {
         return (customFunctionRegistry != nullptr);
     }

     // Precondition: custom function registry exists
     const FunctionRegistry& getCustomFunctionRegistry() const {
         U_ASSERT(hasCustomFunctionRegistry());
         return *customFunctionRegistry;
     }

     // Checking for resolution errors
     void checkDeclarations(MessageContext&, Environment*&, UErrorCode&) const;
     void check(MessageContext&, const Environment&, const MessageFormatDataModel::Expression&, UErrorCode&) const;
     void check(MessageContext&, const Environment&, const MessageFormatDataModel::Operand&, UErrorCode&) const;
     void check(MessageContext&, const Environment&, const MessageFormatDataModel::OptionMap&, UErrorCode&) const;

     void initErrors(UErrorCode&);
     void clearErrors() const;

     // The locale this MessageFormatter was created with
     const Locale locale;

     // Registry for built-in functions
     LocalPointer<FunctionRegistry> standardFunctionRegistry;
     // Registry for custom functions; may be null if no custom registry supplied
     // Note: this is *not* owned by the MessageFormatter object
     const FunctionRegistry* customFunctionRegistry;

     // Data model, representing the parsed message
     // May be either owned (if created by parsing a pattern), or
     // borrowed (if supplied by the builder's setDataModel() method) --
     // the ownedDataModel flag determines which one
     LocalPointer<MessageFormatDataModel> dataModel;
     const MessageFormatDataModel* borrowedDataModel;
     bool ownedDataModel;

     // Upholds the invariant that either the data model or borrowed data model is valid,
     // but not both
     bool dataModelOK() const;

     // Normalized version of the input string (optional whitespace removed)
     UnicodeString normalizedInput;

     // Formatter cache
     LocalPointer<CachedFormatters> cachedFormatters;

     // Errors -- only used while parsing and checking for data model errors; then
     // the MessageContext keeps track of errors
     LocalPointer<Errors> errors;
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
