/**
*******************************************************************************
* Copyright (C) 1996-2000, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/text/Attic/UTF16.java,v $ 
* $Date: 2001/03/07 02:52:05 $ 
* $Revision: 1.2 $
*
*******************************************************************************
*/

package com.ibm.text;

import com.ibm.text.UCharacter;

/**
* Standalone utility class providing UTF16 character conversions and indexing 
* conversions.
* <p>Code that uses strings alone rarely need modification. 
* By design, UTF-16 does not allow overlap, so searching for strings is a safe 
* operation. Similarly, concatenation is always safe. Substringing is safe if 
* the start and end are both on UTF-32 boundaries. In normal code, the values 
* for start and end are on those boundaries, since they arose from operations 
* like searching. If not, the nearest UTF-32 boundaries can be determined 
* using <code>bounds()</code>.
* <strong>Examples:</strong>
* <p>The following examples illustrate use of some of these methods. 
* <pre>
* // iteration forwards: Original
* for (int i = 0; i < s.length(); ++i) {
*   char ch = s.charAt(i);
*   doSomethingWith(ch);
* }
*
* // iteration forwards: Changes for UTF-32
* int ch;
* for (int i = 0; i < s.length(); i+=UTF16.getCharCount(ch)) {
*   ch = UTF16.charAt(s,i);
*   doSomethingWith(ch);
* }
*
* // iteration backwards: Original
* for (int i = s.length()-1; i >= 0; --i) {
*   char ch = s.charAt(i);
*   doSomethingWith(ch);
* }
*  
* // iteration backwards: Changes for UTF-32
* int ch;
* for (int i = s.length()-1; i > 0; i-=UTF16.getCharCount(ch)) {
*   ch = UTF16.charAt(s,i);
*   doSomethingWith(ch);
* }
* </pre>
* <strong>Notes:</strong>
* <ul>
*   <li>
*   <strong>Naming:</strong> For clarity, High and Low surrogates are called 
*   <code>Lead</code> and <code>Trail</code> in the API, which gives a better 
*   sense of their ordering in a string. <code>offset16</code> and 
*   <code>offset32</code> are used to distinguish offsets to UTF-16 
*   boundaries vs offsets to UTF-32 boundaries. <code>int char32</code> is 
*   used to contain UTF-32 characters, as opposed to <code>char16</code>, 
*   which is a UTF-16 code unit.
*   </li>
*   <li>
*   <strong>Roundtripping Offsets:</strong> You can always roundtrip from a 
*   UTF-32 offset to a UTF-16 offset and back. Because of the difference in 
*   structure, you can roundtrip from a UTF-16 offset to a UTF-32 offset and 
*   back if and only if <code>bounds(string, offset16) != TRAIL</code>.
*   </li>
*   <li>
*   <strong>Exceptions:</strong> The error checking will throw an exception 
*   if indices are out of bounds. Other than than that, all methods will 
*   behave reasonably, even if unmatched surrogates or out-of-bounds UTF-32 
*   values are present. <code>UCharacter.isLegal()</code> can be used to check 
*   for validity if desired.
*   </li>
*   <li>
*   <strong>Unmatched Surrogates:</strong> If the string contains unmatched 
*   surrogates, then these are counted as one UTF-32 value. This matches 
*   their iteration behavior, which is vital. It also matches common display 
*   practice as missing glyphs (see the Unicode Standard Section 5.4, 5.5).
*   </li>
*   <li>
*     <strong>Optimization:</strong> The method implementations may need 
*     optimization if the compiler doesn't fold static final methods. Since 
*     surrogate pairs will form an exceeding small percentage of all the text 
*     in the world, the singleton case should always be optimized for.
*   </li>
* </ul>
* @author Mark Davis, with help from Markus Scherer
* @since Nov2400
*/

public final class UTF16
{
  // public variables =============================================
  
  /**
  * Value returned in <code><a href="#bounds(java.lang.String, int)">
  * bounds()</a></code>.
  * These values are chosen specifically so that it actually represents the 
  * position of the character 
  * [offset16 - (value >> 2), offset16 + (value & 3)]
  */
  public static final int SINGLE_CHAR_BOUNDARY = 1, 
                          LEAD_SURROGATE_BOUNDARY = 2, 
                          TRAIL_SURROGATE_BOUNDARY = 5;
                          
  // private variables ============================================
	
  /** 
	* Lead surrogates values from LEAD_SURROGATE_MIN_VALUE_ to LEAD_SURROGATE_MAX_VALUE_
	*/
	private static final int LEAD_SURROGATE_MIN_VALUE_ = 0xD800;
	private static final int LEAD_SURROGATE_MAX_VALUE_ = 0xDBFF;
	
