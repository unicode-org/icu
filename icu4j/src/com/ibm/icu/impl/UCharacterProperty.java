/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: 
*         /usr/cvs/icu4j/icu4j/src/com/ibm/icu/text/UCharacterPropertyDB.java $ 
* $Date: 2002/06/20 01:18:09 $ 
* $Revision: 1.9 $
*
*******************************************************************************
*/

package com.ibm.icu.impl;

import java.io.InputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Locale;
import com.ibm.icu.util.VersionInfo;
import com.ibm.icu.lang.UCharacterCategory;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.BreakIterator;

/**
* <p>Internal class used for Unicode character property database.</p>
* <p>This classes store binary data read from uprops.dat. 
* It does not have the capability to parse the data into more high-level 
* information. It only returns bytes of information when required.</p>
* <p>Due to the form most commonly used for retrieval, array of char is used
* to store the binary data.</p>
* <p>UCharacterPropertyDB also contains information on accessing indexes to 
* significant points in the binary data.</p>
* <p>Responsibility for molding the binary data into more meaning form lies on 
* <a href=UCharacter.html>UCharacter</a>.</p>
* @author Syn Wee Quek
* @since release 2.1, february 1st 2002
* @draft 2.1
*/

public final class UCharacterProperty implements Trie.DataManipulate
{
	// public data members -----------------------------------------------
	
	/**
    * Trie data
    */
    public CharTrie m_trie_;    
    /**
    * Character property table
    */
    public int m_property_[];
    /**
    * Unicode version
    */
    public VersionInfo m_unicodeVersion_;
    /**
     * Exception indicator for uppercase type
     */
    public static final int EXC_UPPERCASE_ = 0;
    /**
     * Exception indicator for lowercase type
     */
    public static final int EXC_LOWERCASE_ = 1;
    /**
     * Exception indicator for titlecase type
     */
    public static final int EXC_TITLECASE_ = 2;
    /**
     * Exception indicator for digit type
     */
    public static final int EXC_UNUSED_ = 3;
    /**
     * Exception indicator for numeric type
     */
    public static final int EXC_NUMERIC_VALUE_ = 4;
    /**
     * Exception indicator for denominator type
     */
    public static final int EXC_DENOMINATOR_VALUE_ = 5;
    /**
     * Exception indicator for mirror type
     */
    public static final int EXC_MIRROR_MAPPING_ = 6;
    /**
     * Exception indicator for special casing type
     */
    public static final int EXC_SPECIAL_CASING_ = 7;
    /**
     * Exception indicator for case folding type
     */
    public static final int EXC_CASE_FOLDING_ = 8;
    /**
     * EXC_COMBINING_CLASS_ is not found in ICU.
     * Used to retrieve the combining class of the character in the exception
     * value
     */
    public static final int EXC_COMBINING_CLASS_ = 9;
    /**
     * Non numeric type
     */
    public static final int NON_NUMERIC_TYPE_ = 0;
    /**
     * Numeric type for decimal digits
     */
    public static final int DECIMAL_DIGIT_NUMERIC_TYPE_ = 1;
    /**
     * Numeric type for digits
     */
    public static final int DIGIT_NUMERIC_TYPE_ = 2;
    /**
     * Numeric type for non digits numbers
     */
    public static final int NON_DIGIT_NUMERIC_TYPE_ = 3;
    /**
     * Maximum number of expansion for a case mapping
     */
    public static final int MAX_CASE_MAP_SIZE = 10;                         
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
    
    // public methods ----------------------------------------------------
  
    /**
    * toString method for printing
    */
    public String toString()
    {
        StringBuffer result = new StringBuffer("Property block\n");
        result.append(super.toString());
        result.append("\nsize of property data ");
        result.append(m_property_.length);
        result.append("\nsize of exception data ");
        result.append(m_exception_.length);
        result.append("\nsize of case data ");
        result.append(m_case_.length);
        return result.toString();
    }
        
    /**
    * Extracts out the type value from property.
    * For use in enumeration.
    * @param value of trie value associated with a codepoint
    */
    public int extract(int value)
    {    
        // access the general category from the 32-bit properties, and those 
        // from the 16-bit trie value    
        return getPropType(m_property_[value]);
    }
      
    /**
    * Called by com.ibm.icu.util.Trie to extract from a lead surrogate's 
    * data the index array offset of the indexes for that lead surrogate.
    * @param property data value for a surrogate from the trie, including the
    *        folding offset
    * @return data offset or 0 if there is no data for the lead surrogate
    */
    public int getFoldingOffset(int value)
    {
        if ((value & SUPPLEMENTARY_FOLD_INDICATOR_MASK_) != 0) {
            return (value & SUPPLEMENTARY_FOLD_OFFSET_MASK_);
        }
        else {
            return 0;
        }
    }
    
    /**
    * Gets the property value at the index
    * @param ch code point whose property value is to be retrieved
    * @return property value of code point
    */
    public int getProperty(int ch)
    {
        return m_property_[m_trie_.getCodePointValue(ch)];
    }
    
    /**
    * Returns a value indicating a character category from the argument property
    * value
    * @param unicode character property
    * @return category
    */
    public static int getPropType(int prop)
    {
        // Since character information data are packed together.
	    // This is the category mask for getting the category information
        return prop & LAST_5_BIT_MASK_;
    }
    
    /**
    * Determines if the argument props indicates that the exception block has 
    * to be accessed for data
    * @param prop property value
    * @return true if this is an exception indicator false otherwise
    */
    public static boolean isExceptionIndicator(int prop)
    {
        return (prop & EXCEPTION_MASK_) != 0;
    }
    
    /**
     * Getting the numberic type
     * @param prop property value
     * @return number type in prop
     */
     public static int getNumericType(int prop)
     {
     	return (prop >> NUMERIC_TYPE_SHIFT_) & NUMERIC_TYPE_MASK_;
     }
    
    /**
    * Getting the signed numeric value of a character embedded in the property
    * argument
    * @param prop the character
    * @return signed numberic value
    */
    public static int getSignedValue(int prop)
    {
        return (prop >> VALUE_SHIFT_);
    }
      
    /**
    * Getting the exception index for argument property
    * @param prop character property 
    * @return exception index 
    */
    public static int getExceptionIndex(int prop)
    {
        return (prop >> VALUE_SHIFT_) & UNSIGNED_VALUE_MASK_AFTER_SHIFT_;
    }
      
    /**
    * Checking if property indicates mirror element
    * @param prop property value
    * @return true if mirror indicator is set, false otherwise
    */
    public static boolean isMirrored(int prop)
    {
        return (prop & MIRROR_MASK_) != 0;
    }
      
    /**
    * Getting the direction data in the property value
    * @param prop property value
    * @return direction value in property
    */
    public static int getDirection(int prop)
    {
        return (prop >> BIDI_SHIFT_) & BIDI_MASK_AFTER_SHIFT_;
    }
    
    /**
    * Getting the unsigned numeric value of a character embedded in the property
    * argument
    * @param prop the character
    * @return unsigned numberic value
    */
    public static int getUnsignedValue(int prop)
    {
        return (prop >> VALUE_SHIFT_) & UNSIGNED_VALUE_MASK_AFTER_SHIFT_;
    }
    
    /**
    * Determines if the exception value passed in has the kind of information
    * which the indicator wants, e.g if the exception value contains the digit
    * value of the character
    * @param index exception index
    * @param indicator type indicator
    * @return true if type value exist
    */
    public boolean hasExceptionValue(int index, int indicator) 
    {
        return (m_exception_[index] & (1 << indicator)) != 0;
    }
      
    /**
    * Gets the exception value at the index, assuming that data type is 
    * available. Result is undefined if data is not available. Use 
    * hasExceptionValue() to determine data's availability.
    * @param index 
    * @param exception data type
    * @return exception data type value at index
    */
    public int getException(int index, int etype)
    {
        // contained in exception data
        int evalue = m_exception_[index];
        
        switch (etype)
        {
        case EXC_COMBINING_CLASS_ :
            return evalue;
        default :
            index ++;
            // contained in the exception digit address
            index = addExceptionOffset(evalue, etype, index);
        }
        return m_exception_[index];
    }
    
    /**
    * Gets the folded case value at the index
    * @param index of the case value to be retrieved
    * @return folded case value at index
    */
    public int getFoldCase(int index)
    {
        char single = m_case_[index];
        if (UTF16.LEAD_SURROGATE_MIN_VALUE <= single && 
            single <= UTF16.TRAIL_SURROGATE_MAX_VALUE) {
        // Convert the UTF-16 surrogate pair if necessary.
        // For simplicity in usage, and because the frequency of pairs is low,
        // look both directions.
                  
	        if (single <= UTF16.LEAD_SURROGATE_MAX_VALUE) {
	            char trail = m_case_[index + 1];
	            if (UTF16.LEAD_SURROGATE_MIN_VALUE <= trail && 
		            trail <= UTF16.TRAIL_SURROGATE_MAX_VALUE) {
	                return getRawSupplementary(single, trail);
	            }
	        } 
	        else 
	        { 
	            char lead = m_case_[index - 1];
	            if (UTF16.LEAD_SURROGATE_MIN_VALUE <= lead && 
		            lead <= UTF16.LEAD_SURROGATE_MAX_VALUE) {
	                return getRawSupplementary(lead, single);
	            }
	        }
	    }
	    return single;
    }
    
    /**
    * Gets the folded case value at the index
    * @param index of the case value to be retrieved
    * @param count number of characters to retrieve
    * @param buffer string buffer to add result to
    */
    public void getFoldCase(int index, int count, StringBuffer str) 
    {
        // first 2 chars are for the simple mappings
        index += 2;
        while (count > 0) {
        	str.append(m_case_[index]);
        	index ++;
        	count --;
        }
    }
    
    /**
    * Gets the upper case value at the index
    * @param index of the case value to be retrieved
    * @param buffer string buffer to add result to
    */
    public void getUpperCase(int index, StringBuffer buffer)
    {
    	int count = m_case_[index];
        // last 5 bits of the first char in m_case_ gives the position of the 
        // alternate uppercase characters
        index += (count & LAST_5_BIT_MASK_) + 1;
        count = (count >> SHIFT_5_) & LAST_5_BIT_MASK_;
                
        for (int j = 0; j < count; j ++) {
        	buffer.append(m_case_[index + j]);
        }
    }
    
    /**
    * Gets the upper case value at the index
    * @param index of the case value to be retrieved
    * @param buffer string buffer to add result to
    */
    public void getTitleCase(int index, StringBuffer buffer)
    {
        int count = m_case_[index];
        // last 5 bits of the first char in m_case_ gives the position of the 
        // alternate uppercase characters
        index += (count & LAST_5_BIT_MASK_) + 1 + 
                 ((count >> SHIFT_5_) & LAST_5_BIT_MASK_);
        count = (count >> SHIFT_10_) & LAST_5_BIT_MASK_;
                
        for (int j = 0; j < count; j ++) {
        	buffer.append(m_case_[index + j]);
        }
    }
      
    /**
    * Gets the lower case value at the index
    * @param index of the case value to be retrieved
    * @param buffer string buffer to add result to
    */
    public void getLowerCase(int index, StringBuffer buffer)
    {
        int count = m_case_[index] & LAST_5_BIT_MASK_;
        // last 5 bits of the first char in m_case_ gives the size of the 
        // lowercase characters
        index ++;
        for (int j = 0; j < count; j ++) {
        	buffer.append(m_case_[index + j]);
        }
    }
    
    /**
     * Gets the unicode additional properties
     * @param codepoint codepoint whose additional properties is to be 
     *                  retrieved
     * @param column
     * @return unicode properties
     */ 
   	public int getAdditional(int codepoint, int column) { 
   		if (column < 0 || column >= m_additionalColumnsCount_) { 
           return 0; 
       } 
      
       return m_additionalVectors_[
                     m_additionalTrie_.getCodePointValue(codepoint) + column]; 
   	} 
   	
   	/**
     * <p>Get the "age" of the code point.</p>
     * <p>The "age" is the Unicode version when the code point was first
     * designated (as a non-character or for Private Use) or assigned a 
     * character.</p>
     * <p>This can be useful to avoid emitting code points to receiving 
     * processes that do not accept newer characters.</p>
     * <p>The data is from the UCD file DerivedAge.txt.</p>
     * <p>This API does not check the validity of the codepoint.</p>
     * @param ch The code point.
     * @return the Unicode version number
     * @draft ICU 2.1
     */
    public VersionInfo getAge(int codepoint) 
    {
    	int version = getAdditional(codepoint, 0) >> AGE_SHIFT_;
    	return VersionInfo.getInstance(
                           (version >> FIRST_NIBBLE_SHIFT_) & LAST_NIBBLE_MASK_,
                           version & LAST_NIBBLE_MASK_, 0, 0);
    }

	/**
	 * <p>Check a binary Unicode property for a code point.</p> 
	 * <p>Unicode, especially in version 3.2, defines many more properties 
	 * than the original set in UnicodeData.txt.</p>
	 * <p>This API is intended to reflect Unicode properties as defined in 
	 * the Unicode Character Database (UCD) and Unicode Technical Reports 
	 * (UTR).</p>
	 * <p>For details about the properties see 
	 * <a href=http://www.unicode.org/>http://www.unicode.org/</a>.</p>
	 * <p>For names of Unicode properties see the UCD file 
	 * PropertyAliases.txt.</p>
	 * <p>This API does not check the validity of the codepoint.</p>
	 * <p>Important: If ICU is built with UCD files from Unicode versions 
	 * below 3.2, then properties marked with "new" are not or 
	 * not fully available.</p>
	 * @param codepoint Code point to test.
	 * @param property selector constant from com.ibm.icu.lang.UProperty, 
	 *        identifies which binary property to check.
	 * @return true or false according to the binary Unicode property value 
	 *         for ch. Also false if property is out of bounds or if the 
	 *         Unicode version does not have data for the property at all, or 
	 *         not for this code point.
	 * @see com.ibm.icu.lang.UProperty
	 * @draft ICU 2.1
	 */
	public boolean hasBinaryProperty(int codepoint, int property) 
	{
		switch(property) {
    		case UProperty.ALPHABETIC: {
        		// Lu+Ll+Lt+Lm+Lo+Other_Alphabetic
        		int generaltype = getPropType(getProperty(codepoint));
        		boolean generalmatch = 
        		        generaltype == UCharacterCategory.UPPERCASE_LETTER ||
        		        generaltype == UCharacterCategory.LOWERCASE_LETTER ||
        		        generaltype == UCharacterCategory.TITLECASE_LETTER ||
        		        generaltype == UCharacterCategory.MODIFIER_LETTER ||
        		        generaltype == UCharacterCategory.OTHER_LETTER;
        		return generalmatch ||
                       compareAdditionalType(getAdditional(codepoint, 1), 
                                             OTHER_ALPHABETIC_PROPERTY_);
    		}
    		case UProperty.ASCII_HEX_DIGIT: {
        		return compareAdditionalType(getAdditional(codepoint, 1),
											 ASCII_HEX_DIGIT_PROPERTY_);
    		}
    		case UProperty.BIDI_CONTROL: {
        		return compareAdditionalType(getAdditional(codepoint, 1),
        		                             BIDI_CONTROL_PROPERTY_);
    		}
    		case UProperty.BIDI_MIRRORED: {
        		return isMirrored(getProperty(codepoint));
    		}
    		case UProperty.DASH: {
        		return compareAdditionalType(getAdditional(codepoint, 1),
        		                             DASH_PROPERTY_);
    		}
    		case UProperty.DEFAULT_IGNORABLE_CODE_POINT: {
        		// Cf+Cc+Cs+Other_Default_Ignorable_Code_Point-White_Space
        		int generaltype = getPropType(getProperty(codepoint));
        		if (generaltype == UCharacterCategory.FORMAT ||
        		    generaltype == UCharacterCategory.CONTROL ||
        		    generaltype == UCharacterCategory.SURROGATE) {
        		    return true;
        		}
        		int additionalproperty = getAdditional(codepoint, 1);
        		return compareAdditionalType(additionalproperty, 
       		                   OTHER_DEFAULT_IGNORABLE_CODE_POINT_PROPERTY_)
		               || compareAdditionalType(additionalproperty, 
		                                        WHITE_SPACE_PROPERTY_) ||
		               compareAdditionalType(additionalproperty, 
                     			OTHER_DEFAULT_IGNORABLE_CODE_POINT_PROPERTY_);
    		}
    		case UProperty.DEPRECATED: {
        		return compareAdditionalType(getAdditional(codepoint, 1),
        		                             DEPRECATED_PROPERTY_);
    		}
    		case UProperty.DIACRITIC: {
        		return compareAdditionalType(getAdditional(codepoint, 1),
        		                             DIACRITIC_PROPERTY_);
    		}
    		case UProperty.EXTENDER: {
        		return compareAdditionalType(getAdditional(codepoint, 1),
        		                         EXTENDER_PROPERTY_);
    		}
    		case UProperty.FULL_COMPOSITION_EXCLUSION: {
        		return NormalizerImpl.isFullCompositionExclusion(codepoint);
    		}
    		case UProperty.GRAPHEME_BASE: {
    			int generaltype = getPropType(getProperty(codepoint));
        		// [0..10FFFF]-Cc-Cf-Cs-Co-Cn-Zl-Zp-Grapheme_Link-Grapheme_Extend ==
         		// [0..10FFFF]-Cc-Cf-Cs-Co-Cn-Zl-Zp-Grapheme_Link-(Me+Mn+Mc+Other_Grapheme_Extend) ==
         		// [0..10FFFF]-Cc-Cf-Cs-Co-Cn-Zl-Zp-Me-Mn-Mc-Grapheme_Link-Other_Grapheme_Extend
         		// getType(c out of range) returns Cn so we need not check for the range
         		if (generaltype == UCharacterCategory.FORMAT || 
         		    generaltype == UCharacterCategory.SURROGATE || 
         		    generaltype == UCharacterCategory.PRIVATE_USE ||
         		    generaltype == UCharacterCategory.GENERAL_OTHER_TYPES ||
         		    generaltype == UCharacterCategory.LINE_SEPARATOR ||
         		    generaltype == UCharacterCategory.PARAGRAPH_SEPARATOR ||
         		    generaltype == UCharacterCategory.ENCLOSING_MARK ||
         		    generaltype == UCharacterCategory.NON_SPACING_MARK ||
         		    generaltype == UCharacterCategory.COMBINING_SPACING_MARK) {
         		    return true;
         		}
         		int additionalproperty = getAdditional(codepoint, 1);
                return compareAdditionalType(additionalproperty,
                                             GRAPHEME_LINK_PROPERTY_) ||
                       compareAdditionalType(additionalproperty,
                                             OTHER_GRAPHEME_EXTEND_PROPERTY_);
    		}
    		case UProperty.GRAPHEME_EXTEND: {
        		// Me+Mn+Mc+Other_Grapheme_Extend-Grapheme_Link
        		int generaltype = getPropType(getProperty(codepoint));
        		if (generaltype == UCharacterCategory.ENCLOSING_MARK ||
        		    generaltype == UCharacterCategory.NON_SPACING_MARK ||
        		    generaltype == UCharacterCategory.COMBINING_SPACING_MARK)
        		{
        			return true;
        		}
                int additionalproperty = getAdditional(codepoint, 1);
                return compareAdditionalType(additionalproperty, 
                                           OTHER_GRAPHEME_EXTEND_PROPERTY_) ||
                       compareAdditionalType(additionalproperty, 
                                             GRAPHEME_LINK_PROPERTY_) || 
					   compareAdditionalType(additionalproperty,
					                         OTHER_GRAPHEME_EXTEND_PROPERTY_);
    		}
    		case UProperty.GRAPHEME_LINK: {
        		return compareAdditionalType(getAdditional(codepoint, 1),
        		                             GRAPHEME_LINK_PROPERTY_);
    		}
    		case UProperty.HEX_DIGIT: {
        		return compareAdditionalType(getAdditional(codepoint, 1),
        		                             HEX_DIGIT_PROPERTY_);
    		}
    		case UProperty.HYPHEN: {
        		return compareAdditionalType(getAdditional(codepoint, 1),
        		                             HYPHEN_PROPERTY_);
    		}
    		case UProperty.ID_CONTINUE: {
        		// ID_Start+Mn+Mc+Nd+Pc == Lu+Ll+Lt+Lm+Lo+Nl+Mn+Mc+Nd+Pc
        		int generaltype = getPropType(getProperty(codepoint));
        		return generaltype == UCharacterCategory.UPPERCASE_LETTER ||
        		       generaltype == UCharacterCategory.LOWERCASE_LETTER || 
        		       generaltype == UCharacterCategory.TITLECASE_LETTER ||
        		       generaltype == UCharacterCategory.MODIFIER_LETTER ||
        		       generaltype == UCharacterCategory.OTHER_LETTER ||
        		       generaltype == UCharacterCategory.LETTER_NUMBER ||
        		       generaltype == UCharacterCategory.NON_SPACING_MARK ||
        		       generaltype == 
        		                  UCharacterCategory.COMBINING_SPACING_MARK ||
        		       generaltype == UCharacterCategory.DECIMAL_DIGIT_NUMBER
        		       || generaltype == 
        		                     UCharacterCategory.CONNECTOR_PUNCTUATION;
    		}
    		case UProperty.ID_START: {
        		// Lu+Ll+Lt+Lm+Lo+Nl
        		int generaltype = getPropType(getProperty(codepoint));
        		return generaltype == UCharacterCategory.UPPERCASE_LETTER ||
        		       generaltype == UCharacterCategory.LOWERCASE_LETTER || 
        		       generaltype == UCharacterCategory.TITLECASE_LETTER ||
        		       generaltype == UCharacterCategory.MODIFIER_LETTER ||
        		       generaltype == UCharacterCategory.OTHER_LETTER ||
        		       generaltype == UCharacterCategory.LETTER_NUMBER;
    		}
    		case UProperty.IDEOGRAPHIC: {
        		return compareAdditionalType(getAdditional(codepoint, 1),
        		                             IDEOGRAPHIC_PROPERTY_);
    		}
    		case UProperty.IDS_BINARY_OPERATOR: {
        		return compareAdditionalType(getAdditional(codepoint, 1),
        		                             IDS_BINARY_OPERATOR_PROPERTY_);
    		}
    		case UProperty.IDS_TRINARY_OPERATOR: {
        		return compareAdditionalType(getAdditional(codepoint, 1),
        		                             IDS_TRINARY_OPERATOR_PROPERTY_);
			}
    		case UProperty.JOIN_CONTROL: {
        		return compareAdditionalType(getAdditional(codepoint, 1),
        		                             JOIN_CONTROL_PROPERTY_);
    		}
    		case UProperty.LOGICAL_ORDER_EXCEPTION: {
        		return compareAdditionalType(getAdditional(codepoint, 1),
      		                             LOGICAL_ORDER_EXCEPTION_PROPERTY_);
    		}
    		case UProperty.LOWERCASE: {
        		// Ll+Other_Lowercase
        		int generaltype = getPropType(getProperty(codepoint));
        		if (generaltype == UCharacterCategory.LOWERCASE_LETTER) {
        			return true;
        		}
        		return compareAdditionalType(getAdditional(codepoint, 1),
        		                             OTHER_LOWERCASE_PROPERTY_);
    		}
    		case UProperty.MATH: {
        		// Sm+Other_Math 
        		int generaltype = getPropType(getProperty(codepoint));
        		if (generaltype == UCharacterCategory.MATH_SYMBOL) {
        			return true;
        		}
        		return compareAdditionalType(getAdditional(codepoint, 1),
        		                             OTHER_MATH_PROPERTY_);
    		}
    		case UProperty.NONCHARACTER_CODE_POINT: {
        		return compareAdditionalType(getAdditional(codepoint, 1),
        		                          NONCHARACTER_CODE_POINT_PROPERTY_);
    		}
    		case UProperty.QUOTATION_MARK: {
        		return compareAdditionalType(getAdditional(codepoint, 1),
        		                             QUOTATION_MARK_PROPERTY_);
    		}
    		case UProperty.RADICAL: {
        		return compareAdditionalType(getAdditional(codepoint, 1),
        		                             RADICAL_PROPERTY_);
    		}
    		case UProperty.SOFT_DOTTED: {
        		return compareAdditionalType(getAdditional(codepoint, 1),
        		                             SOFT_DOTTED_PROPERTY_);
    		}
    		case UProperty.TERMINAL_PUNCTUATION: {
        		return compareAdditionalType(getAdditional(codepoint, 1),
        		                             TERMINAL_PUNCTUATION_PROPERTY_);
    		}
		    case UProperty.UNIFIED_IDEOGRAPH: {
        		return compareAdditionalType(getAdditional(codepoint, 1),
        		                             UNIFIED_IDEOGRAPH_PROPERTY_);
		    }
    		case UProperty.UPPERCASE: {
        		// Lu+Other_Uppercase 
        		int generaltype = getPropType(getProperty(codepoint));
        		if (generaltype == UCharacterCategory.UPPERCASE_LETTER) {
        			return true;
        		}
        		return compareAdditionalType(getAdditional(codepoint, 1),
                     	                     OTHER_UPPERCASE_PROPERTY_);
    		}
    		case UProperty.WHITE_SPACE: {
        		return compareAdditionalType(getAdditional(codepoint, 1),
        		                             WHITE_SPACE_PROPERTY_);
    		}
    		case UProperty.XID_CONTINUE: {
        		return compareAdditionalType(getAdditional(codepoint, 1),
        		                             XID_CONTINUE_PROPERTY_);
    		}
    		case UProperty.XID_START: {
        		return compareAdditionalType(getAdditional(codepoint, 1),
        		                             XID_START_PROPERTY_);
    		}
    		default:
        		// not a known binary property
        		return false;
    	}
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
        return (lead << LEAD_SURROGATE_SHIFT_) + trail + SURROGATE_OFFSET_;
    }
    
    /**
    * Loads the property data and initialize the UCharacterProperty instance. 
    * @Exception thrown when data is missing or data has been corrupted.
    */    
    public static UCharacterProperty getInstance() throws RuntimeException
    {
    	if (INSTANCE_ == null) {
    		try {
    			INSTANCE_ = new UCharacterProperty();
    		}
        	catch (Exception e) {
            	throw new RuntimeException(e.getMessage());
        	}
    	}
    	return INSTANCE_;
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
    public int getSpecialLowerCase(Locale locale, int index, int ch, 
                                   UnicodeCharacterIterator uchariter,
                                   StringBuffer buffer)
    {
    	int exception = getException(index, 
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
            if (hasExceptionValue(index, UCharacterProperty.EXC_LOWERCASE_)) {
                int oldlength = buffer.length();
                UTF16.append(buffer, getException(index, 
                                            UCharacterProperty.EXC_LOWERCASE_)); 
                return buffer.length() - oldlength;                            
            }
            
			UTF16.append(buffer, ch);
			return UTF16.getCharCount(ch);
        }
        else {
            // get the special case mapping string from the data file
            index = exception & LAST_CHAR_MASK_;
            int oldlength = buffer.length();
            getLowerCase(index, buffer);
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
    public int toLowerCase(Locale locale, int ch, 
                                   UnicodeCharacterIterator uchariter, 
                                   StringBuffer buffer)
    {
    	int props = getProperty(ch);
        if (!UCharacterProperty.isExceptionIndicator(props)) {
            int type = UCharacterProperty.getPropType(props);
            if (type == UCharacterCategory.UPPERCASE_LETTER ||
                type == UCharacterCategory.TITLECASE_LETTER) {
                ch += UCharacterProperty.getSignedValue(props);
            }
        } else {
            int index = UCharacterProperty.getExceptionIndex(props);
            if (hasExceptionValue(index, 
                                UCharacterProperty.EXC_SPECIAL_CASING_)) {
                return getSpecialLowerCase(locale, index, ch, uchariter, 
                                           buffer);
            } 
            if (hasExceptionValue(index, 
                                       UCharacterProperty.EXC_LOWERCASE_)) {
                ch = getException(index, UCharacterProperty.EXC_LOWERCASE_);
            }
        }
        UTF16.append(buffer, ch);
        return UTF16.getCharCount(ch);
	}

	/**
     * Gets the lower case map of the argument codepoint
     * @param locale locale which the lowercase is looked for
     * @param ch codepoint whose lower case is to be matched
     * @param uchariter text iterator positioned at the codepoint ch
     * @param result array of char to store the result
     * @return size oflowercased codepoint in UTF16 format
     */
    public int toLowerCase(Locale locale, int ch, 
                           UnicodeCharacterIterator uchariter, char buffer[])
    {
        int props = getProperty(ch);
        if (!UCharacterProperty.isExceptionIndicator(props)) {
            int type = UCharacterProperty.getPropType(props);
            if (type == UCharacterCategory.UPPERCASE_LETTER ||
                type == UCharacterCategory.TITLECASE_LETTER) {
                ch += UCharacterProperty.getSignedValue(props);
            }
        } else {
            int index = UCharacterProperty.getExceptionIndex(props);
            if (hasExceptionValue(index, 
                                  UCharacterProperty.EXC_SPECIAL_CASING_)) {
                StringBuffer strbuffer = new StringBuffer(1);
                int result = getSpecialLowerCase(locale, index, ch, uchariter, 
                                                 strbuffer);
                strbuffer.getChars(0, result, buffer, 0);
                return result;
            } 
            if (hasExceptionValue(index, UCharacterProperty.EXC_LOWERCASE_)) {
                ch = getException(index, UCharacterProperty.EXC_LOWERCASE_);
            }
        }
        if (ch < UTF16.SUPPLEMENTARY_MIN_VALUE) {
        	buffer[0] = (char)ch;
        	return 1;
        }
        buffer[0] = UTF16.getLeadSurrogate(ch);
        buffer[1] = UTF16.getTrailSurrogate(ch);
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
    public void toLowerCase(Locale locale, String str, int start, int limit, 
                            StringBuffer result) 
    {
        UnicodeCharacterIterator ucharIter = new UnicodeCharacterIterator(str);
        int                strIndex  = start;
        
        while (strIndex < limit) { 
        	ucharIter.setIndex(strIndex);
	        int ch = ucharIter.currentCodePoint();
	        
	        toLowerCase(locale, ch, ucharIter, result);
	        strIndex ++;
	        if (ch > UTF16.SUPPLEMENTARY_MIN_VALUE) {
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
    public int getSpecialUpperOrTitleCase(Locale locale, int index, int ch, 
                                          UnicodeCharacterIterator uchariter, 
                                          boolean upperflag, 
                                          StringBuffer buffer)
    {
        int exception = getException(index, 
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
           if (!upperflag && hasExceptionValue(index, 
                                          UCharacterProperty.EXC_TITLECASE_)) {
               ch = getException(index, UCharacterProperty.EXC_TITLECASE_);                    
           }
           else {
               if (hasExceptionValue(index, 
                                     UCharacterProperty.EXC_UPPERCASE_)) {
	               ch = getException(index, UCharacterProperty.EXC_UPPERCASE_); 
               }
           }
           
           UTF16.append(buffer, ch);
           return UTF16.getCharCount(ch);
        }
        
		// get the special case mapping string from the data file
        index = exception & LAST_CHAR_MASK_;
        int oldlength = buffer.length();
        if (upperflag) {
	        getUpperCase(index, buffer);
        }
        else {
          	getTitleCase(index, buffer);
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
	public int toUpperOrTitleCase(Locale locale, int ch, 
	                              UnicodeCharacterIterator uchariter, 
	                              boolean upperflag, StringBuffer buffer) 
    {
        int props = getProperty(ch);
        if (!UCharacterProperty.isExceptionIndicator(props)) {
            int type = UCharacterProperty.getPropType(props);
            if (type == UCharacterCategory.LOWERCASE_LETTER) {
            	ch -= UCharacterProperty.getSignedValue(props);
            }
        } else {
            int index = UCharacterProperty.getExceptionIndex(props);
            if (hasExceptionValue(index, 
                                  UCharacterProperty.EXC_SPECIAL_CASING_)) {
                return getSpecialUpperOrTitleCase(locale, index, ch, uchariter, 
                                                  upperflag, buffer);
            } 
            if (!upperflag && hasExceptionValue(index, 
                                         UCharacterProperty.EXC_TITLECASE_)) {
                ch = getException(index, UCharacterProperty.EXC_TITLECASE_);
            }
            else {
             	if (hasExceptionValue(index, 
                                      UCharacterProperty.EXC_UPPERCASE_)) {
                    ch = getException(index, 
                                      UCharacterProperty.EXC_UPPERCASE_);
                }
            }
        }
        UTF16.append(buffer, ch);
        return UTF16.getCharCount(ch);
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
	public int toUpperOrTitleCase(Locale locale, int ch, 
	                              UnicodeCharacterIterator uchariter, 
	                              boolean upperflag, char buffer[]) 
    {
        int props = getProperty(ch);
        if (!UCharacterProperty.isExceptionIndicator(props)) {
            int type = UCharacterProperty.getPropType(props);
            if (type == UCharacterCategory.LOWERCASE_LETTER) {
            	ch -= UCharacterProperty.getSignedValue(props);
            }
        } else {
            int index = UCharacterProperty.getExceptionIndex(props);
            if (hasExceptionValue(index, 
                                  UCharacterProperty.EXC_SPECIAL_CASING_)) {
               	StringBuffer strbuffer = new StringBuffer(1);
                int result = getSpecialUpperOrTitleCase(locale, index, ch, 
                                                        uchariter, upperflag, 
                                                        strbuffer);
                strbuffer.getChars(0, result, buffer, 0);
                return result;
            } 
            if (!upperflag && hasExceptionValue(index, 
                                         UCharacterProperty.EXC_TITLECASE_)) {
                ch = getException(index, UCharacterProperty.EXC_TITLECASE_);
            }
            else {
             	if (hasExceptionValue(index, 
                                      UCharacterProperty.EXC_UPPERCASE_)) {
                    ch = getException(index, 
                                      UCharacterProperty.EXC_UPPERCASE_);
                }
            }
        }
        if (ch < UTF16.SUPPLEMENTARY_MIN_VALUE) {
        	buffer[0] = (char)ch;
        	return 1;
        }
        buffer[0] = UTF16.getLeadSurrogate(ch);
        buffer[1] = UTF16.getTrailSurrogate(ch);
        return 2;
    }
    
    /**
     * Gets the uppercasing of the argument string.
     * @param locale locale which the mappings will be searched
     * @param str string to map 
     * @param start start index of the substring to map
     * @param limit one index pass the last character to map
     */
    public String toUpperCase(Locale locale, String str, int start, int limit) 
    {
        UnicodeCharacterIterator ucharIter = new UnicodeCharacterIterator(str);
        int                strIndex  = start;
        StringBuffer       result    = new StringBuffer(limit - start);
        
        while (strIndex < limit) { 
        	ucharIter.setIndex(strIndex);
	        int ch = ucharIter.currentCodePoint();
	        
	        toUpperOrTitleCase(locale, ch, ucharIter, true, result);
	        strIndex ++;
	        if (ch > UTF16.SUPPLEMENTARY_MIN_VALUE) {
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
 	public String toTitleCase(Locale locale, String str, 
 	                          BreakIterator breakiter)
 	{
 		UnicodeCharacterIterator ucharIter = new UnicodeCharacterIterator(str);
		int                length    = str.length();
        StringBuffer       result    = new StringBuffer();
        
        breakiter.setText(str);
        
        int                index     = breakiter.first();
       	// titlecasing loop
	    while (index != BreakIterator.DONE && index < length) {
	    	// titlecase the character at the found index
	        int ch = UTF16.charAt(str, index);
	        ucharIter.setIndex(index);
	        index += UTF16.getCharCount(ch);
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

    // protected variables -----------------------------------------------
  
    /**
    * Case table
    */
    protected char m_case_[];
      
    /**
    * Exception property table
    */
    protected int m_exception_[];
    /**
     * Extra property trie
     */
    protected CharTrie m_additionalTrie_;
    /**
     * Extra property vectors, 1st column for age and second for binary 
     * properties.
     */
    protected int m_additionalVectors_[];
    /**
     * Number of additional columns
     */
    protected int m_additionalColumnsCount_;
    
    // private variables -------------------------------------------------
  
  	/**
     * UnicodeData.txt property object
     */
    public static UCharacterProperty INSTANCE_ = null;   
        
    /**
    * Default name of the datafile
    */
    private static final String DATA_FILE_NAME_ = "data/uprops.dat";
      
    /**
    * Default buffer size of datafile
    */
    private static final int DATA_BUFFER_SIZE_ = 25000;
      
    /**
    * This, from what i infer is the max size of the indicators used for the
    * exception values.
    * Number of bits in an 8-bit integer value 
    */
    private static final int EXC_GROUP_ = 8;
      
    /**
    * Mask to get the group  
    */
    private static final int EXC_GROUP_MASK_ = 255;
      
    /**
    * Mask to get the digit value in the exception result
    */
    private static final int EXC_DIGIT_MASK_ = 0xFFFF;
      
    /**
    * Offset table for data in exception block.<br>
    * Table formed by the number of bits used for the index, e.g. 0 = 0 bits, 
    * 1 = 1 bits.
    */
    private static final byte FLAGS_OFFSET_[] = 
    {
        0, 1, 1, 2, 1, 2, 2, 3, 1, 2, 2, 3, 2, 3, 3, 4,
        1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3, 4, 3, 4, 4, 5,
        1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3, 4, 3, 4, 4, 5,
        2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6,
        1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3, 4, 3, 4, 4, 5,
        2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6,
        2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6,
        3, 4, 4, 5, 4, 5, 5, 6, 4, 5, 5, 6, 5, 6, 6, 7,
        1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3, 4, 3, 4, 4, 5,
        2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6,
        2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6,
        3, 4, 4, 5, 4, 5, 5, 6, 4, 5, 5, 6, 5, 6, 6, 7,
        2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6,
        3, 4, 4, 5, 4, 5, 5, 6, 4, 5, 5, 6, 5, 6, 6, 7,
        3, 4, 4, 5, 4, 5, 5, 6, 4, 5, 5, 6, 5, 6, 6, 7,
        4, 5, 5, 6, 5, 6, 6, 7, 5, 6, 6, 7, 6, 7, 7, 8
    };
      
    /**
    * Numeric value shift
    */
    private static final int VALUE_SHIFT_ = 20;
      
    /**
    * Exception test mask
    */
    private static final int EXCEPTION_MASK_ = 0x20;
      
    /**
    * Mask to be applied after shifting to obtain an unsigned numeric value
    */
    private static final int UNSIGNED_VALUE_MASK_AFTER_SHIFT_ = 0x7FF;
    
    /**
    * Shift to get bidi bits
    */
    private static final int BIDI_SHIFT_ = 6;
      
    /**
    * Mask to be applied after shifting to get bidi bits
    */
    private static final int BIDI_MASK_AFTER_SHIFT_ = 0x1F;
      
    /**
    * Mirror test mask
    */
    private static final int MIRROR_MASK_ = 1 << 11;
    
    /**
     * Shift to get numeric type
     */
    private static final int NUMERIC_TYPE_SHIFT_ = 12;
    
    /**
     * Mask to get numeric type
     */
    private static final int NUMERIC_TYPE_MASK_ = 0x7;

	/**
	 * Shift to get reserved value
	 */
	private static final int RESERVED_SHIFT_ = 15;
	
	/**
	 * Bit indicating exception
	 */
  	private static final int EXCEPTION_BIT = 1 << 5;
  	
  	/** 
  	 * Bit to get the actual property value
  	 */
    private static final int VALUE_BITS_ = 0x10000 - VALUE_SHIFT_;

	/**
	 * Minimum value of a property
	 */
    private static final int MIN_VALUE_ = -(1 << (VALUE_BITS_ - 1));
    
    /**
     * Maximum value of a property
     */
    private static final int MAX_VALUE_ = (1 << (VALUE_BITS_ - 1)) - 1;
    /**
     * Maximum number of exceptions
     */
    private static int MAX_EXCEPTIONS_COUNT_ = 1 << VALUE_BITS_;

      
    /**
    * To get the last 5 bits out from a data type
    */
    private static final int LAST_5_BIT_MASK_ = 0x1F;
      
    /**
    * Shift 5 bits
    */
    private static final int SHIFT_5_ = 5;
    /**
    * Shift 10 bits
    */
    private static final int SHIFT_10_ = 10;
      
    /**
    * Folding indicator mask
    */
    private static final int SUPPLEMENTARY_FOLD_INDICATOR_MASK_ = 0x8000;
    /**
    * Folding offset mask
    */
    private static final int SUPPLEMENTARY_FOLD_OFFSET_MASK_ = 0x7FFF;
    /**
    * Shift value for lead surrogate to form a supplementary character.
    */
	private static final int LEAD_SURROGATE_SHIFT_ = 10;
	/** 
    * Offset to add to combined surrogate pair to avoid msking.
    */
    private static final int SURROGATE_OFFSET_ = 
                           UTF16.SUPPLEMENTARY_MIN_VALUE - 
                           (UTF16.SURROGATE_MIN_VALUE << 
                           LEAD_SURROGATE_SHIFT_) - 
                           UTF16.TRAIL_SURROGATE_MIN_VALUE;   
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
   
   	// additional properties ----------------------------------------------
   	 
   	/**
   	 * Additional properties used in internal trie data
   	 */
    private static final int WHITE_SPACE_PROPERTY_ = 0;
    private static final int BIDI_CONTROL_PROPERTY_ = 1;
    private static final int JOIN_CONTROL_PROPERTY_ = 2;
    private static final int DASH_PROPERTY_ = 3;
    private static final int HYPHEN_PROPERTY_ = 4;
    private static final int QUOTATION_MARK_PROPERTY_ = 5;
    private static final int TERMINAL_PUNCTUATION_PROPERTY_ = 6;
    private static final int OTHER_MATH_PROPERTY_ = 7;
    private static final int HEX_DIGIT_PROPERTY_ = 8;
    private static final int ASCII_HEX_DIGIT_PROPERTY_ = 9;
    private static final int OTHER_ALPHABETIC_PROPERTY_ = 10;
    private static final int IDEOGRAPHIC_PROPERTY_ = 11;
    private static final int DIACRITIC_PROPERTY_ = 12;
    private static final int EXTENDER_PROPERTY_ = 13;
    private static final int OTHER_LOWERCASE_PROPERTY_ = 14;
    private static final int OTHER_UPPERCASE_PROPERTY_ = 15;
    private static final int NONCHARACTER_CODE_POINT_PROPERTY_ = 16;
    private static final int OTHER_GRAPHEME_EXTEND_PROPERTY_ = 17;
    private static final int GRAPHEME_LINK_PROPERTY_ = 18;
    private static final int IDS_BINARY_OPERATOR_PROPERTY_ = 19;
    private static final int IDS_TRINARY_OPERATOR_PROPERTY_ = 20;
    private static final int RADICAL_PROPERTY_ = 21;
    private static final int UNIFIED_IDEOGRAPH_PROPERTY_ = 22;
    private static final int OTHER_DEFAULT_IGNORABLE_CODE_POINT_PROPERTY_ = 23;
    private static final int DEPRECATED_PROPERTY_ = 24;
    private static final int SOFT_DOTTED_PROPERTY_ = 25;
    private static final int LOGICAL_ORDER_EXCEPTION_PROPERTY_ = 26;
    private static final int XID_START_PROPERTY_ = 27;
    private static final int XID_CONTINUE_PROPERTY_ = 28;
    private static final int BINARY_1_TOP_PROPERTY_ = 29;
    
    /**
     * First nibble shift
     */
    private static final int FIRST_NIBBLE_SHIFT_ = 0x4;
    /**
     * Second nibble mask
     */
    private static final int LAST_NIBBLE_MASK_ = 0xF;
    /**
     * Age value shift
     */
    private static final int AGE_SHIFT_ = 24;
    
    // private constructors --------------------------------------------------
    
    /**
    * Constructor
    * @exception thrown when data reading fails or data corrupted
    */
    private UCharacterProperty() throws IOException
    {
        // jar access
        InputStream i = getClass().getResourceAsStream(DATA_FILE_NAME_);
        BufferedInputStream b = new BufferedInputStream(i, 
                                                        DATA_BUFFER_SIZE_);
        UCharacterPropertyReader reader = new UCharacterPropertyReader(b);
        reader.read(this);
        b.close();
        i.close();
    }
                                     
	// private methods -------------------------------------------------------
	
	/**
    * <p>Sets a codepoint to the argument char array.</p>
    * <p>This method does not bother to check the validity of the codepoint</p>
    * @param target char array to be set with the new code point
    * @param char32 code point
    * @return the size of the codepoint
    */
    private static int setCodePoint(char[] target, int char32)
    {
        // Write the UTF-16 values
        if (char32 >= UTF16.SUPPLEMENTARY_MIN_VALUE) {
            target[0] = UTF16.getLeadSurrogate(char32);
            target[1] = UTF16.getTrailSurrogate(char32);
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
    private int getType(int ch)
    {
        return getPropType(getProperty(ch));
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
    private static boolean isAFTER_i(UnicodeCharacterIterator uchariter, int offset) 
    {
    	uchariter.setIndex(offset);
    	
    	int ch = uchariter.previousCodePoint();
    	
        while (ch != UnicodeCharacterIterator.DONE_CODEPOINT) {
            if (ch == LATIN_SMALL_LETTER_I_ || ch == LATIN_SMALL_LETTER_J_ || 
                ch == LATIN_SMALL_LETTER_I_WITH_OGONEK_ ||
                ch == LATIN_SMALL_LETTER_I_WITH_TILDE_BELOW_ || 
                ch == LATIN_SMALL_LETTER_I_WITH_DOT_BELOW_) {
                return true; // preceded by TYPE_i
            }
    
            int cc = NormalizerImpl.getCombiningClass(ch);
            if (cc == 0 || cc == COMBINING_MARK_ABOVE_CLASS_) {
                // preceded by different base character not TYPE_i), or 
                // intervening cc == 230
                return false; 
            }
            ch = uchariter.previousCodePoint();
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
    private static boolean isAFTER_I(UnicodeCharacterIterator uchariter, int offset) 
    {
    	uchariter.setIndex(offset);
    	
    	int ch = uchariter.previousCodePoint();
    	
        while (ch != UnicodeCharacterIterator.DONE_CODEPOINT) {
            if (ch == LATIN_CAPITAL_LETTER_I_) {
                return true; // preceded by I
            }

            int cc = NormalizerImpl.getCombiningClass(ch);
            if (cc == 0 || cc == COMBINING_MARK_ABOVE_CLASS_) {
                // preceded by different base character (not I), or 
                // intervening cc == 230
                return false; 
            }
 			ch = uchariter.previousCodePoint();           
        }

        return false; // not preceded by I
    }
    
    /** 
    * Determines if codepoint at offset is not followed by a sequence 
    * consisting of an ignorable sequence and then a cased letter 
    * {Ll, Lu, Lt}.
    * @param uchariter String iterator to determine
    * @param offset codepoint offset in string to check
    * @return false if any character after offset in src is a cased letter
    * @see SpecialCasing.txt
    */
    private boolean isCFINAL(UnicodeCharacterIterator uchariter, int offset) 
    {
    	// iterator should have been determined to be not null by caller
        uchariter.setIndex(offset);
    	uchariter.nextCodePoint(); // rid of current codepoint
        int ch = uchariter.nextCodePoint(); // start checking
    	
    	while (ch != UnicodeCharacterIterator.DONE_CODEPOINT) {
            int cat = getType(ch);
            if (cat == UCharacterCategory.LOWERCASE_LETTER || 
                cat == UCharacterCategory.UPPERCASE_LETTER ||
                cat == UCharacterCategory.TITLECASE_LETTER) {
                return false; // followed by cased letter
            }
            if (!isIgnorable(ch, cat)) {
                return true; // not ignorable
            }
            ch = uchariter.nextCodePoint();
        }

        return true;
    }

    /**
    * Determines if codepoint at offset is not preceded by a sequence 
    * consisting of a cased letter {Ll, Lu, Lt} and an ignorable sequence. 
    * @param uchariter string iterator to determine
    * @param offset codepoint offset in string to check
    * @return true if any character before index in src is a cased letter
    * @see SpecialCasing.txt
    */
    private boolean isNotCINITIAL(UnicodeCharacterIterator uchariter, 
                                         int offset) 
    {
    	uchariter.setIndex(offset);
    	int ch = uchariter.previousCodePoint();
    	
        while (ch != UnicodeCharacterIterator.DONE_CODEPOINT) {
            int cat = getType(ch);
            if (cat == UCharacterCategory.LOWERCASE_LETTER || 
                cat == UCharacterCategory.UPPERCASE_LETTER ||
                cat == UCharacterCategory.TITLECASE_LETTER) {
                return true; // preceded by cased letter
            }
            if (!isIgnorable(ch, cat)) {
                return false; // not ignorable
            }
			ch = uchariter.previousCodePoint();
        }

        return false; 
    }

    /** 
    * Determines if a codepoint at offset in string is followed by one or 
    * more characters of combining class = 230.
    * @param uchariter text iterator to be determined
    * @param offset codepoint offset in string to check
    * @return true if a string at offset is followed by one or more characters 
    *         of combining class = 230.
    * @see SpecialCasing.txt
    */
    private static boolean isFollowedByMOREABOVE(UnicodeCharacterIterator uchariter, 
                                                 int offset) 
    {
        uchariter.setIndex(offset);
        uchariter.nextCodePoint(); // rid of current codepoint
        int ch = uchariter.nextCodePoint(); // start checking
        
        while (ch != UnicodeCharacterIterator.DONE_CODEPOINT) {
            int cc = NormalizerImpl.getCombiningClass(ch);
            if (cc == COMBINING_MARK_ABOVE_CLASS_) {
                return true; // at least one cc==230 following 
            }
            if (cc == 0) {
                return false; // next base character, no more cc==230 following
            }
            ch = uchariter.nextCodePoint();
        }

        return false; // no more cc == 230 following
    }

    /** 
    * Determines if a codepoint at offset in string is followed by a dot 
    * above with no characters of combining class == 230 in between 
    * @param uchariter text iterator to be determined
    * @param offset codepoint offset of the character in string to check
    * @return true if a string at offset is followed by oa dot above 
    *         with no characters of combining class == 230 in between
    * @see SpecialCasing.txt
    */
    private static boolean isFollowedByDotAbove(UnicodeCharacterIterator uchariter, 
                                                int offset) 
    {
        uchariter.setIndex(offset);
        uchariter.nextCodePoint(); // rid off current character
        int ch = uchariter.nextCodePoint(); // start checking
        
        while (ch != UnicodeCharacterIterator.DONE_CODEPOINT) {
            if (ch == COMBINING_DOT_ABOVE_) {
                return true;
            }
            int cc = NormalizerImpl.getCombiningClass(ch);
            if (cc == 0 || cc == COMBINING_MARK_ABOVE_CLASS_) {
                return false; // next base character or cc==230 in between
            }
            ch = uchariter.nextCodePoint();
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
      
    /**
    * Getting the correct address for data in the exception value
    * @param evalue exception value
    * @param indicator type of data to retrieve
    * @param address current address to move from
    * @return the correct address
    */
    private int addExceptionOffset(int evalue, int indicator, int address) 
    { 
        int result = address;
        if (indicator >= EXC_GROUP_) {
        result += FLAGS_OFFSET_[evalue & EXC_GROUP_MASK_]; 
        evalue >>= EXC_GROUP_; 
        indicator -= EXC_GROUP_; 
        }
        int mask = (1 << indicator) - 1;
        result += FLAGS_OFFSET_[evalue & mask]; 
        return result;
    }
    
    /**
     * Compare additional properties to see if it has argument type 
     * @param property 32 bit properties
     * @param type character type
     * @return true if property has type
     */
    private boolean compareAdditionalType(int property, int type) 
    {
    	return (property & (1 << type)) != 0;
    }
}
