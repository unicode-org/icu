/**
*******************************************************************************
* Copyright (C) 1996-2000, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: 
*         /usr/cvs/icu4j/icu4j/src/com/ibm/icu/text/UCharacterPropertyDB.java $ 
* $Date: 2001/02/26 23:45:37 $ 
* $Revision: 1.2 $
*
*******************************************************************************
*/

package com.ibm.icu.text;

import java.io.InputStream;
import java.io.DataInputStream;
import java.io.BufferedInputStream;

/**
* Internal class used for Unicode character property database.
* Database classes store binary data read from uprops.dat and unames for use. 
* It does not have the capability to parse the data into more high-level 
* information. It only returns bytes of information when required. 
* Due to the form most commonly used for retrieval, array of char is used
* to store the binary data
* UCharacterPropertyDB also contains information on accessing indexes to 
* significant points in the binary data.
* Responsibility for molding the binary data into more meaning form lies on 
* <a href=UCharacter.html>UCharacter</a> and 
* <a href=UCharacterName.html>UCharacterName</a>.
* Data populated by <a href=UGenPropReader.html>UGenPropReader</a>
* @author Syn Wee Quek
* @since oct1000
* @see com.ibm.icu.text.UGenReader
*/

final class  UCharacterPropertyDB extends UCharacterDB
{
  // protected variables ================================================
  
  /**
  * Data type indicators
  */
  protected static final int EXC_UPPERCASE_ = 0;
  protected static final int EXC_LOWERCASE_ = EXC_UPPERCASE_ + 1;
  protected static final int EXC_TITLECASE_ = EXC_LOWERCASE_ + 1;
  protected static final int EXC_DIGIT_VALUE_ = EXC_TITLECASE_ + 1;
  protected static final int EXC_NUMERIC_VALUE_ = EXC_DIGIT_VALUE_ + 1;
  protected static final int EXC_DENOMINATOR_VALUE_ = EXC_NUMERIC_VALUE_ + 1;
  protected static final int EXC_MIRROR_MAPPING_ = EXC_DENOMINATOR_VALUE_ + 1;
  protected static final int EXC_SPECIAL_CASING_ = EXC_MIRROR_MAPPING_ + 1;
  // EXC_COMBINING_CLASS_ is not found in ICU
  protected static final int EXC_COMBINING_CLASS_ = EXC_SPECIAL_CASING_ + 1;


  // private variables ==================================================
  
  /**
  * Number of bits to shift right to get the correct segment of bits out for 
  * index to the unicode database
  */
  private int m_stage1shift_;
  private int m_stage2shift_;

  /**
  * Mask for performing on the bit segment after shifting to get an index out 
  * of it
  */
  private int m_stage2maskaftershift_;
  private int m_stage3maskaftershift_;

  /**
  * Table for stages data block
  */
  private char m_stages_[];
  
  /**
  * Character property table
  */
  private int m_property_[];
  
  /**
  * Case table
  */
  private char m_case_[];
  
  /**
  * Exception property table
  */
  private int m_exception_[];
  
  /**
  * Default name of the datafile
  */
  private static final String DATA_FILE_NAME_ = "uprops.dat";
  
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
  
  // constructor ======================================================
  
  /**
  * Constructor
  * @exception thrown when data reading fails or data corrupted
  */
  protected UCharacterPropertyDB() throws Exception
  {
    UGenPropReader reader = new UGenPropReader();
    
    InputStream i = getClass().getResourceAsStream(DATA_FILE_NAME_);
    BufferedInputStream b = new BufferedInputStream(i, DATA_BUFFER_SIZE_);
    DataInputStream d = new DataInputStream(b);
    if (!reader.read(d, this))
      throw new Exception("Data corrupted in " + DATA_FILE_NAME_);
    d.close(); 
  }
  
  // public methods ===================================================
  
