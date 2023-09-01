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
using SelectorKeys    = MessageFormatDataModel::SelectorKeys;
using VariantMap      = MessageFormatDataModel::VariantMap;

using PrioritizedVariantList = ImmutableVector<PrioritizedVariant>;

#define TEXT_SELECTOR UnicodeString("select")

// ------------------------------------------------------
// Formatting

// The result of formatting a literal is just itself.
static const Formattable& evalLiteral(const Literal& lit) {
    return lit.getContents();
}

// Assumes that `var` is a message argument; sets the input in the context
// to the argument's value.
void MessageFormatter::evalArgument(const VariableName& var, ExpressionContext& context) const {
    U_ASSERT(context.hasGlobal(var));
    // The fallback for a variable name is itself.
    context.setFallback(var);
    if (context.hasGlobalAsFormattable(var)) {
        context.setInput(context.getGlobalAsFormattable(var));
    } else {
        context.setInput(context.getGlobalAsObject(var));
    }
}

// Sets the input to the contents of the literal
void MessageFormatter::formatLiteral(const Literal& lit, ExpressionContext& context) const {
    // The fallback for a literal is itself.
    context.setFallback(lit);
    context.setInput(evalLiteral(lit));
}

void MessageFormatter::formatOperand(const Environment& env, const Operand& rand, ExpressionContext& context, UErrorCode &status) const {
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
        const VariableName& var = rand.asVariable();
        // TODO: Currently, this code implements lazy evaluation of locals.
        // That is, the environment binds names to a closure, not a resolved value.
        // Eager vs. lazy evaluation is an open issue:
        // see https://github.com/unicode-org/message-format-wg/issues/299

        // Look up the variable in the environment
        const Closure* rhs = env.lookup(var);
        // If rhs is null, the variable must not be a local
        if (rhs != nullptr) {
            // Format the expression using the environment from the closure
            formatExpression(rhs->getEnv(), rhs->getExpr(), context, status);
            return;
        }
        // Use fallback per
        // https://github.com/unicode-org/message-format-wg/blob/main/spec/formatting.md#fallback-resolution
        context.setFallback(var);
        // Variable wasn't found in locals -- check if it's global
        if (context.hasGlobal(var)) {
            evalArgument(var, context);
            return;
        } else {
            // Unbound variable -- set a resolution error
            context.messageContext().getErrors().setUnresolvedVariable(var, status);
            return;
        }
    } else if (rand.isLiteral()) {
        formatLiteral(rand.asLiteral(), context);
        return;
    }
}

// Resolves a function's options, recording the value of each option in the context
void MessageFormatter::resolveOptions(const Environment& env, const OptionMap& options, ExpressionContext& context, UErrorCode& status) const {
    CHECK_ERROR(status);

    int32_t pos = OptionMap::FIRST;
    LocalPointer<ExpressionContext> rhsContext;
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
        // Format the operand in its own context
        formatOperand(env, *v, *rhsContext, status);
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
            context.setObjectOption(k, rhsContext->getObjectInputPointer(), status);
        } else {
            // Ignore fallbacks
            U_ASSERT(rhsContext->isFallback());
        }
    }
}

// Formats an expression using `globalEnv` for the values of variables
void MessageFormatter::formatExpression(const Environment& globalEnv, const Expression& expr, ExpressionContext& context, UErrorCode &status) const {
    CHECK_ERROR(status);

    // Formatting error
    if (expr.isReserved()) {
        context.messageContext().getErrors().setReservedError(status);
        U_ASSERT(context.isFallback());
        return;
    }

    const Operand& rand = expr.getOperand();
    // Format the operand (formatOperand handles the case of a null operand)
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

        // Call the formatter function
        context.evalFormatterCall(functionName, status);
        // If the call was successful, nothing more to do
        if (context.hasOutput() && U_SUCCESS(status)) {
            return;
        } else if (!(context.messageContext().getErrors().hasError())) {
            // Set formatting warning if formatting function had no output
            // but didn't set an error or warning
            context.messageContext().getErrors().setFormattingError(functionName.name(), status);
        }

        // If we reached this point, the formatter is null --
        // must have been a previous unknown function warning
        if (rand.isNull()) {
            context.setFallback(functionName);
        }
        context.setFallback();
        return;
    }
}

