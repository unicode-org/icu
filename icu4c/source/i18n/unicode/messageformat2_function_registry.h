// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#ifndef U_HIDE_DEPRECATED_API

#ifndef MESSAGEFORMAT2_FUNCTION_REGISTRY_H
#define MESSAGEFORMAT2_FUNCTION_REGISTRY_H

#if U_SHOW_CPLUSPLUS_API

#if !UCONFIG_NO_FORMATTING

#include "unicode/format.h"
#include "unicode/messageformat2_data_model.h"
#include "unicode/messageformat2_macros.h"
#include "unicode/numberformatter.h"
#include "unicode/unistr.h"
#include "unicode/utypes.h"

U_NAMESPACE_BEGIN namespace message2 {

typedef enum {
    CARDINAL,
    ORDINAL
} PluralType;

// TODO: can we use lambdas instead, as in icu4j?

// Fixed options = arguments passed in to format()
// Variable options = option list specified in a single expression


class U_COMMON_API Formatter : public UMemory {
 public:
    // TODO: for now representing the argument as a string. Not sure if that's right; Java
    // uses `Object`.
    virtual UnicodeString& format(const UnicodeString& toFormat, const Hashtable& variableOptions, UnicodeString& result, UErrorCode& errorCode) const = 0;
};

// Interface/mixin class
class U_COMMON_API Selector : public UMemory {
 public:
    // TODO: Same question about the `value` argument as in Formatter
    virtual bool matches(const UnicodeString& value, const UnicodeString& key, const Hashtable& options) const = 0;
};

// Interface/mixin classes
class U_COMMON_API FormatterFactory : public UMemory {
  public:
    virtual const Formatter& createFormatter(Locale locale, const Hashtable& fixedOptions) const = 0;
};

class U_COMMON_API SelectorFactory : public UMemory {
  public:
    virtual const Selector& createSelector(Locale locale, const Hashtable& fixedOptions) const = 0;
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
    // Returns null on failure
    const FormatterFactory* getFormatter(const UnicodeString& formatterName) const;
    const SelectorFactory* getSelector(const UnicodeString& selectorName) const;
    // Not sure yet about the others from icu4j

    class Builder {
      private:
        friend class FunctionRegistry;

        Builder();
        LocalPointer<Hashtable> formatters;
        LocalPointer<Hashtable> selectors;
      public:
        // Adopts `formatterFunction`
        Builder& setFormatter(const UnicodeString& formatterName, FormatterFactory* formatterFactory);
        // Adopts `selectorFunction`
        Builder& setSelector(const UnicodeString& selectorName, SelectorFactory* selectorFactory, UErrorCode& errorCode);

        FunctionRegistry* build(UErrorCode& errorCode);
        // Not sure yet about the others from icu4j
    };
    static Builder* builder(UErrorCode& errorCode);

 private:
    friend class MessageFormatter;
    friend class Builder;

    // Adopts `f` and `s`
    FunctionRegistry(Hashtable* f, Hashtable* s) : formatters(f), selectors(s) {}

    bool has(const FunctionName&) const;
    const LocalPointer<Hashtable> formatters;
    const LocalPointer<Hashtable> selectors;
 };

// Built-in functions
/*
      Following icu4j, the standard functions are :datetime, :number,
      :identity, :plural, :selectordinal, :select, and :gender.
      TODO: Subject to change
*/
class StandardFunctions {
    friend class MessageFormatter;

    class DateTimeFactory : public FormatterFactory {
    private:
        friend class MessageFormatter;

        const Formatter& createFormatter(Locale locale, const Hashtable& arguments) const;
    };

    class DateTime : Formatter {
        private:
        friend class MessageFormatter;

        UnicodeString& format(const UnicodeString& toFormat, const Hashtable& variableOptions);
        DateTime(Locale loc, const Hashtable& opts) : locale(loc), fixedOptions(opts) {}
        const Locale locale;
        const Hashtable& fixedOptions;
    };

    class NumberFactory : public FormatterFactory {
        private:
        friend class MessageFormatter;

        const Formatter& createFormatter(Locale locale, const Hashtable& arguments) const;
    };
        
    class Number : Formatter {
        private:
        friend class MessageFormatter;

        void format(const UnicodeString& toFormat, const Hashtable& variableOptions, UnicodeString& result, UErrorCode& errorCode);
        Number(Locale loc, const Hashtable& opts) : locale(loc), fixedOptions(opts), icuFormatter(number::NumberFormatter::withLocale(loc)) {}

        const Locale locale;
        const Hashtable& fixedOptions;
        const number::LocalizedNumberFormatter icuFormatter;
    };

    class IdentityFactory : public SelectorFactory {
        private:
        friend class MessageFormatter;

        const Selector& createSelector(Locale locale, const Hashtable& arguments) const;
    };

    class Identity : Selector {
        private:
        friend class MessageFormatter;

        UnicodeString format(const UnicodeString& toFormat, const Hashtable& variableOptions);
    };

    class PluralFactory : public SelectorFactory {
        private:
        friend class MessageFormatter;

        PluralFactory(PluralType t) : type(t) {}
        const Selector& createSelector(Locale locale, const Hashtable& arguments) const;

        const PluralType type;
    };

    class Plural : Selector {
        friend class MessageFormatter;

        UnicodeString format(const UnicodeString& toFormat, const Hashtable& variableOptions);
        Plural(Locale loc, const Hashtable& opts, PluralType t) : type(t), locale(loc), fixedOptions(opts) {}

        const PluralType type;
        const Locale locale;
        const Hashtable& fixedOptions;
    };

    class TextFactory : public SelectorFactory {
        private:
        friend class MessageFormatter;

        TextFactory();
        const Selector& createSelector(Locale locale, const Hashtable& arguments) const;
    };

    class TextSelector : Selector {
        friend class MessageFormatter;

        UnicodeString format(const UnicodeString& toFormat, const Hashtable& variableOptions);
    };
};

} // namespace message2
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif /* U_SHOW_CPLUSPLUS_API */

#endif // MESSAGEFORMAT2_FUNCTION_REGISTRY_H

#endif // U_HIDE_DEPRECATED_API
// eof
