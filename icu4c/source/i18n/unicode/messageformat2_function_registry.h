// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#ifndef MESSAGEFORMAT2_FUNCTION_REGISTRY_H
#define MESSAGEFORMAT2_FUNCTION_REGISTRY_H

#if U_SHOW_CPLUSPLUS_API

#if !UCONFIG_NO_NORMALIZATION

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

    class Function;

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
         * Looks up a function by the name of the function. The result is non-const,
         * since functions may have local state. Returns the result by pointer
         * rather than by reference since it can fail.
         *
         * @param functionName Name of the desired function.
         * @return A pointer to the Function registered under `functionName`, or null
         *         if no function was registered under that name. The pointer is not owned
         *         by the caller.
         *
         * @internal ICU 75 technology preview
         * @deprecated This API is for technology preview only.
         */
        Function* getFunction(const FunctionName& functionName) const;
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
            Builder& adoptFunction(const data_model::FunctionName& functionName,
                                   Function* function,
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

        MFFunctionRegistry(FunctionMap* f);

        MFFunctionRegistry() {}

        // Debugging; should only be called on a function registry with
        // all the standard functions registered
        void checkFunction(const char*) const;
        void checkStandard() const;

        bool hasFunction(const data_model::FunctionName& f) const;
        void cleanup() noexcept;

        // Must use raw pointers to avoid instantiating `LocalPointer` on an internal type
        FunctionMap* functions = nullptr;
    }; // class MFFunctionRegistry

    class FunctionValue;

    /**
     * Interface that function handler classes must implement.
     *
     * @internal ICU 75 technology preview
     * @deprecated This API is for technology preview only.
     */
    class U_I18N_API Function : public UObject {
    public:
        virtual FunctionValue* call(FunctionValue&, FunctionOptions&&, UErrorCode&) = 0;
        virtual ~Function();
    }; // class Function

    class U_I18N_API FunctionValue : public UObject {
        public:
            virtual UnicodeString formatToString(UErrorCode& status) const {
                if (U_SUCCESS(status)) {
                    status = U_MF_FORMATTING_ERROR;
                }
                return {};
            }
            virtual const Formattable& getOperand() const { return operand; }
            // `this` can't be used after calling this method
            virtual FunctionOptions getResolvedOptions() { return std::move(opts); }
            // const method is for reading the options attached to another option
            // (i.e. options don't escape) --
            // non-const method is for calling mergeOptions() -- i.e. options escape
            virtual const FunctionOptions& getResolvedOptions() const { return opts; }
            virtual UBool isSelectable() const { return false; }
            virtual UBool isNullOperand() const { return false; }
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
            virtual ~FunctionValue();
         protected:
            Formattable operand;
            FunctionOptions opts;
    }; // class FunctionValue

    class NullValue : public FunctionValue {
        public:
            virtual UBool isNullOperand() const { return true; }
    }; // class NullValue

} // namespace message2

U_NAMESPACE_END

#endif // U_HIDE_DEPRECATED_API

#endif /* #if !UCONFIG_NO_MF2 */

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif /* #if !UCONFIG_NO_NORMALIZATION */

#endif /* U_SHOW_CPLUSPLUS_API */

#endif // MESSAGEFORMAT2_FUNCTION_REGISTRY_H

// eof
