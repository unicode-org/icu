
package com.ibm.icu.text;

import java.text.CharacterIterator;
import com.ibm.icu.impl.NormalizerImpl;
import com.ibm.icu.impl.UCharacterProperty;

/**
 * <p>
 * The <code>CollationElementIterator</code> object is an iterator created
 * by a RuleBasedCollator to walk through an international string. The return
 * result of each iteration is a 32 bit collation element that defines the 
 * ordering priority of the next sequence of characters in the source string. 
 * </p>
 * <p>For better illustration, consider the following in Spanish:
 * <blockquote>
 * <pre>
 * "ca" -> the first collation element is collation_element('c') and second 
 *         collation element is collation_element('a').
 * 
 * Since "ch" in Spanish sorts as one entity, the below example returns one
 * collation element for the 2 characters 'c' and 'h'
 * 
 * "cha" -> the first collation element is collation_element('ch') and second 
 *          collation element is collation_element('a').
 * </pre>
 * </blockquote>
 * And in German,
 * <blockquote>
 * <pre>
 * Since the character '&#230;' is a composed character of 'a' and 'e', the
 * below example returns 2 collation elements for the single character 
 * '&#230;'
 * 
 * "&#230;b" -> the first collation element is collation_element('a'), the 
 *              second collation element is collation_element('e'), and the 
 *              third collation element is collation_element('b').
 * </pre>
 * </blockquote>
 * </p>
 * <p>
 * For collation ordering comparison, the collation element results can not be 
 * compared simply by using basic arithmetric operators, e.g. &lt;, == or &gt;, 
 * further processing has to be done. Details can be found in the ICU
 * <a href=http://oss.software.ibm.com/icu/userguide/Collate_ServiceArchitecture.html>
 * user guide</a>. An example of using the CollationElementIterator for
 * collation ordering comparison is the class <a href=StringSearch.html>
 * com.ibm.icu.text.StringSearch</a>.
 * </p>
 * <p>
 * To construct a CollationElementIterator object, users would have to call the 
 * factory method getCollationElementIterator() in a RuleBasedCollator object
 * that defines the sorting order that is desired.
 * </p>
 * <p>
 * Example:
 * <blockquote>
 * <pre>
 *  String testString = "This is a test";
 *  RuleBasedCollator rbc = new RuleBasedCollator("&amp;a&lt;b");
 *  CollationElementIterator collationElementIterator = rbc.getCollationElementIterator(testString);
 *  int primaryOrder = CollationElementIterator.IGNORABLE;
 *  while (primaryOrder != CollationElementIterator.NULLORDER) {
 *      int order = collationElementIterator.next();
 *      if (order != CollationElementIterator.IGNORABLE &&
 *          order != CollationElementIterator.NULLORDER) {
 *          // order is valid, not ignorable and we have not passed the end
 *          // of the iteration, we do something
 *          primaryOrder = CollationElementIterator.primaryOrder(order);
 *          System.out.println("Next primary order 0x" + Integer.toHexString(primaryOrder));
 *      }
 *  }
 * </pre>
 * </blockquote>
 * </p>
 * <p>
 * @see Collator
 * @see RuleBasedCollator
 * @see StringSearch
 * @author Syn Wee Quek
 * @since release 2.2, April 18 2002
 * @draft 2.2
 */
public final class CollationElementIterator 
{
	// public data members --------------------------------------------------
	
    /**
     * <p>This constant is returned by the iterator in the methods next() and
     * previous() when the end or the beginning of the source string has been
     * reached, and there are no more valid collation elements to return.</p>
     * <p>See class documentation for an example of use.</p>
     * @draft 2.2
     * @see #next
     * @see #previous
     */
    public final static int NULLORDER = 0xffffffff;
    /**
     * <p>This constant is returned by the iterator in the methods next() and
     * previous() when a collation element result is to be ignored.</p>
     * <p>See class documentation for an example of use.</p>
     * @draft 2.2
     * @see #next
     * @see #previous
     */
    public static final int IGNORABLE = 0;

	// public methods -------------------------------------------------------
	
	// public getters -------------------------------------------------------
	
	/**
     * <p>Returns the character offset in the source string corresponding to 
     * the next collation element. i.e. getOffset() returns the position 
     * in source string corresponding to the collation element that will be 
     * returned by the next call to next(). This value could be either
     * <ul>
     * <li> Index of the <b>first</b> character corresponding to the next
     * collation element. This means that if <code>setOffset(offset)</code> 
     * sets the index in the middle of a contraction, <code>getOffset()</code>
     * returns the index of the first character in the contraction, which
     * may not be equals to the original offset that was set. Hence calling
     * getOffset() immediately after setOffset(offset) does not guarantee that
     * the original offset set will be returned.
     * <li> If normalization is on, <code>getOffset()</code> may return the 
     * index of the <b>immediate</b> subsequent character, or composite 
     * character with the first character, having a combining class of 0.
     * <li> the length of the source string if iteration has reached the end.
     * </ul>
     * </p>
     * @return The character offset in the source string corresponding to the 
     *         collation element that will be returned by the next call to 
     *         next().
     * @draft 2.2
     */
    public int getOffset()
    {
    	if (m_bufferOffset_ != -1) {
    		if (m_isForwards_) {
    			return m_FCDLimit_;
    		}
    		return m_FCDStart_;
    	}
        return m_source_.getIndex();
    }


    /**
     * <p>
     * Returns the maximum length of any expansion sequence that ends with 
     * the argument collation element ce. If there is no expansion with the 
     * argument ce as the last element, 1 is returned.
     * </p>
     * @param ce a collation element returned by previous() or next().
     * @return the maximum length of any expansion sequences ending
     *         with the specified collation element.
     * @draft 2.2
     */
    public int getMaxExpansion(int ce)
    {
        int start = 0;                                  
  		int limit = m_collator_.m_expansionEndCE_.length;
  		long unsignedce = ce & 0xFFFFFFFFl;
  		while (start < limit - 1) {
    		int mid = start + ((limit - start) >> 1);              
    		long midce = m_collator_.m_expansionEndCE_[mid] & 0xFFFFFFFFl;
    		if (unsignedce <= midce) {              
      			limit = mid;                                              
    		}                                                             
    		else {                                                        
      			start = mid;                                              
    		}                                                             
  		}          
  		int result = 1;                                                       
  		if (m_collator_.m_expansionEndCE_[start] == ce) {
    		result = m_collator_.m_expansionEndCEMaxSize_[start];
  		}                                                                
  		else if (limit < m_collator_.m_expansionEndCE_.length &&
  		         m_collator_.m_expansionEndCE_[limit] == ce) {           
         	result = m_collator_.m_expansionEndCEMaxSize_[limit]; 
       	}                                  
       	else if ((ce & 0xFFFF) == 0x00C0) {
            result = 2;                                                    
       	}                                                                
    	return result;    
    }

	// public other methods -------------------------------------------------
	
	/**
     * <p>
     * Resets the cursor to the beginning of the string. The next call
     * to next() and previous will return the first and last collation element 
     * in the string respectively. 
     * </p>
     * <p> 
     * If the RuleBasedCollator used in this iterator has its attributes 
     * changed, calling reset() will reinitialize the iterator to use the new
     * RuleBasedCollator attributes.
     * </p>
     * @draft 2.2
     */
    public void reset()
    {
    	m_source_.setIndex(m_source_.getBeginIndex());
    	updateInternalState();
    }

    /**
     * <p>
     * Get the next collation element in the source string.
     * </p>  
     * <p>
     * This iterator iterates over a sequence of collation elements that were 
     * built from the string. Because there isn't necessarily a one-to-one 
     * mapping from characters to collation elements, this doesn't mean the 
     * same thing as "return the collation element [or ordering priority] of 
     * the next character in the string".
     * </p>
     * <p>
     * This function returns the collation element that the iterator is 
     * currently pointing to and then updates the internal pointer to point to 
     * the next element. previous() updates the pointer first and then 
     * returns the element. This means that when you change direction while 
     * iterating (i.e., call next() and then call previous(), or call 
     * previous() and then call next()), you'll get back the same element 
     * twice.
     * </p>
     * @return the next collation element or NULLORDER if the end of the 
     *         iteration has been reached.
     * @draft 2.2
     */
    public int next()
    {
    	m_isForwards_ = true;
        if (m_CEBufferSize_ > 0) { 
        	if (m_CEBufferOffset_ < m_CEBufferSize_) { 
	    		// if there are expansions left in the buffer, we return it
	      		return m_CEBuffer_[m_CEBufferOffset_ ++];
        	}
        	m_CEBufferSize_ = 0;
        	m_CEBufferOffset_ = 0;
	    }
	
		char ch = nextChar();    
		/* System.out.println("ch " + Integer.toHexString(ch) + " " + 
								Integer.toHexString(m_source_.current()));*/
		if (ch == CharacterIterator.DONE) {
	        return NULLORDER;
	    }
	    if (m_collator_.m_isHiragana4_) {
	       	m_isCodePointHiragana_ = (ch >= 0x3040 && ch <= 0x3094) 
	       	                         || ch == 0x309d || ch == 0x309e;
	    }
	    
	    int result = NULLORDER;
	    if (ch <= 0xFF) {
	        // For latin-1 characters we never need to fall back to the UCA 
	        // table because all of the UCA data is replicated in the 
	        // latinOneMapping array
	        result = m_collator_.m_trie_.getLatin1LinearValue(ch);
	        if (RuleBasedCollator.isSpecial(result)) {
	            result = nextSpecial(m_collator_, result, ch);
	        }
	    }
	    else
	    {
	        result = m_collator_.m_trie_.getLeadValue(ch);
	        //System.out.println(Integer.toHexString(result));
	        if (RuleBasedCollator.isSpecial(result)) {               
	        	// surrogate leads are handled as special ces
	        	result = nextSpecial(m_collator_, result, ch);
	        }
	        if (result == CE_NOT_FOUND_) {   
	            // couldn't find a good CE in the tailoring
	            // if we got here, the codepoint MUST be over 0xFF - so we look 
	            // directly in the UCA
	            result = m_collator_.UCA_.m_trie_.getLeadValue(ch);
	            if (RuleBasedCollator.isSpecial(result)) { 
	            	// UCA also gives us a special CE
	              	result = nextSpecial(m_collator_.UCA_, result, ch);
	            }
	        }
	    }
	    return result; 
    }

