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
#include "unistr.h"

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
}

/**
 * Return the maximum context length.
 * @return the length of the longest preceding context.
 */
int32_t TransliterationRuleSet::getMaximumContextLength() const {
    return maxContextLength;
}

/**
 * Add a rule to this set.  Rules are added in order, and order is
 * significant.
 *
 * <p>Once freeze() is called, this method must not be called.
 * @param rule the rule to add
 */
void TransliterationRuleSet::addRule(TransliterationRule* adoptedRule,
                                     UErrorCode& status) {
    
    // Build time, no checking  : 3562 ms
    // Build time, with checking: 6234 ms

    if (U_FAILURE(status)) {
        delete adoptedRule;
        return;
    }

    for (int32_t i=0; i<rules.size(); ++i) {
        TransliterationRule* r = (TransliterationRule*) rules.elementAt(i);
        if (r->masks(*adoptedRule)) {
            //throw new IllegalArgumentException("Rule " + rule +
            //                                   " must precede " + r);
            status = U_ILLEGAL_ARGUMENT_ERROR;
            delete adoptedRule;
            return;
        }
    }

    rules.addElement(adoptedRule);
    int32_t len;
    if ((len = adoptedRule->getAnteContextLength()) > maxContextLength) {
        maxContextLength = len;
    }
}

/**
 * Free up space.  Once this method is called, addRule() must NOT
 * be called again.
 */
void TransliterationRuleSet::freeze() {
    for (int32_t i=0; i<rules.size(); ++i) {
        ((TransliterationRule*) rules.elementAt(i))->freeze();
    }
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
 * <tt>filter.isIn()</tt> returns <tt>false</tt> will not be
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
    for (int32_t i=0; i<rules.size(); ++i) {
        TransliterationRule* rule =
            (TransliterationRule*) rules.elementAt(i);
        if (rule->matches(text, start, limit, result,
                          cursor, data, filter)) {
            return rule;
        }
    }
    return 0;
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
 * <tt>filter.isIn()</tt> returns <tt>false</tt> will not be
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
    for (int32_t i=0; i<rules.size(); ++i) {
        TransliterationRule* rule =
            (TransliterationRule*) rules.elementAt(i);
        if (rule->matches(text, start, limit, cursor,
                          data, filter)) {
            return rule;
        }
    }
    return 0;
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
 * <tt>filter.isIn()</tt> returns <tt>false</tt> will not be
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
    isPartial = FALSE;
    for (int32_t i=0; i<rules.size(); ++i) {
        TransliterationRule* rule =
            (TransliterationRule*) rules.elementAt(i);
        int32_t match = rule->getMatchDegree(text, start, limit, cursor,
                                             data, filter);
        switch (match) {
        case TransliterationRule::FULL_MATCH:
            return rule;
        case TransliterationRule::PARTIAL_MATCH:
            isPartial = TRUE;
            return 0;
        }
    }
    return 0;
}
