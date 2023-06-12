// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/messageformat2.h"
#include "unicode/messageformat2_data_model.h"
#include "uvector.h" // U_ASSERT

U_NAMESPACE_BEGIN namespace message2 {

    MessageFormatDataModel::~MessageFormatDataModel() {
        delete env;
        delete body;
    }

// Generates a string representation of a data model
// ------------------------------------------------

#define SERIALIZER MessageFormatDataModel::Serializer

// Private helper methods

void SERIALIZER::whitespace() {
    result += SPACE;
}

void SERIALIZER::emit(UChar32 c) {
    result += c;
}

void SERIALIZER::emit(const UnicodeString& s) {
    result += s;
}

template <size_t N>
void SERIALIZER::emit(const UChar32 (&token)[N]) {
    // Don't emit the terminator
    for (size_t i = 0; i < N - 1; i++) {
        emit(token[i]);
    }
}

void SERIALIZER::emit(const Key& k) {
    if (k.isWildcard()) {
        emit(ASTERISK);
        return;
    }
    emit(k.getString());
}

void SERIALIZER::emit(const Operand& rand) {
    if (rand.isVariable()) {
        emit(DOLLAR);
        emit(rand.asVariable());
    } else {
        // Literal: quoted or unquoted
        const Literal& lit = rand.asLiteral();
        if (lit.isQuoted) {
            emit(PIPE);
            emit(lit.contents);
            emit(PIPE);
        } else {
            emit(lit.contents);
        }
    }
}

void SERIALIZER::emit(const Operator& rator) {
    if (rator.isReserved()) {
        emit(rator.asReserved());
        return;
    }
    // Must be function name
    // Emit the name only -- emit(Expression) emits options
    // TODO: does this encode the :/+/- prefix? We'll find out!
    emit(rator.getFunctionName());
}

// Option list
// TODO
/*
void SERIALIZER::emit(const Hashtable& options) {
    int32_t pos = UHASH_FIRST;
    while(true) {
        const UHashElement* element = options.nextElement(pos);
        if (element == nullptr) {
            break;
        }
        if (pos != UHASH_FIRST) {
            whitespace();
        }
        const UnicodeString& name = *(static_cast<UnicodeString*>(element->key.pointer)); 
        emit(name);
        emit(EQUALS);
        const Expression& e = *(static_cast<Expression*>(element->value.pointer));
        emit(e);
    }
}
*/

void SERIALIZER::emit(const OptionList& options) {
    for (size_t i = 0; i < options.length(); i++) {
        const Option& opt = *options.get(i);
        whitespace();
        emit(opt.name);
        emit(EQUALS);
        emit(opt.value);
    }
}

void SERIALIZER::emit(const Expression& expr) {
    emit(LEFT_CURLY_BRACE);

    if (!expr.isFunctionCall()) {
        // Literal or variable, no annotation
        emit(expr.getOperand());
    } else if (expr.isReserved()) {
       // Reserved sequence - serializes to itself
       emit(expr.asReserved());
    }
    else {
        // Function call
        if (!expr.isStandaloneAnnotation()) {
          // Must be a function call that has an operand
          emit(expr.getOperand());
          whitespace();
        }
        emit(expr.getFunctionName());
        // No whitespace after function name, in case it has
        // no options. (when there are options, emit(OptionList) will
        // emit the leading whitespace)
        emit(expr.getOptions());
    }
    
    emit(RIGHT_CURLY_BRACE);
}

void SERIALIZER::emit(const PatternPart& part) {
    if (part.isText()) {
        // Raw text
        emit(part.asText());
        return;
    }
    // Expression
    emit(part.contents());
}

void SERIALIZER::emit(const Pattern& pat) {
    size_t len = pat.numParts();
    emit(LEFT_CURLY_BRACE);
    for (size_t i = 0; i < len; i++) {
        // No whitespace is needed here -- see the `pattern` nonterminal in the grammar
        emit(*pat.getPart(i));
    }
    emit(RIGHT_CURLY_BRACE);
}

void SERIALIZER::emit(const Variant& var) {
    const KeyList& ks = *var.keys;
    // Keys should be non-empty (if it was, then this method
    // shouldn't have been called)
    U_ASSERT(ks.length() > 0);

    for (size_t i = 0; i < ks.length(); i++) {
        if (i != 0) {
            whitespace();
        }
        emit(*ks.get(i));
    }
    // No whitespace needed here -- see `variant` in the grammar
    emit(*var.pattern);
}

void SERIALIZER::serializeDeclarations() {
    const Environment& locals = dataModel.getLocalVariables();
    int32_t pos = Environment::FIRST_ELEMENT;

    while(true) {
        const UHashElement* element = locals.nextElement(pos);
        if (element == nullptr) {
            break;
        }
        // No whitespace needed here -- see `message` in the grammar
        emit(ID_LET);
        whitespace();
        emit(DOLLAR);
        const UnicodeString& name = *(static_cast<UnicodeString*>(element->key.pointer));
        emit(name);
        // No whitespace needed here -- see `declaration` in the grammar
        emit(EQUALS);
        // No whitespace needed here -- see `declaration` in the grammar
        const Expression& e = *(static_cast<Expression*>(element->value.pointer));
        emit(e);
    }
}

void SERIALIZER::serializeSelectors() {
    const ExpressionList& selectors = *dataModel.body->scrutinees;
    size_t len = selectors.length();

    // Special case: selectors is empty. Emit nothing in this case;
    // serializeVariants() will emit the pattern.
    if (len == 0) {
        return;
    }

    emit(ID_MATCH);
    for (size_t i = 0; i < len; i++) {
        // No whitespace needed here -- see `selectors` in the grammar
        emit(*selectors.get(i));
    }
}

void SERIALIZER::serializeVariants() {
    const VariantList& variants = *dataModel.body->variants;
    size_t len = variants.length();

    // Special case: one variant with no keys. Just emit the pattern;
    // no `when` keyword.
    if (len == 1 && variants.get(0)->keys->length() == 0) {
        emit(*variants.get(0)->pattern);
        return;
    }
    
    // General case
    for (size_t i = 0; i < len; i++) {
        // No whitespace needed here -- see `body` in the grammar
        emit(ID_WHEN);
        whitespace();
        const Variant& var = *variants.get(i);
        emit(var);
    }
}


// Main (public) serializer method
void SERIALIZER::serialize() {
    serializeDeclarations();
    // We assume the data model is well-formed.
    // Because of how pattern messages (with no selectors) are represented, the following
    // two calls are sufficient to handle both selectors and pattern messages.
    serializeSelectors();
    serializeVariants();
}

} // namespace message2
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

