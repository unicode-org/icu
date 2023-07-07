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
template<typename T>
using List            = MessageFormatDataModel::List<T>;
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

#define PARSER MessageFormatter::Parser

UOBJECT_DEFINE_RTTI_IMPLEMENTATION(MessageFormatter)

/*
    The `ERROR()` macro sets `errorCode` to `U_MESSAGE_PARSE_ERROR
    and sets the offset in `parseError` to `index`. It does not alter control flow.

    For now, all parse errors are denoted by U_MESSAGE_PARSE_ERROR.
    common/unicode/utypes.h defines a broader set of formatting errors,
    but it doesn't capture all possible MessageFormat2 errors and until the
    spec is finalized, we'll just use the same error code for all parse errors.
*/
#define ERROR(parseError, errorCode, index)                                                             \
    setParseError(parseError, index);                                                                   \
    errorCode = U_MESSAGE_PARSE_ERROR;

// Returns true iff `index` is a valid index for the string `source`
static bool inBounds(const UnicodeString &source, uint32_t index) {
    return (((int32_t)index) < source.length());
}

// Increments the line number and updates the "characters seen before
// current line" count in `parseError`, iff `source[index]` is a newline
void PARSER::maybeAdvanceLine() {
    if (source[index] == LF) {
        parseError.line++;
        // add 1 to index to get the number of characters seen so far
        // (including the newline)
        parseError.lengthBeforeCurrentLine = index + 1;
    }
}

/*
    Signals an error and returns either if `parseError` already denotes an
    error, or `index` is out of bounds for the string `source`
*/
#define CHECK_BOUNDS(source, index, parseError, errorCode)                                              \
    if (U_FAILURE(errorCode)) {                                                                         \
        return;                                                                                         \
    }                                                                                                   \
    if (!inBounds(source, index)) {                                                                     \
        ERROR(parseError, errorCode, index);                                                            \
        return;                                                                                         \
    }

/*
  Same as CHECK_BOUNDS but returns null
*/
#define CHECK_BOUNDS_NULL(source, index, parseError, errorCode)                                              \
    if (U_FAILURE(errorCode)) {                                                                         \
        return nullptr;                                                                                         \
    }                                                                                                   \
    if (!inBounds(source, index)) {                                                                     \
        ERROR(parseError, errorCode, index);                                                            \
        return nullptr;                                                                                         \
    }

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

    // Set up the empty options map (this is for convenience
    // when formatting selectors)
    LocalPointer<OptionMap::Builder> optsBuilder(OptionMap::builder(success));
    emptyOptionsMap.adoptInstead(optsBuilder->build(success));
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
    
    // Finally, build the data model based on what was parsed
    LocalPointer<MessageFormatDataModel> dataModelPtr(tree->build(success));
    if (U_SUCCESS(success)) {
      dataModel.adoptInstead(dataModelPtr.orphan());
    }
}

MessageFormatter::ResolvedExpression::~ResolvedExpression() {}
MessageFormatDataModel::~MessageFormatDataModel() {}
MessageFormatter::~MessageFormatter() {}

// -------------------------------------
// Helper functions

static void copyContext(const UChar in[U_PARSE_CONTEXT_LEN], UChar out[U_PARSE_CONTEXT_LEN]) {
    for (size_t i = 0; i < U_PARSE_CONTEXT_LEN; i++) {
        out[i] = in[i];
        if (in[i] == '\0') {
            break;
        }
    }
}

/* static */ void PARSER::translateParseError(const MessageParseError &messageParseError, UParseError &parseError) {
    parseError.line = messageParseError.line;
    parseError.offset = messageParseError.offset;
    copyContext(messageParseError.preContext, parseError.preContext);
    copyContext(messageParseError.postContext, parseError.postContext);
}

/* static */ void PARSER::setParseError(MessageParseError &parseError, uint32_t index) {
    // Translate absolute to relative offset
    parseError.offset = index                               // Start with total number of characters seen
                      - parseError.lengthBeforeCurrentLine; // Subtract all characters before the current line
    // TODO: Fill this in with actual pre and post-context
    parseError.preContext[0] = 0;
    parseError.postContext[0] = 0;
}

// -------------------------------------
// Predicates

// Returns true if `c` is in the interval [`first`, `last`]
static bool inRange(UChar32 c, UChar32 first, UChar32 last) {
    U_ASSERT(first < last);
    return c >= first && c <= last;
}

/*
  The following helper predicates should exactly match nonterminals in the MessageFormat 2 grammar:

  `isTextChar()`      : `text-char`
  `isReservedStart()` : `reserved-start`
  `isReservedChar()`  : `reserved-char`
  `isAlpha()`         : `ALPHA`
  `isDigit()`         : `DIGIT`
  `isNameStart()`     : `name-start`
  `isNameChar()`      : `name-char`
  `isUnquotedStart()` : `unquoted-start`
  `isQuotedChar()`    : `quoted-char`
*/
static bool isTextChar(UChar32 c) {
    return inRange(c, 0x0000, 0x005B)    // Omit backslash
           || inRange(c, 0x005D, 0x007A) // Omit {
           || c == 0x007C                // }
           || inRange(c, 0x007E, 0xD7FF) // Omit surrogates
           || inRange(c, 0xE000, 0x10FFFF);
}

static bool isReservedStart(UChar32 c) {
    switch (c) {
    case BANG:
    case AT:
    case POUND:
    case PERCENT:
    case CARET:
    case AMPERSAND:
    case ASTERISK:
    case LESS_THAN:
    case GREATER_THAN:
    case QUESTION:
    case TILDE:
        return true;
    default:
        return false;
    }
}

static bool isReservedChar(UChar32 c) {
    return inRange(c, 0x0000, 0x0008)    // Omit HTAB and LF
           || inRange(c, 0x000B, 0x000C) // Omit CR
           || inRange(c, 0x000E, 0x0019) // Omit SP
           || inRange(c, 0x0021, 0x005B) // Omit backslash
           || inRange(c, 0x005D, 0x007A) // Omit { | }
           || inRange(c, 0x007E, 0xD7FF) // Omit surrogates
           || inRange(c, 0xE000, 0x10FFFF);
}

static bool isAlpha(UChar32 c) { return inRange(c, 0x0041, 0x005A) || inRange(c, 0x0061, 0x007A); }

static bool isDigit(UChar32 c) { return inRange(c, 0x0030, 0x0039); }

static bool isNameStart(UChar32 c) {
    return isAlpha(c) || c == UNDERSCORE || inRange(c, 0x00C0, 0x00D6) || inRange(c, 0x00D8, 0x00F6) ||
           inRange(c, 0x00F8, 0x02FF) || inRange(c, 0x0370, 0x037D) || inRange(c, 0x037F, 0x1FFF) ||
           inRange(c, 0x200C, 0x200D) || inRange(c, 0x2070, 0x218F) || inRange(c, 0x2C00, 0x2FEF) ||
           inRange(c, 0x3001, 0xD7FF) || inRange(c, 0xF900, 0xFDCF) || inRange(c, 0xFDF0, 0xFFFD) ||
           inRange(c, 0x10000, 0xEFFFF);
}

