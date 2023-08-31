// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/messageformat2_context.h"
#include "unicode/messageformat2_macros.h"
#include "unicode/messageformat2.h"
#include "uvector.h" // U_ASSERT

U_NAMESPACE_BEGIN namespace message2 {


// ------------------------------------------------------
// Context

const Formatter* CachedFormatters::getFormatter(const FunctionName& f) {
    U_ASSERT(cache.isValid());
    return ((Formatter*) cache->get(f.toString()));
}

void CachedFormatters::setFormatter(const FunctionName& f, Formatter* val, UErrorCode& errorCode) {
    CHECK_ERROR(errorCode);
    U_ASSERT(cache.isValid());
    cache->put(f.toString(), val, errorCode);
}

CachedFormatters::CachedFormatters(UErrorCode& errorCode) {
    CHECK_ERROR(errorCode);
    cache.adoptInstead(new Hashtable(compareVariableName, nullptr, errorCode));
    CHECK_ERROR(errorCode);
    // The cache owns the values
    cache->setValueDeleter(uprv_deleteUObject);
}

bool Context::hasVar(const VariableName& v) const {
    return arguments.has(v);
} 

const Formattable& Context::getVar(const VariableName& f) const {
    U_ASSERT(hasVar(f));
    return arguments.get(f);
} 

Context::Context(const MessageFormatter& mf, const MessageArguments& args, Errors& e) : parent(mf), arguments(args), errors(e) {}

/* static */ Context* Context::create(const MessageFormatter& mf, const MessageArguments& args, Errors& e, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);

    LocalPointer<Context> result(new Context(mf, args, e));
    NULL_ON_ERROR(errorCode);
    return result.orphan();
}

// Errors
// -----------

void Context::addError(Error e, UErrorCode& status) {
    CHECK_ERROR(status);
    errors.addError(e, status);
}

void Context::checkErrors(UErrorCode& status) const {
    CHECK_ERROR(status);
    errors.checkErrors(status);
}


bool Context::hasDataModelError() const {
    return errors.hasDataModelError();
}

bool Context::hasError() const {
    return errors.count() > 0;
}

bool Context::hasFormattingWarning() const {
    return errors.hasFormattingWarning();
}

void Context::setFormattingWarning(const FunctionName& formatterName, UErrorCode& status) {
    CHECK_ERROR(status);

    Error err(Error::Type::FormattingWarning, formatterName);
    errors.addError(err, status);
 }

void Context::setSelectorError(const FunctionName& selectorName, UErrorCode& status) {
    CHECK_ERROR(status);

    Error err(Error::Type::SelectorError, selectorName);
    errors.addError(err, status);
 }

void Context::setUnknownFunctionWarning(const FunctionName& formatterName, UErrorCode& status) {
    CHECK_ERROR(status);

    Error err(Error::Type::UnknownFunction, formatterName);
    errors.addError(err, status);
 }

void Context::setUnresolvedVariableWarning(const VariableName& v, UErrorCode& status) {
    CHECK_ERROR(status);

    Error err(Error::Type::UnresolvedVariable, v);
    errors.addError(err, status);
 }

bool Context::hasParseError() const {
    return errors.hasSyntaxError();
}

bool Context::hasUnknownFunctionError() const {
    return errors.hasUnknownFunctionError();
}

bool Context::hasMissingSelectorAnnotationError() const {
    return errors.hasMissingSelectorAnnotationError();
}

bool Context::hasSelectorError() const {
    return errors.hasSelectorError();
}

Errors* Errors::create(UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);
    return new Errors(errorCode);
}

int32_t Errors::count() const {
    return syntaxAndDataModelErrors->size() + resolutionAndFormattingErrors->size();
}

void Errors::clearResolutionAndFormattingErrors() {
    U_ASSERT(resolutionAndFormattingErrors.isValid());
    resolutionAndFormattingErrors->removeAllElements();
    formattingWarning = false;
    selectorError = false;    
}

