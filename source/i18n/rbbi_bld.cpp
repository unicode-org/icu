/*
**********************************************************************
*   Copyright (C) 1999 International Business Machines Corporation   *
*   and others. All rights reserved.                                 *
**********************************************************************
*   Date        Name        Description
*   12/9/99     rgillam     Ported from Java
**********************************************************************
*/

#include "unicode/rbbi.h"
#include "rbbi_bld.h"
#include "cmemory.h"
#include "unicode/unicode.h"

//=======================================================================
// RuleBasedBreakIterator.Builder
//=======================================================================
/**
 * The Builder class has the job of constructing a RuleBasedBreakIterator from a
 * textual description.  A Builder is constructed by RuleBasedBreakIterator's
 * constructor, which uses it to construct the iterator itself and then throws it
 * away.
 * <p>The construction logic is separated out into its own class for two primary
 * reasons:
 * <ul><li>The construction logic is quite complicated and large.  Separating it
 * out into its own class means the code must only be loaded into memory while a
 * RuleBasedBreakIterator is being constructed, and can be purged after that.
 * <li>There is a fair amount of state that must be maintained throughout the
 * construction process that is not needed by the iterator after construction.
 * Separating this state out into another class prevents all of the functions that
 * construct the iterator from having to have really long parameter lists,
 * (hopefully) contributing to readability and maintainability.</ul>
 * <p>It'd be really nice if this could be an independent class rather than an
 * inner class, because that would shorten the source file considerably, but
 * making Builder an inner class of RuleBasedBreakIterator allows it direct access
 * to RuleBasedBreakIterator's private members, which saves us from having to
 * provide some kind of "back door" to the Builder class that could then also be
 * used by other classes.
 */

const int32_t
RuleBasedBreakIteratorBuilder::END_STATE_FLAG = 0x8000;

const int32_t
RuleBasedBreakIteratorBuilder::DONT_LOOP_FLAG = 0x4000;

const int32_t
RuleBasedBreakIteratorBuilder::LOOKAHEAD_STATE_FLAG = 0x2000;

const int32_t
RuleBasedBreakIteratorBuilder::ALL_FLAGS = END_STATE_FLAG
        | DONT_LOOP_FLAG | LOOKAHEAD_STATE_FLAG;
 
// constants for various characters
const UChar NULL_CHAR = 0x0000;
const UChar OPEN_PAREN = 0x28;
const UChar CLOSE_PAREN = 0x29;
const UChar OPEN_BRACKET = 0x5b;
const UChar CLOSE_BRACKET = 0x5d;
const UChar OPEN_BRACE = 0x7b;
const UChar CLOSE_BRACE = 0x7d;
const UChar SEMICOLON = 0x3b;
const UChar EQUAL_SIGN = 0x3d;
const UChar MINUS = 0x2d;
const UChar CARET = 0x5e;
const UChar AMPERSAND = 0x26;
const UChar COLON = 0x3a;
const UChar ASTERISK = 0x2a;
const UChar PLUS = 0x2b;
const UChar QUESTION = 0x3f;
const UChar PERIOD = 0x2e;
const UChar PIPE = 0x7c;
const UChar BANG = 0x21;
const UChar SLASH = 0x2f;
const UChar BACKSLASH = 0x5c;

const UChar ASCII_LOW = 0x20;
const UChar ASCII_HI = 0x7f;

const UnicodeString IGNORE_NAME = UnicodeString("$ignore");
        
//============================================================================

/**
 * This class is a completely non-general quick-and-dirty class to make up
 * for the fact that at the time of this writing (12/20/99) there was no
 * general hash table class in the ICU.  When one is created, this class should
 * be removed and the code that depends on this class should be altered to use
 * the regular hash-table class.  This class is just here as a temporary measure
 * until that happens.                          --rtg 12/20/99
 */
class ExpressionList {
private:
    UVector keys;
    UVector sets;
    UVector strings;

public:
    static const UnicodeSet setNotThere; // an empty UnicodeSet we can use as a return value
                                         // in get() when the key isn't found
    static const UnicodeString stringNotThere;
    ExpressionList();
    ~ExpressionList();

    const UnicodeSet& getSet(const UnicodeString& key) const;
    void putSet(const UnicodeString& key, UnicodeSet* valueToAdopt);

    const UnicodeString& getString(const UnicodeString& key) const;
    void putString(const UnicodeString& key, UnicodeString* valueToAdopt);

    const UnicodeString& getKeyAt(int32_t x) const { return *((UnicodeString*)keys[x]); }
    const UnicodeSet& operator[](int32_t x) const { return *((UnicodeSet*)sets[x]); }
    int32_t size() const { return keys.size(); }
};

const UnicodeSet
ExpressionList::setNotThere;

const UnicodeString
ExpressionList::stringNotThere;

ExpressionList::ExpressionList()
{
}

ExpressionList::~ExpressionList()
{
    for (int32_t i = 0; i < keys.size(); i++) {
        delete (UnicodeString*)keys[i];
        delete (UnicodeSet*)sets[i];
        delete (UnicodeString*)strings[i];
    }
}

const UnicodeSet&
ExpressionList::getSet(const UnicodeString& key) const
{
    for (int32_t i = 0; i < keys.size(); i++) {
        if (key == *((UnicodeString*)keys[i])) {
            return *((UnicodeSet*)sets[i]);
        }
    }
    return setNotThere;
}

void
ExpressionList::putSet(const UnicodeString& key, UnicodeSet* valueToAdopt)
{
    const UnicodeSet& theSet = getSet(key);
    if (&theSet != &setNotThere) {
        UnicodeSet* value = (UnicodeSet*)(&theSet);
        value->clear();
        value->addAll(*valueToAdopt);
        delete valueToAdopt;
    }
    else {
        keys.addElement(new UnicodeString(key));
        sets.addElement(valueToAdopt);
        strings.addElement(new UnicodeString);
    }
}

const UnicodeString&
ExpressionList::getString(const UnicodeString& key) const
{
    for (int32_t i = 0; i < keys.size(); i++) {
        if (key == *((UnicodeString*)keys[i])) {
            return *((UnicodeString*)strings[i]);
        }
    }
    return stringNotThere;
}

void
ExpressionList::putString(const UnicodeString& key, UnicodeString* valueToAdopt)
{
    const UnicodeString& theString = getString(key);
    if (&theString != &stringNotThere) {
        UnicodeString* value = (UnicodeString*)(&theString);
        *value = *valueToAdopt;
        delete valueToAdopt;
    }
    else {
        keys.addElement(new UnicodeString(key));
        sets.addElement(new UnicodeSet);
        strings.addElement(valueToAdopt);
    }
}
//============================================================================

#define error(message, position, context) \
    setUpErrorMessage(message, position, context); \
    err = U_PARSE_ERROR; \
    return

void
stringDeleter(void* o) {
    delete (UnicodeString*)o;
}

void
usetDeleter(void* o) {
    delete (UnicodeSet*)o;
}

void
tableRowDeleter(void* o) {
    delete [] (int16_t*)o;
}

void
vectorDeleter(void* o) {
    delete (UVector*)o;
}

void
mergeRowDeleter(void* o) {
    delete [] (int32_t*)o;
}

/**
 * No special construction is required for the Builder.
 */
RuleBasedBreakIteratorBuilder::RuleBasedBreakIteratorBuilder(
                                    RuleBasedBreakIterator& iteratorToBuild)
: iterator(iteratorToBuild),
  tables(new RuleBasedBreakIteratorTables)
{
    iterator.tables = tables;

    tempRuleList.setDeleter(&stringDeleter);
    categories.setDeleter(&usetDeleter);
    tempStateTable.setDeleter(&tableRowDeleter);
    decisionPointStack.setDeleter(&vectorDeleter);
    // decisionPointList, loopingStates, and statesToBackfill (as well as the
    // individual elements in decisionPointStack) don't need deleters--
    // their element type is int32_t
    mergeList.setDeleter(&mergeRowDeleter);
}

RuleBasedBreakIteratorBuilder::~RuleBasedBreakIteratorBuilder()
{
    delete expressions;
}

/**
 * This is the main function for setting up the BreakIterator's tables.  It
 * just vectors different parts of the job off to other functions.
 */
void 
RuleBasedBreakIteratorBuilder::buildBreakIterator(const UnicodeString& description,
                                                  UErrorCode& err)
{
    if (U_FAILURE(err))
        return;

    UnicodeString tempDesc(description);
    
    buildRuleList(tempDesc, err);
    buildCharCategories(err);
    buildStateTable(err);
    buildBackwardsStateTable(err);
}

/**
 * Thus function has three main purposes:
 * <ul><li>Perform general syntax checking on the description, so the rest of the
 * build code can assume that it's parsing a legal description.
 * <li>Split the description into separate rules
 * <li>Perform variable-name substitutions (so that no one else sees variable names)
 * </ul>
 */
