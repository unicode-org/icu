/**
*******************************************************************************
* Copyright (C) 1996-2004, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/

package com.ibm.icu.util;

import java.util.Enumeration;
import java.util.NoSuchElementException;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UTF16;

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
     * @param returndelims flag indicating whether to return the delimiters 
     *        as tokens.
     * @exception throws a NullPointerException if str is null
     * @stable ICU 2.4
     */
    public StringTokenizer(String str, UnicodeSet delim, boolean returndelims)
    {
        m_source_ = str;
        m_length_ = str.length();
        if (delim == null) {
            m_delimiters_ = EMPTY_DELIMITER_;
        }
        else {
            m_delimiters_ = delim;   
        }
        m_returnDelimiters_ = returndelims;
        m_tokenOffset_ = -1;
        m_tokenSize_ = -1;
        if (m_length_ == 0) {
            // string length 0, no tokens
            m_nextOffset_ = -1;
        }
        else {
            m_nextOffset_ = 0;
            if (!returndelims) {
                m_nextOffset_ = getNextNonDelimiter(0);
            }
        }
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
    public StringTokenizer(String str, UnicodeSet delim)
    {
        this(str, delim, false);
    }
       
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
     * @param returndelims flag indicating whether to return the delimiters 
     *        as tokens.
     * @exception throws a NullPointerException if str is null
     * @stable ICU 2.4
     */
    public StringTokenizer(String str, String delim, boolean returndelims)
    {
        // don't ignore whitespace
        m_delimiters_ = EMPTY_DELIMITER_;
        if (delim != null && delim.length() > 0) {
            m_delimiters_ = new UnicodeSet();
            m_delimiters_.addAll(delim);
        }
        m_source_ = str;
        m_length_ = str.length();
        m_returnDelimiters_ = returndelims;
        m_tokenOffset_ = -1;
        m_tokenSize_ = -1;
        if (m_length_ == 0) {
            // string length 0, no tokens
            m_nextOffset_ = -1;
        }
        else {
            m_nextOffset_ = 0;
            if (!returndelims) {
                m_nextOffset_ = getNextNonDelimiter(0);
            }
        }
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
    public StringTokenizer(String str, String delim)
    {
        // don't ignore whitespace
        this(str, delim, false);
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
    public StringTokenizer(String str) 
    {
        this(str, DEFAULT_DELIMITERS_, false);
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
    public boolean hasMoreTokens() 
    {
        return m_nextOffset_ >= 0;
    }
    
    /**
     * Returns the next token from this string tokenizer.
     * @return the next token from this string tokenizer.
     * @exception NoSuchElementException if there are no more tokens in 
     *            this tokenizer's string.
     * @stable ICU 2.4
     */
    public String nextToken() 
    {
        if (m_tokenOffset_ < 0) {
            if (m_nextOffset_ < 0) {
                throw new NoSuchElementException("No more tokens in String");   
            }
            // pre-calculations of tokens not done
            if (m_returnDelimiters_) {
                int tokenlimit = 0;
                if (m_delimiters_.contains(UTF16.charAt(m_source_, 
                                                        m_nextOffset_))) {
                    tokenlimit = getNextNonDelimiter(m_nextOffset_);
                }
                else {
                    tokenlimit = getNextDelimiter(m_nextOffset_);
                }
                String result;
                if (tokenlimit < 0) {
                    result = m_source_.substring(m_nextOffset_);
                }
                else {
                    result = m_source_.substring(m_nextOffset_, tokenlimit);
                }
                m_nextOffset_ = tokenlimit;
                return result;
            }
            else {
                int tokenlimit = getNextDelimiter(m_nextOffset_);
                String result;
                if (tokenlimit < 0) {
                    result = m_source_.substring(m_nextOffset_);
                    m_nextOffset_ = tokenlimit;
                }
                else {
                    result = m_source_.substring(m_nextOffset_, tokenlimit);
                    m_nextOffset_ = getNextNonDelimiter(tokenlimit);
                }
                
                return result;
            }
        }
        // count was called before and we have all the tokens
        if (m_tokenOffset_ >= m_tokenSize_) {
            throw new NoSuchElementException("No more tokens in String");
        }
        String result;
        if (m_tokenLimit_[m_tokenOffset_] >= 0) {
            result = m_source_.substring(m_tokenStart_[m_tokenOffset_],
                                         m_tokenLimit_[m_tokenOffset_]);
        }
        else {
            result = m_source_.substring(m_tokenStart_[m_tokenOffset_]);
        }
        m_tokenOffset_ ++;
        m_nextOffset_ = m_tokenStart_[m_tokenOffset_];
        return result;
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
    public String nextToken(String delim) 
    {
        m_delimiters_ = EMPTY_DELIMITER_;
        if (delim != null && delim.length() > 0) {
            m_delimiters_ = new UnicodeSet();
            m_delimiters_.addAll(delim);
        }
        return nextToken(m_delimiters_);
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
    public String nextToken(UnicodeSet delim) 
    {
        m_delimiters_ = delim;
        m_tokenOffset_ = -1;
        m_tokenSize_ = -1;
        if (!m_returnDelimiters_) {
            m_nextOffset_ = getNextNonDelimiter(m_nextOffset_);
        }
        return nextToken();
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
    public boolean hasMoreElements() 
    {
        return hasMoreTokens();
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
    public Object nextElement() 
    {
        return nextToken();
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
    public int countTokens() 
    {
        int result = 0;
        if (hasMoreTokens()) {
            if (m_tokenOffset_ >= 0) {
                return m_tokenSize_ - m_tokenOffset_;
            }
            if (m_tokenStart_ == null) {
                m_tokenStart_ = new int[TOKEN_SIZE_];
                m_tokenLimit_ = new int[TOKEN_SIZE_];
            }
            do {
                if (m_tokenStart_.length == result) {
                    int temptokenindex[] = m_tokenStart_;
                    int temptokensize[] = m_tokenLimit_;
                    int originalsize = temptokenindex.length;
                    int newsize = originalsize + TOKEN_SIZE_;
                    m_tokenStart_ = new int[newsize];
                    m_tokenLimit_ = new int[newsize];
                    System.arraycopy(temptokenindex, 0, m_tokenStart_, 0, 
                                     originalsize);
                    System.arraycopy(temptokensize, 0, m_tokenLimit_, 0, 
                                     originalsize);
                }
                m_tokenStart_[result] = m_nextOffset_;
                if (m_returnDelimiters_) {
                    if (m_delimiters_.contains(UTF16.charAt(m_source_, 
                                                            m_nextOffset_))) {
                        m_tokenLimit_[result] = getNextNonDelimiter(
                                                                m_nextOffset_);
                    }
                    else {
                        m_tokenLimit_[result] = getNextDelimiter(m_nextOffset_);
                    }
                    m_nextOffset_ = m_tokenLimit_[result];
                }
                else {
                    m_tokenLimit_[result] = getNextDelimiter(m_nextOffset_);
                    m_nextOffset_ = getNextNonDelimiter(m_tokenLimit_[result]);
                }
                result ++;
            } while (m_nextOffset_ >= 0);
            m_tokenOffset_ = 0;
            m_tokenSize_ = result;
            m_nextOffset_ = m_tokenStart_[0];
        }
        return result;
    }
    
    // private data members -------------------------------------------------
    
    /**
     * Current offset to the token array. If the array token is not set up yet,
     * this value is a -1
     */
    private int m_tokenOffset_;
    /**
     * Size of the token array. If the array token is not set up yet,
     * this value is a -1
     */
    private int m_tokenSize_;
    /**
     * Array of pre-calculated tokens start indexes in source string terminated 
     * by -1.
     * This is only set up during countTokens() and only stores the remaining
     * tokens, not all tokens including parsed ones
     */
    private int m_tokenStart_[];
    /**
     * Array of pre-calculated tokens limit indexes in source string.
     * This is only set up during countTokens() and only stores the remaining
     * tokens, not all tokens including parsed ones
     */
    private int m_tokenLimit_[];
    /**
     * UnicodeSet containing delimiters
     */
    private UnicodeSet m_delimiters_;
    /**
     * String to parse for tokens
     */
    private String m_source_;
    /**
     * Length of m_source_
     */
    private int m_length_;
    /**
     * Current position in string to parse for tokens
     */
    private int m_nextOffset_;
    /**
     * Flag indicator if delimiters are to be treated as tokens too
     */
    private boolean m_returnDelimiters_;
    /**
     * Default set of delimiters &#92;t&#92;n&#92;r&#92;f
     */
    private static final UnicodeSet DEFAULT_DELIMITERS_ 
                                        = new UnicodeSet("[ \t\n\r\f]", false);
    /**
     * Array size increments
     */
    private static final int TOKEN_SIZE_ = 100;
    /**
     * A empty delimiter UnicodeSet, used when user specified null delimiters
     */
    private static final UnicodeSet EMPTY_DELIMITER_ = new UnicodeSet();
    
    // private methods ------------------------------------------------------
    
    /**
     * Gets the index of the next delimiter after offset
     * @param offset to the source string
     * @return offset of the immediate next delimiter, otherwise 
     *         (- source string length - 1) if there
     *         are no more delimiters after m_nextOffset
     */
    private int getNextDelimiter(int offset)
    {
        if (offset >= 0) {
            int result = offset; 
            int c = 0;
            do {
                c = UTF16.charAt(m_source_, result);
                if (m_delimiters_.contains(c)) {
                    break;
                }
                result ++;
            } while (result < m_length_);
            if (result < m_length_) {
                return result;
            }
        }
        return -1 - m_length_;
    }
    
    /**
     * Gets the index of the next non-delimiter after m_nextOffset_
     * @param offset to the source string
     * @return offset of the immediate next non-delimiter, otherwise 
     *         (- source string length - 1) if there
     *         are no more delimiters after m_nextOffset
     */
    private int getNextNonDelimiter(int offset)
    {
        if (offset >= 0) {
            int result = offset; 
            int c = 0;
            do {
                c = UTF16.charAt(m_source_, result);
                if (!m_delimiters_.contains(c)) {
                    break;
                }
                result ++;
            } while (result < m_length_);
            if (result < m_length_) {
                return result;
            }
        }
        return -1 - m_length_;
    }
}
