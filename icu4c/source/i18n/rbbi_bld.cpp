/*
**********************************************************************
*   Copyright (C) 1999 International Business Machines Corporation   *
*   and others. All rights reserved.                                 *
**********************************************************************
*   Date        Name        Description
*   10/22/99    alan        Creation.
**********************************************************************
*/

#include "rbbi.h"
#include "rbbi_bld.h"

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

/**
 * No special construction is required for the Builder.
 */
RuleBasedBreakIteratorBuilder::RuleBasedBreakIteratorBuilder() {
}

/**
 * This is the main function for setting up the BreakIterator's tables.  It
 * just vectors different parts of the job off to other functions.
 */
void RuleBasedBreakIteratorBuilder::buildBreakIterator() {
    Vector tempRuleList = buildRuleList(description);
    buildCharCategories(tempRuleList);
    buildStateTable(tempRuleList);
    buildBackwardsStateTable(tempRuleList);
}

/**
 * Thus function has three main purposes:
 * <ul><li>Perform general syntax checking on the description, so the rest of the
 * build code can assume that it's parsing a legal description.
 * <li>Split the description into separate rules
 * <li>Perform variable-name substitutions (so that no one else sees variable names)
 * </ul>
 */
Vector RuleBasedBreakIteratorBuilder::buildRuleList(UnicodeString description) {
    // invariants:
    // - parentheses must be balanced: ()[]{}<>
    // - nothing can be nested inside <>
    // - nothing can be nested inside [] except more []s
    // - pairs of ()[]{}<> must not be empty
    // - ; can only occur at the outer level
    // - | can only appear inside ()
    // - only one = or / can occur in a single rule
    // - = and / cannot both occur in the same rule
    // - <> can only occur on the left side of a = expression
    //   (because we'll perform substitutions to eliminate them from other places)
    // - the left-hand side of a = expression can only be a single character
    //   (possibly with \) or text inside <>
    // - the right-hand side of a = expression must be enclosed in [] or ()
    // - * may not occur at the beginning of a rule, nor may it follow
    //   =, /, (, (, |, }, ;, or *
    // - ? may only follow *
    // - the rule list must contain at least one / rule
    // - no rule may be empty
    // - all printing characters in the ASCII range except letters and digits
    //   are reserved and must be preceded by \
    // - ! may only occur at the beginning of a rule

    // set up a vector to contain the broken-up description (each entry in the
    // vector is a separate rule) and a stack for keeping track of opening
    // punctuation
    Vector tempRuleList = new Vector();
    Stack parenStack = new Stack();

    int32_t p = 0;
    int32_t ruleStart = 0;
    UChar c = '\u0000';
    UChar lastC = '\u0000';
    UChar lastOpen = '\u0000';
    bool_t haveEquals = FALSE;
    bool_t havePipe = FALSE;
    bool_t sawVarName = FALSE;
    final UnicodeString UCharsThatCantPrecedeAsterisk = "=/{(|}*;\u0000";

    // if the description doesn't end with a semicolon, tack a semicolon onto the end
    if (description.length() != 0 && description.UCharAt(description.length() - 1) != ';')
        description = description + ";";

    // for each character, do...
    while (p < description.length()) {
        c = description.UCharAt(p);
        switch (c) {
            // if the character is opening punctuation, verify that no nesting
            // rules are broken, and push the character onto the stack
            case '{':
            case '<':
            case '[':
            case '(':
                if (lastOpen == '<')
                    error("Can't nest brackets inside <>", p, description);
                if (lastOpen == '[' && c != '[')
                    error("Can't nest anything in [] but []", p, description);
                
                // if we see < anywhere except on the left-hand side of =,
                // we must be seeing a variable name that was never defined
                if (c == '<' && (haveEquals || havePipe))
                    error("Unknown variable name", p, description);

                lastOpen = c;
                parenStack.push(new Character(c));
                if (c == '<')
                    sawVarName = TRUE;
                break;

            // if the character is closing punctuation, verify that it matches the
            // last opening punctuation we saw, and that the brackets contain
            // something, then pop the stack
            case '}':
            case '>':
            case ']':
            case ')':
                UChar expectedClose = '\u0000';
                switch (lastOpen) {
                    case '{':
                        expectedClose = '}';
                        break;
                    case '[':
                        expectedClose = ']';
                        break;
                    case '(':
                        expectedClose = ')';
                        break;
                    case '<':
                        expectedClose = '>';
                        break;
                }
                if (c != expectedClose)
                    error("Unbalanced parentheses", p, description);
                if (lastC == lastOpen)
                    error("Parens don't contain anything", p, description);
                parenStack.pop();
                if (!parenStack.empty())
                    lastOpen = ((Character)(parenStack.peek())).UCharValue();
                else
                    lastOpen = '\u0000';

                break;

            // if the character is an asterisk, make sure it occurs in a place
            // where an asterisk can legally go
            case '*':
                if (UCharsThatCantPrecedeAsterisk.indexOf(lastC) != -1)
                    error("Misplaced asterisk", p, description);
                break;

            // if the character is a question mark, make sure it follows an asterisk
            case '?':
                if (lastC != '*')
                    error("Misplaced ?", p, description);
                break;

            // if the character is an equals sign, make sure we haven't seen another
            // equals sign or a slash yet
            case '=':
                if (havePipe || haveEquals)
                    error("More than one = or / in rule", p, description);
                haveEquals = TRUE;
                break;

            // if the character is a slash, make sure we haven't seen another slash
            // or an equals sign yet
            case '/':
                if (havePipe || haveEquals)
                    error("More than one = or / in rule", p, description);
                if (sawVarName)
                    error("Unknown variable name", p, description);
                havePipe = TRUE;
                break;

            // if the character is an exclamation point, make sure it occurs only
            // at the beginning of a rule
            case '!':
                if (lastC != ';' && lastC != '\u0000')
                    error("! can only occur at the beginning of a rule", p, description);
                break;

            // if the character is a backslash, skip the character that follows it
            // (it'll get treated as a literal character)
            case '\\':
                ++p;
                break;

            // we don't have to do anything special on a period
            case '.':
                break;

            // if the character is a syntax character that can only occur
            // inside [], make sure that it does in fact only occur inside [].
            case '^':
            case '-':
            case ':':
                if (lastOpen != '[' && lastOpen != '<')
                    error("Illegal character", p, description);
                break;

            // if the character is a semicolon, do the following...
            case ';':
                // make sure the rule contains something and that there are no
                // unbalanced parentheses or brackets
                if (lastC == ';' || lastC == '\u0000')
                    error("Empty rule", p, description);
                if (!parenStack.empty())
                    error("Unbalanced parenheses", p, description);

                if (parenStack.empty()) {
                    // if the rule contained an = sign, call processSubstitution()
                    // to replace the substitution name with the substitution text
                    // wherever it appears in the description
                    if (haveEquals)
                        description = processSubstitution(description.substring(ruleStart,
                                        p), description, p + 1);
                    else {
                        // otherwise, check to make sure the rule doesn't reference
                        // any undefined substitutions
                        if (sawVarName)
                            error("Unknown variable name", p, description);

                        // then add it to tempRuleList
                        tempRuleList.addElement(description.substring(ruleStart, p));
                    }
                    
                    // and reset everything to process the next rule
                    ruleStart = p + 1;
                    haveEquals = havePipe = sawVarName = FALSE;
                }
                break;

            // if the character is a vertical bar, check to make sure that it
            // occurs inside a () expression and that the character that precedes
            // it isn't also a vertical bar
            case '|':
                if (lastC == '|')
                    error("Empty alternative", p, description);
                if (parenStack.empty() || lastOpen != '(')
                    error("Misplaced |", p, description);
                break;

            // if the character is anything else (escaped characters are
            // skipped and don't make it here), it's an error
            default:
                if (c >= ' ' && c < '\u007f' && !Character.isLetter(c) &&
                                !Character.isDigit(c))
                    error("Illegal character", p, description);
                break;
        }
        lastC = c;
        ++p;
    }
    if (tempRuleList.size() == 0)
        error("No valid rules in description", p, description);
    return tempRuleList;
}

/**
 * This function performs variable-name substitutions.  First it does syntax
 * checking on the variable-name definition.  If it's syntactically valid, it
 * then goes through the remainder of the description and does a simple
 * find-and-replace of the variable name with its text.  (The variable text
 * must be enclosed in either [] or () for this to work.)
 */
UnicodeString RuleBasedBreakIteratorBuilder::processSubstitution(UnicodeString substitutionRule, UnicodeString description,
                        int32_t startPos) {
    // isolate out the text on either side of the equals sign
    UnicodeString replace;
    UnicodeString replaceWith;
    int32_t equalPos = substitutionRule.indexOf('=');
    replace = substitutionRule.substring(0, equalPos);
    replaceWith = substitutionRule.substring(equalPos + 1);

    // check to see whether the substitution name is something we've declared
    // to be "special".  For RuleBasedBreakIterator itself, this is "<ignore>".
    // This function takes care of any extra processing that has to be done
    // with "special" substitution names.
    handleSpecialSubstitution(replace, replaceWith, startPos, description);

    // perform various other syntax checks on the rule
    if (replaceWith.length() == 0)
        error("Nothing on right-hand side of =", startPos, description);
    if (replace.length() == 0)
        error("Nothing on left-hand side of =", startPos, description);
    if (replace.length() == 2 && replace.UCharAt(0) != '\\')
        error("Illegal left-hand side for =", startPos, description);
    if (replace.length() >= 3 && replace.UCharAt(0) != '<' && replace.UCharAt(equalPos - 1)
                    != '>')
        error("Illegal left-hand side for =", startPos, description);
    if (!(replaceWith.UCharAt(0) == '[' && replaceWith.UCharAt(replaceWith.length() - 1)
                    == ']') && !(replaceWith.UCharAt(0) == '(' && replaceWith.UCharAt(
                    replaceWith.length() - 1) == ')'))
        error("Illegal right-hand side for =", startPos, description);

    // now go through the rest of the description (which hasn't been broken up
    // into separate rules yet) and replace every occurrence of the
    // substitution name with the substitution body
    UnicodeString result = new UnicodeString();
    result.append(description.substring(0, startPos));
    int32_t lastPos = startPos;
    int32_t pos = description.indexOf(replace, startPos);
    while (pos != -1) {
        result.append(description.substring(lastPos, pos));
        result.append(replaceWith);
        lastPos = pos + replace.length();
        pos = description.indexOf(replace, lastPos);
    }
    result.append(description.substring(lastPos));
    return result.toString();
}

/**
 * This function defines a protocol for handling substitution names that
 * are "special," i.e., that have some property beyond just being
 * substitutions.  At the RuleBasedBreakIterator level, we have one
 * special substitution name, "<ignore>".  Subclasses can override this
 * function to add more.  Any special processing that has to go on beyond
 * that which is done by the normal substitution-processing code is done
 * here.
 */
void RuleBasedBreakIteratorBuilder::handleSpecialSubstitution(UnicodeString replace, UnicodeString replaceWith,
                    int32_t startPos, UnicodeString description) {
    // if we get a definition for a substitution called "ignore", it defines
    // the ignore characters for the iterator.  Check to make sure the expression
    // is a [] expression, and if it is, parse it and store the characters off
    // to the side.
    if (replace.equals("<ignore>")) {
        if (replaceWith.UCharAt(0) == '(')
            error("Ignore group can't be enclosed in (", startPos, description);
        ignoreChars = CharSet.parseString(replaceWith);
    }
}

/**
 * This function builds the character category table.  On entry,
 * tempRuleList is a vector of break rules that has had variable names substituted.
 * On exit, the charCategoryTable data member has been initialized to hold the
 * character category table, and tempRuleList's rules have been munged to contain
 * character category numbers everywhere a literal character or a [] expression
 * originally occurred.
 */
void RuleBasedBreakIteratorBuilder::buildCharCategories(Vector tempRuleList) {
    int32_t bracketLevel = 0;
    int32_t p = 0;
    int32_t lineNum = 0;

    // build hash table of every literal character or [] expression in the rule list
    // and use CharSet.parseString() to derive a CharSet object representing the
    // characters each refers to
    expressions = new Hashtable();
    while (lineNum < tempRuleList.size()) {
        UnicodeString line = (UnicodeString)(tempRuleList.elementAt(lineNum));
        p = 0;
        while (p < line.length()) {
            UChar c = line.UCharAt(p);
            switch (c) {
                // skip over all syntax characters except [
                case '{': case '}': case '(': case ')': case '*': case '.':
                case '/': case '|': case ';': case '?': case '!':
                    break;

                // for [, find the matching ] (taking nested [] pairs into account)
                // and add the whole expression to the expression list
                case '[':
                    int32_t q = p + 1;
                    ++bracketLevel;
                    while (q < line.length() && bracketLevel != 0) {
                        c = line.UCharAt(q);
                        if (c == '[')
                            ++bracketLevel;
                        else if (c == ']')
                            --bracketLevel;
                        ++q;
                    }
                    if (expressions.get(line.substring(p, q)) == 0) {
                        expressions.put(line.substring(p, q), CharSet.parseString(line.
                                        substring(p, q)));
                    }
                    p = q - 1;
                    break;

                // for \ sequences, just move to the next character and treat
                // it as a single character
                case '\\':
                    ++p;
                    c = line.UCharAt(p);
                    // DON'T break; fall through into "default" clause

                // for an isolated single character, add it to the expression list
                default:
                    expressions.put(line.substring(p, p + 1), CharSet.parseString(line.
                                    substring(p, p + 1)));
                    break;
            }
            ++p;
        }
        ++lineNum;
    }
    // dump CharSet's internal expression cache
    CharSet.releaseExpressionCache();

    // create the temporary category table (which is a vector of CharSet objects)
    categories = new Vector();
    if (ignoreChars != 0)
        categories.addElement(ignoreChars);
    else
        categories.addElement(new CharSet());
    ignoreChars = 0;

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
    Enumeration iter = expressions.elements();
    while (iter.hasMoreElements()) {
        // initialize the working char set to the chars in the current expression
        CharSet e = (CharSet)iter.nextElement();
        
        // for each category in the category list, do...
        for (int32_t j = categories.size() - 1; !e.empty() && j > 0; j--) {
            
            // if there's overlap between the current working set of chars
            // and the current category...
            CharSet that = (CharSet)(categories.elementAt(j));
            if (!that.intersection(e).empty()) {
                
                // add a new category for the characters that were in the
                // current category but not in the working char set
                CharSet temp = that.difference(e);
                if (!temp.empty())
                    categories.addElement(temp);
                    
                // remove those characters from the working char set and replace
                // the current category with the characters that it did
                // have in common with the current working char set
                temp = e.intersection(that);
                e = e.difference(that);
                if (!temp.equals(that))
                    categories.setElementAt(temp, j);
            }
        }
        
        // if there are still characters left in the working char set,
        // add a new category containing them
        if (!e.empty())
            categories.addElement(e);
    }

    // we have the ignore characters stored in position 0.  Make an extra pass through
    // the character category list and remove anything from the ignore list that shows
    // up in some other category
    CharSet allChars = new CharSet();
    for (int32_t i = 1; i < categories.size(); i++)
        allChars = allChars.union((CharSet)(categories.elementAt(i)));
    CharSet ignoreChars = (CharSet)(categories.elementAt(0));
    ignoreChars = ignoreChars.difference(allChars);
    categories.setElementAt(ignoreChars, 0);

    // now that we've derived the character categories, go back through the expression
    // list and replace each CharSet object with a String that represents the
    // character categories that expression refers to.  The String is encoded: each
    // character is a character category number (plus 0x100 to avoid confusing them
    // with syntax characters in the rule grammar)
    iter = expressions.keys();
    while (iter.hasMoreElements()) {
        UnicodeString key = (UnicodeString)iter.nextElement();
        CharSet cs = (CharSet)expressions.get(key);
        UnicodeString cats = new UnicodeString();
        
        // for each category...
        for (int32_t j = 0; j < categories.size(); j++) {
            
            // if the current expression contains characters in that category...
            CharSet temp = cs.intersection((CharSet)(categories.elementAt(j)));
            if (!temp.empty()) {
                
                // then add the encoded category number to the String for this
                // expression
                cats.append((UChar)(0x100 + j));
                if (temp.equals(cs))
                    break;
            }
        }
        
        // once we've finished building the encoded String for this expression,
        // replace the CharSet object with it
        expressions.put(key, cats.toString());
    }

    // and finally, we turn the temporary category table into a permanent category
    // table, which is a CompactByteArray. (we skip category 0, which by definition
    // refers to all characters not mentioned specifically in the rules)
    UCharCategoryTable = new CompactByteArray((int8_t)0);
    
    // for each category...
    for (int32_t i = 0; i < categories.size(); i++) {
        CharSet UChars = (CharSet)(categories.elementAt(i));
        
        // go through the character ranges in the category one by one...
        Enumeration enum = UChars.getChars();
        while (enum.hasMoreElements()) {
            UChar* range = (UChar*)(enum.nextElement());
            
            // and set the corresponding elements in the CompactArray accordingly
            if (i != 0)
                UCharCategoryTable.setElementAt(range[0], range[1], (int8_t)i);
                
            // (category 0 is special-- it's the hiding place for the ignore
            // characters, whose real category number in the CompactArray is
            // -1 [this is because category 0 contains all characters not
            // specifically mentioned anywhere in the rules] )
            else
                UCharCategoryTable.setElementAt(range[0], range[1], IGNORE);
        }
    }
    
    // once we've populated the CompactArray, compact it
    UCharCategoryTable.compact();
    // initialize numCategories
    numCategories = categories.size();
}

/**
 * This is the function that builds the forward state table.  Most of the real
 * work is done in parseRule(), which is called once for each rule in the
 * description.
 */