    /**
     * <p>
     * Get the previous collation element in the source string.
     * </p>  
     * <p>
     * This iterator iterates over a sequence of collation elements that 
     * were built from the string. Because there isn't necessarily a 
     * one-to-one mapping from characters to collation elements, this doesn't 
     * mean the same thing as "return the collation element [or ordering 
     * priority] of the previous character in the string".
     * </p>
     * <p>
     * This function updates the iterator's internal pointer to point to 
     * the collation element preceding the one it's currently pointing to and 
     * then returns that element, while next() returns the current element and 
     * then updates the pointer. This means that when you change direction 
     * while iterating (i.e., call next() and then call previous(), or call 
     * previous() and then call next()), you'll get back the same element 
     * twice.
     * </p>
     * @return the previous collation element, or NULLORDER when the start of 
     * 		   the iteration has been reached.
     * @draft 2.2
     */
    public int previous()
    {
    	if (m_source_.getIndex() <= 0 && m_isForwards_) {
    		// if iterator is new or reset, we can immediate perform  backwards
    		// iteration even when the offset is not right.
    		m_source_.setIndex(m_source_.getEndIndex());
    		updateInternalState();
    	}
    	m_isForwards_ = false;
        int result = NULLORDER;
	    if (m_CEBufferSize_ > 0) {
	    	if (m_CEBufferOffset_ > 0) {
	        	return m_CEBuffer_[-- m_CEBufferOffset_];
	    	}
	    	m_CEBufferSize_ = 0; 
	    	m_CEBufferOffset_ = 0;
	    }
	    char ch = previousChar();    
		if (ch == CharacterIterator.DONE) {
	        return NULLORDER;
	    } 
	    if (m_collator_.m_isHiragana4_) {
	       	m_isCodePointHiragana_ = (ch >= 0x3040 && ch <= 0x309f);
	    }
	    if (m_collator_.isContractionEnd(ch) && !isBackwardsStart()) {
	        result = previousSpecial(m_collator_, CE_CONTRACTION_, ch);
	    }
	    else {
	        if (ch <= 0xFF) {
	            result = m_collator_.m_trie_.getLatin1LinearValue(ch);
	        	if (RuleBasedCollator.isSpecial(result)) {
	            	result = previousSpecial(m_collator_, result, ch);
	            }
	        }
	        else {
	            if (m_bufferOffset_ < 0 && isThaiBaseConsonant(ch) 
	            	&& m_source_.getIndex() != 0) {
	            	if (isThaiPreVowel(m_source_.previous())) {
	                	result = CE_THAI_;
	            	}
	            	else {
	            		result = m_collator_.m_trie_.getLeadValue(ch);
	            	}		
	            	m_source_.next();
	            }
	            else {
	                result = m_collator_.m_trie_.getLeadValue(ch);
	            }
	            if (RuleBasedCollator.isSpecial(result)) {
	                result = previousSpecial(m_collator_, result, ch);
	            }
	            if (result == CE_NOT_FOUND_) {
	                if (!isBackwardsStart() 
	                    && m_collator_.isContractionEnd(ch)) {
	                    result = CE_CONTRACTION_;
	                }
	                else {
	                    result = m_collator_.m_trie_.getLeadValue(ch);
	                }
	
	                if (RuleBasedCollator.isSpecial(result)) {
	                    result = previousSpecial(m_collator_.UCA_, result, ch);
	                }
	            }
	        }
	    }
	    return result;
    }

    /**
     * Return the primary order of a collation element ce.
     * i.e. the first 16 bits of the argument ce.
     * @param ce the collation element
     * @return the element's 16 bits primary order.
     * @draft 2.2
     */
    public final static int primaryOrder(int ce)
    {
        return (ce & RuleBasedCollator.CE_PRIMARY_MASK_) 
                >>> RuleBasedCollator.CE_PRIMARY_SHIFT_;
    }
    /**
     * Return the secondary order of a collation element ce.
     * i.e. the 16th to 27th inclusive set of bits in the argument ce.
     * @param ce the collation element
     * @return the element's 8 bits secondary order
     * @draft 2.2
     */
    public final static int secondaryOrder(int ce)
    {
        return (ce & RuleBasedCollator.CE_SECONDARY_MASK_) 
               >> RuleBasedCollator.CE_SECONDARY_SHIFT_;
    }
    
    /**
     * Return the tertiary order of a collation element ce. i.e. the last
     * 8 bits in the argument ce.
     * @param ce the collation element
     * @return the element's 8 bits tertiary order
     * @draft 2.2
     */
    public final static int tertiaryOrder(int ce)
    {
        return ce & RuleBasedCollator.CE_TERTIARY_MASK_;
    }

    /**
     * <p>
     * Sets the iterator to point to the collation element corresponding to
     * the specified character argument offset. The value returned by the next 
     * call to next() will be the collation element corresponding to the 
     * characters at argument offset. 
     * </p>
     * <p>
     * If argument offset is in the middle of a contracting character sequence, 
     * the iterator is adjusted to the start of the contracting sequence. This 
     * means that getOffset() is not guaranteed to return the same value as 
     * the argument offset.
     * </p>
     * <p>
     * If the decomposition mode is on and argument offset is in the middle of 
     * a decomposible range of source text, the iterator may not render a 
     * correct result for 
     * the next forwards or backwards iteration. User has to ensure that the
     * argument offset does not violate the mid of a decomposible range in
     * source text.
     * </p>
     * @param offset character offset into the original source string to 
     *        set. Note this argument is not an offset into the corresponding 
     *        sequence of collation elements
     * @draft 2.2
     */
    public void setOffset(int offset)
    {  
    	m_source_.setIndex(offset);
    	char ch = m_source_.current();
    	if (m_collator_.isUnsafe(ch)) {
    		// if it is unsafe we need to check if it is part of a contraction
    		// or a surrogate character
    		if (UTF16.isTrailSurrogate(ch)) {
    			// if it is a surrogate pair we move up one character
    			char prevch = m_source_.previous();
    			if (!UTF16.isLeadSurrogate(prevch)) {
    				m_source_.setIndex(offset); // go back to the same index
    			}
    		}
    		else {
    			// could be part of a contraction
    			// backup to a safe point and iterate till we pass offset
    			while (m_source_.getIndex() > 0) {
    				if (!m_collator_.isUnsafe(ch)) {
    					break;
    				}
    				ch = m_source_.previous();
    			}
    			updateInternalState();
    			int prevoffset = 0;
    			while (m_source_.getIndex() <= offset) {
    				prevoffset = m_source_.getIndex();
    				next();
    			}	
    			m_source_.setIndex(prevoffset);
    		}
    	}
    	updateInternalState();
    }

    /**
     * <p>
     * Sets a new source string for iteration and restart the iteration to
     * start from the beginning of the argument source.
     * </p>
     * @param source the new source string for iteration.
     * @draft 2.2
     */
    public void setText(String source)
    {
    	m_source_ = new StringCharacterIterator(source);
    	updateInternalState();
    }

    /**
     * <p>
     * Sets a new source string iterator for iteration and restart the 
     * iteration to start from the beginning of the argument source.
     * </p>
     * @param source the new source string iterator for iteration.
     * @draft 2.2
     */
    public void setText(CharacterIterator source)
    {
		m_source_ = source;    	
		m_source_.setIndex(m_source_.getBeginIndex());
		updateInternalState();
    }
    
    // public miscellaneous methods -----------------------------------------
    
	// protected constructors -----------------------------------------------
	
	/**
     * <p>CollationElementIterator constructor. This takes the source string 
     * and the Collator. The cursor will walk thru the source string based
     * on the predefined collation rules. If the source string is empty,
     * NULLORDER will be returned on the calls to next().</p>
     * @param source the source string.
     * @param collator the RuleBasedCollator
     * @draft 2.2
     */
    CollationElementIterator(String source, RuleBasedCollator collator) 
    {
    	m_source_ = new StringCharacterIterator(source);
  		m_collator_ = collator;
  		m_CEBuffer_ = new int[CE_BUFFER_INIT_SIZE_];
    	m_buffer_ = new StringBuffer();
    	m_backup_ = new Backup();
    	updateInternalState();
    }

    /**
     * <p>CollationElementIterator constructor. This takes the source string 
     * and the Collator. The cursor will walk thru the source string based
     * on the predefined collation rules. If the source string is empty,
     * NULLORDER will be returned on the calls to next().</p>
     * @param source the source string iterator.
     * @param collator the RuleBasedCollator
     * @draft 2.2
     */
    CollationElementIterator(CharacterIterator source, 
                             RuleBasedCollator collator) 
    {
    	m_source_ = source;
    	m_collator_ = collator;
    	m_CEBuffer_ = new int[CE_BUFFER_INIT_SIZE_];
    	m_buffer_ = new StringBuffer();
    	m_backup_ = new Backup();
    	updateInternalState();
    }
    
    // package private data members -----------------------------------------
	
	/**
  	 * true if current codepoint was Hiragana
  	 */
  	boolean m_isCodePointHiragana_;
  	/**
  	 * Position in the original string that starts with a non-FCD sequence
  	 */
  	int m_FCDStart_;
  	/** 
	 * This is the CE from CEs buffer that should be returned. 
	 * Initial value is 0.
	 * Forwards iteration will end with m_CEBufferOffset_ == m_CEBufferSize_,
	 * backwards will end with m_CEBufferOffset_ == 0.
	 * The next/previous after we reach the end/beginning of the m_CEBuffer_
	 * will cause this value to be reset to 0.
	 */
  	int m_CEBufferOffset_;
  	/** 
  	 * This is the position to which we have stored processed CEs.
  	 * Initial value is 0.
  	 * The next/previous after we reach the end/beginning of the m_CEBuffer_
	 * will cause this value to be reset to 0.
  	 */
  	int m_CEBufferSize_; 
    
    // package private methods ----------------------------------------------
    
    /**
     * Sets the collator used.
     * Internal use, all data members will be reset to the default values
     * @param collator to set
     */
    void setCollator(RuleBasedCollator collator) 
    {
    	m_collator_ = collator;
    	updateInternalState();
    }
    
    /**
     * <p>Sets the iterator to point to the collation element corresponding to
     * the specified character (the parameter is a CHARACTER offset in the
     * original string, not an offset into its corresponding sequence of
     * collation elements). The value returned by the next call to next()
     * will be the collation element corresponding to the specified position
     * in the text. Unlike the public method setOffset(int), this method does 
     * not try to readjust the offset to the start of a contracting sequence.
     * getOffset() is guaranteed to return the same value as was passed to a 
     * preceding call to setOffset().</p>
     * @param offset new character offset into the original text to set. 
     * @draft 2.2
     */
    void setExactOffset(int offset)
    {  
	    m_source_.setIndex(offset);
	    updateInternalState();
    }
    
    /**
     * Checks if iterator is in the buffer zone
     * @return true if iterator is in buffer zone, false otherwise
     */
    boolean isInBuffer()
    {
    	return m_bufferOffset_ != -1;
    }
    
    /**
 	 * Determine if a character is a Thai vowel, which sorts after its base 
 	 * consonant.
 	 * @param ch character to test
 	 * @return true if ch is a Thai prevowel, false otherwise
 	 */
	static final boolean isThaiPreVowel(char ch)
	{ 
		return (ch >= 0xe40 && ch <= 0xe44) || (ch >= 0xec0 && ch <= 0xec4);
	}

	/**
 	 * Determine if a character is a Thai base consonant, which sorts before 
 	 * its prevowel
 	 * @param ch character to test
 	 * @return true if ch is a Thai base consonant, false otherwise
 	 */
	static final boolean isThaiBaseConsonant(char ch)
	{
		return ch >= 0xe01 && ch <= 0xe2e;
	}
    
    // private inner class --------------------------------------------------
    
    /**
     * Backup data class
     */
    private static class Backup
    {
    	// protected data members -------------------------------------------
    	
    	/**
	 	 * Backup non FCD sequence limit
	  	 */
		protected int m_FCDLimit_;
		/**
		 * Backup non FCD sequence start
		 */
		protected int m_FCDStart_;
		/**
		 * Backup if previous Codepoint is Hiragana quatenary
		 */
		protected boolean m_isCodePointHiragana_;
		/**
		 * Backup buffer position 
		 */
		protected int m_bufferOffset_;
		/**
		 * Backup source iterator offset
		 */
		protected int m_offset_;
		/**
		 * Backup buffer contents
		 */
		protected StringBuffer m_buffer_;
		
		// protected constructor --------------------------------------------
		
		/**
		 * Empty constructor
		 */
		protected Backup()
		{
			m_buffer_ = new StringBuffer();
		}
    }
    // end inner class ------------------------------------------------------
    
    /**
     * Direction of travel
     */
    private boolean m_isForwards_;
    /**
     * Source string iterator
     */
    private CharacterIterator m_source_;
    /** 
     * This is position to the m_buffer_, -1 if iterator is not in m_buffer_
     */
    private int m_bufferOffset_;
  	/**
  	 * Buffer for temporary storage of normalized characters, discontiguous
  	 * characters and Thai characters
  	 */
  	private StringBuffer m_buffer_;
  	/** 
  	 * Position in the original string to continue forward FCD check from. 
  	 */
  	private int m_FCDLimit_; 
  	/**
  	 * The collator this iterator is based on
  	 */ 
  	private RuleBasedCollator m_collator_;
  	/**
  	 * true if Hiragana quatenary is on
  	 */
  	private boolean m_isHiragana4_;
  	/**
  	 * CE buffer
  	 */	
  	private int m_CEBuffer_[]; 
	/** 
	 * In reality we should not have to deal with expansion sequences longer 
	 * then 16. However this value can be change if a bigger buffer is needed.
	 * Note, if the size is change to too small a number, BIG trouble.
	 * Reasonable small value is around 10, if there's no Arabic or other 
	 * funky collations that have long expansion sequence. This is the longest 
	 * expansion sequence this can handle without bombing out.
	 */
	private static final int CE_BUFFER_INIT_SIZE_ = 512;
	/**
	 * Backup storage
	 */
	private Backup m_backup_;
	/**
	 * One character before the first non-zero combining class character
	 */
	private static final int FULL_ZERO_COMBINING_CLASS_FAST_LIMIT_ = 0xC0;
	/**
	 * One character before the first character with leading non-zero combining 
	 * class 
	 */
	private static final int LEAD_ZERO_COMBINING_CLASS_FAST_LIMIT_ = 0x300;
	/**
	 * Mask for the last byte
	 */
	private static final int LAST_BYTE_MASK_ = 0xFF;
	/**
	 * Shift value for the second last byte
	 */
	private static final int SECOND_LAST_BYTE_SHIFT_ = 8;

	// special ce values and tags -------------------------------------------
	private static final int CE_NOT_FOUND_ = 0xF0000000;
	private static final int CE_EXPANSION_ = 0xF1000000;
	private static final int CE_CONTRACTION_ = 0xF2000000;
	private static final int CE_THAI_ = 0xF3000000;
	/**
	 * Indicates the last ce has been consumed. Compare with NULLORDER. 
	 * NULLORDER is returned if error occurs.
 	 */
	private static final int CE_NO_MORE_CES_ = 0x00010101;
	private static final int CE_NO_MORE_CES_PRIMARY_ = 0x00010000;
	private static final int CE_NO_MORE_CES_SECONDARY_ = 0x00000100;
	private static final int CE_NO_MORE_CES_TERTIARY_ = 0x00000001;

	private static final int CE_NOT_FOUND_TAG_ = 0;
	private static final int CE_EXPANSION_TAG_ = 1;       
    private static final int CE_CONTRACTION_TAG_ = 2;     
    private static final int CE_THAI_TAG_ = 3;
    /** 
     * Charset processing, not yet implemented 
     */
    private static final int CE_CHARSET_TAG_ = 4;         
    /** 
     * AC00-D7AF
     */
    private static final int CE_HANGUL_SYLLABLE_TAG_ = 6;
    /**
     * D800-DBFF
     */
    private static final int CE_LEAD_SURROGATE_TAG_ = 7;  
    /** 
     * DC00-DFFF
     */
    private static final int CE_TRAIL_SURROGATE_TAG_ = 8; 
    /** 
     * 0x3400-0x4DB5, 0x4E00-0x9FA5, 0xF900-0xFA2D
     */    
    private static final int CE_CJK_IMPLICIT_TAG_ = 9;    
    private static final int CE_IMPLICIT_TAG_ = 10;
    private static final int CE_SPEC_PROC_TAG_ = 11;
    /** 
     * This is a 3 byte primary with starting secondaries and tertiaries.
     * It fits in a single 32 bit CE and is used instead of expansion to save
     * space without affecting the performance (hopefully).
     */
    private static final int CE_LONG_PRIMARY_TAG_ = 12; 
    private static final int CE_CE_TAGS_COUNT = 13;
   	private static final int CE_BYTE_COMMON_ = 0x05;
   	
	// end special ce values and tags ---------------------------------------
	
	private static final int IMPLICIT_HAN_START_ = 0x3400;
	private static final int IMPLICIT_HAN_LIMIT_ = 0xA000;
	private static final int IMPLICIT_SUPPLEMENTARY_COUNT_ = 0x100000;
	private static final int IMPLICIT_BYTES_TO_AVOID_ = 3;
	private static final int IMPLICIT_OTHER_COUNT_ = 
												256 - IMPLICIT_BYTES_TO_AVOID_;
	private static final int IMPLICIT_LAST_COUNT_ = IMPLICIT_OTHER_COUNT_ >> 1;
	private static final int IMPLICIT_LAST_COUNT2_ =
                       	(IMPLICIT_SUPPLEMENTARY_COUNT_ - 1) /
                       	(IMPLICIT_OTHER_COUNT_ * IMPLICIT_OTHER_COUNT_) + 1;
	private static final int IMPLICIT_HAN_SHIFT_ = IMPLICIT_LAST_COUNT_ *
                              	IMPLICIT_OTHER_COUNT_ - IMPLICIT_HAN_START_;
	private static final int IMPLICIT_BOUNDARY_ = 2 * IMPLICIT_OTHER_COUNT_ *
                                  IMPLICIT_LAST_COUNT_ + IMPLICIT_HAN_START_;
	private static final int IMPLICIT_LAST2_MULTIPLIER_ = 
								IMPLICIT_OTHER_COUNT_ / IMPLICIT_LAST_COUNT2_;
	private static final int HANGUL_SBASE_ = 0xAC00;
	private static final int HANGUL_LBASE_ = 0x1100; 
	private static final int HANGUL_VBASE_ = 0x1161;
	private static final int HANGUL_TBASE_ = 0x11A7;
	private static final int HANGUL_VCOUNT_ = 21; 
	private static final int HANGUL_TCOUNT_ = 28;                                                        
	// private methods ------------------------------------------------------
	
	/**
	 * Reset the iterator internally
	 */
	private void updateInternalState()
	{
		m_isCodePointHiragana_ = false;
  		m_bufferOffset_ = -1; 
		m_CEBufferOffset_ = 0;
  		m_CEBufferSize_ = 0; 
  		m_FCDLimit_ = -1;
  		m_FCDStart_ = m_source_.getEndIndex();
    	m_isHiragana4_ = m_collator_.m_isHiragana4_;
    	m_isForwards_ = true;
	}
	
	/**
	 * Backup the current internal state
	 * @param backup object to store the data
	 */
	private void backupInternalState(Backup backup)
	{
		backup.m_offset_ = m_source_.getIndex();
		backup.m_FCDLimit_ = m_FCDLimit_;
		backup.m_FCDStart_ = m_FCDStart_;
		backup.m_isCodePointHiragana_ = m_isCodePointHiragana_;
		backup.m_bufferOffset_ = m_bufferOffset_;
		if (m_bufferOffset_ >= 0) {
			backup.m_buffer_.append(m_buffer_);
		}
	}
		
