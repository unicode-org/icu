/*
 *******************************************************************************
 * Copyright (C) 1996-2003, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/text/TransliterationRuleSet.java,v $
 * $Date: 2003/06/03 18:49:35 $
 * $Revision: 1.27 $
 *
 *****************************************************************************************
 */
package com.ibm.icu.text;

import java.util.*;
import com.ibm.icu.impl.UtilityExtensions;

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
 * @version $RCSfile: TransliterationRuleSet.java,v $ $Revision: 1.27 $ $Date: 2003/06/03 18:49:35 $
 */
class TransliterationRuleSet {
    /**
     * Vector of rules, in the order added.
     */
    private Vector ruleVector;

    /**
     * Length of the longest preceding context
     */
    private int maxContextLength;

    /**
     * Sorted and indexed table of rules.  This is created by freeze() from
     * the rules in ruleVector.  rules.length >= ruleVector.size(), and the
     * references in rules[] are aliases of the references in ruleVector.
     * A single rule in ruleVector is listed one or more times in rules[].
     */
    private TransliterationRule[] rules;

    /**
     * Index table.  For text having a first character c, compute x = c&0xFF.
     * Now use rules[index[x]..index[x+1]-1].  This index table is created by
     * freeze().
     */
    private int[] index;

    private static final String COPYRIGHT =
        "\u00A9 IBM Corporation 1999-2001. All rights reserved.";

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
        ruleVector.addElement(rule);
        int len;
        if ((len = rule.getAnteContextLength()) > maxContextLength) {
            maxContextLength = len;
        }

        rules = null;
    }

    /**
     * Close this rule set to further additions, check it for masked rules,
     * and index it to optimize performance.
     * @exception IllegalArgumentException if some rules are masked
     */
    public void freeze() {
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
            indexValue[j] = r.getIndexValue();
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
                    if (r.matchesIndexValue(x)) {
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
    public boolean transliterate(Replaceable text,
                                 Transliterator.Position pos,
                                 boolean incremental) {
        int indexByte = text.char32At(pos.start) & 0xFF;
        for (int i=index[indexByte]; i<index[indexByte+1]; ++i) {
            int m = rules[i].matchAndReplace(text, pos, incremental);
            switch (m) {
            case UnicodeMatcher.U_MATCH:
                if (Transliterator.DEBUG) {
                    System.out.println((incremental ? "Rule.i: match ":"Rule: match ") +
                                       rules[i].toRule(true) + " => " +
                                       UtilityExtensions.formatInput(text, pos));
                }
                return true;
            case UnicodeMatcher.U_PARTIAL_MATCH:
                if (Transliterator.DEBUG) {
                    System.out.println((incremental ? "Rule.i: partial match ":"Rule: partial match ") +
                                       rules[i].toRule(true) + " => " +
                                       UtilityExtensions.formatInput(text, pos));
                }
                return false;
            }
        }
        // No match or partial match from any rule
        pos.start += UTF16.getCharCount(text.char32At(pos.start));
        if (Transliterator.DEBUG) {
            System.out.println((incremental ? "Rule.i: no match => ":"Rule: no match => ") +
                               UtilityExtensions.formatInput(text, pos));
        }
        return true;
    }

    /**
     * Create rule strings that represents this rule set.
     */
    String toRules(boolean escapeUnprintable) {
        int i;
        int count = ruleVector.size();
        StringBuffer ruleSource = new StringBuffer();
        for (i=0; i<count; ++i) {
            if (i != 0) {
                ruleSource.append('\n');
            }
            TransliterationRule r =
                (TransliterationRule) ruleVector.elementAt(i);
            ruleSource.append(r.toRule(escapeUnprintable));
        }
        return ruleSource.toString();
    }

    /**
     * Return the set of all characters that may be modified (getTarget=false)
     * or emitted (getTarget=true) by this set.
     */
    UnicodeSet getSourceTargetSet(boolean getTarget) {
        UnicodeSet set = new UnicodeSet();
        int count = ruleVector.size();
        for (int i=0; i<count; ++i) {
            TransliterationRule r =
                (TransliterationRule) ruleVector.elementAt(i);
            if (getTarget) {
                r.addTargetSetTo(set);
            } else {
                r.addSourceSetTo(set);
            }
        }
        return set;
    }
}

/* $Log: TransliterationRuleSet.java,v $
 * Revision 1.27  2003/06/03 18:49:35  alan
 * jitterbug 2959: update copyright dates to include 2003
 *
/* Revision 1.26  2003/01/28 18:55:42  rviswanadha
/* jitterbug 2309: Modularize ICU4J big bang commit
/*
/* Revision 1.25  2002/06/28 19:15:53  alan
/* jitterbug 1434: improve method names; minor cleanup
/*
/* Revision 1.24  2002/06/26 18:12:40  alan
/* jitterbug 1434: initial public implementation of getSourceSet and getTargetSet
/*
/* Revision 1.23  2002/02/25 22:43:58  ram
/* Move Utility class to icu.impl
/*
/* Revision 1.22  2002/02/16 03:06:17  Mohan
/* ICU4J reorganization
/*
/* Revision 1.21  2002/02/09 01:01:47  alan
/* jitterbug 1544: add char32At() to Replaceable
/*
/* Revision 1.20  2001/11/29 22:31:18  alan
/* jitterbug 1560: add source-set methods and TransliteratorUtility class
/*
/* Revision 1.19  2001/11/29 16:11:46  alan
/* jitterbug 1560: add debugging code; fix handling of runs; detect incomplete non-incremental processing
/*
/* Revision 1.18  2001/11/27 21:57:05  alan
/* jitterbug 1389: incorporate Mark's review comments - comments only
/*
/* Revision 1.17  2001/11/06 05:06:26  alan
/* jitterbug 60: make toRules() read from original vector
/*
/* Revision 1.16  2001/11/05 18:55:54  alan
/* jitterbug 60: elide duplicate rules in toRules()
/*
/* Revision 1.15  2001/10/26 22:48:41  alan
/* jitterbug 68: add DEBUG support to dump rule-based match progression
/*
 * Revision 1.14  2001/10/25 22:33:19  alan
 * jitterbug 73: use int for index values to avoid signedness problems
 *
 * Revision 1.13  2001/09/26 18:17:42  alan
 * jitterbug 67: delete obsolete code
 *
 * Revision 1.12  2001/09/26 18:00:06  alan
 * jitterbug 67: sync parser with icu4c, allow unlimited, nested segments
 *
 * Revision 1.11  2001/09/19 17:43:38  alan
 * jitterbug 60: initial implementation of toRules()
 *
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
 */
