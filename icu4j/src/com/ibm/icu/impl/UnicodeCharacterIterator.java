/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/impl/Attic/UnicodeCharacterIterator.java,v $ 
 * $Date: 2002/06/20 01:18:09 $ 
 * $Revision: 1.1 $
 *
 *******************************************************************************
 */
package com.ibm.icu.impl;

import com.ibm.icu.text.Replaceable;
import com.ibm.icu.text.ReplaceableString;
import com.ibm.icu.text.UTF16;
import java.text.CharacterIterator;

/**
 * Internal class that iterates through a com.ibm.text.Replacable text object 
 * to return either Unicode characters.
 * @author synwee
 * @version release 2.1, February 2002
 */
public final class UnicodeCharacterIterator implements CharacterIterator
{
	// public data members -----------------------------------------------------
	
	/**
	 * Indicator that we have reached the ends of the UTF16 text when returning
	 * 16 bit character.
	 */
	public static final int DONE = 0xFFFF;
	/**
	 * Indicator that we have reached the ends of the UTF16 text when returning
	 * codepoints.
	 */
	public static final int DONE_CODEPOINT = -1;
	
	// public constructor ------------------------------------------------------
	
	/**
	 * Public constructor.
	 * By default the iteration range will be from 0 to the end of the text.
	 * @param replacable text which the iterator will be based on
	 */
	public UnicodeCharacterIterator(Replaceable replaceable)
	{
		m_replaceable_  = replaceable;
		m_index_        = 0;
		m_start_        = 0;
		m_limit_        = replaceable.length();
	}
	
	/**
	 * Public constructor
	 * By default the iteration range will be from 0 to the end of the text.
	 * @param str text which the iterator will be based on
	 */
	public UnicodeCharacterIterator(String str)
	{
		m_replaceable_  = new ReplaceableString(str);
		m_index_        = 0;
		m_start_        = 0;
		m_limit_        = m_replaceable_.length();
	}
	
	/**
     * Constructs an iterator over the given range of the given string.
     * @param  text  text to be iterated over
     * @param  start offset of the first character to iterate
     * @param  limit offset of the character following the last character to
     * 					iterate
     */
    public UnicodeCharacterIterator(String str, int start, int limit) 
    {
    	m_replaceable_  = new ReplaceableString(str);
		m_start_        = start;
		m_limit_        = limit;
		m_index_        = m_start_;
    }   
    
    /**
     * Constructs an iterator over the given range of the given replaceable 
     * string.
     * @param  text  text to be iterated over
     * @param  start offset of the first character to iterate
     * @param  limit offset of the character following the last character to
     * 					iterate
     */
    public UnicodeCharacterIterator(Replaceable replaceable, int start, int limit) 
    {
    	m_replaceable_  = replaceable;
		m_start_        = start;
		m_limit_        = limit;
		m_index_        = m_start_;
    }   
	
	// public methods ----------------------------------------------------------
	
	/**
     * Creates a copy of this iterator.
     * Cloning will not duplicate a new Replaceable object.
     * @return copy of this iterator
     */
    public Object clone()
    {
        try {
            return super.clone();
        }
        catch (CloneNotSupportedException e) {
            throw new InternalError(
            "Cloning by the super class java.text.CharacterIterator is not " +
            "supported");
        }
    }
    
	/**
     * Returns the current UTF16 character.
     * @return current UTF16 character
     */
    public char current()
    {
        if (m_index_ >= m_start_ && m_index_ < m_limit_) {
            return m_replaceable_.charAt(m_index_);
        }
        return DONE;
    }
    
    /**
     * Returns the current codepoint
     * @return current codepoint
     */
    public int currentCodePoint()
    {
        if (m_index_ >= m_start_ && m_index_ < m_limit_) {
            return m_replaceable_.char32At(m_index_);
        }
        return DONE_CODEPOINT;
    }
    
    /**
     * Gets the first UTF16 character in text.
     * @return the first UTF16 in text.
     */
    public char first()
    {
        m_index_ = m_start_;
        return current();
    }
    
    /**
     * Returns the start of the text to iterate.
     * @return by default this method will return 0, unless a range for 
     * iteration had been specified during construction.
     */
    public int getBeginIndex()
    {
        return m_start_;
    }

    /**
     * Returns the limit offset of the text to iterate
     * @return by default this method returns the length of the text, unless a 
     * range for iteration had been specified during construction.
     */
    public int getEndIndex()
    {
        return m_limit_;
    }
    
    /**
     * Gets the current index in text.
     * @return current index in text.
     */
    public int getIndex()
    {
        return m_index_;
    }
    
    /**
     * Gets the last UTF16 iterateable character from the text and shifts the 
     * index to the end of the text accordingly.
     * @return the last UTF16 iterateable character
     */
    public char last()
    {
        if (m_limit_ != m_start_) {
            m_index_ = m_limit_ - 1;
            return m_replaceable_.charAt(m_index_);
        } 
		m_index_ = m_limit_;
        return DONE;
    }
    
