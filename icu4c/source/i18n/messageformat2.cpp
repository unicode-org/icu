// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/messageformat2.h"
#include "unicode/messageformat2_data_model.h"
#include "uvector.h" // U_ASSERT

U_NAMESPACE_BEGIN namespace message2 {

using Binding         = MessageFormatDataModel::Binding;
using Bindings        = MessageFormatDataModel::Bindings;
using Expression      = MessageFormatDataModel::Expression;
using ExpressionList  = MessageFormatDataModel::ExpressionList;
using Key             = MessageFormatDataModel::Key;
using KeyList         = MessageFormatDataModel::KeyList;
using Literal         = MessageFormatDataModel::Literal;
using OptionMap       = MessageFormatDataModel::OptionMap;
using Operand         = MessageFormatDataModel::Operand;
using Operator        = MessageFormatDataModel::Operator;
using Pattern         = MessageFormatDataModel::Pattern;
using PatternPart     = MessageFormatDataModel::PatternPart;
using Reserved        = MessageFormatDataModel::Reserved;
using SelectorKeys    = MessageFormatDataModel::SelectorKeys;
using VariantMap      = MessageFormatDataModel::VariantMap;

using PrioritizedVariantList = ImmutableVector<PrioritizedVariant>;

#define TEXT_SELECTOR UnicodeString("select")

// -------------------------------------
// Creates a MessageFormat instance based on the pattern.

// Returns a new (uninitialized) builder
MessageFormatter::Builder* MessageFormatter::builder(UErrorCode& errorCode) {
    if (U_FAILURE(errorCode)) {
        return nullptr;
    }
    LocalPointer<MessageFormatter::Builder> tree(new Builder());
    if (!tree.isValid()) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }
    return tree.orphan();
}

MessageFormatter::Builder& MessageFormatter::Builder::setPattern(const UnicodeString& pat, UErrorCode& errorCode) {
    THIS_ON_ERROR(errorCode);

    pattern.adoptInstead(new UnicodeString(pat));
    if (!pattern.isValid()) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    } else {
        // Invalidate the data model
        dataModel.adoptInstead(nullptr);
    }
    return *this;
}

// Precondition: `reg` is non-null
// Does *not* adopt `reg`
MessageFormatter::Builder& MessageFormatter::Builder::setFunctionRegistry(const FunctionRegistry* reg) {
    U_ASSERT(reg != nullptr);
    customFunctionRegistry = reg;
    return *this;
}

MessageFormatter::Builder& MessageFormatter::Builder::setLocale(const Locale& loc) {
    locale = loc;
    return *this;
}

// Takes ownership of `dataModel`
MessageFormatter::Builder& MessageFormatter::Builder::setDataModel(MessageFormatDataModel* newDataModel) {
  U_ASSERT(newDataModel != nullptr);
  dataModel.adoptInstead(newDataModel);

  // Invalidate the pattern
  pattern.adoptInstead(nullptr);
  return *this;
}

/*
  For now, this is a destructive build(); it invalidates the builder

  It's probably better for this method to either copy the builder,
  or use a reference to (e.g.) a function registry with the assumption that it
  has the same lifetime as the formatter.
  See the custom functions example in messageformat2test.cpp for motivation.
*/
MessageFormatter* MessageFormatter::Builder::build(UParseError& parseError, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);

    LocalPointer<MessageFormatter> mf(new MessageFormatter(*this, parseError, errorCode));
    if (U_FAILURE(errorCode)) {
        return nullptr;
    }
    return mf.orphan();
}

void MessageFormatter::initErrors(UErrorCode& errorCode) {
    CHECK_ERROR(errorCode);
    errors.adoptInstead(Errors::create(errorCode));
}

MessageFormatter::MessageFormatter(MessageFormatter::Builder& builder, UParseError &parseError,
                                   UErrorCode &success) : locale(builder.locale), customFunctionRegistry(builder.customFunctionRegistry) {
    CHECK_ERROR(success);

    // Set up the standard function registry
    LocalPointer<FunctionRegistry::Builder> standardFunctionsBuilder(FunctionRegistry::builder(success));
    CHECK_ERROR(success);

    standardFunctionsBuilder->setFormatter(UnicodeString("datetime"), new StandardFunctions::DateTimeFactory(), success)
        .setFormatter(UnicodeString("number"), new StandardFunctions::NumberFactory(), success)
        .setFormatter(UnicodeString("identity"), new StandardFunctions::IdentityFactory(), success)
        .setSelector(UnicodeString("plural"), new StandardFunctions::PluralFactory(UPLURAL_TYPE_CARDINAL), success)
        .setSelector(UnicodeString("selectordinal"), new StandardFunctions::PluralFactory(UPLURAL_TYPE_ORDINAL), success)
        .setSelector(UnicodeString("select"), new StandardFunctions::TextFactory(), success)
        .setSelector(UnicodeString("gender"), new StandardFunctions::TextFactory(), success);
    standardFunctionRegistry.adoptInstead(standardFunctionsBuilder->build(success));
    CHECK_ERROR(success);
    standardFunctionRegistry->checkStandard();

    initErrors(success);
    CHECK_ERROR(success);

    // Validate pattern and build data model
    // First, check that exactly one of the pattern and data model are set, but not both

    bool patternSet = builder.pattern.isValid();
    bool dataModelSet = builder.dataModel.isValid();

    if ((!patternSet && !dataModelSet)
        || (patternSet && dataModelSet)) {
      success = U_INVALID_STATE_ERROR;
      return;
    }

    // If data model was set, just assign it
    if (dataModelSet) {
      dataModel.adoptInstead(builder.dataModel.orphan());
      return;
    }

    LocalPointer<MessageFormatDataModel::Builder> tree(MessageFormatDataModel::builder(success));
    if (U_FAILURE(success)) {
      return;
    }

    // Initialize formatter cache
    cachedFormatters.adoptInstead(new CachedFormatters(success));

    // Parse the pattern
    normalizedInput.adoptInstead(new UnicodeString(u""));
    if (!normalizedInput.isValid()) {
      success = U_MEMORY_ALLOCATION_ERROR;
      return;
    }
    LocalPointer<Parser> parser(Parser::create(*builder.pattern, *tree, *normalizedInput, *errors, success));
    CHECK_ERROR(success);
    parser->parse(parseError, success);

    // Build the data model based on what was parsed
    LocalPointer<MessageFormatDataModel> dataModelPtr(tree->build(success));
    if (U_SUCCESS(success)) {
      dataModel.adoptInstead(dataModelPtr.orphan());
    }
}

