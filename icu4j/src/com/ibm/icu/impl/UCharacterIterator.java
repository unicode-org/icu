/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/impl/Attic/UCharacterIterator.java,v $ 
 * $Date: 2002/04/03 00:00:00 $ 
 * $Revision: 1.4 $
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
public final class UCharacterIterator implements CharacterIterator
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
	 * Public constructor
	 * @param replacable text which the iterator will be based on
	 */
	public UCharacterIterator(Replaceable replaceable)
	{
		m_replaceable_  = replaceable;
		m_index_        = 0;
		m_length_       = replaceable.length();
	}
	
	/**
	 * Public constructor
	 * @param str text which the iterator will be based on
	 */
	public UCharacterIterator(String str)
	{
		m_replaceable_  = new ReplaceableString(str);
		m_index_        = 0;
		m_length_       = m_replaceable_.length();
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
        if (m_index_ >= 0 && m_index_ < m_length_) {
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
        if (m_index_ >= 0 && m_index_ < m_length_) {
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
        m_index_ = 0;
        return current();
    }
    
    /**
     * Returns the start of the text.
     * @return 0
     */
    public int getBeginIndex()
    {
        return 0;
    }

    /**
     * Returns the length of the text
     * @return length of the text
     */
    public int getEndIndex()
    {
        return m_length_;
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
     * Gets the last UTF16 character from the text and shifts the index to the
     * end of the text accordingly.
     * @return the last UTF16 character
     */
    public char last()
    {
        if (m_length_ != 0) {
            m_index_ = m_length_ - 1;
            return m_replaceable_.charAt(m_index_);
        } 
		m_index_ = m_length_;
        return DONE;
    }
    
	/**
     * Returns next UTF16 character and increments the iterator's index by 1. 
	 * If the resulting index is greater or equal to the text length, the 
	 * index is reset to the text length and a value of DONE_CODEPOINT is 
	 * returned. 
	 * @return next UTF16 character in text or DONE if the new index is off the 
	 *         end of the text range.
     */
    public char next()
    {
        if (m_index_ < m_length_) {
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
	 * If the resulting index is greater or equal to the text length, the 
	 * current index is reset to the text length and a value of DONE_CODEPOINT 
	 * is returned. 
	 * @return next codepoint in text or DONE_CODEPOINT if the new index is off the 
	 *         end of the text range.
	 */	
	public int nextCodePoint()
	{
		if (m_index_ < m_length_) {
			char ch = m_replaceable_.charAt(m_index_);
			m_index_ ++;
			if (ch >= UTF16.LEAD_SURROGATE_MIN_VALUE &&
			    ch <= UTF16.LEAD_SURROGATE_MAX_VALUE &&
			    m_index_ < m_length_) {
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
	 * If the resulting index is less than 0, the index is reset to 0 and a 
	 * value of DONE_CODEPOINT is returned. 
	 * @return next UTF16 character in text or DONE if the new index is off the 
	 *         start of the text range.
     */
    public char previous()
    {
        if (m_index_ > 0) {
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
	 * If the resulting index is less than 0, the current index is reset to 0
	 * and a value of DONE_CODEPOINT is returned. 
	 * @return previous codepoint in text or DONE_CODEPOINT if the new index is 
	 *         off the start of the text range.
     */
    public int previousCodePoint()
    {
        if (m_index_ > 0) {
            m_index_ --;
            char ch = m_replaceable_.charAt(m_index_);
			if (ch >= UTF16.TRAIL_SURROGATE_MIN_VALUE &&
			    ch <= UTF16.TRAIL_SURROGATE_MAX_VALUE &&
			    m_index_ > 0) {
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
	 *         index is equal to the end of the text.
	 */
	public char setIndex(int index)
	{
		int length = m_replaceable_.length();
		if (index < 0 || index > length) {
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
	 * Replaceable text length
	 */
	private int m_length_;
}
