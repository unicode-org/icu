/*
**********************************************************************
*   Copyright (C) 1999-2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   11/17/99    aliu        Creation.
**********************************************************************
*/
#include "cstring.h"
#include "hash.h"
#include "quant.h"
#include "rbt_data.h"
#include "rbt_pars.h"
#include "rbt_rule.h"
#include "strmatch.h"
#include "symtable.h"
#include "unirange.h"
#include "unicode/parseerr.h"
#include "unicode/parsepos.h"
#include "unicode/putil.h"
#include "unicode/rbt.h"
#include "unicode/unicode.h"
#include "unicode/uniset.h"

// Operators
#define VARIABLE_DEF_OP ((UChar)0x003D) /*=*/
#define FORWARD_RULE_OP ((UChar)0x003E) /*>*/
#define REVERSE_RULE_OP ((UChar)0x003C) /*<*/
#define FWDREV_RULE_OP  ((UChar)0x007E) /*~*/ // internal rep of <> op
#define OPERATORS       UNICODE_STRING("=><", 3)

// Other special characters
#define QUOTE             ((UChar)0x0027) /*'*/
#define ESCAPE            ((UChar)0x005C) /*\*/
#define END_OF_RULE       ((UChar)0x003B) /*;*/
#define RULE_COMMENT_CHAR ((UChar)0x0023) /*#*/

#define SEGMENT_OPEN       ((UChar)0x0028) /*(*/
#define SEGMENT_CLOSE      ((UChar)0x0029) /*)*/
#define CONTEXT_ANTE       ((UChar)0x007B) /*{*/
#define CONTEXT_POST       ((UChar)0x007D) /*}*/
#define SET_OPEN           ((UChar)0x005B) /*[*/
#define SET_CLOSE          ((UChar)0x005D) /*]*/
#define CURSOR_POS         ((UChar)0x007C) /*|*/
#define CURSOR_OFFSET      ((UChar)0x0040) /*@*/
#define ANCHOR_START       ((UChar)0x005E) /*^*/
#define KLEENE_STAR        ((UChar)0x002A) /***/
#define ONE_OR_MORE        ((UChar)0x002B) /*+*/

// By definition, the ANCHOR_END special character is a
// trailing SymbolTable.SYMBOL_REF character.
// private static final char ANCHOR_END       = '$';

const UnicodeString TransliteratorParser::gOPERATORS = OPERATORS;

// These are also used in Transliterator::toRules()
static const int32_t ID_TOKEN_LEN = 2;
static const UChar   ID_TOKEN[]   = { 0x3A, 0x3A }; // ':', ':'

//----------------------------------------------------------------------
// BEGIN ParseData
//----------------------------------------------------------------------

/**
 * This class implements the SymbolTable interface.  It is used
 * during parsing to give UnicodeSet access to variables that
 * have been defined so far.  Note that it uses variablesVector,
 * _not_ data.setVariables.
 */
class ParseData : public SymbolTable {
public:
    const TransliterationRuleData* data; // alias

    const UVector* variablesVector; // alias

    ParseData(const TransliterationRuleData* data = 0,
              const UVector* variablesVector = 0);

    virtual const UnicodeString* lookup(const UnicodeString& s) const;

    virtual const UnicodeSet* lookupSet(UChar32 ch) const;

    virtual UnicodeString parseReference(const UnicodeString& text,
                                         ParsePosition& pos, int32_t limit) const;
};

ParseData::ParseData(const TransliterationRuleData* d,
                     const UVector* sets) :
    data(d), variablesVector(sets) {}

/**
 * Implement SymbolTable API.
 */
const UnicodeString* ParseData::lookup(const UnicodeString& name) const {
    return (const UnicodeString*) data->variableNames->get(name);
}

/**
 * Implement SymbolTable API.
 */
const UnicodeSet* ParseData::lookupSet(UChar32 ch) const {
    // Note that we cannot use data.lookupSet() because the
    // set array has not been constructed yet.
    const UnicodeSet* set = NULL;
    int32_t i = ch - data->variablesBase;
    if (i >= 0 && i < variablesVector->size()) {
        int32_t i = ch - data->variablesBase;
        set = (i < variablesVector->size()) ?
            (UnicodeSet*) variablesVector->elementAt(i) : 0;
    }
    return set;
}

/**
 * Implement SymbolTable API.  Parse out a symbol reference
 * name.
 */
