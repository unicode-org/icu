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
#include "rbt.h"
#include "rbt_rule.h"
#include "unirange.h"
#include "rbt_data.h"
#include "uniset.h"

// Operators
const UChar TransliterationRuleParser::VARIABLE_DEF_OP = '=';
const UChar TransliterationRuleParser::FORWARD_RULE_OP = '>';
const UChar TransliterationRuleParser::REVERSE_RULE_OP = '<';
const char* TransliterationRuleParser::OPERATORS = "=><";

// Other special characters
const UChar TransliterationRuleParser::QUOTE = '\'';
const UChar TransliterationRuleParser::VARIABLE_REF_OPEN = '{';
const UChar TransliterationRuleParser::VARIABLE_REF_CLOSE = '}';
const UChar TransliterationRuleParser::CONTEXT_OPEN = '[';
const UChar TransliterationRuleParser::CONTEXT_CLOSE = ']';
const UChar TransliterationRuleParser::CURSOR_POS = '|';
const UChar TransliterationRuleParser::RULE_COMMENT_CHAR = '#';


/**
 * Specials must be quoted in rules to be used as literals.
 * Specials may not occur in variable names.
 *
 * This string is a superset of OPERATORS.
 */
const char* TransliterationRuleParser::SPECIALS = "'{}[]|#=><";

/**
 * Specials that must be quoted in variable definitions.
 */
const char* TransliterationRuleParser::DEF_SPECIALS = "'{}";

TransliterationRuleData*
TransliterationRuleParser::parse(const UnicodeString& rules,
                                 RuleBasedTransliterator::Direction direction) {
    TransliterationRuleParser parser(rules, direction);
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
                                     RuleBasedTransliterator::Direction theDirection) :
    rules(theRules), direction(theDirection), data(0) {}

/**
 * Parse the given string as a sequence of rules, separated by newline
 * characters ('\n'), and cause this object to implement those rules.  Any
 * previous rules are discarded.  Typically this method is called exactly
 * once, during construction.
 * @exception IllegalArgumentException if there is a syntax error in the
 * rules
 */
void TransliterationRuleParser::parseRules() {
    status = U_ZERO_ERROR;

    delete data;
    data = new TransliterationRuleData(status);
    if (U_FAILURE(status)) {
        return;
    }
    
    determineVariableRange();

    int32_t n = rules.length();
    int32_t i = 0;
    while (i<n && U_SUCCESS(status)) {
        int32_t limit = rules.indexOf('\n', i);

        // Recognize "\\\n" as an escaped "\n"
        while (limit>0 && rules.charAt(limit-1) == '\\') {
            limit = rules.indexOf('\n', limit+1);
        }

        if (limit == -1) {
            limit = n;
        }
        // Skip over empty lines and line starting with #
        if (limit > i && rules.charAt(i) != RULE_COMMENT_CHAR) {
            applyRule(i, limit);
        }
        i = limit + 1;
    }

    data->ruleSet.freeze();
}

/**
 * Parse the given substring as a rule, and append it to the rules currently
 * represented in this object.
 * @param start the beginning index, inclusive; <code>0 <= start
 * <= limit</code>.
 * @param limit the ending index, exclusive; <code>start <= limit
 * <= rules.length()</code>.
 * @exception IllegalArgumentException if there is a syntax error in the
 * rules
 */