	/**
	 * Update the iterator internally with backed-up state
	 * @param backup object that stored the data
	 */
	private void updateInternalState(Backup backup)
	{
		m_source_.setIndex(backup.m_offset_);
		m_isCodePointHiragana_ = backup.m_isCodePointHiragana_;
		m_bufferOffset_ = backup.m_bufferOffset_;
		m_FCDLimit_ = backup.m_FCDLimit_;
        m_FCDStart_ = backup.m_FCDStart_;
        m_buffer_.delete(0, m_buffer_.length());
		if (m_bufferOffset_ >= 0) {
        	m_buffer_.append(backup.m_buffer_);
    	}
	}
	
	/**
	 * A fast combining class retrieval system.
	 * @param ch UTF16 character
	 * @return combining class of ch
	 */
	private int getCombiningClass(char ch) 
	{
    	if (ch >= LEAD_ZERO_COMBINING_CLASS_FAST_LIMIT_ && 
    	    m_collator_.isUnsafe(ch)) {
        	return NormalizerImpl.getCombiningClass(ch);
    	}
    	return 0;
	}
	
	/**
	 * <p>Incremental normalization, this is an essential optimization.
	 * Assuming FCD checks has been done, normalize the non-FCD characters into 
	 * the buffer.
	 * Source offsets points to the current processing character.
	 * </p>
	 */
	private void normalize() 
	{
		int size = m_FCDLimit_ - m_FCDStart_;
		m_buffer_.delete(0, m_buffer_.length());
		m_source_.setIndex(m_FCDStart_);
		for (int i = 0; i < size; i ++) {
			m_buffer_.append(m_source_.current());
			m_source_.next();
		}
		String decomp = Normalizer.decompose(m_buffer_.toString(), false);
		m_buffer_.delete(0, m_buffer_.length());
		m_buffer_.append(decomp);
		m_bufferOffset_ = 0;
	}
	
	/** 
	 * <p>Incremental FCD check and normalization. Gets the next base character
	 * position and determines if the in-between characters needs normalization.
	 * </p> 
	 * <p>When entering, the state is known to be this:
	 * <ul>
	 * <li>We are working on source string, not the buffer.
	 * <li>The leading combining class from the current character is 0 or the 
	 *     trailing combining class of the previous char was zero.
	 * </ul>
	 * Incoming source offsets points to the current processing character.
	 * Return source offsets points to the current processing character.
	 * </p>
	 * @param ch current character
	 * @param offset current character offset
	 * @return true if FCDCheck passes, false otherwise
	 */
	private boolean FCDCheck(char ch, int offset) 
	{
    	boolean result = true;

    	// Get the trailing combining class of the current character.  
		// If it's zero, we are OK.
    	m_FCDStart_ = offset;
    	m_source_.setIndex(offset);
    	// trie access
    	char fcd = NormalizerImpl.getFCD16(ch);
    	if (fcd != 0 && UTF16.isLeadSurrogate(ch)) {
    		ch = m_source_.next(); // CharacterIterator.DONE has 0 fcd
            if (UTF16.isTrailSurrogate(ch)) {
               	fcd = NormalizerImpl.getFCD16FromSurrogatePair(fcd, ch);
            } else {
               	fcd = 0;
            }
        }

        int prevTrailCC = fcd & LAST_BYTE_MASK_;

        if (prevTrailCC != 0) {
        	// The current char has a non-zero trailing CC. Scan forward until 
            // we find a char with a leading cc of zero.
            while (true) {
            	ch = m_source_.next();
            	if (ch == CharacterIterator.DONE) {
            		break;
            	}
                // trie access
                fcd = NormalizerImpl.getFCD16(ch);
                if (fcd != 0 && UTF16.isLeadSurrogate(ch)) {
                	ch = m_source_.next();
                    if (UTF16.isTrailSurrogate(ch)) {
                        fcd = NormalizerImpl.getFCD16FromSurrogatePair(fcd, ch);
                    } else {
                        fcd = 0;
                    }
                }
                int leadCC = fcd >>> SECOND_LAST_BYTE_SHIFT_;
                if (leadCC == 0) {
                	// this is a base character, we stop the FCD checks
                    break;
                }

                if (leadCC < prevTrailCC) {
                    result = false;
                }

                prevTrailCC = fcd & LAST_BYTE_MASK_;
            }
        }
        m_FCDLimit_ = m_source_.getIndex();
        m_source_.setIndex(m_FCDStart_);
        m_source_.next();
    	return result;
	}
	
	/** 
	 * <p>Method tries to fetch the next character that is in fcd form.</p>
	 * <p>Normalization is done if required.</p>
	 * <p>Offsets are returned at the next character.</p>
	 * @return next fcd character
	 */
	private char nextChar()
	{
		char result;
		int startoffset = m_source_.getIndex();
		// loop handles the next character whether it is in the buffer or not.
	    if (m_bufferOffset_ == -1) {
	        // we're working on the source and not normalizing. fast path.
	        // note Thai pre-vowel reordering uses buffer too
	        result = m_source_.current();
	    }
		else {
	        // we are in the buffer, buffer offset will never be 0 here
	        if (m_bufferOffset_ >= m_buffer_.length()) {
	            // Null marked end of buffer, revert to the source string and
	            // loop back to top to try again to get a character.
	            m_source_.setIndex(m_FCDLimit_);
	            m_bufferOffset_ = -1;
	            m_buffer_.delete(0, m_buffer_.length());
	            return nextChar();
	        }
	        return m_buffer_.charAt(m_bufferOffset_ ++);
		}
	
	    if (m_collator_.getDecomposition() == Collator.NO_DECOMPOSITION 
	        || m_bufferOffset_ != -1 || m_FCDLimit_ > startoffset
	        // skip the fcd checks
	  		|| result < FULL_ZERO_COMBINING_CLASS_FAST_LIMIT_  
	   		// Fast fcd safe path. trail combining class == 0.
	   		) {
	   		
	        m_source_.next();
	   		return result;
	    }
		
		if (result < LEAD_ZERO_COMBINING_CLASS_FAST_LIMIT_) {
	        // We need to peek at the next character in order to tell if we are 
	        // FCD
	        char next = m_source_.next();
	        if (next == CharacterIterator.DONE 
	            || next == LEAD_ZERO_COMBINING_CLASS_FAST_LIMIT_) {
	            return result; // end of source string and if next character 
	            				// starts with a base character is always fcd.
	        }
	    }
	
	    // Need a more complete FCD check and possible normalization.
	    if (!FCDCheck(result, startoffset)) {
	        normalize();
	        result = m_buffer_.charAt(0);
	        m_bufferOffset_ = 1;	  
	    }	
	    return result;
	}
	
	/**
	* <p>Incremental normalization, this is an essential optimization.
	* Assuming FCD checks has been done, normalize the non-FCD characters into 
	* the buffer.
	* Source offsets points to the current processing character.</p>
	*/
	private void normalizeBackwards()
	{
	    normalize();
	    m_bufferOffset_ = m_buffer_.length();
	}

	/**
	 * <p>Incremental backwards FCD check and normalization. Gets the previous 
	 * base character position and determines if the in-between characters 
	 * needs normalization.
	 * </p> 
	 * <p>When entering, the state is known to be this:
	 * <ul>
	 * <li>We are working on source string, not the buffer.
	 * <li>The trailing combining class from the current character is 0 or the 
	 *     leading combining class of the next char was zero.
	 * </ul>
	 * Input source offsets points to the previous character.
	 * Return source offsets points to the current processing character.
	 * </p>
	 * @param ch current character
	 * @param offset current character offset
	 * @return true if FCDCheck passes, false otherwise
	 */
	private boolean FCDCheckBackwards(char ch, int offset)
	{
	    boolean result = true;    
	    char fcd = 0; 
	    m_FCDLimit_ = offset + 1;
	    m_source_.setIndex(offset);
	    if (!UTF16.isSurrogate(ch)) {
	        fcd = NormalizerImpl.getFCD16(ch);
	    } 
	    else if (UTF16.isTrailSurrogate(ch) && m_FCDLimit_ > 0) { 
	    	// note trail surrogate characters gets 0 fcd
	    	char trailch = ch;
	    	ch = m_source_.previous();  
	       	if (UTF16.isLeadSurrogate(ch)) {
	        	fcd = NormalizerImpl.getFCD16(ch);
	        	if (fcd != 0) {
	            	fcd = NormalizerImpl.getFCD16FromSurrogatePair(fcd, 
	            													trailch);
	        	}
	    	} 
	    	else {
	        	fcd = 0; // unpaired surrogate 
	    	}
	    }
	
	    int leadCC = fcd >>> SECOND_LAST_BYTE_SHIFT_;
	    // The current char has a non-zero leading combining class.
	    // Scan backward until we find a char with a trailing cc of zero.
        
	    while (leadCC != 0) {
            offset = m_source_.getIndex();
            if (offset == 0) {
	            break;
	        }
	        ch = m_source_.previous();
	        if (!UTF16.isSurrogate(ch)) {
	            fcd = NormalizerImpl.getFCD16(ch);
	        } 
	        else if (UTF16.isTrailSurrogate(ch) && m_source_.getIndex() > 0) {
	            char trail = ch;
	            ch = m_source_.previous();
	            if (UTF16.isLeadSurrogate(ch)) {
	                fcd = NormalizerImpl.getFCD16(ch);
	            }
	            if (fcd != 0) {
	                fcd = NormalizerImpl.getFCD16FromSurrogatePair(fcd, trail);
	            }
            }
            else {
	            fcd = 0; // unpaired surrogate
	        }
	        int prevTrailCC = fcd & LAST_BYTE_MASK_;
	        if (leadCC < prevTrailCC) {
	            result = false;
	        }
	        leadCC = fcd >>> SECOND_LAST_BYTE_SHIFT_;
	    }
	
        // storing character with 0 lead fcd or the 1st accent with a base 
        // character before it   
        if (fcd == 0) {
            m_FCDStart_ = offset;
        }
        else {
            m_FCDStart_ = m_source_.getIndex();
        } 
	    m_source_.setIndex(m_FCDLimit_);
	    return result;
	}
	
