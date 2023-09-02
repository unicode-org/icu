// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/messageformat2.h"
#include "unicode/messageformat2_data_model.h"
#include "uvector.h" // U_ASSERT

U_NAMESPACE_BEGIN namespace message2 {

using Binding          = MessageFormatDataModel::Binding;
using Expression       = MessageFormatDataModel::Expression;
using ExpressionList   = MessageFormatDataModel::ExpressionList;
using Key              = MessageFormatDataModel::Key;
using KeyList          = MessageFormatDataModel::KeyList;
using Literal          = MessageFormatDataModel::Literal;
using OptionMap        = MessageFormatDataModel::OptionMap;
using Operand          = MessageFormatDataModel::Operand;
using Operator         = MessageFormatDataModel::Operator;
using Pattern          = MessageFormatDataModel::Pattern;
using PatternPart      = MessageFormatDataModel::PatternPart;
using Reserved         = MessageFormatDataModel::Reserved;
using SelectorKeys     = MessageFormatDataModel::SelectorKeys;
using VariantMap       = MessageFormatDataModel::VariantMap;

// Implementation

//------------------ SelectorKeys

SelectorKeys::Builder& SelectorKeys::Builder::add(Key* key, UErrorCode& errorCode) {
    THIS_ON_ERROR(errorCode);
    keys->add(key, errorCode);
    return *this;
}

const KeyList& SelectorKeys::getKeys() const {
    U_ASSERT(!isBogus());
    return *keys;
}

SelectorKeys::Builder::Builder(UErrorCode& errorCode) {
    if (U_FAILURE(errorCode)) {
        return;
    }
    keys.adoptInstead(KeyList::builder(errorCode));
}

SelectorKeys::SelectorKeys(const SelectorKeys& other) : keys(new KeyList(*(other.keys))) {
    U_ASSERT(!other.isBogus());
}

//------------------ VariableName

UnicodeString VariableName::declaration() const {
    UnicodeString result(DOLLAR);
    result += variableName;
    return result;
}

//------------------ Literal

UnicodeString Literal::quotedString() const {
    UnicodeString result(PIPE);
    result += stringContents();
    result += PIPE;
    return result;
}

const UnicodeString& Literal::stringContents() const {
    U_ASSERT(contents.getType() == Formattable::Type::kString);
    return contents.getString();
}

Literal::~Literal() {}

//------------------ Operand

/* static */ Operand* Operand::create(const VariableName& s, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);
    Operand* result = new Operand(s);
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return result;
}
 
// Literal
/* static */ Operand* Operand::create(const Literal& lit, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);
    Operand* result = new Operand(lit);
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return result;
}

Operand::Operand(const Operand& other) : var(other.var), lit(other.lit), type(other.type) {}

UBool Operand::isVariable() const { return type == Type::VARIABLE; }
UBool Operand::isLiteral() const { return type == Type::LITERAL; }
UBool Operand::isNull() const { return type == Type::NULL_OPERAND; }

const Literal& Operand::asLiteral() const {
    U_ASSERT(isLiteral());
    return lit;
}

const VariableName& Operand::asVariable() const {
    U_ASSERT(isVariable());
    return var;
}

Operand::~Operand() {}

//---------------- Key

/* static */ Key* Key::create(UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);
    Key* k = new Key();
    if (k == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return k;
}

/* static */ Key* Key::create(const Literal& lit, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);
    Key* k = new Key(lit);
    if (k == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return k;
}


void Key::toString(UnicodeString& result) const {
    if (isWildcard()) {
        result += ASTERISK;
        return;
    }
    result += contents.stringContents();
}

const Literal& Key::asLiteral() const {
    U_ASSERT(!isWildcard());
    return contents;
}

//---------------- VariantMap

int32_t VariantMap::size() const {
    return contents->size();
}