  /**
   * toString method for printing
   */
  public String toString()
  {
    StringBuffer result = new StringBuffer("Property block\n");
    result.append(super.toString());
    result.append("\nshift 1 : ");
    result.append(m_stage1shift_);
    result.append("\nshift 2 : ");
    result.append(m_stage2shift_);
    result.append("\nmask 2 : ");
    result.append(m_stage2maskaftershift_);
    result.append("\nmask 3 : ");
    result.append(m_stage3maskaftershift_);
    result.append("\nsize of stage data ");
    result.append(m_stages_.length);
    result.append("\nsize of property data ");
    result.append(m_property_.length);
    result.append("\nsize of exception data ");
    result.append(m_exception_.length);
    return result.toString();
  }
  
  // protected methods ================================================
  
  /**
  * Set stage shift bits, mask and property offset
  * @param stage1shift count
  * @param stage2shift count
  * @param stage2mask count
  * @param stage3mask count
  * @param offset property block offset
  * @return false if there is a data error
  */
  protected boolean setInfo(int stage1shift, int stage2shift, int stage2mask,
                            int stage3mask)
  {
    if (stage1shift >= 0 && stage2shift >= 0 && stage2mask != 0 && 
        stage3mask != 0)
    {
      m_stage1shift_ = stage1shift; 
      m_stage2shift_ = stage2shift;
      m_stage2maskaftershift_ = stage2mask;
      m_stage3maskaftershift_ = stage3mask;
      return true;
    }
    return false;
  }
  
  /**
  * Set the stages block data. The first UGenPropReader.INDEX_SIZE char of data 
  * being some other data not used from hence onwards. Note the unused data
  * resides since all indexes are relative to it.
  * @param stages array containing the 2 stages of index pointing to property
  *        data
  * @return true if stages data is set successfully
  */
  protected boolean setStage(char stages[])
  {
    if (stages == null || stages.length <= 0)
      return false;
    m_stages_ = stages;
    return true;
  }
  
  /**
  * Set the property block data. 
  * @param property array containing data regarding the character properties
  * @return true if stages data is set successfully
  */
  protected boolean setProperty(int property[])
  {
    if (property == null || property.length <= 0)
      return false;
    m_property_ = property;
    return true;
  }
  
  /**
  * Set the case block data. 
  * @param case array containing data regarding the case properties
  * @return true if stages data is set successfully
  */
  protected boolean setCase(char casetable[])
  {
    if (casetable == null || casetable.length <= 0)
      return false;
    m_case_ = casetable;
    return true;
  }
  
  /**
  * Set the exception block data. 
  * @param exception array containing extra character properties not found in
  *        property array
  * @return true if stages data is set successfully
  */
  protected boolean setException(int exception[])
  {
    if (exception == null || exception.length <= 0)
      return false;
    m_exception_ = exception;
    return true;
  }
  
  /**
  * Gets the property value at the index
  * @param ch code point whose property value is to be retrieved
  * @return property value of code point
  */
  protected int getProperty(int ch)
  {
    // index of the first access to the database 
    int index1 = ch >> m_stage1shift_;
    // index of the second access to the database
    int index2 = m_stages_[index1] + 
                 ((ch >> m_stage2shift_) & m_stage2maskaftershift_);
    // index of the third access to the database
    int index3 = m_stages_[index2] + (ch & m_stage3maskaftershift_);
    int propindex = m_stages_[index3];
    return m_property_[propindex];
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
            
    for (int j = 0; j < count; j ++)
      buffer.append(m_case_[index + j]);
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
    for (int j = 0; j < count; j ++)
      buffer.append(m_case_[index + j]);
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
    if ((props & EXCEPTION_MASK_) != 0)
      return true;
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
  
  // private methods ===============================================
  
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
    if (indicator >= EXC_GROUP_) 
      result += (FLAGS_OFFSET_[evalue & EXC_GROUP_MASK_] << 1); 
      // evalue >>= EXC_GROUP_; 
      // indicator -= EXC_GROUP_; 
    else 
    {
      int mask = (1 << indicator) - 1;
      result += FLAGS_OFFSET_[evalue & mask]; 
    }
    return result;
  }
}