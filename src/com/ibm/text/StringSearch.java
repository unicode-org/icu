/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/text/Attic/StringSearch.java,v $ 
 * $Date: 2000/03/10 04:07:24 $ 
 * $Revision: 1.2 $
 *
 *****************************************************************************************
 */

package com.ibm.text;

import java.text.BreakIterator;
import java.text.CharacterIterator;
import java.text.CollationElementIterator;
import java.text.Collator;
import java.text.RuleBasedCollator;
import java.text.StringCharacterIterator;
import java.util.Locale;

/**
 * <code>StringSearch</code> is a <code>SearchIterator</code> that provides
 * language-sensitive text searching based on the comparison rules defined
 * in a {@link RuleBasedCollator} object.
 * Instances of <code>StringSearch</code> function as iterators
 * maintain a current position and scan over text returning the index of
 * characters where the pattern occurs and the length of each match.
 * <p>
 * <code>StringSearch</code> uses a version of the fast Boyer-Moore search
 * algorithm that has been adapted to work with the large character set of
 * Unicode.  See "Efficient Text Searching in Java", to be published in
 * <i>Java Report</i> in February, 1999, for further information on the algorithm.
 * <p>
 * Consult the <code>SearchIterator</code> documentation for information on
 * and examples of how to use instances of this class to implement text
 * searching.  <code>SearchIterator</code> provides all of the necessary
 * API; this class only provides constructors and internal implementation
 * methods.
 *
 * @see SearchIterator
 * @see java.text.RuleBasedCollator
 *
 * @author Laura Werner
 * @version 1.0
 */
public final class StringSearch extends SearchIterator
{
    /**
     * Construct a <code>StringSearch</code> object using a specific collator and set
     * of boundary-detection rules.
     * <p>
     * @param pat       The text for which this object will search.
     *
     * @param target    The text in which to search for the pattern.
     *
     * @param coll      A <code>RuleBasedCollator</code> object which defines the
     *                  language-sensitive comparison rules used to determine 
     *                  whether text in the pattern and target matches.
     *
     * @param breaker   A <code>BreakIterator</code> object used to constrain the matches
     *                  that are found.  Matches whose start and end indices
     *                  in the target text are not boundaries as determined
     *                  by the <code>BreakIterator</code> are ignored.  If this behavior
     *                  is not desired, <code>null</code> can be passed in instead.
     */
    public StringSearch(String pat, CharacterIterator target,
                            RuleBasedCollator coll, BreakIterator breaker) {
        super(target, breaker);

        pattern = pat;
        collator = coll;
        strength = coll.getStrength();
        iter = collator.getCollationElementIterator(target);
        
        initialize();   // Initialize the Boyer-Moore tables
    }

    /**
     * Construct a <code>StringSearch</code> object using a specific collator.
     * <p>
     * @param pattern   The text for which this object will search.
     *
     * @param target    The text in which to search for the pattern.
     *
     * @param collator  A <code>RuleBasedCollator</code> object which defines the
     *                  language-sensitive comparison rules used to determine 
     *                  whether text in the pattern and target matches.
     */
    public StringSearch(String pattern,
                            CharacterIterator target,
                            RuleBasedCollator collator) {
        this(pattern, target, collator, BreakIterator.getCharacterInstance());
    }

    /**
     * Construct a <code>StringSearch</code> object using the collator and
     * character boundary detection rules for a given locale
     * <p>
     * @param pattern   The text for which this object will search.
     *
     * @param target    The text in which to search for the pattern.
     *
     * @param loc       The locale whose collation and break-detection rules
     *                  should be used.
     *
     * @exception       ClassCastException thrown if the collator for the specified
     *                  locale is not a RuleBasedCollator.
     */
    public StringSearch(String pattern, CharacterIterator target, Locale loc) {
        this(pattern, target,
             (RuleBasedCollator) Collator.getInstance(loc),
             BreakIterator.getCharacterInstance(loc));
    }