static bool isNameChar(UChar32 c) {
    return isNameStart(c) || isDigit(c) || c == HYPHEN || c == PERIOD || c == COLON || c == 0x00B7 ||
           inRange(c, 0x0300, 0x036F) || inRange(c, 0x203F, 0x2040);
}

static bool isUnquotedStart(UChar32 c) {
    return isNameStart(c) || isDigit(c) || c == PERIOD || c == 0x00B7 ||
           inRange(c, 0x0300, 0x036F) || inRange(c, 0x203F, 0x2040);
}

static bool isQuotedChar(UChar32 c) {
    return inRange(c, 0x0000, 0x005B)    // Omit backslash
           || inRange(c, 0x005D, 0x007B) // Omit pipe
           || inRange(c, 0x007D, 0xD7FF) // Omit surrogates
           || inRange(c, 0xE000, 0x10FFFF);
}

// Returns true iff `c` can begin a `function` nonterminal
static bool isFunctionStart(UChar32 c) {
    switch (c) {
    case COLON:
    case PLUS:
    case HYPHEN: {
        return true;
    }
    default: {
        return false;
    }
    }
}

// Returns true iff `c` can begin an `annotation` nonterminal
static bool isAnnotationStart(UChar32 c) {
    return isFunctionStart(c) || isReservedStart(c);
}

// Returns true iff `c` can begin either a `reserved-char` or `reserved-escape`
// literal
static bool reservedChunkFollows(UChar32 c) {
   switch(c) {
       // reserved-escape
       case BACKSLASH:
       // literal
       case PIPE: {
           return true;
       }
       default: {
           // reserved-char
           return (isReservedChar(c));
       }
    }
}

// -------------------------------------
// Parsing functions

/*
    This is a recursive-descent scannerless parser that,
    with a few exceptions, uses 1 character of lookahead.

All the exceptions involve ambiguities about the meaning of whitespace.

There are four ambiguities in the grammar that can't be resolved with finite
lookahead (since whitespace sequences can be arbitrarily long). They are resolved
with a form of backtracking (early exit). No state needs to be saved/restored
since whitespace doesn't affect the shape of the resulting parse tree, so it's
not true backtracking.

In addition, the grammar has been refactored
in a semantics-preserving way in some cases to make the code easier to structure.

First: variant = when 1*(s key) [s] pattern
   Example: when k     {a}
   When reading the first space after 'k', it's ambiguous whether it's the
   required space before another key, or the optional space before `pattern`.
 (See comments in parseNonEmptyKeys())

Second: expression = "{" [s] (((literal / variable) [s annotation]) / annotation) [s] "}"
        annotation = (function *(s option)) / reserved
   Example: {:f    }
   When reading the first space after 'f', it's ambiguous whether it's the
   required space before an option, or the optional trailing space after an options list
   (in this case, the options list is empty).
 (See comments in parseOptions() -- handling this case also meant it was easier to base
  the code on a slightly refactored grammar, which should be semantically equivalent.)

Third: expression = "{" [s] (((literal / variable) [s annotation]) / annotation) [s] "}"
        annotation = (function *(s option)) / reserved
   Example: {@a }
   Similar to the previous case; see comments in parseReserved()

Fourth: expression = "{" [s] (((literal / variable) [s annotation]) / annotation) [s] "}"
   Example: {|foo|   }
   When reading the first space after the '|', it's ambiguous whether it's the required
   space before an annotation, or the optional trailing space before the '}'.
  (See comments in parseLiteralOrVariableWithAnnotation(); handling this case relies on
  the same grammar refactoring as the second exception.)

    Most functions match a non-terminal in the grammar, except as explained
    in comments.

Unless otherwise noted in a comment, all helper functions that take
    a `source` string, an `index` unsigned int, and an `errorCode` `UErrorCode`
    have the precondition:
      `index` < `source.length()`
    and the postcondition:
      `U_FAILURE(errorCode)` || `index < `source.length()`
*/

/*
  No pre, no post.
  A message may end with whitespace, so `index` may equal `source.length()` on exit.
*/
void PARSER::parseWhitespaceMaybeRequired(bool required, UErrorCode &errorCode) {
    CHECK_ERROR(errorCode);

    bool sawWhitespace = false;

    // The loop exits either when we consume all the input,
    // or when we see a non-whitespace character.
    while (true) {
        // Check if all input has been consumed
        if (!inBounds(source, index)) {
            // If whitespace isn't required -- or if we saw it already --
            // then the caller is responsible for checking this case and
            // setting an error if necessary.
            if (!required || sawWhitespace) {
                // Not an error.
                return;
            }
            // Otherwise, whitespace is required; the end of the input has
            // been reached without whitespace. This is an error.
            ERROR(parseError, errorCode, index);
            return;
        }

        // Input remains; process the next character if it's whitespace,
        // exit the loop otherwise
        if (isWhitespace(source[index])) {
            sawWhitespace = true;
            // Increment line number in parse error if we consume a newline
            maybeAdvanceLine();
            index++;
        } else {
            break;
        }
    }

    if (!sawWhitespace && required) {
        ERROR(parseError, errorCode, index);
    }
}

/*
  No pre, no post, for the same reason as `parseWhitespaceMaybeRequired()`.
*/
void PARSER::parseRequiredWhitespace(UErrorCode &errorCode) {
    parseWhitespaceMaybeRequired(true, errorCode);
    normalizedInput += SPACE;
}

/*
  No pre, no post, for the same reason as `parseWhitespaceMaybeRequired()`.
*/
void PARSER::parseOptionalWhitespace(UErrorCode &errorCode) {
    parseWhitespaceMaybeRequired(false, errorCode);
}

// Consumes a single character, signaling an error if `source[index]` != `c`
void PARSER::parseToken(UChar32 c, UErrorCode &errorCode) {
    CHECK_ERROR(errorCode);

    U_ASSERT(inBounds(source, index));
    if (source[index] == c) {
        index++;
        // Guarantee postcondition
        CHECK_BOUNDS(source, index, parseError, errorCode);
        normalizedInput += c;
        return;
    }
    // Next character didn't match -- error out
    ERROR(parseError, errorCode, index);
}

/*
   Consumes a fixed-length token, signaling an error if the token isn't a prefix of
   the string beginning at `source[index]`
*/
template <size_t N>
void PARSER::parseToken(const UChar32 (&token)[N], UErrorCode &errorCode) {
    CHECK_ERROR(errorCode);

    U_ASSERT(inBounds(source, index));

    size_t tokenPos = 0;
    while (tokenPos < N - 1) {
        if (source[index] != token[tokenPos]) {
            ERROR(parseError, errorCode, index);
            return;
        }
        normalizedInput += token[tokenPos];
        index++;
        // Guarantee postcondition
        CHECK_BOUNDS(source, index, parseError, errorCode);

        tokenPos++;
    }
}

