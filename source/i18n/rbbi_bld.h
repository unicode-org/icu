/*
**********************************************************************
*   Copyright (C) 1999 Alan Liu and others. All rights reserved.
**********************************************************************
*   Date        Name        Description
*   10/22/99    alan        Creation.  This is an internal header; it
*                           shall not be exported.
**********************************************************************
*/

#ifndef RBBI_BLD_H
#define RBBI_BLD_H

#include "rbbi.h"
#include "uniset.h"
#include "uvector.h"

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
class RuleBasedBreakIteratorBuilder {

protected:

    /**
     * A temporary holding place used for calculating the character categories.
     * This object contains UnicodeSet objects.
     */
    UVector categories;

    /**
     * A table used to map parts of regexp text to lists of character categories,
     * rather than having to figure them out from scratch each time
     */
    Hashtable expressions;

    /**
     * A temporary holding place for the list of ignore characters
     */
    UnicodeSet ignoreChars;

    /**
     * A temporary holding place where the forward state table is built
     */
    UVector tempStateTable;

    /**
     * A list of all the states that have to be filled in with transitions to the
     * next state that is created.  Used when building the state table from the
     * regular expressions.
     */
    UVector decisionPointList;

    /**
     * A UStack for holding decision point lists.  This is used to handle nested
     * parentheses and braces in regexps.
     */
    UStack decisionPointStack;

    /**
     * A list of states that loop back on themselves.  Used to handle .*?
     */
    UVector loopingStates;

    /**
     * Looping states actually have to be backfilled later in the process
     * than everything else.  This is where a the list of states to backfill
     * is accumulated.  This is also used to handle .*?
     */
    UVector statesToBackfill;

    /**
     * A list mapping pairs of state numbers for states that are to be combined
     * to the state number of the state representing their combination.  Used
     * in the process of making the state table deterministic to prevent
     * infinite recursion.
     */
    UVector mergeList;

    /**
     * A flag that is used to indicate when the list of looping states can
     * be reset.
     */
    bool_t clearLoopingStates;

public:

    /**
     * No special construction is required for the Builder.
     */
    RuleBasedBreakIteratorBuilder();

    /**
     * This is the main function for setting up the BreakIterator's tables.  It
     * just UVectors different parts of the job off to other functions.
     */
    virtual void buildBreakIterator();

private:

    /**
     * Thus function has three main purposes:
     * <ul><li>Perform general syntax checking on the description, so the rest of the
     * build code can assume that it's parsing a legal description.
     * <li>Split the description into separate rules
     * <li>Perform variable-name substitutions (so that no one else sees variable names)
     * </ul>
     */
    virtual UVector buildRuleList(UnicodeString description);

protected:

    /**
     * This function performs variable-name substitutions.  First it does syntax
     * checking on the variable-name definition.  If it's syntactically valid, it
     * then goes through the remainder of the description and does a simple
     * find-and-replace of the variable name with its text.  (The variable text
     * must be enclosed in either [] or () for this to work.)
     */
    virtual UnicodeString processSubstitution(UnicodeString substitutionRule, UnicodeString description,
                    int32_t startPos);

    /**
     * This function defines a protocol for handling substitution names that
     * are "special," i.e., that have some property beyond just being
     * substitutions.  At the RuleBasedBreakIterator level, we have one
     * special substitution name, "<ignore>".  Subclasses can override this
     * function to add more.  Any special processing that has to go on beyond
     * that which is done by the normal substitution-processing code is done
     * here.
     */
    virtual void handleSpecialSubstitution(UnicodeString replace, UnicodeString replaceWith,
                int32_t startPos, UnicodeString description);

    /**
     * This function builds the character category table.  On entry,
     * tempRuleList is a UVector of break rules that has had variable names substituted.
     * On exit, the charCategoryTable data member has been initialized to hold the
     * character category table, and tempRuleList's rules have been munged to contain
     * character category numbers everywhere a literal character or a [] expression
     * originally occurred.
     */
    virtual void buildCharCategories(UVector tempRuleList);

private:

    /**
     * This is the function that builds the forward state table.  Most of the real
     * work is done in parseRule(), which is called once for each rule in the
     * description.
     */
    virtual void buildStateTable(UVector tempRuleList);

    /**
     * This is where most of the work really happens.  This routine parses a single
     * rule in the rule description, adding and modifying states in the state
     * table according to the new expression.  The state table is kept deterministic
     * throughout the whole operation, although some ugly postprocessing is needed
     * to handle the *? token.
     */
    virtual void parseRule(UnicodeString rule, bool_t forward);

    /**
     * Update entries in the state table, and merge states when necessary to keep
     * the table deterministic.
     * @param rows The list of rows that need updating (the decision point list)
     * @param pendingChars A character category list, encoded in a String.  This is the
     * list of the columns that need updating.
     * @param newValue Update the cells specfied above to contain this value
     */
    virtual void updateStateTable(UVector rows,
                                  UnicodeString pendingChars,
                                  int16_t newValue);

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
    virtual void mergeStates(int32_t rowNum,
                             int16_t* newValues,
                             UVector rowsBeingUpdated);

    /**
     * The merge list is a list of pairs of rows that have been merged somewhere in
     * the process of building this state table, along with the row number of the
     * row containing the merged state.  This function looks up a pair of row numbers
     * and returns the row number of the row they combine into.  (It returns 0 if
     * this pair of rows isn't in the merge list.)
     */
    virtual int32_t searchMergeList(int32_t a, int32_t b);

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
    virtual void setLoopingStates(UVector newLoopingStates, UVector endStates);

    /**
     * This removes "ending states" and states reachable from them from the
     * list of states to backfill.
     * @param The row number of the state to remove from the backfill list
     */
    virtual void eliminateBackfillStates(int32_t baseState);

    /**
     * This function completes the backfilling process by actually doing the
     * backfilling on the states that are marked for it
     */
    virtual void backfillLoopingStates();

    /**
     * This function completes the state-table-building process by doing several
     * postprocessing steps and copying everything into its final resting place
     * in the iterator itself
     * @param forward True if we're working on the forward state table
     */
    virtual void finishBuildingStateTable(bool_t forward);

    /**
     * This function builds the backward state table from the forward state
     * table and any additional rules (identified by the ! on the front)
     * supplied in the description
     */
    virtual void buildBackwardsStateTable(UVector tempRuleList);

protected:

    /**
     * Throws an IllegalArgumentException representing a syntax error in the rule
     * description.  The exception's message contains some debugging information.
     * @param message A message describing the problem
     * @param position The position in the description where the problem was
     * discovered
     * @param context The string containing the error
     */
    virtual void error(UnicodeString message, int32_t position, UnicodeString context);
};

#endif