void 
RuleBasedBreakIteratorBuilder::buildRuleList(UnicodeString& description,
                                             UErrorCode& err)
{
    if (U_FAILURE(err))
        return;

    // invariants:
    // - parentheses must be balanced: ()[]{}
    // - nothing can be nested inside {}
    // - nothing can be nested inside [] except more []s
    // - pairs of ()[]{} must not be empty
    // - ; can only occur at the outer level
    // - | can only appear inside ()
    // - only one = or / can occur in a single rule
    // - = and / cannot both occur in the same rule
    // - the right-hand side of a = expression must be enclosed in [] or ()
    // - *. ?, and + may not occur at the beginning of a rule, nor may they follow
    //   =, /, (, (, |, }, ;, +, ?, or * (except that ? can follow *)
    // - the rule list must contain at least one / rule (which may or may not 
    //   actually contain a /
    // - no rule may be empty
    // - all printing characters in the ASCII range except letters and digits
    //   are reserved and must be preceded by \
    // - ! may only occur at the beginning of a rule

    // set up a vector to contain the broken-up description (each entry in the
    // vector is a separate rule) and a stack for keeping track of opening
    // punctuation
    UStack parenStack;

    UTextOffset p = 0;
    UTextOffset ruleStart = 0;
    UChar c = 0x0000;
    UChar lastC = 0x0000;
    UChar lastOpen = 0x0000;
    UBool haveEquals = FALSE;
    UBool haveSlash = FALSE;
    UBool sawVarName = FALSE;
    UBool sawIllegalChar = FALSE;
    int32_t illegalCharPos = 0;
    UChar expectedClose = 0x0000;

    // if the description doesn't end with a semicolon, tack a semicolon onto the end
    if (description.length() != 0 && description[description.length() - 1] != SEMICOLON) {
        description += SEMICOLON;
    }

    // for each character, do...
    while (p < description.length()) {
        c = description[p];
        switch (c) {
            // if the character is opening punctuation, verify that no nesting
            // rules are broken, and push the character onto the stack
            case OPEN_BRACE:
            case OPEN_BRACKET:
            case OPEN_PAREN:
                if (lastOpen == OPEN_BRACE) {
                    error("Can't nest brackets inside {}", p, description);
                }
                if (lastOpen == OPEN_BRACKET && c != OPEN_BRACKET) {
                    error("Can't nest anything in [] but []", p, description);
                }

                // if we see { anywhere except on the left-hand side of =,
                // we must be seeing a variable name that was never defined
                if (c == OPEN_BRACE && (haveEquals || haveSlash)) {
                    error("Unknown variable name", p, description);
                }

                lastOpen = c;
                parenStack.push((void*)c);
                if (c == OPEN_BRACE) {
                    sawVarName = TRUE;
                }
                break;

            // if the character is closing punctuation, verify that it matches the
            // last opening punctuation we saw, and that the brackets contain
            // something, then pop the stack
            case CLOSE_BRACE:
            case CLOSE_BRACKET:
            case CLOSE_PAREN:
                expectedClose = NULL_CHAR;
                switch (lastOpen) {
                    case OPEN_BRACE:
                        expectedClose = CLOSE_BRACE;
                        break;
                    case OPEN_BRACKET:
                        expectedClose = CLOSE_BRACKET;
                        break;
                    case OPEN_PAREN:
                        expectedClose = CLOSE_PAREN;
                        break;
                }
                if (c != expectedClose) {
                    error("Unbalanced parentheses", p, description);
                }
                if (lastC == lastOpen) {
                    error("Parens don't contain anything", p, description);
                }
                parenStack.pop();
                if (!parenStack.empty()) {
                    lastOpen = (UChar)(int32_t)parenStack.peek();
                }
                else {
                    lastOpen = NULL_CHAR;
                }
                break;

            // if the character is an asterisk, make sure it occurs in a place
            // where an asterisk can legally go
            case ASTERISK: 
            case PLUS: 
            case QUESTION:
                switch (lastC) {
                    case EQUAL_SIGN: case SLASH: case OPEN_PAREN: case PIPE:
                    case ASTERISK: case PLUS: case QUESTION: case SEMICOLON:
                    case NULL_CHAR:
                        error("Misplaced *, +, or ?", p, description);

                    default:
                        break;
                }
                break;

            // if the character is an equals sign, make sure we haven't seen another
            // equals sign or a slash yet
            case EQUAL_SIGN:
                if (haveEquals || haveSlash) {
                    error("More than one = or / in rule", p, description);
                }
                haveEquals = TRUE;
                sawIllegalChar = FALSE;
                break;

            // if the character is a slash, make sure we haven't seen another slash
            // or an equals sign yet
            case SLASH:
                if (haveEquals || haveSlash) {
                    error("More than one = or / in rule", p, description);
                }
                if (sawVarName) {
                    error("Unknown variable name", p, description);
                }
                haveSlash = TRUE;
                break;

            // if the character is an exclamation point, make sure it occurs only
            // at the beginning of a rule
            case BANG:
                if (lastC != SEMICOLON && lastC != NULL_CHAR) {
                    error("! can only occur at the beginning of a rule", p, description);
                }
                break;

            // if the character is a backslash, skip the character that follows it
            // (it'll get treated as a literal character)
            case BACKSLASH:
                ++p;
                break;

            // we don't have to do anything special on a period
            case PERIOD:
                break;

            // if the character is a syntax character that can only occur
            // inside [], make sure that it does in fact only occur inside []
            // (or in a variable name)
            case CARET:
            case MINUS:
            case COLON:
            case AMPERSAND:
                if (lastOpen != OPEN_BRACKET && lastOpen != OPEN_BRACE && !sawIllegalChar) {
                    sawIllegalChar = TRUE;
                    illegalCharPos = p;
                }
                break;

            // if the character is a semicolon, do the following...
            case SEMICOLON:
                // if we saw any illegal characters along the way, throw
                // an error
                if (sawIllegalChar) {
                    error("Illegal character", illegalCharPos, description);
                }
            
                // make sure the rule contains something and that there are no
                // unbalanced parentheses or brackets
                if (lastC == SEMICOLON || lastC == NULL_CHAR) {
                    error("Empty rule", p, description);
                }
                if (!parenStack.empty()) {
                    error("Unbalanced parenheses", p, description);
                }

                if (parenStack.empty()) {
                    // if the rule contained an = sign, call processSubstitution()
                    // to replace the substitution name with the substitution text
                    // wherever it appears in the description
                    if (haveEquals) {
                        processSubstitution(description, ruleStart, p + 1, p + 1, err);
                    }
                    else {
                        // otherwise, check to make sure the rule doesn't reference
                        // any undefined substitutions
                        if (sawVarName) {
                            error("Unknown variable name", p, description);
                        }

                        // then add it to tempRuleList
                        UnicodeString* newRule = new UnicodeString();
                        description.extractBetween(ruleStart, p, *newRule);
                        tempRuleList.addElement(newRule);
                    }

                    // and reset everything to process the next rule
                    ruleStart = p + 1;
                    haveEquals = haveSlash = sawVarName = sawIllegalChar = FALSE;
                }
                break;

            // if the character is a vertical bar, check to make sure that it
            // occurs inside a () expression and that the character that precedes
            // it isn't also a vertical bar
            case PIPE:
                if (lastC == PIPE) {
                    error("Empty alternative", p, description);
                }
                if (parenStack.empty() || lastOpen != OPEN_PAREN) {
                    error("Misplaced |", p, description);
                }
                break;

            // if the character is anything else (escaped characters are
            // skipped and don't make it here), it's an error
            default:
                if (c >= ASCII_LOW && c < ASCII_HI && !Unicode::isLetter(c)
                    && !Unicode::isDigit(c) && !sawIllegalChar) {
                    sawIllegalChar = TRUE;
                    illegalCharPos = p;
                }
                break;
        }
        lastC = c;
        ++p;
    }
    if (tempRuleList.size() == 0) {
        error("No valid rules in description", p, description);
    }
}

/**
 * This function performs variable-name substitutions.  First it does syntax
 * checking on the variable-name definition.  If it's syntactically valid, it
 * then goes through the remainder of the description and does a simple
 * find-and-replace of the variable name with its text.  (The variable text
 * must be enclosed in either [] or () for this to work.)
 */
void
RuleBasedBreakIteratorBuilder::processSubstitution(UnicodeString& description,
                                                   UTextOffset ruleStart,
                                                   UTextOffset ruleEnd,
                                                   UTextOffset startPos,
                                                   UErrorCode& err)
{
    if (U_FAILURE(err))
        return;

    // isolate out the text on either side of the equals sign
    UnicodeString substitutionRule;
    UnicodeString replace;
    UnicodeString replaceWith;

    description.extractBetween(ruleStart, ruleEnd, substitutionRule);
    UTextOffset equalPos = substitutionRule.indexOf(EQUAL_SIGN);
    substitutionRule.extractBetween(0, equalPos, replace);
    substitutionRule.extractBetween(equalPos + 1, substitutionRule.length() - 1, replaceWith);

    // check to see whether the substitution name is something we've declared
    // to be "special".  For RuleBasedBreakIterator itself, this is "$ignore".
    // This function takes care of any extra processing that has to be done
    // with "special" substitution names.
    handleSpecialSubstitution(replace, replaceWith, startPos, description, err);

    // perform various other syntax checks on the rule
    if (replaceWith.length() == 0) {
        error("Nothing on right-hand side of =", startPos, description);
    }
    if (replace.length() == 0) {
        error("Nothing on left-hand side of =", startPos, description);
    }
    if (!(replaceWith[0] == OPEN_BRACKET
            && replaceWith[replaceWith.length() - 1] == CLOSE_BRACKET)
        && !(replaceWith[0] == OPEN_PAREN
            && replaceWith[replaceWith.length() - 1] == CLOSE_PAREN)) {
        error("Illegal right-hand side for =", startPos, description);
    }

    // now go through the rest of the description (which hasn't been broken up
    // into separate rules yet) and replace every occurrence of the
    // substitution name with the substitution body
    if (replace[0] != OPEN_BRACE) {
        replace.insert(0, OPEN_BRACE);
        replace += CLOSE_BRACE;
    }

    description.removeBetween(ruleStart, ruleEnd);

    UTextOffset lastPos = startPos;
    UTextOffset pos = description.indexOf(replace, lastPos);
    while (pos != -1) {
        description.replaceBetween(pos, pos + replace.length(), replaceWith);
        lastPos = pos + replace.length();
        pos = description.indexOf(replace, lastPos);
    }
}

