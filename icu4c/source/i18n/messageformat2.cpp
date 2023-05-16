// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/messageformat2.h"
#include "unicode/messageformat2_data_model.h"
#include "uvector.h" // U_ASSERT

// Syntactically significant characters
#define LEFT_CURLY_BRACE ((UChar32)0x007B)
#define RIGHT_CURLY_BRACE ((UChar32)0x007D)
#define SPACE ((UChar32)0x0020)
#define HTAB ((UChar32)0x0009)
#define CR ((UChar32)0x000D)
#define LF ((UChar32)0x000A)
#define BACKSLASH ((UChar32)0x005C)
#define PIPE ((UChar32)0x007C)
#define EQUALS ((UChar32)0x003D)
#define DOLLAR ((UChar32)0x0024)
#define COLON ((UChar32)0x003A)
#define PLUS ((UChar32)0x002B)
#define HYPHEN ((UChar32)0x002D)
#define PERIOD ((UChar32)0x002E)
#define UNDERSCORE ((UChar32)0x005F)

// Both used (in a `key` context) and reserved (in an annotation context)
#define ASTERISK ((UChar32)0x002A)

// Reserved sigils
#define BANG ((UChar32)0x0021)
#define AT ((UChar32)0x0040)
#define POUND ((UChar32)0x0023)
#define PERCENT ((UChar32)0x0025)
#define CARET ((UChar32)0x005E)
#define AMPERSAND ((UChar32)0x0026)
#define LESS_THAN ((UChar32)0x003C)
#define GREATER_THAN ((UChar32)0x003E)
#define QUESTION ((UChar32)0x003F)
#define TILDE ((UChar32)0x007E)