    /**
     * Construct a <code>StringSearch</code> object using the collator for the default
     * locale
     * <p>
     * @param pattern   The text for which this object will search.
     *
     * @param target    The text in which to search for the pattern.
     *
     * @param collator  A <code>RuleBasedCollator</code> object which defines the
     *                  language-sensitive comparison rules used to determine 
     *                  whether text in the pattern and target matches.
     */
    public StringSearch(String pattern, String target) {
        this(pattern,
             new StringCharacterIterator(target),
             (RuleBasedCollator)Collator.getInstance(),
             BreakIterator.getCharacterInstance());
    }

    //-------------------------------------------------------------------
    // Getters and Setters
    //-------------------------------------------------------------------
    
    /**
     * Sets this object's strength property. The strength determines the
     * minimum level of difference considered significant during a
     * search.  Generally, {@link Collator#TERTIARY} and 
     * {@link Collator#IDENTICAL} indicate that all differences are
     * considered significant, {@link Collator#SECONDARY} indicates
     * that upper/lower case distinctions should be ignored, and
     * {@link Collator#PRIMARY} indicates that both case and accents
     * should be ignored.  However, the exact meanings of these constants
     * are determined by individual Collator objects.
     * <p>
     * @see java.text.Collator#PRIMARY
     * @see java.text.Collator#SECONDARY
     * @see java.text.Collator#TERTIARY
     * @see java.text.Collator#IDENTICAL
     */
    public void setStrength(int newStrength) {
        strength = newStrength;
        
        // Due to a bug (?) in CollationElementIterator, we must set the
        // collator's strength as well, since the iterator is going to
        // mask out the portions of the collation element that are not
        // relevant for the collator's current strength setting
        // Note that this makes it impossible to share a Collator among
        // multiple StringSearch objects if you adjust Strength settings.
        collator.setStrength(strength);
        initialize();
    }
    
    
    /**
     * Returns this object's strength property, which indicates what level
     * of differences are considered significant during a search.
     * <p>
     * @see #setStrength
     */
    public int getStrength() {
        return strength;
    }
    
    /**
     * Set the collator to be used for this string search.  Also changes
     * the search strength to match that of the new collator.
     * <p>
     * This method causes internal data such as Boyer-Moore shift tables
     * to be recalculated, but the iterator's position is unchanged.
     * <p>
     * @see #getCollator
     */
    public void setCollator(RuleBasedCollator coll) {
        collator = coll;
        strength = collator.getStrength();
        
        // Also need to recompute the pattern and get a new target iterator
        iter = collator.getCollationElementIterator(getTarget());
        initialize();
    }
    
    /**
     * Return the RuleBasedCollator being used for this string search.
     */
    public RuleBasedCollator getCollator() {
        return collator;
    }
    
    /**
     * Set the pattern for which to search.  
     * This method causes internal data such as Boyer-Moore shift tables
     * to be recalculated, but the iterator's position is unchanged.
     */
    public void setPattern(String pat) {
        pattern = pat;
        initialize();
    }
    
    /**
     * Returns the pattern for which this object is searching.
     */
    public String getPattern() {
        return pattern;
    }
    
    /**
     * Set the target text which should be searched and resets the
     * iterator's position to point before the start of the new text.
     * This method is useful if you want to re-use an iterator to
     * search for the same pattern within a different body of text.
     */
    public void setTarget(CharacterIterator target) {
        super.setTarget(target);
        
        // Since we're caching a CollationElementIterator, recreate it
        iter = collator.getCollationElementIterator(target);
    }

    //-------------------------------------------------------------------
    // Privates
    //-------------------------------------------------------------------

