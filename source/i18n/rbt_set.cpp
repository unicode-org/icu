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

U_CDECL_BEGIN
static void U_CALLCONV _deleteRule(void *rule) {
    delete (U_NAMESPACE_QUALIFIER TransliterationRule *)rule;
}
U_CDECL_END

// Fill the precontext and postcontext with the patterns of the rules
// that are masking one another.
static void maskingError(const U_NAMESPACE_QUALIFIER TransliterationRule& rule1,
                         const U_NAMESPACE_QUALIFIER TransliterationRule& rule2,
                         UParseError& parseError) {
    U_NAMESPACE_QUALIFIER UnicodeString r;
    int32_t len;

    parseError.line = 0;
    parseError.offset = 0;
    
    // for pre-context
    rule1.toRule(r, FALSE);
    len = uprv_min(r.length(), U_PARSE_CONTEXT_LEN-1);
    r.extract(0, len, parseError.preContext);
    parseError.preContext[len] = 0;   
    
    //for post-context
    r.truncate(0);
    rule2.toRule(r, FALSE);
    len = uprv_min(r.length(), U_PARSE_CONTEXT_LEN-1);
    r.extract(0, len, parseError.postContext);
    parseError.postContext[len] = 0;   
}

U_NAMESPACE_BEGIN

/**
 * Construct a new empty rule set.
 */
TransliterationRuleSet::TransliterationRuleSet(UErrorCode& status) {
    ruleVector = new UVector(&_deleteRule, NULL, status);
    rules = NULL;
    maxContextLength = 0;
    if (ruleVector == NULL) {
        status = U_MEMORY_ALLOCATION_ERROR;
    }
}

/**
 * Copy constructor.
 */
TransliterationRuleSet::TransliterationRuleSet(const TransliterationRuleSet& other) :
    ruleVector(0),
    rules(0),
    maxContextLength(other.maxContextLength) {

    int32_t i, len;
    uprv_memcpy(index, other.index, sizeof(index));
    UErrorCode status = U_ZERO_ERROR;
    ruleVector = new UVector(&_deleteRule, NULL, status);
    if (other.ruleVector != 0 && ruleVector != 0 && U_SUCCESS(status)) {
        len = other.ruleVector->size();
        for (i=0; i<len && U_SUCCESS(status); ++i) {
            ruleVector->addElement(new TransliterationRule(
              *(TransliterationRule*)other.ruleVector->elementAt(i)), status);
        }
    }
    if (other.rules != 0) {
        UParseError p;
        freeze(p, status);
    }
}

/**
 * Destructor.
 */
TransliterationRuleSet::~TransliterationRuleSet() {
    delete ruleVector; // This deletes the contained rules
    delete[] rules;
}

void TransliterationRuleSet::setData(const TransliterationRuleData* d) {
    /**
     * We assume that the ruleset has already been frozen.
     */
    int32_t len = index[256]; // see freeze()
    for (int32_t i=0; i<len; ++i) {
        rules[i]->setData(d);
    }
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
 * <p>If freeze() has already been called, calling addRule()
 * unfreezes the rules, and freeze() must be called again.
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

    delete rules;
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
void TransliterationRuleSet::freeze(UParseError& parseError,UErrorCode& status) {
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
                    status = U_RULE_MASK_ERROR;
                    maskingError(*r1, *r2, parseError);
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
        default: /* Ram: added default to make GCC happy */
            break;
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

U_NAMESPACE_END
