/*
* Copyright © {1999}, International Business Machines Corporation and others. All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   11/17/99    aliu        Creation.
**********************************************************************
*/
#ifndef RBT_PARS_H
#define RBT_PARS_H

#include "rbt.h"

class TransliterationRuleData;

class TransliterationRuleParser {

    /**
     * This is a reference to external data we don't own.  This works because
     * we only hold this for the duration of the call to parse().
     */
    const UnicodeString& rules;

    RuleBasedTransliterator::Direction direction;

    TransliterationRuleData* data;

    /**
     * We use a single error code during parsing.  Rather than pass it
     * through each API, we keep it here.
     */
    UErrorCode status;

    /**
     * The next available stand-in for variables.  This starts at some point in
     * the private use area (discovered dynamically) and increments up toward
     * <code>variableLimit</code>.  At any point during parsing, available
     * variables are <code>variableNext..variableLimit-1</code>.
     */
    UChar variableNext;

    /**
     * The last available stand-in for variables.  This is discovered
     * dynamically.  At any point during parsing, available variables are
     * <code>variableNext..variableLimit-1</code>.
     */
    UChar variableLimit;

    // Operators
    static const UChar VARIABLE_DEF_OP;
    static const UChar FORWARD_RULE_OP;
    static const UChar REVERSE_RULE_OP;
    static const char* OPERATORS;


    // Other special characters
    static const UChar QUOTE;
    static const UChar VARIABLE_REF_OPEN;
    static const UChar VARIABLE_REF_CLOSE;
    static const UChar CONTEXT_OPEN;
    static const UChar CONTEXT_CLOSE;
    static const UChar CURSOR_POS;
    static const UChar RULE_COMMENT_CHAR;


    /**
     * Specials must be quoted in rules to be used as literals.
     * Specials may not occur in variable names.
     */
    static const char* SPECIALS;

    /**
     * Specials that must be quoted in variable definitions.
     */
    static const char* DEF_SPECIALS;

public:

    static TransliterationRuleData*
        parse(const UnicodeString& rules,
              RuleBasedTransliterator::Direction direction);
    
private:

    /**
     * @param rules list of rules, separated by newline characters
     * @exception IllegalArgumentException if there is a syntax error in the
     * rules
     */
    TransliterationRuleParser(const UnicodeString& rules,
                              RuleBasedTransliterator::Direction direction);

    /**
     * Parse the given string as a sequence of rules, separated by newline
     * characters ('\n'), and cause this object to implement those rules.  Any
     * previous rules are discarded.  Typically this method is called exactly
     * once, during construction.
     * @exception IllegalArgumentException if there is a syntax error in the
     * rules
     */
    void parseRules(void);

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
    void applyRule(int32_t start, int32_t limit);

    /**
     * Add a variable definition.
     * @param name the name of the variable.  It must not already be defined.
     * @param pattern the value of the variable.  It may be a single character
     * or a pattern describing a character set.
     * @exception IllegalArgumentException if there is a syntax error
     */
    void applyVariableDef(const UnicodeString& name,
                          const UnicodeString& pattern);

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
     * offset is stored in <code>cursorPos[0]</code>
     * @return The operator character, one of the characters in OPERATORS.
     */
    UChar parseRule(int32_t start, int32_t limit,
                    UnicodeString& left, UnicodeString& right,
                    UnicodeString& anteContext,
                    UnicodeString& postContext,
                    int32_t& cursorPos);

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
    void parseMatchPattern(int32_t start, int32_t limit,
                           UnicodeString& text,
                           UnicodeString& anteContext,
                           UnicodeString& postContext);

    void parseSubPattern(int32_t start, int32_t limit,
                         UnicodeString& text);
    
    /**
     * Parse a variable definition sub pattern.  This kind of sub
     * pattern differs in the set of characters that are considered
     * special.  In particular, the '[' and ']' characters are not
     * special, since these are used in UnicodeSet patterns.
     */
    void parseDefPattern(int32_t start, int32_t limit,
                         UnicodeString& text);
    
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
     * @param cursorPos if this parameter is not null, then cursorPos[0]
     * will be set to the cursor position, or -1 if there is none.  If this
     * parameter is null, then cursors will be disallowed.
     */
    void parseOutputPattern(int32_t start, int32_t limit,
                            UnicodeString& text,
                            int32_t& cursorPos);

    /**
     * Parses a sub-pattern of a rule.  Return the text and the position of the cursor,
     * if any.  Resolves all quotes and variables.
     * @param rules the string to be parsed
     * @param start the beginning index, inclusive; <code>0 <= start
     * <= limit</code>.
     * @param limit the ending index, exclusive; <code>start <= limit
     * <= rules.length()</code>.
     * @param text the output text will be appended to this buffer
     * @param cursorPos if this parameter is not null, then cursorPos[0]
     * will be set to the cursor position, or -1 if there is none.  If this
     * parameter is null, then cursors will be disallowed.
     * @param specials characters that must be quoted; typically either
     * SPECIALS or DEF_SPECIALS.
     */
    void parseSubPattern(int32_t start, int32_t limit,
                         UnicodeString& text,
                         int32_t* cursorPos,
                         const UnicodeString& specials);

    void validateVariableName(const UnicodeString& name);

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
    //$ Character getVariableDef(const UnicodeString& name);

    /**
     * Determines what part of the private use region of Unicode we can use for
     * variable stand-ins.  The correct way to do this is as follows: Parse each
     * rule, and for forward and reverse rules, take the FROM expression, and
     * make a hash of all characters used.  The TO expression should be ignored.
     * When done, everything not in the hash is available for use.  In practice,
     * this method may employ some other algorithm for improved speed.
     */
    void determineVariableRange(void);

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
    static int32_t quotedIndexOf(const UnicodeString& text,
                                 int32_t start, int32_t limit,
                                 const UnicodeString& setOfChars);

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
    static int32_t indexOf(const UnicodeString& text,
                           int32_t start, int32_t limit,
                           const UnicodeString& setOfChars);
    
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
    static int32_t indexOf(const UnicodeString& text,
                           const UnicodeString& setOfChars);
    
};

#endif
