/*
**********************************************************************
*   Copyright (C) 1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   11/17/99    aliu        Creation.
**********************************************************************
*/
#ifndef RBT_SET_H
#define RBT_SET_H

#include "uvector.h"

class Replaceable;
class TransliterationRule;
class TransliterationRuleData;
class UnicodeFilter;
class UnicodeString;

/**
 * A set of rules for a <code>RuleBasedTransliterator</code>.  This set encodes
 * the transliteration in one direction from one set of characters or short
 * strings to another.  A <code>RuleBasedTransliterator</code> consists of up to
 * two such sets, one for the forward direction, and one for the reverse.
 *
 * <p>A <code>TransliterationRuleSet</code> has one important operation, that of
 * finding a matching rule at a given point in the text.  This is accomplished
 * by the <code>findMatch()</code> method.
 *
 * @author Alan Liu
 */
class TransliterationRuleSet {
    /**
     * Vector of rules, in the order added.
     */
    UVector rules;

    /**
     * Length of the longest preceding context
     */
    int32_t maxContextLength;

public:

    /**
     * Construct a new empty rule set.
     */
    TransliterationRuleSet();

    /**
     * Return the maximum context length.
     * @return the length of the longest preceding context.
     */
    virtual int32_t getMaximumContextLength() const;

    /**
     * Add a rule to this set.  Rules are added in order, and order is
     * significant.
     *
     * <p>Once freeze() is called, this method must not be called.
     * @param rule the rule to add
     */
    virtual void addRule(TransliterationRule* adoptedRule,
                         UErrorCode& status);

    /**
     * Free up space.  Once this method is called, addRule() must NOT
     * be called again.
     */
    virtual void freeze();

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
     * @param result tranlated text
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
    virtual TransliterationRule* findMatch(const UnicodeString& text,
                                           int32_t start, int32_t limit,
                                           const UnicodeString& result,
                                           int32_t cursor,
                                           const TransliterationRuleData& data,
                                           const UnicodeFilter* filter) const;

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
    virtual TransliterationRule* findMatch(const Replaceable& text,
                                           int32_t start, int32_t limit,
                                           int32_t cursor,
                                           const TransliterationRuleData& data,
                                           const UnicodeFilter* filter) const;
    
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
    virtual TransliterationRule* findIncrementalMatch(const Replaceable& text,
                                              int32_t start,
                                              int32_t limit, int32_t cursor,
                                              const TransliterationRuleData& data,
                                              bool_t& isPartial,
                                              const UnicodeFilter* filter) const;
};
#endif
