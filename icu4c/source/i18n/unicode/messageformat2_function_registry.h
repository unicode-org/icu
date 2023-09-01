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
#include "unicode/messageformat2_formatting_context.h"
#include "unicode/messageformat2_macros.h"
#include "unicode/numberformatter.h"
#include "unicode/unistr.h"
#include "unicode/upluralrules.h"
#include "unicode/utypes.h"

U_NAMESPACE_BEGIN namespace message2 {

class Formatter;
class Selector;

/**
 * Interface that factory classes for creating formatters must implement.
 *
 * @internal ICU 74.0 technology preview
 * @deprecated This API is for technology preview only.
 */
class U_COMMON_API FormatterFactory : public UMemory {
  public:
    /**
     * Constructs a new formatter object. May return null if a memory
     * allocation error or other error occurs; must return a non-null result
     * if `U_SUCCESS(status)` at the end of the call. This method is not const;
     * formatter factories with local state may be defined.
     *
     * @param locale Locale to be used by the formatter.
     * @param status    Input/output error code.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    virtual Formatter* createFormatter(const Locale& locale, UErrorCode& status) = 0;
    virtual ~FormatterFactory();
};

/**
 * Interface that factory classes for creating selectors must implement.
 *
 * @internal ICU 74.0 technology preview
 * @deprecated This API is for technology preview only.
 */
class U_COMMON_API SelectorFactory : public UMemory {
  public:
    /**
     * Constructs a new selector object. May return null if a memory allocation
     * error or other error occurs; must return a non-null result if
     *`U_SUCCESS(status)` at the end of the call.
     *
     * @param locale Locale to be used by the selector.
     * @param status    Input/output error code.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    virtual Selector* createSelector(const Locale& locale, UErrorCode& status) const = 0;
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
    /**
     * Looks up a formatter factory by the name of the formatter. Returns null
     * if the given formatter factory has not been registered. The result is non-const,
     * since formatter factories may have local state.
     *
     * @param formatterName Name of the desired formatter.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    FormatterFactory* getFormatter(const FunctionName& formatterName) const;
    /**
     * Looks up a selector factory by the name of the selector. Returns null
     * if the given selector factory has not been registered.
     *
     * @param selectorName Name of the desired selector.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    const SelectorFactory* getSelector(const FunctionName& selectorName) const;

    /**
     * The mutable Builder class allows each formatter and selector factory
     * to be initialized separately; calling its `build()` method yields an
     * immutable FunctionRegistry object.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    class Builder {
      private:
        friend class FunctionRegistry;

        Builder(UErrorCode& status);
        LocalPointer<Hashtable> formatters;
        LocalPointer<Hashtable> selectors;
      public:
        /**
         * Registers a formatter factory to a given formatter name. Adopts `formatterFactory`.
         *
         * @param formatterName Name of the formatter being registered.
         * @param formatterFactory A FormatterFactory object to use for creating `formatterName`
         *        formatters.
         * @param status Input/output error code.
         *
         * @internal ICU 74.0 technology preview
         * @deprecated This API is for technology preview only.
         */
        Builder& setFormatter(const FunctionName& formatterName, FormatterFactory* formatterFactory, UErrorCode& status);
        /**
         * Registers a selector factory to a given selector name. Adopts `selectorFactory`.
         *
         * @param selectorName Name of the selector being registered.
         * @param selectorFactory A SelectorFactory object to use for creating `selectorName`
         *        selectors.
         * @param status Input/output error code.
         *
         * @internal ICU 74.0 technology preview
         * @deprecated This API is for technology preview only.
         */
        Builder& setSelector(const FunctionName& selectorName, SelectorFactory* selectorFactory, UErrorCode& status);
        /**
         * Creates an immutable `FunctionRegistry` object with the selectors and formatters
         * that were previously registered. The builder cannot be used after this call.
         *
         * @param status  Input/output error code.
         *
         * @internal ICU 74.0 technology preview
         * @deprecated This API is for technology preview only.
         */
        FunctionRegistry* build(UErrorCode& status);
    };
   /**
     * Returns a new `FunctionRegistry::Builder` object.
     *
     * @param status  Input/output error code.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    static Builder* builder(UErrorCode& status);

 private:
    friend class Builder;
    friend class MessageContext;
    friend class MessageFormatter;

    // Adopts `f` and `s`
    FunctionRegistry(Hashtable* f, Hashtable* s) : formatters(f), selectors(s) {}

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

/**
 * Interface that formatter classes must implement.
 *
 * @internal ICU 74.0 technology preview
 * @deprecated This API is for technology preview only.
 */
class U_COMMON_API Formatter : public UMemory {
 public:
    /**
     * Formats the input passed in `context` by setting an output using one of the
     * `FormattingContext` methods or indicating an error.
     *
     * @param context Formatting context; captures the unnamed function argument,
     *        current output, named options, and output. See the `FormattingContext`
     *        documentation for more details.
     * @param status    Input/output error code. Should not be set directly by the
     *        custom formatter, which should use `FormattingContext::setFormattingWarning()`
     *        to signal errors. The custom formatter may pass `status` to other ICU functions
     *        that can signal errors using this mechanism.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    virtual void format(FormattingContext& context, UErrorCode& status) const = 0;
    virtual ~Formatter();
};


/**
 * Interface that selector classes must implement.
 *
 * @internal ICU 74.0 technology preview
 * @deprecated This API is for technology preview only.
 */
class U_COMMON_API Selector : public UMemory {
 public:
    /**
     * Compares the input passed in `context` to an array of keys, and returns an array of matching
     * keys sorted by preference.
     *
     * @param context Formatting context; captures the unnamed function argument and named options.
     *        See the `FormattingContext` documentation for more details.
     * @param keys An array of strings that are compared to the input (`context.getFormattableInput()`)
     *        in an implementation-specific way.
     * @param numKeys The length of the `keys` array.
     * @param prefs A mutable reference to an array of strings. `selectKey()` should set the contents
     *        of `prefs` to a subset of `keys`, with the best match placed at the lowest index.
     * @param numMatching A mutable reference that should be set to the length of the `prefs` array.
     * @param status    Input/output error code. Should not be set directly by the
     *        custom selector, which should use `FormattingContext::setSelectorError()`
     *        to signal errors. The custom selector may pass `status` to other ICU functions
     *        that can signal errors using this mechanism.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    virtual void selectKey(FormattingContext& context, const UnicodeString* keys/*[]*/, int32_t numKeys, UnicodeString* prefs/*[]*/, int32_t& numMatching, UErrorCode& status) const = 0;
    virtual ~Selector();
};

// Built-in functions
/*
      The standard functions are :datetime, :number,
      :identity, :plural, :selectordinal, :select, and :gender.
      Subject to change
*/
class StandardFunctions {
    friend class MessageFormatter;

    class DateTimeFactory : public FormatterFactory {
    public:
        Formatter* createFormatter(const Locale& locale, UErrorCode& status);
    };

    class DateTime : public Formatter {
        public:
        void format(FormattingContext& context, UErrorCode& status) const;

        private:
        const Locale& locale;
        friend class DateTimeFactory;
        DateTime(const Locale& l) : locale(l) {}
        const LocalPointer<icu::DateFormat> icuFormatter;
    };

    class NumberFactory : public FormatterFactory {
    public:
        Formatter* createFormatter(const Locale& locale, UErrorCode& status);
    };
        
    class Number : public Formatter {
        public:
        void format(FormattingContext& context, UErrorCode& status) const;

        private:
        friend class NumberFactory;

        Number(const Locale& loc) : locale(loc), icuFormatter(number::NumberFormatter::withLocale(loc)) {}

        const Locale& locale;
        const number::LocalizedNumberFormatter icuFormatter;
    };

    class IdentityFactory : public FormatterFactory {
    public:
        Formatter* createFormatter(const Locale& locale, UErrorCode& status);
    };

    class Identity : public Formatter {
    public:
        void format(FormattingContext& context, UErrorCode& status) const;
        
    private:
        friend class IdentityFactory;

        const Locale& locale;
        Identity(const Locale& loc) : locale(loc) {}
        ~Identity();
    };

    class PluralFactory : public SelectorFactory {
    public:
        virtual ~PluralFactory();
        Selector* createSelector(const Locale& locale, UErrorCode& status) const;

    private:
        friend class MessageFormatter;

        PluralFactory(UPluralType t) : type(t) {}
        const UPluralType type;
    };

    class Plural : public Selector {
        public:
        void selectKey(FormattingContext& context, const UnicodeString* keys/*[]*/, int32_t numKeys, UnicodeString* prefs/*[]*/, int32_t& numMatching, UErrorCode& status) const;

        private:
        friend class PluralFactory;

        // Adopts `r`
        Plural(const Locale& loc, UPluralType t, PluralRules* r) : locale(loc), type(t), rules(r) {}
        ~Plural();

        const Locale& locale;
        const UPluralType type;
        LocalPointer<PluralRules> rules;
    };

    class TextFactory : public SelectorFactory {
        public:
        Selector* createSelector(const Locale& locale, UErrorCode& status) const;
    };

    class TextSelector : public Selector {
    public:
        void selectKey(FormattingContext& context, const UnicodeString* keys/*[]*/, int32_t numKeys, UnicodeString* prefs/*[]*/, int32_t& numMatching, UErrorCode& status) const;
        
    private:
        friend class TextFactory;

        // Formatting `value` to a string might require the locale 
        const Locale& locale;

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