/*
   Consumes optional whitespace, possibly advancing `index` to `index'`,
   then consumes a fixed-length token (signaling an error if the token isn't a prefix of
   the string beginning at `source[index']`),
   then consumes optional whitespace again
*/
template <size_t N>
void PARSER::parseTokenWithWhitespace(const UChar32 (&token)[N],
                                      UErrorCode &errorCode) {
    // No need for error check or bounds check before parseOptionalWhitespace
    parseOptionalWhitespace(errorCode);
    // Establish precondition
    CHECK_BOUNDS(source, index, parseError, errorCode);
    parseToken(token, errorCode);
    parseOptionalWhitespace(errorCode);
    // Guarantee postcondition
    CHECK_BOUNDS(source, index, parseError, errorCode);
}

/*
   Consumes optional whitespace, possibly advancing `index` to `index'`,
   then consumes a single character (signaling an error if it doesn't match
   `source[index']`),
   then consumes optional whitespace again
*/
void PARSER::parseTokenWithWhitespace(UChar32 c,
                                     UErrorCode &errorCode) {
    // No need for error check or bounds check before parseOptionalWhitespace
    parseOptionalWhitespace(errorCode);
    // Establish precondition
    CHECK_BOUNDS(source, index, parseError, errorCode);
    parseToken(c, errorCode);
    parseOptionalWhitespace(errorCode);
    // Guarantee postcondition
    CHECK_BOUNDS(source, index, parseError, errorCode);
}

/*
  Consumes a non-empty sequence of `name-char`s, the first of which is
  also a `name-start`.
  that begins with a character `start` such that `isNameStart(start)`.

  Initializes `name` to this sequence.

  (Matches the `name` nonterminal in the grammar.)
*/
void PARSER::parseName(UErrorCode &errorCode,
                       VariableName &name) {
    CHECK_ERROR(errorCode);

    U_ASSERT(inBounds(source, index));

    if (!isNameStart(source[index])) {
        ERROR(parseError, errorCode, index);
        return;
    }

    while (isNameChar(source[index])) {
        name += source[index];
        normalizedInput += source[index];
        index++;
        CHECK_BOUNDS(source, index, parseError, errorCode);
    }
}

/*
  Consumes a '$' followed by a `name`, initializing `var` to `name`.

  (Matches the `variable` nonterminal in the grammar.)
*/
void PARSER::parseVariableName(UErrorCode &errorCode,
                               VariableName &var) {
    CHECK_ERROR(errorCode);

    U_ASSERT(inBounds(source, index));
    parseToken(DOLLAR, errorCode);
    CHECK_BOUNDS(source, index, parseError, errorCode);
    parseName(errorCode, var);
}

static FunctionName::Sigil functionSigil(UChar32 c) {
    switch (c) {
        case PLUS:   { return FunctionName::Sigil::OPEN; }
        case HYPHEN: { return FunctionName::Sigil::CLOSE; }
        default: {
            U_ASSERT(c == COLON);
            return FunctionName::Sigil::DEFAULT;
        }
    }
}
/*
  Consumes a reference to a function, matching the `function` nonterminal in
  the grammar.

  Initializes `func` to this name.
*/
FunctionName* PARSER::parseFunction(UErrorCode &errorCode) {
    NULL_ON_ERROR(errorCode);

    U_ASSERT(inBounds(source, index));
    if (!isFunctionStart(source[index])) {
        ERROR(parseError, errorCode, index);
        return nullptr;
    }

    FunctionName::Sigil sigil = functionSigil(source[index]);
    normalizedInput += source[index];
    index++; // Consume the function start character
    CHECK_BOUNDS_NULL(source, index, parseError, errorCode);
    UnicodeString name;
    parseName(errorCode, name);
    FunctionName* result = new FunctionName(name, sigil);
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return result;
}


/*
  Precondition: source[index] == BACKSLASH

  Consume an escaped character.

  Generalized to handle `reserved-escape`, `text-escape`,
  or `literal-escape`, depending on the `kind` argument.

  Appends result to `str`
*/
void PARSER::parseEscapeSequence(EscapeKind kind,
                                 UErrorCode &errorCode,
                                 String &str) {
    CHECK_ERROR(errorCode);

    U_ASSERT(inBounds(source, index));
    U_ASSERT(source[index] == BACKSLASH);
    normalizedInput += BACKSLASH;
    index++; // Skip the initial backslash
    CHECK_BOUNDS(source, index, parseError, errorCode);

    #define SUCCEED \
       /* Append to the output string */                    \
       str += source[index];                                \
       /* Update normalizedInput */                         \
       normalizedInput += source[index];                    \
       /* Consume the character */                          \
       index++;                                             \
       /* Guarantee postcondition */                        \
       CHECK_BOUNDS(source, index, parseError, errorCode);  \
       return;

    // Expect a '{', '|' or '}'
    switch (source[index]) {
    case LEFT_CURLY_BRACE:
    case RIGHT_CURLY_BRACE: {
        // Allowed in a `text-escape` or `reserved-escape`
        switch (kind) {
        case TEXT:
        case RESERVED: {
            SUCCEED;
        }
        default: {
            break;
        }
        }
        break;
    }
    case PIPE: {
        // Allowed in a `literal-escape` or `reserved-escape`
        switch (kind) {
           case LITERAL:
           case RESERVED: {
               SUCCEED;
           }
           default: {
               break;
           }
        }
        break;
    }
   case BACKSLASH: {
       // Allowed in any escape sequence
       SUCCEED;
   }
   default: {
        // No other characters are allowed here
        break;
    }
   }
   // If control reaches here, there was an error
   ERROR(parseError, errorCode, index);
}

/*
  Consume an escaped pipe or backslash, matching the `literal-escape`
  nonterminal in the grammar
*/
void PARSER::parseLiteralEscape(UErrorCode &errorCode,
                                String &str) {
    parseEscapeSequence(LITERAL, errorCode, str);
}

/*
  Consume a literal, matching the `literal` nonterminal in the grammar.
  May be quoted or unquoted -- returns true iff quoted
*/
void PARSER::parseLiteral(UErrorCode &errorCode, bool& quoted, String& contents) {
    CHECK_ERROR(errorCode);
    U_ASSERT(inBounds(source, index));

    // Parse the opening '|' if present
    if (source[index] == PIPE) {
        quoted = true;
        parseToken(PIPE, errorCode);
        CHECK_BOUNDS(source, index, parseError, errorCode);
    } else {
        if (!isUnquotedStart(source[index])) {
            ERROR(parseError, errorCode, index);
            return;
        }
        quoted = false;
    }

    // Parse the contents
    bool done = false;
    while (!done) {
        if (quoted && source[index] == BACKSLASH) {
            parseLiteralEscape(errorCode, contents);
        } else if ((!quoted && isNameChar(source[index]))
                   || (quoted && isQuotedChar(source[index]))) {
            contents += source[index];
            normalizedInput += source[index];
            index++; // Consume this character
            maybeAdvanceLine();
        } else {
          // Assume the sequence of literal characters ends here
          done = true;
        }
        CHECK_BOUNDS(source, index, parseError, errorCode);
    }

    // Parse the closing '|' if we saw an opening '|'
    if (quoted) {
        parseToken(PIPE, errorCode);
    }

    // Guarantee postcondition
    CHECK_BOUNDS(source, index, parseError, errorCode);
}

/*
  Consume a name-value pair, matching the `option` nonterminal in the grammar.

  Adds the option to `optionList`
*/
void PARSER::parseOption(UErrorCode &errorCode,
                         Operator::Builder &builder) {
    CHECK_ERROR(errorCode);

    U_ASSERT(inBounds(source, index));

    // Parse LHS
    String lhs;
    parseName(errorCode, lhs);

    // Parse '='
    parseTokenWithWhitespace(EQUALS, errorCode);

    String rhsStr;
    LocalPointer<Operand> rand;
    // Parse RHS, which is either a literal or variable
    switch (source[index]) {
    case DOLLAR: {
        parseVariableName(errorCode, rhsStr);
        rand.adoptInstead(Operand::create(rhsStr, errorCode));
        break;
    }
    default: {
        // Must be a literal
        bool isQuoted;
        parseLiteral(errorCode, isQuoted, rhsStr);
        Literal lit(isQuoted, rhsStr);
        rand.adoptInstead(Operand::create(lit, errorCode));
        break;
    }
    }
    // Finally, add the key=value mapping
    CHECK_ERROR(errorCode);
    builder.addOption(lhs, rand.orphan(), errorCode);
}

/*
  Consume optional whitespace followed by a sequence of options
  (possibly empty), separated by whitespace
*/
void PARSER::parseOptions(UErrorCode &errorCode, Operator::Builder& builder) {
    CHECK_ERROR(errorCode);

    U_ASSERT(inBounds(source, index));

/*
Arbitrary lookahead is required to parse option lists. To see why, consider
these rules from the grammar:

expression = "{" [s] (((literal / variable) [s annotation]) / annotation) [s] "}"
annotation = (function *(s option)) / reserved

And this example:
{:foo  }

Derivation:
expression -> "{" [s] (((literal / variable) [s annotation]) / annotation) [s] "}"
           -> "{" [s] annotation [s] "}"
           -> "{" [s] ((function *(s option)) / reserved) [s] "}"
           -> "{" [s] function *(s option) [s] "}"

In this example, knowing whether to expect a '}' or the start of another option
after the whitespace would require arbitrary lookahead -- in other words, which
rule should we apply?
    *(s option) -> s option *(s option)
  or
    *(s option) ->

The same would apply to the example {:foo k=v } (note the trailing space after "v").

This is addressed using a form of backtracking and (to make the backtracking easier
to apply) a slight refactoring to the grammar.

This code is written as if the grammar is:
  expression = "{" [s] (((literal / variable) ([s] / [s annotation])) / annotation) "}"
  annotation = (function *(s option) [s]) / (reserved [s])

Parsing the `*(s option) [s]` sequence can be done within `parseOptions()`, meaning
that `parseExpression()` can safely require a '}' after `parseOptions()` finishes.

Note that when "backtracking" really just means early exit, since only whitespace
is involved and there's no state to save.
*/

    while(true) {
        // If the next character is not whitespace, that means we've already
        // parsed the entire options list (which may have been empty) and there's
        // no trailing whitespace. In that case, exit.
        if (!isWhitespace(source[index])) {
            break;
        }

        // In any case other than an empty options list, there must be at least
        // one whitespace character.
        parseRequiredWhitespace(errorCode);
        // Restore precondition
        CHECK_BOUNDS(source, index, parseError, errorCode);

        // If a name character follows, then at least one more option remains
        // in the list.
        // Otherwise, we've consumed all the options and any trailing whitespace,
        // and can exit.
        // Note that exiting is sort of like backtracking: "(s option)" doesn't apply,
        // so we back out to [s].
        if (!isNameStart(source[index])) {
            // We've consumed all the options (meaning that either we consumed non-empty
            // whitespace, or consumed at least one option.)
            // Done.
            // Remove the whitespace from normalizedInput
            U_ASSERT(normalizedInput.truncate(normalizedInput.length() - 1));
            break;
        }
        parseOption(errorCode, builder);
    }
}

void PARSER::parseReservedEscape(UErrorCode &errorCode,
                                 String &str) {
    parseEscapeSequence(RESERVED, errorCode, str);
}

/*
  Consumes a non-empty sequence of reserved-chars, reserved-escapes, and
  literals (as in 1*(reserved-char / reserved-escape / literal) in the `reserved-body` rule)

  Appends it to `str`
*/
void PARSER::parseReservedChunk(UErrorCode &errorCode,
                                Reserved::Builder& result) {
    CHECK_ERROR(errorCode);

    bool empty = true;
    String chunk;
    while(reservedChunkFollows(source[index])) {
        empty = false;
        // reserved-char
        if (isReservedChar(source[index])) {
            chunk += source[index];
            normalizedInput += source[index];
            // consume the char
            index++;
            // Restore precondition
            CHECK_BOUNDS(source, index, parseError, errorCode);
            continue;
        }

        if (chunk.length() > 0) {
          Literal lit(false, chunk);
          result.add(lit, errorCode);
          CHECK_ERROR(errorCode);
          chunk.setTo(u"", 0);
        }

        if (source[index] == BACKSLASH) {
            // reserved-escape
            parseReservedEscape(errorCode, chunk);
            Literal lit(false, chunk);
            result.add(lit, errorCode);
            CHECK_ERROR(errorCode);
            chunk.setTo(u"", 0);
        } else if (source[index] == PIPE || isUnquotedStart(source[index])) {
            String s;
            bool isQuoted;
            parseLiteral(errorCode, isQuoted, s);
            Literal lit(isQuoted, s);
            result.add(lit, errorCode);
            CHECK_ERROR(errorCode);
        } else {
            // The reserved chunk ends here
            break;
        }
    }

    // Add the last chunk if necessary
    if (chunk.length() > 0) {
        Literal lit(false, chunk);
        result.add(lit, errorCode);
    }

    if (empty) {
        ERROR(parseError, errorCode, index);
    }
}