	/**
	* Surrogate lead offset, to be used when breaking up UTF32 into surrogate
	* pair
	*/
	private static final int LEAD_SURROGATE_OFFSET_ = LEAD_SURROGATE_MIN_VALUE_ - 
	    (UCharacter.SUPPLEMENTARY_MIN_VALUE >> UCharacter.LEAD_SURROGATE_SHIFT_);
	
	/** 
	* Trail surrogates values from TRAIL_SURROGATE_MIN_VALUE_ to 
	* TRAIL_SURROGATE_MAX_VALUE_
	*/
	private static final int TRAIL_SURROGATE_MIN_VALUE_ = 0xDC00; 
	private static final int TRAIL_SURROGATE_MAX_VALUE_ = 0xDFFF;
                          
  // constructor ==================================================
  
  /**
  * Prevent instance from being created.
  */
  private UTF16() 
  {
  }

  // public method ================================================
  
  /**
  * Extract a single UTF-32 value from a string.
  * Used when iterating forwards or backwards (with 
  * <code>UTF16.getCharCount()</code>, as well as random access. If a validity 
  * check is required, use <code><a href="../UCharacter.html#isLegal(char)">
  * UCharacter.isLegal()</a></code> on the return value.
  * If the char retrieved is part of a surrogate pair, its supplementary 
  * character will be returned. If a complete supplementary character is not
  * found the incomplete character will be returned
  * @param source array of UTF-16 chars
  * @param offset16 UTF-16 offset to the start of the character.
  * @return UTF-32 value for the UTF-32 value that contains the char at 
  *         offset16, otherwise -1 if there's an error.
  *         The boundaries of that codepoint are the same as in 
  *         <code>bounds32()</code>. 
  */
  public static int charAt(String source, int offset16) 
  {
    if (offset16 < 0 || offset16 >= source.length())
      return -1;
      
    char single = source.charAt(offset16);
    if (!isSurrogate(single)) 
      return single;

    // Convert the UTF-16 surrogate pair if necessary.
    // For simplicity in usage, and because the frequency of pairs is low,
    // look both directions.
            
	  if (isLeadSurrogate(single)) 
	  {
	    ++ offset16;
	    if (source.length() != offset16)
	    {
	      char trail = source.charAt(offset16);
	      if (isTrailSurrogate(trail))
	        return UCharacter.getRawSupplementary(single, trail);
	    }
	  } 
	  else 
	  { 
	    -- offset16;
	    if (offset16 >= 0)
	    {
	      // single is a trail surrogate so
	      char lead = source.charAt(offset16);
	      if (isLeadSurrogate(lead))
	        return UCharacter.getRawSupplementary(lead, single);
	    }
	  } 
	  return single; // return unmatched surrogate
  }
  
  /**
  * Extract a single UTF-32 value from a string.
  * If a validity check is required, use 
  * <code><a href="../UCharacter.html#isLegal(char)">
  * UCharacter.isLegal()</a></code> on the return value.
  * If tbe char retrieved is part of a surrogate pair, its supplementary 
  * character will be returned. If a complete supplementary character is not
  * found the incomplete character will be returned
  * @return UTF-32 value for the UTF-32 value that contains the char at 
  *         offset16. The boundaries of that codepoint are the same as in 
  *         <code>bounds32()</code>. 
  * @param source array of UTF-16 chars
  * @param offset32 UTF-32 offset to the start of the character.
  * @return a single UTF32 value, otherwise -1 if there's an error
  */
  public static int charAtCodePointOffset(String source, int offset32) 
  {
    int offset16 = findOffsetFromCodePoint(source, offset32);
    return charAt(source, offset16);
  }
  
  /**
  * Determines how many chars this char32 requires.
  * If a validity check is required, use <code>
  * <a href="../UCharacter.html#isLegal(char)">isLegal()</a></code> on 
  * char32 before calling.
  * @param ch the input character.
  * @return 2 if is in surrogate space, otherwise 1. 
  */
  public static int getCharCount(int char32) 
  {
    if (char32 < UCharacter.SUPPLEMENTARY_MIN_VALUE) {
      return 1;
    }
    return 2;
  }
  
  /**
  * Returns the type of the boundaries around the char at offset16.
  * Used for random access.
  * @param source text to analyse
  * @param offset16 UTF-16 offset
  * @return <ul>
  *           <li> SINGLE_CHAR_BOUNDARY : a single char; the bounds are 
  *                                       [offset16, offset16+1]
  *           <li> LEAD_SURROGATE_BOUNDARY : a surrogate pair starting at 
  *                                          offset16; 
  *                                          the bounds are 
  *                                          [offset16, offset16 + 2]
  *           <li> TRAIL_SURROGATE_BOUNDARY : a surrogate pair starting at 
  *                                           offset16 - 1; the bounds are 
  *                                           [offset16 - 1, offset16 + 1]
  *         </ul>
  *         For bit-twiddlers, the return values for these are chosen so that 
  *         the boundaries can be gotten by:
  *         [offset16 - (value >> 2), offset16 + (value & 3)].
  * @exception StringIndexOutOfBoundsException if offset16 is out of bounds.
  */
  public static int bounds(String source, int offset16) 
  {
    char ch = source.charAt(offset16);
    if (isSurrogate(ch))
      if (isLeadSurrogate(ch)) 
      {
       if (++ offset16 < source.length() && 
           isTrailSurrogate(source.charAt(offset16))) {
         return LEAD_SURROGATE_BOUNDARY;
       }
      } 
      else 
        // isTrailSurrogate(ch), so
        if (-- offset16 >= 0 && isLeadSurrogate(source.charAt(offset16))) {
          return TRAIL_SURROGATE_BOUNDARY;
        }
          
    return SINGLE_CHAR_BOUNDARY;
  }
  
  /**
  * Returns the type of the boundaries around the char at offset32.
  * Used for random access.
  * @param source text to analyse
  * @param offset32 UTF-32 offset
  * @return <ul>
  *           <li> SINGLE_CHAR_BOUNDARY : a single char; the bounds are 
  *                                       [offset32, offset32 + 1]
  *           <li> LEAD_SURROGATE_BOUNDARY : a surrogate pair starting at 
  *                                          offset32; 
  *                                          the bounds are 
  *                                          [offset32, offset32 + 2]
  *           <li> TRAIL_SURROGATE_BOUNDARY : a surrogate pair starting at 
  *                                           offset32 - 1; the bounds are 
  *                                           [offset32 - 1, offset32 + 1]
  *         </ul>
  *         For bit-twiddlers, the return values for these are chosen so that 
  *         the boundaries can be gotten by:
  *         [offset32 - (value >> 2), offset32 + (value & 3)].
  * @exception StringIndexOutOfBoundsException if offset32 is out of bounds.
  */
  public static int boundsAtCodePointOffset(String source, int offset32) 
  {
    int offset16 = findOffsetFromCodePoint(source, offset32);
    return bounds(source, offset16);
  }

  /**
  * Determines whether the <b>code value is a surrogate.
  * @param ch the input character.
  * @return true iff the input character is a surrogate.
  */
  public static boolean isSurrogate(char char16) 
  {
    return LEAD_SURROGATE_MIN_VALUE_ <= char16 && 
           char16 <= TRAIL_SURROGATE_MAX_VALUE_;
  }
    
  /**
  * Determines whether the character is a trail surrogate.
  * @param char16 the input character.
  * @return true iff the input character is a trail surrogate.
  */
  public static boolean isTrailSurrogate(char char16) 
  {
    return (TRAIL_SURROGATE_MIN_VALUE_ <= char16 && 
            char16 <= TRAIL_SURROGATE_MAX_VALUE_);
  }
    
  /**
  * Determines whether the character is a lead surrogate.
  * @param char16 the input character.
  * @return true iff the input character is a lead surrogate
  */
  public static boolean isLeadSurrogate(char char16) 
  {
    return LEAD_SURROGATE_MIN_VALUE_ <= char16 && 
           char16 <= LEAD_SURROGATE_MAX_VALUE_;
  }
        
  /**
  * Returns the lead surrogate.
  * If a validity check is required, use 
  * <code><a href="../UCharacter.html#isLegal(char)">isLegal()</a></code> on 
  * char32 before calling.
  * @param char32 the input character.
  * @return lead surrogate if the getCharCount(ch) is 2; <br>
  *         and 0 otherwise (note: 0 is not a valid lead surrogate).
  */
  public static int getLeadSurrogate(int char32) 
  {
    if (char32 >= UCharacter.SUPPLEMENTARY_MIN_VALUE) {
      return LEAD_SURROGATE_OFFSET_ + 
             (char32 >> UCharacter.LEAD_SURROGATE_SHIFT_);
    }
        
    return 0;
  }
    