UnicodeString ParseData::parseReference(const UnicodeString& text,
                                        ParsePosition& pos, int32_t limit) const {
    int32_t start = pos.getIndex();
    int32_t i = start;
    UnicodeString result;
    while (i < limit) {
        UChar c = text.charAt(i);
        if ((i==start && !Unicode::isUnicodeIdentifierStart(c)) ||
            !Unicode::isUnicodeIdentifierPart(c)) {
            break;
        }
        ++i;
    }
    if (i == start) { // No valid name chars
        return result; // Indicate failure with empty string
    }
    pos.setIndex(i);
    text.extractBetween(start, i, result);
    return result;
}

//----------------------------------------------------------------------
// BEGIN RuleHalf
//----------------------------------------------------------------------

/**
 * A class representing one side of a rule.  This class knows how to
 * parse half of a rule.  It is tightly coupled to the method
 * RuleBasedTransliterator.Parser.parseRule().
 */
class RuleHalf {

public:

    UnicodeString text;

    int32_t cursor; // position of cursor in text
    int32_t ante;   // position of ante context marker '{' in text
    int32_t post;   // position of post context marker '}' in text

    // Record the position of the segment substrings and references.  A
    // given side should have segments or segment references, but not
    // both.
    UVector* segments; // ref substring start,limits
    int32_t maxRef;       // index of largest ref (1..9)

    // Record the offset to the cursor either to the left or to the
    // right of the key.  This is indicated by characters on the output
    // side that allow the cursor to be positioned arbitrarily within
    // the matching text.  For example, abc{def} > | @@@ xyz; changes
    // def to xyz and moves the cursor to before abc.  Offset characters
    // must be at the start or end, and they cannot move the cursor past
    // the ante- or postcontext text.  Placeholders are only valid in
    // output text.
    int32_t cursorOffset; // only nonzero on output side

    UBool anchorStart;
    UBool anchorEnd;

    TransliteratorParser& parser;

    static const UnicodeString gOperators;

    //--------------------------------------------------
    // Methods

    RuleHalf(TransliteratorParser& parser);
    ~RuleHalf();

    /**
     * Parse one side of a rule, stopping at either the limit,
     * the END_OF_RULE character, or an operator.  Return
     * the pos of the terminating character (or limit).
     */
    int32_t parse(const UnicodeString& rule, int32_t pos, int32_t limit);

    /**
     * Remove context.
     */
    void removeContext();

    /**
     * Create and return an int[] array of segments.
     */
    int32_t* createSegments() const;

    int syntaxError(int32_t code,
                    const UnicodeString& rule,
                    int32_t start) {
        return parser.syntaxError(code, rule, start);
    }

private:
    // Disallowed methods; no impl.
    RuleHalf(const RuleHalf&);
    RuleHalf& operator=(const RuleHalf&);
};

// Store int32_t as a void* in a UVector.  DO NOT ASSUME sizeof(void*)
// is 32.  Assume sizeof(void*) >= 32.
inline void* _int32_to_voidPtr(int32_t x) {
    void* a = 0; // May be > 32 bits
    *(int32_t*)&a = x; // Careful here...
    return a;
}
inline int32_t _voidPtr_to_int32(void* x) {
    void* a = x; // Copy to stack (portability)
    return *(int32_t*)&a; // Careful here...
}

const UnicodeString RuleHalf::gOperators = OPERATORS;

RuleHalf::RuleHalf(TransliteratorParser& p) : parser(p) {
    cursor = -1;
    ante = -1;
    post = -1;
    segments = NULL;
    maxRef = -1;
    cursorOffset = 0;
    anchorStart = anchorEnd = FALSE;
}

RuleHalf::~RuleHalf() {
    delete segments;
}

/**
 * Parse one side of a rule, stopping at either the limit,
 * the END_OF_RULE character, or an operator.  Return
 * the pos of the terminating character (or limit).
 */
