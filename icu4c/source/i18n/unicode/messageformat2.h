// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#ifndef U_HIDE_DEPRECATED_API

#ifndef MESSAGEFORMAT2_H
#define MESSAGEFORMAT2_H

#if U_SHOW_CPLUSPLUS_API

/**
 * \file
 * \brief C++ API: Formats messages in a language-neutral way using the draft MessageFormat 2.0.
 */

#if !UCONFIG_NO_FORMATTING

#include "unicode/format.h"
#include "unicode/messageformat2_data_model.h"
#include "unicode/messageformat2_macros.h"
#include "unicode/unistr.h"
#include "unicode/utypes.h"

U_NAMESPACE_BEGIN namespace message2 {

class FunctionRegistry;
class FormatterFunction;
class SelectorFunction; 

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

class U_I18N_API MessageFormatter : public Format {
  public:
    /**
     * Destructor.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    virtual ~MessageFormatter();

    // Not yet implemented
    virtual bool operator==(const Format &other) const override;
    // Not yet implemented
    virtual bool operator!=(const Format &other) const;

    // Not yet implemented
    virtual UnicodeString &format(const Formattable &obj, UnicodeString &appendTo, FieldPosition &pos,
                                  UErrorCode &status) const override;

    // Not yet implemented
    virtual void parseObject(const UnicodeString &source, Formattable &result,
                             ParsePosition &pos) const override;

    /**
     * Clones this Format object polymorphically.  The caller owns the
     * result and should delete it when done.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */

    virtual MessageFormatter *clone() const override;

    /**
     * Constructs a new MessageFormatter from an existing one.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    // TODO: I'm not sure if this is needed; see comment in implementation in messageformat2.cpp
    //    MessageFormatter(const MessageFormatter &);

    /**
     * Returns a unique class ID POLYMORPHICALLY.  Pure virtual override.
     * This method is to implement a simple version of RTTI, since not all
     * C++ compilers support genuine RTTI.  Polymorphic operator==() and
     * clone() methods call this method.
     *
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    virtual UClassID getDynamicClassID(void) const override;

    /**
     * Return the class ID for this class.  This is useful only for
     * comparing to a return value from getDynamicClassID().  For example:
     * <pre>
     * .   Base* polymorphic_pointer = createPolymorphicObject();
     * .   if (polymorphic_pointer->getDynamicClassID() ==
     * .      Derived::getStaticClassID()) ...
     * </pre>
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    static UClassID U_EXPORT2 getStaticClassID(void);

    Locale getLocale() const;
    UnicodeString& getPattern() const;

    // Give public access to the data model
    const MessageFormatDataModel& getDataModel() const { return *dataModel; }

    // TODO
    // Given a hash table mapping argument names (strings) to (what?),
    // return a string representation of the formatted message.
    UnicodeString formatToString(const Hashtable& arguments);

    // Not yet implemented (class FormattedMessage is not yet defined)
    // FormattedMessage format(const Hashtable& arguments);

    /**
     * The mutable Builder class allows each part of the MessageFormatter to be initialized
     * separately; calling its `build()` method yields an immutable MessageFormatter.
     */
    class Builder {
        friend class MessageFormatter;
      private:
        // prevent direct construction
        Builder() : pattern(nullptr), functionRegistry(nullptr) {
            // TODO: initialize locale to default
        }

        // The pattern to be parsed to generate the formatted message
        LocalPointer<UnicodeString> pattern;
        // The data model to be used to generate the formatted message
        // Invariant: !(pattern.isValid() && dataModel.isValid())
        LocalPointer<MessageFormatDataModel> dataModel;
        // TODO: set default locale
        Locale locale;
        // TODO: set default function registry
        FunctionRegistry* functionRegistry;
      public:
        void setLocale(Locale locale);
        void setPattern(UnicodeString* pattern);
        // Takes ownership of the FunctionRegistry
        void setFunctionRegistry(FunctionRegistry* functionRegistry);
        // Takes ownership of the data model
        void setDataModel(MessageFormatDataModel* dataModel);
        // Returns an immutable MessageFormatter
        
        // TODO: fix this comment

    /**
     * Constructs a new MessageFormatter using the given pattern and the
     * default locale.
     *
     * @param pattern   Pattern used to construct object.
     * @param parseError Struct to receive information on the position
     *                   of an error within the pattern.
     * @param status    Input/output error code.  If the
     *                  pattern cannot be parsed, set to failure code.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */

      // Note: this is a destructive build(); it invalidates the builder
        MessageFormatter* build(UParseError& parseError, UErrorCode& errorCode);
    };

    static Builder* builder(UErrorCode& errorCode);

    // TODO: Shouldn't be public, only for testing
    const UnicodeString& getNormalizedPattern() const { return *normalizedInput; }

  private:
    friend class Builder;
    // Only called by the builder
    MessageFormatter(MessageFormatter::Builder& builder, UParseError &parseError, UErrorCode &status);

    MessageFormatter() = delete; // default constructor not implemented

    // Do not define default assignment operator
    const MessageFormatter &operator=(const MessageFormatter &) = delete;

    // Parser class (private)
    class Parser : UMemory {
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
             void parseNmtoken(UErrorCode&, VariableName&);
             void parseName(UErrorCode&, VariableName&);
             void parseVariableName(UErrorCode&, VariableName&);
             void parseFunction(UErrorCode&, FunctionName&);
             void parseEscapeSequence(EscapeKind, UErrorCode &, String&);
             void parseLiteralEscape(UErrorCode &, String&);
             void parseLiteral(UErrorCode &, String&);
             void parseOption(UErrorCode &, MessageFormatDataModel::OptionMap::Builder&);
             MessageFormatDataModel::OptionMap* parseOptions(UErrorCode &);
             void parseReservedEscape(UErrorCode&, String&);
             void parseReservedChunk(UErrorCode &, MessageFormatDataModel::Reserved::Builder&);
             MessageFormatDataModel::Operator* parseReserved(UErrorCode &);
             MessageFormatDataModel::Operator* parseAnnotation(UErrorCode &);
             MessageFormatDataModel::Expression* parseLiteralOrVariableWithAnnotation(bool, UErrorCode &);
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


    // Data model, representing the parsed message
    LocalPointer<MessageFormatDataModel> dataModel;

    // Normalized version of the input string (optional whitespace removed)
    LocalPointer<UnicodeString> normalizedInput;
}; // class MessageFormatter

/**
 * Defines mappings from names of formatters and selectors to functions implementing them.
 * The required set of formatter and selector functions is defined in the spec. Users can
 * also define custom formatter and selector functions.
 *
 * @internal ICU 74.0 technology preview
 * @deprecated This API is for technology preview only.
 */
class U_I18N_API FunctionRegistry : UMemory {
 public:
    FormatterFunction getFormatter(const UnicodeString& formatterName);
    SelectorFunction getSelector(const UnicodeString& selectorName);
    // Not sure yet about the others from icu4j

    class Builder {
      private:
        Builder() {} // prevent direct construction
      public:
        Builder setFormatter(const UnicodeString& formatterName, FormatterFunction formatterFunction);
        Builder setSelector(const UnicodeString& selectorName, SelectorFunction selectorFunction);

        FunctionRegistry build();
        // Not sure yet about the others from icu4j
    };
 };

// TODO: can we use lambdas instead, as in icu4j?

// Interface/mixin class
class U_COMMON_API FormatterFunction : UMemory {
 public:
    // TODO: for now representing the argument as a string. Not sure if that's right; Java
    // uses `Object`.
    virtual UnicodeString format(const UnicodeString& toFormat, const MessageFormatDataModel::OptionMap& variableOptions) const = 0;
    // Takes ownership of `fixedOptions`
    FormatterFunction(Locale locale, Hashtable* fixedOptions);
};

// Interface/mixin class
class U_COMMON_API SelectorFunction : UMemory {
 public:
    // TODO: Same question about the `value` argument as in FormatterFunction
    virtual bool matches(const UnicodeString& value, const UnicodeString& key, const MessageFormatDataModel::OptionMap& options) const = 0;
    // Takes ownership of `fixedOptions`
    SelectorFunction(Locale locale, Hashtable* fixedOptions);
};

} // namespace message2
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif /* U_SHOW_CPLUSPLUS_API */

#endif // MESSAGEFORMAT2_H

#endif // U_HIDE_DEPRECATED_API
// eof