  /**
  * Returns the trail surrogate.
  * If a validity check is required, use 
  * <code><a href="../UCharacter.html#isLegal(char)">isLegal()</a></code> on 
  * char32 before calling.
  * @param char32 the input character.
  * @return the trail surrogate if the getCharCount(ch) is 2; <br>otherwise the 
  *         character itself
  */
  public static int getTrailSurrogate(int char32) 
  {
    if (char32 >= UCharacter.SUPPLEMENTARY_MIN_VALUE) {
      return TRAIL_SURROGATE_MIN_VALUE_ + (char32 & 
             UCharacter.TRAIL_SURROGATE_MASK_);       
    }
      
    return (char)char32;
  }
    
  /**
  * Convenience method corresponding to String.valueOf(char). Returns a one 
  * or two char string containing the UTF-32 value in UTF16 format. If the 
  * input value can't be converted, it substitutes REPLACEMENT_CHAR. If a 
  * validity check is required, use 
  * <a href="../UCharacter.html#isLegal(char)">isLegal()</a></code> on 
  * char32 before calling.
  * @param char32 the input character.
  * @return string value of char32 in UTF16 format
  */
  public static String valueOf(int char32)
  {
    if (char32 < UCharacter.MIN_VALUE || char32 > UCharacter.MAX_VALUE) {
      throw new IllegalArgumentException("Illegal codepoint");
    }
    if (char32 < UCharacter.SUPPLEMENTARY_MIN_VALUE) {
      return String.valueOf((char)char32);
    }
    char str[] = new char[2];   
    str[0] = (char)(LEAD_SURROGATE_OFFSET_ + 
                    (char32 >> UCharacter.LEAD_SURROGATE_SHIFT_));
    str[1] = (char)(TRAIL_SURROGATE_MIN_VALUE_ + 
                    (char32 & UCharacter.TRAIL_SURROGATE_MASK_));
    return String.valueOf(str);
  }
  
  /**
  * Returns the UTF-16 offset that corresponds to a UTF-32 offset. 
  * Used for random access. See the <a name="_top_">class description</a> for 
  * notes on roundtripping.
  * @param source the UTF-16 string
  * @param offset32 UTF-32 offset
  * @return UTF-16 offset 
  * @exception StringIndexOutOfBoundsException if offset32 is out of bounds.
  */
  public static int findOffsetFromCodePoint(String source, int offset32) 
  {
    char ch;
    int size = source.length(),
        result = 0,
        count = offset32;
    while (result < size && count > 0)
    {
      ch = source.charAt(result);
      if (isLeadSurrogate(ch) && ((result + 1) < size) && 
          isTrailSurrogate(source.charAt(result + 1))) {
        result ++;
      }
        
      count --;
      result ++;
    }
    if (result >= size) {
      throw new StringIndexOutOfBoundsException(offset32);
    }
    return result;
  }
  
  /**
  * Returns the UTF-32 offset corresponding to the first UTF-32 boundary at or 
  * after the given UTF-16 offset. Used for random access. See the 
  * <a name="_top_">class description</a> for notes on roundtripping.<br>
  * <i>Note: If the UTF-16 offset is into the middle of a surrogate pair, then
  * the UTF-32 offset of the <strong>lead</strong> of the pair is returned.
  * </i>
  * <p>
  * To find the UTF-32 length of a string, use:
  *   <pre>
  *     len32 = getOffset32(source, source.length());
  *   </pre>
  * </p>
  * <p>
  * @param source text to analyse
  * @param offset16 UTF-16 offset < source text length.
  * @return UTF-32 offset
  * @exception StringIndexOutOfBoundsException if offset16 is out of bounds.
  */
  public static int findCodePointOffset(String source, int offset16) 
  {
    if (offset16 >= source.length()) {
      throw new StringIndexOutOfBoundsException(offset16);
    }
     
    int result = 0;
    char ch;
    boolean hadLeadSurrogate = false;
    
    for (int i = 0; i < offset16; ++ i) 
    {
      ch = source.charAt(i);
      if (hadLeadSurrogate && isTrailSurrogate(ch)) {
        hadLeadSurrogate = false;           // count valid trail as zero
      }
      else
      {
        hadLeadSurrogate = isLeadSurrogate(ch);
        ++ result;                          // count others as 1
      }
    }
    // end of source being a supplementary character
    // shift result back to the start of the supplementary character
    if (hadLeadSurrogate && isTrailSurrogate(source.charAt(offset16))) {
      result --;
    }
      
    return result;
  }