int32_t RuleHalf::parse(const UnicodeString& rule, int32_t pos, int32_t limit) {
    int32_t start = pos;
    UnicodeString& buf = text;
    ParsePosition pp;
    int32_t cursorOffsetPos = 0; // Position of first CURSOR_OFFSET on _right_
    UnicodeString scratch;
    bool_t done = FALSE;

    while (pos < limit && !done) {
        UChar c = rule.charAt(pos++);
        if (Unicode::isWhitespace(c)) {
            // Ignore whitespace.  Note that this is not Unicode
            // spaces, but Java spaces -- a subset, representing
            // whitespace likely to be seen in code.
            continue;
        }
        if (gOperators.indexOf(c) >= 0) {
            --pos; // Backup to point to operator
            break;
        }
        if (anchorEnd) {
            // Text after a presumed end anchor is a syntax err
            return syntaxError(RuleBasedTransliterator::MALFORMED_VARIABLE_REFERENCE, rule, start);
        }
        // Handle escapes
        if (c == ESCAPE) {
            if (pos == limit) {
                return syntaxError(RuleBasedTransliterator::TRAILING_BACKSLASH, rule, start);
            }
            UChar32 escaped = rule.unescapeAt(pos); // pos is already past '\\'
            if (escaped == (UChar32) -1) {
                return syntaxError(RuleBasedTransliterator::MALFORMED_UNICODE_ESCAPE, rule, start);
            }
            buf.append(escaped);
            continue;
        }
        // Handle quoted matter
        if (c == QUOTE) {
            int32_t iq = rule.indexOf(QUOTE, pos);
            if (iq == pos) {
                buf.append(c); // Parse [''] outside quotes as [']
                ++pos;
            } else {
                /* This loop picks up a segment of quoted text of the
                 * form 'aaaa' each time through.  If this segment
                 * hasn't really ended ('aaaa''bbbb') then it keeps
                 * looping, each time adding on a new segment.  When it
                 * reaches the final quote it breaks.
                 */
                for (;;) {
                    if (iq < 0) {
                        return syntaxError(RuleBasedTransliterator::UNTERMINATED_QUOTE, rule, start);
                    }
                    scratch.truncate(0);
                    rule.extractBetween(pos, iq, scratch);
                    buf.append(scratch);
                    pos = iq+1;
                    if (pos < limit && rule.charAt(pos) == QUOTE) {
                        // Parse [''] inside quotes as [']
                        iq = rule.indexOf(QUOTE, pos+1);
                        // Continue looping
                    } else {
                        break;
                    }
                }
            }
            continue;
        }
        switch (c) {
        case ANCHOR_START:
            if (buf.length() == 0 && !anchorStart) {
                anchorStart = TRUE;
            } else {
              return syntaxError(RuleBasedTransliterator::MISPLACED_ANCHOR_START,
                                 rule, start);
            }
          break;
        case SEGMENT_OPEN:
        case SEGMENT_CLOSE:
            // Handle segment definitions "(" and ")"
            // Parse "(", ")"
            if (segments == NULL) {
                segments = new UVector();
            }
            if ((c == SEGMENT_OPEN) !=
                (segments->size() % 2 == 0)) {
                return syntaxError(RuleBasedTransliterator::MISMATCHED_SEGMENT_DELIMITERS,
                                   rule, start);
            }
            segments->addElement(_int32_to_voidPtr(buf.length()));
            break;
        case END_OF_RULE:
            --pos; // Backup to point to END_OF_RULE
            done = TRUE;
            break;
        case SymbolTable::SYMBOL_REF:
            // Handle variable references and segment references "$1" .. "$9"
            {
                // A variable reference must be followed immediately
                // by a Unicode identifier start and zero or more
                // Unicode identifier part characters, or by a digit
                // 1..9 if it is a segment reference.
                if (pos == limit) {
                    // A variable ref character at the end acts as
                    // an anchor to the context limit, as in perl.
                    anchorEnd = TRUE;
                    break;
                }
                // Parse "$1" "$2" .. "$9"
                c = rule.charAt(pos);
                int32_t r = Unicode::digit(c, 10);
                if (r >= 1 && r <= 9) {
                    if (r > maxRef) {
                        maxRef = r;
                    }
                    buf.append(parser.data->getSegmentStandin(r));
                    ++pos;
                } else {
                    pp.setIndex(pos);
                    UnicodeString name = parser.parseData->
                                    parseReference(rule, pp, limit);
                    if (name.length() == 0) {
                        // This means the '$' was not followed by a
                        // valid name.  Try to interpret it as an
                        // end anchor then.  If this also doesn't work
                        // (if we see a following character) then signal
                        // an error.
                        anchorEnd = TRUE;
                        break;
                    }
                    pos = pp.getIndex();
                    // If this is a variable definition statement,
                    // then the LHS variable will be undefined.  In
                    // that case appendVariableDef() will append the
                    // special placeholder char variableLimit-1.

                    parser.appendVariableDef(name, buf);
                }
            }
            break;
        case CONTEXT_ANTE:
            if (ante >= 0) {
                return syntaxError(RuleBasedTransliterator::MULTIPLE_ANTE_CONTEXTS, rule, start);
            }
            ante = buf.length();
            break;
        case CONTEXT_POST:
            if (post >= 0) {
                return syntaxError(RuleBasedTransliterator::MULTIPLE_POST_CONTEXTS, rule, start);
            }
            post = buf.length();
            break;
        case SET_OPEN:
            pp.setIndex(pos-1); // Backup to opening '['
            buf.append(parser.parseSet(rule, pp));
            if (U_FAILURE(parser.status)) {
                return syntaxError(RuleBasedTransliterator::MALFORMED_SET, rule, start);
            }
            pos = pp.getIndex();
            break;
        case CURSOR_POS:
            if (cursor >= 0) {
                return syntaxError(RuleBasedTransliterator::MULTIPLE_CURSORS, rule, start);
            }
            cursor = buf.length();
            break;
        case CURSOR_OFFSET:
            if (cursorOffset < 0) {
                if (buf.length() > 0) {
                    return syntaxError(RuleBasedTransliterator::MISPLACED_CURSOR_OFFSET, rule, start);
                }
                --cursorOffset;
            } else if (cursorOffset > 0) {
                if (buf.length() != cursorOffsetPos || cursor >= 0) {
                    return syntaxError(RuleBasedTransliterator::MISPLACED_CURSOR_OFFSET, rule, start);
                }
                ++cursorOffset;
            } else {
                if (cursor == 0 && buf.length() == 0) {
                    cursorOffset = -1;
                } else if (cursor < 0) {
                    cursorOffsetPos = buf.length();
                    cursorOffset = 1;
                } else {
                    return syntaxError(RuleBasedTransliterator::MISPLACED_CURSOR_OFFSET, rule, start);
                }
            }
            break;
        case KLEENE_STAR:
        case ONE_OR_MORE:
            // Very limited initial implementation.  Note that this
            // works strangely for quotes and variables --
            //  'foo'* => fo o*
            //  $a = foo; $a * => fo o*
            // We will fix this later so that
            //  'foo'* => (foo) *
            //  $a = foo; $a * => (foo) *
            // Implement with hidden segments, perhaps at # 10+.
            {
                int32_t start, limit;
                if (segments != 0 &&
                    segments->size() >= 2 &&
                    segments->size() % 2 == 0 &&
                    _voidPtr_to_int32(segments->elementAt(segments->size()-1)) == buf.length()) {
                    // The * immediately follows a segment
                    int32_t len = segments->size();
                    start = _voidPtr_to_int32(segments->elementAt(len - 2));
                    limit = _voidPtr_to_int32(segments->elementAt(len - 1));
                    segments->setElementAt(_int32_to_voidPtr(start+1), len-1);
                } else {
                    // The * follows an isolated character
                    // (or quote, or variable reference)
                    start = buf.length() - 1;
                    limit = start + 1;
                }
                UnicodeMatcher *m =
                    new StringMatcher(buf, start, limit, *parser.data);
                m = new Quantifier(m, (c == ONE_OR_MORE)?1:0, 0x7FFFFFFF);
                buf.truncate(start);
                buf.append(parser.generateStandInFor(m));
            }
            break;
        // case SET_CLOSE:
        default:
            // Disallow unquoted characters other than [0-9A-Za-z]
            // in the printable ASCII range.  These characters are
            // reserved for possible future use.
            if (c >= 0x0021 && c <= 0x007E &&
                !((c >= 0x0030/*'0'*/ && c <= 0x0039/*'9'*/) ||
                  (c >= 0x0041/*'A'*/ && c <= 0x005A/*'Z'*/) ||
                  (c >= 0x0061/*'a'*/ && c <= 0x007A/*'z'*/))) {
                return syntaxError(RuleBasedTransliterator::UNQUOTED_SPECIAL, rule, start);
            }
            buf.append(c);
            break;
        }
    }

    if (cursorOffset > 0 && cursor != cursorOffsetPos) {
        return syntaxError(RuleBasedTransliterator::MISPLACED_CURSOR_OFFSET, rule, start);
    }
    // text = buf.toString();
    return pos;
}

