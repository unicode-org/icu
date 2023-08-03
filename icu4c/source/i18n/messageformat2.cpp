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

using PrioritizedVariantList = List<PrioritizedVariant>;

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
MessageFormatter::Builder& MessageFormatter::Builder::setFunctionRegistry(FunctionRegistry* reg) {
    U_ASSERT(reg != nullptr);
    customFunctionRegistry.adoptInstead(reg);
    return *this;
}

MessageFormatter::Builder& MessageFormatter::Builder::setLocale(Locale loc) {
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

MessageFormatter::MessageFormatter(MessageFormatter::Builder& builder, UParseError &parseError,
                                   UErrorCode &success) : locale(builder.locale) {
    CHECK_ERROR(success);

    // Set up the custom function registry, if given
    if (builder.customFunctionRegistry.isValid()) {
        customFunctionRegistry.adoptInstead(builder.customFunctionRegistry.orphan());
    }

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

    // Parse the pattern
    normalizedInput.adoptInstead(new UnicodeString(u""));
    if (!normalizedInput.isValid()) {
      success = U_MEMORY_ALLOCATION_ERROR;
      return;
    }
    LocalPointer<Parser> parser(Parser::create(*builder.pattern, *tree, *normalizedInput, success));
    if (U_FAILURE(success)) {
      return;
    }
    parser->parse(parseError, success);
    if (U_FAILURE(success)) {
      return;
    }
    
    // Build the data model based on what was parsed
    LocalPointer<MessageFormatDataModel> dataModelPtr(tree->build(success));
    if (U_SUCCESS(success)) {
      dataModel.adoptInstead(dataModelPtr.orphan());
    }

    // Check for data model errors
    Checker(*dataModel).check(success);
}

MessageFormatter::ResolvedExpression::~ResolvedExpression() {}
MessageFormatDataModel::~MessageFormatDataModel() {}
MessageFormatter::~MessageFormatter() {}

// ------------------------------------------------------
// Formatting

// Postcondition: !found || result is non-null
const Expression* lookup(const Bindings& env, const VariableName& lhs, bool& found) {
    size_t len = env.length();
    for (int32_t i = len - 1; i >= 0; i--) {
        const Binding& b = *env.get(i);
        if (b.var == lhs) {
            found = true;
            // getValue() guarantees result is non-null
            return b.getValue();
        }
    }
    found = false;
    return nullptr;
}

// Note: formatting a literal can't fail
// (see https://github.com/unicode-org/message-format-wg/blob/main/spec/formatting.md#literal-resolution )
// so formatLiteral() doesn't take an error code.
void formatLiteral(const Literal& lit, UnicodeString& result) {
    result += lit.contents;
}

void MessageFormatter::formatOperand(const Hashtable& arguments, const Operand& rand, UErrorCode &status, UnicodeString& result) const {
    CHECK_ERROR(status);

    if (rand.isVariable()) {
        // Check if it's local or global
        // TODO: Currently, this code allows name shadowing, but depending on the
        // resolution of:
        //   https://github.com/unicode-org/message-format-wg/issues/310
        // it might need to forbid it.
        VariableName var = rand.asVariable();
        const Bindings& env = dataModel->getLocalVariables();
        // TODO: Currently, this code implements lazy evaluation of locals.
        // That is, the environment binds names to an Expression, not a resolved value.
        // Eager vs. lazy evaluation is an open issue:
        // see https://github.com/unicode-org/message-format-wg/issues/299
        bool found = false;
        const Expression* rhs = lookup(env, var, found);
        if (found) {
            U_ASSERT(rhs != nullptr);
            // Function calls can't occur on the rhs as an option => use formatPatternExpression
            formatPatternExpression(arguments, *rhs, status, result);
            return;
        }
        // Not found in locals -- must be global
        if (arguments.containsKey(var)) {
            UnicodeString* val = (UnicodeString*) arguments.get(var);
            U_ASSERT(val != nullptr);
            result += *val;
            return;
        }
        // Unbound variable -- Resolution error
        status = U_UNRESOLVED_VARIABLE;
        return;
    }
    // Must be a literal
    formatLiteral(rand.asLiteral(), result);
}

bool MessageFormatter::isBuiltInFormatter(const FunctionName& functionName) const {
    return (standardFunctionRegistry->hasFormatter(functionName));
}

bool MessageFormatter::isBuiltInSelector(const FunctionName& functionName) const {
    return (standardFunctionRegistry->hasSelector(functionName));
}

Hashtable* MessageFormatter::resolveOptions(const Hashtable& arguments, const OptionMap& options, UErrorCode& status) const {
    NULL_ON_ERROR(status);

    size_t pos = OptionMap::FIRST;
    LocalPointer<Hashtable> result(new Hashtable(compareVariableName, nullptr, status));
    NULL_ON_ERROR(status);
    LocalPointer<UnicodeString> rhs;
    while (true) {
        UnicodeString k;
        const Operand* v;
        if (!options.next(pos, k, v)) {
            break;
        }
        U_ASSERT(v != nullptr);
        UnicodeString rhsTemp;
        formatOperand(arguments, *v, status, rhsTemp);
        NULL_ON_ERROR(status);
        rhs.adoptInstead(new UnicodeString(rhsTemp));
        if (!rhs.isValid()) {
            status = U_MEMORY_ALLOCATION_ERROR;
            return nullptr;
        }
        result->put(k, rhs.orphan(), status);
    }
    return result.orphan();
}

// Precondition: `env` defines `functionName` as a formatter. (This is determined when checking the data model)
void MessageFormatter::formatFunctionCall(const FormatterFactory& formatterFactory, const Hashtable& arguments, const OptionMap& variableOptions, const Operand& rand, UnicodeString& result, UErrorCode& status) const {
    CHECK_ERROR(status);

    // Create a specific instance of the formatter
    const LocalPointer<Formatter> formatter(formatterFactory.createFormatter(getLocale(), arguments, status));
    // Resolve the operand
    UnicodeString resolvedOperand;
    formatOperand(arguments, rand, status, resolvedOperand);
    LocalPointer<const Hashtable> resolvedOptions(resolveOptions(arguments, variableOptions, status));
    CHECK_ERROR(status);
    formatter->format(resolvedOperand, *resolvedOptions, result, status);
}

// https://github.com/unicode-org/message-format-wg/issues/409
// Unknown function = unknown function error
// Formatter used as selector  = selector error
// Selector used as formatter = formatting error
const SelectorFactory* MessageFormatter::lookupSelectorFactory(const FunctionName& functionName, UErrorCode& status) const {
    NULL_ON_ERROR(status);

    if (isBuiltInSelector(functionName)) {
        return standardFunctionRegistry->getSelector(functionName);
    }
    if (isBuiltInFormatter(functionName)) {
        status = U_SELECTOR_ERROR;
        return nullptr;
    }
    if (customFunctionRegistry.isValid()) {
        const FunctionRegistry& customFunctionRegistry = getCustomFunctionRegistry();
        const SelectorFactory* customSelector = customFunctionRegistry.getSelector(functionName);
        if (customSelector != nullptr) {
            return customSelector;
        }
        if (customFunctionRegistry.getFormatter(functionName) != nullptr) {
            status = U_SELECTOR_ERROR;
            return nullptr;
        }
    }
    // Either there is no custom function registry and the function
    // isn't built-in, or the function doesn't exist in either the built-in
    // or custom registry.
    // Unknown function error
    status = U_UNKNOWN_FUNCTION;
    return nullptr;
}

const FormatterFactory* MessageFormatter::lookupFormatterFactory(const FunctionName& functionName, UErrorCode& status) const {
    NULL_ON_ERROR(status);

    if (isBuiltInFormatter(functionName)) {
        return standardFunctionRegistry->getFormatter(functionName);
    }
    if (isBuiltInSelector(functionName)) {
        status = U_FORMATTING_ERROR;
        return nullptr;
    }
    if (customFunctionRegistry.isValid()) {
        const FunctionRegistry& customFunctionRegistry = getCustomFunctionRegistry();
        const FormatterFactory* customFormatter = customFunctionRegistry.getFormatter(functionName);
        if (customFormatter != nullptr) {
            return customFormatter;
        }
        if (customFunctionRegistry.getSelector(functionName) != nullptr) {
            status = U_FORMATTING_ERROR;
            return nullptr;
        }
    }
    // Either there is no custom function registry and the function
    // isn't built-in, or the function doesn't exist in either the built-in
    // or custom registry.
    // Unknown function error
    status = U_UNKNOWN_FUNCTION;
    return nullptr;
}

void MessageFormatter::resolveVariables(const Hashtable& arguments, const Operand& rand, bool& isFunction, Hashtable*& resolvedOptions, UnicodeString& resolvedOperand, UnicodeString& functionName, UErrorCode &status) const {
    CHECK_ERROR(status);

    if (rand.isLiteral()) {
        formatLiteral(rand.asLiteral(), resolvedOperand);
        isFunction = false;
        return;
    }
    // Must be variable
    const VariableName& var = rand.asVariable();
    // Resolve it
    const Bindings& env = dataModel->getLocalVariables();
    bool found = false;
    const Expression* referent = lookup(env, var, found);
    if (found) {
        U_ASSERT(referent != nullptr);
        // Resolve the referent
        return resolveVariables(arguments, *referent, isFunction, resolvedOptions, resolvedOperand, functionName, status);
    }
    // Must be global; resolution errors were already checked
    U_ASSERT(arguments.containsKey(var));
    UnicodeString* val = (UnicodeString*) arguments.get(var);
    U_ASSERT(val != nullptr);
    isFunction = false;
    resolvedOperand = *val;
}

void MessageFormatter::resolveVariables(const Hashtable& arguments, const Expression& expr, bool& isFunction, Hashtable*& resolvedOptions, UnicodeString& resolvedOperand, UnicodeString& functionName, UErrorCode &status) const {
    CHECK_ERROR(status);

    // Unsupported expression error
    if (expr.isReserved()) {
        status = U_UNSUPPORTED_PROPERTY;
        return;
    }

    const Operand& rand = expr.getOperand();

    // Literal or variable
    if (!expr.isFunctionCall()) {
        return resolveVariables(arguments, rand, isFunction, resolvedOptions, resolvedOperand, functionName, status);
    }

    // Function call -- resolve the operand and options
    isFunction = true;
    formatOperand(arguments, expr.getOperand(), status, resolvedOperand);
    const Operator& rator = expr.getOperator();
    functionName = rator.getFunctionName().name;
    LocalPointer<Hashtable> tempResolvedOptions(resolveOptions(arguments, rator.getOptions(), status));
    CHECK_ERROR(status);
    resolvedOptions = tempResolvedOptions.orphan();
}

MessageFormatter::ResolvedExpression* MessageFormatter::formatSelectorExpression(const Hashtable& arguments, const Expression& expr, UErrorCode &status) const {
    NULL_ON_ERROR(status);

    Hashtable* resolvedOptions = nullptr;
    UnicodeString operand;
    UnicodeString functionName;
    bool isFunction = false;
    resolveVariables(arguments, expr, isFunction, resolvedOptions, operand, functionName, status);
    NULL_ON_ERROR(status);
    const SelectorFactory* selectorFactory;
    if (isFunction) {
        U_ASSERT(resolvedOptions != nullptr);

        // Look up the selector for this function
        selectorFactory = lookupSelectorFactory(functionName, status);
        NULL_ON_ERROR(status);
    } else {
        // Plug in the "text" selector
        selectorFactory = lookupSelectorFactory(TEXT_SELECTOR, status);
        U_ASSERT(U_SUCCESS(status));
    }
    // If no error was set, the selector should be non-null
    U_ASSERT(selectorFactory != nullptr);

    if (resolvedOptions == nullptr) {
        resolvedOptions = emptyOptions(status);
        NULL_ON_ERROR(status);
    }
    // Create a specific instance of the selector
    LocalPointer<Selector> selector(selectorFactory->createSelector(getLocale(), arguments, status));
    // Represent the function call with the given options, applied to the operand
    LocalPointer<ResolvedExpression> result(new ResolvedExpression(selector.orphan(), resolvedOptions, operand));
    if (!result.isValid()) {
        status = U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }
    return result.orphan();
}

void MessageFormatter::formatPatternExpression(const Hashtable& arguments, const Expression& expr, UErrorCode &status, UnicodeString& result) const {
    CHECK_ERROR(status);

    // Formatting error
    if (expr.isReserved()) {
        status = U_UNSUPPORTED_PROPERTY;
        return;
    }
    if (expr.isFunctionCall()) {
        const Operator& rator = expr.getOperator();
        const FunctionName& functionName = rator.getFunctionName();

        // Look up the formatter for this function
        const FormatterFactory* formatterFactory = lookupFormatterFactory(functionName, status);

        if (U_SUCCESS(status)) {
            // If no error was set, the formatter should be non-null
            U_ASSERT(formatterFactory != nullptr);
            
            // Format the call
            formatFunctionCall(*formatterFactory, arguments,
                               rator.getOptions(), expr.getOperand(), result, status);
        }

        // Check for errors and use a fallback value if necessary
        if (U_FAILURE(status)) {
            /*
              https://github.com/unicode-org/message-format-wg/blob/main/spec/formatting.md
              "Fallback Resolution" section
             */
            // expression with no operand: function name
            if (expr.isStandaloneAnnotation()) {
                result += functionName.toString();
                return;
            }
            // expression with literal operand: |value|
            const Operand& rand = expr.getOperand();
            if (rand.isLiteral()) {
                result += PIPE;
                result += rand.asLiteral().contents;
                result += PIPE;
                return;
            }
            // Must be a variable
            result += DOLLAR;
            result += rand.asVariable();
            return;
        }
        return;
    }
    formatOperand(arguments, expr.getOperand(), status, result);

}
void MessageFormatter::formatPattern(const Hashtable& arguments, const Pattern& pat, UErrorCode &status, UnicodeString& result) const {
    CHECK_ERROR(status);

    for (size_t i = 0; i < pat.numParts(); i++) {
        const PatternPart* part = pat.getPart(i);
        U_ASSERT(part != nullptr);
        if (part->isText()) {
            result += part->asText();
        } else {
            formatPatternExpression(arguments, part->contents(), status, result);
        }
    }
}

// See https://github.com/unicode-org/message-format-wg/blob/main/spec/formatting.md#resolve-selectors
void MessageFormatter::resolveSelectors(const Hashtable& arguments, const ExpressionList& selectors, UErrorCode &status, UVector& res) const {
    CHECK_ERROR(status);

    // 1. Let res be a new empty list of resolved values that support selection.
    // (Implicit, since `res` is an out-parameter)
    // 2. For each expression exp of the message's selectors
    LocalPointer<ResolvedExpression> rv;
    for (size_t i = 0; i < selectors.length(); i++) {
        // 2i. Let rv be the resolved value of exp.
        rv.adoptInstead(formatSelectorExpression(arguments, *selectors.get(i), status));
        // 2ii. If selection is supported for rv:
        // (Always true, since here, selector functions are strings)
        // 2ii(a). Append rv as the last element of the list res.
        res.addElement(rv.orphan(), status);
    }
}

// See https://github.com/unicode-org/message-format-wg/blob/main/spec/formatting.md#resolve-preferences
// `keys` and `matches` are both vectors of strings
void MessageFormatter::matchSelectorKeys(const ResolvedExpression& rv, const UVector& keys, UErrorCode& status, UVector& matches) const {
    CHECK_ERROR(status);

    for (size_t i = 0; ((int32_t) i) < keys.size(); i++) {
        UnicodeString* k = ((UnicodeString*) keys[i]);
        U_ASSERT(rv.selectorFunction.isValid());
        const Selector& selectorImpl = *rv.selectorFunction;
        if (selectorImpl.matches(rv.resolvedOperand, *k, *(rv.resolvedOptions), status)) {
            CHECK_ERROR(status);
            // `matches` does not adopt its elements
            matches.addElement(k, status);
        }
    }
}

// Compare strings by value
UBool stringsEqual(const UElement e1, const UElement e2) {
    return (*((UnicodeString*) e1.pointer) == *((UnicodeString*) e2.pointer));
}

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
                LocalPointer<UnicodeString> ks(new UnicodeString(""));
                if (!ks.isValid()) {
                    status = U_MEMORY_ALLOCATION_ERROR;
                    return;
                }
                formatLiteral(key.asLiteral(), *ks);
                // 2ii(b)(c) Append `ks` as the last element of the list `keys`.
                keys->addElement(ks.orphan(), status);
            }
        }
        // 2iii. Let `rv` be the resolved value at index `i` of `res`.
        const ResolvedExpression& rv = *((ResolvedExpression*) res[i]);
        // 2iv. Let matches be the result of calling the method MatchSelectorKeys(rv, keys)
        matches.adoptInstead(new UVector(status));
        CHECK_ERROR(status);
        // Set comparator; `matches` is a vector of strings and we want to be able to search it
        matches->setComparer(stringsEqual);
        // `matches` does not adopt its elements
        matchSelectorKeys(rv, *keys, status, *matches);
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
            UnicodeString ks;
            formatLiteral(key.asLiteral(), ks);
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
                UnicodeString ks;
                formatLiteral(key.asLiteral(), ks);
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

