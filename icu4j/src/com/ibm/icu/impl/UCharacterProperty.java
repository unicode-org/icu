/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: 
*         /usr/cvs/icu4j/icu4j/src/com/ibm/icu/text/UCharacterPropertyDB.java $ 
* $Date: 2002/03/13 18:15:44 $ 
* $Revision: 1.4 $
*
*******************************************************************************
*/

package com.ibm.icu.impl;

import java.io.InputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import com.ibm.icu.util.VersionInfo;
import com.ibm.icu.lang.UCharacterCategory;
import com.ibm.icu.lang.UProperty;

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
     * Numeric type for general numbers
     */
    public static final int GENERAL_NUMERIC_TYPE_ = 1;

    
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
        int result = prop & LAST_5_BIT_MASK_;
        return result;
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
        if (UnicodeProperty.LEAD_SURROGATE_MIN_VALUE <= single && 
            single <= UnicodeProperty.TRAIL_SURROGATE_MAX_VALUE) {
        // Convert the UTF-16 surrogate pair if necessary.
        // For simplicity in usage, and because the frequency of pairs is low,
        // look both directions.
                  
	        if (single <= UnicodeProperty.LEAD_SURROGATE_MAX_VALUE) {
	            char trail = m_case_[index + 1];
	            if (UnicodeProperty.LEAD_SURROGATE_MIN_VALUE <= trail && 
		            trail <= UnicodeProperty.TRAIL_SURROGATE_MAX_VALUE) {
	                return UnicodeProperty.getRawSupplementary(single, trail);
	            }
	        } 
	        else 
	        { 
	            char lead = m_case_[index - 1];
	            if (UnicodeProperty.LEAD_SURROGATE_MIN_VALUE <= lead && 
		            lead <= UnicodeProperty.LEAD_SURROGATE_MAX_VALUE) {
	                return UnicodeProperty.getRawSupplementary(lead, single);
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
                                             OTHER_ALPHABETIC_);
    		}
    		case UProperty.ASCII_HEX_DIGIT: {
        		return compareAdditionalType(getAdditional(codepoint, 1),
											 ASCII_HEX_DIGIT_);
    		}
    		case UProperty.BIDI_CONTROL: {
        		return compareAdditionalType(getAdditional(codepoint, 1),
        		                             BIDI_CONTROL_);
    		}
    		case UProperty.BIDI_MIRRORED: {
        		return isMirrored(getProperty(codepoint));
    		}
    		case UProperty.DASH: {
        		return compareAdditionalType(getAdditional(codepoint, 1),
        		                             DASH_);
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
       		                             OTHER_DEFAULT_IGNORABLE_CODE_POINT_)
		               || compareAdditionalType(additionalproperty, 
		                                        WHITE_SPACE_) ||
		               compareAdditionalType(additionalproperty, 
	                         			OTHER_DEFAULT_IGNORABLE_CODE_POINT_);
    		}
    		case UProperty.DEPRECATED: {
        		return compareAdditionalType(getAdditional(codepoint, 1),
        		                             DEPRECATED_);
    		}
    		case UProperty.DIACRITIC: {
        		return compareAdditionalType(getAdditional(codepoint, 1),
        		                             DIACRITIC_);
    		}
    		case UProperty.EXTENDER: {
        		return compareAdditionalType(getAdditional(codepoint, 1),
        		                         EXTENDER_);
    		}
    		case UProperty.FULL_COMPOSITION_EXCLUSION: {
        		// return unorm_internalIsFullCompositionExclusion(codepoint);
        		return true;
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
                                             GRAPHEME_LINK_) ||
                       compareAdditionalType(additionalproperty,
                                             OTHER_GRAPHEME_EXTEND_);
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
                                             OTHER_GRAPHEME_EXTEND_) ||
                       compareAdditionalType(additionalproperty, 
                                             GRAPHEME_LINK_) || 
					   compareAdditionalType(additionalproperty,
					                         OTHER_GRAPHEME_EXTEND_);
    		}
    		case UProperty.GRAPHEME_LINK: {
        		return compareAdditionalType(getAdditional(codepoint, 1),
        		                             GRAPHEME_LINK_);
    		}
    		case UProperty.HEX_DIGIT: {
        		return compareAdditionalType(getAdditional(codepoint, 1),
        		                             HEX_DIGIT_);
    		}
    		case UProperty.HYPHEN: {
        		return compareAdditionalType(getAdditional(codepoint, 1),
        		                             HYPHEN_);
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
        		                             IDEOGRAPHIC_);
    		}
    		case UProperty.IDS_BINARY_OPERATOR: {
        		return compareAdditionalType(getAdditional(codepoint, 1),
        		                             IDS_BINARY_OPERATOR_);
    		}
    		case UProperty.IDS_TRINARY_OPERATOR: {
        		return compareAdditionalType(getAdditional(codepoint, 1),
        		                             IDS_TRINARY_OPERATOR_);
			}
    		case UProperty.JOIN_CONTROL: {
        		return compareAdditionalType(getAdditional(codepoint, 1),
        		                             JOIN_CONTROL_);
    		}
    		case UProperty.LOGICAL_ORDER_EXCEPTION: {
        		return compareAdditionalType(getAdditional(codepoint, 1),
        		                             LOGICAL_ORDER_EXCEPTION_);
    		}
    		case UProperty.LOWERCASE: {
        		// Ll+Other_Lowercase
        		int generaltype = getPropType(getProperty(codepoint));
        		if (generaltype == UCharacterCategory.LOWERCASE_LETTER) {
        			return true;
        		}
        		return compareAdditionalType(getAdditional(codepoint, 1),
        		                             OTHER_LOWERCASE_);
    		}
    		case UProperty.MATH: {
        		// Sm+Other_Math 
        		int generaltype = getPropType(getProperty(codepoint));
        		if (generaltype == UCharacterCategory.MATH_SYMBOL) {
        			return true;
        		}
        		return compareAdditionalType(getAdditional(codepoint, 1),
        		                             OTHER_MATH_);
    		}
    		case UProperty.NONCHARACTER_CODE_POINT: {
        		return compareAdditionalType(getAdditional(codepoint, 1),
        		                             NONCHARACTER_CODE_POINT_);
    		}
    		case UProperty.QUOTATION_MARK: {
        		return compareAdditionalType(getAdditional(codepoint, 1),
        		                             QUOTATION_MARK_);
    		}
    		case UProperty.RADICAL: {
        		return compareAdditionalType(getAdditional(codepoint, 1),
        		                             RADICAL_);
    		}
    		case UProperty.SOFT_DOTTED: {
        		return compareAdditionalType(getAdditional(codepoint, 1),
        		                             SOFT_DOTTED_);
    		}
    		case UProperty.TERMINAL_PUNCTUATION: {
        		return compareAdditionalType(getAdditional(codepoint, 1),
        		                             TERMINAL_PUNCTUATION_);
    		}
		    case UProperty.UNIFIED_IDEOGRAPH: {
        		return compareAdditionalType(getAdditional(codepoint, 1),
        		                             UNIFIED_IDEOGRAPH_);
		    }
    		case UProperty.UPPERCASE: {
        		// Lu+Other_Uppercase 
        		int generaltype = getPropType(getProperty(codepoint));
        		if (generaltype == UCharacterCategory.UPPERCASE_LETTER) {
        			return true;
        		}
        		return compareAdditionalType(getAdditional(codepoint, 1),
                     	                     OTHER_UPPERCASE_);
    		}
    		case UProperty.WHITE_SPACE: {
        		return compareAdditionalType(getAdditional(codepoint, 1),
        		                             WHITE_SPACE_);
    		}
    		case UProperty.XID_CONTINUE: {
        		return compareAdditionalType(getAdditional(codepoint, 1),
        		                             XID_CONTINUE_);
    		}
    		case UProperty.XID_START: {
        		return compareAdditionalType(getAdditional(codepoint, 1),
        		                             XID_START_);
    		}
    		default:
        		// not a known binary property
        		return false;
    	}
	}
    
    // protected constructor ---------------------------------------------
  
    /**
    * Constructor
    * @exception thrown when data reading fails or data corrupted
    */
    protected UCharacterProperty() throws IOException
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
   
   	// additional properties ----------------------------------------------
   	 
   	/**
   	 * Additional properties used in internal trie data
   	 */
    private static final int WHITE_SPACE_ = 0;
    private static final int BIDI_CONTROL_ = 1;
    private static final int JOIN_CONTROL_ = 2;
    private static final int DASH_ = 3;
    private static final int HYPHEN_ = 4;
    private static final int QUOTATION_MARK_ = 5;
    private static final int TERMINAL_PUNCTUATION_ = 6;
    private static final int OTHER_MATH_ = 7;
    private static final int HEX_DIGIT_ = 8;
    private static final int ASCII_HEX_DIGIT_ = 9;
    private static final int OTHER_ALPHABETIC_ = 10;
    private static final int IDEOGRAPHIC_ = 11;
    private static final int DIACRITIC_ = 12;
    private static final int EXTENDER_ = 13;
    private static final int OTHER_LOWERCASE_ = 14;
    private static final int OTHER_UPPERCASE_ = 15;
    private static final int NONCHARACTER_CODE_POINT_ = 16;
    private static final int OTHER_GRAPHEME_EXTEND_ = 17;
    private static final int GRAPHEME_LINK_ = 18;
    private static final int IDS_BINARY_OPERATOR_ = 19;
    private static final int IDS_TRINARY_OPERATOR_ = 20;
    private static final int RADICAL_ = 21;
    private static final int UNIFIED_IDEOGRAPH_ = 22;
    private static final int OTHER_DEFAULT_IGNORABLE_CODE_POINT_ = 23;
    private static final int DEPRECATED_ = 24;
    private static final int SOFT_DOTTED_ = 25;
    private static final int LOGICAL_ORDER_EXCEPTION_ = 26;
    private static final int XID_START_ = 27;
    private static final int XID_CONTINUE_ = 28;
    private static final int BINARY_1_TOP_ = 29;
    
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
      
    // private methods ---------------------------------------------------
      
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