/*
  Consume a `reserved-start` character followed by a possibly-empty sequence
  of non-empty sequences of reserved characters, separated by whitespace.
  Matches the `reserved` nonterminal in the grammar

*/
Reserved* PARSER::parseReserved(UErrorCode &errorCode) {
    NULL_ON_ERROR(errorCode);

    U_ASSERT(inBounds(source, index));

    LocalPointer<Reserved::Builder> builder(Reserved::builder(errorCode));
    NULL_ON_ERROR(errorCode);

    // Require a `reservedStart` character
    if (!isReservedStart(source[index])) {
        ERROR(parseError, errorCode, index);
        return nullptr;
    }

    // Add the start char as a separate text chunk
    String firstCharString(source[index]);
    Literal firstChunk(false, firstCharString);
    builder->add(firstChunk, errorCode);
    NULL_ON_ERROR(errorCode);
    // Consume reservedStart
    normalizedInput += source[index];
    index++;
    // Restore precondition
    CHECK_BOUNDS_NULL(source, index, parseError, errorCode);

/*
  Arbitrary lookahead is required to parse a `reserved`, for similar reasons
  to why it's required for parsing function annotations.

  In the grammar:

  annotation = (function *(s option)) / reserved
  expression = "{" [s] (((literal / variable) [s annotation]) / annotation) [s] "}"
  reserved       = reserved-start reserved-body
  reserved-body  = *( [s] 1*(reserved-char / reserved-escape / literal))

  When reading a whitespace character, it's ambiguous whether it's the optional
  whitespace in this rule, or the optional whitespace that precedes a '}' in an
  expression.

  The ambiguity is resolved using the same grammar refactoring as shown in
  the comment in `parseOptions()`.
*/
    // Consume reserved characters / literals / reserved escapes
    // until a character that can't be in a `reserved-body` is seen
    while (true) {
        /*
          First, if there is whitespace, it means either a chunk follows it,
          or this is the trailing whitespace before the '}' that terminates an
          expression.

          Next, if the next character can start a reserved-char, reserved-escape,
          or literal, then parse a "chunk" of reserved things.
          In any other case, we exit successfully, since per the refactored
          grammar rule:
               annotation = (function *(s option) [s]) / (reserved [s])
          it's valid to consume whitespace after a `reserved`.
          (`parseExpression()` is responsible for checking that the next
          character is in fact a '}'.)
         */
        bool sawWhitespace = false;
        if (isWhitespace(source[index])) {
            sawWhitespace = true;
            parseOptionalWhitespace(errorCode);
            // Restore precondition
            CHECK_BOUNDS_NULL(source, index, parseError, errorCode);
        }

        if (reservedChunkFollows(source[index])) {
            parseReservedChunk(errorCode, *builder);

            // Avoid looping infinitely
            CHECK_BOUNDS_NULL(source, index, parseError, errorCode);
        } else {
            if (sawWhitespace) {
                if (source[index] == RIGHT_CURLY_BRACE) {
                    // Not an error: just means there's no trailing whitespace
                    // after this `reserved`
                    break;
                }
                // Error: if there's whitespace, it must either be followed
                // by a non-empty sequence or by '}'
                ERROR(parseError, errorCode, index);
                return nullptr;
            }
            // If there was no whitespace, it's not an error,
            // just the end of the reserved string
            break;
        }
    }

    LocalPointer<Reserved> r(builder->build(errorCode));
    NULL_ON_ERROR(errorCode);
    return r.orphan();
}


/*
  Consume a function call or reserved string, matching the `annotation`
  nonterminal in the grammar

  Returns an `Operator` representing this (a reserved is a parse error)
*/
Operator* PARSER::parseAnnotation(UErrorCode &errorCode) {
    NULL_ON_ERROR(errorCode);

    U_ASSERT(inBounds(source, index));
    Operator::Builder* ratorBuilder(Operator::builder(errorCode));
    NULL_ON_ERROR(errorCode);
    if (isFunctionStart(source[index])) {
        // Consume the function name
        LocalPointer<FunctionName> func(parseFunction(errorCode));
        NULL_ON_ERROR(errorCode);
        ratorBuilder->setFunctionName(func.orphan());
        // Consume the options (which may be empty)
        parseOptions(errorCode, *ratorBuilder);
    } else {
      // Must be reserved
      // A reserved sequence is not a parse error, but might be a formatting error
      LocalPointer<Reserved> rator(parseReserved(errorCode));
      NULL_ON_ERROR(errorCode);
      ratorBuilder->setReserved(rator.orphan());
    }
    return ratorBuilder->build(errorCode);
}

/*
  Consume a literal or variable (depending on `isVariable`),
  followed by either required whitespace followed by an annotation,
  or optional whitespace.
*/
void PARSER::parseLiteralOrVariableWithAnnotation(bool isVariable,
                                                  UErrorCode &errorCode,
                                                  Expression::Builder& builder) {
    CHECK_ERROR(errorCode);

    U_ASSERT(inBounds(source, index));

    LocalPointer<Operand> adoptedRand;
    if (isVariable) {
        VariableName var;
        parseVariableName(errorCode, var);
        adoptedRand.adoptInstead(Operand::create(var, errorCode));
    } else {
        String s;
        bool isQuoted;
        parseLiteral(errorCode, isQuoted, s);
        Literal lit(isQuoted, s);
        adoptedRand.adoptInstead(Operand::create(lit, errorCode));
    }

    // Set the operand (if allocation succeeded)
    CHECK_ERROR(errorCode);
    builder.setOperand(adoptedRand.orphan());

/*
Parsing a literal or variable with an optional annotation requires arbitrary lookahead.
To see why, consider this rule from the grammar:

expression = "{" [s] (((literal / variable) [s annotation]) / annotation) [s] "}"

And this example:

{|foo|   }

Derivation:
expression -> "{" [s] (((literal / variable) [s annotation]) / annotation) [s] "}"
           -> "{" [s] ((literal / variable) [s annotation]) [s] "}"
           -> "{" [s] (literal [s annotation]) [s] "}"

When reading the ' ' after the second '|', it's ambiguous whether that's the required
space before an annotation, or the optional space before the '}'.

To make this ambiguity easier to handle, this code is based on the same grammar
refactoring for the `expression` nonterminal that `parseOptions()` relies on. See
the comment in `parseOptions()` for details.
*/

    LocalPointer<Expression> result;
    if (isWhitespace(source[index])) {
      // If the next character is whitespace, either [s annotation] or [s] applies
      // (the character is either the required space before an annotation, or optional
      // trailing space after the literal or variable). It's still ambiguous which
      // one does apply.
      parseOptionalWhitespace(errorCode);
      // Restore precondition
      CHECK_BOUNDS(source, index, parseError, errorCode);

      // This next check resolves the ambiguity between [s annotation] and [s]
      if (isAnnotationStart(source[index])) {
        normalizedInput += SPACE;
        // The previously consumed whitespace precedes an annotation
        LocalPointer<Operator> adoptedRator(parseAnnotation(errorCode));
        CHECK_ERROR(errorCode);
        builder.setOperator(adoptedRator.orphan());
      }
    } else {
      // Either there was never whitespace, or
      // the previously consumed whitespace is the optional trailing whitespace;
      // either the next character is '}' or the error will be handled by parseExpression.
      // Do nothing, since the operand was already set      
    }
}

