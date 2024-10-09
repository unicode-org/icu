// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#ifndef MESSAGEFORMAT2_FUNCTION_REGISTRY_H
#define MESSAGEFORMAT2_FUNCTION_REGISTRY_H

#if U_SHOW_CPLUSPLUS_API

#if !UCONFIG_NO_FORMATTING

#if !UCONFIG_NO_MF2

#include "unicode/messageformat2_data_model_names.h"
#include "unicode/messageformat2_formattable.h"

#ifndef U_HIDE_DEPRECATED_API

#include <map>

U_NAMESPACE_BEGIN

class Hashtable;
class UVector;

namespace message2 {

    using namespace data_model;

    class FunctionFactory;

    /**
     * Defines mappings from names of formatters and selectors to functions implementing them.
     * The required set of formatter and selector functions is defined in the spec. Users can
     * also define custom formatter and selector functions.
     *
     * `MFFunctionRegistry` is immutable and movable. It is not copyable.
     *
     * @internal ICU 75 technology preview
     * @deprecated This API is for technology preview only.
     */
    class U_I18N_API MFFunctionRegistry : public UObject {
    private:

        using FunctionMap = Hashtable; // Map from function names to FunctionFactory*

    public:
        /**
         * Looks up a function factory by the name of the function. The result is non-const,
         * since functions may have local state. Returns the result by pointer
         * rather than by reference since it can fail.
         *
         * @param functionName Name of the desired function.
         * @return A pointer to the function factory registered under `functionName`, or null
         *         if no function was registered under that name. The pointer is not owned
         *         by the caller.
         *
         * @internal ICU 75 technology preview
         * @deprecated This API is for technology preview only.
         */
        FunctionFactory* getFunction(const FunctionName& functionName) const;
        /**
         * Looks up a function by a type tag. This method gets the name of the default formatter registered
         * for that type. If no formatter was explicitly registered for this type, it returns false.
         *
         * @param formatterType Type tag for the desired `FormattableObject` type to be formatted.
         * @param name Output parameter; initialized to the name of the default formatter for `formatterType`
         *        if one has been registered. Its value is undefined otherwise.
         * @return True if and only if the function registry contains a default formatter for `formatterType`.
         *         If the return value is false, then the value of `name` is undefined.
         *
         * @internal ICU 75 technology preview
         * @deprecated This API is for technology preview only.
         */
        UBool getDefaultFormatterNameByType(const UnicodeString& formatterType, FunctionName& name) const;
        /**
         * The mutable Builder class allows each formatter and selector factory
         * to be initialized separately; calling its `build()` method yields an
         * immutable MFFunctionRegistry object.
         *
         * Builder is not copyable or movable.
         *
         * @internal ICU 75 technology preview
         * @deprecated This API is for technology preview only.
         */
        class U_I18N_API Builder : public UObject {
        private:
            // Must use raw pointers to avoid instantiating `LocalPointer` on an internal type
            FunctionMap* functions;
            // Mapping from strings (type tags) to FunctionNames
            Hashtable* formattersByType = nullptr;

            // Do not define copy constructor/assignment operator
            Builder& operator=(const Builder&) = delete;
            Builder(const Builder&) = delete;

        public:
            /*
              Notes about `adoptFormatter()`'s type signature:

              Alternative considered: take a non-owned FormatterFactory*
              This is unsafe.

              Alternative considered: take a FormatterFactory&
                 This requires getFormatter() to cast the reference to a pointer,
                 as it must return an unowned FormatterFactory* since it can fail.
                 That is also unsafe, since the caller could delete the pointer.

              The "TemperatureFormatter" test from the previous ICU4J version doesn't work now,
              as it only works if the `formatterFactory` argument is non-owned.
              If registering a non-owned FormatterFactory is desirable, this could
              be re-thought.
              */
            /**
             * Registers a function to a given name.
             *
             * @param functionName Name of the formatter being registered.
             * @param function A pointer to a Function object.
             *                 This argument is adopted.
             * @param errorCode Input/output error code
             * @return A reference to the builder.
             *
             * @internal ICU 77 technology preview
             * @deprecated This API is for technology preview only.
             */
            Builder& adoptFunctionFactory(const data_model::FunctionName& functionName,
                                          FunctionFactory* function,
                                          UErrorCode& errorCode);
            /**
             * Registers a formatter factory to a given type tag.
             * (See `FormattableObject` for details on type tags.)
             *
             * @param type Tag for objects to be formatted with this formatter.
             * @param functionName A reference to the name of the function to use for
             *        creating formatters for `formatterType` objects.
             * @param errorCode Input/output error code
             * @return A reference to the builder.
             *
             * @internal ICU 75 technology preview
             * @deprecated This API is for technology preview only.
             */
            Builder& setDefaultFormatterNameByType(const UnicodeString& type,
                                                   const data_model::FunctionName& functionName,
                                                   UErrorCode& errorCode);
            /**
             * Creates an immutable `MFFunctionRegistry` object with the selectors and formatters
             * that were previously registered. The builder cannot be used after this call.
             * The `build()` method is destructive to avoid the need for a deep copy of the
             * `FormatterFactory` and `SelectorFactory` objects (this would be necessary because
             * `FormatterFactory` can have mutable state), which in turn would require implementors
             * of those interfaces to implement a `clone()` method.
             *
             * @return The new MFFunctionRegistry
             *
             * @internal ICU 75 technology preview
             * @deprecated This API is for technology preview only.
             */
            MFFunctionRegistry build();
            /**
             * Default constructor.
             * Returns a Builder with no functions registered.
             *
             * @param errorCode Input/output error code
             *
             * @internal ICU 75 technology preview
             * @deprecated This API is for technology preview only.
             */
            Builder(UErrorCode& errorCode);
            /**
             * Destructor.
             *
             * @internal ICU 75 technology preview
             * @deprecated This API is for technology preview only.
             */
            virtual ~Builder();
        }; // class MFFunctionRegistry::Builder