/**
 * Remove context.
 */
void RuleHalf::removeContext() {
    //text = text.substring(ante < 0 ? 0 : ante,
    //                      post < 0 ? text.length() : post);
    if (post >= 0) {
        text.remove(post);
    }
    if (ante >= 0) {
        text.removeBetween(0, ante);
    }
    ante = post = -1;
    anchorStart = anchorEnd = FALSE;
}

/**
 * Create and return an int32_t[] array of segments.
 */
int32_t* RuleHalf::createSegments() const {
    if (segments == NULL) {
        return NULL;
    }
    int32_t len = segments->size();
    int32_t* result = new int32_t[len + 1];
    for (int32_t i=0; i<len; ++i) {
        result[i] = _voidPtr_to_int32(segments->elementAt(i));
    }
    result[len] = -1; // end marker
    return result;
}

//----------------------------------------------------------------------
// END RuleHalf
//----------------------------------------------------------------------

TransliterationRuleData*
TransliteratorParser::parse(const UnicodeString& rules,
                            UTransDirection direction,
                            UParseError* parseError) {
    TransliteratorParser parser(rules, direction, parseError);
    UnicodeString idBlock;
    int32_t idSplitPoint, count;
    parser.parseRules(idBlock, idSplitPoint, count);
    if (U_FAILURE(parser.status) || idBlock.length() != 0) {
        delete parser.data;
        parser.data = 0;
    }
    return parser.data;
}