/*
  Consume an expression, matching the `expression` nonterminal in the grammar
*/
Expression* PARSER::parseExpression(UErrorCode &errorCode) {
    NULL_ON_ERROR(errorCode);

    U_ASSERT(inBounds(source, index));
    // Parse opening brace
    parseToken(LEFT_CURLY_BRACE, errorCode);
    // Optional whitespace after opening brace
    parseOptionalWhitespace(errorCode);
    // Restore precondition
    CHECK_BOUNDS_NULL(source, index, parseError, errorCode);

    LocalPointer<Expression::Builder> exprBuilder(Expression::builder(errorCode));
    NULL_ON_ERROR(errorCode);
    // literal '|', variable '$' or annotation
    switch (source[index]) {
    case PIPE: {
        // Quoted literal
        parseLiteralOrVariableWithAnnotation(false, errorCode, *exprBuilder);
        break;
    }
    case DOLLAR: {
        // Variable
        parseLiteralOrVariableWithAnnotation(true, errorCode, *exprBuilder);
        break;
    }
    default: {
        if (isAnnotationStart(source[index])) {
            LocalPointer<Operator> rator(parseAnnotation(errorCode));
            NULL_ON_ERROR(errorCode);
            exprBuilder->setOperator(rator.orphan());
        } else if (isUnquotedStart(source[index])) {
            // Unquoted literal
            parseLiteralOrVariableWithAnnotation(false, errorCode, *exprBuilder);
        } else {
            // Not a literal, variable or annotation -- error out
            ERROR(parseError, errorCode, index);
            return nullptr;
        }
        break;
    }
    }
    // For why we don't parse optional whitespace here, even though the grammar
    // allows it, see comments in parseLiteralWithAnnotation() and parseOptions()

    // Parse closing brace
    parseToken(RIGHT_CURLY_BRACE, errorCode);

    if (U_FAILURE(errorCode)) {
        return nullptr;
    }
    return exprBuilder->build(errorCode);
}

/*
  Consume a possibly-empty sequence of declarations separated by whitespace;
  each declaration matches the `declaration` nonterminal in the grammar

  Builds up an environment representing those declarations
*/
void PARSER::parseDeclarations(UErrorCode &errorCode) {
    CHECK_ERROR(errorCode);

    // End-of-input here would be an error; even empty
    // declarations must be followed by a body
    CHECK_BOUNDS(source, index, parseError, errorCode);

    while (source[index] == ID_LET[0]) {
        parseToken(ID_LET, errorCode);
        parseRequiredWhitespace(errorCode);
        // Restore precondition
        CHECK_BOUNDS(source, index, parseError, errorCode);
        VariableName lhs;
        parseVariableName(errorCode, lhs);
        parseTokenWithWhitespace(EQUALS, errorCode);
        LocalPointer<Expression> rhs(parseExpression(errorCode));
        parseOptionalWhitespace(errorCode);
        // Restore precondition
        CHECK_BOUNDS(source, index, parseError, errorCode);

        if (U_FAILURE(errorCode)) {
            return;
        }
        // Add binding from lhs to rhs
        dataModel.addLocalVariable(lhs, rhs.orphan(), errorCode);
    }
}

/*
  Consume an escaped curly brace, or backslash, matching the `text-escape`
  nonterminal in the grammar
*/
void PARSER::parseTextEscape(UErrorCode &errorCode, String &str) {
    parseEscapeSequence(TEXT, errorCode, str);
}

/*
  Consume a non-empty sequence of text characters and escaped text characters,
  matching the `text` nonterminal in the grammar
*/
void PARSER::parseText(UErrorCode &errorCode, String &str) {
    CHECK_ERROR(errorCode)
    U_ASSERT(inBounds(source, index));
    bool empty = true;

    while (true) {
        if (source[index] == BACKSLASH) {
            parseTextEscape(errorCode, str);
        } else if (isTextChar(source[index])) {
            normalizedInput += source[index];
            str += source[index];
            index++;
            maybeAdvanceLine();
        } else {
            break;
        }
        // Restore precondition
        CHECK_BOUNDS(source, index, parseError, errorCode);
        empty = false;
    }

    if (empty) {
        // text must be non-empty
        ERROR(parseError, errorCode, index);
    }
}

/*
  Consume an `nmtoken`, `literal`, or the string "*", matching
  the `key` nonterminal in the grammar
*/
Key* PARSER::parseKey(UErrorCode &errorCode) {
    NULL_ON_ERROR(errorCode);
    U_ASSERT(inBounds(source, index));

    LocalPointer<Key> k;
    // Literal | '*'
    switch (source[index]) {
    case ASTERISK: {
        index++;
        // Guarantee postcondition
        CHECK_BOUNDS_NULL(source, index, parseError, errorCode);
        k.adoptInstead(Key::create(errorCode));
        normalizedInput += ASTERISK;
        break;
    }
    default: {
        // Literal
        String s;
        bool isQuoted;
        parseLiteral(errorCode, isQuoted, s);
        Literal lit(isQuoted, s);
        k.adoptInstead(Key::create(lit, errorCode));
        break;
    }
    }

    NULL_ON_ERROR(errorCode);
    return k.orphan();
}

MessageFormatDataModel::SelectorKeys::Builder* MessageFormatDataModel::SelectorKeys::builder(UErrorCode &errorCode) {
    NULL_ON_ERROR(errorCode);
    LocalPointer<MessageFormatDataModel::SelectorKeys::Builder> result(new MessageFormatDataModel::SelectorKeys::Builder(errorCode));
    if (U_FAILURE(errorCode)) {
        return nullptr;
    }
    return result.orphan();        
}

MessageFormatDataModel::SelectorKeys* MessageFormatDataModel::SelectorKeys::Builder::build(UErrorCode &errorCode) const {
    NULL_ON_ERROR(errorCode);

    LocalPointer<KeyList> ks(keys->build(errorCode));
    NULL_ON_ERROR(errorCode);
    // Key list must be non-empty (this should be checked earlier on)
    U_ASSERT(ks->length() >= 1);
    SelectorKeys* result = new SelectorKeys(ks.orphan());
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return result;
}

/*
  Consume a non-empty sequence of `key`s separated by whitespace

  Takes ownership of `keys`
*/
MessageFormatDataModel::SelectorKeys* PARSER::parseNonEmptyKeys(UErrorCode &errorCode) {
    NULL_ON_ERROR(errorCode);
    U_ASSERT(inBounds(source, index));

/*
Arbitrary lookahead is required to parse key lists. To see why, consider
this rule from the grammar:

variant = when 1*(s key) [s] pattern

And this example:
when k1    {a}

Derivation:
   variant -> when 1*(s key) [s] pattern
           -> when s key *(s key) [s] pattern

After matching ' ' to `s` and 'k1' to `key`, it would require arbitrary lookahead
to know whether to expect the start of a pattern or the start of another key.
In other words: is the second whitespace sequence the required space in 1*(s key),
or the optional space in [s] pattern?

This is addressed using "backtracking" (similarly to `parseOptions()`).
*/

    LocalPointer<MessageFormatDataModel::SelectorKeys::Builder> keysBuilder(MessageFormatDataModel::SelectorKeys::builder(errorCode));
    NULL_ON_ERROR(errorCode);

    // Since the first key is required, it's simplest to parse the required
    // whitespace and then the first key separately.
    parseRequiredWhitespace(errorCode);
    // Restore precondition
    CHECK_BOUNDS_NULL(source, index, parseError, errorCode);
    LocalPointer<Key> k(parseKey(errorCode));
    if (U_SUCCESS(errorCode)) {
        keysBuilder->add(k.orphan(), errorCode);
    }

    // We've seen at least one whitespace-key pair, so now we can parse
    // *(s key) [s]
    while (isWhitespace(source[index])) {
        parseRequiredWhitespace(errorCode);
        // Restore precondition
        CHECK_BOUNDS_NULL(source, index, parseError, errorCode);

        // At this point, it's ambiguous whether we are inside (s key) or [s].
        // This check resolves that ambiguity.
        if (source[index] == LEFT_CURLY_BRACE) {
            // A pattern follows, so what we just parsed was the optional
            // trailing whitespace. All the keys have been parsed.

            // Unpush the whitespace from `normalizedInput`
            U_ASSERT(normalizedInput.truncate(normalizedInput.length() - 1));
            break;
        }
        k.adoptInstead(parseKey(errorCode));
        if (U_SUCCESS(errorCode)) {
            keysBuilder->add(k.orphan(), errorCode);
        }
    }

    // Check error code so we won't overwrite the error
    if (U_FAILURE(errorCode)) {
        return nullptr;
    }

    return keysBuilder->build(errorCode);
}

