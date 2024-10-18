// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#if !UCONFIG_NO_MF2

#include "unicode/messageformat2_arguments.h"
#include "unicode/messageformat2_data_model.h"
#include "unicode/messageformat2_formattable.h"
#include "unicode/messageformat2.h"
#include "unicode/ubidi.h"
#include "unicode/unistr.h"
#include "messageformat2_allocation.h"
#include "messageformat2_evaluation.h"
#include "messageformat2_macros.h"


U_NAMESPACE_BEGIN

namespace message2 {

using namespace data_model;

// ------------------------------------------------------
// Formatting

// Assumes that `var` is a message argument; returns the argument's value.
[[nodiscard]] InternalValue MessageFormatter::evalArgument(const VariableName& var,
                                                           MessageContext& context,
                                                           UErrorCode& errorCode) const {
    if (U_SUCCESS(errorCode)) {
        // The fallback for a variable name is itself.
        UnicodeString str(DOLLAR);
        str += var;
        // Look up the variable in the global environment
        const Formattable* val = context.getGlobal(var, errorCode);
        if (U_SUCCESS(errorCode)) {
            // If it exists, create a BaseValue (FunctionValue) for it
            LocalPointer<BaseValue> result(BaseValue::create(locale, *val, errorCode));
            // Add fallback and return an InternalValue
            if (U_SUCCESS(errorCode)) {
                return InternalValue(result.orphan(), str);
            }
        }
    }
    return {};
}

// Returns the contents of the literal
[[nodiscard]] InternalValue MessageFormatter::evalLiteral(const Literal& lit,
                                                          UErrorCode& errorCode) const {
    // Create a BaseValue (FunctionValue) that wraps the literal
    LocalPointer<BaseValue> val(BaseValue::create(locale,
                                                  Formattable(lit.unquoted()),
                                                  errorCode));
    if (U_SUCCESS(errorCode)) {
        // The fallback for a literal is itself.
        return InternalValue(val.orphan(), lit.quoted());
    }
    return {};
}

[[nodiscard]] InternalValue MessageFormatter::evalOperand(const Environment& env,
                                                          const Operand& rand,
                                                          MessageContext& context,
                                                          UErrorCode &status) const {
    if (U_FAILURE(status)) {
        return {};
    }

    // Three cases: absent operand; variable; or literal

    // Absent (null) operand
    if (rand.isNull()) {
        return InternalValue::null(status);
    }
    // Variable reference
    if (rand.isVariable()) {
        // Check if it's local or global
        // Note: there is no name shadowing; this is enforced by the parser
        const VariableName& var = rand.asVariable();
        // Currently, this code implements lazy evaluation of locals.
        // That is, the environment binds names to a closure, not a resolved value.
        // The spec does not require either eager or lazy evaluation.

        // Look up the variable in the environment
        if (env.has(var)) {
          // `var` is a local -- look it up
          const Closure& rhs = env.lookup(var);
          // Evaluate the expression using the environment from the closure
          return evalExpression(rhs.getEnv(), rhs.getExpr(), context, status);
        }
        // Variable wasn't found in locals -- check if it's global
        InternalValue result = evalArgument(var, context, status);
        if (status == U_ILLEGAL_ARGUMENT_ERROR) {
            status = U_ZERO_ERROR;
            // Unbound variable -- set a resolution error
            context.getErrors().setUnresolvedVariable(var, status);
            // Use fallback per
            // https://github.com/unicode-org/message-format-wg/blob/main/spec/formatting.md#fallback-resolution
            UnicodeString str(DOLLAR);
            str += var;
            return InternalValue::fallback(str);
        }
        // Looking up the global variable succeeded; return it
        return result;
    }
    // Literal
    else {
        U_ASSERT(rand.isLiteral());
        return evalLiteral(rand.asLiteral(), status);
    }
}

// Resolves a function's options
FunctionOptions MessageFormatter::resolveOptions(const Environment& env,
                                                 const OptionMap& options,
                                                 MessageContext& context,
                                                 UErrorCode& status) const {
    // Create a vector of options
    LocalPointer<UVector> optionsVector(createUVector(status));
    if (U_FAILURE(status)) {
        return {};
    }
    LocalPointer<ResolvedFunctionOption> resolvedOpt;
    // For each option...
    for (int i = 0; i < options.size(); i++) {
        const Option& opt = options.getOption(i, status);
        if (U_FAILURE(status)) {
            return {};
        }
        const UnicodeString& k = opt.getName();
        const Operand& v = opt.getValue();

        // ...evaluate its right-hand side...
        InternalValue rhsVal = evalOperand(env, v, context, status);
        if (U_FAILURE(status)) {
            return {};
        }
        // ...giving a FunctionValue.
        FunctionValue* optVal = rhsVal.takeValue(status);

        // The option is resolved; add it to the vector
        ResolvedFunctionOption resolvedOpt(k, optVal);
        LocalPointer<ResolvedFunctionOption>
            p(create<ResolvedFunctionOption>(std::move(resolvedOpt), status));
        EMPTY_ON_ERROR(status);
        optionsVector->adoptElement(p.orphan(), status);
    }
    // Return a new FunctionOptions constructed from the vector of options
    return FunctionOptions(std::move(*optionsVector), status);
}

static UBiDiDirection getBiDiDirection(const Locale& locale,
                                       const UnicodeString& s) {
    if (s.isEmpty()) {
        return locale.isRightToLeft() ? UBIDI_RTL : UBIDI_LTR;
    }
    if (s == u"ltr") {
        return UBIDI_LTR;
    }
    if (s == u"rtl") {
        return UBIDI_RTL;
    }
    if (s == u"auto") {
        return UBIDI_MIXED;
    }
    return UBIDI_NEUTRAL;
}

FunctionContext MessageFormatter::makeFunctionContext(const FunctionOptions& options) const {
    // Look up "u:locale", "u:dir", and "u:id" in the options
    UnicodeString localeStr = options.getStringFunctionOption(UnicodeString("u:locale"));

    // Use default locale from context, unless "u:locale" is provided
    Locale localeToUse;
    if (localeStr.isEmpty()) {
        localeToUse = locale;
    } else {
        UErrorCode localStatus = U_ZERO_ERROR;
        int32_t len = localeStr.length();
        char* buf = static_cast<char*>(uprv_malloc(len + 1));
        localeStr.extract(0, len, buf, len);
        Locale l = Locale::forLanguageTag(StringPiece(buf, len), localStatus);
        uprv_free(buf);
        if (U_SUCCESS(localStatus)) {
            localeToUse = l;
        } else {
            localeToUse = locale;
        }
    }
    UBiDiDirection dir = getBiDiDirection(localeToUse,
                                          options.getStringFunctionOption(UnicodeString("u:dir")));
    UnicodeString id = options.getStringFunctionOption(UnicodeString("u:id"));

    return FunctionContext(localeToUse, dir, id);
}

// Looks up `functionName` and applies it to an operand and options,
// handling errors if the function is unbound
[[nodiscard]] InternalValue MessageFormatter::apply(const FunctionName& functionName,
                                                    InternalValue&& rand,
                                                    FunctionOptions&& options,
                                                    MessageContext& context,
                                                    UErrorCode& status) const {
    EMPTY_ON_ERROR(status);

    UnicodeString fallbackStr;

    // Create the fallback string for this function call
    if (rand.isNullOperand()) {
        fallbackStr = UnicodeString(COLON);
        fallbackStr += functionName;
    } else {
        fallbackStr = rand.asFallback();
    }

    // Look up the function name
    Function* function = lookupFunction(functionName, status);
    if (U_FAILURE(status)) {
        // Function is unknown -- set error and use the fallback value
        status = U_ZERO_ERROR;
        context.getErrors().setUnknownFunction(functionName, status);
        return InternalValue::fallback(fallbackStr);
    }
    // Value is not a fallback, so we can safely call takeValue()
    LocalPointer<FunctionValue> functionArg(rand.takeValue(status));
    U_ASSERT(U_SUCCESS(status));
    // Call the function
    LocalPointer<FunctionValue>
        functionResult(function->call(makeFunctionContext(options),
                                      *functionArg,
                                      std::move(options),
                                      status));
    // Handle any errors signaled by the function
    // (and use the fallback value)
    if (status == U_MF_OPERAND_MISMATCH_ERROR) {
        status = U_ZERO_ERROR;
        context.getErrors().setOperandMismatchError(functionName, status);
        return InternalValue::fallback(fallbackStr);
    }
    if (status == U_MF_FORMATTING_ERROR) {
        status = U_ZERO_ERROR;
        context.getErrors().setFormattingError(functionName, status);
        return InternalValue::fallback(fallbackStr);
    }
    if (U_FAILURE(status)) {
        return {};
    }
    // Success; return the result
    return InternalValue(functionResult.orphan(), fallbackStr);
}

// Evaluates an expression using `globalEnv` for the values of variables
[[nodiscard]] InternalValue MessageFormatter::evalExpression(const Environment& globalEnv,
                                                             const Expression& expr,
                                                             MessageContext& context,
                                                             UErrorCode &status) const {
    if (U_FAILURE(status)) {
        return {};
    }

    const Operand& rand = expr.getOperand();
    // Evaluate the operand (evalOperand handles the case of a null operand)
    InternalValue randVal = evalOperand(globalEnv, rand, context, status);

    // If there's no function, we check for an implicit formatter
    if (!expr.isFunctionCall()) {
        const FunctionValue* contained = randVal.getValue(status);
        if (U_FAILURE(status)) {
            // Fallback or null -- no implicit formatter
            status = U_ZERO_ERROR;
            return randVal;
        }
        const Formattable& toFormat = contained->getOperand();
        // If it has an object type, there might be an implicit formatter for it...
        switch (toFormat.getType()) {
        case UFMT_OBJECT: {
            const FormattableObject* obj = toFormat.getObject(status);
            U_ASSERT(U_SUCCESS(status));
            U_ASSERT(obj != nullptr);
            const UnicodeString& type = obj->tag();
            FunctionName functionName;
            if (!getDefaultFormatterNameByType(type, functionName)) {
                // No formatter for this type -- follow default behavior
                return randVal;
            }
            // ... apply the implicit formatter
            return apply(functionName,
                         std::move(randVal),
                         FunctionOptions(),
                         context,
                         status);
        }
        default:
            // No formatters for other types, so just return the evaluated operand
            return randVal;
        }
    } else {
        // Don't call the function on error values
        if (randVal.isFallback()) {
            return randVal;
        }
        // Get the function name and options from the operator
        const Operator* rator = expr.getOperator(status);
        U_ASSERT(U_SUCCESS(status));
        const FunctionName& functionName = rator->getFunctionName();
        const OptionMap& options = rator->getOptionsInternal();
        // Resolve the options
        FunctionOptions resolvedOptions = resolveOptions(globalEnv, options, context, status);

        // Call the function with the operand and arguments
        return apply(functionName,
                     std::move(randVal), std::move(resolvedOptions), context, status);
    }
}

// Formats each text and expression part of a pattern, appending the results to `result`
void MessageFormatter::formatPattern(MessageContext& context,
                                     const Environment& globalEnv,
                                     const Pattern& pat,
                                     UErrorCode &status, UnicodeString& result) const {
    CHECK_ERROR(status);

    for (int32_t i = 0; i < pat.numParts(); i++) {
        const PatternPart& part = pat.getPart(i);
        if (part.isText()) {
            result += part.asText();
        } else if (part.isMarkup()) {
            // Markup is ignored
        } else {
	      // Format the expression
	      InternalValue partVal = evalExpression(globalEnv, part.contents(), context, status);
              if (partVal.isFallback()) {
                  result += LEFT_CURLY_BRACE;
                  result += partVal.asFallback();
                  result += RIGHT_CURLY_BRACE;
              } else {
                  // Do final formatting (e.g. formatting numbers as strings)
                  LocalPointer<FunctionValue> val(partVal.takeValue(status));
                  // Shouldn't be null or a fallback
                  U_ASSERT(U_SUCCESS(status));
                  result += val->formatToString(status);
                  // Handle formatting errors. `formatToString()` can't take a context and thus can't
                  // register an error directly
                  if (status == U_MF_FORMATTING_ERROR) {
                      status = U_ZERO_ERROR;
                      // TODO: The name of the formatter that failed is unavailable.
                      // Not ideal, but it's hard for `formatToString()`
                      // to pass along more detailed diagnostics
                      context.getErrors().setFormattingError(status);
                  }
              }
        }
    }
}

// ------------------------------------------------------
// Selection

// See https://github.com/unicode-org/message-format-wg/blob/main/spec/formatting.md#resolve-selectors
// `res` is a vector of ResolvedSelectors
void MessageFormatter::resolveSelectors(MessageContext& context, const Environment& env, UErrorCode &status, UVector& res) const {
    CHECK_ERROR(status);
    U_ASSERT(!dataModel.hasPattern());

    const Expression* selectors = dataModel.getSelectorsInternal();
    // 1. Let res be a new empty list of resolved values that support selection.
    // (Implicit, since `res` is an out-parameter)
    // 2. For each expression exp of the message's selectors
    for (int32_t i = 0; i < dataModel.numSelectors(); i++) {
        // 2i. Let rv be the resolved value of exp.
        InternalValue rv = evalExpression(env, selectors[i], context, status);
        if (rv.isSelectable()) {
            // 2ii. If selection is supported for rv:
            // (True if this code has been reached)
        } else {
            // 2iii. Else:
            // Let nomatch be a resolved value for which selection always fails.
            // Append nomatch as the last element of the list res.
            // Emit a Selection Error.
            // (Note: in this case, rv, being a fallback, serves as `nomatch`)
            context.getErrors().setSelectorError({}, status);
        }
        // 2ii(a). Append rv as the last element of the list res.
        // (Also fulfills 2iii)
        LocalPointer<InternalValue> v(create<InternalValue>(std::move(rv), status));
        CHECK_ERROR(status);
        res.adoptElement(v.orphan(), status);
    }
}

// See https://github.com/unicode-org/message-format-wg/blob/main/spec/formatting.md#resolve-preferences
// `keys` and `matches` are vectors of strings
void MessageFormatter::matchSelectorKeys(const UVector& keys,
                                         MessageContext& context,
					 InternalValue&& rv,
					 UVector& keysOut,
					 UErrorCode& status) const {
    CHECK_ERROR(status);

    if (!rv.isSelectable()) {
        return;
    }

    UErrorCode savedStatus = status;

    // Convert `keys` to an array
    int32_t keysLen = keys.size();
    UnicodeString* keysArr = new UnicodeString[keysLen];
    if (keysArr == nullptr) {
        status = U_MEMORY_ALLOCATION_ERROR;
        return;
    }
    for (int32_t i = 0; i < keysLen; i++) {
        const UnicodeString* k = static_cast<UnicodeString*>(keys[i]);
        U_ASSERT(k != nullptr);
        keysArr[i] = *k;
    }
    LocalArray<UnicodeString> adoptedKeys(keysArr);

    // Create an array to hold the output
    int32_t* prefsArr = static_cast<int32_t*>(uprv_malloc(keysLen * sizeof(int32_t)));
    if (prefsArr == nullptr) {
        status = U_MEMORY_ALLOCATION_ERROR;
        return;
    }

    int32_t prefsLen = 0;

    // Call the selector
    // Already checked for fallback, so it's safe to call takeValue()
    LocalPointer<FunctionValue> rvVal(rv.takeValue(status));
    rvVal->selectKeys(adoptedKeys.getAlias(), keysLen, prefsArr, prefsLen,
                      status);

    // Update errors
    if (savedStatus != status) {
        if (U_FAILURE(status)) {
            status = U_ZERO_ERROR;
            context.getErrors().setSelectorError({}, status);
        } else {
            // Ignore warnings
            status = savedStatus;
        }
    }

    CHECK_ERROR(status);

    // Copy the resulting keys (if there was no error)
    keysOut.removeAllElements();
    for (int32_t i = 0; i < prefsLen; i++) {
        UnicodeString* k =
            message2::create<UnicodeString>(std::move(keysArr[prefsArr[i]]), status);
        if (k == nullptr) {
            status = U_MEMORY_ALLOCATION_ERROR;
            return;
        }
        keysOut.adoptElement(k, status);
        CHECK_ERROR(status);
    }

    uprv_free(prefsArr);
}

// See https://github.com/unicode-org/message-format-wg/blob/main/spec/formatting.md#resolve-preferences
// `res` is a vector of FormattedPlaceholders;
// `pref` is a vector of vectors of strings
void MessageFormatter::resolvePreferences(MessageContext& context, UVector& res, UVector& pref, UErrorCode &status) const {
    CHECK_ERROR(status);

    // 1. Let pref be a new empty list of lists of strings.
    // (Implicit, since `pref` is an out-parameter)
    UnicodeString ks;
    LocalPointer<UnicodeString> ksP;
    int32_t numVariants = dataModel.numVariants();
    const Variant* variants = dataModel.getVariantsInternal();
    // 2. For each index i in res
    for (int32_t i = 0; i < res.size(); i++) {
        // 2i. Let keys be a new empty list of strings.
        LocalPointer<UVector> keys(createUVector(status));
        CHECK_ERROR(status);
        // 2ii. For each variant `var` of the message
        for (int32_t variantNum = 0; variantNum < numVariants; variantNum++) {
            const SelectorKeys& selectorKeys = variants[variantNum].getKeys();

            // Note: Here, `var` names the key list of `var`,
            // not a Variant itself
            const Key* var = selectorKeys.getKeysInternal();
            // 2ii(a). Let `key` be the `var` key at position i.
            U_ASSERT(i < selectorKeys.len); // established by semantic check in formatSelectors()
            const Key& key = var[i];
            // 2ii(b). If `key` is not the catch-all key '*'
            if (!key.isWildcard()) {
                // 2ii(b)(a) Assert that key is a literal.
                // (Not needed)
                // 2ii(b)(b) Let `ks` be the resolved value of `key`.
                ks = key.asLiteral().unquoted();
                // 2ii(b)(c) Append `ks` as the last element of the list `keys`.
                ksP.adoptInstead(create<UnicodeString>(std::move(ks), status));
                CHECK_ERROR(status);
                keys->adoptElement(ksP.orphan(), status);
            }
        }
        // 2iii. Let `rv` be the resolved value at index `i` of `res`.
        U_ASSERT(i < res.size());
        InternalValue rv = std::move(*(static_cast<InternalValue*>(res[i])));
        // 2iv. Let matches be the result of calling the method MatchSelectorKeys(rv, keys)
        LocalPointer<UVector> matches(createUVector(status));
        matchSelectorKeys(*keys, context, std::move(rv), *matches, status);
        // 2v. Append `matches` as the last element of the list `pref`
        pref.adoptElement(matches.orphan(), status);
    }
}

// `v` is assumed to be a vector of strings
static int32_t vectorFind(const UVector& v, const UnicodeString& k) {
    for (int32_t i = 0; i < v.size(); i++) {
        if (*static_cast<UnicodeString*>(v[i]) == k) {
            return i;
        }
    }
    return -1;
}

static UBool vectorContains(const UVector& v, const UnicodeString& k) {
    return (vectorFind(v, k) != -1);
}

// See https://github.com/unicode-org/message-format-wg/blob/main/spec/formatting.md#filter-variants
// `pref` is a vector of vectors of strings. `vars` is a vector of PrioritizedVariants
void MessageFormatter::filterVariants(const UVector& pref, UVector& vars, UErrorCode& status) const {
    const Variant* variants = dataModel.getVariantsInternal();

    // 1. Let `vars` be a new empty list of variants.
    // (Not needed since `vars` is an out-parameter)
    // 2. For each variant `var` of the message:
    for (int32_t j = 0; j < dataModel.numVariants(); j++) {
        const SelectorKeys& selectorKeys = variants[j].getKeys();
        const Pattern& p = variants[j].getPattern();

        // Note: Here, `var` names the key list of `var`,
        // not a Variant itself
        const Key* var = selectorKeys.getKeysInternal();
        // 2i. For each index `i` in `pref`:
        bool noMatch = false;
        for (int32_t i = 0; i < pref.size(); i++) {
            // 2i(a). Let `key` be the `var` key at position `i`.
            U_ASSERT(i < selectorKeys.len);
            const Key& key = var[i];
            // 2i(b). If key is the catch-all key '*':
            if (key.isWildcard()) {
                // 2i(b)(a). Continue the inner loop on pref.
                continue;
            }
            // 2i(c). Assert that `key` is a literal.
            // (Not needed)
            // 2i(d). Let `ks` be the resolved value of `key`.
            UnicodeString ks = key.asLiteral().unquoted();
            // 2i(e). Let `matches` be the list of strings at index `i` of `pref`.
            const UVector& matches = *(static_cast<UVector*>(pref[i])); // `matches` is a vector of strings
            // 2i(f). If `matches` includes `ks`
            if (vectorContains(matches, ks)) {
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
	    PrioritizedVariant* tuple = create<PrioritizedVariant>(PrioritizedVariant(-1, selectorKeys, p), status);
            CHECK_ERROR(status);
            vars.adoptElement(tuple, status);
        }
    }
}

// See https://github.com/unicode-org/message-format-wg/blob/main/spec/formatting.md#sort-variants
// Leaves the preferred variant as element 0 in `sortable`
// Note: this sorts in-place, so `sortable` is just `vars`
// `pref` is a vector of vectors of strings; `vars` is a vector of PrioritizedVariants
void MessageFormatter::sortVariants(const UVector& pref, UVector& vars, UErrorCode& status) const {
    CHECK_ERROR(status);

// Note: steps 1 and 2 are omitted since we use `vars` as `sortable` (we sort in-place)
    // 1. Let `sortable` be a new empty list of (integer, variant) tuples.
    // (Not needed since `sortable` is an out-parameter)
    // 2. For each variant `var` of `vars`
    // 2i. Let tuple be a new tuple (-1, var).
    // 2ii. Append `tuple` as the last element of the list `sortable`.

    // 3. Let `len` be the integer count of items in `pref`.
    int32_t len = pref.size();
    // 4. Let `i` be `len` - 1.
    int32_t i = len - 1;
    // 5. While i >= 0:
    while (i >= 0) {
        // 5i. Let `matches` be the list of strings at index `i` of `pref`.
        U_ASSERT(pref[i] != nullptr);
	const UVector& matches = *(static_cast<UVector*>(pref[i])); // `matches` is a vector of strings
        // 5ii. Let `minpref` be the integer count of items in `matches`.
        int32_t minpref = matches.size();
        // 5iii. For each tuple `tuple` of `sortable`:
        for (int32_t j = 0; j < vars.size(); j++) {
            U_ASSERT(vars[j] != nullptr);
            PrioritizedVariant& tuple = *(static_cast<PrioritizedVariant*>(vars[j]));
            // 5iii(a). Let matchpref be an integer with the value minpref.
            int32_t matchpref = minpref;
            // 5iii(b). Let `key` be the tuple variant key at position `i`.
            const Key* tupleVariantKeys = tuple.keys.getKeysInternal();
            U_ASSERT(i < tuple.keys.len); // Given by earlier semantic checking
            const Key& key = tupleVariantKeys[i];
            // 5iii(c) If `key` is not the catch-all key '*':
            if (!key.isWildcard()) {
                // 5iii(c)(a). Assert that `key` is a literal.
                // (Not needed)
                // 5iii(c)(b). Let `ks` be the resolved value of `key`.
                UnicodeString ks = key.asLiteral().unquoted();
                // 5iii(c)(c) Let matchpref be the integer position of ks in `matches`.
                matchpref = vectorFind(matches, ks);
                U_ASSERT(matchpref >= 0);
            }
            // 5iii(d) Set the `tuple` integer value as matchpref.
            tuple.priority = matchpref;
        }
        // 5iv. Set `sortable` to be the result of calling the method SortVariants(`sortable`)
        vars.sort(comparePrioritizedVariants, status);
        CHECK_ERROR(status);
        // 5v. Set `i` to be `i` - 1.
        i--;
    }
    // The caller is responsible for steps 6 and 7
    // 6. Let `var` be the `variant` element of the first element of `sortable`.
    // 7. Select the pattern of `var`
}

void MessageFormatter::formatSelectors(MessageContext& context, const Environment& env, UErrorCode &status, UnicodeString& result) const {
    CHECK_ERROR(status);

    // See https://github.com/unicode-org/message-format-wg/blob/main/spec/formatting.md#pattern-selection

    // Resolve Selectors
    // res is a vector of FormattedPlaceholders
    LocalPointer<UVector> res(createUVector(status));
    CHECK_ERROR(status);
    resolveSelectors(context, env, status, *res);

    // Resolve Preferences
    // pref is a vector of vectors of strings
    LocalPointer<UVector> pref(createUVector(status));
    CHECK_ERROR(status);
    resolvePreferences(context, *res, *pref, status);

    // Filter Variants
    // vars is a vector of PrioritizedVariants
    LocalPointer<UVector> vars(createUVector(status));
    CHECK_ERROR(status);
    filterVariants(*pref, *vars, status);

    // Sort Variants and select the final pattern
    // Note: `sortable` in the spec is just `vars` here,
    // which is sorted in-place
    sortVariants(*pref, *vars, status);

    CHECK_ERROR(status);

    // 6. Let `var` be the `variant` element of the first element of `sortable`.
    U_ASSERT(vars->size() > 0); // This should have been checked earlier (having 0 variants would be a data model error)
    const PrioritizedVariant& var = *(static_cast<PrioritizedVariant*>(vars->elementAt(0)));
    // 7. Select the pattern of `var`
    const Pattern& pat = var.pat;

    // Format the pattern
    formatPattern(context, env, pat, status, result);
}

// Note: this is non-const due to the function registry being non-const, which is in turn
// due to the values (`FormatterFactory` objects in the map) having mutable state.
// In other words, formatting a message can mutate the underlying `MessageFormatter` by changing
// state within the factory objects that represent custom formatters.
UnicodeString MessageFormatter::formatToString(const MessageArguments& arguments, UErrorCode &status) {
    EMPTY_ON_ERROR(status);

    // Create a new environment that will store closures for all local variables
    Environment* env = Environment::create(status);
    // Create a new context with the given arguments and the `errors` structure
    MessageContext context(arguments, *errors, status);

    // Check for unresolved variable errors
    checkDeclarations(context, env, status);
    LocalPointer<Environment> globalEnv(env);

    DynamicErrors& err = context.getErrors();
    UnicodeString result;

    if (!(err.hasSyntaxError() || err.hasDataModelError())) {
        if (dataModel.hasPattern()) {
            formatPattern(context, *globalEnv, dataModel.getPattern(), status, result);
        } else {
            formatSelectors(context, *globalEnv, status, result);
        }
    }

    // Update status according to all errors seen while formatting
    if (signalErrors) {
        context.checkErrors(status);
    }
    if (U_FAILURE(status)) {
        result.remove();
    }
    return result;
}

// ----------------------------------------
// Checking for resolution errors

void MessageFormatter::check(MessageContext& context, const Environment& localEnv, const OptionMap& options, UErrorCode& status) const {
    // Check the RHS of each option
    for (int32_t i = 0; i < options.size(); i++) {
        const Option& opt = options.getOption(i, status);
        CHECK_ERROR(status);
        check(context, localEnv, opt.getValue(), status);
    }
}

void MessageFormatter::check(MessageContext& context, const Environment& localEnv, const Operand& rand, UErrorCode& status) const {
    // Nothing to check for literals
    if (rand.isLiteral() || rand.isNull()) {
        return;
    }

    // Check that variable is in scope
    const VariableName& var = rand.asVariable();
    // Check local scope
    if (localEnv.has(var)) {
        return;
    }
    // Check global scope
    context.getGlobal(var, status);
    if (status == U_ILLEGAL_ARGUMENT_ERROR) {
        status = U_ZERO_ERROR;
        context.getErrors().setUnresolvedVariable(var, status);
    }
    // Either `var` is a global, or some other error occurred.
    // Nothing more to do either way
    return;
}

void MessageFormatter::check(MessageContext& context, const Environment& localEnv, const Expression& expr, UErrorCode& status) const {
    // Check for unresolved variable errors
    if (expr.isFunctionCall()) {
        const Operator* rator = expr.getOperator(status);
        U_ASSERT(U_SUCCESS(status));
        const Operand& rand = expr.getOperand();
        check(context, localEnv, rand, status);
        check(context, localEnv, rator->getOptionsInternal(), status);
    }
}

// Check for resolution errors
void MessageFormatter::checkDeclarations(MessageContext& context, Environment*& env, UErrorCode &status) const {
    CHECK_ERROR(status);

    const Binding* decls = getDataModel().getLocalVariablesInternal();
    U_ASSERT(env != nullptr && (decls != nullptr || getDataModel().bindingsLen == 0));

    for (int32_t i = 0; i < getDataModel().bindingsLen; i++) {
        const Binding& decl = decls[i];
        const Expression& rhs = decl.getValue();
        check(context, *env, rhs, status);

        // Add a closure to the global environment,
        // memoizing the value of localEnv up to this point

        // Add the LHS to the environment for checking the next declaration
        env = Environment::create(decl.getVariable(), Closure(rhs, *env), env, status);
        CHECK_ERROR(status);
    }
}
} // namespace message2

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_MF2 */

#endif /* #if !UCONFIG_NO_FORMATTING */
