/*
* Copyright © {1999}, International Business Machines Corporation and others. All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   11/17/99    aliu        Creation.
**********************************************************************
*/
#ifndef RBT_DATA_H
#define RBT_DATA_H

#include "rbt_set.h"

class UnicodeString;
class UnicodeSet;
class Hashtable;

/**
 * The rule data for a RuleBasedTransliterators.  RBT objects hold
 * a const pointer to a TRD object that they do not own.  TRD objects
 * are essentially the parsed rules in compact, usable form.  The
 * TRD objects themselves are held for the life of the process in
 * a static cache owned by Transliterator.
 *
 * This class' API is a little asymmetric.  There is a method to
 * define a variable, but no way to define a set.  This is because the
 * sets are defined by the parser in a UVector, and the vector is
 * copied into a fixed-size array here.  Once this is done, no new
 * sets may be defined.  In practice, there is no need to do so, since
 * generating the data and using it are discrete phases.  When there
 * is a need to access the set data during the parse phase, another
 * data structure handles this.  See the parsing code for more
 * details.
 */
class TransliterationRuleData {

public:

    // PUBLIC DATA MEMBERS

    /**
     * Rule table.  May be empty.
     */
    TransliterationRuleSet ruleSet;

    /**
     * Map variable name (String) to variable (UnicodeString).  A variable name
     * corresponds to zero or more characters, stored in a UnicodeString in
     * this hash.  One or more of these chars may also correspond to a
     * UnicodeSet, in which case the character in the UnicodeString in this hash is
     * a stand-in: it is an index for a secondary lookup in
     * data.setVariables.  The stand-in also represents the UnicodeSet in
     * the stored rules.
     */
    Hashtable* variableNames;
    
    /**
     * Map category variable (UChar) to set (UnicodeSet).
     * Variables that correspond to a set of characters are mapped
     * from variable name to a stand-in character in data.variableNames.
     * The stand-in then serves as a key in this hash to lookup the
     * actual UnicodeSet object.  In addition, the stand-in is
     * stored in the rule text to represent the set of characters.
     * setVariables[i] represents character (setVariablesBase + i).
     */
    UnicodeSet** setVariables;
    
    /**
     * The character that represents setVariables[0].  Characters
     * setVariablesBase through setVariablesBase +
     * setVariables.length - 1 represent UnicodeSet objects.
     */
    UChar setVariablesBase;

    /**
     * The length of setVariables.
     */
    int32_t setVariablesLength;

    /**
     * The character that represents segment 1.  Characters segmentBase
     * through segmentBase + 8 represent segments 1 through 9.
     */
    UChar segmentBase;

public:

    TransliterationRuleData(UErrorCode& status);

    ~TransliterationRuleData();
    
	const UnicodeSet* lookupSet(UChar standIn) const;

    /**
     * Return the zero-based index of the segment represented by the given
     * character, or -1 if none.  Repeat: This is a zero-based return value,
     * 0..8, even though these are notated "$1".."$9".
     */
    int32_t lookupSegmentReference(UChar c) const;

    /**
     * Return the character used to stand for the given segment reference.
     * The reference must be in the range 1..9.
     */
    UChar getSegmentStandin(int32_t ref) const {
        return segmentBase + ref - 1;
    }
};

#endif
