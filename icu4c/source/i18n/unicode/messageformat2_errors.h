// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#ifndef MESSAGEFORMAT2_ERRORS_H
#define MESSAGEFORMAT2_ERRORS_H

#if U_SHOW_CPLUSPLUS_API

#if !UCONFIG_NO_FORMATTING

#if !UCONFIG_NO_MF2

#include "unicode/messageformat2_data_model_names.h"
#include "unicode/unistr.h"

#include "uvector.h"

#ifndef U_HIDE_DEPRECATED_API

#include <vector>

U_NAMESPACE_BEGIN

// Internal only; included in unicode/ to make it possible to write header-only
// code that constructs a `std::vector` from a DynamicErrors

namespace message2 {

    using namespace data_model;

    // Errors
    // ----------

    class DynamicErrors;
    class StaticErrors;

    // Internal class -- used as a private field in MessageFormatter
    template <typename ErrorType>
    class Error : public UObject {
    public:
        Error(ErrorType ty) : type(ty) {}
        Error(ErrorType ty, const UnicodeString& s) : type(ty), contents(s) {}
        virtual ~Error();

        Error(const Error& other) : type(other.type), contents(other.contents) {}
        Error() {}
    private:
        friend class DynamicErrors;
        friend class StaticErrors;

        ErrorType type;
        UnicodeString contents;
    }; // class Error

    enum StaticErrorType {
        DuplicateDeclarationError,
        DuplicateOptionName,
        MissingSelectorAnnotation,
        NonexhaustivePattern,
        SyntaxError,
        UnsupportedStatementError,
        VariantKeyMismatchError
    };

    enum DynamicErrorType {
        UnresolvedVariable,
        FormattingError,
        OperandMismatchError,
        ReservedError,
        SelectorError,
        UnknownFunction,
    };

    using StaticError = Error<StaticErrorType>;
    using DynamicError = Error<DynamicErrorType>;

    // These explicit instantiations have to come before the
    // destructor definitions
    template<>
    Error<StaticErrorType>::~Error();
    template<>
    Error<DynamicErrorType>::~Error();

// StaticErrors and DynamicErrors have separate Builder classes,
// for one reason: it's convenient to represent errors while
// they're still being added to as a UVector, but for the header-only
// code that translates these errors to a std::vector, it's easier
// for the errors to be stored in a LocalArray.
// So we use a builder based on a UVector, and build() turns that
// into a LocalArray, which can then be handled by
// the code in formatToString() in messageformat2.h that calls
// MessageContext::errorsToVector().
    class StaticErrors : public UObject {

    public:
        class Builder {
            public:
            void setMissingSelectorAnnotation(UErrorCode&);
            void setDuplicateOptionName(UErrorCode&);
            void addSyntaxError(UErrorCode&);
            void addError(StaticError&&, UErrorCode&);
            bool hasSyntaxError() const { return syntaxError; }
            Builder(UErrorCode&);
            StaticErrors build(UErrorCode& errorCode) const;

            Builder& operator=(Builder) noexcept;
            friend inline void swap(Builder& b1, Builder& b2) noexcept {
                using std::swap;

                swap(b1.staticErrors, b2.staticErrors);
                swap(b1.syntaxError, b2.syntaxError);
                swap(b1.dataModelError, b2.dataModelError);
                swap(b1.missingSelectorAnnotationError, b2.missingSelectorAnnotationError);
                swap(b1.bogus, b2.bogus);
            }
            Builder(const Builder&);

            private:
            friend class StaticErrors;
            LocalPointer<UVector> staticErrors;
            bool syntaxError = false;
            bool dataModelError = false;
            bool missingSelectorAnnotationError = false;
            bool bogus = false; // Set if a copy fails
        };

        StaticErrors(const StaticErrors::Builder&, UErrorCode&);

        bool hasSyntaxError() const { return syntaxError; }
        bool hasDataModelError() const { return dataModelError; }
        bool hasMissingSelectorAnnotationError() const { return missingSelectorAnnotationError; }