void MessageFormatter::formatSelectors(const Hashtable& arguments, const ExpressionList& selectors, const VariantMap& variants, UErrorCode &status, UnicodeString& result) const {
    CHECK_ERROR(status);

    // See https://github.com/unicode-org/message-format-wg/blob/main/spec/formatting.md#pattern-selection

    // Resolve Selectors
    // res is a vector of ResolvedExpressions
    LocalPointer<UVector> res(new UVector(status));
    CHECK_ERROR(status);
//    res->setDeleter(uprv_deleteUObject);
    resolveSelectors(arguments, selectors, status, *res);

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
    formatPattern(arguments, pat, status, result);
}

static bool isLocal(const UVector& localEnv, const VariableName& v) {
    for (size_t i = 0; ((int32_t) i) < localEnv.size(); i++) {
        const void* definition = localEnv[i];
        U_ASSERT(definition != nullptr);
        if (*((VariableName*) definition) == v) {
            return true;
        }
    }
    return false;
}

void MessageFormatter::check(const Hashtable& globalEnv, const UVector& localEnv, const OptionMap& options, UErrorCode &status) const {
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
        check(globalEnv, localEnv, *rhs, status);
    }
}

void MessageFormatter::check(const Hashtable& globalEnv, const UVector& localEnv, const Operand& rand, UErrorCode &status) const {
    CHECK_ERROR(status);

    // Nothing to check for literals
    if (rand.isLiteral()) {
        return;
    }

    // Check that variable is in scope
    const VariableName& var = rand.asVariable();
    // Check local scope
    if (isLocal(localEnv, var)) {
        return;
    }
    // Check global scope
    if (globalEnv.containsKey(var)) {
        return;
    }
    status = U_UNRESOLVED_VARIABLE;
}

