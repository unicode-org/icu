/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/text/Attic/UCharacterNameDB.java,v $ 
* $Date: 2001/08/22 22:38:30 $ 
* $Revision: 1.5 $
*
*******************************************************************************
*/

package com.ibm.text;

import java.io.InputStream;
import java.io.DataInputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import com.ibm.util.Utility;

/**
* Internal class used for Unicode character name database.
* Database classes store binary data read from uprops.dat and unames for use. 
* It does not have the capability to parse the data into more high-level 
* information. It only returns bytes of information when required. 
* Due to the form most commonly used for retrieval, array of char is used
* to store the binary data
* UCharacterNameDB also contains indexes to significant points in the binary
* data.
* Responsibility for molding the binary data into more meaning form lies on 
* <a href=UCharacterPpty.html>UCharacterPpty</a> and 
* <a href=UCharacterName.html>UCharacterName</a>.
* Data populated by <a href=UGenNameReader.html>UGenNameReader</a>
* @author Syn Wee Quek
* @since oct2700
* @see com.ibm.text.UGenReader
*/

final class UCharacterNameDB extends UCharacterDB
{
  // private variable =============================================
  
  /**
  * Data used in unames.dat
  */
  private char m_tokentable_[];
  private byte m_tokenstring_[];
  private char m_groupinfo_[];
  private byte m_groupstring_[];
  private AlgorithmName m_algorithm_[];
  
  /**
  * Number of group sets
  */
  private int m_groupcount_ = 0;
  private int m_groupsize_ = 0;
  
  /**
  * Default name of the name datafile
  */
  private static final String NAME_FILE_NAME_ = "unames.dat";
  
  /**
  * Default buffer size of datafile
  */
  private static final int NAME_BUFFER_SIZE_ = 100000;
  
  /**
  * Shift count to retrieve group information
  */
  private static final int GROUP_SHIFT_ = 5;
  
  /**
  * Number of lines per group
  */
  private static final int LINES_PER_GROUP_ = 1 << GROUP_SHIFT_;
  
  /**
  * Mask to retrieve the offset for a particular character within a group
  */
  private static final int GROUP_MASK_ = LINES_PER_GROUP_ - 1;
  
  /**
  * Position of offsethigh in group information array
  */
  private static final int OFFSET_HIGH_OFFSET_ = 1;
  
  /**
  * Position of offsetlow in group information array
  */
  private static final int OFFSET_LOW_OFFSET_ = 2;
  
  /**
  * Indicator of if Unicode 1.0 names are available
  */
  private static boolean UNICODE_1_;
  
  /**
  * Double nibble indicator, any nibble > this number has to be combined
  * with its following nibble
  */
  private static final int SINGLE_NIBBLE_MAX_ = 11;
  
  // constructor ====================================================
  
  /**
  * protected constructor
  * @exception thrown when data reading fails or when data has been corrupted
  */
  protected UCharacterNameDB() throws IOException
  {
    UGenNameReader reader = new UGenNameReader();
    InputStream i = getClass().getResourceAsStream(NAME_FILE_NAME_);
    BufferedInputStream b = new BufferedInputStream(i, NAME_BUFFER_SIZE_);
    DataInputStream d = new DataInputStream(b);
    reader.read(d, this);
    d.close();
    UNICODE_1_  = (';' >= m_tokentable_.length) || 
                  (m_tokentable_[(int)';'] == 0xFFFF);
  }
  
  // public method ==================================================
  
  /**
  * toString method for printing
  */
  public String toString()
  {
    StringBuffer result = new StringBuffer("names content \n");
    /*result.append(super.toString());
    result.append('\n');
    result.append("token string offset ");
    result.append(m_tokenstringoffset_);
    result.append("\n");
    result.append("group offset ");
    result.append(m_groupsoffset_);
    result.append("\n");
    result.append("group string offset ");
    result.append(m_groupstringoffset_);
    result.append("\n");
    result.append("alg names offset ");
    result.append(m_algnamesoffset_);
    result.append("\n");
    */
    return result.toString();
  }

  // protected methods ===============================================
  