	/**
     * Returns next UTF16 character and increments the iterator's index by 1. 
	 * If the resulting index is greater or equal to the iteration limit, the 
	 * index is reset to the text iteration limit and a value of DONE_CODEPOINT is 
	 * returned. 
	 * @return next UTF16 character in text or DONE if the new index is off the 
	 *         end of the text iteration limit.
     */
    public char next()
    {
        if (m_index_ < m_limit_) {
        	char result = m_replaceable_.charAt(m_index_);
            m_index_ ++;
            return result;
        }
        return DONE;
    }

	/**
	 * Returns next codepoint after current index and increments the iterator's 
	 * index by a number depending on the returned codepoint. 
	 * This assumes the text is stored as 16-bit code units
     * with surrogate pairs intermixed. If the index of a leading or trailing 
     * code unit of a surrogate pair is given, return the code point after the 
     * surrogate pair.
	 * If the resulting index is greater or equal to the text iterateable limit,
	 * the current index is reset to the text iterateable limit and a value of 
	 * DONE_CODEPOINT is returned. 
	 * @return next codepoint in text or DONE_CODEPOINT if the new index is off the 
	 *         end of the text iterateable limit.
	 */	
	public int nextCodePoint()
	{
		if (m_index_ < m_limit_) {
			char ch = m_replaceable_.charAt(m_index_);
			m_index_ ++;
			if (ch >= UTF16.LEAD_SURROGATE_MIN_VALUE &&
			    ch <= UTF16.LEAD_SURROGATE_MAX_VALUE &&
			    m_index_ < m_limit_) {
			    char trail = m_replaceable_.charAt(m_index_);
			    if (trail >= UTF16.TRAIL_SURROGATE_MIN_VALUE &&
			    	trail <= UTF16.TRAIL_SURROGATE_MAX_VALUE) {
			    	m_index_ ++;
			    	return UCharacterProperty.getRawSupplementary(ch, 
			    	                                              trail);
				}
			}
			return ch;
        }
        return DONE_CODEPOINT;
	}

    /**
     * Returns previous UTF16 character and decrements the iterator's index by 
     * 1. 
	 * If the resulting index is less than the text iterateable limit, the 
	 * index is reset to the start of the text iteration and a value of 
	 * DONE_CODEPOINT is returned. 
	 * @return next UTF16 character in text or DONE if the new index is off the 
	 *         start of the text iteration range.
     */
    public char previous()
    {
        if (m_index_ > m_start_) {
            m_index_ --;
            return m_replaceable_.charAt(m_index_);
        }
        return DONE;
    }
    
    /**
     * Returns previous codepoint before current index and decrements the 
     * iterator's index by a number depending on the returned codepoint. 
	 * This assumes the text is stored as 16-bit code units
     * with surrogate pairs intermixed. If the index of a leading or trailing 
     * code unit of a surrogate pair is given, return the code point before the 
     * surrogate pair.
	 * If the resulting index is less than the text iterateable range, the 
	 * current index is reset to the start of the range and a value of 
	 * DONE_CODEPOINT is returned. 
	 * @return previous codepoint in text or DONE_CODEPOINT if the new index is 
	 *         off the start of the text iteration range.
     */
    public int previousCodePoint()
    {
        if (m_index_ > m_start_) {
            m_index_ --;
            char ch = m_replaceable_.charAt(m_index_);
			if (ch >= UTF16.TRAIL_SURROGATE_MIN_VALUE &&
			    ch <= UTF16.TRAIL_SURROGATE_MAX_VALUE &&
			    m_index_ > m_start_) {
			    char lead = m_replaceable_.charAt(m_index_);
			    if (lead >= UTF16.LEAD_SURROGATE_MIN_VALUE &&
			    	lead <= UTF16.LEAD_SURROGATE_MAX_VALUE) {
			    	m_index_ --;
			    	return UCharacterProperty.getRawSupplementary(ch, 
			    	                                              lead);
				}
			}
   			return ch;
        }
        return DONE_CODEPOINT;
    }

	/**
	 * <p>Sets the index to the specified index in the text and returns that 
	 * single UTF16 character at index. 
	 * This assumes the text is stored as 16-bit code units.</p>
	 * @param index the index within the text. 
	 * @exception IllegalArgumentException is thrown if an invalid index is 
	 *            supplied. i.e. index is out of bounds.
	 * @return the character at the specified index or DONE if the specified 
	 *         index is equal to the limit of the text iteration range.
	 */
	public char setIndex(int index)
	{
		if (index < m_start_ || index > m_limit_) {
			throw new IllegalArgumentException("Index index out of bounds");
		}
		m_index_ = index;
		return current();
	}
	
	// private data members ----------------------------------------------------
	
	/**
	 * Replacable object
	 */
	private Replaceable m_replaceable_;
	/**
	 * Current index
	 */
	private int m_index_;
	/**
	 * Start offset of iterateable range, by default this is 0
	 */
	private int m_start_;
	/**
	 * Limit offset of iterateable range, by default this is the length of the
	 * string
	 */
	private int m_limit_;
}