	/** 
	 * <p>Method tries to fetch the previous character that is in fcd form.</p>
	 * <p>Normalization is done if required.</p>
	 * <p>Offsets are returned at the current character.</p>
	 * @return previous fcd character
	 */
	private char previousChar()
	{
		if (m_bufferOffset_ >= 0) {
			m_bufferOffset_ --;
			if (m_bufferOffset_ >= 0) {
	        	return m_buffer_.charAt(m_bufferOffset_);
			}
	        else {
	            // At the start of buffer, route back to string.
	            m_buffer_.delete(0, m_buffer_.length());
                if (m_FCDStart_ == 0) {
                	m_FCDStart_ = -1;
	                return CharacterIterator.DONE;
                }
	            else {
	                m_FCDLimit_ = m_FCDStart_;
	                return previousChar();
	            }
	        }
		}    
		char result = m_source_.previous();
		int startoffset = m_source_.getIndex();
	    if (result < LEAD_ZERO_COMBINING_CLASS_FAST_LIMIT_ 
	        || m_collator_.getDecomposition() == Collator.NO_DECOMPOSITION 
	        || m_FCDStart_ <= startoffset || m_source_.getIndex() == 0) {
	        return result;
	    }
	    char ch = m_source_.previous();
	    if (ch < FULL_ZERO_COMBINING_CLASS_FAST_LIMIT_) {
	        // if previous character is FCD 
	        m_source_.next();
	        return result;
	    }
	    // Need a more complete FCD check and possible normalization.
	    if (!FCDCheckBackwards(result, startoffset)) {
	        normalizeBackwards();
	        m_bufferOffset_ --;
	        result = m_buffer_.charAt(m_bufferOffset_);
	    }
	    return result;
	}
	
	/**
	 * Determines if it is at the start of source iteration
	 * @return true if iterator at the start, false otherwise
	 */
	private final boolean isBackwardsStart() 
	{
    	return (m_bufferOffset_ < 0 && m_source_.getIndex() == 0)
    	        || (m_bufferOffset_ == 0 && m_FCDStart_ <= 0);
	}
	
	/**
	 * Checks if iterator is at the end of its source string.
	 * @return true if it is at the end, false otherwise
	 */
	private final boolean isEnd() 
	{
    	if (m_bufferOffset_ >= 0) {
    		if (m_bufferOffset_ != m_buffer_.length()) {
	    		return false;
    		}
    		else {
    			// at end of buffer. check if fcd is at the end
    			return m_FCDLimit_ == m_source_.getEndIndex();
    		}
	    }
		return m_source_.getEndIndex() == m_source_.getIndex();
	}
	
	/**
	 * <p>Special CE management for surrogates</p>
	 * <p>Lead surrogate is encountered. CE to be retrieved by using the 
	 * following code unit. If next character is a trail surrogate, both 
	 * characters will be combined to retrieve the CE, otherwise completely 
	 * ignorable (UCA specification) is returned.</p>
	 * @param collator collator to use
	 * @param ce current CE
	 * @param trail character
	 * @return next CE for the surrogate characters
	 */
	private final int nextSurrogate(RuleBasedCollator collator, int ce, 
	                                char trail)
	{
		if (!UTF16.isTrailSurrogate(trail)) {
	        updateInternalState(m_backup_);
	        return IGNORABLE;
	    } 
	    // TODO: CE contain the data from the previous CE + the mask. 
	    // It should at least be unmasked
	    int result = collator.m_trie_.getTrailValue(ce, trail);
	    if (result == CE_NOT_FOUND_) { 
	      	updateInternalState(m_backup_);
	    }
	    return result;
	}
	
	/**
	 * Gets the CE expansion offset
	 * @param collator current collator
	 * @param ce ce to test
	 * @return expansion offset
	 */
	private int getExpansionOffset(RuleBasedCollator collator, int ce)
	{
		return ((ce & 0xFFFFF0) >> 4) - collator.m_expansionOffset_;
	}
	
	/**
	 * Swaps the Thai and Laos characters and returns the CEs.
	 * @param collator collator to use
	 * @param ce current ce
	 * @param ch current character
	 * @return next CE for Thai characters
	 */
	private int nextThai(RuleBasedCollator collator, int ce, char ch)
	{
		if (m_bufferOffset_ != -1 // already swapped
		    || isEnd() || !isThaiBaseConsonant(m_source_.current())) {
		    // next character is also not a thai base consonant
	        // Treat Thai as a length one expansion
	        // find the offset to expansion table
	        return collator.m_expansion_[getExpansionOffset(collator, ce)]; 
	    }
	    else {
	        // swap the prevowel and the following base consonant into the 
	        // buffer with their order swapped
	        // buffer is always clean when we are in the source string
	        m_buffer_.append(nextChar());
	        m_buffer_.append(ch);
	        m_FCDLimit_ = m_source_.getIndex();
	        m_FCDStart_ = m_FCDLimit_ - 2; 
			m_bufferOffset_ = 0;
	        return IGNORABLE;
	   }
	}
	
	/**
	 * Gets the contraction ce offset
	 * @param collator current collator
	 * @param ce current ce
	 * @return contraction offset
	 */
	private int getContractionOffset(RuleBasedCollator collator, int ce)
	{
		return (ce & 0xFFFFFF) - collator.m_contractionOffset_;
	}
	
	/**
	 * Checks if CE is a special tag CE
	 * @param ce to check
	 * @return true if CE is a special tag CE, false otherwise
	 */
	private boolean isSpecialPrefixTag(int ce)
	{
		return RuleBasedCollator.isSpecial(ce) && 
						RuleBasedCollator.getTag(ce) == CE_SPEC_PROC_TAG_;
	}
	
	/**
	 * <p>Special processing getting a CE that is preceded by a certain 
	 * prefix.</p>
	 * <p>Used for optimizing Japanese length and iteration marks. When a 
	 * special processing tag is encountered, iterate backwards to see if 
	 * there's a match.</p> 
	 * <p>Contraction tables are used, prefix data is stored backwards in the 
	 * table.</p>
	 * @param collator collator to use
	 * @param ce current ce
	 * @param entrybackup entry backup iterator status
	 * @return next collation element
	 */
	private int nextSpecialPrefix(RuleBasedCollator collator, int ce,
	                              Backup entrybackup)
	{
		backupInternalState(m_backup_);
	    updateInternalState(entrybackup);
	    previousChar();
	    // We want to look at the character where we entered
	
	 	while (true) {
	        // This loop will run once per source string character, for as 
	        // long as we are matching a potential contraction sequence                  
            // First we position ourselves at the begining of contraction 
            // sequence 
	        int entryoffset = getContractionOffset(collator, ce);
			int offset = entryoffset;						
	        if (isBackwardsStart()) {
	          	ce = collator.m_contractionCE_[offset];
	            break;
	        }
	        int previous = previousChar();
	        while (previous > collator.m_contractionIndex_[offset]) { 
	        	// contraction characters are ordered, skip smaller characters
	            offset ++;
	        }
	
	        if (previous == collator.m_contractionIndex_[offset]) {
	            // Found the source string char in the table.
	            // Pick up the corresponding CE from the table.
	            ce = collator.m_contractionCE_[offset];
	       	}
	        else {
	            // Source string char was not in the table, prefix not found
	            ce = collator.m_contractionCE_[entryoffset];
	        }
	
	        if (!isSpecialPrefixTag(ce)) {
	            // The source string char was in the contraction table, and 
	            // the corresponding CE is not a prefix CE. We found the 
	            // prefix, break out of loop, this CE will end up being 
	            // returned. This is the normal way out of prefix handling 
	            // when the source actually contained the prefix.
	            break;
	        }
	    }
	    if (ce != CE_NOT_FOUND_) { 
	    	// we found something and we can merilly continue
	        updateInternalState(m_backup_);
	    } 
	    else { // prefix search was a failure, we have to backup all the way to 
	    		// the start
	        updateInternalState(entrybackup);
	    }
	    return ce;
	}
	
	/**
	 * Checks if the ce is a contraction tag
	 * @param ce ce to check
	 * @return true if ce is a contraction tag, false otherwise
	 */
	private boolean isContractionTag(int ce)
	{
		return RuleBasedCollator.isSpecial(ce) && 
							RuleBasedCollator.getTag(ce) == CE_CONTRACTION_TAG_;
	}
	
	/**
	 * Method to copy skipped characters into the buffer and sets the fcd 
	 * position. To ensure that the skipped characters are considered later, 
	 * we need to place it in the appropriate position in the buffer and 
	 * reassign the source index. simple case if index reside in string, 
	 * simply copy to buffer and fcdposition = pos, pos = start of buffer. 
	 * if pos in normalization buffer, we'll insert the copy infront of pos 
	 * and point pos to the start of the buffer. why am i doing these copies?
	 * well, so that the whole chunk of codes in the getNextCE, 
	 * ucol_prv_getSpecialCE does not require any changes, which will be 
	 * really painful.
	 * @param skipped character buffer
	 */
	private void setDiscontiguous(StringBuffer skipped)
	{
	    if (m_bufferOffset_ >= 0) {
	        skipped.append(m_buffer_.substring(m_bufferOffset_));
	    }
	    else {
	        m_FCDLimit_ = m_source_.getIndex();
	    }
	
		m_bufferOffset_ = 0;
		m_buffer_ = skipped;
	}
	
	/**
	 * Returns the current character for forward iteration
	 * @return current character
	 */
	private char currentChar()
	{
		if (m_bufferOffset_ < 0) {
			char result = m_source_.previous();
			m_source_.next();
			return result;
		}
		
		// m_bufferOffset_ is never 0 in normal circumstances except after a
		// discontiguous contraction since it is always returned and moved
		// by 1 when we do nextChar()
		return m_buffer_.charAt(m_bufferOffset_ - 1);
	}