  /**
  * Sets the token data
  * @param token array of tokens
  * @param tokenstring array of string values of the tokens
  * @return false if there is a data error
  */
  protected boolean setToken(char token[], byte tokenstring[])
  {
    if (token != null && tokenstring != null && token.length > 0 &&
        tokenstring.length > 0) {
      m_tokentable_ = token;
      m_tokenstring_ = tokenstring;
      return true;
    }
    return false; 
  }
   
  /**
  * Sets the number of group and size of each group in number of char
  * @param count number of groups
  * @param size size of group in char
  * @return true if group size is set correctly
  */
  protected boolean setGroupCountSize(int count, int size)
  {
    if (count <= 0 || size <= 0) {
      return false;
    }
    m_groupcount_ = count;
    m_groupsize_ = size;
    return true;
  }
  
  /**
  * Sets the group name data
  * @param group index information array
  * @param groupstring name information array
  * @return false if there is a data error
  */
  protected boolean setGroup(char group[], byte groupstring[])
  {
    if (group != null && groupstring != null && group.length > 0 &&
        groupstring.length > 0) {
      m_groupinfo_ = group;
      m_groupstring_ = groupstring;
      return true;
    }
    return false; 
  }
  
  /**
  * Binary search for the group strings set that contains the argument Unicode 
  * code point's most significant bits.
  * The return value is always a valid group string set that contain msb.
  * If group string set is not found, -1 is returned
  * @param ch the code point to look for
  * @return group string set index in datatable otherwise -1 is returned if 
  *         group string set is not found
  */
  protected int getGroupStringIndex(int ch)
  {
    // gets the msb
    int msb = ch >> GROUP_SHIFT_,
        end = m_groupcount_,
        start,
        gindex = 0;
        
    // binary search for the group of names that contains the one for code
    for (start = 0; start < end - 1;) {
      gindex = (start + end) >> 1;
      if (msb < getGroupMSB(gindex)) {
        end = gindex;
      }
      else {
        start = gindex;
      }
    }

    // return this if it is an exact match
    if (msb == getGroupMSB(start)) {
      start = start * m_groupsize_;
      return UCharacterUtil.toInt(m_groupinfo_[start + OFFSET_HIGH_OFFSET_], 
                                  m_groupinfo_[start + OFFSET_LOW_OFFSET_]);
    }
    return -1;
  }
  
  /**
  * Returns the number of the group information object
  * @return number of group information object
  */
  protected int countGroup()
  {
    return m_groupcount_;
  }
  
  /**
  * Gets the group name of the character
  * @param ch character to get the group name 
  * @param choice name choice selector to choose a unicode 1.0 or newer name
  */
  protected String getGroupName(int ch, int choice) 
  {
    if (choice != UCharacterNameChoice.U_UNICODE_CHAR_NAME && !UNICODE_1_) {
      // if not modern name requested and semicolon byte value is a character, 
      // not a token number, otherwise since only modern names are stored in 
      // unames.dat and there is no such requested Unicode 1.0 name here
      return null;
    }
        
    // gets the msb
    int msb = ch >> GROUP_SHIFT_,
        end = m_groupcount_,
        start,
        gindex = 0;
    
    // binary search for the group of names that contains the one for code
    for (start = 0; start < end - 1;) {
      gindex = (start + end) >> 1;
      if (msb < getGroupMSB(gindex)) {
        end = gindex;
      }
      else {
        start = gindex;
      }
    }

    // return this if it is an exact match
    if (msb == getGroupMSB(start)) {
      char offsets[] = new char[LINES_PER_GROUP_ + 1];
      char lengths[] = new char[LINES_PER_GROUP_ + 1];
                
      int index = getGroupLengths(start, offsets, lengths);
      int offset = ch & GROUP_MASK_;
      return getGroupName(index + offsets[offset], lengths[offset], choice);
    }
    
    return null;
  }
  