/**
 * This function defines a protocol for handling substitution names that
 * are "special," i.e., that have some property beyond just being
 * substitutions.  At the RuleBasedBreakIterator level, we have one
 * special substitution name, "$ignore".  Subclasses can override this
 * function to add more.  Any special processing that has to go on beyond
 * that which is done by the normal substitution-processing code is done
 * here.
 */
void
RuleBasedBreakIteratorBuilder::handleSpecialSubstitution(const UnicodeString& replace, 
                                                         const UnicodeString& replaceWith,
                                                         int32_t startPos,
                                                         const UnicodeString& description,
                                                         UErrorCode& err)
{
    if (U_FAILURE(err))
        return;

    // if we get a definition for a substitution called "$ignore", it defines
    // the ignore characters for the iterator.  Check to make sure the expression
    // is a [] expression, and if it is, parse it and store the characters off
    // to the side.
    if (replace == IGNORE_NAME) {
        if (replaceWith.charAt(0) == OPEN_PAREN) {
            error("Ignore group can't be enclosed in (", startPos, description);
        }
        ignoreChars = UnicodeSet(replaceWith, err);
    }
}

/**
 * This function provides a hook for subclasses to mess with the character
 * category table.
 */
void
RuleBasedBreakIteratorBuilder::mungeExpressionList()
{
    // base class doesn't do anything-- this is here
    // for subclasses
}

/**
 * This function builds the character category table.  On entry,
 * tempRuleList is a vector of break rules that has had variable names substituted.
 * On exit, the charCategoryTable data member has been initialized to hold the
 * character category table, and tempRuleList's rules have been munged to contain
 * character category numbers everywhere a literal character or a [] expression
 * originally occurred.
 */
void
RuleBasedBreakIteratorBuilder::buildCharCategories(UErrorCode& err)
{
    if (U_FAILURE(err))
        return;

    int32_t bracketLevel = 0;
    UTextOffset p = 0;
    int32_t lineNum = 0;

    // build hash table of every literal character or [] expression in the rule list
    // and derive a UnicodeSet object representing the characters each refers to
    while (lineNum < tempRuleList.size()) {
        UnicodeString* line = (UnicodeString*)(tempRuleList[lineNum]);
        p = 0;
        while (p < line->length()) {
            UChar c = (*line)[p];
            switch (c) {
                // skip over all syntax characters except [
                case OPEN_PAREN: case CLOSE_PAREN: case ASTERISK: case PERIOD: case SLASH:
                case PIPE: case SEMICOLON: case QUESTION: case BANG: case PLUS:
                    break;

                // for [, find the matching ] (taking nested [] pairs into account)
                // and add the whole expression to the expression list
                case OPEN_BRACKET:
                    {
                        UTextOffset q = p + 1;
                        ++bracketLevel;
                        while (q < line->length() && bracketLevel != 0) {
                            c = (*line)[q];
                            if (c == OPEN_BRACKET) {
                                ++bracketLevel;
                            }
                            else if (c == CLOSE_BRACKET) {
                                --bracketLevel;
                            }
                            ++q;
                        }

                        UnicodeString temp;
                        line->extractBetween(p, q, temp);
                        if (&expressions->getSet(temp) == &ExpressionList::setNotThere) {
                            expressions->putSet(temp, new UnicodeSet(temp, err));
                        }
                        p = q - 1;
                    }
                    break;

                // for \ sequences, just move to the next character and treat
                // it as a single character
                case BACKSLASH:
                    ++p;
                    c = (*line)[p];
                    // DON'T break; fall through into "default" clause

                // for an isolated single character, add it to the expression list
                default:
                    {
                        UnicodeString temp;

                        line->extractBetween(p, p + 1, temp);
                        expressions->putSet(temp, new UnicodeSet(temp, err));
                    }
                    break;
            }
            ++p;
        }
        ++lineNum;
    }

    // create the temporary category table (which is a vector of UnicodeSet objects)
    if (ignoreChars.isEmpty()) {
        categories.addElement(new UnicodeSet(ignoreChars));
    }
    else {
        categories.addElement(new UnicodeSet());
    }
    ignoreChars.clear();

    // this is a hook to allow subclasses to add categories on their own
    mungeExpressionList();

    // Derive the character categories.  Go through the existing character categories
    // looking for overlap.  Any time there's overlap, we create a new character
    // category for the characters that overlapped and remove them from their original
    // category.  At the end, any characters that are left in the expression haven't
    // been mentioned in any category, so another new category is created for them.
    // For example, if the first expression is [abc], then a, b, and c will be placed
    // into a single character category.  If the next expression is [bcd], we will first
    // remove b and c from their existing category (leaving a behind), create a new
    // category for b and c, and then create another new category for d (which hadn't
    // been mentioned in the previous expression).
    // At no time should a character ever occur in more than one character category.

    // for each expression in the expressions list, do...
    for (int32_t i = 0; i < expressions->size(); i++) {
        // initialize the working char set to the chars in the current expression
        UnicodeSet e = UnicodeSet((*expressions)[i]);

        // for each category in the category list, do...
        for (int32_t j = categories.size() - 1; !e.isEmpty() && j > 0; j--) {

            // if there's overlap between the current working set of chars
            // and the current category...
            UnicodeSet* that = (UnicodeSet*)(categories[j]);
            UnicodeSet temp = UnicodeSet(e);
            temp.retainAll(*that);
            if (!temp.isEmpty()) {
                // if the current category is not a subset of the current
                // working set of characters, then remove the overlapping
                // characters from the current category and create a new
                // category for them
                if (temp != *that) {
                    that->removeAll(temp);
                    categories.addElement(new UnicodeSet(temp));
                }
                
                // and always remove the overlapping characters from the current
                // working set of characters
                e.removeAll(temp);
            }
        }

        // if there are still characters left in the working char set,
        // add a new category containing them
        if (!e.isEmpty()) {
            categories.addElement(new UnicodeSet(e));
        }
    }

    // we have the ignore characters stored in position 0.  Make an extra pass through
    // the character category list and remove anything from the ignore list that shows
    // up in some other category
    UnicodeSet allChars;
    for (int32_t i = 1; i < categories.size(); i++)
        allChars.addAll(*(UnicodeSet*)(categories[i]));
    UnicodeSet* ignoreChars = (UnicodeSet*)(categories[0]);
    ignoreChars->removeAll(allChars);

    // now that we've derived the character categories, go back through the expression
    // list and replace each UnicodeSet object with a String that represents the
    // character categories that expression refers to.  The String is encoded: each
    // character is a character category number (plus 0x100 to avoid confusing them
    // with syntax characters in the rule grammar)
    for (int32_t i = 0; i < expressions->size(); i++) {
        const UnicodeSet& cs = (*expressions)[i];
        UnicodeString* cats = new UnicodeString;

        // for each category...
        for (int32_t j = 1; j < categories.size(); j++) {

            // if the current expression contains characters in that category...
            if (cs.containsAll(*(UnicodeSet*)(categories[j]))) {

                // then add the encoded category number to the String for this
                // expression
                *cats += (UChar)(0x100 + j);
                if (cs == *(UnicodeSet*)(categories[j])) {
                    break;
                }
            }
        }

        // once we've finished building the encoded String for this expression,
        // replace the UnicodeSet object with it
        expressions->putString(expressions->getKeyAt(i), cats);
    }

    // and finally, we turn the temporary category table into a permanent category
    // table, which is a CompactByteArray. (we skip category 0, which by definition
    // refers to all characters not mentioned specifically in the rules)
    tables->charCategoryTable = ucmp8_open((int8_t)0);

    // for each category...
    for (int32_t i = 0; i < categories.size(); i++) {
        UnicodeSet& chars = *(UnicodeSet*)(categories[i]);
        const UnicodeString& pairs = chars.getPairs();

        // go through the character ranges in the category one by one...
        for (int32_t j = 0; j < pairs.length(); j += 2) {
            // and set the corresponding elements in the CompactArray accordingly
            if (i != 0) {
                ucmp8_setRange(tables->charCategoryTable, pairs[j], pairs[j + 1],
                        (int8_t)i);
            }

            // (category 0 is special-- it's the hiding place for the ignore
            // characters, whose real category number in the CompactArray is
            // -1 [this is because category 0 contains all characters not
            // specifically mentioned anywhere in the rules] )
            else {
                ucmp8_setRange(tables->charCategoryTable, pairs[j], pairs[j + 1],
                    RuleBasedBreakIterator::IGNORE);
            }
        }
    }

    // once we've populated the CompactArray, compact it
    ucmp8_compact(tables->charCategoryTable, 32);

    // initialize numCategories
    numCategories = categories.size();
    tables->numCategories = numCategories;
}

/**
 * This is the function that builds the forward state table.  Most of the real
 * work is done in parseRule(), which is called once for each rule in the
 * description.
 */
void
RuleBasedBreakIteratorBuilder::buildStateTable(UErrorCode& err)
{
    if (U_FAILURE(err))
        return;

    // initialize our temporary state table, and fill it with two states:
    // state 0 is a dummy state that allows state 1 to be the starting state
    // and 0 to represent "stop".  State 1 is added here to seed things
    // before we start parsing
    tempStateTable.addElement(new int16_t[tables->numCategories + 1]);
    tempStateTable.addElement(new int16_t[tables->numCategories + 1]);

    // call parseRule() for every rule in the rule list (except those which
    // start with !, which are actually backwards-iteration rules)
    for (int32_t i = 0; i < tempRuleList.size(); i++) {
        UnicodeString* rule = (UnicodeString*)tempRuleList[i];
        if ((*rule)[0] != BANG) {
            parseRule(*rule, TRUE);
        }
    }

    // finally, use finishBuildingStateTable() to minimize the number of
    // states in the table and perform some other cleanup work
    finishBuildingStateTable(TRUE);
}

