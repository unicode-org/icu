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
#include "unicode/uchar.h"
#include "unicode/ustring.h"
#include "unicode/uniset.h"

// Operators
#define VARIABLE_DEF_OP ((UChar)0x003D) /*=*/
#define FORWARD_RULE_OP ((UChar)0x003E) /*>*/
#define REVERSE_RULE_OP ((UChar)0x003C) /*<*/
#define FWDREV_RULE_OP  ((UChar)0x007E) /*~*/ // internal rep of <> op

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
#define ZERO_OR_ONE        ((UChar)0x003F) /*?*/

// By definition, the ANCHOR_END special character is a
// trailing SymbolTable.SYMBOL_REF character.
// private static final char ANCHOR_END       = '$';

static const UChar gOPERATORS[] = {
    0x3D, 0x3E, 0x3C, 0     // "=><"
};

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
        if ((i==start && !u_isIDStart(c)) || !u_isIDPart(c)) {
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
// Segments
//----------------------------------------------------------------------

/**
 * Segments are parentheses-enclosed regions of the input string.
 * These are referenced in the output string using the notation $1,
 * $2, etc.  Numbering is in order of appearance of the left
 * parenthesis.  Number is one-based.  Segments are defined as start,
 * limit pairs.  Segments may nest.
 *
 * During parsing, segment data is encoded in an object of class
 * Segments.  At runtime, the same data is encoded in compact form as
 * an array of integers in a TransliterationRule.  The runtime encoding
 * must satisfy three goals:
 *
 * 1. Iterate over the offsets in a pattern, from left to right,
 *    and indicate all segment boundaries, in order.  This is done
 *    during matching.
 *
 * 2. Given a reference $n, produce the start and limit offsets
 *    for that segment.  This is done during replacement.
 *
 * 3. Similar to goal 1, but in addition, indicate whether each
 *    segment boundary is a start or a limit, in other words, whether
 *    each is an open paren or a close paren.  This is required by
 *    the toRule() method.
 *
 * Goal 1 must be satisfied at high speed since this is done during
 * matching.  Goal 2 is next most important.  Goal 3 is not performance
 * critical since it is only needed by toRule().
 *
 * The array of integers is actually two arrays concatenated.  The
 * first gives the index values of the open and close parentheses in
 * the order they appear.  The second maps segment numbers to the
 * indices of the first array.  The two arrays have the same length.
 * Iterating over the first array satisfies goal 1.  Indexing into the
 * second array satisfies goal 2.  Goal 3 is satisfied by iterating
 * over the second array and constructing the required data when
 * needed.  This is what toRule() does.
 *
 * Example:  (a b(c d)e f)
 *            0 1 2 3 4 5 6
 *
 * First array: Indices are 0, 2, 4, and 6.
 
 * Second array: $1 is at 0 and 6, and $2 is at 2 and 4, so the
 * second array is 0, 3, 1 2 -- these give the indices in the
 * first array at which $1:open, $1:close, $2:open, and $2:close
 * occur.
 *
 * The final array is: 2, 7, 0, 2, 4, 6, -1, 2, 5, 3, 4, -1
 *
 * Each subarray is terminated with a -1, and two leading entries
 * give the number of segments and the offset to the first entry
 * of the second array.  In addition, the second array value are
 * all offset by 2 so they index directly into the final array.
 * The total array size is 4*segments[0] + 4.  The second index is
 * 2*segments[0] + 3.
 *
 * In the output string, a segment reference is indicated by a
 * character in a special range, as defined by
 * RuleBasedTransliterator.Data.
 *
 * Most rules have no segments, in which case segments is null, and the
 * output string need not be checked for segment reference characters.
 *
 * See also rbt_rule.h/cpp.
 */
class Segments {
    UVector offsets;
    UVector isOpenParen;
public:
    Segments();
    ~Segments();
    void addParenthesisAt(int32_t offset, UBool isOpenParen);
    int32_t getLastParenOffset(UBool& isOpenParen) const;
    UBool extractLastParenSubstring(int32_t& start, int32_t& limit);
    int32_t* createArray() const;
    UBool validate() const;
    int32_t count() const; // number of segments
private:
    int32_t offset(int32_t i) const;
    UBool isOpen(int32_t i) const;
    int32_t size() const; // size of the UVectors
};

int32_t Segments::offset(int32_t i) const {
    return offsets.elementAti(i);
}

UBool Segments::isOpen(int32_t i) const {
    return isOpenParen.elementAti(i) != 0;
}

int32_t Segments::size() const {
    // assert(offset.size() == isOpenParen.size());
    return offsets.size();
}

Segments::Segments() {}
Segments::~Segments() {}

void Segments::addParenthesisAt(int32_t offset, UBool isOpen) {
    offsets.addElement(offset);
    isOpenParen.addElement(isOpen ? 1 : 0);
}

int32_t Segments::getLastParenOffset(UBool& isOpenParen) const {
    if (size() == 0) {
        return -1;
    }
    isOpenParen = isOpen(size()-1);
    return offset(size()-1);
}

// Remove the last (rightmost) segment.  Store its offsets in start
// and limit, and then convert all offsets at or after start to be
// equal to start.  Upon failure, return FALSE.  Assume that the
// caller has already called getLastParenOffset() and validated that
// there is at least one parenthesis and that the last one is a close
// paren.
UBool Segments::extractLastParenSubstring(int32_t& start, int32_t& limit) {
    // assert(offsets.size() > 0);
    // assert(isOpenParen.elementAt(isOpenParen.size()-1) == 0);
    int32_t i = size() - 1;
    int32_t n = 1; // count of close parens we need to match
    // Record position of the last close paren
    limit = offset(i);
    --i; // back up to the one before the last one
    while (i >= 0 && n != 0) {
        n += isOpen(i) ? -1 : 1;
    }
    if (n != 0) {
        return FALSE;
    }
    // assert(i>=0);
    start = offset(i);
    // Reset all segment pairs from i to size() - 1 to [start, start+1).
    while (i<size()) {
        int32_t o = isOpen(i) ? start : (start+1);
        offsets.setElementAt(o, i);
        ++i;
    }
    return TRUE;
}

// Assume caller has already gotten a TRUE validate().
int32_t* Segments::createArray() const {
    int32_t c = count(); // number of segments
    int32_t arrayLen = 4*c + 4;
    int32_t *array = new int32_t[arrayLen];
    int32_t a2offset = 2*c + 3; // offset to array 2
    array[0] = c;
    array[1] = a2offset;
    int32_t i;
    for (i=0; i<2*c; ++i) {
        array[2+i] = offset(i);
    }
    array[a2offset-1] = -1;
    array[arrayLen-1] = -1;
    // Now walk through and match up segment numbers with parentheses.
    // Number segments from 0.  We're going to offset all entries by 2
    // to skip the first two elements, array[0] and array[1].
    UStack stack;
    int32_t nextOpen = 0; // seg # of next open, 0-based
    int32_t j = a2offset; // index of start of array 2
    for (i=0; i<2*c; ++i) {
        UBool open = isOpen(i);
        // Let seg be the zero-based segment number.
        // Open parens are at 2*seg in array 2.
        // Close parens are at 2*seg+1 in array 2.
        if (open) {
            array[a2offset + 2*nextOpen] = 2+i;
            stack.push(nextOpen);
            ++nextOpen;
        } else {
            int32_t nextClose = stack.popi();
            array[a2offset + 2*nextClose+1] = 2+i;
        }
    }
    // assert(stack.empty());
    return array;
}

UBool Segments::validate() const {
    // want number of parens >= 2
    // want number of parens to be even
    // want first paren '('
    // want parens to match up in the end
    if ((size() < 2) || (size() % 2 != 0) || !isOpen(0)) {
        return FALSE;
    }
    int32_t n = 0;
    for (int32_t i=0; i<size(); ++i) {
        n += isOpen(i) ? 1 : -1;
        if (n < 0) {
            return FALSE;
        }
    }
    return n == 0;
}

// Assume caller has already gotten a TRUE validate().
int32_t Segments::count() const {
    // assert(validate());
    return size() / 2;
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
    Segments* segments;
    int32_t maxRef;       // index of largest ref ($n) on the right

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
    int32_t quoteStart = -1; // Most recent 'single quoted string'
    int32_t quoteLimit = -1;
    int32_t varStart = -1; // Most recent $variableReference
    int32_t varLimit = -1;

    while (pos < limit && !done) {
        UChar c = rule.charAt(pos++);
        if (u_isWhitespace(c)) {
            // Ignore whitespace.  Note that this is not Unicode
            // spaces, but Java spaces -- a subset, representing
            // whitespace likely to be seen in code.
            continue;
        }
        if (u_strchr(gOPERATORS, c) != NULL) {
            --pos; // Backup to point to operator
            break;
        }
        if (anchorEnd) {
            // Text after a presumed end anchor is a syntax err
            return syntaxError(U_MALFORMED_VARIABLE_REFERENCE, rule, start);
        }
        // Handle escapes
        if (c == ESCAPE) {
            if (pos == limit) {
                return syntaxError(U_TRAILING_BACKSLASH, rule, start);
            }
            UChar32 escaped = rule.unescapeAt(pos); // pos is already past '\\'
            if (escaped == (UChar32) -1) {
                return syntaxError(U_MALFORMED_UNICODE_ESCAPE, rule, start);
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
                quoteStart = buf.length();
                for (;;) {
                    if (iq < 0) {
                        return syntaxError(U_UNTERMINATED_QUOTE, rule, start);
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
                quoteLimit = buf.length();
            }
            continue;
        }
        switch (c) {
        case ANCHOR_START:
            if (buf.length() == 0 && !anchorStart) {
                anchorStart = TRUE;
            } else {
              return syntaxError(U_MISPLACED_ANCHOR_START,
                                 rule, start);
            }
          break;
        case SEGMENT_OPEN:
        case SEGMENT_CLOSE:
            // Handle segment definitions "(" and ")"
            // Parse "(", ")"
            if (segments == NULL) {
                segments = new Segments();
            }
            segments->addParenthesisAt(buf.length(), c == SEGMENT_OPEN);
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
                // Parse "$1" "$2" .. "$9" .. (no upper limit)
                c = rule.charAt(pos);
                int32_t r = u_charDigitValue(c);
                if (r >= 1 && r <= 9) {
                    ++pos;
                    for (;;) {
                        c = rule.charAt(pos);
                        int32_t d = u_charDigitValue(c);
                        if (d < 0) {
                            break;
                        }
                        if (r > 214748364 ||
                            (r == 214748364 && d > 7)) {
                            return syntaxError(U_UNDEFINED_SEGMENT_REFERENCE,
                                               rule, start);
                        }
                        r = 10*r + d;
                    }
                    if (r > maxRef) {
                        maxRef = r;
                    }
                    buf.append(parser.getSegmentStandin(r));
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
                    varStart = buf.length();
                    parser.appendVariableDef(name, buf);
                    varLimit = buf.length();
                }
            }
            break;
        case CONTEXT_ANTE:
            if (ante >= 0) {
                return syntaxError(U_MULTIPLE_ANTE_CONTEXTS, rule, start);
            }
            ante = buf.length();
            break;
        case CONTEXT_POST:
            if (post >= 0) {
                return syntaxError(U_MULTIPLE_POST_CONTEXTS, rule, start);
            }
            post = buf.length();
            break;
        case SET_OPEN:
            pp.setIndex(pos-1); // Backup to opening '['
            buf.append(parser.parseSet(rule, pp));
            if (U_FAILURE(parser.status)) {
                return syntaxError(U_MALFORMED_SET, rule, start);
            }
            pos = pp.getIndex();
            break;
        case CURSOR_POS:
            if (cursor >= 0) {
                return syntaxError(U_MULTIPLE_CURSORS, rule, start);
            }
            cursor = buf.length();
            break;
        case CURSOR_OFFSET:
            if (cursorOffset < 0) {
                if (buf.length() > 0) {
                    return syntaxError(U_MISPLACED_CURSOR_OFFSET, rule, start);
                }
                --cursorOffset;
            } else if (cursorOffset > 0) {
                if (buf.length() != cursorOffsetPos || cursor >= 0) {
                    return syntaxError(U_MISPLACED_CURSOR_OFFSET, rule, start);
                }
                ++cursorOffset;
            } else {
                if (cursor == 0 && buf.length() == 0) {
                    cursorOffset = -1;
                } else if (cursor < 0) {
                    cursorOffsetPos = buf.length();
                    cursorOffset = 1;
                } else {
                    return syntaxError(U_MISPLACED_CURSOR_OFFSET, rule, start);
                }
            }
            break;
        case KLEENE_STAR:
        case ONE_OR_MORE:
        case ZERO_OR_ONE:
            // Quantifiers.  We handle single characters, quoted strings,
            // variable references, and segments.
            //  a+      matches  aaa
            //  'foo'+  matches  foofoofoo
            //  $v+     matches  xyxyxy if $v == xy
            //  (seg)+  matches  segsegseg
            {
                int32_t start, limit;
                UBool isOpenParen;
                UBool isSegment = FALSE;
                if (segments != 0 &&
                    segments->getLastParenOffset(isOpenParen) == buf.length()) {
                    // The */+ immediately follows a segment
                    if (isOpenParen) {
                        return syntaxError(U_MISPLACED_QUANTIFIER, rule, start);
                    }
                    if (!segments->extractLastParenSubstring(start, limit)) {
                        return syntaxError(U_MISMATCHED_SEGMENT_DELIMITERS, rule, start);
                    }
                    isSegment = TRUE;
                } else {
                    // The */+ follows an isolated character or quote
                    // or variable reference
                    if (buf.length() == quoteLimit) {
                        // The */+ follows a 'quoted string'
                        start = quoteStart;
                        limit = quoteLimit;
                    } else if (buf.length() == varLimit) {
                        // The */+ follows a $variableReference
                        start = varStart;
                        limit = varLimit;
                    } else {
                        // The */+ follows a single character
                        start = buf.length() - 1;
                        limit = start + 1;
                    }
                }
                UnicodeMatcher *m =
                    new StringMatcher(buf, start, limit, isSegment, *parser.data);
                int32_t min = 0;
                int32_t max = Quantifier::MAX;
                switch (c) {
                case ONE_OR_MORE:
                    min = 1;
                    break;
                case ZERO_OR_ONE:
                    min = 0;
                    max = 1;
                    break;
                // case KLEENE_STAR:
                //    do nothing -- min, max already set
                }
                m = new Quantifier(m, min, max);
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
                return syntaxError(U_UNQUOTED_SPECIAL, rule, start);
            }
            buf.append(c);
            break;
        }
    }

    if (cursorOffset > 0 && cursor != cursorOffsetPos) {
        return syntaxError(U_MISPLACED_CURSOR_OFFSET, rule, start);
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
    return (segments == 0) ? 0 : segments->createArray();
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
        //parseError->code = parseError->line = 0;
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
/*    if (parseError != 0) {
        parseError->code = 0;
    }
*/
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
        if (u_isWhitespace(c)) {
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
            while (u_isWhitespace(c) && pos < limit) {
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

    if (pos == limit || u_strchr(gOPERATORS, (op = rule.charAt(pos++))) == NULL) {
        return syntaxError(U_MISSING_OPERATOR, rule, start);
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
            return syntaxError(U_UNQUOTED_SPECIAL, rule, start);
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
            return syntaxError(U_BAD_VARIABLE_DEFINITION, rule, start);
        }
        if (left->text.length() != 1 || left->text.charAt(0) != variableLimit) {
            // "Malformed LHS"
            return syntaxError(U_MALFORMED_VARIABLE_DEFINITION, rule, start);
        }
        if (left->anchorStart || left->anchorEnd ||
            right->anchorStart || right->anchorEnd) {
            return syntaxError(U_MALFORMED_VARIABLE_DEFINITION, rule, start);
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
                    U_UNDEFINED_VARIABLE,
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
        // - The following two checks were used to ensure that the
        // - the cursor offset stayed within the ante- or postcontext.
        // - However, with the addition of quantifiers, we have to
        // - allow arbitrary cursor offsets and do runtime checking.
        //(right->cursorOffset > (left->text.length() - left->post)) ||
        //(-right->cursorOffset > left->ante) ||
        right->anchorStart || right->anchorEnd) {

        return syntaxError(U_MALFORMED_RULE, rule, start);
    }

    // Check integrity of segments and segment references.  Each
    // segment's start must have a corresponding limit, and the
    // references must not refer to segments that do not exist.
    if (left->segments != NULL) {
        if (!left->segments->validate()) {
            return syntaxError(U_MISSING_SEGMENT_CLOSE, rule, start);
        }
        int32_t n = left->segments->count();
        if (right->maxRef > n) {
            return syntaxError(U_UNDEFINED_SEGMENT_REFERENCE, rule, start);
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
//        parseError->code = parseErrorCode;
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
    status = (UErrorCode)parseErrorCode;
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

UChar TransliteratorParser::getSegmentStandin(int32_t r) {
    // assert(r>=1);
    if (r > data->segmentCount) {
        data->segmentCount = r;
        variableLimit = data->segmentBase - r + 1;
        if (variableNext >= variableLimit) {
            status = U_ILLEGAL_ARGUMENT_ERROR;
        }
    }
    return data->getSegmentStandin(r);
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
        // Segment references work down; variables work up.  We don't
        // know how many of each we will need.
        data->segmentBase = (UChar) (r->start + r->length - 1);
        data->segmentCount = 0;
        data->variablesBase = variableNext = (UChar) r->start;
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
