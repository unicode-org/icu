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
     * Map category variable (UChar) to set (UnicodeSet).
     * Variables that correspond to a set of characters are mapped
     * from variable name to a stand-in character in
     * data.variableNames.  The stand-in then serves as a key in
     * this hash to lookup the actual UnicodeSet object.  In
     * addition, the stand-in is stored in the rule text to
     * represent the set of characters.
     *
     * PUBLIC DATA MEMBER for internal use by RBT
     */
    UHashtable* setVariables;
    
    TransliterationRuleData(UErrorCode& status);

    ~TransliterationRuleData();
    
    void defineVariable(const UnicodeString& name,
                        UChar value,
                        UErrorCode& status);
        
    void defineVariable(const UnicodeString& name,
                        UChar standIn,
                        UnicodeSet* adoptedSet,
                        UErrorCode& status);

    void defineSet(UChar standIn,
                   UnicodeSet* adoptedSet,
                   UErrorCode& status);

    UChar lookupVariable(const UnicodeString& name,
                         UErrorCode& status) const;
    
	UnicodeSet* lookupSet(UChar standIn) const;

    bool_t isVariableDefined(const UnicodeString& name) const;
};

#endif