        /**
         * Move assignment operator:
         * The source MFFunctionRegistry will be left in a valid but undefined state.
         *
         * @internal ICU 75 technology preview
         * @deprecated This API is for technology preview only.
         */
        MFFunctionRegistry& operator=(MFFunctionRegistry&&) noexcept;
        /**
         * Move constructor:
         * The source MFFunctionRegistry will be left in a valid but undefined state.
         *
         * @internal ICU 75 technology preview
         * @deprecated This API is for technology preview only.
         */
        MFFunctionRegistry(MFFunctionRegistry&& other) { *this = std::move(other); }
        /**
         * Destructor.
         *
         * @internal ICU 75 technology preview
         * @deprecated This API is for technology preview only.
         */
        virtual ~MFFunctionRegistry();

    private:
        friend class MessageContext;
        friend class MessageFormatter;

        // Do not define copy constructor or copy assignment operator
        MFFunctionRegistry& operator=(const MFFunctionRegistry&) = delete;
        MFFunctionRegistry(const MFFunctionRegistry&) = delete;

        MFFunctionRegistry(FunctionMap*, Hashtable*);

        MFFunctionRegistry() {}

        // Debugging; should only be called on a function registry with
        // all the standard functions registered
        void checkFunction(const char*) const;
        void checkStandard() const;

        bool hasFunction(const data_model::FunctionName& f) const;
        void cleanup() noexcept;

        // Must use raw pointers to avoid instantiating `LocalPointer` on an internal type
        FunctionMap* functions = nullptr;
        // Mapping from strings (type tags) to FunctionNames
        Hashtable* formattersByType = nullptr;
    }; // class MFFunctionRegistry

    class Function;

    /**
     * Interface that function factory classes must implement.
     *
     * @internal ICU 77 technology preview
     * @deprecated This API is for technology preview only.
     */
    class U_I18N_API FunctionFactory : public UObject {
     public:
         /**
          * Constructs a new function object. This method is not const;
          * function factories with local state may be defined.
          *
          * @param locale Locale to be used by the function.
          * @param status    Input/output error code.
          * @return The new Formatter, which is non-null if U_SUCCESS(status).
          *
          * @internal ICU 75 technology preview
          * @deprecated This API is for technology preview only.
          */
         virtual Function* createFunction(const Locale& locale, UErrorCode& status) = 0;
        /**
          * Destructor.
          *
          * @internal ICU 75 technology preview
          * @deprecated This API is for technology preview only.
          */
         virtual ~FunctionFactory();
        /**
         * Copy constructor.
         *
         * @internal ICU 75 technology preview
         * @deprecated This API is for technology preview only.
         */
        FunctionFactory& operator=(const FunctionFactory&) = delete;
    }; // class FunctionFactory

    class FunctionValue;

    /**
     * Interface that function handler classes must implement.
     *
     * @internal ICU 77 technology preview
     * @deprecated This API is for technology preview only.
     */
    class U_I18N_API Function : public UObject {
        public:
            /**
             * Calls this Function on a FunctionValue operand and its FunctionOptions options,
             * returning a new pointer to a FunctionValue (which is adopted by the caller).
             *
             * @param operand The unnamed argument to the function.
             * @param options Resolved options for this function.
             * @param status Input/output error code
             *
             * @internal ICU 77 technology preview
             * @deprecated This API is for technology preview only.
             */
            virtual FunctionValue* call(FunctionValue& operand,
                                        FunctionOptions&& options,
                                        UErrorCode& status) = 0;
            /**
             * Destructor.
             *
             * @internal ICU 77 technology preview
             * @deprecated This API is for technology preview only.
             */
            virtual ~Function();
    }; // class Function