  /**
  * Getting the character with the tokenized argument name
  * @param index of the group to check
  * @param name of the character
  * @param choice of Unicode version used
  * @return character with the tokenized argument name or -1 if character is
  *         not found
  */
  protected int getGroupChar(int index, String name, int choice) 
  {
    if (choice != UCharacterNameChoice.U_UNICODE_CHAR_NAME && 
        !UNICODE_1_) {
      // semicolon byte value is a token number , therefore only modern 
      // names are stored in unames.dat and there is no such requested 
      // Unicode 1.0 name here
      return -1;
    }
            
    // populating the data set of grouptable
    char offsets[] = new char[LINES_PER_GROUP_ + 1];
    char lengths[] = new char[LINES_PER_GROUP_ + 1];
    int startgpstrindex = getGroupLengths(index, offsets, lengths);
      
    // shift out to function
    int result = getGroupChar(startgpstrindex, lengths, name, choice);
    if (result != -1) {
      return (getGroupMSB(index) << GROUP_SHIFT_) | result;
    }
    return -1;
  }
   
  /**
  * Set the algorithm name information array
  * @param algorithm information array
  * @return true if the group string offset has been set correctly
  */
  protected boolean setAlgorithm(AlgorithmName alg[])
  {
    if (alg != null && alg.length != 0) {
      m_algorithm_ = alg;
      return true;
    }
    return false;
  }
  
  /**
  * Get the number of algorithm name groups
  * @return number of algorithm name groups
  */
  protected int countAlgorithm()
  {
    if (m_algorithm_ == null) {
      return 0;
    }
    return m_algorithm_.length;
  }
  
  /**
  * Gets the index of the Algorithm object the argument code point lies
  * @param ch code point 
  * @return index of the Algorithm object the argument code point lies, 
  *         otherwise -1 if code point is not found in Algorithm objects
  */
  protected int getAlgorithmIndex(int ch)
  {
    for (int index = m_algorithm_.length - 1; index >= 0; index --) {
      if (m_algorithm_[index].contains(ch)) {
        return index;
      }
    }
    return -1;
  }
  
  /**
  * Appends algorithm name of code point into StringBuffer.
  * Note this method does not check for validity of code point in Algorithm,
  * result is undefined if code point does not belong in Algorithm.
  * @param index of Algorithm object in array
  * @param ch code point
  * @param str StringBuffer to append to
  */
  protected void appendAlgorithmName(int index, int ch, StringBuffer str)
  {
    m_algorithm_[index].appendName(ch, str);
  }
  
  /**
  * Get algorithm code point for the argument name at index. If name is not
  * found in algorithm, -1 is returned.
  * @param index algorithm index
  * @param name code point name
  * @param code point in algorithm that matches name, -1 otherwise
  */
  protected int getAlgorithmChar(int index, String name)
  {
    return m_algorithm_[index].getAlgorithmChar(name);
  }
  
  // private methods =================================================
  
  /**
  * Gets the most significant bits representation in the argument group
  * @param index the indexth group in datatable
  * @return most significant bits representation of group
  */
  private char getGroupMSB(int index)
  {
    return m_groupinfo_[index * m_groupsize_];
  }
  
  /**
  * Reads a block of compressed lengths of 32 strings and expands them into 
  * offsets and lengths for each string. Lengths are stored with a 
  * variable-width encoding in consecutive nibbles:
  * If a nibble<0xc, then it is the length itself (0 = empty string).
  * If a nibble>=0xc, then it forms a length value with the following nibble.
  * The offsets and lengths arrays must be at least 33 (one more) long because
  * there is no check here at the end if the last nibble is still used.
  * @param index of group string object in array
  * @param offsets array to store the value of the string offsets
  * @param lengths array to store the value of the string length
  * @return next index of the data string immediately after the lengths 
  *         in terms of byte address
  */
  private int getGroupLengths(int index, char offsets[], char lengths[]) 
  {
    char length = 0xffff;
    byte b = 0,
         n = 0;
    int shift;
    index = index * m_groupsize_; // byte count offsets of group strings
    int stringoffset = UCharacterUtil.toInt(
                                     m_groupinfo_[index + OFFSET_HIGH_OFFSET_], 
                                     m_groupinfo_[index + OFFSET_LOW_OFFSET_]);
        
    offsets[0] = 0;
    
    // all 32 lengths must be read to get the offset of the first group string
    for (int i = 0; i < LINES_PER_GROUP_; stringoffset ++) {
      b = m_groupstring_[stringoffset];
      shift = 4;
      
      while (shift >= 0) {
        // getting nibble
        n = (byte)((b >> shift) & 0x0F);   
        if (length == 0xffff && n > SINGLE_NIBBLE_MAX_) {
          length = (char)((n - 12) << 4);
        }
        else {
          if (length != 0xffff) {
            lengths[i] = (char)((length | n) + 12);
          }
          else {
            lengths[i] = (char)n;
          }
            
          if (i < LINES_PER_GROUP_) {
            offsets[i + 1] = (char)(offsets[i] + lengths[i]);
          }
            
          length = 0xffff;
          i ++;
        }
              
        shift -= 4;
      }
    }
    return stringoffset;
  }
  
