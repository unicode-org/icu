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

/* Note: There was an old implementation that indexed by first letter of
 * key.  Problem with this is that key may not have a meaningful first
 * letter; e.g., {Lu}>*.  One solution is to keep a separate vector of all
 * rules whose intial key letter is a category variable.  However, the
 * problem is that they must be kept in order with respect to other rules.
 * One solution -- add a sequence number to each rule.  Do the usual
 * first-letter lookup, and also a lookup from the spare bin with rules like
 * {Lu}>*.  Take the lower sequence number.  This seems complex and not
 * worth the trouble, but we may revisit this later.  For documentation (or
 * possible resurrection) the old code is included below, commented out
 * with the remark "// OLD INDEXED IMPLEMENTATION".  Under the old
 * implementation, <code>rules</code> is a Hashtable, not a Vector.
 */

/**
 * Construct a new empty rule set.
 */
TransliterationRuleSet::TransliterationRuleSet() {
    maxContextLength = 0;
    ruleVector = new UVector();
    rules = NULL;
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
 * significant.
 *
 * <p>Once freeze() is called, this method must not be called.
 * @param adoptedRule the rule to add
 */
void TransliterationRuleSet::addRule(TransliterationRule* adoptedRule,
                                     UErrorCode& status) {
    if (U_FAILURE(status)) {
        delete adoptedRule;
        return;
    }
    if (ruleVector == NULL) {
        // throw new IllegalArgumentException("Cannot add rules after freezing");
        status = U_ILLEGAL_ARGUMENT_ERROR;
        delete adoptedRule;
        return;
    }
    ruleVector->addElement(adoptedRule);

    int32_t len;
    if ((len = adoptedRule->getAnteContextLength()) > maxContextLength) {
        maxContextLength = len;
    }
}

/**
 * Close this rule set to further additions, check it for masked rules,
 * and index it to optimize performance.  Once this method is called,
 * addRule() can no longer be called.
 * @exception IllegalArgumentException if some rules are masked
 */
void TransliterationRuleSet::freeze(const TransliterationRuleData& data,
                                    UErrorCode& status) {
    if (U_FAILURE(status)) {
        return;
    }

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
    UVector v(2*n); // heuristic; adjust as needed

    /* Precompute the index values.  This saves a LOT of time.
     */
    int16_t* indexValue = new int16_t[n];
    for (j=0; j<n; ++j) {
        TransliterationRule* r = (TransliterationRule*) ruleVector->elementAt(j);
        indexValue[j] = r->getIndexValue(data);
    }
    for (x=0; x<256; ++x) {
        index[x] = v.size();
        for (j=0; j<n; ++j) {
            if (indexValue[j] >= 0) {
                if (indexValue[j] == x) {
                    v.addElement(ruleVector->elementAt(j));
                }
            } else {
                // If the indexValue is < 0, then the first key character is
                // a set, and we must use the more time-consuming
                // matchesIndexValue check.  In practice this happens
                // rarely, so we seldom tread this code path.
                TransliterationRule* r = (TransliterationRule*) ruleVector->elementAt(j);
                if (r->matchesIndexValue((uint8_t)x, data)) {
                    v.addElement(r);
                }
            }
        }
    }
    delete[] indexValue;
    index[256] = v.size();

    /* Freeze things into an array.
     */
    rules = new TransliterationRule*[v.size()];
    for (j=0; j<v.size(); ++j) {
        rules[j] = (TransliterationRule*) v.elementAt(j);
    }
    delete ruleVector;
    ruleVector = NULL;

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
 * Attempt to find a matching rule at the specified point in the text.  The
 * text being matched occupies a virtual buffer consisting of the contents
 * of <code>result</code> concatenated to a substring of <code>text</code>.
 * The substring is specified by <code>start</code> and <code>limit</code>.
 * The value of <code>cursor</code> is an index into this virtual buffer,
 * from 0 to the length of the buffer.  In terms of the parameters,
 * <code>cursor</code> must be between 0 and <code>result.length() + limit -
 * start</code>.
 * @param text the untranslated text
 * @param start the beginning index, inclusive; <code>0 <= start
 * <= limit</code>.
 * @param limit the ending index, exclusive; <code>start <= limit
 * <= text.length()</code>.
 * @param result translated text
 * @param cursor position at which to translate next, an offset into result.
 * If greater than or equal to result.length(), represents offset start +
 * cursor - result.length() into text.
 * @param data a dictionary mapping variables to the sets they
 * represent (maps <code>Character</code> to <code>UnicodeSet</code>)
 * @param filter the filter.  Any character for which
 * <tt>filter.contains()</tt> returns <tt>false</tt> will not be
 * altered by this transliterator.  If <tt>filter</tt> is
 * <tt>null</tt> then no filtering is applied.
 * @return the matching rule, or null if none found.
 */
TransliterationRule*
TransliterationRuleSet::findMatch(const UnicodeString& text,
                                  int32_t start, int32_t limit,
                                  const UnicodeString& result,
                                  int32_t cursor,
                                  const TransliterationRuleData& data,
                                  const UnicodeFilter* filter) const {
    /* We only need to check our indexed bin of the rule table,
     * based on the low byte of the first key character.
     */
    int32_t rlen = result.length();
    int16_t x = 0xFF & (cursor < rlen ? result.charAt(cursor)
                        : text.charAt(cursor - rlen + start));
    for (int32_t i=index[x]; i<index[x+1]; ++i) {
        if (rules[i]->matches(text, start, limit, result, cursor, data, filter)) {
            return rules[i];
        }
    }
    return NULL;
}

/**
 * Attempt to find a matching rule at the specified point in the text.
 * @param text the text, both translated and untranslated
 * @param start the beginning index, inclusive; <code>0 <= start
 * <= limit</code>.
 * @param limit the ending index, exclusive; <code>start <= limit
 * <= text.length()</code>.
 * @param cursor position at which to translate next, representing offset
 * into text.  This value must be between <code>start</code> and
 * <code>limit</code>.
 * @param data a dictionary mapping variables to the sets they
 * represent (maps <code>Character</code> to <code>UnicodeSet</code>)
 * @param filter the filter.  Any character for which
 * <tt>filter.contains()</tt> returns <tt>false</tt> will not be
 * altered by this transliterator.  If <tt>filter</tt> is
 * <tt>null</tt> then no filtering is applied.
 * @return the matching rule, or null if none found.
 */
TransliterationRule*
TransliterationRuleSet::findMatch(const Replaceable& text,
                                  int32_t start, int32_t limit,
                                  int32_t cursor,
                                  const TransliterationRuleData& data,
                                  const UnicodeFilter* filter) const {
    /* We only need to check our indexed bin of the rule table,
     * based on the low byte of the first key character.
     */
    int16_t x = text.charAt(cursor) & 0xFF;
    for (int32_t i=index[x]; i<index[x+1]; ++i) {
        if (rules[i]->matches(text, start, limit, cursor, data, filter)) {
            return rules[i];
        }
    }
    return NULL;
}

/**
 * Attempt to find a matching rule at the specified point in the text.
 * Unlike <code>findMatch()</code>, this method does an incremental match.
 * An incremental match requires that there be no partial matches that might
 * pre-empt the full match that is found.  If there are partial matches,
 * then null is returned.  A non-null result indicates that a full match has
 * been found, and that it cannot be pre-empted by a partial match
 * regardless of what additional text is added to the translation buffer.
 * @param text the text, both translated and untranslated
 * @param start the beginning index, inclusive; <code>0 <= start
 * <= limit</code>.
 * @param limit the ending index, exclusive; <code>start <= limit
 * <= text.length()</code>.
 * @param cursor position at which to translate next, representing offset
 * into text.  This value must be between <code>start</code> and
 * <code>limit</code>.
 * @param data a dictionary mapping variables to the sets they
 * represent (maps <code>Character</code> to <code>UnicodeSet</code>)
 * @param partial output parameter.  <code>partial[0]</code> is set to
 * true if a partial match is returned.
 * @param filter the filter.  Any character for which
 * <tt>filter.contains()</tt> returns <tt>false</tt> will not be
 * altered by this transliterator.  If <tt>filter</tt> is
 * <tt>null</tt> then no filtering is applied.
 * @return the matching rule, or null if none found, or if the text buffer
 * does not have enough text yet to unambiguously match a rule.
 */
TransliterationRule*
TransliterationRuleSet::findIncrementalMatch(const Replaceable& text,
                                             int32_t start,
                                             int32_t limit, int32_t cursor,
                                             const TransliterationRuleData& data,
                                             bool_t& isPartial,
                                             const UnicodeFilter* filter) const {

    /* We only need to check our indexed bin of the rule table,
     * based on the low byte of the first key character.
     */
    isPartial = FALSE;
    int16_t x = text.charAt(cursor) & 0xFF;
    for (int32_t i=index[x]; i<index[x+1]; ++i) {
        int32_t match = rules[i]->getMatchDegree(text, start, limit, cursor,
                                                 data, filter);
        switch (match) {
        case TransliterationRule::FULL_MATCH:
            return rules[i];
        case TransliterationRule::PARTIAL_MATCH:
            isPartial = TRUE;
            return NULL;
        }
    }
    return NULL;
}