/**
 * Parse a given set of rules.  Return up to three pieces of
 * parsed data.  These are the header ::id block, the rule block,
 * and the footer ::id block.  Any or all of these may be empty.
 * If the ::id blocks are empty, their corresponding parameters
 * are returned as the empty string.  If there are no rules, the
 * TransliterationRuleData result is 0.
 * @param ruleDataResult caller owns the pointer stored here.
 * May be NULL.
 * @param headerRule string including semicolons for the header
 * ::id block.  May be empty.
 * @param footerRule string including semicolons for the footer
 * ::id block.  May be empty.
 */
void TransliteratorParser::parse(const UnicodeString& rules,
                                 UTransDirection direction,
                                 TransliterationRuleData*& ruleDataResult,
                                 UnicodeString& idBlockResult,
                                 int32_t& idSplitPointResult,
                                 UParseError* parseError,
                                 UErrorCode& ec) {
    if (U_FAILURE(ec)) {
        ruleDataResult = 0;
        return;
    }
    TransliteratorParser parser(rules, direction, parseError);
    int32_t count;
    parser.parseRules(idBlockResult, idSplitPointResult, count);
    if (U_FAILURE(parser.status) || count == 0) {
        delete parser.data;
        parser.data = 0;
    }
    ruleDataResult = parser.data;
    ec = parser.status;
}

/**
 * @param rules list of rules, separated by newline characters
 * @exception IllegalArgumentException if there is a syntax error in the
 * rules
 */
TransliteratorParser::TransliteratorParser(
                                     const UnicodeString& theRules,
                                     UTransDirection theDirection,
                                     UParseError* theParseError) :
    rules(theRules), direction(theDirection), data(0), parseError(theParseError) {
    parseData = new ParseData(0, &variablesVector);
}

/**
 * Destructor.
 */
TransliteratorParser::~TransliteratorParser() {
    delete parseData;
}

/**
 * Parse the given string as a sequence of rules, separated by newline
 * characters ('\n'), and cause this object to implement those rules.  Any
 * previous rules are discarded.  Typically this method is called exactly
 * once, during construction.
 * @exception IllegalArgumentException if there is a syntax error in the
 * rules
 */
