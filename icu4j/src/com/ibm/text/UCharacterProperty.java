/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: 
*         /usr/cvs/icu4j/icu4j/src/com/ibm/icu/text/UCharacterPropertyDB.java $ 
* $Date: 2002/02/08 01:12:10 $ 
* $Revision: 1.1 $
*
*******************************************************************************
*/

package com.ibm.text;

import java.io.InputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import com.ibm.icu.internal.util.Trie;
import com.ibm.icu.util.CharTrie;

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

final class UCharacterProperty implements Trie.DataManipulate
{
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
    * Data type indicators
    */
    protected static final int EXC_UPPERCASE_ = 0;
    protected static final int EXC_LOWERCASE_ = EXC_UPPERCASE_ + 1;
    protected static final int EXC_TITLECASE_ = EXC_LOWERCASE_ + 1;
    protected static final int EXC_DIGIT_VALUE_ = EXC_TITLECASE_ + 1;
    protected static final int EXC_NUMERIC_VALUE_ = EXC_DIGIT_VALUE_ + 1;
    protected static final int EXC_DENOMINATOR_VALUE_ = EXC_NUMERIC_VALUE_ 
                                                        + 1;
    protected static final int EXC_MIRROR_MAPPING_ = EXC_DENOMINATOR_VALUE_
                                                     + 1;
    protected static final int EXC_SPECIAL_CASING_ = EXC_MIRROR_MAPPING_ 
                                                     + 1;
    protected static final int EXC_CASE_FOLDING_ = EXC_SPECIAL_CASING_ + 1;
    // EXC_COMBINING_CLASS_ is not found in ICU
    // Used to retrieve the combining class of the character in the exception
    // value
    protected static final int EXC_COMBINING_CLASS_ = EXC_CASE_FOLDING_ + 1;
      
    /**
    * Trie data
    */
    protected CharTrie m_trie_;
      
    /**
    * Character property table
    */
    protected int m_property_[];
      
    /**
    * Case table
    */
    protected char m_case_[];
      
    /**
    * Exception property table
    */
    protected int m_exception_[];
    /**
    * Unicode version
    */
    protected String m_unicodeVersion_;
    
    // protected methods ================================================
      
    /**
    * Gets the property value at the index
    * @param ch code point whose property value is to be retrieved
    * @return property value of code point
    */
    protected int getProperty(int ch)
    {
        return m_property_[m_trie_.getCodePointValue(ch)];
    }
      
    /**
    * Gets the upper case value at the index
    * @param index of the case value to be retrieved
    * @param buffer string buffer to add result to
    */
    protected void getUpperCase(int index, StringBuffer buffer)
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
    * Gets the lower case value at the index
    * @param index of the case value to be retrieved
    * @param buffer string buffer to add result to
    */
    protected void getLowerCase(int index, StringBuffer buffer)
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
    * Gets the folded case value at the index
    * @param index of the case value to be retrieved
    * @return folded case value at index
    */
    protected int getFoldCase(int index)
    {
        char single = m_case_[index];
        if (UTF16.isSurrogate(single)) {
        // Convert the UTF-16 surrogate pair if necessary.
        // For simplicity in usage, and because the frequency of pairs is low,
        // look both directions.
                  
	        if (UTF16.isLeadSurrogate(single)) 
	        {
	            char trail = m_case_[index + 1];
	            if (UTF16.isTrailSurrogate(trail)) {
	                return UCharacter.getRawSupplementary(single, trail);
	            }
	        } 
	        else 
	        { 
	            char lead = m_case_[index - 1];
	            if (UTF16.isLeadSurrogate(lead)) {
	                return UCharacter.getRawSupplementary(lead, single);
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
    protected void getFoldCase(int index, int count, StringBuffer str) 
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
    * Determines if the exception value passed in has the kind of information
    * which the indicator wants, e.g if the exception value contains the digit
    * value of the character
    * @param index exception index
    * @param indicator type indicator
    * @return true if type value exist
    */
    protected boolean hasExceptionValue(int index, int indicator) 
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
    protected int getException(int index, int etype)
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
    * Returns a value indicating a character category from the argument property
    * value
    * @param unicode character property
    * @return category
    */
    protected static int getPropType(int prop)
    {
        // Since character information data are packed together.
	    // This is the category mask for getting the category information
        int result = prop & LAST_5_BIT_MASK_;
        return result;
    }
      
    /**
    * Determines if the argument props indicates that the exception block has 
    * to be accessed for data
    * @param props property value
    * @return true if this is an exception indicator false otherwise
    */
    protected static boolean isExceptionIndicator(int props)
    {
        if ((props & EXCEPTION_MASK_) != 0) {
        return true;
        }
        return false;
    }
      
    /**
    * Getting the exception index for argument property
    * @param prop character property 
    */
    protected static int getExceptionIndex(int prop)
    {
        return (prop >> VALUE_SHIFT_) & UNSIGNED_VALUE_MASK_AFTER_SHIFT_;
    }
      
    /**
    * Getting the signed numeric value of a character embedded in the property
    * argument
    * @param prop the character
    * @return signed numberic value
    */
    protected static int getSignedValue(int prop)
    {
        return (prop >> VALUE_SHIFT_);
    }
      
    /**
    * Getting the unsigned numeric value of a character embedded in the property
    * argument
    * @param prop the character
    * @return unsigned numberic value
    */
    protected static int getUnsignedValue(int prop)
    {
        return (prop >> VALUE_SHIFT_) & UNSIGNED_VALUE_MASK_AFTER_SHIFT_;
    }
      
    /**
    * Checking if property indicates mirror element
    * @param prop property value
    * @return true if mirror indicator is set, false otherwise
    */
    protected static boolean isMirrored(int prop)
    {
        return (prop & MIRROR_MASK_) != 0;
    }
      
    /**
    * Getting the direction data in the property value
    * @param prop property value
    * @return direction value in property
    */
    protected static int getDirection(int prop)
    {
        return (prop >> BIDI_SHIFT_) & BIDI_MASK_AFTER_SHIFT_;
    }
    
    // private variables ==================================================
      
    /**
    * Default name of the datafile
    */
    private static final String DATA_FILE_NAME_ = "resources/uprops.dat";
      
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
    * Mirror test mask
    */
    private static final int MIRROR_MASK_ = 0x800;
      
    /**
    * Shift to get bidi bits
    */
    private static final int BIDI_SHIFT_ = 6;
      
    /**
    * Mask to be applied after shifting to get bidi bits
    */
    private static final int BIDI_MASK_AFTER_SHIFT_ = 0x1F;
      
    /**
    * To get the last 5 bits out from a data type
    */
    private static final int LAST_5_BIT_MASK_ = 0x1F;
      
    /**
    * Shift 5 bits
    */
    private static final int SHIFT_5_ = 5;
      
    /**
    * Folding indicator mask
    */
    private static final int SUPPLEMENTARY_FOLD_INDICATOR_MASK_ = 0x8000;
    /**
    * Folding offset mask
    */
    private static final int SUPPLEMENTARY_FOLD_OFFSET_MASK_ = 0x7FFF;
      
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
}
