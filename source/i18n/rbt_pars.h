/*
* Copyright (C) {1999}, International Business Machines Corporation and others. All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   11/17/99    aliu        Creation.
**********************************************************************
*/
#ifndef RBT_PARS_H
#define RBT_PARS_H

#include "unicode/rbt.h"
#include "unicode/parseerr.h"

U_NAMESPACE_BEGIN

class TransliterationRuleData;
class UnicodeMatcher;
class ParseData;
class RuleHalf;
class ParsePosition;
class UVector;

class TransliteratorParser {

 public:

    /**
     * PUBLIC data member containing the parsed data object, or null if
     * there were no rules.
     */
    TransliterationRuleData* data;

    /**
     * PUBLIC data member.
     * The block of ::IDs, both at the top and at the bottom.
     * Inserted into these may be additional rules at the
     * idSplitPoint.
     */
    UnicodeString idBlock;

    /**
     * PUBLIC data member.
     * In a compound RBT, the index at which the RBT rules are
     * inserted into the ID block.  Index 0 means before any IDs
     * in the block.  Index idBlock.length() means after all IDs
     * in the block.  Index is a string index.
     */
    int32_t idSplitPoint;

    /**
     * PUBLIC data member containing the parsed compound filter, if any.
     */
    UnicodeSet* compoundFilter;

 private:

    // The number of rules parsed.  This tells us if there were
    // any actual transliterator rules, or if there were just ::ID
    // block IDs.
    int32_t ruleCount;

    UTransDirection direction;

    /**
     * We use a single error code during parsing.  Rather than pass it
     * through each API, we keep it here.
     */
    UErrorCode status;

    /**
     * Parse error information.
     */
    UParseError parseError;

    /**
     * Temporary symbol table used during parsing.
     */
    ParseData* parseData;

    /**
     * Temporary vector of matcher variables.  When parsing is complete, this
     * is copied into the array data.variables.  As with data.variables,
     * element 0 corresponds to character data.variablesBase.
     */
    UVector* variablesVector;

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

    /**
     * When we encounter an undefined variable, we do not immediately signal
     * an error, in case we are defining this variable, e.g., "$a = [a-z];".
     * Instead, we save the name of the undefined variable, and substitute
     * in the placeholder char variableLimit - 1, and decrement
     * variableLimit.
     */
    UnicodeString undefinedVariableName;

public:

    /**
     * Constructor.
     */
    TransliteratorParser();

    /**
     * Destructor.
     */
    ~TransliteratorParser();

    /**
     * Parse the given string as a sequence of rules, separated by newline
     * characters ('\n'), and cause this object to implement those rules.  Any
     * previous rules are discarded.  Typically this method is called exactly
     * once after construction.
     *
     * Parse the given rules, in the given direction.  After this call
     * returns, query the public data members for results.  The caller
     * owns the 'data' and 'compoundFilter' data members after this
     * call returns.
     */
    void parse(const UnicodeString& rules,
               UTransDirection direction,
               UParseError& pe,
               UErrorCode& ec);

    /**
     * Return the compound filter parsed by parse().  Caller owns result.
     */ 
    UnicodeSet* orphanCompoundFilter();

    /**
     * Return the data object parsed by parse().  Caller owns result.
     */
    TransliterationRuleData* orphanData();

private:

    void parseRules(const UnicodeString& rules,
                    UTransDirection direction);

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
    int32_t parseRule(const UnicodeString& rule, int32_t pos, int32_t limit);

    /**
     * Called by main parser upon syntax error.  Search the rule string
     * for the probable end of the rule.  Of course, if the error is that
     * the end of rule marker is missing, then the rule end will not be found.
     * In any case the rule start will be correctly reported.
     * @param msg error description
     * @param rule pattern string
     * @param start position of first character of current rule
     */
    int32_t syntaxError(UErrorCode parseErrorCode, const UnicodeString&, int32_t start);

    /**
     * Parse a UnicodeSet out, store it, and return the stand-in character
     * used to represent it.
     */
    UChar parseSet(const UnicodeString& rule,
                   ParsePosition& pos);

    /**
     * Generate and return a stand-in for a new UnicodeMatcher.  Store
     * the matcher (adopt it).
     */
    UChar generateStandInFor(UnicodeMatcher* adopted);

    /**
     * Append the value of the given variable name to the given
     * UnicodeString.
     */
    void appendVariableDef(const UnicodeString& name,
                           UnicodeString& buf);

    /**
     * Return a stand-in character that refers to the given segments.
     * @param r a reference number >= 1
     * @return a stand-in for the given segment reference
     */
    UChar getSegmentStandin(int32_t r);

    /**
     * Determines what part of the private use region of Unicode we can use for
     * variable stand-ins.  The correct way to do this is as follows: Parse each
     * rule, and for forward and reverse rules, take the FROM expression, and
     * make a hash of all characters used.  The TO expression should be ignored.
     * When done, everything not in the hash is available for use.  In practice,
     * this method may employ some other algorithm for improved speed.
     */
    void determineVariableRange(const UnicodeString&);

    /**
     * Returns the index of a character, ignoring quoted text.
     * For example, in the string "abc'hide'h", the 'h' in "hide" will not be
     * found by a search for 'h'.
     * @param text text to be searched
     * @param start the beginning index, inclusive; <code>0 <= start
     * <= limit</code>.
     * @param limit the ending index, exclusive; <code>start <= limit
     * <= text.length()</code>.
     * @param c character to search for
     * @return Offset of the first instance of c, or -1 if not found.
     */
    static int32_t quotedIndexOf(const UnicodeString& text,
                                 int32_t start, int32_t limit,
                                 UChar c);

    friend class RuleHalf;

    // Disallowed methods; no impl.
    TransliteratorParser(const TransliteratorParser&);
    TransliteratorParser& operator=(const TransliteratorParser&);
};

U_NAMESPACE_END

#endif