// Formats each text and expression part of a pattern, appending the results to `result`
void MessageFormatter::formatPattern(MessageContext& globalContext, const Environment& globalEnv, const Pattern& pat, UErrorCode &status, UnicodeString& result) const {
    CHECK_ERROR(status);

    LocalPointer<ExpressionContext> context;
    for (int32_t i = 0; i < pat.numParts(); i++) {
        const PatternPart* part = pat.getPart(i);
        U_ASSERT(part != nullptr);
        if (part->isText()) {
            result += part->asText();
        } else {
            // Create a new context to evaluate the expression part
            context.adoptInstead(ExpressionContext::create(globalContext, status));
            CHECK_ERROR(status);
            // Format the expression
            formatExpression(globalEnv, part->contents(), *context, status);
            // Force full evaluation, e.g. applying default formatters to
            // unformatted input (or formatting numbers as strings)
            context->formatToString(locale, status);
            CHECK_ERROR(status);
            result += context->getStringOutput();
        }
    }
}

// ------------------------------------------------------
// Selection

// See https://github.com/unicode-org/message-format-wg/blob/main/spec/formatting.md#resolve-selectors
void MessageFormatter::resolveSelectors(MessageContext& context, const Environment& env, const ExpressionList& selectors, UErrorCode &status, ExpressionContext** res/*[]*/) const {
    CHECK_ERROR(status);

    // 1. Let res be a new empty list of resolved values that support selection.
    // (Implicit, since `res` is an out-parameter)
    // 2. For each expression exp of the message's selectors
    LocalPointer<ExpressionContext> rv;
    for (int32_t i = 0; i < selectors.length(); i++) {
        rv.adoptInstead(ExpressionContext::create(context, status));
        CHECK_ERROR(status);
        // 2i. Let rv be the resolved value of exp.
        formatSelectorExpression(env, *selectors.get(i), *rv, status);
        if (rv->hasSelector()) {
            // 2ii. If selection is supported for rv:
            // (True if this code has been reached)
        } else {
            // 2iii. Else:
            // Let nomatch be a resolved value for which selection always fails.
            // Append nomatch as the last element of the list res.
            // Emit a Selection Error.
            // (Note: in this case, rv, being a fallback, serves as `nomatch`)
            const Errors& err = rv->messageContext().getErrors();
            U_ASSERT(err.hasUnknownFunctionError() || err.hasSelectorError());
            U_ASSERT(rv->isFallback());
        }
        // 2ii(a). Append rv as the last element of the list res.
        // (Also fulfills 2iii)
        res[i] = rv.orphan();
    }
}

// See https://github.com/unicode-org/message-format-wg/blob/main/spec/formatting.md#resolve-preferences
void MessageFormatter::matchSelectorKeys(UnicodeString* keys/*[]*/, int32_t numKeys, ExpressionContext& rv, UErrorCode& status, UnicodeString* matches/*[]*/, int32_t& numberMatching) const {
    CHECK_ERROR(status);

    numberMatching = 0;
    if (rv.isFallback()) {
        // Return an empty list of matches
        return;
    }
    U_ASSERT(rv.hasSelector());

    rv.evalPendingSelectorCall(keys, numKeys, matches, numberMatching, status);
}

// See https://github.com/unicode-org/message-format-wg/blob/main/spec/formatting.md#resolve-preferences
// `res` is an array of arrays of ExpressionContexts, with length `numSelectors;
// `pref` is an array of arrays of strings; `prefsLengths` is an array of the length of each element of `pref`
void MessageFormatter::resolvePreferences(ExpressionContext** res/*[]*/, int32_t numSelectors, const VariantMap& variants, UErrorCode &status, UnicodeString** pref/*[]*/, int32_t* prefsLengths/*[]*/) const {
    CHECK_ERROR(status);

    // 1. Let pref be a new empty list of lists of strings.
    // (Implicit, since `pref` is an out-parameter)
    LocalArray<UnicodeString> keys;
    LocalArray<UnicodeString> matches(new UnicodeString[variants.size()]);
    int32_t numVariants = variants.size();
    // 2. For each index i in res
    for (int32_t i = 0; i < numSelectors; i++) {
        // 2i. Let keys be a new empty list of strings.
        keys.adoptInstead(new UnicodeString[numVariants]);
        int32_t keysLen = 0;
        CHECK_ERROR(status);
        // 2ii. For each variant `var` of the message
        int32_t pos = VariantMap::FIRST;
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
                keys[keysLen] = ks;
                keysLen++;
            }
        }
        // 2iii. Let `rv` be the resolved value at index `i` of `res`.
        ExpressionContext* rv = res[i];
        U_ASSERT(rv != nullptr);
        // 2iv. Let matches be the result of calling the method MatchSelectorKeys(rv, keys)
        int32_t numMatches;
        matchSelectorKeys(keys.getAlias(), keysLen, *rv, status, matches.getAlias(), numMatches);
        // 2v. Append `matches` as the last element of the list `pref`
        pref[i] = matches.orphan();
        prefsLengths[i] = numMatches;
        matches.adoptInstead(new UnicodeString[variants.size()]);
    }
}