/*
  Consume a `pattern`, matching the nonterminal in the grammar
  No postcondition (on return, `index` might equal `source.length()` with U_SUCCESS(errorCode)),
  because a message can end with a pattern
*/
Pattern* PARSER::parsePattern(UErrorCode &errorCode) {
    NULL_ON_ERROR(errorCode);
    U_ASSERT(inBounds(source, index));
    
    LocalPointer<Pattern::Builder> result(Pattern::builder(errorCode));
    // Fail immediately if the pattern builder can't be constructed
    NULL_ON_ERROR(errorCode);

    parseToken(LEFT_CURLY_BRACE, errorCode);

    LocalPointer<Expression> expression;
    LocalPointer<PatternPart> part;
    while (source[index] != RIGHT_CURLY_BRACE) {
        switch (source[index]) {
        case LEFT_CURLY_BRACE: {
            // Must be expression
            expression.adoptInstead(parseExpression(errorCode));
            NULL_ON_ERROR(errorCode);
            part.adoptInstead(PatternPart::create(expression.orphan(), errorCode));
            NULL_ON_ERROR(errorCode);
            result->add(part.orphan(), errorCode);
            break;
        }
        default: {
            // Must be text
            String s;
            parseText(errorCode, s);
            part.adoptInstead(PatternPart::create(s, errorCode));
            NULL_ON_ERROR(errorCode);
            result->add(part.orphan(), errorCode);
            break;
        }
        }
        // Need an explicit error check here so we don't loop infinitely
        NULL_ON_ERROR(errorCode);
    }
    // Consume the closing brace
    index++;
    normalizedInput += RIGHT_CURLY_BRACE;

    return result->build(errorCode);
}


/*
  Consume a `selectors` (matching the nonterminal in the grammar),
  followed by a non-empty sequence of `variant`s (matching the nonterminal
  in the grammar) preceded by whitespace
  No postcondition (on return, `index` might equal `source.length()` with U_SUCCESS(errorCode)),
  because a message can end with a variant
*/
void PARSER::parseSelectors(UErrorCode &errorCode) {
    CHECK_ERROR(errorCode);
    U_ASSERT(inBounds(source, index));

    parseToken(ID_MATCH, errorCode);

    LocalPointer<Expression> expression;
    bool empty = true;
    // Parse selectors
    while (isWhitespace(source[index]) || source[index] == LEFT_CURLY_BRACE) {
        parseOptionalWhitespace(errorCode);
        // Restore precondition
        CHECK_BOUNDS(source, index, parseError, errorCode);
        if (source[index] != LEFT_CURLY_BRACE) {
            // This is not necessarily an error, but rather,
            // means the whitespace we parsed was the optional
            // whitespace preceding the first variant, not the
            // optional whitespace preceding a subsequent expression.
            break;
        }

        expression.adoptInstead(parseExpression(errorCode));
        empty = false;

        if (U_FAILURE(errorCode)) {
            break;
        }
        dataModel.addSelector(expression.orphan(), errorCode);
    }

    // At least one selector is required
    if (empty) {
        if (U_SUCCESS(errorCode)) {
            ERROR(parseError, errorCode, index);
        }
        return;
    }

    #define CHECK_END_OF_INPUT                     \
        if (((int32_t)index) >= source.length()) { \
            break;                                 \
        }                                          \

    // Parse variants
    while (isWhitespace(source[index]) || source[index] == ID_WHEN[0]) {
        parseOptionalWhitespace(errorCode);
        // Restore the precondition, *without* erroring out if we've
        // reached the end of input. That's because it's valid for the
        // message to end with trailing whitespace that follows a variant.
        CHECK_END_OF_INPUT

        // Consume the "when"
        parseToken(ID_WHEN, errorCode);

        // At least one key is required
        LocalPointer<MessageFormatDataModel::SelectorKeys> keyList(parseNonEmptyKeys(errorCode));
        CHECK_ERROR(errorCode);

        // parseNonEmptyKeys() consumes any trailing whitespace,
        // so the pattern can be consumed next.
        LocalPointer<Pattern> rhs(parsePattern(errorCode));
        if (U_FAILURE(errorCode)) {
            break;
        }

        dataModel.addVariant(keyList.orphan(), rhs.orphan(), errorCode);
        if (U_FAILURE(errorCode)) {
            break;
        }

        // Restore the precondition, *without* erroring out if we've
        // reached the end of input. That's because it's valid for the
        // message to end with a variant that has no trailing whitespace.
        // Why do we need to check this condition twice inside the loop?
        // Because if we don't check it here, the `isWhitespace()` call in
        // the loop head will read off the end of the input string.
        CHECK_END_OF_INPUT
    }
}

/*
  Consume a `body` (matching the nonterminal in the grammar),
  No postcondition (on return, `index` might equal `source.length()` with U_SUCCESS(errorCode)),
  because a message can end with a body (trailing whitespace is optional)
*/
void PARSER::parseBody(UErrorCode &errorCode) {
    CHECK_ERROR(errorCode);
    U_ASSERT(inBounds(source, index));

    // Body must be either a pattern or selectors
    switch (source[index]) {
    case LEFT_CURLY_BRACE: {
        // Pattern
        LocalPointer<Pattern> pattern(parsePattern(errorCode));
        if (U_FAILURE(errorCode)) {
            return;
        }
        dataModel.setPattern(pattern.orphan());
        break;
    }
    case ID_MATCH[0]: {
        // Selectors
        parseSelectors(errorCode);
        break;
    }
    default: {
        ERROR(parseError, errorCode, index);
        break;
    }
    }
}

// Deep copy methods for AST nodes
//

/*
template<>
void copyElements<Expression>(UElement *dst, UElement *src) {
    dst->pointer = new Expression(*(static_cast<Expression*>(src->pointer)));
}

template<>
void copyElements<PatternPart>(UElement *dst, UElement *src) {
    dst->pointer = new PatternPart(*(static_cast<PatternPart*>(src->pointer)));
}
*/
                    