/**
 * This is where most of the work really happens.  This routine parses a single
 * rule in the rule description, adding and modifying states in the state
 * table according to the new expression.  The state table is kept deterministic
 * throughout the whole operation, although some ugly postprocessing is needed
 * to handle the *? token.
 */
void
RuleBasedBreakIteratorBuilder::parseRule(const UnicodeString& rule,
                                         UBool forward)
{
    // algorithm notes:
    //   - The basic idea here is to read successive character-category groups
    //   from the input string.  For each group, you create a state and point
    //   the appropriate entries in the previous state to it.  This produces a
    //   straight line from the start state to the end state.  The {}, *, and (|)
    //   idioms produce branches in this straight line.  These branches (states
    //   that can transition to more than one other state) are called "decision
    //   points."  A list of decision points is kept.  This contains a list of
    //   all states that can transition to the next state to be created.  For a
    //   straight line progression, the only thing in the decision-point list is
    //   the current state.  But if there's a branch, the decision-point list
    //   will contain all of the beginning points of the branch when the next
    //   state to be created represents the end point of the branch.  A stack is
    //   used to save decision point lists in the presence of nested parentheses
    //   and the like.  For example, when a { is encountered, the current decision
    //   point list is saved on the stack and restored when the corresponding }
    //   is encountered.  This way, after the } is read, the decision point list
    //   will contain both the state right before the } _and_ the state before
    //   the whole {} expression.  Both of these states can transition to the next
    //   state after the {} expression.
    //   - one complication arises when we have to stamp a transition value into
    //   an array cell that already contains one.  The updateStateTable() and
    //   mergeStates() functions handle this case.  Their basic approach is to
    //   create a new state that combines the two states that conflict and point
    //   at it when necessary.  This happens recursively, so if the merged states
    //   also conflict, they're resolved in the same way, and so on.  There are
    //   a number of tests aimed at preventing infinite recursion.
    //   - another complication arises with repeating characters.  It's somewhat
    //   ambiguous whether the user wants a greedy or non-greedy match in these cases.
    //   (e.g., whether "[a-z]*abc" means the SHORTEST sequence of letters ending in
    //   "abc" or the LONGEST sequence of letters ending in "abc".  We've adopted
    //   the *? to mean "shortest" and * by itself to mean "longest".  (You get the
    //   same result with both if there's no overlap between the repeating character
    //   group and the group immediately following it.)  Handling the *? token is
    //   rather complicated and involves keeping track of whether a state needs to
    //   be merged (as described above) or merely overwritten when you update one of
    //   its cells, and copying the contents of a state that loops with a *? token
    //   into some of the states that follow it after the rest of the table-building
    //   process is complete ("backfilling").
    // implementation notes:
    //   - This function assumes syntax checking has been performed on the input string
    //   prior to its being passed in here.  It assumes that parentheses are
    //   balanced, all literal characters are enclosed in [] and turned into category
    //   numbers, that there are no illegal characters or character sequences, and so
    //   on.  Violation of these invariants will lead to undefined behavior.
    //   - It'd probably be better to use linked lists rather than UVector and UStack
    //   to maintain the decision point list and stack.  I went for simplicity in
    //   this initial implementation.  If performance is critical enough, we can go
    //   back and fix this later.
    //   -There are a number of important limitations on the *? token.  It does not work
    //   right when followed by a repeating character sequence (e.g., ".*?(abc)*")
    //   (although it does work right when followed by a single repeating character).
    //   It will not always work right when nested in parentheses or braces (although
    //   sometimes it will).  It also will not work right if the group of repeating
    //   characters and the group of characters that follows overlap partially
    //   (e.g., "[a-g]*?[e-j]").  None of these capabilites was deemed necessary for
    //   describing breaking rules we know about, so we left them out for
    //   expeditiousness.
    //   - Rules such as "[a-z]*?abc;" will be treated the same as "[a-z]*?aa*bc;"--
    //   that is, if the string ends in "aaaabc", the break will go before the first
    //   "a" rather than the last one.  Both of these are limitations in the design
    //   of RuleBasedBreakIterator and not limitations of the rule parser.

    UTextOffset p = 0;
    int32_t currentState = 1;   // don't use state number 0; 0 means "stop"
    int32_t lastState = currentState;
    UnicodeString pendingChars;
    UnicodeString temp;

    int16_t* state;
    UBool sawEarlyBreak = FALSE;

    // if we're adding rules to the backward state table, mark the initial state
    // as a looping state
    if (!forward) {
        loopingStates.addElement((void*)1);
    }

    // put the current state on the decision point list before we start
    decisionPointList.addElement((void*)currentState); // we want currentState to
                                                             // be 1 here...
    currentState = tempStateTable.size() - 1;   // but after that, we want it to be
                                                // 1 less than the state number of the next state
    while (p < rule.length()) {
        UChar c = rule[p];
        clearLoopingStates = FALSE;

        // this section handles literal characters, escaped characters (which are
        // effectively literal characters too), the . token, and [] expressions
        if (c == OPEN_BRACKET
            || c == BACKSLASH
            || Unicode::isLetter(c)
            || Unicode::isDigit(c)
            || c < ASCII_LOW
            || c == PERIOD
            || c >= ASCII_HI) {

            // if we're not on a period, isolate the expression and look up
            // the corresponding category list
            if (c != PERIOD) {
                UTextOffset q = p;

                // if we're on a backslash, the expression is the character
                // after the backslash
                if (c == BACKSLASH) {
                    q = p + 2;
                    ++p;
                }

                // if we're on an opening bracket, scan to the closing bracket
                // to isolate the expression
                else if (c == OPEN_BRACKET) {
                    int32_t bracketLevel = 1;
                    while (bracketLevel > 0) {
                        ++q;
                        c = rule[q];
                        if (c == OPEN_BRACKET) {
                            ++bracketLevel;
                        }
                        else if (c == CLOSE_BRACKET) {
                            --bracketLevel;
                        }
                        else if (c == BACKSLASH) {
                            ++q;
                        }
                    }
                    ++q;
                }

                // otherwise, the expression is just the character itself
                else {
                    q = p + 1;
                }

                // look up the category list for the expression and store it
                // in pendingChars
                rule.extractBetween(p, q, temp);
                pendingChars = expressions->getString(temp);

                // advance the current position past the expression
                p = q - 1;
            }

            // if the character we're on is a period, we end up down here
            else {
                int32_t rowNum = (int32_t)decisionPointList.lastElement();
                state = (int16_t*)tempStateTable[rowNum];

                // if the period is followed by an asterisk, then just set the current
                // state to loop back on itself
                if (p + 1 < rule.length() && rule[p + 1] == ASTERISK && state[0] != 0) {
                    decisionPointList.addElement((void*)state[0]);
                    pendingChars.remove();
                    ++p;
                    if (p + 1 < rule.length() && rule[p + 1] == QUESTION) {
//System.out.println("Saw *?");
                        setLoopingStates(&decisionPointList, decisionPointList);
                        ++p;
                    }
//System.out.println("Saw .*");
                }

                // otherwise, fabricate a category list ("pendingChars") with
                // every category in it
                else {
                    pendingChars.remove();
                    for (int32_t i = 0; i < numCategories; i++)
                        pendingChars += (UChar)(i + 0x100);
                }
            }

            // we'll end up in here for all expressions except for .*, which is
            // special-cased above
            if (pendingChars.length() != 0) {

                // if the expression is followed by an asterisk, then push a copy
                // of the current decision point list onto the stack
                if (p + 1 < rule.length() && (
                    rule[p + 1] == ASTERISK ||
                    rule[p + 1] == QUESTION
                )) {
                    UVector* clone = new UVector;
                    for (int32_t i = 0; i < decisionPointList.size(); i++) {
                        clone->addElement(decisionPointList[i]);
                        // (there's no ownership issue here because the vector
                        // elements are all integers)
                    }
                    decisionPointStack.push(clone);
                }

                // create a new state, add it to the list of states to backfill
                // if we have looping states to worry about, set its "don't make
                // me an accepting state" flag if we've seen a slash, and add
                // it to the end of the state table
                int32_t newState = tempStateTable.size();
                if (loopingStates.size() != 0) {
                    statesToBackfill.addElement((void*)newState);
                }
                state = new int16_t[numCategories + 1];
                if (sawEarlyBreak) {
                    state[numCategories] = DONT_LOOP_FLAG;
                }
                tempStateTable.addElement(state);

                // update everybody in the decision point list to point to
                // the new state (this also performs all the reconciliation
                // needed to make the table deterministic), then clear the
                // decision point list
                updateStateTable(decisionPointList, pendingChars, (int16_t)newState);
                decisionPointList.removeAllElements();

                // add all states created since the last literal character we've
                // seen to the decision point list
                lastState = currentState;
                do {
                    ++currentState;
                    decisionPointList.addElement((void*)currentState);
                } while (currentState + 1 < tempStateTable.size());
            }
        }

        // a * denotes a repeating character or group (* after () is handled separately
        // below).  In addition to restoring the decision point list, modify the
        // current state to point to itself on the appropriate character categories.
        if (c == PLUS || c == ASTERISK || c == QUESTION) {
            // when there's a *, update the current state to loop back on itself
            // on the character categories that caused us to enter this state
            if (c == ASTERISK || c == PLUS) {
                for (int32_t i = lastState + 1; i < tempStateTable.size(); i++) {
                    UVector temp2;
                    temp2.addElement((void*)i);
                    updateStateTable(temp2, pendingChars, (int16_t)(lastState + 1));
                }
            }

            // pop the top element off the decision point stack and merge
            // it with the current decision point list (this causes the divergent
            // paths through the state table to come together again on the next
            // new state)
            if (c == ASTERISK || c == QUESTION) {
                UVector* temp2 = (UVector*)decisionPointStack.pop();
                for (int32_t i = 0; i < temp2->size(); i++)
                    decisionPointList.addElement((*temp2)[i]);
                delete temp2;

                // a ? after a * modifies the behavior of * in cases where there is overlap
                // between the set of characters that repeat and the characters which follow.
                // Without the ?, all states following the repeating state, up to a state which
                // is reached by a character that doesn't overlap, will loop back into the
                // repeating state.  With the ?, the mark states following the *? DON'T loop
                // back into the repeating state.  Thus, "[a-z]*xyz" will match the longest
                // sequence of letters that ends in "xyz," while "[a-z]*? will match the
                // _shortest_ sequence of letters that ends in "xyz".
                // We use extra bookkeeping to achieve this effect, since everything else works
                // according to the "longest possible match" principle.  The basic principle
                // is that transitions out of a looping state are written in over the looping
                // value instead of being reconciled, and that we copy the contents of the
                // looping state into empty cells of all non-terminal states that follow the
                // looping state.
//System.out.println("c = " + c + ", p = " + p + ", rule.length() = " + rule.length());
                if (c == ASTERISK && p + 1 < rule.length() && rule[p + 1] == QUESTION) {
//System.out.println("Saw *?");
                    setLoopingStates(&decisionPointList, decisionPointList);
                    ++p;
                }
            }
        }

        // a ( marks the beginning of a sequence of characters.  Parentheses can either
        // contain several alternative character sequences (i.e., "(ab|cd|ef)"), or
        // they can contain a sequence of characters that can repeat (i.e., "(abc)*").  Thus,
        // A () group can have multiple entry and exit points.  To keep track of this,
        // we reserve TWO spots on the decision-point stack.  The top of the stack is
        // the list of exit points, which becomes the current decision point list when
        // the ) is reached.  The next entry down is the decision point list at the
        // beginning of the (), which becomes the current decision point list at every
        // entry point.
        // In addition to keeping track of the exit points and the active decision
        // points before the ( (i.e., the places from which the () can be entered),
        // we need to keep track of the entry points in case the expression loops
        // (i.e., is followed by *).  We do that by creating a dummy state in the
        // state table and adding it to the decision point list (BEFORE it's duplicated
        // on the stack).  Nobody points to this state, so it'll get optimized out
        // at the end.  It exists only to hold the entry points in case the ()
        // expression loops.
        if (c == OPEN_PAREN) {

            // add a new state to the state table to hold the entry points into
            // the () expression
            tempStateTable.addElement(new int16_t[numCategories + 1]);

            // we have to adjust lastState and currentState to account for the
            // new dummy state
            lastState = currentState;
            ++currentState;

            // add the current state to the decision point list (add it at the
            // BEGINNING so we can find it later)
            decisionPointList.insertElementAt((void*)currentState, 0);

            // finally, push a copy of the current decision point list onto the
            // stack (this keeps track of the active decision point list before
            // the () expression), followed by an empty decision point list
            // (this will hold the exit points)
            UVector* clone = new UVector;
            for (int32_t i = 0; i < decisionPointList.size(); i++) {
                clone->addElement(decisionPointList[i]);
            }
            decisionPointStack.push(clone);
            decisionPointStack.push(new UVector());
        }

        // a | separates alternative character sequences in a () expression.  When
        // a | is encountered, we add the current decision point list to the exit-point
        // list, and restore the decision point list to its state prior to the (.
        if (c == PIPE) {

            // pick out the top two decision point lists on the stack
            UVector* oneDown = (UVector*)decisionPointStack.pop();
            UVector* twoDown = (UVector*)decisionPointStack.peek();
            decisionPointStack.push(oneDown);

            // append the current decision point list to the list below it
            // on the stack (the list of exit points), and restore the
            // current decision point list to its state before the () expression
            for (int32_t i = 0; i < decisionPointList.size(); i++)
                oneDown->addElement(decisionPointList[i]);
            decisionPointList.removeAllElements();
            for (int32_t i = 0; i < twoDown->size(); i++)
                decisionPointList.addElement((*twoDown)[i]);
        }

        // a ) marks the end of a sequence of characters.  We do one of two things
        // depending on whether the sequence repeats (i.e., whether the ) is followed
        // by *):  If the sequence doesn't repeat, then the exit-point list is merged
        // with the current decision point list and the decision point list from before
        // the () is thrown away.  If the sequence does repeat, then we fish out the
        // state we were in before the ( and copy all of its forward transitions
        // (i.e., every transition added by the () expression) into every state in the
        // exit-point list and the current decision point list.  The current decision
        // point list is then merged with both the exit-point list AND the saved version
        // of the decision point list from before the ().  Then we throw out the *.
        if (c == CLOSE_PAREN) {

            // pull the exit point list off the stack, merge it with the current
            // decision point list, and make the merged version the current
            // decision point list
            UVector* exitPoints = (UVector*)decisionPointStack.pop();
            for (int32_t i = 0; i < exitPoints->size(); i++)
                decisionPointList.addElement((*exitPoints)[i]);
            delete exitPoints;

            // if the ) isn't followed by a *, then all we have to do is throw
            // away the other list on the decision point stack, and we're done
            if (p + 1 >= rule.length() || (
                    rule[p + 1] != ASTERISK &&
                    rule[p + 1] != PLUS &&
                    rule[p + 1] != QUESTION)
            ) {
                delete (UVector*)decisionPointStack.pop();
            }

            // but if the sequence repeats, we have a lot more work to do...
            else {

                // now exitPoints and decisionPointList have to point to equivalent
                // vectors, but not the SAME vector
                exitPoints = new UVector;
                for (int32_t i = 0; i < decisionPointList.size(); i++)
                    exitPoints->addElement(decisionPointList[i]);

                // pop the original decision point list off the stack
                UVector* temp2 = (UVector*)decisionPointStack.pop();

                // we squirreled away the row number of our entry point list
                // at the beginning of the original decision point list.  Fish
                // that state number out and retrieve the entry point list
                int32_t tempStateNum = (int32_t)temp2->firstElement();
                int16_t* tempState = (int16_t*)tempStateTable.elementAt(tempStateNum);

                // merge the original decision point list with the current
                // decision point list
                if (rule.charAt(p + 1) == QUESTION || rule.charAt(p + 1) == ASTERISK) {
                    for (int32_t i = 0; i < temp2->size(); i++)
                        decisionPointList.addElement((*temp2)[i]);
                    delete temp2;
                }

                // finally, copy every forward reference from the entry point
                // list into every state in the new decision point list
                if (rule[p + 1] == PLUS || rule[p + 1] == ASTERISK) {
                    for (int32_t i = 0; i < numCategories; i++) {
                        if (tempState[i] > tempStateNum) {
                            updateStateTable(*exitPoints,
                                             UnicodeString((UChar)(i + 0x100)),
                                             tempState[i]);
                        }
                    }
                }

                // update lastState and currentState, and throw away the *
                lastState = currentState;
                currentState = tempStateTable.size() - 1;
                ++p;
                delete exitPoints;
            }
        }

        // a / marks the position where the break is to go if the character sequence
        // matches this rule.  We update the flag word of every state on the decision
        // point list to mark them as ending states, and take note of the fact that
        // we've seen the slash
        if (c == SLASH) {
            sawEarlyBreak = TRUE;
            for (int32_t i = 0; i < decisionPointList.size(); i++) {
                state = (int16_t*)tempStateTable.elementAt((int32_t)decisionPointList[i]);
                state[numCategories] |= LOOKAHEAD_STATE_FLAG;
            }
        }

        // if we get here without executing any of the above clauses, we have a
        // syntax error.  However, for now we just ignore the offending character
        // and move on
/*
debugPrintln("====Parsed \"" + rule.substring(0, p + 1) + "\"...");
System.out.println("      currentState = " + currentState);
debugPrintVectorOfVectors("      decisionPointStack:", "        ", decisionPointStack);
debugPrintVector("        ", decisionPointList);
debugPrintVector("      loopingStates = ", loopingStates);
debugPrintVector("      statesToBackfill = ", statesToBackfill);
System.out.println("      sawEarlyBreak = " + sawEarlyBreak);
debugPrintTempStateTable();
*/

        // clearLoopingStates is a signal back from updateStateTable() that we've
        // transitioned to a state that won't loop back to the current looping
        // state.  (In other words, we've gotten to a point where we can no longer
        // go back into a *? we saw earlier.)  Clear out the list of looping states
        // and backfill any states that need to be backfilled.
        if (clearLoopingStates) {
            setLoopingStates(0, decisionPointList);
        }

        // advance to the next character, now that we've processed the current
        // character
        ++p;
    }

    // this takes care of backfilling any states that still need to be backfilled
    setLoopingStates(0, decisionPointList);

    // when we reach the end of the string, we do a postprocessing step to mark the
    // end states.  The decision point list contains every state that can transition
    // to the end state-- that is, every state that is the last state in a sequence
    // that matches the rule.  All of these states are considered "mark states"
    // or "accepting states"-- that is, states that cause the position returned from
    // next() to be updated.  A mark state represents a possible break position.
    // This allows us to look ahead and remember how far the rule matched
    // before following the new branch (see next() for more information).
    // The temporary state table has an extra "flag column" at the end where this
    // information is stored.  We mark the end states by setting a flag in their
    // flag column.
    // Now if we saw the / in the rule, then everything after it is lookahead
    // material and the break really goes where the slash is.  In this case,
    // we mark these states as BOTH accepting states and lookahead states.  This
    // signals that these states cause the break position to be updated to the
    // position of the slash rather than the current break position.
    for (int32_t i = 0; i < decisionPointList.size(); i++) {
        int32_t rowNum = (int32_t)decisionPointList[i];
        state = (int16_t*)tempStateTable[rowNum];
        state[numCategories] |= END_STATE_FLAG;
        if (sawEarlyBreak) {
            state[numCategories] |= LOOKAHEAD_STATE_FLAG;
        }
    }
/*
debugPrintln("====Parsed \"" + rule + ";");
System.out.println();
System.out.println("      currentState = " + currentState);
debugPrintVectorOfVectors("      decisionPointStack:", "        ", decisionPointStack);
debugPrintVector("        ", decisionPointList);
debugPrintVector("      loopingStates = ", loopingStates);
debugPrintVector("      statesToBackfill = ", statesToBackfill);
System.out.println("      sawEarlyBreak = " + sawEarlyBreak);
debugPrintTempStateTable();
*/
}