MessageFormatDataModel::~MessageFormatDataModel() {}
MessageFormatter::~MessageFormatter() {}

// ------------------------------------------------------
// MessageArguments

using Arguments = MessageArguments;
using Options = FunctionRegistry::Options;
using Option = FunctionRegistry::Option;

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

static Formattable* createFormattable(const UnicodeString& s, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);
    Formattable* result = new Formattable(s);
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return result;
}

static Formattable* createFormattableDouble(double val, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);
    Formattable* result = new Formattable(val);
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return result;
}

static Formattable* createFormattableLong(long val, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);
    Formattable* result = new Formattable(val);
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return result;
}

static Formattable* createFormattableInt64(int64_t val, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);
    Formattable* result = new Formattable(val);
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return result;
}

static Formattable* createFormattableDate(UDate val, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);
    Formattable* result = new Formattable(val, Formattable::kIsDate);
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return result;
}

static Formattable* createFormattableArray(const UnicodeString* in, size_t count, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);

    LocalArray<Formattable> arr(new Formattable[count]);
    if (!arr.isValid()) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }

    LocalPointer<Formattable> val;
    for (size_t i = 0; i < count; i++) {
        // TODO
        // Without this explicit cast, `val` is treated as if it's
        // an object when it's assigned into `arr[i]`. I don't know why.
        val.adoptInstead(new Formattable((const UnicodeString&) in[i]));
        if (!val.isValid()) {
            errorCode = U_MEMORY_ALLOCATION_ERROR;
            return nullptr;
        }
        arr[i] = *val;
    }

    Formattable* result(new Formattable(arr.orphan(), count));
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return result;
}

static Formattable* createFormattableObject(UObject* obj, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);

    Formattable* result(new Formattable(obj));
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return result;
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

    Formattable* valPtr(createFormattable(val, errorCode));
    THIS_ON_ERROR(errorCode);
    return add(name, valPtr, errorCode);
}

Arguments::Builder& Arguments::Builder::addDouble(const UnicodeString& name, double val, UErrorCode& errorCode) {
    THIS_ON_ERROR(errorCode);

    Formattable* valPtr(createFormattableDouble(val, errorCode));
    THIS_ON_ERROR(errorCode);
    return add(name, valPtr, errorCode);
}

Arguments::Builder& Arguments::Builder::addInt64(const UnicodeString& name, int64_t val, UErrorCode& errorCode) {
    THIS_ON_ERROR(errorCode);

    Formattable* valPtr(createFormattableInt64(val, errorCode));
    THIS_ON_ERROR(errorCode);
    return add(name, valPtr, errorCode);
}

Arguments::Builder& Arguments::Builder::addLong(const UnicodeString& name, long val, UErrorCode& errorCode) {
    THIS_ON_ERROR(errorCode);

    Formattable* valPtr(createFormattableLong(val, errorCode));
    THIS_ON_ERROR(errorCode);
    return add(name, valPtr, errorCode);
}

Arguments::Builder& Arguments::Builder::addDate(const UnicodeString& name, UDate val, UErrorCode& errorCode) {
    THIS_ON_ERROR(errorCode);

    Formattable* valPtr(createFormattableDate(val, errorCode));
    THIS_ON_ERROR(errorCode);
    return add(name, valPtr, errorCode);
}

Arguments::Builder& Arguments::Builder::add(const UnicodeString& name, const UnicodeString* arr, size_t count, UErrorCode& errorCode) {
    THIS_ON_ERROR(errorCode);

    Formattable* valPtr(createFormattableArray(arr, count, errorCode));
    THIS_ON_ERROR(errorCode);
    return add(name, valPtr, errorCode);
}