// Not yet implemented
MessageFormatter* MessageFormatter::clone() const { return nullptr; }

// Not yet implemented
bool MessageFormatter::operator==(const Format &other) const { return (this == &other); }
// Not yet implemented
bool MessageFormatter::operator!=(const Format &other) const { return (this != &other); }

// Not yet implemented
UnicodeString &MessageFormatter::format(const Formattable &, UnicodeString &appendTo, FieldPosition &,
                                      UErrorCode &status) const {
    status = U_UNSUPPORTED_ERROR;
    return appendTo;
}

// Not yet implemented
void MessageFormatter::parseObject(const UnicodeString &, Formattable &, ParsePosition &status) const {
    status = U_UNSUPPORTED_ERROR;
}

// -------------------------------------
// Parses the source pattern.

void PARSER::parse(UParseError &parseErrorResult,
                                     UErrorCode &errorCode) {
    // Return immediately in the case of a previous error
    CHECK_ERROR(errorCode);

    // parseOptionalWhitespace() succeeds on an empty string, so don't check bounds yet
    parseOptionalWhitespace(errorCode);
    // parseDeclarations() requires there to be input left, so check to see if
    // parseOptionalWhitespace() consumed it all

    // Skip the check if errorCode is already set, so as to avoid overwriting a
    // previous error offset
    if (U_SUCCESS(errorCode) && !inBounds(source, index)) {
        ERROR(parseError, errorCode, index);
    }

    parseDeclarations(errorCode);
    parseBody(errorCode);
    parseOptionalWhitespace(errorCode);

    // There are no errors; finally, check that the entire input was consumed
    // Skip the check if errorCode is already set, so as to avoid overwriting a
    // previous error offset
    if (U_SUCCESS(errorCode)) {
        if (((int32_t)index) != source.length()) {
            ERROR(parseError, errorCode, index);
        }
    }

    // Finally, copy the relevant fields of the internal `MessageParseError`
    // into the `UParseError` argument
    translateParseError(parseError, parseErrorResult);
}

PARSER::~Parser() {}

// ------------------------------------------------------
// Formatting

// TODO
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
        // TODO: name shadowing errors
        VariableName var = rand.asVariable();
        const Bindings& env = dataModel->getLocalVariables();
        // TODO: maybe env should actually bind names to some sort of resolved value
        // TODO: is evaluation of locals eager or lazy? Treating it as lazy for now
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
        // Unbound variable -- Should have been a data model error checked during that phase
        status = U_UNDEFINED_KEYWORD;
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

// Precondition: `env` defines `functionName` as a selector. (This is determined when checking the data model)
MessageFormatter::ResolvedExpression* MessageFormatter::formatSelector(const FunctionRegistry& env, const Hashtable& arguments, const FunctionName& functionName, const OptionMap& variableOptions, const Operand& rand, UErrorCode& status) const {
    NULL_ON_ERROR(status);

    // Look up the selector
    const SelectorFactory* selectorFactory = env.getSelector(functionName);
    // This should have been checked earlier
    U_ASSERT(selectorFactory != nullptr);
    // Create a specific instance of the selector
    LocalPointer<Selector> selector(selectorFactory->createSelector(getLocale(), arguments, status));
    // Resolve the operand
    LocalPointer<UnicodeString> resolvedOperand(new UnicodeString());
    if (!resolvedOperand.isValid()) {
        status = U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }
    formatOperand(arguments, rand, status, *resolvedOperand);
    LocalPointer<Hashtable> resolvedOptions(resolveOptions(arguments, variableOptions, status));
    NULL_ON_ERROR(status);
    LocalPointer<ResolvedExpression> result(new ResolvedExpression(selector.orphan(), resolvedOptions.orphan(), resolvedOperand.orphan()));
    if (!result.isValid()) {
        status = U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }
    return result.orphan();
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
void MessageFormatter::formatFunctionCall(const FunctionRegistry& env, const Hashtable& arguments, const FunctionName& functionName, const OptionMap& variableOptions, const Operand& rand, UnicodeString& result, UErrorCode& status) const {
    CHECK_ERROR(status);

    // Look up the formatter
    const FormatterFactory* formatterFactory = env.getFormatter(functionName);
    // This should have been checked earlier
    U_ASSERT(formatterFactory != nullptr);
    // Create a specific instance of the selector
    const LocalPointer<Formatter> formatter(formatterFactory->createFormatter(getLocale(), arguments, status));
    // Resolve the operand
    UnicodeString resolvedOperand;
    formatOperand(arguments, rand, status, resolvedOperand);
    LocalPointer<const Hashtable> resolvedOptions(resolveOptions(arguments, variableOptions, status));
    CHECK_ERROR(status);
    formatter->format(resolvedOperand, *resolvedOptions, result, status);
}

MessageFormatter::ResolvedExpression* MessageFormatter::formatSelectorExpression(const Hashtable& arguments, const Expression& expr, UErrorCode &status) const {
    NULL_ON_ERROR(status);

    // Formatting error
    if (expr.isReserved()) {
        status = U_UNSUPPORTED_PROPERTY;
        return nullptr;
    }
    if (expr.isFunctionCall()) {
        const Operator& rator = expr.getOperator();
        const FunctionName& functionName = rator.getFunctionName();
        // Look up the function name in either the standard function
        // registry (if it's a standard function) or the custom registry
        // This means standard functions take precedence over custom
        // definitions of the same names
        const FunctionRegistry& env = isBuiltInSelector(functionName) ?
            *standardFunctionRegistry :
            // It's safe to call this b/c an unbound function
            // would have been flagged as an error earlier
            getCustomFunctionRegistry();
        // Evaluate the function call with the given options, applied to the operand
        return formatSelector(env, arguments, functionName,
                              rator.getOptions(), expr.getOperand(), status);
    }
    // Plug in the "text" selector
    return formatSelector(*standardFunctionRegistry, arguments, TEXT_SELECTOR,
                          emptyOptions(), expr.getOperand(), status);
}

void MessageFormatter::formatPatternExpression(const Hashtable& arguments, const Expression& expr, UErrorCode &status, UnicodeString& result) const {
    CHECK_ERROR(status);

    // Formatting error
    if (expr.isReserved()) {
        status = U_UNSUPPORTED_PROPERTY;
        return;
    }
    // Function call - Not supported yet
    // TODO
    if (expr.isFunctionCall()) {
        const Operator& rator = expr.getOperator();
        const FunctionName& functionName = rator.getFunctionName();
        const FunctionRegistry& env = isBuiltInFormatter(functionName) ? 
                                      *standardFunctionRegistry : getCustomFunctionRegistry();
        formatFunctionCall(env, arguments, functionName,
                               rator.getOptions(), expr.getOperand(), result, status);
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
        if (selectorImpl.matches(*(rv.resolvedOperand), *k, *(rv.resolvedOptions), status)) {
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
    // TODO: check that each variant has a number of keys equal to the length of the selectors

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

void MessageFormatter::formatToString(const Hashtable& arguments, UErrorCode &status, UnicodeString& result) const {
    CHECK_ERROR(status);

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