// Because ImmutableVector::get() returns a T*,
// the out-parameters for `next()` are references to pointers
// rather than references to a `SelectorKeys` or a `Pattern`,
// in order to avoid either copying or creating a reference to
// a temporary value.
UBool VariantMap::next(int32_t &pos, const SelectorKeys*& k, const Pattern*& v) const {
    UnicodeString unused;
    if (!contents->next(pos, unused, v)) {
        return false;
    }
    k = keyLists->get(pos - 1);
    return true;
}

VariantMap::Builder& VariantMap::Builder::add(SelectorKeys* key, Pattern* value, UErrorCode& errorCode) {
    THIS_ON_ERROR(errorCode);
    // Stringify `key`
    UnicodeString keyResult;
    concatenateKeys(*key, keyResult);
    contents->add(keyResult, value, errorCode);
    keyLists->add(key, errorCode);
    return *this;
}

VariantMap* VariantMap::Builder::build(UErrorCode& errorCode) const {
    NULL_ON_ERROR(errorCode);

    LocalPointer<OrderedMap<Pattern>> adoptedContents(contents->build(errorCode));
    LocalPointer<ImmutableVector<SelectorKeys>> adoptedKeyLists(keyLists->build(errorCode));
    NULL_ON_ERROR(errorCode);
    VariantMap* result = new VariantMap(adoptedContents.orphan(), adoptedKeyLists.orphan());
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return result;
}

/* static */ void VariantMap::Builder::concatenateKeys(const SelectorKeys& keys, UnicodeString& result) {
    const KeyList& ks = keys.getKeys();
    int32_t len = ks.length();
    for (int32_t i = 0; i < len; i++) {
        ks.get(i)->toString(result);
        if (i != len - 1) {
            result += SPACE;
        }
    }
}

VariantMap::Builder::Builder(UErrorCode& errorCode) {
    // initialize `contents`
    // No value comparator needed
    contents.adoptInstead(OrderedMap<Pattern>::builder(errorCode));
    // initialize `keyLists`
    keyLists.adoptInstead(ImmutableVector<SelectorKeys>::builder(errorCode));
    // `keyLists` does not adopt its elements
}

/* static */ VariantMap::Builder* VariantMap::builder(UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);
    LocalPointer<VariantMap::Builder> result(new VariantMap::Builder(errorCode));
    NULL_ON_ERROR(errorCode);
    return result.orphan();
}

VariantMap::VariantMap(OrderedMap<Pattern>* vs, ImmutableVector<SelectorKeys>* ks) : contents(vs), keyLists(ks) {
    // Check invariant: `vs` and `ks` have the same size
    U_ASSERT(vs->size() == ks->length());
}

// ------------ Reserved

int32_t Reserved::numParts() const {
    U_ASSERT(!isBogus());
    return parts->length();
}

// Returns a const Literal* because ImmutableVector::get() returns a pointer
const Literal* Reserved::getPart(int32_t i) const {
    U_ASSERT(!isBogus());
    U_ASSERT(i < numParts());
    return parts->get(i);
}

Reserved::Builder::Builder(UErrorCode &errorCode) {
    CHECK_ERROR(errorCode);
    parts.adoptInstead(ImmutableVector<Literal>::builder(errorCode));
}

Reserved::Builder* Reserved::builder(UErrorCode &errorCode) {
    NULL_ON_ERROR(errorCode);
    LocalPointer<Reserved::Builder> tree(new Builder(errorCode));
    NULL_ON_ERROR(errorCode);
    return tree.orphan();
}

Reserved* Reserved::Builder::build(UErrorCode& errorCode) const {
    NULL_ON_ERROR(errorCode);
    LocalPointer<ImmutableVector<Literal>> reservedParts(parts->build(errorCode));
    NULL_ON_ERROR(errorCode);
    Reserved* result = new Reserved(reservedParts.orphan());
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return result;
}

Reserved::Builder& Reserved::Builder::add(const Literal& part, UErrorCode &errorCode) {
    THIS_ON_ERROR(errorCode);

    LocalPointer<Literal> lit(new Literal(part));
    if (!lit.isValid()) {
      errorCode = U_MEMORY_ALLOCATION_ERROR;
      return *this;
    }
    parts->add(lit.orphan(), errorCode);

    return *this;
}