	/**
	 * Method to get the discontiguous collation element within the source.
	 * Note this function will set the position to the appropriate places.
	 * Passed in character offset points to the second combining character 
	 * after the start character.
	 * @param collator current collator used
	 * @param entryoffset index to the start character in the contraction table
	 * @return discontiguous collation element offset
	 */
	private int nextDiscontiguous(RuleBasedCollator collator, int entryoffset)
	{
		int offset = entryoffset;
    	boolean multicontraction = false;
    	StringBuffer skipped = new StringBuffer();
    	char ch = currentChar();
    	skipped.append(currentChar()); // accent after the first character
    	Backup backup = new Backup();
    	backupInternalState(backup);
		char nextch = ch;
        while (true) {
        	ch = nextch;
        	nextch = nextChar(); 
        	if (nextch == CharacterIterator.DONE 
        		|| getCombiningClass(nextch) == 0) {
	            // if there are no more accents to move around
	            // we don't have to shift previousChar, since we are resetting
	            // the offset later
	            if (multicontraction) {
	                setDiscontiguous(skipped);
	                return collator.m_contractionCE_[offset];
	            }
	            break;
	        }

			offset ++; // skip the combining class offset
        	while (nextch > collator.m_contractionIndex_[offset]) {
            	offset ++;
        	}

			int ce = CE_NOT_FOUND_;
        	if (nextch != collator.m_contractionIndex_[offset]
        		|| getCombiningClass(nextch) == getCombiningClass(ch)) {
            	// unmatched or blocked character
            	skipped.append(nextch);
            	continue;
        	}
	        else {
	            ce = collator.m_contractionCE_[offset];
	        }
	        
	        if (ce == CE_NOT_FOUND_) {
	          	break;
	        } 
	        else if (isContractionTag(ce)) {
	            // this is a multi-contraction
	            offset = getContractionOffset(collator, ce);
	            if (collator.m_contractionCE_[offset] != CE_NOT_FOUND_) {
	            	multicontraction = true;
	            	backupInternalState(backup);
	            }
	        } 
	        else {
	           	setDiscontiguous(skipped);
	            return ce;
	        }
    	}
	    updateInternalState(backup);
	    return collator.m_contractionCE_[entryoffset];
	}
	
	/**
	 * Gets the next contraction ce
	 * @param collator collator to use
	 * @param ce current ce
	 * @param entrybackup entry backup iterator status
	 * @return ce of the next contraction
	 */
	private int nextContraction(RuleBasedCollator collator, int ce)
	{
		Backup backup = new Backup();
	    backupInternalState(backup);
	    int entryce = CE_NOT_FOUND_;
	    while (true) {
	        int entryoffset = getContractionOffset(collator, ce);
			int offset = entryoffset;	
	
	        if (isEnd()) {
	        	ce = collator.m_contractionCE_[offset];
	            if (ce == CE_NOT_FOUND_) {
	                // back up the source over all the chars we scanned going 
	                // into this contraction.
	                ce = entryce;  
	                updateInternalState(backup);
	            }
	            break;
	        }
	
			// get the discontiguos maximum combining class
	        byte maxCC = (byte)(collator.m_contractionIndex_[offset] & 0xFF); 
	        // checks if all characters have the same combining class
	        byte allSame = (byte)(collator.m_contractionIndex_[offset] >> 8);
	        char ch = nextChar();
	        offset ++;
	        while(ch > collator.m_contractionIndex_[offset]) { 
	        	// contraction characters are ordered, skip all smaller
	          	offset ++;
	        }
	
	        if (ch == collator.m_contractionIndex_[offset]) {
	            // Found the source string char in the contraction table.
	            //  Pick up the corresponding CE from the table.
	            ce = collator.m_contractionCE_[offset];
	        }
	        else
	        {
	            // Source string char was not in contraction table.
	            // Unless it is a discontiguous contraction, we are done
	            byte sCC;
	            if (maxCC == 0 || (sCC = (byte)getCombiningClass(ch)) == 0 
	            	|| sCC > maxCC || (allSame != 0 && sCC == maxCC) || 
	            	isEnd()) {
	                // Contraction can not be discontiguous, back up by one
	                previousChar(); 
	                ce = collator.m_contractionCE_[entryoffset];
	            } 
	            else {
	                // Contraction is possibly discontiguous.
	                // find the next character if ch is not a base character
	                char nextch = nextChar();
	                if (nextch != CharacterIterator.DONE) {
	                	previousChar();
	                }
	                if (getCombiningClass(nextch) == 0) {
	                    previousChar();
	                    // base character not part of discontiguous contraction
	                    ce = collator.m_contractionCE_[entryoffset];
	                } 
	                else {
	                    ce = nextDiscontiguous(collator, entryoffset);
	                }
	            }
	        }
	
	        if (ce == CE_NOT_FOUND_) {
	            // source did not match the contraction, revert back original
	          	updateInternalState(backup);
	          	ce = entryce;
	          	break;
	        }
	        
	        // source was a contraction
	        if (!isContractionTag(ce)) {
	            break;
	        }
	
	        // ccontinue looping to check for the remaining contraction.
	        if (collator.m_contractionCE_[entryoffset] != CE_NOT_FOUND_) {
	            // there are further contractions to be performed, so we store
	            // the so-far completed ce, so that if we fail in the next
	            // round we just return this one.
	            entryce = collator.m_contractionCE_[entryoffset];
	            backupInternalState(backup);
	            if (backup.m_bufferOffset_ >= 0) {
	            	backup.m_bufferOffset_ --;
	            }
	            else {
	            	backup.m_offset_ --;
	            }
	        }
	    }								
	    return ce;
	}
	
	/**
	 * Gets the next ce for long primaries, stuffs the rest of the collation 
	 * elements into the ce buffer
	 * @param ce current ce
	 * @return next ce
	 */
	private int nextLongPrimary(int ce)
	{
		m_CEBuffer_[1] = ((ce & 0xFF) << 24) 
							| RuleBasedCollator.CE_CONTINUATION_MARKER_;
		m_CEBufferOffset_ = 1;
		m_CEBufferSize_ = 2;
		m_CEBuffer_[0] = ((ce & 0xFFFF00) << 8) | (CE_BYTE_COMMON_ << 8) | 
	   													CE_BYTE_COMMON_;
	   	return m_CEBuffer_[0];
	}
	
	/**
	 * Gets the number of expansion
	 * @param ce current ce
	 * @return number of expansion
	 */
	private int getExpansionCount(int ce)
	{	
		return ce & 0xF;
	}
	
	/**
	 * Gets the next expansion ce and stuffs the rest of the collation elements
	 * into the ce buffer
	 * @param collator current collator
	 * @param ce current ce
	 * @return next expansion ce
	 */
	private int nextExpansion(RuleBasedCollator collator, int ce)
	{
		// NOTE: we can encounter both continuations and expansions in an 
		// expansion!
	    // I have to decide where continuations are going to be dealt with 
	    int offset = getExpansionOffset(collator, ce);
	    m_CEBufferSize_ = getExpansionCount(ce);
	    m_CEBufferOffset_ = 1;
	    m_CEBuffer_[0] = collator.m_expansion_[offset];
	    if (m_CEBufferSize_ != 0) { 
	    	// if there are less than 16 elements in expansion
	    	for (int i = 1; i < m_CEBufferSize_; i ++) {
	          	m_CEBuffer_[i] = collator.m_expansion_[offset + i];
	        }
	    } 
	    else { 
	    	// ce are terminated
	    	m_CEBufferSize_ = 1;
	        while (collator.m_expansion_[offset] != 0) {
	          	m_CEBuffer_[m_CEBufferSize_ ++] = 
	          								collator.m_expansion_[++ offset];
	        }
	    }
	    return m_CEBuffer_[0];
	}
	
	/**
	 * Gets the next implicit ce for codepoints
	 * @param codepoint current codepoint
	 * @param fixupoffset an offset to calculate the implicit ce
	 * @return implicit ce
	 */
	private int nextImplicit(int codepoint, int fixupoffset) 
	{
	  	if ((codepoint & 0xFFFE) == 0xFFFE 
	  		|| (0xD800 <= codepoint && codepoint <= 0xDC00)) {
	  		// illegal code value, use completely ignoreable!
	      	return IGNORABLE;
	  	}
	  	// we must skip all 00, 01, 02 bytes, so most bytes have 253 values
	  	// we must leave a gap of 01 between all values of the last byte, so 
	  	// the last byte has 126 values (3 byte case)
	  	// shift so that HAN all has the same first primary, for compression.
	  	// for the 4 byte case, we make the gap as large as we can fit.
	  	// Three byte forms are EC xx xx, ED xx xx, EE xx xx (with a gap of 1)
	  	// Four byte forms (most supplementaries) are EF xx xx xx 
	  	// (with a gap of LAST2_MULTIPLIER == 14)
	  	int last0 = codepoint - IMPLICIT_BOUNDARY_;
	  	int result = 0;
	  	if (last0 < 0) {
	  		// shift so HAN shares single block
	      	codepoint += IMPLICIT_HAN_SHIFT_; 
	      	int last1 = codepoint / IMPLICIT_LAST_COUNT_;
	      	last0 = codepoint % IMPLICIT_LAST_COUNT_;
	      	int last2 = last1 / IMPLICIT_OTHER_COUNT_;
	      	last1 %= IMPLICIT_OTHER_COUNT_;
	      	result = 0xEC030300 - fixupoffset + (last2 << 24) + (last1 << 16) 
	      														+ (last0 << 9);
	  	} 
	  	else {
	      	int last1 = last0 / IMPLICIT_LAST_COUNT2_;
	      	last0 %= IMPLICIT_LAST_COUNT2_;
	      	int last2 = last1 / IMPLICIT_OTHER_COUNT_;
	      	last1 %= IMPLICIT_OTHER_COUNT_;
	      	result = 0xEF030303 - fixupoffset + (last2 << 16) + (last1 << 8) 
	      							+ (last0 * IMPLICIT_LAST2_MULTIPLIER_);
	  	}
	  	m_CEBuffer_[0] = (result & RuleBasedCollator.CE_PRIMARY_MASK_) 
	  						| 0x00000505;
	  	m_CEBuffer_[1] = ((result & 0x0000FFFF) << 16) | 0x000000C0;
	  	m_CEBufferOffset_ = 1;
	  	m_CEBufferSize_ = 2;
	  	return m_CEBuffer_[0];
	}
	
