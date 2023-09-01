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
// MessageArguments

using Arguments = MessageArguments;

bool Arguments::has(const VariableName& arg) const {
    U_ASSERT(contents.isValid() && objectContents.isValid());
    return contents->containsKey(arg.name()) || objectContents->containsKey(arg.name());
}

const Formattable& Arguments::get(const VariableName& arg) const {
    U_ASSERT(has(arg));
    const Formattable* result = static_cast<const Formattable*>(contents->get(arg.name()));
    if (result == nullptr) {
        result = static_cast<const Formattable*>(objectContents->get(arg.name()));
    }
    U_ASSERT(result != nullptr);
    return *result;
}

Arguments::Builder::Builder(UErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    contents.adoptInstead(new Hashtable(compareVariableName, nullptr, errorCode));
    objectContents.adoptInstead(new Hashtable(compareVariableName, nullptr, errorCode));
    CHECK_ERROR(errorCode);
    // The `contents` hashtable owns the values, but does not own the keys
    contents->setValueDeleter(uprv_deleteUObject);
    // The `objectContents` hashtable does not own the values
}

Arguments::Builder& Arguments::Builder::add(const UnicodeString& name, const UnicodeString& val, UErrorCode& errorCode) {
    THIS_ON_ERROR(errorCode);

    Formattable* valPtr(ExpressionContext::createFormattable(val, errorCode));
    THIS_ON_ERROR(errorCode);
    return add(name, valPtr, errorCode);
}

Arguments::Builder& Arguments::Builder::addDouble(const UnicodeString& name, double val, UErrorCode& errorCode) {
    THIS_ON_ERROR(errorCode);

    Formattable* valPtr(ExpressionContext::createFormattable(val, errorCode));
    THIS_ON_ERROR(errorCode);
    return add(name, valPtr, errorCode);
}

Arguments::Builder& Arguments::Builder::addInt64(const UnicodeString& name, int64_t val, UErrorCode& errorCode) {
    THIS_ON_ERROR(errorCode);

    Formattable* valPtr(ExpressionContext::createFormattable(val, errorCode));
    THIS_ON_ERROR(errorCode);
    return add(name, valPtr, errorCode);
}

Arguments::Builder& Arguments::Builder::addDate(const UnicodeString& name, UDate val, UErrorCode& errorCode) {
    THIS_ON_ERROR(errorCode);

    Formattable* valPtr(ExpressionContext::createFormattableDate(val, errorCode));
    THIS_ON_ERROR(errorCode);
    return add(name, valPtr, errorCode);
}

Arguments::Builder& Arguments::Builder::addDecimal(const UnicodeString& name, StringPiece val, UErrorCode& errorCode) {
    THIS_ON_ERROR(errorCode);

    Formattable* valPtr(ExpressionContext::createFormattableDecimal(val, errorCode));
    THIS_ON_ERROR(errorCode);
    return add(name, valPtr, errorCode);
}

Arguments::Builder& Arguments::Builder::add(const UnicodeString& name, const UnicodeString* arr, int32_t count, UErrorCode& errorCode) {
    THIS_ON_ERROR(errorCode);

    Formattable* valPtr(ExpressionContext::createFormattable(arr, count, errorCode));
    THIS_ON_ERROR(errorCode);
    return add(name, valPtr, errorCode);
}

// Does not adopt the object
Arguments::Builder& Arguments::Builder::addObject(const UnicodeString& name, const UObject* obj, UErrorCode& errorCode) {
    THIS_ON_ERROR(errorCode);

    // The const_cast is valid because the object will only be accessed via
    // getObjectInput(), which returns a const reference
    Formattable* valPtr(ExpressionContext::createFormattable(const_cast<UObject*>(obj), errorCode));
    THIS_ON_ERROR(errorCode);
    objectContents->put(name, valPtr, errorCode);
    return *this;
}

// Adopts its argument
Arguments::Builder& Arguments::Builder::add(const UnicodeString& name, Formattable* value, UErrorCode& errorCode) {
    THIS_ON_ERROR(errorCode);

    U_ASSERT(value != nullptr);

    contents->put(name, value, errorCode);
    return *this;
}

/* static */ MessageArguments::Builder* MessageArguments::builder(UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);
    MessageArguments::Builder* result = new MessageArguments::Builder(errorCode);
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return result;
}

