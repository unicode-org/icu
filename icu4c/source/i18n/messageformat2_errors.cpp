// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#if !UCONFIG_NO_MF2

#include "unicode/messageformat2_errors.h"
#include "messageformat2_allocation.h"
#include "messageformat2_macros.h"
#include "uvector.h" // U_ASSERT

U_NAMESPACE_BEGIN

namespace message2 {

    // Errors
    // -----------

    void DynamicErrors::Builder::setReservedError(UErrorCode& status) {
        addError(DynamicError(DynamicErrorType::ReservedError), status);
    }

    void DynamicErrors::Builder::setFormattingError(const FunctionName& formatterName, UErrorCode& status) {
        addError(DynamicError(DynamicErrorType::FormattingError, formatterName), status);
    }

    void DynamicErrors::Builder::setFormattingError(UErrorCode& status) {
        addError(DynamicError(DynamicErrorType::FormattingError, UnicodeString("unknown formatter")), status);
    }

    void DynamicErrors::Builder::setOperandMismatchError(const FunctionName& formatterName, UErrorCode& status) {
        addError(DynamicError(DynamicErrorType::OperandMismatchError, formatterName), status);
    }

    void StaticErrors::Builder::setDuplicateOptionName(UErrorCode& status) {
        addError(StaticError(StaticErrorType::DuplicateOptionName), status);
    }

    void StaticErrors::Builder::setMissingSelectorAnnotation(UErrorCode& status) {
        addError(StaticError(StaticErrorType::MissingSelectorAnnotation), status);
    }

    void DynamicErrors::Builder::setSelectorError(const FunctionName& selectorName, UErrorCode& status) {
        addError(DynamicError(DynamicErrorType::SelectorError, selectorName), status);
    }

    void DynamicErrors::Builder::setUnknownFunction(const FunctionName& functionName, UErrorCode& status) {
        addError(DynamicError(DynamicErrorType::UnknownFunction, functionName), status);
    }

    void DynamicErrors::Builder::setUnresolvedVariable(const VariableName& v, UErrorCode& status) {
        addError(DynamicError(DynamicErrorType::UnresolvedVariable, v), status);
    }

    DynamicErrors::Builder::Builder(const StaticErrors& e, UErrorCode& status) : staticErrors(e) {
        dynamicErrors.adoptInstead(createUVector(status));
    }

    StaticErrors::Builder::Builder(UErrorCode& status) {
        staticErrors.adoptInstead(createUVector(status));
    }

    StaticErrors::Builder::Builder(const StaticErrors::Builder& other) {
        UErrorCode errorCode = U_ZERO_ERROR;
        U_ASSERT(other.staticErrors.isValid());
        staticErrors.adoptInstead(createUVector(errorCode));
        if (U_FAILURE(errorCode)) {
            bogus = true;
            return;
        }
        for (int32_t i = 0; i < other.staticErrors->size(); i++) {
            StaticError* e = static_cast<StaticError*>(other.staticErrors->elementAt(i));
            U_ASSERT(e != nullptr);
            StaticError* copy = new StaticError(*e);
            if (copy == nullptr) {
                bogus = true;
                return;
            }
            staticErrors->adoptElement(copy, errorCode);
            if (U_FAILURE(errorCode)) {
                bogus = true;
                return;
            }
        }
        dataModelError = other.dataModelError;
        missingSelectorAnnotationError = other.missingSelectorAnnotationError;
        syntaxError = other.syntaxError;
    }

    StaticErrors::StaticErrors(const StaticErrors& other) : len(other.len),
    syntaxError(other.syntaxError), dataModelError(other.dataModelError),
    missingSelectorAnnotationError(other.missingSelectorAnnotationError) {
        UErrorCode errorCode = U_ZERO_ERROR;
        U_ASSERT(!other.bogus);

        if (len > 0) {
            U_ASSERT(other.syntaxAndDataModelErrors.isValid());
            syntaxAndDataModelErrors
                .adoptInstead(copyArray<StaticError>(other.syntaxAndDataModelErrors.getAlias(),
                                                     other.len,
                                                     errorCode));
            if (U_FAILURE(errorCode)) {
                bogus = true;
            }
        }
    }

    StaticErrors::Builder& StaticErrors::Builder::operator=(StaticErrors::Builder other) noexcept {
        swap(*this, other);
        return *this;
    }

    bool DynamicErrors::Builder::hasError() const {
        return dynamicErrors->size() > 0;
    }