void Errors::checkErrors(UErrorCode& status) {
    if (status != U_ZERO_ERROR) {
        return;
    }

    // Just handle the first error
    // TODO: Eventually want to return all errors to caller
    if (count() == 0) {
        return;
    }
    Error* err;
    if (syntaxAndDataModelErrors->size() > 0) {
        err = (Error*) (*syntaxAndDataModelErrors)[0];
    } else {
        U_ASSERT(resolutionAndFormattingErrors->size() > 0);
        err = (Error*) (*resolutionAndFormattingErrors)[0];
    }
    switch (err->type) {
        case Error::Type::DuplicateOptionName: {
            status = U_DUPLICATE_OPTION_NAME_WARNING;
            break;
        }
        case Error::Type::VariantKeyMismatchWarning: {
            status = U_VARIANT_KEY_MISMATCH_WARNING;
            break;
        }
        case Error::Type::NonexhaustivePattern: {
            status = U_NONEXHAUSTIVE_PATTERN_WARNING;
            break;
        }
        case Error::Type::UnknownFunction: {
            status = U_UNKNOWN_FUNCTION_WARNING;
            break;
        }
        case Error::Type::UnresolvedVariable: {
            status = U_UNRESOLVED_VARIABLE_WARNING;
            break;
        }
        case Error::Type::FormattingWarning: {
            status = U_FORMATTING_WARNING;
            break;
        }
        case Error::Type::MissingSelectorAnnotation: {
            status = U_MISSING_SELECTOR_ANNOTATION_WARNING;
            break;
        }

        case Error::Type::ReservedError: {
            status = U_UNSUPPORTED_PROPERTY;
            break;
        }
        case Error::Type::SyntaxError: {
            status = U_SYNTAX_WARNING;
            break;
        }
        case Error::Type::SelectorError: {
            status = U_SELECTOR_WARNING;
            break;
        }
    }
}

void Errors::addSyntaxError(UErrorCode& status) {
    CHECK_ERROR(status);
    addError(Error(Error::Type::SyntaxError), status);
}

void Errors::addError(Error e, UErrorCode& status) {
    CHECK_ERROR(status);

    Error* eP = new Error(e);
    if (eP == nullptr) {
        status = U_MEMORY_ALLOCATION_ERROR;
        return;
    }
    switch (e.type) {
        case Error::Type::SyntaxError: {
            syntaxError = true;
            syntaxAndDataModelErrors->adoptElement(eP, status);
            break;
        }
        case Error::Type::DuplicateOptionName: {
            dataModelError = true;
            syntaxAndDataModelErrors->adoptElement(eP, status);
            break;
        }
        case Error::Type::VariantKeyMismatchWarning: {
            dataModelError = true;
            syntaxAndDataModelErrors->adoptElement(eP, status);
            break;
        }
        case Error::Type::NonexhaustivePattern: {
            dataModelError = true;
            syntaxAndDataModelErrors->adoptElement(eP, status);
            break;
        }
        case Error::Type::UnresolvedVariable: {
            syntaxAndDataModelErrors->adoptElement(eP, status);
            break;
        }
        case Error::Type::FormattingWarning: {
            formattingWarning = true;
            resolutionAndFormattingErrors->adoptElement(eP, status);
            break;
        }
        case Error::Type::MissingSelectorAnnotation: {
            missingSelectorAnnotationError = true;
            dataModelError = true;
            syntaxAndDataModelErrors->adoptElement(eP, status);
            break;
        }
        case Error::Type::ReservedError: {
            dataModelError = true;
            syntaxAndDataModelErrors->adoptElement(eP, status);
            break;
        }
        case Error::Type::SelectorError: {
            selectorError = true;
            resolutionAndFormattingErrors->adoptElement(eP, status);
            break;
        }
        case Error::Type::UnknownFunction: {
            unknownFunctionError = true;
            resolutionAndFormattingErrors->adoptElement(eP, status);
            break;
        }
    }
}

Context::~Context() {}

} // namespace message2
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */
