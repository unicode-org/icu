/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/impl/Attic/UnicodeProperty.java,v $ 
* $Date: 2002/03/12 17:49:15 $ 
* $Revision: 1.3 $
*
*******************************************************************************
*/

package com.ibm.icu.impl;

import java.util.Locale;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.lang.UCharacterCategory;

/**
 * Internal class for Unicode property information.
 * @author synwee
 * @version since Feb 2002
 */
public final class UnicodeProperty 
{
	// public data members -----------------------------------------------
	
	/**
    * The minimum value for Supplementary code points
    */
    public static final int SUPPLEMENTARY_MIN_VALUE  = 0x10000;
    /**
     * Surrogate minimum value
     */
    public static final int SURROGATE_MIN_VALUE      = 0xD800;
    /**
     * Maximum surrogate value
     */
    public static final int SURROGATE_MAX_VALUE      = 0xDFFF;
    /**
     * Lead surrogate minimum value
     */
    public static final int LEAD_SURROGATE_MIN_VALUE = SURROGATE_MIN_VALUE;
	/**
     * Trail surrogate minimum value
     */
    public static final int TRAIL_SURROGATE_MIN_VALUE = 0xDC00; 
    /**
     * Lead surrogate maximum value
     */
    public static final int LEAD_SURROGATE_MAX_VALUE = 0xDBFF;
	/**
     * Trail surrogate maximum value
     */
    public static final int TRAIL_SURROGATE_MAX_VALUE = SURROGATE_MAX_VALUE;  
     /** 
    * The lowest Unicode code point value.
    */
    public static final int MIN_VALUE = 0;
    /**
    * The highest Unicode code point value (scalar value) according to the 
    * Unicode Standard.<br> 
    * This is a 21-bit value (21 bits, rounded up).<br>
    * Up-to-date Unicode implementation of java.lang.Character.MIN_VALUE
    */
    public static final int MAX_VALUE = 0x10ffff;   
    /**
     * Maximum number of expansion for a case mapping
     */
    public static final int MAX_CASE_MAP_SIZE = 10;                         
    /**
     * UnicodeData.txt property object
     */
    public static final UCharacterProperty PROPERTY;   
    /**
    * Turkish ISO 639 2 character code
    */
    public static final String TURKISH_ = "tr";  
    /**
    * Azerbaijani ISO 639 2 character code
    */
    public static final String AZERBAIJANI_ = "az";    
    /**
    * Lithuanian ISO 639 2 character code
    */
    public static final String LITHUANIAN_ = "lt";                  
    /**
    * Latin capital letter i with dot above
    */ 
    public static final char LATIN_CAPITAL_LETTER_I_WITH_DOT_ABOVE_ = 0x130;
    /**
    * Latin small letter i with dot above
    */ 
    public static final char LATIN_SMALL_LETTER_DOTLESS_I_ = 0x131;
    /**
    * Latin lowercase i
    */
    public static final char LATIN_SMALL_LETTER_I_ = 0x69;
    
    // data member initialization -------------------------------------------
    
    /**
    * Initialization of the UCharacterProperty instance. 
    * RuntimeException thrown when data is missing or data has been corrupted.
    */
    static
    {
        try
        {
            PROPERTY = new UCharacterProperty();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e.getMessage());
        }
    }

    // public methods ------------------------------------------------------
    
    /**
     * <p>Returns the lead surrogate of the argument supplementary codepoint.</p>
     * <p>Supplementary codepoint validity checks are not done, hence
     * results are not guaranteed if a non-supplementary codepoint is pass
     * as argument.</p>
     * @param supplementary supplementary codepoint
     * @return lead surrogate character
     */
    public static char getLeadSurrogate(int supplementary) 
    {
        return (char)(LEAD_SURROGATE_OFFSET_ + 
                      (supplementary >> LEAD_SURROGATE_SHIFT));
    }
        
    /**
    * <p>Returns the trail surrogate of the argument supplementary codepoint.
    * </p>
    * <p>Supplementary codepoint validity checks are not done, hence
    * results are not guaranteed if a non-supplementary codepoint is pass
    * as argument.</p>
    * @param supplementary supplementary codepoint     
    * @return trail surrogate character
    */
    public static char getTrailSurrogate(int supplementary) 
    {
        return (char)(TRAIL_SURROGATE_MIN_VALUE + 
                      (supplementary & TRAIL_SURROGATE_MASK));       
    }
    
    /**
    * <p>Converts argument code point and returns a String object representing 
    * the code point's value in UTF16 format.</p>
    * <p>This method does not check for the validity of the codepoint, the
    * results are not guaranteed if a invalid codepoint is passed as 
    * argument.</p>
    * <p>The result is a string whose length is 1 for non-supplementary code 
    * points, 2 otherwise.</p>
    * @param ch code point
    * @return string representation of the code point
    */
    public static String toString(int ch)
    {   
        if (ch < SUPPLEMENTARY_MIN_VALUE) {
            return String.valueOf((char)ch);
        }
        
        StringBuffer result = new StringBuffer();
        result.append(getLeadSurrogate(ch));
        result.append(getTrailSurrogate(ch));
        return result.toString();
    }
      
    /**
    * Forms a supplementary code point from the argument character<br>
    * Note this is for internal use hence no checks for the validity of the
    * surrogate characters are done
    * @param lead lead surrogate character
    * @param trail trailing surrogate character
    * @return code point of the supplementary character
    */
    public static int getRawSupplementary(char lead, char trail)
    {
        return (lead << LEAD_SURROGATE_SHIFT) + trail + SURROGATE_OFFSET_;
    }
    
    /**
    * <p>Extract a single UTF-32 value from a string.</p>
    * <p>If the char retrieved is part of a surrogate pair, its supplementary 
    * character will be returned. If a complete supplementary character is 
    * not found the incomplete character will be returned.</p>
    * <p>Bounds checking is not done here/p>
    * @param source array of UTF-16 chars
    * @param offset16 UTF-16 offset to the start of the character.
    * @return UTF-32 value for the UTF-32 value that contains the char at 
    *         offset16. The boundaries of that codepoint are the same as in 
    *         <code>bounds32()</code>. 
    */
    public static int charAt(String source, int offset16) 
    {             
        char single = source.charAt(offset16);
        if (single < LEAD_SURROGATE_MIN_VALUE || 
            single > TRAIL_SURROGATE_MAX_VALUE) {
            return single;
        }

        // Convert the UTF-16 surrogate pair if necessary.
        // For simplicity in usage, and because the frequency of pairs is 
        // low, look both directions.
                
	    if (single <= LEAD_SURROGATE_MAX_VALUE) {
	        ++ offset16;
	        if (source.length() != offset16) {
    	        char trail = source.charAt(offset16);
	            if (trail >= TRAIL_SURROGATE_MIN_VALUE &&
	                trail <= TRAIL_SURROGATE_MAX_VALUE) {
	                return getRawSupplementary(single, trail);
	            }
	        }
	    } 
	    else 
	    { 
	        -- offset16;
	        if (offset16 >= 0) {
	        	// single is a trail surrogate so
	        	char lead = source.charAt(offset16);
	        	if (lead >= LEAD_SURROGATE_MIN_VALUE &&
	        	    lead <= LEAD_SURROGATE_MAX_VALUE) {
	         	   return getRawSupplementary(lead, single);
	        	}
	        }
	    } 
	    return single; // return unmatched surrogate
    }
    
    /**
    * <p>Determines how many chars this char32 requires.</p>
    * <p>No checks on the codepoint is done</p>
    * @param char32 the input codepoint.
    * @return 2 if is in supplementary space, otherwise 1. 
    */
    public static int getCharCount(int char32) 
    {
        if (char32 < SUPPLEMENTARY_MIN_VALUE) {
        	return 1;
        }
        return 2;
    }
    
    /**
    * <p>Append a single UTF-32 value to the end of a StringBuffer.</p>
    * <p>No checks are done on char32 before appending</p>
    * @param char32 value to append.
    * @return the updated StringBuffer
    */
    public static StringBuffer append(StringBuffer target, int char32)
    {
        // Write the UTF-16 values
        if (char32 >= SUPPLEMENTARY_MIN_VALUE) 
        {
            target.append(getLeadSurrogate(char32));
	        target.append(getTrailSurrogate(char32));
        } 
	    else {
	        target.append((char)char32);
	    }
	    return target;
    }
        
    /**
    * Special casing lowercase management
    * @param locale current locale
    * @param ch code point to convert
    * @param index of exception containing special case information
    * @param uchariter text iterator with index at position of ch
    * @param buffer to add lowercase
    * @return size of the lower case character in UTF16 format
    */
    public static int getSpecialLowerCase(Locale locale, int index, int ch, 
                                           UCharacterIterator uchariter,
                                           StringBuffer buffer)
    {
        int exception = PROPERTY.getException(index, 
                                    UCharacterProperty.EXC_SPECIAL_CASING_);
        if (exception < 0) {
        	int offset = uchariter.getIndex();
            // fill u and i with the case mapping result string
            // use hardcoded conditions and mappings
            if (locale.getLanguage().equals(LITHUANIAN_) &&
                // base characters, find accents above
                (((ch == LATIN_CAPITAL_LETTER_I_ || 
                   ch == LATIN_CAPITAL_LETTER_J_ ||
                   ch == LATIN_CAPITAL_I_WITH_OGONEK_) &&
                  isFollowedByMOREABOVE(uchariter, offset)) ||
                  // precomposed with accent above, no need to find one
                  (ch == LATIN_CAPITAL_I_WITH_GRAVE_ || 
                   ch == LATIN_CAPITAL_I_WITH_ACUTE_ || 
                   ch == LATIN_CAPITAL_I_WITH_TILDE_))) {
                   // lithuanian: add a dot above if there are more accents 
                   // above (to always have the dot)
                   switch(ch) {
                   case LATIN_CAPITAL_LETTER_I_: 
                        buffer.append((char)LATIN_SMALL_LETTER_I_);
                        buffer.append((char)COMBINING_DOT_ABOVE_);
                        return 2;
                   case LATIN_CAPITAL_LETTER_J_: 
                        buffer.append((char)LATIN_SMALL_LETTER_J_);
                        buffer.append((char)COMBINING_DOT_ABOVE_);
                        return 2;
                   case LATIN_CAPITAL_I_WITH_OGONEK_:
                        buffer.append((char)LATIN_SMALL_LETTER_I_WITH_OGONEK_);
                        buffer.append((char)COMBINING_DOT_ABOVE_);
                        return 2;
                   case LATIN_CAPITAL_I_WITH_GRAVE_: 
                        buffer.append((char)LATIN_SMALL_LETTER_I_);
                        buffer.append((char)COMBINING_DOT_ABOVE_);
                        buffer.append((char)COMBINING_GRAVE_ACCENT_);
                        return 3;
                   case LATIN_CAPITAL_I_WITH_ACUTE_: 
                        buffer.append((char)LATIN_SMALL_LETTER_I_);
                        buffer.append((char)COMBINING_DOT_ABOVE_);
                        buffer.append((char)COMBINING_ACUTE_ACCENT_);
                        return 3;
                   case LATIN_CAPITAL_I_WITH_TILDE_:
                        buffer.append((char)LATIN_SMALL_LETTER_I_);
                        buffer.append((char)COMBINING_DOT_ABOVE_);
                        buffer.append((char)COMBINING_TILDE_);
                        return 3;
                   }
                   /*
                   Note: This handling of I and of dot above differs from 
                   Unicode 3.1.1's SpecialCasing-5.txt because the AFTER_i 
                   condition there does not work for decomposed I+dot above.
                   This fix is being proposed to the UTC.
                   */
            } 
            
            String language = locale.getLanguage();
            if ((language.equals(TURKISH_) || language.equals(AZERBAIJANI_)) && 
                ch == LATIN_CAPITAL_LETTER_I_ && 
                !isFollowedByDotAbove(uchariter, offset)) {
                // turkish: I maps to dotless i
                // other languages or turkish with decomposed I+dot above: 
                // I maps to i
                buffer.append(LATIN_SMALL_LETTER_DOTLESS_I_);
                return 1;
            } 
            
			if (ch == COMBINING_DOT_ABOVE_ && isAFTER_I(uchariter, offset) 
			    && !isFollowedByMOREABOVE(uchariter, offset)) {
                // decomposed I+dot above becomes i (see handling of 
                // U+0049 for turkish) and removes the dot above
                return 0; // remove the dot (continue without output)
            } 
            
            if (ch == GREEK_CAPITAL_LETTER_SIGMA_ && 
                isCFINAL(uchariter, offset) && 
                isNotCINITIAL(uchariter, offset)) {
                // greek capital sigma maps depending on surrounding cased 
                // letters
                buffer.append(GREEK_SMALL_LETTER_RHO_);
                return 1;
            } 
            
			// no known conditional special case mapping, use a normal mapping
            if (PROPERTY.hasExceptionValue(index, 
                                        UCharacterProperty.EXC_LOWERCASE_)) {
                int oldlength = buffer.length();
                append(buffer, PROPERTY.getException(index, 
                                            UCharacterProperty.EXC_LOWERCASE_)); 
                return buffer.length() - oldlength;                            
            }
            
			append(buffer, ch);
			return getCharCount(ch);
        }
        else {
            // get the special case mapping string from the data file
            index = exception & LAST_CHAR_MASK_;
            int oldlength = buffer.length();
            PROPERTY.getLowerCase(index, buffer);
            return buffer.length() - oldlength;
        }
    }
    
    /**
     * Gets the lower case map of the argument codepoint
     * @param locale locale which the lowercase is looked for
     * @param ch codepoint whose lower case is to be matched
     * @param uchariter text iterator positioned at the codepoint ch
     * @param buffer buffer to store result string
     * @return size of the lowercased codepoint in UTF16 format
     */
    public static int toLowerCase(Locale locale, int ch, 
                                   UCharacterIterator uchariter, 
                                   StringBuffer buffer)
    {
    	int props = PROPERTY.getProperty(ch);
        if (!UCharacterProperty.isExceptionIndicator(props)) {
            int type = UCharacterProperty.getPropType(props);
            if (type == UCharacterCategory.UPPERCASE_LETTER ||
                type == UCharacterCategory.TITLECASE_LETTER) {
                ch += UCharacterProperty.getSignedValue(props);
            }
        } else {
            int index = UCharacterProperty.getExceptionIndex(props);
            if (PROPERTY.hasExceptionValue(index, 
                                UCharacterProperty.EXC_SPECIAL_CASING_)) {
                return getSpecialLowerCase(locale, index, ch, uchariter, 
                                           buffer);
            } 
            if (PROPERTY.hasExceptionValue(index, 
                                       UCharacterProperty.EXC_LOWERCASE_)) {
                ch = PROPERTY.getException(index, 
                                            UCharacterProperty.EXC_LOWERCASE_);
            }
        }
        append(buffer, ch);
        return getCharCount(ch);
	}

	/**
     * Gets the lower case map of the argument codepoint
     * @param locale locale which the lowercase is looked for
     * @param ch codepoint whose lower case is to be matched
     * @param uchariter text iterator positioned at the codepoint ch
     * @param result array of char to store the result
     * @return size oflowercased codepoint in UTF16 format
     */
    public static int toLowerCase(Locale locale, int ch, 
                                     UCharacterIterator uchariter, 
                                     char buffer[])
    {
        int props = PROPERTY.getProperty(ch);
        if (!UCharacterProperty.isExceptionIndicator(props)) {
            int type = UCharacterProperty.getPropType(props);
            if (type == UCharacterCategory.UPPERCASE_LETTER ||
                type == UCharacterCategory.TITLECASE_LETTER) {
                ch += UCharacterProperty.getSignedValue(props);
            }
        } else {
            int index = UCharacterProperty.getExceptionIndex(props);
            if (PROPERTY.hasExceptionValue(index, 
                                UCharacterProperty.EXC_SPECIAL_CASING_)) {
                StringBuffer strbuffer = new StringBuffer(1);
                int result = getSpecialLowerCase(locale, index, ch, uchariter, 
                                                 strbuffer);
                strbuffer.getChars(0, result, buffer, 0);
                return result;
            } 
            if (PROPERTY.hasExceptionValue(index, 
                                       UCharacterProperty.EXC_LOWERCASE_)) {
                ch = PROPERTY.getException(index, 
                                            UCharacterProperty.EXC_LOWERCASE_);
            }
        }
        if (ch < SUPPLEMENTARY_MIN_VALUE) {
        	buffer[0] = (char)ch;
        	return 1;
        }
        buffer[0] = getLeadSurrogate(ch);
        buffer[1] = getTrailSurrogate(ch);
        return 2;
	}
    
    /**
     * Gets the lower case mappings of the substring from index start to the
     * character before end.
     * @param locale locale which the mappings will be searched
     * @param str string to map 
     * @param start start index of the substring to map
     * @param limit one index pass the last character to map
     * @param result string buffer to store lower case string
     */
    public static void toLowerCase(Locale locale, String str, int start, 
                                   int limit, StringBuffer result) {
        UCharacterIterator ucharIter = new UCharacterIterator(str);
        int                strIndex  = start;
        
        while (strIndex < limit) { 
        	ucharIter.setIndex(strIndex);
	        int ch = ucharIter.currentCodepoint();
	        
	        toLowerCase(locale, ch, ucharIter, result);
	        strIndex ++;
	        if (ch > SUPPLEMENTARY_MIN_VALUE) {
	        	strIndex ++;
	        }
        }
    }
    
    /**
    * Special casing uppercase management
    * @param locale locale which the mappings will be based on
    * @param index of exception containing special case information
    * @param ch code point to convert
    * @param uchariter text iterator which ch belongs to
    * @param upperflag true if uppercase mapping is desired, false for title 
    *        casing
    * @param buffer to add uppercase
    * @return size of uppercased codepoint in UTF16 format
    */
    public static int getSpecialUpperOrTitleCase(Locale locale, int index, 
                                        int ch, UCharacterIterator uchariter, 
                                        boolean upperflag, StringBuffer buffer)
    {
        int exception = PROPERTY.getException(index, 
                                    UCharacterProperty.EXC_SPECIAL_CASING_);
        if (exception < 0) {
            String language = locale.getLanguage();
            // use hardcoded conditions and mappings
            if ((language.equals(TURKISH_) || language.equals(AZERBAIJANI_))
                && ch == LATIN_SMALL_LETTER_I_) {
                // turkish: i maps to dotted I
                buffer.append(LATIN_CAPITAL_LETTER_I_WITH_DOT_ABOVE_);
                return 1;
            } 
            
            if (language.equals(LITHUANIAN_) && ch == COMBINING_DOT_ABOVE_ 
                && isAFTER_i(uchariter, uchariter.getIndex())) {
                // lithuanian: remove DOT ABOVE after U+0069 "i" with 
                // upper or titlecase
                return 0; // remove the dot (continue without output)
            } 
            
            // no known conditional special case mapping, use a normal mapping
           if (!upperflag && PROPERTY.hasExceptionValue(index, 
                                          UCharacterProperty.EXC_TITLECASE_)) {
               ch = PROPERTY.getException(index, 
                                            UCharacterProperty.EXC_TITLECASE_);                    
           }
           else {
               if (PROPERTY.hasExceptionValue(index, 
                                          UCharacterProperty.EXC_UPPERCASE_)) {
	               ch = PROPERTY.getException(index, 
                                            UCharacterProperty.EXC_UPPERCASE_); 
               }
           }
           
           append(buffer, ch);
           return getCharCount(ch);
        }
        
		// get the special case mapping string from the data file
        index = exception & LAST_CHAR_MASK_;
        int oldlength = buffer.length();
        if (upperflag) {
	        PROPERTY.getUpperCase(index, buffer);
        }
        else {
          	PROPERTY.getTitleCase(index, buffer);
        }
        return buffer.length() - oldlength;
    }
    
    /**
     * Gets the upper or title case map of the codepoint
     * @param locale locale which the mappings will be searched 
     * @param ch codepoint whose upper or title case will be mapped
     * @param uchariter text iterator positioned at the codepoint
     * @param upperflag flag true if uppercase is desired, false for title case
     * @param buffer buffer to store result map
     * @return size of uppercased codepoint in UTF16 format
     */
	public static int toUpperOrTitleCase(Locale locale, int ch, 
	                           UCharacterIterator uchariter, boolean upperflag,
	                           StringBuffer buffer) 
    {
        int props = PROPERTY.getProperty(ch);
        if (!UCharacterProperty.isExceptionIndicator(props)) {
            int type = UCharacterProperty.getPropType(props);
            if (type == UCharacterCategory.LOWERCASE_LETTER) {
            	ch -= UCharacterProperty.getSignedValue(props);
            }
        } else {
            int index = UCharacterProperty.getExceptionIndex(props);
            if (PROPERTY.hasExceptionValue(index, 
                                UCharacterProperty.EXC_SPECIAL_CASING_)) {
                return getSpecialUpperOrTitleCase(locale, index, ch, uchariter, 
                                                  upperflag, buffer);
            } 
            if (!upperflag && PROPERTY.hasExceptionValue(index, 
                                          UCharacterProperty.EXC_TITLECASE_)) {
                ch = PROPERTY.getException(index, 
                                           UCharacterProperty.EXC_TITLECASE_);
            }
            else {
             	if (PROPERTY.hasExceptionValue(index, 
                                          UCharacterProperty.EXC_UPPERCASE_)) {
                    ch = PROPERTY.getException(index, 
                                            UCharacterProperty.EXC_UPPERCASE_);
                }
            }
        }
        append(buffer, ch);
        return getCharCount(ch);
    }
    
    /**
     * Gets the upper or title case map of the codepoint
     * @param locale locale which the mappings will be searched 
     * @param ch codepoint whose upper or title case will be mapped
     * @param uchariter text iterator positioned at the codepoint
     * @param upperflag flag true if uppercase is desired, false for title case
     * @param buffer buffer to store result map
     * @return size of uppercased codepoint in UTF16 format
     */
	public static int toUpperOrTitleCase(Locale locale, int ch, 
	                           UCharacterIterator uchariter, boolean upperflag,
	                           char buffer[]) 
    {
        int props = PROPERTY.getProperty(ch);
        if (!UCharacterProperty.isExceptionIndicator(props)) {
            int type = UCharacterProperty.getPropType(props);
            if (type == UCharacterCategory.LOWERCASE_LETTER) {
            	ch -= UCharacterProperty.getSignedValue(props);
            }
        } else {
            int index = UCharacterProperty.getExceptionIndex(props);
            if (PROPERTY.hasExceptionValue(index, 
                                UCharacterProperty.EXC_SPECIAL_CASING_)) {
               	StringBuffer strbuffer = new StringBuffer(1);
                int result = getSpecialUpperOrTitleCase(locale, index, ch, 
                                                        uchariter, upperflag, 
                                                        strbuffer);
                strbuffer.getChars(0, result, buffer, 0);
                return result;
            } 
            if (!upperflag && PROPERTY.hasExceptionValue(index, 
                                          UCharacterProperty.EXC_TITLECASE_)) {
                ch = PROPERTY.getException(index, 
                                           UCharacterProperty.EXC_TITLECASE_);
            }
            else {
             	if (PROPERTY.hasExceptionValue(index, 
                                          UCharacterProperty.EXC_UPPERCASE_)) {
                    ch = PROPERTY.getException(index, 
                                            UCharacterProperty.EXC_UPPERCASE_);
                }
            }
        }
        if (ch < SUPPLEMENTARY_MIN_VALUE) {
        	buffer[0] = (char)ch;
        	return 1;
        }
        buffer[0] = getLeadSurrogate(ch);
        buffer[1] = getTrailSurrogate(ch);
        return 2;
    }
    
    /**
     * Gets the uppercasing of the argument string.
     * @param locale locale which the mappings will be searched
     * @param str string to map 
     * @param start start index of the substring to map
     * @param limit one index pass the last character to map
     */
    public static String toUpperCase(Locale locale, String str, int start, 
                                     int limit) 
    {
        UCharacterIterator ucharIter = new UCharacterIterator(str);
        int                strIndex  = start;
        StringBuffer       result    = new StringBuffer(limit - start);
        
        while (strIndex < limit) { 
        	ucharIter.setIndex(strIndex);
	        int ch = ucharIter.currentCodepoint();
	        
	        toUpperOrTitleCase(locale, ch, ucharIter, true, result);
	        strIndex ++;
	        if (ch > SUPPLEMENTARY_MIN_VALUE) {
	        	strIndex ++;
	        }
        }
        return result.toString();
    }
    
    /**
    * <p>Gets the titlecase version of the argument string.</p>
    * <p>Position for titlecasing is determined by the argument break 
    * iterator, hence the user can customized his break iterator for 
    * a specialized titlecasing. In this case only the forward iteration 
    * needs to be implemented.
    * If the break iterator passed in is null, the default Unicode algorithm
    * will be used to determine the titlecase positions.
    * </p>
    * <p>Only positions returned by the break iterator will be title cased,
    * character in between the positions will all be in lower case.</p>
    * <p>Casing is dependent on the default locale and context-sensitive</p>
    * @param str source string to be performed on
    * @param breakiter break iterator to determine the positions in which
    *        the character should be title cased.
    * @return lowercase version of the argument string
    */
 	public static String toTitleCase(Locale locale, String str, 
 	                                 BreakIterator breakiter)
 	{
 		UCharacterIterator ucharIter = new UCharacterIterator(str);
		int                length    = str.length();
        StringBuffer       result    = new StringBuffer();
        
        breakiter.setText(str);
        
        int                index     = breakiter.first();
       	// titlecasing loop
	    while (index != BreakIterator.DONE && index < length) {
	    	// titlecase the character at the found index
	        int ch = charAt(str, index);
	        ucharIter.setIndex(index);
	        index += getCharCount(ch);
        	int size = toUpperOrTitleCase(locale, ch, ucharIter, false, result);
        	int next = breakiter.next();
        	if (index != BreakIterator.DONE && index < next) {
	        	// lowercase [prev..index]
        		toLowerCase(locale, str, index, next, result);
            }
            index = next;
        }
        return result.toString();
 	}
 	    
    // private data members --------------------------------------------------
 
 	/**
    * Shift value for lead surrogate to form a supplementary character.
    */
	private static final int LEAD_SURROGATE_SHIFT     = 10;
	/**
    * Mask to retrieve the significant value from a trail surrogate.
    */
	private static final int TRAIL_SURROGATE_MASK     = 0x3FF;   
    /** 
    * Offset to add to combined surrogate pair to avoid msking.
    */
    private static final int SURROGATE_OFFSET_ = 
                           SUPPLEMENTARY_MIN_VALUE - 
                           (SURROGATE_MIN_VALUE << LEAD_SURROGATE_SHIFT) - 
                           TRAIL_SURROGATE_MIN_VALUE;   
    /**
     * Value that all lead surrogate starts with
     */
    private static final int LEAD_SURROGATE_OFFSET_ = 
	                                    LEAD_SURROGATE_MIN_VALUE - 
	                                   (SUPPLEMENTARY_MIN_VALUE 
	                                    >> LEAD_SURROGATE_SHIFT); 	                                   
    /**
    * Latin uppercase I
    */
    private static final char LATIN_CAPITAL_LETTER_I_ = 0x49;
    /**
    * Combining dot above
    */
    private static final char COMBINING_DOT_ABOVE_ = 0x307;
    /**
    * LATIN SMALL LETTER J
    */
    private static final int LATIN_SMALL_LETTER_J_ = 0x6a;
    /**
    * LATIN SMALL LETTER I WITH OGONEK
    */
    private static final int LATIN_SMALL_LETTER_I_WITH_OGONEK_ = 0x12f;
    /**
    * LATIN SMALL LETTER I WITH TILDE BELOW
    */
    private static final int LATIN_SMALL_LETTER_I_WITH_TILDE_BELOW_ = 0x1e2d;
    /**
    * LATIN SMALL LETTER I WITH DOT BELOW
    */
    private static final int LATIN_SMALL_LETTER_I_WITH_DOT_BELOW_ = 0x1ecb;
    /**
    * Combining class for combining mark above
    */
    private static final int COMBINING_MARK_ABOVE_CLASS_ = 230;
    
    /**
    * LATIN CAPITAL LETTER J
    */
    private static final int LATIN_CAPITAL_LETTER_J_ = 0x4a;
    
    /**
    * LATIN CAPITAL LETTER I WITH OGONEK
    */
    private static final int LATIN_CAPITAL_I_WITH_OGONEK_ = 0x12e;
    /**
    * LATIN CAPITAL LETTER I WITH TILDE
    */
    private static final int LATIN_CAPITAL_I_WITH_TILDE_ = 0x128;
    /**
    * LATIN CAPITAL LETTER I WITH GRAVE
    */
    private static final int LATIN_CAPITAL_I_WITH_GRAVE_ = 0xcc;
    /**
    * LATIN CAPITAL LETTER I WITH ACUTE
    */
    private static final int LATIN_CAPITAL_I_WITH_ACUTE_ = 0xcd;
    /**
    * COMBINING GRAVE ACCENT
    */
    private static final int COMBINING_GRAVE_ACCENT_ = 0x300;
    /**
    * COMBINING ACUTE ACCENT
    */
    private static final int COMBINING_ACUTE_ACCENT_ = 0x301;
    /**
    * COMBINING TILDE
    */
    private static final int COMBINING_TILDE_ = 0x303;
    /**
    * Greek capital letter sigma
    */
    private static final char GREEK_CAPITAL_LETTER_SIGMA_ = 0x3a3;
    /**
    * Greek small letter sigma
    */
    private static final char GREEK_SMALL_LETTER_SIGMA_ = 0x3c3;
    /**
    * Greek small letter rho
    */
    private static final char GREEK_SMALL_LETTER_RHO_ = 0x3c2;
    /**
    * Hyphens
    */
    private static final int HYPHEN_      = 0x2010;
    private static final int SOFT_HYPHEN_ = 0xAD;
    /**
    * To get the last character out from a data type
    */
    private static final int LAST_CHAR_MASK_ = 0xFFFF;
    /**
    * To get the last byte out from a data type
    */
    private static final int LAST_BYTE_MASK_ = 0xFF;
    /**
    * Shift 16 bits
    */
    private static final int SHIFT_16_ = 16;
                                     
	// private methods -------------------------------------------------------
	
	/**
    * <p>Sets a codepoint to the argument char array.</p>
    * <p>This method does not bother to check the validity of the codepoint</p>
    * @param target char array to be set with the new code point
    * @param char32 code point
    * @return the size of the codepoint
    */
    private static int setCodepoint(char[] target, int char32)
    {
        // Write the UTF-16 values
        if (char32 >= SUPPLEMENTARY_MIN_VALUE) {
            target[0] = getLeadSurrogate(char32);
            target[1] = getTrailSurrogate(char32);
            return 2;
        }
        target[0] = (char)char32;
        return 1;
    }
    
    /**
    * <p>Returns a value indicating a code point's Unicode category.</p>
    * <p>This method does not check for the codepoint validity</p>
    * @param ch code point whose type is to be determined
    * @return category which is a value of UCharacterCategory
    */
    private static int getType(int ch)
    {
        return UCharacterProperty.getPropType(PROPERTY.getProperty(ch));
    }
    
    /**
    * <p>Gets the combining class of the argument codepoint</p>
    * <p>This method does not check for the codepoint validity</p>
    * @param ch code point whose combining is to be retrieved
    * @return the combining class of the codepoint
    */
    private static int getCombiningClass(int ch)
    {
        int props = PROPERTY.getProperty(ch);
        if(!UCharacterProperty.isExceptionIndicator(props)) {
			return NormalizerImpl.getCombiningClass(ch);
        }
        else {
            // the combining class is in bits 23..16 of the first exception value
            return (PROPERTY.getException(
                                    PROPERTY.getExceptionIndex(props), 
                                    UCharacterProperty.EXC_COMBINING_CLASS_)
                                    >> SHIFT_16_) & LAST_BYTE_MASK_;
        }
    }
    
    /**
    * Determines if a string at offset is preceded by any base characters 
    * { 'i', 'j', U+012f, U+1e2d, U+1ecb } with no intervening character with
    * combining class = 230
    * @param uchariter text iterator to be determined
    * @param offset offset in string to check
    * @return true if some characters preceding the offset index belongs to
    *         the set { 'i', 'j', U+012f, U+1e2d, U+1ecb }
    * @see SpecialCasing.txt
    */
    private static boolean isAFTER_i(UCharacterIterator uchariter, int offset) 
    {
    	uchariter.setIndex(offset);
    	
    	int ch = uchariter.previousCodepoint();
    	
        while (ch != UCharacterIterator.DONE_CODEPOINT) {
            if (ch == LATIN_SMALL_LETTER_I_ || ch == LATIN_SMALL_LETTER_J_ || 
                ch == LATIN_SMALL_LETTER_I_WITH_OGONEK_ ||
                ch == LATIN_SMALL_LETTER_I_WITH_TILDE_BELOW_ || 
                ch == LATIN_SMALL_LETTER_I_WITH_DOT_BELOW_) {
                return true; // preceded by TYPE_i
            }
    
            int cc = getCombiningClass(ch);
            if (cc == 0 || cc == COMBINING_MARK_ABOVE_CLASS_) {
                // preceded by different base character not TYPE_i), or 
                // intervening cc == 230
                return false; 
            }
            ch = uchariter.previousCodepoint();
        }

        return false; // not preceded by TYPE_i
    }

    /**
    * Determines if a string at offset is preceded by base characters 'I' with 
    * no intervening combining class = 230
    * @param uchariter text iterator to be determined
    * @param offset offset in string to check
    * @return true if some characters preceding the offset index is the
    *         character 'I' with no intervening combining class = 230
    * @see SpecialCasing.txt
    */
    private static boolean isAFTER_I(UCharacterIterator uchariter, int offset) 
    {
    	uchariter.setIndex(offset);
    	
    	int ch = uchariter.previousCodepoint();
    	
        while (ch != UCharacterIterator.DONE_CODEPOINT) {
            if (ch == LATIN_CAPITAL_LETTER_I_) {
                return true; // preceded by I
            }

            int cc = getCombiningClass(ch);
            if (cc == 0 || cc == COMBINING_MARK_ABOVE_CLASS_) {
                // preceded by different base character (not I), or 
                // intervening cc == 230
                return false; 
            }
 			ch = uchariter.previousCodepoint();           
        }

        return false; // not preceded by I
    }
    
    /** 
    * Determines if offset is not followed by a sequence consisting of
    * an ignorable sequence and then a cased letter {Ll, Lu, Lt}.
    * @param uchariter String iterator to determine
    * @param offset offset in string to check
    * @return false if any character after index in src is a cased letter
    * @see SpecialCasing.txt
    */
    private static boolean isCFINAL(UCharacterIterator uchariter, int offset) 
    {
    	// iterator should have been determined to be not null by caller
        uchariter.setIndex(offset);
    	int ch = uchariter.nextCodepoint();
    	
    	while (ch != UCharacterIterator.DONE_CODEPOINT) {
            int cat = getType(ch);
            if (cat == UCharacterCategory.LOWERCASE_LETTER || 
                cat == UCharacterCategory.UPPERCASE_LETTER ||
                cat == UCharacterCategory.TITLECASE_LETTER) {
                return false; // followed by cased letter
            }
            if (!isIgnorable(ch, cat)) {
                return true; // not ignorable
            }
            ch = uchariter.nextCodepoint();
        }

        return true;
    }

    /**
    * Determines if offset is not preceded by a sequence consisting of a cased 
    * letter {Ll, Lu, Lt} and an ignorable sequence. 
    * @param uchariter string iterator to determine
    * @param offset offset in string to check
    * @return true if any character before index in src is a cased letter
    * @see SpecialCasing.txt
    */
    private static boolean isNotCINITIAL(UCharacterIterator uchariter, 
                                         int offset) 
    {
    	uchariter.setIndex(offset);
    	int ch = uchariter.previousCodepoint();
    	
        while (ch != UCharacterIterator.DONE_CODEPOINT) {
            int cat = getType(ch);
            if (cat == UCharacterCategory.LOWERCASE_LETTER || 
                cat == UCharacterCategory.UPPERCASE_LETTER ||
                cat == UCharacterCategory.TITLECASE_LETTER) {
                return true; // preceded by cased letter
            }
            if (!isIgnorable(ch, cat)) {
                return false; // not ignorable
            }
			ch = uchariter.previousCodepoint();
        }

        return false; 
    }

    /** 
    * Determines if a string at offset is followed by one or more characters 
    * of combining class = 230.
    * @param chariter text iterator to be determined
    * @param offset offset in string to check
    * @return true if a string at offset is followed by one or more characters 
    *         of combining class = 230.
    * @see SpecialCasing.txt
    */
    private static boolean isFollowedByMOREABOVE(UCharacterIterator uchariter, 
                                                 int offset) 
    {
        uchariter.setIndex(offset);
        int ch = uchariter.nextCodepoint();
        
        while (ch != UCharacterIterator.DONE_CODEPOINT) {
            int cc = getCombiningClass(ch);
            if (cc == COMBINING_MARK_ABOVE_CLASS_) {
                return true; // at least one cc==230 following 
            }
            if (cc == 0) {
                return false; // next base character, no more cc==230 following
            }
            ch = uchariter.nextCodepoint();
        }

        return false; // no more cc == 230 following
    }

    /** 
    * Determines if a string at offset is followed by a dot above 
    * with no characters of combining class == 230 in between 
    * @param uchariter text iterator to be determined
    * @param offset offset in string to check
    * @return true if a string at offset is followed by oa dot above 
    *         with no characters of combining class == 230 in between
    * @see SpecialCasing.txt
    */
    private static boolean isFollowedByDotAbove(UCharacterIterator uchariter, 
                                                int offset) 
    {
        uchariter.setIndex(offset);
        int ch = uchariter.nextCodepoint();
        
        while (ch != UCharacterIterator.DONE_CODEPOINT) {
            if (ch == COMBINING_DOT_ABOVE_) {
                return true;
            }
            int cc = getCombiningClass(ch);
            if (cc == 0 || cc == COMBINING_MARK_ABOVE_CLASS_) {
                return false; // next base character or cc==230 in between
            }
            ch = uchariter.nextCodepoint();
        }

        return false; // no dot above following
    }
    
    /**  
    * In Unicode 3.1.1, an ignorable sequence is a sequence of *zero* or more 
    * characters from the set {HYPHEN, SOFT HYPHEN, general category = Mn}.
    * (Expected to change!) 
    * @param ch codepoint
    * @param cat category of the argument codepoint
    * @return true if ch is case ignorable.
    */
    private static boolean isIgnorable(int ch, int cat) 
    {
        return cat == UCharacterCategory.NON_SPACING_MARK || ch == HYPHEN_ || 
                      ch == SOFT_HYPHEN_;
    }
}