//------------------------ Operator

const FunctionName& Operator::getFunctionName() const {
    U_ASSERT(!isBogus() && !isReserved());
    return functionName;
}

UnicodeString FunctionName::toString() const {
    UnicodeString result;
    result += sigilChar();
    result += functionName;
    return result;
}

FunctionName::~FunctionName() {}

const Reserved& Operator::asReserved() const {
    U_ASSERT(!isBogus() && isReserved());
    return *reserved;
}

const OptionMap& Operator::getOptions() const {
    U_ASSERT(!isBogus() && !isReserved());
    return *options;
}

// See comments under `SelectorKeys` for why this is here.
// In this case, the invariant is (isReservedSequence && reserved.isValid() && !options.isValid())
//                              || (!isReservedSequence && !reserved.isValid() && options.isValid())
bool Operator::isBogus() const {
    if (isReservedSequence) {
        return !((reserved.isValid() && !options.isValid()));
    }
    return (!(!reserved.isValid() && options.isValid()));
}

Operator::Builder& Operator::Builder::setReserved(Reserved* reserved) {
    U_ASSERT(reserved != nullptr);
    asReserved.adoptInstead(reserved);
    functionName.adoptInstead(nullptr);
    options.adoptInstead(nullptr);
    return *this;
}

Operator::Builder& Operator::Builder::setFunctionName(const FunctionName& func, UErrorCode& errorCode) {
    asReserved.adoptInstead(nullptr);
    functionName.adoptInstead(new FunctionName(func));
    if (!functionName.isValid()) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return *this;
}

Operator::Builder& Operator::Builder::addOption(const UnicodeString &key, Operand* value, UErrorCode& errorCode) {
    THIS_ON_ERROR(errorCode);

    U_ASSERT(value != nullptr);
    asReserved.adoptInstead(nullptr);
    if (!options.isValid()) {
        options.adoptInstead(OptionMap::builder(errorCode));
        THIS_ON_ERROR(errorCode);
    }
    // If the option name is already in the map, emit a data model error
    if (options->has(key)) {
        errorCode = U_DUPLICATE_OPTION_NAME_ERROR;
    } else {
        options->add(key, value, errorCode);
    }
    return *this;
}

Operator* Operator::Builder::build(UErrorCode& errorCode) const {
    NULL_ON_ERROR(errorCode);

    LocalPointer<Operator> result;
    // Must be either reserved or function, not both; enforced by methods
    if (asReserved.isValid()) {
        // Methods enforce that the function name and options are unset
        // if `setReserved()` is called, so if they were valid, that
        // would indicate a bug.
        U_ASSERT(!(functionName.isValid() || options.isValid()));
        result.adoptInstead(Operator::create(*asReserved, errorCode));
    } else {
        if (!functionName.isValid()) {
            // Neither function name nor reserved was set
            // There is no default, so this case could occur if the
            // caller creates a builder and doesn't make any calls
            // before calling build().
            errorCode = U_INVALID_STATE_ERROR;
            return nullptr;
        }
        if (options.isValid()) {
            LocalPointer<OptionMap> opts(options->build(errorCode));
            NULL_ON_ERROR(errorCode);
            result.adoptInstead(Operator::create(*functionName, opts.orphan(), errorCode));
        } else {
            result.adoptInstead(Operator::create(*functionName, nullptr, errorCode));
        }
    }
    NULL_ON_ERROR(errorCode);
    return result.orphan();
}

/* static */ Operator* Operator::create(const Reserved& r, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);

    Operator* result = new Operator(r);
    if (result == nullptr || result->isBogus()) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return result;
}

/* static */ Operator* Operator::create(const FunctionName& f, OptionMap* opts, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);

    // opts may be null -- in that case, we create an empty OptionMap
    // for simplicity
    LocalPointer<OptionMap> adoptedOpts;
    if (opts == nullptr) {
        LocalPointer<OptionMap::Builder> builder(OptionMap::builder(errorCode));
        adoptedOpts.adoptInstead(builder->build(errorCode));
    } else {
        adoptedOpts.adoptInstead(opts);
    }
    NULL_ON_ERROR(errorCode);

    Operator* result = new Operator(f, adoptedOpts.orphan());
    if (result == nullptr || result->isBogus()) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return result;
}

