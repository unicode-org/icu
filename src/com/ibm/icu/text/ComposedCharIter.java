/*
 *******************************************************************************
 * Copyright (C) 1996-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.text;
import com.ibm.icu.impl.NormalizerImpl;

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
 * @deprecated ICU 2.2
 */
///CLOVER:OFF
public final class ComposedCharIter {
    
    /**
     * Constant that indicates the iteration has completed.
     * {@link #next} returns this value when there are no more composed characters
     * over which to iterate.
     * @deprecated ICU 2.2
     */
    public static final  char DONE = (char) Normalizer.DONE;
    
    /**
     * Construct a new <tt>ComposedCharIter</tt>.  The iterator will return
     * all Unicode characters with canonical decompositions, including Korean
     * Hangul characters.
     * @deprecated ICU 2.2
     */
    public ComposedCharIter() {
        compat = false;
        options =0;
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
     * @deprecated ICU 2.2
     */
    public ComposedCharIter(boolean compat, int options) {
        this.compat = compat;
        this.options = options;
    }
    
    /**
     * Determines whether there any precomposed Unicode characters not yet returned
     * by {@link #next}.
     * @deprecated ICU 2.2
     */
    public boolean hasNext() {
        if (nextChar == Normalizer.DONE)  {
            findNextChar();
        }
        return nextChar != Normalizer.DONE;
    }
    
    /**
     * Returns the next precomposed Unicode character.
     * Repeated calls to <tt>next</tt> return all of the precomposed characters defined
     * by Unicode, in ascending order.  After all precomposed characters have
     * been returned, {@link #hasNext} will return <tt>false</tt> and further calls
     * to <tt>next</tt> will return {@link #DONE}.
     * @deprecated ICU 2.2
     */
    public char next() {
        if (nextChar == Normalizer.DONE)  {
            findNextChar();
        }
        curChar = nextChar;
        nextChar = Normalizer.DONE;
        return (char) curChar;
    }
    
    /**
     * Returns the Unicode decomposition of the current character.
     * This method returns the decomposition of the precomposed character most
     * recently returned by {@link #next}.  The resulting decomposition is
     * affected by the settings of the options passed to the constructor.
     * @deprecated ICU 2.2
     */
    public String decomposition() {
        // the decomposition buffer contains the decomposition of 
        // current char so just return it
        return new String(decompBuf,0, bufLen);
    }
    
    private void findNextChar() {
        int c=curChar+1;
        for(;;){
           if(c < 0xFFFF){
               bufLen = NormalizerImpl.getDecomposition(c,compat,
                                                        decompBuf,0,
                                                        decompBuf.length);
               if(bufLen>0){
                    // the curChar can be decomposed... so it is a composed char
                    // cache the result     
                    break;
               }
               c++;
           }else{
               c=Normalizer.DONE;
               break;
           }
        }
        nextChar=c;  
    }
    
    private int options;
    private boolean compat;
    private char[] decompBuf = new char[100];
    private int bufLen=0;
    private int curChar = 0;
    private int nextChar = Normalizer.DONE;
    

};