  /**
  * Append a single UTF-32 value to the end of a StringBuffer.
  * If a validity check is required, use 
  * <a href="../UCharacter.html#isLegal(char)">isLegal()</a></code> on 
  * char32 before calling.
  * @param char32 value to append. If out of bounds, substitutes 
  *               UTF32.REPLACEMENT_CHAR.
  * @return the updated StringBuffer
  */
  public static StringBuffer append(StringBuffer target, int char32)
  {
    // Check for irregular values
    if (char32 < UCharacter.MIN_VALUE || char32 > UCharacter.MAX_VALUE) {
      throw new IllegalArgumentException("Illegal codepoint");
    }
        
    // Write the UTF-16 values
    if (char32 >= UCharacter.SUPPLEMENTARY_MIN_VALUE) 
    {
      target.append((char)(LEAD_SURROGATE_OFFSET_ + 
                    (char32 >> UCharacter.LEAD_SURROGATE_SHIFT_)));
	    target.append((char)(TRAIL_SURROGATE_MIN_VALUE_ + 
                    (char32 & UCharacter.TRAIL_SURROGATE_MASK_)));
	  } 
	  else {
	    target.append((char)char32);
	  }
	  return target;
	}
    
  /**
  * Compare strings using Unicode code point order, instead of UTF-16 code 
  * unit order.
  */
  public static final class StringComparator implements java.util.Comparator 
  {
    /**
    * Standard String compare. Only one small section is different, marked in 
    * the code.
    */
    public int compare(Object a, Object b) 
    {
	    if (a == b) {
	      return 0;
	    }
      if (a == null) {
        return -1;
      }
      if (b == null) {
        return 1;
      }
          
      String sa = (String) a;
      String sb = (String) b;
      int lena = sa.length();
      int lenb = sb.length();
      int len = lena;
      if (len > lenb) {
        len = lenb;
      }
        
      for (int i = 0; i < len; ++i) 
      {
        char ca = sa.charAt(i);
        char cb = sb.charAt(i);
        if (ca == cb) {
          continue; // skip remap if equal
        }
                
        // start of only different section
        // what this part does is to rearrange the characters 0xE000 to 0xFFFF
        // to the region starting from 0xD800
        // and shift the surrogate characters to above this region
        if (ca >= LEAD_SURROGATE_MIN_VALUE_) {
          ca += (ca <= TRAIL_SURROGATE_MAX_VALUE_) ? 0x2000 : -0x800;
        }
        if (cb >= LEAD_SURROGATE_MIN_VALUE_) {
          cb += (cb <= TRAIL_SURROGATE_MAX_VALUE_) ? 0x2000 : -0x800;
        }
        // end of only different section
                
        if (ca < cb) {
          return -1;
        }
          
        return 1; // wasn't equal, so return 1
      }
      
      if (lena < lenb) {
        return -1;
      }
        
      if (lena > lenb) {
        return 1;
      }
            
      return 0;
    }
  }	
  
  /**
  * Number of codepoints in a UTF16 String
  * @param s UTF16 string
  * @return number of codepoint in string
  */
  public static int countCodePoint(String s)
  {
    return findCodePointOffset(s, s.length() - 1) + 1;
  }
  
  /**
  * Sets a code point into a UTF32 position.
  * @param str stringbuffer
  * @param offset32 UTF32 position to insert into
  * @param char32 code point
  */
  public static void setCharAtCodePointOffset(StringBuffer str, int offset32, 
                                       int char32)
  {
    int offset16 = findOffsetFromCodePoint(str.toString(), offset32);
    setCharAt(str, offset16, char32);
  }
  
  /**
  * Set a code point into a UTF16 position.
  * @param source stringbuffer
  * @param offset16 UTF16 position to insert into
  * @param char32 code point
  */
  public static void setCharAt(StringBuffer source, int offset16, int char32)
  {
    int count = 1;
      
    char single = source.charAt(offset16);
    
    if (isSurrogate(single)) 
    {
      // pairs of the surrogate with offset16 at the lead char found
      if (isLeadSurrogate(single) && (source.length() > offset16 + 1) &&
          isTrailSurrogate(source.charAt(offset16 + 1))) {
	      count ++;
	    }
	    else {
	      // pairs of the surrogate with offset16 at the trail char found
	      if (isTrailSurrogate(single) && (offset16 > 0) &&
	          isLeadSurrogate(source.charAt(offset16 -1)))
	      {
	        offset16 --;
	        count ++;
	      }
	    }
	  }
	  source.replace(offset16, offset16 + count, valueOf(char32));
	}
}
