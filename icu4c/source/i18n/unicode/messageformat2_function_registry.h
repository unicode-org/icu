// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#ifndef U_HIDE_DEPRECATED_API

#ifndef MESSAGEFORMAT2_FUNCTION_REGISTRY_H
#define MESSAGEFORMAT2_FUNCTION_REGISTRY_H

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


} // namespace message2
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif /* U_SHOW_CPLUSPLUS_API */

#endif // MESSAGEFORMAT2_FUNCTION_REGISTRY_H

#endif // U_HIDE_DEPRECATED_API
// eof