MessageArguments* MessageArguments::Builder::build(UErrorCode& errorCode) const {
    NULL_ON_ERROR(errorCode);
    U_ASSERT(contents.isValid() && objectContents.isValid());

    LocalPointer<Hashtable> contentsCopied(new Hashtable(compareVariableName, nullptr, errorCode));
    LocalPointer<Hashtable> objectContentsCopied(new Hashtable(compareVariableName, nullptr, errorCode));
    NULL_ON_ERROR(errorCode);
    // The `contents` hashtable owns the values, but does not own the keys
    contents->setValueDeleter(uprv_deleteUObject);
    // The `objectContents` hashtable does not own the values

    int32_t pos = UHASH_FIRST;
    LocalPointer<Formattable> optionValue;
    // Copy the non-objects
    while (true) {
        const UHashElement* element = contents->nextElement(pos);
        if (element == nullptr) {
            break;
        }
        const Formattable& toCopy = *(static_cast<Formattable*>(element->value.pointer));
        optionValue.adoptInstead(new Formattable(toCopy));
        if (!optionValue.isValid()) {
            errorCode = U_MEMORY_ALLOCATION_ERROR;
            return nullptr;
        }
        UnicodeString* key = static_cast<UnicodeString*>(element->key.pointer);
        contentsCopied->put(*key, optionValue.orphan(), errorCode);
    }
    // Copy the objects
    pos = UHASH_FIRST;
    while (true) {
        const UHashElement* element = objectContents->nextElement(pos);
        if (element == nullptr) {
            break;
        }
        UnicodeString* key = static_cast<UnicodeString*>(element->key.pointer);
        objectContentsCopied->put(*key, element->value.pointer, errorCode);
    }
    MessageArguments* result = new MessageArguments(contentsCopied.orphan(), objectContentsCopied.orphan());
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return result;
}

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

bool Context::hasFormattingError() const {
    return errors.hasFormattingError();
}

bool Context::hasUnresolvedVariableError() const {
    return errors.hasUnresolvedVariableError();
}

void Context::setFormattingError(const FunctionName& formatterName, UErrorCode& status) {
    CHECK_ERROR(status);

    Error err(Error::Type::FormattingError, formatterName);
    errors.addError(err, status);
 }

void Context::setSelectorError(const FunctionName& selectorName, UErrorCode& status) {
    CHECK_ERROR(status);

    Error err(Error::Type::SelectorError, selectorName);
    errors.addError(err, status);
 }

void Context::setUnknownFunctionError(const FunctionName& formatterName, UErrorCode& status) {
    CHECK_ERROR(status);

    Error err(Error::Type::UnknownFunction, formatterName);
    errors.addError(err, status);
 }

void Context::setUnresolvedVariableError(const VariableName& v, UErrorCode& status) {
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
    formattingError = false;
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
            status = U_DUPLICATE_OPTION_NAME_ERROR;
            break;
        }
        case Error::Type::VariantKeyMismatchError: {
            status = U_VARIANT_KEY_MISMATCH_ERROR;
            break;
        }
        case Error::Type::NonexhaustivePattern: {
            status = U_NONEXHAUSTIVE_PATTERN_ERROR;
            break;
        }
        case Error::Type::UnknownFunction: {
            status = U_UNKNOWN_FUNCTION_ERROR;
            break;
        }
        case Error::Type::UnresolvedVariable: {
            status = U_UNRESOLVED_VARIABLE_ERROR;
            break;
        }
        case Error::Type::FormattingError: {
            status = U_FORMATTING_ERROR;
            break;
        }
        case Error::Type::MissingSelectorAnnotation: {
            status = U_MISSING_SELECTOR_ANNOTATION_ERROR;
            break;
        }

        case Error::Type::ReservedError: {
            status = U_UNSUPPORTED_PROPERTY;
            break;
        }
        case Error::Type::SyntaxError: {
            status = U_SYNTAX_ERROR;
            break;
        }
        case Error::Type::SelectorError: {
            status = U_SELECTOR_ERROR;
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
        case Error::Type::VariantKeyMismatchError: {
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
            unresolvedVariableError = true;
            syntaxAndDataModelErrors->adoptElement(eP, status);
            break;
        }
        case Error::Type::FormattingError: {
            formattingError = true;
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