void TransliteratorParser::parseRules(UnicodeString& idBlockResult,
                                      int32_t& idSplitPointResult,
                                      int32_t& ruleCount) {
    status = U_ZERO_ERROR;
    ruleCount = 0;

    // Clear error struct
    if (parseError != 0) {
        parseError->code = parseError->line = 0;
        parseError->offset = 0;
        parseError->preContext[0] = parseError->postContext[0] = (UChar)0;
    }

    delete data;
    data = new TransliterationRuleData(status);
    if (U_FAILURE(status)) {
        return;
    }

    parseData->data = data;
    variablesVector.removeAllElements();
    if (parseError != 0) {
        parseError->code = 0;
    }
    determineVariableRange();

    UnicodeString str; // scratch
    idBlockResult.truncate(0);
    idSplitPointResult = -1;
    int32_t pos = 0;
    int32_t limit = rules.length();
    // The mode marks whether we are in the header ::id block, the
    // rule block, or the footer ::id block.
    // mode == 0: start: rule->1, ::id->0
    // mode == 1: in rules: rule->1, ::id->2
    // mode == 2: in footer rule block: rule->ERROR, ::id->2
    int32_t mode = 0;
    while (pos < limit && U_SUCCESS(status)) {
        UChar c = rules.charAt(pos++);
        if (Unicode::isWhitespace(c)) {
            // Ignore leading whitespace.
            continue;
        }
        // Skip lines starting with the comment character
        if (c == RULE_COMMENT_CHAR) {
            pos = rules.indexOf((UChar)0x000A /*\n*/, pos) + 1;
            if (pos == 0) {
                break; // No "\n" found; rest of rule is a commnet
            }
            continue; // Either fall out or restart with next line
        }
        // We've found the start of a rule or ID.  c is its first
        // character, and pos points past c.
        --pos;
        // Look for an ID token.  Must have at least ID_TOKEN_LEN + 1
        // chars left.
        if ((pos + ID_TOKEN_LEN + 1) <= limit &&
            rules.compare(pos, ID_TOKEN_LEN, ID_TOKEN) == 0) {
            pos += ID_TOKEN_LEN;
            c = rules.charAt(pos);
            while (Unicode::isWhitespace(c) && pos < limit) {
                ++pos;
                c = rules.charAt(pos);
            }
            int32_t p = pos;
            UBool sawDelim;
            UnicodeString regenID;
            Transliterator::parseID(rules, regenID, p, sawDelim, direction, NULL, FALSE);
            if (p == pos || !sawDelim) {
                // Invalid ::id
                status = U_ILLEGAL_ARGUMENT_ERROR;
            } else {
                if (mode == 1) {
                    mode = 2;
                    idSplitPointResult = idBlockResult.length();
                }
                rules.extractBetween(pos, p, str);
                idBlockResult.append(str);
                if (!sawDelim) {
                    idBlockResult.append((UChar)0x003B /*;*/);
                }
                pos = p;
            }
        } else {
            // Parse a rule
            pos = parseRule(pos, limit);
            if (U_SUCCESS(status)) {
                ++ruleCount;
                if (mode == 2) {
                    // ::id in illegal position (because a rule
                    // occurred after the ::id footer block)
                    status = U_ILLEGAL_ARGUMENT_ERROR;
                }
            }
            mode = 1;
        }
    }
    
    // Convert the set vector to an array
    data->variablesLength = variablesVector.size();
    data->variables = data->variablesLength == 0 ? 0 : new UnicodeMatcher*[data->variablesLength];
    // orphanElement removes the given element and shifts all other
    // elements down.  For performance (and code clarity) we work from
    // the end back to index 0.
    int32_t i;
    for (i=data->variablesLength; i>0; ) {
        --i;
        data->variables[i] =
            (UnicodeSet*) variablesVector.orphanElementAt(i);
    }

    // Index the rules
    if (U_SUCCESS(status)) {
        data->ruleSet.freeze(status);
        if (idSplitPointResult < 0) {
            idSplitPointResult = idBlockResult.length();
        }
    }
}

/**
 * MAIN PARSER.  Parse the next rule in the given rule string, starting
 * at pos.  Return the index after the last character parsed.  Do not
 * parse characters at or after limit.
 *
 * Important:  The character at pos must be a non-whitespace character
 * that is not the comment character.
 *
 * This method handles quoting, escaping, and whitespace removal.  It
 * parses the end-of-rule character.  It recognizes context and cursor
 * indicators.  Once it does a lexical breakdown of the rule at pos, it
 * creates a rule object and adds it to our rule list.
 */
