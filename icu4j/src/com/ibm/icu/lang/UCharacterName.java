/**
*******************************************************************************
* Copyright (C) 1996-2000, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: 
*     /usr/cvs/icu4j/icu4j/src/com/ibm/icu/text/UCharacterName.java $ 
* $Date: 2001/03/07 02:52:05 $ 
* $Revision: 1.2 $
*
*******************************************************************************
*/
package com.ibm.text;

/**
* Internal class to manage character names.
* <a href=UCharacterNameDB.html>UCharacterNameDB</a> provides the data
* required and UCharacterName parses it into meaningful results before
* returning value.
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
* For information on parsing of the binary data in 
* <a href=UCharacterNameDB.html>UCharacterNameDB</a> is located at
* <a href=oss.software.ibm.com/icu4j/icu4jhtml/com/ibm/icu/text/readme.html>
* ReadMe</a>
* @author Syn Wee Quek
* @since nov0700
*/

final class UCharacterName
{
  // private variable =============================================
  
  /**
  * Database storing the sets of character name
  */
  private static final UCharacterNameDB NAME_DB_;
  
  // block to initialise name database and unicode 1.0 data indicator
  static
  {
    try
    {
      NAME_DB_ = new UCharacterNameDB();
    }
    catch (Exception e)
    {
      throw new RuntimeException(e.getMessage());
    }
  }
  
  // protected method =============================================
 
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
  protected static String getName(int ch, int choice)
  {
    if (ch < 0 || ch > 0x1ffff || 
        choice >= UCharacterNameChoice.U_CHAR_NAME_CHOICE_COUNT) {
      return null;
    }
      
    String result = "";
    
    // Do not write algorithmic Unicode 1.0 names because Unihan names are 
    // the same as the modern ones, extension A was only introduced with 
    // Unicode 3.0, and the Hangul syllable block was moved and changed around 
    // Unicode 1.1.5.
    if (choice == UCharacterNameChoice.U_UNICODE_CHAR_NAME) {
      // try getting algorithmic name first
      result = getAlgName(ch);
    }
    
    // getting normal character name
    if (result == null || result.length() == 0) {
      result = NAME_DB_.getGroupName(ch, choice);
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
  protected static int getCharFromName(int choice, String name)
  {
    // checks for illegal arguments
    if (choice >= UCharacterNameChoice.U_CHAR_NAME_CHOICE_COUNT || 
        name == null || name.length() == 0) {
        return -1;
    }
   
    // try algorithmic names first, if fails then try group names
    int result = getAlgorithmChar(choice, name);
    if (result >= 0) {
      return result;
    }
    return getGroupChar(name, choice);
  }
  
  // private method =============================================
  
  /**
  * Gets the algorithmic name for the argument character
  * @param ch character to determine name for
  * @return the algorithmic name or null if not found
  */
  private static String getAlgName(int ch) 
  {
    // index in terms integer index
    StringBuffer s = new StringBuffer();
    
    int index = NAME_DB_.getAlgorithmIndex(ch);
    if (index >= 0) {
      NAME_DB_.appendAlgorithmName(index, ch, s);
      return s.toString();
    }
    return null;
  }
  
  /**
  * Gets the character for the argument algorithmic name
  * @param choice of either 1.0 or the most current unicode name
  * @return the algorithmic char or -1 otherwise.
  */
  private static int getAlgorithmChar(int choice, String name) 
  {
    // 1.0 has no algorithmic names
    if (choice != UCharacterNameChoice.U_UNICODE_CHAR_NAME) {
      return -1;
    }
    int result;
    for (int count = NAME_DB_.countAlgorithm() - 1; count >= 0; count --) {
      result = NAME_DB_.getAlgorithmChar(count, name); 
      if (result >= 0) {
        return result;
      }
    }
    return -1;
  }
  
  /**
  * Getting the character with the tokenized argument name
  * @param name of the character
  * @return character with the tokenized argument name or -1 if character is
  *         not found
  */
  private static int getGroupChar(String name, int choice) 
  {
    int groupcount = NAME_DB_.countGroup();
    int result = 0;
    
    for (int i = 0; i < groupcount; i ++) {
      result = NAME_DB_.getGroupChar(i, name, choice);
      if (result != -1) {
        return result;
      }
    }
    return -1;
  }
}
