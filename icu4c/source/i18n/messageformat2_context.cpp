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

Context::Context(const MessageFormatter& mf, const MessageArguments& args, UErrorCode& errorCode) : parent(mf), arguments(args) {
    CHECK_ERROR(errorCode);
    // Initialize errors
    initErrors(errorCode);
}

/* static */ Context* Context::create(const MessageFormatter& mf, const MessageArguments& args, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);

    LocalPointer<Context> result(new Context(mf, args, errorCode));
    NULL_ON_ERROR(errorCode);
    return result.orphan();
}

// Errors
// -----------

void Context::addError(Error e, UErrorCode& status) {
    CHECK_ERROR(status);
    U_ASSERT(errors.isValid());
    errors->addError(e, status);
}

void Context::checkErrors(UErrorCode& status) const {
    CHECK_ERROR(status);
    U_ASSERT(errors.isValid());
    errors->checkErrors(status);
}


bool Context::hasDataModelError() const {
    U_ASSERT(errors.isValid());
    return errors->hasDataModelError();
}

bool Context::hasError() const {
    U_ASSERT(errors.isValid());
    return errors->count() > 0;
}

bool Context::hasFormattingWarning() const {
    U_ASSERT(errors.isValid());
    return errors->hasFormattingWarning();
}

void Context::setFormattingWarning(const FunctionName& formatterName, UErrorCode& status) {
    CHECK_ERROR(status);

    U_ASSERT(errors.isValid());
    Error err(Error::Type::FormattingWarning, formatterName);
    errors->addError(err, status);
 }

bool Context::hasParseError() const {
    U_ASSERT(errors.isValid());
    return errors->hasSyntaxError();
}

bool Context::hasUnknownFunctionError() const {
    U_ASSERT(errors.isValid());
    return errors->hasUnknownFunctionError();
}

bool Context::hasMissingSelectorAnnotationError() const {
    U_ASSERT(errors.isValid());
    return errors->hasMissingSelectorAnnotationError();
}

bool Context::hasSelectorError() const {
    U_ASSERT(errors.isValid());
    return errors->hasSelectorError();
}

bool Context::hasWarning() const {
    U_ASSERT(errors.isValid());
    return errors->hasWarning();
}

void Context::initErrors(UErrorCode& errorCode) {
    CHECK_ERROR(errorCode);
    errors.adoptInstead(Errors::create(errorCode));
}

Errors* Errors::create(UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);
    return new Errors(errorCode);
}

size_t Errors::count() const {
    U_ASSERT(errors.isValid());
    return ((size_t) errors->size());
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
    Error* err = (Error*) (*errors)[0];
    switch (err->type) {
        case Error::Type::VariantKeyMismatchWarning: {
            status = U_VARIANT_KEY_MISMATCH_WARNING;
            break;
        }
        case Error::Type::NonexhaustivePattern: {
            status = U_NONEXHAUSTIVE_PATTERN;
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
            status = U_MISSING_SELECTOR_ANNOTATION;
            break;
        }

        case Error::Type::ReservedError: {
            status = U_UNSUPPORTED_PROPERTY;
            break;
        }
        case Error::Type::SelectorError: {
            status = U_SELECTOR_ERROR;
            break;
        }
    }
}

void Errors::addError(Error e, UErrorCode& status) {
    CHECK_ERROR(status);

    U_ASSERT(errors.isValid());
    Error* eP = new Error(e);
    if (eP == nullptr) {
        status = U_MEMORY_ALLOCATION_ERROR;
        return;
    }
    errors->adoptElement(eP, status);
    switch (e.type) {
        case Error::Type::VariantKeyMismatchWarning: {
            dataModelError = true;
            warning = true;
            break;
        }
        case Error::Type::NonexhaustivePattern: {
            dataModelError = true;
            break;
        }
        case Error::Type::UnresolvedVariable: {
            warning = true;
            break;
        }
        case Error::Type::FormattingWarning: {
            warning = true;
            formattingWarning = true;
            break;
        }
        case Error::Type::MissingSelectorAnnotation: {
            missingSelectorAnnotationError = true;
            dataModelError = true;
            break;
        }
        case Error::Type::ReservedError: {
            dataModelError = true;
            break;
        }
        case Error::Type::SelectorError: {
            selectorError = true;
            break;
        }
        case Error::Type::UnknownFunction: {
            warning = true;
            dataModelError = true;
            unknownFunctionError = true;
            break;
        }
    }
}

Context::~Context() {}

} // namespace message2
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */
