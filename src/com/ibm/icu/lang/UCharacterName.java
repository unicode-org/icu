/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: 
*     /usr/cvs/icu4j/icu4j/src/com/ibm/icu/text/UCharacterName.java $ 
* $Date: 2002/07/30 02:38:11 $ 
* $Revision: 1.17 $
*
*******************************************************************************
*/
package com.ibm.icu.lang;

import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.UTF16;

/**
* Internal class to manage character names.
* Since data in <a href=UCharacterNameDB.html>UCharacterNameDB</a> is stored
* in an array of char, by default indexes used in this class is refering to 
* a 2 byte count, unless otherwise stated. Cases where the index is refering 
* to a byte count, the index is halved and depending on whether the index is 
* even or odd, the MSB or LSB of the result char at the halved index is 
* returned. For indexes to an array of int, the index is multiplied by 2, 
* result char at the multiplied index and its following char is returned as an 
* int.
* <a href=UCharacter.html>UCharacter</a> acts as a public facade for this class
* Note : 0 - 0x1F are control characters without names in Unicode 3.0
* Information on parsing of the binary data is located at
* <a href=oss.software.ibm.com/icu4j/icu4jhtml/com/ibm/icu/text/readme.html>
* ReadMe</a>
* @author Syn Wee Quek
* @since nov0700
*/

final class UCharacterName
{
    // public methods ----------------------------------------------------
    
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
    
    // package protected inner class -------------------------------------
    
    /**
    * Algorithmic name class
    */
    static final class AlgorithmName
    {
        // protected data members ----------------------------------------
        
        /**
        * Constant type value of the different AlgorithmName
        */
        protected static final int TYPE_0_ = 0;
        protected static final int TYPE_1_ = 1;
        
        // protected constructors ----------------------------------------
        
        /**
        * Constructor
        */
        protected AlgorithmName()
        {
        }
        
        // protected methods ---------------------------------------------
        
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
            if (rangestart >= UCharacter.MIN_VALUE && rangestart <= rangeend 
                && rangeend <= UCharacter.MAX_VALUE && 
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
            // factor and variant string can be empty for things like 
            // hanggul code points
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
                    // the factorized elements are determined by modulo 
                    // arithmetic
                    for (int i = m_variant_ - 1; i > 0; i --) 
                    {
                        factor = m_factor_[i] & 0x00FF;
                        indexes[i] = offset % factor;
                        offset /= factor;
                    }
                      
                    // we don't need to calculate the last modulus because 
                    // start <= code <= end guarantees here that 
                    // code <= factors[0]
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
                    int result = Integer.parseInt(name.substring(prefixlen), 
                                                  16);
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
                        // the factorized elements are determined by modulo 
                        // arithmetic
                        for (int i = m_variant_ - 1; i > 0; i --) 
                        {
                            factor = m_factor_[i] & 0x00FF;
                            indexes[i] = offset % factor;
                            offset /= factor;
                        }
                        
                        // we don't need to calculate the last modulus 
                        // because start <= code <= end guarantees here that 
                        // code <= factors[0]
                        indexes[0] = offset;

                        // joining up the factorized strings 
                        if (compareFactorString(indexes, name, prefixlen)) {
                            return ch;
                        }
                    }
            }

            return -1;
        }
        
        // private data members ------------------------------------------
        
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
        
        // private methods -----------------------------------------------
                
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
                count = UCharacterUtil.skipNullTermByteSubString(
                                          m_factorstring_, count, index[i]);
                count = UCharacterUtil.getNullTermByteSubString(
                                          str, m_factorstring_, count);
                if (i != size) {
                    count = UCharacterUtil.skipNullTermByteSubString(
                                                   m_factorstring_, count, 
                                                   factor - index[i] - 1);
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
        * @param offset of str to start comparison
        * @return true if string matches
        */
        private boolean compareFactorString(int index[], String str, 
                                            int offset)
        {
            int size = m_factor_.length;
            if (index == null || index.length != size)
                return false;
                
            int count = 0;
            int strcount = offset;
            int factor;
            size --;
            for (int i = 0; i <= size; i ++)
            {
                factor = m_factor_[i];
                count = UCharacterUtil.skipNullTermByteSubString(
                                          m_factorstring_, count, index[i]);
                strcount = UCharacterUtil.compareNullTermByteSubString(str, 
                                          m_factorstring_, strcount, count);
                if (strcount < 0) {
                    return false;
                }
                  
                if (i != size) {
                    count = UCharacterUtil.skipNullTermByteSubString(
                                  m_factorstring_, count, factor - index[i]);
                }
            }
            if (strcount != str.length()) {
                return false;
            }
            return true;
        }
    }
    
    // protected data members --------------------------------------------
    
    /**
     * Maximum number of groups
     */
    protected int m_groupcount_ = 0;
    /**
     * Size of each groups
     */
    protected int m_groupsize_ = 0;
    /**
    * Number of lines per group 
    * 1 << GROUP_SHIFT_
    */
    protected static final int LINES_PER_GROUP_ = 1 << 5;
    
    // protected constructor ---------------------------------------------
    
    /**
    * <p>Protected constructor for use in UCharacter.</p>
    * @exception IOException thrown when data reading fails
    */
    protected UCharacterName() throws IOException
    {
        InputStream i = getClass().getResourceAsStream(NAME_FILE_NAME_);
        BufferedInputStream b = new BufferedInputStream(i, 
                                                        NAME_BUFFER_SIZE_);
        UCharacterNameReader reader = new UCharacterNameReader(b);
        reader.read(this);
        i.close();
    }
      
    // protected methods -------------------------------------------------
     
    /**
    * Retrieve the name of a Unicode code point.
    * Depending on <code>choice</code>, the character name written into the 
    * buffer is the "modern" name or the name that was defined in Unicode 
    * version 1.0.
    * The name contains only "invariant" characters
    * like A-Z, 0-9, space, and '-'.
    *
    * @param ch the code point for which to get the name.
    * @param choice Selector for which name to get.
    * @return if code point is above 0x1fff, null is returned
    */
    protected String getName(int ch, int choice)
    {
        if (ch < UCharacter.MIN_VALUE || ch > UCharacter.MAX_VALUE || 
            choice >= UCharacterNameChoice.U_CHAR_NAME_CHOICE_COUNT) {
            return null;
        }
        
        String result = null;
        
        result = getAlgName(ch, choice);
          
        // getting normal character name
        if (result == null || result.length() == 0) {
        	if (choice == UCharacterNameChoice.U_EXTENDED_CHAR_NAME) {	  
                result = getExtendedName(ch);	
            } else {
                result = getGroupName(ch, choice);
            }
        }
          
        return result;
    }
      
    /**
    * Find a character by its name and return its code point value
    * @param character name
    * @param choice selector to indicate if argument name is a Unicode 1.0 
    *        or the most current version 
    * @return code point
    */
    protected int getCharFromName(int choice, String name)
    {
        // checks for illegal arguments
        if (choice >= UCharacterNameChoice.U_CHAR_NAME_CHOICE_COUNT || 
            name == null || name.length() == 0) {
            return -1;
        }
        
        // try extended names first  
        int result = getExtendedChar(name.toLowerCase(), choice);
        if (result >= -1) {
            return result;
        }
        
        String upperCaseName = name.toUpperCase();
        // try algorithmic names first, if fails then try group names
        // int result = getAlgorithmChar(choice, uppercasename);
        
        if (choice != UCharacterNameChoice.U_UNICODE_10_CHAR_NAME) {
        	int count = 0;
        	if (m_algorithm_ != null) {
        	    count = m_algorithm_.length;
        	}
        	for (count --; count >= 0; count --) {
         	    result = m_algorithm_[count].getAlgorithmChar(upperCaseName); 
          	    if (result >= 0) {
           	        return result;
            	}
        	}
        }
            
        if (choice == UCharacterNameChoice.U_EXTENDED_CHAR_NAME) {
	        result = getGroupChar(upperCaseName, 
	                              UCharacterNameChoice.U_UNICODE_CHAR_NAME);
        	if (result == -1) {
	            result = getGroupChar(upperCaseName, 
	                              UCharacterNameChoice.U_UNICODE_10_CHAR_NAME);
        	}
        }
        else {
        	result = getGroupChar(upperCaseName, choice);
        }
    	return result;
    }
    
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
    * Reads a block of compressed lengths of 32 strings and expands them into 
    * offsets and lengths for each string. Lengths are stored with a 
    * variable-width encoding in consecutive nibbles:
    * If a nibble<0xc, then it is the length itself (0 = empty string).
    * If a nibble>=0xc, then it forms a length value with the following 
    * nibble.
    * The offsets and lengths arrays must be at least 33 (one more) long 
    * because there is no check here at the end if the last nibble is still 
    * used.
    * @param index of group string object in array
    * @param offsets array to store the value of the string offsets
    * @param lengths array to store the value of the string length
    * @return next index of the data string immediately after the lengths 
    *         in terms of byte address
    */
    protected int getGroupLengths(int index, char offsets[], char lengths[]) 
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
        
        // all 32 lengths must be read to get the offset of the first group 
        // string
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
    protected String getGroupName(int index, int length, int choice) 
    {
        if (choice == UCharacterNameChoice.U_UNICODE_10_CHAR_NAME) {
        	int oldindex = index;
         	index += UCharacterUtil.skipByteSubString(m_groupstring_, 
         		                               index, length, (byte)';');   
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
                    token = m_tokentable_[b << 8 | 
                                      (m_groupstring_[index + i] & 0x00ff)];
                    i ++;
                }
                if (token == 0xFFFF) {
                    if (b == ';') {
                    	// skip the semicolon if we are seeking extended 
                    	// names and there was no 2.0 name but there
                        // is a 1.0 name.
                    	if (s.length() == 0 && choice == 
                    	       UCharacterNameChoice.U_EXTENDED_CHAR_NAME) {
                        	continue;
                    	}
                        break;
                    }
                    s.append((char)(b & 0x00ff)); // explicit letter
                }
                else { // write token word
                    UCharacterUtil.getNullTermByteSubString(s, 
                                                     m_tokenstring_, token);
                }
            }
        }

        if (s.length() == 0) {
            return null;
        }
        return s.toString();
    }
    
    /**
    * Retrieves the extended name
    */
    protected String getExtendedName(int ch) 
    {    
        String result = getName(ch, UCharacterNameChoice.U_UNICODE_CHAR_NAME);    
        if (result == null) {        
            if (getType(ch) == UCharacterCategory.CONTROL) {            
                result = getName(ch, 
                                 UCharacterNameChoice.U_UNICODE_10_CHAR_NAME);        
            }        
            if (result == null) {            
                result = getExtendedOr10Name(ch);
            }
        }    
        return result;
    }
    
    /**
     * Gets the group index for the codepoint, or the group before it.
     * @param codepoint
     * @return group index containing codepoint or the group before it.
     */
    protected int getGroup(int codepoint)
    {
    	int endGroup = m_groupcount_;
    	int msb      = getCodepointMSB(codepoint);
        int result   = 0;    
        // binary search for the group of names that contains the one for 
        // code
        // find the group that contains codepoint, or the highest before it
        while (result < endGroup - 1) {
            int gindex = (result + endGroup) >> 1;
            if (msb < getGroupMSB(gindex)) {
               	endGroup = gindex;
            }
            else {
               	result = gindex;
            }
        }
        return result;
    }
    
    /**
     * Gets the extended and 1.0 name when the most current unicode names
     * fail
     * @param ch codepoint
     * @return name of codepoint extended or 1.0
     */
    protected String getExtendedOr10Name(int ch)
    {
    	String result = null;
    	if (getType(ch) == UCharacterCategory.CONTROL) {            
            result = getName(ch, 
                             UCharacterNameChoice.U_UNICODE_10_CHAR_NAME);        
        }        
        if (result == null) {            
            int type = getType(ch);    
            // Return unknown if the table of names above is not up to 
            // date.
            if (type >= UCharacterCategory.TYPE_NAMES_.length) {       
                result = UCharacterCategory.UNKNOWN_TYPE_NAME_;    
            } 
            else {        
                result = UCharacterCategory.TYPE_NAMES_[type];    
            }
            StringBuffer tempResult = new StringBuffer(result);
            tempResult.insert(0, '<');
            tempResult.append('-');
            String chStr = Integer.toHexString(ch).toUpperCase();
            int zeros = 4 - chStr.length();
            while (zeros > 0) {
                tempResult.append('0');
                zeros --;
            }
            tempResult.append(chStr);
            tempResult.append('>');
            result = tempResult.toString();
        }
        return result;
    }
    
    // these are all UCharacterNameIterator use methods -------------------
    
    /**
     * Gets the MSB from the group index
     * @param gindex group index
     * @return the MSB of the group if gindex is valid, -1 otherwise
     */
    protected int getGroupMSB(int gindex)
    {
    	if (gindex >= m_groupcount_) {
    		return -1;
    	}
    	return m_groupinfo_[gindex * m_groupsize_];
    }
    
    /**
     * Gets the MSB of the codepoint
     * @param codepoint 
     * @return the MSB of the codepoint
     */
    protected int getCodepointMSB(int codepoint)
    {
    	return codepoint >> GROUP_SHIFT_;
    }
    
    /**
     * Gets the maximum codepoint + 1 of the group
     * @param msb most significant byte of the group
     * @return limit codepoint of the group
     */
    protected int getGroupLimit(int msb)
    {
    	return (msb << GROUP_SHIFT_) + LINES_PER_GROUP_;
    }
    
    /**
     * Gets the minimum codepoint of the group
     * @param msb most significant byte of the group
     * @return minimum codepoint of the group
     */
    protected int getGroupMin(int msb)
    {
    	return msb << GROUP_SHIFT_;
    }
    
    /**
     * Gets the offset to a group
     * @param codepoint 
     * @return offset to a group
     */
    protected int getGroupOffset(int codepoint)
    {
    	return codepoint & GROUP_MASK_;
    }

	/**
     * Gets the minimum codepoint of a group
     * @param codepoint
     * @return minimum codepoint in the group which codepoint belongs to
     */
    protected int getGroupMinFromCodepoint(int codepoint)
    {
    	return codepoint & ~GROUP_MASK_;
    }
    
    /**
     * Get the Algorithm range length 
     * @return Algorithm range length
     */
    protected int getAlgorithmLength()
    {
    	return m_algorithm_.length;
    }
        
    /**
     * Gets the start of the range
     * @param index algorithm index
     * @return algorithm range start
     */
    protected int getAlgorithmStart(int index)
    {
      	return m_algorithm_[index].m_rangestart_;
    }
        
    /**
     * Gets the end of the range
     * @param index algorithm index
     * @return algorithm range end
     */
    protected int getAlgorithmEnd(int index)
    {
      	return m_algorithm_[index].m_rangeend_;
    }
    
    /**
     * Gets the Algorithmic name of the codepoint
     * @param index algorithmic range index
     * @param codepoint 
     * @return algorithmic name of codepoint
     */
    protected String getAlgorithmName(int index, int codepoint) 
    {
    	StringBuffer result = new StringBuffer();
    	m_algorithm_[index].appendName(codepoint, result);
        return result.toString();
    }
    
        
    // private data members ----------------------------------------------
    
    /**
    * Data used in unames.icu
    */
    private char m_tokentable_[];
    private byte m_tokenstring_[];
    private char m_groupinfo_[];
    private byte m_groupstring_[];
    private AlgorithmName m_algorithm_[];
      
    /**
    * Group use
    */
    private char m_groupoffsets_[] = new char[LINES_PER_GROUP_ + 1];
    private char m_grouplengths_[] = new char[LINES_PER_GROUP_ + 1];
      	 
    /**
    * Default name of the name datafile
    */
    private static final String NAME_FILE_NAME_ = 
                                           "/com/ibm/icu/impl/data/unames.icu";
    /**
    * Shift count to retrieve group information
    */
    private static final int GROUP_SHIFT_ = 5;
    /**
    * Mask to retrieve the offset for a particular character within a group
    */
    private static final int GROUP_MASK_ = LINES_PER_GROUP_ - 1;
    /**
    * Default buffer size of datafile
    */
    private static final int NAME_BUFFER_SIZE_ = 100000;
      
    /**
    * Position of offsethigh in group information array
    */
    private static final int OFFSET_HIGH_OFFSET_ = 1;
      
    /**
    * Position of offsetlow in group information array
    */
    private static final int OFFSET_LOW_OFFSET_ = 2;
    /**
    * Double nibble indicator, any nibble > this number has to be combined
    * with its following nibble
    */
    private static final int SINGLE_NIBBLE_MAX_ = 11;
     
      
    // private methods ---------------------------------------------------
      
    /**
    * Gets the algorithmic name for the argument character
    * @param ch character to determine name for
    * @param choice name choice
    * @return the algorithmic name or null if not found
    */
    private String getAlgName(int ch, int choice) 
    {
    	// Do not write algorithmic Unicode 1.0 names because Unihan names are 
        // the same as the modern ones, extension A was only introduced with 
        // Unicode 3.0, and the Hangul syllable block was moved and changed 
        // around Unicode 1.1.5.
        if (choice != UCharacterNameChoice.U_UNICODE_10_CHAR_NAME) {
       	 	// index in terms integer index
        	StringBuffer s = new StringBuffer();
        
        	for (int index = m_algorithm_.length - 1; index >= 0; index --) {
         	   if (m_algorithm_[index].contains(ch)) {
          	      m_algorithm_[index].appendName(ch, s);
            	  return s.toString();
         	   }
            }
        }
        return null;
    }
      
    /**
    * Getting the character with the tokenized argument name
    * @param name of the character
    * @return character with the tokenized argument name or -1 if character
    *         is not found
    */
    private synchronized int getGroupChar(String name, int choice) 
    {
    	for (int i = 0; i < m_groupcount_; i ++) {
        	// populating the data set of grouptable
        	
        	int startgpstrindex = getGroupLengths(i, m_groupoffsets_, 
                                                  m_grouplengths_);
          
        	// shift out to function
        	int result = getGroupChar(startgpstrindex, m_grouplengths_, name, 
        	                          choice);
        	if (result != -1) {
            	return (m_groupinfo_[i * m_groupsize_] << GROUP_SHIFT_) 
            	         | result;
        	}
        }
        return -1;
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
    private int getGroupChar(int index, char length[], String name, 
                             int choice)
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
              
            if (choice == UCharacterNameChoice.U_UNICODE_10_CHAR_NAME) {
                int oldindex = index;
                index += UCharacterUtil.skipByteSubString(m_groupstring_, 
                                                     index, len, (byte)';');
                len -= (index - oldindex);
            }
                
            // number of tokens is > the length of the name
            // write each letter directly, and write a token word per token
            for (count = 0; count < len && nindex != -1 && nindex < namelen;
                ) {
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
                        nindex = UCharacterUtil.compareNullTermByteSubString(
                                        name, m_tokenstring_, nindex, token);
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
       
    /**
    * Binary search for the group strings set that contains the argument Unicode 
    * code point's most significant bits.
    * The return value is always a valid group string set that contain msb.
    * If group string set is not found, -1 is returned
    * @param ch the code point to look for
    * @return group string set index in datatable otherwise -1 is returned if 
    *         group string set is not found
    */
    private int getGroupStringIndex(int ch)
    {
        // gets the msb
        int msb = ch >> GROUP_SHIFT_,
            end = m_groupcount_,
            start,
            gindex = 0;
            
        // binary search for the group of names that contains the one for code
        for (start = 0; start < end - 1;) {
            gindex = (start + end) >> 1;
            if (msb < m_groupinfo_[gindex * m_groupsize_]) {
                end = gindex;
            }
            else {
                start = gindex;
            }
        }

        // return this if it is an exact match
        if (msb == m_groupinfo_[start * m_groupsize_]) {
            start = start * m_groupsize_;
            return UCharacterUtil.toInt(
                                m_groupinfo_[start + OFFSET_HIGH_OFFSET_], 
                                m_groupinfo_[start + OFFSET_LOW_OFFSET_]);
        }
        return -1;
    }
    
    /**
    * Gets the group name of the character
    * @param ch character to get the group name 
    * @param choice name choice selector to choose a unicode 1.0 or newer name
    */
    private synchronized String getGroupName(int ch, int choice) 
    {            
        // gets the msb
        int msb   = getCodepointMSB(ch);
        int group = getGroup(ch);

        // return this if it is an exact match
        if (msb == m_groupinfo_[group * m_groupsize_]) {
            int index = getGroupLengths(group, m_groupoffsets_, 
                                        m_grouplengths_);
            int offset = ch & GROUP_MASK_;
            return getGroupName(index + m_groupoffsets_[offset], 
                                m_grouplengths_[offset], choice);
        }
        
        return null;
    }
    
    /**
    * Gets the character extended type
    * @param ch character to be tested
    * @return extended type it is associated with
    */
    private int getType(int ch)
    {
        if (UCharacter.isNonCharacter(ch)) {  
            // not a character we return a invalid category count
            return UCharacterCategory.NON_CHARACTER_;    
        }    
        int result = UCharacter.getType(ch);
        if (result == UCharacterCategory.SURROGATE) {            
            if (ch <= UTF16.LEAD_SURROGATE_MAX_VALUE) {
                result = UCharacterCategory.LEAD_SURROGATE_;
            }
            else {
                result = UCharacterCategory.TRAIL_SURROGATE_;
            }    
        }    
        return result;
    }
    
    /**
    * Getting the character with extended name of the form <....>.
    * @param name of the character to be found
    * @param choice name choice
    * @return character associated with the name, -1 if such character is not
    *                   found and -2 if we should continue with the search.
    */
    private int getExtendedChar(String name, int choice)
    {
        if (name.charAt(0) == '<') {        
            if (choice == UCharacterNameChoice.U_EXTENDED_CHAR_NAME) {            
                int endIndex = name.length() - 1;
                if (name.charAt(endIndex) == '>') {
                    int startIndex = name.lastIndexOf('-');
                    if (startIndex >= 0) { // We've got a category.     
                        startIndex ++;
                        int result = -1;
                        try {
                            result = Integer.parseInt(
                                        name.substring(startIndex, endIndex), 
                                        16);
                        }
                        catch (NumberFormatException e) {
                            return -1;     
                        } 
                        // Now validate the category name. We could use a 
                        // binary search, or a trie, if we really wanted to. 
                        String type = name.substring(1, startIndex - 1);
                        int length = UCharacterCategory.TYPE_NAMES_.length;
                        for (int i = 0; i < length; ++ i) {             
                            if (type.compareTo(
                                   UCharacterCategory.TYPE_NAMES_[i]) == 0) { 
                                if (getType(result) == i) { 
                                    return result;     
                                }  
                                break;          
                            } 
                        }
                    }
                }
            }            
            return -1; 
        }    
        return -2;
    }
}