    /**
     * Search forward for matching text, starting at a given location.
     * Clients should not call this method directly; instead they should call
     * {@link SearchIterator#next}.
     * <p>
     * If a match is found, this method returns the index at which the match
     * starts and calls {@link SearchIterator#setMatchLength}
     * with the number of characters in the target
     * text that make up the match.  If no match is found, the method returns
     * <code>DONE</code> and does not call <tt>setMatchLength</tt>.
     * <p>
     * @param start The index in the target text at which the search starts.
     *
     * @return      The index at which the matched text in the target starts, or DONE
     *              if no match was found.
     * <p>
     * @see SearchIterator#next
     * @see SearchIterator#DONE
     */
    protected int handleNext(int start)
    {
        CharacterIterator target = getTarget();
        
        int mask = getMask(strength);
        int done = CollationElementIterator.NULLORDER & mask;
        
        if (DEBUG) {
            debug("-------------------------handleNext-----------------------------------");
            debug("");
            debug("strength=" + strength + ", mask=" + Integer.toString(mask,16)
                + ", done=" + Integer.toString(done,16));
            debug("decomp=" + collator.getDecomposition());
            
            debug("target.begin=" + getTarget().getBeginIndex());
            debug("target.end=" + getTarget().getEndIndex());
            debug("start = " + start);
        }
        
        int index = start + minLen;
        int matchEnd = 0;

        while (index <= target.getEndIndex())
        {
            int patIndex = normLen;
            int tval = 0, pval = 0;
            boolean getP = true;

            iter.setOffset(index);
            matchEnd = index;
            
            if (DEBUG) debug(" outer loop: patIndex=" + patIndex + ", index=" + index);
            
            while ((patIndex > 0 || getP == false) && iter.getOffset() > start)
            {
                if (DEBUG) {
                    debug("  inner loop: patIndex=" + patIndex + " iter=" + iter.getOffset());
                    debug("   getP=" + getP);
                }
                
                // Get the previous character in both the pattern and the target
                tval = iter.previous() & mask;
                
                if (getP) pval = valueList[--patIndex];
                getP = true;
                
                if (DEBUG) debug("   pval=" + Integer.toString(pval,16) + ", tval=" + Integer.toString(tval,16));
                
                if (tval == 0) {       // skip tval, use same pval
                    if (DEBUG) debug("   tval is ignorable");
                    getP = false;    
                }
                else if (pval != tval) {    // Mismatch, skip ahead
                    if (DEBUG) debug("   mismatch: skippping " + getShift(tval, patIndex));
                    
                    index += getShift(tval, patIndex);
                    break;
                }
                else if (patIndex == 0) {
                    // The values matched, and we're at the beginning of the pattern,
                    // which means we matched the whole thing.
                    start = iter.getOffset();
                    setMatchLength(matchEnd - start);
                    if (DEBUG) debug("Found match at index "+ start );
                    return start;
                }
            }
            if (DEBUG) debug(" end of inner loop: patIndex=" + patIndex + " iter=" + iter.getOffset());
            if (DEBUG) debug("   getP=" + getP);
            
            if (iter.getOffset() <= start) {
                // We hit the beginning of the text being searched, which is
                // possible if it contains lots of ignorable characters.
                // Advance one character and try again.
                if (DEBUG) debug("hit beginning of target; advance by one");
                index++;
            }
        }
        if (DEBUG) debug("Fell off end of outer loop; returning DONE");
        return DONE;
    }