void TransliterationRuleParser::applyRule(int32_t start, int32_t limit) {
    /* General description of parsing: Initially, rules contain two types of
     * quoted characters.  First, there are variable references, such as
     * "{alpha}".  Second, there are quotes, such as "'<'" or "''".  One of
     * the first steps in parsing a rule is to resolve such quoted matter.
     * Quotes are removed early, leaving unquoted literal matter.  Variable
     * references are resolved and replaced by single characters.  In some
     * instances these characters represent themselves; in others, they
     * stand for categories of characters.  Character categories are either
     * predefined (e.g., "{Lu}"), or are defined by the user using a
     * statement (e.g., "vowels:aeiouAEIOU").
     *
     * Another early step in parsing is to split each rule into component
     * pieces.  These pieces are, for every rule, a left-hand side, a right-
     * hand side, and an operator.  The left- and right-hand sides may not
     * be empty, except for the output patterns of forward and reverse
     * rules.  In addition to this partitioning, the match patterns of
     * forward and reverse rules must be partitioned into antecontext,
     * postcontext, and literal pattern, where the context portions may or
     * may not be present.  Finally, output patterns must have the cursor
     * indicator '|' detected and removed, with its position recorded.
     *
     * Quote removal, variable resolution, and sub-pattern splitting must
     * all happen at once.  This is due chiefly to the quoting mechanism,
     * which allows special characters to appear at arbitrary positions in
     * the final unquoted text.  (For this reason, alteration of the rule
     * language is somewhat clumsy; it entails reassessment and revision of
     * the parsing methods as a whole.)
     *
     * After this processing of rules is complete, the final end products
     * are unquoted pieces of text of various types, and an integer cursor
     * position, if one is specified.  These processed raw materials are now
     * easy to deal with; other classes such as UnicodeSet and
     * TransliterationRule need know nothing of quoting or variables.
     */
    UnicodeString left;
    UnicodeString right;
    UnicodeString anteContext;
    UnicodeString postContext;
    int32_t cursorPos;

    UChar op = parseRule(start, limit, left, right,
                         anteContext, postContext, cursorPos);

    if (U_FAILURE(status)) {
        return;
    }

    switch (op) {
    case VARIABLE_DEF_OP:
        applyVariableDef(left, right);
        break;
    case FORWARD_RULE_OP:
        if (direction == RuleBasedTransliterator::FORWARD) {
            data->ruleSet.addRule(new TransliterationRule(
                                     left, right,
                                     anteContext, postContext,
                                     cursorPos, status),
                                  status);
        } // otherwise ignore the rule; it's not the direction we want
        break;
    case REVERSE_RULE_OP:
        if (direction == RuleBasedTransliterator::REVERSE) {
            data->ruleSet.addRule(new TransliterationRule(
                                     right, left,
                                     anteContext, postContext,
                                     cursorPos, status),
                                  status);
        } // otherwise ignore the rule; it's not the direction we want
        break;
    }
}

/**
 * Add a variable definition.
 * @param name the name of the variable.  It must not already be defined.
 * @param pattern the value of the variable.  It may be a single character
 * or a pattern describing a character set.
 * @exception IllegalArgumentException if there is a syntax error
 */