/**
 * Update entries in the state table, and merge states when necessary to keep
 * the table deterministic.
 * @param rows The list of rows that need updating (the decision point list)
 * @param pendingChars A character category list, encoded in a String.  This is the
 * list of the columns that need updating.
 * @param newValue Update the cells specfied above to contain this value
 */
void
RuleBasedBreakIteratorBuilder::updateStateTable(const UVector& rows,
                                                const UnicodeString& pendingChars,
                                                int16_t newValue)
{
    // create a dummy state that has the specified row number (newValue) in
    // the cells that need to be updated (those specified by pendingChars)
    // and 0 in the other cells
    int16_t* newValues = new int16_t[numCategories + 1];
    for (int32_t i = 0; i < pendingChars.length(); i++)
        newValues[(int32_t)(pendingChars[i]) - 0x100] = newValue;

    // go through the list of rows to update, and update them by calling
    // mergeStates() to merge them the the dummy state we created
    for (int32_t i = 0; i < rows.size(); i++) {
        mergeStates((int32_t)rows[i], newValues, rows);
    }
}

/**
 * The real work of making the state table deterministic happens here.  This function
 * merges a state in the state table (specified by rowNum) with a state that is
 * passed in (newValues).  The basic process is to copy the nonzero cells in newStates
 * into the state in the state table (we'll call that oldValues).  If there's a
 * collision (i.e., if the same cell has a nonzero value in both states, and it's
 * not the SAME value), then we have to reconcile the collision.  We do this by
 * creating a new state, adding it to the end of the state table, and using this
 * function recursively to merge the original two states into a single, combined
 * state.  This process may happen recursively (i.e., each successive level may
 * involve collisions).  To prevent infinite recursion, we keep a log of merge
 * operations.  Any time we're merging two states we've merged before, we can just
 * supply the row number for the result of that merge operation rather than creating
 * a new state just like it.
 * @param rowNum The row number in the state table of the state to be updated
 * @param newValues The state to merge it with.
 * @param rowsBeingUpdated A copy of the list of rows passed to updateStateTable()
 * (itself a copy of the decision point list from parseRule()).  Newly-created
 * states get added to the decision point list if their "parents" were on it.
 */