static int32_t indexOf(const UnicodeString* matches/*[]*/, int32_t matchesLen, const UnicodeString& k) {
    for (int32_t i = 0; i < matchesLen; i++) {
        if (matches[i] == k) {
            return i;
        }
    }
    return -1;
}

static bool contains(const UnicodeString* matches/*[]*/, int32_t matchesLen, const UnicodeString& k) {
    return (indexOf(matches, matchesLen, k) != -1);
}

// See https://github.com/unicode-org/message-format-wg/blob/main/spec/formatting.md#filter-variants
// `pref` is an array of arrays of strings; `vars` is an array of PrioritizedVariants
void filterVariants(const VariantMap& variants, /* const */ UnicodeString** pref/*[]*/, int32_t prefLen, const int32_t* prefsLengths, UErrorCode &status, PrioritizedVariant** vars/*[]*/, int32_t& varsLen) {
    CHECK_ERROR(status);

    varsLen = 0;
    // 1. Let `vars` be a new empty list of variants.
    // (Not needed since `vars` is an out-parameter)
    // 2. For each variant `var` of the message:
    int32_t pos = VariantMap::FIRST;
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
        for (int32_t i = 0; i < prefLen; i++) {
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
            const UnicodeString* matches = pref[i];
            // 2i(f). If `matches` includes `ks`
            if (contains(matches, prefsLengths[i], ks)) {
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
            vars[varsLen] = tuple.orphan();
            varsLen++;
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
static void sortVariantTuples(PrioritizedVariant** sortable/*[]*/, int32_t count, UErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    uprv_sortArray(sortable, count, sizeof(PrioritizedVariant*), comparePrioritizedVariants, nullptr, true, &errorCode);
}

// See https://github.com/unicode-org/message-format-wg/blob/main/spec/formatting.md#sort-variants
// Leaves the preferred variant as element 0 in `sortable`
// Note: this sorts in-place, so `sortable` is just `vars`
// `pref` is a vector of vectors of strings; `vars` is a vector of PrioritizedVariants
void sortVariants(/* const */ UnicodeString** pref, int32_t prefsLen, int32_t* prefsLengths, UErrorCode& status, PrioritizedVariant** vars/*[]*/, int32_t varsLen) {
    CHECK_ERROR(status);

// Note: steps 1 and 2 are omitted since we use `vars` as `sortable` (we sort in-place)
    // 1. Let `sortable` be a new empty list of (integer, variant) tuples.
    // (Not needed since `sortable` is an out-parameter)
    // 2. For each variant `var` of `vars`
    // 2i. Let tuple be a new tuple (-1, var).
    // 2ii. Append `tuple` as the last element of the list `sortable`.

    // 3. Let `len` be the integer count of items in `pref`.
    int32_t len = prefsLen;
    // 4. Let `i` be `len` - 1.
    int32_t i = len - 1;
    // 5. While i >= 0:
    while (i >= 0) {
        // 5i. Let `matches` be the list of strings at index `i` of `pref`.
        const UnicodeString* matches = pref[i];
        // 5ii. Let `minpref` be the integer count of items in `matches`.
        int32_t minpref = prefsLengths[i];
        // 5iii. For each tuple `tuple` of `sortable`:
        for (int32_t j = 0; j < varsLen; j++) {
            PrioritizedVariant* tuple = vars[j];
            // 5iii(a). Let matchpref be an integer with the value minpref.
            int32_t matchpref = minpref;
            // 5iii(b). Let `key` be the tuple variant key at position `i`.
            const KeyList& tupleVariantKeys = tuple->keys.getKeys();
            U_ASSERT(i < ((int32_t) tupleVariantKeys.length())); // Given by earlier semantic checking
            const Key& key = *tupleVariantKeys.get(((int32_t) i));
            // 5iii(c) If `key` is not the catch-all key '*':
            if (!key.isWildcard()) {
                // 5iii(c)(a). Assert that `key` is a literal.
                // (Not needed)
                // 5iii(c)(b). Let `ks` be the resolved value of `key`.
                UnicodeString ks = key.asLiteral().stringContents();
                // 5iii(c)(c) Let matchpref be the integer position of ks in `matches`.
                matchpref = indexOf(matches, prefsLengths[i], ks);
                U_ASSERT(matchpref != -1);
            }
            // 5iii(d) Set the `tuple` integer value as matchpref.
            tuple->priority = matchpref;
        }
        // 5iv. Set `sortable` to be the result of calling the method SortVariants(`sortable`)
        sortVariantTuples(vars, varsLen, status);
        // 5v. Set `i` to be `i` - 1.
        i--;
    }
    // The caller is responsible for steps 6 and 7
    // 6. Let `var` be the `variant` element of the first element of `sortable`.
    // 7. Select the pattern of `var`
}


// Evaluate the operand
void MessageFormatter::resolveVariables(const Environment& env, const Operand& rand, ExpressionContext& context, UErrorCode &status) const {
    CHECK_ERROR(status);

    if (rand.isNull()) {
        // Nothing to do
        return;
    } else if (rand.isLiteral()) {
        // If there's already a function name set, this shouldn't have been evaluated
        U_ASSERT(!context.hasFunctionName());
        formatLiteral(rand.asLiteral(), context);
    } else {
        // Must be variable
        const VariableName& var = rand.asVariable();
        // Resolve the variable
        const Closure* referent = env.lookup(var);
        if (referent != nullptr) {
            // Resolve the referent
            resolveVariables(referent->getEnv(), referent->getExpr(), context, status);
            return;
        }
        // Either this is a global var or an unbound var --
        // either way, it can't be bound to a function call.
        context.setFallback(var);
        // Check globals
        if (context.hasGlobal(var)) {
            evalArgument(var, context);
        } else {
            // Unresolved variable -- could be a previous warning. Nothing to resolve
            U_ASSERT(context.messageContext().getErrors().hasUnresolvedVariableError());
        }
    }
}

// Evaluate the expression except for not performing the top-level function call
// (which is expected to be a selector, but may not be, in error cases)
void MessageFormatter::resolveVariables(const Environment& env, const Expression& expr, ExpressionContext& context, UErrorCode &status) const {
    CHECK_ERROR(status);

    // A `reserved` is an error
    if (expr.isReserved()) {
        context.messageContext().getErrors().setReservedError(status);
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
void MessageFormatter::formatSelectorExpression(const Environment& globalEnv, const Expression& expr, ExpressionContext& context, UErrorCode &status) const {
    CHECK_ERROR(status);

    // Resolve expression to determine if it's a function call
    resolveVariables(globalEnv, expr, context, status);

    Errors& err = context.messageContext().getErrors();

    // If there is a selector, then `resolveVariables()` recorded it in the context
    if (context.hasSelector()) {
        // Check if there was an error
        if (context.isFallback()) {
            // Use a null expression if it's a syntax or data model warning;
            // create a valid (non-fallback) formatted placeholder from the
            // fallback string otherwise
            if (err.hasSyntaxError() || err.hasDataModelError()) {
                U_ASSERT(!context.hasInput());
            } else {
                context.promoteFallbackToInput();
            }
        }
    } else {
        // Determine the type of error to set
        if (context.hasFunctionName()) {
            const FunctionName& fn = context.getFunctionName();
            // A selector used as a formatter is a selector error
            if (context.hasFormatter()) {
                err.setSelectorError(fn, status);
            } else {
                // Otherwise, the error is an unknown function error
                err.setUnknownFunction(fn, status);
            }
        } else {
            // No function name -- this is a missing selector annotation error
            err.setMissingSelectorAnnotation(status);
        }
        context.clearFunctionName();
        context.clearFunctionOptions();
        context.setFallback();
    }
}

void MessageFormatter::formatSelectors(MessageContext& context, const Environment& env, const ExpressionList& selectors, const VariantMap& variants, UErrorCode &status, UnicodeString& result) const {
    CHECK_ERROR(status);

    // See https://github.com/unicode-org/message-format-wg/blob/main/spec/formatting.md#pattern-selection

    // Resolve Selectors
    // res is a vector of ResolvedExpressions
    int32_t numSelectors = selectors.length();
    LocalArray<ExpressionContext*> res(new ExpressionContext*[numSelectors]);
    if (!res.isValid()) {
        status = U_MEMORY_ALLOCATION_ERROR;
        return;
    }
    resolveSelectors(context, env, selectors, status, res.getAlias());

    // Resolve Preferences
    // pref is an array of arrays of strings
    LocalArray<UnicodeString*> pref(new UnicodeString*[numSelectors]);
    LocalArray<int32_t> prefsLengths(new int32_t[numSelectors]);
    CHECK_ERROR(status);
    resolvePreferences(res.getAlias(), numSelectors, variants, status, pref.getAlias(), prefsLengths.getAlias());

    // Filter Variants
    // vars is a vector of PrioritizedVariants
    LocalArray<PrioritizedVariant*> vars(new PrioritizedVariant*[variants.size()]);
    if (!vars.isValid()) {
        status = U_MEMORY_ALLOCATION_ERROR;
        return;
    }
    int32_t varsLen;
    filterVariants(variants, pref.getAlias(), numSelectors, prefsLengths.getAlias(), status, vars.getAlias(), varsLen);

    // Sort Variants and select the final pattern
    // Note: `sortable` in the spec is just `vars` here,
    // which is sorted in-place
    sortVariants(pref.getAlias(), numSelectors, prefsLengths.getAlias(), status, vars.getAlias(), varsLen);
    CHECK_ERROR(status); // needs to be checked to ensure that `sortable` is valid

    // 6. Let `var` be the `variant` element of the first element of `sortable`.
    U_ASSERT(varsLen > 0); // This should have been checked earlier (having 0 variants would be a data model error)
    const PrioritizedVariant& var = *(vars[0]);
    // 7. Select the pattern of `var`
    const Pattern& pat = var.pat;

    // Format the pattern
    formatPattern(context, env, pat, status, result);
}


void MessageFormatter::formatToString(const MessageArguments& arguments, UErrorCode &status, UnicodeString& result) const {
    CHECK_ERROR(status);

    // Create a new context with the given arguments and the `errors` structure
    LocalPointer<MessageContext> context(MessageContext::create(*this, arguments, *errors, status));
    CHECK_ERROR(status);

    const MessageFormatDataModel& dataModel = getDataModel();

    // Note: we currently evaluate variables lazily,
    // without memoization. This call is still necessary
    // to check out-of-scope uses of local variables in
    // right-hand sides (unresolved variable errors can
    // only be checked when arguments are known)

    // Check for resolution errors
    Checker(dataModel, context->getErrors()).check(status);

    // Create a new environment that will store closures for all local variables
    Environment* env = Environment::create(status);
    CHECK_ERROR(status);

    // Check for unresolved variable errors
    checkDeclarations(*context, env, status);
    CHECK_ERROR(status);
    LocalPointer<Environment> globalEnv(env);

    if (!dataModel.hasSelectors()) {
        formatPattern(*context, *globalEnv, dataModel.getPattern(), status, result);
    } else {
        // Check for errors/warnings -- if so, then the result of pattern selection is the fallback value
        // See https://github.com/unicode-org/message-format-wg/blob/main/spec/formatting.md#pattern-selection
        Errors& err = context->getErrors();
        if (err.hasSyntaxError() || err.hasDataModelError()) {
            result += REPLACEMENT;
        } else {
            formatSelectors(*context, *globalEnv, dataModel.getSelectors(), dataModel.getVariants(), status, result);
        }
    }
    // Update status according to all errors seen while formatting
    context->checkErrors(status);
    // Clear resolution and formatting errors, in case this MessageFormatter object
    // is used again with different arguments
    clearErrors();
    return;
}

void MessageFormatter::clearErrors() const {
    errors->clearResolutionAndFormattingErrors();
}

// ----------------------------------------
// Checking for resolution errors

void MessageFormatter::check(MessageContext& context, const Environment& localEnv, const OptionMap& options, UErrorCode &status) const {
    CHECK_ERROR(status);

    // Check the RHS of each option
    int32_t pos = OptionMap::FIRST;
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

void MessageFormatter::check(MessageContext& context, const Environment& localEnv, const Operand& rand, UErrorCode &status) const {
    CHECK_ERROR(status);

    // Nothing to check for literals
    if (rand.isLiteral() || rand.isNull()) {
        return;
    }

    // Check that variable is in scope
    const VariableName& var = rand.asVariable();
    // Check local scope
    if (localEnv.lookup(var) != nullptr) {
        return;
    }
    // Check global scope
    if (context.hasVar(var)) {
        return;
    }
    context.getErrors().setUnresolvedVariable(var, status);
}

void MessageFormatter::check(MessageContext& context, const Environment& localEnv, const Expression& expr, UErrorCode &status) const {
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
void MessageFormatter::checkDeclarations(MessageContext& context, Environment*& env, UErrorCode &status) const {
    CHECK_ERROR(status);

    const Bindings& decls = getDataModel().getLocalVariables();
    U_ASSERT(env != nullptr);

    for (int32_t i = 0; i < decls.length(); i++) {
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