  /**
  * Gets the name of the argument group index
  * @param index of the group name string in byte count
  * @param length of the group name string
  * @param choice of Unicode 1.0 name or the most current name
  * @return name of the group 
  */
  private String getGroupName(int index, int length, int choice) 
  {
    if (choice != UCharacterNameChoice.U_UNICODE_CHAR_NAME) {
      int oldindex = index;
      index += UCharacterUtil.skipByteSubString(m_groupstring_, index, length, 
                                                (byte)';');
      length -= (index - oldindex);
    }
    
    StringBuffer s = new StringBuffer();
    byte b;
    char token;
    for (int i = 0; i < length;) {
      b = m_groupstring_[index + i];
      i ++;
      
      if (b >= m_tokentable_.length) {
        if (b == ';') {
          break;
        }
        s.append(b); // implicit letter
      }
      else {
        token = m_tokentable_[b & 0x00ff];
        if (token == 0xFFFE) {
          // this is a lead byte for a double-byte token
          token = m_tokentable_[b << 8 | (m_groupstring_[index + i] & 0x00ff)];
          i ++;
        }
        if (token == 0xFFFF) {
          if (b == ';') {
            break;
          }
          s.append((char)(b & 0x00ff)); // explicit letter
        }
        else { // write token word
          UCharacterUtil.getNullTermByteSubString(s, m_tokenstring_, token);
        }
      }
    }

    if (s.length() == 0) {
      return null;
    }
    return s.toString();
  }
  
  /**
  * Compares and retrieve character if name is found within the argument 
  * group
  * @param index index where the set of names reside in the group block
  * @param length list of lengths of the strings
  * @param name character name to search for
  * @param choice of either 1.0 or the most current unicode name
  * @return relative character in the group which matches name, otherwise if   
  *         not found, -1 will be returned
  */
  private int getGroupChar(int index, char length[], String name, int choice)
  { 
    byte b = 0; 
    char token;
    int len;
    int namelen = name.length();
    int nindex;
    int count;
    
    for (int result = 0; result <= LINES_PER_GROUP_; result ++) {
      nindex = 0;
      len = length[result];
      
      if (choice != UCharacterNameChoice.U_UNICODE_CHAR_NAME) {
        int oldindex = index;
        index += UCharacterUtil.skipByteSubString(m_groupstring_, index, len, 
                                                  (byte)';');
        len -= (index - oldindex);
      }
        
      // number of tokens is > the length of the name
      // write each letter directly, and write a token word per token
      for (count = 0; count < len && nindex != -1 && nindex < namelen;) {
        b = m_groupstring_[index + count];
        count ++;
           
        if (b >= m_tokentable_.length) {
          if (name.charAt(nindex ++) != (b & 0xFF)) {
            nindex = -1;
          }
        }
        else {
          token = m_tokentable_[b & 0xFF];
          if (token == 0xFFFE) {
            // this is a lead byte for a double-byte token
            token = m_tokentable_[b << 8 | 
                                  (m_groupstring_[index + count] & 0x00ff)];
            count ++;
          }
          if (token == 0xFFFF) {
            if (name.charAt(nindex ++) != (b & 0xFF)) {
              nindex = -1;
            }
          }
          else {
            // compare token with name
            nindex = UCharacterUtil.compareNullTermByteSubString(name, 
                     m_tokenstring_, nindex, token);
          }
        }
      }

      if (namelen == nindex && 
          (count == len || m_groupstring_[index + count] == ';')) {
        return result;
          }
        
      index += len;
    }
    return -1;
  }
  