int32_t TransliteratorParser::parseRule(int32_t pos, int32_t limit) {
    // Locate the left side, operator, and right side
    int32_t start = pos;
    UChar op = 0;
    const UnicodeString& rule = rules; // TEMPORARY: FIX LATER

    // Use pointers to automatics to make swapping possible.
    RuleHalf _left(*this), _right(*this);
    RuleHalf* left = &_left;
    RuleHalf* right = &_right;

    undefinedVariableName.remove();
    pos = left->parse(rule, pos, limit);
    if (U_FAILURE(status)) {
        return start;
    }

    if (pos == limit ||
        gOPERATORS.indexOf(op = rule.charAt(pos++)) < 0) {
        return syntaxError(RuleBasedTransliterator::MISSING_OPERATOR, rule, start);
    }

    // Found an operator char.  Check for forward-reverse operator.
    if (op == REVERSE_RULE_OP &&
        (pos < limit && rule.charAt(pos) == FORWARD_RULE_OP)) {
        ++pos;
        op = FWDREV_RULE_OP;
    }

    pos = right->parse(rule, pos, limit);
    if (U_FAILURE(status)) {
        return start;
    }

    if (pos < limit) {
        if (rule.charAt(pos) == END_OF_RULE) {
            ++pos;
        } else {
            // RuleHalf parser must have terminated at an operator
            return syntaxError(RuleBasedTransliterator::UNQUOTED_SPECIAL, rule, start);
        }
    }

    if (op == VARIABLE_DEF_OP) {
        // LHS is the name.  RHS is a single character, either a literal
        // or a set (already parsed).  If RHS is longer than one
        // character, it is either a multi-character string, or multiple
        // sets, or a mixture of chars and sets -- syntax error.

        // We expect to see a single undefined variable (the one being
        // defined).
        if (undefinedVariableName.length() == 0) {
            // "Missing '$' or duplicate definition"
            return syntaxError(RuleBasedTransliterator::BAD_VARIABLE_DEFINITION, rule, start);
        }
        if (left->text.length() != 1 || left->text.charAt(0) != variableLimit) {
            // "Malformed LHS"
            return syntaxError(RuleBasedTransliterator::MALFORMED_VARIABLE_DEFINITION, rule, start);
        }
        if (left->anchorStart || left->anchorEnd ||
            right->anchorStart || right->anchorEnd) {
            return syntaxError(RuleBasedTransliterator::MALFORMED_VARIABLE_DEFINITION, rule, start);
        } 
        // We allow anything on the right, including an empty string.
        UnicodeString* value = new UnicodeString(right->text);
        data->variableNames->put(undefinedVariableName, value, status);

        ++variableLimit;
        return pos;
    }

    // If this is not a variable definition rule, we shouldn't have
    // any undefined variable names.
    if (undefinedVariableName.length() != 0) {
        syntaxError(// "Undefined variable $" + undefinedVariableName,
                    RuleBasedTransliterator::UNDEFINED_VARIABLE,
                    rule, start);
    }

    // If the direction we want doesn't match the rule
    // direction, do nothing.
    if (op != FWDREV_RULE_OP &&
        ((direction == UTRANS_FORWARD) != (op == FORWARD_RULE_OP))) {
        return pos;
    }

    // Transform the rule into a forward rule by swapping the
    // sides if necessary.
    if (direction == UTRANS_REVERSE) {
        left = &_right;
        right = &_left;
    }

    // Remove non-applicable elements in forward-reverse
    // rules.  Bidirectional rules ignore elements that do not
    // apply.
    if (op == FWDREV_RULE_OP) {
        right->removeContext();
        delete right->segments;
        right->segments = NULL;
        left->cursor = left->maxRef = -1;
        left->cursorOffset = 0;
    }

    // Normalize context
    if (left->ante < 0) {
        left->ante = 0;
    }
    if (left->post < 0) {
        left->post = left->text.length();
    }

    // Context is only allowed on the input side.  Cursors are only
    // allowed on the output side.  Segment delimiters can only appear
    // on the left, and references on the right.  Cursor offset
    // cannot appear without an explicit cursor.  Cursor offset
    // cannot place the cursor outside the limits of the context.
    // Anchors are only allowed on the input side.
    if (right->ante >= 0 || right->post >= 0 || left->cursor >= 0 ||
        right->segments != NULL || left->maxRef >= 0 ||
        (right->cursorOffset != 0 && right->cursor < 0) ||
        (right->cursorOffset > (left->text.length() - left->post)) ||
        (-right->cursorOffset > left->ante) ||
        right->anchorStart || right->anchorEnd) {

        return syntaxError(RuleBasedTransliterator::MALFORMED_RULE, rule, start);
    }

    // Check integrity of segments and segment references.  Each
    // segment's start must have a corresponding limit, and the
    // references must not refer to segments that do not exist.
    if (left->segments != NULL) {
        int n = left->segments->size();
        if (n % 2 != 0) {
            return syntaxError(RuleBasedTransliterator::MISSING_SEGMENT_CLOSE, rule, start);
        }
        n /= 2;
        if (right->maxRef > n) {
            return syntaxError(RuleBasedTransliterator::UNDEFINED_SEGMENT_REFERENCE, rule, start);
        }
    }

    data->ruleSet.addRule(new TransliterationRule(
                                 left->text, left->ante, left->post,
                                 right->text, right->cursor, right->cursorOffset,
                                 left->createSegments(),
                                 left->anchorStart, left->anchorEnd,
                                 *data,
                                 status), status);

    return pos;
}

/**
 * Called by main parser upon syntax error.  Search the rule string
 * for the probable end of the rule.  Of course, if the error is that
 * the end of rule marker is missing, then the rule end will not be found.
 * In any case the rule start will be correctly reported.
 * @param msg error description
 * @param rule pattern string
 * @param start position of first character of current rule
 */