void TransliterationRuleParser::applyVariableDef(const UnicodeString& name,
                                                 const UnicodeString& pattern) {
    validateVariableName(name);

    if (U_FAILURE(status)) {
        return;
    }

    if (data->isVariableDefined(name)) {
        // throw new IllegalArgumentException("Duplicate variable definition: "
        //                                   + name + '=' + pattern);
        status = U_ILLEGAL_ARGUMENT_ERROR; 
        return;
    }
//!         if (UnicodeSet.getCategoryID(name) >= 0) {
//!             throw new IllegalArgumentException("Reserved variable name: "
//!                                                + name);
//!         }
    if (pattern.length() < 1) {
        // throw new IllegalArgumentException("Variable definition missing: "
        //                                   + name);
        status = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    if (pattern.length() == 1) {
        // Got a single character variable definition
        //$ data->variableNames.put(name, new Character(pattern.charAt(0)));
        data->defineVariable(name, pattern.charAt(0), status);
    } else {
        // Got more than one character; parse it as a category
        if (variableNext >= variableLimit) {
            //$ throw new RuntimeException("Private use variables exhausted");
            status = U_ILLEGAL_ARGUMENT_ERROR;
            return;
        }
        //$ Character c = new Character(variableNext++);
        //$ data->variableNames.put(name, c);
        //$ data->setVariables.put(c, new UnicodeSet(pattern));
        data->defineVariable(name, variableNext++,
                             new UnicodeSet(pattern, status),
                             status);
    }
}

/**
 * Given a rule, parses it into three pieces: The left side, the right side,
 * and the operator.  Returns the operator.  Quotes and variable references
 * are resolved; the otuput text in all <code>StringBuffer</code> parameters
 * is literal text.  This method delegates to other parsing methods to
 * handle the match pattern, output pattern, and other sub-patterns in the
 * rule.
 * @param start the beginning index, inclusive; <code>0 <= start
 * <= limit</code>.
 * @param limit the ending index, exclusive; <code>start <= limit
 * <= rules.length()</code>.
 * @param left left side of rule is appended to this buffer
 * with the quotes removed and variables resolved
 * @param right right side of rule is appended to this buffer
 * with the quotes removed and variables resolved
 * @param anteContext the preceding context of the match pattern,
 * if there is one, is appended to this buffer
 * @param postContext the following context of the match pattern,
 * if there is one, is appended to this buffer
 * @param cursorPos if there is a cursor in the output pattern, its
 * offset is stored in <code>cursorPos</code>
 * @return The operator character, one of the characters in OPERATORS.
 */
UChar TransliterationRuleParser::parseRule(int32_t start, int32_t limit,
                                           UnicodeString& left,
                                           UnicodeString& right,
                                           UnicodeString& anteContext,
                                           UnicodeString& postContext,
                                           int32_t& cursorPos) {
    /* Parse the rule into three pieces -- left, operator, and right,
     * parsing out quotes.  The result is that left and right will have
     * unquoted text.  E.g., "gt<'>'" will have right = ">".  Unquoted
     * operators throw an exception.  Two quotes inside or outside
     * quotes indicates a quote literal.  E.g., "o''clock" -> "o'clock".
     */
    int32_t i = quotedIndexOf(rules, start, limit, OPERATORS);
    if (i < 0) {
        //$ throw new IllegalArgumentException(
        //$              "Syntax error: "
        //$              + rules.substring(start, limit));
        status = U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }
    UChar c = rules.charAt(i);
    switch (c) {
    case FORWARD_RULE_OP:
        if (i == start) {
            //$ throw new IllegalArgumentException(
            //$               "Empty left side: "
            //$               + rules.substring(start, limit));
            status = U_ILLEGAL_ARGUMENT_ERROR;
            return 0;
        }
        parseMatchPattern(start, i, left, anteContext, postContext);
        if (i != (limit-1)) {
            parseOutputPattern(i+1, limit, right, cursorPos);
        }
        break;
    case REVERSE_RULE_OP:
        if (i == (limit-1)) {
            //$ throw new IllegalArgumentException(
            //$               "Empty right side: "
            //$               + rules.substring(start, limit));
            status = U_ILLEGAL_ARGUMENT_ERROR;
            return 0;
        }
        if (i != start) {
            parseOutputPattern(start, i, left, cursorPos);
        }
        parseMatchPattern(i+1, limit, right, anteContext, postContext);
        break;
    default:
        if (i == start || i == (limit-1)) {
            //$ throw new IllegalArgumentException(
            //$               "Empty left or right side: "
            //$               + rules.substring(start, limit));
            status = U_ILLEGAL_ARGUMENT_ERROR;
            return 0;
        }
        parseSubPattern(start, i, left);
        parseDefPattern(i+1, limit, right);
        break;
    }
    return c;
}

/**
 * Parses the match pattern of a forward or reverse rule.  Given the raw
 * match pattern, return the match text and the context on both sides, if
 * any.  Resolves all quotes and variables.
 * @param start the beginning index, inclusive; <code>0 <= start
 * <= limit</code>.
 * @param limit the ending index, exclusive; <code>start <= limit
 * <= rules.length()</code>.
 * @param text the key to be matched will be appended to this buffer
 * @param anteContext the preceding context, if any, will be appended
 * to this buffer.
 * @param postContext the following context, if any, will be appended
 * to this buffer.
 */
void TransliterationRuleParser::parseMatchPattern(int32_t start, int32_t limit,
                                                  UnicodeString& text,
                                                  UnicodeString& anteContext,
                                                  UnicodeString& postContext) {
    if (start >= limit) {
        //$ throw new IllegalArgumentException(
        //$               "Empty expression in rule: "
        //$               + rules.substring(start, limit));
        status = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }
    //$ if (anteContext != 0) {
        // Ignore optional opening and closing context characters
        if (rules.charAt(start) == CONTEXT_OPEN) {
            ++start;
        }
        if (rules.charAt(limit-1) == CONTEXT_CLOSE) {
            --limit;
        }
        // The four possibilities are:
        //             key
        // anteContext]key
        // anteContext]key[postContext
        //             key[postContext
        int32_t ante = quotedIndexOf(rules, start, limit, CONTEXT_CLOSE);
        int32_t post = quotedIndexOf(rules, start, limit, CONTEXT_OPEN);
        if (ante >= 0 && post >= 0 && ante > post) {
            //$ throw new IllegalArgumentException(
            //$               "Syntax error in context specifier: "
            //$               + rules.substring(start, limit));
            status = U_ILLEGAL_ARGUMENT_ERROR;
            return;
        }
        if (ante >= 0) {
            parseSubPattern(start, ante, anteContext);
            start = ante+1;
        }
        if (post >= 0) {
            parseSubPattern(post+1, limit, postContext);
            limit = post;
        }
    //$ }
    parseSubPattern(start, limit, text);
}

void TransliterationRuleParser::parseSubPattern(int32_t start, int32_t limit,
                                                UnicodeString& text) {
    parseSubPattern(start, limit, text, 0, SPECIALS);
}

/**
 * Parse a variable definition sub pattern.  This kind of sub
 * pattern differs in the set of characters that are considered
 * special.  In particular, the '[' and ']' characters are not
 * special, since these are used in UnicodeSet patterns.
 */
void TransliterationRuleParser::parseDefPattern(int32_t start, int32_t limit,
                                                UnicodeString& text) {
    parseSubPattern(start, limit, text, 0, DEF_SPECIALS);
}

/**
 * Parses the output pattern of a forward or reverse rule.  Given the
 * output pattern, return the output text and the position of the cursor,
 * if any.  Resolves all quotes and variables.
 * @param rules the string to be parsed
 * @param start the beginning index, inclusive; <code>0 <= start
 * <= limit</code>.
 * @param limit the ending index, exclusive; <code>start <= limit
 * <= rules.length()</code>.
 * @param text the output text will be appended to this buffer
 * @param cursorPos if this parameter is not null, then cursorPos
 * will be set to the cursor position, or -1 if there is none.  If this
 * parameter is null, then cursors will be disallowed.
 */
void TransliterationRuleParser::parseOutputPattern(int32_t start, int32_t limit,
                                                   UnicodeString& text,
                                                   int32_t& cursorPos) {
    parseSubPattern(start, limit, text, &cursorPos, SPECIALS);
}

/**
 * Parses a sub-pattern of a rule.  Return the text and the position of the cursor,
 * if any.  Resolves all quotes and variables.
 * @param rules the string to be parsed
 * @param start the beginning index, inclusive; <code>0 <= start
 * <= limit</code>.
 * @param limit the ending index, exclusive; <code>start <= limit
 * <= rules.length()</code>.
 * @param text the output text will be appended to this buffer
 * @param cursorPos if this parameter is not null, then cursorPos
 * will be set to the cursor position, or -1 if there is none.  If this
 * parameter is null, then cursors will be disallowed.
 * @param specials characters that must be quoted; typically either
 * SPECIALS or DEF_SPECIALS.
 */
void TransliterationRuleParser::parseSubPattern(int32_t start, int32_t limit,
                                                UnicodeString& text,
                                                int32_t* cursorPos,
                                                const UnicodeString& specials) {
    bool_t inQuote = FALSE;

    if (start >= limit) {
        //$ throw new IllegalArgumentException("Empty expression in rule");
        status = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }
    if (cursorPos != 0) {
        *cursorPos = -1;
    }
    for (int32_t i=start; i<limit; ++i) {
        UChar c = rules.charAt(i);
        if (c == QUOTE) {
            // Check for double quote
            if ((i+1) < limit
                && rules.charAt(i+1) == QUOTE) {
                text.append(QUOTE);
                ++i; // Skip over both quotes
            } else {
                inQuote = !inQuote;
            }
        } else if (inQuote) {
            text.append(c);
        } else if (c == VARIABLE_REF_OPEN) {
            ++i;
            int32_t j = rules.indexOf(VARIABLE_REF_CLOSE, i);
            if (i == j || j < 0) { // empty or unterminated
                //$ throw new IllegalArgumentException("Illegal variable reference: "
                //$                                    + rules.substring(start, limit));
                status = U_ILLEGAL_ARGUMENT_ERROR;
                return;
            }
            UnicodeString name;
            rules.extractBetween(i, j, name);
            validateVariableName(name);
            if (U_FAILURE(status)) {
                return;
            }
            UChar ch = data->lookupVariable(name, status);
            if (U_FAILURE(status)) {
                return;
            }
            text.append(ch);
            i = j;
        } else if (c == CURSOR_POS && cursorPos != 0) {
            if (*cursorPos >= 0) {
                //$ throw new IllegalArgumentException("Multiple cursors: "
                //$                                    + rules.substring(start, limit));
                status = U_ILLEGAL_ARGUMENT_ERROR;
                return;
            }
            *cursorPos = text.length();
        } else if (specials.indexOf(c) >= 0) {
            //$ throw new IllegalArgumentException("Unquoted special character: "
            //$                                    + rules.substring(start, limit));
            status = U_ILLEGAL_ARGUMENT_ERROR;
            return;
        } else {
            text.append(c);
        }
    }
}

void TransliterationRuleParser::validateVariableName(const UnicodeString& name) {
    if (indexOf(name, SPECIALS) >= 0) {
        //throw new IllegalArgumentException(
        //              "Special character in variable name: "
        //              + name);
        status = U_ILLEGAL_ARGUMENT_ERROR;
    }
}

/**
 * Returns the single character value of the given variable name.  Defined
 * names are recognized.
 *
 * NO LONGER SUPPORTED:
 * If a Unicode category name is given, a standard character variable
 * in the range firstCategoryVariable to lastCategoryVariable is returned,
 * with value firstCategoryVariable + n, where n is the category
 * number.
 * @exception IllegalArgumentException if the name is unknown.
 */
//$ UChar TransliterationRuleParser::getVariableDef(const UnicodeString& name) {
//$     UChar ch = data->lookupVariable(name, status);
//$ //!         if (ch == null) {
//$ //!             int id = UnicodeSet.getCategoryID(name);
//$ //!             if (id >= 0) {
//$ //!                 ch = new Character((char) (firstCategoryVariable + id));
//$ //!                 data->variableNames.put(name, ch);
//$ //!                 data->setVariables.put(ch, new UnicodeSet(id));
//$ //!             }
//$ //!         }
//$     if (ch == 0) {
//$         throw new IllegalArgumentException("Undefined variable: "
//$                                            + name);
//$     }
//$     return ch;
//$ }

/**
 * Determines what part of the private use region of Unicode we can use for
 * variable stand-ins.  The correct way to do this is as follows: Parse each
 * rule, and for forward and reverse rules, take the FROM expression, and
 * make a hash of all characters used.  The TO expression should be ignored.
 * When done, everything not in the hash is available for use.  In practice,
 * this method may employ some other algorithm for improved speed.
 */
void TransliterationRuleParser::determineVariableRange() {
    UnicodeRange privateUse(0xE000, 0x1900); // Private use area

    UnicodeRange* r = privateUse.largestUnusedSubrange(rules);

    variableNext = variableLimit = (UChar) 0;
    
    if (r != 0) {
        variableNext = r->start;
        variableLimit = (UChar) (r->start + r->length);
        delete r;
    }

    if (variableNext >= variableLimit) {
        status = U_ILLEGAL_ARGUMENT_ERROR;
    }
}

/**
 * Returns the index of the first character in a set, ignoring quoted text.
 * For example, in the string "abc'hide'h", the 'h' in "hide" will not be
 * found by a search for "h".  Unlike String.indexOf(), this method searches
 * not for a single character, but for any character of the string
 * <code>setOfChars</code>.
 * @param text text to be searched
 * @param start the beginning index, inclusive; <code>0 <= start
 * <= limit</code>.
 * @param limit the ending index, exclusive; <code>start <= limit
 * <= text.length()</code>.
 * @param setOfChars string with one or more distinct characters
 * @return Offset of the first character in <code>setOfChars</code>
 * found, or -1 if not found.
 * @see #indexOf
 */
int32_t TransliterationRuleParser::quotedIndexOf(const UnicodeString& text,
                                                 int32_t start, int32_t limit,
                                                 const UnicodeString& setOfChars) {
    for (int32_t i=start; i<limit; ++i) {
        UChar c = text.charAt(i);
        if (c == QUOTE) {
            while (++i < limit
                   && text.charAt(i) != QUOTE) {}
        } else if (setOfChars.indexOf(c) >= 0) {
            return i;
        }
    }
    return -1;
}

/**
 * Returns the index of the first character in a set.  Unlike
 * String.indexOf(), this method searches not for a single character, but
 * for any character of the string <code>setOfChars</code>.
 * @param text text to be searched
 * @param start the beginning index, inclusive; <code>0 <= start
 * <= limit</code>.
 * @param limit the ending index, exclusive; <code>start <= limit
 * <= text.length()</code>.
 * @param setOfChars string with one or more distinct characters
 * @return Offset of the first character in <code>setOfChars</code>
 * found, or -1 if not found.
 * @see #quotedIndexOf
 */
int32_t TransliterationRuleParser::indexOf(const UnicodeString& text,
                                           int32_t start, int32_t limit,
                                           const UnicodeString& setOfChars) {
    for (int32_t i=start; i<limit; ++i) {
        if (setOfChars.indexOf(text.charAt(i)) >= 0) {
            return i;
        }
    }
    return -1;
}

/**
 * Returns the index of the first character in a set.  Unlike
 * String.indexOf(), this method searches not for a single character, but
 * for any character of the string <code>setOfChars</code>.
 * @param text text to be searched
 * @param setOfChars string with one or more distinct characters
 * @return Offset of the first character in <code>setOfChars</code>
 * found, or -1 if not found.
 * @see #quotedIndexOf
 */
int32_t TransliterationRuleParser::indexOf(const UnicodeString& text,
                                           const UnicodeString& setOfChars) {
    return indexOf(text, 0, text.length(), setOfChars);
}
