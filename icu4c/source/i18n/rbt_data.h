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
struct UHashtable;

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

    /**
     * Rule table.  May be empty.
     *
     * PUBLIC DATA MEMBER for internal use by RBT
     */
    TransliterationRuleSet ruleSet;

    /**
     * Map variable name (UnicodeString) to variable (Character).
     * A variable name may correspond to a single literal
     * character, in which case the character is stored in this
     * hash.  It may also correspond to a UnicodeSet, in which
     * case a character is again stored in this hash, but the
     * character is a stand-in: it is a key for a secondary lookup
     * in data.setVariables.  The stand-in also represents the
     * UnicodeSet in the stored rules.
     *
     * PUBLIC DATA MEMBER for internal use by RBT
     */
    UHashtable* variableNames;
    
    /**
     * Map category variable (Character) to set (UnicodeSet).
     * Variables that correspond to a set of characters are mapped
     * from variable name to a stand-in character in data.variableNames.
     * The stand-in then serves as a key in this hash to lookup the
     * actual UnicodeSet object.  In addition, the stand-in is
     * stored in the rule text to represent the set of characters.
     * setVariables[i] represents character (setVariablesBase + i).
     *
     * PUBLIC DATA MEMBER for internal use by RBT
     */
    UnicodeSet** setVariables;
    
    /**
     * The character represented by setVariables[0].
     */
    UChar setVariablesBase;

    /**
     * The length of setVariables.
     */
    int32_t setVariablesLength;

    TransliterationRuleData(UErrorCode& status);

    ~TransliterationRuleData();
    
    void defineVariable(const UnicodeString& name,
                        UChar value,
                        UErrorCode& status);
        
    UChar lookupVariable(const UnicodeString& name,
                         UErrorCode& status) const;
    
	const UnicodeSet* lookupSet(UChar standIn) const;

    bool_t isVariableDefined(const UnicodeString& name) const;
};

#endif