void
RuleBasedBreakIteratorBuilder::mergeStates(int32_t rowNum,
                                           int16_t* newValues,
                                           const UVector& rowsBeingUpdated)
{
    int16_t* oldValues = (int16_t*)(tempStateTable[rowNum]);
/*
System.out.print("***Merging " + rowNum + ":");
for (int32_t i = 0; i < oldValues.length; i++) System.out.print("\t" + oldValues[i]);
System.out.println();
System.out.print("    with   \t");
for (int32_t i = 0; i < newValues.length; i++) System.out.print("\t" + newValues[i]);
System.out.println();
*/

    UBool isLoopingState = loopingStates.contains((void*)rowNum);

    // for each of the cells in the rows we're reconciling, do...
    for (int32_t i = 0; i < numCategories; i++) {

        // if they contain the same value, we don't have to do anything
        if (oldValues[i] == newValues[i]) {
            continue;
        }

        // if oldValues is a looping state and the state the current cell points to
        // is too, then we can just stomp over the current value of that cell (and
        // set the clear-looping-states flag if necessary)
        else if (isLoopingState && loopingStates.contains((void*)oldValues[i])) {
            if (newValues[i] != 0) {
                if (oldValues[i] == 0) {
                    clearLoopingStates = TRUE;
                }
                oldValues[i] = newValues[i];
            }
        }

        // if the current cell in oldValues is 0, copy in the corresponding value
        // from newValues
        else if (oldValues[i] == 0) {
            oldValues[i] = newValues[i];
        }

        // the last column of each row is the flag column.  Take care to merge the
        // flag words correctly
        else if (i == numCategories) {
            oldValues[i] = (int16_t)((newValues[i] & ALL_FLAGS) | oldValues[i]);
        }

        // if both newValues and oldValues have a nonzero value in the current
        // cell, and it isn't the same value both places...
        else if (oldValues[i] != 0 && newValues[i] != 0) {

            // look up this pair of cell values in the merge list.  If it's
            // found, update the cell in oldValues to point to the merged state
            int32_t combinedRowNum = searchMergeList(oldValues[i], newValues[i]);
            if (combinedRowNum != 0) {
                oldValues[i] = (int16_t)combinedRowNum;
            }

            // otherwise, we have to reconcile them...
            else {
                // copy our row numbers into variables to make things easier
                int32_t oldRowNum = oldValues[i];
                int32_t newRowNum = newValues[i];
                combinedRowNum = tempStateTable.size();

                // add this pair of row numbers to the merge list (create it first
                // if we haven't created the merge list yet)
                int32_t* entry = new int32_t[3];
                entry[0] = oldRowNum;
                entry[1] = newRowNum;
                entry[2] = combinedRowNum;
                mergeList.addElement(entry);

//System.out.println("***At " + rowNum + ", merging " + oldRowNum + " and " + newRowNum + " into " + combinedRowNum);

                // create a new row to represent the merged state, and copy the
                // contents of oldRow into it, then add it to the end of the
                // state table and update the original row (oldValues) to point
                // to the new, merged, state
                int16_t* newRow = new int16_t[numCategories + 1];
                int16_t* oldRow = (int16_t*)(tempStateTable[oldRowNum]);
                uprv_memcpy(newRow, oldRow, (numCategories + 1) * sizeof int16_t);
                tempStateTable.addElement(newRow);
                oldValues[i] = (int16_t)combinedRowNum;


//System.out.println("lastOldRowNum = " + lastOldRowNum);
//System.out.println("lastCombinedRowNum = " + lastCombinedRowNum);
//System.out.println("decisionPointList.contains(lastOldRowNum) = " + decisionPointList.contains(new Integer(lastOldRowNum)));
//System.out.println("decisionPointList.contains(lastCombinedRowNum) = " + decisionPointList.contains(new Integer(lastCombinedRowNum)));

                // if the decision point list contains either of the parent rows,
                // update it to include the new row as well
                if ((decisionPointList.contains((void*)oldRowNum)
                        || decisionPointList.contains((void*)newRowNum))
                    && !decisionPointList.contains((void*)combinedRowNum)
                ) {
                    decisionPointList.addElement((void*)combinedRowNum);
                }

                // do the same thing with the list of rows being updated
                if ((rowsBeingUpdated.contains((void*)oldRowNum)
                        || rowsBeingUpdated.contains((void*)newRowNum))
                    && !rowsBeingUpdated.contains((void*)combinedRowNum)
                ) {
                    decisionPointList.addElement((void*)combinedRowNum);
                }
                // now (groan) do the same thing for all the entries on the
                // decision point stack
                for (int32_t k = 0; k < decisionPointStack.size(); k++) {
                    UVector* dpl = (UVector*)decisionPointStack[k];
                    if ((dpl->contains((void*)oldRowNum)
                            || dpl->contains((void*)newRowNum))
                        && !dpl->contains((void*)combinedRowNum)
                    ) {
                        dpl->addElement((void*)combinedRowNum);
                    }
                }

                // FINALLY (puff puff puff), call mergeStates() recursively to copy
                // the row referred to by newValues into the new row and resolve any
                // conflicts that come up at that level
                mergeStates(combinedRowNum, (int16_t*)(tempStateTable.elementAt(
                                newValues[i])), rowsBeingUpdated);
            }
        }
    }
}

/**
 * The merge list is a list of pairs of rows that have been merged somewhere in
 * the process of building this state table, along with the row number of the
 * row containing the merged state.  This function looks up a pair of row numbers
 * and returns the row number of the row they combine into.  (It returns 0 if
 * this pair of rows isn't in the merge list.)
 */