void RuleBasedBreakIteratorBuilder::buildStateTable(Vector tempRuleList) {
        // initialize our temporary state table, and fill it with two states:
        // state 0 is a dummy state that allows state 1 to be the starting state
        // and 0 to represent "stop".  State 1 is added here to seed things
        // before we start parsing
        tempStateTable = new Vector();
        tempStateTable.addElement(new int16_t[numCategories + 1]);
        tempStateTable.addElement(new int16_t[numCategories + 1]);
        
        // call parseRule() for every rule in the rule list (except those which
        // start with !, which are actually backwards-iteration rules)
        for (int32_t i = 0; i < tempRuleList.size(); i++) {
            UnicodeString rule = (UnicodeString)tempRuleList.elementAt(i);
            if (rule.UCharAt(0) != '!')
                parseRule(rule, TRUE);
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
void RuleBasedBreakIteratorBuilder::parseRule(UnicodeString rule, bool_t forward) {
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
    //   - It'd probably be better to use linked lists rather than Vector and Stack
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
    //   - The / token is not fully general: There are cases where it will put the
    //   break in the wrong place.  In particular, rule sets such as "?; cat/alog;"
    //   will put a break after "cat" instead of after "c" ANY time it sees "cat",
    //   regardless of whether the text matches "catalog" or not.  Also, rules such
    //   as "[a-z]*?abc;" will be treated the same as "[a-z]*?aa*bc;"-- that is,
    //   if the string ends in "aaaabc", the break will go before the first "a"
    //   rather than the last one.  Both of these are limitations in the design
    //   of RuleBasedBreakIterator and not limitations of the rule parser.

    int32_t p = 0;
    int32_t currentState = 1;   // don't use state number 0; 0 means "stop"
    int32_t lastState = currentState;
    UnicodeString pendingChars = "";

    decisionPointStack = new Stack();
    decisionPointList = new Vector();
    loopingStates = new Vector();
    statesToBackfill = new Vector();

    int16_t* state;
    bool_t sawEarlyBreak = FALSE;

    // if we're adding rules to the backward state table, mark the initial state
    // as a looping state
    if (!forward)
        loopingStates.addElement(new Integer(1));

    // put the current state on the decision point list before we start
    decisionPointList.addElement(new Integer(currentState)); // we want currentState to
                                                             // be 1 here...
    currentState = tempStateTable.size() - 1;   // but after that, we want it to be
                                                // 1 less than the state number of the next state
    while (p < rule.length()) {
        UChar c = rule.UCharAt(p);
        clearLoopingStates = FALSE;

        // this section handles literal characters, escaped character (which are
        // effectively literal characters too), the . token, and [] expressions
        if (c == '[' || c == '\\' || Character.isLetter(c) || Character.isDigit(c)
                        || c < ' ' || c == '.' || c >= '\u007f') {
            
            // if we're not on a period, isolate the expression and look up
            // the corresponding category list
            if (c != '.') {
                int32_t q = p;
                
                // if we're on a backslash, the expression is the character
                // after the backslash
                if (c == '\\') {
                    q = p + 2;
                    ++p;
                }
                
                // if we're on an opening bracket, scan to the closing bracket
                // to isolate the expression
                else if (c == '[') {
                    int32_t bracketLevel = 1;
                    while (bracketLevel > 0) {
                        ++q;
                        c = rule.UCharAt(q);
                        if (c == '[')
                            ++bracketLevel;
                        else if (c == ']')
                            --bracketLevel;
                        else if (c == '\\')
                            ++q;
                    }
                    ++q;
                }
                
                // otherwise, the expression is just the character itself
                else
                    q = p + 1;
                    
                // look up the category list for the expression and store it
                // in pendingChars
                pendingChars = (UnicodeString)expressions.get(rule.substring(p, q));
                
                // advance the current position past the expression
                p = q - 1;
            }
            
            // if the character we're on is a period, we end up down here
            else {
                int32_t rowNum = ((Integer)decisionPointList.lastElement()).intValue();
                state = (int16_t*)tempStateTable.elementAt(rowNum);
                
                // if the period is followed by an asterisk, then just set the current
                // state to loop back on itself
                if (p + 1 < rule.length() && rule.UCharAt(p + 1) == '*' && state[0] != 0) {
                    decisionPointList.addElement(new Integer(state[0]));
                    pendingChars = "";
                    ++p;
                }
                
                // otherwise, fabricate a category list ("pendingChars") with
                // every category in it
                else {
                    UnicodeString temp = new UnicodeString();
                    for (int32_t i = 0; i < numCategories; i++)
                        temp.append((UChar)(i + 0x100));
                    pendingChars = temp.toString();
                }
            }

            // we'll end up in here for all expressions except for .*, which is
            // special-cased above
            if (pendingChars.length() != 0) {
                
                // if the expression is followed by an asterisk, then push a copy
                // of the current desicion point list onto the stack (this is
                // the same thing we do on an opening brace)
                if (p + 1 < rule.length() && rule.UCharAt(p + 1) == '*')
                    decisionPointStack.push(decisionPointList.clone());
                    
                // create a new state, add it to the list of states to backfill
                // if we have looping states to worry about, set its "don't make
                // me an accepting state" flag if we've seen a slash, and add
                // it to the end of the state table
                int32_t newState = tempStateTable.size();
                if (loopingStates.size() != 0)
                    statesToBackfill.addElement(new Integer(newState));
                state = new int16_t[numCategories + 1];
                if (sawEarlyBreak)
                    state[numCategories] = 0x4000;
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
                    decisionPointList.addElement(new Integer(currentState));
                } while (currentState + 1 < tempStateTable.size());
            }
        }

        // a { marks the beginning of an optional run of characters.  Push a
        // copy of the current decision point list onto the stack.  This saves
        // it, preventing it from being affected by whatever's inside the parentheses.
        // This decision point list is restored when a } is encountered.
        else if (c == '{') {
            decisionPointStack.push(decisionPointList.clone());
        }

        // a } marks the end of an optional run of characters.  Pop the last decision
        // point list off the stack and merge it with the current decision point list.
        // a * denotes a repeating character or group (* after () is handled separately
        // below).  In addition to restoring the decision point list, modify the
        // current state to point to itself on the appropriate character categories.
        else if (c == '}' || c == '*') {
            // when there's a *, update the current state to loop back on itself
            // on the character categories that caused us to enter this state
            if (c == '*') {
                for (int32_t i = lastState + 1; i < tempStateTable.size(); i++) {
                    Vector temp = new Vector();
                    temp.addElement(new Integer(i));
                    updateStateTable(temp, pendingChars, (int16_t)(lastState + 1));
                }
            }
            
            // pop the top element off the decision point stack and merge
            // it with the current decision point list (this causes the divergent
            // paths through the state table to come together again on the next
            // new state)
            Vector temp = (Vector)decisionPointStack.pop();
            for (int32_t i = 0; i < decisionPointList.size(); i++)
                temp.addElement(decisionPointList.elementAt(i));
            decisionPointList = temp;
        }

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
        else if (c == '?') {
            setLoopingStates(decisionPointList, decisionPointList);
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
        else if (c == '(') {
            
            // add a new state to the state table to hold the entry points into
            // the () expression
            tempStateTable.addElement(new int16_t[numCategories + 1]);
            
            // we have to adjust lastState and currentState to account for the
            // new dummy state
            lastState = currentState;
            ++currentState;
            
            // add the current state to the decision point list (add it at the
            // BEGINNING so we can find it later)
            decisionPointList.insertElementAt(new Integer(currentState), 0);
            
            // finally, push a copy of the current decision point list onto the
            // stack (this keeps track of the active decision point list before
            // the () expression), followed by an empty decision point list
            // (this will hold the exit points)
            decisionPointStack.push(decisionPointList.clone());
            decisionPointStack.push(new Vector());
        }

        // a | separates alternative character sequences in a () expression.  When
        // a | is encountered, we add the current decision point list to the exit-point
        // list, and restore the decision point list to its state prior to the (.
        else if (c == '|') {
            
            // pick out the top two decision point lists on the stack
            Vector oneDown = (Vector)decisionPointStack.pop();
            Vector twoDown = (Vector)decisionPointStack.peek();
            decisionPointStack.push(oneDown);
            
            // append the current decision point list to the list below it
            // on the stack (the list of exit points), and restore the
            // current decision point list to its state before the () expression
            for (int32_t i = 0; i < decisionPointList.size(); i++)
                oneDown.addElement(decisionPointList.elementAt(i));
            decisionPointList = (Vector)twoDown.clone();
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
        else if (c == ')') {
            
            // pull the exit point list off the stack, merge it with the current
            // decision point list, and make the merged version the current
            // decision point list
            Vector exitPoints = (Vector)decisionPointStack.pop();
            for (int32_t i = 0; i < decisionPointList.size(); i++)
                exitPoints.addElement(decisionPointList.elementAt(i));
            decisionPointList = exitPoints;

            // if the ) isn't followed by a *, then all we have to do is throw
            // away the other list on the decision point stack, and we're done
            if (p + 1 >= rule.length() || rule.UCharAt(p + 1) != '*')
                decisionPointStack.pop();
                
            // but if the sequence repeats, we have a lot more work to do...
            else {
                
                // now exitPoints and decisionPointList have to point to equivalent
                // vectors, but not the SAME vector
                exitPoints = (Vector)decisionPointList.clone();
                
                // pop the original decision point list off the stack
                Vector temp = (Vector)decisionPointStack.pop();

                // we squirreled away the row number of our entry point list
                // at the beginning of the original decision point list.  Fish
                // that state number out and retrieve the entry point list
                int32_t tempStateNum = ((Integer)temp.firstElement()).intValue();
                int16_t* tempState = (int16_t*)tempStateTable.elementAt(tempStateNum);

                // merge the original decision point list with the current
                // decision point list
                for (int32_t i = 0; i < decisionPointList.size(); i++)
                    temp.addElement(decisionPointList.elementAt(i));
                decisionPointList = temp;

                // finally, copy every forward reference from the entry point
                // list into every state in the new decision point list
                for (int32_t i = 0; i < tempState.length; i++) {
                    if (tempState[i] > tempStateNum)
                        updateStateTable(exitPoints,
                                         new Character((UChar)(i + 0x100)).toString(),
                                         tempState[i]);
                }
                
                // update lastState and currentState, and throw away the *
                lastState = currentState;
                currentState = tempStateTable.size() - 1;
                ++p;
            }
        }

        // a / marks the position where the break is to go if the character sequence
        // matches this rule.  We update the flag word of every state on the decision
        // point list to mark them as ending states, and take note of the fact that 
        // we've seen the slash
        else if (c == '/') {
            sawEarlyBreak = TRUE;
            for (int32_t i = 0; i < decisionPointList.size(); i++) {
                state = (int16_t*)tempStateTable.elementAt(((Integer)decisionPointList.
                                elementAt(i)).intValue());
                state[numCategories] |= 0x8000;
            }
        }

        // if we get here without executing any of the above clauses, we have a
        // syntax error.  However, for now we just ignore the offending character
        // and move on

        // clearLoopingStates is a signal back from updateStateTable() that we've
        // transitioned to a state that won't loop back to the current looping
        // state.  (In other words, we've gotten to a point where we can no longer
        // go back into a *? we saw earlier.)  Clear out the list of looping states
        // and backfill any states that need to be backfilled.
        if (clearLoopingStates)
            setLoopingStates(0, decisionPointList);

        // advance to the next character, now that we've processed the current
        // character
        ++p;
    }

    // this takes care of backfilling any states that still need to be backfilled
    setLoopingStates(0, decisionPointList);

    // when we reach the end of the string, we do a postprocessing step to mark the
    // end states.  If we didn't see the / token, then the decision point list
    // contains every state that can transition to the end state-- that is, every
    // state that is the last state in a sequence that matches the rule.  All of
    // these states are considered "mark states"-- that is, states that cause the
    // position returned from next() to be updated.  A mark state represents a possible
    // break position.  This allows us to look ahead and remember how far the rule
    // matched before following the new branch (see next() for more information).
    // The temporary state table has an extra "flag column" at the end where this
    // information is stored.  We mark the end states by setting a flag in their
    // flag column.
    // (If we did see the /, we've already marked the end states.)
    if (!sawEarlyBreak) {
        for (int32_t i = 0; i < decisionPointList.size(); i++) {
            int32_t rowNum = ((Integer)decisionPointList.elementAt(i)).intValue();
            state = (int16_t*)tempStateTable.elementAt(rowNum);
            state[numCategories] |= 0x8000;
        }
    }
}

/**
 * Update entries in the state table, and merge states when necessary to keep
 * the table deterministic.
 * @param rows The list of rows that need updating (the decision point list)
 * @param pendingChars A character category list, encoded in a String.  This is the
 * list of the columns that need updating.
 * @param newValue Update the cells specfied above to contain this value
 */
void RuleBasedBreakIteratorBuilder::updateStateTable(Vector rows,
                                      UnicodeString pendingChars,
                                      int16_t newValue) {
    // create a dummy state that has the specified row number (newValue) in
    // the cells that need to be updated (those specified by pendingChars)
    // and 0 in the other cells
    int16_t* newValues = new int16_t[numCategories + 1];
    for (int32_t i = 0; i < pendingChars.length(); i++)
        newValues[(int32_t)(pendingChars.UCharAt(i)) - 0x100] = newValue;

    // go through the list of rows to update, and update them by calling
    // mergeStates() to merge them the the dummy state we created
    for (int32_t i = 0; i < rows.size(); i++) {
        mergeStates(((Integer)rows.elementAt(i)).intValue(), newValues, rows);
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
void RuleBasedBreakIteratorBuilder::mergeStates(int32_t rowNum,
                                 int16_t* newValues,
                                 Vector rowsBeingUpdated) {
    int16_t* oldValues = (int16_t*)(tempStateTable.elementAt(rowNum));
    bool_t isLoopingState = loopingStates.contains(new Integer(rowNum));

    // for each of the cells in the rows we're reconciling, do...
    for (int32_t i = 0; i < oldValues.length; i++) {
        
        // if they contain the same value, we don't have to do anything
        if (oldValues[i] == newValues[i])
            continue;
            
        // if oldValues is a looping state and the state the current cell points to
        // is too, then we can just stomp over the current value of that cell (and
        // set the clear-looping-states flag if necessaru)
        else if (isLoopingState && loopingStates.contains(new Integer(oldValues[i]))) {
            if (newValues[i] != 0) {
                if (oldValues[i] == 0)
                    clearLoopingStates = TRUE;
                oldValues[i] = newValues[i];
            }
        }
        
        // if the current cell in oldValues is 0, copy in the corresponding value
        // from newValues
        else if (oldValues[i] == 0)
            oldValues[i] = newValues[i];
            
        // the last column of each row is the flag column.  Take care to merge the
        // flag words correctly
        else if (i == numCategories) {
            oldValues[i] = (int16_t)((newValues[i] & 0xc000) | oldValues[i]);
        }
        
        // if both newValues and oldValues have a nonzero value in the current
        // cell, and it isn't the same value both places...
        else if (oldValues[i] != 0 && newValues[i] != 0) {
            
            // look up this pair of cell values in the merge list.  If it's
            // found, update the cell in oldValues to point to the merged state
            int32_t combinedRowNum = searchMergeList(oldValues[i], newValues[i]);
            if (combinedRowNum != 0)
                oldValues[i] = (int16_t)combinedRowNum;
                
            // otherwise, we have to reconcile them...
            else {
                // copy our row numbers into variables to make things easier
                int32_t oldRowNum = oldValues[i];
                int32_t newRowNum = newValues[i];
                combinedRowNum = tempStateTable.size();
                
                // add this pair of row numbers to the merge list (create it first
                // if we haven't created the merge list yet)
                if (mergeList == 0)
                    mergeList = new Vector();
                mergeList.addElement(new int32_t* { oldRowNum, newRowNum, combinedRowNum });

                // create a new row to represent the merged state, and copy the
                // contents of oldRow into it, then add it to the end of the
                // state table and update the original row (oldValues) to point
                // to the new, merged, state
                int16_t* newRow = new int16_t[numCategories + 1];
                int16_t* oldRow = (int16_t*)(tempStateTable.elementAt(oldRowNum));
                System.arraycopy(oldRow, 0, newRow, 0, numCategories + 1);
                tempStateTable.addElement(newRow);
                oldValues[i] = (int16_t)combinedRowNum;

                // if the decision point list contains either of the parent rows,
                // update it to include the new row as well
                if ((decisionPointList.contains(new Integer(oldRowNum)) ||
                                decisionPointList.contains(new Integer(newRowNum))) &&
                                !decisionPointList.contains(new Integer(combinedRowNum)))
                    decisionPointList.addElement(new Integer(combinedRowNum));
                    
                // do the same thing with the list of rows being updated
                if ((rowsBeingUpdated.contains(new Integer(oldRowNum)) ||
                                rowsBeingUpdated.contains(new Integer(newRowNum))) &&
                                !rowsBeingUpdated.contains(new Integer(combinedRowNum)))
                    decisionPointList.addElement(new Integer(combinedRowNum));
                // now (groan) do the same thing for all the entries on the
                // decision point stack
                for (int32_t k = 0; k < decisionPointStack.size(); k++) {
                    Vector dpl = (Vector)decisionPointStack.elementAt(k);
                    if ((dpl.contains(new Integer(oldRowNum)) ||
                                    dpl.contains(new Integer(newRowNum))) && !dpl.contains(
                                    new Integer(combinedRowNum)))
                        dpl.addElement(new Integer(combinedRowNum));
                }

                // FINALLY (puff puff puff), call mergeStates() recursively to copy
                // the row referred to by newValues into the new row and resolve any
                // conflicts that come up at that level
                mergeStates(combinedRowNum, (int16_t*)(tempStateTable.elementAt(
                                newValues[i])), rowsBeingUpdated);
            }
        }
    }
    return;
}

/**
 * The merge list is a list of pairs of rows that have been merged somewhere in
 * the process of building this state table, along with the row number of the
 * row containing the merged state.  This function looks up a pair of row numbers
 * and returns the row number of the row they combine into.  (It returns 0 if
 * this pair of rows isn't in the merge list.)
 */
int32_t RuleBasedBreakIteratorBuilder::searchMergeList(int32_t a, int32_t b) {
    // if there is no merge list, there obviously isn't anything in it
    if (mergeList == 0)
        return 0;
        
    // otherwise, for each element in the merge list...
    else {
        int32_t* entry;
        for (int32_t i = 0; i < mergeList.size(); i++) {
            entry = (int32_t*)(mergeList.elementAt(i));
            
            // we have a hit if the two row numbers match the two row numbers
            // in the beginning of the entry (the two that combine), in either
            // order
            if ((entry[0] == a && entry[1] == b) || (entry[0] == b && entry[1] == a))
                return entry[2];
                
            // we also have a hit if one of the two row numbers matches the marged
            // row number and the other one matches one of the original row numbers
            if ((entry[2] == a && (entry[0] == b || entry[1] == b)))
                return entry[2];
            if ((entry[2] == b && (entry[0] == a || entry[1] == a)))
                return entry[2];
        }
        return 0;
    }
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
void RuleBasedBreakIteratorBuilder::setLoopingStates(Vector newLoopingStates, Vector endStates) {
    
    // if the current list of looping states isn't empty, we have to backfill
    // values from the looping states into the states that are waiting to be
    // backfilled
    if (!loopingStates.isEmpty()) {
        int32_t loopingState = ((Integer)loopingStates.lastElement()).intValue();
        int32_t rowNum;

        // don't backfill into an end state OR any state reachable from an end state
        // (since the search for reachable states is recursive, it's split out into
        // a separate function, eliminateBackfillStates(), below)
        for (int32_t i = 0; i < endStates.size(); i++) {
            eliminateBackfillStates(((Integer)endStates.elementAt(i)).intValue());
        }

        // we DON'T actually backfill the states that need to be backfilled here.
        // Instead, we MARK them for backfilling.  The reason for this is that if
        // there are multiple rules in the state-table description, the looping
        // states may have some of their values changed by a succeeding rule, and
        // this wouldn't be reflected in the backfilled states.  We mark a state
        // for backfilling by putting the row number of the state to copy from
        // into the flag cell at the end of the row
        for (int32_t i = 0; i < statesToBackfill.size(); i++) {
            rowNum = ((Integer)statesToBackfill.elementAt(i)).intValue();
            int16_t* state = (int16_t*)tempStateTable.elementAt(rowNum);
            state[numCategories] = (int16_t)((state[numCategories] & 0xc000) |
                            loopingState);
        }
        statesToBackfill.removeAllElements();
        loopingStates.removeAllElements();
    }

    if (newLoopingStates != 0)
        loopingStates = (Vector)newLoopingStates.clone();
}

/**
 * This removes "ending states" and states reachable from them from the
 * list of states to backfill.
 * @param The row number of the state to remove from the backfill list
 */
void RuleBasedBreakIteratorBuilder::eliminateBackfillStates(int32_t baseState) {
    
    // don't do anything unless this state is actually in the backfill list...
    if (statesToBackfill.contains(new Integer(baseState))) {
        
        // if it is, take it out
        statesToBackfill.removeElement(new Integer(baseState));
        
        // then go through and recursively call this function for every
        // state that the base state points to
        int16_t* state = (int16_t*)tempStateTable.elementAt(baseState);
        for (int32_t i = 0; i < numCategories; i++) {
            if (state[i] != 0)
                eliminateBackfillStates(state[i]);
        }
    }
}

/**
 * This function completes the backfilling process by actually doing the
 * backfilling on the states that are marked for it
 */
void RuleBasedBreakIteratorBuilder::backfillLoopingStates() {
    int16_t* state;
    int16_t* loopingState = 0;
    int32_t loopingStateRowNum = 0;
    int32_t fromState;

    // for each state in the state table...
    for (int32_t i = 0; i < tempStateTable.size(); i++) {
        state = (int16_t*)tempStateTable.elementAt(i);
        
        // check the state's flag word to see if it's marked for backfilling
        // (it's marked for backfilling if any bits other than the two high-order
        // bits are set-- if they are, then the flag word, minus the two high bits,
        // is the row number to copy from)
        fromState = state[numCategories] & 0x3fff;
        if (fromState > 0) {
            
            // load up the state to copy from (if we haven't already)
            if (fromState != loopingStateRowNum) {
                loopingStateRowNum = fromState;
                loopingState = (int16_t*)tempStateTable.elementAt(loopingStateRowNum);
            }
            
            // clear out the backfill part of the flag word
            state[numCategories] &= 0xc000;
            
            // then fill all zero cells in the current state with values
            // from the corresponding cells of the fromState
            for (int32_t j = 0; j < state.length; j++) {
                if (state[j] == 0)
                    state[j] = loopingState[j];
                else if (state[j] == 0x4000)
                    state[j] = 0;
            }
        }
    }
}

/**
 * This function completes the state-table-building process by doing several
 * postprocessing steps and copying everything into its final resting place
 * in the iterator itself
 * @param forward True if we're working on the forward state table
 */
void RuleBasedBreakIteratorBuilder::finishBuildingStateTable(bool_t forward) {
    // start by backfilling the looping states
    backfillLoopingStates();

    int32_t* rowNumMap = new int32_t[tempStateTable.size()];
    Stack rowsToFollow = new Stack();
    rowsToFollow.push(new Integer(1));
    rowNumMap[1] = 1;

    // determine which states are no longer reachable from the start state
    // (the reachable states will have their row numbers in the row number
    // map, and the nonreachable states will have zero in the row number map)
    while (rowsToFollow.size() != 0) {
        int32_t rowNum = ((Integer)rowsToFollow.pop()).intValue();
        int16_t* row = (int16_t*)(tempStateTable.elementAt(rowNum));

        for (int32_t i = 0; i < numCategories; i++) {
            if (row[i] != 0) {
                if (rowNumMap[row[i]] == 0) {
                    rowNumMap[row[i]] = row[i];
                    rowsToFollow.push(new Integer(row[i]));
                }
            }
        }
    }

    bool_t madeChange;
    int32_t newRowNum;

    // algorithm for minimizing the number of states in the table adapted from
    // Aho & Ullman, "Principles of Compiler Design"
    // The basic idea here is to organize the states into classes.  When we're done,
    // all states in the same class can be considered identical and all but one eliminated.

    // initially assign states to classes based on the number of populated cells they
    // contain (the class number is the number of populated cells)
    int32_t* stateClasses = new int32_t[tempStateTable.size()];
    int32_t nextClass = numCategories + 1;
    int16_t* state1, state2;
    for (int32_t i = 1; i < stateClasses.length; i++) {
        if (rowNumMap[i] == 0)
            continue;
        state1 = (int16_t*)tempStateTable.elementAt(i);
        for (int32_t j = 0; j < numCategories; j++)
            if (state1[j] != 0)
                ++stateClasses[i];
        if (stateClasses[i] == 0)
            stateClasses[i] = nextClass;
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
    bool_t split;

    do {
        currentClass = 1;
        lastClass = nextClass;
        while (currentClass < nextClass) {
            split = FALSE;
            state1 = state2 = 0;
            for (int32_t i = 0; i < stateClasses.length; i++) {
                if (stateClasses[i] == currentClass) {
                    if (state1 == 0) {
                        state1 = (int16_t*)tempStateTable.elementAt(i);
                    }
                    else {
                        state2 = (int16_t*)tempStateTable.elementAt(i);
                        for (int32_t j = 0; j < state2.length; j++)
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
            if (split)
                ++nextClass;
            ++currentClass;
        }
    } while (lastClass != nextClass);

    // at this point, all of the states in a class except the first one (the
    //"representative") can be eliminated, so update the row-number map accordingly
    int32_t* representatives = new int32_t[nextClass];
    for (int32_t i = 1; i < stateClasses.length; i++)
        if (representatives[stateClasses[i]] == 0)
            representatives[stateClasses[i]] = i;
        else
            rowNumMap[i] = representatives[stateClasses[i]];

    // renumber all remaining rows...
    // first drop all that are either unreferenced or not a class representative
    for (int32_t i = 1; i < rowNumMap.length; i++)
        if (rowNumMap[i] != i)
            tempStateTable.setElementAt(0, i);
    
    // then calculate everybody's new row number and update the row
    // number map appropriately (the first pass updates the row numbers
    // of all the class representatives [the rows we're keeping], and the
    // second pass updates the cross references for all the rows that
    // are being deleted)
    newRowNum = 1;
    for (int32_t i = 1; i < rowNumMap.length; i++)
        if (tempStateTable.elementAt(i) != 0)
            rowNumMap[i] = newRowNum++;
    for (int32_t i = 1; i < rowNumMap.length; i++)
        if (tempStateTable.elementAt(i) == 0)
            rowNumMap[i] = rowNumMap[rowNumMap[i]];

    // allocate the permanent state table, and copy the remaining rows into it
    // (adjusting all the cell values, of course)
    
    // this section does that for the forward state table
    if (forward) {
        endStates = new bool_t[newRowNum];
        stateTable = new int16_t[newRowNum * numCategories];
        int32_t p = 0;
        int32_t p2 = 0;
        for (int32_t i = 0; i < tempStateTable.size(); i++) {
            int16_t* row = (int16_t*)(tempStateTable.elementAt(i));
            if (row == 0)
                continue;
            for (int32_t j = 0; j < numCategories; j++) {
                stateTable[p] = (int16_t)(rowNumMap[row[j]]);
                ++p;
            }
            endStates[p2++] = ((row[numCategories] & 0x8000) != 0);
        }
    }
    
    // and this section does it for the backward state table
    else {
        backwardsStateTable = new int16_t[newRowNum * numCategories];
        int32_t p = 0;
        for (int32_t i = 0; i < tempStateTable.size(); i++) {
            int16_t* row = (int16_t*)(tempStateTable.elementAt(i));
            if (row == 0)
                continue;
            for (int32_t j = 0; j < numCategories; j++) {
                backwardsStateTable[p] = (int16_t)(rowNumMap[row[j]]);
                ++p;
            }
        }
    }
}

/**
 * This function builds the backward state table from the forward state
 * table and any additional rules (identified by the ! on the front)
 * supplied in the description
 */
void RuleBasedBreakIteratorBuilder::buildBackwardsStateTable(Vector tempRuleList) {
    
    // create the temporary state table and seed it with two rows (row 0
    // isn't used for anything, and we have to create row 1 (the initial
    // state) before we can do anything else
    tempStateTable = new Vector();
    tempStateTable.addElement(new int16_t[numCategories + 1]);
    tempStateTable.addElement(new int16_t[numCategories + 1]);

    // although the backwards state table is built automatically from the forward
    // state table, there are some situations (the default sentence-break rules,
    // for example) where this doesn't yield enough stop states, causing a dramatic
    // drop in performance.  To help with these cases, the user may supply
    // supplemental rules that are added to the backward state table.  These have
    // the same syntax as the normal break rules, but begin with '!' to distinguish
    // them from normal break rules
    for (int32_t i = 0; i < tempRuleList.size(); i++) {
        UnicodeString rule = (UnicodeString)tempRuleList.elementAt(i);
        if (rule.UCharAt(0) == '!') {
            parseRule(rule.substring(1), FALSE);
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
    if (backTableOffset > 2)
        ++backTableOffset;

    // the automatically-generated part of the table models a two-dimensional
    // array where the two dimensions represent the two characters we're currently
    // looking at.  To model this as a state table, we actually need one additional
    // row to represent the initial state.  It gets populated with the row numbers
    // of the other rows (in order).
    for (int32_t i = 0; i < numCategories + 1; i++)
        tempStateTable.addElement(new int16_t[numCategories + 1]);
    int16_t* state = (int16_t*)tempStateTable.elementAt(backTableOffset - 1);
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
    int32_t numRows = stateTable.length / numCategories;
    for (int32_t column = 0; column < numCategories; column++) {
        for (int32_t row = 0; row < numRows; row++) {
            int32_t nextRow = lookupState(row, column);
            if (nextRow != 0) {
                for (int32_t nextColumn = 0; nextColumn < numCategories; nextColumn++) {
                    int32_t cellValue = lookupState(nextRow, nextColumn);
                    if (cellValue != 0) {
                        state = (int16_t*)tempStateTable.elementAt(nextColumn +
                                        backTableOffset);
                        state[column] = (int16_t)(column + backTableOffset);
                    }
                }
            }
        }
    }

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
        state = (int16_t*)tempStateTable.elementAt(1);
        for (int32_t i = backTableOffset - 1; i < tempStateTable.size(); i++) {
            int16_t* state2 = (int16_t*)tempStateTable.elementAt(i);
            for (int32_t j = 0; j < numCategories; j++) {
                if (state[j] != 0 && state2[j] != 0)
                    state2[j] = state[j];
            }
        }
        
        // now, for every row in the rule-based sub-table that is not
        // an end state, fill in all unpopulated cells with the values
        // of the corresponding cells in the first row of the auto-
        // generated sub-table.
        state = (int16_t*)tempStateTable.elementAt(backTableOffset - 1);
        for (int32_t i = 1; i < backTableOffset - 1; i++) {
            int16_t* state2 = (int16_t*)tempStateTable.elementAt(i);
            if ((state2[numCategories] & 0x8000) == 0) {
                for (int32_t j = 0; j < numCategories; j++) {
                    if (state2[j] == 0)
                        state2[j] = state[j];
                }
            }
        }
    }

    // finally, clean everything up and copy it into the actual BreakIterator
    // by calling finishBuildingStateTable()
    finishBuildingStateTable(FALSE);
}

/**
 * Throws an IllegalArgumentException representing a syntax error in the rule
 * description.  The exception's message contains some debugging information.
 * @param message A message describing the problem
 * @param position The position in the description where the problem was
 * discovered
 * @param context The string containing the error
 */
void RuleBasedBreakIteratorBuilder::error(UnicodeString message, int32_t position, UnicodeString context) {
    throw new IllegalArgumentException("Parse error: " + message + " at " + position
                    + " in " + context);
}
