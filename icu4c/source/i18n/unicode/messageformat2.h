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
#include "unicode/parseerr.h"
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
    MessageFormatDataModel& getDataModel() const;

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
      private:
        Builder() {} // prevent direct construction
        // The pattern to be parsed to generate the formatted message
        UnicodeString pattern;
        // TODO: set default locale
        Locale locale;
        // TODO: set default function registry
        FunctionRegistry* functionRegistry;
      public:
        Builder setLocale(Locale locale);
        Builder setPattern(const UnicodeString& pattern);
        // Takes ownership of the FunctionRegistry
        Builder setFunctionRegistry(FunctionRegistry* functionRegistry);
        // Takes ownership of the data model
        Builder setDataModel(MessageFormatDataModel* dataModel);
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

        MessageFormatter* build(UErrorCode& errorCode);
    };
    
  private:
    // Only called by the builder
    MessageFormatter(const UnicodeString &pattern, UParseError &parseError, UErrorCode &status);

    MessageFormatter() = delete; // default constructor not implemented

    // Do not define default assignment operator
    const MessageFormatter &operator=(const MessageFormatter &) = delete;

    // The parser validates the message and builds the data model
    // from it.
    void parse(const UnicodeString &, UParseError &, UErrorCode &);

    // Data model, representing the parsed message
    MessageFormatDataModel dataModel;
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
    // TODO: Same OptionList type as in data model?
    // TODO: for now representing the argument as a string. Not sure if that's right; Java
    // uses `Object`.
    virtual UnicodeString format(const UnicodeString& toFormat, const OptionList& variableOptions) const = 0;
    // Takes ownership of `fixedOptions`
    FormatterFunction(Locale locale, Hashtable* fixedOptions);
};

// Interface/mixin class
class U_COMMON_API SelectorFunction : UMemory {
 public:
    // TODO: Same OptionList type as in data model?
    // TODO: Same question about the `value` argument as in FormatterFunction
    virtual bool matches(const UnicodeString& value, const UnicodeString& key, const OptionList& options) const = 0;
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