/* static */ Operator::Builder* Operator::builder(UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);

    LocalPointer<Operator::Builder> result(new Operator::Builder());
    if (!result.isValid()) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }
    return result.orphan();
}

Operator::Operator(const Operator& other) : isReservedSequence(other.isReservedSequence),
                                            functionName(other.functionName),
                                            options(isReservedSequence ? nullptr
                                                    : new OptionMap(*other.options)),
                                            reserved(isReservedSequence? new Reserved(*(other.reserved))
                                                     : nullptr) {
    U_ASSERT(!other.isBogus());
}

// Function call constructor; adopts `f` and `l`, which must be non-null
Operator::Operator(const FunctionName& f, OptionMap *l) : isReservedSequence(false), functionName(f), options(l), reserved(nullptr) {
    U_ASSERT(l != nullptr);
 }

// ------------ Expression


UBool Expression::isStandaloneAnnotation() const {
    U_ASSERT(!isBogus());
    return rand->isNull();
}

// Returns true for function calls with operands as well as
// standalone annotations.
// Reserved sequences are not function calls
UBool Expression::isFunctionCall() const {
    U_ASSERT(!isBogus());
    return (rator.isValid() && !rator->isReserved());
}

UBool Expression::isReserved() const {
    U_ASSERT(!isBogus());
    return (rator.isValid() && rator->isReserved());
}

const Operator& Expression::getOperator() const {
    U_ASSERT(isFunctionCall() || isReserved());
    return *rator;
}

// May return null operand
const Operand& Expression::getOperand() const {
    return *rand;
}

Expression::Builder& Expression::Builder::setOperand(Operand* rAnd) {
    U_ASSERT(rAnd != nullptr);
    rand.adoptInstead(rAnd);
    return *this;
}

Expression::Builder& Expression::Builder::setOperator(Operator* rAtor) {
    U_ASSERT(rAtor != nullptr);
    rator.adoptInstead(rAtor);
    return *this;
}

// Postcondition: U_FAILURE(errorCode) || (result != nullptr && !isBogus(result))
Expression* Expression::Builder::build(UErrorCode& errorCode) const {
    NULL_ON_ERROR(errorCode);

    if ((!rand.isValid() || rand->isNull()) && !rator.isValid()) {
        errorCode = U_INVALID_STATE_ERROR;
        return nullptr;
    }
    LocalPointer<Expression> result;
    Operand* exprRand;
    if (rand.isValid()) {
        exprRand = rand.getAlias();
    } else {
        U_ASSERT(rator.isValid());
        exprRand = new Operand();
    }
    if (exprRand == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }

    if (rator.isValid()) {
        result.adoptInstead(new Expression(*rator, *exprRand));
    } else {
        result.adoptInstead(new Expression(*exprRand));
    }
    if (!result.isValid() || result->isBogus()) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }
    return result.orphan();
}

/* static */ Expression::Builder* Expression::builder(UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);

    LocalPointer<Builder> result(new Builder());
    if (!result.isValid()) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }
    return result.orphan();
}

Expression::Expression(const Expression& other) : rator(other.rator.isValid() ? new Operator(*(other.rator)) : nullptr),
                                                  rand(other.rand.isValid() ? new Operand(*(other.rand)) : nullptr) {
    U_ASSERT(!other.isBogus());
    if (other.rator.isValid() && other.rand.isValid()) {
        bogus = !(rator.isValid() && rand.isValid());
        return;
    }
    if (other.rator.isValid()) {
        bogus = !rator.isValid();
        return;
    }
    U_ASSERT(other.rand.isValid());
    bogus = !rand.isValid();
}

