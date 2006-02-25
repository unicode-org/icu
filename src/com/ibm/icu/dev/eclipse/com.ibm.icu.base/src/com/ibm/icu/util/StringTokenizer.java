/*
 *******************************************************************************
 * Copyright (C) 1996-2006, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */


package com.ibm.icu.util;

import java.util.Enumeration;

/**
 * <p>The string tokenizer class allows an application to break a string 
 * into tokens by performing code point comparison. 
 * The <code>StringTokenizer</code> methods do not distinguish 
 * among identifiers, numbers, and quoted strings, nor do they recognize 
 * and skip comments.</p>
 * <p>
 * The set of delimiters (the codepoints that separate tokens) may be 
 * specified either at creation time or on a per-token basis. 
 * </p>
 * <p>
 * An instance of <code>StringTokenizer</code> behaves in one of two ways, 
 * depending on whether it was created with the <code>returnDelims</code> 
 * flag having the value <code>true</code> or <code>false</code>: 
 * <ul>
 * <li>If the flag is <code>false</code>, delimiter code points serve to 
 * separate tokens. A token is a maximal sequence of consecutive 
 * code points that are not delimiters. 
 * <li>If the flag is <code>true</code>, delimiter code points are 
 * themselves considered to be tokens. A token is thus either one 
 * delimiter code point, or a maximal sequence of consecutive code 
 * point that are not delimiters.
 * </ul>
 * </p>
 * <p>
 * A <tt>StringTokenizer</tt> object internally maintains a current 
 * position within the string to be tokenized. Some operations advance this 
 * current position past the code point processed.
 * </p>
 * <p>
 * A token is returned by taking a substring of the string that was used to 
 * create the <tt>StringTokenizer</tt> object.
 * </p>
 * <p>
 * Example of the use of the default delimiter tokenizer.
 * <blockquote><pre>
 * StringTokenizer st = new StringTokenizer("this is a test");
 * while (st.hasMoreTokens()) {
 *     println(st.nextToken());
 *     }
 * </pre></blockquote>
 * </p>
 * <p>
 * prints the following output:
 * <blockquote><pre>
 *     this
 *     is
 *     a
 *     test
 * </pre></blockquote>
 * </p>
 * <p>
 * Example of the use of the tokenizer with user specified delimiter.
 * <blockquote><pre>
 *     StringTokenizer st = new StringTokenizer(
 *     "this is a test with supplementary characters &#92;ud800&#92;ud800&#92;udc00&#92;udc00",
 *         " &#92;ud800&#92;udc00");
 *     while (st.hasMoreTokens()) {
 *         println(st.nextToken());
 *     }
 * </pre></blockquote>
 * </p>
 * <p>
 * prints the following output:
 * <blockquote><pre>
 *     this
 *     is
 *     a
 *     test
 *     with
 *     supplementary
 *     characters
 *     &#92;ud800
 *     &#92;udc00
 * </pre></blockquote>
 * </p>
 * @author syn wee
 * @stable ICU 2.4
 */
public final class StringTokenizer implements Enumeration 
{
	/**
	 * @internal
	 */
	public final java.util.StringTokenizer tokenizer;
	
	/**
	 * @internal
	 */
	public StringTokenizer(java.util.StringTokenizer delegate) {
		this.tokenizer = delegate;
	}
	
    // public constructors ---------------------------------------------
     
    /**
     * <p>Constructs a string tokenizer for the specified string. All 
     * characters in the delim argument are the delimiters for separating 
     * tokens.</p> 
     * <p>If the returnDelims flag is true, then the delimiter characters 
     * are also returned as tokens. Each delimiter is returned as a string 
     * of length one. If the flag is false, the delimiter characters are 
     * skipped and only serve as separators between tokens.</p>
     * @param str a string to be parsed.
     * @param delim the delimiters.
     * @param returnDelims flag indicating whether to return the delimiters 
     *        as tokens.
     * @exception throws a NullPointerException if str is null
     * @stable ICU 2.4
     */
    public StringTokenizer(String str, String delim, boolean returnDelims) {
    	this(new java.util.StringTokenizer(str, delim, returnDelims));
    }
    
