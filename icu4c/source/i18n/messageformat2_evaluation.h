// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#ifndef U_HIDE_DEPRECATED_API

#ifndef MESSAGEFORMAT2_EVALUATION_H
#define MESSAGEFORMAT2_EVALUATION_H

#if U_SHOW_CPLUSPLUS_API

/**
 * \file
 * \brief C++ API: Formats messages using the draft MessageFormat 2.0.
 */
#if !UCONFIG_NO_NORMALIZATION

#if !UCONFIG_NO_FORMATTING

#if !UCONFIG_NO_MF2

#include "unicode/messageformat2_arguments.h"
#include "unicode/messageformat2_data_model.h"
#include "unicode/messageformat2_function_registry.h"
#include "messageformat2_errors.h"

// Auxiliary data structures used during formatting a message

U_NAMESPACE_BEGIN

namespace message2 {

    namespace functions {
    static constexpr std::u16string_view DATETIME = u"datetime";
    static constexpr std::u16string_view DATE = u"date";
    static constexpr std::u16string_view TIME = u"time";
    static constexpr std::u16string_view NUMBER = u"number";
    static constexpr std::u16string_view INTEGER = u"integer";
    static constexpr std::u16string_view TEST_FUNCTION = u"test:function";
    static constexpr std::u16string_view TEST_FORMAT = u"test:format";
    static constexpr std::u16string_view TEST_SELECT = u"test:select";
    static constexpr std::u16string_view STRING = u"string";
    }

    using namespace data_model;

    // InternalValue tracks a value along with, possibly, a function that needs
    // to be applied to it in the future (once the value is required
    // (by a .match or pattern));
    // while FormattedPlaceholder tracks a value and how it was constructed in the
    // past (by a function, or from a literal or argument).

    // InternalValue represents an intermediate value in the message
    // formatter. An InternalValue can either be a fallback value (representing
    // an error that occurred during formatting); a "suspension", meaning a function
    // call that has yet to be fully resolved; or a fully-resolved FormattedPlaceholder.
    // The "suspension" state is used in implementing selection; in a message like:
    // .local $x = {1 :number}
    // .match $x
    // [...]
    // $x can't be bound to a fully formatted value; the annotation needs to be
    // preserved until the .match is evaluated. Moreover, any given function could
    // be both a formatter and a selector, and it's ambiguous which one it's intended
    // to be until the body of the message is processed.
    class InternalValue : public UObject {
    public:
        bool isFallback() const { return !fallbackString.isEmpty(); }
        bool isSuspension() const { return !functionName.isEmpty(); }
        InternalValue() : fallbackString("") {}
        // Fallback constructor
        explicit InternalValue(UnicodeString fb) : fallbackString(fb) {}
        // Fully-evaluated value constructor
        explicit InternalValue(FormattedPlaceholder&& f)
            : fallbackString(""), functionName(""), resolvedOptions(nullptr),
              operand(std::move(f)) {}
        // Suspension constructor
        InternalValue(const FunctionName& name,
                      FunctionOptions&& options,
                      FormattedPlaceholder&& rand,
                      UErrorCode& status);
        // Error code is set if this isn't fully evaluated
        FormattedPlaceholder takeValue(UErrorCode& status);
        // Error code is set if this is not a suspension
        FormattedPlaceholder takeOperand(UErrorCode& status);
        // Error code is set if this is not a suspension
        FunctionOptions takeOptions(UErrorCode& status);
        // Error code is set if this is not a suspension
        FunctionName getFunctionName(UErrorCode& status) const;
        UnicodeString asFallback() const { return fallbackString; }
        virtual ~InternalValue();
        InternalValue& operator=(InternalValue&&);
        InternalValue(InternalValue&&);
    private:
        UnicodeString fallbackString; // Non-empty if fallback
        FunctionName functionName; // Non-empty if this is a suspension
        LocalPointer<FunctionOptions> resolvedOptions; // Valid iff this is a suspension
        FormattedPlaceholder operand;
    }; // class InternalValue

    // PrioritizedVariant

    // For how this class is used, see the references to (integer, variant) tuples
    // in https://github.com/unicode-org/message-format-wg/blob/main/spec/formatting.md#pattern-selection
    class PrioritizedVariant : public UObject {
    public:
        PrioritizedVariant() = default;
        PrioritizedVariant(PrioritizedVariant&&) = default;
        PrioritizedVariant& operator=(PrioritizedVariant&&) noexcept = default;
        UBool operator<(const PrioritizedVariant&) const;
        int32_t priority;
        /* const */ SelectorKeys keys;
        /* const */ Pattern pat;
        PrioritizedVariant(uint32_t p,
                           const SelectorKeys& k,
                           const Pattern& pattern) noexcept : priority(p), keys(k), pat(pattern) {}
        virtual ~PrioritizedVariant();
    }; // class PrioritizedVariant