  // protected inner class ===========================================
  
  /**
  * Algorithmic name class
  */
  static final class AlgorithmName
  {
    // protected variables ===========================================
    
    /**
    * Constant type value of the different AlgorithmName
    */
    protected static final int TYPE_0_ = 0;
    protected static final int TYPE_1_ = 1;
    
    // private variables =============================================
    
    /**
    * Algorithmic data information
    */
    private int m_rangestart_;
    private int m_rangeend_;
    private byte m_type_;
    private byte m_variant_;
    private char m_factor_[];
    private String m_prefix_;
    private byte m_factorstring_[];
    
    // constructor ===================================================
    
    /**
    * Constructor
    */
    protected AlgorithmName()
    {
    }
    
    // protected methods =============================================
    
    /**
    * Sets the information for accessing the algorithmic names
    * @param rangestart starting code point that lies within this name group
    * @param rangeend end code point that lies within this name group
    * @param type algorithm type. There's 2 kinds of algorithmic type. First 
    *        which uses code point as part of its name and the other uses 
    *        variant postfix strings
    * @param variant algorithmic variant
    * @return true if values are valid
    */ 
    protected boolean setInfo(int rangestart, int rangeend, byte type,
                              byte variant)
    {
      if (rangestart >= UCharacter.MIN_VALUE && rangestart <= rangeend &&
          rangeend <= UCharacter.MAX_VALUE && 
          (type == TYPE_0_ || type == TYPE_1_)) {
        m_rangestart_ = rangestart;
        m_rangeend_ = rangeend;
        m_type_ = type;
        m_variant_ = variant;
        return true;
      }
      return false;
    }
    
    /**
    * Sets the factor data
    * @param array of factor
    * @return true if factors are valid
    */
    protected boolean setFactor(char factor[])
    {
      if (factor.length == m_variant_) {
        m_factor_ = factor;
        return true;
      }
      return false;
    }
    
    /**
    * Sets the name prefix
    * @param prefix
    * @return true if prefix is set
    */
    protected boolean setPrefix(String prefix)
    {
      if (prefix != null && prefix.length() > 0) {
        m_prefix_ = prefix;
        return true;
      }
      return false;
    }
    
    /**
    * Sets the variant factorized name data 
    * @param string variant factorized name data
    * @return true if values are set
    */
    protected boolean setFactorString(byte string[])
    {
      // factor and variant string can be empty for things like hanggul code
      // points
      m_factorstring_ = string;
      return true;
    }
  
    /**
    * Checks if code point lies in Algorithm object at index
    * @param ch code point 
    */
    protected boolean contains(int ch)
    {
      return m_rangestart_ <= ch && ch <= m_rangeend_;
    }
    
    /**
    * Appends algorithm name of code point into StringBuffer.
    * Note this method does not check for validity of code point in Algorithm,
    * result is undefined if code point does not belong in Algorithm.
    * @param ch code point
    * @param str StringBuffer to append to
    */
    protected void appendName(int ch, StringBuffer str)
    {
      str.append(m_prefix_);
      switch (m_type_) 
      {
        case TYPE_0_: 
          // prefix followed by hex digits indicating variants
          Utility.hex(ch, m_variant_, str);
          break;
        case TYPE_1_: 
          // prefix followed by factorized-elements
          int offset = ch - m_rangestart_;
          int indexes[] = new int[m_variant_];
          int factor;
          
          // write elements according to the factors
          // the factorized elements are determined by modulo arithmetic
          for (int i = m_variant_ - 1; i > 0; i --) 
          {
            factor = m_factor_[i] & 0x00FF;
            indexes[i] = offset % factor;
            offset /= factor;
          }
          
          // we don't need to calculate the last modulus because 
          // start <= code <= end guarantees here that code <= factors[0]
          indexes[0] = offset;

          // joining up the factorized strings 
          String s[] = getFactorString(indexes);
          if (s != null && s.length > 0)
          {
            int size = s.length;
            for (int i = 0; i < size; i ++)
              str.append(s[i]);
          }
          break;
      }
    }
    