    /**
     * <p>Constructs a string tokenizer for the specified string. The 
     * characters in the delim argument are the delimiters for separating 
     * tokens.</p> 
     * <p>Delimiter characters themselves will not be treated as tokens.</p>
     * @param str a string to be parsed.
     * @param delim the delimiters.
     * @exception throws a NullPointerException if str is null
     * @stable ICU 2.4
     */
    public StringTokenizer(String str, String delim) {
        this(new java.util.StringTokenizer(str, delim));
    }

    /**
     * <p>Constructs a string tokenizer for the specified string. 
     * The tokenizer uses the default delimiter set, which is 
     * " &#92;t&#92;n&#92;r&#92;f": 
     * the space character, the tab character, the newline character, the 
     * carriage-return character, and the form-feed character.</p> 
     * <p>Delimiter characters themselves will not be treated as tokens.</p>
     * @param str a string to be parsed
     * @exception throws a NullPointerException if str is null
     * @stable ICU 2.4
     */
    public StringTokenizer(String str) {
        this(new java.util.StringTokenizer(str));
    }
    
    // public methods --------------------------------------------------
    
    /**
     * Tests if there are more tokens available from this tokenizer's 
     * string. 
     * If this method returns <tt>true</tt>, then a subsequent call to 
     * <tt>nextToken</tt> with no argument will successfully return a token.
     * @return <code>true</code> if and only if there is at least one token 
     *         in the string after the current position; <code>false</code> 
     *         otherwise.
     * @stable ICU 2.4
     */
    public boolean hasMoreTokens() {
        return tokenizer.hasMoreTokens();
    }
    
    /**
     * Returns the next token from this string tokenizer.
     * @return the next token from this string tokenizer.
     * @exception NoSuchElementException if there are no more tokens in 
     *            this tokenizer's string.
     * @stable ICU 2.4
     */
    public String nextToken() {
    	return tokenizer.nextToken();
    }
    
    /**
     * Returns the next token in this string tokenizer's string. First, 
     * the set of characters considered to be delimiters by this 
     * <tt>StringTokenizer</tt> object is changed to be the characters in 
     * the string <tt>delim</tt>. Then the next token in the string
     * after the current position is returned. The current position is 
     * advanced beyond the recognized token.  The new delimiter set 
     * remains the default after this call. 
     * @param delim the new delimiters.
     * @return the next token, after switching to the new delimiter set.
     * @exception NoSuchElementException if there are no more tokens in 
     *            this tokenizer's string.
     * @stable ICU 2.4
     */
    public String nextToken(String delim) {
    	return tokenizer.nextToken(delim);
    }
    
    /**
     * Returns the same value as the <code>hasMoreTokens</code> method. 
     * It exists so that this class can implement the 
     * <code>Enumeration</code> interface. 
     * @return <code>true</code> if there are more tokens;
     *         <code>false</code> otherwise.
     * @see #hasMoreTokens()
     * @stable ICU 2.4
     */
    public boolean hasMoreElements() {
        return tokenizer.hasMoreElements();
    }
    
    /**
     * Returns the same value as the <code>nextToken</code> method, except 
     * that its declared return value is <code>Object</code> rather than 
     * <code>String</code>. It exists so that this class can implement the 
     * <code>Enumeration</code> interface. 
     * @return the next token in the string.
     * @exception NoSuchElementException if there are no more tokens in 
     *            this tokenizer's string.
     * @see #nextToken()
     * @stable ICU 2.4
     */
    public Object nextElement() {
        return tokenizer.nextElement();
    }
    
    /**
     * Calculates the number of times that this tokenizer's 
     * <code>nextToken</code> method can be called before it generates an 
     * exception. The current position is not advanced.
     * @return the number of tokens remaining in the string using the 
     *         current delimiter set.
     * @see #nextToken()
     * @stable ICU 2.4
     */
    public int countTokens() {
    	return tokenizer.countTokens();
    }
}