    /**
     * Type representing argument and return values for custom functions.
     * It encapsulates an operand and resolved options, and can be extended with
     * additional state.
     * Adding a new custom function requires adding a new class that
     * implements this interface.
     *
     * @internal ICU 77 technology preview
     * @deprecated This API is for technology preview only.
     */
    class U_I18N_API FunctionValue : public UObject {
        public:
            /**
             * Returns the string representation of this value. The default
             * method signals an error. Must be overridden by classes
             * implementing values that support formatting.
             *
             * @param status Input/output error code
             * @return A string.
             *
             * @internal ICU 77 technology preview
             * @deprecated This API is for technology preview only.
             */
            virtual UnicodeString formatToString(UErrorCode& status) const {
                if (U_SUCCESS(status)) {
                    status = U_MF_FORMATTING_ERROR;
                }
                return {};
            }
            /**
             * Returns the Formattable operand that was used to construct
             * this value. The operand may be obtained from calling getOperand()
             * on the input FunctionValue, or it may be constructed separately.
             *
             * @return A reference to a message2::Formattable object.
             *
             * @internal ICU 77 technology preview
             * @deprecated This API is for technology preview only.
             */
            virtual const Formattable& getOperand() const { return operand; }
            /**
             * Returns the resolved options that were used to construct this value.
             * `this` may not be used after calling this method. This overload
             * is provided so that mergeOptions(), which passes its `this` argument
             * by move, can be called.
             *
             * @return The resolved options for this value.
             *
             * @internal ICU 77 technology preview
             * @deprecated This API is for technology preview only.
             */
            virtual FunctionOptions getResolvedOptions() { return std::move(opts); }
            /**
             * Returns a reference to the resolved options for this value.
             *
             * @return A reference to the resolved options for this value.
             *
             * @internal ICU 77 technology preview
             * @deprecated This API is for technology preview only.
             */
            virtual const FunctionOptions& getResolvedOptions() const { return opts; }
            /**
             * Returns true if this value supports selection. The default method
             * returns false. The method must be overridden for values that support
             * selection.
             *
             * @return True iff this value supports selection.
             *
             * @internal ICU 77 technology preview
             * @deprecated This API is for technology preview only.
             */
            virtual UBool isSelectable() const { return false; }
            /**
             * Returns true if this value represents a null operand, that is,
             * the absence of an argument. This method should not be overridden.
             * It can be called in order to check whether the argument is present.
             * Some functions may be nullary (they may work with no arguments).
             *
             * @return True iff this value represents an absent operand.
             *
             * @internal ICU 77 technology preview
             * @deprecated This API is for technology preview only.
             */
            virtual UBool isNullOperand() const { return false; }
            /**
             * Compares this value to an array of keys, and returns an array of matching
             * keys sorted by preference. The default implementation of this method
             * signals an error. It should be overridden for value classes that support
             * selection.
             *
             * @param keys An array of strings to compare to the input.
             * @param keysLen The length of `keys`.
             * @param prefs An array of strings with length `keysLen`. The contents of
             *        the array is undefined. `selectKey()` should set the contents
             *        of `prefs` to a subset of `keys`, with the best match placed at the lowest index.
             * @param prefsLen A reference that `selectKey()` should set to the length of `prefs`,
             *        which must be less than or equal to `keysLen`.
             * @param status    Input/output error code.
             *
             * @internal ICU 77 technology preview
             * @deprecated This API is for technology preview only.
             */
            virtual void selectKeys(const UnicodeString* keys,
                                    int32_t keysLen,
                                    UnicodeString* prefs,
                                    int32_t& prefsLen,
                                    UErrorCode& status) {
                (void) keys;
                (void) keysLen;
                (void) prefs;
                (void) prefsLen;
                if (U_SUCCESS(status)) {
                    status = U_MF_SELECTOR_ERROR;
                }
            }
            /**
             * Destructor.
             *
             * @internal ICU 77 technology preview
             * @deprecated This API is for technology preview only.
             */
            virtual ~FunctionValue();
         protected:
            /**
             * Operand used to construct this value.
             *
             * @internal ICU 77 technology preview
             * @deprecated This API is for technology preview only.
             */
            Formattable operand;
            /**
             * Resolved options attached to this value.
             *
             * @internal ICU 77 technology preview
             * @deprecated This API is for technology preview only.
             */
            FunctionOptions opts;
    }; // class FunctionValue

} // namespace message2

U_NAMESPACE_END

#endif // U_HIDE_DEPRECATED_API

#endif /* #if !UCONFIG_NO_MF2 */

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif /* U_SHOW_CPLUSPLUS_API */

#endif // MESSAGEFORMAT2_FUNCTION_REGISTRY_H

// eof
