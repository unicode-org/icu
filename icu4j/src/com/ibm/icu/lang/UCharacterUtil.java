/**
*******************************************************************************
* Copyright (C) 1996-2000, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/lang/Attic/UCharacterUtil.java,v $ 
* $Date: 2001/03/07 02:52:05 $ 
* $Revision: 1.2 $
*
*******************************************************************************
*/
package com.ibm.text;

/**
* Internal character utility class for simple data type conversion and String 
* parsing functions. Does not have an analog in the JDK.
* @author Syn Wee Quek
* @since sep2900
*/

final class UCharacterUtil
{
  // constructor =====================================================
  
  /**
  * private constructor to avoid initialisation
  */
  private UCharacterUtil()
  {
  }
  
  // protected methods ===============================================
  
  /**
  * joining 2 chars to form an int
  * @param msc most significant char
  * @param lsc least significant char
  * @return int form
  */
  protected static int toInt(char msc, char lsc)
  {
    return ((msc << 16) | lsc);
  }
   
  /**
  * converting first 2 bytes of a byte array into char
  * if array size is < 2 then algorithmn will only return value == 1 byte
  * @param bytes 2 byte argument
  * @return char form
  */
  protected static char toChar(byte bytes[])
  {
    if (bytes == null || bytes.length == 0) {
      return 0;
    }
    if (bytes.length == 1) {
      return toChar(bytes[0]);
    }
      
    char firstbyte = (char)(toChar(bytes[0]) << 8);
    char secondbyte = toChar(bytes[1]);
    
    return (char)(firstbyte | secondbyte);
  }
   
  /**
  * converting 2 bytes into a char
  * @param msb the most significant byte
  * @param lsb the least significant byte
  * @return char form
  */
  protected static char toChar(byte msb, byte lsb)
  {
    char firstbyte = (char)(toChar(msb) << 8);
    char secondbyte = toChar(lsb);
    
    return (char)(firstbyte | secondbyte);
  }
  
  /**
  * converting first 4 bytes of a byte array into int
  * if array size is < 4 then algorithmn will only return value == # bytes
  * @param bytes 4 byte argument
  * @return int form
  */
  protected static int toInt(byte bytes[])
  {
    if (bytes == null || bytes.length == 0)
      return 0;
    int size = bytes.length;
    if (size > 4)
      size = 4;
    int result = 0;
    for (int i = 0; i < size; i ++)
      result = (result << 8) | (0x000000FF & bytes[i]);
    return result;
  }
   
  /**
  * converting a byte into char
  * @param onebyte 
  * @return char form
  */
  protected static char toChar(byte onebyte)
  {
    char result = (char)(onebyte & 0x000000ff);
    return result;
  }
   
  /**
  * converting a integer to a array of 4 characters where each character
  * corresponds to its respective byte 
  * @param i integer to be converted
  * @return array of 4 characters
  */
  protected static char[] to4Char(int i)
  {
    char result[] = new char[4];
    result[0] = (char)((i >> 24) & 0xFF);
    result[1] = (char)((i & 0x00FF0000) >> 16);
    result[2] = (char)((i & 0x0000FF00) >> 8);
    result[3] = (char)(i & 0xFF);
    return result;
  }
   
  /**
  * Retrieves a null terminated substring from an array of bytes.
  * Substring is a set of non-zero bytes starting from argument start to the 
  * next zero byte. If the first byte is a zero, the next byte will be taken as
  * the first byte.
  * @param str stringbuffer to store data in, data will be store with each
  *            byte as a char
  * @param array byte array
  * @param index to start substring in byte count
  * @return the end position of the substring within the character array
  */
  protected static int getNullTermByteSubString(StringBuffer str, byte[] array, 
                                                int index)
  {
    byte b = 1;
    
    while (b != 0)
    {
      b = array[index];
      if (b != 0) {
        str.append((char)(b & 0x00FF));
      }
      index ++;
    }
    return index;
  }
   
  /**
  * Compares a null terminated substring from an array of bytes.
  * Substring is a set of non-zero bytes starting from argument start to the 
  * next zero byte. if the first byte is a zero, the next byte will be taken as
  * the first byte.
  * @param str string to compare
  * @param array byte array
  * @param strindex index within str to start comparing
  * @param aindex array index to start in byte count
  * @return the end position of the substring within str if matches otherwise 
  *         a -1
  */
  protected static int compareNullTermByteSubString(String str, byte[] array, 
                                                    int strindex, int aindex)
  {
    byte b = 1;
    int length = str.length();
    
    while (b != 0)
    {
      b = array[aindex];  
      aindex ++;
      if (b == 0) {
        break;
      }
      // if we have reached the end of the string and yet the array has not 
      // reached the end of their substring yet, abort
      if (strindex == length || (str.charAt(strindex) != (char)(b & 0xFF))) {
        return -1;
      }
      strindex ++;
    }
    return strindex;
  }
   
  /**
  * Skip null terminated substrings from an array of bytes.
  * Substring is a set of non-zero bytes starting from argument start to the 
  * next zero byte. If the first byte is a zero, the next byte will be taken as
  * the first byte.
  * @param array byte array
  * @param index to start substrings in byte count
  * @param skipcount number of null terminated substrings to skip
  * @return the end position of the substrings within the character array
  */
  protected static int skipNullTermByteSubString(byte[] array, int index, 
                                                 int skipcount)
  {
    byte b;
    for (int i = 0; i < skipcount; i ++)
    {
      b = 1;
      while (b != 0)
      {
        b = array[index];
        index ++;
      }
    }
    return index;
  }
   
  /**
  * skip substrings from an array of characters, where each character is a set 
  * of 2 bytes. substring is a set of non-zero bytes starting from argument 
  * start to the byte of the argument value. skips up to a max number of 
  * characters
  * @param array byte array to parse
  * @param index to start substrings in byte count
  * @param length the max number of bytes to skip
  * @param skipend value of byte to skip to
  * @return the number of bytes skipped
  */
  protected static int skipByteSubString(byte[] array, int index, int length, 
                                         byte skipend)
  {
    int result;
    byte b;
    
    for (result = 0; result < length; result ++)
    {
      b = array[index + result];
      if (b == skipend)
      {
        result ++;
        break;
      }
    }
    
    return result;
  }
   
  /**
  * skip substrings from an array of characters, where each character is a set 
  * of 2 bytes. substring is a set of non-zero bytes starting from argument 
  * start to the byte of the argument value. 
  * @param array byte array to parse
  * @param index to start substrings in byte count
  * @param skipend value of byte to skip to
  * @return the number of bytes skipped
  */
  protected static int skipByteSubString(byte[] array, int index, byte skipend)
  {
    int result = 0;
    byte b;
    
    while (true)
    {
      b = array[index + result];
      result ++;
      if (b == skipend) {
        break;
      }
    }
    
    return result;
  }
}

