// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/messageformat2.h"
#include "unicode/messageformat2_checker.h"
#include "unicode/messageformat2_data_model.h"
#include "uvector.h" // U_ASSERT

U_NAMESPACE_BEGIN namespace message2 {

// Checks semantic properties for data models
// ------------------------------------------------

using Type = TypeEnvironment::Type;

using Binding     = MessageFormatDataModel::Binding;
using Bindings    = MessageFormatDataModel::Bindings;
using Key         = MessageFormatDataModel::Key;
using SelectorKeys = MessageFormatDataModel::SelectorKeys;
using KeyList     = MessageFormatDataModel::KeyList;
using Literal     = MessageFormatDataModel::Literal;
using OptionMap   = MessageFormatDataModel::OptionMap;
using Expression  = MessageFormatDataModel::Expression;
using ExpressionList  = MessageFormatDataModel::ExpressionList;
using Operand     = MessageFormatDataModel::Operand;
using Operator    = MessageFormatDataModel::Operator;
using Pattern     = MessageFormatDataModel::Pattern;
using PatternPart = MessageFormatDataModel::PatternPart;
using Reserved    = MessageFormatDataModel::Reserved;
using VariantMap    = MessageFormatDataModel::VariantMap;

// Type environments
// -----------------

TypeEnvironment::TypeEnvironment(UErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    // initialize `contents`
    annotated.adoptInstead(new UVector(errorCode));
    CHECK_ERROR(errorCode);
    if (U_FAILURE(errorCode)) {
        return;
    }
    annotated->setDeleter(uprv_deleteUObject);
}

Type TypeEnvironment::get(const VariableName& var) const {
    for (size_t i = 0; ((int32_t) i) < annotated->size(); i++) {
        VariableName* lhs = (VariableName*) (*annotated)[i];
        U_ASSERT(lhs != nullptr);
        if (*lhs == var) {
            return Annotated;
        }
    }
    return Unannotated;
}

void TypeEnvironment::extend(const VariableName& var, Type t, UErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    if (t == Unannotated) {
        // Nothing to do, as variables are considered
        // unannotated by default
        return;
    }

    LocalPointer<UnicodeString> s(new UnicodeString(var));
    if (!s.isValid()) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return;
    }
    annotated->adoptElement(s.orphan(), errorCode);
}

TypeEnvironment::~TypeEnvironment() {}

// ---------------------
void MessageFormatter::Checker::check(const Pattern& p, UErrorCode& error) {
    CHECK_ERROR(error);

    for (size_t i = 0; i < p.numParts(); i++) {
        const PatternPart& part = *p.getPart(i);
        // Check each expression part. Text parts are error-free
        if (!part.isText()) {
            check(part.contents(), error);
        }
    }
}

static bool areDefaultKeys(const KeyList& keys) {
    U_ASSERT(keys.length() > 0);
    for (size_t i = 0; i < keys.length(); i++) {
        if (!keys.get(i)->isWildcard()) {
            return false;
        }
    }
    return true;
}

void MessageFormatter::Checker::check(const Operand& rand, UErrorCode& error) {
    CHECK_ERROR(error);

    // Nothing to check for literals
    if (rand.isLiteral()) {
        return;
    }

// TODO: Variables = resolution error, checked during formatting
// Anything else to check here?
}

void MessageFormatter::Checker::check(const OptionMap& options, UErrorCode& error) {
    CHECK_ERROR(error);

    // Check the RHS of each option
    size_t pos = OptionMap::FIRST;
    UnicodeString k; // not used
    const Operand* rhs;
    while(true) {
        if (!options.next(pos, k, rhs)) {
            break;
        }
        U_ASSERT(rhs != nullptr);
        check(*rhs, error);
    }
}

void MessageFormatter::Checker::check(const Expression& e, UErrorCode& error) {
    CHECK_ERROR(error);

    // Checking for duplicate option names was already done
    // during parsing (it has to be, since once parsed,
    // the representation as an `OptionMap` guarantees
    // unique keys)

    // For function calls, check the operand and the RHSs of options
    if (e.isFunctionCall()) {
        const Operator& rator = e.getOperator();
        if (!e.isStandaloneAnnotation()) {
            const Operand& rand = e.getOperand();
            check(rand, error);
        }
        check(rator.getOptions(), error);
    }
}

void MessageFormatter::Checker::checkVariants(UErrorCode& error) {
    CHECK_ERROR(error);
    U_ASSERT(dataModel.hasSelectors());

    // Determine the number of selectors
    size_t numSelectors = dataModel.getSelectors().length();

    // Check that each variant has a key list with length
    // equal to the number of selectors
    const VariantMap& variants = dataModel.getVariants();
    size_t pos = VariantMap::FIRST;
    const SelectorKeys* selectorKeys;
    const Pattern* pattern;

    // Check that one variant includes only wildcards
    bool defaultExists = false;

    while (variants.next(pos, selectorKeys, pattern)) {
        const KeyList& keys = selectorKeys->getKeys();
        if (keys.length() != numSelectors) {
            // Variant key mismatch
            error = U_VARIANT_KEY_MISMATCH;
            return;
        }
        defaultExists |= areDefaultKeys(keys);
        check(*pattern, error);
    }
    if (!defaultExists) {
        error = U_NONEXHAUSTIVE_PATTERN;
        return;
    }
}

void requireAnnotated(const TypeEnvironment& t, const Expression& selectorExpr, UErrorCode& error) {
    CHECK_ERROR(error);

    if (selectorExpr.isFunctionCall()) {
        return; // No error
    }
    if (!selectorExpr.isReserved()) {
        const Operand& rand = selectorExpr.getOperand();
        if (rand.isVariable()) {
            if (t.get(rand.asVariable()) == Type::Annotated) {
                return; // No error
            }
        }
    }
    // If this code is reached, an error was detected
    error = U_MISSING_SELECTOR_ANNOTATION;
}

void MessageFormatter::Checker::checkSelectors(const TypeEnvironment& t, UErrorCode& error) {
    CHECK_ERROR(error);
    U_ASSERT(dataModel.hasSelectors());

    // Check each selector; if it's not annotated, emit a
    // "missing selector annotation" error
    const ExpressionList& selectors = dataModel.getSelectors();
    for (size_t i = 0; i < selectors.length(); i++) {
        const Expression* expr = selectors.get(i);
        U_ASSERT(expr != nullptr);
        requireAnnotated(t, *expr, error);
    }
}

Type typeOf(TypeEnvironment& t, const Expression& expr) {
    if (expr.isFunctionCall()) {
        return Type::Annotated;
    }
    if (expr.isReserved()) {
        return Type::Unannotated;
    }
    const Operand& rand = expr.getOperand();
    if (rand.isLiteral()) {
        return Type::Unannotated;
    }
    U_ASSERT(rand.isVariable());
    return t.get(rand.asVariable());
}

void MessageFormatter::Checker::checkDeclarations(TypeEnvironment& t, UErrorCode& error) {
    CHECK_ERROR(error);
    
    // For each declaration, extend the type environment with its type
    // Only a very simple type system is necessary: local variables
    // have the type "annotated" or "unannotated".
    // Free variables (message arguments) are treated as unannotated.
    const MessageFormatDataModel::Bindings& env = dataModel.getLocalVariables();
    for (size_t i = 0; i < env.length(); i++) {
        const Binding* b = env.get(i);
        U_ASSERT(b != nullptr);
        const Expression* rhs = b->getValue();
        U_ASSERT(rhs != nullptr);
        t.extend(b->var, typeOf(t, *rhs), error);
    }
}

// TODO: currently this only handles a single error
void MessageFormatter::Checker::check(UErrorCode& error) {
    CHECK_ERROR(error);

    TypeEnvironment typeEnv(error);
    checkDeclarations(typeEnv, error);
    // Pattern message
    if (!dataModel.hasSelectors()) {
      check(dataModel.getPattern(), error);
    } else {
      // Selectors message
      checkSelectors(typeEnv, error);
      checkVariants(error);
    }
}

} // namespace message2
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

