/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/text/Attic/TransliterationRuleSet.java,v $ 
 * $Date: 2000/06/29 21:59:23 $ 
 * $Revision: 1.10 $
 *
 *****************************************************************************************
 */
package com.ibm.text;

import java.util.*;

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
 * <p>Copyright &copy; IBM Corporation 1999.  All rights reserved.
 *
 * @author Alan Liu
 * @version $RCSfile: TransliterationRuleSet.java,v $ $Revision: 1.10 $ $Date: 2000/06/29 21:59:23 $
 *
 * $Log: TransliterationRuleSet.java,v $
 * Revision 1.10  2000/06/29 21:59:23  alan4j
 * Fix handling of Transliterator.Position fields
 *
 * Revision 1.9  2000/03/10 04:07:24  johnf
 * Copyright update
 *
 * Revision 1.8  2000/02/03 18:11:19  Alan
 * Use array rather than hashtable for char-to-set map
 *
 * Revision 1.7  2000/01/27 18:59:19  Alan
 * Use Position rather than int[] and move all subclass overrides to one method (handleTransliterate)
 *
 * Revision 1.6  2000/01/18 20:36:17  Alan
 * Make UnicodeSet inherit from UnicodeFilter
 *
 * Revision 1.5  2000/01/04 21:43:57  Alan
 * Add rule indexing, and move masking check to TransliterationRuleSet.
 *
 * Revision 1.4  1999/12/22 01:40:54  Alan
 * Consolidate rule pattern anteContext, key, and postContext into one string.
 *
 * Revision 1.3  1999/12/22 01:05:54  Alan
 * Improve masking checking; turn it off by default, for better performance
 *
 * Revision 1.2  1999/12/22 00:01:36  Alan
 * Detect a>x masking a>y
 *
 */
class TransliterationRuleSet {
    /**
     * Vector of rules, in the order added.  This is only used while the rule
     * set is getting built.  After that, freeze() reorders and indexes the
     * rules, and this Vector is freed.
     */
    private Vector ruleVector;

    /**
     * Length of the longest preceding context
     */
    private int maxContextLength;

    /**
     * Sorted and indexed table of rules.  This is created by freeze() from
     * the rules in ruleVector.
     */
    private TransliterationRule[] rules;

    /**
     * Index table.  For text having a first character c, compute x = c&0xFF.
     * Now use rules[index[x]..index[x+1]-1].  This index table is created by
     * freeze().
     */
    private int[] index;

    private static final String COPYRIGHT =
        "\u00A9 IBM Corporation 1999. All rights reserved.";

    /**
     * Construct a new empty rule set.
     */
    public TransliterationRuleSet() {
        ruleVector = new Vector();
        maxContextLength = 0;
    }

    /**
     * Return the maximum context length.
     * @return the length of the longest preceding context.
     */
    public int getMaximumContextLength() {
        return maxContextLength;
    }

    /**
     * Add a rule to this set.  Rules are added in order, and order is
     * significant.
     * @param rule the rule to add
     */
    public void addRule(TransliterationRule rule) {
        if (ruleVector == null) {
            throw new IllegalArgumentException("Cannot add rules after freezing");
        }
        ruleVector.addElement(rule);
        int len;
        if ((len = rule.getAnteContextLength()) > maxContextLength) {
            maxContextLength = len;
        }
    }

    /**
     * Close this rule set to further additions, check it for masked rules,
     * and index it to optimize performance.  Once this method is called,
     * addRule() can no longer be called.
     * @exception IllegalArgumentException if some rules are masked
     */
    public void freeze(RuleBasedTransliterator.Data variables) {
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
        int n = ruleVector.size();
        index = new int[257]; // [sic]
        Vector v = new Vector(2*n); // heuristic; adjust as needed

        /* Precompute the index values.  This saves a LOT of time.
         */
        int[] indexValue = new int[n];
        for (int j=0; j<n; ++j) {
            TransliterationRule r = (TransliterationRule) ruleVector.elementAt(j);
            indexValue[j] = r.getIndexValue(variables);
        }
        for (int x=0; x<256; ++x) {
            index[x] = v.size();
            for (int j=0; j<n; ++j) {
                if (indexValue[j] >= 0) {
                    if (indexValue[j] == x) {
                        v.addElement(ruleVector.elementAt(j));
                    }
                } else {
                    // If the indexValue is < 0, then the first key character is
                    // a set, and we must use the more time-consuming
                    // matchesIndexValue check.  In practice this happens
                    // rarely, so we seldom tread this code path.
                    TransliterationRule r = (TransliterationRule) ruleVector.elementAt(j);
                    if (r.matchesIndexValue(x, variables)) {
                        v.addElement(r);
                    }
                }
            }
        }
        index[256] = v.size();

        /* Freeze things into an array.
         */
        rules = new TransliterationRule[v.size()];
        v.copyInto(rules);
        ruleVector = null;

        StringBuffer errors = null;

        /* Check for masking.  This is MUCH faster than our old check,
         * which was each rule against each following rule, since we
         * only have to check for masking within each bin now.  It's
         * 256*O(n2^2) instead of O(n1^2), where n1 is the total rule
         * count, and n2 is the per-bin rule count.  But n2<<n1, so
         * it's a big win.
         */
        for (int x=0; x<256; ++x) {
            for (int j=index[x]; j<index[x+1]-1; ++j) {
                TransliterationRule r1 = rules[j];
                for (int k=j+1; k<index[x+1]; ++k) {
                    TransliterationRule r2 = rules[k];
                    if (r1.masks(r2)) {
                        if (errors == null) {
                            errors = new StringBuffer();
                        } else {
                            errors.append("\n");
                        }
                        errors.append("Rule " + r1 + " masks " + r2);
                    }
                }
            }
        }

        if (errors != null) {
            throw new IllegalArgumentException(errors.toString());
        }
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
     * @param variables a dictionary mapping variables to the sets they
     * represent (maps <code>Character</code> to <code>UnicodeSet</code>)
     * @param filter the filter.  Any character for which
     * <tt>filter.contains()</tt> returns <tt>false</tt> will not be
     * altered by this transliterator.  If <tt>filter</tt> is
     * <tt>null</tt> then no filtering is applied.
     * @return the matching rule, or null if none found.
     */
    public TransliterationRule findMatch(Replaceable text,
                                         Transliterator.Position pos,
                                         RuleBasedTransliterator.Data variables,
                                         UnicodeFilter filter) {
        /* We only need to check our indexed bin of the rule table,
         * based on the low byte of the first key character.
         */
        int x = text.charAt(pos.start) & 0xFF;
        for (int i=index[x]; i<index[x+1]; ++i) {
            if (rules[i].matches(text, pos, variables, filter)) {
                return rules[i];
            }
        }
        return null;
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
     * @param variables a dictionary mapping variables to the sets they
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
    public TransliterationRule findIncrementalMatch(Replaceable text,
                                                    Transliterator.Position pos,
                                                    RuleBasedTransliterator.Data variables,
                                                    boolean partial[],
                                                    UnicodeFilter filter) {
        /* We only need to check our indexed bin of the rule table,
         * based on the low byte of the first key character.
         */
        partial[0] = false;
        int x = text.charAt(pos.start) & 0xFF;
        for (int i=index[x]; i<index[x+1]; ++i) {
            int match = rules[i].getMatchDegree(text, pos,
                                                variables, filter);
            switch (match) {
            case TransliterationRule.FULL_MATCH:
                return rules[i];
            case TransliterationRule.PARTIAL_MATCH:
                partial[0] = true;
                return null;
            }
        }
        return null;
    }
}