bool Expression::isBogus() const {
    if (bogus) {
        return true;
    }
    // Invariant: if the expression is not bogus and it
    // has a non-null operator, that operator is not bogus.
    // (Operands are never bogus.)
    U_ASSERT(!rator.isValid() || !rator->isBogus());
    return false;
}

// ----------- PatternPart

/* static */ PatternPart* PatternPart::create(const UnicodeString& t, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);

    PatternPart* result = new PatternPart(t);
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return result;
}

/* static */ PatternPart* PatternPart::create(Expression* e, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);

    U_ASSERT(e != nullptr);
    LocalPointer<Expression> adoptedExpr(e);
    PatternPart* result = new PatternPart(adoptedExpr.orphan());
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return result;
}

const Expression& PatternPart::contents() const {
    U_ASSERT(!isText() && !isBogus());
    return *expression;
}

// Precondition: isText();
const UnicodeString& PatternPart::asText() const {
    U_ASSERT(isText());
    return text;
}

// ---------------- Pattern

const PatternPart* Pattern::getPart(int32_t i) const {
    U_ASSERT(!isBogus() && i < numParts());
    return parts->get(i);
}

Pattern::Builder::Builder(UErrorCode &errorCode) {
    CHECK_ERROR(errorCode);
    parts.adoptInstead(ImmutableVector<PatternPart>::builder(errorCode));
}

Pattern::Builder* Pattern::builder(UErrorCode &errorCode) {
    NULL_ON_ERROR(errorCode);
    LocalPointer<Pattern::Builder> tree(new Builder(errorCode));
    NULL_ON_ERROR(errorCode);
    return tree.orphan();
}

Pattern* Pattern::Builder::build(UErrorCode& errorCode) const {
    NULL_ON_ERROR(errorCode);
    LocalPointer<ImmutableVector<PatternPart>> patternParts(parts->build(errorCode));
    NULL_ON_ERROR(errorCode);
    Pattern* result = new Pattern(patternParts.orphan());
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return result;
}

Pattern::Builder& Pattern::Builder::add(PatternPart* part, UErrorCode &errorCode) {
    THIS_ON_ERROR(errorCode);

    parts->add(part, errorCode);
    return *this;
}

// ---------------- Binding

/* static */ Binding* Binding::create(const VariableName& var, Expression* e, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);

    Binding *b = new Binding(var, e);
    if (b == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return b;
}

const Expression& Binding::getValue() const {
    U_ASSERT(!isBogus());
    return *value;
}

Binding::Binding(const Binding& other) : var(other.var), value(new Expression(*other.value)) {
    U_ASSERT(!other.isBogus());
}

// --------------- MessageFormatDataModel


// The `hasSelectors()` method is provided so that `getSelectors()`,
// `getVariants()` and `getPattern()` can rely on preconditions
// rather than taking error codes as arguments.
UBool MessageFormatDataModel::hasSelectors() const {
    if (pattern.isValid()) {
        U_ASSERT(!selectors.isValid());
         U_ASSERT(!variants.isValid());
         return false;
    }
    U_ASSERT(selectors.isValid());
    U_ASSERT(variants.isValid());
    return true;
}

const ExpressionList& MessageFormatDataModel::getSelectors() const {
    U_ASSERT(hasSelectors());
    return *selectors;
}

const VariantMap& MessageFormatDataModel::getVariants() const {
    U_ASSERT(hasSelectors());
    return *variants;
}

const Pattern& MessageFormatDataModel::getPattern() const {
    U_ASSERT(!hasSelectors());
    return *pattern;
}

MessageFormatDataModel::Builder::Builder(UErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    selectors.adoptInstead(ExpressionList::builder(errorCode));
    variants.adoptInstead(VariantMap::builder(errorCode));
    locals.adoptInstead(Bindings::builder(errorCode));
}

// Invalidate pattern and create selectors/variants if necessary
void MessageFormatDataModel::Builder::buildSelectorsMessage(UErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    if (pattern.isValid()) {
        pattern.adoptInstead(nullptr);
    }
    if (!selectors.isValid()) {
        U_ASSERT(!variants.isValid());
        selectors.adoptInstead(ExpressionList::builder(errorCode));
        variants.adoptInstead(VariantMap::builder(errorCode));
    } else {
        U_ASSERT(variants.isValid());
    }
}

