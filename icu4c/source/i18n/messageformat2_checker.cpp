// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/messageformat2.h"
#include "unicode/messageformat2_data_model.h"
#include "uvector.h" // U_ASSERT

U_NAMESPACE_BEGIN namespace message2 {

// Checks semantic properties for data models
// ------------------------------------------------

using Binding    = MessageFormatDataModel::Binding;
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

void MessageFormatter::Checker::check(const Pattern& p, UErrorCode& error) {
    CHECK_ERROR(error);
    (void) p;
    // TODO
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

void MessageFormatter::Checker::check(const Expression& e, UErrorCode& error) {
    CHECK_ERROR(error);

// TODO check other errors in expressions
    (void) e;

    // Checking for duplicate option names was already done
    // during parsing (it has to be, since once parsed,
    // the representation as an `OptionMap` guarantees
    // unique keys)
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

void MessageFormatter::Checker::checkSelectors(UErrorCode& error) {
    CHECK_ERROR(error);
    U_ASSERT(dataModel.hasSelectors());

    // TODO
}

void MessageFormatter::Checker::checkDeclarations(UErrorCode& error) {
    CHECK_ERROR(error);
    
    // TODO
}

// TODO: currently this only handles a single error
void MessageFormatter::Checker::check(UErrorCode& error) {
    CHECK_ERROR(error);

    checkDeclarations(error);
    // Pattern message
    if (!dataModel.hasSelectors()) {
      check(dataModel.getPattern(), error);
    } else {
      // Selectors message
      checkSelectors(error);
      checkVariants(error);
    }
}

} // namespace message2
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