	/**
	 * Returns the next ce associated with the following surrogate characters
	 * @param ch current character
	 * @return ce
	 */
	private int nextSurrogate(char ch)
	{
		char nextch = nextChar();
		if (nextch != CharacterIterator.DONE && 
			UTF16.isTrailSurrogate(nextch)) {
			int codepoint = UCharacterProperty.getRawSupplementary(ch, nextch);
			if ((codepoint >= 0x20000 && codepoint <= 0x2a6d6) 
				|| (codepoint >= 0x2F800 && codepoint <= 0x2FA1D)) { 
				// this might be a CJK supplementary cp
			    return nextImplicit(codepoint, 0x04000000);
			} 
			// or a regular one
			return nextImplicit(codepoint, 0);
		} 
		if (nextch != CharacterIterator.DONE) {
			previousChar(); // reverts back to the original position
		}
		return IGNORABLE; // completely ignorable
	}
	
	/**
	 * Returns the next ce for a hangul character, this is an implicit 
	 * calculation
	 * @param collator current collator
	 * @param ch current character
	 * @return hangul ce
	 */
	private int nextHangul(RuleBasedCollator collator, char ch)
	{
		char L = (char)(ch - HANGUL_SBASE_);
		
		// divide into pieces
		// do it in this order since some compilers can do % and / in one 
		// operation
		char T = (char)(L % HANGUL_TCOUNT_); 
		L /= HANGUL_TCOUNT_;
		char V = (char)(L % HANGUL_VCOUNT_);
		L /= HANGUL_VCOUNT_;
		
		// offset them
		L += HANGUL_LBASE_;
		V += HANGUL_VBASE_;
		T += HANGUL_TBASE_;
			
		// return the first CE, but first put the rest into the expansion 
		// buffer
		m_CEBufferSize_ = 0;
		if (!collator.m_isJamoSpecial_) { // FAST PATH
			m_CEBuffer_[m_CEBufferSize_ ++] = 
										collator.UCA_.m_trie_.getLeadValue(L);
			m_CEBuffer_[m_CEBufferSize_ ++] = 
										collator.UCA_.m_trie_.getLeadValue(V);
			
			if (T != HANGUL_TBASE_) {
			   	m_CEBuffer_[m_CEBufferSize_ ++] = 
										collator.UCA_.m_trie_.getLeadValue(T);
			}
			m_CEBufferOffset_ = 1;
			return m_CEBuffer_[0];
		} 
		else { 
			// Jamo is Special
			// Since Hanguls pass the FCD check, it is guaranteed that we 
			// won't be in the normalization buffer if something like this 
			// happens
			// Move Jamos into normalization buffer
			m_buffer_.append((char)L);
			m_buffer_.append((char)V);
			if (T != HANGUL_TBASE_) {
			    m_buffer_.append((char)T);
			}
			m_FCDLimit_ = m_source_.getIndex();  
			m_FCDStart_ = m_FCDLimit_ - 1;
			// Indicate where to continue in main input string after 
			// exhausting the buffer
			return IGNORABLE;
		}
	}
	
	/**
	 * <p>Special CE management. Expansions, contractions etc...</p>
	 * @param collator can be plain UCA 
	 * @param ce current ce
	 * @param ch current character
	 * @return next special ce
	 */
	private int nextSpecial(RuleBasedCollator collator, int ce, char ch) 
	{
		int codepoint = ch;
		Backup entrybackup = new Backup();
		backupInternalState(entrybackup);
	  	while (true) {
	    	// This loop will repeat only in the case of contractions, 
	    	// surrogate
		    switch(RuleBasedCollator.getTag(ce)) {
	    		case CE_NOT_FOUND_TAG_:
	      			// impossible case for icu4j
	      			return ce;
	    		case RuleBasedCollator.CE_SURROGATE_TAG_:
	    			if (isEnd()) {
	    				return IGNORABLE;
	    			}
	    			backupInternalState(m_backup_);
	    			char trail = nextChar();
	      			ce = nextSurrogate(collator, ce, trail);
	      			// calculate the supplementary code point value, 
	        		// if surrogate was not tailored we go one more round
	        		codepoint = 
	        		        UCharacterProperty.getRawSupplementary(ch, trail);
	      			break;
	    		case CE_THAI_TAG_:
	    			ce = nextThai(collator, ce, ch);
	      			break;
	    		case CE_SPEC_PROC_TAG_:
	    			ce = nextSpecialPrefix(collator, ce, entrybackup);
	      			break;
	    		case CE_CONTRACTION_TAG_:
	      			ce = nextContraction(collator, ce);
	      			break;
	    		case CE_LONG_PRIMARY_TAG_:
	      			return nextLongPrimary(ce);
	    		case CE_EXPANSION_TAG_:
	      			return nextExpansion(collator, ce);
	    		// various implicits optimization
	    		case CE_CJK_IMPLICIT_TAG_:    
	    			// 0x3400-0x4DB5, 0x4E00-0x9FA5, 0xF900-0xFA2D
	      			return nextImplicit(codepoint, 0x04000000);
			    case CE_IMPLICIT_TAG_: // everything that is not defined 
			      	return nextImplicit(codepoint, 0);
			    case CE_TRAIL_SURROGATE_TAG_: 
			      	return IGNORABLE; // DC00-DFFF broken surrogate
			    case CE_LEAD_SURROGATE_TAG_:  // D800-DBFF
			      	return nextSurrogate(ch);
			    case CE_HANGUL_SYLLABLE_TAG_: // AC00-D7AF
			      	return nextHangul(collator, ch);
			    case CE_CHARSET_TAG_:
			    	// not yet implemented probably after 1.8
			      	return CE_NOT_FOUND_;
			    default:
			      	ce = IGNORABLE;
			      	// synwee todo, throw exception or something here.
			}
			if (!RuleBasedCollator.isSpecial(ce)) {
				break;
			}
		}
		return ce;
	}
	
	/**
	 * Getting the previous Thai ce
	 * @param collator current collator
	 * @param ch current character
	 * @return previous Thai ce
	 */
	private int previousThai(RuleBasedCollator collator, int ce, char ch)
	{
		char prevch = previousChar();
		if (isBackwardsStart() || !isThaiBaseConsonant(ch)
			|| !isThaiPreVowel(prevch)) {
			if (prevch != CharacterIterator.DONE) {
				nextChar();
			}
	        // Treat Thai as a length one expansion
	        return collator.m_expansion_[getExpansionOffset(collator, ce)];
	    }
	    else
	    {
	        // Move the prevowel and the following base Consonant into the
	        // normalization buffer with their order swapped
	        // buffer is always clean when we are in the source string
	        m_buffer_.append(ch);
	        m_buffer_.append(prevch);
			m_bufferOffset_ = 2;
			
			if (m_source_.getIndex() == 0) {
	            m_FCDStart_ = 0;
	            m_FCDLimit_ = 2;
	        } 
	        else {
	            m_FCDStart_ = m_source_.getIndex();
	            m_FCDLimit_ = m_FCDStart_ + 2;
	        }
	
	 		return IGNORABLE;
	    }
	}
	
	/**
	 * Special processing is getting a CE that is preceded by a certain prefix.
	 * Currently this is only needed for optimizing Japanese length and 
	 * iteration marks. When we encouter a special processing tag, we go 
	 * backwards and try to see if we have a match. Contraction tables are used 
	 * - so the whole process is not unlike contraction. prefix data is stored 
	 * backwards in the table.
	 * @param collator current collator
	 * @param ce current ce
	 * @return previous ce
	 */
	private int previousSpecialPrefix(RuleBasedCollator collator, int ce)
	{
	    Backup backup = new Backup();
	    backupInternalState(backup);
	    while (true) {
	        // position ourselves at the begining of contraction sequence 
	        int offset = getContractionOffset(collator, ce);
	        int entryoffset = offset;
			if (isBackwardsStart()) {
	          	ce = collator.m_contractionCE_[offset];
	            break;
	        }
	        char prevch = previousChar();
	        while (prevch > collator.m_contractionIndex_[offset]) { 
	        	// since contraction codepoints are ordered, we skip all that 
	        	// are smaller
	            offset ++;
	        }
	        if (prevch == collator.m_contractionIndex_[offset]) {
	            ce = collator.m_contractionCE_[offset];
	        }
	        else {
	            // char was not in the table. prefix not found
	            ce = collator.m_contractionCE_[entryoffset];
	        }
	
	        if (!isSpecialPrefixTag(ce)) {
	            // char was in the contraction table, and the corresponding ce 
	            // is not a prefix ce.  We found the prefix, break out of loop, 
	            // this ce will end up being returned. 
	            break;
	        }
	    }
	    updateInternalState(backup);
	    return ce;
	}
	
	/**
	 * Retrieves the previous contraction ce. To ensure that the backwards and 
	 * forwards iteration matches, we take the current region of most possible 
	 * match and pass it through the forward iteration. This will ensure that 
	 * the obstinate problem of overlapping contractions will not occur.
	 * @param collator current collator
	 * @param ce current ce
	 * @param ch current character
	 * @return previous contraction ce
	 */
	private int previousContraction(RuleBasedCollator collator, int ce, char ch)
	{
		int entryoffset = getContractionOffset(collator, ce);
	    if (isBackwardsStart()) {
	        // start of string or this is not the end of any contraction
	        return collator.m_contractionCE_[entryoffset];
	    }
	    StringBuffer buffer = new StringBuffer();
	    while (collator.isUnsafe(ch) || isThaiBaseConsonant(ch)) {
	        buffer.insert(0, ch);
	        ch = previousChar();
	        if (isBackwardsStart()) {
	            break;
	        }
	    }
	    // adds the initial base character to the string
	    buffer.insert(0, ch);	
	    // a new collation element iterator is used to simply things, since 
	    // using the current collation element iterator will mean that the 
	    // forward and backwards iteration will share and change the same 
	    // buffers. it is going to be painful. 
	    CollationElementIterator temp = 
	    			new CollationElementIterator(buffer.toString(), collator);
		ce = temp.next();
		m_CEBufferSize_ = 0;
	    while (ce != NULLORDER) {
	    	if (m_CEBufferSize_ == m_CEBuffer_.length) {
	            try {
	            	int tempbuffer[] = new int[m_CEBuffer_.length + 50];
	            	System.arraycopy(m_CEBuffer_, 0, tempbuffer, 0, 
	            										m_CEBuffer_.length);
	            	m_CEBuffer_ = tempbuffer;
	            }
	            catch (Exception e)
	            {
	            	e.printStackTrace();
	            	return NULLORDER;
	            }
	        }
	        m_CEBuffer_[m_CEBufferSize_ ++] = ce;
	        ce = temp.next();
	    }
	    
	    m_CEBufferOffset_ = m_CEBufferSize_ - 1;
	    return m_CEBuffer_[m_CEBufferOffset_];
	}
	
	/**
	 * Returns the previous long primary ces
	 * @param ce long primary ce
	 * @return previous long primary ces
	 */
	private int previousLongPrimary(int ce)
	{
		m_CEBufferSize_ = 0;
		m_CEBuffer_[m_CEBufferSize_ ++] = 
			((ce & 0xFFFF00) << 8) | (CE_BYTE_COMMON_ << 8) | CE_BYTE_COMMON_;
	    m_CEBuffer_[m_CEBufferSize_ ++] = ((ce & 0xFF) << 24) 
	    							| RuleBasedCollator.CE_CONTINUATION_MARKER_;
	    m_CEBufferOffset_ = m_CEBufferSize_ - 1;
	    return m_CEBuffer_[m_CEBufferOffset_];
	}
	
	/**
	 * Returns the previous expansion ces
	 * @param collator current collator
	 * @param ce current ce
	 * @return previous expansion ce
	 */
	private int previousExpansion(RuleBasedCollator collator, int ce)
	{
		// find the offset to expansion table
	    int offset = getExpansionOffset(collator, ce);
	    m_CEBufferSize_ = getExpansionCount(ce);
	    if (m_CEBufferSize_ != 0) {
	        // less than 16 elements in expansion
	        for (int i = 0; i < m_CEBufferSize_; i ++) {
	          	m_CEBuffer_[i] = collator.m_expansion_[offset + i];
	        }
	        
	    }
	    else {
	        // null terminated ces
	        while (collator.m_expansion_[offset + m_CEBufferSize_] != 0) {
	          	m_CEBuffer_[m_CEBufferSize_] = 
	          				collator.m_expansion_[offset + m_CEBufferSize_];
	          	m_CEBufferSize_ ++;
	        }
	    }
	    m_CEBufferOffset_ = m_CEBufferSize_ - 1;
	    return m_CEBuffer_[m_CEBufferOffset_];
	}
	
	/**
	 * Returns previous hangul ces
	 * @param collator current collator
	 * @param ch current character
	 * @return previous hangul ce
	 */
	private int previousHangul(RuleBasedCollator collator, char ch)
	{
		char L = (char)(ch - HANGUL_SBASE_);
	    // we do it in this order since some compilers can do % and / in one
	    // operation
	    char T = (char)(L % HANGUL_TCOUNT_);
	    L /= HANGUL_TCOUNT_;
	    char V = (char)(L % HANGUL_VCOUNT_);
	    L /= HANGUL_VCOUNT_;
	
	    // offset them
	    L += HANGUL_LBASE_;
	    V += HANGUL_VBASE_;
	    T += HANGUL_TBASE_;
	
		m_CEBufferSize_ = 0;
		if (!collator.m_isJamoSpecial_) {
	        m_CEBuffer_[m_CEBufferSize_ ++] =
										collator.UCA_.m_trie_.getLeadValue(L);
	        m_CEBuffer_[m_CEBufferSize_ ++] =
										collator.UCA_.m_trie_.getLeadValue(V);
	        if (T != HANGUL_TBASE_) {
	            m_CEBuffer_[m_CEBufferSize_ ++] =
										collator.UCA_.m_trie_.getLeadValue(T);
	        }
	        m_CEBufferOffset_ = m_CEBufferSize_ - 1;
	        return m_CEBuffer_[m_CEBufferOffset_];
	    } 
	    else {
	        // Since Hanguls pass the FCD check, it is guaranteed that we won't 
	        // be in the normalization buffer if something like this happens
	        // Move Jamos into normalization buffer
	        m_buffer_.append(L);
	        m_buffer_.append(V);
	        if (T != HANGUL_TBASE_) {
	            m_buffer_.append(T);
	        } 
			
			m_FCDStart_ = m_source_.getIndex();
	        m_FCDLimit_ = m_FCDStart_ + 1;
	        return IGNORABLE;
	    }
	}
	
	/**
	 * Gets implicit codepoint ces
	 * @param codepoint current codepoint
	 * @param fixupoffset offset to shift ces for han
	 * @return implicit codepoint ces
	 */
	private int previousImplicit(int codepoint, int fixupoffset)
	{
      	if ((codepoint & 0xFFFE) == 0xFFFE 
      		|| (0xD800 <= codepoint && codepoint <= 0xDC00)) {
          	return IGNORABLE; // illegal code value, completely ignoreable! 
      	}
      	// we must skip all 00, 01, 02 bytes, so most bytes have 253 values
        // we must leave a gap of 01 between all values of the last byte, so 
        // the last byte has 126 values (3 byte case)
       	// we shift so that HAN all has the same first primary, for 
       	// compression.
       	// for the 4 byte case, we make the gap as large as we can fit.
       	// Three byte forms are EC xx xx, ED xx xx, EE xx xx (with a gap of 1)
       	// Four byte forms (most supplementaries) are EF xx xx xx (with a gap 
       	// of LAST2_MULTIPLIER == 14)
      	int last0 = codepoint - IMPLICIT_BOUNDARY_;
      	int result = 0;

      	if (last0 < 0) {
      		// shift HAN to share single block
          	codepoint += IMPLICIT_HAN_SHIFT_; 
          	int last1 = codepoint / IMPLICIT_LAST_COUNT_;
          	last0 = codepoint % IMPLICIT_LAST_COUNT_;
          	int last2 = last1 / IMPLICIT_OTHER_COUNT_;
          	last1 %= IMPLICIT_OTHER_COUNT_;
          	result = 0xEC030300 - fixupoffset + (last2 << 24) + (last1 << 16) 
          				+ (last0 << 9);
      	} 
      	else {
          	int last1 = last0 / IMPLICIT_LAST_COUNT2_;
          	last0 %= IMPLICIT_LAST_COUNT2_;
          	int last2 = last1 / IMPLICIT_OTHER_COUNT_;
          	last1 %= IMPLICIT_OTHER_COUNT_;
          	result = 0xEF030303 - fixupoffset + (last2 << 16) + (last1 << 8) 
          				+ (last0 * IMPLICIT_LAST2_MULTIPLIER_);
      	}
      	m_CEBufferSize_ = 2;
      	m_CEBufferOffset_ = 1;
      	m_CEBuffer_[0] = (result & RuleBasedCollator.CE_PRIMARY_MASK_) 
      						| 0x00000505;
      	m_CEBuffer_[1] = ((result & 0x0000FFFF) << 16) | 0x000000C0;
      	return m_CEBuffer_[1];
	}
	
	/**
	 * Gets the previous surrogate ce
	 * @param ch current character
	 * @return previous surrogate ce
	 */
	private int previousSurrogate(char ch)
	{
	   	if (isBackwardsStart()) {
	       	// we are at the start of the string, wrong place to be at
	       	return IGNORABLE;
	   	}
	   	char prevch = previousChar(); 
	   	// Handles Han and Supplementary characters here.
	   	if (UTF16.isLeadSurrogate(prevch)) {
	       	return previousImplicit(
	       				UCharacterProperty.getRawSupplementary(prevch, ch), 0);
	   	} 
	   	if (prevch != CharacterIterator.DONE) {
	    	nextChar();
	   	}
	   	return IGNORABLE; // completely ignorable 
	}
		
	/**
	 * <p>Special CE management. Expansions, contractions etc...</p>
	 * @param collator can be plain UCA 
	 * @param ce current ce
	 * @param ch current character
	 * @return previous special ce
	 */
	private int previousSpecial(RuleBasedCollator collator, int ce, char ch)
	{
	  	while(true) {
	    	// the only ces that loops are thai, special prefix and 
	    	// contractions 
	    	switch (RuleBasedCollator.getTag(ce)) {
	    		case CE_NOT_FOUND_TAG_:  // this tag always returns
	      			return ce;
	    		case RuleBasedCollator.CE_SURROGATE_TAG_: 
	      			// essentialy a disengaged lead surrogate. a broken 
	      			// sequence was encountered and this is an error
	      			return IGNORABLE;
	    		case CE_THAI_TAG_:
	      			ce = previousThai(collator, ce, ch);
	      			break;
	    		case CE_SPEC_PROC_TAG_:
	    			ce = previousSpecialPrefix(collator, ce);
	      			break;
	    		case CE_CONTRACTION_TAG_:
	        		return previousContraction(collator, ce, ch);
	   	 		case CE_LONG_PRIMARY_TAG_:
	      			return previousLongPrimary(ce);
	    		case CE_EXPANSION_TAG_: // always returns
	      			return previousExpansion(collator, ce);
	    		case CE_HANGUL_SYLLABLE_TAG_: // AC00-D7AF
	    			return previousHangul(collator, ch);
	      		case CE_LEAD_SURROGATE_TAG_:  // D800-DBFF
	      			return IGNORABLE; // broken surrogate sequence
	    		case CE_TRAIL_SURROGATE_TAG_: // DC00-DFFF
	    			return previousSurrogate(ch);
	    		case CE_CJK_IMPLICIT_TAG_: 
	    			// 0x3400-0x4DB5, 0x4E00-0x9FA5, 0xF900-0xFA2D*/
	      			return previousImplicit(ch, 0x04000000);
	    		case CE_IMPLICIT_TAG_: // everything that is not defined
	    			// UCA is filled with these. Tailorings are NOT_FOUND 
	      			return previousImplicit(ch,  0);
	    		case CE_CHARSET_TAG_: // this tag always returns
	      			return CE_NOT_FOUND_;
	    		default:           
	    			// this tag always returns
	      			ce = IGNORABLE;
	      			// synwee todo, throw exception or something here.
	    	}
	    	if (!RuleBasedCollator.isSpecial(ce)) {
	      		break;
	    	}
	  	}
	  	return ce;
	}
}