// Does not adopt the object
Arguments::Builder& Arguments::Builder::addObject(const UnicodeString& name, UObject* obj, UErrorCode& errorCode) {
    THIS_ON_ERROR(errorCode);

    Formattable* value(createFormattableObject(obj, errorCode));
    THIS_ON_ERROR(errorCode);

    objectContents->put(name, value, errorCode);
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
// Formatting

FormatterFactory* MessageFormatter::lookupFormatterFactory(Context& context, const FunctionName& functionName, UErrorCode& status) const {
    NULL_ON_ERROR(status);

    if (isBuiltInFormatter(functionName)) {
        return standardFunctionRegistry->getFormatter(functionName);
    }
    if (isBuiltInSelector(functionName)) {
        context.setFormattingWarning(functionName, status);
        return nullptr;
    }
    if (hasCustomFunctionRegistry()) {
        const FunctionRegistry& customFunctionRegistry = getCustomFunctionRegistry();
        FormatterFactory* customFormatter = customFunctionRegistry.getFormatter(functionName);
        if (customFormatter != nullptr) {
            return customFormatter;
        }
        if (customFunctionRegistry.getSelector(functionName) != nullptr) {
            status = U_FORMATTING_WARNING;
            return nullptr;
        }
    }
    // Either there is no custom function registry and the function
    // isn't built-in, or the function doesn't exist in either the built-in
    // or custom registry.
    // Unknown function error
    context.setUnknownFunctionWarning(functionName, status);
    return nullptr;
}


const Formatter* MessageFormatter::maybeCachedFormatter(Context& context, const FunctionName& f, UErrorCode& errorCode) const {
    NULL_ON_ERROR(errorCode);
    U_ASSERT(cachedFormatters.isValid());

    const Formatter* result = cachedFormatters->getFormatter(f);
    if (result == nullptr) {
        // Create the formatter

        // First, look up the formatter factory for this function
        FormatterFactory* formatterFactory = lookupFormatterFactory(context, f, errorCode);
        NULL_ON_ERROR(errorCode);
        // If the formatter factory was null, there must have been
        // an earlier error/warning
        if (formatterFactory == nullptr) {
            U_ASSERT(context.hasUnknownFunctionError() || context.hasFormattingWarning());
            return nullptr;
        }
        NULL_ON_ERROR(errorCode);

        // Create a specific instance of the formatter
        Formatter* formatter = formatterFactory->createFormatter(locale, errorCode);
        NULL_ON_ERROR(errorCode);
        if (formatter == nullptr) {
            errorCode = U_MEMORY_ALLOCATION_ERROR;
            return nullptr;
        }
        cachedFormatters->setFormatter(f, formatter, errorCode);
        return formatter;
    } else {
        return result;
    }
}

static const Formattable& evalLiteral(const Literal& lit) {
    return lit.getContents();
}

void MessageFormatter::evalArgument(const VariableName& var, FormattedValueBuilder& context) const {
    U_ASSERT(context.hasGlobal(var));
    context.setFallback(var);
    if (context.hasGlobalAsFormattable(var)) {
        context.setInput(context.getGlobalAsFormattable(var));
    } else {
        context.setInput(context.getGlobalAsObject(var));
    }
}

void MessageFormatter::formatLiteral(const Literal& lit, FormattedValueBuilder& context) const {
    context.setFallback(lit);
    context.setInput(evalLiteral(lit));
}

void MessageFormatter::formatOperand(const Environment& env, const Operand& rand, FormattedValueBuilder& context, UErrorCode &status) const {
    CHECK_ERROR(status);
    if (rand.isNull()) {
        context.setNoOperand();
        return;
    }
    if (rand.isVariable()) {
        // Check if it's local or global
        // TODO: Currently, this code allows name shadowing, but depending on the
        // resolution of:
        //   https://github.com/unicode-org/message-format-wg/issues/310
        // it might need to forbid it.
        const VariableName& var = *rand.asVariable();
        // TODO: Currently, this code implements lazy evaluation of locals.
        // That is, the environment binds names to an Expression, not a resolved value.
        // Eager vs. lazy evaluation is an open issue:
        // see https://github.com/unicode-org/message-format-wg/issues/299
        const Closure* rhs = env.lookup(var);
        if (rhs != nullptr) {
            formatExpression(rhs->getEnv(), rhs->getExpr(), context, status);
            return;
        }
        // Use fallback per
        // https://github.com/unicode-org/message-format-wg/blob/main/spec/formatting.md#fallback-resolution
        context.setFallback(var);
        // Not found in locals -- must be global
        if (context.hasGlobal(var)) {
            evalArgument(var, context);
            return;
        } else {
            // Unbound variable -- Resolution error
            context.setUnresolvedVariable(var, status);
            return;
        }
    } else if (rand.isLiteral()) {
        formatLiteral(*rand.asLiteral(), context);
        return;
    }
}

bool MessageFormatter::isBuiltInFormatter(const FunctionName& functionName) const {
    return (standardFunctionRegistry->hasFormatter(functionName));
}

bool MessageFormatter::isBuiltInSelector(const FunctionName& functionName) const {
    return (standardFunctionRegistry->hasSelector(functionName));
}

void MessageFormatter::resolveOptions(const Environment& env, const OptionMap& options, FormattedValueBuilder& context, UErrorCode& status) const {
    CHECK_ERROR(status);

    size_t pos = OptionMap::FIRST;
    LocalPointer<FormattedValueBuilder> rhsContext;
    while (true) {
        UnicodeString k;
        const Operand* v;
        if (!options.next(pos, k, v)) {
            break;
        }
        U_ASSERT(v != nullptr);
        // Options are fully evaluated before calling the function
        // Create a new context for formatting the right-hand side of the option
        rhsContext.adoptInstead(context.create(status));
        CHECK_ERROR(status);
        formatOperand(env, *v, *rhsContext, status);
// TODO
        CHECK_ERROR(status);
/*
        // Try to fully format the result
        context.formatToString(locale, status);
        CHECK_ERROR(status);
*/

        // If formatting succeeded, pass the string
        if (rhsContext->hasStringOutput()) {
            context.setStringOption(k, rhsContext->getStringOutput(), status);
        } else if (rhsContext->hasFormattableInput()) {
            // (Fall back to the input if the result was a formatted number)
            const Formattable& f = rhsContext->getFormattableInput();
            switch (f.getType()) {
                case Formattable::Type::kDate: {
                    context.setDateOption(k, f.getDate(), status);
                    break;
                }
                case Formattable::Type::kDouble: {
                    context.setNumericOption(k, f.getDouble(), status);
                    break;
                }
                case Formattable::Type::kLong: {
                    context.setNumericOption(k, f.getLong(), status);
                    break;
                }
                case Formattable::Type::kInt64: {
                    context.setNumericOption(k, f.getInt64(), status);
                    break;
                }
                case Formattable::Type::kString: {
                    context.setStringOption(k, f.getString(), status);
                    break;
                }
                default: {
                    // Options with array or object types are ignored
                    continue;
                }
            }
        } else if (rhsContext->hasObjectInput()) {
            // Ignore object values; don't pass them as options
        } else {
            // ignore fallbacks
            U_ASSERT(rhsContext->isFallback());
        }
    }
}

// https://github.com/unicode-org/message-format-wg/issues/409
// Unknown function = unknown function error
// Formatter used as selector  = selector error
// Selector used as formatter = formatting error
const SelectorFactory* MessageFormatter::lookupSelectorFactory(Context& context, const FunctionName& functionName, UErrorCode& status) const {
    NULL_ON_ERROR(status);

    if (isBuiltInSelector(functionName)) {
        return standardFunctionRegistry->getSelector(functionName);
    }
    if (isBuiltInFormatter(functionName)) {
        context.setSelectorError(functionName, status);
        return nullptr;
    }
    if (customFunctionRegistry != nullptr) {
        const FunctionRegistry& customFunctionRegistry = getCustomFunctionRegistry();
        const SelectorFactory* customSelector = customFunctionRegistry.getSelector(functionName);
        if (customSelector != nullptr) {
            return customSelector;
        }
        if (customFunctionRegistry.getFormatter(functionName) != nullptr) {
            context.setSelectorError(functionName, status);
            return nullptr;
        }
    }
    // Either there is no custom function registry and the function
    // isn't built-in, or the function doesn't exist in either the built-in
    // or custom registry.
    // Unknown function error
    context.setUnknownFunctionWarning(functionName, status);
    return nullptr;
}


// hasOperand set to true if `rand` resolves to an expression that's a unary function call
// for example $foo => {$bar :plural} => hasOperand set to true
//             $foo => {:func} => hasOperand set to false
void MessageFormatter::resolveVariables(const Environment& env, const Operand& rand, FormattedValueBuilder& context, UErrorCode &status) const {
    CHECK_ERROR(status);

    if (rand.isNull()) {
        // Nothing to do
        return;
    } else if (rand.isLiteral()) {
        U_ASSERT(!context.hasOperator());
        formatLiteral(*rand.asLiteral(), context);
    } else {
        // Must be variable
        const VariableName& var = *rand.asVariable();
        // Resolve it
        const Closure* referent = env.lookup(var);
        if (referent != nullptr) {
            // Resolve the referent
            resolveVariables(referent->getEnv(), referent->getExpr(), context, status);
            return;
        }
        // Either this is a global var or an unbound var --
        // either way, it can't be bound to a function call
        U_ASSERT(context.hasOperator());
        context.setFallback(var);
        // Check globals
        if (context.hasGlobal(var)) {
            evalArgument(var, context);
        } else {   
            // Unresolved variable -- could be a previous warning. Nothing to resolve
        }
    }
}

// Resolves the expression just enough to expose a function call
// (or until a literal is reached)
void MessageFormatter::resolveVariables(const Environment& env, const Expression& expr, FormattedValueBuilder& context, UErrorCode &status) const {
    CHECK_ERROR(status);

    // A `reserved` is an error
    if (expr.isReserved()) {
        context.setReservedError(status);
        U_ASSERT(context.isFallback());
        return;
    }

    // Function call -- resolve the operand and options
    if (expr.isFunctionCall()) {
        const Operator& rator = expr.getOperator();
        context.setFunctionName(rator.getFunctionName(), status);
        resolveOptions(env, rator.getOptions(), context, status);
        // Operand may be the null argument, but resolveVariables() handles that
        formatOperand(env, expr.getOperand(), context, status);
    } else {
        resolveVariables(env, expr.getOperand(), context, status);
    }
}

// Leaves `context` either as a fallback with errors,
// or in a state with a pending call to a selector that has been set
void MessageFormatter::formatSelectorExpression(const Environment& globalEnv, const Expression& expr, FormattedValueBuilder& context, UErrorCode &status) const {
    CHECK_ERROR(status);

    // Resolve expression to determine if it's a function call
    resolveVariables(globalEnv, expr, context, status);

    if (context.hasSelector()) {
        // Represent the function call with the given options, applied to the operand
        if (context.isFallback()) {
            // Use a null expression if it's a syntax or data model warning;
            // create a valid (non-fallback) formatted placeholder from the
            // fallback string otherwise
            if (context.hasParseError() || context.hasDataModelError()) {
                U_ASSERT(!context.hasInput());
            } else {
                context.promoteFallbackToInput();
            }
        }
    } else {
        if (context.hasFunctionName()) {
            const FunctionName& fn = context.getFunctionName();
            // If it's a formatter, then signal a selector error.
            if (context.hasFormatter()) {
                context.setSelectorError(fn, status);
            } else {
                // Otherwise, the function must be unknown
                context.setUnknownFunction(fn, status);
            }
        } else {
            // No function name -- this is a missing selector annotation error
            context.setMissingSelectorAnnotation(status);
        }
        context.clearFunctionName();
        context.clearFunctionOptions();
        context.setFallback();
    }
}

void MessageFormatter::formatExpression(const Environment& globalEnv, const Expression& expr, FormattedValueBuilder& context, UErrorCode &status) const {
    CHECK_ERROR(status);

    // Formatting error
    if (expr.isReserved()) {
        context.setReservedError(status);
        U_ASSERT(context.isFallback());
        return;
    }

    const Operand& rand = expr.getOperand();
    // Format the argument, which may be null (formatOperand handles that)
    formatOperand(globalEnv, rand, context, status);

    if (expr.isFunctionCall()) {
        const Operator& rator = expr.getOperator();
        const FunctionName& functionName = rator.getFunctionName();
        const OptionMap& options = rator.getOptions();
        // Resolve the options
        resolveOptions(globalEnv, options, context, status);

        // Don't call the function on error values
        if (context.isFallback()) {
            return;
        }

        context.evalFormatterCall(functionName, status);
        // Call was successful
        if (context.hasOutput() && U_SUCCESS(status)) {
            return;
        } else if (!(context.hasError())) {
            // Set formatting warning if formatting function had no output
            // but didn't set an error or warning
            context.setFormattingWarning(functionName.name(), status);
        }

        // If we reached this point, the formatter is null --
        // Must have been a previous unknown function warning
        if (rand.isNull()) {
            context.setFallback(functionName);
        }
        context.setFallback();
        return;
    }
}

void MessageFormatter::formatPattern(Context& globalContext, const Environment& globalEnv, const Pattern& pat, UErrorCode &status, UnicodeString& result) const {
    CHECK_ERROR(status);

    LocalPointer<FormattedValueBuilder> context;
    for (size_t i = 0; i < pat.numParts(); i++) {
        const PatternPart* part = pat.getPart(i);
        U_ASSERT(part != nullptr);
        if (part->isText()) {
            result += part->asText();
        } else {
            context.adoptInstead(FormattedValueBuilder::create(globalContext, *this, status));
            CHECK_ERROR(status);
            formatExpression(globalEnv, part->contents(), *context, status);
            context->formatToString(locale, status);
            CHECK_ERROR(status);
            result += context->getStringOutput();
        }
    }
}

// See https://github.com/unicode-org/message-format-wg/blob/main/spec/formatting.md#resolve-selectors
void MessageFormatter::resolveSelectors(Context& context, const Environment& env, const ExpressionList& selectors, UErrorCode &status, UVector& res) const {
    CHECK_ERROR(status);

    // 1. Let res be a new empty list of resolved values that support selection.
    // (Implicit, since `res` is an out-parameter)
    // 2. For each expression exp of the message's selectors
    LocalPointer<FormattedValueBuilder> rv;
    for (size_t i = 0; i < selectors.length(); i++) {
        rv.adoptInstead(FormattedValueBuilder::create(context, *this, status));
        CHECK_ERROR(status);
        // 2i. Let rv be the resolved value of exp.
        formatSelectorExpression(env, *selectors.get(i), *rv, status);
        if (!rv->hasSelector()) {
            U_ASSERT(rv->hasUnknownFunctionError() || rv->hasSelectorError());
            U_ASSERT(rv->isFallback());
        }
        // TODO: update this comment
        // 2ii. If selection is supported for rv:
        // (Always true, since here, selector functions are strings)
        // 2ii(a). Append rv as the last element of the list res.
        res.addElement(rv.orphan(), status);
    }
}

static void keysToArray(const UVector& keys, UnicodeString* out) {
    for (int32_t i = 0; i < keys.size(); i++) {
        out[i] = *((UnicodeString*) keys[i]);
    }
}

static void arrayToKeys(const UnicodeString* in/*[]*/, size_t len, UVector& keys, UErrorCode& status) {
    CHECK_ERROR(status);

    keys.removeAllElements();
    if (in == nullptr) {
        return;
    }
    LocalPointer<UnicodeString> s;

    for (size_t i = 0; i < len; i++) {
        s.adoptInstead(new UnicodeString(in[i]));
        if (!s.isValid()) {
            status = U_MEMORY_ALLOCATION_ERROR;
            return;
        }
        keys.adoptElement(s.orphan(), status);
    }
}


// See https://github.com/unicode-org/message-format-wg/blob/main/spec/formatting.md#resolve-preferences
// `keys` and `matches` are both vectors of strings
void MessageFormatter::matchSelectorKeys(const UVector& keys, FormattedValueBuilder& rv, UErrorCode& status, UVector& matches) const {
    CHECK_ERROR(status);

    if (rv.isFallback()) {
        // Return an empty list of matches
        matches.removeAllElements();
        return;
    }
    U_ASSERT(rv.hasSelector());
    
    // As input to selectKeys(), copy the `keys` vector into
    // an array of Formattables
    size_t numKeys = keys.size();
    LocalArray<UnicodeString> keysIn(new UnicodeString[numKeys]);
    keysToArray(keys, keysIn.getAlias());
    // As output for selectKeys(), create a new empty array
    // with the same size as `keys`
    LocalArray<UnicodeString> keysOut(new UnicodeString[numKeys]);
    if (!keysOut.isValid()) {
        status = U_MEMORY_ALLOCATION_ERROR;
        return;
    }

    size_t numberMatching = 0;
    rv.evalPendingSelectorCall(keysIn.getAlias(), numKeys, keysOut.getAlias(), numberMatching, status);
    CHECK_ERROR(status);
    arrayToKeys(keysOut.getAlias(), numberMatching, matches, status);
}

// Compare strings by value
UBool stringsEqual(const UElement e1, const UElement e2) {
    return (*((UnicodeString*) e1.pointer) == *((UnicodeString*) e2.pointer));
}

// TODO: change vectors to arrays
// See https://github.com/unicode-org/message-format-wg/blob/main/spec/formatting.md#resolve-preferences
// `res` is a vector of strings; `pref` is a vector of vectors of strings
void MessageFormatter::resolvePreferences(const UVector& res, const VariantMap& variants, UErrorCode &status, UVector& pref) const {
    CHECK_ERROR(status);

    // 1. Let pref be a new empty list of lists of strings.
    // (Implicit, since `pref` is an out-parameter)
    LocalPointer<UVector> keys;
    LocalPointer<UVector> matches;
    // 2. For each index i in res
    for (size_t i = 0; ((int32_t) i) < res.size(); i++) {
        // 2i. Let keys be a new empty list of strings.
        keys.adoptInstead(new UVector(status));
        CHECK_ERROR(status);
        // `keys` does adopt its elements
      //  keys->setDeleter(uprv_deleteUObject);
        // 2ii. For each variant `var` of the message
        size_t pos = VariantMap::FIRST;
        while (true) {
            const SelectorKeys* selectorKeys;
            const Pattern* p; // Not used
            if (!variants.next(pos, selectorKeys, p)) {
                break;
            }
            // Note: Here, `var` names the key list of `var`,
            // not a Variant itself
            const KeyList& var = selectorKeys->getKeys();
            // 2ii(a). Let `key` be the `var` key at position i.
            U_ASSERT(i < var.length()); // established by semantic check in formatSelectors()
            const Key& key = *var.get(i);
            // 2ii(b). If `key` is not the catch-all key '*'
            if (!key.isWildcard()) {
                // 2ii(b)(a) Assert that key is a literal.
                // (Not needed)
                // 2ii(b)(b) Let `ks` be the resolved value of `key`.
                const UnicodeString& ks = key.asLiteral().stringContents();
                CHECK_ERROR(status);
                // 2ii(b)(c) Append `ks` as the last element of the list `keys`.
                LocalPointer<UnicodeString> keyAsString(new UnicodeString(ks));
                if (!keyAsString.isValid()) {
                    status = U_MEMORY_ALLOCATION_ERROR;
                    return;
                }
                keys->addElement(keyAsString.orphan(), status);
            }
        }
        // 2iii. Let `rv` be the resolved value at index `i` of `res`.
        FormattedValueBuilder& rv = *((FormattedValueBuilder*) res[i]);
        // 2iv. Let matches be the result of calling the method MatchSelectorKeys(rv, keys)
        matches.adoptInstead(new UVector(status));
        CHECK_ERROR(status);
        matches->setDeleter(uprv_deleteUObject);
        // Set comparator; `matches` is a vector of strings and we want to be able to search it
        matches->setComparer(stringsEqual);
        // `matches` does not adopt its elements
        matchSelectorKeys(*keys, rv, status, *matches);
        // 2v. Append `matches` as the last element of the list `pref`
        pref.addElement(matches.orphan(), status);
    }
}

// See https://github.com/unicode-org/message-format-wg/blob/main/spec/formatting.md#filter-variants
// `pref` is a vector of vectors of strings; `vars` is a vector of PrioritizedVariants
void filterVariants(const VariantMap& variants, const UVector& pref, UErrorCode &status, UVector& vars) {
    CHECK_ERROR(status);

    // 1. Let `vars` be a new empty list of variants.
    // (Not needed since `vars` is an out-parameter)
    // 2. For each variant `var` of the message:
    size_t pos = VariantMap::FIRST;
    while (true) {
        const SelectorKeys* selectorKeys;
        const Pattern* p;
        if (!variants.next(pos, selectorKeys, p)) {
            break;
        }
        // Note: Here, `var` names the key list of `var`,
        // not a Variant itself
        const KeyList& var = selectorKeys->getKeys();
        // 2i. For each index `i` in `pref`:
        bool noMatch = false;
        for (size_t i = 0; ((int32_t) i) < pref.size(); i++) {
            // 2i(a). Let `key` be the `var` key at position `i`.
            U_ASSERT(i < var.length());
            const Key& key = *var.get(i);
            // 2i(b). If key is the catch-all key '*':
            if (key.isWildcard()) {
                // 2i(b)(a). Continue the inner loop on pref.
                continue;
            }
            // 2i(c). Assert that `key` is a literal.
            // (Not needed)
            // 2i(d). Let `ks` be the resolved value of `key`.
            UnicodeString ks = key.asLiteral().stringContents();
            // 2i(e). Let `matches` be the list of strings at index `i` of `pref`.
            const UVector& matches = *((UVector*) pref[i]);
            // 2i(f). If `matches` includes `ks`
            if (matches.contains(&ks)) {
                // 2i(f)(a). Continue the inner loop on `pref`.
                continue;
            }
            // 2i(g). Else:
            // 2i(g)(a). Continue the outer loop on message variants.
            noMatch = true;
            break;
        }
        if (!noMatch) {
            // Append `var` as the last element of the list `vars`.
            LocalPointer<PrioritizedVariant> tuple(new PrioritizedVariant(-1, *selectorKeys, *p));
            if (!tuple.isValid()) {
                status = U_MEMORY_ALLOCATION_ERROR;
                return;
            }
            vars.addElement(tuple.orphan(), status);
        }
    }
}

int32_t comparePrioritizedVariants(const void* context, const void *left, const void *right) {
    (void)(context);

    U_ASSERT(left != nullptr && right != nullptr);
    const PrioritizedVariant& tuple1 = *((PrioritizedVariant*) ((UElement*) left)->pointer);
    const PrioritizedVariant& tuple2 = *((PrioritizedVariant*) ((UElement*) right)->pointer);
    if (tuple1.priority < tuple2.priority) {
        return -1;
    }
    if (tuple1.priority == tuple2.priority) {
        return 0;
    }
    return 1;
}

// See https://github.com/unicode-org/message-format-wg/blob/main/spec/formatting.md#sort-variants
static void sortVariantTuples(UVector& sortable, UErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    sortable.sortWithUComparator(comparePrioritizedVariants, nullptr, errorCode);
}

// See https://github.com/unicode-org/message-format-wg/blob/main/spec/formatting.md#sort-variants
// Leaves the preferred variant as element 0 in `sortable`
// Note: this sorts in-place, so `sortable` is just `vars`
// `pref` is a vector of vectors of strings; `vars` is a vector of PrioritizedVariants
void sortVariants(const UVector& pref, UErrorCode& status, UVector& vars) {
    CHECK_ERROR(status);

// Note: steps 1 and 2 are omitted since we use `vars` as `sortable` (we sort in-place)
    // 1. Let `sortable` be a new empty list of (integer, variant) tuples.
    // (Not needed since `sortable` is an out-parameter)
    // 2. For each variant `var` of `vars`
    // 2i. Let tuple be a new tuple (-1, var).
    // 2ii. Append `tuple` as the last element of the list `sortable`.

    // 3. Let `len` be the integer count of items in `pref`.
    size_t len = pref.size();
    // 4. Let `i` be `len` - 1.
    int32_t i = len - 1;
    // 5. While i >= 0:
    while (i >= 0) {
        // 5i. Let `matches` be the list of strings at index `i` of `pref`.
        const UVector& matches = *((UVector*) pref[i]);
        // 5ii. Let `minpref` be the integer count of items in `matches`.
        size_t minpref = matches.size();
        // 5iii. For each tuple `tuple` of `sortable`:
        for (size_t j = 0; ((int32_t) j) < vars.size(); j++) {
            PrioritizedVariant* tuple = ((PrioritizedVariant*)vars[j]);
            // 5iii(a). Let matchpref be an integer with the value minpref.
            size_t matchpref = minpref;
            // 5iii(b). Let `key` be the tuple variant key at position `i`.
            const KeyList& tupleVariantKeys = tuple->keys.getKeys();
            U_ASSERT(i < ((int32_t) tupleVariantKeys.length())); // Given by earlier semantic checking
            const Key& key = *tupleVariantKeys.get(((size_t) i));
            // 5iii(c) If `key` is not the catch-all key '*':
            if (!key.isWildcard()) {
                // 5iii(c)(a). Assert that `key` is a literal.
                // (Not needed)
                // 5iii(c)(b). Let `ks` be the resolved value of `key`.
                UnicodeString ks = key.asLiteral().stringContents();
                // 5iii(c)(c) Let matchpref be the integer position of ks in `matches`.
                U_ASSERT(matches.contains(&ks));
                matchpref = matches.indexOf(&ks);
            }
            // 5iii(d) Set the `tuple` integer value as matchpref.
            tuple->priority = matchpref;
        }
        // 5iv. Set `sortable` to be the result of calling the method SortVariants(`sortable`)
        sortVariantTuples(vars, status);
        // 5v. Set `i` to be `i` - 1.
        i--;
    }
    // The caller is responsible for steps 6 and 7
    // 6. Let `var` be the `variant` element of the first element of `sortable`.
    // 7. Select the pattern of `var`
}

void MessageFormatter::formatSelectors(Context& context, const Environment& env, const ExpressionList& selectors, const VariantMap& variants, UErrorCode &status, UnicodeString& result) const {
    CHECK_ERROR(status);

    // See https://github.com/unicode-org/message-format-wg/blob/main/spec/formatting.md#pattern-selection

    // Resolve Selectors
    // res is a vector of ResolvedExpressions
    LocalPointer<UVector> res(new UVector(status));
    CHECK_ERROR(status);
//    res->setDeleter(uprv_deleteUObject);
    resolveSelectors(context, env, selectors, status, *res);

    // Resolve Preferences
    // pref is a vector of vectors of strings
    LocalPointer<UVector> pref(new UVector(status));
    CHECK_ERROR(status);
  //  pref->setDeleter(uprv_deleteUObject);
    resolvePreferences(*res, variants, status, *pref);
   
    // Filter Variants
    // vars is a vector of PrioritizedVariants
    LocalPointer<UVector> vars(new UVector(status));
    CHECK_ERROR(status);
    // vars->setDeleter(uprv_deleteUObject);
    filterVariants(variants, *pref, status, *vars);

    // Sort Variants and select the final pattern
    // Note: `sortable` in the spec is just `vars` here,
    // which is sorted in-place
    sortVariants(*pref, status, *vars);
    CHECK_ERROR(status); // needs to be checked to ensure that `sortable` is valid

    // 6. Let `var` be the `variant` element of the first element of `sortable`.
    U_ASSERT(vars->size() > 0); // This should have been checked earlier (having 0 variants would be a data model error)
    const PrioritizedVariant& var = *((PrioritizedVariant*) (*vars)[0]);
    // 7. Select the pattern of `var`
    const Pattern& pat = var.pat;

    // Format the pattern
    formatPattern(context, env, pat, status, result);
}

void MessageFormatter::check(Context& context, const Environment& localEnv, const OptionMap& options, UErrorCode &status) const {
    CHECK_ERROR(status);

    // Check the RHS of each option
    size_t pos = OptionMap::FIRST;
    UnicodeString k; // not used
    const Operand* rhs;
    while(true) {
        if (!options.next(pos, k, rhs)) {
            break;
        }
        U_ASSERT(rhs != nullptr);
        check(context, localEnv, *rhs, status);
    }
}

void MessageFormatter::check(Context& context, const Environment& localEnv, const Operand& rand, UErrorCode &status) const {
    CHECK_ERROR(status);

    // Nothing to check for literals
    if (rand.isLiteral() || rand.isNull()) {
        return;
    }

    // Check that variable is in scope
    const VariableName& var = *rand.asVariable();
    // Check local scope
    if (localEnv.lookup(var) != nullptr) {
        return;
    }
    // Check global scope
    if (context.hasVar(var)) {
        return;
    }
    context.setUnresolvedVariableWarning(var, status);
}

void MessageFormatter::check(Context& context, const Environment& localEnv, const Expression& expr, UErrorCode &status) const {
    CHECK_ERROR(status);

    // Check for unresolved variable errors
    if (expr.isFunctionCall()) {
        const Operator& rator = expr.getOperator();
        const Operand& rand = expr.getOperand();
        check(context, localEnv, rand, status);
        check(context, localEnv, rator.getOptions(), status);
    }
}

// Check for resolution errors
void MessageFormatter::checkDeclarations(Context& context, Environment*& env, UErrorCode &status) const {
    CHECK_ERROR(status);

    const Bindings& decls = dataModel->getLocalVariables();
    U_ASSERT(env != nullptr);

    for (size_t i = 0; i < decls.length(); i++) {
        const Binding* decl = decls.get(i);
        U_ASSERT(decl != nullptr);
        const Expression* rhs = decl->getValue();
        check(context, *env, *rhs, status);

        // Add a closure to the global environment,
        // memoizing the value of localEnv up to this point
        Closure* closure = Closure::create(*rhs, *env, status);
        CHECK_ERROR(status);

        // Add the LHS to the environment for checking the next declaration
        env = Environment::create(decl->var, closure, *env, status);
        CHECK_ERROR(status);
    }
}

void MessageFormatter::formatToString(const Arguments& arguments, UErrorCode &status, UnicodeString& result) const {
    CHECK_ERROR(status);

    // Note: we currently evaluate variables lazily,
    // without memoization. This call is still necessary
    // to check out-of-scope uses of local variables in
    // right-hand sides (unresolved variable errors can
    // only be checked when arguments are known)

    LocalPointer<Context> context(Context::create(*this, arguments, *errors, status));
    CHECK_ERROR(status);

    // Check for data model errors
    Checker(*dataModel, context->getErrors()).check(status);

    Environment* env = Environment::create(status);
    CHECK_ERROR(status);
    checkDeclarations(*context, env, status);
    CHECK_ERROR(status);
    LocalPointer<Environment> globalEnv(env);

    if (!dataModel->hasSelectors()) {
        formatPattern(*context, *globalEnv, dataModel->getPattern(), status, result);
    } else {
        // Check for errors/warnings -- if so, then the result is the fallback value
        // See https://github.com/unicode-org/message-format-wg/blob/main/spec/formatting.md#pattern-selection
        if (context->hasParseError() || context->hasDataModelError()) {
            result += REPLACEMENT;
        } else {
            formatSelectors(*context, *globalEnv, dataModel->getSelectors(), dataModel->getVariants(), status, result);
        }
    }
    // Update status according to all errors seen while formatting
    context->checkErrors(status);
    clearErrors();
    return;
}

void MessageFormatter::clearErrors() const {
    errors->clearResolutionAndFormattingErrors();
}

// ---------------- Environments and closures

Environment* Environment::create(const VariableName& var, Closure* c, const Environment& parent, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);
    Environment* result = new NonEmptyEnvironment(var, c, parent);
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }
    return result;
}

Environment* Environment::create(UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);
    Environment* result = new EmptyEnvironment();
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }
    return result;
}

Closure* Closure::create(const Expression& expr, const Environment& env, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);
    Closure* result = new Closure(expr, env);
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }
    return result;
}

const Closure* EmptyEnvironment::lookup(const VariableName& v) const {
    (void) v;
    return nullptr;
}

const Closure* NonEmptyEnvironment::lookup(const VariableName& v) const {
    if (v == var) {
        U_ASSERT(rhs.isValid());
        return rhs.getAlias();
    }
    return parent.lookup(v);
}

Environment::~Environment() {}
NonEmptyEnvironment::~NonEmptyEnvironment() {}
EmptyEnvironment::~EmptyEnvironment() {}

Closure::~Closure() {}
} // namespace message2
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */
