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
    for (int32_t i = 0; ((int32_t) i) < annotated->size(); i++) {
        UnicodeString* lhs = (UnicodeString*) (*annotated)[i];
        U_ASSERT(lhs != nullptr);
        if (*lhs == var.name()) {
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

    LocalPointer<UnicodeString> s(new UnicodeString(var.name()));
    if (!s.isValid()) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return;
    }
    annotated->adoptElement(s.orphan(), errorCode);
}

TypeEnvironment::~TypeEnvironment() {}

// ---------------------

static bool areDefaultKeys(const KeyList& keys) {
    U_ASSERT(keys.length() > 0);
    for (int32_t i = 0; i < keys.length(); i++) {
        if (!keys.get(i)->isWildcard()) {
            return false;
        }
    }
    return true;
}

void MessageFormatter::Checker::checkVariants(UErrorCode& error) {
    CHECK_ERROR(error);
    U_ASSERT(dataModel.hasSelectors());

    // Determine the number of selectors
    int32_t numSelectors = dataModel.getSelectors().length();

    // Check that each variant has a key list with length
    // equal to the number of selectors
    const VariantMap& variants = dataModel.getVariants();
    int32_t pos = VariantMap::FIRST;
    const SelectorKeys* selectorKeys;
    const Pattern* pattern;

    // Check that one variant includes only wildcards
    bool defaultExists = false;

    while (variants.next(pos, selectorKeys, pattern)) {
        const KeyList& keys = selectorKeys->getKeys();
        if (keys.length() != numSelectors) {
            // Variant key mismatch
            errors.addError(Error::Type::VariantKeyMismatchError, error);
            return;
        }
        defaultExists |= areDefaultKeys(keys);
    }
    if (!defaultExists) {
        errors.addError(Error::Type::NonexhaustivePattern, error);
        return;
    }
}

void MessageFormatter::Checker::requireAnnotated(const TypeEnvironment& t, const Expression& selectorExpr, UErrorCode& error) {
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
    errors.addError(Error::Type::MissingSelectorAnnotation, error);
}

void MessageFormatter::Checker::checkSelectors(const TypeEnvironment& t, UErrorCode& error) {
    CHECK_ERROR(error);
    U_ASSERT(dataModel.hasSelectors());

    // Check each selector; if it's not annotated, emit a
    // "missing selector annotation" error
    const ExpressionList& selectors = dataModel.getSelectors();
    for (int32_t i = 0; i < selectors.length(); i++) {
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
    U_ASSERT(!rand.isNull());
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
    for (int32_t i = 0; i < env.length(); i++) {
        const Binding* b = env.get(i);
        U_ASSERT(b != nullptr);
        const Expression* rhs = b->getValue();
        U_ASSERT(rhs != nullptr);
        t.extend(b->var, typeOf(t, *rhs), error);
    }
}

void MessageFormatter::Checker::check(UErrorCode& error) {
    CHECK_ERROR(error);

    TypeEnvironment typeEnv(error);
    checkDeclarations(typeEnv, error);
    // Pattern message
    if (!dataModel.hasSelectors()) {
        return;
    } else {
      // Selectors message
      checkSelectors(typeEnv, error);
      checkVariants(error);
    }
}

} // namespace message2
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