int32_t
RuleBasedBreakIteratorBuilder::searchMergeList(int32_t a, int32_t b)
{
    int32_t* entry;
    for (int32_t i = 0; i < mergeList.size(); i++) {
        entry = (int32_t*)(mergeList[i]);

        // we have a hit if the two row numbers match the two row numbers
        // in the beginning of the entry (the two that combine), in either
        // order
        if ((entry[0] == a && entry[1] == b) || (entry[0] == b && entry[1] == a)) {
            return entry[2];
        }

        // we also have a hit if one of the two row numbers matches the marged
        // row number and the other one matches one of the original row numbers
        if ((entry[2] == a && (entry[0] == b || entry[1] == b))) {
            return entry[2];
        }
        if ((entry[2] == b && (entry[0] == a || entry[1] == a))) {
            return entry[2];
        }
    }
    return 0;
}

/**
 * This function is used to update the list of current loooping states (i.e.,
 * states that are controlled by a *? construct).  It backfills values from
 * the looping states into unpopulated cells of the states that are currently
 * marked for backfilling, and then updates the list of looping states to be
 * the new list
 * @param newLoopingStates The list of new looping states
 * @param endStates The list of states to treat as end states (states that
 * can exit the loop).
 */
void
RuleBasedBreakIteratorBuilder::setLoopingStates(const UVector* newLoopingStates,
                                                const UVector& endStates)
{
    // if the current list of looping states isn't empty, we have to backfill
    // values from the looping states into the states that are waiting to be
    // backfilled
    if (!loopingStates.isEmpty()) {
        int32_t loopingState = (int32_t)loopingStates.lastElement();
        int32_t rowNum;

        // don't backfill into an end state OR any state reachable from an end state
        // (since the search for reachable states is recursive, it's split out into
        // a separate function, eliminateBackfillStates(), below)
        for (int32_t i = 0; i < endStates.size(); i++) {
            eliminateBackfillStates((int32_t)endStates[i]);
        }

        // we DON'T actually backfill the states that need to be backfilled here.
        // Instead, we MARK them for backfilling.  The reason for this is that if
        // there are multiple rules in the state-table description, the looping
        // states may have some of their values changed by a succeeding rule, and
        // this wouldn't be reflected in the backfilled states.  We mark a state
        // for backfilling by putting the row number of the state to copy from
        // into the flag cell at the end of the row
        for (int32_t i = 0; i < statesToBackfill.size(); i++) {
            rowNum = (int32_t)statesToBackfill.elementAt(i);
            int16_t* state = (int16_t*)tempStateTable[rowNum];
            state[numCategories] =
                (int16_t)((state[numCategories] & ALL_FLAGS) | loopingState);
        }
        statesToBackfill.removeAllElements();
        loopingStates.removeAllElements();
    }

    if (newLoopingStates != 0) {
        for (int32_t i = 0; i < newLoopingStates->size(); i++) {
            loopingStates.addElement((*newLoopingStates)[i]);
        }
    }
}

/**
 * This removes "ending states" and states reachable from them from the
 * list of states to backfill.
 * @param The row number of the state to remove from the backfill list
 */
void
RuleBasedBreakIteratorBuilder::eliminateBackfillStates(int32_t baseState)
{
    // don't do anything unless this state is actually in the backfill list...
    if (statesToBackfill.contains((void*)baseState)) {

        // if it is, take it out
        statesToBackfill.removeElement((void*)baseState);

        // then go through and recursively call this function for every
        // state that the base state points to
        int16_t* state = (int16_t*)tempStateTable[baseState];
        for (int32_t i = 0; i < numCategories; i++) {
            if (state[i] != 0) {
                eliminateBackfillStates(state[i]);
            }
        }
    }
}

/**
 * This function completes the backfilling process by actually doing the
 * backfilling on the states that are marked for it
 */
void
RuleBasedBreakIteratorBuilder::backfillLoopingStates(void)
{
    int16_t* state;
    int16_t* loopingState = 0;
    int32_t loopingStateRowNum = 0;
    int32_t fromState;

    // for each state in the state table...
    for (int32_t i = 0; i < tempStateTable.size(); i++) {
        state = (int16_t*)tempStateTable[i];

        // check the state's flag word to see if it's marked for backfilling
        // (it's marked for backfilling if any bits other than the two high-order
        // bits are set-- if they are, then the flag word, minus the two high bits,
        // is the row number to copy from)
        fromState = state[numCategories] & ~ALL_FLAGS;
        if (fromState > 0) {

            // load up the state to copy from (if we haven't already)
            if (fromState != loopingStateRowNum) {
                loopingStateRowNum = fromState;
                loopingState = (int16_t*)tempStateTable[loopingStateRowNum];
            }

            // clear out the backfill part of the flag word
            state[numCategories] &= ALL_FLAGS;

            // then fill all zero cells in the current state with values
            // from the corresponding cells of the fromState
            for (int32_t j = 0; j < numCategories + 1; j++) {
                if (state[j] == 0) {
                    state[j] = loopingState[j];
                }
                else if (state[j] == DONT_LOOP_FLAG) {
                    state[j] = 0;
                }
            }
        }
    }
}

/**
 * This function completes the state-table-building process by doing several
 * postprocessing steps and copying everything into its final resting place
 * in the iterator itself
 * @param forward TRUE if we're working on the forward state table
 */
void
RuleBasedBreakIteratorBuilder::finishBuildingStateTable(UBool forward)
{
//debugPrintTempStateTable();
    // start by backfilling the looping states
    backfillLoopingStates();
//debugPrintTempStateTable();

    int32_t* rowNumMap = new int32_t[tempStateTable.size()];
    int32_t rowNumMapSize = tempStateTable.size();
    UStack rowsToFollow;
    rowsToFollow.push((void*)1);
    rowNumMap[1] = 1;

    // determine which states are no longer reachable from the start state
    // (the reachable states will have their row numbers in the row number
    // map, and the nonreachable states will have zero in the row number map)
    while (rowsToFollow.size() != 0) {
        int32_t rowNum = (int32_t)rowsToFollow.pop();
        int16_t* row = (int16_t*)(tempStateTable[rowNum]);

        for (int32_t i = 0; i < numCategories; i++) {
            if (row[i] != 0) {
                if (rowNumMap[row[i]] == 0) {
                    rowNumMap[row[i]] = row[i];
                    rowsToFollow.push((void*)row[i]);
                }
            }
        }
    }
/*
System.out.println("The following rows are not reachable:");
for (int32_t i = 1; i < rowNumMap.length; i++)
if (rowNumMap[i] == 0) System.out.print("\t" + i);
System.out.println();
*/

    int32_t newRowNum;

    // algorithm for minimizing the number of states in the table adapted from
    // Aho & Ullman, "Principles of Compiler Design"
    // The basic idea here is to organize the states into classes.  When we're done,
    // all states in the same class can be considered identical and all but one eliminated.

    // initially assign states to classes based on the number of populated cells they
    // contain (the class number is the number of populated cells)
    int32_t* stateClasses = new int32_t[tempStateTable.size()];
    int32_t nextClass = numCategories + 1;
    int16_t* state1 = 0;
    int16_t* state2 = 0;
    for (int32_t i = 1; i < tempStateTable.size(); i++) {
        if (rowNumMap[i] == 0) {
            continue;
        }
        state1 = (int16_t*)tempStateTable[i];
        for (int32_t j = 0; j < numCategories; j++) {
            if (state1[j] != 0) {
                ++stateClasses[i];
            }
        }
        if (stateClasses[i] == 0) {
            stateClasses[i] = nextClass;
        }
    }
    ++nextClass;

    // then, for each class, elect the first member of that class as that class's
    // "representative".  For each member of the class, compare it to the "representative."
    // If there's a column position where the state being tested transitions to a
    // state in a DIFFERENT class from the class where the "representative" transitions,
    // then move the state into a new class.  Repeat this process until no new classes
    // are created.
    int32_t currentClass;
    int32_t lastClass;
    UBool split;

    do {
//System.out.println("Making a pass...");
        currentClass = 1;
        lastClass = nextClass;
        while (currentClass < nextClass) {
//System.out.print("States in class #" + currentClass +":");
            split = FALSE;
            state1 = state2 = 0;
            for (int32_t i = 0; i < tempStateTable.size(); i++) {
                if (stateClasses[i] == currentClass) {
//System.out.print("\t" + i);
                    if (state1 == 0) {
                        state1 = (int16_t*)tempStateTable[i];
                    }
                    else {
                        state2 = (int16_t*)tempStateTable[i];
                        for (int32_t j = 0; j < numCategories + 1; j++) {
                            if ((j == numCategories && state1[j] != state2[j] && forward)
                                    || (j != numCategories && stateClasses[state1[j]]
                                    != stateClasses[state2[j]])) {
                                stateClasses[i] = nextClass;
                                split = TRUE;
                                break;
                            }
                        }
                    }
                }
            }
            if (split) {
                ++nextClass;
            }
            ++currentClass;
//System.out.println();
        }
    } while (lastClass != nextClass);

    // at this point, all of the states in a class except the first one (the
    //"representative") can be eliminated, so update the row-number map accordingly
    int32_t* representatives = new int32_t[nextClass];
    for (int32_t i = 1; i < tempStateTable.size(); i++) {
        if (representatives[stateClasses[i]] == 0) {
            representatives[stateClasses[i]] = i;
        }
        else {
            rowNumMap[i] = representatives[stateClasses[i]];
        }
    }
    delete [] stateClasses;
    delete [] representatives;
//System.out.println("Renumbering...");

    // renumber all remaining rows...
    // first drop all that are either unreferenced or not a class representative
    for (int32_t i = 1; i < rowNumMapSize; i++) {
        if (rowNumMap[i] != i) {
            delete [] tempStateTable[i];
            tempStateTable.setElementAt(0, i);
        }
    }

    // then calculate everybody's new row number and update the row
    // number map appropriately (the first pass updates the row numbers
    // of all the class representatives [the rows we're keeping], and the
    // second pass updates the cross references for all the rows that
    // are being deleted)
    newRowNum = 1;
    for (int32_t i = 1; i < rowNumMapSize; i++) {
        if (tempStateTable[i] != 0) {
            rowNumMap[i] = newRowNum++;
        }
    }
    for (int32_t i = 1; i < rowNumMapSize; i++) {
        if (tempStateTable[i] == 0) {
            rowNumMap[i] = rowNumMap[rowNumMap[i]];
        }
    }
//for (int32_t i = 1; i < rowNumMap.length; i++) rowNumMap[i] = i; int32_t newRowNum = rowNumMap.length;

    // allocate the permanent state table, and copy the remaining rows into it
    // (adjusting all the cell values, of course)

    // this section does that for the forward state table
    if (forward) {
        tables->endStates = new UBool[newRowNum];
        tables->lookaheadStates = new UBool[newRowNum];
        tables->stateTable = new int16_t[newRowNum * numCategories];
        int32_t p = 0;
        int32_t p2 = 0;
        for (int32_t i = 0; i < tempStateTable.size(); i++) {
            int16_t* row = (int16_t*)(tempStateTable[i]);
            if (row == 0) {
                continue;
            }
            for (int32_t j = 0; j < numCategories; j++) {
                tables->stateTable[p] = (int16_t)(rowNumMap[row[j]]);
                ++p;
            }
            tables->endStates[p2] = ((row[numCategories] & END_STATE_FLAG) != 0);
            tables->lookaheadStates[p2] = ((row[numCategories] & LOOKAHEAD_STATE_FLAG) != 0);
            ++p2;
        }
    }

    // and this section does it for the backward state table
    else {
        tables->backwardsStateTable = new int16_t[newRowNum * numCategories];
        int32_t p = 0;
        for (int32_t i = 0; i < tempStateTable.size(); i++) {
            int16_t* row = (int16_t*)(tempStateTable[i]);
            if (row == 0) {
                continue;
            }
            for (int32_t j = 0; j < numCategories; j++) {
                tables->backwardsStateTable[p] = (int16_t)(rowNumMap[row[j]]);
                ++p;
            }
        }
    }

    delete [] rowNumMap;
}

/**
 * This function builds the backward state table from the forward state
 * table and any additional rules (identified by the ! on the front)
 * supplied in the description
 */
void
RuleBasedBreakIteratorBuilder::buildBackwardsStateTable(UErrorCode& err)
{
    if (U_FAILURE(err))
        return;

    // create the temporary state table and seed it with two rows (row 0
    // isn't used for anything, and we have to create row 1 (the initial
    // state) before we can do anything else
    tempStateTable.removeAllElements();
    tempStateTable.addElement(new int16_t[numCategories + 1]);
    tempStateTable.addElement(new int16_t[numCategories + 1]);

    // although the backwards state table is built automatically from the forward
    // state table, there are some situations (the default sentence-break rules,
    // for example) where this doesn't yield enough stop states, causing a dramatic
    // drop in performance.  To help with these cases, the user may supply
    // supplemental rules that are added to the backward state table.  These have
    // the same syntax as the normal break rules, but begin with BANG to distinguish
    // them from normal break rules
    for (int32_t i = 0; i < tempRuleList.size(); i++) {
        UnicodeString* rule = (UnicodeString*)tempRuleList[i];
        if ((*rule)[0] == BANG) {
            rule->remove(0, 1);
            parseRule(*rule, FALSE);
        }
    }
    backfillLoopingStates();

    // Backwards iteration is qualitatively different from forwards iteration.
    // This is because backwards iteration has to be made to operate from no context
    // at all-- the user should be able to ask BreakIterator for the break position
    // immediately on either side of some arbitrary offset in the text.  The
    // forward iteration table doesn't let us do that-- it assumes complete
    // information on the context, which means starting from the beginning of the
    // document.
    // The way we do backward and random-access iteration is to back up from the
    // current (or user-specified) position until we see something we're sure is
    // a break position (it may not be the last break position immediately
    // preceding our starting point, however).  Then we roll forward from there to
    // locate the actual break position we're after.
    // This means that the backwards state table doesn't have to identify every
    // break position, allowing the building algorithm to be much simpler.  Here,
    // we use a "pairs" approach, scanning the forward-iteration state table for
    // pairs of character categories we ALWAYS break between, and building a state
    // table from that information.  No context is required-- all this state table
    // looks at is a pair of adjacent characters.

    // It's possible that the user has supplied supplementary rules (see above).
    // This has to be done first to keep parseRule() and friends from becoming
    // EVEN MORE complicated.  The automatically-generated states are appended
    // onto the end of the state table, and then the two sets of rules are
    // stitched together at the end.  Take note of the row number of the
    // first row of the auromatically-generated part.
    int32_t backTableOffset = tempStateTable.size();
    if (backTableOffset > 2) {
        ++backTableOffset;
    }

    // the automatically-generated part of the table models a two-dimensional
    // array where the two dimensions represent the two characters we're currently
    // looking at.  To model this as a state table, we actually need one additional
    // row to represent the initial state.  It gets populated with the row numbers
    // of the other rows (in order).
    for (int32_t i = 0; i < numCategories + 1; i++)
        tempStateTable.addElement(new int16_t[numCategories + 1]);

    int16_t* state = (int16_t*)tempStateTable[backTableOffset - 1];
    for (int32_t i = 0; i < numCategories; i++)
        state[i] = (int16_t)(i + backTableOffset);

    // scavenge the forward state table for pairs of character categories
    // that always have a break between them.  The algorithm is as follows:
    // Look down each column in the state table.  For each nonzero cell in
    // that column, look up the row it points to.  For each nonzero cell in
    // that row, populate a cell in the backwards state table: the row number
    // of that cell is the number of the column we were scanning (plus the
    // offset that locates this sub-table), and the column number of that cell
    // is the column number of the nonzero cell we just found.  This cell is
    // populated with its own column number (adjusted according to the actual
    // location of the sub-table).  This process will produce a state table
    // whose behavior is the same as looking up successive pairs of characters
    // in an array of Booleans to determine whether there is a break.
    int32_t numRows = tempStateTable.size() / numCategories;
    for (int32_t column = 0; column < numCategories; column++) {
        for (int32_t row = 0; row < numRows; row++) {
            int32_t nextRow = tables->lookupState(row, column);
            if (nextRow != 0) {
                for (int32_t nextColumn = 0; nextColumn < numCategories; nextColumn++) {
                    int32_t cellValue = tables->lookupState(nextRow, nextColumn);
                    if (cellValue != 0) {
                        state = (int16_t*)tempStateTable[nextColumn + backTableOffset];
                        state[column] = (int16_t)(column + backTableOffset);
                    }
                }
            }
        }
    }

//debugPrintTempStateTable();
    // if the user specified some backward-iteration rules with the ! token,
    // we have to merge the resulting state table with the auto-generated one
    // above.  First copy the populated cells from row 1 over the populated
    // cells in the auto-generated table.  Then copy values from row 1 of the
    // auto-generated table into all of the the unpopulated cells of the
    // rule-based table.
    if (backTableOffset > 1) {

        // for every row in the auto-generated sub-table, if a cell is
        // populated that is also populated in row 1 of the rule-based
        // sub-table, copy the value from row 1 over the value in the
        // auto-generated sub-table
        state = (int16_t*)tempStateTable[1];
        for (int32_t i = backTableOffset - 1; i < tempStateTable.size(); i++) {
            int16_t* state2 = (int16_t*)tempStateTable[i];
            for (int32_t j = 0; j < numCategories; j++) {
                if (state[j] != 0 && state2[j] != 0) {
                    state2[j] = state[j];
                }
            }
        }

        // now, for every row in the rule-based sub-table that is not
        // an end state, fill in all unpopulated cells with the values
        // of the corresponding cells in the first row of the auto-
        // generated sub-table.
        state = (int16_t*)tempStateTable[backTableOffset - 1];
        for (int32_t i = 1; i < backTableOffset - 1; i++) {
            int16_t* state2 = (int16_t*)tempStateTable[i];
            if ((state2[numCategories] & END_STATE_FLAG) == 0) {
                for (int32_t j = 0; j < numCategories; j++) {
                    if (state2[j] == 0) {
                        state2[j] = state[j];
                    }
                }
            }
        }
    }

//debugPrintTempStateTable();

    // finally, clean everything up and copy it into the actual BreakIterator
    // by calling finishBuildingStateTable()
    finishBuildingStateTable(FALSE);
/*
System.out.print("C:\t");
for (int32_t i = 0; i < numCategories; i++)
System.out.print(Integer.toString(i) + "\t");
System.out.println(); System.out.print("=================================================");
for (int32_t i = 0; i < backwardsStateTable.length; i++) {
if (i % numCategories == 0) {
System.out.println();
System.out.print(Integer.toString(i / numCategories) + ":\t");
}
if (backwardsStateTable[i] == 0) System.out.print(".\t"); else System.out.print(Integer.toString(backwardsStateTable[i]) + "\t");
}
System.out.println();
*/
}

void
RuleBasedBreakIteratorBuilder::setUpErrorMessage(const UnicodeString& message,
                                                 int32_t position,
                                                 const UnicodeString& context)
{
    static UChar lbrks[] = { 0x000a, 0x000a };

    errorMessage = context;
    errorMessage.insert(position, lbrks, 2);
    errorMessage.insert(0, lbrks, 1);
    errorMessage.insert(0, message);
}
