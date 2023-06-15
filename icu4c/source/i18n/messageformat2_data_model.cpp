// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/messageformat2.h"
#include "unicode/messageformat2_data_model.h"
#include "uvector.h" // U_ASSERT

U_NAMESPACE_BEGIN namespace message2 {

MessageFormatDataModel::~MessageFormatDataModel() {}

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

void SERIALIZER::emit(const Literal& l) {
    if (l.isQuoted) {
      emit(PIPE);
      for (size_t i = 0; ((int32_t) i) < l.contents.length(); i++) {
        // Re-escape any PIPE or BACKSLASH characters
        switch(l.contents[i]) {
        case BACKSLASH:
        case PIPE: {
          emit(BACKSLASH);
          break;
        }
        default: {
          break;
        }
        }
        emit(l.contents[i]);
      }
      emit(PIPE);
    } else {
      emit(l.contents);
    }
}

void SERIALIZER::emit(const Key& k) {
    if (k.isWildcard()) {
        emit(ASTERISK);
        return;
    }
    emit(k.asLiteral());
}

void SERIALIZER::emit(const SelectorKeys& k) {
  const KeyList& ks = k.getKeys();
  size_t len = ks.length();
  // It would be an error for `keys` to be empty;
  // that would mean this is the single `pattern`
  // variant, and in that case, this method shouldn't be called
  U_ASSERT(len > 0);
  for (size_t i = 0; i < len; i++) {
    if (i != 0) {
      whitespace();
    }
    emit(*ks.get(i));
  }
}

void SERIALIZER::emit(const Operand& rand) {
    if (rand.isVariable()) {
        emit(DOLLAR);
        emit(rand.asVariable());
    } else {
        // Literal: quoted or unquoted
        emit(rand.asLiteral());
    }
}

void SERIALIZER::emit(const OptionMap& options) {
    size_t pos = OptionMap::FIRST;
    UnicodeString k;
    Operand* v;
    while (options.next(pos, k, v)) {
      whitespace();
      emit(k);
      emit(EQUALS);
      emit(*v);
    }
}

void SERIALIZER::emit(const Expression& expr) {
    emit(LEFT_CURLY_BRACE);

    if (!expr.isReserved() && !expr.isFunctionCall()) {
        // Literal or variable, no annotation
        emit(expr.getOperand());
    } else {
        // Function call or reserved
        if (!expr.isStandaloneAnnotation()) {
          // Must be a function call that has an operand
          emit(expr.getOperand());
          whitespace();
        }
        if (expr.isReserved()) {
          const Reserved& reserved = expr.asReserved();
          // Re-escape '\' / '{' / '|' / '}'
          for (size_t i = 0; i < reserved.numParts(); i++) {
            const Literal& l = *reserved.getPart(i);
            if (l.isQuoted) {
              emit(l);
            } else {
              const UnicodeString& s = l.contents;
              for (size_t j = 0; ((int32_t) j) < s.length(); j++) {
                switch(s[j]) {
                case LEFT_CURLY_BRACE:
                case PIPE:
                case RIGHT_CURLY_BRACE:
                case BACKSLASH: {
                  emit(BACKSLASH);
                  break;
                }
                default:
                  break;
                }
                emit(s[j]);
              }
            }
          }
        } else {
          emit(expr.getFunctionName());
          // No whitespace after function name, in case it has
          // no options. (when there are options, emit(OptionMap) will
          // emit the leading whitespace)
          emit(expr.getOptions());
        }
    }
    
    emit(RIGHT_CURLY_BRACE);
}

void SERIALIZER::emit(const PatternPart& part) {
    if (part.isText()) {
        // Raw text
        const UnicodeString& text = part.asText();
        // Re-escape '{'/'}'/'\'
        for (size_t i = 0; ((int32_t) i) < text.length(); i++) {
          switch(text[i]) {
          case BACKSLASH:
          case LEFT_CURLY_BRACE:
          case RIGHT_CURLY_BRACE: {
            emit(BACKSLASH);
            break;
          }
          default:
            break;
          }
          emit(text[i]);
        }
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
                    
void SERIALIZER::serializeDeclarations() {
    const Bindings& locals = dataModel.getLocalVariables();
    
    for (size_t i = 0; i < locals.length(); i++) {
        const Binding& b = *locals.get(i);
        // No whitespace needed here -- see `message` in the grammar
        emit(ID_LET);
        whitespace();
        emit(DOLLAR);
        emit(b.var);
        // No whitespace needed here -- see `declaration` in the grammar
        emit(EQUALS);
        // No whitespace needed here -- see `declaration` in the grammar
        emit(*b.value);
    }
}

void SERIALIZER::serializeSelectors() {
    U_ASSERT(dataModel.body->scrutinees != nullptr);
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
    U_ASSERT(dataModel.body->variants != nullptr);

    const VariantMap& variants = *dataModel.body->variants;
    size_t pos = VariantMap::FIRST;

    const SelectorKeys* selectorKeys;
    Pattern* pattern;

    while (variants.next(pos, selectorKeys, pattern)) {
      emit(ID_WHEN);
      whitespace();
      emit(*selectorKeys);
      // No whitespace needed here -- see `variant` in the grammar
      emit(*pattern);
    }    
}


// Main (public) serializer method
void SERIALIZER::serialize() {
    serializeDeclarations();
    // Pattern message
    if (dataModel.body->pattern != nullptr) {
      emit(*dataModel.body->pattern);
    } else {
      // Selectors message
      serializeSelectors();
      serializeVariants();
    }
}

} // namespace message2
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