    static inline int32_t comparePrioritizedVariants(UElement left, UElement right) {
        const PrioritizedVariant& tuple1 = *(static_cast<const PrioritizedVariant*>(left.pointer));
        const PrioritizedVariant& tuple2 = *(static_cast<const PrioritizedVariant*>(right.pointer));
        if (tuple1 < tuple2) {
            return -1;
        }
        if (tuple1.priority == tuple2.priority) {
            return 0;
        }
        return 1;
    }

    // Closures and environments
    // -------------------------

    class Environment;

    // A closure represents the right-hand side of a variable
    // declaration, along with an environment giving values
    // to its free variables
    class Closure : public UMemory {
    public:
        const Expression& getExpr() const {
            return expr;
        }
        const Environment& getEnv() const {
            return env;
        }
        Closure(const Expression& expression, const Environment& environment) : expr(expression), env(environment) {}
        Closure(Closure&&) = default;

        virtual ~Closure();
    private:

        // An unevaluated expression
        const Expression& expr;
        // The environment mapping names used in this
        // expression to other expressions
        const Environment& env;
    };

    // An environment is represented as a linked chain of
    // non-empty environments, terminating at an empty environment.
    // It's searched using linear search.
    class Environment : public UMemory {
    public:
        virtual bool has(const VariableName&) const = 0;
        virtual const Closure& lookup(const VariableName&) const = 0;
        static Environment* create(UErrorCode&);
        static Environment* create(const VariableName&, Closure&&, Environment*, UErrorCode&);
        virtual ~Environment();
    };

    class NonEmptyEnvironment;
    class EmptyEnvironment : public Environment {
    public:
        EmptyEnvironment() = default;
        virtual ~EmptyEnvironment();

    private:
        friend class Environment;

        bool has(const VariableName&) const override;
        const Closure& lookup(const VariableName&) const override;
        static EmptyEnvironment* create(UErrorCode&);
        static NonEmptyEnvironment* create(const VariableName&, Closure&&, Environment*, UErrorCode&);
    };

    class NonEmptyEnvironment : public Environment {
    private:
        friend class Environment;

        bool has(const VariableName&) const override;
        const Closure& lookup(const VariableName&) const override;
        static NonEmptyEnvironment* create(const VariableName&, Closure&&, const Environment*, UErrorCode&);
        virtual ~NonEmptyEnvironment();
    private:
        friend class Environment;

        NonEmptyEnvironment(const VariableName& v, Closure&& c, Environment* e) : var(v), rhs(std::move(c)), parent(e) {}

        // Maps VariableName onto Closure*
        // Chain of linked environments
        VariableName var;
        Closure rhs;
        const LocalPointer<Environment> parent;
    };

    // The context contains all the information needed to process
    // an entire message: arguments, formatter cache, and error list

    class MessageFormatter;

    class MessageContext : public UMemory {
    public:
        MessageContext(const MessageArguments&, const StaticErrors&, UErrorCode&);

        const Formattable* getGlobal(const VariableName&, UErrorCode&) const;

        // If any errors were set, update `status` accordingly
        void checkErrors(UErrorCode& status) const;
        DynamicErrors& getErrors() { return errors; }

        virtual ~MessageContext();

    private:

        const MessageArguments& arguments; // External message arguments
        // Errors accumulated during parsing/formatting
        DynamicErrors errors;

    }; // class MessageContext

    // InternalValue
    // ----------------

    class InternalValue : public UObject {
    public:
        const FunctionName& getFunctionName() const { return name; }
        bool canSelect() const { return selector != nullptr; }
        const Selector* getSelector(UErrorCode&) const;
        FormattedPlaceholder forceFormatting(DynamicErrors& errs,
                                             UErrorCode& errorCode);
        void forceSelection(DynamicErrors& errs,
                            const UnicodeString* keys,
                            int32_t keysLen,
                            UnicodeString* prefs,
                            int32_t& prefsLen,
                            UErrorCode& errorCode);
        // Needs to be deep-copyable and movable
        virtual ~InternalValue();
        InternalValue(FormattedPlaceholder&&);
        // Formatter and selector may be null
        InternalValue(InternalValue*, FunctionOptions&&, const FunctionName&, const Formatter*,
                      const Selector*);
        const UnicodeString& getFallback() const;
        bool isFallback() const;
        bool hasNullOperand() const;
        // Can't be used anymore after calling this
        FormattedPlaceholder takeArgument(UErrorCode& errorCode);
        InternalValue(InternalValue&& other) { *this = std::move(other); }
        InternalValue& operator=(InternalValue&& other) noexcept;
    private:
        // InternalValue is owned (if present)
        std::variant<InternalValue*, FormattedPlaceholder> argument;
        FunctionOptions options;
        FunctionName name;
        const Selector* selector; // May be null
        const Formatter* formatter; // May be null, but one or the other should be non-null unless argument is a FormattedPlaceholder
        bool checkSelectOption() const;
    }; // class InternalValue

} // namespace message2

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_MF2 */

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif /* #if !UCONFIG_NO_NORMALIZATION */

#endif /* U_SHOW_CPLUSPLUS_API */

#endif // MESSAGEFORMAT2_EVALUATION_H

#endif // U_HIDE_DEPRECATED_API
// eof