    DynamicErrors DynamicErrors::Builder::build(UErrorCode& errorCode) const {
        return DynamicErrors(*this, errorCode);
    }

    StaticErrors StaticErrors::Builder::build(UErrorCode& errorCode) const {
        return StaticErrors(*this, errorCode);
    }

    DynamicErrors::DynamicErrors(const DynamicErrors::Builder& b, UErrorCode& errorCode)
        : staticErrors(b.staticErrors),
          dynamicErrorsLen(b.dynamicErrors->size()) {
        resolutionAndFormattingErrors
            .adoptInstead(copyVectorToArray<DynamicError>(*(b.dynamicErrors), errorCode));
    }


    StaticErrors::StaticErrors(const StaticErrors::Builder& b, UErrorCode& errorCode)
        : len(b.staticErrors->size()),
          syntaxError(b.syntaxError),
          dataModelError(b.dataModelError),
          missingSelectorAnnotationError(b.missingSelectorAnnotationError) {
        syntaxAndDataModelErrors
            .adoptInstead(copyVectorToArray<StaticError>(*(b.staticErrors), errorCode));
    }

    /* static */ UErrorCode StaticErrors::toErrorCode(const StaticError& e) {
        switch (e.type) {
        case StaticErrorType::DuplicateDeclarationError: {
            return U_MF_DUPLICATE_DECLARATION_ERROR;
            break;
        }
        case StaticErrorType::DuplicateOptionName: {
            return U_MF_DUPLICATE_OPTION_NAME_ERROR;
            break;
        }
        case StaticErrorType::VariantKeyMismatchError: {
            return U_MF_VARIANT_KEY_MISMATCH_ERROR;
            break;
        }
        case StaticErrorType::NonexhaustivePattern: {
            return U_MF_NONEXHAUSTIVE_PATTERN_ERROR;
            break;
        }
        case StaticErrorType::MissingSelectorAnnotation: {
            return U_MF_MISSING_SELECTOR_ANNOTATION_ERROR;
            break;
        }
        case StaticErrorType::SyntaxError: {
            return U_MF_SYNTAX_ERROR;
            break;
        }
        case StaticErrorType::UnsupportedStatementError: {
            return U_MF_UNSUPPORTED_STATEMENT_ERROR;
        }
        }
    }

/* static */ UErrorCode DynamicErrors::toErrorCode(const DynamicError& e) {
        switch (e.type) {
        case DynamicErrorType::UnknownFunction: {
            return U_MF_UNKNOWN_FUNCTION_ERROR;
            break;
        }
        case DynamicErrorType::UnresolvedVariable: {
            return U_MF_UNRESOLVED_VARIABLE_ERROR;
            break;
        }
        case DynamicErrorType::FormattingError: {
            return U_MF_FORMATTING_ERROR;
            break;
        }
        case DynamicErrorType::OperandMismatchError: {
            return U_MF_OPERAND_MISMATCH_ERROR;
            break;
        }
        case DynamicErrorType::ReservedError: {
            return U_MF_UNSUPPORTED_EXPRESSION_ERROR;
            break;
        }
        case DynamicErrorType::SelectorError: {
            return U_MF_SELECTOR_ERROR;
            break;
        }
        }
    }

/*
    void DynamicErrors::checkErrors(UErrorCode& status) const {
        if (status != U_ZERO_ERROR) {
            return;
        }

        // Just handle the first error
        // TODO: Eventually want to return all errors to caller
        if (count() == 0) {
            return;
        }
        if (staticErrors.syntaxAndDataModelErrors->size() > 0) {
            switch (staticErrors.first().type) {
            case StaticErrorType::DuplicateDeclarationError: {
                status = U_MF_DUPLICATE_DECLARATION_ERROR;
                break;
            }
            case StaticErrorType::DuplicateOptionName: {
                status = U_MF_DUPLICATE_OPTION_NAME_ERROR;
                break;
            }
            case StaticErrorType::VariantKeyMismatchError: {
                status = U_MF_VARIANT_KEY_MISMATCH_ERROR;
                break;
            }
            case StaticErrorType::NonexhaustivePattern: {
                status = U_MF_NONEXHAUSTIVE_PATTERN_ERROR;
                break;
            }
            case StaticErrorType::MissingSelectorAnnotation: {
                status = U_MF_MISSING_SELECTOR_ANNOTATION_ERROR;
                break;
            }
            case StaticErrorType::SyntaxError: {
                status = U_MF_SYNTAX_ERROR;
                break;
            }
            case StaticErrorType::UnsupportedStatementError: {
                status = U_MF_UNSUPPORTED_STATEMENT_ERROR;
            }
            }
        } else {
            U_ASSERT(resolutionAndFormattingErrors->size() > 0);
            switch (first().type) {
            case DynamicErrorType::UnknownFunction: {
                status = U_MF_UNKNOWN_FUNCTION_ERROR;
                break;
            }
            case DynamicErrorType::UnresolvedVariable: {
                status = U_MF_UNRESOLVED_VARIABLE_ERROR;
                break;
            }
            case DynamicErrorType::FormattingError: {
                status = U_MF_FORMATTING_ERROR;
                break;
            }
            case DynamicErrorType::OperandMismatchError: {
                status = U_MF_OPERAND_MISMATCH_ERROR;
                break;
            }
            case DynamicErrorType::ReservedError: {
                status = U_MF_UNSUPPORTED_EXPRESSION_ERROR;
                break;
            }
            case DynamicErrorType::SelectorError: {
                status = U_MF_SELECTOR_ERROR;
                break;
            }
            }
        }
    }
*/