MessageFormatDataModel::Builder& MessageFormatDataModel::Builder::addLocalVariable(const VariableName&variableName, Expression *expression, UErrorCode &errorCode) {
    THIS_ON_ERROR(errorCode);

    LocalPointer<Binding> b(Binding::create(variableName, expression, errorCode));
    THIS_ON_ERROR(errorCode);
    locals->add(b.orphan(), errorCode);

    return *this;
}

/*
  selector must be non-null
*/
MessageFormatDataModel::Builder& MessageFormatDataModel::Builder::addSelector(Expression* selector, UErrorCode& errorCode) {
    THIS_ON_ERROR(errorCode);

    U_ASSERT(selector != nullptr);
    buildSelectorsMessage(errorCode);

    selectors->add(selector, errorCode);

    return *this;
}

/*
  `keys` and `pattern` must be non-null
  Adopts `keys` and `pattern`
*/
MessageFormatDataModel::Builder& MessageFormatDataModel::Builder::addVariant(SelectorKeys* keys, Pattern* pattern, UErrorCode& errorCode) {
    THIS_ON_ERROR(errorCode);

    U_ASSERT(keys != nullptr);
    U_ASSERT(pattern != nullptr);

    buildSelectorsMessage(errorCode);

    variants->add(keys, pattern, errorCode);

    return *this;
}

MessageFormatDataModel::Builder* MessageFormatDataModel::builder(UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);
    LocalPointer<Builder> result(new Builder(errorCode));
    if (U_FAILURE(errorCode)) {
        return nullptr;
    }
    return result.orphan();
}

MessageFormatDataModel::Builder& MessageFormatDataModel::Builder::setPattern(Pattern* pat) {
    // Can't set pattern to null
    U_ASSERT(pat != nullptr);
    pattern.adoptInstead(pat);
    // Invalidate selectors and variants
    selectors.adoptInstead(nullptr);
    variants.adoptInstead(nullptr);
    return *this;
}

MessageFormatDataModel::MessageFormatDataModel(const MessageFormatDataModel::Builder& builder, UErrorCode &errorCode)
    : selectors(builder.pattern.isValid() ? nullptr : builder.selectors->build(errorCode)),
      variants(builder.pattern.isValid() ? nullptr : builder.variants->build(errorCode)),
      pattern(builder.pattern.isValid() ? new Pattern(*(builder.pattern)) : nullptr),
      bindings(builder.locals->build(errorCode))
{
    CHECK_ERROR(errorCode);

    if (builder.pattern.isValid()) {
        // If `pattern` has been set, then assume this is a Pattern message
        U_ASSERT(!builder.selectors.isValid());
        U_ASSERT(!builder.variants.isValid());
        U_ASSERT(!hasSelectors());
    } else {
        // Otherwise, this is a Selectors message
        U_ASSERT(builder.selectors.isValid());
        U_ASSERT(builder.variants.isValid());
        U_ASSERT(hasSelectors());
    }
}

MessageFormatDataModel* MessageFormatDataModel::Builder::build(UErrorCode &errorCode) const {
    NULL_ON_ERROR(errorCode);

    bool patternValid = pattern.isValid();
    bool selectorsValid = selectors.isValid() && variants.isValid();

    // Either pattern is valid, or both selectors and variants are valid; but not both
    if ((patternValid && selectorsValid)
        || (!patternValid && !selectorsValid)) {
      errorCode = U_INVALID_STATE_ERROR;
      return nullptr;
    }

    // Initialize the data model
    LocalPointer<MessageFormatDataModel> dataModel(new MessageFormatDataModel(*this, errorCode));
    NULL_ON_ERROR(errorCode);
    return dataModel.orphan();
}

MessageFormatDataModel::~MessageFormatDataModel() {}

} // namespace message2

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