    /**
    * Gets the character for the argument algorithmic name
    * @return the algorithmic char or -1 otherwise.
    */
    protected int getAlgorithmChar(String name)
    {
      int prefixlen = m_prefix_.length();
      if (name.length() < prefixlen || 
          !m_prefix_.equals(name.substring(0, prefixlen))) {
        return -1;
      }
        
      switch (m_type_) 
      {
        case TYPE_0_ : 
          try
          {
            int result = Integer.parseInt(name.substring(prefixlen), 16);
            // does it fit into the range?
            if (m_rangestart_ <= result && result <= m_rangeend_) {
              return result;
            }
          }
          catch (NumberFormatException e)
          {
            return -1;
          }
          break;
        case TYPE_1_ : 
          // repetitative suffix name comparison done here
          // offset is the character code - start
          for (int ch = m_rangestart_; ch <= m_rangeend_; ch ++)
          {
            int offset = ch - m_rangestart_;
            int indexes[] = new int[m_variant_];
            int factor;
      
            // write elements according to the factors
            // the factorized elements are determined by modulo arithmetic
            for (int i = m_variant_ - 1; i > 0; i --) 
            {
              factor = m_factor_[i] & 0x00FF;
              indexes[i] = offset % factor;
              offset /= factor;
            }
            
            // we don't need to calculate the last modulus because 
            // start <= code <= end guarantees here that code <= factors[0]
            indexes[0] = offset;

            // joining up the factorized strings 
            if (compareFactorString(indexes, name.substring(prefixlen))) {
              return ch;
            }
          }
      }

      return -1;
    }
    
    // private methods ================================================
    
    /**
    * Gets the indexth string in each of the argument factor block
    * @param index array with each index corresponding to each factor block
    * @return array of indexth factor string in factor block
    */
    private String[] getFactorString(int index[])
    {
      int size = m_factor_.length;
      if (index == null || index.length != size) {
        return null;
      }
        
      String result[] = new String[size];
      StringBuffer str = new StringBuffer();
      int count = 0;
      int factor;
      size --;
      for (int i = 0; i <= size; i ++) {
        factor = m_factor_[i];
        count = UCharacterUtil.skipNullTermByteSubString(m_factorstring_, 
                                                         count, index[i]);
        count = UCharacterUtil.getNullTermByteSubString(str, m_factorstring_, 
                                                        count);
        if (i != size) {
          count = UCharacterUtil.skipNullTermByteSubString(m_factorstring_, 
                                                 count, factor - index[i] - 1);
        }
        result[i] = str.toString();
        str.delete(0, str.length());
      }
      return result;
    }
    
    /**
    * Compares the indexth string in each of the argument factor block with
    * the argument string
    * @param index array with each index corresponding to each factor block
    * @param str string to compare with
    * @return true if string matches
    */
    private boolean compareFactorString(int index[], String str)
    {
      int size = m_factor_.length;
      if (index == null || index.length != size)
        return false;
        
      int count = 0;
      int strcount = 0;
      int factor;
      size --;
      for (int i = 0; i <= size; i ++)
      {
        factor = m_factor_[i];
        count = UCharacterUtil.skipNullTermByteSubString(m_factorstring_, 
                                                         count, index[i]);
        strcount = UCharacterUtil.compareNullTermByteSubString(str, 
                                             m_factorstring_, strcount, count);
        if (strcount < 0) {
          return false;
        }
          
        if (i != size) {
          count = UCharacterUtil.skipNullTermByteSubString(m_factorstring_, 
                                                 count, factor - index[i]);
        }
      }
      if (strcount != str.length()) {
        return false;
      }
      return true;
    }
  }
}

