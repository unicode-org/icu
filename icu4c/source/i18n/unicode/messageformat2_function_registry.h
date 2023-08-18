// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#ifndef U_HIDE_DEPRECATED_API

#ifndef MESSAGEFORMAT2_FUNCTION_REGISTRY_H
#define MESSAGEFORMAT2_FUNCTION_REGISTRY_H

#if U_SHOW_CPLUSPLUS_API

#if !UCONFIG_NO_FORMATTING

#include "unicode/datefmt.h"
#include "unicode/format.h"
#include "unicode/messageformat2_data_model.h"
#include "unicode/messageformat2_formatted_value.h"
#include "unicode/messageformat2_macros.h"
#include "unicode/numberformatter.h"
#include "unicode/unistr.h"
#include "unicode/upluralrules.h"
#include "unicode/utypes.h"

U_NAMESPACE_BEGIN namespace message2 {

// TODO: can we use lambdas instead, as in icu4j?

// TODO: create some other data structure to represent resolved options,
// so as to avoid using a Hashtable
using FunctionName = MessageFormatDataModel::FunctionName;

class U_COMMON_API Formatter : public UMemory {
 public:
    // TODO: Java uses `Object` for the argument type. Using `FormattedPlaceholder` here.
    // See if any examples require using an argument that's not a FormattedPlaceholder
    // Needs an error code because internal details may require calling functions that can fail
    // (e.g. parsing a string as a number, for Number)

    // TODO: should change the Hashtable type to a specific `Options` type for type safety

    // Operand can be null
    virtual FormattedPlaceholder* format(FormattedPlaceholder* toFormat, const Hashtable& options, UErrorCode& errorCode) const = 0;
    virtual ~Formatter();
};

// Interface/mixin class
class U_COMMON_API Selector : public UMemory {
 public:
    // TODO: Same comment about the `value` argument as in Formatter
    // TODO: Needs an error code because parsing `value` as a number can error
    // Takes an array of keys and returns a sub-array (through the out-param `prefs`)
    // containing all matching keys, sorted in order of preference (best first).
    /*
      TODO: Needed to change this in order to support best-match.
      See selectKey() in message-value.ts
     */
    // `value` may be null, because the selector might be nullary
    virtual void selectKey(const FormattedPlaceholder* value, const UnicodeString* keys/*[]*/, size_t numKeys, const Hashtable& options, UnicodeString* prefs/*[]*/, size_t& numMatching, UErrorCode& errorCode) const = 0;
    virtual ~Selector();
};

// TODO: This differs from ICU4J, where a separate `fixedOptions` map is passed in
// We evaluate options eagerly, so all options are resolved and are per-expression

// Interface/mixin classes
class U_COMMON_API FormatterFactory : public UMemory {
  public:
    // Since this allocates a new Formatter and has to indicate failure,
    // it returns a Formatter* (not a Formatter&)
    // TODO
    // Note: this method is not const, as formatter factories can have local state
    virtual Formatter* createFormatter(Locale locale, UErrorCode& errorCode) = 0;
    virtual ~FormatterFactory();
};

class U_COMMON_API SelectorFactory : public UMemory {
  public:
    // Same comment as FormatterFactory::createFormatter
    virtual Selector* createSelector(Locale locale, UErrorCode& errorCode) const = 0;
    virtual ~SelectorFactory();
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
    FormatterFactory* getFormatter(const FunctionName& formatterName) const;
    const SelectorFactory* getSelector(const FunctionName& selectorName) const;
    // Not sure yet about the others from icu4j

    class Builder {
      private:
        friend class FunctionRegistry;

        Builder(UErrorCode& errorCode);
        LocalPointer<Hashtable> formatters;
        LocalPointer<Hashtable> selectors;
      public:
        // Adopts `formatterFunction`
        Builder& setFormatter(const FunctionName& formatterName, FormatterFactory* formatterFactory, UErrorCode& errorCode);
        // Adopts `selectorFunction`
        Builder& setSelector(const FunctionName& selectorName, SelectorFactory* selectorFactory, UErrorCode& errorCode);

        FunctionRegistry* build(UErrorCode& errorCode);
        // Not sure yet about the others from icu4j
    };
    static Builder* builder(UErrorCode& errorCode);

 private:
    friend class MessageFormatter;
    friend class Builder;

    // Adopts `f` and `s`
    FunctionRegistry(Hashtable* f, Hashtable* s) : formatters(f), selectors(s) {}
//    FunctionRegistry(const FunctionRegistry& other);

    // Debugging; should only be called on a function registry with
    // all the standard functions registered
    void checkFormatter(const char*) const;
    void checkSelector(const char*) const;
    void checkStandard() const;

    bool hasFormatter(const FunctionName& f) const {
        if (!formatters->containsKey(f.toString())) {
            return false;
        }
        U_ASSERT(getFormatter(f) != nullptr);
        return true;
    }
    bool hasSelector(const FunctionName& s) const {
        if (!selectors->containsKey(s.toString())) {
            return false;
        }
        U_ASSERT(getSelector(s) != nullptr);
        return true;
    }
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
    public:
        Formatter* createFormatter(Locale locale, UErrorCode& errorCode);
    };

    class DateTime : public Formatter {
        public:
        FormattedPlaceholder* format(FormattedPlaceholder* toFormat, const Hashtable& options, UErrorCode& errorCode) const;

        private:
        Locale locale;
        friend class DateTimeFactory;
        DateTime(Locale l) : locale(l) {}
        const LocalPointer<icu::DateFormat> icuFormatter;
    };

    class NumberFactory : public FormatterFactory {
    public:
        Formatter* createFormatter(Locale locale, UErrorCode& errorCode);
    };
        
    class Number : public Formatter {
        public:
        FormattedPlaceholder* format(FormattedPlaceholder* toFormat, const Hashtable& options, UErrorCode& errorCode) const;

        private:
        friend class NumberFactory;

        Number(Locale loc) : locale(loc), icuFormatter(number::NumberFormatter::withLocale(loc)) {}

        const Locale locale;
        const number::LocalizedNumberFormatter icuFormatter;
    };

    class IdentityFactory : public FormatterFactory {
    public:
        Formatter* createFormatter(Locale locale, UErrorCode& errorCode);
    };

    class Identity : public Formatter {
    public:
        FormattedPlaceholder* format(FormattedPlaceholder* toFormat, const Hashtable& options, UErrorCode& errorCode) const;
        
    private:
        friend class IdentityFactory;

        Identity() {}
        ~Identity();
    };

    class PluralFactory : public SelectorFactory {
    public:
        virtual ~PluralFactory();
        Selector* createSelector(Locale locale, UErrorCode& errorCode) const;

    private:
        friend class MessageFormatter;

        PluralFactory(UPluralType t) : type(t) {}
        const UPluralType type;
    };

    class Plural : public Selector {
        public:
        void selectKey(const FormattedPlaceholder* value, const UnicodeString* keys, size_t numKeys, const Hashtable& options, UnicodeString* prefs, size_t& numMatching, UErrorCode& errorCode) const;

        private:
        friend class PluralFactory;

        // Adopts `r`
        Plural(Locale loc, UPluralType t, PluralRules* r) : locale(loc), type(t), rules(r) {}
        ~Plural();

        const Locale locale;
        const UPluralType type;
        LocalPointer<PluralRules> rules;
    };

    class TextFactory : public SelectorFactory {
        public:
        Selector* createSelector(Locale locale, UErrorCode& errorCode) const;
    };

    class TextSelector : public Selector {
    public:
        void selectKey(const FormattedPlaceholder* value, const UnicodeString* keys, size_t numKeys, const Hashtable& options, UnicodeString* prefs, size_t& numMatching, UErrorCode& errorCode) const;
        
    private:
        friend class TextFactory;

        // Formatting `value` to a string might require the locale 
        const Locale locale;

        TextSelector(Locale l) : locale(l) {}
        ~TextSelector();
    };
};

} // namespace message2
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif /* U_SHOW_CPLUSPLUS_API */

#endif // MESSAGEFORMAT2_FUNCTION_REGISTRY_H

#endif // U_HIDE_DEPRECATED_API
// eof
