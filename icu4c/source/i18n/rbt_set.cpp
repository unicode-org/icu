/*
**********************************************************************
*   Copyright (C) 1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   11/17/99    aliu        Creation.
**********************************************************************
*/
#include "rbt_set.h"
#include "rbt_rule.h"
#include "unicode/unistr.h"
#include "cmemory.h"

static void U_CALLCONV _deleteRule(void *rule) {
    delete (TransliterationRule *)rule;
}

/**
 * Construct a new empty rule set.
 */
TransliterationRuleSet::TransliterationRuleSet(UErrorCode& status) {
    maxContextLength = 0;
    ruleVector = new UVector(status);
    ruleVector->setDeleter(&_deleteRule);
    rules = NULL;
    if (ruleVector == NULL) {
        status = U_MEMORY_ALLOCATION_ERROR;
    }
}

/**
 * Copy constructor.  We assume that the ruleset being copied
 * has already been frozen.
 */
TransliterationRuleSet::TransliterationRuleSet(const TransliterationRuleSet& other) :
    ruleVector(0),
    maxContextLength(other.maxContextLength) {

    uprv_memcpy(index, other.index, sizeof(index));
    int32_t len = index[256]; // see freeze()
    rules = new TransliterationRule*[len];
    for (int32_t i=0; i<len; ++i) {
        rules[i] = new TransliterationRule(*other.rules[i]);
    }
}

/**
 * Destructor.
 */
TransliterationRuleSet::~TransliterationRuleSet() {
    delete ruleVector;
    delete[] rules;
}

/**
 * Return the maximum context length.
 * @return the length of the longest preceding context.
 */
int32_t TransliterationRuleSet::getMaximumContextLength(void) const {
    return maxContextLength;
}

/**
 * Add a rule to this set.  Rules are added in order, and order is
 * significant.  The last call to this method must be followed by
 * a call to <code>freeze()</code> before the rule set is used.
 *
 * @param adoptedRule the rule to add
 */
void TransliterationRuleSet::addRule(TransliterationRule* adoptedRule,
                                     UErrorCode& status) {
    if (U_FAILURE(status)) {
        delete adoptedRule;
        return;
    }
    ruleVector->addElement(adoptedRule, status);

    int32_t len;
    if ((len = adoptedRule->getContextLength()) > maxContextLength) {
        maxContextLength = len;
    }

    delete[] rules; // Contains alias pointers
    rules = 0;
}

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
void TransliterationRuleSet::freeze(UErrorCode& status) {
    /* Construct the rule array and index table.  We reorder the
     * rules by sorting them into 256 bins.  Each bin contains all
     * rules matching the index value for that bin.  A rule
     * matches an index value if string whose first key character
     * has a low byte equal to the index value can match the rule.
     *
     * Each bin contains zero or more rules, in the same order
     * they were found originally.  However, the total rules in
     * the bins may exceed the number in the original vector,
     * since rules that have a variable as their first key
     * character will generally fall into more than one bin.
     *
     * That is, each bin contains all rules that either have that
     * first index value as their first key character, or have
     * a set containing the index value as their first character.
     */
    int32_t n = ruleVector->size();
    int32_t j;
    int16_t x;
    UVector v(status, 2*n); // heuristic; adjust as needed

    if (U_FAILURE(status)) {
        return;
    }

    /* Precompute the index values.  This saves a LOT of time.
     */
    int16_t* indexValue = new int16_t[n];
    for (j=0; j<n; ++j) {
        TransliterationRule* r = (TransliterationRule*) ruleVector->elementAt(j);
        indexValue[j] = r->getIndexValue();
    }
    for (x=0; x<256; ++x) {
        index[x] = v.size();
        for (j=0; j<n; ++j) {
            if (indexValue[j] >= 0) {
                if (indexValue[j] == x) {
                    v.addElement(ruleVector->elementAt(j), status);
                }
            } else {
                // If the indexValue is < 0, then the first key character is
                // a set, and we must use the more time-consuming
                // matchesIndexValue check.  In practice this happens
                // rarely, so we seldom tread this code path.
                TransliterationRule* r = (TransliterationRule*) ruleVector->elementAt(j);
                if (r->matchesIndexValue((uint8_t)x)) {
                    v.addElement(r, status);
                }
            }
        }
    }
    delete[] indexValue;
    index[256] = v.size();

    /* Freeze things into an array.
     */
    delete[] rules; // Contains alias pointers
    rules = new TransliterationRule*[v.size()];
    for (j=0; j<v.size(); ++j) {
        rules[j] = (TransliterationRule*) v.elementAt(j);
    }

    // TODO Add error reporting that indicates the rules that
    //      are being masked.
    //UnicodeString errors;

    /* Check for masking.  This is MUCH faster than our old check,
     * which was each rule against each following rule, since we
     * only have to check for masking within each bin now.  It's
     * 256*O(n2^2) instead of O(n1^2), where n1 is the total rule
     * count, and n2 is the per-bin rule count.  But n2<<n1, so
     * it's a big win.
     */
    for (x=0; x<256; ++x) {
        for (j=index[x]; j<index[x+1]-1; ++j) {
            TransliterationRule* r1 = rules[j];
            for (int32_t k=j+1; k<index[x+1]; ++k) {
                TransliterationRule* r2 = rules[k];
                if (r1->masks(*r2)) {
//|                 if (errors == null) {
//|                     errors = new StringBuffer();
//|                 } else {
//|                     errors.append("\n");
//|                 }
//|                 errors.append("Rule " + r1 + " masks " + r2);
                    status = U_ILLEGAL_ARGUMENT_ERROR;
                    return;
                }
            }
        }
    }

    //if (errors != null) {
    //    throw new IllegalArgumentException(errors.toString());
    //}
}

/**
 * Transliterate the given text with the given UTransPosition
 * indices.  Return TRUE if the transliteration should continue
 * or FALSE if it should halt (because of a U_PARTIAL_MATCH match).
 * Note that FALSE is only ever returned if isIncremental is TRUE.
 * @param text the text to be transliterated
 * @param pos the position indices, which will be updated
 * @param incremental if TRUE, assume new text may be inserted
 * at index.limit, and return FALSE if thre is a partial match.
 * @return TRUE unless a U_PARTIAL_MATCH has been obtained,
 * indicating that transliteration should stop until more text
 * arrives.
 */
UBool TransliterationRuleSet::transliterate(Replaceable& text,
                                            UTransPosition& pos,
                                            UBool incremental) {
    int16_t indexByte = (int16_t) (text.char32At(pos.start) & 0xFF);
    for (int32_t i=index[indexByte]; i<index[indexByte+1]; ++i) {
        UMatchDegree m = rules[i]->matchAndReplace(text, pos, incremental);
        switch (m) {
        case U_MATCH:
            return TRUE;
        case U_PARTIAL_MATCH:
            return FALSE;
        }
    }
    // No match or partial match from any rule
    pos.start += UTF_CHAR_LENGTH(text.char32At(pos.start));
    return TRUE;
}

/**
 * Create rule strings that represents this rule set.
 */
UnicodeString& TransliterationRuleSet::toRules(UnicodeString& ruleSource,
                                               UBool escapeUnprintable) const {
    int32_t i;
    int32_t count = index[256];
    ruleSource.truncate(0);
    for (i=0; i<count; ++i) {
        if (i != 0) {
            ruleSource.append((UChar) 0x000A /*\n*/);
        }
        rules[i]->toRule(ruleSource, escapeUnprintable);
    }
    return ruleSource;
}