        StaticErrors(const StaticErrors&);
        virtual ~StaticErrors();

        StaticErrors& operator=(StaticErrors) noexcept;
        friend inline void swap(StaticErrors& e1, StaticErrors& e2) noexcept {
            using std::swap;

            swap(e1.syntaxAndDataModelErrors, e2.syntaxAndDataModelErrors);
            swap(e1.len, e2.len);
            swap(e1.missingSelectorAnnotationError, e2.missingSelectorAnnotationError);
            swap(e1.bogus, e2.bogus);
        }

    private:
        friend class DynamicErrors;
        friend class Builder;

        LocalArray<StaticError> syntaxAndDataModelErrors;
        int32_t len;
        bool syntaxError = false;
        bool dataModelError = false;
        bool missingSelectorAnnotationError = false;
        bool bogus = false; // Used in case of copy constructor failure
        static UErrorCode toErrorCode(const StaticError&);

    }; // class StaticErrors

    class DynamicErrors : public UObject {
    private:
        StaticErrors staticErrors;
        LocalArray<DynamicError> resolutionAndFormattingErrors;
        int32_t dynamicErrorsLen;
        static UErrorCode toErrorCode(const DynamicError&);

    public:
        class Builder {
            private:
            friend class DynamicErrors;

            LocalPointer<UVector> dynamicErrors;
            StaticErrors staticErrors;
            bool formattingError = false;
            bool selectorError = false;
            bool unresolvedVariableError = false;
            bool unknownFunctionError = false;
            public:
            void setSelectorError(const FunctionName&, UErrorCode&);
            void setReservedError(UErrorCode&);
            void setUnresolvedVariable(const VariableName&, UErrorCode&);
            void setUnknownFunction(const FunctionName&, UErrorCode&);
            void setFormattingError(const FunctionName&, UErrorCode&);
            // Used when the name of the offending formatter is unknown
            void setFormattingError(UErrorCode&);
            void setOperandMismatchError(const FunctionName&, UErrorCode&);
            bool hasFormattingError() const { return formattingError; }
            bool hasUnresolvedVariableError() const { return unresolvedVariableError; }
            void addError(DynamicError&&, UErrorCode&);
            bool hasError() const;
            bool hasDataModelError() const { return staticErrors.hasDataModelError(); }
            bool hasSyntaxError() const { return staticErrors.hasSyntaxError(); }
            bool hasUnknownFunctionError() const { return unknownFunctionError; }
            bool hasMissingSelectorAnnotationError() const { return staticErrors.hasMissingSelectorAnnotationError(); }
            bool hasSelectorError() const { return selectorError; }

            Builder(const StaticErrors&, UErrorCode&);
            DynamicErrors build(UErrorCode&) const;
        };

        DynamicErrors(StaticErrors&&, UErrorCode&);

        void errorsToVector(std::vector<UErrorCode>& output) const {
            for (int32_t i = 0; i < staticErrors.len; i++) {
                output.push_back(StaticErrors::toErrorCode(staticErrors.syntaxAndDataModelErrors[i]));
            }
            for (int32_t i = 0; i < dynamicErrorsLen; i++) {
                output.push_back(DynamicErrors::toErrorCode(resolutionAndFormattingErrors[i]));
            }
        }

        virtual ~DynamicErrors();

        DynamicErrors& operator=(DynamicErrors&&) = delete;
        DynamicErrors& operator=(const DynamicErrors&) = delete;
        DynamicErrors(const DynamicErrors&) = delete;
        DynamicErrors(DynamicErrors&&) = delete;

        private:
        friend class Builder;
        DynamicErrors(const DynamicErrors::Builder&, UErrorCode&);
    }; // class DynamicErrors

} // namespace message2

U_NAMESPACE_END

#endif // U_HIDE_DEPRECATED_API

#endif /* #if !UCONFIG_NO_MF2 */

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif /* U_SHOW_CPLUSPLUS_API */

#endif // MESSAGEFORMAT_DATA_MODEL_H
// eof