    /**
     * Search backward for matching text ,starting at a given location.
     * Clients should not call this method directly; instead they should call
     * <code>SearchIterator.previous()</code>, which this method overrides.
     * <p>
     * If a match is found, this method returns the index at which the match
     * starts and calls {@link SearchIterator#setMatchLength}
     * with the number of characters in the target
     * text that make up the match.  If no match is found, the method returns
     * <code>DONE</code> and does not call <tt>setMatchLength</tt>.
     * <p>
     * @param start The index in the target text at which the search starts.
     *
     * @return      The index at which the matched text in the target starts, or DONE
     *              if no match was found.
     * <p>
     * @see SearchIterator#previous
     * @see SearchIterator#DONE
     */
    protected int handlePrev(int start)
    {
        int patLen = normLen;
        int index = start - minLen;

        int mask = getMask(strength);
        int done = CollationElementIterator.NULLORDER & mask;

        if (DEBUG) {
            debug("-------------------------handlePrev-----------------------------------");
            debug("");
            debug("strength=" + strength + ", mask=" + Integer.toString(mask,16)
                + ", done=" + Integer.toString(done,16));
            debug("decomp=" + collator.getDecomposition());
            
            debug("target.begin=" + getTarget().getBeginIndex());
            debug("target.end=" + getTarget().getEndIndex());
        }
        
        while (index >= 0) {
            int patIndex = 0;
            int tval = 0, pval = 0;
            boolean getP = true;

            iter.setOffset(index);

            if (DEBUG) debug(" outer loop: patIndex=" + patIndex + ", index=" + index);
            
            while ((patIndex < patLen || !getP) && iter.getOffset() < start)
            {
                if (DEBUG) {
                    debug("  inner loop: patIndex=" + patIndex + " iter=" + iter.getOffset());
                }
                tval = iter.next() & mask;
                if (getP) pval = valueList[patIndex++];
                getP = true;

                if (DEBUG) debug("   pval=" + Integer.toString(pval,16) + ", tval=" + Integer.toString(tval,16));

                if (tval == done) {
                    if (DEBUG) debug("   end of target; no match");
                    return DONE;
                }
                else if (tval == 0) {
                    if (DEBUG) debug("   tval is ignorable");
                    getP = false;
                }
                else if (pval != tval) {
                    // We didn't match this pattern.  Skip ahead
                    if (DEBUG) debug("   mismatch: skippping " + getBackShift(tval, patIndex));
                    
                    int shift = getBackShift(tval, patIndex);
                    index -= shift;
                    break;
                }
                else if (patIndex == patLen) {
                    // The elements matched and we're at the end of the pattern,
                    // which means we matched the whole thing.
                    setMatchLength(iter.getOffset() - index);
                    return index;
                }
            }
            if (iter.getOffset() >= start) {
                // We hit the end of the text being searched, which is
                // possible if it contains lots of ignorable characters.
                // Back up one character and try again.
                if (DEBUG) debug("hit end of target; back by one");
                index--;
            }
        }
        return DONE;
    }

    /**
     * Return a bitmask that will select only the portions of a collation 
     * element that are significant at the given strength level.
     */
    private static final int getMask(int strength) {
        switch (strength) {
            case Collator.PRIMARY:
                return 0xFFFF0000;
            case Collator.SECONDARY:
                return 0xFFFFFF00;
            default:
                return 0xFFFFFFFF;
        }
    }
    

    //------------------------------------------------------------------------
    // Private Data
    //
    private CollationElementIterator    iter;
    private RuleBasedCollator           collator;
    private int                         strength;
    
    //------------------------------------------------------------------------
    // Everything from here on down is the data used to represent the
    // Boyer-Moore shift tables and the code that generates and manipulates
    // them.
    //
    private static final int MAX_TABLE = 256;        // Size of the shift tables
    
    private int     valueList[] = null;
    private int     shiftTable[] = new int[MAX_TABLE];
    private int     backShiftTable[] = new int[MAX_TABLE];

    private String  pattern;            // The pattern string
    private int     normLen = 0;        // num. of collation elements in pattern.
    private int     minLen = 0;         // Min of composed, decomposed versions
    private int     maxLen = 0;         // Max