void MessageFormatter::check(const Hashtable& globalEnv, const UVector& localEnv, const Expression& expr, UErrorCode &status) const {
    CHECK_ERROR(status);

    // Check for unresolved variable errors
    if (expr.isFunctionCall()) {
        const Operator& rator = expr.getOperator();
        if (!expr.isStandaloneAnnotation()) {
            const Operand& rand = expr.getOperand();
            check(globalEnv, localEnv, rand, status);
        }
        check(globalEnv, localEnv, rator.getOptions(), status);
    }

    if (!expr.isReserved()) {
        // Check the operand
        const Operand& rand = expr.getOperand();
        check(globalEnv, localEnv, rand, status);
    }
}

// Check for resolution errors
void MessageFormatter::checkDeclarations(const Hashtable& arguments, UErrorCode &status) const {
    CHECK_ERROR(status);

    const Bindings& decls = dataModel->getLocalVariables();

    // Create an empty environment
    LocalPointer<UVector> localEnv(new UVector(status));
    CHECK_ERROR(status);
    localEnv->setDeleter(uprv_deleteUObject);
    for (size_t i = 0; i < decls.length(); i++) {
        const Binding* decl = decls.get(i);
        U_ASSERT(decl != nullptr);
        const Expression* rhs = decl->getValue();
        check(arguments, *localEnv, *rhs, status);
        // Add the LHS to the environment for checking the next declaration
        LocalPointer<UnicodeString> definition(new UnicodeString(decl->var));
        if (!definition.isValid()) {
            status = U_MEMORY_ALLOCATION_ERROR;
            return;
        }
        localEnv->adoptElement(definition.orphan(), status);
        CHECK_ERROR(status);
    }
}

void MessageFormatter::formatToString(const Hashtable& arguments, UErrorCode &status, UnicodeString& result) const {
    CHECK_ERROR(status);

    // Note: we currently evaluate variables lazily,
    // without memoization. This call is still necessary
    // to check out-of-scope uses of local variables in
    // right-hand sides (unresolved variable errors can
    // only be checked when arguments are known)
    checkDeclarations(arguments, status);

    if (!dataModel->hasSelectors()) {
        formatPattern(arguments, dataModel->getPattern(), status, result);
    } else {
        formatSelectors(arguments, dataModel->getSelectors(), dataModel->getVariants(), status, result);
    }
    return;
}

} // namespace message2
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