U_NAMESPACE_BEGIN namespace message2 {

// MessageFormat2 uses three keywords: `let`, `when`, and `match`.

static constexpr UChar32 ID_LET[] = {
    0x6C, 0x65, 0x74, 0 /* "let" */
};

static constexpr UChar32 ID_WHEN[] = {
    0x77, 0x68, 0x65, 0x6E, 0 /* "when" */
};

static constexpr UChar32 ID_MATCH[] = {
    0x6D, 0x61, 0x74, 0x63, 0x68, 0 /* "match" */
};

UOBJECT_DEFINE_RTTI_IMPLEMENTATION(MessageFormat2)

// Used so `parseEscapeSequence()` can handle all types of escape sequences
// (literal, text, and reserved)
typedef enum { LITERAL, TEXT, RESERVED } EscapeKind;

/*
  Use an internal "parse error" structure to make it easier to translate
  absolute offsets to line offsets.
  This is translated back to a `UParseError` at the end of parsing.
*/
typedef struct MessageParseError {
    // The line on which the error occurred
    uint32_t line;
    // The offset, relative to the erroneous line, on which the error occurred
    uint32_t offset;
    // The total number of characters seen before advancing to the current line. It has a value of 0 if line == 0.
    // It includes newline characters, because the index does too.
    uint32_t lengthBeforeCurrentLine;

    // This parser doesn't yet use the last two fields.
    UChar   preContext[U_PARSE_CONTEXT_LEN];
    UChar   postContext[U_PARSE_CONTEXT_LEN];
} MessageParseError;

static const MessageParseError INITIAL_MESSAGE_PARSE_ERROR = {
    0, 0, 0, {0}, {0}
};

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


// Returns immediately if `errorCode` indicates failure
#define CHECK_ERROR(errorCode)                                                                          \
    if (U_FAILURE(errorCode)) {                                                                         \
        return;                                                                                         \
    }

// Returns immediately if `errorCode` indicates failure
#define NULL_ON_ERROR(errorCode)                                                                          \
    if (U_FAILURE(errorCode)) {                                                                         \
        return nullptr;                                                                                         \
    }

// Returns true iff `index` is a valid index for the string `source`
static bool inBounds(const UnicodeString &source, uint32_t index) {
    return (((int32_t)index) < source.length());
}

// Increments the line number and updates the "characters seen before
// current line" count in `parseError`, iff `source[index]` is a newline
static void maybeAdvanceLine(const UnicodeString& source,
                             uint32_t index,
                             MessageParseError &parseError) {
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

// -------------------------------------
// Creates a MessageFormat instance based on the pattern.

MessageFormat2::MessageFormat2(const UnicodeString &pattern, UParseError &parseError,
                               UErrorCode &success) : dataModel(success) {
    CHECK_ERROR(success);

    // Validate pattern and build data model
    parse(pattern, parseError, success);
}

MessageFormat2::~MessageFormat2() {}

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

static void translateParseError(const MessageParseError &messageParseError, UParseError &parseError) {
    parseError.line = messageParseError.line;
    parseError.offset = messageParseError.offset;
    copyContext(messageParseError.preContext, parseError.preContext);
    copyContext(messageParseError.postContext, parseError.postContext);
}

static void setParseError(MessageParseError &parseError, uint32_t index) {
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

// See `s` in the MessageFormat2 grammar
static bool isWhitespace(UChar32 c) {
    switch (c) {
    case SPACE:
    case HTAB:
    case CR:
    case LF:
        return true;
    default:
        return false;
    }
}

/*
  The following helper predicates should exactly match nonterminals in the MessageFormat2 grammar:

  `isTextChar()`      : `text-char`
  `isReservedStart()` : `reserved-start`
  `isReservedChar()`  : `reserved-char`
  `isAlpha()`         : `ALPHA`
  `isDigit()`         : `DIGIT`
  `isNameStart()`     : `name-start`
  `isNameChar()`      : `name-char`
  `isLiteralChar()`   : `literal-char`
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

static bool isLiteralChar(UChar32 c) {
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
static void parseWhitespaceMaybeRequired(bool required,
                                         const UnicodeString &source,
                                         uint32_t &index,
                                         MessageParseError &parseError,
                                         UErrorCode &errorCode) {
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
            maybeAdvanceLine(source, index, parseError);
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
static void parseRequiredWhitespace(const UnicodeString &source,
                                    uint32_t &index,
                                    MessageParseError &parseError,
                                    UErrorCode &errorCode) {
    parseWhitespaceMaybeRequired(true, source, index, parseError, errorCode);
}

/*
  No pre, no post, for the same reason as `parseWhitespaceMaybeRequired()`.
*/
static void parseOptionalWhitespace(const UnicodeString &source,
                                    uint32_t &index,
                                    MessageParseError &parseError,
                                    UErrorCode &errorCode) {
    parseWhitespaceMaybeRequired(false, source, index, parseError, errorCode);
}

// Consumes a single character, signaling an error if `source[index]` != `c`
static void parseToken(UChar32 c,
                       const UnicodeString &source,
                       uint32_t &index,
                       MessageParseError &parseError,
                       UErrorCode &errorCode) {
    CHECK_ERROR(errorCode);

    U_ASSERT(inBounds(source, index));
    if (source[index] == c) {
        index++;
        // Guarantee postcondition
        CHECK_BOUNDS(source, index, parseError, errorCode);
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
static void parseToken(const UChar32 (&token)[N],
                       const UnicodeString &source,
                       uint32_t &index,
                       MessageParseError &parseError,
                       UErrorCode &errorCode) {
    CHECK_ERROR(errorCode);

    U_ASSERT(inBounds(source, index));

    size_t tokenPos = 0;
    while (tokenPos < N - 1) {
        if (source[index] != token[tokenPos]) {
            ERROR(parseError, errorCode, index);
            return;
        }
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
static void parseTokenWithWhitespace(const UChar32 (&token)[N],
                                     const UnicodeString &source,
                                     uint32_t &index,
                                     MessageParseError &parseError,
                                     UErrorCode &errorCode) {
    // No need for error check or bounds check before parseOptionalWhitespace
    parseOptionalWhitespace(source, index, parseError, errorCode);
    // Establish precondition
    CHECK_BOUNDS(source, index, parseError, errorCode);
    parseToken(token, source, index, parseError, errorCode);
    parseOptionalWhitespace(source, index, parseError, errorCode);
    // Guarantee postcondition
    CHECK_BOUNDS(source, index, parseError, errorCode);
}

/*
   Consumes optional whitespace, possibly advancing `index` to `index'`,
   then consumes a single character (signaling an error if it doesn't match
   `source[index']`),
   then consumes optional whitespace again
*/
static void parseTokenWithWhitespace(UChar32 c,
                                     const UnicodeString &source,
                                     uint32_t &index,
                                     MessageParseError &parseError,
                                     UErrorCode &errorCode) {
    // No need for error check or bounds check before parseOptionalWhitespace
    parseOptionalWhitespace(source, index, parseError, errorCode);
    // Establish precondition
    CHECK_BOUNDS(source, index, parseError, errorCode);
    parseToken(c, source, index, parseError, errorCode);
    parseOptionalWhitespace(source, index, parseError, errorCode);
    // Guarantee postcondition
    CHECK_BOUNDS(source, index, parseError, errorCode);
}

/*
  Consumes a non-empty sequence of `name-char`s.

  (Matches the `nmtoken` nonterminal in the grammar.)
*/
static void parseNmtoken(const UnicodeString &source,
                         uint32_t &index,
                         MessageParseError &parseError,
                         UErrorCode &errorCode,
                         VariableName &name) {
    CHECK_ERROR(errorCode);

    U_ASSERT(inBounds(source, index));
    if (!isNameChar(source[index])) {
        ERROR(parseError, errorCode, index);
        return;
    }

    while (isNameChar(source[index])) {
        name += source[index];
        index++;
        CHECK_BOUNDS(source, index, parseError, errorCode);
    }
}

/*
  Consumes a non-empty sequence of `name-char`s, the first of which is
  also a `name-start`.
  that begins with a character `start` such that `isNameStart(start)`.

  Initializes `name` to this sequence.

  (Matches the `name` nonterminal in the grammar.)
*/
static void parseName(const UnicodeString &source,
                      uint32_t &index,
                      MessageParseError &parseError,
                      UErrorCode &errorCode,
                      VariableName &name) {
    CHECK_ERROR(errorCode);

    U_ASSERT(inBounds(source, index));

    if (!isNameStart(source[index])) {
        ERROR(parseError, errorCode, index);
        return;
    }

    parseNmtoken(source, index, parseError, errorCode, name);
}

/*
  Consumes a '$' followed by a `name`, initializing `var` to `name`.

  (Matches the `variable` nonterminal in the grammar.)
*/
static void parseVariableName(const UnicodeString &source,
                              uint32_t &index,
                              MessageParseError &parseError,
                              UErrorCode &errorCode,
                              VariableName &var) {
    CHECK_ERROR(errorCode);

    U_ASSERT(inBounds(source, index));
    if (source[index] != DOLLAR) {
        ERROR(parseError, errorCode, index);
        return;
    }

    index++; // Consume the '$'
    CHECK_BOUNDS(source, index, parseError, errorCode);
    parseName(source, index, parseError, errorCode, var);
}


/*
  Consumes a reference to a function, matching the `function` nonterminal in
  the grammar.

  Initializes `func` to this name.
*/
static void parseFunction(const UnicodeString &source,
                          uint32_t &index,
                          MessageParseError &parseError,
                          UErrorCode &errorCode,
                          FunctionName &func) {
    CHECK_ERROR(errorCode);

    U_ASSERT(inBounds(source, index));
    if (!isFunctionStart(source[index])) {
        ERROR(parseError, errorCode, index);
        return;
    }

    index++; // Consume the function start character
    CHECK_BOUNDS(source, index, parseError, errorCode);
    parseName(source, index, parseError, errorCode, func);
}


/*
  Precondition: source[index] == BACKSLASH

  Consume an escaped character.

  Generalized to handle `reserved-escape`, `text-escape`,
  or `literal-escape`, depending on the `kind` argument.

  Appends result to `str`
*/
static void parseEscapeSequence(const UnicodeString &source,
                                uint32_t &index,
                                EscapeKind kind,
                                MessageParseError &parseError,
                                UErrorCode &errorCode,
                                String &str) {
    CHECK_ERROR(errorCode);

    U_ASSERT(inBounds(source, index));
    U_ASSERT(source[index] == BACKSLASH);
    index++; // Skip the initial backslash
    CHECK_BOUNDS(source, index, parseError, errorCode);

    #define SUCCEED \
       /* Append to the output string */                    \
       str += source[index];                                \
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
static void parseLiteralEscape(const UnicodeString &source,
                               uint32_t &index,
                               MessageParseError &parseError,
                               UErrorCode &errorCode,
                               String &str) {
    parseEscapeSequence(source, index, LITERAL, parseError, errorCode, str);
}

/*
  Consume a literal, matching the `literal` nonterminal in the grammar.
*/
// TODO: currently initializes `str` with the contents of the literal.
// Not sure if literals should be represented differently from strings.
static void parseLiteral(const UnicodeString &source,
                         uint32_t &index,
                         MessageParseError &parseError,
                         UErrorCode &errorCode,
                         String &str) {
    CHECK_ERROR(errorCode);
    U_ASSERT(inBounds(source, index));

    // Parse the opening '|'
    parseToken(PIPE, source, index, parseError, errorCode);
    CHECK_BOUNDS(source, index, parseError, errorCode);

    // Parse the contents
    bool done = false;
    while (!done) {
        if (source[index] == BACKSLASH) {
            parseLiteralEscape(source, index, parseError, errorCode, str);
        } else if (isLiteralChar(source[index])) {
            str += source[index];
            index++; // Consume this character
            maybeAdvanceLine(source, index, parseError);
        } else {
          // Assume the sequence of literal characters ends here
          done = true;
        }
        CHECK_BOUNDS(source, index, parseError, errorCode);
    }

    // Parse the closing '|'
    parseToken(PIPE, source, index, parseError, errorCode);

    // Guarantee postcondition
    CHECK_BOUNDS(source, index, parseError, errorCode);
}

/*
  Consume a name-value pair, matching the `option` nonterminal in the grammar.

  Adds the option to `optionList`
*/
static void parseOption(const UnicodeString &source,
                        uint32_t &index,
                        MessageParseError &parseError,
                        UErrorCode &errorCode,
                        OptionList &opts) {
    CHECK_ERROR(errorCode);

    U_ASSERT(inBounds(source, index));

    // Parse LHS
    String lhs;
    parseName(source, index, parseError, errorCode, lhs);

    // Parse '='
    parseTokenWithWhitespace(EQUALS, source, index, parseError, errorCode);

    String rhs;
    bool isVariable = false;

    // Parse RHS, which is either a literal, nmtoken, or variable
    switch (source[index]) {
    case PIPE: {
        parseLiteral(source, index, parseError, errorCode, rhs);
        break;
    }
    case DOLLAR: {
        parseVariableName(source, index, parseError, errorCode, rhs);
        isVariable = true;
        break;
    }
    default: {
        // Not a literal or variable, so it must be an nmtoken
        parseNmtoken(source, index, parseError, errorCode, rhs);
        break;
    }
    }

    // Finally, add the lhs=rhs mapping to the list
    Option opt(lhs, Operand(isVariable, rhs));
    opts.add(opt, errorCode);
}

/*
  Consume optional whitespace followed by a sequence of options
  (possibly empty), separated by whitespace
*/
static void parseOptions(const UnicodeString &source,
                         uint32_t &index,
                         MessageParseError &parseError,
                         UErrorCode &errorCode,
                         OptionList &options) {
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
        parseRequiredWhitespace(source, index, parseError, errorCode);
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
            break;
        }
        parseOption(source, index, parseError, errorCode, options);
    }
}

static void parseReservedEscape(const UnicodeString &source,
                                uint32_t &index,
                                MessageParseError &parseError,
                                UErrorCode &errorCode,
                                String &str) {
    parseEscapeSequence(source, index, RESERVED, parseError, errorCode, str);
}

/*
  Consumes a non-empty sequence of reserved-chars, reserved-escapes, and
  literals (as in 1*(reserved-char / reserved-escape / literal) in the `reserved-body` rule)
*/
static void parseReservedChunk(const UnicodeString &source,
                               uint32_t &index,
                               MessageParseError &parseError,
                               UErrorCode &errorCode) {
    CHECK_ERROR(errorCode);

    bool empty = true;
    while(reservedChunkFollows(source[index])) {
        String str;

        empty = false;
        // reserved-char
        if (isReservedChar(source[index])) {
            // consume the char
            index++;
            // Restore precondition
            CHECK_BOUNDS(source, index, parseError, errorCode);
        } else if (source[index] == BACKSLASH) {
            // reserved-escape
            // Result `str` is ignored since reserved strings get dropped
            parseReservedEscape(source, index, parseError, errorCode, str);
        } else if (source[index] == PIPE) {
            // Result `str` is ignored since reserved strings get dropped
            parseLiteral(source, index, parseError, errorCode, str);
        } else {
            // The reserved chunk ends here
            break;
        }
    }

    if (empty) {
        ERROR(parseError, errorCode, index);
    }
}

/*
  Consume a `reserved-start` character followed by a possibly-empty sequence
  of non-empty sequences of reserved characters, separated by whitespace.
  Matches the `reserved` nonterminal in the grammar

  Ignores the result, since a reserved string results in a parse error.
  TODO: test for that error
*/
static void parseReserved(const UnicodeString &source,
                          uint32_t &index,
                          MessageParseError &parseError,
                          UErrorCode &errorCode) {
    CHECK_ERROR(errorCode);

    U_ASSERT(inBounds(source, index));

    // Require a `reservedStart` character
    if (!isReservedStart(source[index])) {
        ERROR(parseError, errorCode, index);
        return;
    }
    // Consume reservedStart
    index++;
    // Restore precondition
    CHECK_BOUNDS(source, index, parseError, errorCode);

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
            parseOptionalWhitespace(source, index, parseError, errorCode);
            // Restore precondition
            CHECK_BOUNDS(source, index, parseError, errorCode);
        }

        if (reservedChunkFollows(source[index])) {
            parseReservedChunk(source, index, parseError, errorCode);

            // Avoid looping infinitely
            CHECK_BOUNDS(source, index, parseError, errorCode);
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
                return;
            }
            // If there was no whitespace, it's not an error,
            // just the end of the reserved string
            break;
        }
    }
}


/*
  Consume a function call or reserved string, matching the `annotation`
  nonterminal in the grammar

  Initializes `operator` to represent this (a reserved is a parse error)
*/
static void parseAnnotation(const UnicodeString &source,
                            uint32_t &index,
                            MessageParseError &parseError,
                            UErrorCode &errorCode,
                            Operator &rator) {
    CHECK_ERROR(errorCode);

    U_ASSERT(inBounds(source, index));
    if (isFunctionStart(source[index])) {
        // Consume the function name
        FunctionName func;
        parseFunction(source, index, parseError, errorCode, func);

        // Consume the options (which may be empty)
        OptionList options;
        parseOptions(source, index, parseError, errorCode, options);
        rator = Operator(func, options);
    }
    // Must be reserved
    parseReserved(source, index, parseError, errorCode);
    U_ASSERT(U_FAILURE(errorCode)); // reserved is an error
}

/*
  Consume a literal or variable (depending on `isVariable`),
  followed by either required whitespace followed by an annotation,
  or optional whitespace.
*/
static void parseLiteralOrVariableWithAnnotation(const UnicodeString &source,
                                                 uint32_t &index,
                                                 bool isVariable,
                                                 MessageParseError &parseError,
                                                 UErrorCode &errorCode,
                                                 Expression &expression) {
    CHECK_ERROR(errorCode);

    U_ASSERT(inBounds(source, index));

    Operand rand;
    if (isVariable) {
        VariableName var;
        parseVariableName(source, index, parseError, errorCode, var);
        rand = Operand(true, var);
    } else {
        String str;
        parseLiteral(source, index, parseError, errorCode, str);
        rand = Operand(false, str);
    }

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

    // If the next character is not whitespace, return.
    if (!isWhitespace(source[index])) {
        // This means there's no annotation, since an annotation is preceded by
        // required whitespace. We're done.
        return;
    }

    // If the next character is whitespace, either [s annotation] or [s] applies
    // (the character is either the required space before an annotation, or optional
    // trailing space after the literal or variable). It's still ambiguous which
    // one does apply.
    parseRequiredWhitespace(source, index, parseError, errorCode);
    // Restore precondition
    CHECK_BOUNDS(source, index, parseError, errorCode);

    // This next check resolves the ambiguity between [s annotation] and [s]
    if (isAnnotationStart(source[index])) {
        // The previously consumed whitespace precedes an annotation
        Operator rator;
        parseAnnotation(source, index, parseError, errorCode, rator);
        expression = Expression(rator, rand);
        return;
    }
    // The previously consumed whitespace is the optional trailing whitespace;
    // either the next character is '}' or the error will be handled by parseExpression.

    expression = Expression(rand);
}

/*
  Consume an expression, matching the `expression` nonterminal in the grammar
*/
static void parseExpression(const UnicodeString &source,
                            uint32_t &index,
                            MessageParseError &parseError,
                            UErrorCode &errorCode,
                            Expression& expression) {
    CHECK_ERROR(errorCode);

    U_ASSERT(inBounds(source, index));
    // Parse opening brace
    parseToken(LEFT_CURLY_BRACE, source, index, parseError, errorCode);
    // Optional whitespace after opening brace
    parseOptionalWhitespace(source, index, parseError, errorCode);
    // Restore precondition
    CHECK_BOUNDS(source, index, parseError, errorCode);
    // literal '|', variable '$' or annotation
    switch (source[index]) {
    case PIPE: {
        // Literal
        parseLiteralOrVariableWithAnnotation(source, index, false, parseError, errorCode, expression);
        break;
    }
    case DOLLAR: {
        // Variable
        parseLiteralOrVariableWithAnnotation(source, index, true, parseError, errorCode, expression);
        break;
    }
    default: {
        if (isAnnotationStart(source[index])) {
            Operator rator;
            parseAnnotation(source, index, parseError, errorCode, rator);
            expression = Expression(rator);
            break;
        }
        // Not a literal, variable or annotation -- error out
        ERROR(parseError, errorCode, index);
        return;
    }
    }
    // For why we don't parse optional whitespace here, even though the grammar
    // allows it, see comments in parseLiteralWithAnnotation() and parseOptions()

    // Parse closing brace
    parseToken(RIGHT_CURLY_BRACE, source, index, parseError, errorCode);
}

/*
  Consume a possibly-empty sequence of declarations separated by whitespace;
  each declaration matches the `declaration` nonterminal in the grammar

  Builds up an environment representing those declarations
*/
static void parseDeclarations(const UnicodeString &source,
                              uint32_t &index,
                              MessageParseError &parseError,
                              UErrorCode &errorCode,
                              Environment &env) {
    CHECK_ERROR(errorCode);

    // End-of-input here would be an error; even empty
    // declarations must be followed by a body
    CHECK_BOUNDS(source, index, parseError, errorCode);

    while (source[index] == ID_LET[0]) {
        parseToken(ID_LET, source, index, parseError, errorCode);
        parseRequiredWhitespace(source, index, parseError, errorCode);
        // Restore precondition
        CHECK_BOUNDS(source, index, parseError, errorCode);
        VariableName lhs;
        parseVariableName(source, index, parseError, errorCode, lhs);
        parseTokenWithWhitespace(EQUALS, source, index, parseError, errorCode);
        Expression rhs;
        parseExpression(source, index, parseError, errorCode, rhs);
        parseOptionalWhitespace(source, index, parseError, errorCode);
        // Restore precondition
        CHECK_BOUNDS(source, index, parseError, errorCode);

        // Add binding from lhs to rhs
        env.define(lhs, rhs, errorCode);
    }
}

/*
  Consume an escaped curly brace, or backslash, matching the `text-escape`
  nonterminal in the grammar
*/
static void parseTextEscape(const UnicodeString &source,
                            uint32_t &index,
                            MessageParseError &parseError,
                            UErrorCode &errorCode,
                            String &str) {
    parseEscapeSequence(source, index, TEXT, parseError, errorCode, str);
}

/*
  Consume a non-empty sequence of text characters and escaped text characters,
  matching the `text` nonterminal in the grammar
*/
static void parseText(const UnicodeString &source,
                      uint32_t &index,
                      MessageParseError &parseError,
                      UErrorCode &errorCode,
                      String str) {
    CHECK_ERROR(errorCode)
    U_ASSERT(inBounds(source, index));
    bool empty = true;

    while (true) {
        if (source[index] == BACKSLASH) {
            parseTextEscape(source, index, parseError, errorCode, str);
        } else if (isTextChar(source[index])) {
            index++;
            maybeAdvanceLine(source, index, parseError);
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
static void parseKey(const UnicodeString &source,
                     uint32_t &index,
                     MessageParseError &parseError,
                     UErrorCode &errorCode,
                     Key &key) {
    CHECK_ERROR(errorCode);
    U_ASSERT(inBounds(source, index));

    // Literal | nmtoken | '*'
    switch (source[index]) {
    case PIPE: {
        String s;
        parseLiteral(source, index, parseError, errorCode, s);
        key = Key(s);
        break;
    }
    case ASTERISK: {
        index++;
        // Guarantee postcondition
        CHECK_BOUNDS(source, index, parseError, errorCode);
        key = Key();
        break;
    }
    default: {
        // nmtoken
        String s;
        parseNmtoken(source, index, parseError, errorCode, s);
        key = Key(s);
        break;
    }
    }
}

/*
  Consume a non-empty sequence of `key`s separated by whitespace
*/
static void parseNonEmptyKeys(const UnicodeString &source,
                              uint32_t &index,
                              MessageParseError &parseError,
                              UErrorCode &errorCode,
                              KeyList &keys) {
    CHECK_ERROR(errorCode);
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

    // Since the first key is required, it's simplest to parse the required
    // whitespace and then the first key separately.
    parseRequiredWhitespace(source, index, parseError, errorCode);
    // Restore precondition
    CHECK_BOUNDS(source, index, parseError, errorCode);
    Key k;
    parseKey(source, index, parseError, errorCode, k);
    keys.add(k, errorCode);

    // We've seen at least one whitespace-key pair, so now we can parse
    // *(s key) [s]
    while (isWhitespace(source[index])) {
        parseRequiredWhitespace(source, index, parseError, errorCode);
        // Restore precondition
        CHECK_BOUNDS(source, index, parseError, errorCode);

        // At this point, it's ambiguous whether we are inside (s key) or [s].
        // This check resolves that ambiguity.
        if (source[index] == LEFT_CURLY_BRACE) {
            // A pattern follows, so what we just parsed was the optional
            // trailing whitespace. All the keys have been parsed.
            break;
        }
        parseKey(source, index, parseError, errorCode, k);
        keys.add(k, errorCode);
    }
}

/*
  Consume a `pattern`, matching the nonterminal in the grammar
  No postcondition (on return, `index` might equal `source.length()` with U_SUCCESS(errorCode)),
  because a message can end with a pattern
*/
static void parsePattern(const UnicodeString &source,
                         uint32_t &index,
                         MessageParseError &parseError,
                         UErrorCode &errorCode,
                         Expression &expression) {
    CHECK_ERROR(errorCode);
    U_ASSERT(inBounds(source, index));

    parseToken(LEFT_CURLY_BRACE, source, index, parseError, errorCode);
    while (source[index] != RIGHT_CURLY_BRACE) {
        switch (source[index]) {
        case LEFT_CURLY_BRACE: {
            // Must be expression
            parseExpression(source, index, parseError, errorCode, expression);
            break;
        }
        default: {
            // Must be text
            String s;
            parseText(source, index, parseError, errorCode, s);
            // Text => uninterpreted-string operand
            Operand rand(false, s);
            // Texts are represented as expression
            expression = Expression(rand);
            break;
        }
        }
        // Need an explicit error check here so we don't loop infinitely
        CHECK_ERROR(errorCode);
    }
    // Consume the closing brace
    index++;
}

/*
  Consume a `selectors` (matching the nonterminal in the grammar),
  followed by a non-empty sequence of `variant`s (matching the nonterminal
  in the grammar) preceded by whitespace
  No postcondition (on return, `index` might equal `source.length()` with U_SUCCESS(errorCode)),
  because a message can end with a variant
*/
static void parseSelectors(const UnicodeString &source,
                           uint32_t &index,
                           MessageParseError &parseError,
                           UErrorCode &errorCode,
                           MessageBody &body) {
    CHECK_ERROR(errorCode);
    U_ASSERT(inBounds(source, index));

    parseToken(ID_MATCH, source, index, parseError, errorCode);

    bool selectorsEmpty = true;
    ExpressionList scrutinees;
    Expression expression;

    // Parse selectors
    while (isWhitespace(source[index]) || source[index] == LEFT_CURLY_BRACE) {
        parseOptionalWhitespace(source, index, parseError, errorCode);
        // Restore precondition
        CHECK_BOUNDS(source, index, parseError, errorCode);

        if (source[index] != LEFT_CURLY_BRACE) {
            // This is not necessarily an error, but rather,
            // means the whitespace we parsed was the optional
            // whitespace preceding the first variant, not the
            // optional whitespace preceding a subsequent expression.
            break;
        }

        parseExpression(source, index, parseError, errorCode, expression);
        scrutinees.add(expression, errorCode);
        selectorsEmpty = false;
    }

    // At least one selector is required
    if (selectorsEmpty) {
        ERROR(parseError, errorCode, index);
        return;
    }

    #define CHECK_END_OF_INPUT                     \
        if (((int32_t)index) >= source.length()) { \
            break;                                 \
        }                                          \

    // Parse variants
    bool empty = true;
    VariantList variants;
    while (isWhitespace(source[index]) || source[index] == ID_WHEN[0]) {
        parseOptionalWhitespace(source, index, parseError, errorCode);
        // Restore the precondition, *without* erroring out if we've
        // reached the end of input. That's because it's valid for the
        // message to end with trailing whitespace that follows a variant.
        CHECK_END_OF_INPUT

        // Consume the "when"
        parseToken(ID_WHEN, source, index, parseError, errorCode);

        // At least one key is required
        KeyList keys;
        parseNonEmptyKeys(source, index, parseError, errorCode, keys);

        Expression rhs;
        // parseNonEmptyKeys() consumes any trailing whitespace,
        // so the pattern can be consumed next.
        parsePattern(source, index, parseError, errorCode, rhs);

        variants.add(Variant(keys, rhs), errorCode);
        empty = false;

        // Restore the precondition, *without* erroring out if we've
        // reached the end of input. That's because it's valid for the
        // message to end with a variant that has no trailing whitespace.
        // Why do we need to check this condition twice inside the loop?
        // Because if we don't check it here, the `isWhitespace()` call in
        // the loop head will read off the end of the input string.
        CHECK_END_OF_INPUT
    }

    // At least one variant is required
    if (empty) {
        ERROR(parseError, errorCode, index);
    }

    body = MessageBody(scrutinees, variants);
}

/*
  Consume a `body` (matching the nonterminal in the grammar),
  No postcondition (on return, `index` might equal `source.length()` with U_SUCCESS(errorCode)),
  because a message can end with a body (trailing whitespace is optional)
*/
static void parseBody(const UnicodeString &source, uint32_t &index, MessageParseError &parseError,
                      UErrorCode &errorCode, MessageBody &body) {
    CHECK_ERROR(errorCode);
    U_ASSERT(inBounds(source, index));

    // Body must be either a pattern or selectors
    switch (source[index]) {
    case LEFT_CURLY_BRACE: {
        // Pattern
        Expression expression;
        parsePattern(source, index, parseError, errorCode, expression);
        body = MessageBody(expression, errorCode);
        break;
    }
    case ID_MATCH[0]: {
        parseSelectors(source, index, parseError, errorCode, body);
        // Selectors
        break;
    }
    default: {
        ERROR(parseError, errorCode, index);
        break;
    }
    }
}

// -------------------------------------
// The copy constructor copies the data model.

/*
MessageFormat2::MessageFormat2(const MessageFormat2 & that) {
    dataModel = that.dataModel;
}
*/

// -------------------------------------
// Creates a copy of this MessageFormat2; the caller owns the copy.

MessageFormat2 *MessageFormat2::clone() const { return new MessageFormat2(*this); }

// Not yet implemented
bool MessageFormat2::operator==(const Format &other) const { return (this == &other); }
// Not yet implemented
bool MessageFormat2::operator!=(const Format &other) const { return (this != &other); }

// Not yet implemented
UnicodeString &MessageFormat2::format(const Formattable &, UnicodeString &appendTo, FieldPosition &,
                                      UErrorCode &status) const {
    status = U_UNSUPPORTED_ERROR;
    return appendTo;
}

// Not yet implemented
void MessageFormat2::parseObject(const UnicodeString &, Formattable &, ParsePosition &status) const {
    status = U_UNSUPPORTED_ERROR;
}

// -------------------------------------
// Parses (currently: validates) the source pattern.
// Building a data model is not yet implemented.
void MessageFormat2::parse(const UnicodeString &source,
                           UParseError &parseError,
                           UErrorCode &errorCode) const {
    // Return immediately in the case of a previous error
    CHECK_ERROR(errorCode);

    uint32_t index = 0;

    // Create a `MessageParseError` whose relevant fields will be copied
    // into the `UParseError` on exit
    MessageParseError messageParseError = INITIAL_MESSAGE_PARSE_ERROR;

    // parseOptionalWhitespace() succeeds on an empty string, so don't check bounds yet
    parseOptionalWhitespace(source, index, messageParseError, errorCode);
    // parseDeclarations() requires there to be input left, so check to see if
    // parseOptionalWhitespace() consumed it all

    // Skip the check if errorCode is already set, so as to avoid overwriting a
    // previous error offset
    if (U_SUCCESS(errorCode) && !inBounds(source, index)) {
        ERROR(messageParseError, errorCode, index);
    }

    // TODO: why does it think this is const?
xxx

    parseDeclarations(source, index, messageParseError, errorCode, dataModel.env);
    MessageBody body;
    parseBody(source, index, messageParseError, errorCode, dataModel.body);
    parseOptionalWhitespace(source, index, messageParseError, errorCode);

    // There are no errors; finally, check that the entire input was consumed
    // Skip the check if errorCode is already set, so as to avoid overwriting a
    // previous error offset
    if (U_SUCCESS(errorCode) && ((int32_t)index) != source.length()) {
        ERROR(messageParseError, errorCode, index);
    }

    // Finally, copy the relevant fields of the `MessageParseError` into the `UParseError`
    translateParseError(messageParseError, parseError);
}
} // namespace message2
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */
