/*
**********************************************************************
*   Copyright (C) 1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   11/17/99    aliu        Creation.
**********************************************************************
*/
#include "rbt_pars.h"
#include "unicode/rbt.h"
#include "rbt_rule.h"
#include "unirange.h"
#include "rbt_data.h"
#include "unicode/uniset.h"
#include "cstring.h"
#include "unicode/parsepos.h"
#include "symtable.h"
#include "unicode/parseerr.h"

// Operators
const UChar TransliterationRuleParser::VARIABLE_DEF_OP = 0x003D/*=*/;
const UChar TransliterationRuleParser::FORWARD_RULE_OP = 0x003E/*>*/;
const UChar TransliterationRuleParser::REVERSE_RULE_OP = 0x003C/*<*/;
const UChar TransliterationRuleParser::FWDREV_RULE_OP  = 0x007E/*~*/; // internal rep of <> op
const UnicodeString TransliterationRuleParser::OPERATORS = UNICODE_STRING("=><", 3);

// Other special characters
const UChar TransliterationRuleParser::QUOTE = 0x0027/*'*/;
const UChar TransliterationRuleParser::ESCAPE = 0x005C/*\*/;
const UChar TransliterationRuleParser::END_OF_RULE = 0x003B/*;*/;
const UChar TransliterationRuleParser::RULE_COMMENT_CHAR = 0x0023/*#*/;

const UChar TransliterationRuleParser::VARIABLE_REF_OPEN = 0x007B/*{*/;
const UChar TransliterationRuleParser::VARIABLE_REF_CLOSE = 0x007D/*}*/;
const UChar TransliterationRuleParser::CONTEXT_OPEN = 0x0028/*(*/;
const UChar TransliterationRuleParser::CONTEXT_CLOSE = 0x0029/*)*/;
const UChar TransliterationRuleParser::SET_OPEN = 0x005B/*[*/;
const UChar TransliterationRuleParser::SET_CLOSE = 0x005D/*]*/;
const UChar TransliterationRuleParser::CURSOR_POS = 0x007C/*|*/;

//----------------------------------------------------------------------
// BEGIN ParseData
//----------------------------------------------------------------------

/**
 * This class implements the SymbolTable interface.  It is used
 * during parsing to give UnicodeSet access to variables that
 * have been defined so far.  Note that it uses setVariablesVector,
 * _not_ data.setVariables.
 */
class ParseData : public SymbolTable {
public:
    const TransliterationRuleData* data; // alias

    const UVector* setVariablesVector; // alias

    ParseData(const TransliterationRuleData* data = 0,
              const UVector* setVariablesVector = 0);

    /**
     * Lookup the object associated with this string and return it.
     * Return U_ILLEGAL_ARGUMENT_ERROR status if the name does not
     * exist.  Return a non-NULL set if the name is mapped to a set;
     * otherwise return a NULL set.
     */
    virtual void lookup(const UnicodeString& name, UChar& c, UnicodeSet*& set,
                        UErrorCode& status) const;
};

ParseData::ParseData(const TransliterationRuleData* d,
                     const UVector* sets) :
    data(d), setVariablesVector(sets) {}

/**
 * Implement SymbolTable API.  Lookup a variable, returning
 * either a Character, a UnicodeSet, or null.
 */
void ParseData::lookup(const UnicodeString& name, UChar& c, UnicodeSet*& set,
                       UErrorCode& status) const {
    c = data->lookupVariable(name, status);
    if (U_SUCCESS(status)) {
        int32_t i = c - data->setVariablesBase;
        set = (i < setVariablesVector->size()) ?
            (UnicodeSet*) setVariablesVector->elementAt(i) : 0;
    }
}

//----------------------------------------------------------------------
// END ParseData
//----------------------------------------------------------------------

TransliterationRuleData*
TransliterationRuleParser::parse(const UnicodeString& rules,
                                 RuleBasedTransliterator::Direction direction,
                                 ParseError* parseError) {
    TransliterationRuleParser parser(rules, direction, parseError);
    parser.parseRules();
    if (U_FAILURE(parser.status)) {
        delete parser.data;
        parser.data = 0;
    }
    return parser.data;
}