    private void initialize() {
        if (DEBUG)  {
            debug("-------------------------initialize-----------------------------------");
            debug("pattern=" + pattern);
        }
        
        CollationElementIterator iter = collator.getCollationElementIterator(pattern);

        int mask = getMask(strength);

        // See how many non-ignorable collation keys are in the text
        normLen = 0;
        int elem;
        while ((elem = iter.next()) != CollationElementIterator.NULLORDER)
        {
            if ((elem & mask) != 0) {
                normLen++;
            }
        }

        // Save them all
        valueList = new int[normLen];
        int expandLen = 0;
        iter.reset();
        
        for (int i = 0; i < normLen; i++)
        {
            elem = iter.next();

            if ((elem & mask) != 0) {
                valueList[i] = elem & mask;
                
            }
            // Keep track of whether there are any expanding-character
            // sequences that can result in one of the characters that's in
            // the pattern.  If there are, we have to reduce the shift
            // distances calculated below to account for it.
            expandLen += iter.getMaxExpansion(elem) - 1;
        }

        //
        // We need to remember the size of the composed and decomposed
        // versions of the string.  Standard Boyer-Moore shift calculations
        // can be wrong by an amount up to that difference, since a small
        // small number of characters in the pattern can map to a larger
        // number in the text being searched, or vice-versa.
        //
        int uniLen = pattern.length();
        maxLen = Math.max(normLen, uniLen);
        minLen = Math.min(normLen, uniLen) - expandLen;

        if (DEBUG) debug("normLen=" + normLen + ", expandLen=" + expandLen
                        + ", maxLen=" + maxLen + ", minLen=" + minLen);
        
        // Now initialize the shift tables
        //
        // NOTE: This is the most conservative way to build them.  If we had a way
        // of knowing that there were no expanding/contracting chars in the rules,
        // we could get rid of the "- 1" in the shiftTable calculations.
        // But all of the default collators have at least one expansion or
        // contraction, so it probably doesn't matter anyway.
        //
        for (int i = 0; i < MAX_TABLE; i++) {
            shiftTable[i] = backShiftTable[i] = minLen;
        }

        for (int i = 0; i < normLen-1; i++) {
            shiftTable[hash(valueList[i])] = Math.max(minLen - i - 1, 1);
        }
        shiftTable[hash(valueList[normLen-1])] = 1;
        
        for (int i = normLen - 1; i > 0; i--) {
            backShiftTable[hash(valueList[i])] = i;
        }
        backShiftTable[hash(valueList[0])] = 1;

        if (DEBUG) dumpTables();
    }

    /**
     * Method used by StringSearch to determine how far to the right to
     * shift the pattern during a Boyer-Moore search.  
     *
     * @param curValue  The current value in the target text
     * @param curIndex  The index in the pattern at which we failed to match
     *                  curValue in the target text.
     */
    private int getShift( int curValue, int curIndex ) {
        int shiftAmt = shiftTable[hash(curValue)];

        if (minLen != maxLen) {
            int adjust = normLen - curIndex;
            if (shiftAmt > adjust + 1) {
                if (DEBUG) debug("getShift: adjusting by " + adjust);
                shiftAmt -= adjust;
            }
        }
        return shiftAmt;
    }

    /**
     * Method used by StringSearch to determine how far to the left to
     * shift the pattern during a reverse Boyer-Moore search.  
     *
     * @param curValue  The current value in the target text
     * @param curIndex  The index in the pattern at which we failed to match
     *                  curValue in the target text.
     */
    private int getBackShift( int curValue, int curIndex ) {
        int shiftAmt = backShiftTable[hash(curValue)];

        if (minLen != maxLen) {
            int adjust = normLen - (minLen - curIndex);
            if (shiftAmt > adjust + 1) {
                if (DEBUG) debug("getBackShift: adjusting by " + adjust);
                shiftAmt -= adjust;
            }
        }
        return shiftAmt;
    }

    /**
     * Hash a collation element from its full size (32 bits) down into a
     * value that can be used as an index into the shift tables.  Right
     * now we do a modulus by the size of the hash table.
     *
     * TODO: At some point I should experiment to see whether a slightly
     * more complicated hash function gives us a better distribution
     * on multilingual text.  I doubt it will have much effect on
     * performance, though.
     */
    private static final int hash(int order) {
        return CollationElementIterator.primaryOrder(order) % MAX_TABLE;
    }


    //-------------------------------------------------------------------------
    // Debugging support...
    //-------------------------------------------------------------------------

    static private final boolean DEBUG = false;

    static void debug(String str) {
        System.out.println(str);
    }

    void dumpTables() {
        for (int i = 0; i < MAX_TABLE; i++) {
            if (shiftTable[i] != minLen) {
                debug("shift[" + Integer.toString(i,16) + "] = " + shiftTable[i]);
            }
        }
        for (int i = 0; i < MAX_TABLE; i++) {
            if (backShiftTable[i] != minLen) {
                debug("backShift[" + Integer.toString(i,16) + "] = " + backShiftTable[i]);
            }
        }
    }
};