int32_t TransliteratorParser::syntaxError(int32_t parseErrorCode,
                                               const UnicodeString& rule,
                                               int32_t start) {
    if (parseError != 0) {
        parseError->code = parseErrorCode;
        parseError->line = 0; // We don't return a line #
        parseError->offset = start; // Character offset from rule start
        int32_t end = quotedIndexOf(rule, start, rule.length(), END_OF_RULE);
        if (end < 0) {
            end = rule.length();
        }
        int32_t len = uprv_min(end - start, U_PARSE_CONTEXT_LEN-1);
        // Extract everything into the preContext and leave the postContext
        // blank, since we don't have precise error position.
        // TODO: Fix this.
        rule.extract(start, len, parseError->preContext); // Current rule
        parseError->preContext[len] = 0;
        parseError->postContext[0] = 0;
    }
    status = U_ILLEGAL_ARGUMENT_ERROR;
    return start;
}

/**
 * Parse a UnicodeSet out, store it, and return the stand-in character
 * used to represent it.
 */
UChar TransliteratorParser::parseSet(const UnicodeString& rule,
                                          ParsePosition& pos) {
    UnicodeSet* set = new UnicodeSet(rule, pos, *parseData, status);
    set->compact();
    return generateStandInFor(set);
}

/**
 * Generate and return a stand-in for a new UnicodeMatcher.  Store
 * the matcher (adopt it).
 */
UChar TransliteratorParser::generateStandInFor(UnicodeMatcher* adopted) {
    // assert(adopted != 0);
    if (variableNext >= variableLimit) {
        // throw new RuntimeException("Private use variables exhausted");
        delete adopted;
        status = U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }
    variablesVector.addElement(adopted);
    return variableNext++;
}

/**
 * Append the value of the given variable name to the given
 * UnicodeString.
 */
void TransliteratorParser::appendVariableDef(const UnicodeString& name,
                                                  UnicodeString& buf) {
    const UnicodeString* s = (const UnicodeString*) data->variableNames->get(name);
    if (s == NULL) {
        // We allow one undefined variable so that variable definition
        // statements work.  For the first undefined variable we return
        // the special placeholder variableLimit-1, and save the variable
        // name.
        if (undefinedVariableName.length() == 0) {
            undefinedVariableName = name;
            if (variableNext >= variableLimit) {
                // throw new RuntimeException("Private use variables exhausted");
                status = U_ILLEGAL_ARGUMENT_ERROR;
                return;
            }
            buf.append((UChar) --variableLimit);
        } else {
            //throw new IllegalArgumentException("Undefined variable $"
            //                                   + name);
            status = U_ILLEGAL_ARGUMENT_ERROR;
            return;
        }
    } else {
        buf.append(*s);
    }
}

/**
 * Determines what part of the private use region of Unicode we can use for
 * variable stand-ins.  The correct way to do this is as follows: Parse each
 * rule, and for forward and reverse rules, take the FROM expression, and
 * make a hash of all characters used.  The TO expression should be ignored.
 * When done, everything not in the hash is available for use.  In practice,
 * this method may employ some other algorithm for improved speed.
 */
void TransliteratorParser::determineVariableRange(void) {
    UnicodeRange privateUse(0xE000, 0x1900); // Private use area

    UnicodeRange* r = privateUse.largestUnusedSubrange(rules);

    data->variablesBase = variableNext = variableLimit = (UChar) 0;
    
    if (r != 0) {
        // Allocate 9 characters for segment references 1 through 9
        data->segmentBase = r->start;
        data->variablesBase = variableNext = (UChar) (data->segmentBase + 9);
        variableLimit = (UChar) (r->start + r->length);
        delete r;
    }

    if (variableNext >= variableLimit) {
        status = U_ILLEGAL_ARGUMENT_ERROR;
    }
}

/**
 * Returns the index of a character, ignoring quoted text.
 * For example, in the string "abc'hide'h", the 'h' in "hide" will not be
 * found by a search for 'h'.
 */
int32_t TransliteratorParser::quotedIndexOf(const UnicodeString& text,
                                                 int32_t start, int32_t limit,
                                                 UChar charToFind) {
    for (int32_t i=start; i<limit; ++i) {
        UChar c = text.charAt(i);
        if (c == ESCAPE) {
            ++i;
        } else if (c == QUOTE) {
            while (++i < limit
                   && text.charAt(i) != QUOTE) {}
        } else if (c == charToFind) {
            return i;
        }
    }
    return -1;
}