/**
 * @param rules list of rules, separated by newline characters
 * @exception IllegalArgumentException if there is a syntax error in the
 * rules
 */
TransliterationRuleParser::TransliterationRuleParser(
                                     const UnicodeString& theRules,
                                     RuleBasedTransliterator::Direction theDirection,
                                     ParseError* theParseError) :
    rules(theRules), direction(theDirection), data(0), parseError(theParseError) {
    parseData = new ParseData(0, &setVariablesVector);
}

/**
 * Destructor.
 */
TransliterationRuleParser::~TransliterationRuleParser() {
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
void TransliterationRuleParser::parseRules(void) {
    status = U_ZERO_ERROR;

    delete data;
    data = new TransliterationRuleData(status);
    if (U_FAILURE(status)) {
        return;
    }

    parseData->data = data;
    setVariablesVector.removeAllElements();
    if (parseError != 0) {
        parseError->code = 0;
    }
    determineVariableRange();

    int32_t pos = 0;
    int32_t limit = rules.length();
    while (pos < limit && U_SUCCESS(status)) {
        UChar c = rules.charAt(pos++);
        if (Unicode::isWhitespace(c)) {
            // Ignore leading whitespace.  Note that this is not
            // Unicode spaces, but Java spaces -- a subset,
            // representing whitespace likely to be seen in code.
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
        // We've found the start of a rule.  c is its first
        // character, and pos points past c.  Lexically parse the
        // rule into component pieces.
        pos = parseRule(--pos, limit);                    
    }
    
    // Convert the set vector to an array
    data->setVariablesLength = setVariablesVector.size();
    data->setVariables = new UnicodeSet*[data->setVariablesLength];
    // orphanElement removes the given element and shifts all other
    // elements down.  For performance (and code clarity) we work from
    // the end back to index 0.
    for (int32_t i=data->setVariablesLength; i>0; ) {
        --i;
        data->setVariables[i] =
            (UnicodeSet*) setVariablesVector.orphanElementAt(i);
    }

    // Index the rules
    if (U_SUCCESS(status)) {
        data->ruleSet.freeze(*data, status);
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
int32_t TransliterationRuleParser::parseRule(int32_t pos, int32_t limit) {
    // Locate the left side, operator, and right side
    int32_t start = pos;
    UChar op = 0;

    UnicodeString buf;
    int32_t cursor = -1; // position of cursor in buf
    int32_t ante = -1;   // position of ante context marker ')' in buf
    int32_t post = -1;   // position of post context marker '(' in buf
    int32_t postClose = -1; // position of post context close ')' in buf

    // Assigned to buf and its adjuncts after the LHS has been
    // parsed.  Thereafter, buf etc. refer to the RHS.
    UnicodeString left;
    int32_t leftCursor = -1, leftAnte = -1, leftPost = -1, leftPostClose = -1;

    UnicodeString scratch;

    while (pos < limit) {
        UChar c = rules.charAt(pos++);
        if (Unicode::isWhitespace(c)) {
            // Ignore whitespace.  Note that this is not Unicode
            // spaces, but Java spaces -- a subset, representing
            // whitespace likely to be seen in code.
            continue;
        }
        // Handle escapes
        if (c == ESCAPE) {
            if (pos == limit) {
                return syntaxError(RuleBasedTransliterator::TRAILING_BACKSLASH, rules, start);
            }
            // Parse \uXXXX escapes
            c = rules.charAt(pos++);
            if (c == 0x0075/*u*/) {
                if ((pos+4) > limit) {
                    return syntaxError(RuleBasedTransliterator::MALFORMED_UNICODE_ESCAPE, rules, start);
                }
                c = (UChar)0x0000;
                for (int32_t plim=pos+4; pos<plim; ++pos) { // [sic]
                    int32_t digit = Unicode::digit(rules.charAt(pos), 16);
                    if (digit<0) {
                        return syntaxError(RuleBasedTransliterator::MALFORMED_UNICODE_ESCAPE, rules, start);
                    }
                    c = (UChar) ((c << 4) | digit);
                }
            }

            buf.append(c);
            continue;
        }
        // Handle quoted matter
        if (c == QUOTE) {
            int32_t iq = rules.indexOf(QUOTE, pos);
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
                        return syntaxError(RuleBasedTransliterator::UNTERMINATED_QUOTE, rules, start);
                    }
                    scratch.truncate(0);
                    rules.extractBetween(pos, iq, scratch);
                    buf.append(scratch);
                    pos = iq+1;
                    if (pos < limit && rules.charAt(pos) == QUOTE) {
                        // Parse [''] inside quotes as [']
                        iq = rules.indexOf(QUOTE, pos+1);
                        // Continue looping
                    } else {
                        break;
                    }
                }
            }
            continue;
        }
        if (OPERATORS.indexOf(c) >= 0) {
            if (op != 0) {
                return syntaxError(RuleBasedTransliterator::UNQUOTED_SPECIAL, rules, start);
            }
            // Found an operator char.  Check for forward-reverse operator.
            if (c == REVERSE_RULE_OP &&
                (pos < limit && rules.charAt(pos) == FORWARD_RULE_OP)) {
                ++pos;
                op = FWDREV_RULE_OP;
            } else {
                op = c;
            }
            left = buf; // lhs
            leftCursor = cursor;
            leftAnte = ante;
            leftPost = post;
            leftPostClose = postClose;

            buf.truncate(0);
            cursor = ante = post = postClose = -1;
            continue;
        }
        if (c == END_OF_RULE) {
            break;
        }
        switch (c) {
        case VARIABLE_REF_OPEN:
            {
                int32_t j = rules.indexOf(VARIABLE_REF_CLOSE, pos);
                if (pos == j || j < 0) { // empty or unterminated
                    return syntaxError(RuleBasedTransliterator::MALFORMED_VARIABLE_REFERENCE, rules, start);
                }
                scratch.truncate(0);
                rules.extractBetween(pos, j, scratch);
                pos = j+1;
                UChar v = data->lookupVariable(scratch, status);
                if (U_FAILURE(status)) {
                    return syntaxError(RuleBasedTransliterator::UNDEFINED_VARIABLE, rules, start);
                }
                buf.append(v);
            }
            break;
        case CONTEXT_OPEN:
            if (post >= 0) {
                return syntaxError(RuleBasedTransliterator::MULTIPLE_POST_CONTEXTS, rules, start);
            }
            // Ignore CONTEXT_OPEN if buffer length is zero -- that means
            // this is the optional opening delimiter for the ante context.
            if (buf.length() > 0) {
                post = buf.length();
            }
            break;
        case CONTEXT_CLOSE:
            if (postClose >= 0) {
                return syntaxError(RuleBasedTransliterator::UNEXPECTED_CLOSE_CONTEXT, rules, start);
            }
            if (post >= 0) {
                // This is probably the optional closing delimiter
                // for the post context; save the pos and check later.
                postClose = buf.length();
            } else if (ante >= 0) {
                return syntaxError(RuleBasedTransliterator::MULTIPLE_ANTE_CONTEXTS, rules, start);
            } else {
                ante = buf.length();
            }
            break;
        case SET_OPEN: {
            ParsePosition pp(pos-1); // Backup to opening '['
            buf.append(registerSet(new UnicodeSet(rules, pp, *parseData, status)));
            if (U_FAILURE(status)) {
                return syntaxError(RuleBasedTransliterator::MALFORMED_SET, rules, start);
            }
            pos = pp.getIndex(); }
            break;
        case VARIABLE_REF_CLOSE:
        case SET_CLOSE:
            return syntaxError(RuleBasedTransliterator::UNQUOTED_SPECIAL, rules, start);
        case CURSOR_POS:
            if (cursor >= 0) {
                return syntaxError(RuleBasedTransliterator::MULTIPLE_CURSORS, rules, start);
            }
            cursor = buf.length();
            break;
        default:
            buf.append(c);
            break;
        }
    }
    if (op == 0) {
        return syntaxError(RuleBasedTransliterator::MISSING_OPERATOR, rules, start);
    }

    // Check context close parameters
    if ((leftPostClose >= 0 && leftPostClose != left.length()) ||
        (postClose >= 0 && postClose != buf.length())) {
        return syntaxError(RuleBasedTransliterator::TEXT_AFTER_CLOSE_CONTEXT, rules, start);
    }

    // Context is only allowed on the input side; that is, the left side
    // for forward rules.  Cursors are only allowed on the output side;
    // that is, the right side for forward rules.  Bidirectional rules
    // ignore elements that do not apply.

    switch (op) {
    case VARIABLE_DEF_OP:
        // LHS is the name.  RHS is a single character, either a literal
        // or a set (already parsed).  If RHS is longer than one
        // character, it is either a multi-character string, or multiple
        // sets, or a mixture of chars and sets -- syntax error.
        if (buf.length() != 1) {
            return syntaxError(RuleBasedTransliterator::MALFORMED_RHS, rules, start);
        }
        if (data->isVariableDefined(left)) {
            return syntaxError(RuleBasedTransliterator::DUPLICATE_VARIABLE_DEFINITION, rules, start);
        }
        data->defineVariable(left, buf.charAt(0), status);
        break;

    case FORWARD_RULE_OP:
        if (direction == RuleBasedTransliterator::FORWARD) {
            if (ante >= 0 || post >= 0 || leftCursor >= 0) {
                return syntaxError(RuleBasedTransliterator::MALFORMED_RULE, rules, start);
            }
            data->ruleSet.addRule(new TransliterationRule(
                                     left, leftAnte, leftPost,
                                     buf, cursor, status), status);
        } // otherwise ignore the rule; it's not the direction we want
        break;

    case REVERSE_RULE_OP:
        if (direction == RuleBasedTransliterator::REVERSE) {
            if (leftAnte >= 0 || leftPost >= 0 || cursor >= 0) {
                return syntaxError(RuleBasedTransliterator::MALFORMED_RULE, rules, start);
            }
            data->ruleSet.addRule(new TransliterationRule(
                                     buf, ante, post,
                                     left, leftCursor, status), status);
        } // otherwise ignore the rule; it's not the direction we want
        break;

    case FWDREV_RULE_OP:
        if (direction == RuleBasedTransliterator::FORWARD) {
            // The output side is the right; trim off any context
            if (post >= 0) {
                buf.remove(post);
            }
            if (ante >= 0) {
                buf.removeBetween(0, ante);
            }
            data->ruleSet.addRule(new TransliterationRule(
                                     left, leftAnte, leftPost,
                                     buf, cursor, status), status);
        } else {
            // The output side is the left; trim off any context
            if (leftPost >= 0) {
                left.remove(leftPost);
            }
            if (leftAnte >= 0) {
                left.removeBetween(0, leftAnte);
            }
            data->ruleSet.addRule(new TransliterationRule(
                                     buf, ante, post,
                                     left, leftCursor, status), status);
        }
        break;
    }

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
int32_t TransliterationRuleParser::syntaxError(int32_t parseErrorCode,
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
        rule.extractBetween(start, end, parseError->context); // Current rule
    }
    status = U_ILLEGAL_ARGUMENT_ERROR;
    return start;
}

/**
 * Allocate a private-use substitution character for the given set,
 * register it in the setVariables hash, and return the substitution
 * character.
 */
UChar TransliterationRuleParser::registerSet(UnicodeSet* adoptedSet) {
    if (variableNext >= variableLimit) {
        // throw new RuntimeException("Private use variables exhausted");
        status = U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }
    setVariablesVector.addElement(adoptedSet);
    return variableNext++;
}

/**
 * Determines what part of the private use region of Unicode we can use for
 * variable stand-ins.  The correct way to do this is as follows: Parse each
 * rule, and for forward and reverse rules, take the FROM expression, and
 * make a hash of all characters used.  The TO expression should be ignored.
 * When done, everything not in the hash is available for use.  In practice,
 * this method may employ some other algorithm for improved speed.
 */
void TransliterationRuleParser::determineVariableRange(void) {
    UnicodeRange privateUse(0xE000, 0x1900); // Private use area

    UnicodeRange* r = privateUse.largestUnusedSubrange(rules);

    data->setVariablesBase = variableNext = variableLimit = (UChar) 0;
    
    if (r != 0) {
        data->setVariablesBase = variableNext = r->start;
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
int32_t TransliterationRuleParser::quotedIndexOf(const UnicodeString& text,
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