    void StaticErrors::Builder::addSyntaxError(UErrorCode& status) {
        addError(StaticError(StaticErrorType::SyntaxError), status);
    }

    void StaticErrors::Builder::addError(StaticError&& e, UErrorCode& status) {
        CHECK_ERROR(status);

        U_ASSERT(!bogus);

        void* errorP = static_cast<void*>(create<StaticError>(std::move(e), status));
        U_ASSERT(staticErrors.isValid());

        switch (e.type) {
        case StaticErrorType::SyntaxError: {
            syntaxError = true;
            break;
        }
        case StaticErrorType::DuplicateDeclarationError: {
            dataModelError = true;
            break;
        }
        case StaticErrorType::DuplicateOptionName: {
            dataModelError = true;
            break;
        }
        case StaticErrorType::VariantKeyMismatchError: {
            dataModelError = true;
            break;
        }
        case StaticErrorType::NonexhaustivePattern: {
            dataModelError = true;
            break;
        }
        case StaticErrorType::MissingSelectorAnnotation: {
            missingSelectorAnnotationError = true;
            dataModelError = true;
            break;
        }
        case StaticErrorType::UnsupportedStatementError: {
            dataModelError = true;
            break;
        }
        }
        staticErrors->adoptElement(errorP, status);
    }

    void DynamicErrors::Builder::addError(DynamicError&& e, UErrorCode& status) {
        CHECK_ERROR(status);

        void* errorP = static_cast<void*>(create<DynamicError>(std::move(e), status));
        U_ASSERT(dynamicErrors.isValid());

        switch (e.type) {
        case DynamicErrorType::UnresolvedVariable: {
            unresolvedVariableError = true;
            dynamicErrors->adoptElement(errorP, status);
            break;
        }
        case DynamicErrorType::FormattingError: {
            formattingError = true;
            dynamicErrors->adoptElement(errorP, status);
            break;
        }
        case DynamicErrorType::OperandMismatchError: {
            formattingError = true;
            dynamicErrors->adoptElement(errorP, status);
            break;
        }
        case DynamicErrorType::ReservedError: {
            dynamicErrors->adoptElement(errorP, status);
            break;
        }
        case DynamicErrorType::SelectorError: {
            selectorError = true;
            dynamicErrors->adoptElement(errorP, status);
            break;
        }
        case DynamicErrorType::UnknownFunction: {
            unknownFunctionError = true;
            dynamicErrors->adoptElement(errorP, status);
            break;
        }
        }
    }

/*
    const StaticError& StaticErrors::first() const {
        U_ASSERT(syntaxAndDataModelErrors.isValid() && syntaxAndDataModelErrors->size() > 0);
        return *static_cast<StaticError*>(syntaxAndDataModelErrors->elementAt(0));
    }
*/

    StaticErrors::~StaticErrors() {}
    DynamicErrors::~DynamicErrors() {}

    template<typename ErrorType>
    Error<ErrorType>::~Error() {}

    template<>
    Error<StaticErrorType>::~Error() {}
    template<>
    Error<DynamicErrorType>::~Error() {}

} // namespace message2

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_MF2 */

#endif /* #if !UCONFIG_NO_FORMATTING */
