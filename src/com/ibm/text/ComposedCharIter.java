/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/text/Attic/ComposedCharIter.java,v $ 
 * $Date: 2000/03/10 04:07:19 $ 
 * $Revision: 1.2 $
 *
 *****************************************************************************************
 */
package com.ibm.text;

/**
 * <tt>ComposedCharIter</tt> is an iterator class that returns all
 * of the precomposed characters defined in the Unicode standard, along
 * with their decomposed forms.  This is often useful when building
 * data tables (<i>e.g.</i> collation tables) which need to treat composed
 * and decomposed characters equivalently.
 * <p>
 * For example, imagine that you have built a collation table with ordering
 * rules for the {@link Normalizer#DECOMP canonically decomposed} forms of all
 * characters used in a particular language.  When you process input text using
 * this table, the text must first be decomposed so that it matches the form
 * used in the table.  This can impose a performance penalty that may be
 * unacceptable in some situations.
 * <p>
 * You can avoid this problem by ensuring that the collation table contains
 * rules for both the decomposed <i>and</i> composed versions of each character.
 * To do so, use a <tt>ComposedCharIter</tt> to iterate through all of the
 * composed characters in Unicode.  If the decomposition for that character
 * consists solely of characters that are listed in your ruleset, you can
 * add a new rule for the composed character that makes it equivalent to
 * its decomposition sequence.
 * <p>
 * Note that <tt>ComposedCharIter</tt> iterates over a <em>static</em> table
 * of the composed characters in Unicode.  If you want to iterate over the
 * composed characters in a particular string, use {@link Normalizer} instead.
 * <p>
 * When constructing a <tt>ComposedCharIter</tt> there is one
 * optional feature that you can enable or disable:
 * <ul>
 *   <li>{@link Normalizer#IGNORE_HANGUL} - Do not iterate over the Hangul
 *          characters and their corresponding Jamo decompositions.
 *          This option is off by default (<i>i.e.</i> Hangul processing is enabled)
 *          since the Unicode standard specifies that Hangul to Jamo 
 *          is a canonical decomposition.
 * </ul>
 * <p>
 * <tt>ComposedCharIter</tt> is currently based on version 2.1.8 of the
 * <a href="http://www.unicode.org" target="unicode">Unicode Standard</a>.
 * It will be updated as later versions of Unicode are released.
 */
public final class ComposedCharIter {
    
    /**
     * Constant that indicates the iteration has completed.
     * {@link #next} returns this value when there are no more composed characters
     * over which to iterate.
     */
    public static final char DONE = Normalizer.DONE;
    
    /**
     * Construct a new <tt>ComposedCharIter</tt>.  The iterator will return
     * all Unicode characters with canonical decompositions, including Korean
     * Hangul characters.
     */
    public ComposedCharIter() {
        minDecomp = DecompData.MAX_COMPAT;
        hangul = false;
    }
    
    
    /**
     * Constructs a non-default <tt>ComposedCharIter</tt> with optional behavior.
     * <p>
     * @param compat    <tt>false</tt> for canonical decompositions only;
     *                  <tt>true</tt> for both canonical and compatibility
     *                  decompositions.
     *
     * @param options   Optional decomposition features.  Currently, the only
     *                  supported option is {@link Normalizer#IGNORE_HANGUL}, which
     *                  causes this <tt>ComposedCharIter</tt> not to iterate
     *                  over the Hangul characters and their corresponding
     *                  Jamo decompositions.
     */
    public ComposedCharIter(boolean compat, int options) {
        // Compatibility explosions have lower indices; skip them if necessary
        minDecomp = compat ? 0 : DecompData.MAX_COMPAT;
        
        hangul = (options & Normalizer.IGNORE_HANGUL) == 0;
    }
    
    /**
     * Determines whether there any precomposed Unicode characters not yet returned
     * by {@link #next}.
     */
    public boolean hasNext() {
        if (nextChar == DONE)  {
            findNextChar();
        }
        return nextChar != DONE;
    }
    
    /**
     * Returns the next precomposed Unicode character.
     * Repeated calls to <tt>next</tt> return all of the precomposed characters defined
     * by Unicode, in ascending order.  After all precomposed characters have
     * been returned, {@link #hasNext} will return <tt>false</tt> and further calls
     * to <tt>next</tt> will return {@link #DONE}.
     */
    public char next() {
        if (nextChar == DONE)  {
            findNextChar();
        }
        curChar = nextChar;
        nextChar = DONE;
        return curChar;
    }
    
    /**
     * Returns the Unicode decomposition of the current character.
     * This method returns the decomposition of the precomposed character most
     * recently returned by {@link #next}.  The resulting decomposition is
     * affected by the settings of the options passed to the constructor.
     */
    public String decomposition() {
        StringBuffer result = new StringBuffer();
        
        int pos = (char)(DecompData.offsets.elementAt(curChar) & DecompData.DECOMP_MASK);
        
        if (pos > minDecomp) {
            Normalizer.doAppend(DecompData.contents, pos, result);
            
            
        } else if (hangul && curChar >= HANGUL_BASE && curChar < HANGUL_LIMIT) {
            Normalizer.hangulToJamo(curChar, result, minDecomp);
        } else {
            result.append(curChar);
        }
        return result.toString();
    }
    
    private void findNextChar() {
        if (curChar != DONE) {
            char ch = curChar;
            while (++ch < 0xFFFF) {
                int offset = DecompData.offsets.elementAt(ch) & DecompData.DECOMP_MASK;
                if (offset > minDecomp
                    || (hangul && ch >= HANGUL_BASE && ch < HANGUL_LIMIT) ) {
                    nextChar = ch;
                    break;
                }
            }
        }
    }
    
    private final int minDecomp;
    private final boolean hangul;
    
    private char curChar = 0;
    private char nextChar = Normalizer.DONE;
    
    private static final char HANGUL_BASE = Normalizer.HANGUL_BASE;
    private static final char HANGUL_LIMIT = Normalizer.HANGUL_LIMIT;
};
