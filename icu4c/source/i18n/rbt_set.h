/*
* Copyright (C) {1999}, International Business Machines Corporation and others. All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   11/17/99    aliu        Creation.
**********************************************************************
*/
#ifndef RBT_SET_H
#define RBT_SET_H

#include "uvector.h"
#include "unicode/utrans.h"

class Replaceable;
class TransliterationRule;
class TransliterationRuleData;
class UnicodeFilter;
class UnicodeString;

/**
 * A set of rules for a <code>RuleBasedTransliterator</code>.
 * @author Alan Liu
 */
class TransliterationRuleSet {
    /**
     * Vector of rules, in the order added.  This is only used while the rule
     * set is getting built.  After that, freeze() reorders and indexes the
     * rules into rules[].  However, the vector is kept until destruction.
     */
    UVector* ruleVector;

    /**
     * Length of the longest preceding context
     */
    int32_t maxContextLength;

    /**
     * Sorted and indexed table of rules.  This is created by freeze() from
     * the rules in ruleVector.
     */
    TransliterationRule** rules;

    /**
     * Index table.  For text having a first character c, compute x = c&0xFF.
     * Now use rules[index[x]..index[x+1]-1].  This index table is created by
     * freeze().
     */
    int32_t index[257];

public:

    /**
     * Construct a new empty rule set.
     */
    TransliterationRuleSet(UErrorCode& status);

    /**
     * Copy constructor.
     */
    TransliterationRuleSet(const TransliterationRuleSet&);

    /**
     * Destructor.
     */
    virtual ~TransliterationRuleSet();

    /**
     * Return the maximum context length.
     * @return the length of the longest preceding context.
     */
    virtual int32_t getMaximumContextLength(void) const;

    /**
     * Add a rule to this set.  Rules are added in order, and order is
     * significant.  The last call to this method must be followed by
     * a call to <code>freeze()</code> before the rule set is used.
     *
     * @param adoptedRule the rule to add
     */
    virtual void addRule(TransliterationRule* adoptedRule,
                         UErrorCode& status);

    /**
     * Check this for masked rules and index it to optimize performance.
     * The sequence of operations is: (1) add rules to a set using
     * <code>addRule()</code>; (2) freeze the set using
     * <code>freeze()</code>; (3) use the rule set.  If
     * <code>addRule()</code> is called after calling this method, it
     * invalidates this object, and this method must be called again.
     * That is, <code>freeze()</code> may be called multiple times,
     * although for optimal performance it shouldn't be.
     */
    virtual void freeze(UErrorCode& status);
    
    /**
     * Transliterate the given text with the given UTransPosition
     * indices.  Return TRUE if the transliteration should continue
     * or FALSE if it should halt (because of a U_PARTIAL_MATCH match).
     * Note that FALSE is only ever returned if isIncremental is TRUE.
     * @param text the text to be transliterated
     * @param index the position indices, which will be updated
     * @param isIncremental if TRUE, assume new text may be inserted
     * at index.limit, and return FALSE if thre is a partial match.
     * @return TRUE unless a U_PARTIAL_MATCH has been obtained,
     * indicating that transliteration should stop until more text
     * arrives.
     */
    UBool transliterate(Replaceable& text,
                        UTransPosition& index,
                        UBool isIncremental);

    /**
     * Create rule strings that represents this rule set.
     * @param result string to receive the rule strings.  Current
     * contents will be deleted.
     */
    virtual UnicodeString& toRules(UnicodeString& result,
                                   UBool escapeUnprintable) const;
};
#endif
